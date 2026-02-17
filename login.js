
const supabaseUrl = 'https://optucpelkueqmlhwlbej.supabase.co'
const supabaseKey = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9wdHVjcGVsa3VlcW1saHdsYmVqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzAxOTY4MTgsImV4cCI6MjA4NTc3MjgxOH0.Vqaey9pcnltu9uRbPk0J-AGWaGDZjQLw92pcRv67GNE'
const sb = supabase.createClient(supabaseUrl, supabaseKey)

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

async function login() {
  const id = document.getElementById('id_karyawan').value.trim()
  const pass = document.getElementById('password').value.trim()
  const errorEl = document.getElementById('error')

  const { data } = await sb
    .from('karyawan')
    .select('id_karyawan, nama, role, aktif')
    .eq('id_karyawan', id)
    .eq('password', pass)
    .maybeSingle()

  if (!data) {
    errorEl.textContent = 'ID atau password salah'
    return
  }

  if (!isActiveValue(data.aktif)) {
    errorEl.textContent = 'Akun nonaktif. Silakan hubungi admin.'
    return
  }

  const roles = parseRoleList(data.role)
  if (roles.length === 0) {
    errorEl.textContent = 'Role pengguna belum diatur'
    return
  }

  const knownRoles = getKnownRoles(roles)
  if (knownRoles.length === 0) {
    errorEl.textContent = 'Role tidak dikenal'
    return
  }

  let selectedRole = knownRoles[0]
  if (knownRoles.length > 1) {
    selectedRole = await askRoleChoice(knownRoles)
    if (!selectedRole) return
  }

  const landingPage = getLandingPageByRole(selectedRole)
  if (!landingPage) {
    errorEl.textContent = 'Role tidak dikenal'
    return
  }

  // simpan session
  localStorage.setItem('login_id', data.id_karyawan)
  localStorage.setItem('login_name', String(data.nama || '').trim())
  localStorage.setItem('login_role', selectedRole)
  localStorage.setItem('login_roles', JSON.stringify(knownRoles))
  localStorage.setItem('admin_force_dashboard_once', '1')

  // redirect sesuai role pilihan
  location.href = landingPage
}



