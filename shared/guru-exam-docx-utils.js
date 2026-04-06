;(function initGuruExamDocxUtils() {
  if (window.guruExamDocxUtils) return

  const WORD_NS = 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'
  const DOCX_MIME = 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
  const TEMPLATE_PATH_AR = 'ExamAR.docx'
  const TEMPLATE_PATH_ID = 'ExamIN.docx'

  function getPrintUtils() {
    return window.guruExamPrintUtils || {}
  }

  function getSectionParts(type, index, langCode = 'AR') {
    const printUtils = getPrintUtils()
    if (typeof printUtils.getExamPrintTypeParts === 'function') {
      return printUtils.getExamPrintTypeParts(type, index, langCode)
    }
    const lang = String(langCode || 'AR').toUpperCase()
    const markerLetters = lang === 'AR' ? ['أ', 'ب', 'ج', 'د', 'هـ', 'و'] : ['A', 'B', 'C', 'D', 'E', 'F']
    const marker = markerLetters[index % markerLetters.length] || (lang === 'AR' ? 'أ' : 'A')
    let label = lang === 'AR' ? 'اختيار من متعدد' : 'Pilihan Ganda'
    if (type === 'esai') label = lang === 'AR' ? 'مقال' : 'Esai'
    else if (type === 'benar-salah') label = lang === 'AR' ? 'الصحيح أو الخطأ' : 'Pernyataan Benar atau Salah'
    else if (type === 'cari-kata') label = lang === 'AR' ? 'ابحث عن الكلمات' : 'Cari Kata'
    else if (type === 'pasangkan-kata') label = lang === 'AR' ? 'وصل الكلمات' : 'Pasangkan Kata'
    else if (type === 'isi-titik') label = lang === 'AR' ? 'املأ الفراغ' : 'Isi Titik Kosong'
    return { marker, label }
  }

  function buildSectionsFromQuestions(questions, fallbackType) {
    const printUtils = getPrintUtils()
    if (typeof printUtils.buildExamPrintSections === 'function') {
      return printUtils.buildExamPrintSections(questions, fallbackType)
    }
    return []
  }

  function formatDocxNumber(value, langCode = 'AR') {
    const printUtils = getPrintUtils()
    if (typeof printUtils.formatExamNumber === 'function') {
      return printUtils.formatExamNumber(value, langCode)
    }
    if (String(langCode || 'AR').toUpperCase() !== 'AR') return String(value == null ? '' : value)
    const map = ['٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩']
    return String(value == null ? '' : value).replace(/\d/g, d => map[Number(d)] || d)
  }

  function getPgOptionEntries(question, langCode = 'AR') {
    if (String(question?.type || '').trim().toLowerCase() === 'benar-salah') {
      const isAr = String(langCode || 'AR').toUpperCase() === 'AR'
      return [
        { key: 'benar', label: isAr ? 'أ' : 'a', value: isAr ? 'صحيح' : 'Benar' },
        { key: 'salah', label: isAr ? 'ب' : 'b', value: isAr ? 'خطأ' : 'Salah' }
      ]
    }
    const printUtils = getPrintUtils()
    if (typeof window.guruExamHtmlUtils?.getExamPgOptionEntries === 'function') {
      return window.guruExamHtmlUtils.getExamPgOptionEntries(question?.options || {}, String(langCode || 'AR').toUpperCase() === 'AR')
    }
    const fallbackLetters = String(langCode || 'AR').toUpperCase() === 'AR' ? ['أ', 'ب', 'ج', 'د'] : ['a', 'b', 'c', 'd']
    const raw = question?.options || {}
    const keys = Object.keys(raw).sort()
    return keys.map((key, index) => ({
      key,
      label: fallbackLetters[index] || String.fromCharCode(1571 + index),
      value: String(raw[key] || '').trim()
    }))
  }

  function uniqueWordBank(items) {
    const words = []
    const seen = new Set()
    ;(Array.isArray(items) ? items : []).forEach(item => {
      const list = Array.isArray(item?.fragments) ? item.fragments : []
      list.forEach(word => {
        const value = String(word || '').trim()
        if (!value || seen.has(value)) return
        seen.add(value)
        words.push(value)
      })
    })
    return words
  }

  function qsa(node, tagName, direct = false) {
    if (!node) return []
    if (direct) {
      return Array.from(node.childNodes || []).filter(child => child?.namespaceURI === WORD_NS && child?.localName === tagName)
    }
    return Array.from(node.getElementsByTagNameNS(WORD_NS, tagName))
  }

  function first(node, tagName, direct = false) {
    return qsa(node, tagName, direct)[0] || null
  }

  function createWordNode(doc, tagName) {
    return doc.createElementNS(WORD_NS, `w:${tagName}`)
  }

  function getBodyNodes(doc) {
    return first(doc.documentElement, 'body', true)
  }

  function getTableRows(table) {
    return qsa(table, 'tr', true)
  }

  function getRowCells(row) {
    return qsa(row, 'tc', true)
  }

  function getTableWidth(table) {
    const tblPr = first(table, 'tblPr', true)
    const tblW = tblPr ? first(tblPr, 'tblW', true) : null
    const widthValue = Number(tblW?.getAttributeNS(WORD_NS, 'w') || tblW?.getAttribute('w:w') || 0)
    if (Number.isFinite(widthValue) && widthValue > 0) return widthValue
    return qsa(first(table, 'tblGrid', true), 'gridCol', true).reduce((sum, col) => {
      const part = Number(col?.getAttributeNS(WORD_NS, 'w') || col?.getAttribute('w:w') || 0)
      return sum + (Number.isFinite(part) ? part : 0)
    }, 0)
  }

  function removeAllRuns(paragraph) {
    Array.from(paragraph.childNodes || []).forEach(child => {
      if (child?.namespaceURI === WORD_NS && child?.localName !== 'pPr') {
        paragraph.removeChild(child)
      }
    })
  }

  function createRunWithTemplate(doc, templateRun) {
    if (templateRun) {
      const clone = templateRun.cloneNode(true)
      Array.from(clone.childNodes || []).forEach(child => {
        if (child?.namespaceURI === WORD_NS && child?.localName !== 'rPr') {
          clone.removeChild(child)
        }
      })
      let textNode = first(clone, 't', true)
      if (!textNode) {
        textNode = doc.createElementNS(WORD_NS, 'w:t')
        clone.appendChild(textNode)
      }
      return { run: clone, textNode }
    }
    const run = doc.createElementNS(WORD_NS, 'w:r')
    const textNode = doc.createElementNS(WORD_NS, 'w:t')
    run.appendChild(textNode)
    return { run, textNode }
  }

  function setParagraphText(paragraph, text) {
    const doc = paragraph.ownerDocument
    const templateRun = first(paragraph, 'r', true)?.cloneNode(true) || null
    removeAllRuns(paragraph)
    const { run, textNode } = createRunWithTemplate(doc, templateRun)
    const safeText = String(text ?? '')
    if (/^\s|\s$/.test(safeText)) {
      textNode.setAttributeNS('http://www.w3.org/XML/1998/namespace', 'xml:space', 'preserve')
    } else {
      textNode.removeAttributeNS('http://www.w3.org/XML/1998/namespace', 'space')
    }
    textNode.textContent = safeText
    paragraph.appendChild(run)
  }

  function ensureParagraph(cell) {
    let paragraph = first(cell, 'p', true)
    if (paragraph) return paragraph
    paragraph = cell.ownerDocument.createElementNS(WORD_NS, 'w:p')
    cell.appendChild(paragraph)
    return paragraph
  }

  function setCellText(cell, text) {
    const paragraph = ensureParagraph(cell)
    setParagraphText(paragraph, text)
    const extraParagraphs = qsa(cell, 'p', true).slice(1)
    extraParagraphs.forEach(node => cell.removeChild(node))
  }

  function setCellLines(cell, lines) {
    const doc = cell.ownerDocument
    const sourceParagraph = ensureParagraph(cell)
    const paragraphs = qsa(cell, 'p', true)
    paragraphs.forEach(node => cell.removeChild(node))
    const safeLines = Array.isArray(lines)
      ? lines.map(line => String(line == null ? '' : line).replace(/\r/g, ''))
      : []
    const hasContent = safeLines.some(line => line.length > 0)
    if (!hasContent) {
      const emptyParagraph = sourceParagraph.cloneNode(true)
      setParagraphText(emptyParagraph, '')
      cell.appendChild(emptyParagraph)
      return
    }
    safeLines.forEach((line, index) => {
      const paragraph = index === 0 ? sourceParagraph.cloneNode(true) : sourceParagraph.cloneNode(true)
      setParagraphText(paragraph, line)
      cell.appendChild(paragraph)
    })
  }

  function clearTableRows(table) {
    getTableRows(table).forEach(row => table.removeChild(row))
  }

  function ensureParagraphProperties(paragraph) {
    let pPr = first(paragraph, 'pPr', true)
    if (pPr) return pPr
    pPr = createWordNode(paragraph.ownerDocument, 'pPr')
    paragraph.insertBefore(pPr, paragraph.firstChild || null)
    return pPr
  }

  function setParagraphAlignment(paragraph, alignment = 'right') {
    const pPr = ensureParagraphProperties(paragraph)
    let jc = first(pPr, 'jc', true)
    if (!jc) {
      jc = createWordNode(paragraph.ownerDocument, 'jc')
      pPr.appendChild(jc)
    }
    jc.setAttributeNS(WORD_NS, 'w:val', alignment)
  }

  function removeParagraphIndent(paragraph) {
    const pPr = first(paragraph, 'pPr', true)
    const ind = pPr ? first(pPr, 'ind', true) : null
    if (pPr && ind) pPr.removeChild(ind)
  }

  function ensureCellProperties(cell) {
    let tcPr = first(cell, 'tcPr', true)
    if (tcPr) return tcPr
    tcPr = createWordNode(cell.ownerDocument, 'tcPr')
    cell.insertBefore(tcPr, cell.firstChild || null)
    return tcPr
  }

  function getCellWidth(cell) {
    if (!cell) return 0
    const tcPr = ensureCellProperties(cell)
    const tcW = tcPr ? first(tcPr, 'tcW', true) : null
    const width = Number(tcW?.getAttributeNS(WORD_NS, 'w') || tcW?.getAttribute('w:w') || 0)
    return Number.isFinite(width) && width > 0 ? width : 0
  }

  function setCellWidth(cell, width) {
    if (!cell || !(Number(width) > 0)) return
    const tcPr = ensureCellProperties(cell)
    const current = first(tcPr, 'tcW', true)
    if (current) tcPr.removeChild(current)
    const tcW = createWordNode(cell.ownerDocument, 'tcW')
    tcW.setAttributeNS(WORD_NS, 'w:w', String(Math.round(Number(width))))
    tcW.setAttributeNS(WORD_NS, 'w:type', 'dxa')
    tcPr.appendChild(tcW)
  }

  function clearCellGridSpan(cell) {
    if (!cell) return
    const tcPr = ensureCellProperties(cell)
    const gridSpan = first(tcPr, 'gridSpan', true)
    if (gridSpan) tcPr.removeChild(gridSpan)
  }

  function setTableGrid(table, colWidths = []) {
    if (!table || !Array.isArray(colWidths) || !colWidths.length) return
    const existingGrid = first(table, 'tblGrid', true)
    if (existingGrid) table.removeChild(existingGrid)
    const tblGrid = createWordNode(table.ownerDocument, 'tblGrid')
    colWidths.forEach(width => {
      const gridCol = createWordNode(table.ownerDocument, 'gridCol')
      gridCol.setAttributeNS(WORD_NS, 'w:w', String(Math.round(Number(width) || 0)))
      tblGrid.appendChild(gridCol)
    })
    const firstRow = first(table, 'tr', true)
    table.insertBefore(tblGrid, firstRow || null)
    const tblPr = first(table, 'tblPr', true)
    const tblW = tblPr ? first(tblPr, 'tblW', true) : null
    if (tblW) {
      tblW.setAttributeNS(WORD_NS, 'w:w', String(colWidths.reduce((sum, part) => sum + (Number(part) || 0), 0)))
      tblW.setAttributeNS(WORD_NS, 'w:type', 'dxa')
    }
  }

  function setCellBorderMode(cell, mode = 'visible') {
    if (!cell) return
    const tcPr = ensureCellProperties(cell)
    let tcBorders = first(tcPr, 'tcBorders', true)
    if (!tcBorders) {
      tcBorders = createWordNode(cell.ownerDocument, 'tcBorders')
      tcPr.appendChild(tcBorders)
    }
    const borderValue = mode === 'hidden' ? 'nil' : 'single'
    ;['top', 'left', 'bottom', 'right'].forEach(side => {
      let border = first(tcBorders, side, true)
      if (!border) {
        border = createWordNode(cell.ownerDocument, side)
        tcBorders.appendChild(border)
      }
      border.setAttributeNS(WORD_NS, 'w:val', borderValue)
      if (mode === 'hidden') {
        border.removeAttributeNS(WORD_NS, 'w:sz')
        border.removeAttributeNS(WORD_NS, 'w:space')
        border.removeAttributeNS(WORD_NS, 'w:color')
      } else {
        if (!border.getAttributeNS(WORD_NS, 'w:sz')) border.setAttributeNS(WORD_NS, 'w:sz', '4')
        if (!border.getAttributeNS(WORD_NS, 'w:space')) border.setAttributeNS(WORD_NS, 'w:space', '0')
        if (!border.getAttributeNS(WORD_NS, 'w:color')) border.setAttributeNS(WORD_NS, 'w:color', 'auto')
      }
    })
  }

  function setCellBorderSides(cell, sides = {}) {
    if (!cell) return
    const tcPr = ensureCellProperties(cell)
    let tcBorders = first(tcPr, 'tcBorders', true)
    if (!tcBorders) {
      tcBorders = createWordNode(cell.ownerDocument, 'tcBorders')
      tcPr.appendChild(tcBorders)
    }
    ;['top', 'left', 'bottom', 'right'].forEach(side => {
      let border = first(tcBorders, side, true)
      if (!border) {
        border = createWordNode(cell.ownerDocument, side)
        tcBorders.appendChild(border)
      }
      const value = String(sides?.[side] || 'single').toLowerCase() === 'hidden' ? 'nil' : String(sides?.[side] || 'single')
      border.setAttributeNS(WORD_NS, 'w:val', value)
      if (value === 'nil') {
        border.removeAttributeNS(WORD_NS, 'w:sz')
        border.removeAttributeNS(WORD_NS, 'w:space')
        border.removeAttributeNS(WORD_NS, 'w:color')
      } else {
        if (!border.getAttributeNS(WORD_NS, 'w:sz')) border.setAttributeNS(WORD_NS, 'w:sz', '4')
        if (!border.getAttributeNS(WORD_NS, 'w:space')) border.setAttributeNS(WORD_NS, 'w:space', '0')
        if (!border.getAttributeNS(WORD_NS, 'w:color')) border.setAttributeNS(WORD_NS, 'w:color', 'auto')
      }
    })
  }

  function getStaticText(langCode = 'ID') {
    const printUtils = getPrintUtils()
    if (typeof printUtils.getExamPdfStaticText === 'function') {
      return printUtils.getExamPdfStaticText(langCode)
    }
    return {
      instruksiUmum: langCode === 'AR' ? 'ØªØ¹Ù„ÙŠÙ…Ø§Øª Ø¹Ø§Ù…Ø©' : 'Instruksi Umum'
    }
  }

  function getSectionInstruction(type, langCode = 'ID') {
    const printUtils = getPrintUtils()
    if (typeof printUtils.getExamPrintTypeInstruction === 'function') {
      return printUtils.getExamPrintTypeInstruction(type, langCode)
    }
    return ''
  }

  function runHasGraphic(run) {
    if (!run) return false
    return Boolean(
      first(run, 'drawing') ||
      first(run, 'pict') ||
      Array.from(run.childNodes || []).some(child => String(child?.localName || '') === 'AlternateContent')
    )
  }

  function buildRunNodeFromTemplate(doc, templateRun, text = '', type = 'text') {
    const { run, textNode } = createRunWithTemplate(doc, templateRun)
    Array.from(run.childNodes || []).forEach(child => {
      if (child?.namespaceURI === WORD_NS && child?.localName !== 'rPr') {
        run.removeChild(child)
      }
    })
    if (type === 'tab') {
      const tab = createWordNode(doc, 'tab')
      run.appendChild(tab)
      return run
    }
    if (/^\s|\s$/.test(String(text))) {
      textNode.setAttributeNS('http://www.w3.org/XML/1998/namespace', 'xml:space', 'preserve')
    } else {
      textNode.removeAttributeNS('http://www.w3.org/XML/1998/namespace', 'space')
    }
    textNode.textContent = String(text ?? '')
    run.appendChild(textNode)
    return run
  }

  function setParagraphSegments(paragraph, segments = [], opts = {}) {
    const doc = paragraph.ownerDocument
    const templateRun = first(paragraph, 'r', true)?.cloneNode(true) || null
    const preserved = opts.preserveGraphicRuns
      ? Array.from(paragraph.childNodes || []).filter(child => child?.namespaceURI === WORD_NS && child?.localName === 'r' && runHasGraphic(child))
          .map(node => node.cloneNode(true))
      : []
    Array.from(paragraph.childNodes || []).forEach(child => {
      if (child?.namespaceURI === WORD_NS && child?.localName !== 'pPr') {
        paragraph.removeChild(child)
      }
    })
    preserved.forEach(node => paragraph.appendChild(node))
    ;(Array.isArray(segments) ? segments : []).forEach(segment => {
      if (!segment) return
      const type = segment.type === 'tab' ? 'tab' : 'text'
      paragraph.appendChild(buildRunNodeFromTemplate(doc, templateRun, segment.text, type))
    })
  }

  function setParagraphTextPreserveGraphics(paragraph, text) {
    setParagraphSegments(paragraph, [{ type: 'text', text: String(text ?? '') }], { preserveGraphicRuns: true })
  }

  function getParagraphTextWithTabs(paragraph) {
    const parts = []
    Array.from(paragraph?.childNodes || []).forEach(child => {
      if (child?.namespaceURI !== WORD_NS || child?.localName !== 'r') return
      Array.from(child.childNodes || []).forEach(runChild => {
        if (runChild?.namespaceURI !== WORD_NS) return
        if (runChild.localName === 'tab') parts.push('\t')
        else if (runChild.localName === 't') parts.push(String(runChild.textContent || ''))
      })
    })
    return parts.join('')
  }

  function setParagraphTextWithTabs(paragraph, text, opts = {}) {
    const parts = String(text ?? '').split(/(\t)/)
    const segments = []
    parts.forEach(part => {
      if (!part) return
      if (part === '\t') segments.push({ type: 'tab' })
      else segments.push({ type: 'text', text: part })
    })
    setParagraphSegments(paragraph, segments, opts)
  }

  function replaceLabeledValue(text, label, value, nextLabel = '') {
    const raw = String(text || '')
    const labelIndex = raw.indexOf(label)
    if (labelIndex < 0) return raw
    let colonIndex = raw.indexOf(':', labelIndex + label.length)
    if (colonIndex < 0) colonIndex = labelIndex + label.length
    let valueStart = colonIndex + (raw[colonIndex] === ':' ? 1 : 0)
    while (valueStart < raw.length && (raw[valueStart] === ' ' || raw[valueStart] === '\t')) valueStart += 1
    let valueEnd = nextLabel ? raw.indexOf(nextLabel, valueStart) : raw.length
    if (valueEnd < 0) valueEnd = raw.length
    let suffixStart = valueEnd
    while (suffixStart > valueStart && (raw[suffixStart - 1] === ' ' || raw[suffixStart - 1] === '\t')) suffixStart -= 1
    return raw.slice(0, valueStart) + String(value ?? '') + raw.slice(suffixStart)
  }

  function parseInstructionMeta(value, fallbackLang = 'ID') {
    const printUtils = getPrintUtils()
    if (typeof printUtils.parseExamInstruksiMeta === 'function') {
      const meta = printUtils.parseExamInstruksiMeta(value)
      return {
        lang: String(meta?.lang || fallbackLang || 'ID').toUpperCase() === 'AR' ? 'AR' : 'ID',
        text: String(meta?.text || '')
      }
    }
    return {
      lang: String(fallbackLang || 'ID').toUpperCase() === 'AR' ? 'AR' : 'ID',
      text: String(value || '')
    }
  }

  function splitPreserveLines(value) {
    return String(value || '')
      .replace(/\r/g, '')
      .split('\n')
  }

  function getCellPlainText(cell) {
    return qsa(cell, 't').map(entry => String(entry?.textContent || '')).join('')
  }

  function applyHeaderParagraphs(paragraphs = [], headerMeta = {}) {
    if (!Array.isArray(paragraphs) || !paragraphs.length) return
    const title = String(headerMeta.examTitle || '').trim()
    const schoolName = String(headerMeta.schoolName || '').trim()
    const schoolYear = String(headerMeta.schoolYear || '').trim()
    const subject = String(headerMeta.subject || '-').trim() || '-'
    const examDate = String(headerMeta.examDate || '-').trim() || '-'
    const className = String(headerMeta.className || '-').trim() || '-'
    const duration = String(headerMeta.duration || '-').trim() || '-'
    const studentLine = String(headerMeta.studentLine || '_______________________________')

    if (paragraphs[0]) setParagraphTextPreserveGraphics(paragraphs[0], title)
    if (paragraphs[1]) setParagraphText(paragraphs[1], schoolName)
    if (paragraphs[2]) setParagraphText(paragraphs[2], schoolYear)
    if (paragraphs[3]) {
      let line = getParagraphTextWithTabs(paragraphs[3])
      line = replaceLabeledValue(line, 'Hari/Tanggal', examDate)
      line = replaceLabeledValue(line, 'Pelajaran', subject, 'Hari/Tanggal')
      setParagraphTextWithTabs(paragraphs[3], line)
    }
    if (paragraphs[4]) {
      let line = getParagraphTextWithTabs(paragraphs[4])
      line = replaceLabeledValue(line, 'Waktu', duration)
      line = replaceLabeledValue(line, 'Kelas', className, 'Waktu')
      setParagraphTextWithTabs(paragraphs[4], line)
    }
    if (paragraphs[5]) setParagraphText(paragraphs[5], `NAMA : ${studentLine}`)
  }

  function getTextDensity(text) {
    return String(text || '').replace(/\s+/g, '').length
  }

  function resolvePgLayoutMode(entries = []) {
    const lengths = (Array.isArray(entries) ? entries : []).map(entry => getTextDensity(entry?.value || ''))
    const maxLen = Math.max(0, ...lengths)
    if (maxLen > 42) return 'stack'
    if (maxLen > 20) return 'double'
    return 'single'
  }

  function clearPgOptionRow(cells) {
    ;[1,2,3,4,5,6,7,8,9,10,11,12].forEach(index => {
      if (cells[index]) setCellText(cells[index], '')
    })
  }

  function fillPgOptionSlot(cells, slot, entry) {
    if (!entry) return
    const slotMap = [
      { labelCell: 1, dotCell: 2, valueCell: 3 },
      { labelCell: 4, dotCell: 5, valueCell: 6 },
      { labelCell: 7, dotCell: 8, valueCell: 9 },
      { labelCell: 10, dotCell: 11, valueCell: 12 }
    ]
    const target = slotMap[slot]
    if (!target) return
    if (cells[target.labelCell]) setCellText(cells[target.labelCell], String(entry.label || ''))
    if (cells[target.dotCell]) setCellText(cells[target.dotCell], '.')
    if (cells[target.valueCell]) setCellText(cells[target.valueCell], String(entry.value || ''))
  }

  function fillPgDoubleOptionRow(cells, leftEntry, rightEntry) {
    if (cells[1]) setCellText(cells[1], String(leftEntry?.label || ''))
    if (cells[2]) setCellText(cells[2], leftEntry ? '.' : '')
    if (cells[3]) setCellText(cells[3], String(leftEntry?.value || ''))
    if (cells[4]) setCellText(cells[4], String(rightEntry?.label || ''))
    if (cells[5]) setCellText(cells[5], rightEntry ? '.' : '')
    if (cells[6]) setCellText(cells[6], String(rightEntry?.value || ''))
  }

  function fillPgStackOptionRow(cells, entry) {
    if (cells[1]) setCellText(cells[1], String(entry?.label || ''))
    if (cells[2]) setCellText(cells[2], entry ? '.' : '')
    if (cells[3]) setCellText(cells[3], String(entry?.value || ''))
  }

  function buildParagraphFromTemplate(templateParagraph, text, opts = {}) {
    const doc = templateParagraph?.ownerDocument || opts.doc
    const paragraph = templateParagraph
      ? templateParagraph.cloneNode(true)
      : createWordNode(doc, 'p')
    setParagraphText(paragraph, text)
    removeParagraphIndent(paragraph)
    setParagraphAlignment(paragraph, opts.align || 'right')
    return paragraph
  }

  function createTableCell(doc, width, paragraphs = [], opts = {}) {
    const cell = createWordNode(doc, 'tc')
    const tcPr = createWordNode(doc, 'tcPr')
    const tcW = createWordNode(doc, 'tcW')
    tcW.setAttributeNS(WORD_NS, 'w:w', String(width))
    tcW.setAttributeNS(WORD_NS, 'w:type', 'dxa')
    tcPr.appendChild(tcW)
    if (opts.vAlign) {
      const vAlign = createWordNode(doc, 'vAlign')
      vAlign.setAttributeNS(WORD_NS, 'w:val', opts.vAlign)
      tcPr.appendChild(vAlign)
    }
    if (opts.borderBox) {
      const tcBorders = createWordNode(doc, 'tcBorders')
      ;['top', 'left', 'bottom', 'right'].forEach(side => {
        const border = createWordNode(doc, side)
        border.setAttributeNS(WORD_NS, 'w:val', 'single')
        border.setAttributeNS(WORD_NS, 'w:sz', '10')
        border.setAttributeNS(WORD_NS, 'w:space', '0')
        border.setAttributeNS(WORD_NS, 'w:color', '000000')
        tcBorders.appendChild(border)
      })
      tcPr.appendChild(tcBorders)
    }
    cell.appendChild(tcPr)
    const paragraphList = Array.isArray(paragraphs) ? paragraphs : []
    if (!paragraphList.length) {
      cell.appendChild(createWordNode(doc, 'p'))
    } else {
      paragraphList.forEach(paragraph => cell.appendChild(paragraph))
    }
    return cell
  }

  function createTableRow(doc, cells = []) {
    const row = createWordNode(doc, 'tr')
    ;(Array.isArray(cells) ? cells : []).forEach(cell => row.appendChild(cell))
    return row
  }

  function createTable(doc, width, colWidths = [], rows = [], opts = {}) {
    const table = createWordNode(doc, 'tbl')
    const tblPr = createWordNode(doc, 'tblPr')
    const bidiVisual = createWordNode(doc, 'bidiVisual')
    const tblW = createWordNode(doc, 'tblW')
    tblW.setAttributeNS(WORD_NS, 'w:w', String(width))
    tblW.setAttributeNS(WORD_NS, 'w:type', 'dxa')
    tblPr.appendChild(bidiVisual)
    tblPr.appendChild(tblW)
    if (opts.noBorders !== false) {
      const tblBorders = createWordNode(doc, 'tblBorders')
      ;['top', 'left', 'bottom', 'right', 'insideH', 'insideV'].forEach(side => {
        const border = createWordNode(doc, side)
        border.setAttributeNS(WORD_NS, 'w:val', 'nil')
        tblBorders.appendChild(border)
      })
      tblPr.appendChild(tblBorders)
    }
    table.appendChild(tblPr)
    const tblGrid = createWordNode(doc, 'tblGrid')
    ;(Array.isArray(colWidths) ? colWidths : []).forEach(colWidth => {
      const gridCol = createWordNode(doc, 'gridCol')
      gridCol.setAttributeNS(WORD_NS, 'w:w', String(colWidth))
      tblGrid.appendChild(gridCol)
    })
    table.appendChild(tblGrid)
    ;(Array.isArray(rows) ? rows : []).forEach(row => table.appendChild(row))
    return table
  }

  function setHeaderRow(headerRow, section, sectionIndex, langCode = 'AR') {
    const cells = getRowCells(headerRow)
    const parts = getSectionParts(section.type, sectionIndex, langCode)
    const markerText = cells[1] ? String(parts.marker || '').replace(/\.$/, '') : String(parts.marker || '')
    if (cells[0]) setCellText(cells[0], markerText)
    if (cells[1]) setCellText(cells[1], '.')
    if (cells[2]) setCellText(cells[2], parts.label)
  }

  function cloneRow(row) {
    if (!row || typeof row.cloneNode !== 'function') {
      throw new Error('Template ExamAR belum sesuai. Ada baris tabel yang tidak ditemukan.')
    }
    return row.cloneNode(true)
  }

  function maybeCloneRow(row) {
    return row && typeof row.cloneNode === 'function' ? row.cloneNode(true) : null
  }

  function pickTemplateRow(table, preferredIndexes = []) {
    const rows = getTableRows(table)
    const indexes = Array.isArray(preferredIndexes) ? preferredIndexes : [preferredIndexes]
    for (const index of indexes) {
      const row = rows[Number(index)]
      if (row) return row
    }
    return rows[rows.length - 1] || null
  }

  function resolveSectionInfoText(section, headerMeta = {}) {
    const raw = String(section?.instruction || '')
    if (raw.trim()) {
      const meta = parseInstructionMeta(raw, headerMeta.lang || 'AR')
      return String(meta?.text || '')
    }
    return String(getSectionInstruction(section?.type, headerMeta.lang || 'AR') || '')
  }

  function fillSectionInfoRow(infoRow, text) {
    const cells = getRowCells(infoRow)
    const targetCell = cells[1] || cells[cells.length - 1] || cells[0] || null
    const lines = Array.isArray(text) ? text : splitPreserveLines(text)
    if (targetCell) setCellLines(targetCell, lines)
    return infoRow
  }

  function fillPgTable(table, section, sectionIndex, templates, headerMeta = {}) {
    clearTableRows(table)
    const headerRow = cloneRow(templates.header)
    setHeaderRow(headerRow, section, sectionIndex, headerMeta.lang || 'AR')
    table.appendChild(headerRow)
    const infoText = resolveSectionInfoText(section, headerMeta)
    if (templates.info && infoText) {
      table.appendChild(fillSectionInfoRow(cloneRow(templates.info), infoText))
    }
    if (templates.spacer) table.appendChild(cloneRow(templates.spacer))

    ;(section.items || []).forEach((item, idx) => {
      const questionRow = cloneRow(templates.question)
      const qCells = getRowCells(questionRow)
      if (qCells[1]) setCellText(qCells[1], formatDocxNumber(idx + 1, headerMeta.lang || 'AR'))
      if (qCells[2]) setCellText(qCells[2], '.')
      if (qCells[3]) setCellText(qCells[3], String(item?.text || ''))
      table.appendChild(questionRow)

      const entries = getPgOptionEntries(item, headerMeta.lang || 'AR').slice(0, 4)
      const layoutMode = resolvePgLayoutMode(entries)
      if (layoutMode === 'stack') {
        entries.forEach(entry => {
          const optionRow = cloneRow(templates.optionsStack || templates.options || templates.optionsSingle)
          const oCells = getRowCells(optionRow)
          clearPgOptionRow(oCells)
          fillPgStackOptionRow(oCells, entry)
          table.appendChild(optionRow)
        })
      } else if (layoutMode === 'double') {
        const pairRows = [
          [entries[0], entries[1]],
          [entries[2], entries[3]]
        ]
        pairRows.forEach(pair => {
          const optionRow = cloneRow(templates.optionsDouble || templates.options || templates.optionsSingle)
          const oCells = getRowCells(optionRow)
          clearPgOptionRow(oCells)
          fillPgDoubleOptionRow(oCells, pair[0], pair[1])
          table.appendChild(optionRow)
        })
      } else {
        const optionRow = cloneRow(templates.optionsSingle || templates.options)
        const oCells = getRowCells(optionRow)
        clearPgOptionRow(oCells)
        entries.forEach((entry, idx) => fillPgOptionSlot(oCells, idx, entry))
        table.appendChild(optionRow)
      }
    })
  }

  function fillTrueFalseTable(table, section, sectionIndex, templates, headerMeta = {}) {
    clearTableRows(table)
    const headerRow = cloneRow(templates.header)
    setHeaderRow(headerRow, section, sectionIndex, headerMeta.lang || 'AR')
    table.appendChild(headerRow)
    const infoText = resolveSectionInfoText(section, headerMeta)
    if (templates.info && infoText) {
      table.appendChild(fillSectionInfoRow(cloneRow(templates.info), infoText))
    }
    if (templates.spacer) table.appendChild(cloneRow(templates.spacer))

    const isAr = String(headerMeta.lang || 'AR').toUpperCase() === 'AR'
    const optionA = isAr ? 'الصحيح' : 'Benar'
    const optionB = isAr ? 'الخطأ' : 'Salah'
    const labelA = isAr ? 'أ' : 'a'
    const labelB = isAr ? 'ب' : 'b'

    ;(section.items || []).forEach((item, idx) => {
      const questionRow = cloneRow(templates.question)
      const qCells = getRowCells(questionRow)
      if (qCells[1]) setCellText(qCells[1], formatDocxNumber(idx + 1, headerMeta.lang || 'AR'))
      if (qCells[2]) setCellText(qCells[2], '.')
      if (qCells[3]) setCellText(qCells[3], String(item?.text || ''))
      table.appendChild(questionRow)

      const optionRow = cloneRow(templates.options)
      const oCells = getRowCells(optionRow)
      if (oCells[1]) setCellText(oCells[1], labelA)
      if (oCells[2]) setCellText(oCells[2], '.')
      if (oCells[3]) setCellText(oCells[3], optionA)
      if (oCells[4]) setCellText(oCells[4], labelB)
      if (oCells[5]) setCellText(oCells[5], '.')
      if (oCells[6]) setCellText(oCells[6], optionB)
      table.appendChild(optionRow)
    })
  }

  function fillEssayTable(table, section, sectionIndex, templates, headerMeta = {}) {
    clearTableRows(table)
    const headerRow = cloneRow(templates.header)
    setHeaderRow(headerRow, section, sectionIndex, headerMeta.lang || 'AR')
    table.appendChild(headerRow)
    const infoText = resolveSectionInfoText(section, headerMeta)
    if (templates.info && infoText) {
      table.appendChild(fillSectionInfoRow(cloneRow(templates.info), infoText))
    }
    if (templates.spacer) table.appendChild(cloneRow(templates.spacer))

    ;(section.items || []).forEach((item, idx) => {
      const questionRow = cloneRow(idx === 0 ? templates.firstQuestion : templates.question)
      const cells = getRowCells(questionRow)
      if (cells[1]) setCellText(cells[1], formatDocxNumber(idx + 1, headerMeta.lang || 'AR'))
      if (cells[2]) setCellText(cells[2], '.')
      if (cells[3]) setCellText(cells[3], String(item?.text || ''))
      table.appendChild(questionRow)
    })
  }

  function buildWordSearchDocxLines(item, langCode = 'AR') {
    const safeLang = String(langCode || 'AR').toUpperCase() === 'AR' ? 'AR' : 'ID'
    const lines = []
    const intro = String(item?.text || '').trim()
    if (intro) lines.push(intro)
    const words = Array.isArray(item?.words)
      ? item.words.map(word => String(word || '').trim()).filter(Boolean)
      : []
    if (words.length) {
      lines.push(`${safeLang === 'AR' ? 'الكلمات المطلوبة' : 'Kata yang dicari'}: ${words.join(safeLang === 'AR' ? '، ' : ', ')}`)
    }
    const grid = Array.isArray(item?.grid) ? item.grid : []
    if (grid.length) {
      lines.push('')
      grid.forEach(row => {
        const text = (Array.isArray(row) ? row : []).map(cell => String(cell || '').trim()).join(' ')
        if (text) lines.push(text)
      })
    }
    return lines.length ? lines : ['-']
  }

  function buildCrosswordDocxLines(item, langCode = 'AR') {
    const safeLang = String(langCode || 'AR').toUpperCase() === 'AR' ? 'AR' : 'ID'
    const lines = []
    const intro = String(item?.text || '').trim()
    if (intro) lines.push(intro)
    const mask = Array.isArray(item?.mask) ? item.mask : []
    if (mask.length) {
      lines.push('')
      mask.forEach(row => {
        const text = Array.from(String(row || '')).map(cell => (cell === '#' ? '■' : '□')).join(' ')
        if (text) lines.push(text)
      })
    }
    const buildListLines = (title, entries) => {
      lines.push('')
      lines.push(title)
      if (!entries.length) {
        lines.push('-')
        return
      }
      entries.forEach(entry => {
        const clue = String(entry?.clue || '-').trim() || '-'
        const length = Number(entry?.length || 0) || 0
        lines.push(`${formatDocxNumber(entry?.number || '', safeLang)}. ${clue} (${length})`)
      })
    }
    buildListLines(safeLang === 'AR' ? 'أفقيًا' : 'Mendatar', Array.isArray(item?.entriesAcross) ? item.entriesAcross : [])
    buildListLines(safeLang === 'AR' ? 'عموديًا' : 'Menurun', Array.isArray(item?.entriesDown) ? item.entriesDown : [])
    return lines.length ? lines : ['-']
  }

  function buildCrosswordClueLines(item, langCode = 'AR') {
    const safeLang = String(langCode || 'AR').toUpperCase() === 'AR' ? 'AR' : 'ID'
    const acrossLabel = safeLang === 'AR' ? 'أفقيًا' : 'Mendatar'
    const downLabel = safeLang === 'AR' ? 'عموديًا' : 'Menurun'
    const across = (Array.isArray(item?.entriesAcross) ? item.entriesAcross : [])
      .filter(entry => String(entry?.clue || '').trim())
    const down = (Array.isArray(item?.entriesDown) ? item.entriesDown : [])
      .filter(entry => String(entry?.clue || '').trim())
    const lines = []
    const pushGroup = (title, entries) => {
      if (!entries.length) return
      if (lines.length) lines.push('')
      lines.push(title)
      entries.forEach(entry => {
        const clueLines = splitPreserveLines(String(entry?.clue || '').trim())
        const firstLine = clueLines.shift() || ''
        const length = Number(entry?.length || 0) || 0
        lines.push(`${formatDocxNumber(entry?.number || '', safeLang)}. ${firstLine}${length ? ` (${length})` : ''}`)
        clueLines.forEach(line => lines.push(line))
      })
    }
    pushGroup(acrossLabel, across)
    pushGroup(downLabel, down)
    return lines
  }

  function buildWordSearchListLines(item, langCode = 'AR', headingText = '') {
    const safeLang = String(langCode || 'AR').toUpperCase() === 'AR' ? 'AR' : 'ID'
    const words = Array.isArray(item?.words)
      ? item.words.map(word => String(word || '').trim()).filter(Boolean)
      : []
    const lines = []
    const safeHeading = String(headingText || '').trim()
    if (safeHeading) lines.push(safeHeading)
    if (!words.length) {
      if (!lines.length) lines.push('-')
      return lines
    }
    const separator = safeLang === 'AR' ? '، ' : ', '
    const maxChars = safeLang === 'AR' ? 34 : 42
    let currentLine = ''
    words.forEach(word => {
      const nextLine = currentLine ? `${currentLine}${separator}${word}` : word
      if (currentLine && nextLine.length > maxChars) {
        lines.push(currentLine)
        currentLine = word
        return
      }
      currentLine = nextLine
    })
    if (currentLine) lines.push(currentLine)
    return lines.length ? lines : ['-']
  }

  function fillWordSearchGridRow(rowNode, gridRow = []) {
    const cells = getRowCells(rowNode)
    cells.forEach((cell, index) => {
      if (index === 0) {
        setCellText(cell, '')
        setCellBorderMode(cell, 'hidden')
        return
      }
      const value = String(Array.isArray(gridRow) ? (gridRow[index - 1] || '') : '').trim()
      setCellBorderMode(cell, value ? 'visible' : 'hidden')
      setCellText(cell, value)
    })
    return rowNode
  }

  function buildCrosswordNumberMap(item) {
    const map = new Map()
    ;[
      ...(Array.isArray(item?.entriesAcross) ? item.entriesAcross : []),
      ...(Array.isArray(item?.entriesDown) ? item.entriesDown : [])
    ].forEach(entry => {
      const key = `${Number(entry?.row || 0)}:${Number(entry?.col || 0)}`
      if (!map.has(key)) map.set(key, Number(entry?.number || 0) || 0)
    })
    return map
  }

  function buildCrosswordBounds(maskRows = []) {
    let minRow = Infinity
    let maxRow = -1
    let minCol = Infinity
    let maxCol = -1
    ;(Array.isArray(maskRows) ? maskRows : []).forEach((rowText, rowIndex) => {
      const safeRow = String(rowText || '')
      for (let colIndex = 0; colIndex < safeRow.length; colIndex += 1) {
        if (safeRow[colIndex] !== '.') continue
        minRow = Math.min(minRow, rowIndex)
        maxRow = Math.max(maxRow, rowIndex)
        minCol = Math.min(minCol, colIndex)
        maxCol = Math.max(maxCol, colIndex)
      }
    })
    if (!Number.isFinite(minRow) || maxRow < 0 || !Number.isFinite(minCol) || maxCol < 0) {
      return { minRow: 0, maxRow: -1, minCol: 0, maxCol: -1 }
    }
    return { minRow, maxRow, minCol, maxCol }
  }

  function fillCrosswordGridRowSingle(rowNode, maskRow = '', rowIndex = 0, numberMap = new Map(), langCode = 'AR', bounds = null) {
    let cells = getRowCells(rowNode)
    const safeMask = String(maskRow || '')
    const rangeMin = Number.isInteger(bounds?.minCol) ? bounds.minCol : 0
    const rangeMax = Number.isInteger(bounds?.maxCol) && bounds.maxCol >= rangeMin ? bounds.maxCol : Math.max(0, safeMask.length - 1)
    const cellTemplate = cells[0]?.cloneNode(true) || null
    const cellWidth = getCellWidth(cells[0]) || 340
    if (cellTemplate) {
      while (rowNode.firstChild) rowNode.removeChild(rowNode.firstChild)
      for (let col = rangeMin; col <= rangeMax; col += 1) {
        const cell = cellTemplate.cloneNode(true)
        clearCellGridSpan(cell)
        setCellWidth(cell, cellWidth)
        const isOpen = safeMask[col] === '.'
        if (!isOpen) {
          setCellBorderMode(cell, 'hidden')
          setCellText(cell, '')
        } else {
          setCellBorderMode(cell, 'visible')
          const number = numberMap.get(`${rowIndex}:${col}`) || ''
          setCellText(cell, number ? formatDocxNumber(number, langCode) : '')
        }
        rowNode.appendChild(cell)
      }
      return rowNode
    }
    cells.forEach((cell, index) => {
      const actualColIndex = rangeMin + index
      const isOpen = actualColIndex <= rangeMax && safeMask[actualColIndex] === '.'
      if (!isOpen) {
        setCellBorderMode(cell, 'hidden')
        setCellText(cell, '')
        return
      }
      setCellBorderMode(cell, 'visible')
      const number = numberMap.get(`${rowIndex}:${actualColIndex}`) || ''
      setCellText(cell, number ? formatDocxNumber(number, langCode) : '')
    })
    return rowNode
  }

  function fillCrosswordGridRow(rowNode, maskRow = '', rowIndex = 0, numberMap = new Map(), langCode = 'AR', bounds = null) {
    let cells = getRowCells(rowNode)
    const safeMask = String(maskRow || '')
    const isRtl = String(langCode || 'AR').toUpperCase() === 'AR'
    const rangeMin = Number.isInteger(bounds?.minCol) ? bounds.minCol : 0
    const rangeMax = Number.isInteger(bounds?.maxCol) && bounds.maxCol >= rangeMin ? bounds.maxCol : Math.max(0, safeMask.length - 1)
    const compactTemplate = cells.length > 0 && cells.length <= 5 && safeMask.length > 0
    if (compactTemplate) {
      const numberTemplate = cells[1]?.cloneNode(true) || cells[0]?.cloneNode(true) || null
      const letterTemplate = cells[2]?.cloneNode(true) || cells[1]?.cloneNode(true) || null
      const numberWidth = getCellWidth(cells[1] || cells[3] || cells[0]) || 120
      const letterWidth = getCellWidth(cells[2] || cells[4] || cells[1]) || 320
      while (rowNode.firstChild) rowNode.removeChild(rowNode.firstChild)
      const visualCols = []
      for (let col = rangeMin; col <= rangeMax; col += 1) visualCols.push(col)
      visualCols.forEach(actualColIndex => {
        if (numberTemplate) {
          const cell = numberTemplate.cloneNode(true)
          clearCellGridSpan(cell)
          setCellWidth(cell, numberWidth)
          const isOpen = safeMask[actualColIndex] === '.'
          if (!isOpen) {
            setCellBorderMode(cell, 'hidden')
            setCellText(cell, '')
          } else {
            setCellBorderSides(cell, { top: 'single', left: 'single', bottom: 'single', right: 'nil' })
            const number = numberMap.get(`${rowIndex}:${actualColIndex}`) || ''
            setCellText(cell, number ? formatDocxNumber(number, langCode) : '')
          }
          rowNode.appendChild(cell)
        }
        if (letterTemplate) {
          const cell = letterTemplate.cloneNode(true)
          clearCellGridSpan(cell)
          setCellWidth(cell, letterWidth)
          const isOpen = safeMask[actualColIndex] === '.'
          if (!isOpen) {
            setCellBorderMode(cell, 'hidden')
            setCellText(cell, '')
          } else {
            setCellBorderSides(cell, { top: 'single', left: 'nil', bottom: 'single', right: 'single' })
            setCellText(cell, '')
          }
          rowNode.appendChild(cell)
        }
      })
      return rowNode
    }
    const expectedPairCells = (safeMask.length * 2) + 1
    if (safeMask.length && cells.length < expectedPairCells && cells.length >= 3) {
      const numberTemplate = cells[1]?.cloneNode(true) || null
      const letterTemplate = cells[2]?.cloneNode(true) || null
      for (let index = cells.length; index < expectedPairCells; index += 1) {
        const template = ((index - 1) % 2) === 0 ? numberTemplate : letterTemplate
        if (!template) break
        rowNode.appendChild(template.cloneNode(true))
      }
      cells = getRowCells(rowNode)
    }
    if (cells.length >= (safeMask.length * 2) + 1) {
      cells.forEach((cell, index) => {
        if (index === 0) {
          setCellText(cell, '')
          setCellBorderMode(cell, 'hidden')
          return
        }
        const pairIndex = Math.floor((index - 1) / 2)
        const actualColIndex = isRtl ? (safeMask.length - 1 - pairIndex) : pairIndex
        const isNumberCell = ((index - 1) % 2) === 0
        const isOpen = safeMask[actualColIndex] === '.'
        if (!isOpen) {
          setCellBorderMode(cell, 'hidden')
          setCellText(cell, '')
          return
        }
        if (isNumberCell) {
          setCellBorderSides(cell, { top: 'single', left: 'single', bottom: 'single', right: 'nil' })
          const number = numberMap.get(`${rowIndex}:${actualColIndex}`) || ''
          setCellText(cell, number ? formatDocxNumber(number, langCode) : '')
        } else {
          setCellBorderSides(cell, { top: 'single', left: 'nil', bottom: 'single', right: 'single' })
          setCellText(cell, '')
        }
      })
      return rowNode
    }
    cells.forEach((cell, index) => {
      if (index === 0) {
        setCellText(cell, '')
        setCellBorderMode(cell, 'hidden')
        return
      }
      const actualColIndex = isRtl ? (safeMask.length - index) : (index - 1)
      const isOpen = safeMask[actualColIndex] === '.'
      if (!isOpen) {
        setCellBorderMode(cell, 'hidden')
        setCellText(cell, '')
        return
      }
      setCellBorderMode(cell, 'visible')
      const number = numberMap.get(`${rowIndex}:${actualColIndex}`) || ''
      setCellText(cell, number ? formatDocxNumber(number, langCode) : '')
    })
    return rowNode
  }

  function fillWordSearchTable(table, section, sectionIndex, templates, headerMeta = {}) {
    clearTableRows(table)
    const headerRow = cloneRow(templates.header)
    setHeaderRow(headerRow, section, sectionIndex, headerMeta.lang || 'AR')
    table.appendChild(headerRow)
    const infoText = resolveSectionInfoText(section, headerMeta)
    if (templates.info && infoText) {
      table.appendChild(fillSectionInfoRow(cloneRow(templates.info), infoText))
    }
    if (templates.spacer) table.appendChild(cloneRow(templates.spacer))

    ;(section.items || []).forEach((item, idx) => {
      const questionTemplate = idx === 0
        ? (templates.firstQuestion || templates.question)
        : (templates.question || templates.firstQuestion)
      const questionRow = cloneRow(questionTemplate)
      const cells = getRowCells(questionRow)
      if (cells[1]) setCellText(cells[1], formatDocxNumber(idx + 1, headerMeta.lang || 'AR'))
      if (cells[2]) setCellText(cells[2], '.')
      if (cells[3]) setCellLines(cells[3], splitPreserveLines(String(item?.text || '')))
      table.appendChild(questionRow)

      if (templates.wordList) {
        const wordListRow = cloneRow(templates.wordList)
        const listCells = getRowCells(wordListRow)
        const targetCell = listCells[listCells.length > 1 ? 1 : 0]
        const headingText = targetCell ? getCellPlainText(targetCell).trim() : ''
        if (targetCell) setCellLines(targetCell, buildWordSearchListLines(item, headerMeta.lang || 'AR', headingText))
        table.appendChild(wordListRow)
      }
      if (templates.wordListSpacer) table.appendChild(cloneRow(templates.wordListSpacer))

      const gridRows = Array.isArray(item?.grid) ? item.grid : []
      if (templates.gridRow && gridRows.length) {
        gridRows.forEach(row => {
          const gridRowNode = cloneRow(templates.gridRow)
          table.appendChild(fillWordSearchGridRow(gridRowNode, row))
        })
      }
    })
  }

  function fillCrosswordTable(table, section, sectionIndex, templates, headerMeta = {}) {
    const items = Array.isArray(section.items) ? section.items : []
    const singleItem = items.length === 1 ? items[0] : null
    const splitGridTemplate = !!templates.gridTable
    if (splitGridTemplate) {
      const headerRows = getTableRows(table)
      if (headerRows[0]) {
        setHeaderRow(headerRows[0], section, sectionIndex, headerMeta.lang || 'AR')
      }
      const outputGridTable = templates.gridTable.cloneNode(true)
      clearTableRows(outputGridTable)
      items.forEach(item => {
        const maskRows = Array.isArray(item?.mask) ? item.mask : []
        const numberMap = buildCrosswordNumberMap(item)
        const bounds = buildCrosswordBounds(maskRows)
        const sourceCells = getRowCells(templates.gridRow)
        const cellWidth = getCellWidth(sourceCells[0]) || 340
        if (bounds.maxCol >= bounds.minCol) {
          setTableGrid(outputGridTable, Array.from({ length: (bounds.maxCol - bounds.minCol + 1) }, () => cellWidth))
        }
        if (templates.gridRow && maskRows.length) {
          maskRows.forEach((maskRow, rowIndex) => {
            if (rowIndex < bounds.minRow || rowIndex > bounds.maxRow) return
            const gridRowNode = cloneRow(templates.gridRow)
            outputGridTable.appendChild(
              fillCrosswordGridRowSingle(gridRowNode, maskRow, rowIndex, numberMap, headerMeta.lang || 'AR', bounds)
            )
          })
        }
      })
      return { gridTable: outputGridTable }
    }

    clearTableRows(table)
    const headerRow = cloneRow(templates.header)
    setHeaderRow(headerRow, section, sectionIndex, headerMeta.lang || 'AR')
    table.appendChild(headerRow)
    const infoText = resolveSectionInfoText(section, headerMeta)
    const compactTemplate = !templates.question && !templates.wordList && !!templates.gridRow && !splitGridTemplate
    const infoLines = infoText ? splitPreserveLines(infoText) : []
    if (compactTemplate && singleItem) {
      const questionLines = splitPreserveLines(String(singleItem?.text || '').trim()).filter(line => String(line || '').trim())
      const clueLines = buildCrosswordClueLines(singleItem, headerMeta.lang || 'AR')
      if (questionLines.length) {
        if (infoLines.length) infoLines.push('')
        infoLines.push(...questionLines)
      }
      if (clueLines.length) {
        if (infoLines.length) infoLines.push('')
        infoLines.push(...clueLines)
      }
    }
    if (templates.info && infoLines.some(line => String(line || '').trim())) {
      table.appendChild(fillSectionInfoRow(cloneRow(templates.info), infoLines))
    }
    if (templates.spacer) table.appendChild(cloneRow(templates.spacer))
    if (compactTemplate && templates.gridRow && singleItem) {
      const sourceCells = getRowCells(templates.gridRow)
      const numberWidth = getCellWidth(sourceCells[1] || sourceCells[3] || sourceCells[0]) || 120
      const letterWidth = getCellWidth(sourceCells[2] || sourceCells[4] || sourceCells[1]) || 320
      const bounds = buildCrosswordBounds(Array.isArray(singleItem?.mask) ? singleItem.mask : [])
      if (bounds.maxCol >= bounds.minCol) {
        const colWidths = []
        for (let col = bounds.minCol; col <= bounds.maxCol; col += 1) {
          colWidths.push(numberWidth, letterWidth)
        }
        setTableGrid(table, colWidths)
      }
    }
    items.forEach((item, idx) => {
      const questionTemplate = idx === 0
        ? (templates.firstQuestion || templates.question)
        : (templates.question || templates.firstQuestion)
      if (questionTemplate) {
        const questionRow = cloneRow(questionTemplate)
        const cells = getRowCells(questionRow)
        if (cells[1]) setCellText(cells[1], formatDocxNumber(idx + 1, headerMeta.lang || 'AR'))
        if (cells[2]) setCellText(cells[2], '.')
        if (cells[3] || cells[cells.length - 1]) {
          const targetCell = cells[3] || cells[cells.length - 1]
          setCellLines(targetCell, splitPreserveLines(String(item?.text || '')))
        }
        table.appendChild(questionRow)
      }

      const clueLines = buildCrosswordClueLines(item, headerMeta.lang || 'AR')
      if (templates.wordList && clueLines.length) {
        const clueRow = cloneRow(templates.wordList)
        const clueCells = getRowCells(clueRow)
        const targetCell = clueCells[1] || clueCells[clueCells.length - 1] || clueCells[0]
        if (targetCell) setCellLines(targetCell, clueLines)
        table.appendChild(clueRow)
        if (templates.wordListSpacer) table.appendChild(cloneRow(templates.wordListSpacer))
      }

      const maskRows = Array.isArray(item?.mask) ? item.mask : []
      const numberMap = buildCrosswordNumberMap(item)
      const bounds = buildCrosswordBounds(maskRows)
      if (templates.gridRow && maskRows.length) {
        maskRows.forEach((maskRow, rowIndex) => {
          if (rowIndex < bounds.minRow || rowIndex > bounds.maxRow) return
          const gridRowNode = cloneRow(templates.gridRow)
          table.appendChild(fillCrosswordGridRow(gridRowNode, maskRow, rowIndex, numberMap, headerMeta.lang || 'AR', bounds))
        })
      }
    })
    return null
  }

  function fillMatchingTable(table, section, sectionIndex, templates, headerMeta = {}) {
    clearTableRows(table)
    const headerRow = cloneRow(templates.header)
    setHeaderRow(headerRow, section, sectionIndex, headerMeta.lang || 'AR')
    table.appendChild(headerRow)
    const infoText = resolveSectionInfoText(section, headerMeta)
    if (templates.info && infoText) {
      table.appendChild(fillSectionInfoRow(cloneRow(templates.info), infoText))
    }
    if (templates.spacer) table.appendChild(cloneRow(templates.spacer))

    const titleRow = cloneRow(templates.titles)
    table.appendChild(titleRow)

    const contentRow = cloneRow(templates.content)
    const cells = getRowCells(contentRow)
    const items = Array.isArray(section.items) ? section.items : []
    const columnA = []
    const columnB = []
    items.forEach(item => {
      ;(Array.isArray(item?.columnA) ? item.columnA : []).forEach(val => {
        const text = String(val || '').trim()
        if (text) columnA.push(text)
      })
      ;(Array.isArray(item?.columnB) ? item.columnB : []).forEach(val => {
        const text = String(val || '').trim()
        if (text) columnB.push(text)
      })
    })
    if (cells[1]) setCellLines(cells[1], columnA)
    if (cells[2]) setCellLines(cells[2], columnB)
    table.appendChild(contentRow)
  }

  function buildCrosswordPrototype(table, gridTable = null) {
    const rows = getTableRows(table)
    const compact = rows.length <= 4
    return {
      table: table.cloneNode(true),
      header: cloneRow(pickTemplateRow(table, [0])),
      info: maybeCloneRow(pickTemplateRow(table, [1])),
      spacer: maybeCloneRow(pickTemplateRow(table, [2])),
      question: (compact || gridTable) ? null : maybeCloneRow(pickTemplateRow(table, [3])),
      wordList: (compact || gridTable) ? null : maybeCloneRow(pickTemplateRow(table, [4])),
      wordListSpacer: (compact || gridTable) ? null : maybeCloneRow(pickTemplateRow(table, [5])),
      gridRow: maybeCloneRow(pickTemplateRow(gridTable || table, [gridTable ? 0 : (compact ? 3 : 6)])),
      gridTable: gridTable ? gridTable.cloneNode(true) : null
    }
  }

  function fillFillBlankTable(table, section, sectionIndex, templates, headerMeta = {}) {
    clearTableRows(table)
    const headerRow = cloneRow(templates.header)
    setHeaderRow(headerRow, section, sectionIndex, headerMeta.lang || 'AR')
    table.appendChild(headerRow)
    const infoText = resolveSectionInfoText(section, headerMeta)
    if (templates.info && infoText) {
      table.appendChild(fillSectionInfoRow(cloneRow(templates.info), infoText))
    }
    if (templates.spacer) table.appendChild(cloneRow(templates.spacer))

    const bankRow = cloneRow(templates.wordBank)
    const bankCells = getRowCells(bankRow)
    const wordBank = uniqueWordBank(section.items)
    const separator = String(headerMeta.lang || 'AR').toUpperCase() === 'AR' ? '، ' : ', '
    const bankText = wordBank.length ? `(${wordBank.join(separator)})` : ''
    if (bankCells[1]) setCellText(bankCells[1], bankText)
    if (bankCells[2]) setCellText(bankCells[2], '')
    table.appendChild(bankRow)
    if (templates.wordBankSpacer) table.appendChild(cloneRow(templates.wordBankSpacer))

    ;(section.items || []).forEach((item, idx) => {
      const questionRow = cloneRow(idx === 0 ? templates.firstQuestion : templates.question)
      const cells = getRowCells(questionRow)
      if (cells[1]) setCellText(cells[1], formatDocxNumber(idx + 1, headerMeta.lang || 'AR'))
      if (cells[2]) setCellText(cells[2], '.')
      if (cells[3]) setCellText(cells[3], String(item?.text || ''))
      table.appendChild(questionRow)
    })
  }

  function extractTemplateTables(doc) {
    const body = getBodyNodes(doc)
    const bodyChildren = Array.from(body.childNodes || [])
    const sectionPr = bodyChildren.find(node => node?.namespaceURI === WORD_NS && node?.localName === 'sectPr') || null
    const tables = bodyChildren.filter(node => node?.namespaceURI === WORD_NS && node?.localName === 'tbl')
    const paragraphs = bodyChildren.filter(node => node?.namespaceURI === WORD_NS && node?.localName === 'p')
    const spacerParagraph = paragraphs.find(node => {
      const text = ((qsa(node, 't') || []).map(entry => String(entry?.textContent || '')).join('')).trim()
      return !text
    }) || paragraphs[paragraphs.length - 1] || null
    if (tables.length < 8) {
      throw new Error('Template ExamAR tidak lengkap. Tabel soal Arab termasuk Cari Kata belum ditemukan utuh.')
    }
    const hasHeaderInfoTable = tables.length >= 6
    const tableOffset = hasHeaderInfoTable ? 1 : 0
    const splitCrossword = tables.length >= 10 && getTableRows(tables[tables.length - 1]).length === 1
    const wordSearchTable = splitCrossword ? (tables[tables.length - 3] || tables[7]) : (tables[tables.length - 2] || tables[7])
    const crosswordTable = splitCrossword ? (tables[tables.length - 2] || tables[7]) : (tables[tables.length - 1] || tables[7])
    const crosswordGridTable = splitCrossword ? tables[tables.length - 1] : null
    return {
      body,
      sectionPr,
      spacerParagraph,
      headerParagraphs: (hasHeaderInfoTable ? paragraphs.slice(0, 3) : paragraphs.slice(0, 6)).map(node => node.cloneNode(true)),
      headerInfoTable: hasHeaderInfoTable ? tables[0].cloneNode(true) : null,
      baseTableWidth: getTableWidth(tables[1 + tableOffset]) || 9266,
      paragraphTemplates: {
        heading: first(getRowCells(pickTemplateRow(tables[1 + tableOffset], [0]))[2], 'p', true)?.cloneNode(true) || null,
        body: first(getRowCells(pickTemplateRow(tables[1 + tableOffset], [2, 1, 0]))[3], 'p', true)?.cloneNode(true) || null
      },
      prototypes: {
        generalInfo: {
          table: tables[0 + tableOffset].cloneNode(true),
          row: cloneRow(pickTemplateRow(tables[0 + tableOffset], [0]))
        },
        'pilihan-ganda': {
          table: tables[1 + tableOffset].cloneNode(true),
          header: cloneRow(pickTemplateRow(tables[1 + tableOffset], [0])),
          info: cloneRow(pickTemplateRow(tables[1 + tableOffset], [1])),
          question: cloneRow(pickTemplateRow(tables[1 + tableOffset], [2, 1])),
          options: cloneRow(pickTemplateRow(tables[1 + tableOffset], [3, 4, 5, 2])),
          optionsSingle: cloneRow(pickTemplateRow(tables[1 + tableOffset], [3, 4, 5])),
          optionsDouble: cloneRow(pickTemplateRow(tables[1 + tableOffset], [4, 3, 5])),
          optionsStack: cloneRow(pickTemplateRow(tables[1 + tableOffset], [5, 4, 3]))
        },
        esai: {
          table: tables[2 + tableOffset].cloneNode(true),
          header: cloneRow(pickTemplateRow(tables[2 + tableOffset], [0])),
          info: cloneRow(pickTemplateRow(tables[2 + tableOffset], [1])),
          firstQuestion: cloneRow(pickTemplateRow(tables[2 + tableOffset], [2, 1])),
          question: cloneRow(pickTemplateRow(tables[2 + tableOffset], [2, 1]))
        },
        'cari-kata': {
          table: wordSearchTable.cloneNode(true),
          header: cloneRow(pickTemplateRow(wordSearchTable, [0])),
          info: cloneRow(pickTemplateRow(wordSearchTable, [1])),
          question: cloneRow(pickTemplateRow(wordSearchTable, [2])),
          wordList: cloneRow(pickTemplateRow(wordSearchTable, [3])),
          wordListSpacer: cloneRow(pickTemplateRow(wordSearchTable, [4])),
          gridRow: cloneRow(pickTemplateRow(wordSearchTable, [5]))
        },
        'teka-silang': buildCrosswordPrototype(crosswordTable, crosswordGridTable),
        'pasangkan-kata': {
          table: tables[3 + tableOffset].cloneNode(true),
          header: cloneRow(pickTemplateRow(tables[3 + tableOffset], [0])),
          info: cloneRow(pickTemplateRow(tables[3 + tableOffset], [1])),
          titles: cloneRow(pickTemplateRow(tables[3 + tableOffset], [2, 1])),
          content: cloneRow(pickTemplateRow(tables[3 + tableOffset], [3, 2]))
        },
        'isi-titik': {
          table: tables[4 + tableOffset].cloneNode(true),
          header: cloneRow(pickTemplateRow(tables[4 + tableOffset], [0])),
          info: cloneRow(pickTemplateRow(tables[4 + tableOffset], [1])),
          wordBank: cloneRow(pickTemplateRow(tables[4 + tableOffset], [2, 1])),
          firstQuestion: cloneRow(pickTemplateRow(tables[4 + tableOffset], [3, 2])),
          question: cloneRow(pickTemplateRow(tables[4 + tableOffset], [3, 2]))
        },
        'benar-salah': tables[5 + tableOffset] ? {
          table: tables[5 + tableOffset].cloneNode(true),
          header: cloneRow(pickTemplateRow(tables[5 + tableOffset], [0])),
          info: cloneRow(pickTemplateRow(tables[5 + tableOffset], [1])),
          question: cloneRow(pickTemplateRow(tables[5 + tableOffset], [2])),
          options: cloneRow(pickTemplateRow(tables[5 + tableOffset], [3]))
        } : null
      }
    }
  }

  function rebuildBody(doc, sections, headerMeta = {}) {
    const templateData = extractTemplateTables(doc)
    const { body, sectionPr, spacerParagraph, prototypes, headerInfoTable } = templateData
    Array.from(body.childNodes || []).forEach(node => {
      if (node !== sectionPr) body.removeChild(node)
    })

    const headerParagraphs = templateData.headerParagraphs.map(node => node.cloneNode(true))
    applyHeaderParagraphs(headerParagraphs, headerMeta)
    headerParagraphs.forEach(node => body.insertBefore(node, sectionPr))
    if (headerInfoTable) body.insertBefore(fillHeaderInfoTableArabic(headerInfoTable.cloneNode(true), headerMeta), sectionPr)
    if (spacerParagraph) body.insertBefore(spacerParagraph.cloneNode(true), sectionPr)

    const generalInstruction = String(headerMeta.generalInstruction || '')
    if (generalInstruction) {
      const generalTable = prototypes.generalInfo.table.cloneNode(true)
      clearTableRows(generalTable)
      const infoRow = cloneRow(prototypes.generalInfo.row)
      const infoCells = getRowCells(infoRow)
      const isArabic = String(headerMeta.lang || 'ID').toUpperCase() === 'AR'
      const instructionLines = splitPreserveLines(generalInstruction)
      if (infoCells[0]) {
        if (isArabic) {
          const heading = getCellPlainText(infoCells[0]).trim() || 'ØªØ¹Ù„ÙŠÙ…Ø§Øª Ø¹Ø§Ù…Ø©'
          setCellLines(infoCells[0], [heading, ...instructionLines])
        } else {
          setCellText(infoCells[0], '')
        }
      }
      if (infoCells[1]) {
        if (isArabic) {
          setCellText(infoCells[1], '')
        } else {
          const heading = getCellPlainText(infoCells[1]).trim() || 'Instruksi Umum'
          setCellLines(infoCells[1], [heading, ...instructionLines])
        }
      }
      generalTable.appendChild(infoRow)
      body.insertBefore(generalTable, sectionPr)
      if (spacerParagraph) body.insertBefore(spacerParagraph.cloneNode(true), sectionPr)
    }

    const activeSections = (Array.isArray(sections) ? sections : []).filter(section => {
      const items = Array.isArray(section?.items) ? section.items : []
      return items.length > 0
    })
    activeSections.forEach((section, sectionIndex) => {
      const proto = prototypes[section.type]
      if (!proto) return
      const table = proto.table.cloneNode(true)
      let extra = null
      if (section.type === 'pilihan-ganda') fillPgTable(table, section, sectionIndex, proto, headerMeta)
      else if (section.type === 'benar-salah') fillTrueFalseTable(table, section, sectionIndex, proto, headerMeta)
      else if (section.type === 'esai') fillEssayTable(table, section, sectionIndex, proto, headerMeta)
      else if (section.type === 'cari-kata') fillWordSearchTable(table, section, sectionIndex, proto, headerMeta)
      else if (section.type === 'teka-silang') extra = fillCrosswordTable(table, section, sectionIndex, proto, headerMeta)
      else if (section.type === 'pasangkan-kata') fillMatchingTable(table, section, sectionIndex, proto, headerMeta)
      else if (section.type === 'isi-titik') fillFillBlankTable(table, section, sectionIndex, proto, headerMeta)
      body.insertBefore(table, sectionPr)
      if (extra?.gridTable) body.insertBefore(extra.gridTable, sectionPr)
      if (spacerParagraph) body.insertBefore(spacerParagraph.cloneNode(true), sectionPr)
    })
  }

  function extractTemplateTablesIndonesian(doc) {
    const body = getBodyNodes(doc)
    const bodyChildren = Array.from(body.childNodes || [])
    const sectionPr = bodyChildren.find(node => node?.namespaceURI === WORD_NS && node?.localName === 'sectPr') || null
    const tables = bodyChildren.filter(node => node?.namespaceURI === WORD_NS && node?.localName === 'tbl')
    const paragraphs = bodyChildren.filter(node => node?.namespaceURI === WORD_NS && node?.localName === 'p')
    const spacerParagraph = paragraphs.find(node => {
      const text = ((qsa(node, 't') || []).map(entry => String(entry?.textContent || '')).join('')).trim()
      return !text
    }) || paragraphs[paragraphs.length - 1] || null
    if (tables.length < 8) {
      throw new Error('Template ExamIN tidak lengkap. Tabel soal Indonesia termasuk Cari Kata belum ditemukan utuh.')
    }
    const splitCrossword = tables.length >= 10 && getTableRows(tables[tables.length - 1]).length === 1
    const wordSearchTable = splitCrossword ? (tables[tables.length - 3] || tables[7]) : (tables[tables.length - 2] || tables[7])
    const crosswordTable = splitCrossword ? (tables[tables.length - 2] || tables[8] || tables[7]) : (tables[tables.length - 1] || tables[8] || tables[7])
    const crosswordGridTable = splitCrossword ? tables[tables.length - 1] : null
    return {
      body,
      sectionPr,
      spacerParagraph,
      headerParagraphs: paragraphs.slice(0, 3).map(node => node.cloneNode(true)),
      headerInfoTable: tables[0].cloneNode(true),
      prototypes: {
        generalInfo: {
          table: tables[1].cloneNode(true),
          row: cloneRow(pickTemplateRow(tables[1], [0]))
        },
        'pilihan-ganda': {
          table: tables[2].cloneNode(true),
          header: cloneRow(pickTemplateRow(tables[2], [0])),
          info: cloneRow(pickTemplateRow(tables[2], [1])),
          spacer: cloneRow(pickTemplateRow(tables[2], [2])),
          question: cloneRow(pickTemplateRow(tables[2], [3])),
          options: cloneRow(pickTemplateRow(tables[2], [4, 5, 6])),
          optionsSingle: cloneRow(pickTemplateRow(tables[2], [4, 5, 6])),
          optionsDouble: cloneRow(pickTemplateRow(tables[2], [5, 4, 6])),
          optionsStack: cloneRow(pickTemplateRow(tables[2], [6, 5, 4]))
        },
        esai: {
          table: tables[3].cloneNode(true),
          header: cloneRow(pickTemplateRow(tables[3], [0])),
          info: cloneRow(pickTemplateRow(tables[3], [1])),
          spacer: cloneRow(pickTemplateRow(tables[3], [2])),
          firstQuestion: cloneRow(pickTemplateRow(tables[3], [3])),
          question: cloneRow(pickTemplateRow(tables[3], [3]))
        },
        'cari-kata': {
          table: wordSearchTable.cloneNode(true),
          header: cloneRow(pickTemplateRow(wordSearchTable, [0])),
          info: cloneRow(pickTemplateRow(wordSearchTable, [1])),
          spacer: cloneRow(pickTemplateRow(wordSearchTable, [2])),
          question: cloneRow(pickTemplateRow(wordSearchTable, [3])),
          wordList: cloneRow(pickTemplateRow(wordSearchTable, [4])),
          wordListSpacer: cloneRow(pickTemplateRow(wordSearchTable, [5])),
          gridRow: cloneRow(pickTemplateRow(wordSearchTable, [6]))
        },
        'teka-silang': buildCrosswordPrototype(crosswordTable, crosswordGridTable),
        'pasangkan-kata': {
          table: tables[4].cloneNode(true),
          header: cloneRow(pickTemplateRow(tables[4], [0])),
          info: cloneRow(pickTemplateRow(tables[4], [1])),
          spacer: cloneRow(pickTemplateRow(tables[4], [2])),
          titles: cloneRow(pickTemplateRow(tables[4], [3])),
          content: cloneRow(pickTemplateRow(tables[4], [4]))
        },
        'isi-titik': {
          table: tables[5].cloneNode(true),
          header: cloneRow(pickTemplateRow(tables[5], [0])),
          info: cloneRow(pickTemplateRow(tables[5], [1])),
          spacer: cloneRow(pickTemplateRow(tables[5], [2])),
          wordBank: cloneRow(pickTemplateRow(tables[5], [3])),
          wordBankSpacer: cloneRow(pickTemplateRow(tables[5], [4])),
          firstQuestion: cloneRow(pickTemplateRow(tables[5], [5])),
          question: cloneRow(pickTemplateRow(tables[5], [5]))
        },
        'benar-salah': tables[6] ? {
          table: tables[6].cloneNode(true),
          header: cloneRow(pickTemplateRow(tables[6], [0])),
          info: cloneRow(pickTemplateRow(tables[6], [1])),
          spacer: cloneRow(pickTemplateRow(tables[6], [2])),
          question: cloneRow(pickTemplateRow(tables[6], [3])),
          options: cloneRow(pickTemplateRow(tables[6], [4]))
        } : null
      }
    }
  }

  function applyHeaderParagraphsIndonesian(paragraphs = [], headerMeta = {}) {
    if (!Array.isArray(paragraphs) || !paragraphs.length) return
    if (paragraphs[0]) setParagraphTextPreserveGraphics(paragraphs[0], String(headerMeta.examTitle || '').trim())
    if (paragraphs[1]) setParagraphText(paragraphs[1], String(headerMeta.schoolName || '').trim())
    if (paragraphs[2]) setParagraphText(paragraphs[2], String(headerMeta.schoolYear || '').trim())
  }

  function fillHeaderInfoTableIndonesian(table, headerMeta = {}) {
    const rows = getTableRows(table)
    const row0 = rows[0]
    const row1 = rows[1]
    const row2 = rows[2]
    const cells0 = getRowCells(row0)
    const cells1 = getRowCells(row1)
    const cells2 = getRowCells(row2)
    if (cells0[2]) setCellText(cells0[2], String(headerMeta.subject || '-'))
    if (cells0[5]) setCellText(cells0[5], String(headerMeta.examDate || '-'))
    if (cells1[2]) setCellText(cells1[2], String(headerMeta.className || '-'))
    if (cells1[5]) setCellText(cells1[5], String(headerMeta.duration || '-'))
    if (cells2[2]) setCellText(cells2[2], String(headerMeta.studentLine || '_______________________________'))
    return table
  }

  function fillHeaderInfoTableArabic(table, headerMeta = {}) {
    const rows = getTableRows(table)
    const row0 = rows[0]
    const row1 = rows[1]
    const row2 = rows[2]
    const cells0 = getRowCells(row0)
    const cells1 = getRowCells(row1)
    const cells2 = getRowCells(row2)
    if (cells0[2]) setCellText(cells0[2], String(headerMeta.subject || '-'))
    if (cells0[5]) setCellText(cells0[5], String(headerMeta.examDate || '-'))
    if (cells1[2]) setCellText(cells1[2], String(headerMeta.className || '-'))
    if (cells1[5]) setCellText(cells1[5], String(headerMeta.duration || '-'))
    if (cells2[2]) setCellText(cells2[2], String(headerMeta.studentLine || '_______________________________'))
    return table
  }

  function rebuildBodyIndonesian(doc, sections, headerMeta = {}) {
    const templateData = extractTemplateTablesIndonesian(doc)
    const { body, sectionPr, spacerParagraph, prototypes, headerInfoTable } = templateData
    Array.from(body.childNodes || []).forEach(node => {
      if (node !== sectionPr) body.removeChild(node)
    })

    const headerParagraphs = templateData.headerParagraphs.map(node => node.cloneNode(true))
    applyHeaderParagraphsIndonesian(headerParagraphs, headerMeta)
    headerParagraphs.forEach(node => body.insertBefore(node, sectionPr))
    body.insertBefore(fillHeaderInfoTableIndonesian(headerInfoTable.cloneNode(true), headerMeta), sectionPr)
    if (spacerParagraph) body.insertBefore(spacerParagraph.cloneNode(true), sectionPr)

    const generalInstruction = String(headerMeta.generalInstruction || '')
    if (generalInstruction.trim()) {
      const generalTable = prototypes.generalInfo.table.cloneNode(true)
      clearTableRows(generalTable)
      const infoRow = cloneRow(prototypes.generalInfo.row)
      const infoCells = getRowCells(infoRow)
      if (infoCells[0]) {
        const heading = getCellPlainText(infoCells[0]).trim() || 'Instruksi Umum'
        setCellLines(infoCells[0], [heading, ...splitPreserveLines(generalInstruction)])
      }
      generalTable.appendChild(infoRow)
      body.insertBefore(generalTable, sectionPr)
      if (spacerParagraph) body.insertBefore(spacerParagraph.cloneNode(true), sectionPr)
    }

    const activeSections = (Array.isArray(sections) ? sections : []).filter(section => {
      const items = Array.isArray(section?.items) ? section.items : []
      return items.length > 0
    })
    activeSections.forEach((section, sectionIndex) => {
      const proto = prototypes[section.type]
      if (!proto) return
      const table = proto.table.cloneNode(true)
      let extra = null
      if (section.type === 'pilihan-ganda') fillPgTable(table, section, sectionIndex, proto, { ...headerMeta, lang: 'ID' })
      else if (section.type === 'benar-salah') fillTrueFalseTable(table, section, sectionIndex, proto, { ...headerMeta, lang: 'ID' })
      else if (section.type === 'esai') fillEssayTable(table, section, sectionIndex, proto, { ...headerMeta, lang: 'ID' })
      else if (section.type === 'cari-kata') fillWordSearchTable(table, section, sectionIndex, proto, { ...headerMeta, lang: 'ID' })
      else if (section.type === 'teka-silang') extra = fillCrosswordTable(table, section, sectionIndex, proto, { ...headerMeta, lang: 'ID' })
      else if (section.type === 'pasangkan-kata') fillMatchingTable(table, section, sectionIndex, proto, { ...headerMeta, lang: 'ID' })
      else if (section.type === 'isi-titik') fillFillBlankTable(table, section, sectionIndex, proto, { ...headerMeta, lang: 'ID' })
      body.insertBefore(table, sectionPr)
      if (extra?.gridTable) body.insertBefore(extra.gridTable, sectionPr)
      if (spacerParagraph) body.insertBefore(spacerParagraph.cloneNode(true), sectionPr)
    })
  }

  function serializeXml(doc) {
    const xml = new XMLSerializer().serializeToString(doc)
    return xml.startsWith('<?xml')
      ? xml
      : `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>${xml}`
  }

  async function loadTemplateZip(templatePath = TEMPLATE_PATH_AR) {
    if (!window.JSZip || typeof window.JSZip.loadAsync !== 'function') {
      throw new Error('Library DOCX belum termuat. Refresh halaman lalu coba lagi.')
    }
    const templateUrl = new URL(String(templatePath || TEMPLATE_PATH_AR), window.location.href)
    const response = await fetch(templateUrl.toString(), { cache: 'no-cache' })
    if (!response.ok) {
      throw new Error(`Template ${String(templatePath || TEMPLATE_PATH_AR)} tidak ditemukan.`)
    }
    const buffer = await response.arrayBuffer()
    return window.JSZip.loadAsync(buffer)
  }

  function triggerBlobDownload(blob, fileName) {
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = fileName
    document.body.appendChild(anchor)
    anchor.click()
    document.body.removeChild(anchor)
    setTimeout(() => URL.revokeObjectURL(url), 1500)
  }

  async function exportArabicExamDocx(jadwal, soal, fileName = 'Soal-Arab.docx', headerMeta = {}) {
    const payload = typeof soal?.questions_json === 'string'
      ? JSON.parse(soal.questions_json || '[]')
      : (Array.isArray(soal?.questions_json) ? soal.questions_json : [])
    const questions = Array.isArray(payload?.questions) ? payload.questions : (Array.isArray(payload) ? payload : [])
    if (!questions.length) {
      throw new Error('Belum ada soal untuk diexport.')
    }
    const fallbackType = String(soal?.bentuk_soal || 'pilihan-ganda')
    const sections = buildSectionsFromQuestions(questions, fallbackType)
    if (!sections.length) {
      throw new Error('Bagian soal Arab tidak berhasil dibentuk.')
    }

    const zip = await loadTemplateZip(TEMPLATE_PATH_AR)
    const documentXmlFile = zip.file('word/document.xml')
    if (!documentXmlFile) throw new Error('Template DOCX tidak memiliki word/document.xml')
    const xmlString = await documentXmlFile.async('string')
    const parser = new DOMParser()
    const doc = parser.parseFromString(xmlString, 'application/xml')
    const parserError = doc.getElementsByTagName('parsererror')[0]
    if (parserError) {
      throw new Error('Template DOCX tidak dapat dibaca.')
    }

    rebuildBody(doc, sections, { ...headerMeta, lang: 'AR' })
    zip.file('word/document.xml', serializeXml(doc))
    const blob = await zip.generateAsync({
      type: 'blob',
      mimeType: DOCX_MIME,
      compression: 'DEFLATE',
      compressionOptions: { level: 6 }
    })
    triggerBlobDownload(blob, fileName)
    return { ok: true, fileName, jadwal }
  }

  async function exportIndonesianExamDocx(jadwal, soal, fileName = 'Soal-Indonesia.docx', headerMeta = {}) {
    const payload = typeof soal?.questions_json === 'string'
      ? JSON.parse(soal.questions_json || '[]')
      : (Array.isArray(soal?.questions_json) ? soal.questions_json : [])
    const questions = Array.isArray(payload?.questions) ? payload.questions : (Array.isArray(payload) ? payload : [])
    if (!questions.length) {
      throw new Error('Belum ada soal untuk diexport.')
    }
    const fallbackType = String(soal?.bentuk_soal || 'pilihan-ganda')
    const sections = buildSectionsFromQuestions(questions, fallbackType)
    if (!sections.length) {
      throw new Error('Bagian soal Indonesia tidak berhasil dibentuk.')
    }

    const zip = await loadTemplateZip(TEMPLATE_PATH_ID)
    const documentXmlFile = zip.file('word/document.xml')
    if (!documentXmlFile) throw new Error('Template DOCX tidak memiliki word/document.xml')
    const xmlString = await documentXmlFile.async('string')
    const parser = new DOMParser()
    const doc = parser.parseFromString(xmlString, 'application/xml')
    const parserError = doc.getElementsByTagName('parsererror')[0]
    if (parserError) {
      throw new Error('Template DOCX tidak dapat dibaca.')
    }

    rebuildBodyIndonesian(doc, sections, { ...headerMeta, lang: 'ID' })
    zip.file('word/document.xml', serializeXml(doc))
    const blob = await zip.generateAsync({
      type: 'blob',
      mimeType: DOCX_MIME,
      compression: 'DEFLATE',
      compressionOptions: { level: 6 }
    })
    triggerBlobDownload(blob, fileName)
    return { ok: true, fileName, jadwal }
  }

  window.guruExamDocxUtils = {
    exportArabicExamDocx,
    exportIndonesianExamDocx
  }
})()

