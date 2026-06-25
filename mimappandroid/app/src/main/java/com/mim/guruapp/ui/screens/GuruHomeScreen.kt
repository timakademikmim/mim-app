package com.mim.guruapp.ui.screens

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.mim.guruapp.GuruSidebarParent
import com.mim.guruapp.GuruSidebarDestination
import com.mim.guruapp.APP_ROLE_ADMIN
import com.mim.guruapp.AttendanceSaveOutcome
import com.mim.guruapp.LeaveRequestSaveOutcome
import com.mim.guruapp.PatronMateriSaveOutcome
import com.mim.guruapp.QuestionSaveOutcome
import com.mim.guruapp.ScoreSaveOutcome
import com.mim.guruapp.ProfileSaveOutcome
import com.mim.guruapp.SantriSaveOutcome
import com.mim.guruapp.MutabaahSaveOutcome
import com.mim.guruapp.MonthlyExtracurricularSaveOutcome
import com.mim.guruapp.MonthlyReportSaveOutcome
import com.mim.guruapp.UtsReportSaveOutcome
import com.mim.guruapp.WakasekReviewOutcome
import com.mim.guruapp.SampleDataFactory
import com.mim.guruapp.appRoleDashboardLabel
import com.mim.guruapp.isAdminRoleDestination
import com.mim.guruapp.data.model.AttendanceHistoryEntry
import com.mim.guruapp.data.model.AttendanceApprovalRequest
import com.mim.guruapp.data.model.AppNotification
import com.mim.guruapp.data.model.CalendarEvent
import com.mim.guruapp.data.model.DashboardPayload
import com.mim.guruapp.data.model.InputAbsensiNavigationTarget
import com.mim.guruapp.data.model.LeaveRequestSnapshot
import com.mim.guruapp.data.model.MapelAttendanceSnapshot
import com.mim.guruapp.data.model.MapelPatronMateriSnapshot
import com.mim.guruapp.data.model.MapelScoreSnapshot
import com.mim.guruapp.data.model.MutabaahSnapshot
import com.mim.guruapp.data.model.MonthlyExtracurricularReport
import com.mim.guruapp.data.model.MonthlyAttendanceSummary
import com.mim.guruapp.data.model.MonthlyReportItem
import com.mim.guruapp.data.model.PatronMateriItem
import com.mim.guruapp.data.model.ScoreStudent
import com.mim.guruapp.data.model.SubjectOverview
import com.mim.guruapp.data.model.SyncBannerState
import com.mim.guruapp.data.model.TeacherOption
import com.mim.guruapp.data.model.TeachingReminderSettings
import com.mim.guruapp.data.model.UtsReportOverride
import com.mim.guruapp.data.model.WaliAttendanceDetailSnapshot
import com.mim.guruapp.data.model.WaliSantriProfile
import com.mim.guruapp.data.remote.GuruAiGenerateRequest
import com.mim.guruapp.data.remote.GuruAiGenerateResult
import com.mim.guruapp.data.remote.GuruAiTokenWallet
import com.mim.guruapp.data.remote.GuruExamQuestionItem
import com.mim.guruapp.data.remote.GuruExamQuestionSnapshot
import com.mim.guruapp.data.remote.AdminEmployee
import com.mim.guruapp.data.remote.AdminEmployeeListResult
import com.mim.guruapp.data.remote.AdminEmployeeSaveResult
import com.mim.guruapp.data.remote.AdminSchoolProfile
import com.mim.guruapp.data.remote.AdminSchoolProfileLoadResult
import com.mim.guruapp.data.remote.AdminSchoolProfileSaveResult
import com.mim.guruapp.data.remote.AdminSchoolProfileSnapshot
import com.mim.guruapp.data.remote.AdminSantri
import com.mim.guruapp.data.remote.AdminSantriLoadResult
import com.mim.guruapp.data.remote.AdminSantriSaveResult
import com.mim.guruapp.data.remote.AdminSantriSnapshot
import com.mim.guruapp.ui.components.AgendaCard
import com.mim.guruapp.ui.components.AdminKaryawanScreen
import com.mim.guruapp.ui.components.AdminSantriScreen
import com.mim.guruapp.ui.components.AdminSchoolProfileScreen
import com.mim.guruapp.ui.components.AttendanceApprovalPopup
import com.mim.guruapp.ui.components.AvailableMapelPanel
import com.mim.guruapp.ui.components.BottomNavBar
import com.mim.guruapp.ui.components.BottomNavShortcutDialog
import com.mim.guruapp.ui.components.CalendarScreen
import com.mim.guruapp.ui.components.DashboardScreenScaffold
import com.mim.guruapp.ui.components.EditProfileScreen
import com.mim.guruapp.ui.components.EmptyPlaceholderCard
import com.mim.guruapp.ui.components.HomeHeroCard
import com.mim.guruapp.ui.components.InputAbsensiScreen
import com.mim.guruapp.ui.components.InputNilaiScreen
import com.mim.guruapp.ui.components.LaporanAbsensiScreen
import com.mim.guruapp.ui.components.LaporanBulananScreen
import com.mim.guruapp.ui.components.LaporanUtsScreen
import com.mim.guruapp.ui.components.MapelToolbarCard
import com.mim.guruapp.ui.components.MapelScreen
import com.mim.guruapp.ui.components.MutabaahScreen
import com.mim.guruapp.ui.components.NotificationPopup
import com.mim.guruapp.ui.components.NoticeCard
import com.mim.guruapp.ui.components.PerizinanScreen
import com.mim.guruapp.ui.components.PlaceholderPanel
import com.mim.guruapp.ui.components.ProfileInfoRow
import com.mim.guruapp.ui.components.RaporScreen
import com.mim.guruapp.ui.components.SantriScreen
import com.mim.guruapp.ui.components.Sidebar
import com.mim.guruapp.ui.components.SidebarScrim
import com.mim.guruapp.ui.components.TeachingScheduleScreen
import com.mim.guruapp.ui.components.UjianScreen
import com.mim.guruapp.ui.components.WakasekKurikulumPage
import com.mim.guruapp.ui.components.WakasekKurikulumScreen
import com.mim.guruapp.ui.components.WebMapelCard
import com.mim.guruapp.ui.components.buildAdminSidebarContent
import com.mim.guruapp.ui.components.buildAvailableBottomNavShortcutItems
import com.mim.guruapp.ui.components.buildGuruBottomNavItems
import com.mim.guruapp.ui.components.buildGuruSidebarContent
import com.mim.guruapp.ui.i18n.t
import com.mim.guruapp.ui.theme.AppBackground
import com.mim.guruapp.ui.theme.CardBackground
import com.mim.guruapp.ui.theme.CardBorder
import com.mim.guruapp.ui.theme.HighlightCard
import com.mim.guruapp.ui.theme.PrimaryBlue
import com.mim.guruapp.ui.theme.MimGuruTheme
import com.mim.guruapp.ui.theme.PrimaryBlueDark
import com.mim.guruapp.ui.theme.SoftPanel
import com.mim.guruapp.ui.theme.SubtleInk
import com.mim.guruapp.ui.theme.SuccessTint
import com.mim.guruapp.ui.theme.WarmAccent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SidebarWidth = 304.dp
private const val MainNavigationGuideKey = "main_navigation"
private const val ProfileSectionGuide = "guide"
private const val ProfileSectionSettings = "settings"

private enum class UserGuideTourTarget {
  ContentArea,
  PageHeader,
  TopContent,
  MiddleContent,
  LowerContent,
  ListContent,
  FormContent,
  SecondaryFormContent,
  DetailContent,
  TopActions,
  BottomActions,
  FloatingAction,
  SettingsLanguage,
  SettingsTheme,
  SettingsSync,
  MenuButton,
  DashboardDate,
  DashboardNotification,
  DashboardSearch,
  DashboardCategories,
  DashboardAgenda,
  ScheduleDay,
  ScheduleTimeline,
  ScheduleToday,
  ScheduleReminderButton,
  ScheduleReminderPopup,
  BottomNavigation,
  ProfileShortcut
}

private enum class UserGuideAutoScrollDirection {
  Up,
  Down
}

private data class UserGuideAutoScrollPlan(
  val direction: UserGuideAutoScrollDirection,
  val swipes: Int
)

private data class UserGuideTourStep(
  val target: UserGuideTourTarget,
  val title: String,
  val body: String
)

private data class UserGuideTourDefinition(
  val key: String,
  val destination: GuruSidebarDestination,
  val profileSection: String = "",
  val steps: List<UserGuideTourStep>
)

private val UserGuideTourDefinitions = listOf(
  UserGuideTourDefinition(
    key = "start_app",
    destination = GuruSidebarDestination.Dashboard,
    steps = listOf(
      UserGuideTourStep(
        target = UserGuideTourTarget.TopContent,
        title = "1. Login dan Sesi",
        body = "Masukkan ID karyawan dan password pada halaman login. Jika Remember me aktif, aplikasi menyimpan sesi sehingga guru tidak perlu login ulang kecuali logout manual atau sesi tidak valid."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.MiddleContent,
        title = "2. Persiapan Data Awal",
        body = "Setelah login, welcome screen menyiapkan data awal. Instalasi pertama bisa lebih lama karena aplikasi mengunduh banyak data, sedangkan pembukaan berikutnya memakai cache lokal lebih dulu."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.DashboardSearch,
        title = "3. Sinkronisasi Manual",
        body = "Tarik halaman ke bawah untuk menyinkronkan data terbaru dari server. Jika jaringan belum stabil, gunakan data lokal yang sudah ada lalu sinkronkan saat koneksi normal."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.BottomNavigation,
        title = "4. Masuk ke Fitur Harian",
        body = "Setelah dashboard terbuka, gunakan bottom navigation atau sidebar untuk masuk ke fitur harian seperti jadwal, mapel, input absensi, dan profil."
      )
    )
  ),
  UserGuideTourDefinition(
    key = MainNavigationGuideKey,
    destination = GuruSidebarDestination.Dashboard,
    steps = listOf(
      UserGuideTourStep(
        target = UserGuideTourTarget.MenuButton,
        title = "1. Tombol Sidebar",
        body = "Tekan tombol garis menu di kiri atas untuk membuka sidebar. Sidebar adalah daftar lengkap fitur aplikasi."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.TopContent,
        title = "2. Isi Sidebar",
        body = "Sidebar dikelompokkan menjadi Dashboard, Kinerja Guru, Akademik, Aktivitas Harian, Kelas Saya, Wakasek Akademik, dan Profil. Beberapa grup hanya muncul jika guru punya hak akses."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.BottomNavigation,
        title = "3. Bottom Navigation",
        body = "Bottom navigation di bawah layar adalah shortcut harian. Label selalu terlihat agar guru tidak menebak fungsi ikon."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.BottomNavigation,
        title = "4. Atur Shortcut",
        body = "Tekan lama area bottom navigation untuk membuka popup Atur Shortcut. Pilih menu yang sering dipakai, lalu ubah urutan dengan tombol geser ke atas atau ke bawah."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.ProfileShortcut,
        title = "5. Profil",
        body = "Shortcut Profil membawa guru ke data akun, pengaturan bahasa dan tema, serta Panduan Pengguna. Jika shortcut Profil tidak tampil, menu Profil tetap tersedia dari sidebar."
      )
    )
  ),
  UserGuideTourDefinition(
    key = "dashboard_agenda",
    destination = GuruSidebarDestination.Dashboard,
    steps = listOf(
      UserGuideTourStep(
        target = UserGuideTourTarget.MenuButton,
        title = "1. Tombol Menu",
        body = "Tombol menu di kiri atas membuka sidebar berisi semua fitur aplikasi."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.DashboardDate,
        title = "2. Tanggal Dashboard",
        body = "Tanggal di tengah header dapat ditekan untuk membuka kalender dan timeline kegiatan pada tanggal tertentu."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.DashboardNotification,
        title = "3. Tombol Notifikasi",
        body = "Tombol notifikasi di kanan atas menampilkan pesan belum dibaca. Badge menunjukkan jumlah notifikasi yang perlu dicek."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.DashboardCategories,
        title = "4. Kategori Agenda",
        body = "Pilih kategori untuk memfilter agenda. Semua Kategori menampilkan agenda utama, sedangkan kategori lain menyaring kegiatan sesuai jenis kalender."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.DashboardSearch,
        title = "5. Search Agenda",
        body = "Gunakan kolom pencarian untuk mencari agenda berdasarkan judul, keterangan, atau kategori. Cukup ketik sebagian kata, daftar agenda akan menyesuaikan."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.DashboardAgenda,
        title = "6. Daftar Agenda",
        body = "Area ini menampilkan agenda yang cocok dengan filter dan pencarian. Jika kosong, berarti tidak ada agenda yang sesuai dengan pilihan saat ini."
      )
    )
  ),
  UserGuideTourDefinition(
    key = "teaching_schedule",
    destination = GuruSidebarDestination.Jadwal,
    steps = listOf(
      UserGuideTourStep(
        target = UserGuideTourTarget.ScheduleDay,
        title = "1. Pilih Hari",
        body = "Halaman Jadwal menampilkan pilihan hari dalam satu pekan. Geser horizontal untuk melihat hari lain, lalu pilih tanggal yang ingin dicek."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.ScheduleTimeline,
        title = "2. Timeline Jadwal",
        body = "Timeline menampilkan mapel, kelas, jam pelajaran, dan status jadwal. Kartu jadwal disusun berdasarkan waktu."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.ScheduleToday,
        title = "3. Tombol Hari Ini",
        body = "Gunakan tombol Hari ini untuk kembali ke tanggal sekarang jika guru sudah berpindah ke tanggal lain."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.ScheduleReminderButton,
        title = "4. Pengingat Jadwal",
        body = "Ikon pengingat membuka pengaturan notifikasi jam pelajaran. Guru dapat mengatur pengingat untuk semua mapel atau mapel tertentu."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.ScheduleReminderPopup,
        title = "5. Notifikasi dan Alarm",
        body = "Di popup pengingat, aktifkan pengingat, pilih target mapel, frekuensi, dan nada alarm. Notifikasi tampil sebagai pemberitahuan HP, sedangkan alarm tampil lebih menonjol saat aplikasi aktif."
      )
    )
  ),
  UserGuideTourDefinition(
    key = "attendance_input",
    destination = GuruSidebarDestination.InputAbsensi,
    steps = listOf(
      UserGuideTourStep(
        target = UserGuideTourTarget.FormContent,
        title = "1. Pilih Tanggal",
        body = "Pilih tanggal terlebih dahulu. Aplikasi akan mencari jadwal guru pada tanggal tersebut lalu menyiapkan kelas, mapel, dan jam pelajaran."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.FormContent,
        title = "2. Pilih Jadwal",
        body = "Jika ada lebih dari satu jadwal, pilih jadwal yang sesuai dari dropdown. Jika kosong, coba refresh atau periksa apakah tanggal itu memang punya jadwal."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.SecondaryFormContent,
        title = "3. Isi Materi",
        body = "Isi materi yang diajarkan secara manual atau pilih dari patron materi. Jika ada materi terakhir, kartu pengingat dapat ditekan untuk mengisi otomatis."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.SecondaryFormContent,
        title = "4. Guru Pengganti",
        body = "Gunakan bagian guru pengganti jika sedang menggantikan guru lain atau mengamanatkan absensi kepada guru pengganti. Data pengganti mengikuti alur review yang tersedia."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.ListContent,
        title = "5. Status Santri",
        body = "Setiap baris santri memiliki dropdown status seperti Hadir, Terlambat, Izin, Sakit, atau Alpa. Ubah status sesuai kondisi sebenarnya."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.BottomActions,
        title = "6. Simpan Absensi",
        body = "Setelah status dan materi sesuai, tekan Simpan. Data masuk ke riwayat absensi dan dapat dipakai pada laporan kelas."
      )
    )
  ),
  UserGuideTourDefinition(
    key = "score_input",
    destination = GuruSidebarDestination.InputNilai,
    steps = listOf(
      UserGuideTourStep(
        target = UserGuideTourTarget.FormContent,
        title = "1. Pilih Data Nilai",
        body = "Pilih tanggal, kelas, mapel, dan jenis nilai. Input nilai tidak memakai jam pelajaran, guru pengganti, atau materi."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.FormContent,
        title = "2. Jenis Nilai",
        body = "Pilih jenis nilai seperti tugas, ulangan harian, UTS, UAS, atau jenis lain yang tersedia. Jenis nilai menentukan batas maksimal dan rekapnya."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.ListContent,
        title = "3. Isi Nilai Santri",
        body = "Masukkan nilai pada kolom tiap santri. Nilai kosong dihitung sebagai 0 saat disimpan, dan nilai melebihi batas maksimal akan dicegah."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.BottomActions,
        title = "4. Simpan Nilai",
        body = "Tombol Simpan menyimpan nilai semua santri sekaligus. Setelah tersimpan, data muncul di detail mapel bagian Nilai."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.DetailContent,
        title = "5. Edit dari Data Sumber",
        body = "Jika angka akumulasi perlu diubah, buka detail jenis nilai agar guru mengedit data sumbernya, bukan hanya angka rata-rata."
      )
    )
  ),
  UserGuideTourDefinition(
    key = "subject_detail",
    destination = GuruSidebarDestination.Mapel,
    steps = listOf(
      UserGuideTourStep(
        target = UserGuideTourTarget.ListContent,
        title = "1. Daftar Mapel",
        body = "Halaman Mapel menampilkan mata pelajaran yang diampu guru. Tekan card mapel untuk membuka detail."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.BottomActions,
        title = "2. Detail Mapel",
        body = "Di detail mapel, bottom navigation berubah menjadi tab khusus: Absensi, Nilai, Patron Materi, dan Soal."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.ListContent,
        title = "3. Tab Absensi",
        body = "Tab Absensi bisa disortir per nama atau per tanggal. Tekan card untuk melihat riwayat, lalu tekan lama riwayat jika perlu mengubah status."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.DetailContent,
        title = "4. Tab Nilai",
        body = "Tab Nilai bisa disortir per nama, jenis nilai, atau tanggal. Tekan jenis nilai untuk melihat rincian dan mengedit nilai sumber."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.SecondaryFormContent,
        title = "5. Patron Materi",
        body = "Patron Materi dipakai untuk menyiapkan daftar materi yang bisa dipilih saat input absensi. Tambahan materi muncul di atas agar mudah diisi."
      )
    )
  ),
  UserGuideTourDefinition(
    key = "question_export",
    destination = GuruSidebarDestination.Mapel,
    steps = listOf(
      UserGuideTourStep(
        target = UserGuideTourTarget.BottomActions,
        title = "1. Buka Tab Soal",
        body = "Pilih mapel, lalu masuk ke tab Soal. Dari sini guru bisa membuat file soal bebas tanpa bergantung pada jadwal ujian admin."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.FormContent,
        title = "2. Buat Soal",
        body = "Popup buat soal berisi judul, kategori seperti Tugas atau UTS, tanggal, tahun ajaran, instruksi, dan bahasa cetak jika tersedia."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.DetailContent,
        title = "3. Editor Soal",
        body = "Menekan card soal membuka editor. Bottom navigation disembunyikan agar ruang menulis lebih luas, dan tombol back di topbar kembali ke daftar soal."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.FloatingAction,
        title = "4. Tambah Model",
        body = "Tombol plus di kanan bawah membuka pilihan Tambah Model Soal. Pilih model seperti Pilihan Ganda, Esai, Isian, Benar/Salah, Pasangkan Kata, Cari Kata, atau Teka-teki Silang."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.MiddleContent,
        title = "5. Isi Pertanyaan",
        body = "Setiap model memiliki area pertanyaan. Pilihan ganda menyediakan pilihan jawaban, sedangkan puzzle memakai grid yang bisa digeser jika kolom banyak."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.TopActions,
        title = "6. Cetak atau Kirim",
        body = "Gunakan titik tiga di card soal untuk edit detail, cetak, kirim, atau hapus. Hasil cetak memakai data terbaru dari editor."
      )
    )
  ),
  UserGuideTourDefinition(
    key = "mutabaah",
    destination = GuruSidebarDestination.Tugas,
    steps = listOf(
      UserGuideTourStep(
        target = UserGuideTourTarget.ListContent,
        title = "1. Timeline Mutabaah",
        body = "Mutabaah menampilkan tugas harian dalam timeline. Setiap card memiliki indikator dan tombol centang untuk menandai tugas selesai."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.MiddleContent,
        title = "2. Hari Tanpa Jadwal",
        body = "Jika guru tidak memiliki jadwal mengajar, hanya tugas hadir tepat waktu dan pulang tepat waktu yang ditampilkan."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.MiddleContent,
        title = "3. Hari Izin",
        body = "Jika izin guru disetujui, hari itu ditandai Izin dan tidak dihitung sebagai tugas gagal."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.TopActions,
        title = "4. Cetak dan Kirim",
        body = "Titik tiga di topbar membuka Cetak Mutabaah dan Kirim Mutabaah. Keduanya meminta periode terlebih dahulu."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.FormContent,
        title = "5. Pilih Periode",
        body = "Popup periode berisi pilihan bulan, semester, atau tahun. Jika beberapa bulan dipilih, file Excel dibuat dengan satu tab untuk setiap bulan."
      )
    )
  ),
  UserGuideTourDefinition(
    key = "permission",
    destination = GuruSidebarDestination.Perizinan,
    steps = listOf(
      UserGuideTourStep(
        target = UserGuideTourTarget.FormContent,
        title = "1. Isi Pengajuan",
        body = "Isi tanggal mulai, tanggal selesai, durasi, dan keperluan izin. Tanggal dipilih dari picker agar format konsisten."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.BottomActions,
        title = "2. Kirim Pengajuan",
        body = "Setelah data lengkap, tekan kirim. Status awal biasanya Menunggu sampai ditinjau oleh pihak terkait."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.ListContent,
        title = "3. Pantau Status",
        body = "Daftar pengajuan menampilkan status Menunggu, Diterima, atau Ditolak. Pengajuan tertentu masih bisa dibatalkan atau dihapus."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.DetailContent,
        title = "4. Review Wakasek",
        body = "Untuk wakasek akademik, detail izin dapat dibuka untuk memberi catatan lalu memilih Setujui atau Tolak."
      )
    )
  ),
  UserGuideTourDefinition(
    key = "reports",
    destination = GuruSidebarDestination.LaporanBulanan,
    steps = listOf(
      UserGuideTourStep(
        target = UserGuideTourTarget.FormContent,
        title = "1. Pilih Periode",
        body = "Pilih periode laporan yang tersedia, lalu tekan nama santri untuk membuka detail laporan."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.DetailContent,
        title = "2. Detail Laporan",
        body = "Detail laporan memisahkan aspek penilaian dalam card agar mudah dibaca. Nilai angka dan teks keterangan ditampilkan sesuai jenis datanya."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.MiddleContent,
        title = "3. Edit Khusus Laporan",
        body = "Mode edit mengubah data khusus laporan tanpa mengubah data asli sistem seperti absensi atau nilai sumber."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.TopActions,
        title = "4. Reset Data",
        body = "Tombol reset mengembalikan laporan ke data sistem jika guru ingin membatalkan edit khusus laporan."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.BottomActions,
        title = "5. Cetak dan Kirim",
        body = "Cetak menghasilkan PDF sesuai template. Kirim WhatsApp membuka pilihan nomor orang tua atau input manual."
      )
    )
  ),
  UserGuideTourDefinition(
    key = "student_teacher",
    destination = GuruSidebarDestination.Santri,
    steps = listOf(
      UserGuideTourStep(
        target = UserGuideTourTarget.PageHeader,
        title = "1. Akses Wali Kelas",
        body = "Menu Santri dan laporan kelas hanya muncul jika guru terdaftar sebagai wali kelas."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.ListContent,
        title = "2. Daftar Santri",
        body = "Daftar santri menampilkan santri di kelas wali. Tekan nama santri untuk membuka biodata dan riwayat akademik."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.FormContent,
        title = "3. Biodata Santri",
        body = "Biodata berisi nama, NISN, kontak, alamat, orang tua, wali, dan catatan. Jenis kelamin dipilih dari dropdown."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.DetailContent,
        title = "4. Riwayat Akademik",
        body = "Riwayat akademik berisi kelas, rapor, laporan bulanan, dan nilai santri dari periode sebelumnya."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.BottomActions,
        title = "5. Simpan Perubahan",
        body = "Jika ada perubahan biodata, tombol simpan muncul. Data dikirim ke server dan cache lokal diperbarui."
      )
    )
  ),
  UserGuideTourDefinition(
    key = "wakasek",
    destination = GuruSidebarDestination.WakasekMonitoringGuru,
    steps = listOf(
      UserGuideTourStep(
        target = UserGuideTourTarget.PageHeader,
        title = "1. Akses Wakasek",
        body = "Menu Wakasek Akademik muncul untuk akun yang tercatat sebagai wakasek akademik."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.ListContent,
        title = "2. Monitoring Guru",
        body = "Monitoring Guru menyediakan periode harian, pekanan, bulanan, dan semester. Detail guru menampilkan kelas, mapel, waktu, dan status."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.ListContent,
        title = "3. Monitoring Siswa",
        body = "Monitoring Siswa menampilkan santri sakit, izin, terlambat, atau alpa. Santri hadir tidak dimunculkan agar daftar tetap fokus."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.TopContent,
        title = "4. Sort dan Filter",
        body = "Gunakan sort atau filter kelas, nama, dan status untuk menemukan data monitoring yang dibutuhkan."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.DetailContent,
        title = "5. Perizinan Wakasek",
        body = "Perizinan Wakasek dipakai untuk melihat pengajuan izin guru, memberi catatan, lalu menyetujui atau menolak."
      )
    )
  ),
  UserGuideTourDefinition(
    key = "settings_sync",
    destination = GuruSidebarDestination.Profil,
    profileSection = ProfileSectionSettings,
    steps = listOf(
      UserGuideTourStep(
        target = UserGuideTourTarget.PageHeader,
        title = "1. Buka Pengaturan",
        body = "Bagian Pengaturan di profil berisi bahasa, tema, sinkronisasi, keamanan, dan preferensi aplikasi."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.SettingsLanguage,
        title = "2. Bahasa",
        body = "Pilih bahasa Indonesia, Inggris, atau Arab lalu tekan Terapkan. Data dari database seperti nama santri dan mapel tidak diterjemahkan."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.SettingsTheme,
        title = "3. Tema",
        body = "Pilih Ikuti Sistem, Terang, atau Gelap sesuai kebutuhan perangkat dan kenyamanan membaca."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.SettingsSync,
        title = "4. Sinkronisasi Data",
        body = "Gunakan Sinkronisasi Data untuk menarik ulang data terbaru dari server dan memperbarui cache lokal."
      ),
      UserGuideTourStep(
        target = UserGuideTourTarget.LowerContent,
        title = "5. Pembaruan Aplikasi",
        body = "Jika versi baru tersedia, aplikasi menampilkan popup pembaruan saat dibuka. Popup berisi informasi versi dan tombol unduh APK terbaru."
      )
    )
  )
)

private fun userGuideTourDefinition(key: String?): UserGuideTourDefinition? {
  return UserGuideTourDefinitions.firstOrNull { it.key == key }
}

private fun resolveUserGuideTargetRect(
  target: UserGuideTourTarget,
  targets: Map<UserGuideTourTarget, Rect>
): Rect? {
  targets[target]?.let { return it }
  if (target == UserGuideTourTarget.ProfileShortcut) {
    targets[UserGuideTourTarget.BottomNavigation]?.let { return it }
  }
  val content = targets[UserGuideTourTarget.ContentArea] ?: return null
  return when (target) {
    UserGuideTourTarget.ContentArea -> content.region(0.06f, 0.16f, 0.94f, 0.62f)
    UserGuideTourTarget.PageHeader -> content.region(0.00f, 0.00f, 1.00f, 0.16f)
    UserGuideTourTarget.TopContent -> content.region(0.05f, 0.14f, 0.95f, 0.32f)
    UserGuideTourTarget.MiddleContent -> content.region(0.06f, 0.32f, 0.94f, 0.58f)
    UserGuideTourTarget.LowerContent -> content.region(0.06f, 0.58f, 0.94f, 0.82f)
    UserGuideTourTarget.ListContent -> content.region(0.04f, 0.38f, 0.96f, 0.84f)
    UserGuideTourTarget.FormContent -> content.region(0.04f, 0.18f, 0.96f, 0.40f)
    UserGuideTourTarget.SecondaryFormContent -> content.region(0.04f, 0.36f, 0.96f, 0.58f)
    UserGuideTourTarget.DetailContent -> content.region(0.04f, 0.24f, 0.96f, 0.72f)
    UserGuideTourTarget.TopActions -> content.region(0.48f, 0.04f, 0.98f, 0.22f)
    UserGuideTourTarget.BottomActions -> content.region(0.12f, 0.70f, 0.88f, 0.88f)
    UserGuideTourTarget.FloatingAction -> content.region(0.72f, 0.66f, 0.96f, 0.88f)
    UserGuideTourTarget.SettingsLanguage -> content.region(0.04f, 0.18f, 0.96f, 0.36f)
    UserGuideTourTarget.SettingsTheme -> content.region(0.04f, 0.36f, 0.96f, 0.56f)
    UserGuideTourTarget.SettingsSync -> content.region(0.04f, 0.56f, 0.96f, 0.76f)
    UserGuideTourTarget.MenuButton -> content.region(0.00f, 0.00f, 0.22f, 0.16f)
    UserGuideTourTarget.DashboardDate -> content.region(0.24f, 0.00f, 0.76f, 0.16f)
    UserGuideTourTarget.DashboardNotification -> content.region(0.78f, 0.00f, 1.00f, 0.16f)
    UserGuideTourTarget.DashboardSearch -> content.region(0.04f, 0.14f, 0.96f, 0.26f)
    UserGuideTourTarget.DashboardCategories -> content.region(0.00f, 0.28f, 1.00f, 0.44f)
    UserGuideTourTarget.DashboardAgenda -> content.region(0.04f, 0.52f, 0.96f, 0.86f)
    UserGuideTourTarget.ScheduleDay -> content.region(0.38f, 0.28f, 0.62f, 0.48f)
    UserGuideTourTarget.ScheduleTimeline -> content.region(0.04f, 0.54f, 0.96f, 0.90f)
    UserGuideTourTarget.ScheduleToday -> content.region(0.68f, 0.18f, 0.96f, 0.30f)
    UserGuideTourTarget.ScheduleReminderButton -> content.region(0.82f, 0.46f, 0.98f, 0.58f)
    UserGuideTourTarget.ScheduleReminderPopup -> content.region(0.06f, 0.18f, 0.94f, 0.82f)
    UserGuideTourTarget.BottomNavigation -> content.region(0.04f, 0.82f, 0.96f, 0.98f)
    UserGuideTourTarget.ProfileShortcut -> content.region(0.76f, 0.82f, 0.96f, 0.98f)
  }
}

private fun userGuideAutoScrollPlan(target: UserGuideTourTarget): UserGuideAutoScrollPlan? {
  return when (target) {
    UserGuideTourTarget.PageHeader,
    UserGuideTourTarget.TopContent,
    UserGuideTourTarget.TopActions,
    UserGuideTourTarget.MenuButton,
    UserGuideTourTarget.DashboardDate,
    UserGuideTourTarget.DashboardNotification,
    UserGuideTourTarget.DashboardSearch,
    UserGuideTourTarget.DashboardCategories,
    UserGuideTourTarget.SettingsLanguage,
    UserGuideTourTarget.ScheduleDay,
    UserGuideTourTarget.ScheduleToday,
    UserGuideTourTarget.ScheduleReminderButton -> UserGuideAutoScrollPlan(UserGuideAutoScrollDirection.Up, 1)

    UserGuideTourTarget.MiddleContent,
    UserGuideTourTarget.ListContent,
    UserGuideTourTarget.SecondaryFormContent,
    UserGuideTourTarget.DetailContent,
    UserGuideTourTarget.SettingsTheme,
    UserGuideTourTarget.ScheduleTimeline -> UserGuideAutoScrollPlan(UserGuideAutoScrollDirection.Down, 1)

    UserGuideTourTarget.LowerContent,
    UserGuideTourTarget.BottomActions,
    UserGuideTourTarget.FloatingAction,
    UserGuideTourTarget.SettingsSync,
    UserGuideTourTarget.DashboardAgenda,
    UserGuideTourTarget.BottomNavigation,
    UserGuideTourTarget.ProfileShortcut -> UserGuideAutoScrollPlan(UserGuideAutoScrollDirection.Down, 2)

    UserGuideTourTarget.ContentArea,
    UserGuideTourTarget.FormContent,
    UserGuideTourTarget.ScheduleReminderPopup -> null
  }
}

private fun View.dispatchGuideSwipe(direction: UserGuideAutoScrollDirection) {
  if (width <= 0 || height <= 0) return

  val startTime = SystemClock.uptimeMillis()
  val x = width * 0.5f
  val fromY = when (direction) {
    UserGuideAutoScrollDirection.Down -> height * 0.74f
    UserGuideAutoScrollDirection.Up -> height * 0.30f
  }
  val toY = when (direction) {
    UserGuideAutoScrollDirection.Down -> height * 0.30f
    UserGuideAutoScrollDirection.Up -> height * 0.74f
  }
  val moveSteps = 8

  dispatchRecycledMotionEvent(startTime, startTime, MotionEvent.ACTION_DOWN, x, fromY)
  for (step in 1..moveSteps) {
    val progress = step / moveSteps.toFloat()
    val y = fromY + ((toY - fromY) * progress)
    dispatchRecycledMotionEvent(
      downTime = startTime,
      eventTime = startTime + step * 16L,
      action = MotionEvent.ACTION_MOVE,
      x = x,
      y = y
    )
  }
  dispatchRecycledMotionEvent(
    downTime = startTime,
    eventTime = startTime + (moveSteps + 1) * 16L,
    action = MotionEvent.ACTION_UP,
    x = x,
    y = toY
  )
}

private fun View.dispatchRecycledMotionEvent(
  downTime: Long,
  eventTime: Long,
  action: Int,
  x: Float,
  y: Float
) {
  val event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0)
  dispatchTouchEvent(event)
  event.recycle()
}

private fun Rect.region(
  leftFraction: Float,
  topFraction: Float,
  rightFraction: Float,
  bottomFraction: Float
): Rect {
  val width = this.width
  val height = this.height
  return Rect(
    left = left + width * leftFraction.coerceIn(0f, 1f),
    top = top + height * topFraction.coerceIn(0f, 1f),
    right = left + width * rightFraction.coerceIn(0f, 1f),
    bottom = top + height * bottomFraction.coerceIn(0f, 1f)
  )
}

private data class GuruHomeContentTarget(
  val destination: GuruSidebarDestination,
  val isCalendarOpen: Boolean
)

private fun GuruHomeContentTarget.transitionOrder(): Int {
  if (destination == GuruSidebarDestination.Dashboard && isCalendarOpen) return 1
  return when (destination) {
    GuruSidebarDestination.Dashboard -> 0
    GuruSidebarDestination.Tugas -> 2
    GuruSidebarDestination.Mapel -> 2
    GuruSidebarDestination.Jadwal -> 3
    GuruSidebarDestination.InputNilai,
    GuruSidebarDestination.InputAbsensi -> 4
    GuruSidebarDestination.Perizinan,
    GuruSidebarDestination.AdminKaryawan,
    GuruSidebarDestination.AdminProfilSekolah,
    GuruSidebarDestination.AdminKalenderTahunAjaran,
    GuruSidebarDestination.AdminKelasMapel,
    GuruSidebarDestination.AdminSantri,
    GuruSidebarDestination.AdminJadwalUjian,
    GuruSidebarDestination.AdminEkstrakurikuler,
    GuruSidebarDestination.AdminTahfiz,
    GuruSidebarDestination.AdminAsrama,
    GuruSidebarDestination.AdminPresensiIzin,
    GuruSidebarDestination.AdminMutabaahKaryawan,
    GuruSidebarDestination.Profil,
    GuruSidebarDestination.Pesan,
    GuruSidebarDestination.Notifikasi -> 5
    GuruSidebarDestination.WakasekMonitoringGuru,
    GuruSidebarDestination.WakasekMonitoringSiswa,
    GuruSidebarDestination.WakasekNilaiSiswa,
    GuruSidebarDestination.WakasekPerizinan -> 6
    else -> 6
  }
}

@Composable
fun GuruHomeScreen(
  dashboard: DashboardPayload,
  activeRole: String,
  availableRoles: List<String>,
  syncBanner: SyncBannerState,
  selectedDestination: GuruSidebarDestination,
  isCalendarScreenOpen: Boolean,
  isNotificationPopupOpen: Boolean,
  selectedCalendarDateIso: String,
  teachingReminderSettings: TeachingReminderSettings,
  pendingInputAbsensiTarget: InputAbsensiNavigationTarget?,
  isSidebarOpen: Boolean,
  expandedSidebarParent: GuruSidebarParent?,
  bottomNavShortcutDestinations: List<GuruSidebarDestination>,
  languageCode: String,
  themeModeCode: String,
  isClaimSectionVisible: Boolean,
  selectedClaimSubjectIds: Set<String>,
  onToggleSidebar: () -> Unit,
  onCloseSidebar: () -> Unit,
  onToggleSidebarParent: (GuruSidebarParent) -> Unit,
  onSelectDestination: (GuruSidebarDestination) -> Unit,
  onOpenRolePicker: () -> Unit,
  onUpdateBottomNavShortcuts: (List<GuruSidebarDestination>) -> Unit,
  onOpenCalendarScreen: () -> Unit,
  onCloseCalendarScreen: () -> Unit,
  onOpenNotificationPopup: () -> Unit,
  onCloseNotificationPopup: () -> Unit,
  onMarkNotificationAsRead: (String) -> Unit,
  onMarkAllNotificationsAsRead: () -> Unit,
  onUpdateTeachingReminderSettings: (TeachingReminderSettings) -> Unit,
  onOpenInputAbsensiTarget: (InputAbsensiNavigationTarget) -> Unit,
  onConsumePendingInputAbsensiTarget: () -> Unit,
  onLoadAttendanceApprovalRequest: suspend (String) -> AttendanceApprovalRequest?,
  onReviewAttendanceApproval: suspend (String, Boolean, String) -> AttendanceSaveOutcome,
  onSelectCalendarDate: (String) -> Unit,
  onToggleClaimSection: () -> Unit,
  onToggleClaimSubject: (String) -> Unit,
  onClearClaimSelection: () -> Unit,
  onClaimSelectedSubjects: () -> Unit,
  onLoadMapelAttendance: suspend (String, SubjectOverview) -> MapelAttendanceSnapshot?,
  onSaveMapelAttendance: suspend (String, SubjectOverview, String, List<AttendanceHistoryEntry>) -> AttendanceSaveOutcome,
  onSaveMapelAttendanceBatch: suspend (String, SubjectOverview, Map<String, List<AttendanceHistoryEntry>>, String, String) -> AttendanceSaveOutcome,
  onDeleteMapelAttendance: suspend (String, SubjectOverview, List<String>) -> AttendanceSaveOutcome,
  onSendMapelAttendanceDelegation: suspend (String, SubjectOverview, String, List<String>, String, String) -> AttendanceSaveOutcome,
  onLoadSubstituteTeacherContext: suspend (TeacherOption, String) -> Pair<List<SubjectOverview>, List<CalendarEvent>>,
  onLoadDelegatedAttendanceContext: suspend (String) -> Pair<List<SubjectOverview>, List<CalendarEvent>>,
  onLoadMapelScores: suspend (String, SubjectOverview) -> MapelScoreSnapshot?,
  onSaveMapelScores: suspend (String, SubjectOverview, ScoreStudent) -> ScoreSaveOutcome,
  onSaveMapelScoresBatch: suspend (String, SubjectOverview, List<ScoreStudent>) -> ScoreSaveOutcome,
  onLoadMapelPatronMateri: suspend (String, SubjectOverview) -> MapelPatronMateriSnapshot?,
  onSaveMapelPatronMateri: suspend (String, SubjectOverview, List<PatronMateriItem>) -> PatronMateriSaveOutcome,
  onLoadMapelQuestions: suspend (String, SubjectOverview) -> String?,
  onSaveMapelQuestions: suspend (String, SubjectOverview, String) -> QuestionSaveOutcome,
  onLoadMapelRaporDescriptions: suspend (String, SubjectOverview) -> String?,
  onSaveMapelRaporDescriptions: suspend (String, SubjectOverview, String) -> QuestionSaveOutcome,
  onLoadExamQuestions: suspend () -> GuruExamQuestionSnapshot,
  onSaveExamQuestions: suspend (GuruExamQuestionItem, String) -> QuestionSaveOutcome,
  onLoadAiTokenBalance: suspend () -> GuruAiTokenWallet?,
  onGenerateAiContent: suspend (GuruAiGenerateRequest) -> GuruAiGenerateResult,
  onLoadAdminEmployees: suspend () -> AdminEmployeeListResult,
  onSaveAdminEmployee: suspend (AdminEmployee, String) -> AdminEmployeeSaveResult,
  onLoadAdminSchoolProfile: suspend () -> AdminSchoolProfileLoadResult,
  onSaveAdminSchoolProfile: suspend (AdminSchoolProfile) -> AdminSchoolProfileSaveResult,
  onLoadAdminSantri: suspend () -> AdminSantriLoadResult,
  onSaveAdminSantri: suspend (AdminSantri) -> AdminSantriSaveResult,
  onPromoteAdminSantri: suspend (AdminSantri) -> AdminSantriSaveResult,
  onGraduateAdminSantri: suspend (AdminSantri) -> AdminSantriSaveResult,
  onSaveProfile: suspend (com.mim.guruapp.data.model.GuruProfile) -> ProfileSaveOutcome,
  onSaveSantri: suspend (WaliSantriProfile) -> SantriSaveOutcome,
  onSaveMonthlyReport: suspend (MonthlyReportItem) -> MonthlyReportSaveOutcome,
  onSaveMonthlyExtracurricularReports: suspend (List<MonthlyExtracurricularReport>) -> MonthlyExtracurricularSaveOutcome,
  onLoadMonthlyAttendanceSummaries: suspend (String) -> List<MonthlyAttendanceSummary>,
  onLoadMonthlyAttendanceDetail: suspend (String, WaliSantriProfile) -> WaliAttendanceDetailSnapshot?,
  onSaveUtsReportOverride: suspend (UtsReportOverride) -> UtsReportSaveOutcome,
  onLoadMutabaah: suspend (LocalDate) -> MutabaahSnapshot?,
  onSaveMutabaahStatus: suspend (String, String, Boolean) -> MutabaahSaveOutcome,
  onSaveMutabaahStatuses: suspend (List<String>, String, String) -> MutabaahSaveOutcome,
  onLoadLeaveRequests: suspend () -> LeaveRequestSnapshot?,
  onSubmitLeaveRequest: suspend (String, String, String) -> LeaveRequestSaveOutcome,
  onDeleteLeaveRequest: suspend (String) -> LeaveRequestSaveOutcome,
  onReviewWakasekLeaveRequest: suspend (String, Boolean, String) -> WakasekReviewOutcome,
  onApplyLanguage: (String) -> Unit,
  onApplyThemeMode: (String) -> Unit,
  onRefreshClick: () -> Unit,
  onLogoutClick: () -> Unit
) {
  val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
    .withZone(ZoneId.systemDefault())
  val scope = rememberCoroutineScope()
  val rootView = LocalView.current
  val isAdminMode = activeRole == APP_ROLE_ADMIN
  val canChangeRole = availableRoles.size > 1
  val selectedCalendarDate = runCatching { LocalDate.parse(selectedCalendarDateIso) }
    .getOrDefault(LocalDate.now())
  val adminBottomNavDestinations = listOf(
    GuruSidebarDestination.Dashboard,
    GuruSidebarDestination.AdminKaryawan,
    GuruSidebarDestination.Profil
  )
  val availableBottomNavItems = if (isAdminMode) {
    buildGuruBottomNavItems(adminBottomNavDestinations)
  } else {
    buildAvailableBottomNavShortcutItems(
      isWaliKelas = dashboard.waliSantriSnapshot.isWaliKelas,
      isWakasekKurikulum = dashboard.wakasekKurikulumSnapshot.isWakasekKurikulum
    )
  }
  val availableBottomNavDestinations = availableBottomNavItems.map { it.destination }.toSet()
  val effectiveBottomNavShortcuts = if (isAdminMode) {
    adminBottomNavDestinations
  } else {
    bottomNavShortcutDestinations
      .filter { availableBottomNavDestinations.contains(it) || it == GuruSidebarDestination.Dashboard }
      .ifEmpty { listOf(GuruSidebarDestination.Dashboard) }
  }
  val bottomNavItems = buildGuruBottomNavItems(effectiveBottomNavShortcuts)
  var isShortcutDialogOpen by remember { mutableStateOf(false) }
  val sidebarWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { SidebarWidth.toPx() }
  val sidebarOffsetPx = remember { Animatable(-sidebarWidthPx) }
  val sidebarGestureScope = rememberCoroutineScope()
  val selectedBottomNavDestination = if (isAdminMode) {
    when (selectedDestination) {
      GuruSidebarDestination.Profil -> GuruSidebarDestination.Profil
      GuruSidebarDestination.AdminKaryawan -> GuruSidebarDestination.AdminKaryawan
      else -> GuruSidebarDestination.Dashboard
    }
  } else {
    when (selectedDestination) {
      GuruSidebarDestination.Dashboard -> GuruSidebarDestination.Dashboard.takeIf { effectiveBottomNavShortcuts.contains(it) }
      GuruSidebarDestination.Mapel -> GuruSidebarDestination.Mapel.takeIf { effectiveBottomNavShortcuts.contains(it) }
      GuruSidebarDestination.Jadwal -> GuruSidebarDestination.Jadwal.takeIf { effectiveBottomNavShortcuts.contains(it) }
      GuruSidebarDestination.InputNilai,
      GuruSidebarDestination.InputAbsensi -> GuruSidebarDestination.InputAbsensi.takeIf { effectiveBottomNavShortcuts.contains(it) }
      GuruSidebarDestination.Profil,
      GuruSidebarDestination.Pesan,
      GuruSidebarDestination.Notifikasi -> GuruSidebarDestination.Profil.takeIf { effectiveBottomNavShortcuts.contains(it) }
      else -> selectedDestination.takeIf { effectiveBottomNavShortcuts.contains(it) }
    }
  }
  val contentDestination = if (isAdminMode) {
    selectedDestination.takeIf { it.isAdminRoleDestination() } ?: GuruSidebarDestination.Dashboard
  } else {
    when (selectedDestination) {
      GuruSidebarDestination.InputNilai,
      GuruSidebarDestination.InputAbsensi -> GuruSidebarDestination.InputAbsensi
      else -> selectedDestination
    }
  }
  val sidebarSelectedDestination = if (
    isAdminMode &&
    !selectedDestination.isAdminRoleDestination()
  ) {
    GuruSidebarDestination.Dashboard
  } else {
    selectedDestination
  }
  LaunchedEffect(sidebarWidthPx) {
    if (sidebarOffsetPx.value == 0f || sidebarOffsetPx.value == -sidebarWidthPx) return@LaunchedEffect
    sidebarOffsetPx.snapTo(if (isSidebarOpen) 0f else -sidebarWidthPx)
  }
  LaunchedEffect(isSidebarOpen, sidebarWidthPx) {
    val target = if (isSidebarOpen) 0f else -sidebarWidthPx
    sidebarOffsetPx.animateTo(
      targetValue = target,
      animationSpec = tween(durationMillis = 260)
    )
  }
  val sidebarProgress = if (sidebarWidthPx == 0f) 0f else 1f - ((-sidebarOffsetPx.value) / sidebarWidthPx).coerceIn(0f, 1f)
  var isMapelDetailMode by androidx.compose.runtime.remember { mutableStateOf(false) }
  var isUjianQuestionEditorMode by androidx.compose.runtime.remember { mutableStateOf(false) }
  var isLaporanBulananDetailMode by androidx.compose.runtime.remember { mutableStateOf(false) }
  var isLaporanUtsDetailMode by androidx.compose.runtime.remember { mutableStateOf(false) }
  var isRaporDetailMode by androidx.compose.runtime.remember { mutableStateOf(false) }
  var pendingInteractiveGuideKey by androidx.compose.runtime.remember { mutableStateOf<String?>(null) }
  var activeInteractiveGuideKey by androidx.compose.runtime.remember { mutableStateOf<String?>(null) }
  var activeInteractiveGuideStepIndex by androidx.compose.runtime.remember { mutableStateOf(0) }
  var isInteractiveGuideAutoScrolling by androidx.compose.runtime.remember { mutableStateOf(false) }
  var userGuideTargetBounds by androidx.compose.runtime.remember { mutableStateOf<Map<UserGuideTourTarget, Rect>>(emptyMap()) }
  var profileGuideOpenRequest by androidx.compose.runtime.remember { mutableStateOf(0) }
  var profileSettingsOpenRequest by androidx.compose.runtime.remember { mutableStateOf(0) }
  var teachingReminderOpenRequest by androidx.compose.runtime.remember { mutableStateOf(0) }

  fun updateUserGuideTarget(target: UserGuideTourTarget, rect: Rect) {
    userGuideTargetBounds = userGuideTargetBounds + (target to rect)
  }

  fun closeInteractiveGuide(returnToGuide: Boolean = false) {
    activeInteractiveGuideKey = null
    pendingInteractiveGuideKey = null
    activeInteractiveGuideStepIndex = 0
    if (returnToGuide) {
      onCloseSidebar()
      onCloseCalendarScreen()
      onCloseNotificationPopup()
      profileGuideOpenRequest += 1
      onSelectDestination(GuruSidebarDestination.Profil)
    }
  }

  fun startInteractiveGuide(key: String) {
    val definition = userGuideTourDefinition(key) ?: return
    onCloseSidebar()
    onCloseCalendarScreen()
    onCloseNotificationPopup()
    pendingInteractiveGuideKey = key
    activeInteractiveGuideStepIndex = 0
    userGuideTargetBounds = emptyMap()
    if (definition.profileSection == ProfileSectionSettings) {
      profileSettingsOpenRequest += 1
    }
    onSelectDestination(definition.destination)
  }

  LaunchedEffect(pendingInteractiveGuideKey, selectedDestination, isCalendarScreenOpen, contentDestination) {
    val pendingDefinition = userGuideTourDefinition(pendingInteractiveGuideKey)
    if (
      pendingDefinition != null &&
      selectedDestination == pendingDefinition.destination &&
      contentDestination == when (pendingDefinition.destination) {
        GuruSidebarDestination.InputNilai,
        GuruSidebarDestination.InputAbsensi -> GuruSidebarDestination.InputAbsensi
        else -> pendingDefinition.destination
      } &&
      (pendingDefinition.destination != GuruSidebarDestination.Dashboard || !isCalendarScreenOpen)
    ) {
      activeInteractiveGuideKey = pendingInteractiveGuideKey
      pendingInteractiveGuideKey = null
      activeInteractiveGuideStepIndex = 0
    }
  }

  LaunchedEffect(activeInteractiveGuideKey, activeInteractiveGuideStepIndex, rootView) {
    val activeStep = userGuideTourDefinition(activeInteractiveGuideKey)
      ?.steps
      ?.getOrNull(activeInteractiveGuideStepIndex)
    val waitsForReminderPopup = activeStep?.target == UserGuideTourTarget.ScheduleReminderPopup
    val scrollPlan = activeStep?.target?.let(::userGuideAutoScrollPlan)
    if (scrollPlan == null && !waitsForReminderPopup) {
      isInteractiveGuideAutoScrolling = false
      return@LaunchedEffect
    }

    isInteractiveGuideAutoScrolling = true
    if (waitsForReminderPopup) {
      delay(420)
      isInteractiveGuideAutoScrolling = false
      return@LaunchedEffect
    }
    val activeScrollPlan = scrollPlan ?: run {
      isInteractiveGuideAutoScrolling = false
      return@LaunchedEffect
    }
    delay(120)
    repeat(activeScrollPlan.swipes) {
      rootView.dispatchGuideSwipe(activeScrollPlan.direction)
      delay(240)
    }
    delay(180)
    isInteractiveGuideAutoScrolling = false
  }

  LaunchedEffect(activeInteractiveGuideKey, activeInteractiveGuideStepIndex) {
    val activeStep = userGuideTourDefinition(activeInteractiveGuideKey)
      ?.steps
      ?.getOrNull(activeInteractiveGuideStepIndex)
    if (activeInteractiveGuideKey == "teaching_schedule" && activeStep?.target == UserGuideTourTarget.ScheduleReminderPopup) {
      teachingReminderOpenRequest += 1
    }
  }

  BackHandler(enabled = activeInteractiveGuideKey != null || pendingInteractiveGuideKey != null) {
    closeInteractiveGuide(returnToGuide = true)
  }

  val isFullScreenSidebarSwipeEnabled = if (isAdminMode) {
    contentDestination != GuruSidebarDestination.Dashboard || !isCalendarScreenOpen
  } else {
    when (selectedDestination) {
      GuruSidebarDestination.Dashboard -> !isCalendarScreenOpen
      GuruSidebarDestination.Tugas,
      GuruSidebarDestination.Jadwal,
      GuruSidebarDestination.Perizinan,
      GuruSidebarDestination.LaporanBulanan,
      GuruSidebarDestination.LaporanUTS,
      GuruSidebarDestination.Profil,
      GuruSidebarDestination.Santri,
      GuruSidebarDestination.Rapor -> true
      GuruSidebarDestination.Mapel -> !isMapelDetailMode
      GuruSidebarDestination.Ujian -> !isUjianQuestionEditorMode
      else -> false
    }
  }
  var isSidebarDragging by remember { mutableStateOf(false) }
  var sidebarDragStartOffset by remember { mutableStateOf(-sidebarWidthPx) }
  var sidebarDragAmount by remember { mutableStateOf(0f) }
  var selectedApprovalRequest by remember { mutableStateOf<AttendanceApprovalRequest?>(null) }
  var isLoadingApprovalRequest by remember { mutableStateOf(false) }
  var isReviewingApprovalRequest by remember { mutableStateOf(false) }
  var loadingApprovalRequestId by remember { mutableStateOf("") }
  val shouldHandleHomeBack = isSidebarOpen ||
    isLoadingApprovalRequest ||
    selectedApprovalRequest != null ||
    isNotificationPopupOpen ||
    selectedDestination != GuruSidebarDestination.Dashboard

  BackHandler(enabled = shouldHandleHomeBack) {
    when {
      isSidebarOpen -> onCloseSidebar()
      isLoadingApprovalRequest -> {
        isLoadingApprovalRequest = false
        selectedApprovalRequest = null
        loadingApprovalRequestId = ""
      }
      selectedApprovalRequest != null -> {
        selectedApprovalRequest = null
        loadingApprovalRequestId = ""
      }
      isNotificationPopupOpen -> onCloseNotificationPopup()
      selectedDestination != GuruSidebarDestination.Dashboard ->
        onSelectDestination(GuruSidebarDestination.Dashboard)
    }
  }

  val handleNotificationClick: (AppNotification) -> Unit = { notification ->
    onMarkNotificationAsRead(notification.id)
    when (notification.actionType) {
      "attendance_approval" -> {
        val submissionId = notification.actionId.trim()
        if (submissionId.isNotBlank()) {
          loadingApprovalRequestId = submissionId
          isLoadingApprovalRequest = true
          onCloseNotificationPopup()
          scope.launch {
            val request = onLoadAttendanceApprovalRequest(submissionId)
            if (loadingApprovalRequestId == submissionId) {
              selectedApprovalRequest = request
            }
            isLoadingApprovalRequest = false
          }
        }
      }
      "open_input_absensi" -> {
        val target = InputAbsensiNavigationTarget(
          requestId = "${notification.id}|${System.currentTimeMillis()}",
          dateIso = notification.actionDateIso.ifBlank { LocalDate.now().toString() },
          distribusiId = notification.actionDistribusiId,
          lessonSlotId = notification.actionLessonSlotId
        )
        onCloseNotificationPopup()
        onOpenInputAbsensiTarget(target)
      }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .pointerInput(isFullScreenSidebarSwipeEnabled, isSidebarOpen, sidebarWidthPx) {
        if (sidebarWidthPx <= 0f || (!isFullScreenSidebarSwipeEnabled && !isSidebarOpen)) {
          return@pointerInput
        }
        detectHorizontalDragGestures(
          onDragStart = {
            isSidebarDragging = true
            sidebarDragStartOffset = sidebarOffsetPx.value
            sidebarDragAmount = 0f
          },
          onDragCancel = {
            val target = if (isSidebarOpen) 0f else -sidebarWidthPx
            isSidebarDragging = false
            sidebarDragAmount = 0f
            sidebarGestureScope.launch {
              sidebarOffsetPx.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
              )
            }
          },
          onDragEnd = {
            val currentOffset = sidebarOffsetPx.value
            val shouldOpen = when {
              sidebarDragAmount > sidebarWidthPx * 0.24f -> true
              sidebarDragAmount < -sidebarWidthPx * 0.24f -> false
              else -> currentOffset > -sidebarWidthPx * 0.56f
            }
            val target = if (shouldOpen) 0f else -sidebarWidthPx
            isSidebarDragging = false
            sidebarDragAmount = 0f
            sidebarGestureScope.launch {
              sidebarOffsetPx.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
              )
              if (shouldOpen && !isSidebarOpen) {
                onToggleSidebar()
              } else if (!shouldOpen && isSidebarOpen) {
                onCloseSidebar()
              }
            }
          },
          onHorizontalDrag = { change, dragAmount ->
            sidebarDragAmount += dragAmount
            val nextOffset = (sidebarDragStartOffset + sidebarDragAmount).coerceIn(-sidebarWidthPx, 0f)
            change.consume()
            sidebarGestureScope.launch {
              sidebarOffsetPx.snapTo(nextOffset)
            }
          }
        )
      }
      .background(AppBackground)
  ) {
    AnimatedContent(
      targetState = GuruHomeContentTarget(
        destination = contentDestination,
        isCalendarOpen = selectedDestination == GuruSidebarDestination.Dashboard && isCalendarScreenOpen
      ),
      transitionSpec = {
        val forward = targetState.transitionOrder() >= initialState.transitionOrder()
        fadeIn(animationSpec = tween(180)) +
          slideInHorizontally(animationSpec = tween(260)) { width -> if (forward) width / 10 else -width / 10 } togetherWith
          fadeOut(animationSpec = tween(150)) +
          slideOutHorizontally(animationSpec = tween(220)) { width -> if (forward) -width / 12 else width / 12 }
      },
      modifier = Modifier
        .fillMaxSize()
        .onGloballyPositioned { updateUserGuideTarget(UserGuideTourTarget.ContentArea, it.boundsInRoot()) },
      label = "guru-home-content-transition"
    ) { target ->
      val targetDestination = target.destination
      if (targetDestination == GuruSidebarDestination.Dashboard) {
      if (target.isCalendarOpen) {
        CalendarScreen(
          selectedDate = selectedCalendarDate,
          events = dashboard.calendarEvents,
          isRefreshing = syncBanner.isSyncing,
          onSelectDate = { onSelectCalendarDate(it.toString()) },
          onJumpToToday = { onSelectCalendarDate(LocalDate.now().toString()) },
          onRefresh = onRefreshClick,
          onBackClick = onCloseCalendarScreen,
          modifier = Modifier.fillMaxSize()
        )
      } else {
        DashboardScreenScaffold(
          currentDate = selectedCalendarDate,
          notificationCount = dashboard.unreadNotificationCount,
          isRefreshing = syncBanner.isSyncing,
          onMenuClick = onToggleSidebar,
          onDateClick = onOpenCalendarScreen,
          onNotificationClick = onOpenNotificationPopup,
          onRefresh = onRefreshClick,
          categories = dashboard.categories,
          tasks = dashboard.ongoingTasks,
          calendarEvents = dashboard.calendarEvents,
          onMenuButtonPositioned = { updateUserGuideTarget(UserGuideTourTarget.MenuButton, it) },
          onDatePositioned = { updateUserGuideTarget(UserGuideTourTarget.DashboardDate, it) },
          onNotificationButtonPositioned = { updateUserGuideTarget(UserGuideTourTarget.DashboardNotification, it) },
          onSearchBarPositioned = { updateUserGuideTarget(UserGuideTourTarget.DashboardSearch, it) },
          onCategoriesPositioned = { updateUserGuideTarget(UserGuideTourTarget.DashboardCategories, it) },
          onAgendaPositioned = { updateUserGuideTarget(UserGuideTourTarget.DashboardAgenda, it) },
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
        )
      }
    } else if (targetDestination == GuruSidebarDestination.Jadwal) {
      TeachingScheduleScreen(
        selectedDate = selectedCalendarDate,
        events = dashboard.teachingScheduleEvents,
        reminderSettings = teachingReminderSettings,
        isRefreshing = syncBanner.isSyncing,
        onSelectDate = { onSelectCalendarDate(it.toString()) },
        onJumpToToday = { onSelectCalendarDate(LocalDate.now().toString()) },
        onRefresh = onRefreshClick,
        onMenuClick = onToggleSidebar,
        onReminderSettingsChange = onUpdateTeachingReminderSettings,
        openReminderSettingsRequest = teachingReminderOpenRequest,
        onSelectedDayPositioned = { updateUserGuideTarget(UserGuideTourTarget.ScheduleDay, it) },
        onTodayButtonPositioned = { updateUserGuideTarget(UserGuideTourTarget.ScheduleToday, it) },
        onReminderButtonPositioned = { updateUserGuideTarget(UserGuideTourTarget.ScheduleReminderButton, it) },
        onTimelinePositioned = { updateUserGuideTarget(UserGuideTourTarget.ScheduleTimeline, it) },
        onReminderSettingsPositioned = { updateUserGuideTarget(UserGuideTourTarget.ScheduleReminderPopup, it) },
        modifier = Modifier
          .fillMaxSize()
      )
    } else if (targetDestination == GuruSidebarDestination.Ujian) {
      UjianScreen(
        isRefreshing = syncBanner.isSyncing,
        onMenuClick = onToggleSidebar,
        onRefresh = onRefreshClick,
        onLoadSnapshot = onLoadExamQuestions,
        onSaveQuestion = onSaveExamQuestions,
        onEditorModeChange = { isUjianQuestionEditorMode = it },
        modifier = Modifier.fillMaxSize()
      )
    } else if (targetDestination == GuruSidebarDestination.Tugas) {
      MutabaahScreen(
        selectedDate = selectedCalendarDate,
        teacherName = dashboard.teacherName,
        snapshot = dashboard.mutabaahSnapshot,
        teachingScheduleEvents = dashboard.teachingScheduleEvents,
        leaveRequests = dashboard.leaveRequestSnapshot.requests,
        isRefreshing = syncBanner.isSyncing,
        onSelectDate = { onSelectCalendarDate(it.toString()) },
        onJumpToToday = { onSelectCalendarDate(LocalDate.now().toString()) },
        onRefresh = onRefreshClick,
        onMenuClick = onToggleSidebar,
        onLoadSnapshot = onLoadMutabaah,
        onToggleTask = onSaveMutabaahStatus,
        onToggleTasks = onSaveMutabaahStatuses,
        modifier = Modifier.fillMaxSize()
      )
    } else if (targetDestination == GuruSidebarDestination.Perizinan) {
      PerizinanScreen(
        snapshot = dashboard.leaveRequestSnapshot,
        isRefreshing = syncBanner.isSyncing,
        onMenuClick = onToggleSidebar,
        onRefresh = onRefreshClick,
        onLoadSnapshot = onLoadLeaveRequests,
        onSubmitRequest = onSubmitLeaveRequest,
        onDeleteRequest = onDeleteLeaveRequest,
        modifier = Modifier
          .fillMaxSize()
      )
    } else if (targetDestination == GuruSidebarDestination.Profil) {
      EditProfileScreen(
        profile = dashboard.profile,
        isRefreshing = syncBanner.isSyncing,
        onMenuClick = onToggleSidebar,
        onRefresh = onRefreshClick,
        languageCode = languageCode,
        onApplyLanguage = onApplyLanguage,
        themeModeCode = themeModeCode,
        onApplyThemeMode = onApplyThemeMode,
        onSaveClick = onSaveProfile,
        onStartInteractiveGuide = ::startInteractiveGuide,
        openGuideRequest = profileGuideOpenRequest,
        openSettingsRequest = profileSettingsOpenRequest,
        modifier = Modifier
          .fillMaxSize()
      )
    } else if (targetDestination == GuruSidebarDestination.AdminProfilSekolah) {
      AdminSchoolProfileScreen(
        isRefreshing = syncBanner.isSyncing,
        onMenuClick = onToggleSidebar,
        onRefresh = onRefreshClick,
        onLoadSnapshot = onLoadAdminSchoolProfile,
        onSaveProfile = onSaveAdminSchoolProfile,
        modifier = Modifier.fillMaxSize()
      )
    } else if (targetDestination == GuruSidebarDestination.AdminKaryawan) {
      AdminKaryawanScreen(
        isRefreshing = syncBanner.isSyncing,
        onMenuClick = onToggleSidebar,
        onRefresh = onRefreshClick,
        onLoadEmployees = onLoadAdminEmployees,
        onSaveEmployee = onSaveAdminEmployee,
        modifier = Modifier.fillMaxSize()
      )
    } else if (targetDestination == GuruSidebarDestination.AdminSantri) {
      AdminSantriScreen(
        isRefreshing = syncBanner.isSyncing,
        onMenuClick = onToggleSidebar,
        onRefresh = onRefreshClick,
        onLoadSantri = onLoadAdminSantri,
        onSaveSantri = onSaveAdminSantri,
        onPromoteSantri = onPromoteAdminSantri,
        onGraduateSantri = onGraduateAdminSantri,
        modifier = Modifier.fillMaxSize()
      )
    } else if (targetDestination == GuruSidebarDestination.Santri) {
      SantriScreen(
        waliSantriSnapshot = dashboard.waliSantriSnapshot,
        monthlyReportSnapshot = dashboard.monthlyReportSnapshot,
        utsReportSnapshot = dashboard.utsReportSnapshot,
        scoreSnapshots = dashboard.scoreSnapshots,
        isRefreshing = syncBanner.isSyncing,
        onMenuClick = onToggleSidebar,
        onRefresh = onRefreshClick,
        onSaveSantri = onSaveSantri,
        modifier = Modifier
          .fillMaxSize()
      )
    } else if (targetDestination == GuruSidebarDestination.Rapor) {
      RaporScreen(
        waliSantriSnapshot = dashboard.waliSantriSnapshot,
        utsReportSnapshot = dashboard.utsReportSnapshot,
        scoreSnapshots = dashboard.scoreSnapshots,
        attendanceSnapshots = dashboard.attendanceSnapshots,
        profile = dashboard.profile,
        isRefreshing = syncBanner.isSyncing,
        onMenuClick = onToggleSidebar,
        onRefresh = onRefreshClick,
        onLoadScores = onLoadMapelScores,
        onLoadRaporDescriptions = onLoadMapelRaporDescriptions,
        onLoadAttendance = onLoadMapelAttendance,
        onDetailModeChange = { isRaporDetailMode = it },
        modifier = Modifier
          .fillMaxSize()
      )
    } else if (targetDestination == GuruSidebarDestination.LaporanBulanan) {
      LaporanBulananScreen(
        waliSantriSnapshot = dashboard.waliSantriSnapshot,
        monthlyReportSnapshot = dashboard.monthlyReportSnapshot,
        profile = dashboard.profile,
        isRefreshing = syncBanner.isSyncing,
        onMenuClick = onToggleSidebar,
        onRefresh = onRefreshClick,
        onSaveReport = onSaveMonthlyReport,
        onSaveExtracurricularReports = onSaveMonthlyExtracurricularReports,
        onLoadAttendanceSummaries = onLoadMonthlyAttendanceSummaries,
        onDetailModeChange = { isLaporanBulananDetailMode = it },
        modifier = Modifier
          .fillMaxSize()
      )
    } else if (targetDestination == GuruSidebarDestination.LaporanAbsensi) {
      LaporanAbsensiScreen(
        waliSantriSnapshot = dashboard.waliSantriSnapshot,
        monthlyReportSnapshot = dashboard.monthlyReportSnapshot,
        isRefreshing = syncBanner.isSyncing,
        onMenuClick = onToggleSidebar,
        onRefresh = onRefreshClick,
        onLoadAttendanceSummaries = onLoadMonthlyAttendanceSummaries,
        onLoadAttendanceDetail = onLoadMonthlyAttendanceDetail,
        modifier = Modifier
          .fillMaxSize()
      )
    } else if (targetDestination == GuruSidebarDestination.LaporanUTS) {
      LaporanUtsScreen(
        waliSantriSnapshot = dashboard.waliSantriSnapshot,
        utsReportSnapshot = dashboard.utsReportSnapshot,
        profile = dashboard.profile,
        isRefreshing = syncBanner.isSyncing,
        onMenuClick = onToggleSidebar,
        onRefresh = onRefreshClick,
        onSaveOverride = onSaveUtsReportOverride,
        onDetailModeChange = { isLaporanUtsDetailMode = it },
        modifier = Modifier
          .fillMaxSize()
      )
    } else if (targetDestination == GuruSidebarDestination.InputAbsensi) {
      InputSwipePager(
        selectedDestination = when (selectedDestination) {
          GuruSidebarDestination.InputNilai -> GuruSidebarDestination.InputNilai
          else -> GuruSidebarDestination.InputAbsensi
        },
        onSelectDestination = onSelectDestination,
        subjects = dashboard.subjects,
        scoreSnapshots = dashboard.scoreSnapshots,
        attendanceSnapshots = dashboard.attendanceSnapshots,
        patronMateriSnapshots = dashboard.patronMateriSnapshots,
        teachingScheduleEvents = dashboard.teachingScheduleEvents,
        substituteTeacherOptions = dashboard.substituteTeacherOptions,
        sourceTeacherOptions = dashboard.sourceTeacherOptions,
        syncBanner = syncBanner,
        onMenuClick = onToggleSidebar,
        onRefresh = onRefreshClick,
        onLoadScores = onLoadMapelScores,
        onSaveScoresBatch = onSaveMapelScoresBatch,
        onLoadAttendance = onLoadMapelAttendance,
        onLoadPatronMateri = onLoadMapelPatronMateri,
        onSaveAttendanceBatch = onSaveMapelAttendanceBatch,
        onSaveAttendance = onSaveMapelAttendance,
        onSendSubstituteDelegation = onSendMapelAttendanceDelegation,
        onLoadSubstituteTeacherContext = onLoadSubstituteTeacherContext,
        onLoadDelegatedAttendanceContext = onLoadDelegatedAttendanceContext,
        inputAbsensiLaunchTarget = pendingInputAbsensiTarget,
        onInputAbsensiLaunchTargetConsumed = onConsumePendingInputAbsensiTarget,
        modifier = Modifier
          .fillMaxSize()
      )
    } else if (targetDestination == GuruSidebarDestination.Mapel) {
      MapelScreen(
        subjects = dashboard.subjects,
        syncBanner = syncBanner,
        isClaimSectionVisible = isClaimSectionVisible,
        availableSubjects = dashboard.availableSubjects,
        selectedClaimSubjectIds = selectedClaimSubjectIds,
        onToggleClaimSection = onToggleClaimSection,
        onToggleClaimSubject = onToggleClaimSubject,
        onClearClaimSelection = onClearClaimSelection,
        onClaimSelectedSubjects = onClaimSelectedSubjects,
        onLoadAttendance = onLoadMapelAttendance,
        onSaveAttendance = onSaveMapelAttendance,
        onDeleteAttendance = onDeleteMapelAttendance,
        onLoadScores = onLoadMapelScores,
        onSaveScores = onSaveMapelScores,
        onSaveScoresBatch = onSaveMapelScoresBatch,
        onLoadPatronMateri = onLoadMapelPatronMateri,
        onSavePatronMateri = onSaveMapelPatronMateri,
        onLoadQuestions = onLoadMapelQuestions,
        onSaveQuestions = onSaveMapelQuestions,
        onLoadRaporDescriptions = onLoadMapelRaporDescriptions,
        onSaveRaporDescriptions = onSaveMapelRaporDescriptions,
        onLoadAiTokenBalance = onLoadAiTokenBalance,
        onGenerateAiContent = onGenerateAiContent,
        isRefreshing = syncBanner.isSyncing,
        onRefresh = onRefreshClick,
        onDetailModeChange = { isMapelDetailMode = it },
        onMenuClick = onToggleSidebar,
        modifier = Modifier
          .fillMaxSize()
      )
    } else if (
      targetDestination == GuruSidebarDestination.WakasekMonitoringGuru ||
      targetDestination == GuruSidebarDestination.WakasekMonitoringSiswa ||
      targetDestination == GuruSidebarDestination.WakasekNilaiSiswa ||
      targetDestination == GuruSidebarDestination.WakasekPerizinan
    ) {
      WakasekKurikulumScreen(
        page = when (targetDestination) {
          GuruSidebarDestination.WakasekMonitoringSiswa -> WakasekKurikulumPage.Student
          GuruSidebarDestination.WakasekNilaiSiswa -> WakasekKurikulumPage.StudentScores
          GuruSidebarDestination.WakasekPerizinan -> WakasekKurikulumPage.Permission
          else -> WakasekKurikulumPage.Teacher
        },
        snapshot = dashboard.wakasekKurikulumSnapshot,
        scoreSnapshots = dashboard.scoreSnapshots,
        isRefreshing = syncBanner.isSyncing,
        onMenuClick = onToggleSidebar,
        onRefresh = onRefreshClick,
        onLoadScores = onLoadMapelScores,
        onReviewLeaveRequest = onReviewWakasekLeaveRequest,
        modifier = Modifier.fillMaxSize()
      )
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 124.dp)
      ) {
        item {
          GuruHomeTopBar(
            title = targetDestination.title,
            onMenuClick = onToggleSidebar,
            modifier = Modifier.padding(top = 18.dp)
          )
        }

        when (targetDestination) {

        GuruSidebarDestination.Pesan -> {
          item {
            HomeHeroCard(
              teacherName = dashboard.teacherName,
              teacherRole = dashboard.teacherRole,
              message = "Inbox pesan guru akan ditempatkan di section profile sesuai struktur drawer."
            )
          }
          item {
            PlaceholderPanel(
              title = "Pesan",
              subtitle = "Badge dan item navigasinya sudah aktif di sidebar. Konten native pesan bisa kita bangun pada langkah berikutnya."
            ) {
              EmptyPlaceholderCard("Belum ada screen native untuk pesan.")
            }
          }
        }

        else -> {
          item {
            HomeHeroCard(
              teacherName = dashboard.teacherName,
              teacherRole = dashboard.teacherRole,
              message = if (isAdminMode) {
                "Menu ${targetDestination.title} sudah masuk ke kerangka admin Android dan siap diisi bertahap."
              } else {
                "Menu ${targetDestination.title} sudah ditempatkan di sidebar sesuai struktur web dan siap dihubungkan ke halaman native berikutnya."
              }
            )
          }
          item {
            PlaceholderPanel(
              title = targetDestination.title,
              subtitle = if (isAdminMode) {
                "Konten native untuk menu admin ini bisa kita bangun pada tahap berikutnya."
              } else {
                "Struktur navigasi sudah sama dengan panel guru web. Konten halaman ini bisa kita isi pada tahap berikutnya."
              }
            ) {
              EmptyPlaceholderCard("Halaman ${targetDestination.title} sedang disiapkan sebagai screen native Android.")
            }
          }
        }
        }

        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = onRefreshClick,
              modifier = Modifier.weight(1f)
            ) {
              Text("Refresh Data")
            }
            OutlinedButton(
              onClick = onLogoutClick,
              modifier = Modifier.weight(1f)
            ) {
              Text("Logout")
            }
          }
        }

        item {
          Text(
            text = "Terakhir sync: ${formatter.format(Instant.ofEpochMilli(dashboard.lastSuccessfulSyncAt))}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 22.dp),
            fontWeight = FontWeight.Medium
          )
        }
      }
    }
    }

    AnimatedVisibility(
      visible = !(selectedDestination == GuruSidebarDestination.Mapel && isMapelDetailMode) &&
        !(selectedDestination == GuruSidebarDestination.Ujian && isUjianQuestionEditorMode) &&
        !(selectedDestination == GuruSidebarDestination.LaporanBulanan && isLaporanBulananDetailMode) &&
        !(selectedDestination == GuruSidebarDestination.LaporanUTS && isLaporanUtsDetailMode) &&
        !(selectedDestination == GuruSidebarDestination.Rapor && isRaporDetailMode),
      enter = fadeIn(animationSpec = tween(180)) + slideInVertically(animationSpec = tween(220)) { it / 2 },
      exit = fadeOut(animationSpec = tween(150)) + slideOutVertically(animationSpec = tween(200)) { it / 2 },
      modifier = Modifier.align(Alignment.BottomCenter)
    ) {
      BottomNavBar(
        items = bottomNavItems,
        selectedDestination = selectedBottomNavDestination,
        onSelect = onSelectDestination,
        onLongPress = { if (!isAdminMode) isShortcutDialogOpen = true },
        onItemPositioned = { destination, rect ->
          if (destination == GuruSidebarDestination.Profil) {
            updateUserGuideTarget(UserGuideTourTarget.ProfileShortcut, rect)
          }
        },
        modifier = Modifier
          .navigationBarsPadding()
          .padding(horizontal = 18.dp, vertical = 10.dp)
          .onGloballyPositioned {
            updateUserGuideTarget(UserGuideTourTarget.BottomNavigation, it.boundsInRoot())
          }
      )
    }

    if (isShortcutDialogOpen && !isAdminMode) {
      BottomNavShortcutDialog(
        availableItems = availableBottomNavItems,
        selectedDestinations = effectiveBottomNavShortcuts,
        onDismiss = { isShortcutDialogOpen = false },
        onSave = { destinations ->
          onUpdateBottomNavShortcuts(destinations)
          isShortcutDialogOpen = false
        }
      )
    }

    SidebarScrim(
      progress = sidebarProgress,
      onDismiss = onCloseSidebar
    )

    Sidebar(
      appName = "MIM App",
      appSubtitle = appRoleDashboardLabel(activeRole),
      content = if (isAdminMode) {
        buildAdminSidebarContent(canChangeRole = canChangeRole)
      } else {
        buildGuruSidebarContent(
          pendingSyncCount = dashboard.pendingSyncCount,
          activeMapelCount = dashboard.subjects.size,
          isWaliKelas = dashboard.waliSantriSnapshot.isWaliKelas,
          isWakasekKurikulum = dashboard.wakasekKurikulumSnapshot.isWakasekKurikulum,
          canChangeRole = canChangeRole
        )
      },
      selectedDestination = sidebarSelectedDestination,
      expandedParent = expandedSidebarParent,
      onDismiss = onCloseSidebar,
      onToggleParent = onToggleSidebarParent,
      onSelectItem = onSelectDestination,
      onChangeRole = onOpenRolePicker,
      onLogout = onLogoutClick,
      modifier = Modifier
        .align(Alignment.CenterStart)
        .offset { IntOffset(sidebarOffsetPx.value.roundToInt(), 0) }
    )

    if (isNotificationPopupOpen) {
      NotificationPopup(
        notifications = dashboard.notifications,
        onDismiss = onCloseNotificationPopup,
        onMarkAllAsRead = onMarkAllNotificationsAsRead,
        onNotificationClick = handleNotificationClick,
        modifier = Modifier.align(Alignment.BottomCenter)
      )
    }

    if (isLoadingApprovalRequest || selectedApprovalRequest != null) {
      AttendanceApprovalPopup(
        request = selectedApprovalRequest,
        isLoading = isLoadingApprovalRequest,
        isSaving = isReviewingApprovalRequest,
        onDismiss = {
          if (!isReviewingApprovalRequest) {
            selectedApprovalRequest = null
            isLoadingApprovalRequest = false
            loadingApprovalRequestId = ""
          }
        },
        onApprove = { reviewerNote ->
          val approvalId = selectedApprovalRequest?.id.orEmpty()
          if (approvalId.isBlank() || isReviewingApprovalRequest) return@AttendanceApprovalPopup
          scope.launch {
            isReviewingApprovalRequest = true
            val result = onReviewAttendanceApproval(approvalId, true, reviewerNote)
            isReviewingApprovalRequest = false
            if (result.success) {
              selectedApprovalRequest = null
              loadingApprovalRequestId = ""
            }
          }
        },
        onReject = { reviewerNote ->
          val approvalId = selectedApprovalRequest?.id.orEmpty()
          if (approvalId.isBlank() || isReviewingApprovalRequest) return@AttendanceApprovalPopup
          scope.launch {
            isReviewingApprovalRequest = true
            val result = onReviewAttendanceApproval(approvalId, false, reviewerNote)
            isReviewingApprovalRequest = false
            if (result.success) {
              selectedApprovalRequest = null
              loadingApprovalRequestId = ""
            }
          }
        },
        modifier = Modifier.align(Alignment.TopCenter)
      )
    }

    val activeTourDefinition = userGuideTourDefinition(activeInteractiveGuideKey)
    if (activeTourDefinition != null && !isInteractiveGuideAutoScrolling) {
      GuidedUserTourOverlay(
        steps = activeTourDefinition.steps,
        stepIndex = activeInteractiveGuideStepIndex,
        targets = userGuideTargetBounds,
        onPrevious = {
          activeInteractiveGuideStepIndex = (activeInteractiveGuideStepIndex - 1).coerceAtLeast(0)
        },
        onNext = {
          if (activeInteractiveGuideStepIndex >= activeTourDefinition.steps.lastIndex) {
            closeInteractiveGuide(returnToGuide = true)
          } else {
            activeInteractiveGuideStepIndex += 1
          }
        },
        onSkip = { closeInteractiveGuide(returnToGuide = true) },
        modifier = Modifier.fillMaxSize()
      )
    }
  }
}

@Composable
private fun GuidedUserTourOverlay(
  steps: List<UserGuideTourStep>,
  stepIndex: Int,
  targets: Map<UserGuideTourTarget, Rect>,
  onPrevious: () -> Unit,
  onNext: () -> Unit,
  onSkip: () -> Unit,
  modifier: Modifier = Modifier
) {
  val step = steps.getOrNull(stepIndex) ?: return
  val targetRect = resolveUserGuideTargetRect(step.target, targets)
  val interactionSource = remember { MutableInteractionSource() }
  val density = LocalDensity.current

  BoxWithConstraints(
    modifier = modifier
      .background(Color.Transparent)
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = {}
      )
  ) {
    val screenHeightPx = with(density) { maxHeight.toPx() }
    val targetCenterY = targetRect?.center?.y ?: (screenHeightPx * 0.25f)
    val popupAlignment = if (targetCenterY < screenHeightPx * 0.50f) {
      Alignment.BottomCenter
    } else {
      Alignment.TopCenter
    }

    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
      drawRect(Color(0xB3051020))
      targetRect?.let { rect ->
        val padding = 10.dp.toPx()
        val corner = 24.dp.toPx()
        val left = (rect.left - padding).coerceAtLeast(8.dp.toPx())
        val top = (rect.top - padding).coerceAtLeast(8.dp.toPx())
        val right = (rect.right + padding).coerceAtMost(size.width - 8.dp.toPx())
        val bottom = (rect.bottom + padding).coerceAtMost(size.height - 8.dp.toPx())
        val highlightSize = Size(right - left, bottom - top)
        drawRoundRect(
          color = Color.Transparent,
          topLeft = Offset(left, top),
          size = highlightSize,
          cornerRadius = CornerRadius(corner, corner),
          blendMode = BlendMode.Clear
        )
        drawRoundRect(
          color = Color.White.copy(alpha = 0.22f),
          topLeft = Offset(left, top),
          size = highlightSize,
          cornerRadius = CornerRadius(corner, corner),
          style = Stroke(width = 14.dp.toPx())
        )
        drawRoundRect(
          color = Color(0xFFF59E0B),
          topLeft = Offset(left, top),
          size = highlightSize,
          cornerRadius = CornerRadius(corner, corner),
          style = Stroke(width = 3.dp.toPx())
        )
      }
    }

    Column(
      modifier = Modifier
        .align(popupAlignment)
        .padding(horizontal = 18.dp, vertical = 24.dp)
        .fillMaxWidth()
        .shadow(18.dp, RoundedCornerShape(26.dp), ambientColor = Color(0x26000000), spotColor = Color(0x26000000))
        .background(CardBackground.copy(alpha = 0.98f), RoundedCornerShape(26.dp))
        .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(26.dp))
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "${stepIndex + 1}/${steps.size}",
          style = MaterialTheme.typography.labelLarge,
          color = PrimaryBlue,
          fontWeight = FontWeight.ExtraBold
        )
        TextButton(onClick = onSkip) {
          Text(t("Skip"))
        }
      }
      Text(
        text = t(step.title),
        style = MaterialTheme.typography.titleMedium,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold
      )
      Text(
        text = t(step.body),
        style = MaterialTheme.typography.bodyMedium,
        color = SubtleInk
      )
      if (targetRect == null) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(SoftPanel.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
            .border(1.dp, CardBorder.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
            .padding(12.dp)
        ) {
          Text(
            text = t("Menyiapkan sorotan. Jika target belum tampil, tekan Lanjut untuk melihat langkah berikutnya."),
            style = MaterialTheme.typography.bodySmall,
            color = SubtleInk
          )
        }
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedButton(
          onClick = onPrevious,
          enabled = stepIndex > 0,
          modifier = Modifier.weight(1f)
        ) {
          Text(t("Sebelumnya"))
        }
        Button(
          onClick = onNext,
          modifier = Modifier.weight(1f)
        ) {
          Text(t(if (stepIndex >= steps.lastIndex) "Selesai" else "Lanjut"))
        }
      }
    }
  }
}

@Composable
private fun InputSwipePager(
  selectedDestination: GuruSidebarDestination,
  onSelectDestination: (GuruSidebarDestination) -> Unit,
  subjects: List<SubjectOverview>,
  scoreSnapshots: List<MapelScoreSnapshot>,
  attendanceSnapshots: List<MapelAttendanceSnapshot>,
  patronMateriSnapshots: List<MapelPatronMateriSnapshot>,
  teachingScheduleEvents: List<CalendarEvent>,
  substituteTeacherOptions: List<TeacherOption>,
  sourceTeacherOptions: List<TeacherOption>,
  syncBanner: SyncBannerState,
  onMenuClick: () -> Unit,
  onRefresh: () -> Unit,
  onLoadScores: suspend (String, SubjectOverview) -> MapelScoreSnapshot?,
  onSaveScoresBatch: suspend (String, SubjectOverview, List<ScoreStudent>) -> ScoreSaveOutcome,
  onLoadAttendance: suspend (String, SubjectOverview) -> MapelAttendanceSnapshot?,
  onLoadPatronMateri: suspend (String, SubjectOverview) -> MapelPatronMateriSnapshot?,
  onSaveAttendanceBatch: suspend (String, SubjectOverview, Map<String, List<AttendanceHistoryEntry>>, String, String) -> AttendanceSaveOutcome,
  onSaveAttendance: suspend (String, SubjectOverview, String, List<AttendanceHistoryEntry>) -> AttendanceSaveOutcome,
  onSendSubstituteDelegation: suspend (String, SubjectOverview, String, List<String>, String, String) -> AttendanceSaveOutcome,
  onLoadSubstituteTeacherContext: suspend (TeacherOption, String) -> Pair<List<SubjectOverview>, List<CalendarEvent>>,
  onLoadDelegatedAttendanceContext: suspend (String) -> Pair<List<SubjectOverview>, List<CalendarEvent>>,
  inputAbsensiLaunchTarget: InputAbsensiNavigationTarget? = null,
  onInputAbsensiLaunchTargetConsumed: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .clipToBounds()
  ) {
    val widthPx = constraints.maxWidth.toFloat()
    val selectedIndex = inputPagerIndex(selectedDestination)
    val swipeThresholdPx = widthPx * 0.22f
    val pagerOffsetPx = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetPx by remember { mutableStateOf(0f) }
    val baseOffsetPx = -selectedIndex * widthPx
    val visualOffsetPx = if (isDragging) baseOffsetPx + dragOffsetPx else pagerOffsetPx.value

    LaunchedEffect(widthPx, selectedIndex) {
      if (widthPx > 0f && !isDragging) {
        pagerOffsetPx.animateTo(
          targetValue = -selectedIndex * widthPx,
          animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
        )
      }
    }

    Column(modifier = Modifier.fillMaxSize()) {
      InputPagerHeader(
        title = if (selectedIndex == 0) "Input Absensi" else "Input Nilai",
        activeInputPage = selectedIndex,
        onMenuClick = onMenuClick
      )

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .clipToBounds()
          .pointerInput(widthPx, selectedIndex) {
            if (widthPx <= 0f) return@pointerInput
            detectHorizontalDragGestures(
              onDragStart = {
                isDragging = true
                dragOffsetPx = pagerOffsetPx.value - (-selectedIndex * widthPx)
              },
              onDragCancel = {
                val currentOffset = (-selectedIndex * widthPx) + dragOffsetPx
                isDragging = false
                dragOffsetPx = 0f
                scope.launch {
                  pagerOffsetPx.snapTo(currentOffset)
                  pagerOffsetPx.animateTo(
                    targetValue = -selectedIndex * widthPx,
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                  )
                }
              },
              onDragEnd = {
                val nextIndex = when {
                  selectedIndex == 1 && dragOffsetPx > swipeThresholdPx -> 0
                  selectedIndex == 0 && dragOffsetPx < -swipeThresholdPx -> 1
                  else -> selectedIndex
                }
                val currentVisualOffset = (-selectedIndex * widthPx) + dragOffsetPx
                val targetOffset = -nextIndex * widthPx
                isDragging = false
                dragOffsetPx = 0f
                scope.launch {
                  pagerOffsetPx.snapTo(currentVisualOffset)
                  pagerOffsetPx.animateTo(
                    targetValue = targetOffset,
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                  )
                  if (nextIndex != selectedIndex) {
                    onSelectDestination(inputPagerDestination(nextIndex))
                  }
                }
              },
              onHorizontalDrag = { change, dragAmount ->
                change.consume()
                val nextOffset = dragOffsetPx + dragAmount
                dragOffsetPx = if (selectedIndex == 0) {
                  nextOffset.coerceIn(-widthPx, 0f)
                } else {
                  nextOffset.coerceIn(0f, widthPx)
                }
              }
            )
          }
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(visualOffsetPx.roundToInt(), 0) }
        ) {
          Box(
            modifier = Modifier.fillMaxSize()
          ) {
            InputAbsensiScreen(
              subjects = subjects,
              attendanceSnapshots = attendanceSnapshots,
              patronMateriSnapshots = patronMateriSnapshots,
              teachingScheduleEvents = teachingScheduleEvents,
              substituteTeacherOptions = substituteTeacherOptions,
              sourceTeacherOptions = sourceTeacherOptions,
              syncBanner = syncBanner,
              onMenuClick = onMenuClick,
              onRefresh = onRefresh,
              onLoadAttendance = onLoadAttendance,
              onLoadPatronMateri = onLoadPatronMateri,
              onSaveAttendanceBatch = onSaveAttendanceBatch,
              onSaveAttendance = onSaveAttendance,
              onSendSubstituteDelegation = onSendSubstituteDelegation,
              onLoadSubstituteTeacherContext = onLoadSubstituteTeacherContext,
              onLoadDelegatedAttendanceContext = onLoadDelegatedAttendanceContext,
              launchTarget = inputAbsensiLaunchTarget,
              onLaunchTargetConsumed = onInputAbsensiLaunchTargetConsumed,
              activeInputPage = 0,
              showTopBar = false,
              modifier = Modifier.fillMaxSize()
            )
          }

          Box(
            modifier = Modifier
              .fillMaxSize()
              .offset { IntOffset(widthPx.roundToInt(), 0) }
          ) {
            InputNilaiScreen(
              subjects = subjects,
              scoreSnapshots = scoreSnapshots,
              patronMateriSnapshots = patronMateriSnapshots,
              syncBanner = syncBanner,
              onMenuClick = onMenuClick,
              onRefresh = onRefresh,
              onLoadScores = onLoadScores,
              onLoadPatronMateri = onLoadPatronMateri,
              onSaveScoresBatch = onSaveScoresBatch,
              activeInputPage = 1,
              showTopBar = false,
              modifier = Modifier.fillMaxSize()
            )
          }
        }
      }
    }
  }
}

private fun inputPagerIndex(destination: GuruSidebarDestination): Int {
  return if (destination == GuruSidebarDestination.InputNilai) 1 else 0
}

private fun inputPagerDestination(index: Int): GuruSidebarDestination {
  return if (index == 0) GuruSidebarDestination.InputAbsensi else GuruSidebarDestination.InputNilai
}

@Composable
private fun InputPagerHeader(
  title: String,
  activeInputPage: Int,
  onMenuClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(top = 6.dp, start = 18.dp, end = 18.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Box(
      modifier = Modifier
        .size(42.dp)
        .background(Color.White.copy(alpha = 0.86f), CircleShape)
        .border(1.dp, CardBorder, CircleShape)
        .clickable(onClick = onMenuClick),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Outlined.Menu,
        contentDescription = t("Buka sidebar"),
        tint = PrimaryBlueDark
      )
    }

    Box(
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 10.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
      ) {
        Text(
          text = t(title),
          style = MaterialTheme.typography.titleLarge,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Row(
          horizontalArrangement = Arrangement.spacedBy(5.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          repeat(2) { index ->
            val active = index == activeInputPage
            Box(
              modifier = Modifier
                .size(if (active) 7.dp else 5.dp)
                .background(if (active) HighlightCard else CardBorder.copy(alpha = 0.9f), CircleShape)
            )
          }
        }
      }
    }

    Box(modifier = Modifier.size(42.dp))
  }
}

@Composable
private fun GuruHomeTopBar(
  title: String,
  onMenuClick: () -> Unit,
  subtitle: String = "Drawer ini mengikuti struktur menu guru versi web",
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .shadow(12.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x160F172A), spotColor = Color(0x160F172A))
      .background(Color.White, RoundedCornerShape(20.dp))
      .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .background(Color(0xFFF8FAFC), RoundedCornerShape(14.dp))
        .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
        .clickable(onClick = onMenuClick)
        .padding(10.dp)
    ) {
      Icon(
        imageVector = Icons.Outlined.Menu,
        contentDescription = t("Buka sidebar"),
        tint = PrimaryBlueDark
      )
    }
    Column(modifier = Modifier.padding(start = 14.dp)) {
      Text(
        text = t(title),
        style = MaterialTheme.typography.titleSmall,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold
      )
      Text(
        text = t(subtitle),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun GuruHomeScreenPreview() {
  MimGuruTheme {
    GuruHomeScreen(
      dashboard = SampleDataFactory.createDashboard("Ustadz Fulan"),
      activeRole = "guru",
      availableRoles = listOf("guru", "admin"),
      syncBanner = SyncBannerState("Data lokal siap. Sinkronisasi akan berjalan otomatis di background.", false),
      selectedDestination = GuruSidebarDestination.Mapel,
      isCalendarScreenOpen = false,
      isNotificationPopupOpen = false,
      selectedCalendarDateIso = LocalDate.now().toString(),
      teachingReminderSettings = TeachingReminderSettings(),
      pendingInputAbsensiTarget = null,
      isSidebarOpen = true,
      expandedSidebarParent = GuruSidebarParent.AktivitasHarian,
      bottomNavShortcutDestinations = com.mim.guruapp.defaultBottomNavShortcutDestinations(),
      languageCode = "id",
      themeModeCode = "system",
      isClaimSectionVisible = true,
      selectedClaimSubjectIds = setOf("offer-1"),
      onToggleSidebar = {},
      onCloseSidebar = {},
      onToggleSidebarParent = {},
      onSelectDestination = {},
      onOpenRolePicker = {},
      onUpdateBottomNavShortcuts = {},
      onOpenCalendarScreen = {},
      onCloseCalendarScreen = {},
      onOpenNotificationPopup = {},
      onCloseNotificationPopup = {},
      onMarkNotificationAsRead = {},
      onMarkAllNotificationsAsRead = {},
      onUpdateTeachingReminderSettings = {},
      onOpenInputAbsensiTarget = {},
      onConsumePendingInputAbsensiTarget = {},
      onLoadAttendanceApprovalRequest = { null },
      onReviewAttendanceApproval = { _, _, _ -> AttendanceSaveOutcome(true, "OK") },
      onSelectCalendarDate = {},
      onToggleClaimSection = {},
      onToggleClaimSubject = {},
      onClearClaimSelection = {},
      onClaimSelectedSubjects = {},
      onLoadMapelAttendance = { _, _ -> null },
      onSaveMapelAttendance = { _, _, _, _ -> AttendanceSaveOutcome(true, "OK") },
      onSaveMapelAttendanceBatch = { _, _, _, _, _ -> AttendanceSaveOutcome(true, "OK") },
      onDeleteMapelAttendance = { _, _, _ -> AttendanceSaveOutcome(true, "OK") },
      onSendMapelAttendanceDelegation = { _, _, _, _, _, _ -> AttendanceSaveOutcome(true, "OK") },
      onLoadSubstituteTeacherContext = { _, _ -> emptyList<SubjectOverview>() to emptyList() },
      onLoadDelegatedAttendanceContext = { emptyList<SubjectOverview>() to emptyList() },
      onLoadMapelScores = { _, _ -> null },
      onSaveMapelScores = { _, _, _ -> ScoreSaveOutcome(true, "OK") },
      onSaveMapelScoresBatch = { _, _, _ -> ScoreSaveOutcome(true, "OK") },
      onLoadMapelPatronMateri = { _, _ -> null },
      onSaveMapelPatronMateri = { _, _, _ -> PatronMateriSaveOutcome(true, "OK") },
      onLoadMapelQuestions = { _, _ -> null },
      onSaveMapelQuestions = { _, _, _ -> QuestionSaveOutcome(true, "OK") },
      onLoadMapelRaporDescriptions = { _, _ -> null },
      onSaveMapelRaporDescriptions = { _, _, _ -> QuestionSaveOutcome(true, "OK") },
      onLoadExamQuestions = { GuruExamQuestionSnapshot(emptyList()) },
      onSaveExamQuestions = { _, _ -> QuestionSaveOutcome(true, "OK") },
      onLoadAiTokenBalance = { null },
      onGenerateAiContent = { GuruAiGenerateResult.Error("AI tidak tersedia di preview.") },
      onLoadAdminEmployees = { AdminEmployeeListResult.Success(emptyList()) },
      onSaveAdminEmployee = { employee, _ -> AdminEmployeeSaveResult.Success(employee) },
      onLoadAdminSchoolProfile = {
        AdminSchoolProfileLoadResult.Success(
          AdminSchoolProfileSnapshot(
            profile = AdminSchoolProfile(),
            employeeOptions = emptyList(),
            waliKelasItems = emptyList(),
            musyrifItems = emptyList(),
            muhaffizItems = emptyList(),
            statusMessage = ""
          )
        )
      },
      onSaveAdminSchoolProfile = { profile ->
        AdminSchoolProfileSaveResult.Success(
          snapshot = AdminSchoolProfileSnapshot(
            profile = profile,
            employeeOptions = emptyList(),
            waliKelasItems = emptyList(),
            musyrifItems = emptyList(),
            muhaffizItems = emptyList(),
            statusMessage = ""
          ),
          message = "OK"
        )
      },
      onLoadAdminSantri = { AdminSantriLoadResult.Success(AdminSantriSnapshot(emptyList())) },
      onSaveAdminSantri = { student -> AdminSantriSaveResult.Success(student, "OK") },
      onPromoteAdminSantri = { student -> AdminSantriSaveResult.Success(student, "OK") },
      onGraduateAdminSantri = { student -> AdminSantriSaveResult.Success(student, "OK") },
      onSaveProfile = { ProfileSaveOutcome(true, "OK") },
      onSaveSantri = { SantriSaveOutcome(true, "OK") },
      onSaveMonthlyReport = { MonthlyReportSaveOutcome(true, "OK") },
      onSaveMonthlyExtracurricularReports = { MonthlyExtracurricularSaveOutcome(true, "OK") },
      onLoadMonthlyAttendanceSummaries = { emptyList() },
      onLoadMonthlyAttendanceDetail = { _, _ -> null },
      onSaveUtsReportOverride = { UtsReportSaveOutcome(true, "OK") },
      onLoadMutabaah = { null },
      onSaveMutabaahStatus = { _, _, _ -> MutabaahSaveOutcome(true, "OK") },
      onSaveMutabaahStatuses = { _, _, _ -> MutabaahSaveOutcome(true, "OK") },
      onLoadLeaveRequests = { null },
      onSubmitLeaveRequest = { _, _, _ -> LeaveRequestSaveOutcome(true, "OK") },
      onDeleteLeaveRequest = { _ -> LeaveRequestSaveOutcome(true, "OK") },
      onReviewWakasekLeaveRequest = { _, _, _ -> WakasekReviewOutcome(true, "OK") },
      onApplyLanguage = {},
      onApplyThemeMode = {},
      onRefreshClick = {},
      onLogoutClick = {}
    )
  }
}
