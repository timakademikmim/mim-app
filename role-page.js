const id = localStorage.getItem('login_id')
const requiredRole = String(document.body?.dataset?.role || '').trim().toLowerCase()
const ROLE_PAGE_MAP = {
  admin: 'admin.html',
  guru: 'guru.html',
  muhaffiz: 'muhaffiz.html',
  musyrif: 'musyrif.html'
}

let roles = []
try {
  roles = JSON.parse(localStorage.getItem('login_roles') || '[]')
} catch (_error) {
  roles = []
}

if (!Array.isArray(roles) || roles.length === 0) {
  const singleRole = localStorage.getItem('login_role')
  roles = singleRole ? [singleRole] : []
}

roles = roles
  .map(v => String(v || '').trim().toLowerCase())
  .filter(Boolean)

if (!id || roles.length === 0) {
  location.replace('index.html')
}

if (requiredRole && !roles.includes(requiredRole)) {
  location.replace('index.html')
}

function rememberLastOpenPage() {
  try {
    const path = String(window.location.pathname || '')
      .replace(/\\/g, '/')
      .split('/')
      .filter(Boolean)
      .slice(-2)
      .join('/')
    if (!path || path === 'index.html') return
    localStorage.setItem('last_open_page', path)
  } catch (_error) {}
}
rememberLastOpenPage()

const PREVIEW_PLATFORM = (() => {
  try {
    const raw = new URLSearchParams(window.location.search || '').get('preview')
    const value = String(raw || '').trim().toLowerCase()
    if (value === 'android' || value === 'desktop' || value === 'web') return value
  } catch (_error) {}
  return ''
})()

function hasTauriRuntime() {
  return !!(window.__TAURI_INTERNALS__ || window.__TAURI__)
}

const FCM_FUNCTION_BASE = 'https://optucpelkueqmlhwlbej.supabase.co/functions/v1'
const CHAT_OPEN_STORAGE_KEY = 'chat_open_thread_id'

function isAndroidRuntime() {
  return hasTauriRuntime() && /android/i.test(String(navigator.userAgent || ''))
}

function getOrCreateDeviceId() {
  try {
    const existing = localStorage.getItem('mim_device_id')
    if (existing) return existing
    const next = (crypto?.randomUUID ? crypto.randomUUID() : `mim-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`)
    localStorage.setItem('mim_device_id', next)
    return next
  } catch (_error) {
    return ''
  }
}

async function getAppVersionSafe() {
  try {
    if (!hasTauriRuntime()) return ''
    const invoke = window.__TAURI__?.core?.invoke || window.__TAURI__?.invoke
    if (typeof invoke !== 'function') return ''
    const version = await invoke('get_app_version')
    return String(version || '').trim()
  } catch (_error) {
    return ''
  }
}

async function registerFcmToken(token) {
  const clean = String(token || '').trim()
  if (!clean || !isAndroidRuntime() || !id) return
  try {
    const cached = localStorage.getItem('mim_fcm_token')
    if (cached === clean) return
    const payload = {
      user_id: String(id || '').trim(),
      token: clean,
      platform: 'android',
      device_id: getOrCreateDeviceId(),
      app_version: await getAppVersionSafe()
    }
    await fetch(`${FCM_FUNCTION_BASE}/register-push-token`, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify(payload)
    })
    localStorage.setItem('mim_fcm_token', clean)
  } catch (error) {
    console.warn('Gagal register FCM token:', error)
  }
}

if (isAndroidRuntime()) {
  window.addEventListener('mim-fcm-token', event => {
    registerFcmToken(event?.detail?.token)
  })
  if (window.__mimLastFcmToken) {
    registerFcmToken(window.__mimLastFcmToken)
  }
}

let chatOpenRetryCount = 0
const MAX_CHAT_OPEN_RETRY = 8
const CHAT_OPEN_RETRY_DELAY = 450

function tryOpenChatRoute() {
  const role = getActiveRole()
  if (role === 'admin' && typeof window.loadPage === 'function') {
    window.loadPage('chat')
    return true
  }
  if (role === 'guru' && typeof window.loadGuruPage === 'function') {
    window.loadGuruPage('chat')
    return true
  }
  if (role === 'muhaffiz' && typeof window.loadMuhaffizPage === 'function') {
    window.loadMuhaffizPage('chat')
    return true
  }
  if (role === 'musyrif' && typeof window.loadMusyrifPage === 'function') {
    window.loadMusyrifPage('chat')
    return true
  }
  if (typeof window.loadPage === 'function') {
    window.loadPage('chat')
    return true
  }
  return false
}

function scheduleChatOpenRetry(threadId) {
  if (chatOpenRetryCount >= MAX_CHAT_OPEN_RETRY) return false
  chatOpenRetryCount += 1
  window.setTimeout(() => {
    openChatPageFromNotification(threadId)
  }, CHAT_OPEN_RETRY_DELAY)
  return true
}

function isAndroidPlatform() {
  if (PREVIEW_PLATFORM === 'android') return true
  if (PREVIEW_PLATFORM === 'desktop' || PREVIEW_PLATFORM === 'web') return false
  return hasTauriRuntime() && /android/i.test(String(navigator.userAgent || ''))
}

function isDesktopPlatform() {
  if (PREVIEW_PLATFORM === 'desktop') return true
  if (PREVIEW_PLATFORM === 'android' || PREVIEW_PLATFORM === 'web') return false
  return hasTauriRuntime() && !/android/i.test(String(navigator.userAgent || ''))
}

function isWebPlatform() {
  if (PREVIEW_PLATFORM === 'web') return true
  if (PREVIEW_PLATFORM === 'android' || PREVIEW_PLATFORM === 'desktop') return false
  return !hasTauriRuntime()
}

function normalizeVersionText(version) {
  return String(version || '').trim().replace(/^v/i, '')
}

function compareVersionText(a, b) {
  const pa = normalizeVersionText(a).split('.').map(n => parseInt(n, 10) || 0)
  const pb = normalizeVersionText(b).split('.').map(n => parseInt(n, 10) || 0)
  const maxLen = Math.max(pa.length, pb.length)
  for (let i = 0; i < maxLen; i += 1) {
    const ai = pa[i] || 0
    const bi = pb[i] || 0
    if (ai > bi) return 1
    if (ai < bi) return -1
  }
  return 0
}

async function getMobileCurrentVersionText() {
  const candidates = []
  try {
    const v = await invokeTauriCommand('get_app_version', {})
    const n = normalizeVersionText(v)
    if (n) candidates.push(n)
  } catch (_error) {}
  try {
    if (window.__TAURI__?.app?.getVersion) {
      const v = await window.__TAURI__.app.getVersion()
      const n = normalizeVersionText(v)
      if (n) candidates.push(n)
    }
  } catch (_error) {}
  try {
    const stored = normalizeVersionText(localStorage.getItem('mobile_app_version') || '')
    if (stored) candidates.push(stored)
  } catch (_error) {}
  if (!candidates.length) return '0.0.0'
  return candidates.sort((a, b) => compareVersionText(b, a))[0] || '0.0.0'
}

async function fetchLatestAndroidReleaseMeta() {
  const fallback = {
    version: '',
    notes: '',
    apkUrl: '',
    aabUrl: ''
  }
  try {
    const res = await fetch('https://api.github.com/repos/timakademikmim/mim-app/releases?per_page=30', {
      cache: 'no-store',
      headers: { Accept: 'application/vnd.github+json' }
    })
    if (!res.ok) return fallback
    const releases = await res.json()
    if (!Array.isArray(releases)) return fallback

    const androidReleases = releases
      .filter(item => String(item?.tag_name || '').toLowerCase().startsWith('android-v'))
      .map(item => {
        const tag = String(item?.tag_name || '').trim()
        return {
          raw: item,
          tag,
          version: normalizeVersionText(tag.replace(/^android-v/i, ''))
        }
      })
      .filter(item => item.version)

    if (androidReleases.length === 0) return fallback

    androidReleases.sort((a, b) => {
      const byVersion = compareVersionText(b.version, a.version)
      if (byVersion !== 0) return byVersion
      const aTime = Date.parse(String(a.raw?.published_at || a.raw?.created_at || '')) || 0
      const bTime = Date.parse(String(b.raw?.published_at || b.raw?.created_at || '')) || 0
      return bTime - aTime
    })

    const androidRelease = androidReleases[0].raw

    const tag = String(androidRelease.tag_name || '').trim()
    const version = normalizeVersionText(tag.replace(/^android-v/i, ''))
    const notes = String(androidRelease.body || '').trim()
    const assets = Array.isArray(androidRelease.assets) ? androidRelease.assets : []

    const apkAsset = assets.find(asset => {
      const name = String(asset?.name || '').toLowerCase()
      return name.endsWith('.apk')
    })
    const aabAsset = assets.find(asset => {
      const name = String(asset?.name || '').toLowerCase()
      return name.endsWith('.aab')
    })

    return {
      version,
      notes,
      apkUrl: String(apkAsset?.browser_download_url || '').trim(),
      aabUrl: String(aabAsset?.browser_download_url || '').trim()
    }
  } catch (_error) {
    return fallback
  }
}

function applyPlatformUiSkin() {
  const body = document.body
  if (!body) return

  body.classList.remove('platform-android', 'platform-desktop', 'platform-web')
  if (isAndroidPlatform()) body.classList.add('platform-android')
  else if (isDesktopPlatform()) body.classList.add('platform-desktop')
  else body.classList.add('platform-web')

  if (!isAndroidPlatform()) return
  if (document.getElementById('android-ui-css')) return

  const link = document.createElement('link')
  link.id = 'android-ui-css'
  link.rel = 'stylesheet'
  link.href = 'android-ui.css?v=20260311-android-laporan-bulanan-01'
  document.head.appendChild(link)

  ensureAndroidBottomNav()
  ensureAndroidSidebarDrawer()
  ensureGlobalCalendarAutoClose()
  showAndroidWelcomeScreen()
}

function showAndroidWelcomeScreen() {
  if (!isAndroidPlatform()) return
  if (!document.body) return
  if (sessionStorage.getItem('android_welcome_seen') === '1') {
    document.documentElement.classList.remove('android-preboot')
    return
  }
  sessionStorage.setItem('android_welcome_seen', '1')
  document.body.classList.add('android-welcome-active')

  if (!document.getElementById('android-welcome-style')) {
    const style = document.createElement('style')
    style.id = 'android-welcome-style'
    style.textContent = `
      body.android-welcome-active .layout {
        opacity: 0;
        pointer-events: none;
      }
      body.android-welcome-active #android-bottom-nav {
        opacity: 0;
        pointer-events: none;
      }
      .android-welcome-screen {
        position: fixed;
        inset: 0;
        z-index: 13000;
        display: flex;
        align-items: center;
        justify-content: center;
        background: radial-gradient(560px 360px at 50% 20%, rgba(43, 140, 255, 0.18), transparent 60%), #f4f7fb;
        opacity: 0;
        pointer-events: none;
        transition: opacity 280ms ease;
      }
      .android-welcome-screen.show {
        opacity: 1;
      }
      .android-welcome-logo {
        width: 116px;
        height: 116px;
        object-fit: contain;
      }
    `
    document.head.appendChild(style)
  }

  let overlay = document.getElementById('android-welcome-screen')
  if (!overlay) {
    overlay = document.createElement('div')
    overlay.id = 'android-welcome-screen'
    overlay.className = 'android-welcome-screen'
    overlay.innerHTML = `
      <img class="android-welcome-logo" src="00%20Logo%20MIM%20.png" alt="Logo MIM">
    `
    document.body.appendChild(overlay)
  }

  overlay.classList.add('show')
  const minShowMs = 900
  window.setTimeout(() => {
    overlay.classList.remove('show')
    window.setTimeout(() => {
      try {
        overlay.remove()
      } catch (_error) {}
      document.body.classList.remove('android-welcome-active')
      document.documentElement.classList.remove('android-preboot')
    }, 320)
  }, minShowMs)
}

function ensureGlobalCalendarAutoClose() {
  if (window.__calendarAutoCloseBound) return
  window.__calendarAutoCloseBound = true

  function closeTopbarCalendarWithAnimation() {
    const popup = document.getElementById('topbar-calendar-popup')
    if (!popup || popup.style.display === 'none') return
    if (popup.dataset.closing === '1') return
    popup.dataset.closing = '1'
    popup.classList.add('calendar-closing')
    window.setTimeout(() => {
      if (typeof window.closeTopbarCalendarPopup === 'function') {
        window.closeTopbarCalendarPopup()
      } else {
        popup.style.display = 'none'
      }
      popup.classList.remove('calendar-closing')
      popup.dataset.closing = '0'
    }, 130)
  }

  document.addEventListener('click', event => {
    const popup = document.getElementById('topbar-calendar-popup')
    if (!popup || popup.style.display === 'none') return
    const card = popup.querySelector('.topbar-calendar-card')
    const target = event.target instanceof HTMLElement ? event.target : null
    if (!target) return

    const clickedInsideCard = !!(card && (card.contains(target) || (typeof event.composedPath === 'function' && event.composedPath().includes(card))))
    if (clickedInsideCard) return

    const clickedDateButton = target.closest('.topbar-title-date-btn')
    if (clickedDateButton) return

    closeTopbarCalendarWithAnimation()
  }, true)

  document.addEventListener('click', event => {
    const popup = document.getElementById('topbar-calendar-popup')
    if (!popup || popup.style.display === 'none') return
    const target = event.target instanceof HTMLElement ? event.target : null
    if (!target) return

    const triggerOtherPopup = target.closest(
      '.topbar-notif-trigger, .topbar-user-trigger, .topbar-sidebar-toggle, #topbar-chat-trigger, .android-bottom-nav-btn'
    )
    if (!triggerOtherPopup) return
    closeTopbarCalendarWithAnimation()
  }, true)
}

function setAndroidSidebarOpen(nextOpen) {
  if (!document.body) return
  document.body.classList.toggle('android-sidebar-open', Boolean(nextOpen))
}

function ensureAndroidSidebarDrawer() {
  if (!isAndroidPlatform() || !document.body) return
  const body = document.body
  const sidebar = document.querySelector('.sidebar')
  const layout = document.querySelector('.layout')
  if (!sidebar) return

  body.classList.add('android-sidebar-drawer-enabled')
  if (layout) {
    layout.classList.remove('sidebar-collapsed')
    layout.classList.remove('sidebar-icons-only')
  }

  let scrim = document.getElementById('android-sidebar-scrim')
  if (!scrim) {
    scrim = document.createElement('div')
    scrim.id = 'android-sidebar-scrim'
    scrim.className = 'android-sidebar-scrim'
    scrim.addEventListener('click', () => setAndroidSidebarOpen(false))
    body.appendChild(scrim)
  }

  if (body.dataset.androidSidebarDrawerBound === '1') return
  body.dataset.androidSidebarDrawerBound = '1'

  document.addEventListener('click', event => {
    const target = event.target
    if (!(target instanceof HTMLElement)) return
    if (target.closest('.topbar-sidebar-toggle')) {
      event.preventDefault()
      event.stopPropagation()
      setAndroidSidebarOpen(!body.classList.contains('android-sidebar-open'))
      return
    }
    const clickedSidebarBtn = target.closest('.sidebar button')
    if (clickedSidebarBtn) {
      const isParentToggle = Boolean(
        clickedSidebarBtn.classList.contains('sidebar-parent-btn')
        || clickedSidebarBtn.classList.contains('sidebar-submenu-parent-btn')
        || clickedSidebarBtn.classList.contains('guru-parent-btn')
      )
      if (isParentToggle) return
      setTimeout(() => setAndroidSidebarOpen(false), 50)
    }
  }, true)
}

function goToAndroidDashboard() {
  try {
    if (typeof window.loadPage === 'function') {
      window.loadPage('dashboard')
      return
    }
    if (typeof window.loadGuruPage === 'function') {
      window.loadGuruPage('dashboard')
      return
    }
    if (typeof window.loadMuhaffizPage === 'function') {
      window.loadMuhaffizPage('dashboard')
      return
    }
    if (typeof window.loadMusyrifPage === 'function') {
      window.loadMusyrifPage('dashboard')
      return
    }
  } catch (_error) {}
}

function ensureAndroidBottomNav() {
  if (!isAndroidPlatform()) return
  if (document.getElementById('android-bottom-nav')) return
  if (!document.body) return

  const role = String(document.body.dataset?.role || '').trim().toLowerCase()
  const panel = String(document.body.dataset?.panel || '').trim().toLowerCase()
  const isGuruMainPage = role === 'guru' && panel !== 'wakasek-kurikulum'

  const items = isGuruMainPage
    ? [
      { key: 'dashboard', label: 'Dashboard', icon: '<path d="M3 9.5L12 3l9 6.5V20a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1V9.5Z"/>' },
      { key: 'input', label: 'Input', icon: '<path d="M4 19.5h16"/><path d="M7.5 15.5V8.5M12 15.5V5.5M16.5 15.5v-4"/>' },
      { key: 'tugas', label: 'Mutabaah', icon: '<rect x="4" y="3.5" width="16" height="18" rx="2.5"/><path d="M8 8h8M8 12h8M8 16h5"/>' },
      { key: 'chat', label: 'Pesan', icon: '<path d="M5 6.5h14a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H10l-4.5 3V17.5H5a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2Z"/>' },
      { key: 'profil', label: 'Profil', icon: '<circle cx="12" cy="8" r="3.2"/><path d="M5.5 19a6.5 6.5 0 0 1 13 0"/>' }
    ]
    : [
      { key: 'dashboard', label: 'Dashboard', icon: '<path d="M3 9.5L12 3l9 6.5V20a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1V9.5Z"/>' },
      { key: 'chat', label: 'Pesan', icon: '<path d="M5 6.5h14a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H10l-4.5 3V17.5H5a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2Z"/>' },
      { key: 'profil', label: 'Profil', icon: '<circle cx="12" cy="8" r="3.2"/><path d="M5.5 19a6.5 6.5 0 0 1 13 0"/>' }
    ]

  const renderItem = (item, index) => {
    if (item.key === 'profil') {
      const displayName = String(localStorage.getItem('login_name') || '').trim() || String(id || '').trim()
      const photoUrl = String(localStorage.getItem('login_photo_url') || '').trim()
      const initials = getInitials(displayName)
      const imgPart = photoUrl
        ? `<img class="android-bottom-avatar-img" src="${photoUrl}" alt="Avatar profil">`
        : ''
      const initialsPart = photoUrl ? '' : `<span class="android-bottom-avatar-initials">${initials}</span>`
      return `
        <button type="button" class="android-bottom-nav-btn ${index === 0 ? 'active' : ''}" data-nav-key="${item.key}">
          <span class="android-bottom-avatar-wrap">
            <span class="android-bottom-avatar" id="android-bottom-avatar">
              ${imgPart}${initialsPart}
            </span>
          </span>
          <span class="android-bottom-nav-label">${item.label}</span>
        </button>
      `
    }
    return `
      <button type="button" class="android-bottom-nav-btn ${index === 0 ? 'active' : ''}" data-nav-key="${item.key}">
        <span class="android-bottom-nav-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">${item.icon}</svg>
        </span>
        <span class="android-bottom-nav-label">${item.label}</span>
      </button>
    `
  }

  const nav = document.createElement('nav')
  nav.id = 'android-bottom-nav'
  nav.className = 'android-bottom-nav'
  nav.innerHTML = items.map(renderItem).join('')
  document.body.appendChild(nav)

  const setActive = key => {
    const buttons = nav.querySelectorAll('.android-bottom-nav-btn')
    buttons.forEach(btn => {
      btn.classList.toggle('active', String(btn.getAttribute('data-nav-key') || '') === String(key || ''))
    })
  }

  const chooseGuruInputTarget = async () => {
    const accountMenu = document.getElementById('topbar-user-menu')
    if (accountMenu) {
      accountMenu.classList.remove('open')
      if (accountMenu.dataset.anchorSource === 'android-bottom') {
        accountMenu.dataset.anchorSource = ''
        accountMenu.style.position = ''
        accountMenu.style.left = ''
        accountMenu.style.top = ''
        accountMenu.style.right = ''
        accountMenu.style.bottom = ''
        accountMenu.style.maxHeight = ''
        accountMenu.style.overflowY = ''
        accountMenu.style.zIndex = ''
      }
    }

    const existing = document.getElementById('android-input-picker')
    if (existing) {
      existing.remove()
      return ''
    }
    const anchorBtn = nav.querySelector('.android-bottom-nav-btn[data-nav-key="input"]')
    if (!anchorBtn) return ''

    return new Promise(resolve => {
      const menu = document.createElement('div')
      menu.id = 'android-input-picker'
      menu.style.position = 'fixed'
      menu.style.zIndex = '12130'
      menu.style.minWidth = '168px'
      menu.style.maxWidth = '220px'
      menu.style.background = '#ffffff'
      menu.style.border = '1px solid #cbd5e1'
      menu.style.borderRadius = '10px'
      menu.style.boxShadow = '0 12px 28px rgba(15,23,42,0.18)'
      menu.style.padding = '6px'
      menu.style.fontFamily = '"Poppins", Arial, sans-serif'

      const makeRow = (label, value) => {
        const btn = document.createElement('button')
        btn.type = 'button'
        btn.textContent = label
        btn.style.width = '100%'
        btn.style.border = 'none'
        btn.style.background = 'transparent'
        btn.style.color = '#0f172a'
        btn.style.fontSize = '12px'
        btn.style.fontWeight = '600'
        btn.style.fontFamily = '"Poppins", Arial, sans-serif'
        btn.style.textAlign = 'left'
        btn.style.padding = '8px 10px'
        btn.style.borderRadius = '7px'
        btn.style.cursor = 'pointer'
        btn.addEventListener('click', event => {
          event.preventDefault()
          event.stopPropagation()
          cleanup()
          resolve(value)
        })
        btn.addEventListener('mouseenter', () => { btn.style.background = '#f1f5f9' })
        btn.addEventListener('mouseleave', () => { btn.style.background = 'transparent' })
        return btn
      }

      const cleanup = () => {
        document.removeEventListener('click', onOutsideClick, true)
        window.removeEventListener('resize', onResize)
        menu.remove()
      }

      const onOutsideClick = event => {
        if (menu.contains(event.target) || anchorBtn.contains(event.target)) return
        cleanup()
        resolve('')
      }

      const onResize = () => {
        cleanup()
        resolve('')
      }

      menu.appendChild(makeRow('Input Nilai', 'input-nilai'))
      menu.appendChild(makeRow('Input Absen', 'input-absensi'))
      document.body.appendChild(menu)

      const rect = anchorBtn.getBoundingClientRect()
      const menuW = Math.max(menu.offsetWidth || 168, 168)
      const menuH = Math.max(menu.offsetHeight || 80, 80)
      const gap = 8
      const vw = window.innerWidth || document.documentElement.clientWidth || 360
      const vh = window.innerHeight || document.documentElement.clientHeight || 640

      let left = rect.left + Math.max(0, Math.round((rect.width - menuW) / 2))
      if (left < 8) left = 8
      if (left + menuW > vw - 8) left = vw - menuW - 8

      let top = rect.top - menuH - gap
      if (top < 8) top = rect.bottom + gap
      if (top + menuH > vh - 8) top = vh - menuH - 8

      menu.style.left = `${Math.round(left)}px`
      menu.style.top = `${Math.round(top)}px`

      setTimeout(() => {
        document.addEventListener('click', onOutsideClick, true)
        window.addEventListener('resize', onResize)
      }, 0)
    })
  }

  const toggleAndroidBottomProfileMenu = anchorBtn => {
    const menu = document.getElementById('topbar-user-menu')
    if (!menu) return
    const isOpenFromBottom = menu.classList.contains('open') && menu.dataset.anchorSource === 'android-bottom'
    if (isOpenFromBottom) {
      menu.classList.remove('open')
      menu.dataset.anchorSource = ''
      menu.style.position = ''
      menu.style.left = ''
      menu.style.top = ''
      menu.style.right = ''
      menu.style.bottom = ''
      menu.style.maxHeight = ''
      menu.style.overflowY = ''
      return
    }

    menu.dataset.anchorSource = 'android-bottom'
    menu.classList.add('open')

    const rect = anchorBtn?.getBoundingClientRect?.()
    if (!rect) return

    menu.style.position = 'fixed'
    menu.style.zIndex = '12120'
    menu.style.left = '8px'
    menu.style.top = '8px'
    menu.style.right = 'auto'
    menu.style.bottom = 'auto'
    menu.style.maxHeight = 'min(62vh, 460px)'
    menu.style.overflowY = 'auto'

    const menuWidth = Math.max(menu.offsetWidth || 220, 220)
    const menuHeight = Math.max(menu.offsetHeight || 160, 160)
    const gap = 8
    const viewportW = window.innerWidth || document.documentElement.clientWidth || 360
    const viewportH = window.innerHeight || document.documentElement.clientHeight || 640

    let left = viewportW - menuWidth - 8
    let top = rect.top - menuHeight - gap

    const isTablet = viewportW >= 821
    if (isTablet) {
      left = rect.right + gap
      top = rect.top - Math.max(8, Math.round((menuHeight - rect.height) / 2))
      if (left + menuWidth > viewportW - 8) {
        left = rect.left - menuWidth - gap
      }
    }

    if (left < 8) left = 8
    if (left + menuWidth > viewportW - 8) left = viewportW - menuWidth - 8
    if (top < 8) top = 8
    if (top + menuHeight > viewportH - 8) top = viewportH - menuHeight - 8

    menu.style.left = `${Math.round(left)}px`
    menu.style.top = `${Math.round(top)}px`
  }

  const runAction = async key => {
    const navKey = String(key || '').trim().toLowerCase()
    if (navKey === 'profil') {
      setActive('profil')
      const button = nav.querySelector('.android-bottom-nav-btn[data-nav-key="profil"]')
      if (typeof window.closeTopbarNotifMenu === 'function') {
        try { window.closeTopbarNotifMenu() } catch (_error) {}
      }
      toggleAndroidBottomProfileMenu(button)
      return
    }
    try {
      if (role === 'guru' && panel !== 'wakasek-kurikulum' && typeof window.loadGuruPage === 'function') {
        if (navKey === 'dashboard') window.loadGuruPage('dashboard')
        else if (navKey === 'input') {
          const target = await chooseGuruInputTarget()
          if (!target) return
          window.loadGuruPage(target)
        }
        else if (navKey === 'tugas') window.loadGuruPage('tugas')
        else if (navKey === 'chat') window.loadGuruPage('chat')
        else window.loadGuruPage('dashboard')
        const menu = document.getElementById('topbar-user-menu')
        if (menu?.dataset?.anchorSource === 'android-bottom') {
          menu.classList.remove('open')
          menu.dataset.anchorSource = ''
        }
        setActive(navKey)
        return
      }

      if (typeof window.loadPage === 'function') {
        if (navKey === 'chat') {
          window.loadPage('chat')
          setActive('chat')
          return
        }
        if (navKey === 'dashboard') {
          window.loadPage('dashboard')
          setActive('dashboard')
          return
        }
      }

      if (typeof window.loadMuhaffizPage === 'function') {
        if (navKey === 'chat') {
          window.loadMuhaffizPage('chat')
          setActive('chat')
          return
        }
        if (navKey === 'dashboard') {
          window.loadMuhaffizPage('dashboard')
          setActive('dashboard')
          return
        }
      }

      if (typeof window.loadMusyrifPage === 'function') {
        if (navKey === 'chat') {
          window.loadMusyrifPage('chat')
          setActive('chat')
          return
        }
        if (navKey === 'dashboard') {
          window.loadMusyrifPage('dashboard')
          setActive('dashboard')
          return
        }
      }

      if (typeof window.loadGuruPage === 'function') {
        if (navKey === 'chat') {
          window.loadGuruPage('chat')
          setActive('chat')
          return
        }
        if (navKey === 'dashboard') {
          window.loadGuruPage('dashboard')
          setActive('dashboard')
          return
        }
      }
    } catch (_error) {}
    goToAndroidDashboard()
    setActive('dashboard')
  }

  nav.querySelectorAll('.android-bottom-nav-btn').forEach(btn => {
    btn.addEventListener('click', event => {
      event.preventDefault()
      event.stopPropagation()
      void runAction(btn.getAttribute('data-nav-key') || '')
    })
  })

  if (!window.__androidBottomProfileMenuOutsideBound) {
    window.__androidBottomProfileMenuOutsideBound = true
    document.addEventListener('click', event => {
      const menu = document.getElementById('topbar-user-menu')
      if (!menu || menu.dataset.anchorSource !== 'android-bottom') return
      const profileBtn = document.querySelector('#android-bottom-nav .android-bottom-nav-btn[data-nav-key="profil"]')
      if (menu.contains(event.target) || profileBtn?.contains(event.target)) return
      menu.classList.remove('open')
      menu.dataset.anchorSource = ''
      menu.style.position = ''
      menu.style.left = ''
      menu.style.top = ''
      menu.style.right = ''
      menu.style.bottom = ''
      menu.style.maxHeight = ''
      menu.style.overflowY = ''
    })
  }

  async function resolveAndroidOnlineStatus() {
    if (!navigator.onLine) return false
    // Android WebView kadang mengembalikan onLine=true walau internet putus.
    // Tambah probe ringan agar status cincin avatar lebih akurat.
    const probeWithTimeout = async (url, timeoutMs = 2200) => {
      const controller = new AbortController()
      const timer = setTimeout(() => controller.abort(), timeoutMs)
      try {
        await fetch(url, { method: 'GET', mode: 'no-cors', cache: 'no-store', signal: controller.signal })
        return true
      } catch (_error) {
        return false
      } finally {
        clearTimeout(timer)
      }
    }
    const ok = await probeWithTimeout('https://www.gstatic.com/generate_204')
    if (ok) return true
    return probeWithTimeout('https://api.github.com/')
  }

  const updateAndroidBottomAvatarStatus = async () => {
    const avatar = document.getElementById('android-bottom-avatar')
    if (!avatar) return
    const online = await resolveAndroidOnlineStatus()
    avatar.classList.toggle('android-online', online)
    avatar.classList.toggle('android-offline', !online)
  }

  if (!window.__androidBottomAvatarStatusBound) {
    window.__androidBottomAvatarStatusBound = true
    window.__androidBottomAvatarStatusRefresh = updateAndroidBottomAvatarStatus
    window.addEventListener('online', () => void updateAndroidBottomAvatarStatus())
    window.addEventListener('offline', () => void updateAndroidBottomAvatarStatus())
    window.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'visible') void updateAndroidBottomAvatarStatus()
    })
    window.addEventListener('focus', () => void updateAndroidBottomAvatarStatus())
    window.__androidBottomAvatarStatusInterval = window.setInterval(() => {
      if (document.visibilityState === 'hidden') return
      void updateAndroidBottomAvatarStatus()
    }, 8000)
  }
  if (typeof window.__androidBottomAvatarStatusRefresh === 'function') {
    void window.__androidBottomAvatarStatusRefresh()
  } else {
    void updateAndroidBottomAvatarStatus()
  }

  window.updateAndroidBottomNavActive = function updateAndroidBottomNavActive(pageKey) {
    const key = String(pageKey || '').trim().toLowerCase()
    if (role === 'guru' && panel !== 'wakasek-kurikulum') {
      if (key === 'input-nilai' || key === 'input-absensi' || key === 'input' || key === 'nilai' || key === 'absensi') {
        setActive('input')
        return
      }
      if (key === 'tugas') {
        setActive('tugas')
        return
      }
      if (key === 'chat') {
        setActive('chat')
        return
      }
      if (key === 'profil') {
        setActive('profil')
        return
      }
      setActive('dashboard')
      return
    }
    if (key === 'profil') {
      setActive('profil')
      return
    }
    if (key === 'chat') {
      setActive('chat')
      return
    }
    setActive('dashboard')
  }
}

function getActiveRole() {
  if (requiredRole && roles.includes(requiredRole)) return requiredRole
  return roles[0] || ''
}

function getInitials(name) {
  const parts = String(name || '')
    .trim()
    .split(/\s+/)
    .filter(Boolean)
  if (!parts.length) return 'U'
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase()
  return `${parts[0][0] || ''}${parts[1][0] || ''}`.toUpperCase()
}

function setTopbarAvatar(name, fotoUrl = '') {
  const trigger = document.querySelector('.topbar-user-trigger')
  if (!trigger) return
  trigger.classList.add('topbar-avatar-trigger')
  trigger.innerHTML = `
    <span class="topbar-avatar" id="topbar-avatar">
      <span class="topbar-avatar-initials" id="topbar-avatar-initials">${getInitials(name)}</span>
      <img id="topbar-avatar-img" class="topbar-avatar-img" alt="Avatar pengguna" style="display:none;">
    </span>
    <span id="welcome" class="topbar-user-name" style="display:none;">${String(name || '')}</span>
  `

  const imgEl = document.getElementById('topbar-avatar-img')
  const initialsEl = document.getElementById('topbar-avatar-initials')
  if (!imgEl || !initialsEl) return

  const url = String(fotoUrl || '').trim()
  if (!url) {
    imgEl.style.display = 'none'
    imgEl.src = ''
    initialsEl.style.display = 'inline-flex'
    return
  }
  imgEl.src = url
  imgEl.style.display = 'block'
  initialsEl.style.display = 'none'
}

function syncAndroidBottomAvatar(name, fotoUrl = '') {
  const avatar = document.getElementById('android-bottom-avatar')
  if (!avatar) return
  const safeName = String(name || '').trim() || String(localStorage.getItem('login_name') || '').trim() || 'User'
  const safeUrl = String(fotoUrl || '').trim()

  let imgEl = avatar.querySelector('.android-bottom-avatar-img')
  let initialsEl = avatar.querySelector('.android-bottom-avatar-initials')

  if (!safeUrl) {
    if (imgEl) {
      imgEl.remove()
      imgEl = null
    }
    if (!initialsEl) {
      initialsEl = document.createElement('span')
      initialsEl.className = 'android-bottom-avatar-initials'
      avatar.appendChild(initialsEl)
    }
    initialsEl.textContent = getInitials(safeName)
    return
  }

  if (!imgEl) {
    imgEl = document.createElement('img')
    imgEl.className = 'android-bottom-avatar-img'
    imgEl.alt = 'Avatar profil'
    avatar.appendChild(imgEl)
  }
  imgEl.src = safeUrl
  imgEl.style.display = 'block'
  imgEl.onerror = () => {
    imgEl.style.display = 'none'
    if (!initialsEl) {
      initialsEl = document.createElement('span')
      initialsEl.className = 'android-bottom-avatar-initials'
      avatar.appendChild(initialsEl)
    }
    initialsEl.textContent = getInitials(safeName)
    initialsEl.style.display = 'inline-flex'
  }
  if (initialsEl) initialsEl.remove()
}

function initTopbarAccountMenu() {
  const wrap = document.querySelector('.topbar-user-menu-wrap')
  const trigger = document.querySelector('.topbar-user-trigger')
  const menu = document.getElementById('topbar-user-menu')
  if (!wrap || !trigger || !menu) return

  const displayName = String(localStorage.getItem('login_name') || '').trim() || String(id || '').trim()
  const photoUrl = String(localStorage.getItem('login_photo_url') || '').trim()
  const activeRole = getActiveRole()
  setTopbarAvatar(displayName, photoUrl)

  menu.classList.add('topbar-account-menu')

  let nameEl = document.getElementById('topbar-account-name')
  if (!nameEl) {
    nameEl = document.createElement('div')
    nameEl.id = 'topbar-account-name'
    nameEl.className = 'topbar-account-name'
    menu.prepend(nameEl)
  }
  nameEl.textContent = displayName

  let roleWrap = document.getElementById('topbar-role-switch-wrap')
  if (!roleWrap) {
    roleWrap = document.createElement('div')
    roleWrap.id = 'topbar-role-switch-wrap'
    roleWrap.className = 'topbar-role-switch-wrap'
    roleWrap.innerHTML = `
      <button type="button" id="topbar-role-switch-btn" class="topbar-role-switch-btn">Role</button>
      <div id="topbar-role-switch-menu" class="topbar-role-switch-menu"></div>
    `
    const firstButton = menu.querySelector('button')
    if (firstButton) firstButton.before(roleWrap)
    else menu.appendChild(roleWrap)
  }

  const isTauriApp = hasTauriRuntime()
  const isAndroidApp = isAndroidPlatform()
  const isDesktopApp = isDesktopPlatform()
  if (isDesktopApp && !document.getElementById('topbar-app-info-btn')) {
    const infoBtn = document.createElement('button')
    infoBtn.type = 'button'
    infoBtn.id = 'topbar-app-info-btn'
    infoBtn.textContent = 'Info'
    const logoutBtn = menu.querySelector('button[onclick*="logout"], button[onclick*="Logout"]')
    if (logoutBtn) logoutBtn.before(infoBtn)
    else menu.appendChild(infoBtn)
  }
  if (isAndroidApp && !document.getElementById('topbar-mobile-info-btn')) {
    const infoBtn = document.createElement('button')
    infoBtn.type = 'button'
    infoBtn.id = 'topbar-mobile-info-btn'
    infoBtn.textContent = 'Info'
    const logoutBtn = menu.querySelector('button[onclick*="logout"], button[onclick*="Logout"]')
    if (logoutBtn) logoutBtn.before(infoBtn)
    else menu.appendChild(infoBtn)
  }
  if (isWebPlatform() && !document.getElementById('topbar-web-info-btn')) {
    const infoBtn = document.createElement('button')
    infoBtn.type = 'button'
    infoBtn.id = 'topbar-web-info-btn'
    infoBtn.textContent = 'Info'
    const logoutBtn = menu.querySelector('button[onclick*="logout"], button[onclick*="Logout"]')
    if (logoutBtn) logoutBtn.before(infoBtn)
    else menu.appendChild(infoBtn)
  }

  const roleBtn = document.getElementById('topbar-role-switch-btn')
  const roleMenu = document.getElementById('topbar-role-switch-menu')
  if (!roleBtn || !roleMenu) {
    initWebDesktopInfoPopup()
    initAndroidReleaseInfoPopup()
    initMobileInAppUpdatePrompt()
    return
  }

  if (!document.getElementById('topbar-web-desktop-info-style')) {
    const style = document.createElement('style')
    style.id = 'topbar-web-desktop-info-style'
    style.textContent = `
      .topbar-web-info-overlay {
        position: fixed;
        inset: 0;
        z-index: 12100;
        display: none;
        align-items: center;
        justify-content: center;
        background: rgba(15, 23, 42, 0.4);
        padding: 16px;
      }
      .topbar-web-info-overlay.open {
        display: flex;
      }
      .topbar-web-info-card {
        width: min(520px, calc(100vw - 24px));
        max-height: min(78vh, 620px);
        overflow: auto;
        background: #fff;
        border: 1px solid #cbd5e1;
        border-radius: 14px;
        box-shadow: 0 20px 40px rgba(15, 23, 42, 0.2);
        padding: 16px;
      }
      .topbar-web-info-card h3 {
        margin: 0 0 8px;
        font-size: 16px;
        color: #0f172a;
      }
      .topbar-web-desktop-version {
        font-size: 12px;
        color: #475569;
        margin-bottom: 6px;
      }
      .topbar-web-desktop-notes {
        white-space: pre-wrap;
        font-size: 12px;
        color: #334155;
        line-height: 1.45;
        margin-bottom: 8px;
      }
      .topbar-web-desktop-downloads {
        display: grid;
        grid-template-columns: 1fr;
        gap: 8px;
      }
      .topbar-web-download-group {
        border: 1px solid #e2e8f0;
        border-radius: 10px;
        padding: 8px;
      }
      .topbar-web-download-group-title {
        font-size: 11px;
        font-weight: 700;
        color: #475569;
        margin: 0 0 6px;
      }
      .topbar-web-download-group-buttons {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 8px;
      }
      .topbar-web-desktop-download-btn {
        width: 100%;
        border: 1px solid #cbd5e1;
        border-radius: 10px;
        background: #fff;
        color: #0f172a;
        font-size: 12px;
        font-weight: 600;
        padding: 8px 10px;
        cursor: pointer;
      }
      .topbar-web-desktop-download-btn:hover {
        background: #f8fafc;
      }
      .topbar-web-info-actions {
        margin-top: 12px;
        display: flex;
        justify-content: flex-end;
      }
      .topbar-web-info-actions button {
        border: 1px solid #cbd5e1;
        border-radius: 10px;
        background: #fff;
        color: #0f172a;
        font-size: 12px;
        font-weight: 600;
        padding: 6px 12px;
        cursor: pointer;
      }
    `
    document.head.appendChild(style)
  }

  initWebDesktopInfoPopup()
  initAndroidReleaseInfoPopup()
  initMobileInAppUpdatePrompt()

  roleBtn.textContent = `Role: ${activeRole || '-'}`
  const optionRoles = [...new Set(roles)].filter(role => !!ROLE_PAGE_MAP[role] && role !== activeRole)
  roleMenu.innerHTML = optionRoles.length
    ? optionRoles.map(role => `<button type="button" class="topbar-role-option" data-role="${role}">${role}</button>`).join('')
    : '<div class="topbar-role-empty">Tidak ada role lain</div>'

  roleBtn.onclick = event => {
    event.stopPropagation()
    roleMenu.classList.toggle('open')
  }

  roleMenu.onclick = event => {
    const btn = event.target.closest('.topbar-role-option')
    if (!btn) return
    const targetRole = String(btn.getAttribute('data-role') || '').trim().toLowerCase()
    const page = ROLE_PAGE_MAP[targetRole]
    if (!targetRole || !page) return
    localStorage.setItem('login_role', targetRole)
    location.href = page
  }

  document.addEventListener('click', event => {
    if (roleWrap.contains(event.target)) return
    roleMenu.classList.remove('open')
  })
}

async function initAndroidReleaseInfoPopup() {
  if (!isAndroidPlatform()) return

  const infoBtn = document.getElementById('topbar-mobile-info-btn')
  if (!infoBtn) return

  if (!document.getElementById('topbar-mobile-info-style')) {
    const style = document.createElement('style')
    style.id = 'topbar-mobile-info-style'
    style.textContent = `
      .topbar-mobile-info-overlay {
        position: fixed;
        inset: 0;
        z-index: 12100;
        display: none;
        align-items: center;
        justify-content: center;
        background: rgba(15, 23, 42, 0.4);
        padding: 16px;
      }
      .topbar-mobile-info-overlay.open {
        display: flex;
      }
      .topbar-mobile-info-card {
        width: min(520px, calc(100vw - 24px));
        max-height: min(78vh, 620px);
        overflow: auto;
        background: #fff;
        border: 1px solid #cbd5e1;
        border-radius: 14px;
        box-shadow: 0 20px 40px rgba(15, 23, 42, 0.2);
        padding: 16px;
      }
      .topbar-mobile-info-card h3 {
        margin: 0 0 8px;
        font-size: 16px;
        color: #0f172a;
      }
      .topbar-mobile-info-version {
        font-size: 12px;
        color: #475569;
        margin-bottom: 8px;
      }
      .topbar-mobile-info-notes {
        white-space: pre-wrap;
        font-size: 12px;
        color: #334155;
        line-height: 1.45;
        margin-bottom: 10px;
      }
      .topbar-mobile-info-actions {
        margin-top: 12px;
        display: flex;
        justify-content: space-between;
        gap: 8px;
      }
      .topbar-mobile-info-actions button {
        border: 1px solid #cbd5e1;
        border-radius: 10px;
        background: #fff;
        color: #0f172a;
        font-size: 12px;
        font-weight: 600;
        padding: 6px 12px;
        cursor: pointer;
      }
    `
    document.head.appendChild(style)
  }

  let overlay = document.getElementById('topbar-mobile-info-overlay')
  if (!overlay) {
    overlay = document.createElement('div')
    overlay.id = 'topbar-mobile-info-overlay'
    overlay.className = 'topbar-mobile-info-overlay'
    overlay.innerHTML = `
      <div class="topbar-mobile-info-card">
        <h3>Info Versi Android</h3>
        <div id="topbar-mobile-info-version" class="topbar-mobile-info-version">Memuat versi...</div>
        <div id="topbar-mobile-info-notes" class="topbar-mobile-info-notes">Memuat catatan rilis...</div>
        <div class="topbar-mobile-info-actions">
          <button type="button" id="topbar-mobile-download-apk">Download APK</button>
          <button type="button" id="topbar-mobile-info-close">Tutup</button>
        </div>
      </div>
    `
    document.body.appendChild(overlay)
    overlay.querySelector('#topbar-mobile-info-close')?.addEventListener('click', () => {
      overlay.classList.remove('open')
    })
    overlay.addEventListener('click', event => {
      if (event.target === overlay) overlay.classList.remove('open')
    })
  }

  const versionEl = overlay.querySelector('#topbar-mobile-info-version')
  const notesEl = overlay.querySelector('#topbar-mobile-info-notes')
  const downloadApkBtn = overlay.querySelector('#topbar-mobile-download-apk')
  if (!versionEl || !notesEl || !downloadApkBtn) return

  let apkUrl = 'https://github.com/timakademikmim/mim-app/releases/latest/download/app-universal-release.apk'
  let currentVersion = await getMobileCurrentVersionText()

  try {
    let mobileRes = await fetch(`https://github.com/timakademikmim/mim-app/releases/latest/download/mobile-latest.json?t=${Date.now()}`, { cache: 'no-store' })
    if (!mobileRes.ok) {
      mobileRes = await fetch(`https://github.com/timakademikmim/mim-app/releases/latest/download/latest.json?t=${Date.now()}`, { cache: 'no-store' })
    }
    let latestVersion = ''
    let notes = ''
    if (mobileRes.ok) {
      const latest = await mobileRes.json()
      latestVersion = normalizeVersionText(latest?.version || '')
      const mobile = latest?.mobile && typeof latest.mobile === 'object' ? latest.mobile : {}
      apkUrl = String(mobile?.apk || apkUrl).trim() || apkUrl
      notes = String(latest?.notes || '').trim()
    }

    const androidMeta = await fetchLatestAndroidReleaseMeta()
    if (androidMeta.version && compareVersionText(androidMeta.version, latestVersion) > 0) {
      latestVersion = normalizeVersionText(androidMeta.version)
    }
    if (androidMeta.apkUrl) apkUrl = androidMeta.apkUrl
    if ((!notes || notes.length < 10) && androidMeta.notes) notes = androidMeta.notes

    if (!latestVersion) throw new Error('mobile latest not available')
    if (!notes) notes = "What's new in this version:\n- Pembaruan Android tersedia."
    versionEl.textContent = `Versi Android saat ini: v${currentVersion} | Versi terbaru: v${latestVersion}`
    notesEl.textContent = notes
  } catch (_error) {
    versionEl.textContent = `Versi Android saat ini: v${currentVersion}`
    notesEl.textContent = "Catatan rilis belum tersedia. Kamu tetap bisa unduh APK terbaru secara manual."
  }

  if (downloadApkBtn.dataset.boundMobileDownload !== '1') {
    downloadApkBtn.dataset.boundMobileDownload = '1'
    downloadApkBtn.addEventListener('click', async () => {
      const originalLabel = downloadApkBtn.textContent || 'Download APK'
      let loadingTick = 0
      downloadApkBtn.disabled = true
      downloadApkBtn.textContent = 'Mengunduh'
      const timer = window.setInterval(() => {
        loadingTick = (loadingTick + 1) % 4
        downloadApkBtn.textContent = `Mengunduh${'.'.repeat(loadingTick)}`
      }, 280)
      const stopLoading = () => {
        window.clearInterval(timer)
        downloadApkBtn.disabled = false
        downloadApkBtn.textContent = originalLabel
      }
      const inferredName = `MIM-App-Android-v${String(versionEl.textContent || '').match(/v([0-9.]+)/)?.[1] || 'latest'}.apk`
      try {
        if (typeof window.downloadToAndroidAppStorage === 'function') {
          const saved = await window.downloadToAndroidAppStorage(apkUrl, inferredName)
          if (saved?.ok) {
            stopLoading()
            let opened = false
            try {
              await invokeTauriCommand('open_file_path', { path: String(saved.path || '') })
              opened = true
            } catch (_error) {}
            alert(opened
              ? `File APK tersimpan. Installer dibuka otomatis.\n\nLokasi:\n${saved.path}`
              : `File APK tersimpan di:\n${saved.path}`)
            return
          }
        }
      } catch (_error) {}
      try {
        if (typeof window.openExternalUrl === 'function') {
          const opened = await window.openExternalUrl(apkUrl)
          if (opened) {
            stopLoading()
            return
          }
        }
      } catch (_error) {}
      stopLoading()
      window.location.href = apkUrl
    })
  }

  if (infoBtn.dataset.boundMobileInfo !== '1') {
    infoBtn.dataset.boundMobileInfo = '1'
    infoBtn.addEventListener('click', () => {
      overlay.classList.add('open')
    })
  }
}

async function initWebDesktopInfoPopup() {
  if (!isWebPlatform()) return
  const GENERIC_NOTE = 'desktop release otomatis dengan updater artifacts.'
  const WEB_VERSION_WHATS_NEW = {
    '0.3.19': `What's new in this version:
- Card agenda dashboard Android diperkecil lagi agar proporsional dengan layar HP.
- Pembukaan installer APK otomatis diperkuat dengan intent INSTALL_PACKAGE + fallback VIEW.`,
    '0.3.18': `What's new in this version:
- Setelah unduh APK selesai, aplikasi sekarang otomatis mencoba membuka installer APK.
- Alur update Android dipercepat agar pengguna tidak perlu cari file manual di File Manager.`,
    '0.3.17': `What's new in this version:
- Download Android ditingkatkan: prioritas simpan ke folder Download publik, fallback ke folder aplikasi jika diperlukan.
- Tombol download pada popup info Android kini menampilkan animasi loading agar proses terlihat jelas.
- Cetak laporan bulanan Android kini disimpan sebagai file PDF ke folder download melalui command native.
- Sinkron avatar Android diperkuat agar foto profil lebih konsisten tampil.`,
    '0.3.16': `What's new in this version:
- Perbaikan build Android: path resolver Tauri diperbaiki agar rilis APK tidak gagal compile.
- Stabilitas proses rilis Android ditingkatkan setelah penyesuaian command native.`,
    '0.3.15': `What's new in this version:
- Jarak aman topbar Android ditambah agar tombol tidak mepet ke ujung atas layar.
- Download APK Android diperbarui: file bisa disimpan ke folder lokal aplikasi (AppData/Downloads).
- Sinkron avatar Android diperbaiki agar foto profil ikut tampil di bottom navigation.
- Deklarasi izin Android untuk instal paket/akses storage kompatibel ditambahkan.`,
    '0.3.14': `What's new in this version:
- Perbaikan layout Android: topbar menempel di atas dan tidak ikut masuk ke kontainer konten.
- Perbaikan drawer Android: posisi sidebar/logo lebih stabil agar tidak terpotong.
- Perbaikan buka tautan eksternal Android (download APK) dengan fallback intent.
- Perbaikan cetak PDF Android dengan fallback buka PDF ke viewer eksternal.`,
    '0.3.8': `What's new in this version:
- Alur update Android dan Desktop dipisah (Android: mobile-latest.json, Desktop: latest.json).
- Asset rilis Android kini otomatis terupload bersama metadata mobile-latest.json.
- Build pipeline lintas platform diperbaiki agar runner Linux tidak gagal karena PowerShell.`,
    '0.3.7': `What's new in this version:
- Android release kini terotomatisasi: APK ikut terupload saat tag rilis dibuat.
- Asset APK updater distandarkan ke app-universal-release.apk agar update in-app lebih konsisten.
- Stabilitas updater desktop ditingkatkan.`,
    '0.3.6': `What's new in this version:
- Perbaikan updater desktop agar status "pembaruan berjalan" tidak stuck berkepanjangan.
- Lock update otomatis dilepas pada kondisi siap restart / error / timeout watchdog.
- Stabilitas alur update desktop ditingkatkan untuk mencegah freeze di antarmuka.`,
    '0.3.5': `What's new in this version:
- Perbaikan checker update Android agar tetap mendeteksi versi baru meski versi lokal belum terbaca.
- Cache-busting pada pembacaan latest.json supaya tidak tertahan data versi lama.`,
    '0.3.4': `What's new in this version:
- Android update prompt hanya muncul jika ada versi yang benar-benar lebih baru.
- Tautan unduhan Android diarahkan ke paket ARM64 yang lebih ringan untuk install manual.
- Catatan rilis fallback diperjelas agar popup Info tetap informatif.`,
    '0.3.0': `What's new in this version:
- Cincin status avatar sekarang tampil permanen (biru saat online, merah saat offline).
- Menu akun menambahkan tombol Info di bawah Profil.
- Popup Info rilis dirapikan agar catatan versi lebih jelas.`,
    '0.2.9': `What's new in this version:
- Info rilis dipindahkan ke menu avatar di topbar.
- Indikator online/offline dipindah ke cincin di sekitar avatar.`
  }
  const normalizeVersion = version => String(version || '').trim().replace(/^v/i, '')
  const isGenericNotes = text => String(text || '').trim().toLowerCase() === GENERIC_NOTE
  const getStoredVersion = () => normalizeVersion(localStorage.getItem('web_desktop_latest_version') || '')
  const setStoredVersion = version => {
    const v = normalizeVersion(version)
    if (v) localStorage.setItem('web_desktop_latest_version', v)
  }
  const fallbackNotes = version => {
    const v = normalizeVersion(version)
    if (WEB_VERSION_WHATS_NEW[v]) return WEB_VERSION_WHATS_NEW[v]
    return `What's new in this version:
- Pembaruan fitur dan penyempurnaan stabilitas aplikasi desktop.`
  }
  const fetchLatestDesktopVersionFromApi = async () => {
    try {
      const res = await fetch('https://api.github.com/repos/timakademikmim/mim-app/releases/latest', {
        cache: 'no-store',
        headers: { Accept: 'application/vnd.github+json' }
      })
      if (!res.ok) return ''
      const json = await res.json()
      return normalizeVersion(json?.tag_name || json?.name || '')
    } catch (_error) {
      return ''
    }
  }
  const infoBtn = document.getElementById('topbar-web-info-btn')
  if (!infoBtn) return
  let overlay = document.getElementById('topbar-web-info-overlay')
  if (!overlay) {
    overlay = document.createElement('div')
    overlay.id = 'topbar-web-info-overlay'
    overlay.className = 'topbar-web-info-overlay'
    overlay.innerHTML = `
      <div class="topbar-web-info-card">
        <h3>Info Versi Desktop</h3>
        <div id="topbar-web-desktop-version" class="topbar-web-desktop-version">Versi desktop: memuat...</div>
        <div id="topbar-web-desktop-notes" class="topbar-web-desktop-notes">Memuat catatan rilis...</div>
        <div class="topbar-web-desktop-downloads">
          <div class="topbar-web-download-group">
            <div class="topbar-web-download-group-title">Desktop</div>
            <div class="topbar-web-download-group-buttons">
              <button type="button" id="topbar-web-desktop-download-exe" class="topbar-web-desktop-download-btn">Download EXE</button>
              <button type="button" id="topbar-web-desktop-download-msi" class="topbar-web-desktop-download-btn">Download MSI</button>
            </div>
          </div>
          <div class="topbar-web-download-group">
            <div class="topbar-web-download-group-title">Android</div>
            <div class="topbar-web-download-group-buttons">
              <button type="button" id="topbar-web-mobile-download-apk" class="topbar-web-desktop-download-btn">Download APK</button>
              <button type="button" id="topbar-web-mobile-download-aab" class="topbar-web-desktop-download-btn">Download AAB</button>
            </div>
          </div>
        </div>
        <div class="topbar-web-info-actions">
          <button type="button" id="topbar-web-info-close">Tutup</button>
        </div>
      </div>
    `
    document.body.appendChild(overlay)
    const closeBtn = overlay.querySelector('#topbar-web-info-close')
    closeBtn?.addEventListener('click', () => overlay.classList.remove('open'))
    overlay.addEventListener('click', event => {
      if (event.target === overlay) overlay.classList.remove('open')
    })
  }
  const versionEl = overlay.querySelector('#topbar-web-desktop-version')
  const notesEl = overlay.querySelector('#topbar-web-desktop-notes')
  const downloadExeBtn = overlay.querySelector('#topbar-web-desktop-download-exe')
  const downloadMsiBtn = overlay.querySelector('#topbar-web-desktop-download-msi')
  const downloadApkBtn = overlay.querySelector('#topbar-web-mobile-download-apk')
  const downloadAabBtn = overlay.querySelector('#topbar-web-mobile-download-aab')
  if (!versionEl || !notesEl || !downloadExeBtn || !downloadMsiBtn || !downloadApkBtn || !downloadAabBtn) return

  const downloadState = {
    exe: '',
    msi: '',
    apk: '',
    aab: ''
  }

  if (infoBtn.dataset.boundDesktopInfo !== '1') {
    infoBtn.dataset.boundDesktopInfo = '1'
    infoBtn.addEventListener('click', () => {
      overlay.classList.add('open')
    })
  }
  if (downloadExeBtn.dataset.boundDesktopDownload !== '1') {
    downloadExeBtn.dataset.boundDesktopDownload = '1'
    downloadExeBtn.addEventListener('click', () => {
      const url = downloadState.exe || `https://github.com/timakademikmim/mim-app/releases/latest/download/MIM.App_${getStoredVersion() || '0.3.8'}_x64-setup.exe`
      window.open(url, '_blank', 'noopener,noreferrer')
    })
  }
  if (downloadMsiBtn.dataset.boundDesktopDownload !== '1') {
    downloadMsiBtn.dataset.boundDesktopDownload = '1'
    downloadMsiBtn.addEventListener('click', () => {
      const url = downloadState.msi || `https://github.com/timakademikmim/mim-app/releases/latest/download/MIM.App_${getStoredVersion() || '0.3.8'}_x64_en-US.msi`
      window.open(url, '_blank', 'noopener,noreferrer')
    })
  }
  if (downloadApkBtn.dataset.boundDesktopDownload !== '1') {
    downloadApkBtn.dataset.boundDesktopDownload = '1'
    downloadApkBtn.addEventListener('click', () => {
      const url = downloadState.apk || 'https://github.com/timakademikmim/mim-app/releases/latest/download/app-universal-release.apk'
      window.open(url, '_blank', 'noopener,noreferrer')
    })
  }
  if (downloadAabBtn.dataset.boundDesktopDownload !== '1') {
    downloadAabBtn.dataset.boundDesktopDownload = '1'
    downloadAabBtn.addEventListener('click', () => {
      const url = downloadState.aab || 'https://github.com/timakademikmim/mim-app/releases/latest/download/app-universal-release.aab'
      window.open(url, '_blank', 'noopener,noreferrer')
    })
  }

  try {
    const latestRes = await fetch(`https://github.com/timakademikmim/mim-app/releases/latest/download/latest.json?t=${Date.now()}`, { cache: 'no-store' })
    if (!latestRes.ok) throw new Error('latest.json not available')
    const latest = await latestRes.json()
    const fetchedVersion = normalizeVersion(latest?.version || '')
    const version = fetchedVersion || getStoredVersion() || '0.3.8'
    setStoredVersion(version)
    versionEl.textContent = `Versi desktop terbaru: v${version}`
    const platforms = latest?.platforms && typeof latest.platforms === 'object' ? latest.platforms : {}
    downloadState.exe = String(platforms?.['windows-x86_64-nsis']?.url || '').trim()
    downloadState.msi = String(platforms?.['windows-x86_64-msi']?.url || platforms?.['windows-x86_64']?.url || '').trim()
    if (latest?.mobile && typeof latest.mobile === 'object') {
      downloadState.apk = String(latest.mobile.apk || '').trim()
      downloadState.aab = String(latest.mobile.aab || '').trim()
    }
    try {
      const mobileRes = await fetch('https://github.com/timakademikmim/mim-app/releases/latest/download/mobile-latest.json', { cache: 'no-store' })
      if (mobileRes.ok) {
        const mobileLatest = await mobileRes.json()
        if (mobileLatest && typeof mobileLatest === 'object' && mobileLatest.mobile) {
          downloadState.apk = String(mobileLatest.mobile.apk || downloadState.apk || '').trim()
          downloadState.aab = String(mobileLatest.mobile.aab || downloadState.aab || '').trim()
        }
      }
    } catch (_error) {}
    let notes = String(latest?.notes || latest?.body || latest?.changelog || '').trim()
    if (!notes || isGenericNotes(notes) || !downloadState.apk || !downloadState.aab) {
      const releaseRes = await fetch(`https://api.github.com/repos/timakademikmim/mim-app/releases/tags/v${encodeURIComponent(version)}`, {
        cache: 'no-store',
        headers: { Accept: 'application/vnd.github+json' }
      })
      if (releaseRes.ok) {
        const release = await releaseRes.json()
        notes = String(release?.body || '').trim()
        const assets = Array.isArray(release?.assets) ? release.assets : []
        if (!downloadState.apk) {
          const apkAsset = assets.find(asset => String(asset?.name || '').toLowerCase().endsWith('.apk'))
          downloadState.apk = String(apkAsset?.browser_download_url || '').trim()
        }
        if (!downloadState.aab) {
          const aabAsset = assets.find(asset => String(asset?.name || '').toLowerCase().endsWith('.aab'))
          downloadState.aab = String(aabAsset?.browser_download_url || '').trim()
        }
      }
    }
    notesEl.textContent = (!notes || isGenericNotes(notes)) ? fallbackNotes(version) : notes
  } catch (_error) {
    const fallbackVersion = getStoredVersion() || await fetchLatestDesktopVersionFromApi() || '0.3.8'
    setStoredVersion(fallbackVersion)
    versionEl.textContent = `Versi desktop terbaru: v${fallbackVersion}`
    notesEl.textContent = fallbackNotes(fallbackVersion)
  }
}

async function initMobileInAppUpdatePrompt() {
  if (!isAndroidPlatform()) return
  if (document.getElementById('mobile-update-overlay')) return

  const normalizeVersion = normalizeVersionText

  const getCurrentVersion = async () => {
    const current = await getMobileCurrentVersionText()
    return normalizeVersion(current) || '0.0.0'
  }

  const getReleaseBodyByTag = async version => {
    try {
      const clean = normalizeVersion(version)
      if (!clean) return ''
      const res = await fetch(`https://api.github.com/repos/timakademikmim/mim-app/releases/tags/v${encodeURIComponent(clean)}`, {
        cache: 'no-store',
        headers: { Accept: 'application/vnd.github+json' }
      })
      if (!res.ok) return ''
      const json = await res.json()
      return String(json?.body || '').trim()
    } catch (_error) {
      return ''
    }
  }

  const mobileLatestUrl = `https://github.com/timakademikmim/mim-app/releases/latest/download/mobile-latest.json?t=${Date.now()}`
  let latestRes = await fetch(mobileLatestUrl, { cache: 'no-store' }).catch(() => null)
  if (!latestRes || !latestRes.ok) {
    const fallbackUrl = `https://github.com/timakademikmim/mim-app/releases/latest/download/latest.json?t=${Date.now()}`
    latestRes = await fetch(fallbackUrl, { cache: 'no-store' }).catch(() => null)
  }
  const latest = latestRes && latestRes.ok ? await latestRes.json().catch(() => null) : null
  const androidMeta = await fetchLatestAndroidReleaseMeta()
  let latestVersion = normalizeVersion(latest?.version || '')
  if (androidMeta.version && compareVersionText(androidMeta.version, latestVersion) > 0) {
    latestVersion = normalizeVersion(androidMeta.version)
  }
  const currentVersion = await getCurrentVersion()
  if (!latestVersion) return
  localStorage.setItem('mobile_app_version', currentVersion)

  if (compareVersionText(latestVersion, currentVersion) <= 0) return

  let apkUrl = ''
  if (latest?.mobile && typeof latest.mobile === 'object') {
    apkUrl = String(latest.mobile.apk || '').trim()
  }
  if (!apkUrl && latest?.platforms && typeof latest.platforms === 'object') {
    apkUrl = String(latest.platforms?.['android-arm64-apk']?.url || '').trim()
  }
  if (!apkUrl && androidMeta.apkUrl) apkUrl = androidMeta.apkUrl
  if (!apkUrl) apkUrl = 'https://github.com/timakademikmim/mim-app/releases/latest/download/app-universal-release.apk'

  let notes = String(latest?.notes || latest?.body || latest?.changelog || '').trim()
  if (!notes && androidMeta.notes) notes = androidMeta.notes
  if (!notes) notes = await getReleaseBodyByTag(latestVersion)
  if (!notes) {
    notes = `What's new in this version:\n- Peningkatan stabilitas dan pembaruan fitur aplikasi mobile.`
  }

  const styleId = 'mobile-update-style'
  if (!document.getElementById(styleId)) {
    const style = document.createElement('style')
    style.id = styleId
    style.textContent = `
      .mobile-update-overlay {
        position: fixed;
        inset: 0;
        z-index: 12500;
        background: rgba(15, 23, 42, 0.45);
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 16px;
      }
      .mobile-update-card {
        width: min(520px, calc(100vw - 24px));
        max-height: min(80vh, 720px);
        overflow: auto;
        background: #fff;
        border: 1px solid #cbd5e1;
        border-radius: 14px;
        box-shadow: 0 20px 40px rgba(15, 23, 42, 0.25);
        padding: 16px;
      }
      .mobile-update-title {
        margin: 0 0 6px;
        font-size: 18px;
        color: #0f172a;
      }
      .mobile-update-version {
        font-size: 12px;
        color: #475569;
        margin: 0 0 8px;
      }
      .mobile-update-notes {
        white-space: pre-wrap;
        font-size: 13px;
        color: #334155;
        line-height: 1.45;
      }
      .mobile-update-actions {
        margin-top: 14px;
        display: flex;
        gap: 8px;
        justify-content: flex-end;
      }
      .mobile-update-actions button {
        border: 1px solid #cbd5e1;
        border-radius: 10px;
        background: #fff;
        color: #0f172a;
        font-size: 12px;
        font-weight: 600;
        padding: 7px 12px;
        cursor: pointer;
      }
      .mobile-update-actions .primary {
        background: #0f172a;
        color: #fff;
        border-color: #0f172a;
      }
    `
    document.head.appendChild(style)
  }

  const overlay = document.createElement('div')
  overlay.id = 'mobile-update-overlay'
  overlay.className = 'mobile-update-overlay'
  overlay.innerHTML = `
    <div class="mobile-update-card">
      <h3 class="mobile-update-title">Versi baru tersedia</h3>
      <div class="mobile-update-version">Versi saat ini: v${currentVersion} | Versi terbaru: v${latestVersion}</div>
      <div class="mobile-update-notes">${notes}</div>
      <div class="mobile-update-actions">
        <button type="button" id="mobile-update-later">Nanti</button>
        <button type="button" id="mobile-update-install" class="primary">Install</button>
      </div>
    </div>
  `
  document.body.appendChild(overlay)

  overlay.querySelector('#mobile-update-later')?.addEventListener('click', () => {
    overlay.remove()
  })
  overlay.querySelector('#mobile-update-install')?.addEventListener('click', async () => {
    const installBtn = overlay.querySelector('#mobile-update-install')
    const originalLabel = installBtn?.textContent || 'Install'
    let loadingTick = 0
    let timer = 0
    const startLoading = () => {
      if (!installBtn) return
      installBtn.disabled = true
      installBtn.textContent = 'Mengunduh'
      timer = window.setInterval(() => {
        loadingTick = (loadingTick + 1) % 4
        installBtn.textContent = `Mengunduh${'.'.repeat(loadingTick)}`
      }, 280)
    }
    const stopLoading = () => {
      if (timer) window.clearInterval(timer)
      if (!installBtn) return
      installBtn.disabled = false
      installBtn.textContent = originalLabel
    }
    startLoading()
    const inferredName = `MIM-App-Android-v${latestVersion}.apk`
    try {
      if (typeof window.downloadToAndroidAppStorage === 'function') {
        const saved = await window.downloadToAndroidAppStorage(apkUrl, inferredName)
        if (saved?.ok) {
          stopLoading()
          let opened = false
          try {
            await invokeTauriCommand('open_file_path', { path: String(saved.path || '') })
            opened = true
          } catch (_error) {}
          alert(opened
            ? `File APK tersimpan. Installer dibuka otomatis.\n\nLokasi:\n${saved.path}`
            : `File APK tersimpan di:\n${saved.path}`)
          overlay.remove()
          return
        }
      }
    } catch (_error) {}
    try {
      if (typeof window.openExternalUrl === 'function') {
        const opened = await window.openExternalUrl(apkUrl)
        if (opened) {
          stopLoading()
          return
        }
      }
      window.location.href = apkUrl
    } catch (_error) {
      stopLoading()
      window.location.href = apkUrl
    }
  })
}

function initDesktopUpdaterUi() {
  const isDesktopApp = !!(window.__TAURI_INTERNALS__ || window.__TAURI__)
  if (!isDesktopApp) return

  const GENERIC_RELEASE_NOTE_PATTERNS = [
    'desktop release otomatis dengan updater artifacts.'
  ]
  const VERSION_WHATS_NEW = {
    '0.3.19': `What's new in this version:
- Card agenda dashboard Android diperkecil lagi agar proporsional dengan layar HP.
- Pembukaan installer APK otomatis diperkuat dengan intent INSTALL_PACKAGE + fallback VIEW.`,
    '0.3.18': `What's new in this version:
- Setelah unduh APK selesai, aplikasi sekarang otomatis mencoba membuka installer APK.
- Alur update Android dipercepat agar pengguna tidak perlu cari file manual di File Manager.`,
    '0.3.17': `What's new in this version:
- Download Android ditingkatkan: prioritas simpan ke folder Download publik, fallback ke folder aplikasi jika diperlukan.
- Tombol download pada popup info Android kini menampilkan animasi loading agar proses terlihat jelas.
- Cetak laporan bulanan Android kini disimpan sebagai file PDF ke folder download melalui command native.
- Sinkron avatar Android diperkuat agar foto profil lebih konsisten tampil.`,
    '0.3.16': `What's new in this version:
- Perbaikan build Android: path resolver Tauri diperbaiki agar rilis APK tidak gagal compile.
- Stabilitas proses rilis Android ditingkatkan setelah penyesuaian command native.`,
    '0.3.15': `What's new in this version:
- Jarak aman topbar Android ditambah agar tombol tidak mepet ke ujung atas layar.
- Download APK Android diperbarui: file bisa disimpan ke folder lokal aplikasi (AppData/Downloads).
- Sinkron avatar Android diperbaiki agar foto profil ikut tampil di bottom navigation.
- Deklarasi izin Android untuk instal paket/akses storage kompatibel ditambahkan.`,
    '0.3.14': `What's new in this version:
- Perbaikan layout Android: topbar menempel di atas dan tidak ikut masuk ke kontainer konten.
- Perbaikan drawer Android: posisi sidebar/logo lebih stabil agar tidak terpotong.
- Perbaikan buka tautan eksternal Android (download APK) dengan fallback intent.
- Perbaikan cetak PDF Android dengan fallback buka PDF ke viewer eksternal.`,
    '0.3.8': `What's new in this version:
- Alur update Android dan Desktop dipisah (Android: mobile-latest.json, Desktop: latest.json).
- Asset rilis Android kini otomatis terupload bersama metadata mobile-latest.json.
- Build pipeline lintas platform diperbaiki agar runner Linux tidak gagal karena PowerShell.`,
    '0.3.7': `What's new in this version:
- Android release kini terotomatisasi: APK ikut terupload saat tag rilis dibuat.
- Asset APK updater distandarkan ke app-universal-release.apk agar update in-app lebih konsisten.
- Stabilitas updater desktop ditingkatkan.`,
    '0.3.6': `What's new in this version:
- Perbaikan updater desktop agar status "pembaruan berjalan" tidak stuck berkepanjangan.
- Lock update otomatis dilepas pada kondisi siap restart / error / timeout watchdog.
- Stabilitas alur update desktop ditingkatkan untuk mencegah freeze di antarmuka.`,
    '0.3.5': `What's new in this version:
- Perbaikan checker update Android agar tetap mendeteksi versi baru meski versi lokal belum terbaca.
- Cache-busting pada pembacaan latest.json supaya tidak tertahan data versi lama.`,
    '0.3.4': `What's new in this version:
- Android update prompt hanya muncul jika ada versi yang benar-benar lebih baru.
- Tautan unduhan Android diarahkan ke paket ARM64 yang lebih ringan untuk install manual.
- Catatan rilis fallback diperjelas agar popup Info tetap informatif.`,
    '0.3.0': `What's new in this version:
- Cincin status avatar sekarang tampil permanen (biru saat online, merah saat offline).
- Menu akun menambahkan tombol Info di bawah Profil.
- Popup Info rilis dirapikan agar catatan versi lebih jelas.`,
    '0.2.9': `What's new in this version:
- Info rilis dipindahkan ke menu avatar di topbar.
- Indikator online/offline dipindah ke cincin di sekitar avatar.`
  }

  const releaseInfoState = {
    currentVersion: String(localStorage.getItem('desktop_app_version') || '').trim(),
    latestVersion: '',
    notes: '',
    notesByVersion: {}
  }
  let updaterLockWatchdog = null

  let infoBtn = null
  let avatarEl = null
  let avatarTriggerEl = null
  let changelogOverlay = null
  let changelogBody = null
  let updateLockOverlay = null
  let lastOnlineState = null

  function cleanVersion(version) {
    return String(version || '').trim().replace(/^v/i, '')
  }

  function isGenericReleaseNotes(text) {
    const value = String(text || '').trim().toLowerCase()
    if (!value) return true
    return GENERIC_RELEASE_NOTE_PATTERNS.includes(value)
  }

  function getFallbackReleaseNotes(version) {
    const v = cleanVersion(version)
    if (VERSION_WHATS_NEW[v]) return VERSION_WHATS_NEW[v]
    return `What's new in this version:
- Pembaruan stabilitas dan penyempurnaan antarmuka.
- Perbaikan minor pada pengalaman penggunaan desktop.`
  }

  function ensureUpdaterStyle() {
    if (document.getElementById('desktop-updater-style')) return
    const style = document.createElement('style')
    style.id = 'desktop-updater-style'
    style.textContent = `
      .topbar-avatar.desktop-online {
        box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.9), 0 0 0 4px rgba(37, 99, 235, 0.24);
      }
      .topbar-avatar.desktop-offline {
        box-shadow: 0 0 0 2px rgba(220, 38, 38, 0.9), 0 0 0 4px rgba(220, 38, 38, 0.24);
      }
      .topbar-avatar-trigger.desktop-online .topbar-avatar {
        box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.9), 0 0 0 4px rgba(37, 99, 235, 0.24);
      }
      .topbar-avatar-trigger.desktop-offline .topbar-avatar {
        box-shadow: 0 0 0 2px rgba(220, 38, 38, 0.9), 0 0 0 4px rgba(220, 38, 38, 0.24);
      }
      .desktop-release-overlay {
        position: fixed;
        inset: 0;
        z-index: 12100;
        display: none;
        align-items: center;
        justify-content: center;
        background: rgba(15, 23, 42, 0.4);
        padding: 16px;
      }
      .desktop-release-overlay.open {
        display: flex;
      }
      .desktop-release-card {
        width: min(520px, calc(100vw - 24px));
        max-height: min(78vh, 620px);
        overflow: auto;
        background: #fff;
        border: 1px solid #cbd5e1;
        border-radius: 14px;
        box-shadow: 0 20px 40px rgba(15, 23, 42, 0.2);
        padding: 16px;
      }
      .desktop-release-card h3 {
        margin: 0 0 8px;
        font-size: 16px;
        color: #0f172a;
      }
      .desktop-release-meta {
        font-size: 12px;
        color: #475569;
        margin-bottom: 10px;
      }
      .desktop-release-body {
        font-size: 12px;
        color: #334155;
        white-space: pre-wrap;
        line-height: 1.45;
      }
      .desktop-release-actions {
        margin-top: 12px;
        display: flex;
        justify-content: flex-end;
      }
      .desktop-release-actions button {
        border: 1px solid #cbd5e1;
        border-radius: 10px;
        background: #fff;
        color: #0f172a;
        font-size: 12px;
        font-weight: 600;
        padding: 6px 12px;
        cursor: pointer;
      }
      .desktop-updater-lock {
        position: fixed;
        inset: 0;
        z-index: 12000;
        display: none;
        align-items: center;
        justify-content: center;
        background: rgba(15, 23, 42, 0.42);
      }
      .desktop-updater-lock.active {
        display: flex;
      }
      .desktop-updater-lock-card {
        width: min(460px, calc(100vw - 24px));
        background: #ffffff;
        border: 1px solid #cbd5e1;
        border-radius: 14px;
        padding: 18px;
        box-shadow: 0 20px 40px rgba(15, 23, 42, 0.2);
      }
      .desktop-updater-lock-title {
        margin: 0 0 6px;
        font-size: 16px;
        font-weight: 700;
        color: #0f172a;
      }
      .desktop-updater-lock-note {
        margin: 0;
        font-size: 13px;
        color: #334155;
      }
    `
    document.head.appendChild(style)
  }

  function ensureUpdaterElements() {
    ensureUpdaterStyle()
    if (!avatarEl) avatarEl = document.getElementById('topbar-avatar')
    if (!avatarTriggerEl) avatarTriggerEl = document.querySelector('.topbar-user-trigger')
    if (!infoBtn) infoBtn = document.getElementById('topbar-app-info-btn')
    if (!changelogOverlay) {
      changelogOverlay = document.createElement('div')
      changelogOverlay.className = 'desktop-release-overlay'
      changelogOverlay.innerHTML = `
        <div class="desktop-release-card">
          <h3>Info Versi Aplikasi</h3>
          <div class="desktop-release-meta" id="desktop-release-meta">Versi aplikasi</div>
          <div class="desktop-release-body" id="desktop-release-body">Belum ada catatan perubahan.</div>
          <div class="desktop-release-actions">
            <button type="button" id="desktop-release-close">Tutup</button>
          </div>
        </div>
      `
      changelogBody = changelogOverlay.querySelector('#desktop-release-body')
      const closeBtn = changelogOverlay.querySelector('#desktop-release-close')
      closeBtn?.addEventListener('click', () => changelogOverlay.classList.remove('open'))
      changelogOverlay.addEventListener('click', event => {
        if (event.target === changelogOverlay) changelogOverlay.classList.remove('open')
      })
      document.body.appendChild(changelogOverlay)
    }
    if (!updateLockOverlay) {
      updateLockOverlay = document.createElement('div')
      updateLockOverlay.className = 'desktop-updater-lock'
      updateLockOverlay.innerHTML = `
        <div class="desktop-updater-lock-card">
          <div class="desktop-updater-lock-title">Pembaruan aplikasi sedang berjalan</div>
          <p class="desktop-updater-lock-note">Untuk mencegah bentrok data, input dinonaktifkan sementara hingga update selesai.</p>
        </div>
      `
      document.body.appendChild(updateLockOverlay)
    }
    if (!window.openDesktopReleaseInfo) {
      window.openDesktopReleaseInfo = async function openDesktopReleaseInfo() {
      await hydrateLatestReleaseInfo()
      syncVersionFromStorage()
      const meta = changelogOverlay?.querySelector('#desktop-release-meta')
      if (meta) {
        const current = releaseInfoState.currentVersion || '-'
        const latest = releaseInfoState.latestVersion || current
        meta.textContent = `Versi saat ini: v${current} | Versi terbaru: v${latest}`
      }
      const noteVersion = releaseInfoState.currentVersion || releaseInfoState.latestVersion
      if (noteVersion) await ensureReleaseNotesForVersion(noteVersion)
      if (changelogBody) {
        const resolvedNote = noteVersion ? (releaseInfoState.notesByVersion[noteVersion] || '') : ''
        changelogBody.textContent = resolvedNote || releaseInfoState.notes || getFallbackReleaseNotes(noteVersion)
      }
      changelogOverlay?.classList.add('open')
      }
    }
    if (infoBtn && infoBtn.dataset.boundInfoClick !== '1') {
      infoBtn.dataset.boundInfoClick = '1'
      infoBtn.addEventListener('click', () => {
        if (typeof window.openDesktopReleaseInfo === 'function') window.openDesktopReleaseInfo()
      })
    }
  }

  function setOnlineIndicator() {
    ensureUpdaterElements()
    const isOnline = navigator.onLine
    lastOnlineState = isOnline
    avatarEl = document.getElementById('topbar-avatar')
    avatarTriggerEl = document.querySelector('.topbar-user-trigger')
    if (!avatarEl && !avatarTriggerEl) return
    avatarEl?.classList.toggle('desktop-online', isOnline)
    avatarEl?.classList.toggle('desktop-offline', !isOnline)
    avatarTriggerEl?.classList.toggle('desktop-online', isOnline)
    avatarTriggerEl?.classList.toggle('desktop-offline', !isOnline)
  }

  function bindAvatarPersistence() {
    const host = document.querySelector('.topbar-user-menu-wrap') || document.body
    if (!host || host.dataset.desktopStatusObserver === '1') return
    host.dataset.desktopStatusObserver = '1'
    const observer = new MutationObserver(() => {
      if (typeof lastOnlineState === 'boolean') {
        setOnlineIndicator()
      }
    })
    observer.observe(host, { childList: true, subtree: true })
  }

  function syncVersionFromStorage() {
    if (!releaseInfoState.currentVersion) {
      const stored = String(localStorage.getItem('desktop_app_version') || '').trim()
      if (stored) releaseInfoState.currentVersion = stored
    }
    if (!releaseInfoState.currentVersion && releaseInfoState.latestVersion) {
      releaseInfoState.currentVersion = releaseInfoState.latestVersion
    }
    if (!releaseInfoState.latestVersion && releaseInfoState.currentVersion) {
      releaseInfoState.latestVersion = releaseInfoState.currentVersion
    }
    if (releaseInfoState.currentVersion) {
      localStorage.setItem('desktop_app_version', releaseInfoState.currentVersion)
    }
  }

  async function hydrateLatestReleaseInfo() {
    try {
      const res = await fetch('https://github.com/timakademikmim/mim-app/releases/latest/download/latest.json', { cache: 'no-store' })
      if (!res.ok) return
      const info = await res.json()
      if (info && typeof info === 'object') {
        const latestVersion = String(info.version || '').trim()
        const notes = String(info.notes || info.body || info.changelog || info.releaseNotes || '').trim()
        if (latestVersion) releaseInfoState.latestVersion = latestVersion
        if (notes && !isGenericReleaseNotes(notes)) {
          releaseInfoState.notes = notes
          if (latestVersion) releaseInfoState.notesByVersion[latestVersion] = notes
        } else if (latestVersion) {
          const fallback = getFallbackReleaseNotes(latestVersion)
          releaseInfoState.notes = fallback
          releaseInfoState.notesByVersion[latestVersion] = fallback
        }
        if (!releaseInfoState.currentVersion && latestVersion) releaseInfoState.currentVersion = latestVersion
        if (latestVersion) await ensureReleaseNotesForVersion(latestVersion)
        renderVersionLabel()
      }
    } catch (_error) {}
  }

  async function fetchReleaseBodyByTag(version) {
    const clean = cleanVersion(version)
    if (!clean) return ''
    try {
      const res = await fetch(`https://api.github.com/repos/timakademikmim/mim-app/releases/tags/v${encodeURIComponent(clean)}`, {
        cache: 'no-store',
        headers: { Accept: 'application/vnd.github+json' }
      })
      if (!res.ok) return ''
      const info = await res.json()
      const body = String(info?.body || '').trim()
      if (isGenericReleaseNotes(body)) return ''
      return body
    } catch (_error) {
      return ''
    }
  }

  async function ensureReleaseNotesForVersion(version) {
    const clean = cleanVersion(version)
    if (!clean) return
    if (releaseInfoState.notesByVersion[clean]) return
    const notes = await fetchReleaseBodyByTag(clean)
    if (notes) {
      releaseInfoState.notesByVersion[clean] = notes
      if (releaseInfoState.latestVersion === clean || !releaseInfoState.notes) {
        releaseInfoState.notes = notes
      }
      return
    }
    const fallback = getFallbackReleaseNotes(clean)
    releaseInfoState.notesByVersion[clean] = fallback
    if (releaseInfoState.latestVersion === clean && releaseInfoState.notes) {
      releaseInfoState.notesByVersion[clean] = isGenericReleaseNotes(releaseInfoState.notes)
        ? fallback
        : releaseInfoState.notes
    }
  }

  function renderVersionLabel() {
    ensureUpdaterElements()
    syncVersionFromStorage()
    if (infoBtn) infoBtn.textContent = 'Info'
  }

  function updateDesktopUpdaterUi(detail) {
    ensureUpdaterElements()
    const stage = String(detail?.stage || '').trim().toLowerCase()
    const currentVersion = String(detail?.currentVersion || '').trim()
    const latestVersion = String(detail?.latestVersion || '').trim()
    const notes = String(detail?.notes || '').trim()
    if (currentVersion) releaseInfoState.currentVersion = currentVersion
    if (latestVersion) releaseInfoState.latestVersion = latestVersion
    if (notes && !isGenericReleaseNotes(notes)) {
      releaseInfoState.notes = notes
      if (latestVersion) releaseInfoState.notesByVersion[latestVersion] = notes
    } else if (latestVersion) {
      const fallback = getFallbackReleaseNotes(latestVersion)
      releaseInfoState.notes = fallback
      releaseInfoState.notesByVersion[latestVersion] = fallback
    }
    if (releaseInfoState.currentVersion) localStorage.setItem('desktop_app_version', releaseInfoState.currentVersion)
    renderVersionLabel()

    if (updaterLockWatchdog) {
      clearTimeout(updaterLockWatchdog)
      updaterLockWatchdog = null
    }

    const isLock = stage === 'downloading' || stage === 'installing'
    updateLockOverlay.classList.toggle('active', isLock)
    if (isLock) {
      updaterLockWatchdog = setTimeout(() => {
        updateLockOverlay?.classList.remove('active')
      }, 120000)
    }

    if (stage === 'no_update') {
      updateLockOverlay.classList.remove('active')
      return
    }

    if (stage === 'error') {
      updateLockOverlay.classList.remove('active')
      return
    }

    if (stage === 'ready_restart') {
      updateLockOverlay.classList.remove('active')
    }
  }

  ensureUpdaterElements()
  syncVersionFromStorage()
  bindAvatarPersistence()
  setOnlineIndicator()
  renderVersionLabel()
  hydrateLatestReleaseInfo()
  window.addEventListener('online', setOnlineIndicator)
  window.addEventListener('offline', setOnlineIndicator)
  window.addEventListener('desktop-updater-status', event => {
    updateDesktopUpdaterUi(event?.detail || {})
  })
}

async function invokeTauriCommand(command, payload = {}) {
  if (window.__TAURI__?.core?.invoke) {
    return window.__TAURI__.core.invoke(command, payload)
  }
  if (window.__TAURI_INTERNALS__?.invoke) {
    return window.__TAURI_INTERNALS__.invoke(command, payload)
  }
  throw new Error('Tauri invoke tidak tersedia di window.')
}

window.openExternalUrl = async function openExternalUrl(url) {
  const target = String(url || '').trim()
  if (!target) return false
  const isTauriApp = !!(window.__TAURI_INTERNALS__ || window.__TAURI__)
  const isAndroidApp = /android/i.test(String(navigator.userAgent || ''))
  if (!isTauriApp) {
    const popup = window.open(target, '_blank', 'noopener,noreferrer')
    return !!popup
  }
  try {
    const opened = await invokeTauriCommand('open_external_url', { url: target })
    if (opened === false) throw new Error('Native open_external_url mengembalikan status gagal.')
    return true
  } catch (error) {
    console.error('openExternalUrl invoke failed:', error)
  }
  if (isTauriApp && isAndroidApp) {
    // Avoid WebView intent navigation on Android Tauri because it triggers
    // ERR_UNKNOWN_URL_SCHEME. Rely on native open_external_url instead.
    return false
  }
  if (isAndroidApp) {
    try {
      const popup = window.open(target, '_blank', 'noopener,noreferrer')
      if (popup) return true
    } catch (_error) {}
    try {
      const a = document.createElement('a')
      a.href = target
      a.target = '_blank'
      a.rel = 'noopener noreferrer'
      document.body.appendChild(a)
      a.click()
      a.remove()
      return true
    } catch (_error) {}
    try {
      window.location.assign(target)
      return true
    } catch (_error) {
      try {
        window.location.href = target
        return true
      } catch (__error) {
        return false
      }
    }
  }
  try {
    const popup = window.open(target, '_blank', 'noopener,noreferrer')
    return !!popup
  } catch (_error) {
    return false
  }
}

function safeChatId(value) {
  return String(value || '').trim()
}

function getChatNotifyStorageKey(userId) {
  return `chat_notify_state:${userId}`
}

function getChatNotifyOpenKey(userId) {
  return `chat_notify_open:${userId}`
}

function loadChatNotifyState(userId) {
  const key = getChatNotifyStorageKey(userId)
  try {
    const raw = localStorage.getItem(key)
    if (!raw) return { lastTs: 0, lastId: '' }
    const parsed = JSON.parse(raw)
    return {
      lastTs: Number(parsed?.lastTs || 0) || 0,
      lastId: String(parsed?.lastId || '')
    }
  } catch (_error) {
    return { lastTs: 0, lastId: '' }
  }
}

function saveChatNotifyState(userId, nextState) {
  const key = getChatNotifyStorageKey(userId)
  try {
    localStorage.setItem(key, JSON.stringify(nextState || {}))
  } catch (_error) {}
}

function loadChatNotifyOpen(userId) {
  const key = getChatNotifyOpenKey(userId)
  try {
    const raw = localStorage.getItem(key)
    if (!raw) return null
    const parsed = JSON.parse(raw)
    return {
      threadId: safeChatId(parsed?.threadId || ''),
      ts: Number(parsed?.ts || 0) || 0
    }
  } catch (_error) {
    return null
  }
}

function saveChatNotifyOpen(userId, threadId) {
  const key = getChatNotifyOpenKey(userId)
  try {
    localStorage.setItem(key, JSON.stringify({ threadId: safeChatId(threadId), ts: Date.now() }))
  } catch (_error) {}
}

function clearChatNotifyOpen(userId) {
  const key = getChatNotifyOpenKey(userId)
  try {
    localStorage.removeItem(key)
  } catch (_error) {}
}

function openChatPageFromNotification(forcedThreadId = '') {
  const directThreadId = safeChatId(forcedThreadId || '')
  const uid = safeChatId(id)
  const pending = uid ? loadChatNotifyOpen(uid) : null
  const threadId = safeChatId(directThreadId || pending?.threadId || '')
  if (!threadId) return false
  if (pending?.ts) {
    const ageMs = Date.now() - pending.ts
    if (ageMs > 10 * 60 * 1000) {
      clearChatNotifyOpen(uid)
      return false
    }
  }
  try {
    localStorage.setItem(CHAT_OPEN_STORAGE_KEY, threadId)
  } catch (_error) {}
  const opened = tryOpenChatRoute()
  if (!opened) {
    scheduleChatOpenRetry(threadId)
    return false
  }
  if (uid) clearChatNotifyOpen(uid)
  if (typeof window.updateAndroidBottomNavActive === 'function') {
    window.updateAndroidBottomNavActive('chat')
  }
  return true
}

function openChatPanelFromNotification() {
  const opened = tryOpenChatRoute()
  if (!opened) {
    window.setTimeout(() => {
      tryOpenChatRoute()
    }, CHAT_OPEN_RETRY_DELAY)
    return false
  }
  if (typeof window.updateAndroidBottomNavActive === 'function') {
    window.updateAndroidBottomNavActive('chat')
  }
  return true
}

if (!window.__chatNotifyOpenHandlerBound) {
  window.__chatNotifyOpenHandlerBound = true
  window.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') {
      openChatPageFromNotification()
    }
  })
  window.addEventListener('focus', () => {
    openChatPageFromNotification()
  })
}

window.addEventListener('mim-open-chat-thread', event => {
  const tid = safeChatId(event?.detail?.threadId || '')
  if (!tid) return
  try {
    localStorage.setItem(CHAT_OPEN_STORAGE_KEY, tid)
  } catch (_error) {}
  if (window.ChatModule && typeof window.ChatModule.openThread === 'function') {
    window.ChatModule.openThread(tid)
    return
  }
  saveChatNotifyOpen(id, tid)
  openChatPageFromNotification(tid)
})

window.addEventListener('mim-open-chat-panel', () => {
  openChatPanelFromNotification()
})

window.addEventListener('load', () => {
  const pendingThread = safeChatId(localStorage.getItem(CHAT_OPEN_STORAGE_KEY) || '')
  if (pendingThread) {
    openChatPageFromNotification(pendingThread)
  }
})

window.showLocalNotification = async function showLocalNotification(title, body, options = {}) {
  const isTauriApp = !!(window.__TAURI_INTERNALS__ || window.__TAURI__)
  const isAndroidApp = /android/i.test(String(navigator.userAgent || ''))
  if (!isTauriApp || !isAndroidApp) return false
  const threadId = safeChatId(options?.threadId || '')
  const userId = safeChatId(options?.userId || '')
  try {
    if (threadId) {
      const chatResult = await invokeTauriCommand('show_chat_notification', {
        title: String(title || '').trim(),
        body: String(body || '').trim(),
        threadId,
        userId
      })
      if (chatResult === true) return true
    }
    const result = await invokeTauriCommand('show_local_notification', {
      title: String(title || '').trim(),
      body: String(body || '').trim()
    })
    return result === true
  } catch (error) {
    console.error('showLocalNotification failed:', error)
    return false
  }
}

window.maybeNotifyChatMessage = async function maybeNotifyChatMessage({ userId, latestIncoming }) {
  const uid = safeChatId(userId)
  if (!uid || !latestIncoming) return false
  const messageId = safeChatId(latestIncoming.id || latestIncoming.message_id)
  const threadId = safeChatId(latestIncoming.thread_id || latestIncoming.threadId)
  const messageText = String(latestIncoming.message_text || latestIncoming.text || '').trim()
  const createdAt = String(latestIncoming.created_at || latestIncoming.createdAt || '').trim()
  const ts = Number.isFinite(latestIncoming.ts)
    ? Number(latestIncoming.ts)
    : Date.parse(createdAt)
  if (!Number.isFinite(ts) || ts <= 0) return false

  const state = loadChatNotifyState(uid)
  if ((state.lastTs || 0) >= ts) return false
  if (state.lastId && messageId && state.lastId === messageId) return false

  const chatState = window.__chatModuleActiveState
  const isViewingThread = !!(
    chatState &&
    safeChatId(chatState.selectedThreadId) === threadId &&
    (!chatState.mobileChatView || chatState.mobileChatView === 'thread')
  )

  saveChatNotifyState(uid, { lastTs: ts, lastId: messageId || state.lastId || '' })

  if (isViewingThread) return false

  const title = 'Pesan baru'
  let body = messageText || 'Anda menerima pesan baru.'
  if (body.startsWith('[[sticker]]')) body = 'Sticker'
  if (body.length > 160) body = `${body.slice(0, 157)}...`
  if (document.hidden) {
    saveChatNotifyOpen(uid, threadId)
  }
  await window.showLocalNotification(title, body, { threadId, userId: uid })
  return true
}

window.downloadToAndroidAppStorage = async function downloadToAndroidAppStorage(url, fileName = '') {
  const target = String(url || '').trim()
  if (!target) return { ok: false, path: '', error: 'URL kosong.' }
  const isAndroidApp = /android/i.test(String(navigator.userAgent || ''))
  if (!isAndroidApp) return { ok: false, path: '', error: 'Bukan mode Android.' }
  try {
    const savedPath = await invokeTauriCommand('download_url_to_app_storage', {
      url: target,
      fileName: String(fileName || '').trim()
    })
    return { ok: true, path: String(savedPath || '') }
  } catch (error) {
    return { ok: false, path: '', error: String(error?.message || error || 'Gagal menyimpan file.') }
  }
}

window.saveBase64ToAndroidDownloads = async function saveBase64ToAndroidDownloads(fileName, base64Data) {
  const isAndroidApp = /android/i.test(String(navigator.userAgent || ''))
  if (!isAndroidApp) return { ok: false, path: '', error: 'Bukan mode Android.' }
  try {
    const savedPath = await invokeTauriCommand('save_base64_to_downloads', {
      fileName: String(fileName || '').trim(),
      base64Data: String(base64Data || '').trim()
    })
    return { ok: true, path: String(savedPath || '') }
  } catch (error) {
    return { ok: false, path: '', error: String(error?.message || error || 'Gagal menyimpan file.') }
  }
}

window.printPdfBlobInPlace = async function printPdfBlobInPlace(blob) {
  if (!(blob instanceof Blob)) return false
  if (/android/i.test(String(navigator.userAgent || ''))) return false
  try {
    const url = URL.createObjectURL(blob)
    const frame = document.createElement('iframe')
    frame.style.position = 'fixed'
    frame.style.width = '1px'
    frame.style.height = '1px'
    frame.style.opacity = '0'
    frame.style.pointerEvents = 'none'
    frame.style.bottom = '0'
    frame.style.right = '0'
    frame.src = url
    document.body.appendChild(frame)

    await new Promise(resolve => {
      let done = false
      const cleanup = () => {
        if (done) return
        done = true
        setTimeout(() => {
          try {
            document.body.removeChild(frame)
          } catch (_err) {}
          URL.revokeObjectURL(url)
        }, 2500)
        resolve()
      }
      frame.onload = () => {
        try {
          frame.contentWindow?.focus?.()
          frame.contentWindow?.print?.()
        } catch (_err) {}
        cleanup()
      }
      setTimeout(cleanup, 2200)
    })
    return true
  } catch (error) {
    console.error('printPdfBlobInPlace failed:', error)
    return false
  }
}

window.savePdfDesktopAndOpen = async function savePdfDesktopAndOpen(blob, fileName) {
  if (!(blob instanceof Blob)) return { ok: false, path: '', error: 'Blob tidak valid.' }
  const isDesktopApp = !!(window.__TAURI_INTERNALS__ || window.__TAURI__)
  if (!isDesktopApp) {
    return { ok: false, path: '', error: 'Bukan mode desktop.' }
  }
  try {
    const bytes = new Uint8Array(await blob.arrayBuffer())
    let binary = ''
    const chunk = 0x8000
    for (let i = 0; i < bytes.length; i += chunk) {
      binary += String.fromCharCode(...bytes.subarray(i, i + chunk))
    }
    const base64Data = btoa(binary)
    const savedPath = await invokeTauriCommand('save_pdf_base64', {
      fileName: String(fileName || 'Dokumen.pdf'),
      base64Data
    })
    await invokeTauriCommand('open_file_path', { path: String(savedPath || '') })
    return { ok: true, path: String(savedPath || '') }
  } catch (error) {
    console.error('savePdfDesktopAndOpen failed:', error)
    return { ok: false, path: '', error: String(error?.message || error || 'Unknown error') }
  }
}

window.showSavedFilePopup = async function showSavedFilePopup(options = {}) {
  const filePath = String(options.path || '').trim()
  const title = String(options.title || 'File berhasil disimpan')
  const message = String(options.message || 'Pilih aksi untuk file yang baru disimpan.')
  const fallbackUrl = String(options.fallbackUrl || '').trim()
  if (!filePath) return

  if (!document.getElementById('saved-file-popup-style')) {
    const style = document.createElement('style')
    style.id = 'saved-file-popup-style'
    style.textContent = `
      .saved-file-overlay {
        position: fixed;
        inset: 0;
        z-index: 14000;
        background: rgba(15, 23, 42, 0.38);
        display: none;
        align-items: center;
        justify-content: center;
        padding: 16px;
      }
      .saved-file-overlay.open {
        display: flex;
      }
      .saved-file-card {
        width: min(460px, calc(100vw - 24px));
        background: #fff;
        border: 1px solid #cbd5e1;
        border-radius: 14px;
        box-shadow: 0 20px 40px rgba(15, 23, 42, 0.2);
        padding: 14px;
      }
      .saved-file-title {
        margin: 0 0 6px;
        font-size: 16px;
        font-weight: 700;
        color: #0f172a;
      }
      .saved-file-message {
        margin: 0 0 8px;
        font-size: 13px;
        color: #334155;
      }
      .saved-file-path {
        margin: 0;
        font-size: 12px;
        color: #0f172a;
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        border-radius: 8px;
        padding: 8px;
        word-break: break-word;
      }
      .saved-file-actions {
        margin-top: 12px;
        display: flex;
        justify-content: flex-end;
        gap: 8px;
      }
      .saved-file-actions button {
        border: 1px solid #cbd5e1;
        border-radius: 9px;
        padding: 6px 12px;
        background: #fff;
        color: #0f172a;
        font-size: 12px;
        font-weight: 600;
        cursor: pointer;
      }
      .saved-file-actions .saved-file-open-btn {
        border-color: #0ea5e9;
        background: #e0f2fe;
        color: #075985;
      }
    `
    document.head.appendChild(style)
  }

  let overlay = document.getElementById('saved-file-overlay')
  if (!overlay) {
    overlay = document.createElement('div')
    overlay.id = 'saved-file-overlay'
    overlay.className = 'saved-file-overlay'
    overlay.innerHTML = `
      <div class="saved-file-card">
        <h3 class="saved-file-title" id="saved-file-title"></h3>
        <p class="saved-file-message" id="saved-file-message"></p>
        <p class="saved-file-path" id="saved-file-path"></p>
        <div class="saved-file-actions">
          <button type="button" class="saved-file-close-btn" id="saved-file-close-btn">Tutup</button>
          <button type="button" class="saved-file-open-btn" id="saved-file-open-btn">Buka</button>
        </div>
      </div>
    `
    document.body.appendChild(overlay)
    overlay.addEventListener('click', event => {
      if (event.target === overlay) overlay.classList.remove('open')
    })
  }

  const titleEl = overlay.querySelector('#saved-file-title')
  const messageEl = overlay.querySelector('#saved-file-message')
  const pathEl = overlay.querySelector('#saved-file-path')
  const closeBtn = overlay.querySelector('#saved-file-close-btn')
  const openBtn = overlay.querySelector('#saved-file-open-btn')
  if (!titleEl || !messageEl || !pathEl || !closeBtn || !openBtn) return

  titleEl.textContent = title
  messageEl.textContent = message
  pathEl.textContent = filePath
  overlay.classList.add('open')

  const close = () => overlay.classList.remove('open')
  closeBtn.onclick = close
  openBtn.onclick = async () => {
    try {
      await invokeTauriCommand('open_file_path', { path: filePath })
      close()
      return
    } catch (_error) {}
    if (fallbackUrl && typeof window.openExternalUrl === 'function') {
      try {
        const opened = await window.openExternalUrl(fallbackUrl)
        if (opened) {
          close()
          return
        }
      } catch (_error) {}
    }
    alert(`File tersimpan di:\n${filePath}`)
    close()
  }
}

window.setTopbarUserIdentity = function setTopbarUserIdentity(nameOrObject, maybeFotoUrl) {
  let name = ''
  let fotoUrl = ''
  if (typeof nameOrObject === 'object' && nameOrObject !== null) {
    name = String(nameOrObject.name || nameOrObject.nama || '').trim()
    fotoUrl = String(nameOrObject.fotoUrl || nameOrObject.photoUrl || nameOrObject.foto_url || '').trim()
  } else {
    name = String(nameOrObject || '').trim()
    fotoUrl = String(maybeFotoUrl || '').trim()
  }
  if (name) localStorage.setItem('login_name', name)
  if (fotoUrl) localStorage.setItem('login_photo_url', fotoUrl)
  if (!fotoUrl) localStorage.removeItem('login_photo_url')

  const accountNameEl = document.getElementById('topbar-account-name')
  if (accountNameEl && name) accountNameEl.textContent = name
  const effectiveName = name || String(localStorage.getItem('login_name') || '').trim() || id
  setTopbarAvatar(effectiveName, fotoUrl)
  syncAndroidBottomAvatar(effectiveName, fotoUrl)
}

applyPlatformUiSkin()
initTopbarAccountMenu()
initDesktopUpdaterUi()

function logout() {
  localStorage.clear()
  location.replace('index.html')
}
