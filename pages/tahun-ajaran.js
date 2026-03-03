const TAHUN_AJARAN_CACHE_KEY = 'tahun_ajaran:list'
const TAHUN_AJARAN_CACHE_TTL_MS = 2 * 60 * 1000
let currentSemesterTahunId = null
let currentSemesterTahunNama = ''
let currentEditSemesterId = null
let currentSemesterTahunMulai = ''
let currentSemesterTahunSelesai = ''

function isMissingColumnError(error) {
  const code = String(error?.code || '').toUpperCase()
  const msg = String(error?.message || '').toLowerCase()
  return code === '42703' || (msg.includes('column') && msg.includes('does not exist'))
}

function buildAcademicDateColumnsMissingMessage() {
  return `Kolom tanggal untuk tahun ajaran/semester belum tersedia.\n\nJalankan SQL ini di Supabase:\n\nalter table public.tahun_ajaran\n  add column if not exists tanggal_mulai date,\n  add column if not exists tanggal_selesai date;\n\nalter table public.semester\n  add column if not exists tanggal_mulai date,\n  add column if not exists tanggal_selesai date;`
}

function toDateOnly(value) {
  const raw = String(value || '').trim()
  if (!raw) return null
  const d = new Date(`${raw}T00:00:00`)
  if (Number.isNaN(d.getTime())) return null
  return d
}

function toIsoDate(value) {
  const d = toDateOnly(value)
  if (!d) return ''
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function toIsoFromDate(dateObj) {
  if (!(dateObj instanceof Date) || Number.isNaN(dateObj.getTime())) return ''
  const y = dateObj.getFullYear()
  const m = String(dateObj.getMonth() + 1).padStart(2, '0')
  const day = String(dateObj.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function addDaysIso(value, days) {
  const d = toDateOnly(value)
  if (!d) return ''
  d.setDate(d.getDate() + Number(days || 0))
  return toIsoFromDate(d)
}

function formatDateLabel(value) {
  const d = toDateOnly(value)
  if (!d) return '-'
  return d.toLocaleDateString('id-ID', { day: '2-digit', month: 'short', year: 'numeric' })
}

function compareIsoDate(a, b) {
  const aIso = toIsoDate(a)
  const bIso = toIsoDate(b)
  if (!aIso || !bIso) return 0
  return aIso.localeCompare(bIso)
}

function getAcademicYearNameFromDates(startDate, endDate) {
  const start = toDateOnly(startDate)
  const end = toDateOnly(endDate)
  if (!start || !end) return ''
  const startYear = start.getFullYear()
  const endYear = end.getFullYear()
  return `${startYear}/${endYear}`
}

function getSemesterMidpointDate(startDate, endDate) {
  const start = toDateOnly(startDate)
  const end = toDateOnly(endDate)
  if (!start || !end) return null
  const midpointMs = Math.floor((start.getTime() + end.getTime()) / 2)
  return new Date(midpointMs)
}

function getSemesterNameByRange(startDate, endDate, yearStartDate, yearEndDate) {
  const semStart = toDateOnly(startDate)
  const semEnd = toDateOnly(endDate)
  const yearStart = toDateOnly(yearStartDate)
  const yearEnd = toDateOnly(yearEndDate)
  if (!semStart || !semEnd || !yearStart || !yearEnd) return ''
  const midpoint = getSemesterMidpointDate(yearStartDate, yearEndDate)
  if (!midpoint) return ''
  if (semEnd.getTime() <= midpoint.getTime()) return 'Ganjil / Semester 1'
  if (semStart.getTime() > midpoint.getTime()) return 'Genap / Semester 2'
  return ''
}

function bumpJadwalCacheVersion() {
  try {
    localStorage.setItem('jadwal_cache_version', String(Date.now()))
  } catch (e) {
    // ignore storage access error
  }
}

function getInsetFieldStyle(extra = '') {
  return `width:100%; padding:10px 12px; box-sizing:border-box; border:1px solid #cbd5e1; border-radius:999px; background:#f8fafc; box-shadow:none; outline:none; transition:border-color 0.2s, box-shadow 0.2s; ${extra}`
}

function ensureTahunFieldStyle() {
  if (document.getElementById('tahun-field-style')) return

  const style = document.createElement('style')
  style.id = 'tahun-field-style'
  style.innerHTML = `
    .tahun-field {
      border: 1px solid #cbd5e1;
      border-radius: 999px;
      padding: 9px 12px;
      box-sizing: border-box;
      background: #f8fafc;
      outline: none;
    }
    .tahun-field:focus {
      border-color: #16a34a !important;
      box-shadow: 0 0 0 3px rgba(22, 163, 74, 0.25) !important;
    }
  `
  document.head.appendChild(style)
}

function ensureTahunActionStyle() {
  if (document.getElementById('tahun-btn-style')) return

  const style = document.createElement('style')
  style.id = 'tahun-btn-style'
  style.innerHTML = `
    .btn-aktif {
      background: #0f766e;
      color: #fff;
      border: none;
      padding: 7px 16px;
      border-radius: 5px;
      margin-right: 6px;
      cursor: pointer;
      box-shadow: 0 0 8px 0 #0f766e55;
      transition: box-shadow 0.2s, transform 0.1s;
      font-weight: bold;
      outline: none;
    }
    .btn-aktif:active {
      box-shadow: 0 0 18px 2px #0f766ecc, 0 0 0 2px #fff;
      transform: scale(0.96);
    }
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
    .btn-semester {
      background: #7c3aed;
      color: #fff;
      border: none;
      padding: 7px 16px;
      border-radius: 5px;
      margin-right: 6px;
      cursor: pointer;
      box-shadow: 0 0 8px 0 #7c3aed55;
      transition: box-shadow 0.2s, transform 0.1s;
      font-weight: bold;
      outline: none;
    }
    .btn-semester:active {
      box-shadow: 0 0 18px 2px #7c3aedcc, 0 0 0 2px #fff;
      transform: scale(0.96);
    }
    .semester-chip {
      display: inline-block;
      margin: 2px 6px 2px 0;
      padding: 3px 10px;
      border-radius: 999px;
      font-size: 12px;
      border: 1px solid #cbd5e1;
      color: #334155;
      background: #f8fafc;
    }
    .semester-chip.active {
      color: #166534;
      border-color: #86efac;
      background: #dcfce7;
      font-weight: 600;
    }
  `
  document.head.appendChild(style)
}

function createAddTahunModal() {
  let modal = document.getElementById('add-tahun-modal')
  if (modal) modal.remove()

  modal = document.createElement('div')
  modal.id = 'add-tahun-modal'
  modal.style.position = 'fixed'
  modal.style.top = '0'
  modal.style.left = '0'
  modal.style.width = '100vw'
  modal.style.height = '100vh'
  modal.style.background = 'rgba(0,0,0,0.3)'
  modal.style.display = 'none'
  modal.style.zIndex = '9999'
  modal.innerHTML = `
    <div style="background:#fff; margin:80px auto; padding:24px; border-radius:8px; width:320px; box-shadow:0 2px 12px #0002; position:relative;">
      <h3>Tambah Tahun Ajaran</h3>
      <label style="display:block; margin-bottom:6px; font-size:12px; color:#475569;">Tanggal Mulai</label>
      <input class="tahun-field" type="date" id="modal-add-tahun-mulai" style="${getInsetFieldStyle('margin-bottom:8px;')}">
      <label style="display:block; margin-bottom:6px; font-size:12px; color:#475569;">Tanggal Selesai</label>
      <input class="tahun-field" type="date" id="modal-add-tahun-selesai" style="${getInsetFieldStyle('margin-bottom:8px;')}">
      <div style="margin-bottom:12px; font-size:12px; color:#475569;">
        Nama otomatis: <span id="modal-add-tahun-preview" style="font-weight:600; color:#0f172a;">-</span>
      </div>
      <button class="modal-btn modal-btn-primary" onclick="tambahTahun()">Simpan</button>
      <button class="modal-btn modal-btn-secondary" onclick="closeAddTahunModal()" style="margin-left:8px;">Batal</button>
      <span onclick="closeAddTahunModal()" style="position:absolute;top:8px;right:12px;cursor:pointer;font-size:18px;">&times;</span>
    </div>
  `
  document.body.appendChild(modal)
  const mulaiEl = document.getElementById('modal-add-tahun-mulai')
  const selesaiEl = document.getElementById('modal-add-tahun-selesai')
  const previewEl = document.getElementById('modal-add-tahun-preview')
  const syncPreview = () => {
    if (!previewEl) return
    const name = getAcademicYearNameFromDates(mulaiEl?.value, selesaiEl?.value)
    previewEl.textContent = name || '-'
  }
  if (mulaiEl) mulaiEl.addEventListener('change', syncPreview)
  if (selesaiEl) selesaiEl.addEventListener('change', syncPreview)
  return modal
}

function openAddTahunModal() {
  createAddTahunModal()
  document.getElementById('add-tahun-modal').style.display = 'block'
}

function closeAddTahunModal() {
  const modal = document.getElementById('add-tahun-modal')
  if (!modal) return
  modal.style.display = 'none'
  const mulaiEl = document.getElementById('modal-add-tahun-mulai')
  const selesaiEl = document.getElementById('modal-add-tahun-selesai')
  const previewEl = document.getElementById('modal-add-tahun-preview')
  if (mulaiEl) mulaiEl.value = ''
  if (selesaiEl) selesaiEl.value = ''
  if (previewEl) previewEl.textContent = '-'
}

function createSemesterModal() {
  let modal = document.getElementById('semester-modal')
  if (modal) modal.remove()

  modal = document.createElement('div')
  modal.id = 'semester-modal'
  modal.style.position = 'fixed'
  modal.style.top = '0'
  modal.style.left = '0'
  modal.style.width = '100vw'
  modal.style.height = '100vh'
  modal.style.background = 'rgba(0,0,0,0.3)'
  modal.style.display = 'none'
  modal.style.zIndex = '9999'
  modal.innerHTML = `
    <div style="background:#fff; margin:40px auto; padding:24px; border-radius:8px; width:560px; max-width:calc(100vw - 24px); max-height:88vh; overflow-y:auto; box-shadow:0 2px 12px #0002; position:relative;">
      <h3 id="semester-modal-title" style="margin-top:0;">Kelola Semester</h3>
      <div style="margin-bottom:10px;">
        <div style="margin-bottom:8px; font-size:12px; color:#475569;">
          Nama semester otomatis: <span id="semester-auto-name" style="font-weight:600; color:#0f172a;">-</span>
        </div>
        <label style="display:block; margin-bottom:6px; font-size:12px; color:#475569;">Tanggal Mulai</label>
        <input class="tahun-field" type="date" id="semester-mulai-input" style="${getInsetFieldStyle('margin-bottom:8px;')}" onchange="onSemesterDateInputChange()">
        <label style="display:block; margin-bottom:6px; font-size:12px; color:#475569;">Tanggal Selesai</label>
        <input class="tahun-field" type="date" id="semester-selesai-input" style="${getInsetFieldStyle('margin-bottom:8px;')}" onchange="onSemesterDateInputChange()">
        <label style="display:flex; gap:8px; align-items:center; margin-bottom:10px;">
          <input type="checkbox" id="semester-aktif-input">
          Aktif
        </label>
        <button class="modal-btn modal-btn-primary" onclick="simpanSemester()">Simpan Semester</button>
        <button class="modal-btn modal-btn-secondary" onclick="resetSemesterForm()" style="margin-left:8px;">Reset</button>
      </div>
      <div id="semester-list-container" style="border:1px solid #e2e8f0; border-radius:8px; padding:10px;">
        Loading...
      </div>
      <div style="margin-top:12px;">
        <button class="modal-btn modal-btn-secondary" onclick="closeSemesterModal()">Tutup</button>
      </div>
      <span onclick="closeSemesterModal()" style="position:absolute;top:8px;right:12px;cursor:pointer;font-size:18px;">&times;</span>
    </div>
  `
  document.body.appendChild(modal)
  return modal
}

function closeSemesterModal() {
  const modal = document.getElementById('semester-modal')
  if (!modal) return
  modal.style.display = 'none'
  currentSemesterTahunId = null
  currentSemesterTahunNama = ''
  currentSemesterTahunMulai = ''
  currentSemesterTahunSelesai = ''
  currentEditSemesterId = null
}

function resetSemesterForm() {
  const mulaiEl = document.getElementById('semester-mulai-input')
  const selesaiEl = document.getElementById('semester-selesai-input')
  const aktifEl = document.getElementById('semester-aktif-input')
  const autoNameEl = document.getElementById('semester-auto-name')
  if (mulaiEl) mulaiEl.value = ''
  if (selesaiEl) selesaiEl.value = ''
  if (aktifEl) aktifEl.checked = false
  if (autoNameEl) autoNameEl.textContent = '-'
  currentEditSemesterId = null
}

function updateSemesterAutoNamePreview() {
  const mulaiEl = document.getElementById('semester-mulai-input')
  const selesaiEl = document.getElementById('semester-selesai-input')
  const autoNameEl = document.getElementById('semester-auto-name')
  if (!autoNameEl) return ''
  const name = getSemesterNameByRange(
    mulaiEl?.value,
    selesaiEl?.value,
    currentSemesterTahunMulai,
    currentSemesterTahunSelesai
  )
  autoNameEl.textContent = name || '-'
  return name
}

function onSemesterDateInputChange() {
  updateSemesterAutoNamePreview()
}

async function openSemesterModal(tahunId, tahunNama, tahunMulai = '', tahunSelesai = '') {
  createSemesterModal()
  currentSemesterTahunId = tahunId
  currentSemesterTahunNama = tahunNama || ''
  currentSemesterTahunMulai = toIsoDate(tahunMulai)
  currentSemesterTahunSelesai = toIsoDate(tahunSelesai)
  currentEditSemesterId = null
  const titleEl = document.getElementById('semester-modal-title')
  if (titleEl) titleEl.textContent = `Kelola Semester - ${currentSemesterTahunNama}`
  resetSemesterForm()
  const mulaiEl = document.getElementById('semester-mulai-input')
  const selesaiEl = document.getElementById('semester-selesai-input')
  if (mulaiEl && currentSemesterTahunMulai) {
    mulaiEl.min = currentSemesterTahunMulai
    mulaiEl.max = currentSemesterTahunSelesai || ''
  }
  if (selesaiEl && currentSemesterTahunSelesai) {
    selesaiEl.min = currentSemesterTahunMulai || ''
    selesaiEl.max = currentSemesterTahunSelesai
  }
  await loadSemesterListForModal()
  document.getElementById('semester-modal').style.display = 'block'
}

async function loadSemesterListForModal() {
  const container = document.getElementById('semester-list-container')
  if (!container || !currentSemesterTahunId) return

  container.innerHTML = 'Loading...'
  const { data, error } = await sb
    .from('semester')
    .select('id, tahun_ajaran_id, nama, aktif, tanggal_mulai, tanggal_selesai')
    .eq('tahun_ajaran_id', currentSemesterTahunId)
    .order('aktif', { ascending: false })
    .order('tanggal_mulai', { ascending: true, nullsFirst: true })

  if (error) {
    console.error(error)
    if (isMissingColumnError(error)) {
      container.innerHTML = 'Kolom tanggal semester belum tersedia.'
      alert(buildAcademicDateColumnsMissingMessage())
      return
    }
    container.innerHTML = 'Gagal load semester'
    return
  }

  const list = data || []
  window.currentSemesterList = list

  if (list.length === 0) {
    container.innerHTML = 'Belum ada semester pada tahun ajaran ini.'
    return
  }

  let html = `
    <div style="overflow-x:auto;">
      <table style="width:100%; border-collapse:collapse; font-size:13px;">
        <thead>
          <tr style="background:#f8fafc;">
            <th style="padding:8px; border:1px solid #e2e8f0; text-align:center;">Nama</th>
            <th style="padding:8px; border:1px solid #e2e8f0; text-align:center; width:230px;">Rentang</th>
            <th style="padding:8px; border:1px solid #e2e8f0; text-align:center; width:100px;">Status</th>
            <th style="padding:8px; border:1px solid #e2e8f0; text-align:center; width:210px;">Aksi</th>
          </tr>
        </thead>
        <tbody>
  `

  html += list.map(item => `
    <tr>
      <td style="padding:8px; border:1px solid #e2e8f0;">${item.nama ?? '-'}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${formatDateLabel(item.tanggal_mulai)} - ${formatDateLabel(item.tanggal_selesai)}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${item.aktif ? 'Aktif' : 'Tidak Aktif'}</td>
      <td style="padding:8px; border:1px solid #e2e8f0; text-align:center; white-space:nowrap;">
        <button class="btn-aktif" onclick="toggleAktifSemester('${item.id}', ${item.aktif ? 'true' : 'false'})">${item.aktif ? 'Nonaktifkan' : 'Set Aktif'}</button>
        <button class="btn-edit" onclick="editSemester('${item.id}')">Edit</button>
        <button class="btn-hapus" onclick="hapusSemester('${item.id}')">Hapus</button>
      </td>
    </tr>
  `).join('')

  html += '</tbody></table></div>'
  container.innerHTML = html
}

async function setSemesterAktif(targetId, active) {
  if (!currentSemesterTahunId || !targetId) return

  if (active) {
    const { error: resetError } = await sb
      .from('semester')
      .update({ aktif: false })
      .eq('tahun_ajaran_id', currentSemesterTahunId)
      .neq('id', targetId)

    if (resetError) {
      console.error(resetError)
      alert('Gagal mengatur status aktif semester')
      return false
    }
  }

  const { error } = await sb
    .from('semester')
    .update({ aktif: active })
    .eq('id', targetId)

  if (error) {
    console.error(error)
    alert('Gagal mengubah status semester')
    return false
  }
  return true
}

async function simpanSemester() {
  if (!currentSemesterTahunId) return
  const tanggalMulai = toIsoDate(document.getElementById('semester-mulai-input')?.value)
  const tanggalSelesai = toIsoDate(document.getElementById('semester-selesai-input')?.value)
  const aktif = document.getElementById('semester-aktif-input')?.checked ?? false
  const nama = updateSemesterAutoNamePreview()

  if (!tanggalMulai || !tanggalSelesai) {
    alert('Tanggal mulai dan tanggal selesai semester wajib diisi')
    return
  }
  if (compareIsoDate(tanggalMulai, tanggalSelesai) > 0) {
    alert('Tanggal mulai semester tidak boleh lebih besar dari tanggal selesai')
    return
  }
  if (
    (currentSemesterTahunMulai && compareIsoDate(tanggalMulai, currentSemesterTahunMulai) < 0) ||
    (currentSemesterTahunSelesai && compareIsoDate(tanggalSelesai, currentSemesterTahunSelesai) > 0)
  ) {
    alert('Rentang semester harus berada dalam rentang tahun ajaran')
    return
  }
  if (!nama) {
    alert('Rentang semester harus berada penuh di paruh pertama atau paruh kedua tahun ajaran')
    return
  }

  const overlapRes = await sb
    .from('semester')
    .select('id, tanggal_mulai, tanggal_selesai')
    .eq('tahun_ajaran_id', currentSemesterTahunId)
  if (overlapRes.error) {
    console.error(overlapRes.error)
    if (isMissingColumnError(overlapRes.error)) {
      alert(buildAcademicDateColumnsMissingMessage())
      return
    }
    alert('Gagal memvalidasi rentang semester')
    return
  }
  const overlaps = (overlapRes.data || []).some(item => {
    if (currentEditSemesterId && String(item.id) === String(currentEditSemesterId)) return false
    const start = toIsoDate(item.tanggal_mulai)
    const end = toIsoDate(item.tanggal_selesai)
    if (!start || !end) return false
    return !(compareIsoDate(tanggalSelesai, start) < 0 || compareIsoDate(tanggalMulai, end) > 0)
  })
  if (overlaps) {
    alert('Rentang semester bentrok dengan semester lain pada tahun ajaran ini')
    return
  }

  const payload = {
    nama,
    aktif,
    tanggal_mulai: tanggalMulai,
    tanggal_selesai: tanggalSelesai
  }

  if (currentEditSemesterId) {
    const { error } = await sb
      .from('semester')
      .update(payload)
      .eq('id', currentEditSemesterId)

    if (error) {
      console.error(error)
      if (isMissingColumnError(error)) {
        alert(buildAcademicDateColumnsMissingMessage())
        return
      }
      alert('Gagal mengubah semester')
      return
    }

    if (aktif) {
      const ok = await setSemesterAktif(currentEditSemesterId, true)
      if (!ok) return
    }
  } else {
    const { data, error } = await sb
      .from('semester')
      .insert([{
        tahun_ajaran_id: currentSemesterTahunId,
        ...payload
      }])
      .select('id')
      .single()

    if (error) {
      console.error(error)
      if (isMissingColumnError(error)) {
        alert(buildAcademicDateColumnsMissingMessage())
        return
      }
      alert('Gagal menambah semester')
      return
    }

    if (aktif && data?.id) {
      const ok = await setSemesterAktif(data.id, true)
      if (!ok) return
    }
  }

  if (typeof clearCachedData === 'function') clearCachedData(TAHUN_AJARAN_CACHE_KEY)
  bumpJadwalCacheVersion()
  resetSemesterForm()
  await loadSemesterListForModal()
  loadTahunAjaran(true)
}

function editSemester(id) {
  const item = (window.currentSemesterList || []).find(s => String(s.id) === String(id))
  if (!item) {
    alert('Data semester tidak ditemukan')
    return
  }
  currentEditSemesterId = id
  const mulaiEl = document.getElementById('semester-mulai-input')
  const selesaiEl = document.getElementById('semester-selesai-input')
  const aktifEl = document.getElementById('semester-aktif-input')
  if (mulaiEl) mulaiEl.value = toIsoDate(item.tanggal_mulai)
  if (selesaiEl) selesaiEl.value = toIsoDate(item.tanggal_selesai)
  if (aktifEl) aktifEl.checked = item.aktif ?? false
  updateSemesterAutoNamePreview()
}

async function toggleAktifSemester(id, currentAktif) {
  if (!id) return
  if (!currentAktif) {
    const confirmed = typeof showPopupConfirm === 'function'
      ? await showPopupConfirm('Aktifkan semester ini?')
      : confirm('Aktifkan semester ini?')
    if (!confirmed) return
    const ok = await setSemesterAktif(id, true)
    if (!ok) return
  } else {
    const confirmed = typeof showPopupConfirm === 'function'
      ? await showPopupConfirm('Nonaktifkan semester ini?')
      : confirm('Nonaktifkan semester ini?')
    if (!confirmed) return
    const ok = await setSemesterAktif(id, false)
    if (!ok) return
  }

  if (typeof clearCachedData === 'function') clearCachedData(TAHUN_AJARAN_CACHE_KEY)
  bumpJadwalCacheVersion()
  await loadSemesterListForModal()
  loadTahunAjaran(true)
}

async function hapusSemester(id) {
  const item = (window.currentSemesterList || []).find(s => String(s.id) === String(id))
  if (item?.aktif) {
    alert('Semester aktif tidak boleh dihapus')
    return
  }

  const confirmed = typeof showPopupConfirm === 'function'
    ? await showPopupConfirm('Yakin ingin menghapus semester ini?')
    : confirm('Yakin ingin menghapus semester ini?')
  if (!confirmed) return

  const { error } = await sb
    .from('semester')
    .delete()
    .eq('id', id)

  if (error) {
    console.error(error)
    alert('Gagal menghapus semester')
    return
  }

  if (typeof clearCachedData === 'function') clearCachedData(TAHUN_AJARAN_CACHE_KEY)
  bumpJadwalCacheVersion()
  await loadSemesterListForModal()
  loadTahunAjaran(true)
}

async function loadTahunAjaran(forceRefresh = false) {
  const container = document.getElementById('list-tahun')
  if (!container) return

  if (!forceRefresh && typeof getCachedData === 'function') {
    const cached = getCachedData(TAHUN_AJARAN_CACHE_KEY, TAHUN_AJARAN_CACHE_TTL_MS)
    if (cached) {
      const normalizedCached = Array.isArray(cached)
        ? { tahunList: cached, semesterList: [] }
        : cached
      renderTahunAjaranTable(normalizedCached, container)
      return
    }
  }

  container.innerHTML = 'Loading...'

  const { data, error } = await sb
    .from('tahun_ajaran')
    .select('*')
    .order('tanggal_mulai', { ascending: false, nullsFirst: false })
    .order('nama', { ascending: false })

  if (error) {
    container.innerHTML = 'Terjadi kesalahan'
    console.error(error)
    if (isMissingColumnError(error)) {
      alert(buildAcademicDateColumnsMissingMessage())
    }
    return
  }

  const tahunList = data || []
  const tahunIds = tahunList.map(item => item.id)
  let semesterList = []
  if (tahunIds.length > 0) {
    const { data: semesterData, error: semesterError } = await sb
      .from('semester')
      .select('id, tahun_ajaran_id, nama, aktif, tanggal_mulai, tanggal_selesai')
      .in('tahun_ajaran_id', tahunIds)
      .order('aktif', { ascending: false })
      .order('tanggal_mulai', { ascending: true, nullsFirst: true })
    if (semesterError) {
      console.error(semesterError)
      if (isMissingColumnError(semesterError)) {
        alert(buildAcademicDateColumnsMissingMessage())
      }
    } else {
      semesterList = semesterData || []
    }
  }

  const payload = { tahunList, semesterList }
  if (typeof setCachedData === 'function') {
    setCachedData(TAHUN_AJARAN_CACHE_KEY, payload)
  }
  renderTahunAjaranTable(payload, container)
}

function renderTahunAjaranTable(payload, container) {
  if (!container) return
  const data = payload?.tahunList || []
  const semesterList = payload?.semesterList || []
  if (!data || data.length === 0) {
    container.innerHTML = '<p>Belum ada data.</p>'
    return
  }

  const semesterMap = new Map()
  semesterList.forEach(item => {
    const key = String(item.tahun_ajaran_id)
    if (!semesterMap.has(key)) semesterMap.set(key, [])
    semesterMap.get(key).push(item)
  })

  ensureTahunActionStyle()

  let html = `
    <div style="overflow-x:auto;">
      <table style="width:100%; border-collapse:collapse; margin-top:8px; font-size:13px;">
        <thead>
          <tr style="background:#f3f3f3;">
            <th style="padding:8px; border:1px solid #ddd; text-align:center;">Nama Tahun Ajaran</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:240px;">Rentang Tanggal</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:280px;">Semester</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:120px;">Status</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:380px;">Aksi</th>
          </tr>
        </thead>
        <tbody>
  `

  html += data.map(tahun => {
    const semList = semesterMap.get(String(tahun.id)) || []
    const semHtml = semList.length === 0
      ? '<span style="color:#94a3b8;">Belum ada semester</span>'
      : semList.map(sem => `<span class="semester-chip ${sem.aktif ? 'active' : ''}">${sem.nama}</span>`).join('')
    const safeNamaArg = JSON.stringify(tahun.nama ?? '')
    const safeMulaiArg = JSON.stringify(toIsoDate(tahun.tanggal_mulai) || '')
    const safeSelesaiArg = JSON.stringify(toIsoDate(tahun.tanggal_selesai) || '')
    const rentang = `${formatDateLabel(tahun.tanggal_mulai)} - ${formatDateLabel(tahun.tanggal_selesai)}`

    return `
      <tr>
        <td style="padding:8px; border:1px solid #ddd;">${tahun.nama}</td>
        <td style="padding:8px; border:1px solid #ddd;">${rentang}</td>
        <td style="padding:8px; border:1px solid #ddd;">${semHtml}</td>
        <td style="padding:8px; border:1px solid #ddd;">${tahun.aktif ? 'Aktif' : 'Tidak Aktif'}</td>
        <td style="padding:8px; border:1px solid #ddd; text-align:center; white-space:nowrap;">
          <button class="btn-semester" onclick='openSemesterModal("${tahun.id}", ${safeNamaArg}, ${safeMulaiArg}, ${safeSelesaiArg})'>Kelola Semester</button>
          <button class="btn-aktif" onclick="setAktif('${tahun.id}')">
            ${tahun.aktif ? 'Nonaktifkan' : 'Set Aktif'}
          </button>
          <button class="btn-hapus" onclick="hapusTahun('${tahun.id}')">Hapus</button>
        </td>
      </tr>
    `
  }).join('')

  html += '</tbody></table></div>'
  container.innerHTML = html
}

async function tambahTahun() {
  const mulaiEl = document.getElementById('modal-add-tahun-mulai')
  const selesaiEl = document.getElementById('modal-add-tahun-selesai')
  if (!mulaiEl || !selesaiEl) return

  const tanggalMulai = toIsoDate(mulaiEl.value)
  const tanggalSelesai = toIsoDate(selesaiEl.value)
  if (!tanggalMulai || !tanggalSelesai) {
    alert('Tanggal mulai dan tanggal selesai tahun ajaran wajib diisi')
    return
  }
  if (compareIsoDate(tanggalMulai, tanggalSelesai) > 0) {
    alert('Tanggal mulai tidak boleh lebih besar dari tanggal selesai')
    return
  }

  const nama = getAcademicYearNameFromDates(tanggalMulai, tanggalSelesai)
  if (!nama) {
    alert('Gagal membentuk nama tahun ajaran dari rentang tanggal')
    return
  }

  const { data, error } = await sb
    .from('tahun_ajaran')
    .insert([{
      nama,
      tanggal_mulai: tanggalMulai,
      tanggal_selesai: tanggalSelesai
    }])
    .select('id')
    .single()

  if (error) {
    console.error(error)
    if (isMissingColumnError(error)) {
      alert(buildAcademicDateColumnsMissingMessage())
      return
    }
    alert('Gagal menambah data')
    return
  }

  const tahunId = String(data?.id || '')
  if (tahunId) {
    const midpoint = getSemesterMidpointDate(tanggalMulai, tanggalSelesai)
    const firstEnd = midpoint ? toIsoFromDate(midpoint) : ''
    const secondStart = midpoint ? addDaysIso(firstEnd, 1) : ''
    const semesterRows = []
    if (firstEnd && compareIsoDate(tanggalMulai, firstEnd) <= 0) {
      semesterRows.push({
        tahun_ajaran_id: tahunId,
        nama: 'Ganjil / Semester 1',
        tanggal_mulai: tanggalMulai,
        tanggal_selesai: firstEnd,
        aktif: true
      })
    }
    if (secondStart && compareIsoDate(secondStart, tanggalSelesai) <= 0) {
      semesterRows.push({
        tahun_ajaran_id: tahunId,
        nama: 'Genap / Semester 2',
        tanggal_mulai: secondStart,
        tanggal_selesai: tanggalSelesai,
        aktif: false
      })
    }

    if (semesterRows.length > 0) {
      const { error: semesterInsertError } = await sb
        .from('semester')
        .insert(semesterRows)
      if (semesterInsertError) {
        console.error(semesterInsertError)
        if (isMissingColumnError(semesterInsertError)) {
          alert(buildAcademicDateColumnsMissingMessage())
          return
        }
        alert('Tahun ajaran berhasil dibuat, tetapi gagal membuat semester otomatis.')
      }
    }
  }

  if (typeof clearCachedData === 'function') clearCachedData(TAHUN_AJARAN_CACHE_KEY)
  bumpJadwalCacheVersion()
  closeAddTahunModal()
  loadTahunAjaran(true)
}

async function setAktif(selectedId) {
  const { data: current } = await sb
    .from('tahun_ajaran')
    .select('aktif, nama')
    .eq('id', selectedId)
    .single()

  if (!current) return

  if (current.aktif) {
    const confirmNonaktif = typeof showPopupConfirm === 'function'
      ? await showPopupConfirm(`Nonaktifkan Tahun Ajaran "${current.nama}" ?`)
      : confirm(`Nonaktifkan Tahun Ajaran "${current.nama}" ?`)
    if (!confirmNonaktif) return

    const { error: updateError } = await sb
      .from('tahun_ajaran')
      .update({ aktif: false })
      .eq('id', selectedId)

    if (updateError) {
      alert('Gagal menonaktifkan.')
      console.error(updateError)
      return
    }

    const { error: semesterOffError } = await sb
      .from('semester')
      .update({ aktif: false })
      .eq('tahun_ajaran_id', selectedId)

    if (semesterOffError) {
      alert('Tahun ajaran berhasil dinonaktifkan, tetapi gagal menonaktifkan semester.')
      console.error(semesterOffError)
      return
    }
  } else {
    const confirmAktif = typeof showPopupConfirm === 'function'
      ? await showPopupConfirm(`Aktifkan Tahun Ajaran "${current.nama}" ?`)
      : confirm(`Aktifkan Tahun Ajaran "${current.nama}" ?`)
    if (!confirmAktif) return

    const { error: deactivateError } = await sb
      .from('tahun_ajaran')
      .update({ aktif: false })
      .neq('id', selectedId)
      .eq('aktif', true)

    if (deactivateError) {
      alert('Gagal mengatur tahun ajaran aktif.')
      console.error(deactivateError)
      return
    }

    const { error: deactivateOtherSemesterError } = await sb
      .from('semester')
      .update({ aktif: false })
      .neq('tahun_ajaran_id', selectedId)

    if (deactivateOtherSemesterError) {
      alert('Gagal menonaktifkan semester pada tahun ajaran lain.')
      console.error(deactivateOtherSemesterError)
      return
    }

    const { error: updateError } = await sb
      .from('tahun_ajaran')
      .update({ aktif: true })
      .eq('id', selectedId)

    if (updateError) {
      alert('Gagal mengaktifkan tahun ajaran.')
      console.error(updateError)
      return
    }

    const { data: semesterRows, error: semesterLoadError } = await sb
      .from('semester')
      .select('id, aktif, tanggal_mulai, tanggal_selesai')
      .eq('tahun_ajaran_id', selectedId)
      .order('tanggal_mulai', { ascending: true, nullsFirst: true })

    if (semesterLoadError) {
      alert('Tahun ajaran aktif, tetapi gagal memuat semester.')
      console.error(semesterLoadError)
      return
    }

    if ((semesterRows || []).length > 0) {
      const todayIso = toIsoFromDate(new Date())
      const byToday = semesterRows.find(item => {
        const start = toIsoDate(item?.tanggal_mulai)
        const end = toIsoDate(item?.tanggal_selesai)
        if (!start || !end) return false
        return compareIsoDate(todayIso, start) >= 0 && compareIsoDate(todayIso, end) <= 0
      })
      const targetSemester = byToday || semesterRows.find(item => item.aktif) || semesterRows[0]

      const { error: resetSelectedSemesterError } = await sb
        .from('semester')
        .update({ aktif: false })
        .eq('tahun_ajaran_id', selectedId)
        .neq('id', targetSemester.id)

      if (resetSelectedSemesterError) {
        alert('Tahun ajaran aktif, tetapi gagal mereset semester.')
        console.error(resetSelectedSemesterError)
        return
      }

      const { error: activateSelectedSemesterError } = await sb
        .from('semester')
        .update({ aktif: true })
        .eq('id', targetSemester.id)

      if (activateSelectedSemesterError) {
        alert('Tahun ajaran aktif, tetapi gagal mengaktifkan semester.')
        console.error(activateSelectedSemesterError)
        return
      }
    }
  }

  if (typeof clearCachedData === 'function') clearCachedData(TAHUN_AJARAN_CACHE_KEY)
  if (typeof window.invalidateTopbarActiveYearCache === 'function') {
    window.invalidateTopbarActiveYearCache()
  }
  bumpJadwalCacheVersion()
  loadTahunAjaran(true)
}

async function hapusTahun(selectedId) {
  const { data: current } = await sb
    .from('tahun_ajaran')
    .select('aktif, nama')
    .eq('id', selectedId)
    .single()

  if (!current) return

  if (current.aktif) {
    alert('Tidak boleh menghapus Tahun Ajaran yang sedang aktif.')
    return
  }

  const confirmHapus = typeof showPopupConfirm === 'function'
    ? await showPopupConfirm(`Yakin ingin menghapus Tahun Ajaran "${current.nama}" ?`)
    : confirm(`Yakin ingin menghapus Tahun Ajaran "${current.nama}" ?`)
  if (!confirmHapus) return

  // Hapus semester terlebih dahulu agar tidak mentok FK ke tahun_ajaran.
  const { error: semesterDeleteError } = await sb
    .from('semester')
    .delete()
    .eq('tahun_ajaran_id', selectedId)

  if (semesterDeleteError) {
    console.error(semesterDeleteError)
    const message = String(semesterDeleteError.message || '').toLowerCase()
    const isFk = String(semesterDeleteError.code || '') === '23503' || message.includes('foreign key')
    if (isFk) {
      alert('Gagal menghapus semester pada tahun ajaran ini karena masih dipakai data lain. Hapus data turunannya terlebih dahulu.')
      return
    }
    alert('Gagal menghapus semester pada tahun ajaran ini.')
    return
  }

  const { error } = await sb
    .from('tahun_ajaran')
    .delete()
    .eq('id', selectedId)

  if (error) {
    console.error(error)
    const message = String(error.message || '').toLowerCase()
    const isFk = String(error.code || '') === '23503' || message.includes('foreign key')
    if (isFk) {
      alert('Gagal menghapus tahun ajaran karena masih dipakai data lain (kelas/mapel/jadwal/tugas). Hapus data terkait dulu.')
      return
    }
    alert('Gagal menghapus data')
    return
  }

  if (typeof clearCachedData === 'function') clearCachedData(TAHUN_AJARAN_CACHE_KEY)
  loadTahunAjaran(true)
}

function initTahunAjaranPage() {
  ensureTahunFieldStyle()
  ensureTahunActionStyle()
  createAddTahunModal()
  createSemesterModal()
  loadTahunAjaran()
}
