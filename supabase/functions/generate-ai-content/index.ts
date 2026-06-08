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
const supabaseKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""
const openAiApiKey = Deno.env.get("OPENAI_API_KEY") ?? ""
const openAiModel = String(Deno.env.get("MIM_AI_MODEL") || Deno.env.get("OPENAI_MODEL") || "gpt-5.5").trim()
const tokenMultiplier = Math.max(1, Number(Deno.env.get("MIM_AI_TOKEN_MULTIPLIER") || "1"))
const minChargeTokens = Math.max(1, Number(Deno.env.get("MIM_AI_MIN_CHARGE_TOKENS") || "100"))
const supabase = createClient(supabaseUrl, supabaseKey)

type AiFeature = "materi" | "soal"

type GeneratePayload = {
  action?: string
  guru_id?: string
  guru_nama?: string
  distribusi_id?: string
  feature?: string
  subject_title?: string
  class_name?: string
  semester?: string
  language?: string
  prompt?: string
  count?: number
  question_type?: string
  existing_items?: string[]
  academic_year?: string
}

function normalizeText(value: unknown) {
  return String(value ?? "").trim()
}

function errorMessage(error: unknown) {
  if (error && typeof error === "object" && "message" in error) {
    return String((error as { message?: unknown }).message || error)
  }
  return String(error)
}

function normalizeFeature(value: unknown): AiFeature {
  return normalizeText(value).toLowerCase() === "soal" ? "soal" : "materi"
}

function normalizeLanguage(value: unknown) {
  const raw = normalizeText(value).toUpperCase()
  if (raw === "AR" || raw === "ARAB" || raw === "ARABIC") return "AR"
  return "ID"
}

function normalizeCount(value: unknown, fallback: number, max: number) {
  const numberValue = Number(value)
  if (!Number.isFinite(numberValue)) return fallback
  return Math.max(1, Math.min(max, Math.round(numberValue)))
}

function estimateChargeTokens(payload: GeneratePayload, feature: AiFeature) {
  const promptLength = normalizeText(payload.prompt).length
  const count = normalizeCount(payload.count, feature === "soal" ? 5 : 3, feature === "soal" ? 30 : 8)
  const base = feature === "soal" ? 900 : 700
  const perItem = feature === "soal" ? 260 : 380
  const promptCost = Math.ceil(promptLength / 4)
  return Math.max(minChargeTokens, base + perItem * count + promptCost)
}

function chargeFromUsage(totalTokens: number) {
  return Math.max(minChargeTokens, Math.ceil(Math.max(0, totalTokens) * tokenMultiplier))
}

async function getWallet(guruId: string) {
  const { data, error } = await supabase.rpc("ai_get_or_create_wallet", {
    p_guru_id: guruId
  })
  if (error) throw error
  return normalizeWallet(data)
}

async function debitTokens(
  guruId: string,
  amount: number,
  reason: string,
  refType: string,
  refId: string,
  metadata: Record<string, unknown>
) {
  const { data, error } = await supabase.rpc("ai_debit_tokens", {
    p_guru_id: guruId,
    p_amount: amount,
    p_reason: reason,
    p_ref_type: refType,
    p_ref_id: refId,
    p_metadata_json: metadata,
    p_created_by: "generate-ai-content"
  })
  if (error) throw error
  return normalizeRpcJson(data)
}

async function creditTokens(
  guruId: string,
  amount: number,
  reason: string,
  refType: string,
  refId: string,
  metadata: Record<string, unknown>
) {
  if (amount <= 0) return null
  const { data, error } = await supabase.rpc("ai_credit_tokens", {
    p_guru_id: guruId,
    p_amount: amount,
    p_reason: reason,
    p_ref_type: refType,
    p_ref_id: refId,
    p_metadata_json: metadata,
    p_created_by: "generate-ai-content"
  })
  if (error) throw error
  return normalizeRpcJson(data)
}

async function logUsage(row: Record<string, unknown>) {
  await supabase.from("ai_usage_logs").insert([row])
}

function normalizeRpcJson(value: unknown): Record<string, unknown> {
  if (value && typeof value === "object") return value as Record<string, unknown>
  if (typeof value === "string") {
    try {
      const parsed = JSON.parse(value)
      if (parsed && typeof parsed === "object") return parsed as Record<string, unknown>
    } catch (_error) {
      return {}
    }
  }
  return {}
}

function normalizeWallet(value: unknown) {
  const row = normalizeRpcJson(value)
  return {
    guru_id: normalizeText(row.guru_id),
    balance_tokens: Number(row.balance_tokens || 0),
    total_purchased_tokens: Number(row.total_purchased_tokens || 0),
    total_used_tokens: Number(row.total_used_tokens || 0)
  }
}

function materialSchema() {
  return {
    type: "object",
    additionalProperties: false,
    properties: {
      title: { type: "string" },
      summary: { type: "string" },
      items: {
        type: "array",
        minItems: 1,
        maxItems: 8,
        items: {
          type: "object",
          additionalProperties: false,
          properties: {
            title: { type: "string" },
            body: { type: "string" },
            bullet_points: {
              type: "array",
              items: { type: "string" }
            }
          },
          required: ["title", "body", "bullet_points"]
        }
      }
    },
    required: ["title", "summary", "items"]
  }
}

function questionSchema() {
  return {
    type: "object",
    additionalProperties: false,
    properties: {
      title: { type: "string" },
      instruction: { type: "string" },
      language: { type: "string", enum: ["ID", "AR"] },
      question_type: { type: "string" },
      questions: {
        type: "array",
        minItems: 1,
        maxItems: 30,
        items: {
          type: "object",
          additionalProperties: false,
          properties: {
            prompt: { type: "string" },
            options: {
              type: "array",
              items: { type: "string" }
            },
            answer: { type: "string" }
          },
          required: ["prompt", "options", "answer"]
        }
      }
    },
    required: ["title", "instruction", "language", "question_type", "questions"]
  }
}

function buildInstructions(feature: AiFeature, language: string) {
  const targetLanguage = language === "AR" ? "Arab" : "Indonesia"
  if (feature === "soal") {
    return [
      "Anda adalah asisten guru Madrasah yang membuat draft soal siap review.",
      `Gunakan bahasa ${targetLanguage}.`,
      "Soal harus sesuai konteks mapel, kelas, dan instruksi guru.",
      "Jangan menyebut bahwa soal dibuat AI.",
      "Kembalikan hanya data yang sesuai JSON Schema."
    ].join("\n")
  }
  return [
    "Anda adalah asisten guru Madrasah yang menyusun materi pembelajaran ringkas dan siap dikembangkan guru.",
    `Gunakan bahasa ${targetLanguage}.`,
    "Materi harus terstruktur, mudah diajarkan, dan cocok untuk kelas yang diminta.",
    "Jangan menyebut bahwa materi dibuat AI.",
    "Kembalikan hanya data yang sesuai JSON Schema."
  ].join("\n")
}

function buildUserInput(payload: GeneratePayload, feature: AiFeature, language: string) {
  const count = normalizeCount(payload.count, feature === "soal" ? 5 : 3, feature === "soal" ? 30 : 8)
  const existing = Array.isArray(payload.existing_items)
    ? payload.existing_items.map(normalizeText).filter(Boolean).slice(0, 12)
    : []
  const lines = [
    `Fitur: ${feature}`,
    `Mapel: ${normalizeText(payload.subject_title) || "-"}`,
    `Kelas: ${normalizeText(payload.class_name) || "-"}`,
    `Semester: ${normalizeText(payload.semester) || "-"}`,
    `Bahasa: ${language}`,
    `Jumlah: ${count}`,
    `Instruksi guru: ${normalizeText(payload.prompt) || "Buatkan konten pembelajaran yang relevan."}`
  ]

  if (feature === "soal") {
    lines.push(`Jenis soal: ${normalizeText(payload.question_type) || "pilihan-ganda"}`)
    lines.push(`Tahun ajaran: ${normalizeText(payload.academic_year) || "-"}`)
  } else if (existing.length) {
    lines.push(`Materi yang sudah ada: ${existing.join("; ")}`)
  }

  return lines.join("\n")
}

function extractOutputText(response: Record<string, unknown>) {
  const output = Array.isArray(response.output) ? response.output : []
  for (const item of output) {
    if (!item || typeof item !== "object") continue
    const content = Array.isArray((item as Record<string, unknown>).content)
      ? (item as Record<string, unknown>).content as unknown[]
      : []
    for (const part of content) {
      if (!part || typeof part !== "object") continue
      const row = part as Record<string, unknown>
      if (normalizeText(row.type) === "output_text") {
        const text = normalizeText(row.text)
        if (text) return text
      }
      if (normalizeText(row.type) === "refusal") {
        const refusal = normalizeText(row.refusal)
        if (refusal) throw new Error(refusal)
      }
    }
  }
  return ""
}

async function createOpenAiResponse(payload: GeneratePayload, feature: AiFeature, language: string) {
  if (!openAiApiKey) {
    throw new Error("OPENAI_API_KEY belum diset di Supabase secrets.")
  }

  const schema = feature === "soal" ? questionSchema() : materialSchema()
  const response = await fetch("https://api.openai.com/v1/responses", {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${openAiApiKey}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      model: openAiModel,
      instructions: buildInstructions(feature, language),
      input: buildUserInput(payload, feature, language),
      max_output_tokens: feature === "soal" ? 3200 : 2200,
      reasoning: { effort: "low" },
      text: {
        verbosity: "low",
        format: {
          type: "json_schema",
          name: `mim_${feature}_generation`,
          strict: true,
          schema
        }
      },
      store: false,
      metadata: {
        feature,
        guru_id: normalizeText(payload.guru_id).slice(0, 64),
        distribusi_id: normalizeText(payload.distribusi_id).slice(0, 64)
      }
    })
  })

  const raw = await response.text()
  let parsed: Record<string, unknown>
  try {
    parsed = JSON.parse(raw)
  } catch (_error) {
    throw new Error(raw || `OpenAI HTTP ${response.status}`)
  }

  if (!response.ok) {
    const error = parsed.error && typeof parsed.error === "object"
      ? normalizeText((parsed.error as Record<string, unknown>).message)
      : ""
    throw new Error(error || `OpenAI HTTP ${response.status}`)
  }

  const outputText = extractOutputText(parsed)
  if (!outputText) throw new Error("OpenAI tidak mengembalikan teks hasil.")

  let result: Record<string, unknown>
  try {
    result = JSON.parse(outputText)
  } catch (_error) {
    throw new Error("Format JSON hasil AI tidak valid.")
  }

  const usage = parsed.usage && typeof parsed.usage === "object"
    ? parsed.usage as Record<string, unknown>
    : {}

  return {
    result,
    response_id: normalizeText(parsed.id),
    usage: {
      input_tokens: Number(usage.input_tokens || 0),
      output_tokens: Number(usage.output_tokens || 0),
      total_tokens: Number(usage.total_tokens || 0)
    }
  }
}

serve(async req => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders })
  }

  if (req.method !== "POST") {
    return jsonResponse(405, { ok: false, error: "Method not allowed" })
  }

  if (!supabaseUrl || !supabaseKey) {
    return jsonResponse(500, { ok: false, error: "Supabase service role belum dikonfigurasi." })
  }

  let payload: GeneratePayload
  try {
    payload = await req.json()
  } catch (_error) {
    return jsonResponse(400, { ok: false, error: "Invalid JSON" })
  }

  const guruId = normalizeText(payload.guru_id)
  if (!guruId) {
    return jsonResponse(400, { ok: false, error: "guru_id wajib diisi" })
  }

  const action = normalizeText(payload.action || "generate").toLowerCase()
  if (action === "balance") {
    try {
      const wallet = await getWallet(guruId)
      return jsonResponse(200, { ok: true, wallet })
    } catch (error) {
      return jsonResponse(500, { ok: false, error: errorMessage(error) })
    }
  }

  const feature = normalizeFeature(payload.feature)
  const language = normalizeLanguage(payload.language)
  const distribusiId = normalizeText(payload.distribusi_id)
  const usageId = crypto.randomUUID()
  const reservedTokens = estimateChargeTokens(payload, feature)
  const metadata = {
    feature,
    model: openAiModel,
    subject_title: normalizeText(payload.subject_title),
    class_name: normalizeText(payload.class_name),
    count: normalizeCount(payload.count, feature === "soal" ? 5 : 3, feature === "soal" ? 30 : 8)
  }

  let reserveResult: Record<string, unknown>
  try {
    reserveResult = await debitTokens(guruId, reservedTokens, "usage", "ai_usage_logs", usageId, metadata)
  } catch (error) {
    return jsonResponse(500, { ok: false, error: errorMessage(error) })
  }

  if (reserveResult.ok !== true) {
    return jsonResponse(402, {
      ok: false,
      error: normalizeText(reserveResult.error) || "Saldo token AI tidak cukup.",
      wallet: reserveResult
    })
  }

  try {
    const openAiResult = await createOpenAiResponse(payload, feature, language)
    let chargedTokens = chargeFromUsage(openAiResult.usage.total_tokens)
    const adjustment = chargedTokens - reservedTokens

    let wallet: Record<string, unknown> | null = null
    if (adjustment > 0) {
      const extraDebit = await debitTokens(guruId, adjustment, "usage_adjustment", "ai_usage_logs", usageId, {
        ...metadata,
        response_id: openAiResult.response_id
      })
      if (extraDebit.ok === true) {
        wallet = extraDebit
      } else {
        chargedTokens = reservedTokens
        wallet = reserveResult
      }
    } else if (adjustment < 0) {
      wallet = await creditTokens(guruId, Math.abs(adjustment), "usage_refund", "ai_usage_logs", usageId, {
        ...metadata,
        response_id: openAiResult.response_id
      })
    } else {
      wallet = reserveResult
    }

    await logUsage({
      id: usageId,
      guru_id: guruId,
      feature,
      distribusi_id: distribusiId || null,
      model: openAiModel,
      prompt_tokens: openAiResult.usage.input_tokens,
      completion_tokens: openAiResult.usage.output_tokens,
      total_tokens: openAiResult.usage.total_tokens,
      reserved_tokens: reservedTokens,
      charged_tokens: chargedTokens,
      status: "success",
      metadata_json: {
        ...metadata,
        response_id: openAiResult.response_id,
        language
      }
    })

    return jsonResponse(200, {
      ok: true,
      feature,
      result: openAiResult.result,
      wallet,
      usage: {
        ...openAiResult.usage,
        reserved_tokens: reservedTokens,
        charged_tokens: chargedTokens
      }
    })
  } catch (error) {
    const refundWallet = await creditTokens(guruId, reservedTokens, "refund", "ai_usage_logs", usageId, metadata).catch(() => null)
    await logUsage({
      id: usageId,
      guru_id: guruId,
      feature,
      distribusi_id: distribusiId || null,
      model: openAiModel,
      reserved_tokens: reservedTokens,
      charged_tokens: 0,
      status: "error",
      error_message: errorMessage(error),
      metadata_json: metadata
    }).catch(() => null)

    return jsonResponse(500, {
      ok: false,
      error: errorMessage(error),
      wallet: refundWallet
    })
  }
})
