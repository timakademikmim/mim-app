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

function base64UrlEncode(input: Uint8Array) {
  const base64 = btoa(String.fromCharCode(...input))
  return base64.replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "")
}

function base64UrlEncodeJson(obj: Record<string, unknown>) {
  const json = JSON.stringify(obj)
  return base64UrlEncode(new TextEncoder().encode(json))
}

function pemToArrayBuffer(pem: string) {
  const cleaned = pem
    .replace("-----BEGIN PRIVATE KEY-----", "")
    .replace("-----END PRIVATE KEY-----", "")
    .replace(/\s+/g, "")
  const binary = atob(cleaned)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i += 1) {
    bytes[i] = binary.charCodeAt(i)
  }
  return bytes.buffer
}

async function getAccessToken(serviceAccount: {
  client_email: string
  private_key: string
  project_id: string
}) {
  const now = Math.floor(Date.now() / 1000)
  const header = { alg: "RS256", typ: "JWT" }
  const payload = {
    iss: serviceAccount.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600
  }

  const unsignedJwt = `${base64UrlEncodeJson(header)}.${base64UrlEncodeJson(payload)}`
  const keyData = pemToArrayBuffer(serviceAccount.private_key)
  const cryptoKey = await crypto.subtle.importKey(
    "pkcs8",
    keyData,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  )
  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    cryptoKey,
    new TextEncoder().encode(unsignedJwt)
  )
  const signedJwt = `${unsignedJwt}.${base64UrlEncode(new Uint8Array(signature))}`

  const tokenRes = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: signedJwt
    })
  })

  if (!tokenRes.ok) {
    const text = await tokenRes.text()
    throw new Error(`OAuth gagal: ${tokenRes.status} ${text}`)
  }
  const tokenJson = await tokenRes.json()
  return { access_token: tokenJson.access_token as string, project_id: serviceAccount.project_id }
}

serve(async req => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders })
  }
  if (req.method !== "POST") {
    return jsonResponse(405, { ok: false, error: "Method not allowed" })
  }

  let payload: {
    token?: string
    user_id?: string
    title?: string
    body?: string
    data?: Record<string, string>
  }

  try {
    payload = await req.json()
  } catch (_error) {
    return jsonResponse(400, { ok: false, error: "Invalid JSON" })
  }

  const serviceAccountRaw = Deno.env.get("FCM_SERVICE_ACCOUNT_JSON") || ""
  if (!serviceAccountRaw) {
    return jsonResponse(500, { ok: false, error: "FCM_SERVICE_ACCOUNT_JSON belum diset" })
  }

  const serviceAccount = JSON.parse(serviceAccountRaw)
  const { access_token, project_id } = await getAccessToken(serviceAccount)
  const title = String(payload.title || "Pesan baru")
  const body = String(payload.body || "Anda menerima pesan baru.")
  const rawData = payload.data || {}
  const data: Record<string, string> = {}
  for (const [key, value] of Object.entries(rawData)) {
    data[key] = String(value ?? "")
  }
  if (!data.open_chat_thread_id && data.thread_id) {
    data.open_chat_thread_id = data.thread_id
  }
  if (!data.thread_id && data.open_chat_thread_id) {
    data.thread_id = data.open_chat_thread_id
  }
  if (!data.route && data.open_chat_thread_id) {
    data.route = "chat"
  }
  const targetUserId = String(payload.user_id || "").trim()
  if (targetUserId && !data.notify_user_id) {
    data.notify_user_id = targetUserId
  }

  let tokens: string[] = []
  const explicitToken = String(payload.token || "").trim()
  if (explicitToken) {
    tokens = [explicitToken]
  } else {
    const userId = targetUserId
    if (!userId) {
      return jsonResponse(400, { ok: false, error: "token atau user_id wajib diisi" })
    }
    const { data: rows, error } = await supabase
      .from("push_tokens")
      .select("token")
      .eq("user_id", userId)
    if (error) {
      return jsonResponse(500, { ok: false, error: error.message })
    }
    tokens = (rows || []).map(row => String(row.token || "").trim()).filter(Boolean)
  }

  if (tokens.length === 0) {
    return jsonResponse(200, { ok: false, sent: 0, message: "Token kosong" })
  }

  const results: Array<{ token: string; ok: boolean; error?: string }> = []

  for (const token of tokens) {
    const payloadData: Record<string, string> = {
      ...data,
      title,
      body
    }
    const messageBody = {
      message: {
        token,
        data: payloadData,
        android: {
          priority: "HIGH"
        }
      }
    }
    const fcmRes = await fetch(`https://fcm.googleapis.com/v1/projects/${project_id}/messages:send`, {
      method: "POST",
      headers: {
        authorization: `Bearer ${access_token}`,
        "content-type": "application/json; charset=utf-8"
      },
      body: JSON.stringify(messageBody)
    })
    if (!fcmRes.ok) {
      const text = await fcmRes.text()
      results.push({ token, ok: false, error: text })
    } else {
      results.push({ token, ok: true })
    }
  }

  const sent = results.filter(item => item.ok).length
  return jsonResponse(200, { ok: sent > 0, sent, results })
})
