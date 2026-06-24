package com.mim.guruapp

const val APP_ROLE_ADMIN = "admin"
const val APP_ROLE_GURU = "guru"

fun normalizeRoleToken(value: String): String {
  return value.trim().lowercase()
    .replace("_", " ")
    .replace("-", " ")
    .replace(Regex("\\s+"), " ")
}

fun isAdminRoleToken(value: String): Boolean {
  return normalizeRoleToken(value) == APP_ROLE_ADMIN
}

fun isGuruRoleToken(value: String): Boolean {
  return normalizeRoleToken(value) == APP_ROLE_GURU
}

fun isWakasekKurikulumRoleToken(value: String): Boolean {
  val clean = normalizeRoleToken(value)
  val compact = clean.replace(" ", "")
  return compact == "wakasekakademik" ||
    compact == "wakasekbidangakademik" ||
    compact == "wakasekkurikulum" ||
    compact == "wakasekbidangkurikulum" ||
    (clean.contains("wakasek") && (clean.contains("akademik") || clean.contains("kurikulum")))
}

fun availableAppRoles(roles: List<String>): List<String> {
  return buildList {
    if (roles.any(::isAdminRoleToken)) add(APP_ROLE_ADMIN)
    if (roles.any { isGuruRoleToken(it) || isWakasekKurikulumRoleToken(it) }) add(APP_ROLE_GURU)
  }
}

fun appRoleLabel(role: String): String {
  return when (normalizeRoleToken(role)) {
    APP_ROLE_ADMIN -> "Admin"
    APP_ROLE_GURU -> "Guru"
    else -> role.ifBlank { "Guru" }
  }
}

fun appRoleDashboardLabel(role: String): String {
  return when (normalizeRoleToken(role)) {
    APP_ROLE_ADMIN -> "Admin Dashboard"
    APP_ROLE_GURU -> "Guru Dashboard"
    else -> "Guru Dashboard"
  }
}
