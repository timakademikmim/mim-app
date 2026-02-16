const ALUMNI_CACHE_KEY = 'alumni:list'
const ALUMNI_CACHE_TTL_MS = 2 * 60 * 1000

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

function getYearFromText(raw) {
  const text = String(raw || '')
  const match = text.match(/\b(19|20)\d{2}\b/)
  return match ? match[0] : ''
}

function getAlumniAngkatan(item) {
  const direct = String(item?.tahun_diterima || item?.angkatan || '').trim()
  if (direct) return direct
  const created = String(item?.created_at || '').slice(0, 4)
  return /^\d{4}$/.test(created) ? created : '-'
}

function getAlumniTahunLulus(item, tahunMap) {
  const direct = String(item?.tahun_lulus || '').trim()
  if (direct) return direct

  const kelasTahunId = String(item?.kelas?.tahun_ajaran_id || '')
  const kelasTahunNama = kelasTahunId ? String(tahunMap.get(kelasTahunId) || '') : ''
  const yearFromKelas = getYearFromText(kelasTahunNama)
  if (yearFromKelas) return yearFromKelas

  const updated = String(item?.updated_at || '').slice(0, 4)
  return /^\d{4}$/.test(updated) ? updated : '-'
}

function isSantriLulus(item) {
  return String(item?.status || '').toLowerCase() === 'lulus'
}

function isMissingSantriStatusColumnError(error) {
  const msg = String(error?.message || '').toLowerCase()
  return msg.includes('status') && msg.includes('santri')
}

function buildSantriStatusColumnMissingMessage() {
  return `Kolom status di tabel santri belum tersedia.\n\nJalankan SQL ini di Supabase:\n\nalter table public.santri\n  add column if not exists status text;\n\nupdate public.santri\nset status = case when aktif = true then 'aktif' else 'tidak_aktif' end\nwhere status is null;`
}

function renderAlumniFilterOptions(data) {
  const angkatanSelect = document.getElementById('alumni-filter-angkatan')
  const lulusSelect = document.getElementById('alumni-filter-lulus')
  if (!angkatanSelect || !lulusSelect) return

  const angkatanSet = new Set()
  const lulusSet = new Set()
  ;(data || []).forEach(item => {
    const a = String(item.tahun_angkatan || '').trim()
    const l = String(item.tahun_lulus_display || '').trim()
    if (a && a !== '-') angkatanSet.add(a)
    if (l && l !== '-') lulusSet.add(l)
  })

  const prevA = angkatanSelect.value || ''
  const prevL = lulusSelect.value || ''

  angkatanSelect.innerHTML = '<option value="">Semua Angkatan</option>'
  Array.from(angkatanSet).sort((a, b) => a.localeCompare(b)).forEach(value => {
    const opt = document.createElement('option')
    opt.value = value
    opt.textContent = value
    angkatanSelect.appendChild(opt)
  })
  angkatanSelect.value = prevA

  lulusSelect.innerHTML = '<option value="">Semua Tahun Lulus</option>'
  Array.from(lulusSet).sort((a, b) => b.localeCompare(a)).forEach(value => {
    const opt = document.createElement('option')
    opt.value = value
    opt.textContent = value
    lulusSelect.appendChild(opt)
  })
  lulusSelect.value = prevL
}

function renderAlumniTable() {
  const container = document.getElementById('list-alumni')
  if (!container) return

  const keyword = String(document.getElementById('alumni-search')?.value || '').trim().toLowerCase()
  const fAngkatan = String(document.getElementById('alumni-filter-angkatan')?.value || '')
  const fLulus = String(document.getElementById('alumni-filter-lulus')?.value || '')

  const all = Array.isArray(window.alumniList) ? window.alumniList : []
  const rows = all.filter(item => {
    const nama = String(item?.nama || '').toLowerCase()
    const nisn = String(item?.nisn || '').toLowerCase()
    const matchKeyword = !keyword || nama.includes(keyword) || nisn.includes(keyword)
    const matchAngkatan = !fAngkatan || String(item?.tahun_angkatan || '') === fAngkatan
    const matchLulus = !fLulus || String(item?.tahun_lulus_display || '') === fLulus
    return matchKeyword && matchAngkatan && matchLulus
  })

  if (!rows.length) {
    container.innerHTML = 'Belum ada data alumni.'
    return
  }

  let html = `
    <div style="overflow:auto;">
      <table style="width:100%; border-collapse:collapse; font-size:13px;">
        <thead>
          <tr style="background:#f3f3f3;">
            <th style="padding:8px; border:1px solid #ddd; width:60px;">No</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:left;">Nama</th>
            <th style="padding:8px; border:1px solid #ddd; width:120px;">NISN</th>
            <th style="padding:8px; border:1px solid #ddd; width:140px;">Tahun Angkatan</th>
            <th style="padding:8px; border:1px solid #ddd; width:140px;">Tahun Lulus</th>
            <th style="padding:8px; border:1px solid #ddd; width:160px;">Kelas Terakhir</th>
          </tr>
        </thead>
        <tbody>
  `

  html += rows.map((item, index) => `
    <tr>
      <td style="padding:8px; border:1px solid #ddd; text-align:center;">${index + 1}</td>
      <td style="padding:8px; border:1px solid #ddd;">${escapeHtml(item.nama || '-')}</td>
      <td style="padding:8px; border:1px solid #ddd; text-align:center;">${escapeHtml(item.nisn || '-')}</td>
      <td style="padding:8px; border:1px solid #ddd; text-align:center;">${escapeHtml(item.tahun_angkatan || '-')}</td>
      <td style="padding:8px; border:1px solid #ddd; text-align:center;">${escapeHtml(item.tahun_lulus_display || '-')}</td>
      <td style="padding:8px; border:1px solid #ddd; text-align:center;">${escapeHtml(item.kelas?.nama_kelas || '-')}</td>
    </tr>
  `).join('')

  html += '</tbody></table></div>'
  container.innerHTML = html
}

async function loadAlumni(forceRefresh = false) {
  const container = document.getElementById('list-alumni')
  if (!container) return

  if (!forceRefresh && typeof getCachedData === 'function') {
    const cached = getCachedData(ALUMNI_CACHE_KEY, ALUMNI_CACHE_TTL_MS)
    if (Array.isArray(cached)) {
      window.alumniList = cached
      renderAlumniFilterOptions(cached)
      renderAlumniTable()
      return
    }
  }

  container.innerHTML = 'Loading...'

  const [santriRes, tahunRes] = await Promise.all([
    sb
      .from('santri')
      .select(`
        *,
        kelas (
          id,
          nama_kelas,
          tahun_ajaran_id
        )
      `)
      .eq('status', 'lulus')
      .order('nama'),
    sb.from('tahun_ajaran').select('id, nama')
  ])

  if (santriRes.error || tahunRes.error) {
    console.error(santriRes.error || tahunRes.error)
    if (isMissingSantriStatusColumnError(santriRes.error)) {
      container.innerHTML = `<div style="white-space:pre-wrap;">${escapeHtml(buildSantriStatusColumnMissingMessage())}</div>`
      return
    }
    container.innerHTML = 'Gagal load data alumni.'
    return
  }

  const tahunMap = new Map((tahunRes.data || []).map(item => [String(item.id), String(item.nama || '')]))
  const data = (santriRes.data || [])
    .filter(isSantriLulus)
    .map(item => ({
      ...item,
      tahun_angkatan: getAlumniAngkatan(item),
      tahun_lulus_display: getAlumniTahunLulus(item, tahunMap)
    }))

  if (typeof setCachedData === 'function') setCachedData(ALUMNI_CACHE_KEY, data)
  window.alumniList = data
  renderAlumniFilterOptions(data)
  renderAlumniTable()
}

function initAlumniPage() {
  const search = document.getElementById('alumni-search')
  const angkatan = document.getElementById('alumni-filter-angkatan')
  const lulus = document.getElementById('alumni-filter-lulus')

  if (search && !search.dataset.bound) {
    search.addEventListener('input', () => renderAlumniTable())
    search.dataset.bound = 'true'
  }
  if (angkatan && !angkatan.dataset.bound) {
    angkatan.addEventListener('change', () => renderAlumniTable())
    angkatan.dataset.bound = 'true'
  }
  if (lulus && !lulus.dataset.bound) {
    lulus.addEventListener('change', () => renderAlumniTable())
    lulus.dataset.bound = 'true'
  }

  loadAlumni()
}

window.initAlumniPage = initAlumniPage
window.loadAlumni = loadAlumni
