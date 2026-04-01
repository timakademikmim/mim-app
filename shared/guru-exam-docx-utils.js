;(function initGuruExamDocxUtils() {
  if (window.guruExamDocxUtils) return

  const WORD_NS = 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'
  const DOCX_MIME = 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
  const TEMPLATE_PATH = 'ExamAR.docx'

  function getPrintUtils() {
    return window.guruExamPrintUtils || {}
  }

  function getArabicSectionParts(type, index) {
    const printUtils = getPrintUtils()
    if (typeof printUtils.getExamPrintTypeParts === 'function') {
      return printUtils.getExamPrintTypeParts(type, index, 'AR')
    }
    const markerLetters = ['أ', 'ب', 'ج', 'د', 'هـ', 'و']
    const marker = markerLetters[index % markerLetters.length] || 'أ'
    let label = 'اختيار من متعدد'
    if (type === 'esai') label = 'مقال'
    else if (type === 'pasangkan-kata') label = 'وصل الكلمات'
    else if (type === 'isi-titik') label = 'املأ الفراغ'
    return { marker, label }
  }

  function buildSectionsFromQuestions(questions, fallbackType) {
    const printUtils = getPrintUtils()
    if (typeof printUtils.buildExamPrintSections === 'function') {
      return printUtils.buildExamPrintSections(questions, fallbackType)
    }
    return []
  }

  function getPgOptionEntries(question) {
    const printUtils = getPrintUtils()
    if (typeof window.guruExamHtmlUtils?.getExamPgOptionEntries === 'function') {
      return window.guruExamHtmlUtils.getExamPgOptionEntries(question?.options || {}, true)
    }
    const fallbackLetters = ['أ', 'ب', 'ج', 'د']
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

  function getBodyNodes(doc) {
    return first(doc.documentElement, 'body', true)
  }

  function getTableRows(table) {
    return qsa(table, 'tr', true)
  }

  function getRowCells(row) {
    return qsa(row, 'tc', true)
  }

  function removeAllRuns(paragraph) {
    Array.from(paragraph.childNodes || []).forEach(child => {
      if (child?.namespaceURI === WORD_NS && child?.localName !== 'pPr') {
        paragraph.removeChild(child)
      }
    })
  }

  function createRunWithTemplate(doc, paragraph) {
    const existingRun = first(paragraph, 'r', true)
    if (existingRun) {
      const clone = existingRun.cloneNode(true)
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
    removeAllRuns(paragraph)
    const { run, textNode } = createRunWithTemplate(doc, paragraph)
    const safeText = String(text ?? '')
    if (/^\s|\s$/.test(safeText)) {
      textNode.setAttributeNS('http://www.w3.org/XML/1998/namespace', 'xml:space', 'preserve')
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
    const safeLines = (Array.isArray(lines) ? lines : [])
      .map(line => String(line || '').trim())
      .filter(Boolean)
    if (!safeLines.length) {
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

  function setHeaderRow(headerRow, section, sectionIndex) {
    const cells = getRowCells(headerRow)
    const parts = getArabicSectionParts(section.type, sectionIndex)
    if (cells[0]) setCellText(cells[0], parts.marker)
    if (cells[1]) setCellText(cells[1], '.')
    if (cells[2]) setCellText(cells[2], parts.label)
  }

  function cloneRow(row) {
    return row.cloneNode(true)
  }

  function fillPgTable(table, section, sectionIndex, templates) {
    clearTableRows(table)
    const headerRow = cloneRow(templates.header)
    setHeaderRow(headerRow, section, sectionIndex)
    table.appendChild(headerRow)

    ;(section.items || []).forEach(item => {
      const questionRow = cloneRow(templates.question)
      const qCells = getRowCells(questionRow)
      if (qCells[1]) setCellText(qCells[1], String(item?.no || ''))
      if (qCells[2]) setCellText(qCells[2], '.')
      if (qCells[3]) setCellText(qCells[3], String(item?.text || ''))
      table.appendChild(questionRow)

      const optionRow = cloneRow(templates.options)
      const oCells = getRowCells(optionRow)
      const entries = getPgOptionEntries(item).slice(0, 4)
      const valueCells = [3, 6, 9, 12]
      valueCells.forEach((cellIndex, idx) => {
        if (oCells[cellIndex]) setCellText(oCells[cellIndex], String(entries[idx]?.value || ''))
      })
      table.appendChild(optionRow)
    })
  }

  function fillEssayTable(table, section, sectionIndex, templates) {
    clearTableRows(table)
    const headerRow = cloneRow(templates.header)
    setHeaderRow(headerRow, section, sectionIndex)
    table.appendChild(headerRow)

    ;(section.items || []).forEach((item, idx) => {
      const questionRow = cloneRow(idx === 0 ? templates.firstQuestion : templates.question)
      const cells = getRowCells(questionRow)
      if (cells[1]) setCellText(cells[1], String(item?.no || ''))
      if (cells[2]) setCellText(cells[2], '.')
      if (cells[3]) setCellText(cells[3], String(item?.text || ''))
      table.appendChild(questionRow)
    })
  }

  function fillMatchingTable(table, section, sectionIndex, templates) {
    clearTableRows(table)
    const headerRow = cloneRow(templates.header)
    setHeaderRow(headerRow, section, sectionIndex)
    table.appendChild(headerRow)

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

  function fillFillBlankTable(table, section, sectionIndex, templates) {
    clearTableRows(table)
    const headerRow = cloneRow(templates.header)
    setHeaderRow(headerRow, section, sectionIndex)
    table.appendChild(headerRow)

    const bankRow = cloneRow(templates.wordBank)
    const bankCells = getRowCells(bankRow)
    const wordBank = uniqueWordBank(section.items)
    const bankText = wordBank.length ? `(${wordBank.join('، ')})` : ''
    if (bankCells[1]) setCellText(bankCells[1], bankText)
    if (bankCells[2]) setCellText(bankCells[2], '')
    table.appendChild(bankRow)

    ;(section.items || []).forEach((item, idx) => {
      const questionRow = cloneRow(idx === 0 ? templates.firstQuestion : templates.question)
      const cells = getRowCells(questionRow)
      if (cells[1]) setCellText(cells[1], String(item?.no || ''))
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
    const spacerParagraph = bodyChildren.find(node => node?.namespaceURI === WORD_NS && node?.localName === 'p') || null
    if (tables.length < 4) {
      throw new Error('Template ExamAR tidak lengkap. Tabel soal Arab tidak ditemukan utuh.')
    }
    return {
      body,
      sectionPr,
      spacerParagraph,
      prototypes: {
        'pilihan-ganda': {
          table: tables[0].cloneNode(true),
          header: cloneRow(getTableRows(tables[0])[0]),
          question: cloneRow(getTableRows(tables[0])[1]),
          options: cloneRow(getTableRows(tables[0])[2])
        },
        esai: {
          table: tables[1].cloneNode(true),
          header: cloneRow(getTableRows(tables[1])[0]),
          firstQuestion: cloneRow(getTableRows(tables[1])[1]),
          question: cloneRow(getTableRows(tables[1])[2] || getTableRows(tables[1])[1])
        },
        'pasangkan-kata': {
          table: tables[2].cloneNode(true),
          header: cloneRow(getTableRows(tables[2])[0]),
          titles: cloneRow(getTableRows(tables[2])[1]),
          content: cloneRow(getTableRows(tables[2])[2])
        },
        'isi-titik': {
          table: tables[3].cloneNode(true),
          header: cloneRow(getTableRows(tables[3])[0]),
          wordBank: cloneRow(getTableRows(tables[3])[1]),
          firstQuestion: cloneRow(getTableRows(tables[3])[2]),
          question: cloneRow(getTableRows(tables[3])[3] || getTableRows(tables[3])[2])
        }
      }
    }
  }

  function rebuildBody(doc, sections) {
    const { body, sectionPr, spacerParagraph, prototypes } = extractTemplateTables(doc)
    Array.from(body.childNodes || []).forEach(node => {
      if (node !== sectionPr) body.removeChild(node)
    })

    const activeSections = (Array.isArray(sections) ? sections : []).filter(section => {
      const items = Array.isArray(section?.items) ? section.items : []
      return items.length > 0
    })
    activeSections.forEach((section, sectionIndex) => {
      const proto = prototypes[section.type]
      if (!proto) return
      const table = proto.table.cloneNode(true)
      if (section.type === 'pilihan-ganda') fillPgTable(table, section, sectionIndex, proto)
      else if (section.type === 'esai') fillEssayTable(table, section, sectionIndex, proto)
      else if (section.type === 'pasangkan-kata') fillMatchingTable(table, section, sectionIndex, proto)
      else if (section.type === 'isi-titik') fillFillBlankTable(table, section, sectionIndex, proto)
      body.insertBefore(table, sectionPr)
      if (spacerParagraph) body.insertBefore(spacerParagraph.cloneNode(true), sectionPr)
    })
  }

  function serializeXml(doc) {
    const xml = new XMLSerializer().serializeToString(doc)
    return xml.startsWith('<?xml')
      ? xml
      : `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>${xml}`
  }

  async function loadTemplateZip() {
    if (!window.JSZip || typeof window.JSZip.loadAsync !== 'function') {
      throw new Error('Library DOCX belum termuat. Refresh halaman lalu coba lagi.')
    }
    const templateUrl = new URL(TEMPLATE_PATH, window.location.href)
    const response = await fetch(templateUrl.toString(), { cache: 'no-cache' })
    if (!response.ok) {
      throw new Error('Template ExamAR.docx tidak ditemukan.')
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

  async function exportArabicExamDocx(jadwal, soal, fileName = 'Soal-Arab.docx') {
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

    const zip = await loadTemplateZip()
    const documentXmlFile = zip.file('word/document.xml')
    if (!documentXmlFile) throw new Error('Template DOCX tidak memiliki word/document.xml')
    const xmlString = await documentXmlFile.async('string')
    const parser = new DOMParser()
    const doc = parser.parseFromString(xmlString, 'application/xml')
    const parserError = doc.getElementsByTagName('parsererror')[0]
    if (parserError) {
      throw new Error('Template DOCX tidak dapat dibaca.')
    }

    rebuildBody(doc, sections)
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
    exportArabicExamDocx
  }
})()
