const supabaseUrl = 'https://optucpelkueqmlhwlbej.supabase.co'
const supabaseKey = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9wdHVjcGVsa3VlcW1saHdsYmVqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzAxOTY4MTgsImV4cCI6MjA4NTc3MjgxOH0.Vqaey9pcnltu9uRbPk0J-AGWaGDZjQLw92pcRv67GNE'
const sb = supabase.createClient(supabaseUrl, supabaseKey)

const MONTHLY_REPORT_TABLE = 'laporan_bulanan_wali'
const MUHAFFIZ_LAST_PAGE_KEY = 'muhaffiz_last_page'
const TOPBAR_KALENDER_TABLE = 'kalender_akademik'
const TOPBAR_KALENDER_DEFAULT_COLOR = '#2563eb'

const PAGE_TITLES = {
  dashboard: 'Dashboard',
  'laporan-bulanan': 'Laporan Bulanan'
}

let muhaffizState = {
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
let muhaffizDashboardAgendaRows = []

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

async function getCurrentMuhaffiz() {
  const loginId = String(localStorage.getItem('login_id') || '').trim()
  if (!loginId) return null
  const { data, error } = await sb
    .from('karyawan')
    .select('id, id_karyawan, nama, aktif')
    .eq('id_karyawan', loginId)
    .maybeSingle()
  if (error) throw error
  if (!data || !asBool(data.aktif)) return null
  return data
}

function setNavActive(page) {
  document.querySelectorAll('.guru-nav-btn').forEach(btn => {
    btn.classList.toggle('active', btn.getAttribute('data-page') === page)
  })
}

function setTopbarTitle(page) {
  const el = document.getElementById('muhaffiz-topbar-title')
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
  menu.classList.toggle('open')
}

function closeTopbarUserMenu() {
  const menu = document.getElementById('topbar-user-menu')
  if (!menu) return
  menu.classList.remove('open')
}

async function renderDashboard() {
  const content = document.getElementById('muhaffiz-content')
  if (!content) return
  content.innerHTML = '<div class="placeholder-card">Loading agenda kalender akademik...</div>'

  try {
    await loadTopbarCalendarData()
    const rows = (topbarKalenderState.list || []).slice()
      .sort((a, b) => String(a?.mulai || '').localeCompare(String(b?.mulai || '')))
    muhaffizDashboardAgendaRows = rows

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
      const rentang = getMuhaffizDashboardCalendarRangeLabel(item)
      const itemId = String(item?.id || '')
      return `
        <button type="button" onclick="openMuhaffizDashboardAgendaPopup('${escapeHtml(itemId)}')" style="text-align:left; width:100%; min-height:210px; position:relative; border:1px solid #e2e8f0; border-radius:16px; background:linear-gradient(180deg,#ffffff 0%,#f8fafc 100%); box-shadow:0 12px 24px rgba(15,23,42,0.08); padding:22px 20px 18px 22px; overflow:hidden; cursor:pointer;">
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

function formatMuhaffizDashboardCalendarDate(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleDateString('id-ID', { day: '2-digit', month: 'short', year: 'numeric' })
}

function getMuhaffizDashboardCalendarRangeLabel(item) {
  const startKey = getTopbarKalenderDateKey(item?.mulai)
  const endKey = getTopbarKalenderDateKey(item?.selesai || item?.mulai)
  if (!startKey) return '-'
  if (!endKey || endKey === startKey) return formatMuhaffizDashboardCalendarDate(startKey)
  return `${formatMuhaffizDashboardCalendarDate(startKey)} - ${formatMuhaffizDashboardCalendarDate(endKey)}`
}

function ensureMuhaffizDashboardAgendaPopup() {
  let popup = document.getElementById('muhaffiz-dashboard-agenda-popup')
  if (popup) return popup
  popup = document.createElement('div')
  popup.id = 'muhaffiz-dashboard-agenda-popup'
  popup.style.cssText = 'position:fixed; inset:0; background:rgba(15,23,42,0.35); display:none; align-items:center; justify-content:center; z-index:10001; padding:16px; box-sizing:border-box;'
  popup.innerHTML = `
    <div style="width:min(680px, calc(100vw - 32px)); max-height:calc(100vh - 32px); overflow:auto; border:1px solid #dbeafe; border-radius:0; background:#fff; box-shadow:0 18px 34px rgba(15,23,42,0.18); padding:14px 16px; position:relative;">
      <button type="button" onclick="closeMuhaffizDashboardAgendaPopup()" style="position:absolute; right:12px; top:10px; border:1px solid #cbd5e1; background:#fff; border-radius:999px; width:28px; height:28px; cursor:pointer;">×</button>
      <div id="muhaffiz-dashboard-agenda-popup-body"></div>
    </div>
  `
  popup.addEventListener('click', event => {
    if (event.target !== popup) return
    closeMuhaffizDashboardAgendaPopup()
  })
  document.body.appendChild(popup)
  return popup
}

function openMuhaffizDashboardAgendaPopup(id) {
  const sid = String(id || '')
  const selected = muhaffizDashboardAgendaRows.find(item => String(item?.id || '') === sid)
  if (!selected) return
  const popup = ensureMuhaffizDashboardAgendaPopup()
  const body = document.getElementById('muhaffiz-dashboard-agenda-popup-body')
  if (!popup || !body) return
  const warna = normalizeTopbarKalenderColor(selected?.warna)
  body.innerHTML = `
    <div style="display:flex; align-items:flex-start; justify-content:space-between; gap:10px; margin-bottom:8px; padding-right:30px;">
      <div style="font-size:20px; font-weight:700; color:#0f172a; line-height:1.35;">${escapeHtml(selected?.judul || '-')}</div>
      <span style="width:12px; height:12px; border-radius:999px; background:${escapeHtml(warna)}; margin-top:8px;"></span>
    </div>
    <div style="font-size:12px; font-weight:600; color:#334155; margin-bottom:8px;">${escapeHtml(getMuhaffizDashboardCalendarRangeLabel(selected))}</div>
    <div style="font-size:14px; color:#475569; line-height:1.7; white-space:pre-wrap;">${escapeHtml(selected?.detail || '-')}</div>
  `
  popup.style.display = 'flex'
}

function closeMuhaffizDashboardAgendaPopup() {
  const popup = document.getElementById('muhaffiz-dashboard-agenda-popup')
  if (!popup) return
  popup.style.display = 'none'
}

async function renderLaporanBulananPage() {
  const content = document.getElementById('muhaffiz-content')
  if (!content) return
  content.innerHTML = 'Loading laporan bulanan...'

  const periode = muhaffizState.periode || getMonthInputToday()
  muhaffizState.periode = periode

  const [kelasRes, santriRes] = await Promise.all([
    sb.from('kelas').select('id, nama_kelas, wali_kelas_id').order('nama_kelas'),
    sb.from('santri').select('id, nama, kelas_id, aktif').eq('aktif', true).order('nama')
  ])
  if (kelasRes.error || santriRes.error) {
    const error = kelasRes.error || santriRes.error
    console.error(error)
    content.innerHTML = `<div class="placeholder-card">Gagal load laporan bulanan: ${escapeHtml(error.message || 'Unknown error')}</div>`
    return
  }

  muhaffizState.kelasMap = new Map((kelasRes.data || []).map(k => [String(k.id), k]))
  muhaffizState.santriList = santriRes.data || []

  const rowsHtml = muhaffizState.santriList.map((item, idx) => {
    const kelas = muhaffizState.kelasMap.get(String(item.kelas_id || ''))
    return `
      <tr>
        <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${idx + 1}</td>
        <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.nama || '-')}</td>
        <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(kelas?.nama_kelas || '-')}</td>
        <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">
          <button type="button" class="modal-btn modal-btn-primary" onclick="openMuhaffizLaporanDetail('${escapeHtml(String(item.id))}')">Input Ketahfizan</button>
        </td>
      </tr>
    `
  }).join('')

  content.innerHTML = `
    <div style="display:flex; align-items:end; justify-content:space-between; gap:10px; margin-bottom:12px; flex-wrap:wrap;">
      <div>
        <label class="guru-label">Periode</label>
        <input id="muhaffiz-periode" class="guru-field" type="month" value="${escapeHtml(periode)}" onchange="onMuhaffizPeriodeChange(this.value)">
      </div>
      <div style="font-size:13px; color:#475569;">Input ketahfizan di sini akan otomatis tampil di detail laporan bulanan page guru.</div>
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
          ${rowsHtml || '<tr><td colspan="4" style="padding:12px; text-align:center; border:1px solid #e2e8f0;">Belum ada data santri aktif.</td></tr>'}
        </tbody>
      </table>
    </div>
  `
}

function onMuhaffizPeriodeChange(value) {
  const periode = String(value || '').trim()
  muhaffizState.periode = /^\d{4}-\d{2}$/.test(periode) ? periode : getMonthInputToday()
  renderLaporanBulananPage()
}

async function openMuhaffizLaporanDetail(santriId) {
  const sid = String(santriId || '').trim()
  const santri = (muhaffizState.santriList || []).find(item => String(item.id) === sid)
  const content = document.getElementById('muhaffiz-content')
  if (!content || !santri) return

  const kelas = muhaffizState.kelasMap.get(String(santri.kelas_id || ''))
  const waliGuruId = String(kelas?.wali_kelas_id || '').trim()
  if (!waliGuruId) {
    alert('Wali kelas belum diatur. Data tidak bisa disimpan ke laporan bulanan.')
    return
  }
  const periode = muhaffizState.periode || getMonthInputToday()

  const { data: reportRow, error } = await sb
    .from(MONTHLY_REPORT_TABLE)
    .select('id, muhaffiz, no_hp_muhaffiz, nilai_kehadiran_halaqah, sakit_halaqah, izin_halaqah, nilai_akhlak_halaqah, keterangan_akhlak_halaqah, nilai_ujian_bulanan, keterangan_ujian_bulanan, nilai_target_hafalan, keterangan_target_hafalan, nilai_capaian_hafalan_bulanan, nilai_jumlah_hafalan_halaman, nilai_jumlah_hafalan_juz, catatan_muhaffiz')
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

  muhaffizState.selectedSantriId = sid
  muhaffizState.detail = {
    santriId: sid,
    kelasId: String(santri.kelas_id),
    waliGuruId
  }

  const nilaiAkhlakHalaqah = normalizeAkhlakGrade(reportRow?.nilai_akhlak_halaqah)
  const ketAkhlakHalaqah = String(reportRow?.keterangan_akhlak_halaqah || '').trim() || (nilaiAkhlakHalaqah ? getAkhlakKeteranganByGrade(nilaiAkhlakHalaqah) : '')
  const nilaiUjianBulanan = toNullableNumber(reportRow?.nilai_ujian_bulanan)
  const ketUjianBulanan = String(reportRow?.keterangan_ujian_bulanan || '').trim() || getUjianBulananKeterangan(nilaiUjianBulanan)

  content.innerHTML = `
    <div style="display:flex; align-items:center; gap:10px; margin-bottom:12px;">
      <button type="button" class="mapel-back-btn" onclick="renderLaporanBulananPage()">&lt;</button>
      <div style="font-weight:700; color:#0f172a;">Input Ketahfizan - ${escapeHtml(santri.nama || '-')}</div>
    </div>

    <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap:10px; margin-bottom:12px;">
      <div class="placeholder-card"><strong>Periode</strong><br>${escapeHtml(getPeriodeLabel(periode))}</div>
      <div class="placeholder-card"><strong>Nama</strong><br>${escapeHtml(santri.nama || '-')}</div>
      <div class="placeholder-card"><strong>Kelas</strong><br>${escapeHtml(kelas?.nama_kelas || '-')}</div>
    </div>

    <div class="placeholder-card" style="margin-bottom:12px;">
      <div style="font-weight:700; margin-bottom:8px;">B. Laporan Ketahfizan</div>
      <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap:10px; margin-bottom:10px;">
        <div>
          <label class="guru-label">Muhaffiz</label>
          <input id="m-lap-muhaffiz" class="guru-field" type="text" value="${escapeHtml(String(reportRow?.muhaffiz || '').trim())}" placeholder="Nama muhaffiz">
        </div>
        <div>
          <label class="guru-label">Nomor HP Muhaffiz</label>
          <input id="m-lap-nohp-muhaffiz" class="guru-field" type="text" value="${escapeHtml(String(reportRow?.no_hp_muhaffiz || '').trim())}" placeholder="08xxxxxxxxxx">
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
              <td style="padding:8px; border:1px solid #e2e8f0;">Kehadiran di Halaqah</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <div style="display:flex; gap:6px; align-items:center;">
                  <input id="m-lap-nilai-kehadiran-halaqah" class="guru-field" type="number" min="0" max="100" step="0.01" value="${escapeHtml(toInputValue(reportRow?.nilai_kehadiran_halaqah))}">
                  <span style="font-size:12px; color:#64748b;">%</span>
                </div>
              </td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <div style="display:flex; gap:6px; align-items:center; flex-wrap:wrap;">
                  <label style="font-size:12px; color:#475569;">Sakit</label>
                  <input id="m-lap-sakit-halaqah" class="guru-field" type="number" min="0" step="1" value="${escapeHtml(toInputValue(reportRow?.sakit_halaqah))}" style="max-width:90px;">
                  <label style="font-size:12px; color:#475569;">Izin</label>
                  <input id="m-lap-izin-halaqah" class="guru-field" type="number" min="0" step="1" value="${escapeHtml(toInputValue(reportRow?.izin_halaqah))}" style="max-width:90px;">
                  <span style="font-size:12px; color:#64748b;">kali</span>
                </div>
              </td>
            </tr>
            <tr>
              <td style="padding:8px; border:1px solid #e2e8f0;">Akhlak di Halaqah</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <select id="m-lap-nilai-akhlak-halaqah" class="guru-field" onchange="onMuhaffizAkhlakChange(this.value)">
                  <option value="" ${!nilaiAkhlakHalaqah ? 'selected' : ''}>Pilih Nilai</option>
                  <option value="A" ${nilaiAkhlakHalaqah === 'A' ? 'selected' : ''}>A</option>
                  <option value="B" ${nilaiAkhlakHalaqah === 'B' ? 'selected' : ''}>B</option>
                  <option value="C" ${nilaiAkhlakHalaqah === 'C' ? 'selected' : ''}>C</option>
                  <option value="D" ${nilaiAkhlakHalaqah === 'D' ? 'selected' : ''}>D</option>
                  <option value="E" ${nilaiAkhlakHalaqah === 'E' ? 'selected' : ''}>E</option>
                </select>
              </td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <input id="m-lap-ket-akhlak-halaqah" class="guru-field" type="text" value="${escapeHtml(ketAkhlakHalaqah)}" readonly style="background:#f8fafc; color:#475569;">
              </td>
            </tr>
            <tr>
              <td style="padding:8px; border:1px solid #e2e8f0;">Ujian Bulanan</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <input id="m-lap-nilai-ujian-bulanan" class="guru-field" type="number" min="0" max="100" step="0.01" value="${escapeHtml(toInputValue(nilaiUjianBulanan))}" onchange="onMuhaffizUjianChange(this.value)">
              </td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <input id="m-lap-ket-ujian-bulanan" class="guru-field" type="text" value="${escapeHtml(ketUjianBulanan)}" readonly style="background:#f8fafc; color:#475569;">
              </td>
            </tr>
            <tr>
              <td style="padding:8px; border:1px solid #e2e8f0;">Target Hafalan</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <div style="display:flex; gap:6px; align-items:center;">
                  <input id="m-lap-nilai-target-hafalan" class="guru-field" type="number" min="0" max="100" step="0.01" value="${escapeHtml(toInputValue(reportRow?.nilai_target_hafalan))}">
                  <span style="font-size:12px; color:#64748b;">%</span>
                </div>
              </td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <input id="m-lap-ket-target-hafalan" class="guru-field" type="text" value="${escapeHtml(String(reportRow?.keterangan_target_hafalan || '').trim())}" placeholder="Isi manual keterangan target hafalan">
              </td>
            </tr>
            <tr>
              <td style="padding:8px; border:1px solid #e2e8f0;">Capaian Hafalan Bulanan</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <div style="display:flex; gap:6px; align-items:center;">
                  <input id="m-lap-nilai-capaian-hafalan" class="guru-field" type="number" min="0" step="0.01" value="${escapeHtml(toInputValue(reportRow?.nilai_capaian_hafalan_bulanan))}">
                  <span style="font-size:12px; color:#64748b;">halaman</span>
                </div>
              </td>
              <td style="padding:8px; border:1px solid #e2e8f0; color:#94a3b8;">-</td>
            </tr>
            <tr>
              <td style="padding:8px; border:1px solid #e2e8f0;">Jumlah Hafalan</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <div style="display:flex; gap:6px; align-items:center; flex-wrap:wrap;">
                  <input id="m-lap-jumlah-hafalan-halaman" class="guru-field" type="number" min="0" step="0.01" value="${escapeHtml(toInputValue(reportRow?.nilai_jumlah_hafalan_halaman))}" style="max-width:120px;">
                  <span style="font-size:12px; color:#64748b;">halaman</span>
                  <input id="m-lap-jumlah-hafalan-juz" class="guru-field" type="number" min="0" step="0.01" value="${escapeHtml(toInputValue(reportRow?.nilai_jumlah_hafalan_juz))}" style="max-width:120px;">
                  <span style="font-size:12px; color:#64748b;">juz</span>
                </div>
              </td>
              <td style="padding:8px; border:1px solid #e2e8f0; color:#94a3b8;">-</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div style="margin-top:10px;">
        <label class="guru-label">Catatan Muhaffiz</label>
        <textarea id="m-lap-catatan-muhaffiz" class="guru-field" rows="5" placeholder="Tulis catatan perkembangan tahfiz santri...">${escapeHtml(String(reportRow?.catatan_muhaffiz || '').trim())}</textarea>
      </div>
      <div style="margin-top:10px; display:flex; gap:8px; flex-wrap:wrap;">
        <button type="button" class="modal-btn modal-btn-primary" onclick="saveMuhaffizLaporanDetail()">Simpan Ketahfizan</button>
      </div>
    </div>
  `
}

function onMuhaffizAkhlakChange(value) {
  const el = document.getElementById('m-lap-ket-akhlak-halaqah')
  if (!el) return
  const grade = normalizeAkhlakGrade(value)
  el.value = grade ? getAkhlakKeteranganByGrade(grade) : ''
}

function onMuhaffizUjianChange(value) {
  const el = document.getElementById('m-lap-ket-ujian-bulanan')
  if (!el) return
  el.value = getUjianBulananKeterangan(value)
}

async function saveMuhaffizLaporanDetail() {
  const detail = muhaffizState.detail
  if (!detail) return

  const nilaiKehadiranHalaqah = toNullableNumber(document.getElementById('m-lap-nilai-kehadiran-halaqah')?.value)
  const sakitHalaqah = toNullableNumber(document.getElementById('m-lap-sakit-halaqah')?.value)
  const izinHalaqah = toNullableNumber(document.getElementById('m-lap-izin-halaqah')?.value)
  const nilaiAkhlakHalaqahRaw = String(document.getElementById('m-lap-nilai-akhlak-halaqah')?.value || '').trim().toUpperCase()
  const nilaiAkhlakHalaqah = nilaiAkhlakHalaqahRaw ? normalizeAkhlakGrade(nilaiAkhlakHalaqahRaw) : ''
  const nilaiUjianBulanan = toNullableNumber(document.getElementById('m-lap-nilai-ujian-bulanan')?.value)
  const nilaiTargetHafalan = toNullableNumber(document.getElementById('m-lap-nilai-target-hafalan')?.value)
  const nilaiCapaianHafalan = toNullableNumber(document.getElementById('m-lap-nilai-capaian-hafalan')?.value)
  const nilaiJumlahHafalanHalaman = toNullableNumber(document.getElementById('m-lap-jumlah-hafalan-halaman')?.value)
  const nilaiJumlahHafalanJuz = toNullableNumber(document.getElementById('m-lap-jumlah-hafalan-juz')?.value)

  const numericChecks = [
    [nilaiKehadiranHalaqah, 'Nilai kehadiran halaqah harus angka.'],
    [sakitHalaqah, 'Jumlah sakit halaqah harus angka.'],
    [izinHalaqah, 'Jumlah izin halaqah harus angka.'],
    [nilaiUjianBulanan, 'Nilai ujian bulanan harus angka.'],
    [nilaiTargetHafalan, 'Target hafalan harus angka.'],
    [nilaiCapaianHafalan, 'Capaian hafalan bulanan harus angka.'],
    [nilaiJumlahHafalanHalaman, 'Jumlah hafalan halaman harus angka.'],
    [nilaiJumlahHafalanJuz, 'Jumlah hafalan juz harus angka.']
  ]
  for (const [num, message] of numericChecks) {
    if (Number.isNaN(num)) {
      alert(message)
      return
    }
  }
  if (nilaiKehadiranHalaqah !== null && (nilaiKehadiranHalaqah < 0 || nilaiKehadiranHalaqah > 100)) {
    alert('Nilai kehadiran halaqah harus 0-100.')
    return
  }
  if (nilaiUjianBulanan !== null && (nilaiUjianBulanan < 0 || nilaiUjianBulanan > 100)) {
    alert('Nilai ujian bulanan harus 0-100.')
    return
  }
  if (nilaiTargetHafalan !== null && (nilaiTargetHafalan < 0 || nilaiTargetHafalan > 100)) {
    alert('Target hafalan harus 0-100%.')
    return
  }
  if (nilaiAkhlakHalaqahRaw && !nilaiAkhlakHalaqah) {
    alert('Nilai akhlak halaqah harus A, B, C, D, atau E.')
    return
  }

  const payload = {
    periode: muhaffizState.periode || getMonthInputToday(),
    guru_id: detail.waliGuruId,
    kelas_id: detail.kelasId,
    santri_id: detail.santriId,
    muhaffiz: String(document.getElementById('m-lap-muhaffiz')?.value || '').trim() || null,
    no_hp_muhaffiz: String(document.getElementById('m-lap-nohp-muhaffiz')?.value || '').trim() || null,
    nilai_kehadiran_halaqah: nilaiKehadiranHalaqah === null ? null : nilaiKehadiranHalaqah,
    sakit_halaqah: sakitHalaqah === null ? null : Math.round(sakitHalaqah),
    izin_halaqah: izinHalaqah === null ? null : Math.round(izinHalaqah),
    nilai_akhlak_halaqah: nilaiAkhlakHalaqah || null,
    keterangan_akhlak_halaqah: String(document.getElementById('m-lap-ket-akhlak-halaqah')?.value || '').trim() || null,
    nilai_ujian_bulanan: nilaiUjianBulanan === null ? null : nilaiUjianBulanan,
    keterangan_ujian_bulanan: String(document.getElementById('m-lap-ket-ujian-bulanan')?.value || '').trim() || null,
    nilai_target_hafalan: nilaiTargetHafalan === null ? null : nilaiTargetHafalan,
    keterangan_target_hafalan: String(document.getElementById('m-lap-ket-target-hafalan')?.value || '').trim() || null,
    nilai_capaian_hafalan_bulanan: nilaiCapaianHafalan === null ? null : nilaiCapaianHafalan,
    nilai_jumlah_hafalan_halaman: nilaiJumlahHafalanHalaman === null ? null : nilaiJumlahHafalanHalaman,
    nilai_jumlah_hafalan_juz: nilaiJumlahHafalanJuz === null ? null : nilaiJumlahHafalanJuz,
    catatan_muhaffiz: String(document.getElementById('m-lap-catatan-muhaffiz')?.value || '').trim() || null
  }

  const { error } = await sb
    .from(MONTHLY_REPORT_TABLE)
    .upsert(payload, { onConflict: 'periode,guru_id,kelas_id,santri_id' })
  if (error) {
    console.error(error)
    alert(`Gagal simpan ketahfizan: ${error.message || 'Unknown error'}`)
    return
  }
  alert('Data ketahfizan berhasil disimpan.')
  await openMuhaffizLaporanDetail(detail.santriId)
}

async function loadMuhaffizPage(page) {
  const targetPage = PAGE_TITLES[page] ? page : 'dashboard'
  setTopbarTitle(targetPage)
  setNavActive(targetPage)
  localStorage.setItem(MUHAFFIZ_LAST_PAGE_KEY, targetPage)
  closeTopbarUserMenu()
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

window.loadMuhaffizPage = loadMuhaffizPage
window.onMuhaffizPeriodeChange = onMuhaffizPeriodeChange
window.openMuhaffizLaporanDetail = openMuhaffizLaporanDetail
window.saveMuhaffizLaporanDetail = saveMuhaffizLaporanDetail
window.onMuhaffizAkhlakChange = onMuhaffizAkhlakChange
window.onMuhaffizUjianChange = onMuhaffizUjianChange
window.toggleTopbarUserMenu = toggleTopbarUserMenu
window.openTopbarCalendarPopup = openTopbarCalendarPopup
window.closeTopbarCalendarPopup = closeTopbarCalendarPopup
window.shiftTopbarCalendarMonth = shiftTopbarCalendarMonth
window.selectTopbarCalendarDate = selectTopbarCalendarDate
window.openMuhaffizDashboardAgendaPopup = openMuhaffizDashboardAgendaPopup
window.closeMuhaffizDashboardAgendaPopup = closeMuhaffizDashboardAgendaPopup
window.logout = logout

document.addEventListener('DOMContentLoaded', async () => {
  setupCustomPopupSystem()

  const loginId = String(localStorage.getItem('login_id') || '').trim()
  if (!loginId) {
    location.href = 'index.html'
    return
  }

  try {
    const profile = await getCurrentMuhaffiz()
    if (!profile) {
      location.href = 'index.html'
      return
    }
    muhaffizState.profile = profile
    const welcomeEl = document.getElementById('welcome')
    if (welcomeEl) welcomeEl.textContent = String(profile.nama || profile.id_karyawan || '-')
  } catch (error) {
    console.error(error)
    location.href = 'index.html'
    return
  }

  const lastPage = localStorage.getItem(MUHAFFIZ_LAST_PAGE_KEY) || 'dashboard'
  await loadMuhaffizPage(lastPage)

  document.addEventListener('click', event => {
    const wrap = document.querySelector('.topbar-user-menu-wrap')
    if (!wrap) return
    if (!wrap.contains(event.target)) closeTopbarUserMenu()
  })
})
