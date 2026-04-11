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
const sharedSecret = String(
  Deno.env.get("CHATGPT_APP_GURU_RESOLVER_SHARED_SECRET") ||
  Deno.env.get("EXAM_AI_APP_SHARED_SECRET") ||
  ""
).trim()
const supabase = createClient(supabaseUrl, supabaseKey)

type ResolvePayload = {
  shared_secret?: string
  provider?: string
  external_subject?: string
}

function normalizeText(value: unknown) {
  return String(value ?? "").trim()
}

serve(async req => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders })
  }

  if (req.method !== "POST") {
    return jsonResponse(405, { ok: false, error: "Method not allowed" })
  }

  let payload: ResolvePayload
  try {
    payload = await req.json()
  } catch (_error) {
    return jsonResponse(400, { ok: false, error: "Invalid JSON" })
  }

  if (sharedSecret) {
    const incomingSecret = normalizeText(payload.shared_secret)
    if (!incomingSecret || incomingSecret !== sharedSecret) {
      return jsonResponse(401, { ok: false, error: "Unauthorized" })
    }
  }

  const provider = normalizeText(payload.provider || "chatgpt") || "chatgpt"
  const externalSubject = normalizeText(payload.external_subject)

  if (!externalSubject) {
    return jsonResponse(400, { ok: false, error: "external_subject wajib diisi" })
  }

  const { data, error } = await supabase
    .from("chatgpt_app_guru_links")
    .select("id, provider, external_subject, guru_id, guru_nama, display_name, email, metadata_json, is_active, created_at, updated_at, last_seen_at")
    .eq("provider", provider)
    .eq("external_subject", externalSubject)
    .eq("is_active", true)
    .maybeSingle()

  if (error) {
    return jsonResponse(500, { ok: false, error: error.message })
  }

  if (!data) {
    return jsonResponse(200, {
      ok: true,
      status: "not_linked",
      guru: null,
      meta: {
        provider,
        external_subject: externalSubject
      }
    })
  }

  await supabase
    .from("chatgpt_app_guru_links")
    .update({ last_seen_at: new Date().toISOString() })
    .eq("id", String(data.id || ""))

  return jsonResponse(200, {
    ok: true,
    status: "linked",
    guru: data,
    meta: {
      provider,
      external_subject: externalSubject
    }
  })
})
