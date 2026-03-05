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
const SANTRI_PERIZINAN_TABLE = 'izin_santri'
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
const CHAT_MEMBERS_TABLE = 'chat_thread_members'
const CHAT_MESSAGES_TABLE = 'chat_messages'
const SCHOOL_PROFILE_TABLE = 'struktur_sekolah'
const TOPBAR_NOTIF_READ_KEY = 'musyrif_topbar_notif_read'
const TOPBAR_NOTIF_RANGE_KEY = 'musyrif_topbar_notif_range_days'

const PAGE_TITLES = {
  dashboard: 'Dashboard',
  'laporan-bulanan': 'Laporan Bulanan',
  chat: 'Chat',
  'data-kamar': 'Data Kamar',
  'perizinan-santri': 'Perizinan Santri',
  'prestasi-pelanggaran': 'Prestasi & Pelanggaran',
  ekskul: 'Ekskul',
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
let topbarChatBadgeState = {
  intervalId: null,
  refreshInFlight: false
}
let musyrifDashboardAgendaRows = []
let musyrifPerizinanState = {
  musyrifId: '',
  santriRows: [],
  ownRows: [],
  reviewRows: [],
  reviewTargets: []
}
let musyrifEkskulState = {
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
let musyrifPrestasiPelanggaranState = {
  tab: 'prestasi',
  santriRows: [],
  kelasMap: new Map(),
  prestasiRows: [],
  pelanggaranRows: [],
  editingPrestasiId: '',
  editingPelanggaranId: ''
}
let musyrifBulkImportState = {
  parsedRows: [],
  matchedRows: [],
  errors: [],
  fileName: ''
}
let musyrifManageKamarState = {
  kamarRows: [],
  memberRows: [],
  santriRows: [],
  kelasMap: new Map(),
  selectedKamarId: '',
  selectedSet: new Set(),
  blockedSet: new Set(),
  search: ''
}
let wakasekKesantrianAccessCache = null

const MUSYRIF_BULK_EXCEL_HEADERS = [
  'Nama Santri',
  'Kelas',
  "Kehadiran Liqa' Muhasabah (%)",
  'Sakit',
  'Izin',
  'Ibadah',
  'Kedisiplinan',
  'Kebersihan',
  'Adab',
  'Catatan Musyrif'
]

function escapeHtml(value) {
  if (typeof window.appEscapeHtml === 'function') return window.appEscapeHtml(value)
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

function getKaryawanFotoInitial(nama) {
  if (typeof window.getProfileInitials === 'function') return window.getProfileInitials(nama)
  const words = String(nama || '').trim().split(/\s+/).filter(Boolean)
  if (!words.length) return 'U'
  if (words.length === 1) return words[0].slice(0, 2).toUpperCase()
  return `${words[0][0] || ''}${words[1][0] || ''}`.toUpperCase()
}

function renderMusyrifProfilFotoPreview(fotoUrl, nama) {
  if (typeof window.renderProfilePhotoPreview === 'function') {
    window.renderProfilePhotoPreview('musyrif-profil-foto-preview', fotoUrl, nama)
    return
  }
  const box = document.getElementById('musyrif-profil-foto-preview')
  if (!box) return
  const url = String(fotoUrl || '').trim()
  if (url) {
    box.innerHTML = `<img src="${escapeHtml(url)}" alt="Foto Profil" style="width:64px; height:64px; border-radius:999px; object-fit:cover; border:1px solid #cbd5e1;">`
    return
  }
  box.innerHTML = `<span style="width:64px; height:64px; border-radius:999px; display:inline-flex; align-items:center; justify-content:center; background:#e2e8f0; color:#0f172a; font-weight:700; border:1px solid #cbd5e1;">${escapeHtml(getKaryawanFotoInitial(nama))}</span>`
}

function getFotoFileExt(fileName = '') {
  if (typeof window.getProfilePhotoFileExt === 'function') return window.getProfilePhotoFileExt(fileName)
  const raw = String(fileName || '').trim().toLowerCase()
  const parts = raw.split('.')
  const ext = parts.length > 1 ? parts.pop() : ''
  if (!ext) return 'jpg'
  if (ext === 'jpeg') return 'jpg'
  if (ext === 'png' || ext === 'jpg' || ext === 'webp') return ext
  return 'jpg'
}

async function uploadMusyrifProfilePhoto(event) {
  if (typeof window.uploadProfilePhotoShared === 'function') {
    try {
      const result = await window.uploadProfilePhotoShared({
        event,
        sb,
        bucket: KARYAWAN_FOTO_BUCKET,
        maxSizeBytes: KARYAWAN_FOTO_MAX_SIZE_BYTES,
        idInputId: 'musyrif-profil-id-karyawan',
        defaultId: 'musyrif',
        fileUrlInputId: 'musyrif-profil-foto-url',
        namaInputId: 'musyrif-profil-nama',
        previewId: 'musyrif-profil-foto-preview'
      })
      if (result?.ok) return
      if (result?.reason === 'no_file') return
    } catch (error) {
      alert(`Gagal upload foto: ${error?.message || 'Unknown error'}`)
      if (event?.target) event.target.value = ''
      return
    }
  }
  const file = event?.target?.files?.[0]
  if (!file) return
  try {
    if (!String(file.type || '').toLowerCase().startsWith('image/')) {
      throw new Error('File harus berupa gambar (JPG, PNG, WEBP).')
    }
    if (Number(file.size || 0) > KARYAWAN_FOTO_MAX_SIZE_BYTES) {
      throw new Error('Ukuran gambar maksimal 300 KB.')
    }
    const idKaryawan = String(document.getElementById('musyrif-profil-id-karyawan')?.value || 'musyrif').trim().replaceAll(' ', '_')
    const ext = getFotoFileExt(file.name)
    const filePath = `${idKaryawan}_${Date.now()}.${ext}`
    const uploadRes = await sb.storage.from(KARYAWAN_FOTO_BUCKET).upload(filePath, file, { upsert: true })
    if (uploadRes.error) throw uploadRes.error
    const pub = sb.storage.from(KARYAWAN_FOTO_BUCKET).getPublicUrl(filePath)
    const fotoUrl = String(pub?.data?.publicUrl || '').trim()
    if (!fotoUrl) throw new Error('URL foto tidak valid.')
    const input = document.getElementById('musyrif-profil-foto-url')
    if (input) input.value = fotoUrl
    const nama = String(document.getElementById('musyrif-profil-nama')?.value || '').trim()
    renderMusyrifProfilFotoPreview(fotoUrl, nama)
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

function isWakasekKurikulumRole(role) {
  const clean = normalizeRoleValue(role).replaceAll(' ', '')
  if (!clean) return false
  return clean.includes('wakasek') && (clean.includes('kurikulum') || clean.includes('akademik'))
}

function isWakasekKesantrianRole(role) {
  const clean = normalizeRoleValue(role).replaceAll(' ', '')
  if (!clean) return false
  return clean.includes('wakasek') && clean.includes('kesantrian')
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

function isAnyWakasekRole(role) {
  const clean = normalizeRoleValue(role).replaceAll(' ', '')
  return !!clean && clean.includes('wakasek')
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

async function getIsWakasekKesantrian(forceReload = false) {
  const loginId = String(localStorage.getItem('login_id') || '').trim()
  if (!forceReload && wakasekKesantrianAccessCache && wakasekKesantrianAccessCache.loginId === loginId) {
    return !!wakasekKesantrianAccessCache.allowed
  }

  try {
    const profile = musyrifState.profile || await getCurrentMusyrif()
    const profileName = normalizePersonName(profile?.nama)
    const profileId = String(profile?.id || '').trim().toLowerCase()
    const profileIdKaryawan = String(profile?.id_karyawan || '').trim().toLowerCase()

    if (!profileName) {
      wakasekKesantrianAccessCache = { loginId, allowed: false }
      return false
    }

    const selectAttempts = [
      'wakasek_bidang_kesantrian, wakasek_kesantrian, wakasek_bidang_kemusyrifan, wakasek_kemusyrifan',
      'wakasek_bidang_kemusyrifan, wakasek_kemusyrifan',
      'wakasek_bidang_kesantrian',
      'wakasek_kesantrian',
      'wakasek_bidang_kemusyrifan',
      'wakasek_kemusyrifan'
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
      row?.wakasek_bidang_kesantrian ||
      row?.wakasek_kesantrian ||
      row?.wakasek_bidang_kemusyrifan ||
      row?.wakasek_kemusyrifan ||
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
      allowed = parseRoleList().some(isWakasekKesantrianRole)
    }

    wakasekKesantrianAccessCache = { loginId, allowed }
    return !!allowed
  } catch (error) {
    console.error(error)
    wakasekKesantrianAccessCache = { loginId, allowed: false }
    return false
  }
}

function getSantriPerizinanReviewTargets() {
  const roles = parseRoleList()
  const targets = []
  const hasAnyWakasek = roles.some(isAnyWakasekRole)
  const hasKurikulum = roles.some(isWakasekKurikulumRole)
  const hasKesantrian = roles.some(isWakasekKesantrianRole)
  const hasKetahfizan = roles.some(isWakasekKetahfizanRole)

  if (hasKurikulum) targets.push('wakasek_kurikulum')
  if (hasKesantrian) targets.push('wakasek_kesantrian')
  if (hasKetahfizan) targets.push('wakasek_ketahfizan')

  // Fallback: jika role tertulis "wakasek" tapi tanpa bidang yang jelas,
  // tampilkan semua pengajuan agar panel review tetap berfungsi.
  if (hasAnyWakasek && targets.length === 0) {
    targets.push('wakasek_kurikulum', 'wakasek_kesantrian', 'wakasek_ketahfizan')
  }
  return [...new Set(targets)]
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
  if (typeof window.setupSharedPopupSystem === 'function') {
    const ok = window.setupSharedPopupSystem()
    if (ok) return
  }
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

async function setMusyrifWelcomeName() {
  try {
    const profile = await getCurrentMusyrif()
    if (!profile) return
    musyrifState.profile = profile
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
  const hasNotif = document.getElementById('topbar-notif-trigger') && document.getElementById('topbar-notif-menu')

  if (!hasNotif) {
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

  if (!document.getElementById('topbar-chat-trigger')) {
    const chatBtn = document.createElement('button')
    chatBtn.type = 'button'
    chatBtn.id = 'topbar-chat-trigger'
    chatBtn.className = 'topbar-notif-trigger'
    chatBtn.title = 'Chat'
    chatBtn.innerHTML = '<span aria-hidden="true">&#128172;</span><span id="topbar-chat-badge" class="topbar-notif-badge hidden">0</span>'
    chatBtn.addEventListener('click', event => {
      event.preventDefault()
      event.stopPropagation()
      loadMusyrifPage('chat')
    })
    const userTrigger = wrap.querySelector('.topbar-user-trigger')
    wrap.insertBefore(chatBtn, userTrigger || null)
  }
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
  if (typeof window.setTopbarBadgeCount === 'function') {
    window.setTopbarBadgeCount('topbar-notif-badge', count)
    return
  }
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

function setTopbarChatBadge(count) {
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
  badge.classList.remove('hidden')
  badge.textContent = total > 99 ? '99+' : String(total)
}

function getChatTimestampMs(value) {
  const ms = Date.parse(String(value || '').trim())
  return Number.isFinite(ms) ? ms : 0
}

async function fetchMusyrifUnreadChatThreadCount() {
  const userId = String(musyrifState?.profile?.id || '').trim()
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
    const nextMs = getChatTimestampMs(row?.created_at)
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
    const readMs = getChatTimestampMs(member?.last_read_at)
    if (!readMs || incomingMs > readMs) unread += 1
  })
  return unread
}

async function refreshMusyrifTopbarChatBadge() {
  ensureTopbarNotification()
  if (topbarChatBadgeState.refreshInFlight) return
  topbarChatBadgeState.refreshInFlight = true
  try {
    const unreadCount = await fetchMusyrifUnreadChatThreadCount()
    setTopbarChatBadge(unreadCount)
  } catch (error) {
    console.error(error)
  } finally {
    topbarChatBadgeState.refreshInFlight = false
  }
}

function startMusyrifTopbarChatBadgeTicker() {
  if (topbarChatBadgeState.intervalId) return
  topbarChatBadgeState.intervalId = window.setInterval(() => {
    refreshMusyrifTopbarChatBadge().catch(error => console.error(error))
  }, 10000)
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

function normalizeSantriPerizinanStatus(value) {
  const raw = String(value || '').trim().toLowerCase()
  if (raw === 'diterima') return 'diterima'
  if (raw === 'ditolak') return 'ditolak'
  return 'menunggu'
}

function getSantriPerizinanStatusLabel(value) {
  const status = normalizeSantriPerizinanStatus(value)
  if (status === 'diterima') return 'Diterima'
  if (status === 'ditolak') return 'Ditolak'
  return 'Menunggu'
}

function normalizeSantriPerizinanTarget(value) {
  const raw = String(value || '').trim().toLowerCase().replaceAll('-', '_')
  if (raw === 'wakasek_kesantrian') return 'wakasek_kesantrian'
  if (raw === 'wakasek_ketahfizan') return 'wakasek_ketahfizan'
  return 'wakasek_kurikulum'
}

function getSantriPerizinanTargetLabel(value) {
  const target = normalizeSantriPerizinanTarget(value)
  if (target === 'wakasek_kesantrian') return 'Wakasek Kesantrian'
  if (target === 'wakasek_ketahfizan') return 'Wakasek Ketahfizan'
  return 'Wakasek Kurikulum'
}

function formatSantriPerizinanDate(value) {
  const text = String(value || '').slice(0, 10)
  if (!text) return '-'
  const date = new Date(`${text}T00:00:00`)
  if (Number.isNaN(date.getTime())) return text
  return date.toLocaleDateString('id-ID', { day: '2-digit', month: 'short', year: 'numeric' })
}

function isSantriPerizinanMissingTableError(error) {
  const code = String(error?.code || '').toUpperCase()
  const msg = String(error?.message || '').toLowerCase()
  return code === '42P01' || (msg.includes(SANTRI_PERIZINAN_TABLE) && (msg.includes('does not exist') || msg.includes('schema cache')))
}

function getSantriPerizinanMissingTableMessage() {
  return `Tabel '${SANTRI_PERIZINAN_TABLE}' belum ada.\n\nJalankan SQL:\ncreate table if not exists public.${SANTRI_PERIZINAN_TABLE} (\n  id uuid primary key default gen_random_uuid(),\n  santri_id uuid not null,\n  pengaju_id uuid not null,\n  tujuan_wakasek text not null,\n  keperluan text not null,\n  status text not null default 'menunggu',\n  catatan_wakasek text null,\n  reviewed_by uuid null,\n  reviewed_at timestamptz null,\n  created_at timestamptz not null default now(),\n  updated_at timestamptz not null default now()\n);\n\ncreate index if not exists izin_santri_pengaju_idx on public.${SANTRI_PERIZINAN_TABLE}(pengaju_id);\ncreate index if not exists izin_santri_tujuan_idx on public.${SANTRI_PERIZINAN_TABLE}(tujuan_wakasek);\ncreate index if not exists izin_santri_status_idx on public.${SANTRI_PERIZINAN_TABLE}(status);\ncreate index if not exists izin_santri_created_idx on public.${SANTRI_PERIZINAN_TABLE}(created_at desc);`
}

async function getMusyrifAssignedSantriRows(musyrifId) {
  const sid = String(musyrifId || '').trim()
  if (!sid) return []

  const kamarRes = await sb
    .from(HALAQAH_TABLE)
    .select('id')
    .eq('musyrif_id', sid)

  if (kamarRes.error) throw kamarRes.error
  const kamarIds = [...new Set((kamarRes.data || []).map(item => String(item.id || '')).filter(Boolean))]
  if (!kamarIds.length) return []

  const kamarSantriRes = await sb
    .from(HALAQAH_SANTRI_TABLE)
    .select('santri_id')
    .in('kamar_id', kamarIds)
  if (kamarSantriRes.error) throw kamarSantriRes.error

  const santriIds = [...new Set((kamarSantriRes.data || []).map(item => String(item.santri_id || '')).filter(Boolean))]
  if (!santriIds.length) return []

  const santriRes = await sb
    .from('santri')
    .select('id, nama, kelas_id, aktif')
    .in('id', santriIds)
    .eq('aktif', true)
    .order('nama')

  if (santriRes.error) throw santriRes.error
  return santriRes.data || []
}

async function loadMusyrifPerizinanData() {
  const musyrifId = String(musyrifState?.profile?.id || '').trim()
  if (!musyrifId) return { musyrifId: '', santriRows: [], ownRows: [], reviewRows: [], reviewTargets: [] }

  const reviewTargets = getSantriPerizinanReviewTargets()
  const [santriRows, ownRes, reviewRes, karyawanRes] = await Promise.all([
    getMusyrifAssignedSantriRows(musyrifId),
    sb
      .from(SANTRI_PERIZINAN_TABLE)
      .select('id, santri_id, pengaju_id, tujuan_wakasek, keperluan, status, catatan_wakasek, reviewed_by, reviewed_at, created_at, updated_at')
      .eq('pengaju_id', musyrifId)
      .order('created_at', { ascending: false }),
    reviewTargets.length
      ? sb
        .from(SANTRI_PERIZINAN_TABLE)
        .select('id, santri_id, pengaju_id, tujuan_wakasek, keperluan, status, catatan_wakasek, reviewed_by, reviewed_at, created_at, updated_at')
        .order('created_at', { ascending: false })
      : Promise.resolve({ data: [], error: null }),
    sb.from('karyawan').select('id, nama, id_karyawan')
  ])

  const firstError = ownRes.error || reviewRes.error || karyawanRes.error
  if (firstError) throw firstError

  const linkedSantriIds = [...new Set([
    ...(santriRows || []).map(item => String(item.id || '')),
    ...(ownRes.data || []).map(item => String(item.santri_id || '')),
    ...(reviewRes.data || []).map(item => String(item.santri_id || ''))
  ].filter(Boolean))]

  let santriMap = new Map((santriRows || []).map(item => [String(item.id || ''), item]))
  if (linkedSantriIds.length) {
    const extraSantriRes = await sb
      .from('santri')
      .select('id, nama, kelas_id')
      .in('id', linkedSantriIds)
    if (!extraSantriRes.error) {
      santriMap = new Map((extraSantriRes.data || []).map(item => [String(item.id || ''), item]))
    }
  }

  const kelasIds = [...new Set(
    Array.from(santriMap.values())
      .map(item => String(item?.kelas_id || ''))
      .filter(Boolean)
  )]
  let kelasMap = new Map()
  if (kelasIds.length) {
    const kelasRes = await sb
      .from('kelas')
      .select('id, nama_kelas')
      .in('id', kelasIds)
    if (!kelasRes.error) {
      kelasMap = new Map((kelasRes.data || []).map(item => [String(item.id || ''), item]))
    }
  }

  const karyawanMap = new Map((karyawanRes.data || []).map(item => [String(item.id || ''), item]))
  const enrich = row => {
    const santri = santriMap.get(String(row?.santri_id || ''))
    const kelas = kelasMap.get(String(santri?.kelas_id || ''))
    return {
      ...row,
      status: normalizeSantriPerizinanStatus(row?.status),
      tujuan_wakasek: normalizeSantriPerizinanTarget(row?.tujuan_wakasek),
      santri_nama: String(santri?.nama || '-'),
      kelas_nama: String(kelas?.nama_kelas || '-'),
      pengaju_nama: String(karyawanMap.get(String(row?.pengaju_id || ''))?.nama || '-'),
      reviewer_nama: String(karyawanMap.get(String(row?.reviewed_by || ''))?.nama || '-')
    }
  }

  return {
    musyrifId,
    santriRows: santriRows || [],
    ownRows: (ownRes.data || []).map(enrich),
    reviewRows: (reviewRes.data || [])
      .map(enrich)
      .filter(item => reviewTargets.includes(normalizeSantriPerizinanTarget(item?.tujuan_wakasek))),
    reviewTargets
  }
}

function renderMusyrifPerizinanOwnRows() {
  const box = document.getElementById('musyrif-perizinan-own-list')
  if (!box) return
  const rows = musyrifPerizinanState.ownRows || []
  if (!rows.length) {
    box.innerHTML = '<div style="color:#64748b;">Belum ada pengajuan izin santri.</div>'
    return
  }

  const htmlRows = rows.map((item, index) => `
    <tr>
      <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${index + 1}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(formatSantriPerizinanDate(item.created_at))}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.santri_nama || '-')}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.kelas_nama || '-')}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(getSantriPerizinanTargetLabel(item.tujuan_wakasek))}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(String(item.keperluan || '-'))}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(getSantriPerizinanStatusLabel(item.status))}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(String(item.catatan_wakasek || '-'))}</td>
    </tr>
  `).join('')

  box.innerHTML = `
    <div style="overflow:auto;">
      <table style="width:100%; min-width:1080px; border-collapse:collapse; font-size:13px;">
        <thead>
          <tr style="background:#f8fafc;">
            <th style="padding:8px; border:1px solid #e2e8f0; width:52px;">No</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Tanggal Ajukan</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Nama Santri</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Kelas</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Tujuan Izin</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Keperluan</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Status</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Catatan Wakasek</th>
          </tr>
        </thead>
        <tbody>${htmlRows}</tbody>
      </table>
    </div>
  `
}

function renderMusyrifPerizinanReviewRows() {
  const panel = document.getElementById('musyrif-perizinan-review-panel')
  const box = document.getElementById('musyrif-perizinan-review-list')
  if (!panel || !box) return
  const rows = musyrifPerizinanState.reviewRows || []
  const hasAccess = (musyrifPerizinanState.reviewTargets || []).length > 0
  if (!hasAccess) {
    panel.style.display = 'none'
    box.innerHTML = ''
    return
  }
  panel.style.display = 'block'
  if (!rows.length) {
    box.innerHTML = '<div style="color:#64748b;">Belum ada pengajuan izin untuk Anda.</div>'
    return
  }

  const htmlRows = rows.map((item, index) => `
    <tr>
      <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${index + 1}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(formatSantriPerizinanDate(item.created_at))}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.santri_nama || '-')}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.kelas_nama || '-')}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.pengaju_nama || '-')}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(getSantriPerizinanTargetLabel(item.tujuan_wakasek))}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(String(item.keperluan || '-'))}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">
        <select id="musyrif-perizinan-status-${escapeHtml(String(item.id || ''))}" class="guru-field" style="min-width:130px; padding:6px 8px; font-size:12px;">
          <option value="menunggu" ${item.status === 'menunggu' ? 'selected' : ''}>Menunggu</option>
          <option value="diterima" ${item.status === 'diterima' ? 'selected' : ''}>Diterima</option>
          <option value="ditolak" ${item.status === 'ditolak' ? 'selected' : ''}>Ditolak</option>
        </select>
      </td>
      <td style="padding:8px; border:1px solid #e2e8f0;">
        <input id="musyrif-perizinan-note-${escapeHtml(String(item.id || ''))}" class="guru-field" type="text" value="${escapeHtml(String(item.catatan_wakasek || ''))}" placeholder="Catatan wakasek" style="padding:6px 8px; font-size:12px;">
      </td>
      <td style="padding:8px; border:1px solid #e2e8f0;">
        <button type="button" class="modal-btn modal-btn-primary" onclick="saveSantriPerizinanReview('${escapeHtml(String(item.id || ''))}')">Simpan</button>
      </td>
    </tr>
  `).join('')

  box.innerHTML = `
    <div style="overflow:auto;">
      <table style="width:100%; min-width:1260px; border-collapse:collapse; font-size:13px;">
        <thead>
          <tr style="background:#f8fafc;">
            <th style="padding:8px; border:1px solid #e2e8f0; width:52px;">No</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Tanggal</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Santri</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Kelas</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Pengaju</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Tujuan</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Keperluan</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Status</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Catatan</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Aksi</th>
          </tr>
        </thead>
        <tbody>${htmlRows}</tbody>
      </table>
    </div>
  `
}

async function submitSantriPerizinanForm() {
  const musyrifId = String(musyrifPerizinanState.musyrifId || '').trim()
  const santriId = String(document.getElementById('musyrif-perizinan-santri')?.value || '').trim()
  const tujuan = normalizeSantriPerizinanTarget(document.getElementById('musyrif-perizinan-tujuan')?.value || '')
  const keperluan = String(document.getElementById('musyrif-perizinan-keperluan')?.value || '').trim()
  if (!musyrifId) {
    alert('Data musyrif tidak ditemukan.')
    return
  }
  if (!santriId || !keperluan) {
    alert('Nama santri dan keperluan izin wajib diisi.')
    return
  }

  const payload = {
    santri_id: santriId,
    pengaju_id: musyrifId,
    tujuan_wakasek: tujuan,
    keperluan,
    status: 'menunggu',
    catatan_wakasek: null,
    reviewed_by: null,
    reviewed_at: null
  }
  const { error } = await sb.from(SANTRI_PERIZINAN_TABLE).insert([payload])
  if (error) {
    if (isSantriPerizinanMissingTableError(error)) {
      alert(getSantriPerizinanMissingTableMessage())
      return
    }
    alert(`Gagal mengajukan izin santri: ${error.message || 'Unknown error'}`)
    return
  }

  alert('Pengajuan izin santri berhasil dikirim.')
  await renderMusyrifPerizinanPage(true)
}

async function saveSantriPerizinanReview(id) {
  const sid = String(id || '').trim()
  if (!sid) return
  if (!(musyrifPerizinanState.reviewTargets || []).length) return
  const status = normalizeSantriPerizinanStatus(document.getElementById(`musyrif-perizinan-status-${sid}`)?.value || '')
  const catatan = String(document.getElementById(`musyrif-perizinan-note-${sid}`)?.value || '').trim()
  const reviewerId = String(musyrifState?.profile?.id || '').trim() || null
  const payload = {
    status,
    catatan_wakasek: catatan || null,
    reviewed_by: reviewerId,
    reviewed_at: new Date().toISOString(),
    updated_at: new Date().toISOString()
  }
  const { error } = await sb.from(SANTRI_PERIZINAN_TABLE).update(payload).eq('id', sid)
  if (error) {
    if (isSantriPerizinanMissingTableError(error)) {
      alert(getSantriPerizinanMissingTableMessage())
      return
    }
    alert(`Gagal menyimpan persetujuan: ${error.message || 'Unknown error'}`)
    return
  }
  await renderMusyrifPerizinanPage(true)
}

async function renderMusyrifPerizinanPage(forceReload = false) {
  const content = document.getElementById('musyrif-content')
  if (!content) return
  content.innerHTML = '<div class="placeholder-card">Loading perizinan santri...</div>'

  try {
    const payload = await loadMusyrifPerizinanData()
    musyrifPerizinanState = payload
    const santriOptions = (payload.santriRows || []).map(item => (
      `<option value="${escapeHtml(String(item.id || ''))}">${escapeHtml(String(item.nama || '-'))}</option>`
    )).join('')

    content.innerHTML = `
      <div style="display:grid; gap:12px;">
        <div style="border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">
          <div style="font-weight:700; margin-bottom:8px; color:#0f172a;">Form Pengajuan Izin Santri</div>
          <div style="display:grid; grid-template-columns:repeat(auto-fit,minmax(260px,1fr)); gap:8px;">
            <div>
              <div style="font-size:12px; color:#64748b; margin-bottom:4px;">Nama Santri (kamar Anda)</div>
              <select id="musyrif-perizinan-santri" class="guru-field">
                <option value="">-- Pilih Santri --</option>
                ${santriOptions}
              </select>
            </div>
            <div>
              <div style="font-size:12px; color:#64748b; margin-bottom:4px;">Tujuan Izin</div>
              <select id="musyrif-perizinan-tujuan" class="guru-field">
                <option value="wakasek_kurikulum">Wakasek Kurikulum</option>
                <option value="wakasek_kesantrian">Wakasek Kesantrian</option>
                <option value="wakasek_ketahfizan">Wakasek Ketahfizan</option>
              </select>
            </div>
          </div>
          <div style="margin-top:8px;">
            <div style="font-size:12px; color:#64748b; margin-bottom:4px;">Keperluan Izin</div>
            <textarea id="musyrif-perizinan-keperluan" class="guru-field" rows="3" placeholder="Tuliskan keperluan izin santri..."></textarea>
          </div>
          <div style="margin-top:10px;">
            <button type="button" class="modal-btn modal-btn-primary" onclick="submitSantriPerizinanForm()">Ajukan Izin</button>
            <button type="button" class="modal-btn modal-btn-secondary" onclick="renderMusyrifPerizinanPage(true)" style="margin-left:8px;">Refresh</button>
          </div>
        </div>

        <div style="border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">
          <div style="font-weight:700; margin-bottom:8px; color:#0f172a;">Daftar Pengajuan Izin Santri</div>
          <div id="musyrif-perizinan-own-list">Loading...</div>
        </div>

        <div id="musyrif-perizinan-review-panel" style="display:none; border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">
          <div style="font-weight:700; margin-bottom:8px; color:#0f172a;">Persetujuan Izin Santri (Wakasek)</div>
          <div id="musyrif-perizinan-review-list">Loading...</div>
        </div>
      </div>
    `

    renderMusyrifPerizinanOwnRows()
    renderMusyrifPerizinanReviewRows()
  } catch (error) {
    console.error(error)
    if (isSantriPerizinanMissingTableError(error)) {
      content.innerHTML = `<div class="placeholder-card" style="white-space:pre-wrap;">${escapeHtml(getSantriPerizinanMissingTableMessage())}</div>`
      return
    }
    content.innerHTML = `<div class="placeholder-card">Gagal load perizinan santri: ${escapeHtml(error?.message || 'Unknown error')}</div>`
  }
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

function normalizeMusyrifImportHeader(value) {
  return String(value || '')
    .trim()
    .toLowerCase()
    .replace(/[%().']/g, '')
    .replace(/\s+/g, ' ')
}

function getMusyrifImportCellByAliases(row, aliases) {
  const entries = Object.entries(row || {})
  const normalizedMap = new Map(entries.map(([key, val]) => [normalizeMusyrifImportHeader(key), val]))
  for (const alias of aliases) {
    const value = normalizedMap.get(normalizeMusyrifImportHeader(alias))
    if (value !== undefined) return value
  }
  return ''
}

function getMusyrifBulkDomRowMaps() {
  const rows = Array.from(document.querySelectorAll('#musyrif-bulk-body tr[data-bulk-row="1"]'))
  const byNameClass = new Map()
  rows.forEach(row => {
    const nama = String(row.children?.[1]?.textContent || '').trim().toLowerCase()
    const kelas = String(row.children?.[2]?.textContent || '').trim().toLowerCase()
    const key = `${nama}|${kelas}`
    if (!byNameClass.has(key)) byNameClass.set(key, [])
    byNameClass.get(key).push(row)
  })
  return { rows, byNameClass }
}

function setMusyrifBulkDomRowValues(rowEl, rowData) {
  const setVal = (field, value) => {
    const input = rowEl.querySelector(`[data-bulk-field="${field}"]`)
    if (!input) return
    input.value = value === null || value === undefined ? '' : String(value)
  }
  setVal('kehadiran', rowData.nilaiKehadiran)
  setVal('sakit', rowData.sakit)
  setVal('izin', rowData.izin)
  setVal('ibadah', normalizeAkhlakGrade(rowData.ibadah) || '')
  setVal('kedisiplinan', normalizeAkhlakGrade(rowData.kedisiplinan) || '')
  setVal('kebersihan', normalizeAkhlakGrade(rowData.kebersihan) || '')
  setVal('adab', normalizeAkhlakGrade(rowData.adab) || '')
  setVal('catatan', rowData.catatan)
}

function collectMusyrifBulkRowsForTemplate() {
  const { rows } = getMusyrifBulkDomRowMaps()
  return rows.map(row => ({
    nama: String(row.children?.[1]?.textContent || '').trim(),
    kelas: String(row.children?.[2]?.textContent || '').trim(),
    nilaiKehadiran: String(row.querySelector('[data-bulk-field="kehadiran"]')?.value || '').trim(),
    sakit: String(row.querySelector('[data-bulk-field="sakit"]')?.value || '').trim(),
    izin: String(row.querySelector('[data-bulk-field="izin"]')?.value || '').trim(),
    ibadah: String(row.querySelector('[data-bulk-field="ibadah"]')?.value || '').trim(),
    kedisiplinan: String(row.querySelector('[data-bulk-field="kedisiplinan"]')?.value || '').trim(),
    kebersihan: String(row.querySelector('[data-bulk-field="kebersihan"]')?.value || '').trim(),
    adab: String(row.querySelector('[data-bulk-field="adab"]')?.value || '').trim(),
    catatan: String(row.querySelector('[data-bulk-field="catatan"]')?.value || '').trim()
  }))
}

function renderMusyrifBulkImportPreview() {
  const box = document.getElementById('musyrif-bulk-import-preview')
  if (!box) return
  const matchedRows = musyrifBulkImportState.matchedRows || []
  const errors = musyrifBulkImportState.errors || []
  const fileName = String(musyrifBulkImportState.fileName || '').trim()

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
        <button type="button" class="modal-btn modal-btn-primary" onclick="applyMusyrifBulkExcelToTable()" ${matchedRows.length ? '' : 'disabled'}>Gunakan Data Excel</button>
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

function downloadMusyrifBulkTemplate() {
  const rows = collectMusyrifBulkRowsForTemplate()
  if (!window.XLSX) {
    alert('Library Excel belum termuat. Refresh halaman lalu coba lagi.')
    return
  }
  const exportRows = (rows.length ? rows : (musyrifState.santriList || []).map(item => {
    const kelas = musyrifState.kelasMap.get(String(item.kelas_id || ''))
    return { nama: item.nama || '-', kelas: kelas?.nama_kelas || '-' }
  })).map(item => ({
    'Nama Santri': item.nama || '',
    'Kelas': item.kelas || '',
    "Kehadiran Liqa' Muhasabah (%)": item.nilaiKehadiran || '',
    'Sakit': item.sakit || '',
    'Izin': item.izin || '',
    'Ibadah': item.ibadah || '',
    'Kedisiplinan': item.kedisiplinan || '',
    'Kebersihan': item.kebersihan || '',
    'Adab': item.adab || '',
    'Catatan Musyrif': item.catatan || ''
  }))
  const wb = window.XLSX.utils.book_new()
  const ws = window.XLSX.utils.json_to_sheet(exportRows, { header: MUSYRIF_BULK_EXCEL_HEADERS })
  window.XLSX.utils.book_append_sheet(wb, ws, 'InputMassalKesantrian')
  const periode = musyrifState.periode || getMonthInputToday()
  window.XLSX.writeFile(wb, `Template-Input-Massal-Kesantrian-${periode}.xlsx`)
}

async function onMusyrifBulkExcelFileChange(event) {
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
    const { byNameClass } = getMusyrifBulkDomRowMaps()
    const keyUsedCount = new Map()
    const parsedRows = []
    const matchedRows = []
    const errors = []

    rawRows.forEach((raw, idx) => {
      const nama = String(getMusyrifImportCellByAliases(raw, ['Nama Santri', 'Nama'])).trim()
      const kelas = String(getMusyrifImportCellByAliases(raw, ['Kelas'])).trim()
      if (!nama && !kelas) return

      const rowData = {
        nama,
        kelas,
        nilaiKehadiran: String(getMusyrifImportCellByAliases(raw, ["Kehadiran Liqa' Muhasabah (%)", 'Kehadiran Liqa Muhasabah (%)', "Kehadiran Liqa' Muhasabah", 'Kehadiran Liqa Muhasabah', 'Kehadiran Liqa (%)', 'Kehadiran Liqa', 'Kehadiran'])).trim(),
        sakit: String(getMusyrifImportCellByAliases(raw, ['Sakit'])).trim(),
        izin: String(getMusyrifImportCellByAliases(raw, ['Izin'])).trim(),
        ibadah: String(getMusyrifImportCellByAliases(raw, ['Ibadah'])).trim(),
        kedisiplinan: String(getMusyrifImportCellByAliases(raw, ['Kedisiplinan'])).trim(),
        kebersihan: String(getMusyrifImportCellByAliases(raw, ['Kebersihan'])).trim(),
        adab: String(getMusyrifImportCellByAliases(raw, ['Adab'])).trim(),
        catatan: String(getMusyrifImportCellByAliases(raw, ['Catatan Musyrif', 'Catatan'])).trim()
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
      const validationErrors = validateMusyrifBulkRow({ ...rowData, nama: `${nama} (${kelas})` })
      if (validationErrors.length) {
        errors.push(...validationErrors)
        return
      }
      matchedRows.push({ targetRow, rowData })
    })

    musyrifBulkImportState = {
      parsedRows,
      matchedRows,
      errors,
      fileName: String(file.name || '')
    }
    renderMusyrifBulkImportPreview()
  } catch (error) {
    console.error(error)
    alert(`Gagal membaca file Excel: ${error?.message || 'Unknown error'}`)
  } finally {
    event.target.value = ''
  }
}

function applyMusyrifBulkExcelToTable() {
  const matchedRows = musyrifBulkImportState.matchedRows || []
  const errors = musyrifBulkImportState.errors || []
  if (!matchedRows.length) {
    alert('Tidak ada data valid dari Excel yang bisa diterapkan.')
    return
  }
  matchedRows.forEach(item => {
    setMusyrifBulkDomRowValues(item.targetRow, item.rowData)
  })
  alert(`Data Excel diterapkan ke tabel: ${matchedRows.length} baris.${errors.length ? `\nError: ${errors.length} baris.` : ''}`)
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
      .select('santri_id, kelas_id, guru_id, musyrif, no_hp_musyrif, nilai_kehadiran_liqa_muhasabah, sakit_liqa_muhasabah, izin_liqa_muhasabah, nilai_ibadah, nilai_kedisiplinan, nilai_kebersihan, nilai_adab, catatan_musyrif')
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
  musyrifBulkImportState = { parsedRows: [], matchedRows: [], errors: [], fileName: '' }
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
        <div style="display:flex; gap:8px; flex-wrap:wrap;">
          <input id="musyrif-bulk-excel-input" type="file" accept=".xlsx,.xls" style="display:none;" onchange="onMusyrifBulkExcelFileChange(event)">
          <button type="button" class="modal-btn" onclick="downloadMusyrifBulkTemplate()">Download Template Excel</button>
          <button type="button" class="modal-btn" onclick="document.getElementById('musyrif-bulk-excel-input')?.click()">Upload Excel</button>
          <button type="button" class="modal-btn modal-btn-primary" onclick="saveMusyrifBulkInput()">Simpan Semua</button>
        </div>
      </div>
      <div id="musyrif-bulk-import-preview" style="display:none;"></div>
      <div style="overflow:auto; border:1px solid #e2e8f0; border-radius:10px;">
        <table style="width:100%; min-width:1460px; border-collapse:collapse; font-size:12px;">
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
    .select('id, musyrif, no_hp_musyrif, nilai_kehadiran_liqa_muhasabah, sakit_liqa_muhasabah, izin_liqa_muhasabah, nilai_ibadah, keterangan_ibadah, nilai_kedisiplinan, keterangan_kedisiplinan, nilai_kebersihan, keterangan_kebersihan, nilai_adab, keterangan_adab, catatan_musyrif')
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
  let prestasi = ''
  let pelanggaran = ''
  try {
    const [prestasiRes, pelanggaranRes] = await Promise.all([
      sb
        .from(SANTRI_PRESTASI_TABLE)
        .select('judul, waktu, created_at')
        .eq('santri_id', sid)
        .gte('waktu', periodeRange.start)
        .lte('waktu', periodeRange.end)
        .order('waktu', { ascending: true })
        .order('created_at', { ascending: true }),
      sb
        .from(SANTRI_PELANGGARAN_TABLE)
        .select('judul, hukuman, surat_jenis, waktu, created_at')
        .eq('santri_id', sid)
        .gte('waktu', periodeRange.start)
        .lte('waktu', periodeRange.end)
        .order('waktu', { ascending: true })
        .order('created_at', { ascending: true })
    ])
    if (prestasiRes.error) throw prestasiRes.error
    if (pelanggaranRes.error) throw pelanggaranRes.error
    prestasi = (prestasiRes.data || [])
      .map(item => String(item?.judul || '').trim())
      .filter(Boolean)
      .join(' | ')
    pelanggaran = (pelanggaranRes.data || [])
      .map(item => {
        const judul = String(item?.judul || '').trim()
        const hukuman = String(item?.hukuman || '').trim()
        const surat = String(item?.surat_jenis || '').trim()
        if (!judul) return ''
        const sanksi = surat || hukuman
        return sanksi ? `${judul} (${sanksi})` : judul
      })
      .filter(Boolean)
      .join(' | ')
  } catch (err) {
    console.error('Gagal memuat prestasi/pelanggaran otomatis kesantrian:', err)
  }

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
                <input id="m-lap-prestasi-kesantrian" class="guru-field" type="text" value="${escapeHtml(prestasi)}" readonly style="background:#f8fafc; color:#475569;">
              </td>
            </tr>
            <tr>
              <td style="padding:8px; border:1px solid #e2e8f0;">Pelanggaran</td>
              <td colspan="2" style="padding:8px; border:1px solid #e2e8f0;">
                <input id="m-lap-pelanggaran-kesantrian" class="guru-field" type="text" value="${escapeHtml(pelanggaran)}" readonly style="background:#f8fafc; color:#475569;">
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
      <div style="margin-bottom:12px;">
        <label class="guru-label">Foto Profil</label>
        <div style="display:flex; align-items:center; gap:10px; flex-wrap:wrap;">
          <div id="musyrif-profil-foto-preview"></div>
          <input id="musyrif-profil-foto-url" type="hidden" value="${escapeHtml(String(profile.foto_url || '').trim())}">
          <input id="musyrif-profil-foto-file" type="file" accept="image/*" style="display:none;" onchange="uploadMusyrifProfilePhoto(event)">
          <button type="button" class="modal-btn" onclick="document.getElementById('musyrif-profil-foto-file')?.click()">Upload Foto</button>
        </div>
        <div style="font-size:12px; color:#64748b; margin-top:6px;">Maksimal 300 KB.</div>
      </div>
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
        <label class="guru-label">Password</label>
        <div style="position:relative;">
          <input id="musyrif-profil-password" type="password" value="${escapeHtml(profile.password || '')}" placeholder="Password" class="guru-field" autocomplete="off" style="padding-right:46px;">
          <button id="musyrif-profil-password-toggle" type="button" onclick="toggleMusyrifProfilePassword()" aria-label="Tampilkan password" title="Tampilkan/Sembunyikan password" style="position:absolute; right:10px; top:50%; transform:translateY(-50%); border:none; background:transparent; cursor:pointer; font-size:16px; line-height:1;">👁</button>
        </div>
      </div>
      <button type="button" class="modal-btn modal-btn-primary" onclick="saveMusyrifProfil('${escapeHtml(profile.id)}')">Simpan Profil</button>
    </div>
  `
  renderMusyrifProfilFotoPreview(String(profile.foto_url || '').trim(), String(profile.nama || '').trim())
  const namaInput = document.getElementById('musyrif-profil-nama')
  if (namaInput) {
    namaInput.addEventListener('input', () => {
      const fotoUrl = String(document.getElementById('musyrif-profil-foto-url')?.value || '').trim()
      renderMusyrifProfilFotoPreview(fotoUrl, namaInput.value || '')
    })
  }
}

function toggleMusyrifProfilePassword() {
  const input = document.getElementById('musyrif-profil-password')
  const btn = document.getElementById('musyrif-profil-password-toggle')
  if (!input || !btn) return
  const willShow = input.type === 'password'
  input.type = willShow ? 'text' : 'password'
  btn.textContent = willShow ? '👁̶' : '👁'
  btn.setAttribute('aria-label', willShow ? 'Sembunyikan password' : 'Tampilkan password')
}

async function saveMusyrifProfil(musyrifId) {
  const nama = String(document.getElementById('musyrif-profil-nama')?.value || '').trim()
  const noHp = String(document.getElementById('musyrif-profil-no-hp')?.value || '').trim()
  const alamat = String(document.getElementById('musyrif-profil-alamat')?.value || '').trim()
  const password = String(document.getElementById('musyrif-profil-password')?.value || '').trim()
  const fotoUrl = String(document.getElementById('musyrif-profil-foto-url')?.value || '').trim()

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
    .eq('id', musyrifId)
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
  await setMusyrifWelcomeName()
  await renderMusyrifProfil()
}

function getMusyrifTodayDate() {
  return new Date().toISOString().slice(0, 10)
}

function isMusyrifEkskulMissingTableError(error) {
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

function getMusyrifMonthPeriodToday() {
  return new Date().toISOString().slice(0, 7)
}

function getMusyrifEkskulMemberRows() {
  const selected = getMusyrifSelectedEkskul()
  if (!selected) return []
  return (musyrifEkskulState.memberRows || []).filter(item => String(item.ekskul_id || '') === String(selected.id || ''))
}

function getMusyrifEkskulMonthlyPeriode() {
  const fromInput = String(document.getElementById('musyrif-ekskul-monthly-periode')?.value || '').trim()
  const stateValue = String(musyrifEkskulState.monthlyPeriode || '').trim()
  return fromInput || stateValue || getMusyrifMonthPeriodToday()
}

function setMusyrifEkskulTab(tabName = 'progres') {
  const tab = String(tabName || '').trim().toLowerCase() === 'laporan' ? 'laporan' : 'progres'
  musyrifEkskulState.activeTab = tab
  const btnProgres = document.getElementById('musyrif-ekskul-tab-btn-progres')
  const btnLaporan = document.getElementById('musyrif-ekskul-tab-btn-laporan')
  const panelProgres = document.getElementById('musyrif-ekskul-tab-progres')
  const panelLaporan = document.getElementById('musyrif-ekskul-tab-laporan')
  if (btnProgres) btnProgres.className = tab === 'progres' ? 'modal-btn modal-btn-primary' : 'modal-btn'
  if (btnLaporan) btnLaporan.className = tab === 'laporan' ? 'modal-btn modal-btn-primary' : 'modal-btn'
  if (panelProgres) panelProgres.style.display = tab === 'progres' ? '' : 'none'
  if (panelLaporan) panelLaporan.style.display = tab === 'laporan' ? '' : 'none'
}

async function loadMusyrifEkskulMonthlyRows() {
  const selected = getMusyrifSelectedEkskul()
  const periode = getMusyrifEkskulMonthlyPeriode()
  musyrifEkskulState.monthlyPeriode = periode
  if (!selected?.id || !periode) {
    musyrifEkskulState.monthlyRows = []
    return
  }
  const memberSantriIds = getMusyrifEkskulMemberRows().map(item => String(item.santri_id || '').trim()).filter(Boolean)
  if (!memberSantriIds.length) {
    musyrifEkskulState.monthlyRows = []
    return
  }
  const { data, error } = await sb
    .from(EKSKUL_MONTHLY_TABLE)
    .select('id, periode, ekskul_id, santri_id, kehadiran_persen, catatan_pj, updated_at')
    .eq('ekskul_id', String(selected.id))
    .eq('periode', periode)
    .in('santri_id', memberSantriIds)
  if (error) throw error
  musyrifEkskulState.monthlyRows = data || []
}

function renderMusyrifEkskulMonthlyRows() {
  const box = document.getElementById('musyrif-ekskul-monthly-list')
  if (!box) return
  const selected = getMusyrifSelectedEkskul()
  if (!selected) {
    box.innerHTML = '<div style="color:#64748b; font-size:12px;">Pilih ekskul terlebih dahulu.</div>'
    return
  }
  const members = getMusyrifEkskulMemberRows()
  if (!members.length) {
    box.innerHTML = '<div style="color:#64748b; font-size:12px;">Belum ada anggota ekskul.</div>'
    return
  }
  const santriMap = new Map((musyrifEkskulState.santriRows || []).map(item => [String(item.id || ''), item]))
  const monthlyMap = new Map((musyrifEkskulState.monthlyRows || []).map(item => [String(item.santri_id || ''), item]))
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
              <tr data-musyrif-ekskul-monthly-row="1" data-santri-id="${escapeHtml(sid)}">
                <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${idx + 1}</td>
                <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(String(santriMap.get(sid)?.nama || '-'))}</td>
                <td style="padding:8px; border:1px solid #e2e8f0;"><input class="guru-field" type="number" min="0" max="100" step="0.01" data-musyrif-ekskul-monthly-kehadiran="1" value="${escapeHtml(kehadiranValue)}" placeholder="0-100"></td>
                <td style="padding:8px; border:1px solid #e2e8f0;"><input class="guru-field" type="text" data-musyrif-ekskul-monthly-catatan="1" value="${escapeHtml(catatanValue)}" placeholder="Catatan PJ ekskul"></td>
              </tr>
            `
          }).join('')}
        </tbody>
      </table>
    </div>
  `
}

async function onMusyrifEkskulMonthlyPeriodeChange() {
  try {
    await loadMusyrifEkskulMonthlyRows()
    renderMusyrifEkskulMonthlyRows()
  } catch (error) {
    console.error(error)
    alert(`Gagal memuat laporan bulanan ekskul: ${error?.message || 'Unknown error'}`)
  }
}

async function saveMusyrifEkskulMonthlyReport() {
  const selected = getMusyrifSelectedEkskul()
  const periode = getMusyrifEkskulMonthlyPeriode()
  const updatedBy = String(musyrifState?.profile?.id || '').trim() || null
  if (!selected?.id || !periode) {
    alert('Pilih ekskul dan periode terlebih dahulu.')
    return
  }
  const rowEls = Array.from(document.querySelectorAll('[data-musyrif-ekskul-monthly-row="1"]'))
  if (!rowEls.length) {
    alert('Belum ada anggota ekskul untuk diinput.')
    return
  }
  const payload = []
  rowEls.forEach(rowEl => {
    const sid = String(rowEl.getAttribute('data-santri-id') || '').trim()
    if (!sid) return
    const kehadiranRaw = String(rowEl.querySelector('[data-musyrif-ekskul-monthly-kehadiran="1"]')?.value || '').trim()
    const catatanRaw = String(rowEl.querySelector('[data-musyrif-ekskul-monthly-catatan="1"]')?.value || '').trim()
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
  await loadMusyrifEkskulMonthlyRows()
  renderMusyrifEkskulMonthlyRows()
  alert('Laporan bulanan ekskul berhasil disimpan.')
}

function isMusyrifEkskulMissingPj2ColumnError(error) {
  const msg = String(error?.message || '').toLowerCase()
  return msg.includes('pj_karyawan_id_2')
}

async function setupMusyrifEkskulAccess(forceReload = false) {
  const btn = document.getElementById('musyrif-nav-ekskul')
  const blockedByHigherPriorityRole = hasBaseRole('guru')
  if (blockedByHigherPriorityRole) {
    musyrifEkskulState.hasAccess = false
    if (btn) btn.style.display = 'none'
    return false
  }
  const profile = musyrifState.profile || await getCurrentMusyrif()
  if (!profile?.id) {
    musyrifEkskulState.hasAccess = false
    if (btn) btn.style.display = 'none'
    return false
  }
  if (!forceReload && musyrifEkskulState.hasAccess) {
    if (btn) btn.style.display = ''
    return true
  }
  let accessRes = await sb
    .from(EKSKUL_TABLE)
    .select('id')
    .or(`pj_karyawan_id.eq.${String(profile.id)},pj_karyawan_id_2.eq.${String(profile.id)}`)
    .eq('aktif', true)
    .limit(1)
  if (accessRes.error && isMusyrifEkskulMissingPj2ColumnError(accessRes.error)) {
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
    musyrifEkskulState.hasAccess = false
    return false
  }
  const hasAccess = (data || []).length > 0
  musyrifEkskulState.hasAccess = hasAccess
  if (btn) btn.style.display = hasAccess ? '' : 'none'
  return hasAccess
}

async function setupMusyrifPrestasiPelanggaranAccess(_forceReload = false) {
  const btn = document.getElementById('musyrif-nav-prestasi-pelanggaran')
  const isAllowed = parseRoleList().some(isWakasekKesantrianRole)
  if (btn) btn.style.display = isAllowed ? '' : 'none'
  return isAllowed
}

function getMusyrifPrestasiSantriLabel(item) {
  const kelas = musyrifPrestasiPelanggaranState.kelasMap.get(String(item?.kelas_id || ''))
  return `${String(item?.nama || '-')} (${String(kelas?.nama_kelas || '-')})`
}

function formatMusyrifPrestasiDate(value) {
  const text = String(value || '').slice(0, 10)
  if (!text) return '-'
  const date = new Date(`${text}T00:00:00`)
  if (Number.isNaN(date.getTime())) return text
  return date.toLocaleDateString('id-ID', { day: '2-digit', month: 'short', year: 'numeric' })
}

function resetMusyrifPrestasiForm() {
  musyrifPrestasiPelanggaranState.editingPrestasiId = ''
  const fields = [
    ['spp-prestasi-kelas-filter', ''],
    ['spp-prestasi-santri-search', ''],
    ['spp-prestasi-waktu', getMusyrifTodayDate()],
    ['spp-prestasi-judul', ''],
    ['spp-prestasi-sertifikat', '']
  ]
  fields.forEach(([id, value]) => {
    const el = document.getElementById(id)
    if (el) el.value = value
  })
  renderMusyrifPrestasiSantriSearchList('prestasi')
}

function resetMusyrifPelanggaranForm() {
  musyrifPrestasiPelanggaranState.editingPelanggaranId = ''
  const fields = [
    ['spp-pelanggaran-kelas-filter', ''],
    ['spp-pelanggaran-santri-search', ''],
    ['spp-pelanggaran-waktu', getMusyrifTodayDate()],
    ['spp-pelanggaran-judul', ''],
    ['spp-pelanggaran-hukuman', ''],
    ['spp-pelanggaran-surat-jenis', ''],
    ['spp-pelanggaran-surat-url', '']
  ]
  fields.forEach(([id, value]) => {
    const el = document.getElementById(id)
    if (el) el.value = value
  })
  renderMusyrifPrestasiSantriSearchList('pelanggaran')
}

function setMusyrifPrestasiTab(tab) {
  const mode = tab === 'pelanggaran' ? 'pelanggaran' : 'prestasi'
  musyrifPrestasiPelanggaranState.tab = mode
  const prestasiPanel = document.getElementById('spp-panel-prestasi')
  const pelanggaranPanel = document.getElementById('spp-panel-pelanggaran')
  const prestasiList = document.getElementById('spp-list-prestasi')
  const pelanggaranList = document.getElementById('spp-list-pelanggaran')
  const btnPrestasi = document.getElementById('spp-tab-prestasi')
  const btnPelanggaran = document.getElementById('spp-tab-pelanggaran')
  if (prestasiPanel) prestasiPanel.style.display = mode === 'prestasi' ? 'block' : 'none'
  if (pelanggaranPanel) pelanggaranPanel.style.display = mode === 'pelanggaran' ? 'block' : 'none'
  if (prestasiList) prestasiList.style.display = mode === 'prestasi' ? 'block' : 'none'
  if (pelanggaranList) pelanggaranList.style.display = mode === 'pelanggaran' ? 'block' : 'none'
  if (btnPrestasi) btnPrestasi.classList.toggle('modal-btn-primary', mode === 'prestasi')
  if (btnPelanggaran) btnPelanggaran.classList.toggle('modal-btn-primary', mode === 'pelanggaran')
}

function renderMusyrifPrestasiSantriSearchList(mode) {
  const isPrestasi = mode === 'prestasi'
  const filterEl = document.getElementById(isPrestasi ? 'spp-prestasi-kelas-filter' : 'spp-pelanggaran-kelas-filter')
  const listEl = document.getElementById(isPrestasi ? 'spp-prestasi-santri-list' : 'spp-pelanggaran-santri-list')
  if (!filterEl || !listEl) return
  const kelasId = String(filterEl.value || '').trim()
  const rows = (musyrifPrestasiPelanggaranState.santriRows || []).filter(item => !kelasId || String(item.kelas_id || '') === kelasId)
  listEl.innerHTML = rows.map(item => `<option value="${escapeHtml(getMusyrifPrestasiSantriLabel(item))}"></option>`).join('')
}

function onMusyrifPrestasiClassFilterChange(mode) {
  renderMusyrifPrestasiSantriSearchList(mode)
}

function resolveMusyrifPrestasiSantriId(mode) {
  const isPrestasi = mode === 'prestasi'
  const searchEl = document.getElementById(isPrestasi ? 'spp-prestasi-santri-search' : 'spp-pelanggaran-santri-search')
  const filterEl = document.getElementById(isPrestasi ? 'spp-prestasi-kelas-filter' : 'spp-pelanggaran-kelas-filter')
  const text = String(searchEl?.value || '').trim().toLowerCase()
  const kelasId = String(filterEl?.value || '').trim()
  if (!text) return ''
  const rows = (musyrifPrestasiPelanggaranState.santriRows || []).filter(item => !kelasId || String(item.kelas_id || '') === kelasId)
  const exactLabel = rows.find(item => getMusyrifPrestasiSantriLabel(item).toLowerCase() === text)
  if (exactLabel?.id) return String(exactLabel.id)
  const exactName = rows.filter(item => String(item.nama || '').trim().toLowerCase() === text)
  if (exactName.length === 1) return String(exactName[0].id || '')
  return ''
}

function openMusyrifPrestasiDoc(url) {
  const link = String(url || '').trim()
  if (!link) {
    alert('Dokumen belum tersedia.')
    return
  }
  const overlay = document.getElementById('spp-doc-overlay')
  const frame = document.getElementById('spp-doc-frame')
  const dl = document.getElementById('spp-doc-download')
  if (!overlay || !frame || !dl) return
  frame.src = link
  dl.href = link
  overlay.style.display = 'block'
}

function closeMusyrifPrestasiDoc() {
  const overlay = document.getElementById('spp-doc-overlay')
  const frame = document.getElementById('spp-doc-frame')
  if (!overlay || !frame) return
  frame.src = 'about:blank'
  overlay.style.display = 'none'
}

async function uploadMusyrifPelanggaranSuratFile(event) {
  const file = event?.target?.files?.[0]
  if (!file) return
  try {
    const ext = String(file.name || '').split('.').pop() || 'bin'
    const filePath = `surat/${Date.now()}-${Math.random().toString(36).slice(2, 8)}.${ext}`
    const { error: uploadError } = await sb.storage.from(SANTRI_SURAT_BUCKET).upload(filePath, file, { upsert: true, cacheControl: '3600' })
    if (uploadError) throw uploadError
    const { data } = sb.storage.from(SANTRI_SURAT_BUCKET).getPublicUrl(filePath)
    const input = document.getElementById('spp-pelanggaran-surat-url')
    if (input) input.value = String(data?.publicUrl || '')
    alert('Upload surat berhasil.')
  } catch (error) {
    console.error(error)
    alert(`Gagal upload surat: ${error?.message || 'Unknown error'}`)
  } finally {
    event.target.value = ''
  }
}

function renderMusyrifPrestasiLists() {
  const santriMap = new Map((musyrifPrestasiPelanggaranState.santriRows || []).map(item => [String(item.id || ''), item]))
  const prestasiBox = document.getElementById('spp-list-prestasi')
  const pelanggaranBox = document.getElementById('spp-list-pelanggaran')
  if (prestasiBox) {
    const rows = musyrifPrestasiPelanggaranState.prestasiRows || []
    prestasiBox.innerHTML = rows.length ? `
      <div style="overflow:auto;"><table style="width:100%; min-width:980px; border-collapse:collapse; font-size:12px;"><thead><tr style="background:#f8fafc;"><th style="padding:7px; border:1px solid #e2e8f0;">No</th><th style="padding:7px; border:1px solid #e2e8f0;">Santri</th><th style="padding:7px; border:1px solid #e2e8f0;">Waktu</th><th style="padding:7px; border:1px solid #e2e8f0;">Kategori</th><th style="padding:7px; border:1px solid #e2e8f0;">Prestasi</th><th style="padding:7px; border:1px solid #e2e8f0;">Sertifikat</th><th style="padding:7px; border:1px solid #e2e8f0;">Aksi</th></tr></thead><tbody>
      ${rows.map((row, idx) => `<tr><td style="padding:7px; border:1px solid #e2e8f0; text-align:center;">${idx + 1}</td><td style="padding:7px; border:1px solid #e2e8f0;">${escapeHtml(String(santriMap.get(String(row.santri_id || ''))?.nama || '-'))}</td><td style="padding:7px; border:1px solid #e2e8f0;">${escapeHtml(formatMusyrifPrestasiDate(row.waktu))}</td><td style="padding:7px; border:1px solid #e2e8f0;">Kesantrian</td><td style="padding:7px; border:1px solid #e2e8f0;">${escapeHtml(String(row.judul || '-'))}</td><td style="padding:7px; border:1px solid #e2e8f0;"><button type="button" class="modal-btn" onclick="openMusyrifPrestasiDoc('${escapeHtml(String(row.sertifikat_url || ''))}')">Lihat</button></td><td style="padding:7px; border:1px solid #e2e8f0;"><button type="button" class="modal-btn" onclick="editMusyrifPrestasiEntry('${escapeHtml(String(row.id || ''))}')">Edit</button><button type="button" class="modal-btn" style="margin-left:6px;" onclick="deleteMusyrifPrestasiEntry('${escapeHtml(String(row.id || ''))}')">Hapus</button></td></tr>`).join('')}
      </tbody></table></div>
    ` : '<div style="color:#64748b;">Belum ada data prestasi kesantrian.</div>'
  }
  if (pelanggaranBox) {
    const rows = musyrifPrestasiPelanggaranState.pelanggaranRows || []
    pelanggaranBox.innerHTML = rows.length ? `
      <div style="overflow:auto;"><table style="width:100%; min-width:1160px; border-collapse:collapse; font-size:12px;"><thead><tr style="background:#f8fafc;"><th style="padding:7px; border:1px solid #e2e8f0;">No</th><th style="padding:7px; border:1px solid #e2e8f0;">Santri</th><th style="padding:7px; border:1px solid #e2e8f0;">Waktu</th><th style="padding:7px; border:1px solid #e2e8f0;">Kategori</th><th style="padding:7px; border:1px solid #e2e8f0;">Pelanggaran</th><th style="padding:7px; border:1px solid #e2e8f0;">Hukuman</th><th style="padding:7px; border:1px solid #e2e8f0;">Surat</th><th style="padding:7px; border:1px solid #e2e8f0;">Dokumen</th><th style="padding:7px; border:1px solid #e2e8f0;">Aksi</th></tr></thead><tbody>
      ${rows.map((row, idx) => `<tr><td style="padding:7px; border:1px solid #e2e8f0; text-align:center;">${idx + 1}</td><td style="padding:7px; border:1px solid #e2e8f0;">${escapeHtml(String(santriMap.get(String(row.santri_id || ''))?.nama || '-'))}</td><td style="padding:7px; border:1px solid #e2e8f0;">${escapeHtml(formatMusyrifPrestasiDate(row.waktu))}</td><td style="padding:7px; border:1px solid #e2e8f0;">Kesantrian</td><td style="padding:7px; border:1px solid #e2e8f0;">${escapeHtml(String(row.judul || '-'))}</td><td style="padding:7px; border:1px solid #e2e8f0;">${escapeHtml(String(row.hukuman || '-'))}</td><td style="padding:7px; border:1px solid #e2e8f0;">${escapeHtml(String(row.surat_jenis || '-'))}</td><td style="padding:7px; border:1px solid #e2e8f0;"><button type="button" class="modal-btn" onclick="openMusyrifPrestasiDoc('${escapeHtml(String(row.surat_url || ''))}')">Lihat</button></td><td style="padding:7px; border:1px solid #e2e8f0;"><button type="button" class="modal-btn" onclick="editMusyrifPelanggaranEntry('${escapeHtml(String(row.id || ''))}')">Edit</button><button type="button" class="modal-btn" style="margin-left:6px;" onclick="deleteMusyrifPelanggaranEntry('${escapeHtml(String(row.id || ''))}')">Hapus</button></td></tr>`).join('')}
      </tbody></table></div>
    ` : '<div style="color:#64748b;">Belum ada data pelanggaran kesantrian.</div>'
  }
}

async function loadMusyrifPrestasiPelanggaranData() {
  const [santriRes, kelasRes, prestasiRes, pelanggaranRes] = await Promise.all([
    sb.from('santri').select('id, nama, kelas_id, aktif').eq('aktif', true).order('nama'),
    sb.from('kelas').select('id, nama_kelas').order('nama_kelas'),
    sb.from(SANTRI_PRESTASI_TABLE).select('id, santri_id, waktu, kategori, judul, sertifikat_url, created_at').eq('kategori', 'kemusyrifan').order('waktu', { ascending: false }).order('created_at', { ascending: false }),
    sb.from(SANTRI_PELANGGARAN_TABLE).select('id, santri_id, waktu, kategori, judul, hukuman, surat_jenis, surat_url, created_at').eq('kategori', 'kemusyrifan').order('waktu', { ascending: false }).order('created_at', { ascending: false })
  ])
  const err = santriRes.error || kelasRes.error || prestasiRes.error || pelanggaranRes.error
  if (err) throw err
  musyrifPrestasiPelanggaranState.santriRows = santriRes.data || []
  musyrifPrestasiPelanggaranState.kelasMap = new Map((kelasRes.data || []).map(item => [String(item.id || ''), item]))
  musyrifPrestasiPelanggaranState.prestasiRows = prestasiRes.data || []
  musyrifPrestasiPelanggaranState.pelanggaranRows = pelanggaranRes.data || []
}

async function saveMusyrifPrestasiEntry() {
  const santriId = resolveMusyrifPrestasiSantriId('prestasi')
  const waktu = String(document.getElementById('spp-prestasi-waktu')?.value || '').trim()
  const judul = String(document.getElementById('spp-prestasi-judul')?.value || '').trim()
  const sertifikatUrl = String(document.getElementById('spp-prestasi-sertifikat')?.value || '').trim()
  if (!santriId || !waktu || !judul) {
    alert('Nama santri, waktu, dan prestasi wajib diisi.')
    return
  }
  const payload = { santri_id: santriId, waktu, kategori: 'kemusyrifan', judul, sertifikat_url: sertifikatUrl || null }
  const res = musyrifPrestasiPelanggaranState.editingPrestasiId
    ? await sb.from(SANTRI_PRESTASI_TABLE).update(payload).eq('id', musyrifPrestasiPelanggaranState.editingPrestasiId)
    : await sb.from(SANTRI_PRESTASI_TABLE).insert([payload])
  if (res.error) {
    alert(`Gagal simpan prestasi: ${res.error.message || 'Unknown error'}`)
    return
  }
  resetMusyrifPrestasiForm()
  await loadMusyrifPrestasiPelanggaranData()
  renderMusyrifPrestasiLists()
}

async function saveMusyrifPelanggaranEntry() {
  const santriId = resolveMusyrifPrestasiSantriId('pelanggaran')
  const waktu = String(document.getElementById('spp-pelanggaran-waktu')?.value || '').trim()
  const judul = String(document.getElementById('spp-pelanggaran-judul')?.value || '').trim()
  const hukuman = String(document.getElementById('spp-pelanggaran-hukuman')?.value || '').trim()
  const suratJenis = String(document.getElementById('spp-pelanggaran-surat-jenis')?.value || '').trim()
  const suratUrl = String(document.getElementById('spp-pelanggaran-surat-url')?.value || '').trim()
  if (!santriId || !waktu || !judul) {
    alert('Nama santri, waktu, dan pelanggaran wajib diisi.')
    return
  }
  const payload = { santri_id: santriId, waktu, kategori: 'kemusyrifan', judul, hukuman: hukuman || null, surat_jenis: suratJenis || null, surat_url: suratUrl || null }
  const res = musyrifPrestasiPelanggaranState.editingPelanggaranId
    ? await sb.from(SANTRI_PELANGGARAN_TABLE).update(payload).eq('id', musyrifPrestasiPelanggaranState.editingPelanggaranId)
    : await sb.from(SANTRI_PELANGGARAN_TABLE).insert([payload])
  if (res.error) {
    alert(`Gagal simpan pelanggaran: ${res.error.message || 'Unknown error'}`)
    return
  }
  resetMusyrifPelanggaranForm()
  await loadMusyrifPrestasiPelanggaranData()
  renderMusyrifPrestasiLists()
}

function editMusyrifPrestasiEntry(id) {
  const row = (musyrifPrestasiPelanggaranState.prestasiRows || []).find(item => String(item.id || '') === String(id || ''))
  if (!row) return
  const santri = (musyrifPrestasiPelanggaranState.santriRows || []).find(item => String(item.id || '') === String(row.santri_id || ''))
  musyrifPrestasiPelanggaranState.editingPrestasiId = String(row.id || '')
  const kelasFilter = document.getElementById('spp-prestasi-kelas-filter')
  if (kelasFilter) kelasFilter.value = String(santri?.kelas_id || '')
  renderMusyrifPrestasiSantriSearchList('prestasi')
  const search = document.getElementById('spp-prestasi-santri-search')
  if (search) search.value = getMusyrifPrestasiSantriLabel(santri || {})
  document.getElementById('spp-prestasi-waktu').value = String(row.waktu || '').slice(0, 10)
  document.getElementById('spp-prestasi-judul').value = String(row.judul || '')
  document.getElementById('spp-prestasi-sertifikat').value = String(row.sertifikat_url || '')
}

function editMusyrifPelanggaranEntry(id) {
  const row = (musyrifPrestasiPelanggaranState.pelanggaranRows || []).find(item => String(item.id || '') === String(id || ''))
  if (!row) return
  const santri = (musyrifPrestasiPelanggaranState.santriRows || []).find(item => String(item.id || '') === String(row.santri_id || ''))
  musyrifPrestasiPelanggaranState.editingPelanggaranId = String(row.id || '')
  const kelasFilter = document.getElementById('spp-pelanggaran-kelas-filter')
  if (kelasFilter) kelasFilter.value = String(santri?.kelas_id || '')
  renderMusyrifPrestasiSantriSearchList('pelanggaran')
  const search = document.getElementById('spp-pelanggaran-santri-search')
  if (search) search.value = getMusyrifPrestasiSantriLabel(santri || {})
  document.getElementById('spp-pelanggaran-waktu').value = String(row.waktu || '').slice(0, 10)
  document.getElementById('spp-pelanggaran-judul').value = String(row.judul || '')
  document.getElementById('spp-pelanggaran-hukuman').value = String(row.hukuman || '')
  document.getElementById('spp-pelanggaran-surat-jenis').value = String(row.surat_jenis || '')
  document.getElementById('spp-pelanggaran-surat-url').value = String(row.surat_url || '')
}

async function deleteMusyrifPrestasiEntry(id) {
  if (!confirm('Hapus data prestasi ini?')) return
  const { error } = await sb.from(SANTRI_PRESTASI_TABLE).delete().eq('id', String(id || ''))
  if (error) {
    alert(`Gagal hapus prestasi: ${error.message || 'Unknown error'}`)
    return
  }
  await loadMusyrifPrestasiPelanggaranData()
  renderMusyrifPrestasiLists()
}

async function deleteMusyrifPelanggaranEntry(id) {
  if (!confirm('Hapus data pelanggaran ini?')) return
  const { error } = await sb.from(SANTRI_PELANGGARAN_TABLE).delete().eq('id', String(id || ''))
  if (error) {
    alert(`Gagal hapus pelanggaran: ${error.message || 'Unknown error'}`)
    return
  }
  await loadMusyrifPrestasiPelanggaranData()
  renderMusyrifPrestasiLists()
}

async function renderMusyrifPrestasiPelanggaranPage() {
  const content = document.getElementById('musyrif-content')
  if (!content) return
  content.innerHTML = '<div class="placeholder-card">Loading prestasi & pelanggaran...</div>'
  try {
    await loadMusyrifPrestasiPelanggaranData()
    content.innerHTML = `
      <div style="display:grid; gap:12px;">
        <div style="display:flex; gap:8px; flex-wrap:wrap;">
          <button id="spp-tab-prestasi" type="button" class="modal-btn modal-btn-primary" onclick="setMusyrifPrestasiTab('prestasi')">Prestasi</button>
          <button id="spp-tab-pelanggaran" type="button" class="modal-btn" onclick="setMusyrifPrestasiTab('pelanggaran')">Pelanggaran</button>
        </div>
        <div id="spp-panel-prestasi" style="border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">
          <div style="font-weight:700; margin-bottom:8px;">Input Prestasi Kesantrian</div>
          <div style="display:grid; grid-template-columns:repeat(auto-fit,minmax(220px,1fr)); gap:10px;">
            <div><label class="guru-label">Filter Kelas</label><select id="spp-prestasi-kelas-filter" class="guru-field" onchange="onMusyrifPrestasiClassFilterChange('prestasi')"></select></div>
            <div><label class="guru-label">Nama Santri (Ketik / Cari)</label><input id="spp-prestasi-santri-search" class="guru-field" type="text" list="spp-prestasi-santri-list" placeholder="Ketik nama santri..."><datalist id="spp-prestasi-santri-list"></datalist></div>
            <div><label class="guru-label">Waktu</label><input id="spp-prestasi-waktu" class="guru-field" type="date" value="${escapeHtml(getMusyrifTodayDate())}"></div>
            <div><label class="guru-label">Kategori</label><input class="guru-field" type="text" value="Kesantrian" readonly style="background:#f8fafc;color:#475569;"></div>
            <div style="grid-column:1/-1;"><label class="guru-label">Prestasi</label><input id="spp-prestasi-judul" class="guru-field" type="text" placeholder="Contoh: Teladan Adab Asrama"></div>
            <div style="grid-column:1/-1;"><label class="guru-label">Sertifikat (URL File)</label><input id="spp-prestasi-sertifikat" class="guru-field" type="text" placeholder="https://..."></div>
          </div>
          <div style="display:flex; gap:8px; margin-top:10px;"><button type="button" class="modal-btn modal-btn-primary" onclick="saveMusyrifPrestasiEntry()">Simpan</button><button type="button" class="modal-btn" onclick="resetMusyrifPrestasiForm()">Reset</button></div>
        </div>
        <div id="spp-panel-pelanggaran" style="display:none; border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">
          <div style="font-weight:700; margin-bottom:8px;">Input Pelanggaran Kesantrian</div>
          <div style="display:grid; grid-template-columns:repeat(auto-fit,minmax(220px,1fr)); gap:10px;">
            <div><label class="guru-label">Filter Kelas</label><select id="spp-pelanggaran-kelas-filter" class="guru-field" onchange="onMusyrifPrestasiClassFilterChange('pelanggaran')"></select></div>
            <div><label class="guru-label">Nama Santri (Ketik / Cari)</label><input id="spp-pelanggaran-santri-search" class="guru-field" type="text" list="spp-pelanggaran-santri-list" placeholder="Ketik nama santri..."><datalist id="spp-pelanggaran-santri-list"></datalist></div>
            <div><label class="guru-label">Waktu</label><input id="spp-pelanggaran-waktu" class="guru-field" type="date" value="${escapeHtml(getMusyrifTodayDate())}"></div>
            <div><label class="guru-label">Kategori</label><input class="guru-field" type="text" value="Kesantrian" readonly style="background:#f8fafc;color:#475569;"></div>
            <div><label class="guru-label">Pelanggaran</label><input id="spp-pelanggaran-judul" class="guru-field" type="text" placeholder="Deskripsi pelanggaran"></div>
            <div><label class="guru-label">Jenis Hukuman</label><input id="spp-pelanggaran-hukuman" class="guru-field" type="text" placeholder="Jenis hukuman"></div>
            <div><label class="guru-label">Surat Peringatan</label><select id="spp-pelanggaran-surat-jenis" class="guru-field"><option value="">- Pilih -</option><option value="Sanksi dan Teguran">Sanksi dan Teguran</option><option value="ST1">ST1</option><option value="ST2">ST2</option><option value="ST3">ST3</option><option value="SP1">SP1</option><option value="SP2">SP2</option><option value="SP3">SP3</option><option value="DO">DO</option></select></div>
            <div style="grid-column:1/-1;"><label class="guru-label">Surat Pemberitahuan (URL File)</label><input id="spp-pelanggaran-surat-url" class="guru-field" type="text" placeholder="https://..."><div style="margin-top:8px;"><input id="spp-pelanggaran-surat-file" type="file" accept=".pdf,.jpg,.jpeg,.png,.webp" style="display:none;" onchange="uploadMusyrifPelanggaranSuratFile(event)"><button type="button" class="modal-btn" onclick="document.getElementById('spp-pelanggaran-surat-file')?.click()">Upload File</button></div></div>
          </div>
          <div style="display:flex; gap:8px; margin-top:10px;"><button type="button" class="modal-btn modal-btn-primary" onclick="saveMusyrifPelanggaranEntry()">Simpan</button><button type="button" class="modal-btn" onclick="resetMusyrifPelanggaranForm()">Reset</button></div>
        </div>
        <div id="spp-list-prestasi" style="border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">Loading...</div>
        <div id="spp-list-pelanggaran" style="display:none; border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">Loading...</div>
      </div>
      <div id="spp-doc-overlay" style="display:none; position:fixed; inset:0; background:rgba(15,23,42,0.45); z-index:3000; padding:20px;">
        <div style="max-width:960px; margin:0 auto; background:#fff; border:1px solid #e2e8f0; border-radius:12px; overflow:hidden;">
          <div style="display:flex; justify-content:space-between; padding:12px; border-bottom:1px solid #e2e8f0;"><div style="font-weight:700;">Dokumen</div><div style="display:flex; gap:8px;"><a id="spp-doc-download" href="#" target="_blank" rel="noopener" class="modal-btn">Download</a><button type="button" class="modal-btn" onclick="closeMusyrifPrestasiDoc()">Tutup</button></div></div>
          <div style="padding:12px;"><iframe id="spp-doc-frame" title="Dokumen" style="width:100%; height:70vh; border:1px solid #e2e8f0; border-radius:8px;"></iframe></div>
        </div>
      </div>
    `
    const kelasOptions = ['<option value="">Semua Kelas</option>', ...Array.from(musyrifPrestasiPelanggaranState.kelasMap.values()).map(item => `<option value="${escapeHtml(String(item.id || ''))}">${escapeHtml(String(item.nama_kelas || '-'))}</option>`)]
    const prestasiFilter = document.getElementById('spp-prestasi-kelas-filter')
    const pelanggaranFilter = document.getElementById('spp-pelanggaran-kelas-filter')
    if (prestasiFilter) prestasiFilter.innerHTML = kelasOptions.join('')
    if (pelanggaranFilter) pelanggaranFilter.innerHTML = kelasOptions.join('')
    renderMusyrifPrestasiSantriSearchList('prestasi')
    renderMusyrifPrestasiSantriSearchList('pelanggaran')
    renderMusyrifPrestasiLists()
    setMusyrifPrestasiTab('prestasi')
  } catch (error) {
    console.error(error)
    content.innerHTML = `<div class="placeholder-card">Gagal load prestasi & pelanggaran: ${escapeHtml(error?.message || 'Unknown error')}</div>`
  }
}

function getMusyrifSelectedEkskul() {
  const eid = String(musyrifEkskulState.selectedEkskulId || '')
  return (musyrifEkskulState.ekskulRows || []).find(item => String(item.id || '') === eid) || null
}

function renderMusyrifEkskulMemberList() {
  const box = document.getElementById('musyrif-ekskul-member-list')
  if (!box) return
  const selected = getMusyrifSelectedEkskul()
  if (!selected) {
    box.innerHTML = '<div style="color:#64748b; font-size:12px;">Pilih ekskul terlebih dahulu.</div>'
    return
  }
  const santriMap = new Map((musyrifEkskulState.santriRows || []).map(item => [String(item.id || ''), item]))
  const rows = (musyrifEkskulState.memberRows || []).filter(item => String(item.ekskul_id || '') === String(selected.id || ''))
  if (!rows.length) {
    box.innerHTML = '<div style="color:#64748b; font-size:12px;">Belum ada anggota.</div>'
    return
  }
  box.innerHTML = rows.map((item, idx) => {
    const sid = String(item.santri_id || '')
    const isActive = String(musyrifEkskulState.selectedSantriId || '') === sid
    return `<div style=\"display:grid; grid-template-columns:1fr auto; gap:6px; align-items:center; padding:8px; border:1px solid #e2e8f0; border-radius:8px; margin-bottom:6px; background:#fff;\"><button type=\"button\" class=\"modal-btn\" onclick=\"selectMusyrifEkskulSantri('${escapeHtml(sid)}')\" style=\"display:block; width:100%; text-align:left; font-size:13px; ${isActive ? 'border-color:#d4d456; background:#fefce8; font-weight:700;' : 'font-weight:500;'}\">${idx + 1}. ${escapeHtml(String(santriMap.get(sid)?.nama || '-'))}</button><button type=\"button\" class=\"modal-btn\" onclick=\"openMusyrifEkskulSantriDetail('${escapeHtml(sid)}')\">Detail</button></div>`
  }).join('')
}

function renderMusyrifEkskulSelects() {
  const selected = getMusyrifSelectedEkskul()
  const santriSel = document.getElementById('musyrif-ekskul-santri')
  if (santriSel) {
    if (!selected) {
      santriSel.innerHTML = '<option value=\"\">Pilih ekskul dulu</option>'
    } else {
      const joined = new Set((musyrifEkskulState.memberRows || []).filter(item => String(item.ekskul_id || '') === String(selected.id || '')).map(item => String(item.santri_id || '')))
      const options = ['<option value=\"\">Pilih siswa...</option>']
      ;(musyrifEkskulState.santriRows || []).forEach(item => {
        const sid = String(item.id || '')
        if (!sid || joined.has(sid)) return
        options.push(`<option value=\"${escapeHtml(sid)}\">${escapeHtml(String(item.nama || '-'))}</option>`)
      })
      santriSel.innerHTML = options.join('')
    }
  }
  const indikatorRows = (musyrifEkskulState.indikatorRows || []).filter(item => String(item.ekskul_id || '') === String(selected?.id || ''))
  const indikatorBox = document.getElementById('musyrif-ekskul-indikator-list')
  if (indikatorBox) {
    indikatorBox.innerHTML = indikatorRows.length
      ? indikatorRows.map(item => `<div style=\"padding:6px 8px; border:1px solid #e2e8f0; border-radius:8px; margin-bottom:6px;\"><div style=\"font-weight:600; font-size:13px;\">${escapeHtml(String(item.nama || '-'))}</div><div style=\"font-size:12px; color:#64748b;\">${escapeHtml(String(item.deskripsi || '-'))}</div></div>`).join('')
      : '<div style=\"color:#64748b; font-size:12px;\">Belum ada indikator.</div>'
  }
}

function renderMusyrifEkskulProgressList() {
  const box = document.getElementById('musyrif-ekskul-progres-list')
  if (!box) return
  const eid = String(musyrifEkskulState.selectedEkskulId || '')
  const sid = String(musyrifEkskulState.selectedSantriId || '')
  if (!eid || !sid) {
    box.innerHTML = '<div style=\"color:#64748b; font-size:12px;\">Pilih anggota untuk melihat progres.</div>'
    return
  }
  const indikatorMap = new Map((musyrifEkskulState.indikatorRows || []).map(item => [String(item.id || ''), item]))
  const rows = (musyrifEkskulState.progressRows || []).filter(item => String(item.ekskul_id || '') === eid && String(item.santri_id || '') === sid)
  if (!rows.length) {
    box.innerHTML = '<div style=\"color:#64748b; font-size:12px;\">Belum ada catatan progres.</div>'
    return
  }
  box.innerHTML = rows.map(item => `<div style=\"padding:8px; border:1px solid #e2e8f0; border-radius:8px; margin-bottom:6px;\"><div style=\"font-size:12px; color:#64748b;\">${escapeHtml(String(item.tanggal || '-'))} | ${escapeHtml(String(indikatorMap.get(String(item.indikator_id || ''))?.nama || 'Umum'))}</div><div style=\"margin-top:4px;\">${escapeHtml(String(item.catatan || '-'))}</div><div style=\"font-size:12px; color:#64748b;\">Nilai: ${escapeHtml(String(item.nilai ?? '-'))}</div></div>`).join('')
}

function musyrifEkskulUpdateIndicatorStars(inputEl) {
  if (!inputEl) return
  const wrap = inputEl.closest('[data-musyrif-ekskul-indikator-row="1"]')
  const starEl = wrap?.querySelector('[data-musyrif-ekskul-star-view="1"]')
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

function renderMusyrifEkskulProgressInputRows() {
  const box = document.getElementById('musyrif-ekskul-progres-input-list')
  if (!box) return
  const selected = getMusyrifSelectedEkskul()
  const sid = String(musyrifEkskulState.selectedSantriId || '').trim()
  if (!selected || !sid) {
    box.innerHTML = '<div style=\"color:#64748b; font-size:12px;\">Pilih santri terlebih dahulu.</div>'
    return
  }
  const santriMap = new Map((musyrifEkskulState.santriRows || []).map(item => [String(item.id || ''), item]))
  const indikatorRows = (musyrifEkskulState.indikatorRows || []).filter(item => String(item.ekskul_id || '') === String(selected.id || ''))
  if (!indikatorRows.length) {
    box.innerHTML = '<div style=\"color:#64748b; font-size:12px;\">Belum ada indikator.</div>'
    return
  }
  box.innerHTML = `
    <div style="margin-bottom:8px;"><strong>Santri:</strong> ${escapeHtml(String(santriMap.get(sid)?.nama || '-'))}</div>
    <div style="overflow:auto;">
      <table style="width:100%; min-width:900px; border-collapse:collapse; font-size:13px;">
        <thead><tr style="background:#f8fafc;"><th style="padding:8px; border:1px solid #e2e8f0; width:52px;">No</th><th style="padding:8px; border:1px solid #e2e8f0;">Indikator</th><th style="padding:8px; border:1px solid #e2e8f0; width:220px;">Nilai</th><th style="padding:8px; border:1px solid #e2e8f0;">Catatan</th></tr></thead>
        <tbody>
          ${indikatorRows.map((indikator, idx) => `<tr data-musyrif-ekskul-indikator-row="1" data-indikator-id="${escapeHtml(String(indikator.id || ''))}"><td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${idx + 1}</td><td style="padding:8px; border:1px solid #e2e8f0;"><div style="font-weight:600;">${escapeHtml(String(indikator.nama || '-'))}</div><div style="font-size:11px; color:#64748b;">${escapeHtml(String(indikator.deskripsi || '-'))}</div></td><td style="padding:8px; border:1px solid #e2e8f0;"><div style="display:grid; grid-template-columns:86px 1fr; gap:8px; align-items:center;"><input class="guru-field" type="number" min="1" max="100" step="1" placeholder="1-100" data-musyrif-ekskul-indikator-nilai="1" oninput="musyrifEkskulUpdateIndicatorStars(this)"><div data-musyrif-ekskul-star-view="1">${Array.from({ length: 5 }).map(() => '<span style="color:#cbd5e1; font-size:17px; line-height:1;">&#9733;</span>').join('')}</div></div></td><td style="padding:8px; border:1px solid #e2e8f0;"><input class="guru-field" type="text" placeholder="Catatan indikator" data-musyrif-ekskul-indikator-catatan="1"></td></tr>`).join('')}
        </tbody>
      </table>
    </div>
  `
}

function openMusyrifEkskulSantriDetail(sid) {
  selectMusyrifEkskulSantri(sid)
  const overlay = document.getElementById('musyrif-ekskul-santri-detail-overlay')
  const body = document.getElementById('musyrif-ekskul-santri-detail-body')
  if (!overlay || !body) return
  renderMusyrifEkskulProgressList()
  body.innerHTML = document.getElementById('musyrif-ekskul-progres-list')?.innerHTML || '-'
  overlay.style.display = 'block'
}

function closeMusyrifEkskulSantriDetail() {
  const overlay = document.getElementById('musyrif-ekskul-santri-detail-overlay')
  if (overlay) overlay.style.display = 'none'
}

async function selectMusyrifEkskul(id) {
  musyrifEkskulState.selectedEkskulId = String(id || '')
  musyrifEkskulState.selectedSantriId = ''
  renderMusyrifEkskulMemberList()
  renderMusyrifEkskulSelects()
  renderMusyrifEkskulProgressInputRows()
  renderMusyrifEkskulProgressList()
  try {
    await loadMusyrifEkskulMonthlyRows()
    renderMusyrifEkskulMonthlyRows()
  } catch (error) {
    console.error(error)
  }
}

function selectMusyrifEkskulSantri(sid) {
  musyrifEkskulState.selectedSantriId = String(sid || '')
  renderMusyrifEkskulMemberList()
  renderMusyrifEkskulProgressInputRows()
  renderMusyrifEkskulProgressList()
}

async function addMusyrifEkskulMember() {
  const eid = String(musyrifEkskulState.selectedEkskulId || '')
  const sid = String(document.getElementById('musyrif-ekskul-santri')?.value || '').trim()
  if (!eid || !sid) {
    alert('Pilih ekskul dan siswa terlebih dahulu.')
    return
  }
  const { error } = await sb.from(EKSKUL_MEMBER_TABLE).insert([{ ekskul_id: eid, santri_id: sid }])
  if (error) {
    alert(`Gagal tambah anggota: ${error.message || 'Unknown error'}`)
    return
  }
  await renderMusyrifEkskulPage(true)
}

async function addMusyrifEkskulIndikator() {
  const eid = String(musyrifEkskulState.selectedEkskulId || '')
  const nama = String(document.getElementById('musyrif-ekskul-indikator-nama')?.value || '').trim()
  const deskripsi = String(document.getElementById('musyrif-ekskul-indikator-deskripsi')?.value || '').trim()
  if (!eid || !nama) {
    alert('Pilih ekskul dan isi indikator.')
    return
  }
  const urutan = ((musyrifEkskulState.indikatorRows || []).filter(item => String(item.ekskul_id || '') === eid).length || 0) + 1
  const { error } = await sb.from(EKSKUL_INDIKATOR_TABLE).insert([{ ekskul_id: eid, nama, deskripsi: deskripsi || null, urutan }])
  if (error) {
    alert(`Gagal tambah indikator: ${error.message || 'Unknown error'}`)
    return
  }
  document.getElementById('musyrif-ekskul-indikator-nama').value = ''
  document.getElementById('musyrif-ekskul-indikator-deskripsi').value = ''
  await renderMusyrifEkskulPage(true)
}

async function saveMusyrifEkskulProgress() {
  const eid = String(musyrifEkskulState.selectedEkskulId || '')
  const sid = String(musyrifEkskulState.selectedSantriId || '')
  const tanggal = String(document.getElementById('musyrif-ekskul-progres-tanggal')?.value || '').trim()
  if (!eid || !sid || !tanggal) {
    alert('Lengkapi data progres.')
    return
  }
  const rowEls = Array.from(document.querySelectorAll('[data-musyrif-ekskul-indikator-row="1"]'))
  if (!rowEls.length) {
    alert('Belum ada indikator untuk diinput.')
    return
  }
  const payload = []
  rowEls.forEach(rowEl => {
    const indikatorId = String(rowEl.getAttribute('data-indikator-id') || '').trim() || null
    const nilaiRaw = String(rowEl.querySelector('[data-musyrif-ekskul-indikator-nilai="1"]')?.value || '').trim()
    const nilaiParsed = nilaiRaw ? Number(nilaiRaw) : null
    const nilai = Number.isFinite(nilaiParsed) ? Math.max(1, Math.min(100, Math.round(nilaiParsed))) : null
    const catatan = String(rowEl.querySelector('[data-musyrif-ekskul-indikator-catatan="1"]')?.value || '').trim()
    if (!catatan && !Number.isFinite(nilai)) return
    payload.push({
      ekskul_id: eid,
      santri_id: sid,
      indikator_id: indikatorId,
      tanggal,
      nilai: Number.isFinite(nilai) ? nilai : null,
      catatan: catatan || null,
      updated_by: String(musyrifState.profile?.id || '').trim() || null
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
  await renderMusyrifEkskulPage(true)
}

async function renderMusyrifEkskulPage(_forceReload = false) {
  const content = document.getElementById('musyrif-content')
  if (!content) return
  content.innerHTML = '<div class=\"placeholder-card\">Loading ekskul...</div>'
  try {
    const profile = musyrifState.profile || await getCurrentMusyrif()
    let exRes = await sb.from(EKSKUL_TABLE)
      .select('id, nama, deskripsi')
      .or(`pj_karyawan_id.eq.${String(profile?.id || '')},pj_karyawan_id_2.eq.${String(profile?.id || '')}`)
      .eq('aktif', true)
      .order('nama')
    if (exRes.error && isMusyrifEkskulMissingPj2ColumnError(exRes.error)) {
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

    musyrifEkskulState.ekskulRows = exRes.data || []
    const ekskulIds = musyrifEkskulState.ekskulRows.map(item => String(item.id || ''))
    musyrifEkskulState.memberRows = (memberRes.data || []).filter(item => ekskulIds.includes(String(item.ekskul_id || '')))
    musyrifEkskulState.indikatorRows = (indikatorRes.data || []).filter(item => ekskulIds.includes(String(item.ekskul_id || '')))
    musyrifEkskulState.santriRows = santriRes.data || []
    if (!musyrifEkskulState.ekskulRows.length) {
      content.innerHTML = '<div class=\"placeholder-card\">Anda belum ditetapkan sebagai PJ ekskul.</div>'
      return
    }
    if (!musyrifEkskulState.selectedEkskulId || !ekskulIds.includes(String(musyrifEkskulState.selectedEkskulId || ''))) {
      musyrifEkskulState.selectedEkskulId = String(musyrifEkskulState.ekskulRows[0]?.id || '')
      musyrifEkskulState.selectedSantriId = ''
    }
    const selected = getMusyrifSelectedEkskul()
    const { data: progressRows, error: progressError } = await sb
      .from(EKSKUL_PROGRES_TABLE)
      .select('id, ekskul_id, santri_id, indikator_id, tanggal, nilai, catatan, created_at')
      .eq('ekskul_id', String(selected?.id || ''))
      .order('tanggal', { ascending: false })
      .order('created_at', { ascending: false })
    if (progressError) throw progressError
    musyrifEkskulState.progressRows = progressRows || []
    if (!musyrifEkskulState.monthlyPeriode) musyrifEkskulState.monthlyPeriode = getMusyrifMonthPeriodToday()
    await loadMusyrifEkskulMonthlyRows()

    content.innerHTML = `
      <div style=\"display:grid; gap:12px;\">
        <div style=\"border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;\">
          <div style=\"font-weight:700; margin-bottom:8px;\">Ekskul Binaan</div>
          <div style=\"display:flex; flex-wrap:wrap; gap:8px;\">
            ${(musyrifEkskulState.ekskulRows || []).map(item => `<button type=\"button\" class=\"modal-btn\" onclick=\"selectMusyrifEkskul('${escapeHtml(String(item.id || ''))}')\" style=\"${String(musyrifEkskulState.selectedEkskulId || '') === String(item.id || '') ? 'border-color:#d4d456; background:#fefce8;' : ''}\">${escapeHtml(String(item.nama || '-'))}</button>`).join('')}
          </div>
        </div>

        <div style="display:flex; gap:8px; flex-wrap:wrap;">
          <button id="musyrif-ekskul-tab-btn-progres" type="button" class="modal-btn modal-btn-primary" onclick="setMusyrifEkskulTab('progres')">Input Progres</button>
          <button id="musyrif-ekskul-tab-btn-laporan" type="button" class="modal-btn" onclick="setMusyrifEkskulTab('laporan')">Laporan Bulanan Ekskul</button>
        </div>

        <div id="musyrif-ekskul-tab-progres" style=\"display:grid; gap:12px;\">
          <div style=\"display:grid; grid-template-columns:1fr 1fr; gap:12px;\">
            <div style=\"border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;\">
              <div style=\"font-weight:700; margin-bottom:8px;\">Anggota Ekskul</div>
              <div style=\"display:grid; grid-template-columns:1fr auto; gap:8px; margin-bottom:8px;\">
                <select id=\"musyrif-ekskul-santri\" class=\"guru-field\"></select>
                <button type=\"button\" class=\"modal-btn modal-btn-primary\" onclick=\"addMusyrifEkskulMember()\">Tambah</button>
              </div>
              <div id=\"musyrif-ekskul-member-list\">Loading...</div>
            </div>

            <div style=\"border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;\">
              <div style=\"font-weight:700; margin-bottom:8px;\">Indikator Penilaian</div>
              <input id=\"musyrif-ekskul-indikator-nama\" class=\"guru-field\" type=\"text\" placeholder=\"Nama indikator\">
              <input id=\"musyrif-ekskul-indikator-deskripsi\" class=\"guru-field\" type=\"text\" placeholder=\"Deskripsi indikator\" style=\"margin-top:6px;\">
              <button type=\"button\" class=\"modal-btn modal-btn-primary\" onclick=\"addMusyrifEkskulIndikator()\" style=\"margin-top:8px;\">Tambah Indikator</button>
              <div id=\"musyrif-ekskul-indikator-list\" style=\"margin-top:8px;\">Loading...</div>
            </div>
          </div>

          <div style=\"border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;\">
            <div style=\"font-weight:700; margin-bottom:8px;\">Progres Santri</div>
            <div style=\"display:grid; grid-template-columns:160px 1fr auto; gap:8px; margin-bottom:8px;\">
              <input id=\"musyrif-ekskul-progres-tanggal\" class=\"guru-field\" type=\"date\" value=\"${escapeHtml(getMusyrifTodayDate())}\">
              <button type=\"button\" class=\"modal-btn modal-btn-primary\" onclick=\"saveMusyrifEkskulProgress()\">Simpan</button>
            </div>
            <div id=\"musyrif-ekskul-progres-input-list\" style=\"margin-bottom:10px;\">Loading...</div>
            <div id=\"musyrif-ekskul-progres-list\" style=\"margin-top:10px;\">Loading...</div>
          </div>
        </div>

        <div id="musyrif-ekskul-tab-laporan" style="display:none; border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">
          <div style="font-weight:700; margin-bottom:8px;">Input Laporan Bulanan Ekskul</div>
          <div style="font-size:12px; color:#64748b; margin-bottom:8px;">Data ini akan dipakai di Detail Laporan Bulanan pada page guru (bagian D. Ekstrakulikuler).</div>
          <div style="display:grid; grid-template-columns:180px 1fr auto; gap:8px; margin-bottom:8px; align-items:end;">
            <div>
              <label class="guru-label">Periode Bulan</label>
              <input id="musyrif-ekskul-monthly-periode" class="guru-field" type="month" value="${escapeHtml(String(musyrifEkskulState.monthlyPeriode || getMusyrifMonthPeriodToday()))}" onchange="onMusyrifEkskulMonthlyPeriodeChange()">
            </div>
            <div></div>
            <div><button type="button" class="modal-btn modal-btn-primary" onclick="saveMusyrifEkskulMonthlyReport()">Simpan Laporan Bulanan</button></div>
          </div>
          <div id="musyrif-ekskul-monthly-list">Loading...</div>
        </div>
      </div>
      <div id=\"musyrif-ekskul-santri-detail-overlay\" style=\"display:none; position:fixed; inset:0; background:rgba(15,23,42,0.45); z-index:2000; padding:20px;\">
        <div style=\"max-width:900px; margin:0 auto; background:#fff; border-radius:12px; border:1px solid #e2e8f0; overflow:hidden;\">
          <div style=\"display:flex; align-items:center; justify-content:space-between; padding:12px 14px; border-bottom:1px solid #e2e8f0;\">
            <div style=\"font-weight:700; color:#0f172a;\">Detail Progres Santri</div>
            <button type=\"button\" class=\"modal-btn\" onclick=\"closeMusyrifEkskulSantriDetail()\">Tutup</button>
          </div>
          <div id=\"musyrif-ekskul-santri-detail-body\" style=\"padding:12px; max-height:70vh; overflow:auto;\">Loading...</div>
        </div>
      </div>
    `
    renderMusyrifEkskulMemberList()
    renderMusyrifEkskulSelects()
    renderMusyrifEkskulProgressInputRows()
    renderMusyrifEkskulProgressList()
    renderMusyrifEkskulMonthlyRows()
    setMusyrifEkskulTab(musyrifEkskulState.activeTab || 'progres')
  } catch (error) {
    console.error(error)
    if (isMusyrifEkskulMissingTableError(error)) {
      content.innerHTML = '<div class=\"placeholder-card\" style=\"white-space:pre-wrap;\">Tabel ekskul belum ada. Hubungi admin untuk setup tabel ekskul.</div>'
      return
    }
    content.innerHTML = `<div class=\"placeholder-card\">Gagal load ekskul: ${escapeHtml(error?.message || 'Unknown error')}</div>`
  }
}

function getMusyrifManageSelectedKamar() {
  const sid = String(musyrifManageKamarState.selectedKamarId || '')
  return (musyrifManageKamarState.kamarRows || []).find(item => String(item?.id || '') === sid) || null
}

function sortMusyrifManageSantriRows(rows = []) {
  return [...rows].sort((a, b) => {
    const kelasA = String(musyrifManageKamarState.kelasMap.get(String(a?.kelas_id || ''))?.nama_kelas || '')
    const kelasB = String(musyrifManageKamarState.kelasMap.get(String(b?.kelas_id || ''))?.nama_kelas || '')
    const byKelas = kelasA.localeCompare(kelasB, 'id')
    if (byKelas !== 0) return byKelas
    return String(a?.nama || '').localeCompare(String(b?.nama || ''), 'id')
  })
}

function renderMusyrifManageSelectedRowsHtml(rows = []) {
  return rows.map(item => {
    const sid = String(item?.id || '')
    const kelasNama = musyrifManageKamarState.kelasMap.get(String(item?.kelas_id || ''))?.nama_kelas || '-'
    return `
      <div class="ekskul-card-item" style="display:grid; grid-template-columns:1fr auto; gap:8px; align-items:flex-start; margin-bottom:8px; font-size:13px;">
        <span>
          <span style="font-weight:600;">${escapeHtml(item?.nama || '-')}</span>
          <span style="display:block; font-size:11px; color:#64748b;">${escapeHtml(kelasNama)}</span>
        </span>
        <button type="button" class="modal-btn modal-btn-danger" style="padding:2px 8px; min-width:auto; line-height:1; border-radius:999px;" onclick="removeMusyrifManageKamarSantri('${escapeHtml(sid)}')" title="Keluarkan dari kamar ini">x</button>
      </div>
    `
  }).join('')
}

function renderMusyrifManageAvailableRowsHtml(rows = []) {
  return rows.map(item => {
    const sid = String(item?.id || '')
    const kelasNama = musyrifManageKamarState.kelasMap.get(String(item?.kelas_id || ''))?.nama_kelas || '-'
    return `
      <label class="ekskul-card-item" style="display:block; margin-bottom:8px; font-size:13px;">
        <input type="checkbox" onchange="toggleMusyrifManageKamarSantri('${escapeHtml(sid)}', this.checked)">
        <span style="margin-left:6px; font-weight:600;">${escapeHtml(item?.nama || '-')}</span>
        <span style="display:block; margin-left:22px; font-size:11px; color:#64748b;">${escapeHtml(kelasNama)}</span>
      </label>
    `
  }).join('')
}

function renderMusyrifManageKamarPanels() {
  const selectedWrap = document.getElementById('musyrif-kamar-selected-list')
  const availableWrap = document.getElementById('musyrif-kamar-available-list')
  if (!selectedWrap || !availableWrap) return

  const selectedSet = musyrifManageKamarState.selectedSet || new Set()
  const blockedSet = musyrifManageKamarState.blockedSet || new Set()
  const search = String(musyrifManageKamarState.search || '').trim().toLowerCase()

  const selectedRows = sortMusyrifManageSantriRows(
    (musyrifManageKamarState.santriRows || []).filter(item => selectedSet.has(String(item?.id || '')))
  )
  selectedWrap.innerHTML = renderMusyrifManageSelectedRowsHtml(selectedRows) || '<div style="font-size:12px; color:#64748b;">Belum ada santri di kamar ini.</div>'

  const availableRows = sortMusyrifManageSantriRows(
    (musyrifManageKamarState.santriRows || []).filter(item => {
      const sid = String(item?.id || '')
      if (!sid) return false
      if (selectedSet.has(sid)) return false
      if (blockedSet.has(sid)) return false
      if (!search) return true
      return String(item?.nama || '').toLowerCase().includes(search)
    })
  )
  availableWrap.innerHTML = renderMusyrifManageAvailableRowsHtml(availableRows) || '<div style="font-size:12px; color:#64748b;">Tidak ada santri tersedia.</div>'
}

function selectMusyrifManageKamar(kamarId) {
  const sid = String(kamarId || '')
  if (!sid) return
  const memberRows = musyrifManageKamarState.memberRows || []
  musyrifManageKamarState.selectedKamarId = sid
  musyrifManageKamarState.selectedSet = new Set(
    memberRows
      .filter(item => String(item?.kamar_id || '') === sid)
      .map(item => String(item?.santri_id || ''))
      .filter(Boolean)
  )
  musyrifManageKamarState.blockedSet = new Set(
    memberRows
      .filter(item => String(item?.kamar_id || '') !== sid)
      .map(item => String(item?.santri_id || ''))
      .filter(Boolean)
  )
  musyrifManageKamarState.search = ''
  renderMusyrifManageKamarPage()
}

function searchMusyrifManageKamarSantri(keyword) {
  musyrifManageKamarState.search = String(keyword || '')
  renderMusyrifManageKamarPanels()
}

function toggleMusyrifManageKamarSantri(santriId, checked) {
  const sid = String(santriId || '').trim()
  if (!sid) return
  if (!(musyrifManageKamarState.selectedSet instanceof Set)) musyrifManageKamarState.selectedSet = new Set()
  if (checked) musyrifManageKamarState.selectedSet.add(sid)
  else musyrifManageKamarState.selectedSet.delete(sid)
  renderMusyrifManageKamarPanels()
}

function removeMusyrifManageKamarSantri(santriId) {
  const sid = String(santriId || '').trim()
  if (!sid) return
  if (!(musyrifManageKamarState.selectedSet instanceof Set)) musyrifManageKamarState.selectedSet = new Set()
  musyrifManageKamarState.selectedSet.delete(sid)
  renderMusyrifManageKamarPanels()
}

async function saveMusyrifManageKamarName() {
  const selected = getMusyrifManageSelectedKamar()
  const profileId = String(musyrifState?.profile?.id || '').trim()
  if (!selected || !profileId) return
  const nama = String(document.getElementById('musyrif-kamar-name-input')?.value || '').trim()
  if (!nama) {
    alert('Nama kamar wajib diisi.')
    return
  }
  const { error } = await sb
    .from(HALAQAH_TABLE)
    .update({ nama })
    .eq('id', String(selected.id))
    .eq('musyrif_id', profileId)
  if (error) {
    console.error(error)
    alert(`Gagal menyimpan nama kamar: ${error?.message || 'Unknown error'}`)
    return
  }
  await renderMusyrifManageKamarPage()
}

async function saveMusyrifManageKamarMembers() {
  const selected = getMusyrifManageSelectedKamar()
  if (!selected) return
  const kamarId = String(selected.id || '')
  const selectedSantriIds = [...(musyrifManageKamarState.selectedSet || new Set())]
    .map(item => String(item || '').trim())
    .filter(Boolean)

  const { error: deleteError } = await sb
    .from(HALAQAH_SANTRI_TABLE)
    .delete()
    .eq('kamar_id', kamarId)
  if (deleteError) {
    console.error(deleteError)
    alert(`Gagal reset anggota kamar: ${deleteError?.message || 'Unknown error'}`)
    return
  }

  if (selectedSantriIds.length) {
    const payload = selectedSantriIds.map(santriId => ({ kamar_id: kamarId, santri_id: santriId }))
    const { error: insertError } = await sb.from(HALAQAH_SANTRI_TABLE).insert(payload)
    if (insertError) {
      console.error(insertError)
      alert(`Gagal menyimpan anggota kamar: ${insertError?.message || 'Unknown error'}`)
      return
    }
  }

  await renderMusyrifManageKamarPage()
}

async function renderMusyrifManageKamarPage() {
  const content = document.getElementById('musyrif-content')
  if (!content) return
  const profileId = String(musyrifState?.profile?.id || '').trim()
  if (!profileId) {
    content.innerHTML = '<div class="placeholder-card">Profil musyrif tidak ditemukan.</div>'
    return
  }

  content.innerHTML = '<div class="placeholder-card">Memuat data kamar...</div>'
  try {
    const [kamarRes, memberRes, santriRes, kelasRes] = await Promise.all([
      sb.from(HALAQAH_TABLE).select('id, nama, musyrif_id').eq('musyrif_id', profileId).order('nama'),
      sb.from(HALAQAH_SANTRI_TABLE).select('kamar_id, santri_id'),
      sb.from('santri').select('id, nama, kelas_id, aktif').eq('aktif', true).order('nama'),
      sb.from('kelas').select('id, nama_kelas').order('nama_kelas')
    ])
    if (kamarRes.error) throw kamarRes.error
    if (memberRes.error) throw memberRes.error
    if (santriRes.error) throw santriRes.error
    if (kelasRes.error) throw kelasRes.error

    const kamarRows = kamarRes.data || []
    const memberRows = memberRes.data || []
    const santriRows = santriRes.data || []
    const kelasMap = new Map((kelasRes.data || []).map(item => [String(item?.id || ''), item]))

    musyrifManageKamarState.kamarRows = kamarRows
    musyrifManageKamarState.memberRows = memberRows
    musyrifManageKamarState.santriRows = santriRows
    musyrifManageKamarState.kelasMap = kelasMap

    const kamarIds = kamarRows.map(item => String(item?.id || ''))
    if (!kamarIds.length) {
      musyrifManageKamarState.selectedKamarId = ''
      musyrifManageKamarState.selectedSet = new Set()
      musyrifManageKamarState.blockedSet = new Set()
      content.innerHTML = '<div class="placeholder-card">Belum ada kamar yang ditugaskan untuk Anda.</div>'
      return
    }

    const selectedId = kamarIds.includes(String(musyrifManageKamarState.selectedKamarId || ''))
      ? String(musyrifManageKamarState.selectedKamarId || '')
      : kamarIds[0]
    musyrifManageKamarState.selectedKamarId = selectedId
    musyrifManageKamarState.selectedSet = new Set(
      memberRows
        .filter(item => String(item?.kamar_id || '') === selectedId)
        .map(item => String(item?.santri_id || ''))
        .filter(Boolean)
    )
    musyrifManageKamarState.blockedSet = new Set(
      memberRows
        .filter(item => String(item?.kamar_id || '') !== selectedId)
        .map(item => String(item?.santri_id || ''))
        .filter(Boolean)
    )

    const selected = getMusyrifManageSelectedKamar()
    content.innerHTML = `
      <div style="display:grid; gap:12px;">
        <div style="border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">
          <div style="font-weight:700; margin-bottom:8px;">Kamar Binaan</div>
          <div style="display:flex; gap:8px; flex-wrap:wrap;">
            ${(kamarRows || []).map(item => `<button type="button" class="modal-btn" onclick="selectMusyrifManageKamar('${escapeHtml(String(item?.id || ''))}')" style="${String(item?.id || '') === selectedId ? 'border-color:#d4d456; background:#fefce8;' : ''}">${escapeHtml(item?.nama || '-')}</button>`).join('')}
          </div>
        </div>
        <div style="border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">
          <div style="display:grid; grid-template-columns:1fr auto; gap:8px;">
            <input id="musyrif-kamar-name-input" class="guru-field" type="text" value="${escapeHtml(String(selected?.nama || ''))}" placeholder="Nama kamar">
            <button type="button" class="modal-btn modal-btn-primary" onclick="saveMusyrifManageKamarName()">Simpan Nama</button>
          </div>
        </div>
        <div style="display:grid; grid-template-columns:repeat(auto-fit, minmax(280px, 1fr)); gap:12px;">
          <div style="border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">
            <div style="font-weight:700; margin-bottom:8px;">Santri di Kamar Ini</div>
            <div id="musyrif-kamar-selected-list"></div>
          </div>
          <div style="border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">
            <div style="display:grid; grid-template-columns:1fr auto; gap:8px; margin-bottom:8px;">
              <input id="musyrif-kamar-search-input" class="guru-field" type="text" value="${escapeHtml(musyrifManageKamarState.search || '')}" placeholder="Cari nama santri..." oninput="searchMusyrifManageKamarSantri(this.value)">
              <button type="button" class="modal-btn modal-btn-secondary" onclick="renderMusyrifManageKamarPanels()">Sort Per Kelas</button>
            </div>
            <div id="musyrif-kamar-available-list" style="max-height:340px; overflow:auto;"></div>
          </div>
        </div>
        <div style="display:flex; justify-content:flex-end;">
          <button type="button" class="modal-btn modal-btn-primary" onclick="saveMusyrifManageKamarMembers()">Simpan Anggota</button>
        </div>
      </div>
    `
    renderMusyrifManageKamarPanels()
  } catch (error) {
    console.error(error)
    content.innerHTML = `<div class="placeholder-card">Gagal load data kamar: ${escapeHtml(error?.message || 'Unknown error')}</div>`
  }
}

async function loadMusyrifPage(page) {
  const targetPage = PAGE_TITLES[page] ? page : 'dashboard'
  if (targetPage !== 'chat' && window.ChatModule && typeof window.ChatModule.stop === 'function') {
    window.ChatModule.stop()
  }
  const isEkskulPj = await setupMusyrifEkskulAccess()
  const isWakasekPrestasi = await setupMusyrifPrestasiPelanggaranAccess()
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
  if (targetPage === 'data-kamar') {
    await renderMusyrifManageKamarPage()
    return
  }
  if (targetPage === 'chat') {
    await renderMusyrifChatPage()
    return
  }
  if (targetPage === 'perizinan-santri') {
    await renderMusyrifPerizinanPage()
    return
  }
  if (targetPage === 'ekskul') {
    if (!isEkskulPj) {
      const content = document.getElementById('musyrif-content')
      if (content) content.innerHTML = '<div class="placeholder-card">Menu ekskul hanya untuk PJ ekskul.</div>'
      return
    }
    await renderMusyrifEkskulPage()
    return
  }
  if (targetPage === 'prestasi-pelanggaran') {
    if (!isWakasekPrestasi) {
      const content = document.getElementById('musyrif-content')
      if (content) content.innerHTML = '<div class="placeholder-card">Menu ini hanya dapat diakses oleh wakasek kesantrian.</div>'
      return
    }
    await renderMusyrifPrestasiPelanggaranPage()
    return
  }
  await renderDashboard()
}

async function renderMusyrifChatPage() {
  const content = document.getElementById('musyrif-content')
  if (!content) return
  content.innerHTML = 'Loading chat...'
  try {
    const profile = await getCurrentMusyrif()
    if (!profile?.id) {
      content.innerHTML = '<div class="placeholder-card">Data profil musyrif tidak ditemukan.</div>'
      return
    }
    if (!window.ChatModule || typeof window.ChatModule.render !== 'function') {
      content.innerHTML = '<div class="placeholder-card">Modul chat belum termuat. Refresh halaman.</div>'
      return
    }
    await window.ChatModule.render({
      sb,
      containerId: 'musyrif-content',
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

window.loadMusyrifPage = loadMusyrifPage
window.onMusyrifPeriodeChange = onMusyrifPeriodeChange
window.openMusyrifBulkInputModal = openMusyrifBulkInputModal
window.saveMusyrifBulkInput = saveMusyrifBulkInput
window.downloadMusyrifBulkTemplate = downloadMusyrifBulkTemplate
window.onMusyrifBulkExcelFileChange = onMusyrifBulkExcelFileChange
window.applyMusyrifBulkExcelToTable = applyMusyrifBulkExcelToTable
window.closeMusyrifBulkInputModal = closeMusyrifBulkInputModal
window.openMusyrifLaporanDetail = openMusyrifLaporanDetail
window.saveMusyrifLaporanDetail = saveMusyrifLaporanDetail
window.renderMusyrifPerizinanPage = renderMusyrifPerizinanPage
window.submitSantriPerizinanForm = submitSantriPerizinanForm
window.saveSantriPerizinanReview = saveSantriPerizinanReview
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
window.uploadMusyrifProfilePhoto = uploadMusyrifProfilePhoto
window.selectMusyrifManageKamar = selectMusyrifManageKamar
window.searchMusyrifManageKamarSantri = searchMusyrifManageKamarSantri
window.toggleMusyrifManageKamarSantri = toggleMusyrifManageKamarSantri
window.removeMusyrifManageKamarSantri = removeMusyrifManageKamarSantri
window.saveMusyrifManageKamarName = saveMusyrifManageKamarName
window.saveMusyrifManageKamarMembers = saveMusyrifManageKamarMembers
window.renderMusyrifManageKamarPanels = renderMusyrifManageKamarPanels
window.selectMusyrifEkskul = selectMusyrifEkskul
window.selectMusyrifEkskulSantri = selectMusyrifEkskulSantri
window.openMusyrifEkskulSantriDetail = openMusyrifEkskulSantriDetail
window.closeMusyrifEkskulSantriDetail = closeMusyrifEkskulSantriDetail
window.musyrifEkskulUpdateIndicatorStars = musyrifEkskulUpdateIndicatorStars
window.addMusyrifEkskulMember = addMusyrifEkskulMember
window.addMusyrifEkskulIndikator = addMusyrifEkskulIndikator
window.saveMusyrifEkskulProgress = saveMusyrifEkskulProgress
window.setMusyrifEkskulTab = setMusyrifEkskulTab
window.onMusyrifEkskulMonthlyPeriodeChange = onMusyrifEkskulMonthlyPeriodeChange
window.saveMusyrifEkskulMonthlyReport = saveMusyrifEkskulMonthlyReport
window.setMusyrifPrestasiTab = setMusyrifPrestasiTab
window.onMusyrifPrestasiClassFilterChange = onMusyrifPrestasiClassFilterChange
window.saveMusyrifPrestasiEntry = saveMusyrifPrestasiEntry
window.saveMusyrifPelanggaranEntry = saveMusyrifPelanggaranEntry
window.resetMusyrifPrestasiForm = resetMusyrifPrestasiForm
window.resetMusyrifPelanggaranForm = resetMusyrifPelanggaranForm
window.editMusyrifPrestasiEntry = editMusyrifPrestasiEntry
window.editMusyrifPelanggaranEntry = editMusyrifPelanggaranEntry
window.deleteMusyrifPrestasiEntry = deleteMusyrifPrestasiEntry
window.deleteMusyrifPelanggaranEntry = deleteMusyrifPelanggaranEntry
window.openMusyrifPrestasiDoc = openMusyrifPrestasiDoc
window.closeMusyrifPrestasiDoc = closeMusyrifPrestasiDoc
window.uploadMusyrifPelanggaranSuratFile = uploadMusyrifPelanggaranSuratFile
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
    await setupMusyrifEkskulAccess(true)
    await setupMusyrifPrestasiPelanggaranAccess(true)
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
  refreshMusyrifTopbarChatBadge().catch(error => console.error(error))
  startMusyrifTopbarChatBadgeTicker()

  document.addEventListener('click', event => {
    const topWrap = document.querySelector('.topbar-user-menu-wrap')
    const sideWrap = document.querySelector('.sidebar-user-menu-wrap')
    if ((topWrap && topWrap.contains(event.target)) || (sideWrap && sideWrap.contains(event.target))) return
    closeTopbarUserMenu()
    closeTopbarNotifMenu()
  })
})

