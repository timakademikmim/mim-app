const supabaseUrl = 'https://optucpelkueqmlhwlbej.supabase.co'
const supabaseKey = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9wdHVjcGVsa3VlcW1saHdsYmVqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzAxOTY4MTgsImV4cCI6MjA4NTc3MjgxOH0.Vqaey9pcnltu9uRbPk0J-AGWaGDZjQLw92pcRv67GNE'
const sb = supabase.createClient(supabaseUrl, supabaseKey)

const GURU_LAST_PAGE_KEY = 'guru_last_page'
const GURU_MAPEL_DETAIL_STATE_KEY = 'guru_mapel_detail_state'
const DEFAULT_GURU_PAGE = 'dashboard'
const ATTENDANCE_TABLE = 'absensi_santri'
const INPUT_NILAI_TABLE = 'nilai_input_akademik'
const RAPOR_DESC_TABLE = 'rapor_deskripsi_mapel'
const MONTHLY_REPORT_TABLE = 'laporan_bulanan_wali'
const MONTHLY_REPORT_STORAGE_BUCKET = 'laporan-bulanan'
const MONTHLY_REPORT_WA_TEMPLATE_KEY = 'laporan_bulanan_wa_template'
const DAILY_TASK_TEMPLATE_TABLE = 'tugas_harian_template'
const DAILY_TASK_SUBMIT_TABLE = 'tugas_harian_submit'
const EXAM_SCHEDULE_TABLE = 'jadwal_ujian'
const EXAM_QUESTION_TABLE = 'soal_ujian'
const EXAM_ARABIC_FONT_FILE = 'Traditional Arabic Regular.ttf'
const EXAM_ARABIC_FONT_NAME = 'TraditionalArabic'
const EXAM_ARABIC_FONT_BOLD_FILE = 'Traditional Arabic Bold.ttf'
const EXAM_ARABIC_FONT_BOLD_NAME = 'TraditionalArabicBold'
const EXAM_ARABIC_FONT_VFS_KEY = 'traditional-arabic-regular.ttf'
const EXAM_ARABIC_FONT_BOLD_VFS_KEY = 'traditional-arabic-bold.ttf'
const EXAM_PRINT_BACKGROUND_URL = 'Bg Ujian.png'
const TOPBAR_KALENDER_TABLE = 'kalender_akademik'
const SCHOOL_PROFILE_TABLE = 'struktur_sekolah'
const TOPBAR_KALENDER_CACHE_KEY = 'kalender_akademik:list'
const TOPBAR_KALENDER_CACHE_TTL_MS = 2 * 60 * 1000
const TOPBAR_NOTIF_READ_KEY = 'guru_topbar_notif_read'
const TOPBAR_NOTIF_RANGE_KEY = 'guru_topbar_notif_range_days'
const GURU_PAGE_CACHE_TTL_MS = 90 * 1000
const GURU_PAGE_CACHEABLE = new Set([
  'dashboard',
  'monitoring',
  'input-nilai',
  'input-absensi',
  'jadwal',
  'tugas',
  'laporan-pekanan',
  'laporan-absensi',
  'laporan-bulanan',
  'rapor',
  'ujian'
])
const TOPBAR_KALENDER_DEFAULT_COLOR = '#2563eb'
const RAPOR_PDF_BACKGROUND_URL = 'Background Rapor.png'
const ATTENDANCE_STATUSES = ['Hadir', 'Terlambat', 'Sakit', 'Izin', 'Alpa']
const INPUT_NILAI_JENIS_LIST = ['Tugas', 'Ulangan Harian', 'UTS', 'UAS', 'Keterampilan']

const PAGE_TITLES = {
  dashboard: 'Dashboard',
  input: 'Input',
  'input-nilai': 'Input Nilai',
  'input-absensi': 'Input Absen',
  laporan: 'Laporan',
  'laporan-absensi': 'Absensi',
  'laporan-pekanan': 'Laporan Pekanan',
  'laporan-bulanan': 'Laporan Bulanan',
  jadwal: 'Jadwal',
  ujian: 'Ujian',
  mapel: 'Mapel',
  monitoring: 'Monitoring',
  absensi: 'Absensi',
  tugas: 'Mutabaah',
  nilai: 'Input Nilai',
  rapor: 'Rapor',
  profil: 'Profil'
}

let guruContextCache = null
let activeTahunAjaranCache = null
let activeSemesterCache = null
let currentGuruRowCache = null
let currentMapelDetailDistribusiId = ''
let currentAbsensiSantriList = []
let currentMapelDetailState = null
let currentMapelDetailTab = 'absensi'
let currentMapelEditMode = {
  absensi: false,
  nilai: false
}
let currentInputNilaiSantriList = []
let currentNilaiDetailModalState = null
let waliKelasAccessCache = null
let wakasekAkademikAccessCache = null
let jamPelajaranSupportsTahunAjaran = null
let laporanBulananState = {
  periode: '',
  guru: null,
  kelasList: [],
  kelasMap: new Map(),
  santriList: [],
  selectedSantriId: '',
  currentDetail: null,
  absensiRows: [],
  waTemplate: '',
  waTemplateEditing: false
}
let waTargetModalResolver = null
let guruDailyTaskState = {
  periode: '',
  tanggal: '',
  templates: [],
  submissions: [],
  guruId: ''
}
let raporState = {
  guru: null,
  kelasMap: new Map(),
  santriList: [],
  semesterList: [],
  semesterId: '',
  tahunAjaranNama: '',
  selectedSantriId: '',
  currentDetail: null
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
let schoolProfileCache = null
let guruDashboardAgendaRows = []
let guruPageHtmlCache = {}
let monitoringState = {
  periode: '',
  tab: 'guru',
  santriMode: 'bulan',
  santriKelasFilter: '',
  santriDate: '',
  santriWeek: '',
  santriMonth: '',
  selectedGuruId: '',
  guruRows: [],
  guruDetail: [],
  santriRows: [],
  santriWeeklyRows: []
}
let ujianGuruState = {
  rows: [],
  mapelPairs: new Set(),
  classListByMapelPerangkatan: new Map(),
  soalByJadwal: new Map(),
  openFolders: new Set(),
  supportsKelasTarget: true,
  activeJadwal: null,
  activeKelasName: '',
  sectionDefs: [],
  activeSoal: null
}
let examArabicFontBase64 = ''
let examArabicFontBoldBase64 = ''
let examArabicFontLoadPromise = null
let examPrintBackgroundDataUrl = ''
let examPrintBackgroundLoadPromise = null

function getGuruPageCache(page) {
  if (!GURU_PAGE_CACHEABLE.has(String(page || ''))) return ''
  const key = String(page || '')
  const entry = guruPageHtmlCache[key]
  if (!entry) return ''
  if (Date.now() - Number(entry.ts || 0) > GURU_PAGE_CACHE_TTL_MS) return ''
  return String(entry.html || '')
}

function setGuruPageCache(page) {
  const key = String(page || '')
  if (!GURU_PAGE_CACHEABLE.has(key)) return
  const content = document.getElementById('guru-content')
  if (!content) return
  guruPageHtmlCache[key] = {
    ts: Date.now(),
    html: String(content.innerHTML || '')
  }
}

function clearGuruPageCache(page = '') {
  const key = String(page || '').trim()
  if (!key) {
    guruPageHtmlCache = {}
    return
  }
  delete guruPageHtmlCache[key]
}

async function checkJamPelajaranSupportsTahunAjaran() {
  if (jamPelajaranSupportsTahunAjaran !== null) return jamPelajaranSupportsTahunAjaran
  const { error } = await sb
    .from('jam_pelajaran')
    .select('id, tahun_ajaran_id')
    .limit(1)
  jamPelajaranSupportsTahunAjaran = !error
  if (error) console.warn('Kolom jam_pelajaran.tahun_ajaran_id belum tersedia di guru page.', error)
  return jamPelajaranSupportsTahunAjaran
}

async function getActiveTahunAjaran(forceReload = false) {
  if (!forceReload && activeTahunAjaranCache) return activeTahunAjaranCache

  const { data, error } = await sb
    .from('tahun_ajaran')
    .select('id, nama')
    .eq('aktif', true)
    .order('id', { ascending: false })
    .limit(1)

  if (error) {
    console.error(error)
    activeTahunAjaranCache = null
    return null
  }

  activeTahunAjaranCache = data?.[0] || null
  return activeTahunAjaranCache
}

function saveMapelDetailState(distribusiId, tab) {
  const validTab = tab === 'nilai' || tab === 'rapor-desc' ? tab : 'absensi'
  const payload = {
    distribusiId: String(distribusiId || ''),
    tab: validTab
  }
  if (!payload.distribusiId) {
    localStorage.removeItem(GURU_MAPEL_DETAIL_STATE_KEY)
    return
  }
  localStorage.setItem(GURU_MAPEL_DETAIL_STATE_KEY, JSON.stringify(payload))
}

function getMapelDetailState() {
  try {
    const raw = localStorage.getItem(GURU_MAPEL_DETAIL_STATE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw)
    if (!parsed || typeof parsed !== 'object') return null
    const distribusiId = String(parsed.distribusiId || '').trim()
    if (!distribusiId) return null
    const tab = parsed.tab === 'nilai' || parsed.tab === 'rapor-desc'
      ? parsed.tab
      : 'absensi'
    return { distribusiId, tab }
  } catch (error) {
    return null
  }
}

function clearMapelDetailState() {
  localStorage.removeItem(GURU_MAPEL_DETAIL_STATE_KEY)
}

async function hasWaliKelasAssignment(guruId) {
  if (!guruId) return false

  try {
    const { data: tahunAktif, error: tahunError } = await sb
      .from('tahun_ajaran')
      .select('id')
      .eq('aktif', true)
      .order('id', { ascending: false })
      .limit(1)

    if (tahunError) {
      console.error(tahunError)
    }

    const tahunAjaranId = tahunAktif?.[0]?.id || null
    let query = sb
      .from('kelas')
      .select('id')
      .eq('wali_kelas_id', guruId)
      .limit(1)

    if (tahunAjaranId) query = query.eq('tahun_ajaran_id', tahunAjaranId)

    const { data, error } = await query
    if (error) {
      console.error(error)
      return false
    }

    return (data || []).length > 0
  } catch (error) {
    console.error(error)
    return false
  }
}

async function getIsWaliKelas(forceReload = false) {
  if (!forceReload && typeof waliKelasAccessCache === 'boolean') return waliKelasAccessCache

  const roles = parseRoleList()
  const isByRole = roles.some(isWaliKelasRole)
  if (isByRole) {
    waliKelasAccessCache = true
    return true
  }

  try {
    const guru = await getCurrentGuruRow()
    const isByAssignment = await hasWaliKelasAssignment(guru?.id)
    waliKelasAccessCache = !!isByAssignment
    return waliKelasAccessCache
  } catch (error) {
    console.error(error)
    waliKelasAccessCache = false
    return false
  }
}

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

function normalizeRole(role) {
  return String(role || '').trim().toLowerCase().replaceAll('_', ' ')
}

function parseRoleList() {
  let roles = []
  try {
    roles = JSON.parse(localStorage.getItem('login_roles') || '[]')
  } catch (e) {
    roles = []
  }

  if (!Array.isArray(roles) || roles.length === 0) {
    const singleRole = localStorage.getItem('login_role')
    roles = singleRole ? [singleRole] : []
  }

  return roles.map(normalizeRole).filter(Boolean)
}

function isWaliKelasRole(role) {
  const clean = normalizeRole(role).replaceAll(' ', '')
  return clean === 'walikelas'
}

function isWakasekAkademikRole(role) {
  const clean = normalizeRole(role).replaceAll(' ', '')
  if (!clean) return false
  if (clean === 'wakasekakademik') return true
  if (clean === 'wakasekbidangakademik') return true
  return clean.includes('wakasek') && clean.includes('akademik')
}

function normalizePersonName(value) {
  return String(value || '').trim().replace(/\s+/g, ' ').toLowerCase()
}

function isMissingSchoolProfileColumnError(error) {
  const msg = String(error?.message || '').toLowerCase()
  return msg.includes('column') && msg.includes(SCHOOL_PROFILE_TABLE)
}

async function getIsWakasekAkademik(forceReload = false) {
  const loginId = String(localStorage.getItem('login_id') || '').trim()
  if (!forceReload && wakasekAkademikAccessCache && wakasekAkademikAccessCache.loginId === loginId) {
    return !!wakasekAkademikAccessCache.allowed
  }

  try {
    const guru = await getCurrentGuruRow()
    const guruName = normalizePersonName(guru?.nama)
    const guruId = String(guru?.id || '').trim().toLowerCase()
    const guruIdKaryawan = String(guru?.id_karyawan || '').trim().toLowerCase()

    if (!guruName) {
      wakasekAkademikAccessCache = { loginId, allowed: false }
      return false
    }

    const selectAttempts = [
      'wakasek_bidang_akademik, wakasek_akademik',
      'wakasek_bidang_akademik',
      'wakasek_akademik'
    ]

    let row = null
    for (const selectText of selectAttempts) {
      const { data, error } = await sb
        .from(SCHOOL_PROFILE_TABLE)
        .select(selectText)
        .order('updated_at', { ascending: false })
        .order('created_at', { ascending: false })
        .limit(1)

      if (error) {
        if (isMissingSchoolProfileTableError(error) || isMissingSchoolProfileColumnError(error)) {
          continue
        }
        throw error
      }

      row = data?.[0] || null
      break
    }

    const wakasekRaw = String(row?.wakasek_bidang_akademik || row?.wakasek_akademik || '').trim()
    const wakasekName = normalizePersonName(wakasekRaw)
    const wakasekToken = wakasekRaw.toLowerCase()
    const allowed = !!(
      (wakasekName && wakasekName === guruName) ||
      (guruId && wakasekToken === guruId) ||
      (guruIdKaryawan && wakasekToken === guruIdKaryawan)
    )

    wakasekAkademikAccessCache = { loginId, allowed }
    return !!wakasekAkademikAccessCache.allowed
  } catch (error) {
    console.error(error)
    wakasekAkademikAccessCache = { loginId, allowed: false }
    return false
  }
}

function asBool(value) {
  if (value === true || value === 1) return true
  const text = String(value ?? '').trim().toLowerCase()
  return text === 'true' || text === 't' || text === '1' || text === 'yes'
}

function toTimeLabel(value) {
  const text = String(value || '').trim()
  if (!text) return '-'
  return text.length >= 5 ? text.slice(0, 5) : text
}

function normalizeHari(raw) {
  return String(raw || '').trim().toLowerCase()
}

function getHariLabel(raw) {
  const value = normalizeHari(raw)
  const map = {
    senin: 'Senin',
    selasa: 'Selasa',
    rabu: 'Rabu',
    kamis: 'Kamis',
    jumat: 'Jumat',
    sabtu: 'Sabtu',
    minggu: 'Minggu'
  }
  return map[value] || (raw || '-')
}

function getHariOrder(raw) {
  const value = normalizeHari(raw)
  const map = {
    senin: 1,
    selasa: 2,
    rabu: 3,
    kamis: 4,
    jumat: 5,
    sabtu: 6,
    minggu: 7
  }
  return map[value] || 99
}

function getMonthInputToday() {
  return new Date().toISOString().slice(0, 7)
}

function getDateInputToday() {
  return new Date().toISOString().slice(0, 10)
}

function getWeekInputToday() {
  const now = new Date()
  const date = new Date(Date.UTC(now.getFullYear(), now.getMonth(), now.getDate()))
  const dayNum = date.getUTCDay() || 7
  date.setUTCDate(date.getUTCDate() + 4 - dayNum)
  const yearStart = new Date(Date.UTC(date.getUTCFullYear(), 0, 1))
  const weekNo = Math.ceil((((date - yearStart) / 86400000) + 1) / 7)
  return `${date.getUTCFullYear()}-W${String(weekNo).padStart(2, '0')}`
}

function getRangeFromWeekInput(weekText) {
  const value = String(weekText || '').trim()
  const match = value.match(/^(\d{4})-W(\d{2})$/)
  if (!match) return null
  const year = Number(match[1])
  const week = Number(match[2])
  if (!Number.isFinite(year) || !Number.isFinite(week) || week < 1 || week > 53) return null
  const jan4 = new Date(Date.UTC(year, 0, 4))
  const jan4Day = jan4.getUTCDay() || 7
  const monday = new Date(jan4)
  monday.setUTCDate(jan4.getUTCDate() - jan4Day + 1 + ((week - 1) * 7))
  const sunday = new Date(monday)
  sunday.setUTCDate(monday.getUTCDate() + 6)
  return {
    start: monday.toISOString().slice(0, 10),
    end: sunday.toISOString().slice(0, 10)
  }
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
  if (num <= 5 && num >= 1) {
    const rounded = Math.round(num)
    if (rounded === 5) return 'A'
    if (rounded === 4) return 'B'
    if (rounded === 3) return 'C'
    if (rounded === 2) return 'D'
    if (rounded === 1) return 'E'
  }
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
  return '-'
}

function getAkhlakNumericValueByGrade(grade) {
  const g = normalizeAkhlakGrade(grade)
  if (g === 'A') return 5
  if (g === 'B') return 4
  if (g === 'C') return 3
  if (g === 'D') return 2
  if (g === 'E') return 1
  return null
}

function getAkhlakPredikat(nilai) {
  const grade = normalizeAkhlakGrade(nilai)
  if (!grade) return ''
  return getAkhlakKeteranganByGrade(grade)
}

function getAkhlakGradeInfo(nilai) {
  const grade = normalizeAkhlakGrade(nilai)
  if (!grade) return { grade: '-', desc: '-' }
  return { grade, desc: getAkhlakKeteranganByGrade(grade) }
}

function normalizeWhatsappNumber(raw) {
  const digits = String(raw || '').replace(/\D/g, '')
  if (!digits) return ''
  if (digits.startsWith('62')) return digits
  if (digits.startsWith('0')) return `62${digits.slice(1)}`
  return digits
}

function sanitizeFileNamePart(raw) {
  return String(raw || '')
    .replace(/[\\/:*?"<>|]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
}

function isMissingSchoolProfileTableError(error) {
  const msg = String(error?.message || '').toLowerCase()
  const code = String(error?.code || '').toUpperCase()
  return (
    code === '42P01' ||
    msg.includes(`'${SCHOOL_PROFILE_TABLE}'`) ||
    msg.includes(`relation \"${SCHOOL_PROFILE_TABLE}\" does not exist`) ||
    (msg.includes('could not find the table') && msg.includes('schema cache')) ||
    msg.includes(`public.${SCHOOL_PROFILE_TABLE}`)
  )
}

async function getSchoolProfile(forceReload = false) {
  if (!forceReload && schoolProfileCache) return schoolProfileCache

  const fallback = {
    nama_sekolah: String(localStorage.getItem('school_name') || localStorage.getItem('nama_sekolah') || '').trim() || null,
    alamat_sekolah: String(localStorage.getItem('school_address') || localStorage.getItem('alamat_sekolah') || '').trim() || null
  }

  const { data, error } = await sb
    .from(SCHOOL_PROFILE_TABLE)
    .select('nama_sekolah, alamat_sekolah, updated_at, created_at')
    .order('updated_at', { ascending: false })
    .order('created_at', { ascending: false })
    .limit(1)

  if (error) {
    if (!isMissingSchoolProfileTableError(error)) {
      console.error(error)
    }
    schoolProfileCache = fallback
    return schoolProfileCache
  }

  const row = data?.[0] || null
  schoolProfileCache = {
    nama_sekolah: String(row?.nama_sekolah || fallback.nama_sekolah || '').trim() || null,
    alamat_sekolah: String(row?.alamat_sekolah || fallback.alamat_sekolah || '').trim() || null
  }

  if (schoolProfileCache.nama_sekolah) {
    localStorage.setItem('school_name', schoolProfileCache.nama_sekolah)
    localStorage.setItem('nama_sekolah', schoolProfileCache.nama_sekolah)
  }
  if (schoolProfileCache.alamat_sekolah) {
    localStorage.setItem('school_address', schoolProfileCache.alamat_sekolah)
    localStorage.setItem('alamat_sekolah', schoolProfileCache.alamat_sekolah)
  }

  return schoolProfileCache
}

function blobToDataUrl(blob) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(new Error('Gagal membaca file gambar background rapor.'))
    reader.readAsDataURL(blob)
  })
}

async function loadPdfBackgroundDataUrl(url) {
  const response = await fetch(encodeURI(String(url || '')), { cache: 'no-cache' })
  if (!response.ok) {
    throw new Error(`Background rapor tidak ditemukan (${response.status}).`)
  }
  const blob = await response.blob()
  const dataUrl = await blobToDataUrl(blob)
  if (!String(dataUrl || '').startsWith('data:image/')) {
    throw new Error('Format background rapor harus berupa gambar.')
  }
  return dataUrl
}

function getMonthlyWaTemplateStorageKey(guruId) {
  const suffix = String(guruId || '').trim() || 'default'
  return `${MONTHLY_REPORT_WA_TEMPLATE_KEY}:${suffix}`
}

function getDefaultMonthlyWaTemplate() {
  return [
    "Assalamu'alaikum warahmatullahi wabarakatuh",
    '',
    "Bapak/Ibu hafizakumullahu ta'ala",
    '',
    'Alhamdulillah kembali menyampaikan Laporan Evaluasi Perkembangan Santri bulan ini ananda <nama santri>.',
    '',
    'Mohon dibaca dengan seksama dan jika ada hal yang kurang jelas maka Ibu/Bapak bisa menanyakan secara langsung dengan menghubungi nomor penanggung jawab yang tertera.',
    '',
    'Laporan ini bisa menjadi catatan muhasabah untuk Ibu/Bapak atas perkembangan ananda selama sebulan di pondok.',
    '',
    'Semoga Allah SWT mengistiqamahkan ananda dalam kebaikan dan menjadikannya pribadi yang lebih baik ke depannya.',
    '',
    'Syukron wajazakumullahu khairan',
    '',
    'Link laporan:',
    '<link>'
  ].join('\n')
}

function loadMonthlyWaTemplate(guruId) {
  const key = getMonthlyWaTemplateStorageKey(guruId)
  const saved = String(localStorage.getItem(key) || '').trim()
  return saved || getDefaultMonthlyWaTemplate()
}

function buildMonthlyReportStorageMissingMessage() {
  return `Bucket storage '${MONTHLY_REPORT_STORAGE_BUCKET}' belum ada atau belum bisa diakses.\n\nBuat bucket public di Supabase Storage dengan nama: ${MONTHLY_REPORT_STORAGE_BUCKET}`
}

function buildDailyTaskMissingTableMessage() {
  return `Tabel tugas harian belum ada di Supabase.\n\nSilakan buat tabel berikut:\n\n1) ${DAILY_TASK_TEMPLATE_TABLE}\n- id (uuid primary key, default gen_random_uuid())\n- tahun_ajaran_id (uuid, nullable)\n- tanggal (date)\n- judul (text)\n- deskripsi (text, nullable)\n- aktif (boolean, default true)\n- created_at (timestamptz, default now())\n\n2) ${DAILY_TASK_SUBMIT_TABLE}\n- id (uuid primary key, default gen_random_uuid())\n- template_id (uuid)\n- guru_id (uuid)\n- tanggal (date)\n- status (text: selesai/belum)\n- catatan (text, nullable)\n- submitted_at (timestamptz, default now())\n- updated_at (timestamptz, default now())\n\nUnique disarankan:\n- ${DAILY_TASK_TEMPLATE_TABLE}: (tahun_ajaran_id, tanggal, judul)\n- ${DAILY_TASK_SUBMIT_TABLE}: (template_id, guru_id, tanggal)`
}

function isMissingDailyTaskTableError(error) {
  const code = String(error?.code || '').toUpperCase()
  const msg = String(error?.message || '').toLowerCase()
  if (code === '42P01') return true
  if (msg.includes(DAILY_TASK_TEMPLATE_TABLE.toLowerCase())) return true
  if (msg.includes(DAILY_TASK_SUBMIT_TABLE.toLowerCase())) return true
  return false
}

function getKehadiranPredikat(nilaiPersen) {
  const num = Number(nilaiPersen)
  if (!Number.isFinite(num)) return '-'
  if (num >= 95) return 'A (Sangat Baik)'
  if (num >= 85) return 'B (Baik)'
  if (num >= 75) return 'C (Cukup)'
  if (num >= 60) return 'D (Kurang)'
  return 'E (Perlu Pembinaan)'
}

function getDailyAttendanceStatus(rows = []) {
  const statuses = rows
    .map(item => String(item?.status || '').trim().toLowerCase())
    .filter(Boolean)

  if (statuses.includes('hadir') || statuses.includes('terlambat')) return 'Hadir'
  if (statuses.includes('sakit')) return 'Sakit'
  if (statuses.includes('izin')) return 'Izin'
  if (statuses.includes('alpa')) return 'Alpa'
  return '-'
}

function aggregateAttendanceByDay(rows = []) {
  const map = new Map()

  ;(rows || []).forEach(item => {
    const tanggal = String(item?.tanggal || '').slice(0, 10)
    if (!tanggal) return
    if (!map.has(tanggal)) map.set(tanggal, [])
    map.get(tanggal).push(item)
  })

  return Array.from(map.entries())
    .map(([tanggal, items]) => ({
      tanggal,
      status: getDailyAttendanceStatus(items),
      totalMapel: items.length
    }))
    .sort((a, b) => String(a.tanggal).localeCompare(String(b.tanggal)))
}

function pickLabelByKeys(item, keys) {
  for (const key of keys) {
    const value = item?.[key]
    if (value !== null && value !== undefined && String(value).trim() !== '') {
      return String(value).trim()
    }
  }
  return ''
}

function getSemesterLabel(semester) {
  const label = pickLabelByKeys(semester, ['nama_semester', 'nama', 'label', 'kode', 'semester'])
  return label || (semester?.id ? `Semester #${semester.id}` : '-')
}

function getMapelLabel(mapel) {
  if (!mapel) return '-'
  const namaMapel = pickLabelByKeys(mapel, ['nama', 'nama_mapel', 'mapel']) || '-'
  return `${namaMapel}${mapel.kategori ? ` (${mapel.kategori})` : ''}`
}

function getMapelPlainName(mapel) {
  if (!mapel) return '-'
  return pickLabelByKeys(mapel, ['nama', 'nama_mapel', 'mapel']) || '-'
}

function setTopbarTitle(page) {
  const titleEl = document.getElementById('guru-topbar-title')
  if (!titleEl) return
  const title = PAGE_TITLES[page] || 'Panel Guru'
  const todayLabel = new Date().toLocaleDateString('id-ID', {
    weekday: 'long',
    day: '2-digit',
    month: 'long',
    year: 'numeric'
  })
  titleEl.innerHTML = `<span class="topbar-title-main">${escapeHtml(title)}</span><span class="topbar-title-sep" aria-hidden="true"></span><button type="button" class="topbar-title-date-btn" onclick="openTopbarCalendarPopup()" title="Lihat kalender kegiatan">${escapeHtml(todayLabel)}</button>`
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

function setNavActive(page) {
  const mainButtons = document.querySelectorAll('.guru-nav-btn')
  const subButtons = document.querySelectorAll('.guru-submenu-btn')

  mainButtons.forEach(btn => btn.classList.remove('active'))
  subButtons.forEach(btn => btn.classList.remove('active'))

  if (!page) return

  const isInputPage = page === 'input-nilai' || page === 'input-absensi' || page === 'nilai' || page === 'absensi'
  const isLaporanPage = page === 'laporan-absensi' || page === 'laporan-pekanan' || page === 'laporan-bulanan' || page === 'laporan'

  if (isInputPage) {
    document.querySelector('.guru-nav-btn[data-page="input"]')?.classList.add('active')
    const subPage = (page === 'absensi' || page === 'input-absensi') ? 'input-absensi' : 'input-nilai'
    document.querySelector(`.guru-submenu-btn[data-page="${subPage}"]`)?.classList.add('active')
    openGuruInputMenu()
    closeGuruLaporanMenu()
    return
  }

  if (isLaporanPage) {
    document.querySelector('.guru-nav-btn[data-page="laporan"]')?.classList.add('active')
    const subPage = page === 'laporan-bulanan'
      ? 'laporan-bulanan'
      : page === 'laporan-absensi'
        ? 'laporan-absensi'
        : 'laporan-pekanan'
    document.querySelector(`.guru-submenu-btn[data-page="${subPage}"]`)?.classList.add('active')
    openGuruLaporanMenu()
    closeGuruInputMenu()
    return
  }

  document.querySelector(`.guru-nav-btn[data-page="${page}"]`)?.classList.add('active')
  closeGuruInputMenu()
  closeGuruLaporanMenu()
}

function openGuruInputMenu() {
  animateGuruSidebarSubmenu(document.getElementById('guru-input-submenu'), true)
  document.querySelector('.guru-nav-btn[data-page="input"]')?.classList.add('expanded')
}

function closeGuruInputMenu() {
  animateGuruSidebarSubmenu(document.getElementById('guru-input-submenu'), false)
  document.querySelector('.guru-nav-btn[data-page="input"]')?.classList.remove('expanded')
}

function toggleGuruInputMenu() {
  const submenu = document.getElementById('guru-input-submenu')
  if (!submenu) return
  const isOpen = submenu.classList.contains('open')
  if (isOpen) {
    closeGuruInputMenu()
    return
  }
  openGuruInputMenu()
  closeGuruLaporanMenu()
}

function openGuruLaporanMenu() {
  animateGuruSidebarSubmenu(document.getElementById('guru-laporan-submenu'), true)
  document.querySelector('.guru-nav-btn[data-page="laporan"]')?.classList.add('expanded')
}

function closeGuruLaporanMenu() {
  animateGuruSidebarSubmenu(document.getElementById('guru-laporan-submenu'), false)
  document.querySelector('.guru-nav-btn[data-page="laporan"]')?.classList.remove('expanded')
}

function animateGuruSidebarSubmenu(submenu, expand) {
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

function toggleGuruLaporanMenu() {
  const laporanBtn = document.querySelector('.guru-nav-btn[data-page="laporan"]')
  if (laporanBtn?.disabled) return
  const submenu = document.getElementById('guru-laporan-submenu')
  if (!submenu) return
  const isOpen = submenu.classList.contains('open')
  if (isOpen) {
    closeGuruLaporanMenu()
    return
  }
  openGuruLaporanMenu()
  closeGuruInputMenu()
}

function loadGuruInputFromSidebar(page) {
  loadGuruPage(page)
}

function loadGuruLaporanFromSidebar(page) {
  loadGuruPage(page)
}

function renderPlaceholder(title, desc) {
  const content = document.getElementById('guru-content')
  if (!content) return

  content.innerHTML = `
    <div class="placeholder-card">
      <div style="font-size:15px; font-weight:700; margin-bottom:8px;">${escapeHtml(title)}</div>
      <div style="font-size:13px; line-height:1.6;">${escapeHtml(desc)}</div>
    </div>
  `
}

function setButtonLoading(buttonEl, loading, loadingText = 'Memproses...') {
  if (!buttonEl) return
  if (loading) {
    if (!buttonEl.dataset.originalHtml) buttonEl.dataset.originalHtml = buttonEl.innerHTML
    buttonEl.disabled = true
    buttonEl.classList.add('is-loading')
    buttonEl.innerHTML = `<span class="btn-spinner" aria-hidden="true"></span>${escapeHtml(loadingText)}`
    return
  }
  buttonEl.disabled = false
  buttonEl.classList.remove('is-loading')
  if (buttonEl.dataset.originalHtml) {
    buttonEl.innerHTML = buttonEl.dataset.originalHtml
    delete buttonEl.dataset.originalHtml
  }
}

function getDailyTaskStatusLabel(raw) {
  return String(raw || '').toLowerCase() === 'selesai' ? 'Selesai' : 'Belum'
}

function getDailyTaskChecked(templateId, tanggal) {
  const sid = `${templateId}__${tanggal}`
  const row = (guruDailyTaskState.submissions || []).find(item => `${item.template_id}__${item.tanggal}` === sid)
  return String(row?.status || '').toLowerCase() === 'selesai'
}

function getDailyTaskCatatan(templateId, tanggal) {
  const sid = `${templateId}__${tanggal}`
  const row = (guruDailyTaskState.submissions || []).find(item => `${item.template_id}__${item.tanggal}` === sid)
  return String(row?.catatan || '')
}

function isAhadForDailyTask(dateText) {
  return normalizeHari(getHariFromDate(String(dateText || '').trim())) === 'minggu'
}

function getNextNonAhadDate(dateText) {
  const text = String(dateText || '').trim()
  if (!/^\d{4}-\d{2}-\d{2}$/.test(text)) return ''
  const date = new Date(`${text}T00:00:00`)
  if (Number.isNaN(date.getTime())) return ''
  while (date.getDay() === 0) date.setDate(date.getDate() + 1)
  return date.toISOString().slice(0, 10)
}

function adjustDailyTaskDateForPeriode(dateText, periode) {
  const text = String(dateText || '').trim()
  const monthKey = String(periode || '').trim()
  if (!/^\d{4}-\d{2}-\d{2}$/.test(text)) return text
  if (!/^\d{4}-\d{2}$/.test(monthKey)) return isAhadForDailyTask(text) ? (getNextNonAhadDate(text) || text) : text
  if (!isAhadForDailyTask(text)) return text

  const next = getNextNonAhadDate(text)
  if (next && next.startsWith(`${monthKey}-`)) return next

  const date = new Date(`${text}T00:00:00`)
  if (Number.isNaN(date.getTime())) return text
  while (date.getDay() === 0) date.setDate(date.getDate() - 1)
  const prev = date.toISOString().slice(0, 10)
  if (prev.startsWith(`${monthKey}-`)) return prev
  return text
}

async function loadGuruDailyTaskData(periode) {
  const range = getPeriodeRange(periode)
  if (!range) throw new Error('Periode tidak valid')

  const ctx = await getGuruContext(false)
  const guruId = String(ctx?.guru?.id || '')
  if (!guruId) return { guruId: '', templates: [], submissions: [] }

  const tahunAjaranId = String(ctx?.activeTahunAjaran?.id || '')
  let templateQuery = sb
    .from(DAILY_TASK_TEMPLATE_TABLE)
    .select('id, tahun_ajaran_id, tanggal, judul, deskripsi, aktif')
    .gte('tanggal', range.start)
    .lte('tanggal', range.end)
    .order('tanggal', { ascending: true })
    .order('created_at', { ascending: true })
  if (tahunAjaranId) templateQuery = templateQuery.eq('tahun_ajaran_id', tahunAjaranId)

  const [templateRes, submitRes] = await Promise.all([
    templateQuery,
    sb
      .from(DAILY_TASK_SUBMIT_TABLE)
      .select('id, template_id, guru_id, tanggal, status, catatan, submitted_at, updated_at')
      .eq('guru_id', guruId)
      .gte('tanggal', range.start)
      .lte('tanggal', range.end)
  ])

  if (templateRes.error || submitRes.error) {
    throw (templateRes.error || submitRes.error)
  }

  return {
    guruId,
    templates: (templateRes.data || []).filter(item => !isAhadForDailyTask(String(item?.tanggal || ''))),
    submissions: submitRes.data || []
  }
}

function renderGuruDailyTaskRows() {
  const box = document.getElementById('guru-tugas-list')
  if (!box) return
  const tanggal = String(guruDailyTaskState.tanggal || '')
  if (isAhadForDailyTask(tanggal)) {
    box.innerHTML = '<div class="placeholder-card">Hari Ahad libur. Mutabaah tidak diinput pada hari Ahad.</div>'
    return
  }
  const templates = (guruDailyTaskState.templates || []).filter(item => item.aktif !== false && String(item.tanggal || '') === tanggal)

  if (!templates.length) {
    box.innerHTML = '<div class="placeholder-card">Belum ada tugas harian untuk tanggal ini.</div>'
    return
  }

  let html = '<div style="display:grid; gap:8px;">'
  html += templates.map(item => `
    <div style="border:1px solid #e2e8f0; border-radius:10px; padding:10px; background:#fff;">
      <label style="display:flex; gap:10px; align-items:flex-start;">
        <input type="checkbox" data-guru-task-template-id="${escapeHtml(String(item.id))}" ${getDailyTaskChecked(String(item.id), tanggal) ? 'checked' : ''} style="margin-top:3px;">
        <div style="flex:1;">
          <div style="font-weight:700; color:#0f172a;">${escapeHtml(item.judul || '-')}</div>
          <div style="font-size:12px; color:#64748b; margin-top:2px;">${escapeHtml(item.deskripsi || '-')}</div>
          <input type="text" class="guru-field" data-guru-task-catatan-id="${escapeHtml(String(item.id))}" value="${escapeHtml(getDailyTaskCatatan(String(item.id), tanggal))}" placeholder="Catatan (opsional)" style="margin-top:8px;">
        </div>
      </label>
    </div>
  `).join('')
  html += '</div>'
  box.innerHTML = html
}

function renderGuruDailyTaskSummary() {
  const box = document.getElementById('guru-tugas-summary')
  if (!box) return

  const byDate = new Map()
  const templates = (guruDailyTaskState.templates || []).filter(item => item.aktif !== false)
  const submissions = guruDailyTaskState.submissions || []
  const templateById = new Map(templates.map(item => [String(item.id), item]))

  templates.forEach(item => {
    const key = String(item.tanggal || '')
    if (!key) return
    if (!byDate.has(key)) byDate.set(key, { total: 0, done: 0, lastSubmit: '' })
    byDate.get(key).total += 1
  })

  submissions.forEach(item => {
    const template = templateById.get(String(item.template_id || ''))
    if (!template) return
    const key = String(item.tanggal || template.tanggal || '')
    if (!byDate.has(key)) byDate.set(key, { total: 0, done: 0, lastSubmit: '' })
    const entry = byDate.get(key)
    if (String(item.status || '').toLowerCase() === 'selesai') entry.done += 1
    const stamp = String(item.submitted_at || item.updated_at || '')
    if (stamp && (!entry.lastSubmit || stamp > entry.lastSubmit)) entry.lastSubmit = stamp
  })

  const rows = Array.from(byDate.entries()).sort((a, b) => a[0].localeCompare(b[0]))
  if (!rows.length) {
    box.innerHTML = '<div style="color:#64748b;">Belum ada ringkasan tugas bulan ini.</div>'
    return
  }

  let html = `
    <div style="overflow:auto; border:1px solid #e2e8f0; border-radius:10px;">
      <table style="width:100%; border-collapse:collapse; font-size:13px;">
        <thead>
          <tr style="background:#f8fafc;">
            <th style="padding:8px; border:1px solid #e2e8f0; width:120px;">Tanggal</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:120px;">Selesai/Total</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:90px;">%</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Submit Terakhir</th>
          </tr>
        </thead>
        <tbody>
  `

  html += rows.map(([tanggal, item]) => {
    const pct = item.total > 0 ? Math.round((item.done / item.total) * 100) : 0
    return `
      <tr>
        <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(tanggal)}</td>
        <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${item.done}/${item.total}</td>
        <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${pct}%</td>
        <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.lastSubmit ? item.lastSubmit.replace('T', ' ').slice(0, 16) : '-')}</td>
      </tr>
    `
  }).join('')

  html += '</tbody></table></div>'
  box.innerHTML = html
}

async function renderTugasHarianPage(forceReload = false) {
  const content = document.getElementById('guru-content')
  if (!content) return
  content.innerHTML = 'Loading tugas harian...'

  const periode = guruDailyTaskState.periode || getMonthInputToday()
  const rawTanggalDefault = guruDailyTaskState.tanggal || new Date().toISOString().slice(0, 10)
  const tanggalDefault = adjustDailyTaskDateForPeriode(rawTanggalDefault, periode)

  content.innerHTML = `
    <div style="display:grid; gap:12px;">
      <div style="border:1px solid #e2e8f0; border-radius:12px; padding:12px; background:#fff;">
        <div style="display:grid; grid-template-columns:repeat(auto-fit,minmax(180px,1fr)); gap:8px; align-items:end;">
          <div>
            <div style="font-size:12px; color:#64748b; margin-bottom:4px;">Periode</div>
            <input id="guru-tugas-periode" class="guru-field" type="month" value="${escapeHtml(periode)}">
          </div>
          <div>
            <div style="font-size:12px; color:#64748b; margin-bottom:4px;">Tanggal</div>
            <input id="guru-tugas-tanggal" class="guru-field" type="date" value="${escapeHtml(tanggalDefault)}">
          </div>
          <div style="display:flex; gap:8px; flex-wrap:wrap;">
            <button type="button" class="modal-btn modal-btn-muted" onclick="reloadGuruDailyTask()">Muat</button>
            <button type="button" class="modal-btn modal-btn-primary" onclick="submitGuruDailyTask()">Submit</button>
          </div>
        </div>
      </div>
      <div id="guru-tugas-list"></div>
      <div style="border:1px solid #e2e8f0; border-radius:12px; padding:12px; background:#fff;">
        <div style="font-weight:700; margin-bottom:8px; color:#0f172a;">Ringkasan Bulanan</div>
        <div id="guru-tugas-summary">Loading...</div>
      </div>
    </div>
  `

  try {
    const periodeValue = String(document.getElementById('guru-tugas-periode')?.value || periode)
    const tanggalValue = String(document.getElementById('guru-tugas-tanggal')?.value || tanggalDefault)
    const data = await loadGuruDailyTaskData(periodeValue)
    guruDailyTaskState = {
      ...guruDailyTaskState,
      periode: periodeValue,
      tanggal: tanggalValue,
      guruId: data.guruId,
      templates: data.templates,
      submissions: data.submissions
    }
    renderGuruDailyTaskRows()
    renderGuruDailyTaskSummary()
  } catch (error) {
    console.error(error)
    if (isMissingDailyTaskTableError(error)) {
      alert(buildDailyTaskMissingTableMessage())
    }
    content.innerHTML = `<div class="placeholder-card">Gagal load tugas harian: ${escapeHtml(error.message || 'Unknown error')}</div>`
  }
}

async function reloadGuruDailyTask() {
  const periode = String(document.getElementById('guru-tugas-periode')?.value || getMonthInputToday())
  const rawTanggal = String(document.getElementById('guru-tugas-tanggal')?.value || new Date().toISOString().slice(0, 10))
  const tanggal = adjustDailyTaskDateForPeriode(rawTanggal, periode)
  if (isAhadForDailyTask(rawTanggal)) {
    alert(`Hari Ahad libur. Tanggal otomatis dipindah ke ${tanggal}.`)
  }
  guruDailyTaskState.periode = periode
  guruDailyTaskState.tanggal = tanggal
  await renderTugasHarianPage(true)
}

async function submitGuruDailyTask() {
  const guruId = String(guruDailyTaskState.guruId || '')
  const tanggal = String(document.getElementById('guru-tugas-tanggal')?.value || '').trim()
  const periode = String(document.getElementById('guru-tugas-periode')?.value || '').trim()
  if (!guruId || !tanggal || !periode) {
    alert('Periode dan tanggal wajib diisi.')
    return
  }
  if (isAhadForDailyTask(tanggal)) {
    alert('Hari Ahad libur. Mutabaah tidak bisa disubmit pada hari Ahad.')
    return
  }
  if (!tanggal.startsWith(`${periode}-`)) {
    alert('Tanggal harus sesuai dengan periode bulan yang dipilih.')
    return
  }

  const templates = (guruDailyTaskState.templates || []).filter(item => item.aktif !== false && String(item.tanggal || '') === tanggal)
  if (!templates.length) {
    alert('Tidak ada tugas pada tanggal ini.')
    return
  }

  const nowIso = new Date().toISOString()
  const payload = templates.map(item => {
    const tid = String(item.id || '')
    const checked = Boolean(document.querySelector(`[data-guru-task-template-id="${tid}"]`)?.checked)
    const catatan = String(document.querySelector(`[data-guru-task-catatan-id="${tid}"]`)?.value || '').trim()
    return {
      template_id: tid,
      guru_id: guruId,
      tanggal,
      status: checked ? 'selesai' : 'belum',
      catatan: catatan || null,
      submitted_at: nowIso,
      updated_at: nowIso
    }
  })

  const { error } = await sb
    .from(DAILY_TASK_SUBMIT_TABLE)
    .upsert(payload, { onConflict: 'template_id,guru_id,tanggal' })

  if (error) {
    console.error(error)
    if (isMissingDailyTaskTableError(error)) {
      alert(buildDailyTaskMissingTableMessage())
      return
    }
    alert(`Gagal submit tugas harian: ${error.message || 'Unknown error'}`)
    return
  }

  alert('Tugas harian berhasil disubmit.')
  clearGuruPageCache('tugas')
  await renderTugasHarianPage(true)
}

function formatGuruDashboardCalendarDate(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleDateString('id-ID', { day: '2-digit', month: 'short', year: 'numeric' })
}

function getGuruDashboardCalendarRangeLabel(item) {
  const startKey = getTopbarKalenderDateKey(item?.mulai)
  const endKey = getTopbarKalenderDateKey(item?.selesai || item?.mulai)
  if (!startKey) return '-'
  if (!endKey || endKey === startKey) return formatGuruDashboardCalendarDate(startKey)
  return `${formatGuruDashboardCalendarDate(startKey)} - ${formatGuruDashboardCalendarDate(endKey)}`
}

async function renderDashboard() {
  const content = document.getElementById('guru-content')
  if (!content) return

  content.innerHTML = '<div class="placeholder-card">Loading agenda kalender akademik...</div>'

  try {
    await loadTopbarCalendarData(false)
    const rows = (topbarKalenderState.list || []).slice()
      .sort((a, b) => String(a?.mulai || '').localeCompare(String(b?.mulai || '')))
    guruDashboardAgendaRows = rows

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
      const rentang = getGuruDashboardCalendarRangeLabel(item)
      const itemId = String(item?.id || '')
      return `
        <button type="button" onclick="openGuruDashboardAgendaPopup('${escapeHtml(itemId)}')" style="text-align:left; width:100%; min-height:210px; position:relative; border:1px solid #e2e8f0; border-radius:16px; background:linear-gradient(180deg,#ffffff 0%,#f8fafc 100%); box-shadow:0 12px 24px rgba(15,23,42,0.08); padding:22px 20px 18px 22px; overflow:hidden; cursor:pointer;">
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

function ensureGuruDashboardAgendaPopup() {
  let popup = document.getElementById('guru-dashboard-agenda-popup')
  if (popup) return popup
  popup = document.createElement('div')
  popup.id = 'guru-dashboard-agenda-popup'
  popup.style.cssText = 'position:fixed; inset:0; background:rgba(15,23,42,0.35); display:none; align-items:center; justify-content:center; z-index:10001; padding:16px; box-sizing:border-box;'
  popup.innerHTML = `
    <div style="width:min(680px, calc(100vw - 32px)); max-height:calc(100vh - 32px); overflow:auto; border:1px solid #dbeafe; border-radius:0; background:#fff; box-shadow:0 18px 34px rgba(15,23,42,0.18); padding:14px 16px; position:relative;">
      <button type="button" onclick="closeGuruDashboardAgendaPopup()" style="position:absolute; right:12px; top:10px; border:1px solid #cbd5e1; background:#fff; border-radius:999px; width:28px; height:28px; cursor:pointer;">×</button>
      <div id="guru-dashboard-agenda-popup-body"></div>
    </div>
  `
  popup.addEventListener('click', event => {
    if (event.target !== popup) return
    closeGuruDashboardAgendaPopup()
  })
  document.body.appendChild(popup)
  return popup
}

function openGuruDashboardAgendaPopup(id) {
  const sid = String(id || '')
  const selected = guruDashboardAgendaRows.find(item => String(item?.id || '') === sid)
  if (!selected) return
  const popup = ensureGuruDashboardAgendaPopup()
  const body = document.getElementById('guru-dashboard-agenda-popup-body')
  if (!popup || !body) return
  const warna = normalizeTopbarKalenderColor(selected?.warna)
  body.innerHTML = `
    <div style="display:flex; align-items:flex-start; justify-content:space-between; gap:10px; margin-bottom:8px; padding-right:30px;">
      <div style="font-size:20px; font-weight:700; color:#0f172a; line-height:1.35;">${escapeHtml(selected?.judul || '-')}</div>
      <span style="width:12px; height:12px; border-radius:999px; background:${escapeHtml(warna)}; margin-top:8px;"></span>
    </div>
    <div style="font-size:12px; font-weight:600; color:#334155; margin-bottom:8px;">${escapeHtml(getGuruDashboardCalendarRangeLabel(selected))}</div>
    <div style="font-size:14px; color:#475569; line-height:1.7; white-space:pre-wrap;">${escapeHtml(selected?.detail || '-')}</div>
  `
  popup.style.display = 'flex'
}

function closeGuruDashboardAgendaPopup() {
  const popup = document.getElementById('guru-dashboard-agenda-popup')
  if (!popup) return
  popup.style.display = 'none'
}

function ensureLaporanBulananBulkModal() {
  let overlay = document.getElementById('laporan-bulanan-bulk-overlay')
  if (overlay) return overlay
  overlay = document.createElement('div')
  overlay.id = 'laporan-bulanan-bulk-overlay'
  overlay.style.cssText = 'position:fixed; inset:0; background:rgba(15,23,42,0.35); display:none; align-items:center; justify-content:center; z-index:11000; padding:16px; box-sizing:border-box;'
  overlay.innerHTML = `
    <div style="width:min(1180px, calc(100vw - 32px)); max-height:calc(100vh - 32px); overflow:hidden; border:1px solid #dbeafe; border-radius:0; background:#fff; box-shadow:0 18px 34px rgba(15,23,42,0.18); display:flex; flex-direction:column;">
      <div style="display:flex; align-items:center; justify-content:space-between; padding:12px 14px; border-bottom:1px solid #e2e8f0; gap:10px;">
        <div style="font-weight:700; color:#0f172a;">Input Massal Laporan Bulanan</div>
        <button type="button" class="modal-btn" onclick="closeLaporanBulananBulkInputModal()">Tutup</button>
      </div>
      <div id="laporan-bulanan-bulk-body" style="padding:12px; overflow:auto;">Loading...</div>
    </div>
  `
  overlay.addEventListener('click', event => {
    if (event.target !== overlay) return
    closeLaporanBulananBulkInputModal()
  })
  document.body.appendChild(overlay)
  return overlay
}

function closeLaporanBulananBulkInputModal() {
  const overlay = document.getElementById('laporan-bulanan-bulk-overlay')
  if (!overlay) return
  overlay.style.display = 'none'
}

function onLaporanBulananBulkAkhlakChange(selectEl) {
  const grade = normalizeAkhlakGrade(selectEl?.value)
  const row = selectEl?.closest('tr[data-lap-bulk-row="1"]')
  if (!row) return
  const predikatInput = row.querySelector('[data-bulk-field="predikat"]')
  if (!predikatInput) return
  predikatInput.value = grade ? getAkhlakKeteranganByGrade(grade) : ''
}

async function openLaporanBulananBulkInputModal() {
  const santriList = laporanBulananState.santriList || []
  const kelasMap = laporanBulananState.kelasMap || new Map()
  const guru = laporanBulananState.guru
  const periode = laporanBulananState.periode || getMonthInputToday()
  if (!guru?.id || !santriList.length) {
    alert('Data laporan bulanan belum siap untuk input massal.')
    return
  }

  const overlay = ensureLaporanBulananBulkModal()
  const body = document.getElementById('laporan-bulanan-bulk-body')
  if (!overlay || !body) return
  overlay.style.display = 'flex'
  body.innerHTML = 'Loading data input massal...'

  const santriIds = santriList.map(item => String(item.id || '')).filter(Boolean)
  const reportMap = new Map()
  const reportRes = await sb
    .from(MONTHLY_REPORT_TABLE)
    .select('santri_id, nilai_akhlak, predikat, catatan_wali')
    .eq('periode', periode)
    .eq('guru_id', String(guru.id))
    .in('santri_id', santriIds)

  if (reportRes.error) {
    if (isMissingMonthlyReportTableError(reportRes.error)) {
      body.innerHTML = `<div class="placeholder-card">${escapeHtml(buildMonthlyReportMissingTableMessage())}</div>`
      return
    }
    console.error(reportRes.error)
    body.innerHTML = `<div class="placeholder-card">Gagal load data laporan bulanan: ${escapeHtml(reportRes.error.message || 'Unknown error')}</div>`
    return
  }
  ;(reportRes.data || []).forEach(row => {
    reportMap.set(String(row.santri_id || ''), row)
  })

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
    const kelas = kelasMap.get(String(santri.kelas_id || ''))
    const report = reportMap.get(sid)
    const grade = report?.nilai_akhlak === null || report?.nilai_akhlak === undefined
      ? ''
      : normalizeAkhlakGrade(report.nilai_akhlak)
    const predikat = grade ? getAkhlakKeteranganByGrade(grade) : String(report?.predikat || '').trim()
    const catatan = String(report?.catatan_wali || '').trim()
    return `
      <tr data-lap-bulk-row="1" data-santri-id="${escapeHtml(sid)}" data-kelas-id="${escapeHtml(String(santri.kelas_id || ''))}">
        <td style="padding:6px; border:1px solid #e2e8f0; text-align:center;">${idx + 1}</td>
        <td style="padding:6px; border:1px solid #e2e8f0; min-width:220px;">${escapeHtml(santri.nama || '-')}</td>
        <td style="padding:6px; border:1px solid #e2e8f0; min-width:120px;">${escapeHtml(kelas?.nama_kelas || '-')}</td>
        <td style="padding:6px; border:1px solid #e2e8f0; min-width:120px;">
          <select data-bulk-field="nilai-akhlak" class="guru-field" onchange="onLaporanBulananBulkAkhlakChange(this)">${gradeOptions(grade)}</select>
        </td>
        <td style="padding:6px; border:1px solid #e2e8f0; min-width:170px;">
          <input data-bulk-field="predikat" class="guru-field" type="text" value="${escapeHtml(predikat)}" readonly style="background:#f8fafc; color:#475569;">
        </td>
        <td style="padding:6px; border:1px solid #e2e8f0; min-width:320px;">
          <input data-bulk-field="catatan" class="guru-field" type="text" value="${escapeHtml(catatan)}">
        </td>
      </tr>
    `
  }).join('')

  body.innerHTML = `
    <div style="display:grid; gap:8px;">
      <div style="display:flex; align-items:center; justify-content:space-between; gap:8px; flex-wrap:wrap;">
        <div style="font-size:13px; color:#334155;">
          Periode: <strong>${escapeHtml(getPeriodeLabel(periode))}</strong> | Wali Kelas: <strong>${escapeHtml(String(guru?.nama || '-'))}</strong>
        </div>
        <button type="button" class="modal-btn modal-btn-primary" onclick="saveLaporanBulananBulkInput()">Simpan Semua</button>
      </div>
      <div style="overflow:auto; border:1px solid #e2e8f0; border-radius:10px;">
        <table style="width:100%; min-width:980px; border-collapse:collapse; font-size:12px;">
          <thead>
            <tr style="background:#f8fafc;">
              <th style="padding:6px; border:1px solid #e2e8f0; width:44px;">No</th>
              <th style="padding:6px; border:1px solid #e2e8f0; text-align:left;">Nama Santri</th>
              <th style="padding:6px; border:1px solid #e2e8f0; text-align:left;">Kelas</th>
              <th style="padding:6px; border:1px solid #e2e8f0;">Nilai Akhlak</th>
              <th style="padding:6px; border:1px solid #e2e8f0;">Predikat</th>
              <th style="padding:6px; border:1px solid #e2e8f0;">Catatan Wali Kelas</th>
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

async function saveLaporanBulananBulkInput() {
  const guru = laporanBulananState.guru
  const periode = laporanBulananState.periode || getMonthInputToday()
  if (!guru?.id) {
    alert('Data guru tidak valid.')
    return
  }
  const rows = Array.from(document.querySelectorAll('#laporan-bulanan-bulk-body tr[data-lap-bulk-row="1"]'))
  if (!rows.length) {
    alert('Tidak ada data untuk disimpan.')
    return
  }

  const payload = []
  const validationErrors = []
  rows.forEach(row => {
    const sid = String(row.getAttribute('data-santri-id') || '').trim()
    const kelasId = String(row.getAttribute('data-kelas-id') || '').trim()
    const nama = String(row.children?.[1]?.textContent || '-').trim()
    const gradeRaw = String(row.querySelector('[data-bulk-field="nilai-akhlak"]')?.value || '').trim().toUpperCase()
    const grade = gradeRaw ? normalizeAkhlakGrade(gradeRaw) : ''
    const catatan = String(row.querySelector('[data-bulk-field="catatan"]')?.value || '').trim()

    if (gradeRaw && !grade) validationErrors.push(`${nama}: Nilai akhlak harus A-E.`)
    if (!sid || !kelasId) validationErrors.push(`${nama}: Data santri/kelas tidak valid.`)
    if (!sid || !kelasId) return

    payload.push({
      periode,
      guru_id: String(guru.id),
      kelas_id: kelasId,
      santri_id: sid,
      nilai_akhlak: grade ? round2(getAkhlakNumericValueByGrade(grade)) : null,
      predikat: grade ? getAkhlakKeteranganByGrade(grade) : null,
      catatan_wali: catatan || null
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
    if (isMissingMonthlyReportTableError(error)) {
      alert(buildMonthlyReportMissingTableMessage())
      return
    }
    if (isMissingMonthlyReportColumnError(error)) {
      alert(buildMonthlyReportMissingColumnsMessage())
      return
    }
    alert(`Gagal simpan input massal: ${error.message || 'Unknown error'}`)
    return
  }

  alert(`Input massal berhasil disimpan (${payload.length} santri).`)
  closeLaporanBulananBulkInputModal()
  clearGuruPageCache('laporan-bulanan')
  await renderLaporanBulananPage(true)
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

  window.showPopupConfirm = function showPopupConfirm(message) {
    return new Promise(resolve => {
      messageEl.textContent = String(message ?? '')
      actionsEl.innerHTML = ''

      const cancelBtn = document.createElement('button')
      cancelBtn.type = 'button'
      cancelBtn.textContent = 'Batal'
      cancelBtn.onclick = () => {
        closePopup()
        resolve(false)
      }

      const okConfirmBtn = document.createElement('button')
      okConfirmBtn.type = 'button'
      okConfirmBtn.textContent = 'Ya'
      okConfirmBtn.className = 'app-popup-primary'
      okConfirmBtn.onclick = () => {
        closePopup()
        resolve(true)
      }

      actionsEl.appendChild(cancelBtn)
      actionsEl.appendChild(okConfirmBtn)

      overlay.classList.add('open')
      overlay.setAttribute('aria-hidden', 'false')
      okConfirmBtn.focus()
    })
  }

  window.alert = function customAlert(message) {
    window.showPopupMessage(message)
  }

  overlay.addEventListener('click', event => {
    if (event.target !== overlay) return
    const cancelButton = actionsEl.querySelector('button:not(.app-popup-primary)')
    if (cancelButton) {
      cancelButton.click()
      return
    }
    const primaryButton = actionsEl.querySelector('button.app-popup-primary') || actionsEl.querySelector('button')
    if (primaryButton) primaryButton.click()
  })

  window.__popupReady = true
}

function ensureWaTargetModal() {
  if (document.getElementById('wa-target-overlay')) return

  const overlay = document.createElement('div')
  overlay.id = 'wa-target-overlay'
  overlay.className = 'app-popup-overlay'
  overlay.setAttribute('aria-hidden', 'true')
  overlay.innerHTML = `
    <div class="app-popup-card" role="dialog" aria-modal="true" aria-labelledby="wa-target-title">
      <div id="wa-target-title" class="app-popup-message" style="font-weight:700; margin-bottom:8px;">Kirim kemana?</div>
      <div style="margin-bottom:8px; font-size:13px; color:#475569;">Nomor tujuan bisa diubah sebelum lanjut kirim.</div>
      <div id="wa-target-choice-wrap" style="display:none; margin-bottom:8px;">
        <label class="guru-label" for="wa-target-choice">Pilih nomor orang tua</label>
        <select id="wa-target-choice" class="guru-field"></select>
      </div>
      <input id="wa-target-input" class="guru-field" type="text" placeholder="Masukkan nomor WhatsApp">
      <div class="app-popup-actions" style="margin-top:12px;">
        <button type="button" id="wa-target-cancel-btn">Batal</button>
        <button type="button" id="wa-target-send-btn" class="app-popup-primary">Lanjut</button>
      </div>
    </div>
  `

  overlay.addEventListener('click', event => {
    if (event.target !== overlay) return
    closeWaTargetModal(null)
  })

  document.body.appendChild(overlay)
}

function closeWaTargetModal(value = null) {
  const overlay = document.getElementById('wa-target-overlay')
  if (!overlay) return
  overlay.classList.remove('open')
  overlay.setAttribute('aria-hidden', 'true')
  if (waTargetModalResolver) {
    waTargetModalResolver(value)
    waTargetModalResolver = null
  }
}

function askWaTargetNumber(defaultNumber = '', choices = []) {
  ensureWaTargetModal()
  const overlay = document.getElementById('wa-target-overlay')
  const input = document.getElementById('wa-target-input')
  const choiceWrap = document.getElementById('wa-target-choice-wrap')
  const choiceSelect = document.getElementById('wa-target-choice')
  const cancelBtn = document.getElementById('wa-target-cancel-btn')
  const sendBtn = document.getElementById('wa-target-send-btn')
  if (!overlay || !input || !cancelBtn || !sendBtn || !choiceWrap || !choiceSelect) return Promise.resolve(null)

  return new Promise(resolve => {
    waTargetModalResolver = resolve

    const validChoices = (choices || []).filter(item => String(item?.number || '').trim() !== '')
    if (validChoices.length > 0) {
      choiceSelect.innerHTML = ''
      validChoices.forEach((item, index) => {
        const opt = document.createElement('option')
        opt.value = String(item.number || '')
        opt.textContent = `${item.label || `Nomor ${index + 1}`} (${item.number})`
        choiceSelect.appendChild(opt)
      })
      const manualOpt = document.createElement('option')
      manualOpt.value = '__manual__'
      manualOpt.textContent = 'Nomor Lain (manual)'
      choiceSelect.appendChild(manualOpt)

      choiceWrap.style.display = 'block'
      choiceSelect.onchange = () => {
        const selected = String(choiceSelect.value || '')
        if (selected === '__manual__') {
          input.value = ''
          input.focus()
          return
        }
        input.value = selected
      }
      input.value = String(validChoices[0].number || '')
    } else {
      choiceWrap.style.display = 'none'
      choiceSelect.innerHTML = ''
      choiceSelect.onchange = null
      input.value = String(defaultNumber || '')
    }

    input.focus()
    input.select()

    cancelBtn.onclick = () => closeWaTargetModal(null)
    sendBtn.onclick = () => closeWaTargetModal(input.value)
    input.onkeydown = event => {
      if (event.key === 'Enter') {
        event.preventDefault()
        closeWaTargetModal(input.value)
      }
      if (event.key === 'Escape') {
        event.preventDefault()
        closeWaTargetModal(null)
      }
    }

    overlay.classList.add('open')
    overlay.setAttribute('aria-hidden', 'false')
  })
}

async function popupConfirm(message) {
  if (typeof window.showPopupConfirm === 'function') {
    return await window.showPopupConfirm(message)
  }
  return confirm(message)
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

function loadGuruNotifPrefs() {
  try {
    const raw = localStorage.getItem(TOPBAR_NOTIF_READ_KEY)
    const parsed = raw ? JSON.parse(raw) : {}
    topbarNotifState.readMap = parsed && typeof parsed === 'object' ? parsed : {}
  } catch (_err) {
    topbarNotifState.readMap = {}
  }

  window.showPopupChoices = function showPopupChoices(message, choices = []) {
    return new Promise(resolve => {
      messageEl.textContent = String(message ?? '')
      actionsEl.innerHTML = ''

      const list = Array.isArray(choices) ? choices : []
      if (!list.length) {
        const btn = document.createElement('button')
        btn.type = 'button'
        btn.textContent = 'Tutup'
        btn.className = 'app-popup-primary'
        btn.onclick = () => {
          closePopup()
          resolve('')
        }
        actionsEl.appendChild(btn)
      } else {
        list.forEach((choice, idx) => {
          const btn = document.createElement('button')
          btn.type = 'button'
          btn.textContent = String(choice?.label || `Opsi ${idx + 1}`)
          if (choice?.primary) btn.className = 'app-popup-primary'
          btn.onclick = () => {
            closePopup()
            resolve(String(choice?.value || ''))
          }
          actionsEl.appendChild(btn)
        })
      }

      overlay.classList.add('open')
      overlay.setAttribute('aria-hidden', 'false')
      const focusBtn = actionsEl.querySelector('button.app-popup-primary') || actionsEl.querySelector('button')
      if (focusBtn) focusBtn.focus()
    })
  }
  const rangeRaw = Number(localStorage.getItem(TOPBAR_NOTIF_RANGE_KEY) || '3')
  topbarNotifState.rangeDays = [1, 3, 7].includes(rangeRaw) ? rangeRaw : 3
}

function saveGuruNotifReadMap() {
  try {
    localStorage.setItem(TOPBAR_NOTIF_READ_KEY, JSON.stringify(topbarNotifState.readMap || {}))
  } catch (_err) {}
}

function setGuruNotifRangeDays(days) {
  const value = Number(days)
  topbarNotifState.rangeDays = [1, 3, 7].includes(value) ? value : 3
  localStorage.setItem(TOPBAR_NOTIF_RANGE_KEY, String(topbarNotifState.rangeDays))
}

function buildGuruNotifDateKeys() {
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

function getGuruNotifId(item) {
  return String(item?.id || `${item?.type || ''}|${item?.title || ''}|${item?.meta || ''}|${item?.sortKey || ''}`)
}

function isGuruNotifRead(item) {
  return topbarNotifState.readMap?.[getGuruNotifId(item)] === true
}

function markGuruNotifRead(id) {
  const key = String(id || '').trim()
  if (!key) return
  if (!topbarNotifState.readMap) topbarNotifState.readMap = {}
  topbarNotifState.readMap[key] = true
  saveGuruNotifReadMap()
}

function markGuruNotifItemRead(id) {
  markGuruNotifRead(id)
  renderTopbarNotifMenu(topbarNotifState.items)
  setTopbarNotifBadge((topbarNotifState.items || []).filter(item => !isGuruNotifRead(item)).length)
}

function markAllGuruNotifRead() {
  ;(topbarNotifState.items || []).forEach(item => markGuruNotifRead(getGuruNotifId(item)))
  renderTopbarNotifMenu(topbarNotifState.items)
  setTopbarNotifBadge(0)
}

function setGuruNotifRangeFilter(days) {
  setGuruNotifRangeDays(days)
  topbarNotifState.loaded = false
  refreshGuruTopbarNotifications(true).catch(error => console.error(error))
}

function renderTopbarNotifMenu(items = []) {
  const menu = document.getElementById('topbar-notif-menu')
  if (!menu) return

  const list = Array.isArray(items) ? items : []
  const selectedRange = Number(topbarNotifState.rangeDays || 3)
  const filtersHtml = [1, 3, 7].map(days => (
    `<button type="button" class="topbar-notif-filter-btn ${selectedRange === days ? 'active' : ''}" onclick="setGuruNotifRangeFilter(${days})">${days === 1 ? 'Hari ini' : `${days} hari`}</button>`
  )).join('')
  const headHtml = `
    <div class="topbar-notif-head">
      <div class="topbar-notif-head-row">
        <span>Aktivitas Terdekat</span>
        <button type="button" class="topbar-notif-mark-btn" onclick="markAllGuruNotifRead()">Tandai semua dibaca</button>
      </div>
      <div class="topbar-notif-filters">${filtersHtml}</div>
    </div>
  `

  if (!list.length) {
    menu.innerHTML = `${headHtml}<div class="topbar-notif-empty">Belum ada notifikasi aktivitas terdekat.</div>`
    return
  }

  const rowsHtml = list.map(item => `
    <button type="button" class="topbar-notif-item ${isGuruNotifRead(item) ? 'read' : 'unread'}" data-notif-id="${escapeHtml(getGuruNotifId(item))}" onclick="markGuruNotifItemRead(this.getAttribute('data-notif-id'))">
      <div class="topbar-notif-type">${escapeHtml(item.type || 'Aktivitas')}</div>
      <div class="topbar-notif-title">${escapeHtml(item.title || '-')}</div>
      <div class="topbar-notif-meta">${escapeHtml(item.meta || '-')}</div>
      ${item.desc ? `<div class="topbar-notif-desc">${escapeHtml(item.desc)}</div>` : ''}
      <div class="topbar-notif-status">${isGuruNotifRead(item) ? 'Dibaca' : 'Belum dibaca'}</div>
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

async function fetchGuruTopbarNotifications() {
  const items = []
  const dateKeys = buildGuruNotifDateKeys()

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
    const ctx = await getGuruContext()
    const distribusiMap = new Map((ctx?.activeDistribusiList || []).map(item => [String(item.id || ''), item]))
    ;(ctx?.jadwalList || []).forEach(item => {
      const distribusi = distribusiMap.get(String(item?.distribusi_id || ''))
      if (!distribusi) return
      dateKeys.forEach(dateKey => {
        const dayName = normalizeHari(getHariFromDate(dateKey))
        if (!dayName || normalizeHari(item?.hari) !== dayName) return
        const kelas = ctx?.kelasMap?.get(String(distribusi?.kelas_id || ''))
        const mapel = ctx?.mapelMap?.get(String(distribusi?.mapel_id || ''))
        items.push({
          id: `ajar|${dateKey}|${String(item?.id || '')}|${String(distribusi?.id || '')}`,
          type: 'Jam Mengajar',
          title: `${getMapelLabel(mapel)} - ${String(kelas?.nama_kelas || '-')}`,
          meta: `${formatNotifDateLabel(dateKey)} | ${toTimeLabel(item?.jam_mulai)}-${toTimeLabel(item?.jam_selesai)}`,
          desc: '',
          sortKey: `${dateKey} ${String(item?.jam_mulai || '00:00')}`
        })
      })
    })
  } catch (error) {
    console.error(error)
  }

  items.sort((a, b) => String(a.sortKey || '').localeCompare(String(b.sortKey || '')))
  return items.slice(0, 30)
}

async function refreshGuruTopbarNotifications(forceReload = false) {
  ensureTopbarNotification()
  if (!forceReload && topbarNotifState.loaded) {
    renderTopbarNotifMenu(topbarNotifState.items)
    setTopbarNotifBadge((topbarNotifState.items || []).filter(item => !isGuruNotifRead(item)).length)
    return
  }
  const items = await fetchGuruTopbarNotifications()
  topbarNotifState.items = items
  topbarNotifState.loaded = true
  renderTopbarNotifMenu(items)
  setTopbarNotifBadge(items.filter(item => !isGuruNotifRead(item)).length)
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
  await refreshGuruTopbarNotifications(true)
}

async function getCurrentGuruRow() {
  const loginId = localStorage.getItem('login_id')
  if (!loginId) return null
  const now = Date.now()
  if (currentGuruRowCache && currentGuruRowCache.loginId === loginId && (now - currentGuruRowCache.ts) < 60 * 1000) {
    return currentGuruRowCache.data
  }

  const { data, error } = await sb
    .from('karyawan')
    .select('id, id_karyawan, nama, role, no_hp, alamat, aktif')
    .eq('id_karyawan', loginId)
    .maybeSingle()

  if (error) throw error
  currentGuruRowCache = {
    loginId,
    ts: now,
    data: data || null
  }
  return data || null
}

async function setGuruWelcomeName() {
  const welcomeEl = document.getElementById('welcome')
  if (!welcomeEl) return

  try {
    const guru = await getCurrentGuruRow()
    const name = String(guru?.nama || '').trim()
    if (name) {
      welcomeEl.textContent = name
      return
    }
  } catch (error) {
    console.error(error)
  }

  welcomeEl.textContent = String(localStorage.getItem('login_id') || '').trim()
}

async function getActiveSemester() {
  const tahunAktif = await getActiveTahunAjaran()
  const tahunAjaranId = tahunAktif?.id || null
  const cacheKey = String(tahunAjaranId || '')
  if (activeSemesterCache && activeSemesterCache.key === cacheKey) {
    return activeSemesterCache.value
  }

  let query = sb
    .from('semester')
    .select('id, nama, aktif, tahun_ajaran_id')
    .order('id', { ascending: false })

  if (tahunAjaranId) query = query.eq('tahun_ajaran_id', tahunAjaranId)

  const { data, error } = await query
  if (error) {
    console.error(error)
    activeSemesterCache = { key: cacheKey, value: null }
    return null
  }

  const rows = data || []
  const picked = rows.find(item => asBool(item.aktif)) || rows[0] || null
  activeSemesterCache = { key: cacheKey, value: picked }
  return picked
}

async function getMapelRowsByIds(mapelIds = []) {
  if (!mapelIds.length) return { data: [], error: null }

  const attempts = [
    'id, nama, kategori, jenis, kkm, tingkat, tingkatan',
    'id, nama, kategori, jenis, kkm, tingkat',
    'id, nama, kategori, jenis, kkm, tingkatan',
    'id, nama, kategori, jenis, kkm',
    'id, nama, kategori, kkm, tingkat, tingkatan',
    'id, nama, kategori, kkm, tingkat',
    'id, nama, kategori, kkm, tingkatan',
    'id, nama, kategori, kkm',
    'id, nama, kkm',
    'id, nama, kategori, jenis, tingkat, tingkatan',
    'id, nama, kategori, jenis, tingkat',
    'id, nama, kategori, jenis, tingkatan',
    'id, nama, kategori, jenis',
    'id, nama, kategori, tingkat, tingkatan',
    'id, nama, kategori, tingkat',
    'id, nama, kategori, tingkatan',
    'id, nama, kategori'
  ]

  for (const fields of attempts) {
    const res = await sb.from('mapel').select(fields).in('id', mapelIds)
    if (!res.error) return res
  }

  return await sb.from('mapel').select('id, nama, kategori').in('id', mapelIds)
}

async function getGuruContext(forceReload = false) {
  if (!forceReload && guruContextCache) return guruContextCache

  const guru = await getCurrentGuruRow()
  if (!guru?.id) {
    guruContextCache = {
      guru: null,
      activeTahunAjaran: null,
      activeSemester: null,
      distribusiList: [],
      yearDistribusiList: [],
      activeDistribusiList: [],
      kelasMap: new Map(),
      mapelMap: new Map(),
      semesterMap: new Map(),
      jadwalList: [],
      jamList: []
    }
    return guruContextCache
  }

  const activeTahunAjaran = await getActiveTahunAjaran()
  const activeSemester = await getActiveSemester()
  const activeTahunAjaranId = String(activeTahunAjaran?.id || '')

  const distribusiRes = await sb
    .from('distribusi_mapel')
    .select('id, kelas_id, mapel_id, guru_id, semester_id')
    .eq('guru_id', guru.id)

  if (distribusiRes.error) throw distribusiRes.error

  const distribusiList = distribusiRes.data || []
  const semesterIds = [...new Set(distribusiList.map(item => item.semester_id).filter(Boolean))]

  const semesterRes = semesterIds.length
    ? await sb.from('semester').select('id, nama, aktif, tahun_ajaran_id').in('id', semesterIds)
    : { data: [], error: null }
  if (semesterRes.error) console.error(semesterRes.error)

  const semesterMap = new Map((semesterRes.data || []).map(item => [String(item.id), item]))

  const yearDistribusiList = activeTahunAjaranId
    ? distribusiList.filter(item => String(semesterMap.get(String(item.semester_id || ''))?.tahun_ajaran_id || '') === activeTahunAjaranId)
    : distribusiList

  const activeDistribusiList = activeSemester?.id
    ? yearDistribusiList.filter(item => String(item.semester_id || '') === String(activeSemester.id))
    : yearDistribusiList

  const kelasIds = [...new Set(yearDistribusiList.map(item => item.kelas_id).filter(Boolean))]
  const mapelIds = [...new Set(yearDistribusiList.map(item => item.mapel_id).filter(Boolean))]
  const distribusiIds = [...new Set(yearDistribusiList.map(item => item.id).filter(Boolean))]

  const supportJamTahunAjaran = await checkJamPelajaranSupportsTahunAjaran()
  let jamQuery = sb
    .from('jam_pelajaran')
    .select('id, nama, jam_mulai, jam_selesai, aktif, urutan')
    .order('urutan', { ascending: true })
    .order('jam_mulai', { ascending: true })
  if (supportJamTahunAjaran && activeTahunAjaranId) {
    jamQuery = jamQuery.eq('tahun_ajaran_id', activeTahunAjaranId)
  }

  const [kelasRes, mapelRes, jadwalRes, jamRes] = await Promise.all([
    kelasIds.length ? sb.from('kelas').select('id, nama_kelas, tingkat, tahun_ajaran_id').in('id', kelasIds) : Promise.resolve({ data: [] }),
    getMapelRowsByIds(mapelIds),
    distribusiIds.length ? sb.from('jadwal_pelajaran').select('id, distribusi_id, hari, jam_mulai, jam_selesai').in('distribusi_id', distribusiIds) : Promise.resolve({ data: [] }),
    jamQuery
  ])

  if (kelasRes.error) console.error(kelasRes.error)
  if (mapelRes.error) console.error(mapelRes.error)
  if (jadwalRes.error) console.error(jadwalRes.error)

  let jamList = []
  if (!jamRes.error) {
    jamList = jamRes.data || []
  } else {
    const jamRetry = await sb
      .from('jam_pelajaran')
      .select('id, nama, jam_mulai, jam_selesai')
      .order('jam_mulai', { ascending: true })
    if (!jamRetry.error) jamList = jamRetry.data || []
  }

  const kelasMap = new Map((kelasRes.data || []).map(item => [String(item.id), item]))
  const mapelMap = new Map((mapelRes.data || []).map(item => [String(item.id), item]))

  guruContextCache = {
    guru,
    activeTahunAjaran,
    activeSemester,
    distribusiList,
    yearDistribusiList,
    activeDistribusiList,
    kelasMap,
    mapelMap,
    semesterMap,
    jadwalList: jadwalRes.data || [],
    jamList
  }

  return guruContextCache
}

async function getSantriByKelas(kelasId) {
  if (!kelasId) return []
  const tahunAktif = await getActiveTahunAjaran()
  if (tahunAktif?.id) {
    const { data: kelasRow, error: kelasError } = await sb
      .from('kelas')
      .select('id')
      .eq('id', kelasId)
      .eq('tahun_ajaran_id', tahunAktif.id)
      .maybeSingle()
    if (kelasError) {
      console.error(kelasError)
      return []
    }
    if (!kelasRow) return []
  }

  const { data, error } = await sb
    .from('santri')
    .select('id, nama, kelas_id')
    .eq('kelas_id', kelasId)
    .eq('aktif', true)
    .order('nama')

  if (error) {
    console.error(error)
    return []
  }
  return data || []
}

function buildAbsensiMissingTableMessage() {
  return `Tabel '${ATTENDANCE_TABLE}' belum ada di Supabase.\n\nSilakan buat tabel dulu dengan kolom minimal:\n- id (primary key)\n- tanggal (date)\n- kelas_id\n- mapel_id\n- guru_id\n- jam_pelajaran_id (nullable)\n- semester_id (nullable)\n- distribusi_id (nullable)\n- santri_id\n- status (text)\n- guru_pengganti_id (uuid, nullable)\n- keterangan_pengganti (text, nullable)`
}

function buildAbsensiPenggantiColumnsMessage() {
  return `Kolom guru pengganti belum tersedia di tabel '${ATTENDANCE_TABLE}'.\n\nJalankan SQL berikut di Supabase:\n\nalter table public.${ATTENDANCE_TABLE}\n  add column if not exists guru_pengganti_id uuid null,\n  add column if not exists keterangan_pengganti text null;`
}

function isMissingAbsensiPenggantiColumnError(error) {
  const msg = String(error?.message || '').toLowerCase()
  return msg.includes('guru_pengganti_id') || msg.includes('keterangan_pengganti')
}

function isMissingAbsensiTableError(error) {
  const code = String(error?.code || '').toUpperCase()
  const msg = String(error?.message || '').toLowerCase()
  if (code === '42P01') return true
  if (msg.includes(`table 'public.${ATTENDANCE_TABLE}'`.toLowerCase())) return true
  if (msg.includes('relation') && msg.includes(ATTENDANCE_TABLE.toLowerCase())) return true
  return false
}

function isMissingInputNilaiTableError(error) {
  const code = String(error?.code || '').toUpperCase()
  const msg = String(error?.message || '').toLowerCase()
  if (code === '42P01') return true
  if (msg.includes(`table 'public.${INPUT_NILAI_TABLE}'`.toLowerCase())) return true
  if (msg.includes('relation') && msg.includes(INPUT_NILAI_TABLE.toLowerCase())) return true
  return false
}

function buildInputNilaiMissingTableMessage() {
  return `Tabel '${INPUT_NILAI_TABLE}' belum ada di Supabase.\n\nSilakan buat tabel dengan kolom minimal:\n- id (primary key)\n- tanggal (date)\n- kelas_id\n- mapel_id\n- guru_id\n- semester_id (nullable)\n- distribusi_id (nullable)\n- santri_id\n- jenis (text: Tugas/Ulangan Harian/UTS/UAS/Keterampilan)\n- nilai (numeric)`
}

function isMissingRaporDescTableError(error) {
  const code = String(error?.code || '').toUpperCase()
  const msg = String(error?.message || '').toLowerCase()
  if (code === '42P01') return true
  if (msg.includes(`table 'public.${RAPOR_DESC_TABLE}'`.toLowerCase())) return true
  if (msg.includes('relation') && msg.includes(RAPOR_DESC_TABLE.toLowerCase())) return true
  return false
}

function buildRaporDescMissingTableMessage() {
  return `Tabel '${RAPOR_DESC_TABLE}' belum ada di Supabase.\n\nJalankan SQL berikut:\n\ncreate table if not exists public.${RAPOR_DESC_TABLE} (\n  id bigserial primary key,\n  distribusi_id text not null,\n  guru_id text not null,\n  mapel_id text not null,\n  semester_id text null,\n  deskripsi_a_pengetahuan text null,\n  deskripsi_b_pengetahuan text null,\n  deskripsi_c_pengetahuan text null,\n  deskripsi_d_pengetahuan text null,\n  deskripsi_a_keterampilan text null,\n  deskripsi_b_keterampilan text null,\n  deskripsi_c_keterampilan text null,\n  deskripsi_d_keterampilan text null,\n  updated_at timestamptz not null default now(),\n  unique (distribusi_id)\n);`
}

function isMissingMonthlyReportTableError(error) {
  const code = String(error?.code || '').toUpperCase()
  const msg = String(error?.message || '').toLowerCase()
  if (code === '42P01') return true
  if (msg.includes(`table 'public.${MONTHLY_REPORT_TABLE}'`.toLowerCase())) return true
  if (msg.includes('relation') && msg.includes(MONTHLY_REPORT_TABLE.toLowerCase())) return true
  return false
}

function buildMonthlyReportMissingTableMessage() {
  return `Tabel '${MONTHLY_REPORT_TABLE}' belum ada di Supabase.\n\nSilakan buat tabel dengan kolom minimal:\n- id (primary key)\n- periode (text, format: YYYY-MM)\n- guru_id\n- kelas_id\n- santri_id\n- nilai_akhlak (numeric, nullable)\n- predikat (text, nullable)\n- catatan_wali (text, nullable)\n- muhaffiz (text, nullable)\n- no_hp_muhaffiz (text, nullable)\n- nilai_kehadiran_halaqah (numeric, nullable)\n- sakit_halaqah (integer, nullable)\n- izin_halaqah (integer, nullable)\n- nilai_akhlak_halaqah (text, nullable)\n- keterangan_akhlak_halaqah (text, nullable)\n- nilai_ujian_bulanan (numeric, nullable)\n- keterangan_ujian_bulanan (text, nullable)\n- nilai_target_hafalan (numeric, nullable)\n- keterangan_target_hafalan (text, nullable)\n- nilai_capaian_hafalan_bulanan (numeric, nullable)\n- nilai_jumlah_hafalan_halaman (numeric, nullable)\n- nilai_jumlah_hafalan_juz (numeric, nullable)\n- catatan_muhaffiz (text, nullable)\n- musyrif (text, nullable)\n- no_hp_musyrif (text, nullable)\n- nilai_kehadiran_liqa_muhasabah (numeric, nullable)\n- sakit_liqa_muhasabah (integer, nullable)\n- izin_liqa_muhasabah (integer, nullable)\n- nilai_ibadah (text, nullable)\n- keterangan_ibadah (text, nullable)\n- nilai_kedisiplinan (text, nullable)\n- keterangan_kedisiplinan (text, nullable)\n- nilai_kebersihan (text, nullable)\n- keterangan_kebersihan (text, nullable)\n- nilai_adab (text, nullable)\n- keterangan_adab (text, nullable)\n- prestasi_kesantrian (text, nullable)\n- pelanggaran_kesantrian (text, nullable)\n- catatan_musyrif (text, nullable)\n\nDisarankan unique key: (periode, guru_id, kelas_id, santri_id).`
}

function isMissingMonthlyReportColumnError(error) {
  const msg = String(error?.message || '').toLowerCase()
  return msg.includes('column') && msg.includes(MONTHLY_REPORT_TABLE.toLowerCase())
}

function buildMonthlyReportMissingColumnsMessage() {
  return `Kolom Ketahfizan/Kesantrian di tabel '${MONTHLY_REPORT_TABLE}' belum ada.\n\nJalankan SQL berikut:\n\nalter table public.${MONTHLY_REPORT_TABLE}\n  add column if not exists muhaffiz text,\n  add column if not exists no_hp_muhaffiz text,\n  add column if not exists nilai_kehadiran_halaqah numeric,\n  add column if not exists sakit_halaqah integer,\n  add column if not exists izin_halaqah integer,\n  add column if not exists nilai_akhlak_halaqah text,\n  add column if not exists keterangan_akhlak_halaqah text,\n  add column if not exists nilai_ujian_bulanan numeric,\n  add column if not exists keterangan_ujian_bulanan text,\n  add column if not exists nilai_target_hafalan numeric,\n  add column if not exists keterangan_target_hafalan text,\n  add column if not exists nilai_capaian_hafalan_bulanan numeric,\n  add column if not exists nilai_jumlah_hafalan_halaman numeric,\n  add column if not exists nilai_jumlah_hafalan_juz numeric,\n  add column if not exists catatan_muhaffiz text,\n  add column if not exists musyrif text,\n  add column if not exists no_hp_musyrif text,\n  add column if not exists nilai_kehadiran_liqa_muhasabah numeric,\n  add column if not exists sakit_liqa_muhasabah integer,\n  add column if not exists izin_liqa_muhasabah integer,\n  add column if not exists nilai_ibadah text,\n  add column if not exists keterangan_ibadah text,\n  add column if not exists nilai_kedisiplinan text,\n  add column if not exists keterangan_kedisiplinan text,\n  add column if not exists nilai_kebersihan text,\n  add column if not exists keterangan_kebersihan text,\n  add column if not exists nilai_adab text,\n  add column if not exists keterangan_adab text,\n  add column if not exists prestasi_kesantrian text,\n  add column if not exists pelanggaran_kesantrian text,\n  add column if not exists catatan_musyrif text;`
}

function getGradeByScoreAtoE(value) {
  const num = Number(value)
  if (!Number.isFinite(num)) return ''
  return normalizeAkhlakGrade(num)
}

function getUjianBulananKeterangan(value) {
  const grade = getGradeByScoreAtoE(value)
  return grade || ''
}

function toJenisNilaiKey(jenis) {
  const value = String(jenis || '').trim().toLowerCase()
  if (value === 'tugas') return 'nilai_tugas'
  if (value === 'ulangan harian') return 'nilai_ulangan_harian'
  if (value === 'uts') return 'nilai_pts'
  if (value === 'uas') return 'nilai_pas'
  if (value === 'keterampilan') return 'nilai_keterampilan'
  return ''
}

function getJenisNilaiMax(jenis) {
  const value = String(jenis || '').trim().toLowerCase()
  if (value === 'tugas') return 5
  if (value === 'ulangan harian') return 10
  if (value === 'uts') return 25
  if (value === 'uas') return 50
  if (value === 'keterampilan') return 100
  return null
}

function round2(value) {
  const num = Number(value)
  if (!Number.isFinite(num)) return null
  return Math.round(num * 100) / 100
}

function calculateNilaiKehadiranFromRows(rows = [], maxScore = 10) {
  const total = rows.length
  if (!total) return null

  const hadirCount = rows.filter(row => {
    const status = String(row?.status || '').trim().toLowerCase()
    return status === 'hadir' || status === 'terlambat'
  }).length

  return round2((hadirCount / total) * maxScore)
}

function toNullableNumber(rawValue) {
  const text = String(rawValue ?? '').trim()
  if (!text) return null
  const num = Number(text)
  if (!Number.isFinite(num)) return NaN
  return num
}

function toInputValue(value) {
  return value === null || value === undefined ? '' : String(value)
}

function hitungNilaiPengetahuan(parts = {}) {
  const tugas = Number(parts.nilai_tugas || 0)
  const uh = Number(parts.nilai_ulangan_harian || 0)
  const pts = Number(parts.nilai_pts || 0)
  const pas = Number(parts.nilai_pas || 0)
  const kehadiran = Number(parts.nilai_kehadiran || 0)
  return tugas + uh + pts + pas + kehadiran
}

function validateRange(value, label, maxValue) {
  if (value === null) return true
  if (Number.isNaN(value)) {
    alert(`${label} harus berupa angka valid.`)
    return false
  }
  if (value < 0) {
    alert(`${label} tidak boleh kurang dari 0.`)
    return false
  }
  if (maxValue !== null && maxValue !== undefined && value > maxValue) {
    alert(`${label} maksimal ${maxValue}.`)
    return false
  }
  return true
}

function renderAbsensiSantriRows() {
  const box = document.getElementById('absensi-santri-list')
  if (!box) return

  if (!currentAbsensiSantriList.length) {
    box.innerHTML = '<div class="placeholder-card">Belum ada data siswa untuk kelas ini.</div>'
    return
  }

  let html = `
    <div style="overflow:auto; border:1px solid #e2e8f0; border-radius:10px; margin-top:10px;">
      <table style="width:100%; min-width:500px; border-collapse:collapse; font-size:13px;">
        <thead>
          <tr style="background:#f8fafc;">
            <th style="padding:10px; border:1px solid #e2e8f0; width:70px; text-align:center;">No</th>
            <th style="padding:10px; border:1px solid #e2e8f0; text-align:left;">Nama Siswa</th>
            <th style="padding:10px; border:1px solid #e2e8f0; width:180px; text-align:left;">Kehadiran</th>
          </tr>
        </thead>
        <tbody>
  `

  html += currentAbsensiSantriList.map((santri, index) => {
    const options = ATTENDANCE_STATUSES
      .map(status => `<option value="${status}" ${status === 'Hadir' ? 'selected' : ''}>${status}</option>`)
      .join('')

    return `
      <tr>
        <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${index + 1}</td>
        <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(santri.nama || '-')}</td>
        <td style="padding:8px; border:1px solid #e2e8f0;">
          <select class="guru-field" data-absen-santri-id="${escapeHtml(santri.id)}" style="width:100%;">
            ${options}
          </select>
        </td>
      </tr>
    `
  }).join('')

  html += '</tbody></table></div>'
  box.innerHTML = html
}

function getAbsensiDistribusiCandidates() {
  const kelasId = String(document.getElementById('absensi-kelas')?.value || '')
  const mapelId = String(document.getElementById('absensi-mapel')?.value || '')
  const ctx = guruContextCache
  if (!ctx) return []

  return (ctx.activeDistribusiList || []).filter(item => {
    return String(item.kelas_id || '') === kelasId && String(item.mapel_id || '') === mapelId
  })
}

function getSelectedAbsensiHari() {
  const tanggal = String(document.getElementById('absensi-tanggal')?.value || '').trim()
  if (!tanggal) return ''
  return normalizeHari(getHariFromDate(tanggal))
}

function renderAbsensiMapelOptions() {
  const mapelSelect = document.getElementById('absensi-mapel')
  if (!mapelSelect) return

  const prevValue = String(mapelSelect.value || '')
  const kelasId = String(document.getElementById('absensi-kelas')?.value || '')
  const selectedHari = getSelectedAbsensiHari()
  const ctx = guruContextCache
  const list = (ctx?.activeDistribusiList || []).filter(item => String(item.kelas_id || '') === kelasId)
  let defaultMapelId = ''
  if (selectedHari) {
    const distribusiById = new Map(list.map(item => [String(item.id || ''), item]))
    const firstMatch = (ctx?.jadwalList || []).find(item => {
      if (normalizeHari(item?.hari) !== selectedHari) return false
      return distribusiById.has(String(item.distribusi_id || ''))
    })
    if (firstMatch) {
      const distribusi = distribusiById.get(String(firstMatch.distribusi_id || ''))
      defaultMapelId = String(distribusi?.mapel_id || '')
    }
  }

  const uniqueMapelIds = [...new Set(list.map(item => String(item.mapel_id || '')).filter(Boolean))]
  mapelSelect.innerHTML = '<option value="">-- Pilih Mapel --</option>'

  uniqueMapelIds.forEach(mapelId => {
    const mapel = ctx?.mapelMap.get(mapelId)
    const opt = document.createElement('option')
    opt.value = mapelId
    opt.textContent = getMapelLabel(mapel)
    mapelSelect.appendChild(opt)
  })

  if (prevValue && uniqueMapelIds.includes(prevValue)) {
    mapelSelect.value = prevValue
    return
  }
  if (defaultMapelId && uniqueMapelIds.includes(defaultMapelId)) {
    mapelSelect.value = defaultMapelId
  }
}

function renderAbsensiJamOptions() {
  const jamSelect1 = document.getElementById('absensi-jam-1')
  const jamSelect2 = document.getElementById('absensi-jam-2')
  if (!jamSelect1 && !jamSelect2) return

  const candidates = getAbsensiDistribusiCandidates()
  const selectedHari = getSelectedAbsensiHari()
  const ctx = guruContextCache

  const jadwalMap = new Map()
  ;(ctx?.jadwalList || []).forEach(item => {
    if (selectedHari && normalizeHari(item?.hari) !== selectedHari) return
    if (!candidates.find(c => String(c.id) === String(item.distribusi_id))) return
    jadwalMap.set(String(item.id), item)
  })

  const selected1 = String(jamSelect1?.value || '')
  const selected2 = String(jamSelect2?.value || '')
  const targetSelects = [jamSelect1, jamSelect2].filter(Boolean)
  targetSelects.forEach(selectEl => {
    selectEl.innerHTML = '<option value="">-- Pilih Jam --</option>'
  })

  const jamByRange = new Map((ctx?.jamList || []).map(item => [`${toTimeLabel(item.jam_mulai)}|${toTimeLabel(item.jam_selesai)}`, item]))

  const jadwalList = Array.from(jadwalMap.values()).sort((a, b) => {
    const dayCmp = getHariOrder(a.hari) - getHariOrder(b.hari)
    if (dayCmp !== 0) return dayCmp
    return String(a.jam_mulai || '').localeCompare(String(b.jam_mulai || ''))
  })

  if (!jadwalList.length) {
    ;(ctx?.jamList || []).forEach(item => {
      targetSelects.forEach(selectEl => {
        const opt = document.createElement('option')
        opt.value = String(item.id)
        opt.textContent = `${item.nama || 'Jam'} (${toTimeLabel(item.jam_mulai)}-${toTimeLabel(item.jam_selesai)})`
        selectEl.appendChild(opt)
      })
    })
    if (jamSelect1) jamSelect1.value = ''
    if (jamSelect2) jamSelect2.value = ''
    return
  }

  const matchedJamValues = []
  jadwalList.forEach(item => {
    const key = `${toTimeLabel(item.jam_mulai)}|${toTimeLabel(item.jam_selesai)}`
    const jam = jamByRange.get(key)
    const optValue = jam?.id ? String(jam.id) : ''
    const jamNama = jam?.nama || `${toTimeLabel(item.jam_mulai)}-${toTimeLabel(item.jam_selesai)}`
    if (optValue && !matchedJamValues.includes(optValue)) {
      matchedJamValues.push(optValue)
    }
    targetSelects.forEach(selectEl => {
      const opt = document.createElement('option')
      opt.value = optValue
      opt.textContent = `${getHariLabel(item.hari)} - ${jamNama}`
      selectEl.appendChild(opt)
    })
  })

  const jam1Values = jamSelect1 ? Array.from(jamSelect1.options || []).map(opt => String(opt.value || '')) : []
  const jam2Values = jamSelect2 ? Array.from(jamSelect2.options || []).map(opt => String(opt.value || '')) : []

  if (!matchedJamValues.length) {
    if (jamSelect1) jamSelect1.value = ''
    if (jamSelect2) jamSelect2.value = ''
    return
  }

  if (jamSelect1) {
    if (selected1 && matchedJamValues.includes(selected1) && jam1Values.includes(selected1)) {
      jamSelect1.value = selected1
    } else if (matchedJamValues[0] && jam1Values.includes(matchedJamValues[0])) {
      jamSelect1.value = matchedJamValues[0]
    } else {
      jamSelect1.value = ''
    }
  }
  if (jamSelect2) {
    const jam1Value = String(jamSelect1?.value || '')
    if (selected2 && selected2 !== jam1Value && matchedJamValues.includes(selected2) && jam2Values.includes(selected2)) {
      jamSelect2.value = selected2
    } else {
      const jam2Default = matchedJamValues.find(value => value && value !== jam1Value) || ''
      if (jam2Default && jam2Values.includes(jam2Default)) {
        jamSelect2.value = jam2Default
      } else {
        jamSelect2.value = ''
      }
    }
  }
}

async function handleAbsensiKelasMapelChange() {
  renderAbsensiJamOptions()

  const kelasId = String(document.getElementById('absensi-kelas')?.value || '')
  currentAbsensiSantriList = await getSantriByKelas(kelasId)
  renderAbsensiSantriRows()
}

async function renderAbsensiPage() {
  const content = document.getElementById('guru-content')
  if (!content) return

  content.innerHTML = 'Loading absensi...'

  let ctx
  try {
    ctx = await getGuruContext()
  } catch (error) {
    console.error(error)
    content.innerHTML = `<div class="placeholder-card">Gagal load data absensi: ${escapeHtml(error.message || 'Unknown error')}</div>`
    return
  }

  if (!ctx.guru?.id) {
    content.innerHTML = '<div class="placeholder-card">Data guru tidak ditemukan.</div>'
    return
  }

  if (!ctx.activeDistribusiList.length) {
    content.innerHTML = '<div class="placeholder-card">Distribusi mapel semester aktif belum tersedia untuk guru ini.</div>'
    return
  }

  const kelasIds = [...new Set(ctx.activeDistribusiList.map(item => String(item.kelas_id || '')).filter(Boolean))]
  const kelasOptions = kelasIds
    .map(id => ({ id, nama: ctx.kelasMap.get(id)?.nama_kelas || '-' }))
    .sort((a, b) => a.nama.localeCompare(b.nama))
  let penggantiList = []
  try {
    const { data, error } = await sb
      .from('karyawan')
      .select('id, nama, aktif')
      .eq('aktif', true)
      .order('nama')
    if (error) {
      console.error(error)
    } else {
      penggantiList = data || []
    }
  } catch (error) {
    console.error(error)
    penggantiList = []
  }

  const today = new Date().toISOString().slice(0, 10)

  content.innerHTML = `
    <div>
      <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap:10px; align-items:end;">
        <div>
          <label class="guru-label">Tanggal</label>
          <input id="absensi-tanggal" class="guru-field" type="date" value="${today}" onchange="onAbsensiTanggalChange()">
        </div>
        <div>
          <label class="guru-label">Kelas</label>
          <select id="absensi-kelas" class="guru-field" onchange="onAbsensiKelasChange()">
            <option value="">-- Pilih Kelas --</option>
            ${kelasOptions.map(item => `<option value="${escapeHtml(item.id)}">${escapeHtml(item.nama)}</option>`).join('')}
          </select>
        </div>
        <div>
          <label class="guru-label">Mapel</label>
          <select id="absensi-mapel" class="guru-field" onchange="onAbsensiMapelChange()">
            <option value="">-- Pilih Mapel --</option>
          </select>
        </div>
        <div>
          <label class="guru-label">Jam Pelajaran 1</label>
          <select id="absensi-jam-1" class="guru-field">
            <option value="">-- Pilih Jam --</option>
          </select>
        </div>
        <div>
          <label class="guru-label">Jam Pelajaran 2 (Opsional)</label>
          <select id="absensi-jam-2" class="guru-field">
            <option value="">-- Pilih Jam --</option>
          </select>
        </div>
      </div>

      <div style="margin-top:10px; display:grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap:10px; align-items:end;">
        <div style="display:flex; align-items:center; gap:8px;">
          <input id="absensi-pakai-pengganti" type="checkbox" onchange="onAbsensiPenggantiToggle()">
          <label for="absensi-pakai-pengganti" class="guru-label" style="margin:0;">Gunakan Guru Pengganti</label>
        </div>
        <div>
          <label class="guru-label">Pengganti (Karyawan)</label>
          <select id="absensi-guru-pengganti" class="guru-field" disabled>
            <option value="">-- Pilih Pengganti --</option>
            ${penggantiList.map(item => `<option value="${escapeHtml(String(item.id || ''))}">${escapeHtml(String(item.nama || '-'))}</option>`).join('')}
          </select>
        </div>
        <div>
          <label class="guru-label">Keterangan Pengganti (Opsional)</label>
          <input id="absensi-keterangan-pengganti" class="guru-field" type="text" placeholder="Contoh: Guru utama izin" disabled>
        </div>
      </div>

      <div id="absensi-santri-list" style="margin-top:12px;"></div>

      <div style="margin-top:14px;">
        <button id="btn-save-absensi" type="button" class="modal-btn modal-btn-primary" onclick="saveGuruAbsensi()">Simpan Absensi</button>
      </div>
    </div>
  `

  currentAbsensiSantriList = []
}

async function onAbsensiKelasChange() {
  renderAbsensiMapelOptions()
  renderAbsensiJamOptions()

  const kelasId = String(document.getElementById('absensi-kelas')?.value || '')
  currentAbsensiSantriList = await getSantriByKelas(kelasId)
  renderAbsensiSantriRows()
}

async function onAbsensiMapelChange() {
  await handleAbsensiKelasMapelChange()
}

async function onAbsensiTanggalChange() {
  const mapelSelect = document.getElementById('absensi-mapel')
  const prevMapel = String(mapelSelect?.value || '')
  renderAbsensiMapelOptions()
  if (mapelSelect && prevMapel) {
    const stillExists = Array.from(mapelSelect.options || []).some(opt => String(opt.value || '') === prevMapel)
    if (!stillExists) mapelSelect.value = ''
  }
  renderAbsensiJamOptions()
}

function onAbsensiPenggantiToggle() {
  const checked = document.getElementById('absensi-pakai-pengganti')?.checked === true
  const selectEl = document.getElementById('absensi-guru-pengganti')
  const noteEl = document.getElementById('absensi-keterangan-pengganti')
  if (selectEl) {
    selectEl.disabled = !checked
    if (!checked) selectEl.value = ''
  }
  if (noteEl) {
    noteEl.disabled = !checked
    if (!checked) noteEl.value = ''
  }
}

async function saveGuruAbsensi() {
  const saveBtn = document.getElementById('btn-save-absensi')
  if (saveBtn?.disabled) return
  setButtonLoading(saveBtn, true, 'Menyimpan...')
  try {
  const tanggal = String(document.getElementById('absensi-tanggal')?.value || '').trim()
  const kelasId = String(document.getElementById('absensi-kelas')?.value || '').trim()
  const mapelId = String(document.getElementById('absensi-mapel')?.value || '').trim()
  const jamId1 = String(document.getElementById('absensi-jam-1')?.value || '').trim()
  const jamId2 = String(document.getElementById('absensi-jam-2')?.value || '').trim()
  const pakaiPengganti = document.getElementById('absensi-pakai-pengganti')?.checked === true
  const penggantiId = String(document.getElementById('absensi-guru-pengganti')?.value || '').trim()
  const keteranganPengganti = String(document.getElementById('absensi-keterangan-pengganti')?.value || '').trim()

  if (!tanggal || !kelasId || !mapelId) {
    alert('Tanggal, kelas, dan mapel wajib diisi.')
    return
  }
  if (pakaiPengganti && !penggantiId) {
    alert('Pilih karyawan pengganti terlebih dahulu.')
    return
  }
  if (jamId1 && jamId2 && jamId1 === jamId2) {
    alert('Jam Pelajaran 1 dan Jam Pelajaran 2 tidak boleh sama.')
    return
  }

  if (!currentAbsensiSantriList.length) {
    alert('Daftar siswa masih kosong.')
    return
  }

  const ctx = await getGuruContext()
  const guruId = ctx?.guru?.id
  if (!guruId) {
    alert('Data guru tidak ditemukan.')
    return
  }

  const distribusi = getAbsensiDistribusiCandidates()[0] || null
  const jamIds = [...new Set([jamId1, jamId2].filter(Boolean))]
  const jamIdsToSave = jamIds.length ? jamIds : [null]

  const statusMap = new Map()
  document.querySelectorAll('[data-absen-santri-id]').forEach(selectEl => {
    const santriId = String(selectEl.getAttribute('data-absen-santri-id') || '').trim()
    const status = String(selectEl.value || '').trim() || 'Hadir'
    if (!santriId) return
    statusMap.set(santriId, status)
  })

  const payloads = currentAbsensiSantriList.flatMap(santri => (
    jamIdsToSave.map(jamId => ({
      tanggal,
      kelas_id: kelasId,
      mapel_id: mapelId,
      guru_id: String(guruId),
      jam_pelajaran_id: jamId || null,
      semester_id: distribusi?.semester_id ? String(distribusi.semester_id) : (ctx.activeSemester?.id ? String(ctx.activeSemester.id) : null),
      distribusi_id: distribusi?.id ? String(distribusi.id) : null,
      santri_id: String(santri.id),
      status: statusMap.get(String(santri.id)) || 'Hadir',
      guru_pengganti_id: pakaiPengganti ? penggantiId : null,
      keterangan_pengganti: pakaiPengganti ? (keteranganPengganti || null) : null
    }))
  ))

  let saveError = null
  let saveRes = await sb
    .from(ATTENDANCE_TABLE)
    .upsert(payloads, {
      onConflict: 'tanggal,kelas_id,mapel_id,jam_pelajaran_id,santri_id'
    })

  if (saveRes.error) {
    const conflictMsg = String(saveRes.error.message || '').toLowerCase()
    const noUniqueForConflict = conflictMsg.includes('no unique or exclusion constraint matching the on conflict specification')
    if (noUniqueForConflict) {
      if (jamIds.length > 1) {
        alert(
          "Database absensi masih memakai unique lama (tanpa jam_pelajaran_id), jadi 2 jam tidak bisa disimpan sekaligus.\n\n" +
          "Silakan update unique constraint agar mencakup jam_pelajaran_id, contoh:\n" +
          "drop index if exists ux_absensi_sesi_siswa;\n" +
          "create unique index if not exists ux_absensi_sesi_siswa on public.absensi_santri (tanggal, kelas_id, mapel_id, jam_pelajaran_id, santri_id);"
        )
        return
      }
      saveRes = await sb
        .from(ATTENDANCE_TABLE)
        .upsert(payloads, {
          onConflict: 'tanggal,kelas_id,mapel_id,santri_id'
        })
    }
  }

  saveError = saveRes.error

  if (saveError) {
    console.error(saveError)
    const msg = String(saveError.message || '')
    if (isMissingAbsensiTableError(saveError)) {
      alert(buildAbsensiMissingTableMessage())
      return
    }
    if (isMissingAbsensiPenggantiColumnError(saveError)) {
      alert(buildAbsensiPenggantiColumnsMessage())
      return
    }
    if (jamIds.length > 1 && msg.toLowerCase().includes('cannot affect row a second time')) {
      alert(
        "Database absensi masih memakai unique lama (tanpa jam_pelajaran_id), jadi 2 jam tidak bisa disimpan sekaligus.\n\n" +
        "Silakan update unique constraint agar mencakup jam_pelajaran_id, contoh:\n" +
        "drop index if exists ux_absensi_sesi_siswa;\n" +
        "create unique index if not exists ux_absensi_sesi_siswa on public.absensi_santri (tanggal, kelas_id, mapel_id, jam_pelajaran_id, santri_id);"
      )
      return
    }
    if (jamIds.length > 1 && msg.toLowerCase().includes('duplicate key value')) {
      alert('Gagal menyimpan 2 jam sekaligus karena constraint database masih membatasi 1 sesi per mapel/hari. Mohon update unique constraint absensi agar memasukkan jam_pelajaran_id.')
      return
    }
    alert(`Gagal menyimpan absensi: ${msg || 'Unknown error'}`)
    return
  }

  if (distribusi) {
    try {
      await recalculateNilaiKehadiranFromAbsensi(
        distribusi,
        currentAbsensiSantriList.map(item => String(item.id))
      )
    } catch (calcErr) {
      console.error(calcErr)
      alert(`Absensi tersimpan, tapi gagal update nilai kehadiran: ${calcErr.message || 'Unknown error'}`)
      return
    }
  }

  alert('Absensi berhasil disimpan.')
  document.querySelectorAll('[data-absen-santri-id]').forEach(selectEl => {
    selectEl.value = 'Hadir'
  })
  clearGuruPageCache('input-absensi')
  clearGuruPageCache('laporan-absensi')
  } finally {
    setButtonLoading(saveBtn, false)
  }
}

function renderInputNilaiMapelOptions() {
  const mapelSelect = document.getElementById('input-nilai-mapel')
  if (!mapelSelect) return

  const kelasId = String(document.getElementById('input-nilai-kelas')?.value || '')
  const ctx = guruContextCache
  const list = (ctx?.activeDistribusiList || []).filter(item => String(item.kelas_id || '') === kelasId)
  const uniqueMapelIds = [...new Set(list.map(item => String(item.mapel_id || '')).filter(Boolean))]

  mapelSelect.innerHTML = '<option value="">-- Pilih Mapel --</option>'
  uniqueMapelIds.forEach(mapelId => {
    const mapel = ctx?.mapelMap.get(mapelId)
    const opt = document.createElement('option')
    opt.value = mapelId
    opt.textContent = getMapelLabel(mapel)
    mapelSelect.appendChild(opt)
  })
}

function renderInputNilaiSantriRows() {
  const container = document.getElementById('input-nilai-santri-list')
  if (!container) return
  const jenis = String(document.getElementById('input-nilai-jenis')?.value || '').trim()
  const maxValue = getJenisNilaiMax(jenis)

  if (!currentInputNilaiSantriList.length) {
    container.innerHTML = '<div class="placeholder-card">Belum ada data siswa untuk kelas ini.</div>'
    return
  }

  let html = `
    <div style="overflow:auto; border:1px solid #e2e8f0; border-radius:10px; margin-top:10px;">
      <table style="width:100%; min-width:520px; border-collapse:collapse; font-size:13px;">
        <thead>
          <tr style="background:#f8fafc;">
            <th style="padding:10px; border:1px solid #e2e8f0; width:70px; text-align:center;">No</th>
            <th style="padding:10px; border:1px solid #e2e8f0; text-align:left;">Nama Siswa</th>
            <th style="padding:10px; border:1px solid #e2e8f0; width:170px; text-align:center;">Nilai</th>
          </tr>
        </thead>
        <tbody>
  `

  html += currentInputNilaiSantriList.map((santri, index) => `
    <tr>
      <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${index + 1}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(santri.nama || '-')}</td>
      <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">
        <input type="number" step="1" min="0" ${maxValue !== null ? `max="${maxValue}"` : ''} class="guru-field" style="max-width:120px; text-align:center;" data-input-nilai-santri-id="${escapeHtml(santri.id)}" placeholder="0">
      </td>
    </tr>
  `).join('')

  html += '</tbody></table></div>'
  container.innerHTML = html
}

async function onInputNilaiKelasChange() {
  renderInputNilaiMapelOptions()
  const mapelEl = document.getElementById('input-nilai-mapel')
  if (mapelEl) mapelEl.value = ''

  const kelasId = String(document.getElementById('input-nilai-kelas')?.value || '')
  currentInputNilaiSantriList = await getSantriByKelas(kelasId)
  renderInputNilaiSantriRows()
}

async function onInputNilaiMapelChange() {
  const kelasId = String(document.getElementById('input-nilai-kelas')?.value || '')
  currentInputNilaiSantriList = await getSantriByKelas(kelasId)
  renderInputNilaiSantriRows()
}

function onInputNilaiJenisChange() {
  renderInputNilaiSantriRows()
}

async function recalculateNilaiAkademikFromInput(distribusi, santriIdList) {
  if (!distribusi || !santriIdList.length) return

  const [inputRes, absensiRes] = await Promise.all([
    sb
      .from(INPUT_NILAI_TABLE)
      .select('santri_id, jenis, nilai')
      .eq('kelas_id', distribusi.kelas_id)
      .eq('mapel_id', distribusi.mapel_id)
      .eq('semester_id', distribusi.semester_id)
      .in('santri_id', santriIdList),
    sb
      .from(ATTENDANCE_TABLE)
      .select('santri_id, status')
      .eq('kelas_id', distribusi.kelas_id)
      .eq('mapel_id', distribusi.mapel_id)
      .eq('semester_id', distribusi.semester_id)
      .in('santri_id', santriIdList)
  ])

  if (inputRes.error) throw inputRes.error
  if (absensiRes.error) throw absensiRes.error

  const inputRows = inputRes.data || []
  const absensiRows = absensiRes.data || []

  const aggregate = new Map()
  inputRows.forEach(row => {
    const sid = String(row.santri_id || '')
    const key = toJenisNilaiKey(row.jenis)
    if (!sid || !key) return
    if (!aggregate.has(sid)) aggregate.set(sid, {})
    const entry = aggregate.get(sid)
    if (!entry[key]) entry[key] = { total: 0, count: 0 }
    const nilai = Number(row.nilai)
    if (!Number.isFinite(nilai)) return
    entry[key].total += nilai
    entry[key].count += 1
  })

  const absensiBySantri = new Map()
  absensiRows.forEach(row => {
    const sid = String(row.santri_id || '')
    if (!sid) return
    if (!absensiBySantri.has(sid)) absensiBySantri.set(sid, [])
    absensiBySantri.get(sid).push(row)
  })

  const { data: existingRows, error: existingErr } = await sb
    .from('nilai_akademik')
    .select('*')
    .eq('mapel_id', distribusi.mapel_id)
    .eq('semester_id', distribusi.semester_id)
    .in('santri_id', santriIdList)

  if (existingErr) throw existingErr
  const existingMap = new Map((existingRows || []).map(item => [String(item.santri_id), item]))

  for (const sid of santriIdList) {
    const agg = aggregate.get(String(sid)) || {}
    const sidText = String(sid)
    const existing = existingMap.get(sidText) || {}
    const nilai_tugas = agg.nilai_tugas?.count ? round2(agg.nilai_tugas.total / agg.nilai_tugas.count) : null
    const nilai_ulangan_harian = agg.nilai_ulangan_harian?.count ? round2(agg.nilai_ulangan_harian.total / agg.nilai_ulangan_harian.count) : null
    const nilai_pts = agg.nilai_pts?.count ? round2(agg.nilai_pts.total / agg.nilai_pts.count) : null
    const nilai_pas = agg.nilai_pas?.count ? round2(agg.nilai_pas.total / agg.nilai_pas.count) : null
    const nilai_keterampilan = agg.nilai_keterampilan?.count ? round2(agg.nilai_keterampilan.total / agg.nilai_keterampilan.count) : null
    const nilai_kehadiran = calculateNilaiKehadiranFromRows(absensiBySantri.get(sidText) || [])
    const nilai_akhir = round2(
      Number(nilai_tugas || 0) +
      Number(nilai_ulangan_harian || 0) +
      Number(nilai_pts || 0) +
      Number(nilai_pas || 0) +
      Number(nilai_kehadiran || 0)
    )

    const payload = {
      santri_id: String(sid),
      mapel_id: String(distribusi.mapel_id),
      semester_id: distribusi.semester_id ? String(distribusi.semester_id) : null,
      nilai_tugas,
      nilai_ulangan_harian,
      nilai_pts,
      nilai_pas,
      nilai_kehadiran,
      nilai_akhir,
      nilai_keterampilan
    }

    if (existing.id) {
      const { error } = await sb.from('nilai_akademik').update(payload).eq('id', existing.id)
      if (error) throw error
    } else {
      const { error } = await sb.from('nilai_akademik').insert(payload)
      if (error) throw error
    }
  }
}

async function recalculateNilaiKehadiranFromAbsensi(distribusi, santriIdList) {
  if (!distribusi || !santriIdList.length) return

  const [absensiRes, existingRes] = await Promise.all([
    sb
      .from(ATTENDANCE_TABLE)
      .select('santri_id, status')
      .eq('kelas_id', distribusi.kelas_id)
      .eq('mapel_id', distribusi.mapel_id)
      .eq('semester_id', distribusi.semester_id)
      .in('santri_id', santriIdList),
    sb
      .from('nilai_akademik')
      .select('*')
      .eq('mapel_id', distribusi.mapel_id)
      .eq('semester_id', distribusi.semester_id)
      .in('santri_id', santriIdList)
  ])

  if (absensiRes.error) throw absensiRes.error
  if (existingRes.error) throw existingRes.error

  const absensiBySantri = new Map()
  ;(absensiRes.data || []).forEach(row => {
    const sid = String(row.santri_id || '')
    if (!sid) return
    if (!absensiBySantri.has(sid)) absensiBySantri.set(sid, [])
    absensiBySantri.get(sid).push(row)
  })

  const existingMap = new Map((existingRes.data || []).map(item => [String(item.santri_id), item]))

  for (const sidRaw of santriIdList) {
    const sid = String(sidRaw)
    const existing = existingMap.get(sid) || {}
    const nilai_kehadiran = calculateNilaiKehadiranFromRows(absensiBySantri.get(sid) || [])
    const nilai_tugas = existing.nilai_tugas ?? null
    const nilai_ulangan_harian = existing.nilai_ulangan_harian ?? null
    const nilai_pts = existing.nilai_pts ?? null
    const nilai_pas = existing.nilai_pas ?? null
    const nilai_keterampilan = existing.nilai_keterampilan ?? null
    const nilai_akhir = round2(
      Number(nilai_tugas || 0) +
      Number(nilai_ulangan_harian || 0) +
      Number(nilai_pts || 0) +
      Number(nilai_pas || 0) +
      Number(nilai_kehadiran || 0)
    )

    const payload = {
      santri_id: sid,
      mapel_id: String(distribusi.mapel_id),
      semester_id: distribusi.semester_id ? String(distribusi.semester_id) : null,
      nilai_tugas,
      nilai_ulangan_harian,
      nilai_pts,
      nilai_pas,
      nilai_kehadiran,
      nilai_akhir,
      nilai_keterampilan
    }

    if (existing.id) {
      const { error } = await sb.from('nilai_akademik').update(payload).eq('id', existing.id)
      if (error) throw error
    } else {
      const { error } = await sb.from('nilai_akademik').insert(payload)
      if (error) throw error
    }
  }
}

async function saveInputNilaiBatch() {
  const saveBtn = document.getElementById('btn-save-input-nilai')
  if (saveBtn?.disabled) return
  setButtonLoading(saveBtn, true, 'Menyimpan...')
  try {
  const tanggal = String(document.getElementById('input-nilai-tanggal')?.value || '').trim()
  const kelasId = String(document.getElementById('input-nilai-kelas')?.value || '').trim()
  const mapelId = String(document.getElementById('input-nilai-mapel')?.value || '').trim()
  const jenis = String(document.getElementById('input-nilai-jenis')?.value || '').trim()
  const maxJenis = getJenisNilaiMax(jenis)

  if (!tanggal || !kelasId || !mapelId || !jenis) {
    alert('Tanggal, kelas, mapel, dan jenis nilai wajib diisi.')
    return
  }

  if (!currentInputNilaiSantriList.length) {
    alert('Data siswa belum tersedia.')
    return
  }

  const ctx = await getGuruContext()
  const distribusi = (ctx.activeDistribusiList || []).find(item => {
    return String(item.kelas_id) === kelasId && String(item.mapel_id) === mapelId
  }) || null

  if (!distribusi) {
    alert('Distribusi mapel semester aktif untuk kelas dan mapel ini belum tersedia.')
    return
  }

  const rows = []
  let hasInvalidValue = false
  document.querySelectorAll('[data-input-nilai-santri-id]').forEach(inputEl => {
    if (hasInvalidValue) return
    const sid = String(inputEl.getAttribute('data-input-nilai-santri-id') || '').trim()
    const nilai = toNullableNumber(inputEl.value || '')
    if (!sid || nilai === null) return
    if (Number.isNaN(nilai)) return
    if (!validateRange(nilai, `Nilai ${jenis}`, maxJenis)) {
      hasInvalidValue = true
      return
    }
    rows.push({
      tanggal,
      kelas_id: kelasId,
      mapel_id: mapelId,
      guru_id: String(ctx.guru?.id || ''),
      semester_id: distribusi.semester_id ? String(distribusi.semester_id) : null,
      distribusi_id: String(distribusi.id),
      santri_id: sid,
      jenis,
      nilai: round2(nilai)
    })
  })

  if (hasInvalidValue) return

  if (!rows.length) {
    alert('Isi minimal satu nilai siswa.')
    return
  }

  const { error } = await sb
    .from(INPUT_NILAI_TABLE)
    .upsert(rows, {
      onConflict: 'tanggal,kelas_id,mapel_id,semester_id,santri_id,jenis,guru_id'
    })
  if (error) {
    console.error(error)
    if (isMissingInputNilaiTableError(error)) {
      alert(buildInputNilaiMissingTableMessage())
      return
    }
    alert(`Gagal simpan input nilai: ${error.message || 'Unknown error'}`)
    return
  }

  try {
    await recalculateNilaiAkademikFromInput(distribusi, rows.map(item => item.santri_id))
  } catch (error) {
    console.error(error)
    alert(`Input nilai tersimpan, tapi gagal hitung rata-rata: ${error.message || 'Unknown error'}`)
    return
  }

  alert('Input nilai berhasil disimpan dan rata-rata diperbarui.')
  clearGuruPageCache('input-nilai')
  clearGuruPageCache('mapel')
  clearGuruPageCache('rapor')
  document.querySelectorAll('[data-input-nilai-santri-id]').forEach(inputEl => {
    inputEl.value = ''
  })
  } finally {
    setButtonLoading(saveBtn, false)
  }
}

async function renderInputNilaiPage() {
  const content = document.getElementById('guru-content')
  if (!content) return

  content.innerHTML = 'Loading input nilai...'

  let ctx
  try {
    ctx = await getGuruContext()
  } catch (error) {
    console.error(error)
    content.innerHTML = `<div class="placeholder-card">Gagal load input nilai: ${escapeHtml(error.message || 'Unknown error')}</div>`
    return
  }

  if (!ctx.guru?.id) {
    content.innerHTML = '<div class="placeholder-card">Data guru tidak ditemukan.</div>'
    return
  }

  if (!ctx.activeDistribusiList.length) {
    content.innerHTML = '<div class="placeholder-card">Distribusi mapel semester aktif belum tersedia untuk guru ini.</div>'
    return
  }

  const kelasIds = [...new Set(ctx.activeDistribusiList.map(item => String(item.kelas_id || '')).filter(Boolean))]
  const kelasOptions = kelasIds
    .map(id => ({ id, nama: ctx.kelasMap.get(id)?.nama_kelas || '-' }))
    .sort((a, b) => a.nama.localeCompare(b.nama))

  const today = new Date().toISOString().slice(0, 10)
  content.innerHTML = `
    <div>
      <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap:10px; align-items:end;">
        <div>
          <label class="guru-label">Tanggal</label>
          <input id="input-nilai-tanggal" class="guru-field" type="date" value="${today}">
        </div>
        <div>
          <label class="guru-label">Kelas</label>
          <select id="input-nilai-kelas" class="guru-field" onchange="onInputNilaiKelasChange()">
            <option value="">-- Pilih Kelas --</option>
            ${kelasOptions.map(item => `<option value="${escapeHtml(item.id)}">${escapeHtml(item.nama)}</option>`).join('')}
          </select>
        </div>
        <div>
          <label class="guru-label">Mapel</label>
          <select id="input-nilai-mapel" class="guru-field" onchange="onInputNilaiMapelChange()">
            <option value="">-- Pilih Mapel --</option>
          </select>
        </div>
        <div>
          <label class="guru-label">Jenis Nilai</label>
          <select id="input-nilai-jenis" class="guru-field" onchange="onInputNilaiJenisChange()">
            ${INPUT_NILAI_JENIS_LIST.map(item => `<option value="${escapeHtml(item)}">${escapeHtml(item)}</option>`).join('')}
          </select>
        </div>
      </div>

      <div id="input-nilai-santri-list" style="margin-top:12px;"></div>
      <div style="margin-top:14px;">
        <button id="btn-save-input-nilai" type="button" class="modal-btn modal-btn-primary" onclick="saveInputNilaiBatch()">Simpan Input Nilai</button>
      </div>
    </div>
  `
  currentInputNilaiSantriList = []
}
async function renderGuruProfil() {
  const content = document.getElementById('guru-content')
  if (!content) return

  content.innerHTML = 'Loading profil...'

  let guru
  try {
    guru = await getCurrentGuruRow()
  } catch (error) {
    console.error(error)
    content.innerHTML = `<div class="placeholder-card">Gagal load profil: ${escapeHtml(error.message || 'Unknown error')}</div>`
    return
  }

  if (!guru?.id) {
    content.innerHTML = '<div class="placeholder-card">Data profil guru tidak ditemukan.</div>'
    return
  }

  content.innerHTML = `
    <div style="max-width:580px;">
      <div style="margin-bottom:10px;">
        <label class="guru-label">ID Karyawan</label>
        <input id="guru-profil-id-karyawan" type="text" value="${escapeHtml(guru.id_karyawan || '')}" disabled class="guru-field" autocomplete="off" style="background:#f8fafc; color:#64748b;">
      </div>
      <div style="margin-bottom:10px;">
        <label class="guru-label">Nama</label>
        <input id="guru-profil-nama" type="text" value="${escapeHtml(guru.nama || '')}" class="guru-field" autocomplete="off">
      </div>
      <div style="margin-bottom:10px;">
        <label class="guru-label">No HP</label>
        <input id="guru-profil-no-hp" type="text" value="${escapeHtml(guru.no_hp || '')}" class="guru-field" autocomplete="off">
      </div>
      <div style="margin-bottom:10px;">
        <label class="guru-label">Alamat</label>
        <input id="guru-profil-alamat" type="text" value="${escapeHtml(guru.alamat || '')}" class="guru-field" autocomplete="off">
      </div>
      <div style="margin-bottom:12px;">
        <label class="guru-label">Password Baru (opsional)</label>
        <div style="display:flex; gap:8px; align-items:center;">
          <input id="guru-profil-password" type="password" placeholder="Isi jika ingin ganti password" class="guru-field" autocomplete="new-password" style="flex:1;">
          <button id="guru-profil-password-toggle" type="button" class="modal-btn" onclick="toggleGuruProfilePassword()">Lihat</button>
        </div>
      </div>
      <button type="button" class="modal-btn modal-btn-primary" onclick="saveGuruProfil('${escapeHtml(guru.id)}')">Simpan Profil</button>
    </div>
  `
}

function toggleGuruProfilePassword() {
  const input = document.getElementById('guru-profil-password')
  const btn = document.getElementById('guru-profil-password-toggle')
  if (!input || !btn) return
  const isHidden = input.type === 'password'
  input.type = isHidden ? 'text' : 'password'
  btn.textContent = isHidden ? 'Sembunyikan' : 'Lihat'
}

async function saveGuruProfil(guruId) {
  const nama = String(document.getElementById('guru-profil-nama')?.value || '').trim()
  const no_hp = String(document.getElementById('guru-profil-no-hp')?.value || '').trim()
  const alamat = String(document.getElementById('guru-profil-alamat')?.value || '').trim()
  const password = String(document.getElementById('guru-profil-password')?.value || '').trim()

  if (!nama) {
    alert('Nama wajib diisi.')
    return
  }

  const payload = {
    nama,
    no_hp: no_hp || null,
    alamat: alamat || null
  }

  if (password) payload.password = password

  const { error } = await sb
    .from('karyawan')
    .update(payload)
    .eq('id', guruId)

  if (error) {
    console.error(error)
    alert(`Gagal menyimpan profil: ${error.message || 'Unknown error'}`)
    return
  }

  alert('Profil berhasil disimpan.')
  guruContextCache = null
  currentGuruRowCache = null
  clearGuruPageCache()
  await setGuruWelcomeName()
  await renderGuruProfil()
}

async function renderLaporanBulananPage(forceReload = false) {
  const content = document.getElementById('guru-content')
  if (!content) return

  content.innerHTML = 'Loading laporan bulanan...'

  let ctx
  try {
    ctx = await getGuruContext(forceReload)
  } catch (error) {
    console.error(error)
    content.innerHTML = `<div class="placeholder-card">Gagal load laporan bulanan: ${escapeHtml(error.message || 'Unknown error')}</div>`
    return
  }

  const guru = ctx.guru
  if (!guru?.id) {
    content.innerHTML = '<div class="placeholder-card">Data guru tidak ditemukan.</div>'
    return
  }

  const tahunAktif = await getActiveTahunAjaran()
  const tahunAjaranId = tahunAktif?.id || null

  let kelasQuery = sb
    .from('kelas')
    .select('id, nama_kelas, wali_kelas_id, tahun_ajaran_id')
    .eq('wali_kelas_id', guru.id)
    .order('nama_kelas')

  if (tahunAjaranId) kelasQuery = kelasQuery.eq('tahun_ajaran_id', tahunAjaranId)
  const kelasRes = await kelasQuery

  if (kelasRes.error) {
    console.error(kelasRes.error)
    content.innerHTML = `<div class="placeholder-card">Gagal load data kelas wali: ${escapeHtml(kelasRes.error.message || 'Unknown error')}</div>`
    return
  }

  const kelasList = kelasRes.data || []
  if (!kelasList.length) {
    content.innerHTML = '<div class="placeholder-card">Anda belum terdaftar sebagai wali kelas.</div>'
    return
  }

  const kelasMap = new Map(kelasList.map(item => [String(item.id), item]))
  const kelasIds = [...kelasMap.keys()]

  const { data: santriData, error: santriError } = await sb
    .from('santri')
    .select('*')
    .in('kelas_id', kelasIds)
    .eq('aktif', true)
    .order('nama')

  if (santriError) {
    console.error(santriError)
    content.innerHTML = `<div class="placeholder-card">Gagal load data santri: ${escapeHtml(santriError.message || 'Unknown error')}</div>`
    return
  }

  const santriList = (santriData || []).sort((a, b) => {
    const kelasA = kelasMap.get(String(a.kelas_id || ''))?.nama_kelas || ''
    const kelasB = kelasMap.get(String(b.kelas_id || ''))?.nama_kelas || ''
    const kelasCmp = kelasA.localeCompare(kelasB)
    if (kelasCmp !== 0) return kelasCmp
    return String(a.nama || '').localeCompare(String(b.nama || ''))
  })

  const previousState = { ...laporanBulananState }
  const periode = previousState.periode || getMonthInputToday()
  laporanBulananState = {
    ...previousState,
    periode,
    guru,
    kelasList,
    kelasMap,
    santriList,
    selectedSantriId: ''
  }

  if (!santriList.length) {
    content.innerHTML = `
      <div style="display:flex; gap:10px; align-items:end; margin-bottom:12px;">
        <div>
          <label class="guru-label">Periode</label>
          <input id="laporan-bulanan-periode" class="guru-field" type="month" value="${escapeHtml(periode)}" onchange="onLaporanBulananPeriodChange(this.value)">
        </div>
      </div>
      <div class="placeholder-card">Belum ada santri aktif pada kelas yang Anda naungi.</div>
    `
    return
  }

  if (!laporanBulananState.waTemplate) {
    laporanBulananState.waTemplate = loadMonthlyWaTemplate(guru.id)
  }
  const isTemplateEditing = laporanBulananState.waTemplateEditing === true

  const santriIds = santriList.map(item => String(item.id))
  const monthlyReportMap = new Map()
  const reportRes = await sb
    .from(MONTHLY_REPORT_TABLE)
    .select('santri_id, nilai_akhlak, predikat, catatan_wali')
    .eq('periode', periode)
    .eq('guru_id', String(guru.id))
    .in('santri_id', santriIds)

  if (reportRes.error) {
    if (!isMissingMonthlyReportTableError(reportRes.error)) {
      console.error(reportRes.error)
    }
  } else {
    ;(reportRes.data || []).forEach(row => {
      monthlyReportMap.set(String(row.santri_id), row)
    })
  }

  const rowsHtml = santriList.map((item, index) => {
    const kelasNama = kelasMap.get(String(item.kelas_id || ''))?.nama_kelas || '-'
    const reportRow = monthlyReportMap.get(String(item.id))
    const gradeAkhlak = reportRow?.nilai_akhlak === null || reportRow?.nilai_akhlak === undefined
      ? ''
      : normalizeAkhlakGrade(reportRow.nilai_akhlak)
    const keteranganAkhlak = gradeAkhlak ? getAkhlakKeteranganByGrade(gradeAkhlak) : ''
    const missing = []
    if (!gradeAkhlak) missing.push('Nilai akhlak')
    if (!keteranganAkhlak) missing.push('Keterangan')
    if (!reportRow || String(reportRow.catatan_wali || '').trim() === '') missing.push('Catatan wali kelas')
    const isLengkap = missing.length === 0
    const keterangan = isLengkap ? 'Lengkap' : `Belum lengkap: ${missing.join(', ')}`
    const keteranganStyle = isLengkap ? 'color:#166534; font-weight:600;' : 'color:#991b1b;'
    return `
      <tr>
        <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${index + 1}</td>
        <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.nama || '-')}</td>
        <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(kelasNama)}</td>
        <td style="padding:8px; border:1px solid #e2e8f0; ${keteranganStyle}">${escapeHtml(keterangan)}</td>
        <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">
          <div style="display:flex; gap:6px; justify-content:center; flex-wrap:nowrap; white-space:nowrap;">
            <button type="button" class="modal-btn modal-btn-primary" style="padding:6px 10px; font-size:12px;" onclick="openLaporanBulananDetail('${escapeHtml(String(item.id))}')">Detail</button>
            <button type="button" class="modal-btn" style="padding:6px 10px; font-size:12px;" onclick="quickPrintLaporanBulanan('${escapeHtml(String(item.id))}')">Cetak</button>
            <button type="button" class="modal-btn" style="padding:6px 10px; font-size:12px;" onclick="quickSendLaporanBulananWA('${escapeHtml(String(item.id))}')">Kirim</button>
          </div>
        </td>
      </tr>
    `
  }).join('')

  content.innerHTML = `
    <div class="placeholder-card" style="margin-bottom:12px;">
      <div style="font-weight:700; margin-bottom:8px;">Template Pesan WhatsApp Laporan Bulanan</div>
      <div style="font-size:12px; color:#475569; margin-bottom:8px;">
        Gunakan placeholder: <code>&lt;nama santri&gt;</code> dan <code>&lt;link&gt;</code>. Saat kirim, placeholder akan diganti otomatis.
      </div>
      <textarea id="laporan-bulanan-wa-template" class="guru-field" rows="8" placeholder="Tulis template pesan..." ${isTemplateEditing ? '' : 'readonly'} style="${isTemplateEditing ? '' : 'background:#f8fafc; color:#475569;'}">${escapeHtml(laporanBulananState.waTemplate || '')}</textarea>
      <div style="margin-top:8px; display:flex; gap:8px; flex-wrap:wrap;">
        <button id="btn-wa-template-edit" type="button" class="modal-btn" onclick="startMonthlyWaTemplateEdit()" ${isTemplateEditing ? 'style="display:none;"' : ''}>Edit Template</button>
        <button id="btn-wa-template-save" type="button" class="modal-btn modal-btn-primary" onclick="saveMonthlyWaTemplate()" ${isTemplateEditing ? '' : 'style="display:none;"'}>Simpan Template</button>
        <button id="btn-wa-template-cancel" type="button" class="modal-btn" onclick="cancelMonthlyWaTemplateEdit()" ${isTemplateEditing ? '' : 'style="display:none;"'}>Batal</button>
      </div>
    </div>

    <div style="display:flex; align-items:end; justify-content:space-between; gap:10px; margin-bottom:12px; flex-wrap:wrap;">
      <div>
        <label class="guru-label">Periode</label>
        <input id="laporan-bulanan-periode" class="guru-field" type="month" value="${escapeHtml(periode)}" onchange="onLaporanBulananPeriodChange(this.value)">
      </div>
      <div style="display:flex; align-items:center; gap:10px; flex-wrap:wrap;">
        <button type="button" class="modal-btn modal-btn-primary" onclick="openLaporanBulananBulkInputModal()">Input Massal</button>
        <div style="font-size:13px; color:#475569;">Klik detail untuk melihat laporan bulanan per santri.</div>
      </div>
    </div>

    <div style="overflow:auto; border:1px solid #e2e8f0; border-radius:10px;">
      <table style="width:100%; min-width:980px; border-collapse:collapse; font-size:13px;">
        <thead>
          <tr style="background:#f8fafc;">
            <th style="padding:8px; border:1px solid #e2e8f0; width:44px;">No</th>
            <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Nama</th>
            <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Kelas</th>
            <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Keterangan</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:260px;">Aksi</th>
          </tr>
        </thead>
        <tbody>
          ${rowsHtml}
        </tbody>
      </table>
    </div>
  `
}

async function renderLaporanAbsensiPage(forceReload = false) {
  const content = document.getElementById('guru-content')
  if (!content) return

  content.innerHTML = 'Loading rekap absensi...'

  let ctx
  try {
    ctx = await getGuruContext(forceReload)
  } catch (error) {
    console.error(error)
    content.innerHTML = `<div class="placeholder-card">Gagal load rekap absensi: ${escapeHtml(error.message || 'Unknown error')}</div>`
    return
  }

  const guru = ctx.guru
  if (!guru?.id) {
    content.innerHTML = '<div class="placeholder-card">Data guru tidak ditemukan.</div>'
    return
  }

  const { data: tahunAktif } = await sb
    .from('tahun_ajaran')
    .select('id')
    .eq('aktif', true)
    .order('id', { ascending: false })
    .limit(1)
  const tahunAjaranId = tahunAktif?.[0]?.id || null

  let kelasQuery = sb
    .from('kelas')
    .select('id, nama_kelas, wali_kelas_id, tahun_ajaran_id')
    .eq('wali_kelas_id', guru.id)
    .order('nama_kelas')

  if (tahunAjaranId) kelasQuery = kelasQuery.eq('tahun_ajaran_id', tahunAjaranId)
  let kelasRes = await kelasQuery

  if ((!kelasRes.data || kelasRes.data.length === 0) && tahunAjaranId) {
    kelasRes = await sb
      .from('kelas')
      .select('id, nama_kelas, wali_kelas_id, tahun_ajaran_id')
      .eq('wali_kelas_id', guru.id)
      .order('nama_kelas')
  }

  if (kelasRes.error) {
    console.error(kelasRes.error)
    content.innerHTML = `<div class="placeholder-card">Gagal load data kelas wali: ${escapeHtml(kelasRes.error.message || 'Unknown error')}</div>`
    return
  }

  const kelasList = kelasRes.data || []
  if (!kelasList.length) {
    content.innerHTML = '<div class="placeholder-card">Anda belum terdaftar sebagai wali kelas.</div>'
    return
  }

  const kelasMap = new Map(kelasList.map(item => [String(item.id), item]))
  const kelasIds = [...kelasMap.keys()]

  const { data: santriData, error: santriError } = await sb
    .from('santri')
    .select('id, nama, kelas_id, aktif')
    .in('kelas_id', kelasIds)
    .eq('aktif', true)
    .order('nama')

  if (santriError) {
    console.error(santriError)
    content.innerHTML = `<div class="placeholder-card">Gagal load data santri: ${escapeHtml(santriError.message || 'Unknown error')}</div>`
    return
  }

  const santriList = (santriData || []).sort((a, b) => {
    const kelasA = kelasMap.get(String(a.kelas_id || ''))?.nama_kelas || ''
    const kelasB = kelasMap.get(String(b.kelas_id || ''))?.nama_kelas || ''
    const kelasCmp = kelasA.localeCompare(kelasB)
    if (kelasCmp !== 0) return kelasCmp
    return String(a.nama || '').localeCompare(String(b.nama || ''))
  })

  const periode = laporanBulananState.periode || getMonthInputToday()
  const periodeRange = getPeriodeRange(periode)
  if (!periodeRange) {
    content.innerHTML = '<div class="placeholder-card">Periode tidak valid.</div>'
    return
  }

  if (!santriList.length) {
    content.innerHTML = `
      <div style="display:flex; gap:10px; align-items:end; margin-bottom:12px;">
        <div>
          <label class="guru-label">Periode</label>
          <input id="laporan-bulanan-periode" class="guru-field" type="month" value="${escapeHtml(periode)}" onchange="onLaporanBulananPeriodChange(this.value)">
        </div>
      </div>
      <div class="placeholder-card">Belum ada santri aktif pada kelas yang Anda naungi.</div>
    `
    return
  }

  laporanBulananState.guru = guru
  laporanBulananState.kelasList = kelasList
  laporanBulananState.kelasMap = kelasMap
  laporanBulananState.santriList = santriList
  laporanBulananState.periode = periode

  const santriIds = santriList.map(item => String(item.id))
  const { data: absensiRows, error: absensiError } = await sb
    .from(ATTENDANCE_TABLE)
    .select('santri_id, tanggal, status, kelas_id')
    .in('santri_id', santriIds)
    .in('kelas_id', kelasIds)
    .gte('tanggal', periodeRange.start)
    .lte('tanggal', periodeRange.end)

  if (absensiError && !isMissingAbsensiTableError(absensiError)) {
    console.error(absensiError)
    content.innerHTML = `<div class="placeholder-card">Gagal load absensi: ${escapeHtml(absensiError.message || 'Unknown error')}</div>`
    return
  }

  const bySantri = new Map()
  ;(absensiRows || []).forEach(row => {
    const sid = String(row.santri_id || '')
    if (!sid) return
    if (!bySantri.has(sid)) bySantri.set(sid, [])
    bySantri.get(sid).push(row)
  })

  laporanBulananState.absensiRows = absensiRows || []

  const rowsHtml = santriList.map((santri, index) => {
    const sid = String(santri.id || '')
    const kelasNama = kelasMap.get(String(santri.kelas_id || ''))?.nama_kelas || '-'
    const daily = aggregateAttendanceByDay(bySantri.get(sid) || [])
    const totalHari = daily.length
    const hadir = daily.filter(item => item.status === 'Hadir').length
    const sakit = daily.filter(item => item.status === 'Sakit').length
    const izin = daily.filter(item => item.status === 'Izin').length
    const alpa = daily.filter(item => item.status === 'Alpa').length
    const persen = totalHari > 0 ? round2((hadir / totalHari) * 100) : 0

    return `
      <tr>
        <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${index + 1}</td>
        <td style="padding:8px; border:1px solid #e2e8f0;">
          <button type="button" style="border:none; background:transparent; padding:0; margin:0; font-size:13px; color:#0f172a; cursor:pointer; text-align:left;" onclick="openLaporanAbsensiSantriDetail('${escapeHtml(sid)}')">${escapeHtml(santri.nama || '-')}</button>
        </td>
        <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(kelasNama)}</td>
        <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${hadir}</td>
        <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${sakit}</td>
        <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${izin}</td>
        <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${alpa}</td>
        <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${persen}%</td>
      </tr>
    `
  }).join('')

  content.innerHTML = `
    <div style="display:flex; align-items:end; justify-content:space-between; gap:10px; margin-bottom:12px; flex-wrap:wrap;">
      <div>
        <label class="guru-label">Periode</label>
        <input id="laporan-bulanan-periode" class="guru-field" type="month" value="${escapeHtml(periode)}" onchange="onLaporanBulananPeriodChange(this.value)">
      </div>
      <div style="font-size:13px; color:#475569;">Rekap dihitung per hari dari semua mapel.</div>
    </div>

    <div style="overflow:auto; border:1px solid #e2e8f0; border-radius:10px;">
      <table style="width:100%; min-width:920px; border-collapse:collapse; font-size:13px;">
        <thead>
          <tr style="background:#f8fafc;">
            <th style="padding:8px; border:1px solid #e2e8f0; width:44px;">No</th>
            <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Nama</th>
            <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Kelas</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Hadir</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Sakit</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Izin</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Alpa</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">% Kehadiran</th>
          </tr>
        </thead>
        <tbody>
          ${rowsHtml}
        </tbody>
      </table>
    </div>
  `
}

async function openLaporanAbsensiSantriDetail(santriId) {
  const sid = String(santriId || '').trim()
  if (!sid) return
  const content = document.getElementById('guru-content')
  if (!content) return

  const santri = (laporanBulananState.santriList || []).find(item => String(item.id) === sid)
  if (!santri) {
    alert('Data santri tidak ditemukan.')
    return
  }

  const kelas = laporanBulananState.kelasMap.get(String(santri.kelas_id || ''))
  const periode = laporanBulananState.periode || getMonthInputToday()
  const periodeRange = getPeriodeRange(periode)
  if (!periodeRange) {
    alert('Periode tidak valid.')
    return
  }

  content.innerHTML = 'Loading detail absensi...'

  const { data: rows, error } = await sb
    .from(ATTENDANCE_TABLE)
    .select('id, tanggal, status, mapel_id, kelas_id')
    .eq('santri_id', sid)
    .eq('kelas_id', String(santri.kelas_id))
    .gte('tanggal', periodeRange.start)
    .lte('tanggal', periodeRange.end)
    .order('tanggal', { ascending: true })

  if (error) {
    console.error(error)
    if (isMissingAbsensiTableError(error)) {
      content.innerHTML = `<div class="placeholder-card">${escapeHtml(buildAbsensiMissingTableMessage())}</div>`
      return
    }
    content.innerHTML = `<div class="placeholder-card">Gagal load detail absensi: ${escapeHtml(error.message || 'Unknown error')}</div>`
    return
  }

  const mapelIds = [...new Set((rows || []).map(item => item.mapel_id).filter(Boolean).map(String))]
  let mapelMap = new Map()
  if (mapelIds.length) {
    const mapelRes = await getMapelRowsByIds(mapelIds)
    if (!mapelRes.error) {
      mapelMap = new Map((mapelRes.data || []).map(item => [String(item.id), item]))
    }
  }

  const rowHtml = (rows || []).map((item, index) => {
    const mapel = mapelMap.get(String(item.mapel_id || ''))
    const mapelLabel = getMapelLabel(mapel)
    return `
      <tr>
        <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${index + 1}</td>
        <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(String(item.tanggal || '').slice(0, 10))}</td>
        <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(mapelLabel)}</td>
        <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.status || '-')}</td>
      </tr>
    `
  }).join('')

  content.innerHTML = `
    <div style="display:flex; align-items:center; gap:10px; margin-bottom:12px;">
      <button type="button" class="mapel-back-btn" onclick="renderLaporanAbsensiPage()">&lt;</button>
      <div style="font-weight:700; color:#0f172a;">Detail Absensi: ${escapeHtml(santri.nama || '-')} (${escapeHtml(kelas?.nama_kelas || '-')}) - ${escapeHtml(getPeriodeLabel(periode))}</div>
    </div>

    <div style="overflow:auto; border:1px solid #e2e8f0; border-radius:10px;">
      <table style="width:100%; min-width:620px; border-collapse:collapse; font-size:13px;">
        <thead>
          <tr style="background:#f8fafc;">
            <th style="padding:8px; border:1px solid #e2e8f0; width:44px;">No</th>
            <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Tanggal</th>
            <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Mapel</th>
            <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Status Kehadiran</th>
          </tr>
        </thead>
        <tbody>
          ${rowHtml || '<tr><td colspan="4" style="padding:10px; border:1px solid #e2e8f0; text-align:center;">Belum ada data absensi pada periode ini.</td></tr>'}
        </tbody>
      </table>
    </div>
  `
}

function onLaporanBulananPeriodChange(value) {
  const periode = String(value || '').trim()
  laporanBulananState.periode = /^\d{4}-\d{2}$/.test(periode) ? periode : getMonthInputToday()
  const currentPage = localStorage.getItem(GURU_LAST_PAGE_KEY) || ''
  if (currentPage === 'laporan-absensi') {
    renderLaporanAbsensiPage()
    return
  }
  renderLaporanBulananPage()
}

function getCurrentMonthlyWaTemplate() {
  const el = document.getElementById('laporan-bulanan-wa-template')
  if (el) {
    const value = String(el.value || '').trim()
    if (value) return value
  }
  return String(laporanBulananState.waTemplate || '').trim() || getDefaultMonthlyWaTemplate()
}

function setMonthlyWaTemplateEditMode(editing) {
  const isEditing = editing === true
  laporanBulananState.waTemplateEditing = isEditing

  const textarea = document.getElementById('laporan-bulanan-wa-template')
  if (textarea) {
    textarea.readOnly = !isEditing
    textarea.style.background = isEditing ? '#ffffff' : '#f8fafc'
    textarea.style.color = isEditing ? '#0f172a' : '#475569'
    if (isEditing) textarea.focus()
  }

  const editBtn = document.getElementById('btn-wa-template-edit')
  const saveBtn = document.getElementById('btn-wa-template-save')
  const cancelBtn = document.getElementById('btn-wa-template-cancel')
  if (editBtn) editBtn.style.display = isEditing ? 'none' : ''
  if (saveBtn) saveBtn.style.display = isEditing ? '' : 'none'
  if (cancelBtn) cancelBtn.style.display = isEditing ? '' : 'none'
}

function startMonthlyWaTemplateEdit() {
  setMonthlyWaTemplateEditMode(true)
}

function cancelMonthlyWaTemplateEdit() {
  const guruId = String(laporanBulananState?.guru?.id || '').trim()
  laporanBulananState.waTemplate = loadMonthlyWaTemplate(guruId)
  const el = document.getElementById('laporan-bulanan-wa-template')
  if (el) el.value = laporanBulananState.waTemplate || ''
  setMonthlyWaTemplateEditMode(false)
}

function saveMonthlyWaTemplate() {
  const guruId = String(laporanBulananState?.guru?.id || '').trim()
  if (!guruId) {
    alert('Data guru belum siap.')
    return
  }
  const template = getCurrentMonthlyWaTemplate()
  laporanBulananState.waTemplate = template
  localStorage.setItem(getMonthlyWaTemplateStorageKey(guruId), template)
  setMonthlyWaTemplateEditMode(false)
  alert('Template pesan berhasil disimpan.')
}

async function openLaporanBulananDetail(santriId) {
  const sid = String(santriId || '').trim()
  const content = document.getElementById('guru-content')
  if (!sid || !content) return

  const detailData = await getLaporanBulananDetailData(sid, { showError: true })
  if (!detailData) return

  const {
    santri,
    guru,
    kelas,
    periode,
    monthlyReportMissingTable,
    monthlyReportMissingColumns,
    nilaiAkhlak,
    predikat,
    catatanWali,
    muhaffiz,
    noHpMuhaffiz,
    nilaiKehadiranHalaqah,
    sakitHalaqah,
    izinHalaqah,
    nilaiAkhlakHalaqah,
    keteranganAkhlakHalaqah,
    nilaiUjianBulanan,
    keteranganUjianBulanan,
    nilaiTargetHafalan,
    keteranganTargetHafalan,
    nilaiCapaianHafalanBulanan,
    nilaiJumlahHafalanHalaman,
    nilaiJumlahHafalanJuz,
    catatanMuhaffiz,
    musyrif,
    noHpMusyrif,
    nilaiKehadiranLiqaMuhasabah,
    sakitLiqaMuhasabah,
    izinLiqaMuhasabah,
    nilaiIbadah,
    keteranganIbadah,
    nilaiKedisiplinan,
    keteranganKedisiplinan,
    nilaiKebersihan,
    keteranganKebersihan,
    nilaiAdab,
    keteranganAdab,
    prestasiKesantrian,
    pelanggaranKesantrian,
    catatanMusyrif,
    hpGuru,
    sakitCount,
    izinCount,
    nilaiKehadiranPersen,
    currentDetail
  } = detailData

  laporanBulananState.selectedSantriId = sid
  laporanBulananState.currentDetail = currentDetail

  content.innerHTML = `
    <div style="display:flex; align-items:center; gap:10px; margin-bottom:12px;">
      <button type="button" class="mapel-back-btn" onclick="backToLaporanBulananList()">&lt;</button>
      <div style="font-weight:700; color:#0f172a;">Detail Laporan Bulanan Santri</div>
    </div>

    ${monthlyReportMissingTable ? `<div class="placeholder-card" style="margin-bottom:12px;">${escapeHtml(buildMonthlyReportMissingTableMessage())}</div>` : ''}
    ${!monthlyReportMissingTable && monthlyReportMissingColumns ? `<div class="placeholder-card" style="margin-bottom:12px;">${escapeHtml(buildMonthlyReportMissingColumnsMessage())}</div>` : ''}

    <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap:10px; margin-bottom:12px;">
      <div class="placeholder-card"><strong>Periode</strong><br>${escapeHtml(getPeriodeLabel(periode))}</div>
      <div class="placeholder-card"><strong>Nama</strong><br>${escapeHtml(santri.nama || '-')}</div>
      <div class="placeholder-card"><strong>Kelas</strong><br>${escapeHtml(kelas?.nama_kelas || '-')}</div>
      <div class="placeholder-card"><strong>Wali Kelas</strong><br>${escapeHtml(guru?.nama || '-')}</div>
      <div class="placeholder-card"><strong>Nomor HP</strong><br>${escapeHtml(hpGuru)}</div>
    </div>

    <div class="placeholder-card" style="margin-bottom:12px; border-color:#cbd5e1;">
      <div style="font-weight:800; margin-bottom:10px; color:#0f172a;">A. Laporan Akademik</div>
      <div class="placeholder-card" style="margin-bottom:10px;">
        <div style="font-weight:700; margin-bottom:8px;">Kehadiran di Kelas</div>
        <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap:8px;">
          <div><strong>Nilai Kehadiran:</strong> ${escapeHtml(String(nilaiKehadiranPersen))}%</div>
          <div><strong>Keterangan Kehadiran:</strong> Sakit ${sakitCount} kali, Izin ${izinCount} kali</div>
        </div>
      </div>
      <div class="placeholder-card" style="margin-bottom:10px;">
        <div style="font-weight:700; margin-bottom:8px;">Akhlak di Kelas</div>
        <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap:10px; align-items:end;">
          <div>
            <label class="guru-label">Nilai Akhlak (A-E)</label>
            <select id="laporan-bulanan-nilai-akhlak" class="guru-field" onchange="onLaporanBulananNilaiAkhlakChange(this.value)">
              <option value="" ${nilaiAkhlak === '' ? 'selected' : ''}>Pilih Nilai</option>
              <option value="A" ${nilaiAkhlak === 'A' ? 'selected' : ''}>A</option>
              <option value="B" ${nilaiAkhlak === 'B' ? 'selected' : ''}>B</option>
              <option value="C" ${nilaiAkhlak === 'C' ? 'selected' : ''}>C</option>
              <option value="D" ${nilaiAkhlak === 'D' ? 'selected' : ''}>D</option>
              <option value="E" ${nilaiAkhlak === 'E' ? 'selected' : ''}>E</option>
            </select>
          </div>
          <div>
            <label class="guru-label">Keterangan</label>
            <input id="laporan-bulanan-predikat" class="guru-field" type="text" value="${escapeHtml(predikat)}" readonly style="background:#f8fafc; color:#475569;">
          </div>
        </div>
      </div>
      <div class="placeholder-card">
        <div style="font-weight:700; margin-bottom:8px;">Catatan Wali Kelas</div>
        <textarea id="laporan-bulanan-catatan-wali" class="guru-field" rows="5" placeholder="Tulis catatan perkembangan santri...">${escapeHtml(catatanWali)}</textarea>
      </div>
    </div>

    <div class="placeholder-card" style="margin-bottom:12px; border-color:#cbd5e1;">
      <div style="font-weight:800; margin-bottom:10px; color:#0f172a;">B. Laporan Ketahfizan</div>
      <div class="placeholder-card" style="margin-bottom:10px;">
      <div style="font-weight:700; margin-bottom:8px;">Data Ketahfizan</div>
      <div style="font-size:12px; color:#64748b; margin-bottom:8px;">Bagian ini diinput oleh Muhaffiz (view only).</div>
      <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap:10px; margin-bottom:10px;">
        <div>
          <label class="guru-label">Muhaffiz</label>
          <input id="laporan-bulanan-muhaffiz" class="guru-field" type="text" value="${escapeHtml(muhaffiz || '')}" placeholder="Nama muhaffiz" readonly style="background:#f8fafc; color:#475569;">
        </div>
        <div>
          <label class="guru-label">Nomor HP Muhaffiz</label>
          <input id="laporan-bulanan-nohp-muhaffiz" class="guru-field" type="text" value="${escapeHtml(noHpMuhaffiz || '')}" placeholder="08xxxxxxxxxx" readonly style="background:#f8fafc; color:#475569;">
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
                  <input id="laporan-bulanan-nilai-kehadiran-halaqah" class="guru-field" type="number" min="0" max="100" step="0.01" value="${escapeHtml(toInputValue(nilaiKehadiranHalaqah))}" placeholder="Nilai %" readonly style="background:#f8fafc; color:#475569;">
                  <span style="font-size:12px; color:#64748b;">%</span>
                </div>
              </td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <div style="display:flex; gap:6px; align-items:center; flex-wrap:wrap;">
                  <label style="font-size:12px; color:#475569;">Sakit</label>
                  <input id="laporan-bulanan-sakit-halaqah" class="guru-field" type="number" min="0" step="1" value="${escapeHtml(toInputValue(sakitHalaqah))}" style="max-width:90px; background:#f8fafc; color:#475569;" readonly>
                  <label style="font-size:12px; color:#475569;">Izin</label>
                  <input id="laporan-bulanan-izin-halaqah" class="guru-field" type="number" min="0" step="1" value="${escapeHtml(toInputValue(izinHalaqah))}" style="max-width:90px; background:#f8fafc; color:#475569;" readonly>
                  <span style="font-size:12px; color:#64748b;">kali</span>
                </div>
              </td>
            </tr>
            <tr>
              <td style="padding:8px; border:1px solid #e2e8f0;">Akhlak di Halaqah</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <select id="laporan-bulanan-nilai-akhlak-halaqah" class="guru-field" disabled style="background:#f8fafc; color:#475569;">
                  <option value="" ${!nilaiAkhlakHalaqah ? 'selected' : ''}>Pilih Nilai</option>
                  <option value="A" ${nilaiAkhlakHalaqah === 'A' ? 'selected' : ''}>A</option>
                  <option value="B" ${nilaiAkhlakHalaqah === 'B' ? 'selected' : ''}>B</option>
                  <option value="C" ${nilaiAkhlakHalaqah === 'C' ? 'selected' : ''}>C</option>
                  <option value="D" ${nilaiAkhlakHalaqah === 'D' ? 'selected' : ''}>D</option>
                  <option value="E" ${nilaiAkhlakHalaqah === 'E' ? 'selected' : ''}>E</option>
                </select>
              </td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <input id="laporan-bulanan-ket-akhlak-halaqah" class="guru-field" type="text" value="${escapeHtml(keteranganAkhlakHalaqah || '')}" readonly style="background:#f8fafc; color:#475569;">
              </td>
            </tr>
            <tr>
              <td style="padding:8px; border:1px solid #e2e8f0;">Ujian Bulanan</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <input id="laporan-bulanan-nilai-ujian-bulanan" class="guru-field" type="number" min="0" max="100" step="0.01" value="${escapeHtml(toInputValue(nilaiUjianBulanan))}" readonly style="background:#f8fafc; color:#475569;">
              </td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <input id="laporan-bulanan-ket-ujian-bulanan" class="guru-field" type="text" value="${escapeHtml(keteranganUjianBulanan || '')}" readonly style="background:#f8fafc; color:#475569;">
              </td>
            </tr>
            <tr>
              <td style="padding:8px; border:1px solid #e2e8f0;">Target Hafalan</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <div style="display:flex; gap:6px; align-items:center;">
                  <input id="laporan-bulanan-nilai-target-hafalan" class="guru-field" type="number" min="0" max="100" step="0.01" value="${escapeHtml(toInputValue(nilaiTargetHafalan))}" readonly style="background:#f8fafc; color:#475569;">
                  <span style="font-size:12px; color:#64748b;">%</span>
                </div>
              </td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <input id="laporan-bulanan-ket-target-hafalan" class="guru-field" type="text" value="${escapeHtml(keteranganTargetHafalan || '')}" placeholder="Isi manual keterangan target hafalan" readonly style="background:#f8fafc; color:#475569;">
              </td>
            </tr>
            <tr>
              <td style="padding:8px; border:1px solid #e2e8f0;">Capaian Hafalan Bulanan</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <div style="display:flex; gap:6px; align-items:center;">
                  <input id="laporan-bulanan-nilai-capaian-hafalan" class="guru-field" type="number" min="0" step="0.01" value="${escapeHtml(toInputValue(nilaiCapaianHafalanBulanan))}" readonly style="background:#f8fafc; color:#475569;">
                  <span style="font-size:12px; color:#64748b;">halaman</span>
                </div>
              </td>
              <td style="padding:8px; border:1px solid #e2e8f0; color:#94a3b8;">-</td>
            </tr>
            <tr>
              <td style="padding:8px; border:1px solid #e2e8f0;">Jumlah Hafalan</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <div style="display:flex; gap:6px; align-items:center; flex-wrap:wrap;">
                  <input id="laporan-bulanan-jumlah-hafalan-halaman" class="guru-field" type="number" min="0" step="0.01" value="${escapeHtml(toInputValue(nilaiJumlahHafalanHalaman))}" style="max-width:120px; background:#f8fafc; color:#475569;" readonly>
                  <span style="font-size:12px; color:#64748b;">halaman</span>
                  <input id="laporan-bulanan-jumlah-hafalan-juz" class="guru-field" type="number" min="0" step="0.01" value="${escapeHtml(toInputValue(nilaiJumlahHafalanJuz))}" style="max-width:120px; background:#f8fafc; color:#475569;" readonly>
                  <span style="font-size:12px; color:#64748b;">juz</span>
                </div>
              </td>
              <td style="padding:8px; border:1px solid #e2e8f0; color:#94a3b8;">-</td>
            </tr>
          </tbody>
        </table>
      </div>
      </div>
      <div class="placeholder-card" style="margin-bottom:0;">
      <div style="font-weight:700; margin-bottom:8px;">Catatan Muhaffiz</div>
      <textarea id="laporan-bulanan-catatan-muhaffiz" class="guru-field" rows="5" placeholder="Tulis catatan perkembangan tahfiz santri..." readonly style="background:#f8fafc; color:#475569;">${escapeHtml(catatanMuhaffiz || '')}</textarea>
    </div>
    </div>

    <div class="placeholder-card" style="margin-bottom:12px; border-color:#cbd5e1;">
      <div style="font-weight:800; margin-bottom:10px; color:#0f172a;">C. Laporan Kesantrian</div>
      <div class="placeholder-card" style="margin-bottom:10px;">
      <div style="font-weight:700; margin-bottom:8px;">Data Kesantrian</div>
      <div style="font-size:12px; color:#64748b; margin-bottom:8px;">Bagian ini diinput oleh Musyrif (view only).</div>
      <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap:10px; margin-bottom:10px;">
        <div>
          <label class="guru-label">Musyrif</label>
          <input id="laporan-bulanan-musyrif" class="guru-field" type="text" value="${escapeHtml(musyrif || '')}" placeholder="Nama musyrif" readonly style="background:#f8fafc; color:#475569;">
        </div>
        <div>
          <label class="guru-label">Nomor HP Musyrif</label>
          <input id="laporan-bulanan-nohp-musyrif" class="guru-field" type="text" value="${escapeHtml(noHpMusyrif || '')}" placeholder="08xxxxxxxxxx" readonly style="background:#f8fafc; color:#475569;">
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
                  <input id="laporan-bulanan-nilai-kehadiran-liqa-muhasabah" class="guru-field" type="number" min="0" max="100" step="0.01" value="${escapeHtml(toInputValue(nilaiKehadiranLiqaMuhasabah))}" placeholder="Nilai %" readonly style="background:#f8fafc; color:#475569;">
                  <span style="font-size:12px; color:#64748b;">%</span>
                </div>
              </td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <div style="display:flex; gap:6px; align-items:center; flex-wrap:wrap;">
                  <label style="font-size:12px; color:#475569;">Sakit</label>
                  <input id="laporan-bulanan-sakit-liqa-muhasabah" class="guru-field" type="number" min="0" step="1" value="${escapeHtml(toInputValue(sakitLiqaMuhasabah))}" style="max-width:90px; background:#f8fafc; color:#475569;" readonly>
                  <label style="font-size:12px; color:#475569;">Izin</label>
                  <input id="laporan-bulanan-izin-liqa-muhasabah" class="guru-field" type="number" min="0" step="1" value="${escapeHtml(toInputValue(izinLiqaMuhasabah))}" style="max-width:90px; background:#f8fafc; color:#475569;" readonly>
                  <span style="font-size:12px; color:#64748b;">kali</span>
                </div>
              </td>
            </tr>
            <tr>
              <td style="padding:8px; border:1px solid #e2e8f0;">Ibadah</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <select id="laporan-bulanan-nilai-ibadah" class="guru-field" disabled style="background:#f8fafc; color:#475569;">
                  <option value="" ${!nilaiIbadah ? 'selected' : ''}>Pilih Nilai</option>
                  <option value="A" ${nilaiIbadah === 'A' ? 'selected' : ''}>A</option>
                  <option value="B" ${nilaiIbadah === 'B' ? 'selected' : ''}>B</option>
                  <option value="C" ${nilaiIbadah === 'C' ? 'selected' : ''}>C</option>
                  <option value="D" ${nilaiIbadah === 'D' ? 'selected' : ''}>D</option>
                  <option value="E" ${nilaiIbadah === 'E' ? 'selected' : ''}>E</option>
                </select>
              </td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <input id="laporan-bulanan-ket-ibadah" class="guru-field" type="text" value="${escapeHtml(keteranganIbadah || '')}" readonly style="background:#f8fafc; color:#475569;">
              </td>
            </tr>
            <tr>
              <td style="padding:8px; border:1px solid #e2e8f0;">Kedisiplinan</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <select id="laporan-bulanan-nilai-kedisiplinan" class="guru-field" disabled style="background:#f8fafc; color:#475569;">
                  <option value="" ${!nilaiKedisiplinan ? 'selected' : ''}>Pilih Nilai</option>
                  <option value="A" ${nilaiKedisiplinan === 'A' ? 'selected' : ''}>A</option>
                  <option value="B" ${nilaiKedisiplinan === 'B' ? 'selected' : ''}>B</option>
                  <option value="C" ${nilaiKedisiplinan === 'C' ? 'selected' : ''}>C</option>
                  <option value="D" ${nilaiKedisiplinan === 'D' ? 'selected' : ''}>D</option>
                  <option value="E" ${nilaiKedisiplinan === 'E' ? 'selected' : ''}>E</option>
                </select>
              </td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <input id="laporan-bulanan-ket-kedisiplinan" class="guru-field" type="text" value="${escapeHtml(keteranganKedisiplinan || '')}" readonly style="background:#f8fafc; color:#475569;">
              </td>
            </tr>
            <tr>
              <td style="padding:8px; border:1px solid #e2e8f0;">Kebersihan</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <select id="laporan-bulanan-nilai-kebersihan" class="guru-field" disabled style="background:#f8fafc; color:#475569;">
                  <option value="" ${!nilaiKebersihan ? 'selected' : ''}>Pilih Nilai</option>
                  <option value="A" ${nilaiKebersihan === 'A' ? 'selected' : ''}>A</option>
                  <option value="B" ${nilaiKebersihan === 'B' ? 'selected' : ''}>B</option>
                  <option value="C" ${nilaiKebersihan === 'C' ? 'selected' : ''}>C</option>
                  <option value="D" ${nilaiKebersihan === 'D' ? 'selected' : ''}>D</option>
                  <option value="E" ${nilaiKebersihan === 'E' ? 'selected' : ''}>E</option>
                </select>
              </td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <input id="laporan-bulanan-ket-kebersihan" class="guru-field" type="text" value="${escapeHtml(keteranganKebersihan || '')}" readonly style="background:#f8fafc; color:#475569;">
              </td>
            </tr>
            <tr>
              <td style="padding:8px; border:1px solid #e2e8f0;">Adab</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <select id="laporan-bulanan-nilai-adab" class="guru-field" disabled style="background:#f8fafc; color:#475569;">
                  <option value="" ${!nilaiAdab ? 'selected' : ''}>Pilih Nilai</option>
                  <option value="A" ${nilaiAdab === 'A' ? 'selected' : ''}>A</option>
                  <option value="B" ${nilaiAdab === 'B' ? 'selected' : ''}>B</option>
                  <option value="C" ${nilaiAdab === 'C' ? 'selected' : ''}>C</option>
                  <option value="D" ${nilaiAdab === 'D' ? 'selected' : ''}>D</option>
                  <option value="E" ${nilaiAdab === 'E' ? 'selected' : ''}>E</option>
                </select>
              </td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <input id="laporan-bulanan-ket-adab" class="guru-field" type="text" value="${escapeHtml(keteranganAdab || '')}" readonly style="background:#f8fafc; color:#475569;">
              </td>
            </tr>
            <tr>
              <td style="padding:8px; border:1px solid #e2e8f0;">Prestasi</td>
              <td colspan="2" style="padding:8px; border:1px solid #e2e8f0;">
                <input id="laporan-bulanan-prestasi-kesantrian" class="guru-field" type="text" value="${escapeHtml(prestasiKesantrian || '')}" readonly style="background:#f8fafc; color:#475569;">
              </td>
            </tr>
            <tr>
              <td style="padding:8px; border:1px solid #e2e8f0;">Pelanggaran</td>
              <td colspan="2" style="padding:8px; border:1px solid #e2e8f0;">
                <input id="laporan-bulanan-pelanggaran-kesantrian" class="guru-field" type="text" value="${escapeHtml(pelanggaranKesantrian || '')}" readonly style="background:#f8fafc; color:#475569;">
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      </div>
      <div class="placeholder-card" style="margin-bottom:0;">
      <div style="font-weight:700; margin-bottom:8px;">Catatan Musyrif</div>
      <textarea id="laporan-bulanan-catatan-musyrif" class="guru-field" rows="5" placeholder="Tulis catatan perkembangan kesantrian santri..." readonly style="background:#f8fafc; color:#475569;">${escapeHtml(catatanMusyrif || '')}</textarea>
    </div>
    </div>

    <div class="placeholder-card">
      <div style="margin-top:10px; display:flex; gap:8px; flex-wrap:wrap;">
        <button type="button" class="modal-btn" onclick="printLaporanBulanan()">Cetak PDF</button>
        <button type="button" class="modal-btn" onclick="quickSendLaporanBulananWA('${escapeHtml(String(santri.id))}')">Kirim WA</button>
        <button type="button" class="modal-btn modal-btn-primary" onclick="saveLaporanBulananDetail()">Simpan Laporan Bulanan</button>
      </div>
    </div>
  `
}

async function getLaporanBulananDetailData(santriId, opts = {}) {
  const showError = opts.showError !== false
  const sid = String(santriId || '').trim()
  const santri = (laporanBulananState.santriList || []).find(item => String(item.id) === sid)
  if (!santri) {
    if (showError) alert('Data santri tidak ditemukan.')
    return null
  }

  const guru = laporanBulananState.guru
  const kelas = laporanBulananState.kelasMap.get(String(santri.kelas_id || ''))
  const periode = laporanBulananState.periode || getMonthInputToday()
  const periodeRange = getPeriodeRange(periode)
  if (!periodeRange) {
    if (showError) alert('Periode tidak valid.')
    return null
  }

  const { data: absensiRows, error: absensiError } = await sb
    .from(ATTENDANCE_TABLE)
    .select('status, tanggal')
    .eq('santri_id', sid)
    .eq('kelas_id', String(santri.kelas_id))
    .gte('tanggal', periodeRange.start)
    .lte('tanggal', periodeRange.end)

  if (absensiError && !isMissingAbsensiTableError(absensiError)) {
    console.error(absensiError)
  }

  const attendanceRows = absensiError ? [] : (absensiRows || [])
  const dailyAttendanceList = aggregateAttendanceByDay(attendanceRows)
  const totalHari = dailyAttendanceList.length
  const hadirCount = dailyAttendanceList.filter(item => item.status === 'Hadir').length
  const sakitCount = dailyAttendanceList.filter(item => item.status === 'Sakit').length
  const izinCount = dailyAttendanceList.filter(item => item.status === 'Izin').length
  const nilaiKehadiranPersen = totalHari > 0 ? round2((hadirCount / totalHari) * 100) : 0
  const predikatKehadiran = getKehadiranPredikat(nilaiKehadiranPersen)

  let monthlyReport = null
  let monthlyReportMissingTable = false
  let monthlyReportMissingColumns = false
  const reportSelectNew = 'id, nilai_akhlak, predikat, catatan_wali, muhaffiz, no_hp_muhaffiz, nilai_kehadiran_halaqah, sakit_halaqah, izin_halaqah, nilai_akhlak_halaqah, keterangan_akhlak_halaqah, nilai_ujian_bulanan, keterangan_ujian_bulanan, nilai_target_hafalan, keterangan_target_hafalan, nilai_capaian_hafalan_bulanan, nilai_jumlah_hafalan_halaman, nilai_jumlah_hafalan_juz, catatan_muhaffiz, musyrif, no_hp_musyrif, nilai_kehadiran_liqa_muhasabah, sakit_liqa_muhasabah, izin_liqa_muhasabah, nilai_ibadah, keterangan_ibadah, nilai_kedisiplinan, keterangan_kedisiplinan, nilai_kebersihan, keterangan_kebersihan, nilai_adab, keterangan_adab, prestasi_kesantrian, pelanggaran_kesantrian, catatan_musyrif'
  const reportSelectLegacy = 'id, nilai_akhlak, predikat, catatan_wali'

  let reportRes = await sb
    .from(MONTHLY_REPORT_TABLE)
    .select(reportSelectNew)
    .eq('periode', periode)
    .eq('guru_id', String(guru.id))
    .eq('kelas_id', String(santri.kelas_id))
    .eq('santri_id', sid)
    .maybeSingle()

  if (reportRes.error && isMissingMonthlyReportColumnError(reportRes.error)) {
    monthlyReportMissingColumns = true
    reportRes = await sb
      .from(MONTHLY_REPORT_TABLE)
      .select(reportSelectLegacy)
      .eq('periode', periode)
      .eq('guru_id', String(guru.id))
      .eq('kelas_id', String(santri.kelas_id))
      .eq('santri_id', sid)
      .maybeSingle()
  }

  if (reportRes.error) {
    if (isMissingMonthlyReportTableError(reportRes.error)) {
      monthlyReportMissingTable = true
    } else {
      console.error(reportRes.error)
      if (showError) alert(`Gagal load data laporan bulanan: ${reportRes.error.message || 'Unknown error'}`)
      return null
    }
  } else {
    monthlyReport = reportRes.data || null
  }

  const nilaiAkhlak = monthlyReport?.nilai_akhlak === null || monthlyReport?.nilai_akhlak === undefined
    ? ''
    : normalizeAkhlakGrade(monthlyReport.nilai_akhlak)
  const predikat = nilaiAkhlak ? getAkhlakKeteranganByGrade(nilaiAkhlak) : ''
  const catatanWali = monthlyReport?.catatan_wali || ''
  const hpGuru = pickLabelByKeys(guru, ['no_hp', 'hp', 'no_telp', 'nomor_hp', 'telepon']) || '-'
  const muhaffiz = String(monthlyReport?.muhaffiz || '').trim()
  const noHpMuhaffiz = String(monthlyReport?.no_hp_muhaffiz || '').trim()
  const nilaiKehadiranHalaqah = toNullableNumber(monthlyReport?.nilai_kehadiran_halaqah)
  const sakitHalaqah = toNullableNumber(monthlyReport?.sakit_halaqah)
  const izinHalaqah = toNullableNumber(monthlyReport?.izin_halaqah)
  const nilaiAkhlakHalaqah = normalizeAkhlakGrade(monthlyReport?.nilai_akhlak_halaqah)
  const keteranganAkhlakHalaqah = String(monthlyReport?.keterangan_akhlak_halaqah || '').trim() || (nilaiAkhlakHalaqah ? getAkhlakKeteranganByGrade(nilaiAkhlakHalaqah) : '')
  const nilaiUjianBulanan = toNullableNumber(monthlyReport?.nilai_ujian_bulanan)
  const keteranganUjianBulanan = String(monthlyReport?.keterangan_ujian_bulanan || '').trim() || getUjianBulananKeterangan(nilaiUjianBulanan)
  const nilaiTargetHafalan = toNullableNumber(monthlyReport?.nilai_target_hafalan)
  const keteranganTargetHafalan = String(monthlyReport?.keterangan_target_hafalan || '').trim()
  const nilaiCapaianHafalanBulanan = toNullableNumber(monthlyReport?.nilai_capaian_hafalan_bulanan)
  const nilaiJumlahHafalanHalaman = toNullableNumber(monthlyReport?.nilai_jumlah_hafalan_halaman)
  const nilaiJumlahHafalanJuz = toNullableNumber(monthlyReport?.nilai_jumlah_hafalan_juz)
  const catatanMuhaffiz = String(monthlyReport?.catatan_muhaffiz || '').trim()
  const musyrif = String(monthlyReport?.musyrif || '').trim()
  const noHpMusyrif = String(monthlyReport?.no_hp_musyrif || '').trim()
  const nilaiKehadiranLiqaMuhasabah = toNullableNumber(monthlyReport?.nilai_kehadiran_liqa_muhasabah)
  const sakitLiqaMuhasabah = toNullableNumber(monthlyReport?.sakit_liqa_muhasabah)
  const izinLiqaMuhasabah = toNullableNumber(monthlyReport?.izin_liqa_muhasabah)
  const nilaiIbadah = normalizeAkhlakGrade(monthlyReport?.nilai_ibadah)
  const keteranganIbadah = String(monthlyReport?.keterangan_ibadah || '').trim() || (nilaiIbadah ? getAkhlakKeteranganByGrade(nilaiIbadah) : '')
  const nilaiKedisiplinan = normalizeAkhlakGrade(monthlyReport?.nilai_kedisiplinan)
  const keteranganKedisiplinan = String(monthlyReport?.keterangan_kedisiplinan || '').trim() || (nilaiKedisiplinan ? getAkhlakKeteranganByGrade(nilaiKedisiplinan) : '')
  const nilaiKebersihan = normalizeAkhlakGrade(monthlyReport?.nilai_kebersihan)
  const keteranganKebersihan = String(monthlyReport?.keterangan_kebersihan || '').trim() || (nilaiKebersihan ? getAkhlakKeteranganByGrade(nilaiKebersihan) : '')
  const nilaiAdab = normalizeAkhlakGrade(monthlyReport?.nilai_adab)
  const keteranganAdab = String(monthlyReport?.keterangan_adab || '').trim() || (nilaiAdab ? getAkhlakKeteranganByGrade(nilaiAdab) : '')
  const prestasiKesantrian = String(monthlyReport?.prestasi_kesantrian || '').trim()
  const pelanggaranKesantrian = String(monthlyReport?.pelanggaran_kesantrian || '').trim()
  const catatanMusyrif = String(monthlyReport?.catatan_musyrif || '').trim()

  const noHpAyahRaw = pickLabelByKeys(santri, ['no_hp_ayah', 'hp_ayah']) || ''
  const noHpIbuRaw = pickLabelByKeys(santri, ['no_hp_ibu', 'hp_ibu']) || ''

  const noHpOrtuRaw = pickLabelByKeys(
    santri,
    [
      'no_hp_ayah',
      'hp_ayah',
      'no_hp_ibu',
      'hp_ibu',
      'no_hp_orang_tua',
      'hp_orang_tua',
      'no_hp_wali',
      'hp_wali',
      // fallback lama jika data ortu belum ada
      'no_hp',
      'hp',
      'no_telp',
      'nomor_hp',
      'telepon'
    ]
  ) || ''

  const currentDetail = {
    periodeLabel: getPeriodeLabel(periode),
    nama: santri.nama || '-',
    kelas: kelas?.nama_kelas || '-',
    waliKelas: guru?.nama || '-',
    nomorHp: hpGuru,
    nilaiKehadiranPersen,
    predikatKehadiran,
    sakitCount,
    izinCount,
    nilaiAkhlakRaw: nilaiAkhlak === '' ? null : nilaiAkhlak,
    nilaiAkhlak: nilaiAkhlak === '' ? '-' : nilaiAkhlak,
    predikatAkhlak: predikat || '-',
    catatanWali: catatanWali || '-',
    muhaffiz: muhaffiz || '-',
    nomorHpMuhaffiz: noHpMuhaffiz || '-',
    nilaiKehadiranHalaqah: nilaiKehadiranHalaqah === null ? '-' : String(round2(nilaiKehadiranHalaqah)),
    sakitHalaqah: Number.isFinite(sakitHalaqah) ? sakitHalaqah : 0,
    izinHalaqah: Number.isFinite(izinHalaqah) ? izinHalaqah : 0,
    nilaiAkhlakHalaqah: nilaiAkhlakHalaqah || '-',
    keteranganAkhlakHalaqah: keteranganAkhlakHalaqah || '-',
    nilaiUjianBulanan: nilaiUjianBulanan === null ? '-' : String(round2(nilaiUjianBulanan)),
    keteranganUjianBulanan: keteranganUjianBulanan || '-',
    nilaiTargetHafalan: nilaiTargetHafalan === null ? '-' : `${round2(nilaiTargetHafalan)}%`,
    keteranganTargetHafalan: keteranganTargetHafalan || '-',
    nilaiCapaianHafalanBulanan: nilaiCapaianHafalanBulanan === null ? '-' : `${round2(nilaiCapaianHafalanBulanan)} halaman`,
    nilaiJumlahHafalan: (nilaiJumlahHafalanHalaman === null && nilaiJumlahHafalanJuz === null)
      ? '-'
      : `${nilaiJumlahHafalanHalaman === null ? '-' : round2(nilaiJumlahHafalanHalaman)} halaman / ${nilaiJumlahHafalanJuz === null ? '-' : round2(nilaiJumlahHafalanJuz)} juz`,
    catatanMuhaffiz: catatanMuhaffiz || '-',
    musyrif: musyrif || '-',
    nomorHpMusyrif: noHpMusyrif || '-',
    nilaiKehadiranLiqaMuhasabah: nilaiKehadiranLiqaMuhasabah === null ? '-' : String(round2(nilaiKehadiranLiqaMuhasabah)),
    sakitLiqaMuhasabah: Number.isFinite(sakitLiqaMuhasabah) ? sakitLiqaMuhasabah : 0,
    izinLiqaMuhasabah: Number.isFinite(izinLiqaMuhasabah) ? izinLiqaMuhasabah : 0,
    nilaiIbadah: nilaiIbadah || '-',
    keteranganIbadah: keteranganIbadah || '-',
    nilaiKedisiplinan: nilaiKedisiplinan || '-',
    keteranganKedisiplinan: keteranganKedisiplinan || '-',
    nilaiKebersihan: nilaiKebersihan || '-',
    keteranganKebersihan: keteranganKebersihan || '-',
    nilaiAdab: nilaiAdab || '-',
    keteranganAdab: keteranganAdab || '-',
    prestasiKesantrian: prestasiKesantrian || '-',
    pelanggaranKesantrian: pelanggaranKesantrian || '-',
    catatanMusyrif: catatanMusyrif || '-'
  }

  return {
    santri,
    guru,
    kelas,
    periode,
    periodeRange,
    monthlyReportMissingTable,
    monthlyReportMissingColumns,
    nilaiAkhlak,
    predikat,
    catatanWali,
    muhaffiz,
    noHpMuhaffiz,
    nilaiKehadiranHalaqah,
    sakitHalaqah,
    izinHalaqah,
    nilaiAkhlakHalaqah,
    keteranganAkhlakHalaqah,
    nilaiUjianBulanan,
    keteranganUjianBulanan,
    nilaiTargetHafalan,
    keteranganTargetHafalan,
    nilaiCapaianHafalanBulanan,
    nilaiJumlahHafalanHalaman,
    nilaiJumlahHafalanJuz,
    catatanMuhaffiz,
    musyrif,
    noHpMusyrif,
    nilaiKehadiranLiqaMuhasabah,
    sakitLiqaMuhasabah,
    izinLiqaMuhasabah,
    nilaiIbadah,
    keteranganIbadah,
    nilaiKedisiplinan,
    keteranganKedisiplinan,
    nilaiKebersihan,
    keteranganKebersihan,
    nilaiAdab,
    keteranganAdab,
    prestasiKesantrian,
    pelanggaranKesantrian,
    catatanMusyrif,
    hpGuru,
    noHpAyahRaw,
    noHpIbuRaw,
    noHpOrtuRaw,
    dailyAttendanceList,
    sakitCount,
    izinCount,
    nilaiKehadiranPersen,
    currentDetail
  }
}

async function quickPrintLaporanBulanan(santriId) {
  const sid = String(santriId || '').trim()
  const detailData = await getLaporanBulananDetailData(sid, { showError: true })
  if (!detailData) return
  laporanBulananState.currentDetail = detailData.currentDetail
  printLaporanBulanan()
}

async function quickSendLaporanBulananWA(santriId) {
  const sid = String(santriId || '').trim()
  const detailData = await getLaporanBulananDetailData(sid, { showError: true })
  if (!detailData) return

  const defaultPhone = String(detailData.noHpOrtuRaw || '').trim()
  const parentChoices = []
  if (String(detailData.noHpAyahRaw || '').trim()) {
    parentChoices.push({ label: 'Ayah', number: String(detailData.noHpAyahRaw || '').trim() })
  }
  if (String(detailData.noHpIbuRaw || '').trim()) {
    parentChoices.push({ label: 'Ibu', number: String(detailData.noHpIbuRaw || '').trim() })
  }
  const chosenPhoneRaw = await askWaTargetNumber(defaultPhone, parentChoices)
  if (chosenPhoneRaw === null) return

  const phone = normalizeWhatsappNumber(chosenPhoneRaw)
  if (!phone) {
    alert('Nomor WhatsApp tujuan belum valid. Isi nomor yang benar lalu coba lagi.')
    return
  }

  const detail = detailData.currentDetail
  laporanBulananState.currentDetail = detail
  const doc = createLaporanBulananPdfDoc(detail)
  const pdfBlob = doc.output('blob')

  const periodeSlug = sanitizeFileNamePart(detail.periodeLabel || '').replace(/\s+/g, '-')
  const namaSlug = sanitizeFileNamePart(detail.nama || '').replace(/\s+/g, '-')
  const fileName = `Laporan Evaluasi Bulan ${sanitizeFileNamePart(detail.periodeLabel)} - ${sanitizeFileNamePart(detail.nama)}.pdf`
  const storagePath = `${String(detailData.guru.id)}/${periodeSlug || 'periode'}/${namaSlug || sid}-${Date.now()}.pdf`

  const uploadRes = await sb
    .storage
    .from(MONTHLY_REPORT_STORAGE_BUCKET)
    .upload(storagePath, pdfBlob, {
      cacheControl: '3600',
      contentType: 'application/pdf',
      upsert: true
    })

  if (uploadRes.error) {
    const msg = String(uploadRes.error.message || '').toLowerCase()
    if (msg.includes('bucket') || msg.includes('not found')) {
      alert(buildMonthlyReportStorageMissingMessage())
      return
    }
    alert(`Gagal upload PDF laporan: ${uploadRes.error.message || 'Unknown error'}`)
    return
  }

  const { data: publicUrlData } = sb
    .storage
    .from(MONTHLY_REPORT_STORAGE_BUCKET)
    .getPublicUrl(storagePath)

  const publicUrl = publicUrlData?.publicUrl || ''
  if (!publicUrl) {
    alert('Gagal mendapatkan link laporan PDF.')
    return
  }

  const rawTemplate = getCurrentMonthlyWaTemplate()
  const message = rawTemplate
    .replace(/<nama santri>/gi, String(detail.nama || '-'))
    .replace(/<link>/gi, publicUrl)

  const waUrl = `https://wa.me/${phone}?text=${encodeURIComponent(message)}`
  window.open(waUrl, '_blank')
}

function backToLaporanBulananList() {
  laporanBulananState.selectedSantriId = ''
  renderLaporanBulananPage()
}

function onLaporanBulananNilaiAkhlakChange(value) {
  const predikatEl = document.getElementById('laporan-bulanan-predikat')
  if (!predikatEl) return
  const grade = normalizeAkhlakGrade(value)
  predikatEl.value = grade ? getAkhlakKeteranganByGrade(grade) : ''
}

function onLaporanBulananAkhlakHalaqahChange(value) {
  const ketEl = document.getElementById('laporan-bulanan-ket-akhlak-halaqah')
  if (!ketEl) return
  const grade = normalizeAkhlakGrade(value)
  ketEl.value = grade ? getAkhlakKeteranganByGrade(grade) : ''
}

function onLaporanBulananUjianBulananChange(value) {
  const ketEl = document.getElementById('laporan-bulanan-ket-ujian-bulanan')
  if (!ketEl) return
  ketEl.value = getUjianBulananKeterangan(value)
}

async function saveLaporanBulananDetail() {
  const sid = String(laporanBulananState.selectedSantriId || '').trim()
  const santri = (laporanBulananState.santriList || []).find(item => String(item.id) === sid)
  const guru = laporanBulananState.guru
  const periode = laporanBulananState.periode || getMonthInputToday()
  if (!sid || !santri || !guru?.id) {
    alert('Data laporan belum siap.')
    return
  }

  const nilaiAkhlakInput = document.getElementById('laporan-bulanan-nilai-akhlak')
  const predikatInput = document.getElementById('laporan-bulanan-predikat')
  const catatanInput = document.getElementById('laporan-bulanan-catatan-wali')

  const nilaiAkhlakRaw = String(nilaiAkhlakInput?.value || '').trim().toUpperCase()
  const nilaiAkhlakGrade = nilaiAkhlakRaw === '' ? '' : normalizeAkhlakGrade(nilaiAkhlakRaw)
  if (nilaiAkhlakRaw && !nilaiAkhlakGrade) {
    alert('Nilai akhlak harus A, B, C, D, atau E.')
    return
  }

  const predikat = String(predikatInput?.value || '').trim()
  const catatanWali = String(catatanInput?.value || '').trim()
  const nilaiAkhlakNumeric = nilaiAkhlakGrade ? getAkhlakNumericValueByGrade(nilaiAkhlakGrade) : null

  const payload = {
    periode,
    guru_id: String(guru.id),
    kelas_id: String(santri.kelas_id),
    santri_id: sid,
    nilai_akhlak: nilaiAkhlakNumeric === null ? null : round2(nilaiAkhlakNumeric),
    predikat: predikat || null,
    catatan_wali: catatanWali || null
  }

  const { error } = await sb
    .from(MONTHLY_REPORT_TABLE)
    .upsert(payload, { onConflict: 'periode,guru_id,kelas_id,santri_id' })

  if (error) {
    console.error(error)
    if (isMissingMonthlyReportTableError(error)) {
      alert(buildMonthlyReportMissingTableMessage())
      return
    }
    if (isMissingMonthlyReportColumnError(error)) {
      alert(buildMonthlyReportMissingColumnsMessage())
      return
    }
    alert(`Gagal simpan laporan bulanan: ${error.message || 'Unknown error'}`)
    return
  }

  alert('Laporan bulanan berhasil disimpan.')
  clearGuruPageCache('laporan-bulanan')
  await openLaporanBulananDetail(sid)
}

function printLaporanBulanan() {
  const detail = laporanBulananState.currentDetail
  if (!detail) {
    alert('Detail laporan belum siap dicetak.')
    return
  }

  const doc = createLaporanBulananPdfDoc(detail)
  if (!doc) return

  const cleanPeriode = sanitizeFileNamePart(detail.periodeLabel || '') || 'Periode'
  const cleanNama = sanitizeFileNamePart(detail.nama || '') || 'Santri'
  const fileName = `Laporan Evaluasi Bulan ${cleanPeriode} - ${cleanNama}.pdf`
  doc.save(fileName)
}

function createLaporanBulananPdfDoc(detail) {
  if (!detail) return null

  const akhlakInfo = getAkhlakGradeInfo(detail.nilaiAkhlakRaw)
  let akhlakKeterangan = detail.predikatAkhlak && detail.predikatAkhlak !== '-'
    ? detail.predikatAkhlak
    : akhlakInfo.desc
  akhlakKeterangan = String(akhlakKeterangan || '')
    .replace(/^[A-E]\s*/i, '')
    .replace(/[()]/g, '')
    .replace(/\s{2,}/g, ' ')
    .trim() || '-'

  const jsPdfApi = window.jspdf
  if (!jsPdfApi || typeof jsPdfApi.jsPDF !== 'function') {
    alert('Library PDF belum termuat. Refresh halaman lalu coba lagi.')
    return
  }

  const { jsPDF } = jsPdfApi
  const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' })

  const margin = 25
  const pageWidth = doc.internal.pageSize.getWidth()
  const usableWidth = pageWidth - (margin * 2)
  let y = margin

  doc.setFont('times', 'bold')
  doc.setFontSize(12)
  doc.text('LAPORAN EVALUASI SANTRI', pageWidth / 2, y, { align: 'center' })
  y += 6
  doc.text(`Periode ${detail.periodeLabel}`, pageWidth / 2, y, { align: 'center' })

  y += 12
  doc.text(`Nama: ${detail.nama}`, margin, y)
  y += 7
  doc.text(`Kelas: ${detail.kelas}`, margin, y)

  y += 12
  doc.text('A. Laporan Akademik', margin, y)

  y += 6
  doc.setFont('times', 'normal')
  doc.text(`Wali Kelas: ${detail.waliKelas}`, margin + 8, y)
  y += 7
  doc.text(`Nomor HP: ${detail.nomorHp}`, margin + 8, y)

  y += 5
  const tableBody = [
    [
      'Kehadiran di kelas',
      `${detail.nilaiKehadiranPersen}%`,
      `Sakit ${detail.sakitCount || 0} kali\nIzin ${detail.izinCount || 0} kali`
    ],
    [
      'Akhlak di kelas',
      `${akhlakInfo.grade}`,
      akhlakKeterangan
    ],
    [
      {
        content: `Catatan wali kelas:\n${detail.catatanWali || '-'}`,
        colSpan: 3,
        styles: { halign: 'left' }
      }
    ]
  ]

  if (typeof doc.autoTable === 'function') {
    doc.autoTable({
      startY: y,
      margin: { left: margin + 8, right: margin },
      head: [['Aspek Penilaian', 'Nilai', 'Keterangan']],
      body: tableBody,
      theme: 'grid',
      styles: {
        font: 'times',
        fontSize: 12,
        cellPadding: 2.5,
        textColor: [17, 24, 39],
        valign: 'middle',
        lineWidth: 0.2,
        lineColor: [17, 24, 39]
      },
      headStyles: {
        fillColor: [237, 211, 127],
        textColor: [17, 24, 39],
        halign: 'center',
        fontStyle: 'bold',
        lineWidth: 0.2,
        lineColor: [17, 24, 39]
      },
      bodyStyles: {
        lineWidth: 0.2,
        lineColor: [17, 24, 39]
      },
      columnStyles: {
        0: { cellWidth: usableWidth * 0.40 },
        1: { cellWidth: usableWidth * 0.18, halign: 'center' },
        2: { cellWidth: usableWidth * 0.34 }
      }
    })

    let nextY = Number(doc.lastAutoTable?.finalY || y) + 10
    doc.setFont('times', 'bold')
    doc.setFontSize(12)
    doc.text('B. Laporan Ketahfizan', margin, nextY)
    nextY += 6
    doc.setFont('times', 'normal')
    doc.text(`Muhaffiz: ${detail.muhaffiz || '-'}`, margin + 8, nextY)
    nextY += 7
    doc.text(`Nomor HP: ${detail.nomorHpMuhaffiz || '-'}`, margin + 8, nextY)

    const tableBodyTahfiz = [
      [
        'Kehadiran di halaqah',
        `${detail.nilaiKehadiranHalaqah || '-'}`,
        `Sakit ${detail.sakitHalaqah || 0} kali\nIzin ${detail.izinHalaqah || 0} kali`
      ],
      [
        'Akhlak di halaqah',
        `${detail.nilaiAkhlakHalaqah || '-'}`,
        `${detail.keteranganAkhlakHalaqah || '-'}`
      ],
      [
        'Ujian bulanan',
        `${detail.nilaiUjianBulanan || '-'}`,
        `${detail.keteranganUjianBulanan || '-'}`
      ],
      [
        'Target hafalan',
        `${detail.nilaiTargetHafalan || '-'}`,
        `${detail.keteranganTargetHafalan || '-'}`
      ],
      [
        'Capaian hafalan bulanan',
        `${detail.nilaiCapaianHafalanBulanan || '-'}`,
        '-'
      ],
      [
        'Jumlah hafalan',
        `${detail.nilaiJumlahHafalan || '-'}`,
        '-'
      ],
      [
        {
          content: `Catatan muhaffiz:\n${detail.catatanMuhaffiz || '-'}`,
          colSpan: 3,
          styles: { halign: 'left' }
        }
      ]
    ]

    doc.autoTable({
      startY: nextY + 5,
      margin: { left: margin + 8, right: margin },
      head: [['Aspek Penilaian', 'Nilai', 'Keterangan']],
      body: tableBodyTahfiz,
      theme: 'grid',
      styles: {
        font: 'times',
        fontSize: 12,
        cellPadding: 2.5,
        textColor: [17, 24, 39],
        valign: 'middle',
        lineWidth: 0.2,
        lineColor: [17, 24, 39]
      },
      headStyles: {
        fillColor: [237, 211, 127],
        textColor: [17, 24, 39],
        halign: 'center',
        fontStyle: 'bold',
        lineWidth: 0.2,
        lineColor: [17, 24, 39]
      },
      bodyStyles: {
        lineWidth: 0.2,
        lineColor: [17, 24, 39]
      },
      columnStyles: {
        0: { cellWidth: usableWidth * 0.40 },
        1: { cellWidth: usableWidth * 0.18, halign: 'center' },
        2: { cellWidth: usableWidth * 0.34 }
      }
    })

    nextY = Number(doc.lastAutoTable?.finalY || (nextY + 5)) + 10
    doc.addPage()
    nextY = margin
    doc.setFont('times', 'bold')
    doc.setFontSize(12)
    doc.text('C. Laporan Kesantrian', margin, nextY)
    nextY += 6
    doc.setFont('times', 'normal')
    doc.text(`Musyrif: ${detail.musyrif || '-'}`, margin + 8, nextY)
    nextY += 7
    doc.text(`Nomor HP: ${detail.nomorHpMusyrif || '-'}`, margin + 8, nextY)

    const tableBodyKesantrian = [
      [
        "Kehadiran di Liqa' Muhasabah",
        `${detail.nilaiKehadiranLiqaMuhasabah || '-'}`,
        `Sakit ${detail.sakitLiqaMuhasabah || 0} kali\nIzin ${detail.izinLiqaMuhasabah || 0} kali`
      ],
      [
        'Ibadah',
        `${detail.nilaiIbadah || '-'}`,
        `${detail.keteranganIbadah || '-'}`
      ],
      [
        'Kedisiplinan',
        `${detail.nilaiKedisiplinan || '-'}`,
        `${detail.keteranganKedisiplinan || '-'}`
      ],
      [
        'Kebersihan',
        `${detail.nilaiKebersihan || '-'}`,
        `${detail.keteranganKebersihan || '-'}`
      ],
      [
        'Adab',
        `${detail.nilaiAdab || '-'}`,
        `${detail.keteranganAdab || '-'}`
      ],
      [
        'Prestasi',
        `${detail.prestasiKesantrian || '-'}`,
        '-'
      ],
      [
        'Pelanggaran',
        `${detail.pelanggaranKesantrian || '-'}`,
        '-'
      ],
      [
        {
          content: `Catatan musyrif:\n${detail.catatanMusyrif || '-'}`,
          colSpan: 3,
          styles: { halign: 'left' }
        }
      ]
    ]

    doc.autoTable({
      startY: nextY + 5,
      margin: { left: margin + 8, right: margin },
      head: [['Aspek Penilaian', 'Nilai', 'Keterangan']],
      body: tableBodyKesantrian,
      theme: 'grid',
      styles: {
        font: 'times',
        fontSize: 12,
        cellPadding: 2.5,
        textColor: [17, 24, 39],
        valign: 'middle',
        lineWidth: 0.2,
        lineColor: [17, 24, 39]
      },
      headStyles: {
        fillColor: [237, 211, 127],
        textColor: [17, 24, 39],
        halign: 'center',
        fontStyle: 'bold',
        lineWidth: 0.2,
        lineColor: [17, 24, 39]
      },
      bodyStyles: {
        lineWidth: 0.2,
        lineColor: [17, 24, 39]
      },
      columnStyles: {
        0: { cellWidth: usableWidth * 0.40 },
        1: { cellWidth: usableWidth * 0.18, halign: 'center' },
        2: { cellWidth: usableWidth * 0.34 }
      }
    })
  } else {
    alert('Plugin tabel PDF belum termuat. Refresh halaman lalu coba lagi.')
    return null
  }

  return doc
}

async function printRaporDetail() {
  const detail = raporState.currentDetail
  if (!detail) {
    alert('Detail rapor belum siap dicetak.')
    return
  }

  const jsPdfApi = window.jspdf
  if (!jsPdfApi || typeof jsPdfApi.jsPDF !== 'function') {
    alert('Library PDF belum termuat. Refresh halaman lalu coba lagi.')
    return
  }

  let bgDataUrl = ''
  try {
    bgDataUrl = await loadPdfBackgroundDataUrl(RAPOR_PDF_BACKGROUND_URL)
  } catch (error) {
    console.warn(error)
  }

  const doc = createRaporPdfDoc(detail, bgDataUrl)
  if (!doc) return

  const semesterRaw = String(detail.semesterLabel || '').toLowerCase()
  const semesterTitle = semesterRaw.includes('genap')
    ? 'Genap'
    : semesterRaw.includes('ganjil')
      ? 'Ganjil'
      : (sanitizeFileNamePart(detail.semesterLabel || '') || 'Semester')
  const tahunLabelForFile = sanitizeFileNamePart(String(detail.tahunPelajaranLabel || '').replace(/\//g, '-')) || 'Tahun'
  const cleanNama = sanitizeFileNamePart(detail.santriNama || '') || 'Santri'
  const fileName = `Rapor ${semesterTitle} ${tahunLabelForFile} - ${cleanNama}.pdf`
  doc.save(fileName)
}

function createRaporPdfDoc(detail, bgDataUrl = '') {
  if (!detail) return null
  const jsPdfApi = window.jspdf
  if (!jsPdfApi || typeof jsPdfApi.jsPDF !== 'function') return null

  const { jsPDF } = jsPdfApi
  const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: [210, 330] })
  const pageWidth = doc.internal.pageSize.getWidth()
  const pageHeight = doc.internal.pageSize.getHeight()

  if (bgDataUrl) {
    try {
      doc.addImage(bgDataUrl, 'PNG', 0, 0, pageWidth, pageHeight)
    } catch (error) {
      console.warn('Gagal menambahkan background rapor ke PDF.', error)
    }
  }

  const tableMargin = 15
  const usableWidth = pageWidth - (tableMargin * 2)
  const infoStartY = 50

  doc.setFont('times', 'normal')
  doc.setFontSize(12)
  const leftLabelX = tableMargin
  const leftColonX = tableMargin + 31
  const leftValueX = tableMargin + 35
  const rightLabelX = tableMargin + 96
  const rightColonX = tableMargin + 129
  const rightValueX = tableMargin + 133
  const rowGap = 6
  let rowY = infoStartY

  const topSeparatorY = infoStartY - 6
  doc.setDrawColor(17, 24, 39)
  doc.setLineWidth(0.8)
  doc.line(tableMargin, topSeparatorY, pageWidth - tableMargin, topSeparatorY)

  doc.text('Nama Sekolah', leftLabelX, rowY)
  doc.text(':', leftColonX, rowY)
  doc.text(String(detail.namaSekolah || '-'), leftValueX, rowY)
  doc.text('Kelas', rightLabelX, rowY)
  doc.text(':', rightColonX, rowY)
  doc.text(String(detail.kelasNama || '-'), rightValueX, rowY)

  rowY += rowGap
  doc.text('Alamat', leftLabelX, rowY)
  doc.text(':', leftColonX, rowY)
  doc.text(String(detail.alamatSekolah || '-'), leftValueX, rowY)
  doc.text('Semester', rightLabelX, rowY)
  doc.text(':', rightColonX, rowY)
  doc.text(String(detail.semesterLabel || '-'), rightValueX, rowY)

  rowY += rowGap
  doc.text('Nama Santri', leftLabelX, rowY)
  doc.text(':', leftColonX, rowY)
  doc.text(String(detail.santriNama || '-'), leftValueX, rowY)
  doc.text('Tahun Pelajaran', rightLabelX, rowY)
  doc.text(':', rightColonX, rowY)
  doc.text(String(detail.tahunPelajaranLabel || '-'), rightValueX, rowY)

  rowY += rowGap
  doc.text('Nomor Induk', leftLabelX, rowY)
  doc.text(':', leftColonX, rowY)
  doc.text(String(detail.nomorInduk || '-'), leftValueX, rowY)

  const separatorY = rowY + 4
  doc.setDrawColor(17, 24, 39)
  doc.setLineWidth(0.8)
  doc.line(tableMargin, separatorY, pageWidth - tableMargin, separatorY)

  const sectionTitleY = separatorY + 6
  const startTableY = sectionTitleY + 3
  doc.setFont('times', 'bold')
  doc.setFontSize(12)
  doc.text('D. PENGETAHUAN DAN KETERAMPILAN', tableMargin, sectionTitleY)

  const rows = Array.isArray(detail.rowsForPdf) ? detail.rowsForPdf : []
  const descTextLength = rows.reduce((acc, row) => {
    if (!row || row.type !== 'item') return acc
    return acc + String(row.deskripsiPengetahuan || '').length + String(row.deskripsiKeterampilan || '').length
  }, 0)
  const descFontSize = descTextLength > 1400 || rows.length > 14 ? 8.5 : 10
  const body = []
  rows.forEach(row => {
    if (!row || typeof row !== 'object') return
    if (row.type === 'group') {
      body.push([
        {
          content: row.label || '-',
          colSpan: 9,
          styles: {
            fontStyle: 'bold',
            halign: 'left'
          }
        }
      ])
      return
    }
    if (row.type === 'empty') {
      body.push([
        '',
        {
          content: row.text || '-',
          colSpan: 8,
          styles: {
            halign: 'left'
          }
        }
      ])
      return
    }
    body.push([
      String(row.no || ''),
      String(row.muatanPelajaran || '-'),
      String(row.kkm || '-'),
      String(row.nilaiPengetahuan || '-'),
      String(row.predikatPengetahuan || '-'),
      String(row.deskripsiPengetahuan || '-'),
      String(row.nilaiKeterampilan || '-'),
      String(row.predikatKeterampilan || '-'),
      String(row.deskripsiKeterampilan || '-')
    ])
  })

  body.push([
    '',
    'Total',
    '',
    String(detail.totalPengetahuan || '-'),
    String(detail.predikatRataPengetahuan || '-'),
    '',
    String(detail.totalKeterampilan || '-'),
    String(detail.predikatRataKeterampilan || '-'),
    ''
  ])

  if (typeof doc.autoTable !== 'function') {
    alert('Plugin tabel PDF belum termuat. Refresh halaman lalu coba lagi.')
    return null
  }

  doc.autoTable({
    startY: startTableY,
    margin: { left: tableMargin, right: tableMargin, bottom: 40 },
    tableWidth: usableWidth,
    head: [
      [
        { content: 'No', rowSpan: 2 },
        { content: 'Muatan\nPelajaran', rowSpan: 2 },
        { content: 'KKM', rowSpan: 2 },
        { content: 'Pengetahuan', colSpan: 3 },
        { content: 'Keterampilan', colSpan: 3 }
      ],
      [
        'Nilai',
        'Pred\nikat',
        'Deskripsi',
        'Nilai',
        'Pred\nikat',
        'Deskripsi'
      ]
    ],
    body,
    theme: 'grid',
    styles: {
      font: 'times',
      fontSize: 10,
      cellPadding: 1.2,
      textColor: [17, 24, 39],
      lineColor: [17, 24, 39],
      lineWidth: 0.15,
      valign: 'middle'
    },
    headStyles: {
      fillColor: [255, 255, 255],
      textColor: [17, 24, 39],
      halign: 'center',
      fontStyle: 'bold',
      valign: 'middle',
      minCellHeight: 10.5
    },
    columnStyles: {
      0: { cellWidth: 8, halign: 'center' },
      1: { cellWidth: 35 },
      2: { cellWidth: 12, halign: 'center' },
      3: { cellWidth: 10, halign: 'center' },
      4: { cellWidth: 10, halign: 'center' },
      5: { cellWidth: 42 },
      6: { cellWidth: 10, halign: 'center' },
      7: { cellWidth: 10, halign: 'center' },
      8: { cellWidth: 43 }
    },
    didParseCell: data => {
      if (data.section === 'head' && data.row.index === 0) {
        data.cell.styles.minCellHeight = 12
      }
      if (data.section === 'body' && (data.column.index === 5 || data.column.index === 8)) {
        data.cell.styles.fontSize = descFontSize
      }
    }
  })

  return doc
}

async function loadJadwalGuru() {
  const content = document.getElementById('guru-content')
  if (!content) return

  content.innerHTML = 'Loading jadwal...'

  let ctx
  try {
    ctx = await getGuruContext()
  } catch (error) {
    console.error(error)
    content.innerHTML = `<div class="placeholder-card">Gagal load jadwal: ${escapeHtml(error.message || 'Unknown error')}</div>`
    return
  }

  if (!ctx.guru?.id) {
    content.innerHTML = '<div class="placeholder-card">Data akun guru tidak ditemukan.</div>'
    return
  }

  const distribusiList = ctx.yearDistribusiList || []
  const rows = (ctx.jadwalList || [])
    .slice()
    .sort((a, b) => {
      const dayCmp = getHariOrder(a.hari) - getHariOrder(b.hari)
      if (dayCmp !== 0) return dayCmp
      return String(a.jam_mulai || '').localeCompare(String(b.jam_mulai || ''))
    })

  if (!distribusiList.length || !rows.length) {
    content.innerHTML = '<div class="placeholder-card">Belum ada jadwal pelajaran.</div>'
    return
  }

  const distribusiMap = new Map(distribusiList.map(item => [String(item.id), item]))

  const tableRows = rows.map(item => {
    const distribusi = distribusiMap.get(String(item.distribusi_id))
    const kelas = ctx.kelasMap.get(String(distribusi?.kelas_id || ''))
    const mapel = ctx.mapelMap.get(String(distribusi?.mapel_id || ''))
    const semester = ctx.semesterMap.get(String(distribusi?.semester_id || ''))

    return `
      <tr>
        <td style="padding:10px; border:1px solid #e2e8f0;">${escapeHtml(getHariLabel(item.hari))}</td>
        <td style="padding:10px; border:1px solid #e2e8f0; text-align:center;">${escapeHtml(toTimeLabel(item.jam_mulai))} - ${escapeHtml(toTimeLabel(item.jam_selesai))}</td>
        <td style="padding:10px; border:1px solid #e2e8f0;">${escapeHtml(kelas?.nama_kelas || '-')}</td>
        <td style="padding:10px; border:1px solid #e2e8f0;">${escapeHtml(getMapelLabel(mapel))}</td>
        <td style="padding:10px; border:1px solid #e2e8f0;">${escapeHtml(getSemesterLabel(semester))}</td>
      </tr>
    `
  }).join('')

  content.innerHTML = `
    <div style="font-size:14px; font-weight:600; margin-bottom:10px; color:#334155;">
      Jadwal mengajar: ${escapeHtml(ctx.guru.nama || ctx.guru.id_karyawan || '-')}
    </div>
    <div style="overflow:auto; border:1px solid #e2e8f0; border-radius:12px;">
      <table style="width:100%; border-collapse:collapse; min-width:700px; font-size:13px;">
        <thead>
          <tr style="background:#f8fafc;">
            <th style="padding:10px; border:1px solid #e2e8f0; text-align:left; width:110px;">Hari</th>
            <th style="padding:10px; border:1px solid #e2e8f0; text-align:center; width:150px;">Jam</th>
            <th style="padding:10px; border:1px solid #e2e8f0; text-align:left;">Kelas</th>
            <th style="padding:10px; border:1px solid #e2e8f0; text-align:left;">Mapel</th>
            <th style="padding:10px; border:1px solid #e2e8f0; text-align:left; width:170px;">Semester</th>
          </tr>
        </thead>
        <tbody>
          ${tableRows}
        </tbody>
      </table>
    </div>
  `
}

async function renderMapelPage() {
  const content = document.getElementById('guru-content')
  if (!content) return

  currentMapelDetailDistribusiId = ''
  currentMapelEditMode = { absensi: false, nilai: false }
  content.innerHTML = 'Loading mapel...'

  let ctx
  try {
    ctx = await getGuruContext()
  } catch (error) {
    console.error(error)
    content.innerHTML = `<div class="placeholder-card">Gagal load mapel: ${escapeHtml(error.message || 'Unknown error')}</div>`
    return
  }

  const list = ctx.yearDistribusiList || []

  const rows = list
    .map(item => {
      const kelas = ctx.kelasMap.get(String(item.kelas_id || ''))
      const mapel = ctx.mapelMap.get(String(item.mapel_id || ''))
      const semester = ctx.semesterMap.get(String(item.semester_id || ''))
      return {
        id: item.id,
        kelasNama: kelas?.nama_kelas || '-',
        mapelLabel: getMapelLabel(mapel),
        semesterNama: getSemesterLabel(semester),
        semesterActive: asBool(semester?.aktif)
      }
    })
    .sort((a, b) => {
      if (a.semesterActive !== b.semesterActive) return a.semesterActive ? -1 : 1
      const mapelCmp = a.mapelLabel.localeCompare(b.mapelLabel)
      if (mapelCmp !== 0) return mapelCmp
      return a.kelasNama.localeCompare(b.kelasNama)
    })

  let availableRows = []
  let availableKelasMap = new Map()
  let availableMapelMap = new Map()
  let availableSemesterMap = new Map()
  try {
    const availableRes = await getAvailableDistribusiForGuru()
    availableRows = availableRes.rows || []
    availableKelasMap = availableRes.kelasMap || new Map()
    availableMapelMap = availableRes.mapelMap || new Map()
    availableSemesterMap = availableRes.semesterMap || new Map()
  } catch (error) {
    console.error(error)
  }

  const availableRowsSorted = [...availableRows].sort((a, b) => {
    const mapelA = getMapelLabel(availableMapelMap.get(String(a.mapel_id || '')))
    const mapelB = getMapelLabel(availableMapelMap.get(String(b.mapel_id || '')))
    const mapelCmp = mapelA.localeCompare(mapelB)
    if (mapelCmp !== 0) return mapelCmp
    const kelasA = String(availableKelasMap.get(String(a.kelas_id || ''))?.nama_kelas || '-')
    const kelasB = String(availableKelasMap.get(String(b.kelas_id || ''))?.nama_kelas || '-')
    return kelasA.localeCompare(kelasB)
  })

  const rememberedDetail = getMapelDetailState()
  if (rememberedDetail) {
    const exists = rows.some(item => String(item.id) === rememberedDetail.distribusiId)
    if (exists) {
      await openMapelDetail(rememberedDetail.distribusiId, rememberedDetail.tab)
      return
    }
    clearMapelDetailState()
  }

  let html = ''

  html += `
    <div style="display:flex; justify-content:space-between; align-items:center; gap:8px; margin-bottom:12px; flex-wrap:wrap;">
      <div style="font-size:13px; color:#64748b;">Guru hanya bisa mengambil mapel yang belum memiliki pengajar. Penggantian pengajar tetap melalui admin.</div>
      <button type="button" class="modal-btn modal-btn-primary" onclick="toggleGuruAvailableMapelSection()">Tambah Mapel</button>
    </div>
    <div id="guru-available-mapel-section" style="display:none; margin-bottom:14px; border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">
      <div style="font-weight:700; color:#0f172a; margin-bottom:8px;">Mapel tersedia untuk diambil</div>
      ${
        availableRowsSorted.length
          ? `<div style="display:grid; grid-template-columns:repeat(auto-fit, minmax(260px, 1fr)); gap:10px;">
              ${availableRowsSorted.map(item => {
                const kelas = availableKelasMap.get(String(item.kelas_id || ''))
                const mapel = availableMapelMap.get(String(item.mapel_id || ''))
                const semester = availableSemesterMap.get(String(item.semester_id || ''))
                return `
                  <div class="mapel-card" style="border-style:dashed;">
                    <label style="display:flex; align-items:center; gap:8px; margin-bottom:8px; font-size:12px; color:#334155;">
                      <input type="checkbox" data-claim-distribusi-id="${escapeHtml(item.id)}">
                      Pilih mapel ini
                    </label>
                    <div class="mapel-card-title">${escapeHtml(getMapelLabel(mapel))}</div>
                    <div class="mapel-card-meta">Kelas: ${escapeHtml(kelas?.nama_kelas || '-')}</div>
                    <div class="mapel-card-meta">Semester: ${escapeHtml(getSemesterLabel(semester))}${asBool(semester?.aktif) ? ' (Aktif)' : ''}</div>
                  </div>
                `
              }).join('')}
            </div>`
          : '<div style="font-size:13px; color:#64748b;">Tidak ada mapel kosong untuk tahun ajaran aktif saat ini.</div>'
      }
      ${
        availableRowsSorted.length
          ? `<div style="margin-top:12px; display:flex; justify-content:flex-end; gap:8px;">
              <button type="button" class="modal-btn modal-btn-muted" onclick="clearSelectedGuruMapelClaim()">Batal Pilih</button>
              <button type="button" class="modal-btn modal-btn-primary" onclick="claimSelectedGuruMapel()">Tambahkan Terpilih</button>
            </div>`
          : ''
      }
    </div>
  `

  if (!rows.length) {
    html += '<div class="placeholder-card">Belum ada data mapel untuk guru ini.</div>'
    content.innerHTML = html
    return
  }

  html += '<div style="display:grid; grid-template-columns:repeat(auto-fit, minmax(260px, 1fr)); gap:12px;">'

  html += rows.map(item => `
    <div class="mapel-card">
      <div class="mapel-card-title">${escapeHtml(item.mapelLabel)}</div>
      <div class="mapel-card-meta">Kelas: ${escapeHtml(item.kelasNama)}</div>
      <div class="mapel-card-meta">Semester: ${escapeHtml(item.semesterNama)}${item.semesterActive ? ' (Aktif)' : ''}</div>
      <div style="margin-top:10px;">
        <button class="modal-btn modal-btn-primary" type="button" onclick="openMapelDetail('${escapeHtml(item.id)}')">Lihat Detail</button>
      </div>
    </div>
  `).join('')

  html += '</div>'

  content.innerHTML = html
}

function toggleGuruAvailableMapelSection() {
  const section = document.getElementById('guru-available-mapel-section')
  if (!section) return
  section.style.display = section.style.display === 'none' ? 'block' : 'none'
}

async function getAvailableDistribusiForGuru() {
  const activeTahunAjaran = await getActiveTahunAjaran()
  const activeTahunAjaranId = String(activeTahunAjaran?.id || '')

  const { data: distribusiRows, error: distribusiError } = await sb
    .from('distribusi_mapel')
    .select('id, kelas_id, mapel_id, guru_id, semester_id')
    .is('guru_id', null)
    .order('id', { ascending: false })

  if (distribusiError) throw distribusiError

  const rows = distribusiRows || []
  if (!rows.length) {
    return { rows: [], kelasMap: new Map(), mapelMap: new Map(), semesterMap: new Map() }
  }

  const semesterIds = [...new Set(rows.map(item => item.semester_id).filter(Boolean))]
  const semesterRes = semesterIds.length
    ? await sb.from('semester').select('id, nama, aktif, tahun_ajaran_id').in('id', semesterIds)
    : { data: [], error: null }
  if (semesterRes.error) throw semesterRes.error

  const semesterMap = new Map((semesterRes.data || []).map(item => [String(item.id), item]))
  const yearRows = activeTahunAjaranId
    ? rows.filter(item => String(semesterMap.get(String(item.semester_id || ''))?.tahun_ajaran_id || '') === activeTahunAjaranId)
    : rows

  const kelasIds = [...new Set(yearRows.map(item => item.kelas_id).filter(Boolean))]
  const mapelIds = [...new Set(yearRows.map(item => item.mapel_id).filter(Boolean))]
  const [kelasRes, mapelRes] = await Promise.all([
    kelasIds.length ? sb.from('kelas').select('id, nama_kelas').in('id', kelasIds) : Promise.resolve({ data: [], error: null }),
    getMapelRowsByIds(mapelIds)
  ])
  if (kelasRes.error) throw kelasRes.error
  if (mapelRes.error) throw mapelRes.error

  return {
    rows: yearRows,
    kelasMap: new Map((kelasRes.data || []).map(item => [String(item.id), item])),
    mapelMap: new Map((mapelRes.data || []).map(item => [String(item.id), item])),
    semesterMap
  }
}

function getSelectedGuruMapelClaimIds() {
  return [...document.querySelectorAll('[data-claim-distribusi-id]:checked')]
    .map(el => String(el.getAttribute('data-claim-distribusi-id') || '').trim())
    .filter(Boolean)
}

function clearSelectedGuruMapelClaim() {
  document.querySelectorAll('[data-claim-distribusi-id]:checked').forEach(el => {
    el.checked = false
  })
  const section = document.getElementById('guru-available-mapel-section')
  if (section) section.style.display = 'none'
}

async function claimSelectedGuruMapel() {
  const ctx = await getGuruContext()
  const guruId = String(ctx?.guru?.id || '')
  const ids = getSelectedGuruMapelClaimIds()
  if (!guruId) {
    alert('Data guru atau mapel tidak valid.')
    return
  }
  if (!ids.length) {
    alert('Pilih minimal satu mapel.')
    return
  }

  const ok = await popupConfirm(`Tambahkan ${ids.length} mapel terpilih ke daftar mengajar Anda?`)
  if (!ok) return

  const { data, error } = await sb
    .from('distribusi_mapel')
    .update({ guru_id: guruId })
    .in('id', ids)
    .is('guru_id', null)
    .select('id')

  if (error) {
    console.error(error)
    alert(`Gagal menambahkan mapel: ${error.message || 'Unknown error'}`)
    return
  }

  const updatedCount = (data || []).length
  const skippedCount = ids.length - updatedCount

  guruContextCache = null
  clearGuruPageCache('mapel')
  clearGuruPageCache('jadwal')
  await renderMapelPage()
  if (updatedCount > 0 && skippedCount > 0) {
    alert(`Berhasil menambahkan ${updatedCount} mapel. ${skippedCount} mapel gagal karena sudah diambil guru lain.`)
    return
  }
  if (updatedCount > 0) {
    alert(`Berhasil menambahkan ${updatedCount} mapel.`)
    return
  }
  alert('Tidak ada mapel yang ditambahkan. Semua mapel terpilih kemungkinan sudah diambil guru lain.')
}
async function openMapelDetail(distribusiId, tab = 'absensi') {
  const nextDistribusiId = String(distribusiId || '')
  const isSameDistribusi = nextDistribusiId && nextDistribusiId === currentMapelDetailDistribusiId
  if (!isSameDistribusi) {
    currentMapelEditMode = { absensi: false, nilai: false }
  }
  currentMapelDetailDistribusiId = nextDistribusiId
  currentMapelDetailTab = tab === 'nilai' || tab === 'rapor-desc' ? tab : 'absensi'
  saveMapelDetailState(currentMapelDetailDistribusiId, currentMapelDetailTab)
  currentMapelDetailState = null
  const content = document.getElementById('guru-content')
  if (!content || !currentMapelDetailDistribusiId) return
  content.classList.add('mapel-detail-locked')

  content.innerHTML = 'Loading detail mapel...'

  let ctx
  try {
    ctx = await getGuruContext()
  } catch (error) {
    console.error(error)
    content.innerHTML = `<div class="placeholder-card">Gagal load detail mapel: ${escapeHtml(error.message || 'Unknown error')}</div>`
    return
  }

  const distribusi = (ctx.yearDistribusiList || []).find(item => String(item.id) === currentMapelDetailDistribusiId)
  if (!distribusi) {
    content.innerHTML = '<div class="placeholder-card">Data distribusi mapel tidak ditemukan.</div>'
    return
  }

  const kelas = ctx.kelasMap.get(String(distribusi.kelas_id || ''))
  let mapel = ctx.mapelMap.get(String(distribusi.mapel_id || ''))
  if (mapel && (mapel.kkm === undefined || mapel.kkm === null || mapel.kkm === '')) {
    const mapelRefetch = await sb
      .from('mapel')
      .select('id, nama, kategori, kkm')
      .eq('id', String(distribusi.mapel_id))
      .maybeSingle()
    if (!mapelRefetch.error && mapelRefetch.data) {
      mapel = { ...mapel, ...mapelRefetch.data }
      ctx.mapelMap.set(String(distribusi.mapel_id), mapel)
    }
  }
  const semester = ctx.semesterMap.get(String(distribusi.semester_id || ''))
  const mapelKkm = toInputValue(pickLabelByKeys(mapel, ['kkm']))

  const santriList = await getSantriByKelas(distribusi.kelas_id)
  const santriMap = new Map((santriList || []).map(item => [String(item.id), item]))

  const [nilaiRes, absensiByDistribusiRes, absensiByMapelRes] = await Promise.all([
    sb
      .from('nilai_akademik')
      .select('*')
      .eq('mapel_id', distribusi.mapel_id)
      .eq('semester_id', distribusi.semester_id),
    sb
      .from(ATTENDANCE_TABLE)
      .select('*')
      .eq('distribusi_id', String(distribusi.id))
      .order('tanggal', { ascending: false }),
    sb
      .from(ATTENDANCE_TABLE)
      .select('*')
      .eq('kelas_id', String(distribusi.kelas_id))
      .eq('mapel_id', String(distribusi.mapel_id))
      .eq('semester_id', String(distribusi.semester_id))
      .order('tanggal', { ascending: false })
  ])

  let absensiErrorMessage = ''
  let absensiRows = []
  const absensiError = absensiByDistribusiRes.error || absensiByMapelRes.error
  if (absensiError) {
    const msg = String(absensiError.message || '')
    if (isMissingAbsensiTableError(absensiError)) {
      absensiErrorMessage = buildAbsensiMissingTableMessage()
    } else {
      absensiErrorMessage = `Gagal load absensi: ${msg || 'Unknown error'}`
    }
  } else {
    const mergedAbsensiRows = []
    const seenKeys = new Set()
    ;[...(absensiByDistribusiRes.data || []), ...(absensiByMapelRes.data || [])].forEach(row => {
      const rowId = String(row?.id || '').trim()
      const dedupeKey = rowId || [
        String(row?.santri_id || ''),
        String(row?.tanggal || ''),
        String(row?.mapel_id || ''),
        String(row?.kelas_id || ''),
        String(row?.semester_id || ''),
        String(row?.status || '')
      ].join('|')
      if (!dedupeKey || seenKeys.has(dedupeKey)) return
      seenKeys.add(dedupeKey)
      mergedAbsensiRows.push(row)
    })
    absensiRows = mergedAbsensiRows
  }

  const nilaiRows = nilaiRes.error ? [] : (nilaiRes.data || []).filter(row => santriMap.has(String(row.santri_id)))
  const nilaiBySantriId = new Map((nilaiRows || []).map(row => [String(row.santri_id), row]))

  const absensiRowsByKey = new Map()
  absensiRows.forEach(row => {
    const santriId = String(row.santri_id || '')
    const tanggal = String(row.tanggal || '').slice(0, 10)
    if (!santriId || !tanggal) return
    const key = `${santriId}|${tanggal}`
    if (!absensiRowsByKey.has(key)) absensiRowsByKey.set(key, [])
    absensiRowsByKey.get(key).push(row)
  })

  currentMapelDetailState = {
    distribusi,
    guruId: String(ctx.guru?.id || ''),
    nilaiBySantriId,
    absensiRowsByKey,
    santriIdList: (santriList || []).map(item => String(item.id)),
    santriMap
  }
  const editAbsensi = currentMapelEditMode.absensi === true

  const absensiDateList = [...new Set(
    absensiRows
      .map(row => String(row.tanggal || '').slice(0, 10))
      .filter(Boolean)
  )].sort((a, b) => a.localeCompare(b))

  const absensiSantriTanggalMap = new Map()
  absensiRows.forEach(row => {
    const santriId = String(row.santri_id || '')
    const tanggal = String(row.tanggal || '').slice(0, 10)
    if (!santriId || !tanggal || !santriMap.has(santriId)) return

    const rawStatus = String(row.status || '').trim()
    const status = ATTENDANCE_STATUSES.find(s => s.toLowerCase() === rawStatus.toLowerCase()) || (rawStatus || '-')
    const key = `${santriId}|${tanggal}`

    if (!absensiSantriTanggalMap.has(key)) {
      absensiSantriTanggalMap.set(key, [])
    }

    const list = absensiSantriTanggalMap.get(key)
    if (!list.includes(status)) list.push(status)
  })

  const absensiPivotRowsHtml = (santriList || [])
    .map((santri, index) => {
      const cells = absensiDateList.map(tanggal => {
        const key = `${String(santri.id)}|${tanggal}`
        const statusList = absensiSantriTanggalMap.get(key) || []
        const statusText = statusList.length ? statusList.join(', ') : '-'
        if (!editAbsensi) {
          return `<td style="padding:8px; border:1px solid #e2e8f0; text-align:center; min-width:130px;">${escapeHtml(statusText)}</td>`
        }

        const selectedStatus = statusList[0] || ''
        const options = [`<option value="">-</option>`]
          .concat(ATTENDANCE_STATUSES.map(status => `<option value="${status}" ${status === selectedStatus ? 'selected' : ''}>${status}</option>`))
          .join('')
        return `
          <td style="padding:6px; border:1px solid #e2e8f0; text-align:center; min-width:130px;">
            <select class="guru-field" style="padding:6px 8px; font-size:12px;" data-mapel-absen-santri-id="${escapeHtml(santri.id)}" data-mapel-absen-tanggal="${escapeHtml(tanggal)}">
              ${options}
            </select>
          </td>
        `
      }).join('')

      return `
        <tr>
          <td style="padding:8px; border:1px solid #e2e8f0; text-align:center; width:60px; min-width:60px; position:sticky; left:0; z-index:2; background:#fff;">${index + 1}</td>
          <td style="padding:8px; border:1px solid #e2e8f0; min-width:200px; position:sticky; left:60px; z-index:2; background:#fff;">${escapeHtml(santri.nama || '-')}</td>
          ${cells || '<td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">-</td>'}
        </tr>
      `
    }).join('')

  const absensiDateHeaderHtml = absensiDateList
    .map(tanggal => {
      if (!editAbsensi) {
        return `<th style="padding:8px; border:1px solid #e2e8f0; min-width:110px;">${escapeHtml(tanggal)}</th>`
      }
      return `<th style="padding:8px; border:1px solid #e2e8f0; min-width:120px; text-align:center;">
        <div>${escapeHtml(tanggal)}</div>
        <button type="button" class="modal-btn modal-btn-danger" data-absen-delete-date="${escapeHtml(tanggal)}" style="padding:4px 8px; font-size:11px; margin-top:4px;" onclick="deleteMapelAbsensiDate('${escapeHtml(tanggal)}', this)">Hapus</button>
      </th>`
    })
    .join('')

  const buildNilaiCellBtn = (santriId, jenis, value) => {
    const text = value === null || value === undefined || value === '' ? '-' : String(value)
    return `<button type="button" class="nilai-click-btn" onclick="openMapelNilaiDetail('${escapeHtml(santriId)}','${escapeHtml(jenis)}')">${escapeHtml(text)}</button>`
  }

  let raporDescRow = null
  let raporDescErrorText = ''
  const raporDescRes = await sb
    .from(RAPOR_DESC_TABLE)
    .select('*')
    .eq('distribusi_id', String(distribusi.id))
    .maybeSingle()
  if (raporDescRes.error) {
    if (isMissingRaporDescTableError(raporDescRes.error)) {
      raporDescErrorText = buildRaporDescMissingTableMessage()
    } else {
      raporDescErrorText = `Gagal load deskripsi rapor: ${raporDescRes.error.message || 'Unknown error'}`
    }
  } else {
    raporDescRow = raporDescRes.data || null
  }

  const nilaiRowsHtml = (santriList || [])
    .map((santri, index) => {
      const nilai = nilaiRows.find(item => String(item.santri_id) === String(santri.id)) || {}
      const nilaiPengetahuan = hitungNilaiPengetahuan({
        nilai_tugas: nilai.nilai_tugas,
        nilai_ulangan_harian: nilai.nilai_ulangan_harian,
        nilai_pts: nilai.nilai_pts,
        nilai_pas: nilai.nilai_pas,
        nilai_kehadiran: nilai.nilai_kehadiran
      })
      return `
        <tr>
          <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${index + 1}</td>
          <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(santri.nama || '-')}</td>
          <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${escapeHtml(mapelKkm)}</td>
          <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${buildNilaiCellBtn(String(santri.id), 'Tugas', nilai.nilai_tugas)}</td>
          <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${buildNilaiCellBtn(String(santri.id), 'Ulangan Harian', nilai.nilai_ulangan_harian)}</td>
          <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${buildNilaiCellBtn(String(santri.id), 'UTS', nilai.nilai_pts)}</td>
          <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${buildNilaiCellBtn(String(santri.id), 'UAS', nilai.nilai_pas)}</td>
          <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${toInputValue(nilai.nilai_kehadiran)}</td>
          <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${toInputValue(nilai.nilai_akhir ?? nilaiPengetahuan)}</td>
          <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${buildNilaiCellBtn(String(santri.id), 'Keterampilan', nilai.nilai_keterampilan)}</td>
        </tr>
      `
    }).join('')

  content.innerHTML = `
    <div class="mapel-detail-shell">
    <div style="display:flex; align-items:center; gap:10px; margin-bottom:12px;">
      <button type="button" class="mapel-back-btn" onclick="goBackToMapelList()">&lt;</button>
      <div style="font-weight:700; color:#0f172a;">${escapeHtml(getMapelLabel(mapel))} - ${escapeHtml(kelas?.nama_kelas || '-')} - ${escapeHtml(getSemesterLabel(semester))}</div>
    </div>

    <div class="mapel-detail-tabs" style="margin-bottom:12px;">
      <button type="button" class="mapel-detail-tab-btn ${currentMapelDetailTab === 'absensi' ? 'active' : ''}" data-mapel-detail-tab="absensi" onclick="setMapelDetailTab('absensi')">Absensi</button>
      <button type="button" class="mapel-detail-tab-btn ${currentMapelDetailTab === 'nilai' ? 'active' : ''}" data-mapel-detail-tab="nilai" onclick="setMapelDetailTab('nilai')">Nilai</button>
      <button type="button" class="mapel-detail-tab-btn ${currentMapelDetailTab === 'rapor-desc' ? 'active' : ''}" data-mapel-detail-tab="rapor-desc" onclick="setMapelDetailTab('rapor-desc')">Deskripsi Rapor</button>
    </div>

    <div id="mapel-detail-pane-absensi" class="mapel-detail-pane ${currentMapelDetailTab === 'absensi' ? 'active' : ''}">
      <div style="display:flex; align-items:center; justify-content:space-between; gap:10px; margin-bottom:8px; flex-wrap:wrap;">
        <div class="mapel-section-title" style="margin-bottom:0;">Absensi</div>
        <div style="display:flex; gap:8px;">
          ${editAbsensi
            ? `<button type="button" class="modal-btn" onclick="cancelMapelAbsensiEdit()">Batal Edit</button>`
            : `<button type="button" class="modal-btn" onclick="startMapelAbsensiEdit()">Edit Absensi</button>`
          }
        </div>
      </div>
      ${absensiErrorMessage
        ? `<div class="placeholder-card">${escapeHtml(absensiErrorMessage)}</div>`
        : absensiDateList.length
          ? `<div class="mapel-table-scroll mapel-absensi-table-scroll"><table style="width:max-content; min-width:100%; border-collapse:separate; border-spacing:0; font-size:13px;"><thead><tr style="background:#f8fafc;"><th style="padding:8px; border:1px solid #e2e8f0; width:60px; min-width:60px; position:sticky; left:0; z-index:5; background:#f8fafc;">No</th><th style="padding:8px; border:1px solid #e2e8f0; min-width:200px; text-align:left; position:sticky; left:60px; z-index:5; background:#f8fafc;">Nama</th>${absensiDateHeaderHtml}</tr></thead><tbody>${absensiPivotRowsHtml}</tbody></table></div>
             ${editAbsensi ? '<div style="margin-top:10px;"><button type="button" class="modal-btn modal-btn-primary" onclick="saveMapelAbsensiEdit()">Simpan Perubahan Absensi</button></div>' : ''}`
          : '<div class="placeholder-card">Belum ada data absensi.</div>'
      }
    </div>

    <div id="mapel-detail-pane-nilai" class="mapel-detail-pane ${currentMapelDetailTab === 'nilai' ? 'active' : ''}">
      <div style="display:flex; align-items:center; justify-content:space-between; gap:10px; margin-bottom:8px; flex-wrap:wrap;">
        <div class="mapel-section-title" style="margin-bottom:0;">Nilai</div>
        <div style="display:flex; align-items:center; gap:10px; flex-wrap:wrap;">
          <div style="font-size:12px; color:#0f172a; border:1px solid #cbd5e1; background:#f8fafc; border-radius:999px; padding:4px 10px; font-weight:600;">KKM: ${escapeHtml(mapelKkm)}</div>
          <div style="font-size:12px; color:#64748b;">Klik nilai untuk melihat detail input</div>
        </div>
      </div>
      <div class="mapel-table-scroll mapel-nilai-table-scroll">
        <table style="width:100%; min-width:1040px; border-collapse:collapse; font-size:13px;">
          <thead>
            <tr style="background:#f8fafc;">
              <th style="padding:8px; border:1px solid #e2e8f0; width:60px;">No</th>
              <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Nama Siswa</th>
              <th style="padding:8px; border:1px solid #e2e8f0;">KKM</th>
              <th style="padding:8px; border:1px solid #e2e8f0;">Tugas</th>
              <th style="padding:8px; border:1px solid #e2e8f0;">UH</th>
              <th style="padding:8px; border:1px solid #e2e8f0;">PTS</th>
              <th style="padding:8px; border:1px solid #e2e8f0;">PAS</th>
              <th style="padding:8px; border:1px solid #e2e8f0;">Kehadiran</th>
              <th style="padding:8px; border:1px solid #e2e8f0;">Pengetahuan</th>
              <th style="padding:8px; border:1px solid #e2e8f0;">Keterampilan</th>
            </tr>
          </thead>
          <tbody>
            ${nilaiRowsHtml || '<tr><td colspan="10" style="padding:10px; text-align:center; border:1px solid #e2e8f0;">Belum ada data siswa.</td></tr>'}
          </tbody>
        </table>
      </div>
    </div>

    <div id="mapel-detail-pane-rapor-desc" class="mapel-detail-pane ${currentMapelDetailTab === 'rapor-desc' ? 'active' : ''}">
      <div class="placeholder-card">
        <div style="font-weight:700; margin-bottom:8px;">Deskripsi Rapor Mapel (A/B/C/D)</div>
        ${raporDescErrorText
          ? `<div style="white-space:pre-wrap; color:#991b1b; font-size:12px; margin-bottom:8px;">${escapeHtml(raporDescErrorText)}</div>`
          : '<div style="font-size:12px; color:#64748b; margin-bottom:8px;">Isi deskripsi untuk tiap predikat. Rapor akan mengambil deskripsi sesuai predikat nilai santri.</div>'
        }
        <div style="display:grid; grid-template-columns:repeat(auto-fit,minmax(280px,1fr)); gap:10px;">
          <div>
            <div style="font-weight:600; margin-bottom:6px; color:#334155;">Pengetahuan</div>
            <input id="rapor-desc-a-pengetahuan" class="guru-field" type="text" placeholder="Predikat A" value="${escapeHtml(raporDescRow?.deskripsi_a_pengetahuan || '')}" style="margin-bottom:6px;">
            <input id="rapor-desc-b-pengetahuan" class="guru-field" type="text" placeholder="Predikat B" value="${escapeHtml(raporDescRow?.deskripsi_b_pengetahuan || '')}" style="margin-bottom:6px;">
            <input id="rapor-desc-c-pengetahuan" class="guru-field" type="text" placeholder="Predikat C" value="${escapeHtml(raporDescRow?.deskripsi_c_pengetahuan || '')}" style="margin-bottom:6px;">
            <input id="rapor-desc-d-pengetahuan" class="guru-field" type="text" placeholder="Predikat D" value="${escapeHtml(raporDescRow?.deskripsi_d_pengetahuan || '')}">
          </div>
          <div>
            <div style="font-weight:600; margin-bottom:6px; color:#334155;">Keterampilan</div>
            <input id="rapor-desc-a-keterampilan" class="guru-field" type="text" placeholder="Predikat A" value="${escapeHtml(raporDescRow?.deskripsi_a_keterampilan || '')}" style="margin-bottom:6px;">
            <input id="rapor-desc-b-keterampilan" class="guru-field" type="text" placeholder="Predikat B" value="${escapeHtml(raporDescRow?.deskripsi_b_keterampilan || '')}" style="margin-bottom:6px;">
            <input id="rapor-desc-c-keterampilan" class="guru-field" type="text" placeholder="Predikat C" value="${escapeHtml(raporDescRow?.deskripsi_c_keterampilan || '')}" style="margin-bottom:6px;">
            <input id="rapor-desc-d-keterampilan" class="guru-field" type="text" placeholder="Predikat D" value="${escapeHtml(raporDescRow?.deskripsi_d_keterampilan || '')}">
          </div>
        </div>
        <div style="margin-top:8px;">
          <button id="btn-save-rapor-desc" type="button" class="modal-btn modal-btn-primary" onclick="saveMapelRaporDesc('${escapeHtml(String(distribusi.id || ''))}')">Simpan Deskripsi Rapor</button>
        </div>
      </div>
    </div>
    </div>

  `
}

function setMapelDetailTab(tab) {
  const validTab = tab === 'nilai' || tab === 'rapor-desc' ? tab : 'absensi'
  currentMapelDetailTab = validTab
  saveMapelDetailState(currentMapelDetailDistribusiId, currentMapelDetailTab)
  const buttons = document.querySelectorAll('.mapel-detail-tab-btn')
  buttons.forEach(button => {
    button.classList.toggle('active', button.getAttribute('data-mapel-detail-tab') === validTab)
  })

  const panes = document.querySelectorAll('.mapel-detail-pane')
  panes.forEach(pane => pane.classList.remove('active'))
  const activePane = document.getElementById(`mapel-detail-pane-${validTab}`)
  if (activePane) activePane.classList.add('active')
}

function goBackToMapelList() {
  clearMapelDetailState()
  clearGuruPageCache('mapel')
  currentMapelDetailDistribusiId = ''
  currentMapelDetailState = null
  currentMapelEditMode = { absensi: false, nilai: false }
  loadGuruPage('mapel')
}

function startMapelAbsensiEdit() {
  currentMapelEditMode.absensi = true
  openMapelDetail(currentMapelDetailDistribusiId, 'absensi')
}

function cancelMapelAbsensiEdit() {
  currentMapelEditMode.absensi = false
  openMapelDetail(currentMapelDetailDistribusiId, 'absensi')
}

function startMapelNilaiEdit() {
  currentMapelEditMode.nilai = true
  openMapelDetail(currentMapelDetailDistribusiId, 'nilai')
}

function cancelMapelNilaiEdit() {
  currentMapelEditMode.nilai = false
  openMapelDetail(currentMapelDetailDistribusiId, 'nilai')
}

async function saveMapelAbsensiEdit() {
  const state = currentMapelDetailState
  if (!state?.distribusi) {
    alert('Detail mapel belum siap.')
    return
  }

  const selectEls = Array.from(document.querySelectorAll('[data-mapel-absen-santri-id][data-mapel-absen-tanggal]'))
  if (!selectEls.length) {
    alert('Tidak ada data absensi yang dapat disimpan.')
    return
  }

  const updates = []
  const inserts = []

  selectEls.forEach(selectEl => {
    const santriId = String(selectEl.getAttribute('data-mapel-absen-santri-id') || '').trim()
    const tanggal = String(selectEl.getAttribute('data-mapel-absen-tanggal') || '').trim()
    const status = String(selectEl.value || '').trim()
    if (!santriId || !tanggal) return

    const key = `${santriId}|${tanggal}`
    const existingRows = state.absensiRowsByKey.get(key) || []

    if (!status) return

    if (existingRows.length) {
      existingRows.forEach(row => {
        const currentStatus = String(row.status || '').trim()
        if (currentStatus === status) return
        updates.push({ id: row.id, status })
      })
      return
    }

    inserts.push({
      tanggal,
      kelas_id: String(state.distribusi.kelas_id),
      mapel_id: String(state.distribusi.mapel_id),
      guru_id: String(state.guruId),
      jam_pelajaran_id: null,
      semester_id: state.distribusi.semester_id ? String(state.distribusi.semester_id) : null,
      distribusi_id: String(state.distribusi.id),
      santri_id: santriId,
      status
    })
  })

  if (!updates.length && !inserts.length) {
    alert('Tidak ada perubahan absensi.')
    return
  }

  for (const item of updates) {
    const { error } = await sb
      .from(ATTENDANCE_TABLE)
      .update({ status: item.status })
      .eq('id', item.id)

    if (error) {
      console.error(error)
      alert(`Gagal update absensi: ${error.message || 'Unknown error'}`)
      return
    }
  }

  if (inserts.length) {
    const { error } = await sb.from(ATTENDANCE_TABLE).insert(inserts)
    if (error) {
      console.error(error)
      alert(`Gagal menambah absensi: ${error.message || 'Unknown error'}`)
      return
    }
  }

  try {
    await recalculateNilaiKehadiranFromAbsensi(
      state.distribusi,
      state.santriIdList || []
    )
  } catch (calcErr) {
    console.error(calcErr)
    alert(`Absensi diperbarui, tapi gagal update nilai kehadiran: ${calcErr.message || 'Unknown error'}`)
    return
  }

  alert('Absensi berhasil diperbarui.')
  clearGuruPageCache('mapel')
  clearGuruPageCache('laporan-absensi')
  await openMapelDetail(currentMapelDetailDistribusiId, 'absensi')
}

async function deleteMapelAbsensiDate(tanggal, triggerBtn = null) {
  const state = currentMapelDetailState
  if (!state?.absensiRowsByKey) {
    alert('Data absensi belum siap.')
    return
  }

  const tgl = String(tanggal || '').trim()
  if (!tgl) {
    alert('Tidak ada data absensi untuk dihapus.')
    return
  }

  if (!await popupConfirm(`Hapus seluruh data absensi tanggal ${tgl}?`)) return

  const targetBtn = triggerBtn || [...document.querySelectorAll('[data-absen-delete-date]')]
    .find(btn => String(btn.getAttribute('data-absen-delete-date') || '') === tgl)
  if (targetBtn?.disabled) return
  setButtonLoading(targetBtn, true, 'Menghapus...')

  try {
    const ids = []
    state.absensiRowsByKey.forEach((rows, key) => {
      if (!key.endsWith(`|${tgl}`)) return
      rows.forEach(item => {
        if (item?.id) ids.push(item.id)
      })
    })

    if (!ids.length) {
      alert('Data absensi tanggal ini tidak ditemukan.')
      return
    }

    const { error } = await sb.from(ATTENDANCE_TABLE).delete().in('id', ids)
    if (error) {
      console.error(error)
      alert(`Gagal hapus absensi: ${error.message || 'Unknown error'}`)
      return
    }

    try {
      await recalculateNilaiKehadiranFromAbsensi(
        state.distribusi,
        state.santriIdList || []
      )
    } catch (calcErr) {
      console.error(calcErr)
      alert(`Absensi dihapus, tapi gagal update nilai kehadiran: ${calcErr.message || 'Unknown error'}`)
      return
    }

    alert('Absensi berhasil dihapus.')
    await openMapelDetail(currentMapelDetailDistribusiId, 'absensi')
  } finally {
    setButtonLoading(targetBtn, false)
  }
}

async function openMapelNilaiDetail(santriId, jenis) {
  const state = currentMapelDetailState
  if (!state?.distribusi) {
    alert('Detail nilai belum siap.')
    return
  }

  const sid = String(santriId || '')
  const jenisNilai = String(jenis || '').trim()
  if (!sid || !jenisNilai) return

  const { data, error } = await sb
    .from(INPUT_NILAI_TABLE)
    .select('id, tanggal, nilai, jenis')
    .eq('santri_id', sid)
    .eq('kelas_id', state.distribusi.kelas_id)
    .eq('mapel_id', state.distribusi.mapel_id)
    .eq('semester_id', state.distribusi.semester_id)
    .eq('jenis', jenisNilai)
    .order('tanggal', { ascending: false })

  if (error) {
    console.error(error)
    if (isMissingInputNilaiTableError(error)) {
      alert(buildInputNilaiMissingTableMessage())
      return
    }
    alert(`Gagal load detail nilai: ${error.message || 'Unknown error'}`)
    return
  }

  const santriNama = state.santriMap?.get(sid)?.nama || sid
  currentNilaiDetailModalState = {
    santriId: sid,
    santriNama,
    jenis: jenisNilai,
    distribusi: state.distribusi,
    rows: (data || []).map(item => ({
      id: item.id,
      tanggal: String(item.tanggal || '').slice(0, 10),
      nilai: item.nilai === null || item.nilai === undefined ? '' : String(item.nilai)
    })),
    deletedIds: []
  }

  ensureNilaiDetailModal()
  renderNilaiDetailModalContent()
  const overlay = document.getElementById('nilai-detail-overlay')
  if (overlay) {
    overlay.classList.add('open')
    overlay.setAttribute('aria-hidden', 'false')
  }
}

function ensureNilaiDetailModal() {
  if (document.getElementById('nilai-detail-overlay')) return

  const overlay = document.createElement('div')
  overlay.id = 'nilai-detail-overlay'
  overlay.className = 'nilai-detail-overlay'
  overlay.setAttribute('aria-hidden', 'true')
  overlay.innerHTML = `
    <div class="nilai-detail-card" role="dialog" aria-modal="true" aria-labelledby="nilai-detail-title">
      <div class="nilai-detail-header">
        <div>
          <div id="nilai-detail-title" class="nilai-detail-title">Detail Nilai</div>
          <div id="nilai-detail-subtitle" class="nilai-detail-subtitle"></div>
        </div>
        <button type="button" class="modal-btn" onclick="closeNilaiDetailModal()">Tutup</button>
      </div>
      <div id="nilai-detail-body"></div>
      <div class="nilai-detail-footer">
        <button type="button" class="modal-btn" onclick="addNilaiDetailRow()">Tambah Baris</button>
        <button type="button" class="modal-btn modal-btn-primary" onclick="saveNilaiDetailChanges()">Simpan Perubahan</button>
      </div>
    </div>
  `

  overlay.addEventListener('click', event => {
    if (event.target === overlay) closeNilaiDetailModal()
  })

  document.body.appendChild(overlay)
}

function closeNilaiDetailModal() {
  const overlay = document.getElementById('nilai-detail-overlay')
  if (!overlay) return
  overlay.classList.remove('open')
  overlay.setAttribute('aria-hidden', 'true')
}

function renderNilaiDetailModalContent() {
  const state = currentNilaiDetailModalState
  const body = document.getElementById('nilai-detail-body')
  const subtitle = document.getElementById('nilai-detail-subtitle')
  if (!state || !body || !subtitle) return
  const maxValue = getJenisNilaiMax(state.jenis)

  subtitle.textContent = `${state.santriNama} - ${state.jenis}`

  const rows = state.rows || []
  const rowsHtml = rows.map((row, index) => `
    <tr>
      <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${index + 1}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">
        <input type="date" class="guru-field" style="padding:6px 8px;" value="${escapeHtml(row.tanggal || '')}" oninput="updateNilaiDetailRow(${index}, 'tanggal', this.value)">
      </td>
      <td style="padding:8px; border:1px solid #e2e8f0;">
        <input type="number" step="1" min="0" ${maxValue !== null ? `max="${maxValue}"` : ''} class="guru-field" style="padding:6px 8px; text-align:center;" value="${escapeHtml(String(row.nilai ?? ''))}" oninput="updateNilaiDetailRow(${index}, 'nilai', this.value)">
      </td>
      <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">
        <button type="button" class="modal-btn modal-btn-danger" style="padding:5px 10px; font-size:12px;" onclick="removeNilaiDetailRow(${index})">Hapus</button>
      </td>
    </tr>
  `).join('')

  const avg = calculateNilaiDetailAverage(rows)
  body.innerHTML = `
    <div style="margin-bottom:10px; font-size:13px; color:#475569;">Rata-rata saat ini: <strong>${avg === null ? '-' : avg}</strong></div>
    <div style="overflow:auto; border:1px solid #e2e8f0; border-radius:10px;">
      <table style="width:100%; min-width:520px; border-collapse:collapse; font-size:13px;">
        <thead>
          <tr style="background:#f8fafc;">
            <th style="padding:8px; border:1px solid #e2e8f0; width:60px;">No</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Tanggal</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Nilai</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:90px;">Aksi</th>
          </tr>
        </thead>
        <tbody>
          ${rowsHtml || '<tr><td colspan="4" style="padding:10px; border:1px solid #e2e8f0; text-align:center;">Belum ada data.</td></tr>'}
        </tbody>
      </table>
    </div>
  `
}

function calculateNilaiDetailAverage(rows = []) {
  const valid = rows
    .map(item => Number(item.nilai))
    .filter(num => Number.isFinite(num))
  if (!valid.length) return null
  const sum = valid.reduce((acc, num) => acc + num, 0)
  return round2(sum / valid.length)
}

function updateNilaiDetailRow(index, field, value) {
  const state = currentNilaiDetailModalState
  if (!state?.rows || !state.rows[index]) return
  state.rows[index][field] = String(value ?? '')
}

function addNilaiDetailRow() {
  const state = currentNilaiDetailModalState
  if (!state) return
  state.rows.push({
    id: null,
    tanggal: new Date().toISOString().slice(0, 10),
    nilai: ''
  })
  renderNilaiDetailModalContent()
}

function removeNilaiDetailRow(index) {
  const state = currentNilaiDetailModalState
  if (!state?.rows || !state.rows[index]) return
  const row = state.rows[index]
  if (row.id) state.deletedIds.push(row.id)
  state.rows.splice(index, 1)
  renderNilaiDetailModalContent()
}

async function saveNilaiDetailChanges() {
  const state = currentNilaiDetailModalState
  if (!state?.distribusi) {
    alert('Detail nilai belum siap.')
    return
  }
  const maxJenis = getJenisNilaiMax(state.jenis)

  const updateRows = []
  const insertRows = []

  for (const row of state.rows) {
    const tanggal = String(row.tanggal || '').trim()
    const nilai = toNullableNumber(row.nilai)
    if (!tanggal || nilai === null) continue
    if (Number.isNaN(nilai)) {
      alert('Nilai harus berupa angka valid.')
      return
    }
    if (!validateRange(nilai, `Nilai ${state.jenis}`, maxJenis)) return

    if (row.id) {
      updateRows.push({ id: row.id, tanggal, nilai: round2(nilai) })
    } else {
      insertRows.push({
        tanggal,
        kelas_id: String(state.distribusi.kelas_id),
        mapel_id: String(state.distribusi.mapel_id),
        guru_id: String(currentMapelDetailState?.guruId || ''),
        semester_id: state.distribusi.semester_id ? String(state.distribusi.semester_id) : null,
        distribusi_id: String(state.distribusi.id),
        santri_id: String(state.santriId),
        jenis: state.jenis,
        nilai: round2(nilai)
      })
    }
  }

  if (state.deletedIds.length) {
    const { error } = await sb.from(INPUT_NILAI_TABLE).delete().in('id', state.deletedIds)
    if (error) {
      console.error(error)
      alert(`Gagal hapus detail nilai: ${error.message || 'Unknown error'}`)
      return
    }
  }

  for (const row of updateRows) {
    const { error } = await sb
      .from(INPUT_NILAI_TABLE)
      .update({ tanggal: row.tanggal, nilai: row.nilai })
      .eq('id', row.id)
    if (error) {
      console.error(error)
      alert(`Gagal update detail nilai: ${error.message || 'Unknown error'}`)
      return
    }
  }

  if (insertRows.length) {
    const { error } = await sb.from(INPUT_NILAI_TABLE).insert(insertRows)
    if (error) {
      console.error(error)
      alert(`Gagal tambah detail nilai: ${error.message || 'Unknown error'}`)
      return
    }
  }

  try {
    await recalculateNilaiAkademikFromInput(state.distribusi, [String(state.santriId)])
  } catch (error) {
    console.error(error)
    alert(`Detail nilai tersimpan, tapi gagal hitung rata-rata: ${error.message || 'Unknown error'}`)
    return
  }

  alert('Detail nilai berhasil disimpan.')
  closeNilaiDetailModal()
  await openMapelDetail(currentMapelDetailDistribusiId, 'nilai')
}

function onMapelNilaiInput(santriId) {
  const sid = String(santriId || '')
  if (!sid) return

  const nilai_tugas = toNullableNumber(document.getElementById(`nilai-tugas-${sid}`)?.value || '')
  const nilai_ulangan_harian = toNullableNumber(document.getElementById(`nilai-uh-${sid}`)?.value || '')
  const nilai_pts = toNullableNumber(document.getElementById(`nilai-pts-${sid}`)?.value || '')
  const nilai_pas = toNullableNumber(document.getElementById(`nilai-pas-${sid}`)?.value || '')
  const nilai_kehadiran = toNullableNumber(document.getElementById(`nilai-kehadiran-${sid}`)?.value || '')

  if ([nilai_tugas, nilai_ulangan_harian, nilai_pts, nilai_pas, nilai_kehadiran].some(Number.isNaN)) return

  const nilaiAkhir = hitungNilaiPengetahuan({
    nilai_tugas,
    nilai_ulangan_harian,
    nilai_pts,
    nilai_pas,
    nilai_kehadiran
  })

  const akhirEl = document.getElementById(`nilai-akhir-${sid}`)
  if (akhirEl) akhirEl.value = String(nilaiAkhir)
}

async function saveMapelNilaiRow(santriId) {
  return saveMapelNilaiInternal(santriId, { notify: true, reload: true })
}

async function saveMapelNilaiInternal(santriId, opts = {}) {
  const notify = opts.notify !== false
  const reload = opts.reload !== false
  const sid = String(santriId || '')
  const state = currentMapelDetailState
  if (!sid || !state?.distribusi) {
    if (notify) alert('Detail nilai belum siap.')
    return { ok: false }
  }

  const nilai_tugas = toNullableNumber(document.getElementById(`nilai-tugas-${sid}`)?.value || '')
  const nilai_ulangan_harian = toNullableNumber(document.getElementById(`nilai-uh-${sid}`)?.value || '')
  const nilai_pts = toNullableNumber(document.getElementById(`nilai-pts-${sid}`)?.value || '')
  const nilai_pas = toNullableNumber(document.getElementById(`nilai-pas-${sid}`)?.value || '')
  const nilai_kehadiran = toNullableNumber(document.getElementById(`nilai-kehadiran-${sid}`)?.value || '')
  const nilai_keterampilan = toNullableNumber(document.getElementById(`nilai-keterampilan-${sid}`)?.value || '')

  if (!validateRange(nilai_tugas, 'Nilai Tugas', 5)) return { ok: false }
  if (!validateRange(nilai_ulangan_harian, 'Nilai Ulangan Harian', 10)) return { ok: false }
  if (!validateRange(nilai_pts, 'Nilai PTS', 25)) return { ok: false }
  if (!validateRange(nilai_pas, 'Nilai PAS', 50)) return { ok: false }
  if (!validateRange(nilai_kehadiran, 'Nilai Kehadiran', null)) return { ok: false }
  if (!validateRange(nilai_keterampilan, 'Nilai Keterampilan', null)) return { ok: false }

  const nilai_akhir = hitungNilaiPengetahuan({
    nilai_tugas,
    nilai_ulangan_harian,
    nilai_pts,
    nilai_pas,
    nilai_kehadiran
  })

  const payload = {
    santri_id: sid,
    mapel_id: String(state.distribusi.mapel_id),
    semester_id: state.distribusi.semester_id ? String(state.distribusi.semester_id) : null,
    nilai_tugas,
    nilai_ulangan_harian,
    nilai_pts,
    nilai_pas,
    nilai_kehadiran,
    nilai_akhir,
    nilai_keterampilan
  }

  const existing = state.nilaiBySantriId.get(sid)

  let error = null
  if (existing?.id) {
    const res = await sb
      .from('nilai_akademik')
      .update(payload)
      .eq('id', existing.id)
    error = res.error
  } else {
    const res = await sb
      .from('nilai_akademik')
      .insert(payload)
    error = res.error
  }

  if (error) {
    console.error(error)
    if (notify) alert(`Gagal simpan nilai: ${error.message || 'Unknown error'}`)
    return { ok: false, error }
  }

  if (notify) alert('Nilai berhasil disimpan.')
  if (reload) await openMapelDetail(currentMapelDetailDistribusiId, 'nilai')
  return { ok: true }
}

async function saveMapelNilaiBulk() {
  const state = currentMapelDetailState
  const santriIdList = state?.santriIdList || []
  if (!santriIdList.length) {
    alert('Data siswa tidak ditemukan.')
    return
  }

  for (const sid of santriIdList) {
    const result = await saveMapelNilaiInternal(sid, { notify: false, reload: false })
    if (!result?.ok) return
  }

  alert('Perubahan nilai berhasil disimpan.')
  await openMapelDetail(currentMapelDetailDistribusiId, 'nilai')
}

async function deleteMapelNilaiRow(santriId) {
  const sid = String(santriId || '')
  const state = currentMapelDetailState
  if (!sid || !state?.nilaiBySantriId) {
    alert('Data nilai belum siap.')
    return
  }

  const row = state.nilaiBySantriId.get(sid)
  if (!row?.id) {
    alert('Data nilai siswa belum ada.')
    return
  }

  if (!await popupConfirm('Hapus data nilai siswa ini?')) return

  const { error } = await sb
    .from('nilai_akademik')
    .delete()
    .eq('id', row.id)

  if (error) {
    console.error(error)
    alert(`Gagal hapus nilai: ${error.message || 'Unknown error'}`)
    return
  }

  alert('Nilai berhasil dihapus.')
  await openMapelDetail(currentMapelDetailDistribusiId, 'nilai')
}

async function saveMapelRaporDesc(distribusiId) {
  const distribusiIdText = String(distribusiId || '').trim()
  if (!distribusiIdText) {
    alert('Data distribusi belum valid.')
    return
  }

  const saveBtn = document.getElementById('btn-save-rapor-desc')
  if (saveBtn?.disabled) return
  setButtonLoading(saveBtn, true, 'Menyimpan...')

  try {
    const ctx = await getGuruContext()
    const distribusi = (ctx.yearDistribusiList || []).find(item => String(item.id) === distribusiIdText)
    if (!distribusi) {
      alert('Distribusi mapel tidak ditemukan.')
      return
    }

    const payload = {
      distribusi_id: distribusiIdText,
      guru_id: String(ctx?.guru?.id || ''),
      mapel_id: String(distribusi.mapel_id || ''),
      semester_id: distribusi.semester_id ? String(distribusi.semester_id) : null,
      deskripsi_a_pengetahuan: String(document.getElementById('rapor-desc-a-pengetahuan')?.value || '').trim() || null,
      deskripsi_b_pengetahuan: String(document.getElementById('rapor-desc-b-pengetahuan')?.value || '').trim() || null,
      deskripsi_c_pengetahuan: String(document.getElementById('rapor-desc-c-pengetahuan')?.value || '').trim() || null,
      deskripsi_d_pengetahuan: String(document.getElementById('rapor-desc-d-pengetahuan')?.value || '').trim() || null,
      deskripsi_a_keterampilan: String(document.getElementById('rapor-desc-a-keterampilan')?.value || '').trim() || null,
      deskripsi_b_keterampilan: String(document.getElementById('rapor-desc-b-keterampilan')?.value || '').trim() || null,
      deskripsi_c_keterampilan: String(document.getElementById('rapor-desc-c-keterampilan')?.value || '').trim() || null,
      deskripsi_d_keterampilan: String(document.getElementById('rapor-desc-d-keterampilan')?.value || '').trim() || null,
      updated_at: new Date().toISOString()
    }

    const { error } = await sb
      .from(RAPOR_DESC_TABLE)
      .upsert(payload, { onConflict: 'distribusi_id' })

    if (error) {
      console.error(error)
      if (isMissingRaporDescTableError(error)) {
        alert(buildRaporDescMissingTableMessage())
        return
      }
      alert(`Gagal simpan deskripsi rapor: ${error.message || 'Unknown error'}`)
      return
    }

    alert('Deskripsi rapor berhasil disimpan.')
    clearGuruPageCache('mapel')
    clearGuruPageCache('rapor')
  } finally {
    setButtonLoading(saveBtn, false)
  }
}

function getSelectedRaporSemester() {
  const semesterId = String(raporState.semesterId || '')
  return (raporState.semesterList || []).find(item => String(item.id) === semesterId) || null
}

function getRaporSemesterDisplayLabel(semester) {
  const base = String(getSemesterLabel(semester) || '-')
  const lower = base.toLowerCase()
  if (lower.includes('ganjil')) return 'I/Ganjil'
  if (lower.includes('genap')) return 'II/Genap'
  return base
}

function getMapelKompetensiGroup(mapel) {
  const raw = String(
    pickLabelByKeys(mapel, ['jenis', 'kategori', 'kelompok', 'group']) || ''
  ).trim().toLowerCase()
  if (!raw) return 'Kompetensi Khusus'
  if (raw.includes('umum')) return 'Kompetensi Umum'
  if (raw.includes('khusus')) return 'Kompetensi Khusus'
  return 'Kompetensi Khusus'
}

function getNilaiPredikat(value, kkm = null) {
  const nilai = Number(value)
  if (!Number.isFinite(nilai)) return '-'

  const kkmNum = Number(kkm)
  if (Number.isFinite(kkmNum)) {
    const safeKkm = Math.max(0, Math.min(100, Math.round(kkmNum)))
    const rentang = Math.max(0, 100 - safeKkm)
    const bMin = safeKkm + Math.ceil(rentang / 3)
    const aMin = safeKkm + Math.ceil((2 * rentang) / 3)

    if (nilai >= aMin) return 'A'
    if (nilai >= bMin) return 'B'
    if (nilai >= safeKkm) return 'C'
    return 'D'
  }

  if (nilai >= 90) return 'A'
  if (nilai >= 80) return 'B'
  if (nilai >= 70) return 'C'
  return 'D'
}

function getNilaiDeskripsi(value, kkm, label = 'Nilai') {
  const nilai = Number(value)
  if (!Number.isFinite(nilai)) return '-'
  const kkmNum = Number(kkm)
  if (Number.isFinite(kkmNum)) {
    if (nilai >= kkmNum) return `${label} tuntas (>= KKM ${kkmNum}).`
    return `${label} belum tuntas (< KKM ${kkmNum}).`
  }
  return `${label} tercatat ${round2(nilai)}.`
}

function getRaporDescFieldName(aspek = 'pengetahuan', predikat = 'D') {
  const a = String(aspek || '').trim().toLowerCase()
  const p = String(predikat || '').trim().toUpperCase()
  if (!['A', 'B', 'C', 'D'].includes(p)) return ''
  if (a !== 'pengetahuan' && a !== 'keterampilan') return ''
  return `deskripsi_${p.toLowerCase()}_${a}`
}

function pickRaporDeskripsiByPredikat(descRow, aspek, predikat) {
  if (!descRow) return ''
  const field = getRaporDescFieldName(aspek, predikat)
  if (!field) return ''
  return String(descRow[field] || '').trim()
}

async function renderRaporPage(forceReload = false) {
  const content = document.getElementById('guru-content')
  if (!content) return
  content.innerHTML = 'Loading rapor...'

  let ctx
  try {
    ctx = await getGuruContext(forceReload)
  } catch (error) {
    console.error(error)
    content.innerHTML = `<div class="placeholder-card">Gagal load rapor: ${escapeHtml(error.message || 'Unknown error')}</div>`
    return
  }

  const guru = ctx.guru
  if (!guru?.id) {
    content.innerHTML = '<div class="placeholder-card">Data guru tidak ditemukan.</div>'
    return
  }

  const tahunAktif = await getActiveTahunAjaran()
  const tahunAjaranId = String(tahunAktif?.id || '')

  let kelasQuery = sb
    .from('kelas')
    .select('id, nama_kelas, wali_kelas_id, tahun_ajaran_id')
    .eq('wali_kelas_id', guru.id)
    .order('nama_kelas')
  if (tahunAjaranId) kelasQuery = kelasQuery.eq('tahun_ajaran_id', tahunAjaranId)
  const kelasRes = await kelasQuery

  if (kelasRes.error) {
    console.error(kelasRes.error)
    content.innerHTML = `<div class="placeholder-card">Gagal load data kelas wali: ${escapeHtml(kelasRes.error.message || 'Unknown error')}</div>`
    return
  }

  const kelasList = kelasRes.data || []
  if (!kelasList.length) {
    content.innerHTML = '<div class="placeholder-card">Anda belum terdaftar sebagai wali kelas.</div>'
    return
  }

  const kelasMap = new Map(kelasList.map(item => [String(item.id), item]))
  const kelasIds = [...kelasMap.keys()]

  let santriData = []
  let santriError = null
  const santriSelectAttempts = [
    'id, nama, kelas_id, aktif, nisn, id_santri, no_induk, nomor_induk',
    'id, nama, kelas_id, aktif, nisn, no_induk, nomor_induk',
    'id, nama, kelas_id, aktif, nisn, nomor_induk',
    'id, nama, kelas_id, aktif, nisn',
    'id, nama, kelas_id, aktif'
  ]
  for (const fields of santriSelectAttempts) {
    const res = await sb
      .from('santri')
      .select(fields)
      .in('kelas_id', kelasIds)
      .eq('aktif', true)
      .order('nama')
    if (!res.error) {
      santriData = res.data || []
      santriError = null
      break
    }
    santriError = res.error
  }

  if (santriError) {
    console.error(santriError)
    content.innerHTML = `<div class="placeholder-card">Gagal load data santri: ${escapeHtml(santriError.message || 'Unknown error')}</div>`
    return
  }

  let semesterQuery = sb
    .from('semester')
    .select('id, nama, aktif, tahun_ajaran_id')
    .order('id', { ascending: true })
  if (tahunAjaranId) semesterQuery = semesterQuery.eq('tahun_ajaran_id', tahunAjaranId)
  const semesterRes = await semesterQuery
  if (semesterRes.error) {
    console.error(semesterRes.error)
    content.innerHTML = `<div class="placeholder-card">Gagal load semester: ${escapeHtml(semesterRes.error.message || 'Unknown error')}</div>`
    return
  }

  const semesterList = semesterRes.data || []
  if (!semesterList.length) {
    content.innerHTML = '<div class="placeholder-card">Data semester belum tersedia.</div>'
    return
  }

  const santriList = (santriData || []).sort((a, b) => {
    const kelasA = kelasMap.get(String(a.kelas_id || ''))?.nama_kelas || ''
    const kelasB = kelasMap.get(String(b.kelas_id || ''))?.nama_kelas || ''
    const kelasCmp = kelasA.localeCompare(kelasB)
    if (kelasCmp !== 0) return kelasCmp
    return String(a.nama || '').localeCompare(String(b.nama || ''))
  })

  const activeSemester = semesterList.find(item => asBool(item.aktif))
  const prevSemesterId = String(raporState.semesterId || '')
  const semesterId = semesterList.some(item => String(item.id) === prevSemesterId)
    ? prevSemesterId
    : String(activeSemester?.id || semesterList[0]?.id || '')

  raporState = {
    ...raporState,
    guru,
    kelasMap,
    santriList,
    semesterList,
    semesterId,
    tahunAjaranNama: String(tahunAktif?.nama || ''),
    selectedSantriId: ''
  }

  renderRaporSantriList()
}

function renderRaporSantriList() {
  const content = document.getElementById('guru-content')
  if (!content) return
  raporState.currentDetail = null

  const semesterOptions = (raporState.semesterList || [])
    .map(item => `<option value="${escapeHtml(String(item.id))}" ${String(item.id) === String(raporState.semesterId) ? 'selected' : ''}>${escapeHtml(getSemesterLabel(item))}${asBool(item.aktif) ? ' (Aktif)' : ''}</option>`)
    .join('')

  const rowsHtml = (raporState.santriList || []).map((item, index) => {
    const kelasNama = raporState.kelasMap.get(String(item.kelas_id || ''))?.nama_kelas || '-'
    return `
      <tr>
        <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${index + 1}</td>
        <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.nama || '-')}</td>
        <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(kelasNama)}</td>
        <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">
          <div style="display:flex; align-items:center; justify-content:center; gap:6px; flex-wrap:wrap;">
            <button type="button" class="modal-btn modal-btn-primary" style="padding:6px 10px; font-size:12px;" onclick="openRaporSantriDetail('${escapeHtml(String(item.id || ''))}')">Detail</button>
            <button type="button" class="modal-btn" style="padding:6px 10px; font-size:12px;" onclick="quickPrintRaporSantri('${escapeHtml(String(item.id || ''))}')">Cetak</button>
          </div>
        </td>
      </tr>
    `
  }).join('')

  content.innerHTML = `
    <div style="display:flex; align-items:end; justify-content:space-between; gap:10px; margin-bottom:12px; flex-wrap:wrap;">
      <div>
        <label class="guru-label">Semester</label>
        <select id="rapor-semester" class="guru-field" onchange="onRaporSemesterChange(this.value)">
          ${semesterOptions}
        </select>
      </div>
      <div style="font-size:13px; color:#475569;">Rapor ditampilkan berdasarkan semester yang dipilih.</div>
    </div>

    <div style="overflow:auto; border:1px solid #e2e8f0; border-radius:10px;">
      <table style="width:100%; min-width:780px; border-collapse:collapse; font-size:13px;">
        <thead>
          <tr style="background:#f8fafc;">
            <th style="padding:8px; border:1px solid #e2e8f0; width:44px;">No</th>
            <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Nama Santri</th>
            <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Kelas</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:190px;">Aksi</th>
          </tr>
        </thead>
        <tbody>
          ${rowsHtml || '<tr><td colspan="4" style="padding:10px; border:1px solid #e2e8f0; text-align:center;">Belum ada santri aktif.</td></tr>'}
        </tbody>
      </table>
    </div>
  `
}

function onRaporSemesterChange(value) {
  const semesterId = String(value || '').trim()
  raporState.semesterId = semesterId
  if (raporState.selectedSantriId) {
    openRaporSantriDetail(raporState.selectedSantriId)
    return
  }
  renderRaporSantriList()
}

async function quickPrintRaporSantri(santriId) {
  const sid = String(santriId || '').trim()
  if (!sid) return
  await openRaporSantriDetail(sid, { silent: true, render: false })
  await printRaporDetail()
}

async function openRaporSantriDetail(santriId, options = {}) {
  const content = document.getElementById('guru-content')
  const shouldRender = options?.render !== false
  const silent = options?.silent === true
  if (shouldRender && !content) return

  const sid = String(santriId || '').trim()
  const santri = (raporState.santriList || []).find(item => String(item.id) === sid)
  if (!santri) {
    alert('Data santri tidak ditemukan.')
    return
  }

  const semester = getSelectedRaporSemester()
  if (!semester?.id) {
    alert('Semester belum dipilih.')
    return
  }

  raporState.selectedSantriId = sid
  if (shouldRender && !silent) {
    content.innerHTML = 'Loading detail rapor...'
  }

  const kelas = raporState.kelasMap.get(String(santri.kelas_id || ''))
  const nomorInduk = pickLabelByKeys(santri, ['id_santri', 'no_induk', 'nomor_induk', 'nisn']) || '-'
  const semesterRaporLabel = getRaporSemesterDisplayLabel(semester)
  const tahunPelajaranLabel = String(raporState.tahunAjaranNama || pickLabelByKeys(semester, ['tahun_ajaran_nama']) || '-')
  const schoolProfile = await getSchoolProfile(false)
  const namaSekolah = String(schoolProfile?.nama_sekolah || 'Sekolah').trim() || 'Sekolah'
  const alamatSekolah = String(schoolProfile?.alamat_sekolah || '-').trim() || '-'

  const nilaiRes = await sb
    .from('nilai_akademik')
    .select('id, mapel_id, nilai_akhir, nilai_keterampilan')
    .eq('santri_id', sid)
    .eq('semester_id', String(semester.id))
    .order('id', { ascending: false })

  if (nilaiRes.error) {
    console.error(nilaiRes.error)
    if (shouldRender) {
      content.innerHTML = `<div class="placeholder-card">Gagal load nilai rapor: ${escapeHtml(nilaiRes.error.message || 'Unknown error')}</div>`
    } else {
      alert(`Gagal load nilai rapor: ${nilaiRes.error.message || 'Unknown error'}`)
    }
    return
  }

  const nilaiRowsRaw = nilaiRes.data || []
  const nilaiByMapel = new Map()
  nilaiRowsRaw.forEach(item => {
    const mapelId = String(item.mapel_id || '')
    if (!mapelId || nilaiByMapel.has(mapelId)) return
    nilaiByMapel.set(mapelId, item)
  })
  const nilaiRows = [...nilaiByMapel.values()]

  const mapelIds = [...new Set(nilaiRows.map(item => String(item.mapel_id || '')).filter(Boolean))]
  const mapelRes = await getMapelRowsByIds(mapelIds)
  const mapelMap = mapelRes.error
    ? new Map()
    : new Map((mapelRes.data || []).map(item => [String(item.id), item]))

  const distribusiMapByMapelId = new Map()
  if (mapelIds.length) {
    const distribusiRes = await sb
      .from('distribusi_mapel')
      .select('id, mapel_id, kelas_id, semester_id')
      .eq('kelas_id', String(santri.kelas_id || ''))
      .eq('semester_id', String(semester.id))
      .in('mapel_id', mapelIds)

    if (!distribusiRes.error) {
      ;(distribusiRes.data || []).forEach(item => {
        const mapelId = String(item.mapel_id || '')
        if (!mapelId || distribusiMapByMapelId.has(mapelId)) return
        distribusiMapByMapelId.set(mapelId, item)
      })
    } else {
      console.error(distribusiRes.error)
    }
  }

  const raporDescMapByDistribusiId = new Map()
  const distribusiIds = [...new Set([...distribusiMapByMapelId.values()].map(item => String(item.id || '')).filter(Boolean))]
  if (distribusiIds.length) {
    const descRes = await sb
      .from(RAPOR_DESC_TABLE)
      .select('*')
      .in('distribusi_id', distribusiIds)
    if (!descRes.error) {
      ;(descRes.data || []).forEach(item => {
        raporDescMapByDistribusiId.set(String(item.distribusi_id || ''), item)
      })
    } else if (!isMissingRaporDescTableError(descRes.error)) {
      console.error(descRes.error)
    }
  }

  const groupedRows = {
    'Kompetensi Umum': [],
    'Kompetensi Khusus': []
  }
  nilaiRows.forEach(item => {
    const mapel = mapelMap.get(String(item.mapel_id || ''))
    const group = getMapelKompetensiGroup(mapel)
    if (!groupedRows[group]) groupedRows[group] = []
    groupedRows[group].push({ item, mapel })
  })

  const buildGroupRows = (groupKey, groupLabel, startIndex = 1) => {
    const list = groupedRows[groupKey] || []
    const pdfRows = []
    if (!list.length) {
      pdfRows.push({
        type: 'group',
        label: groupLabel
      })
      pdfRows.push({
        type: 'empty',
        text: 'Belum ada mapel pada kelompok ini.'
      })
      return {
        nextIndex: startIndex,
        rowsForPdf: pdfRows,
        html: `
          <tr style="background:#f8fafc;">
            <td style="padding:8px; border:1px solid #e2e8f0; font-weight:700;" colspan="9">${escapeHtml(groupLabel)}</td>
          </tr>
          <tr>
            <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">-</td>
            <td style="padding:8px; border:1px solid #e2e8f0;" colspan="8">Belum ada mapel pada kelompok ini.</td>
          </tr>
        `
      }
    }

    let idx = startIndex
    let html = `
      <tr style="background:#f8fafc;">
        <td style="padding:8px; border:1px solid #e2e8f0; font-weight:700;" colspan="9">${escapeHtml(groupLabel)}</td>
      </tr>
    `
    pdfRows.push({
      type: 'group',
      label: groupLabel
    })
    list.forEach(row => {
      const kkm = pickLabelByKeys(row.mapel, ['kkm'])
      const nilaiPengetahuan = toNullableNumber(row.item.nilai_akhir)
      const nilaiKeterampilan = toNullableNumber(row.item.nilai_keterampilan)
      const predikatPengetahuan = getNilaiPredikat(nilaiPengetahuan, kkm)
      const predikatKeterampilan = getNilaiPredikat(nilaiKeterampilan, kkm)
      const distribusiId = String(distribusiMapByMapelId.get(String(row.item.mapel_id || ''))?.id || '')
      const descRow = raporDescMapByDistribusiId.get(distribusiId)
      const deskripsiPengetahuan = pickRaporDeskripsiByPredikat(descRow, 'pengetahuan', predikatPengetahuan) || getNilaiDeskripsi(nilaiPengetahuan, kkm, 'Pengetahuan')
      const deskripsiKeterampilan = pickRaporDeskripsiByPredikat(descRow, 'keterampilan', predikatKeterampilan) || getNilaiDeskripsi(nilaiKeterampilan, kkm, 'Keterampilan')
      pdfRows.push({
        type: 'item',
        no: idx,
        muatanPelajaran: getMapelPlainName(row.mapel),
        kkm: toInputValue(kkm),
        nilaiPengetahuan: toInputValue(nilaiPengetahuan),
        predikatPengetahuan,
        deskripsiPengetahuan,
        nilaiKeterampilan: toInputValue(nilaiKeterampilan),
        predikatKeterampilan,
        deskripsiKeterampilan
      })
      html += `
        <tr>
          <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${idx}</td>
          <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(getMapelLabel(row.mapel))}</td>
          <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${escapeHtml(toInputValue(kkm))}</td>
          <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${escapeHtml(toInputValue(nilaiPengetahuan))}</td>
          <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${escapeHtml(predikatPengetahuan)}</td>
          <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(deskripsiPengetahuan)}</td>
          <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${escapeHtml(toInputValue(nilaiKeterampilan))}</td>
          <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${escapeHtml(predikatKeterampilan)}</td>
          <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(deskripsiKeterampilan)}</td>
        </tr>
      `
      idx += 1
    })
    return { nextIndex: idx, rowsForPdf: pdfRows, html }
  }

  const umumRows = buildGroupRows('Kompetensi Umum', 'A. Kompetensi Umum', 1)
  const khususRows = buildGroupRows('Kompetensi Khusus', 'B. Kompetensi Khusus', umumRows.nextIndex)
  const pengetahuanRowsHtml = `${umumRows.html}${khususRows.html}`
  const raporRowsForPdf = [...(umumRows.rowsForPdf || []), ...(khususRows.rowsForPdf || [])]

  const pengetahuanNums = nilaiRows
    .map(item => toNullableNumber(item.nilai_akhir))
    .filter(num => Number.isFinite(num))
  const keterampilanNums = nilaiRows
    .map(item => toNullableNumber(item.nilai_keterampilan))
    .filter(num => Number.isFinite(num))
  const kkmNums = nilaiRows
    .map(item => {
      const mapel = mapelMap.get(String(item.mapel_id || ''))
      return toNullableNumber(pickLabelByKeys(mapel, ['kkm']))
    })
    .filter(num => Number.isFinite(num))

  const totalPengetahuan = pengetahuanNums.length ? round2(pengetahuanNums.reduce((a, b) => a + b, 0)) : null
  const totalKeterampilan = keterampilanNums.length ? round2(keterampilanNums.reduce((a, b) => a + b, 0)) : null
  const rataPengetahuan = pengetahuanNums.length ? round2(totalPengetahuan / pengetahuanNums.length) : null
  const rataKeterampilan = keterampilanNums.length ? round2(totalKeterampilan / keterampilanNums.length) : null
  const rataKkm = kkmNums.length ? round2(kkmNums.reduce((a, b) => a + b, 0) / kkmNums.length) : null
  const predikatRataPengetahuan = rataPengetahuan === null ? '-' : getNilaiPredikat(rataPengetahuan, rataKkm)
  const predikatRataKeterampilan = rataKeterampilan === null ? '-' : getNilaiPredikat(rataKeterampilan, rataKkm)

  raporState.currentDetail = {
    santriNama: String(santri.nama || '-'),
    kelasNama: String(kelas?.nama_kelas || '-'),
    semesterLabel: String(semesterRaporLabel || '-'),
    semesterTableLabel: String(getSemesterLabel(semester) || '-'),
    tahunPelajaranLabel: String(tahunPelajaranLabel || '-'),
    namaSekolah: String(namaSekolah || '-'),
    alamatSekolah: String(alamatSekolah || '-'),
    nomorInduk: String(nomorInduk || '-'),
    rowsForPdf: raporRowsForPdf,
    totalPengetahuan: toInputValue(totalPengetahuan),
    rataPengetahuan: toInputValue(rataPengetahuan),
    predikatRataPengetahuan: String(predikatRataPengetahuan || '-'),
    totalKeterampilan: toInputValue(totalKeterampilan),
    rataKeterampilan: toInputValue(rataKeterampilan),
    predikatRataKeterampilan: String(predikatRataKeterampilan || '-')
  }

  if (!shouldRender) return

  content.innerHTML = `
    <div style="display:flex; align-items:center; gap:10px; margin-bottom:12px;">
      <button type="button" class="mapel-back-btn" onclick="backToRaporList()">&lt;</button>
      <div style="font-weight:700; color:#0f172a;">Rapor ${escapeHtml(santri.nama || '-')} - ${escapeHtml(kelas?.nama_kelas || '-')} - ${escapeHtml(getSemesterLabel(semester))}</div>
      <button type="button" class="modal-btn modal-btn-primary" onclick="printRaporDetail()" style="margin-left:auto;">Cetak Rapor</button>
    </div>

    <div style="display:grid; grid-template-columns:repeat(auto-fit,minmax(280px,1fr)); gap:10px; margin-bottom:10px;">
      <div class="placeholder-card">
        <div><b>Nama Sekolah:</b> ${escapeHtml(namaSekolah)}</div>
        <div><b>Alamat:</b> ${escapeHtml(alamatSekolah)}</div>
        <div><b>Nama Santri:</b> ${escapeHtml(santri.nama || '-')}</div>
        <div><b>Nomor Induk:</b> ${escapeHtml(nomorInduk)}</div>
      </div>
      <div class="placeholder-card">
        <div><b>Kelas:</b> ${escapeHtml(kelas?.nama_kelas || '-')}</div>
        <div><b>Semester:</b> ${escapeHtml(semesterRaporLabel)}</div>
        <div><b>Tahun Pelajaran:</b> ${escapeHtml(tahunPelajaranLabel)}</div>
      </div>
    </div>

    <div class="placeholder-card" style="margin-bottom:10px;">
      <div style="font-weight:700; margin-bottom:8px;">A. Sikap</div>
      <div style="font-size:13px; color:#64748b;">Bagian ini akan diisi oleh Musyrif.</div>
    </div>

    <div class="placeholder-card" style="margin-bottom:10px;">
      <div style="font-weight:700; margin-bottom:8px;">B. Afektif</div>
      <div style="font-size:13px; color:#64748b;">Bagian ini akan diisi oleh Musyrif.</div>
    </div>

    <div class="placeholder-card" style="margin-bottom:10px;">
      <div style="font-weight:700; margin-bottom:8px;">C. Capaian Quran</div>
      <div style="font-size:13px; color:#64748b;">Bagian ini akan diisi oleh Muhaffiz.</div>
    </div>

    <div class="placeholder-card">
      <div style="font-weight:700; margin-bottom:8px;">D. Pengetahuan dan Keterampilan</div>
      <div style="overflow:auto; border:1px solid #e2e8f0; border-radius:10px; background:#fff;">
        <table style="width:100%; min-width:1160px; border-collapse:collapse; font-size:13px;">
          <thead>
            <tr style="background:#f8fafc;">
              <th rowspan="2" style="padding:8px; border:1px solid #e2e8f0; width:44px;">No</th>
              <th rowspan="2" style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Muatan Pelajaran</th>
              <th rowspan="2" style="padding:8px; border:1px solid #e2e8f0; width:70px;">KKM</th>
              <th colspan="3" style="padding:8px; border:1px solid #e2e8f0;">Pengetahuan</th>
              <th colspan="3" style="padding:8px; border:1px solid #e2e8f0;">Keterampilan</th>
            </tr>
            <tr style="background:#f8fafc;">
              <th style="padding:8px; border:1px solid #e2e8f0; width:80px;">Nilai</th>
              <th style="padding:8px; border:1px solid #e2e8f0; width:80px;">Predikat</th>
              <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Deskripsi</th>
              <th style="padding:8px; border:1px solid #e2e8f0; width:80px;">Nilai</th>
              <th style="padding:8px; border:1px solid #e2e8f0; width:80px;">Predikat</th>
              <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Deskripsi</th>
            </tr>
          </thead>
          <tbody>
            ${pengetahuanRowsHtml || '<tr><td colspan="9" style="padding:10px; border:1px solid #e2e8f0; text-align:center;">Belum ada nilai untuk semester ini.</td></tr>'}
          </tbody>
        </table>
      </div>
      <div style="overflow:auto; border:1px solid #e2e8f0; border-radius:10px; background:#fff; margin-top:10px;">
        <table style="width:100%; min-width:520px; border-collapse:collapse; font-size:13px;">
          <thead>
            <tr style="background:#f8fafc;">
              <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Aspek</th>
              <th style="padding:8px; border:1px solid #e2e8f0; width:120px;">Total Nilai</th>
              <th style="padding:8px; border:1px solid #e2e8f0; width:120px;">Rata-rata</th>
              <th style="padding:8px; border:1px solid #e2e8f0; width:120px;">Predikat</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td style="padding:8px; border:1px solid #e2e8f0;">Pengetahuan</td>
              <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${escapeHtml(toInputValue(totalPengetahuan))}</td>
              <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${escapeHtml(toInputValue(rataPengetahuan))}</td>
              <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${escapeHtml(predikatRataPengetahuan)}</td>
            </tr>
            <tr>
              <td style="padding:8px; border:1px solid #e2e8f0;">Keterampilan</td>
              <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${escapeHtml(toInputValue(totalKeterampilan))}</td>
              <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${escapeHtml(toInputValue(rataKeterampilan))}</td>
              <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${escapeHtml(predikatRataKeterampilan)}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

  `
}

function backToRaporList() {
  raporState.selectedSantriId = ''
  raporState.currentDetail = null
  renderRaporSantriList()
}

function getMonitoringRange(periode) {
  const range = getPeriodeRange(periode)
  if (!range) return null
  return { start: range.start, end: range.end }
}

function getWeekRangeWithinPeriod(periode) {
  const range = getPeriodeRange(periode)
  if (!range) return null
  const periodStart = new Date(`${range.start}T00:00:00`)
  const periodEnd = new Date(`${range.end}T00:00:00`)
  const now = new Date()
  const end = now < periodEnd ? now : periodEnd
  const start = new Date(end)
  start.setDate(start.getDate() - 6)
  const finalStart = start < periodStart ? periodStart : start
  return {
    start: finalStart.toISOString().slice(0, 10),
    end: end.toISOString().slice(0, 10)
  }
}

function getAbsensiDateBounds(absensiRows = []) {
  let min = ''
  let max = ''
  ;(absensiRows || []).forEach(row => {
    const tanggal = String(row?.tanggal || '').slice(0, 10)
    if (!tanggal) return
    if (!min || tanggal < min) min = tanggal
    if (!max || tanggal > max) max = tanggal
  })
  return { min, max }
}

function getMonitoringSantriRange({ mode, periode, absensiRows, semester }) {
  const selectedMode = String(mode || 'bulan')
  if (selectedMode === 'hari') {
    const day = String(monitoringState.santriDate || getDateInputToday())
    return { start: day, end: day }
  }
  if (selectedMode === 'pekan') {
    const weekRange = getRangeFromWeekInput(monitoringState.santriWeek || getWeekInputToday())
    if (weekRange) return weekRange
  }
  if (selectedMode === 'semester') {
    const byRows = getAbsensiDateBounds(absensiRows)
    if (byRows.min && byRows.max) return { start: byRows.min, end: byRows.max }
    const monthRange = getMonitoringRange(periode)
    if (monthRange) return monthRange
  }
  const month = String(monitoringState.santriMonth || periode || getMonthInputToday())
  const monthRange = getMonitoringRange(month)
  if (monthRange) return monthRange
  const fallbackDay = getDateInputToday()
  return { start: fallbackDay, end: fallbackDay }
}

function getMonitoringSantriRangeLabel(mode, range) {
  if (!range?.start || !range?.end) return '-'
  const selectedMode = String(mode || 'bulan')
  if (selectedMode === 'hari') return `Hari: ${range.start}`
  if (selectedMode === 'pekan') return `Pekan: ${range.start} s.d. ${range.end}`
  if (selectedMode === 'semester') {
    const semesterName = String((activeSemesterCache && activeSemesterCache.value?.nama) || '')
    return semesterName
      ? `Semester ${semesterName}: ${range.start} s.d. ${range.end}`
      : `Semester: ${range.start} s.d. ${range.end}`
  }
  return `Bulan: ${range.start.slice(0, 7)}`
}

function getHariFromDate(dateText) {
  const date = new Date(`${dateText}T00:00:00`)
  const names = ['minggu', 'senin', 'selasa', 'rabu', 'kamis', 'jumat', 'sabtu']
  return names[date.getDay()] || ''
}

function getDatesByDayNameInRange(startDate, endDate, dayName) {
  const target = normalizeHari(dayName)
  if (!target) return []
  const rows = []
  const cursor = new Date(`${startDate}T00:00:00`)
  const end = new Date(`${endDate}T00:00:00`)
  const toLocalDate = date => {
    const y = date.getFullYear()
    const m = String(date.getMonth() + 1).padStart(2, '0')
    const d = String(date.getDate()).padStart(2, '0')
    return `${y}-${m}-${d}`
  }
  while (cursor <= end) {
    const text = toLocalDate(cursor)
    if (normalizeHari(getHariFromDate(text)) === target) rows.push(text)
    cursor.setDate(cursor.getDate() + 1)
  }
  return rows
}

function buildSantriDailyStatus(rows) {
  const grouped = new Map()
  ;(rows || []).forEach(row => {
    const sid = String(row.santri_id || '')
    const tanggal = String(row.tanggal || '').slice(0, 10)
    if (!sid || !tanggal) return
    const key = `${sid}|${tanggal}`
    if (!grouped.has(key)) grouped.set(key, [])
    grouped.get(key).push(String(row.status || '').trim().toLowerCase())
  })

  const finalMap = new Map()
  grouped.forEach((statuses, key) => {
    const hasHadir = statuses.some(item => item === 'hadir' || item === 'terlambat')
    let status = 'alpa'
    if (hasHadir) status = 'hadir'
    else if (statuses.includes('sakit')) status = 'sakit'
    else if (statuses.includes('izin')) status = 'izin'
    else if (statuses.includes('alpa')) status = 'alpa'
    finalMap.set(key, status)
  })
  return finalMap
}

async function loadMonitoringData(periode) {
  const range = getMonitoringRange(periode)
  if (!range) throw new Error('Periode tidak valid.')

  const semester = await getActiveSemester()
  const semesterId = String(semester?.id || '')

  const getMonitoringJadwalRows = async () => {
    const withJamId = await sb.from('jadwal_pelajaran').select('id, distribusi_id, hari, jam_mulai, jam_selesai, jam_pelajaran_id')
    if (!withJamId.error) return withJamId
    const msg = String(withJamId.error?.message || '').toLowerCase()
    if (!msg.includes('jam_pelajaran_id')) return withJamId
    return await sb.from('jadwal_pelajaran').select('id, distribusi_id, hari, jam_mulai, jam_selesai')
  }

  const [distribusiRes, jadwalRes, absensiRes, kelasRes, mapelRes, karyawanRes, santriRes, jamRes] = await Promise.all([
    sb.from('distribusi_mapel').select('id, kelas_id, mapel_id, guru_id, semester_id'),
    getMonitoringJadwalRows(),
    sb.from('absensi_santri')
      .select('id, tanggal, kelas_id, mapel_id, guru_id, santri_id, status, semester_id, distribusi_id, jam_pelajaran_id, guru_pengganti_id')
      .gte('tanggal', range.start)
      .lte('tanggal', range.end),
    sb.from('kelas').select('id, nama_kelas'),
    sb.from('mapel').select('id, nama'),
    sb.from('karyawan').select('id, nama, role, aktif'),
    sb.from('santri').select('id, nama, kelas_id, aktif').eq('aktif', true),
    sb.from('jam_pelajaran').select('id, jam_mulai, jam_selesai')
  ])

  const firstError = distribusiRes.error || jadwalRes.error || absensiRes.error || kelasRes.error || mapelRes.error || karyawanRes.error || santriRes.error || jamRes.error
  if (firstError) throw firstError

  const distribusiAll = distribusiRes.data || []
  const distribusiAllMap = new Map(distribusiAll.map(item => [String(item?.id || ''), item]))
  const absensiRows = absensiRes.data || []
  const inferredSemesterIds = [...new Set(
    absensiRows
      .map(item => String(item?.semester_id || '').trim())
      .filter(Boolean)
  )]
  const inferredSemesterIdsFromDistribusi = [...new Set(
    absensiRows
      .map(item => distribusiAllMap.get(String(item?.distribusi_id || '')))
      .map(item => String(item?.semester_id || '').trim())
      .filter(Boolean)
  )]
  const mergedSemesterIds = [...new Set([
    ...inferredSemesterIds,
    ...inferredSemesterIdsFromDistribusi
  ])]
  const targetSemesterIds = mergedSemesterIds.length
    ? mergedSemesterIds
    : [semesterId].filter(Boolean)

  const distribusiList = targetSemesterIds.length
    ? distribusiAll.filter(item => targetSemesterIds.includes(String(item?.semester_id || '').trim()))
    : distribusiAll
  const distribusiMap = new Map(distribusiList.map(item => [String(item.id), item]))
  const kelasMap = new Map((kelasRes.data || []).map(item => [String(item.id), item]))
  const mapelMap = new Map((mapelRes.data || []).map(item => [String(item.id), item]))
  const karyawanMap = new Map((karyawanRes.data || []).map(item => [String(item.id), item]))
  const santriMap = new Map((santriRes.data || []).map(item => [String(item.id), item]))
  const jamMap = new Map((jamRes.data || []).map(item => [String(item.id), item]))
  const jadwalRows = (jadwalRes.data || []).filter(item => distribusiMap.has(String(item.distribusi_id || '')))
  const guruAbsensiRows = absensiRows

  const expectedSessions = []
  jadwalRows.forEach(jadwal => {
    const distribusi = distribusiMap.get(String(jadwal.distribusi_id || ''))
    if (!distribusi) return
    const jamData = jadwal.jam_pelajaran_id ? jamMap.get(String(jadwal.jam_pelajaran_id)) : null
    const jamMulai = jamData?.jam_mulai || jadwal.jam_mulai
    const jamSelesai = jamData?.jam_selesai || jadwal.jam_selesai
    const jamKey = `${String(jamMulai || '').slice(0, 5)}|${String(jamSelesai || '').slice(0, 5)}|${String(jadwal.jam_pelajaran_id || '')}`
    const dates = getDatesByDayNameInRange(range.start, range.end, jadwal.hari)
    dates.forEach(tanggal => {
      expectedSessions.push({
        tanggal,
        guru_id: String(distribusi.guru_id || ''),
        kelas_id: String(distribusi.kelas_id || ''),
        mapel_id: String(distribusi.mapel_id || ''),
        semester_id: String(distribusi.semester_id || ''),
        jam_key: jamKey,
        jam_label: `${toTimeLabel(jamMulai)}-${toTimeLabel(jamSelesai)}`
      })
    })
  })

  const exactMap = new Map()
  const genericMap = new Map()
  const guruDayMap = new Map()
  const broadMap = new Map()
  const broadNoSemMap = new Map()
  const pushToMap = (map, key, row) => {
    if (!map.has(key)) map.set(key, [])
    map.get(key).push(row)
  }
  guruAbsensiRows.forEach(row => {
    const tanggal = String(row.tanggal || '').slice(0, 10)
    const distribusi = distribusiAllMap.get(String(row.distribusi_id || '')) || null
    const kelasId = String(row.kelas_id || distribusi?.kelas_id || '')
    const mapelId = String(row.mapel_id || distribusi?.mapel_id || '')
    const guruId = String(row.guru_id || distribusi?.guru_id || '')
    const semesterRef = String(row.semester_id || distribusi?.semester_id || '')
    if (!tanggal || !kelasId || !mapelId) return
    const jamData = row.jam_pelajaran_id ? jamMap.get(String(row.jam_pelajaran_id)) : null
    const jamKey = `${String(jamData?.jam_mulai || '').slice(0, 5)}|${String(jamData?.jam_selesai || '').slice(0, 5)}|${String(row.jam_pelajaran_id || '')}`
    if (guruId) {
      pushToMap(exactMap, `${tanggal}|${kelasId}|${mapelId}|${guruId}|${jamKey}`, row)
      pushToMap(genericMap, `${tanggal}|${kelasId}|${mapelId}|${guruId}`, row)
      pushToMap(guruDayMap, `${tanggal}|${guruId}`, row)
    }
    pushToMap(broadMap, `${tanggal}|${kelasId}|${mapelId}|${semesterRef}`, row)
    pushToMap(broadNoSemMap, `${tanggal}|${kelasId}|${mapelId}`, row)
  })

  const summaryByGuru = new Map()
  const detailRows = []
  expectedSessions.forEach(item => {
    if (!item.guru_id) return
    const keyExact = `${item.tanggal}|${item.kelas_id}|${item.mapel_id}|${item.guru_id}|${item.jam_key}`
    const keyGeneric = `${item.tanggal}|${item.kelas_id}|${item.mapel_id}|${item.guru_id}`
    const keyGuruDay = `${item.tanggal}|${item.guru_id}`
    const keyBroad = `${item.tanggal}|${item.kelas_id}|${item.mapel_id}|${item.semester_id || ''}`
    const keyBroadNoSem = `${item.tanggal}|${item.kelas_id}|${item.mapel_id}`
    const rows = exactMap.get(keyExact)
      || genericMap.get(keyGeneric)
      || guruDayMap.get(keyGuruDay)
      || broadMap.get(keyBroad)
      || broadNoSemMap.get(keyBroadNoSem)
      || []
    const penggantiIds = [...new Set(rows.map(row => String(row.guru_pengganti_id || '')).filter(Boolean))]
    const status = rows.length === 0 ? 'Tidak Masuk' : (penggantiIds.length ? 'Diganti' : 'Masuk')

    const summary = summaryByGuru.get(item.guru_id) || {
      guru_id: item.guru_id,
      nama: String(karyawanMap.get(item.guru_id)?.nama || '-'),
      total: 0,
      masuk: 0,
      diganti: 0,
      tidak_masuk: 0
    }
    summary.total += 1
    if (status === 'Masuk') summary.masuk += 1
    else if (status === 'Diganti') summary.diganti += 1
    else summary.tidak_masuk += 1
    summaryByGuru.set(item.guru_id, summary)

    detailRows.push({
      guru_id: item.guru_id,
      tanggal: item.tanggal,
      kelas: String(kelasMap.get(item.kelas_id)?.nama_kelas || '-'),
      mapel: String(mapelMap.get(item.mapel_id)?.nama || '-'),
      jam: item.jam_label,
      status,
      pengganti: penggantiIds.map(id => String(karyawanMap.get(id)?.nama || id)).join(', ') || '-'
    })
  })

  const santriMode = String(monitoringState.santriMode || 'bulan')
  const santriRange = getMonitoringSantriRange({
    mode: santriMode,
    periode,
    absensiRows,
    semester
  })
  const santriFilteredRows = absensiRows.filter(row => {
    const tanggal = String(row.tanggal || '').slice(0, 10)
    if (!tanggal) return false
    return tanggal >= santriRange.start && tanggal <= santriRange.end
  })
  const dailyStatusMap = buildSantriDailyStatus(santriFilteredRows)
  const santriAggMap = new Map()
  dailyStatusMap.forEach((status, key) => {
    if (status === 'hadir') return
    const [sid] = key.split('|')
    const santri = santriMap.get(sid)
    if (!santri) return
    if (!santriAggMap.has(sid)) {
      santriAggMap.set(sid, {
        santri_id: sid,
        nama: String(santri.nama || '-'),
        kelas: String(kelasMap.get(String(santri.kelas_id || ''))?.nama_kelas || '-'),
        sakit: 0,
        izin: 0,
        alpa: 0,
        total: 0
      })
    }
    const item = santriAggMap.get(sid)
    item.total += 1
    if (status === 'sakit') item.sakit += 1
    else if (status === 'izin') item.izin += 1
    else item.alpa += 1
  })

  const santriWeeklyMap = new Map()
  dailyStatusMap.forEach((status, key) => {
    if (status === 'hadir') return
    const [sid, tanggal] = key.split('|')
    const santri = santriMap.get(sid)
    if (!santri) return
    if (!santriWeeklyMap.has(sid)) {
      santriWeeklyMap.set(sid, {
        santri_id: sid,
        nama: String(santri.nama || '-'),
        kelas: String(kelasMap.get(String(santri.kelas_id || ''))?.nama_kelas || '-'),
        kejadian: []
      })
    }
    santriWeeklyMap.get(sid).kejadian.push(`${tanggal} (${status})`)
  })

  const guruRows = Array.from(summaryByGuru.values()).sort((a, b) => a.nama.localeCompare(b.nama, undefined, { sensitivity: 'base' }))
  const santriRows = Array.from(santriAggMap.values()).sort((a, b) => {
    const kelasCompare = String(a.kelas || '').localeCompare(String(b.kelas || ''), undefined, { sensitivity: 'base', numeric: true })
    if (kelasCompare !== 0) return kelasCompare
    if (b.total !== a.total) return b.total - a.total
    return String(a.nama || '').localeCompare(String(b.nama || ''), undefined, { sensitivity: 'base' })
  })
  const santriWeeklyRows = Array.from(santriWeeklyMap.values())
    .map(item => ({ ...item, detail: item.kejadian.join(', ') }))
    .sort((a, b) => b.kejadian.length - a.kejadian.length || a.nama.localeCompare(b.nama, undefined, { sensitivity: 'base' }))

  return {
    guruRows,
    guruDetail: detailRows,
    santriRows,
    santriWeeklyRows,
    santriRangeLabel: getMonitoringSantriRangeLabel(santriMode, santriRange)
  }
}

function renderMonitoringGuruPane() {
  const box = document.getElementById('monitoring-guru-pane')
  if (!box) return
  const rows = monitoringState.guruRows || []
  if (!rows.length) {
    box.innerHTML = '<div class="placeholder-card">Belum ada data monitoring guru pada periode ini.</div>'
    return
  }

  let html = `
    <div style="overflow:auto; border:1px solid #e2e8f0; border-radius:10px;">
      <table style="width:100%; min-width:760px; border-collapse:collapse; font-size:13px;">
        <thead><tr style="background:#f8fafc;">
          <th style="padding:8px; border:1px solid #e2e8f0;">Nama Guru</th>
          <th style="padding:8px; border:1px solid #e2e8f0; width:90px;">Total</th>
          <th style="padding:8px; border:1px solid #e2e8f0; width:90px;">Masuk</th>
          <th style="padding:8px; border:1px solid #e2e8f0; width:90px;">Diganti</th>
          <th style="padding:8px; border:1px solid #e2e8f0; width:110px;">Tidak Masuk</th>
          <th style="padding:8px; border:1px solid #e2e8f0; width:140px;">Aksi</th>
        </tr></thead><tbody>
  `
  html += rows.map(item => `
    <tr>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.nama)}</td>
      <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${item.total}</td>
      <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${item.masuk}</td>
      <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${item.diganti}</td>
      <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${item.tidak_masuk}</td>
      <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">
        <button type="button" class="modal-btn modal-btn-secondary" onclick="showMonitoringGuruDetail('${escapeHtml(String(item.guru_id || ''))}')">Tampilkan Data</button>
      </td>
    </tr>
  `).join('')
  html += '</tbody></table></div>'
  box.innerHTML = html
}

function renderMonitoringSantriPane() {
  const ringkasBox = document.getElementById('monitoring-santri-summary')
  const labelEl = document.getElementById('monitoring-santri-range-label')
  const classSelect = document.getElementById('monitoring-santri-kelas-filter')
  if (!ringkasBox) return
  if (labelEl) labelEl.textContent = String(monitoringState.santriRangeLabel || '-')
  const allRows = monitoringState.santriRows || []
  const kelasOptions = [...new Set(allRows.map(item => String(item.kelas || '').trim()).filter(Boolean))]
    .sort((a, b) => a.localeCompare(b, undefined, { sensitivity: 'base', numeric: true }))
  let kelasFilter = String(monitoringState.santriKelasFilter || '')
  if (kelasFilter && !kelasOptions.includes(kelasFilter)) {
    kelasFilter = ''
    monitoringState.santriKelasFilter = ''
  }
  if (classSelect) {
    classSelect.innerHTML = '<option value="">Semua Kelas</option>' + kelasOptions
      .map(kelas => `<option value="${escapeHtml(kelas)}">${escapeHtml(kelas)}</option>`)
      .join('')
    classSelect.value = kelasFilter
  }

  const santriRows = allRows
    .filter(item => !kelasFilter || String(item.kelas || '') === kelasFilter)
    .slice()
    .sort((a, b) => {
      const kelasCompare = String(a.kelas || '').localeCompare(String(b.kelas || ''), undefined, { sensitivity: 'base', numeric: true })
      if (kelasCompare !== 0) return kelasCompare
      if (b.total !== a.total) return b.total - a.total
      return String(a.nama || '').localeCompare(String(b.nama || ''), undefined, { sensitivity: 'base' })
    })

  if (!santriRows.length) {
    ringkasBox.innerHTML = '<div class="placeholder-card">Tidak ada data santri untuk kelas ini pada rentang yang dipilih.</div>'
    return
  }
  let html = `
    <div style="overflow:auto; border:1px solid #e2e8f0; border-radius:10px;">
      <table style="width:100%; min-width:920px; border-collapse:collapse; font-size:13px;">
        <thead><tr style="background:#f8fafc;">
          <th style="padding:8px; border:1px solid #e2e8f0;">Nama Santri</th>
          <th style="padding:8px; border:1px solid #e2e8f0;">Kelas</th>
          <th style="padding:8px; border:1px solid #e2e8f0; width:90px;">Sakit</th>
          <th style="padding:8px; border:1px solid #e2e8f0; width:90px;">Izin</th>
          <th style="padding:8px; border:1px solid #e2e8f0; width:90px;">Alpa</th>
          <th style="padding:8px; border:1px solid #e2e8f0; width:90px;">Total</th>
          <th style="padding:8px; border:1px solid #e2e8f0; width:140px;">Aksi</th>
        </tr></thead><tbody>
  `
  html += santriRows.map(item => `
    <tr>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.nama)}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.kelas)}</td>
      <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${item.sakit}</td>
      <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${item.izin}</td>
      <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${item.alpa}</td>
      <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${item.total}</td>
      <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">
        <button type="button" class="modal-btn modal-btn-secondary" onclick="showMonitoringSantriDetail('${escapeHtml(String(item.santri_id || ''))}')">Tampilkan Data</button>
      </td>
    </tr>
  `).join('')
  html += '</tbody></table></div>'
  ringkasBox.innerHTML = html
}

function toggleMonitoringSantriSort() {
  onMonitoringSantriClassFilterChange()
}

function onMonitoringSantriClassFilterChange() {
  monitoringState.santriKelasFilter = String(document.getElementById('monitoring-santri-kelas-filter')?.value || '')
  renderMonitoringSantriPane()
}

function ensureMonitoringDetailPopup() {
  let overlay = document.getElementById('monitoring-detail-overlay')
  if (overlay) return overlay

  overlay = document.createElement('div')
  overlay.id = 'monitoring-detail-overlay'
  overlay.style.cssText = 'position:fixed; inset:0; background:rgba(15,23,42,0.4); display:none; align-items:center; justify-content:center; z-index:12000; padding:16px; box-sizing:border-box;'
  overlay.innerHTML = `
    <div style="width:min(1200px, calc(100vw - 32px)); max-height:calc(100vh - 32px); background:#fff; border-radius:12px; border:1px solid #e2e8f0; display:flex; flex-direction:column;">
      <div style="display:flex; align-items:center; justify-content:space-between; gap:8px; padding:12px 14px; border-bottom:1px solid #e2e8f0;">
        <div id="monitoring-detail-popup-title" style="font-size:15px; font-weight:700; color:#0f172a;">Detail Monitoring</div>
        <button type="button" class="modal-btn" onclick="closeMonitoringDetailPopup()">Tutup</button>
      </div>
      <div id="monitoring-detail-popup-body" style="padding:12px; overflow:auto;"></div>
    </div>
  `
  overlay.addEventListener('click', event => {
    if (event.target !== overlay) return
    closeMonitoringDetailPopup()
  })
  document.body.appendChild(overlay)
  return overlay
}

function closeMonitoringDetailPopup() {
  const overlay = document.getElementById('monitoring-detail-overlay')
  if (!overlay) return
  overlay.style.display = 'none'
}

function showMonitoringGuruDetail(guruId = '') {
  const selectedGuruId = String(guruId || '')
  const overlay = ensureMonitoringDetailPopup()
  const titleEl = document.getElementById('monitoring-detail-popup-title')
  const bodyEl = document.getElementById('monitoring-detail-popup-body')
  if (!overlay || !titleEl || !bodyEl) return

  let detail = (monitoringState.guruDetail || []).slice()
  if (selectedGuruId) detail = detail.filter(item => String(item.guru_id || '') === selectedGuruId)
  detail.sort((a, b) => {
    const byDate = String(a.tanggal || '').localeCompare(String(b.tanggal || ''))
    if (byDate !== 0) return byDate
    return String(a.kelas || '').localeCompare(String(b.kelas || ''))
  })

  const selectedGuruName = selectedGuruId
    ? String((monitoringState.guruRows || []).find(g => String(g.guru_id || '') === selectedGuruId)?.nama || '-')
    : 'Semua Guru'
  titleEl.textContent = `Detail Monitor Guru: ${selectedGuruName}`

  if (!detail.length) {
    bodyEl.innerHTML = '<div class="placeholder-card">Belum ada detail sesi untuk filter ini.</div>'
    overlay.style.display = 'flex'
    return
  }

  bodyEl.innerHTML = `
    <div style="overflow:auto; border:1px solid #e2e8f0; border-radius:10px;">
      <table style="width:100%; min-width:980px; border-collapse:collapse; font-size:13px;">
        <thead><tr style="background:#f8fafc;">
          <th style="padding:8px; border:1px solid #e2e8f0;">Tanggal</th>
          <th style="padding:8px; border:1px solid #e2e8f0;">Guru</th>
          <th style="padding:8px; border:1px solid #e2e8f0;">Kelas</th>
          <th style="padding:8px; border:1px solid #e2e8f0;">Mapel</th>
          <th style="padding:8px; border:1px solid #e2e8f0;">Jam</th>
          <th style="padding:8px; border:1px solid #e2e8f0;">Status</th>
          <th style="padding:8px; border:1px solid #e2e8f0;">Pengganti</th>
        </tr></thead>
        <tbody>
          ${detail.map(item => `
            <tr>
              <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.tanggal)}</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(String((monitoringState.guruRows || []).find(g => g.guru_id === item.guru_id)?.nama || '-'))}</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.kelas)}</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.mapel)}</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.jam)}</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.status)}</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.pengganti)}</td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    </div>
  `
  overlay.style.display = 'flex'
}

function showMonitoringSantriDetail(santriId = '') {
  const overlay = ensureMonitoringDetailPopup()
  const titleEl = document.getElementById('monitoring-detail-popup-title')
  const bodyEl = document.getElementById('monitoring-detail-popup-body')
  if (!overlay || !titleEl || !bodyEl) return

  const selectedSantriId = String(santriId || '')
  const allSantriRows = monitoringState.santriRows || []
  const allWeeklyRows = monitoringState.santriWeeklyRows || []
  const santriRows = selectedSantriId
    ? allSantriRows.filter(item => String(item.santri_id || '') === selectedSantriId)
    : allSantriRows
  const weeklyRows = selectedSantriId
    ? allWeeklyRows.filter(item => String(item.santri_id || '') === selectedSantriId)
    : allWeeklyRows

  const selectedSantriName = selectedSantriId
    ? String(allSantriRows.find(item => String(item.santri_id || '') === selectedSantriId)?.nama || '-')
    : 'Semua Santri'
  titleEl.textContent = `Detail Monitor Santri: ${selectedSantriName} (${String(monitoringState.santriRangeLabel || '-')})`

  const ringkasHtml = santriRows.length
    ? `
      <div style="margin-bottom:12px;">
        <div style="font-weight:700; margin-bottom:6px; color:#0f172a;">Santri Tidak Masuk di Kelas</div>
        <div style="overflow:auto; border:1px solid #e2e8f0; border-radius:10px;">
          <table style="width:100%; min-width:860px; border-collapse:collapse; font-size:13px;">
            <thead><tr style="background:#f8fafc;">
              <th style="padding:8px; border:1px solid #e2e8f0;">Nama Santri</th>
              <th style="padding:8px; border:1px solid #e2e8f0;">Kelas</th>
              <th style="padding:8px; border:1px solid #e2e8f0; width:90px;">Sakit</th>
              <th style="padding:8px; border:1px solid #e2e8f0; width:90px;">Izin</th>
              <th style="padding:8px; border:1px solid #e2e8f0; width:90px;">Alpa</th>
              <th style="padding:8px; border:1px solid #e2e8f0; width:90px;">Total</th>
            </tr></thead>
            <tbody>
              ${santriRows.map(item => `
                <tr>
                  <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.nama)}</td>
                  <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.kelas)}</td>
                  <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${item.sakit}</td>
                  <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${item.izin}</td>
                  <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${item.alpa}</td>
                  <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${item.total}</td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        </div>
      </div>
    `
    : '<div class="placeholder-card" style="margin-bottom:12px;">Tidak ada data ketidakhadiran santri pada periode ini.</div>'

  const weeklyHtml = weeklyRows.length
    ? `
      <div>
        <div style="font-weight:700; margin-bottom:6px; color:#0f172a;">Detail Ketidakhadiran Santri</div>
        <div style="overflow:auto; border:1px solid #e2e8f0; border-radius:10px;">
          <table style="width:100%; min-width:760px; border-collapse:collapse; font-size:13px;">
            <thead><tr style="background:#f8fafc;">
              <th style="padding:8px; border:1px solid #e2e8f0;">Nama Santri</th>
              <th style="padding:8px; border:1px solid #e2e8f0;">Kelas</th>
              <th style="padding:8px; border:1px solid #e2e8f0;">Detail Ketidakhadiran</th>
            </tr></thead>
            <tbody>
              ${weeklyRows.map(item => `
                <tr>
                  <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.nama)}</td>
                  <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.kelas)}</td>
                  <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.detail)}</td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        </div>
      </div>
    `
    : '<div class="placeholder-card">Tidak ada detail ketidakhadiran santri pada rentang yang dipilih.</div>'

  bodyEl.innerHTML = ringkasHtml + weeklyHtml
  overlay.style.display = 'flex'
}

function setMonitoringTab(tab) {
  const validTab = tab === 'santri' ? 'santri' : 'guru'
  monitoringState.tab = validTab
  const guruPane = document.getElementById('monitoring-pane-guru')
  const santriPane = document.getElementById('monitoring-pane-santri')
  if (guruPane) guruPane.style.display = validTab === 'guru' ? 'block' : 'none'
  if (santriPane) santriPane.style.display = validTab === 'santri' ? 'block' : 'none'
  document.querySelectorAll('.monitoring-tab-btn').forEach(btn => {
    const isActive = btn.getAttribute('data-monitoring-tab') === validTab
    btn.style.background = isActive ? '#d4d456ff' : '#fff'
    btn.style.borderColor = isActive ? '#d4d456ff' : '#cbd5e1'
    btn.style.color = '#0f172a'
  })
}

function onMonitoringSantriModeChange() {
  const mode = String(document.getElementById('monitoring-santri-mode')?.value || 'bulan')
  monitoringState.santriMode = mode
  const dayWrap = document.getElementById('monitoring-santri-day-wrap')
  const weekWrap = document.getElementById('monitoring-santri-week-wrap')
  const monthWrap = document.getElementById('monitoring-santri-month-wrap')
  if (dayWrap) dayWrap.style.display = mode === 'hari' ? '' : 'none'
  if (weekWrap) weekWrap.style.display = mode === 'pekan' ? '' : 'none'
  if (monthWrap) monthWrap.style.display = mode === 'bulan' ? '' : 'none'
}

async function reloadMonitoringData() {
  const periode = String(document.getElementById('monitoring-periode')?.value || getMonthInputToday())
  monitoringState.periode = periode
  monitoringState.santriMode = String(document.getElementById('monitoring-santri-mode')?.value || monitoringState.santriMode || 'bulan')
  monitoringState.santriDate = String(document.getElementById('monitoring-santri-date')?.value || monitoringState.santriDate || getDateInputToday())
  monitoringState.santriWeek = String(document.getElementById('monitoring-santri-week')?.value || monitoringState.santriWeek || getWeekInputToday())
  monitoringState.santriMonth = String(document.getElementById('monitoring-santri-month')?.value || monitoringState.santriMonth || periode)
  const info = document.getElementById('monitoring-loading-info')
  if (info) info.textContent = 'Memuat data monitoring...'
  try {
    const data = await loadMonitoringData(periode)
    monitoringState = { ...monitoringState, ...data }
    renderMonitoringGuruPane()
    renderMonitoringSantriPane()
    if (info) info.textContent = ''
  } catch (error) {
    console.error(error)
    if (info) info.textContent = `Gagal load monitoring: ${error.message || 'Unknown error'}`
  }
}

async function renderMonitoringPage() {
  const content = document.getElementById('guru-content')
  if (!content) return
  const periode = monitoringState.periode || getMonthInputToday()
  const santriMode = monitoringState.santriMode || 'bulan'
  const santriKelasFilter = monitoringState.santriKelasFilter || ''
  const santriDate = monitoringState.santriDate || getDateInputToday()
  const santriWeek = monitoringState.santriWeek || getWeekInputToday()
  const santriMonth = monitoringState.santriMonth || periode
  monitoringState.periode = periode
  monitoringState.santriMode = santriMode
  monitoringState.santriKelasFilter = santriKelasFilter
  monitoringState.santriDate = santriDate
  monitoringState.santriWeek = santriWeek
  monitoringState.santriMonth = santriMonth

  content.innerHTML = `
    <div style="display:grid; gap:12px;">
      <div style="border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">
        <div style="display:flex; align-items:center; justify-content:space-between; gap:8px; flex-wrap:wrap;">
          <div style="display:flex; gap:8px; align-items:center;">
            <button type="button" class="monitoring-tab-btn" data-monitoring-tab="guru" onclick="setMonitoringTab('guru')" style="border:1px solid #d4d456ff; background:#d4d456ff; border-radius:999px; padding:8px 14px; font-size:12px; font-weight:700; cursor:pointer;">Monitor Guru</button>
            <button type="button" class="monitoring-tab-btn" data-monitoring-tab="santri" onclick="setMonitoringTab('santri')" style="border:1px solid #cbd5e1; background:#fff; border-radius:999px; padding:8px 14px; font-size:12px; font-weight:700; cursor:pointer;">Monitor Santri</button>
          </div>
          <div style="display:flex; gap:8px; align-items:center;">
            <input id="monitoring-periode" class="guru-field" type="month" value="${escapeHtml(periode)}" style="max-width:180px;">
            <button type="button" class="modal-btn modal-btn-primary" onclick="reloadMonitoringData()">Refresh</button>
          </div>
        </div>
        <div id="monitoring-loading-info" style="margin-top:8px; font-size:12px; color:#64748b;"></div>
      </div>

      <div id="monitoring-pane-guru" style="display:block;">
        <div style="border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">
          <div style="font-weight:700; margin-bottom:8px; color:#0f172a;">Monitoring Kehadiran Guru per Kelas</div>
          <div id="monitoring-guru-pane">Loading...</div>
        </div>
      </div>

      <div id="monitoring-pane-santri" style="display:none;">
        <div style="border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px; margin-bottom:12px;">
          <div style="display:flex; gap:8px; align-items:center; flex-wrap:wrap;">
            <label style="font-size:12px; color:#475569;">Lihat data:</label>
            <select id="monitoring-santri-mode" class="guru-field" style="max-width:160px;" onchange="onMonitoringSantriModeChange()">
              <option value="hari" ${santriMode === 'hari' ? 'selected' : ''}>Hari</option>
              <option value="pekan" ${santriMode === 'pekan' ? 'selected' : ''}>Pekan</option>
              <option value="bulan" ${santriMode === 'bulan' ? 'selected' : ''}>Bulan</option>
              <option value="semester" ${santriMode === 'semester' ? 'selected' : ''}>Semester</option>
            </select>
            <div id="monitoring-santri-day-wrap" style="display:${santriMode === 'hari' ? '' : 'none'};">
              <input id="monitoring-santri-date" class="guru-field" type="date" value="${escapeHtml(santriDate)}" style="max-width:180px;">
            </div>
            <div id="monitoring-santri-week-wrap" style="display:${santriMode === 'pekan' ? '' : 'none'};">
              <input id="monitoring-santri-week" class="guru-field" type="week" value="${escapeHtml(santriWeek)}" style="max-width:180px;">
            </div>
            <div id="monitoring-santri-month-wrap" style="display:${santriMode === 'bulan' ? '' : 'none'};">
              <input id="monitoring-santri-month" class="guru-field" type="month" value="${escapeHtml(santriMonth)}" style="max-width:180px;">
            </div>
            <button type="button" class="modal-btn modal-btn-primary" onclick="reloadMonitoringData()">Terapkan</button>
            <select id="monitoring-santri-kelas-filter" class="guru-field" style="max-width:180px;" onchange="onMonitoringSantriClassFilterChange()">
              <option value="">Semua Kelas</option>
            </select>
            <span id="monitoring-santri-range-label" style="margin-left:auto; font-size:12px; color:#334155;"></span>
          </div>
        </div>
        <div style="border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">
          <div style="font-weight:700; margin-bottom:8px; color:#0f172a;">Ringkasan Monitor Santri</div>
          <div id="monitoring-santri-summary">Loading...</div>
        </div>
      </div>
    </div>
  `

  setMonitoringTab(monitoringState.tab || 'guru')
  onMonitoringSantriModeChange()
  await reloadMonitoringData()
}

function normalizeExamLookup(value) {
  return String(value || '').trim().toLowerCase().replace(/\s+/g, ' ')
}

function getExamPerangkatanFromClassName(kelasName) {
  const text = String(kelasName || '').trim().toLowerCase()
  if (!text) return 'SMP'
  if (text.includes('smp') || /^([789])([a-z]|\b|[-\s]|$)/i.test(text) || /\b7\b|\b8\b|\b9\b/.test(text)) return 'SMP'
  if (text.includes('sma') || text.includes('ma ') || text.endsWith(' ma') || /^(x|xi|xii)(\b|[-\s]|$)/i.test(text) || /\b10\b|\b11\b|\b12\b/.test(text)) return 'SMA'
  return 'SMP'
}

function getExamMapelBaseLabel(mapelText) {
  const raw = String(mapelText || '').trim()
  if (!raw) return ''
  return raw
    .replace(/\(\s*(SMP|SMA|Umum)\s*\)/ig, '')
    .replace(/(\s+(SMP|SMA|Umum))+$/i, '')
    .replace(/\s{2,}/g, ' ')
    .trim()
}

function parseExamMetaFromSchedule(row) {
  const raw = String(row?.keterangan || '').trim()
  if (!raw) return {}
  try {
    const parsed = JSON.parse(raw)
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch (_err) {
    return {}
  }
}

function splitExamClassTokens(value) {
  const raw = String(value || '').trim()
  if (!raw) return []
  const normalized = raw.replace(/\s+(dan|&)\s+/ig, ',')
  return [...new Set(
    normalized
      .split(/[;,/|]+/)
      .map(item => String(item || '').trim())
      .filter(Boolean)
  )]
}

function getExamRowClassLabel(row) {
  const meta = parseExamMetaFromSchedule(row)
  const classRows = Array.isArray(meta?.class_rows) ? meta.class_rows : []
  const kelasNames = [...new Set(classRows.map(item => String(item?.kelas_nama || '').trim()).filter(Boolean))]
  if (kelasNames.length) return kelasNames.join(', ')
  return String(row?.kelas || '-')
}

function getExamRowClassList(row, fallbackClassNames = []) {
  const meta = parseExamMetaFromSchedule(row)
  const classRows = Array.isArray(meta?.class_rows) ? meta.class_rows : []
  const kelasNames = [...new Set(classRows.map(item => String(item?.kelas_nama || '').trim()).filter(Boolean))]
  if (kelasNames.length) return kelasNames

  const altMetaList = []
    .concat(Array.isArray(meta?.kelas_list) ? meta.kelas_list : [])
    .concat(Array.isArray(meta?.kelas_rows) ? meta.kelas_rows.map(item => item?.kelas_nama || item?.kelas || '') : [])
    .concat(Array.isArray(meta?.classes) ? meta.classes : [])
  const metaClasses = [...new Set(altMetaList.map(item => String(item || '').trim()).filter(Boolean))]
  if (metaClasses.length) return metaClasses

  const fallbackFromDistribusi = [...new Set(
    (Array.isArray(fallbackClassNames) ? fallbackClassNames : [])
      .map(item => String(item || '').trim())
      .filter(Boolean)
  )]
  if (fallbackFromDistribusi.length) return fallbackFromDistribusi

  const fallback = String(row?.kelas || '').trim()
  const splitFallback = splitExamClassTokens(fallback)
  return splitFallback.length ? splitFallback : (fallback ? [fallback] : [])
}

function getExamRowMapelLabel(row) {
  const meta = parseExamMetaFromSchedule(row)
  const mapelRaw = String(meta?.mapel_nama || '').trim() || String(row?.mapel || '').trim()
  const mapelBase = getExamMapelBaseLabel(mapelRaw)
  return mapelBase || '-'
}

function isExamTableMissingError(error) {
  const msg = String(error?.message || '').toLowerCase()
  const code = String(error?.code || '').toUpperCase()
  return code === '42P01' ||
    msg.includes('does not exist') ||
    msg.includes('relation') ||
    msg.includes('could not find the table') ||
    msg.includes(`public.${EXAM_SCHEDULE_TABLE}`) ||
    msg.includes(`public.${EXAM_QUESTION_TABLE}`)
}

function isExamColumnMissingError(error) {
  const msg = String(error?.message || '').toLowerCase()
  return msg.includes('column') || msg.includes('schema cache')
}

function buildExamScheduleMissingTableMessage() {
  return `Tabel '${EXAM_SCHEDULE_TABLE}' belum ada.\n\nJalankan SQL:\ncreate table if not exists public.${EXAM_SCHEDULE_TABLE} (\n  id uuid primary key default gen_random_uuid(),\n  jenis text not null,\n  nama text not null,\n  kelas text not null,\n  mapel text not null,\n  tanggal date not null,\n  jam_mulai time,\n  jam_selesai time,\n  lokasi text,\n  keterangan text,\n  created_at timestamptz not null default now(),\n  updated_at timestamptz not null default now()\n);`
}

function buildExamQuestionMissingTableMessage() {
  return `Tabel '${EXAM_QUESTION_TABLE}' belum ada.\n\nJalankan SQL:\ncreate table if not exists public.${EXAM_QUESTION_TABLE} (\n  id uuid primary key default gen_random_uuid(),\n  jadwal_id uuid not null,\n  guru_id text not null,\n  guru_nama text,\n  bentuk_soal text,\n  jumlah_nomor integer,\n  instruksi text,\n  questions_json text,\n  status text not null default 'draft',\n  created_at timestamptz not null default now(),\n  updated_at timestamptz not null default now()\n);\n\ncreate unique index if not exists soal_ujian_jadwal_guru_uidx on public.${EXAM_QUESTION_TABLE}(jadwal_id, guru_id);`
}

function parseExamQuestions(value) {
  if (Array.isArray(value)) return value
  const raw = String(value || '').trim()
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : []
  } catch (_err) {
    return []
  }
}

async function ensureExamArabicFontLoaded() {
  if (examArabicFontBase64) return true
  if (examArabicFontLoadPromise) return examArabicFontLoadPromise
  examArabicFontLoadPromise = (async () => {
    try {
      const loadFile = async (filename, required = true) => {
        const candidates = [
          String(filename || '').trim(),
          `./${String(filename || '').trim()}`,
          `../${String(filename || '').trim()}`,
          `/${String(filename || '').trim()}`
        ].filter(Boolean)
        let lastErr = null
        for (const candidate of candidates) {
          try {
            const res = await fetch(encodeURI(candidate), { cache: 'no-cache' })
            if (!res.ok) throw new Error(`HTTP ${res.status}`)
            const buf = await res.arrayBuffer()
            const bytes = new Uint8Array(buf)
            let binary = ''
            const chunkSize = 0x8000
            for (let i = 0; i < bytes.length; i += chunkSize) {
              binary += String.fromCharCode(...bytes.subarray(i, i + chunkSize))
            }
            return btoa(binary)
          } catch (err) {
            lastErr = err
          }
        }
        if (!required) return ''
        throw (lastErr || new Error(`Gagal memuat font: ${String(filename || '-')}`))
      }
      examArabicFontBase64 = await loadFile(EXAM_ARABIC_FONT_FILE, true)
      examArabicFontBoldBase64 = await loadFile(EXAM_ARABIC_FONT_BOLD_FILE, false)
      return Boolean(examArabicFontBase64)
    } catch (err) {
      console.warn('Gagal memuat font Arab untuk PDF.', err)
      examArabicFontBase64 = ''
      examArabicFontBoldBase64 = ''
      return false
    } finally {
      examArabicFontLoadPromise = null
    }
  })()
  return examArabicFontLoadPromise
}

async function ensureExamPrintBackgroundLoaded() {
  if (examPrintBackgroundDataUrl) return examPrintBackgroundDataUrl
  if (examPrintBackgroundLoadPromise) return examPrintBackgroundLoadPromise
  examPrintBackgroundLoadPromise = (async () => {
    try {
      const candidates = [EXAM_PRINT_BACKGROUND_URL, 'Ujian.png', 'bg ujian.png']
      let dataUrl = ''
      for (const candidate of candidates) {
        try {
          dataUrl = await loadPdfBackgroundDataUrl(candidate)
          if (dataUrl) break
        } catch (_err) {}
      }
      examPrintBackgroundDataUrl = dataUrl
      return examPrintBackgroundDataUrl
    } catch (err) {
      console.warn('Gagal memuat background ujian.', err)
      examPrintBackgroundDataUrl = ''
      return ''
    } finally {
      examPrintBackgroundLoadPromise = null
    }
  })()
  return examPrintBackgroundLoadPromise
}

function parseExamInstruksiMeta(value) {
  const raw = String(value || '')
  const marker = raw.match(/^\[\[LANG:(AR|ID)\]\]\s*\n?/i)
  const lang = marker ? String(marker[1] || 'ID').toUpperCase() : 'ID'
  const text = marker ? raw.slice(marker[0].length) : raw
  return {
    lang: lang === 'AR' ? 'AR' : 'ID',
    text: String(text || '').trim()
  }
}

function buildExamInstruksiWithMeta(lang, text) {
  const safeLang = String(lang || 'ID').toUpperCase() === 'AR' ? 'AR' : 'ID'
  const body = String(text || '').trim()
  if (!body && safeLang === 'ID') return null
  if (!body) return `[[LANG:${safeLang}]]`
  return `[[LANG:${safeLang}]]\n${body}`
}

function getExamPdfStaticText(langCode) {
  const lang = String(langCode || 'ID').toUpperCase()
  if (lang === 'AR') {
    return {
      title: 'أسئلة الامتحان',
      jenis: 'النوع',
      namaUjian: 'اسم الاختبار',
      kelasMapel: 'الصف',
      mapel: 'المادة',
      tanggalWaktu: 'التاريخ',
      waktu: 'الوقت',
      guru: 'المعلم',
      instruksiUmum: 'تعليمات عامة',
      modelSoal: 'نموذج الأسئلة'
    }
  }
  return {
    title: 'SOAL UJIAN',
    jenis: 'Jenis',
    namaUjian: 'Nama Ujian',
    kelasMapel: 'Kelas',
    mapel: 'Mapel',
    tanggalWaktu: 'Tanggal',
    waktu: 'Waktu',
    guru: 'Guru',
    instruksiUmum: 'Instruksi Umum',
    modelSoal: 'Model Soal'
  }
}

function toArabicIndicDigits(value) {
  const map = ['٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩']
  return String(value == null ? '' : value).replace(/\d/g, d => map[Number(d)] || d)
}

function formatExamNumber(value, langCode = 'ID') {
  const lang = String(langCode || 'ID').toUpperCase()
  return lang === 'AR' ? toArabicIndicDigits(value) : String(value)
}

function getExamMarkerSeparator(langCode = 'ID') {
  return '.'
}

function formatExamMarker(token, langCode = 'ID') {
  const lang = String(langCode || 'ID').toUpperCase()
  const body = String(token || '')
  return lang === 'AR' ? `.${body}` : `${body}.`
}

function getArabicLetterByIndex(index) {
  const letters = ['أ', 'ب', 'ج', 'د', 'هـ', 'و', 'ز', 'ح', 'ط', 'ي', 'ك', 'ل', 'م', 'ن', 'س', 'ع', 'ف', 'ص', 'ق', 'ر', 'ش', 'ت', 'ث', 'خ', 'ذ', 'ض']
  return letters[Number(index || 0) % letters.length]
}

function buildExamPrintSections(questions, fallbackType = 'pilihan-ganda') {
  const rows = Array.isArray(questions) ? questions : []
  const sections = []
  let currentType = ''
  let currentItems = []
  rows.forEach((item, idx) => {
    const qType = normalizeExamQuestionType(item?.type, fallbackType)
    const numbered = {
      ...item,
      no: Number(item?.no || (idx + 1))
    }
    if (!currentType) {
      currentType = qType
      currentItems.push(numbered)
      return
    }
    if (qType !== currentType) {
      sections.push({ type: currentType, items: currentItems })
      currentType = qType
      currentItems = [numbered]
      return
    }
    currentItems.push(numbered)
  })
  if (currentItems.length) sections.push({ type: currentType || 'pilihan-ganda', items: currentItems })
  return sections
}

function getExamPrintTypeTitle(type, index, langCode = 'ID') {
  const parts = getExamPrintTypeParts(type, index, langCode)
  return `${parts.marker} ${parts.label}`
}

function getExamPrintTypeParts(type, index, langCode = 'ID') {
  const lang = String(langCode || 'ID').toUpperCase()
  let label = 'Pilihan Ganda'
  if (lang === 'AR') {
    if (type === 'esai') label = 'مقال'
    else if (type === 'pasangkan-kata') label = 'وصل الكلمات'
    else if (type === 'isi-titik') label = 'املأ الفراغ'
    else label = 'اختيار من متعدد'
  } else {
    if (type === 'esai') label = 'Esai'
    else if (type === 'pasangkan-kata') label = 'Pasangkan Kata'
    else if (type === 'isi-titik') label = 'Isi Titik Kosong'
  }
  const code = lang === 'AR' ? getArabicLetterByIndex(index) : String.fromCharCode(65 + (Number(index || 0) % 26))
  return {
    marker: formatExamMarker(code, lang),
    label
  }
}

function getExamPrintTypeInstruction(type, langCode = 'ID') {
  const lang = String(langCode || 'ID').toUpperCase()
  if (lang === 'AR') {
    if (type === 'esai') return 'أجب عن الأسئلة التالية بوضوح وصحة.'
    if (type === 'pasangkan-kata') return 'صِل كلمات العمود (أ) بما يناسبها في العمود (ب).'
    if (type === 'isi-titik') return 'أكمل الفراغ بالكلمة المناسبة من الكلمات المعطاة.'
    return 'اختر إجابة واحدة صحيحة.'
  }
  if (type === 'esai') return 'Jawablah soal berikut dengan jelas dan benar.'
  if (type === 'pasangkan-kata') return 'Pasangkan kata pada baris A dengan pasangan yang tepat pada baris B.'
  if (type === 'isi-titik') return 'Lengkapi bagian yang kosong dengan penggalan kata yang disediakan.'
  return 'Pilihlah satu jawaban yang paling tepat.'
}

function normalizeExamQuestionType(value, fallbackType = '') {
  const raw = String(value || '').trim().toLowerCase()
  if (raw === 'esai' || raw === 'essay') return 'esai'
  if (raw === 'pilihan-ganda' || raw === 'pilihan ganda' || raw === 'pg') return 'pilihan-ganda'
  if (raw === 'pasangkan-kata' || raw === 'pasangkan kata' || raw === 'matching') return 'pasangkan-kata'
  if (raw === 'isi-titik' || raw === 'isi titik' || raw === 'fill-blank' || raw === 'fill blank') return 'isi-titik'
  const fallback = String(fallbackType || '').trim().toLowerCase()
  if (fallback === 'esai' || fallback === 'essay') return 'esai'
  if (fallback === 'pasangkan-kata' || fallback === 'pasangkan kata' || fallback === 'matching') return 'pasangkan-kata'
  if (fallback === 'isi-titik' || fallback === 'isi titik' || fallback === 'fill-blank' || fallback === 'fill blank') return 'isi-titik'
  return 'pilihan-ganda'
}

function deriveExamSectionsFromQuestions(questions, fallbackType = 'pilihan-ganda', totalCount = 0) {
  const rows = Array.isArray(questions) ? questions : []
  let maxNo = 0
  rows.forEach((item, idx) => {
    const no = Number(item?.no || (idx + 1))
    if (Number.isFinite(no) && no > maxNo) maxNo = no
  })
  const safeCount = Number.isFinite(totalCount) ? Math.max(1, Math.min(200, Math.round(totalCount))) : Math.max(1, maxNo || 1)
  const typeMap = new Array(safeCount + 1).fill(normalizeExamQuestionType('', fallbackType))
  rows.forEach((item, idx) => {
    const no = Number(item?.no || (idx + 1))
    if (!Number.isFinite(no) || no <= 0 || no > safeCount) return
    typeMap[no] = normalizeExamQuestionType(item?.type, fallbackType)
  })
  const sections = []
  let start = 1
  let current = typeMap[1]
  for (let i = 2; i <= safeCount; i += 1) {
    if (typeMap[i] === current) continue
    const segmentItems = rows.filter((item, idx) => {
      const no = Number(item?.no || (idx + 1))
      return no >= start && no <= (i - 1)
    })
    const fragSet = new Set()
    segmentItems.forEach(item => {
      const frags = Array.isArray(item?.fragments) ? item.fragments : []
      frags.forEach(f => {
        const txt = String(f || '').trim()
        if (txt) fragSet.add(txt)
      })
    })
    sections.push({ type: current, start, end: i - 1, wordPool: [...fragSet].join(', '), blankCount: (i - 1) - start + 1 })
    start = i
    current = typeMap[i]
  }
  {
    const segmentItems = rows.filter((item, idx) => {
      const no = Number(item?.no || (idx + 1))
      return no >= start && no <= safeCount
    })
    const fragSet = new Set()
    segmentItems.forEach(item => {
      const frags = Array.isArray(item?.fragments) ? item.fragments : []
      frags.forEach(f => {
        const txt = String(f || '').trim()
        if (txt) fragSet.add(txt)
      })
    })
    sections.push({ type: current, start, end: safeCount, wordPool: [...fragSet].join(', '), blankCount: safeCount - start + 1 })
  }
  return sections
}

function parseGuruUjianRangeValue(value, maxCount) {
  const raw = String(value || '').trim()
  if (!raw) return null
  const num = Number(raw)
  if (!Number.isFinite(num)) return null
  const rounded = Math.round(num)
  return Math.max(1, Math.min(Math.max(1, maxCount), rounded))
}

function estimateGuruUjianTotalFromSections(rawSections = [], fallbackTotal = 1) {
  const source = Array.isArray(rawSections) ? rawSections : []
  let cursor = 1
  source.forEach(item => {
    const type = normalizeExamQuestionType(item?.type, 'pilihan-ganda')
    const parsedEnd = parseGuruUjianRangeValue(item?.end, 200)
    const parsedCount = parseGuruUjianRangeValue(item?.count, 200)
    let length = 1
    if (parsedCount !== null) {
      length = parsedCount
    } else if (type === 'isi-titik') {
      const words = String(item?.wordPool || item?.words || '').split(',').map(x => String(x || '').trim()).filter(Boolean)
      length = Math.max(1, words.length || 1)
    } else if (parsedEnd !== null && parsedEnd >= cursor) {
      length = (parsedEnd - cursor + 1)
    }
    cursor += Math.max(1, length)
  })
  const estimated = cursor - 1
  const fallback = Number.isFinite(Number(fallbackTotal)) ? Math.max(0, Math.round(Number(fallbackTotal))) : 0
  return source.length ? Math.max(1, estimated) : fallback
}

function normalizeGuruUjianSections(totalCount, rawSections = []) {
  const safeCount = Number.isFinite(totalCount) ? Math.max(0, Math.min(200, Math.round(totalCount))) : 0
  const list = []
  let cursor = 1
  ;(Array.isArray(rawSections) ? rawSections : []).forEach(item => {
    if (cursor > safeCount) return
    const type = normalizeExamQuestionType(item?.type, 'pilihan-ganda')
    let end = cursor
    let wordPool = ''
    let blankCount = null
    let countHint = null
    const parsedCount = parseGuruUjianRangeValue(item?.count, safeCount)
    if (type === 'isi-titik') {
      wordPool = String(item?.wordPool || item?.words || '').trim()
      const words = wordPool.split(',').map(x => String(x || '').trim()).filter(Boolean)
      const autoCount = parsedCount === null ? Math.max(1, words.length || 1) : parsedCount
      blankCount = autoCount
      countHint = autoCount
      end = Math.min(safeCount, cursor + autoCount - 1)
    } else {
      const parsedEnd = parseGuruUjianRangeValue(item?.end, safeCount)
      if (parsedCount !== null) {
        countHint = parsedCount
        end = Math.min(safeCount, cursor + parsedCount - 1)
      } else {
        end = parsedEnd === null ? cursor : Math.max(cursor, parsedEnd)
      }
    }
    list.push({ type, start: cursor, end, wordPool, blankCount, countHint })
    cursor = end + 1
  })
  if (!list.length) return []
  return list.map(item => ({
    type: normalizeExamQuestionType(item.type, 'pilihan-ganda'),
    start: Math.max(1, Math.min(Math.max(1, safeCount), Number(item.start || 1))),
    end: Math.max(1, Math.min(Math.max(1, safeCount), Number(item.end || Math.max(1, safeCount)))),
    wordPool: String(item.wordPool || '').trim(),
    blankCount: Number.isFinite(Number(item.blankCount)) ? Math.max(1, Math.min(Math.max(1, safeCount), Math.round(Number(item.blankCount)))) : null,
    count: Number.isFinite(Number(item.countHint)) ? Math.max(1, Math.min(Math.max(1, safeCount), Math.round(Number(item.countHint)))) : Math.max(1, Math.min(Math.max(1, safeCount), Number(item.end || 1) - Number(item.start || 1) + 1)),
    wordList: String(item.wordPool || '').split(',').map(x => String(x || '').trim()).filter(Boolean)
  }))
}

function readGuruUjianSectionsFromDom() {
  const sectionRows = document.querySelectorAll('.guru-ujian-section-row')
  const rows = []
  sectionRows.forEach((rowEl, idx) => {
    const dataIndex = Number(rowEl?.dataset?.index || idx)
    const type = String(document.getElementById(`guru-ujian-section-type-${dataIndex}`)?.value || 'pilihan-ganda')
    const end = document.getElementById(`guru-ujian-section-end-${dataIndex}`)?.value
    const words = String(document.getElementById(`guru-ujian-section-words-${dataIndex}`)?.value || '')
    const count = document.getElementById(`guru-ujian-section-count-${dataIndex}`)?.value
    rows.push({ type, end, words, count })
  })
  return rows
}

function getGuruUjianSectionsSource(preferred = null) {
  if (Array.isArray(preferred) && preferred.length) return preferred
  const fromDom = readGuruUjianSectionsFromDom()
  if (Array.isArray(fromDom) && fromDom.length) return fromDom
  if (Array.isArray(ujianGuruState.sectionDefs) && ujianGuruState.sectionDefs.length) return ujianGuruState.sectionDefs
  return []
}

function renderGuruUjianSectionRows(sourceSections = null) {
  const wrap = document.getElementById('guru-ujian-sections')
  if (!wrap) return
  const baseSections = getGuruUjianSectionsSource(sourceSections)
  const totalCount = estimateGuruUjianTotalFromSections(baseSections, Number(document.getElementById('guru-ujian-jumlah')?.value || 0))
  const safeCount = Math.max(0, Math.min(200, totalCount))
  const normalized = normalizeGuruUjianSections(safeCount, baseSections)
  ujianGuruState.sectionDefs = normalized.map(item => ({ type: item.type, end: item.end, words: item.wordPool, count: item.count }))
  const totalEl = document.getElementById('guru-ujian-jumlah')
  if (totalEl) totalEl.value = String(normalized[normalized.length - 1]?.end || 0)
  wrap.innerHTML = `
    <div style="border:1px dashed #cbd5e1; border-radius:10px; padding:10px; display:grid; grid-template-columns:1fr 180px auto; gap:8px; align-items:end;">
      <div>
        <label class="guru-label">Tambah Model Soal</label>
        <select id="guru-ujian-new-section-type" class="guru-field">
          <option value="pilihan-ganda">Pilihan Ganda</option>
          <option value="esai">Esai</option>
          <option value="pasangkan-kata">Pasangkan Kata (A-B)</option>
          <option value="isi-titik">Isi Titik Kosong</option>
        </select>
      </div>
      <div>
        <label class="guru-label">Jumlah Soal</label>
        <input id="guru-ujian-new-section-count" class="guru-field" type="number" min="1" max="200" value="1">
      </div>
      <div>
        <button type="button" class="modal-btn" onclick="addGuruUjianSection()">Tambahkan</button>
      </div>
    </div>
  `
}

function getGuruUjianTypeMap(totalCount, options = {}) {
  const baseSections = getGuruUjianSectionsSource(options.sections)
  const estimatedTotal = estimateGuruUjianTotalFromSections(baseSections, totalCount)
  const safeCount = Number.isFinite(estimatedTotal) ? Math.max(0, Math.min(200, Math.round(estimatedTotal))) : 0
  const sections = normalizeGuruUjianSections(safeCount, baseSections)
  const errors = []
  const typeMap = new Array(Math.max(0, safeCount) + 1).fill('pilihan-ganda')
  sections.forEach(item => {
    for (let i = item.start; i <= item.end; i += 1) typeMap[i] = item.type
  })
  if (!sections.length) errors.push('Tambahkan minimal 1 model soal.')
  return { typeMap, errors, safeCount, sections }
}

function getGuruUjianDraftByNumber() {
  const map = new Map()
  const rows = document.querySelectorAll('.guru-ujian-question-row')
  rows.forEach(rowEl => {
    const no = Number(rowEl?.dataset?.no || 0)
    if (!Number.isFinite(no) || no <= 0) return
    const type = normalizeExamQuestionType(rowEl?.dataset?.type || 'pilihan-ganda', 'pilihan-ganda')
    const text = String(document.getElementById(`guru-ujian-q-${no}`)?.value || '')
    if (type === 'pilihan-ganda') {
      map.set(no, {
        no,
        type,
        text,
        options: {
          a: String(document.getElementById(`guru-ujian-q-${no}-a`)?.value || ''),
          b: String(document.getElementById(`guru-ujian-q-${no}-b`)?.value || ''),
          c: String(document.getElementById(`guru-ujian-q-${no}-c`)?.value || ''),
          d: String(document.getElementById(`guru-ujian-q-${no}-d`)?.value || '')
        },
        answer: String(document.getElementById(`guru-ujian-q-${no}-answer`)?.value || '')
      })
    } else if (type === 'pasangkan-kata') {
      const pairs = [1, 2, 3, 4].map(n => ({
        a: String(document.getElementById(`guru-ujian-q-${no}-a${n}`)?.value || ''),
        b: String(document.getElementById(`guru-ujian-q-${no}-b${n}`)?.value || '')
      }))
      map.set(no, { no, type, text, pairs })
    } else if (type === 'isi-titik') {
      const answer = String(document.getElementById(`guru-ujian-q-${no}-answer`)?.value || '')
      map.set(no, { no, type, text, answer })
    } else {
      map.set(no, { no, type, text })
    }
  })
  return map
}

function toExamStatusLabel(status) {
  const normalized = String(status || '').trim().toLowerCase()
  if (normalized === 'submitted') return 'Terkirim'
  if (normalized === 'draft') return 'Draft'
  return '-'
}

async function createExamPdfDoc(jadwal, soal) {
  const jsPdfApi = window.jspdf
  if (!jsPdfApi || typeof jsPdfApi.jsPDF !== 'function') {
    alert('Library PDF belum termuat. Refresh halaman lalu coba lagi.')
    return null
  }
  const { jsPDF } = jsPdfApi
  const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' })
  const margin = 15
  const pageWidth = doc.internal.pageSize.getWidth()
  const pageHeight = doc.internal.pageSize.getHeight()
  const usableWidth = pageWidth - margin * 2
  let y = margin

  await ensureExamPrintBackgroundLoaded()
  const drawPageBackground = () => {
    if (!examPrintBackgroundDataUrl) return
    try {
      doc.addImage(examPrintBackgroundDataUrl, 'PNG', 0, 0, pageWidth, pageHeight, undefined, 'FAST')
    } catch (bgErr) {
      console.warn('Gagal render background ujian.', bgErr)
    }
  }
  const originalAddPage = doc.addPage.bind(doc)
  doc.addPage = (...args) => {
    const out = originalAddPage(...args)
    drawPageBackground()
    return out
  }
  drawPageBackground()

  const instruksiMeta = parseExamInstruksiMeta(soal?.instruksi)
  const lang = instruksiMeta.lang || 'ID'
  const isAr = lang === 'AR'
  if (isAr) {
    await ensureExamArabicFontLoaded()
    if (!examArabicFontBase64) return null
  }
  const textMap = getExamPdfStaticText(lang)
  let arabicRegularReady = false

  if (isAr && examArabicFontBase64) {
    const tryRegister = (vfsKey, familyName, base64) => {
      try {
        doc.addFileToVFS(vfsKey, base64)
        doc.addFont(vfsKey, familyName, 'normal')
        doc.setFont(familyName, 'normal')
        return true
      } catch (_err) {
        return false
      }
    }
    arabicRegularReady = tryRegister(EXAM_ARABIC_FONT_VFS_KEY, EXAM_ARABIC_FONT_NAME, examArabicFontBase64) ||
      tryRegister(EXAM_ARABIC_FONT_FILE, EXAM_ARABIC_FONT_NAME, examArabicFontBase64)
    if (!arabicRegularReady) {
      console.warn('Registrasi font Arab regular gagal.')
    }
    if (!arabicRegularReady) return null
    try {
      doc.setFont(EXAM_ARABIC_FONT_NAME, 'normal')
      doc.splitTextToSize('اختبار', 20)
    } catch (fontErr) {
      console.warn('Validasi font Arab regular gagal.', fontErr)
      return null
    }
  }

  const setBold = () => doc.setFont(isAr ? EXAM_ARABIC_FONT_NAME : 'times', isAr ? 'normal' : 'bold')
  const setNormal = () => doc.setFont(isAr ? EXAM_ARABIC_FONT_NAME : 'times', 'normal')
  const safeSplit = (text, width) => {
    try {
      return doc.splitTextToSize(String(text || ''), width)
    } catch (err) {
      console.warn('splitTextToSize gagal, fallback single-line.', err)
      return [String(text || '')]
    }
  }
  if (typeof doc.setR2L === 'function') doc.setR2L(false)
  const lineX = (indent = 0) => (isAr ? pageWidth - margin - indent : margin + indent)
  const toRtl = text => (isAr ? `\u202B${String(text || '')}\u202C` : String(text || ''))
  const drawLine = (text, indent = 0) => {
    const str = toRtl(text)
    if (isAr) doc.text(str, lineX(indent), y, { align: 'right' })
    else doc.text(str, lineX(indent), y)
  }
  const drawWrapped = (text, wrapWidth, indent = 0) => {
    const lines = safeSplit(text, wrapWidth)
    lines.forEach(line => {
      if (y > 285) {
        doc.addPage()
        y = margin
      }
      const wrappedLine = toRtl(line)
      if (isAr) doc.text(wrappedLine, lineX(indent), y, { align: 'right' })
      else doc.text(wrappedLine, lineX(indent), y)
      y += 5
    })
  }

  setBold()
  doc.setFontSize(14)
  doc.text(textMap.title, pageWidth / 2, y, { align: 'center' })
  y += 8
  setNormal()
  doc.setFontSize(11)
  drawLine(`${textMap.jenis}: ${String(jadwal?.jenis || '-')}`)
  y += 6
  drawLine(`${textMap.namaUjian}: ${String(jadwal?.nama || '-')}`)
  y += 6
  drawLine(`${textMap.kelasMapel}: ${String(jadwal?.kelas || '-')} | ${textMap.mapel}: ${String(jadwal?.mapel || '-')}`)
  y += 6
  drawLine(`${textMap.tanggalWaktu}: ${String(jadwal?.tanggal || '-')} | ${textMap.waktu}: ${toTimeLabel(jadwal?.jam_mulai)} - ${toTimeLabel(jadwal?.jam_selesai)}`)
  y += 6
  drawLine(`${textMap.guru}: ${String(soal?.guru_nama || '-')}`)
  y += 8

  const instruksi = instruksiMeta.text
  if (instruksi) {
    setBold()
    drawLine(`${textMap.instruksiUmum}:`)
    y += 5
    setNormal()
    drawWrapped(instruksi, usableWidth, 0)
    y += 2
  }

  const questions = parseExamQuestions(soal?.questions_json)
  const sections = buildExamPrintSections(questions, soal?.bentuk_soal)
  const questionIndent = 4
  const optionIndent = 10
  sections.forEach((section, sectionIndex) => {
    const headingParts = getExamPrintTypeParts(section.type, sectionIndex, lang)
    const heading = `${headingParts.marker} ${headingParts.label}`
    const instruksiModel = getExamPrintTypeInstruction(section.type, lang)
    if (y + 12 > 285) {
      doc.addPage()
      y = margin
    }
    if (isAr) {
      setBold()
      drawLine(headingParts.marker)
      setNormal()
      drawLine(headingParts.label, 16)
    } else {
      setBold()
      drawLine(heading)
    }
    y += 5
    setNormal()
    drawWrapped(instruksiModel, usableWidth - 4, 4)
    y += 2

    const sectionItems = section.items || []
    if (section.type === 'isi-titik') {
      const fragSet = new Set()
      sectionItems.forEach(item => {
        const fragments = Array.isArray(item?.fragments) ? item.fragments : []
        fragments.forEach(f => {
          const txt = String(f || '').trim()
          if (txt) fragSet.add(txt)
        })
      })
      const fragList = [...fragSet]
      if (fragList.length) {
        const fragLine = `Pilihan kata: (${fragList.join(', ')})`
        drawWrapped(fragLine, usableWidth - optionIndent, optionIndent)
        y += 2
      }
    }

    sectionItems.forEach((q, idx) => {
      const isPg = section.type === 'pilihan-ganda'
      const no = idx + 1
      const qTextRaw = String(q?.text || '-')
      setNormal()
      if (isAr) {
        const noPrefix = formatExamMarker(formatExamNumber(no, lang), lang)
        const qLines = safeSplit(qTextRaw, usableWidth - questionIndent - 14)
        if (y > 285) {
          doc.addPage()
          y = margin
        }
        doc.text(toRtl(noPrefix), lineX(questionIndent), y, { align: 'right' })
        doc.text(toRtl(String(qLines[0] || '-')), lineX(questionIndent + 14), y, { align: 'right' })
        y += 5
        for (let li = 1; li < qLines.length; li += 1) {
          if (y > 285) {
            doc.addPage()
            y = margin
          }
          doc.text(toRtl(String(qLines[li] || '')), lineX(questionIndent + 14), y, { align: 'right' })
          y += 5
        }
      } else {
        const title = `${formatExamMarker(formatExamNumber(no, lang), lang)} ${qTextRaw}`
        drawWrapped(title, usableWidth - questionIndent, questionIndent)
      }
      if (isPg) {
        const opts = q?.options || {}
        const aTxt = `a. ${String(opts.a || '-')}`
        const bTxt = `b. ${String(opts.b || '-')}`
        const cTxt = `c. ${String(opts.c || '-')}`
        const dTxt = `d. ${String(opts.d || '-')}`
        const maxLen = Math.max(aTxt.length, bTxt.length, cTxt.length, dTxt.length)
        const useTwoCols = maxLen <= 36 && !isAr
        if (useTwoCols) {
          const leftX = margin + optionIndent
          const rightX = margin + optionIndent + 70
          const rows = [
            [aTxt, cTxt],
            [bTxt, dTxt]
          ]
          rows.forEach(pair => {
            if (y > 285) {
              doc.addPage()
              y = margin
            }
            doc.text(pair[0], leftX, y)
            doc.text(pair[1], rightX, y)
            y += 5
          })
        } else {
          const optLines = isAr
            ? [
              { marker: 'أ', text: String(opts.a || '-') },
              { marker: 'ب', text: String(opts.b || '-') },
              { marker: 'ج', text: String(opts.c || '-') },
              { marker: 'د', text: String(opts.d || '-') }
            ]
            : [aTxt, bTxt, cTxt, dTxt]
          optLines.forEach(line => {
            if (y > 285) {
              doc.addPage()
              y = margin
            }
            if (isAr) {
              const marker = formatExamMarker(String(line.marker || ''), lang)
              const val = String(line.text || '-')
              const vLines = safeSplit(val, usableWidth - optionIndent - 14)
              doc.text(toRtl(marker), lineX(optionIndent), y, { align: 'right' })
              doc.text(toRtl(String(vLines[0] || '-')), lineX(optionIndent + 14), y, { align: 'right' })
              y += 5
              for (let vi = 1; vi < vLines.length; vi += 1) {
                if (y > 285) {
                  doc.addPage()
                  y = margin
                }
                doc.text(toRtl(String(vLines[vi] || '')), lineX(optionIndent + 14), y, { align: 'right' })
                y += 5
              }
            } else {
              doc.text(line, margin + optionIndent, y)
              y += 5
            }
          })
        }
      } else if (section.type === 'pasangkan-kata') {
        const pairs = Array.isArray(q?.pairs) ? q.pairs : []
        if (pairs.length) {
          if (y > 282) {
            doc.addPage()
            y = margin
          }
          setBold()
          const colA = isAr ? 'العمود أ' : 'Baris A'
          const colB = isAr ? 'العمود ب' : 'Baris B'
          if (isAr) {
            doc.text(colA, lineX(optionIndent), y, { align: 'right' })
            doc.text(colB, lineX(optionIndent + 60), y, { align: 'right' })
          } else {
            doc.text(colA, margin + optionIndent, y)
            doc.text(colB, margin + optionIndent + 60, y)
          }
          y += 5
          setNormal()
          pairs.forEach((pair, idxPair) => {
            if (y > 285) {
              doc.addPage()
              y = margin
            }
            const left = `${formatExamMarker(formatExamNumber(idxPair + 1, lang), lang)} ${String(pair?.a || '-')}`
            const right = `${formatExamMarker((isAr ? getArabicLetterByIndex(idxPair) : String.fromCharCode(65 + idxPair)), lang)} ${String(pair?.b || '-')}`
            if (isAr) {
              doc.text(left, lineX(optionIndent), y, { align: 'right' })
              doc.text(right, lineX(optionIndent + 60), y, { align: 'right' })
            } else {
              doc.text(left, margin + optionIndent, y)
              doc.text(right, margin + optionIndent + 60, y)
            }
            y += 5
          })
        }
      }
      y += 2
    })
  })

  return doc
}

function openExamBrowserPrint(jadwal, soal) {
  const instruksiMeta = parseExamInstruksiMeta(soal?.instruksi)
  const lang = instruksiMeta.lang || 'ID'
  const textMap = getExamPdfStaticText(lang)
  const isAr = lang === 'AR'
  const questions = parseExamQuestions(soal?.questions_json)
  const sections = buildExamPrintSections(questions, soal?.bentuk_soal)
  const sectionHtml = sections.map((section, sectionIndex) => {
    const headingParts = getExamPrintTypeParts(section.type, sectionIndex, lang)
    const instruksiModel = getExamPrintTypeInstruction(section.type, lang)
    const items = section.items || []
    const extraLine = section.type === 'isi-titik'
      ? (() => {
          const fragSet = new Set()
          items.forEach(item => {
            const frags = Array.isArray(item?.fragments) ? item.fragments : []
            frags.forEach(f => {
              const txt = String(f || '').trim()
              if (txt) fragSet.add(txt)
            })
          })
          const list = [...fragSet]
          if (!list.length) return ''
          const label = isAr ? 'اختيارات الكلمات' : 'Pilihan kata'
          return `<p><strong>${escapeHtml(label)}:</strong> (${escapeHtml(list.join(', '))})</p>`
        })()
      : ''

    const questionsHtml = items.map((q, idx) => {
      const no = idx + 1
      const qText = `<div class="q-title">${escapeHtml(formatExamMarker(formatExamNumber(no, lang), lang))} ${escapeHtml(String(q?.text || '-'))}</div>`
      if (section.type === 'pilihan-ganda') {
        const opts = q?.options || {}
        const mA = isAr ? 'أ' : 'a'
        const mB = isAr ? 'ب' : 'b'
        const mC = isAr ? 'ج' : 'c'
        const mD = isAr ? 'د' : 'd'
        return `
          <li>
            ${qText}
            <div class="pg-grid">
              <div>${escapeHtml(formatExamMarker(mA, lang))} ${escapeHtml(String(opts.a || '-'))}</div>
              <div>${escapeHtml(formatExamMarker(mC, lang))} ${escapeHtml(String(opts.c || '-'))}</div>
              <div>${escapeHtml(formatExamMarker(mB, lang))} ${escapeHtml(String(opts.b || '-'))}</div>
              <div>${escapeHtml(formatExamMarker(mD, lang))} ${escapeHtml(String(opts.d || '-'))}</div>
            </div>
          </li>
        `
      }
      if (section.type === 'pasangkan-kata') {
        const pairs = Array.isArray(q?.pairs) ? q.pairs : []
        const rows = pairs.map((pair, i) => `
          <tr>
            <td>${escapeHtml(formatExamMarker(formatExamNumber(i + 1, lang), lang))} ${escapeHtml(String(pair?.a || '-'))}</td>
            <td>${escapeHtml(formatExamMarker((isAr ? getArabicLetterByIndex(i) : String.fromCharCode(65 + i)), lang))} ${escapeHtml(String(pair?.b || '-'))}</td>
          </tr>
        `).join('')
        return `
          <li>
            ${qText}
            <table class="pair-table">
              <thead><tr><th>${isAr ? 'العمود أ' : 'Baris A'}</th><th>${isAr ? 'العمود ب' : 'Baris B'}</th></tr></thead>
              <tbody>${rows}</tbody>
            </table>
          </li>
        `
      }
      return `<li>${qText}</li>`
    }).join('')

    return `
      <section class="sec">
        <h3><strong>${escapeHtml(headingParts.marker)}</strong> ${escapeHtml(headingParts.label)}</h3>
        <p>${escapeHtml(instruksiModel)}</p>
        ${extraLine}
        <ol>${questionsHtml}</ol>
      </section>
    `
  }).join('')

  const instruksiUmum = instruksiMeta.text
    ? `<p><strong>${escapeHtml(textMap.instruksiUmum)}:</strong> ${escapeHtml(instruksiMeta.text)}</p>`
    : ''

  const html = `
<!doctype html>
<html lang="${isAr ? 'ar' : 'id'}" dir="${isAr ? 'rtl' : 'ltr'}">
<head>
  <meta charset="utf-8">
  <title>${escapeHtml(textMap.title)}</title>
  <style>
    body { font-family: 'Times New Roman', serif; margin: 20px; color: #111; }
    h1 { text-align: center; font-size: 22px; margin: 0 0 12px 0; }
    .meta p { margin: 4px 0; font-size: 14px; }
    .sec { margin-top: 14px; }
    .sec h3 { margin: 0 0 6px 0; font-size: 16px; }
    .sec p { margin: 4px 0; font-size: 14px; }
    ol { margin: 6px 0 0 0; padding-${isAr ? 'right' : 'left'}: 20px; }
    li { margin: 8px 0; }
    .q-title { margin-bottom: 4px; }
    .pg-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 2px 18px; margin-${isAr ? 'right' : 'left'}: 20px; }
    .pair-table { border-collapse: collapse; width: 100%; margin-top: 4px; }
    .pair-table th, .pair-table td { border: 1px solid #999; padding: 4px 6px; font-size: 13px; text-align: ${isAr ? 'right' : 'left'}; }
    @media print { body { margin: 10mm; } }
  </style>
</head>
<body>
  <h1>${escapeHtml(textMap.title)}</h1>
  <div class="meta">
    <p><strong>${escapeHtml(textMap.jenis)}:</strong> ${escapeHtml(String(jadwal?.jenis || '-'))}</p>
    <p><strong>${escapeHtml(textMap.namaUjian)}:</strong> ${escapeHtml(String(jadwal?.nama || '-'))}</p>
    <p><strong>${escapeHtml(textMap.kelasMapel)}:</strong> ${escapeHtml(String(jadwal?.kelas || '-'))} | <strong>${escapeHtml(textMap.mapel)}:</strong> ${escapeHtml(String(jadwal?.mapel || '-'))}</p>
    <p><strong>${escapeHtml(textMap.tanggalWaktu)}:</strong> ${escapeHtml(String(jadwal?.tanggal || '-'))} | <strong>${escapeHtml(textMap.waktu)}:</strong> ${escapeHtml(`${toTimeLabel(jadwal?.jam_mulai)} - ${toTimeLabel(jadwal?.jam_selesai)}`)}</p>
    <p><strong>${escapeHtml(textMap.guru)}:</strong> ${escapeHtml(String(soal?.guru_nama || '-'))}</p>
    ${instruksiUmum}
  </div>
  ${sectionHtml}
</body>
</html>`

  const printWin = window.open('', '_blank')
  if (!printWin) {
    alert('Popup diblokir browser. Izinkan popup untuk mencetak soal Arab.')
    return
  }
  printWin.document.open()
  printWin.document.write(html)
  printWin.document.close()
  printWin.focus()
  setTimeout(() => {
    printWin.print()
  }, 250)
}

async function buildExamWordHtml(jadwal, soal) {
  const instruksiMeta = parseExamInstruksiMeta(soal?.instruksi)
  const lang = instruksiMeta.lang || 'ID'
  const textMap = getExamPdfStaticText(lang)
  const isAr = lang === 'AR'
  const bgDataUrl = await ensureExamPrintBackgroundLoaded()
  const questions = parseExamQuestions(soal?.questions_json)
  const sections = buildExamPrintSections(questions, soal?.bentuk_soal)
  const wordFontFamily = isAr ? '"Traditional Arabic","Times New Roman",serif' : '"Times New Roman",serif'
  const wordDirectionCss = isAr ? 'direction:rtl; unicode-bidi:embed; text-align:right;' : 'direction:ltr; text-align:left;'
  const wordBidiCss = isAr ? 'mso-bidi-font-family:"Traditional Arabic"; mso-fareast-font-family:"Traditional Arabic";' : ''
  const markerHtml = token => {
    const marker = formatExamMarker(token, lang)
    if (!isAr) return escapeHtml(marker)
    return `<span class="ar-marker">${escapeHtml(marker)}</span>`
  }
  const sectionHtml = sections.map((section, sectionIndex) => {
    const headingParts = getExamPrintTypeParts(section.type, sectionIndex, lang)
    const instruksiModel = getExamPrintTypeInstruction(section.type, lang)
    const items = section.items || []
    const questionsHtml = items.map((q, idx) => {
      const no = idx + 1
      const qText = `<div class="q-title">${markerHtml(formatExamNumber(no, lang))} ${escapeHtml(String(q?.text || '-'))}</div>`
      if (section.type === 'pilihan-ganda') {
        const opts = q?.options || {}
        const mA = isAr ? 'أ' : 'a'
        const mB = isAr ? 'ب' : 'b'
        const mC = isAr ? 'ج' : 'c'
        const mD = isAr ? 'د' : 'd'
        return `<li>${qText}<div class="pg-grid">
          <div>${markerHtml(mA)} ${escapeHtml(String(opts.a || '-'))}</div>
          <div>${markerHtml(mC)} ${escapeHtml(String(opts.c || '-'))}</div>
          <div>${markerHtml(mB)} ${escapeHtml(String(opts.b || '-'))}</div>
          <div>${markerHtml(mD)} ${escapeHtml(String(opts.d || '-'))}</div>
        </div></li>`
      }
      return `<li>${qText}</li>`
    }).join('')
    return `<section class="sec"><h3><strong>${isAr ? `<span class="ar-marker">${escapeHtml(headingParts.marker)}</span>` : escapeHtml(headingParts.marker)}</strong> ${escapeHtml(headingParts.label)}</h3><p>${escapeHtml(instruksiModel)}</p><ol>${questionsHtml}</ol></section>`
  }).join('')
  const instruksiUmum = instruksiMeta.text
    ? `<p><strong>${escapeHtml(textMap.instruksiUmum)}:</strong> ${escapeHtml(instruksiMeta.text)}</p>`
    : ''
  return `<!doctype html>
<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:w="urn:schemas-microsoft-com:office:word" xmlns:v="urn:schemas-microsoft-com:vml" lang="${isAr ? 'ar' : 'id'}" dir="${isAr ? 'rtl' : 'ltr'}">
<head><meta charset="utf-8"><title>${escapeHtml(textMap.title)}</title>
<style>
@page { size: A4; margin: 15mm; }
body { font-family: ${wordFontFamily}; ${wordBidiCss} ${wordDirectionCss} margin: 0; padding: 15mm; color: #111; }
.page-bg { position: fixed; top: 0; left: 0; width: 210mm; height: 297mm; z-index: 0; }
.page-bg img { width: 100%; height: 100%; display: block; }
.doc-wrap { position: relative; z-index: 1; }
h1 { text-align: center; font-size: 22px; margin: 0 0 12px 0; }
.meta p { margin: 4px 0; font-size: 14px; }
.sec { margin-top: 14px; }
.sec h3 { margin: 0 0 6px 0; font-size: 16px; }
.sec p { margin: 4px 0; font-size: 14px; }
ol { margin: 6px 0 0 0; padding-${isAr ? 'right' : 'left'}: 20px; }
li { margin: 8px 0; }
.q-title { margin-bottom: 4px; }
.pg-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 2px 18px; margin-${isAr ? 'right' : 'left'}: 20px; }
.ar-marker { display:inline-block; white-space:nowrap; direction:rtl; unicode-bidi:isolate-override; min-width:20px; }
* { font-family: ${wordFontFamily}; ${wordBidiCss} }
</style></head>
<body>
${bgDataUrl ? `<!--[if gte mso 9]><v:background id="bg" o:bwmode="white"><v:fill type="frame" src="${bgDataUrl}" /></v:background><![endif]-->` : ''}
${bgDataUrl ? `<div class="page-bg"><img src="${bgDataUrl}" alt=""></div>` : ''}
<div class="doc-wrap">
<h1>${escapeHtml(textMap.title)}</h1>
<div class="meta">
<p><strong>${escapeHtml(textMap.jenis)}:</strong> ${escapeHtml(String(jadwal?.jenis || '-'))}</p>
<p><strong>${escapeHtml(textMap.namaUjian)}:</strong> ${escapeHtml(String(jadwal?.nama || '-'))}</p>
<p><strong>${escapeHtml(textMap.kelasMapel)}:</strong> ${escapeHtml(String(jadwal?.kelas || '-'))} | <strong>${escapeHtml(textMap.mapel)}:</strong> ${escapeHtml(String(jadwal?.mapel || '-'))}</p>
<p><strong>${escapeHtml(textMap.tanggalWaktu)}:</strong> ${escapeHtml(String(jadwal?.tanggal || '-'))} | <strong>${escapeHtml(textMap.waktu)}:</strong> ${escapeHtml(`${toTimeLabel(jadwal?.jam_mulai)} - ${toTimeLabel(jadwal?.jam_selesai)}`)}</p>
<p><strong>${escapeHtml(textMap.guru)}:</strong> ${escapeHtml(String(soal?.guru_nama || '-'))}</p>
${instruksiUmum}
</div>
${sectionHtml}
</div>
</body></html>`
}

async function exportExamWordFile(jadwal, soal, fileName) {
  const html = await buildExamWordHtml(jadwal, soal)
  const blob = new Blob([`\ufeff${html}`], { type: 'application/msword;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  setTimeout(() => URL.revokeObjectURL(url), 1200)
}

async function renderUjianPage() {
  const content = document.getElementById('guru-content')
  if (!content) return
  content.innerHTML = 'Loading ujian...'

  let ctx
  try {
    ctx = await getGuruContext()
  } catch (error) {
    console.error(error)
    content.innerHTML = `<div class="placeholder-card">Gagal load ujian: ${escapeHtml(error.message || 'Unknown error')}</div>`
    return
  }

  if (!ctx?.guru?.id) {
    content.innerHTML = '<div class="placeholder-card">Data guru tidak ditemukan.</div>'
    return
  }

  const mapelPairsByClass = new Set(
    (ctx.yearDistribusiList || []).map(item => {
      const kelas = ctx.kelasMap.get(String(item.kelas_id || ''))
      const mapel = ctx.mapelMap.get(String(item.mapel_id || ''))
      return `${normalizeExamLookup(kelas?.nama_kelas)}|${normalizeExamLookup(getMapelLabel(mapel))}`
    }).filter(Boolean)
  )
  const mapelPairsByPerangkatan = new Set(
    (ctx.yearDistribusiList || []).map(item => {
      const kelas = ctx.kelasMap.get(String(item.kelas_id || ''))
      const mapel = ctx.mapelMap.get(String(item.mapel_id || ''))
      const perangkatan = getExamPerangkatanFromClassName(kelas?.nama_kelas)
      const mapelBase = getExamMapelBaseLabel(getMapelLabel(mapel))
      if (!perangkatan || !mapelBase) return ''
      return `${normalizeExamLookup(mapelBase)}|${normalizeExamLookup(perangkatan)}`
    }).filter(Boolean)
  )
  const classListByMapelPerangkatan = new Map()
  ;(ctx.yearDistribusiList || []).forEach(item => {
    const kelas = ctx.kelasMap.get(String(item.kelas_id || ''))
    const mapel = ctx.mapelMap.get(String(item.mapel_id || ''))
    const kelasNama = String(kelas?.nama_kelas || '').trim()
    const perangkatan = getExamPerangkatanFromClassName(kelasNama)
    const mapelBase = getExamMapelBaseLabel(getMapelLabel(mapel))
    if (!kelasNama || !perangkatan || !mapelBase) return
    const key = `${normalizeExamLookup(mapelBase)}|${normalizeExamLookup(perangkatan)}`
    if (!classListByMapelPerangkatan.has(key)) classListByMapelPerangkatan.set(key, new Set())
    classListByMapelPerangkatan.get(key).add(kelasNama)
  })
  const normalizedClassMap = new Map()
  classListByMapelPerangkatan.forEach((setValue, key) => {
    normalizedClassMap.set(key, [...setValue].sort((a, b) => a.localeCompare(b)))
  })
  ujianGuruState.mapelPairs = mapelPairsByClass
  ujianGuruState.classListByMapelPerangkatan = normalizedClassMap

  const jadwalRes = await sb
    .from(EXAM_SCHEDULE_TABLE)
    .select('id, jenis, nama, kelas, mapel, tanggal, jam_mulai, jam_selesai, lokasi, keterangan')
    .order('tanggal', { ascending: true })
  if (jadwalRes.error) {
    if (isExamTableMissingError(jadwalRes.error)) {
      content.innerHTML = `<div class="placeholder-card">${escapeHtml(buildExamScheduleMissingTableMessage())}</div>`
      return
    }
    console.error(jadwalRes.error)
    content.innerHTML = `<div class="placeholder-card">Gagal load jadwal ujian: ${escapeHtml(jadwalRes.error.message || 'Unknown error')}</div>`
    return
  }

  const filtered = (jadwalRes.data || []).filter(item => {
    const keyByClass = `${normalizeExamLookup(item.kelas)}|${normalizeExamLookup(item.mapel)}`
    if (mapelPairsByClass.has(keyByClass)) return true

    const meta = parseExamMetaFromSchedule(item)
    const perangkatan = String(meta?.perangkatan || '').trim() || String(item?.kelas || '').trim()
    const mapelBase = getExamMapelBaseLabel(String(meta?.mapel_nama || '').trim()) || getExamMapelBaseLabel(item?.mapel)
    const keyByPerangkatan = `${normalizeExamLookup(mapelBase)}|${normalizeExamLookup(perangkatan)}`
    return mapelPairsByPerangkatan.has(keyByPerangkatan)
  })
  const examRows = []
  filtered.forEach(item => {
    const meta = parseExamMetaFromSchedule(item)
    const perangkatan = String(meta?.perangkatan || '').trim() || String(item?.kelas || '').trim()
    const mapelBase = getExamMapelBaseLabel(String(meta?.mapel_nama || '').trim()) || getExamMapelBaseLabel(item?.mapel)
    const distKey = `${normalizeExamLookup(mapelBase)}|${normalizeExamLookup(perangkatan)}`
    const fallbackClassNames = normalizedClassMap.get(distKey) || []
    const kelasList = getExamRowClassList(item, fallbackClassNames)
    if (!kelasList.length) {
      examRows.push({
        rowKey: `${String(item.id || '')}|-`,
        jadwal: item,
        kelasNama: '-',
        mapelLabel: getExamRowMapelLabel(item)
      })
      return
    }
    kelasList.forEach(kelasNama => {
      examRows.push({
        rowKey: `${String(item.id || '')}|${kelasNama}`,
        jadwal: item,
        kelasNama,
        mapelLabel: getExamRowMapelLabel(item)
      })
    })
  })
  ujianGuruState.rows = examRows

  const jadwalIds = [...new Set(examRows.map(item => String(item.jadwal?.id || '')).filter(Boolean))]
  const soalMap = new Map()
  ujianGuruState.supportsKelasTarget = true
  if (jadwalIds.length) {
    let soalRes = await sb
      .from(EXAM_QUESTION_TABLE)
      .select('id, jadwal_id, kelas_target, guru_id, guru_nama, bentuk_soal, jumlah_nomor, instruksi, questions_json, status, updated_at')
      .eq('guru_id', String(ctx.guru.id))
      .in('jadwal_id', jadwalIds)

    if (soalRes.error && isExamColumnMissingError(soalRes.error)) {
      ujianGuruState.supportsKelasTarget = false
      soalRes = await sb
        .from(EXAM_QUESTION_TABLE)
        .select('id, jadwal_id, guru_id, guru_nama, bentuk_soal, jumlah_nomor, instruksi, questions_json, status, updated_at')
        .eq('guru_id', String(ctx.guru.id))
        .in('jadwal_id', jadwalIds)
    }
    if (soalRes.error && !isExamTableMissingError(soalRes.error)) {
      console.error(soalRes.error)
      content.innerHTML = `<div class="placeholder-card">Gagal load soal ujian: ${escapeHtml(soalRes.error.message || 'Unknown error')}</div>`
      return
    }
    ;(soalRes.data || []).forEach(item => {
      const kelasTarget = String(item.kelas_target || '').trim()
      const key = `${String(item.jadwal_id || '')}|${kelasTarget || '-'}`
      soalMap.set(key, item)
      if (!ujianGuruState.supportsKelasTarget && !kelasTarget) {
        soalMap.set(`${String(item.jadwal_id || '')}|*`, item)
      }
    })
  }
  ujianGuruState.soalByJadwal = soalMap

  const folderMap = new Map()
  examRows.forEach(item => {
    const folderName = String(item?.jadwal?.nama || '-').trim() || '-'
    if (!folderMap.has(folderName)) folderMap.set(folderName, [])
    folderMap.get(folderName).push(item)
  })

  const folderNames = [...folderMap.keys()].sort((a, b) => a.localeCompare(b))
  const folderHtml = folderNames.map(folderName => {
    const list = folderMap.get(folderName) || []
    const sortedList = [...list].sort((a, b) => {
      const kelasA = String(a?.kelasNama || '')
      const kelasB = String(b?.kelasNama || '')
      const kelasCmp = kelasA.localeCompare(kelasB, undefined, { sensitivity: 'base', numeric: true })
      if (kelasCmp !== 0) return kelasCmp
      const mapelCmp = String(a?.mapelLabel || '').localeCompare(String(b?.mapelLabel || ''), undefined, { sensitivity: 'base' })
      if (mapelCmp !== 0) return mapelCmp
      return String(a?.jadwal?.tanggal || '').localeCompare(String(b?.jadwal?.tanggal || ''))
    })
    const isOpen = ujianGuruState.openFolders.has(folderName)
    const rowsHtml = sortedList.map((item, idx) => {
      const sid = String(item.jadwal?.id || '')
      const rowKey = String(item.rowKey || `${sid}|-`)
      const soal = ujianGuruState.supportsKelasTarget
        ? (soalMap.get(rowKey) || null)
        : (soalMap.get(rowKey) || soalMap.get(`${sid}|*`) || null)
      return `
        <tr>
          <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${idx + 1}</td>
          <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.kelasNama || '-')}</td>
          <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.mapelLabel || '-')}</td>
          <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.jadwal?.tanggal || '-')}</td>
          <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(toExamStatusLabel(soal?.status))}</td>
          <td style="padding:8px; border:1px solid #e2e8f0; white-space:nowrap;">
            <button type="button" class="modal-btn modal-btn-primary" onclick="openGuruUjianEditorPage('${encodeURIComponent(rowKey)}')">${soal ? 'Edit Soal' : 'Buat Soal'}</button>
            <button type="button" class="modal-btn" ${soal ? '' : 'disabled'} onclick="chooseAndPrintGuruUjianByRow('${encodeURIComponent(rowKey)}')">Cetak</button>
          </td>
        </tr>
      `
    }).join('')

    return `
      <div class="placeholder-card" style="margin-bottom:10px;">
        <div style="display:flex; align-items:center; justify-content:space-between; gap:8px; flex-wrap:wrap;">
          <div style="font-weight:700; color:#0f172a;">${escapeHtml(folderName)}</div>
          <button type="button" class="modal-btn" onclick="toggleGuruExamFolder('${encodeURIComponent(folderName)}')">${isOpen ? 'Tutup' : 'Buka'}</button>
        </div>
        ${isOpen ? `
          <div style="overflow:auto; border:1px solid #e2e8f0; border-radius:10px; margin-top:8px;">
            <table style="width:100%; min-width:980px; border-collapse:collapse; font-size:13px;">
              <thead>
                <tr style="background:#f8fafc;">
                  <th style="padding:8px; border:1px solid #e2e8f0; width:44px;">No</th>
                  <th style="padding:8px; border:1px solid #e2e8f0;">Kelas</th>
                  <th style="padding:8px; border:1px solid #e2e8f0;">Mapel</th>
                  <th style="padding:8px; border:1px solid #e2e8f0;">Tanggal</th>
                  <th style="padding:8px; border:1px solid #e2e8f0;">Status</th>
                  <th style="padding:8px; border:1px solid #e2e8f0; width:220px;">Aksi</th>
                </tr>
              </thead>
              <tbody>
                ${rowsHtml || '<tr><td colspan="6" style="padding:12px; text-align:center; border:1px solid #e2e8f0;">Tidak ada data.</td></tr>'}
              </tbody>
            </table>
          </div>
        ` : ''}
      </div>
    `
  }).join('')

  content.innerHTML = `
    <div class="placeholder-card" style="margin-bottom:12px;">
      <div style="font-weight:700; margin-bottom:6px; color:#0f172a;">Folder Ujian</div>
      <div style="font-size:12px; color:#64748b;">Klik folder ujian untuk menampilkan daftar mapel dan membuat soal.</div>
    </div>
    ${folderHtml || '<div class="placeholder-card">Belum ada jadwal ujian yang sesuai mapel Anda.</div>'}
  `
}

function renderGuruUjianQuestionRows(forcedSections = null) {
  const count = Number(document.getElementById('guru-ujian-jumlah')?.value || 0)
  const listEl = document.getElementById('guru-ujian-questions')
  if (!listEl) return
  if (!document.querySelector('.guru-ujian-section-row')) {
    renderGuruUjianSectionRows(forcedSections || ujianGuruState.sectionDefs)
  }
  const draftByNo = getGuruUjianDraftByNumber()
  const existing = parseExamQuestions(ujianGuruState.activeSoal?.questions_json)
  const fallbackType = String(ujianGuruState.activeSoal?.bentuk_soal || 'pilihan-ganda')
  const existingByNo = new Map(
    existing.map((item, idx) => {
      const no = Number(item?.no || (idx + 1))
      return [no, { ...item, no, type: normalizeExamQuestionType(item?.type, fallbackType) }]
    })
  )
  const typeCfg = getGuruUjianTypeMap(count, { sections: forcedSections || ujianGuruState.sectionDefs })
  const safeCount = typeCfg.safeCount
  if (!safeCount || !(typeCfg.sections || []).length) {
    listEl.innerHTML = '<div class="placeholder-card">Belum ada model soal. Tambahkan model soal di bagian bawah.</div>'
    const infoEl = document.getElementById('guru-ujian-type-info')
    if (infoEl) infoEl.textContent = 'Belum ada model soal.'
    return
  }
  const sectionIndexByStart = new Map()
  ;(typeCfg.sections || []).forEach((sec, idx) => {
    sectionIndexByStart.set(Number(sec.start || 0), { ...sec, idx })
  })
  const localNoByAbsNo = new Map()
  let prevType = ''
  let localCounter = 0
  for (let i = 1; i <= safeCount; i += 1) {
    const t = typeCfg.typeMap[i] || 'pilihan-ganda'
    if (i === 1 || t !== prevType) localCounter = 1
    else localCounter += 1
    localNoByAbsNo.set(i, localCounter)
    prevType = t
  }
  let html = ''
  for (let i = 1; i <= safeCount; i += 1) {
    const prev = draftByNo.get(i) || existingByNo.get(i) || {}
    const qType = typeCfg.typeMap[i] || 'pilihan-ganda'
    const sectionForNo = (typeCfg.sections || []).find(sec => i >= sec.start && i <= sec.end) || null
    const localNo = localNoByAbsNo.get(i) || i
    if (sectionForNo && Number(sectionForNo.start) === i) {
      const secData = sectionIndexByStart.get(i) || { idx: 0, ...sectionForNo }
      const secIdx = Number(secData.idx || 0)
      const secCount = Math.max(1, Number(secData.end || i) - Number(secData.start || i) + 1)
      html += `
        <div class="placeholder-card guru-ujian-section-row" data-index="${secIdx}" style="margin-bottom:8px; border-color:#cbd5e1; background:#f8fafc;">
          <div style="display:grid; grid-template-columns:1fr 180px auto; gap:8px; align-items:end;">
            <div>
              <label class="guru-label">Model Soal</label>
              <select id="guru-ujian-section-type-${secIdx}" class="guru-field" onchange="onGuruUjianSectionChange()">
                <option value="pilihan-ganda" ${qType === 'pilihan-ganda' ? 'selected' : ''}>Pilihan Ganda</option>
                <option value="esai" ${qType === 'esai' ? 'selected' : ''}>Esai</option>
                <option value="pasangkan-kata" ${qType === 'pasangkan-kata' ? 'selected' : ''}>Pasangkan Kata (A-B)</option>
                <option value="isi-titik" ${qType === 'isi-titik' ? 'selected' : ''}>Isi Titik Kosong</option>
              </select>
            </div>
            <div>
              <label class="guru-label">Jumlah Soal</label>
              <input id="guru-ujian-section-count-${secIdx}" class="guru-field" type="number" min="1" max="200" value="${escapeHtml(String(secCount))}" onchange="onGuruUjianSectionChange()">
            </div>
            <div>
              <button type="button" class="modal-btn" ${(typeCfg.sections || []).length > 1 ? '' : 'disabled'} onclick="removeGuruUjianSection(${secIdx})">Hapus</button>
            </div>
          </div>
          <input id="guru-ujian-section-end-${secIdx}" type="hidden" value="${escapeHtml(String(sectionForNo.end || i))}">
          ${qType === 'isi-titik' ? `
            <div style="margin-top:8px;">
              <label class="guru-label">Pilihan Kata (pisahkan koma)</label>
              <input id="guru-ujian-section-words-${secIdx}" class="guru-field" type="text" value="${escapeHtml(String(sectionForNo.wordPool || ''))}" placeholder="ana, ila, aina" onchange="onGuruUjianSectionChange()">
            </div>
          ` : ''}
        </div>
      `
    }
    if (qType === 'pilihan-ganda') {
      const opts = prev.options || {}
      html += `
        <div class="placeholder-card guru-ujian-question-row" data-no="${i}" data-type="pilihan-ganda" style="margin-bottom:8px;">
          <div style="font-weight:700; margin-bottom:6px;">Nomor ${localNo} <span style="font-weight:600; color:#2563eb;">(Pilihan Ganda)</span></div>
          <textarea id="guru-ujian-q-${i}" class="guru-field" rows="2" placeholder="Tulis pertanyaan">${escapeHtml(String(prev.text || ''))}</textarea>
          <div style="display:grid; grid-template-columns:1fr 1fr; gap:8px; margin-top:8px;">
            <input id="guru-ujian-q-${i}-a" class="guru-field" type="text" placeholder="Opsi A" value="${escapeHtml(String(opts.a || ''))}">
            <input id="guru-ujian-q-${i}-b" class="guru-field" type="text" placeholder="Opsi B" value="${escapeHtml(String(opts.b || ''))}">
            <input id="guru-ujian-q-${i}-c" class="guru-field" type="text" placeholder="Opsi C" value="${escapeHtml(String(opts.c || ''))}">
            <input id="guru-ujian-q-${i}-d" class="guru-field" type="text" placeholder="Opsi D" value="${escapeHtml(String(opts.d || ''))}">
          </div>
          <div style="margin-top:8px;">
            <label class="guru-label">Kunci Jawaban</label>
            <select id="guru-ujian-q-${i}-answer" class="guru-field" style="max-width:140px;">
              <option value="">Pilih</option>
              <option value="A" ${String(prev.answer || '') === 'A' ? 'selected' : ''}>A</option>
              <option value="B" ${String(prev.answer || '') === 'B' ? 'selected' : ''}>B</option>
              <option value="C" ${String(prev.answer || '') === 'C' ? 'selected' : ''}>C</option>
              <option value="D" ${String(prev.answer || '') === 'D' ? 'selected' : ''}>D</option>
            </select>
          </div>
        </div>
      `
    } else if (qType === 'pasangkan-kata') {
      const pairs = Array.isArray(prev.pairs) ? prev.pairs : []
      const getPair = idx => pairs[idx] || {}
      html += `
        <div class="placeholder-card guru-ujian-question-row" data-no="${i}" data-type="pasangkan-kata" style="margin-bottom:8px;">
          <div style="font-weight:700; margin-bottom:6px;">Nomor ${localNo} <span style="font-weight:600; color:#7c3aed;">(Pasangkan Kata)</span></div>
          <textarea id="guru-ujian-q-${i}" class="guru-field" rows="2" placeholder="Tulis instruksi singkat soal (opsional)">${escapeHtml(String(prev.text || ''))}</textarea>
          <div style="display:grid; grid-template-columns:1fr 1fr; gap:8px; margin-top:8px;">
            <input id="guru-ujian-q-${i}-a1" class="guru-field" type="text" placeholder="Baris A 1" value="${escapeHtml(String(getPair(0).a || ''))}">
            <input id="guru-ujian-q-${i}-b1" class="guru-field" type="text" placeholder="Baris B 1" value="${escapeHtml(String(getPair(0).b || ''))}">
            <input id="guru-ujian-q-${i}-a2" class="guru-field" type="text" placeholder="Baris A 2" value="${escapeHtml(String(getPair(1).a || ''))}">
            <input id="guru-ujian-q-${i}-b2" class="guru-field" type="text" placeholder="Baris B 2" value="${escapeHtml(String(getPair(1).b || ''))}">
            <input id="guru-ujian-q-${i}-a3" class="guru-field" type="text" placeholder="Baris A 3" value="${escapeHtml(String(getPair(2).a || ''))}">
            <input id="guru-ujian-q-${i}-b3" class="guru-field" type="text" placeholder="Baris B 3" value="${escapeHtml(String(getPair(2).b || ''))}">
            <input id="guru-ujian-q-${i}-a4" class="guru-field" type="text" placeholder="Baris A 4" value="${escapeHtml(String(getPair(3).a || ''))}">
            <input id="guru-ujian-q-${i}-b4" class="guru-field" type="text" placeholder="Baris B 4" value="${escapeHtml(String(getPair(3).b || ''))}">
          </div>
        </div>
      `
    } else if (qType === 'isi-titik') {
      const fragments = Array.isArray(sectionForNo?.wordList) ? sectionForNo.wordList : []
      html += `
        <div class="placeholder-card guru-ujian-question-row" data-no="${i}" data-type="isi-titik" style="margin-bottom:8px;">
          <div style="font-weight:700; margin-bottom:6px;">Nomor ${localNo} <span style="font-weight:600; color:#ea580c;">(Isi Titik Kosong)</span></div>
          <div style="font-size:12px; color:#64748b; margin-bottom:6px;">Pilihan kata: (${escapeHtml(fragments.join(', ') || '-')})</div>
          <textarea id="guru-ujian-q-${i}" class="guru-field" rows="2" placeholder="Tulis kalimat dengan titik kosong, contoh: Ana ... ila madrasah.">${escapeHtml(String(prev.text || ''))}</textarea>
          <input id="guru-ujian-q-${i}-answer" class="guru-field" type="text" placeholder="Jawaban benar" value="${escapeHtml(String(prev.answer || ''))}" style="margin-top:8px;">
        </div>
      `
    } else {
      html += `
        <div class="placeholder-card guru-ujian-question-row" data-no="${i}" data-type="esai" style="margin-bottom:8px;">
          <div style="font-weight:700; margin-bottom:6px;">Nomor ${localNo} <span style="font-weight:600; color:#0f766e;">(Esai)</span></div>
          <textarea id="guru-ujian-q-${i}" class="guru-field" rows="3" placeholder="Tulis pertanyaan">${escapeHtml(String(prev.text || ''))}</textarea>
        </div>
      `
    }
  }
  const infoEl = document.getElementById('guru-ujian-type-info')
  if (infoEl) {
    if (typeCfg.errors.length) {
      infoEl.innerHTML = `<span style="color:#b91c1c;">${escapeHtml(typeCfg.errors[0])}</span>`
    } else {
      const sectionLabel = (typeCfg.sections || []).map(item => {
        const label = item.type === 'esai'
          ? 'Esai'
          : (item.type === 'pasangkan-kata'
            ? 'Pasangkan Kata'
            : (item.type === 'isi-titik' ? 'Isi Titik' : 'PG'))
        return `${label} ${item.start}-${item.end}`
      }).join(' | ')
      infoEl.textContent = `Model aktif: ${sectionLabel || '-'}`
    }
  }
  listEl.innerHTML = html
}
function toggleGuruExamFolder(folderNameEncoded) {
  const key = decodeURIComponent(String(folderNameEncoded || '')).trim()
  if (!key) return
  if (ujianGuruState.openFolders.has(key)) ujianGuruState.openFolders.delete(key)
  else ujianGuruState.openFolders.add(key)
  renderUjianPage()
}

function openGuruUjianEditorPage(jadwalId) {
  const decodedKey = decodeURIComponent(String(jadwalId || '')).trim()
  if (!decodedKey) return
  const row = (ujianGuruState.rows || []).find(item => String(item.rowKey || '') === decodedKey)
  if (!row?.jadwal) return
  const jadwal = row.jadwal
  const sid = String(jadwal.id || '')
  ujianGuruState.activeSoal = ujianGuruState.supportsKelasTarget
    ? (ujianGuruState.soalByJadwal.get(decodedKey) || null)
    : (ujianGuruState.soalByJadwal.get(decodedKey) || ujianGuruState.soalByJadwal.get(`${sid}|*`) || null)

  const content = document.getElementById('guru-content')
  if (!content) return

  const existing = parseExamQuestions(ujianGuruState.activeSoal?.questions_json)
  const countValue = ujianGuruState.activeSoal?.jumlah_nomor || existing.length || 0
  const fallbackType = String(ujianGuruState.activeSoal?.bentuk_soal || 'pilihan-ganda')
  const sections = deriveExamSectionsFromQuestions(existing, fallbackType, Number(countValue))
  const instruksiMeta = parseExamInstruksiMeta(ujianGuruState.activeSoal?.instruksi)
  const instruksi = String(instruksiMeta.text || '').trim()
  const kelasLabel = String(row.kelasNama || '-')
  const mapelLabel = String(row.mapelLabel || getExamRowMapelLabel(jadwal))

  ujianGuruState.activeJadwal = jadwal
  ujianGuruState.activeKelasName = kelasLabel
  ujianGuruState.sectionDefs = sections.map(item => ({ type: item.type, end: item.end, words: item.wordPool || '', count: item.blankCount || null }))
  content.innerHTML = `
    <div class="placeholder-card" style="border-color:#93c5fd;">
      <div style="display:flex; align-items:center; justify-content:space-between; gap:8px; margin-bottom:8px; flex-wrap:wrap;">
        <div style="font-weight:700; color:#0f172a;">Editor Soal - ${escapeHtml(jadwal.nama || '-')}</div>
        <button type="button" class="modal-btn" onclick="backToGuruUjianList()">Kembali ke Folder</button>
      </div>
      <div style="display:grid; grid-template-columns:repeat(auto-fit,minmax(220px,1fr)); gap:8px; margin-bottom:10px;">
        <div class="placeholder-card"><strong>Kelas</strong><br>${escapeHtml(kelasLabel)}</div>
        <div class="placeholder-card"><strong>Mapel</strong><br>${escapeHtml(mapelLabel)}</div>
        <div class="placeholder-card"><strong>Tanggal</strong><br>${escapeHtml(String(jadwal?.tanggal || '-'))}</div>
      </div>
      <div style="display:grid; grid-template-columns:repeat(auto-fit,minmax(200px,1fr)); gap:8px; margin-bottom:10px;">
        <div>
          <label class="guru-label">Jumlah Soal</label>
          <input id="guru-ujian-jumlah" class="guru-field" type="number" min="0" max="200" value="${escapeHtml(String(countValue))}" readonly>
        </div>
      </div>
      <div id="guru-ujian-type-info" style="font-size:12px; color:#64748b; margin:-4px 0 8px 0;"></div>
      <div style="margin-bottom:10px;">
        <label class="guru-label">Bahasa Soal</label>
        <select id="guru-ujian-lang" class="guru-field" style="max-width:240px;">
          <option value="ID" ${instruksiMeta.lang !== 'AR' ? 'selected' : ''}>Indonesia</option>
          <option value="AR" ${instruksiMeta.lang === 'AR' ? 'selected' : ''}>Arab</option>
        </select>
      </div>
      <div style="margin-bottom:10px;">
        <label class="guru-label">Instruksi</label>
        <textarea id="guru-ujian-instruksi" class="guru-field" rows="2" placeholder="Tulis instruksi pengerjaan soal...">${escapeHtml(instruksi)}</textarea>
      </div>
      <div id="guru-ujian-questions"></div>
      <div id="guru-ujian-sections" style="margin-top:10px; margin-bottom:10px;"></div>
      <div style="display:flex; gap:8px; margin-top:8px; flex-wrap:wrap;">
        <button type="button" class="modal-btn" onclick="saveGuruUjian(false)">Simpan Draft</button>
        <button type="button" class="modal-btn modal-btn-primary" onclick="saveGuruUjian(true)">Kirim Soal</button>
        <button type="button" class="modal-btn" onclick="chooseAndPrintGuruUjianActive()">Cetak</button>
      </div>
    </div>
  `
  renderGuruUjianSectionRows(ujianGuruState.sectionDefs)
  renderGuruUjianQuestionRows(ujianGuruState.sectionDefs)
}

function backToGuruUjianList() {
  ujianGuruState.activeJadwal = null
  ujianGuruState.activeKelasName = ''
  ujianGuruState.sectionDefs = []
  ujianGuruState.activeSoal = null
  renderUjianPage()
}

function openGuruUjianEditor(jadwalId) {
  const sid = String(jadwalId || '')
  const row = (ujianGuruState.rows || []).find(item => String(item.rowKey || '') === sid || String(item.jadwal?.id || '') === sid)
  if (!row) return
  openGuruUjianEditorPage(encodeURIComponent(String(row.rowKey || sid)))
}

function closeGuruUjianEditor() {
  backToGuruUjianList()
}

function onGuruUjianCountChange() {
  const latest = getGuruUjianSectionsSource()
  ujianGuruState.sectionDefs = latest
  renderGuruUjianSectionRows(latest)
  renderGuruUjianQuestionRows(latest)
}

function onGuruUjianShapeChange() {
  const latest = getGuruUjianSectionsSource()
  ujianGuruState.sectionDefs = latest
  renderGuruUjianSectionRows(latest)
  renderGuruUjianQuestionRows(latest)
}

function onGuruUjianSectionChange() {
  const latest = getGuruUjianSectionsSource()
  ujianGuruState.sectionDefs = latest
  renderGuruUjianSectionRows(latest)
  renderGuruUjianQuestionRows(latest)
}

function addGuruUjianSection() {
  const current = Array.isArray(ujianGuruState.sectionDefs) ? [...ujianGuruState.sectionDefs] : []
  const fromDom = readGuruUjianSectionsFromDom()
  const baseCurrent = fromDom.length ? fromDom : current
  const newType = String(document.getElementById('guru-ujian-new-section-type')?.value || 'pilihan-ganda')
  const newCountRaw = document.getElementById('guru-ujian-new-section-count')?.value
  const newCount = parseGuruUjianRangeValue(newCountRaw, 200) || 1
  const base = baseCurrent.concat([{ type: newType, count: newCount }])
  ujianGuruState.sectionDefs = base
  renderGuruUjianSectionRows(base)
  renderGuruUjianQuestionRows(base)
}

function removeGuruUjianSection(index) {
  const idx = Number(index)
  const fromDom = readGuruUjianSectionsFromDom()
  const sections = fromDom.length ? fromDom : (Array.isArray(ujianGuruState.sectionDefs) ? [...ujianGuruState.sectionDefs] : [])
  if (!Number.isFinite(idx) || idx < 0 || idx >= sections.length) return
  if (sections.length <= 1) return
  const next = sections.filter((_item, i) => i !== idx)
  ujianGuruState.sectionDefs = next
  renderGuruUjianSectionRows(next)
  renderGuruUjianQuestionRows(next)
}

function collectGuruUjianQuestions() {
  const count = Number(document.getElementById('guru-ujian-jumlah')?.value || 0)
  const safeCount = Number.isFinite(count) ? Math.max(1, Math.min(200, Math.round(count))) : 1
  const typeCfg = getGuruUjianTypeMap(safeCount)
  if (typeCfg.errors.length) {
    alert(typeCfg.errors[0])
    return null
  }
  const questions = []
  for (let i = 1; i <= safeCount; i += 1) {
    const text = String(document.getElementById(`guru-ujian-q-${i}`)?.value || '').trim()
    if (!text) continue
    const qType = typeCfg.typeMap[i] || 'pilihan-ganda'
    if (qType === 'pilihan-ganda') {
      const options = {
        a: String(document.getElementById(`guru-ujian-q-${i}-a`)?.value || '').trim(),
        b: String(document.getElementById(`guru-ujian-q-${i}-b`)?.value || '').trim(),
        c: String(document.getElementById(`guru-ujian-q-${i}-c`)?.value || '').trim(),
        d: String(document.getElementById(`guru-ujian-q-${i}-d`)?.value || '').trim()
      }
      const answer = String(document.getElementById(`guru-ujian-q-${i}-answer`)?.value || '').trim().toUpperCase()
      questions.push({ no: i, type: qType, text, options, answer })
    } else if (qType === 'pasangkan-kata') {
      const pairs = [1, 2, 3, 4].map(n => ({
        a: String(document.getElementById(`guru-ujian-q-${i}-a${n}`)?.value || '').trim(),
        b: String(document.getElementById(`guru-ujian-q-${i}-b${n}`)?.value || '').trim()
      })).filter(item => item.a || item.b)
      questions.push({ no: i, type: qType, text, pairs })
    } else if (qType === 'isi-titik') {
      const sectionForNo = (typeCfg.sections || []).find(sec => i >= sec.start && i <= sec.end) || null
      const fragments = Array.isArray(sectionForNo?.wordList) ? sectionForNo.wordList : []
      const answer = String(document.getElementById(`guru-ujian-q-${i}-answer`)?.value || '').trim()
      questions.push({ no: i, type: qType, text, fragments, answer })
    } else {
      questions.push({ no: i, type: qType, text })
    }
  }
  return questions
}

async function saveGuruUjian(submitMode) {
  const jadwal = ujianGuruState.activeJadwal
  const kelasTarget = String(ujianGuruState.activeKelasName || '').trim()
  if (!jadwal?.id) {
    alert('Jadwal ujian belum dipilih.')
    return
  }
  const ctx = await getGuruContext()
  if (!ctx?.guru?.id) {
    alert('Data guru tidak ditemukan.')
    return
  }

  const jumlahNomor = Number(document.getElementById('guru-ujian-jumlah')?.value || 0)
  const bahasaSoal = String(document.getElementById('guru-ujian-lang')?.value || 'ID').toUpperCase() === 'AR' ? 'AR' : 'ID'
  const instruksi = String(document.getElementById('guru-ujian-instruksi')?.value || '').trim()
  const questions = collectGuruUjianQuestions()
  if (!questions) return
  if (!questions.length) {
    alert('Minimal isi satu soal.')
    return
  }

  const payload = {
    jadwal_id: String(jadwal.id),
    kelas_target: kelasTarget || null,
    guru_id: String(ctx.guru.id),
    guru_nama: String(ctx.guru.nama || ctx.guru.id_karyawan || '').trim() || null,
    bentuk_soal: 'campuran',
    jumlah_nomor: Number.isFinite(jumlahNomor) ? Math.max(1, Math.round(jumlahNomor)) : questions.length,
    instruksi: buildExamInstruksiWithMeta(bahasaSoal, instruksi),
    questions_json: JSON.stringify(questions),
    status: submitMode ? 'submitted' : 'draft',
    updated_at: new Date().toISOString()
  }

  let query
  if (!ujianGuruState.supportsKelasTarget) delete payload.kelas_target

  const activeSoalKelas = String(ujianGuruState.activeSoal?.kelas_target || '').trim()
  const isSameClassRecord = !ujianGuruState.supportsKelasTarget || activeSoalKelas === kelasTarget
  if (ujianGuruState.activeSoal?.id && isSameClassRecord) {
    query = sb
      .from(EXAM_QUESTION_TABLE)
      .update(payload)
      .eq('id', ujianGuruState.activeSoal.id)
      .select('id, jadwal_id, kelas_target, guru_id, guru_nama, bentuk_soal, jumlah_nomor, instruksi, questions_json, status, updated_at')
      .maybeSingle()
  } else {
    query = sb
      .from(EXAM_QUESTION_TABLE)
      .insert([payload])
      .select('id, jadwal_id, kelas_target, guru_id, guru_nama, bentuk_soal, jumlah_nomor, instruksi, questions_json, status, updated_at')
      .maybeSingle()
  }

  const { data, error } = await query
  if (error) {
    const errMsg = String(error.message || '')
    const errCode = String(error.code || '').toUpperCase()
    if (errCode === '23505' && ujianGuruState.supportsKelasTarget) {
      alert(
        `Soal per kelas belum bisa disimpan karena index unik lama masih aktif.\n\n` +
        `Jalankan SQL ini:\n` +
        `drop index if exists public.soal_ujian_jadwal_guru_uidx;\n` +
        `create unique index if not exists soal_ujian_jadwal_guru_kelas_uidx on public.${EXAM_QUESTION_TABLE}(jadwal_id, guru_id, kelas_target);`
      )
      return
    }
    if (isExamTableMissingError(error)) {
      alert(buildExamQuestionMissingTableMessage())
      return
    }
    if (isExamColumnMissingError(error)) {
      alert(`Kolom tabel '${EXAM_QUESTION_TABLE}' belum sesuai.\nGunakan kolom: jadwal_id, kelas_target, guru_id, guru_nama, bentuk_soal, jumlah_nomor, instruksi, questions_json, status, updated_at.`)
      return
    }
    console.error(error)
    alert(`Gagal menyimpan soal ujian: ${errMsg || 'Unknown error'}`)
    return
  }

  if (data && typeof data === 'object') {
    ujianGuruState.activeSoal = data
    const rowKey = `${String(data.jadwal_id || '')}|${String(data.kelas_target || '-')}`
    ujianGuruState.soalByJadwal.set(rowKey, data)
    if (!String(data.kelas_target || '').trim()) ujianGuruState.soalByJadwal.set(`${String(data.jadwal_id || '')}|*`, data)
  }

  alert(submitMode ? 'Soal berhasil dikirim.' : 'Draft soal berhasil disimpan.')
  if (submitMode) {
    await renderUjianPage()
    return
  }
  renderGuruUjianQuestionRows(ujianGuruState.sectionDefs)
}

async function printGuruUjianByJadwal(jadwalId) {
  const sid = String(jadwalId || '')
  const row = (ujianGuruState.rows || []).find(item => String(item.jadwal?.id || '') === sid)
  if (!row) return
  await printGuruUjianByRow(encodeURIComponent(String(row.rowKey || `${sid}|-`)))
}

async function printGuruUjianActive(format = '') {
  const mode = String(format || '').trim().toLowerCase()
  try {
    const jadwal = ujianGuruState.activeJadwal
    if (!jadwal?.id) {
      alert('Pilih jadwal ujian terlebih dahulu.')
      return
    }
    const questions = collectGuruUjianQuestions()
    if (!questions || !questions.length) {
      alert('Minimal isi satu soal sebelum cetak.')
      return
    }
    const jumlahNomor = Number(document.getElementById('guru-ujian-jumlah')?.value || questions.length || 0)
    const bahasaSoal = String(document.getElementById('guru-ujian-lang')?.value || 'ID').toUpperCase() === 'AR' ? 'AR' : 'ID'
    const instruksi = String(document.getElementById('guru-ujian-instruksi')?.value || '').trim()
    const kelasTarget = String(ujianGuruState.activeKelasName || '-')
    const soal = {
      guru_nama: String(ujianGuruState.activeSoal?.guru_nama || ''),
      bentuk_soal: 'campuran',
      jumlah_nomor: Number.isFinite(jumlahNomor) ? Math.max(1, Math.round(jumlahNomor)) : questions.length,
      instruksi: buildExamInstruksiWithMeta(bahasaSoal, instruksi),
      questions_json: JSON.stringify(questions),
      status: 'draft'
    }
    const lang = parseExamInstruksiMeta(soal.instruksi).lang || 'ID'
    if (mode === 'word' || (mode !== 'pdf' && lang === 'AR')) {
      const fileName = `Soal ${sanitizeFileNamePart(jadwal.nama || 'Ujian')} - ${sanitizeFileNamePart(kelasTarget || '-')}.doc`
      await exportExamWordFile({ ...jadwal, kelas: kelasTarget }, soal, fileName)
      return
    }
    const doc = await createExamPdfDoc({ ...jadwal, kelas: kelasTarget }, soal)
    if (!doc) {
      alert('Cetak gagal: font Arab/PDF belum siap. Pastikan file TTF tersedia dan refresh halaman.')
      return
    }
    const fileName = `Soal ${sanitizeFileNamePart(jadwal.nama || 'Ujian')} - ${sanitizeFileNamePart(kelasTarget || '-')}.pdf`
    doc.save(fileName)
  } catch (err) {
    console.error('printGuruUjianActive error:', err)
    alert(`Cetak gagal: ${String(err?.message || err || 'Unknown error')}`)
  }
}

async function printGuruUjianByRow(rowKeyEncoded, format = '') {
  const mode = String(format || '').trim().toLowerCase()
  try {
    const decodedKey = decodeURIComponent(String(rowKeyEncoded || '')).trim()
    if (!decodedKey) return
    const [jadwalId, kelasNamaRaw] = decodedKey.split('|')
    const kelasNama = String(kelasNamaRaw || '-').trim() || '-'
    const jadwal = (ujianGuruState.rows || []).find(item => String(item.rowKey || '') === decodedKey)?.jadwal || (ujianGuruState.rows || []).find(item => String(item.jadwal?.id || '') === String(jadwalId || ''))?.jadwal
    if (!jadwal) return

    const ctx = await getGuruContext()
    let soalRes = await sb
      .from(EXAM_QUESTION_TABLE)
      .select('id, jadwal_id, kelas_target, guru_id, guru_nama, bentuk_soal, jumlah_nomor, instruksi, questions_json, status')
      .eq('jadwal_id', String(jadwalId || ''))
      .eq('guru_id', String(ctx.guru.id))
      .eq('kelas_target', kelasNama)
      .maybeSingle()

    if (soalRes.error && isExamColumnMissingError(soalRes.error)) {
      soalRes = await sb
        .from(EXAM_QUESTION_TABLE)
        .select('id, jadwal_id, guru_id, guru_nama, bentuk_soal, jumlah_nomor, instruksi, questions_json, status')
        .eq('jadwal_id', String(jadwalId || ''))
        .eq('guru_id', String(ctx.guru.id))
        .maybeSingle()
    }

    if (soalRes.error || !soalRes.data) {
      alert('Soal belum tersedia untuk dicetak.')
      return
    }
    const lang = parseExamInstruksiMeta(soalRes.data?.instruksi).lang || 'ID'
    if (mode === 'word' || (mode !== 'pdf' && lang === 'AR')) {
      const fileName = `Soal ${sanitizeFileNamePart(jadwal.nama || 'Ujian')} - ${sanitizeFileNamePart(kelasNama || '-')}.doc`
      await exportExamWordFile({ ...jadwal, kelas: kelasNama }, soalRes.data, fileName)
      return
    }

    const doc = await createExamPdfDoc({ ...jadwal, kelas: kelasNama }, soalRes.data)
    if (!doc) {
      alert('Cetak gagal: font Arab/PDF belum siap. Pastikan file TTF tersedia dan refresh halaman.')
      return
    }
    const fileName = `Soal ${sanitizeFileNamePart(jadwal.nama || 'Ujian')} - ${sanitizeFileNamePart(kelasNama || '-')}.pdf`
    doc.save(fileName)
  } catch (err) {
    console.error('printGuruUjianByRow error:', err)
    alert(`Cetak gagal: ${String(err?.message || err || 'Unknown error')}`)
  }
}

async function chooseExamPrintFormatPopup() {
  // Always use native confirm for this flow to avoid hidden custom-popup issues.
  const asWord = window.confirm('Cetak sebagai Word?\nPilih OK untuk Word, Cancel untuk PDF.')
  if (asWord) return 'word'
  return 'pdf'
}

async function chooseAndPrintGuruUjianActive() {
  await printGuruUjianActive()
}

async function chooseAndPrintGuruUjianByRow(rowKeyEncoded) {
  await printGuruUjianByRow(rowKeyEncoded)
}

async function exportGuruUjianActiveWord() {
  try {
    const jadwal = ujianGuruState.activeJadwal
    if (!jadwal?.id) {
      alert('Pilih jadwal ujian terlebih dahulu.')
      return
    }
    const questions = collectGuruUjianQuestions()
    if (!questions || !questions.length) {
      alert('Minimal isi satu soal sebelum export Word.')
      return
    }
    const jumlahNomor = Number(document.getElementById('guru-ujian-jumlah')?.value || questions.length || 0)
    const bahasaSoal = String(document.getElementById('guru-ujian-lang')?.value || 'ID').toUpperCase() === 'AR' ? 'AR' : 'ID'
    const instruksi = String(document.getElementById('guru-ujian-instruksi')?.value || '').trim()
    const kelasTarget = String(ujianGuruState.activeKelasName || '-')
    const soal = {
      guru_nama: String(ujianGuruState.activeSoal?.guru_nama || ''),
      bentuk_soal: 'campuran',
      jumlah_nomor: Number.isFinite(jumlahNomor) ? Math.max(1, Math.round(jumlahNomor)) : questions.length,
      instruksi: buildExamInstruksiWithMeta(bahasaSoal, instruksi),
      questions_json: JSON.stringify(questions),
      status: 'draft'
    }
    const fileName = `Soal ${sanitizeFileNamePart(jadwal.nama || 'Ujian')} - ${sanitizeFileNamePart(kelasTarget || '-')}.doc`
    await exportExamWordFile({ ...jadwal, kelas: kelasTarget }, soal, fileName)
  } catch (err) {
    console.error('exportGuruUjianActiveWord error:', err)
    alert(`Export Word gagal: ${String(err?.message || err || 'Unknown error')}`)
  }
}

async function exportGuruUjianByRowWord(rowKeyEncoded) {
  try {
    const decodedKey = decodeURIComponent(String(rowKeyEncoded || '')).trim()
    if (!decodedKey) return
    const [jadwalId, kelasNamaRaw] = decodedKey.split('|')
    const kelasNama = String(kelasNamaRaw || '-').trim() || '-'
    const jadwal = (ujianGuruState.rows || []).find(item => String(item.rowKey || '') === decodedKey)?.jadwal || (ujianGuruState.rows || []).find(item => String(item.jadwal?.id || '') === String(jadwalId || ''))?.jadwal
    if (!jadwal) return

    const ctx = await getGuruContext()
    let soalRes = await sb
      .from(EXAM_QUESTION_TABLE)
      .select('id, jadwal_id, kelas_target, guru_id, guru_nama, bentuk_soal, jumlah_nomor, instruksi, questions_json, status')
      .eq('jadwal_id', String(jadwalId || ''))
      .eq('guru_id', String(ctx.guru.id))
      .eq('kelas_target', kelasNama)
      .maybeSingle()

    if (soalRes.error && isExamColumnMissingError(soalRes.error)) {
      soalRes = await sb
        .from(EXAM_QUESTION_TABLE)
        .select('id, jadwal_id, guru_id, guru_nama, bentuk_soal, jumlah_nomor, instruksi, questions_json, status')
        .eq('jadwal_id', String(jadwalId || ''))
        .eq('guru_id', String(ctx.guru.id))
        .maybeSingle()
    }
    if (soalRes.error || !soalRes.data) {
      alert('Soal belum tersedia untuk export Word.')
      return
    }
    const fileName = `Soal ${sanitizeFileNamePart(jadwal.nama || 'Ujian')} - ${sanitizeFileNamePart(kelasNama || '-')}.doc`
    await exportExamWordFile({ ...jadwal, kelas: kelasNama }, soalRes.data, fileName)
  } catch (err) {
    console.error('exportGuruUjianByRowWord error:', err)
    alert(`Export Word gagal: ${String(err?.message || err || 'Unknown error')}`)
  }
}

async function setupRaporAccess(forceReload = false) {
  const isWaliKelas = await getIsWaliKelas(forceReload)
  const raporBtn = document.getElementById('guru-nav-rapor')
  const laporanBtn = document.querySelector('.guru-nav-btn[data-page="laporan"]')
  const laporanSubBtns = document.querySelectorAll('#guru-laporan-submenu .guru-submenu-btn')

  if (raporBtn) {
    raporBtn.disabled = !isWaliKelas
    if (!isWaliKelas) {
      raporBtn.title = 'Menu ini khusus wali kelas'
    } else {
      raporBtn.removeAttribute('title')
    }
  }

  if (laporanBtn) {
    laporanBtn.disabled = !isWaliKelas
    if (!isWaliKelas) {
      laporanBtn.title = 'Menu ini khusus wali kelas'
      closeGuruLaporanMenu()
    } else {
      laporanBtn.removeAttribute('title')
    }
  }

  laporanSubBtns.forEach(btn => {
    btn.disabled = !isWaliKelas
    if (!isWaliKelas) {
      btn.title = 'Menu ini khusus wali kelas'
    } else {
      btn.removeAttribute('title')
    }
  })

  return isWaliKelas
}

async function setupMonitoringAccess(forceReload = false) {
  const isAllowed = await getIsWakasekAkademik(forceReload)
  const btn = document.getElementById('guru-nav-monitoring')
  if (btn) btn.style.display = isAllowed ? '' : 'none'
  return isAllowed
}

async function loadGuruPage(page) {
  const requestedPage = String(page || DEFAULT_GURU_PAGE)
  const validPages = Object.keys(PAGE_TITLES)
  const targetPage = validPages.includes(requestedPage) ? requestedPage : DEFAULT_GURU_PAGE
  const contentEl = document.getElementById('guru-content')
  if (contentEl) contentEl.classList.remove('mapel-detail-locked')

  const isWaliKelas = await setupRaporAccess()
  const isWakasekAkademik = await setupMonitoringAccess()
  const isLaporanPage = targetPage === 'laporan' || targetPage === 'laporan-absensi' || targetPage === 'laporan-pekanan' || targetPage === 'laporan-bulanan'
  const isMonitoringPage = targetPage === 'monitoring'
  if ((targetPage === 'rapor' || isLaporanPage) && !isWaliKelas) {
    const blockedTitle = targetPage === 'rapor' ? 'Rapor' : 'Laporan'
    const blockedMessage = targetPage === 'rapor'
      ? 'Menu rapor hanya dapat diakses oleh guru dengan role wali kelas.'
      : 'Menu laporan hanya dapat diakses oleh guru dengan role wali kelas.'
    renderPlaceholder(blockedTitle, blockedMessage)
    setTopbarTitle(targetPage === 'rapor' ? 'rapor' : 'laporan')
    setNavActive('')
    closeTopbarUserMenu()
    return
  }
  if (isMonitoringPage && !isWakasekAkademik) {
    renderPlaceholder('Monitoring', 'Menu monitoring hanya dapat diakses oleh wakasek akademik.')
    setTopbarTitle('monitoring')
    setNavActive('')
    closeTopbarUserMenu()
    return
  }

  setTopbarTitle(targetPage)
  setNavActive(targetPage === 'profil' ? '' : targetPage)
  if (targetPage !== 'profil') localStorage.setItem(GURU_LAST_PAGE_KEY, targetPage)
  closeTopbarUserMenu()

  const cachedHtml = getGuruPageCache(targetPage)
  if (cachedHtml) {
    const content = document.getElementById('guru-content')
    if (content) {
      content.innerHTML = cachedHtml
      return
    }
  }

  switch (targetPage) {
    case 'dashboard':
      await renderDashboard()
      setGuruPageCache(targetPage)
      return
    case 'input':
    case 'input-nilai':
    case 'nilai':
      await renderInputNilaiPage()
      setGuruPageCache('input-nilai')
      return
    case 'input-absensi':
    case 'absensi':
      await renderAbsensiPage()
      setGuruPageCache('input-absensi')
      return
    case 'jadwal':
      await loadJadwalGuru()
      setGuruPageCache(targetPage)
      return
    case 'ujian':
      await renderUjianPage()
      setGuruPageCache(targetPage)
      return
    case 'mapel':
      await renderMapelPage()
      setGuruPageCache(targetPage)
      return
    case 'monitoring':
      await renderMonitoringPage()
      setGuruPageCache(targetPage)
      return
    case 'tugas':
      await renderTugasHarianPage()
      setGuruPageCache(targetPage)
      return
    case 'laporan':
    case 'laporan-pekanan':
      renderPlaceholder('Laporan Pekanan', 'Modul laporan pekanan disiapkan untuk rekap aktivitas mingguan.')
      setGuruPageCache('laporan-pekanan')
      return
    case 'laporan-absensi':
      await renderLaporanAbsensiPage()
      setGuruPageCache(targetPage)
      return
    case 'laporan-bulanan':
      await renderLaporanBulananPage()
      setGuruPageCache(targetPage)
      return
    case 'rapor':
      await renderRaporPage()
      setGuruPageCache(targetPage)
      return
    case 'profil':
      await renderGuruProfil()
      return
    default:
      renderPlaceholder('Panel Guru', 'Pilih menu di sidebar.')
  }
}

window.loadGuruPage = loadGuruPage
window.toggleGuruInputMenu = toggleGuruInputMenu
window.toggleGuruLaporanMenu = toggleGuruLaporanMenu
window.loadGuruInputFromSidebar = loadGuruInputFromSidebar
window.loadGuruLaporanFromSidebar = loadGuruLaporanFromSidebar
window.openGuruProfile = () => loadGuruPage('profil')
window.toggleTopbarUserMenu = toggleTopbarUserMenu
window.setGuruNotifRangeFilter = setGuruNotifRangeFilter
window.markAllGuruNotifRead = markAllGuruNotifRead
window.markGuruNotifItemRead = markGuruNotifItemRead
window.openGuruDashboardAgendaPopup = openGuruDashboardAgendaPopup
window.closeGuruDashboardAgendaPopup = closeGuruDashboardAgendaPopup
window.saveGuruProfil = saveGuruProfil
window.toggleGuruProfilePassword = toggleGuruProfilePassword
window.onAbsensiKelasChange = onAbsensiKelasChange
window.onAbsensiMapelChange = onAbsensiMapelChange
window.onAbsensiTanggalChange = onAbsensiTanggalChange
window.onAbsensiPenggantiToggle = onAbsensiPenggantiToggle
window.saveGuruAbsensi = saveGuruAbsensi
window.onInputNilaiKelasChange = onInputNilaiKelasChange
window.onInputNilaiMapelChange = onInputNilaiMapelChange
window.onInputNilaiJenisChange = onInputNilaiJenisChange
window.saveInputNilaiBatch = saveInputNilaiBatch
window.onLaporanBulananPeriodChange = onLaporanBulananPeriodChange
window.openLaporanBulananBulkInputModal = openLaporanBulananBulkInputModal
window.saveLaporanBulananBulkInput = saveLaporanBulananBulkInput
window.closeLaporanBulananBulkInputModal = closeLaporanBulananBulkInputModal
window.onLaporanBulananBulkAkhlakChange = onLaporanBulananBulkAkhlakChange
window.onLaporanBulananAkhlakHalaqahChange = onLaporanBulananAkhlakHalaqahChange
window.onLaporanBulananUjianBulananChange = onLaporanBulananUjianBulananChange
window.saveMonthlyWaTemplate = saveMonthlyWaTemplate
window.startMonthlyWaTemplateEdit = startMonthlyWaTemplateEdit
window.cancelMonthlyWaTemplateEdit = cancelMonthlyWaTemplateEdit
window.openLaporanBulananDetail = openLaporanBulananDetail
window.openLaporanAbsensiSantriDetail = openLaporanAbsensiSantriDetail
window.quickPrintLaporanBulanan = quickPrintLaporanBulanan
window.quickSendLaporanBulananWA = quickSendLaporanBulananWA
window.backToLaporanBulananList = backToLaporanBulananList
window.onLaporanBulananNilaiAkhlakChange = onLaporanBulananNilaiAkhlakChange
window.saveLaporanBulananDetail = saveLaporanBulananDetail
window.printLaporanBulanan = printLaporanBulanan
window.onRaporSemesterChange = onRaporSemesterChange
window.openRaporSantriDetail = openRaporSantriDetail
window.quickPrintRaporSantri = quickPrintRaporSantri
window.printRaporDetail = printRaporDetail
window.backToRaporList = backToRaporList
window.setMonitoringTab = setMonitoringTab
window.reloadMonitoringData = reloadMonitoringData
window.showMonitoringGuruDetail = showMonitoringGuruDetail
window.showMonitoringSantriDetail = showMonitoringSantriDetail
window.closeMonitoringDetailPopup = closeMonitoringDetailPopup
window.onMonitoringSantriModeChange = onMonitoringSantriModeChange
window.toggleMonitoringSantriSort = toggleMonitoringSantriSort
window.toggleGuruExamFolder = toggleGuruExamFolder
window.openGuruUjianEditorPage = openGuruUjianEditorPage
window.backToGuruUjianList = backToGuruUjianList
window.openGuruUjianEditor = openGuruUjianEditor
window.closeGuruUjianEditor = closeGuruUjianEditor
window.onGuruUjianCountChange = onGuruUjianCountChange
window.onGuruUjianShapeChange = onGuruUjianShapeChange
window.onGuruUjianSectionChange = onGuruUjianSectionChange
window.addGuruUjianSection = addGuruUjianSection
window.removeGuruUjianSection = removeGuruUjianSection
window.saveGuruUjian = saveGuruUjian
window.printGuruUjianByJadwal = printGuruUjianByJadwal
window.printGuruUjianByRow = printGuruUjianByRow
window.printGuruUjianActive = printGuruUjianActive
window.chooseAndPrintGuruUjianByRow = chooseAndPrintGuruUjianByRow
window.chooseAndPrintGuruUjianActive = chooseAndPrintGuruUjianActive
window.exportGuruUjianByRowWord = exportGuruUjianByRowWord
window.exportGuruUjianActiveWord = exportGuruUjianActiveWord
window.onMonitoringSantriClassFilterChange = onMonitoringSantriClassFilterChange
window.saveMapelRaporDesc = saveMapelRaporDesc
window.openMapelDetail = openMapelDetail
window.toggleGuruAvailableMapelSection = toggleGuruAvailableMapelSection
window.clearSelectedGuruMapelClaim = clearSelectedGuruMapelClaim
window.claimSelectedGuruMapel = claimSelectedGuruMapel
window.goBackToMapelList = goBackToMapelList
window.openMapelNilaiDetail = openMapelNilaiDetail
window.closeNilaiDetailModal = closeNilaiDetailModal
window.addNilaiDetailRow = addNilaiDetailRow
window.updateNilaiDetailRow = updateNilaiDetailRow
window.removeNilaiDetailRow = removeNilaiDetailRow
window.saveNilaiDetailChanges = saveNilaiDetailChanges
window.setMapelDetailTab = setMapelDetailTab
window.startMapelAbsensiEdit = startMapelAbsensiEdit
window.cancelMapelAbsensiEdit = cancelMapelAbsensiEdit
window.saveMapelAbsensiEdit = saveMapelAbsensiEdit
window.deleteMapelAbsensiDate = deleteMapelAbsensiDate
window.onMapelNilaiInput = onMapelNilaiInput
window.reloadGuruDailyTask = reloadGuruDailyTask
window.submitGuruDailyTask = submitGuruDailyTask

document.addEventListener('DOMContentLoaded', () => {
  setupCustomPopupSystem()
  loadGuruNotifPrefs()
  ensureTopbarNotification()
  refreshGuruTopbarNotifications().catch(error => console.error(error))
  setupRaporAccess(true)
  setupMonitoringAccess(true).catch(error => console.error(error))
  setGuruWelcomeName()
  const lastPage = localStorage.getItem(GURU_LAST_PAGE_KEY) || DEFAULT_GURU_PAGE
  loadGuruPage(lastPage)

  document.addEventListener('click', event => {
    const wrap = document.querySelector('.topbar-user-menu-wrap')
    if (!wrap) return
    if (!wrap.contains(event.target)) {
      closeTopbarUserMenu()
      closeTopbarNotifMenu()
    }
  })
})


