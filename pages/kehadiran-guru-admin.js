const KG_ADMIN_STATE = {
  periode: '',
  summaryRows: [],
  detailByGuru: new Map(),
  selectedGuruId: '',
  penggantiRows: [],
  roleMode: 'guru'
}

function parseKgRoleList(rawRole) {
  if (Array.isArray(rawRole)) {
    return rawRole
      .map(value => String(value || '').trim().toLowerCase())
      .filter(Boolean)
  }
  return String(rawRole || '')
    .split(/[,\|;]+/)
    .map(value => value.trim().toLowerCase())
    .filter(Boolean)
}

function getKgRoleModeLabel(mode = KG_ADMIN_STATE.roleMode) {
  if (mode === 'muhaffiz') return 'Muhaffiz'
  if (mode === 'musyrif') return 'Musyrif'
  return 'Guru'
}

function isKgRoleMatch(rawRole, mode = KG_ADMIN_STATE.roleMode) {
  const roles = parseKgRoleList(rawRole)
  return roles.includes(String(mode || '').toLowerCase())
}

function getKgFilteredSummaryRows() {
  return KG_ADMIN_STATE.summaryRows || []
}

function getKgFilteredPenggantiRows() {
  return KG_ADMIN_STATE.penggantiRows || []
}

function isKgGuruMode() {
  return String(KG_ADMIN_STATE.roleMode || 'guru') === 'guru'
}

function renderKgModeView() {
  const placeholder = document.getElementById('kg-mode-placeholder')
  const placeholderTitle = document.getElementById('kg-mode-placeholder-title')
  const placeholderBody = document.getElementById('kg-mode-placeholder-body')
  const guruLayout = document.getElementById('kg-guru-layout')

  if (!placeholder || !guruLayout) return

  if (isKgGuruMode()) {
    placeholder.style.display = 'none'
    guruLayout.style.display = 'block'
    return
  }

  const label = getKgRoleModeLabel()
  placeholder.style.display = 'block'
  guruLayout.style.display = 'none'
  if (placeholderTitle) placeholderTitle.textContent = `Kehadiran ${label}`
  if (placeholderBody) placeholderBody.textContent = `Fitur kehadiran ${label.toLowerCase()} belum dikembangkan.`
}

function renderKehadiranKaryawanModeButtons() {
  const modes = ['guru', 'muhaffiz', 'musyrif']
  modes.forEach(mode => {
    const btn = document.getElementById(`kg-mode-${mode}`)
    if (!btn) return
    btn.classList.toggle('active', KG_ADMIN_STATE.roleMode === mode)
  })
}

function setKehadiranKaryawanMode(mode) {
  const normalized = String(mode || '').toLowerCase()
  if (!['guru', 'muhaffiz', 'musyrif'].includes(normalized)) return
  KG_ADMIN_STATE.roleMode = normalized

  if (!isKgGuruMode()) {
    KG_ADMIN_STATE.selectedGuruId = ''
  } else {
    const filteredRows = getKgFilteredSummaryRows()
    const selectedStillVisible = filteredRows.some(row => String(row.guru_id || '') === String(KG_ADMIN_STATE.selectedGuruId || ''))
    if (!selectedStillVisible) {
      KG_ADMIN_STATE.selectedGuruId = ''
    }
  }

  renderKehadiranKaryawanModeButtons()
  renderKgModeView()
  if (!isKgGuruMode()) {
    return
  }
  renderKehadiranGuruSummary()
  renderKehadiranGuruDetail()
  renderKehadiranGuruPengganti()
}

function getKgTodayMonth() {
  return new Date().toISOString().slice(0, 7)
}

function getKgMonthRange(periode) {
  const text = String(periode || '').trim()
  if (!/^\d{4}-\d{2}$/.test(text)) return null
  const [yearText, monthText] = text.split('-')
  const year = Number(yearText)
  const month = Number(monthText)
  if (!Number.isFinite(year) || !Number.isFinite(month) || month < 1 || month > 12) return null
  const start = `${yearText}-${monthText}-01`
  const endDate = new Date(year, month, 0)
  const end = `${yearText}-${monthText}-${String(endDate.getDate()).padStart(2, '0')}`
  return { start, end, year, month }
}

function normalizeKgHari(raw) {
  return String(raw || '').trim().toLowerCase()
}

function getKgHariLabel(raw) {
  const value = normalizeKgHari(raw)
  const map = {
    senin: 'Senin',
    selasa: 'Selasa',
    rabu: 'Rabu',
    kamis: 'Kamis',
    jumat: 'Jumat',
    sabtu: 'Sabtu',
    minggu: 'Minggu'
  }
  return map[value] || (raw || '-')
}

function getKgHariFromDate(dateText) {
  const date = new Date(`${dateText}T00:00:00`)
  const names = ['minggu', 'senin', 'selasa', 'rabu', 'kamis', 'jumat', 'sabtu']
  return names[date.getDay()] || ''
}

function getKgDatesByHariInRange(range, hari) {
  const target = normalizeKgHari(hari)
  if (!target) return []
  const rows = []
  let cursor = new Date(`${range.start}T00:00:00`)
  const end = new Date(`${range.end}T00:00:00`)
  while (cursor <= end) {
    const text = cursor.toISOString().slice(0, 10)
    if (getKgHariFromDate(text) === target) rows.push(text)
    cursor.setDate(cursor.getDate() + 1)
  }
  return rows
}

function getKgJamKey(jamMulai, jamSelesai) {
  const start = String(jamMulai || '').slice(0, 5)
  const end = String(jamSelesai || '').slice(0, 5)
  if (!start && !end) return ''
  return `${start}|${end}`
}

function getKgJamLabel(jamMulai, jamSelesai) {
  const start = String(jamMulai || '').slice(0, 5)
  const end = String(jamSelesai || '').slice(0, 5)
  if (start && end) return `${start}-${end}`
  return start || end || '-'
}

function buildKgMissingPenggantiColumnsMessage() {
  return `Kolom guru pengganti belum tersedia di tabel 'absensi_santri'.\n\nJalankan SQL berikut di Supabase:\n\nalter table public.absensi_santri\n  add column if not exists guru_pengganti_id uuid null,\n  add column if not exists keterangan_pengganti text null;`
}

function isKgMissingPenggantiColumnError(error) {
  const msg = String(error?.message || '').toLowerCase()
  return msg.includes('guru_pengganti_id') || msg.includes('keterangan_pengganti')
}

async function getKgJadwalRows() {
  const withJamRef = await sb
    .from('jadwal_pelajaran')
    .select('id, distribusi_id, hari, jam_mulai, jam_selesai, jam_pelajaran_id')
  if (!withJamRef.error) return withJamRef

  const msg = String(withJamRef.error?.message || '').toLowerCase()
  if (!msg.includes('jam_pelajaran_id')) return withJamRef

  return await sb
    .from('jadwal_pelajaran')
    .select('id, distribusi_id, hari, jam_mulai, jam_selesai')
}

async function getKgActiveSemesterByTahunAktif() {
  const { data: tahunData, error: tahunError } = await sb
    .from('tahun_ajaran')
    .select('id, nama')
    .eq('aktif', true)
    .order('id', { ascending: false })
    .limit(1)
  if (tahunError) throw tahunError

  const tahunAjaranId = String(tahunData?.[0]?.id || '')
  if (!tahunAjaranId) return { tahunAjaranId: '', semesterId: '' }

  const { data: semData, error: semError } = await sb
    .from('semester')
    .select('id, aktif, tahun_ajaran_id')
    .eq('tahun_ajaran_id', tahunAjaranId)
    .order('id', { ascending: false })
  if (semError) throw semError

  const asBool = value => {
    if (value === true || value === 1) return true
    const text = String(value ?? '').trim().toLowerCase()
    return text === 'true' || text === 't' || text === '1' || text === 'yes'
  }

  const semesterAktif = (semData || []).find(item => asBool(item?.aktif)) || semData?.[0] || null
  return { tahunAjaranId, semesterId: String(semesterAktif?.id || '') }
}

function buildKgSessionAggMaps(absensiRows, jamMap) {
  const exactMap = new Map()
  const genericMap = new Map()

  ;(absensiRows || []).forEach(row => {
    const tanggal = String(row.tanggal || '').slice(0, 10)
    const kelasId = String(row.kelas_id || '')
    const mapelId = String(row.mapel_id || '')
    const guruId = String(row.guru_id || '')
    if (!tanggal || !kelasId || !mapelId || !guruId) return

    const jamData = row.jam_pelajaran_id ? jamMap.get(String(row.jam_pelajaran_id)) : null
    const jamKey = getKgJamKey(jamData?.jam_mulai, jamData?.jam_selesai)

    const keyExact = `${tanggal}|${kelasId}|${mapelId}|${guruId}|${jamKey}`
    const keyGeneric = `${tanggal}|${kelasId}|${mapelId}|${guruId}`

    const apply = (map, key) => {
      if (!map.has(key)) {
        map.set(key, {
          penggantiIds: new Set(),
          notes: new Set()
        })
      }
      const item = map.get(key)
      const penggantiId = String(row.guru_pengganti_id || '').trim()
      if (penggantiId) item.penggantiIds.add(penggantiId)
      const note = String(row.keterangan_pengganti || '').trim()
      if (note) item.notes.add(note)
    }

    apply(genericMap, keyGeneric)
    apply(exactMap, keyExact)
  })

  return { exactMap, genericMap }
}

async function loadKehadiranGuruAdminData(periode) {
  const range = getKgMonthRange(periode)
  if (!range) throw new Error('Periode tidak valid.')

  const { semesterId } = await getKgActiveSemesterByTahunAktif()
  if (!semesterId) return { summaryRows: [], detailByGuru: new Map(), penggantiRows: [] }

  const [distribusiRes, jadwalRes, absensiRes, kelasRes, mapelRes, karyawanRes, jamRes] = await Promise.all([
    sb.from('distribusi_mapel')
      .select('id, kelas_id, mapel_id, guru_id, semester_id')
      .eq('semester_id', semesterId),
    getKgJadwalRows(),
    sb.from('absensi_santri')
      .select('id, tanggal, kelas_id, mapel_id, guru_id, jam_pelajaran_id, distribusi_id, guru_pengganti_id, keterangan_pengganti')
      .gte('tanggal', range.start)
      .lte('tanggal', range.end)
      .eq('semester_id', semesterId),
    sb.from('kelas').select('id, nama_kelas'),
    sb.from('mapel').select('id, nama'),
    sb.from('karyawan').select('id, nama, aktif, role'),
    sb.from('jam_pelajaran').select('id, jam_mulai, jam_selesai')
  ])

  const firstError = distribusiRes.error || jadwalRes.error || absensiRes.error || kelasRes.error || mapelRes.error || karyawanRes.error || jamRes.error
  if (firstError) {
    if (isKgMissingPenggantiColumnError(firstError)) {
      throw new Error(buildKgMissingPenggantiColumnsMessage())
    }
    throw firstError
  }

  const distribusiList = distribusiRes.data || []
  const distribusiMap = new Map(distribusiList.map(item => [String(item.id), item]))
  const kelasMap = new Map((kelasRes.data || []).map(item => [String(item.id), item]))
  const mapelMap = new Map((mapelRes.data || []).map(item => [String(item.id), item]))
  const karyawanMap = new Map((karyawanRes.data || []).map(item => [String(item.id), item]))
  const jamMap = new Map((jamRes.data || []).map(item => [String(item.id), item]))

  const jadwalList = (jadwalRes.data || []).filter(item => distribusiMap.has(String(item.distribusi_id || '')))
  const absensiRows = absensiRes.data || []

  const expectedSessions = []
  jadwalList.forEach(jadwal => {
    const distribusi = distribusiMap.get(String(jadwal.distribusi_id || ''))
    if (!distribusi) return

    const jamData = jadwal.jam_pelajaran_id ? jamMap.get(String(jadwal.jam_pelajaran_id)) : null
    const jamMulai = jamData?.jam_mulai || jadwal.jam_mulai
    const jamSelesai = jamData?.jam_selesai || jadwal.jam_selesai
    const jamKey = getKgJamKey(jamMulai, jamSelesai)

    const dates = getKgDatesByHariInRange(range, jadwal.hari)
    dates.forEach(tanggal => {
      expectedSessions.push({
        tanggal,
        hari: getKgHariLabel(jadwal.hari),
        guru_id: String(distribusi.guru_id || ''),
        kelas_id: String(distribusi.kelas_id || ''),
        mapel_id: String(distribusi.mapel_id || ''),
        distribusi_id: String(distribusi.id || ''),
        jam_key: jamKey,
        jam_label: getKgJamLabel(jamMulai, jamSelesai)
      })
    })
  })

  const genericExpectedCount = new Map()
  expectedSessions.forEach(item => {
    const key = `${item.tanggal}|${item.kelas_id}|${item.mapel_id}|${item.guru_id}`
    genericExpectedCount.set(key, (genericExpectedCount.get(key) || 0) + 1)
  })

  const { exactMap, genericMap } = buildKgSessionAggMaps(absensiRows, jamMap)
  const summaryByGuru = new Map()
  const detailByGuru = new Map()
  const penggantiCountMap = new Map()
  const penggantiByGuruMap = new Map()

  expectedSessions.forEach(session => {
    const guruId = String(session.guru_id || '')
    if (!guruId) return
    const guruNama = String(karyawanMap.get(guruId)?.nama || '-')

    const keyExact = `${session.tanggal}|${session.kelas_id}|${session.mapel_id}|${session.guru_id}|${session.jam_key}`
    const keyGeneric = `${session.tanggal}|${session.kelas_id}|${session.mapel_id}|${session.guru_id}`
    const exactAgg = exactMap.get(keyExact) || null
    const genericAgg = genericExpectedCount.get(keyGeneric) === 1 ? (genericMap.get(keyGeneric) || null) : null
    const agg = exactAgg || genericAgg

    const penggantiIds = agg ? Array.from(agg.penggantiIds) : []
    const notes = agg ? Array.from(agg.notes) : []
    const status = !agg
      ? 'Tidak Masuk'
      : (penggantiIds.length ? 'Diganti' : 'Masuk')

    if (!summaryByGuru.has(guruId)) {
      summaryByGuru.set(guruId, {
        guru_id: guruId,
        nama: guruNama,
        role: String(karyawanMap.get(guruId)?.role || ''),
        total_sesi: 0,
        masuk: 0,
        diganti: 0,
        tidak_masuk: 0
      })
    }
    const sum = summaryByGuru.get(guruId)
    sum.total_sesi += 1
    if (status === 'Masuk') sum.masuk += 1
    else if (status === 'Diganti') sum.diganti += 1
    else sum.tidak_masuk += 1

    if (!detailByGuru.has(guruId)) detailByGuru.set(guruId, [])
    const penggantiNamaList = penggantiIds
      .map(id => String(karyawanMap.get(id)?.nama || id))
      .filter(Boolean)

    detailByGuru.get(guruId).push({
      tanggal: session.tanggal,
      hari: session.hari,
      kelas: String(kelasMap.get(session.kelas_id)?.nama_kelas || '-'),
      mapel: String(mapelMap.get(session.mapel_id)?.nama || '-'),
      jam: session.jam_label || '-',
      status,
      pengganti: penggantiNamaList.join(', ') || '-',
      keterangan: notes.join(' | ') || '-'
    })

    if (status === 'Diganti') {
      const sessionKey = `${session.tanggal}|${session.kelas_id}|${session.mapel_id}|${session.guru_id}|${session.jam_key}`
      penggantiIds.forEach(pid => {
        if (!penggantiCountMap.has(pid)) {
          penggantiCountMap.set(pid, new Set())
        }
        penggantiCountMap.get(pid).add(sessionKey)

        if (!penggantiByGuruMap.has(pid)) {
          penggantiByGuruMap.set(pid, new Map())
        }
        const byGuru = penggantiByGuruMap.get(pid)
        if (!byGuru.has(guruId)) {
          byGuru.set(guruId, new Set())
        }
        byGuru.get(guruId).add(sessionKey)
      })
    }
  })

  const summaryRows = Array.from(summaryByGuru.values())
    .sort((a, b) => String(a.nama || '').localeCompare(String(b.nama || ''), undefined, { sensitivity: 'base' }))

  detailByGuru.forEach(list => {
    list.sort((a, b) => {
      const dateCmp = String(a.tanggal).localeCompare(String(b.tanggal))
      if (dateCmp !== 0) return dateCmp
      return String(a.jam).localeCompare(String(b.jam))
    })
  })

  const penggantiRows = Array.from(penggantiCountMap.entries())
    .map(([id, set]) => {
      const byGuru = penggantiByGuruMap.get(id) || new Map()
      const gantiSiapa = Array.from(byGuru.entries())
        .map(([gid, sesSet]) => ({
          guru_id: gid,
          nama: String(karyawanMap.get(gid)?.nama || gid),
          total: sesSet.size
        }))
        .sort((a, b) => b.total - a.total || String(a.nama || '').localeCompare(String(b.nama || ''), undefined, { sensitivity: 'base' }))
      return {
        pengganti_id: id,
        nama: String(karyawanMap.get(id)?.nama || id),
        role: String(karyawanMap.get(id)?.role || ''),
        total_ganti: set.size,
        ganti_siapa: gantiSiapa
      }
    })
    .sort((a, b) => b.total_ganti - a.total_ganti || a.nama.localeCompare(b.nama, undefined, { sensitivity: 'base' }))

  return { summaryRows, detailByGuru, penggantiRows }
}

function renderKehadiranGuruSummary() {
  const box = document.getElementById('kg-summary')
  if (!box) return
  const rows = getKgFilteredSummaryRows()
  if (!rows.length) {
    box.innerHTML = '<div style="color:#64748b;">Belum ada data jadwal/kehadiran guru pada periode ini.</div>'
    return
  }

  let html = `
    <div style="overflow:auto;">
      <table style="width:100%; border-collapse:collapse; font-size:13px;">
        <thead>
          <tr style="background:#f8fafc;">
            <th style="padding:8px; border:1px solid #e2e8f0; width:60px;">No</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Guru</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:90px;">Total</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:90px;">Masuk</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:90px;">Diganti</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:110px;">Tidak Masuk</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:110px;">Aksi</th>
          </tr>
        </thead>
        <tbody>
  `

  html += rows.map((row, index) => `
    <tr>
      <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${index + 1}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(row.nama || '-')}</td>
      <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${row.total_sesi}</td>
      <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${row.masuk}</td>
      <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${row.diganti}</td>
      <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${row.tidak_masuk}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">
        <button type="button" class="kg-btn" onclick="openKehadiranGuruDetail('${escapeHtml(String(row.guru_id || ''))}')">Detail</button>
      </td>
    </tr>
  `).join('')

  html += '</tbody></table></div>'
  box.innerHTML = html
}

function renderKehadiranGuruDetail() {
  const box = document.getElementById('kg-detail')
  const title = document.getElementById('kg-detail-title')
  if (!box || !title) return

  const guruId = String(KG_ADMIN_STATE.selectedGuruId || '')
  if (!guruId) {
    title.textContent = 'Pilih guru dari tabel atas'
    box.innerHTML = 'Pilih guru dari tabel atas.'
    return
  }

  const guru = getKgFilteredSummaryRows().find(item => String(item.guru_id) === guruId)
  if (!guru) {
    title.textContent = 'Pilih guru dari tabel atas'
    box.innerHTML = 'Pilih guru dari tabel atas.'
    return
  }
  title.textContent = `Detail: ${guru?.nama || '-'}`

  const rows = KG_ADMIN_STATE.detailByGuru.get(guruId) || []
  if (!rows.length) {
    box.innerHTML = '<div style="color:#64748b;">Tidak ada detail sesi pada periode ini.</div>'
    return
  }

  let html = `
    <div style="overflow:auto;">
      <table style="width:100%; border-collapse:collapse; font-size:13px;">
        <thead>
          <tr style="background:#f8fafc;">
            <th style="padding:8px; border:1px solid #e2e8f0; width:110px;">Tanggal</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:90px;">Hari</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Kelas</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Mapel</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:110px;">Jam</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:100px;">Status</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Diganti Oleh</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Keterangan</th>
          </tr>
        </thead>
        <tbody>
  `

  html += rows.map(item => `
    <tr>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.tanggal)}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.hari)}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.kelas)}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.mapel)}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.jam)}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.status)}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.pengganti)}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.keterangan)}</td>
    </tr>
  `).join('')

  html += '</tbody></table></div>'
  box.innerHTML = html
}

function renderKehadiranGuruPengganti() {
  const box = document.getElementById('kg-pengganti')
  if (!box) return
  const rows = getKgFilteredPenggantiRows()
  if (!rows.length) {
    box.innerHTML = '<div style="color:#64748b;">Belum ada data guru pengganti pada periode ini.</div>'
    return
  }

  let html = `
    <div style="overflow:auto;">
      <table style="width:100%; border-collapse:collapse; font-size:13px;">
        <thead>
          <tr style="background:#f8fafc;">
            <th style="padding:8px; border:1px solid #e2e8f0; width:60px;">No</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Nama Karyawan Pengganti</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:150px;">Total Mengganti</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Menggantikan Siapa</th>
          </tr>
        </thead>
        <tbody>
  `

  html += rows.map((row, index) => `
    <tr>
      <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${index + 1}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(row.nama || '-')}</td>
      <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${row.total_ganti}</td>
      <td style="padding:8px; border:1px solid #e2e8f0;">${(row.ganti_siapa || []).map(item => `${escapeHtml(item.nama || '-')} (${item.total}x)`).join(' | ') || '-'}</td>
    </tr>
  `).join('')

  html += '</tbody></table></div>'
  box.innerHTML = html
}

function openKehadiranGuruDetail(guruId) {
  KG_ADMIN_STATE.selectedGuruId = String(guruId || '')
  renderKehadiranGuruDetail()
}

async function loadKehadiranGuruAdminPage(forceRefresh = false) {
  const periodeEl = document.getElementById('kg-periode')
  if (!periodeEl) return

  const periode = String(periodeEl.value || KG_ADMIN_STATE.periode || getKgTodayMonth())
  periodeEl.value = periode
  KG_ADMIN_STATE.periode = periode

  const summaryBox = document.getElementById('kg-summary')
  const detailBox = document.getElementById('kg-detail')
  const penggantiBox = document.getElementById('kg-pengganti')
  if (summaryBox) summaryBox.innerHTML = 'Loading...'
  if (detailBox) detailBox.innerHTML = 'Loading...'
  if (penggantiBox) penggantiBox.innerHTML = 'Loading...'

  try {
    const payload = await loadKehadiranGuruAdminData(periode)
    KG_ADMIN_STATE.summaryRows = payload.summaryRows
    KG_ADMIN_STATE.detailByGuru = payload.detailByGuru
    KG_ADMIN_STATE.penggantiRows = payload.penggantiRows
    if (forceRefresh) KG_ADMIN_STATE.selectedGuruId = ''
    renderKgModeView()
    if (!isKgGuruMode()) {
      return
    }
    renderKehadiranGuruSummary()
    renderKehadiranGuruDetail()
    renderKehadiranGuruPengganti()
  } catch (error) {
    console.error(error)
    const msg = String(error?.message || 'Unknown error')
    if (summaryBox) summaryBox.innerHTML = `Gagal load monitoring: ${escapeHtml(msg)}`
    if (detailBox) detailBox.innerHTML = `Gagal load detail: ${escapeHtml(msg)}`
    if (penggantiBox) penggantiBox.innerHTML = `Gagal load rekap pengganti: ${escapeHtml(msg)}`
  }
}

function initKehadiranGuruAdminPage() {
  renderKehadiranKaryawanModeButtons()
  renderKgModeView()
  const periodeEl = document.getElementById('kg-periode')
  if (periodeEl && !periodeEl.value) periodeEl.value = getKgTodayMonth()
  if (periodeEl && !periodeEl.dataset.bound) {
    periodeEl.addEventListener('change', () => loadKehadiranGuruAdminPage(true))
    periodeEl.dataset.bound = 'true'
  }
  loadKehadiranGuruAdminPage(true)
}

window.initKehadiranGuruAdminPage = initKehadiranGuruAdminPage
window.loadKehadiranGuruAdminPage = loadKehadiranGuruAdminPage
window.openKehadiranGuruDetail = openKehadiranGuruDetail
window.setKehadiranKaryawanMode = setKehadiranKaryawanMode
