const DAILY_TASK_TEMPLATE_TABLE = 'tugas_harian_template'
const DAILY_TASK_SUBMIT_TABLE = 'tugas_harian_submit'

const adminDailyTaskState = {
  periode: '',
  tahunAjaran: null,
  templates: [],
  templateGroups: [],
  draftTasks: [],
  submissions: [],
  guruList: [],
  selectedGuruId: ''
}
const ADMIN_TASK_KALENDER_TABLE = 'kalender_akademik'
const ADMIN_TASK_LIBUR_SEMUA = 'libur_semua_kegiatan'
const ADMIN_TASK_LIBUR_AKADEMIK = 'libur_akademik'

function normalizeAdminTaskKalenderType(value) {
  const raw = String(value || '').trim().toLowerCase()
  if (raw === ADMIN_TASK_LIBUR_SEMUA) return ADMIN_TASK_LIBUR_SEMUA
  if (raw === ADMIN_TASK_LIBUR_AKADEMIK) return ADMIN_TASK_LIBUR_AKADEMIK
  return ''
}

function inferAdminTaskKalenderType(row) {
  const direct = normalizeAdminTaskKalenderType(row?.jenis_kegiatan)
  if (direct) return direct
  const text = `${String(row?.judul || '')} ${String(row?.detail || '')}`.toLowerCase()
  if (text.includes('libur semua')) return ADMIN_TASK_LIBUR_SEMUA
  if (text.includes('libur akademik')) return ADMIN_TASK_LIBUR_AKADEMIK
  return ''
}

function isAdminTaskMissingKalenderTypeColumnError(error) {
  const msg = String(error?.message || '').toLowerCase()
  return msg.includes('jenis_kegiatan') && (msg.includes('schema cache') || msg.includes('column') || msg.includes('does not exist'))
}

function getAdminTaskDateRangeKeys(startValue, endValue) {
  const startText = String(startValue || '').slice(0, 10)
  const endText = String(endValue || startValue || '').slice(0, 10)
  if (!/^\d{4}-\d{2}-\d{2}$/.test(startText)) return []
  const start = new Date(`${startText}T00:00:00`)
  const end = new Date(`${endText}T00:00:00`)
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) return [startText]
  const result = []
  const cursor = new Date(start)
  while (cursor.getTime() <= end.getTime()) {
    result.push(cursor.toISOString().slice(0, 10))
    cursor.setDate(cursor.getDate() + 1)
  }
  return result
}

async function getAdminTaskAcademicHolidayDates(startDate, endDate) {
  const withType = await sb
    .from(ADMIN_TASK_KALENDER_TABLE)
    .select('id, judul, detail, jenis_kegiatan, mulai, selesai')
  let rows = []
  if (!withType.error) {
    rows = withType.data || []
  } else if (isAdminTaskMissingKalenderTypeColumnError(withType.error)) {
    const fallback = await sb
      .from(ADMIN_TASK_KALENDER_TABLE)
      .select('id, judul, detail, mulai, selesai')
    if (fallback.error) throw fallback.error
    rows = fallback.data || []
  } else {
    const msg = String(withType.error?.message || '').toLowerCase()
    if (msg.includes(ADMIN_TASK_KALENDER_TABLE) && (msg.includes('does not exist') || msg.includes('schema cache'))) return new Set()
    throw withType.error
  }

  const dateSet = new Set()
  ;(rows || []).forEach(item => {
    const type = inferAdminTaskKalenderType(item)
    if (type !== ADMIN_TASK_LIBUR_SEMUA && type !== ADMIN_TASK_LIBUR_AKADEMIK) return
    const keys = getAdminTaskDateRangeKeys(item?.mulai, item?.selesai || item?.mulai)
    keys.forEach(key => {
      if (key >= startDate && key <= endDate) dateSet.add(key)
    })
  })
  return dateSet
}

function getAdminDailyTaskMissingTableMessage() {
  return `Tabel tugas harian belum ada di Supabase.\n\nSilakan buat tabel berikut:\n\n1) ${DAILY_TASK_TEMPLATE_TABLE}\n- id (uuid primary key, default gen_random_uuid())\n- tahun_ajaran_id (uuid, nullable)\n- tanggal (date)\n- judul (text)\n- deskripsi (text, nullable)\n- aktif (boolean, default true)\n- created_at (timestamptz, default now())\n\n2) ${DAILY_TASK_SUBMIT_TABLE}\n- id (uuid primary key, default gen_random_uuid())\n- template_id (uuid)\n- guru_id (uuid)\n- tanggal (date)\n- status (text: selesai/belum)\n- catatan (text, nullable)\n- submitted_at (timestamptz, default now())\n- updated_at (timestamptz, default now())\n\nUnique disarankan:\n- ${DAILY_TASK_TEMPLATE_TABLE}: (tahun_ajaran_id, tanggal, judul)\n- ${DAILY_TASK_SUBMIT_TABLE}: (template_id, guru_id, tanggal)`
}

function isMissingDailyTaskTableError(error) {
  const code = String(error?.code || '').toUpperCase()
  const msg = String(error?.message || '').toLowerCase()
  if (code === '42P01') return true
  if (msg.includes(DAILY_TASK_TEMPLATE_TABLE.toLowerCase())) return true
  if (msg.includes(DAILY_TASK_SUBMIT_TABLE.toLowerCase())) return true
  return false
}

function getTodayMonth() {
  return new Date().toISOString().slice(0, 7)
}

function getMonthRange(periode) {
  const text = String(periode || '').trim()
  if (!/^\d{4}-\d{2}$/.test(text)) return null
  const [yearText, monthText] = text.split('-')
  const year = Number(yearText)
  const month = Number(monthText)
  if (!Number.isFinite(year) || !Number.isFinite(month) || month < 1 || month > 12) return null

  const start = `${yearText}-${monthText}-01`
  const endDate = new Date(year, month, 0)
  const end = `${yearText}-${monthText}-${String(endDate.getDate()).padStart(2, '0')}`
  return { start, end }
}

function isAhadDate(dateText) {
  const text = String(dateText || '').trim()
  if (!/^\d{4}-\d{2}-\d{2}$/.test(text)) return false
  const date = new Date(`${text}T00:00:00`)
  if (Number.isNaN(date.getTime())) return false
  return date.getDay() === 0
}

function shiftToNonAhad(dateText) {
  const text = String(dateText || '').trim()
  if (!/^\d{4}-\d{2}-\d{2}$/.test(text)) return ''
  const date = new Date(`${text}T00:00:00`)
  if (Number.isNaN(date.getTime())) return ''
  while (date.getDay() === 0) date.setDate(date.getDate() + 1)
  return date.toISOString().slice(0, 10)
}

function getDateListByPeriode(periode) {
  const range = getMonthRange(periode)
  if (!range) return []
  const dates = []
  let cursor = new Date(`${range.start}T00:00:00`)
  const end = new Date(`${range.end}T00:00:00`)
  while (cursor <= end) {
    const text = cursor.toISOString().slice(0, 10)
    if (!isAhadDate(text)) dates.push(text)
    cursor.setDate(cursor.getDate() + 1)
  }
  return dates
}

function getDateListByTaskFrequency(periode, frekuensi) {
  const mode = String(frekuensi || 'harian').toLowerCase()
  const monthRange = getMonthRange(periode)
  if (!monthRange) return []

  if (mode === 'bulanan') {
    const first = shiftToNonAhad(monthRange.start)
    return first && first.startsWith(`${periode}-`) ? [first] : []
  }
  if (mode === 'pekanan') {
    const dates = []
    let cursor = new Date(`${monthRange.start}T00:00:00`)
    if (cursor.getDay() === 0) cursor.setDate(cursor.getDate() + 1)
    const end = new Date(`${monthRange.end}T00:00:00`)
    while (cursor <= end) {
      const text = cursor.toISOString().slice(0, 10)
      if (!isAhadDate(text)) dates.push(text)
      cursor.setDate(cursor.getDate() + 7)
    }
    return dates
  }
  return getDateListByPeriode(periode)
}

function getNextPeriode(periode) {
  const text = String(periode || '').trim()
  if (!/^\d{4}-\d{2}$/.test(text)) return ''
  const [y, m] = text.split('-').map(Number)
  if (!Number.isFinite(y) || !Number.isFinite(m)) return ''
  const next = new Date(y, m, 1)
  return `${next.getFullYear()}-${String(next.getMonth() + 1).padStart(2, '0')}`
}

function shiftDateToNextMonth(dateText) {
  const text = String(dateText || '').trim()
  if (!/^\d{4}-\d{2}-\d{2}$/.test(text)) return ''
  const [y, m, d] = text.split('-').map(Number)
  if (!Number.isFinite(y) || !Number.isFinite(m) || !Number.isFinite(d)) return ''
  const nextStart = new Date(y, m, 1)
  const year = nextStart.getFullYear()
  const month = nextStart.getMonth() + 1
  const maxDay = new Date(year, month, 0).getDate()
  const day = Math.min(d, maxDay)
  return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}

function normalizeTaskText(raw) {
  return String(raw || '').trim()
}

function escapeAttr(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('"', '&quot;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
}

function pickStatusLabel(raw) {
  return String(raw || '').toLowerCase() === 'selesai' ? 'Selesai' : 'Belum'
}

function ensureAdminTaskDraftSeed() {
  if (!Array.isArray(adminDailyTaskState.draftTasks)) adminDailyTaskState.draftTasks = []
  if (!adminDailyTaskState.draftTasks.length) {
    adminDailyTaskState.draftTasks = [{ judul: '', deskripsi: '', frekuensi: 'harian' }]
  }
}

function updateAdminTaskTriggerSummary() {
  const trigger = document.getElementById('th-admin-input-trigger')
  if (!trigger) return
  const count = (adminDailyTaskState.draftTasks || []).filter(item => normalizeTaskText(item.judul)).length
  trigger.value = count ? `${count} tugas siap diset` : ''
}

function renderAdminTaskModalRows() {
  const list = document.getElementById('th-admin-modal-list')
  if (!list) return
  ensureAdminTaskDraftSeed()

  const rows = adminDailyTaskState.draftTasks || []
  list.innerHTML = rows.map((item, index) => `
    <div class="th-admin-modal-row">
      <input class="th-admin-field" type="text" value="${escapeAttr(item.judul || '')}" placeholder="Tulis tugas" oninput="updateAdminTaskModalRow(${index}, this.value)">
      <select class="th-admin-field" style="max-width:140px;" onchange="updateAdminTaskModalFrequency(${index}, this.value)">
        <option value="harian" ${String(item.frekuensi || 'harian') === 'harian' ? 'selected' : ''}>Harian</option>
        <option value="pekanan" ${String(item.frekuensi || '') === 'pekanan' ? 'selected' : ''}>Pekanan</option>
        <option value="bulanan" ${String(item.frekuensi || '') === 'bulanan' ? 'selected' : ''}>Bulanan</option>
      </select>
      <button type="button" class="th-admin-btn th-admin-btn-danger" onclick="removeAdminTaskModalRow(${index})" ${rows.length <= 1 ? 'disabled' : ''}>Hapus</button>
    </div>
  `).join('')
}

function openAdminTaskDraftModal() {
  const backdrop = document.getElementById('th-admin-task-modal-backdrop')
  if (!backdrop) return
  renderAdminTaskModalRows()
  backdrop.style.display = 'flex'
}

function closeAdminTaskDraftModal() {
  const backdrop = document.getElementById('th-admin-task-modal-backdrop')
  if (!backdrop) return
  backdrop.style.display = 'none'
  updateAdminTaskTriggerSummary()
}

function addAdminTaskModalRow() {
  ensureAdminTaskDraftSeed()
  adminDailyTaskState.draftTasks.push({ judul: '', deskripsi: '', frekuensi: 'harian' })
  renderAdminTaskModalRows()
}

function addAdminMonthlyTaskDraft() {
  addAdminTaskModalRow()
}

function updateAdminTaskModalRow(index, value) {
  ensureAdminTaskDraftSeed()
  const row = adminDailyTaskState.draftTasks[index]
  if (!row) return
  row.judul = normalizeTaskText(value)
}

function updateAdminTaskModalFrequency(index, value) {
  ensureAdminTaskDraftSeed()
  const row = adminDailyTaskState.draftTasks[index]
  if (!row) return
  row.frekuensi = ['harian', 'bulanan', 'pekanan'].includes(String(value)) ? String(value) : 'harian'
}

function removeAdminTaskModalRow(index) {
  const rows = adminDailyTaskState.draftTasks || []
  if (rows.length <= 1) {
    rows[0] = { judul: '', deskripsi: '', frekuensi: 'harian' }
  } else {
    adminDailyTaskState.draftTasks = rows.filter((_, i) => i !== Number(index))
  }
  renderAdminTaskModalRows()
}

async function submitAdminTaskDraftModal() {
  const list = document.getElementById('th-admin-modal-list')
  if (list) {
    const inputs = Array.from(list.querySelectorAll('input.th-admin-field'))
    inputs.forEach((input, index) => updateAdminTaskModalRow(index, input.value))
  }

  const hasTask = (adminDailyTaskState.draftTasks || []).some(item => normalizeTaskText(item.judul))
  if (!hasTask) {
    alert('Minimal isi 1 tugas.')
    return
  }

  await saveAdminMonthlyTaskTemplate()
}

function updateAdminMonthlyTaskDraft(index, field, value) {
  const drafts = adminDailyTaskState.draftTasks || []
  const row = drafts[index]
  if (!row) return
  row[field] = normalizeTaskText(value)
}

function removeAdminMonthlyTaskDraft(index) {
  adminDailyTaskState.draftTasks = (adminDailyTaskState.draftTasks || []).filter((_, i) => i !== Number(index))
  renderAdminTaskModalRows()
}

function clearAdminMonthlyTaskDraft() {
  adminDailyTaskState.draftTasks = [{ judul: '', deskripsi: '', frekuensi: 'harian' }]
  renderAdminTaskModalRows()
  updateAdminTaskTriggerSummary()
}

function collectAdminMonthlyTaskPayloadFromForm() {
  const draftTasks = (adminDailyTaskState.draftTasks || [])
    .map(item => ({
      judul: normalizeTaskText(item.judul),
      deskripsi: normalizeTaskText(item.deskripsi),
      frekuensi: ['harian', 'bulanan', 'pekanan'].includes(String(item.frekuensi || ''))
        ? String(item.frekuensi)
        : 'harian'
    }))
    .filter(item => item.judul)

  const dedupMap = new Map()
  draftTasks.forEach(item => {
    const key = `${item.judul.toLowerCase()}__${item.frekuensi}`
    if (!dedupMap.has(key)) dedupMap.set(key, item)
  })

  return Array.from(dedupMap.values())
}

async function getActiveTahunAjaranAdminDailyTask() {
  const { data, error } = await sb
    .from('tahun_ajaran')
    .select('id, nama')
    .eq('aktif', true)
    .order('id', { ascending: false })
    .limit(1)

  if (error) {
    console.error(error)
    return null
  }
  return data?.[0] || null
}

async function loadAdminDailyTaskData(periode) {
  const range = getMonthRange(periode)
  if (!range) throw new Error('Periode tidak valid')
  let academicHolidayDates = new Set()
  try {
    academicHolidayDates = await getAdminTaskAcademicHolidayDates(range.start, range.end)
  } catch (error) {
    console.warn('Gagal memuat libur akademik untuk mutabaah admin.', error)
  }

  const tahunAjaran = await getActiveTahunAjaranAdminDailyTask()
  const tahunAjaranId = tahunAjaran?.id || null

  let templateQuery = sb
    .from(DAILY_TASK_TEMPLATE_TABLE)
    .select('id, tahun_ajaran_id, tanggal, judul, deskripsi, aktif')
    .gte('tanggal', range.start)
    .lte('tanggal', range.end)
    .order('tanggal', { ascending: true })
    .order('created_at', { ascending: true })
  if (tahunAjaranId) templateQuery = templateQuery.eq('tahun_ajaran_id', tahunAjaranId)

  const [templateRes, submissionRes, guruRes] = await Promise.all([
    templateQuery,
    sb
      .from(DAILY_TASK_SUBMIT_TABLE)
      .select('id, template_id, guru_id, tanggal, status, catatan, submitted_at, updated_at')
      .gte('tanggal', range.start)
      .lte('tanggal', range.end),
    sb
      .from('karyawan')
      .select('id, nama, role, aktif')
      .eq('aktif', true)
      .order('nama')
  ])

  if (templateRes.error || submissionRes.error || guruRes.error) {
    const firstError = templateRes.error || submissionRes.error || guruRes.error
    throw firstError
  }

  const guruList = (guruRes.data || []).filter(item => {
    const roles = String(item.role || '').toLowerCase()
    return roles.includes('guru')
  })

  return {
    tahunAjaran,
    templates: (templateRes.data || []).filter(item => {
      const tanggal = String(item?.tanggal || '').slice(0, 10)
      return !isAhadDate(tanggal) && !academicHolidayDates.has(tanggal)
    }),
    submissions: (submissionRes.data || []).filter(item => !academicHolidayDates.has(String(item?.tanggal || '').slice(0, 10))),
    guruList
  }
}

function renderAdminDailyTaskTemplateList() {
  const box = document.getElementById('th-admin-template-list')
  if (!box) return
  const rows = adminDailyTaskState.templates || []

  if (!rows.length) {
    box.innerHTML = '<div style="color:#64748b;">Belum ada template tugas pada periode ini.</div>'
    adminDailyTaskState.templateGroups = []
    return
  }

  const groupMap = new Map()
  rows.forEach(item => {
    const judul = String(item.judul || '').trim()
    const deskripsi = String(item.deskripsi || '').trim()
    const key = `${judul}__${deskripsi}`
    if (!groupMap.has(key)) {
      groupMap.set(key, {
        key,
        judul,
        deskripsi,
        totalHari: 0,
        aktifHari: 0,
        ids: [],
        tanggalSet: new Set()
      })
    }
    const g = groupMap.get(key)
    g.totalHari += 1
    if (item.aktif !== false) g.aktifHari += 1
    g.ids.push(String(item.id || ''))
    if (item.tanggal) g.tanggalSet.add(String(item.tanggal))
  })

  const groups = Array.from(groupMap.values())
    .sort((a, b) => a.judul.localeCompare(b.judul, undefined, { sensitivity: 'base' }))
    .map(group => ({
      ...group,
      tanggalList: Array.from(group.tanggalSet).sort()
    }))
  adminDailyTaskState.templateGroups = groups

  let html = `
    <div style="overflow:auto;">
      <table style="width:100%; border-collapse:collapse; font-size:13px;">
        <thead>
          <tr style="background:#f8fafc;">
            <th style="padding:8px; border:1px solid #e2e8f0; width:120px;">Periode</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Judul</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Deskripsi</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:130px;">Hari Terpasang</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:90px;">Status</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:130px;">Aksi</th>
          </tr>
        </thead>
        <tbody>
  `

  html += groups.map((item, index) => `
    <tr>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(adminDailyTaskState.periode || '-')}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.judul || '-')}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.deskripsi || '-')}</td>
      <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${item.aktifHari}/${item.totalHari}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${item.aktifHari > 0 ? 'Aktif' : 'Nonaktif'}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">
        <button type="button" class="th-admin-btn th-admin-btn-danger" onclick="deleteAdminMonthlyTaskGroup(${index})">Hapus</button>
      </td>
    </tr>
  `).join('')

  html += '</tbody></table></div>'
  box.innerHTML = html
}

function renderAdminDailyTaskRekap() {
  const box = document.getElementById('th-admin-rekap-list')
  if (!box) return

  const templates = adminDailyTaskState.templates || []
  const submissions = adminDailyTaskState.submissions || []
  const guruList = adminDailyTaskState.guruList || []

  if (!guruList.length) {
    box.innerHTML = '<div style="color:#64748b;">Data guru tidak ditemukan.</div>'
    return
  }

  const templateIds = new Set(templates.filter(t => t.aktif !== false).map(t => String(t.id)))
  const totalTask = templateIds.size

  let html = `
    <div style="overflow:auto;">
      <table style="width:100%; border-collapse:collapse; font-size:13px;">
        <thead>
          <tr style="background:#f8fafc;">
            <th style="padding:8px; border:1px solid #e2e8f0; width:60px;">No</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Nama Guru</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:130px;">Selesai/Total</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:90px;">%</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:110px;">Aksi</th>
          </tr>
        </thead>
        <tbody>
  `

  html += guruList.map((guru, index) => {
    const sid = String(guru.id || '')
    const doneSet = new Set(
      submissions
        .filter(row => String(row.guru_id || '') === sid && templateIds.has(String(row.template_id || '')) && String(row.status || '').toLowerCase() === 'selesai')
        .map(row => String(row.template_id))
    )
    const done = doneSet.size
    const pct = totalTask > 0 ? Math.round((done / totalTask) * 100) : 0

    return `
      <tr>
        <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${index + 1}</td>
        <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(guru.nama || '-')}</td>
        <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${done}/${totalTask}</td>
        <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${pct}%</td>
        <td style="padding:8px; border:1px solid #e2e8f0;">
          <button type="button" class="th-admin-btn th-admin-btn-primary" onclick="openAdminDailyTaskGuruDetail('${escapeHtml(sid)}')">Detail</button>
        </td>
      </tr>
    `
  }).join('')

  html += '</tbody></table></div>'
  box.innerHTML = html
}

function renderAdminDailyTaskGuruDetail() {
  const box = document.getElementById('th-admin-detail-list')
  const title = document.getElementById('th-admin-detail-title')
  if (!box || !title) return

  const guruId = String(adminDailyTaskState.selectedGuruId || '')
  if (!guruId) {
    title.textContent = 'Pilih guru dari tabel rekap'
    box.innerHTML = 'Pilih guru dari tabel rekap.'
    return
  }

  const guru = (adminDailyTaskState.guruList || []).find(item => String(item.id) === guruId)
  const templates = (adminDailyTaskState.templates || []).filter(item => item.aktif !== false)
  const submissions = adminDailyTaskState.submissions || []
  const templateMap = new Map(templates.map(item => [String(item.id), item]))

  const rows = submissions
    .filter(item => String(item.guru_id || '') === guruId && templateMap.has(String(item.template_id || '')))
    .slice()
    .sort((a, b) => {
      const dateCmp = String(a.tanggal || '').localeCompare(String(b.tanggal || ''))
      if (dateCmp !== 0) return dateCmp
      return String(a.submitted_at || '').localeCompare(String(b.submitted_at || ''))
    })

  title.textContent = `Riwayat: ${guru?.nama || '-'}`

  if (!rows.length) {
    box.innerHTML = '<div style="color:#64748b;">Belum ada submit tugas pada periode ini.</div>'
    return
  }

  let html = `
    <div style="overflow:auto;">
      <table style="width:100%; border-collapse:collapse; font-size:13px;">
        <thead>
          <tr style="background:#f8fafc;">
            <th style="padding:8px; border:1px solid #e2e8f0; width:120px;">Tanggal</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Tugas</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:100px;">Status</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Catatan</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:160px;">Submit</th>
          </tr>
        </thead>
        <tbody>
  `

  html += rows.map(item => {
    const task = templateMap.get(String(item.template_id || ''))
    return `
      <tr>
        <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.tanggal || '-')}</td>
        <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(task?.judul || '-')}</td>
        <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(pickStatusLabel(item.status))}</td>
        <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.catatan || '-')}</td>
        <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.submitted_at ? String(item.submitted_at).replace('T', ' ').slice(0, 16) : '-')}</td>
      </tr>
    `
  }).join('')

  html += '</tbody></table></div>'
  box.innerHTML = html
}

function openAdminDailyTaskGuruDetail(guruId) {
  adminDailyTaskState.selectedGuruId = String(guruId || '')
  renderAdminDailyTaskGuruDetail()
}

async function deleteAdminDailyTaskTemplate(templateId) {
  const confirmed = typeof showPopupConfirm === 'function'
    ? await showPopupConfirm('Hapus template tugas ini?')
    : confirm('Hapus template tugas ini?')
  if (!confirmed) return

  const { error } = await sb
    .from(DAILY_TASK_TEMPLATE_TABLE)
    .delete()
    .eq('id', templateId)

  if (error) {
    console.error(error)
    alert(`Gagal menghapus template tugas: ${error.message || 'Unknown error'}`)
    return
  }

  await loadAdminDailyTaskPage(true)
}

async function deleteAdminMonthlyTaskGroup(groupIndex) {
  const group = (adminDailyTaskState.templateGroups || [])[Number(groupIndex)]
  if (!group) return

  const confirmed = typeof showPopupConfirm === 'function'
    ? await showPopupConfirm(`Hapus set bulanan untuk "${group.judul}" pada periode ${adminDailyTaskState.periode}?`)
    : confirm(`Hapus set bulanan untuk "${group.judul}" pada periode ${adminDailyTaskState.periode}?`)
  if (!confirmed) return

  const ids = (group.ids || []).filter(Boolean)
  if (!ids.length) return

  const { error } = await sb
    .from(DAILY_TASK_TEMPLATE_TABLE)
    .delete()
    .in('id', ids)

  if (error) {
    console.error(error)
    alert(`Gagal menghapus set bulanan: ${error.message || 'Unknown error'}`)
    return
  }

  await loadAdminDailyTaskPage(true)
}

async function saveAdminMonthlyTaskTemplate() {
  const periode = String(document.getElementById('th-admin-periode')?.value || adminDailyTaskState.periode || '').trim()
  const taskItems = collectAdminMonthlyTaskPayloadFromForm()

  if (!periode || !taskItems.length) {
    alert('Periode dan minimal satu judul tugas wajib diisi.')
    return
  }

  const monthRange = getMonthRange(periode)
  if (!monthRange) {
    alert('Periode tidak valid.')
    return
  }

  const tahunAjaran = await getActiveTahunAjaranAdminDailyTask()
  const tahunAjaranId = tahunAjaran?.id || null
  const range = { start: monthRange.start, end: monthRange.end }
  let existingSet = new Set()
  if (range) {
    let existingQuery = sb
      .from(DAILY_TASK_TEMPLATE_TABLE)
      .select('tanggal, judul')
      .gte('tanggal', range.start)
      .lte('tanggal', range.end)
    if (tahunAjaranId) existingQuery = existingQuery.eq('tahun_ajaran_id', tahunAjaranId)
    const existingRes = await existingQuery
    if (existingRes.error) {
      console.error(existingRes.error)
      alert(`Gagal validasi template bulanan: ${existingRes.error.message || 'Unknown error'}`)
      return
    }
    existingSet = new Set((existingRes.data || []).map(item => `${item.tanggal}__${normalizeTaskText(item.judul).toLowerCase()}`))
  }

  const payload = []
  taskItems.forEach(item => {
    const dateList = getDateListByTaskFrequency(periode, item.frekuensi)
    dateList.forEach(tanggal => {
      const key = `${tanggal}__${item.judul.toLowerCase()}`
      if (existingSet.has(key)) return
      payload.push({
        tahun_ajaran_id: tahunAjaranId,
        tanggal,
        judul: item.judul,
        deskripsi: item.deskripsi || null,
        aktif: true
      })
      existingSet.add(key)
    })
  })

  if (!payload.length) {
    alert('Semua tugas pada periode ini sudah tersedia.')
    return
  }

  const { error } = await sb
    .from(DAILY_TASK_TEMPLATE_TABLE)
    .upsert(payload, {
      onConflict: 'tahun_ajaran_id,tanggal,judul',
      ignoreDuplicates: true
    })

  if (error) {
    console.error(error)
    alert(`Gagal menyimpan template tugas: ${error.message || 'Unknown error'}`)
    return
  }

  clearAdminMonthlyTaskDraft()
  closeAdminTaskDraftModal()
  alert(`Berhasil set tugas untuk ${payload.length} data.`)
  await loadAdminDailyTaskPage(true)
}

async function setAdminDailyTaskToNextMonth() {
  const periode = String(document.getElementById('th-admin-periode')?.value || adminDailyTaskState.periode || '').trim()
  if (!periode) {
    alert('Periode belum dipilih.')
    return
  }

  const nextPeriode = getNextPeriode(periode)
  if (!nextPeriode) {
    alert('Periode tidak valid.')
    return
  }

  const groups = adminDailyTaskState.templateGroups || []
  if (!groups.length) {
    alert('Belum ada set tugas pada bulan ini untuk disalin.')
    return
  }

  const confirmed = typeof showPopupConfirm === 'function'
    ? await showPopupConfirm(`Salin semua set tugas dari ${periode} ke ${nextPeriode}?`)
    : confirm(`Salin semua set tugas dari ${periode} ke ${nextPeriode}?`)
  if (!confirmed) return

  const tahunAjaran = await getActiveTahunAjaranAdminDailyTask()
  const tahunAjaranId = tahunAjaran?.id || null
  const nextRange = getMonthRange(nextPeriode)
  let existingRows = []
  if (nextRange) {
    let query = sb
      .from(DAILY_TASK_TEMPLATE_TABLE)
      .select('tanggal, judul')
      .gte('tanggal', nextRange.start)
      .lte('tanggal', nextRange.end)
    if (tahunAjaranId) query = query.eq('tahun_ajaran_id', tahunAjaranId)
    const res = await query
    if (res.error) {
      console.error(res.error)
      alert(`Gagal validasi bulan berikutnya: ${res.error.message || 'Unknown error'}`)
      return
    }
    existingRows = res.data || []
  }

  const existingKey = new Set(existingRows.map(item => `${item.tanggal}__${String(item.judul || '').trim().toLowerCase()}`))
  const payload = []

  groups.forEach(group => {
    const sourceDates = Array.isArray(group.tanggalList) && group.tanggalList.length
      ? group.tanggalList
      : []
    const targetDates = sourceDates
      .map(src => shiftDateToNextMonth(src))
      .map(dateText => shiftToNonAhad(dateText))
      .filter(dateText => String(dateText || '').startsWith(`${nextPeriode}-`))
      .filter(Boolean)

    targetDates.forEach(tanggal => {
      const key = `${tanggal}__${String(group.judul || '').trim().toLowerCase()}`
      if (existingKey.has(key)) return
      payload.push({
        tahun_ajaran_id: tahunAjaranId,
        tanggal,
        judul: group.judul,
        deskripsi: group.deskripsi || null,
        aktif: true
      })
      existingKey.add(key)
    })
  })

  if (!payload.length) {
    alert('Data bulan berikutnya sudah tersedia semua.')
    return
  }

  const { error } = await sb
    .from(DAILY_TASK_TEMPLATE_TABLE)
    .upsert(payload, {
      onConflict: 'tahun_ajaran_id,tanggal,judul',
      ignoreDuplicates: true
    })
  if (error) {
    console.error(error)
    alert(`Gagal set ke bulan berikutnya: ${error.message || 'Unknown error'}`)
    return
  }

  document.getElementById('th-admin-periode').value = nextPeriode
  adminDailyTaskState.periode = nextPeriode
  alert(`Berhasil menyalin ${payload.length} data ke ${nextPeriode}.`)
  await loadAdminDailyTaskPage(true)
}

async function loadAdminDailyTaskPage(forceRefresh = false) {
  const periodeEl = document.getElementById('th-admin-periode')
  if (!periodeEl) return

  const periode = String(periodeEl.value || adminDailyTaskState.periode || getTodayMonth())
  periodeEl.value = periode
  adminDailyTaskState.periode = periode

  if (!getMonthRange(periode)) {
    alert('Periode tidak valid.')
    return
  }

  const templateBox = document.getElementById('th-admin-template-list')
  const rekapBox = document.getElementById('th-admin-rekap-list')
  if (templateBox) templateBox.innerHTML = 'Loading...'
  if (rekapBox) rekapBox.innerHTML = 'Loading...'

  try {
    const payload = await loadAdminDailyTaskData(periode)
    adminDailyTaskState.tahunAjaran = payload.tahunAjaran
    adminDailyTaskState.templates = payload.templates
    adminDailyTaskState.submissions = payload.submissions
    adminDailyTaskState.guruList = payload.guruList
    if (forceRefresh) adminDailyTaskState.selectedGuruId = ''

    renderAdminDailyTaskTemplateList()
    renderAdminDailyTaskRekap()
    renderAdminDailyTaskGuruDetail()
  } catch (error) {
    console.error(error)
    if (isMissingDailyTaskTableError(error)) {
      alert(getAdminDailyTaskMissingTableMessage())
      if (templateBox) templateBox.innerHTML = 'Tabel tugas harian belum tersedia.'
      if (rekapBox) rekapBox.innerHTML = 'Tabel tugas harian belum tersedia.'
      return
    }

    const msg = error?.message || 'Unknown error'
    if (templateBox) templateBox.innerHTML = `Gagal load data tugas harian: ${escapeHtml(msg)}`
    if (rekapBox) rekapBox.innerHTML = `Gagal load rekap: ${escapeHtml(msg)}`
  }
}

function initTugasHarianAdminPage() {
  const periodeEl = document.getElementById('th-admin-periode')
  if (periodeEl && !periodeEl.value) periodeEl.value = getTodayMonth()
  if (periodeEl && !periodeEl.dataset.bound) {
    periodeEl.addEventListener('change', () => loadAdminDailyTaskPage(true))
    periodeEl.dataset.bound = 'true'
  }
  const trigger = document.getElementById('th-admin-input-trigger')
  if (trigger && !trigger.dataset.bound) {
    trigger.addEventListener('focus', () => openAdminTaskDraftModal())
    trigger.dataset.bound = 'true'
  }
  const backdrop = document.getElementById('th-admin-task-modal-backdrop')
  if (backdrop && !backdrop.dataset.bound) {
    backdrop.addEventListener('click', (event) => {
      if (event.target === backdrop) closeAdminTaskDraftModal()
    })
    backdrop.dataset.bound = 'true'
  }
  clearAdminMonthlyTaskDraft()
  loadAdminDailyTaskPage(true)
}

window.initTugasHarianAdminPage = initTugasHarianAdminPage
window.loadAdminDailyTaskPage = loadAdminDailyTaskPage
window.saveAdminMonthlyTaskTemplate = saveAdminMonthlyTaskTemplate
window.setAdminDailyTaskToNextMonth = setAdminDailyTaskToNextMonth
window.deleteAdminDailyTaskTemplate = deleteAdminDailyTaskTemplate
window.deleteAdminMonthlyTaskGroup = deleteAdminMonthlyTaskGroup
window.openAdminDailyTaskGuruDetail = openAdminDailyTaskGuruDetail
window.addAdminMonthlyTaskDraft = addAdminMonthlyTaskDraft
window.updateAdminMonthlyTaskDraft = updateAdminMonthlyTaskDraft
window.removeAdminMonthlyTaskDraft = removeAdminMonthlyTaskDraft
window.clearAdminMonthlyTaskDraft = clearAdminMonthlyTaskDraft
window.openAdminTaskDraftModal = openAdminTaskDraftModal
window.closeAdminTaskDraftModal = closeAdminTaskDraftModal
window.addAdminTaskModalRow = addAdminTaskModalRow
window.updateAdminTaskModalRow = updateAdminTaskModalRow
window.updateAdminTaskModalFrequency = updateAdminTaskModalFrequency
window.removeAdminTaskModalRow = removeAdminTaskModalRow
window.submitAdminTaskDraftModal = submitAdminTaskDraftModal
