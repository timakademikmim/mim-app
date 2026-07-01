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
    return text.includes('/rest/v1/') || text.includes('/storage/v1/') || text.includes('/functions/v1/')
  }

  function buildCacheKey(request) {
    const url = new URL(request.url)
    const tenantId = String(localStorage.getItem('login_tenant_id') || '').trim()
    const loginId = String(localStorage.getItem('login_id') || '').trim().toLowerCase()
    const scope = tenantId && loginId ? `${tenantId}:${loginId}` : 'anonymous'
    return `${scope}::${request.method.toUpperCase()}::${url.origin}${url.pathname}${url.search}`
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

  function createDesktopAwareFetch(baseFetch, publicAnonKey) {
    return async function desktopAwareFetch(input, init) {
      const req = new Request(input, init)
      const method = String(req.method || 'GET').toUpperCase()
      const isRead = method === 'GET'
      const isSupaData = isSupabaseDataEndpoint(req.url)
      const authMode = String(localStorage.getItem('login_auth_mode') || '').trim().toLowerCase()
      const authorization = String(req.headers.get('authorization') || '')
      const bearerToken = authorization.replace(/^bearer\s+/i, '').trim()
      if (isSupaData && authMode === 'auth' && (!bearerToken || bearerToken === publicAnonKey)) {
        return makeOfflineResponse('Sesi login tidak valid. Silakan login kembali.', 401)
      }
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
        fetch: createDesktopAwareFetch(baseFetch, key)
      }
    }
    return supa.createClient(url, key, mergedOptions)
  }

  function getSharedSupabaseClient(url, key, options = {}) {
    if (window.mimSupabaseClient) return window.mimSupabaseClient
    window.mimSupabaseClient = createDesktopAwareSupabaseClient(url, key, options)
    return window.mimSupabaseClient
  }

  function getActiveTenantId() {
    const tenantId = String(localStorage.getItem('login_tenant_id') || '').trim().toLowerCase()
    return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/.test(tenantId)
      ? tenantId
      : ''
  }

  function buildTenantStoragePath(relativePath) {
    const cleanPath = String(relativePath || '')
      .replaceAll('\\', '/')
      .split('/')
      .map(part => part.trim())
      .filter(part => part && part !== '.' && part !== '..')
      .join('/')
    if (!cleanPath) throw new Error('Path file Storage tidak valid.')

    const tenantId = getActiveTenantId()
    if (!tenantId) throw new Error('Unit login tidak valid. Silakan login kembali.')
    return `${tenantId}/${cleanPath}`
  }

  function buildStorageObjectReference(bucket, path) {
    const safeBucket = String(bucket || '').trim()
    const safePath = String(path || '').replace(/^\/+/, '').trim()
    if (!safeBucket || !safePath) throw new Error('Referensi file Storage tidak valid.')
    return `storage://${safeBucket}/${safePath}`
  }

  function parseStorageObjectReference(value) {
    const source = String(value || '').trim()
    if (!source) return null
    if (source.startsWith('storage://')) {
      const remainder = source.slice('storage://'.length)
      const separator = remainder.indexOf('/')
      if (separator <= 0) return null
      return { bucket: remainder.slice(0, separator), path: remainder.slice(separator + 1) }
    }
    try {
      const url = new URL(source, window.location.href)
      const match = url.pathname.match(/\/storage\/v1\/object\/(?:public|sign|authenticated)\/([^/]+)\/(.+)$/)
      if (!match) return null
      return { bucket: decodeURIComponent(match[1]), path: decodeURIComponent(match[2]) }
    } catch (_error) {
      return null
    }
  }

  async function createSignedStorageUrl(bucket, path, expiresIn = 604800) {
    const client = window.mimSupabaseClient
    if (!client) throw new Error('Sesi Storage belum siap.')
    const { data, error } = await client.storage
      .from(String(bucket || '').trim())
      .createSignedUrl(String(path || '').replace(/^\/+/, ''), Math.max(60, Number(expiresIn || 0)))
    if (error) throw error
    const signedUrl = String(data?.signedUrl || data?.signedURL || '').trim()
    if (!signedUrl) throw new Error('Link aman file tidak tersedia.')
    return signedUrl
  }

  async function resolveStorageObjectUrl(value, expiresIn = 3600) {
    const source = String(value || '').trim()
    const reference = parseStorageObjectReference(source)
    if (!reference) return source
    return createSignedStorageUrl(reference.bucket, reference.path, expiresIn)
  }

  window.createDesktopAwareSupabaseClient = createDesktopAwareSupabaseClient
  window.getSharedSupabaseClient = getSharedSupabaseClient
  window.getActiveTenantId = getActiveTenantId
  window.buildTenantStoragePath = buildTenantStoragePath
  window.buildStorageObjectReference = buildStorageObjectReference
  window.parseStorageObjectReference = parseStorageObjectReference
  window.createSignedStorageUrl = createSignedStorageUrl
  window.resolveStorageObjectUrl = resolveStorageObjectUrl
  window.__sharedAppUtilsReady = true
})()
