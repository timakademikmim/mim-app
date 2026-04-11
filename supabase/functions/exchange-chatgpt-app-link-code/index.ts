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
  Deno.env.get("CHATGPT_APP_LINK_CODE_SHARED_SECRET") ||
  Deno.env.get("EXAM_AI_APP_SHARED_SECRET") ||
  ""
).trim()
const supabase = createClient(supabaseUrl, supabaseKey)

type ExchangePayload = {
  shared_secret?: string
  provider?: string
  code?: string
  external_subject?: string
  display_name?: string
  email?: string
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

  let payload: ExchangePayload
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
  const code = normalizeText(payload.code).toUpperCase()
  const externalSubject = normalizeText(payload.external_subject)
  const displayName = normalizeText(payload.display_name)
  const email = normalizeText(payload.email)

  if (!code) {
    return jsonResponse(400, { ok: false, error: "code wajib diisi" })
  }
  if (!externalSubject) {
    return jsonResponse(400, { ok: false, error: "external_subject wajib diisi" })
  }

  const { data: codeRow, error: codeError } = await supabase
    .from("chatgpt_app_link_codes")
    .select("id, provider, code, guru_id, guru_nama, expires_at, used_at, used_by_subject, is_active, metadata_json, created_at, updated_at")
    .eq("provider", provider)
    .eq("code", code)
    .maybeSingle()

  if (codeError) {
    return jsonResponse(500, { ok: false, error: codeError.message })
  }

  if (!codeRow) {
    return jsonResponse(404, { ok: false, error: "Kode tautan tidak ditemukan" })
  }

  const now = new Date()
  const expiresAt = new Date(String(codeRow.expires_at || ""))
  const isExpired = Number.isNaN(expiresAt.getTime()) ? false : expiresAt.getTime() < now.getTime()
  const isUsed = Boolean(codeRow.used_at || codeRow.used_by_subject)
  const isActive = Boolean(codeRow.is_active)

  if (!isActive || isUsed || isExpired) {
    return jsonResponse(409, {
      ok: false,
      error: "Kode tautan sudah tidak aktif",
      status: {
        is_active: isActive,
        is_used: isUsed,
        is_expired: isExpired
      }
    })
  }

  const linkPayload = {
    provider,
    external_subject: externalSubject,
    guru_id: codeRow.guru_id,
    guru_nama: codeRow.guru_nama || null,
    display_name: displayName || codeRow.guru_nama || null,
    email: email || null,
    is_active: true,
    metadata_json: {
      source: "chatgpt-link-code",
      linked_via: "exchange-chatgpt-app-link-code",
      code_id: codeRow.id,
      code_created_at: codeRow.created_at
    }
  }

  const { data: savedLink, error: linkError } = await supabase
    .from("chatgpt_app_guru_links")
    .upsert(linkPayload, { onConflict: "provider,external_subject" })
    .select("id, provider, external_subject, guru_id, guru_nama, display_name, email, metadata_json, is_active, created_at, updated_at, last_seen_at")
    .single()

  if (linkError) {
    return jsonResponse(500, { ok: false, error: linkError.message })
  }

  const { error: updateCodeError } = await supabase
    .from("chatgpt_app_link_codes")
    .update({
      used_at: now.toISOString(),
      used_by_subject: externalSubject,
      is_active: false,
      metadata_json: {
        ...(codeRow.metadata_json && typeof codeRow.metadata_json === "object" ? codeRow.metadata_json as Record<string, unknown> : {}),
        used_via: "exchange-chatgpt-app-link-code"
      }
    })
    .eq("id", String(codeRow.id || ""))

  if (updateCodeError) {
    return jsonResponse(500, {
      ok: false,
      error: updateCodeError.message,
      partial: {
        link_saved: true,
        code_updated: false
      }
    })
  }

  return jsonResponse(200, {
    ok: true,
    status: "linked",
    guru: savedLink,
    link_code: {
      id: codeRow.id,
      code,
      used_at: now.toISOString(),
      used_by_subject: externalSubject
    }
  })
})
