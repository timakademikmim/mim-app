package com.mim.guruapp.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.mim.guruapp.SampleDataFactory
import com.mim.guruapp.ui.components.CalendarScreen
import com.mim.guruapp.ui.theme.MimGuruTheme
import java.time.LocalDate

@Composable
fun CalendarScreenPreviewHost() {
  var selectedDate by remember { mutableStateOf(LocalDate.now()) }
  val dashboard = remember { SampleDataFactory.createDashboard("Ustadz Fulan") }

  CalendarScreen(
    selectedDate = selectedDate,
    events = dashboard.calendarEvents,
    isRefreshing = false,
    onSelectDate = { selectedDate = it },
    onJumpToToday = { selectedDate = LocalDate.now() },
    onRefresh = {},
    onBackClick = {}
  )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CalendarScreenPreview() {
  MimGuruTheme {
    CalendarScreenPreviewHost()
  }
}
