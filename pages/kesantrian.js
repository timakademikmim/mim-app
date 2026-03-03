const KESANTRIAN_TABLE_KAMAR = 'kamar'
const KESANTRIAN_TABLE_KAMAR_SANTRI = 'kamar_santri'
const KESANTRIAN_CACHE_KEY = 'kesantrian:data'
const KESANTRIAN_CACHE_TTL_MS = 90 * 1000

let kesantrianState = {
  subtab: 'kamar',
  kamarRows: [],
  kamarSantriRows: [],
  santriRows: [],
  karyawanRows: [],
  kelasMap: new Map()
}
let kesantrianDetailState = {
  kamarId: '',
  selectedSet: new Set(),
  blockedSet: new Set(),
  search: ''
}

function ksEscapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

function ksAsBool(value) {
  if (value === true || value === 1) return true
  const text = String(value ?? '').trim().toLowerCase()
  return text === 'true' || text === 't' || text === '1' || text === 'yes'
}

function ksIsMissingTableError(error, tableName) {
  const code = String(error?.code || '').toUpperCase()
  const msg = String(error?.message || '').toLowerCase()
  if (code === '42P01') return true
  if (msg.includes(`'${String(tableName || '').toLowerCase()}'`)) return true
  if (msg.includes(`public.${String(tableName || '').toLowerCase()}`)) return true
  if (msg.includes('relation') && msg.includes(String(tableName || '').toLowerCase())) return true
  if (msg.includes('could not find the table') && msg.includes('schema cache')) return true
  return false
}

function ksMissingTablesMessage() {
  return `Tabel Data Kesantrian belum ada di Supabase.

Jalankan SQL berikut:

create table if not exists public.${KESANTRIAN_TABLE_KAMAR} (
  id uuid primary key default gen_random_uuid(),
  nama text not null unique,
  musyrif_id uuid null,
  created_at timestamptz not null default now()
);

create table if not exists public.${KESANTRIAN_TABLE_KAMAR_SANTRI} (
  kamar_id uuid not null references public.${KESANTRIAN_TABLE_KAMAR}(id) on delete cascade,
  santri_id uuid not null references public.santri(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (kamar_id, santri_id)
);`
}

function ksGetRoot() {
  return document.getElementById('ks-root')
}

function ksOpenModal(html) {
  const modal = document.getElementById('ks-modal')
  const body = document.getElementById('ks-modal-body')
  if (!modal || !body) return
  body.innerHTML = html
  modal.style.display = 'flex'
  modal.setAttribute('aria-hidden', 'false')
}

function ksCloseModal() {
  const modal = document.getElementById('ks-modal')
  if (!modal) return
  modal.style.display = 'none'
  modal.setAttribute('aria-hidden', 'true')
}

function ksRenderFrame(contentHtml) {
  const root = ksGetRoot()
  if (!root) return
  root.innerHTML = contentHtml
}

function ksInvalidateCache() {
  if (typeof clearCachedData === 'function') clearCachedData(KESANTRIAN_CACHE_KEY)
}

async function ksLoadMasterData(forceReload = false) {
  if (!forceReload && typeof getCachedData === 'function') {
    const cached = getCachedData(KESANTRIAN_CACHE_KEY, KESANTRIAN_CACHE_TTL_MS)
    if (cached) {
      kesantrianState = { ...kesantrianState, ...cached }
      kesantrianState.kelasMap = new Map(cached.kelasMap || [])
      return
    }
  }

  const [kamarRes, kamarSantriRes, santriRes, karyawanRes, kelasRes] = await Promise.all([
    sb.from(KESANTRIAN_TABLE_KAMAR).select('id, nama, musyrif_id').order('nama'),
    sb.from(KESANTRIAN_TABLE_KAMAR_SANTRI).select('kamar_id, santri_id'),
    sb.from('santri').select('id, nama, kelas_id, aktif').eq('aktif', true).order('nama'),
    sb.from('karyawan').select('id, nama, role, aktif').order('nama'),
    sb.from('kelas').select('id, nama_kelas').order('nama_kelas')
  ])

  const firstError = kamarRes.error || kamarSantriRes.error || santriRes.error || karyawanRes.error || kelasRes.error
  if (firstError) {
    if (
      ksIsMissingTableError(firstError, KESANTRIAN_TABLE_KAMAR) ||
      ksIsMissingTableError(firstError, KESANTRIAN_TABLE_KAMAR_SANTRI)
    ) {
      throw new Error(ksMissingTablesMessage())
    }
    throw firstError
  }

  kesantrianState.kamarRows = kamarRes.data || []
  kesantrianState.kamarSantriRows = kamarSantriRes.data || []
  kesantrianState.santriRows = santriRes.data || []
  kesantrianState.karyawanRows = (karyawanRes.data || []).filter(item => ksAsBool(item.aktif))
  kesantrianState.kelasMap = new Map((kelasRes.data || []).map(item => [String(item.id), item]))

  if (typeof setCachedData === 'function') {
    setCachedData(KESANTRIAN_CACHE_KEY, {
      kamarRows: kesantrianState.kamarRows,
      kamarSantriRows: kesantrianState.kamarSantriRows,
      santriRows: kesantrianState.santriRows,
      karyawanRows: kesantrianState.karyawanRows,
      kelasMap: Array.from(kesantrianState.kelasMap.entries())
    })
  }
}

function ksGetMusyrifOptions() {
  return (kesantrianState.karyawanRows || [])
    .filter(item => String(item?.nama || '').trim() !== '')
    .filter(item => String(item?.role || '').toLowerCase().includes('musyrif'))
}

function ksSortSantriRows(rows) {
  const list = [...(rows || [])]
  list.sort((a, b) => {
    const kelasA = String(kesantrianState.kelasMap.get(String(a?.kelas_id || ''))?.nama_kelas || '')
    const kelasB = String(kesantrianState.kelasMap.get(String(b?.kelas_id || ''))?.nama_kelas || '')
    const byKelas = kelasA.localeCompare(kelasB, 'id')
    if (byKelas !== 0) return byKelas
    return String(a?.nama || '').localeCompare(String(b?.nama || ''), 'id')
  })
  return list
}

function ksRefreshSantriChecklist() {
  ksRenderKamarSantriPanels()
}

function ksRenderSantriChecklistHtml(rows, selectedSet) {
  return (rows || []).map(item => {
    const sid = String(item.id || '')
    const checked = selectedSet.has(sid) ? 'checked' : ''
    const kelasNama = kesantrianState.kelasMap.get(String(item.kelas_id || ''))?.nama_kelas || '-'
    return `
      <label class="ks-check-item">
        <input type="checkbox" data-ks-santri-id="${ksEscapeHtml(sid)}" ${checked} onchange="toggleKamarSantri('${ksEscapeHtml(sid)}', this.checked)">
        <span style="margin-left:6px; font-weight:600;">${ksEscapeHtml(item.nama || '-')}</span>
        <span style="display:block; margin-left:22px; font-size:11px; color:#64748b;">${ksEscapeHtml(kelasNama)}</span>
      </label>
    `
  }).join('')
}

function ksRenderSelectedSantriListHtml(rows) {
  return (rows || []).map(item => {
    const sid = String(item.id || '')
    const kelasNama = kesantrianState.kelasMap.get(String(item.kelas_id || ''))?.nama_kelas || '-'
    return `
      <div class="ks-check-item" style="background:#f8fafc; display:grid; grid-template-columns:1fr auto; gap:8px; align-items:flex-start;">
        <span>
          <span style="font-weight:600;">${ksEscapeHtml(item.nama || '-')}</span>
          <span style="display:block; font-size:11px; color:#64748b;">${ksEscapeHtml(kelasNama)}</span>
        </span>
        <button type="button" class="modal-btn modal-btn-danger" style="padding:2px 8px; min-width:auto; line-height:1; border-radius:999px;" onclick="removeKamarSantri('${ksEscapeHtml(sid)}')" title="Keluarkan dari kamar ini">x</button>
      </div>
    `
  }).join('')
}

function ksRenderKamarSantriPanels() {
  const checkGrid = document.getElementById('ks-santri-check-grid')
  const selectedGrid = document.getElementById('ks-selected-santri-grid')
  if (!checkGrid || !selectedGrid) return
  const selectedSet = kesantrianDetailState.selectedSet || new Set()
  const blockedSet = kesantrianDetailState.blockedSet || new Set()
  const search = String(kesantrianDetailState.search || '').trim().toLowerCase()

  const selectedRows = ksSortSantriRows(
    (kesantrianState.santriRows || []).filter(item => selectedSet.has(String(item.id || '')))
  )
  selectedGrid.innerHTML = ksRenderSelectedSantriListHtml(selectedRows) || '<div style="font-size:12px; color:#64748b;">Belum ada santri di kamar ini.</div>'

  const availableRows = ksSortSantriRows(
    (kesantrianState.santriRows || []).filter(item => {
      const sid = String(item.id || '')
      if (!sid) return false
      if (selectedSet.has(sid)) return false
      if (blockedSet.has(sid)) return false
      if (!search) return true
      return String(item.nama || '').toLowerCase().includes(search)
    })
  )
  checkGrid.innerHTML = ksRenderSantriChecklistHtml(availableRows, selectedSet) || '<div style="font-size:12px; color:#64748b;">Tidak ada santri tersedia.</div>'
}

function toggleKamarSantri(santriId, checked) {
  const sid = String(santriId || '').trim()
  if (!sid) return
  if (!(kesantrianDetailState.selectedSet instanceof Set)) kesantrianDetailState.selectedSet = new Set()
  if (checked) kesantrianDetailState.selectedSet.add(sid)
  else kesantrianDetailState.selectedSet.delete(sid)
  ksRenderKamarSantriPanels()
}

function removeKamarSantri(santriId) {
  const sid = String(santriId || '').trim()
  if (!sid) return
  if (!(kesantrianDetailState.selectedSet instanceof Set)) kesantrianDetailState.selectedSet = new Set()
  kesantrianDetailState.selectedSet.delete(sid)
  ksRenderKamarSantriPanels()
}

function searchKamarSantri(keyword) {
  kesantrianDetailState.search = String(keyword || '')
  ksRenderKamarSantriPanels()
}

function ksRenderKamarTable() {
  const musyrifMap = new Map((kesantrianState.karyawanRows || []).map(item => [String(item.id), item]))
  const countMap = new Map()
  ;(kesantrianState.kamarSantriRows || []).forEach(item => {
    const kid = String(item.kamar_id || '')
    countMap.set(kid, (countMap.get(kid) || 0) + 1)
  })

  const rowsHtml = (kesantrianState.kamarRows || []).map((item, index) => {
    const musyrif = musyrifMap.get(String(item.musyrif_id || ''))
    return `
      <tr>
        <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${index + 1}</td>
        <td style="padding:8px; border:1px solid #e2e8f0;">
          <button type="button" style="border:none; background:transparent; padding:0; margin:0; color:#0f172a; cursor:pointer; font-weight:600;" onclick="openKamarDetail('${ksEscapeHtml(String(item.id))}')">${ksEscapeHtml(item.nama || '-')}</button>
        </td>
        <td style="padding:8px; border:1px solid #e2e8f0;">${ksEscapeHtml(musyrif?.nama || '-')}</td>
        <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${countMap.get(String(item.id)) || 0}</td>
        <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">
          <div style="display:flex; gap:6px; justify-content:center; flex-wrap:nowrap; white-space:nowrap;">
            <button type="button" class="modal-btn modal-btn-primary" onclick="openKamarDetail('${ksEscapeHtml(String(item.id))}')">Detail</button>
            <button type="button" class="modal-btn modal-btn-danger" onclick="deleteKamar('${ksEscapeHtml(String(item.id))}')">Hapus</button>
          </div>
        </td>
      </tr>
    `
  }).join('')

  return `
    <div class="ks-card">
      <div class="ks-toolbar" style="margin-bottom:10px;">
        <div style="font-size:13px; color:#64748b;">Klik nama kamar untuk mengatur musyrif dan anggota santri.</div>
        <button type="button" class="modal-btn modal-btn-primary" onclick="openCreateKamarModal()">Tambah Kamar</button>
      </div>
      <div style="overflow:auto; border:1px solid #e2e8f0; border-radius:10px;">
        <table style="width:100%; min-width:760px; border-collapse:collapse; font-size:13px;">
          <thead>
            <tr style="background:#f8fafc;">
              <th style="padding:8px; border:1px solid #e2e8f0; width:44px;">No</th>
              <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Nama Kamar</th>
              <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Musyrif</th>
              <th style="padding:8px; border:1px solid #e2e8f0; width:150px;">Jumlah Santri</th>
              <th style="padding:8px; border:1px solid #e2e8f0; width:200px;">Aksi</th>
            </tr>
          </thead>
          <tbody>
            ${rowsHtml || '<tr><td colspan="5" style="padding:12px; border:1px solid #e2e8f0; text-align:center;">Belum ada data kamar.</td></tr>'}
          </tbody>
        </table>
      </div>
    </div>
  `
}

async function renderKesantrianPage(forceReload = false) {
  const root = ksGetRoot()
  if (!root) return
  root.innerHTML = '<div class="placeholder-card">Loading data kesantrian...</div>'

  try {
    await ksLoadMasterData(forceReload)
    ksRenderFrame(ksRenderKamarTable())
  } catch (error) {
    console.error(error)
    ksRenderFrame(`<div class="ks-card"><div class="placeholder-card">${ksEscapeHtml(error.message || 'Gagal load data kesantrian')}</div></div>`)
  }
}

function openCreateKamarModal() {
  ksOpenModal(`
    <div style="display:flex; align-items:center; justify-content:space-between; gap:8px; margin-bottom:10px;">
      <strong style="font-size:15px;">Tambah Kamar</strong>
      <button type="button" class="modal-btn modal-btn-secondary" onclick="ksCloseModal()">Tutup</button>
    </div>
    <div style="display:grid; gap:8px;">
      <input id="ks-new-kamar-nama" class="guru-field" type="text" placeholder="Nama kamar">
      <div style="display:flex; justify-content:flex-end; gap:8px;">
        <button type="button" class="modal-btn modal-btn-primary" onclick="createKamar()">Simpan</button>
      </div>
    </div>
  `)
}

async function createKamar() {
  const nama = String(document.getElementById('ks-new-kamar-nama')?.value || '').trim()
  if (!nama) {
    alert('Nama kamar wajib diisi.')
    return
  }
  const { error } = await sb.from(KESANTRIAN_TABLE_KAMAR).insert([{ nama }])
  if (error) {
    console.error(error)
    alert(`Gagal menambah kamar: ${error.message || 'Unknown error'}`)
    return
  }
  ksInvalidateCache()
  ksCloseModal()
  await renderKesantrianPage(true)
}

function openKamarDetail(kamarId) {
  const kid = String(kamarId || '')
  const kamar = (kesantrianState.kamarRows || []).find(item => String(item.id) === kid)
  if (!kamar) return

  const selectedSantri = new Set(
    (kesantrianState.kamarSantriRows || [])
      .filter(item => String(item.kamar_id || '') === kid)
      .map(item => String(item.santri_id || ''))
  )
  const blockedSet = new Set(
    (kesantrianState.kamarSantriRows || [])
      .filter(item => String(item.kamar_id || '') !== kid)
      .map(item => String(item.santri_id || ''))
      .filter(Boolean)
  )
  kesantrianDetailState = {
    kamarId: kid,
    selectedSet: new Set(selectedSantri),
    blockedSet,
    search: ''
  }

  const musyrifOptions = ksGetMusyrifOptions()
    .map(item => `<option value="${ksEscapeHtml(String(item.id))}" ${String(item.id) === String(kamar.musyrif_id || '') ? 'selected' : ''}>${ksEscapeHtml(item.nama || '-')}</option>`)
    .join('')
  ksOpenModal(`
    <div style="display:flex; align-items:center; justify-content:space-between; gap:8px; margin-bottom:10px;">
      <strong style="font-size:15px;">Detail Kamar: ${ksEscapeHtml(kamar.nama || '-')}</strong>
      <button type="button" class="modal-btn modal-btn-secondary" onclick="ksCloseModal()">Tutup</button>
    </div>
    <div style="display:grid; gap:8px; margin-bottom:10px;">
      <label class="guru-label">Nama Kamar</label>
      <input id="ks-detail-kamar-nama" class="ks-detail-field" type="text" value="${ksEscapeHtml(kamar.nama || '')}" placeholder="Nama kamar">
      <label class="guru-label">Musyrif Pembina Kamar</label>
      <select id="ks-detail-musyrif-id" class="ks-detail-field">
        <option value="">-- Pilih Musyrif --</option>
        ${musyrifOptions}
      </select>
      <div style="font-size:12px; color:#64748b;">Pilih satu musyrif yang bertanggung jawab untuk kamar ini.</div>
    </div>
    <div style="font-size:12px; color:#64748b; margin-bottom:6px;">Santri di kamar ini:</div>
    <div id="ks-selected-santri-grid" class="ks-check-grid" style="max-height:170px; overflow:auto; border:1px solid #e2e8f0; border-radius:10px; padding:8px; margin-bottom:10px;"></div>
    <div style="display:flex; align-items:center; justify-content:space-between; gap:8px; margin-bottom:8px; flex-wrap:wrap;">
      <div style="font-size:12px; color:#64748b;">Tambah/atur santri kamar:</div>
      <div style="display:flex; align-items:center; gap:8px; flex-wrap:wrap;">
        <input id="ks-santri-search" class="ks-detail-field" type="text" placeholder="Cari nama santri..." style="max-width:220px;" oninput="searchKamarSantri(this.value)">
        <button type="button" class="modal-btn modal-btn-secondary" onclick="ksRefreshSantriChecklist()">Sort Per Kelas</button>
      </div>
    </div>
    <div id="ks-santri-panel" class="ks-santri-panel">
      <div id="ks-santri-check-grid" class="ks-check-grid" style="max-height:300px; overflow:auto; border:1px solid #e2e8f0; border-radius:10px; padding:8px;">
        -
      </div>
    </div>
    <div style="display:flex; justify-content:flex-end; gap:8px; margin-top:10px;">
      <button type="button" class="modal-btn modal-btn-primary" onclick="saveKamarDetail('${ksEscapeHtml(kid)}')">Simpan Detail</button>
    </div>
  `)
  ksRenderKamarSantriPanels()
}

async function saveKamarDetail(kamarId) {
  const kid = String(kamarId || '')
  if (!kid) return
  const namaKamar = String(document.getElementById('ks-detail-kamar-nama')?.value || '').trim()
  const musyrifIdRaw = String(document.getElementById('ks-detail-musyrif-id')?.value || '').trim()
  if (!namaKamar) {
    alert('Nama kamar wajib diisi.')
    return
  }
  const selectedSantriIds = [...(kesantrianDetailState.selectedSet || new Set())]
    .map(item => String(item || '').trim())
    .filter(Boolean)

  const { error: updateError } = await sb
    .from(KESANTRIAN_TABLE_KAMAR)
    .update({ nama: namaKamar, musyrif_id: musyrifIdRaw || null })
    .eq('id', kid)
  if (updateError) {
    console.error(updateError)
    alert(`Gagal menyimpan detail kamar: ${updateError.message || 'Unknown error'}`)
    return
  }

  const { error: deleteError } = await sb
    .from(KESANTRIAN_TABLE_KAMAR_SANTRI)
    .delete()
    .eq('kamar_id', kid)
  if (deleteError) {
    console.error(deleteError)
    alert(`Gagal reset anggota kamar: ${deleteError.message || 'Unknown error'}`)
    return
  }

  if (selectedSantriIds.length) {
    const payload = selectedSantriIds.map(sid => ({ kamar_id: kid, santri_id: sid }))
    const { error: insertError } = await sb
      .from(KESANTRIAN_TABLE_KAMAR_SANTRI)
      .insert(payload)
    if (insertError) {
      console.error(insertError)
      alert(`Gagal menyimpan anggota kamar: ${insertError.message || 'Unknown error'}`)
      return
    }
  }

  ksInvalidateCache()
  ksCloseModal()
  await renderKesantrianPage(true)
}

async function deleteKamar(kamarId) {
  const kid = String(kamarId || '').trim()
  if (!kid) return
  const ok = typeof showPopupConfirm === 'function'
    ? await showPopupConfirm('Yakin ingin menghapus kamar ini?')
    : confirm('Yakin ingin menghapus kamar ini?')
  if (!ok) return

  const { error } = await sb
    .from(KESANTRIAN_TABLE_KAMAR)
    .delete()
    .eq('id', kid)
  if (error) {
    console.error(error)
    alert(`Gagal menghapus kamar: ${error.message || 'Unknown error'}`)
    return
  }
  ksInvalidateCache()
  await renderKesantrianPage(true)
}

function initKesantrianPage(params = {}) {
  const subtab = String(params?.subtab || '').trim()
  kesantrianState.subtab = subtab === 'kamar' ? 'kamar' : 'kamar'
  const modal = document.getElementById('ks-modal')
  if (modal && !modal.dataset.bound) {
    modal.addEventListener('click', event => {
      if (event.target !== modal) return
      ksCloseModal()
      kesantrianDetailState = {
        kamarId: '',
        selectedSet: new Set(),
        blockedSet: new Set(),
        search: ''
      }
    })
    modal.dataset.bound = '1'
  }
  renderKesantrianPage(false)
}

window.initKesantrianPage = initKesantrianPage
window.openCreateKamarModal = openCreateKamarModal
window.createKamar = createKamar
window.openKamarDetail = openKamarDetail
window.saveKamarDetail = saveKamarDetail
window.deleteKamar = deleteKamar
window.ksRefreshSantriChecklist = ksRefreshSantriChecklist
window.searchKamarSantri = searchKamarSantri
window.toggleKamarSantri = toggleKamarSantri
window.removeKamarSantri = removeKamarSantri
window.ksCloseModal = ksCloseModal
