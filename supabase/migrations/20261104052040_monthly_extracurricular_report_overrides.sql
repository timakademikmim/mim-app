alter table public.ekstrakurikuler_laporan_bulanan
  add column if not exists nama_kegiatan text,
  add column if not exists penanggung_jawab text,
  add column if not exists no_hp_pj text;
