self.addEventListener('install', event => {
  event.waitUntil(self.skipWaiting())
})

self.addEventListener('activate', event => {
  event.waitUntil(self.clients.claim())
})

function normalizeNotificationTargetUrl(rawUrl) {
  const urlText = String(rawUrl || '').trim()
  if (!urlText) return ''
  try {
    return new URL(urlText, self.location.origin).toString()
  } catch (_error) {
    return ''
  }
}

function normalizePushPayload(rawPayload) {
  const payload = rawPayload && typeof rawPayload === 'object' ? rawPayload : {}
  const options = payload && payload.options && typeof payload.options === 'object'
    ? payload.options
    : {
        route: String(payload.route || '').trim().toLowerCase(),
        threadId: String(payload.thread_id || payload.threadId || '').trim(),
        scope: String(payload.scope || '').trim().toLowerCase(),
        userId: String(payload.user_id || payload.userId || '').trim(),
        tag: String(payload.tag || '').trim()
      }
  return {
    title: String(payload.title || 'Notifikasi').trim() || 'Notifikasi',
    body: String(payload.body || '').trim(),
    icon: String(payload.icon || '').trim(),
    badge: String(payload.badge || '').trim(),
    targetUrl: normalizeNotificationTargetUrl(payload.targetUrl || payload.url || ''),
    options
  }
}

self.addEventListener('push', event => {
  event.waitUntil((async () => {
    let rawPayload = {}
    try {
      rawPayload = event.data ? await event.data.json() : {}
    } catch (_error) {
      try {
        rawPayload = { body: String(event.data?.text() || '').trim() }
      } catch (__error) {
        rawPayload = {}
      }
    }
    const payload = normalizePushPayload(rawPayload)
    await self.registration.showNotification(payload.title, {
      body: payload.body || '',
      tag: String(payload.options?.tag || '').trim() || 'mim-notification',
      icon: payload.icon || undefined,
      badge: payload.badge || payload.icon || undefined,
      data: {
        type: 'mim-notification',
        options: payload.options,
        targetUrl: payload.targetUrl
      }
    })
  })())
})

self.addEventListener('notificationclick', event => {
  const notification = event.notification
  const payload = notification && notification.data && typeof notification.data === 'object'
    ? notification.data
    : {}
  const options = payload && payload.options && typeof payload.options === 'object'
    ? payload.options
    : {}
  const targetUrl = normalizeNotificationTargetUrl(payload.targetUrl || payload.url || '')
  if (notification && typeof notification.close === 'function') {
    notification.close()
  }
  event.waitUntil((async () => {
    const clientList = await self.clients.matchAll({
      type: 'window',
      includeUncontrolled: true
    })
    const preferredClient = clientList.find(client => {
      if (!client || !client.url) return false
      if (!targetUrl) return true
      try {
        return new URL(client.url).origin === new URL(targetUrl).origin
      } catch (_error) {
        return false
      }
    }) || null

    if (preferredClient) {
      try {
        await preferredClient.focus()
      } catch (_error) {}
      try {
        preferredClient.postMessage({
          type: 'mim-notification-click',
          options
        })
      } catch (_error) {}
      return
    }

    const fallbackUrl = targetUrl || String(self.registration && self.registration.scope || self.location.origin || '').trim()
    if (fallbackUrl && self.clients.openWindow) {
      await self.clients.openWindow(fallbackUrl)
    }
  })())
})
