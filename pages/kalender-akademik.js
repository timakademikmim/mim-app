const KALENDER_AKADEMIK_TABLE = 'kalender_akademik'
const KALENDER_AKADEMIK_CACHE_KEY = 'kalender_akademik:list'
const KALENDER_AKADEMIK_CACHE_TTL_MS = 2 * 60 * 1000
const DEFAULT_KALENDER_COLOR = '#2563eb'
let currentEditKalenderAkademikId = null
let currentKalenderAkademikList = []
let currentKalenderAkademikMonth = ''
let currentKalenderAkademikDateKey = ''
let currentKalenderAkademikPopupVisible = false

function getKalenderAkademikInsetFieldStyle(extra = '') {
  return `width:100%; padding:10px 12px; box-sizing:border-box; border:1px solid #cbd5e1; border-radius:999px; background:#f8fafc; box-shadow:none; outline:none; transition:border-color 0.2s, box-shadow 0.2s; ${extra}`
}

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

function getTodayMonthKey() {
  return new Date().toISOString().slice(0, 7)
}

function formatDateLocal(value) {
  const text = String(value || '').trim()
  if (!text) return '-'
  const date = new Date(text)
  if (Number.isNaN(date.getTime())) return text
  const dd = String(date.getDate()).padStart(2, '0')
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const yyyy = date.getFullYear()
  return `${dd}-${mm}-${yyyy}`
}

function getLocalDateKey(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const yyyy = date.getFullYear()
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

function normalizeKalenderColor(value) {
  const raw = String(value || '').trim()
  if (/^#[0-9a-fA-F]{6}$/.test(raw)) return raw
  return DEFAULT_KALENDER_COLOR
}

function getKalenderAkademikMissingTableMessage() {
  return `Tabel '${KALENDER_AKADEMIK_TABLE}' belum ada di Supabase.\n\nSilakan buat tabel:\n- id (uuid primary key, default gen_random_uuid())\n- judul (text)\n- mulai (timestamptz)\n- selesai (timestamptz, nullable)\n- detail (text, nullable)\n- warna (text, default '#2563eb')\n- created_at (timestamptz default now())`
}

function getKalenderAkademikMissingColorMessage() {
  return `Kolom warna belum ada di tabel '${KALENDER_AKADEMIK_TABLE}'.\n\nJalankan SQL:\nalter table public.${KALENDER_AKADEMIK_TABLE} add column if not exists warna text default '#2563eb';`
}

function isKalenderAkademikMissingTableError(error) {
  const msg = String(error?.message || '').toLowerCase()
  const code = String(error?.code || '').toUpperCase()
  return code === '42P01' || msg.includes('does not exist') || msg.includes(`table '${KALENDER_AKADEMIK_TABLE}'`)
}

function isKalenderAkademikMissingColorError(error) {
  const msg = String(error?.message || '').toLowerCase()
  return msg.includes('warna') && (msg.includes('schema cache') || msg.includes('column') || msg.includes('does not exist'))
}

function createKalenderAkademikModal() {
  let modal = document.getElementById('kalender-akademik-modal')
  if (modal) modal.remove()

  modal = document.createElement('div')
  modal.id = 'kalender-akademik-modal'
  modal.style.position = 'fixed'
  modal.style.top = '0'
  modal.style.left = '0'
  modal.style.width = '100vw'
  modal.style.height = '100vh'
  modal.style.background = 'rgba(0,0,0,0.3)'
  modal.style.display = 'none'
  modal.style.zIndex = '9999'
  modal.innerHTML = `
    <div style="background:#fff; margin:50px auto; padding:24px; border-radius:8px; width:460px; max-width:calc(100vw - 24px); box-shadow:0 2px 12px #0002; position:relative;">
      <h3 id="ka-modal-title">Tambah Kegiatan Akademik</h3>
      <input class="ka-field" type="text" id="ka-judul" placeholder="Judul kegiatan" style="${getKalenderAkademikInsetFieldStyle('margin-bottom:8px;')}">
      <label for="ka-warna" style="display:block; margin:0 4px 4px; font-size:12px; color:#475569;">Warna Penanda</label>
      <input type="color" id="ka-warna" value="${DEFAULT_KALENDER_COLOR}" style="width:58px; height:36px; border:none; background:transparent; margin-bottom:8px; cursor:pointer;">
      <label for="ka-mulai" style="display:block; margin:0 4px 4px; font-size:12px; color:#475569;">Mulai (Tanggal)</label>
      <input class="ka-field" type="date" id="ka-mulai" style="${getKalenderAkademikInsetFieldStyle('margin-bottom:8px;')}">
      <label for="ka-selesai" style="display:block; margin:0 4px 4px; font-size:12px; color:#475569;">Selesai (Tanggal, opsional)</label>
      <input class="ka-field" type="date" id="ka-selesai" style="${getKalenderAkademikInsetFieldStyle('margin-bottom:8px;')}">
      <textarea id="ka-detail" placeholder="Detail kegiatan" style="width:100%; min-height:110px; padding:10px 12px; box-sizing:border-box; border:1px solid #cbd5e1; border-radius:12px; background:#f8fafc; outline:none; font-size:13px; resize:vertical;"></textarea>
      <div style="margin-top:12px;">
        <button class="modal-btn modal-btn-primary" type="button" onclick="saveKalenderAkademik()">Simpan</button>
        <button class="modal-btn modal-btn-secondary" type="button" onclick="closeKalenderAkademikModal()" style="margin-left:8px;">Batal</button>
      </div>
      <span onclick="closeKalenderAkademikModal()" style="position:absolute;top:8px;right:12px;cursor:pointer;font-size:18px;">&times;</span>
    </div>
  `
  document.body.appendChild(modal)
}

function openKalenderAkademikModal(id = '') {
  if (!document.getElementById('kalender-akademik-modal')) createKalenderAkademikModal()

  currentEditKalenderAkademikId = id ? String(id) : null
  const row = currentEditKalenderAkademikId
    ? currentKalenderAkademikList.find(item => String(item.id) === currentEditKalenderAkademikId)
    : null

  const titleEl = document.getElementById('ka-modal-title')
  const judulEl = document.getElementById('ka-judul')
  const warnaEl = document.getElementById('ka-warna')
  const mulaiEl = document.getElementById('ka-mulai')
  const selesaiEl = document.getElementById('ka-selesai')
  const detailEl = document.getElementById('ka-detail')

  if (titleEl) titleEl.textContent = row ? 'Edit Kegiatan Akademik' : 'Tambah Kegiatan Akademik'
  if (judulEl) judulEl.value = row?.judul || ''
  if (warnaEl) warnaEl.value = normalizeKalenderColor(row?.warna)
  if (mulaiEl) mulaiEl.value = String(row?.mulai || '').slice(0, 10)
  if (selesaiEl) selesaiEl.value = String(row?.selesai || '').slice(0, 10)
  if (detailEl) detailEl.value = row?.detail || ''

  const modal = document.getElementById('kalender-akademik-modal')
  if (modal) modal.style.display = 'block'
}

function closeKalenderAkademikModal() {
  const modal = document.getElementById('kalender-akademik-modal')
  if (!modal) return
  modal.style.display = 'none'
  currentEditKalenderAkademikId = null
}

function parseKalenderAkademikForm() {
  const judul = String(document.getElementById('ka-judul')?.value || '').trim()
  const warna = normalizeKalenderColor(document.getElementById('ka-warna')?.value || DEFAULT_KALENDER_COLOR)
  const mulai = String(document.getElementById('ka-mulai')?.value || '').trim()
  const selesai = String(document.getElementById('ka-selesai')?.value || '').trim()
  const detail = String(document.getElementById('ka-detail')?.value || '').trim()

  if (!judul || !mulai) return { error: 'Judul kegiatan dan waktu mulai wajib diisi.' }

  const mulaiDate = new Date(`${mulai}T00:00:00`)
  if (Number.isNaN(mulaiDate.getTime())) return { error: 'Format waktu mulai tidak valid.' }

  if (selesai) {
    const selesaiDate = new Date(`${selesai}T00:00:00`)
    if (Number.isNaN(selesaiDate.getTime())) return { error: 'Format waktu selesai tidak valid.' }
    if (selesaiDate.getTime() < mulaiDate.getTime()) return { error: 'Waktu selesai tidak boleh lebih kecil dari waktu mulai.' }
  }

  return {
    payload: {
      judul,
      warna,
      mulai: mulaiDate.toISOString(),
      selesai: selesai ? new Date(`${selesai}T00:00:00`).toISOString() : null,
      detail: detail || null
    }
  }
}

async function saveKalenderAkademik() {
  const { payload, error: formError } = parseKalenderAkademikForm()
  if (formError) {
    alert(formError)
    return
  }

  const query = currentEditKalenderAkademikId
    ? sb.from(KALENDER_AKADEMIK_TABLE).update(payload).eq('id', currentEditKalenderAkademikId)
    : sb.from(KALENDER_AKADEMIK_TABLE).insert([payload])

  const { error } = await query
  if (error) {
    if (isKalenderAkademikMissingTableError(error)) {
      alert(getKalenderAkademikMissingTableMessage())
      return
    }
    if (isKalenderAkademikMissingColorError(error)) {
      alert(getKalenderAkademikMissingColorMessage())
      return
    }
    console.error(error)
    alert(`Gagal menyimpan kegiatan: ${error.message || 'Unknown error'}`)
    return
  }

  if (typeof clearCachedData === 'function') clearCachedData(KALENDER_AKADEMIK_CACHE_KEY)
  closeKalenderAkademikModal()
  loadKalenderAkademik(true)
}

async function deleteKalenderAkademik(id) {
  const confirmed = typeof showPopupConfirm === 'function'
    ? await showPopupConfirm('Yakin ingin menghapus kegiatan ini?')
    : confirm('Yakin ingin menghapus kegiatan ini?')
  if (!confirmed) return

  const { error } = await sb
    .from(KALENDER_AKADEMIK_TABLE)
    .delete()
    .eq('id', id)

  if (error) {
    if (isKalenderAkademikMissingTableError(error)) {
      alert(getKalenderAkademikMissingTableMessage())
      return
    }
    console.error(error)
    alert(`Gagal menghapus kegiatan: ${error.message || 'Unknown error'}`)
    return
  }

  if (typeof clearCachedData === 'function') clearCachedData(KALENDER_AKADEMIK_CACHE_KEY)
  loadKalenderAkademik(true)
}

function renderKalenderAkademikTable(container, rows) {
  if (!rows.length) {
    container.innerHTML = 'Belum ada kegiatan akademik.'
    return
  }

  let html = `
    <div style="overflow:auto;">
      <table style="width:100%; border-collapse:collapse; font-size:13px;">
        <thead>
          <tr style="background:#f8fafc;">
            <th style="padding:8px; border:1px solid #e2e8f0; width:50px;">No</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Kegiatan</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:170px;">Mulai</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:170px;">Selesai</th>
            <th style="padding:8px; border:1px solid #e2e8f0;">Detail</th>
            <th style="padding:8px; border:1px solid #e2e8f0; width:170px;">Aksi</th>
          </tr>
        </thead>
        <tbody>
  `

  html += rows.map((item, index) => {
    const warna = normalizeKalenderColor(item.warna)
    return `
      <tr>
        <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${index + 1}</td>
        <td style="padding:8px 8px 8px 12px; border:1px solid #e2e8f0; position:relative;">
          ${escapeHtml(item.judul || '-')}
          <span style="position:absolute; top:0; left:0; bottom:0; width:4px; background:${escapeHtml(warna)};"></span>
        </td>
        <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(formatDateLocal(item.mulai))}</td>
        <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(formatDateLocal(item.selesai))}</td>
        <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(item.detail || '-')}</td>
        <td style="padding:8px; border:1px solid #e2e8f0; white-space:nowrap;">
          <button type="button" class="ka-btn" onclick="openKalenderAkademikModal('${escapeHtml(String(item.id))}')">Edit</button>
          <button type="button" class="ka-btn" style="background:#dc2626; margin-left:6px;" onclick="deleteKalenderAkademik('${escapeHtml(String(item.id))}')">Hapus</button>
        </td>
      </tr>
    `
  }).join('')

  html += '</tbody></table></div>'
  container.innerHTML = html
}

function getDateRangeKeys(mulaiValue, selesaiValue) {
  const startKey = getLocalDateKey(mulaiValue)
  const endKey = getLocalDateKey(selesaiValue || mulaiValue)
  if (!startKey) return []

  const start = new Date(`${startKey}T00:00:00`)
  const end = new Date(`${endKey}T00:00:00`)
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) return [startKey]
  if (end.getTime() < start.getTime()) return [startKey]

  const result = []
  const cursor = new Date(start)
  while (cursor.getTime() <= end.getTime()) {
    const yyyy = cursor.getFullYear()
    const mm = String(cursor.getMonth() + 1).padStart(2, '0')
    const dd = String(cursor.getDate()).padStart(2, '0')
    result.push(`${yyyy}-${mm}-${dd}`)
    cursor.setDate(cursor.getDate() + 1)
  }
  return result
}

function getMonthMeta(monthKey) {
  const [y, m] = String(monthKey || '').split('-').map(Number)
  const year = Number.isFinite(y) ? y : new Date().getFullYear()
  const month = Number.isFinite(m) ? m : (new Date().getMonth() + 1)
  const first = new Date(year, month - 1, 1)
  const last = new Date(year, month, 0)
  return { year, month, first, last, firstWeekday: first.getDay(), daysInMonth: last.getDate() }
}

function getKalenderEventsByDateKey() {
  const map = new Map()
  ;(currentKalenderAkademikList || []).forEach(item => {
    const keys = getDateRangeKeys(item.mulai, item.selesai)
    keys.forEach(key => {
      if (!map.has(key)) map.set(key, [])
      map.get(key).push(item)
    })
  })
  return map
}

function getKalenderDayBackgroundStyle(events) {
  const colors = [...new Set((events || []).map(item => normalizeKalenderColor(item?.warna)).filter(Boolean))]
  if (colors.length === 0) return 'transparent'
  if (colors.length === 1) return `${colors[0]}22`

  const step = 100 / colors.length
  const parts = colors.map((color, index) => {
    const start = (index * step).toFixed(2)
    const end = ((index + 1) * step).toFixed(2)
    return `${color}33 ${start}% ${end}%`
  })
  return `linear-gradient(90deg, ${parts.join(', ')})`
}

function renderKalenderAkademikCalendar() {
  const titleEl = document.getElementById('ka-calendar-title')
  const gridEl = document.getElementById('ka-calendar-grid')
  const monthEl = document.getElementById('ka-month-filter')
  const popupEl = document.getElementById('ka-calendar-popup')
  if (!titleEl || !gridEl || !monthEl || !popupEl) return
  if (popupEl.style.display === 'none') return

  const monthKey = currentKalenderAkademikMonth || getTodayMonthKey()
  currentKalenderAkademikMonth = monthKey
  monthEl.value = monthKey

  const meta = getMonthMeta(monthKey)
  const todayKey = getLocalDateKey(new Date())
  const monthName = new Date(meta.year, meta.month - 1, 1).toLocaleDateString('id-ID', { month: 'long', year: 'numeric' })
  titleEl.textContent = `Kalender ${monthName}`

  const eventMap = getKalenderEventsByDateKey()
  const headers = ['Min', 'Sen', 'Sel', 'Rab', 'Kam', 'Jum', 'Sab']
  let html = headers.map(label => `<div class="ka-day-head">${label}</div>`).join('')

  for (let i = 0; i < meta.firstWeekday; i += 1) {
    html += '<div class="ka-day-cell empty"></div>'
  }

  for (let day = 1; day <= meta.daysInMonth; day += 1) {
    const dateKey = `${meta.year}-${String(meta.month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    const events = eventMap.get(dateKey) || []
    const firstColor = normalizeKalenderColor(events[0]?.warna)
    const isActive = currentKalenderAkademikDateKey === dateKey
    const isToday = todayKey === dateKey
    const dayBg = getKalenderDayBackgroundStyle(events)
    const dayNumColor = events.length ? '#0f172a' : '#475569'
    const dots = events.slice(0, 5).map(item => `<span class="ka-day-dot" style="background:${escapeHtml(normalizeKalenderColor(item.warna))};"></span>`).join('')
    const todayRing = isToday ? '0 0 0 2px #2563eb inset' : ''
    const todayBadge = isToday ? 'outline:2px solid #2563eb; outline-offset:-1px;' : ''
    html += `
      <button type="button" class="ka-day-cell ${isActive ? 'active' : ''}" onclick="selectKalenderAkademikDate('${dateKey}')" style="background:${dayBg}; box-shadow:${todayRing};">
        <span class="ka-day-number" style="background:${events.length ? `${firstColor}55` : '#f1f5f9'}; color:${dayNumColor}; ${todayBadge}">${day}</span>
        <div class="ka-day-dot-wrap">${dots}</div>
      </button>
    `
  }

  const trailingCells = (42 - (meta.firstWeekday + meta.daysInMonth))
  for (let i = 0; i < trailingCells; i += 1) {
    html += '<div class="ka-day-cell empty"></div>'
  }

  gridEl.innerHTML = html
  renderKalenderAkademikDateDetail()
}

function renderKalenderAkademikDateDetail() {
  const detailEl = document.getElementById('ka-calendar-detail')
  if (!detailEl) return

  if (!currentKalenderAkademikDateKey) {
    detailEl.innerHTML = 'Pilih tanggal untuk melihat detail kegiatan.'
    return
  }

  const allEvents = getKalenderEventsByDateKey().get(currentKalenderAkademikDateKey) || []
  if (allEvents.length === 0) {
    detailEl.innerHTML = `<div style="color:#64748b;">Tidak ada kegiatan pada tanggal ${escapeHtml(currentKalenderAkademikDateKey)}.</div>`
    return
  }

  const cards = allEvents.map(item => {
    const warna = normalizeKalenderColor(item.warna)
    return `
      <div style="border:1px solid #e2e8f0; border-left:4px solid ${escapeHtml(warna)}; border-radius:8px; padding:10px; margin-bottom:8px; background:#fff;">
        <div style="display:flex; align-items:center; gap:8px;">
          <span style="display:inline-block; width:10px; height:10px; border-radius:999px; background:${escapeHtml(warna)};"></span>
          <strong style="font-size:13px; color:#0f172a;">${escapeHtml(item.judul || '-')}</strong>
        </div>
        <div style="margin-top:6px; font-size:12px; color:#475569;">${escapeHtml(formatDateLocal(item.mulai))}${item.selesai ? ` - ${escapeHtml(formatDateLocal(item.selesai))}` : ''}</div>
        <div style="margin-top:6px; font-size:12px; color:#334155;">${escapeHtml(item.detail || '-')}</div>
      </div>
    `
  }).join('')

  detailEl.innerHTML = cards
}

function selectKalenderAkademikDate(dateKey) {
  currentKalenderAkademikDateKey = String(dateKey || '')
  renderKalenderAkademikCalendar()
}

function shiftKalenderAkademikMonth(step) {
  const meta = getMonthMeta(currentKalenderAkademikMonth || getTodayMonthKey())
  const date = new Date(meta.year, meta.month - 1 + Number(step || 0), 1)
  currentKalenderAkademikMonth = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
  currentKalenderAkademikDateKey = ''
  renderKalenderAkademikCalendar()
}

async function fetchKalenderAkademikRows() {
  const fullQuery = await sb
    .from(KALENDER_AKADEMIK_TABLE)
    .select('id, judul, warna, mulai, selesai, detail, created_at')
    .order('mulai', { ascending: true })

  if (!fullQuery.error) {
    return (fullQuery.data || []).map(item => ({ ...item, warna: normalizeKalenderColor(item.warna) }))
  }

  if (isKalenderAkademikMissingTableError(fullQuery.error)) {
    throw fullQuery.error
  }

  if (!isKalenderAkademikMissingColorError(fullQuery.error)) {
    throw fullQuery.error
  }

  const fallback = await sb
    .from(KALENDER_AKADEMIK_TABLE)
    .select('id, judul, mulai, selesai, detail, created_at')
    .order('mulai', { ascending: true })

  if (fallback.error) throw fallback.error

  alert(getKalenderAkademikMissingColorMessage())
  return (fallback.data || []).map(item => ({ ...item, warna: DEFAULT_KALENDER_COLOR }))
}

async function loadKalenderAkademik(forceRefresh = false) {
  const listEl = document.getElementById('ka-list')
  if (!listEl) return

  if (!forceRefresh && typeof getCachedData === 'function') {
    const cached = getCachedData(KALENDER_AKADEMIK_CACHE_KEY, KALENDER_AKADEMIK_CACHE_TTL_MS)
    if (Array.isArray(cached)) {
      currentKalenderAkademikList = cached
      renderKalenderAkademikTable(listEl, cached)
      if (currentKalenderAkademikPopupVisible) renderKalenderAkademikCalendar()
      return
    }
  }

  listEl.innerHTML = 'Loading...'
  try {
    const list = await fetchKalenderAkademikRows()
    currentKalenderAkademikList = list
    if (typeof setCachedData === 'function') setCachedData(KALENDER_AKADEMIK_CACHE_KEY, list)
    renderKalenderAkademikTable(listEl, list)

    if (!currentKalenderAkademikMonth) currentKalenderAkademikMonth = getTodayMonthKey()
    const selectedStillExists = currentKalenderAkademikDateKey && list.some(item => getLocalDateKey(item.mulai) === currentKalenderAkademikDateKey)
    if (!selectedStillExists) {
      const firstInMonth = list.find(item => String(getLocalDateKey(item.mulai) || '').startsWith(currentKalenderAkademikMonth))
      currentKalenderAkademikDateKey = firstInMonth ? getLocalDateKey(firstInMonth.mulai) : ''
    }
    if (currentKalenderAkademikPopupVisible) renderKalenderAkademikCalendar()
  } catch (error) {
    if (isKalenderAkademikMissingTableError(error)) {
      const msg = getKalenderAkademikMissingTableMessage().replaceAll('\n', '<br>')
      listEl.innerHTML = msg
      const gridEl = document.getElementById('ka-calendar-grid')
      if (gridEl) gridEl.innerHTML = msg
      return
    }
    console.error(error)
    listEl.innerHTML = `Gagal load kalender akademik: ${escapeHtml(error?.message || 'Unknown error')}`
  }
}

function openKalenderAkademikPopup() {
  const popupEl = document.getElementById('ka-calendar-popup')
  if (!popupEl) return
  popupEl.style.display = 'flex'
  currentKalenderAkademikPopupVisible = true
  renderKalenderAkademikCalendar()
}

function closeKalenderAkademikPopup(event) {
  if (event && event.target && event.currentTarget && event.target !== event.currentTarget) return
  const popupEl = document.getElementById('ka-calendar-popup')
  if (!popupEl) return
  popupEl.style.display = 'none'
  currentKalenderAkademikPopupVisible = false
}

function bindKalenderAkademikMonthFilter() {
  const monthEl = document.getElementById('ka-month-filter')
  if (!monthEl || monthEl.dataset.bound) return
  monthEl.value = currentKalenderAkademikMonth || getTodayMonthKey()
  monthEl.addEventListener('change', () => {
    currentKalenderAkademikMonth = monthEl.value || getTodayMonthKey()
    currentKalenderAkademikDateKey = ''
    renderKalenderAkademikCalendar()
  })
  monthEl.dataset.bound = 'true'
}

function initKalenderAkademikPage() {
  if (!currentKalenderAkademikMonth) currentKalenderAkademikMonth = getTodayMonthKey()
  createKalenderAkademikModal()
  bindKalenderAkademikMonthFilter()
  const popupEl = document.getElementById('ka-calendar-popup')
  if (popupEl) popupEl.style.display = 'none'
  currentKalenderAkademikPopupVisible = false
  loadKalenderAkademik()
}

window.initKalenderAkademikPage = initKalenderAkademikPage
window.loadKalenderAkademik = loadKalenderAkademik
window.openKalenderAkademikModal = openKalenderAkademikModal
window.closeKalenderAkademikModal = closeKalenderAkademikModal
window.saveKalenderAkademik = saveKalenderAkademik
window.deleteKalenderAkademik = deleteKalenderAkademik
window.selectKalenderAkademikDate = selectKalenderAkademikDate
window.shiftKalenderAkademikMonth = shiftKalenderAkademikMonth
window.openKalenderAkademikPopup = openKalenderAkademikPopup
window.closeKalenderAkademikPopup = closeKalenderAkademikPopup
