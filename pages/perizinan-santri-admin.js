const SANTRI_IZIN_TABLE = 'izin_santri'

function isPerizinanSantriMissingTableError(error) {
  const code = String(error?.code || '').toUpperCase()
  const msg = String(error?.message || '').toLowerCase()
  return code === '42P01' || (msg.includes(SANTRI_IZIN_TABLE) && (msg.includes('does not exist') || msg.includes('schema cache')))
}

function getPerizinanSantriMissingTableMessage() {
  return `Tabel '${SANTRI_IZIN_TABLE}' belum ada.\n\nJalankan SQL:\ncreate table if not exists public.${SANTRI_IZIN_TABLE} (\n  id uuid primary key default gen_random_uuid(),\n  santri_id uuid not null,\n  pengaju_id uuid not null,\n  tujuan_wakasek text not null,\n  keperluan text not null,\n  status text not null default 'menunggu',\n  catatan_wakasek text null,\n  reviewed_by uuid null,\n  reviewed_at timestamptz null,\n  created_at timestamptz not null default now(),\n  updated_at timestamptz not null default now()\n);`
}

function normalizePerizinanSantriTarget(value) {
  const raw = String(value || '').trim().toLowerCase().replaceAll('-', '_')
  if (raw === 'wakasek_kesantrian') return 'wakasek_kesantrian'
  if (raw === 'wakasek_ketahfizan') return 'wakasek_ketahfizan'
  return 'wakasek_kurikulum'
}

function getPerizinanSantriTargetLabel(value) {
  const target = normalizePerizinanSantriTarget(value)
  if (target === 'wakasek_kesantrian') return 'Wakasek Kesantrian'
  if (target === 'wakasek_ketahfizan') return 'Wakasek Ketahfizan'
  return 'Wakasek Kurikulum'
}

function formatPerizinanSantriDate(value) {
  const text = String(value || '').slice(0, 10)
  if (!text) return '-'
  const date = new Date(`${text}T00:00:00`)
  if (Number.isNaN(date.getTime())) return text
  return date.toLocaleDateString('id-ID', { day: '2-digit', month: 'short', year: 'numeric' })
}

function renderPerizinanSantriAdminRows(rows) {
  const box = document.getElementById('perizinan-santri-admin-list')
  if (!box) return
  if (!rows.length) {
    box.innerHTML = '<div style="color:#64748b;">Belum ada perizinan siswa yang disetujui wakasek kurikulum.</div>'
    return
  }

  const htmlRows = rows.map((item, index) => `
    <tr>
      <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${index + 1}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(formatPerizinanSantriDate(item.created_at))}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.santri_nama || '-')}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.kelas_nama || '-')}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.pengaju_nama || '-')}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(getPerizinanSantriTargetLabel(item.tujuan_wakasek))}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(String(item.keperluan || '-'))}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(String(item.catatan_wakasek || '-'))}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.reviewer_nama || '-')}</td>
    </tr>
  `).join('')

  box.innerHTML = `
    <div style="overflow:auto;">
      <table style="width:100%; min-width:1180px; border-collapse:collapse; font-size:13px;">
        <thead>
          <tr style="background:#f8fafc;">
            <th style="padding:8px; border:1px solid #e2e8f0; width:52px;">No</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Tanggal Ajukan</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Nama Santri</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Kelas</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Pengaju (Musyrif)</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Tujuan</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Keperluan</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Catatan Wakasek</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Disetujui Oleh</th>
          </tr>
        </thead>
        <tbody>${htmlRows}</tbody>
      </table>
    </div>
  `
}

async function loadPerizinanSantriAdminPage(forceRefresh = false) {
  const box = document.getElementById('perizinan-santri-admin-list')
  if (box && forceRefresh) box.innerHTML = 'Loading...'
  try {
    const izinRes = await sb
      .from(SANTRI_IZIN_TABLE)
      .select('id, santri_id, pengaju_id, tujuan_wakasek, keperluan, status, catatan_wakasek, reviewed_by, reviewed_at, created_at')
      .eq('tujuan_wakasek', 'wakasek_kurikulum')
      .eq('status', 'diterima')
      .order('created_at', { ascending: false })

    if (izinRes.error) throw izinRes.error
    const rows = izinRes.data || []
    if (!rows.length) {
      renderPerizinanSantriAdminRows([])
      return
    }

    const santriIds = [...new Set(rows.map(item => String(item.santri_id || '')).filter(Boolean))]
    const pengajuIds = [...new Set(rows.map(item => String(item.pengaju_id || '')).filter(Boolean))]
    const reviewerIds = [...new Set(rows.map(item => String(item.reviewed_by || '')).filter(Boolean))]
    const karyawanIds = [...new Set([...pengajuIds, ...reviewerIds])]

    const [santriRes, karyawanRes] = await Promise.all([
      santriIds.length
        ? sb.from('santri').select('id, nama, kelas_id').in('id', santriIds)
        : Promise.resolve({ data: [], error: null }),
      karyawanIds.length
        ? sb.from('karyawan').select('id, nama').in('id', karyawanIds)
        : Promise.resolve({ data: [], error: null })
    ])
    const infoError = santriRes.error || karyawanRes.error
    if (infoError) throw infoError

    const kelasIds = [...new Set((santriRes.data || []).map(item => String(item.kelas_id || '')).filter(Boolean))]
    const kelasRes = kelasIds.length
      ? await sb.from('kelas').select('id, nama_kelas').in('id', kelasIds)
      : { data: [], error: null }
    if (kelasRes.error) throw kelasRes.error

    const santriMap = new Map((santriRes.data || []).map(item => [String(item.id || ''), item]))
    const kelasMap = new Map((kelasRes.data || []).map(item => [String(item.id || ''), item]))
    const karyawanMap = new Map((karyawanRes.data || []).map(item => [String(item.id || ''), item]))

    const enrichedRows = rows.map(item => {
      const santri = santriMap.get(String(item.santri_id || ''))
      const kelas = kelasMap.get(String(santri?.kelas_id || ''))
      return {
        ...item,
        santri_nama: String(santri?.nama || '-'),
        kelas_nama: String(kelas?.nama_kelas || '-'),
        pengaju_nama: String(karyawanMap.get(String(item.pengaju_id || ''))?.nama || '-'),
        reviewer_nama: String(karyawanMap.get(String(item.reviewed_by || ''))?.nama || '-')
      }
    })

    renderPerizinanSantriAdminRows(enrichedRows)
  } catch (error) {
    console.error(error)
    if (isPerizinanSantriMissingTableError(error)) {
      if (box) box.innerHTML = `<div style="white-space:pre-wrap;">${escapeHtml(getPerizinanSantriMissingTableMessage())}</div>`
      return
    }
    if (box) box.innerHTML = `Gagal load data perizinan siswa: ${escapeHtml(error?.message || 'Unknown error')}`
  }
}

function initPerizinanSantriAdminPage() {
  loadPerizinanSantriAdminPage(true)
}

window.initPerizinanSantriAdminPage = initPerizinanSantriAdminPage
window.loadPerizinanSantriAdminPage = loadPerizinanSantriAdminPage
