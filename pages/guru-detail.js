let currentGuruDetailId = null
let currentGuruDetailTab = 'biodata'
let currentGuruDetailData = null

function getGuruIdFromParams(params = {}) {
  if (params && params.guruId) return params.guruId
  return null
}

function backToGuruList() {
  if (typeof loadPage === 'function') loadPage('guru')
}

function setGuruDetailHeaderName(name) {
  const el = document.getElementById('guru-detail-header-name')
  if (!el) return
  const safeName = String(name || '').trim()
  el.textContent = safeName || 'Detail Guru'
}

function setGuruDetailTab(tab) {
  const validTab = tab === 'jadwal' ? 'jadwal' : 'biodata'
  currentGuruDetailTab = validTab

  const buttons = document.querySelectorAll('.guru-detail-tab-btn')
  buttons.forEach(button => {
    button.classList.toggle('active', button.getAttribute('data-guru-detail-tab') === validTab)
  })

  const panes = document.querySelectorAll('.guru-detail-pane')
  panes.forEach(p => p.classList.remove('active'))
  const activePane = document.getElementById(`guru-detail-pane-${validTab}`)
  if (activePane) activePane.classList.add('active')

  if (currentGuruDetailId) {
    localStorage.setItem('admin_last_page', 'guru-detail')
    localStorage.setItem('admin_last_page_params', JSON.stringify({
      guruId: currentGuruDetailId,
      tab: validTab
    }))
  }

  if (validTab === 'jadwal') {
    renderGuruJadwalSection()
    loadGuruJadwal()
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

function getInsetFieldStyle(extra = '') {
  return `width:100%; padding:10px 12px; box-sizing:border-box; border:1px solid #cbd5e1; border-radius:999px; background:#f8fafc; box-shadow:none; outline:none; transition:border-color 0.2s, box-shadow 0.2s; ${extra}`
}

function pickLabelByKeys(item, keys) {
  for (const key of keys) {
    const value = item?.[key]
    if (value !== null && value !== undefined && String(value).trim() !== '') return String(value).trim()
  }
  return ''
}

function getSemesterLabel(semester) {
  const label = pickLabelByKeys(semester, ['nama_semester', 'nama', 'label', 'kode', 'semester'])
  return label || (semester?.id ? `Semester #${semester.id}` : '-')
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

function renderGuruBiodata() {
  const container = document.getElementById('guru-detail-biodata-container')
  if (!container || !currentGuruDetailData) return
  const data = currentGuruDetailData
  container.innerHTML = `
    <div style="margin-bottom:10px; color:#555;"><strong>ID:</strong> ${escapeHtml(data.id)}</div>
    <div style="margin-bottom:10px;">
      <label style="display:block; margin-bottom:4px;">ID Karyawan</label>
      <input type="text" id="guru-detail-id-karyawan" value="${escapeHtml(data.id_karyawan || '')}" style="${getInsetFieldStyle()}">
    </div>
    <div style="margin-bottom:10px;">
      <label style="display:block; margin-bottom:4px;">Nama</label>
      <input type="text" id="guru-detail-nama" value="${escapeHtml(data.nama || '')}" style="${getInsetFieldStyle()}">
    </div>
    <div style="margin-bottom:10px;">
      <label style="display:block; margin-bottom:4px;">Role</label>
      <input type="text" id="guru-detail-role" value="${escapeHtml(data.role || '')}" style="${getInsetFieldStyle()}">
    </div>
    <div style="margin-bottom:10px;">
      <label style="display:block; margin-bottom:4px;">No HP</label>
      <input type="text" id="guru-detail-no-hp" value="${escapeHtml(data.no_hp || '')}" style="${getInsetFieldStyle()}">
    </div>
    <div style="margin-bottom:10px;">
      <label style="display:block; margin-bottom:4px;">Alamat</label>
      <input type="text" id="guru-detail-alamat" value="${escapeHtml(data.alamat || '')}" style="${getInsetFieldStyle()}">
    </div>
    <div style="margin-bottom:10px;">
      <label style="display:flex; gap:8px; align-items:center;">
        <input type="checkbox" id="guru-detail-aktif" ${data.aktif ? 'checked' : ''}>
        Aktif
      </label>
    </div>
    <div style="margin-top:14px;">
      <button class="modal-btn modal-btn-primary" onclick="saveGuruDetail()">Simpan Perubahan</button>
    </div>
  `
}

async function saveGuruDetail() {
  if (!currentGuruDetailId) return
  const id_karyawan = String(document.getElementById('guru-detail-id-karyawan')?.value || '').trim()
  const nama = String(document.getElementById('guru-detail-nama')?.value || '').trim()
  const role = String(document.getElementById('guru-detail-role')?.value || '').trim()
  const no_hp = String(document.getElementById('guru-detail-no-hp')?.value || '').trim()
  const alamat = String(document.getElementById('guru-detail-alamat')?.value || '').trim()
  const aktif = document.getElementById('guru-detail-aktif')?.checked ?? true

  if (!id_karyawan || !nama || !role) {
    alert('ID Karyawan, nama, dan role wajib diisi')
    return
  }

  const { error } = await sb
    .from('karyawan')
    .update({
      id_karyawan,
      nama,
      role,
      no_hp: no_hp || null,
      alamat: alamat || null,
      aktif
    })
    .eq('id', currentGuruDetailId)

  if (error) {
    console.error(error)
    alert(`Gagal menyimpan biodata guru: ${error.message || 'Unknown error'}`)
    return
  }

  alert('Biodata guru berhasil diperbarui')
  await initGuruDetailPage({ guruId: currentGuruDetailId, tab: 'biodata' })
}

function renderGuruJadwalSection() {
  const container = document.getElementById('guru-detail-jadwal-container')
  if (!container) return
  container.innerHTML = '<div id="guru-detail-jadwal-list">Loading...</div>'
}

async function loadGuruJadwal() {
  const listEl = document.getElementById('guru-detail-jadwal-list')
  if (!listEl || !currentGuruDetailId) return

  listEl.innerHTML = 'Loading...'

  const { data: distribusiList, error: distribusiError } = await sb
    .from('distribusi_mapel')
    .select('id, kelas_id, mapel_id, guru_id, semester_id')
    .eq('guru_id', currentGuruDetailId)

  if (distribusiError) {
    console.error(distribusiError)
    listEl.innerHTML = 'Gagal load jadwal.'
    return
  }

  if (!distribusiList || distribusiList.length === 0) {
    listEl.innerHTML = 'Belum ada jadwal pelajaran.'
    return
  }

  const distribusiIds = distribusiList.map(item => item.id)
  const kelasIds = [...new Set(distribusiList.map(item => item.kelas_id).filter(Boolean))]
  const mapelIds = [...new Set(distribusiList.map(item => item.mapel_id).filter(Boolean))]
  const semesterIds = [...new Set(distribusiList.map(item => item.semester_id).filter(Boolean))]

  const [jadwalRes, kelasRes, mapelRes, semesterRes] = await Promise.all([
    sb.from('jadwal_pelajaran').select('id, distribusi_id, hari, jam_mulai, jam_selesai').in('distribusi_id', distribusiIds),
    kelasIds.length ? sb.from('kelas').select('id, nama_kelas').in('id', kelasIds) : Promise.resolve({ data: [] }),
    mapelIds.length ? sb.from('mapel').select('id, nama, kategori').in('id', mapelIds) : Promise.resolve({ data: [] }),
    semesterIds.length ? sb.from('semester').select('id, nama').in('id', semesterIds) : Promise.resolve({ data: [] })
  ])

  if (jadwalRes.error) {
    console.error(jadwalRes.error)
    listEl.innerHTML = 'Gagal load jadwal.'
    return
  }

  const distribusiMap = new Map(distribusiList.map(item => [String(item.id), item]))
  const kelasMap = new Map((kelasRes.data || []).map(item => [String(item.id), item]))
  const mapelMap = new Map((mapelRes.data || []).map(item => [String(item.id), item]))
  const semesterMap = new Map((semesterRes.data || []).map(item => [String(item.id), item]))
  const jadwalRows = (jadwalRes.data || [])
    .slice()
    .sort((a, b) => {
      const hariCmp = String(a.hari || '').localeCompare(String(b.hari || ''))
      if (hariCmp !== 0) return hariCmp
      return String(a.jam_mulai || '').localeCompare(String(b.jam_mulai || ''))
    })

  if (jadwalRows.length === 0) {
    listEl.innerHTML = 'Belum ada jadwal pelajaran.'
    return
  }

  let html = `
    <div style="overflow-x:auto;">
      <table style="width:100%; border-collapse:collapse; font-size:13px;">
        <thead>
          <tr style="background:#f3f3f3;">
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:110px;">Hari</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:130px;">Jam</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center;">Kelas</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center;">Mapel</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:150px;">Semester</th>
          </tr>
        </thead>
        <tbody>
  `

  html += jadwalRows.map(row => {
    const distribusi = distribusiMap.get(String(row.distribusi_id))
    const kelasNama = kelasMap.get(String(distribusi?.kelas_id))?.nama_kelas || '-'
    const mapel = mapelMap.get(String(distribusi?.mapel_id))
    const mapelNama = mapel ? `${mapel.nama}${mapel.kategori ? ` (${mapel.kategori})` : ''}` : '-'
    const semesterNama = getSemesterLabel(semesterMap.get(String(distribusi?.semester_id)))
    return `
      <tr>
        <td style="padding:8px; border:1px solid #ddd;">${getHariLabel(row.hari)}</td>
        <td style="padding:8px; border:1px solid #ddd; text-align:center;">${toTimeLabel(row.jam_mulai)} - ${toTimeLabel(row.jam_selesai)}</td>
        <td style="padding:8px; border:1px solid #ddd;">${escapeHtml(kelasNama)}</td>
        <td style="padding:8px; border:1px solid #ddd;">${escapeHtml(mapelNama)}</td>
        <td style="padding:8px; border:1px solid #ddd;">${escapeHtml(semesterNama)}</td>
      </tr>
    `
  }).join('')

  html += '</tbody></table></div>'
  listEl.innerHTML = html
}

async function initGuruDetailPage(params = {}) {
  const guruId = getGuruIdFromParams(params)
  currentGuruDetailId = guruId
  currentGuruDetailTab = params?.tab === 'jadwal' ? 'jadwal' : 'biodata'

  const biodataEl = document.getElementById('guru-detail-biodata-container')
  const jadwalEl = document.getElementById('guru-detail-jadwal-container')
  if (!biodataEl || !jadwalEl) return

  if (!guruId) {
    setGuruDetailHeaderName('')
    biodataEl.innerHTML = 'ID guru tidak ditemukan.'
    jadwalEl.innerHTML = 'ID guru tidak ditemukan.'
    return
  }

  setGuruDetailHeaderName('')
  biodataEl.innerHTML = 'Loading...'
  jadwalEl.innerHTML = 'Loading...'
  setGuruDetailTab(currentGuruDetailTab)

  const { data, error } = await sb
    .from('karyawan')
    .select('id, id_karyawan, nama, role, no_hp, alamat, aktif')
    .eq('id', guruId)
    .single()

  if (error || !data) {
    console.error(error)
    biodataEl.innerHTML = 'Gagal load data guru.'
    jadwalEl.innerHTML = 'Gagal load data guru.'
    return
  }

  currentGuruDetailData = data
  setGuruDetailHeaderName(data.nama)
  renderGuruBiodata()
  renderGuruJadwalSection()
  setGuruDetailTab(currentGuruDetailTab)
}
