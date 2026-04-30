package com.mim.guruapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mim.guruapp.SampleDataFactory
import com.mim.guruapp.ui.components.DashboardScreenScaffold
import com.mim.guruapp.ui.theme.AppBackground
import com.mim.guruapp.ui.theme.MimGuruTheme
import java.time.LocalDate

@Composable
fun DashboardScreenPreviewHost() {
  val dashboard = SampleDataFactory.createDashboard("Ustadz Fulan")

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(AppBackground)
      .padding(horizontal = 18.dp)
  ) {
    DashboardScreenScaffold(
      currentDate = LocalDate.now(),
      notificationCount = dashboard.unreadNotificationCount,
      isRefreshing = false,
      onMenuClick = {},
      onDateClick = {},
      onNotificationClick = {},
      onRefresh = {},
      categories = dashboard.categories,
      tasks = dashboard.ongoingTasks,
      calendarEvents = dashboard.calendarEvents,
      modifier = Modifier.fillMaxSize()
    )
  }
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 393, heightDp = 852)
@Composable
private fun DashboardScreenPreview() {
  MimGuruTheme {
    DashboardScreenPreviewHost()
  }
}
