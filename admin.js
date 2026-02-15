// =======================
// SUPABASE INIT
// =======================
const supabaseUrl = 'https://optucpelkueqmlhwlbej.supabase.co'
const supabaseKey = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9wdHVjcGVsa3VlcW1saHdsYmVqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzAxOTY4MTgsImV4cCI6MjA4NTc3MjgxOH0.Vqaey9pcnltu9uRbPk0J-AGWaGDZjQLw92pcRv67GNE'
const sb = supabase.createClient(supabaseUrl, supabaseKey)
const externalPageHtmlCache = {}
const externalPageScriptLoaded = {}
const pageDataCache = window.__pageDataCache || {}
window.__pageDataCache = pageDataCache

window.getCachedData = function getCachedData(key, ttlMs) {
  const entry = pageDataCache[key]
  if (!entry) return null
  if (Date.now() - entry.ts > ttlMs) return null
  return entry.data
}

window.setCachedData = function setCachedData(key, data) {
  pageDataCache[key] = { ts: Date.now(), data }
}

window.clearCachedData = function clearCachedData(key) {
  delete pageDataCache[key]
}

function setupCustomPopupSystem() {
  if (window.__popupReady) return

  const overlay = document.getElementById('app-popup-overlay')
  const messageEl = document.getElementById('app-popup-message')
  const actionsEl = document.getElementById('app-popup-actions')
  const okBtn = document.getElementById('app-popup-ok-btn')
  if (!overlay || !messageEl || !actionsEl || !okBtn) return

  const closePopup = () => {
    overlay.classList.remove('open')
    overlay.setAttribute('aria-hidden', 'true')
    actionsEl.innerHTML = ''
  }

  window.showPopupMessage = function showPopupMessage(message) {
    return new Promise(resolve => {
      messageEl.textContent = String(message ?? '')
      actionsEl.innerHTML = ''

      const btn = document.createElement('button')
      btn.type = 'button'
      btn.textContent = 'OK'
      btn.className = 'app-popup-primary'
      btn.onclick = () => {
        closePopup()
        resolve(true)
      }
      actionsEl.appendChild(btn)

      overlay.classList.add('open')
      overlay.setAttribute('aria-hidden', 'false')
      btn.focus()
    })
  }

  window.showPopupConfirm = function showPopupConfirm(message) {
    return new Promise(resolve => {
      messageEl.textContent = String(message ?? '')
      actionsEl.innerHTML = ''

      const cancelBtn = document.createElement('button')
      cancelBtn.type = 'button'
      cancelBtn.textContent = 'Batal'
      cancelBtn.onclick = () => {
        closePopup()
        resolve(false)
      }

      const okConfirmBtn = document.createElement('button')
      okConfirmBtn.type = 'button'
      okConfirmBtn.textContent = 'Ya'
      okConfirmBtn.className = 'app-popup-primary'
      okConfirmBtn.onclick = () => {
        closePopup()
        resolve(true)
      }

      actionsEl.appendChild(cancelBtn)
      actionsEl.appendChild(okConfirmBtn)

      overlay.classList.add('open')
      overlay.setAttribute('aria-hidden', 'false')
      okConfirmBtn.focus()
    })
  }

  window.alert = function customAlert(message) {
    window.showPopupMessage(message)
  }

  overlay.addEventListener('click', event => {
    if (event.target !== overlay) return
    const cancelButton = actionsEl.querySelector('button:not(.app-popup-primary)')
    if (cancelButton) {
      cancelButton.click()
      return
    }
    const primaryButton = actionsEl.querySelector('button.app-popup-primary') || actionsEl.querySelector('button')
    if (primaryButton) primaryButton.click()
  })

  window.__popupReady = true
}


// =======================
// LOGIN CHECK
// =======================
const loginId = localStorage.getItem('login_id')

if (!loginId) {
  location.href = 'index.html'
}

document.addEventListener('DOMContentLoaded', () => {
  setupCustomPopupSystem()

  const forceDashboardOnce = localStorage.getItem('admin_force_dashboard_once') === '1'

  if (forceDashboardOnce) {
    localStorage.removeItem('admin_force_dashboard_once')
    localStorage.removeItem('admin_last_page')
    localStorage.removeItem('admin_last_page_params')
    loadPage('dashboard')
    return
  }

  const lastPage = localStorage.getItem('admin_last_page') || 'dashboard'
  let lastParams = {}
  try {
    lastParams = JSON.parse(localStorage.getItem('admin_last_page_params') || '{}')
  } catch (e) {
    lastParams = {}
  }
  loadPage(lastPage, lastParams)
})


// =======================
// LOGOUT
// =======================
function logout() {
  localStorage.removeItem('login_id')
  localStorage.removeItem('admin_force_dashboard_once')
  localStorage.removeItem('admin_last_page')
  localStorage.removeItem('admin_last_page_params')
  location.href = 'index.html'
}

function toggleTopbarUserMenu() {
  const menu = document.getElementById('topbar-user-menu')
  if (!menu) return
  menu.classList.toggle('open')
}

function closeTopbarUserMenu() {
  const menu = document.getElementById('topbar-user-menu')
  if (!menu) return
  menu.classList.remove('open')
}

function setupTopbarUserMenuHandlers() {
  if (document.body.dataset.topbarMenuBound === 'true') return

  document.addEventListener('click', event => {
    const wrap = document.querySelector('.topbar-user-menu-wrap')
    if (!wrap) return
    if (wrap.contains(event.target)) return
    closeTopbarUserMenu()
  })

  document.body.dataset.topbarMenuBound = 'true'
}

function setActiveSidebarTab(page) {
  const isAkademikGroup =
    page === 'kelas' ||
    page === 'kelas-distribusi-mapel' ||
    page === 'kelas-guru-mapel' ||
    page === 'jadwal' ||
    page === 'santri' ||
    page === 'santri-detail' ||
    page === 'guru' ||
    page === 'guru-detail'

  const navButtons = document.querySelectorAll('.sidebar-nav-btn')
  navButtons.forEach(button => {
    const btnPage = button.getAttribute('data-page')
    if (btnPage === 'akademik') {
      button.classList.toggle('active', isAkademikGroup)
      return
    }
    button.classList.toggle('active', btnPage === page)
  })

  const submenuButtons = document.querySelectorAll('.sidebar-submenu-btn')
  submenuButtons.forEach(button => {
    const btnPage = button.getAttribute('data-page')
    const isSantriDetail = page === 'santri-detail' && btnPage === 'santri'
    const isGuruDetail = page === 'guru-detail' && btnPage === 'guru'
    const isLegacyDistribusi = page === 'kelas-distribusi-mapel' && btnPage === 'kelas-guru-mapel'
    button.classList.toggle('active', btnPage === page || isSantriDetail || isGuruDetail || isLegacyDistribusi)
  })

  setAkademikSidebarMenuExpanded(isAkademikGroup)
}

function setTopbarTitle(page) {
  const titleMap = {
    dashboard: 'Dashboard',
    'tahun-ajaran': 'Tahun Ajaran',
    kelas: 'Data Kelas',
    'kelas-distribusi-mapel': 'Distribusi Mapel',
    'kelas-guru-mapel': 'Data Mapel',
    jadwal: 'Jadwal Pelajaran',
    santri: 'Data Siswa',
    'santri-detail': 'Detail Santri',
    guru: 'Data Guru',
    'guru-detail': 'Detail Guru',
    karyawan: 'Data Karyawan'
  }
  const titleEl = document.getElementById('topbar-title')
  if (!titleEl) return
  titleEl.textContent = titleMap[page] || 'Admin Panel'
}

function setAkademikSidebarMenuExpanded(expanded) {
  const submenu = document.getElementById('sidebar-akademik-submenu')
  if (!submenu) return
  submenu.classList.toggle('open', expanded)

  const parentBtn = document.querySelector('.sidebar-parent-btn[data-page="akademik"]')
  if (parentBtn) {
    parentBtn.classList.toggle('expanded', expanded)
  }
}

function toggleAkademikSidebarMenu() {
  const submenu = document.getElementById('sidebar-akademik-submenu')
  if (!submenu) return
  const willExpand = !submenu.classList.contains('open')
  setAkademikSidebarMenuExpanded(willExpand)
}

function getAkademikPageFromSubtab(subtab) {
  if (subtab === 'kelas') return 'kelas'
  if (subtab === 'jadwal') return 'jadwal'
  if (subtab === 'santri') return 'santri'
  if (subtab === 'guru') return 'guru'
  if (subtab === 'distribusi-mapel') return 'kelas-distribusi-mapel'
  if (subtab === 'guru-mapel') return 'kelas-guru-mapel'
  return 'kelas'
}

function loadAkademikFromSidebar(subtab) {
  const page = getAkademikPageFromSubtab(subtab)
  loadPage(page, { subtab })
}

window.syncKelasSidebarSelection = function syncKelasSidebarSelection(subtab) {
  const page = getAkademikPageFromSubtab(subtab)
  localStorage.setItem('admin_last_page', page)
  localStorage.setItem('admin_last_page_params', JSON.stringify({ subtab }))
  setActiveSidebarTab(page)
  setTopbarTitle(page)
}


// =======================
// PAGE ROUTER
// =======================
function loadPage(page, params = {}) {
  localStorage.setItem('admin_last_page', page);
  localStorage.setItem('admin_last_page_params', JSON.stringify(params));
  const area = document.getElementById('content-area')
  const welcomeEl = document.getElementById('welcome')
  if (!area) return

  if (welcomeEl) {
    welcomeEl.textContent = loginId
  }

  setActiveSidebarTab(page)
  setTopbarTitle(page)
  setupTopbarUserMenuHandlers()
  closeTopbarUserMenu()

  switch (page) {
    case 'dashboard':
      area.innerHTML = `
        <p>Selamat datang di panel admin.</p>
      `
      break
    case 'tahun-ajaran':
      loadExternalPage('tahun-ajaran')
      break
    case 'kelas':
      loadExternalPage('kelas', { subtab: 'data-kelas' })
      break
    case 'kelas-distribusi-mapel':
      loadExternalPage('kelas', { subtab: 'distribusi-mapel' })
      break
    case 'kelas-guru-mapel':
      loadExternalPage('kelas', { subtab: 'guru-mapel' })
      break
    case 'santri':
      loadExternalPage('santri')
      break
    case 'jadwal':
      loadExternalPage('jadwal')
      break
    case 'santri-detail':
      loadExternalPage('santri-detail', params)
      break
    case 'karyawan':
      loadExternalPage('karyawan', { mode: 'all' })
      break
    case 'guru':
      loadExternalPage('karyawan', { mode: 'guru-only' })
      break
    case 'guru-detail':
      loadExternalPage('guru-detail', params)
      break
  }
}


async function loadExternalPage(page, params = {}) {
  const area = document.getElementById('content-area')
  if (!area) return

  if (!externalPageHtmlCache[page]) {
    const response = await fetch(`pages/${page}.html`)
    externalPageHtmlCache[page] = await response.text()
  }
  area.innerHTML = externalPageHtmlCache[page]

  const runPageInit = () => {
    if (page === 'tahun-ajaran' && typeof initTahunAjaranPage === 'function') {
      initTahunAjaranPage()
      return
    }
    if (page === 'kelas' && typeof initKelasPage === 'function') {
      initKelasPage(params)
      return
    }
    if (page === 'santri' && typeof initSantriPage === 'function') {
      initSantriPage()
      return
    }
    if (page === 'santri-detail' && typeof initSantriDetailPage === 'function') {
      initSantriDetailPage(params)
      return
    }
    if (page === 'guru-detail' && typeof initGuruDetailPage === 'function') {
      initGuruDetailPage(params)
      return
    }
    if (page === 'jadwal' && typeof initJadwalPage === 'function') {
      initJadwalPage()
      return
    }
    if (page === 'karyawan' && typeof initKaryawanPage === 'function') {
      initKaryawanPage(params)
    }
  }

  if (externalPageScriptLoaded[page]) {
    runPageInit()
    return
  }

  const script = document.createElement('script')
  script.src = `pages/${page}.js`
  script.defer = true
  script.onload = () => {
    externalPageScriptLoaded[page] = true
    runPageInit()
  }
  document.body.appendChild(script)
}



