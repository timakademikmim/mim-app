# Uji ChatGPT App Dari Web ChatGPT

Dokumen ini merangkum langkah uji dari sisi ChatGPT web, setelah backend linking dan MCP server lokal sudah siap.

## Status Sebelum Mulai

Yang sudah siap:

1. `resolve-chatgpt-app-guru` live
2. `exchange-chatgpt-app-link-code` live
3. MCP server lokal mendukung `stdio`
4. MCP server lokal mendukung `streamable HTTP` pada `/mcp`
5. smoke test lokal MCP HTTP sudah lolos

## Yang Perlu Disiapkan

1. File `.env` di folder:
   - `chatgpt-app/mcp-server/.env`
2. Jalankan server lokal:
   - [run-chatgpt-mcp-http.ps1](d:\004%20MA%20MIM\000%20App\login-supabase\scripts\run-chatgpt-mcp-http.ps1)
3. Expose ke HTTPS
   - bisa lewat `cloudflared` quick tunnel
   - atau `ngrok` kalau kamu punya authtoken

## Langkah Lokal

1. Jalankan MCP server:

```powershell
.\scripts\run-chatgpt-mcp-http.ps1
```

2. Di terminal lain, cek smoke test:

```powershell
.\scripts\smoke-chatgpt-mcp-http.ps1
```

3. Jalankan tunnel HTTPS. Rekomendasi yang sudah saya siapkan:

```powershell
.\scripts\run-chatgpt-mcp-cloudflare.ps1
```

4. Catat URL tunnel, lalu tambahkan path `/mcp`

Contoh:

```text
https://xxxx.trycloudflare.com/mcp
```

Alternatif kalau mau pakai ngrok:

```powershell
ngrok http 3030
```

## Langkah Di ChatGPT Web

1. Aktifkan `developer mode`
   - `Settings -> Apps & Connectors -> Advanced settings`
2. Buka:
   - `Settings -> Connectors -> Create`
3. Isi connector:
   - `Connector name`: `MIM Generator Soal`
   - `Connector URL`: URL HTTPS tunnel + `/mcp`

Contoh:

```text
https://xxxx.ngrok-free.app/mcp
```

4. Simpan connector
5. Buka chat baru di ChatGPT
6. Aktifkan connector yang baru dibuat

## Skenario Uji Yang Disarankan

### 1. Uji linking guru

Prompt:

```text
Saya ingin menautkan akun saya ke guru di sistem. Saya punya kode tautan 24 jam.
```

Harapan:

1. ChatGPT memanggil `resolve_chatgpt_app_guru`
2. hasil `not_linked`
3. ChatGPT meminta kode
4. ChatGPT memanggil `exchange_chatgpt_app_link_code`
5. link permanen terbentuk

### 2. Uji cari target ujian

Prompt:

```text
Buatkan draft soal untuk kelas X mapel Sharf tanggal 2026-04-10.
```

Harapan:

1. ChatGPT resolve guru
2. ChatGPT mencari target dengan `find_exam_ai_target`
3. jika target tunggal ditemukan, lanjut membaca draft lama

### 3. Uji simpan draft

Prompt:

```text
Buatkan 5 soal pilihan ganda materi wazan fi'il tsulatsi mujarrad untuk kelas X mapel Sharf tanggal 2026-04-10.
```

Harapan:

1. tool target terpanggil
2. tool draft lama terpanggil
3. ChatGPT menghasilkan soal terstruktur
4. ChatGPT memanggil `save_exam_ai_draft` atau `revise_exam_ai_draft`
5. editor ujian di web sekolah terisi

## Referensi Resmi OpenAI

OpenAI menyebut:

1. ChatGPT testing memakai `developer mode`
2. connector dibuat dari `Settings -> Connectors -> Create`
3. MCP server harus reachable lewat `HTTPS`
4. untuk development lokal, bisa pakai `ngrok` atau `Cloudflare Tunnel`

Sumber:

1. [Connect from ChatGPT](https://developers.openai.com/apps-sdk/deploy/connect-chatgpt)
2. [Build your MCP server](https://developers.openai.com/apps-sdk/build/mcp-server)

Cuplikan penting dari docs resmi:

1. `Connect from ChatGPT` menyebut developer mode dipakai untuk test app dengan akun sendiri, dan connector dibuat dari ChatGPT settings.
2. `Build your MCP server` menyebut ChatGPT membutuhkan HTTPS, dan untuk development lokal bisa menunnel localhost dengan `ngrok`.
