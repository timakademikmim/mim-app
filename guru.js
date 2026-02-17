const supabaseUrl = 'https://optucpelkueqmlhwlbej.supabase.co'
const supabaseKey = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9wdHVjcGVsa3VlcW1saHdsYmVqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzAxOTY4MTgsImV4cCI6MjA4NTc3MjgxOH0.Vqaey9pcnltu9uRbPk0J-AGWaGDZjQLw92pcRv67GNE'
const sb = supabase.createClient(supabaseUrl, supabaseKey)

const GURU_LAST_PAGE_KEY = 'guru_last_page'
const GURU_MAPEL_DETAIL_STATE_KEY = 'guru_mapel_detail_state'
const DEFAULT_GURU_PAGE = 'dashboard'
const ATTENDANCE_TABLE = 'absensi_santri'
const INPUT_NILAI_TABLE = 'nilai_input_akademik'
const MONTHLY_REPORT_TABLE = 'laporan_bulanan_wali'
const MONTHLY_REPORT_STORAGE_BUCKET = 'laporan-bulanan'
const MONTHLY_REPORT_WA_TEMPLATE_KEY = 'laporan_bulanan_wa_template'
const DAILY_TASK_TEMPLATE_TABLE = 'tugas_harian_template'
const DAILY_TASK_SUBMIT_TABLE = 'tugas_harian_submit'
const TOPBAR_KALENDER_TABLE = 'kalender_akademik'
const TOPBAR_KALENDER_CACHE_KEY = 'kalender_akademik:list'
const TOPBAR_KALENDER_CACHE_TTL_MS = 2 * 60 * 1000
const TOPBAR_KALENDER_DEFAULT_COLOR = '#2563eb'
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
  mapel: 'Mapel',
  absensi: 'Absensi',
  tugas: 'Mutabaah',
  nilai: 'Input Nilai',
  rapor: 'Rapor',
  profil: 'Profil'
}

let guruContextCache = null
let activeTahunAjaranCache = null
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
let topbarKalenderState = {
  list: [],
  month: '',
  selectedDateKey: '',
  visible: false
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
  const payload = {
    distribusiId: String(distribusiId || ''),
    tab: tab === 'nilai' ? 'nilai' : 'absensi'
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
    const tab = parsed.tab === 'nilai' ? 'nilai' : 'absensi'
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
  document.getElementById('guru-input-submenu')?.classList.add('open')
  document.querySelector('.guru-nav-btn[data-page="input"]')?.classList.add('expanded')
}

function closeGuruInputMenu() {
  document.getElementById('guru-input-submenu')?.classList.remove('open')
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
  document.getElementById('guru-laporan-submenu')?.classList.add('open')
  document.querySelector('.guru-nav-btn[data-page="laporan"]')?.classList.add('expanded')
}

function closeGuruLaporanMenu() {
  document.getElementById('guru-laporan-submenu')?.classList.remove('open')
  document.querySelector('.guru-nav-btn[data-page="laporan"]')?.classList.remove('expanded')
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

async function loadGuruDailyTaskData(periode) {
  const range = getPeriodeRange(periode)
  if (!range) throw new Error('Periode tidak valid')

  const ctx = await getGuruContext(true)
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
    templates: templateRes.data || [],
    submissions: submitRes.data || []
  }
}

function renderGuruDailyTaskRows() {
  const box = document.getElementById('guru-tugas-list')
  if (!box) return
  const tanggal = String(guruDailyTaskState.tanggal || '')
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
  const tanggalDefault = guruDailyTaskState.tanggal || new Date().toISOString().slice(0, 10)

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
  const tanggal = String(document.getElementById('guru-tugas-tanggal')?.value || new Date().toISOString().slice(0, 10))
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
  await renderTugasHarianPage(true)
}

function renderDashboard() {
  const content = document.getElementById('guru-content')
  if (!content) return

  content.innerHTML = `
    <div class="placeholder-card">
      <div style="font-size:15px; font-weight:700; margin-bottom:8px;">Ringkasan Guru</div>
      <div style="font-size:13px; line-height:1.6;">
        Gunakan menu di sidebar: Dashboard, Mutabaah, Jadwal, Input, Laporan, dan Rapor (khusus wali kelas).
      </div>
    </div>
  `
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
  menu.classList.toggle('open')
}

function closeTopbarUserMenu() {
  const menu = document.getElementById('topbar-user-menu')
  if (!menu) return
  menu.classList.remove('open')
}

async function getCurrentGuruRow() {
  const loginId = localStorage.getItem('login_id')
  if (!loginId) return null

  const { data, error } = await sb
    .from('karyawan')
    .select('id, id_karyawan, nama, role, no_hp, alamat, aktif')
    .eq('id_karyawan', loginId)
    .maybeSingle()

  if (error) throw error
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

  let query = sb
    .from('semester')
    .select('id, nama, aktif, tahun_ajaran_id')
    .order('id', { ascending: false })

  if (tahunAjaranId) query = query.eq('tahun_ajaran_id', tahunAjaranId)

  const { data, error } = await query
  if (error) {
    console.error(error)
    return null
  }

  const rows = data || []
  return rows.find(item => asBool(item.aktif)) || rows[0] || null
}

async function getMapelRowsByIds(mapelIds = []) {
  if (!mapelIds.length) return { data: [], error: null }

  const attempts = [
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

function isMissingMonthlyReportTableError(error) {
  const code = String(error?.code || '').toUpperCase()
  const msg = String(error?.message || '').toLowerCase()
  if (code === '42P01') return true
  if (msg.includes(`table 'public.${MONTHLY_REPORT_TABLE}'`.toLowerCase())) return true
  if (msg.includes('relation') && msg.includes(MONTHLY_REPORT_TABLE.toLowerCase())) return true
  return false
}

function buildMonthlyReportMissingTableMessage() {
  return `Tabel '${MONTHLY_REPORT_TABLE}' belum ada di Supabase.\n\nSilakan buat tabel dengan kolom minimal:\n- id (primary key)\n- periode (text, format: YYYY-MM)\n- guru_id\n- kelas_id\n- santri_id\n- nilai_akhlak (numeric, nullable)\n- predikat (text, nullable)\n- catatan_wali (text, nullable)\n\nDisarankan unique key: (periode, guru_id, kelas_id, santri_id).`
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

function renderAbsensiMapelOptions() {
  const mapelSelect = document.getElementById('absensi-mapel')
  if (!mapelSelect) return

  const kelasId = String(document.getElementById('absensi-kelas')?.value || '')
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

function renderAbsensiJamOptions() {
  const jamSelect = document.getElementById('absensi-jam')
  if (!jamSelect) return

  const candidates = getAbsensiDistribusiCandidates()
  const ctx = guruContextCache

  const jadwalMap = new Map()
  ;(ctx?.jadwalList || []).forEach(item => {
    if (!candidates.find(c => String(c.id) === String(item.distribusi_id))) return
    jadwalMap.set(String(item.id), item)
  })

  jamSelect.innerHTML = '<option value="">-- Pilih Jam --</option>'

  const jamByRange = new Map((ctx?.jamList || []).map(item => [`${toTimeLabel(item.jam_mulai)}|${toTimeLabel(item.jam_selesai)}`, item]))

  const jadwalList = Array.from(jadwalMap.values()).sort((a, b) => {
    const dayCmp = getHariOrder(a.hari) - getHariOrder(b.hari)
    if (dayCmp !== 0) return dayCmp
    return String(a.jam_mulai || '').localeCompare(String(b.jam_mulai || ''))
  })

  if (!jadwalList.length) {
    ;(ctx?.jamList || []).forEach(item => {
      const opt = document.createElement('option')
      opt.value = String(item.id)
      opt.textContent = `${item.nama || 'Jam'} (${toTimeLabel(item.jam_mulai)}-${toTimeLabel(item.jam_selesai)})`
      jamSelect.appendChild(opt)
    })
    return
  }

  jadwalList.forEach(item => {
    const key = `${toTimeLabel(item.jam_mulai)}|${toTimeLabel(item.jam_selesai)}`
    const jam = jamByRange.get(key)
    const opt = document.createElement('option')
    opt.value = jam?.id ? String(jam.id) : ''
    const jamNama = jam?.nama || `${toTimeLabel(item.jam_mulai)}-${toTimeLabel(item.jam_selesai)}`
    opt.textContent = `${getHariLabel(item.hari)} - ${jamNama}`
    jamSelect.appendChild(opt)
  })
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
          <input id="absensi-tanggal" class="guru-field" type="date" value="${today}">
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
          <label class="guru-label">Jam Pelajaran</label>
          <select id="absensi-jam" class="guru-field">
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
        <button type="button" class="modal-btn modal-btn-primary" onclick="saveGuruAbsensi()">Simpan Absensi</button>
      </div>
    </div>
  `

  currentAbsensiSantriList = []
}

async function onAbsensiKelasChange() {
  renderAbsensiMapelOptions()
  document.getElementById('absensi-mapel').value = ''
  renderAbsensiJamOptions()

  const kelasId = String(document.getElementById('absensi-kelas')?.value || '')
  currentAbsensiSantriList = await getSantriByKelas(kelasId)
  renderAbsensiSantriRows()
}

async function onAbsensiMapelChange() {
  await handleAbsensiKelasMapelChange()
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
  const tanggal = String(document.getElementById('absensi-tanggal')?.value || '').trim()
  const kelasId = String(document.getElementById('absensi-kelas')?.value || '').trim()
  const mapelId = String(document.getElementById('absensi-mapel')?.value || '').trim()
  const jamId = String(document.getElementById('absensi-jam')?.value || '').trim()
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

  const statusMap = new Map()
  document.querySelectorAll('[data-absen-santri-id]').forEach(selectEl => {
    const santriId = String(selectEl.getAttribute('data-absen-santri-id') || '').trim()
    const status = String(selectEl.value || '').trim() || 'Hadir'
    if (!santriId) return
    statusMap.set(santriId, status)
  })

  const payloads = currentAbsensiSantriList.map(santri => ({
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

  const { error } = await sb
    .from(ATTENDANCE_TABLE)
    .upsert(payloads, {
      onConflict: 'tanggal,kelas_id,mapel_id,santri_id'
    })

  if (error) {
    console.error(error)
    const msg = String(error.message || '')
    if (isMissingAbsensiTableError(error)) {
      alert(buildAbsensiMissingTableMessage())
      return
    }
    if (isMissingAbsensiPenggantiColumnError(error)) {
      alert(buildAbsensiPenggantiColumnsMessage())
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
  document.querySelectorAll('[data-input-nilai-santri-id]').forEach(inputEl => {
    inputEl.value = ''
  })
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
        <button type="button" class="modal-btn modal-btn-primary" onclick="saveInputNilaiBatch()">Simpan Input Nilai</button>
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
        <input id="guru-profil-id-karyawan" type="text" value="${escapeHtml(guru.id_karyawan || '')}" disabled class="guru-field" style="background:#f8fafc; color:#64748b;">
      </div>
      <div style="margin-bottom:10px;">
        <label class="guru-label">Nama</label>
        <input id="guru-profil-nama" type="text" value="${escapeHtml(guru.nama || '')}" class="guru-field">
      </div>
      <div style="margin-bottom:10px;">
        <label class="guru-label">No HP</label>
        <input id="guru-profil-no-hp" type="text" value="${escapeHtml(guru.no_hp || '')}" class="guru-field">
      </div>
      <div style="margin-bottom:10px;">
        <label class="guru-label">Alamat</label>
        <input id="guru-profil-alamat" type="text" value="${escapeHtml(guru.alamat || '')}" class="guru-field">
      </div>
      <div style="margin-bottom:12px;">
        <label class="guru-label">Password Baru (opsional)</label>
        <input id="guru-profil-password" type="password" placeholder="Isi jika ingin ganti password" class="guru-field">
      </div>
      <button type="button" class="modal-btn modal-btn-primary" onclick="saveGuruProfil('${escapeHtml(guru.id)}')">Simpan Profil</button>
    </div>
  `
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
      <div style="font-size:13px; color:#475569;">Klik detail untuk melihat laporan bulanan per santri.</div>
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
    nilaiAkhlak,
    predikat,
    catatanWali,
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

    <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap:10px; margin-bottom:12px;">
      <div class="placeholder-card"><strong>Periode</strong><br>${escapeHtml(getPeriodeLabel(periode))}</div>
      <div class="placeholder-card"><strong>Nama</strong><br>${escapeHtml(santri.nama || '-')}</div>
      <div class="placeholder-card"><strong>Kelas</strong><br>${escapeHtml(kelas?.nama_kelas || '-')}</div>
      <div class="placeholder-card"><strong>Wali Kelas</strong><br>${escapeHtml(guru?.nama || '-')}</div>
      <div class="placeholder-card"><strong>Nomor HP</strong><br>${escapeHtml(hpGuru)}</div>
    </div>

    <div class="placeholder-card" style="margin-bottom:12px;">
      <div style="font-weight:700; margin-bottom:8px;">Kehadiran di Kelas</div>
      <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap:8px;">
        <div><strong>Nilai Kehadiran:</strong> ${escapeHtml(String(nilaiKehadiranPersen))}%</div>
        <div><strong>Keterangan Kehadiran:</strong> Sakit ${sakitCount} kali, Izin ${izinCount} kali</div>
      </div>
    </div>

    <div class="placeholder-card" style="margin-bottom:12px;">
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
  const reportRes = await sb
    .from(MONTHLY_REPORT_TABLE)
    .select('id, nilai_akhlak, predikat, catatan_wali')
    .eq('periode', periode)
    .eq('guru_id', String(guru.id))
    .eq('kelas_id', String(santri.kelas_id))
    .eq('santri_id', sid)
    .maybeSingle()

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
    catatanWali: catatanWali || '-'
  }

  return {
    santri,
    guru,
    kelas,
    periode,
    periodeRange,
    monthlyReportMissingTable,
    nilaiAkhlak,
    predikat,
    catatanWali,
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
    alert(`Gagal simpan laporan bulanan: ${error.message || 'Unknown error'}`)
    return
  }

  alert('Laporan bulanan berhasil disimpan.')
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
  } else {
    alert('Plugin tabel PDF belum termuat. Refresh halaman lalu coba lagi.')
    return null
  }

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
  if (!list.length) {
    content.innerHTML = '<div class="placeholder-card">Belum ada data mapel untuk guru ini.</div>'
    return
  }

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
      const kelasCmp = a.kelasNama.localeCompare(b.kelasNama)
      if (kelasCmp !== 0) return kelasCmp
      return a.mapelLabel.localeCompare(b.mapelLabel)
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

  let html = '<div style="display:grid; grid-template-columns:repeat(auto-fit, minmax(260px, 1fr)); gap:12px;">'

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
async function openMapelDetail(distribusiId, tab = 'absensi') {
  const nextDistribusiId = String(distribusiId || '')
  const isSameDistribusi = nextDistribusiId && nextDistribusiId === currentMapelDetailDistribusiId
  if (!isSameDistribusi) {
    currentMapelEditMode = { absensi: false, nilai: false }
  }
  currentMapelDetailDistribusiId = nextDistribusiId
  currentMapelDetailTab = tab === 'nilai' ? 'nilai' : 'absensi'
  saveMapelDetailState(currentMapelDetailDistribusiId, currentMapelDetailTab)
  currentMapelDetailState = null
  const content = document.getElementById('guru-content')
  if (!content || !currentMapelDetailDistribusiId) return

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
  const mapel = ctx.mapelMap.get(String(distribusi.mapel_id || ''))
  const semester = ctx.semesterMap.get(String(distribusi.semester_id || ''))

  const santriList = await getSantriByKelas(distribusi.kelas_id)
  const santriMap = new Map((santriList || []).map(item => [String(item.id), item]))

  const [nilaiRes, absensiRes] = await Promise.all([
    sb
      .from('nilai_akademik')
      .select('*')
      .eq('mapel_id', distribusi.mapel_id)
      .eq('semester_id', distribusi.semester_id),
    sb
      .from(ATTENDANCE_TABLE)
      .select('*')
      .order('tanggal', { ascending: false })
      .limit(2000)
  ])

  let absensiErrorMessage = ''
  let absensiRows = []
  if (absensiRes.error) {
    const msg = String(absensiRes.error.message || '')
    if (isMissingAbsensiTableError(absensiRes.error)) {
      absensiErrorMessage = buildAbsensiMissingTableMessage()
    } else {
      absensiErrorMessage = `Gagal load absensi: ${msg || 'Unknown error'}`
    }
  } else {
    absensiRows = (absensiRes.data || []).filter(row => {
      const byDistribusi = row.distribusi_id !== undefined && row.distribusi_id !== null
        ? String(row.distribusi_id) === String(distribusi.id)
        : false
      if (byDistribusi) return true

      const matchKelas = row.kelas_id !== undefined ? String(row.kelas_id) === String(distribusi.kelas_id) : true
      const matchMapel = row.mapel_id !== undefined ? String(row.mapel_id) === String(distribusi.mapel_id) : true
      const matchSemester = row.semester_id !== undefined ? String(row.semester_id) === String(distribusi.semester_id) : true
      return matchKelas && matchMapel && matchSemester
    })
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
          <td style="padding:8px; border:1px solid #e2e8f0; text-align:center; width:60px;">${index + 1}</td>
          <td style="padding:8px; border:1px solid #e2e8f0; min-width:200px;">${escapeHtml(santri.nama || '-')}</td>
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
        <button type="button" class="modal-btn modal-btn-danger" style="padding:4px 8px; font-size:11px; margin-top:4px;" onclick="deleteMapelAbsensiDate('${escapeHtml(tanggal)}')">Hapus</button>
      </th>`
    })
    .join('')

  const buildNilaiCellBtn = (santriId, jenis, value) => {
    const text = value === null || value === undefined || value === '' ? '-' : String(value)
    return `<button type="button" class="nilai-click-btn" onclick="openMapelNilaiDetail('${escapeHtml(santriId)}','${escapeHtml(jenis)}')">${escapeHtml(text)}</button>`
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
    <div style="display:flex; align-items:center; gap:10px; margin-bottom:12px;">
      <button type="button" class="mapel-back-btn" onclick="goBackToMapelList()">&lt;</button>
      <div style="font-weight:700; color:#0f172a;">${escapeHtml(getMapelLabel(mapel))} - ${escapeHtml(kelas?.nama_kelas || '-')} - ${escapeHtml(getSemesterLabel(semester))}</div>
    </div>

    <div class="mapel-detail-tabs" style="margin-bottom:12px;">
      <button type="button" class="mapel-detail-tab-btn ${currentMapelDetailTab === 'absensi' ? 'active' : ''}" data-mapel-detail-tab="absensi" onclick="setMapelDetailTab('absensi')">Absensi</button>
      <button type="button" class="mapel-detail-tab-btn ${currentMapelDetailTab === 'nilai' ? 'active' : ''}" data-mapel-detail-tab="nilai" onclick="setMapelDetailTab('nilai')">Nilai</button>
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
          ? `<div style="overflow:auto; border:1px solid #e2e8f0; border-radius:10px;"><table style="width:100%; min-width:780px; border-collapse:collapse; font-size:13px;"><thead><tr style="background:#f8fafc;"><th style="padding:8px; border:1px solid #e2e8f0; width:60px;">No</th><th style="padding:8px; border:1px solid #e2e8f0; min-width:200px; text-align:left;">Nama</th>${absensiDateHeaderHtml}</tr></thead><tbody>${absensiPivotRowsHtml}</tbody></table></div>
             ${editAbsensi ? '<div style="margin-top:10px;"><button type="button" class="modal-btn modal-btn-primary" onclick="saveMapelAbsensiEdit()">Simpan Perubahan Absensi</button></div>' : ''}`
          : '<div class="placeholder-card">Belum ada data absensi.</div>'
      }
    </div>

    <div id="mapel-detail-pane-nilai" class="mapel-detail-pane ${currentMapelDetailTab === 'nilai' ? 'active' : ''}">
      <div style="display:flex; align-items:center; justify-content:space-between; gap:10px; margin-bottom:8px; flex-wrap:wrap;">
        <div class="mapel-section-title" style="margin-bottom:0;">Nilai</div>
        <div style="font-size:12px; color:#64748b;">Klik nilai untuk melihat detail input</div>
      </div>
      <div style="overflow:auto; border:1px solid #e2e8f0; border-radius:10px;">
        <table style="width:100%; min-width:980px; border-collapse:collapse; font-size:13px;">
          <thead>
            <tr style="background:#f8fafc;">
              <th style="padding:8px; border:1px solid #e2e8f0; width:60px;">No</th>
              <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Nama Siswa</th>
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
            ${nilaiRowsHtml || '<tr><td colspan="9" style="padding:10px; text-align:center; border:1px solid #e2e8f0;">Belum ada data siswa.</td></tr>'}
          </tbody>
        </table>
      </div>
    </div>
  `
}

function setMapelDetailTab(tab) {
  const validTab = tab === 'nilai' ? 'nilai' : 'absensi'
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
  await openMapelDetail(currentMapelDetailDistribusiId, 'absensi')
}

async function deleteMapelAbsensiDate(tanggal) {
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

async function loadGuruPage(page) {
  const requestedPage = String(page || DEFAULT_GURU_PAGE)
  const validPages = Object.keys(PAGE_TITLES)
  const targetPage = validPages.includes(requestedPage) ? requestedPage : DEFAULT_GURU_PAGE

  const isWaliKelas = await setupRaporAccess()
  const isLaporanPage = targetPage === 'laporan' || targetPage === 'laporan-absensi' || targetPage === 'laporan-pekanan' || targetPage === 'laporan-bulanan'
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

  setTopbarTitle(targetPage)
  setNavActive(targetPage === 'profil' ? '' : targetPage)
  if (targetPage !== 'profil') localStorage.setItem(GURU_LAST_PAGE_KEY, targetPage)
  closeTopbarUserMenu()

  switch (targetPage) {
    case 'dashboard':
      renderDashboard()
      return
    case 'input':
    case 'input-nilai':
    case 'nilai':
      await renderInputNilaiPage()
      return
    case 'input-absensi':
    case 'absensi':
      await renderAbsensiPage()
      return
    case 'jadwal':
      await loadJadwalGuru()
      return
    case 'mapel':
      await renderMapelPage()
      return
    case 'tugas':
      await renderTugasHarianPage()
      return
    case 'laporan':
    case 'laporan-pekanan':
      renderPlaceholder('Laporan Pekanan', 'Modul laporan pekanan disiapkan untuk rekap aktivitas mingguan.')
      return
    case 'laporan-absensi':
      await renderLaporanAbsensiPage()
      return
    case 'laporan-bulanan':
      await renderLaporanBulananPage()
      return
    case 'rapor':
      renderPlaceholder('Rapor', 'Modul rapor wali kelas disiapkan sebagai dasar pengembangan tahap berikutnya.')
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
window.saveGuruProfil = saveGuruProfil
window.onAbsensiKelasChange = onAbsensiKelasChange
window.onAbsensiMapelChange = onAbsensiMapelChange
window.onAbsensiPenggantiToggle = onAbsensiPenggantiToggle
window.saveGuruAbsensi = saveGuruAbsensi
window.onInputNilaiKelasChange = onInputNilaiKelasChange
window.onInputNilaiMapelChange = onInputNilaiMapelChange
window.onInputNilaiJenisChange = onInputNilaiJenisChange
window.saveInputNilaiBatch = saveInputNilaiBatch
window.onLaporanBulananPeriodChange = onLaporanBulananPeriodChange
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
window.openMapelDetail = openMapelDetail
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
  setupRaporAccess(true)
  setGuruWelcomeName()
  const lastPage = localStorage.getItem(GURU_LAST_PAGE_KEY) || DEFAULT_GURU_PAGE
  loadGuruPage(lastPage)

  document.addEventListener('click', event => {
    const wrap = document.querySelector('.topbar-user-menu-wrap')
    if (!wrap) return
    if (!wrap.contains(event.target)) closeTopbarUserMenu()
  })
})
