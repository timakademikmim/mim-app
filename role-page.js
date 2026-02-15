const id = localStorage.getItem('login_id')
const requiredRole = document.body.dataset.role
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

// belum login
if (!id || roles.length === 0) {
  location.replace('index.html')
}

// role salah
if (!roles.includes(String(requiredRole || '').trim().toLowerCase())) {
  location.replace('index.html')
}

document.getElementById('welcome').textContent =
  `Selamat datang ${id} (${roles.join(', ')})`

function logout() {
  localStorage.clear()
  location.replace('index.html')
}
