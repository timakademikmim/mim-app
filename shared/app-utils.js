;(function initSharedAppUtils() {
  if (window.__sharedAppUtilsReady) return

  function appEscapeHtml(value) {
    return String(value ?? '')
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#39;')
  }

  window.appEscapeHtml = appEscapeHtml
  const DESKTOP_OFFLINE_DB_NAME = 'mim_desktop_offline_cache'
  const DESKTOP_OFFLINE_STORE = 'http_cache'
  const DESKTOP_OFFLINE_MAX_AGE_MS = 7 * 24 * 60 * 60 * 1000

  function isDesktopRuntime() {
    return !!(window.__TAURI_INTERNALS__ || window.__TAURI__)
  }

  function openOfflineDb() {
    return new Promise((resolve, reject) => {
      if (!('indexedDB' in window)) {
        reject(new Error('IndexedDB tidak tersedia'))
        return
      }
      const req = indexedDB.open(DESKTOP_OFFLINE_DB_NAME, 1)
      req.onupgradeneeded = () => {
        const db = req.result
        if (!db.objectStoreNames.contains(DESKTOP_OFFLINE_STORE)) {
          db.createObjectStore(DESKTOP_OFFLINE_STORE, { keyPath: 'key' })
        }
      }
      req.onsuccess = () => resolve(req.result)
      req.onerror = () => reject(req.error || new Error('Gagal membuka IndexedDB'))
    })
  }

  async function getOfflineCacheItem(key) {
    try {
      const db = await openOfflineDb()
      return await new Promise((resolve, reject) => {
        const tx = db.transaction(DESKTOP_OFFLINE_STORE, 'readonly')
        const store = tx.objectStore(DESKTOP_OFFLINE_STORE)
        const req = store.get(key)
        req.onsuccess = () => resolve(req.result || null)
        req.onerror = () => reject(req.error || new Error('Gagal membaca cache'))
      })
    } catch (_error) {
      return null
    }
  }

  async function setOfflineCacheItem(record) {
    try {
      const db = await openOfflineDb()
      await new Promise((resolve, reject) => {
        const tx = db.transaction(DESKTOP_OFFLINE_STORE, 'readwrite')
        const store = tx.objectStore(DESKTOP_OFFLINE_STORE)
        const req = store.put(record)
        req.onsuccess = () => resolve()
        req.onerror = () => reject(req.error || new Error('Gagal menyimpan cache'))
      })
    } catch (_error) {}
  }

  function makeOfflineResponse(message, status = 503) {
    const body = JSON.stringify({ message, offline: true })
    return new Response(body, {
      status,
      headers: { 'content-type': 'application/json; charset=utf-8' }
    })
  }

  async function fetchWithTimeout(fetcher, request, timeoutMs = 2500) {
    const controller = new AbortController()
    const timer = setTimeout(() => controller.abort(), timeoutMs)
    try {
      return await fetcher(new Request(request, { signal: controller.signal, cache: 'no-store' }))
    } finally {
      clearTimeout(timer)
    }
  }

  function isSupabaseDataEndpoint(urlText) {
    const text = String(urlText || '')
    return text.includes('/rest/v1/') || text.includes('/storage/v1/')
  }

  function buildCacheKey(request) {
    const url = new URL(request.url)
    return `${request.method.toUpperCase()}::${url.origin}${url.pathname}${url.search}`
  }

  async function responseToCacheRecord(key, response) {
    const clone = response.clone()
    const text = await clone.text()
    return {
      key,
      status: response.status,
      statusText: response.statusText,
      contentType: response.headers.get('content-type') || 'application/json; charset=utf-8',
      bodyText: text,
      cachedAt: Date.now()
    }
  }

  function cacheRecordToResponse(record) {
    const headers = { 'content-type': record.contentType || 'application/json; charset=utf-8', 'x-mim-offline-cache': 'hit' }
    return new Response(record.bodyText || '', {
      status: Number(record.status || 200),
      statusText: String(record.statusText || 'OK'),
      headers
    })
  }

  function createDesktopAwareFetch(baseFetch) {
    return async function desktopAwareFetch(input, init) {
      const req = new Request(input, init)
      const method = String(req.method || 'GET').toUpperCase()
      const isRead = method === 'GET'
      const isSupaData = isSupabaseDataEndpoint(req.url)
      if (!isDesktopRuntime() || !isSupaData) {
        return baseFetch(req)
      }

      const key = buildCacheKey(req)
      const online = navigator.onLine

      try {
        let response
        if (online) {
          response = await baseFetch(req)
        } else {
          // Di desktop kadang navigator.onLine terlambat update saat jaringan sudah kembali.
          // Coba reconnect singkat dulu sebelum memutuskan benar-benar offline.
          response = await fetchWithTimeout(baseFetch, req, 2500)
        }
        if (isRead && response.ok) {
          const cacheRecord = await responseToCacheRecord(key, response)
          await setOfflineCacheItem(cacheRecord)
        }
        return response
      } catch (error) {
        if (!isRead) {
          return makeOfflineResponse('Mode offline: perubahan data dinonaktifkan. Silakan coba lagi setelah jaringan stabil.')
        }
        if (isRead) {
          const cached = await getOfflineCacheItem(key)
          if (cached) return cacheRecordToResponse(cached)
        }
        return makeOfflineResponse('Data belum tersedia offline. Buka data ini saat online agar tersimpan di cache.')
      }
    }
  }

  function createDesktopAwareSupabaseClient(url, key, options = {}) {
    const supa = window.supabase
    if (!supa || typeof supa.createClient !== 'function') {
      throw new Error('Supabase client belum tersedia di window.')
    }
    const globalOptions = options.global || {}
    const baseFetch = globalOptions.fetch || window.fetch.bind(window)
    const mergedOptions = {
      ...options,
      global: {
        ...globalOptions,
        fetch: createDesktopAwareFetch(baseFetch)
      }
    }
    return supa.createClient(url, key, mergedOptions)
  }

  window.createDesktopAwareSupabaseClient = createDesktopAwareSupabaseClient
  window.__sharedAppUtilsReady = true
})()
