;(function initSharedPopupUtils() {
  if (window.__sharedPopupUtilsReady) return

  window.setupSharedPopupSystem = function setupSharedPopupSystem() {
    if (window.__popupReady) return true

    const overlay = document.getElementById('app-popup-overlay')
    const messageEl = document.getElementById('app-popup-message')
    const actionsEl = document.getElementById('app-popup-actions')
    const okBtn = document.getElementById('app-popup-ok-btn')
    if (!overlay || !messageEl || !actionsEl || !okBtn) return false

    const closePopup = () => {
      overlay.classList.remove('open')
      overlay.setAttribute('aria-hidden', 'true')
      actionsEl.innerHTML = ''
    }

    window.showPopupMessage = function showPopupMessage(message) {
      return new Promise(resolve => {
        messageEl.textContent = String(message ?? '')
        actionsEl.innerHTML = ''

        const btn = document.createElement('button')
        btn.type = 'button'
        btn.textContent = 'OK'
        btn.className = 'app-popup-primary'
        btn.onclick = () => {
          closePopup()
          resolve(true)
        }
        actionsEl.appendChild(btn)

        overlay.classList.add('open')
        overlay.setAttribute('aria-hidden', 'false')
        btn.focus()
      })
    }

    window.showPopupConfirm = function showPopupConfirm(message) {
      return new Promise(resolve => {
        messageEl.textContent = String(message ?? '')
        actionsEl.innerHTML = ''

        const cancelBtn = document.createElement('button')
        cancelBtn.type = 'button'
        cancelBtn.textContent = 'Batal'
        cancelBtn.onclick = () => {
          closePopup()
          resolve(false)
        }

        const okConfirmBtn = document.createElement('button')
        okConfirmBtn.type = 'button'
        okConfirmBtn.textContent = 'Ya'
        okConfirmBtn.className = 'app-popup-primary'
        okConfirmBtn.onclick = () => {
          closePopup()
          resolve(true)
        }

        actionsEl.appendChild(cancelBtn)
        actionsEl.appendChild(okConfirmBtn)

        overlay.classList.add('open')
        overlay.setAttribute('aria-hidden', 'false')
        okConfirmBtn.focus()
      })
    }

    window.alert = function customAlert(message) {
      window.showPopupMessage(message)
    }

    overlay.addEventListener('click', event => {
      if (event.target !== overlay) return
      const cancelButton = actionsEl.querySelector('button:not(.app-popup-primary)')
      if (cancelButton) {
        cancelButton.click()
        return
      }
      const primaryButton = actionsEl.querySelector('button.app-popup-primary') || actionsEl.querySelector('button')
      if (primaryButton) primaryButton.click()
    })

    window.__popupReady = true
    return true
  }

  window.__sharedPopupUtilsReady = true
})()

