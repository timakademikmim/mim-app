// =======================
// SUPABASE INIT
// =======================
const supabaseUrl = 'https://optucpelkueqmlhwlbej.supabase.co'
const supabaseKey = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9wdHVjcGVsa3VlcW1saHdsYmVqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzAxOTY4MTgsImV4cCI6MjA4NTc3MjgxOH0.Vqaey9pcnltu9uRbPk0J-AGWaGDZjQLw92pcRv67GNE'
const sb = supabase.createClient(supabaseUrl, supabaseKey)
const externalPageHtmlCache = {}
const externalPageScriptLoaded = {}
const EXTERNAL_PAGE_ASSET_VERSION = '20260304-desktop-wa-print-fix-01'
const CHAT_MEMBERS_TABLE = 'chat_thread_members'
const CHAT_MESSAGES_TABLE = 'chat_messages'
const ADMIN_HISTORY_STATE_PAGE_KEY = 'admin_page'
const ADMIN_HISTORY_STATE_PARAMS_KEY = 'admin_params'
const ADMIN_SIDEBAR_COLLAPSED_KEY = 'admin_sidebar_collapsed'
const ADMIN_SIDEBAR_ICON_ONLY_BREAKPOINT = 1180
const pageDataCache = window.__pageDataCache || {}
window.__pageDataCache = pageDataCache
let adminTopbarChatBadgeState = {
  userId: '',
  intervalId: null,
  refreshInFlight: false
}

function getAdminLayoutElement() {
  return document.querySelector('.layout')
}

function buildAdminSidebarIconSvg(pageKey = '') {
  const key = String(pageKey || '').trim()
  const svgMap = {
    dashboard: '<path d="M3 9.5L12 3l9 6.5V20a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1V9.5Z" />',
    'struktur-sekolah': '<path d="M4 19h16M6 19V8l6-4 6 4v11M9 12h6M12 12v7" />',
    'kalender-akademik': '<rect x="3.5" y="5" width="17" height="15.5" rx="2.5"/><path d="M3.5 10h17M8 3.5v3M16 3.5v3"/>',
    'tahun-ajaran': '<rect x="4" y="4" width="16" height="16" rx="2.5"/><path d="M8 8h8M8 12h8M8 16h5"/>',
    akademik: '<path d="M4 19.5h16"/><path d="M6.5 16V10M12 16V6M17.5 16v-3.5"/>',
    ketahfizan: '<path d="M5 5.5h14v13H5z"/><path d="M9 5.5v13M12 9h5M12 13h5"/>',
    kesantrian: '<circle cx="12" cy="8" r="3.2"/><path d="M5.5 19a6.5 6.5 0 0 1 13 0"/>',
    karyawan: '<path d="M4 6h16v12H4z"/><path d="M8 10h8M8 14h5"/>',
    'set-tugas': '<rect x="4" y="3.5" width="16" height="18" rx="2.5"/><path d="M8 8h8M8 12h8M8 16h5"/>',
    kelas: '<rect x="4" y="4" width="7" height="7" rx="1.5"/><rect x="13" y="4" width="7" height="7" rx="1.5"/><rect x="4" y="13" width="7" height="7" rx="1.5"/><rect x="13" y="13" width="7" height="7" rx="1.5"/>',
    santri: '<circle cx="12" cy="8" r="3.2"/><path d="M5.5 19a6.5 6.5 0 0 1 13 0"/>',
    'perizinan-santri': '<path d="M8 3h8M7 6h10a2 2 0 0 1 2 2v10a3 3 0 0 1-3 3H8a3 3 0 0 1-3-3V8a2 2 0 0 1 2-2Z"/><path d="M9 12l2 2 4-4"/>',
    'prestasi-pelanggaran': '<path d="M12 3.5l2.3 4.7 5.2.8-3.8 3.7.9 5.3L12 15.8 7.4 18l.9-5.3L4.5 9l5.2-.8L12 3.5Z"/>',
    alumni: '<path d="M6 4h9l3 3v13H6V4Z"/><path d="M9 11h6M9 15h4"/>',
    'kelas-guru-mapel': '<path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H20v16.5A1.5 1.5 0 0 1 18.5 21H6.5A2.5 2.5 0 0 1 4 18.5v-13Z"/><path d="M7.5 7.5h8M7.5 11h8M7.5 14.5h5.5"/>',
    jadwal: '<rect x="3.5" y="5" width="17" height="15.5" rx="2.5"/><path d="M3.5 10h17M8 3.5v3M16 3.5v3"/>',
    ekstrakurikuler: '<circle cx="12" cy="8" r="3.2"/><path d="M5.5 19a6.5 6.5 0 0 1 13 0"/><path d="M4 11.5h3M17 11.5h3"/>',
    ujian: '<path d="M8 5h8l2 2v12a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V7l2-2Z"/><path d="M9 3h6M9 11h6M9 15h4"/>',
    'jadwal-ujian': '<rect x="4" y="5" width="16" height="16" rx="2.5"/><path d="M8 3.5v3M16 3.5v3M4 10h16M8.5 14l2 2 5-5"/>',
    'ketahfizan-halaqah': '<path d="M5 5.5h14v13H5z"/><path d="M9 5.5v13"/>',
    'ketahfizan-hafalan': '<path d="M6 4h12v16H6z"/><path d="M9 8h6M9 12h6M9 16h4"/>',
    'ketahfizan-jadwal': '<rect x="3.5" y="5" width="17" height="15.5" rx="2.5"/><path d="M3.5 10h17M8 3.5v3M16 3.5v3"/>',
    'kesantrian-kamar': '<path d="M4 8h16v11H4z"/><path d="M4 12h16M12 8v11"/>',
    'kehadiran-guru': '<path d="M4 19.5h16"/><path d="M6.5 16V10M12 16V6M17.5 16v-3.5"/>',
    'perizinan-karyawan': '<path d="M8 3h8M7 6h10a2 2 0 0 1 2 2v10a3 3 0 0 1-3 3H8a3 3 0 0 1-3-3V8a2 2 0 0 1 2-2Z"/><path d="M9 12l2 2 4-4"/>'
  }
  const paths = svgMap[key] || '<circle cx="12" cy="12" r="4.2"/><path d="M12 4v2M12 18v2M4 12h2M18 12h2"/>'
  return `<svg class="sidebar-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">${paths}</svg>`
}

function ensureAdminSidebarIcons() {
  const buttons = document.querySelectorAll('.sidebar-nav-btn, .sidebar-submenu-btn, .sidebar-submenu-btn-nested, .sidebar-submenu-parent-btn')
  buttons.forEach(btn => {
    if (btn.querySelector('.sidebar-btn-content')) return
    const pageKey = String(btn.getAttribute('data-page') || '').trim()
    const label = String(btn.textContent || '').trim()
    btn.textContent = ''
    const content = document.createElement('span')
    content.className = 'sidebar-btn-content'
    content.innerHTML = `${buildAdminSidebarIconSvg(pageKey)}<span class="sidebar-btn-label"></span>`
    const labelEl = content.querySelector('.sidebar-btn-label')
    if (labelEl) labelEl.textContent = label
    btn.appendChild(content)
  })
}

function applyAdminSidebarIconsOnlyByViewport() {
  const layout = getAdminLayoutElement()
  if (!layout) return
  layout.classList.toggle('sidebar-icons-only', window.innerWidth <= ADMIN_SIDEBAR_ICON_ONLY_BREAKPOINT)
}

function isAdminSidebarIconsOnlyMode() {
  const layout = getAdminLayoutElement()
  return Boolean(layout && layout.classList.contains('sidebar-icons-only'))
}

function updateAdminSidebarToggleState() {
  const layout = getAdminLayoutElement()
  const btn = document.getElementById('admin-sidebar-toggle')
  if (!layout || !btn) return
  const collapsed = layout.classList.contains('sidebar-collapsed')
  btn.setAttribute('aria-label', collapsed ? 'Tampilkan sidebar' : 'Sembunyikan sidebar')
  btn.title = collapsed ? 'Tampilkan Sidebar' : 'Sembunyikan Sidebar'
}

function setAdminSidebarCollapsed(collapsed) {
  const layout = getAdminLayoutElement()
  if (!layout) return
  layout.classList.toggle('sidebar-collapsed', Boolean(collapsed))
  try {
    localStorage.setItem(ADMIN_SIDEBAR_COLLAPSED_KEY, collapsed ? '1' : '0')
  } catch (_) {}
  updateAdminSidebarToggleState()
}

function toggleAdminSidebar() {
  const layout = getAdminLayoutElement()
  if (!layout) return
  setAdminSidebarCollapsed(!layout.classList.contains('sidebar-collapsed'))
}

function getAdminNavButtonLabel(btn) {
  return String(btn?.querySelector('.sidebar-btn-label')?.textContent || btn?.textContent || '').trim()
}

function getAdminNavSubtabButtons(btn) {
  const submenu = btn?.nextElementSibling
  if (!submenu || !(submenu instanceof HTMLElement)) return []
  if (!(submenu.classList.contains('sidebar-submenu') || submenu.classList.contains('sidebar-submenu-nested'))) return []
  return Array.from(submenu.querySelectorAll('.sidebar-submenu-btn, .sidebar-submenu-btn-nested'))
}

function removeAdminSidebarTooltip() {
  document.getElementById('admin-sidebar-tooltip')?.remove()
}

function showAdminSidebarTooltip(btn, options = {}) {
  if (!isAdminSidebarIconsOnlyMode()) return
  removeAdminSidebarTooltip()
  const title = getAdminNavButtonLabel(btn)
  if (!title) return
  const { persistent = false, interactive = false } = options
  const subTabButtons = getAdminNavSubtabButtons(btn)
  const tip = document.createElement('div')
  tip.id = 'admin-sidebar-tooltip'
  tip.className = 'sidebar-nav-tooltip'
  tip.innerHTML = `<div class="sidebar-nav-tooltip-title">${escapeHtml(title)}</div>`
  if (subTabButtons.length) {
    const sublist = document.createElement('div')
    sublist.className = 'sidebar-nav-tooltip-sublist'
    subTabButtons.forEach(subBtn => {
      const label = getAdminNavButtonLabel(subBtn)
      if (!label) return
      if (interactive) {
        const actionBtn = document.createElement('button')
        actionBtn.type = 'button'
        actionBtn.className = 'sidebar-nav-tooltip-subbtn'
        actionBtn.textContent = label
        actionBtn.addEventListener('click', () => {
          subBtn.click()
          removeAdminSidebarTooltip()
        })
        sublist.appendChild(actionBtn)
      } else {
        const row = document.createElement('div')
        row.className = 'sidebar-nav-tooltip-subitem'
        row.textContent = label
        sublist.appendChild(row)
      }
    })
    tip.appendChild(sublist)
  }
  document.body.appendChild(tip)
  const rect = btn.getBoundingClientRect()
  const tipRect = tip.getBoundingClientRect()
  const margin = 8
  let left = rect.right + margin
  let top = rect.top + (rect.height / 2) - (tipRect.height / 2)
  if (left + tipRect.width > window.innerWidth - 8) left = Math.max(8, rect.left - tipRect.width - margin)
  top = Math.max(8, Math.min(top, window.innerHeight - tipRect.height - 8))
  tip.style.left = `${left}px`
  tip.style.top = `${top}px`
  if (!persistent) {
    window.setTimeout(() => {
      if (tip.isConnected) tip.remove()
    }, 1600)
  }
}

function setupAdminSidebarTooltips() {
  const buttons = document.querySelectorAll('.sidebar-nav-btn, .sidebar-submenu-btn, .sidebar-submenu-btn-nested, .sidebar-submenu-parent-btn')
  buttons.forEach(btn => {
    if (btn.dataset.iconTooltipBound === '1') return
    btn.dataset.iconTooltipBound = '1'
    btn.addEventListener('mouseenter', () => showAdminSidebarTooltip(btn))
    btn.addEventListener('focus', () => showAdminSidebarTooltip(btn))
    btn.addEventListener('touchstart', () => showAdminSidebarTooltip(btn, { persistent: true, interactive: true }), { passive: true })
  })
  document.addEventListener('click', event => {
    const target = event.target
    if (!(target instanceof HTMLElement)) return
    if (!target.closest('.sidebar-nav-btn, .sidebar-submenu-btn, .sidebar-submenu-btn-nested, .sidebar-submenu-parent-btn, #admin-sidebar-tooltip')) {
      removeAdminSidebarTooltip()
    }
  })
}

function initAdminSidebarState() {
  ensureAdminSidebarIcons()
  applyAdminSidebarIconsOnlyByViewport()
  try {
    setAdminSidebarCollapsed(localStorage.getItem(ADMIN_SIDEBAR_COLLAPSED_KEY) === '1')
  } catch (_) {
    setAdminSidebarCollapsed(false)
  }
  setupAdminSidebarTooltips()
  window.addEventListener('resize', applyAdminSidebarIconsOnlyByViewport)
}

function normalizeAdminHistoryParams(params = {}) {
  const plain = {}
  Object.keys(params || {}).forEach(key => {
    const value = params[key]
    if (value === undefined) return
    if (typeof value === 'function') return
    plain[key] = value
  })
  return plain
}

function pushAdminTabHistory(page, params = {}) {
  if (!window?.history?.pushState) return
  const targetPage = String(page || '').trim()
  if (!targetPage) return
  const targetParams = normalizeAdminHistoryParams(params)
  const currentPage = String(window.history.state?.[ADMIN_HISTORY_STATE_PAGE_KEY] || '').trim()
  const currentParams = window.history.state?.[ADMIN_HISTORY_STATE_PARAMS_KEY] || {}
  if (currentPage === targetPage && JSON.stringify(currentParams) === JSON.stringify(targetParams)) return
  window.history.pushState({
    [ADMIN_HISTORY_STATE_PAGE_KEY]: targetPage,
    [ADMIN_HISTORY_STATE_PARAMS_KEY]: targetParams
  }, '', window.location.href)
}

function replaceAdminTabHistory(page, params = {}) {
  if (!window?.history?.replaceState) return
  const targetPage = String(page || '').trim()
  if (!targetPage) return
  const targetParams = normalizeAdminHistoryParams(params)
  window.history.replaceState({
    [ADMIN_HISTORY_STATE_PAGE_KEY]: targetPage,
    [ADMIN_HISTORY_STATE_PARAMS_KEY]: targetParams
  }, '', window.location.href)
}

window.getCachedData = function getCachedData(key, ttlMs) {
  const entry = pageDataCache[key]
  if (!entry) return null
  if (Date.now() - entry.ts > ttlMs) return null
  return entry.data
}

window.setCachedData = function setCachedData(key, data) {
  pageDataCache[key] = { ts: Date.now(), data }
}

window.clearCachedData = function clearCachedData(key) {
  delete pageDataCache[key]
}

function setupCustomPopupSystem() {
  if (typeof window.setupSharedPopupSystem === 'function' && window.setupSharedPopupSystem()) return
  if (window.__popupReady) return
  window.showPopupMessage = async function showPopupMessage(message) {
    globalThis.alert(String(message ?? ''))
    return true
  }
  window.showPopupConfirm = async function showPopupConfirm(message) {
    return globalThis.confirm(String(message ?? ''))
  }
  window.__popupReady = true
}


// =======================
// LOGIN CHECK
// =======================
const loginId = localStorage.getItem('login_id')
let loginDisplayName = String(localStorage.getItem('login_name') || '').trim()
if (!loginDisplayName) loginDisplayName = String(loginId || '').trim()

if (!loginId) {
  location.href = 'index.html'
}

document.addEventListener('DOMContentLoaded', () => {
  setupCustomPopupSystem()
  initAdminSidebarState()
  renderAdminTopbarName()
  hydrateAdminLoginDisplayName()
  ensureAdminTopbarChatButton()
  refreshAdminTopbarChatBadge().catch(error => console.error(error))
  startAdminTopbarChatBadgeTicker()

  const forceDashboardOnce = localStorage.getItem('admin_force_dashboard_once') === '1'

  if (forceDashboardOnce) {
    localStorage.removeItem('admin_force_dashboard_once')
    localStorage.removeItem('admin_last_page')
    localStorage.removeItem('admin_last_page_params')
    loadPage('dashboard', {}, { updateHistory: true, replaceHistory: true })
    return
  }

  const lastPage = localStorage.getItem('admin_last_page') || 'dashboard'
  let lastParams = {}
  try {
    lastParams = JSON.parse(localStorage.getItem('admin_last_page_params') || '{}')
  } catch (e) {
    lastParams = {}
  }
  loadPage(lastPage, lastParams, { updateHistory: true, replaceHistory: true })

  window.addEventListener('popstate', event => {
    const pageFromState = String(event.state?.[ADMIN_HISTORY_STATE_PAGE_KEY] || '').trim()
    if (!pageFromState) return
    const paramsFromState = event.state?.[ADMIN_HISTORY_STATE_PARAMS_KEY] || {}
    loadPage(pageFromState, paramsFromState, { updateHistory: false })
  })
})


// =======================
// LOGOUT
// =======================
function logout() {
  localStorage.removeItem('login_id')
  localStorage.removeItem('login_name')
  localStorage.removeItem('admin_force_dashboard_once')
  localStorage.removeItem('admin_last_page')
  localStorage.removeItem('admin_last_page_params')
  location.href = 'index.html'
}

function renderAdminTopbarName() {
  const welcomeEl = document.getElementById('welcome')
  if (!welcomeEl) return
  welcomeEl.textContent = loginDisplayName || String(loginId || '').trim()
  if (typeof window.setTopbarUserIdentity === 'function') {
    window.setTopbarUserIdentity({ name: welcomeEl.textContent, foto_url: localStorage.getItem('login_photo_url') || '' })
  }
}

async function hydrateAdminLoginDisplayName() {
  if (!loginId) return
  let data = null
  let error = null
  const variants = ['nama, foto_url', 'nama']
  for (const selectCols of variants) {
    const result = await sb
      .from('karyawan')
      .select(selectCols)
      .eq('id_karyawan', loginId)
      .maybeSingle()
    if (!result.error) {
      data = result.data || null
      error = null
      break
    }
    error = result.error
    const msg = String(result.error?.message || '').toLowerCase()
    if (!(msg.includes('column') && msg.includes('foto_url'))) break
  }

  if (error) {
    console.error(error)
    return
  }

  const name = String(data?.nama || '').trim()
  if (!name) return

  loginDisplayName = name
  localStorage.setItem('login_name', name)
  const fotoUrl = String(data?.foto_url || '').trim()
  if (fotoUrl) localStorage.setItem('login_photo_url', fotoUrl)
  else localStorage.removeItem('login_photo_url')
  renderAdminTopbarName()
}

function toggleTopbarUserMenu() {
  const menu = document.getElementById('topbar-user-menu')
  if (!menu) return
  menu.classList.toggle('open')
}

function closeTopbarUserMenu() {
  const menu = document.getElementById('topbar-user-menu')
  if (!menu) return
  menu.classList.remove('open')
}

function setupTopbarUserMenuHandlers() {
  if (document.body.dataset.topbarMenuBound === 'true') return

  document.addEventListener('click', event => {
    const topWrap = document.querySelector('.topbar-user-menu-wrap')
    const sideWrap = document.querySelector('.sidebar-user-menu-wrap')
    if ((topWrap && topWrap.contains(event.target)) || (sideWrap && sideWrap.contains(event.target))) return
    closeTopbarUserMenu()
  })

  document.body.dataset.topbarMenuBound = 'true'
}

function setActiveSidebarTab(page) {
  const isAkademikGroup =
    page === 'kelas' ||
    page === 'kelas-distribusi-mapel' ||
    page === 'kelas-guru-mapel' ||
    page === 'jadwal' ||
    page === 'ekstrakurikuler' ||
    page === 'ujian' ||
    page === 'jadwal-ujian' ||
    page === 'santri' ||
    page === 'prestasi-pelanggaran' ||
    page === 'perizinan-santri' ||
    page === 'alumni' ||
    page === 'santri-detail'

  const isKetahfizanGroup =
    page === 'ketahfizan-halaqah' ||
    page === 'ketahfizan-hafalan' ||
    page === 'ketahfizan-jadwal'

  const isKesantrianGroup =
    page === 'kesantrian-kamar'

  const isKaryawanGroup =
    page === 'karyawan' ||
    page === 'guru' ||
    page === 'guru-detail' ||
    page === 'kehadiran-guru' ||
    page === 'perizinan-karyawan'

  const navButtons = document.querySelectorAll('.sidebar-nav-btn')
  navButtons.forEach(button => {
    const btnPage = button.getAttribute('data-page')
    if (btnPage === 'akademik') {
      button.classList.toggle('active', isAkademikGroup)
      return
    }
    if (btnPage === 'karyawan') {
      button.classList.toggle('active', isKaryawanGroup)
      return
    }
    if (btnPage === 'ketahfizan') {
      button.classList.toggle('active', isKetahfizanGroup)
      return
    }
    if (btnPage === 'kesantrian') {
      button.classList.toggle('active', isKesantrianGroup)
      return
    }
    if (btnPage === 'mutabaah-karyawan') {
      button.classList.toggle('active', page === 'set-tugas' || page === 'tugas-harian' || page === 'mutabaah-karyawan')
      return
    }
    button.classList.toggle('active', btnPage === page)
  })

  const submenuButtons = document.querySelectorAll('.sidebar-submenu-btn')
  submenuButtons.forEach(button => {
    const btnPage = button.getAttribute('data-page')
    const isSantriDetail = page === 'santri-detail' && btnPage === 'santri'
    const isGuruDetail = page === 'guru-detail' && btnPage === 'karyawan'
    const isLegacyDistribusi = page === 'kelas-distribusi-mapel' && btnPage === 'kelas-guru-mapel'
    button.classList.toggle('active', btnPage === page || isSantriDetail || isGuruDetail || isLegacyDistribusi)
  })

  setAkademikSidebarMenuExpanded(isAkademikGroup)
  setKetahfizanSidebarMenuExpanded(isKetahfizanGroup)
  setKesantrianSidebarMenuExpanded(isKesantrianGroup)
  setKaryawanSidebarMenuExpanded(isKaryawanGroup)
}

function ensureAdminTopbarChatButton() {
  const wrap = document.querySelector('.topbar-user-menu-wrap')
  if (!wrap) return
  if (document.getElementById('topbar-chat-trigger')) return

  const chatBtn = document.createElement('button')
  chatBtn.type = 'button'
  chatBtn.id = 'topbar-chat-trigger'
  chatBtn.className = 'topbar-notif-trigger'
  chatBtn.title = 'Chat'
  chatBtn.innerHTML = '<span aria-hidden="true">&#128172;</span><span id="topbar-chat-badge" class="topbar-notif-badge hidden">0</span>'
  chatBtn.addEventListener('click', event => {
    event.preventDefault()
    event.stopPropagation()
    loadPage('chat')
  })
  const userTrigger = wrap.querySelector('.topbar-user-trigger')
  wrap.insertBefore(chatBtn, userTrigger || null)
}

function setAdminTopbarChatBadge(count) {
  if (typeof window.setTopbarBadgeCount === 'function') {
    window.setTopbarBadgeCount('topbar-chat-badge', count)
    return
  }
  const badge = document.getElementById('topbar-chat-badge')
  if (!badge) return
  const total = Number.isFinite(Number(count)) ? Number(count) : 0
  if (total <= 0) {
    badge.textContent = '0'
    badge.classList.add('hidden')
    return
  }
  badge.textContent = total > 99 ? '99+' : String(total)
  badge.classList.remove('hidden')
}

async function getAdminChatUserId() {
  if (adminTopbarChatBadgeState.userId) return adminTopbarChatBadgeState.userId
  if (!loginId) return ''
  const { data, error } = await sb
    .from('karyawan')
    .select('id')
    .eq('id_karyawan', String(loginId || '').trim())
    .maybeSingle()
  if (error) {
    console.error(error)
    return ''
  }
  const userId = String(data?.id || '').trim()
  adminTopbarChatBadgeState.userId = userId
  return userId
}

function toTimestampMs(value) {
  const ms = Date.parse(String(value || '').trim())
  return Number.isFinite(ms) ? ms : 0
}

async function fetchAdminUnreadChatThreadCount() {
  const userId = await getAdminChatUserId()
  if (!userId) return 0

  let hasLastReadColumn = true
  let membersRes = await sb
    .from(CHAT_MEMBERS_TABLE)
    .select('thread_id, last_read_at')
    .eq('karyawan_id', userId)

  if (membersRes.error && String(membersRes.error?.message || '').toLowerCase().includes('last_read_at')) {
    hasLastReadColumn = false
    membersRes = await sb
      .from(CHAT_MEMBERS_TABLE)
      .select('thread_id')
      .eq('karyawan_id', userId)
  }

  if (membersRes.error) throw membersRes.error
  const members = Array.isArray(membersRes.data) ? membersRes.data : []
  const threadIds = [...new Set(members.map(item => String(item?.thread_id || '').trim()).filter(Boolean))]
  if (!threadIds.length) return 0

  const { data: msgRows, error: msgErr } = await sb
    .from(CHAT_MESSAGES_TABLE)
    .select('thread_id, sender_id, created_at')
    .in('thread_id', threadIds)
    .order('created_at', { ascending: false })
    .limit(1000)
  if (msgErr) throw msgErr

  const latestIncomingMsByThread = new Map()
  ;(msgRows || []).forEach(row => {
    const threadId = String(row?.thread_id || '').trim()
    if (!threadId) return
    if (String(row?.sender_id || '').trim() === userId) return
    const currentMs = latestIncomingMsByThread.get(threadId) || 0
    const nextMs = toTimestampMs(row?.created_at)
    if (nextMs > currentMs) latestIncomingMsByThread.set(threadId, nextMs)
  })

  let unread = 0
  members.forEach(member => {
    const threadId = String(member?.thread_id || '').trim()
    if (!threadId) return
    const incomingMs = latestIncomingMsByThread.get(threadId) || 0
    if (!incomingMs) return
    if (!hasLastReadColumn) {
      unread += 1
      return
    }
    const readMs = toTimestampMs(member?.last_read_at)
    if (!readMs || incomingMs > readMs) unread += 1
  })
  return unread
}

async function refreshAdminTopbarChatBadge() {
  ensureAdminTopbarChatButton()
  if (adminTopbarChatBadgeState.refreshInFlight) return
  adminTopbarChatBadgeState.refreshInFlight = true
  try {
    const unreadCount = await fetchAdminUnreadChatThreadCount()
    setAdminTopbarChatBadge(unreadCount)
  } catch (error) {
    console.error(error)
  } finally {
    adminTopbarChatBadgeState.refreshInFlight = false
  }
}

function startAdminTopbarChatBadgeTicker() {
  if (adminTopbarChatBadgeState.intervalId) return
  adminTopbarChatBadgeState.intervalId = window.setInterval(() => {
    refreshAdminTopbarChatBadge().catch(error => console.error(error))
  }, 10000)
}

function setTopbarTitle(page) {
  const titleMap = {
    dashboard: 'Dashboard',
    'struktur-sekolah': 'Struktur Sekolah',
    'tahun-ajaran': 'Tahun Ajaran',
    'tugas-harian': 'Set Tugas',
    'mutabaah-karyawan': 'Mutabaah Karyawan',
    'set-tugas': 'Mutabaah Guru',
    kelas: 'Data Kelas',
    'kelas-distribusi-mapel': 'Distribusi Mapel',
    'kelas-guru-mapel': 'Data Mapel',
    jadwal: 'Jadwal Pelajaran',
    ekstrakurikuler: 'Ekstrakulikuler',
    ujian: 'Ujian',
    'jadwal-ujian': 'Jadwal Ujian',
    'ketahfizan-halaqah': 'Data Halaqah',
    'ketahfizan-hafalan': 'Data Hafalan',
    'ketahfizan-jadwal': 'Jadwal Halaqah',
    'kesantrian-kamar': 'Data Kamar',
    'kalender-akademik': 'Kalender Akademik',
    'kehadiran-guru': 'Kehadiran Karyawan',
    'perizinan-karyawan': 'Perizinan Karyawan',
    'perizinan-santri': 'Perizinan Siswa',
    'prestasi-pelanggaran': 'Prestasi & Pelanggaran',
    santri: 'Data Siswa',
    alumni: 'Data Alumni',
    'santri-detail': 'Detail Santri',
    guru: 'Data Guru',
    'guru-detail': 'Detail Guru',
    karyawan: 'Data Karyawan',
    chat: 'Chat'
  }
  const titleEl = document.getElementById('topbar-title')
  if (!titleEl) return
  titleEl.textContent = titleMap[page] || 'Admin Panel'
  titleEl.dataset.baseTitle = titleEl.textContent
  appendTopbarMeta(page)
}

let topbarActiveYearCache = {
  value: '',
  ts: 0
}
const TOPBAR_KALENDER_TABLE = 'kalender_akademik'
const TOPBAR_KALENDER_CACHE_KEY = 'kalender_akademik:list'
const TOPBAR_KALENDER_CACHE_TTL_MS = 2 * 60 * 1000
const TOPBAR_KALENDER_DEFAULT_COLOR = '#2563eb'
let topbarKalenderState = {
  list: [],
  month: '',
  selectedDateKey: '',
  visible: false
}
let adminDashboardAgendaRows = []

function shouldShowYearInTopbar(page) {
  return page !== 'tahun-ajaran'
}

window.invalidateTopbarActiveYearCache = function invalidateTopbarActiveYearCache() {
  topbarActiveYearCache = { value: '', ts: 0 }
}

async function getActiveYearLabel() {
  const now = Date.now()
  if (topbarActiveYearCache.value && now - topbarActiveYearCache.ts < 60 * 1000) {
    return topbarActiveYearCache.value
  }

  const { data, error } = await sb
    .from('tahun_ajaran')
    .select('nama')
    .eq('aktif', true)
    .limit(1)

  if (error) {
    console.error(error)
    return ''
  }

  const value = String(data?.[0]?.nama || '').trim()
  topbarActiveYearCache = {
    value,
    ts: now
  }
  return value
}

async function appendActiveYearToTopbarTitle(page) {
  if (!shouldShowYearInTopbar(page)) return ''
  const yearLabel = await getActiveYearLabel()
  return yearLabel || ''
}

function getTopbarTodayLabel() {
  return new Date().toLocaleDateString('id-ID', {
    weekday: 'long',
    day: '2-digit',
    month: 'long',
    year: 'numeric'
  })
}

async function appendTopbarMeta(page) {
  const titleEl = document.getElementById('topbar-title')
  if (!titleEl) return

  const currentTitle = titleEl.dataset.baseTitle || titleEl.textContent || 'Admin Panel'
  const yearLabel = await appendActiveYearToTopbarTitle(page)
  const todayLabel = getTopbarTodayLabel()

  if ((titleEl.dataset.baseTitle || titleEl.textContent || '') !== currentTitle) return
  const parts = [`<span class="topbar-title-main">${escapeHtml(currentTitle)}</span>`]
  if (yearLabel) {
    parts.push(`<span class="topbar-title-sep" aria-hidden="true"></span><span class="topbar-title-year">${escapeHtml(yearLabel)}</span>`)
  }
  parts.push(`<span class="topbar-title-sep" aria-hidden="true"></span><button type="button" class="topbar-title-date-btn" onclick="openTopbarCalendarPopup()" title="Lihat kalender kegiatan">${escapeHtml(todayLabel)}</button>`)
  titleEl.innerHTML = parts.join('')
}

function escapeHtml(value) {
  if (typeof window.appEscapeHtml === 'function') return window.appEscapeHtml(value)
  return String(value || '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

function getTopbarKalenderMonthNow() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

function normalizeTopbarKalenderColor(value) {
  const raw = String(value || '').trim()
  return /^#[0-9a-fA-F]{6}$/.test(raw) ? raw : TOPBAR_KALENDER_DEFAULT_COLOR
}

function getTopbarKalenderDateKey(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function getTopbarKalenderRangeKeys(mulaiValue, selesaiValue) {
  const startKey = getTopbarKalenderDateKey(mulaiValue)
  const endKey = getTopbarKalenderDateKey(selesaiValue || mulaiValue)
  if (!startKey) return []
  const start = new Date(`${startKey}T00:00:00`)
  const end = new Date(`${endKey}T00:00:00`)
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime()) || end.getTime() < start.getTime()) return [startKey]
  const keys = []
  const cursor = new Date(start)
  while (cursor.getTime() <= end.getTime()) {
    keys.push(`${cursor.getFullYear()}-${String(cursor.getMonth() + 1).padStart(2, '0')}-${String(cursor.getDate()).padStart(2, '0')}`)
    cursor.setDate(cursor.getDate() + 1)
  }
  return keys
}

function getTopbarKalenderEventsByDate() {
  const map = new Map()
  ;(topbarKalenderState.list || []).forEach(item => {
    const keys = getTopbarKalenderRangeKeys(item.mulai, item.selesai)
    keys.forEach(key => {
      if (!map.has(key)) map.set(key, [])
      map.get(key).push(item)
    })
  })
  return map
}

function getTopbarKalenderMonthMeta(monthKey) {
  const [y, m] = String(monthKey || '').split('-').map(Number)
  const year = Number.isFinite(y) ? y : new Date().getFullYear()
  const month = Number.isFinite(m) ? m : (new Date().getMonth() + 1)
  const first = new Date(year, month - 1, 1)
  const last = new Date(year, month, 0)
  return { year, month, firstWeekday: first.getDay(), daysInMonth: last.getDate() }
}

function getTopbarKalenderDayBg(events) {
  const colors = [...new Set((events || []).map(item => normalizeTopbarKalenderColor(item?.warna)).filter(Boolean))]
  if (!colors.length) return 'transparent'
  if (colors.length === 1) return `${colors[0]}22`
  const step = 100 / colors.length
  const parts = colors.map((color, index) => {
    const start = (index * step).toFixed(2)
    const end = ((index + 1) * step).toFixed(2)
    return `${color}33 ${start}% ${end}%`
  })
  return `linear-gradient(90deg, ${parts.join(', ')})`
}

function ensureTopbarKalenderPopup() {
  let popup = document.getElementById('topbar-calendar-popup')
  if (popup) return popup

  popup = document.createElement('div')
  popup.id = 'topbar-calendar-popup'
  popup.className = 'topbar-calendar-overlay'
  popup.style.display = 'none'
  popup.innerHTML = `
    <div class="topbar-calendar-card" onclick="event.stopPropagation()">
      <div class="topbar-calendar-head">
        <strong>Kalender Kegiatan</strong>
        <div class="topbar-calendar-actions">
          <button type="button" class="topbar-calendar-btn topbar-calendar-btn-secondary" onclick="shiftTopbarCalendarMonth(-1)">Bulan Sebelumnya</button>
          <input id="topbar-calendar-month" class="topbar-calendar-month" type="month">
          <button type="button" class="topbar-calendar-btn topbar-calendar-btn-secondary" onclick="shiftTopbarCalendarMonth(1)">Bulan Berikutnya</button>
          <button type="button" class="topbar-calendar-btn topbar-calendar-btn-secondary topbar-calendar-btn-close" onclick="closeTopbarCalendarPopup()">×</button>
        </div>
      </div>
      <div id="topbar-calendar-title" class="topbar-calendar-title">Loading...</div>
      <div id="topbar-calendar-grid" class="topbar-calendar-grid">Loading...</div>
      <div id="topbar-calendar-detail" class="topbar-calendar-detail">Pilih tanggal untuk melihat detail kegiatan.</div>
    </div>
  `
  popup.addEventListener('click', event => {
    if (event.target !== popup) return
    closeTopbarCalendarPopup()
  })
  document.body.appendChild(popup)

  const monthEl = popup.querySelector('#topbar-calendar-month')
  if (monthEl) {
    monthEl.addEventListener('change', () => {
      topbarKalenderState.month = monthEl.value || getTopbarKalenderMonthNow()
      topbarKalenderState.selectedDateKey = ''
      renderTopbarCalendar()
    })
  }
  return popup
}

async function fetchTopbarKalenderRows() {
  const { data, error } = await sb
    .from(TOPBAR_KALENDER_TABLE)
    .select('id, judul, mulai, selesai, detail, warna')
    .order('mulai', { ascending: true })

  if (error) throw error
  return (data || []).map(item => ({ ...item, warna: normalizeTopbarKalenderColor(item.warna) }))
}

async function loadTopbarCalendarData(forceRefresh = false) {
  if (!forceRefresh && typeof getCachedData === 'function') {
    const cached = getCachedData(TOPBAR_KALENDER_CACHE_KEY, TOPBAR_KALENDER_CACHE_TTL_MS)
    if (cached) {
      topbarKalenderState.list = cached
      return
    }
  }

  const list = await fetchTopbarKalenderRows()
  topbarKalenderState.list = list
  if (typeof setCachedData === 'function') setCachedData(TOPBAR_KALENDER_CACHE_KEY, list)
}

function formatDashboardCalendarDate(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleDateString('id-ID', { day: '2-digit', month: 'short', year: 'numeric' })
}

function getDashboardCalendarRangeLabel(item) {
  const startKey = getTopbarKalenderDateKey(item?.mulai)
  const endKey = getTopbarKalenderDateKey(item?.selesai || item?.mulai)
  if (!startKey) return '-'
  if (!endKey || endKey === startKey) return formatDashboardCalendarDate(startKey)
  return `${formatDashboardCalendarDate(startKey)} - ${formatDashboardCalendarDate(endKey)}`
}

async function renderAdminDashboardCalendar() {
  const area = document.getElementById('content-area')
  if (!area) return

  area.innerHTML = '<div class="placeholder-card">Loading agenda kalender akademik...</div>'

  try {
    await loadTopbarCalendarData(false)
    const rows = (topbarKalenderState.list || []).slice()
      .sort((a, b) => String(a?.mulai || '').localeCompare(String(b?.mulai || '')))
    adminDashboardAgendaRows = rows

    if (!rows.length) {
      area.innerHTML = `
        <div class="placeholder-card">
          <div style="font-size:16px; font-weight:700; margin-bottom:8px;">Dashboard Agenda</div>
          <div style="font-size:13px; color:#64748b;">Belum ada kegiatan di Kalender Akademik.</div>
        </div>
      `
      return
    }

    const todayKey = getTopbarKalenderDateKey(new Date())
    const thisMonthKey = getTopbarKalenderMonthNow()
    const totalKegiatan = rows.length
    const bulanIniCount = rows.filter(item => String(getTopbarKalenderDateKey(item?.mulai) || '').startsWith(thisMonthKey)).length
    const hariIniCount = rows.filter(item => getTopbarKalenderRangeKeys(item?.mulai, item?.selesai).includes(todayKey)).length

    const itemsHtml = rows.map(item => {
      const warna = normalizeTopbarKalenderColor(item?.warna)
      const rentang = getDashboardCalendarRangeLabel(item)
      const itemId = String(item?.id || '')
      return `
        <button type="button" onclick="openAdminDashboardAgendaPopup('${escapeHtml(itemId)}')" style="text-align:left; width:100%; min-height:210px; position:relative; border:1px solid #e2e8f0; border-radius:16px; background:linear-gradient(180deg,#ffffff 0%,#f8fafc 100%); box-shadow:0 12px 24px rgba(15,23,42,0.08); padding:22px 20px 18px 22px; overflow:hidden; cursor:pointer;">
          <span style="pointer-events:none; position:absolute; inset:0; background:linear-gradient(92deg, ${escapeHtml(warna)}0b 0%, ${escapeHtml(warna)}08 20%, rgba(255,255,255,0) 54%), linear-gradient(165deg, rgba(255,255,255,0.42) 0%, rgba(255,255,255,0) 38%); box-shadow:inset 1px 0 8px ${escapeHtml(warna)}1a;"></span>
          <div style="display:flex; flex-direction:column; align-items:center; justify-content:center; gap:10px; min-height:160px; text-align:center;">
            <div style="font-family:'Poppins',sans-serif; font-size:54px; font-weight:700; color:#0f172a; line-height:1.2;">${escapeHtml(item?.judul || '-')}</div>
            <span style="font-family:'Poppins',sans-serif; font-size:24px; font-weight:700; color:#334155; background:#ffffff; border:none; border-radius:999px; padding:6px 12px; white-space:nowrap;">${escapeHtml(rentang)}</span>
          </div>
        </button>
      `
    }).join('')

    area.innerHTML = `
      <div class="placeholder-card">
        <div style="font-size:16px; font-weight:700; margin-bottom:8px; color:#0f172a;">Dashboard Agenda</div>
        <div style="font-size:13px; color:#475569; margin-bottom:12px;">Menampilkan seluruh kegiatan dari Kalender Akademik.</div>
        <div style="display:grid; grid-template-columns:repeat(auto-fit,minmax(180px,1fr)); gap:10px; margin-bottom:12px;">
          <div style="border:1px solid #e2e8f0; border-radius:12px; background:#ffffff; padding:10px 12px;">
            <div style="font-size:11px; color:#64748b;">Total Kegiatan</div>
            <div style="font-size:22px; font-weight:700; color:#0f172a; line-height:1.2;">${totalKegiatan}</div>
          </div>
          <div style="border:1px solid #e2e8f0; border-radius:12px; background:#ffffff; padding:10px 12px;">
            <div style="font-size:11px; color:#64748b;">Bulan Ini</div>
            <div style="font-size:22px; font-weight:700; color:#0f172a; line-height:1.2;">${bulanIniCount}</div>
          </div>
          <div style="border:1px solid #e2e8f0; border-radius:12px; background:#ffffff; padding:10px 12px;">
            <div style="font-size:11px; color:#64748b;">Sedang Berjalan Hari Ini</div>
            <div style="font-size:22px; font-weight:700; color:#0f172a; line-height:1.2;">${hariIniCount}</div>
          </div>
        </div>
        <div style="display:grid; grid-template-columns:repeat(auto-fit,minmax(620px,1fr)); gap:14px;">
          ${itemsHtml}
        </div>
      </div>
    `
  } catch (error) {
    console.error(error)
    area.innerHTML = `<div class="placeholder-card">Gagal load agenda dashboard: ${escapeHtml(error?.message || 'Unknown error')}</div>`
  }
}

function ensureAdminDashboardAgendaPopup() {
  let popup = document.getElementById('admin-dashboard-agenda-popup')
  if (popup) return popup
  popup = document.createElement('div')
  popup.id = 'admin-dashboard-agenda-popup'
  popup.style.cssText = 'position:fixed; inset:0; background:rgba(15,23,42,0.35); display:none; align-items:center; justify-content:center; z-index:10001; padding:16px; box-sizing:border-box;'
  popup.innerHTML = `
    <div style="width:min(680px, calc(100vw - 32px)); max-height:calc(100vh - 32px); overflow:auto; border:1px solid #dbeafe; border-radius:0; background:#fff; box-shadow:0 18px 34px rgba(15,23,42,0.18); padding:14px 16px; position:relative;">
      <button type="button" onclick="closeAdminDashboardAgendaPopup()" style="position:absolute; right:12px; top:10px; border:1px solid #cbd5e1; background:#fff; border-radius:999px; width:28px; height:28px; cursor:pointer;">×</button>
      <div id="admin-dashboard-agenda-popup-body"></div>
    </div>
  `
  popup.addEventListener('click', event => {
    if (event.target !== popup) return
    closeAdminDashboardAgendaPopup()
  })
  document.body.appendChild(popup)
  return popup
}

function openAdminDashboardAgendaPopup(id) {
  const sid = String(id || '')
  const selected = adminDashboardAgendaRows.find(item => String(item?.id || '') === sid)
  if (!selected) return
  const popup = ensureAdminDashboardAgendaPopup()
  const body = document.getElementById('admin-dashboard-agenda-popup-body')
  if (!popup || !body) return
  const warna = normalizeTopbarKalenderColor(selected?.warna)
  body.innerHTML = `
    <div style="display:flex; align-items:flex-start; justify-content:space-between; gap:10px; margin-bottom:8px; padding-right:30px;">
      <div style="font-size:20px; font-weight:700; color:#0f172a; line-height:1.35;">${escapeHtml(selected?.judul || '-')}</div>
      <span style="width:12px; height:12px; border-radius:999px; background:${escapeHtml(warna)}; margin-top:8px;"></span>
    </div>
    <div style="font-size:12px; font-weight:600; color:#334155; margin-bottom:8px;">${escapeHtml(getDashboardCalendarRangeLabel(selected))}</div>
    <div style="font-size:14px; color:#475569; line-height:1.7; white-space:pre-wrap;">${escapeHtml(selected?.detail || '-')}</div>
  `
  popup.style.display = 'flex'
}

function closeAdminDashboardAgendaPopup() {
  const popup = document.getElementById('admin-dashboard-agenda-popup')
  if (!popup) return
  popup.style.display = 'none'
}

function renderTopbarCalendarDetail() {
  const detailEl = document.getElementById('topbar-calendar-detail')
  if (!detailEl) return

  if (!topbarKalenderState.selectedDateKey) {
    detailEl.innerHTML = 'Pilih tanggal untuk melihat detail kegiatan.'
    return
  }

  const byDate = getTopbarKalenderEventsByDate()
  const events = byDate.get(topbarKalenderState.selectedDateKey) || []
  if (!events.length) {
    detailEl.innerHTML = `<div style="color:#64748b;">Tidak ada kegiatan pada tanggal ${escapeHtml(topbarKalenderState.selectedDateKey)}.</div>`
    return
  }

  detailEl.innerHTML = events.map(item => {
    const warna = normalizeTopbarKalenderColor(item.warna)
    const mulai = getTopbarKalenderDateKey(item.mulai)
    const selesai = getTopbarKalenderDateKey(item.selesai)
    const rentang = selesai && selesai !== mulai ? `${mulai} - ${selesai}` : mulai
    return `
      <div class="topbar-calendar-event" style="border-left-color:${escapeHtml(warna)};">
        <div class="topbar-calendar-event-title"><span class="topbar-calendar-event-dot" style="background:${escapeHtml(warna)};"></span>${escapeHtml(item.judul || '-')}</div>
        <div class="topbar-calendar-event-meta">${escapeHtml(rentang || '-')}</div>
        <div class="topbar-calendar-event-detail">${escapeHtml(item.detail || '-')}</div>
      </div>
    `
  }).join('')
}

function renderTopbarCalendar() {
  const popup = ensureTopbarKalenderPopup()
  if (!popup || popup.style.display === 'none') return

  const titleEl = document.getElementById('topbar-calendar-title')
  const gridEl = document.getElementById('topbar-calendar-grid')
  const monthEl = document.getElementById('topbar-calendar-month')
  if (!titleEl || !gridEl || !monthEl) return

  const monthKey = topbarKalenderState.month || getTopbarKalenderMonthNow()
  topbarKalenderState.month = monthKey
  monthEl.value = monthKey

  const meta = getTopbarKalenderMonthMeta(monthKey)
  const todayKey = getTopbarKalenderDateKey(new Date())
  const monthName = new Date(meta.year, meta.month - 1, 1).toLocaleDateString('id-ID', { month: 'long', year: 'numeric' })
  titleEl.textContent = `Kalender ${monthName}`

  const byDate = getTopbarKalenderEventsByDate()
  const headers = ['Min', 'Sen', 'Sel', 'Rab', 'Kam', 'Jum', 'Sab']
  let html = headers.map(label => `<div class="topbar-calendar-day-head">${label}</div>`).join('')

  for (let i = 0; i < meta.firstWeekday; i += 1) html += '<div class="topbar-calendar-day empty"></div>'

  for (let day = 1; day <= meta.daysInMonth; day += 1) {
    const dateKey = `${meta.year}-${String(meta.month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    const events = byDate.get(dateKey) || []
    const firstColor = normalizeTopbarKalenderColor(events[0]?.warna)
    const isActive = topbarKalenderState.selectedDateKey === dateKey
    const isToday = dateKey === todayKey
    const bg = getTopbarKalenderDayBg(events)
    const dots = events.slice(0, 5).map(item => `<span class="topbar-calendar-day-dot" style="background:${escapeHtml(normalizeTopbarKalenderColor(item.warna))};"></span>`).join('')
    const ring = isToday ? '0 0 0 2px #2563eb inset' : ''
    html += `
      <button type="button" class="topbar-calendar-day ${isActive ? 'active' : ''}" onclick="selectTopbarCalendarDate('${dateKey}')" style="background:${bg}; box-shadow:${ring};">
        <span class="topbar-calendar-day-num" style="background:${events.length ? `${firstColor}55` : '#f1f5f9'};">${day}</span>
        <div class="topbar-calendar-day-dots">${dots}</div>
      </button>
    `
  }

  const trailing = 42 - (meta.firstWeekday + meta.daysInMonth)
  for (let i = 0; i < trailing; i += 1) html += '<div class="topbar-calendar-day empty"></div>'

  gridEl.innerHTML = html
  renderTopbarCalendarDetail()
}

async function openTopbarCalendarPopup() {
  const popup = ensureTopbarKalenderPopup()
  if (!popup) return
  popup.style.display = 'flex'
  topbarKalenderState.visible = true
  if (!topbarKalenderState.month) topbarKalenderState.month = getTopbarKalenderMonthNow()
  try {
    await loadTopbarCalendarData(false)
  } catch (error) {
    console.error(error)
    topbarKalenderState.list = []
    const detailEl = document.getElementById('topbar-calendar-detail')
    if (detailEl) detailEl.innerHTML = `Gagal load kalender: ${escapeHtml(error?.message || 'Unknown error')}`
  }
  renderTopbarCalendar()
}

function closeTopbarCalendarPopup() {
  const popup = document.getElementById('topbar-calendar-popup')
  if (!popup) return
  popup.style.display = 'none'
  topbarKalenderState.visible = false
}

function shiftTopbarCalendarMonth(step) {
  const meta = getTopbarKalenderMonthMeta(topbarKalenderState.month || getTopbarKalenderMonthNow())
  const date = new Date(meta.year, meta.month - 1 + Number(step || 0), 1)
  topbarKalenderState.month = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
  topbarKalenderState.selectedDateKey = ''
  renderTopbarCalendar()
}

function selectTopbarCalendarDate(dateKey) {
  topbarKalenderState.selectedDateKey = String(dateKey || '')
  renderTopbarCalendar()
}

function setAkademikSidebarMenuExpanded(expanded) {
  const submenu = document.getElementById('sidebar-akademik-submenu')
  if (!submenu) return
  animateSidebarSubmenu(submenu, expanded)

  const parentBtn = document.querySelector('.sidebar-parent-btn[data-page="akademik"]')
  if (parentBtn) {
    parentBtn.classList.toggle('expanded', expanded)
  }
}

function setKaryawanSidebarMenuExpanded(expanded) {
  const submenu = document.getElementById('sidebar-karyawan-submenu')
  if (!submenu) return
  animateSidebarSubmenu(submenu, expanded)

  const parentBtn = document.querySelector('.sidebar-parent-btn[data-page="karyawan"]')
  if (parentBtn) {
    parentBtn.classList.toggle('expanded', expanded)
  }
}

function setKetahfizanSidebarMenuExpanded(expanded) {
  const submenu = document.getElementById('sidebar-ketahfizan-submenu')
  if (!submenu) return
  animateSidebarSubmenu(submenu, expanded)

  const parentBtn = document.querySelector('.sidebar-parent-btn[data-page="ketahfizan"]')
  if (parentBtn) {
    parentBtn.classList.toggle('expanded', expanded)
  }
}

function setKesantrianSidebarMenuExpanded(expanded) {
  const submenu = document.getElementById('sidebar-kesantrian-submenu')
  if (!submenu) return
  animateSidebarSubmenu(submenu, expanded)

  const parentBtn = document.querySelector('.sidebar-parent-btn[data-page="kesantrian"]')
  if (parentBtn) {
    parentBtn.classList.toggle('expanded', expanded)
  }
}

function animateSidebarSubmenu(submenu, expand) {
  if (!submenu) return

  if (!submenu.dataset.animBound) {
    submenu.addEventListener('transitionend', event => {
      if (event.propertyName !== 'max-height') return
      if (submenu.classList.contains('open')) {
        submenu.style.maxHeight = 'none'
        submenu.classList.add('content-visible')
      }
    })
    submenu.dataset.animBound = '1'
  }

  const isOpen = submenu.classList.contains('open')

  if (expand) {
    if (isOpen) {
      submenu.classList.add('content-visible')
      submenu.style.maxHeight = 'none'
      return
    }

    submenu.classList.remove('content-visible')
    submenu.classList.add('open')
    submenu.style.maxHeight = '0px'
    requestAnimationFrame(() => {
      submenu.style.maxHeight = `${submenu.scrollHeight}px`
    })
    return
  }

  if (!isOpen) {
    submenu.classList.remove('content-visible')
    submenu.style.maxHeight = '0px'
    return
  }

  const currentHeight = submenu.scrollHeight
  submenu.classList.remove('content-visible')
  submenu.style.maxHeight = `${currentHeight}px`
  submenu.getBoundingClientRect()
  submenu.classList.remove('open')
  submenu.style.maxHeight = '0px'
}

function toggleAkademikSidebarMenu() {
  const parentBtn = document.querySelector('.sidebar-parent-btn[data-page="akademik"]')
  if (isAdminSidebarIconsOnlyMode()) {
    if (parentBtn) showAdminSidebarTooltip(parentBtn, { persistent: true, interactive: true })
    return
  }
  const submenu = document.getElementById('sidebar-akademik-submenu')
  if (!submenu) return
  const willExpand = !submenu.classList.contains('open')
  setAkademikSidebarMenuExpanded(willExpand)
  if (willExpand) {
    setKaryawanSidebarMenuExpanded(false)
    setKetahfizanSidebarMenuExpanded(false)
    setKesantrianSidebarMenuExpanded(false)
  }
}

function toggleKaryawanSidebarMenu() {
  const parentBtn = document.querySelector('.sidebar-parent-btn[data-page="karyawan"]')
  if (isAdminSidebarIconsOnlyMode()) {
    if (parentBtn) showAdminSidebarTooltip(parentBtn, { persistent: true, interactive: true })
    return
  }
  const submenu = document.getElementById('sidebar-karyawan-submenu')
  if (!submenu) return
  const willExpand = !submenu.classList.contains('open')
  setKaryawanSidebarMenuExpanded(willExpand)
  if (willExpand) {
    setAkademikSidebarMenuExpanded(false)
    setKetahfizanSidebarMenuExpanded(false)
    setKesantrianSidebarMenuExpanded(false)
  }
}

function toggleKetahfizanSidebarMenu() {
  const parentBtn = document.querySelector('.sidebar-parent-btn[data-page="ketahfizan"]')
  if (isAdminSidebarIconsOnlyMode()) {
    if (parentBtn) showAdminSidebarTooltip(parentBtn, { persistent: true, interactive: true })
    return
  }
  const submenu = document.getElementById('sidebar-ketahfizan-submenu')
  if (!submenu) return
  const willExpand = !submenu.classList.contains('open')
  setKetahfizanSidebarMenuExpanded(willExpand)
  if (willExpand) {
    setAkademikSidebarMenuExpanded(false)
    setKaryawanSidebarMenuExpanded(false)
    setKesantrianSidebarMenuExpanded(false)
  }
}

function toggleKesantrianSidebarMenu() {
  const parentBtn = document.querySelector('.sidebar-parent-btn[data-page="kesantrian"]')
  if (isAdminSidebarIconsOnlyMode()) {
    if (parentBtn) showAdminSidebarTooltip(parentBtn, { persistent: true, interactive: true })
    return
  }
  const submenu = document.getElementById('sidebar-kesantrian-submenu')
  if (!submenu) return
  const willExpand = !submenu.classList.contains('open')
  setKesantrianSidebarMenuExpanded(willExpand)
  if (willExpand) {
    setAkademikSidebarMenuExpanded(false)
    setKetahfizanSidebarMenuExpanded(false)
    setKaryawanSidebarMenuExpanded(false)
  }
}

function getAkademikPageFromSubtab(subtab) {
  if (subtab === 'kelas') return 'kelas'
  if (subtab === 'jadwal') return 'jadwal'
  if (subtab === 'ujian') return 'ujian'
  if (subtab === 'ekstrakurikuler') return 'ekstrakurikuler'
  if (subtab === 'jadwal-ujian') return 'jadwal-ujian'
  if (subtab === 'kalender-akademik') return 'kalender-akademik'
  if (subtab === 'set-tugas') return 'set-tugas'
  if (subtab === 'santri') return 'santri'
  if (subtab === 'perizinan-santri') return 'perizinan-santri'
  if (subtab === 'prestasi-pelanggaran') return 'prestasi-pelanggaran'
  if (subtab === 'alumni') return 'alumni'
  if (subtab === 'distribusi-mapel') return 'kelas-distribusi-mapel'
  if (subtab === 'guru-mapel') return 'kelas-guru-mapel'
  return 'kelas'
}

function loadAkademikFromSidebar(subtab) {
  const page = getAkademikPageFromSubtab(subtab)
  loadPage(page, { subtab })
}

function getKaryawanPageFromSubtab(subtab) {
  if (subtab === 'karyawan') return 'karyawan'
  if (subtab === 'guru') return 'guru'
  if (subtab === 'kehadiran-guru') return 'kehadiran-guru'
  if (subtab === 'perizinan-karyawan') return 'perizinan-karyawan'
  return 'karyawan'
}

function loadKaryawanFromSidebar(subtab) {
  const page = getKaryawanPageFromSubtab(subtab)
  loadPage(page, { subtab })
}

function getKetahfizanPageFromSubtab(subtab) {
  if (subtab === 'halaqah') return 'ketahfizan-halaqah'
  if (subtab === 'hafalan') return 'ketahfizan-hafalan'
  if (subtab === 'jadwal-halaqah') return 'ketahfizan-jadwal'
  return 'ketahfizan-halaqah'
}

function loadKetahfizanFromSidebar(subtab) {
  const page = getKetahfizanPageFromSubtab(subtab)
  loadPage(page, { subtab })
}

function getKesantrianPageFromSubtab(subtab) {
  if (subtab === 'kamar') return 'kesantrian-kamar'
  return 'kesantrian-kamar'
}

function loadKesantrianFromSidebar(subtab) {
  const page = getKesantrianPageFromSubtab(subtab)
  loadPage(page, { subtab })
}

window.syncKelasSidebarSelection = function syncKelasSidebarSelection(subtab) {
  const page = getAkademikPageFromSubtab(subtab)
  localStorage.setItem('admin_last_page', page)
  localStorage.setItem('admin_last_page_params', JSON.stringify({ subtab }))
  setActiveSidebarTab(page)
  setTopbarTitle(page)
}


// =======================
// PAGE ROUTER
// =======================
function loadPage(page, params = {}, options = {}) {
  const { updateHistory = true, replaceHistory = false } = options || {}
  localStorage.setItem('admin_last_page', page);
  localStorage.setItem('admin_last_page_params', JSON.stringify(params));
  const area = document.getElementById('content-area')
  if (!area) return

  renderAdminTopbarName()

  setActiveSidebarTab(page)
  setTopbarTitle(page)
  if (updateHistory) {
    if (replaceHistory) replaceAdminTabHistory(page, params)
    else pushAdminTabHistory(page, params)
  }
  setupTopbarUserMenuHandlers()
  ensureAdminTopbarChatButton()
  refreshAdminTopbarChatBadge().catch(error => console.error(error))
  closeTopbarUserMenu()
  if (page !== 'chat' && window.ChatModule && typeof window.ChatModule.stop === 'function') {
    window.ChatModule.stop()
  }

  switch (page) {
    case 'dashboard':
      renderAdminDashboardCalendar()
      break
    case 'struktur-sekolah':
      loadExternalPage('struktur-sekolah')
      break
    case 'tahun-ajaran':
      loadExternalPage('tahun-ajaran')
      break
    case 'tugas-harian':
    case 'set-tugas':
      loadExternalPage('tugas-harian-admin')
      break
    case 'kehadiran-guru':
      loadExternalPage('kehadiran-guru-admin')
      break
    case 'perizinan-karyawan':
      loadExternalPage('perizinan-karyawan-admin')
      break
    case 'kelas':
      loadExternalPage('kelas', { subtab: 'data-kelas' })
      break
    case 'kelas-distribusi-mapel':
      loadExternalPage('kelas', { subtab: 'distribusi-mapel' })
      break
    case 'kelas-guru-mapel':
      loadExternalPage('kelas', { subtab: 'guru-mapel' })
      break
    case 'santri':
      loadExternalPage('santri')
      break
    case 'perizinan-santri':
      loadExternalPage('perizinan-santri-admin')
      break
    case 'prestasi-pelanggaran':
      loadExternalPage('prestasi-pelanggaran-admin')
      break
    case 'alumni':
      loadExternalPage('alumni')
      break
    case 'jadwal':
      loadExternalPage('jadwal')
      break
    case 'ekstrakurikuler':
      loadExternalPage('ekstrakurikuler-admin')
      break
    case 'ujian':
      loadExternalPage('ujian')
      break
    case 'jadwal-ujian':
      loadExternalPage('jadwal-ujian')
      break
    case 'ketahfizan-halaqah':
      loadExternalPage('ketahfizan', { subtab: 'halaqah' })
      break
    case 'ketahfizan-hafalan':
      loadExternalPage('ketahfizan', { subtab: 'hafalan' })
      break
    case 'ketahfizan-jadwal':
      loadExternalPage('ketahfizan', { subtab: 'jadwal-halaqah' })
      break
    case 'kesantrian-kamar':
      loadExternalPage('kesantrian', { subtab: 'kamar' })
      break
    case 'kalender-akademik':
      loadExternalPage('kalender-akademik')
      break
    case 'santri-detail':
      loadExternalPage('santri-detail', params)
      break
    case 'karyawan':
      loadExternalPage('karyawan', { mode: 'all' })
      break
    case 'guru':
      loadExternalPage('karyawan', { mode: 'guru-only' })
      break
    case 'guru-detail':
      loadExternalPage('guru-detail', params)
      break
    case 'chat':
      renderAdminChatPage()
      break
  }
}

async function renderAdminChatPage() {
  const area = document.getElementById('content-area')
  if (!area) return
  area.innerHTML = 'Loading chat...'
  try {
    const { data, error } = await sb
      .from('karyawan')
      .select('id, nama, id_karyawan')
      .eq('id_karyawan', String(loginId || '').trim())
      .maybeSingle()
    if (error) throw error
    if (!data?.id) {
      area.innerHTML = '<div class="placeholder-card">Data admin tidak ditemukan.</div>'
      return
    }
    if (!window.ChatModule || typeof window.ChatModule.render !== 'function') {
      area.innerHTML = '<div class="placeholder-card">Modul chat belum termuat. Refresh halaman.</div>'
      return
    }
    await window.ChatModule.render({
      sb,
      containerId: 'content-area',
      currentUser: { id: String(data.id), nama: String(data.nama || data.id_karyawan || '-') }
    })
  } catch (error) {
    console.error(error)
    area.innerHTML = `<div class="placeholder-card">Gagal load chat: ${escapeHtml(error?.message || 'Unknown error')}</div>`
  }
}


async function loadExternalPage(page, params = {}) {
  const area = document.getElementById('content-area')
  if (!area) return

  const htmlUrl = `pages/${page}.html?v=${EXTERNAL_PAGE_ASSET_VERSION}`
  if (!externalPageHtmlCache[page]) {
    const response = await fetch(htmlUrl)
    externalPageHtmlCache[page] = await response.text()
  }
  area.innerHTML = externalPageHtmlCache[page]

  const runPageInit = () => {
    if (page === 'tahun-ajaran' && typeof initTahunAjaranPage === 'function') {
      initTahunAjaranPage()
      return
    }
    if (page === 'tugas-harian-admin' && typeof initTugasHarianAdminPage === 'function') {
      initTugasHarianAdminPage()
      return
    }
    if (page === 'kehadiran-guru-admin' && typeof initKehadiranGuruAdminPage === 'function') {
      initKehadiranGuruAdminPage()
      return
    }
    if (page === 'perizinan-karyawan-admin' && typeof initPerizinanKaryawanAdminPage === 'function') {
      initPerizinanKaryawanAdminPage()
      return
    }
    if (page === 'perizinan-santri-admin' && typeof initPerizinanSantriAdminPage === 'function') {
      initPerizinanSantriAdminPage()
      return
    }
    if (page === 'prestasi-pelanggaran-admin' && typeof initPrestasiPelanggaranAdminPage === 'function') {
      initPrestasiPelanggaranAdminPage()
      return
    }
    if (page === 'kelas' && typeof initKelasPage === 'function') {
      initKelasPage(params)
      return
    }
    if (page === 'santri' && typeof initSantriPage === 'function') {
      initSantriPage()
      return
    }
    if (page === 'alumni' && typeof initAlumniPage === 'function') {
      initAlumniPage()
      return
    }
    if (page === 'santri-detail' && typeof initSantriDetailPage === 'function') {
      initSantriDetailPage(params)
      return
    }
    if (page === 'guru-detail' && typeof initGuruDetailPage === 'function') {
      initGuruDetailPage(params)
      return
    }
    if (page === 'jadwal' && typeof initJadwalPage === 'function') {
      initJadwalPage()
      return
    }
    if (page === 'ekstrakurikuler-admin' && typeof initEkstrakurikulerAdminPage === 'function') {
      initEkstrakurikulerAdminPage()
      return
    }
    if (page === 'ujian' && typeof initUjianPage === 'function') {
      initUjianPage()
      return
    }
    if (page === 'jadwal-ujian' && typeof initJadwalUjianPage === 'function') {
      initJadwalUjianPage()
      return
    }
    if (page === 'kalender-akademik' && typeof initKalenderAkademikPage === 'function') {
      initKalenderAkademikPage()
      return
    }
    if (page === 'struktur-sekolah' && typeof initStrukturSekolahPage === 'function') {
      initStrukturSekolahPage()
      return
    }
    if (page === 'karyawan' && typeof initKaryawanPage === 'function') {
      initKaryawanPage(params)
      return
    }
    if (page === 'ketahfizan' && typeof initKetahfizanPage === 'function') {
      initKetahfizanPage(params)
      return
    }
    if (page === 'kesantrian' && typeof initKesantrianPage === 'function') {
      initKesantrianPage(params)
    }
  }

  if (externalPageScriptLoaded[page]) {
    runPageInit()
    return
  }

  const script = document.createElement('script')
  script.src = `pages/${page}.js?v=${EXTERNAL_PAGE_ASSET_VERSION}`
  script.defer = true
  script.onload = () => {
    externalPageScriptLoaded[page] = true
    runPageInit()
  }
  document.body.appendChild(script)
}




