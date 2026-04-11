# Testing Lokal MCP HTTP Untuk ChatGPT App

Panduan ini untuk memastikan server MCP lokal sudah siap sebelum kita sambungkan ke ChatGPT.

## Prasyarat

1. Isi environment di folder:
   - [`.env.example`](d:\004%20MA%20MIM\000%20App\login-supabase\chatgpt-app\mcp-server\.env.example)
2. Minimal isi:
   - `SUPABASE_FUNCTIONS_BASE_URL`
   - `EXAM_AI_APP_SHARED_SECRET`
3. Opsional:
   - `CHATGPT_APP_GURU_RESOLVER_SHARED_SECRET`
   - `CHATGPT_APP_GURU_RESOLVER_MODE`
   - `MCP_HTTP_PORT`
   - `MCP_HTTP_PATH`

## Menjalankan Server MCP HTTP

```powershell
cd chatgpt-app\mcp-server
npm run dev:http
```

Default endpoint:

1. root: `http://localhost:3030/`
2. health: `http://localhost:3030/healthz`
3. MCP: `http://localhost:3030/mcp`

## Smoke Test MCP

Dalam terminal kedua:

```powershell
cd chatgpt-app\mcp-server
npm run smoke:http
```

Hasil yang diharapkan:

1. `ok: true`
2. `tool_count: 6`
3. daftar tool berisi:
   - `resolve_chatgpt_app_guru`
   - `find_exam_ai_target`
   - `get_existing_exam_draft`
   - `exchange_chatgpt_app_link_code`
   - `save_exam_ai_draft`
   - `revise_exam_ai_draft`

## Langkah Berikut Untuk Uji Dengan ChatGPT

Karena ChatGPT membutuhkan endpoint MCP yang bisa diakses lewat HTTPS, jalur lokal perlu ditunnel.

Contoh umum:

```powershell
ngrok http 3030
```

Setelah itu:

1. ambil URL HTTPS hasil tunnel
2. arahkan ke path `/mcp`
3. baru dipakai untuk uji integrasi ChatGPT

Contoh:

```text
https://xxxx.ngrok-free.app/mcp
```

## Catatan

1. Panduan ini masih untuk testing lokal/internals.
2. Untuk produksi, kita tetap butuh endpoint HTTPS yang stabil, bukan tunnel sementara.
3. Link code dan resolver live sudah siap, jadi setelah MCP endpoint stabil, fokus berikutnya tinggal penyambungan ke ChatGPT.
