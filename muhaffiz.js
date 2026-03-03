const supabaseUrl = 'https://optucpelkueqmlhwlbej.supabase.co'
const supabaseKey = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9wdHVjcGVsa3VlcW1saHdsYmVqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzAxOTY4MTgsImV4cCI6MjA4NTc3MjgxOH0.Vqaey9pcnltu9uRbPk0J-AGWaGDZjQLw92pcRv67GNE'
const sb = supabase.createClient(supabaseUrl, supabaseKey)

const MONTHLY_REPORT_TABLE = 'laporan_bulanan_wali'
const MONTHLY_REPORT_CAPAIAN_TEXT_COLUMN = 'keterangan_capaian_hafalan_bulanan'
const MONTHLY_REPORT_JUMLAH_TEXT_COLUMN = 'keterangan_jumlah_hafalan_bulanan'
const MUHAFFIZ_LAST_PAGE_KEY = 'muhaffiz_last_page'
const TOPBAR_KALENDER_TABLE = 'kalender_akademik'
const TOPBAR_KALENDER_DEFAULT_COLOR = '#2563eb'
const HALAQAH_TABLE = 'halaqah'
const HALAQAH_SANTRI_TABLE = 'halaqah_santri'
const HALAQAH_SCHEDULE_TABLE = 'jadwal_halaqah'
const EKSKUL_TABLE = 'ekstrakurikuler'
const EKSKUL_MEMBER_TABLE = 'ekstrakurikuler_anggota'
const EKSKUL_INDIKATOR_TABLE = 'ekstrakurikuler_indikator'
const EKSKUL_PROGRES_TABLE = 'ekstrakurikuler_progres'
const EKSKUL_MONTHLY_TABLE = 'ekstrakurikuler_laporan_bulanan'
const SANTRI_PRESTASI_TABLE = 'santri_prestasi'
const SANTRI_PELANGGARAN_TABLE = 'santri_pelanggaran'
const SANTRI_SURAT_BUCKET = 'surat-pemberitahuan'
const KARYAWAN_FOTO_BUCKET = 'karyawan-foto'
const KARYAWAN_FOTO_MAX_SIZE_BYTES = 300 * 1024
const SCHOOL_PROFILE_TABLE = 'struktur_sekolah'
const TOPBAR_NOTIF_READ_KEY = 'muhaffiz_topbar_notif_read'
const TOPBAR_NOTIF_RANGE_KEY = 'muhaffiz_topbar_notif_range_days'

const PAGE_TITLES = {
  dashboard: 'Dashboard',
  'laporan-bulanan': 'Laporan Bulanan',
  chat: 'Chat',
  'data-halaqah': 'Data Halaqah',
  'prestasi-pelanggaran': 'Prestasi & Pelanggaran',
  ekskul: 'Ekskul',
  profil: 'Profil'
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
let topbarNotifState = {
  items: [],
  loaded: false,
  rangeDays: 3,
  readMap: {}
}
let muhaffizDashboardAgendaRows = []
let muhaffizEkskulState = {
  hasAccess: false,
  ekskulRows: [],
  memberRows: [],
  indikatorRows: [],
  santriRows: [],
  progressRows: [],
  selectedEkskulId: '',
  selectedSantriId: '',
  activeTab: 'progres',
  monthlyPeriode: '',
  monthlyRows: []
}
let muhaffizPrestasiPelanggaranState = {
  tab: 'prestasi',
  santriRows: [],
  kelasMap: new Map(),
  prestasiRows: [],
  pelanggaranRows: [],
  editingPrestasiId: '',
  editingPelanggaranId: ''
}
let muhaffizBulkImportState = {
  parsedRows: [],
  matchedRows: [],
  errors: [],
  fileName: ''
}
let muhaffizManageHalaqahState = {
  halaqahRows: [],
  memberRows: [],
  santriRows: [],
  kelasMap: new Map(),
  selectedHalaqahId: '',
  selectedSet: new Set(),
  blockedSet: new Set(),
  search: ''
}
let wakasekKetahfizanAccessCache = null

const MUHAFFIZ_BULK_EXCEL_HEADERS = [
  'Nama Santri',
  'Kelas',
  'Kehadiran Halaqah (%)',
  'Sakit',
  'Izin',
  'Akhlak',
  'Ujian Bulanan',
  'Target Hafalan',
  'Ket. Target',
  'Capaian Hafalan',
  'Jumlah Hafalan',
  'Catatan Muhaffiz'
]

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

function getKaryawanFotoInitial(nama) {
  const words = String(nama || '').trim().split(/\s+/).filter(Boolean)
  if (!words.length) return 'U'
  if (words.length === 1) return words[0].slice(0, 2).toUpperCase()
  return `${words[0][0] || ''}${words[1][0] || ''}`.toUpperCase()
}

function renderMuhaffizProfilFotoPreview(fotoUrl, nama) {
  const box = document.getElementById('muhaffiz-profil-foto-preview')
  if (!box) return
  const url = String(fotoUrl || '').trim()
  if (url) {
    box.innerHTML = `<img src="${escapeHtml(url)}" alt="Foto Profil" style="width:64px; height:64px; border-radius:999px; object-fit:cover; border:1px solid #cbd5e1;">`
    return
  }
  box.innerHTML = `<span style="width:64px; height:64px; border-radius:999px; display:inline-flex; align-items:center; justify-content:center; background:#e2e8f0; color:#0f172a; font-weight:700; border:1px solid #cbd5e1;">${escapeHtml(getKaryawanFotoInitial(nama))}</span>`
}

function getFotoFileExt(fileName = '') {
  const raw = String(fileName || '').trim().toLowerCase()
  const parts = raw.split('.')
  const ext = parts.length > 1 ? parts.pop() : ''
  if (!ext) return 'jpg'
  if (ext === 'jpeg') return 'jpg'
  if (ext === 'png' || ext === 'jpg' || ext === 'webp') return ext
  return 'jpg'
}

async function uploadMuhaffizProfilePhoto(event) {
  const file = event?.target?.files?.[0]
  if (!file) return
  try {
    if (!String(file.type || '').toLowerCase().startsWith('image/')) {
      throw new Error('File harus berupa gambar (JPG, PNG, WEBP).')
    }
    if (Number(file.size || 0) > KARYAWAN_FOTO_MAX_SIZE_BYTES) {
      throw new Error('Ukuran gambar maksimal 300 KB.')
    }
    const idKaryawan = String(document.getElementById('muhaffiz-profil-id-karyawan')?.value || 'muhaffiz').trim().replaceAll(' ', '_')
    const ext = getFotoFileExt(file.name)
    const filePath = `${idKaryawan}_${Date.now()}.${ext}`
    const uploadRes = await sb.storage.from(KARYAWAN_FOTO_BUCKET).upload(filePath, file, { upsert: true })
    if (uploadRes.error) throw uploadRes.error
    const pub = sb.storage.from(KARYAWAN_FOTO_BUCKET).getPublicUrl(filePath)
    const fotoUrl = String(pub?.data?.publicUrl || '').trim()
    if (!fotoUrl) throw new Error('URL foto tidak valid.')
    const input = document.getElementById('muhaffiz-profil-foto-url')
    if (input) input.value = fotoUrl
    const nama = String(document.getElementById('muhaffiz-profil-nama')?.value || '').trim()
    renderMuhaffizProfilFotoPreview(fotoUrl, nama)
  } catch (error) {
    alert(`Gagal upload foto: ${error?.message || 'Unknown error'}`)
  } finally {
    if (event?.target) event.target.value = ''
  }
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

function normalizeRoleValue(role) {
  return String(role || '').trim().toLowerCase().replaceAll('_', ' ')
}

function parseRoleList() {
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
  return roles.map(normalizeRoleValue).filter(Boolean)
}

function hasBaseRole(roleName) {
  const target = String(roleName || '').trim().toLowerCase()
  if (!target) return false
  return parseRoleList().some(role => normalizeRoleValue(role).replaceAll(' ', '') === target.replaceAll(' ', ''))
}

function isWakasekKetahfizanRole(role) {
  const clean = normalizeRoleValue(role).replaceAll(' ', '')
  if (!clean) return false
  return clean.includes('wakasek') && (
    clean.includes('ketahfizan') ||
    clean.includes('ketahfidz') ||
    clean.includes('tahfiz') ||
    clean.includes('tahfidz')
  )
}

function normalizePersonName(value) {
  return String(value || '').trim().replace(/\s+/g, ' ').toLowerCase()
}

function normalizePersonNameLoose(value) {
  return normalizePersonName(value)
    .replace(/[.,/#!$%^&*;:{}=_`~()\-+[\]\\|'"<>?]/g, ' ')
    .replace(/\b(ust|ustadz|ustad|ustaz|ust\.)\b/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

function isMissingSchoolProfileTableError(error) {
  const msg = String(error?.message || '').toLowerCase()
  const code = String(error?.code || '').toUpperCase()
  if (code === '42P01') return true
  if (msg.includes('schema cache') && msg.includes(SCHOOL_PROFILE_TABLE)) return true
  return (msg.includes('relation') && msg.includes(SCHOOL_PROFILE_TABLE) && msg.includes('does not exist')) || (msg.includes('table') && msg.includes('not found') && msg.includes(SCHOOL_PROFILE_TABLE))
}

function isMissingSchoolProfileColumnError(error) {
  const msg = String(error?.message || '').toLowerCase()
  return msg.includes('column') && msg.includes(SCHOOL_PROFILE_TABLE)
}

async function getIsWakasekKetahfizan(forceReload = false) {
  const loginId = String(localStorage.getItem('login_id') || '').trim()
  if (!forceReload && wakasekKetahfizanAccessCache && wakasekKetahfizanAccessCache.loginId === loginId) {
    return !!wakasekKetahfizanAccessCache.allowed
  }

  try {
    const profile = muhaffizState.profile || await getCurrentMuhaffiz()
    const profileName = normalizePersonName(profile?.nama)
    const profileId = String(profile?.id || '').trim().toLowerCase()
    const profileIdKaryawan = String(profile?.id_karyawan || '').trim().toLowerCase()

    if (!profileName) {
      wakasekKetahfizanAccessCache = { loginId, allowed: false }
      return false
    }

    const selectAttempts = [
      'wakasek_bidang_ketahfizan, wakasek_ketahfizan, wakasek_bidang_tahfiz, wakasek_tahfiz',
      'wakasek_bidang_ketahfizan, wakasek_ketahfizan',
      'wakasek_bidang_ketahfizan',
      'wakasek_ketahfizan',
      'wakasek_bidang_tahfiz',
      'wakasek_tahfiz'
    ]

    let row = null
    for (const selectText of selectAttempts) {
      const attempts = [
        () => sb.from(SCHOOL_PROFILE_TABLE).select(selectText).order('updated_at', { ascending: false }).order('created_at', { ascending: false }).limit(1),
        () => sb.from(SCHOOL_PROFILE_TABLE).select(selectText).order('created_at', { ascending: false }).limit(1),
        () => sb.from(SCHOOL_PROFILE_TABLE).select(selectText).limit(1)
      ]
      let data = null
      let handled = false
      for (const run of attempts) {
        const { data: rows, error } = await run()
        if (error) {
          if (isMissingSchoolProfileTableError(error) || isMissingSchoolProfileColumnError(error)) continue
          throw error
        }
        data = rows || []
        handled = true
        break
      }
      if (!handled) continue
      row = data?.[0] || null
      break
    }

    const wakasekRaw = String(
      row?.wakasek_bidang_ketahfizan ||
      row?.wakasek_ketahfizan ||
      row?.wakasek_bidang_tahfiz ||
      row?.wakasek_tahfiz ||
      ''
    ).trim()
    const wakasekName = normalizePersonName(wakasekRaw)
    const wakasekNameLoose = normalizePersonNameLoose(wakasekRaw)
    const wakasekToken = wakasekRaw.toLowerCase()
    const profileNameLoose = normalizePersonNameLoose(profileName)
    let allowed = !!(
      (wakasekName && wakasekName === profileName) ||
      (wakasekNameLoose && profileNameLoose && (
        wakasekNameLoose === profileNameLoose ||
        wakasekNameLoose.includes(profileNameLoose) ||
        profileNameLoose.includes(wakasekNameLoose)
      )) ||
      (profileId && wakasekToken === profileId) ||
      (profileIdKaryawan && wakasekToken === profileIdKaryawan)
    )

    // Fallback backward compatibility: gunakan role login jika struktur sekolah belum terisi.
    if (!allowed && !wakasekRaw) {
      allowed = parseRoleList().some(isWakasekKetahfizanRole)
    }

    wakasekKetahfizanAccessCache = { loginId, allowed }
    return !!allowed
  } catch (error) {
    console.error(error)
    wakasekKetahfizanAccessCache = { loginId, allowed: false }
    return false
  }
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
  let data = null
  let error = null
  const variants = [
    'id, id_karyawan, nama, no_hp, alamat, password, aktif, foto_url',
    'id, id_karyawan, nama, no_hp, alamat, password, aktif'
  ]
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
  if (error) throw error
  if (!data || !asBool(data.aktif)) return null
  return data
}

async function setMuhaffizWelcomeName() {
  try {
    const profile = await getCurrentMuhaffiz()
    if (!profile) return
    muhaffizState.profile = profile
    const welcomeEl = document.getElementById('welcome')
    const name = String(profile.nama || profile.id_karyawan || '-')
    if (welcomeEl) welcomeEl.textContent = name
    if (typeof window.setTopbarUserIdentity === 'function') {
      window.setTopbarUserIdentity({ name, foto_url: String(profile.foto_url || '') })
    }
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

function loadMuhaffizNotifPrefs() {
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

function saveMuhaffizNotifReadMap() {
  try {
    localStorage.setItem(TOPBAR_NOTIF_READ_KEY, JSON.stringify(topbarNotifState.readMap || {}))
  } catch (_err) {}
}

function setMuhaffizNotifRangeDays(days) {
  const value = Number(days)
  topbarNotifState.rangeDays = [1, 3, 7].includes(value) ? value : 3
  localStorage.setItem(TOPBAR_NOTIF_RANGE_KEY, String(topbarNotifState.rangeDays))
}

function buildMuhaffizNotifDateKeys() {
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

function getMuhaffizNotifId(item) {
  return String(item?.id || `${item?.type || ''}|${item?.title || ''}|${item?.meta || ''}|${item?.sortKey || ''}`)
}

function isMuhaffizNotifRead(item) {
  return topbarNotifState.readMap?.[getMuhaffizNotifId(item)] === true
}

function markMuhaffizNotifRead(id) {
  const key = String(id || '').trim()
  if (!key) return
  if (!topbarNotifState.readMap) topbarNotifState.readMap = {}
  topbarNotifState.readMap[key] = true
  saveMuhaffizNotifReadMap()
}

function markMuhaffizNotifItemRead(id) {
  markMuhaffizNotifRead(id)
  renderTopbarNotifMenu(topbarNotifState.items)
  setTopbarNotifBadge((topbarNotifState.items || []).filter(item => !isMuhaffizNotifRead(item)).length)
}

function markAllMuhaffizNotifRead() {
  ;(topbarNotifState.items || []).forEach(item => markMuhaffizNotifRead(getMuhaffizNotifId(item)))
  renderTopbarNotifMenu(topbarNotifState.items)
  setTopbarNotifBadge(0)
}

function setMuhaffizNotifRangeFilter(days) {
  setMuhaffizNotifRangeDays(days)
  topbarNotifState.loaded = false
  refreshMuhaffizTopbarNotifications(true).catch(error => console.error(error))
}

function renderTopbarNotifMenu(items = []) {
  const menu = document.getElementById('topbar-notif-menu')
  if (!menu) return
  const list = Array.isArray(items) ? items : []
  const selectedRange = Number(topbarNotifState.rangeDays || 3)
  const filtersHtml = [1, 3, 7].map(days => (
    `<button type="button" class="topbar-notif-filter-btn ${selectedRange === days ? 'active' : ''}" onclick="setMuhaffizNotifRangeFilter(${days})">${days === 1 ? 'Hari ini' : `${days} hari`}</button>`
  )).join('')
  const headHtml = `
    <div class="topbar-notif-head">
      <div class="topbar-notif-head-row">
        <span>Aktivitas Terdekat</span>
        <button type="button" class="topbar-notif-mark-btn" onclick="markAllMuhaffizNotifRead()">Tandai semua dibaca</button>
      </div>
      <div class="topbar-notif-filters">${filtersHtml}</div>
    </div>
  `
  if (!list.length) {
    menu.innerHTML = `${headHtml}<div class="topbar-notif-empty">Belum ada notifikasi aktivitas terdekat.</div>`
    return
  }
  const rowsHtml = list.map(item => `
    <button type="button" class="topbar-notif-item ${isMuhaffizNotifRead(item) ? 'read' : 'unread'}" data-notif-id="${escapeHtml(getMuhaffizNotifId(item))}" onclick="markMuhaffizNotifItemRead(this.getAttribute('data-notif-id'))">
      <div class="topbar-notif-type">${escapeHtml(item.type || 'Aktivitas')}</div>
      <div class="topbar-notif-title">${escapeHtml(item.title || '-')}</div>
      <div class="topbar-notif-meta">${escapeHtml(item.meta || '-')}</div>
      ${item.desc ? `<div class="topbar-notif-desc">${escapeHtml(item.desc)}</div>` : ''}
      <div class="topbar-notif-status">${isMuhaffizNotifRead(item) ? 'Dibaca' : 'Belum dibaca'}</div>
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

async function fetchMuhaffizTopbarNotifications() {
  const items = []
  const dateKeys = buildMuhaffizNotifDateKeys()

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
    const muhaffizId = String(muhaffizState?.profile?.id || '').trim()
    if (muhaffizId) {
      const [halaqahRes, jadwalRes] = await Promise.all([
        sb.from(HALAQAH_TABLE).select('id, nama').eq('muhaffiz_id', muhaffizId).order('nama'),
        sb.from(HALAQAH_SCHEDULE_TABLE).select('id, nama_sesi, jam_mulai, jam_selesai, urutan').order('urutan', { ascending: true }).order('jam_mulai', { ascending: true })
      ])
      if (!halaqahRes.error && !jadwalRes.error) {
        const halaqahNames = (halaqahRes.data || []).map(item => String(item?.nama || '').trim()).filter(Boolean)
        const halaqahLabel = halaqahNames.length ? halaqahNames.join(', ') : 'Belum ada halaqah binaan'
        dateKeys.forEach(dateKey => {
          ;(jadwalRes.data || []).forEach(row => {
            items.push({
              id: `halaqah|${dateKey}|${String(row?.id || '')}`,
              type: 'Jam Halaqah',
              title: String(row?.nama_sesi || 'Sesi Halaqah'),
              meta: `${formatNotifDateLabel(dateKey)} | ${toTimeLabel(row?.jam_mulai)}-${toTimeLabel(row?.jam_selesai)}`,
              desc: `Halaqah: ${halaqahLabel}`,
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

async function refreshMuhaffizTopbarNotifications(forceReload = false) {
  ensureTopbarNotification()
  if (!forceReload && topbarNotifState.loaded) {
    renderTopbarNotifMenu(topbarNotifState.items)
    setTopbarNotifBadge((topbarNotifState.items || []).filter(item => !isMuhaffizNotifRead(item)).length)
    return
  }
  const items = await fetchMuhaffizTopbarNotifications()
  topbarNotifState.items = items
  topbarNotifState.loaded = true
  renderTopbarNotifMenu(items)
  setTopbarNotifBadge(items.filter(item => !isMuhaffizNotifRead(item)).length)
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
  await refreshMuhaffizTopbarNotifications(true)
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

function ensureMuhaffizBulkModal() {
  let overlay = document.getElementById('muhaffiz-bulk-overlay')
  if (overlay) return overlay
  overlay = document.createElement('div')
  overlay.id = 'muhaffiz-bulk-overlay'
  overlay.style.cssText = 'position:fixed; inset:0; background:rgba(15,23,42,0.35); display:none; align-items:center; justify-content:center; z-index:11000; padding:16px; box-sizing:border-box;'
  overlay.innerHTML = `
    <div style="width:min(1320px, calc(100vw - 32px)); max-height:calc(100vh - 32px); overflow:hidden; border:1px solid #dbeafe; border-radius:0; background:#fff; box-shadow:0 18px 34px rgba(15,23,42,0.18); display:flex; flex-direction:column;">
      <div style="display:flex; align-items:center; justify-content:space-between; padding:12px 14px; border-bottom:1px solid #e2e8f0; gap:10px;">
        <div style="font-weight:700; color:#0f172a;">Input Massal Ketahfizan</div>
        <button type="button" class="modal-btn" onclick="closeMuhaffizBulkInputModal()">Tutup</button>
      </div>
      <div id="muhaffiz-bulk-body" style="padding:12px; overflow:auto;">Loading...</div>
    </div>
  `
  overlay.addEventListener('click', event => {
    if (event.target !== overlay) return
    closeMuhaffizBulkInputModal()
  })
  document.body.appendChild(overlay)
  return overlay
}

function closeMuhaffizBulkInputModal() {
  const overlay = document.getElementById('muhaffiz-bulk-overlay')
  if (!overlay) return
  overlay.style.display = 'none'
}

function isMuhaffizMissingCapaianTextColumnError(error) {
  const msg = String(error?.message || '').toLowerCase()
  return msg.includes('column') && msg.includes(MONTHLY_REPORT_CAPAIAN_TEXT_COLUMN)
}

function isMuhaffizMissingJumlahTextColumnError(error) {
  const msg = String(error?.message || '').toLowerCase()
  return msg.includes('column') && msg.includes(MONTHLY_REPORT_JUMLAH_TEXT_COLUMN)
}

function validateMuhaffizBulkRow(row) {
  const errors = []
  const nama = String(row.nama || '-')
  const nilaiKehadiran = toNullableNumber(row.nilaiKehadiran)
  const sakit = toNullableNumber(row.sakit)
  const izin = toNullableNumber(row.izin)
  const nilaiUjian = toNullableNumber(row.nilaiUjian)
  const nilaiTarget = toNullableNumber(row.nilaiTarget)
  const nilaiCapaian = toNullableNumber(row.nilaiCapaian)

  if (Number.isNaN(nilaiKehadiran)) errors.push(`${nama}: Kehadiran harus angka.`)
  if (Number.isNaN(sakit)) errors.push(`${nama}: Sakit harus angka.`)
  if (Number.isNaN(izin)) errors.push(`${nama}: Izin harus angka.`)
  if (Number.isNaN(nilaiUjian)) errors.push(`${nama}: Ujian bulanan harus angka.`)
  if (Number.isNaN(nilaiTarget)) errors.push(`${nama}: Target hafalan harus angka.`)

  if (nilaiKehadiran !== null && (nilaiKehadiran < 0 || nilaiKehadiran > 100)) errors.push(`${nama}: Kehadiran harus 0-100.`)
  if (nilaiUjian !== null && (nilaiUjian < 0 || nilaiUjian > 100)) errors.push(`${nama}: Ujian bulanan harus 0-100.`)
  if (nilaiTarget !== null && nilaiTarget < 0) errors.push(`${nama}: Target hafalan tidak boleh minus.`)
  if (sakit !== null && sakit < 0) errors.push(`${nama}: Sakit tidak boleh minus.`)
  if (izin !== null && izin < 0) errors.push(`${nama}: Izin tidak boleh minus.`)
  if (nilaiCapaian !== null && !Number.isNaN(nilaiCapaian) && nilaiCapaian < 0) errors.push(`${nama}: Capaian hafalan tidak boleh minus.`)

  const akhlakRaw = String(row.akhlak || '').trim().toUpperCase()
  if (akhlakRaw && !normalizeAkhlakGrade(akhlakRaw)) errors.push(`${nama}: Nilai akhlak halaqah harus A-E.`)

  return errors
}

function normalizeMuhaffizImportHeader(value) {
  return String(value || '')
    .trim()
    .toLowerCase()
    .replace(/[%().]/g, '')
    .replace(/\s+/g, ' ')
}

function getMuhaffizImportCellByAliases(row, aliases) {
  const entries = Object.entries(row || {})
  const normalizedMap = new Map(entries.map(([key, val]) => [normalizeMuhaffizImportHeader(key), val]))
  for (const alias of aliases) {
    const value = normalizedMap.get(normalizeMuhaffizImportHeader(alias))
    if (value !== undefined) return value
  }
  return ''
}

function getMuhaffizBulkDomRowMaps() {
  const rows = Array.from(document.querySelectorAll('#muhaffiz-bulk-body tr[data-m-bulk-row="1"]'))
  const bySantriId = new Map()
  const byNameClass = new Map()
  rows.forEach(row => {
    const sid = String(row.getAttribute('data-santri-id') || '').trim()
    const nama = String(row.children?.[1]?.textContent || '').trim().toLowerCase()
    const kelas = String(row.children?.[2]?.textContent || '').trim().toLowerCase()
    if (sid) bySantriId.set(sid, row)
    const key = `${nama}|${kelas}`
    if (!byNameClass.has(key)) byNameClass.set(key, [])
    byNameClass.get(key).push(row)
  })
  return { rows, bySantriId, byNameClass }
}

function setMuhaffizBulkDomRowValues(rowEl, rowData) {
  const setVal = (field, value) => {
    const input = rowEl.querySelector(`[data-bulk-field="${field}"]`)
    if (!input) return
    input.value = value === null || value === undefined ? '' : String(value)
  }
  setVal('kehadiran', rowData.nilaiKehadiran)
  setVal('sakit', rowData.sakit)
  setVal('izin', rowData.izin)
  setVal('akhlak', normalizeAkhlakGrade(rowData.akhlak) || '')
  setVal('ujian', rowData.nilaiUjian)
  setVal('target', rowData.nilaiTarget)
  setVal('ket-target', rowData.ketTarget)
  setVal('capaian', rowData.nilaiCapaian)
  setVal('jumlah', rowData.jumlah)
  setVal('catatan', rowData.catatan)
}

function collectMuhaffizBulkRowsForTemplate() {
  const { rows } = getMuhaffizBulkDomRowMaps()
  return rows.map(row => ({
    nama: String(row.children?.[1]?.textContent || '').trim(),
    kelas: String(row.children?.[2]?.textContent || '').trim(),
    nilaiKehadiran: String(row.querySelector('[data-bulk-field="kehadiran"]')?.value || '').trim(),
    sakit: String(row.querySelector('[data-bulk-field="sakit"]')?.value || '').trim(),
    izin: String(row.querySelector('[data-bulk-field="izin"]')?.value || '').trim(),
    akhlak: String(row.querySelector('[data-bulk-field="akhlak"]')?.value || '').trim(),
    nilaiUjian: String(row.querySelector('[data-bulk-field="ujian"]')?.value || '').trim(),
    nilaiTarget: String(row.querySelector('[data-bulk-field="target"]')?.value || '').trim(),
    ketTarget: String(row.querySelector('[data-bulk-field="ket-target"]')?.value || '').trim(),
    nilaiCapaian: String(row.querySelector('[data-bulk-field="capaian"]')?.value || '').trim(),
    jumlah: String(row.querySelector('[data-bulk-field="jumlah"]')?.value || '').trim(),
    catatan: String(row.querySelector('[data-bulk-field="catatan"]')?.value || '').trim()
  }))
}

function renderMuhaffizBulkImportPreview() {
  const box = document.getElementById('muhaffiz-bulk-import-preview')
  if (!box) return
  const matchedRows = muhaffizBulkImportState.matchedRows || []
  const errors = muhaffizBulkImportState.errors || []
  const fileName = String(muhaffizBulkImportState.fileName || '').trim()

  if (!fileName) {
    box.style.display = 'none'
    box.innerHTML = ''
    return
  }

  box.style.display = 'block'
  box.innerHTML = `
    <div style="border:1px solid #e2e8f0; border-radius:10px; padding:10px; background:#f8fafc;">
      <div style="display:flex; justify-content:space-between; gap:10px; align-items:center; flex-wrap:wrap;">
        <div style="font-size:13px; color:#0f172a;">
          File: <strong>${escapeHtml(fileName)}</strong> |
          Data cocok: <strong>${matchedRows.length}</strong> |
          Error: <strong style="color:${errors.length ? '#b91c1c' : '#16a34a'};">${errors.length}</strong>
        </div>
        <button type="button" class="modal-btn modal-btn-primary" onclick="applyMuhaffizBulkExcelToTable()" ${matchedRows.length ? '' : 'disabled'}>Gunakan Data Excel</button>
      </div>
      ${errors.length ? `
        <div style="margin-top:8px; max-height:130px; overflow:auto; font-size:12px; color:#b91c1c; background:#fff; border:1px solid #fecaca; border-radius:8px; padding:8px;">
          ${errors.slice(0, 40).map(item => `<div>- ${escapeHtml(item)}</div>`).join('')}
          ${errors.length > 40 ? '<div>... dan lainnya</div>' : ''}
        </div>
      ` : ''}
    </div>
  `
}

function downloadMuhaffizBulkTemplate() {
  const rows = collectMuhaffizBulkRowsForTemplate()
  if (!window.XLSX) {
    alert('Library Excel belum termuat. Refresh halaman lalu coba lagi.')
    return
  }
  const exportRows = (rows.length ? rows : (muhaffizState.santriList || []).map(item => {
    const kelas = muhaffizState.kelasMap.get(String(item.kelas_id || ''))
    return { nama: item.nama || '-', kelas: kelas?.nama_kelas || '-' }
  })).map(item => ({
    'Nama Santri': item.nama || '',
    'Kelas': item.kelas || '',
    'Kehadiran Halaqah (%)': item.nilaiKehadiran || '',
    'Sakit': item.sakit || '',
    'Izin': item.izin || '',
    'Akhlak': item.akhlak || '',
    'Ujian Bulanan': item.nilaiUjian || '',
    'Target Hafalan': item.nilaiTarget || '',
    'Ket. Target': item.ketTarget || '',
    'Capaian Hafalan': item.nilaiCapaian || '',
    'Jumlah Hafalan': item.jumlah || '',
    'Catatan Muhaffiz': item.catatan || ''
  }))
  const wb = window.XLSX.utils.book_new()
  const ws = window.XLSX.utils.json_to_sheet(exportRows, { header: MUHAFFIZ_BULK_EXCEL_HEADERS })
  window.XLSX.utils.book_append_sheet(wb, ws, 'InputMassalKetahfizan')
  const periode = muhaffizState.periode || getMonthInputToday()
  window.XLSX.writeFile(wb, `Template-Input-Massal-Ketahfizan-${periode}.xlsx`)
}

async function onMuhaffizBulkExcelFileChange(event) {
  const file = event?.target?.files?.[0]
  if (!file) return
  if (!window.XLSX) {
    alert('Library Excel belum termuat. Refresh halaman lalu coba lagi.')
    event.target.value = ''
    return
  }
  try {
    const buffer = await file.arrayBuffer()
    const wb = window.XLSX.read(buffer, { type: 'array' })
    const firstSheet = wb.SheetNames?.[0]
    if (!firstSheet) {
      alert('File Excel kosong.')
      event.target.value = ''
      return
    }
    const ws = wb.Sheets[firstSheet]
    const rawRows = window.XLSX.utils.sheet_to_json(ws, { defval: '' })
    const { byNameClass } = getMuhaffizBulkDomRowMaps()
    const keyUsedCount = new Map()
    const parsedRows = []
    const matchedRows = []
    const errors = []

    rawRows.forEach((raw, idx) => {
      const nama = String(getMuhaffizImportCellByAliases(raw, ['Nama Santri', 'Nama'])) .trim()
      const kelas = String(getMuhaffizImportCellByAliases(raw, ['Kelas'])) .trim()
      if (!nama && !kelas) return

      const jumlahHafalanRaw = String(getMuhaffizImportCellByAliases(raw, ['Jumlah Hafalan', 'Jumlah'])).trim()
      const jumlahHalamanLegacy = String(getMuhaffizImportCellByAliases(raw, ['Jumlah (halaman)', 'Jumlah Halaman'])).trim()
      const jumlahJuzLegacy = String(getMuhaffizImportCellByAliases(raw, ['Jumlah (juz)', 'Jumlah Juz'])).trim()
      const rowData = {
        nama,
        kelas,
        nilaiKehadiran: String(getMuhaffizImportCellByAliases(raw, ['Kehadiran Halaqah (%)', 'Kehadiran Halaqah', 'Kehadiran'])).trim(),
        sakit: String(getMuhaffizImportCellByAliases(raw, ['Sakit'])).trim(),
        izin: String(getMuhaffizImportCellByAliases(raw, ['Izin'])).trim(),
        akhlak: String(getMuhaffizImportCellByAliases(raw, ['Akhlak'])).trim(),
        nilaiUjian: String(getMuhaffizImportCellByAliases(raw, ['Ujian Bulanan', 'Ujian'])).trim(),
        nilaiTarget: String(getMuhaffizImportCellByAliases(raw, ['Target Hafalan', 'Target Hafalan (%)', 'Target'])).trim(),
        ketTarget: String(getMuhaffizImportCellByAliases(raw, ['Ket. Target', 'Ket Target', 'Keterangan Target'])).trim(),
        nilaiCapaian: String(getMuhaffizImportCellByAliases(raw, ['Capaian Hafalan', 'Capaian (halaman)', 'Capaian'])).trim(),
        jumlah: jumlahHafalanRaw || ((jumlahHalamanLegacy || jumlahJuzLegacy) ? `${jumlahHalamanLegacy || '-'} halaman / ${jumlahJuzLegacy || '-'} juz` : ''),
        catatan: String(getMuhaffizImportCellByAliases(raw, ['Catatan Muhaffiz', 'Catatan'])).trim()
      }
      parsedRows.push(rowData)

      const normKey = `${nama.toLowerCase()}|${kelas.toLowerCase()}`
      const candidates = byNameClass.get(normKey) || []
      const used = keyUsedCount.get(normKey) || 0
      const targetRow = candidates[used] || null
      if (!targetRow) {
        errors.push(`Baris ${idx + 2}: Santri "${nama}" kelas "${kelas}" tidak ditemukan di tabel.`)
        return
      }
      keyUsedCount.set(normKey, used + 1)
      const validationErrors = validateMuhaffizBulkRow({ ...rowData, nama: `${nama} (${kelas})` })
      if (validationErrors.length) {
        errors.push(...validationErrors)
        return
      }
      matchedRows.push({ targetRow, rowData })
    })

    muhaffizBulkImportState = {
      parsedRows,
      matchedRows,
      errors,
      fileName: String(file.name || '')
    }
    renderMuhaffizBulkImportPreview()
  } catch (error) {
    console.error(error)
    alert(`Gagal membaca file Excel: ${error?.message || 'Unknown error'}`)
  } finally {
    event.target.value = ''
  }
}

function applyMuhaffizBulkExcelToTable() {
  const matchedRows = muhaffizBulkImportState.matchedRows || []
  const errors = muhaffizBulkImportState.errors || []
  if (!matchedRows.length) {
    alert('Tidak ada data valid dari Excel yang bisa diterapkan.')
    return
  }
  matchedRows.forEach(item => {
    setMuhaffizBulkDomRowValues(item.targetRow, item.rowData)
  })
  alert(`Data Excel diterapkan ke tabel: ${matchedRows.length} baris.${errors.length ? `\nError: ${errors.length} baris.` : ''}`)
}

async function openMuhaffizBulkInputModal() {
  if (!Array.isArray(muhaffizState.santriList) || !muhaffizState.santriList.length) {
    alert('Belum ada data santri untuk input massal.')
    return
  }
  const periode = muhaffizState.periode || getMonthInputToday()
  const overlay = ensureMuhaffizBulkModal()
  const body = document.getElementById('muhaffiz-bulk-body')
  if (!overlay || !body) return
  overlay.style.display = 'flex'
  body.innerHTML = 'Loading data input massal...'

  const santriList = muhaffizState.santriList || []
  const kelasMap = muhaffizState.kelasMap || new Map()
  const santriIds = santriList.map(item => String(item.id || '')).filter(Boolean)

  const reportByKey = new Map()
  let reportRes = await sb
    .from(MONTHLY_REPORT_TABLE)
    .select(`santri_id, kelas_id, guru_id, muhaffiz, no_hp_muhaffiz, nilai_kehadiran_halaqah, sakit_halaqah, izin_halaqah, nilai_akhlak_halaqah, nilai_ujian_bulanan, nilai_target_hafalan, keterangan_target_hafalan, nilai_capaian_hafalan_bulanan, ${MONTHLY_REPORT_CAPAIAN_TEXT_COLUMN}, ${MONTHLY_REPORT_JUMLAH_TEXT_COLUMN}, nilai_jumlah_hafalan_halaman, nilai_jumlah_hafalan_juz, catatan_muhaffiz`)
    .eq('periode', periode)
    .in('santri_id', santriIds)
  if (reportRes.error && isMuhaffizMissingCapaianTextColumnError(reportRes.error)) {
    reportRes = await sb
      .from(MONTHLY_REPORT_TABLE)
      .select('santri_id, kelas_id, guru_id, muhaffiz, no_hp_muhaffiz, nilai_kehadiran_halaqah, sakit_halaqah, izin_halaqah, nilai_akhlak_halaqah, nilai_ujian_bulanan, nilai_target_hafalan, keterangan_target_hafalan, nilai_capaian_hafalan_bulanan, nilai_jumlah_hafalan_halaman, nilai_jumlah_hafalan_juz, catatan_muhaffiz')
      .eq('periode', periode)
      .in('santri_id', santriIds)
  }
  if (reportRes.error && isMuhaffizMissingJumlahTextColumnError(reportRes.error)) {
    reportRes = await sb
      .from(MONTHLY_REPORT_TABLE)
      .select(`santri_id, kelas_id, guru_id, muhaffiz, no_hp_muhaffiz, nilai_kehadiran_halaqah, sakit_halaqah, izin_halaqah, nilai_akhlak_halaqah, nilai_ujian_bulanan, nilai_target_hafalan, keterangan_target_hafalan, nilai_capaian_hafalan_bulanan, ${MONTHLY_REPORT_CAPAIAN_TEXT_COLUMN}, nilai_jumlah_hafalan_halaman, nilai_jumlah_hafalan_juz, catatan_muhaffiz`)
      .eq('periode', periode)
      .in('santri_id', santriIds)
  }
  if (reportRes.error) {
    console.error(reportRes.error)
    body.innerHTML = `<div class="placeholder-card">Gagal load data laporan bulanan: ${escapeHtml(reportRes.error.message || 'Unknown error')}</div>`
    return
  }
  ;(reportRes.data || []).forEach(row => {
    const key = `${String(row?.santri_id || '')}|${String(row?.kelas_id || '')}|${String(row?.guru_id || '')}`
    reportByKey.set(key, row)
  })

  const currentMuhaffizNama = String(muhaffizState?.profile?.nama || '').trim()
  const currentMuhaffizNoHp = String(muhaffizState?.profile?.no_hp || '').trim()
  muhaffizBulkImportState = { parsedRows: [], matchedRows: [], errors: [], fileName: '' }
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
    const nilaiAkhlak = normalizeAkhlakGrade(report?.nilai_akhlak_halaqah)

    return `
      <tr data-m-bulk-row="1" data-santri-id="${escapeHtml(sid)}" data-kelas-id="${escapeHtml(kelasId)}" data-guru-id="${escapeHtml(waliGuruId)}">
        <td style="padding:6px; border:1px solid #e2e8f0; text-align:center;">${idx + 1}</td>
        <td style="padding:6px; border:1px solid #e2e8f0; min-width:220px;">${escapeHtml(santri.nama || '-')}</td>
        <td style="padding:6px; border:1px solid #e2e8f0; min-width:110px;">${escapeHtml(kelas?.nama_kelas || '-')}</td>
        <td style="padding:6px; border:1px solid #e2e8f0; min-width:110px;"><input data-bulk-field="kehadiran" class="guru-field" type="number" min="0" max="100" step="0.01" value="${escapeHtml(toInputValue(report?.nilai_kehadiran_halaqah))}"></td>
        <td style="padding:6px; border:1px solid #e2e8f0; min-width:90px;"><input data-bulk-field="sakit" class="guru-field" type="number" min="0" step="1" value="${escapeHtml(toInputValue(report?.sakit_halaqah))}"></td>
        <td style="padding:6px; border:1px solid #e2e8f0; min-width:90px;"><input data-bulk-field="izin" class="guru-field" type="number" min="0" step="1" value="${escapeHtml(toInputValue(report?.izin_halaqah))}"></td>
        <td style="padding:6px; border:1px solid #e2e8f0; min-width:95px;"><select data-bulk-field="akhlak" class="guru-field">${gradeOptions(nilaiAkhlak)}</select></td>
        <td style="padding:6px; border:1px solid #e2e8f0; min-width:110px;"><input data-bulk-field="ujian" class="guru-field" type="number" min="0" max="100" step="0.01" value="${escapeHtml(toInputValue(report?.nilai_ujian_bulanan))}"></td>
        <td style="padding:6px; border:1px solid #e2e8f0; min-width:120px;"><input data-bulk-field="target" class="guru-field" type="number" min="0" step="0.01" value="${escapeHtml(toInputValue(report?.nilai_target_hafalan))}"></td>
        <td style="padding:6px; border:1px solid #e2e8f0; min-width:200px;"><input data-bulk-field="ket-target" class="guru-field" type="text" value="${escapeHtml(String(report?.keterangan_target_hafalan || '').trim())}"></td>
        <td style="padding:6px; border:1px solid #e2e8f0; min-width:200px;"><input data-bulk-field="capaian" class="guru-field" type="text" value="${escapeHtml(String(report?.[MONTHLY_REPORT_CAPAIAN_TEXT_COLUMN] || '').trim() || toInputValue(report?.nilai_capaian_hafalan_bulanan))}" placeholder="Contoh: Murojaah 1/2 juz"></td>
        <td style="padding:6px; border:1px solid #e2e8f0; min-width:220px;"><input data-bulk-field="jumlah" class="guru-field" type="text" value="${escapeHtml(String(report?.[MONTHLY_REPORT_JUMLAH_TEXT_COLUMN] || '').trim() || ((report?.nilai_jumlah_hafalan_halaman !== null && report?.nilai_jumlah_hafalan_halaman !== undefined) || (report?.nilai_jumlah_hafalan_juz !== null && report?.nilai_jumlah_hafalan_juz !== undefined) ? `${toInputValue(report?.nilai_jumlah_hafalan_halaman)} halaman / ${toInputValue(report?.nilai_jumlah_hafalan_juz)} juz` : ''))}" placeholder="Contoh: 1 juz 5 halaman / murojaah juz 3"></td>
        <td style="padding:6px; border:1px solid #e2e8f0; min-width:260px;"><input data-bulk-field="catatan" class="guru-field" type="text" value="${escapeHtml(String(report?.catatan_muhaffiz || '').trim())}"></td>
      </tr>
    `
  }).join('')

  body.innerHTML = `
    <div style="display:grid; gap:8px;">
      <div style="display:flex; align-items:center; justify-content:space-between; gap:8px; flex-wrap:wrap;">
        <div style="font-size:13px; color:#334155;">
          Periode: <strong>${escapeHtml(getPeriodeLabel(periode))}</strong> | Muhaffiz: <strong>${escapeHtml(currentMuhaffizNama || '-')}</strong> | No HP: <strong>${escapeHtml(currentMuhaffizNoHp || '-')}</strong>
        </div>
        <div style="display:flex; gap:8px; flex-wrap:wrap;">
          <input id="muhaffiz-bulk-excel-input" type="file" accept=".xlsx,.xls" style="display:none;" onchange="onMuhaffizBulkExcelFileChange(event)">
          <button type="button" class="modal-btn" onclick="downloadMuhaffizBulkTemplate()">Download Template Excel</button>
          <button type="button" class="modal-btn" onclick="document.getElementById('muhaffiz-bulk-excel-input')?.click()">Upload Excel</button>
          <button type="button" class="modal-btn modal-btn-primary" onclick="saveMuhaffizBulkInput()">Simpan Semua</button>
        </div>
      </div>
      <div id="muhaffiz-bulk-import-preview" style="display:none;"></div>
      <div style="overflow:auto; border:1px solid #e2e8f0; border-radius:10px;">
        <table style="width:100%; min-width:2100px; border-collapse:collapse; font-size:12px;">
          <thead>
            <tr style="background:#f8fafc;">
              <th style="padding:6px; border:1px solid #e2e8f0; width:44px;">No</th>
              <th style="padding:6px; border:1px solid #e2e8f0; text-align:left;">Nama Santri</th>
              <th style="padding:6px; border:1px solid #e2e8f0; text-align:left;">Kelas</th>
              <th style="padding:6px; border:1px solid #e2e8f0;">Kehadiran Halaqah (%)</th>
              <th style="padding:6px; border:1px solid #e2e8f0;">Sakit</th>
              <th style="padding:6px; border:1px solid #e2e8f0;">Izin</th>
              <th style="padding:6px; border:1px solid #e2e8f0;">Akhlak</th>
              <th style="padding:6px; border:1px solid #e2e8f0;">Ujian Bulanan</th>
              <th style="padding:6px; border:1px solid #e2e8f0;">Target Hafalan</th>
              <th style="padding:6px; border:1px solid #e2e8f0;">Ket. Target</th>
              <th style="padding:6px; border:1px solid #e2e8f0;">Capaian Hafalan</th>
              <th style="padding:6px; border:1px solid #e2e8f0;">Jumlah Hafalan</th>
              <th style="padding:6px; border:1px solid #e2e8f0;">Catatan Muhaffiz</th>
            </tr>
          </thead>
          <tbody>
            ${rowsHtml}
          </tbody>
        </table>
      </div>
    </div>
  `
}

async function saveMuhaffizBulkInput() {
  const periode = muhaffizState.periode || getMonthInputToday()
  const currentMuhaffizNama = String(muhaffizState?.profile?.nama || '').trim()
  const currentMuhaffizNoHp = String(muhaffizState?.profile?.no_hp || '').trim()
  const rows = Array.from(document.querySelectorAll('#muhaffiz-bulk-body tr[data-m-bulk-row="1"]'))
  if (!rows.length) {
    alert('Tidak ada data untuk disimpan.')
    return
  }

  const payload = []
  const validationErrors = []
  rows.forEach(row => {
    const sid = String(row.getAttribute('data-santri-id') || '').trim()
    const kelasId = String(row.getAttribute('data-kelas-id') || '').trim()
    const guruId = String(row.getAttribute('data-guru-id') || '').trim()
    const nama = String(row.children?.[1]?.textContent || '-').trim()
    const nilaiKehadiran = String(row.querySelector('[data-bulk-field="kehadiran"]')?.value || '').trim()
    const sakit = String(row.querySelector('[data-bulk-field="sakit"]')?.value || '').trim()
    const izin = String(row.querySelector('[data-bulk-field="izin"]')?.value || '').trim()
    const akhlak = String(row.querySelector('[data-bulk-field="akhlak"]')?.value || '').trim().toUpperCase()
    const nilaiUjian = String(row.querySelector('[data-bulk-field="ujian"]')?.value || '').trim()
    const nilaiTarget = String(row.querySelector('[data-bulk-field="target"]')?.value || '').trim()
    const ketTarget = String(row.querySelector('[data-bulk-field="ket-target"]')?.value || '').trim()
    const nilaiCapaian = String(row.querySelector('[data-bulk-field="capaian"]')?.value || '').trim()
    const jumlahHafalan = String(row.querySelector('[data-bulk-field="jumlah"]')?.value || '').trim()
    const catatan = String(row.querySelector('[data-bulk-field="catatan"]')?.value || '').trim()

    validationErrors.push(...validateMuhaffizBulkRow({
      nama,
      nilaiKehadiran,
      sakit,
      izin,
      akhlak,
      nilaiUjian,
      nilaiTarget,
      nilaiCapaian,
      jumlahHafalan
    }))

    if (!sid || !kelasId || !guruId) {
      validationErrors.push(`${nama}: wali kelas/guru belum terdeteksi.`)
      return
    }

    const akhlakGrade = normalizeAkhlakGrade(akhlak)
    const ujianValue = toNullableNumber(nilaiUjian)
    const capaianNumeric = toNullableNumber(nilaiCapaian)
    payload.push({
      periode,
      guru_id: guruId,
      kelas_id: kelasId,
      santri_id: sid,
      muhaffiz: currentMuhaffizNama || null,
      no_hp_muhaffiz: currentMuhaffizNoHp || null,
      nilai_kehadiran_halaqah: toNullableNumber(nilaiKehadiran),
      sakit_halaqah: toNullableNumber(sakit) === null ? null : Math.round(Number(sakit)),
      izin_halaqah: toNullableNumber(izin) === null ? null : Math.round(Number(izin)),
      nilai_akhlak_halaqah: akhlakGrade || null,
      keterangan_akhlak_halaqah: akhlakGrade ? getAkhlakKeteranganByGrade(akhlakGrade) : null,
      nilai_ujian_bulanan: ujianValue,
      keterangan_ujian_bulanan: ujianValue === null ? null : getUjianBulananKeterangan(ujianValue),
      nilai_target_hafalan: toNullableNumber(nilaiTarget),
      keterangan_target_hafalan: ketTarget || null,
      nilai_capaian_hafalan_bulanan: Number.isNaN(capaianNumeric) ? null : capaianNumeric,
      [MONTHLY_REPORT_CAPAIAN_TEXT_COLUMN]: nilaiCapaian || null,
      [MONTHLY_REPORT_JUMLAH_TEXT_COLUMN]: jumlahHafalan || null,
      nilai_jumlah_hafalan_halaman: null,
      nilai_jumlah_hafalan_juz: null,
      catatan_muhaffiz: catatan || null
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

  let { error } = await sb
    .from(MONTHLY_REPORT_TABLE)
    .upsert(payload, { onConflict: 'periode,guru_id,kelas_id,santri_id' })
  if (error && isMuhaffizMissingCapaianTextColumnError(error)) {
    const payloadFallback = payload.map(item => {
      const clone = { ...item }
      delete clone[MONTHLY_REPORT_CAPAIAN_TEXT_COLUMN]
      return clone
    })
    ;({ error } = await sb
      .from(MONTHLY_REPORT_TABLE)
      .upsert(payloadFallback, { onConflict: 'periode,guru_id,kelas_id,santri_id' }))
  }
  if (error && isMuhaffizMissingJumlahTextColumnError(error)) {
    const payloadFallback = payload.map(item => {
      const clone = { ...item }
      delete clone[MONTHLY_REPORT_JUMLAH_TEXT_COLUMN]
      return clone
    })
    ;({ error } = await sb
      .from(MONTHLY_REPORT_TABLE)
      .upsert(payloadFallback, { onConflict: 'periode,guru_id,kelas_id,santri_id' }))
  }

  if (error) {
    console.error(error)
    alert(`Gagal simpan input massal: ${error.message || 'Unknown error'}`)
    return
  }

  alert(`Input massal berhasil disimpan (${payload.length} santri).`)
  closeMuhaffizBulkInputModal()
  await renderLaporanBulananPage()
}

async function renderLaporanBulananPage() {
  const content = document.getElementById('muhaffiz-content')
  if (!content) return
  content.innerHTML = 'Loading laporan bulanan...'

  const periode = muhaffizState.periode || getMonthInputToday()
  muhaffizState.periode = periode

  const muhaffizId = String(muhaffizState?.profile?.id || '').trim()
  if (!muhaffizId) {
    content.innerHTML = '<div class="placeholder-card">Profil muhaffiz tidak ditemukan. Silakan login ulang.</div>'
    return
  }

  const halaqahRes = await sb.from(HALAQAH_TABLE).select('id').eq('muhaffiz_id', muhaffizId)
  if (halaqahRes.error) {
    console.error(halaqahRes.error)
    content.innerHTML = `<div class="placeholder-card">Gagal load halaqah muhaffiz: ${escapeHtml(halaqahRes.error.message || 'Unknown error')}</div>`
    return
  }

  const halaqahIds = [...new Set((halaqahRes.data || []).map(item => String(item.id || '')).filter(Boolean))]
  let emptyMessage = 'Belum ada data santri aktif.'
  let santriIds = []

  if (!halaqahIds.length) {
    emptyMessage = 'Belum ada halaqah yang ditugaskan ke Anda.'
  } else {
    const halaqahSantriRes = await sb
      .from(HALAQAH_SANTRI_TABLE)
      .select('santri_id')
      .in('halaqah_id', halaqahIds)
    if (halaqahSantriRes.error) {
      console.error(halaqahSantriRes.error)
      content.innerHTML = `<div class="placeholder-card">Gagal load anggota halaqah: ${escapeHtml(halaqahSantriRes.error.message || 'Unknown error')}</div>`
      return
    }
    santriIds = [...new Set((halaqahSantriRes.data || []).map(item => String(item.santri_id || '')).filter(Boolean))]
    if (!santriIds.length) {
      emptyMessage = 'Belum ada santri yang terdaftar pada halaqah Anda.'
    }
  }

  const kelasRes = await sb.from('kelas').select('id, nama_kelas, wali_kelas_id').order('nama_kelas')
  if (kelasRes.error) {
    console.error(kelasRes.error)
    content.innerHTML = `<div class="placeholder-card">Gagal load data kelas: ${escapeHtml(kelasRes.error.message || 'Unknown error')}</div>`
    return
  }
  muhaffizState.kelasMap = new Map((kelasRes.data || []).map(k => [String(k.id), k]))

  if (!santriIds.length) {
    muhaffizState.santriList = []
  } else {
    const santriRes = await sb
      .from('santri')
      .select('id, nama, kelas_id, aktif')
      .in('id', santriIds)
      .eq('aktif', true)
      .order('nama')
    if (santriRes.error) {
      console.error(santriRes.error)
      content.innerHTML = `<div class="placeholder-card">Gagal load data santri halaqah: ${escapeHtml(santriRes.error.message || 'Unknown error')}</div>`
      return
    }
    muhaffizState.santriList = santriRes.data || []
    if (!muhaffizState.santriList.length) {
      emptyMessage = 'Belum ada santri aktif pada halaqah Anda.'
    }
  }

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
      <div style="display:flex; align-items:center; gap:10px; flex-wrap:wrap;">
        <button type="button" class="modal-btn modal-btn-primary" onclick="openMuhaffizBulkInputModal()">Input Massal</button>
        <div style="font-size:13px; color:#475569;">Input ketahfizan di sini akan otomatis tampil di detail laporan bulanan page guru.</div>
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

  let reportRes = await sb
    .from(MONTHLY_REPORT_TABLE)
    .select(`id, muhaffiz, no_hp_muhaffiz, nilai_kehadiran_halaqah, sakit_halaqah, izin_halaqah, nilai_akhlak_halaqah, keterangan_akhlak_halaqah, nilai_ujian_bulanan, keterangan_ujian_bulanan, nilai_target_hafalan, keterangan_target_hafalan, nilai_capaian_hafalan_bulanan, ${MONTHLY_REPORT_CAPAIAN_TEXT_COLUMN}, ${MONTHLY_REPORT_JUMLAH_TEXT_COLUMN}, nilai_jumlah_hafalan_halaman, nilai_jumlah_hafalan_juz, catatan_muhaffiz`)
    .eq('periode', periode)
    .eq('guru_id', waliGuruId)
    .eq('kelas_id', String(santri.kelas_id))
    .eq('santri_id', sid)
    .maybeSingle()
  if (reportRes.error && isMuhaffizMissingCapaianTextColumnError(reportRes.error)) {
    reportRes = await sb
      .from(MONTHLY_REPORT_TABLE)
      .select('id, muhaffiz, no_hp_muhaffiz, nilai_kehadiran_halaqah, sakit_halaqah, izin_halaqah, nilai_akhlak_halaqah, keterangan_akhlak_halaqah, nilai_ujian_bulanan, keterangan_ujian_bulanan, nilai_target_hafalan, keterangan_target_hafalan, nilai_capaian_hafalan_bulanan, nilai_jumlah_hafalan_halaman, nilai_jumlah_hafalan_juz, catatan_muhaffiz')
      .eq('periode', periode)
      .eq('guru_id', waliGuruId)
      .eq('kelas_id', String(santri.kelas_id))
      .eq('santri_id', sid)
      .maybeSingle()
  }
  if (reportRes.error && isMuhaffizMissingJumlahTextColumnError(reportRes.error)) {
    reportRes = await sb
      .from(MONTHLY_REPORT_TABLE)
      .select(`id, muhaffiz, no_hp_muhaffiz, nilai_kehadiran_halaqah, sakit_halaqah, izin_halaqah, nilai_akhlak_halaqah, keterangan_akhlak_halaqah, nilai_ujian_bulanan, keterangan_ujian_bulanan, nilai_target_hafalan, keterangan_target_hafalan, nilai_capaian_hafalan_bulanan, ${MONTHLY_REPORT_CAPAIAN_TEXT_COLUMN}, nilai_jumlah_hafalan_halaman, nilai_jumlah_hafalan_juz, catatan_muhaffiz`)
      .eq('periode', periode)
      .eq('guru_id', waliGuruId)
      .eq('kelas_id', String(santri.kelas_id))
      .eq('santri_id', sid)
      .maybeSingle()
  }
  const { data: reportRow, error } = reportRes

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
  const currentMuhaffizNama = String(muhaffizState?.profile?.nama || '').trim()
  const currentMuhaffizNoHp = String(muhaffizState?.profile?.no_hp || '').trim()
  const muhaffizValue = currentMuhaffizNama || String(reportRow?.muhaffiz || '').trim()
  const muhaffizNoHpValue = currentMuhaffizNoHp || String(reportRow?.no_hp_muhaffiz || '').trim()

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
          <input id="m-lap-muhaffiz" class="guru-field" type="text" value="${escapeHtml(muhaffizValue)}" placeholder="Nama muhaffiz" autocomplete="off">
        </div>
        <div>
          <label class="guru-label">Nomor HP Muhaffiz</label>
          <input id="m-lap-nohp-muhaffiz" class="guru-field" type="text" value="${escapeHtml(muhaffizNoHpValue)}" placeholder="08xxxxxxxxxx" autocomplete="off">
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
                  <input id="m-lap-nilai-target-hafalan" class="guru-field" type="number" min="0" step="0.01" value="${escapeHtml(toInputValue(reportRow?.nilai_target_hafalan))}">
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
                  <input id="m-lap-ket-capaian-hafalan" class="guru-field" type="text" value="${escapeHtml(String(reportRow?.[MONTHLY_REPORT_CAPAIAN_TEXT_COLUMN] || '').trim() || toInputValue(reportRow?.nilai_capaian_hafalan_bulanan))}" placeholder="Contoh: Murojaah 1/2 juz atau 12 halaman">
                </div>
              </td>
              <td style="padding:8px; border:1px solid #e2e8f0; color:#94a3b8;">-</td>
            </tr>
            <tr>
              <td style="padding:8px; border:1px solid #e2e8f0;">Jumlah Hafalan</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <input id="m-lap-ket-jumlah-hafalan" class="guru-field" type="text" value="${escapeHtml(String(reportRow?.[MONTHLY_REPORT_JUMLAH_TEXT_COLUMN] || '').trim() || ((reportRow?.nilai_jumlah_hafalan_halaman !== null && reportRow?.nilai_jumlah_hafalan_halaman !== undefined) || (reportRow?.nilai_jumlah_hafalan_juz !== null && reportRow?.nilai_jumlah_hafalan_juz !== undefined) ? `${toInputValue(reportRow?.nilai_jumlah_hafalan_halaman)} halaman / ${toInputValue(reportRow?.nilai_jumlah_hafalan_juz)} juz` : ''))}" placeholder="Contoh: Murojaah juz 2, tambah 3 halaman">
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
  const currentMuhaffizNama = String(muhaffizState?.profile?.nama || '').trim()
  const currentMuhaffizNoHp = String(muhaffizState?.profile?.no_hp || '').trim()

  const nilaiKehadiranHalaqah = toNullableNumber(document.getElementById('m-lap-nilai-kehadiran-halaqah')?.value)
  const sakitHalaqah = toNullableNumber(document.getElementById('m-lap-sakit-halaqah')?.value)
  const izinHalaqah = toNullableNumber(document.getElementById('m-lap-izin-halaqah')?.value)
  const nilaiAkhlakHalaqahRaw = String(document.getElementById('m-lap-nilai-akhlak-halaqah')?.value || '').trim().toUpperCase()
  const nilaiAkhlakHalaqah = nilaiAkhlakHalaqahRaw ? normalizeAkhlakGrade(nilaiAkhlakHalaqahRaw) : ''
  const nilaiUjianBulanan = toNullableNumber(document.getElementById('m-lap-nilai-ujian-bulanan')?.value)
  const nilaiTargetHafalan = toNullableNumber(document.getElementById('m-lap-nilai-target-hafalan')?.value)
  const capaianHafalanTextRaw = String(document.getElementById('m-lap-ket-capaian-hafalan')?.value || '').trim()
  const jumlahHafalanTextRaw = String(document.getElementById('m-lap-ket-jumlah-hafalan')?.value || '').trim()
  const nilaiCapaianHafalan = toNullableNumber(capaianHafalanTextRaw)

  const numericChecks = [
    [nilaiKehadiranHalaqah, 'Nilai kehadiran halaqah harus angka.'],
    [sakitHalaqah, 'Jumlah sakit halaqah harus angka.'],
    [izinHalaqah, 'Jumlah izin halaqah harus angka.'],
    [nilaiUjianBulanan, 'Nilai ujian bulanan harus angka.'],
    [nilaiTargetHafalan, 'Target hafalan harus angka.']
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
  if (nilaiTargetHafalan !== null && nilaiTargetHafalan < 0) {
    alert('Target hafalan tidak boleh minus.')
    return
  }
  if (nilaiAkhlakHalaqahRaw && !nilaiAkhlakHalaqah) {
    alert('Nilai akhlak halaqah harus A, B, C, D, atau E.')
    return
  }

  if (Number.isNaN(nilaiCapaianHafalan)) {
    // Nilai capaian boleh teks bebas; saat bukan angka, kolom numeric diset null.
  } else if (nilaiCapaianHafalan !== null && nilaiCapaianHafalan < 0) {
    alert('Capaian hafalan tidak boleh minus.')
    return
  }

  const payload = {
    periode: muhaffizState.periode || getMonthInputToday(),
    guru_id: detail.waliGuruId,
    kelas_id: detail.kelasId,
    santri_id: detail.santriId,
    muhaffiz: String(document.getElementById('m-lap-muhaffiz')?.value || '').trim() || currentMuhaffizNama || null,
    no_hp_muhaffiz: String(document.getElementById('m-lap-nohp-muhaffiz')?.value || '').trim() || currentMuhaffizNoHp || null,
    nilai_kehadiran_halaqah: nilaiKehadiranHalaqah === null ? null : nilaiKehadiranHalaqah,
    sakit_halaqah: sakitHalaqah === null ? null : Math.round(sakitHalaqah),
    izin_halaqah: izinHalaqah === null ? null : Math.round(izinHalaqah),
    nilai_akhlak_halaqah: nilaiAkhlakHalaqah || null,
    keterangan_akhlak_halaqah: String(document.getElementById('m-lap-ket-akhlak-halaqah')?.value || '').trim() || null,
    nilai_ujian_bulanan: nilaiUjianBulanan === null ? null : nilaiUjianBulanan,
    keterangan_ujian_bulanan: String(document.getElementById('m-lap-ket-ujian-bulanan')?.value || '').trim() || null,
    nilai_target_hafalan: nilaiTargetHafalan === null ? null : nilaiTargetHafalan,
    keterangan_target_hafalan: String(document.getElementById('m-lap-ket-target-hafalan')?.value || '').trim() || null,
    nilai_capaian_hafalan_bulanan: Number.isNaN(nilaiCapaianHafalan) || nilaiCapaianHafalan === null ? null : nilaiCapaianHafalan,
    [MONTHLY_REPORT_CAPAIAN_TEXT_COLUMN]: capaianHafalanTextRaw || null,
    [MONTHLY_REPORT_JUMLAH_TEXT_COLUMN]: jumlahHafalanTextRaw || null,
    nilai_jumlah_hafalan_halaman: null,
    nilai_jumlah_hafalan_juz: null,
    catatan_muhaffiz: String(document.getElementById('m-lap-catatan-muhaffiz')?.value || '').trim() || null
  }

  let { error } = await sb
    .from(MONTHLY_REPORT_TABLE)
    .upsert(payload, { onConflict: 'periode,guru_id,kelas_id,santri_id' })
  if (error && isMuhaffizMissingCapaianTextColumnError(error)) {
    const fallbackPayload = { ...payload }
    delete fallbackPayload[MONTHLY_REPORT_CAPAIAN_TEXT_COLUMN]
    ;({ error } = await sb
      .from(MONTHLY_REPORT_TABLE)
      .upsert(fallbackPayload, { onConflict: 'periode,guru_id,kelas_id,santri_id' }))
  }
  if (error && isMuhaffizMissingJumlahTextColumnError(error)) {
    const fallbackPayload = { ...payload }
    delete fallbackPayload[MONTHLY_REPORT_JUMLAH_TEXT_COLUMN]
    ;({ error } = await sb
      .from(MONTHLY_REPORT_TABLE)
      .upsert(fallbackPayload, { onConflict: 'periode,guru_id,kelas_id,santri_id' }))
  }
  if (error) {
    console.error(error)
    alert(`Gagal simpan ketahfizan: ${error.message || 'Unknown error'}`)
    return
  }
  alert('Data ketahfizan berhasil disimpan.')
  await openMuhaffizLaporanDetail(detail.santriId)
}

async function renderMuhaffizProfil() {
  const content = document.getElementById('muhaffiz-content')
  if (!content) return

  content.innerHTML = 'Loading profil...'

  let profile
  try {
    profile = await getCurrentMuhaffiz()
  } catch (error) {
    console.error(error)
    content.innerHTML = `<div class="placeholder-card">Gagal load profil: ${escapeHtml(error.message || 'Unknown error')}</div>`
    return
  }

  if (!profile?.id) {
    content.innerHTML = '<div class="placeholder-card">Data profil muhaffiz tidak ditemukan.</div>'
    return
  }

  content.innerHTML = `
    <div style="max-width:580px;">
      <div style="margin-bottom:12px;">
        <label class="guru-label">Foto Profil</label>
        <div style="display:flex; align-items:center; gap:10px; flex-wrap:wrap;">
          <div id="muhaffiz-profil-foto-preview"></div>
          <input id="muhaffiz-profil-foto-url" type="hidden" value="${escapeHtml(String(profile.foto_url || '').trim())}">
          <input id="muhaffiz-profil-foto-file" type="file" accept="image/*" style="display:none;" onchange="uploadMuhaffizProfilePhoto(event)">
          <button type="button" class="modal-btn" onclick="document.getElementById('muhaffiz-profil-foto-file')?.click()">Upload Foto</button>
        </div>
        <div style="font-size:12px; color:#64748b; margin-top:6px;">Maksimal 300 KB.</div>
      </div>
      <div style="margin-bottom:10px;">
        <label class="guru-label">ID Karyawan</label>
        <input id="muhaffiz-profil-id-karyawan" type="text" value="${escapeHtml(profile.id_karyawan || '')}" disabled class="guru-field" autocomplete="off" style="background:#f8fafc; color:#64748b;">
      </div>
      <div style="margin-bottom:10px;">
        <label class="guru-label">Nama</label>
        <input id="muhaffiz-profil-nama" type="text" value="${escapeHtml(profile.nama || '')}" class="guru-field" autocomplete="off">
      </div>
      <div style="margin-bottom:10px;">
        <label class="guru-label">No HP</label>
        <input id="muhaffiz-profil-no-hp" type="text" value="${escapeHtml(profile.no_hp || '')}" class="guru-field" autocomplete="off">
      </div>
      <div style="margin-bottom:10px;">
        <label class="guru-label">Alamat</label>
        <input id="muhaffiz-profil-alamat" type="text" value="${escapeHtml(profile.alamat || '')}" class="guru-field" autocomplete="off">
      </div>
      <div style="margin-bottom:12px;">
        <label class="guru-label">Password</label>
        <div style="position:relative;">
          <input id="muhaffiz-profil-password" type="password" value="${escapeHtml(profile.password || '')}" placeholder="Password" class="guru-field" autocomplete="off" style="padding-right:46px;">
          <button id="muhaffiz-profil-password-toggle" type="button" onclick="toggleMuhaffizProfilePassword()" aria-label="Tampilkan password" title="Tampilkan/Sembunyikan password" style="position:absolute; right:10px; top:50%; transform:translateY(-50%); border:none; background:transparent; cursor:pointer; font-size:16px; line-height:1;">👁</button>
        </div>
      </div>
      <button type="button" class="modal-btn modal-btn-primary" onclick="saveMuhaffizProfil('${escapeHtml(profile.id)}')">Simpan Profil</button>
    </div>
  `
  renderMuhaffizProfilFotoPreview(String(profile.foto_url || '').trim(), String(profile.nama || '').trim())
  const namaInput = document.getElementById('muhaffiz-profil-nama')
  if (namaInput) {
    namaInput.addEventListener('input', () => {
      const fotoUrl = String(document.getElementById('muhaffiz-profil-foto-url')?.value || '').trim()
      renderMuhaffizProfilFotoPreview(fotoUrl, namaInput.value || '')
    })
  }
}

function toggleMuhaffizProfilePassword() {
  const input = document.getElementById('muhaffiz-profil-password')
  const btn = document.getElementById('muhaffiz-profil-password-toggle')
  if (!input || !btn) return
  const willShow = input.type === 'password'
  input.type = willShow ? 'text' : 'password'
  btn.textContent = willShow ? '👁̶' : '👁'
  btn.setAttribute('aria-label', willShow ? 'Sembunyikan password' : 'Tampilkan password')
}

async function saveMuhaffizProfil(muhaffizId) {
  const nama = String(document.getElementById('muhaffiz-profil-nama')?.value || '').trim()
  const noHp = String(document.getElementById('muhaffiz-profil-no-hp')?.value || '').trim()
  const alamat = String(document.getElementById('muhaffiz-profil-alamat')?.value || '').trim()
  const password = String(document.getElementById('muhaffiz-profil-password')?.value || '').trim()
  const fotoUrl = String(document.getElementById('muhaffiz-profil-foto-url')?.value || '').trim()

  if (!nama) {
    alert('Nama wajib diisi.')
    return
  }

  const payload = {
    nama,
    no_hp: noHp || null,
    alamat: alamat || null,
    foto_url: fotoUrl || null
  }
  if (password) payload.password = password

  const { error } = await sb
    .from('karyawan')
    .update(payload)
    .eq('id', muhaffizId)
  if (error) {
    console.error(error)
    alert(`Gagal menyimpan profil: ${error.message || 'Unknown error'}`)
    return
  }

  alert('Profil berhasil disimpan.')
  localStorage.setItem('login_name', nama)
  if (fotoUrl) localStorage.setItem('login_photo_url', fotoUrl)
  else localStorage.removeItem('login_photo_url')
  if (typeof window.setTopbarUserIdentity === 'function') {
    window.setTopbarUserIdentity({ name: nama, foto_url: fotoUrl })
  }
  await setMuhaffizWelcomeName()
  await renderMuhaffizProfil()
}

function getMuhaffizTodayDate() {
  return new Date().toISOString().slice(0, 10)
}

function isMuhaffizEkskulMissingTableError(error) {
  const msg = String(error?.message || '').toLowerCase()
  const code = String(error?.code || '').toUpperCase()
  if (code === '42P01') return true
  if (msg.includes('schema cache') && (
    msg.includes(EKSKUL_TABLE) ||
    msg.includes(EKSKUL_MEMBER_TABLE) ||
    msg.includes(EKSKUL_INDIKATOR_TABLE) ||
    msg.includes(EKSKUL_PROGRES_TABLE) ||
    msg.includes(EKSKUL_MONTHLY_TABLE)
  )) return true
  return msg.includes('does not exist') && (
    msg.includes(EKSKUL_TABLE) ||
    msg.includes(EKSKUL_MEMBER_TABLE) ||
    msg.includes(EKSKUL_INDIKATOR_TABLE) ||
    msg.includes(EKSKUL_PROGRES_TABLE) ||
    msg.includes(EKSKUL_MONTHLY_TABLE)
  )
}

function getMuhaffizMonthPeriodToday() {
  return new Date().toISOString().slice(0, 7)
}

function getMuhaffizEkskulMemberRows() {
  const selected = getMuhaffizSelectedEkskul()
  if (!selected) return []
  return (muhaffizEkskulState.memberRows || []).filter(item => String(item.ekskul_id || '') === String(selected.id || ''))
}

function getMuhaffizEkskulMonthlyPeriode() {
  const fromInput = String(document.getElementById('muhaffiz-ekskul-monthly-periode')?.value || '').trim()
  const stateValue = String(muhaffizEkskulState.monthlyPeriode || '').trim()
  return fromInput || stateValue || getMuhaffizMonthPeriodToday()
}

function setMuhaffizEkskulTab(tabName = 'progres') {
  const tab = String(tabName || '').trim().toLowerCase() === 'laporan' ? 'laporan' : 'progres'
  muhaffizEkskulState.activeTab = tab
  const btnProgres = document.getElementById('muhaffiz-ekskul-tab-btn-progres')
  const btnLaporan = document.getElementById('muhaffiz-ekskul-tab-btn-laporan')
  const panelProgres = document.getElementById('muhaffiz-ekskul-tab-progres')
  const panelLaporan = document.getElementById('muhaffiz-ekskul-tab-laporan')
  if (btnProgres) btnProgres.className = tab === 'progres' ? 'modal-btn modal-btn-primary' : 'modal-btn'
  if (btnLaporan) btnLaporan.className = tab === 'laporan' ? 'modal-btn modal-btn-primary' : 'modal-btn'
  if (panelProgres) panelProgres.style.display = tab === 'progres' ? '' : 'none'
  if (panelLaporan) panelLaporan.style.display = tab === 'laporan' ? '' : 'none'
}

async function loadMuhaffizEkskulMonthlyRows() {
  const selected = getMuhaffizSelectedEkskul()
  const periode = getMuhaffizEkskulMonthlyPeriode()
  muhaffizEkskulState.monthlyPeriode = periode
  if (!selected?.id || !periode) {
    muhaffizEkskulState.monthlyRows = []
    return
  }
  const memberSantriIds = getMuhaffizEkskulMemberRows().map(item => String(item.santri_id || '').trim()).filter(Boolean)
  if (!memberSantriIds.length) {
    muhaffizEkskulState.monthlyRows = []
    return
  }
  const { data, error } = await sb
    .from(EKSKUL_MONTHLY_TABLE)
    .select('id, periode, ekskul_id, santri_id, kehadiran_persen, catatan_pj, updated_at')
    .eq('ekskul_id', String(selected.id))
    .eq('periode', periode)
    .in('santri_id', memberSantriIds)
  if (error) throw error
  muhaffizEkskulState.monthlyRows = data || []
}

function renderMuhaffizEkskulMonthlyRows() {
  const box = document.getElementById('muhaffiz-ekskul-monthly-list')
  if (!box) return
  const selected = getMuhaffizSelectedEkskul()
  if (!selected) {
    box.innerHTML = '<div style="color:#64748b; font-size:12px;">Pilih ekskul terlebih dahulu.</div>'
    return
  }
  const members = getMuhaffizEkskulMemberRows()
  if (!members.length) {
    box.innerHTML = '<div style="color:#64748b; font-size:12px;">Belum ada anggota ekskul.</div>'
    return
  }
  const santriMap = new Map((muhaffizEkskulState.santriRows || []).map(item => [String(item.id || ''), item]))
  const monthlyMap = new Map((muhaffizEkskulState.monthlyRows || []).map(item => [String(item.santri_id || ''), item]))
  box.innerHTML = `
    <div style="overflow:auto;">
      <table style="width:100%; min-width:860px; border-collapse:collapse; font-size:13px;">
        <thead>
          <tr style="background:#f8fafc;">
            <th style="padding:8px; border:1px solid #e2e8f0; width:52px;">No</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Nama Santri</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:170px;">Kehadiran (%)</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Catatan PJ</th>
          </tr>
        </thead>
        <tbody>
          ${members.map((item, idx) => {
            const sid = String(item.santri_id || '')
            const row = monthlyMap.get(sid)
            const kehadiranValue = row?.kehadiran_persen === null || row?.kehadiran_persen === undefined ? '' : String(row.kehadiran_persen)
            const catatanValue = String(row?.catatan_pj || '')
            return `
              <tr data-muhaffiz-ekskul-monthly-row="1" data-santri-id="${escapeHtml(sid)}">
                <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${idx + 1}</td>
                <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(String(santriMap.get(sid)?.nama || '-'))}</td>
                <td style="padding:8px; border:1px solid #e2e8f0;"><input class="guru-field" type="number" min="0" max="100" step="0.01" data-muhaffiz-ekskul-monthly-kehadiran="1" value="${escapeHtml(kehadiranValue)}" placeholder="0-100"></td>
                <td style="padding:8px; border:1px solid #e2e8f0;"><input class="guru-field" type="text" data-muhaffiz-ekskul-monthly-catatan="1" value="${escapeHtml(catatanValue)}" placeholder="Catatan PJ ekskul"></td>
              </tr>
            `
          }).join('')}
        </tbody>
      </table>
    </div>
  `
}

async function onMuhaffizEkskulMonthlyPeriodeChange() {
  try {
    await loadMuhaffizEkskulMonthlyRows()
    renderMuhaffizEkskulMonthlyRows()
  } catch (error) {
    console.error(error)
    alert(`Gagal memuat laporan bulanan ekskul: ${error?.message || 'Unknown error'}`)
  }
}

async function saveMuhaffizEkskulMonthlyReport() {
  const selected = getMuhaffizSelectedEkskul()
  const periode = getMuhaffizEkskulMonthlyPeriode()
  const updatedBy = String(muhaffizState?.profile?.id || '').trim() || null
  if (!selected?.id || !periode) {
    alert('Pilih ekskul dan periode terlebih dahulu.')
    return
  }
  const rowEls = Array.from(document.querySelectorAll('[data-muhaffiz-ekskul-monthly-row="1"]'))
  if (!rowEls.length) {
    alert('Belum ada anggota ekskul untuk diinput.')
    return
  }
  const payload = []
  rowEls.forEach(rowEl => {
    const sid = String(rowEl.getAttribute('data-santri-id') || '').trim()
    if (!sid) return
    const kehadiranRaw = String(rowEl.querySelector('[data-muhaffiz-ekskul-monthly-kehadiran="1"]')?.value || '').trim()
    const catatanRaw = String(rowEl.querySelector('[data-muhaffiz-ekskul-monthly-catatan="1"]')?.value || '').trim()
    const parsed = kehadiranRaw === '' ? null : Number(kehadiranRaw)
    const kehadiran = Number.isFinite(parsed) ? Math.max(0, Math.min(100, Number(parsed.toFixed(2)))) : null
    const catatan = catatanRaw || null
    if (kehadiran === null && !catatan) return
    payload.push({
      periode,
      ekskul_id: String(selected.id),
      santri_id: sid,
      kehadiran_persen: kehadiran,
      catatan_pj: catatan,
      updated_by: updatedBy,
      updated_at: new Date().toISOString()
    })
  })
  if (!payload.length) {
    alert('Isi minimal satu data kehadiran atau catatan.')
    return
  }
  const { error } = await sb
    .from(EKSKUL_MONTHLY_TABLE)
    .upsert(payload, { onConflict: 'periode,ekskul_id,santri_id' })
  if (error) {
    console.error(error)
    alert(`Gagal menyimpan laporan bulanan ekskul: ${error?.message || 'Unknown error'}`)
    return
  }
  await loadMuhaffizEkskulMonthlyRows()
  renderMuhaffizEkskulMonthlyRows()
  alert('Laporan bulanan ekskul berhasil disimpan.')
}

function isMuhaffizEkskulMissingPj2ColumnError(error) {
  const msg = String(error?.message || '').toLowerCase()
  return msg.includes('pj_karyawan_id_2')
}

async function setupMuhaffizEkskulAccess(forceReload = false) {
  const btn = document.getElementById('muhaffiz-nav-ekskul')
  const blockedByHigherPriorityRole = hasBaseRole('guru') || hasBaseRole('musyrif')
  if (blockedByHigherPriorityRole) {
    muhaffizEkskulState.hasAccess = false
    if (btn) btn.style.display = 'none'
    return false
  }
  const profile = muhaffizState.profile || await getCurrentMuhaffiz()
  if (!profile?.id) {
    muhaffizEkskulState.hasAccess = false
    if (btn) btn.style.display = 'none'
    return false
  }
  if (!forceReload && muhaffizEkskulState.hasAccess) {
    if (btn) btn.style.display = ''
    return true
  }
  let accessRes = await sb
    .from(EKSKUL_TABLE)
    .select('id')
    .or(`pj_karyawan_id.eq.${String(profile.id)},pj_karyawan_id_2.eq.${String(profile.id)}`)
    .eq('aktif', true)
    .limit(1)
  if (accessRes.error && isMuhaffizEkskulMissingPj2ColumnError(accessRes.error)) {
    accessRes = await sb
      .from(EKSKUL_TABLE)
      .select('id')
      .eq('pj_karyawan_id', String(profile.id))
      .eq('aktif', true)
      .limit(1)
  }
  const { data, error } = accessRes
  if (error) {
    console.error(error)
    if (btn) btn.style.display = 'none'
    muhaffizEkskulState.hasAccess = false
    return false
  }
  const hasAccess = (data || []).length > 0
  muhaffizEkskulState.hasAccess = hasAccess
  if (btn) btn.style.display = hasAccess ? '' : 'none'
  return hasAccess
}

async function setupMuhaffizPrestasiPelanggaranAccess(_forceReload = false) {
  const btn = document.getElementById('muhaffiz-nav-prestasi-pelanggaran')
  const isAllowed = parseRoleList().some(isWakasekKetahfizanRole)
  if (btn) btn.style.display = isAllowed ? '' : 'none'
  return isAllowed
}

function getMuhaffizPrestasiSantriLabel(item) {
  const kelas = muhaffizPrestasiPelanggaranState.kelasMap.get(String(item?.kelas_id || ''))
  return `${String(item?.nama || '-')} (${String(kelas?.nama_kelas || '-')})`
}

function formatMuhaffizPrestasiDate(value) {
  const text = String(value || '').slice(0, 10)
  if (!text) return '-'
  const date = new Date(`${text}T00:00:00`)
  if (Number.isNaN(date.getTime())) return text
  return date.toLocaleDateString('id-ID', { day: '2-digit', month: 'short', year: 'numeric' })
}

function resetMuhaffizPrestasiForm() {
  muhaffizPrestasiPelanggaranState.editingPrestasiId = ''
  const fields = [
    ['mpp-prestasi-kelas-filter', ''],
    ['mpp-prestasi-santri-search', ''],
    ['mpp-prestasi-waktu', getMuhaffizTodayDate()],
    ['mpp-prestasi-judul', ''],
    ['mpp-prestasi-sertifikat', '']
  ]
  fields.forEach(([id, value]) => {
    const el = document.getElementById(id)
    if (el) el.value = value
  })
  renderMuhaffizPrestasiSantriSearchList('prestasi')
}

function resetMuhaffizPelanggaranForm() {
  muhaffizPrestasiPelanggaranState.editingPelanggaranId = ''
  const fields = [
    ['mpp-pelanggaran-kelas-filter', ''],
    ['mpp-pelanggaran-santri-search', ''],
    ['mpp-pelanggaran-waktu', getMuhaffizTodayDate()],
    ['mpp-pelanggaran-judul', ''],
    ['mpp-pelanggaran-hukuman', ''],
    ['mpp-pelanggaran-surat-jenis', ''],
    ['mpp-pelanggaran-surat-url', '']
  ]
  fields.forEach(([id, value]) => {
    const el = document.getElementById(id)
    if (el) el.value = value
  })
  renderMuhaffizPrestasiSantriSearchList('pelanggaran')
}

function setMuhaffizPrestasiTab(tab) {
  const mode = tab === 'pelanggaran' ? 'pelanggaran' : 'prestasi'
  muhaffizPrestasiPelanggaranState.tab = mode
  const prestasiPanel = document.getElementById('mpp-panel-prestasi')
  const pelanggaranPanel = document.getElementById('mpp-panel-pelanggaran')
  const prestasiList = document.getElementById('mpp-list-prestasi')
  const pelanggaranList = document.getElementById('mpp-list-pelanggaran')
  const btnPrestasi = document.getElementById('mpp-tab-prestasi')
  const btnPelanggaran = document.getElementById('mpp-tab-pelanggaran')
  if (prestasiPanel) prestasiPanel.style.display = mode === 'prestasi' ? 'block' : 'none'
  if (pelanggaranPanel) pelanggaranPanel.style.display = mode === 'pelanggaran' ? 'block' : 'none'
  if (prestasiList) prestasiList.style.display = mode === 'prestasi' ? 'block' : 'none'
  if (pelanggaranList) pelanggaranList.style.display = mode === 'pelanggaran' ? 'block' : 'none'
  if (btnPrestasi) btnPrestasi.classList.toggle('modal-btn-primary', mode === 'prestasi')
  if (btnPelanggaran) btnPelanggaran.classList.toggle('modal-btn-primary', mode === 'pelanggaran')
}

function renderMuhaffizPrestasiSantriSearchList(mode) {
  const isPrestasi = mode === 'prestasi'
  const filterEl = document.getElementById(isPrestasi ? 'mpp-prestasi-kelas-filter' : 'mpp-pelanggaran-kelas-filter')
  const listEl = document.getElementById(isPrestasi ? 'mpp-prestasi-santri-list' : 'mpp-pelanggaran-santri-list')
  if (!filterEl || !listEl) return
  const kelasId = String(filterEl.value || '').trim()
  const rows = (muhaffizPrestasiPelanggaranState.santriRows || []).filter(item => !kelasId || String(item.kelas_id || '') === kelasId)
  listEl.innerHTML = rows.map(item => `<option value="${escapeHtml(getMuhaffizPrestasiSantriLabel(item))}"></option>`).join('')
}

function onMuhaffizPrestasiClassFilterChange(mode) {
  renderMuhaffizPrestasiSantriSearchList(mode)
}

function resolveMuhaffizPrestasiSantriId(mode) {
  const isPrestasi = mode === 'prestasi'
  const searchEl = document.getElementById(isPrestasi ? 'mpp-prestasi-santri-search' : 'mpp-pelanggaran-santri-search')
  const filterEl = document.getElementById(isPrestasi ? 'mpp-prestasi-kelas-filter' : 'mpp-pelanggaran-kelas-filter')
  const text = String(searchEl?.value || '').trim().toLowerCase()
  const kelasId = String(filterEl?.value || '').trim()
  if (!text) return ''
  const rows = (muhaffizPrestasiPelanggaranState.santriRows || []).filter(item => !kelasId || String(item.kelas_id || '') === kelasId)
  const exactLabel = rows.find(item => getMuhaffizPrestasiSantriLabel(item).toLowerCase() === text)
  if (exactLabel?.id) return String(exactLabel.id)
  const exactName = rows.filter(item => String(item.nama || '').trim().toLowerCase() === text)
  if (exactName.length === 1) return String(exactName[0].id || '')
  return ''
}

function openMuhaffizPrestasiDoc(url) {
  const link = String(url || '').trim()
  if (!link) {
    alert('Dokumen belum tersedia.')
    return
  }
  const overlay = document.getElementById('mpp-doc-overlay')
  const frame = document.getElementById('mpp-doc-frame')
  const dl = document.getElementById('mpp-doc-download')
  if (!overlay || !frame || !dl) return
  frame.src = link
  dl.href = link
  overlay.style.display = 'block'
}

function closeMuhaffizPrestasiDoc() {
  const overlay = document.getElementById('mpp-doc-overlay')
  const frame = document.getElementById('mpp-doc-frame')
  if (!overlay || !frame) return
  frame.src = 'about:blank'
  overlay.style.display = 'none'
}

async function uploadMuhaffizPelanggaranSuratFile(event) {
  const file = event?.target?.files?.[0]
  if (!file) return
  try {
    const ext = String(file.name || '').split('.').pop() || 'bin'
    const filePath = `surat/${Date.now()}-${Math.random().toString(36).slice(2, 8)}.${ext}`
    const { error: uploadError } = await sb.storage.from(SANTRI_SURAT_BUCKET).upload(filePath, file, { upsert: true, cacheControl: '3600' })
    if (uploadError) throw uploadError
    const { data } = sb.storage.from(SANTRI_SURAT_BUCKET).getPublicUrl(filePath)
    const input = document.getElementById('mpp-pelanggaran-surat-url')
    if (input) input.value = String(data?.publicUrl || '')
    alert('Upload surat berhasil.')
  } catch (error) {
    console.error(error)
    alert(`Gagal upload surat: ${error?.message || 'Unknown error'}`)
  } finally {
    event.target.value = ''
  }
}

function renderMuhaffizPrestasiLists() {
  const santriMap = new Map((muhaffizPrestasiPelanggaranState.santriRows || []).map(item => [String(item.id || ''), item]))
  const prestasiBox = document.getElementById('mpp-list-prestasi')
  const pelanggaranBox = document.getElementById('mpp-list-pelanggaran')
  if (prestasiBox) {
    const rows = muhaffizPrestasiPelanggaranState.prestasiRows || []
    prestasiBox.innerHTML = rows.length ? `
      <div style="overflow:auto;"><table style="width:100%; min-width:980px; border-collapse:collapse; font-size:12px;"><thead><tr style="background:#f8fafc;"><th style="padding:7px; border:1px solid #e2e8f0;">No</th><th style="padding:7px; border:1px solid #e2e8f0;">Santri</th><th style="padding:7px; border:1px solid #e2e8f0;">Waktu</th><th style="padding:7px; border:1px solid #e2e8f0;">Kategori</th><th style="padding:7px; border:1px solid #e2e8f0;">Prestasi</th><th style="padding:7px; border:1px solid #e2e8f0;">Sertifikat</th><th style="padding:7px; border:1px solid #e2e8f0;">Aksi</th></tr></thead><tbody>
      ${rows.map((row, idx) => `<tr><td style="padding:7px; border:1px solid #e2e8f0; text-align:center;">${idx + 1}</td><td style="padding:7px; border:1px solid #e2e8f0;">${escapeHtml(String(santriMap.get(String(row.santri_id || ''))?.nama || '-'))}</td><td style="padding:7px; border:1px solid #e2e8f0;">${escapeHtml(formatMuhaffizPrestasiDate(row.waktu))}</td><td style="padding:7px; border:1px solid #e2e8f0;">Ketahfizan</td><td style="padding:7px; border:1px solid #e2e8f0;">${escapeHtml(String(row.judul || '-'))}</td><td style="padding:7px; border:1px solid #e2e8f0;"><button type="button" class="modal-btn" onclick="openMuhaffizPrestasiDoc('${escapeHtml(String(row.sertifikat_url || ''))}')">Lihat</button></td><td style="padding:7px; border:1px solid #e2e8f0;"><button type="button" class="modal-btn" onclick="editMuhaffizPrestasiEntry('${escapeHtml(String(row.id || ''))}')">Edit</button><button type="button" class="modal-btn" style="margin-left:6px;" onclick="deleteMuhaffizPrestasiEntry('${escapeHtml(String(row.id || ''))}')">Hapus</button></td></tr>`).join('')}
      </tbody></table></div>
    ` : '<div style="color:#64748b;">Belum ada data prestasi ketahfizan.</div>'
  }
  if (pelanggaranBox) {
    const rows = muhaffizPrestasiPelanggaranState.pelanggaranRows || []
    pelanggaranBox.innerHTML = rows.length ? `
      <div style="overflow:auto;"><table style="width:100%; min-width:1160px; border-collapse:collapse; font-size:12px;"><thead><tr style="background:#f8fafc;"><th style="padding:7px; border:1px solid #e2e8f0;">No</th><th style="padding:7px; border:1px solid #e2e8f0;">Santri</th><th style="padding:7px; border:1px solid #e2e8f0;">Waktu</th><th style="padding:7px; border:1px solid #e2e8f0;">Kategori</th><th style="padding:7px; border:1px solid #e2e8f0;">Pelanggaran</th><th style="padding:7px; border:1px solid #e2e8f0;">Hukuman</th><th style="padding:7px; border:1px solid #e2e8f0;">Surat</th><th style="padding:7px; border:1px solid #e2e8f0;">Dokumen</th><th style="padding:7px; border:1px solid #e2e8f0;">Aksi</th></tr></thead><tbody>
      ${rows.map((row, idx) => `<tr><td style="padding:7px; border:1px solid #e2e8f0; text-align:center;">${idx + 1}</td><td style="padding:7px; border:1px solid #e2e8f0;">${escapeHtml(String(santriMap.get(String(row.santri_id || ''))?.nama || '-'))}</td><td style="padding:7px; border:1px solid #e2e8f0;">${escapeHtml(formatMuhaffizPrestasiDate(row.waktu))}</td><td style="padding:7px; border:1px solid #e2e8f0;">Ketahfizan</td><td style="padding:7px; border:1px solid #e2e8f0;">${escapeHtml(String(row.judul || '-'))}</td><td style="padding:7px; border:1px solid #e2e8f0;">${escapeHtml(String(row.hukuman || '-'))}</td><td style="padding:7px; border:1px solid #e2e8f0;">${escapeHtml(String(row.surat_jenis || '-'))}</td><td style="padding:7px; border:1px solid #e2e8f0;"><button type="button" class="modal-btn" onclick="openMuhaffizPrestasiDoc('${escapeHtml(String(row.surat_url || ''))}')">Lihat</button></td><td style="padding:7px; border:1px solid #e2e8f0;"><button type="button" class="modal-btn" onclick="editMuhaffizPelanggaranEntry('${escapeHtml(String(row.id || ''))}')">Edit</button><button type="button" class="modal-btn" style="margin-left:6px;" onclick="deleteMuhaffizPelanggaranEntry('${escapeHtml(String(row.id || ''))}')">Hapus</button></td></tr>`).join('')}
      </tbody></table></div>
    ` : '<div style="color:#64748b;">Belum ada data pelanggaran ketahfizan.</div>'
  }
}

async function loadMuhaffizPrestasiPelanggaranData() {
  const [santriRes, kelasRes, prestasiRes, pelanggaranRes] = await Promise.all([
    sb.from('santri').select('id, nama, kelas_id, aktif').eq('aktif', true).order('nama'),
    sb.from('kelas').select('id, nama_kelas').order('nama_kelas'),
    sb.from(SANTRI_PRESTASI_TABLE).select('id, santri_id, waktu, kategori, judul, sertifikat_url, created_at').eq('kategori', 'ketahfizan').order('waktu', { ascending: false }).order('created_at', { ascending: false }),
    sb.from(SANTRI_PELANGGARAN_TABLE).select('id, santri_id, waktu, kategori, judul, hukuman, surat_jenis, surat_url, created_at').eq('kategori', 'ketahfizan').order('waktu', { ascending: false }).order('created_at', { ascending: false })
  ])
  const err = santriRes.error || kelasRes.error || prestasiRes.error || pelanggaranRes.error
  if (err) throw err
  muhaffizPrestasiPelanggaranState.santriRows = santriRes.data || []
  muhaffizPrestasiPelanggaranState.kelasMap = new Map((kelasRes.data || []).map(item => [String(item.id || ''), item]))
  muhaffizPrestasiPelanggaranState.prestasiRows = prestasiRes.data || []
  muhaffizPrestasiPelanggaranState.pelanggaranRows = pelanggaranRes.data || []
}

async function saveMuhaffizPrestasiEntry() {
  const santriId = resolveMuhaffizPrestasiSantriId('prestasi')
  const waktu = String(document.getElementById('mpp-prestasi-waktu')?.value || '').trim()
  const judul = String(document.getElementById('mpp-prestasi-judul')?.value || '').trim()
  const sertifikatUrl = String(document.getElementById('mpp-prestasi-sertifikat')?.value || '').trim()
  if (!santriId || !waktu || !judul) {
    alert('Nama santri, waktu, dan prestasi wajib diisi.')
    return
  }
  const payload = { santri_id: santriId, waktu, kategori: 'ketahfizan', judul, sertifikat_url: sertifikatUrl || null }
  const res = muhaffizPrestasiPelanggaranState.editingPrestasiId
    ? await sb.from(SANTRI_PRESTASI_TABLE).update(payload).eq('id', muhaffizPrestasiPelanggaranState.editingPrestasiId)
    : await sb.from(SANTRI_PRESTASI_TABLE).insert([payload])
  if (res.error) {
    alert(`Gagal simpan prestasi: ${res.error.message || 'Unknown error'}`)
    return
  }
  resetMuhaffizPrestasiForm()
  await loadMuhaffizPrestasiPelanggaranData()
  renderMuhaffizPrestasiLists()
}

async function saveMuhaffizPelanggaranEntry() {
  const santriId = resolveMuhaffizPrestasiSantriId('pelanggaran')
  const waktu = String(document.getElementById('mpp-pelanggaran-waktu')?.value || '').trim()
  const judul = String(document.getElementById('mpp-pelanggaran-judul')?.value || '').trim()
  const hukuman = String(document.getElementById('mpp-pelanggaran-hukuman')?.value || '').trim()
  const suratJenis = String(document.getElementById('mpp-pelanggaran-surat-jenis')?.value || '').trim()
  const suratUrl = String(document.getElementById('mpp-pelanggaran-surat-url')?.value || '').trim()
  if (!santriId || !waktu || !judul) {
    alert('Nama santri, waktu, dan pelanggaran wajib diisi.')
    return
  }
  const payload = { santri_id: santriId, waktu, kategori: 'ketahfizan', judul, hukuman: hukuman || null, surat_jenis: suratJenis || null, surat_url: suratUrl || null }
  const res = muhaffizPrestasiPelanggaranState.editingPelanggaranId
    ? await sb.from(SANTRI_PELANGGARAN_TABLE).update(payload).eq('id', muhaffizPrestasiPelanggaranState.editingPelanggaranId)
    : await sb.from(SANTRI_PELANGGARAN_TABLE).insert([payload])
  if (res.error) {
    alert(`Gagal simpan pelanggaran: ${res.error.message || 'Unknown error'}`)
    return
  }
  resetMuhaffizPelanggaranForm()
  await loadMuhaffizPrestasiPelanggaranData()
  renderMuhaffizPrestasiLists()
}

function editMuhaffizPrestasiEntry(id) {
  const row = (muhaffizPrestasiPelanggaranState.prestasiRows || []).find(item => String(item.id || '') === String(id || ''))
  if (!row) return
  const santri = (muhaffizPrestasiPelanggaranState.santriRows || []).find(item => String(item.id || '') === String(row.santri_id || ''))
  muhaffizPrestasiPelanggaranState.editingPrestasiId = String(row.id || '')
  const kelasFilter = document.getElementById('mpp-prestasi-kelas-filter')
  if (kelasFilter) kelasFilter.value = String(santri?.kelas_id || '')
  renderMuhaffizPrestasiSantriSearchList('prestasi')
  const search = document.getElementById('mpp-prestasi-santri-search')
  if (search) search.value = getMuhaffizPrestasiSantriLabel(santri || {})
  document.getElementById('mpp-prestasi-waktu').value = String(row.waktu || '').slice(0, 10)
  document.getElementById('mpp-prestasi-judul').value = String(row.judul || '')
  document.getElementById('mpp-prestasi-sertifikat').value = String(row.sertifikat_url || '')
}

function editMuhaffizPelanggaranEntry(id) {
  const row = (muhaffizPrestasiPelanggaranState.pelanggaranRows || []).find(item => String(item.id || '') === String(id || ''))
  if (!row) return
  const santri = (muhaffizPrestasiPelanggaranState.santriRows || []).find(item => String(item.id || '') === String(row.santri_id || ''))
  muhaffizPrestasiPelanggaranState.editingPelanggaranId = String(row.id || '')
  const kelasFilter = document.getElementById('mpp-pelanggaran-kelas-filter')
  if (kelasFilter) kelasFilter.value = String(santri?.kelas_id || '')
  renderMuhaffizPrestasiSantriSearchList('pelanggaran')
  const search = document.getElementById('mpp-pelanggaran-santri-search')
  if (search) search.value = getMuhaffizPrestasiSantriLabel(santri || {})
  document.getElementById('mpp-pelanggaran-waktu').value = String(row.waktu || '').slice(0, 10)
  document.getElementById('mpp-pelanggaran-judul').value = String(row.judul || '')
  document.getElementById('mpp-pelanggaran-hukuman').value = String(row.hukuman || '')
  document.getElementById('mpp-pelanggaran-surat-jenis').value = String(row.surat_jenis || '')
  document.getElementById('mpp-pelanggaran-surat-url').value = String(row.surat_url || '')
}

async function deleteMuhaffizPrestasiEntry(id) {
  if (!confirm('Hapus data prestasi ini?')) return
  const { error } = await sb.from(SANTRI_PRESTASI_TABLE).delete().eq('id', String(id || ''))
  if (error) {
    alert(`Gagal hapus prestasi: ${error.message || 'Unknown error'}`)
    return
  }
  await loadMuhaffizPrestasiPelanggaranData()
  renderMuhaffizPrestasiLists()
}

async function deleteMuhaffizPelanggaranEntry(id) {
  if (!confirm('Hapus data pelanggaran ini?')) return
  const { error } = await sb.from(SANTRI_PELANGGARAN_TABLE).delete().eq('id', String(id || ''))
  if (error) {
    alert(`Gagal hapus pelanggaran: ${error.message || 'Unknown error'}`)
    return
  }
  await loadMuhaffizPrestasiPelanggaranData()
  renderMuhaffizPrestasiLists()
}

async function renderMuhaffizPrestasiPelanggaranPage() {
  const content = document.getElementById('muhaffiz-content')
  if (!content) return
  content.innerHTML = '<div class="placeholder-card">Loading prestasi & pelanggaran...</div>'
  try {
    await loadMuhaffizPrestasiPelanggaranData()
    content.innerHTML = `
      <div style="display:grid; gap:12px;">
        <div style="display:flex; gap:8px; flex-wrap:wrap;">
          <button id="mpp-tab-prestasi" type="button" class="modal-btn modal-btn-primary" onclick="setMuhaffizPrestasiTab('prestasi')">Prestasi</button>
          <button id="mpp-tab-pelanggaran" type="button" class="modal-btn" onclick="setMuhaffizPrestasiTab('pelanggaran')">Pelanggaran</button>
        </div>
        <div id="mpp-panel-prestasi" style="border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">
          <div style="font-weight:700; margin-bottom:8px;">Input Prestasi Ketahfizan</div>
          <div style="display:grid; grid-template-columns:repeat(auto-fit,minmax(220px,1fr)); gap:10px;">
            <div><label class="guru-label">Filter Kelas</label><select id="mpp-prestasi-kelas-filter" class="guru-field" onchange="onMuhaffizPrestasiClassFilterChange('prestasi')"></select></div>
            <div><label class="guru-label">Nama Santri (Ketik / Cari)</label><input id="mpp-prestasi-santri-search" class="guru-field" type="text" list="mpp-prestasi-santri-list" placeholder="Ketik nama santri..."><datalist id="mpp-prestasi-santri-list"></datalist></div>
            <div><label class="guru-label">Waktu</label><input id="mpp-prestasi-waktu" class="guru-field" type="date" value="${escapeHtml(getMuhaffizTodayDate())}"></div>
            <div><label class="guru-label">Kategori</label><input class="guru-field" type="text" value="Ketahfizan" readonly style="background:#f8fafc;color:#475569;"></div>
            <div style="grid-column:1/-1;"><label class="guru-label">Prestasi</label><input id="mpp-prestasi-judul" class="guru-field" type="text" placeholder="Contoh: Juara Hifdz"></div>
            <div style="grid-column:1/-1;"><label class="guru-label">Sertifikat (URL File)</label><input id="mpp-prestasi-sertifikat" class="guru-field" type="text" placeholder="https://..."></div>
          </div>
          <div style="display:flex; gap:8px; margin-top:10px;"><button type="button" class="modal-btn modal-btn-primary" onclick="saveMuhaffizPrestasiEntry()">Simpan</button><button type="button" class="modal-btn" onclick="resetMuhaffizPrestasiForm()">Reset</button></div>
        </div>
        <div id="mpp-panel-pelanggaran" style="display:none; border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">
          <div style="font-weight:700; margin-bottom:8px;">Input Pelanggaran Ketahfizan</div>
          <div style="display:grid; grid-template-columns:repeat(auto-fit,minmax(220px,1fr)); gap:10px;">
            <div><label class="guru-label">Filter Kelas</label><select id="mpp-pelanggaran-kelas-filter" class="guru-field" onchange="onMuhaffizPrestasiClassFilterChange('pelanggaran')"></select></div>
            <div><label class="guru-label">Nama Santri (Ketik / Cari)</label><input id="mpp-pelanggaran-santri-search" class="guru-field" type="text" list="mpp-pelanggaran-santri-list" placeholder="Ketik nama santri..."><datalist id="mpp-pelanggaran-santri-list"></datalist></div>
            <div><label class="guru-label">Waktu</label><input id="mpp-pelanggaran-waktu" class="guru-field" type="date" value="${escapeHtml(getMuhaffizTodayDate())}"></div>
            <div><label class="guru-label">Kategori</label><input class="guru-field" type="text" value="Ketahfizan" readonly style="background:#f8fafc;color:#475569;"></div>
            <div><label class="guru-label">Pelanggaran</label><input id="mpp-pelanggaran-judul" class="guru-field" type="text" placeholder="Deskripsi pelanggaran"></div>
            <div><label class="guru-label">Jenis Hukuman</label><input id="mpp-pelanggaran-hukuman" class="guru-field" type="text" placeholder="Jenis hukuman"></div>
            <div><label class="guru-label">Surat Peringatan</label><select id="mpp-pelanggaran-surat-jenis" class="guru-field"><option value="">- Pilih -</option><option value="Sanksi dan Teguran">Sanksi dan Teguran</option><option value="ST1">ST1</option><option value="ST2">ST2</option><option value="ST3">ST3</option><option value="SP1">SP1</option><option value="SP2">SP2</option><option value="SP3">SP3</option><option value="DO">DO</option></select></div>
            <div style="grid-column:1/-1;"><label class="guru-label">Surat Pemberitahuan (URL File)</label><input id="mpp-pelanggaran-surat-url" class="guru-field" type="text" placeholder="https://..."><div style="margin-top:8px;"><input id="mpp-pelanggaran-surat-file" type="file" accept=".pdf,.jpg,.jpeg,.png,.webp" style="display:none;" onchange="uploadMuhaffizPelanggaranSuratFile(event)"><button type="button" class="modal-btn" onclick="document.getElementById('mpp-pelanggaran-surat-file')?.click()">Upload File</button></div></div>
          </div>
          <div style="display:flex; gap:8px; margin-top:10px;"><button type="button" class="modal-btn modal-btn-primary" onclick="saveMuhaffizPelanggaranEntry()">Simpan</button><button type="button" class="modal-btn" onclick="resetMuhaffizPelanggaranForm()">Reset</button></div>
        </div>
        <div id="mpp-list-prestasi" style="border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">Loading...</div>
        <div id="mpp-list-pelanggaran" style="display:none; border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">Loading...</div>
      </div>
      <div id="mpp-doc-overlay" style="display:none; position:fixed; inset:0; background:rgba(15,23,42,0.45); z-index:3000; padding:20px;">
        <div style="max-width:960px; margin:0 auto; background:#fff; border:1px solid #e2e8f0; border-radius:12px; overflow:hidden;">
          <div style="display:flex; justify-content:space-between; padding:12px; border-bottom:1px solid #e2e8f0;"><div style="font-weight:700;">Dokumen</div><div style="display:flex; gap:8px;"><a id="mpp-doc-download" href="#" target="_blank" rel="noopener" class="modal-btn">Download</a><button type="button" class="modal-btn" onclick="closeMuhaffizPrestasiDoc()">Tutup</button></div></div>
          <div style="padding:12px;"><iframe id="mpp-doc-frame" title="Dokumen" style="width:100%; height:70vh; border:1px solid #e2e8f0; border-radius:8px;"></iframe></div>
        </div>
      </div>
    `
    const kelasOptions = ['<option value="">Semua Kelas</option>', ...Array.from(muhaffizPrestasiPelanggaranState.kelasMap.values()).map(item => `<option value="${escapeHtml(String(item.id || ''))}">${escapeHtml(String(item.nama_kelas || '-'))}</option>`)]
    const prestasiFilter = document.getElementById('mpp-prestasi-kelas-filter')
    const pelanggaranFilter = document.getElementById('mpp-pelanggaran-kelas-filter')
    if (prestasiFilter) prestasiFilter.innerHTML = kelasOptions.join('')
    if (pelanggaranFilter) pelanggaranFilter.innerHTML = kelasOptions.join('')
    renderMuhaffizPrestasiSantriSearchList('prestasi')
    renderMuhaffizPrestasiSantriSearchList('pelanggaran')
    renderMuhaffizPrestasiLists()
    setMuhaffizPrestasiTab('prestasi')
  } catch (error) {
    console.error(error)
    content.innerHTML = `<div class="placeholder-card">Gagal load prestasi & pelanggaran: ${escapeHtml(error?.message || 'Unknown error')}</div>`
  }
}

function getMuhaffizSelectedEkskul() {
  const eid = String(muhaffizEkskulState.selectedEkskulId || '')
  return (muhaffizEkskulState.ekskulRows || []).find(item => String(item.id || '') === eid) || null
}

function renderMuhaffizEkskulMemberList() {
  const box = document.getElementById('muhaffiz-ekskul-member-list')
  if (!box) return
  const selected = getMuhaffizSelectedEkskul()
  if (!selected) {
    box.innerHTML = '<div style="color:#64748b; font-size:12px;">Pilih ekskul terlebih dahulu.</div>'
    return
  }
  const santriMap = new Map((muhaffizEkskulState.santriRows || []).map(item => [String(item.id || ''), item]))
  const rows = (muhaffizEkskulState.memberRows || []).filter(item => String(item.ekskul_id || '') === String(selected.id || ''))
  if (!rows.length) {
    box.innerHTML = '<div style="color:#64748b; font-size:12px;">Belum ada anggota.</div>'
    return
  }
  box.innerHTML = rows.map((item, idx) => {
    const sid = String(item.santri_id || '')
    const isActive = String(muhaffizEkskulState.selectedSantriId || '') === sid
    return `<div style="display:grid; grid-template-columns:1fr auto; gap:6px; align-items:center; padding:8px; border:1px solid #e2e8f0; border-radius:8px; margin-bottom:6px; background:#fff;"><button type="button" class="modal-btn" onclick="selectMuhaffizEkskulSantri('${escapeHtml(sid)}')" style="display:block; width:100%; text-align:left; font-size:13px; ${isActive ? 'border-color:#d4d456; background:#fefce8; font-weight:700;' : 'font-weight:500;'}">${idx + 1}. ${escapeHtml(String(santriMap.get(sid)?.nama || '-'))}</button><button type="button" class="modal-btn" onclick="openMuhaffizEkskulSantriDetail('${escapeHtml(sid)}')">Detail</button></div>`
  }).join('')
}

function renderMuhaffizEkskulSelects() {
  const selected = getMuhaffizSelectedEkskul()
  const santriSel = document.getElementById('muhaffiz-ekskul-santri')
  if (santriSel) {
    if (!selected) {
      santriSel.innerHTML = '<option value="">Pilih ekskul dulu</option>'
    } else {
      const joined = new Set((muhaffizEkskulState.memberRows || []).filter(item => String(item.ekskul_id || '') === String(selected.id || '')).map(item => String(item.santri_id || '')))
      const options = ['<option value="">Pilih siswa...</option>']
      ;(muhaffizEkskulState.santriRows || []).forEach(item => {
        const sid = String(item.id || '')
        if (!sid || joined.has(sid)) return
        options.push(`<option value="${escapeHtml(sid)}">${escapeHtml(String(item.nama || '-'))}</option>`)
      })
      santriSel.innerHTML = options.join('')
    }
  }
  const indikatorRows = (muhaffizEkskulState.indikatorRows || []).filter(item => String(item.ekskul_id || '') === String(selected?.id || ''))
  const indikatorBox = document.getElementById('muhaffiz-ekskul-indikator-list')
  if (indikatorBox) {
    indikatorBox.innerHTML = indikatorRows.length
      ? indikatorRows.map(item => `<div style="padding:6px 8px; border:1px solid #e2e8f0; border-radius:8px; margin-bottom:6px;"><div style="font-weight:600; font-size:13px;">${escapeHtml(String(item.nama || '-'))}</div><div style="font-size:12px; color:#64748b;">${escapeHtml(String(item.deskripsi || '-'))}</div></div>`).join('')
      : '<div style="color:#64748b; font-size:12px;">Belum ada indikator.</div>'
  }
}

function renderMuhaffizEkskulProgressList() {
  const box = document.getElementById('muhaffiz-ekskul-progres-list')
  if (!box) return
  const eid = String(muhaffizEkskulState.selectedEkskulId || '')
  const sid = String(muhaffizEkskulState.selectedSantriId || '')
  if (!eid || !sid) {
    box.innerHTML = '<div style="color:#64748b; font-size:12px;">Pilih anggota untuk melihat progres.</div>'
    return
  }
  const indikatorMap = new Map((muhaffizEkskulState.indikatorRows || []).map(item => [String(item.id || ''), item]))
  const rows = (muhaffizEkskulState.progressRows || []).filter(item => String(item.ekskul_id || '') === eid && String(item.santri_id || '') === sid)
  if (!rows.length) {
    box.innerHTML = '<div style="color:#64748b; font-size:12px;">Belum ada catatan progres.</div>'
    return
  }
  box.innerHTML = rows.map(item => `<div style="padding:8px; border:1px solid #e2e8f0; border-radius:8px; margin-bottom:6px;"><div style="font-size:12px; color:#64748b;">${escapeHtml(String(item.tanggal || '-'))} | ${escapeHtml(String(indikatorMap.get(String(item.indikator_id || ''))?.nama || 'Umum'))}</div><div style="margin-top:4px;">${escapeHtml(String(item.catatan || '-'))}</div><div style="font-size:12px; color:#64748b;">Nilai: ${escapeHtml(String(item.nilai ?? '-'))}</div></div>`).join('')
}

function muhaffizEkskulUpdateIndicatorStars(inputEl) {
  if (!inputEl) return
  const wrap = inputEl.closest('[data-muhaffiz-ekskul-indikator-row="1"]')
  const starEl = wrap?.querySelector('[data-muhaffiz-ekskul-star-view="1"]')
  if (!starEl) return
  const parsed = Number(String(inputEl.value || '').trim())
  const score = Number.isFinite(parsed) ? Math.max(1, Math.min(100, parsed)) : 0
  const rating = score > 0 ? Math.round((score / 20) * 2) / 2 : 0
  const full = Math.floor(rating)
  const half = rating - full >= 0.5
  let html = ''
  for (let i = 0; i < 5; i += 1) {
    if (i < full) html += '<span style="color:#f59e0b; font-size:17px; line-height:1;">&#9733;</span>'
    else if (i === full && half) html += '<span style="background:linear-gradient(90deg,#f59e0b 50%,#cbd5e1 50%); -webkit-background-clip:text; background-clip:text; color:transparent; -webkit-text-fill-color:transparent; font-size:17px; line-height:1;">&#9733;</span>'
    else html += '<span style="color:#cbd5e1; font-size:17px; line-height:1;">&#9733;</span>'
  }
  starEl.innerHTML = html
}

function renderMuhaffizEkskulProgressInputRows() {
  const box = document.getElementById('muhaffiz-ekskul-progres-input-list')
  if (!box) return
  const selected = getMuhaffizSelectedEkskul()
  const sid = String(muhaffizEkskulState.selectedSantriId || '').trim()
  if (!selected || !sid) {
    box.innerHTML = '<div style="color:#64748b; font-size:12px;">Pilih santri terlebih dahulu.</div>'
    return
  }
  const santriMap = new Map((muhaffizEkskulState.santriRows || []).map(item => [String(item.id || ''), item]))
  const indikatorRows = (muhaffizEkskulState.indikatorRows || []).filter(item => String(item.ekskul_id || '') === String(selected.id || ''))
  if (!indikatorRows.length) {
    box.innerHTML = '<div style="color:#64748b; font-size:12px;">Belum ada indikator.</div>'
    return
  }
  box.innerHTML = `
    <div style="margin-bottom:8px;"><strong>Santri:</strong> ${escapeHtml(String(santriMap.get(sid)?.nama || '-'))}</div>
    <div style="overflow:auto;">
      <table style="width:100%; min-width:900px; border-collapse:collapse; font-size:13px;">
        <thead><tr style="background:#f8fafc;"><th style="padding:8px; border:1px solid #e2e8f0; width:52px;">No</th><th style="padding:8px; border:1px solid #e2e8f0;">Indikator</th><th style="padding:8px; border:1px solid #e2e8f0; width:220px;">Nilai</th><th style="padding:8px; border:1px solid #e2e8f0;">Catatan</th></tr></thead>
        <tbody>
          ${indikatorRows.map((indikator, idx) => `<tr data-muhaffiz-ekskul-indikator-row="1" data-indikator-id="${escapeHtml(String(indikator.id || ''))}"><td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${idx + 1}</td><td style="padding:8px; border:1px solid #e2e8f0;"><div style="font-weight:600;">${escapeHtml(String(indikator.nama || '-'))}</div><div style="font-size:11px; color:#64748b;">${escapeHtml(String(indikator.deskripsi || '-'))}</div></td><td style="padding:8px; border:1px solid #e2e8f0;"><div style="display:grid; grid-template-columns:86px 1fr; gap:8px; align-items:center;"><input class="guru-field" type="number" min="1" max="100" step="1" placeholder="1-100" data-muhaffiz-ekskul-indikator-nilai="1" oninput="muhaffizEkskulUpdateIndicatorStars(this)"><div data-muhaffiz-ekskul-star-view="1">${Array.from({ length: 5 }).map(() => '<span style="color:#cbd5e1; font-size:17px; line-height:1;">&#9733;</span>').join('')}</div></div></td><td style="padding:8px; border:1px solid #e2e8f0;"><input class="guru-field" type="text" placeholder="Catatan indikator" data-muhaffiz-ekskul-indikator-catatan="1"></td></tr>`).join('')}
        </tbody>
      </table>
    </div>
  `
}

function openMuhaffizEkskulSantriDetail(sid) {
  selectMuhaffizEkskulSantri(sid)
  const overlay = document.getElementById('muhaffiz-ekskul-santri-detail-overlay')
  const body = document.getElementById('muhaffiz-ekskul-santri-detail-body')
  if (!overlay || !body) return
  renderMuhaffizEkskulProgressList()
  body.innerHTML = document.getElementById('muhaffiz-ekskul-progres-list')?.innerHTML || '-'
  overlay.style.display = 'block'
}

function closeMuhaffizEkskulSantriDetail() {
  const overlay = document.getElementById('muhaffiz-ekskul-santri-detail-overlay')
  if (overlay) overlay.style.display = 'none'
}

async function selectMuhaffizEkskul(id) {
  muhaffizEkskulState.selectedEkskulId = String(id || '')
  muhaffizEkskulState.selectedSantriId = ''
  renderMuhaffizEkskulMemberList()
  renderMuhaffizEkskulSelects()
  renderMuhaffizEkskulProgressInputRows()
  renderMuhaffizEkskulProgressList()
  try {
    await loadMuhaffizEkskulMonthlyRows()
    renderMuhaffizEkskulMonthlyRows()
  } catch (error) {
    console.error(error)
  }
}

function selectMuhaffizEkskulSantri(sid) {
  muhaffizEkskulState.selectedSantriId = String(sid || '')
  renderMuhaffizEkskulMemberList()
  renderMuhaffizEkskulProgressInputRows()
  renderMuhaffizEkskulProgressList()
}

async function addMuhaffizEkskulMember() {
  const eid = String(muhaffizEkskulState.selectedEkskulId || '')
  const sid = String(document.getElementById('muhaffiz-ekskul-santri')?.value || '').trim()
  if (!eid || !sid) {
    alert('Pilih ekskul dan siswa terlebih dahulu.')
    return
  }
  const { error } = await sb.from(EKSKUL_MEMBER_TABLE).insert([{ ekskul_id: eid, santri_id: sid }])
  if (error) {
    alert(`Gagal tambah anggota: ${error.message || 'Unknown error'}`)
    return
  }
  await renderMuhaffizEkskulPage(true)
}

async function addMuhaffizEkskulIndikator() {
  const eid = String(muhaffizEkskulState.selectedEkskulId || '')
  const nama = String(document.getElementById('muhaffiz-ekskul-indikator-nama')?.value || '').trim()
  const deskripsi = String(document.getElementById('muhaffiz-ekskul-indikator-deskripsi')?.value || '').trim()
  if (!eid || !nama) {
    alert('Pilih ekskul dan isi indikator.')
    return
  }
  const urutan = ((muhaffizEkskulState.indikatorRows || []).filter(item => String(item.ekskul_id || '') === eid).length || 0) + 1
  const { error } = await sb.from(EKSKUL_INDIKATOR_TABLE).insert([{ ekskul_id: eid, nama, deskripsi: deskripsi || null, urutan }])
  if (error) {
    alert(`Gagal tambah indikator: ${error.message || 'Unknown error'}`)
    return
  }
  document.getElementById('muhaffiz-ekskul-indikator-nama').value = ''
  document.getElementById('muhaffiz-ekskul-indikator-deskripsi').value = ''
  await renderMuhaffizEkskulPage(true)
}

async function saveMuhaffizEkskulProgress() {
  const eid = String(muhaffizEkskulState.selectedEkskulId || '')
  const sid = String(muhaffizEkskulState.selectedSantriId || '')
  const tanggal = String(document.getElementById('muhaffiz-ekskul-progres-tanggal')?.value || '').trim()
  if (!eid || !sid || !tanggal) {
    alert('Lengkapi data progres.')
    return
  }
  const rowEls = Array.from(document.querySelectorAll('[data-muhaffiz-ekskul-indikator-row="1"]'))
  if (!rowEls.length) {
    alert('Belum ada indikator untuk diinput.')
    return
  }
  const payload = []
  rowEls.forEach(rowEl => {
    const indikatorId = String(rowEl.getAttribute('data-indikator-id') || '').trim() || null
    const nilaiRaw = String(rowEl.querySelector('[data-muhaffiz-ekskul-indikator-nilai="1"]')?.value || '').trim()
    const nilaiParsed = nilaiRaw ? Number(nilaiRaw) : null
    const nilai = Number.isFinite(nilaiParsed) ? Math.max(1, Math.min(100, Math.round(nilaiParsed))) : null
    const catatan = String(rowEl.querySelector('[data-muhaffiz-ekskul-indikator-catatan="1"]')?.value || '').trim()
    if (!catatan && !Number.isFinite(nilai)) return
    payload.push({
      ekskul_id: eid,
      santri_id: sid,
      indikator_id: indikatorId,
      tanggal,
      nilai: Number.isFinite(nilai) ? nilai : null,
      catatan: catatan || null,
      updated_by: String(muhaffizState.profile?.id || '').trim() || null
    })
  })
  if (!payload.length) {
    alert('Isi minimal satu indikator.')
    return
  }
  const { error } = await sb.from(EKSKUL_PROGRES_TABLE).insert(payload)
  if (error) {
    alert(`Gagal simpan progres: ${error.message || 'Unknown error'}`)
    return
  }
  await renderMuhaffizEkskulPage(true)
}

async function renderMuhaffizEkskulPage(_forceReload = false) {
  const content = document.getElementById('muhaffiz-content')
  if (!content) return
  content.innerHTML = '<div class="placeholder-card">Loading ekskul...</div>'
  try {
    const profile = muhaffizState.profile || await getCurrentMuhaffiz()
    let exRes = await sb.from(EKSKUL_TABLE)
      .select('id, nama, deskripsi')
      .or(`pj_karyawan_id.eq.${String(profile?.id || '')},pj_karyawan_id_2.eq.${String(profile?.id || '')}`)
      .eq('aktif', true)
      .order('nama')
    if (exRes.error && isMuhaffizEkskulMissingPj2ColumnError(exRes.error)) {
      exRes = await sb.from(EKSKUL_TABLE)
        .select('id, nama, deskripsi')
        .eq('pj_karyawan_id', String(profile?.id || ''))
        .eq('aktif', true)
        .order('nama')
    }
    const [memberRes, indikatorRes, santriRes] = await Promise.all([
      sb.from(EKSKUL_MEMBER_TABLE).select('id, ekskul_id, santri_id').order('created_at', { ascending: false }),
      sb.from(EKSKUL_INDIKATOR_TABLE).select('id, ekskul_id, nama, deskripsi, urutan').order('urutan', { ascending: true }),
      sb.from('santri').select('id, nama, aktif').eq('aktif', true).order('nama')
    ])
    if (exRes.error) throw exRes.error
    if (memberRes.error) throw memberRes.error
    if (indikatorRes.error) throw indikatorRes.error
    if (santriRes.error) throw santriRes.error

    muhaffizEkskulState.ekskulRows = exRes.data || []
    const ekskulIds = muhaffizEkskulState.ekskulRows.map(item => String(item.id || ''))
    muhaffizEkskulState.memberRows = (memberRes.data || []).filter(item => ekskulIds.includes(String(item.ekskul_id || '')))
    muhaffizEkskulState.indikatorRows = (indikatorRes.data || []).filter(item => ekskulIds.includes(String(item.ekskul_id || '')))
    muhaffizEkskulState.santriRows = santriRes.data || []
    if (!muhaffizEkskulState.ekskulRows.length) {
      content.innerHTML = '<div class="placeholder-card">Anda belum ditetapkan sebagai PJ ekskul.</div>'
      return
    }
    if (!muhaffizEkskulState.selectedEkskulId || !ekskulIds.includes(String(muhaffizEkskulState.selectedEkskulId || ''))) {
      muhaffizEkskulState.selectedEkskulId = String(muhaffizEkskulState.ekskulRows[0]?.id || '')
      muhaffizEkskulState.selectedSantriId = ''
    }
    const selected = getMuhaffizSelectedEkskul()
    const { data: progressRows, error: progressError } = await sb
      .from(EKSKUL_PROGRES_TABLE)
      .select('id, ekskul_id, santri_id, indikator_id, tanggal, nilai, catatan, created_at')
      .eq('ekskul_id', String(selected?.id || ''))
      .order('tanggal', { ascending: false })
      .order('created_at', { ascending: false })
    if (progressError) throw progressError
    muhaffizEkskulState.progressRows = progressRows || []
    if (!muhaffizEkskulState.monthlyPeriode) muhaffizEkskulState.monthlyPeriode = getMuhaffizMonthPeriodToday()
    await loadMuhaffizEkskulMonthlyRows()

    content.innerHTML = `
      <div style="display:grid; gap:12px;">
        <div style="border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">
          <div style="font-weight:700; margin-bottom:8px;">Ekskul Binaan</div>
          <div style="display:flex; flex-wrap:wrap; gap:8px;">
            ${(muhaffizEkskulState.ekskulRows || []).map(item => `<button type="button" class="modal-btn" onclick="selectMuhaffizEkskul('${escapeHtml(String(item.id || ''))}')" style="${String(muhaffizEkskulState.selectedEkskulId || '') === String(item.id || '') ? 'border-color:#d4d456; background:#fefce8;' : ''}">${escapeHtml(String(item.nama || '-'))}</button>`).join('')}
          </div>
        </div>

        <div style="display:flex; gap:8px; flex-wrap:wrap;">
          <button id="muhaffiz-ekskul-tab-btn-progres" type="button" class="modal-btn modal-btn-primary" onclick="setMuhaffizEkskulTab('progres')">Input Progres</button>
          <button id="muhaffiz-ekskul-tab-btn-laporan" type="button" class="modal-btn" onclick="setMuhaffizEkskulTab('laporan')">Laporan Bulanan Ekskul</button>
        </div>

        <div id="muhaffiz-ekskul-tab-progres" style="display:grid; gap:12px;">
          <div style="display:grid; grid-template-columns:repeat(auto-fit, minmax(280px, 1fr)); gap:12px;">
            <div style="border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">
              <div style="font-weight:700; margin-bottom:8px;">Anggota Ekskul</div>
              <div style="display:grid; grid-template-columns:1fr auto; gap:8px; margin-bottom:8px;">
                <select id="muhaffiz-ekskul-santri" class="guru-field"></select>
                <button type="button" class="modal-btn modal-btn-primary" onclick="addMuhaffizEkskulMember()">Tambah</button>
              </div>
              <div id="muhaffiz-ekskul-member-list">Loading...</div>
            </div>

            <div style="border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">
              <div style="font-weight:700; margin-bottom:8px;">Indikator Penilaian</div>
              <input id="muhaffiz-ekskul-indikator-nama" class="guru-field" type="text" placeholder="Nama indikator">
              <input id="muhaffiz-ekskul-indikator-deskripsi" class="guru-field" type="text" placeholder="Deskripsi indikator" style="margin-top:6px;">
              <button type="button" class="modal-btn modal-btn-primary" onclick="addMuhaffizEkskulIndikator()" style="margin-top:8px;">Tambah Indikator</button>
              <div id="muhaffiz-ekskul-indikator-list" style="margin-top:8px;">Loading...</div>
            </div>
          </div>

          <div style="border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">
            <div style="font-weight:700; margin-bottom:8px;">Progres Santri</div>
            <div style="display:grid; grid-template-columns:160px 1fr auto; gap:8px; margin-bottom:8px;">
              <input id="muhaffiz-ekskul-progres-tanggal" class="guru-field" type="date" value="${escapeHtml(getMuhaffizTodayDate())}">
              <button type="button" class="modal-btn modal-btn-primary" onclick="saveMuhaffizEkskulProgress()">Simpan</button>
            </div>
            <div id="muhaffiz-ekskul-progres-input-list" style="margin-bottom:10px;">Loading...</div>
            <div id="muhaffiz-ekskul-progres-list" style="margin-top:10px;">Loading...</div>
          </div>
        </div>

        <div id="muhaffiz-ekskul-tab-laporan" style="display:none; border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">
          <div style="font-weight:700; margin-bottom:8px;">Input Laporan Bulanan Ekskul</div>
          <div style="font-size:12px; color:#64748b; margin-bottom:8px;">Data ini akan dipakai di Detail Laporan Bulanan pada page guru (bagian D. Ekstrakulikuler).</div>
          <div style="display:grid; grid-template-columns:180px 1fr auto; gap:8px; margin-bottom:8px; align-items:end;">
            <div>
              <label class="guru-label">Periode Bulan</label>
              <input id="muhaffiz-ekskul-monthly-periode" class="guru-field" type="month" value="${escapeHtml(String(muhaffizEkskulState.monthlyPeriode || getMuhaffizMonthPeriodToday()))}" onchange="onMuhaffizEkskulMonthlyPeriodeChange()">
            </div>
            <div></div>
            <div><button type="button" class="modal-btn modal-btn-primary" onclick="saveMuhaffizEkskulMonthlyReport()">Simpan Laporan Bulanan</button></div>
          </div>
          <div id="muhaffiz-ekskul-monthly-list">Loading...</div>
        </div>
      </div>
      <div id="muhaffiz-ekskul-santri-detail-overlay" style="display:none; position:fixed; inset:0; background:rgba(15,23,42,0.45); z-index:2000; padding:20px;">
        <div style="max-width:900px; margin:0 auto; background:#fff; border-radius:12px; border:1px solid #e2e8f0; overflow:hidden;">
          <div style="display:flex; align-items:center; justify-content:space-between; padding:12px 14px; border-bottom:1px solid #e2e8f0;">
            <div style="font-weight:700; color:#0f172a;">Detail Progres Santri</div>
            <button type="button" class="modal-btn" onclick="closeMuhaffizEkskulSantriDetail()">Tutup</button>
          </div>
          <div id="muhaffiz-ekskul-santri-detail-body" style="padding:12px; max-height:70vh; overflow:auto;">Loading...</div>
        </div>
      </div>
    `
    renderMuhaffizEkskulMemberList()
    renderMuhaffizEkskulSelects()
    renderMuhaffizEkskulProgressInputRows()
    renderMuhaffizEkskulProgressList()
    renderMuhaffizEkskulMonthlyRows()
    setMuhaffizEkskulTab(muhaffizEkskulState.activeTab || 'progres')
  } catch (error) {
    console.error(error)
    if (isMuhaffizEkskulMissingTableError(error)) {
      content.innerHTML = '<div class="placeholder-card" style="white-space:pre-wrap;">Tabel ekskul belum ada. Hubungi admin untuk setup tabel ekskul.</div>'
      return
    }
    content.innerHTML = `<div class="placeholder-card">Gagal load ekskul: ${escapeHtml(error?.message || 'Unknown error')}</div>`
  }
}

function getMuhaffizManageSelectedHalaqah() {
  const sid = String(muhaffizManageHalaqahState.selectedHalaqahId || '')
  return (muhaffizManageHalaqahState.halaqahRows || []).find(item => String(item?.id || '') === sid) || null
}

function sortMuhaffizManageSantriRows(rows = []) {
  return [...rows].sort((a, b) => {
    const kelasA = String(muhaffizManageHalaqahState.kelasMap.get(String(a?.kelas_id || ''))?.nama_kelas || '')
    const kelasB = String(muhaffizManageHalaqahState.kelasMap.get(String(b?.kelas_id || ''))?.nama_kelas || '')
    const byKelas = kelasA.localeCompare(kelasB, 'id')
    if (byKelas !== 0) return byKelas
    return String(a?.nama || '').localeCompare(String(b?.nama || ''), 'id')
  })
}

function renderMuhaffizManageSelectedRowsHtml(rows = []) {
  return rows.map(item => {
    const sid = String(item?.id || '')
    const kelasNama = muhaffizManageHalaqahState.kelasMap.get(String(item?.kelas_id || ''))?.nama_kelas || '-'
    return `
      <div class="ekskul-card-item" style="display:grid; grid-template-columns:1fr auto; gap:8px; align-items:flex-start; margin-bottom:8px; font-size:13px;">
        <span>
          <span style="font-weight:600;">${escapeHtml(item?.nama || '-')}</span>
          <span style="display:block; font-size:11px; color:#64748b;">${escapeHtml(kelasNama)}</span>
        </span>
        <button type="button" class="modal-btn modal-btn-danger" style="padding:2px 8px; min-width:auto; line-height:1; border-radius:999px;" onclick="removeMuhaffizManageHalaqahSantri('${escapeHtml(sid)}')" title="Keluarkan dari halaqah ini">x</button>
      </div>
    `
  }).join('')
}

function renderMuhaffizManageAvailableRowsHtml(rows = []) {
  return rows.map(item => {
    const sid = String(item?.id || '')
    const kelasNama = muhaffizManageHalaqahState.kelasMap.get(String(item?.kelas_id || ''))?.nama_kelas || '-'
    return `
      <label class="ekskul-card-item" style="display:block; margin-bottom:8px; font-size:13px;">
        <input type="checkbox" onchange="toggleMuhaffizManageHalaqahSantri('${escapeHtml(sid)}', this.checked)">
        <span style="margin-left:6px; font-weight:600;">${escapeHtml(item?.nama || '-')}</span>
        <span style="display:block; margin-left:22px; font-size:11px; color:#64748b;">${escapeHtml(kelasNama)}</span>
      </label>
    `
  }).join('')
}

function renderMuhaffizManageHalaqahPanels() {
  const selectedWrap = document.getElementById('muhaffiz-halaqah-selected-list')
  const availableWrap = document.getElementById('muhaffiz-halaqah-available-list')
  if (!selectedWrap || !availableWrap) return

  const selectedSet = muhaffizManageHalaqahState.selectedSet || new Set()
  const blockedSet = muhaffizManageHalaqahState.blockedSet || new Set()
  const search = String(muhaffizManageHalaqahState.search || '').trim().toLowerCase()

  const selectedRows = sortMuhaffizManageSantriRows(
    (muhaffizManageHalaqahState.santriRows || []).filter(item => selectedSet.has(String(item?.id || '')))
  )
  selectedWrap.innerHTML = renderMuhaffizManageSelectedRowsHtml(selectedRows) || '<div style="font-size:12px; color:#64748b;">Belum ada santri di halaqah ini.</div>'

  const availableRows = sortMuhaffizManageSantriRows(
    (muhaffizManageHalaqahState.santriRows || []).filter(item => {
      const sid = String(item?.id || '')
      if (!sid) return false
      if (selectedSet.has(sid)) return false
      if (blockedSet.has(sid)) return false
      if (!search) return true
      return String(item?.nama || '').toLowerCase().includes(search)
    })
  )
  availableWrap.innerHTML = renderMuhaffizManageAvailableRowsHtml(availableRows) || '<div style="font-size:12px; color:#64748b;">Tidak ada santri tersedia.</div>'
}

function selectMuhaffizManageHalaqah(halaqahId) {
  const sid = String(halaqahId || '')
  if (!sid) return
  const memberRows = muhaffizManageHalaqahState.memberRows || []
  muhaffizManageHalaqahState.selectedHalaqahId = sid
  muhaffizManageHalaqahState.selectedSet = new Set(
    memberRows
      .filter(item => String(item?.halaqah_id || '') === sid)
      .map(item => String(item?.santri_id || ''))
      .filter(Boolean)
  )
  muhaffizManageHalaqahState.blockedSet = new Set(
    memberRows
      .filter(item => String(item?.halaqah_id || '') !== sid)
      .map(item => String(item?.santri_id || ''))
      .filter(Boolean)
  )
  muhaffizManageHalaqahState.search = ''
  renderMuhaffizManageHalaqahPage()
}

function searchMuhaffizManageHalaqahSantri(keyword) {
  muhaffizManageHalaqahState.search = String(keyword || '')
  renderMuhaffizManageHalaqahPanels()
}

function toggleMuhaffizManageHalaqahSantri(santriId, checked) {
  const sid = String(santriId || '').trim()
  if (!sid) return
  if (!(muhaffizManageHalaqahState.selectedSet instanceof Set)) muhaffizManageHalaqahState.selectedSet = new Set()
  if (checked) muhaffizManageHalaqahState.selectedSet.add(sid)
  else muhaffizManageHalaqahState.selectedSet.delete(sid)
  renderMuhaffizManageHalaqahPanels()
}

function removeMuhaffizManageHalaqahSantri(santriId) {
  const sid = String(santriId || '').trim()
  if (!sid) return
  if (!(muhaffizManageHalaqahState.selectedSet instanceof Set)) muhaffizManageHalaqahState.selectedSet = new Set()
  muhaffizManageHalaqahState.selectedSet.delete(sid)
  renderMuhaffizManageHalaqahPanels()
}

async function saveMuhaffizManageHalaqahName() {
  const selected = getMuhaffizManageSelectedHalaqah()
  const profileId = String(muhaffizState?.profile?.id || '').trim()
  if (!selected || !profileId) return
  const nama = String(document.getElementById('muhaffiz-halaqah-name-input')?.value || '').trim()
  if (!nama) {
    alert('Nama halaqah wajib diisi.')
    return
  }
  const { error } = await sb
    .from(HALAQAH_TABLE)
    .update({ nama })
    .eq('id', String(selected.id))
    .eq('muhaffiz_id', profileId)
  if (error) {
    console.error(error)
    alert(`Gagal menyimpan nama halaqah: ${error?.message || 'Unknown error'}`)
    return
  }
  await renderMuhaffizManageHalaqahPage()
}

async function saveMuhaffizManageHalaqahMembers() {
  const selected = getMuhaffizManageSelectedHalaqah()
  if (!selected) return
  const halaqahId = String(selected.id || '')
  const selectedSantriIds = [...(muhaffizManageHalaqahState.selectedSet || new Set())]
    .map(item => String(item || '').trim())
    .filter(Boolean)

  const { error: deleteError } = await sb
    .from(HALAQAH_SANTRI_TABLE)
    .delete()
    .eq('halaqah_id', halaqahId)
  if (deleteError) {
    console.error(deleteError)
    alert(`Gagal reset anggota halaqah: ${deleteError?.message || 'Unknown error'}`)
    return
  }

  if (selectedSantriIds.length) {
    const payload = selectedSantriIds.map(santriId => ({ halaqah_id: halaqahId, santri_id: santriId }))
    const { error: insertError } = await sb.from(HALAQAH_SANTRI_TABLE).insert(payload)
    if (insertError) {
      console.error(insertError)
      alert(`Gagal menyimpan anggota halaqah: ${insertError?.message || 'Unknown error'}`)
      return
    }
  }

  await renderMuhaffizManageHalaqahPage()
}

async function renderMuhaffizManageHalaqahPage() {
  const content = document.getElementById('muhaffiz-content')
  if (!content) return
  const profileId = String(muhaffizState?.profile?.id || '').trim()
  if (!profileId) {
    content.innerHTML = '<div class="placeholder-card">Profil muhaffiz tidak ditemukan.</div>'
    return
  }

  content.innerHTML = '<div class="placeholder-card">Memuat data halaqah...</div>'
  try {
    const [halaqahRes, memberRes, santriRes, kelasRes] = await Promise.all([
      sb.from(HALAQAH_TABLE).select('id, nama, muhaffiz_id').eq('muhaffiz_id', profileId).order('nama'),
      sb.from(HALAQAH_SANTRI_TABLE).select('halaqah_id, santri_id'),
      sb.from('santri').select('id, nama, kelas_id, aktif').eq('aktif', true).order('nama'),
      sb.from('kelas').select('id, nama_kelas').order('nama_kelas')
    ])
    if (halaqahRes.error) throw halaqahRes.error
    if (memberRes.error) throw memberRes.error
    if (santriRes.error) throw santriRes.error
    if (kelasRes.error) throw kelasRes.error

    const halaqahRows = halaqahRes.data || []
    const memberRows = memberRes.data || []
    const santriRows = santriRes.data || []
    const kelasMap = new Map((kelasRes.data || []).map(item => [String(item?.id || ''), item]))

    muhaffizManageHalaqahState.halaqahRows = halaqahRows
    muhaffizManageHalaqahState.memberRows = memberRows
    muhaffizManageHalaqahState.santriRows = santriRows
    muhaffizManageHalaqahState.kelasMap = kelasMap

    const halaqahIds = halaqahRows.map(item => String(item?.id || ''))
    if (!halaqahIds.length) {
      muhaffizManageHalaqahState.selectedHalaqahId = ''
      muhaffizManageHalaqahState.selectedSet = new Set()
      muhaffizManageHalaqahState.blockedSet = new Set()
      content.innerHTML = '<div class="placeholder-card">Belum ada halaqah yang ditugaskan untuk Anda.</div>'
      return
    }

    const selectedId = halaqahIds.includes(String(muhaffizManageHalaqahState.selectedHalaqahId || ''))
      ? String(muhaffizManageHalaqahState.selectedHalaqahId || '')
      : halaqahIds[0]
    muhaffizManageHalaqahState.selectedHalaqahId = selectedId
    muhaffizManageHalaqahState.selectedSet = new Set(
      memberRows
        .filter(item => String(item?.halaqah_id || '') === selectedId)
        .map(item => String(item?.santri_id || ''))
        .filter(Boolean)
    )
    muhaffizManageHalaqahState.blockedSet = new Set(
      memberRows
        .filter(item => String(item?.halaqah_id || '') !== selectedId)
        .map(item => String(item?.santri_id || ''))
        .filter(Boolean)
    )

    const selected = getMuhaffizManageSelectedHalaqah()
    content.innerHTML = `
      <div style="display:grid; gap:12px;">
        <div style="border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">
          <div style="font-weight:700; margin-bottom:8px;">Halaqah Binaan</div>
          <div style="display:flex; gap:8px; flex-wrap:wrap;">
            ${(halaqahRows || []).map(item => `<button type="button" class="modal-btn" onclick="selectMuhaffizManageHalaqah('${escapeHtml(String(item?.id || ''))}')" style="${String(item?.id || '') === selectedId ? 'border-color:#d4d456; background:#fefce8;' : ''}">${escapeHtml(item?.nama || '-')}</button>`).join('')}
          </div>
        </div>
        <div style="border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">
          <div style="display:grid; grid-template-columns:1fr auto; gap:8px;">
            <input id="muhaffiz-halaqah-name-input" class="guru-field" type="text" value="${escapeHtml(String(selected?.nama || ''))}" placeholder="Nama halaqah">
            <button type="button" class="modal-btn modal-btn-primary" onclick="saveMuhaffizManageHalaqahName()">Simpan Nama</button>
          </div>
        </div>
        <div style="display:grid; grid-template-columns:repeat(auto-fit, minmax(280px, 1fr)); gap:12px;">
          <div style="border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">
            <div style="font-weight:700; margin-bottom:8px;">Santri di Halaqah Ini</div>
            <div id="muhaffiz-halaqah-selected-list"></div>
          </div>
          <div style="border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">
            <div style="display:grid; grid-template-columns:1fr auto; gap:8px; margin-bottom:8px;">
              <input id="muhaffiz-halaqah-search-input" class="guru-field" type="text" value="${escapeHtml(muhaffizManageHalaqahState.search || '')}" placeholder="Cari nama santri..." oninput="searchMuhaffizManageHalaqahSantri(this.value)">
              <button type="button" class="modal-btn modal-btn-secondary" onclick="renderMuhaffizManageHalaqahPanels()">Sort Per Kelas</button>
            </div>
            <div id="muhaffiz-halaqah-available-list" style="max-height:340px; overflow:auto;"></div>
          </div>
        </div>
        <div style="display:flex; justify-content:flex-end;">
          <button type="button" class="modal-btn modal-btn-primary" onclick="saveMuhaffizManageHalaqahMembers()">Simpan Anggota</button>
        </div>
      </div>
    `
    renderMuhaffizManageHalaqahPanels()
  } catch (error) {
    console.error(error)
    content.innerHTML = `<div class="placeholder-card">Gagal load data halaqah: ${escapeHtml(error?.message || 'Unknown error')}</div>`
  }
}

async function loadMuhaffizPage(page) {
  const targetPage = PAGE_TITLES[page] ? page : 'dashboard'
  if (targetPage !== 'chat' && window.ChatModule && typeof window.ChatModule.stop === 'function') {
    window.ChatModule.stop()
  }
  const isEkskulPj = await setupMuhaffizEkskulAccess()
  const isWakasekPrestasi = await setupMuhaffizPrestasiPelanggaranAccess()
  setTopbarTitle(targetPage)
  setNavActive(targetPage === 'profil' ? '' : targetPage)
  if (targetPage !== 'profil') localStorage.setItem(MUHAFFIZ_LAST_PAGE_KEY, targetPage)
  closeTopbarUserMenu()
  if (targetPage === 'profil') {
    await renderMuhaffizProfil()
    return
  }
  if (targetPage === 'laporan-bulanan') {
    await renderLaporanBulananPage()
    return
  }
  if (targetPage === 'data-halaqah') {
    await renderMuhaffizManageHalaqahPage()
    return
  }
  if (targetPage === 'chat') {
    await renderMuhaffizChatPage()
    return
  }
  if (targetPage === 'ekskul') {
    if (!isEkskulPj) {
      const content = document.getElementById('muhaffiz-content')
      if (content) content.innerHTML = '<div class="placeholder-card">Menu ekskul hanya untuk PJ ekskul.</div>'
      return
    }
    await renderMuhaffizEkskulPage()
    return
  }
  if (targetPage === 'prestasi-pelanggaran') {
    if (!isWakasekPrestasi) {
      const content = document.getElementById('muhaffiz-content')
      if (content) content.innerHTML = '<div class="placeholder-card">Menu ini hanya dapat diakses oleh wakasek ketahfizan.</div>'
      return
    }
    await renderMuhaffizPrestasiPelanggaranPage()
    return
  }
  await renderDashboard()
}

async function renderMuhaffizChatPage() {
  const content = document.getElementById('muhaffiz-content')
  if (!content) return
  content.innerHTML = 'Loading chat...'
  try {
    const profile = await getCurrentMuhaffiz()
    if (!profile?.id) {
      content.innerHTML = '<div class="placeholder-card">Data profil muhaffiz tidak ditemukan.</div>'
      return
    }
    if (!window.ChatModule || typeof window.ChatModule.render !== 'function') {
      content.innerHTML = '<div class="placeholder-card">Modul chat belum termuat. Refresh halaman.</div>'
      return
    }
    await window.ChatModule.render({
      sb,
      containerId: 'muhaffiz-content',
      currentUser: { id: String(profile.id), nama: String(profile.nama || profile.id_karyawan || '-') }
    })
  } catch (error) {
    console.error(error)
    content.innerHTML = `<div class="placeholder-card">Gagal load chat: ${escapeHtml(error?.message || 'Unknown error')}</div>`
  }
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
window.openMuhaffizBulkInputModal = openMuhaffizBulkInputModal
window.saveMuhaffizBulkInput = saveMuhaffizBulkInput
window.downloadMuhaffizBulkTemplate = downloadMuhaffizBulkTemplate
window.onMuhaffizBulkExcelFileChange = onMuhaffizBulkExcelFileChange
window.applyMuhaffizBulkExcelToTable = applyMuhaffizBulkExcelToTable
window.closeMuhaffizBulkInputModal = closeMuhaffizBulkInputModal
window.openMuhaffizLaporanDetail = openMuhaffizLaporanDetail
window.saveMuhaffizLaporanDetail = saveMuhaffizLaporanDetail
window.onMuhaffizAkhlakChange = onMuhaffizAkhlakChange
window.onMuhaffizUjianChange = onMuhaffizUjianChange
window.toggleTopbarUserMenu = toggleTopbarUserMenu
window.setMuhaffizNotifRangeFilter = setMuhaffizNotifRangeFilter
window.markAllMuhaffizNotifRead = markAllMuhaffizNotifRead
window.markMuhaffizNotifItemRead = markMuhaffizNotifItemRead
window.openTopbarCalendarPopup = openTopbarCalendarPopup
window.closeTopbarCalendarPopup = closeTopbarCalendarPopup
window.shiftTopbarCalendarMonth = shiftTopbarCalendarMonth
window.selectTopbarCalendarDate = selectTopbarCalendarDate
window.openMuhaffizDashboardAgendaPopup = openMuhaffizDashboardAgendaPopup
window.closeMuhaffizDashboardAgendaPopup = closeMuhaffizDashboardAgendaPopup
window.openMuhaffizProfile = () => loadMuhaffizPage('profil')
window.saveMuhaffizProfil = saveMuhaffizProfil
window.toggleMuhaffizProfilePassword = toggleMuhaffizProfilePassword
window.uploadMuhaffizProfilePhoto = uploadMuhaffizProfilePhoto
window.selectMuhaffizManageHalaqah = selectMuhaffizManageHalaqah
window.searchMuhaffizManageHalaqahSantri = searchMuhaffizManageHalaqahSantri
window.toggleMuhaffizManageHalaqahSantri = toggleMuhaffizManageHalaqahSantri
window.removeMuhaffizManageHalaqahSantri = removeMuhaffizManageHalaqahSantri
window.saveMuhaffizManageHalaqahName = saveMuhaffizManageHalaqahName
window.saveMuhaffizManageHalaqahMembers = saveMuhaffizManageHalaqahMembers
window.renderMuhaffizManageHalaqahPanels = renderMuhaffizManageHalaqahPanels
window.selectMuhaffizEkskul = selectMuhaffizEkskul
window.selectMuhaffizEkskulSantri = selectMuhaffizEkskulSantri
window.openMuhaffizEkskulSantriDetail = openMuhaffizEkskulSantriDetail
window.closeMuhaffizEkskulSantriDetail = closeMuhaffizEkskulSantriDetail
window.muhaffizEkskulUpdateIndicatorStars = muhaffizEkskulUpdateIndicatorStars
window.addMuhaffizEkskulMember = addMuhaffizEkskulMember
window.addMuhaffizEkskulIndikator = addMuhaffizEkskulIndikator
window.saveMuhaffizEkskulProgress = saveMuhaffizEkskulProgress
window.setMuhaffizEkskulTab = setMuhaffizEkskulTab
window.onMuhaffizEkskulMonthlyPeriodeChange = onMuhaffizEkskulMonthlyPeriodeChange
window.saveMuhaffizEkskulMonthlyReport = saveMuhaffizEkskulMonthlyReport
window.setMuhaffizPrestasiTab = setMuhaffizPrestasiTab
window.onMuhaffizPrestasiClassFilterChange = onMuhaffizPrestasiClassFilterChange
window.saveMuhaffizPrestasiEntry = saveMuhaffizPrestasiEntry
window.saveMuhaffizPelanggaranEntry = saveMuhaffizPelanggaranEntry
window.resetMuhaffizPrestasiForm = resetMuhaffizPrestasiForm
window.resetMuhaffizPelanggaranForm = resetMuhaffizPelanggaranForm
window.editMuhaffizPrestasiEntry = editMuhaffizPrestasiEntry
window.editMuhaffizPelanggaranEntry = editMuhaffizPelanggaranEntry
window.deleteMuhaffizPrestasiEntry = deleteMuhaffizPrestasiEntry
window.deleteMuhaffizPelanggaranEntry = deleteMuhaffizPelanggaranEntry
window.openMuhaffizPrestasiDoc = openMuhaffizPrestasiDoc
window.closeMuhaffizPrestasiDoc = closeMuhaffizPrestasiDoc
window.uploadMuhaffizPelanggaranSuratFile = uploadMuhaffizPelanggaranSuratFile
window.logout = logout

document.addEventListener('DOMContentLoaded', async () => {
  setupCustomPopupSystem()
  loadMuhaffizNotifPrefs()
  ensureTopbarNotification()

  const loginId = String(localStorage.getItem('login_id') || '').trim()
  if (!loginId) {
    location.href = 'index.html'
    return
  }

  try {
    await setMuhaffizWelcomeName()
    await setupMuhaffizEkskulAccess(true)
    await setupMuhaffizPrestasiPelanggaranAccess(true)
    if (!muhaffizState.profile) {
      location.href = 'index.html'
      return
    }
  } catch (error) {
    console.error(error)
    location.href = 'index.html'
    return
  }

  const lastPage = localStorage.getItem(MUHAFFIZ_LAST_PAGE_KEY) || 'dashboard'
  await loadMuhaffizPage(lastPage)
  refreshMuhaffizTopbarNotifications().catch(error => console.error(error))

  document.addEventListener('click', event => {
    const topWrap = document.querySelector('.topbar-user-menu-wrap')
    const sideWrap = document.querySelector('.sidebar-user-menu-wrap')
    if ((topWrap && topWrap.contains(event.target)) || (sideWrap && sideWrap.contains(event.target))) return
    closeTopbarUserMenu()
    closeTopbarNotifMenu()
  })
})
