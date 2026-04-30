package com.mim.guruapp.data.remote

import com.mim.guruapp.BuildConfig
import com.mim.guruapp.data.model.MonthlyAttendanceSummary
import com.mim.guruapp.data.model.MonthlyExtracurricularReport
import com.mim.guruapp.data.model.MonthlyReportItem
import com.mim.guruapp.data.model.MonthlyReportSnapshot
import com.mim.guruapp.data.model.WaliSantriSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.YearMonth

sealed interface GuruMonthlyReportSaveResult {
  data class Success(
    val report: MonthlyReportItem,
    val message: String
  ) : GuruMonthlyReportSaveResult
  data class Error(val message: String) : GuruMonthlyReportSaveResult
}

sealed interface GuruMonthlyExtracurricularSaveResult {
  data class Success(
    val reports: List<MonthlyExtracurricularReport>,
    val message: String
  ) : GuruMonthlyExtracurricularSaveResult
  data class Error(val message: String) : GuruMonthlyExtracurricularSaveResult
}

class GuruMonthlyReportRemoteDataSource {
  suspend fun fetchMonthlyReportSnapshot(
    teacherRowId: String,
    teacherKaryawanId: String,
    waliSantriSnapshot: WaliSantriSnapshot
  ): MonthlyReportSnapshot? = withContext(Dispatchers.IO) {
    val guruId = teacherRowId.trim().ifBlank { resolveTeacherRowId(teacherKaryawanId) }
    val students = waliSantriSnapshot.students
    val periods = buildAvailablePeriods()
    if (guruId.isBlank() || students.isEmpty()) {
      return@withContext MonthlyReportSnapshot(
        guruId = guruId,
        availablePeriods = periods,
        attendanceSummaries = emptyList(),
        updatedAt = System.currentTimeMillis()
      )
    }

    val studentIds = students.map { it.id.trim() }.filter { it.isNotBlank() }.distinct()
    if (studentIds.isEmpty()) {
      return@withContext MonthlyReportSnapshot(
        guruId = guruId,
        availablePeriods = periods,
        attendanceSummaries = emptyList(),
        updatedAt = System.currentTimeMillis()
      )
    }

    val attendanceSummaries = fetchAttendanceSummaries(
      periods = periods,
      studentIds = studentIds,
      classIds = waliSantriSnapshot.classes.map { it.id }
    )
    val extracurricularReports = fetchExtracurricularReports(
      periods = periods,
      studentIds = studentIds
    )

    val optionalResult = runCatching {
      fetchRows(
        table = MONTHLY_REPORT_TABLE,
        query = buildMonthlyReportQuery(
          select = MONTHLY_REPORT_SELECT_WITH_OPTIONAL_TEXT,
          guruId = guruId,
          periods = periods,
          studentIds = studentIds
        )
      )
    }

    val rows = optionalResult.getOrElse { error ->
      if (isMissingTableError(error)) {
        return@withContext MonthlyReportSnapshot(
          guruId = guruId,
          availablePeriods = periods,
          extracurricularReports = extracurricularReports,
          attendanceSummaries = attendanceSummaries,
          updatedAt = System.currentTimeMillis(),
          missingTable = true
        )
      }
      if (!isMissingColumnError(error)) return@withContext null

      val extendedRows = runCatching {
        fetchRows(
          table = MONTHLY_REPORT_TABLE,
          query = buildMonthlyReportQuery(
            select = MONTHLY_REPORT_SELECT_EXTENDED,
            guruId = guruId,
            periods = periods,
            studentIds = studentIds
          )
        )
      }.getOrElse { extendedError ->
        if (isMissingTableError(extendedError)) {
          return@withContext MonthlyReportSnapshot(
            guruId = guruId,
            availablePeriods = periods,
            extracurricularReports = extracurricularReports,
            attendanceSummaries = attendanceSummaries,
            updatedAt = System.currentTimeMillis(),
            missingTable = true,
            missingExtendedColumns = true
          )
        }
        if (!isMissingColumnError(extendedError)) return@withContext null
        val legacyRows = runCatching {
          fetchRows(
            table = MONTHLY_REPORT_TABLE,
            query = buildMonthlyReportQuery(
              select = MONTHLY_REPORT_SELECT_LEGACY,
              guruId = guruId,
              periods = periods,
              studentIds = studentIds
            )
          )
        }.getOrElse { legacyError ->
          if (isMissingTableError(legacyError)) {
            return@withContext MonthlyReportSnapshot(
              guruId = guruId,
              availablePeriods = periods,
              extracurricularReports = extracurricularReports,
              attendanceSummaries = attendanceSummaries,
              updatedAt = System.currentTimeMillis(),
              missingTable = true,
              missingExtendedColumns = true
            )
          }
          return@withContext null
        }
        return@withContext MonthlyReportSnapshot(
          guruId = guruId,
          availablePeriods = periods,
          reports = legacyRows.mapNotNull { parseMonthlyReportItem(it, extended = false) },
          extracurricularReports = extracurricularReports,
          attendanceSummaries = attendanceSummaries,
          updatedAt = System.currentTimeMillis(),
          missingExtendedColumns = true
        )
      }
      extendedRows
    }

    MonthlyReportSnapshot(
      guruId = guruId,
      availablePeriods = periods,
      reports = rows.mapNotNull { parseMonthlyReportItem(it, extended = true) },
      extracurricularReports = extracurricularReports,
      attendanceSummaries = attendanceSummaries,
      updatedAt = System.currentTimeMillis()
    )
  }

  suspend fun saveMonthlyReport(
    report: MonthlyReportItem
  ): GuruMonthlyReportSaveResult = withContext(Dispatchers.IO) {
    val normalizedReport = report.copy(
      period = report.period.trim(),
      guruId = report.guruId.trim(),
      classId = report.classId.trim(),
      studentId = report.studentId.trim(),
      predikat = getAkhlakKeteranganByGrade(report.nilaiAkhlak)
    )
    if (normalizedReport.period.isBlank() || normalizedReport.guruId.isBlank() || normalizedReport.studentId.isBlank()) {
      return@withContext GuruMonthlyReportSaveResult.Error("Data laporan belum lengkap untuk disimpan.")
    }

    runCatching {
      val savedReport = runCatching {
        saveMonthlyReportRequest(normalizedReport, includeOptionalText = true)
      }.getOrElse { error ->
        if (!isMissingColumnError(error)) throw error
        saveMonthlyReportRequest(normalizedReport, includeOptionalText = false)
      }

      GuruMonthlyReportSaveResult.Success(
        report = savedReport,
        message = "Laporan bulanan berhasil disimpan."
      )
    }.getOrElse { error ->
      val message = when {
        error is SocketTimeoutException -> "Koneksi ke server terlalu lama. Coba lagi."
        isMissingTableError(error) -> "Tabel laporan_bulanan_wali belum tersedia atau belum bisa diakses."
        isMissingColumnError(error) -> "Kolom laporan bulanan belum lengkap untuk menyimpan semua data."
        else -> "Gagal menyimpan laporan bulanan ke server."
      }
      GuruMonthlyReportSaveResult.Error(message)
    }
  }

  suspend fun saveMonthlyExtracurricularReports(
    reports: List<MonthlyExtracurricularReport>,
    updatedBy: String
  ): GuruMonthlyExtracurricularSaveResult = withContext(Dispatchers.IO) {
    val drafts = reports.filter {
      it.period.isNotBlank() && it.studentId.isNotBlank() && it.activityId.isNotBlank()
    }
    if (drafts.isEmpty()) {
      return@withContext GuruMonthlyExtracurricularSaveResult.Success(emptyList(), "Tidak ada laporan ekskul yang perlu disimpan.")
    }
    runCatching {
      val requestUrl = buildString {
        append(BuildConfig.SUPABASE_URL)
        append("/rest/v1/")
        append(EKSKUL_MONTHLY_TABLE)
        append("?on_conflict=periode,ekskul_id,santri_id&select=id,periode,ekskul_id,santri_id,kehadiran_persen,catatan_pj,updated_at")
      }
      val payload = JSONArray().apply {
        drafts.forEach { report ->
          put(JSONObject().apply {
            putNullableString("periode", report.period)
            putNullableString("ekskul_id", report.activityId)
            putNullableString("santri_id", report.studentId)
            putNullableNumber("kehadiran_persen", report.attendanceLabel.removeSuffix("%"))
            putNullableString("catatan_pj", report.note)
            putNullableString("updated_by", updatedBy)
            putNullableString("updated_at", java.time.OffsetDateTime.now().toString())
          })
        }
      }
      val connection = createConnection(requestUrl, method = "POST").apply {
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
        setRequestProperty("Prefer", "resolution=merge-duplicates,return=representation")
      }
      connection.outputStream.use { stream ->
        stream.write(payload.toString().toByteArray(Charsets.UTF_8))
        stream.flush()
      }
      val saved = connection.useJsonArrayResponse { rows ->
        List(rows.length()) { index -> rows.optJSONObject(index) }
          .filterNotNull()
          .mapNotNull { row ->
            val period = row.cleanString("periode")
            val studentId = row.cleanString("santri_id")
            val activityId = row.cleanString("ekskul_id")
            if (period.isBlank() || studentId.isBlank() || activityId.isBlank()) null
            else {
              val source = drafts.firstOrNull {
                it.period == period && it.studentId == studentId && it.activityId == activityId
              }
              val savedPercent = row.cleanString("kehadiran_persen")
              val savedNote = row.cleanString("catatan_pj")
              source?.copy(
                id = row.cleanString("id"),
                attendanceLabel = savedPercent.withPercentSuffix(),
                note = savedNote,
                hasMonthlyOverride = savedPercent.isNotBlank() || savedNote.isNotBlank()
              )
            }
          }
      }
      GuruMonthlyExtracurricularSaveResult.Success(saved, "Laporan ekskul berhasil disimpan.")
    }.getOrElse { error ->
      val message = when {
        error is SocketTimeoutException -> "Koneksi ke server terlalu lama. Coba lagi."
        isMissingTableError(error) -> "Tabel laporan bulanan ekskul belum tersedia atau belum bisa diakses."
        else -> "Gagal menyimpan laporan bulanan ekskul."
      }
      GuruMonthlyExtracurricularSaveResult.Error(message)
    }
  }

  private fun fetchExtracurricularReports(
    periods: List<String>,
    studentIds: List<String>
  ): List<MonthlyExtracurricularReport> {
    if (periods.isEmpty() || studentIds.isEmpty()) return emptyList()
    val memberRows = runCatching {
      fetchRows(
        table = EKSKUL_MEMBER_TABLE,
        query = buildString {
          append("select=ekskul_id,santri_id")
          append("&santri_id=in.(")
          append(studentIds.joinToString(",") { "\"${encodeValue(it)}\"" })
          append(")")
        }
      )
    }.getOrDefault(emptyList())
    if (memberRows.isEmpty()) return emptyList()

    val ekskulIds = memberRows.map { it.cleanString("ekskul_id") }.filter { it.isNotBlank() }.distinct()
    if (ekskulIds.isEmpty()) return emptyList()

    val ekskulRows = fetchExtracurricularRows(ekskulIds)
    if (ekskulRows.isEmpty()) return emptyList()

    val sortedPeriods = periods.mapNotNull { runCatching { YearMonth.parse(it) }.getOrNull() }.sorted()
    val start = sortedPeriods.firstOrNull()?.atDay(1) ?: return emptyList()
    val end = sortedPeriods.lastOrNull()?.atEndOfMonth() ?: return emptyList()

    val progressRows = runCatching {
      fetchRows(
        table = EKSKUL_PROGRESS_TABLE,
        query = buildString {
          append("select=ekskul_id,santri_id,tanggal,catatan,created_at")
          append("&santri_id=in.(")
          append(studentIds.joinToString(",") { "\"${encodeValue(it)}\"" })
          append(")&ekskul_id=in.(")
          append(ekskulIds.joinToString(",") { "\"${encodeValue(it)}\"" })
          append(")&tanggal=gte.")
          append(start)
          append("&tanggal=lte.")
          append(end)
          append("&order=tanggal.desc&order=created_at.desc")
        }
      )
    }.getOrDefault(emptyList())

    val monthlyRows = runCatching {
      fetchRows(
        table = EKSKUL_MONTHLY_TABLE,
        query = buildString {
          append("select=id,periode,ekskul_id,santri_id,kehadiran_persen,catatan_pj,updated_at")
          append("&santri_id=in.(")
          append(studentIds.joinToString(",") { "\"${encodeValue(it)}\"" })
          append(")&ekskul_id=in.(")
          append(ekskulIds.joinToString(",") { "\"${encodeValue(it)}\"" })
          append(")&periode=in.(")
          append(periods.joinToString(",") { "\"${encodeValue(it)}\"" })
          append(")")
        }
      )
    }.getOrDefault(emptyList())

    val pjIds = ekskulRows.flatMap { row ->
      listOf(row.cleanString("pj_karyawan_id"), row.cleanString("pj_karyawan_id_2"))
    }.filter { it.isNotBlank() }.distinct()
    val karyawanRows = if (pjIds.isEmpty()) emptyList() else fetchActiveKaryawanRows()
    val pjByToken = buildPjLookup(karyawanRows)

    val ekskulById = ekskulRows.associateBy { it.cleanString("id") }
    val membersByStudent = memberRows.groupBy { it.cleanString("santri_id") }
    val progressByKey = linkedMapOf<Triple<String, String, String>, Pair<Int, String>>()
    progressRows.forEach { row ->
      val studentId = row.cleanString("santri_id")
      val ekskulId = row.cleanString("ekskul_id")
      val date = row.cleanString("tanggal").take(10)
      val period = runCatching { YearMonth.from(LocalDate.parse(date)).toString() }.getOrNull().orEmpty()
      if (studentId.isBlank() || ekskulId.isBlank() || period !in periods) return@forEach
      val key = Triple(period, studentId, ekskulId)
      val current = progressByKey[key] ?: (0 to "")
      val note = current.second.ifBlank { row.cleanString("catatan") }
      progressByKey[key] = (current.first + 1) to note
    }
    val monthlyByKey = monthlyRows.associateBy {
      Triple(it.cleanString("periode"), it.cleanString("santri_id"), it.cleanString("ekskul_id"))
    }

    return periods.flatMap { period ->
      studentIds.flatMap { studentId ->
        membersByStudent[studentId].orEmpty().mapNotNull { member ->
          val ekskulId = member.cleanString("ekskul_id")
          val ekskul = ekskulById[ekskulId] ?: return@mapNotNull null
          val key = Triple(period, studentId, ekskulId)
          val progress = progressByKey[key] ?: (0 to "")
          val monthly = monthlyByKey[key]
          val monthlyPercent = monthly?.cleanString("kehadiran_persen").orEmpty()
          val monthlyNote = monthly?.cleanString("catatan_pj").orEmpty()
          val pjRows = listOf(
            resolvePj(ekskul.cleanString("pj_karyawan_id"), pjByToken),
            resolvePj(ekskul.cleanString("pj_karyawan_id_2"), pjByToken)
          )
          val rawPjNames = listOf(ekskul.cleanString("pj_karyawan_id"), ekskul.cleanString("pj_karyawan_id_2"))
          val pjNames = pjRows.mapIndexedNotNull { index, row ->
            row?.cleanString("nama")?.takeIf { it.isNotBlank() } ?: rawPjNames.getOrNull(index)?.takeIf { it.isNotBlank() }
          }.distinct()
          val pjPhones = pjRows.mapNotNull { row ->
            row?.let { pickFirst(it, "no_hp", "hp", "no_telp", "nomor_hp", "telepon") }
          }.filter { it.isNotBlank() }.distinct()
          MonthlyExtracurricularReport(
            id = monthly?.cleanString("id").orEmpty(),
            period = period,
            studentId = studentId,
            activityId = ekskulId,
            activityName = ekskul.cleanString("nama").ifBlank { "-" },
            pjName = pjNames.joinToString(" / ").ifBlank { "-" },
            pjPhone = pjPhones.joinToString(" / ").ifBlank { "-" },
            attendanceLabel = if (monthlyPercent.isNotBlank()) {
              monthlyPercent.withPercentSuffix()
            } else if (progress.first > 0) {
              "${progress.first} kali"
            } else {
              "-"
            },
            note = monthlyNote.ifBlank { progress.second.ifBlank { "-" } },
            hasMonthlyOverride = monthlyPercent.isNotBlank() || monthlyNote.isNotBlank()
          )
        }
      }
    }.sortedWith(
      compareBy<MonthlyExtracurricularReport> { it.period }
        .thenBy { it.activityName.lowercase() }
        .thenBy { it.studentId }
    )
  }

  private fun fetchExtracurricularRows(ekskulIds: List<String>): List<JSONObject> {
    return runCatching {
      fetchRows(
        table = EKSKUL_TABLE,
        query = buildString {
          append("select=id,nama,pj_karyawan_id,pj_karyawan_id_2")
          append("&id=in.(")
          append(ekskulIds.joinToString(",") { "\"${encodeValue(it)}\"" })
          append(")&order=nama.asc")
        }
      )
    }.getOrElse { error ->
      if (!error.message.orEmpty().contains("pj_karyawan_id_2", ignoreCase = true)) return emptyList()
      runCatching {
        fetchRows(
          table = EKSKUL_TABLE,
          query = buildString {
            append("select=id,nama,pj_karyawan_id")
            append("&id=in.(")
            append(ekskulIds.joinToString(",") { "\"${encodeValue(it)}\"" })
            append(")&order=nama.asc")
          }
        )
      }.getOrDefault(emptyList())
    }
  }

  private fun fetchActiveKaryawanRows(): List<JSONObject> {
    val variants = listOf(
      "id,id_karyawan,nama,no_hp,hp,no_telp,nomor_hp,telepon,aktif",
      "id,id_karyawan,nama,no_hp,hp,aktif",
      "id,id_karyawan,nama,no_hp,hp",
      "id,id_karyawan,nama,no_hp",
      "id,id_karyawan,nama",
      "id,nama"
    )
    variants.forEach { select ->
      val rows = runCatching {
        fetchRows(
          table = "karyawan",
          query = "select=$select&aktif=eq.true"
        )
      }.getOrNull()
      if (rows != null) return rows
    }
    return emptyList()
  }

  private fun buildPjLookup(rows: List<JSONObject>): Map<String, JSONObject> {
    val result = linkedMapOf<String, JSONObject>()
    rows.forEach { row ->
      listOf(
        row.cleanString("id"),
        row.cleanString("id_karyawan"),
        normalizePersonName(row.cleanString("nama"))
      ).filter { it.isNotBlank() }.forEach { token ->
        result[token] = row
      }
    }
    return result
  }

  private fun resolvePj(
    rawToken: String,
    lookup: Map<String, JSONObject>
  ): JSONObject? {
    val token = rawToken.trim()
    if (token.isBlank()) return null
    return lookup[token] ?: lookup[normalizePersonName(token)]
  }

  private fun normalizePersonName(value: String): String {
    return value.trim().replace(Regex("\\s+"), " ").lowercase()
  }

  private fun pickFirst(
    row: JSONObject,
    vararg keys: String
  ): String {
    return keys.firstNotNullOfOrNull { key -> row.cleanString(key).takeIf { it.isNotBlank() } }.orEmpty()
  }

  private fun fetchAttendanceSummaries(
    periods: List<String>,
    studentIds: List<String>,
    classIds: List<String>
  ): List<MonthlyAttendanceSummary> {
    if (periods.isEmpty() || studentIds.isEmpty()) return emptyList()
    val sortedPeriods = periods.mapNotNull { runCatching { YearMonth.parse(it) }.getOrNull() }.sorted()
    val start = sortedPeriods.firstOrNull()?.atDay(1) ?: return emptyList()
    val end = sortedPeriods.lastOrNull()?.atEndOfMonth() ?: return emptyList()

    val rows = runCatching {
      fetchRows(
        table = "absensi_santri",
        query = buildString {
          append("select=santri_id,tanggal,status,kelas_id")
          append("&santri_id=in.(")
          append(studentIds.joinToString(",") { "\"${encodeValue(it)}\"" })
          append(")&tanggal=gte.")
          append(start)
          append("&tanggal=lte.")
          append(end)
          val normalizedClassIds = classIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
          if (normalizedClassIds.isNotEmpty()) {
            append("&kelas_id=in.(")
            append(normalizedClassIds.joinToString(",") { "\"${encodeValue(it)}\"" })
            append(")")
          }
        }
      )
    }.getOrDefault(emptyList())

    val dailyByPeriodStudent = linkedMapOf<Pair<String, String>, MutableMap<String, MutableList<String>>>()
    rows.forEach { row ->
      val studentId = row.cleanString("santri_id")
      val date = row.cleanString("tanggal").take(10)
      val period = runCatching { YearMonth.from(LocalDate.parse(date)).toString() }.getOrNull().orEmpty()
      if (studentId.isBlank() || date.isBlank() || period !in periods) return@forEach
      val key = period to studentId
      val daily = dailyByPeriodStudent.getOrPut(key) { linkedMapOf() }
      daily.getOrPut(date) { mutableListOf() } += row.cleanString("status")
    }

    return dailyByPeriodStudent.map { (key, dailyRows) ->
      val statuses = dailyRows.values.map { resolveDailyAttendanceStatus(it) }
      val total = statuses.size
      val hadir = statuses.count { it == "Hadir" }
      val sakit = statuses.count { it == "Sakit" }
      val izin = statuses.count { it == "Izin" }
      val alpa = statuses.count { it == "Alpa" }
      val percent = if (total > 0) (hadir.toDouble() / total.toDouble()) * 100.0 else 0.0
      val percentLabel = trimNumber(percent)
      MonthlyAttendanceSummary(
        period = key.first,
        studentId = key.second,
        attendancePercent = percentLabel,
        attendancePredicate = getKehadiranPredikat(percent),
        sakitCount = sakit,
        izinCount = izin,
        alpaCount = alpa,
        totalDays = total
      )
    }
  }

  private fun buildMonthlyReportQuery(
    select: String,
    guruId: String,
    periods: List<String>,
    studentIds: List<String>
  ): String {
    return buildString {
      append("select=")
      append(select)
      append("&guru_id=eq.")
      append(encodeValue(guruId))
      append("&periode=in.(")
      append(periods.joinToString(",") { "\"${encodeValue(it)}\"" })
      append(")&santri_id=in.(")
      append(studentIds.joinToString(",") { "\"${encodeValue(it)}\"" })
      append(")&order=periode.desc")
    }
  }

  private fun parseMonthlyReportItem(
    row: JSONObject,
    extended: Boolean
  ): MonthlyReportItem? {
    val studentId = row.cleanString("santri_id")
    val period = row.cleanString("periode")
    if (studentId.isBlank() || period.isBlank()) return null
    val nilaiAkhlak = normalizeAkhlakGrade(row.cleanString("nilai_akhlak"))
    return MonthlyReportItem(
      id = row.cleanString("id"),
      period = period,
      guruId = row.cleanString("guru_id"),
      classId = row.cleanString("kelas_id"),
      studentId = studentId,
      nilaiAkhlak = nilaiAkhlak,
      predikat = row.cleanString("predikat").ifBlank { getAkhlakKeteranganByGrade(nilaiAkhlak) },
      catatanWali = row.cleanString("catatan_wali"),
      muhaffiz = if (extended) row.cleanString("muhaffiz") else "",
      noHpMuhaffiz = if (extended) row.cleanString("no_hp_muhaffiz") else "",
      nilaiKehadiranHalaqah = if (extended) row.cleanString("nilai_kehadiran_halaqah") else "",
      sakitHalaqah = if (extended) row.cleanString("sakit_halaqah") else "",
      izinHalaqah = if (extended) row.cleanString("izin_halaqah") else "",
      nilaiAkhlakHalaqah = if (extended) normalizeAkhlakGrade(row.cleanString("nilai_akhlak_halaqah")) else "",
      keteranganAkhlakHalaqah = if (extended) row.cleanString("keterangan_akhlak_halaqah") else "",
      nilaiUjianBulanan = if (extended) row.cleanString("nilai_ujian_bulanan") else "",
      keteranganUjianBulanan = if (extended) row.cleanString("keterangan_ujian_bulanan") else "",
      nilaiTargetHafalan = if (extended) row.cleanString("nilai_target_hafalan") else "",
      keteranganTargetHafalan = if (extended) row.cleanString("keterangan_target_hafalan") else "",
      nilaiCapaianHafalanBulanan = if (extended) row.cleanString("nilai_capaian_hafalan_bulanan") else "",
      keteranganCapaianHafalanBulanan = if (extended) row.cleanString("keterangan_capaian_hafalan_bulanan") else "",
      keteranganJumlahHafalanBulanan = if (extended) row.cleanString("keterangan_jumlah_hafalan_bulanan") else "",
      nilaiJumlahHafalanHalaman = if (extended) row.cleanString("nilai_jumlah_hafalan_halaman") else "",
      nilaiJumlahHafalanJuz = if (extended) row.cleanString("nilai_jumlah_hafalan_juz") else "",
      catatanMuhaffiz = if (extended) row.cleanString("catatan_muhaffiz") else "",
      musyrif = if (extended) row.cleanString("musyrif") else "",
      noHpMusyrif = if (extended) row.cleanString("no_hp_musyrif") else "",
      nilaiKehadiranLiqaMuhasabah = if (extended) row.cleanString("nilai_kehadiran_liqa_muhasabah") else "",
      sakitLiqaMuhasabah = if (extended) row.cleanString("sakit_liqa_muhasabah") else "",
      izinLiqaMuhasabah = if (extended) row.cleanString("izin_liqa_muhasabah") else "",
      nilaiIbadah = if (extended) normalizeAkhlakGrade(row.cleanString("nilai_ibadah")) else "",
      keteranganIbadah = if (extended) row.cleanString("keterangan_ibadah") else "",
      nilaiKedisiplinan = if (extended) normalizeAkhlakGrade(row.cleanString("nilai_kedisiplinan")) else "",
      keteranganKedisiplinan = if (extended) row.cleanString("keterangan_kedisiplinan") else "",
      nilaiKebersihan = if (extended) normalizeAkhlakGrade(row.cleanString("nilai_kebersihan")) else "",
      keteranganKebersihan = if (extended) row.cleanString("keterangan_kebersihan") else "",
      nilaiAdab = if (extended) normalizeAkhlakGrade(row.cleanString("nilai_adab")) else "",
      keteranganAdab = if (extended) row.cleanString("keterangan_adab") else "",
      prestasiKesantrian = if (extended) row.cleanString("prestasi_kesantrian") else "",
      pelanggaranKesantrian = if (extended) row.cleanString("pelanggaran_kesantrian") else "",
      catatanMusyrif = if (extended) row.cleanString("catatan_musyrif") else ""
    )
  }

  private fun buildAvailablePeriods(): List<String> {
    val start = YearMonth.from(LocalDate.now()).minusMonths(1)
    return (0 until 12).map { offset -> start.minusMonths(offset.toLong()).toString() }
  }

  private fun fetchRows(table: String, query: String): List<JSONObject> {
    val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/$table?$query"
    val connection = createConnection(requestUrl)
    return connection.useJsonArrayResponse { rows ->
      List(rows.length()) { index -> rows.optJSONObject(index) }.filterNotNull()
    }
  }

  private fun createConnection(requestUrl: String, method: String = "GET"): HttpURLConnection {
    return (URL(requestUrl).openConnection() as HttpURLConnection).apply {
      requestMethod = method
      connectTimeout = 15_000
      readTimeout = 15_000
      setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
      setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
      setRequestProperty("Accept", "application/json")
      setRequestProperty("Accept-Charset", "UTF-8")
    }
  }

  private fun resolveTeacherRowId(teacherKaryawanId: String): String {
    if (teacherKaryawanId.isBlank()) return ""
    return runCatching {
      fetchRows(
        table = "karyawan",
        query = "select=id&id_karyawan=eq.${encodeValue(teacherKaryawanId)}&limit=1"
      ).firstOrNull()?.cleanString("id").orEmpty()
    }.getOrDefault("")
  }

  private fun encodeValue(value: String): String {
    return URLEncoder.encode(value.trim(), Charsets.UTF_8.name())
  }

  private fun normalizeAkhlakGrade(value: String): String {
    val raw = value.trim().uppercase()
    if (raw in listOf("A", "B", "C", "D", "E")) return raw
    val number = raw.toDoubleOrNull() ?: return ""
    if (number in 1.0..5.0) {
      return when (number.toInt().coerceIn(1, 5)) {
        5 -> "A"
        4 -> "B"
        3 -> "C"
        2 -> "D"
        else -> "E"
      }
    }
    return when {
      number >= 90 -> "A"
      number >= 80 -> "B"
      number >= 70 -> "C"
      number >= 60 -> "D"
      else -> "E"
    }
  }

  private fun getAkhlakKeteranganByGrade(value: String): String {
    return when (normalizeAkhlakGrade(value)) {
      "A" -> "Istimewa"
      "B" -> "Baik Sekali"
      "C" -> "Baik"
      "D" -> "Kurang"
      "E" -> "Sangat Kurang"
      else -> ""
    }
  }

  private fun getAkhlakNumericValueByGrade(value: String): String {
    return when (normalizeAkhlakGrade(value)) {
      "A" -> "5"
      "B" -> "4"
      "C" -> "3"
      "D" -> "2"
      "E" -> "1"
      else -> ""
    }
  }

  private fun resolveDailyAttendanceStatus(statuses: List<String>): String {
    val normalized = statuses.map { it.trim().lowercase() }.filter { it.isNotBlank() }
    return when {
      normalized.any { it == "hadir" || it == "terlambat" } -> "Hadir"
      normalized.any { it == "sakit" } -> "Sakit"
      normalized.any { it == "izin" } -> "Izin"
      normalized.any { it == "alpa" } -> "Alpa"
      else -> "-"
    }
  }

  private fun getKehadiranPredikat(value: Double): String {
    return when {
      value >= 95.0 -> "A (Sangat Baik)"
      value >= 85.0 -> "B (Baik)"
      value >= 75.0 -> "C (Cukup)"
      value >= 60.0 -> "D (Kurang)"
      else -> "E (Perlu Pembinaan)"
    }
  }

  private fun trimNumber(value: Double): String {
    val rounded = kotlin.math.round(value * 100.0) / 100.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
  }

  private fun saveMonthlyReportRequest(
    report: MonthlyReportItem,
    includeOptionalText: Boolean
  ): MonthlyReportItem {
    val isUpdate = report.id.isNotBlank()
    val requestUrl = buildString {
      append(BuildConfig.SUPABASE_URL)
      append("/rest/v1/")
      append(MONTHLY_REPORT_TABLE)
      if (isUpdate) {
        append("?id=eq.")
        append(encodeValue(report.id))
        append("&select=")
      } else {
        append("?select=")
      }
      append(if (includeOptionalText) MONTHLY_REPORT_SELECT_WITH_OPTIONAL_TEXT else MONTHLY_REPORT_SELECT_EXTENDED)
    }
    val connection = createConnection(requestUrl, method = if (isUpdate) "PATCH" else "POST").apply {
      doOutput = true
      setRequestProperty("Content-Type", "application/json; charset=utf-8")
      setRequestProperty("Prefer", "return=representation")
    }
    val payload = report.toJsonPayload(
      includeIdentity = !isUpdate,
      includeOptionalText = includeOptionalText
    )
    connection.outputStream.use { stream ->
      stream.write(payload.toString().toByteArray(Charsets.UTF_8))
      stream.flush()
    }
    return connection.useJsonArrayResponse { rows ->
      rows.optJSONObject(0)?.let { parseMonthlyReportItem(it, extended = true) }
    } ?: error("Server tidak mengembalikan data laporan yang disimpan.")
  }

  private fun MonthlyReportItem.toJsonPayload(
    includeIdentity: Boolean,
    includeOptionalText: Boolean
  ): JSONObject {
    return JSONObject().apply {
      if (includeIdentity) {
        putNullableString("periode", period)
        putNullableString("guru_id", guruId)
        putNullableString("kelas_id", classId)
        putNullableString("santri_id", studentId)
      }
      putNullableNumber("nilai_akhlak", getAkhlakNumericValueByGrade(nilaiAkhlak))
      putNullableString("predikat", predikat.ifBlank { getAkhlakKeteranganByGrade(nilaiAkhlak) })
      putNullableString("catatan_wali", catatanWali)
      putNullableString("muhaffiz", muhaffiz)
      putNullableString("no_hp_muhaffiz", noHpMuhaffiz)
      putNullableNumber("nilai_kehadiran_halaqah", nilaiKehadiranHalaqah)
      putNullableInteger("sakit_halaqah", sakitHalaqah)
      putNullableInteger("izin_halaqah", izinHalaqah)
      putNullableString("nilai_akhlak_halaqah", normalizeAkhlakGrade(nilaiAkhlakHalaqah))
      putNullableString("keterangan_akhlak_halaqah", getAkhlakKeteranganByGrade(nilaiAkhlakHalaqah))
      putNullableNumber("nilai_ujian_bulanan", nilaiUjianBulanan)
      putNullableString("keterangan_ujian_bulanan", keteranganUjianBulanan)
      putNullableNumber("nilai_target_hafalan", nilaiTargetHafalan)
      putNullableString("keterangan_target_hafalan", keteranganTargetHafalan)
      putNullableNumber("nilai_capaian_hafalan_bulanan", nilaiCapaianHafalanBulanan)
      if (includeOptionalText) {
        putNullableString("keterangan_capaian_hafalan_bulanan", keteranganCapaianHafalanBulanan)
        putNullableString("keterangan_jumlah_hafalan_bulanan", keteranganJumlahHafalanBulanan)
      }
      putNullableNumber("nilai_jumlah_hafalan_halaman", nilaiJumlahHafalanHalaman)
      putNullableNumber("nilai_jumlah_hafalan_juz", nilaiJumlahHafalanJuz)
      putNullableString("catatan_muhaffiz", catatanMuhaffiz)
      putNullableString("musyrif", musyrif)
      putNullableString("no_hp_musyrif", noHpMusyrif)
      putNullableNumber("nilai_kehadiran_liqa_muhasabah", nilaiKehadiranLiqaMuhasabah)
      putNullableInteger("sakit_liqa_muhasabah", sakitLiqaMuhasabah)
      putNullableInteger("izin_liqa_muhasabah", izinLiqaMuhasabah)
      putNullableString("nilai_ibadah", normalizeAkhlakGrade(nilaiIbadah))
      putNullableString("keterangan_ibadah", getAkhlakKeteranganByGrade(nilaiIbadah))
      putNullableString("nilai_kedisiplinan", normalizeAkhlakGrade(nilaiKedisiplinan))
      putNullableString("keterangan_kedisiplinan", getAkhlakKeteranganByGrade(nilaiKedisiplinan))
      putNullableString("nilai_kebersihan", normalizeAkhlakGrade(nilaiKebersihan))
      putNullableString("keterangan_kebersihan", getAkhlakKeteranganByGrade(nilaiKebersihan))
      putNullableString("nilai_adab", normalizeAkhlakGrade(nilaiAdab))
      putNullableString("keterangan_adab", getAkhlakKeteranganByGrade(nilaiAdab))
      putNullableString("prestasi_kesantrian", prestasiKesantrian)
      putNullableString("pelanggaran_kesantrian", pelanggaranKesantrian)
      putNullableString("catatan_musyrif", catatanMusyrif)
    }
  }

  private fun JSONObject.putNullableString(key: String, value: String) {
    val normalized = value.trim()
    put(key, normalized.ifBlank { JSONObject.NULL })
  }

  private fun JSONObject.putNullableNumber(key: String, value: String) {
    val normalized = value.trim().replace(",", ".").removeSuffix("%").trim()
    val number = normalized.toDoubleOrNull()
    if (number == null) {
      put(key, JSONObject.NULL)
    } else {
      put(key, number)
    }
  }

  private fun JSONObject.putNullableInteger(key: String, value: String) {
    val normalized = value.trim()
      .replace(",", ".")
      .replace(Regex("""[^\d.-]"""), "")
    val number = normalized.toDoubleOrNull()
    if (number == null) {
      put(key, JSONObject.NULL)
    } else {
      put(key, number.toInt())
    }
  }

  private fun isMissingTableError(error: Throwable): Boolean {
    val message = error.message.orEmpty()
    return message.contains(MONTHLY_REPORT_TABLE, ignoreCase = true) &&
      (message.contains("does not exist", ignoreCase = true) ||
        message.contains("relation", ignoreCase = true) ||
        message.contains("42P01", ignoreCase = true))
  }

  private fun isMissingColumnError(error: Throwable): Boolean {
    val message = error.message.orEmpty()
    return message.contains("column", ignoreCase = true) ||
      message.contains("PGRST", ignoreCase = true) ||
      message.contains("42703", ignoreCase = true)
  }

  private companion object {
    const val MONTHLY_REPORT_TABLE = "laporan_bulanan_wali"
    const val EKSKUL_TABLE = "ekstrakurikuler"
    const val EKSKUL_MEMBER_TABLE = "ekstrakurikuler_anggota"
    const val EKSKUL_PROGRESS_TABLE = "ekstrakurikuler_progres"
    const val EKSKUL_MONTHLY_TABLE = "ekstrakurikuler_laporan_bulanan"
    const val MONTHLY_REPORT_SELECT_LEGACY = "id,periode,guru_id,kelas_id,santri_id,nilai_akhlak,predikat,catatan_wali"
    const val MONTHLY_REPORT_SELECT_WITH_OPTIONAL_TEXT =
      "id,periode,guru_id,kelas_id,santri_id,nilai_akhlak,predikat,catatan_wali,muhaffiz,no_hp_muhaffiz,nilai_kehadiran_halaqah,sakit_halaqah,izin_halaqah,nilai_akhlak_halaqah,keterangan_akhlak_halaqah,nilai_ujian_bulanan,keterangan_ujian_bulanan,nilai_target_hafalan,keterangan_target_hafalan,nilai_capaian_hafalan_bulanan,keterangan_capaian_hafalan_bulanan,keterangan_jumlah_hafalan_bulanan,nilai_jumlah_hafalan_halaman,nilai_jumlah_hafalan_juz,catatan_muhaffiz,musyrif,no_hp_musyrif,nilai_kehadiran_liqa_muhasabah,sakit_liqa_muhasabah,izin_liqa_muhasabah,nilai_ibadah,keterangan_ibadah,nilai_kedisiplinan,keterangan_kedisiplinan,nilai_kebersihan,keterangan_kebersihan,nilai_adab,keterangan_adab,prestasi_kesantrian,pelanggaran_kesantrian,catatan_musyrif"
    const val MONTHLY_REPORT_SELECT_EXTENDED =
      "id,periode,guru_id,kelas_id,santri_id,nilai_akhlak,predikat,catatan_wali,muhaffiz,no_hp_muhaffiz,nilai_kehadiran_halaqah,sakit_halaqah,izin_halaqah,nilai_akhlak_halaqah,keterangan_akhlak_halaqah,nilai_ujian_bulanan,keterangan_ujian_bulanan,nilai_target_hafalan,keterangan_target_hafalan,nilai_capaian_hafalan_bulanan,nilai_jumlah_hafalan_halaman,nilai_jumlah_hafalan_juz,catatan_muhaffiz,musyrif,no_hp_musyrif,nilai_kehadiran_liqa_muhasabah,sakit_liqa_muhasabah,izin_liqa_muhasabah,nilai_ibadah,keterangan_ibadah,nilai_kedisiplinan,keterangan_kedisiplinan,nilai_kebersihan,keterangan_kebersihan,nilai_adab,keterangan_adab,prestasi_kesantrian,pelanggaran_kesantrian,catatan_musyrif"
  }
}

private inline fun <T> HttpURLConnection.useJsonArrayResponse(block: (JSONArray) -> T): T {
  return try {
    val code = responseCode
    val payload = readPayload(code in 200..299)
    if (code !in 200..299) {
      throw IllegalStateException(payload.ifBlank { "HTTP $code" })
    }
    block(JSONArray(payload.ifBlank { "[]" }))
  } finally {
    disconnect()
  }
}

private fun HttpURLConnection.readPayload(useInputStream: Boolean): String {
  val stream = if (useInputStream) inputStream else errorStream
  if (stream == null) return ""
  return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
    reader.lineSequence().joinToString(separator = "")
  }
}

private fun JSONObject.cleanString(key: String): String {
  val value = opt(key)
  if (value == null || value == JSONObject.NULL) return ""
  return value.toString().trim().takeUnless { it == "null" }.orEmpty()
}

private fun String.withPercentSuffix(): String {
  val value = trim()
  if (value.isBlank()) return "-"
  return if (value.endsWith("%")) value else "$value%"
}
