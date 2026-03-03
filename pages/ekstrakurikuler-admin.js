const EKSKUL_TABLE = 'ekstrakurikuler'
const EKSKUL_MEMBER_TABLE = 'ekstrakurikuler_anggota'

let ekskulAdminState = {
  rows: [],
  karyawanRows: [],
  santriRows: [],
  memberRows: [],
  selectedEkskulId: '',
  detailEkskulId: '',
  hasPj2Column: true
}

function escapeEkskulAdminHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

function isEkskulAdminMissingTableError(error) {
  const msg = String(error?.message || '').toLowerCase()
  const code = String(error?.code || '').toUpperCase()
  if (code === '42P01') return true
  if (msg.includes('schema cache') && (
    msg.includes(EKSKUL_TABLE) || msg.includes(EKSKUL_MEMBER_TABLE)
  )) return true
  return msg.includes('does not exist') && (
    msg.includes(EKSKUL_TABLE) || msg.includes(EKSKUL_MEMBER_TABLE)
  )
}

function isEkskulAdminMissingColumnError(error, columnName) {
  const msg = String(error?.message || '').toLowerCase()
  const col = String(columnName || '').toLowerCase()
  return !!col && msg.includes('column') && msg.includes(col)
}

function getEkskulAdminMissingTableMessage() {
  return `Tabel ekskul belum ada. Jalankan SQL:\n\ncreate table if not exists public.${EKSKUL_TABLE} (\n  id uuid primary key default gen_random_uuid(),\n  nama text not null,\n  pj_karyawan_id uuid not null,\n  pj_karyawan_id_2 uuid null,\n  deskripsi text null,\n  aktif boolean not null default true,\n  created_at timestamptz not null default now(),\n  updated_at timestamptz not null default now()\n);\n\nalter table public.${EKSKUL_TABLE}\n  add column if not exists pj_karyawan_id_2 uuid null;\n\ncreate table if not exists public.${EKSKUL_MEMBER_TABLE} (\n  id uuid primary key default gen_random_uuid(),\n  ekskul_id uuid not null,\n  santri_id uuid not null,\n  created_at timestamptz not null default now(),\n  unique (ekskul_id, santri_id)\n);\n\ncreate table if not exists public.ekstrakurikuler_indikator (\n  id uuid primary key default gen_random_uuid(),\n  ekskul_id uuid not null,\n  nama text not null,\n  deskripsi text null,\n  urutan integer not null default 1,\n  created_at timestamptz not null default now()\n);\n\ncreate table if not exists public.ekstrakurikuler_progres (\n  id uuid primary key default gen_random_uuid(),\n  ekskul_id uuid not null,\n  santri_id uuid not null,\n  indikator_id uuid null,\n  tanggal date not null default current_date,\n  nilai numeric null,\n  catatan text null,\n  updated_by uuid null,\n  created_at timestamptz not null default now()\n);`
}

function renderEkskulAdminSelectors() {
  const pjSel = document.getElementById('ekskul-admin-pj')
  const pjSel2 = document.getElementById('ekskul-admin-pj-2')
  if (pjSel) {
    const options = ['<option value="">Pilih PJ...</option>']
    ;(ekskulAdminState.karyawanRows || []).forEach(item => {
      options.push(`<option value="${escapeEkskulAdminHtml(String(item.id || ''))}">${escapeEkskulAdminHtml(String(item.nama || '-'))}</option>`)
    })
    pjSel.innerHTML = options.join('')
    if (pjSel2) pjSel2.innerHTML = options.join('')
  }
}

function getEkskulAdminPjNames(row, karyawanMap) {
  const pj1 = karyawanMap.get(String(row?.pj_karyawan_id || ''))
  const pj2 = karyawanMap.get(String(row?.pj_karyawan_id_2 || ''))
  const names = [String(pj1?.nama || '').trim(), String(pj2?.nama || '').trim()].filter(Boolean)
  return names.length ? names.join(' / ') : '-'
}

function buildEkskulAdminMemberListHtml(eid) {
  const karyawanOptions = ['<option value="">Pilih PJ...</option>']
  ;(ekskulAdminState.karyawanRows || []).forEach(item => {
    karyawanOptions.push(`<option value="${escapeEkskulAdminHtml(String(item.id || ''))}">${escapeEkskulAdminHtml(String(item.nama || '-'))}</option>`)
  })
  const santriMap = new Map((ekskulAdminState.santriRows || []).map(item => [String(item.id || ''), item]))
  const row = (ekskulAdminState.rows || []).find(item => String(item.id || '') === String(eid || '')) || null
  const rows = (ekskulAdminState.memberRows || []).filter(item => String(item.ekskul_id || '') === String(eid || ''))
  if (!eid) {
    return '<div style="color:#64748b;">Ekskul tidak ditemukan.</div>'
  }
  const formPanelHtml = `
    <div style="border:1px solid #e2e8f0; border-radius:10px; padding:10px; margin-bottom:10px; background:#fff;">
      <div style="font-weight:700; margin-bottom:8px;">Edit Data Ekskul</div>
      <div style="display:grid; grid-template-columns:repeat(auto-fit,minmax(220px,1fr)); gap:8px;">
        <div>
          <label class="ekskul-label">Nama Ekskul</label>
          <input id="ekskul-admin-detail-nama" class="ekskul-field" type="text" value="${escapeEkskulAdminHtml(String(row?.nama || ''))}" placeholder="Nama ekskul">
        </div>
        <div>
          <label class="ekskul-label">PJ Karyawan 1</label>
          <select id="ekskul-admin-detail-pj" class="ekskul-field">${karyawanOptions.join('')}</select>
        </div>
        <div>
          <label class="ekskul-label">PJ Karyawan 2 (Opsional)</label>
          <select id="ekskul-admin-detail-pj-2" class="ekskul-field">${karyawanOptions.join('')}</select>
        </div>
        <div style="grid-column:1/-1;">
          <label class="ekskul-label">Deskripsi</label>
          <input id="ekskul-admin-detail-deskripsi" class="ekskul-field" type="text" value="${escapeEkskulAdminHtml(String(row?.deskripsi || ''))}" placeholder="Deskripsi singkat">
        </div>
      </div>
      <div style="margin-top:10px;">
        <button type="button" class="modal-btn modal-btn-primary" onclick="saveEkskulAdminDetail()">Simpan Perubahan</button>
      </div>
    </div>
  `
  const joinedIds = new Set(rows.map(item => String(item.santri_id || '')))
  const addOptions = ['<option value="">Pilih siswa...</option>']
  ;(ekskulAdminState.santriRows || []).forEach(item => {
    const sid = String(item.id || '')
    if (!sid || joinedIds.has(sid)) return
    addOptions.push(`<option value="${escapeEkskulAdminHtml(sid)}">${escapeEkskulAdminHtml(String(item.nama || '-'))}</option>`)
  })
  const addPanelHtml = `
    <div style="display:grid; grid-template-columns:1fr auto; gap:8px; align-items:end; margin-bottom:10px;">
      <div>
        <label class="ekskul-label">Tambah Siswa</label>
        <select id="ekskul-admin-detail-santri" class="ekskul-field">${addOptions.join('')}</select>
      </div>
      <div>
        <button type="button" class="modal-btn modal-btn-primary" onclick="addEkskulAdminMemberFromDetail()">Tambah</button>
      </div>
    </div>
  `
  if (!rows.length) {
    return `${formPanelHtml}${addPanelHtml}<div style="color:#64748b;">Belum ada peserta pada ekskul ini.</div>`
  }
  return `
    ${formPanelHtml}
    ${addPanelHtml}
    <div style="overflow:auto;">
      <table style="width:100%; min-width:520px; border-collapse:collapse; font-size:13px;">
        <thead>
          <tr style="background:#f8fafc;">
            <th style="padding:8px; border:1px solid #e2e8f0; width:52px;">No</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Nama Siswa</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:120px;">Aksi</th>
          </tr>
        </thead>
        <tbody>
          ${rows.map((item, idx) => {
            const santri = santriMap.get(String(item.santri_id || ''))
            return `
              <tr>
                <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${idx + 1}</td>
                <td style="padding:8px; border:1px solid #e2e8f0;">${escapeEkskulAdminHtml(String(santri?.nama || '-'))}</td>
                <td style="padding:8px; border:1px solid #e2e8f0;">
                  <button type="button" class="modal-btn modal-btn-danger" onclick="removeEkskulAdminMember('${escapeEkskulAdminHtml(String(item.id || ''))}')">Hapus</button>
                </td>
              </tr>
            `
          }).join('')}
        </tbody>
      </table>
    </div>
  `
}

function renderEkskulAdminList() {
  const box = document.getElementById('ekskul-admin-list')
  if (!box) return
  const karyawanMap = new Map((ekskulAdminState.karyawanRows || []).map(item => [String(item.id || ''), item]))
  const memberRows = ekskulAdminState.memberRows || []
  const rows = ekskulAdminState.rows || []

  if (!rows.length) {
    box.innerHTML = '<div style="color:#64748b;">Belum ada data ekskul.</div>'
    return
  }

  box.innerHTML = `
    <div style="overflow:auto;">
      <table style="width:100%; min-width:760px; border-collapse:collapse; font-size:13px;">
        <thead>
          <tr style="background:#f8fafc;">
            <th style="padding:8px; border:1px solid #e2e8f0; width:52px;">No</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Nama Ekskul</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">PJ</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:120px;">Jumlah Siswa</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:190px;">Aksi</th>
          </tr>
        </thead>
        <tbody>
          ${rows.map((row, idx) => {
            const eid = String(row.id || '')
            const jumlahSiswa = memberRows.filter(item => String(item.ekskul_id || '') === eid).length
            return `
              <tr>
                <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${idx + 1}</td>
                <td style="padding:8px; border:1px solid #e2e8f0;">${escapeEkskulAdminHtml(String(row.nama || '-'))}</td>
                <td style="padding:8px; border:1px solid #e2e8f0;">${escapeEkskulAdminHtml(getEkskulAdminPjNames(row, karyawanMap))}</td>
                <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${jumlahSiswa}</td>
                <td style="padding:8px; border:1px solid #e2e8f0;">
                  <div style="display:flex; gap:6px; flex-wrap:nowrap; white-space:nowrap;">
                    <button type="button" class="modal-btn" onclick="openEkskulAdminDetail('${escapeEkskulAdminHtml(eid)}')">Detail</button>
                    <button type="button" class="modal-btn modal-btn-danger" onclick="deleteEkskulAdminRow('${escapeEkskulAdminHtml(eid)}')">Hapus</button>
                  </div>
                </td>
              </tr>
            `
          }).join('')}
        </tbody>
      </table>
    </div>
  `
  renderEkskulAdminSelectors()
}

function openEkskulAdminDetail(exskulId) {
  const eid = String(exskulId || '').trim()
  const overlay = document.getElementById('ekskul-admin-detail-overlay')
  const titleEl = document.getElementById('ekskul-admin-detail-title')
  const bodyEl = document.getElementById('ekskul-admin-detail-body')
  if (!overlay || !titleEl || !bodyEl) return
  const row = (ekskulAdminState.rows || []).find(item => String(item.id || '') === eid) || null
  titleEl.textContent = row ? `Detail Ekskul: ${String(row.nama || '-')}` : 'Detail Ekskul'
  ekskulAdminState.detailEkskulId = eid
  bodyEl.innerHTML = buildEkskulAdminMemberListHtml(eid)
  const pj1El = document.getElementById('ekskul-admin-detail-pj')
  const pj2El = document.getElementById('ekskul-admin-detail-pj-2')
  if (pj1El) pj1El.value = String(row?.pj_karyawan_id || '')
  if (pj2El) pj2El.value = String(row?.pj_karyawan_id_2 || '')
  overlay.style.display = 'block'
}

function closeEkskulAdminDetail() {
  const overlay = document.getElementById('ekskul-admin-detail-overlay')
  if (!overlay) return
  ekskulAdminState.detailEkskulId = ''
  overlay.style.display = 'none'
}

function refreshEkskulAdminDetailBody() {
  const eid = String(ekskulAdminState.detailEkskulId || '').trim()
  const bodyEl = document.getElementById('ekskul-admin-detail-body')
  if (!eid || !bodyEl) return
  bodyEl.innerHTML = buildEkskulAdminMemberListHtml(eid)
}

async function loadEkskulAdminPage(_forceRefresh = false) {
  const list = document.getElementById('ekskul-admin-list')
  if (list) list.innerHTML = 'Loading...'
  try {
    let exPromise = sb.from(EKSKUL_TABLE).select('id, nama, pj_karyawan_id, pj_karyawan_id_2, deskripsi, aktif, created_at').eq('aktif', true).order('nama')
    let exRes
    try {
      exRes = await exPromise
      if (exRes.error && isEkskulAdminMissingColumnError(exRes.error, 'pj_karyawan_id_2')) {
        ekskulAdminState.hasPj2Column = false
        exRes = await sb.from(EKSKUL_TABLE).select('id, nama, pj_karyawan_id, deskripsi, aktif, created_at').eq('aktif', true).order('nama')
      } else {
        ekskulAdminState.hasPj2Column = true
      }
    } catch (error) {
      throw error
    }

    const [kRes, sRes, mRes] = await Promise.all([
      sb.from('karyawan').select('id, nama, aktif').eq('aktif', true).order('nama'),
      sb.from('santri').select('id, nama, aktif').eq('aktif', true).order('nama'),
      sb.from(EKSKUL_MEMBER_TABLE).select('id, ekskul_id, santri_id').order('created_at', { ascending: false })
    ])
    if (exRes.error) throw exRes.error
    if (kRes.error) throw kRes.error
    if (sRes.error) throw sRes.error
    if (mRes.error) throw mRes.error

    ekskulAdminState.rows = exRes.data || []
    ekskulAdminState.karyawanRows = kRes.data || []
    ekskulAdminState.santriRows = sRes.data || []
    ekskulAdminState.memberRows = mRes.data || []
    if (!ekskulAdminState.selectedEkskulId && ekskulAdminState.rows.length) {
      ekskulAdminState.selectedEkskulId = String(ekskulAdminState.rows[0].id || '')
    }
    renderEkskulAdminList()
    refreshEkskulAdminDetailBody()
  } catch (error) {
    if (isEkskulAdminMissingTableError(error)) {
      if (list) list.innerHTML = `<div style="white-space:pre-wrap; color:#334155;">${escapeEkskulAdminHtml(getEkskulAdminMissingTableMessage())}</div>`
      return
    }
    console.error(error)
    if (list) list.innerHTML = `<div style="color:#b91c1c;">Gagal load data ekskul: ${escapeEkskulAdminHtml(error?.message || 'Unknown error')}</div>`
  }
}

async function saveEkskulAdmin() {
  const nama = String(document.getElementById('ekskul-admin-nama')?.value || '').trim()
  const pj = String(document.getElementById('ekskul-admin-pj')?.value || '').trim()
  const pj2 = String(document.getElementById('ekskul-admin-pj-2')?.value || '').trim()
  const deskripsi = String(document.getElementById('ekskul-admin-deskripsi')?.value || '').trim()
  if (!nama || !pj) {
    alert('Nama ekskul dan PJ wajib diisi.')
    return
  }
  if (pj2 && pj2 === pj) {
    alert('PJ 2 harus berbeda dari PJ 1.')
    return
  }
  if (pj2 && !ekskulAdminState.hasPj2Column) {
    alert(`Kolom PJ 2 belum tersedia di database.\nJalankan SQL:\nalter table public.${EKSKUL_TABLE}\n  add column if not exists pj_karyawan_id_2 uuid null;`)
    return
  }
  const payload = { nama, pj_karyawan_id: pj, deskripsi: deskripsi || null }
  if (ekskulAdminState.hasPj2Column) payload.pj_karyawan_id_2 = pj2 || null
  const { error } = await sb.from(EKSKUL_TABLE).insert([payload])
  if (error) {
    console.error(error)
    alert(`Gagal simpan ekskul: ${error.message || 'Unknown error'}`)
    return
  }
  document.getElementById('ekskul-admin-nama').value = ''
  const pj2El = document.getElementById('ekskul-admin-pj-2')
  if (pj2El) pj2El.value = ''
  document.getElementById('ekskul-admin-deskripsi').value = ''
  await loadEkskulAdminPage(true)
}

async function saveEkskulAdminDetail() {
  const eid = String(ekskulAdminState.detailEkskulId || '').trim()
  if (!eid) return
  const nama = String(document.getElementById('ekskul-admin-detail-nama')?.value || '').trim()
  const pj = String(document.getElementById('ekskul-admin-detail-pj')?.value || '').trim()
  const pj2 = String(document.getElementById('ekskul-admin-detail-pj-2')?.value || '').trim()
  const deskripsi = String(document.getElementById('ekskul-admin-detail-deskripsi')?.value || '').trim()
  if (!nama || !pj) {
    alert('Nama ekskul dan PJ 1 wajib diisi.')
    return
  }
  if (pj2 && pj2 === pj) {
    alert('PJ 2 harus berbeda dari PJ 1.')
    return
  }
  if (pj2 && !ekskulAdminState.hasPj2Column) {
    alert(`Kolom PJ 2 belum tersedia di database.\nJalankan SQL:\nalter table public.${EKSKUL_TABLE}\n  add column if not exists pj_karyawan_id_2 uuid null;`)
    return
  }
  const payload = { nama, pj_karyawan_id: pj, deskripsi: deskripsi || null, updated_at: new Date().toISOString() }
  if (ekskulAdminState.hasPj2Column) payload.pj_karyawan_id_2 = pj2 || null
  const { error } = await sb.from(EKSKUL_TABLE).update(payload).eq('id', eid)
  if (error) {
    console.error(error)
    alert(`Gagal menyimpan perubahan ekskul: ${error.message || 'Unknown error'}`)
    return
  }
  await loadEkskulAdminPage(true)
  openEkskulAdminDetail(eid)
}

async function addEkskulAdminMemberFromDetail() {
  const eid = String(ekskulAdminState.detailEkskulId || '').trim()
  const sid = String(document.getElementById('ekskul-admin-detail-santri')?.value || '').trim()
  if (!eid || !sid) {
    alert('Pilih siswa terlebih dahulu.')
    return
  }
  const { error } = await sb.from(EKSKUL_MEMBER_TABLE).insert([{ ekskul_id: eid, santri_id: sid }])
  if (error) {
    console.error(error)
    alert(`Gagal menambah siswa: ${error.message || 'Unknown error'}`)
    return
  }
  await loadEkskulAdminPage(true)
  refreshEkskulAdminDetailBody()
}

async function removeEkskulAdminMember(memberId) {
  const mid = String(memberId || '').trim()
  if (!mid) return
  const { error } = await sb.from(EKSKUL_MEMBER_TABLE).delete().eq('id', mid)
  if (error) {
    console.error(error)
    alert(`Gagal menghapus siswa: ${error.message || 'Unknown error'}`)
    return
  }
  await loadEkskulAdminPage(true)
  refreshEkskulAdminDetailBody()
}

async function deleteEkskulAdminRow(ekskulId) {
  const eid = String(ekskulId || '').trim()
  if (!eid) return
  const ok = window.confirm('Hapus ekskul ini? Data anggota/progres akan tetap tersimpan sebagai histori.')
  if (!ok) return

  let { error } = await sb
    .from(EKSKUL_TABLE)
    .update({ aktif: false, updated_at: new Date().toISOString() })
    .eq('id', eid)

  if (error && isEkskulAdminMissingColumnError(error, 'updated_at')) {
    const retry = await sb
      .from(EKSKUL_TABLE)
      .update({ aktif: false })
      .eq('id', eid)
    error = retry.error
  }

  if (error) {
    console.error(error)
    alert(`Gagal menghapus ekskul: ${error.message || 'Unknown error'}`)
    return
  }

  if (String(ekskulAdminState.detailEkskulId || '') === eid) {
    closeEkskulAdminDetail()
  }
  await loadEkskulAdminPage(true)
}

function initEkstrakurikulerAdminPage() {
  ekskulAdminState.selectedEkskulId = ''
  loadEkskulAdminPage(true)
}

window.initEkstrakurikulerAdminPage = initEkstrakurikulerAdminPage
window.loadEkskulAdminPage = loadEkskulAdminPage
window.saveEkskulAdmin = saveEkskulAdmin
window.openEkskulAdminDetail = openEkskulAdminDetail
window.closeEkskulAdminDetail = closeEkskulAdminDetail
window.saveEkskulAdminDetail = saveEkskulAdminDetail
window.addEkskulAdminMemberFromDetail = addEkskulAdminMemberFromDetail
window.removeEkskulAdminMember = removeEkskulAdminMember
window.deleteEkskulAdminRow = deleteEkskulAdminRow
