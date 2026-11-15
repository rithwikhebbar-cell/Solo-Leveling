// Supabase Edge Function: Deterministic Progression Validator
// Mirrors ProgressionEngine rules on the trusted server boundary before writing
// immutable progression_events, rpg_reward_audit, and player_identities totals.
//
// Security:
// 1. Authenticates caller via Supabase Auth JWT (never trusts body user_id).
// 2. Uses SUPABASE_SERVICE_ROLE_KEY strictly server-side.
// 3. Enforces idempotency_key deduplication & daily module XP caps.

import { serve } from "https://deno.land/std@0.224.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.49.1";

interface ProgressionEventInput {
  idempotencyKey: string;
  sourceModule: "PHYSICAL" | "ACADEMIC" | "LIFE" | "RPG";
  sourceRecordId: string;
  title: string;
  verificationStatus: "VERIFIED_HEALTH_CONNECT" | "VERIFIED_ASSESSMENT" | "VERIFIED_SYSTEM_TIMER" | "SELF_REPORTED";
  baseXp: number;
  primaryAttribute: string;
  primaryAttrXp: number;
  secondaryAttribute?: string | null;
  secondaryAttrXp?: number;
  deviceId?: string | null;
  occurredAtEpochMs: number;
}

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

const DAILY_CAPS: Record<string, number> = {
  PHYSICAL: 220,
  ACADEMIC: 240,
  LIFE: 200,
  RPG: 300,
};

function xpRequiredForLevel(level: number): number {
  const safe = Math.max(1, level);
  const delta = safe - 1;
  return 100 + delta * 50 + delta * delta * 15;
}

function multiplierFor(status: string): number {
  switch (status) {
    case "VERIFIED_HEALTH_CONNECT":
    case "VERIFIED_ASSESSMENT":
      return 1.0;
    case "VERIFIED_SYSTEM_TIMER":
      return 0.85;
    default:
      return 0.65;
  }
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

  const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
  const authHeader = req.headers.get("authorization") ?? "";

  if (!supabaseUrl || !serviceRoleKey || !authHeader.startsWith("Bearer ")) {
    return new Response(JSON.stringify({ error: "Unauthorized" }), {
      status: 401,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }

  const supabaseAdmin = createClient(supabaseUrl, serviceRoleKey);
  const jwt = authHeader.replace(/^Bearer\s+/i, "");
  const { data: userData, error: userErr } = await supabaseAdmin.auth.getUser(jwt);
  if (userErr || !userData.user) {
    return new Response(JSON.stringify({ error: "Invalid or expired session" }), {
      status: 401,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }

  const userId = userData.user.id;
  const body = await req.json();
  const events: ProgressionEventInput[] = Array.isArray(body.events) ? body.events.slice(0, 50) : [];

  const accepted: string[] = [];
  const duplicates: string[] = [];

  for (const ev of events) {
    if (!ev.idempotencyKey || !ev.sourceModule) continue;

    // Check existing idempotency_key
    const { data: existing } = await supabaseAdmin
      .from("progression_events")
      .select("id")
      .eq("user_id", userId)
      .eq("idempotency_key", ev.idempotencyKey)
      .maybeSingle();

    if (existing) {
      duplicates.push(ev.idempotencyKey);
      continue;
    }

    const dayStart = new Date(ev.occurredAtEpochMs);
    dayStart.setUTCHours(0, 0, 0, 0);
    const { data: dayRows } = await supabaseAdmin
      .from("progression_events")
      .select("xp_awarded")
      .eq("user_id", userId)
      .eq("source_module", ev.sourceModule)
      .gte("occurred_at", dayStart.toISOString());

    const earnedToday = (dayRows ?? []).reduce((sum, r) => sum + (r.xp_awarded ?? 0), 0);
    const cap = DAILY_CAPS[ev.sourceModule] ?? 200;
    const scaled = Math.max(0, Math.floor(ev.baseXp * multiplierFor(ev.verificationStatus)));
    const awarded = Math.min(scaled, Math.max(0, cap - earnedToday));
    const ratio = scaled <= 0 ? 0 : awarded / scaled;

    await supabaseAdmin.from("progression_events").insert({
      user_id: userId,
      idempotency_key: ev.idempotencyKey,
      source_module: ev.sourceModule,
      source_record_id: ev.sourceRecordId,
      device_id: ev.deviceId ?? null,
      verification_status: ev.verificationStatus,
      xp_awarded: awarded,
      primary_attribute: ev.primaryAttribute,
      primary_attr_xp: Math.floor((ev.primaryAttrXp ?? 0) * ratio),
      secondary_attribute: ev.secondaryAttribute ?? null,
      secondary_attr_xp: Math.floor((ev.secondaryAttrXp ?? 0) * ratio),
      was_capped: awarded < scaled,
      validation_note: awarded < scaled ? `Daily ${ev.sourceModule} cap applied` : "Server validated",
      occurred_at: new Date(ev.occurredAtEpochMs).toISOString(),
    });

    // Update authoritative player_identities level/xp curve
    const { data: identity } = await supabaseAdmin
      .from("player_identities")
      .select("level, current_level_xp, total_xp")
      .eq("user_id", userId)
      .maybeSingle();

    if (identity) {
      let level = identity.level ?? 1;
      let pool = (identity.current_level_xp ?? 0) + awarded;
      while (pool >= xpRequiredForLevel(level)) {
        pool -= xpRequiredForLevel(level);
        level += 1;
      }
      await supabaseAdmin
        .from("player_identities")
        .update({
          level,
          current_level_xp: pool,
          total_xp: (identity.total_xp ?? 0) + awarded,
          last_sync_at: new Date().toISOString(),
        })
        .eq("user_id", userId);
    }

    accepted.push(ev.idempotencyKey);
  }

  return new Response(JSON.stringify({ accepted, duplicates }), {
    status: 200,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
});
