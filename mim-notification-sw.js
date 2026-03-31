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
