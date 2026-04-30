package com.mim.guruapp.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController

fun Modifier.clearFocusOnOutsideTap(): Modifier = composed {
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current

  pointerInput(focusManager, keyboardController) {
    detectUnconsumedTap(
      focusManager = focusManager,
      keyboardController = keyboardController
    )
  }
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectUnconsumedTap(
  focusManager: FocusManager,
  keyboardController: SoftwareKeyboardController?
) {
  awaitEachGesture {
    val down = awaitFirstDown(
      requireUnconsumed = false,
      pass = PointerEventPass.Final
    )
    val up = waitForUpOrCancellation(pass = PointerEventPass.Final)

    if (up != null && !down.isConsumed && !up.isConsumed) {
      focusManager.clearFocus()
      keyboardController?.hide()
    }
  }
}
