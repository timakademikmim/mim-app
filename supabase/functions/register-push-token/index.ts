import { serve } from "https://deno.land/std@0.224.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"
import { requireTenantCaller, TenantAuthError } from "../_shared/tenant-auth.ts"

const corsHeaders = {
  "access-control-allow-origin": "*",
  "access-control-allow-headers": "authorization, x-client-info, apikey, content-type"
}

function jsonResponse(status: number, body: Record<string, unknown>) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", ...corsHeaders }
  })
}

const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? ""
const supabaseKey =
  Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ||
  Deno.env.get("SUPABASE_ANON_KEY") ||
  ""

const supabase = createClient(supabaseUrl, supabaseKey)

serve(async req => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders })
  }
  if (req.method !== "POST") {
    return jsonResponse(405, { ok: false, error: "Method not allowed" })
  }

  let payload: {
    user_id?: string
    token?: string
    device_id?: string
    platform?: string
    app_version?: string
  }

  try {
    payload = await req.json()
  } catch (_error) {
    return jsonResponse(400, { ok: false, error: "Invalid JSON" })
  }

  let caller
  try {
    caller = await requireTenantCaller(req)
  } catch (error) {
    const status = error instanceof TenantAuthError ? error.status : 401
    return jsonResponse(status, { ok: false, error: error instanceof Error ? error.message : "Sesi tidak valid" })
  }

  const requestedUserId = String(payload.user_id || "").trim()
  const userId = caller.employeeLoginId
  const token = String(payload.token || "").trim()
  if (requestedUserId && requestedUserId.toLowerCase() !== userId.toLowerCase()) {
    return jsonResponse(403, { ok: false, error: "Token hanya dapat didaftarkan untuk akun sendiri" })
  }
  if (!userId || !token || !caller.tenantId) {
    return jsonResponse(400, { ok: false, error: "Identitas pengguna dan token wajib tersedia" })
  }

  const deviceId = String(payload.device_id || "").trim() || null
  const platform = String(payload.platform || "android").trim() || "android"
  const appVersion = String(payload.app_version || "").trim() || null

  const { error: upsertError } = await supabase
    .from("push_tokens")
    .upsert(
      {
        user_id: userId,
        tenant_id: caller.tenantId,
        token,
        device_id: deviceId,
        platform,
        app_version: appVersion,
        updated_at: new Date().toISOString()
      },
      { onConflict: "token" }
    )

  if (upsertError) {
    return jsonResponse(500, { ok: false, error: upsertError.message })
  }

  if (deviceId) {
    await supabase
      .from("push_tokens")
      .delete()
      .eq("user_id", userId)
      .eq("tenant_id", caller.tenantId)
      .eq("device_id", deviceId)
      .neq("token", token)
  }

  return jsonResponse(200, { ok: true })
})
