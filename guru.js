const supabaseUrl = 'https://optucpelkueqmlhwlbej.supabase.co'
const supabaseKey = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9wdHVjcGVsa3VlcW1saHdsYmVqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzAxOTY4MTgsImV4cCI6MjA4NTc3MjgxOH0.Vqaey9pcnltu9uRbPk0J-AGWaGDZjQLw92pcRv67GNE'
const sb = supabase.createClient(supabaseUrl, supabaseKey)

const GURU_LAST_PAGE_KEY = 'guru_last_page'
const GURU_MAPEL_DETAIL_STATE_KEY = 'guru_mapel_detail_state'
const DEFAULT_GURU_PAGE = 'dashboard'
const ATTENDANCE_TABLE = 'absensi_santri'
const INPUT_NILAI_TABLE = 'nilai_input_akademik'
const ATTENDANCE_STATUSES = ['Hadir', 'Terlambat', 'Sakit', 'Izin', 'Alpa']
const INPUT_NILAI_JENIS_LIST = ['Tugas', 'Ulangan Harian', 'UTS', 'UAS', 'Keterampilan']

const PAGE_TITLES = {
  dashboard: 'Dashboard',
  input: 'Input',
  'input-nilai': 'Input Nilai',
  'input-absensi': 'Input Absen',
  laporan: 'Laporan',
  'laporan-pekanan': 'Laporan Pekanan',
  'laporan-bulanan': 'Laporan Bulanan',
  jadwal: 'Jadwal',
  mapel: 'Mapel',
  absensi: 'Absensi',
  tugas: 'Tugas Harian',
  nilai: 'Input Nilai',
  rapor: 'Rapor',
  profil: 'Profil'
}

let guruContextCache = null
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
  titleEl.textContent = PAGE_TITLES[page] || 'Panel Guru'
}

function setNavActive(page) {
  const mainButtons = document.querySelectorAll('.guru-nav-btn')
  const subButtons = document.querySelectorAll('.guru-submenu-btn')

  mainButtons.forEach(btn => btn.classList.remove('active'))
  subButtons.forEach(btn => btn.classList.remove('active'))

  if (!page) return

  const isInputPage = page === 'input-nilai' || page === 'input-absensi' || page === 'nilai' || page === 'absensi'
  const isLaporanPage = page === 'laporan-pekanan' || page === 'laporan-bulanan' || page === 'laporan'

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
    const subPage = page === 'laporan-bulanan' ? 'laporan-bulanan' : 'laporan-pekanan'
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

function renderDashboard() {
  const content = document.getElementById('guru-content')
  if (!content) return

  content.innerHTML = `
    <div class="placeholder-card">
      <div style="font-size:15px; font-weight:700; margin-bottom:8px;">Ringkasan Guru</div>
      <div style="font-size:13px; line-height:1.6;">
        Gunakan menu di sidebar: Dashboard, Tugas Harian, Jadwal, Input, Laporan, dan Rapor (khusus wali kelas).
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
  const { data: tahunAktif } = await sb
    .from('tahun_ajaran')
    .select('id')
    .eq('aktif', true)
    .order('id', { ascending: false })
    .limit(1)

  const tahunAjaranId = tahunAktif?.[0]?.id || null

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
      activeSemester: null,
      distribusiList: [],
      activeDistribusiList: [],
      kelasMap: new Map(),
      mapelMap: new Map(),
      semesterMap: new Map(),
      jadwalList: [],
      jamList: []
    }
    return guruContextCache
  }

  const activeSemester = await getActiveSemester()

  const distribusiRes = await sb
    .from('distribusi_mapel')
    .select('id, kelas_id, mapel_id, guru_id, semester_id')
    .eq('guru_id', guru.id)

  if (distribusiRes.error) throw distribusiRes.error

  const distribusiList = distribusiRes.data || []
  const kelasIds = [...new Set(distribusiList.map(item => item.kelas_id).filter(Boolean))]
  const mapelIds = [...new Set(distribusiList.map(item => item.mapel_id).filter(Boolean))]
  const semesterIds = [...new Set(distribusiList.map(item => item.semester_id).filter(Boolean))]
  const distribusiIds = [...new Set(distribusiList.map(item => item.id).filter(Boolean))]

  const [kelasRes, mapelRes, semesterRes, jadwalRes, jamRes] = await Promise.all([
    kelasIds.length ? sb.from('kelas').select('id, nama_kelas, tingkat').in('id', kelasIds) : Promise.resolve({ data: [] }),
    getMapelRowsByIds(mapelIds),
    semesterIds.length ? sb.from('semester').select('id, nama, aktif').in('id', semesterIds) : Promise.resolve({ data: [] }),
    distribusiIds.length ? sb.from('jadwal_pelajaran').select('id, distribusi_id, hari, jam_mulai, jam_selesai').in('distribusi_id', distribusiIds) : Promise.resolve({ data: [] }),
    sb.from('jam_pelajaran').select('id, nama, jam_mulai, jam_selesai, aktif, urutan').order('urutan', { ascending: true }).order('jam_mulai', { ascending: true })
  ])

  if (kelasRes.error) console.error(kelasRes.error)
  if (mapelRes.error) console.error(mapelRes.error)
  if (semesterRes.error) console.error(semesterRes.error)
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
  const semesterMap = new Map((semesterRes.data || []).map(item => [String(item.id), item]))

  const activeDistribusiList = activeSemester?.id
    ? distribusiList.filter(item => String(item.semester_id || '') === String(activeSemester.id))
    : distribusiList

  guruContextCache = {
    guru,
    activeSemester,
    distribusiList,
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
  const { data, error } = await sb
    .from('santri')
    .select('id, nama, kelas_id')
    .eq('kelas_id', kelasId)
    .order('nama')

  if (error) {
    console.error(error)
    return []
  }
  return data || []
}

function buildAbsensiMissingTableMessage() {
  return `Tabel '${ATTENDANCE_TABLE}' belum ada di Supabase.\n\nSilakan buat tabel dulu dengan kolom minimal:\n- id (primary key)\n- tanggal (date)\n- kelas_id\n- mapel_id\n- guru_id\n- jam_pelajaran_id (nullable)\n- semester_id (nullable)\n- distribusi_id (nullable)\n- santri_id\n- status (text)`
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

async function saveGuruAbsensi() {
  const tanggal = String(document.getElementById('absensi-tanggal')?.value || '').trim()
  const kelasId = String(document.getElementById('absensi-kelas')?.value || '').trim()
  const mapelId = String(document.getElementById('absensi-mapel')?.value || '').trim()
  const jamId = String(document.getElementById('absensi-jam')?.value || '').trim()

  if (!tanggal || !kelasId || !mapelId) {
    alert('Tanggal, kelas, dan mapel wajib diisi.')
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
    jam_pelajaran_id: jamId ? Number(jamId) : null,
    semester_id: distribusi?.semester_id ? String(distribusi.semester_id) : (ctx.activeSemester?.id ? String(ctx.activeSemester.id) : null),
    distribusi_id: distribusi?.id ? String(distribusi.id) : null,
    santri_id: String(santri.id),
    status: statusMap.get(String(santri.id)) || 'Hadir'
  }))

  const { error } = await sb.from(ATTENDANCE_TABLE).insert(payloads)

  if (error) {
    console.error(error)
    const msg = String(error.message || '')
    if (isMissingAbsensiTableError(error)) {
      alert(buildAbsensiMissingTableMessage())
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
    const nilai_keterampilan = agg.nilai_keterampilan?.count ? round2(agg.nilai_keterampilan.total / agg.nilai_keterampilan.count) : (existing.nilai_keterampilan ?? null)
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

  const distribusiList = ctx.distribusiList || []
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

  const list = ctx.distribusiList || []
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

  const distribusi = (ctx.distribusiList || []).find(item => String(item.id) === currentMapelDetailDistribusiId)
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

function setupRaporAccess() {
  const roles = parseRoleList()
  const isWaliKelas = roles.some(isWaliKelasRole)
  const raporBtn = document.getElementById('guru-nav-rapor')
  if (!raporBtn) return isWaliKelas

  raporBtn.disabled = !isWaliKelas
  if (!isWaliKelas) {
    raporBtn.title = 'Menu ini khusus wali kelas'
  } else {
    raporBtn.removeAttribute('title')
  }

  return isWaliKelas
}

async function loadGuruPage(page) {
  const requestedPage = String(page || DEFAULT_GURU_PAGE)
  const validPages = Object.keys(PAGE_TITLES)
  const targetPage = validPages.includes(requestedPage) ? requestedPage : DEFAULT_GURU_PAGE

  const isWaliKelas = setupRaporAccess()
  if (targetPage === 'rapor' && !isWaliKelas) {
    renderPlaceholder('Rapor', 'Menu rapor hanya dapat diakses oleh guru dengan role wali kelas.')
    setTopbarTitle('rapor')
    setNavActive('rapor')
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
      renderPlaceholder('Tugas Harian', 'Modul tugas harian siap sebagai fondasi untuk pengembangan berikutnya.')
      return
    case 'laporan':
    case 'laporan-pekanan':
      renderPlaceholder('Laporan Pekanan', 'Modul laporan pekanan disiapkan untuk rekap aktivitas mingguan.')
      return
    case 'laporan-bulanan':
      renderPlaceholder('Laporan Bulanan', 'Modul laporan bulanan disiapkan untuk rekap aktivitas mengajar.')
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
window.saveGuruAbsensi = saveGuruAbsensi
window.onInputNilaiKelasChange = onInputNilaiKelasChange
window.onInputNilaiMapelChange = onInputNilaiMapelChange
window.onInputNilaiJenisChange = onInputNilaiJenisChange
window.saveInputNilaiBatch = saveInputNilaiBatch
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

document.addEventListener('DOMContentLoaded', () => {
  setupCustomPopupSystem()
  setupRaporAccess()
  setGuruWelcomeName()
  const lastPage = localStorage.getItem(GURU_LAST_PAGE_KEY) || DEFAULT_GURU_PAGE
  loadGuruPage(lastPage)

  document.addEventListener('click', event => {
    const wrap = document.querySelector('.topbar-user-menu-wrap')
    if (!wrap) return
    if (!wrap.contains(event.target)) closeTopbarUserMenu()
  })
})
