const ALUMNI_CACHE_KEY = 'alumni:list'
const ALUMNI_CACHE_TTL_MS = 2 * 60 * 1000
let alumniSortField = 'nama'
let alumniSortDirection = 'asc'
let alumniCurrentPage = 1
const ALUMNI_ROWS_PER_PAGE = 10
let alumniOutsideClickBound = false

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
  const filtered = all.filter(item => {
    const nama = String(item?.nama || '').toLowerCase()
    const nisn = String(item?.nisn || '').toLowerCase()
    const matchKeyword = !keyword || nama.includes(keyword) || nisn.includes(keyword)
    const matchAngkatan = !fAngkatan || String(item?.tahun_angkatan || '') === fAngkatan
    const matchLulus = !fLulus || String(item?.tahun_lulus_display || '') === fLulus
    return matchKeyword && matchAngkatan && matchLulus
  })

  const rows = [...filtered].sort((a, b) => {
    const getValue = (item) => {
      if (alumniSortField === 'angkatan') return String(item?.tahun_angkatan || '')
      if (alumniSortField === 'lulus') return String(item?.tahun_lulus_display || '')
      return String(item?.nama || '')
    }
    const cmp = getValue(a).localeCompare(getValue(b), undefined, { sensitivity: 'base' })
    return alumniSortDirection === 'desc' ? -cmp : cmp
  })

  const totalRows = rows.length
  const totalPages = Math.max(1, Math.ceil(totalRows / ALUMNI_ROWS_PER_PAGE))
  if (alumniCurrentPage > totalPages) alumniCurrentPage = totalPages
  if (alumniCurrentPage < 1) alumniCurrentPage = 1
  const startIndex = totalRows === 0 ? 0 : (alumniCurrentPage - 1) * ALUMNI_ROWS_PER_PAGE
  const endIndex = Math.min(startIndex + ALUMNI_ROWS_PER_PAGE, totalRows)
  const pagedRows = rows.slice(startIndex, endIndex)

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

  html += pagedRows.map((item, index) => `
    <tr>
      <td style="padding:8px; border:1px solid #ddd; text-align:center;">${startIndex + index + 1}</td>
      <td style="padding:8px; border:1px solid #ddd;">${escapeHtml(item.nama || '-')}</td>
      <td style="padding:8px; border:1px solid #ddd; text-align:center;">${escapeHtml(item.nisn || '-')}</td>
      <td style="padding:8px; border:1px solid #ddd; text-align:center;">${escapeHtml(item.tahun_angkatan || '-')}</td>
      <td style="padding:8px; border:1px solid #ddd; text-align:center;">${escapeHtml(item.tahun_lulus_display || '-')}</td>
      <td style="padding:8px; border:1px solid #ddd; text-align:center;">${escapeHtml(item.kelas?.nama_kelas || '-')}</td>
    </tr>
  `).join('')

  const pageButtons = []
  const pageWindow = 5
  const half = Math.floor(pageWindow / 2)
  let pageStart = Math.max(1, alumniCurrentPage - half)
  let pageEnd = Math.min(totalPages, pageStart + pageWindow - 1)
  if (pageEnd - pageStart + 1 < pageWindow) pageStart = Math.max(1, pageEnd - pageWindow + 1)
  for (let pageNum = pageStart; pageNum <= pageEnd; pageNum += 1) {
    const activeStyle = pageNum === alumniCurrentPage
      ? 'background:#d4d456ff; border-color:#d4d456ff; color:#0f172a;'
      : 'background:#fff; border-color:#cbd5e1; color:#334155;'
    pageButtons.push(`<button type="button" onclick="goAlumniPage(${pageNum})" style="border:1px solid; ${activeStyle} border-radius:999px; min-width:34px; height:34px; padding:0 10px; font-size:12px; font-weight:700; cursor:pointer;">${pageNum}</button>`)
  }

  html += `
    </tbody></table></div>
    <div style="display:flex; align-items:center; justify-content:space-between; gap:10px; flex-wrap:wrap; margin-top:10px;">
      <div style="font-size:12px; color:#64748b;">
        Menampilkan ${totalRows === 0 ? 0 : startIndex + 1}-${endIndex} dari ${totalRows} data
      </div>
      <div style="display:flex; align-items:center; gap:6px; flex-wrap:wrap;">
        <button type="button" onclick="prevAlumniPage()" ${alumniCurrentPage <= 1 ? 'disabled' : ''} style="border:1px solid #cbd5e1; background:#fff; color:#334155; border-radius:999px; height:34px; padding:0 12px; font-size:12px; font-weight:700; cursor:${alumniCurrentPage <= 1 ? 'not-allowed' : 'pointer'}; opacity:${alumniCurrentPage <= 1 ? '0.5' : '1'};">Prev</button>
        ${pageButtons.join('')}
        <button type="button" onclick="nextAlumniPage()" ${alumniCurrentPage >= totalPages ? 'disabled' : ''} style="border:1px solid #cbd5e1; background:#fff; color:#334155; border-radius:999px; height:34px; padding:0 12px; font-size:12px; font-weight:700; cursor:${alumniCurrentPage >= totalPages ? 'not-allowed' : 'pointer'}; opacity:${alumniCurrentPage >= totalPages ? '0.5' : '1'};">Next</button>
      </div>
    </div>
  `
  container.innerHTML = html
}

function goAlumniPage(page) {
  const target = Number(page)
  if (!Number.isFinite(target)) return
  alumniCurrentPage = Math.max(1, Math.floor(target))
  renderAlumniTable()
}

function nextAlumniPage() {
  goAlumniPage(alumniCurrentPage + 1)
}

function prevAlumniPage() {
  goAlumniPage(alumniCurrentPage - 1)
}

function toggleAlumniSortBox() {
  const sortBox = document.getElementById('alumni-sort-box')
  if (!sortBox) return
  const willShow = sortBox.style.display === 'none' || !sortBox.style.display
  sortBox.style.display = willShow ? 'block' : 'none'
}

function applyAlumniSortControl() {
  const fieldSelect = document.getElementById('alumni-sort-field')
  const directionSelect = document.getElementById('alumni-sort-direction')
  if (fieldSelect) alumniSortField = fieldSelect.value || 'nama'
  if (directionSelect) alumniSortDirection = directionSelect.value || 'asc'
  alumniCurrentPage = 1
  renderAlumniTable()
}

function resetAlumniSortOrder() {
  alumniSortField = 'nama'
  alumniSortDirection = 'asc'
  const fieldSelect = document.getElementById('alumni-sort-field')
  const directionSelect = document.getElementById('alumni-sort-direction')
  if (fieldSelect) fieldSelect.value = 'nama'
  if (directionSelect) directionSelect.value = 'asc'
  alumniCurrentPage = 1
  renderAlumniTable()
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
    search.addEventListener('input', () => {
      alumniCurrentPage = 1
      renderAlumniTable()
    })
    search.dataset.bound = 'true'
  }
  if (angkatan && !angkatan.dataset.bound) {
    angkatan.addEventListener('change', () => {
      alumniCurrentPage = 1
      renderAlumniTable()
    })
    angkatan.dataset.bound = 'true'
  }
  if (lulus && !lulus.dataset.bound) {
    lulus.addEventListener('change', () => {
      alumniCurrentPage = 1
      renderAlumniTable()
    })
    lulus.dataset.bound = 'true'
  }
  if (!alumniOutsideClickBound) {
    document.addEventListener('click', event => {
      const tools = document.getElementById('alumni-tools')
      const sortBox = document.getElementById('alumni-sort-box')
      if (!tools || !sortBox) return
      const target = event.target
      if (!(target instanceof Node)) return
      if (!tools.contains(target)) sortBox.style.display = 'none'
    })
    alumniOutsideClickBound = true
  }

  loadAlumni()
}

window.initAlumniPage = initAlumniPage
window.loadAlumni = loadAlumni
