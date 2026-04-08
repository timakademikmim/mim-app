import { serve } from "https://deno.land/std@0.224.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

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

  if (req.method === "GET") {
    const publicKey = String(Deno.env.get("WEB_PUSH_VAPID_PUBLIC_KEY") || "").trim()
    if (!publicKey) {
      return jsonResponse(500, { ok: false, error: "WEB_PUSH_VAPID_PUBLIC_KEY belum diset" })
    }
    return jsonResponse(200, { ok: true, public_key: publicKey })
  }

  if (req.method !== "POST") {
    return jsonResponse(405, { ok: false, error: "Method not allowed" })
  }

  let payload: {
    user_id?: string
    endpoint?: string
    keys?: { p256dh?: string; auth?: string }
    role?: string
    user_agent?: string
    device_id?: string
  }

  try {
    payload = await req.json()
  } catch (_error) {
    return jsonResponse(400, { ok: false, error: "Invalid JSON" })
  }

  const userId = String(payload.user_id || "").trim()
  const endpoint = String(payload.endpoint || "").trim()
  const p256dh = String(payload.keys?.p256dh || "").trim()
  const auth = String(payload.keys?.auth || "").trim()
  const role = String(payload.role || "").trim().toLowerCase() || null
  const userAgent = String(payload.user_agent || "").trim() || null
  const deviceId = String(payload.device_id || "").trim() || null

  if (!userId || !endpoint || !p256dh || !auth) {
    return jsonResponse(400, {
      ok: false,
      error: "user_id, endpoint, keys.p256dh, dan keys.auth wajib diisi"
    })
  }

  const nowIso = new Date().toISOString()
  const { error: upsertError } = await supabase
    .from("web_push_subscriptions")
    .upsert(
      {
        user_id: userId,
        endpoint,
        p256dh,
        auth,
        role,
        user_agent: userAgent,
        device_id: deviceId,
        is_active: true,
        last_seen_at: nowIso,
        updated_at: nowIso
      },
      { onConflict: "endpoint" }
    )

  if (upsertError) {
    return jsonResponse(500, { ok: false, error: upsertError.message })
  }

  if (deviceId) {
    await supabase
      .from("web_push_subscriptions")
      .update({
        is_active: false,
        updated_at: nowIso
      })
      .eq("user_id", userId)
      .eq("device_id", deviceId)
      .neq("endpoint", endpoint)
  }

  return jsonResponse(200, { ok: true })
})
