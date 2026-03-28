use base64::Engine;
use std::io::{Read, Write};
use std::path::PathBuf;
use tauri::Manager;
use tauri_plugin_notification::NotificationExt;

#[cfg(all(
  not(debug_assertions),
  any(target_os = "windows", target_os = "macos", target_os = "linux")
))]
use std::sync::{Arc, Mutex};
#[cfg(all(
  not(debug_assertions),
  any(target_os = "windows", target_os = "macos", target_os = "linux")
))]
use tauri_plugin_updater::UpdaterExt;

#[cfg(all(
  not(debug_assertions),
  any(target_os = "windows", target_os = "macos", target_os = "linux")
))]
fn emit_updater_status(
  app_handle: &tauri::AppHandle,
  stage: &str,
  message: &str,
  progress: Option<f64>,
  current_version: Option<&str>,
  latest_version: Option<&str>,
  notes: Option<&str>,
) {
  let payload = serde_json::json!({
    "stage": stage,
    "message": message,
    "progress": progress,
    "currentVersion": current_version,
    "latestVersion": latest_version,
    "notes": notes
  })
  .to_string();
  let script = format!(
    "window.dispatchEvent(new CustomEvent('desktop-updater-status', {{ detail: {} }}));",
    payload
  );
  if let Some(window) = app_handle.get_webview_window("main") {
    let _ = window.eval(&script);
  }
}

#[cfg(target_os = "android")]
fn run_android_am(args: &[&str]) -> bool {
  let bins = ["am", "/system/bin/am"];
  for bin in bins {
    match std::process::Command::new(bin).args(args).status() {
      Ok(status) if status.success() => return true,
      _ => {}
    }
  }
  false
}

#[cfg(target_os = "android")]
fn run_android_monkey(args: &[&str]) -> bool {
  let bins = ["monkey", "/system/bin/monkey"];
  for bin in bins {
    match std::process::Command::new(bin).args(args).status() {
      Ok(status) if status.success() => return true,
      _ => {}
    }
  }
  false
}

#[cfg(target_os = "android")]
fn guess_mime_from_path(path: &str) -> &'static str {
  let lower = path.to_lowercase();
  if lower.ends_with(".apk") {
    "application/vnd.android.package-archive"
  } else if lower.ends_with(".pdf") {
    "application/pdf"
  } else if lower.ends_with(".png") {
    "image/png"
  } else if lower.ends_with(".jpg") || lower.ends_with(".jpeg") {
    "image/jpeg"
  } else {
    "*/*"
  }
}

#[cfg(target_os = "android")]
fn download_notification_id(seed: &str) -> i32 {
  use std::collections::hash_map::DefaultHasher;
  use std::hash::{Hash, Hasher};
  let mut hasher = DefaultHasher::new();
  seed.hash(&mut hasher);
  let value = hasher.finish();
  30_000 + (value % 9_000) as i32
}

#[cfg(target_os = "android")]
fn broadcast_download_notification(
  stage: &str,
  id: i32,
  title: &str,
  progress: Option<u32>,
  path: Option<&str>,
  mime: Option<&str>,
  error: Option<&str>,
) {
  let mut args = vec![
    "broadcast".to_string(),
    "-a".to_string(),
    "com.mim.app.DOWNLOAD_STATUS".to_string(),
    "--es".to_string(),
    "stage".to_string(),
    stage.to_string(),
    "--ei".to_string(),
    "id".to_string(),
    id.to_string(),
    "--es".to_string(),
    "title".to_string(),
    title.to_string(),
  ];
  if let Some(p) = progress {
    args.push("--ei".to_string());
    args.push("progress".to_string());
    args.push(p.to_string());
  }
  if let Some(p) = path {
    args.push("--es".to_string());
    args.push("path".to_string());
    args.push(p.to_string());
  }
  if let Some(m) = mime {
    args.push("--es".to_string());
    args.push("mime".to_string());
    args.push(m.to_string());
  }
  if let Some(e) = error {
    args.push("--es".to_string());
    args.push("error".to_string());
    args.push(e.to_string());
  }
  let refs = args.iter().map(String::as_str).collect::<Vec<_>>();
  let _ = run_android_am(&refs);
}

#[tauri::command]
fn open_external_url(url: String) -> Result<bool, String> {
  let target = url.trim();
  if target.is_empty() {
    return Err("URL kosong.".to_string());
  }
  #[cfg(target_os = "android")]
  {
    let try_start = |args: &[&str]| -> bool { run_android_am(args) };

    let is_whatsapp_target = target.contains("wa.me/")
      || target.contains("api.whatsapp.com/")
      || target.starts_with("whatsapp://");

    if is_whatsapp_target {
      if try_start(&[
        "start",
        "--activity-new-task",
        "-a",
        "android.intent.action.VIEW",
        "-c",
        "android.intent.category.BROWSABLE",
        "-d",
        target,
        "-p",
        "com.whatsapp",
      ]) {
        return Ok(true);
      }

      if target.starts_with("whatsapp://send?") {
        let query = target.trim_start_matches("whatsapp://send?");
        let https_fallback = format!("https://api.whatsapp.com/send?{query}");
        if try_start(&[
          "start",
          "--activity-new-task",
          "-a",
          "android.intent.action.VIEW",
          "-c",
          "android.intent.category.BROWSABLE",
          "-d",
          &https_fallback,
          "-p",
          "com.whatsapp",
        ]) {
          return Ok(true);
        }
      }

      // Intent URL fallback (bypasses custom scheme errors on some Android builds).
      let intent_url = if target.starts_with("whatsapp://send?") {
        let query = target.trim_start_matches("whatsapp://send?");
        format!(
          "intent://api.whatsapp.com/send?{}#Intent;scheme=https;package=com.whatsapp;action=android.intent.action.VIEW;end",
          query
        )
      } else if target.starts_with("https://") || target.starts_with("http://") {
        let without_scheme = target
          .trim_start_matches("https://")
          .trim_start_matches("http://");
        let scheme = if target.starts_with("https://") { "https" } else { "http" };
        format!(
          "intent://{}#Intent;scheme={};package=com.whatsapp;action=android.intent.action.VIEW;end",
          without_scheme, scheme
        )
      } else {
        String::new()
      };
      if !intent_url.is_empty()
        && try_start(&[
          "start",
          "--activity-new-task",
          "-a",
          "android.intent.action.VIEW",
          "-d",
          &intent_url,
        ])
      {
        return Ok(true);
      }

      // Generic whatsapp URL without forcing package (lets Android resolve handler).
      if try_start(&[
        "start",
        "--activity-new-task",
        "-a",
        "android.intent.action.VIEW",
        "-c",
        "android.intent.category.BROWSABLE",
        "-d",
        target,
      ]) {
        return Ok(true);
      }
    }

    if try_start(&[
      "start",
      "--activity-new-task",
      "-a",
      "android.intent.action.VIEW",
      "-c",
      "android.intent.category.BROWSABLE",
      "-d",
      target,
    ]) {
      return Ok(true);
    }
    Ok(false)
  }
  #[cfg(target_os = "windows")]
  {
    let escaped = target.replace('\'', "''");
    std::process::Command::new("powershell")
      .args([
        "-NoProfile",
        "-Command",
        &format!("Start-Process -FilePath '{}'", escaped),
      ])
      .spawn()
      .map(|_| true)
      .map_err(|e| format!("Gagal membuka URL eksternal: {e}"))
  }
  #[cfg(target_os = "macos")]
  {
    std::process::Command::new("open")
      .arg(target)
      .spawn()
      .map(|_| true)
      .map_err(|e| format!("Gagal membuka URL eksternal: {e}"))
  }
  #[cfg(all(unix, not(target_os = "macos"), not(target_os = "android")))]
  {
    std::process::Command::new("xdg-open")
      .arg(target)
      .spawn()
      .map(|_| true)
      .map_err(|e| format!("Gagal membuka URL eksternal: {e}"))
  }
}

#[tauri::command]
fn open_whatsapp_message(phone: String, message: String) -> Result<bool, String> {
  #[cfg(target_os = "android")]
  {
    let normalized_phone = phone
      .chars()
      .filter(|c| c.is_ascii_digit())
      .collect::<String>();
    if normalized_phone.is_empty() {
      return Ok(false);
    }

    let plain_message = message.trim();
    let encoded_message = urlencoding::encode(plain_message).to_string();
    let try_start = |args: &[&str]| -> bool { run_android_am(args) };

    // Prefer ACTION_SEND to avoid long URL issues. User will pick the contact.
    let jid = format!("{}@s.whatsapp.net", normalized_phone);
    if try_start(&[
      "start",
      "--activity-new-task",
      "-a",
      "android.intent.action.SEND",
      "-t",
      "text/plain",
      "--es",
      "android.intent.extra.TEXT",
      plain_message,
      "--es",
      "jid",
      &jid,
      "-p",
      "com.whatsapp",
    ]) {
      return Ok(true);
    }

    // Fallback: open WhatsApp share sheet without text to avoid long argument limits.
    if try_start(&[
      "start",
      "--activity-new-task",
      "-a",
      "android.intent.action.SEND",
      "-t",
      "text/plain",
      "-p",
      "com.whatsapp",
    ]) {
      return Ok(true);
    }

    if try_start(&[
      "start",
      "--activity-new-task",
      "-a",
      "android.intent.action.SEND",
      "-t",
      "text/plain",
    ]) {
      return Ok(true);
    }
    // Last fallback: open WhatsApp app directly so user can paste message.
    if try_start(&[
      "start",
      "--activity-new-task",
      "-a",
      "android.intent.action.MAIN",
      "-c",
      "android.intent.category.LAUNCHER",
      "-p",
      "com.whatsapp",
    ]) {
      return Ok(true);
    }

    if run_android_monkey(&[
      "-p",
      "com.whatsapp",
      "-c",
      "android.intent.category.LAUNCHER",
      "1",
    ]) {
      return Ok(true);
    }
    return Ok(false);
  }

  #[cfg(not(target_os = "android"))]
  {
    let _ = phone;
    let _ = message;
    Ok(false)
  }
}

#[tauri::command]
fn show_local_notification(app: tauri::AppHandle, title: String, body: String) -> Result<bool, String> {
  let title = title.trim();
  let body = body.trim();
  if title.is_empty() && body.is_empty() {
    return Ok(false);
  }
  let mut builder = app.notification().builder();
  if !title.is_empty() {
    builder = builder.title(title);
  }
  if !body.is_empty() {
    builder = builder.body(body);
  }
  builder
    .show()
    .map(|_| true)
    .map_err(|e| format!("Gagal menampilkan notifikasi: {e}"))
}

fn get_desktop_export_dir() -> Result<PathBuf, String> {
  let user = std::env::var("USERPROFILE").map_err(|e| format!("USERPROFILE tidak tersedia: {e}"))?;
  let dir = PathBuf::from(user).join("Documents").join("MIM App").join("Cetak");
  std::fs::create_dir_all(&dir).map_err(|e| format!("Gagal membuat folder export: {e}"))?;
  Ok(dir)
}

fn sanitize_file_name(name: &str, fallback: &str) -> String {
  let mut cleaned = name
    .chars()
    .map(|c| match c {
      '<' | '>' | ':' | '"' | '/' | '\\' | '|' | '?' | '*' => '_',
      _ => c,
    })
    .collect::<String>();
  if cleaned.trim().is_empty() {
    cleaned = fallback.to_string();
  }
  cleaned
}

fn resolve_mobile_download_dir(app: &tauri::AppHandle) -> Result<(PathBuf, bool), String> {
  if let Ok(public_dir) = app.path().download_dir() {
    if std::fs::create_dir_all(&public_dir).is_ok() {
      return Ok((public_dir, true));
    }
  }
  let app_dir = app
    .path()
    .app_data_dir()
    .map_err(|e| format!("Gagal resolve app data dir: {e}"))?;
  let private_dir = app_dir.join("Downloads");
  std::fs::create_dir_all(&private_dir)
    .map_err(|e| format!("Gagal membuat folder Downloads aplikasi: {e}"))?;
  Ok((private_dir, false))
}

#[tauri::command]
fn download_url_to_app_storage(
  app: tauri::AppHandle,
  url: String,
  file_name: String,
) -> Result<String, String> {
  let target = url.trim();
  if target.is_empty() {
    return Err("URL kosong.".to_string());
  }

  let parsed = reqwest::Url::parse(target).map_err(|e| format!("URL tidak valid: {e}"))?;
  let fallback_name = parsed
    .path_segments()
    .and_then(|mut seg| seg.next_back())
    .filter(|name| !name.trim().is_empty())
    .unwrap_or("download.bin")
    .to_string();

  let requested_name = file_name.trim();
  let final_name_raw = if requested_name.is_empty() {
    fallback_name
  } else {
    requested_name.to_string()
  };
  let final_name = sanitize_file_name(&final_name_raw, "download.bin");
  let (download_dir, _is_public) = resolve_mobile_download_dir(&app)?;

  let output_path = download_dir.join(final_name);
  #[cfg(target_os = "android")]
  let notif_id = download_notification_id(&output_path.to_string_lossy());
  #[cfg(target_os = "android")]
  broadcast_download_notification("progress", notif_id, "Mengunduh file", Some(0), None, None, None);

  let mut response = reqwest::blocking::Client::new()
    .get(parsed)
    .send()
    .map_err(|e| {
      #[cfg(target_os = "android")]
      broadcast_download_notification(
        "error",
        notif_id,
        "Unduhan gagal",
        None,
        None,
        None,
        Some("Gagal memulai unduhan."),
      );
      format!("Gagal mengunduh file: {e}")
    })?;
  if !response.status().is_success() {
    #[cfg(target_os = "android")]
    broadcast_download_notification(
      "error",
      notif_id,
      "Unduhan gagal",
      None,
      None,
      None,
      Some(&format!("HTTP {}", response.status())),
    );
    return Err(format!("Unduhan gagal. HTTP {}", response.status()));
  }

  let total_size = response.content_length().unwrap_or(0);
  let mut file = std::fs::File::create(&output_path).map_err(|e| {
    #[cfg(target_os = "android")]
    broadcast_download_notification(
      "error",
      notif_id,
      "Unduhan gagal",
      None,
      None,
      None,
      Some("Gagal membuat file unduhan."),
    );
    format!("Gagal menyiapkan file unduhan: {e}")
  })?;

  let mut downloaded: u64 = 0;
  let mut last_progress: u32 = 0;
  let mut buffer = [0_u8; 64 * 1024];
  loop {
    let read = response.read(&mut buffer).map_err(|e| {
      #[cfg(target_os = "android")]
      broadcast_download_notification(
        "error",
        notif_id,
        "Unduhan gagal",
        None,
        None,
        None,
        Some("Gagal membaca data unduhan."),
      );
      format!("Gagal membaca konten unduhan: {e}")
    })?;
    if read == 0 {
      break;
    }
    file.write_all(&buffer[..read]).map_err(|e| {
      #[cfg(target_os = "android")]
      broadcast_download_notification(
        "error",
        notif_id,
        "Unduhan gagal",
        None,
        None,
        None,
        Some("Gagal menulis file unduhan."),
      );
      format!("Gagal menyimpan file unduhan: {e}")
    })?;
    downloaded += read as u64;

    #[cfg(target_os = "android")]
    if total_size > 0 {
      let progress = ((downloaded.saturating_mul(100)) / total_size).min(100) as u32;
      if progress >= last_progress.saturating_add(5) || progress == 100 {
        last_progress = progress;
        broadcast_download_notification(
          "progress",
          notif_id,
          "Mengunduh file",
          Some(progress),
          None,
          None,
          None,
        );
      }
    }
  }
  file.flush()
    .map_err(|e| format!("Gagal menyelesaikan file unduhan: {e}"))?;

  let saved_path = output_path.to_string_lossy().to_string();
  #[cfg(target_os = "android")]
  {
    let mime = guess_mime_from_path(&saved_path);
    broadcast_download_notification(
      "complete",
      notif_id,
      "Unduhan selesai",
      Some(100),
      Some(&saved_path),
      Some(mime),
      None,
    );
  }
  Ok(saved_path)
}

#[tauri::command]
fn save_base64_to_downloads(
  app: tauri::AppHandle,
  file_name: String,
  base64_data: String,
) -> Result<String, String> {
  let clean_name = sanitize_file_name(file_name.trim(), "Dokumen.pdf");
  let final_name = if clean_name.to_lowercase().ends_with(".pdf") {
    clean_name
  } else {
    format!("{clean_name}.pdf")
  };
  let (download_dir, _is_public) = resolve_mobile_download_dir(&app)?;
  let output_path = download_dir.join(final_name);

  #[cfg(target_os = "android")]
  let notif_id = download_notification_id(&output_path.to_string_lossy());
  #[cfg(target_os = "android")]
  broadcast_download_notification("progress", notif_id, "Menyimpan file", Some(10), None, None, None);

  let bytes = base64::engine::general_purpose::STANDARD
    .decode(base64_data.trim())
    .map_err(|e| format!("Gagal decode base64: {e}"))?;
  std::fs::write(&output_path, bytes).map_err(|e| {
    #[cfg(target_os = "android")]
    broadcast_download_notification(
      "error",
      notif_id,
      "Gagal menyimpan file",
      None,
      None,
      None,
      Some("Tidak bisa menulis file hasil unduhan."),
    );
    format!("Gagal menyimpan file: {e}")
  })?;
  let saved_path = output_path.to_string_lossy().to_string();
  #[cfg(target_os = "android")]
  {
    let mime = guess_mime_from_path(&saved_path);
    broadcast_download_notification(
      "complete",
      notif_id,
      "File tersimpan",
      Some(100),
      Some(&saved_path),
      Some(mime),
      None,
    );
  }
  Ok(saved_path)
}

#[tauri::command]
fn save_pdf_base64(file_name: String, base64_data: String) -> Result<String, String> {
  let clean_name = file_name
    .chars()
    .map(|c| match c {
      '<' | '>' | ':' | '"' | '/' | '\\' | '|' | '?' | '*' => '_',
      _ => c,
    })
    .collect::<String>();
  let final_name = if clean_name.to_lowercase().ends_with(".pdf") {
    clean_name
  } else {
    format!("{clean_name}.pdf")
  };

  let dir = get_desktop_export_dir()?;
  let path = dir.join(final_name);
  let bytes = base64::engine::general_purpose::STANDARD
    .decode(base64_data.trim())
    .map_err(|e| format!("Gagal decode PDF base64: {e}"))?;
  std::fs::write(&path, bytes).map_err(|e| format!("Gagal menyimpan PDF: {e}"))?;
  Ok(path.to_string_lossy().to_string())
}

#[tauri::command]
fn open_file_path(path: String) -> Result<(), String> {
  let target = path.trim();
  if target.is_empty() {
    return Err("Path file kosong.".to_string());
  }

  #[cfg(target_os = "android")]
  {
    let strip_file_scheme = |value: &str| -> String {
      value
        .strip_prefix("file://")
        .map(|v| v.to_string())
        .unwrap_or_else(|| value.to_string())
    };
    let guess_mime = |file_path: &str| -> &'static str {
      let p = file_path.to_lowercase();
      if p.ends_with(".pdf") {
        "application/pdf"
      } else if p.ends_with(".apk") {
        "application/vnd.android.package-archive"
      } else if p.ends_with(".png") {
        "image/png"
      } else if p.ends_with(".jpg") || p.ends_with(".jpeg") {
        "image/jpeg"
      } else {
        "*/*"
      }
    };
    let to_content_uri = |file_path: &str| -> String {
      let rel = file_path.trim_start_matches('/').replace('\\', "/");
      format!("content://com.mim.app.fileprovider/root/{rel}")
    };
    let try_start = |args: &[&str]| -> bool {
      std::process::Command::new("am").args(args).spawn().is_ok()
    };

    let is_apk = target.to_lowercase().ends_with(".apk");
    if is_apk {
      let file_path = strip_file_scheme(target);
      let file_uri = if target.starts_with("file://") || target.starts_with("content://") {
        target.to_string()
      } else {
        format!("file://{target}")
      };
      let content_uri = if target.starts_with("content://") {
        target.to_string()
      } else {
        to_content_uri(&file_path)
      };

      if std::process::Command::new("am")
        .args([
          "start",
          "-a",
          "android.intent.action.INSTALL_PACKAGE",
          "-d",
          &content_uri,
          "--grant-read-uri-permission",
          "--activity-new-task",
        ])
        .spawn()
        .is_ok()
      {
        return Ok(());
      }
      if std::process::Command::new("am")
        .args([
          "start",
          "-a",
          "android.intent.action.INSTALL_PACKAGE",
          "-d",
          &file_uri,
          "--grant-read-uri-permission",
          "--activity-new-task",
        ])
        .spawn()
        .is_ok()
      {
        return Ok(());
      }
      if std::process::Command::new("am")
        .args([
          "start",
          "-a",
          "android.intent.action.VIEW",
          "-d",
          &content_uri,
          "-t",
          "application/vnd.android.package-archive",
          "--grant-read-uri-permission",
          "--activity-new-task",
        ])
        .spawn()
        .is_ok()
      {
        return Ok(());
      }
      if std::process::Command::new("am")
        .args([
          "start",
          "-a",
          "android.intent.action.VIEW",
          "-d",
          &file_uri,
          "-t",
          "application/vnd.android.package-archive",
          "--grant-read-uri-permission",
          "--activity-new-task",
        ])
        .spawn()
        .is_ok()
      {
        return Ok(());
      }
      return Err("Gagal membuka installer APK otomatis.".to_string());
    }
    let file_path = strip_file_scheme(target);
    let file_uri = if target.starts_with("file://") || target.starts_with("content://") {
      target.to_string()
    } else {
      format!("file://{target}")
    };
    let content_uri = if target.starts_with("content://") {
      target.to_string()
    } else {
      to_content_uri(&file_path)
    };
    let mime = guess_mime(&file_path);

    if try_start(&[
      "start",
      "--activity-new-task",
      "-a",
      "android.intent.action.VIEW",
      "-d",
      &content_uri,
      "-t",
      mime,
      "--grant-read-uri-permission",
    ]) {
      return Ok(());
    }
    if try_start(&[
      "start",
      "--activity-new-task",
      "-a",
      "android.intent.action.VIEW",
      "-d",
      &file_uri,
      "-t",
      mime,
      "--grant-read-uri-permission",
    ]) {
      return Ok(());
    }
    if try_start(&[
      "start",
      "--activity-new-task",
      "-a",
      "android.intent.action.VIEW",
      "-d",
      &content_uri,
    ]) {
      return Ok(());
    }
    if try_start(&[
      "start",
      "--activity-new-task",
      "-a",
      "android.intent.action.VIEW",
      "-d",
      &file_uri,
    ]) {
      return Ok(());
    }
    return Err("Gagal membuka file di Android.".to_string());
  }

  #[cfg(target_os = "windows")]
  {
    std::process::Command::new("cmd")
      .args(["/C", "start", "", target])
      .spawn()
      .map(|_| ())
      .map_err(|e| format!("Gagal membuka file: {e}"))
  }
  #[cfg(target_os = "macos")]
  {
    std::process::Command::new("open")
      .arg(target)
      .spawn()
      .map(|_| ())
      .map_err(|e| format!("Gagal membuka file: {e}"))
  }
  #[cfg(all(unix, not(target_os = "macos"), not(target_os = "android")))]
  {
    std::process::Command::new("xdg-open")
      .arg(target)
      .spawn()
      .map(|_| ())
      .map_err(|e| format!("Gagal membuka file: {e}"))
  }
}

#[tauri::command]
fn get_app_version(app: tauri::AppHandle) -> String {
  #[cfg(target_os = "android")]
  {
    let identifier = app.config().identifier.clone();

    let parse_version_name = |text: &str| -> Option<String> {
      let line = text.lines().find(|ln| ln.contains("versionName="))?;
      let version = line.split('=').nth(1).map(|v| v.trim()).unwrap_or("");
      if version.is_empty() {
        None
      } else {
        Some(version.to_string())
      }
    };

    if let Ok(output) = std::process::Command::new("dumpsys")
      .args(["package", &identifier])
      .output()
    {
      if output.status.success() {
        let text = String::from_utf8_lossy(&output.stdout);
        if let Some(version) = parse_version_name(&text) {
          return version;
        }
      }
    }

    if let Ok(output) = std::process::Command::new("pm")
      .args(["dump", &identifier])
      .output()
    {
      if output.status.success() {
        let text = String::from_utf8_lossy(&output.stdout);
        if let Some(version) = parse_version_name(&text) {
          return version;
        }
      }
    }
  }
  app.package_info().version.to_string()
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
  #[cfg(target_os = "android")]
  {
    // Reqwest/rustls on Android needs an explicit crypto provider.
    let _ = rustls::crypto::ring::default_provider().install_default();
  }

  let builder = tauri::Builder::default().plugin(tauri_plugin_notification::init());
  #[cfg(any(target_os = "windows", target_os = "macos", target_os = "linux"))]
  let builder = builder.plugin(tauri_plugin_updater::Builder::new().build());

  builder
    .invoke_handler(tauri::generate_handler![
      open_external_url,
      open_whatsapp_message,
      show_local_notification,
      download_url_to_app_storage,
      save_base64_to_downloads,
      save_pdf_base64,
      open_file_path,
      get_app_version
    ])
    .setup(|app| {
      #[cfg(all(
        not(debug_assertions),
        any(target_os = "windows", target_os = "macos", target_os = "linux")
      ))]
      {
        let current_version = app.package_info().version.to_string();
        let app_handle = app.handle().clone();
        tauri::async_runtime::spawn(async move {
          emit_updater_status(
            &app_handle,
            "checking",
            "Memeriksa pembaruan aplikasi...",
            None,
            Some(&current_version),
            None,
            None,
          );

          let Ok(updater) = app_handle.updater() else {
            emit_updater_status(
              &app_handle,
              "error",
              "Gagal inisialisasi updater.",
              None,
              Some(&current_version),
              None,
              None,
            );
            return;
          };

          let Ok(update_result) = updater.check().await else {
            emit_updater_status(
              &app_handle,
              "error",
              "Gagal cek update dari server.",
              None,
              Some(&current_version),
              None,
              None,
            );
            return;
          };

          let Some(update) = update_result else {
            emit_updater_status(
              &app_handle,
              "no_update",
              "Siap.",
              Some(100.0),
              Some(&current_version),
              Some(&current_version),
              None,
            );
            return;
          };

          let latest_version = update.version.clone();
          let release_notes = update.body.clone();

          emit_updater_status(
            &app_handle,
            "available",
            "Update ditemukan. Mengunduh pembaruan...",
            Some(0.0),
            Some(&current_version),
            Some(&latest_version),
            release_notes.as_deref(),
          );

          let downloaded = Arc::new(Mutex::new(0_u64));
          let downloaded_for_progress = downloaded.clone();
          let app_for_progress = app_handle.clone();
          let app_for_downloaded = app_handle.clone();
          let current_version_for_progress = current_version.clone();
          let latest_version_for_progress = latest_version.clone();
          let release_notes_for_progress = release_notes.clone();
          let current_version_for_installing = current_version.clone();
          let latest_version_for_installing = latest_version.clone();
          let release_notes_for_installing = release_notes.clone();

          let Ok(_) = update
            .download_and_install(
              move |chunk_length, content_length| {
                if let Ok(mut current) = downloaded_for_progress.lock() {
                  *current = current.saturating_add(chunk_length as u64);
                  let total = content_length.unwrap_or(0);
                  if total > 0 {
                    let progress = ((*current as f64 / total as f64) * 100.0).clamp(0.0, 100.0);
                    emit_updater_status(
                      &app_for_progress,
                      "downloading",
                      "Mengunduh update desktop...",
                      Some(progress),
                      Some(&current_version_for_progress),
                      Some(&latest_version_for_progress),
                      release_notes_for_progress.as_deref(),
                    );
                  } else {
                    emit_updater_status(
                      &app_for_progress,
                      "downloading",
                      "Mengunduh update desktop...",
                      None,
                      Some(&current_version_for_progress),
                      Some(&latest_version_for_progress),
                      release_notes_for_progress.as_deref(),
                    );
                  }
                }
              },
              move || {
                emit_updater_status(
                  &app_for_downloaded,
                  "installing",
                  "Menginstal update. Mohon tunggu...",
                  Some(100.0),
                  Some(&current_version_for_installing),
                  Some(&latest_version_for_installing),
                  release_notes_for_installing.as_deref(),
                );
              },
            )
            .await
          else {
            emit_updater_status(
              &app_handle,
              "error",
              "Update gagal dipasang. Silakan coba lagi nanti.",
              None,
              Some(&current_version),
              Some(&latest_version),
              release_notes.as_deref(),
            );
            return;
          };

          emit_updater_status(
            &app_handle,
            "ready_restart",
            "Update selesai. Aplikasi akan restart otomatis.",
            Some(100.0),
            Some(&current_version),
            Some(&latest_version),
            release_notes.as_deref(),
          );
          std::thread::sleep(std::time::Duration::from_secs(2));
          app_handle.restart();
        });
      }
      #[cfg(debug_assertions)]
      let _ = app;
      Ok(())
    })
    .run(tauri::generate_context!())
    .expect("error while running tauri application");
}
