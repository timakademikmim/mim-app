let currentEditJadwalId = null
let currentEditJamPelajaranId = null
let currentJadwalSubtab = 'jadwal'
let currentActiveSemesterId = ''
let currentActiveTahunAjaranId = ''
let currentJadwalCacheKey = ''
let currentJamCacheKey = ''
const jadwalCacheKeys = new Set()
let jadwalList = []
let jadwalDistribusiList = []
let jamPelajaranList = []

const JADWAL_CACHE_KEY = 'jadwal_pelajaran:list:v3'
const JADWAL_CACHE_TTL_MS = 2 * 60 * 1000
const JAM_PELAJARAN_CACHE_KEY = 'jam_pelajaran:list:v1'
const JAM_PELAJARAN_CACHE_TTL_MS = 2 * 60 * 1000
let jamPelajaranSupportsTahunAjaran = null

function rememberJadwalCacheKey(key) {
  if (!key) return
  jadwalCacheKeys.add(String(key))
}

function clearAllJadwalCaches() {
  clearCachedData?.(JADWAL_CACHE_KEY)
  if (currentJadwalCacheKey) clearCachedData?.(currentJadwalCacheKey)
  jadwalCacheKeys.forEach(key => clearCachedData?.(key))
}

function rememberJamCacheKey(key) {
  if (!key) return
  jadwalCacheKeys.add(String(key))
}

function clearAllJamPelajaranCaches() {
  clearCachedData?.(JAM_PELAJARAN_CACHE_KEY)
  if (currentJamCacheKey) clearCachedData?.(currentJamCacheKey)
  jadwalCacheKeys.forEach(key => {
    if (String(key).startsWith(`${JAM_PELAJARAN_CACHE_KEY}:`)) {
      clearCachedData?.(key)
    }
  })
}

function getJadwalCacheVersion() {
  try {
    return String(localStorage.getItem('jadwal_cache_version') || 'v0')
  } catch (e) {
    return 'v0'
  }
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

function toTimeLabel(value) {
  const text = String(value || '').trim()
  if (!text) return '-'
  return text.length >= 5 ? text.slice(0, 5) : text
}

function getJamKey(jamMulai, jamSelesai) {
  return `${toTimeLabel(jamMulai)}|${toTimeLabel(jamSelesai)}`
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

function getJadwalInsetFieldStyle(extra = '') {
  return `width:100%; padding:10px 12px; box-sizing:border-box; border:1px solid #cbd5e1; border-radius:999px; background:#f8fafc; box-shadow:none; outline:none; transition:border-color 0.2s, box-shadow 0.2s; ${extra}`
}

function ensureJadwalFieldStyle() {
  if (document.getElementById('jadwal-field-style')) return

  const style = document.createElement('style')
  style.id = 'jadwal-field-style'
  style.innerHTML = `
    .jadwal-field {
      border: 1px solid #cbd5e1;
      border-radius: 999px;
      padding: 9px 12px;
      box-sizing: border-box;
      background: #f8fafc;
      outline: none;
      font-size: 13px;
    }
    .jadwal-field:focus {
      border-color: #16a34a !important;
      box-shadow: 0 0 0 3px rgba(22, 163, 74, 0.25) !important;
    }
    #list-jadwal .btn-edit,
    #list-jadwal .btn-hapus,
    #list-jam-pelajaran .btn-edit,
    #list-jam-pelajaran .btn-hapus {
      border: none;
      border-radius: 999px;
      padding: 7px 14px;
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
      transition: background-color .2s ease, transform .08s ease;
      margin: 0 3px;
    }
    #list-jadwal .btn-edit,
    #list-jam-pelajaran .btn-edit {
      background: #e6f4ea;
      color: #166534;
    }
    #list-jadwal .btn-edit:hover,
    #list-jam-pelajaran .btn-edit:hover {
      background: #d1f0da;
    }
    #list-jadwal .btn-hapus,
    #list-jam-pelajaran .btn-hapus {
      background: #fee2e2;
      color: #991b1b;
    }
    #list-jadwal .btn-hapus:hover,
    #list-jam-pelajaran .btn-hapus:hover {
      background: #fecaca;
    }
    #list-jadwal .btn-edit:active,
    #list-jadwal .btn-hapus:active,
    #list-jam-pelajaran .btn-edit:active,
    #list-jam-pelajaran .btn-hapus:active {
      transform: scale(0.98);
    }
  `
  document.head.appendChild(style)
}

async function getActiveSemesterForJadwal() {
  const asBool = (value) => {
    if (value === true || value === 1) return true
    const text = String(value ?? '').trim().toLowerCase()
    return text === 'true' || text === 't' || text === '1' || text === 'yes'
  }

  const { data: tahunAktif } = await sb
    .from('tahun_ajaran')
    .select('id')
    .eq('aktif', true)
    .order('id', { ascending: false })
    .limit(1)

  const tahunAjaranId = tahunAktif?.[0]?.id || null
  currentActiveTahunAjaranId = tahunAjaranId ? String(tahunAjaranId) : ''

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
  return rows.find(item => asBool(item?.aktif)) || rows[0] || null
}

async function checkJamPelajaranSupportsTahunAjaran() {
  if (jamPelajaranSupportsTahunAjaran !== null) return jamPelajaranSupportsTahunAjaran
  const { error } = await sb
    .from('jam_pelajaran')
    .select('id, tahun_ajaran_id')
    .limit(1)
  jamPelajaranSupportsTahunAjaran = !error
  if (error) {
    console.warn('Kolom jam_pelajaran.tahun_ajaran_id belum tersedia.', error)
  }
  return jamPelajaranSupportsTahunAjaran
}

async function getDistribusiOptions(activeSemester = null) {
  const semester = activeSemester || await getActiveSemesterForJadwal()
  currentActiveTahunAjaranId = semester?.tahun_ajaran_id ? String(semester.tahun_ajaran_id) : currentActiveTahunAjaranId
  currentActiveSemesterId = semester?.id ? String(semester.id) : ''
  if (!currentActiveSemesterId) return []

  const [distribusiRes, kelasRes, mapelRes, guruRes, semesterRes] = await Promise.all([
    sb
      .from('distribusi_mapel')
      .select('id, kelas_id, mapel_id, guru_id, semester_id')
      .eq('semester_id', currentActiveSemesterId)
      .order('id', { ascending: false }),
    sb.from('kelas').select('id, nama_kelas, tingkat'),
    sb.from('mapel').select('id, nama, kategori'),
    sb.from('karyawan').select('id, nama'),
    sb.from('semester').select('id, nama, aktif')
  ])

  if (distribusiRes.error) {
    console.error(distribusiRes.error)
    return []
  }
  if (kelasRes.error) console.error(kelasRes.error)
  if (mapelRes.error) console.error(mapelRes.error)
  if (guruRes.error) console.error(guruRes.error)
  if (semesterRes.error) console.error(semesterRes.error)

  const kelasMap = new Map((kelasRes.data || []).map(item => [String(item.id), item]))
  const mapelMap = new Map((mapelRes.data || []).map(item => [String(item.id), item]))
  const guruMap = new Map((guruRes.data || []).map(item => [String(item.id), item]))
  const semesterMap = new Map((semesterRes.data || []).map(item => [String(item.id), item]))

  return (distribusiRes.data || []).map(item => ({
    ...item,
    kelas: kelasMap.get(String(item.kelas_id)) || null,
    mapel: mapelMap.get(String(item.mapel_id)) || null,
    guru: guruMap.get(String(item.guru_id)) || null,
    semester: semesterMap.get(String(item.semester_id)) || null
  }))
}

async function getJadwalRows() {
  const { data, error } = await sb
    .from('jadwal_pelajaran')
    .select('id, distribusi_id, hari, jam_mulai, jam_selesai')
    .order('hari')
    .order('jam_mulai')

  if (error) throw error
  return data || []
}

async function getJamPelajaranRows(tahunAjaranId = '') {
  const supportTahunAjaran = await checkJamPelajaranSupportsTahunAjaran()
  let query = sb
    .from('jam_pelajaran')
    .select('id, nama, jam_mulai, jam_selesai, urutan, aktif')
    .order('urutan', { ascending: true })
    .order('jam_mulai', { ascending: true })

  if (supportTahunAjaran && tahunAjaranId) {
    query = query.eq('tahun_ajaran_id', tahunAjaranId)
  }

  let { data, error } = await query

  if (!error) return data || []

  const message = String(error.message || '').toLowerCase()
  if (!message.includes('urutan') && !message.includes('aktif')) {
    throw error
  }

  const retry = await sb
    .from('jam_pelajaran')
    .select('id, nama, jam_mulai, jam_selesai')
    .order('jam_mulai', { ascending: true })

  if (retry.error) throw retry.error
  return retry.data || []
}
function renderJadwalKelasFilterOptions() {
  const kelasFilter = document.getElementById('jadwal-filter-kelas')
  if (!kelasFilter) return

  const kelasMap = new Map()
  ;(jadwalDistribusiList || []).forEach(item => {
    const kelas = item?.kelas
    if (!kelas?.id) return
    kelasMap.set(String(kelas.id), kelas.nama_kelas || '-')
  })

  kelasFilter.innerHTML = '<option value="">Semua Kelas</option>'
  Array.from(kelasMap.entries())
    .sort((a, b) => a[1].localeCompare(b[1]))
    .forEach(([id, nama]) => {
      const opt = document.createElement('option')
      opt.value = id
      opt.textContent = nama
      kelasFilter.appendChild(opt)
    })
}

function getJadwalFilteredRows() {
  const keyword = String(document.getElementById('jadwal-search-input')?.value || '').trim().toLowerCase()
  const hariFilter = normalizeHari(document.getElementById('jadwal-filter-hari')?.value || '')
  const kelasFilter = String(document.getElementById('jadwal-filter-kelas')?.value || '')
  const distribusiMap = new Map((jadwalDistribusiList || []).map(item => [String(item.id), item]))

  return (jadwalList || []).filter(row => {
    const distribusi = distribusiMap.get(String(row.distribusi_id))
    if (!distribusi) return false

    const matchHari = !hariFilter || normalizeHari(row.hari) === hariFilter
    const matchKelas = !kelasFilter || String(distribusi.kelas_id || '') === kelasFilter
    if (!matchHari || !matchKelas) return false

    if (!keyword) return true
    const kelasNama = String(distribusi?.kelas?.nama_kelas || '').toLowerCase()
    const mapelNama = String(distribusi?.mapel?.nama || '').toLowerCase()
    const guruNama = String(distribusi?.guru?.nama || '').toLowerCase()
    return kelasNama.includes(keyword) || mapelNama.includes(keyword) || guruNama.includes(keyword)
  })
}

function renderJadwalTable() {
  const container = document.getElementById('list-jadwal')
  if (!container) return

  const distribusiMap = new Map((jadwalDistribusiList || []).map(item => [String(item.id), item]))
  const jamMap = new Map((jamPelajaranList || []).map(item => [getJamKey(item.jam_mulai, item.jam_selesai), item]))
  const rows = getJadwalFilteredRows()
  if (!rows.length) {
    container.innerHTML = 'Belum ada jadwal pelajaran.'
    return
  }

  let html = `
    <div style="overflow-x:auto;">
      <table style="width:100%; border-collapse:collapse; margin-top:8px; font-size:13px;">
        <thead>
          <tr style="background:#f3f3f3;">
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:110px;">Hari</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:180px;">Jam Pelajaran</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center;">Kelas</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center;">Mapel</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center;">Guru</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:150px;">Semester</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:170px;">Aksi</th>
          </tr>
        </thead>
        <tbody>
  `

  html += rows.map(row => {
    const distribusi = distribusiMap.get(String(row.distribusi_id))
    const kelasNama = distribusi?.kelas?.nama_kelas || '-'
    const mapelNama = distribusi?.mapel?.nama
      ? `${distribusi.mapel.nama}${distribusi.mapel.kategori ? ` (${distribusi.mapel.kategori})` : ''}`
      : '-'
    const guruNama = distribusi?.guru?.nama || '-'
    const semesterNama = getSemesterLabel(distribusi?.semester)
    const jamMatch = jamMap.get(getJamKey(row.jam_mulai, row.jam_selesai))
    const jamNama = jamMatch?.nama || `${toTimeLabel(row.jam_mulai)} - ${toTimeLabel(row.jam_selesai)}`
    const jamWaktu = `${toTimeLabel(row.jam_mulai)} - ${toTimeLabel(row.jam_selesai)}`

    return `
      <tr>
        <td style="padding:8px; border:1px solid #ddd;">${getHariLabel(row.hari)}</td>
        <td style="padding:8px; border:1px solid #ddd;">
          <div style="font-weight:600;">${jamNama}</div>
          <div style="font-size:12px; color:#64748b;">${jamWaktu}</div>
        </td>
        <td style="padding:8px; border:1px solid #ddd;">${kelasNama}</td>
        <td style="padding:8px; border:1px solid #ddd;">${mapelNama}</td>
        <td style="padding:8px; border:1px solid #ddd;">${guruNama}</td>
        <td style="padding:8px; border:1px solid #ddd;">${semesterNama}</td>
        <td style="padding:8px; border:1px solid #ddd; text-align:center; white-space:nowrap;">
          <button class="btn-edit" onclick="openEditJadwalModal('${row.id}')">Edit</button>
          <button class="btn-hapus" onclick="hapusJadwal('${row.id}')">Hapus</button>
        </td>
      </tr>
    `
  }).join('')

  html += '</tbody></table></div>'
  container.innerHTML = html
}

function renderJamPelajaranTable() {
  const container = document.getElementById('list-jam-pelajaran')
  if (!container) return

  const rows = Array.isArray(jamPelajaranList) ? jamPelajaranList : []
  if (rows.length === 0) {
    container.innerHTML = 'Belum ada jam pelajaran.'
    return
  }

  let html = `
    <div style="overflow-x:auto;">
      <table style="width:100%; border-collapse:collapse; margin-top:8px; font-size:13px;">
        <thead>
          <tr style="background:#f3f3f3;">
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:120px;">Nama Jam</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:150px;">Waktu</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:170px;">Aksi</th>
          </tr>
        </thead>
        <tbody>
  `

  html += rows.map(item => `
    <tr>
      <td style="padding:8px; border:1px solid #ddd;">${item.nama || '-'}</td>
      <td style="padding:8px; border:1px solid #ddd; text-align:center;">${toTimeLabel(item.jam_mulai)} - ${toTimeLabel(item.jam_selesai)}</td>
      <td style="padding:8px; border:1px solid #ddd; text-align:center; white-space:nowrap;">
        <button class="btn-edit" onclick="openEditJamPelajaranModal('${item.id}')">Edit</button>
        <button class="btn-hapus" onclick="hapusJamPelajaran('${item.id}')">Hapus</button>
      </td>
    </tr>
  `).join('')

  html += '</tbody></table></div>'
  container.innerHTML = html
}

function setJadwalSubtab(tab) {
  const validTab = tab === 'jam-pelajaran' ? 'jam-pelajaran' : 'jadwal'
  currentJadwalSubtab = validTab

  const tabButtons = document.querySelectorAll('.jadwal-subtab-btn')
  tabButtons.forEach(button => {
    button.classList.toggle('active', button.getAttribute('data-jadwal-tab') === validTab)
  })

  const panes = document.querySelectorAll('.jadwal-subtab-pane')
  panes.forEach(pane => pane.classList.remove('active'))
  const activePane = document.getElementById(`jadwal-pane-${validTab}`)
  if (activePane) activePane.classList.add('active')

  if (validTab === 'jadwal') {
    loadJadwalPelajaran()
  } else {
    loadJamPelajaran()
  }
}

function resetJadwalFilter() {
  const searchEl = document.getElementById('jadwal-search-input')
  const hariEl = document.getElementById('jadwal-filter-hari')
  const kelasEl = document.getElementById('jadwal-filter-kelas')
  if (searchEl) searchEl.value = ''
  if (hariEl) hariEl.value = ''
  if (kelasEl) kelasEl.value = ''
  renderJadwalTable()
}

function createJadwalModal(modalId, title, saveHandler) {
  let modal = document.getElementById(modalId)
  if (modal) modal.remove()

  const prefix = modalId === 'add-jadwal-modal' ? 'modal-add' : 'modal-edit'
  modal = document.createElement('div')
  modal.id = modalId
  modal.style.position = 'fixed'
  modal.style.top = '0'
  modal.style.left = '0'
  modal.style.width = '100vw'
  modal.style.height = '100vh'
  modal.style.background = 'rgba(0,0,0,0.3)'
  modal.style.display = 'none'
  modal.style.zIndex = '9999'
  modal.innerHTML = `
    <div style="background:#fff; margin:40px auto; padding:24px; border-radius:8px; width:420px; max-width:calc(100vw - 24px); box-shadow:0 2px 12px #0002; position:relative;">
      <h3 style="margin-top:0;">${title}</h3>
      <select class="jadwal-field" id="${prefix}-jadwal-kelas" style="${getJadwalInsetFieldStyle('margin-bottom:8px;')}"></select>
      <select class="jadwal-field" id="${prefix}-jadwal-mapel" style="${getJadwalInsetFieldStyle('margin-bottom:8px;')}"></select>
      <select class="jadwal-field" id="${prefix}-jadwal-jam-slot" style="${getJadwalInsetFieldStyle('margin-bottom:8px;')}"></select>
      <div id="${prefix}-jadwal-jam-warning" style="display:none; margin:0 4px 8px; color:#b91c1c; font-size:12px; font-weight:600;"></div>
      <select class="jadwal-field" id="${prefix}-jadwal-hari" style="${getJadwalInsetFieldStyle('margin-bottom:8px;')}">
        <option value="">-- Pilih Hari --</option>
        <option value="senin">Senin</option>
        <option value="selasa">Selasa</option>
        <option value="rabu">Rabu</option>
        <option value="kamis">Kamis</option>
        <option value="jumat">Jumat</option>
        <option value="sabtu">Sabtu</option>
        <option value="minggu">Minggu</option>
      </select>
      <button class="modal-btn modal-btn-primary" onclick="${saveHandler}()">Simpan</button>
      <button class="modal-btn modal-btn-secondary" onclick="closeJadwalModal('${modalId}')" style="margin-left:8px;">Batal</button>
      <span onclick="closeJadwalModal('${modalId}')" style="position:absolute;top:8px;right:12px;cursor:pointer;font-size:18px;">&times;</span>
    </div>
  `
  document.body.appendChild(modal)
}

function closeJadwalModal(modalId) {
  const modal = document.getElementById(modalId)
  if (!modal) return
  modal.style.display = 'none'
}
function createJamPelajaranModal(modalId, title, saveHandler) {
  let modal = document.getElementById(modalId)
  if (modal) modal.remove()

  const prefix = modalId === 'add-jam-pelajaran-modal' ? 'modal-add-jam' : 'modal-edit-jam'
  modal = document.createElement('div')
  modal.id = modalId
  modal.style.position = 'fixed'
  modal.style.top = '0'
  modal.style.left = '0'
  modal.style.width = '100vw'
  modal.style.height = '100vh'
  modal.style.background = 'rgba(0,0,0,0.3)'
  modal.style.display = 'none'
  modal.style.zIndex = '9999'
  modal.innerHTML = `
    <div style="background:#fff; margin:60px auto; padding:24px; border-radius:8px; width:380px; max-width:calc(100vw - 24px); box-shadow:0 2px 12px #0002; position:relative;">
      <h3 style="margin-top:0;">${title}</h3>
      <input class="jadwal-field" type="text" id="${prefix}-nama" placeholder="Nama Jam (contoh: Jam 1)" style="${getJadwalInsetFieldStyle('margin-bottom:8px;')}">
      <div style="display:grid; grid-template-columns:1fr 1fr; gap:8px; margin-bottom:12px;">
        <input class="jadwal-field" type="time" id="${prefix}-jam-mulai" style="${getJadwalInsetFieldStyle()}">
        <input class="jadwal-field" type="time" id="${prefix}-jam-selesai" style="${getJadwalInsetFieldStyle()}">
      </div>
      <button class="modal-btn modal-btn-primary" onclick="${saveHandler}()">Simpan</button>
      <button class="modal-btn modal-btn-secondary" onclick="closeJamPelajaranModal('${modalId}')" style="margin-left:8px;">Batal</button>
      <span onclick="closeJamPelajaranModal('${modalId}')" style="position:absolute;top:8px;right:12px;cursor:pointer;font-size:18px;">&times;</span>
    </div>
  `
  document.body.appendChild(modal)
}

function closeJamPelajaranModal(modalId) {
  const modal = document.getElementById(modalId)
  if (!modal) return
  modal.style.display = 'none'
}

function getMapelOptionsForKelas(kelasId) {
  const map = new Map()
  ;(jadwalDistribusiList || []).forEach(item => {
    if (String(item.kelas_id || '') !== String(kelasId || '')) return
    const mapel = item?.mapel
    if (!mapel?.id) return
    const label = `${mapel.nama || '-'}${mapel.kategori ? ` (${mapel.kategori})` : ''}`
    map.set(String(mapel.id), label)
  })
  return Array.from(map.entries()).sort((a, b) => a[1].localeCompare(b[1]))
}

function populateJadwalMapelByKelas(prefix, selectedMapelId = '') {
  const kelasSelect = document.getElementById(`${prefix}-jadwal-kelas`)
  const mapelSelect = document.getElementById(`${prefix}-jadwal-mapel`)
  if (!kelasSelect || !mapelSelect) return

  const kelasId = kelasSelect.value || ''
  const options = getMapelOptionsForKelas(kelasId)
  mapelSelect.innerHTML = '<option value="">-- Pilih Mapel --</option>'
  options.forEach(([id, label]) => {
    const opt = document.createElement('option')
    opt.value = id
    opt.textContent = label
    mapelSelect.appendChild(opt)
  })
  if (selectedMapelId) mapelSelect.value = String(selectedMapelId)
}

function populateJamSlotSelect(prefix, selectedJamMulai = '', selectedJamSelesai = '') {
  const select = document.getElementById(`${prefix}-jadwal-jam-slot`)
  const warning = document.getElementById(`${prefix}-jadwal-jam-warning`)
  if (!select) return

  select.innerHTML = '<option value="">-- Pilih Jam Pelajaran --</option>'
  ;(jamPelajaranList || []).forEach(item => {
    const opt = document.createElement('option')
    opt.value = String(item.id)
    opt.textContent = `${item.nama || 'Jam'} (${toTimeLabel(item.jam_mulai)} - ${toTimeLabel(item.jam_selesai)})`
    select.appendChild(opt)
  })

  if (selectedJamMulai && selectedJamSelesai) {
    const match = (jamPelajaranList || []).find(item =>
      getJamKey(item.jam_mulai, item.jam_selesai) === getJamKey(selectedJamMulai, selectedJamSelesai)
    )
    if (match) {
      select.value = String(match.id)
      if (warning) {
        warning.textContent = ''
        warning.style.display = 'none'
      }
    } else if (warning) {
      warning.textContent = 'Jadwal lama memakai jam yang belum ada di daftar Jam Pelajaran. Pilih ulang jam.'
      warning.style.display = 'block'
    }
  } else if (warning) {
    warning.textContent = ''
    warning.style.display = 'none'
  }
}

function fillJadwalForm(prefix, selected = {}) {
  const selectedDistribusi = selected.distribusi_id
    ? (jadwalDistribusiList || []).find(item => String(item.id) === String(selected.distribusi_id))
    : null

  const kelasSelect = document.getElementById(`${prefix}-jadwal-kelas`)
  if (kelasSelect) {
    const kelasMap = new Map()
    ;(jadwalDistribusiList || []).forEach(item => {
      const kelas = item?.kelas
      if (!kelas?.id) return
      kelasMap.set(String(kelas.id), kelas.nama_kelas || '-')
    })
    kelasSelect.innerHTML = '<option value="">-- Pilih Kelas --</option>'
    Array.from(kelasMap.entries())
      .sort((a, b) => a[1].localeCompare(b[1]))
      .forEach(([id, nama]) => {
        const opt = document.createElement('option')
        opt.value = id
        opt.textContent = nama
        kelasSelect.appendChild(opt)
      })
    kelasSelect.value = selectedDistribusi?.kelas_id ? String(selectedDistribusi.kelas_id) : ''
  }

  populateJadwalMapelByKelas(prefix, selectedDistribusi?.mapel_id ? String(selectedDistribusi.mapel_id) : '')
  populateJamSlotSelect(prefix, selected.jam_mulai, selected.jam_selesai)

  const kelasSelectForBind = document.getElementById(`${prefix}-jadwal-kelas`)
  if (kelasSelectForBind && kelasSelectForBind.dataset.bound !== 'true') {
    kelasSelectForBind.addEventListener('change', () => populateJadwalMapelByKelas(prefix))
    kelasSelectForBind.dataset.bound = 'true'
  }

  const hariSelect = document.getElementById(`${prefix}-jadwal-hari`)
  if (hariSelect) hariSelect.value = selected.hari ? normalizeHari(selected.hari) : ''
}

function buildJadwalPayload(prefix) {
  const kelas_id = String(document.getElementById(`${prefix}-jadwal-kelas`)?.value || '')
  const mapel_id = String(document.getElementById(`${prefix}-jadwal-mapel`)?.value || '')
  const jam_pelajaran_id = String(document.getElementById(`${prefix}-jadwal-jam-slot`)?.value || '')
  const hari = normalizeHari(document.getElementById(`${prefix}-jadwal-hari`)?.value || '')

  if (!kelas_id || !mapel_id || !jam_pelajaran_id || !hari) {
    return { error: 'Kelas, mapel, jam pelajaran, dan hari wajib diisi' }
  }
  if (!currentActiveSemesterId) {
    return { error: 'Semester aktif tidak ditemukan. Aktifkan semester terlebih dahulu.' }
  }

  const matchedDistribusi = (jadwalDistribusiList || []).filter(item =>
    String(item.kelas_id || '') === kelas_id &&
    String(item.mapel_id || '') === mapel_id &&
    String(item.semester_id || '') === String(currentActiveSemesterId)
  )

  if (matchedDistribusi.length === 0) {
    return { error: 'Distribusi mapel untuk kelas + mapel pada semester aktif belum ada' }
  }
  if (matchedDistribusi.length > 1) {
    return { error: 'Ditemukan lebih dari satu guru untuk kelas + mapel pada semester aktif. Rapikan data Distribusi Mapel terlebih dahulu.' }
  }

  const jam = (jamPelajaranList || []).find(item => String(item.id) === jam_pelajaran_id)
  if (!jam) return { error: 'Jam pelajaran tidak valid. Pilih ulang.' }

  return {
    payload: {
      distribusi_id: String(matchedDistribusi[0].id),
      hari,
      jam_mulai: toTimeLabel(jam.jam_mulai),
      jam_selesai: toTimeLabel(jam.jam_selesai)
    }
  }
}

function buildJamPelajaranPayload(prefix) {
  const nama = String(document.getElementById(`${prefix}-nama`)?.value || '').trim()
  const jam_mulai = String(document.getElementById(`${prefix}-jam-mulai`)?.value || '')
  const jam_selesai = String(document.getElementById(`${prefix}-jam-selesai`)?.value || '')

  if (!nama || !jam_mulai || !jam_selesai) {
    return { error: 'Nama jam, jam mulai, dan jam selesai wajib diisi' }
  }
  if (jam_selesai <= jam_mulai) {
    return { error: 'Jam selesai harus lebih besar dari jam mulai' }
  }

  return { payload: { nama, jam_mulai, jam_selesai } }
}

async function ensureDistribusiLoadedForForm() {
  if ((jadwalDistribusiList || []).length > 0) return true
  jadwalDistribusiList = await getDistribusiOptions()
  return jadwalDistribusiList.length > 0
}

async function ensureJamPelajaranLoadedForForm() {
  if ((jamPelajaranList || []).length > 0) return true
  try {
    const activeSemester = await getActiveSemesterForJadwal()
    currentActiveTahunAjaranId = activeSemester?.tahun_ajaran_id ? String(activeSemester.tahun_ajaran_id) : currentActiveTahunAjaranId
    jamPelajaranList = await getJamPelajaranRows(currentActiveTahunAjaranId)
  } catch (error) {
    console.error(error)
    jamPelajaranList = []
  }
  return jamPelajaranList.length > 0
}

async function openAddJadwalModal() {
  const [distribusiReady, jamReady] = await Promise.all([
    ensureDistribusiLoadedForForm(),
    ensureJamPelajaranLoadedForForm()
  ])

  if (!distribusiReady) {
    if (!currentActiveSemesterId) {
      alert('Semester aktif belum ada. Aktifkan semester terlebih dahulu.')
      return
    }
    alert('Data Distribusi Mapel untuk semester aktif belum tersedia. Isi dulu Distribusi Mapel.')
    return
  }
  if (!jamReady) {
    alert('Data Jam Pelajaran belum ada. Isi dulu tab Jam Pelajaran.')
    return
  }

  createJadwalModal('add-jadwal-modal', 'Tambah Jadwal Pelajaran', 'tambahJadwalPelajaran')
  fillJadwalForm('modal-add')
  document.getElementById('add-jadwal-modal').style.display = 'block'
}

async function tambahJadwalPelajaran() {
  const { payload, error: payloadError } = buildJadwalPayload('modal-add')
  if (payloadError) {
    alert(payloadError)
    return
  }

  const { error } = await sb.from('jadwal_pelajaran').insert([payload])
  if (error) {
    console.error(error)
    alert(`Gagal menambah jadwal: ${error.message || 'Unknown error'}`)
    return
  }

  closeJadwalModal('add-jadwal-modal')
  clearAllJadwalCaches()
  await loadJadwalPelajaran(true)
}

async function openEditJadwalModal(id) {
  await Promise.all([ensureDistribusiLoadedForForm(), ensureJamPelajaranLoadedForForm()])
  const row = (jadwalList || []).find(item => String(item.id) === String(id))
  if (!row) {
    alert('Data jadwal tidak ditemukan')
    return
  }

  currentEditJadwalId = id
  createJadwalModal('edit-jadwal-modal', 'Edit Jadwal Pelajaran', 'simpanEditJadwalPelajaran')
  fillJadwalForm('modal-edit', row)
  document.getElementById('edit-jadwal-modal').style.display = 'block'
}

async function simpanEditJadwalPelajaran() {
  if (!currentEditJadwalId) return

  const { payload, error: payloadError } = buildJadwalPayload('modal-edit')
  if (payloadError) {
    alert(payloadError)
    return
  }

  const { error } = await sb.from('jadwal_pelajaran').update(payload).eq('id', currentEditJadwalId)
  if (error) {
    console.error(error)
    alert(`Gagal mengubah jadwal: ${error.message || 'Unknown error'}`)
    return
  }

  closeJadwalModal('edit-jadwal-modal')
  currentEditJadwalId = null
  clearAllJadwalCaches()
  await loadJadwalPelajaran(true)
}

async function hapusJadwal(id) {
  const confirmed = typeof showPopupConfirm === 'function'
    ? await showPopupConfirm('Yakin ingin hapus jadwal ini?')
    : confirm('Yakin ingin hapus jadwal ini?')
  if (!confirmed) return

  const { error } = await sb.from('jadwal_pelajaran').delete().eq('id', id)
  if (error) {
    console.error(error)
    alert('Gagal menghapus jadwal')
    return
  }

  clearAllJadwalCaches()
  await loadJadwalPelajaran(true)
}

function openAddJamPelajaranModal() {
  createJamPelajaranModal('add-jam-pelajaran-modal', 'Tambah Jam Pelajaran', 'tambahJamPelajaran')
  document.getElementById('add-jam-pelajaran-modal').style.display = 'block'
}

async function tambahJamPelajaran() {
  const { payload, error: payloadError } = buildJamPelajaranPayload('modal-add-jam')
  if (payloadError) {
    alert(payloadError)
    return
  }

  const supportTahunAjaran = await checkJamPelajaranSupportsTahunAjaran()
  if (supportTahunAjaran && !currentActiveTahunAjaranId) {
    alert('Tahun ajaran aktif belum ada. Aktifkan tahun ajaran terlebih dahulu.')
    return
  }

  const finalPayload = { ...payload }
  if (supportTahunAjaran) finalPayload.tahun_ajaran_id = currentActiveTahunAjaranId

  const { error } = await sb.from('jam_pelajaran').insert([finalPayload])
  if (error) {
    console.error(error)
    alert(`Gagal menambah jam pelajaran: ${error.message || 'Unknown error'}`)
    return
  }

  closeJamPelajaranModal('add-jam-pelajaran-modal')
  clearAllJamPelajaranCaches()
  await loadJamPelajaran(true)
}

function openEditJamPelajaranModal(id) {
  const row = (jamPelajaranList || []).find(item => String(item.id) === String(id))
  if (!row) {
    alert('Data jam pelajaran tidak ditemukan')
    return
  }

  currentEditJamPelajaranId = id
  createJamPelajaranModal('edit-jam-pelajaran-modal', 'Edit Jam Pelajaran', 'simpanEditJamPelajaran')
  document.getElementById('modal-edit-jam-nama').value = row.nama || ''
  document.getElementById('modal-edit-jam-jam-mulai').value = toTimeLabel(row.jam_mulai)
  document.getElementById('modal-edit-jam-jam-selesai').value = toTimeLabel(row.jam_selesai)
  document.getElementById('edit-jam-pelajaran-modal').style.display = 'block'
}

async function simpanEditJamPelajaran() {
  if (!currentEditJamPelajaranId) return

  const { payload, error: payloadError } = buildJamPelajaranPayload('modal-edit-jam')
  if (payloadError) {
    alert(payloadError)
    return
  }

  const supportTahunAjaran = await checkJamPelajaranSupportsTahunAjaran()
  if (supportTahunAjaran && !currentActiveTahunAjaranId) {
    alert('Tahun ajaran aktif belum ada. Aktifkan tahun ajaran terlebih dahulu.')
    return
  }

  const finalPayload = { ...payload }
  if (supportTahunAjaran) finalPayload.tahun_ajaran_id = currentActiveTahunAjaranId

  const { error } = await sb.from('jam_pelajaran').update(finalPayload).eq('id', currentEditJamPelajaranId)
  if (error) {
    console.error(error)
    alert(`Gagal mengubah jam pelajaran: ${error.message || 'Unknown error'}`)
    return
  }

  closeJamPelajaranModal('edit-jam-pelajaran-modal')
  currentEditJamPelajaranId = null
  clearAllJamPelajaranCaches()
  await loadJamPelajaran(true)
}

async function hapusJamPelajaran(id) {
  const confirmed = typeof showPopupConfirm === 'function'
    ? await showPopupConfirm('Yakin ingin hapus jam pelajaran ini?')
    : confirm('Yakin ingin hapus jam pelajaran ini?')
  if (!confirmed) return

  const { error } = await sb.from('jam_pelajaran').delete().eq('id', id)
  if (error) {
    console.error(error)
    alert('Gagal menghapus jam pelajaran')
    return
  }

  clearAllJamPelajaranCaches()
  await loadJamPelajaran(true)
}

async function loadJamPelajaran(forceRefresh = false) {
  const container = document.getElementById('list-jam-pelajaran')
  if (!container) return

  const activeSemester = await getActiveSemesterForJadwal()
  const tahunAjaranId = String(activeSemester?.tahun_ajaran_id || currentActiveTahunAjaranId || 'none')
  currentActiveTahunAjaranId = tahunAjaranId === 'none' ? '' : tahunAjaranId
  const cacheVersion = getJadwalCacheVersion()
  const dynamicCacheKey = `${JAM_PELAJARAN_CACHE_KEY}:${tahunAjaranId}:${cacheVersion}`
  currentJamCacheKey = dynamicCacheKey
  rememberJamCacheKey(dynamicCacheKey)

  if (!forceRefresh && typeof getCachedData === 'function') {
    const cached = getCachedData(dynamicCacheKey, JAM_PELAJARAN_CACHE_TTL_MS)
    if (Array.isArray(cached)) {
      jamPelajaranList = cached
      renderJamPelajaranTable()
      return
    }
  }

  container.innerHTML = 'Loading...'
  try {
    jamPelajaranList = await getJamPelajaranRows(currentActiveTahunAjaranId)
    setCachedData?.(dynamicCacheKey, jamPelajaranList)
    renderJamPelajaranTable()
  } catch (error) {
    console.error(error)
    container.innerHTML = `Gagal load jam pelajaran: ${error.message || 'Unknown error'}`
  }
}

async function loadJadwalPelajaran(forceRefresh = false) {
  const container = document.getElementById('list-jadwal')
  if (!container) return

  const activeSemester = await getActiveSemesterForJadwal()
  currentActiveSemesterId = activeSemester?.id ? String(activeSemester.id) : ''
  const contextTahunId = String(activeSemester?.tahun_ajaran_id || 'none')
  const contextSemesterId = String(activeSemester?.id || 'none')
  const cacheVersion = getJadwalCacheVersion()
  const dynamicCacheKey = `${JADWAL_CACHE_KEY}:${contextTahunId}:${contextSemesterId}:${cacheVersion}`
  currentJadwalCacheKey = dynamicCacheKey
  rememberJadwalCacheKey(dynamicCacheKey)

  if (!forceRefresh && typeof getCachedData === 'function') {
    const cached = getCachedData(dynamicCacheKey, JADWAL_CACHE_TTL_MS)
    if (cached && Array.isArray(cached.jadwalList) && Array.isArray(cached.jadwalDistribusiList)) {
      jadwalList = cached.jadwalList
      jadwalDistribusiList = cached.jadwalDistribusiList
      renderJadwalKelasFilterOptions()
      renderJadwalTable()
      return
    }
  }

  container.innerHTML = 'Loading...'
  try {
    const [distribusiList, rows] = await Promise.all([
      getDistribusiOptions(activeSemester),
      getJadwalRows(),
      loadJamPelajaran()
    ])

    jadwalDistribusiList = distribusiList || []
    jadwalList = rows || []

    setCachedData?.(dynamicCacheKey, { jadwalList, jadwalDistribusiList })
    renderJadwalKelasFilterOptions()
    renderJadwalTable()
  } catch (error) {
    console.error(error)
    container.innerHTML = `Gagal load jadwal: ${error.message || 'Unknown error'}`
  }
}

function setupJadwalFilterHandlers() {
  const search = document.getElementById('jadwal-search-input')
  const hari = document.getElementById('jadwal-filter-hari')
  const kelas = document.getElementById('jadwal-filter-kelas')

  if (search && !search.dataset.bound) {
    search.addEventListener('input', renderJadwalTable)
    search.dataset.bound = 'true'
  }
  if (hari && !hari.dataset.bound) {
    hari.addEventListener('change', renderJadwalTable)
    hari.dataset.bound = 'true'
  }
  if (kelas && !kelas.dataset.bound) {
    kelas.addEventListener('change', renderJadwalTable)
    kelas.dataset.bound = 'true'
  }
}

function initJadwalPage() {
  ensureJadwalFieldStyle()
  setupJadwalFilterHandlers()
  setJadwalSubtab(currentJadwalSubtab || 'jadwal')
}
