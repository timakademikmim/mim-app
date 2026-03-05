const id = localStorage.getItem('login_id')
const requiredRole = String(document.body?.dataset?.role || '').trim().toLowerCase()
const ROLE_PAGE_MAP = {
  admin: 'admin.html',
  guru: 'guru.html',
  muhaffiz: 'muhaffiz.html',
  musyrif: 'musyrif.html'
}

let roles = []
try {
  roles = JSON.parse(localStorage.getItem('login_roles') || '[]')
} catch (_error) {
  roles = []
}

if (!Array.isArray(roles) || roles.length === 0) {
  const singleRole = localStorage.getItem('login_role')
  roles = singleRole ? [singleRole] : []
}

roles = roles
  .map(v => String(v || '').trim().toLowerCase())
  .filter(Boolean)

if (!id || roles.length === 0) {
  location.replace('index.html')
}

if (requiredRole && !roles.includes(requiredRole)) {
  location.replace('index.html')
}

function rememberLastOpenPage() {
  try {
    const path = String(window.location.pathname || '')
      .replace(/\\/g, '/')
      .split('/')
      .filter(Boolean)
      .slice(-2)
      .join('/')
    if (!path || path === 'index.html') return
    localStorage.setItem('last_open_page', path)
  } catch (_error) {}
}
rememberLastOpenPage()

function getActiveRole() {
  if (requiredRole && roles.includes(requiredRole)) return requiredRole
  return roles[0] || ''
}

function getInitials(name) {
  const parts = String(name || '')
    .trim()
    .split(/\s+/)
    .filter(Boolean)
  if (!parts.length) return 'U'
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase()
  return `${parts[0][0] || ''}${parts[1][0] || ''}`.toUpperCase()
}

function setTopbarAvatar(name, fotoUrl = '') {
  const trigger = document.querySelector('.topbar-user-trigger')
  if (!trigger) return
  trigger.classList.add('topbar-avatar-trigger')
  trigger.innerHTML = `
    <span class="topbar-avatar" id="topbar-avatar">
      <span class="topbar-avatar-initials" id="topbar-avatar-initials">${getInitials(name)}</span>
      <img id="topbar-avatar-img" class="topbar-avatar-img" alt="Avatar pengguna" style="display:none;">
    </span>
    <span id="welcome" class="topbar-user-name" style="display:none;">${String(name || '')}</span>
  `

  const imgEl = document.getElementById('topbar-avatar-img')
  const initialsEl = document.getElementById('topbar-avatar-initials')
  if (!imgEl || !initialsEl) return

  const url = String(fotoUrl || '').trim()
  if (!url) {
    imgEl.style.display = 'none'
    imgEl.src = ''
    initialsEl.style.display = 'inline-flex'
    return
  }
  imgEl.src = url
  imgEl.style.display = 'block'
  initialsEl.style.display = 'none'
}

function initTopbarAccountMenu() {
  const wrap = document.querySelector('.topbar-user-menu-wrap')
  const trigger = document.querySelector('.topbar-user-trigger')
  const menu = document.getElementById('topbar-user-menu')
  if (!wrap || !trigger || !menu) return

  const displayName = String(localStorage.getItem('login_name') || '').trim() || String(id || '').trim()
  const photoUrl = String(localStorage.getItem('login_photo_url') || '').trim()
  const activeRole = getActiveRole()
  setTopbarAvatar(displayName, photoUrl)

  menu.classList.add('topbar-account-menu')

  let nameEl = document.getElementById('topbar-account-name')
  if (!nameEl) {
    nameEl = document.createElement('div')
    nameEl.id = 'topbar-account-name'
    nameEl.className = 'topbar-account-name'
    menu.prepend(nameEl)
  }
  nameEl.textContent = displayName

  let roleWrap = document.getElementById('topbar-role-switch-wrap')
  if (!roleWrap) {
    roleWrap = document.createElement('div')
    roleWrap.id = 'topbar-role-switch-wrap'
    roleWrap.className = 'topbar-role-switch-wrap'
    roleWrap.innerHTML = `
      <button type="button" id="topbar-role-switch-btn" class="topbar-role-switch-btn">Role</button>
      <div id="topbar-role-switch-menu" class="topbar-role-switch-menu"></div>
    `
    const firstButton = menu.querySelector('button')
    if (firstButton) firstButton.before(roleWrap)
    else menu.appendChild(roleWrap)
  }

  const roleBtn = document.getElementById('topbar-role-switch-btn')
  const roleMenu = document.getElementById('topbar-role-switch-menu')
  if (!roleBtn || !roleMenu) return

  roleBtn.textContent = `Role: ${activeRole || '-'}`
  const optionRoles = [...new Set(roles)].filter(role => !!ROLE_PAGE_MAP[role] && role !== activeRole)
  roleMenu.innerHTML = optionRoles.length
    ? optionRoles.map(role => `<button type="button" class="topbar-role-option" data-role="${role}">${role}</button>`).join('')
    : '<div class="topbar-role-empty">Tidak ada role lain</div>'

  roleBtn.onclick = event => {
    event.stopPropagation()
    roleMenu.classList.toggle('open')
  }

  roleMenu.onclick = event => {
    const btn = event.target.closest('.topbar-role-option')
    if (!btn) return
    const targetRole = String(btn.getAttribute('data-role') || '').trim().toLowerCase()
    const page = ROLE_PAGE_MAP[targetRole]
    if (!targetRole || !page) return
    localStorage.setItem('login_role', targetRole)
    location.href = page
  }

  document.addEventListener('click', event => {
    if (roleWrap.contains(event.target)) return
    roleMenu.classList.remove('open')
  })
}

function initDesktopUpdaterUi() {
  const isDesktopApp = !!(window.__TAURI_INTERNALS__ || window.__TAURI__)
  if (!isDesktopApp) return

  const releaseInfoState = {
    currentVersion: String(localStorage.getItem('desktop_app_version') || '').trim(),
    latestVersion: '',
    notes: ''
  }

  let sidebarPanel = null
  let versionBtn = null
  let statusLabel = null
  let onlineDot = null
  let onlineText = null
  let changelogOverlay = null
  let changelogBody = null
  let updateLockOverlay = null

  function ensureUpdaterStyle() {
    if (document.getElementById('desktop-updater-style')) return
    const style = document.createElement('style')
    style.id = 'desktop-updater-style'
    style.textContent = `
      .desktop-updater-sidebar {
        margin-top: auto;
        padding: 10px;
        border-top: 1px solid #e2e8f0;
        display: grid;
        gap: 6px;
        justify-items: center;
      }
      .desktop-version-btn {
        width: auto;
        text-align: center;
        border: none;
        background: transparent;
        color: #cbd5e1;
        font-size: 11px;
        line-height: 1.2;
        padding: 4px 6px;
        cursor: pointer;
        display: inline-flex;
        align-items: center;
        gap: 6px;
        order: 3;
      }
      .desktop-version-btn:hover {
        color: #94a3b8;
      }
      .desktop-status-row {
        display: flex;
        align-items: center;
        gap: 8px;
        width: 100%;
        justify-content: center;
        order: 1;
      }
      .desktop-online-indicator {
        display: inline-flex;
        align-items: center;
        gap: 5px;
        font-size: 11px;
        color: #64748b;
        min-width: 72px;
      }
      .desktop-online-indicator i {
        width: 7px;
        height: 7px;
        border-radius: 999px;
        background: #ef4444;
      }
      .desktop-online-indicator.online i {
        background: #22c55e;
      }
      .desktop-updater-status {
        font-size: 11px;
        color: #64748b;
        line-height: 1.25;
        min-height: 14px;
        text-align: center;
        order: 2;
      }
      .desktop-release-overlay {
        position: fixed;
        inset: 0;
        z-index: 12100;
        display: none;
        align-items: center;
        justify-content: center;
        background: rgba(15, 23, 42, 0.4);
        padding: 16px;
      }
      .desktop-release-overlay.open {
        display: flex;
      }
      .desktop-release-card {
        width: min(520px, calc(100vw - 24px));
        max-height: min(78vh, 620px);
        overflow: auto;
        background: #fff;
        border: 1px solid #cbd5e1;
        border-radius: 14px;
        box-shadow: 0 20px 40px rgba(15, 23, 42, 0.2);
        padding: 16px;
      }
      .desktop-release-card h3 {
        margin: 0 0 8px;
        font-size: 16px;
        color: #0f172a;
      }
      .desktop-release-meta {
        font-size: 12px;
        color: #475569;
        margin-bottom: 10px;
      }
      .desktop-release-body {
        font-size: 12px;
        color: #334155;
        white-space: pre-wrap;
        line-height: 1.45;
      }
      .desktop-release-actions {
        margin-top: 12px;
        display: flex;
        justify-content: flex-end;
      }
      .desktop-release-actions button {
        border: 1px solid #cbd5e1;
        border-radius: 10px;
        background: #fff;
        color: #0f172a;
        font-size: 12px;
        font-weight: 600;
        padding: 6px 12px;
        cursor: pointer;
      }
      .desktop-updater-lock {
        position: fixed;
        inset: 0;
        z-index: 12000;
        display: none;
        align-items: center;
        justify-content: center;
        background: rgba(15, 23, 42, 0.42);
      }
      .desktop-updater-lock.active {
        display: flex;
      }
      .desktop-updater-lock-card {
        width: min(460px, calc(100vw - 24px));
        background: #ffffff;
        border: 1px solid #cbd5e1;
        border-radius: 14px;
        padding: 18px;
        box-shadow: 0 20px 40px rgba(15, 23, 42, 0.2);
      }
      .desktop-updater-lock-title {
        margin: 0 0 6px;
        font-size: 16px;
        font-weight: 700;
        color: #0f172a;
      }
      .desktop-updater-lock-note {
        margin: 0;
        font-size: 13px;
        color: #334155;
      }
    `
    document.head.appendChild(style)
  }

  function ensureUpdaterElements() {
    ensureUpdaterStyle()
    if (!sidebarPanel) {
      sidebarPanel = document.createElement('div')
      sidebarPanel.className = 'desktop-updater-sidebar'
      sidebarPanel.innerHTML = `
        <button type="button" class="desktop-version-btn">versi - <span aria-hidden="true">›</span></button>
        <div class="desktop-status-row">
          <span class="desktop-online-indicator"><i></i><span>Offline</span></span>
        </div>
        <div class="desktop-updater-status">Siap.</div>
      `
      versionBtn = sidebarPanel.querySelector('.desktop-version-btn')
      statusLabel = sidebarPanel.querySelector('.desktop-updater-status')
      onlineDot = sidebarPanel.querySelector('.desktop-online-indicator')
      onlineText = sidebarPanel.querySelector('.desktop-online-indicator span')
      const sidebar = document.querySelector('.sidebar')
      if (sidebar) sidebar.appendChild(sidebarPanel)
      else document.body.appendChild(sidebarPanel)
    }
    if (!changelogOverlay) {
      changelogOverlay = document.createElement('div')
      changelogOverlay.className = 'desktop-release-overlay'
      changelogOverlay.innerHTML = `
        <div class="desktop-release-card">
          <h3>Info Versi Aplikasi</h3>
          <div class="desktop-release-meta" id="desktop-release-meta">Versi aplikasi</div>
          <div class="desktop-release-body" id="desktop-release-body">Belum ada catatan perubahan.</div>
          <div class="desktop-release-actions">
            <button type="button" id="desktop-release-close">Tutup</button>
          </div>
        </div>
      `
      changelogBody = changelogOverlay.querySelector('#desktop-release-body')
      const closeBtn = changelogOverlay.querySelector('#desktop-release-close')
      closeBtn?.addEventListener('click', () => changelogOverlay.classList.remove('open'))
      changelogOverlay.addEventListener('click', event => {
        if (event.target === changelogOverlay) changelogOverlay.classList.remove('open')
      })
      document.body.appendChild(changelogOverlay)
    }
    if (!updateLockOverlay) {
      updateLockOverlay = document.createElement('div')
      updateLockOverlay.className = 'desktop-updater-lock'
      updateLockOverlay.innerHTML = `
        <div class="desktop-updater-lock-card">
          <div class="desktop-updater-lock-title">Pembaruan aplikasi sedang berjalan</div>
          <p class="desktop-updater-lock-note">Untuk mencegah bentrok data, input dinonaktifkan sementara hingga update selesai.</p>
        </div>
      `
      document.body.appendChild(updateLockOverlay)
    }
    versionBtn?.addEventListener('click', () => {
      const meta = changelogOverlay?.querySelector('#desktop-release-meta')
      if (meta) {
        const current = releaseInfoState.currentVersion || '-'
        const latest = releaseInfoState.latestVersion || current
        meta.textContent = `Versi saat ini: v${current} | Versi terbaru: v${latest}`
      }
      if (changelogBody) {
        changelogBody.textContent = releaseInfoState.notes || 'Belum ada catatan perubahan pada versi ini.'
      }
      changelogOverlay?.classList.add('open')
    })
  }

  function setOnlineIndicator() {
    ensureUpdaterElements()
    const isOnline = navigator.onLine
    onlineDot?.classList.toggle('online', isOnline)
    if (onlineText) onlineText.textContent = isOnline ? 'Online' : 'Offline'
  }

  async function hydrateLatestReleaseInfo() {
    try {
      const res = await fetch('https://github.com/timakademikmim/mim-app/releases/latest/download/latest.json', { cache: 'no-store' })
      if (!res.ok) return
      const info = await res.json()
      if (info && typeof info === 'object') {
        const latestVersion = String(info.version || '').trim()
        const notes = String(info.notes || info.body || info.changelog || info.releaseNotes || '').trim()
        if (latestVersion) releaseInfoState.latestVersion = latestVersion
        if (notes) releaseInfoState.notes = notes
        if (!releaseInfoState.currentVersion && latestVersion) releaseInfoState.currentVersion = latestVersion
        renderVersionLabel()
      }
    } catch (_error) {}
  }

  function renderVersionLabel() {
    ensureUpdaterElements()
    const current = releaseInfoState.currentVersion || '-'
    if (versionBtn) versionBtn.textContent = `versi ${current}`
  }

  function updateDesktopUpdaterUi(detail) {
    ensureUpdaterElements()
    const stage = String(detail?.stage || '').trim().toLowerCase()
    const message = String(detail?.message || '').trim() || 'Memproses pembaruan...'
    const progress = Number(detail?.progress)
    const currentVersion = String(detail?.currentVersion || '').trim()
    const latestVersion = String(detail?.latestVersion || '').trim()
    const notes = String(detail?.notes || '').trim()
    if (currentVersion) releaseInfoState.currentVersion = currentVersion
    if (latestVersion) releaseInfoState.latestVersion = latestVersion
    if (notes) releaseInfoState.notes = notes
    if (releaseInfoState.currentVersion) localStorage.setItem('desktop_app_version', releaseInfoState.currentVersion)
    renderVersionLabel()

    statusLabel.textContent = message

    const isLock = stage === 'downloading' || stage === 'installing' || stage === 'ready_restart'
    updateLockOverlay.classList.toggle('active', isLock)

    if (stage === 'no_update') {
      return
    }

    if (stage === 'error') {
      if (statusLabel) statusLabel.style.color = '#b91c1c'
      updateLockOverlay.classList.remove('active')
      return
    }

    if (statusLabel) statusLabel.style.color = '#64748b'
  }

  ensureUpdaterElements()
  setOnlineIndicator()
  renderVersionLabel()
  hydrateLatestReleaseInfo()
  window.addEventListener('online', setOnlineIndicator)
  window.addEventListener('offline', setOnlineIndicator)
  window.addEventListener('desktop-updater-status', event => {
    updateDesktopUpdaterUi(event?.detail || {})
  })
}

async function invokeTauriCommand(command, payload = {}) {
  if (window.__TAURI__?.core?.invoke) {
    return window.__TAURI__.core.invoke(command, payload)
  }
  if (window.__TAURI_INTERNALS__?.invoke) {
    return window.__TAURI_INTERNALS__.invoke(command, payload)
  }
  throw new Error('Tauri invoke tidak tersedia di window.')
}

window.openExternalUrl = async function openExternalUrl(url) {
  const target = String(url || '').trim()
  if (!target) return false
  const isDesktopApp = !!(window.__TAURI_INTERNALS__ || window.__TAURI__)
  if (!isDesktopApp) {
    const popup = window.open(target, '_blank', 'noopener,noreferrer')
    return !!popup
  }
  try {
    await invokeTauriCommand('open_external_url', { url: target })
    return true
  } catch (error) {
    console.error('openExternalUrl invoke failed:', error)
  }
  try {
    const popup = window.open(target, '_blank', 'noopener,noreferrer')
    return !!popup
  } catch (_error) {
    return false
  }
}

window.printPdfBlobInPlace = async function printPdfBlobInPlace(blob) {
  if (!(blob instanceof Blob)) return false
  try {
    const url = URL.createObjectURL(blob)
    const frame = document.createElement('iframe')
    frame.style.position = 'fixed'
    frame.style.width = '1px'
    frame.style.height = '1px'
    frame.style.opacity = '0'
    frame.style.pointerEvents = 'none'
    frame.style.bottom = '0'
    frame.style.right = '0'
    frame.src = url
    document.body.appendChild(frame)

    await new Promise(resolve => {
      let done = false
      const cleanup = () => {
        if (done) return
        done = true
        setTimeout(() => {
          try {
            document.body.removeChild(frame)
          } catch (_err) {}
          URL.revokeObjectURL(url)
        }, 2500)
        resolve()
      }
      frame.onload = () => {
        try {
          frame.contentWindow?.focus?.()
          frame.contentWindow?.print?.()
        } catch (_err) {}
        cleanup()
      }
      setTimeout(cleanup, 2200)
    })
    return true
  } catch (error) {
    console.error('printPdfBlobInPlace failed:', error)
    return false
  }
}

window.savePdfDesktopAndOpen = async function savePdfDesktopAndOpen(blob, fileName) {
  if (!(blob instanceof Blob)) return { ok: false, path: '', error: 'Blob tidak valid.' }
  const isDesktopApp = !!(window.__TAURI_INTERNALS__ || window.__TAURI__)
  if (!isDesktopApp) {
    return { ok: false, path: '', error: 'Bukan mode desktop.' }
  }
  try {
    const bytes = new Uint8Array(await blob.arrayBuffer())
    let binary = ''
    const chunk = 0x8000
    for (let i = 0; i < bytes.length; i += chunk) {
      binary += String.fromCharCode(...bytes.subarray(i, i + chunk))
    }
    const base64Data = btoa(binary)
    const savedPath = await invokeTauriCommand('save_pdf_base64', {
      fileName: String(fileName || 'Dokumen.pdf'),
      base64Data
    })
    await invokeTauriCommand('open_file_path', { path: String(savedPath || '') })
    return { ok: true, path: String(savedPath || '') }
  } catch (error) {
    console.error('savePdfDesktopAndOpen failed:', error)
    return { ok: false, path: '', error: String(error?.message || error || 'Unknown error') }
  }
}

window.setTopbarUserIdentity = function setTopbarUserIdentity(nameOrObject, maybeFotoUrl) {
  let name = ''
  let fotoUrl = ''
  if (typeof nameOrObject === 'object' && nameOrObject !== null) {
    name = String(nameOrObject.name || nameOrObject.nama || '').trim()
    fotoUrl = String(nameOrObject.fotoUrl || nameOrObject.photoUrl || nameOrObject.foto_url || '').trim()
  } else {
    name = String(nameOrObject || '').trim()
    fotoUrl = String(maybeFotoUrl || '').trim()
  }
  if (name) localStorage.setItem('login_name', name)
  if (fotoUrl) localStorage.setItem('login_photo_url', fotoUrl)
  if (!fotoUrl) localStorage.removeItem('login_photo_url')

  const accountNameEl = document.getElementById('topbar-account-name')
  if (accountNameEl && name) accountNameEl.textContent = name
  setTopbarAvatar(name || String(localStorage.getItem('login_name') || '').trim() || id, fotoUrl)
}

initTopbarAccountMenu()
initDesktopUpdaterUi()

function logout() {
  localStorage.clear()
  location.replace('index.html')
}
