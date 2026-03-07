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

  const isTauriApp = !!(window.__TAURI_INTERNALS__ || window.__TAURI__)
  const isAndroidApp = isTauriApp && /android/i.test(String(navigator.userAgent || ''))
  const isDesktopApp = isTauriApp && !isAndroidApp
  if (isDesktopApp && !document.getElementById('topbar-app-info-btn')) {
    const infoBtn = document.createElement('button')
    infoBtn.type = 'button'
    infoBtn.id = 'topbar-app-info-btn'
    infoBtn.textContent = 'Info'
    const logoutBtn = menu.querySelector('button[onclick*="logout"], button[onclick*="Logout"]')
    if (logoutBtn) logoutBtn.before(infoBtn)
    else menu.appendChild(infoBtn)
  }
  if (!isTauriApp && !document.getElementById('topbar-web-info-btn')) {
    const infoBtn = document.createElement('button')
    infoBtn.type = 'button'
    infoBtn.id = 'topbar-web-info-btn'
    infoBtn.textContent = 'Info'
    const logoutBtn = menu.querySelector('button[onclick*="logout"], button[onclick*="Logout"]')
    if (logoutBtn) logoutBtn.before(infoBtn)
    else menu.appendChild(infoBtn)
  }

  if (!document.getElementById('topbar-web-desktop-info-style')) {
    const style = document.createElement('style')
    style.id = 'topbar-web-desktop-info-style'
    style.textContent = `
      .topbar-web-info-overlay {
        position: fixed;
        inset: 0;
        z-index: 12100;
        display: none;
        align-items: center;
        justify-content: center;
        background: rgba(15, 23, 42, 0.4);
        padding: 16px;
      }
      .topbar-web-info-overlay.open {
        display: flex;
      }
      .topbar-web-info-card {
        width: min(520px, calc(100vw - 24px));
        max-height: min(78vh, 620px);
        overflow: auto;
        background: #fff;
        border: 1px solid #cbd5e1;
        border-radius: 14px;
        box-shadow: 0 20px 40px rgba(15, 23, 42, 0.2);
        padding: 16px;
      }
      .topbar-web-info-card h3 {
        margin: 0 0 8px;
        font-size: 16px;
        color: #0f172a;
      }
      .topbar-web-desktop-version {
        font-size: 12px;
        color: #475569;
        margin-bottom: 6px;
      }
      .topbar-web-desktop-notes {
        white-space: pre-wrap;
        font-size: 12px;
        color: #334155;
        line-height: 1.45;
        margin-bottom: 8px;
      }
      .topbar-web-desktop-downloads {
        display: grid;
        grid-template-columns: 1fr;
        gap: 8px;
      }
      .topbar-web-download-group {
        border: 1px solid #e2e8f0;
        border-radius: 10px;
        padding: 8px;
      }
      .topbar-web-download-group-title {
        font-size: 11px;
        font-weight: 700;
        color: #475569;
        margin: 0 0 6px;
      }
      .topbar-web-download-group-buttons {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 8px;
      }
      .topbar-web-desktop-download-btn {
        width: 100%;
        border: 1px solid #cbd5e1;
        border-radius: 10px;
        background: #fff;
        color: #0f172a;
        font-size: 12px;
        font-weight: 600;
        padding: 8px 10px;
        cursor: pointer;
      }
      .topbar-web-desktop-download-btn:hover {
        background: #f8fafc;
      }
      .topbar-web-info-actions {
        margin-top: 12px;
        display: flex;
        justify-content: flex-end;
      }
      .topbar-web-info-actions button {
        border: 1px solid #cbd5e1;
        border-radius: 10px;
        background: #fff;
        color: #0f172a;
        font-size: 12px;
        font-weight: 600;
        padding: 6px 12px;
        cursor: pointer;
      }
    `
    document.head.appendChild(style)
  }

  initWebDesktopInfoPopup()
  initMobileInAppUpdatePrompt()

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

async function initWebDesktopInfoPopup() {
  const isTauriApp = !!(window.__TAURI_INTERNALS__ || window.__TAURI__)
  if (isTauriApp) return
  const GENERIC_NOTE = 'desktop release otomatis dengan updater artifacts.'
  const WEB_VERSION_WHATS_NEW = {
    '0.3.8': `What's new in this version:
- Alur update Android dan Desktop dipisah (Android: mobile-latest.json, Desktop: latest.json).
- Asset rilis Android kini otomatis terupload bersama metadata mobile-latest.json.
- Build pipeline lintas platform diperbaiki agar runner Linux tidak gagal karena PowerShell.`,
    '0.3.7': `What's new in this version:
- Android release kini terotomatisasi: APK ikut terupload saat tag rilis dibuat.
- Asset APK updater distandarkan ke app-universal-release.apk agar update in-app lebih konsisten.
- Stabilitas updater desktop ditingkatkan.`,
    '0.3.6': `What's new in this version:
- Perbaikan updater desktop agar status "pembaruan berjalan" tidak stuck berkepanjangan.
- Lock update otomatis dilepas pada kondisi siap restart / error / timeout watchdog.
- Stabilitas alur update desktop ditingkatkan untuk mencegah freeze di antarmuka.`,
    '0.3.5': `What's new in this version:
- Perbaikan checker update Android agar tetap mendeteksi versi baru meski versi lokal belum terbaca.
- Cache-busting pada pembacaan latest.json supaya tidak tertahan data versi lama.`,
    '0.3.4': `What's new in this version:
- Android update prompt hanya muncul jika ada versi yang benar-benar lebih baru.
- Tautan unduhan Android diarahkan ke paket ARM64 yang lebih ringan untuk install manual.
- Catatan rilis fallback diperjelas agar popup Info tetap informatif.`,
    '0.3.0': `What's new in this version:
- Cincin status avatar sekarang tampil permanen (biru saat online, merah saat offline).
- Menu akun menambahkan tombol Info di bawah Profil.
- Popup Info rilis dirapikan agar catatan versi lebih jelas.`,
    '0.2.9': `What's new in this version:
- Info rilis dipindahkan ke menu avatar di topbar.
- Indikator online/offline dipindah ke cincin di sekitar avatar.`
  }
  const normalizeVersion = version => String(version || '').trim().replace(/^v/i, '')
  const isGenericNotes = text => String(text || '').trim().toLowerCase() === GENERIC_NOTE
  const getStoredVersion = () => normalizeVersion(localStorage.getItem('web_desktop_latest_version') || '')
  const setStoredVersion = version => {
    const v = normalizeVersion(version)
    if (v) localStorage.setItem('web_desktop_latest_version', v)
  }
  const fallbackNotes = version => {
    const v = normalizeVersion(version)
    if (WEB_VERSION_WHATS_NEW[v]) return WEB_VERSION_WHATS_NEW[v]
    return `What's new in this version:
- Pembaruan fitur dan penyempurnaan stabilitas aplikasi desktop.`
  }
  const fetchLatestDesktopVersionFromApi = async () => {
    try {
      const res = await fetch('https://api.github.com/repos/timakademikmim/mim-app/releases/latest', {
        cache: 'no-store',
        headers: { Accept: 'application/vnd.github+json' }
      })
      if (!res.ok) return ''
      const json = await res.json()
      return normalizeVersion(json?.tag_name || json?.name || '')
    } catch (_error) {
      return ''
    }
  }
  const infoBtn = document.getElementById('topbar-web-info-btn')
  if (!infoBtn) return
  let overlay = document.getElementById('topbar-web-info-overlay')
  if (!overlay) {
    overlay = document.createElement('div')
    overlay.id = 'topbar-web-info-overlay'
    overlay.className = 'topbar-web-info-overlay'
    overlay.innerHTML = `
      <div class="topbar-web-info-card">
        <h3>Info Versi Desktop</h3>
        <div id="topbar-web-desktop-version" class="topbar-web-desktop-version">Versi desktop: memuat...</div>
        <div id="topbar-web-desktop-notes" class="topbar-web-desktop-notes">Memuat catatan rilis...</div>
        <div class="topbar-web-desktop-downloads">
          <div class="topbar-web-download-group">
            <div class="topbar-web-download-group-title">Desktop</div>
            <div class="topbar-web-download-group-buttons">
              <button type="button" id="topbar-web-desktop-download-exe" class="topbar-web-desktop-download-btn">Download EXE</button>
              <button type="button" id="topbar-web-desktop-download-msi" class="topbar-web-desktop-download-btn">Download MSI</button>
            </div>
          </div>
          <div class="topbar-web-download-group">
            <div class="topbar-web-download-group-title">Android</div>
            <div class="topbar-web-download-group-buttons">
              <button type="button" id="topbar-web-mobile-download-apk" class="topbar-web-desktop-download-btn">Download APK</button>
              <button type="button" id="topbar-web-mobile-download-aab" class="topbar-web-desktop-download-btn">Download AAB</button>
            </div>
          </div>
        </div>
        <div class="topbar-web-info-actions">
          <button type="button" id="topbar-web-info-close">Tutup</button>
        </div>
      </div>
    `
    document.body.appendChild(overlay)
    const closeBtn = overlay.querySelector('#topbar-web-info-close')
    closeBtn?.addEventListener('click', () => overlay.classList.remove('open'))
    overlay.addEventListener('click', event => {
      if (event.target === overlay) overlay.classList.remove('open')
    })
  }
  const versionEl = overlay.querySelector('#topbar-web-desktop-version')
  const notesEl = overlay.querySelector('#topbar-web-desktop-notes')
  const downloadExeBtn = overlay.querySelector('#topbar-web-desktop-download-exe')
  const downloadMsiBtn = overlay.querySelector('#topbar-web-desktop-download-msi')
  const downloadApkBtn = overlay.querySelector('#topbar-web-mobile-download-apk')
  const downloadAabBtn = overlay.querySelector('#topbar-web-mobile-download-aab')
  if (!versionEl || !notesEl || !downloadExeBtn || !downloadMsiBtn || !downloadApkBtn || !downloadAabBtn) return

  const downloadState = {
    exe: '',
    msi: '',
    apk: '',
    aab: ''
  }

  if (infoBtn.dataset.boundDesktopInfo !== '1') {
    infoBtn.dataset.boundDesktopInfo = '1'
    infoBtn.addEventListener('click', () => {
      overlay.classList.add('open')
    })
  }
  if (downloadExeBtn.dataset.boundDesktopDownload !== '1') {
    downloadExeBtn.dataset.boundDesktopDownload = '1'
    downloadExeBtn.addEventListener('click', () => {
      const url = downloadState.exe || `https://github.com/timakademikmim/mim-app/releases/latest/download/MIM.App_${getStoredVersion() || '0.3.8'}_x64-setup.exe`
      window.open(url, '_blank', 'noopener,noreferrer')
    })
  }
  if (downloadMsiBtn.dataset.boundDesktopDownload !== '1') {
    downloadMsiBtn.dataset.boundDesktopDownload = '1'
    downloadMsiBtn.addEventListener('click', () => {
      const url = downloadState.msi || `https://github.com/timakademikmim/mim-app/releases/latest/download/MIM.App_${getStoredVersion() || '0.3.8'}_x64_en-US.msi`
      window.open(url, '_blank', 'noopener,noreferrer')
    })
  }
  if (downloadApkBtn.dataset.boundDesktopDownload !== '1') {
    downloadApkBtn.dataset.boundDesktopDownload = '1'
    downloadApkBtn.addEventListener('click', () => {
      const url = downloadState.apk || 'https://github.com/timakademikmim/mim-app/releases/latest/download/app-universal-release.apk'
      window.open(url, '_blank', 'noopener,noreferrer')
    })
  }
  if (downloadAabBtn.dataset.boundDesktopDownload !== '1') {
    downloadAabBtn.dataset.boundDesktopDownload = '1'
    downloadAabBtn.addEventListener('click', () => {
      const url = downloadState.aab || 'https://github.com/timakademikmim/mim-app/releases/latest/download/app-universal-release.aab'
      window.open(url, '_blank', 'noopener,noreferrer')
    })
  }

  try {
    const latestRes = await fetch(`https://github.com/timakademikmim/mim-app/releases/latest/download/latest.json?t=${Date.now()}`, { cache: 'no-store' })
    if (!latestRes.ok) throw new Error('latest.json not available')
    const latest = await latestRes.json()
    const fetchedVersion = normalizeVersion(latest?.version || '')
    const version = fetchedVersion || getStoredVersion() || '0.3.8'
    setStoredVersion(version)
    versionEl.textContent = `Versi desktop terbaru: v${version}`
    const platforms = latest?.platforms && typeof latest.platforms === 'object' ? latest.platforms : {}
    downloadState.exe = String(platforms?.['windows-x86_64-nsis']?.url || '').trim()
    downloadState.msi = String(platforms?.['windows-x86_64-msi']?.url || platforms?.['windows-x86_64']?.url || '').trim()
    if (latest?.mobile && typeof latest.mobile === 'object') {
      downloadState.apk = String(latest.mobile.apk || '').trim()
      downloadState.aab = String(latest.mobile.aab || '').trim()
    }
    try {
      const mobileRes = await fetch('https://github.com/timakademikmim/mim-app/releases/latest/download/mobile-latest.json', { cache: 'no-store' })
      if (mobileRes.ok) {
        const mobileLatest = await mobileRes.json()
        if (mobileLatest && typeof mobileLatest === 'object' && mobileLatest.mobile) {
          downloadState.apk = String(mobileLatest.mobile.apk || downloadState.apk || '').trim()
          downloadState.aab = String(mobileLatest.mobile.aab || downloadState.aab || '').trim()
        }
      }
    } catch (_error) {}
    let notes = String(latest?.notes || latest?.body || latest?.changelog || '').trim()
    if (!notes || isGenericNotes(notes) || !downloadState.apk || !downloadState.aab) {
      const releaseRes = await fetch(`https://api.github.com/repos/timakademikmim/mim-app/releases/tags/v${encodeURIComponent(version)}`, {
        cache: 'no-store',
        headers: { Accept: 'application/vnd.github+json' }
      })
      if (releaseRes.ok) {
        const release = await releaseRes.json()
        notes = String(release?.body || '').trim()
        const assets = Array.isArray(release?.assets) ? release.assets : []
        if (!downloadState.apk) {
          const apkAsset = assets.find(asset => String(asset?.name || '').toLowerCase().endsWith('.apk'))
          downloadState.apk = String(apkAsset?.browser_download_url || '').trim()
        }
        if (!downloadState.aab) {
          const aabAsset = assets.find(asset => String(asset?.name || '').toLowerCase().endsWith('.aab'))
          downloadState.aab = String(aabAsset?.browser_download_url || '').trim()
        }
      }
    }
    notesEl.textContent = (!notes || isGenericNotes(notes)) ? fallbackNotes(version) : notes
  } catch (_error) {
    const fallbackVersion = getStoredVersion() || await fetchLatestDesktopVersionFromApi() || '0.3.8'
    setStoredVersion(fallbackVersion)
    versionEl.textContent = `Versi desktop terbaru: v${fallbackVersion}`
    notesEl.textContent = fallbackNotes(fallbackVersion)
  }
}

async function initMobileInAppUpdatePrompt() {
  const isTauriApp = !!(window.__TAURI_INTERNALS__ || window.__TAURI__)
  const isAndroidApp = isTauriApp && /android/i.test(String(navigator.userAgent || ''))
  if (!isAndroidApp) return
  if (document.getElementById('mobile-update-overlay')) return

  const compareVersions = (a, b) => {
    const pa = String(a || '').replace(/^v/i, '').split('.').map(n => parseInt(n, 10) || 0)
    const pb = String(b || '').replace(/^v/i, '').split('.').map(n => parseInt(n, 10) || 0)
    const maxLen = Math.max(pa.length, pb.length)
    for (let i = 0; i < maxLen; i += 1) {
      const ai = pa[i] || 0
      const bi = pb[i] || 0
      if (ai > bi) return 1
      if (ai < bi) return -1
    }
    return 0
  }

  const normalizeVersion = version => String(version || '').trim().replace(/^v/i, '')

  const getCurrentVersion = async () => {
    try {
      const version = await invokeTauriCommand('get_app_version', {})
      const clean = normalizeVersion(version)
      if (clean) return clean
    } catch (_error) {}
    return normalizeVersion(localStorage.getItem('mobile_app_version') || '') || '0.0.0'
  }

  const getReleaseBodyByTag = async version => {
    try {
      const clean = normalizeVersion(version)
      if (!clean) return ''
      const res = await fetch(`https://api.github.com/repos/timakademikmim/mim-app/releases/tags/v${encodeURIComponent(clean)}`, {
        cache: 'no-store',
        headers: { Accept: 'application/vnd.github+json' }
      })
      if (!res.ok) return ''
      const json = await res.json()
      return String(json?.body || '').trim()
    } catch (_error) {
      return ''
    }
  }

  const mobileLatestUrl = `https://github.com/timakademikmim/mim-app/releases/latest/download/mobile-latest.json?t=${Date.now()}`
  let latestRes = await fetch(mobileLatestUrl, { cache: 'no-store' }).catch(() => null)
  if (!latestRes || !latestRes.ok) {
    const fallbackUrl = `https://github.com/timakademikmim/mim-app/releases/latest/download/latest.json?t=${Date.now()}`
    latestRes = await fetch(fallbackUrl, { cache: 'no-store' }).catch(() => null)
  }
  if (!latestRes || !latestRes.ok) return
  const latest = await latestRes.json().catch(() => null)
  if (!latest || typeof latest !== 'object') return

  const latestVersion = normalizeVersion(latest.version)
  const currentVersion = await getCurrentVersion()
  if (!latestVersion) return
  localStorage.setItem('mobile_app_version', currentVersion)

  if (compareVersions(latestVersion, currentVersion) <= 0) return

  let apkUrl = ''
  if (latest.mobile && typeof latest.mobile === 'object') {
    apkUrl = String(latest.mobile.apk || '').trim()
  }
  if (!apkUrl && latest.platforms && typeof latest.platforms === 'object') {
    apkUrl = String(latest.platforms?.['android-arm64-apk']?.url || '').trim()
  }
  if (!apkUrl) apkUrl = 'https://github.com/timakademikmim/mim-app/releases/latest/download/app-universal-release.apk'

  let notes = String(latest.notes || latest.body || latest.changelog || '').trim()
  if (!notes) notes = await getReleaseBodyByTag(latestVersion)
  if (!notes) {
    notes = `What's new in this version:\n- Peningkatan stabilitas dan pembaruan fitur aplikasi mobile.`
  }

  const styleId = 'mobile-update-style'
  if (!document.getElementById(styleId)) {
    const style = document.createElement('style')
    style.id = styleId
    style.textContent = `
      .mobile-update-overlay {
        position: fixed;
        inset: 0;
        z-index: 12500;
        background: rgba(15, 23, 42, 0.45);
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 16px;
      }
      .mobile-update-card {
        width: min(520px, calc(100vw - 24px));
        max-height: min(80vh, 720px);
        overflow: auto;
        background: #fff;
        border: 1px solid #cbd5e1;
        border-radius: 14px;
        box-shadow: 0 20px 40px rgba(15, 23, 42, 0.25);
        padding: 16px;
      }
      .mobile-update-title {
        margin: 0 0 6px;
        font-size: 18px;
        color: #0f172a;
      }
      .mobile-update-version {
        font-size: 12px;
        color: #475569;
        margin: 0 0 8px;
      }
      .mobile-update-notes {
        white-space: pre-wrap;
        font-size: 13px;
        color: #334155;
        line-height: 1.45;
      }
      .mobile-update-actions {
        margin-top: 14px;
        display: flex;
        gap: 8px;
        justify-content: flex-end;
      }
      .mobile-update-actions button {
        border: 1px solid #cbd5e1;
        border-radius: 10px;
        background: #fff;
        color: #0f172a;
        font-size: 12px;
        font-weight: 600;
        padding: 7px 12px;
        cursor: pointer;
      }
      .mobile-update-actions .primary {
        background: #0f172a;
        color: #fff;
        border-color: #0f172a;
      }
    `
    document.head.appendChild(style)
  }

  const overlay = document.createElement('div')
  overlay.id = 'mobile-update-overlay'
  overlay.className = 'mobile-update-overlay'
  overlay.innerHTML = `
    <div class="mobile-update-card">
      <h3 class="mobile-update-title">Versi baru tersedia</h3>
      <div class="mobile-update-version">Versi saat ini: v${currentVersion} | Versi terbaru: v${latestVersion}</div>
      <div class="mobile-update-notes">${notes}</div>
      <div class="mobile-update-actions">
        <button type="button" id="mobile-update-later">Nanti</button>
        <button type="button" id="mobile-update-install" class="primary">Install</button>
      </div>
    </div>
  `
  document.body.appendChild(overlay)

  overlay.querySelector('#mobile-update-later')?.addEventListener('click', () => {
    overlay.remove()
  })
  overlay.querySelector('#mobile-update-install')?.addEventListener('click', async () => {
    try {
      if (typeof window.openExternalUrl === 'function') {
        const opened = await window.openExternalUrl(apkUrl)
        if (opened) return
      }
      window.location.href = apkUrl
    } catch (_error) {
      window.location.href = apkUrl
    }
  })
}

function initDesktopUpdaterUi() {
  const isDesktopApp = !!(window.__TAURI_INTERNALS__ || window.__TAURI__)
  if (!isDesktopApp) return

  const GENERIC_RELEASE_NOTE_PATTERNS = [
    'desktop release otomatis dengan updater artifacts.'
  ]
  const VERSION_WHATS_NEW = {
    '0.3.8': `What's new in this version:
- Alur update Android dan Desktop dipisah (Android: mobile-latest.json, Desktop: latest.json).
- Asset rilis Android kini otomatis terupload bersama metadata mobile-latest.json.
- Build pipeline lintas platform diperbaiki agar runner Linux tidak gagal karena PowerShell.`,
    '0.3.7': `What's new in this version:
- Android release kini terotomatisasi: APK ikut terupload saat tag rilis dibuat.
- Asset APK updater distandarkan ke app-universal-release.apk agar update in-app lebih konsisten.
- Stabilitas updater desktop ditingkatkan.`,
    '0.3.6': `What's new in this version:
- Perbaikan updater desktop agar status "pembaruan berjalan" tidak stuck berkepanjangan.
- Lock update otomatis dilepas pada kondisi siap restart / error / timeout watchdog.
- Stabilitas alur update desktop ditingkatkan untuk mencegah freeze di antarmuka.`,
    '0.3.5': `What's new in this version:
- Perbaikan checker update Android agar tetap mendeteksi versi baru meski versi lokal belum terbaca.
- Cache-busting pada pembacaan latest.json supaya tidak tertahan data versi lama.`,
    '0.3.4': `What's new in this version:
- Android update prompt hanya muncul jika ada versi yang benar-benar lebih baru.
- Tautan unduhan Android diarahkan ke paket ARM64 yang lebih ringan untuk install manual.
- Catatan rilis fallback diperjelas agar popup Info tetap informatif.`,
    '0.3.0': `What's new in this version:
- Cincin status avatar sekarang tampil permanen (biru saat online, merah saat offline).
- Menu akun menambahkan tombol Info di bawah Profil.
- Popup Info rilis dirapikan agar catatan versi lebih jelas.`,
    '0.2.9': `What's new in this version:
- Info rilis dipindahkan ke menu avatar di topbar.
- Indikator online/offline dipindah ke cincin di sekitar avatar.`
  }

  const releaseInfoState = {
    currentVersion: String(localStorage.getItem('desktop_app_version') || '').trim(),
    latestVersion: '',
    notes: '',
    notesByVersion: {}
  }
  let updaterLockWatchdog = null

  let infoBtn = null
  let avatarEl = null
  let avatarTriggerEl = null
  let changelogOverlay = null
  let changelogBody = null
  let updateLockOverlay = null
  let lastOnlineState = null

  function cleanVersion(version) {
    return String(version || '').trim().replace(/^v/i, '')
  }

  function isGenericReleaseNotes(text) {
    const value = String(text || '').trim().toLowerCase()
    if (!value) return true
    return GENERIC_RELEASE_NOTE_PATTERNS.includes(value)
  }

  function getFallbackReleaseNotes(version) {
    const v = cleanVersion(version)
    if (VERSION_WHATS_NEW[v]) return VERSION_WHATS_NEW[v]
    return `What's new in this version:
- Pembaruan stabilitas dan penyempurnaan antarmuka.
- Perbaikan minor pada pengalaman penggunaan desktop.`
  }

  function ensureUpdaterStyle() {
    if (document.getElementById('desktop-updater-style')) return
    const style = document.createElement('style')
    style.id = 'desktop-updater-style'
    style.textContent = `
      .topbar-avatar.desktop-online {
        box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.9), 0 0 0 4px rgba(37, 99, 235, 0.24);
      }
      .topbar-avatar.desktop-offline {
        box-shadow: 0 0 0 2px rgba(220, 38, 38, 0.9), 0 0 0 4px rgba(220, 38, 38, 0.24);
      }
      .topbar-avatar-trigger.desktop-online .topbar-avatar {
        box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.9), 0 0 0 4px rgba(37, 99, 235, 0.24);
      }
      .topbar-avatar-trigger.desktop-offline .topbar-avatar {
        box-shadow: 0 0 0 2px rgba(220, 38, 38, 0.9), 0 0 0 4px rgba(220, 38, 38, 0.24);
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
    if (!avatarEl) avatarEl = document.getElementById('topbar-avatar')
    if (!avatarTriggerEl) avatarTriggerEl = document.querySelector('.topbar-user-trigger')
    if (!infoBtn) infoBtn = document.getElementById('topbar-app-info-btn')
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
    if (!window.openDesktopReleaseInfo) {
      window.openDesktopReleaseInfo = async function openDesktopReleaseInfo() {
      await hydrateLatestReleaseInfo()
      syncVersionFromStorage()
      const meta = changelogOverlay?.querySelector('#desktop-release-meta')
      if (meta) {
        const current = releaseInfoState.currentVersion || '-'
        const latest = releaseInfoState.latestVersion || current
        meta.textContent = `Versi saat ini: v${current} | Versi terbaru: v${latest}`
      }
      const noteVersion = releaseInfoState.currentVersion || releaseInfoState.latestVersion
      if (noteVersion) await ensureReleaseNotesForVersion(noteVersion)
      if (changelogBody) {
        const resolvedNote = noteVersion ? (releaseInfoState.notesByVersion[noteVersion] || '') : ''
        changelogBody.textContent = resolvedNote || releaseInfoState.notes || getFallbackReleaseNotes(noteVersion)
      }
      changelogOverlay?.classList.add('open')
      }
    }
    if (infoBtn && infoBtn.dataset.boundInfoClick !== '1') {
      infoBtn.dataset.boundInfoClick = '1'
      infoBtn.addEventListener('click', () => {
        if (typeof window.openDesktopReleaseInfo === 'function') window.openDesktopReleaseInfo()
      })
    }
  }

  function setOnlineIndicator() {
    ensureUpdaterElements()
    const isOnline = navigator.onLine
    lastOnlineState = isOnline
    avatarEl = document.getElementById('topbar-avatar')
    avatarTriggerEl = document.querySelector('.topbar-user-trigger')
    if (!avatarEl && !avatarTriggerEl) return
    avatarEl?.classList.toggle('desktop-online', isOnline)
    avatarEl?.classList.toggle('desktop-offline', !isOnline)
    avatarTriggerEl?.classList.toggle('desktop-online', isOnline)
    avatarTriggerEl?.classList.toggle('desktop-offline', !isOnline)
  }

  function bindAvatarPersistence() {
    const host = document.querySelector('.topbar-user-menu-wrap') || document.body
    if (!host || host.dataset.desktopStatusObserver === '1') return
    host.dataset.desktopStatusObserver = '1'
    const observer = new MutationObserver(() => {
      if (typeof lastOnlineState === 'boolean') {
        setOnlineIndicator()
      }
    })
    observer.observe(host, { childList: true, subtree: true })
  }

  function syncVersionFromStorage() {
    if (!releaseInfoState.currentVersion) {
      const stored = String(localStorage.getItem('desktop_app_version') || '').trim()
      if (stored) releaseInfoState.currentVersion = stored
    }
    if (!releaseInfoState.currentVersion && releaseInfoState.latestVersion) {
      releaseInfoState.currentVersion = releaseInfoState.latestVersion
    }
    if (!releaseInfoState.latestVersion && releaseInfoState.currentVersion) {
      releaseInfoState.latestVersion = releaseInfoState.currentVersion
    }
    if (releaseInfoState.currentVersion) {
      localStorage.setItem('desktop_app_version', releaseInfoState.currentVersion)
    }
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
        if (notes && !isGenericReleaseNotes(notes)) {
          releaseInfoState.notes = notes
          if (latestVersion) releaseInfoState.notesByVersion[latestVersion] = notes
        } else if (latestVersion) {
          const fallback = getFallbackReleaseNotes(latestVersion)
          releaseInfoState.notes = fallback
          releaseInfoState.notesByVersion[latestVersion] = fallback
        }
        if (!releaseInfoState.currentVersion && latestVersion) releaseInfoState.currentVersion = latestVersion
        if (latestVersion) await ensureReleaseNotesForVersion(latestVersion)
        renderVersionLabel()
      }
    } catch (_error) {}
  }

  async function fetchReleaseBodyByTag(version) {
    const clean = cleanVersion(version)
    if (!clean) return ''
    try {
      const res = await fetch(`https://api.github.com/repos/timakademikmim/mim-app/releases/tags/v${encodeURIComponent(clean)}`, {
        cache: 'no-store',
        headers: { Accept: 'application/vnd.github+json' }
      })
      if (!res.ok) return ''
      const info = await res.json()
      const body = String(info?.body || '').trim()
      if (isGenericReleaseNotes(body)) return ''
      return body
    } catch (_error) {
      return ''
    }
  }

  async function ensureReleaseNotesForVersion(version) {
    const clean = cleanVersion(version)
    if (!clean) return
    if (releaseInfoState.notesByVersion[clean]) return
    const notes = await fetchReleaseBodyByTag(clean)
    if (notes) {
      releaseInfoState.notesByVersion[clean] = notes
      if (releaseInfoState.latestVersion === clean || !releaseInfoState.notes) {
        releaseInfoState.notes = notes
      }
      return
    }
    const fallback = getFallbackReleaseNotes(clean)
    releaseInfoState.notesByVersion[clean] = fallback
    if (releaseInfoState.latestVersion === clean && releaseInfoState.notes) {
      releaseInfoState.notesByVersion[clean] = isGenericReleaseNotes(releaseInfoState.notes)
        ? fallback
        : releaseInfoState.notes
    }
  }

  function renderVersionLabel() {
    ensureUpdaterElements()
    syncVersionFromStorage()
    if (infoBtn) infoBtn.textContent = 'Info'
  }

  function updateDesktopUpdaterUi(detail) {
    ensureUpdaterElements()
    const stage = String(detail?.stage || '').trim().toLowerCase()
    const currentVersion = String(detail?.currentVersion || '').trim()
    const latestVersion = String(detail?.latestVersion || '').trim()
    const notes = String(detail?.notes || '').trim()
    if (currentVersion) releaseInfoState.currentVersion = currentVersion
    if (latestVersion) releaseInfoState.latestVersion = latestVersion
    if (notes && !isGenericReleaseNotes(notes)) {
      releaseInfoState.notes = notes
      if (latestVersion) releaseInfoState.notesByVersion[latestVersion] = notes
    } else if (latestVersion) {
      const fallback = getFallbackReleaseNotes(latestVersion)
      releaseInfoState.notes = fallback
      releaseInfoState.notesByVersion[latestVersion] = fallback
    }
    if (releaseInfoState.currentVersion) localStorage.setItem('desktop_app_version', releaseInfoState.currentVersion)
    renderVersionLabel()

    if (updaterLockWatchdog) {
      clearTimeout(updaterLockWatchdog)
      updaterLockWatchdog = null
    }

    const isLock = stage === 'downloading' || stage === 'installing'
    updateLockOverlay.classList.toggle('active', isLock)
    if (isLock) {
      updaterLockWatchdog = setTimeout(() => {
        updateLockOverlay?.classList.remove('active')
      }, 120000)
    }

    if (stage === 'no_update') {
      updateLockOverlay.classList.remove('active')
      return
    }

    if (stage === 'error') {
      updateLockOverlay.classList.remove('active')
      return
    }

    if (stage === 'ready_restart') {
      updateLockOverlay.classList.remove('active')
    }
  }

  ensureUpdaterElements()
  syncVersionFromStorage()
  bindAvatarPersistence()
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
