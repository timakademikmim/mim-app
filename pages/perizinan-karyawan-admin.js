const PERIZINAN_KARYAWAN_TABLE = 'izin_karyawan'
const PERIZINAN_KARYAWAN_STATUS = ['menunggu', 'diterima', 'ditolak']

let perizinanKaryawanAdminState = {
  rows: [],
  statusFilter: ''
}

function normalizePerizinanKaryawanStatus(value) {
  const raw = String(value || '').trim().toLowerCase()
  return PERIZINAN_KARYAWAN_STATUS.includes(raw) ? raw : 'menunggu'
}

function getPerizinanKaryawanStatusLabel(value) {
  const status = normalizePerizinanKaryawanStatus(value)
  if (status === 'diterima') return 'Diterima'
  if (status === 'ditolak') return 'Ditolak'
  return 'Menunggu'
}

function getPerizinanKaryawanMissingTableMessage() {
  return `Tabel '${PERIZINAN_KARYAWAN_TABLE}' belum ada.\n\nJalankan SQL:\ncreate table if not exists public.${PERIZINAN_KARYAWAN_TABLE} (\n  id uuid primary key default gen_random_uuid(),\n  guru_id uuid not null,\n  tanggal_mulai date not null,\n  tanggal_selesai date not null,\n  durasi_hari integer not null,\n  keperluan text not null,\n  status text not null default 'menunggu',\n  catatan_wakasek text null,\n  reviewed_by uuid null,\n  reviewed_at timestamptz null,\n  created_at timestamptz not null default now(),\n  updated_at timestamptz not null default now()\n);\n\ncreate index if not exists izin_karyawan_guru_idx on public.${PERIZINAN_KARYAWAN_TABLE}(guru_id);\ncreate index if not exists izin_karyawan_status_idx on public.${PERIZINAN_KARYAWAN_TABLE}(status);\ncreate index if not exists izin_karyawan_created_idx on public.${PERIZINAN_KARYAWAN_TABLE}(created_at desc);`
}

function isPerizinanKaryawanMissingTableError(error) {
  const code = String(error?.code || '').toUpperCase()
  const msg = String(error?.message || '').toLowerCase()
  return code === '42P01' || (msg.includes(PERIZINAN_KARYAWAN_TABLE) && msg.includes('does not exist'))
}

function escapePerizinanAdminHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

function formatPerizinanAdminDate(value) {
  const text = String(value || '').slice(0, 10)
  if (!text) return '-'
  const date = new Date(`${text}T00:00:00`)
  if (Number.isNaN(date.getTime())) return text
  return date.toLocaleDateString('id-ID', { day: '2-digit', month: 'short', year: 'numeric' })
}

async function loadPerizinanKaryawanAdminData() {
  const [izinRes, karyawanRes] = await Promise.all([
    sb
      .from(PERIZINAN_KARYAWAN_TABLE)
      .select('id, guru_id, tanggal_mulai, tanggal_selesai, durasi_hari, keperluan, status, catatan_wakasek, reviewed_by, reviewed_at, created_at')
      .order('created_at', { ascending: false }),
    sb.from('karyawan').select('id, nama, id_karyawan')
  ])

  if (izinRes.error) throw izinRes.error
  if (karyawanRes.error) throw karyawanRes.error

  const karyawanMap = new Map((karyawanRes.data || []).map(item => [String(item.id || ''), item]))
  return (izinRes.data || []).map(item => {
    const guru = karyawanMap.get(String(item?.guru_id || ''))
    const reviewer = karyawanMap.get(String(item?.reviewed_by || ''))
    return {
      ...item,
      status: normalizePerizinanKaryawanStatus(item?.status),
      guru_nama: String(guru?.nama || '-'),
      guru_kode: String(guru?.id_karyawan || '-'),
      reviewer_nama: String(reviewer?.nama || '-')
    }
  })
}

function renderPerizinanKaryawanAdminRows() {
  const box = document.getElementById('pk-admin-list')
  if (!box) return
  const statusFilter = String(perizinanKaryawanAdminState.statusFilter || '')
  const rows = (perizinanKaryawanAdminState.rows || []).filter(item => !statusFilter || item.status === statusFilter)

  if (!rows.length) {
    box.innerHTML = '<div style="color:#64748b;">Belum ada data perizinan karyawan pada filter ini.</div>'
    return
  }

  const htmlRows = rows.map((item, index) => `
    <tr>
      <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${index + 1}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapePerizinanAdminHtml(item.guru_nama)}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapePerizinanAdminHtml(item.guru_kode)}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapePerizinanAdminHtml(formatPerizinanAdminDate(item.created_at))}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapePerizinanAdminHtml(formatPerizinanAdminDate(item.tanggal_mulai))} s.d. ${escapePerizinanAdminHtml(formatPerizinanAdminDate(item.tanggal_selesai))}</td>
      <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${Number(item.durasi_hari || 0)} hari</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapePerizinanAdminHtml(String(item.keperluan || '-'))}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapePerizinanAdminHtml(getPerizinanKaryawanStatusLabel(item.status))}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapePerizinanAdminHtml(String(item.catatan_wakasek || '-'))}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapePerizinanAdminHtml(String(item.reviewer_nama || '-'))}</td>
    </tr>
  `).join('')

  box.innerHTML = `
    <div style="overflow:auto;">
      <table style="width:100%; min-width:1120px; border-collapse:collapse; font-size:13px;">
        <thead>
          <tr style="background:#f8fafc;">
            <th style="padding:8px; border:1px solid #e2e8f0; width:52px;">No</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Nama Karyawan</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">ID Karyawan</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Tanggal Ajukan</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Rentang Izin</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Durasi</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Keperluan</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Status</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Catatan Wakasek</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Diproses Oleh</th>
          </tr>
        </thead>
        <tbody>${htmlRows}</tbody>
      </table>
    </div>
  `
}

async function loadPerizinanKaryawanAdminPage(forceRefresh = false) {
  const box = document.getElementById('pk-admin-list')
  if (!box) return
  box.innerHTML = 'Loading...'
  try {
    const rows = await loadPerizinanKaryawanAdminData()
    perizinanKaryawanAdminState.rows = rows
    renderPerizinanKaryawanAdminRows()
  } catch (error) {
    if (isPerizinanKaryawanMissingTableError(error)) {
      box.innerHTML = `<div style="white-space:pre-wrap; color:#334155;">${escapePerizinanAdminHtml(getPerizinanKaryawanMissingTableMessage())}</div>`
      return
    }
    console.error(error)
    box.innerHTML = `Gagal load data perizinan: ${escapePerizinanAdminHtml(error?.message || 'Unknown error')}`
  }
}

function applyPerizinanKaryawanAdminFilter() {
  perizinanKaryawanAdminState.statusFilter = String(document.getElementById('pk-admin-status-filter')?.value || '')
  renderPerizinanKaryawanAdminRows()
}

function initPerizinanKaryawanAdminPage() {
  perizinanKaryawanAdminState.statusFilter = ''
  const statusFilter = document.getElementById('pk-admin-status-filter')
  if (statusFilter) statusFilter.value = ''
  loadPerizinanKaryawanAdminPage()
}

window.initPerizinanKaryawanAdminPage = initPerizinanKaryawanAdminPage
window.loadPerizinanKaryawanAdminPage = loadPerizinanKaryawanAdminPage
window.applyPerizinanKaryawanAdminFilter = applyPerizinanKaryawanAdminFilter
