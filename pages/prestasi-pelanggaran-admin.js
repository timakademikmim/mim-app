const SANTRI_PRESTASI_TABLE = 'santri_prestasi'
const SANTRI_PELANGGARAN_TABLE = 'santri_pelanggaran'
const SANTRI_SURAT_BUCKET = 'surat-pemberitahuan'

let prestasiPelanggaranAdminState = {
  tab: 'prestasi',
  santriRows: [],
  kelasMap: new Map(),
  prestasiRows: [],
  pelanggaranRows: [],
  editingPrestasiId: '',
  editingPelanggaranId: ''
}

function escapePrestasiPelanggaranHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

function normalizePrestasiKategori(value) {
  const raw = String(value || '').trim().toLowerCase()
  if (raw === 'ketahfizan') return 'ketahfizan'
  if (raw === 'kemusyrifan') return 'kemusyrifan'
  return 'akademik'
}

function formatPrestasiKategori(value) {
  const v = normalizePrestasiKategori(value)
  if (v === 'ketahfizan') return 'Ketahfizan'
  if (v === 'kemusyrifan') return 'Kemusyrifan'
  return 'Akademik'
}

function formatPrestasiDate(value) {
  const text = String(value || '').slice(0, 10)
  if (!text) return '-'
  const date = new Date(`${text}T00:00:00`)
  if (Number.isNaN(date.getTime())) return text
  return date.toLocaleDateString('id-ID', { day: '2-digit', month: 'short', year: 'numeric' })
}

function normalizeSuratJenis(value) {
  const raw = String(value || '').trim().toUpperCase()
  if (!raw) return ''
  if (raw === 'SANKSI DAN TEGURAN') return 'Sanksi dan Teguran'
  if (['ST1', 'ST2', 'ST3', 'SP1', 'SP2', 'SP3', 'DO'].includes(raw)) return raw
  return ''
}

function getSantriLabelWithKelas(santri) {
  const kelas = prestasiPelanggaranAdminState.kelasMap.get(String(santri?.kelas_id || ''))
  return `${String(santri?.nama || '-')} (${String(kelas?.nama_kelas || '-')})`
}

function isPrestasiPelanggaranMissingTableError(error) {
  const msg = String(error?.message || '').toLowerCase()
  const code = String(error?.code || '').toUpperCase()
  if (code === '42P01') return true
  if (msg.includes('schema cache') && (msg.includes(SANTRI_PRESTASI_TABLE) || msg.includes(SANTRI_PELANGGARAN_TABLE))) return true
  return msg.includes('does not exist') && (msg.includes(SANTRI_PRESTASI_TABLE) || msg.includes(SANTRI_PELANGGARAN_TABLE))
}

function getPrestasiPelanggaranMissingTableMessage() {
  return `Tabel Prestasi/Pelanggaran belum ada.\n\nJalankan SQL:\n\ncreate table if not exists public.${SANTRI_PRESTASI_TABLE} (\n  id uuid primary key default gen_random_uuid(),\n  santri_id uuid not null,\n  waktu date not null,\n  kategori text not null default 'akademik',\n  judul text not null,\n  sertifikat_url text null,\n  created_at timestamptz not null default now(),\n  updated_at timestamptz not null default now()\n);\n\ncreate table if not exists public.${SANTRI_PELANGGARAN_TABLE} (\n  id uuid primary key default gen_random_uuid(),\n  santri_id uuid not null,\n  waktu date not null,\n  kategori text not null default 'akademik',\n  judul text not null,\n  hukuman text null,\n  surat_jenis text null,\n  surat_url text null,\n  created_at timestamptz not null default now(),\n  updated_at timestamptz not null default now()\n);`
}

function setPrestasiPelanggaranTab(tab) {
  const value = tab === 'pelanggaran' ? 'pelanggaran' : 'prestasi'
  prestasiPelanggaranAdminState.tab = value
  const btnPrestasi = document.getElementById('pp-admin-tab-prestasi')
  const btnPelanggaran = document.getElementById('pp-admin-tab-pelanggaran')
  const panelPrestasi = document.getElementById('pp-admin-panel-prestasi')
  const panelPelanggaran = document.getElementById('pp-admin-panel-pelanggaran')
  const listPrestasi = document.getElementById('pp-admin-list-prestasi')
  const listPelanggaran = document.getElementById('pp-admin-list-pelanggaran')

  if (btnPrestasi) btnPrestasi.classList.toggle('modal-btn-primary', value === 'prestasi')
  if (btnPelanggaran) btnPelanggaran.classList.toggle('modal-btn-primary', value === 'pelanggaran')
  if (panelPrestasi) panelPrestasi.style.display = value === 'prestasi' ? 'block' : 'none'
  if (panelPelanggaran) panelPelanggaran.style.display = value === 'pelanggaran' ? 'block' : 'none'
  if (listPrestasi) listPrestasi.style.display = value === 'prestasi' ? 'block' : 'none'
  if (listPelanggaran) listPelanggaran.style.display = value === 'pelanggaran' ? 'block' : 'none'
}

function renderPrestasiPelanggaranSantriOptions() {
  const kelasRows = Array.from(prestasiPelanggaranAdminState.kelasMap.values())
  const kelasOptions = ['<option value="">Semua Kelas</option>', ...kelasRows.map(item => `<option value="${escapePrestasiPelanggaranHtml(String(item.id || ''))}">${escapePrestasiPelanggaranHtml(String(item.nama_kelas || '-'))}</option>`)]
  const prestasiKelas = document.getElementById('pp-prestasi-kelas-filter')
  const pelanggaranKelas = document.getElementById('pp-pelanggaran-kelas-filter')
  if (prestasiKelas) prestasiKelas.innerHTML = kelasOptions.join('')
  if (pelanggaranKelas) pelanggaranKelas.innerHTML = kelasOptions.join('')
  renderPrestasiPelanggaranSantriSearchList('prestasi')
  renderPrestasiPelanggaranSantriSearchList('pelanggaran')
}

function renderPrestasiPelanggaranSantriSearchList(mode) {
  const isPrestasi = mode === 'prestasi'
  const kelasFilterEl = document.getElementById(isPrestasi ? 'pp-prestasi-kelas-filter' : 'pp-pelanggaran-kelas-filter')
  const listEl = document.getElementById(isPrestasi ? 'pp-prestasi-santri-list' : 'pp-pelanggaran-santri-list')
  if (!kelasFilterEl || !listEl) return
  const kelasId = String(kelasFilterEl.value || '').trim()
  const rows = (prestasiPelanggaranAdminState.santriRows || []).filter(item => !kelasId || String(item.kelas_id || '') === kelasId)
  listEl.innerHTML = rows.map(item => `<option value="${escapePrestasiPelanggaranHtml(getSantriLabelWithKelas(item))}"></option>`).join('')
}

function onPrestasiPelanggaranClassFilterChange(mode) {
  renderPrestasiPelanggaranSantriSearchList(mode)
}

function resolveSantriIdFromSearch(mode) {
  const isPrestasi = mode === 'prestasi'
  const searchEl = document.getElementById(isPrestasi ? 'pp-prestasi-santri-search' : 'pp-pelanggaran-santri-search')
  const kelasFilterEl = document.getElementById(isPrestasi ? 'pp-prestasi-kelas-filter' : 'pp-pelanggaran-kelas-filter')
  const text = String(searchEl?.value || '').trim()
  const kelasId = String(kelasFilterEl?.value || '').trim()
  if (!text) return ''
  const rows = (prestasiPelanggaranAdminState.santriRows || []).filter(item => !kelasId || String(item.kelas_id || '') === kelasId)
  const exactLabel = rows.find(item => getSantriLabelWithKelas(item).toLowerCase() === text.toLowerCase())
  if (exactLabel?.id) return String(exactLabel.id)
  const exactName = rows.filter(item => String(item.nama || '').trim().toLowerCase() === text.toLowerCase())
  if (exactName.length === 1) return String(exactName[0].id || '')
  const containsName = rows.filter(item => String(item.nama || '').toLowerCase().includes(text.toLowerCase()))
  if (containsName.length === 1) return String(containsName[0].id || '')
  return ''
}

function openPrestasiPelanggaranDocPopup(url, title) {
  const link = String(url || '').trim()
  if (!link) {
    alert('Dokumen belum tersedia.')
    return
  }
  const overlay = document.getElementById('pp-admin-doc-overlay')
  const titleEl = document.getElementById('pp-admin-doc-title')
  const frame = document.getElementById('pp-admin-doc-frame')
  const download = document.getElementById('pp-admin-doc-download')
  if (!overlay || !titleEl || !frame || !download) return
  titleEl.textContent = String(title || 'Dokumen')
  frame.src = link
  download.href = link
  overlay.style.display = 'block'
}

function closePrestasiPelanggaranDocPopup() {
  const overlay = document.getElementById('pp-admin-doc-overlay')
  const frame = document.getElementById('pp-admin-doc-frame')
  if (!overlay || !frame) return
  frame.src = 'about:blank'
  overlay.style.display = 'none'
}

function resetPrestasiAdminForm() {
  prestasiPelanggaranAdminState.editingPrestasiId = ''
  const fields = [
    ['pp-prestasi-santri-search', ''],
    ['pp-prestasi-waktu', new Date().toISOString().slice(0, 10)],
    ['pp-prestasi-kategori', 'akademik'],
    ['pp-prestasi-judul', ''],
    ['pp-prestasi-sertifikat', '']
  ]
  fields.forEach(([id, val]) => {
    const el = document.getElementById(id)
    if (el) el.value = val
  })
  const kelasFilter = document.getElementById('pp-prestasi-kelas-filter')
  if (kelasFilter) kelasFilter.value = ''
  renderPrestasiPelanggaranSantriSearchList('prestasi')
}

function resetPelanggaranAdminForm() {
  prestasiPelanggaranAdminState.editingPelanggaranId = ''
  const fields = [
    ['pp-pelanggaran-santri-search', ''],
    ['pp-pelanggaran-waktu', new Date().toISOString().slice(0, 10)],
    ['pp-pelanggaran-kategori', 'akademik'],
    ['pp-pelanggaran-judul', ''],
    ['pp-pelanggaran-hukuman', ''],
    ['pp-pelanggaran-surat-jenis', ''],
    ['pp-pelanggaran-surat-url', '']
  ]
  fields.forEach(([id, val]) => {
    const el = document.getElementById(id)
    if (el) el.value = val
  })
  const kelasFilter = document.getElementById('pp-pelanggaran-kelas-filter')
  if (kelasFilter) kelasFilter.value = ''
  renderPrestasiPelanggaranSantriSearchList('pelanggaran')
}

function renderPrestasiAdminList() {
  const box = document.getElementById('pp-admin-list-prestasi')
  if (!box) return
  const santriMap = new Map((prestasiPelanggaranAdminState.santriRows || []).map(item => [String(item.id || ''), item]))
  const rows = prestasiPelanggaranAdminState.prestasiRows || []
  if (!rows.length) {
    box.innerHTML = '<div style="color:#64748b;">Belum ada data prestasi.</div>'
    return
  }
  box.innerHTML = `
    <div style="overflow:auto;">
      <table class="pp-admin-table" style="min-width:980px;">
        <thead>
          <tr style="background:#f8fafc;">
            <th style="padding:8px; border:1px solid #e2e8f0; width:52px;">No</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Santri</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:130px;">Waktu</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:140px;">Kategori</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Prestasi</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:130px;">Sertifikat</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:150px;">Aksi</th>
          </tr>
        </thead>
        <tbody>
          ${rows.map((row, idx) => `
            <tr>
              <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${idx + 1}</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">${escapePrestasiPelanggaranHtml(String(santriMap.get(String(row.santri_id || ''))?.nama || '-'))}</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">${escapePrestasiPelanggaranHtml(formatPrestasiDate(row.waktu))}</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">${escapePrestasiPelanggaranHtml(formatPrestasiKategori(row.kategori))}</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">${escapePrestasiPelanggaranHtml(String(row.judul || '-'))}</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <button type="button" class="modal-btn" onclick="openPrestasiPelanggaranDocPopup('${escapePrestasiPelanggaranHtml(String(row.sertifikat_url || ''))}', 'Sertifikat')">Lihat</button>
              </td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <button type="button" class="modal-btn" onclick="editPrestasiAdminEntry('${escapePrestasiPelanggaranHtml(String(row.id || ''))}')">Edit</button>
                <button type="button" class="modal-btn" onclick="deletePrestasiAdminEntry('${escapePrestasiPelanggaranHtml(String(row.id || ''))}')" style="margin-left:6px;">Hapus</button>
              </td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    </div>
  `
}

function renderPelanggaranAdminList() {
  const box = document.getElementById('pp-admin-list-pelanggaran')
  if (!box) return
  const santriMap = new Map((prestasiPelanggaranAdminState.santriRows || []).map(item => [String(item.id || ''), item]))
  const rows = prestasiPelanggaranAdminState.pelanggaranRows || []
  if (!rows.length) {
    box.innerHTML = '<div style="color:#64748b;">Belum ada data pelanggaran.</div>'
    return
  }
  box.innerHTML = `
    <div style="overflow:auto;">
      <table class="pp-admin-table" style="min-width:1160px;">
        <thead>
          <tr style="background:#f8fafc;">
            <th style="padding:8px; border:1px solid #e2e8f0; width:52px;">No</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Santri</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:130px;">Waktu</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:140px;">Kategori</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Pelanggaran</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Jenis Hukuman</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:90px;">Surat</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:130px;">Pemberitahuan</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:150px;">Aksi</th>
          </tr>
        </thead>
        <tbody>
          ${rows.map((row, idx) => `
            <tr>
              <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${idx + 1}</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">${escapePrestasiPelanggaranHtml(String(santriMap.get(String(row.santri_id || ''))?.nama || '-'))}</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">${escapePrestasiPelanggaranHtml(formatPrestasiDate(row.waktu))}</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">${escapePrestasiPelanggaranHtml(formatPrestasiKategori(row.kategori))}</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">${escapePrestasiPelanggaranHtml(String(row.judul || '-'))}</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">${escapePrestasiPelanggaranHtml(String(row.hukuman || '-'))}</td>
              <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${escapePrestasiPelanggaranHtml(String(row.surat_jenis || '-'))}</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <button type="button" class="modal-btn" onclick="openPrestasiPelanggaranDocPopup('${escapePrestasiPelanggaranHtml(String(row.surat_url || ''))}', 'Surat Pemberitahuan')">Lihat</button>
              </td>
              <td style="padding:8px; border:1px solid #e2e8f0;">
                <button type="button" class="modal-btn" onclick="editPelanggaranAdminEntry('${escapePrestasiPelanggaranHtml(String(row.id || ''))}')">Edit</button>
                <button type="button" class="modal-btn" onclick="deletePelanggaranAdminEntry('${escapePrestasiPelanggaranHtml(String(row.id || ''))}')" style="margin-left:6px;">Hapus</button>
              </td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    </div>
  `
}

async function loadPrestasiPelanggaranAdminData() {
  const [santriRes, kelasRes, prestasiRes, pelanggaranRes] = await Promise.all([
    sb.from('santri').select('id, nama, kelas_id, aktif').eq('aktif', true).order('nama'),
    sb.from('kelas').select('id, nama_kelas').order('nama_kelas'),
    sb.from(SANTRI_PRESTASI_TABLE).select('id, santri_id, waktu, kategori, judul, sertifikat_url, created_at').order('waktu', { ascending: false }).order('created_at', { ascending: false }),
    sb.from(SANTRI_PELANGGARAN_TABLE).select('id, santri_id, waktu, kategori, judul, hukuman, surat_jenis, surat_url, created_at').order('waktu', { ascending: false }).order('created_at', { ascending: false })
  ])
  const firstError = santriRes.error || kelasRes.error || prestasiRes.error || pelanggaranRes.error
  if (firstError) throw firstError
  prestasiPelanggaranAdminState.santriRows = santriRes.data || []
  prestasiPelanggaranAdminState.kelasMap = new Map((kelasRes.data || []).map(item => [String(item.id || ''), item]))
  prestasiPelanggaranAdminState.prestasiRows = prestasiRes.data || []
  prestasiPelanggaranAdminState.pelanggaranRows = pelanggaranRes.data || []
  renderPrestasiPelanggaranSantriOptions()
  renderPrestasiAdminList()
  renderPelanggaranAdminList()
}

async function savePrestasiAdminEntry() {
  const santriId = resolveSantriIdFromSearch('prestasi')
  const waktu = String(document.getElementById('pp-prestasi-waktu')?.value || '').trim()
  const kategori = normalizePrestasiKategori(document.getElementById('pp-prestasi-kategori')?.value)
  const judul = String(document.getElementById('pp-prestasi-judul')?.value || '').trim()
  const sertifikatUrl = String(document.getElementById('pp-prestasi-sertifikat')?.value || '').trim()
  if (!santriId || !waktu || !judul) {
    alert('Nama santri, waktu, dan prestasi wajib diisi. Pastikan nama santri cocok dengan daftar.')
    return
  }
  const payload = { santri_id: santriId, waktu, kategori, judul, sertifikat_url: sertifikatUrl || null }
  let error = null
  if (prestasiPelanggaranAdminState.editingPrestasiId) {
    const res = await sb.from(SANTRI_PRESTASI_TABLE).update(payload).eq('id', prestasiPelanggaranAdminState.editingPrestasiId)
    error = res.error
  } else {
    const res = await sb.from(SANTRI_PRESTASI_TABLE).insert([payload])
    error = res.error
  }
  if (error) {
    console.error(error)
    alert(`Gagal simpan data prestasi: ${error.message || 'Unknown error'}`)
    return
  }
  resetPrestasiAdminForm()
  await loadPrestasiPelanggaranAdminData()
}

function editPrestasiAdminEntry(id) {
  const row = (prestasiPelanggaranAdminState.prestasiRows || []).find(item => String(item.id || '') === String(id || ''))
  if (!row) return
  setPrestasiPelanggaranTab('prestasi')
  prestasiPelanggaranAdminState.editingPrestasiId = String(row.id || '')
  const santri = (prestasiPelanggaranAdminState.santriRows || []).find(item => String(item.id || '') === String(row.santri_id || ''))
  document.getElementById('pp-prestasi-kelas-filter').value = String(santri?.kelas_id || '')
  renderPrestasiPelanggaranSantriSearchList('prestasi')
  document.getElementById('pp-prestasi-santri-search').value = getSantriLabelWithKelas(santri || {})
  document.getElementById('pp-prestasi-waktu').value = String(row.waktu || '').slice(0, 10)
  document.getElementById('pp-prestasi-kategori').value = normalizePrestasiKategori(row.kategori)
  document.getElementById('pp-prestasi-judul').value = String(row.judul || '')
  document.getElementById('pp-prestasi-sertifikat').value = String(row.sertifikat_url || '')
}

async function deletePrestasiAdminEntry(id) {
  const ok = window.showPopupConfirm ? await window.showPopupConfirm('Hapus data prestasi ini?') : window.confirm('Hapus data prestasi ini?')
  if (!ok) return
  const { error } = await sb.from(SANTRI_PRESTASI_TABLE).delete().eq('id', String(id || ''))
  if (error) {
    console.error(error)
    alert(`Gagal menghapus data prestasi: ${error.message || 'Unknown error'}`)
    return
  }
  await loadPrestasiPelanggaranAdminData()
}

async function savePelanggaranAdminEntry() {
  const santriId = resolveSantriIdFromSearch('pelanggaran')
  const waktu = String(document.getElementById('pp-pelanggaran-waktu')?.value || '').trim()
  const kategori = normalizePrestasiKategori(document.getElementById('pp-pelanggaran-kategori')?.value)
  const judul = String(document.getElementById('pp-pelanggaran-judul')?.value || '').trim()
  const hukuman = String(document.getElementById('pp-pelanggaran-hukuman')?.value || '').trim()
  const suratJenis = normalizeSuratJenis(document.getElementById('pp-pelanggaran-surat-jenis')?.value)
  const suratUrl = String(document.getElementById('pp-pelanggaran-surat-url')?.value || '').trim()
  if (!santriId || !waktu || !judul) {
    alert('Nama santri, waktu, dan pelanggaran wajib diisi. Pastikan nama santri cocok dengan daftar.')
    return
  }
  if (String(document.getElementById('pp-pelanggaran-surat-jenis')?.value || '').trim() && !suratJenis) {
    alert('Jenis surat tidak valid.')
    return
  }
  const payload = {
    santri_id: santriId,
    waktu,
    kategori,
    judul,
    hukuman: hukuman || null,
    surat_jenis: suratJenis || null,
    surat_url: suratUrl || null
  }
  let error = null
  if (prestasiPelanggaranAdminState.editingPelanggaranId) {
    const res = await sb.from(SANTRI_PELANGGARAN_TABLE).update(payload).eq('id', prestasiPelanggaranAdminState.editingPelanggaranId)
    error = res.error
  } else {
    const res = await sb.from(SANTRI_PELANGGARAN_TABLE).insert([payload])
    error = res.error
  }
  if (error) {
    console.error(error)
    alert(`Gagal simpan data pelanggaran: ${error.message || 'Unknown error'}`)
    return
  }
  resetPelanggaranAdminForm()
  await loadPrestasiPelanggaranAdminData()
}

function editPelanggaranAdminEntry(id) {
  const row = (prestasiPelanggaranAdminState.pelanggaranRows || []).find(item => String(item.id || '') === String(id || ''))
  if (!row) return
  setPrestasiPelanggaranTab('pelanggaran')
  prestasiPelanggaranAdminState.editingPelanggaranId = String(row.id || '')
  const santri = (prestasiPelanggaranAdminState.santriRows || []).find(item => String(item.id || '') === String(row.santri_id || ''))
  document.getElementById('pp-pelanggaran-kelas-filter').value = String(santri?.kelas_id || '')
  renderPrestasiPelanggaranSantriSearchList('pelanggaran')
  document.getElementById('pp-pelanggaran-santri-search').value = getSantriLabelWithKelas(santri || {})
  document.getElementById('pp-pelanggaran-waktu').value = String(row.waktu || '').slice(0, 10)
  document.getElementById('pp-pelanggaran-kategori').value = normalizePrestasiKategori(row.kategori)
  document.getElementById('pp-pelanggaran-judul').value = String(row.judul || '')
  document.getElementById('pp-pelanggaran-hukuman').value = String(row.hukuman || '')
  document.getElementById('pp-pelanggaran-surat-jenis').value = normalizeSuratJenis(row.surat_jenis)
  document.getElementById('pp-pelanggaran-surat-url').value = String(row.surat_url || '')
}

async function uploadPelanggaranSuratFile(event) {
  const file = event?.target?.files?.[0]
  if (!file) return
  try {
    const ext = String(file.name || '').split('.').pop() || 'bin'
    const path = `surat/${Date.now()}-${Math.random().toString(36).slice(2, 8)}.${ext}`
    const { error: uploadError } = await sb.storage.from(SANTRI_SURAT_BUCKET).upload(path, file, {
      cacheControl: '3600',
      upsert: true
    })
    if (uploadError) {
      throw uploadError
    }
    const { data: pubData } = sb.storage.from(SANTRI_SURAT_BUCKET).getPublicUrl(path)
    const publicUrl = String(pubData?.publicUrl || '').trim()
    if (!publicUrl) {
      alert('Upload berhasil, tapi URL file tidak ditemukan.')
      return
    }
    const urlInput = document.getElementById('pp-pelanggaran-surat-url')
    if (urlInput) urlInput.value = publicUrl
    alert('Upload surat berhasil.')
  } catch (error) {
    console.error(error)
    alert(`Gagal upload surat: ${error?.message || 'Unknown error'}`)
  } finally {
    event.target.value = ''
  }
}

async function deletePelanggaranAdminEntry(id) {
  const ok = window.showPopupConfirm ? await window.showPopupConfirm('Hapus data pelanggaran ini?') : window.confirm('Hapus data pelanggaran ini?')
  if (!ok) return
  const { error } = await sb.from(SANTRI_PELANGGARAN_TABLE).delete().eq('id', String(id || ''))
  if (error) {
    console.error(error)
    alert(`Gagal menghapus data pelanggaran: ${error.message || 'Unknown error'}`)
    return
  }
  await loadPrestasiPelanggaranAdminData()
}

async function initPrestasiPelanggaranAdminPage() {
  const listPrestasi = document.getElementById('pp-admin-list-prestasi')
  const listPelanggaran = document.getElementById('pp-admin-list-pelanggaran')
  if (listPrestasi) listPrestasi.innerHTML = 'Loading...'
  if (listPelanggaran) listPelanggaran.innerHTML = 'Loading...'
  resetPrestasiAdminForm()
  resetPelanggaranAdminForm()
  setPrestasiPelanggaranTab('prestasi')
  try {
    await loadPrestasiPelanggaranAdminData()
  } catch (error) {
    console.error(error)
    if (isPrestasiPelanggaranMissingTableError(error)) {
      const msg = `<div style="white-space:pre-wrap; color:#334155;">${escapePrestasiPelanggaranHtml(getPrestasiPelanggaranMissingTableMessage())}</div>`
      if (listPrestasi) listPrestasi.innerHTML = msg
      if (listPelanggaran) listPelanggaran.innerHTML = msg
      return
    }
    const txt = `<div style="color:#b91c1c;">Gagal load data prestasi/pelanggaran: ${escapePrestasiPelanggaranHtml(error?.message || 'Unknown error')}</div>`
    if (listPrestasi) listPrestasi.innerHTML = txt
    if (listPelanggaran) listPelanggaran.innerHTML = txt
  }
}

window.initPrestasiPelanggaranAdminPage = initPrestasiPelanggaranAdminPage
window.setPrestasiPelanggaranTab = setPrestasiPelanggaranTab
window.onPrestasiPelanggaranClassFilterChange = onPrestasiPelanggaranClassFilterChange
window.savePrestasiAdminEntry = savePrestasiAdminEntry
window.resetPrestasiAdminForm = resetPrestasiAdminForm
window.editPrestasiAdminEntry = editPrestasiAdminEntry
window.deletePrestasiAdminEntry = deletePrestasiAdminEntry
window.savePelanggaranAdminEntry = savePelanggaranAdminEntry
window.resetPelanggaranAdminForm = resetPelanggaranAdminForm
window.editPelanggaranAdminEntry = editPelanggaranAdminEntry
window.deletePelanggaranAdminEntry = deletePelanggaranAdminEntry
window.openPrestasiPelanggaranDocPopup = openPrestasiPelanggaranDocPopup
window.closePrestasiPelanggaranDocPopup = closePrestasiPelanggaranDocPopup
window.uploadPelanggaranSuratFile = uploadPelanggaranSuratFile
