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
      <td style="padding:8px; border:1px solid #e2e8f0;">
        <div style="display:flex; gap:8px; flex-wrap:wrap;">
          <button type="button" class="modal-btn modal-btn-secondary" style="padding:5px 10px; font-size:12px;" onclick="editPerizinanKaryawanAdmin('${escapePerizinanAdminHtml(String(item.id || ''))}')">Edit</button>
          <button type="button" class="modal-btn modal-btn-danger" style="padding:5px 10px; font-size:12px;" onclick="deletePerizinanKaryawanAdmin('${escapePerizinanAdminHtml(String(item.id || ''))}')">Hapus</button>
        </div>
      </td>
    </tr>
  `).join('')

  box.innerHTML = `
    <div style="overflow:auto;">
      <table style="width:100%; min-width:1280px; border-collapse:collapse; font-size:13px;">
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
            <th style="padding:8px; border:1px solid #e2e8f0; width:170px;">Action</th>
          </tr>
        </thead>
        <tbody>${htmlRows}</tbody>
      </table>
    </div>
  `
}

function getPerizinanKaryawanAdminRowById(id) {
  const sid = String(id || '').trim()
  if (!sid) return null
  return (perizinanKaryawanAdminState.rows || []).find(item => String(item?.id || '') === sid) || null
}

function showPerizinanAdminMessage(message) {
  if (typeof window.showPopupMessage === 'function') return window.showPopupMessage(message)
  alert(message)
  return Promise.resolve(true)
}

function showPerizinanAdminConfirm(message) {
  if (typeof window.showPopupConfirm === 'function') return window.showPopupConfirm(message)
  return Promise.resolve(confirm(message))
}

function ensurePerizinanKaryawanAdminEditModal() {
  let overlay = document.getElementById('pk-admin-edit-overlay')
  if (overlay) return overlay

  overlay = document.createElement('div')
  overlay.id = 'pk-admin-edit-overlay'
  overlay.style.cssText = 'position:fixed; inset:0; background:rgba(15,23,42,0.4); display:none; align-items:center; justify-content:center; z-index:12050; padding:16px; box-sizing:border-box;'
  overlay.innerHTML = `
    <div style="width:min(560px, 100%); max-height:90vh; overflow:auto; border:1px solid #e2e8f0; border-radius:14px; background:#fff; padding:14px;">
      <div style="font-size:16px; font-weight:700; color:#0f172a; margin-bottom:10px;">Edit Izin Karyawan</div>
      <div style="display:grid; gap:10px;">
        <div>
          <label style="display:block; font-size:12px; color:#64748b; margin-bottom:4px;">Tanggal Mulai</label>
          <input id="pk-admin-edit-mulai" class="karyawan-field" type="date">
        </div>
        <div>
          <label style="display:block; font-size:12px; color:#64748b; margin-bottom:4px;">Tanggal Selesai</label>
          <input id="pk-admin-edit-selesai" class="karyawan-field" type="date">
        </div>
        <div>
          <label style="display:block; font-size:12px; color:#64748b; margin-bottom:4px;">Status</label>
          <select id="pk-admin-edit-status" class="karyawan-field">
            <option value="menunggu">Menunggu</option>
            <option value="diterima">Diterima</option>
            <option value="ditolak">Ditolak</option>
          </select>
        </div>
        <div>
          <label style="display:block; font-size:12px; color:#64748b; margin-bottom:4px;">Keperluan</label>
          <textarea id="pk-admin-edit-keperluan" class="karyawan-field" rows="3" placeholder="Keperluan izin"></textarea>
        </div>
        <div>
          <label style="display:block; font-size:12px; color:#64748b; margin-bottom:4px;">Catatan Wakasek</label>
          <input id="pk-admin-edit-catatan" class="karyawan-field" type="text" placeholder="Catatan (opsional)">
        </div>
      </div>
      <div style="display:flex; justify-content:flex-end; gap:8px; margin-top:12px;">
        <button type="button" class="modal-btn modal-btn-secondary" id="pk-admin-edit-batal">Batal</button>
        <button type="button" class="modal-btn modal-btn-primary" id="pk-admin-edit-simpan">Simpan</button>
      </div>
    </div>
  `
  overlay.addEventListener('click', event => {
    if (event.target === overlay) overlay.style.display = 'none'
  })
  document.body.appendChild(overlay)
  return overlay
}

async function editPerizinanKaryawanAdmin(id) {
  const row = getPerizinanKaryawanAdminRowById(id)
  if (!row) {
    await showPerizinanAdminMessage('Data izin tidak ditemukan.')
    return
  }
  const overlay = ensurePerizinanKaryawanAdminEditModal()
  const mulaiEl = document.getElementById('pk-admin-edit-mulai')
  const selesaiEl = document.getElementById('pk-admin-edit-selesai')
  const statusEl = document.getElementById('pk-admin-edit-status')
  const keperluanEl = document.getElementById('pk-admin-edit-keperluan')
  const catatanEl = document.getElementById('pk-admin-edit-catatan')
  const batalBtn = document.getElementById('pk-admin-edit-batal')
  const simpanBtn = document.getElementById('pk-admin-edit-simpan')
  if (!mulaiEl || !selesaiEl || !statusEl || !keperluanEl || !catatanEl || !batalBtn || !simpanBtn) return

  mulaiEl.value = String(row.tanggal_mulai || '').slice(0, 10)
  selesaiEl.value = String(row.tanggal_selesai || '').slice(0, 10)
  statusEl.value = normalizePerizinanKaryawanStatus(row.status)
  keperluanEl.value = String(row.keperluan || '')
  catatanEl.value = String(row.catatan_wakasek || '')
  overlay.style.display = 'flex'

  batalBtn.onclick = () => {
    overlay.style.display = 'none'
  }
  simpanBtn.onclick = async () => {
    const tanggalMulai = String(mulaiEl.value || '').trim()
    const tanggalSelesai = String(selesaiEl.value || '').trim()
    const keperluan = String(keperluanEl.value || '').trim()
    const status = normalizePerizinanKaryawanStatus(statusEl.value)
    const catatan = String(catatanEl.value || '').trim()
    if (!tanggalMulai || !tanggalSelesai || !keperluan) {
      await showPerizinanAdminMessage('Tanggal mulai, tanggal selesai, dan keperluan wajib diisi.')
      return
    }
    if (tanggalSelesai < tanggalMulai) {
      await showPerizinanAdminMessage('Tanggal selesai tidak boleh sebelum tanggal mulai.')
      return
    }
    const diff = Math.floor((new Date(`${tanggalSelesai}T00:00:00`) - new Date(`${tanggalMulai}T00:00:00`)) / 86400000) + 1
    simpanBtn.disabled = true
    try {
      const { error } = await sb
        .from(PERIZINAN_KARYAWAN_TABLE)
        .update({
          tanggal_mulai: tanggalMulai,
          tanggal_selesai: tanggalSelesai,
          durasi_hari: Math.max(1, diff),
          keperluan,
          status,
          catatan_wakasek: catatan || null,
          updated_at: new Date().toISOString()
        })
        .eq('id', String(row.id || ''))
      if (error) throw error
      overlay.style.display = 'none'
      await loadPerizinanKaryawanAdminPage(true)
      await showPerizinanAdminMessage('Data izin berhasil diperbarui.')
    } catch (error) {
      await showPerizinanAdminMessage(`Gagal memperbarui izin: ${error?.message || 'Unknown error'}`)
    } finally {
      simpanBtn.disabled = false
    }
  }
}

async function deletePerizinanKaryawanAdmin(id) {
  const row = getPerizinanKaryawanAdminRowById(id)
  if (!row) {
    await showPerizinanAdminMessage('Data izin tidak ditemukan.')
    return
  }
  const ok = await showPerizinanAdminConfirm(`Hapus pengajuan izin ${String(row.guru_nama || '-')}\n${formatPerizinanAdminDate(row.tanggal_mulai)} s.d. ${formatPerizinanAdminDate(row.tanggal_selesai)}?`)
  if (!ok) return
  try {
    const { error } = await sb
      .from(PERIZINAN_KARYAWAN_TABLE)
      .delete()
      .eq('id', String(row.id || ''))
    if (error) throw error
    await loadPerizinanKaryawanAdminPage(true)
    await showPerizinanAdminMessage('Data izin berhasil dihapus.')
  } catch (error) {
    await showPerizinanAdminMessage(`Gagal menghapus izin: ${error?.message || 'Unknown error'}`)
  }
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
window.editPerizinanKaryawanAdmin = editPerizinanKaryawanAdmin
window.deletePerizinanKaryawanAdmin = deletePerizinanKaryawanAdmin
