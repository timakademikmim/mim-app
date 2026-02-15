// =============================
// Ambil Tahun Ajaran Aktif
// =============================
let currentEditSantriId = null
let currentEditExtraFieldKeys = []
let santriSearchOutsideClickBound = false
let santriSortField = 'nama'
let santriSortDirection = 'asc'
const santriSelectFilters = {
  kelas: '',
  kamar: '',
  status: ''
}
const SANTRI_CACHE_KEY = 'santri:list'
const SANTRI_CACHE_TTL_MS = 2 * 60 * 1000

async function getTahunAktif() {
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


// =============================
// Helpers
// =============================
async function getKelasAktifList() {
  const tahunAktif = await getTahunAktif()
  if (!tahunAktif) return null

  const { data, error } = await sb
    .from('kelas')
    .select('id, nama_kelas')
    .eq('tahun_ajaran_id', tahunAktif.id)
    .order('nama_kelas')

  if (error) {
    console.error(error)
    return null
  }

  return data || []
}

function renderKelasOptions(select, kelasList, selectedId = '') {
  select.innerHTML = '<option value="">-- Pilih Kelas --</option>'
  kelasList.forEach(kelas => {
    const option = document.createElement('option')
    option.value = kelas.id
    option.textContent = kelas.nama_kelas
    if (selectedId && String(selectedId) === String(kelas.id)) {
      option.selected = true
    }
    select.appendChild(option)
  })
}

async function isNisnUsed(nisn, excludeId = null) {
  if (!nisn) return false

  let query = sb
    .from('santri')
    .select('id')
    .eq('nisn', nisn)
    .limit(1)

  if (excludeId) {
    query = query.neq('id', excludeId)
  }

  const { data, error } = await query
  if (error) {
    console.error(error)
    alert('Gagal validasi NISN')
    return true
  }

  return data && data.length > 0
}

function toLabel(fieldName) {
  return String(fieldName || '')
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

function ensureSantriFieldFocusStyle() {
  if (document.getElementById('santri-field-focus-style')) return

  const style = document.createElement('style')
  style.id = 'santri-field-focus-style'
  style.innerHTML = `
    .santri-field {
      border: 1px solid #cbd5e1;
      border-radius: 999px;
      padding: 9px 12px;
      box-sizing: border-box;
      background: #f8fafc;
      outline: none;
    }
    .santri-field:focus {
      border-color: #16a34a !important;
      box-shadow: 0 0 0 3px rgba(22, 163, 74, 0.25) !important;
    }
  `
  document.head.appendChild(style)
}

function getKnownEditFields() {
  return [
    'id',
    'nama',
    'nisn',
    'jenis_kelamin',
    'kelas_id',
    'aktif',
    'created_at',
    'updated_at',
    'kelas'
  ]
}

function renderEditExtraFields(santri) {
  const known = new Set(getKnownEditFields())
  currentEditExtraFieldKeys = Object.keys(santri || {}).filter(key => {
    if (known.has(key)) return false
    const value = santri[key]
    return value === null || typeof value !== 'object'
  })

  const extraContainer = document.getElementById('modal-edit-santri-extra-fields')
  if (!extraContainer) return

  if (currentEditExtraFieldKeys.length === 0) {
    extraContainer.innerHTML = ''
    return
  }

  let html = '<div style="margin:8px 0 12px;"><strong>Biodata Tambahan</strong></div>'

  currentEditExtraFieldKeys.forEach(key => {
    const value = santri[key]

    if (typeof value === 'boolean') {
      html += `
        <div style="margin-bottom:8px;">
          <label style="display:flex; gap:8px; align-items:center;">
            <input type="checkbox" id="modal-edit-extra-${key}" ${value ? 'checked' : ''}>
            ${toLabel(key)}
          </label>
        </div>
      `
      return
    }

    const inputType = key.toLowerCase().includes('tanggal') ? 'date' : 'text'
    const inputValue = inputType === 'date' && value
      ? String(value).slice(0, 10)
      : (value ?? '')
    html += `
      <div style="margin-bottom:8px;">
        <label for="modal-edit-extra-${key}" style="display:block; margin-bottom:3px;">${toLabel(key)}</label>
        <input
          class="santri-field"
          type="${inputType}"
          id="modal-edit-extra-${key}"
          value="${escapeHtml(inputValue)}"
          style="${getInsetFieldStyle()}"
        >
      </div>
    `
  })

  extraContainer.innerHTML = html
}

function ensureSantriActionStyle() {
  if (document.getElementById('santri-btn-style')) return

  const style = document.createElement('style')
  style.id = 'santri-btn-style'
  style.innerHTML = `
    .btn-detail {
      background: #0f766e;
      color: #fff;
      border: none;
      padding: 7px 16px;
      border-radius: 5px;
      margin-right: 6px;
      cursor: pointer;
      box-shadow: 0 0 8px 0 #0f766e55;
      transition: box-shadow 0.2s, transform 0.1s;
      font-weight: bold;
      outline: none;
    }
    .btn-detail:active {
      box-shadow: 0 0 18px 2px #0f766ecc, 0 0 0 2px #fff;
      transform: scale(0.96);
    }
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

function createAddSantriModal() {
  let modal = document.getElementById('add-santri-modal')
  if (modal) modal.remove()

  modal = document.createElement('div')
  modal.id = 'add-santri-modal'
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
      <h3>Tambah Santri</h3>
      <input class="santri-field" type="text" id="modal-add-santri-nama" placeholder="Nama Santri" style="${getInsetFieldStyle('margin-bottom:8px;')}">
      <input class="santri-field" type="text" id="modal-add-santri-nisn" placeholder="NISN" style="${getInsetFieldStyle('margin-bottom:8px;')}">
      <select class="santri-field" id="modal-add-santri-jk" style="${getInsetFieldStyle('margin-bottom:8px;')}">
        <option value="">-- Jenis Kelamin --</option>
        <option value="L">Laki-laki</option>
        <option value="P">Perempuan</option>
      </select>
      <select class="santri-field" id="modal-add-santri-kelas" style="${getInsetFieldStyle('margin-bottom:12px;')}">
        <option value="">Loading kelas...</option>
      </select>
      <button class="modal-btn modal-btn-primary" onclick="tambahSantri()">Simpan</button>
      <button class="modal-btn modal-btn-secondary" onclick="closeAddSantriModal()" style="margin-left:8px;">Batal</button>
      <span onclick="closeAddSantriModal()" style="position:absolute;top:8px;right:12px;cursor:pointer;font-size:18px;">&times;</span>
    </div>
  `
  document.body.appendChild(modal)
  return modal
}

async function openAddSantriModal() {
  createAddSantriModal()

  const kelasSelect = document.getElementById('modal-add-santri-kelas')
  kelasSelect.innerHTML = '<option value="">Loading kelas...</option>'

  const kelasList = await getKelasAktifList()
  if (kelasList === null) {
    kelasSelect.innerHTML = '<option value="">Tidak ada tahun aktif</option>'
  } else if (kelasList.length === 0) {
    kelasSelect.innerHTML = '<option value="">Tidak ada kelas</option>'
  } else {
    renderKelasOptions(kelasSelect, kelasList)
  }

  document.getElementById('add-santri-modal').style.display = 'block'
}

function closeAddSantriModal() {
  const modal = document.getElementById('add-santri-modal')
  if (!modal) return

  modal.style.display = 'none'
  document.getElementById('modal-add-santri-nama').value = ''
  document.getElementById('modal-add-santri-nisn').value = ''
  document.getElementById('modal-add-santri-jk').value = ''
  document.getElementById('modal-add-santri-kelas').value = ''
}

function createEditSantriModal() {
  let modal = document.getElementById('edit-santri-modal')
  if (modal) modal.remove()

  modal = document.createElement('div')
  modal.id = 'edit-santri-modal'
  modal.style.position = 'fixed'
  modal.style.top = '0'
  modal.style.left = '0'
  modal.style.width = '100vw'
  modal.style.height = '100vh'
  modal.style.background = 'rgba(0,0,0,0.3)'
  modal.style.display = 'none'
  modal.style.zIndex = '9999'
  modal.innerHTML = `
    <div style="background:#fff; margin:40px auto; padding:24px; border-radius:8px; width:420px; max-height:85vh; overflow-y:auto; box-shadow:0 2px 12px #0002; position:relative;">
      <h3>Edit Santri</h3>
      <input class="santri-field" type="text" id="modal-edit-santri-nama" placeholder="Nama Santri" style="${getInsetFieldStyle('margin-bottom:8px;')}">
      <input class="santri-field" type="text" id="modal-edit-santri-nisn" placeholder="NISN" style="${getInsetFieldStyle('margin-bottom:8px;')}">
      <select class="santri-field" id="modal-edit-santri-jk" style="${getInsetFieldStyle('margin-bottom:8px;')}">
        <option value="">-- Jenis Kelamin --</option>
        <option value="L">Laki-laki</option>
        <option value="P">Perempuan</option>
      </select>
      <select class="santri-field" id="modal-edit-santri-kelas" style="${getInsetFieldStyle('margin-bottom:12px;')}">
        <option value="">Loading kelas...</option>
      </select>
      <div style="margin-bottom:10px;">
        <label style="display:flex; gap:8px; align-items:center;">
          <input type="checkbox" id="modal-edit-santri-aktif">
          Aktif
        </label>
      </div>
      <div id="modal-edit-santri-extra-fields"></div>
      <button class="modal-btn modal-btn-primary" onclick="modalEditSantri()">Simpan</button>
      <button class="modal-btn modal-btn-secondary" onclick="closeEditSantriModal()" style="margin-left:8px;">Batal</button>
      <span onclick="closeEditSantriModal()" style="position:absolute;top:8px;right:12px;cursor:pointer;font-size:18px;">&times;</span>
    </div>
  `
  document.body.appendChild(modal)
  return modal
}


// =============================
// Tambah Santri
// =============================
async function tambahSantri() {
  const nama = document.getElementById('modal-add-santri-nama').value.trim()
  const nisn = document.getElementById('modal-add-santri-nisn').value.trim()
  const jk = document.getElementById('modal-add-santri-jk').value
  const kelas_id = document.getElementById('modal-add-santri-kelas').value

  if (!nama || !kelas_id) {
    alert('Nama dan kelas wajib diisi')
    return
  }

  if (await isNisnUsed(nisn)) {
    alert('NISN sudah terdaftar')
    return
  }

  const { error } = await sb.from('santri').insert([{
    nama: nama,
    nisn: nisn || null,
    jenis_kelamin: jk || null,
    kelas_id: kelas_id,
    aktif: true
  }])

  if (error) {
    console.error(error)
    if (error.code === '23505') {
      alert('NISN sudah terdaftar')
      return
    }
    alert('Gagal tambah santri')
    return
  }

  closeAddSantriModal()
  if (typeof clearCachedData === 'function') clearCachedData(SANTRI_CACHE_KEY)
  loadSantri(true)
}


// =============================
// Navigasi Detail
// =============================
function openSantriDetail(id) {
  if (typeof loadPage !== 'function') {
    alert('Gagal membuka halaman detail santri')
    return
  }
  loadPage('santri-detail', { santriId: id })
}


// =============================
// Edit Santri
// =============================
async function showEditSantriForm(id) {
  createEditSantriModal()

  const santri = window.santriList?.find(s => String(s.id) === String(id))
  if (!santri) {
    alert('Data santri tidak ditemukan')
    return
  }

  currentEditSantriId = id

  document.getElementById('modal-edit-santri-nama').value = santri.nama ?? ''
  document.getElementById('modal-edit-santri-nisn').value = santri.nisn ?? ''
  document.getElementById('modal-edit-santri-jk').value = santri.jenis_kelamin ?? ''
  document.getElementById('modal-edit-santri-aktif').checked = santri.aktif ?? true
  renderEditExtraFields(santri)

  const kelasSelect = document.getElementById('modal-edit-santri-kelas')
  kelasSelect.innerHTML = '<option value="">Loading kelas...</option>'
  const kelasList = await getKelasAktifList()
  if (kelasList && kelasList.length > 0) {
    renderKelasOptions(kelasSelect, kelasList, santri.kelas_id)
  } else {
    kelasSelect.innerHTML = '<option value="">Tidak ada kelas</option>'
  }

  document.getElementById('edit-santri-modal').style.display = 'block'
}

function closeEditSantriModal() {
  const modal = document.getElementById('edit-santri-modal')
  if (!modal) return
  modal.style.display = 'none'
  currentEditSantriId = null
  currentEditExtraFieldKeys = []
}

async function modalEditSantri() {
  if (!currentEditSantriId) return

  const nama = document.getElementById('modal-edit-santri-nama').value.trim()
  const nisn = document.getElementById('modal-edit-santri-nisn').value.trim()
  const jk = document.getElementById('modal-edit-santri-jk').value
  const kelas_id = document.getElementById('modal-edit-santri-kelas').value
  const aktif = document.getElementById('modal-edit-santri-aktif').checked

  if (!nama || !jk || !kelas_id) {
    alert('Nama, jenis kelamin dan kelas wajib diisi')
    return
  }

  if (await isNisnUsed(nisn, currentEditSantriId)) {
    alert('NISN sudah terdaftar')
    return
  }

  const payload = {
    nama: nama,
    nisn: nisn || null,
    jenis_kelamin: jk,
    kelas_id: kelas_id,
    aktif: aktif
  }

  currentEditExtraFieldKeys.forEach(key => {
    const el = document.getElementById(`modal-edit-extra-${key}`)
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
    .eq('id', currentEditSantriId)

  if (error) {
    console.error(error)
    if (error.code === '23505') {
      alert('NISN sudah terdaftar')
      return
    }
    alert('Gagal edit santri')
    return
  }

  closeEditSantriModal()
  if (typeof clearCachedData === 'function') clearCachedData(SANTRI_CACHE_KEY)
  loadSantri(true)
}


// =============================
// Hapus Santri
// =============================
async function hapusSantri(id) {
  const confirmed = typeof showPopupConfirm === 'function'
    ? await showPopupConfirm('Yakin ingin hapus santri ini?')
    : confirm('Yakin ingin hapus santri ini?')
  if (!confirmed) return

  const { error } = await sb
    .from('santri')
    .delete()
    .eq('id', id)

  if (error) {
    console.error(error)
    alert('Gagal hapus santri')
    return
  }

  if (typeof clearCachedData === 'function') clearCachedData(SANTRI_CACHE_KEY)
  loadSantri(true)
}

function getSantriSearchKeyword() {
  return (document.getElementById('santri-search-input')?.value || '').trim().toLowerCase()
}

function hasActiveSantriSelectFilters() {
  return Boolean(santriSelectFilters.kelas || santriSelectFilters.kamar || santriSelectFilters.status)
}

function applySantriSort(data) {
  const list = [...(data || [])]
  if (list.length === 0) return list

  const getSortValue = (item) => {
    if (santriSortField === 'nisn') return String(item?.nisn ?? '')
    return String(item?.nama ?? '')
  }

  list.sort((a, b) => {
    const aValue = getSortValue(a)
    const bValue = getSortValue(b)
    const cmp = aValue.localeCompare(bValue, undefined, { sensitivity: 'base' })
    return santriSortDirection === 'desc' ? -cmp : cmp
  })

  return list
}

function populateSantriSelectFilterOptions(data) {
  const kelasMap = new Map()
  const kamarSet = new Set()

  ;(data || []).forEach(s => {
    const kelasId = String(s?.kelas?.id ?? s?.kelas_id ?? '')
    const kelasNama = String(s?.kelas?.nama_kelas ?? '').trim()
    if (kelasId && kelasNama) kelasMap.set(kelasId, kelasNama)

    const kamar = String(s?.kamar ?? '').trim()
    if (kamar) kamarSet.add(kamar)
  })

  const kelasSelect = document.getElementById('santri-filter-kelas')
  const kamarSelect = document.getElementById('santri-filter-kamar')
  if (kelasSelect) {
    kelasSelect.innerHTML = '<option value="">Semua Kelas</option>'
    Array.from(kelasMap.entries())
      .sort((a, b) => a[1].localeCompare(b[1]))
      .forEach(([id, nama]) => {
        const opt = document.createElement('option')
        opt.value = id
        opt.textContent = nama
        kelasSelect.appendChild(opt)
      })
    kelasSelect.value = santriSelectFilters.kelas || ''
  }

  if (kamarSelect) {
    kamarSelect.innerHTML = '<option value="">Semua Kamar</option>'
    Array.from(kamarSet)
      .sort((a, b) => a.localeCompare(b))
      .forEach(kamar => {
        const opt = document.createElement('option')
        opt.value = kamar
        opt.textContent = kamar
        kamarSelect.appendChild(opt)
      })
    kamarSelect.value = santriSelectFilters.kamar || ''
  }
}

function renderSantriSearchTable() {
  const container = document.getElementById('list-santri')
  if (!container) return

  const data = Array.isArray(window.santriList) ? window.santriList : []
  const keyword = getSantriSearchKeyword()
  const filteredBySearch = !keyword
    ? data
    : data.filter(s => {
      const nama = String(s?.nama ?? '').toLowerCase()
      const nisn = String(s?.nisn ?? '').toLowerCase()
      return nama.includes(keyword) || nisn.includes(keyword)
    })

  const filteredBySelect = filteredBySearch.filter(s => {
    const kelasId = String(s?.kelas?.id ?? s?.kelas_id ?? '')
    const kamar = String(s?.kamar ?? '').trim()
    const statusValue = s?.aktif ? 'aktif' : 'tidak_aktif'

    const matchKelas = !santriSelectFilters.kelas || kelasId === santriSelectFilters.kelas
    const matchKamar = !santriSelectFilters.kamar || kamar === santriSelectFilters.kamar
    const matchStatus = !santriSelectFilters.status || statusValue === santriSelectFilters.status

    return matchKelas && matchKamar && matchStatus
  })

  const filteredData = applySantriSort(filteredBySelect)

  if (filteredData.length === 0) {
    container.innerHTML = (keyword || hasActiveSantriSelectFilters())
      ? 'Tidak ada data santri yang sesuai filter/pencarian.'
      : 'Belum ada santri'
    return
  }

  ensureSantriActionStyle()

  let html = `
    <div style="overflow-x:auto;">
    <table style="width:100%; border-collapse:collapse; margin-top:8px; font-size:13px;">
      <thead>
        <tr style="background:#f3f3f3;">
          <th style="padding:8px; border:1px solid #ddd; text-align:center; min-width:280px;">Nama</th>
          <th style="padding:8px; border:1px solid #ddd; text-align:center; width:120px;">NISN</th>
          <th style="padding:8px; border:1px solid #ddd; text-align:center; width:100px;">Jenis Kelamin</th>
          <th style="padding:8px; border:1px solid #ddd; text-align:center;">Kelas</th>
          <th style="padding:8px; border:1px solid #ddd; text-align:center; width:140px;">Halaqah</th>
          <th style="padding:8px; border:1px solid #ddd; text-align:center; width:120px;">Kamar</th>
          <th style="padding:8px; border:1px solid #ddd; text-align:center; width:90px;">Status</th>
          <th style="padding:8px; border:1px solid #ddd; text-align:center; width:300px;">Aksi</th>
        </tr>
      </thead>
      <tbody>
  `

  html += filteredData.map(s => `
    <tr onclick="openSantriDetail('${s.id}')" style="cursor:pointer;">
      <td style="padding:8px 8px 8px 14px; border:1px solid #ddd; position:relative; white-space:nowrap; min-width:280px;">
        <span style="position:absolute; left:0; top:0; bottom:0; width:4px; background:${s.aktif ? '#16a34a' : '#dc2626'};"></span>
        <span>${s.nama ?? '-'}</span>
      </td>
      <td style="padding:8px; border:1px solid #ddd;">${s.nisn ?? '-'}</td>
      <td style="padding:8px; border:1px solid #ddd;">${s.jenis_kelamin ?? '-'}</td>
      <td style="padding:8px; border:1px solid #ddd;">${s.kelas?.nama_kelas ?? '-'}</td>
      <td style="padding:8px; border:1px solid #ddd;">${s.halaqah ?? '-'}</td>
      <td style="padding:8px; border:1px solid #ddd;">${s.kamar ?? '-'}</td>
      <td style="padding:8px; border:1px solid #ddd;">${s.aktif ? 'Aktif' : 'Tidak Aktif'}</td>
      <td style="padding:8px; border:1px solid #ddd; text-align:center; white-space:nowrap;">
        <button class="btn-detail" onclick="event.stopPropagation(); openSantriDetail('${s.id}')">Detail</button>
        <button class="btn-edit" onclick="event.stopPropagation(); showEditSantriForm('${s.id}')">Edit</button>
        <button class="btn-hapus" onclick="event.stopPropagation(); hapusSantri('${s.id}')">Hapus</button>
      </td>
    </tr>
  `).join('')

  html += '</tbody></table></div>'
  container.innerHTML = html
}

function toggleSantriSearchBox() {
  const box = document.getElementById('santri-search-box')
  const sortBox = document.getElementById('santri-sort-box')
  if (!box) return

  const willShow = box.style.display === 'none' || !box.style.display
  box.style.display = willShow ? 'block' : 'none'
  if (sortBox) sortBox.style.display = 'none'

  if (willShow) {
    const input = document.getElementById('santri-search-input')
    if (input) input.focus()
  }
}

function toggleSantriSortBox() {
  const sortBox = document.getElementById('santri-sort-box')
  const searchBox = document.getElementById('santri-search-box')
  if (!sortBox) return

  const willShow = sortBox.style.display === 'none' || !sortBox.style.display
  sortBox.style.display = willShow ? 'block' : 'none'
  if (searchBox) searchBox.style.display = 'none'
}

function applySantriSortControl() {
  const fieldSelect = document.getElementById('santri-sort-field')
  const directionSelect = document.getElementById('santri-sort-direction')
  const kelasFilter = document.getElementById('santri-filter-kelas')
  const kamarFilter = document.getElementById('santri-filter-kamar')
  const statusFilter = document.getElementById('santri-filter-status')

  if (fieldSelect) santriSortField = fieldSelect.value || 'nama'
  if (directionSelect) santriSortDirection = directionSelect.value || 'asc'
  if (kelasFilter) santriSelectFilters.kelas = kelasFilter.value || ''
  if (kamarFilter) santriSelectFilters.kamar = kamarFilter.value || ''
  if (statusFilter) santriSelectFilters.status = statusFilter.value || ''
  renderSantriSearchTable()
}

function resetSantriSortOrder() {
  santriSortField = 'nama'
  santriSortDirection = 'asc'
  santriSelectFilters.kelas = ''
  santriSelectFilters.kamar = ''
  santriSelectFilters.status = ''

  const fieldSelect = document.getElementById('santri-sort-field')
  const directionSelect = document.getElementById('santri-sort-direction')
  const kelasFilter = document.getElementById('santri-filter-kelas')
  const kamarFilter = document.getElementById('santri-filter-kamar')
  const statusFilter = document.getElementById('santri-filter-status')

  if (fieldSelect) fieldSelect.value = 'nama'
  if (directionSelect) directionSelect.value = 'asc'
  if (kelasFilter) kelasFilter.value = ''
  if (kamarFilter) kamarFilter.value = ''
  if (statusFilter) statusFilter.value = ''

  renderSantriSearchTable()
}

function resetSantriSearchAndSortState() {
  const input = document.getElementById('santri-search-input')
  if (input) input.value = ''
  resetSantriSortOrder()
}

function setupSantriSearchHandlers() {
  const tools = document.getElementById('santri-search-tools')
  const box = document.getElementById('santri-search-box')
  const sortBox = document.getElementById('santri-sort-box')
  const input = document.getElementById('santri-search-input')
  if (!tools || !box || !input) return

  if (!input.dataset.bound) {
    input.addEventListener('input', () => renderSantriSearchTable())
    input.dataset.bound = 'true'
  }

  if (!santriSearchOutsideClickBound) {
    document.addEventListener('click', event => {
      const target = event.target
      if (!(target instanceof Node)) return
      if (tools.contains(target)) return
      box.style.display = 'none'
      if (sortBox) sortBox.style.display = 'none'
    })
    santriSearchOutsideClickBound = true
  }
}

// =============================
// Load Santri
// =============================
async function loadSantri(forceRefresh = false) {

  const container = document.getElementById('list-santri')
  if (!container) return

  if (!forceRefresh && typeof getCachedData === 'function') {
    const cached = getCachedData(SANTRI_CACHE_KEY, SANTRI_CACHE_TTL_MS)
    if (Array.isArray(cached)) {
      window.santriList = cached
      populateSantriSelectFilterOptions(window.santriList)
      renderSantriSearchTable()
      return
    }
  }

  container.innerHTML = 'Loading...'

  const tahunAktif = await getTahunAktif()
  if (!tahunAktif) {
    container.innerHTML = 'Tidak ada Tahun Ajaran aktif'
    return
  }

  const { data, error } = await sb
    .from('santri')
    .select(`
      *,
      kelas (
        id,
        nama_kelas,
        tahun_ajaran_id
      )
    `)
    .eq('kelas.tahun_ajaran_id', tahunAktif.id)
    .order('aktif', { ascending: false })
    .order('nama')

  if (error) {
    console.error(error)
    container.innerHTML = 'Gagal load santri'
    return
  }

  if (!data || data.length === 0) {
    if (typeof setCachedData === 'function') setCachedData(SANTRI_CACHE_KEY, [])
    container.innerHTML = 'Belum ada santri'
    return
  }

  if (typeof setCachedData === 'function') setCachedData(SANTRI_CACHE_KEY, data)
  window.santriList = data
  populateSantriSelectFilterOptions(window.santriList)
  renderSantriSearchTable()
  return

  ensureSantriActionStyle()

  let html = `
    <div style="overflow-x:auto;">
    <table style="width:100%; border-collapse:collapse; margin-top:8px; font-size:13px;">
      <thead>
        <tr style="background:#f3f3f3;">
          <th style="padding:8px; border:1px solid #ddd; text-align:center; min-width:280px;">Nama</th>
          <th style="padding:8px; border:1px solid #ddd; text-align:center; width:120px;">NISN</th>
          <th style="padding:8px; border:1px solid #ddd; text-align:center; width:100px;">Jenis Kelamin</th>
          <th style="padding:8px; border:1px solid #ddd; text-align:center;">Kelas</th>
          <th style="padding:8px; border:1px solid #ddd; text-align:center; width:140px;">Halaqah</th>
          <th style="padding:8px; border:1px solid #ddd; text-align:center; width:120px;">Kamar</th>
          <th style="padding:8px; border:1px solid #ddd; text-align:center; width:90px;">Status</th>
          <th style="padding:8px; border:1px solid #ddd; text-align:center; width:300px;">Aksi</th>
        </tr>
      </thead>
      <tbody>
  `

  html += data.map(s => `
    <tr>
      <td style="padding:8px 8px 8px 14px; border:1px solid #ddd; position:relative; white-space:nowrap; min-width:280px;">
        <span style="position:absolute; left:0; top:0; bottom:0; width:4px; background:${s.aktif ? '#16a34a' : '#dc2626'};"></span>
        <span>${s.nama ?? '-'}</span>
      </td>
      <td style="padding:8px; border:1px solid #ddd;">${s.nisn ?? '-'}</td>
      <td style="padding:8px; border:1px solid #ddd;">${s.jenis_kelamin ?? '-'}</td>
      <td style="padding:8px; border:1px solid #ddd;">${s.kelas?.nama_kelas ?? '-'}</td>
      <td style="padding:8px; border:1px solid #ddd;">${s.halaqah ?? '-'}</td>
      <td style="padding:8px; border:1px solid #ddd;">${s.kamar ?? '-'}</td>
      <td style="padding:8px; border:1px solid #ddd;">${s.aktif ? 'Aktif' : 'Tidak Aktif'}</td>
      <td style="padding:8px; border:1px solid #ddd; text-align:center; white-space:nowrap;">
        <button class="btn-detail" onclick="openSantriDetail('${s.id}')">Detail</button>
        <button class="btn-edit" onclick="showEditSantriForm('${s.id}')">Edit</button>
        <button class="btn-hapus" onclick="hapusSantri('${s.id}')">Hapus</button>
      </td>
    </tr>
  `).join('')

  html += '</tbody></table></div>'
  container.innerHTML = html
}


// =============================
// INIT
// =============================
function initSantriPage() {
  ensureSantriFieldFocusStyle()
  setupSantriSearchHandlers()
  createAddSantriModal()
  createEditSantriModal()
  resetSantriSearchAndSortState()
  loadSantri()
}
