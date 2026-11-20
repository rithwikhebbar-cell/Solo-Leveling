// Supabase Edge Function: AI System Administrator & Gemini Game Master Proxy
// Runtime: Deno (Supabase Edge Functions)
//
// Security guarantees:
// 1. Reads GEMINI_API_KEY / OPENROUTER_API_KEY exclusively from server-side environment secrets (Deno.env).
// 2. Never accepts API keys from client request body or headers.
// 3. Optionally enforces a shared proxy token (CLIENT_SHARED_TOKEN) or Supabase JWT.
// 4. Applies sliding-window rate limiting per IP / client token before calling upstream AI providers.
// 5. Validates model IDs, caps payload size, enforces request timeouts, retries transient 429/5xx, and logs non-sensitive telemetry.

import { serve } from "https://deno.land/std@0.224.0/http/server.ts";

interface RecentTurn {
  role: "user" | "assistant";
  content: string;
}

interface ProxyRequestPayload {
  model?: string;
  requestType?: "CHAT" | "QUEST_PROPOSALS" | "DAILY_PLAN" | "VOICE_INTENT";
  systemPrompt?: string;
  contextSummary?: string;
  recentTurns?: RecentTurn[];
  userPrompt?: string;
  stream?: boolean;
  temperature?: number;
  maxOutputTokens?: number;
}

const OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";
const GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta/models";
const DEFAULT_GEMINI_MODEL = Deno.env.get("DEFAULT_GEMINI_MODEL")?.trim() || "gemini-3.8-flash";
const UPSTREAM_TIMEOUT_MS = 35_000;

const ALLOWED_GEMINI_MODELS = new Set([
  "gemini-3.8-flash",
  "gemini-3.5-flash-lite",
  "gemini-4-argon",
]);

const ALLOWED_OPENROUTER_MODELS = new Set([
  "google/gemini-3.8-flash",
  "anthropic/claude-3.5-sonnet",
  "openai/gpt-4o-mini",
  "meta-llama/llama-3.3-70b-instruct",
]);

// Sliding-window in-memory rate limit per isolate instance (60s window, max 25 requests)
const RATE_WINDOW_MS = 60_000;
const RATE_MAX_REQUESTS = 25;
const rateBuckets = new Map<string, number[]>();

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type, x-client-token",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

function checkRateLimit(clientKey: string): boolean {
  const now = Date.now();
  const timestamps = (rateBuckets.get(clientKey) ?? []).filter((ts) => now - ts < RATE_WINDOW_MS);
  if (timestamps.length >= RATE_MAX_REQUESTS) {
    rateBuckets.set(clientKey, timestamps);
    return false;
  }
  timestamps.push(now);
  rateBuckets.set(clientKey, timestamps);
  return true;
}

function buildSystemInstruction(payload: ProxyRequestPayload): string {
  return [
    payload.systemPrompt?.slice(0, 1800) ||
      "You are the SOLO LEVELING Reality OS System Companion & Game Master. Provide intelligent, analytical, direct, safety-conscious guidance grounded strictly in the player snapshot. Never claim to award XP, modify attributes, or promote ranks.",
    payload.contextSummary ? `\n[MINIMIZED PLAYER TELEMETRY]\n${payload.contextSummary.slice(0, 3200)}` : "",
  ]
    .join("\n")
    .trim();
}

function buildOpenRouterMessages(payload: ProxyRequestPayload) {
  const messages: Array<{ role: string; content: string }> = [
    { role: "system", content: buildSystemInstruction(payload) },
  ];
  const turns = (payload.recentTurns ?? []).slice(-8);
  for (const turn of turns) {
    if ((turn.role === "user" || turn.role === "assistant") && typeof turn.content === "string") {
      messages.push({ role: turn.role, content: turn.content.slice(0, 800) });
    }
  }
  messages.push({
    role: "user",
    content: (payload.userPrompt ?? "").slice(0, 1400),
  });
  return messages;
}

function buildGeminiContents(payload: ProxyRequestPayload) {
  const contents: Array<{ role: "user" | "model"; parts: Array<{ text: string }> }> = [];
  const turns = (payload.recentTurns ?? []).slice(-8);
  for (const turn of turns) {
    if ((turn.role === "user" || turn.role === "assistant") && typeof turn.content === "string") {
      contents.push({
        role: turn.role === "assistant" ? "model" : "user",
        parts: [{ text: turn.content.slice(0, 800) }],
      });
    }
  }
  contents.push({
    role: "user",
    parts: [{ text: (payload.userPrompt ?? "").slice(0, 1400) }],
  });
  return contents;
}

async function fetchWithRetry(url: string, init: RequestInit, retries = 2): Promise<Response> {
  let attempt = 0;
  while (true) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), UPSTREAM_TIMEOUT_MS);
    try {
      const response = await fetch(url, { ...init, signal: controller.signal });
      clearTimeout(timer);
      if ((response.status === 429 || response.status >= 500) && attempt < retries) {
        attempt += 1;
        await new Promise((r) => setTimeout(r, 400 * Math.pow(2, attempt - 1)));
        continue;
      }
      return response;
    } catch (err) {
      clearTimeout(timer);
      if (attempt < retries) {
        attempt += 1;
        await new Promise((r) => setTimeout(r, 400 * Math.pow(2, attempt - 1)));
        continue;
      }
      throw err;
    }
  }
}

async function handleGeminiRequest(
  geminiApiKey: string,
  requestedModel: string,
  payload: ProxyRequestPayload
): Promise<Response> {
  const normalizedModel = requestedModel.replace(/^google\//i, "");
  const effectiveModel = ALLOWED_GEMINI_MODELS.has(normalizedModel) ? normalizedModel : DEFAULT_GEMINI_MODEL;
  const shouldStream = payload.stream === true && payload.requestType === "CHAT";
  const isJsonStructured =
    !shouldStream &&
    (payload.requestType === "QUEST_PROPOSALS" ||
      payload.requestType === "DAILY_PLAN" ||
      payload.requestType === "VOICE_INTENT");

  const temperature =
    typeof payload.temperature === "number"
      ? Math.min(Math.max(payload.temperature, 0), 1.2)
      : payload.requestType === "CHAT"
      ? 0.55
      : 0.2;
  const maxOutputTokens =
    typeof payload.maxOutputTokens === "number"
      ? Math.min(Math.max(payload.maxOutputTokens, 128), 2048)
      : payload.requestType === "CHAT"
      ? 900
      : 1024;

  const geminiBody: Record<string, unknown> = {
    systemInstruction: {
      parts: [{ text: buildSystemInstruction(payload) }],
    },
    contents: buildGeminiContents(payload),
    generationConfig: {
      temperature,
      maxOutputTokens,
      ...(isJsonStructured ? { responseMimeType: "application/json" } : {}),
    },
  };

  const action = shouldStream ? "streamGenerateContent?alt=sse" : "generateContent";
  const url = `${GEMINI_API_BASE}/${encodeURIComponent(effectiveModel)}:${action}`;

  const upstream = await fetchWithRetry(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "x-goog-api-key": geminiApiKey,
    },
    body: JSON.stringify(geminiBody),
  });

  if (!upstream.ok) {
    const errText = (await upstream.text()).slice(0, 300);
    return new Response(
      JSON.stringify({ error: `Gemini upstream ${upstream.status}: ${errText}` }),
      {
        status: upstream.status === 429 ? 429 : 502,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      }
    );
  }

  if (shouldStream && upstream.body) {
    const encoder = new TextEncoder();
    const decoder = new TextDecoder();
    const transformed = new ReadableStream({
      async start(controller) {
        const reader = upstream.body!.getReader();
        let buffer = "";
        try {
          while (true) {
            const { done, value } = await reader.read();
            if (done) break;
            buffer += decoder.decode(value, { stream: true });
            const lines = buffer.split("\n");
            buffer = lines.pop() ?? "";
            for (const line of lines) {
              const trimmed = line.trim();
              if (!trimmed.startsWith("data:")) continue;
              const rawJson = trimmed.slice(5).trim();
              if (!rawJson || rawJson === "[DONE]") continue;
              try {
                const parsed = JSON.parse(rawJson);
                const textDelta = parsed?.candidates?.[0]?.content?.parts?.[0]?.text ?? "";
                if (textDelta) {
                  const sseChunk = JSON.stringify({ choices: [{ delta: { content: textDelta } }] });
                  controller.enqueue(encoder.encode(`data: ${sseChunk}\n\n`));
                }
              } catch {
                // Skip malformed partial chunk safely
              }
            }
          }
          controller.enqueue(encoder.encode("data: [DONE]\n\n"));
          controller.close();
        } catch (err) {
          controller.error(err);
        }
      },
    });
    return new Response(transformed, {
      status: 200,
      headers: {
        ...corsHeaders,
        "Content-Type": "text/event-stream; charset=utf-8",
        "Cache-Control": "no-cache",
      },
    });
  }

  const jsonResp = await upstream.json();
  const content = jsonResp?.candidates?.[0]?.content?.parts?.[0]?.text ?? "";
  const usage = jsonResp?.usageMetadata ?? {};

  return new Response(
    JSON.stringify({
      content,
      modelUsed: effectiveModel,
      promptTokens: Number(usage?.promptTokenCount ?? 0),
      completionTokens: Number(usage?.candidatesTokenCount ?? 0),
    }),
    {
      status: 200,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    }
  );
}

serve(async (req: Request) => {
  const startedAt = Date.now();
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (req.method !== "POST") {
    return new Response(JSON.stringify({ error: "Method not allowed" }), {
      status: 405,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }

  const expectedToken = Deno.env.get("CLIENT_SHARED_TOKEN")?.trim();
  if (expectedToken) {
    const provided =
      req.headers.get("x-client-token") ??
      req.headers.get("authorization")?.replace(/^Bearer\s+/i, "") ??
      "";
    if (provided !== expectedToken) {
      return new Response(JSON.stringify({ error: "Unauthorized backend proxy token" }), {
        status: 401,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }
  }

  const clientKey =
    req.headers.get("x-forwarded-for")?.split(",")[0]?.trim() ??
    req.headers.get("x-client-token") ??
    "anonymous";

  if (!checkRateLimit(clientKey)) {
    return new Response(JSON.stringify({ error: "Rate limit exceeded. Retry shortly." }), {
      status: 429,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }

  let payload: ProxyRequestPayload;
  try {
    payload = await req.json();
  } catch {
    return new Response(JSON.stringify({ error: "Invalid JSON payload" }), {
      status: 400,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }

  if (!payload.userPrompt || !payload.userPrompt.trim()) {
    return new Response(JSON.stringify({ error: "userPrompt is required" }), {
      status: 400,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }

  const requestedModel = payload.model?.trim() || DEFAULT_GEMINI_MODEL;
  const geminiApiKey = Deno.env.get("GEMINI_API_KEY")?.trim();
  const openRouterApiKey = Deno.env.get("OPENROUTER_API_KEY")?.trim();

  // Prefer official Google Gemini API when server-side GEMINI_API_KEY is configured and model is Gemini
  const isGeminiModel =
    ALLOWED_GEMINI_MODELS.has(requestedModel) ||
    requestedModel.startsWith("gemini-") ||
    requestedModel.startsWith("google/gemini");

  if (geminiApiKey && isGeminiModel) {
    try {
      const resp = await handleGeminiRequest(geminiApiKey, requestedModel, payload);
      console.log(
        JSON.stringify({
          provider: "gemini",
          requestType: payload.requestType ?? "CHAT",
          model: requestedModel,
          status: resp.status,
          latencyMs: Date.now() - startedAt,
        })
      );
      return resp;
    } catch (err) {
      return new Response(
        JSON.stringify({ error: `Gemini upstream error: ${String(err).slice(0, 200)}` }),
        { status: 502, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }
  }

  if (!openRouterApiKey) {
    return new Response(
      JSON.stringify({ error: "Server secret GEMINI_API_KEY (or OPENROUTER_API_KEY) is not configured." }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }

  const allowCustomModels = Deno.env.get("ALLOW_CUSTOM_OPENROUTER_MODELS") === "true";
  if (!ALLOWED_OPENROUTER_MODELS.has(requestedModel) && !allowCustomModels) {
    return new Response(JSON.stringify({ error: `Disallowed model: ${requestedModel}` }), {
      status: 400,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }

  const shouldStream = payload.stream === true && payload.requestType === "CHAT";
  const openRouterBody: Record<string, unknown> = {
    model: requestedModel,
    messages: buildOpenRouterMessages(payload),
    stream: shouldStream,
    temperature: payload.requestType === "CHAT" ? 0.55 : 0.25,
    max_tokens: payload.requestType === "CHAT" ? 800 : 950,
  };

  if (
    !shouldStream &&
    (payload.requestType === "QUEST_PROPOSALS" ||
      payload.requestType === "DAILY_PLAN" ||
      payload.requestType === "VOICE_INTENT") &&
    requestedModel !== "meta-llama/llama-3.3-70b-instruct"
  ) {
    openRouterBody.response_format = { type: "json_object" };
  }

  const upstream = await fetchWithRetry(OPENROUTER_URL, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${openRouterApiKey}`,
      "Content-Type": "application/json",
      "HTTP-Referer": Deno.env.get("APP_REFERER") ?? "https://sololeveling-reality-os.local",
      "X-Title": "SOLO LEVELING Reality OS",
    },
    body: JSON.stringify(openRouterBody),
  });

  if (!upstream.ok) {
    const errText = (await upstream.text()).slice(0, 300);
    return new Response(
      JSON.stringify({ error: `Upstream AI ${upstream.status}: ${errText}` }),
      {
        status: upstream.status === 429 ? 429 : 502,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      }
    );
  }

  if (shouldStream && upstream.body) {
    return new Response(upstream.body, {
      status: 200,
      headers: {
        ...corsHeaders,
        "Content-Type": "text/event-stream; charset=utf-8",
        "Cache-Control": "no-cache",
      },
    });
  }

  const jsonResp = await upstream.json();
  const content = jsonResp?.choices?.[0]?.message?.content ?? "";
  const usage = jsonResp?.usage ?? {};

  return new Response(
    JSON.stringify({
      content,
      modelUsed: jsonResp?.model ?? requestedModel,
      promptTokens: Number(usage?.prompt_tokens ?? 0),
      completionTokens: Number(usage?.completion_tokens ?? 0),
    }),
    {
      status: 200,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    }
  );
});

