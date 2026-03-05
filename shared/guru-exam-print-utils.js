;(function initGuruExamPrintUtils() {
  if (window.guruExamPrintUtils) return

  function parseExamInstruksiMeta(value) {
    const raw = String(value || '')
    const marker = raw.match(/^\[\[LANG:(AR|ID)\]\]\s*\n?/i)
    const lang = marker ? String(marker[1] || 'ID').toUpperCase() : 'ID'
    const text = marker ? raw.slice(marker[0].length) : raw
    return {
      lang: lang === 'AR' ? 'AR' : 'ID',
      text: String(text || '').trim()
    }
  }

  function buildExamInstruksiWithMeta(lang, text) {
    const safeLang = String(lang || 'ID').toUpperCase() === 'AR' ? 'AR' : 'ID'
    const body = String(text || '').trim()
    if (!body && safeLang === 'ID') return null
    if (!body) return `[[LANG:${safeLang}]]`
    return `[[LANG:${safeLang}]]\n${body}`
  }

  function getExamPdfStaticText(langCode) {
    const lang = String(langCode || 'ID').toUpperCase()
    if (lang === 'AR') {
      return {
        title: 'Ø£Ø³Ø¦Ù„Ø© Ø§Ù„Ø§Ù…ØªØ­Ø§Ù†',
        jenis: 'Ø§Ù„Ù†ÙˆØ¹',
        namaUjian: 'Ø§Ø³Ù… Ø§Ù„Ø§Ø®ØªØ¨Ø§Ø±',
        kelasMapel: 'Ø§Ù„ØµÙ',
        mapel: 'Ø§Ù„Ù…Ø§Ø¯Ø©',
        tanggalWaktu: 'Ø§Ù„ØªØ§Ø±ÙŠØ®',
        waktu: 'Ø§Ù„ÙˆÙ‚Øª',
        guru: 'Ø§Ù„Ù…Ø¹Ù„Ù…',
        instruksiUmum: 'ØªØ¹Ù„ÙŠÙ…Ø§Øª Ø¹Ø§Ù…Ø©',
        modelSoal: 'Ù†Ù…ÙˆØ°Ø¬ Ø§Ù„Ø£Ø³Ø¦Ù„Ø©'
      }
    }
    return {
      title: 'SOAL UJIAN',
      jenis: 'Jenis',
      namaUjian: 'Nama Ujian',
      kelasMapel: 'Kelas',
      mapel: 'Mapel',
      tanggalWaktu: 'Tanggal',
      waktu: 'Waktu',
      guru: 'Guru',
      instruksiUmum: 'Instruksi Umum',
      modelSoal: 'Model Soal'
    }
  }

  function toArabicIndicDigits(value) {
    const map = ['Ù ', 'Ù¡', 'Ù¢', 'Ù£', 'Ù¤', 'Ù¥', 'Ù¦', 'Ù§', 'Ù¨', 'Ù©']
    return String(value == null ? '' : value).replace(/\d/g, d => map[Number(d)] || d)
  }

  function formatExamNumber(value, langCode = 'ID') {
    const lang = String(langCode || 'ID').toUpperCase()
    return lang === 'AR' ? toArabicIndicDigits(value) : String(value)
  }

  function getExamMarkerSeparator(_langCode = 'ID') {
    return '.'
  }

  function formatExamMarker(token, langCode = 'ID') {
    const lang = String(langCode || 'ID').toUpperCase()
    const body = String(token || '').trim()
    if (!body) return ''
    return lang === 'AR' ? `${body}` : `${body}.`
  }

  function getArabicLetterByIndex(index) {
    const letters = ['Ø£', 'Ø¨', 'Ø¬', 'Ø¯', 'Ù‡Ù€', 'Ùˆ', 'Ø²', 'Ø­', 'Ø·', 'ÙŠ', 'Ùƒ', 'Ù„', 'Ù…', 'Ù†', 'Ø³', 'Ø¹', 'Ù', 'Øµ', 'Ù‚', 'Ø±', 'Ø´', 'Øª', 'Ø«', 'Ø®', 'Ø°', 'Ø¶']
    return letters[Number(index || 0) % letters.length]
  }

  function normalizeExamQuestionType(value, fallbackType = '') {
    const raw = String(value || '').trim().toLowerCase()
    if (raw === 'esai' || raw === 'essay') return 'esai'
    if (raw === 'pilihan-ganda' || raw === 'pilihan ganda' || raw === 'pg') return 'pilihan-ganda'
    if (raw === 'pasangkan-kata' || raw === 'pasangkan kata' || raw === 'matching') return 'pasangkan-kata'
    if (raw === 'isi-titik' || raw === 'isi titik' || raw === 'fill-blank' || raw === 'fill blank') return 'isi-titik'
    const fallback = String(fallbackType || '').trim().toLowerCase()
    if (fallback === 'esai' || fallback === 'essay') return 'esai'
    if (fallback === 'pasangkan-kata' || fallback === 'pasangkan kata' || fallback === 'matching') return 'pasangkan-kata'
    if (fallback === 'isi-titik' || fallback === 'isi titik' || fallback === 'fill-blank' || fallback === 'fill blank') return 'isi-titik'
    return 'pilihan-ganda'
  }

  function buildExamPrintSections(questions, fallbackType = 'pilihan-ganda') {
    const rows = Array.isArray(questions) ? questions : []
    const sections = []
    let currentType = ''
    let currentItems = []
    rows.forEach((item, idx) => {
      const qType = normalizeExamQuestionType(item?.type, fallbackType)
      const numbered = {
        ...item,
        no: Number(item?.no || (idx + 1))
      }
      if (!currentType) {
        currentType = qType
        currentItems.push(numbered)
        return
      }
      if (qType !== currentType) {
        sections.push({ type: currentType, items: currentItems })
        currentType = qType
        currentItems = [numbered]
        return
      }
      currentItems.push(numbered)
    })
    if (currentItems.length) sections.push({ type: currentType || 'pilihan-ganda', items: currentItems })
    return sections
  }

  function getExamPrintTypeParts(type, index, langCode = 'ID') {
    const lang = String(langCode || 'ID').toUpperCase()
    let label = 'Pilihan Ganda'
    if (lang === 'AR') {
      if (type === 'esai') label = 'Ù…Ù‚Ø§Ù„'
      else if (type === 'pasangkan-kata') label = 'ÙˆØµÙ„ Ø§Ù„ÙƒÙ„Ù…Ø§Øª'
      else if (type === 'isi-titik') label = 'Ø§Ù…Ù„Ø£ Ø§Ù„ÙØ±Ø§Øº'
      else label = 'Ø§Ø®ØªÙŠØ§Ø± Ù…Ù† Ù…ØªØ¹Ø¯Ø¯'
    } else {
      if (type === 'esai') label = 'Esai'
      else if (type === 'pasangkan-kata') label = 'Pasangkan Kata'
      else if (type === 'isi-titik') label = 'Isi Titik Kosong'
    }
    const code = lang === 'AR' ? getArabicLetterByIndex(index) : String.fromCharCode(65 + (Number(index || 0) % 26))
    return {
      marker: formatExamMarker(code, lang),
      label
    }
  }

  function getExamPrintTypeTitle(type, index, langCode = 'ID') {
    const parts = getExamPrintTypeParts(type, index, langCode)
    return `${parts.marker} ${parts.label}`
  }

  function getExamPrintTypeInstruction(type, langCode = 'ID') {
    const lang = String(langCode || 'ID').toUpperCase()
    if (lang === 'AR') {
      if (type === 'esai') return 'Ø£Ø¬Ø¨ Ø¹Ù† Ø§Ù„Ø£Ø³Ø¦Ù„Ø© Ø§Ù„ØªØ§Ù„ÙŠØ© Ø¨ÙˆØ¶ÙˆØ­ ÙˆØµØ­Ø©.'
      if (type === 'pasangkan-kata') return 'ØµÙÙ„ ÙƒÙ„Ù…Ø§Øª Ø§Ù„Ø¹Ù…ÙˆØ¯ (Ø£) Ø¨Ù…Ø§ ÙŠÙ†Ø§Ø³Ø¨Ù‡Ø§ ÙÙŠ Ø§Ù„Ø¹Ù…ÙˆØ¯ (Ø¨).'
      if (type === 'isi-titik') return 'Ø£ÙƒÙ…Ù„ Ø§Ù„ÙØ±Ø§Øº Ø¨Ø§Ù„ÙƒÙ„Ù…Ø© Ø§Ù„Ù…Ù†Ø§Ø³Ø¨Ø© Ù…Ù† Ø§Ù„ÙƒÙ„Ù…Ø§Øª Ø§Ù„Ù…Ø¹Ø·Ø§Ø©.'
      return 'Ø§Ø®ØªØ± Ø¥Ø¬Ø§Ø¨Ø© ÙˆØ§Ø­Ø¯Ø© ØµØ­ÙŠØ­Ø©.'
    }
    if (type === 'esai') return 'Jawablah soal berikut dengan jelas dan benar.'
    if (type === 'pasangkan-kata') return 'Pasangkan kata pada baris A dengan pasangan yang tepat pada baris B.'
    if (type === 'isi-titik') return 'Lengkapi bagian yang kosong dengan penggalan kata yang disediakan.'
    return 'Pilihlah satu jawaban yang paling tepat.'
  }

  function deriveExamSectionsFromQuestions(questions, fallbackType = 'pilihan-ganda', totalCount = 0) {
    const rows = Array.isArray(questions) ? questions : []
    let maxNo = 0
    rows.forEach((item, idx) => {
      const no = Number(item?.no || (idx + 1))
      if (Number.isFinite(no) && no > maxNo) maxNo = no
    })
    const safeCount = Number.isFinite(totalCount) ? Math.max(1, Math.min(200, Math.round(totalCount))) : Math.max(1, maxNo || 1)
    const typeMap = new Array(safeCount + 1).fill(normalizeExamQuestionType('', fallbackType))
    rows.forEach((item, idx) => {
      const no = Number(item?.no || (idx + 1))
      if (!Number.isFinite(no) || no <= 0 || no > safeCount) return
      typeMap[no] = normalizeExamQuestionType(item?.type, fallbackType)
    })
    const sections = []
    let start = 1
    let current = typeMap[1]
    for (let i = 2; i <= safeCount; i += 1) {
      if (typeMap[i] === current) continue
      const segmentItems = rows.filter((item, idx) => {
        const no = Number(item?.no || (idx + 1))
        return no >= start && no <= (i - 1)
      })
      const fragSet = new Set()
      segmentItems.forEach(item => {
        const frags = Array.isArray(item?.fragments) ? item.fragments : []
        frags.forEach(f => {
          const txt = String(f || '').trim()
          if (txt) fragSet.add(txt)
        })
      })
      sections.push({ type: current, start, end: i - 1, wordPool: [...fragSet].join(', '), blankCount: (i - 1) - start + 1 })
      start = i
      current = typeMap[i]
    }
    {
      const segmentItems = rows.filter((item, idx) => {
        const no = Number(item?.no || (idx + 1))
        return no >= start && no <= safeCount
      })
      const fragSet = new Set()
      segmentItems.forEach(item => {
        const frags = Array.isArray(item?.fragments) ? item.fragments : []
        frags.forEach(f => {
          const txt = String(f || '').trim()
          if (txt) fragSet.add(txt)
        })
      })
      sections.push({ type: current, start, end: safeCount, wordPool: [...fragSet].join(', '), blankCount: safeCount - start + 1 })
    }
    return sections
  }

  window.guruExamPrintUtils = {
    parseExamInstruksiMeta,
    buildExamInstruksiWithMeta,
    getExamPdfStaticText,
    formatExamNumber,
    getExamMarkerSeparator,
    formatExamMarker,
    getArabicLetterByIndex,
    buildExamPrintSections,
    getExamPrintTypeTitle,
    getExamPrintTypeParts,
    getExamPrintTypeInstruction,
    normalizeExamQuestionType,
    deriveExamSectionsFromQuestions
  }
})()
