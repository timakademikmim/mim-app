;(function initSharedProfilePhotoUtils() {
  if (window.__sharedProfilePhotoUtilsReady) return

  function escape(value) {
    if (typeof window.appEscapeHtml === 'function') return window.appEscapeHtml(value)
    return String(value ?? '')
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#39;')
  }

  window.getProfileInitials = function getProfileInitials(nama) {
    const words = String(nama || '').trim().split(/\s+/).filter(Boolean)
    if (!words.length) return 'U'
    if (words.length === 1) return words[0].slice(0, 2).toUpperCase()
    return `${words[0][0] || ''}${words[1][0] || ''}`.toUpperCase()
  }

  window.getProfilePhotoFileExt = function getProfilePhotoFileExt(fileName = '') {
    const raw = String(fileName || '').trim().toLowerCase()
    const parts = raw.split('.')
    const ext = parts.length > 1 ? parts.pop() : ''
    if (!ext) return 'jpg'
    if (ext === 'jpeg') return 'jpg'
    if (ext === 'png' || ext === 'jpg' || ext === 'webp') return ext
    return 'jpg'
  }

  window.renderProfilePhotoPreview = function renderProfilePhotoPreview(previewId, fotoUrl, nama) {
    const box = document.getElementById(String(previewId || '').trim())
    if (!box) return
    const url = String(fotoUrl || '').trim()
    if (url) {
      box.innerHTML = `<img src="${escape(url)}" alt="Foto Profil" style="width:64px; height:64px; border-radius:999px; object-fit:cover; border:1px solid #cbd5e1;">`
      return
    }
    const initial = window.getProfileInitials(String(nama || ''))
    box.innerHTML = `<span style="width:64px; height:64px; border-radius:999px; display:inline-flex; align-items:center; justify-content:center; background:#e2e8f0; color:#0f172a; font-weight:700; border:1px solid #cbd5e1;">${escape(initial)}</span>`
  }

  window.uploadProfilePhotoShared = async function uploadProfilePhotoShared(config = {}) {
    const {
      event,
      sb,
      bucket,
      maxSizeBytes,
      idInputId,
      defaultId,
      fileUrlInputId,
      namaInputId,
      previewId
    } = config

    const file = event?.target?.files?.[0]
    if (!file) return { ok: false, reason: 'no_file' }

    try {
      if (!String(file.type || '').toLowerCase().startsWith('image/')) {
        throw new Error('File harus berupa gambar (JPG, PNG, WEBP).')
      }
      if (Number(file.size || 0) > Number(maxSizeBytes || 0)) {
        throw new Error('Ukuran gambar maksimal 300 KB.')
      }

      const idKaryawan = String(document.getElementById(String(idInputId || '').trim())?.value || defaultId || 'karyawan')
        .trim()
        .replaceAll(' ', '_')
      const ext = window.getProfilePhotoFileExt(file.name)
      const filePath = `${idKaryawan}_${Date.now()}.${ext}`
      const uploadRes = await sb.storage.from(String(bucket || '')).upload(filePath, file, { upsert: true })
      if (uploadRes.error) throw uploadRes.error

      const pub = sb.storage.from(String(bucket || '')).getPublicUrl(filePath)
      const fotoUrl = String(pub?.data?.publicUrl || '').trim()
      if (!fotoUrl) throw new Error('URL foto tidak valid.')

      const input = document.getElementById(String(fileUrlInputId || '').trim())
      if (input) input.value = fotoUrl
      const nama = String(document.getElementById(String(namaInputId || '').trim())?.value || '').trim()
      window.renderProfilePhotoPreview(previewId, fotoUrl, nama)

      return { ok: true, fotoUrl }
    } finally {
      if (event?.target) event.target.value = ''
    }
  }

  window.__sharedProfilePhotoUtilsReady = true
})()

