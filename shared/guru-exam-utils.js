;(function initGuruExamUtils() {
  if (window.guruExamUtils) return

  function normalizeExamLookup(value) {
    return String(value || '').trim().toLowerCase().replace(/\s+/g, ' ')
  }

  function getExamPerangkatanFromClassName(kelasName) {
    const text = String(kelasName || '').trim().toLowerCase()
    if (!text) return 'SMP'
    if (text.includes('smp') || /^([789])([a-z]|\b|[-\s]|$)/i.test(text) || /\b7\b|\b8\b|\b9\b/.test(text)) return 'SMP'
    if (text.includes('sma') || text.includes('ma ') || text.endsWith(' ma') || /^(x|xi|xii)(\b|[-\s]|$)/i.test(text) || /\b10\b|\b11\b|\b12\b/.test(text)) return 'SMA'
    return 'SMP'
  }

  function getExamMapelBaseLabel(mapelText) {
    const raw = String(mapelText || '').trim()
    if (!raw) return ''
    return raw
      .replace(/\(\s*(SMP|SMA|Umum)\s*\)/ig, '')
      .replace(/(\s+(SMP|SMA|Umum))+$/i, '')
      .replace(/\s{2,}/g, ' ')
      .trim()
  }

  function parseExamMetaFromSchedule(row) {
    const raw = String(row?.keterangan || '').trim()
    if (!raw) return {}
    try {
      const parsed = JSON.parse(raw)
      return parsed && typeof parsed === 'object' ? parsed : {}
    } catch (_err) {
      return {}
    }
  }

  function splitExamClassTokens(value) {
    const raw = String(value || '').trim()
    if (!raw) return []
    const normalized = raw.replace(/\s+(dan|&)\s+/ig, ',')
    return [...new Set(
      normalized
        .split(/[;,/|]+/)
        .map(item => String(item || '').trim())
        .filter(Boolean)
    )]
  }

  function getExamRowClassLabel(row) {
    const meta = parseExamMetaFromSchedule(row)
    const classRows = Array.isArray(meta?.class_rows) ? meta.class_rows : []
    const kelasNames = [...new Set(classRows.map(item => String(item?.kelas_nama || '').trim()).filter(Boolean))]
    if (kelasNames.length) return kelasNames.join(', ')
    return String(row?.kelas || '-')
  }

  function getExamRowClassList(row, fallbackClassNames = []) {
    const meta = parseExamMetaFromSchedule(row)
    const classRows = Array.isArray(meta?.class_rows) ? meta.class_rows : []
    const kelasNames = [...new Set(classRows.map(item => String(item?.kelas_nama || '').trim()).filter(Boolean))]
    if (kelasNames.length) return kelasNames

    const altMetaList = []
      .concat(Array.isArray(meta?.kelas_list) ? meta.kelas_list : [])
      .concat(Array.isArray(meta?.kelas_rows) ? meta.kelas_rows.map(item => item?.kelas_nama || item?.kelas || '') : [])
      .concat(Array.isArray(meta?.classes) ? meta.classes : [])
    const metaClasses = [...new Set(altMetaList.map(item => String(item || '').trim()).filter(Boolean))]
    if (metaClasses.length) return metaClasses

    const fallbackFromDistribusi = [...new Set(
      (Array.isArray(fallbackClassNames) ? fallbackClassNames : [])
        .map(item => String(item || '').trim())
        .filter(Boolean)
    )]
    if (fallbackFromDistribusi.length) return fallbackFromDistribusi

    const fallback = String(row?.kelas || '').trim()
    const splitFallback = splitExamClassTokens(fallback)
    return splitFallback.length ? splitFallback : (fallback ? [fallback] : [])
  }

  function getExamRowMapelLabel(row) {
    const meta = parseExamMetaFromSchedule(row)
    const mapelRaw = String(meta?.mapel_nama || '').trim() || String(row?.mapel || '').trim()
    const mapelBase = getExamMapelBaseLabel(mapelRaw)
    return mapelBase || '-'
  }

  window.guruExamUtils = {
    normalizeExamLookup,
    getExamPerangkatanFromClassName,
    getExamMapelBaseLabel,
    parseExamMetaFromSchedule,
    splitExamClassTokens,
    getExamRowClassLabel,
    getExamRowClassList,
    getExamRowMapelLabel
  }
})()

