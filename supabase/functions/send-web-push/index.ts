import { serve } from "https://deno.land/std@0.224.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"
import webpush from "npm:web-push@3.6.7"

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

const publicKey = String(Deno.env.get("WEB_PUSH_VAPID_PUBLIC_KEY") || "").trim()
const privateKey = String(Deno.env.get("WEB_PUSH_VAPID_PRIVATE_KEY") || "").trim()
const subject = String(Deno.env.get("WEB_PUSH_VAPID_SUBJECT") || "mailto:admin@example.com").trim()

if (publicKey && privateKey) {
  webpush.setVapidDetails(subject, publicKey, privateKey)
}

const supabase = createClient(supabaseUrl, supabaseKey)

type PushPayload = {
  user_id?: string
  user_ids?: string[]
  title?: string
  body?: string
  route?: string
  scope?: string
  event_type?: string
  thread_id?: string
  tag?: string
  dedupe_key?: string
  store_event?: boolean
  source_user_id?: string
  data?: Record<string, unknown>
}

type SubscriptionRow = {
  id: string
  user_id: string
  endpoint: string
  p256dh: string
  auth: string
  role: string | null
}

function uniqueStrings(values: Array<string | null | undefined>) {
  return [...new Set(values.map(item => String(item || "").trim()).filter(Boolean))]
}

function sanitizeText(value: unknown) {
  return String(value ?? "").trim()
}

function buildTargetUrl(role: string, route: string, threadId: string) {
  const pagePath = role === "admin"
    ? "admin.html"
    : role === "muhaffiz"
      ? "muhaffiz.html"
      : role === "musyrif"
        ? "musyrif.html"
        : "guru.html"

  const params = new URLSearchParams()
  if (route || threadId) {
    params.set("mim_notify", "1")
    if (route) params.set("mim_route", route)
    if (threadId) params.set("mim_thread", threadId)
  }
  const qs = params.toString()
  return qs ? `${pagePath}?${qs}` : pagePath
}

function buildNotificationEventRows(rows: SubscriptionRow[], payload: PushPayload) {
  if (payload.store_event === false) return []
  const nowIso = new Date().toISOString()
  const route = sanitizeText(payload.route).toLowerCase()
  const threadId = sanitizeText(payload.thread_id)
  const scope = sanitizeText(payload.scope || "general").toLowerCase() || "general"
  const eventType = sanitizeText(payload.event_type || "info").toLowerCase() || "info"
  const dedupeKey = sanitizeText(payload.dedupe_key)
  const sourceUserId = sanitizeText(payload.source_user_id) || null
  const title = sanitizeText(payload.title || "Notifikasi baru") || "Notifikasi baru"
  const body = sanitizeText(payload.body)
  const payloadJson = payload.data && typeof payload.data === "object" ? payload.data : {}

  return uniqueStrings(rows.map(row => row.user_id)).map(userId => ({
    user_id: userId,
    scope,
    event_type: eventType,
    title,
    body,
    route: route || null,
    thread_id: threadId || null,
    payload_json: payloadJson,
    source_user_id: sourceUserId,
    dedupe_key: dedupeKey || null,
    created_at: nowIso
  }))
}

async function markSubscriptionInactive(ids: string[]) {
  const targetIds = uniqueStrings(ids)
  if (!targetIds.length) return
  await supabase
    .from("web_push_subscriptions")
    .update({
      is_active: false,
      updated_at: new Date().toISOString()
    })
    .in("id", targetIds)
}

serve(async req => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders })
  }

  if (req.method !== "POST") {
    return jsonResponse(405, { ok: false, error: "Method not allowed" })
  }

  if (!publicKey || !privateKey) {
    return jsonResponse(500, { ok: false, error: "VAPID key belum diset" })
  }

  let payload: PushPayload
  try {
    payload = await req.json()
  } catch (_error) {
    return jsonResponse(400, { ok: false, error: "Invalid JSON" })
  }

  const userIds = uniqueStrings([
    ...(Array.isArray(payload.user_ids) ? payload.user_ids : []),
    payload.user_id
  ])
  if (!userIds.length) {
    return jsonResponse(400, { ok: false, error: "user_id atau user_ids wajib diisi" })
  }

  const { data: subscriptionRows, error: subscriptionError } = await supabase
    .from("web_push_subscriptions")
    .select("id, user_id, endpoint, p256dh, auth, role")
    .in("user_id", userIds)
    .eq("is_active", true)

  if (subscriptionError) {
    return jsonResponse(500, { ok: false, error: subscriptionError.message })
  }

  const rows = (subscriptionRows || []) as SubscriptionRow[]
  if (!rows.length) {
    return jsonResponse(200, { ok: false, sent: 0, reason: "subscription kosong" })
  }

  const title = sanitizeText(payload.title || "Notifikasi baru") || "Notifikasi baru"
  const body = sanitizeText(payload.body)
  const route = sanitizeText(payload.route).toLowerCase()
  const threadId = sanitizeText(payload.thread_id)
  const scope = sanitizeText(payload.scope || "general").toLowerCase() || "general"
  const eventType = sanitizeText(payload.event_type || "info").toLowerCase() || "info"
  const tag = sanitizeText(payload.tag) || `mim-${scope}`
  const rawData = payload.data && typeof payload.data === "object" ? payload.data : {}
  const inactiveIds: string[] = []
  const results: Array<{ user_id: string; subscription_id: string; ok: boolean; error?: string }> = []

  for (const row of rows) {
    const role = sanitizeText(row.role || "guru").toLowerCase() || "guru"
    const targetUrl = buildTargetUrl(role, route, threadId)
    const data: Record<string, string> = {}
    for (const [key, value] of Object.entries(rawData)) {
      data[String(key || "").trim()] = String(value ?? "")
    }
    if (route) data.route = route
    if (threadId) data.thread_id = threadId
    if (!data.notify_user_id) data.notify_user_id = sanitizeText(row.user_id)

    const notificationPayload = {
      title,
      body,
      targetUrl,
      scope,
      route,
      thread_id: threadId,
      event_type: eventType,
      tag,
      user_id: sanitizeText(row.user_id),
      options: {
        scope,
        route,
        threadId,
        userId: sanitizeText(row.user_id),
        tag
      },
      data
    }

    try {
      await webpush.sendNotification(
        {
          endpoint: row.endpoint,
          keys: {
            p256dh: row.p256dh,
            auth: row.auth
          }
        },
        JSON.stringify(notificationPayload)
      )
      results.push({ user_id: row.user_id, subscription_id: row.id, ok: true })
    } catch (error) {
      const statusCode = Number((error as { statusCode?: number })?.statusCode || 0)
      const message = (error as { body?: string; message?: string })?.body ||
        (error as { message?: string })?.message ||
        "Push gagal"
      if (statusCode === 404 || statusCode === 410) {
        inactiveIds.push(row.id)
      }
      results.push({
        user_id: row.user_id,
        subscription_id: row.id,
        ok: false,
        error: String(message)
      })
    }
  }

  if (inactiveIds.length) {
    await markSubscriptionInactive(inactiveIds)
  }

  const eventRows = buildNotificationEventRows(rows, payload)
  if (eventRows.length) {
    const { error: insertError } = await supabase
      .from("notification_events")
      .insert(eventRows)
    if (insertError) {
      const message = String(insertError.message || "").toLowerCase()
      if (!message.includes("duplicate key")) {
        console.warn("Gagal menyimpan notification_events:", insertError.message)
      }
    }
  }

  const sent = results.filter(item => item.ok).length
  return jsonResponse(200, { ok: sent > 0, sent, results })
})
