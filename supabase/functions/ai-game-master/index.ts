// Supabase Edge Function: AI System Administrator & Game Master Proxy
// Runtime: Deno (Supabase Edge Functions)
//
// Security guarantees:
// 1. Reads OPENROUTER_API_KEY exclusively from server-side environment secrets (Deno.env).
// 2. Never accepts an OpenRouter key from the client request body or headers.
// 3. Optionally enforces a shared proxy token (CLIENT_SHARED_TOKEN) or Supabase JWT.
// 4. Applies sliding-window rate limiting per IP / client token before calling OpenRouter.
// 5. Validates model IDs, caps payload size, and strips unneeded metadata.

import { serve } from "https://deno.land/std@0.224.0/http/server.ts";

interface RecentTurn {
  role: "user" | "assistant";
  content: string;
}

interface ProxyRequestPayload {
  model?: string;
  requestType?: "CHAT" | "QUEST_PROPOSALS" | "DAILY_PLAN";
  systemPrompt?: string;
  contextSummary?: string;
  recentTurns?: RecentTurn[];
  userPrompt?: string;
  stream?: boolean;
}

const OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";
const DEFAULT_MODEL = "google/gemini-2.5-flash";
const ALLOWED_MODELS = new Set([
  "google/gemini-2.5-flash",
  "anthropic/claude-3.5-sonnet",
  "openai/gpt-4o-mini",
  "meta-llama/llama-3.3-70b-instruct",
]);

// Sliding-window in-memory rate limit per isolate instance (60s window, max 20 requests)
const RATE_WINDOW_MS = 60_000;
const RATE_MAX_REQUESTS = 20;
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

function buildMessages(payload: ProxyRequestPayload) {
  const systemText = [
    payload.systemPrompt?.slice(0, 1400) ||
      "You are the SOLO LEVELING Reality OS System Administrator & Game Master. Provide concise, actionable, safety-conscious guidance grounded strictly in the player snapshot. Never claim to award XP, modify attributes, or promote ranks.",
    payload.contextSummary ? `\n[MINIMIZED PLAYER TELEMETRY]\n${payload.contextSummary.slice(0, 2800)}` : "",
  ]
    .join("\n")
    .trim();

  const messages: Array<{ role: string; content: string }> = [
    { role: "system", content: systemText },
  ];

  const turns = (payload.recentTurns ?? []).slice(-6);
  for (const turn of turns) {
    if ((turn.role === "user" || turn.role === "assistant") && typeof turn.content === "string") {
      messages.push({ role: turn.role, content: turn.content.slice(0, 600) });
    }
  }

  messages.push({
    role: "user",
    content: (payload.userPrompt ?? "").slice(0, 1200),
  });
  return messages;
}

serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (req.method !== "POST") {
    return new Response(JSON.stringify({ error: "Method not allowed" }), {
      status: 405,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }

  // Optional shared token verification if configured on backend
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

  const openRouterApiKey = Deno.env.get("OPENROUTER_API_KEY")?.trim();
  if (!openRouterApiKey) {
    return new Response(
      JSON.stringify({ error: "Server secret OPENROUTER_API_KEY is not configured." }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
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

  const requestedModel = payload.model?.trim() || DEFAULT_MODEL;
  const allowCustomModels = Deno.env.get("ALLOW_CUSTOM_OPENROUTER_MODELS") === "true";
  if (!ALLOWED_MODELS.has(requestedModel) && !allowCustomModels) {
    return new Response(JSON.stringify({ error: `Disallowed model: ${requestedModel}` }), {
      status: 400,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }

  const shouldStream = payload.stream === true && payload.requestType === "CHAT";
  const openRouterBody: Record<string, unknown> = {
    model: requestedModel,
    messages: buildMessages(payload),
    stream: shouldStream,
    temperature: payload.requestType === "CHAT" ? 0.55 : 0.25,
    max_tokens: payload.requestType === "CHAT" ? 700 : 900,
  };

  if (
    !shouldStream &&
    (payload.requestType === "QUEST_PROPOSALS" || payload.requestType === "DAILY_PLAN") &&
    requestedModel !== "meta-llama/llama-3.3-70b-instruct"
  ) {
    openRouterBody.response_format = { type: "json_object" };
  }

  const upstream = await fetch(OPENROUTER_URL, {
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
      JSON.stringify({ error: `OpenRouter upstream ${upstream.status}: ${errText}` }),
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
