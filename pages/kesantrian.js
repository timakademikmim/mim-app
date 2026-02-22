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
let ksDetailSelectedSantriSet = new Set()

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

function ksGetSelectedSantriSetFromDom() {
  return new Set(
    [...document.querySelectorAll('[data-ks-santri-id]:checked')]
      .map(el => String(el.getAttribute('data-ks-santri-id') || '').trim())
      .filter(Boolean)
  )
}

function ksSyncDetailSelectedSetFromDom() {
  const inputs = [...document.querySelectorAll('[data-ks-santri-id]')]
  inputs.forEach(el => {
    const sid = String(el.getAttribute('data-ks-santri-id') || '').trim()
    if (!sid) return
    ksDetailSelectedSantriSet.delete(sid)
    if (el.checked) ksDetailSelectedSantriSet.add(sid)
  })
}

function ksGetKelasFilterOptions() {
  const map = new Map()
  ;(kesantrianState.santriRows || []).forEach(item => {
    const kelasId = String(item?.kelas_id || '').trim()
    if (!kelasId) return
    const kelasNama = String(kesantrianState.kelasMap.get(kelasId)?.nama_kelas || '').trim()
    if (!kelasNama) return
    map.set(kelasId, kelasNama)
  })
  return [...map.entries()]
    .sort((a, b) => a[1].localeCompare(b[1], 'id'))
    .map(([id, nama]) => ({ id, nama }))
}

function ksBuildSantriChecklistHtml(selectedSet) {
  const sortMode = String(document.getElementById('ks-santri-sort-mode')?.value || 'kelas')
  const kelasFilter = String(document.getElementById('ks-santri-kelas-filter')?.value || '')
  let rows = [...(kesantrianState.santriRows || [])]

  if (kelasFilter) rows = rows.filter(item => String(item?.kelas_id || '') === kelasFilter)
  rows = sortMode === 'nama' ? rows.sort((a, b) => String(a?.nama || '').localeCompare(String(b?.nama || ''), 'id')) : ksSortSantriRows(rows)
  return ksRenderSantriChecklistHtml(rows, selectedSet)
}

function ksRefreshSantriChecklist() {
  const grid = document.getElementById('ks-santri-check-grid')
  if (!grid) return
  ksSyncDetailSelectedSetFromDom()
  grid.innerHTML = ksBuildSantriChecklistHtml(ksDetailSelectedSantriSet) || '<div style="font-size:13px; color:#64748b;">Belum ada santri aktif pada filter ini.</div>'
}

function ksRenderSantriChecklistHtml(rows, selectedSet) {
  return (rows || []).map(item => {
    const checked = selectedSet.has(String(item.id)) ? 'checked' : ''
    const kelasNama = kesantrianState.kelasMap.get(String(item.kelas_id || ''))?.nama_kelas || '-'
    return `
      <label class="ks-check-item">
        <input type="checkbox" data-ks-santri-id="${ksEscapeHtml(String(item.id))}" ${checked}>
        <span style="margin-left:6px; font-weight:600;">${ksEscapeHtml(item.nama || '-')}</span>
        <span style="display:block; margin-left:22px; font-size:11px; color:#64748b;">${ksEscapeHtml(kelasNama)}</span>
      </label>
    `
  }).join('')
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
          <button type="button" class="modal-btn modal-btn-primary" onclick="openKamarDetail('${ksEscapeHtml(String(item.id))}')">Detail</button>
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
              <th style="padding:8px; border:1px solid #e2e8f0; width:130px;">Aksi</th>
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
  ksDetailSelectedSantriSet = new Set(selectedSantri)

  const musyrifOptions = ksGetMusyrifOptions()
    .map(item => `<option value="${ksEscapeHtml(String(item.id))}" ${String(item.id) === String(kamar.musyrif_id || '') ? 'selected' : ''}>${ksEscapeHtml(item.nama || '-')}</option>`)
    .join('')
  const kelasFilterOptions = ksGetKelasFilterOptions()
    .map(item => `<option value="${ksEscapeHtml(item.id)}">${ksEscapeHtml(item.nama)}</option>`)
    .join('')
  const santriRowsHtml = ksRenderSantriChecklistHtml(ksSortSantriRows(kesantrianState.santriRows || []), ksDetailSelectedSantriSet)

  ksOpenModal(`
    <div style="display:flex; align-items:center; justify-content:space-between; gap:8px; margin-bottom:10px;">
      <strong style="font-size:15px;">Detail Kamar: ${ksEscapeHtml(kamar.nama || '-')}</strong>
      <button type="button" class="modal-btn modal-btn-secondary" onclick="ksCloseModal()">Tutup</button>
    </div>
    <div style="display:grid; gap:8px; margin-bottom:10px;">
      <label class="guru-label">Musyrif Pembina Kamar</label>
      <select id="ks-detail-musyrif-id" class="ks-detail-field">
        <option value="">-- Pilih Musyrif --</option>
        ${musyrifOptions}
      </select>
      <div style="font-size:12px; color:#64748b;">Pilih satu musyrif yang bertanggung jawab untuk kamar ini.</div>
    </div>
    <div style="display:flex; align-items:center; justify-content:space-between; gap:8px; margin-bottom:8px; flex-wrap:wrap;">
      <div style="font-size:12px; color:#64748b;">Anggota santri kamar:</div>
      <div style="display:flex; align-items:center; gap:8px; flex-wrap:wrap;">
        <select id="ks-santri-sort-mode" class="ks-detail-field" onchange="ksRefreshSantriChecklist()" style="max-width:190px;">
          <option value="kelas" selected>Urutkan: Kelas</option>
          <option value="nama">Urutkan: Nama</option>
        </select>
        <select id="ks-santri-kelas-filter" class="ks-detail-field" onchange="ksRefreshSantriChecklist()" style="max-width:220px;">
          <option value="">Semua Kelas</option>
          ${kelasFilterOptions}
        </select>
      </div>
    </div>
    <div id="ks-santri-check-grid" class="ks-check-grid" style="max-height:340px; overflow:auto; border:1px solid #e2e8f0; border-radius:10px; padding:8px;">
      ${santriRowsHtml || '<div style="font-size:13px; color:#64748b;">Belum ada santri aktif.</div>'}
    </div>
    <div style="display:flex; justify-content:flex-end; gap:8px; margin-top:10px;">
      <button type="button" class="modal-btn modal-btn-primary" onclick="saveKamarDetail('${ksEscapeHtml(kid)}')">Simpan Detail</button>
    </div>
  `)
}

async function saveKamarDetail(kamarId) {
  const kid = String(kamarId || '')
  if (!kid) return
  const musyrifIdRaw = String(document.getElementById('ks-detail-musyrif-id')?.value || '').trim()
  ksSyncDetailSelectedSetFromDom()
  const selectedSantriIds = [...ksDetailSelectedSantriSet]

  const { error: updateError } = await sb
    .from(KESANTRIAN_TABLE_KAMAR)
    .update({ musyrif_id: musyrifIdRaw || null })
    .eq('id', kid)
  if (updateError) {
    console.error(updateError)
    alert(`Gagal menyimpan musyrif kamar: ${updateError.message || 'Unknown error'}`)
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

function initKesantrianPage(params = {}) {
  const subtab = String(params?.subtab || '').trim()
  kesantrianState.subtab = subtab === 'kamar' ? 'kamar' : 'kamar'
  const modal = document.getElementById('ks-modal')
  if (modal && !modal.dataset.bound) {
    modal.addEventListener('click', event => {
      if (event.target !== modal) return
      ksCloseModal()
      ksDetailSelectedSantriSet = new Set()
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
window.ksRefreshSantriChecklist = ksRefreshSantriChecklist
window.ksCloseModal = ksCloseModal
