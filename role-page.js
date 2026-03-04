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
  let updaterBadge = null
  let updaterLabel = null
  let updaterProgress = null
  let updateLockOverlay = null

  function ensureUpdaterStyle() {
    if (document.getElementById('desktop-updater-style')) return
    const style = document.createElement('style')
    style.id = 'desktop-updater-style'
    style.textContent = `
      .desktop-updater-badge {
        display: none;
        align-items: center;
        gap: 8px;
        height: 38px;
        padding: 0 12px;
        border-radius: 12px;
        border: 1px solid #cbd5e1;
        background: #ffffff;
        color: #0f172a;
        font-size: 12px;
        font-weight: 700;
      }
      .desktop-updater-badge.active {
        display: inline-flex;
      }
      .desktop-updater-dot {
        width: 8px;
        height: 8px;
        border-radius: 999px;
        background: #3b82f6;
        animation: desktop-updater-pulse 1s ease-in-out infinite;
      }
      .desktop-updater-progress {
        width: 64px;
        height: 5px;
        border-radius: 999px;
        background: #e2e8f0;
        overflow: hidden;
      }
      .desktop-updater-progress > i {
        display: block;
        width: 0%;
        height: 100%;
        background: #22c55e;
        transition: width 0.2s ease;
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
      @keyframes desktop-updater-pulse {
        0%, 100% { opacity: 1; transform: scale(1); }
        50% { opacity: 0.45; transform: scale(1.2); }
      }
    `
    document.head.appendChild(style)
  }

  function ensureUpdaterElements() {
    ensureUpdaterStyle()
    if (!updaterBadge) {
      updaterBadge = document.createElement('div')
      updaterBadge.className = 'desktop-updater-badge'
      updaterBadge.innerHTML = `
        <span class="desktop-updater-dot"></span>
        <span class="desktop-updater-label">Memeriksa update...</span>
        <span class="desktop-updater-progress"><i></i></span>
      `
      updaterLabel = updaterBadge.querySelector('.desktop-updater-label')
      updaterProgress = updaterBadge.querySelector('.desktop-updater-progress > i')
      const topbarRight = document.querySelector('.topbar-right') || document.querySelector('.topbar') || document.body
      topbarRight.appendChild(updaterBadge)
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
  }

  function updateDesktopUpdaterUi(detail) {
    ensureUpdaterElements()
    const stage = String(detail?.stage || '').trim().toLowerCase()
    const message = String(detail?.message || '').trim() || 'Memproses pembaruan...'
    const progress = Number(detail?.progress)
    const hasProgress = Number.isFinite(progress)

    updaterLabel.textContent = message
    if (hasProgress) {
      updaterProgress.style.width = `${Math.max(0, Math.min(100, progress))}%`
    }

    const isLock = stage === 'downloading' || stage === 'installing' || stage === 'ready_restart'
    updateLockOverlay.classList.toggle('active', isLock)

    const shouldShowBadge =
      stage === 'checking' ||
      stage === 'available' ||
      stage === 'downloading' ||
      stage === 'installing' ||
      stage === 'ready_restart' ||
      stage === 'error'
    updaterBadge.classList.toggle('active', shouldShowBadge)

    if (stage === 'no_update') {
      updaterLabel.textContent = message
      updaterProgress.style.width = '100%'
      updaterBadge.classList.add('active')
      setTimeout(() => {
        if (!updateLockOverlay.classList.contains('active')) {
          updaterBadge.classList.remove('active')
        }
      }, 1800)
      return
    }

    if (stage === 'error') {
      updaterBadge.style.borderColor = '#fecaca'
      updaterBadge.style.color = '#b91c1c'
      updaterProgress.style.background = '#fca5a5'
      updateLockOverlay.classList.remove('active')
      return
    }

    updaterBadge.style.borderColor = '#cbd5e1'
    updaterBadge.style.color = '#0f172a'
    updaterProgress.style.background = '#22c55e'
  }

  window.addEventListener('desktop-updater-status', event => {
    updateDesktopUpdaterUi(event?.detail || {})
  })
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
