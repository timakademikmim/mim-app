const supabaseUrl = 'https://optucpelkueqmlhwlbej.supabase.co'
const supabaseKey = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9wdHVjcGVsa3VlcW1saHdsYmVqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzAxOTY4MTgsImV4cCI6MjA4NTc3MjgxOH0.Vqaey9pcnltu9uRbPk0J-AGWaGDZjQLw92pcRv67GNE'
const sb = supabase.createClient(supabaseUrl, supabaseKey)

const MONTHLY_REPORT_TABLE = 'laporan_bulanan_wali'
const MUHAFFIZ_LAST_PAGE_KEY = 'musyrif_last_page'
const TOPBAR_KALENDER_TABLE = 'kalender_akademik'
const TOPBAR_KALENDER_DEFAULT_COLOR = '#2563eb'
const HALAQAH_TABLE = 'kamar'
const HALAQAH_SANTRI_TABLE = 'kamar_santri'
const HALAQAH_SCHEDULE_TABLE = 'jadwal_halaqah'
const TOPBAR_NOTIF_READ_KEY = 'musyrif_topbar_notif_read'
const TOPBAR_NOTIF_RANGE_KEY = 'musyrif_topbar_notif_range_days'

const PAGE_TITLES = {
  dashboard: 'Dashboard',
  'laporan-bulanan': 'Laporan Bulanan',
  profil: 'Profil'
}

let musyrifState = {
  profile: null,
  kelasMap: new Map(),
  santriList: [],
  periode: '',
  selectedSantriId: '',
  dashboardAgenda: [],
  detail: null
}
let topbarKalenderState = {
  list: [],
  month: '',
  selectedDateKey: '',
  visible: false
}
let topbarNotifState = {
  items: [],
  loaded: false,
  rangeDays: 3,
  readMap: {}
}
let musyrifDashboardAgendaRows = []

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

function asBool(value) {
  if (value === true || value === 1) return true
  const text = String(value ?? '').trim().toLowerCase()
  return text === 'true' || text === 't' || text === '1' || text === 'yes'
}

function toInputValue(value) {
  return value === null || value === undefined ? '' : String(value)
}

function toNullableNumber(rawValue) {
  const text = String(rawValue ?? '').trim()
  if (!text) return null
  const num = Number(text)
  if (!Number.isFinite(num)) return NaN
  return num
}

function toTimeLabel(value) {
  const text = String(value || '').trim()
  if (!text) return '-'
  return text.length >= 5 ? text.slice(0, 5) : text
}

function getMonthInputToday() {
  return new Date().toISOString().slice(0, 7)
}

function getPeriodeRange(periode) {
  const text = String(periode || '').trim()
  if (!/^\d{4}-\d{2}$/.test(text)) return null
  const [yearText, monthText] = text.split('-')
  const year = Number(yearText)
  const month = Number(monthText)
  if (!Number.isFinite(year) || !Number.isFinite(month) || month < 1 || month > 12) return null
  const start = `${yearText}-${monthText}-01`
  const endDate = new Date(year, month, 0)
  const end = `${yearText}-${monthText}-${String(endDate.getDate()).padStart(2, '0')}`
  return { start, end, year, month }
}

function getPeriodeLabel(periode) {
  const range = getPeriodeRange(periode)
  if (!range) return '-'
  const monthNames = [
    'Januari', 'Februari', 'Maret', 'April', 'Mei', 'Juni',
    'Juli', 'Agustus', 'September', 'Oktober', 'November', 'Desember'
  ]
  return `${monthNames[range.month - 1]} ${range.year}`
}

function normalizeAkhlakGrade(value) {
  const raw = String(value ?? '').trim().toUpperCase()
  if (['A', 'B', 'C', 'D', 'E'].includes(raw)) return raw
  const num = Number(value)
  if (!Number.isFinite(num)) return ''
  if (num >= 90) return 'A'
  if (num >= 80) return 'B'
  if (num >= 70) return 'C'
  if (num >= 60) return 'D'
  return 'E'
}

function getAkhlakKeteranganByGrade(grade) {
  const g = normalizeAkhlakGrade(grade)
  if (g === 'A') return 'Istimewa'
  if (g === 'B') return 'Baik Sekali'
  if (g === 'C') return 'Baik'
  if (g === 'D') return 'Kurang'
  if (g === 'E') return 'Sangat Kurang'
  return ''
}

function getUjianBulananKeterangan(value) {
  return normalizeAkhlakGrade(value)
}

function setupCustomPopupSystem() {
  if (window.__popupReady) return
  const overlay = document.getElementById('app-popup-overlay')
  const messageEl = document.getElementById('app-popup-message')
  const actionsEl = document.getElementById('app-popup-actions')
  const okBtn = document.getElementById('app-popup-ok-btn')
  if (!overlay || !messageEl || !actionsEl || !okBtn) return

  const closePopup = () => {
    overlay.classList.remove('open')
    overlay.setAttribute('aria-hidden', 'true')
    actionsEl.innerHTML = ''
  }

  window.showPopupMessage = function showPopupMessage(message) {
    return new Promise(resolve => {
      messageEl.textContent = String(message ?? '')
      actionsEl.innerHTML = ''
      const btn = document.createElement('button')
      btn.type = 'button'
      btn.textContent = 'OK'
      btn.className = 'app-popup-primary'
      btn.onclick = () => {
        closePopup()
        resolve(true)
      }
      actionsEl.appendChild(btn)
      overlay.classList.add('open')
      overlay.setAttribute('aria-hidden', 'false')
      btn.focus()
    })
  }

  window.alert = function customAlert(message) {
    window.showPopupMessage(message)
  }

  overlay.addEventListener('click', event => {
    if (event.target !== overlay) return
    const primaryButton = actionsEl.querySelector('button.app-popup-primary') || actionsEl.querySelector('button')
    if (primaryButton) primaryButton.click()
  })

  window.__popupReady = true
}

async function getCurrentMusyrif() {
  const loginId = String(localStorage.getItem('login_id') || '').trim()
  if (!loginId) return null
  const { data, error } = await sb
    .from('karyawan')
    .select('id, id_karyawan, nama, no_hp, alamat, aktif')
    .eq('id_karyawan', loginId)
    .maybeSingle()
  if (error) throw error
  if (!data || !asBool(data.aktif)) return null
  return data
}

async function setMusyrifWelcomeName() {
  try {
    const profile = await getCurrentMusyrif()
    if (!profile) return
    musyrifState.profile = profile
    const welcomeEl = document.getElementById('welcome')
    if (welcomeEl) welcomeEl.textContent = String(profile.nama || profile.id_karyawan || '-')
  } catch (error) {
    console.error(error)
  }
}

function setNavActive(page) {
  document.querySelectorAll('.guru-nav-btn').forEach(btn => {
    btn.classList.toggle('active', btn.getAttribute('data-page') === page)
  })
}

function setTopbarTitle(page) {
  const el = document.getElementById('musyrif-topbar-title')
  if (!el) return
  const title = PAGE_TITLES[page] || PAGE_TITLES.dashboard
  const todayLabel = new Date().toLocaleDateString('id-ID', {
    weekday: 'long',
    day: '2-digit',
    month: 'long',
    year: 'numeric'
  })
  el.innerHTML = `<span class="topbar-title-main">${escapeHtml(title)}</span><span class="topbar-title-sep" aria-hidden="true"></span><button type="button" class="topbar-title-date-btn" onclick="openTopbarCalendarPopup()" title="Lihat kalender kegiatan">${escapeHtml(todayLabel)}</button>`
}

function toggleTopbarUserMenu() {
  const menu = document.getElementById('topbar-user-menu')
  if (!menu) return
  closeTopbarNotifMenu()
  menu.classList.toggle('open')
}

function closeTopbarUserMenu() {
  const menu = document.getElementById('topbar-user-menu')
  if (!menu) return
  menu.classList.remove('open')
}

function closeTopbarNotifMenu() {
  const menu = document.getElementById('topbar-notif-menu')
  if (!menu) return
  menu.classList.remove('open')
}

function loadMusyrifNotifPrefs() {
  try {
    const raw = localStorage.getItem(TOPBAR_NOTIF_READ_KEY)
    const parsed = raw ? JSON.parse(raw) : {}
    topbarNotifState.readMap = parsed && typeof parsed === 'object' ? parsed : {}
  } catch (_err) {
    topbarNotifState.readMap = {}
  }
  const rangeRaw = Number(localStorage.getItem(TOPBAR_NOTIF_RANGE_KEY) || '3')
  topbarNotifState.rangeDays = [1, 3, 7].includes(rangeRaw) ? rangeRaw : 3
}

function saveMusyrifNotifReadMap() {
  try {
    localStorage.setItem(TOPBAR_NOTIF_READ_KEY, JSON.stringify(topbarNotifState.readMap || {}))
  } catch (_err) {}
}

function setMusyrifNotifRangeDays(days) {
  const value = Number(days)
  topbarNotifState.rangeDays = [1, 3, 7].includes(value) ? value : 3
  localStorage.setItem(TOPBAR_NOTIF_RANGE_KEY, String(topbarNotifState.rangeDays))
}

function buildMusyrifNotifDateKeys() {
  const keys = []
  const span = Number(topbarNotifState.rangeDays || 3)
  const now = new Date()
  for (let i = 0; i < span; i += 1) {
    const date = new Date(now.getTime() + (i * 24 * 60 * 60 * 1000))
    const key = getTopbarKalenderDateKey(date)
    if (key) keys.push(key)
  }
  return keys
}

function ensureTopbarNotification() {
  const wrap = document.querySelector('.topbar-user-menu-wrap')
  if (!wrap) return
  if (document.getElementById('topbar-notif-trigger') && document.getElementById('topbar-notif-menu')) return

  const notifBtn = document.createElement('button')
  notifBtn.type = 'button'
  notifBtn.id = 'topbar-notif-trigger'
  notifBtn.className = 'topbar-notif-trigger'
  notifBtn.title = 'Notifikasi Aktivitas'
  notifBtn.innerHTML = '<span aria-hidden="true">&#128276;</span><span id="topbar-notif-badge" class="topbar-notif-badge hidden">0</span>'
  notifBtn.addEventListener('click', async event => {
    event.preventDefault()
    event.stopPropagation()
    await toggleTopbarNotifMenu()
  })

  const notifMenu = document.createElement('div')
  notifMenu.id = 'topbar-notif-menu'
  notifMenu.className = 'topbar-notif-menu'
  notifMenu.innerHTML = '<div class="topbar-notif-head">Aktivitas</div><div class="topbar-notif-empty">Memuat notifikasi...</div>'
  notifMenu.addEventListener('click', event => {
    event.stopPropagation()
  })

  wrap.insertBefore(notifBtn, wrap.firstChild)
  wrap.insertBefore(notifMenu, notifBtn.nextSibling)
}

function formatNotifDateLabel(dateKey) {
  const text = String(dateKey || '').trim()
  if (!text) return '-'
  const date = new Date(`${text}T00:00:00`)
  if (Number.isNaN(date.getTime())) return text
  return date.toLocaleDateString('id-ID', { weekday: 'long', day: '2-digit', month: 'long' })
}

function getMusyrifNotifId(item) {
  return String(item?.id || `${item?.type || ''}|${item?.title || ''}|${item?.meta || ''}|${item?.sortKey || ''}`)
}

function isMusyrifNotifRead(item) {
  return topbarNotifState.readMap?.[getMusyrifNotifId(item)] === true
}

function markMusyrifNotifRead(id) {
  const key = String(id || '').trim()
  if (!key) return
  if (!topbarNotifState.readMap) topbarNotifState.readMap = {}
  topbarNotifState.readMap[key] = true
  saveMusyrifNotifReadMap()
}

function markMusyrifNotifItemRead(id) {
  markMusyrifNotifRead(id)
  renderTopbarNotifMenu(topbarNotifState.items)
  setTopbarNotifBadge((topbarNotifState.items || []).filter(item => !isMusyrifNotifRead(item)).length)
}

function markAllMusyrifNotifRead() {
  ;(topbarNotifState.items || []).forEach(item => markMusyrifNotifRead(getMusyrifNotifId(item)))
  renderTopbarNotifMenu(topbarNotifState.items)
  setTopbarNotifBadge(0)
}

function setMusyrifNotifRangeFilter(days) {
  setMusyrifNotifRangeDays(days)
  topbarNotifState.loaded = false
  refreshMusyrifTopbarNotifications(true).catch(error => console.error(error))
}

function renderTopbarNotifMenu(items = []) {
  const menu = document.getElementById('topbar-notif-menu')
  if (!menu) return
  const list = Array.isArray(items) ? items : []
  const selectedRange = Number(topbarNotifState.rangeDays || 3)
  const filtersHtml = [1, 3, 7].map(days => (
    `<button type="button" class="topbar-notif-filter-btn ${selectedRange === days ? 'active' : ''}" onclick="setMusyrifNotifRangeFilter(${days})">${days === 1 ? 'Hari ini' : `${days} hari`}</button>`
  )).join('')
  const headHtml = `
    <div class="topbar-notif-head">
      <div class="topbar-notif-head-row">
        <span>Aktivitas Terdekat</span>
        <button type="button" class="topbar-notif-mark-btn" onclick="markAllMusyrifNotifRead()">Tandai semua dibaca</button>
      </div>
      <div class="topbar-notif-filters">${filtersHtml}</div>
    </div>
  `
  if (!list.length) {
    menu.innerHTML = `${headHtml}<div class="topbar-notif-empty">Belum ada notifikasi aktivitas terdekat.</div>`
    return
  }
  const rowsHtml = list.map(item => `
    <button type="button" class="topbar-notif-item ${isMusyrifNotifRead(item) ? 'read' : 'unread'}" data-notif-id="${escapeHtml(getMusyrifNotifId(item))}" onclick="markMusyrifNotifItemRead(this.getAttribute('data-notif-id'))">
      <div class="topbar-notif-type">${escapeHtml(item.type || 'Aktivitas')}</div>
      <div class="topbar-notif-title">${escapeHtml(item.title || '-')}</div>
      <div class="topbar-notif-meta">${escapeHtml(item.meta || '-')}</div>
      ${item.desc ? `<div class="topbar-notif-desc">${escapeHtml(item.desc)}</div>` : ''}
      <div class="topbar-notif-status">${isMusyrifNotifRead(item) ? 'Dibaca' : 'Belum dibaca'}</div>
    </button>
  `).join('')
  menu.innerHTML = `${headHtml}${rowsHtml}`
}

function setTopbarNotifBadge(count) {
  const badge = document.getElementById('topbar-notif-badge')
  if (!badge) return
  const total = Number.isFinite(Number(count)) ? Number(count) : 0
  if (total <= 0) {
    badge.textContent = '0'
    badge.classList.add('hidden')
    return
  }
  badge.classList.remove('hidden')
  badge.textContent = total > 99 ? '99+' : String(total)
}

async function fetchMusyrifTopbarNotifications() {
  const items = []
  const dateKeys = buildMusyrifNotifDateKeys()

  try {
    await loadTopbarCalendarData()
    const byDate = getTopbarKalenderEventsByDate()
    const seenCalendar = new Set()
    dateKeys.forEach(dateKey => {
      const rows = byDate.get(dateKey) || []
      rows.forEach(row => {
        const id = String(row?.id || '')
        if (!id || seenCalendar.has(id)) return
        seenCalendar.add(id)
        const mulai = getTopbarKalenderDateKey(row?.mulai)
        const selesai = getTopbarKalenderDateKey(row?.selesai || row?.mulai)
        const range = selesai && selesai !== mulai ? `${formatNotifDateLabel(mulai)} - ${formatNotifDateLabel(selesai)}` : formatNotifDateLabel(mulai)
        items.push({
          id: `agenda|${id}`,
          type: 'Agenda Akademik',
          title: String(row?.judul || '-'),
          meta: range || '-',
          desc: String(row?.detail || '').trim(),
          sortKey: `${mulai || dateKey} 00:00`
        })
      })
    })
  } catch (error) {
    console.error(error)
  }

  try {
    const musyrifId = String(musyrifState?.profile?.id || '').trim()
    if (musyrifId) {
      const [halaqahRes, jadwalRes] = await Promise.all([
        sb.from(HALAQAH_TABLE).select('id, nama').eq('musyrif_id', musyrifId).order('nama'),
        sb.from(HALAQAH_SCHEDULE_TABLE).select('id, nama_sesi, jam_mulai, jam_selesai, urutan').order('urutan', { ascending: true }).order('jam_mulai', { ascending: true })
      ])
      if (!halaqahRes.error && !jadwalRes.error) {
        const halaqahNames = (halaqahRes.data || []).map(item => String(item?.nama || '').trim()).filter(Boolean)
        const halaqahLabel = halaqahNames.length ? halaqahNames.join(', ') : 'Belum ada kamar binaan'
        dateKeys.forEach(dateKey => {
          ;(jadwalRes.data || []).forEach(row => {
            items.push({
              id: `halaqah|${dateKey}|${String(row?.id || '')}`,
              type: 'Jam Halaqah',
              title: String(row?.nama_sesi || 'Sesi Halaqah'),
              meta: `${formatNotifDateLabel(dateKey)} | ${toTimeLabel(row?.jam_mulai)}-${toTimeLabel(row?.jam_selesai)}`,
              desc: `Kamar: ${halaqahLabel}`,
              sortKey: `${dateKey} ${String(row?.jam_mulai || '00:00')}`
            })
          })
        })
      }
    }
  } catch (error) {
    console.error(error)
  }

  items.sort((a, b) => String(a.sortKey || '').localeCompare(String(b.sortKey || '')))
  return items.slice(0, 30)
}

async function refreshMusyrifTopbarNotifications(forceReload = false) {
  ensureTopbarNotification()
  if (!forceReload && topbarNotifState.loaded) {
    renderTopbarNotifMenu(topbarNotifState.items)
    setTopbarNotifBadge((topbarNotifState.items || []).filter(item => !isMusyrifNotifRead(item)).length)
    return
  }
  const items = await fetchMusyrifTopbarNotifications()
  topbarNotifState.items = items
  topbarNotifState.loaded = true
  renderTopbarNotifMenu(items)
  setTopbarNotifBadge(items.filter(item => !isMusyrifNotifRead(item)).length)
}

async function toggleTopbarNotifMenu() {
  ensureTopbarNotification()
  const menu = document.getElementById('topbar-notif-menu')
  if (!menu) return
  const willOpen = !menu.classList.contains('open')
  closeTopbarUserMenu()
  if (!willOpen) {
    closeTopbarNotifMenu()
    return
  }
  menu.classList.add('open')
  menu.innerHTML = '<div class="topbar-notif-head">Aktivitas</div><div class="topbar-notif-empty">Memuat notifikasi...</div>'
  await refreshMusyrifTopbarNotifications(true)
}

async function renderDashboard() {
  const content = document.getElementById('musyrif-content')
  if (!content) return
  content.innerHTML = '<div class="placeholder-card">Loading agenda kalender akademik...</div>'

  try {
    await loadTopbarCalendarData()
    const rows = (topbarKalenderState.list || []).slice()
      .sort((a, b) => String(a?.mulai || '').localeCompare(String(b?.mulai || '')))
    musyrifDashboardAgendaRows = rows

    if (!rows.length) {
      content.innerHTML = `
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

    const rowsHtml = rows.map(item => {
      const warna = normalizeTopbarKalenderColor(item?.warna)
      const rentang = getMusyrifDashboardCalendarRangeLabel(item)
      const itemId = String(item?.id || '')
      return `
        <button type="button" onclick="openMusyrifDashboardAgendaPopup('${escapeHtml(itemId)}')" style="text-align:left; width:100%; min-height:210px; position:relative; border:1px solid #e2e8f0; border-radius:16px; background:linear-gradient(180deg,#ffffff 0%,#f8fafc 100%); box-shadow:0 12px 24px rgba(15,23,42,0.08); padding:22px 20px 18px 22px; overflow:hidden; cursor:pointer;">
          <span style="pointer-events:none; position:absolute; inset:0; background:linear-gradient(92deg, ${escapeHtml(warna)}0b 0%, ${escapeHtml(warna)}08 20%, rgba(255,255,255,0) 54%), linear-gradient(165deg, rgba(255,255,255,0.42) 0%, rgba(255,255,255,0) 38%); box-shadow:inset 1px 0 8px ${escapeHtml(warna)}1a;"></span>
          <div style="display:flex; flex-direction:column; align-items:center; justify-content:center; gap:10px; min-height:160px; text-align:center;">
            <div style="font-family:'Poppins',sans-serif; font-size:54px; font-weight:700; color:#0f172a; line-height:1.2;">${escapeHtml(item?.judul || '-')}</div>
            <span style="font-family:'Poppins',sans-serif; font-size:24px; font-weight:700; color:#334155; background:#ffffff; border:none; border-radius:999px; padding:6px 12px; white-space:nowrap;">${escapeHtml(rentang)}</span>
          </div>
        </button>
      `
    }).join('')

    content.innerHTML = `
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
          ${rowsHtml}
        </div>
      </div>
    `
  } catch (error) {
    console.error(error)
    content.innerHTML = `<div class="placeholder-card">Gagal load agenda dashboard: ${escapeHtml(error?.message || 'Unknown error')}</div>`
  }
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

async function loadTopbarCalendarData() {
  topbarKalenderState.list = await fetchTopbarKalenderRows()
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
    await loadTopbarCalendarData()
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

function formatMusyrifDashboardCalendarDate(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleDateString('id-ID', { day: '2-digit', month: 'short', year: 'numeric' })
}

function getMusyrifDashboardCalendarRangeLabel(item) {
  const startKey = getTopbarKalenderDateKey(item?.mulai)
  const endKey = getTopbarKalenderDateKey(item?.selesai || item?.mulai)
  if (!startKey) return '-'
  if (!endKey || endKey === startKey) return formatMusyrifDashboardCalendarDate(startKey)
  return `${formatMusyrifDashboardCalendarDate(startKey)} - ${formatMusyrifDashboardCalendarDate(endKey)}`
}

function ensureMusyrifDashboardAgendaPopup() {
  let popup = document.getElementById('musyrif-dashboard-agenda-popup')
  if (popup) return popup
  popup = document.createElement('div')
  popup.id = 'musyrif-dashboard-agenda-popup'
  popup.style.cssText = 'position:fixed; inset:0; background:rgba(15,23,42,0.35); display:none; align-items:center; justify-content:center; z-index:10001; padding:16px; box-sizing:border-box;'
  popup.innerHTML = `
    <div style="width:min(680px, calc(100vw - 32px)); max-height:calc(100vh - 32px); overflow:auto; border:1px solid #dbeafe; border-radius:0; background:#fff; box-shadow:0 18px 34px rgba(15,23,42,0.18); padding:14px 16px; position:relative;">
      <button type="button" onclick="closeMusyrifDashboardAgendaPopup()" style="position:absolute; right:12px; top:10px; border:1px solid #cbd5e1; background:#fff; border-radius:999px; width:28px; height:28px; cursor:pointer;">×</button>
      <div id="musyrif-dashboard-agenda-popup-body"></div>
    </div>
  `
  popup.addEventListener('click', event => {
    if (event.target !== popup) return
    closeMusyrifDashboardAgendaPopup()
  })
  document.body.appendChild(popup)
  return popup
}

function openMusyrifDashboardAgendaPopup(id) {
  const sid = String(id || '')
  const selected = musyrifDashboardAgendaRows.find(item => String(item?.id || '') === sid)
  if (!selected) return
  const popup = ensureMusyrifDashboardAgendaPopup()
  const body = document.getElementById('musyrif-dashboard-agenda-popup-body')
  if (!popup || !body) return
  const warna = normalizeTopbarKalenderColor(selected?.warna)
  body.innerHTML = `
    <div style="display:flex; align-items:flex-start; justify-content:space-between; gap:10px; margin-bottom:8px; padding-right:30px;">
      <div style="font-size:20px; font-weight:700; color:#0f172a; line-height:1.35;">${escapeHtml(selected?.judul || '-')}</div>
      <span style="width:12px; height:12px; border-radius:999px; background:${escapeHtml(warna)}; margin-top:8px;"></span>
    </div>
    <div style="font-size:12px; font-weight:600; color:#334155; margin-bottom:8px;">${escapeHtml(getMusyrifDashboardCalendarRangeLabel(selected))}</div>
    <div style="font-size:14px; color:#475569; line-height:1.7; white-space:pre-wrap;">${escapeHtml(selected?.detail || '-')}</div>
  `
  popup.style.display = 'flex'
}

function closeMusyrifDashboardAgendaPopup() {
  const popup = document.getElementById('musyrif-dashboard-agenda-popup')
  if (!popup) return
  popup.style.display = 'none'
}

function ensureMusyrifBulkModal() {
  let overlay = document.getElementById('musyrif-bulk-overlay')
  if (overlay) return overlay

  overlay = document.createElement('div')
  overlay.id = 'musyrif-bulk-overlay'
  overlay.style.cssText = 'position:fixed; inset:0; background:rgba(15,23,42,0.4); display:none; align-items:center; justify-content:center; z-index:12050; padding:14px; box-sizing:border-box;'
  overlay.innerHTML = `
    <div style="width:min(98vw, 1500px); max-height:calc(100vh - 28px); background:#fff; border:1px solid #e2e8f0; border-radius:12px; display:flex; flex-direction:column;">
      <div style="padding:12px 14px; border-bottom:1px solid #e2e8f0; display:flex; align-items:center; justify-content:space-between; gap:8px;">
        <div style="font-weight:700; color:#0f172a;">Input Massal Kesantrian</div>
        <button type="button" class="modal-btn" onclick="closeMusyrifBulkInputModal()">Tutup</button>
      </div>
      <div id="musyrif-bulk-body" style="padding:12px; overflow:auto;">Loading...</div>
    </div>
  `
  overlay.addEventListener('click', event => {
    if (event.target !== overlay) return
    closeMusyrifBulkInputModal()
  })
  document.body.appendChild(overlay)
  return overlay
}

function closeMusyrifBulkInputModal() {
  const overlay = document.getElementById('musyrif-bulk-overlay')
  if (!overlay) return
  overlay.style.display = 'none'
}

function validateMusyrifBulkRow(row) {
  const errors = []
  const nama = String(row.nama || '-')

  const nilaiKehadiran = toNullableNumber(row.nilaiKehadiran)
  const sakit = toNullableNumber(row.sakit)
  const izin = toNullableNumber(row.izin)

  if (Number.isNaN(nilaiKehadiran)) errors.push(`${nama}: Kehadiran harus angka.`)
  if (Number.isNaN(sakit)) errors.push(`${nama}: Sakit harus angka.`)
  if (Number.isNaN(izin)) errors.push(`${nama}: Izin harus angka.`)

  if (nilaiKehadiran !== null && (nilaiKehadiran < 0 || nilaiKehadiran > 100)) {
    errors.push(`${nama}: Kehadiran harus 0-100.`)
  }
  if (sakit !== null && sakit < 0) errors.push(`${nama}: Sakit tidak boleh minus.`)
  if (izin !== null && izin < 0) errors.push(`${nama}: Izin tidak boleh minus.`)

  const gradeKeys = ['ibadah', 'kedisiplinan', 'kebersihan', 'adab']
  gradeKeys.forEach(key => {
    const gradeRaw = String(row[key] || '').trim().toUpperCase()
    const grade = gradeRaw ? normalizeAkhlakGrade(gradeRaw) : ''
    if (gradeRaw && !grade) errors.push(`${nama}: Nilai ${key} harus A-E.`)
  })

  return errors
}

async function openMusyrifBulkInputModal() {
  const periode = String(musyrifState.periode || getMonthInputToday())
  const periodeRange = getPeriodeRange(periode)
  if (!periodeRange) {
    alert('Periode tidak valid.')
    return
  }

  const santriList = musyrifState.santriList || []
  if (!santriList.length) {
    alert('Belum ada santri binaan untuk diinput.')
    return
  }

  const overlay = ensureMusyrifBulkModal()
  const body = document.getElementById('musyrif-bulk-body')
  if (!overlay || !body) return
  overlay.style.display = 'flex'
  body.innerHTML = 'Loading data input massal...'

  const kelasMap = musyrifState.kelasMap || new Map()
  const santriIds = santriList.map(item => String(item.id || '')).filter(Boolean)
  const kelasIds = [...new Set(santriList.map(item => String(item.kelas_id || '')).filter(Boolean))]

  const [absensiRes, reportRes] = await Promise.all([
    sb
      .from('absensi_santri')
      .select('santri_id, kelas_id, status, tanggal')
      .in('santri_id', santriIds)
      .in('kelas_id', kelasIds)
      .gte('tanggal', periodeRange.start)
      .lte('tanggal', periodeRange.end),
    sb
      .from(MONTHLY_REPORT_TABLE)
      .select('santri_id, kelas_id, guru_id, musyrif, no_hp_musyrif, nilai_kehadiran_liqa_muhasabah, sakit_liqa_muhasabah, izin_liqa_muhasabah, nilai_ibadah, nilai_kedisiplinan, nilai_kebersihan, nilai_adab, prestasi_kesantrian, pelanggaran_kesantrian, catatan_musyrif')
      .eq('periode', periode)
      .in('santri_id', santriIds)
  ])

  if (absensiRes.error) {
    console.error(absensiRes.error)
    body.innerHTML = `<div class="placeholder-card">Gagal load absensi: ${escapeHtml(absensiRes.error.message || 'Unknown error')}</div>`
    return
  }
  if (reportRes.error) {
    console.error(reportRes.error)
    body.innerHTML = `<div class="placeholder-card">Gagal load laporan bulanan: ${escapeHtml(reportRes.error.message || 'Unknown error')}</div>`
    return
  }

  const absensiBySantri = new Map()
  ;(absensiRes.data || []).forEach(row => {
    const sid = String(row?.santri_id || '')
    if (!sid) return
    if (!absensiBySantri.has(sid)) absensiBySantri.set(sid, [])
    absensiBySantri.get(sid).push(row)
  })

  const reportByKey = new Map()
  ;(reportRes.data || []).forEach(row => {
    const key = `${String(row?.santri_id || '')}|${String(row?.kelas_id || '')}|${String(row?.guru_id || '')}`
    reportByKey.set(key, row)
  })

  const currentMusyrifNama = String(musyrifState?.profile?.nama || '').trim()
  const currentMusyrifNoHp = String(musyrifState?.profile?.no_hp || '').trim()
  const gradeOptions = grade => `
    <option value="" ${!grade ? 'selected' : ''}>-</option>
    <option value="A" ${grade === 'A' ? 'selected' : ''}>A</option>
    <option value="B" ${grade === 'B' ? 'selected' : ''}>B</option>
    <option value="C" ${grade === 'C' ? 'selected' : ''}>C</option>
    <option value="D" ${grade === 'D' ? 'selected' : ''}>D</option>
    <option value="E" ${grade === 'E' ? 'selected' : ''}>E</option>
  `

  const rowsHtml = santriList.map((santri, idx) => {
    const sid = String(santri.id || '')
    const kelasId = String(santri.kelas_id || '')
    const kelas = kelasMap.get(kelasId)
    const waliGuruId = String(kelas?.wali_kelas_id || '')
    const key = `${sid}|${kelasId}|${waliGuruId}`
    const report = reportByKey.get(key) || null
    const absensiRows = absensiBySantri.get(sid) || []
    const totalHari = absensiRows.length
    const hadir = absensiRows.filter(item => {
      const status = String(item?.status || '').trim().toLowerCase()
      return status === 'hadir' || status === 'terlambat'
    }).length
    const kehadiranDefault = totalHari > 0 ? Math.round(((hadir / totalHari) * 100) * 100) / 100 : null
    const sakitDefault = absensiRows.filter(item => String(item?.status || '').trim().toLowerCase() === 'sakit').length
    const izinDefault = absensiRows.filter(item => String(item?.status || '').trim().toLowerCase() === 'izin').length

    const nilaiIbadah = normalizeAkhlakGrade(report?.nilai_ibadah)
    const nilaiKedisiplinan = normalizeAkhlakGrade(report?.nilai_kedisiplinan)
    const nilaiKebersihan = normalizeAkhlakGrade(report?.nilai_kebersihan)
    const nilaiAdab = normalizeAkhlakGrade(report?.nilai_adab)

    return `
      <tr data-bulk-row="1" data-santri-id="${escapeHtml(sid)}" data-kelas-id="${escapeHtml(kelasId)}" data-guru-id="${escapeHtml(waliGuruId)}">
        <td style="padding:6px; border:1px solid #e2e8f0; text-align:center;">${idx + 1}</td>
        <td style="padding:6px; border:1px solid #e2e8f0; min-width:220px;">${escapeHtml(santri.nama || '-')}</td>
        <td style="padding:6px; border:1px solid #e2e8f0; min-width:110px;">${escapeHtml(kelas?.nama_kelas || '-')}</td>
        <td style="padding:6px; border:1px solid #e2e8f0; min-width:110px;">
          <input data-bulk-field="kehadiran" class="guru-field" type="number" min="0" max="100" step="0.01" value="${escapeHtml(toInputValue(report?.nilai_kehadiran_liqa_muhasabah ?? kehadiranDefault))}">
        </td>
        <td style="padding:6px; border:1px solid #e2e8f0; min-width:90px;">
          <input data-bulk-field="sakit" class="guru-field" type="number" min="0" step="1" value="${escapeHtml(toInputValue(report?.sakit_liqa_muhasabah ?? sakitDefault))}">
        </td>
        <td style="padding:6px; border:1px solid #e2e8f0; min-width:90px;">
          <input data-bulk-field="izin" class="guru-field" type="number" min="0" step="1" value="${escapeHtml(toInputValue(report?.izin_liqa_muhasabah ?? izinDefault))}">
        </td>
        <td style="padding:6px; border:1px solid #e2e8f0; min-width:90px;">
          <select data-bulk-field="ibadah" class="guru-field">${gradeOptions(nilaiIbadah)}</select>
        </td>
        <td style="padding:6px; border:1px solid #e2e8f0; min-width:110px;">
          <select data-bulk-field="kedisiplinan" class="guru-field">${gradeOptions(nilaiKedisiplinan)}</select>
        </td>
        <td style="padding:6px; border:1px solid #e2e8f0; min-width:100px;">
          <select data-bulk-field="kebersihan" class="guru-field">${gradeOptions(nilaiKebersihan)}</select>
        </td>
        <td style="padding:6px; border:1px solid #e2e8f0; min-width:90px;">
          <select data-bulk-field="adab" class="guru-field">${gradeOptions(nilaiAdab)}</select>
        </td>
        <td style="padding:6px; border:1px solid #e2e8f0; min-width:170px;">
          <input data-bulk-field="prestasi" class="guru-field" type="text" value="${escapeHtml(String(report?.prestasi_kesantrian || '').trim())}">
        </td>
        <td style="padding:6px; border:1px solid #e2e8f0; min-width:210px;">
          <input data-bulk-field="pelanggaran" class="guru-field" type="text" value="${escapeHtml(String(report?.pelanggaran_kesantrian || '').trim())}">
        </td>
        <td style="padding:6px; border:1px solid #e2e8f0; min-width:260px;">
          <input data-bulk-field="catatan" class="guru-field" type="text" value="${escapeHtml(String(report?.catatan_musyrif || '').trim())}">
        </td>
      </tr>
    `
  }).join('')

  body.innerHTML = `
    <div style="display:grid; gap:8px;">
      <div style="display:flex; align-items:center; justify-content:space-between; gap:8px; flex-wrap:wrap;">
        <div style="font-size:13px; color:#334155;">
          Periode: <strong>${escapeHtml(getPeriodeLabel(periode))}</strong> | Musyrif: <strong>${escapeHtml(currentMusyrifNama || '-')}</strong> | No HP: <strong>${escapeHtml(currentMusyrifNoHp || '-')}</strong>
        </div>
        <button type="button" class="modal-btn modal-btn-primary" onclick="saveMusyrifBulkInput()">Simpan Semua</button>
      </div>
      <div style="overflow:auto; border:1px solid #e2e8f0; border-radius:10px;">
        <table style="width:100%; min-width:1850px; border-collapse:collapse; font-size:12px;">
          <thead>
            <tr style="background:#f8fafc;">
              <th style="padding:6px; border:1px solid #e2e8f0; width:44px;">No</th>
              <th style="padding:6px; border:1px solid #e2e8f0; text-align:left;">Nama Santri</th>
              <th style="padding:6px; border:1px solid #e2e8f0; text-align:left;">Kelas</th>
              <th style="padding:6px; border:1px solid #e2e8f0;">Kehadiran Liqa (%)</th>
              <th style="padding:6px; border:1px solid #e2e8f0;">Sakit</th>
              <th style="padding:6px; border:1px solid #e2e8f0;">Izin</th>
              <th style="padding:6px; border:1px solid #e2e8f0;">Ibadah</th>
              <th style="padding:6px; border:1px solid #e2e8f0;">Kedisiplinan</th>
              <th style="padding:6px; border:1px solid #e2e8f0;">Kebersihan</th>
              <th style="padding:6px; border:1px solid #e2e8f0;">Adab</th>
              <th style="padding:6px; border:1px solid #e2e8f0;">Prestasi</th>
              <th style="padding:6px; border:1px solid #e2e8f0;">Pelanggaran (auto)</th>
              <th style="padding:6px; border:1px solid #e2e8f0;">Catatan Musyrif</th>
            </tr>
          </thead>
          <tbody>${rowsHtml}</tbody>
        </table>
      </div>
    </div>
  `
}

async function saveMusyrifBulkInput() {
  const rows = [...document.querySelectorAll('tr[data-bulk-row="1"]')]
  if (!rows.length) {
    alert('Tidak ada data untuk disimpan.')
    return
  }

  const periode = String(musyrifState.periode || getMonthInputToday())
  const currentMusyrifNama = String(musyrifState?.profile?.nama || '').trim()
  const currentMusyrifNoHp = String(musyrifState?.profile?.no_hp || '').trim()

  const validationErrors = []
  const payload = []

  rows.forEach(row => {
    const sid = String(row.getAttribute('data-santri-id') || '').trim()
    const kelasId = String(row.getAttribute('data-kelas-id') || '').trim()
    const guruId = String(row.getAttribute('data-guru-id') || '').trim()
    const nama = String(row.children?.[1]?.textContent || '').trim() || sid

    const nilaiKehadiran = String(row.querySelector('[data-bulk-field="kehadiran"]')?.value || '').trim()
    const sakit = String(row.querySelector('[data-bulk-field="sakit"]')?.value || '').trim()
    const izin = String(row.querySelector('[data-bulk-field="izin"]')?.value || '').trim()
    const ibadah = String(row.querySelector('[data-bulk-field="ibadah"]')?.value || '').trim().toUpperCase()
    const kedisiplinan = String(row.querySelector('[data-bulk-field="kedisiplinan"]')?.value || '').trim().toUpperCase()
    const kebersihan = String(row.querySelector('[data-bulk-field="kebersihan"]')?.value || '').trim().toUpperCase()
    const adab = String(row.querySelector('[data-bulk-field="adab"]')?.value || '').trim().toUpperCase()
    const prestasi = String(row.querySelector('[data-bulk-field="prestasi"]')?.value || '').trim()
    const pelanggaran = String(row.querySelector('[data-bulk-field="pelanggaran"]')?.value || '').trim()
    const catatan = String(row.querySelector('[data-bulk-field="catatan"]')?.value || '').trim()

    const rowData = { nama, nilaiKehadiran, sakit, izin, ibadah, kedisiplinan, kebersihan, adab }
    validationErrors.push(...validateMusyrifBulkRow(rowData))

    if (!sid || !kelasId || !guruId) {
      validationErrors.push(`${nama}: wali kelas/guru belum terdeteksi.`)
      return
    }

    const gradeIbadah = normalizeAkhlakGrade(ibadah)
    const gradeKedisiplinan = normalizeAkhlakGrade(kedisiplinan)
    const gradeKebersihan = normalizeAkhlakGrade(kebersihan)
    const gradeAdab = normalizeAkhlakGrade(adab)

    payload.push({
      periode,
      guru_id: guruId,
      kelas_id: kelasId,
      santri_id: sid,
      musyrif: currentMusyrifNama || null,
      no_hp_musyrif: currentMusyrifNoHp || null,
      nilai_kehadiran_liqa_muhasabah: toNullableNumber(nilaiKehadiran),
      sakit_liqa_muhasabah: toNullableNumber(sakit) === null ? null : Math.round(Number(sakit)),
      izin_liqa_muhasabah: toNullableNumber(izin) === null ? null : Math.round(Number(izin)),
      nilai_ibadah: gradeIbadah || null,
      keterangan_ibadah: gradeIbadah ? getAkhlakKeteranganByGrade(gradeIbadah) : null,
      nilai_kedisiplinan: gradeKedisiplinan || null,
      keterangan_kedisiplinan: gradeKedisiplinan ? getAkhlakKeteranganByGrade(gradeKedisiplinan) : null,
      nilai_kebersihan: gradeKebersihan || null,
      keterangan_kebersihan: gradeKebersihan ? getAkhlakKeteranganByGrade(gradeKebersihan) : null,
      nilai_adab: gradeAdab || null,
      keterangan_adab: gradeAdab ? getAkhlakKeteranganByGrade(gradeAdab) : null,
      prestasi_kesantrian: prestasi || null,
      pelanggaran_kesantrian: pelanggaran || null,
      catatan_musyrif: catatan || null
    })
  })

  if (validationErrors.length) {
    alert(`Ada data yang belum valid:\n- ${validationErrors.slice(0, 10).join('\n- ')}${validationErrors.length > 10 ? '\n- ...' : ''}`)
    return
  }

  if (!payload.length) {
    alert('Tidak ada baris valid untuk disimpan.')
    return
  }

  const { error } = await sb
    .from(MONTHLY_REPORT_TABLE)
    .upsert(payload, { onConflict: 'periode,guru_id,kelas_id,santri_id' })

  if (error) {
    console.error(error)
    alert(`Gagal simpan input massal: ${error.message || 'Unknown error'}`)
    return
  }

  alert(`Input massal berhasil disimpan (${payload.length} santri).`)
  closeMusyrifBulkInputModal()
  await renderLaporanBulananPage()
}

async function renderLaporanBulananPage() {
  const content = document.getElementById('musyrif-content')
  if (!content) return
  content.innerHTML = 'Loading laporan bulanan...'

  const periode = musyrifState.periode || getMonthInputToday()
  musyrifState.periode = periode

  const musyrifId = String(musyrifState?.profile?.id || '').trim()
  if (!musyrifId) {
    content.innerHTML = '<div class="placeholder-card">Profil musyrif tidak ditemukan. Silakan login ulang.</div>'
    return
  }

  const halaqahRes = await sb.from(HALAQAH_TABLE).select('id').eq('musyrif_id', musyrifId)
  if (halaqahRes.error) {
    console.error(halaqahRes.error)
    content.innerHTML = `<div class="placeholder-card">Gagal load kamar musyrif: ${escapeHtml(halaqahRes.error.message || 'Unknown error')}</div>`
    return
  }

  const halaqahIds = [...new Set((halaqahRes.data || []).map(item => String(item.id || '')).filter(Boolean))]
  let emptyMessage = 'Belum ada data santri aktif.'
  let santriIds = []

  if (!halaqahIds.length) {
    emptyMessage = 'Belum ada kamar yang ditugaskan ke Anda.'
  } else {
    const halaqahSantriRes = await sb
      .from(HALAQAH_SANTRI_TABLE)
      .select('santri_id')
      .in('kamar_id', halaqahIds)
    if (halaqahSantriRes.error) {
      console.error(halaqahSantriRes.error)
      content.innerHTML = `<div class="placeholder-card">Gagal load anggota kamar: ${escapeHtml(halaqahSantriRes.error.message || 'Unknown error')}</div>`
      return
    }
    santriIds = [...new Set((halaqahSantriRes.data || []).map(item => String(item.santri_id || '')).filter(Boolean))]
    if (!santriIds.length) {
      emptyMessage = 'Belum ada santri yang terdaftar pada kamar Anda.'
    }
  }

  const kelasRes = await sb.from('kelas').select('id, nama_kelas, wali_kelas_id').order('nama_kelas')
  if (kelasRes.error) {
    console.error(kelasRes.error)
    content.innerHTML = `<div class="placeholder-card">Gagal load data kelas: ${escapeHtml(kelasRes.error.message || 'Unknown error')}</div>`
    return
  }
  musyrifState.kelasMap = new Map((kelasRes.data || []).map(k => [String(k.id), k]))

  if (!santriIds.length) {
    musyrifState.santriList = []
  } else {
    const santriRes = await sb
      .from('santri')
      .select('id, nama, kelas_id, aktif')
      .in('id', santriIds)
      .eq('aktif', true)
      .order('nama')
    if (santriRes.error) {
      console.error(santriRes.error)
      content.innerHTML = `<div class="placeholder-card">Gagal load data santri kamar: ${escapeHtml(santriRes.error.message || 'Unknown error')}</div>`
      return
    }
    musyrifState.santriList = santriRes.data || []
    if (!musyrifState.santriList.length) {
      emptyMessage = 'Belum ada santri aktif pada kamar Anda.'
    }
  }

  const rowsHtml = musyrifState.santriList.map((item, idx) => {
    const kelas = musyrifState.kelasMap.get(String(item.kelas_id || ''))
    return `
      <tr>
        <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${idx + 1}</td>
        <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.nama || '-')}</td>
        <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(kelas?.nama_kelas || '-')}</td>
        <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">
          <button type="button" class="modal-btn modal-btn-primary" onclick="openMusyrifLaporanDetail('${escapeHtml(String(item.id))}')">Input Kesantrian</button>
        </td>
      </tr>
    `
  }).join('')

  content.innerHTML = `
    <div style="display:flex; align-items:end; justify-content:space-between; gap:10px; margin-bottom:12px; flex-wrap:wrap;">
      <div>
        <label class="guru-label">Periode</label>
        <input id="musyrif-periode" class="guru-field" type="month" value="${escapeHtml(periode)}" onchange="onMusyrifPeriodeChange(this.value)">
      </div>
      <div style="display:flex; align-items:center; gap:10px; flex-wrap:wrap;">
        <button type="button" class="modal-btn modal-btn-primary" onclick="openMusyrifBulkInputModal()">Input Massal</button>
      </div>
    </div>
    <div style="overflow:auto; border:1px solid #e2e8f0; border-radius:10px;">
      <table style="width:100%; min-width:720px; border-collapse:collapse; font-size:13px;">
        <thead>
          <tr style="background:#f8fafc;">
            <th style="padding:8px; border:1px solid #e2e8f0; width:44px;">No</th>
            <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Nama Santri</th>
            <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Kelas</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:220px;">Aksi</th>
          </tr>
        </thead>
        <tbody>
          ${rowsHtml || `<tr><td colspan="4" style="padding:12px; text-align:center; border:1px solid #e2e8f0;">${escapeHtml(emptyMessage)}</td></tr>`}
        </tbody>
      </table>
    </div>
  `
}

function onMusyrifPeriodeChange(value) {
  const periode = String(value || '').trim()
  musyrifState.periode = /^\d{4}-\d{2}$/.test(periode) ? periode : getMonthInputToday()
  renderLaporanBulananPage()
}

async function openMusyrifLaporanDetail(santriId) {
  const sid = String(santriId || '').trim()
  const santri = (musyrifState.santriList || []).find(item => String(item.id) === sid)
  const content = document.getElementById('musyrif-content')
  if (!content || !santri) return

  const kelas = musyrifState.kelasMap.get(String(santri.kelas_id || ''))
  const waliGuruId = String(kelas?.wali_kelas_id || '').trim()
  if (!waliGuruId) {
    alert('Wali kelas belum diatur. Data tidak bisa disimpan ke laporan bulanan.')
    return
  }
  const periode = musyrifState.periode || getMonthInputToday()
  const periodeRange = getPeriodeRange(periode)
  if (!periodeRange) {
    alert('Periode tidak valid.')
    return
  }

  const { data: absensiRows, error: absensiError } = await sb
    .from('absensi_santri')
    .select('status, tanggal')
    .eq('santri_id', sid)
    .eq('kelas_id', String(santri.kelas_id))
    .gte('tanggal', periodeRange.start)
    .lte('tanggal', periodeRange.end)

  if (absensiError) {
    console.error(absensiError)
    alert(`Gagal load absensi santri: ${absensiError.message || 'Unknown error'}`)
    return
  }

  const absensiList = absensiRows || []
  const totalHari = absensiList.length
  const hadirCount = absensiList.filter(item => {
    const status = String(item?.status || '').trim().toLowerCase()
    return status === 'hadir' || status === 'terlambat'
  }).length
  const sakitLiqaDefault = absensiList.filter(item => String(item?.status || '').trim().toLowerCase() === 'sakit').length
  const izinLiqaDefault = absensiList.filter(item => String(item?.status || '').trim().toLowerCase() === 'izin').length
  const nilaiKehadiranDefault = totalHari > 0 ? Math.round(((hadirCount / totalHari) * 100) * 100) / 100 : null

  const { data: reportRow, error } = await sb
    .from(MONTHLY_REPORT_TABLE)
    .select('id, musyrif, no_hp_musyrif, nilai_kehadiran_liqa_muhasabah, sakit_liqa_muhasabah, izin_liqa_muhasabah, nilai_ibadah, keterangan_ibadah, nilai_kedisiplinan, keterangan_kedisiplinan, nilai_kebersihan, keterangan_kebersihan, nilai_adab, keterangan_adab, prestasi_kesantrian, pelanggaran_kesantrian, catatan_musyrif')
    .eq('periode', periode)
    .eq('guru_id', waliGuruId)
    .eq('kelas_id', String(santri.kelas_id))
    .eq('santri_id', sid)
    .maybeSingle()

  if (error) {
    console.error(error)
    alert(`Gagal load detail laporan: ${error.message || 'Unknown error'}`)
    return
  }

  musyrifState.selectedSantriId = sid
  musyrifState.detail = {
    santriId: sid,
    kelasId: String(santri.kelas_id),
    waliGuruId
  }

  const nilaiIbadah = normalizeAkhlakGrade(reportRow?.nilai_ibadah)
  const nilaiKedisiplinan = normalizeAkhlakGrade(reportRow?.nilai_kedisiplinan)
  const nilaiKebersihan = normalizeAkhlakGrade(reportRow?.nilai_kebersihan)
  const nilaiAdab = normalizeAkhlakGrade(reportRow?.nilai_adab)
  const ketIbadah = String(reportRow?.keterangan_ibadah || '').trim() || (nilaiIbadah ? getAkhlakKeteranganByGrade(nilaiIbadah) : '')
  const ketKedisiplinan = String(reportRow?.keterangan_kedisiplinan || '').trim() || (nilaiKedisiplinan ? getAkhlakKeteranganByGrade(nilaiKedisiplinan) : '')
  const ketKebersihan = String(reportRow?.keterangan_kebersihan || '').trim() || (nilaiKebersihan ? getAkhlakKeteranganByGrade(nilaiKebersihan) : '')
  const ketAdab = String(reportRow?.keterangan_adab || '').trim() || (nilaiAdab ? getAkhlakKeteranganByGrade(nilaiAdab) : '')
  const currentMusyrifNama = String(musyrifState?.profile?.nama || '').trim()
  const currentMusyrifNoHp = String(musyrifState?.profile?.no_hp || '').trim()
  const musyrifValue = currentMusyrifNama || String(reportRow?.musyrif || '').trim()
  const musyrifNoHpValue = currentMusyrifNoHp || String(reportRow?.no_hp_musyrif || '').trim()
  const nilaiKehadiranLiqa = toNullableNumber(reportRow?.nilai_kehadiran_liqa_muhasabah)
  const sakitLiqa = toNullableNumber(reportRow?.sakit_liqa_muhasabah)
  const izinLiqa = toNullableNumber(reportRow?.izin_liqa_muhasabah)
  const prestasi = String(reportRow?.prestasi_kesantrian || '').trim()
  const pelanggaran = String(reportRow?.pelanggaran_kesantrian || '').trim()

  content.innerHTML = `
    <div style="display:flex; align-items:center; gap:10px; margin-bottom:12px;">
      <button type="button" class="mapel-back-btn" onclick="renderLaporanBulananPage()">&lt;</button>
      <div style="font-weight:700; color:#0f172a;">Input Kesantrian - ${escapeHtml(santri.nama || '-')}</div>
    </div>

    <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap:10px; margin-bottom:12px;">
      <div class="placeholder-card"><strong>Periode</strong><br>${escapeHtml(getPeriodeLabel(periode))}</div>
      <div class="placeholder-card"><strong>Nama</strong><br>${escapeHtml(santri.nama || '-')}</div>
      <div class="placeholder-card"><strong>Kelas</strong><br>${escapeHtml(kelas?.nama_kelas || '-')}</div>
    </div>

    <div class="placeholder-card" style="margin-bottom:12px;">
      <div style="font-weight:700; margin-bottom:8px;">C. Laporan Kesantrian</div>
      <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap:10px; margin-bottom:10px;">
        <div>
          <label class="guru-label">Musyrif</label>
          <input id="m-lap-musyrif" class="guru-field" type="text" value="${escapeHtml(musyrifValue)}" placeholder="Nama musyrif" autocomplete="off">
        </div>
        <div>
          <label class="guru-label">Nomor HP Musyrif</label>
          <input id="m-lap-nohp-musyrif" class="guru-field" type="text" value="${escapeHtml(musyrifNoHpValue)}" placeholder="08xxxxxxxxxx" autocomplete="off">
        </div>
      </div>

      <div style="overflow:auto; border:1px solid #e2e8f0; border-radius:10px;">
        <table style="width:100%; min-width:860px; border-collapse:collapse; font-size:13px;">
          <thead>
            <tr style="background:#f8fafc;">
              <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Aspek Penilaian</th>
              <th style="padding:8px; border:1px solid #e2e8f0; width:240px;">Nilai</th>
              <th style="padding:8px; border:1px solid #e2e8f0;">Keterangan</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td style="padding:8px; border:1px solid #e2e8f0;">Kehadiran di Liqa' Muhasabah</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <div style="display:flex; gap:6px; align-items:center;">
                  <input id="m-lap-nilai-kehadiran-liqa" class="guru-field" type="number" min="0" max="100" step="0.01" value="${escapeHtml(toInputValue(nilaiKehadiranLiqa === null ? nilaiKehadiranDefault : nilaiKehadiranLiqa))}">
                  <span style="font-size:12px; color:#64748b;">%</span>
                </div>
              </td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <div style="display:flex; gap:6px; align-items:center; flex-wrap:wrap;">
                  <label style="font-size:12px; color:#475569;">Sakit</label>
                  <input id="m-lap-sakit-liqa" class="guru-field" type="number" min="0" step="1" value="${escapeHtml(toInputValue(sakitLiqa === null ? sakitLiqaDefault : sakitLiqa))}" style="max-width:90px;">
                  <label style="font-size:12px; color:#475569;">Izin</label>
                  <input id="m-lap-izin-liqa" class="guru-field" type="number" min="0" step="1" value="${escapeHtml(toInputValue(izinLiqa === null ? izinLiqaDefault : izinLiqa))}" style="max-width:90px;">
                  <span style="font-size:12px; color:#64748b;">kali</span>
                </div>
              </td>
            </tr>
            <tr>
              <td style="padding:8px; border:1px solid #e2e8f0;">Ibadah</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <select id="m-lap-nilai-ibadah" class="guru-field" onchange="onMusyrifKesantrianGradeChange('ibadah', this.value)">
                  <option value="" ${!nilaiIbadah ? 'selected' : ''}>Pilih Nilai</option>
                  <option value="A" ${nilaiIbadah === 'A' ? 'selected' : ''}>A</option>
                  <option value="B" ${nilaiIbadah === 'B' ? 'selected' : ''}>B</option>
                  <option value="C" ${nilaiIbadah === 'C' ? 'selected' : ''}>C</option>
                  <option value="D" ${nilaiIbadah === 'D' ? 'selected' : ''}>D</option>
                  <option value="E" ${nilaiIbadah === 'E' ? 'selected' : ''}>E</option>
                </select>
              </td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <input id="m-lap-ket-ibadah" class="guru-field" type="text" value="${escapeHtml(ketIbadah)}" readonly style="background:#f8fafc; color:#475569;">
              </td>
            </tr>
            <tr>
              <td style="padding:8px; border:1px solid #e2e8f0;">Kedisiplinan</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <select id="m-lap-nilai-kedisiplinan" class="guru-field" onchange="onMusyrifKesantrianGradeChange('kedisiplinan', this.value)">
                  <option value="" ${!nilaiKedisiplinan ? 'selected' : ''}>Pilih Nilai</option>
                  <option value="A" ${nilaiKedisiplinan === 'A' ? 'selected' : ''}>A</option>
                  <option value="B" ${nilaiKedisiplinan === 'B' ? 'selected' : ''}>B</option>
                  <option value="C" ${nilaiKedisiplinan === 'C' ? 'selected' : ''}>C</option>
                  <option value="D" ${nilaiKedisiplinan === 'D' ? 'selected' : ''}>D</option>
                  <option value="E" ${nilaiKedisiplinan === 'E' ? 'selected' : ''}>E</option>
                </select>
              </td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <input id="m-lap-ket-kedisiplinan" class="guru-field" type="text" value="${escapeHtml(ketKedisiplinan)}" readonly style="background:#f8fafc; color:#475569;">
              </td>
            </tr>
            <tr>
              <td style="padding:8px; border:1px solid #e2e8f0;">Kebersihan</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <select id="m-lap-nilai-kebersihan" class="guru-field" onchange="onMusyrifKesantrianGradeChange('kebersihan', this.value)">
                  <option value="" ${!nilaiKebersihan ? 'selected' : ''}>Pilih Nilai</option>
                  <option value="A" ${nilaiKebersihan === 'A' ? 'selected' : ''}>A</option>
                  <option value="B" ${nilaiKebersihan === 'B' ? 'selected' : ''}>B</option>
                  <option value="C" ${nilaiKebersihan === 'C' ? 'selected' : ''}>C</option>
                  <option value="D" ${nilaiKebersihan === 'D' ? 'selected' : ''}>D</option>
                  <option value="E" ${nilaiKebersihan === 'E' ? 'selected' : ''}>E</option>
                </select>
              </td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <input id="m-lap-ket-kebersihan" class="guru-field" type="text" value="${escapeHtml(ketKebersihan)}" readonly style="background:#f8fafc; color:#475569;">
              </td>
            </tr>
            <tr>
              <td style="padding:8px; border:1px solid #e2e8f0;">Adab</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <select id="m-lap-nilai-adab" class="guru-field" onchange="onMusyrifKesantrianGradeChange('adab', this.value)">
                  <option value="" ${!nilaiAdab ? 'selected' : ''}>Pilih Nilai</option>
                  <option value="A" ${nilaiAdab === 'A' ? 'selected' : ''}>A</option>
                  <option value="B" ${nilaiAdab === 'B' ? 'selected' : ''}>B</option>
                  <option value="C" ${nilaiAdab === 'C' ? 'selected' : ''}>C</option>
                  <option value="D" ${nilaiAdab === 'D' ? 'selected' : ''}>D</option>
                  <option value="E" ${nilaiAdab === 'E' ? 'selected' : ''}>E</option>
                </select>
              </td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <input id="m-lap-ket-adab" class="guru-field" type="text" value="${escapeHtml(ketAdab)}" readonly style="background:#f8fafc; color:#475569;">
              </td>
            </tr>
            <tr>
              <td style="padding:8px; border:1px solid #e2e8f0;">Prestasi</td>
              <td colspan="2" style="padding:8px; border:1px solid #e2e8f0;">
                <input id="m-lap-prestasi-kesantrian" class="guru-field" type="text" value="${escapeHtml(prestasi)}" placeholder="Isi prestasi santri (manual)">
              </td>
            </tr>
            <tr>
              <td style="padding:8px; border:1px solid #e2e8f0;">Pelanggaran</td>
              <td colspan="2" style="padding:8px; border:1px solid #e2e8f0;">
                <input id="m-lap-pelanggaran-kesantrian" class="guru-field" type="text" value="${escapeHtml(pelanggaran)}" placeholder="Isi pelanggaran (manual)">
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div style="margin-top:10px;">
        <label class="guru-label">Catatan Musyrif</label>
        <textarea id="m-lap-catatan-musyrif" class="guru-field" rows="5" placeholder="Tulis catatan perkembangan kesantrian santri...">${escapeHtml(String(reportRow?.catatan_musyrif || '').trim())}</textarea>
      </div>
      <div style="margin-top:10px; display:flex; gap:8px; flex-wrap:wrap;">
        <button type="button" class="modal-btn modal-btn-primary" onclick="saveMusyrifLaporanDetail()">Simpan Kesantrian</button>
      </div>
    </div>
  `
}

function onMusyrifKesantrianGradeChange(aspek, value) {
  const key = String(aspek || '').trim().toLowerCase()
  const idMap = {
    ibadah: 'm-lap-ket-ibadah',
    kedisiplinan: 'm-lap-ket-kedisiplinan',
    kebersihan: 'm-lap-ket-kebersihan',
    adab: 'm-lap-ket-adab'
  }
  const targetId = idMap[key]
  if (!targetId) return
  const el = document.getElementById(targetId)
  if (!el) return
  const grade = normalizeAkhlakGrade(value)
  el.value = grade ? getAkhlakKeteranganByGrade(grade) : ''
}

async function saveMusyrifLaporanDetail() {
  const detail = musyrifState.detail
  if (!detail) return
  const currentMusyrifNama = String(musyrifState?.profile?.nama || '').trim()
  const currentMusyrifNoHp = String(musyrifState?.profile?.no_hp || '').trim()

  const nilaiKehadiranLiqa = toNullableNumber(document.getElementById('m-lap-nilai-kehadiran-liqa')?.value)
  const sakitLiqa = toNullableNumber(document.getElementById('m-lap-sakit-liqa')?.value)
  const izinLiqa = toNullableNumber(document.getElementById('m-lap-izin-liqa')?.value)
  const nilaiIbadahRaw = String(document.getElementById('m-lap-nilai-ibadah')?.value || '').trim().toUpperCase()
  const nilaiKedisiplinanRaw = String(document.getElementById('m-lap-nilai-kedisiplinan')?.value || '').trim().toUpperCase()
  const nilaiKebersihanRaw = String(document.getElementById('m-lap-nilai-kebersihan')?.value || '').trim().toUpperCase()
  const nilaiAdabRaw = String(document.getElementById('m-lap-nilai-adab')?.value || '').trim().toUpperCase()
  const nilaiIbadah = nilaiIbadahRaw ? normalizeAkhlakGrade(nilaiIbadahRaw) : ''
  const nilaiKedisiplinan = nilaiKedisiplinanRaw ? normalizeAkhlakGrade(nilaiKedisiplinanRaw) : ''
  const nilaiKebersihan = nilaiKebersihanRaw ? normalizeAkhlakGrade(nilaiKebersihanRaw) : ''
  const nilaiAdab = nilaiAdabRaw ? normalizeAkhlakGrade(nilaiAdabRaw) : ''

  const numericChecks = [
    [nilaiKehadiranLiqa, "Nilai kehadiran Liqa' Muhasabah harus angka."],
    [sakitLiqa, "Jumlah sakit Liqa' Muhasabah harus angka."],
    [izinLiqa, "Jumlah izin Liqa' Muhasabah harus angka."]
  ]
  for (const [num, message] of numericChecks) {
    if (Number.isNaN(num)) {
      alert(message)
      return
    }
  }
  if (nilaiKehadiranLiqa !== null && (nilaiKehadiranLiqa < 0 || nilaiKehadiranLiqa > 100)) {
    alert("Nilai kehadiran Liqa' Muhasabah harus 0-100.")
    return
  }
  if (sakitLiqa !== null && sakitLiqa < 0) {
    alert('Jumlah sakit tidak boleh minus.')
    return
  }
  if (izinLiqa !== null && izinLiqa < 0) {
    alert('Jumlah izin tidak boleh minus.')
    return
  }
  if ((nilaiIbadahRaw && !nilaiIbadah) || (nilaiKedisiplinanRaw && !nilaiKedisiplinan) || (nilaiKebersihanRaw && !nilaiKebersihan) || (nilaiAdabRaw && !nilaiAdab)) {
    alert('Nilai Ibadah/Kedisiplinan/Kebersihan/Adab harus A, B, C, D, atau E.')
    return
  }

  const pelanggaranManual = String(document.getElementById('m-lap-pelanggaran-kesantrian')?.value || '').trim()
  const payload = {
    periode: musyrifState.periode || getMonthInputToday(),
    guru_id: detail.waliGuruId,
    kelas_id: detail.kelasId,
    santri_id: detail.santriId,
    musyrif: String(document.getElementById('m-lap-musyrif')?.value || '').trim() || currentMusyrifNama || null,
    no_hp_musyrif: String(document.getElementById('m-lap-nohp-musyrif')?.value || '').trim() || currentMusyrifNoHp || null,
    nilai_kehadiran_liqa_muhasabah: nilaiKehadiranLiqa === null ? null : nilaiKehadiranLiqa,
    sakit_liqa_muhasabah: sakitLiqa === null ? null : Math.round(sakitLiqa),
    izin_liqa_muhasabah: izinLiqa === null ? null : Math.round(izinLiqa),
    nilai_ibadah: nilaiIbadah || null,
    keterangan_ibadah: String(document.getElementById('m-lap-ket-ibadah')?.value || '').trim() || null,
    nilai_kedisiplinan: nilaiKedisiplinan || null,
    keterangan_kedisiplinan: String(document.getElementById('m-lap-ket-kedisiplinan')?.value || '').trim() || null,
    nilai_kebersihan: nilaiKebersihan || null,
    keterangan_kebersihan: String(document.getElementById('m-lap-ket-kebersihan')?.value || '').trim() || null,
    nilai_adab: nilaiAdab || null,
    keterangan_adab: String(document.getElementById('m-lap-ket-adab')?.value || '').trim() || null,
    prestasi_kesantrian: String(document.getElementById('m-lap-prestasi-kesantrian')?.value || '').trim() || null,
    pelanggaran_kesantrian: pelanggaranManual || null,
    catatan_musyrif: String(document.getElementById('m-lap-catatan-musyrif')?.value || '').trim() || null
  }

  const { error } = await sb
    .from(MONTHLY_REPORT_TABLE)
    .upsert(payload, { onConflict: 'periode,guru_id,kelas_id,santri_id' })
  if (error) {
    console.error(error)
    alert(`Gagal simpan kesantrian: ${error.message || 'Unknown error'}`)
    return
  }
  alert('Data kesantrian berhasil disimpan.')
  await openMusyrifLaporanDetail(detail.santriId)
}

async function renderMusyrifProfil() {
  const content = document.getElementById('musyrif-content')
  if (!content) return

  content.innerHTML = 'Loading profil...'

  let profile
  try {
    profile = await getCurrentMusyrif()
  } catch (error) {
    console.error(error)
    content.innerHTML = `<div class="placeholder-card">Gagal load profil: ${escapeHtml(error.message || 'Unknown error')}</div>`
    return
  }

  if (!profile?.id) {
    content.innerHTML = '<div class="placeholder-card">Data profil musyrif tidak ditemukan.</div>'
    return
  }

  content.innerHTML = `
    <div style="max-width:580px;">
      <div style="margin-bottom:10px;">
        <label class="guru-label">ID Karyawan</label>
        <input id="musyrif-profil-id-karyawan" type="text" value="${escapeHtml(profile.id_karyawan || '')}" disabled class="guru-field" autocomplete="off" style="background:#f8fafc; color:#64748b;">
      </div>
      <div style="margin-bottom:10px;">
        <label class="guru-label">Nama</label>
        <input id="musyrif-profil-nama" type="text" value="${escapeHtml(profile.nama || '')}" class="guru-field" autocomplete="off">
      </div>
      <div style="margin-bottom:10px;">
        <label class="guru-label">No HP</label>
        <input id="musyrif-profil-no-hp" type="text" value="${escapeHtml(profile.no_hp || '')}" class="guru-field" autocomplete="off">
      </div>
      <div style="margin-bottom:10px;">
        <label class="guru-label">Alamat</label>
        <input id="musyrif-profil-alamat" type="text" value="${escapeHtml(profile.alamat || '')}" class="guru-field" autocomplete="off">
      </div>
      <div style="margin-bottom:12px;">
        <label class="guru-label">Password Baru (opsional)</label>
        <div style="display:flex; gap:8px; align-items:center;">
          <input id="musyrif-profil-password" type="password" placeholder="Isi jika ingin ganti password" class="guru-field" autocomplete="new-password" style="flex:1;">
          <button id="musyrif-profil-password-toggle" type="button" class="modal-btn" onclick="toggleMusyrifProfilePassword()">Lihat</button>
        </div>
      </div>
      <button type="button" class="modal-btn modal-btn-primary" onclick="saveMusyrifProfil('${escapeHtml(profile.id)}')">Simpan Profil</button>
    </div>
  `
}

function toggleMusyrifProfilePassword() {
  const input = document.getElementById('musyrif-profil-password')
  const btn = document.getElementById('musyrif-profil-password-toggle')
  if (!input || !btn) return
  const isHidden = input.type === 'password'
  input.type = isHidden ? 'text' : 'password'
  btn.textContent = isHidden ? 'Sembunyikan' : 'Lihat'
}

async function saveMusyrifProfil(musyrifId) {
  const nama = String(document.getElementById('musyrif-profil-nama')?.value || '').trim()
  const noHp = String(document.getElementById('musyrif-profil-no-hp')?.value || '').trim()
  const alamat = String(document.getElementById('musyrif-profil-alamat')?.value || '').trim()
  const password = String(document.getElementById('musyrif-profil-password')?.value || '').trim()

  if (!nama) {
    alert('Nama wajib diisi.')
    return
  }

  const payload = {
    nama,
    no_hp: noHp || null,
    alamat: alamat || null
  }
  if (password) payload.password = password

  const { error } = await sb
    .from('karyawan')
    .update(payload)
    .eq('id', musyrifId)
  if (error) {
    console.error(error)
    alert(`Gagal menyimpan profil: ${error.message || 'Unknown error'}`)
    return
  }

  alert('Profil berhasil disimpan.')
  await setMusyrifWelcomeName()
  await renderMusyrifProfil()
}

async function loadMusyrifPage(page) {
  const targetPage = PAGE_TITLES[page] ? page : 'dashboard'
  setTopbarTitle(targetPage)
  setNavActive(targetPage === 'profil' ? '' : targetPage)
  if (targetPage !== 'profil') localStorage.setItem(MUHAFFIZ_LAST_PAGE_KEY, targetPage)
  closeTopbarUserMenu()
  if (targetPage === 'profil') {
    await renderMusyrifProfil()
    return
  }
  if (targetPage === 'laporan-bulanan') {
    await renderLaporanBulananPage()
    return
  }
  await renderDashboard()
}

function logout() {
  localStorage.removeItem('login_id')
  localStorage.removeItem('login_name')
  localStorage.removeItem('login_role')
  localStorage.removeItem('login_roles')
  localStorage.removeItem(MUHAFFIZ_LAST_PAGE_KEY)
  location.href = 'index.html'
}

window.loadMusyrifPage = loadMusyrifPage
window.onMusyrifPeriodeChange = onMusyrifPeriodeChange
window.openMusyrifBulkInputModal = openMusyrifBulkInputModal
window.saveMusyrifBulkInput = saveMusyrifBulkInput
window.closeMusyrifBulkInputModal = closeMusyrifBulkInputModal
window.openMusyrifLaporanDetail = openMusyrifLaporanDetail
window.saveMusyrifLaporanDetail = saveMusyrifLaporanDetail
window.onMusyrifKesantrianGradeChange = onMusyrifKesantrianGradeChange
window.toggleTopbarUserMenu = toggleTopbarUserMenu
window.setMusyrifNotifRangeFilter = setMusyrifNotifRangeFilter
window.markAllMusyrifNotifRead = markAllMusyrifNotifRead
window.markMusyrifNotifItemRead = markMusyrifNotifItemRead
window.openTopbarCalendarPopup = openTopbarCalendarPopup
window.closeTopbarCalendarPopup = closeTopbarCalendarPopup
window.shiftTopbarCalendarMonth = shiftTopbarCalendarMonth
window.selectTopbarCalendarDate = selectTopbarCalendarDate
window.openMusyrifDashboardAgendaPopup = openMusyrifDashboardAgendaPopup
window.closeMusyrifDashboardAgendaPopup = closeMusyrifDashboardAgendaPopup
window.openMusyrifProfile = () => loadMusyrifPage('profil')
window.saveMusyrifProfil = saveMusyrifProfil
window.toggleMusyrifProfilePassword = toggleMusyrifProfilePassword
window.logout = logout

document.addEventListener('DOMContentLoaded', async () => {
  setupCustomPopupSystem()
  loadMusyrifNotifPrefs()
  ensureTopbarNotification()

  const loginId = String(localStorage.getItem('login_id') || '').trim()
  if (!loginId) {
    location.href = 'index.html'
    return
  }

  try {
    await setMusyrifWelcomeName()
    if (!musyrifState.profile) {
      location.href = 'index.html'
      return
    }
  } catch (error) {
    console.error(error)
    location.href = 'index.html'
    return
  }

  const lastPage = localStorage.getItem(MUHAFFIZ_LAST_PAGE_KEY) || 'dashboard'
  await loadMusyrifPage(lastPage)
  refreshMusyrifTopbarNotifications().catch(error => console.error(error))

  document.addEventListener('click', event => {
    const wrap = document.querySelector('.topbar-user-menu-wrap')
    if (!wrap) return
    if (!wrap.contains(event.target)) {
      closeTopbarUserMenu()
      closeTopbarNotifMenu()
    }
  })
})

