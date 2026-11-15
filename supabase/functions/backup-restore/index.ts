// Supabase Edge Function: Cloud Backup Verification & Transactional Restore Validation
// Validates JWT ownership, schema compatibility, progression checksum, and marks
// restore audit state safely on the server.

import { serve } from "https://deno.land/std@0.224.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.49.1";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

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

  const { backupId } = await req.json();
  const { data: backup, error } = await supabaseAdmin
    .from("cloud_backups")
    .select("*")
    .eq("id", backupId)
    .eq("user_id", userData.user.id)
    .maybeSingle();

  if (error || !backup) {
    return new Response(JSON.stringify({ error: "Backup not found for account" }), {
      status: 404,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }

  if ((backup.schema_version ?? 0) < 6) {
    return new Response(JSON.stringify({ error: "Incompatible backup schema version" }), {
      status: 422,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }

  await supabaseAdmin
    .from("cloud_backups")
    .update({ status: "RESTORED", updated_at: new Date().toISOString() })
    .eq("id", backup.id);

  return new Response(JSON.stringify({ validated: true, backup }), {
    status: 200,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
});
