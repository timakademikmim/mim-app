const STRUKTUR_SEKOLAH_TABLE = 'struktur_sekolah'
const STRUKTUR_SEKOLAH_CACHE_KEY = 'struktur_sekolah:row'
const STRUKTUR_SEKOLAH_CACHE_TTL_MS = 2 * 60 * 1000

let currentStrukturSekolahId = ''
let strukturSekolahHasNewColumns = null
let strukturSekolahKaryawanList = []

function ssEscapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

function getStrukturSekolahMissingTableMessage() {
  return `Tabel '${STRUKTUR_SEKOLAH_TABLE}' belum ada di Supabase.

Silakan jalankan SQL berikut:

create table if not exists public.${STRUKTUR_SEKOLAH_TABLE} (
  id uuid primary key default gen_random_uuid(),
  nama_sekolah text,
  alamat_sekolah text,
  kepala_sekolah text,
  wakasek_bidang_akademik text,
  wakasek_bidang_ketahfizan text,
  wakasek_bidang_kesantrian text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);`
}

function getStrukturSekolahMissingColumnsMessage() {
  return `Kolom wakasek terbaru belum ada di tabel '${STRUKTUR_SEKOLAH_TABLE}'.

Jalankan SQL ini:

alter table public.${STRUKTUR_SEKOLAH_TABLE}
  add column if not exists wakasek_bidang_akademik text,
  add column if not exists wakasek_bidang_ketahfizan text,
  add column if not exists wakasek_bidang_kesantrian text;`
}

function isStrukturSekolahMissingTableError(error) {
  const msg = String(error?.message || '').toLowerCase()
  const code = String(error?.code || '').toUpperCase()
  return (
    code === '42P01' ||
    msg.includes(`'${STRUKTUR_SEKOLAH_TABLE}'`) ||
    msg.includes(`relation \"${STRUKTUR_SEKOLAH_TABLE}\" does not exist`) ||
    (msg.includes('could not find the table') && msg.includes('schema cache')) ||
    msg.includes(`public.${STRUKTUR_SEKOLAH_TABLE}`)
  )
}

function setStrukturSekolahStatus(message, isError = false) {
  const el = document.getElementById('ss-status')
  if (!el) return
  el.style.color = isError ? '#991b1b' : '#64748b'
  el.innerHTML = isError ? ssEscapeHtml(message).replaceAll('\n', '<br>') : ssEscapeHtml(message)
}

function parseRoles(roleRaw) {
  return String(roleRaw || '')
    .split(/[;,]/)
    .map(part => part.trim().toLowerCase())
    .filter(Boolean)
}

function hasRole(item, roleName) {
  const target = String(roleName || '').trim().toLowerCase()
  if (!target) return false
  return parseRoles(item?.role).includes(target)
}

async function checkStrukturSekolahNewColumns() {
  if (strukturSekolahHasNewColumns !== null) return strukturSekolahHasNewColumns
  const { error } = await sb
    .from(STRUKTUR_SEKOLAH_TABLE)
    .select('id, wakasek_bidang_akademik')
    .limit(1)
  strukturSekolahHasNewColumns = !error
  return strukturSekolahHasNewColumns
}

function mapStrukturSekolahRowFromForm() {
  const namaSekolah = String(document.getElementById('ss-nama-sekolah')?.value || '').trim()
  const alamatSekolah = String(document.getElementById('ss-alamat-sekolah')?.value || '').trim()
  const wakasekAkademik = String(document.getElementById('ss-wakasek-akademik')?.value || '').trim()
  const wakasekKetahfizan = String(document.getElementById('ss-wakasek-ketahfizan')?.value || '').trim()
  const wakasekKesantrian = String(document.getElementById('ss-wakasek-kesantrian')?.value || '').trim()
  return {
    nama_sekolah: namaSekolah || null,
    alamat_sekolah: alamatSekolah || null,
    kepala_sekolah: String(document.getElementById('ss-kepala-sekolah')?.value || '').trim() || null,
    wakasek_bidang_akademik: wakasekAkademik || null,
    wakasek_bidang_ketahfizan: wakasekKetahfizan || null,
    wakasek_bidang_kesantrian: wakasekKesantrian || null
  }
}

function renderKaryawanSelect(selectId, placeholder) {
  const el = document.getElementById(selectId)
  if (!el) return
  const currentValue = String(el.value || '').trim()
  const options = (strukturSekolahKaryawanList || [])
    .map(item => {
      const nama = String(item?.nama || '').trim()
      if (!nama) return ''
      return `<option value="${ssEscapeHtml(nama)}">${ssEscapeHtml(nama)}</option>`
    })
    .filter(Boolean)
    .join('')
  el.innerHTML = `<option value="">${ssEscapeHtml(placeholder)}</option>${options}`
  if (currentValue) el.value = currentValue
}

function renderStrukturSekolahKaryawanOptions() {
  renderKaryawanSelect('ss-kepala-sekolah', '-- Pilih Kepala Sekolah --')
  renderKaryawanSelect('ss-wakasek-akademik', '-- Pilih Wakasek Akademik --')
  renderKaryawanSelect('ss-wakasek-ketahfizan', '-- Pilih Wakasek Ketahfizan --')
  renderKaryawanSelect('ss-wakasek-kesantrian', '-- Pilih Wakasek Kesantrian --')
}

async function loadStrukturSekolahKaryawanOptions() {
  const { data, error } = await sb
    .from('karyawan')
    .select('id, nama, role, aktif')
    .order('nama', { ascending: true })

  if (error) {
    console.error(error)
    strukturSekolahKaryawanList = []
    renderStrukturSekolahKaryawanOptions()
    return
  }

  strukturSekolahKaryawanList = (data || [])
    .filter(item => String(item?.aktif || '').toLowerCase() !== 'false')
    .filter(item => String(item?.nama || '').trim() !== '')
  renderStrukturSekolahKaryawanOptions()
}

function fillStrukturSekolahForm(row) {
  const data = row || {}
  const setValue = (id, value) => {
    const el = document.getElementById(id)
    if (el) el.value = String(value || '')
  }
  setValue('ss-nama-sekolah', data.nama_sekolah)
  setValue('ss-alamat-sekolah', data.alamat_sekolah)
  setValue('ss-kepala-sekolah', data.kepala_sekolah)
  setValue('ss-wakasek-akademik', data.wakasek_bidang_akademik || data.wakasek_akademik || '')
  setValue('ss-wakasek-ketahfizan', data.wakasek_bidang_ketahfizan || data.ketahfizan || '')
  setValue('ss-wakasek-kesantrian', data.wakasek_bidang_kesantrian || data.kesantrian || '')
}

function syncSchoolProfileToLocalStorage(row) {
  const nama = String(row?.nama_sekolah || '').trim()
  const alamat = String(row?.alamat_sekolah || '').trim()
  if (nama) {
    localStorage.setItem('school_name', nama)
    localStorage.setItem('nama_sekolah', nama)
  }
  if (alamat) {
    localStorage.setItem('school_address', alamat)
    localStorage.setItem('alamat_sekolah', alamat)
  }
}

async function fetchStrukturSekolahRow() {
  const { data, error } = await sb
    .from(STRUKTUR_SEKOLAH_TABLE)
    .select('*')
    .order('updated_at', { ascending: false })
    .order('created_at', { ascending: false })
    .limit(1)

  if (error) throw error
  return data?.[0] || null
}

function renderAutoList(containerId, items, emptyText = '-') {
  const container = document.getElementById(containerId)
  if (!container) return
  if (!Array.isArray(items) || items.length === 0) {
    container.innerHTML = `<span style="color:#64748b;">${ssEscapeHtml(emptyText)}</span>`
    return
  }
  container.innerHTML = `<ul class="ss-auto-list">${items.map(item => `<li>${ssEscapeHtml(item)}</li>`).join('')}</ul>`
}

async function loadAutoStructureSection() {
  const [kelasRes, karyawanRes] = await Promise.all([
    sb.from('kelas').select('id, nama_kelas, wali_kelas_id').order('nama_kelas'),
    sb.from('karyawan').select('id, nama, role, aktif')
  ])

  if (kelasRes.error) throw kelasRes.error
  if (karyawanRes.error) throw karyawanRes.error

  // Fallback query jika kolom kamar/halaqah belum ada di schema.
  let santriList = []
  const santriSelectAttempts = [
    'id, kamar, halaqah, aktif',
    'id, kamar, halaqah',
    'id, halaqah, aktif',
    'id, halaqah',
    'id, aktif',
    'id'
  ]
  for (const selectText of santriSelectAttempts) {
    const res = await sb.from('santri').select(selectText)
    if (res.error) continue
    santriList = res.data || []
    if (selectText.includes('aktif')) {
      santriList = santriList.filter(item => String(item?.aktif || '').toLowerCase() !== 'false')
    }
    break
  }

  const karyawanMap = new Map((karyawanRes.data || []).map(item => [String(item.id || ''), item]))

  const waliItems = (kelasRes.data || []).map(kelas => {
    const wali = karyawanMap.get(String(kelas.wali_kelas_id || ''))
    const waliNama = String(wali?.nama || '-').trim() || '-'
    return `${kelas.nama_kelas || '-'}: ${waliNama}`
  })

  const musyrifNames = (karyawanRes.data || [])
    .filter(item => String(item.aktif || '').toLowerCase() !== 'false')
    .filter(item => hasRole(item, 'musyrif'))
    .map(item => String(item.nama || '-').trim())
    .filter(Boolean)
  const muhaffizNames = (karyawanRes.data || [])
    .filter(item => String(item.aktif || '').toLowerCase() !== 'false')
    .filter(item => hasRole(item, 'muhaffiz'))
    .map(item => String(item.nama || '-').trim())
    .filter(Boolean)

  const kamarList = [...new Set((santriList || []).map(item => String(item?.kamar || '').trim()).filter(Boolean))].sort((a, b) => a.localeCompare(b))
  const halaqahList = [...new Set((santriList || []).map(item => String(item?.halaqah || '').trim()).filter(Boolean))].sort((a, b) => a.localeCompare(b))

  const musyrifItems = [
    `Daftar Musyrif: ${musyrifNames.length ? musyrifNames.join(', ') : '-'}`,
    `Kamar Aktif: ${kamarList.length ? kamarList.join(', ') : '-'}`
  ]
  const muhaffizItems = [
    `Daftar Muhaffiz: ${muhaffizNames.length ? muhaffizNames.join(', ') : '-'}`,
    `Halaqah Aktif: ${halaqahList.length ? halaqahList.join(', ') : '-'}`
  ]

  renderAutoList('ss-auto-wali-kelas', waliItems, 'Belum ada data kelas.')
  renderAutoList('ss-auto-musyrif', musyrifItems, 'Belum ada data musyrif/kamar.')
  renderAutoList('ss-auto-muhaffiz', muhaffizItems, 'Belum ada data muhaffiz/halaqah.')
}

async function loadStrukturSekolah(forceRefresh = false) {
  setStrukturSekolahStatus('Loading...')

  if (!forceRefresh && typeof getCachedData === 'function') {
    const cached = getCachedData(STRUKTUR_SEKOLAH_CACHE_KEY, STRUKTUR_SEKOLAH_CACHE_TTL_MS)
    if (cached) {
      currentStrukturSekolahId = String(cached.id || '')
      fillStrukturSekolahForm(cached)
      syncSchoolProfileToLocalStorage(cached)
      await loadAutoStructureSection()
      setStrukturSekolahStatus('')
      return
    }
  }

  try {
    const row = await fetchStrukturSekolahRow()
    currentStrukturSekolahId = String(row?.id || '')
    fillStrukturSekolahForm(row)
    if (row) {
      if (typeof setCachedData === 'function') setCachedData(STRUKTUR_SEKOLAH_CACHE_KEY, row)
      syncSchoolProfileToLocalStorage(row)
      await loadAutoStructureSection()
      setStrukturSekolahStatus('')
    } else {
      await loadAutoStructureSection()
      setStrukturSekolahStatus('Belum ada data. Isi form lalu klik Simpan.')
    }
  } catch (error) {
    if (isStrukturSekolahMissingTableError(error)) {
      const msg = getStrukturSekolahMissingTableMessage()
      setStrukturSekolahStatus(msg, true)
      alert(msg)
      return
    }
    console.error(error)
    setStrukturSekolahStatus(`Gagal load struktur sekolah: ${error?.message || 'Unknown error'}`, true)
  }
}

async function saveStrukturSekolah() {
  const payload = mapStrukturSekolahRowFromForm()
  const hasNewColumns = await checkStrukturSekolahNewColumns()
  const finalPayload = hasNewColumns
    ? payload
    : {
        nama_sekolah: payload.nama_sekolah,
        alamat_sekolah: payload.alamat_sekolah,
        kepala_sekolah: payload.kepala_sekolah,
        wakasek_akademik: payload.wakasek_bidang_akademik,
        ketahfizan: payload.wakasek_bidang_ketahfizan,
        kesantrian: payload.wakasek_bidang_kesantrian
      }
  finalPayload.updated_at = new Date().toISOString()

  const query = currentStrukturSekolahId
    ? sb.from(STRUKTUR_SEKOLAH_TABLE).update(finalPayload).eq('id', currentStrukturSekolahId).select().single()
    : sb.from(STRUKTUR_SEKOLAH_TABLE).insert([finalPayload]).select().single()

  const { data, error } = await query
  if (error) {
    if (isStrukturSekolahMissingTableError(error)) {
      alert(getStrukturSekolahMissingTableMessage())
      return
    }
    if (String(error?.message || '').toLowerCase().includes('wakasek_bidang_')) {
      alert(getStrukturSekolahMissingColumnsMessage())
      return
    }
    console.error(error)
    alert(`Gagal menyimpan struktur sekolah: ${error.message || 'Unknown error'}`)
    return
  }

  currentStrukturSekolahId = String(data?.id || currentStrukturSekolahId || '')
  if (typeof clearCachedData === 'function') clearCachedData(STRUKTUR_SEKOLAH_CACHE_KEY)
  await loadStrukturSekolah(true)
  alert('Struktur sekolah berhasil disimpan.')
}

function refreshStrukturSekolah() {
  if (typeof clearCachedData === 'function') clearCachedData(STRUKTUR_SEKOLAH_CACHE_KEY)
  loadStrukturSekolah(true)
}

function initStrukturSekolahPage() {
  loadStrukturSekolahKaryawanOptions().finally(() => {
    loadStrukturSekolah(false)
  })
}

window.initStrukturSekolahPage = initStrukturSekolahPage
window.saveStrukturSekolah = saveStrukturSekolah
window.refreshStrukturSekolah = refreshStrukturSekolah
