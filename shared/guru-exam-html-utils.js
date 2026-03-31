;(function initGuruExamHtmlUtils() {
  if (window.guruExamHtmlUtils) return

  const ARABIC_INLINE_RE = /[\u0600-\u06FF\u0750-\u077F\u0870-\u08FF\uFB50-\uFDFF\uFE70-\uFEFF]/u
  const ARABIC_INLINE_RUN_RE = /[\u0600-\u06FF\u0750-\u077F\u0870-\u08FF\uFB50-\uFDFF\uFE70-\uFEFF]+/gu
  const ARABIC_MOJIBAKE_RE = /[ØÙ][\u0080-\u00BF]/u

  function repairArabicMojibake(text) {
    const source = String(text || '')
    if (!source || !ARABIC_MOJIBAKE_RE.test(source) || typeof TextDecoder !== 'function') return source
    try {
      const bytes = Uint8Array.from(Array.from(source).map(ch => ch.charCodeAt(0) & 0xFF))
      const decoded = new TextDecoder('utf-8', { fatal: false }).decode(bytes)
      if (ARABIC_INLINE_RE.test(decoded)) return decoded
    } catch (_err) {}
    return source
  }

  function hasInlineArabic(text) {
    return ARABIC_INLINE_RE.test(repairArabicMojibake(text))
  }

  function tokenizeMixedInlineText(text) {
    const source = repairArabicMojibake(text)
    if (!source) return []
    const tokens = []
    let lastIndex = 0
    ARABIC_INLINE_RUN_RE.lastIndex = 0
    let match = ARABIC_INLINE_RUN_RE.exec(source)
    while (match) {
      if (match.index > lastIndex) {
        tokens.push({ kind: 'latin', text: source.slice(lastIndex, match.index) })
      }
      tokens.push({ kind: 'arabic', text: match[0] })
      lastIndex = match.index + match[0].length
      match = ARABIC_INLINE_RUN_RE.exec(source)
    }
    if (lastIndex < source.length) {
      tokens.push({ kind: 'latin', text: source.slice(lastIndex) })
    }
    return tokens
  }

  function renderExamInlineTextHtml(text, escapeHtml, forceArabic = false) {
    const source = repairArabicMojibake(text)
    if (!source) return ''
    if (forceArabic || !hasInlineArabic(source)) return escapeHtml(source)
    return tokenizeMixedInlineText(source)
      .map(token => (token.kind === 'arabic'
        ? `<span class="mixed-ar">${escapeHtml(token.text)}</span>`
        : escapeHtml(token.text)))
      .join('')
  }

  function buildExamIsiTitikExtraLineHtml({ items, isAr, escapeHtml, renderText }) {
    const fragSet = new Set()
    ;(items || []).forEach(item => {
      const frags = Array.isArray(item?.fragments) ? item.fragments : []
      frags.forEach(f => {
        const txt = String(f || '').trim()
        if (txt) fragSet.add(txt)
      })
    })
    const list = [...fragSet]
    if (!list.length) return ''
    const label = isAr ? 'خيارات الكلمات' : 'Pilihan kata'
    return `<p><strong>${escapeHtml(label)}:</strong> (${renderText(list.join(', '), isAr)})</p>`
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

  function getArabicLetterByIndexLocal(index) {
    const letters = ['أ', 'ب', 'ج', 'د', 'هـ', 'و', 'ز', 'ح', 'ط', 'ي', 'ك', 'ل', 'م', 'ن', 'س', 'ع', 'ف', 'ص', 'ق', 'ر', 'ش', 'ت', 'ث', 'خ', 'ذ', 'ض']
    return letters[Number(index || 0) % letters.length]
  }

  function buildExamBrowserQuestionTitleHtml({ q, no, lang, isAr, escapeHtml, formatExamMarker, formatExamNumber, renderText }) {
    return `<div class="q-title">${escapeHtml(formatExamMarker(formatExamNumber(no, lang), lang))} ${renderText(String(q?.text || '-'), isAr)}</div>`
  }

  function getExamPgGridClass(opts, isAr) {
    const values = ['a', 'b', 'c', 'd'].map(key => String(opts?.[key] || '-').trim())
    const maxLen = values.reduce((max, item) => Math.max(max, item.length), 0)
    if (maxLen <= (isAr ? 10 : 12)) return 'pg-grid-4'
    if (maxLen <= (isAr ? 22 : 24)) return 'pg-grid-2'
    return 'pg-grid-1'
  }

  function buildExamBrowserPgQuestionHtml({ q, qText, lang, isAr, escapeHtml, formatExamMarker, renderText }) {
    const opts = q?.options || {}
    const mA = isAr ? 'أ' : 'a'
    const mB = isAr ? 'ب' : 'b'
    const mC = isAr ? 'ج' : 'c'
    const mD = isAr ? 'د' : 'd'
    const gridClass = getExamPgGridClass(opts, isAr)
    return `
      <li>
        ${qText}
        <div class="pg-grid ${gridClass}">
          <div>${escapeHtml(formatExamMarker(mA, lang))} ${renderText(String(opts.a || '-'), isAr)}</div>
          <div>${escapeHtml(formatExamMarker(mB, lang))} ${renderText(String(opts.b || '-'), isAr)}</div>
          <div>${escapeHtml(formatExamMarker(mC, lang))} ${renderText(String(opts.c || '-'), isAr)}</div>
          <div>${escapeHtml(formatExamMarker(mD, lang))} ${renderText(String(opts.d || '-'), isAr)}</div>
        </div>
      </li>
    `
  }

  function buildExamBrowserPairQuestionHtml({ q, qText, lang, isAr, escapeHtml, formatExamMarker, formatExamNumber, getArabicLetterByIndex, renderText }) {
    const pairs = Array.isArray(q?.pairs) ? q.pairs : []
    const rows = pairs.map((pair, i) => `
      <tr>
        <td>${escapeHtml(formatExamMarker(formatExamNumber(i + 1, lang), lang))} ${renderText(String(pair?.a || '-'), isAr)}</td>
        <td>${escapeHtml(formatExamMarker((isAr ? getArabicLetterByIndex(i) : String.fromCharCode(65 + i)), lang))} ${renderText(String(pair?.b || '-'), isAr)}</td>
      </tr>
    `).join('')
    return `
      <li>
        ${qText}
        <table class="pair-table">
          <thead><tr><th>${isAr ? 'العمود أ' : 'Baris A'}</th><th>${isAr ? 'العمود ب' : 'Baris B'}</th></tr></thead>
          <tbody>${rows}</tbody>
        </table>
      </li>
    `
  }

  function buildExamBrowserQuestionHtml({
    section,
    q,
    idx,
    lang,
    isAr,
    escapeHtml,
    formatExamMarker,
    formatExamNumber,
    getArabicLetterByIndex,
    renderText
  }) {
    const no = idx + 1
    const qText = buildExamBrowserQuestionTitleHtml({ q, no, lang, isAr, escapeHtml, formatExamMarker, formatExamNumber, renderText })
    if (section.type === 'pilihan-ganda') return buildExamBrowserPgQuestionHtml({ q, qText, lang, isAr, escapeHtml, formatExamMarker, renderText })
    if (section.type === 'pasangkan-kata') return buildExamBrowserPairQuestionHtml({ q, qText, lang, isAr, escapeHtml, formatExamMarker, formatExamNumber, getArabicLetterByIndex, renderText })
    return `<li>${qText}</li>`
  }

  function buildExamBrowserSectionHtml({
    section,
    sectionIndex,
    lang,
    isAr,
    escapeHtml,
    getExamPrintTypeParts,
    formatExamMarker,
    formatExamNumber,
    getArabicLetterByIndex
  }) {
    const headingParts = getExamPrintTypeParts(section.type, sectionIndex, lang)
    const instruksiModel = String(section?.instruction || '').trim()
    const items = section.items || []
    const renderText = (value, forceArabic = isAr) => renderExamInlineTextHtml(value, escapeHtml, forceArabic)
    if (section.type === 'pasangkan-kata') {
      const matchingCols = buildExamMatchingColumns(items)
      const maxRows = Math.max(matchingCols.columnA.length, matchingCols.columnB.length)
      const rowsHtml = Array.from({ length: maxRows }, (_item, idx) => `
        <tr>
          <td>${renderText(String(matchingCols.columnA[idx] || '-'))}</td>
          <td>${renderText(String(matchingCols.columnB[idx] || '-'))}</td>
        </tr>
      `).join('')
      return `
        <section class="sec">
          <h3><strong>${escapeHtml(headingParts.marker)}</strong> ${renderText(headingParts.label)}</h3>
          ${instruksiModel ? `<p>${renderText(instruksiModel)}</p>` : ''}
          <table class="pair-table">
            <thead><tr><th>${isAr ? 'القائمة أ' : 'Qoimah A'}</th><th>${isAr ? 'القائمة ب' : 'Qoimah B'}</th></tr></thead>
            <tbody>${rowsHtml}</tbody>
          </table>
        </section>
      `
    }
    const extraLine = section.type === 'isi-titik'
      ? buildExamIsiTitikExtraLineHtml({ items, isAr, escapeHtml, renderText })
      : ''
    const questionsHtml = items.map((q, idx) => buildExamBrowserQuestionHtml({
      section,
      q,
      idx,
      lang,
      isAr,
      escapeHtml,
      formatExamMarker,
      formatExamNumber,
      getArabicLetterByIndex,
      renderText
    })).join('')
    return `
      <section class="sec">
        <h3><strong>${escapeHtml(headingParts.marker)}</strong> ${renderText(headingParts.label)}</h3>
        ${instruksiModel ? `<p>${renderText(instruksiModel)}</p>` : ''}
        ${extraLine}
        <ol>${questionsHtml}</ol>
      </section>
    `
  }

  function buildExamBrowserSectionsHtml({
    sections,
    lang,
    isAr,
    escapeHtml,
    getExamPrintTypeParts,
    getExamPrintTypeInstruction,
    formatExamMarker,
    formatExamNumber,
    getArabicLetterByIndex
  }) {
    return (sections || []).map((section, sectionIndex) => buildExamBrowserSectionHtml({
      section,
      sectionIndex,
      lang,
      isAr,
      escapeHtml,
      getExamPrintTypeParts,
      formatExamMarker,
      formatExamNumber,
      getArabicLetterByIndex
    })).join('')
  }

  function buildExamBrowserDocumentHtml({ jadwal, soal, textMap, isAr, instruksiUmum, sectionHtml, escapeHtml, toTimeLabel }) {
    const bodyFontFamily = isAr ? "'Traditional Arabic','Times New Roman',serif" : "'Times New Roman',serif"
    const contentFontSize = isAr ? '16pt' : '14px'
    const pairFontSize = isAr ? '16pt' : '13px'
    return `
<!doctype html>
<html lang="${isAr ? 'ar' : 'id'}" dir="${isAr ? 'rtl' : 'ltr'}">
<head>
  <meta charset="utf-8">
  <title>${escapeHtml(textMap.title)}</title>
  <style>
    body { font-family: ${bodyFontFamily}; margin: 20px; color: #111; }
    h1 { text-align: center; font-size: 22px; margin: 0 0 12px 0; }
    .meta p { margin: 4px 0; font-size: 14px; }
    .sec { margin-top: 14px; }
    .sec h3 { margin: 0 0 6px 0; font-size: 16px; }
    .sec p { margin: 4px 0; font-size: ${contentFontSize}; }
    ol { margin: 6px 0 0 0; padding-${isAr ? 'right' : 'left'}: 20px; }
    li { margin: 8px 0; font-size: ${contentFontSize}; }
    .q-title { margin-bottom: 4px; font-size: ${contentFontSize}; }
    .pg-grid { display: grid; gap: 2px 18px; margin-${isAr ? 'right' : 'left'}: 20px; }
    .pg-grid.pg-grid-4 { grid-template-columns: repeat(4, minmax(0, 1fr)); }
    .pg-grid.pg-grid-2 { grid-template-columns: repeat(2, minmax(0, 1fr)); }
    .pg-grid.pg-grid-1 { grid-template-columns: minmax(0, 1fr); }
    .pg-grid > div { font-size: ${contentFontSize}; }
    .pair-table { border-collapse: collapse; width: 100%; margin-top: 4px; }
    .pair-table th, .pair-table td { border: 1px solid #999; padding: 4px 6px; font-size: ${pairFontSize}; text-align: ${isAr ? 'right' : 'left'}; }
    .mixed-ar { display:inline-block; direction:rtl; unicode-bidi:isolate-override; white-space:pre-wrap; font-family:'Traditional Arabic','Times New Roman',serif; font-size:1.15em; line-height:1; }
    @media print { body { margin: 10mm; } }
  </style>
</head>
<body>
  <h1>${escapeHtml(textMap.title)}</h1>
  <div class="meta">
    <p><strong>${escapeHtml(textMap.jenis)}:</strong> ${escapeHtml(String(jadwal?.jenis || '-'))}</p>
    <p><strong>${escapeHtml(textMap.namaUjian)}:</strong> ${escapeHtml(String(jadwal?.nama || '-'))}</p>
    <p><strong>${escapeHtml(textMap.kelasMapel)}:</strong> ${escapeHtml(String(jadwal?.kelas || '-'))} | <strong>${escapeHtml(textMap.mapel)}:</strong> ${escapeHtml(String(jadwal?.mapel || '-'))}</p>
    <p><strong>${escapeHtml(textMap.tanggalWaktu)}:</strong> ${escapeHtml(String(jadwal?.tanggal || '-'))} | <strong>${escapeHtml(textMap.waktu)}:</strong> ${escapeHtml(`${toTimeLabel(jadwal?.jam_mulai)} - ${toTimeLabel(jadwal?.jam_selesai)}`)}</p>
    <p><strong>${escapeHtml(textMap.guru)}:</strong> ${escapeHtml(String(soal?.guru_nama || '-'))}</p>
    ${instruksiUmum}
  </div>
  ${sectionHtml}
</body>
</html>`
  }

  function createExamWordMarkerFormatter({ lang, isAr, formatExamMarker, escapeHtml }) {
    return token => {
      const marker = formatExamMarker(token, lang)
      if (!isAr) return escapeHtml(marker)
      return `<span class="ar-marker">${escapeHtml(marker)}</span>`
    }
  }

  function buildExamWordQuestionHtml({ section, q, idx, lang, isAr, markerHtml, escapeHtml }) {
    const renderText = value => renderExamInlineTextHtml(value, escapeHtml, isAr)
    const no = idx + 1
    const qText = `<div class="q-title">${markerHtml(formatExamNumber(no, lang))} ${renderText(String(q?.text || '-'))}</div>`
    if (section.type === 'pilihan-ganda') {
      const opts = q?.options || {}
      const mA = isAr ? 'أ' : 'a'
      const mB = isAr ? 'ب' : 'b'
      const mC = isAr ? 'ج' : 'c'
      const mD = isAr ? 'د' : 'd'
      const gridClass = getExamPgGridClass(opts, isAr)
      return `<li>${qText}<div class="pg-grid ${gridClass}">
        <div>${markerHtml(mA)} ${renderText(String(opts.a || '-'))}</div>
        <div>${markerHtml(mB)} ${renderText(String(opts.b || '-'))}</div>
        <div>${markerHtml(mC)} ${renderText(String(opts.c || '-'))}</div>
        <div>${markerHtml(mD)} ${renderText(String(opts.d || '-'))}</div>
      </div></li>`
    }
    return `<li>${qText}</li>`
  }

  function buildExamWordSectionHtml({ section, sectionIndex, lang, isAr, markerHtml, escapeHtml, getExamPrintTypeParts }) {
    const headingParts = getExamPrintTypeParts(section.type, sectionIndex, lang)
    const instruksiModel = String(section?.instruction || '').trim()
    const items = section.items || []
    const renderText = value => renderExamInlineTextHtml(value, escapeHtml, isAr)
    if (section.type === 'pasangkan-kata') {
      const matchingCols = buildExamMatchingColumns(items)
      const maxRows = Math.max(matchingCols.columnA.length, matchingCols.columnB.length)
      const rowsHtml = Array.from({ length: maxRows }, (_item, idx) => `
        <tr>
          <td>${renderText(String(matchingCols.columnA[idx] || '-'))}</td>
          <td>${renderText(String(matchingCols.columnB[idx] || '-'))}</td>
        </tr>
      `).join('')
      return `<section class="sec"><h3><strong>${isAr ? `<span class="ar-marker">${escapeHtml(headingParts.marker)}</span>` : escapeHtml(headingParts.marker)}</strong> ${renderText(headingParts.label)}</h3>${instruksiModel ? `<p>${renderText(instruksiModel)}</p>` : ''}<table class="pair-table"><thead><tr><th>${isAr ? 'القائمة أ' : 'Qoimah A'}</th><th>${isAr ? 'القائمة ب' : 'Qoimah B'}</th></tr></thead><tbody>${rowsHtml}</tbody></table></section>`
    }
    const questionsHtml = items.map((q, idx) => buildExamWordQuestionHtml({
      section,
      q,
      idx,
      lang,
      isAr,
      markerHtml,
      escapeHtml
    })).join('')
    return `<section class="sec"><h3><strong>${isAr ? `<span class="ar-marker">${escapeHtml(headingParts.marker)}</span>` : escapeHtml(headingParts.marker)}</strong> ${renderText(headingParts.label)}</h3>${instruksiModel ? `<p>${renderText(instruksiModel)}</p>` : ''}<ol>${questionsHtml}</ol></section>`
  }

  function buildExamWordSectionsHtml({ sections, lang, isAr, markerHtml, escapeHtml, getExamPrintTypeParts }) {
    return (sections || []).map((section, sectionIndex) => buildExamWordSectionHtml({
      section,
      sectionIndex,
      lang,
      isAr,
      markerHtml,
      escapeHtml,
      getExamPrintTypeParts
    })).join('')
  }

  function buildExamWordDocumentHtml({
    jadwal,
    soal,
    textMap,
    isAr,
    bgDataUrl,
    wordFontFamily,
    wordDirectionCss,
    wordBidiCss,
    instruksiUmum,
    sectionHtml,
    escapeHtml,
    toTimeLabel
  }) {
    const contentFontSize = isAr ? '16pt' : '14px'
    const pairFontSize = isAr ? '16pt' : '13px'
    return `<!doctype html>
<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:w="urn:schemas-microsoft-com:office:word" xmlns:v="urn:schemas-microsoft-com:vml" lang="${isAr ? 'ar' : 'id'}" dir="${isAr ? 'rtl' : 'ltr'}">
<head><meta charset="utf-8"><title>${escapeHtml(textMap.title)}</title>
<style>
@page { size: A4; margin: 15mm; }
body { font-family: ${wordFontFamily}; ${wordBidiCss} ${wordDirectionCss} margin: 0; padding: 0; color: #111; }
.doc-wrap { position: relative; width: 210mm; min-height: 297mm; box-sizing: border-box; padding: 15mm; }
.page-bg { position: absolute; top: 0; left: 0; width: 210mm; height: 297mm; z-index: 0; pointer-events: none; }
.page-bg img { display: block; width: 100%; height: 100%; object-fit: cover; }
.doc-content { position: relative; z-index: 1; }
h1 { text-align: center; font-size: 22px; margin: 0 0 12px 0; }
.meta p { margin: 4px 0; font-size: 14px; }
.sec { margin-top: 14px; }
.sec h3 { margin: 0 0 6px 0; font-size: 16px; }
.sec p { margin: 4px 0; font-size: ${contentFontSize}; }
ol { margin: 6px 0 0 0; padding-${isAr ? 'right' : 'left'}: 20px; }
li { margin: 8px 0; font-size: ${contentFontSize}; }
.q-title { margin-bottom: 4px; font-size: ${contentFontSize}; }
.pg-grid { display: grid; gap: 2px 18px; margin-${isAr ? 'right' : 'left'}: 20px; }
.pg-grid.pg-grid-4 { grid-template-columns: repeat(4, minmax(0, 1fr)); }
.pg-grid.pg-grid-2 { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.pg-grid.pg-grid-1 { grid-template-columns: minmax(0, 1fr); }
.pg-grid > div { font-size: ${contentFontSize}; }
.pair-table { border-collapse: collapse; width: 100%; margin-top: 4px; }
.pair-table th, .pair-table td { border: 1px solid #999; padding: 4px 6px; font-size: ${pairFontSize}; text-align: ${isAr ? 'right' : 'left'}; }
.ar-marker { display:inline-block; white-space:nowrap; direction:rtl; unicode-bidi:isolate-override; min-width:20px; }
.mixed-ar { display:inline-block; direction:rtl; unicode-bidi:isolate-override; white-space:pre-wrap; font-family:'Traditional Arabic','Times New Roman',serif; font-size:1.15em; line-height:1; }
* { font-family: ${wordFontFamily}; ${wordBidiCss} }
</style></head>
<body>
${bgDataUrl ? `<!--[if gte mso 9]><v:background id="bg" o:bwmode="white"><v:fill type="frame" src="${bgDataUrl}" /></v:background><![endif]-->` : ''}
<div class="doc-wrap">
${bgDataUrl ? `<div class="page-bg"><img src="${bgDataUrl}" alt=""></div>` : ''}
<div class="doc-content">
<h1>${escapeHtml(textMap.title)}</h1>
<div class="meta">
<p><strong>${escapeHtml(textMap.jenis)}:</strong> ${escapeHtml(String(jadwal?.jenis || '-'))}</p>
<p><strong>${escapeHtml(textMap.namaUjian)}:</strong> ${escapeHtml(String(jadwal?.nama || '-'))}</p>
<p><strong>${escapeHtml(textMap.kelasMapel)}:</strong> ${escapeHtml(String(jadwal?.kelas || '-'))} | <strong>${escapeHtml(textMap.mapel)}:</strong> ${escapeHtml(String(jadwal?.mapel || '-'))}</p>
<p><strong>${escapeHtml(textMap.tanggalWaktu)}:</strong> ${escapeHtml(String(jadwal?.tanggal || '-'))} | <strong>${escapeHtml(textMap.waktu)}:</strong> ${escapeHtml(`${toTimeLabel(jadwal?.jam_mulai)} - ${toTimeLabel(jadwal?.jam_selesai)}`)}</p>
<p><strong>${escapeHtml(textMap.guru)}:</strong> ${escapeHtml(String(soal?.guru_nama || '-'))}</p>
${instruksiUmum}
</div>
${sectionHtml}
</div>
</div>
</body></html>`
  }

  window.guruExamHtmlUtils = {
    buildExamBrowserSectionsHtml,
    buildExamBrowserDocumentHtml,
    createExamWordMarkerFormatter,
    buildExamWordSectionsHtml,
    buildExamWordDocumentHtml
  }
})()
