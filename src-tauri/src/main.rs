#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

#[cfg(not(debug_assertions))]
use tauri_plugin_updater::UpdaterExt;

fn main() {
  tauri::Builder::default()
    .plugin(tauri_plugin_updater::Builder::new().build())
    .setup(|app| {
      #[cfg(not(debug_assertions))]
      {
        let app_handle = app.handle().clone();
        tauri::async_runtime::spawn(async move {
          let Ok(updater) = app_handle.updater() else {
            return;
          };

          let Ok(Some(update)) = updater.check().await else {
            return;
          };

          let Ok(_) = update.download_and_install(|_, _| {}, || {}).await else {
            return;
          };
        });
      }
      #[cfg(debug_assertions)]
      let _ = app;
      Ok(())
    })
    .run(tauri::generate_context!())
    .expect("error while running tauri application");
}
