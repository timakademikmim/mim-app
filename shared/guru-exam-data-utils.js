;(function initGuruExamDataUtils() {
  if (window.guruExamDataUtils) return

  function buildExamDistribusiMaps({
    yearDistribusiList = [],
    kelasMap,
    mapelMap,
    getMapelLabel,
    normalizeExamLookup,
    getExamPerangkatanFromClassName,
    getExamMapelBaseLabel
  }) {
    const mapelPairsByClass = new Set(
      yearDistribusiList.map(item => {
        const kelas = kelasMap.get(String(item.kelas_id || ''))
        const mapel = mapelMap.get(String(item.mapel_id || ''))
        return `${normalizeExamLookup(kelas?.nama_kelas)}|${normalizeExamLookup(getMapelLabel(mapel))}`
      }).filter(Boolean)
    )

    const mapelPairsByPerangkatan = new Set(
      yearDistribusiList.map(item => {
        const kelas = kelasMap.get(String(item.kelas_id || ''))
        const mapel = mapelMap.get(String(item.mapel_id || ''))
        const perangkatan = getExamPerangkatanFromClassName(kelas?.nama_kelas)
        const mapelBase = getExamMapelBaseLabel(getMapelLabel(mapel))
        if (!perangkatan || !mapelBase) return ''
        return `${normalizeExamLookup(mapelBase)}|${normalizeExamLookup(perangkatan)}`
      }).filter(Boolean)
    )

    const classListByMapelPerangkatan = new Map()
    yearDistribusiList.forEach(item => {
      const kelas = kelasMap.get(String(item.kelas_id || ''))
      const mapel = mapelMap.get(String(item.mapel_id || ''))
      const kelasNama = String(kelas?.nama_kelas || '').trim()
      const perangkatan = getExamPerangkatanFromClassName(kelasNama)
      const mapelBase = getExamMapelBaseLabel(getMapelLabel(mapel))
      if (!kelasNama || !perangkatan || !mapelBase) return
      const key = `${normalizeExamLookup(mapelBase)}|${normalizeExamLookup(perangkatan)}`
      if (!classListByMapelPerangkatan.has(key)) classListByMapelPerangkatan.set(key, new Set())
      classListByMapelPerangkatan.get(key).add(kelasNama)
    })

    const normalizedClassMap = new Map()
    classListByMapelPerangkatan.forEach((setValue, key) => {
      normalizedClassMap.set(key, [...setValue].sort((a, b) => a.localeCompare(b)))
    })

    return { mapelPairsByClass, mapelPairsByPerangkatan, normalizedClassMap }
  }

  function resolveVisibleClassList({
    item,
    distKey,
    normalizedClassMap,
    normalizeExamLookup,
    getExamRowClassList
  }) {
    var allowedClasses = Array.isArray(normalizedClassMap && normalizedClassMap.get(distKey))
      ? normalizedClassMap.get(distKey)
      : []
    var rowClasses = typeof getExamRowClassList === 'function'
      ? getExamRowClassList(item, [])
      : []

    var normalizedAllowedMap = new Map(
      allowedClasses
        .map(function (kelasNama) { return String(kelasNama || '').trim() })
        .filter(Boolean)
        .map(function (kelasNama) { return [normalizeExamLookup(kelasNama), kelasNama] })
    )

    if (!normalizedAllowedMap.size) return rowClasses
    if (!rowClasses.length) return allowedClasses

    return rowClasses.filter(function (kelasNama) {
      return normalizedAllowedMap.has(normalizeExamLookup(kelasNama))
    })
  }

  function filterExamScheduleRows({
    jadwalRows = [],
    mapelPairsByClass,
    mapelPairsByPerangkatan,
    normalizedClassMap,
    normalizeExamLookup,
    parseExamMetaFromSchedule,
    getExamMapelBaseLabel,
    getExamRowClassList
  }) {
    return jadwalRows.filter(item => {
      const keyByClass = `${normalizeExamLookup(item.kelas)}|${normalizeExamLookup(item.mapel)}`
      if (mapelPairsByClass.has(keyByClass)) return true

      const meta = parseExamMetaFromSchedule(item)
      const perangkatan = String(meta?.perangkatan || '').trim() || String(item?.kelas || '').trim()
      const mapelBase = getExamMapelBaseLabel(String(meta?.mapel_nama || '').trim()) || getExamMapelBaseLabel(item?.mapel)
      const keyByPerangkatan = `${normalizeExamLookup(mapelBase)}|${normalizeExamLookup(perangkatan)}`
      if (!mapelPairsByPerangkatan.has(keyByPerangkatan)) return false

      const visibleClasses = resolveVisibleClassList({
        item: item,
        distKey: keyByPerangkatan,
        normalizedClassMap: normalizedClassMap,
        normalizeExamLookup: normalizeExamLookup,
        getExamRowClassList: getExamRowClassList
      })
      return visibleClasses.length > 0
    })
  }

  function buildExamRowsFromSchedule({
    filteredRows = [],
    normalizedClassMap,
    normalizeExamLookup,
    parseExamMetaFromSchedule,
    getExamMapelBaseLabel,
    getExamRowClassList,
    getExamRowMapelLabel
  }) {
    const examRows = []
    filteredRows.forEach(item => {
      const meta = parseExamMetaFromSchedule(item)
      const perangkatan = String(meta?.perangkatan || '').trim() || String(item?.kelas || '').trim()
      const mapelBase = getExamMapelBaseLabel(String(meta?.mapel_nama || '').trim()) || getExamMapelBaseLabel(item?.mapel)
      const distKey = `${normalizeExamLookup(mapelBase)}|${normalizeExamLookup(perangkatan)}`
      const kelasList = resolveVisibleClassList({
        item: item,
        distKey: distKey,
        normalizedClassMap: normalizedClassMap,
        normalizeExamLookup: normalizeExamLookup,
        getExamRowClassList: getExamRowClassList
      })
      if (!kelasList.length) {
        return
      }
      kelasList.forEach(kelasNama => {
        examRows.push({
          rowKey: `${String(item.id || '')}|${kelasNama}`,
          jadwal: item,
          kelasNama,
          mapelLabel: getExamRowMapelLabel(item)
        })
      })
    })
    return examRows
  }

  window.guruExamDataUtils = {
    buildExamDistribusiMaps,
    filterExamScheduleRows,
    buildExamRowsFromSchedule
  }
})()
