;(function initSharedTopbarUtils() {
  if (window.__sharedTopbarUtilsReady) return

  window.setTopbarBadgeCount = function setTopbarBadgeCount(elementId, count) {
    const badge = document.getElementById(String(elementId || '').trim())
    if (!badge) return
    const total = Number.isFinite(Number(count)) ? Number(count) : 0
    if (total <= 0) {
      badge.textContent = '0'
      badge.classList.add('hidden')
      return
    }
    badge.classList.remove('hidden')
    badge.textContent = total > 99 ? '99+' : String(total)
  }

  window.__sharedTopbarUtilsReady = true
})()

