import { serve } from "https://deno.land/std@0.224.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const corsHeaders = {
  "access-control-allow-origin": "*",
  "access-control-allow-headers": "authorization, x-client-info, apikey, content-type",
}

function response(status: number, body: Record<string, unknown>) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", ...corsHeaders },
  })
}

async function fingerprint(value: string) {
  const bytes = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value))
  return Array.from(new Uint8Array(bytes)).map(byte => byte.toString(16).padStart(2, "0")).join("")
}

serve(async req => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders })
  if (req.method !== "POST") return response(405, { ok: false, error: "Method not allowed" })

  const generic = { ok: true, message: "Jika akun valid, permintaan reset telah dikirim kepada admin unit." }
  try {
    const payload = await req.json()
    const tenantId = String(payload?.tenant_id || "").trim()
    const loginId = String(payload?.login_id || "").trim().toLowerCase().replace(/\s+/g, "")
    if (!/^[0-9a-f-]{36}$/i.test(tenantId) || !loginId || loginId.length > 120) return response(200, generic)

    const url = String(Deno.env.get("SUPABASE_URL") || "")
    const serviceKey = String(Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || "")
    if (!url || !serviceKey) return response(500, { ok: false, error: "Konfigurasi server belum lengkap." })
    const admin = createClient(url, serviceKey, { auth: { persistSession: false, autoRefreshToken: false } })
    const forwarded = String(req.headers.get("x-forwarded-for") || req.headers.get("cf-connecting-ip") || "unknown").split(",")[0].trim()
    const agent = String(req.headers.get("user-agent") || "unknown").slice(0, 300)
    const requesterFingerprint = await fingerprint(`${forwarded}|${agent}|${tenantId}`)
    const since = new Date(Date.now() - 60 * 60 * 1000).toISOString()
    const { count } = await admin
      .from("password_reset_requests")
      .select("id", { head: true, count: "exact" })
      .eq("requester_fingerprint", requesterFingerprint)
      .gte("requested_at", since)
    if ((count || 0) >= 5) return response(200, generic)

    const { data: employee } = await admin
      .from("karyawan")
      .select("id")
      .eq("tenant_id", tenantId)
      .eq("aktif", true)
      .not("auth_user_id", "is", null)
      .ilike("id_karyawan", loginId)
      .maybeSingle()
    if (!employee?.id) return response(200, generic)

    const { data: pending } = await admin
      .from("password_reset_requests")
      .select("id")
      .eq("employee_id", employee.id)
      .eq("status", "pending")
      .maybeSingle()
    if (!pending) {
      await admin.from("password_reset_requests").insert({
        tenant_id: tenantId,
        employee_id: employee.id,
        requester_fingerprint: requesterFingerprint,
      })
    }
    return response(200, generic)
  } catch (_error) {
    return response(200, generic)
  }
})
