const KETAHFIZAN_TABLE_HALAQAH = 'halaqah'
const KETAHFIZAN_TABLE_HALAQAH_SANTRI = 'halaqah_santri'
const KETAHFIZAN_TABLE_JADWAL = 'jadwal_halaqah'
const KETAHFIZAN_CACHE_KEY = 'ketahfizan:data'
const KETAHFIZAN_CACHE_TTL_MS = 90 * 1000

let ketahfizanState = {
  subtab: 'halaqah',
  periode: '',
  halaqahRows: [],
  halaqahSantriRows: [],
  santriRows: [],
  karyawanRows: [],
  kelasMap: new Map(),
  jadwalRows: []
}

function ktEscapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

function ktAsBool(value) {
  if (value === true || value === 1) return true
  const text = String(value ?? '').trim().toLowerCase()
  return text === 'true' || text === 't' || text === '1' || text === 'yes'
}

function ktMonthToday() {
  return new Date().toISOString().slice(0, 7)
}

function ktPeriodeLabel(periode) {
  const text = String(periode || '').trim()
  if (!/^\d{4}-\d{2}$/.test(text)) return '-'
  const [yearText, monthText] = text.split('-')
  const month = Number(monthText)
  const names = [
    'Januari', 'Februari', 'Maret', 'April', 'Mei', 'Juni',
    'Juli', 'Agustus', 'September', 'Oktober', 'November', 'Desember'
  ]
  return `${names[month - 1] || monthText} ${yearText}`
}

function ktIsMissingTableError(error, tableName) {
  const code = String(error?.code || '').toUpperCase()
  const msg = String(error?.message || '').toLowerCase()
  if (code === '42P01') return true
  if (msg.includes(`'${String(tableName || '').toLowerCase()}'`)) return true
  if (msg.includes(`public.${String(tableName || '').toLowerCase()}`)) return true
  if (msg.includes('relation') && msg.includes(String(tableName || '').toLowerCase())) return true
  if (msg.includes('could not find the table') && msg.includes('schema cache')) return true
  return false
}

function ktMissingTablesMessage() {
  return `Tabel Data Ketahfizan belum ada di Supabase.

Jalankan SQL berikut:

create table if not exists public.${KETAHFIZAN_TABLE_HALAQAH} (
  id uuid primary key default gen_random_uuid(),
  nama text not null unique,
  muhaffiz_id uuid null,
  created_at timestamptz not null default now()
);

create table if not exists public.${KETAHFIZAN_TABLE_HALAQAH_SANTRI} (
  halaqah_id uuid not null references public.${KETAHFIZAN_TABLE_HALAQAH}(id) on delete cascade,
  santri_id uuid not null references public.santri(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (halaqah_id, santri_id)
);

create table if not exists public.${KETAHFIZAN_TABLE_JADWAL} (
  id uuid primary key default gen_random_uuid(),
  nama_sesi text not null,
  jam_mulai time not null,
  jam_selesai time not null,
  urutan integer null,
  created_at timestamptz not null default now()
);`
}

function ktGetRoot() {
  return document.getElementById('kt-root')
}

function ktOpenModal(html) {
  const modal = document.getElementById('kt-modal')
  const body = document.getElementById('kt-modal-body')
  if (!modal || !body) return
  body.innerHTML = html
  modal.style.display = 'flex'
  modal.setAttribute('aria-hidden', 'false')
}

function ktCloseModal() {
  const modal = document.getElementById('kt-modal')
  if (!modal) return
  modal.style.display = 'none'
  modal.setAttribute('aria-hidden', 'true')
}

function ktRenderFrame(contentHtml) {
  const root = ktGetRoot()
  if (!root) return
  root.innerHTML = contentHtml
}

async function ktLoadMasterData(forceReload = false) {
  if (!forceReload && typeof getCachedData === 'function') {
    const cached = getCachedData(KETAHFIZAN_CACHE_KEY, KETAHFIZAN_CACHE_TTL_MS)
    if (cached) {
      ketahfizanState = { ...ketahfizanState, ...cached }
      ketahfizanState.kelasMap = new Map(cached.kelasMap || [])
      return
    }
  }

  const [halaqahRes, halaqahSantriRes, santriRes, karyawanRes, kelasRes, jadwalRes] = await Promise.all([
    sb.from(KETAHFIZAN_TABLE_HALAQAH).select('id, nama, muhaffiz_id, created_at').order('nama'),
    sb.from(KETAHFIZAN_TABLE_HALAQAH_SANTRI).select('halaqah_id, santri_id'),
    sb.from('santri').select('id, nama, kelas_id, aktif').eq('aktif', true).order('nama'),
    sb.from('karyawan').select('id, nama, role, aktif').order('nama'),
    sb.from('kelas').select('id, nama_kelas').order('nama_kelas'),
    sb.from(KETAHFIZAN_TABLE_JADWAL).select('id, nama_sesi, jam_mulai, jam_selesai, urutan').order('urutan', { ascending: true })
  ])

  const firstError = halaqahRes.error || halaqahSantriRes.error || santriRes.error || karyawanRes.error || kelasRes.error || jadwalRes.error
  if (firstError) {
    if (
      ktIsMissingTableError(firstError, KETAHFIZAN_TABLE_HALAQAH) ||
      ktIsMissingTableError(firstError, KETAHFIZAN_TABLE_HALAQAH_SANTRI) ||
      ktIsMissingTableError(firstError, KETAHFIZAN_TABLE_JADWAL)
    ) {
      throw new Error(ktMissingTablesMessage())
    }
    throw firstError
  }

  ketahfizanState.halaqahRows = halaqahRes.data || []
  ketahfizanState.halaqahSantriRows = halaqahSantriRes.data || []
  ketahfizanState.santriRows = santriRes.data || []
  ketahfizanState.karyawanRows = (karyawanRes.data || []).filter(item => ktAsBool(item.aktif))
  ketahfizanState.kelasMap = new Map((kelasRes.data || []).map(item => [String(item.id), item]))
  ketahfizanState.jadwalRows = jadwalRes.data || []

  if (typeof setCachedData === 'function') {
    setCachedData(KETAHFIZAN_CACHE_KEY, {
      halaqahRows: ketahfizanState.halaqahRows,
      halaqahSantriRows: ketahfizanState.halaqahSantriRows,
      santriRows: ketahfizanState.santriRows,
      karyawanRows: ketahfizanState.karyawanRows,
      kelasMap: Array.from(ketahfizanState.kelasMap.entries()),
      jadwalRows: ketahfizanState.jadwalRows
    })
  }
}

function ktInvalidateCache() {
  if (typeof clearCachedData === 'function') clearCachedData(KETAHFIZAN_CACHE_KEY)
}

function ktGetMuhaffizOptions() {
  const list = (ketahfizanState.karyawanRows || [])
    .filter(item => String(item?.nama || '').trim() !== '')
    .filter(item => String(item?.role || '').toLowerCase().includes('muhaffiz'))
  return list
}

function ktSortSantriRows(rows, sortMode) {
  const list = [...(rows || [])]
  if (sortMode === 'kelas') {
    list.sort((a, b) => {
      const kelasA = String(ketahfizanState.kelasMap.get(String(a?.kelas_id || ''))?.nama_kelas || '')
      const kelasB = String(ketahfizanState.kelasMap.get(String(b?.kelas_id || ''))?.nama_kelas || '')
      const byKelas = kelasA.localeCompare(kelasB, 'id')
      if (byKelas !== 0) return byKelas
      return String(a?.nama || '').localeCompare(String(b?.nama || ''), 'id')
    })
    return list
  }
  if (sortMode === 'selected') {
    const selectedSet = new Set(
      [...document.querySelectorAll('[data-kt-santri-id]:checked')]
        .map(el => String(el.getAttribute('data-kt-santri-id') || ''))
        .filter(Boolean)
    )
    list.sort((a, b) => {
      const sa = selectedSet.has(String(a?.id || '')) ? 0 : 1
      const sb = selectedSet.has(String(b?.id || '')) ? 0 : 1
      if (sa !== sb) return sa - sb
      return String(a?.nama || '').localeCompare(String(b?.nama || ''), 'id')
    })
    return list
  }
  list.sort((a, b) => String(a?.nama || '').localeCompare(String(b?.nama || ''), 'id'))
  return list
}

function ktRenderSantriChecklistHtml(rows, selectedSet) {
  return (rows || []).map(item => {
    const checked = selectedSet.has(String(item.id)) ? 'checked' : ''
    const kelasNama = ketahfizanState.kelasMap.get(String(item.kelas_id || ''))?.nama_kelas || '-'
    return `
      <label class="kt-check-item">
        <input type="checkbox" data-kt-santri-id="${ktEscapeHtml(String(item.id))}" ${checked}>
        <span style="margin-left:6px; font-weight:600;">${ktEscapeHtml(item.nama || '-')}</span>
        <span style="display:block; margin-left:22px; font-size:11px; color:#64748b;">${ktEscapeHtml(kelasNama)}</span>
      </label>
    `
  }).join('')
}

function ktRenderSelectedSantriListHtml(rows) {
  return (rows || []).map(item => {
    const kelasNama = ketahfizanState.kelasMap.get(String(item.kelas_id || ''))?.nama_kelas || '-'
    return `
      <div class="kt-check-item" style="background:#f8fafc;">
        <span style="font-weight:600;">${ktEscapeHtml(item.nama || '-')}</span>
        <span style="display:block; font-size:11px; color:#64748b;">${ktEscapeHtml(kelasNama)}</span>
      </div>
    `
  }).join('')
}

function ktRefreshSantriChecklist() {
  const grid = document.getElementById('kt-santri-check-grid')
  if (!grid) return
  const sortMode = 'kelas'
  const selectedSet = new Set(
    [...document.querySelectorAll('[data-kt-santri-id]:checked')]
      .map(el => String(el.getAttribute('data-kt-santri-id') || ''))
      .filter(Boolean)
  )
  const sortedRows = ktSortSantriRows(ketahfizanState.santriRows || [], sortMode)
  grid.innerHTML = ktRenderSantriChecklistHtml(sortedRows, selectedSet) || '<div style="font-size:13px; color:#64748b;">Belum ada santri aktif.</div>'
}

function ktRenderHalaqahTable() {
  const muhaffizMap = new Map((ketahfizanState.karyawanRows || []).map(item => [String(item.id), item]))
  const countMap = new Map()
  ;(ketahfizanState.halaqahSantriRows || []).forEach(item => {
    const hid = String(item.halaqah_id || '')
    countMap.set(hid, (countMap.get(hid) || 0) + 1)
  })

  const rowsHtml = (ketahfizanState.halaqahRows || []).map((item, index) => {
    const muhaffiz = muhaffizMap.get(String(item.muhaffiz_id || ''))
    return `
      <tr>
        <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${index + 1}</td>
        <td style="padding:8px; border:1px solid #e2e8f0;">
          <button type="button" style="border:none; background:transparent; padding:0; margin:0; color:#0f172a; cursor:pointer; font-weight:600;" onclick="openHalaqahDetail('${ktEscapeHtml(String(item.id))}')">${ktEscapeHtml(item.nama || '-')}</button>
        </td>
        <td style="padding:8px; border:1px solid #e2e8f0;">${ktEscapeHtml(muhaffiz?.nama || '-')}</td>
        <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${countMap.get(String(item.id)) || 0}</td>
        <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">
          <button type="button" class="modal-btn modal-btn-primary" onclick="openHalaqahDetail('${ktEscapeHtml(String(item.id))}')">Detail</button>
        </td>
      </tr>
    `
  }).join('')

  return `
    <div class="kt-card">
      <div class="kt-toolbar" style="margin-bottom:10px;">
        <div style="font-size:13px; color:#64748b;">Klik nama halaqah untuk mengatur muhaffiz dan anggota santri.</div>
        <button type="button" class="modal-btn modal-btn-primary" onclick="openCreateHalaqahModal()">Tambah Halaqah</button>
      </div>
      <div style="overflow:auto; border:1px solid #e2e8f0; border-radius:10px;">
        <table style="width:100%; min-width:760px; border-collapse:collapse; font-size:13px;">
          <thead>
            <tr style="background:#f8fafc;">
              <th style="padding:8px; border:1px solid #e2e8f0; width:44px;">No</th>
              <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Nama Halaqah</th>
              <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Muhaffiz</th>
              <th style="padding:8px; border:1px solid #e2e8f0; width:150px;">Jumlah Santri</th>
              <th style="padding:8px; border:1px solid #e2e8f0; width:130px;">Aksi</th>
            </tr>
          </thead>
          <tbody>
            ${rowsHtml || '<tr><td colspan="5" style="padding:12px; border:1px solid #e2e8f0; text-align:center;">Belum ada data halaqah.</td></tr>'}
          </tbody>
        </table>
      </div>
    </div>
  `
}

function ktRenderHafalanTable() {
  const periode = ketahfizanState.periode || ktMonthToday()
  const rows = (ketahfizanState.halaqahRows || [])
  const halaqahSantriMap = new Map()
  ;(ketahfizanState.halaqahSantriRows || []).forEach(item => {
    const hid = String(item.halaqah_id || '')
    if (!halaqahSantriMap.has(hid)) halaqahSantriMap.set(hid, [])
    halaqahSantriMap.get(hid).push(String(item.santri_id || ''))
  })
  const santriMap = new Map((ketahfizanState.santriRows || []).map(item => [String(item.id), item]))

  const sections = rows.map(halaqah => {
    const santriIds = halaqahSantriMap.get(String(halaqah.id)) || []
    const santriList = santriIds.map(id => santriMap.get(id)).filter(Boolean)
    const listHtml = santriList.map((santri, index) => {
      const report = santri.__hafalan || {}
      return `
        <tr>
          <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${index + 1}</td>
          <td style="padding:8px; border:1px solid #e2e8f0;">${ktEscapeHtml(santri.nama || '-')}</td>
          <td style="padding:8px; border:1px solid #e2e8f0;">${ktEscapeHtml(ketahfizanState.kelasMap.get(String(santri.kelas_id || ''))?.nama_kelas || '-')}</td>
          <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${ktEscapeHtml(report.nilai_target_hafalan ?? '-')}</td>
          <td style="padding:8px; border:1px solid #e2e8f0;">${ktEscapeHtml(report.keterangan_target_hafalan || '-')}</td>
          <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${ktEscapeHtml(report.nilai_capaian_hafalan_bulanan ?? '-')}</td>
          <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${ktEscapeHtml(report.nilai_jumlah_hafalan_halaman ?? '-')}</td>
          <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${ktEscapeHtml(report.nilai_jumlah_hafalan_juz ?? '-')}</td>
        </tr>
      `
    }).join('')

    return `
      <div class="kt-card" style="margin-bottom:10px;">
        <div style="font-weight:700; margin-bottom:8px; color:#0f172a;">${ktEscapeHtml(halaqah.nama || '-')}</div>
        <div style="overflow:auto; border:1px solid #e2e8f0; border-radius:10px;">
          <table style="width:100%; min-width:980px; border-collapse:collapse; font-size:13px;">
            <thead>
              <tr style="background:#f8fafc;">
                <th style="padding:8px; border:1px solid #e2e8f0; width:44px;">No</th>
                <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Santri</th>
                <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Kelas</th>
                <th style="padding:8px; border:1px solid #e2e8f0; width:120px;">Target (%)</th>
                <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Keterangan Target</th>
                <th style="padding:8px; border:1px solid #e2e8f0; width:140px;">Capaian Bulanan</th>
                <th style="padding:8px; border:1px solid #e2e8f0; width:130px;">Jumlah (Hal)</th>
                <th style="padding:8px; border:1px solid #e2e8f0; width:120px;">Jumlah (Juz)</th>
              </tr>
            </thead>
            <tbody>
              ${listHtml || '<tr><td colspan="8" style="padding:10px; border:1px solid #e2e8f0; text-align:center;">Belum ada anggota halaqah.</td></tr>'}
            </tbody>
          </table>
        </div>
      </div>
    `
  }).join('')

  return `
    <div class="kt-card">
      <div class="kt-toolbar" style="margin-bottom:10px;">
        <div>
          <div style="font-size:13px; color:#64748b;">Capaian hafalan per santri berdasarkan halaqah.</div>
          <div style="font-size:12px; color:#94a3b8;">Periode: ${ktEscapeHtml(ktPeriodeLabel(periode))}</div>
        </div>
        <input id="kt-hafalan-periode" class="guru-field" type="month" value="${ktEscapeHtml(periode)}" style="max-width:180px;" onchange="reloadKetahfizanHafalan()">
      </div>
      ${sections || '<div class="placeholder-card">Belum ada data halaqah.</div>'}
    </div>
  `
}

function ktRenderJadwalTable() {
  const rowsHtml = (ketahfizanState.jadwalRows || []).map((item, index) => `
    <tr>
      <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${index + 1}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${ktEscapeHtml(item.nama_sesi || '-')}</td>
      <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${ktEscapeHtml(String(item.jam_mulai || '').slice(0, 5))}</td>
      <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${ktEscapeHtml(String(item.jam_selesai || '').slice(0, 5))}</td>
      <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">
        <button type="button" class="modal-btn modal-btn-danger" onclick="deleteJadwalHalaqah('${ktEscapeHtml(String(item.id))}')">Hapus</button>
      </td>
    </tr>
  `).join('')

  return `
    <div class="kt-card">
      <div class="kt-toolbar" style="margin-bottom:10px;">
        <div style="font-size:13px; color:#64748b;">Jadwal ini berlaku untuk semua halaqah.</div>
        <button type="button" class="modal-btn modal-btn-primary" onclick="openCreateJadwalModal()">Tambah Jadwal Halaqah</button>
      </div>
      <div style="overflow:auto; border:1px solid #e2e8f0; border-radius:10px;">
        <table style="width:100%; min-width:700px; border-collapse:collapse; font-size:13px;">
          <thead>
            <tr style="background:#f8fafc;">
              <th style="padding:8px; border:1px solid #e2e8f0; width:44px;">No</th>
              <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Nama Sesi</th>
              <th style="padding:8px; border:1px solid #e2e8f0; width:130px;">Jam Mulai</th>
              <th style="padding:8px; border:1px solid #e2e8f0; width:130px;">Jam Selesai</th>
              <th style="padding:8px; border:1px solid #e2e8f0; width:120px;">Aksi</th>
            </tr>
          </thead>
          <tbody>
            ${rowsHtml || '<tr><td colspan="5" style="padding:12px; border:1px solid #e2e8f0; text-align:center;">Belum ada jadwal halaqah.</td></tr>'}
          </tbody>
        </table>
      </div>
    </div>
  `
}

async function renderKetahfizanPage(forceReload = false) {
  const root = ktGetRoot()
  if (!root) return
  root.innerHTML = '<div class="placeholder-card">Loading data ketahfizan...</div>'

  try {
    await ktLoadMasterData(forceReload)
    if (ketahfizanState.subtab === 'hafalan') {
      await attachHafalanDataByPeriode()
      ktRenderFrame(ktRenderHafalanTable())
      return
    }
    if (ketahfizanState.subtab === 'jadwal-halaqah') {
      ktRenderFrame(ktRenderJadwalTable())
      return
    }
    ktRenderFrame(ktRenderHalaqahTable())
  } catch (error) {
    console.error(error)
    ktRenderFrame(`<div class="kt-card"><div class="placeholder-card">${ktEscapeHtml(error.message || 'Gagal load data ketahfizan')}</div></div>`)
  }
}

async function attachHafalanDataByPeriode() {
  const periode = ketahfizanState.periode || ktMonthToday()
  ketahfizanState.periode = periode
  const santriIds = (ketahfizanState.santriRows || []).map(item => String(item.id))
  if (!santriIds.length) return

  const { data, error } = await sb
    .from('laporan_bulanan_wali')
    .select('santri_id, nilai_target_hafalan, keterangan_target_hafalan, nilai_capaian_hafalan_bulanan, nilai_jumlah_hafalan_halaman, nilai_jumlah_hafalan_juz')
    .eq('periode', periode)
    .in('santri_id', santriIds)

  if (error) {
    console.error(error)
    return
  }
  const reportMap = new Map((data || []).map(item => [String(item.santri_id), item]))
  ketahfizanState.santriRows = (ketahfizanState.santriRows || []).map(item => ({
    ...item,
    __hafalan: reportMap.get(String(item.id)) || null
  }))
}

function switchKetahfizanSubtab(subtab) {
  const valid = ['halaqah', 'hafalan', 'jadwal-halaqah']
  ketahfizanState.subtab = valid.includes(subtab) ? subtab : 'halaqah'
  const page = ketahfizanState.subtab === 'hafalan'
    ? 'ketahfizan-hafalan'
    : ketahfizanState.subtab === 'jadwal-halaqah'
      ? 'ketahfizan-jadwal'
      : 'ketahfizan-halaqah'
  if (typeof setTopbarTitle === 'function') setTopbarTitle(page)
  if (typeof setActiveSidebarTab === 'function') setActiveSidebarTab(page)
  localStorage.setItem('admin_last_page', page)
  localStorage.setItem('admin_last_page_params', JSON.stringify({ subtab: ketahfizanState.subtab }))
  renderKetahfizanPage(false)
}

function openCreateHalaqahModal() {
  ktOpenModal(`
    <div style="display:flex; align-items:center; justify-content:space-between; gap:8px; margin-bottom:10px;">
      <strong style="font-size:15px;">Tambah Halaqah</strong>
      <button type="button" class="modal-btn modal-btn-secondary" onclick="ktCloseModal()">Tutup</button>
    </div>
    <div style="display:grid; gap:8px;">
      <input id="kt-new-halaqah-nama" class="guru-field" type="text" placeholder="Nama halaqah">
      <div style="display:flex; justify-content:flex-end; gap:8px;">
        <button type="button" class="modal-btn modal-btn-primary" onclick="createHalaqah()">Simpan</button>
      </div>
    </div>
  `)
}

async function createHalaqah() {
  const nama = String(document.getElementById('kt-new-halaqah-nama')?.value || '').trim()
  if (!nama) {
    alert('Nama halaqah wajib diisi.')
    return
  }
  const { error } = await sb.from(KETAHFIZAN_TABLE_HALAQAH).insert([{ nama }])
  if (error) {
    console.error(error)
    alert(`Gagal menambah halaqah: ${error.message || 'Unknown error'}`)
    return
  }
  ktInvalidateCache()
  ktCloseModal()
  await renderKetahfizanPage(true)
}

function openHalaqahDetail(halaqahId) {
  const hid = String(halaqahId || '')
  const halaqah = (ketahfizanState.halaqahRows || []).find(item => String(item.id) === hid)
  if (!halaqah) return
  const selectedSantri = new Set(
    (ketahfizanState.halaqahSantriRows || [])
      .filter(item => String(item.halaqah_id || '') === hid)
      .map(item => String(item.santri_id || ''))
  )
  const muhaffizOptions = ktGetMuhaffizOptions()
    .map(item => `<option value="${ktEscapeHtml(String(item.id))}" ${String(item.id) === String(halaqah.muhaffiz_id || '') ? 'selected' : ''}>${ktEscapeHtml(item.nama || '-')}</option>`)
    .join('')

  const sortedRows = ktSortSantriRows(ketahfizanState.santriRows || [], 'kelas')
  const santriRowsHtml = ktRenderSantriChecklistHtml(sortedRows, selectedSantri)
  const selectedSantriRows = ktSortSantriRows(
    (ketahfizanState.santriRows || []).filter(item => selectedSantri.has(String(item.id))),
    'kelas'
  )
  const selectedSantriHtml = ktRenderSelectedSantriListHtml(selectedSantriRows)

  ktOpenModal(`
    <div style="display:flex; align-items:center; justify-content:space-between; gap:8px; margin-bottom:10px;">
      <strong style="font-size:15px;">Detail Halaqah: ${ktEscapeHtml(halaqah.nama || '-')}</strong>
      <button type="button" class="modal-btn modal-btn-secondary" onclick="ktCloseModal()">Tutup</button>
    </div>
    <div style="display:grid; gap:8px; margin-bottom:10px;">
      <label class="guru-label">Muhaffiz</label>
      <select id="kt-detail-muhaffiz-id" class="kt-detail-field">
        <option value="">-- Pilih Muhaffiz --</option>
        ${muhaffizOptions}
      </select>
    </div>
    <div style="font-size:12px; color:#64748b; margin-bottom:6px;">Santri di halaqah ini:</div>
    <div class="kt-check-grid" style="max-height:170px; overflow:auto; border:1px solid #e2e8f0; border-radius:10px; padding:8px; margin-bottom:10px;">
      ${selectedSantriHtml || '<div style="font-size:13px; color:#64748b;">Belum ada santri di halaqah ini.</div>'}
    </div>
    <div style="display:flex; align-items:center; justify-content:space-between; gap:8px; margin-bottom:8px;">
      <div style="font-size:12px; color:#64748b;">Tambah/atur santri halaqah:</div>
      <button type="button" class="modal-btn modal-btn-secondary" onclick="ktRefreshSantriChecklist()">Sort Per Kelas</button>
    </div>
    <div id="kt-santri-panel" class="kt-santri-panel">
      <div id="kt-santri-check-grid" class="kt-check-grid" style="max-height:300px; overflow:auto; border:1px solid #e2e8f0; border-radius:10px; padding:8px;">
        ${santriRowsHtml || '<div style="font-size:13px; color:#64748b;">Belum ada santri aktif.</div>'}
      </div>
    </div>
    <div style="display:flex; justify-content:flex-end; gap:8px; margin-top:10px;">
      <button type="button" class="modal-btn modal-btn-primary" onclick="saveHalaqahDetail('${ktEscapeHtml(hid)}')">Simpan Detail</button>
    </div>
  `)
}

async function saveHalaqahDetail(halaqahId) {
  const hid = String(halaqahId || '')
  if (!hid) return
  const muhaffizIdRaw = String(document.getElementById('kt-detail-muhaffiz-id')?.value || '').trim()
  const selectedSantriIds = [...document.querySelectorAll('[data-kt-santri-id]:checked')]
    .map(el => String(el.getAttribute('data-kt-santri-id') || '').trim())
    .filter(Boolean)

  const { error: updateError } = await sb
    .from(KETAHFIZAN_TABLE_HALAQAH)
    .update({ muhaffiz_id: muhaffizIdRaw || null })
    .eq('id', hid)
  if (updateError) {
    console.error(updateError)
    alert(`Gagal menyimpan muhaffiz halaqah: ${updateError.message || 'Unknown error'}`)
    return
  }

  const { error: deleteError } = await sb
    .from(KETAHFIZAN_TABLE_HALAQAH_SANTRI)
    .delete()
    .eq('halaqah_id', hid)
  if (deleteError) {
    console.error(deleteError)
    alert(`Gagal reset anggota halaqah: ${deleteError.message || 'Unknown error'}`)
    return
  }

  if (selectedSantriIds.length) {
    const payload = selectedSantriIds.map(sid => ({ halaqah_id: hid, santri_id: sid }))
    const { error: insertError } = await sb
      .from(KETAHFIZAN_TABLE_HALAQAH_SANTRI)
      .insert(payload)
    if (insertError) {
      console.error(insertError)
      alert(`Gagal menyimpan anggota halaqah: ${insertError.message || 'Unknown error'}`)
      return
    }
  }

  ktInvalidateCache()
  ktCloseModal()
  await renderKetahfizanPage(true)
}

function openCreateJadwalModal() {
  ktOpenModal(`
    <div style="display:flex; align-items:center; justify-content:space-between; gap:8px; margin-bottom:10px;">
      <strong style="font-size:15px;">Tambah Jadwal Halaqah</strong>
      <button type="button" class="modal-btn modal-btn-secondary" onclick="ktCloseModal()">Tutup</button>
    </div>
    <div style="display:grid; gap:8px;">
      <input id="kt-jadwal-nama" class="guru-field" type="text" placeholder="Nama sesi (mis. Halaqah Pertama)">
      <div style="display:grid; grid-template-columns:repeat(2,minmax(120px,1fr)); gap:8px;">
        <input id="kt-jadwal-mulai" class="guru-field" type="time">
        <input id="kt-jadwal-selesai" class="guru-field" type="time">
      </div>
      <input id="kt-jadwal-urutan" class="guru-field" type="number" min="1" step="1" placeholder="Urutan (opsional)">
      <div style="display:flex; justify-content:flex-end; gap:8px;">
        <button type="button" class="modal-btn modal-btn-primary" onclick="createJadwalHalaqah()">Simpan</button>
      </div>
    </div>
  `)
}

async function createJadwalHalaqah() {
  const nama = String(document.getElementById('kt-jadwal-nama')?.value || '').trim()
  const jamMulai = String(document.getElementById('kt-jadwal-mulai')?.value || '').trim()
  const jamSelesai = String(document.getElementById('kt-jadwal-selesai')?.value || '').trim()
  const urutanRaw = String(document.getElementById('kt-jadwal-urutan')?.value || '').trim()
  if (!nama || !jamMulai || !jamSelesai) {
    alert('Nama sesi, jam mulai, dan jam selesai wajib diisi.')
    return
  }
  const urutan = urutanRaw ? Number(urutanRaw) : null
  const { error } = await sb
    .from(KETAHFIZAN_TABLE_JADWAL)
    .insert([{
      nama_sesi: nama,
      jam_mulai: jamMulai,
      jam_selesai: jamSelesai,
      urutan: Number.isFinite(urutan) ? urutan : null
    }])
  if (error) {
    console.error(error)
    alert(`Gagal menambah jadwal halaqah: ${error.message || 'Unknown error'}`)
    return
  }
  ktInvalidateCache()
  ktCloseModal()
  await renderKetahfizanPage(true)
}

async function deleteJadwalHalaqah(jadwalId) {
  const ok = typeof showPopupConfirm === 'function'
    ? await showPopupConfirm('Yakin ingin menghapus jadwal halaqah ini?')
    : confirm('Yakin ingin menghapus jadwal halaqah ini?')
  if (!ok) return
  const { error } = await sb
    .from(KETAHFIZAN_TABLE_JADWAL)
    .delete()
    .eq('id', String(jadwalId || ''))
  if (error) {
    console.error(error)
    alert(`Gagal menghapus jadwal halaqah: ${error.message || 'Unknown error'}`)
    return
  }
  ktInvalidateCache()
  await renderKetahfizanPage(true)
}

async function reloadKetahfizanHafalan() {
  ketahfizanState.periode = String(document.getElementById('kt-hafalan-periode')?.value || ktMonthToday())
  await renderKetahfizanPage(false)
}

function initKetahfizanPage(params = {}) {
  ketahfizanState.periode = ketahfizanState.periode || ktMonthToday()
  const subtab = String(params?.subtab || '').trim()
  const normalized = subtab === 'hafalan' || subtab === 'jadwal-halaqah' ? subtab : 'halaqah'
  ketahfizanState.subtab = normalized
  const modal = document.getElementById('kt-modal')
  if (modal && !modal.dataset.bound) {
    modal.addEventListener('click', event => {
      if (event.target !== modal) return
      ktCloseModal()
    })
    modal.dataset.bound = '1'
  }
  renderKetahfizanPage(false)
}

window.initKetahfizanPage = initKetahfizanPage
window.switchKetahfizanSubtab = switchKetahfizanSubtab
window.openCreateHalaqahModal = openCreateHalaqahModal
window.createHalaqah = createHalaqah
window.openHalaqahDetail = openHalaqahDetail
window.saveHalaqahDetail = saveHalaqahDetail
window.ktCloseModal = ktCloseModal
window.ktRefreshSantriChecklist = ktRefreshSantriChecklist
window.reloadKetahfizanHafalan = reloadKetahfizanHafalan
window.openCreateJadwalModal = openCreateJadwalModal
window.createJadwalHalaqah = createJadwalHalaqah
window.deleteJadwalHalaqah = deleteJadwalHalaqah
