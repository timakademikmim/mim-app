;(function initGuruEkskulUtils() {
  if (window.guruEkskulUtils) return

  function getEmptyStarsHtml() {
    return Array.from({ length: 5 })
      .map(() => '<span style="color:#cbd5e1; font-size:17px; line-height:1;">&#9733;</span>')
      .join('')
  }

  function buildStarsHtml(value, { emptyAsDash = true } = {}) {
    const num = Number(value)
    if (!Number.isFinite(num)) return emptyAsDash ? '-' : getEmptyStarsHtml()
    const score = Math.max(1, Math.min(100, num))
    const rating = Math.round((score / 20) * 2) / 2
    const full = Math.floor(rating)
    const half = rating - full >= 0.5
    let html = ''
    for (let i = 0; i < 5; i += 1) {
      if (i < full) {
        html += '<span style="color:#f59e0b; font-size:17px; line-height:1;">&#9733;</span>'
      } else if (i === full && half) {
        html += '<span style="background:linear-gradient(90deg,#f59e0b 50%,#cbd5e1 50%); -webkit-background-clip:text; background-clip:text; color:transparent; -webkit-text-fill-color:transparent; font-size:17px; line-height:1;">&#9733;</span>'
      } else {
        html += '<span style="color:#cbd5e1; font-size:17px; line-height:1;">&#9733;</span>'
      }
    }
    return html
  }

  function buildMemberCardHtml({ sid, name, index, isActive, escapeHtml }) {
    return `
      <div style="display:grid; grid-template-columns:1fr auto; gap:6px; align-items:center; padding:8px; border:1px solid #e2e8f0; border-radius:8px; margin-bottom:6px; background:#fff;">
        <button type="button" class="modal-btn" onclick="selectGuruEkskulProgresSantri('${escapeHtml(sid)}')" style="display:block; width:100%; text-align:left; font-size:13px; ${isActive ? 'border-color:#d4d456; background:#fefce8; font-weight:700;' : 'font-weight:500;'}">${index}. ${escapeHtml(String(name || '-'))}</button>
        <button type="button" class="modal-btn" onclick="openGuruEkskulSantriDetail('${escapeHtml(sid)}')">Detail</button>
      </div>
    `
  }

  function buildIndikatorCardHtml({ nama, deskripsi, escapeHtml }) {
    return `
      <div style="padding:6px 8px; border:1px solid #e2e8f0; border-radius:8px; margin-bottom:6px;">
        <div style="font-weight:600; font-size:13px;">${escapeHtml(String(nama || '-'))}</div>
        <div style="font-size:12px; color:#64748b;">${escapeHtml(String(deskripsi || '-'))}</div>
      </div>
    `
  }

  function buildProgressDetailRowHtml({ tanggal, indikatorNama, nilaiHtml, catatan, escapeHtml }) {
    return `
      <tr>
        <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(String(tanggal || '-'))}</td>
        <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(String(indikatorNama || 'Umum'))}</td>
        <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${nilaiHtml}</td>
        <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(String(catatan || '-'))}</td>
      </tr>
    `
  }

  window.guruEkskulUtils = {
    buildStarsHtml,
    getEmptyStarsHtml,
    buildMemberCardHtml,
    buildIndikatorCardHtml,
    buildProgressDetailRowHtml
  }
})()
