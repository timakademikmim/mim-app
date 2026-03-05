;(function initGuruExamHtmlUtils() {
  if (window.guruExamHtmlUtils) return

  function buildExamIsiTitikExtraLineHtml({ items, isAr, escapeHtml }) {
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
    const label = isAr ? 'Ø§Ø®ØªÙŠØ§Ø±Ø§Øª Ø§Ù„ÙƒÙ„Ù…Ø§Øª' : 'Pilihan kata'
    return `<p><strong>${escapeHtml(label)}:</strong> (${escapeHtml(list.join(', '))})</p>`
  }

  function buildExamBrowserQuestionTitleHtml({ q, no, lang, escapeHtml, formatExamMarker, formatExamNumber }) {
    return `<div class="q-title">${escapeHtml(formatExamMarker(formatExamNumber(no, lang), lang))} ${escapeHtml(String(q?.text || '-'))}</div>`
  }

  function buildExamBrowserPgQuestionHtml({ q, qText, lang, isAr, escapeHtml, formatExamMarker }) {
    const opts = q?.options || {}
    const mA = isAr ? 'Ø£' : 'a'
    const mB = isAr ? 'Ø¨' : 'b'
    const mC = isAr ? 'Ø¬' : 'c'
    const mD = isAr ? 'Ø¯' : 'd'
    return `
      <li>
        ${qText}
        <div class="pg-grid">
          <div>${escapeHtml(formatExamMarker(mA, lang))} ${escapeHtml(String(opts.a || '-'))}</div>
          <div>${escapeHtml(formatExamMarker(mC, lang))} ${escapeHtml(String(opts.c || '-'))}</div>
          <div>${escapeHtml(formatExamMarker(mB, lang))} ${escapeHtml(String(opts.b || '-'))}</div>
          <div>${escapeHtml(formatExamMarker(mD, lang))} ${escapeHtml(String(opts.d || '-'))}</div>
        </div>
      </li>
    `
  }

  function buildExamBrowserPairQuestionHtml({ q, qText, lang, isAr, escapeHtml, formatExamMarker, formatExamNumber, getArabicLetterByIndex }) {
    const pairs = Array.isArray(q?.pairs) ? q.pairs : []
    const rows = pairs.map((pair, i) => `
      <tr>
        <td>${escapeHtml(formatExamMarker(formatExamNumber(i + 1, lang), lang))} ${escapeHtml(String(pair?.a || '-'))}</td>
        <td>${escapeHtml(formatExamMarker((isAr ? getArabicLetterByIndex(i) : String.fromCharCode(65 + i)), lang))} ${escapeHtml(String(pair?.b || '-'))}</td>
      </tr>
    `).join('')
    return `
      <li>
        ${qText}
        <table class="pair-table">
          <thead><tr><th>${isAr ? 'Ø§Ù„Ø¹Ù…ÙˆØ¯ Ø£' : 'Baris A'}</th><th>${isAr ? 'Ø§Ù„Ø¹Ù…ÙˆØ¯ Ø¨' : 'Baris B'}</th></tr></thead>
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
    getArabicLetterByIndex
  }) {
    const no = idx + 1
    const qText = buildExamBrowserQuestionTitleHtml({ q, no, lang, escapeHtml, formatExamMarker, formatExamNumber })
    if (section.type === 'pilihan-ganda') return buildExamBrowserPgQuestionHtml({ q, qText, lang, isAr, escapeHtml, formatExamMarker })
    if (section.type === 'pasangkan-kata') return buildExamBrowserPairQuestionHtml({ q, qText, lang, isAr, escapeHtml, formatExamMarker, formatExamNumber, getArabicLetterByIndex })
    return `<li>${qText}</li>`
  }

  function buildExamBrowserSectionHtml({
    section,
    sectionIndex,
    lang,
    isAr,
    escapeHtml,
    getExamPrintTypeParts,
    getExamPrintTypeInstruction,
    formatExamMarker,
    formatExamNumber,
    getArabicLetterByIndex
  }) {
    const headingParts = getExamPrintTypeParts(section.type, sectionIndex, lang)
    const instruksiModel = getExamPrintTypeInstruction(section.type, lang)
    const items = section.items || []
    const extraLine = section.type === 'isi-titik'
      ? buildExamIsiTitikExtraLineHtml({ items, isAr, escapeHtml })
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
      getArabicLetterByIndex
    })).join('')
    return `
      <section class="sec">
        <h3><strong>${escapeHtml(headingParts.marker)}</strong> ${escapeHtml(headingParts.label)}</h3>
        <p>${escapeHtml(instruksiModel)}</p>
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
      getExamPrintTypeInstruction,
      formatExamMarker,
      formatExamNumber,
      getArabicLetterByIndex
    })).join('')
  }

  function buildExamBrowserDocumentHtml({ jadwal, soal, textMap, isAr, instruksiUmum, sectionHtml, escapeHtml, toTimeLabel }) {
    return `
<!doctype html>
<html lang="${isAr ? 'ar' : 'id'}" dir="${isAr ? 'rtl' : 'ltr'}">
<head>
  <meta charset="utf-8">
  <title>${escapeHtml(textMap.title)}</title>
  <style>
    body { font-family: 'Times New Roman', serif; margin: 20px; color: #111; }
    h1 { text-align: center; font-size: 22px; margin: 0 0 12px 0; }
    .meta p { margin: 4px 0; font-size: 14px; }
    .sec { margin-top: 14px; }
    .sec h3 { margin: 0 0 6px 0; font-size: 16px; }
    .sec p { margin: 4px 0; font-size: 14px; }
    ol { margin: 6px 0 0 0; padding-${isAr ? 'right' : 'left'}: 20px; }
    li { margin: 8px 0; }
    .q-title { margin-bottom: 4px; }
    .pg-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 2px 18px; margin-${isAr ? 'right' : 'left'}: 20px; }
    .pair-table { border-collapse: collapse; width: 100%; margin-top: 4px; }
    .pair-table th, .pair-table td { border: 1px solid #999; padding: 4px 6px; font-size: 13px; text-align: ${isAr ? 'right' : 'left'}; }
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
    const no = idx + 1
    const qText = `<div class="q-title">${markerHtml(formatExamNumber(no, lang))} ${escapeHtml(String(q?.text || '-'))}</div>`
    if (section.type === 'pilihan-ganda') {
      const opts = q?.options || {}
      const mA = isAr ? 'Ø£' : 'a'
      const mB = isAr ? 'Ø¨' : 'b'
      const mC = isAr ? 'Ø¬' : 'c'
      const mD = isAr ? 'Ø¯' : 'd'
      return `<li>${qText}<div class="pg-grid">
        <div>${markerHtml(mA)} ${escapeHtml(String(opts.a || '-'))}</div>
        <div>${markerHtml(mC)} ${escapeHtml(String(opts.c || '-'))}</div>
        <div>${markerHtml(mB)} ${escapeHtml(String(opts.b || '-'))}</div>
        <div>${markerHtml(mD)} ${escapeHtml(String(opts.d || '-'))}</div>
      </div></li>`
    }
    return `<li>${qText}</li>`
  }

  function buildExamWordSectionHtml({ section, sectionIndex, lang, isAr, markerHtml, escapeHtml, getExamPrintTypeParts, getExamPrintTypeInstruction }) {
    const headingParts = getExamPrintTypeParts(section.type, sectionIndex, lang)
    const instruksiModel = getExamPrintTypeInstruction(section.type, lang)
    const items = section.items || []
    const questionsHtml = items.map((q, idx) => buildExamWordQuestionHtml({
      section,
      q,
      idx,
      lang,
      isAr,
      markerHtml,
      escapeHtml
    })).join('')
    return `<section class="sec"><h3><strong>${isAr ? `<span class="ar-marker">${escapeHtml(headingParts.marker)}</span>` : escapeHtml(headingParts.marker)}</strong> ${escapeHtml(headingParts.label)}</h3><p>${escapeHtml(instruksiModel)}</p><ol>${questionsHtml}</ol></section>`
  }

  function buildExamWordSectionsHtml({ sections, lang, isAr, markerHtml, escapeHtml, getExamPrintTypeParts, getExamPrintTypeInstruction }) {
    return (sections || []).map((section, sectionIndex) => buildExamWordSectionHtml({
      section,
      sectionIndex,
      lang,
      isAr,
      markerHtml,
      escapeHtml,
      getExamPrintTypeParts,
      getExamPrintTypeInstruction
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
.sec p { margin: 4px 0; font-size: 14px; }
ol { margin: 6px 0 0 0; padding-${isAr ? 'right' : 'left'}: 20px; }
li { margin: 8px 0; }
.q-title { margin-bottom: 4px; }
.pg-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 2px 18px; margin-${isAr ? 'right' : 'left'}: 20px; }
.ar-marker { display:inline-block; white-space:nowrap; direction:rtl; unicode-bidi:isolate-override; min-width:20px; }
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
