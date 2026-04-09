import { serve } from "https://deno.land/std@0.224.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const corsHeaders = {
  "access-control-allow-origin": "*",
  "access-control-allow-headers": "authorization, x-client-info, apikey, content-type"
}

function jsonResponse(status: number, body: Record<string, unknown>) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", ...corsHeaders }
  })
}

const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? ""
const supabaseKey =
  Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ||
  Deno.env.get("SUPABASE_ANON_KEY") ||
  ""
const sharedSecret = String(Deno.env.get("EXAM_AI_APP_SHARED_SECRET") || "").trim()
const supabase = createClient(supabaseUrl, supabaseKey)

type DraftTargetPayload = {
  shared_secret?: string
  guru_id?: string
  kelas?: string
  mapel?: string
  tanggal?: string
  nama_ujian?: string | null
  jenis?: string | null
}

type KelasRow = {
  id?: string
  nama_kelas?: string
}

type MapelRow = {
  id?: string
  nama?: string
  nama_mapel?: string
}

type DistribusiRow = {
  id?: string
  kelas_id?: string
  mapel_id?: string
  guru_id?: string
  semester_id?: string | null
}

type JadwalRow = {
  id?: string
  jenis?: string
  nama?: string
  kelas?: string
  mapel?: string
  tanggal?: string
  jam_mulai?: string | null
  jam_selesai?: string | null
  keterangan?: string | null
}

function normalizeText(value: unknown) {
  return String(value ?? "").trim()
}

function normalizeLookup(value: unknown) {
  return normalizeText(value).toLowerCase().replace(/\s+/g, " ")
}

function normalizeClassLookup(value: unknown) {
  return normalizeText(value)
    .replace(/^kelas\s+/i, "")
    .replace(/\s+/g, " ")
    .trim()
    .toUpperCase()
}

function getExamPerangkatanFromClassName(kelasName: unknown) {
  const text = normalizeText(kelasName).toLowerCase()
  if (!text) return "SMP"
  if (text.includes("smp") || /^([789])([a-z]|\b|[-\s]|$)/i.test(text) || /\b7\b|\b8\b|\b9\b/.test(text)) return "SMP"
  if (text.includes("sma") || text.includes("ma ") || text.endsWith(" ma") || /^(x|xi|xii)(\b|[-\s]|$)/i.test(text) || /\b10\b|\b11\b|\b12\b/.test(text)) return "SMA"
  return "SMP"
}

function getExamMapelBaseLabel(mapelText: unknown) {
  const raw = normalizeText(mapelText)
  if (!raw) return ""
  return raw
    .replace(/\s*\([^)]*\)\s*/g, " ")
    .replace(/\(\s*(SMP|SMA|Umum)\s*\)/ig, "")
    .replace(/(\s+(SMP|SMA|Umum))+$/i, "")
    .replace(/\s{2,}/g, " ")
    .trim()
}

function normalizeMapelKey(mapelText: unknown) {
  const raw = normalizeLookup(getExamMapelBaseLabel(mapelText))
    .replace(/'/g, "")
    .replace(/[.]/g, " ")
    .replace(/[^a-z0-9\s-]/g, " ")
    .replace(/\s+/g, " ")
    .trim()
  if (!raw) return ""
  if (/\b(sharf|sharaf|shorof|sorof)\b/.test(raw)) return "sharf"
  if (/\b(nahwu)\b/.test(raw)) return "nahwu"
  if (/\b(fiqih|fikih|fiqh)\b/.test(raw)) return "fikih"
  if (/\b(aqidah|akidah)\b/.test(raw)) return "akidah"
  if (/\b(sirah|siroh|tarikh)\b/.test(raw)) return "sirah"
  if (/\b(hadits|hadis)\b/.test(raw)) return "hadits"
  if (/\b(tafsir)\b/.test(raw)) return "tafsir"
  if (/\b(akhlak)\b/.test(raw)) return "akhlak"
  if (/\b(bahasa arab|arab)\b/.test(raw)) return "bahasa arab"
  if (/\b(bahasa indonesia|indonesia)\b/.test(raw)) return "bahasa indonesia"
  if (/\b(bahasa inggris|inggris|english)\b/.test(raw)) return "bahasa inggris"
  if (/\b(matematika|math|mathematics|mtk)\b/.test(raw)) return "matematika"
  if (/\b(ipa|ilmu pengetahuan alam|science)\b/.test(raw)) return "ipa"
  if (/\b(ips|ilmu pengetahuan sosial|social studies)\b/.test(raw)) return "ips"
  if (/\b(ppkn|pkn|kewarganegaraan)\b/.test(raw)) return "pkn"
  if (/\b(quran|qur an|alquran|al quran|al-quran)\b/.test(raw)) return "quran"
  return raw
}

function mapelMatches(left: unknown, right: unknown) {
  const a = normalizeMapelKey(left)
  const b = normalizeMapelKey(right)
  if (!a || !b) return false
  return a === b || a.includes(b) || b.includes(a)
}

function parseExamMetaFromSchedule(row: JadwalRow) {
  const raw = normalizeText(row?.keterangan)
  if (!raw) return {}
  try {
    const parsed = JSON.parse(raw)
    return parsed && typeof parsed === "object" ? parsed : {}
  } catch (_error) {
    return {}
  }
}

function splitExamClassTokens(value: unknown) {
  const raw = normalizeText(value)
  if (!raw) return []
  const normalized = raw.replace(/\s+(dan|&)\s+/ig, ",")
  return [...new Set(
    normalized
      .split(/[;,/|]+/)
      .map(item => normalizeText(item))
      .filter(Boolean)
  )]
}

function getExamRowClassList(row: JadwalRow, fallbackClassNames: string[] = []) {
  const meta = parseExamMetaFromSchedule(row) as Record<string, unknown>
  const classRows = Array.isArray(meta?.class_rows) ? meta.class_rows : []
  const kelasNames = [...new Set(classRows
    .map(item => normalizeText((item as Record<string, unknown>)?.kelas_nama))
    .filter(Boolean))]
  if (kelasNames.length) return kelasNames

  const altMetaList = []
    .concat(Array.isArray(meta?.kelas_list) ? meta.kelas_list : [])
    .concat(Array.isArray(meta?.kelas_rows) ? meta.kelas_rows.map(item => (item as Record<string, unknown>)?.kelas_nama || (item as Record<string, unknown>)?.kelas || "") : [])
    .concat(Array.isArray(meta?.classes) ? meta.classes : [])
  const metaClasses = [...new Set(altMetaList.map(item => normalizeText(item)).filter(Boolean))]
  if (metaClasses.length) return metaClasses

  const fallbackFromDistribusi = [...new Set(fallbackClassNames.map(item => normalizeText(item)).filter(Boolean))]
  if (fallbackFromDistribusi.length) return fallbackFromDistribusi

  const fallback = normalizeText(row?.kelas)
  const splitFallback = splitExamClassTokens(fallback)
  return splitFallback.length ? splitFallback : (fallback ? [fallback] : [])
}

function getGradeToken(value: unknown) {
  const raw = normalizeClassLookup(value)
  if (!raw) return ""
  const directRoman = raw.match(/\b(XII|XI|X|IX|VIII|VII)\b/)
  if (directRoman?.[1]) return directRoman[1]
  const directNumber = raw.match(/\b(12|11|10|9|8|7)\b/)
  if (directNumber?.[1]) {
    const map: Record<string, string> = { "12": "XII", "11": "XI", "10": "X", "9": "IX", "8": "VIII", "7": "VII" }
    return map[directNumber[1]] || ""
  }
  return ""
}

function classMatches(requestedClass: unknown, candidateClass: unknown) {
  const requested = normalizeClassLookup(requestedClass)
  const candidate = normalizeClassLookup(candidateClass)
  if (!requested || !candidate) return false
  if (requested === candidate) return true

  const requestedGrade = getGradeToken(requested)
  const candidateGrade = getGradeToken(candidate)
  if (requestedGrade && candidateGrade && requestedGrade === candidateGrade) return true
  if (candidate.startsWith(`${requested} `) || requested.startsWith(`${candidate} `)) return true
  return false
}

function examNameMatches(requestedName: unknown, scheduleName: unknown, requestedJenis: unknown, scheduleJenis: unknown) {
  const reqName = normalizeLookup(requestedName)
  const schedName = normalizeLookup(scheduleName)
  const reqJenis = normalizeLookup(requestedJenis)
  const schedJenis = normalizeLookup(scheduleJenis)
  if (reqName && schedName && !(schedName.includes(reqName) || reqName.includes(schedName))) return false
  if (reqJenis && schedJenis && !(schedJenis.includes(reqJenis) || reqJenis.includes(schedJenis))) return false
  return true
}

async function fetchMapelRows(ids: string[]) {
  if (!ids.length) return [] as Array<{ id: string; nama_mapel: string }>
  const uniqueIds = [...new Set(ids.map(item => normalizeText(item)).filter(Boolean))]
  let res = await supabase.from("mapel").select("id, nama").in("id", uniqueIds)
  if (!res.error) {
    return (res.data || []).map((item: MapelRow) => ({
      id: normalizeText(item?.id),
      nama_mapel: normalizeText(item?.nama)
    })).filter(item => item.id)
  }

  const fallback = await supabase.from("mapel").select("id, nama_mapel").in("id", uniqueIds)
  if (fallback.error) throw fallback.error
  return (fallback.data || []).map((item: MapelRow) => ({
    id: normalizeText(item?.id),
    nama_mapel: normalizeText(item?.nama_mapel)
  })).filter(item => item.id)
}

serve(async req => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders })
  }

  if (req.method !== "POST") {
    return jsonResponse(405, { ok: false, error: "Method not allowed" })
  }

  let payload: DraftTargetPayload
  try {
    payload = await req.json()
  } catch (_error) {
    return jsonResponse(400, { ok: false, error: "Invalid JSON" })
  }

  if (sharedSecret) {
    const incomingSecret = normalizeText(payload.shared_secret)
    if (!incomingSecret || incomingSecret !== sharedSecret) {
      return jsonResponse(401, { ok: false, error: "Unauthorized" })
    }
  }

  const guruId = normalizeText(payload.guru_id)
  const kelas = normalizeText(payload.kelas)
  const mapel = normalizeText(payload.mapel)
  const tanggal = normalizeText(payload.tanggal)
  const namaUjian = normalizeText(payload.nama_ujian)
  const jenis = normalizeText(payload.jenis)

  if (!guruId || !kelas || !mapel || !tanggal) {
    return jsonResponse(400, {
      ok: false,
      error: "guru_id, kelas, mapel, dan tanggal wajib diisi"
    })
  }

  const distribusiRes = await supabase
    .from("distribusi_mapel")
    .select("id, kelas_id, mapel_id, guru_id, semester_id")
    .eq("guru_id", guruId)

  if (distribusiRes.error) {
    return jsonResponse(500, { ok: false, error: distribusiRes.error.message })
  }

  const distribusiRows = (distribusiRes.data || []) as DistribusiRow[]
  if (!distribusiRows.length) {
    return jsonResponse(200, {
      ok: false,
      status: "not_found",
      error: "Distribusi mapel guru tidak ditemukan"
    })
  }

  const kelasIds = [...new Set(distribusiRows.map(item => normalizeText(item?.kelas_id)).filter(Boolean))]
  const mapelIds = [...new Set(distribusiRows.map(item => normalizeText(item?.mapel_id)).filter(Boolean))]

  const [kelasRes, mapelRows] = await Promise.all([
    kelasIds.length ? supabase.from("kelas").select("id, nama_kelas").in("id", kelasIds) : Promise.resolve({ data: [], error: null }),
    fetchMapelRows(mapelIds)
  ])

  if (kelasRes.error) {
    return jsonResponse(500, { ok: false, error: kelasRes.error.message })
  }

  const kelasMap = new Map((kelasRes.data || []).map((item: KelasRow) => [normalizeText(item?.id), normalizeText(item?.nama_kelas)]))
  const mapelMap = new Map(mapelRows.map(item => [item.id, item.nama_mapel]))

  const distribusiDetailList = distribusiRows.map(item => {
    const kelasNama = normalizeText(kelasMap.get(normalizeText(item?.kelas_id)))
    const mapelNama = normalizeText(mapelMap.get(normalizeText(item?.mapel_id)))
    const perangkatan = getExamPerangkatanFromClassName(kelasNama)
    const mapelKey = normalizeMapelKey(mapelNama)
    return {
      id: normalizeText(item?.id),
      kelas_id: normalizeText(item?.kelas_id),
      kelas_nama: kelasNama,
      mapel_id: normalizeText(item?.mapel_id),
      mapel_nama: mapelNama,
      perangkatan,
      mapel_key: mapelKey
    }
  }).filter(item => item.kelas_nama && item.mapel_nama)

  const matchingDistribusi = distribusiDetailList.filter(item => (
    mapelMatches(item.mapel_nama, mapel) &&
    classMatches(kelas, item.kelas_nama)
  ))

  if (!matchingDistribusi.length) {
    return jsonResponse(200, {
      ok: false,
      status: "not_found",
      error: "Tidak ada distribusi mapel guru yang cocok dengan kelas dan mapel tersebut"
    })
  }

  const allowedByPerangkatanMapel = new Map<string, Set<string>>()
  matchingDistribusi.forEach(item => {
    const key = `${normalizeLookup(item.perangkatan)}|${item.mapel_key}`
    if (!allowedByPerangkatanMapel.has(key)) allowedByPerangkatanMapel.set(key, new Set())
    allowedByPerangkatanMapel.get(key)?.add(item.kelas_nama)
  })

  const jadwalRes = await supabase
    .from("jadwal_ujian")
    .select("id, jenis, nama, kelas, mapel, tanggal, jam_mulai, jam_selesai, keterangan")
    .eq("tanggal", tanggal)

  if (jadwalRes.error) {
    return jsonResponse(500, { ok: false, error: jadwalRes.error.message })
  }

  const candidateRows = ((jadwalRes.data || []) as JadwalRow[]).flatMap(row => {
    const meta = parseExamMetaFromSchedule(row) as Record<string, unknown>
    const perangkatan = normalizeText(meta?.perangkatan) || normalizeText(row?.kelas)
    const mapelLabel = getExamMapelBaseLabel(normalizeText(meta?.mapel_nama) || normalizeText(row?.mapel))
    const mapelKey = normalizeMapelKey(mapelLabel)
    const allowKey = `${normalizeLookup(perangkatan)}|${mapelKey}`
    const allowedClasses = [...(allowedByPerangkatanMapel.get(allowKey) || new Set())]

    if (!allowedClasses.length) return []
    if (!mapelMatches(mapelLabel, mapel)) return []
    if (!examNameMatches(namaUjian, row?.nama, jenis, row?.jenis)) return []

    const visibleClasses = getExamRowClassList(row, allowedClasses)
      .filter(className => allowedClasses.some(allowedClass => classMatches(className, allowedClass)))
      .filter(className => classMatches(kelas, className))

    return visibleClasses.map(className => ({
      jadwal_id: normalizeText(row?.id),
      kelas_target: className,
      guru_id: guruId,
      mapel_label: mapelLabel || normalizeText(row?.mapel),
      nama_ujian: normalizeText(row?.nama),
      jenis_ujian: normalizeText(row?.jenis),
      tanggal: normalizeText(row?.tanggal),
      jam_mulai: normalizeText(row?.jam_mulai) || null,
      jam_selesai: normalizeText(row?.jam_selesai) || null
    }))
  })

  const dedupedCandidates = [...new Map(candidateRows
    .map(item => [`${item.jadwal_id}|${normalizeClassLookup(item.kelas_target)}`, item] as const))
    .values()]
    .sort((a, b) => {
      const classCompare = normalizeClassLookup(a.kelas_target).localeCompare(normalizeClassLookup(b.kelas_target))
      if (classCompare !== 0) return classCompare
      const examCompare = normalizeLookup(a.nama_ujian).localeCompare(normalizeLookup(b.nama_ujian))
      if (examCompare !== 0) return examCompare
      return normalizeLookup(a.mapel_label).localeCompare(normalizeLookup(b.mapel_label))
    })

  if (!dedupedCandidates.length) {
    return jsonResponse(200, {
      ok: false,
      status: "not_found",
      error: "Jadwal ujian yang cocok belum ditemukan untuk kelas, mapel, dan tanggal tersebut"
    })
  }

  if (dedupedCandidates.length > 1) {
    return jsonResponse(200, {
      ok: true,
      status: "ambiguous",
      target: null,
      candidates: dedupedCandidates,
      summary: {
        total_candidates: dedupedCandidates.length,
        guru_id: guruId,
        tanggal,
        kelas,
        mapel
      }
    })
  }

  return jsonResponse(200, {
    ok: true,
    status: "found",
    target: dedupedCandidates[0],
    candidates: dedupedCandidates,
    summary: {
      total_candidates: 1,
      guru_id: guruId,
      tanggal,
      kelas,
      mapel
    }
  })
})

