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
const sharedSecret = String(Deno.env.get("EXAM_AI_APP_SHARED_SECRET") || "").trim()
const supabase = createClient(supabaseUrl, supabaseKey)

type DraftLookupPayload = {
  shared_secret?: string
  jadwal_id?: string
  guru_id?: string
  kelas_target?: string | null
}

function normalizeText(value: unknown) {
  return String(value ?? "").trim()
}

function parseQuestionsPayload(value: unknown) {
  const raw = normalizeText(value)
  if (!raw) return { questions: [], sections: [], meta: {} }
  try {
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed)) return { questions: parsed, sections: [], meta: {} }
    if (parsed && typeof parsed === "object") {
      return {
        questions: Array.isArray(parsed.questions) ? parsed.questions : [],
        sections: Array.isArray(parsed.sections) ? parsed.sections : [],
        meta: parsed.meta && typeof parsed.meta === "object" ? parsed.meta : {}
      }
    }
  } catch (_error) {
    return { questions: [], sections: [], meta: {} }
  }
  return { questions: [], sections: [], meta: {} }
}

serve(async req => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders })
  }

  if (req.method !== "POST") {
    return jsonResponse(405, { ok: false, error: "Method not allowed" })
  }

  let payload: DraftLookupPayload
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

  const jadwalId = normalizeText(payload.jadwal_id)
  const guruId = normalizeText(payload.guru_id)
  const kelasTarget = normalizeText(payload.kelas_target) || null

  if (!jadwalId || !guruId) {
    return jsonResponse(400, {
      ok: false,
      error: "jadwal_id dan guru_id wajib diisi"
    })
  }

  let query = supabase
    .from("soal_ujian")
    .select("id, jadwal_id, kelas_target, guru_id, guru_nama, bentuk_soal, jumlah_nomor, instruksi, questions_json, status, created_at, updated_at")
    .eq("jadwal_id", jadwalId)
    .eq("guru_id", guruId)

  if (kelasTarget == null) {
    query = query.is("kelas_target", null)
  } else {
    query = query.eq("kelas_target", kelasTarget)
  }

  const { data, error } = await query.maybeSingle()
  if (error) {
    return jsonResponse(500, { ok: false, error: error.message })
  }

  if (!data) {
    return jsonResponse(200, {
      ok: true,
      status: "not_found",
      draft: null
    })
  }

  const parsed = parseQuestionsPayload(data.questions_json)

  return jsonResponse(200, {
    ok: true,
    status: "found",
    draft: {
      ...data,
      parsed_questions_json: parsed
    },
    summary: {
      jadwal_id: normalizeText(data.jadwal_id),
      guru_id: normalizeText(data.guru_id),
      kelas_target: normalizeText(data.kelas_target) || null,
      question_count: Array.isArray(parsed.questions) ? parsed.questions.length : 0,
      section_count: Array.isArray(parsed.sections) ? parsed.sections.length : 0,
      source: normalizeText((parsed.meta as Record<string, unknown>)?.source) || null
    }
  })
})
