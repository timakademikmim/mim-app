let currentSantriDetailId = null
let currentExtraFieldKeys = []
let currentSantriDetailData = null
let currentSantriKelasList = []
let currentSantriDetailTab = 'biodata'
let currentEditNilaiId = null
let currentNilaiActiveSemesterId = ''
let currentNilaiFilterSemesterId = ''
let currentNilaiSemesterList = []
let currentNilaiFilterKelasId = 'all'
let currentNilaiKelasList = []

function getSantriIdFromParams(params = {}) {
  if (params && params.santriId) return params.santriId
  return null
}

function backToSantriList() {
  if (typeof loadPage === 'function') {
    loadPage('santri')
  }
}

function setSantriDetailHeaderName(name) {
  const headerName = document.getElementById('santri-detail-header-name')
  if (!headerName) return
  const safeName = String(name || '').trim()
  headerName.textContent = safeName || 'Detail Santri'
}

function setSantriDetailTab(tab) {
  const validTab = tab === 'nilai' ? 'nilai' : 'biodata'
  currentSantriDetailTab = validTab

  const tabButtons = document.querySelectorAll('.santri-detail-tab-btn')
  tabButtons.forEach(button => {
    button.classList.toggle('active', button.getAttribute('data-santri-detail-tab') === validTab)
  })

  const panes = document.querySelectorAll('.santri-detail-pane')
  panes.forEach(pane => pane.classList.remove('active'))
  const activePane = document.getElementById(`santri-detail-pane-${validTab}`)
  if (activePane) activePane.classList.add('active')

  if (currentSantriDetailId) {
    localStorage.setItem('admin_last_page', 'santri-detail')
    localStorage.setItem('admin_last_page_params', JSON.stringify({
      santriId: currentSantriDetailId,
      tab: validTab
    }))
  }

  if (validTab === 'nilai' && currentSantriDetailId) {
    loadSantriNilaiList()
  }
}

function toLabel(fieldName) {
  return fieldName
    .replace(/_/g, ' ')
    .replace(/\b\w/g, char => char.toUpperCase())
}

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

function getInsetFieldStyle(extra = '') {
  return `width:100%; padding:10px 12px; box-sizing:border-box; border:1px solid #cbd5e1; border-radius:999px; background:#f8fafc; box-shadow:none; outline:none; transition:border-color 0.2s, box-shadow 0.2s; ${extra}`
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

function getKelasLabel(kelas) {
  const label = pickLabelByKeys(kelas, ['nama_kelas', 'nama', 'label', 'kode', 'kelas'])
  return label || `Kelas #${kelas?.id ?? '-'}`
}

function getJenjangFromKelasTingkat(rawTingkat) {
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

function getSelectedKelasForNilai() {
  const kelasId = currentNilaiFilterKelasId && currentNilaiFilterKelasId !== 'all'
    ? String(currentNilaiFilterKelasId)
    : String(currentSantriDetailData?.kelas_id || '')
  if (!kelasId) return null
  const kelasFromFilter = (currentNilaiKelasList || []).find(item => String(item.id) === kelasId)
  const kelasFromCurrent = (currentSantriKelasList || []).find(item => String(item.id) === kelasId)
  return kelasFromFilter || kelasFromCurrent || null
}

async function getMapelByKelasTingkatan(kelasId) {
  if (!kelasId) return []

  let kelas = (currentNilaiKelasList || []).find(item => String(item.id) === String(kelasId))
  if (!kelas) kelas = (currentSantriKelasList || []).find(item => String(item.id) === String(kelasId))

  if (!kelas || kelas.tingkat === null || kelas.tingkat === undefined) {
    const { data, error } = await sb
      .from('kelas')
      .select('id, tingkat')
      .eq('id', kelasId)
      .maybeSingle()
    if (!error && data) kelas = data
  }

  const jenjang = getJenjangFromKelasTingkat(kelas?.tingkat)
  const mapelOptions = await getMapelOptions()
  if (!jenjang) return mapelOptions || []

  return (mapelOptions || []).filter(item => {
    const tingkatan = normalizeMapelTingkatan(item?.tingkatan || item?.jenjang)
    if (!tingkatan) return true
    return tingkatan === jenjang
  })
}

async function getMapelByDistribusi(kelasId, semesterId = '') {
  if (!kelasId) return []

  let query = sb
    .from('distribusi_mapel')
    .select(`
      mapel_id,
      mapel:mapel_id (
        id,
        nama,
        kategori
      )
    `)
    .eq('kelas_id', kelasId)

  if (semesterId) query = query.eq('semester_id', semesterId)

  const { data, error } = await query
  if (error) {
    console.error(error)
    return []
  }

  const map = new Map()
  ;(data || []).forEach(row => {
    const mapel = row.mapel
    const key = String(mapel?.id || row.mapel_id || '')
    if (!key || map.has(key)) return
    map.set(key, {
      id: key,
      nama: mapel?.nama || '-',
      kategori: mapel?.kategori || null
    })
  })
  return Array.from(map.values())
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

function validateNilaiRange(value, label, maxValue) {
  if (value === null || value === undefined) return true
  if (value < 0) {
    alert(`${label} tidak boleh kurang dari 0`)
    return false
  }
  if (value > maxValue) {
    alert(`${label} maksimal ${maxValue}`)
    return false
  }
  return true
}

function ensureSantriDetailActionStyle() {
  if (document.getElementById('santri-detail-action-style')) return

  const style = document.createElement('style')
  style.id = 'santri-detail-action-style'
  style.innerHTML = `
    #santri-nilai-list-container .btn-edit,
    #santri-nilai-list-container .btn-hapus {
      border: none;
      border-radius: 999px;
      padding: 7px 14px;
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
      transition: background-color .2s ease, transform .08s ease;
      margin: 0 3px;
    }
    #santri-nilai-list-container .btn-edit {
      background: #e6f4ea;
      color: #166534;
    }
    #santri-nilai-list-container .btn-edit:hover {
      background: #d1f0da;
    }
    #santri-nilai-list-container .btn-hapus {
      background: #fee2e2;
      color: #991b1b;
    }
    #santri-nilai-list-container .btn-hapus:hover {
      background: #fecaca;
    }
    #santri-nilai-list-container .btn-edit:active,
    #santri-nilai-list-container .btn-hapus:active {
      transform: scale(0.98);
    }
  `
  document.head.appendChild(style)
}

function ensureSantriDetailFieldFocusStyle() {
  if (document.getElementById('santri-detail-field-focus-style')) return

  const style = document.createElement('style')
  style.id = 'santri-detail-field-focus-style'
  style.innerHTML = `
    #santri-detail-biodata-container input[type="text"]:focus,
    #santri-detail-biodata-container input[type="date"]:focus,
    #santri-detail-biodata-container select:focus {
      border-color: #16a34a !important;
      box-shadow: 0 0 0 3px rgba(22, 163, 74, 0.25) !important;
    }
  `
  document.head.appendChild(style)
}

function calculateNilaiParts(prefix) {
  const nilai_tugas = toNullableNumber(document.getElementById(`${prefix}-nilai-tugas`)?.value || '')
  const nilai_ulangan_harian = toNullableNumber(document.getElementById(`${prefix}-nilai-ulangan-harian`)?.value || '')
  const nilai_pts = toNullableNumber(document.getElementById(`${prefix}-nilai-pts`)?.value || '')
  const nilai_pas = toNullableNumber(document.getElementById(`${prefix}-nilai-pas`)?.value || '')
  const nilai_kehadiran = toNullableNumber(document.getElementById(`${prefix}-nilai-kehadiran`)?.value || '')
  return { nilai_tugas, nilai_ulangan_harian, nilai_pts, nilai_pas, nilai_kehadiran }
}

function updateNilaiAkhirField(prefix, values) {
  const nilaiAkhirEl = document.getElementById(`${prefix}-nilai-akhir`)
  if (!nilaiAkhirEl) return
  const nilaiAkhir = hitungNilaiAkhir(values)
  nilaiAkhirEl.value = nilaiAkhir === null ? '' : String(nilaiAkhir)
}

function updateNilaiWarning(prefix, values) {
  const warningEl = document.getElementById(`${prefix}-nilai-warning`)
  if (!warningEl) return

  const rules = [
    { key: 'nilai_tugas', label: 'Nilai Tugas', max: 5 },
    { key: 'nilai_ulangan_harian', label: 'Nilai Ulangan Harian', max: 10 },
    { key: 'nilai_pts', label: 'Nilai PTS', max: 25 },
    { key: 'nilai_pas', label: 'Nilai PAS', max: 50 }
  ]

  let message = ''
  for (const rule of rules) {
    const value = values[rule.key]
    if (value === null || value === undefined) continue
    if (Number.isNaN(value)) {
      message = `${rule.label} harus berupa angka yang valid.`
      break
    }
    if (value < 0) {
      message = `${rule.label} tidak boleh kurang dari 0.`
      break
    }
    if (value > rule.max) {
      message = `${rule.label} melebihi maksimal ${rule.max}.`
      break
    }
  }

  warningEl.textContent = message
  warningEl.style.display = message ? 'block' : 'none'
}

function bindNilaiFormAuto(prefix) {
  const inputIds = [
    `${prefix}-nilai-tugas`,
    `${prefix}-nilai-ulangan-harian`,
    `${prefix}-nilai-pts`,
    `${prefix}-nilai-pas`,
    `${prefix}-nilai-kehadiran`
  ]

  const recalc = () => {
    const values = calculateNilaiParts(prefix)
    updateNilaiAkhirField(prefix, values)
    updateNilaiWarning(prefix, values)
  }

  inputIds.forEach(id => {
    const input = document.getElementById(id)
    if (!input) return
    input.oninput = recalc
  })
  recalc()
}

async function getTahunAktifDetail() {
  const { data, error } = await sb
    .from('tahun_ajaran')
    .select('id')
    .eq('aktif', true)
    .single()

  if (error) {
    console.error(error)
    return null
  }
  return data
}

async function getKelasAktifForDetail() {
  const tahunAktif = await getTahunAktifDetail()
  if (!tahunAktif) return []

  const { data, error } = await sb
    .from('kelas')
    .select('id, nama_kelas, tingkat')
    .eq('tahun_ajaran_id', tahunAktif.id)
    .order('nama_kelas')

  if (error) {
    console.error(error)
    return []
  }

  return data || []
}

async function isNisnUsedByOther(nisn, santriId) {
  if (!nisn) return false

  const { data, error } = await sb
    .from('santri')
    .select('id')
    .eq('nisn', nisn)
    .neq('id', santriId)
    .limit(1)

  if (error) {
    console.error(error)
    alert('Gagal validasi NISN')
    return true
  }

  return data && data.length > 0
}

function getKnownFields() {
  return [
    'id',
    'nama',
    'nisn',
    'jenis_kelamin',
    'kelas_id',
    'kelas',
    'aktif',
    'created_at',
    'updated_at'
  ]
}

function renderExtraFields(santri) {
  const known = new Set(getKnownFields())
  currentExtraFieldKeys = Object.keys(santri).filter(key => !known.has(key))

  if (currentExtraFieldKeys.length === 0) {
    return `
      <div style="margin-top:18px;">
        <h3 style="margin-bottom:8px;">Biodata Tambahan</h3>
        <p style="margin:0; color:#666;">Belum ada field biodata tambahan di tabel santri.</p>
      </div>
    `
  }

  let html = `
    <div style="margin-top:18px;">
      <h3 style="margin-bottom:8px;">Biodata Tambahan</h3>
  `

  currentExtraFieldKeys.forEach(key => {
    const value = santri[key]
    const inputType = key.toLowerCase().includes('tanggal') ? 'date' : 'text'
    const inputValue = value ?? ''

    if (typeof value === 'boolean') {
      html += `
        <div style="margin-bottom:10px;">
          <label style="display:flex; gap:8px; align-items:center;">
            <input type="checkbox" id="extra-${key}" ${value ? 'checked' : ''}>
            ${toLabel(key)}
          </label>
        </div>
      `
      return
    }

    html += `
      <div style="margin-bottom:10px;">
        <label for="extra-${key}" style="display:block; margin-bottom:4px;">${toLabel(key)}</label>
        <input
          type="${inputType}"
          id="extra-${key}"
          value="${escapeHtml(inputValue)}"
          style="${getInsetFieldStyle()}"
        >
      </div>
    `
  })

  html += '</div>'
  return html
}

function renderDetailForm(santri, kelasList) {
  const kelasOptions = kelasList.map(kelas => {
    const selected = String(kelas.id) === String(santri.kelas_id) ? 'selected' : ''
    return `<option value="${kelas.id}" ${selected}>${kelas.nama_kelas}</option>`
  }).join('')

  const container = document.getElementById('santri-detail-biodata-container')
  if (!container) return

  container.innerHTML = `
    <div style="margin-bottom:10px; color:#555;">
      <strong>ID:</strong> ${santri.id}
    </div>

    <div style="margin-bottom:10px;">
      <label for="detail-nama" style="display:block; margin-bottom:4px;">Nama</label>
      <input type="text" id="detail-nama" value="${escapeHtml(santri.nama ?? '')}" style="${getInsetFieldStyle()}">
    </div>

    <div style="margin-bottom:10px;">
      <label for="detail-nisn" style="display:block; margin-bottom:4px;">NISN</label>
      <input type="text" id="detail-nisn" value="${escapeHtml(santri.nisn ?? '')}" style="${getInsetFieldStyle()}">
    </div>

    <div style="margin-bottom:10px;">
      <label for="detail-jk" style="display:block; margin-bottom:4px;">Jenis Kelamin</label>
      <select id="detail-jk" style="${getInsetFieldStyle()}">
        <option value="">-- Jenis Kelamin --</option>
        <option value="L" ${santri.jenis_kelamin === 'L' ? 'selected' : ''}>Laki-laki</option>
        <option value="P" ${santri.jenis_kelamin === 'P' ? 'selected' : ''}>Perempuan</option>
      </select>
    </div>

    <div style="margin-bottom:10px;">
      <label for="detail-kelas-id" style="display:block; margin-bottom:4px;">Kelas</label>
      <select id="detail-kelas-id" style="${getInsetFieldStyle()}">
        <option value="">-- Pilih Kelas --</option>
        ${kelasOptions}
      </select>
    </div>

    <div style="margin-bottom:10px;">
      <label style="display:flex; gap:8px; align-items:center;">
        <input type="checkbox" id="detail-aktif" ${santri.aktif ? 'checked' : ''}>
        Aktif
      </label>
    </div>

    ${renderExtraFields(santri)}

    <div style="margin-top:18px;">
      <button onclick="saveSantriDetail()">Simpan Perubahan</button>
    </div>
  `
}

function renderNilaiSection() {
  const container = document.getElementById('santri-detail-nilai-container')
  if (!container) return

  container.innerHTML = `
    <div style="margin-bottom:12px; display:flex; justify-content:space-between; gap:10px; align-items:center; flex-wrap:wrap;">
      <div style="display:flex; align-items:center; gap:8px; flex-wrap:wrap;">
        <label for="santri-nilai-kelas-filter" style="font-size:12px; color:#475569; font-weight:600;">Kelas</label>
        <select id="santri-nilai-kelas-filter" onchange="onNilaiKelasFilterChange()" style="${getInsetFieldStyle('width:auto; min-width:220px;')}">
          <option value="">Loading kelas...</option>
        </select>
        <label for="santri-nilai-semester-filter" style="font-size:12px; color:#475569; font-weight:600;">Tampilkan Semester</label>
        <select id="santri-nilai-semester-filter" onchange="onNilaiSemesterFilterChange()" style="${getInsetFieldStyle('width:auto; min-width:220px;')}">
          <option value="">Loading semester...</option>
        </select>
      </div>
      <button class="modal-btn modal-btn-primary" type="button" onclick="openAddNilaiModal()">Tambah Nilai</button>
    </div>
    <div id="santri-nilai-list-container">Loading...</div>
  `
}

function createAddNilaiModal() {
  let modal = document.getElementById('add-nilai-modal')
  if (modal) modal.remove()

  modal = document.createElement('div')
  modal.id = 'add-nilai-modal'
  modal.style.position = 'fixed'
  modal.style.top = '0'
  modal.style.left = '0'
  modal.style.width = '100vw'
  modal.style.height = '100vh'
  modal.style.background = 'rgba(0,0,0,0.3)'
  modal.style.display = 'none'
  modal.style.zIndex = '9999'
  modal.innerHTML = `
    <div style="background:#fff; margin:36px auto; padding:24px; border-radius:8px; width:420px; max-width:calc(100vw - 24px); box-shadow:0 2px 12px #0002; position:relative;">
      <h3>Tambah Nilai Akademik</h3>
      <select id="modal-add-nilai-mapel" style="${getInsetFieldStyle('margin-bottom:8px;')}"></select>
      <select id="modal-add-nilai-semester" style="${getInsetFieldStyle('margin-bottom:8px;')}"></select>
      <input type="number" step="0.01" id="modal-add-nilai-tugas" placeholder="Nilai Tugas" style="${getInsetFieldStyle('margin-bottom:8px;')}">
      <input type="number" step="0.01" id="modal-add-nilai-ulangan-harian" placeholder="Nilai Ulangan Harian" style="${getInsetFieldStyle('margin-bottom:8px;')}">
      <input type="number" step="0.01" id="modal-add-nilai-pts" placeholder="Nilai PTS" style="${getInsetFieldStyle('margin-bottom:8px;')}">
      <input type="number" step="0.01" id="modal-add-nilai-pas" placeholder="Nilai PAS" style="${getInsetFieldStyle('margin-bottom:8px;')}">
      <div id="modal-add-nilai-warning" style="display:none; margin:0 4px 8px; color:#b91c1c; font-size:12px; font-weight:600;"></div>
      <input type="number" step="0.01" id="modal-add-nilai-kehadiran" placeholder="Nilai Kehadiran" style="${getInsetFieldStyle('margin-bottom:8px;')}">
      <input type="number" step="0.01" id="modal-add-nilai-akhir" readonly placeholder="Nilai Pengetahuan (otomatis jumlah seluruh nilai)" style="${getInsetFieldStyle('margin-bottom:8px; background:#f1f5f9; color:#334155; cursor:not-allowed;')}">
      <input type="number" step="0.01" id="modal-add-nilai-keterampilan" placeholder="Nilai Keterampilan" style="${getInsetFieldStyle('margin-bottom:12px;')}">
      <button class="modal-btn modal-btn-primary" onclick="tambahNilaiAkademik()">Simpan</button>
      <button class="modal-btn modal-btn-secondary" onclick="closeAddNilaiModal()" style="margin-left:8px;">Batal</button>
      <span onclick="closeAddNilaiModal()" style="position:absolute;top:8px;right:12px;cursor:pointer;font-size:18px;">&times;</span>
    </div>
  `
  document.body.appendChild(modal)
}

function closeAddNilaiModal() {
  const modal = document.getElementById('add-nilai-modal')
  if (!modal) return
  modal.style.display = 'none'
}

function createEditNilaiModal() {
  let modal = document.getElementById('edit-nilai-modal')
  if (modal) modal.remove()

  modal = document.createElement('div')
  modal.id = 'edit-nilai-modal'
  modal.style.position = 'fixed'
  modal.style.top = '0'
  modal.style.left = '0'
  modal.style.width = '100vw'
  modal.style.height = '100vh'
  modal.style.background = 'rgba(0,0,0,0.3)'
  modal.style.display = 'none'
  modal.style.zIndex = '9999'
  modal.innerHTML = `
    <div style="background:#fff; margin:36px auto; padding:24px; border-radius:8px; width:420px; max-width:calc(100vw - 24px); box-shadow:0 2px 12px #0002; position:relative;">
      <h3>Edit Nilai Akademik</h3>
      <select id="modal-edit-nilai-mapel" style="${getInsetFieldStyle('margin-bottom:8px;')}"></select>
      <select id="modal-edit-nilai-semester" style="${getInsetFieldStyle('margin-bottom:8px;')}"></select>
      <input type="number" step="0.01" id="modal-edit-nilai-tugas" placeholder="Nilai Tugas" style="${getInsetFieldStyle('margin-bottom:8px;')}">
      <input type="number" step="0.01" id="modal-edit-nilai-ulangan-harian" placeholder="Nilai Ulangan Harian" style="${getInsetFieldStyle('margin-bottom:8px;')}">
      <input type="number" step="0.01" id="modal-edit-nilai-pts" placeholder="Nilai PTS" style="${getInsetFieldStyle('margin-bottom:8px;')}">
      <input type="number" step="0.01" id="modal-edit-nilai-pas" placeholder="Nilai PAS" style="${getInsetFieldStyle('margin-bottom:8px;')}">
      <div id="modal-edit-nilai-warning" style="display:none; margin:0 4px 8px; color:#b91c1c; font-size:12px; font-weight:600;"></div>
      <input type="number" step="0.01" id="modal-edit-nilai-kehadiran" placeholder="Nilai Kehadiran" style="${getInsetFieldStyle('margin-bottom:8px;')}">
      <input type="number" step="0.01" id="modal-edit-nilai-akhir" readonly placeholder="Nilai Pengetahuan (otomatis jumlah seluruh nilai)" style="${getInsetFieldStyle('margin-bottom:8px; background:#f1f5f9; color:#334155; cursor:not-allowed;')}">
      <input type="number" step="0.01" id="modal-edit-nilai-keterampilan" placeholder="Nilai Keterampilan" style="${getInsetFieldStyle('margin-bottom:12px;')}">
      <button class="modal-btn modal-btn-primary" onclick="simpanEditNilaiAkademik()">Simpan</button>
      <button class="modal-btn modal-btn-secondary" onclick="closeEditNilaiModal()" style="margin-left:8px;">Batal</button>
      <span onclick="closeEditNilaiModal()" style="position:absolute;top:8px;right:12px;cursor:pointer;font-size:18px;">&times;</span>
    </div>
  `
  document.body.appendChild(modal)
}

function closeEditNilaiModal() {
  const modal = document.getElementById('edit-nilai-modal')
  if (!modal) return
  modal.style.display = 'none'
  currentEditNilaiId = null
}

async function getMapelOptions() {
  const { data, error } = await sb
    .from('mapel')
    .select('*')
    .order('nama')
  if (error) {
    console.error(error)
    return []
  }
  return data || []
}

async function getSemesterOptions() {
  const tahunAktif = await getTahunAktifDetail()
  const tahunAjaranId = tahunAktif?.id || ''
  if (!tahunAjaranId) return []

  const { data, error } = await sb
    .from('semester')
    .select('*')
    .eq('tahun_ajaran_id', tahunAjaranId)
    .eq('aktif', true)
    .order('id')
  if (error) {
    console.error(error)
    return []
  }
  return data || []
}

async function getAllSemesterOptionsForNilai() {
  const { data, error } = await sb
    .from('semester')
    .select('id, nama, aktif, tahun_ajaran_id')
    .order('id', { ascending: false })

  if (error) {
    console.error(error)
    return []
  }
  return data || []
}

async function getRiwayatKelasForNilai() {
  if (!currentSantriDetailId) return []

  const { data, error } = await sb
    .from('riwayat_kelas_santri')
    .select(`
      kelas_id,
      aktif,
      tanggal_mulai,
      kelas:kelas_id (
        id,
        nama_kelas,
        tingkat
      )
    `)
    .eq('santri_id', currentSantriDetailId)
    .order('aktif', { ascending: false })
    .order('tanggal_mulai', { ascending: false })

  if (error) {
    console.error(error)
    return []
  }

  const map = new Map()
  ;(data || []).forEach(row => {
    const key = String(row.kelas_id || '')
    if (!key || map.has(key)) return
    map.set(key, {
      id: key,
      nama_kelas: row.kelas?.nama_kelas || `Kelas #${key}`,
      tingkat: row.kelas?.tingkat ?? null,
      aktif: row.aktif ?? false
    })
  })
  return Array.from(map.values())
}

function renderNilaiKelasFilter() {
  const select = document.getElementById('santri-nilai-kelas-filter')
  if (!select) return

  select.innerHTML = ''
  const allOption = document.createElement('option')
  allOption.value = 'all'
  allOption.textContent = 'Semua Kelas'
  select.appendChild(allOption)

  ;(currentNilaiKelasList || []).forEach(item => {
    const option = document.createElement('option')
    option.value = String(item.id)
    option.textContent = `${getKelasLabel(item)}${item.aktif ? ' (aktif)' : ''}`
    select.appendChild(option)
  })

  select.value = currentNilaiFilterKelasId || 'all'
}

function getDefaultNilaiKelasFilterId() {
  const activeClass = (currentNilaiKelasList || []).find(item => item.aktif)
  if (activeClass?.id) return String(activeClass.id)
  if (currentSantriDetailData?.kelas_id) return String(currentSantriDetailData.kelas_id)
  return 'all'
}

async function ensureNilaiKelasFilterState() {
  const kelasList = await getRiwayatKelasForNilai()
  currentNilaiKelasList = kelasList || []

  if (currentNilaiKelasList.length === 0 && currentSantriDetailData?.kelas_id) {
    const currentKelasId = String(currentSantriDetailData.kelas_id)
    const currentKelas = (currentSantriKelasList || []).find(item => String(item.id) === currentKelasId)
    currentNilaiKelasList = [{
      id: currentKelasId,
      nama_kelas: currentKelas?.nama_kelas || `Kelas #${currentKelasId}`,
      aktif: true
    }]
  }

  const hasFilter = (currentNilaiKelasList || []).some(item => String(item.id) === String(currentNilaiFilterKelasId))
  if (!currentNilaiFilterKelasId || currentNilaiFilterKelasId === '') {
    const activeClass = (currentNilaiKelasList || []).find(item => item.aktif)
    if (activeClass) {
      currentNilaiFilterKelasId = String(activeClass.id)
    } else if (currentSantriDetailData?.kelas_id) {
      currentNilaiFilterKelasId = String(currentSantriDetailData.kelas_id)
    } else {
      currentNilaiFilterKelasId = 'all'
    }
  } else if (currentNilaiFilterKelasId !== 'all' && !hasFilter) {
    const activeClass = (currentNilaiKelasList || []).find(item => item.aktif)
    if (activeClass) {
      currentNilaiFilterKelasId = String(activeClass.id)
    } else if (currentSantriDetailData?.kelas_id) {
      currentNilaiFilterKelasId = String(currentSantriDetailData.kelas_id)
    } else {
      currentNilaiFilterKelasId = 'all'
    }
  }

  renderNilaiKelasFilter()
}

function onNilaiKelasFilterChange() {
  const select = document.getElementById('santri-nilai-kelas-filter')
  if (!select) return
  currentNilaiFilterKelasId = select.value || 'all'
  currentNilaiFilterSemesterId = getDefaultNilaiSemesterFilterId()
  renderNilaiSemesterFilter()
  loadSantriNilaiList()
}

async function getActiveSemesterDetailForNilai() {
  const tahunAktif = await getTahunAktifDetail()
  const tahunAjaranId = tahunAktif?.id || ''
  if (!tahunAjaranId) return null

  const { data, error } = await sb
    .from('semester')
    .select('id, nama, aktif, tahun_ajaran_id')
    .eq('tahun_ajaran_id', tahunAjaranId)
    .eq('aktif', true)
    .order('id', { ascending: false })
    .limit(1)
    .maybeSingle()

  if (error) {
    console.error(error)
    return null
  }
  return data || null
}

function renderNilaiSemesterFilter() {
  const select = document.getElementById('santri-nilai-semester-filter')
  if (!select) return

  const rows = currentNilaiSemesterList || []
  select.innerHTML = ''

  const allOption = document.createElement('option')
  allOption.value = 'all'
  allOption.textContent = 'Semua Semester'
  select.appendChild(allOption)

  rows.forEach(item => {
    const option = document.createElement('option')
    option.value = String(item.id)
    const activeTag = item.aktif ? ' (aktif)' : ''
    option.textContent = `${getSemesterLabel(item)}${activeTag}`
    select.appendChild(option)
  })

  const fallbackValue = currentNilaiActiveSemesterId || (rows[0] ? String(rows[0].id) : 'all')
  select.value = currentNilaiFilterSemesterId || fallbackValue
}

function getDefaultNilaiSemesterFilterId() {
  if (currentNilaiActiveSemesterId) return String(currentNilaiActiveSemesterId)
  if ((currentNilaiSemesterList || []).length > 0) return String(currentNilaiSemesterList[0].id)
  return 'all'
}

async function ensureNilaiSemesterFilterState() {
  const [activeSemester, semesterList] = await Promise.all([
    getActiveSemesterDetailForNilai(),
    getAllSemesterOptionsForNilai()
  ])

  currentNilaiSemesterList = semesterList || []
  currentNilaiActiveSemesterId = activeSemester?.id ? String(activeSemester.id) : ''

  const hasCurrentFilter = (currentNilaiSemesterList || []).some(item => String(item.id) === String(currentNilaiFilterSemesterId))
  if (!currentNilaiFilterSemesterId) {
    currentNilaiFilterSemesterId = getDefaultNilaiSemesterFilterId()
  } else if (currentNilaiFilterSemesterId !== 'all' && !hasCurrentFilter) {
    currentNilaiFilterSemesterId = getDefaultNilaiSemesterFilterId()
  }

  renderNilaiSemesterFilter()
}

function onNilaiSemesterFilterChange() {
  const select = document.getElementById('santri-nilai-semester-filter')
  if (!select) return
  currentNilaiFilterSemesterId = select.value || getDefaultNilaiSemesterFilterId()
  loadSantriNilaiList()
}

function renderSelectOptions(selectEl, rows, placeholder, labelGetter, selectedId = '') {
  if (!selectEl) return
  selectEl.innerHTML = `<option value="">${placeholder}</option>`
  ;(rows || []).forEach(row => {
    const option = document.createElement('option')
    option.value = row.id
    option.textContent = labelGetter(row)
    if (selectedId && String(selectedId) === String(row.id)) option.selected = true
    selectEl.appendChild(option)
  })
}

async function fillNilaiFormOptions(mapelSelect, semesterSelect, selectedMapel = '', selectedSemester = '') {
  const semesterOptions = await getSemesterOptions()
  const kelas = getSelectedKelasForNilai()
  const filteredMapelOptions = await getMapelByKelasTingkatan(
    kelas?.id ? String(kelas.id) : ''
  )

  renderSelectOptions(
    mapelSelect,
    filteredMapelOptions,
    '-- Pilih Mapel --',
    item => `${item.nama ?? '-'}${item.kategori ? ` (${item.kategori})` : ''}`,
    selectedMapel
  )
  if (selectedMapel && mapelSelect) {
    const exists = (filteredMapelOptions || []).some(item => String(item.id) === String(selectedMapel))
    if (!exists) {
      const selectedMapelObj = (filteredMapelOptions || []).find(item => String(item.id) === String(selectedMapel))
      const option = document.createElement('option')
      option.value = selectedMapel
      option.textContent = selectedMapelObj
        ? `${selectedMapelObj.nama ?? '-'}${selectedMapelObj.kategori ? ` (${selectedMapelObj.kategori})` : ''}`
        : `Mapel #${selectedMapel}`
      option.selected = true
      mapelSelect.appendChild(option)
    }
  }
  renderSelectOptions(
    semesterSelect,
    semesterOptions,
    '-- Pilih Semester --',
    item => getSemesterLabel(item),
    selectedSemester
  )

  if (selectedSemester && semesterSelect) {
    const exists = (semesterOptions || []).some(item => String(item.id) === String(selectedSemester))
    if (!exists) {
      const fallbackOption = document.createElement('option')
      fallbackOption.value = selectedSemester
      fallbackOption.textContent = `Semester #${selectedSemester} (nonaktif)`
      fallbackOption.selected = true
      semesterSelect.appendChild(fallbackOption)
    }
  }
}

function hitungNilaiAkhir(payload) {
  const values = [
    payload.nilai_tugas,
    payload.nilai_ulangan_harian,
    payload.nilai_pts,
    payload.nilai_pas,
    payload.nilai_kehadiran
  ]
    .filter(v => v !== null && v !== undefined)
  if (values.length === 0) return null
  const total = values.reduce((sum, value) => sum + value, 0)
  return Number(total.toFixed(2))
}

function buildNilaiPayload(prefix) {
  const mapelId = document.getElementById(`${prefix}-nilai-mapel`)?.value || ''
  const semesterId = document.getElementById(`${prefix}-nilai-semester`)?.value || ''
  const nilai_keterampilan = toNullableNumber(document.getElementById(`${prefix}-nilai-keterampilan`)?.value || '')

  const {
    nilai_tugas,
    nilai_ulangan_harian,
    nilai_pts,
    nilai_pas,
    nilai_kehadiran
  } = calculateNilaiParts(prefix)

  if ([nilai_tugas, nilai_ulangan_harian, nilai_pts, nilai_pas, nilai_kehadiran, nilai_keterampilan].some(v => Number.isNaN(v))) {
    return { error: 'Nilai harus berupa angka yang valid' }
  }

  if (!mapelId || !semesterId) {
    return { error: 'Mapel dan semester wajib diisi' }
  }

  if (!validateNilaiRange(nilai_tugas, 'Nilai Tugas', 5)) return { error: '__shown__' }
  if (!validateNilaiRange(nilai_ulangan_harian, 'Nilai Ulangan Harian', 10)) return { error: '__shown__' }
  if (!validateNilaiRange(nilai_pts, 'Nilai PTS', 25)) return { error: '__shown__' }
  if (!validateNilaiRange(nilai_pas, 'Nilai PAS', 50)) return { error: '__shown__' }

  const payload = {
    santri_id: currentSantriDetailId,
    mapel_id: mapelId,
    semester_id: semesterId,
    nilai_tugas,
    nilai_ulangan_harian,
    nilai_pts,
    nilai_pas,
    nilai_kehadiran,
    nilai_keterampilan
  }
  payload.nilai_akhir = hitungNilaiAkhir(payload)
  return { payload }
}

async function openAddNilaiModal() {
  createAddNilaiModal()
  const semesterSelect = document.getElementById('modal-add-nilai-semester')
  await fillNilaiFormOptions(
    document.getElementById('modal-add-nilai-mapel'),
    semesterSelect
  )
  bindNilaiFormAuto('modal-add')
  if (semesterSelect && semesterSelect.options.length <= 1) {
    alert('Belum ada semester aktif pada tahun ajaran aktif')
  }
  document.getElementById('add-nilai-modal').style.display = 'block'
}

async function tambahNilaiAkademik() {
  if (!currentSantriDetailId) return

  const { payload, error: payloadError } = buildNilaiPayload('modal-add')
  if (payloadError) {
    if (payloadError !== '__shown__') alert(payloadError)
    return
  }

  const { error } = await sb
    .from('nilai_akademik')
    .insert([payload])

  if (error) {
    console.error(error)
    if (error.code === '23505') {
      alert('Nilai mapel pada semester ini sudah ada')
      return
    }
    alert('Gagal menambah nilai akademik')
    return
  }

  closeAddNilaiModal()
  loadSantriNilaiList(true)
}

async function showEditNilaiForm(id) {
  createEditNilaiModal()
  const nilai = (window.santriNilaiList || []).find(item => String(item.id) === String(id))
  if (!nilai) {
    alert('Data nilai tidak ditemukan')
    return
  }

  currentEditNilaiId = id
  await fillNilaiFormOptions(
    document.getElementById('modal-edit-nilai-mapel'),
    document.getElementById('modal-edit-nilai-semester'),
    nilai.mapel_id,
    nilai.semester_id
  )
  document.getElementById('modal-edit-nilai-tugas').value = toInputValue(nilai.nilai_tugas)
  document.getElementById('modal-edit-nilai-ulangan-harian').value = toInputValue(nilai.nilai_ulangan_harian)
  document.getElementById('modal-edit-nilai-pts').value = toInputValue(nilai.nilai_pts)
  document.getElementById('modal-edit-nilai-pas').value = toInputValue(nilai.nilai_pas)
  document.getElementById('modal-edit-nilai-kehadiran').value = toInputValue(nilai.nilai_kehadiran)
  document.getElementById('modal-edit-nilai-akhir').value = toInputValue(nilai.nilai_akhir)
  document.getElementById('modal-edit-nilai-keterampilan').value = toInputValue(nilai.nilai_keterampilan)
  bindNilaiFormAuto('modal-edit')

  document.getElementById('edit-nilai-modal').style.display = 'block'
}

async function simpanEditNilaiAkademik() {
  if (!currentSantriDetailId || !currentEditNilaiId) return

  const { payload, error: payloadError } = buildNilaiPayload('modal-edit')
  if (payloadError) {
    if (payloadError !== '__shown__') alert(payloadError)
    return
  }

  const { error } = await sb
    .from('nilai_akademik')
    .update(payload)
    .eq('id', currentEditNilaiId)

  if (error) {
    console.error(error)
    if (error.code === '23505') {
      alert('Nilai mapel pada semester ini sudah ada')
      return
    }
    alert('Gagal mengubah nilai akademik')
    return
  }

  closeEditNilaiModal()
  loadSantriNilaiList(true)
}

async function hapusNilaiAkademik(id) {
  const confirmed = typeof showPopupConfirm === 'function'
    ? await showPopupConfirm('Yakin ingin hapus data nilai ini?')
    : confirm('Yakin ingin hapus data nilai ini?')
  if (!confirmed) return

  const { error } = await sb
    .from('nilai_akademik')
    .delete()
    .eq('id', id)

  if (error) {
    console.error(error)
    alert('Gagal menghapus nilai akademik')
    return
  }

  loadSantriNilaiList(true)
}

async function loadSantriNilaiList() {
  const container = document.getElementById('santri-nilai-list-container')
  if (!container || !currentSantriDetailId) return
  if (!currentNilaiKelasList.length) {
    await ensureNilaiKelasFilterState()
  }
  if (!currentNilaiSemesterList.length) {
    await ensureNilaiSemesterFilterState()
  }

  container.innerHTML = 'Loading...'
  const { data, error } = await sb
    .from('nilai_akademik')
    .select(`
      id,
      santri_id,
      kelas_id,
      mapel_id,
      semester_id,
      nilai_tugas,
      nilai_ulangan_harian,
      nilai_pts,
      nilai_pas,
      nilai_kehadiran,
      nilai_akhir,
      nilai_keterampilan,
      mapel:mapel_id ( nama, kategori ),
      semester:semester_id ( * )
    `)
    .eq('santri_id', currentSantriDetailId)
    .order('id', { ascending: false })

  if (error) {
    console.error(error)
    container.innerHTML = 'Gagal load nilai akademik'
    return
  }

  const nilaiList = data || []
  const nilaiByKelas = currentNilaiFilterKelasId && currentNilaiFilterKelasId !== 'all'
    ? nilaiList.filter(item => String(item.kelas_id || '') === String(currentNilaiFilterKelasId))
    : nilaiList

  const filteredNilaiList = currentNilaiFilterSemesterId && currentNilaiFilterSemesterId !== 'all'
    ? nilaiByKelas.filter(item => String(item.semester_id) === String(currentNilaiFilterSemesterId))
    : nilaiByKelas
  let displayList = filteredNilaiList

  if (currentNilaiFilterSemesterId !== 'all') {
    const selectedKelas = getSelectedKelasForNilai()
    const targetKelasId = selectedKelas?.id
      ? String(selectedKelas.id)
      : (currentSantriDetailData?.kelas_id ? String(currentSantriDetailData.kelas_id) : '')
    const mapelByTingkatan = await getMapelByKelasTingkatan(targetKelasId)
    const nilaiForCompose = targetKelasId
      ? filteredNilaiList.filter(item => String(item.kelas_id || '') === targetKelasId)
      : filteredNilaiList
    const nilaiByMapelId = new Map(nilaiForCompose.map(item => [String(item.mapel_id), item]))
    const semesterObj = (currentNilaiSemesterList || []).find(sem => String(sem.id) === String(currentNilaiFilterSemesterId)) || null

    displayList = (mapelByTingkatan || []).map(mapel => {
      const existing = nilaiByMapelId.get(String(mapel.id))
      if (existing) return existing
      return {
        id: null,
        santri_id: currentSantriDetailId,
        kelas_id: targetKelasId || null,
        mapel_id: mapel.id,
        semester_id: currentNilaiFilterSemesterId,
        nilai_tugas: null,
        nilai_ulangan_harian: null,
        nilai_pts: null,
        nilai_pas: null,
        nilai_kehadiran: null,
        nilai_akhir: null,
        nilai_keterampilan: null,
        mapel: { nama: mapel.nama, kategori: mapel.kategori },
        semester: semesterObj
      }
    })
  }
  window.santriNilaiList = filteredNilaiList

  if (displayList.length === 0) {
    container.innerHTML = 'Belum ada data nilai akademik.'
    return
  }

  let html = `
    <div style="overflow-x:auto;">
      <table style="width:100%; border-collapse:collapse; font-size:13px;">
        <thead>
          <tr style="background:#f3f3f3;">
            <th style="padding:8px; border:1px solid #ddd; text-align:center;">Mapel</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center;">Semester</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:90px;">Tugas</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:120px;">Ulangan Harian</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:90px;">PTS</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:90px;">PAS</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:90px;">Kehadiran</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:130px;">Nilai Pengetahuan</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:130px;">Nilai Keterampilan</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:180px;">Aksi</th>
          </tr>
        </thead>
        <tbody>
  `

  html += displayList.map(item => {
    const mapelNama = item.mapel?.nama
      ? `${item.mapel.nama}${item.mapel?.kategori ? ` (${item.mapel.kategori})` : ''}`
      : '-'
    const semesterNama = item.semester ? getSemesterLabel(item.semester) : '-'

    return `
      <tr>
        <td style="padding:8px; border:1px solid #ddd;">${escapeHtml(mapelNama)}</td>
        <td style="padding:8px; border:1px solid #ddd;">${escapeHtml(semesterNama)}</td>
        <td style="padding:8px; border:1px solid #ddd; text-align:center;">${toInputValue(item.nilai_tugas) || '-'}</td>
        <td style="padding:8px; border:1px solid #ddd; text-align:center;">${toInputValue(item.nilai_ulangan_harian) || '-'}</td>
        <td style="padding:8px; border:1px solid #ddd; text-align:center;">${toInputValue(item.nilai_pts) || '-'}</td>
        <td style="padding:8px; border:1px solid #ddd; text-align:center;">${toInputValue(item.nilai_pas) || '-'}</td>
        <td style="padding:8px; border:1px solid #ddd; text-align:center;">${toInputValue(item.nilai_kehadiran) || '-'}</td>
        <td style="padding:8px; border:1px solid #ddd; text-align:center;">${toInputValue(item.nilai_akhir) || '-'}</td>
        <td style="padding:8px; border:1px solid #ddd; text-align:center;">${toInputValue(item.nilai_keterampilan) || '-'}</td>
        <td style="padding:8px; border:1px solid #ddd; text-align:center; white-space:nowrap;">
          ${item.id
            ? `<button class="btn-edit" onclick="showEditNilaiForm('${item.id}')">Edit</button>
               <button class="btn-hapus" onclick="hapusNilaiAkademik('${item.id}')">Hapus</button>`
            : '<span style="color:#94a3b8; font-size:12px;">Belum ada nilai</span>'}
        </td>
      </tr>
    `
  }).join('')

  html += '</tbody></table></div>'
  container.innerHTML = html
}

async function saveSantriDetail() {
  if (!currentSantriDetailId) return

  const nama = document.getElementById('detail-nama')?.value.trim() || ''
  const nisn = document.getElementById('detail-nisn')?.value.trim() || ''
  const jenis_kelamin = document.getElementById('detail-jk')?.value || ''
  const kelas_id = document.getElementById('detail-kelas-id')?.value || ''
  const aktif = document.getElementById('detail-aktif')?.checked ?? true

  if (!nama || !jenis_kelamin || !kelas_id) {
    alert('Nama, jenis kelamin, dan kelas wajib diisi')
    return
  }

  if (await isNisnUsedByOther(nisn, currentSantriDetailId)) {
    alert('NISN sudah terdaftar')
    return
  }

  const payload = {
    nama,
    nisn: nisn || null,
    jenis_kelamin,
    kelas_id,
    aktif
  }

  currentExtraFieldKeys.forEach(key => {
    const el = document.getElementById(`extra-${key}`)
    if (!el) return

    if (el.type === 'checkbox') {
      payload[key] = el.checked
      return
    }

    const rawValue = el.value.trim()
    payload[key] = rawValue === '' ? null : rawValue
  })

  const { error } = await sb
    .from('santri')
    .update(payload)
    .eq('id', currentSantriDetailId)

  if (error) {
    console.error(error)
    if (error.code === '23505') {
      alert('NISN sudah terdaftar')
      return
    }
    alert('Gagal menyimpan biodata santri')
    return
  }

  if (typeof clearCachedData === 'function') clearCachedData('santri:list')
  alert('Biodata santri berhasil diperbarui')
  await initSantriDetailPage({ santriId: currentSantriDetailId, tab: 'biodata' })
}

async function initSantriDetailPage(params = {}) {
  ensureSantriDetailFieldFocusStyle()
  ensureSantriDetailActionStyle()

  const santriId = getSantriIdFromParams(params)
  currentSantriDetailId = santriId
  currentSantriDetailTab = params?.tab === 'nilai' ? 'nilai' : 'biodata'
  currentNilaiActiveSemesterId = ''
  currentNilaiFilterSemesterId = ''
  currentNilaiSemesterList = []
  currentNilaiFilterKelasId = 'all'
  currentNilaiKelasList = []

  const biodataContainer = document.getElementById('santri-detail-biodata-container')
  const nilaiContainer = document.getElementById('santri-detail-nilai-container')
  if (!biodataContainer || !nilaiContainer) return

  if (!santriId) {
    setSantriDetailHeaderName('')
    biodataContainer.innerHTML = 'ID santri tidak ditemukan.'
    nilaiContainer.innerHTML = 'ID santri tidak ditemukan.'
    return
  }

  setSantriDetailHeaderName('')
  biodataContainer.innerHTML = 'Loading...'
  nilaiContainer.innerHTML = 'Loading...'
  setSantriDetailTab(currentSantriDetailTab)

  const [{ data: santri, error: santriError }, kelasList] = await Promise.all([
    sb
      .from('santri')
      .select('*')
      .eq('id', santriId)
      .single(),
    getKelasAktifForDetail()
  ])

  if (santriError || !santri) {
    console.error(santriError)
    setSantriDetailHeaderName('')
    biodataContainer.innerHTML = 'Gagal load data santri.'
    nilaiContainer.innerHTML = 'Gagal load data santri.'
    return
  }

  currentSantriDetailData = santri
  setSantriDetailHeaderName(santri.nama)
  currentSantriKelasList = kelasList
  renderDetailForm(currentSantriDetailData, currentSantriKelasList)
  createAddNilaiModal()
  createEditNilaiModal()
  renderNilaiSection()
  setSantriDetailTab(currentSantriDetailTab)
  await ensureNilaiKelasFilterState()
  await ensureNilaiSemesterFilterState()
  currentNilaiFilterKelasId = getDefaultNilaiKelasFilterId()
  currentNilaiFilterSemesterId = getDefaultNilaiSemesterFilterId()
  renderNilaiKelasFilter()
  renderNilaiSemesterFilter()
  await loadSantriNilaiList()
}
