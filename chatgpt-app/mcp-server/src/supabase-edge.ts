import type { ChatGptExamMcpConfig } from "./config.js"

export async function callSupabaseFunction<TPayload extends Record<string, unknown>, TResult>(
  config: ChatGptExamMcpConfig,
  functionName: string,
  payload: TPayload,
  options: {
    sharedSecret?: string
  } = {}
): Promise<TResult> {
  const response = await fetch(`${config.supabaseFunctionsBaseUrl}/${functionName}`, {
    method: "POST",
    headers: {
      "content-type": "application/json",
      "apikey": config.supabaseAnonKey,
      "authorization": `Bearer ${config.supabaseAnonKey}`
    },
    body: JSON.stringify({
      shared_secret: String(options.sharedSecret || config.sharedSecret || "").trim(),
      ...payload
    })
  })

  const text = await response.text()
  let body: unknown = null
  try {
    body = text ? JSON.parse(text) : null
  } catch (_error) {
    throw new Error(`${functionName} mengembalikan respons non-JSON: ${text}`)
  }

  if (!response.ok) {
    const message =
      typeof body === "object" && body && "error" in body
        ? String((body as Record<string, unknown>).error || "Unknown error")
        : `HTTP ${response.status}`
    throw new Error(`${functionName} gagal: ${message}`)
  }

  return body as TResult
}
