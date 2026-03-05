;(function initSharedAppUtils() {
  if (window.__sharedAppUtilsReady) return

  function appEscapeHtml(value) {
    return String(value ?? '')
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#39;')
  }

  window.appEscapeHtml = appEscapeHtml
  window.__sharedAppUtilsReady = true
})()

