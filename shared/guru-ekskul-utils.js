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

  window.guruEkskulUtils = {
    buildStarsHtml,
    getEmptyStarsHtml
  }
})()
