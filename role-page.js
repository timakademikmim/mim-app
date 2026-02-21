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
} catch (e) {
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

const welcomeEl = document.getElementById('welcome')
if (welcomeEl) {
  welcomeEl.textContent = `Selamat datang ${id} (${roles.join(', ')})`
}

function initSidebarRoleSwitcher() {
  const sidebarEl = document.querySelector('.sidebar')
  if (!sidebarEl) return

  const roleList = [...new Set(roles)].filter(role => !!ROLE_PAGE_MAP[role])
  if (!roleList.length) return

  const activeRole = requiredRole && roleList.includes(requiredRole)
    ? requiredRole
    : roleList[0]

  const wrap = document.createElement('div')
  wrap.className = 'role-switcher'
  wrap.innerHTML = `
    <div class="role-switcher-label">Role</div>
    <button type="button" class="role-switcher-toggle" aria-expanded="false" aria-label="Pilih role">
      <span class="role-switcher-current">${activeRole}</span>
      <span class="role-switcher-arrow">&#9662;</span>
    </button>
    <div class="role-switcher-menu" role="menu"></div>
  `

  const toggleBtn = wrap.querySelector('.role-switcher-toggle')
  const menuEl = wrap.querySelector('.role-switcher-menu')
  const currentEl = wrap.querySelector('.role-switcher-current')
  if (!toggleBtn || !menuEl || !currentEl) return

  const optionRoles = roleList.filter(role => role !== activeRole)
  if (!optionRoles.length) {
    wrap.classList.add('single-role')
    sidebarEl.appendChild(wrap)
    return
  }

  menuEl.innerHTML = optionRoles
    .map(role => `<button type="button" class="role-switcher-item" data-role="${role}" role="menuitem">${role}</button>`)
    .join('')

  const closeMenu = () => {
    wrap.classList.remove('open')
    toggleBtn.setAttribute('aria-expanded', 'false')
    menuEl.style.left = ''
    menuEl.style.top = ''
  }

  const placeMenu = () => {
    const rect = toggleBtn.getBoundingClientRect()
    const menuWidth = Math.max(menuEl.offsetWidth, 130)
    const menuHeight = Math.max(menuEl.offsetHeight, 40)
    const viewportW = window.innerWidth
    const viewportH = window.innerHeight

    let left = rect.right + 4
    let top = rect.top + (rect.height / 2) - (menuHeight / 2)

    if (left + menuWidth > viewportW - 8) {
      left = Math.max(8, rect.left - menuWidth - 4)
    }
    if (top < 8) top = 8
    if (top + menuHeight > viewportH - 8) {
      top = Math.max(8, viewportH - menuHeight - 8)
    }

    menuEl.style.left = `${Math.round(left)}px`
    menuEl.style.top = `${Math.round(top)}px`
  }

  toggleBtn.addEventListener('click', event => {
    event.stopPropagation()
    const willOpen = !wrap.classList.contains('open')
    if (!willOpen) {
      closeMenu()
      return
    }
    wrap.classList.add('open')
    toggleBtn.setAttribute('aria-expanded', 'true')
    placeMenu()
  })

  menuEl.addEventListener('click', event => {
    const btn = event.target.closest('.role-switcher-item')
    if (!btn) return
    const targetRole = String(btn.getAttribute('data-role') || '').trim().toLowerCase()
    const targetPage = ROLE_PAGE_MAP[targetRole]
    if (!targetRole || !targetPage) return
    currentEl.textContent = targetRole
    closeMenu()
    location.href = targetPage
  })

  document.addEventListener('click', event => {
    if (wrap.contains(event.target)) return
    closeMenu()
  })

  window.addEventListener('resize', () => {
    if (!wrap.classList.contains('open')) return
    placeMenu()
  })

  window.addEventListener('scroll', () => {
    if (!wrap.classList.contains('open')) return
    placeMenu()
  }, true)

  sidebarEl.appendChild(wrap)
}

initSidebarRoleSwitcher()

function logout() {
  localStorage.clear()
  location.replace('index.html')
}
