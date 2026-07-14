
const supabaseUrl = window.MIM_SUPABASE_URL
const supabaseKey = window.MIM_SUPABASE_ANON_KEY
const sb = window.mimSupabaseClient

function isAndroidApp() {
  const hasTauri = !!(window.__TAURI_INTERNALS__ || window.__TAURI__)
  return hasTauri && /android/i.test(String(navigator.userAgent || ''))
}

function initAndroidColdStart() {
  if (!isAndroidApp()) return false
  if (sessionStorage.getItem('android_session_started') === '1') return false
  const hasPendingChatOpen = !!String(localStorage.getItem('chat_open_thread_id') || '').trim()
  if (hasPendingChatOpen) {
    sessionStorage.setItem('android_session_started', '1')
    return false
  }
  sessionStorage.setItem('android_session_started', '1')

  localStorage.removeItem('last_open_page')
  localStorage.setItem('admin_force_dashboard_once', '1')
  localStorage.removeItem('admin_last_page')
  localStorage.removeItem('admin_last_page_params')
  localStorage.setItem('guru_last_page', 'dashboard')
  localStorage.setItem('muhaffiz_last_page', 'dashboard')
  localStorage.setItem('musyrif_last_page', 'dashboard')
  return true
}

const ANDROID_COLD_START = initAndroidColdStart()

function showAndroidLoginSplashIfLoggedIn() {
  if (!isAndroidApp()) return
  const loginId = String(localStorage.getItem('login_id') || '').trim()
  if (!loginId) return
  if (!document.body) return
  if (document.getElementById('android-login-splash')) return

  if (!document.getElementById('android-login-splash-style')) {
    const style = document.createElement('style')
    style.id = 'android-login-splash-style'
    style.textContent = `
      .login-page { visibility: hidden; }
      .android-login-splash {
        position: fixed;
        inset: 0;
        z-index: 12000;
        display: flex;
        align-items: center;
        justify-content: center;
        background: radial-gradient(560px 360px at 50% 20%, rgba(43, 140, 255, 0.18), transparent 60%), #f4f7fb;
      }
      .android-login-splash img {
        width: 116px;
        height: 116px;
        object-fit: contain;
      }
    `
    document.head.appendChild(style)
  }

  const splash = document.createElement('div')
  splash.id = 'android-login-splash'
  splash.className = 'android-login-splash'
  const logo = document.createElement('img')
  logo.alt = 'Logo MIM'
  logo.src = 'iconMIM-android.png'
  logo.addEventListener('error', () => {
    if (logo.dataset.fallback === '1') return
    logo.dataset.fallback = '1'
    logo.src = 'iconMIM.png'
  })
  splash.appendChild(logo)
  document.body.appendChild(splash)
}

showAndroidLoginSplashIfLoggedIn()

function parseRoleList(rawRole) {
  if (Array.isArray(rawRole)) {
    return rawRole
      .map(v => String(v || '').trim().toLowerCase())
      .filter(Boolean)
  }

  const text = String(rawRole || '').trim()
  if (!text) return []

  return text
    .split(/[,\|;]+/)
    .map(v => v.trim().toLowerCase())
    .filter(Boolean)
}

function getLandingPageByRoles(roles) {
  const roleToPage = {
    admin: 'admin.html',
    guru: 'guru.html',
    muhaffiz: 'muhaffiz.html',
    musyrif: 'musyrif.html'
  }

  const priority = ['admin', 'guru', 'muhaffiz', 'musyrif']
  const chosen = priority.find(role => roles.includes(role))
  return chosen ? roleToPage[chosen] : null
}

function getLandingPageByRole(role) {
  const roleToPage = {
    admin: 'admin.html',
    guru: 'guru.html',
    muhaffiz: 'muhaffiz.html',
    musyrif: 'musyrif.html'
  }
  return roleToPage[String(role || '').trim().toLowerCase()] || null
}

function getSavedLandingPage() {
  const loginId = String(localStorage.getItem('login_id') || '').trim()
  if (!loginId) return ''

  let roles = []
  try {
    roles = JSON.parse(localStorage.getItem('login_roles') || '[]')
  } catch (_error) {
    roles = []
  }
  if (!Array.isArray(roles) || roles.length === 0) {
    const single = String(localStorage.getItem('login_role') || '').trim()
    if (single) roles = [single]
  }
  const normalized = roles
    .map(item => String(item || '').trim().toLowerCase())
    .filter(Boolean)
  if (!normalized.length) return ''

  const activeRole = String(localStorage.getItem('login_role') || '').trim().toLowerCase()
  if (activeRole) {
    const page = getLandingPageByRole(activeRole)
    if (page) return page
  }
  return getLandingPageByRoles(normalized) || ''
}

function getSavedLastPage() {
  const loginId = String(localStorage.getItem('login_id') || '').trim()
  if (!loginId) return ''
  const raw = String(localStorage.getItem('last_open_page') || '').trim()
  if (!raw) return ''

  // Only allow known internal pages to avoid broken redirects.
  const allow = new Set([
    'admin.html',
    'guru.html',
    'wakasek-kurikulum.html',
    'muhaffiz.html',
    'musyrif.html',
    'pages/guru-detail.html',
    'pages/muhaffiz-detail.html',
    'pages/musyrif-detail.html'
  ])
  return allow.has(raw) ? raw : ''
}

function getKnownRoles(roles) {
  const known = ['admin', 'guru', 'muhaffiz', 'musyrif']
  return (roles || []).filter(role => known.includes(role))
}

function getRoleLabel(role) {
  const map = {
    admin: 'Admin',
    guru: 'Guru',
    muhaffiz: 'Muhaffiz',
    musyrif: 'Musyrif'
  }
  return map[String(role || '').trim().toLowerCase()] || role
}

function askRoleChoice(roles) {
  return new Promise(resolve => {
    const list = Array.isArray(roles) ? roles : []
    if (list.length <= 1) {
      resolve(list[0] || null)
      return
    }

    const old = document.getElementById('role-choice-overlay')
    if (old) old.remove()

    const overlay = document.createElement('div')
    overlay.id = 'role-choice-overlay'
    overlay.style.position = 'fixed'
    overlay.style.inset = '0'
    overlay.style.background = 'rgba(15, 23, 42, 0.35)'
    overlay.style.display = 'flex'
    overlay.style.alignItems = 'center'
    overlay.style.justifyContent = 'center'
    overlay.style.zIndex = '9999'
    overlay.innerHTML = `
      <div style="width:min(420px, calc(100vw - 24px)); background:#ffffff; border:1px solid #e2e8f0; border-radius:12px; padding:16px;">
        <div style="font-size:16px; font-weight:700; color:#0f172a; margin-bottom:6px;">Pilih role login</div>
        <div style="font-size:13px; color:#475569; margin-bottom:12px;">Akun ini memiliki lebih dari satu role. Masuk sebagai:</div>
        <div id="role-choice-actions" style="display:grid; gap:8px;"></div>
      </div>
    `
    document.body.appendChild(overlay)

    const actions = overlay.querySelector('#role-choice-actions')
    list.forEach(role => {
      const btn = document.createElement('button')
      btn.type = 'button'
      btn.textContent = getRoleLabel(role)
      btn.style.border = '1px solid #cbd5e1'
      btn.style.background = '#ffffff'
      btn.style.color = '#0f172a'
      btn.style.borderRadius = '999px'
      btn.style.padding = '10px 12px'
      btn.style.fontWeight = '600'
      btn.style.cursor = 'pointer'
      btn.onmouseenter = () => { btn.style.background = '#f8fafc' }
      btn.onmouseleave = () => { btn.style.background = '#ffffff' }
      btn.onclick = () => {
        overlay.remove()
        resolve(role)
      }
      actions.appendChild(btn)
    })
  })
}

function isActiveValue(value) {
  if (value === true || value === 1) return true
  const text = String(value ?? '').trim().toLowerCase()
  return text === 'true' || text === 't' || text === '1' || text === 'yes'
}

let loginTenants = []

function setLoginBusy(busy) {
  const button = document.getElementById('login-button')
  const googleButton = document.getElementById('google-login-button')
  const tenantSelect = document.getElementById('tenant_id')
  if (button) {
    button.disabled = !!busy
    button.textContent = busy ? 'Memeriksa akun...' : 'Masuk'
  }
  if (googleButton) googleButton.disabled = !!busy
  if (tenantSelect) tenantSelect.disabled = !!busy || loginTenants.length === 0
}

async function loadLoginTenants() {
  const select = document.getElementById('tenant_id')
  const errorEl = document.getElementById('error')
  if (!select) return
  const { data, error } = await sb.rpc('list_login_tenants')
  if (error) {
    loginTenants = []
    select.innerHTML = '<option value="">Unit tidak tersedia</option>'
    select.disabled = true
    if (errorEl) errorEl.textContent = 'Daftar unit sekolah belum dapat dimuat.'
    return
  }

  loginTenants = (Array.isArray(data) ? data : [])
    .map(item => ({
      id: String(item?.id || '').trim(),
      name: String(item?.name || '').trim(),
      officialName: String(item?.official_name || '').trim()
    }))
    .filter(item => item.id && item.name)
  const savedTenantId = String(localStorage.getItem('login_tenant_id') || '').trim()
  const escape = window.appEscapeHtml || (value => String(value ?? ''))
  select.innerHTML = loginTenants.length
    ? loginTenants.map(item => `<option value="${escape(item.id)}">${escape(item.name)}</option>`).join('')
    : '<option value="">Belum ada unit aktif</option>'
  if (savedTenantId && loginTenants.some(item => item.id === savedTenantId)) {
    select.value = savedTenantId
  }
  select.disabled = loginTenants.length === 0
}

async function sha256Hex(value) {
  if (!window.crypto?.subtle) throw new Error('Perangkat tidak mendukung login aman.')
  const bytes = new TextEncoder().encode(String(value || ''))
  const digest = await window.crypto.subtle.digest('SHA-256', bytes)
  return Array.from(new Uint8Array(digest))
    .map(byte => byte.toString(16).padStart(2, '0'))
    .join('')
}

async function buildInternalAuthEmail(tenantId, loginId) {
  const normalizedId = String(loginId || '').trim().toLowerCase().replace(/\s+/g, '')
  const hash = await sha256Hex(`${tenantId}:${normalizedId}`)
  return `${hash}@accounts.mim.invalid`
}

function clearAppStorageForScopeChange(nextScope) {
  const previousScope = String(
    localStorage.getItem('login_scope') || localStorage.getItem('last_security_scope') || ''
  ).trim()
  const hadPreviousLogin = !!String(localStorage.getItem('login_id') || '').trim()
  if ((!previousScope && !hadPreviousLogin) || previousScope === nextScope) return
  const removable = []
  for (let index = 0; index < localStorage.length; index += 1) {
    const key = String(localStorage.key(index) || '')
    if (key && !key.startsWith('sb-')) removable.push(key)
  }
  removable.forEach(key => localStorage.removeItem(key))
}

function clearWebLoginState() {
  const keys = [
    'login_id', 'login_name', 'login_role', 'login_roles', 'login_photo_url',
    'login_tenant_id', 'login_tenant_name', 'login_auth_mode', 'login_auth_user_id',
    'login_scope'
  ]
  keys.forEach(key => localStorage.removeItem(key))
}

function saveWebLoginState({ employee, roles, selectedRole, tenant, authMode, authUserId }) {
  const loginId = String(employee?.id_karyawan || '').trim()
  const scope = `${tenant.id}:${loginId.toLowerCase()}`
  clearAppStorageForScopeChange(scope)
  localStorage.setItem('login_id', loginId)
  localStorage.setItem('login_name', String(employee?.nama || '').trim())
  localStorage.setItem('login_role', selectedRole)
  localStorage.setItem('login_roles', JSON.stringify(roles))
  localStorage.setItem('login_tenant_id', tenant.id)
  localStorage.setItem('login_tenant_name', tenant.name)
  localStorage.setItem('login_auth_mode', authMode)
  localStorage.setItem('login_auth_user_id', String(authUserId || ''))
  localStorage.setItem('login_scope', scope)
  localStorage.setItem('last_security_scope', scope)
  localStorage.setItem('admin_force_dashboard_once', '1')
}

async function fetchAuthenticatedEmployee(session, tenant) {
  if (!session?.user?.id) return null
  const { data, error } = await sb
    .from('karyawan')
    .select('id,id_karyawan,nama,role,aktif,tenant_id,auth_user_id')
    .eq('auth_user_id', session.user.id)
    .eq('tenant_id', tenant.id)
    .maybeSingle()
  if (error) throw error
  return data || null
}

async function fetchActiveEmployeeForAuthUser(session) {
  if (!session?.user?.id) return null
  const { data, error } = await sb
    .from('karyawan')
    .select('id,id_karyawan,nama,role,aktif,tenant_id,auth_user_id')
    .eq('auth_user_id', session.user.id)
    .limit(2)
  if (error) throw error
  const activeRows = (Array.isArray(data) ? data : []).filter(row => isActiveValue(row?.aktif))
  if (activeRows.length > 1) {
    throw new Error('Akun Google ini terhubung ke lebih dari satu profil aktif. Silakan hubungi admin.')
  }
  return activeRows[0] || null
}

async function fetchTenantById(tenantId) {
  const id = String(tenantId || '').trim()
  if (!id) return null
  const cached = loginTenants.find(item => item.id === id)
  if (cached) return cached
  const { data, error } = await sb
    .from('tenants')
    .select('id,name,official_name,active')
    .eq('id', id)
    .maybeSingle()
  if (error) throw error
  if (!data || !isActiveValue(data.active)) return null
  return {
    id: String(data.id || '').trim(),
    name: String(data.name || '').trim(),
    officialName: String(data.official_name || '').trim()
  }
}

async function ensureWebMfaChallenge() {
  const { data: factorsData, error: factorsError } = await sb.auth.mfa.listFactors()
  if (factorsError) throw factorsError
  const factor = (factorsData?.totp || []).find(item => String(item?.status || '').toLowerCase() === 'verified')
  if (!factor?.id) return
  const { data: aalData, error: aalError } = await sb.auth.mfa.getAuthenticatorAssuranceLevel()
  if (aalError) throw aalError
  if (aalData?.currentLevel === 'aal2') return
  const code = String(window.prompt('Masukkan 6 digit kode dari aplikasi authenticator:') || '').trim()
  if (!/^\d{6}$/.test(code)) throw new Error('Kode authenticator wajib berisi 6 digit.')
  const { error } = await sb.auth.mfa.challengeAndVerify({ factorId: factor.id, code })
  if (error) throw new Error('Kode authenticator salah atau sudah kedaluwarsa.')
}

async function completeGoogleLoginFromCurrentSession() {
  const { data, error } = await sb.auth.getSession()
  const session = data?.session || null
  if (error || !session?.user?.id) return false
  await ensureWebMfaChallenge()
  const employee = await fetchActiveEmployeeForAuthUser(session)
  if (!employee) {
    await sb.auth.signOut({ scope: 'local' }).catch(() => undefined)
    throw new Error('Akun Google ini belum ditautkan ke akun karyawan aktif.')
  }
  const tenant = await fetchTenantById(employee.tenant_id)
  if (!tenant) {
    await sb.auth.signOut({ scope: 'local' }).catch(() => undefined)
    throw new Error('Unit sekolah untuk akun ini tidak aktif atau tidak ditemukan.')
  }
  return completeWebLogin(employee, tenant, 'auth', session.user.id)
}

async function completeWebLogin(employee, tenant, authMode, authUserId = '') {
  if (!employee || !isActiveValue(employee.aktif)) {
    throw new Error(employee ? 'Akun nonaktif. Silakan hubungi admin.' : 'ID atau password salah')
  }
  const roles = parseRoleList(employee.role)
  const knownRoles = getKnownRoles(roles)
  if (!knownRoles.length) throw new Error('Role pengguna belum diatur atau tidak dikenal')

  const selectedRole = knownRoles.length > 1
    ? await askRoleChoice(knownRoles)
    : knownRoles[0]
  if (!selectedRole) return false
  const landingPage = getLandingPageByRole(selectedRole)
  if (!landingPage) throw new Error('Role tidak dikenal')

  saveWebLoginState({ employee, roles, selectedRole, tenant, authMode, authUserId })
  location.href = landingPage
  return true
}

async function loginWithGoogle() {
  const errorEl = document.getElementById('error')
  if (errorEl) errorEl.textContent = ''
  setLoginBusy(true)
  try {
    const previousScope = String(localStorage.getItem('login_scope') || '').trim()
    if (previousScope) localStorage.setItem('last_security_scope', previousScope)
    clearWebLoginState()
    await sb.auth.signOut({ scope: 'local' }).catch(() => undefined)
    const redirectTo = `${window.location.origin}${window.location.pathname}`
    const { error } = await sb.auth.signInWithOAuth({
      provider: 'google',
      options: { redirectTo }
    })
    if (error) throw error
  } catch (error) {
    if (errorEl) errorEl.textContent = String(error?.message || 'Login Google belum dapat dibuka.')
    setLoginBusy(false)
  }
}

async function login() {
  const id = document.getElementById('id_karyawan').value.trim()
  const pass = document.getElementById('password').value
  const tenantId = String(document.getElementById('tenant_id')?.value || '').trim()
  const errorEl = document.getElementById('error')
  const tenant = loginTenants.find(item => item.id === tenantId)
  errorEl.textContent = ''
  if (!tenant) {
    errorEl.textContent = 'Pilih unit sekolah terlebih dahulu.'
    return
  }
  if (!id || !pass) {
    errorEl.textContent = 'ID Karyawan dan password wajib diisi.'
    return
  }

  setLoginBusy(true)
  try {
    const previousScope = String(localStorage.getItem('login_scope') || '').trim()
    if (previousScope) localStorage.setItem('last_security_scope', previousScope)
    clearWebLoginState()
    await sb.auth.signOut({ scope: 'local' }).catch(() => undefined)
    const email = await buildInternalAuthEmail(tenant.id, id)
    const { data: authData, error: authError } = await sb.auth.signInWithPassword({ email, password: pass })
    if (authError || !authData?.session) throw new Error('ID atau password salah')
    await ensureWebMfaChallenge()
    const employee = await fetchAuthenticatedEmployee(authData.session, tenant)
    if (!employee) throw new Error('Akun belum terhubung ke data karyawan unit ini.')
    await completeWebLogin(employee, tenant, 'auth', authData.session.user.id)
  } catch (error) {
    errorEl.textContent = String(error?.message || 'Login gagal. Coba lagi.')
  } finally {
    setLoginBusy(false)
  }
}

async function requestPasswordReset() {
  const tenantId = String(document.getElementById('tenant_id')?.value || '').trim()
  const loginId = String(document.getElementById('id_karyawan')?.value || '').trim()
  const errorEl = document.getElementById('error')
  if (!tenantId || !loginId) {
    if (errorEl) errorEl.textContent = 'Pilih unit dan isi ID Karyawan terlebih dahulu.'
    return
  }
  const { data, error } = await sb.functions.invoke('request-password-reset', {
    body: { tenant_id: tenantId, login_id: loginId }
  })
  if (error) {
    if (errorEl) errorEl.textContent = 'Permintaan reset belum dapat dikirim. Coba lagi.'
    return
  }
  if (errorEl) errorEl.textContent = String(data?.message || 'Permintaan reset telah dikirim kepada admin unit.')
}

async function resumeSavedAuthSession() {
  const authMode = String(localStorage.getItem('login_auth_mode') || '').trim().toLowerCase()
  if (authMode !== 'auth') return true
  const tenantId = String(localStorage.getItem('login_tenant_id') || '').trim()
  const tenant = {
    id: tenantId,
    name: String(localStorage.getItem('login_tenant_name') || '').trim()
  }
  const { data, error } = await sb.auth.getSession()
  const session = data?.session || null
  if (error || !session || !tenant.id) {
    await sb.auth.signOut({ scope: 'local' }).catch(() => undefined)
    clearWebLoginState()
    return false
  }
  try {
    const { data: tenantRow, error: tenantError } = await sb
      .from('tenants')
      .select('id,name,active')
      .eq('id', tenant.id)
      .maybeSingle()
    if (tenantError || !tenantRow || !isActiveValue(tenantRow.active)) {
      throw new Error('Unit sekolah tidak aktif.')
    }
    const employee = await fetchAuthenticatedEmployee(session, tenant)
    if (!employee || !isActiveValue(employee.aktif)) throw new Error('Sesi akun tidak aktif.')
    const roles = parseRoleList(employee.role)
    const activeRole = String(localStorage.getItem('login_role') || '').trim().toLowerCase()
    if (!roles.includes(activeRole)) localStorage.setItem('login_role', getKnownRoles(roles)[0] || '')
    localStorage.setItem('login_id', String(employee.id_karyawan || '').trim())
    localStorage.setItem('login_name', String(employee.nama || '').trim())
    localStorage.setItem('login_roles', JSON.stringify(roles))
    localStorage.setItem('login_auth_user_id', session.user.id)
    return true
  } catch (_error) {
    await sb.auth.signOut({ scope: 'local' }).catch(() => undefined)
    clearWebLoginState()
    return false
  }
}

document.addEventListener('DOMContentLoaded', async () => {
  try {
    if (!String(localStorage.getItem('login_id') || '').trim()) {
      const completedGoogleLogin = await completeGoogleLoginFromCurrentSession()
      if (completedGoogleLogin) return
    }
  } catch (error) {
    const errorEl = document.getElementById('error')
    if (errorEl) errorEl.textContent = String(error?.message || 'Login Google gagal.')
  }
  const canResume = await resumeSavedAuthSession()
  if (!canResume) {
    await loadLoginTenants()
    document.documentElement.classList.remove('android-preboot')
    return
  }
  const lastPage = ANDROID_COLD_START ? '' : getSavedLastPage()
  if (lastPage) {
    location.replace(lastPage)
    return
  }
  const target = getSavedLandingPage()
  if (target) {
    location.replace(target)
    return
  }
  await loadLoginTenants()
  document.documentElement.classList.remove('android-preboot')
})



