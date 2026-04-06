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
        title: 'أسئلة الامتحان',
        jenis: 'النوع',
        namaUjian: 'اسم الاختبار',
        kelasMapel: 'الصف',
        mapel: 'المادة',
        tanggalWaktu: 'التاريخ',
        waktu: 'الوقت',
        guru: 'المعلم',
        instruksiUmum: 'تعليمات عامة',
        modelSoal: 'نموذج الأسئلة'
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
    const map = ['٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩']
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
    const letters = ['أ', 'ب', 'ج', 'د', 'هـ', 'و', 'ز', 'ح', 'ط', 'ي', 'ك', 'ل', 'م', 'ن', 'س', 'ع', 'ف', 'ص', 'ق', 'ر', 'ش', 'ت', 'ث', 'خ', 'ذ', 'ض']
    return letters[Number(index || 0) % letters.length]
  }

  function normalizeExamQuestionType(value, fallbackType = '') {
    const raw = String(value || '').trim().toLowerCase()
    if (raw === 'esai' || raw === 'essay') return 'esai'
    if (raw === 'pilihan-ganda' || raw === 'pilihan ganda' || raw === 'pg') return 'pilihan-ganda'
    if (raw === 'benar-salah' || raw === 'benar salah' || raw === 'true-false' || raw === 'true false' || raw === 'bs') return 'benar-salah'
    if (raw === 'cari-kata' || raw === 'cari kata' || raw === 'word-search' || raw === 'word search' || raw === 'teka-kata') return 'cari-kata'
    if (raw === 'teka-silang' || raw === 'teka silang' || raw === 'tts' || raw === 'crossword') return 'teka-silang'
    if (raw === 'pasangkan-kata' || raw === 'pasangkan kata' || raw === 'matching') return 'pasangkan-kata'
    if (raw === 'isi-titik' || raw === 'isi titik' || raw === 'fill-blank' || raw === 'fill blank') return 'isi-titik'
    const fallback = String(fallbackType || '').trim().toLowerCase()
    if (fallback === 'esai' || fallback === 'essay') return 'esai'
    if (fallback === 'benar-salah' || fallback === 'benar salah' || fallback === 'true-false' || fallback === 'true false' || fallback === 'bs') return 'benar-salah'
    if (fallback === 'cari-kata' || fallback === 'cari kata' || fallback === 'word-search' || fallback === 'word search' || fallback === 'teka-kata') return 'cari-kata'
    if (fallback === 'teka-silang' || fallback === 'teka silang' || fallback === 'tts' || fallback === 'crossword') return 'teka-silang'
    if (fallback === 'pasangkan-kata' || fallback === 'pasangkan kata' || fallback === 'matching') return 'pasangkan-kata'
    if (fallback === 'isi-titik' || fallback === 'isi titik' || fallback === 'fill-blank' || fallback === 'fill blank') return 'isi-titik'
    return 'pilihan-ganda'
  }

  function buildExamPrintSections(questions, fallbackType = 'pilihan-ganda') {
    const rows = Array.isArray(questions) ? questions : []
    const sections = []
    let currentType = ''
    let currentItems = []
    let currentSectionKey = ''
    let currentInstruction = ''
    rows.forEach((item, idx) => {
      const qType = normalizeExamQuestionType(item?.type, fallbackType)
      const sectionKey = String(item?.sectionKey || item?.section_key || '').trim()
      const sectionInstruction = String(item?.sectionInstruction || item?.section_instruction || '').trim()
      const numbered = {
        ...item,
        no: Number(item?.no || (idx + 1))
      }
      if (!currentType) {
        currentType = qType
        currentSectionKey = sectionKey
        currentInstruction = sectionInstruction
        currentItems.push(numbered)
        return
      }
      const sectionChanged = sectionKey
        ? sectionKey !== currentSectionKey
        : (Boolean(currentSectionKey) || qType !== currentType)
      if (sectionChanged || qType !== currentType) {
        const firstItem = currentItems[0] || {}
        sections.push({
          type: currentType,
          items: currentItems,
          instruction: currentInstruction,
          score: firstItem?.sectionScore ?? firstItem?.section_score ?? null
        })
        currentType = qType
        currentSectionKey = sectionKey
        currentInstruction = sectionInstruction
        currentItems = [numbered]
        return
      }
      currentItems.push(numbered)
    })
    if (currentItems.length) {
      const firstItem = currentItems[0] || {}
      sections.push({
        type: currentType || 'pilihan-ganda',
        items: currentItems,
        instruction: currentInstruction,
        score: firstItem?.sectionScore ?? firstItem?.section_score ?? null
      })
    }
    return sections
  }

  function buildExamMatchingColumns(items) {
    const columnA = []
    const columnB = []
    ;(Array.isArray(items) ? items : []).forEach(item => {
      const leftList = Array.isArray(item?.columnA)
        ? item.columnA
        : (Array.isArray(item?.pairs) ? item.pairs.map(pair => String(pair?.a || '').trim()) : [])
      const rightList = Array.isArray(item?.columnB)
        ? item.columnB
        : (Array.isArray(item?.pairs) ? item.pairs.map(pair => String(pair?.b || '').trim()) : [])
      leftList.forEach(entry => {
        const value = String(entry || '').trim()
        if (value) columnA.push(value)
      })
      rightList.forEach(entry => {
        const value = String(entry || '').trim()
        if (value) columnB.push(value)
      })
    })
    return { columnA, columnB }
  }

  function getExamPrintTypeParts(type, index, langCode = 'ID') {
    const lang = String(langCode || 'ID').toUpperCase()
    let label = 'Pilihan Ganda'
    if (lang === 'AR') {
      if (type === 'esai') label = 'مقال'
      else if (type === 'benar-salah') label = 'الصحيح أو الخطأ'
      else if (type === 'cari-kata') label = 'ابحث عن الكلمات'
      else if (type === 'teka-silang') label = 'الكلمات المتقاطعة'
      else if (type === 'pasangkan-kata') label = 'وصل الكلمات'
      else if (type === 'isi-titik') label = 'املأ الفراغ'
      else label = 'اختيار من متعدد'
    } else {
      if (type === 'esai') label = 'Esai'
      else if (type === 'benar-salah') label = 'Pernyataan Benar atau Salah'
      else if (type === 'cari-kata') label = 'Cari Kata'
      else if (type === 'teka-silang') label = 'Teka-Teki Silang'
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
      if (type === 'esai') return 'أجب عن الأسئلة التالية بوضوح وصحة.'
      if (type === 'benar-salah') return 'حدد هل العبارة التالية صحيحة أم خاطئة.'
      if (type === 'cari-kata') return 'ابحث عن الكلمات المطلوبة في الشبكة، ثم ظللها أو ضع دائرة حولها.'
      if (type === 'teka-silang') return 'املأ الشبكة اعتمادًا على القرائن الأفقية والعمودية.'
      if (type === 'pasangkan-kata') return 'صل كلمات العمود (أ) بما يناسبها في العمود (ب).'
      if (type === 'isi-titik') return 'أكمل الفراغ بالكلمة المناسبة من الكلمات المعطاة.'
      return 'اختر إجابة واحدة صحيحة.'
    }
    if (type === 'esai') return 'Jawablah soal berikut dengan jelas dan benar.'
    if (type === 'benar-salah') return 'Tentukan apakah pernyataan berikut benar atau salah.'
    if (type === 'cari-kata') return 'Temukan kata-kata berikut di dalam kotak huruf, lalu tandai dengan jelas.'
    if (type === 'teka-silang') return 'Lengkapi teka-teki silang berdasarkan petunjuk mendatar dan menurun.'
    if (type === 'pasangkan-kata') return 'Pasangkan kata pada baris A dengan pasangan yang tepat pada baris B.'
    if (type === 'isi-titik') return 'Lengkapi bagian yang kosong dengan penggalan kata yang disediakan.'
    return 'Pilihlah satu jawaban yang paling tepat.'
  }

  function deriveExamSectionsFromQuestions(questions, fallbackType = 'pilihan-ganda', totalCount = 0) {
    const rows = Array.isArray(questions) ? questions : []
    const hasSectionKeys = rows.some(item => String(item?.sectionKey || item?.section_key || '').trim())
    if (hasSectionKeys) {
      const orderedRows = rows
        .map((item, idx) => ({
          ...item,
          no: Number(item?.no || (idx + 1))
        }))
        .filter(item => Number.isFinite(item.no) && item.no > 0)
        .sort((a, b) => a.no - b.no)
      const sections = []
      let currentItems = []
      let currentKey = ''
      orderedRows.forEach(item => {
        const key = String(item?.sectionKey || item?.section_key || `auto-${item.no}`).trim()
        if (!currentItems.length) {
          currentKey = key
          currentItems = [item]
          return
        }
        if (key !== currentKey) {
          const first = currentItems[0] || {}
          const last = currentItems[currentItems.length - 1] || first
          const fragSet = new Set()
          currentItems.forEach(entry => {
            const frags = Array.isArray(entry?.fragments) ? entry.fragments : []
            frags.forEach(f => {
              const txt = String(f || '').trim()
              if (txt) fragSet.add(txt)
            })
          })
          sections.push({
            type: normalizeExamQuestionType(first?.type, fallbackType),
            start: Number(first?.no || 1),
            end: Number(last?.no || first?.no || 1),
            wordPool: [...fragSet].join(', '),
            blankCount: Math.max(1, Number(last?.no || first?.no || 1) - Number(first?.no || 1) + 1),
            instruction: String(first?.sectionInstruction || first?.section_instruction || '').trim(),
            score: first?.sectionScore ?? first?.section_score ?? null
          })
          currentKey = key
          currentItems = [item]
          return
        }
        currentItems.push(item)
      })
      if (currentItems.length) {
        const first = currentItems[0] || {}
        const last = currentItems[currentItems.length - 1] || first
        const fragSet = new Set()
        currentItems.forEach(entry => {
          const frags = Array.isArray(entry?.fragments) ? entry.fragments : []
          frags.forEach(f => {
            const txt = String(f || '').trim()
            if (txt) fragSet.add(txt)
          })
        })
        sections.push({
          type: normalizeExamQuestionType(first?.type, fallbackType),
          start: Number(first?.no || 1),
          end: Number(last?.no || first?.no || 1),
          wordPool: [...fragSet].join(', '),
          blankCount: Math.max(1, Number(last?.no || first?.no || 1) - Number(first?.no || 1) + 1),
          instruction: String(first?.sectionInstruction || first?.section_instruction || '').trim(),
          score: first?.sectionScore ?? first?.section_score ?? null
        })
      }
      return sections
    }
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
      const firstItem = segmentItems[0] || {}
      sections.push({
        type: current,
        start,
        end: i - 1,
        wordPool: [...fragSet].join(', '),
        blankCount: (i - 1) - start + 1,
        instruction: String(firstItem?.sectionInstruction || firstItem?.section_instruction || '').trim(),
        score: firstItem?.sectionScore ?? firstItem?.section_score ?? null
      })
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
      const firstItem = segmentItems[0] || {}
      sections.push({
        type: current,
        start,
        end: safeCount,
        wordPool: [...fragSet].join(', '),
        blankCount: safeCount - start + 1,
        instruction: String(firstItem?.sectionInstruction || firstItem?.section_instruction || '').trim(),
        score: firstItem?.sectionScore ?? firstItem?.section_score ?? null
      })
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
    buildExamMatchingColumns,
    normalizeExamQuestionType,
    deriveExamSectionsFromQuestions
  }
})()
