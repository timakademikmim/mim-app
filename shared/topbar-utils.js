;(function initSharedTopbarUtils() {
  if (window.__sharedTopbarUtilsReady) return

  window.setTopbarBadgeCount = function setTopbarBadgeCount(elementId, count) {
    const safeId = String(elementId || '').trim()
    const badge = document.getElementById(safeId)
    if (!badge) return
    const total = Number.isFinite(Number(count)) ? Number(count) : 0
    if (total <= 0) {
      badge.textContent = '0'
      badge.classList.add('hidden')
      if (safeId === 'topbar-chat-badge') {
        window.__androidBottomPendingChatBadgeCount = 0
        if (typeof window.setAndroidBottomChatBadge === 'function') {
          window.setAndroidBottomChatBadge(0)
        }
      }
      return
    }
    badge.classList.remove('hidden')
    badge.textContent = total > 99 ? '99+' : String(total)
    if (safeId === 'topbar-chat-badge') {
      window.__androidBottomPendingChatBadgeCount = total
      if (typeof window.setAndroidBottomChatBadge === 'function') {
        window.setAndroidBottomChatBadge(total)
      }
    }
  }

  window.__sharedTopbarUtilsReady = true
})()
