#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

#[cfg(not(debug_assertions))]
use std::sync::{Arc, Mutex};
#[cfg(not(debug_assertions))]
use tauri::Manager;
#[cfg(not(debug_assertions))]
use tauri_plugin_updater::UpdaterExt;

#[cfg(not(debug_assertions))]
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

#[tauri::command]
fn open_external_url(url: String) -> Result<(), String> {
  let target = url.trim();
  if target.is_empty() {
    return Err("URL kosong.".to_string());
  }
  tauri::webbrowser::open(target)
    .map(|_| ())
    .map_err(|e| format!("Gagal membuka URL eksternal: {e}"))
}

fn main() {
  tauri::Builder::default()
    .plugin(tauri_plugin_updater::Builder::new().build())
    .invoke_handler(tauri::generate_handler![open_external_url])
    .setup(|app| {
      #[cfg(not(debug_assertions))]
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
              "Aplikasi sudah versi terbaru.",
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
