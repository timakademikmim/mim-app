
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

async function login() {
  const id = document.getElementById('id_karyawan').value.trim()
  const pass = document.getElementById('password').value.trim()
  const errorEl = document.getElementById('error')

  const { data } = await sb
    .from('karyawan')
    .select('id_karyawan, role')
    .eq('id_karyawan', id)
    .eq('password', pass)
    .maybeSingle()

  if (!data) {
    errorEl.textContent = 'ID atau password salah'
    return
  }

  const roles = parseRoleList(data.role)
  if (roles.length === 0) {
    errorEl.textContent = 'Role pengguna belum diatur'
    return
  }

  const landingPage = getLandingPageByRoles(roles)
  if (!landingPage) {
    errorEl.textContent = 'Role tidak dikenal'
    return
  }

  // simpan session
  localStorage.setItem('login_id', data.id_karyawan)
  localStorage.setItem('login_role', roles[0])
  localStorage.setItem('login_roles', JSON.stringify(roles))
  localStorage.setItem('admin_force_dashboard_once', '1')

  // redirect sesuai role prioritas
  location.href = landingPage
}



