let currentEditId = null
let currentEditDistribusiMapelId = null
let currentEditMapelId = null
let currentDistribusiTingkatanView = 'all'
let distribusiSortField = 'kelas'
let distribusiSortDirection = 'asc'
let distribusiSearchKeyword = ''
const distribusiSelectFilters = {
  tingkatan: '',
  kelas: '',
  semester: '',
  guru: ''
}
let currentDistribusiPayload = null
const KELAS_CACHE_KEY = 'kelas:list'
const KELAS_CACHE_TTL_MS = 2 * 60 * 1000
const DISTRIBUSI_MAPEL_CACHE_KEY = 'distribusi_mapel:list'
const DISTRIBUSI_MAPEL_CACHE_TTL_MS = 2 * 60 * 1000
const MAPEL_CACHE_KEY = 'mapel:list'
const MAPEL_CACHE_TTL_MS = 2 * 60 * 1000
let kelasActiveSubtab = 'data-kelas'

function parseRoleList(rawRole) {
  if (Array.isArray(rawRole)) {
    return rawRole
      .map(v => String(v || '').trim().toLowerCase())
      .filter(Boolean)
  }

  return String(rawRole || '')
    .split(/[,\|;]+/)
    .map(v => v.trim().toLowerCase())
    .filter(Boolean)
}

function getTingkatOptions() {
  return [
    { value: '7', label: 'SMP - Kelas 1' },
    { value: '8', label: 'SMP - Kelas 2' },
    { value: '9', label: 'SMP - Kelas 3' },
    { value: '10', label: 'SMA - Kelas 1' },
    { value: '11', label: 'SMA - Kelas 2' },
    { value: '12', label: 'SMA - Kelas 3' }
  ]
}

function getTingkatLabel(value) {
  const key = String(value ?? '')
  const item = getTingkatOptions().find(opt => opt.value === key)
  return item ? item.label : (value ?? '-')
}

function getJenjangFromTingkat(rawTingkat) {
  const tingkat = Number(rawTingkat)
  if (!Number.isFinite(tingkat)) return ''
  if (tingkat >= 7 && tingkat <= 9) return 'smp'
  if (tingkat >= 10 && tingkat <= 12) return 'sma'
  return ''
}

function normalizeMapelTingkatan(rawValue) {
  const value = String(rawValue || '').trim().toLowerCase()
  if (!value) return ''
  if (['smp', 'mts', 'junior', 'jhs'].includes(value)) return 'smp'
  if (['sma', 'ma', 'senior', 'shs'].includes(value)) return 'sma'
  return ''
}

function getMapelTingkatanLabel(rawValue) {
  const value = normalizeMapelTingkatan(rawValue)
  if (value === 'smp') return 'SMP'
  if (value === 'sma') return 'SMA'
  return '-'
}

function renderMapelTingkatanOptions(selectEl, selectedValue = '', options = {}) {
  if (!selectEl) return
  const {
    placeholder = '-- Pilih Tingkatan --',
    includeAll = false
  } = options

  selectEl.innerHTML = ''
  if (includeAll) {
    const allOpt = document.createElement('option')
    allOpt.value = 'all'
    allOpt.textContent = 'Semua Tingkatan'
    selectEl.appendChild(allOpt)
  } else {
    const emptyOpt = document.createElement('option')
    emptyOpt.value = ''
    emptyOpt.textContent = placeholder
    selectEl.appendChild(emptyOpt)
  }

  ;[
    { value: 'smp', label: 'SMP' },
    { value: 'sma', label: 'SMA' }
  ].forEach(item => {
    const opt = document.createElement('option')
    opt.value = item.value
    opt.textContent = item.label
    selectEl.appendChild(opt)
  })

  const normalized = normalizeMapelTingkatan(selectedValue)
  if (normalized) {
    selectEl.value = normalized
  } else if (includeAll && String(selectedValue || '').toLowerCase() === 'all') {
    selectEl.value = 'all'
  }
}

function renderTingkatOptions(selectEl, selectedValue = '') {
  if (!selectEl) return
  selectEl.innerHTML = '<option value="">-- Pilih Tingkat --</option>'

  getTingkatOptions().forEach(opt => {
    const option = document.createElement('option')
    option.value = opt.value
    option.textContent = opt.label
    if (String(selectedValue) === opt.value) {
      option.selected = true
    }
    selectEl.appendChild(option)
  })
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
  return label || `Semester #${semester?.id ?? '-'}`
}

function getInsetFieldStyle(extra = '') {
  return `width:100%; padding:10px 12px; box-sizing:border-box; border:1px solid #cbd5e1; border-radius:999px; background:#f8fafc; box-shadow:none; outline:none; transition:border-color 0.2s, box-shadow 0.2s; ${extra}`
}

function ensureKelasFieldStyle() {
  if (document.getElementById('kelas-field-style')) return

  const style = document.createElement('style')
  style.id = 'kelas-field-style'
  style.innerHTML = `
    .kelas-field {
      border: 1px solid #cbd5e1;
      border-radius: 999px;
      padding: 9px 12px;
      box-sizing: border-box;
      background: #f8fafc;
      outline: none;
    }
    .kelas-field:focus {
      border-color: #16a34a !important;
      box-shadow: 0 0 0 3px rgba(22, 163, 74, 0.25) !important;
    }
  `
  document.head.appendChild(style)
}

function ensureKelasActionStyle() {
  if (document.getElementById('kelas-btn-style')) return

  const style = document.createElement('style')
  style.id = 'kelas-btn-style'
  style.innerHTML = `
    .btn-edit {
      background: #2563eb;
      color: #fff;
      border: none;
      padding: 7px 16px;
      border-radius: 5px;
      margin-right: 6px;
      cursor: pointer;
      box-shadow: 0 0 8px 0 #2563eb55;
      transition: box-shadow 0.2s, transform 0.1s;
      font-weight: bold;
      outline: none;
    }
    .btn-edit:active {
      box-shadow: 0 0 18px 2px #2563ebcc, 0 0 0 2px #fff;
      transform: scale(0.96);
    }
    .btn-hapus {
      background: #dc2626;
      color: #fff;
      border: none;
      padding: 7px 16px;
      border-radius: 5px;
      cursor: pointer;
      box-shadow: 0 0 8px 0 #dc262655;
      transition: box-shadow 0.2s, transform 0.1s;
      font-weight: bold;
      outline: none;
    }
    .btn-hapus:active {
      box-shadow: 0 0 18px 2px #dc2626cc, 0 0 0 2px #fff;
      transform: scale(0.96);
    }
  `
  document.head.appendChild(style)
}

function ensureDistribusiFilterStyle() {
  if (document.getElementById('distribusi-filter-style')) return

  const style = document.createElement('style')
  style.id = 'distribusi-filter-style'
  style.innerHTML = `
    .distribusi-filter-wrap {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;
      margin-bottom: 10px;
    }
    .distribusi-filter-btn {
      border: 1px solid #cbd5e1;
      background: #fff;
      color: #334155;
      border-radius: 999px;
      padding: 6px 12px;
      cursor: pointer;
      font-size: 12px;
      font-weight: 600;
    }
    .distribusi-filter-btn.active {
      background: #d4d456ff;
      border-color: #d4d456ff;
      color: #0f172a;
    }
    .distribusi-reset-btn {
      border: none;
      border-radius: 999px;
      padding: 8px 14px;
      background: #e2e8f0;
      color: #0f172a;
      font-size: 12px;
      font-weight: 700;
      cursor: pointer;
      transition: background-color .2s ease, transform .08s ease;
    }
    .distribusi-reset-btn:hover {
      background: #cbd5e1;
    }
    .distribusi-reset-btn:active {
      transform: scale(0.98);
    }
  `
  document.head.appendChild(style)
}

function setDistribusiTingkatanView(view) {
  const normalized = ['all', 'smp', 'sma'].includes(String(view || '').toLowerCase())
    ? String(view).toLowerCase()
    : 'all'
  currentDistribusiTingkatanView = normalized

  const buttons = document.querySelectorAll('.distribusi-filter-btn')
  buttons.forEach(btn => {
    btn.classList.toggle('active', btn.getAttribute('data-distribusi-tingkatan') === normalized)
  })

  loadDistribusiMapel()
}

function getDistribusiSortValue(item, maps = {}) {
  const { kelasMap, mapelMap, guruMap, semesterMap } = maps
  if (distribusiSortField === 'tingkatan') {
    return getMapelTingkatanLabel(getJenjangFromTingkat(kelasMap.get(String(item.kelas_id))?.tingkat))
  }
  if (distribusiSortField === 'mapel') return String(mapelMap.get(String(item.mapel_id)) || '')
  if (distribusiSortField === 'guru') return String(guruMap.get(String(item.guru_id)) || '')
  if (distribusiSortField === 'semester') return String(semesterMap.get(String(item.semester_id)) || '')
  return String(kelasMap.get(String(item.kelas_id))?.nama_kelas || '')
}

function applyDistribusiFilterAndSort(distribusiList, maps = {}) {
  const { kelasMap, mapelMap, guruMap, semesterMap } = maps
  const keyword = String(distribusiSearchKeyword || '').trim().toLowerCase()

  const filtered = (distribusiList || []).filter(item => {
    const kelasObj = kelasMap.get(String(item.kelas_id))
    const tingkatan = getJenjangFromTingkat(kelasObj?.tingkat)
    const kelasId = String(item.kelas_id || '')
    const semesterId = String(item.semester_id || '')
    const guruId = String(item.guru_id || '')

    const matchTingkatan = !distribusiSelectFilters.tingkatan || tingkatan === distribusiSelectFilters.tingkatan
    const matchKelas = !distribusiSelectFilters.kelas || kelasId === distribusiSelectFilters.kelas
    const matchSemester = !distribusiSelectFilters.semester || semesterId === distribusiSelectFilters.semester
    const matchGuru = !distribusiSelectFilters.guru || guruId === distribusiSelectFilters.guru

    if (!matchTingkatan || !matchKelas || !matchSemester || !matchGuru) return false

    if (!keyword) return true
    const kelasNama = String(kelasObj?.nama_kelas || '').toLowerCase()
    const mapelNama = String(mapelMap.get(String(item.mapel_id)) || '').toLowerCase()
    const guruNama = String(guruMap.get(String(item.guru_id)) || '').toLowerCase()
    const semesterNama = String(semesterMap.get(String(item.semester_id)) || '').toLowerCase()
    return (
      kelasNama.includes(keyword) ||
      mapelNama.includes(keyword) ||
      guruNama.includes(keyword) ||
      semesterNama.includes(keyword)
    )
  })

  filtered.sort((a, b) => {
    const aValue = getDistribusiSortValue(a, maps)
    const bValue = getDistribusiSortValue(b, maps)
    const cmp = aValue.localeCompare(bValue, undefined, { sensitivity: 'base' })
    return distribusiSortDirection === 'desc' ? -cmp : cmp
  })

  return filtered
}

function applyDistribusiSortControl() {
  const sortFieldEl = document.getElementById('distribusi-sort-field')
  const sortDirEl = document.getElementById('distribusi-sort-direction')
  const tingkatanEl = document.getElementById('distribusi-filter-tingkatan')
  const kelasEl = document.getElementById('distribusi-filter-kelas')
  const semesterEl = document.getElementById('distribusi-filter-semester')
  const guruEl = document.getElementById('distribusi-filter-guru')
  const searchEl = document.getElementById('distribusi-search-input')

  if (sortFieldEl) distribusiSortField = sortFieldEl.value || 'kelas'
  if (sortDirEl) distribusiSortDirection = sortDirEl.value || 'asc'
  if (tingkatanEl) distribusiSelectFilters.tingkatan = tingkatanEl.value || ''
  if (kelasEl) distribusiSelectFilters.kelas = kelasEl.value || ''
  if (semesterEl) distribusiSelectFilters.semester = semesterEl.value || ''
  if (guruEl) distribusiSelectFilters.guru = guruEl.value || ''
  if (searchEl) distribusiSearchKeyword = searchEl.value || ''

  const container = document.getElementById('list-distribusi-mapel')
  if (!container || !currentDistribusiPayload) return
  renderDistribusiMapelTable(container, currentDistribusiPayload)
}

function resetDistribusiSortOrder() {
  distribusiSortField = 'kelas'
  distribusiSortDirection = 'asc'
  distribusiSearchKeyword = ''
  distribusiSelectFilters.tingkatan = ''
  distribusiSelectFilters.kelas = ''
  distribusiSelectFilters.semester = ''
  distribusiSelectFilters.guru = ''

  const container = document.getElementById('list-distribusi-mapel')
  if (!container || !currentDistribusiPayload) return
  renderDistribusiMapelTable(container, currentDistribusiPayload)
}

function toggleDistribusiSortBox() {
  const sortBox = document.getElementById('distribusi-sort-box')
  if (!sortBox) return
  const willShow = sortBox.style.display === 'none' || !sortBox.style.display
  sortBox.style.display = willShow ? 'block' : 'none'
}

function setupDistribusiSortHandlers() {
  const tools = document.getElementById('distribusi-search-tools')
  const sortBox = document.getElementById('distribusi-sort-box')
  if (!tools || !sortBox || tools.dataset.bound === 'true') return

  document.addEventListener('click', (event) => {
    if (!sortBox || !tools) return
    const target = event.target
    if (!target || !(target instanceof Node)) return
    if (!tools.contains(target)) {
      sortBox.style.display = 'none'
    }
  })

  tools.dataset.bound = 'true'
}

async function getTahunAktifKelas() {
  const { data, error } = await sb
    .from('tahun_ajaran')
    .select('id, nama')
    .eq('aktif', true)
    .single()

  if (error) {
    console.error(error)
    return null
  }

  return data
}

async function loadGuruOptions(selectEl, selectedId = '') {
  if (!selectEl) return

  selectEl.innerHTML = '<option value="">-- Pilih Wali Kelas --</option>'

  const { data: karyawanList, error } = await sb
    .from('karyawan')
    .select('id, nama, role')
    .order('nama')

  if (error) {
    console.error(error)
    return
  }

  const guruList = (karyawanList || []).filter(karyawan => {
    const roles = parseRoleList(karyawan.role)
    return roles.includes('guru')
  })

  ;(guruList || []).forEach(guru => {
    const opt = document.createElement('option')
    opt.value = guru.id
    opt.textContent = guru.nama
    if (selectedId && String(selectedId) === String(guru.id)) {
      opt.selected = true
    }
    selectEl.appendChild(opt)
  })
}

async function getKelasListForDistribusi() {
  const tahunAktif = await getTahunAktifKelas()
  if (!tahunAktif) return []

  const { data, error } = await sb
    .from('kelas')
    .select('id, nama_kelas, tingkat')
    .eq('tahun_ajaran_id', tahunAktif.id)
    .order('tingkat', { ascending: true })
    .order('nama_kelas')

  if (error) {
    console.error(error)
    return []
  }
  return data || []
}

async function getMapelList() {
  if (typeof getCachedData === 'function') {
    const cached = getCachedData(MAPEL_CACHE_KEY, MAPEL_CACHE_TTL_MS)
    if (Array.isArray(cached)) return cached
  }

  const { data, error } = await sb
    .from('mapel')
    .select('*')
    .order('nama')

  if (error) {
    console.error(error)
    return []
  }
  const rows = data || []
  if (typeof setCachedData === 'function') setCachedData(MAPEL_CACHE_KEY, rows)
  return rows
}

async function getGuruList() {
  const { data, error } = await sb
    .from('karyawan')
    .select('id, nama, role')
    .order('nama')

  if (error) {
    console.error(error)
    return []
  }

  return (data || []).filter(karyawan => parseRoleList(karyawan.role).includes('guru'))
}

async function getSemesterList() {
  const { data, error } = await sb
    .from('semester')
    .select('*')
    .order('id')

  if (error) {
    console.error(error)
    return []
  }
  return data || []
}

function renderSimpleOptions(selectEl, rows, placeholder, labelGetter, selectedId = '') {
  if (!selectEl) return
  selectEl.innerHTML = `<option value="">${placeholder}</option>`
  ;(rows || []).forEach(row => {
    const opt = document.createElement('option')
    opt.value = row.id
    opt.textContent = labelGetter(row)
    if (selectedId && String(selectedId) === String(row.id)) {
      opt.selected = true
    }
    selectEl.appendChild(opt)
  })
}

async function loadDistribusiFormOptions(options = {}) {
  const {
    tingkatanSelect,
    kelasSelect,
    mapelSelect,
    guruSelect,
    semesterSelect,
    selectedTingkatan = '',
    selectedKelasId = '',
    selectedMapelId = '',
    selectedGuruId = '',
    selectedSemesterId = ''
  } = options

  const [kelasList, mapelList, guruList, semesterList] = await Promise.all([
    getKelasListForDistribusi(),
    getMapelList(),
    getGuruList(),
    getSemesterList()
  ])

  let effectiveTingkatan = normalizeMapelTingkatan(selectedTingkatan)
  if (!effectiveTingkatan) {
    const selectedKelas = (kelasList || []).find(item => String(item.id) === String(selectedKelasId))
    effectiveTingkatan = getJenjangFromTingkat(selectedKelas?.tingkat)
  }
  if (!effectiveTingkatan) {
    const selectedMapel = (mapelList || []).find(item => String(item.id) === String(selectedMapelId))
    effectiveTingkatan = normalizeMapelTingkatan(selectedMapel?.tingkatan || selectedMapel?.jenjang)
  }
  if (tingkatanSelect) {
    renderMapelTingkatanOptions(tingkatanSelect, effectiveTingkatan, { placeholder: '-- Pilih Tingkatan --' })
  }

  const filteredKelasList = effectiveTingkatan
    ? (kelasList || []).filter(item => getJenjangFromTingkat(item.tingkat) === effectiveTingkatan)
    : (kelasList || [])
  const filteredMapelList = effectiveTingkatan
    ? (mapelList || []).filter(item => normalizeMapelTingkatan(item.tingkatan || item.jenjang) === effectiveTingkatan)
    : (mapelList || [])

  renderSimpleOptions(
    kelasSelect,
    filteredKelasList,
    '-- Pilih Kelas --',
    item => item.nama_kelas ?? '-',
    selectedKelasId
  )
  renderSimpleOptions(
    mapelSelect,
    filteredMapelList,
    '-- Pilih Mapel --',
    item => {
      const kategori = item.kategori ? ` (${item.kategori})` : ''
      return `${item.nama ?? '-'}${kategori}`
    },
    selectedMapelId
  )
  renderSimpleOptions(
    guruSelect,
    guruList,
    '-- Pilih Guru --',
    item => item.nama ?? '-',
    selectedGuruId
  )
  renderSimpleOptions(
    semesterSelect,
    semesterList,
    '-- Pilih Semester --',
    item => getSemesterLabel(item),
    selectedSemesterId
  )
}

function createAddKelasModal() {
  let modal = document.getElementById('add-kelas-modal')
  if (modal) modal.remove()

  modal = document.createElement('div')
  modal.id = 'add-kelas-modal'
  modal.style.position = 'fixed'
  modal.style.top = '0'
  modal.style.left = '0'
  modal.style.width = '100vw'
  modal.style.height = '100vh'
  modal.style.background = 'rgba(0,0,0,0.3)'
  modal.style.display = 'none'
  modal.style.zIndex = '9999'
  modal.innerHTML = `
    <div style="background:#fff; margin:80px auto; padding:24px; border-radius:8px; width:320px; box-shadow:0 2px 12px #0002; position:relative;">
      <h3>Tambah Kelas</h3>
      <input class="kelas-field" type="text" id="modal-add-kelas-nama" placeholder="Nama Kelas (contoh: VII A)" style="${getInsetFieldStyle('margin-bottom:8px;')}">
      <select class="kelas-field" id="modal-add-kelas-tingkat" style="${getInsetFieldStyle('margin-bottom:8px;')}">
        <option value="">-- Pilih Tingkat --</option>
      </select>
      <select class="kelas-field" id="modal-add-kelas-wali" style="${getInsetFieldStyle('margin-bottom:12px;')}">
        <option value="">-- Pilih Wali Kelas --</option>
      </select>
      <button class="modal-btn modal-btn-primary" onclick="tambahKelas()">Simpan</button>
      <button class="modal-btn modal-btn-secondary" onclick="closeAddKelasModal()" style="margin-left:8px;">Batal</button>
      <span onclick="closeAddKelasModal()" style="position:absolute;top:8px;right:12px;cursor:pointer;font-size:18px;">&times;</span>
    </div>
  `
  document.body.appendChild(modal)
  return modal
}

async function openAddKelasModal() {
  createAddKelasModal()
  const waliSelect = document.getElementById('modal-add-kelas-wali')
  const tingkatSelect = document.getElementById('modal-add-kelas-tingkat')
  renderTingkatOptions(tingkatSelect)
  await loadGuruOptions(waliSelect)
  document.getElementById('add-kelas-modal').style.display = 'block'
}

function closeAddKelasModal() {
  const modal = document.getElementById('add-kelas-modal')
  if (!modal) return

  modal.style.display = 'none'
  document.getElementById('modal-add-kelas-nama').value = ''
  document.getElementById('modal-add-kelas-tingkat').value = ''
  document.getElementById('modal-add-kelas-wali').value = ''
}

function createEditModal() {
  let modal = document.getElementById('edit-modal')
  if (modal) modal.remove()

  modal = document.createElement('div')
  modal.id = 'edit-modal'
  modal.style.position = 'fixed'
  modal.style.top = '0'
  modal.style.left = '0'
  modal.style.width = '100vw'
  modal.style.height = '100vh'
  modal.style.background = 'rgba(0,0,0,0.3)'
  modal.style.display = 'none'
  modal.style.zIndex = '9999'
  modal.innerHTML = `
    <div id="edit-modal-content" style="background:#fff; margin:80px auto; padding:24px; border-radius:8px; width:320px; box-shadow:0 2px 12px #0002; position:relative;">
      <h3>Edit Kelas</h3>
      <input class="kelas-field" type="text" id="modal-edit-nama" placeholder="Nama Kelas" style="${getInsetFieldStyle('margin-bottom:8px;')}">
      <select class="kelas-field" id="modal-edit-tingkat" style="${getInsetFieldStyle('margin-bottom:8px;')}">
        <option value="">-- Pilih Tingkat --</option>
      </select>
      <select class="kelas-field" id="modal-edit-wali" style="${getInsetFieldStyle('margin-bottom:12px;')}"></select>
      <button class="modal-btn modal-btn-primary" onclick="modalEditKelas()">Simpan</button>
      <button class="modal-btn modal-btn-secondary" onclick="closeEditModal()" style="margin-left:8px;">Batal</button>
      <span onclick="closeEditModal()" style="position:absolute;top:8px;right:12px;cursor:pointer;font-size:18px;">&times;</span>
    </div>
  `
  document.body.appendChild(modal)
}

async function showEditForm(id) {
  createEditModal()
  currentEditId = id

  const kelas = window.kelasList?.find(k => String(k.id) === String(id))
  if (!kelas) {
    alert('Data kelas tidak ditemukan')
    return
  }

  document.getElementById('edit-modal').style.display = 'block'
  document.getElementById('modal-edit-nama').value = kelas.nama_kelas || ''
  const tingkatSelect = document.getElementById('modal-edit-tingkat')
  renderTingkatOptions(tingkatSelect, kelas.tingkat)

  const waliSelect = document.getElementById('modal-edit-wali')
  await loadGuruOptions(waliSelect, kelas?.wali?.id || '')
}

function closeEditModal() {
  const modal = document.getElementById('edit-modal')
  if (!modal) return
  modal.style.display = 'none'
  currentEditId = null
}

async function modalEditKelas() {
  if (!currentEditId) return

  const nama = document.getElementById('modal-edit-nama').value.trim()
  const tingkat = parseInt(document.getElementById('modal-edit-tingkat').value, 10)
  const wali_kelas_id = document.getElementById('modal-edit-wali').value

  if (!nama || !tingkat) {
    alert('Nama dan tingkat harus diisi')
    return
  }

  const { error } = await sb
    .from('kelas')
    .update({
      nama_kelas: nama,
      tingkat: tingkat,
      wali_kelas_id: wali_kelas_id || null
    })
    .eq('id', currentEditId)

  if (error) {
    console.error(error)
    alert('Gagal edit kelas')
    return
  }

  if (typeof clearCachedData === 'function') clearCachedData(KELAS_CACHE_KEY)
  if (typeof clearCachedData === 'function') clearCachedData(DISTRIBUSI_MAPEL_CACHE_KEY)
  closeEditModal()
  loadKelas(true)
}

async function tambahKelas() {
  const nama = document.getElementById('modal-add-kelas-nama').value.trim()
  const tingkat = parseInt(document.getElementById('modal-add-kelas-tingkat').value, 10)
  const wali_kelas_id = document.getElementById('modal-add-kelas-wali').value

  if (!nama || !tingkat) {
    alert('Nama dan tingkat harus diisi')
    return
  }

  const tahunAktif = await getTahunAktifKelas()
  if (!tahunAktif) {
    alert('Tidak ada Tahun Ajaran aktif')
    return
  }

  const { error } = await sb
    .from('kelas')
    .insert([{
      nama_kelas: nama,
      tingkat: tingkat,
      tahun_ajaran_id: tahunAktif.id,
      wali_kelas_id: wali_kelas_id || null
    }])

  if (error) {
    console.error(error)
    alert('Gagal tambah kelas')
    return
  }

  if (typeof clearCachedData === 'function') clearCachedData(KELAS_CACHE_KEY)
  if (typeof clearCachedData === 'function') clearCachedData(DISTRIBUSI_MAPEL_CACHE_KEY)
  closeAddKelasModal()
  loadKelas(true)
}

async function hapusKelas(id) {
  const confirmed = typeof showPopupConfirm === 'function'
    ? await showPopupConfirm('Yakin ingin hapus kelas ini?')
    : confirm('Yakin ingin hapus kelas ini?')
  if (!confirmed) return

  const { error } = await sb
    .from('kelas')
    .delete()
    .eq('id', id)

  if (error) {
    console.error(error)
    alert('Gagal hapus kelas')
    return
  }

  if (typeof clearCachedData === 'function') clearCachedData(KELAS_CACHE_KEY)
  if (typeof clearCachedData === 'function') clearCachedData(DISTRIBUSI_MAPEL_CACHE_KEY)
  loadKelas(true)
}

function createAddDistribusiMapelModal() {
  let modal = document.getElementById('add-distribusi-mapel-modal')
  if (modal) modal.remove()

  modal = document.createElement('div')
  modal.id = 'add-distribusi-mapel-modal'
  modal.style.position = 'fixed'
  modal.style.top = '0'
  modal.style.left = '0'
  modal.style.width = '100vw'
  modal.style.height = '100vh'
  modal.style.background = 'rgba(0,0,0,0.3)'
  modal.style.display = 'none'
  modal.style.zIndex = '9999'
  modal.innerHTML = `
    <div style="background:#fff; margin:80px auto; padding:24px; border-radius:8px; width:360px; box-shadow:0 2px 12px #0002; position:relative;">
      <h3>Tambah Distribusi Mapel</h3>
      <select class="kelas-field" id="modal-add-distribusi-tingkatan" onchange="onAddDistribusiTingkatanChange()" style="${getInsetFieldStyle('margin-bottom:8px;')}">
        <option value="">-- Pilih Tingkatan --</option>
      </select>
      <select class="kelas-field" id="modal-add-distribusi-kelas" style="${getInsetFieldStyle('margin-bottom:8px;')}">
        <option value="">Loading...</option>
      </select>
      <select class="kelas-field" id="modal-add-distribusi-mapel" style="${getInsetFieldStyle('margin-bottom:8px;')}">
        <option value="">Loading...</option>
      </select>
      <select class="kelas-field" id="modal-add-distribusi-guru" style="${getInsetFieldStyle('margin-bottom:8px;')}">
        <option value="">Loading...</option>
      </select>
      <select class="kelas-field" id="modal-add-distribusi-semester" style="${getInsetFieldStyle('margin-bottom:12px;')}">
        <option value="">Loading...</option>
      </select>
      <button class="modal-btn modal-btn-primary" onclick="tambahDistribusiMapel()">Simpan</button>
      <button class="modal-btn modal-btn-secondary" onclick="closeAddDistribusiMapelModal()" style="margin-left:8px;">Batal</button>
      <span onclick="closeAddDistribusiMapelModal()" style="position:absolute;top:8px;right:12px;cursor:pointer;font-size:18px;">&times;</span>
    </div>
  `
  document.body.appendChild(modal)
  return modal
}

async function openAddDistribusiMapelModal() {
  createAddDistribusiMapelModal()
  await loadDistribusiFormOptions({
    tingkatanSelect: document.getElementById('modal-add-distribusi-tingkatan'),
    kelasSelect: document.getElementById('modal-add-distribusi-kelas'),
    mapelSelect: document.getElementById('modal-add-distribusi-mapel'),
    guruSelect: document.getElementById('modal-add-distribusi-guru'),
    semesterSelect: document.getElementById('modal-add-distribusi-semester')
  })
  document.getElementById('add-distribusi-mapel-modal').style.display = 'block'
}

function closeAddDistribusiMapelModal() {
  const modal = document.getElementById('add-distribusi-mapel-modal')
  if (!modal) return
  modal.style.display = 'none'
}

function createEditDistribusiMapelModal() {
  let modal = document.getElementById('edit-distribusi-mapel-modal')
  if (modal) modal.remove()

  modal = document.createElement('div')
  modal.id = 'edit-distribusi-mapel-modal'
  modal.style.position = 'fixed'
  modal.style.top = '0'
  modal.style.left = '0'
  modal.style.width = '100vw'
  modal.style.height = '100vh'
  modal.style.background = 'rgba(0,0,0,0.3)'
  modal.style.display = 'none'
  modal.style.zIndex = '9999'
  modal.innerHTML = `
    <div style="background:#fff; margin:80px auto; padding:24px; border-radius:8px; width:360px; box-shadow:0 2px 12px #0002; position:relative;">
      <h3>Edit Distribusi Mapel</h3>
      <select class="kelas-field" id="modal-edit-distribusi-tingkatan" onchange="onEditDistribusiTingkatanChange()" style="${getInsetFieldStyle('margin-bottom:8px;')}">
        <option value="">-- Pilih Tingkatan --</option>
      </select>
      <select class="kelas-field" id="modal-edit-distribusi-kelas" style="${getInsetFieldStyle('margin-bottom:8px;')}">
        <option value="">Loading...</option>
      </select>
      <select class="kelas-field" id="modal-edit-distribusi-mapel" style="${getInsetFieldStyle('margin-bottom:8px;')}">
        <option value="">Loading...</option>
      </select>
      <select class="kelas-field" id="modal-edit-distribusi-guru" style="${getInsetFieldStyle('margin-bottom:8px;')}">
        <option value="">Loading...</option>
      </select>
      <select class="kelas-field" id="modal-edit-distribusi-semester" style="${getInsetFieldStyle('margin-bottom:12px;')}">
        <option value="">Loading...</option>
      </select>
      <button class="modal-btn modal-btn-primary" onclick="modalEditDistribusiMapel()">Simpan</button>
      <button class="modal-btn modal-btn-secondary" onclick="closeEditDistribusiMapelModal()" style="margin-left:8px;">Batal</button>
      <span onclick="closeEditDistribusiMapelModal()" style="position:absolute;top:8px;right:12px;cursor:pointer;font-size:18px;">&times;</span>
    </div>
  `
  document.body.appendChild(modal)
  return modal
}

function closeEditDistribusiMapelModal() {
  const modal = document.getElementById('edit-distribusi-mapel-modal')
  if (!modal) return
  modal.style.display = 'none'
  currentEditDistribusiMapelId = null
}

async function onAddDistribusiTingkatanChange() {
  await loadDistribusiFormOptions({
    tingkatanSelect: document.getElementById('modal-add-distribusi-tingkatan'),
    kelasSelect: document.getElementById('modal-add-distribusi-kelas'),
    mapelSelect: document.getElementById('modal-add-distribusi-mapel'),
    guruSelect: document.getElementById('modal-add-distribusi-guru'),
    semesterSelect: document.getElementById('modal-add-distribusi-semester'),
    selectedTingkatan: document.getElementById('modal-add-distribusi-tingkatan')?.value || '',
    selectedGuruId: document.getElementById('modal-add-distribusi-guru')?.value || '',
    selectedSemesterId: document.getElementById('modal-add-distribusi-semester')?.value || ''
  })
}

async function onEditDistribusiTingkatanChange() {
  await loadDistribusiFormOptions({
    tingkatanSelect: document.getElementById('modal-edit-distribusi-tingkatan'),
    kelasSelect: document.getElementById('modal-edit-distribusi-kelas'),
    mapelSelect: document.getElementById('modal-edit-distribusi-mapel'),
    guruSelect: document.getElementById('modal-edit-distribusi-guru'),
    semesterSelect: document.getElementById('modal-edit-distribusi-semester'),
    selectedTingkatan: document.getElementById('modal-edit-distribusi-tingkatan')?.value || '',
    selectedGuruId: document.getElementById('modal-edit-distribusi-guru')?.value || '',
    selectedSemesterId: document.getElementById('modal-edit-distribusi-semester')?.value || ''
  })
}

async function tambahDistribusiMapel() {
  const tingkatan = normalizeMapelTingkatan(document.getElementById('modal-add-distribusi-tingkatan')?.value || '')
  const kelasId = document.getElementById('modal-add-distribusi-kelas')?.value || ''
  const mapelId = document.getElementById('modal-add-distribusi-mapel')?.value || ''
  const guruId = document.getElementById('modal-add-distribusi-guru')?.value || ''
  const semesterId = document.getElementById('modal-add-distribusi-semester')?.value || ''

  if (!tingkatan) {
    alert('Tingkatan wajib dipilih')
    return
  }
  if (!kelasId || !mapelId || !guruId || !semesterId) {
    alert('Kelas, mapel, guru, dan semester wajib diisi')
    return
  }

  const { error } = await sb
    .from('distribusi_mapel')
    .insert([{
      kelas_id: kelasId,
      mapel_id: mapelId,
      guru_id: guruId,
      semester_id: semesterId
    }])

  if (error) {
    console.error(error)
    alert('Gagal menambah distribusi mapel')
    return
  }

  if (typeof clearCachedData === 'function') clearCachedData(DISTRIBUSI_MAPEL_CACHE_KEY)
  closeAddDistribusiMapelModal()
  loadDistribusiMapel(true)
}

async function showEditDistribusiMapelForm(id) {
  createEditDistribusiMapelModal()
  const row = window.distribusiMapelList?.find(item => String(item.id) === String(id))
  if (!row) {
    alert('Data distribusi mapel tidak ditemukan')
    return
  }

  currentEditDistribusiMapelId = id
  await loadDistribusiFormOptions({
    tingkatanSelect: document.getElementById('modal-edit-distribusi-tingkatan'),
    kelasSelect: document.getElementById('modal-edit-distribusi-kelas'),
    mapelSelect: document.getElementById('modal-edit-distribusi-mapel'),
    guruSelect: document.getElementById('modal-edit-distribusi-guru'),
    semesterSelect: document.getElementById('modal-edit-distribusi-semester'),
    selectedKelasId: row.kelas_id,
    selectedMapelId: row.mapel_id,
    selectedGuruId: row.guru_id,
    selectedSemesterId: row.semester_id
  })

  document.getElementById('edit-distribusi-mapel-modal').style.display = 'block'
}

async function modalEditDistribusiMapel() {
  if (!currentEditDistribusiMapelId) return

  const tingkatan = normalizeMapelTingkatan(document.getElementById('modal-edit-distribusi-tingkatan')?.value || '')
  const kelasId = document.getElementById('modal-edit-distribusi-kelas')?.value || ''
  const mapelId = document.getElementById('modal-edit-distribusi-mapel')?.value || ''
  const guruId = document.getElementById('modal-edit-distribusi-guru')?.value || ''
  const semesterId = document.getElementById('modal-edit-distribusi-semester')?.value || ''

  if (!tingkatan) {
    alert('Tingkatan wajib dipilih')
    return
  }
  if (!kelasId || !mapelId || !guruId || !semesterId) {
    alert('Kelas, mapel, guru, dan semester wajib diisi')
    return
  }

  const { error } = await sb
    .from('distribusi_mapel')
    .update({
      kelas_id: kelasId,
      mapel_id: mapelId,
      guru_id: guruId,
      semester_id: semesterId
    })
    .eq('id', currentEditDistribusiMapelId)

  if (error) {
    console.error(error)
    alert('Gagal mengubah distribusi mapel')
    return
  }

  if (typeof clearCachedData === 'function') clearCachedData(DISTRIBUSI_MAPEL_CACHE_KEY)
  closeEditDistribusiMapelModal()
  loadDistribusiMapel(true)
}

async function hapusDistribusiMapel(id) {
  const confirmed = typeof showPopupConfirm === 'function'
    ? await showPopupConfirm('Yakin ingin hapus distribusi mapel ini?')
    : confirm('Yakin ingin hapus distribusi mapel ini?')
  if (!confirmed) return

  const { error } = await sb
    .from('distribusi_mapel')
    .delete()
    .eq('id', id)

  if (error) {
    console.error(error)
    alert('Gagal menghapus distribusi mapel')
    return
  }

  if (typeof clearCachedData === 'function') clearCachedData(DISTRIBUSI_MAPEL_CACHE_KEY)
  loadDistribusiMapel(true)
}

function createAddMapelModal() {
  let modal = document.getElementById('add-mapel-modal')
  if (modal) modal.remove()

  modal = document.createElement('div')
  modal.id = 'add-mapel-modal'
  modal.style.position = 'fixed'
  modal.style.top = '0'
  modal.style.left = '0'
  modal.style.width = '100vw'
  modal.style.height = '100vh'
  modal.style.background = 'rgba(0,0,0,0.3)'
  modal.style.display = 'none'
  modal.style.zIndex = '9999'
  modal.innerHTML = `
    <div style="background:#fff; margin:80px auto; padding:24px; border-radius:8px; width:320px; box-shadow:0 2px 12px #0002; position:relative;">
      <h3>Tambah Mapel</h3>
      <input class="kelas-field" type="text" id="modal-add-mapel-nama" placeholder="Nama Mapel" style="${getInsetFieldStyle('margin-bottom:8px;')}">
      <select class="kelas-field" id="modal-add-mapel-tingkatan" style="${getInsetFieldStyle('margin-bottom:8px;')}"></select>
      <input class="kelas-field" type="text" id="modal-add-mapel-kategori" placeholder="Kategori (opsional)" style="${getInsetFieldStyle('margin-bottom:12px;')}">
      <button class="modal-btn modal-btn-primary" onclick="tambahMapel()">Simpan</button>
      <button class="modal-btn modal-btn-secondary" onclick="closeAddMapelModal()" style="margin-left:8px;">Batal</button>
      <span onclick="closeAddMapelModal()" style="position:absolute;top:8px;right:12px;cursor:pointer;font-size:18px;">&times;</span>
    </div>
  `
  document.body.appendChild(modal)
  renderMapelTingkatanOptions(document.getElementById('modal-add-mapel-tingkatan'))
  return modal
}

function openAddMapelModal() {
  createAddMapelModal()
  document.getElementById('add-mapel-modal').style.display = 'block'
}

function closeAddMapelModal() {
  const modal = document.getElementById('add-mapel-modal')
  if (!modal) return
  modal.style.display = 'none'
}

function createEditMapelModal() {
  let modal = document.getElementById('edit-mapel-modal')
  if (modal) modal.remove()

  modal = document.createElement('div')
  modal.id = 'edit-mapel-modal'
  modal.style.position = 'fixed'
  modal.style.top = '0'
  modal.style.left = '0'
  modal.style.width = '100vw'
  modal.style.height = '100vh'
  modal.style.background = 'rgba(0,0,0,0.3)'
  modal.style.display = 'none'
  modal.style.zIndex = '9999'
  modal.innerHTML = `
    <div style="background:#fff; margin:80px auto; padding:24px; border-radius:8px; width:320px; box-shadow:0 2px 12px #0002; position:relative;">
      <h3>Edit Mapel</h3>
      <input class="kelas-field" type="text" id="modal-edit-mapel-nama" placeholder="Nama Mapel" style="${getInsetFieldStyle('margin-bottom:8px;')}">
      <select class="kelas-field" id="modal-edit-mapel-tingkatan" style="${getInsetFieldStyle('margin-bottom:8px;')}"></select>
      <input class="kelas-field" type="text" id="modal-edit-mapel-kategori" placeholder="Kategori (opsional)" style="${getInsetFieldStyle('margin-bottom:12px;')}">
      <button class="modal-btn modal-btn-primary" onclick="modalEditMapel()">Simpan</button>
      <button class="modal-btn modal-btn-secondary" onclick="closeEditMapelModal()" style="margin-left:8px;">Batal</button>
      <span onclick="closeEditMapelModal()" style="position:absolute;top:8px;right:12px;cursor:pointer;font-size:18px;">&times;</span>
    </div>
  `
  document.body.appendChild(modal)
  renderMapelTingkatanOptions(document.getElementById('modal-edit-mapel-tingkatan'))
  return modal
}

function closeEditMapelModal() {
  const modal = document.getElementById('edit-mapel-modal')
  if (!modal) return
  modal.style.display = 'none'
  currentEditMapelId = null
}

async function tambahMapel() {
  const nama = (document.getElementById('modal-add-mapel-nama')?.value || '').trim()
  const tingkatan = normalizeMapelTingkatan(document.getElementById('modal-add-mapel-tingkatan')?.value || '')
  const kategori = (document.getElementById('modal-add-mapel-kategori')?.value || '').trim()

  if (!nama || !tingkatan) {
    alert('Nama mapel dan tingkatan wajib diisi')
    return
  }

  let { error } = await sb
    .from('mapel')
    .insert([{ nama, tingkatan, kategori: kategori || null }])

  if (error && String(error.message || '').toLowerCase().includes('tingkatan')) {
    const retry = await sb
      .from('mapel')
      .insert([{ nama, jenjang: tingkatan, kategori: kategori || null }])
    error = retry.error
  }

  if (error) {
    console.error(error)
    alert('Gagal menambah mapel')
    return
  }

  if (typeof clearCachedData === 'function') clearCachedData(MAPEL_CACHE_KEY)
  if (typeof clearCachedData === 'function') clearCachedData(DISTRIBUSI_MAPEL_CACHE_KEY)
  closeAddMapelModal()
  loadMapel(true)
}

async function showEditMapelForm(id) {
  createEditMapelModal()

  const mapel = window.mapelList?.find(item => String(item.id) === String(id))
  if (!mapel) {
    alert('Data mapel tidak ditemukan')
    return
  }

  currentEditMapelId = id
  document.getElementById('modal-edit-mapel-nama').value = mapel.nama ?? ''
  renderMapelTingkatanOptions(
    document.getElementById('modal-edit-mapel-tingkatan'),
    mapel.tingkatan || mapel.jenjang || ''
  )
  document.getElementById('modal-edit-mapel-kategori').value = mapel.kategori ?? ''
  document.getElementById('edit-mapel-modal').style.display = 'block'
}

async function modalEditMapel() {
  if (!currentEditMapelId) return

  const nama = (document.getElementById('modal-edit-mapel-nama')?.value || '').trim()
  const tingkatan = normalizeMapelTingkatan(document.getElementById('modal-edit-mapel-tingkatan')?.value || '')
  const kategori = (document.getElementById('modal-edit-mapel-kategori')?.value || '').trim()

  if (!nama || !tingkatan) {
    alert('Nama mapel dan tingkatan wajib diisi')
    return
  }

  let { error } = await sb
    .from('mapel')
    .update({ nama, tingkatan, kategori: kategori || null })
    .eq('id', currentEditMapelId)

  if (error && String(error.message || '').toLowerCase().includes('tingkatan')) {
    const retry = await sb
      .from('mapel')
      .update({ nama, jenjang: tingkatan, kategori: kategori || null })
      .eq('id', currentEditMapelId)
    error = retry.error
  }

  if (error) {
    console.error(error)
    alert('Gagal mengubah mapel')
    return
  }

  if (typeof clearCachedData === 'function') clearCachedData(MAPEL_CACHE_KEY)
  if (typeof clearCachedData === 'function') clearCachedData(DISTRIBUSI_MAPEL_CACHE_KEY)
  closeEditMapelModal()
  loadMapel(true)
}

async function hapusMapel(id) {
  const confirmed = typeof showPopupConfirm === 'function'
    ? await showPopupConfirm('Yakin ingin hapus mapel ini?')
    : confirm('Yakin ingin hapus mapel ini?')
  if (!confirmed) return

  const { error } = await sb
    .from('mapel')
    .delete()
    .eq('id', id)

  if (error) {
    console.error(error)
    alert('Gagal menghapus mapel')
    return
  }

  if (typeof clearCachedData === 'function') clearCachedData(MAPEL_CACHE_KEY)
  if (typeof clearCachedData === 'function') clearCachedData(DISTRIBUSI_MAPEL_CACHE_KEY)
  loadMapel(true)
}

async function loadMapel(forceRefresh = false) {
  const container = document.getElementById('list-mapel')
  if (!container) return

  if (!forceRefresh && typeof getCachedData === 'function') {
    const cached = getCachedData(MAPEL_CACHE_KEY, MAPEL_CACHE_TTL_MS)
    if (Array.isArray(cached)) {
      renderMapelTable(container, cached)
      return
    }
  }

  container.innerHTML = 'Loading...'
  const mapelList = await getMapelList()
  renderMapelTable(container, mapelList)
}

function renderMapelTable(container, mapelList) {
  window.mapelList = mapelList || []

  if (!mapelList || mapelList.length === 0) {
    container.innerHTML = 'Belum ada mapel'
    return
  }

  let html = `
    <div style="overflow-x:auto;">
      <table style="width:100%; border-collapse:collapse; margin-top:8px; font-size:13px;">
        <thead>
          <tr style="background:#f3f3f3;">
            <th style="padding:8px; border:1px solid #ddd; text-align:center;">Nama Mapel</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:120px;">Tingkatan</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:180px;">Kategori</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:180px;">Aksi</th>
          </tr>
        </thead>
        <tbody>
  `

  html += mapelList.map(item => `
    <tr>
      <td style="padding:8px; border:1px solid #ddd;">${item.nama ?? '-'}</td>
      <td style="padding:8px; border:1px solid #ddd;">${getMapelTingkatanLabel(item.tingkatan || item.jenjang)}</td>
      <td style="padding:8px; border:1px solid #ddd;">${item.kategori ?? '-'}</td>
      <td style="padding:8px; border:1px solid #ddd; text-align:center; white-space:nowrap;">
        <button class="btn-edit" onclick="showEditMapelForm('${item.id}')">Edit</button>
        <button class="btn-hapus" onclick="hapusMapel('${item.id}')">Hapus</button>
      </td>
    </tr>
  `).join('')

  html += '</tbody></table></div>'
  container.innerHTML = html
}

async function loadDistribusiMapel(forceRefresh = false) {
  const container = document.getElementById('list-distribusi-mapel')
  if (!container) return

  if (!forceRefresh && typeof getCachedData === 'function') {
    const cached = getCachedData(DISTRIBUSI_MAPEL_CACHE_KEY, DISTRIBUSI_MAPEL_CACHE_TTL_MS)
    if (cached) {
      const likelyBrokenMapelCache =
        Array.isArray(cached.distribusiList) &&
        cached.distribusiList.length > 0 &&
        Array.isArray(cached.mapelList) &&
        cached.mapelList.length === 0
      if (likelyBrokenMapelCache) {
        clearCachedData?.(DISTRIBUSI_MAPEL_CACHE_KEY)
      } else {
        currentDistribusiPayload = cached
        renderDistribusiMapelTable(container, cached)
        return
      }
    }
  }

  container.innerHTML = 'Loading...'

  const [distribusiRes, kelasList, mapelList, karyawanList, semesterList] = await Promise.all([
    sb
      .from('distribusi_mapel')
      .select('id, kelas_id, mapel_id, guru_id, semester_id')
      .order('id', { ascending: false }),
    sb.from('kelas').select('id, nama_kelas, tingkat'),
    sb.from('mapel').select('id, nama, kategori'),
    sb.from('karyawan').select('id, nama'),
    getSemesterList()
  ])

  if (distribusiRes.error) {
    console.error(distribusiRes.error)
    container.innerHTML = 'Gagal load distribusi mapel'
    return
  }

  if (mapelList.error) {
    console.error(mapelList.error)
  }

  if (kelasList.error) {
    console.error(kelasList.error)
  }

  if (karyawanList.error) {
    console.error(karyawanList.error)
  }

  const cachedPayload = {
    distribusiList: distribusiRes.data || [],
    kelasList: kelasList.data || [],
    mapelList: mapelList.data || [],
    guruList: karyawanList.data || [],
    semesterList: semesterList || []
  }
  if (typeof setCachedData === 'function') {
    setCachedData(DISTRIBUSI_MAPEL_CACHE_KEY, cachedPayload)
  }
  currentDistribusiPayload = cachedPayload
  renderDistribusiMapelTable(container, cachedPayload)
}

function renderDistribusiMapelTable(container, payload) {
  ensureDistribusiFilterStyle()
  const distribusiList = payload?.distribusiList || []
  const kelasMap = new Map((payload?.kelasList || []).map(item => [String(item.id), item]))
  const mapelMap = new Map((payload?.mapelList || []).map(item => {
    const kategori = item.kategori ? ` (${item.kategori})` : ''
    return [String(item.id), `${item.nama ?? '-'}${kategori}`]
  }))
  const guruMap = new Map((payload?.guruList || []).map(item => [String(item.id), item.nama ?? '-']))
  const semesterMap = new Map((payload?.semesterList || []).map(item => [String(item.id), getSemesterLabel(item)]))

  const filteredDistribusiList = applyDistribusiFilterAndSort(distribusiList, {
    kelasMap,
    mapelMap,
    guruMap,
    semesterMap
  })

  window.distribusiMapelList = filteredDistribusiList

  const kelasOptions = Array.from(kelasMap.values())
    .sort((a, b) => String(a?.nama_kelas || '').localeCompare(String(b?.nama_kelas || '')))
    .map(item => `<option value="${item.id}">${item.nama_kelas ?? '-'}</option>`)
    .join('')

  const semesterOptions = (payload?.semesterList || [])
    .slice()
    .sort((a, b) => String(getSemesterLabel(a)).localeCompare(String(getSemesterLabel(b))))
    .map(item => `<option value="${item.id}">${getSemesterLabel(item)}</option>`)
    .join('')

  const guruOptions = (payload?.guruList || [])
    .slice()
    .sort((a, b) => String(a?.nama || '').localeCompare(String(b?.nama || '')))
    .map(item => `<option value="${item.id}">${item.nama ?? '-'}</option>`)
    .join('')

  const controlsHtml = `
    <div class="distribusi-filter-wrap" style="margin-bottom:0;">
      <select class="kelas-field" id="distribusi-sort-field" onchange="applyDistribusiSortControl()" style="${getInsetFieldStyle('width:auto; min-width:130px;')}">
        <option value="kelas">Sort: Kelas</option>
        <option value="tingkatan">Sort: Tingkatan</option>
        <option value="mapel">Sort: Mapel</option>
        <option value="guru">Sort: Guru</option>
        <option value="semester">Sort: Semester</option>
      </select>
      <select class="kelas-field" id="distribusi-sort-direction" onchange="applyDistribusiSortControl()" style="${getInsetFieldStyle('width:auto; min-width:100px;')}">
        <option value="asc">A-Z</option>
        <option value="desc">Z-A</option>
      </select>
      <select class="kelas-field" id="distribusi-filter-tingkatan" onchange="applyDistribusiSortControl()" style="${getInsetFieldStyle('width:auto; min-width:120px;')}">
        <option value="">Semua Tingkatan</option>
        <option value="smp">SMP</option>
        <option value="sma">SMA</option>
      </select>
      <select class="kelas-field" id="distribusi-filter-kelas" onchange="applyDistribusiSortControl()" style="${getInsetFieldStyle('width:auto; min-width:160px;')}">
        <option value="">Semua Kelas</option>
        ${kelasOptions}
      </select>
      <select class="kelas-field" id="distribusi-filter-semester" onchange="applyDistribusiSortControl()" style="${getInsetFieldStyle('width:auto; min-width:160px;')}">
        <option value="">Semua Semester</option>
        ${semesterOptions}
      </select>
      <select class="kelas-field" id="distribusi-filter-guru" onchange="applyDistribusiSortControl()" style="${getInsetFieldStyle('width:auto; min-width:160px;')}">
        <option value="">Semua Guru</option>
        ${guruOptions}
      </select>
      <input class="kelas-field" type="text" id="distribusi-search-input" placeholder="Cari mapel/kelas/guru" oninput="applyDistribusiSortControl()" value="${String(distribusiSearchKeyword || '').replaceAll('"', '&quot;')}" style="${getInsetFieldStyle('width:auto; min-width:220px;')}">
      <button type="button" class="distribusi-reset-btn" onclick="resetDistribusiSortOrder()">Reset</button>
    </div>
  `

  const sortContent = document.getElementById('distribusi-sort-content')
  if (sortContent) sortContent.innerHTML = controlsHtml

  if (filteredDistribusiList.length === 0) {
    container.innerHTML = 'Belum ada distribusi mapel'
    const sortFieldEl = document.getElementById('distribusi-sort-field')
    const sortDirEl = document.getElementById('distribusi-sort-direction')
    const tingkatanEl = document.getElementById('distribusi-filter-tingkatan')
    const kelasEl = document.getElementById('distribusi-filter-kelas')
    const semesterEl = document.getElementById('distribusi-filter-semester')
    const guruEl = document.getElementById('distribusi-filter-guru')
    if (sortFieldEl) sortFieldEl.value = distribusiSortField
    if (sortDirEl) sortDirEl.value = distribusiSortDirection
    if (tingkatanEl) tingkatanEl.value = distribusiSelectFilters.tingkatan || ''
    if (kelasEl) kelasEl.value = distribusiSelectFilters.kelas || ''
    if (semesterEl) semesterEl.value = distribusiSelectFilters.semester || ''
    if (guruEl) guruEl.value = distribusiSelectFilters.guru || ''
    return
  }

  let html = `
    <div style="overflow-x:auto;">
      <table style="width:100%; border-collapse:collapse; margin-top:8px; font-size:13px;">
        <thead>
          <tr style="background:#f3f3f3;">
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:110px;">Tingkatan</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center;">Kelas</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center;">Mapel</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center;">Guru</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center;">Semester</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:180px;">Aksi</th>
          </tr>
        </thead>
        <tbody>
  `

  html += filteredDistribusiList.map(item => `
    <tr>
      <td style="padding:8px; border:1px solid #ddd;">${getMapelTingkatanLabel(getJenjangFromTingkat(kelasMap.get(String(item.kelas_id))?.tingkat))}</td>
      <td style="padding:8px; border:1px solid #ddd;">${kelasMap.get(String(item.kelas_id))?.nama_kelas || '-'}</td>
      <td style="padding:8px; border:1px solid #ddd;">${mapelMap.get(String(item.mapel_id)) || '-'}</td>
      <td style="padding:8px; border:1px solid #ddd;">${guruMap.get(String(item.guru_id)) || '-'}</td>
      <td style="padding:8px; border:1px solid #ddd;">${semesterMap.get(String(item.semester_id)) || '-'}</td>
      <td style="padding:8px; border:1px solid #ddd; text-align:center; white-space:nowrap;">
        <button class="btn-edit" onclick="showEditDistribusiMapelForm('${item.id}')">Edit</button>
        <button class="btn-hapus" onclick="hapusDistribusiMapel('${item.id}')">Hapus</button>
      </td>
    </tr>
  `).join('')

  html += '</tbody></table></div>'
  container.innerHTML = html

  const sortFieldEl = document.getElementById('distribusi-sort-field')
  const sortDirEl = document.getElementById('distribusi-sort-direction')
  const tingkatanEl = document.getElementById('distribusi-filter-tingkatan')
  const kelasEl = document.getElementById('distribusi-filter-kelas')
  const semesterEl = document.getElementById('distribusi-filter-semester')
  const guruEl = document.getElementById('distribusi-filter-guru')
  if (sortFieldEl) sortFieldEl.value = distribusiSortField
  if (sortDirEl) sortDirEl.value = distribusiSortDirection
  if (tingkatanEl) tingkatanEl.value = distribusiSelectFilters.tingkatan || ''
  if (kelasEl) kelasEl.value = distribusiSelectFilters.kelas || ''
  if (semesterEl) semesterEl.value = distribusiSelectFilters.semester || ''
  if (guruEl) guruEl.value = distribusiSelectFilters.guru || ''
}

async function loadKelas(forceRefresh = false) {
  const container = document.getElementById('list-kelas')
  if (!container) return

  if (!forceRefresh && typeof getCachedData === 'function') {
    const cached = getCachedData(KELAS_CACHE_KEY, KELAS_CACHE_TTL_MS)
    if (cached) {
      renderKelasTable(container, cached.tahunAktif, cached.kelasList, cached.santriAktifMap)
      return
    }
  }

  container.innerHTML = 'Loading...'

  const tahunAktif = await getTahunAktifKelas()
  if (!tahunAktif) {
    renderKelasTable(container, null, [], {})
    return
  }

  const { data: kelasList, error } = await sb
    .from('kelas')
    .select(`
      *,
      wali:karyawan!kelas_wali_kelas_id_fkey ( id, nama )
    `)
    .eq('tahun_ajaran_id', tahunAktif.id)
    .order('tingkat', { ascending: true })
    .order('nama_kelas')

  if (error) {
    console.error(error)
    container.innerHTML = '<p>Gagal load kelas.</p>'
    return
  }

  if (!kelasList || kelasList.length === 0) {
    if (typeof setCachedData === 'function') {
      setCachedData(KELAS_CACHE_KEY, { tahunAktif, kelasList: [], santriAktifMap: {} })
    }
    renderKelasTable(container, tahunAktif, [], {})
    return
  }

  const kelasIds = kelasList.map(kelas => kelas.id)
  const santriAktifMap = {}

  if (kelasIds.length > 0) {
    const { data: santriAktifList } = await sb
      .from('santri')
      .select('kelas_id')
      .in('kelas_id', kelasIds)
      .eq('aktif', true)

    ;(santriAktifList || []).forEach(santri => {
      const key = String(santri.kelas_id)
      santriAktifMap[key] = (santriAktifMap[key] || 0) + 1
    })
  }

  if (typeof setCachedData === 'function') {
    setCachedData(KELAS_CACHE_KEY, { tahunAktif, kelasList, santriAktifMap })
  }
  renderKelasTable(container, tahunAktif, kelasList, santriAktifMap)
}

function renderKelasTable(container, tahunAktif, kelasList, santriAktifMap) {
  let badge = ''
  if (tahunAktif && tahunAktif.nama) {
    badge = `<span style="display:inline-block;background:#2563eb;color:#fff;padding:4px 14px;border-radius:16px;font-size:14px;margin-bottom:10px;">Tahun Ajaran Aktif: <b>${tahunAktif.nama}</b></span>`
  }

  if (!tahunAktif) {
    container.innerHTML = badge + '<p>Tidak ada Tahun Ajaran aktif.</p>'
    return
  }

  if (!kelasList || kelasList.length === 0) {
    container.innerHTML = badge + '<p>Belum ada kelas.</p>'
    return
  }

  window.kelasList = kelasList
  ensureKelasActionStyle()

  let html = badge + `
    <div style="overflow-x:auto;">
      <table style="width:100%; border-collapse:collapse; margin-top:8px; font-size:13px;">
        <thead>
          <tr style="background:#f3f3f3;">
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:180px;">Nama Kelas</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:90px;">Tingkat</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center;">Wali Kelas</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:130px;">Santri Aktif</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:180px;">Aksi</th>
          </tr>
        </thead>
        <tbody>
  `

  html += kelasList.map(kelas => `
    <tr>
      <td style="padding:8px; border:1px solid #ddd;">${kelas.nama_kelas ?? '-'}</td>
      <td style="padding:8px; border:1px solid #ddd;">${getTingkatLabel(kelas.tingkat)}</td>
      <td style="padding:8px; border:1px solid #ddd;">${kelas.wali?.nama ?? '-'}</td>
      <td style="padding:8px; border:1px solid #ddd;">${santriAktifMap[String(kelas.id)] || 0} orang</td>
      <td style="padding:8px; border:1px solid #ddd; text-align:center; white-space:nowrap;">
        <button class="btn-edit" onclick="showEditForm('${kelas.id}')">Edit</button>
        <button class="btn-hapus" onclick="hapusKelas('${kelas.id}')">Hapus</button>
      </td>
    </tr>
  `).join('')

  html += '</tbody></table></div>'
  container.innerHTML = html
}

function initKelasPage(params = {}) {
  ensureKelasFieldStyle()
  ensureKelasActionStyle()
  ensureDistribusiFilterStyle()
  setupDistribusiSortHandlers()
  createAddKelasModal()
  createEditModal()
  createAddDistribusiMapelModal()
  createEditDistribusiMapelModal()
  createAddMapelModal()
  createEditMapelModal()
  const requestedSubtab = params?.subtab || kelasActiveSubtab
  setKelasSubtab(requestedSubtab)
}

function setKelasSubtab(tab) {
  const validTab = ['data-kelas', 'guru-mapel', 'distribusi-mapel'].includes(tab)
    ? tab
    : 'data-kelas'
  kelasActiveSubtab = validTab

  const tabButtons = document.querySelectorAll('.kelas-subtab-btn')
  tabButtons.forEach(button => {
    button.classList.toggle('active', button.getAttribute('data-kelas-tab') === validTab)
  })

  const panes = document.querySelectorAll('.kelas-subtab-pane')
  panes.forEach(pane => pane.classList.remove('active'))

  const activePane = document.getElementById(`kelas-pane-${validTab}`)
  if (activePane) activePane.classList.add('active')

  if (typeof syncKelasSidebarSelection === 'function') {
    syncKelasSidebarSelection(validTab)
  }

  if (validTab === 'data-kelas') {
    loadKelas()
    return
  }

  if (validTab === 'distribusi-mapel') {
    loadDistribusiMapel()
    return
  }

  if (validTab === 'guru-mapel') {
    loadMapel()
  }
}
