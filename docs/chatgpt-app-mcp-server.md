# MCP Server Lokal Untuk ChatGPT App

Folder ini menyiapkan scaffold MCP server lokal:

- [package.json](d:\004%20MA%20MIM\000%20App\login-supabase\chatgpt-app\mcp-server\package.json)
- [index.ts](d:\004%20MA%20MIM\000%20App\login-supabase\chatgpt-app\mcp-server\src\index.ts)
- [http-server.ts](d:\004%20MA%20MIM\000%20App\login-supabase\chatgpt-app\mcp-server\src\http-server.ts)
- [server-factory.ts](d:\004%20MA%20MIM\000%20App\login-supabase\chatgpt-app\mcp-server\src\server-factory.ts)

## Tujuan

ChatGPT App nantinya tidak berbicara langsung ke database. Jalurnya:

1. ChatGPT App memanggil MCP server lokal
2. MCP server memanggil Supabase Edge Functions yang sudah kita siapkan
3. Edge Function menulis atau membaca draft ujian

Tools yang sudah disiapkan di scaffold ini:

1. `resolve_chatgpt_app_guru`
2. `find_exam_ai_target`
3. `get_existing_exam_draft`
4. `exchange_chatgpt_app_link_code`
5. `save_exam_ai_draft`
6. `revise_exam_ai_draft`

## Status Integrasi

1. `find_exam_ai_target`: backend live
2. `get_existing_exam_draft`: backend live
3. `save_exam_ai_draft`: backend live
4. `resolve_chatgpt_app_guru`: backend live
5. `exchange_chatgpt_app_link_code`: backend live
6. `revise_exam_ai_draft`: logika lokal di MCP, memanfaatkan `get_existing_exam_draft` + `save_exam_ai_draft`
7. Transport MCP:
   - `stdio`: siap untuk local smoke test
   - `streamable HTTP`: siap lokal pada `http-server.ts`

## Environment

Lihat contoh:

- [.env.example](d:\004%20MA%20MIM\000%20App\login-supabase\chatgpt-app\mcp-server\.env.example)

Yang wajib diisi:

1. `SUPABASE_FUNCTIONS_BASE_URL`
2. `EXAM_AI_APP_SHARED_SECRET`

Opsional untuk linking dan transisi ke resolver live:

3. `CHATGPT_APP_GURU_RESOLVER_MODE`
4. `CHATGPT_APP_GURU_RESOLVER_SHARED_SECRET`
5. `CHATGPT_APP_LOCAL_LINKS_JSON`
6. `MCP_HTTP_PORT`
7. `MCP_HTTP_PATH`

Jika `CHATGPT_APP_GURU_RESOLVER_SHARED_SECRET` tidak diisi, MCP akan fallback ke `EXAM_AI_APP_SHARED_SECRET`.

Mode resolver yang didukung:

1. `local-only`
2. `local-first`
3. `live-first`
4. `live-only`

Rekomendasi transisi:

1. fase lokal: `local-only` atau `local-first`
2. fase staging: `live-first`
3. fase production stabil: `live-only`

Opsional untuk smoke test:

8. `SMOKE_EXTERNAL_SUBJECT`
9. `SMOKE_PROVIDER`

## Cara Menjalankan

### Mode stdio

```powershell
cd chatgpt-app\mcp-server
npm run dev
```

### Mode HTTP `/mcp`

```powershell
cd chatgpt-app\mcp-server
npm run dev:http
```

Smoke test MCP endpoint:

```powershell
cd chatgpt-app\mcp-server
npm run smoke:http
```

Default lokal:

1. root: `http://localhost:3030/`
2. health: `http://localhost:3030/healthz`
3. MCP endpoint: `http://localhost:3030/mcp`

Kalau mau dihubungkan ke ChatGPT saat uji lokal, endpoint ini perlu ditunnel ke HTTPS, misalnya lewat `ngrok`.

Panduan lebih rinci:

- [chatgpt-app-local-http-testing.md](d:\004%20MA%20MIM\000%20App\login-supabase\docs\chatgpt-app-local-http-testing.md)

## Catatan

1. Logika bisnis utama tetap ada di Supabase Functions, jadi MCP server ini berperan sebagai jembatan untuk ChatGPT App.
2. Resolver identitas guru sekarang sudah mendukung strategi bertahap: lokal, live, atau fallback campuran.
3. `CHATGPT_APP_LOCAL_LINKS_JSON` tetap berguna untuk fallback cepat kalau live link sedang belum tersedia.
4. Untuk integrasi produksi, endpoint MCP sebaiknya berada di domain HTTPS yang stabil.

## Referensi Resmi

OpenAI menjelaskan bahwa Apps SDK membangun ChatGPT apps dengan:

1. MCP server untuk tools
2. komponen UI web bila diperlukan

Referensi:

1. Apps SDK quickstart: https://developers.openai.com/apps-sdk/quickstart
2. Apps SDK reference: https://developers.openai.com/apps-sdk/reference
3. Build an MCP server: https://developers.openai.com/apps-sdk/build/mcp-server
4. Connect from ChatGPT: https://developers.openai.com/apps-sdk/deploy/connect-chatgpt
