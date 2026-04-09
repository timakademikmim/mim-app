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

const SUPPORTED_TYPES = new Set(["pilihan-ganda", "benar-salah", "esai", "isi-titik"])

type QuestionInput = {
  no?: number
  type?: string
  text?: string
  answer?: string
  options?: Record<string, string>
}

type SectionInput = {
  type?: string
  start?: number
  end?: number
  count?: number
  instruction?: string
  score?: number | null
  wordPool?: string
}

type DraftPayload = {
  shared_secret?: string
  jadwal_id?: string
  kelas_target?: string | null
  guru_id?: string
  guru_nama?: string | null
  instruksi?: string | null
  instruksi_lang?: string | null
  questions?: QuestionInput[]
  sections?: SectionInput[]
  source?: string | null
}

function normalizeText(value: unknown) {
  return String(value ?? "").trim()
}

function normalizeLang(value: unknown) {
  return normalizeText(value).toUpperCase() === "AR" ? "AR" : "ID"
}

function buildInstruksiWithMeta(lang: string, text: string) {
  const clean = normalizeText(text)
  if (!clean) return ""
  return `[lang=${lang}] ${clean}`
}

function normalizeQuestionType(value: unknown) {
  const normalized = normalizeText(value).toLowerCase()
  if (SUPPORTED_TYPES.has(normalized)) return normalized
  return "pilihan-ganda"
}

function normalizePgOptions(value: unknown) {
  const source = value && typeof value === "object" ? value as Record<string, unknown> : {}
  const optionKeys = ["a", "b", "c", "d", "e"]
  const out: Record<string, string> = {}
  for (const key of optionKeys) {
    const text = normalizeText(source[key])
    if (text) out[key] = text
  }
  return out
}

function normalizeQuestions(value: unknown) {
  const rows = Array.isArray(value) ? value : []
  const normalized = rows
    .map((item, index) => {
      const row = item && typeof item === "object" ? item as QuestionInput : {}
      const no = Number(row.no || index + 1)
      const type = normalizeQuestionType(row.type)
      const text = normalizeText(row.text)
      if (!text && type !== "esai") return null
      if (type === "pilihan-ganda") {
        const options = normalizePgOptions(row.options)
        if (Object.keys(options).length < 2) return null
        return {
          no,
          type,
          text,
          options,
          answer: normalizeText(row.answer).toUpperCase()
        }
      }
      if (type === "benar-salah") {
        const answer = normalizeText(row.answer).toLowerCase()
        return {
          no,
          type,
          text,
          answer: answer === "benar" || answer === "salah" ? answer : ""
        }
      }
      if (type === "isi-titik") {
        return {
          no,
          type,
          text,
          answer: normalizeText(row.answer)
        }
      }
      return {
        no,
        type,
        text
      }
    })
    .filter(Boolean)
    .sort((a, b) => Number((a as { no: number }).no || 0) - Number((b as { no: number }).no || 0))

  return normalized
}

function normalizeSections(value: unknown, questions: Array<{ no: number; type: string }>) {
  const rows = Array.isArray(value) ? value : []
  const normalized = rows
    .map(item => {
      const row = item && typeof item === "object" ? item as SectionInput : {}
      const type = normalizeQuestionType(row.type)
      const start = Math.max(1, Number(row.start || 1))
      const endRaw = Number(row.end || row.start || start)
      const end = Math.max(start, endRaw)
      const count = Math.max(1, Number(row.count || (end - start + 1)))
      return {
        type,
        start,
        end,
        count,
        instruction: normalizeText(row.instruction),
        score: row.score == null || row.score === "" ? null : Number(row.score),
        wordPool: normalizeText(row.wordPool)
      }
    })
    .filter(Boolean)

  if (normalized.length) return normalized

  const total = questions.length
  if (!total) return []
  const firstType = normalizeQuestionType(questions[0]?.type)
  return [{
    type: firstType,
    start: 1,
    end: total,
    count: total,
    instruction: "",
    score: null,
    wordPool: ""
  }]
}

serve(async req => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders })
  }

  if (req.method !== "POST") {
    return jsonResponse(405, { ok: false, error: "Method not allowed" })
  }

  let payload: DraftPayload
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
  const kelasTarget = normalizeText(payload.kelas_target) || null
  const guruId = normalizeText(payload.guru_id)
  const guruNama = normalizeText(payload.guru_nama) || null
  const instruksiLang = normalizeLang(payload.instruksi_lang)
  const instruksi = buildInstruksiWithMeta(instruksiLang, normalizeText(payload.instruksi))
  const questions = normalizeQuestions(payload.questions)
  const sections = normalizeSections(payload.sections, questions as Array<{ no: number; type: string }>)

  if (!jadwalId || !guruId) {
    return jsonResponse(400, {
      ok: false,
      error: "jadwal_id dan guru_id wajib diisi"
    })
  }

  if (!questions.length) {
    return jsonResponse(400, {
      ok: false,
      error: "questions minimal satu item yang valid"
    })
  }

  const questionsJson = JSON.stringify({
    questions,
    sections,
    meta: {
      source: normalizeText(payload.source || "chatgpt-app") || "chatgpt-app",
      created_via: "save-exam-ai-draft",
      created_at: new Date().toISOString()
    }
  })

  const basePayload = {
    jadwal_id: jadwalId,
    kelas_target: kelasTarget,
    guru_id: guruId,
    guru_nama: guruNama,
    bentuk_soal: "campuran",
    jumlah_nomor: questions.length,
    instruksi,
    questions_json: questionsJson,
    status: "draft",
    updated_at: new Date().toISOString()
  }

  let existingId: string | null = null
  const existingQuery = await supabase
    .from("soal_ujian")
    .select("id, kelas_target")
    .eq("jadwal_id", jadwalId)
    .eq("guru_id", guruId)
    .eq("kelas_target", kelasTarget)
    .maybeSingle()

  if (!existingQuery.error && existingQuery.data?.id) {
    existingId = String(existingQuery.data.id)
  } else if (kelasTarget == null) {
    const fallbackQuery = await supabase
      .from("soal_ujian")
      .select("id, kelas_target")
      .eq("jadwal_id", jadwalId)
      .eq("guru_id", guruId)
      .is("kelas_target", null)
      .maybeSingle()
    if (!fallbackQuery.error && fallbackQuery.data?.id) {
      existingId = String(fallbackQuery.data.id)
    }
  }

  const query = existingId
    ? supabase
        .from("soal_ujian")
        .update(basePayload)
        .eq("id", existingId)
        .select("id, jadwal_id, kelas_target, guru_id, guru_nama, bentuk_soal, jumlah_nomor, instruksi, questions_json, status, updated_at")
        .maybeSingle()
    : supabase
        .from("soal_ujian")
        .insert([basePayload])
        .select("id, jadwal_id, kelas_target, guru_id, guru_nama, bentuk_soal, jumlah_nomor, instruksi, questions_json, status, updated_at")
        .maybeSingle()

  const { data, error } = await query
  if (error) {
    return jsonResponse(500, { ok: false, error: error.message })
  }

  return jsonResponse(200, {
    ok: true,
    draft: data,
    summary: {
      jadwal_id: jadwalId,
      kelas_target: kelasTarget,
      guru_id: guruId,
      question_count: questions.length,
      section_count: sections.length
    }
  })
})
