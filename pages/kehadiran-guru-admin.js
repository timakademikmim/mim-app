const KG_ADMIN_STATE = {
  periode: '',
  summaryRows: [],
  detailByGuru: new Map(),
  selectedGuruId: '',
  penggantiRows: [],
  roleMode: 'guru',
  debugMode: false
}
const KG_KALENDER_TABLE = 'kalender_akademik'
const KG_PERIZINAN_TABLE = 'izin_karyawan'
const KG_LIBUR_SEMUA = 'libur_semua_kegiatan'
const KG_LIBUR_AKADEMIK = 'libur_akademik'

function normalizeKgKalenderType(value) {
  const raw = String(value || '').trim().toLowerCase()
  if (raw === KG_LIBUR_SEMUA) return KG_LIBUR_SEMUA
  if (raw === KG_LIBUR_AKADEMIK) return KG_LIBUR_AKADEMIK
  return ''
}

function inferKgKalenderType(row) {
  const direct = normalizeKgKalenderType(row?.jenis_kegiatan)
  if (direct) return direct
  const text = `${String(row?.judul || '')} ${String(row?.detail || '')}`.toLowerCase()
  if (text.includes('libur semua')) return KG_LIBUR_SEMUA
  if (text.includes('libur akademik')) return KG_LIBUR_AKADEMIK
  return ''
}

function isKgMissingKalenderTypeColumnError(error) {
  const msg = String(error?.message || '').toLowerCase()
  return msg.includes('jenis_kegiatan') && (msg.includes('schema cache') || msg.includes('column') || msg.includes('does not exist'))
}

function getKgDateRangeKeys(startValue, endValue) {
  const startText = normalizeKgDateText(startValue)
  const endText = normalizeKgDateText(endValue || startValue)
  if (!startText) return []
  const start = new Date(`${startText}T00:00:00`)
  const end = new Date(`${endText}T00:00:00`)
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) return [startText]
  const result = []
  const cursor = new Date(start)
  while (cursor.getTime() <= end.getTime()) {
    result.push(formatKgLocalDate(cursor))
    cursor.setDate(cursor.getDate() + 1)
  }
  return result
}

async function getKgAcademicHolidayDateSet(startDate, endDate) {
  const withType = await sb
    .from(KG_KALENDER_TABLE)
    .select('id, judul, detail, jenis_kegiatan, mulai, selesai')
  let rows = []
  if (!withType.error) {
    rows = withType.data || []
  } else if (isKgMissingKalenderTypeColumnError(withType.error)) {
    const fallback = await sb
      .from(KG_KALENDER_TABLE)
      .select('id, judul, detail, mulai, selesai')
    if (fallback.error) throw fallback.error
    rows = fallback.data || []
  } else {
    const msg = String(withType.error?.message || '').toLowerCase()
    if (msg.includes(KG_KALENDER_TABLE) && (msg.includes('does not exist') || msg.includes('schema cache'))) return new Set()
    throw withType.error
  }

  const dateSet = new Set()
  ;(rows || []).forEach(item => {
    const type = inferKgKalenderType(item)
    if (type !== KG_LIBUR_SEMUA && type !== KG_LIBUR_AKADEMIK) return
    const keys = getKgDateRangeKeys(item?.mulai, item?.selesai || item?.mulai)
    keys.forEach(key => {
      if (key >= startDate && key <= endDate) dateSet.add(key)
    })
  })
  return dateSet
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
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  return `${year}-${month}`
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

function normalizeKgDateText(value) {
  return String(value || '').slice(0, 10)
}

function formatKgLocalDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function getKgDatesByHariInRange(range, hari) {
  const target = normalizeKgHari(hari)
  if (!target) return []
  const rows = []
  let cursor = new Date(`${range.start}T00:00:00`)
  const end = new Date(`${range.end}T00:00:00`)
  while (cursor <= end) {
    const text = formatKgLocalDate(cursor)
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

function getKgJamMatchKey(jamMulai, jamSelesai, jamPelajaranId) {
  const byRange = getKgJamKey(jamMulai, jamSelesai)
  if (byRange) return byRange
  const byId = String(jamPelajaranId || '').trim()
  if (byId) return `JP:${byId}`
  return ''
}

function getKgJamLabel(jamMulai, jamSelesai) {
  const start = String(jamMulai || '').slice(0, 5)
  const end = String(jamSelesai || '').slice(0, 5)
  if (start && end) return `${start}-${end}`
  return start || end || '-'
}

function normalizeKgLookupLabel(value) {
  return String(value || '')
    .trim()
    .toLowerCase()
    .replace(/\s+/g, ' ')
}

function summarizeKgDebugRows(rows = []) {
  const list = Array.isArray(rows) ? rows : []
  const rowIds = new Set()
  const santriIds = new Set()
  const guruIds = new Set()
  const kelasIds = new Set()
  const mapelIds = new Set()
  const distribusiIds = new Set()
  const semesterIds = new Set()
  const jamIds = new Set()
  const statuses = new Set()

  list.forEach(row => {
    const rowId = String(row?.id || '').trim()
    const santriId = String(row?.santri_id || '').trim()
    const guruId = String(row?.guru_id || '').trim()
    const kelasId = String(row?.kelas_id || '').trim()
    const mapelId = String(row?.mapel_id || '').trim()
    const distribusiId = String(row?.distribusi_id || '').trim()
    const semesterId = String(row?.semester_id || '').trim()
    const jamId = String(row?.jam_pelajaran_id || '').trim()
    const status = String(row?.status || '').trim()

    if (rowId) rowIds.add(rowId)
    if (santriId) santriIds.add(santriId)
    if (guruId) guruIds.add(guruId)
    if (kelasId) kelasIds.add(kelasId)
    if (mapelId) mapelIds.add(mapelId)
    if (distribusiId) distribusiIds.add(distribusiId)
    if (semesterId) semesterIds.add(semesterId)
    if (jamId) jamIds.add(jamId)
    if (status) statuses.add(status)
  })

  return {
    rowCount: rowIds.size || list.length,
    santriCount: santriIds.size,
    guruIds: Array.from(guruIds),
    kelasIds: Array.from(kelasIds),
    mapelIds: Array.from(mapelIds),
    distribusiIds: Array.from(distribusiIds),
    semesterIds: Array.from(semesterIds),
    jamIds: Array.from(jamIds),
    statuses: Array.from(statuses)
  }
}

function buildKgMissingPenggantiColumnsMessage() {
  return `Kolom guru pengganti belum tersedia di tabel 'absensi_santri'.\n\nJalankan SQL berikut di Supabase:\n\nalter table public.absensi_santri\n  add column if not exists guru_pengganti_id uuid null,\n  add column if not exists keterangan_pengganti text null;`
}

function isKgMissingPenggantiColumnError(error) {
  const msg = String(error?.message || '').toLowerCase()
  return msg.includes('guru_pengganti_id') || msg.includes('keterangan_pengganti')
}

function isKgMissingPerizinanTableError(error) {
  const code = String(error?.code || '').toUpperCase()
  const msg = String(error?.message || '').toLowerCase()
  return code === '42P01' || (msg.includes(KG_PERIZINAN_TABLE) && (msg.includes('does not exist') || msg.includes('schema cache')))
}

function buildKgIzinReasonText(item) {
  const keperluan = String(item?.keperluan || '').trim()
  const catatan = String(item?.catatan_wakasek || '').trim()
  if (keperluan && catatan) return `${keperluan} | Catatan wakasek: ${catatan}`
  if (keperluan) return keperluan
  if (catatan) return `Catatan wakasek: ${catatan}`
  return 'Izin disetujui wakasek'
}

function buildKgApprovedIzinLookup(rows, startDate, endDate) {
  const map = new Map()
  ;(rows || []).forEach(item => {
    const guruId = String(item?.guru_id || '').trim()
    if (!guruId) return
    const keys = getKgDateRangeKeys(item?.tanggal_mulai, item?.tanggal_selesai || item?.tanggal_mulai)
    keys.forEach(dateKey => {
      if (dateKey < startDate || dateKey > endDate) return
      const key = `${guruId}|${dateKey}`
      if (map.has(key)) return
      map.set(key, {
        reason: buildKgIzinReasonText(item)
      })
    })
  })
  return map
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


function buildKgSessionAggMaps(absensiRows, jamMap, distribusiMap = new Map()) {
  const exactMap = new Map()
  const genericMap = new Map()
  const distribusiExactMap = new Map()
  const distribusiGenericMap = new Map()
  const guruJamMap = new Map()
  const guruDayMap = new Map()
  const broadMap = new Map()
  const broadNoSemMap = new Map()
  const distribusiSessionSetMap = new Map()
  const genericSessionSetMap = new Map()
  const guruJamSessionSetMap = new Map()
  const guruDaySessionSetMap = new Map()
  const broadSessionSetMap = new Map()
  const broadNoSemSessionSetMap = new Map()
  const genericSessionOnlyNoJam = new Map()

  ;(absensiRows || []).forEach(row => {
    const tanggal = String(row.tanggal || '').slice(0, 10)
    const distribusiId = String(row.distribusi_id || '')
    const distribusi = distribusiMap.get(String(row.distribusi_id || '')) || null
    const kelasId = String(row.kelas_id || distribusi?.kelas_id || '')
    const mapelId = String(row.mapel_id || distribusi?.mapel_id || '')
    const guruId = String(row.guru_id || distribusi?.guru_id || '')
    const semesterId = String(row.semester_id || distribusi?.semester_id || '')
    if (!tanggal || !kelasId || !mapelId) return

    const jamData = row.jam_pelajaran_id ? jamMap.get(String(row.jam_pelajaran_id)) : null
    const jamKey = getKgJamMatchKey(jamData?.jam_mulai, jamData?.jam_selesai, row.jam_pelajaran_id)

    const keyExact = `${tanggal}|${kelasId}|${mapelId}|${guruId}|${jamKey}`
    const keyGeneric = `${tanggal}|${kelasId}|${mapelId}|${guruId}`
    const keyDistribusiExact = `${tanggal}|${distribusiId}|${jamKey}`
    const keyDistribusiGeneric = `${tanggal}|${distribusiId}`
    const keyGuruJam = `${tanggal}|${guruId}|${jamKey}`
    const keyGuruDay = `${tanggal}|${guruId}`
    const keyBroad = `${tanggal}|${kelasId}|${mapelId}|${semesterId}`
    const keyBroadNoSem = `${tanggal}|${kelasId}|${mapelId}`

    const apply = (map, key) => {
      if (!map.has(key)) {
        map.set(key, {
          penggantiIds: new Set(),
          notes: new Set(),
          rowIds: new Set(),
          guruIds: new Set(),
          kelasIds: new Set(),
          mapelIds: new Set(),
          distribusiIds: new Set(),
          semesterIds: new Set(),
          jamIds: new Set()
        })
      }
      const item = map.get(key)
      const penggantiId = String(row.guru_pengganti_id || '').trim()
      if (penggantiId) item.penggantiIds.add(penggantiId)
      const note = String(row.keterangan_pengganti || '').trim()
      if (note) item.notes.add(note)
      const rowId = String(row.id || '').trim()
      if (rowId) item.rowIds.add(rowId)
      if (guruId) item.guruIds.add(guruId)
      if (kelasId) item.kelasIds.add(kelasId)
      if (mapelId) item.mapelIds.add(mapelId)
      if (distribusiId) item.distribusiIds.add(distribusiId)
      if (semesterId) item.semesterIds.add(semesterId)
      if (row.jam_pelajaran_id) item.jamIds.add(String(row.jam_pelajaran_id))
    }

    if (guruId) {
      apply(genericMap, keyGeneric)
      apply(exactMap, keyExact)
      apply(guruJamMap, keyGuruJam)
      apply(guruDayMap, keyGuruDay)
    }
    if (distribusiId) {
      apply(distribusiExactMap, keyDistribusiExact)
      apply(distribusiGenericMap, keyDistribusiGeneric)
    }
    apply(broadMap, keyBroad)
    apply(broadNoSemMap, keyBroadNoSem)

    const marker = jamKey || '__NO_JAM__'
    if (distribusiId) {
      if (!distribusiSessionSetMap.has(keyDistribusiGeneric)) {
        distribusiSessionSetMap.set(keyDistribusiGeneric, new Set())
      }
      distribusiSessionSetMap.get(keyDistribusiGeneric).add(marker)
    }
    if (guruId) {
      if (!genericSessionSetMap.has(keyGeneric)) {
        genericSessionSetMap.set(keyGeneric, new Set())
        genericSessionOnlyNoJam.set(keyGeneric, true)
      }
      genericSessionSetMap.get(keyGeneric).add(marker)
      if (marker !== '__NO_JAM__') genericSessionOnlyNoJam.set(keyGeneric, false)

      if (!guruJamSessionSetMap.has(keyGuruJam)) {
        guruJamSessionSetMap.set(keyGuruJam, new Set())
      }
      guruJamSessionSetMap.get(keyGuruJam).add(marker)

      if (!guruDaySessionSetMap.has(keyGuruDay)) {
        guruDaySessionSetMap.set(keyGuruDay, new Set())
      }
      guruDaySessionSetMap.get(keyGuruDay).add(marker)
    }

    if (!broadSessionSetMap.has(keyBroad)) {
      broadSessionSetMap.set(keyBroad, new Set())
    }
    broadSessionSetMap.get(keyBroad).add(marker)

    if (!broadNoSemSessionSetMap.has(keyBroadNoSem)) {
      broadNoSemSessionSetMap.set(keyBroadNoSem, new Set())
    }
    broadNoSemSessionSetMap.get(keyBroadNoSem).add(marker)
  })

  const genericSessionCount = new Map()
  genericSessionSetMap.forEach((set, key) => {
    genericSessionCount.set(key, set.size)
  })

  const broadSessionCount = new Map()
  broadSessionSetMap.forEach((set, key) => {
    broadSessionCount.set(key, set.size)
  })

  const broadNoSemSessionCount = new Map()
  broadNoSemSessionSetMap.forEach((set, key) => {
    broadNoSemSessionCount.set(key, set.size)
  })

  const distribusiSessionCount = new Map()
  distribusiSessionSetMap.forEach((set, key) => {
    distribusiSessionCount.set(key, set.size)
  })

  const guruJamSessionCount = new Map()
  guruJamSessionSetMap.forEach((set, key) => {
    guruJamSessionCount.set(key, set.size)
  })

  const guruDaySessionCount = new Map()
  guruDaySessionSetMap.forEach((set, key) => {
    guruDaySessionCount.set(key, set.size)
  })

  return {
    exactMap,
    genericMap,
    distribusiExactMap,
    distribusiGenericMap,
    guruJamMap,
    guruDayMap,
    broadMap,
    broadNoSemMap,
    distribusiSessionCount,
    genericSessionCount,
    genericSessionOnlyNoJam,
    broadSessionCount,
    broadNoSemSessionCount,
    guruJamSessionCount,
    guruDaySessionCount
  }
}

async function loadKehadiranGuruAdminData(periode) {
  const range = getKgMonthRange(periode)
  if (!range) throw new Error('Periode tidak valid.')
  let academicHolidayDates = new Set()
  try {
    academicHolidayDates = await getKgAcademicHolidayDateSet(range.start, range.end)
  } catch (error) {
    console.warn('Gagal memuat libur akademik untuk kehadiran guru admin.', error)
  }

  const { semesterId } = await getKgActiveSemesterByTahunAktif()
  let approvedIzinRows = []
  try {
    const izinRes = await sb
      .from(KG_PERIZINAN_TABLE)
      .select('id, guru_id, tanggal_mulai, tanggal_selesai, keperluan, catatan_wakasek, status, created_at')
      .eq('status', 'diterima')
      .lte('tanggal_mulai', range.end)
      .gte('tanggal_selesai', range.start)
      .order('created_at', { ascending: false })
    if (izinRes.error) throw izinRes.error
    approvedIzinRows = izinRes.data || []
  } catch (error) {
    if (!isKgMissingPerizinanTableError(error)) {
      console.warn('Gagal memuat izin karyawan pada kehadiran admin.', error)
    }
  }

  const [distribusiRes, jadwalRes, absensiRes, kelasRes, mapelRes, karyawanRes, jamRes] = await Promise.all([
    sb.from('distribusi_mapel')
      .select('id, kelas_id, mapel_id, guru_id, semester_id'),
    getKgJadwalRows(),
    sb.from('absensi_santri')
      .select('id, tanggal, kelas_id, mapel_id, guru_id, jam_pelajaran_id, distribusi_id, semester_id, santri_id, status, guru_pengganti_id, keterangan_pengganti')
      .gte('tanggal', range.start)
      .lte('tanggal', range.end),
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

  const distribusiAll = distribusiRes.data || []
  const distribusiAllMap = new Map(distribusiAll.map(item => [String(item?.id || ''), item]))
  const absensiRows = (absensiRes.data || []).filter(item => !academicHolidayDates.has(normalizeKgDateText(item?.tanggal)))
  const inferredSemesterIds = [...new Set(
    absensiRows
      .map(item => String(item?.semester_id || '').trim())
      .filter(Boolean)
  )]
  const inferredSemesterIdsFromDistribusi = [...new Set(
    absensiRows
      .map(item => distribusiAllMap.get(String(item?.distribusi_id || '')))
      .map(item => String(item?.semester_id || '').trim())
      .filter(Boolean)
  )]
  const inferredSemesterIdsFromKelasMapel = [...new Set(
    absensiRows.flatMap(item => {
      const kelasId = String(item?.kelas_id || '').trim()
      const mapelId = String(item?.mapel_id || '').trim()
      const guruId = String(item?.guru_id || '').trim()
      if (!kelasId || !mapelId) return []

      const candidates = distribusiAll.filter(dist => {
        if (String(dist?.kelas_id || '').trim() !== kelasId) return false
        if (String(dist?.mapel_id || '').trim() !== mapelId) return false
        if (!guruId) return true
        return String(dist?.guru_id || '').trim() === guruId
      })
      return candidates
        .map(dist => String(dist?.semester_id || '').trim())
        .filter(Boolean)
    })
  )]
  const mergedInferredSemesterIds = [...new Set([
    ...inferredSemesterIds,
    ...inferredSemesterIdsFromDistribusi,
    ...inferredSemesterIdsFromKelasMapel
  ])]
  const targetSemesterIds = mergedInferredSemesterIds.length
    ? mergedInferredSemesterIds
    : [String(semesterId || '').trim()].filter(Boolean)

  const distribusiList = targetSemesterIds.length
    ? distribusiAll.filter(item => targetSemesterIds.includes(String(item?.semester_id || '').trim()))
    : distribusiAll
  const distribusiMap = new Map(distribusiList.map(item => [String(item.id), item]))
  const kelasMap = new Map((kelasRes.data || []).map(item => [String(item.id), item]))
  const mapelMap = new Map((mapelRes.data || []).map(item => [String(item.id), item]))
  const karyawanMap = new Map((karyawanRes.data || []).map(item => [String(item.id), item]))
  const jamMap = new Map((jamRes.data || []).map(item => [String(item.id), item]))

  const jadwalList = (jadwalRes.data || []).filter(item => distribusiMap.has(String(item.distribusi_id || '')))

  const expectedSessions = []
  jadwalList.forEach(jadwal => {
    const distribusi = distribusiMap.get(String(jadwal.distribusi_id || ''))
    if (!distribusi) return

    const jamData = jadwal.jam_pelajaran_id ? jamMap.get(String(jadwal.jam_pelajaran_id)) : null
    const jamMulai = jamData?.jam_mulai || jadwal.jam_mulai
    const jamSelesai = jamData?.jam_selesai || jadwal.jam_selesai
    const jamKey = getKgJamMatchKey(jamMulai, jamSelesai, jadwal.jam_pelajaran_id)

    const dates = getKgDatesByHariInRange(range, jadwal.hari)
    dates.forEach(tanggal => {
      if (academicHolidayDates.has(tanggal)) return
      expectedSessions.push({
        tanggal,
        hari: getKgHariLabel(jadwal.hari),
        guru_id: String(distribusi.guru_id || ''),
        kelas_id: String(distribusi.kelas_id || ''),
        mapel_id: String(distribusi.mapel_id || ''),
        semester_id: String(distribusi.semester_id || ''),
        distribusi_id: String(distribusi.id || ''),
        jam_key: jamKey,
        jam_label: getKgJamLabel(jamMulai, jamSelesai)
      })
    })
  })

  const {
    exactMap,
    genericMap,
    distribusiExactMap,
    distribusiGenericMap,
    guruJamMap,
    guruDayMap,
    broadMap,
    broadNoSemMap,
    distribusiSessionCount,
    genericSessionCount,
    genericSessionOnlyNoJam,
    guruJamSessionCount,
    guruDaySessionCount
  } = buildKgSessionAggMaps(absensiRows, jamMap, distribusiAllMap)
  const approvedIzinLookup = buildKgApprovedIzinLookup(approvedIzinRows, range.start, range.end)
  const summaryByGuru = new Map()
  const detailByGuru = new Map()
  const penggantiCountMap = new Map()
  const penggantiByGuruMap = new Map()
  const labelFallbackMap = new Map()
  const broadLabelFallbackMap = new Map()
  const debugRowsByDate = new Map()
  const debugRowsByGuruDay = new Map()
  const debugRowsByClassMapelDay = new Map()
  const debugRowsByDistribusiDay = new Map()
  const distribusiGenericRemainingCount = new Map(distribusiSessionCount)
  const guruJamRemainingCount = new Map(guruJamSessionCount)
  const guruDayRemainingCount = new Map(guruDaySessionCount)

  const applyLabelFallback = (map, key, agg) => {
    if (!key || !agg) return
    if (!map.has(key)) map.set(key, agg)
  }

  ;(absensiRows || []).forEach(row => {
    const tanggal = String(row.tanggal || '').slice(0, 10)
    const distribusi = distribusiAllMap.get(String(row.distribusi_id || '')) || null
    const distribusiId = String(row.distribusi_id || distribusi?.id || '')
    const guruId = String(row.guru_id || distribusi?.guru_id || '')
    const kelasId = String(row.kelas_id || distribusi?.kelas_id || '')
    const mapelId = String(row.mapel_id || distribusi?.mapel_id || '')
    const kelasLabel = normalizeKgLookupLabel(kelasMap.get(kelasId)?.nama_kelas || '')
    const mapelLabel = normalizeKgLookupLabel(mapelMap.get(mapelId)?.nama || '')
    if (!tanggal || !kelasLabel || !mapelLabel) return
    const broadKey = `${tanggal}|${kelasLabel}|${mapelLabel}`
    const agg =
      (guruId ? genericMap.get(`${tanggal}|${kelasId}|${mapelId}|${guruId}`) : null)
      || broadNoSemMap.get(`${tanggal}|${kelasId}|${mapelId}`)
      || null
    if (guruId) {
      const key = `${tanggal}|${guruId}|${kelasLabel}|${mapelLabel}`
      applyLabelFallback(labelFallbackMap, key, agg)
    }
    applyLabelFallback(broadLabelFallbackMap, broadKey, agg)

    const guruDayKey = guruId ? `${tanggal}|${guruId}` : ''
    const classMapelDayKey = `${tanggal}|${kelasId}|${mapelId}`
    const distribusiDayKey = distribusiId ? `${tanggal}|${distribusiId}` : ''
    if (!debugRowsByDate.has(tanggal)) debugRowsByDate.set(tanggal, [])
    debugRowsByDate.get(tanggal).push(row)
    if (guruDayKey) {
      if (!debugRowsByGuruDay.has(guruDayKey)) debugRowsByGuruDay.set(guruDayKey, [])
      debugRowsByGuruDay.get(guruDayKey).push(row)
    }
    if (kelasId && mapelId) {
      if (!debugRowsByClassMapelDay.has(classMapelDayKey)) debugRowsByClassMapelDay.set(classMapelDayKey, [])
      debugRowsByClassMapelDay.get(classMapelDayKey).push(row)
    }
    if (distribusiDayKey) {
      if (!debugRowsByDistribusiDay.has(distribusiDayKey)) debugRowsByDistribusiDay.set(distribusiDayKey, [])
      debugRowsByDistribusiDay.get(distribusiDayKey).push(row)
    }
  })

  const sessionsByGeneric = new Map()
  const expectedCountByGuruDay = new Map()
  const expectedGenericGroupSetByGuruDay = new Map()
  expectedSessions.forEach(session => {
    const keyGeneric = `${session.tanggal}|${session.kelas_id}|${session.mapel_id}|${session.guru_id}`
    if (!sessionsByGeneric.has(keyGeneric)) sessionsByGeneric.set(keyGeneric, [])
    sessionsByGeneric.get(keyGeneric).push(session)
    const keyGuruDay = `${session.tanggal}|${session.guru_id}`
    expectedCountByGuruDay.set(keyGuruDay, Number(expectedCountByGuruDay.get(keyGuruDay) || 0) + 1)
    if (!expectedGenericGroupSetByGuruDay.has(keyGuruDay)) {
      expectedGenericGroupSetByGuruDay.set(keyGuruDay, new Set())
    }
    expectedGenericGroupSetByGuruDay.get(keyGuruDay).add(keyGeneric)
  })

  sessionsByGeneric.forEach((sessionList, keyGeneric) => {
    const orderedSessions = [...sessionList].sort((a, b) => String(a.jam_label || '').localeCompare(String(b.jam_label || '')))
    const totalSubmitted = Number(genericSessionCount.get(keyGeneric) || 0)

    const exactHitFlags = orderedSessions.map(session => {
      const keyExact = `${session.tanggal}|${session.kelas_id}|${session.mapel_id}|${session.guru_id}|${session.jam_key}`
      return exactMap.has(keyExact)
    })
    const exactHitCount = exactHitFlags.filter(Boolean).length
    const fillAllSessionsFromGeneric = genericSessionOnlyNoJam.get(keyGeneric) === true && totalSubmitted > 0
    let remainingFallbackSlots = fillAllSessionsFromGeneric
      ? Math.max(0, orderedSessions.length - exactHitCount)
      : Math.max(0, totalSubmitted - exactHitCount)

    orderedSessions.forEach(session => {
      const guruId = String(session.guru_id || '')
      if (!guruId) return
      const guruNama = String(karyawanMap.get(guruId)?.nama || '-')
      let matchSource = 'none'

      const keyExact = `${session.tanggal}|${session.kelas_id}|${session.mapel_id}|${session.guru_id}|${session.jam_key}`
      const exactAgg = exactMap.get(keyExact) || null
      let agg = exactAgg
      if (agg) matchSource = 'exact'
      if (!agg && remainingFallbackSlots > 0) {
        const fallbackAgg = genericMap.get(keyGeneric) || null
        if (fallbackAgg) {
          agg = fallbackAgg
          matchSource = 'generic'
          remainingFallbackSlots -= 1
        }
      }
      if (!agg) {
        const keyDistribusiExact = `${session.tanggal}|${session.distribusi_id || ''}|${session.jam_key}`
        const keyDistribusiGeneric = `${session.tanggal}|${session.distribusi_id || ''}`
        const distribusiExactAgg = session.distribusi_id ? (distribusiExactMap.get(keyDistribusiExact) || null) : null
        if (distribusiExactAgg) {
          agg = distribusiExactAgg
          matchSource = 'distribusi-exact'
          const remaining = Number(distribusiGenericRemainingCount.get(keyDistribusiGeneric) || 0)
          if (remaining > 0) distribusiGenericRemainingCount.set(keyDistribusiGeneric, remaining - 1)
        }
      }
      if (!agg) {
        const keyDistribusiGeneric = `${session.tanggal}|${session.distribusi_id || ''}`
        const remaining = Number(distribusiGenericRemainingCount.get(keyDistribusiGeneric) || 0)
        if (session.distribusi_id && remaining > 0) {
          const distribusiGenericAgg = distribusiGenericMap.get(keyDistribusiGeneric) || null
          if (distribusiGenericAgg) {
            agg = distribusiGenericAgg
            matchSource = 'distribusi-generic'
            distribusiGenericRemainingCount.set(keyDistribusiGeneric, remaining - 1)
          }
        }
      }
      if (!agg) {
        const keyGuruJam = `${session.tanggal}|${session.guru_id}|${session.jam_key}`
        const guruJamRemaining = Number(guruJamRemainingCount.get(keyGuruJam) || 0)
        if (guruJamRemaining > 0) {
          const guruJamAgg = guruJamMap.get(keyGuruJam) || null
          if (guruJamAgg) {
            agg = guruJamAgg
            matchSource = 'guru-jam'
            guruJamRemainingCount.set(keyGuruJam, guruJamRemaining - 1)
          }
        }
      }
      if (!agg) {
        const keyGuruDay = `${session.tanggal}|${session.guru_id}`
        const expectedOnDay = Number(expectedCountByGuruDay.get(keyGuruDay) || 0)
        const expectedGenericGroupCountOnDay = Number(expectedGenericGroupSetByGuruDay.get(keyGuruDay)?.size || 0)
        const guruDayRemaining = Number(guruDayRemainingCount.get(keyGuruDay) || 0)
        if ((expectedOnDay === 1 || expectedGenericGroupCountOnDay === 1) && guruDayRemaining > 0) {
          const guruDayAgg = guruDayMap.get(keyGuruDay) || null
          if (guruDayAgg) {
            agg = guruDayAgg
            matchSource = 'guru-day'
            guruDayRemainingCount.set(keyGuruDay, guruDayRemaining - 1)
          }
        }
      }
      if (!agg) {
        const broadKey = `${session.tanggal}|${session.kelas_id}|${session.mapel_id}|${session.semester_id || ''}`
        const broadAgg = broadMap.get(broadKey) || null
        if (broadAgg) {
          agg = broadAgg
          matchSource = 'broad'
        }
      }
      if (!agg) {
        const broadNoSemKey = `${session.tanggal}|${session.kelas_id}|${session.mapel_id}`
        const broadNoSemAgg = broadNoSemMap.get(broadNoSemKey) || null
        if (broadNoSemAgg) {
          agg = broadNoSemAgg
          matchSource = 'broad-no-sem'
        }
      }
      if (!agg) {
        const kelasLabel = normalizeKgLookupLabel(kelasMap.get(session.kelas_id)?.nama_kelas || '')
        const mapelLabel = normalizeKgLookupLabel(mapelMap.get(session.mapel_id)?.nama || '')
        const labelKey = `${session.tanggal}|${session.guru_id}|${kelasLabel}|${mapelLabel}`
        const labelAgg = labelFallbackMap.get(labelKey) || null
        if (labelAgg) {
          agg = labelAgg
          matchSource = 'label-guru'
        }
      }
      if (!agg) {
        const kelasLabel = normalizeKgLookupLabel(kelasMap.get(session.kelas_id)?.nama_kelas || '')
        const mapelLabel = normalizeKgLookupLabel(mapelMap.get(session.mapel_id)?.nama || '')
        const broadLabelKey = `${session.tanggal}|${kelasLabel}|${mapelLabel}`
        const broadLabelAgg = broadLabelFallbackMap.get(broadLabelKey) || null
        if (broadLabelAgg) {
          agg = broadLabelAgg
          matchSource = 'label-broad'
        }
      }

      const penggantiIds = agg ? Array.from(agg.penggantiIds) : []
      const notes = agg ? Array.from(agg.notes) : []
      const hasPenggantiSignal = penggantiIds.length > 0 || notes.length > 0
      const izinInfo = approvedIzinLookup.get(`${guruId}|${session.tanggal}`) || null
      const status = izinInfo
        ? 'Izin'
        : (!agg ? 'Tidak Masuk' : (hasPenggantiSignal ? 'Diganti' : 'Masuk'))

      if (!summaryByGuru.has(guruId)) {
        summaryByGuru.set(guruId, {
          guru_id: guruId,
          nama: guruNama,
          role: String(karyawanMap.get(guruId)?.role || ''),
          total_sesi: 0,
          masuk: 0,
          izin: 0,
          diganti: 0,
          tidak_masuk: 0
        })
      }
      const sum = summaryByGuru.get(guruId)
      sum.total_sesi += 1
      if (status === 'Masuk') sum.masuk += 1
      else if (status === 'Izin') sum.izin += 1
      else sum.tidak_masuk += 1
      if (hasPenggantiSignal) sum.diganti += 1

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
        keterangan: status === 'Izin'
          ? String(izinInfo?.reason || 'Izin disetujui wakasek')
          : (notes.join(' | ') || '-'),
        debug: {
          match_source: matchSource,
          expected: {
            guru_id: session.guru_id || '-',
            kelas_id: session.kelas_id || '-',
            mapel_id: session.mapel_id || '-',
            distribusi_id: session.distribusi_id || '-',
            semester_id: session.semester_id || '-',
            jam_key: session.jam_key || '-'
          },
          matched: agg ? {
            row_ids: Array.from(agg.rowIds || []),
            guru_ids: Array.from(agg.guruIds || []),
            kelas_ids: Array.from(agg.kelasIds || []),
            mapel_ids: Array.from(agg.mapelIds || []),
            distribusi_ids: Array.from(agg.distribusiIds || []),
            semester_ids: Array.from(agg.semesterIds || []),
            jam_ids: Array.from(agg.jamIds || [])
          } : null,
          candidates: {
            date_all: summarizeKgDebugRows(debugRowsByDate.get(session.tanggal) || []),
            guru_day: summarizeKgDebugRows(debugRowsByGuruDay.get(`${session.tanggal}|${session.guru_id}`) || []),
            class_mapel_day: summarizeKgDebugRows(debugRowsByClassMapelDay.get(`${session.tanggal}|${session.kelas_id}|${session.mapel_id}`) || []),
            distribusi_day: summarizeKgDebugRows(debugRowsByDistribusiDay.get(`${session.tanggal}|${session.distribusi_id || ''}`) || [])
          }
        }
      })

      if (hasPenggantiSignal) {
        const sessionKey = `${session.tanggal}|${session.kelas_id}|${session.mapel_id}|${session.guru_id}|${session.jam_key}`
        penggantiIds.forEach(pid => {
          if (!pid) return
          if (!summaryByGuru.has(pid)) {
            summaryByGuru.set(pid, {
              guru_id: pid,
              nama: String(karyawanMap.get(pid)?.nama || pid),
              role: String(karyawanMap.get(pid)?.role || ''),
              total_sesi: 0,
              masuk: 0,
              izin: 0,
              diganti: 0,
              tidak_masuk: 0
            })
          }
          const penggantiSummary = summaryByGuru.get(pid)
          penggantiSummary.total_sesi += 1
          penggantiSummary.masuk += 1

          if (!detailByGuru.has(pid)) detailByGuru.set(pid, [])
          detailByGuru.get(pid).push({
            tanggal: session.tanggal,
            hari: session.hari,
            kelas: String(kelasMap.get(session.kelas_id)?.nama_kelas || '-'),
            mapel: String(mapelMap.get(session.mapel_id)?.nama || '-'),
            jam: session.jam_label || '-',
            status: 'Masuk',
            pengganti: '-',
            keterangan: `Menggantikan ${guruNama}${notes.length ? ` | ${notes.join(' | ')}` : ''}`
          })

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
            <th style="padding:8px; border:1px solid #e2e8f0; width:90px;">Izin</th>
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
      <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${row.izin || 0}</td>
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
            ${KG_ADMIN_STATE.debugMode ? '<th style="padding:8px; border:1px solid #e2e8f0; min-width:280px;">Debug Match</th>' : ''}
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
      ${KG_ADMIN_STATE.debugMode ? `
        <td style="padding:8px; border:1px solid #e2e8f0; font-size:11px; line-height:1.5; white-space:pre-wrap;">
          ${escapeHtml(
            [
              `source: ${item?.debug?.match_source || 'none'}`,
              `expected guru=${item?.debug?.expected?.guru_id || '-'} kelas=${item?.debug?.expected?.kelas_id || '-'} mapel=${item?.debug?.expected?.mapel_id || '-'} distribusi=${item?.debug?.expected?.distribusi_id || '-'} sem=${item?.debug?.expected?.semester_id || '-'} jam=${item?.debug?.expected?.jam_key || '-'}`,
              item?.debug?.matched
                ? `matched guru=${(item.debug.matched.guru_ids || []).join(',') || '-'} kelas=${(item.debug.matched.kelas_ids || []).join(',') || '-'} mapel=${(item.debug.matched.mapel_ids || []).join(',') || '-'} distribusi=${(item.debug.matched.distribusi_ids || []).join(',') || '-'} sem=${(item.debug.matched.semester_ids || []).join(',') || '-'} jam=${(item.debug.matched.jam_ids || []).join(',') || '-'}`
                : 'matched: -',
              `candidate date-all rows=${item?.debug?.candidates?.date_all?.rowCount || 0} santri=${item?.debug?.candidates?.date_all?.santriCount || 0} guru=${(item?.debug?.candidates?.date_all?.guruIds || []).join(',') || '-'} kelas=${(item?.debug?.candidates?.date_all?.kelasIds || []).join(',') || '-'} mapel=${(item?.debug?.candidates?.date_all?.mapelIds || []).join(',') || '-'} distribusi=${(item?.debug?.candidates?.date_all?.distribusiIds || []).join(',') || '-'} jam=${(item?.debug?.candidates?.date_all?.jamIds || []).join(',') || '-'} status=${(item?.debug?.candidates?.date_all?.statuses || []).join(',') || '-'}`,
              `candidate guru-day rows=${item?.debug?.candidates?.guru_day?.rowCount || 0} santri=${item?.debug?.candidates?.guru_day?.santriCount || 0} kelas=${(item?.debug?.candidates?.guru_day?.kelasIds || []).join(',') || '-'} mapel=${(item?.debug?.candidates?.guru_day?.mapelIds || []).join(',') || '-'} distribusi=${(item?.debug?.candidates?.guru_day?.distribusiIds || []).join(',') || '-'} jam=${(item?.debug?.candidates?.guru_day?.jamIds || []).join(',') || '-'} status=${(item?.debug?.candidates?.guru_day?.statuses || []).join(',') || '-'}`,
              `candidate kelas-mapel-day rows=${item?.debug?.candidates?.class_mapel_day?.rowCount || 0} santri=${item?.debug?.candidates?.class_mapel_day?.santriCount || 0} guru=${(item?.debug?.candidates?.class_mapel_day?.guruIds || []).join(',') || '-'} distribusi=${(item?.debug?.candidates?.class_mapel_day?.distribusiIds || []).join(',') || '-'} jam=${(item?.debug?.candidates?.class_mapel_day?.jamIds || []).join(',') || '-'} status=${(item?.debug?.candidates?.class_mapel_day?.statuses || []).join(',') || '-'}`,
              `candidate distribusi-day rows=${item?.debug?.candidates?.distribusi_day?.rowCount || 0} santri=${item?.debug?.candidates?.distribusi_day?.santriCount || 0} guru=${(item?.debug?.candidates?.distribusi_day?.guruIds || []).join(',') || '-'} kelas=${(item?.debug?.candidates?.distribusi_day?.kelasIds || []).join(',') || '-'} mapel=${(item?.debug?.candidates?.distribusi_day?.mapelIds || []).join(',') || '-'} jam=${(item?.debug?.candidates?.distribusi_day?.jamIds || []).join(',') || '-'} status=${(item?.debug?.candidates?.distribusi_day?.statuses || []).join(',') || '-'}`
            ].join('\n')
          )}
        </td>
      ` : ''}
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

function toggleKehadiranGuruAdminDebugMode() {
  KG_ADMIN_STATE.debugMode = !KG_ADMIN_STATE.debugMode
  const btn = document.getElementById('kg-toggle-debug')
  if (btn) btn.textContent = KG_ADMIN_STATE.debugMode ? 'Sembunyikan Debug' : 'Tampilkan Debug'
  renderKehadiranGuruDetail()
}

window.initKehadiranGuruAdminPage = initKehadiranGuruAdminPage
window.loadKehadiranGuruAdminPage = loadKehadiranGuruAdminPage
window.openKehadiranGuruDetail = openKehadiranGuruDetail
window.setKehadiranKaryawanMode = setKehadiranKaryawanMode
window.toggleKehadiranGuruAdminDebugMode = toggleKehadiranGuruAdminDebugMode
