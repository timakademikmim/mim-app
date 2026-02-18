let currentEditKaryawanId = null
let karyawanSearchOutsideClickBound = false
let karyawanSortField = 'nama'
let karyawanSortDirection = 'asc'
let karyawanPageMode = 'all'
const karyawanSelectFilters = {
  status: '',
  role: ''
}
const KARYAWAN_CACHE_KEY = 'karyawan:list:all'
const KARYAWAN_CACHE_TTL_MS = 2 * 60 * 1000

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

function normalizeRoleForStore(rawRole) {
  const unique = [...new Set(parseRoleList(rawRole))]
  return unique.join(',')
}

function formatRoleDisplay(rawRole) {
  const roles = parseRoleList(rawRole)
  return roles.length > 0 ? roles.join(', ') : '-'
}

function filterKaryawanByMode(data) {
  if (!Array.isArray(data)) return []
  if (karyawanPageMode !== 'guru-only') return data
  return data.filter(item => parseRoleList(item?.role).includes('guru'))
}

function getSupabaseErrorMessage(error) {
  if (!error) return 'Unknown error'
  const parts = [error.message, error.details, error.hint].filter(Boolean)
  return parts.length > 0 ? parts.join(' | ') : JSON.stringify(error)
}

function generateClientUuid() {
  if (window.crypto && typeof window.crypto.randomUUID === 'function') {
    return window.crypto.randomUUID()
  }
  return `k-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

async function getNextNumericKaryawanId() {
  const { data, error } = await sb
    .from('karyawan')
    .select('id')
    .order('id', { ascending: false })
    .limit(1)

  if (error) return null
  const lastValue = Number(data?.[0]?.id || 0)
  if (!Number.isFinite(lastValue)) return null
  return lastValue + 1
}

function isIdRequiredError(error) {
  const text = getSupabaseErrorMessage(error).toLowerCase()
  return text.includes('column "id"') && text.includes('null')
}

function isNumericIdTypeError(error) {
  const text = getSupabaseErrorMessage(error).toLowerCase()
  return text.includes('invalid input syntax for type integer')
    || text.includes('invalid input syntax for type bigint')
    || text.includes('type bigint')
    || text.includes('type integer')
}

async function insertKaryawanWithFallback(payload) {
  const firstAttempt = await sb
    .from('karyawan')
    .insert([payload])

  if (!firstAttempt.error) return { error: null }
  if (!isIdRequiredError(firstAttempt.error)) return firstAttempt

  const uuidAttempt = await sb
    .from('karyawan')
    .insert([{ id: generateClientUuid(), ...payload }])

  if (!uuidAttempt.error) return { error: null }
  if (!isNumericIdTypeError(uuidAttempt.error)) return uuidAttempt

  const nextId = await getNextNumericKaryawanId()
  if (nextId === null) return uuidAttempt

  return sb
    .from('karyawan')
    .insert([{ id: nextId, ...payload }])
}

function getInsetFieldStyle(extra = '') {
  return `width:100%; padding:10px 12px; box-sizing:border-box; border:1px solid #cbd5e1; border-radius:999px; background:#f8fafc; box-shadow:none; outline:none; transition:border-color 0.2s, box-shadow 0.2s; ${extra}`
}

function ensureKaryawanFieldFocusStyle() {
  if (document.getElementById('karyawan-field-focus-style')) return

  const style = document.createElement('style')
  style.id = 'karyawan-field-focus-style'
  style.innerHTML = `
    .karyawan-field {
      border: 1px solid #cbd5e1;
      border-radius: 999px;
      padding: 9px 12px;
      box-sizing: border-box;
      background: #f8fafc;
      outline: none;
    }
    .karyawan-field:focus {
      border-color: #16a34a !important;
      box-shadow: 0 0 0 3px rgba(22, 163, 74, 0.25) !important;
    }
  `
  document.head.appendChild(style)
}

function ensureKaryawanActionStyle() {
  if (document.getElementById('karyawan-btn-style')) return

  const style = document.createElement('style')
  style.id = 'karyawan-btn-style'
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
    .btn-detail {
      background: #16a34a;
      color: #fff;
      border: none;
      padding: 7px 16px;
      border-radius: 5px;
      margin-right: 6px;
      cursor: pointer;
      box-shadow: 0 0 8px 0 #16a34a55;
      transition: box-shadow 0.2s, transform 0.1s;
      font-weight: bold;
      outline: none;
    }
    .btn-detail:active {
      box-shadow: 0 0 18px 2px #16a34acc, 0 0 0 2px #fff;
      transform: scale(0.96);
    }
  `
  document.head.appendChild(style)
}

function openGuruDetail(id) {
  if (typeof loadPage !== 'function') return
  loadPage('guru-detail', { guruId: id, tab: 'biodata' })
}

async function isIdKaryawanUsed(idKaryawan, excludeId = null) {
  if (!idKaryawan) return false

  let query = sb
    .from('karyawan')
    .select('id')
    .eq('id_karyawan', idKaryawan)
    .limit(1)

  if (excludeId) {
    query = query.neq('id', excludeId)
  }

  const { data, error } = await query
  if (error) {
    console.error(error)
    alert('Gagal validasi ID Karyawan')
    return true
  }

  return data && data.length > 0
}

function createAddKaryawanModal() {
  let modal = document.getElementById('add-karyawan-modal')
  if (modal) modal.remove()

  modal = document.createElement('div')
  modal.id = 'add-karyawan-modal'
  modal.style.position = 'fixed'
  modal.style.top = '0'
  modal.style.left = '0'
  modal.style.width = '100vw'
  modal.style.height = '100vh'
  modal.style.background = 'rgba(0,0,0,0.3)'
  modal.style.display = 'none'
  modal.style.zIndex = '9999'
  modal.innerHTML = `
    <div style="background:#fff; margin:60px auto; padding:24px; border-radius:8px; width:360px; box-shadow:0 2px 12px #0002; position:relative;">
      <h3>Tambah Karyawan</h3>
      <input class="karyawan-field" type="text" id="modal-add-karyawan-id-karyawan" placeholder="ID Karyawan" style="${getInsetFieldStyle('margin-bottom:8px;')}">
      <input class="karyawan-field" type="password" id="modal-add-karyawan-password" placeholder="Password" style="${getInsetFieldStyle('margin-bottom:8px;')}">
      <input class="karyawan-field" type="text" id="modal-add-karyawan-nama" placeholder="Nama Karyawan" style="${getInsetFieldStyle('margin-bottom:8px;')}">
      <input class="karyawan-field" type="text" id="modal-add-karyawan-role" placeholder="Role (contoh: admin,guru)" style="${getInsetFieldStyle('margin-bottom:8px;')}">
      <input class="karyawan-field" type="text" id="modal-add-karyawan-no-hp" placeholder="No HP" style="${getInsetFieldStyle('margin-bottom:8px;')}">
      <input class="karyawan-field" type="text" id="modal-add-karyawan-alamat" placeholder="Alamat" style="${getInsetFieldStyle('margin-bottom:10px;')}">
      <label style="display:flex; gap:8px; align-items:center; margin-bottom:12px;">
        <input type="checkbox" id="modal-add-karyawan-aktif" checked>
        Aktif
      </label>
      <button class="modal-btn modal-btn-primary" onclick="tambahKaryawan()">Simpan</button>
      <button class="modal-btn modal-btn-secondary" onclick="closeAddKaryawanModal()" style="margin-left:8px;">Batal</button>
      <span onclick="closeAddKaryawanModal()" style="position:absolute;top:8px;right:12px;cursor:pointer;font-size:18px;">&times;</span>
    </div>
  `
  document.body.appendChild(modal)
  return modal
}

function openAddKaryawanModal() {
  createAddKaryawanModal()
  document.getElementById('add-karyawan-modal').style.display = 'block'
}

function closeAddKaryawanModal() {
  const modal = document.getElementById('add-karyawan-modal')
  if (!modal) return
  modal.style.display = 'none'
}

function createEditKaryawanModal() {
  let modal = document.getElementById('edit-karyawan-modal')
  if (modal) modal.remove()

  modal = document.createElement('div')
  modal.id = 'edit-karyawan-modal'
  modal.style.position = 'fixed'
  modal.style.top = '0'
  modal.style.left = '0'
  modal.style.width = '100vw'
  modal.style.height = '100vh'
  modal.style.background = 'rgba(0,0,0,0.3)'
  modal.style.display = 'none'
  modal.style.zIndex = '9999'
  modal.innerHTML = `
    <div style="background:#fff; margin:60px auto; padding:24px; border-radius:8px; width:360px; box-shadow:0 2px 12px #0002; position:relative;">
      <h3>Edit Karyawan</h3>
      <input class="karyawan-field" type="text" id="modal-edit-karyawan-id-karyawan" placeholder="ID Karyawan" style="${getInsetFieldStyle('margin-bottom:8px;')}">
      <input class="karyawan-field" type="password" id="modal-edit-karyawan-password" placeholder="Password baru (opsional)" style="${getInsetFieldStyle('margin-bottom:8px;')}">
      <input class="karyawan-field" type="text" id="modal-edit-karyawan-nama" placeholder="Nama Karyawan" style="${getInsetFieldStyle('margin-bottom:8px;')}">
      <input class="karyawan-field" type="text" id="modal-edit-karyawan-role" placeholder="Role (contoh: admin,guru)" style="${getInsetFieldStyle('margin-bottom:8px;')}">
      <input class="karyawan-field" type="text" id="modal-edit-karyawan-no-hp" placeholder="No HP" style="${getInsetFieldStyle('margin-bottom:8px;')}">
      <input class="karyawan-field" type="text" id="modal-edit-karyawan-alamat" placeholder="Alamat" style="${getInsetFieldStyle('margin-bottom:10px;')}">
      <label style="display:flex; gap:8px; align-items:center; margin-bottom:12px;">
        <input type="checkbox" id="modal-edit-karyawan-aktif">
        Aktif
      </label>
      <button class="modal-btn modal-btn-primary" onclick="modalEditKaryawan()">Simpan</button>
      <button class="modal-btn modal-btn-secondary" onclick="closeEditKaryawanModal()" style="margin-left:8px;">Batal</button>
      <span onclick="closeEditKaryawanModal()" style="position:absolute;top:8px;right:12px;cursor:pointer;font-size:18px;">&times;</span>
    </div>
  `
  document.body.appendChild(modal)
  return modal
}

function closeEditKaryawanModal() {
  const modal = document.getElementById('edit-karyawan-modal')
  if (!modal) return
  modal.style.display = 'none'
  currentEditKaryawanId = null
}

async function tambahKaryawan() {
  const idKaryawan = (document.getElementById('modal-add-karyawan-id-karyawan')?.value || '').trim()
  const password = (document.getElementById('modal-add-karyawan-password')?.value || '').trim()
  const nama = (document.getElementById('modal-add-karyawan-nama')?.value || '').trim()
  const roleInput = (document.getElementById('modal-add-karyawan-role')?.value || '').trim()
  const noHp = (document.getElementById('modal-add-karyawan-no-hp')?.value || '').trim()
  const alamat = (document.getElementById('modal-add-karyawan-alamat')?.value || '').trim()
  const aktif = document.getElementById('modal-add-karyawan-aktif')?.checked ?? true

  const normalizedRole = normalizeRoleForStore(roleInput)

  if (!idKaryawan || !password || !nama || !normalizedRole) {
    alert('ID Karyawan, password, nama, dan role wajib diisi')
    return
  }

  if (karyawanPageMode === 'guru-only' && !parseRoleList(normalizedRole).includes('guru')) {
    alert('Di Data Guru, role wajib mengandung guru')
    return
  }

  if (await isIdKaryawanUsed(idKaryawan)) {
    alert('ID Karyawan sudah terdaftar')
    return
  }

  const payload = {
    id_karyawan: idKaryawan,
    password,
    nama,
    role: normalizedRole,
    no_hp: noHp || null,
    alamat: alamat || null,
    aktif
  }

  const { error } = await insertKaryawanWithFallback(payload)

  if (error) {
    console.error(error)
    if (error.code === '23505') {
      alert('ID Karyawan sudah terdaftar')
      return
    }
    alert(`Gagal tambah karyawan: ${getSupabaseErrorMessage(error)}`)
    return
  }

  closeAddKaryawanModal()
  if (typeof clearCachedData === 'function') clearCachedData(KARYAWAN_CACHE_KEY)
  loadKaryawan(true)
}

async function showEditKaryawanForm(id) {
  createEditKaryawanModal()
  const karyawan = window.karyawanList?.find(k => String(k.id) === String(id))
  if (!karyawan) {
    alert('Data karyawan tidak ditemukan')
    return
  }

  currentEditKaryawanId = id
  document.getElementById('modal-edit-karyawan-id-karyawan').value = karyawan.id_karyawan ?? ''
  document.getElementById('modal-edit-karyawan-password').value = ''
  document.getElementById('modal-edit-karyawan-nama').value = karyawan.nama ?? ''
  document.getElementById('modal-edit-karyawan-role').value = formatRoleDisplay(karyawan.role) === '-' ? '' : formatRoleDisplay(karyawan.role)
  document.getElementById('modal-edit-karyawan-no-hp').value = karyawan.no_hp ?? ''
  document.getElementById('modal-edit-karyawan-alamat').value = karyawan.alamat ?? ''
  document.getElementById('modal-edit-karyawan-aktif').checked = karyawan.aktif ?? true

  document.getElementById('edit-karyawan-modal').style.display = 'block'
}

async function modalEditKaryawan() {
  if (!currentEditKaryawanId) return

  const idKaryawan = (document.getElementById('modal-edit-karyawan-id-karyawan')?.value || '').trim()
  const password = (document.getElementById('modal-edit-karyawan-password')?.value || '').trim()
  const nama = (document.getElementById('modal-edit-karyawan-nama')?.value || '').trim()
  const roleInput = (document.getElementById('modal-edit-karyawan-role')?.value || '').trim()
  const noHp = (document.getElementById('modal-edit-karyawan-no-hp')?.value || '').trim()
  const alamat = (document.getElementById('modal-edit-karyawan-alamat')?.value || '').trim()
  const aktif = document.getElementById('modal-edit-karyawan-aktif')?.checked ?? true

  const normalizedRole = normalizeRoleForStore(roleInput)

  if (!idKaryawan || !nama || !normalizedRole) {
    alert('ID Karyawan, nama, dan role wajib diisi')
    return
  }

  if (karyawanPageMode === 'guru-only' && !parseRoleList(normalizedRole).includes('guru')) {
    alert('Di Data Guru, role wajib mengandung guru')
    return
  }

  if (await isIdKaryawanUsed(idKaryawan, currentEditKaryawanId)) {
    alert('ID Karyawan sudah terdaftar')
    return
  }

  const payload = {
    id_karyawan: idKaryawan,
    nama,
    role: normalizedRole,
    no_hp: noHp || null,
    alamat: alamat || null,
    aktif
  }

  if (password) payload.password = password

  const { error } = await sb
    .from('karyawan')
    .update(payload)
    .eq('id', currentEditKaryawanId)

  if (error) {
    console.error(error)
    if (error.code === '23505') {
      alert('ID Karyawan sudah terdaftar')
      return
    }
    alert(`Gagal edit karyawan: ${getSupabaseErrorMessage(error)}`)
    return
  }

  closeEditKaryawanModal()
  if (typeof clearCachedData === 'function') clearCachedData(KARYAWAN_CACHE_KEY)
  loadKaryawan(true)
}

async function hapusKaryawan(id) {
  const confirmed = typeof showPopupConfirm === 'function'
    ? await showPopupConfirm('Yakin ingin hapus karyawan ini?')
    : confirm('Yakin ingin hapus karyawan ini?')
  if (!confirmed) return

  const { error } = await sb
    .from('karyawan')
    .delete()
    .eq('id', id)

  if (error) {
    console.error(error)
    alert('Gagal hapus karyawan')
    return
  }

  if (typeof clearCachedData === 'function') clearCachedData(KARYAWAN_CACHE_KEY)
  loadKaryawan(true)
}

function getKaryawanSearchKeyword() {
  return (document.getElementById('karyawan-search-input')?.value || '').trim().toLowerCase()
}

function hasActiveKaryawanSelectFilters() {
  return Boolean(karyawanSelectFilters.status || karyawanSelectFilters.role)
}

function populateKaryawanRoleFilterOptions(data) {
  const roleSelect = document.getElementById('karyawan-filter-role')
  if (!roleSelect) return

  const roleSet = new Set()
  ;(data || []).forEach(k => {
    parseRoleList(k?.role).forEach(role => roleSet.add(role))
  })

  roleSelect.innerHTML = '<option value="">Semua Role</option>'
  Array.from(roleSet)
    .sort((a, b) => a.localeCompare(b))
    .forEach(role => {
      const opt = document.createElement('option')
      opt.value = role
      opt.textContent = role
      roleSelect.appendChild(opt)
    })

  roleSelect.value = karyawanSelectFilters.role || ''
}

function applyKaryawanSort(data) {
  const list = [...(data || [])]
  if (list.length === 0) return list

  const getSortValue = (item) => {
    if (karyawanSortField === 'id_karyawan') return String(item?.id_karyawan ?? '')
    if (karyawanSortField === 'role') return formatRoleDisplay(item?.role)
    return String(item?.nama ?? '')
  }

  list.sort((a, b) => {
    const aValue = getSortValue(a)
    const bValue = getSortValue(b)
    const cmp = aValue.localeCompare(bValue, undefined, { sensitivity: 'base' })
    return karyawanSortDirection === 'desc' ? -cmp : cmp
  })

  return list
}

function renderKaryawanSearchTable() {
  const container = document.getElementById('list-karyawan')
  if (!container) return

  const data = Array.isArray(window.karyawanList) ? window.karyawanList : []
  const keyword = getKaryawanSearchKeyword()
  const filteredBySearch = !keyword
    ? data
    : data.filter(k => {
      const nama = String(k?.nama ?? '').toLowerCase()
      const idKaryawan = String(k?.id_karyawan ?? '').toLowerCase()
      const noHp = String(k?.no_hp ?? '').toLowerCase()
      return nama.includes(keyword) || idKaryawan.includes(keyword) || noHp.includes(keyword)
    })

  const filteredBySelect = filteredBySearch.filter(k => {
    const statusValue = k?.aktif ? 'aktif' : 'tidak_aktif'
    const roles = parseRoleList(k?.role)
    const matchStatus = !karyawanSelectFilters.status || statusValue === karyawanSelectFilters.status
    const matchRole = !karyawanSelectFilters.role || roles.includes(karyawanSelectFilters.role)
    return matchStatus && matchRole
  })

  const filteredData = applyKaryawanSort(filteredBySelect)
  if (filteredData.length === 0) {
    container.innerHTML = (keyword || hasActiveKaryawanSelectFilters())
      ? 'Tidak ada data karyawan yang sesuai filter/pencarian.'
      : 'Belum ada karyawan'
    return
  }

  ensureKaryawanActionStyle()

  let html = `
    <div class="table-scroll-area" style="overflow-x:auto;">
      <table style="width:100%; border-collapse:collapse; margin-top:8px; font-size:13px;">
        <thead>
          <tr style="background:#f3f3f3;">
            <th style="padding:8px; border:1px solid #ddd; text-align:center; min-width:220px;">Nama</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:140px;">ID Karyawan</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:130px;">Role</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:150px;">No HP</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; min-width:230px;">Alamat</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:100px;">Status</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:${karyawanPageMode === 'guru-only' ? '280px' : '200px'};">Aksi</th>
          </tr>
        </thead>
        <tbody>
  `

  html += filteredData.map(k => `
    <tr>
      <td style="padding:8px 8px 8px 14px; border:1px solid #ddd; position:relative; white-space:nowrap; min-width:220px;">
        <span style="position:absolute; left:0; top:0; bottom:0; width:4px; background:${k.aktif ? '#16a34a' : '#dc2626'};"></span>
        <span>${k.nama ?? '-'}</span>
      </td>
      <td style="padding:8px; border:1px solid #ddd;">${k.id_karyawan ?? '-'}</td>
      <td style="padding:8px; border:1px solid #ddd;">${formatRoleDisplay(k.role)}</td>
      <td style="padding:8px; border:1px solid #ddd;">${k.no_hp ?? '-'}</td>
      <td style="padding:8px; border:1px solid #ddd;">${k.alamat ?? '-'}</td>
      <td style="padding:8px; border:1px solid #ddd;">${k.aktif ? 'Aktif' : 'Tidak Aktif'}</td>
      <td style="padding:8px; border:1px solid #ddd; text-align:center; white-space:nowrap;">
        ${karyawanPageMode === 'guru-only' ? `<button class="btn-detail" onclick="openGuruDetail('${k.id}')">Detail</button>` : ''}
        <button class="btn-edit" onclick="showEditKaryawanForm('${k.id}')">Edit</button>
        <button class="btn-hapus" onclick="hapusKaryawan('${k.id}')">Hapus</button>
      </td>
    </tr>
  `).join('')

  html += '</tbody></table></div>'
  container.innerHTML = html
}

function toggleKaryawanSearchBox() {
  const box = document.getElementById('karyawan-search-box')
  const sortBox = document.getElementById('karyawan-sort-box')
  if (!box) return

  const willShow = box.style.display === 'none' || !box.style.display
  box.style.display = willShow ? 'block' : 'none'
  if (sortBox) sortBox.style.display = 'none'
  if (willShow) {
    const input = document.getElementById('karyawan-search-input')
    if (input) input.focus()
  }
}

function toggleKaryawanSortBox() {
  const sortBox = document.getElementById('karyawan-sort-box')
  const searchBox = document.getElementById('karyawan-search-box')
  if (!sortBox) return

  const willShow = sortBox.style.display === 'none' || !sortBox.style.display
  sortBox.style.display = willShow ? 'block' : 'none'
  if (searchBox) searchBox.style.display = 'none'
}

function applyKaryawanSortControl() {
  const fieldSelect = document.getElementById('karyawan-sort-field')
  const directionSelect = document.getElementById('karyawan-sort-direction')
  const statusFilter = document.getElementById('karyawan-filter-status')
  const roleFilter = document.getElementById('karyawan-filter-role')

  if (fieldSelect) karyawanSortField = fieldSelect.value || 'nama'
  if (directionSelect) karyawanSortDirection = directionSelect.value || 'asc'
  if (statusFilter) karyawanSelectFilters.status = statusFilter.value || ''
  if (roleFilter) karyawanSelectFilters.role = roleFilter.value || ''

  renderKaryawanSearchTable()
}

function resetKaryawanSortOrder() {
  karyawanSortField = 'nama'
  karyawanSortDirection = 'asc'
  karyawanSelectFilters.status = ''
  karyawanSelectFilters.role = ''

  const fieldSelect = document.getElementById('karyawan-sort-field')
  const directionSelect = document.getElementById('karyawan-sort-direction')
  const statusFilter = document.getElementById('karyawan-filter-status')
  const roleFilter = document.getElementById('karyawan-filter-role')

  if (fieldSelect) fieldSelect.value = 'nama'
  if (directionSelect) directionSelect.value = 'asc'
  if (statusFilter) statusFilter.value = ''
  if (roleFilter) roleFilter.value = ''

  renderKaryawanSearchTable()
}

function resetKaryawanSearchAndSortState() {
  const input = document.getElementById('karyawan-search-input')
  if (input) input.value = ''
  resetKaryawanSortOrder()
}

function setupKaryawanSearchHandlers() {
  const tools = document.getElementById('karyawan-search-tools')
  const box = document.getElementById('karyawan-search-box')
  const sortBox = document.getElementById('karyawan-sort-box')
  const input = document.getElementById('karyawan-search-input')
  if (!tools || !box || !input) return

  if (!input.dataset.bound) {
    input.addEventListener('input', () => renderKaryawanSearchTable())
    input.dataset.bound = 'true'
  }

  if (!karyawanSearchOutsideClickBound) {
    document.addEventListener('click', event => {
      const target = event.target
      if (!(target instanceof Node)) return
      if (tools.contains(target)) return
      box.style.display = 'none'
      if (sortBox) sortBox.style.display = 'none'
    })
    karyawanSearchOutsideClickBound = true
  }
}

async function loadKaryawan(forceRefresh = false) {
  const container = document.getElementById('list-karyawan')
  if (!container) return

  if (!forceRefresh && typeof getCachedData === 'function') {
    const cached = getCachedData(KARYAWAN_CACHE_KEY, KARYAWAN_CACHE_TTL_MS)
    if (Array.isArray(cached)) {
      window.karyawanList = filterKaryawanByMode(cached)
      populateKaryawanRoleFilterOptions(window.karyawanList)
      renderKaryawanSearchTable()
      return
    }
  }

  container.innerHTML = 'Loading...'

  const { data, error } = await sb
    .from('karyawan')
    .select('id, id_karyawan, password, nama, role, no_hp, alamat, aktif')
    .order('aktif', { ascending: false })
    .order('nama')

  if (error) {
    console.error(error)
    container.innerHTML = 'Gagal load karyawan'
    return
  }

  if (!data || data.length === 0) {
    window.karyawanList = []
    if (typeof setCachedData === 'function') setCachedData(KARYAWAN_CACHE_KEY, [])
    container.innerHTML = 'Belum ada karyawan'
    return
  }

  if (typeof setCachedData === 'function') setCachedData(KARYAWAN_CACHE_KEY, data)
  window.karyawanList = filterKaryawanByMode(data)
  populateKaryawanRoleFilterOptions(window.karyawanList)
  renderKaryawanSearchTable()
}

function initKaryawanPage(params = {}) {
  karyawanPageMode = params?.mode === 'guru-only' ? 'guru-only' : 'all'
  ensureKaryawanFieldFocusStyle()
  ensureKaryawanActionStyle()
  createAddKaryawanModal()
  createEditKaryawanModal()
  setupKaryawanSearchHandlers()
  resetKaryawanSearchAndSortState()
  loadKaryawan()
}
