let currentGuruDetailId = null
let currentGuruDetailTab = 'biodata'
let currentGuruDetailData = null
const GURU_CHATGPT_LINK_TABLE = 'chatgpt_app_guru_links'
const GURU_CHATGPT_LINK_CODE_TABLE = 'chatgpt_app_link_codes'

function getGuruIdFromParams(params = {}) {
  if (params && params.guruId) return params.guruId
  return null
}

function backToGuruList() {
  if (typeof loadPage === 'function') loadPage('guru')
}

function setGuruDetailHeaderName(name) {
  const el = document.getElementById('guru-detail-header-name')
  if (!el) return
  const safeName = String(name || '').trim()
  el.textContent = safeName || 'Detail Guru'
}

function setGuruDetailTab(tab) {
  const validTab = tab === 'jadwal' || tab === 'chatgpt' ? tab : 'biodata'
  currentGuruDetailTab = validTab

  const buttons = document.querySelectorAll('.guru-detail-tab-btn')
  buttons.forEach(button => {
    button.classList.toggle('active', button.getAttribute('data-guru-detail-tab') === validTab)
  })

  const panes = document.querySelectorAll('.guru-detail-pane')
  panes.forEach(p => p.classList.remove('active'))
  const activePane = document.getElementById(`guru-detail-pane-${validTab}`)
  if (activePane) activePane.classList.add('active')

  if (currentGuruDetailId) {
    localStorage.setItem('admin_last_page', 'guru-detail')
    localStorage.setItem('admin_last_page_params', JSON.stringify({
      guruId: currentGuruDetailId,
      tab: validTab
    }))
  }

  if (validTab === 'jadwal') {
    renderGuruJadwalSection()
    loadGuruJadwal()
    return
  }

  if (validTab === 'chatgpt') {
    renderGuruChatGptSection()
    loadGuruChatGptLinkCodes()
    loadGuruChatGptLinks()
  }
}

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

function getInsetFieldStyle(extra = '') {
  return `width:100%; padding:10px 12px; box-sizing:border-box; border:1px solid #cbd5e1; border-radius:999px; background:#f8fafc; box-shadow:none; outline:none; transition:border-color 0.2s, box-shadow 0.2s; ${extra}`
}

function getBoxFieldStyle(extra = '') {
  return `width:100%; padding:10px 12px; box-sizing:border-box; border:1px solid #cbd5e1; border-radius:12px; background:#f8fafc; box-shadow:none; outline:none; transition:border-color 0.2s, box-shadow 0.2s; ${extra}`
}

function pickLabelByKeys(item, keys) {
  for (const key of keys) {
    const value = item?.[key]
    if (value !== null && value !== undefined && String(value).trim() !== '') return String(value).trim()
  }
  return ''
}

function getSemesterLabel(semester) {
  const label = pickLabelByKeys(semester, ['nama_semester', 'nama', 'label', 'kode', 'semester'])
  return label || (semester?.id ? `Semester #${semester.id}` : '-')
}

function normalizeHari(raw) {
  return String(raw || '').trim().toLowerCase()
}

function formatDateTimeLabel(value) {
  const raw = String(value || '').trim()
  if (!raw) return '-'
  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) return raw
  return date.toLocaleString('id-ID', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function isGuruChatGptLinkTableMissingError(error) {
  const msg = String(error?.message || '').toLowerCase()
  const code = String(error?.code || '').toUpperCase()
  return code === '42P01' ||
    msg.includes('does not exist') ||
    msg.includes('relation') ||
    msg.includes(`public.${GURU_CHATGPT_LINK_TABLE}`) ||
    msg.includes(`public.${GURU_CHATGPT_LINK_CODE_TABLE}`)
}

function isGuruChatGptLinkTablePermissionError(error) {
  const msg = String(error?.message || '').toLowerCase()
  const code = String(error?.code || '').toUpperCase()
  return code === '42501' ||
    msg.includes('permission denied') ||
    msg.includes('row-level security') ||
    msg.includes('policy')
}

function missingGuruChatGptLinkTableMessage() {
  return `Tabel '${GURU_CHATGPT_LINK_TABLE}' belum ada.\n\nJalankan SQL dari file:\n- scripts/supabase-chatgpt-app-link-setup.sql`
}

function guruChatGptLinkPermissionMessage() {
  return `Tabel ChatGPT link/code ada, tapi policy aksesnya belum siap.\n\nJalankan ulang SQL dari file:\n- scripts/supabase-chatgpt-app-link-setup.sql`
}

function generateChatGptLinkCode(length = 8) {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
  let result = ''
  for (let i = 0; i < length; i += 1) {
    result += chars[Math.floor(Math.random() * chars.length)]
  }
  return `CGPT-${result}`
}

function addHoursToIso(hours) {
  const date = new Date()
  date.setHours(date.getHours() + Number(hours || 0))
  return date.toISOString()
}

function getHariLabel(raw) {
  const value = normalizeHari(raw)
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

function toTimeLabel(value) {
  const text = String(value || '').trim()
  if (!text) return '-'
  return text.length >= 5 ? text.slice(0, 5) : text
}

function renderGuruBiodata() {
  const container = document.getElementById('guru-detail-biodata-container')
  if (!container || !currentGuruDetailData) return
  const data = currentGuruDetailData
  container.innerHTML = `
    <div style="margin-bottom:10px; color:#555;"><strong>ID:</strong> ${escapeHtml(data.id)}</div>
    <div style="margin-bottom:10px;">
      <label style="display:block; margin-bottom:4px;">ID Karyawan</label>
      <input type="text" id="guru-detail-id-karyawan" value="${escapeHtml(data.id_karyawan || '')}" style="${getInsetFieldStyle()}">
    </div>
    <div style="margin-bottom:10px;">
      <label style="display:block; margin-bottom:4px;">Nama</label>
      <input type="text" id="guru-detail-nama" value="${escapeHtml(data.nama || '')}" style="${getInsetFieldStyle()}">
    </div>
    <div style="margin-bottom:10px;">
      <label style="display:block; margin-bottom:4px;">Role</label>
      <input type="text" id="guru-detail-role" value="${escapeHtml(data.role || '')}" style="${getInsetFieldStyle()}">
    </div>
    <div style="margin-bottom:10px;">
      <label style="display:block; margin-bottom:4px;">No HP</label>
      <input type="text" id="guru-detail-no-hp" value="${escapeHtml(data.no_hp || '')}" style="${getInsetFieldStyle()}">
    </div>
    <div style="margin-bottom:10px;">
      <label style="display:block; margin-bottom:4px;">Alamat</label>
      <input type="text" id="guru-detail-alamat" value="${escapeHtml(data.alamat || '')}" style="${getInsetFieldStyle()}">
    </div>
    <div style="margin-bottom:10px;">
      <label style="display:flex; gap:8px; align-items:center;">
        <input type="checkbox" id="guru-detail-aktif" ${data.aktif ? 'checked' : ''}>
        Aktif
      </label>
    </div>
    <div style="margin-top:14px;">
      <button class="modal-btn modal-btn-primary" onclick="saveGuruDetail()">Simpan Perubahan</button>
    </div>
  `
}

async function saveGuruDetail() {
  if (!currentGuruDetailId) return
  const id_karyawan = String(document.getElementById('guru-detail-id-karyawan')?.value || '').trim()
  const nama = String(document.getElementById('guru-detail-nama')?.value || '').trim()
  const role = String(document.getElementById('guru-detail-role')?.value || '').trim()
  const no_hp = String(document.getElementById('guru-detail-no-hp')?.value || '').trim()
  const alamat = String(document.getElementById('guru-detail-alamat')?.value || '').trim()
  const aktif = document.getElementById('guru-detail-aktif')?.checked ?? true

  if (!id_karyawan || !nama || !role) {
    alert('ID Karyawan, nama, dan role wajib diisi')
    return
  }

  const { error } = await sb
    .from('karyawan')
    .update({
      id_karyawan,
      nama,
      role,
      no_hp: no_hp || null,
      alamat: alamat || null,
      aktif
    })
    .eq('id', currentGuruDetailId)

  if (error) {
    console.error(error)
    alert(`Gagal menyimpan biodata guru: ${error.message || 'Unknown error'}`)
    return
  }

  alert('Biodata guru berhasil diperbarui')
  await initGuruDetailPage({ guruId: currentGuruDetailId, tab: 'biodata' })
}

function renderGuruJadwalSection() {
  const container = document.getElementById('guru-detail-jadwal-container')
  if (!container) return
  container.innerHTML = '<div id="guru-detail-jadwal-list">Loading...</div>'
}

function renderGuruChatGptSection() {
  const container = document.getElementById('guru-detail-chatgpt-container')
  if (!container) return
  container.innerHTML = `
    <div style="display:grid; gap:14px;">
      <div style="padding:14px; border:1px solid #e2e8f0; border-radius:16px; background:#fff;">
        <div style="font-weight:700; color:#0f172a; margin-bottom:6px;">Tautkan Akun ChatGPT</div>
        <div style="font-size:13px; color:#475569; line-height:1.5;">
          Link ini dipakai agar ChatGPT App bisa mengenali guru tanpa perlu mengetik <code>guru_id</code> manual.
          Jalur yang lebih disarankan adalah memakai <strong>kode tautan sekali pakai</strong>, lalu link permanen akan terbentuk otomatis setelah kode ditukar di ChatGPT App.
        </div>
      </div>
      <div style="padding:14px; border:1px solid #e2e8f0; border-radius:16px; background:#fff;">
        <div style="font-weight:700; color:#0f172a; margin-bottom:10px;">Kode Tautan Sekali Pakai</div>
        <div style="font-size:13px; color:#475569; line-height:1.5; margin-bottom:10px;">
          Buat kode untuk guru ini, lalu masukkan kode tersebut di ChatGPT App. Setelah berhasil dipakai sekali, kode akan nonaktif otomatis.
        </div>
        <div style="display:flex; gap:8px; flex-wrap:wrap; margin-bottom:12px;">
          <button type="button" class="modal-btn modal-btn-primary" onclick="createGuruChatGptLinkCode()">Buat Kode 24 Jam</button>
        </div>
        <div id="guru-detail-chatgpt-code-list">Loading...</div>
      </div>
      <div id="guru-detail-chatgpt-list">Loading...</div>
      <div style="padding:14px; border:1px solid #e2e8f0; border-radius:16px; background:#fff;">
        <div style="font-weight:700; color:#0f172a; margin-bottom:10px;">Tambah / Update Link Manual</div>
        <div style="display:grid; gap:10px;">
          <div>
            <label style="display:block; margin-bottom:4px;">Provider</label>
            <input type="text" id="guru-detail-chatgpt-provider" value="chatgpt" style="${getInsetFieldStyle()}" readonly>
          </div>
          <div>
            <label style="display:block; margin-bottom:4px;">External Subject</label>
            <input type="text" id="guru-detail-chatgpt-subject" placeholder="contoh: chatgpt-user-abc123" style="${getBoxFieldStyle()}">
          </div>
          <div>
            <label style="display:block; margin-bottom:4px;">Display Name</label>
            <input type="text" id="guru-detail-chatgpt-display-name" placeholder="Nama yang tampil di ChatGPT App" style="${getBoxFieldStyle()}">
          </div>
          <div>
            <label style="display:block; margin-bottom:4px;">Email</label>
            <input type="text" id="guru-detail-chatgpt-email" placeholder="opsional" style="${getBoxFieldStyle()}">
          </div>
          <label style="display:flex; gap:8px; align-items:center;">
            <input type="checkbox" id="guru-detail-chatgpt-active" checked>
            Link aktif
          </label>
          <div style="display:flex; gap:8px; flex-wrap:wrap;">
            <button type="button" class="modal-btn modal-btn-primary" onclick="saveGuruChatGptLink()">Simpan Link</button>
            <button type="button" class="modal-btn" onclick="prefillGuruChatGptLinkForm()">Isi Nama Guru</button>
          </div>
        </div>
      </div>
    </div>
  `
}

function buildGuruChatGptLinkCodeListHtml(rows) {
  const safeRows = Array.isArray(rows) ? rows : []
  if (!safeRows.length) {
    return `
      <div style="padding:14px; border:1px dashed #cbd5e1; border-radius:16px; background:#f8fafc; color:#475569;">
        Belum ada kode tautan aktif atau riwayat kode untuk guru ini.
      </div>
    `
  }

  return `
    <div style="overflow-x:auto;">
      <table style="width:100%; border-collapse:collapse; font-size:13px;">
        <thead>
          <tr style="background:#f8fafc;">
            <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Kode</th>
            <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Berlaku Sampai</th>
            <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Dipakai Oleh</th>
            <th style="padding:8px; border:1px solid #e2e8f0; text-align:center;">Status</th>
            <th style="padding:8px; border:1px solid #e2e8f0; text-align:center;">Aksi</th>
          </tr>
        </thead>
        <tbody>
          ${safeRows.map(row => {
            const isUsed = Boolean(row.used_at || row.used_by_subject)
            const isExpired = row.expires_at ? new Date(row.expires_at).getTime() < Date.now() : false
            const statusLabel = !row.is_active ? 'Nonaktif' : isUsed ? 'Sudah Dipakai' : isExpired ? 'Kedaluwarsa' : 'Aktif'
            const statusBg = !row.is_active ? '#e2e8f0' : isUsed ? '#dbeafe' : isExpired ? '#fee2e2' : '#dcfce7'
            const statusColor = !row.is_active ? '#334155' : isUsed ? '#1d4ed8' : isExpired ? '#991b1b' : '#166534'
            return `
              <tr>
                <td style="padding:8px; border:1px solid #e2e8f0; font-weight:700; letter-spacing:0.05em;">${escapeHtml(String(row.code || '-'))}</td>
                <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(formatDateTimeLabel(row.expires_at))}</td>
                <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(String(row.used_by_subject || '-'))}</td>
                <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">
                  <span style="display:inline-flex; align-items:center; padding:4px 10px; border-radius:999px; background:${statusBg}; color:${statusColor}; font-weight:700;">
                    ${statusLabel}
                  </span>
                </td>
                <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">
                  ${row.is_active && !isUsed ? `<button type="button" class="modal-btn" onclick="revokeGuruChatGptLinkCode('${escapeHtml(String(row.id || ''))}')">Cabut</button>` : '-'}
                </td>
              </tr>
            `
          }).join('')}
        </tbody>
      </table>
    </div>
  `
}

function buildGuruChatGptLinkListHtml(rows) {
  const safeRows = Array.isArray(rows) ? rows : []
  if (!safeRows.length) {
    return `
      <div style="padding:14px; border:1px dashed #cbd5e1; border-radius:16px; background:#f8fafc; color:#475569;">
        Belum ada link ChatGPT untuk guru ini.
      </div>
    `
  }

  return `
    <div style="overflow-x:auto; padding:14px; border:1px solid #e2e8f0; border-radius:16px; background:#fff;">
      <table style="width:100%; border-collapse:collapse; font-size:13px;">
        <thead>
          <tr style="background:#f8fafc;">
            <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Subject</th>
            <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Display</th>
            <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Email</th>
            <th style="padding:8px; border:1px solid #e2e8f0; text-align:center;">Status</th>
            <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Terakhir Dipakai</th>
            <th style="padding:8px; border:1px solid #e2e8f0; text-align:center;">Aksi</th>
          </tr>
        </thead>
        <tbody>
          ${safeRows.map(row => `
            <tr>
              <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(String(row.external_subject || '-'))}</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(String(row.display_name || row.guru_nama || '-'))}</td>
              <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(String(row.email || '-'))}</td>
              <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">
                <span style="display:inline-flex; align-items:center; padding:4px 10px; border-radius:999px; background:${row.is_active ? '#dcfce7' : '#fee2e2'}; color:${row.is_active ? '#166534' : '#991b1b'}; font-weight:700;">
                  ${row.is_active ? 'Aktif' : 'Nonaktif'}
                </span>
              </td>
              <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(formatDateTimeLabel(row.last_seen_at))}</td>
              <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">
                <div style="display:flex; gap:6px; justify-content:center; flex-wrap:wrap;">
                  <button type="button" class="modal-btn" onclick="toggleGuruChatGptLinkActive('${escapeHtml(String(row.id || ''))}', ${row.is_active ? 'false' : 'true'})">${row.is_active ? 'Nonaktifkan' : 'Aktifkan'}</button>
                  <button type="button" class="modal-btn" onclick="deleteGuruChatGptLink('${escapeHtml(String(row.id || ''))}')">Hapus</button>
                </div>
              </td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    </div>
  `
}

function prefillGuruChatGptLinkForm() {
  document.getElementById('guru-detail-chatgpt-display-name').value = String(currentGuruDetailData?.nama || '')
}

async function loadGuruChatGptLinks() {
  const listEl = document.getElementById('guru-detail-chatgpt-list')
  if (!listEl || !currentGuruDetailId) return
  listEl.innerHTML = 'Loading...'

  const { data, error } = await sb
    .from(GURU_CHATGPT_LINK_TABLE)
    .select('id, provider, external_subject, guru_id, guru_nama, display_name, email, metadata_json, is_active, created_at, updated_at, last_seen_at')
    .eq('guru_id', currentGuruDetailId)
    .order('created_at', { ascending: false })

  if (error) {
    console.error(error)
    if (isGuruChatGptLinkTableMissingError(error)) {
      listEl.innerHTML = `
        <div style="padding:14px; border:1px dashed #f59e0b; border-radius:16px; background:#fffbeb; color:#92400e; white-space:pre-line;">
          ${escapeHtml(missingGuruChatGptLinkTableMessage())}
        </div>
      `
      return
    }
    if (isGuruChatGptLinkTablePermissionError(error)) {
      listEl.innerHTML = `
        <div style="padding:14px; border:1px dashed #f59e0b; border-radius:16px; background:#fffbeb; color:#92400e; white-space:pre-line;">
          ${escapeHtml(guruChatGptLinkPermissionMessage())}
        </div>
      `
      return
    }
    listEl.innerHTML = 'Gagal load link ChatGPT.'
    return
  }

  listEl.innerHTML = buildGuruChatGptLinkListHtml(data || [])
}

async function loadGuruChatGptLinkCodes() {
  const listEl = document.getElementById('guru-detail-chatgpt-code-list')
  if (!listEl || !currentGuruDetailId) return
  listEl.innerHTML = 'Loading...'

  const { data, error } = await sb
    .from(GURU_CHATGPT_LINK_CODE_TABLE)
    .select('id, provider, code, guru_id, guru_nama, expires_at, used_at, used_by_subject, is_active, metadata_json, created_at, updated_at')
    .eq('guru_id', currentGuruDetailId)
    .order('created_at', { ascending: false })
    .limit(12)

  if (error) {
    console.error(error)
    if (isGuruChatGptLinkTableMissingError(error)) {
      listEl.innerHTML = `
        <div style="padding:14px; border:1px dashed #f59e0b; border-radius:16px; background:#fffbeb; color:#92400e; white-space:pre-line;">
          ${escapeHtml(missingGuruChatGptLinkTableMessage())}
        </div>
      `
      return
    }
    if (isGuruChatGptLinkTablePermissionError(error)) {
      listEl.innerHTML = `
        <div style="padding:14px; border:1px dashed #f59e0b; border-radius:16px; background:#fffbeb; color:#92400e; white-space:pre-line;">
          ${escapeHtml(guruChatGptLinkPermissionMessage())}
        </div>
      `
      return
    }
    listEl.innerHTML = 'Gagal load kode tautan ChatGPT.'
    return
  }

  listEl.innerHTML = buildGuruChatGptLinkCodeListHtml(data || [])
}

async function createGuruChatGptLinkCode() {
  if (!currentGuruDetailId) return
  const payload = {
    provider: 'chatgpt',
    code: generateChatGptLinkCode(),
    guru_id: currentGuruDetailId,
    guru_nama: String(currentGuruDetailData?.nama || '').trim() || null,
    expires_at: addHoursToIso(24),
    is_active: true,
    metadata_json: {
      source: 'admin-manual',
      created_via: 'guru-detail'
    }
  }

  const { error } = await sb
    .from(GURU_CHATGPT_LINK_CODE_TABLE)
    .insert([payload])

  if (error) {
    console.error(error)
    if (isGuruChatGptLinkTableMissingError(error)) {
      alert(missingGuruChatGptLinkTableMessage())
      return
    }
    if (isGuruChatGptLinkTablePermissionError(error)) {
      alert(guruChatGptLinkPermissionMessage())
      return
    }
    alert(`Gagal membuat kode tautan ChatGPT: ${error.message || 'Unknown error'}`)
    return
  }

  alert(`Kode tautan berhasil dibuat:\n${payload.code}`)
  await loadGuruChatGptLinkCodes()
}

async function revokeGuruChatGptLinkCode(codeId) {
  const id = String(codeId || '').trim()
  if (!id) return
  if (!confirm('Cabut kode tautan ini?')) return

  const { error } = await sb
    .from(GURU_CHATGPT_LINK_CODE_TABLE)
    .update({ is_active: false })
    .eq('id', id)

  if (error) {
    console.error(error)
    if (isGuruChatGptLinkTablePermissionError(error)) {
      alert(guruChatGptLinkPermissionMessage())
      return
    }
    alert(`Gagal mencabut kode tautan: ${error.message || 'Unknown error'}`)
    return
  }

  await loadGuruChatGptLinkCodes()
}

async function saveGuruChatGptLink() {
  if (!currentGuruDetailId) return
  const provider = String(document.getElementById('guru-detail-chatgpt-provider')?.value || 'chatgpt').trim() || 'chatgpt'
  const external_subject = String(document.getElementById('guru-detail-chatgpt-subject')?.value || '').trim()
  const rawDisplayName = String(document.getElementById('guru-detail-chatgpt-display-name')?.value || '').trim()
  const email = String(document.getElementById('guru-detail-chatgpt-email')?.value || '').trim()
  const is_active = document.getElementById('guru-detail-chatgpt-active')?.checked ?? true
  const defaultGuruName = String(currentGuruDetailData?.nama || '').trim()
  const display_name = rawDisplayName || defaultGuruName

  if (!external_subject) {
    alert('External subject wajib diisi.')
    return
  }

  const payload = {
    provider,
    external_subject,
    guru_id: currentGuruDetailId,
    guru_nama: String(currentGuruDetailData?.nama || '').trim() || null,
    display_name: display_name || null,
    email: email || null,
    is_active,
    metadata_json: {
      source: 'admin-manual',
      updated_via: 'guru-detail'
    }
  }

  const { error } = await sb
    .from(GURU_CHATGPT_LINK_TABLE)
    .upsert(payload, { onConflict: 'provider,external_subject' })

  if (error) {
    console.error(error)
    if (isGuruChatGptLinkTableMissingError(error)) {
      alert(missingGuruChatGptLinkTableMessage())
      return
    }
    if (isGuruChatGptLinkTablePermissionError(error)) {
      alert(guruChatGptLinkPermissionMessage())
      return
    }
    alert(`Gagal menyimpan link ChatGPT: ${error.message || 'Unknown error'}`)
    return
  }

  alert('Link ChatGPT berhasil disimpan.')
  document.getElementById('guru-detail-chatgpt-subject').value = ''
  document.getElementById('guru-detail-chatgpt-display-name').value = ''
  document.getElementById('guru-detail-chatgpt-email').value = ''
  document.getElementById('guru-detail-chatgpt-active').checked = true
  await loadGuruChatGptLinks()
}

async function toggleGuruChatGptLinkActive(linkId, nextActive) {
  const id = String(linkId || '').trim()
  if (!id) return
  const normalizedNextActive = String(nextActive).trim().toLowerCase() === 'true'
  const { error } = await sb
    .from(GURU_CHATGPT_LINK_TABLE)
    .update({ is_active: normalizedNextActive })
    .eq('id', id)

  if (error) {
    console.error(error)
    if (isGuruChatGptLinkTablePermissionError(error)) {
      alert(guruChatGptLinkPermissionMessage())
      return
    }
    alert(`Gagal mengubah status link: ${error.message || 'Unknown error'}`)
    return
  }

  await loadGuruChatGptLinks()
}

async function deleteGuruChatGptLink(linkId) {
  const id = String(linkId || '').trim()
  if (!id) return
  if (!confirm('Hapus link ChatGPT ini?')) return

  const { error } = await sb
    .from(GURU_CHATGPT_LINK_TABLE)
    .delete()
    .eq('id', id)

  if (error) {
    console.error(error)
    if (isGuruChatGptLinkTablePermissionError(error)) {
      alert(guruChatGptLinkPermissionMessage())
      return
    }
    alert(`Gagal menghapus link: ${error.message || 'Unknown error'}`)
    return
  }

  await loadGuruChatGptLinks()
}

async function loadGuruJadwal() {
  const listEl = document.getElementById('guru-detail-jadwal-list')
  if (!listEl || !currentGuruDetailId) return

  listEl.innerHTML = 'Loading...'

  const { data: distribusiList, error: distribusiError } = await sb
    .from('distribusi_mapel')
    .select('id, kelas_id, mapel_id, guru_id, semester_id')
    .eq('guru_id', currentGuruDetailId)

  if (distribusiError) {
    console.error(distribusiError)
    listEl.innerHTML = 'Gagal load jadwal.'
    return
  }

  if (!distribusiList || distribusiList.length === 0) {
    listEl.innerHTML = 'Belum ada jadwal pelajaran.'
    return
  }

  const distribusiIds = distribusiList.map(item => item.id)
  const kelasIds = [...new Set(distribusiList.map(item => item.kelas_id).filter(Boolean))]
  const mapelIds = [...new Set(distribusiList.map(item => item.mapel_id).filter(Boolean))]
  const semesterIds = [...new Set(distribusiList.map(item => item.semester_id).filter(Boolean))]

  const [jadwalRes, kelasRes, mapelRes, semesterRes] = await Promise.all([
    sb.from('jadwal_pelajaran').select('id, distribusi_id, hari, jam_mulai, jam_selesai').in('distribusi_id', distribusiIds),
    kelasIds.length ? sb.from('kelas').select('id, nama_kelas').in('id', kelasIds) : Promise.resolve({ data: [] }),
    mapelIds.length ? sb.from('mapel').select('id, nama, kategori').in('id', mapelIds) : Promise.resolve({ data: [] }),
    semesterIds.length ? sb.from('semester').select('id, nama').in('id', semesterIds) : Promise.resolve({ data: [] })
  ])

  if (jadwalRes.error) {
    console.error(jadwalRes.error)
    listEl.innerHTML = 'Gagal load jadwal.'
    return
  }

  const distribusiMap = new Map(distribusiList.map(item => [String(item.id), item]))
  const kelasMap = new Map((kelasRes.data || []).map(item => [String(item.id), item]))
  const mapelMap = new Map((mapelRes.data || []).map(item => [String(item.id), item]))
  const semesterMap = new Map((semesterRes.data || []).map(item => [String(item.id), item]))
  const jadwalRows = (jadwalRes.data || [])
    .slice()
    .sort((a, b) => {
      const hariCmp = String(a.hari || '').localeCompare(String(b.hari || ''))
      if (hariCmp !== 0) return hariCmp
      return String(a.jam_mulai || '').localeCompare(String(b.jam_mulai || ''))
    })

  if (jadwalRows.length === 0) {
    listEl.innerHTML = 'Belum ada jadwal pelajaran.'
    return
  }

  let html = `
    <div style="overflow-x:auto;">
      <table style="width:100%; border-collapse:collapse; font-size:13px;">
        <thead>
          <tr style="background:#f3f3f3;">
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:110px;">Hari</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:130px;">Jam</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center;">Kelas</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center;">Mapel</th>
            <th style="padding:8px; border:1px solid #ddd; text-align:center; width:150px;">Semester</th>
          </tr>
        </thead>
        <tbody>
  `

  html += jadwalRows.map(row => {
    const distribusi = distribusiMap.get(String(row.distribusi_id))
    const kelasNama = kelasMap.get(String(distribusi?.kelas_id))?.nama_kelas || '-'
    const mapel = mapelMap.get(String(distribusi?.mapel_id))
    const mapelNama = mapel ? `${mapel.nama}${mapel.kategori ? ` (${mapel.kategori})` : ''}` : '-'
    const semesterNama = getSemesterLabel(semesterMap.get(String(distribusi?.semester_id)))
    return `
      <tr>
        <td style="padding:8px; border:1px solid #ddd;">${getHariLabel(row.hari)}</td>
        <td style="padding:8px; border:1px solid #ddd; text-align:center;">${toTimeLabel(row.jam_mulai)} - ${toTimeLabel(row.jam_selesai)}</td>
        <td style="padding:8px; border:1px solid #ddd;">${escapeHtml(kelasNama)}</td>
        <td style="padding:8px; border:1px solid #ddd;">${escapeHtml(mapelNama)}</td>
        <td style="padding:8px; border:1px solid #ddd;">${escapeHtml(semesterNama)}</td>
      </tr>
    `
  }).join('')

  html += '</tbody></table></div>'
  listEl.innerHTML = html
}

async function initGuruDetailPage(params = {}) {
  const guruId = getGuruIdFromParams(params)
  currentGuruDetailId = guruId
  currentGuruDetailTab = params?.tab === 'jadwal' || params?.tab === 'chatgpt' ? params.tab : 'biodata'
  currentGuruDetailData = null

  const biodataEl = document.getElementById('guru-detail-biodata-container')
  const jadwalEl = document.getElementById('guru-detail-jadwal-container')
  const chatGptEl = document.getElementById('guru-detail-chatgpt-container')
  if (!biodataEl || !jadwalEl || !chatGptEl) return

  if (!guruId) {
    setGuruDetailHeaderName('')
    biodataEl.innerHTML = 'ID guru tidak ditemukan.'
    jadwalEl.innerHTML = 'ID guru tidak ditemukan.'
    chatGptEl.innerHTML = 'ID guru tidak ditemukan.'
    return
  }

  setGuruDetailHeaderName('')
  biodataEl.innerHTML = 'Loading...'
  jadwalEl.innerHTML = 'Loading...'
  chatGptEl.innerHTML = 'Loading...'
  setGuruDetailTab(currentGuruDetailTab)

  const { data, error } = await sb
    .from('karyawan')
    .select('id, id_karyawan, nama, role, no_hp, alamat, aktif')
    .eq('id', guruId)
    .single()

  if (error || !data) {
    console.error(error)
    biodataEl.innerHTML = 'Gagal load data guru.'
    jadwalEl.innerHTML = 'Gagal load data guru.'
    chatGptEl.innerHTML = 'Gagal load data guru.'
    return
  }

  currentGuruDetailData = data
  setGuruDetailHeaderName(data.nama)
  renderGuruBiodata()
  renderGuruJadwalSection()
  setGuruDetailTab(currentGuruDetailTab)
}
