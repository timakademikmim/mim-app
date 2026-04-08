;(function initGuruLaporanUtsDocxUtils() {
  if (window.guruLaporanUtsDocxUtils) return

  const WORD_NS = 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'
  const DOCX_MIME = 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
  const TEMPLATE_PATH = 'Laporan UTS.docx'

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

  function getTableRows(table) {
    return qsa(table, 'tr', true)
  }

  function getRowCells(row) {
    return qsa(row, 'tc', true)
  }

  function createWordNode(doc, tagName) {
    return doc.createElementNS(WORD_NS, `w:${tagName}`)
  }

  function ensureParagraph(cell) {
    let paragraph = first(cell, 'p', true)
    if (paragraph) return paragraph
    paragraph = createWordNode(cell.ownerDocument, 'p')
    cell.appendChild(paragraph)
    return paragraph
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
        textNode = createWordNode(doc, 't')
        clone.appendChild(textNode)
      }
      return { run: clone, textNode }
    }
    const run = createWordNode(doc, 'r')
    const textNode = createWordNode(doc, 't')
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

  function setCellText(cell, text) {
    const paragraph = ensureParagraph(cell)
    setParagraphText(paragraph, text)
    qsa(cell, 'p', true).slice(1).forEach(node => cell.removeChild(node))
  }

  function getParagraphText(paragraph) {
    return qsa(paragraph, 't')
      .map(node => String(node?.textContent || ''))
      .join('')
  }

  function normalizeLooseText(value) {
    return String(value || '')
      .replace(/\s+/g, ' ')
      .trim()
      .toLowerCase()
  }

  function replaceWaliKelasSignature(doc, value) {
    const safeText = String(value || '').trim()
    if (!safeText) return
    const tables = qsa(doc, 'tbl')
    for (const table of tables) {
      const rows = getTableRows(table)
      let anchorRowIndex = -1
      for (let i = 0; i < rows.length; i += 1) {
        const rowText = normalizeLooseText(rows[i]?.textContent || '')
        if (rowText.includes('wali kelas')) {
          anchorRowIndex = i
          break
        }
      }
      if (anchorRowIndex < 0) continue
      for (let i = anchorRowIndex + 1; i < rows.length; i += 1) {
        const cells = getRowCells(rows[i])
        if (!cells.length) continue
        const targetCell = cells.find(cell => normalizeLooseText(cell.textContent || '')) || cells[0]
        setCellText(targetCell, safeText)
        return
      }
    }
    const paragraphs = qsa(doc, 'p')
    let anchorIndex = -1
    for (let i = paragraphs.length - 1; i >= 0; i -= 1) {
      const text = normalizeLooseText(getParagraphText(paragraphs[i]))
      if (text.includes('wali kelas')) {
        anchorIndex = i
        break
      }
    }
    if (anchorIndex < 0) return
    for (let i = anchorIndex + 1; i < paragraphs.length; i += 1) {
      const paragraph = paragraphs[i]
      const text = normalizeLooseText(getParagraphText(paragraph))
      if (!text) continue
      setParagraphText(paragraph, safeText)
      return
    }
  }

  function getFieldCharType(run) {
    const fld = first(run, 'fldChar', true)
    return String(fld?.getAttributeNS(WORD_NS, 'fldCharType') || fld?.getAttribute('w:fldCharType') || '').trim()
  }

  function replaceMergeFields(doc, replacements = {}) {
    const paragraphs = qsa(doc, 'p')
    paragraphs.forEach(paragraph => {
      let changed = true
      while (changed) {
        changed = false
        const runs = qsa(paragraph, 'r', true)
        for (let i = 0; i < runs.length; i += 1) {
          if (getFieldCharType(runs[i]) !== 'begin') continue
          let fieldName = ''
          let endIndex = -1
          let sawSeparate = false
          let styleRun = null
          for (let j = i + 1; j < runs.length; j += 1) {
            const instr = first(runs[j], 'instrText', true)
            if (instr) {
              const match = String(instr.textContent || '').match(/MERGEFIELD\s+([A-Za-z0-9_]+)/i)
              if (match?.[1]) fieldName = String(match[1]).trim()
            }
            const fldType = getFieldCharType(runs[j])
            if (fldType === 'separate') {
              sawSeparate = true
            } else if (fldType === 'end') {
              endIndex = j
              break
            } else if (sawSeparate && !styleRun && (first(runs[j], 't', true) || first(runs[j], 'rPr', true))) {
              styleRun = runs[j]
            }
          }
          if (endIndex < 0 || !fieldName || !Object.prototype.hasOwnProperty.call(replacements, fieldName)) continue
          if (!styleRun) {
            styleRun = runs.slice(i, endIndex + 1).find(run => first(run, 'rPr', true) || first(run, 't', true)) || runs[i]
          }
          const safeText = String(replacements[fieldName] ?? '')
          const replacementRun = styleRun.cloneNode(true)
          Array.from(replacementRun.childNodes || []).forEach(child => {
            if (child?.namespaceURI === WORD_NS && child?.localName !== 'rPr') {
              replacementRun.removeChild(child)
            }
          })
          const textNode = createWordNode(doc, 't')
          if (/^\s|\s$/.test(safeText)) {
            textNode.setAttributeNS('http://www.w3.org/XML/1998/namespace', 'xml:space', 'preserve')
          }
          textNode.textContent = safeText
          replacementRun.appendChild(textNode)
          paragraph.insertBefore(replacementRun, runs[i])
          for (let j = endIndex; j >= i; j -= 1) {
            paragraph.removeChild(runs[j])
          }
          changed = true
          break
        }
      }
    })
  }

  function normalizeSubjectName(value) {
    return String(value || '')
      .toLowerCase()
      .replace(/\s+/g, ' ')
      .trim()
  }

  function extractTemplateSubjectOrder(table) {
    const rows = getTableRows(table)
    if (rows.length < 4) return []
    return rows
      .slice(1, Math.max(1, rows.length - 2))
      .map(row => {
        const cells = getRowCells(row)
        return normalizeSubjectName(cells[1]?.textContent || '')
      })
      .filter(Boolean)
  }

  function sortSubjectRows(subjects = [], templateOrder = []) {
    const orderMap = new Map()
    templateOrder.forEach((name, index) => {
      if (!orderMap.has(name)) orderMap.set(name, index)
    })
    return [...subjects].sort((a, b) => {
      const nameA = normalizeSubjectName(a?.name || '')
      const nameB = normalizeSubjectName(b?.name || '')
      const idxA = orderMap.has(nameA) ? orderMap.get(nameA) : Number.MAX_SAFE_INTEGER
      const idxB = orderMap.has(nameB) ? orderMap.get(nameB) : Number.MAX_SAFE_INTEGER
      if (idxA !== idxB) return idxA - idxB
      return String(a?.name || '').localeCompare(String(b?.name || ''), 'id')
    })
  }

  function rebuildSubjectTable(table, subjects = []) {
    const rows = getTableRows(table)
    if (rows.length < 4) {
      throw new Error('Template Laporan UTS tidak sesuai. Tabel mata pelajaran belum lengkap.')
    }
    const templateOrder = extractTemplateSubjectOrder(table)
    const sampleRow = rows[1]
    const jumlahRow = rows[rows.length - 2]
    const rataRow = rows[rows.length - 1]
    rows.slice(1, rows.length - 2).forEach(row => table.removeChild(row))
    const sortedSubjects = sortSubjectRows(subjects, templateOrder)
    const safeSubjects = sortedSubjects.length
      ? sortedSubjects
      : [{ name: 'Belum ada data mapel', kkmText: '17', scoreText: '-' }]
    safeSubjects.forEach((subject, index) => {
      const row = sampleRow.cloneNode(true)
      const cells = getRowCells(row)
      setCellText(cells[0], String(index + 1))
      setCellText(cells[1], String(subject?.name || '-'))
      setCellText(cells[2], String(subject?.kkmText || '17'))
      setCellText(cells[3], String(subject?.scoreText || '-'))
      table.insertBefore(row, jumlahRow)
    })
    if (!table.contains(jumlahRow)) table.appendChild(jumlahRow)
    if (!table.contains(rataRow)) table.appendChild(rataRow)
  }

  function serializeXml(doc) {
    const xml = new XMLSerializer().serializeToString(doc)
    return xml.startsWith('<?xml')
      ? xml
      : `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>${xml}`
  }

  async function loadTemplateZip(templatePath = TEMPLATE_PATH) {
    if (!window.JSZip || typeof window.JSZip.loadAsync !== 'function') {
      throw new Error('Library DOCX belum termuat. Refresh halaman lalu coba lagi.')
    }
    const templateUrl = new URL(String(templatePath || TEMPLATE_PATH), window.location.href)
    const response = await fetch(templateUrl.toString(), { cache: 'no-cache' })
    if (!response.ok) {
      throw new Error(`Template ${String(templatePath || TEMPLATE_PATH)} tidak ditemukan.`)
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

  async function buildLaporanUtsDocxBlob(payload = {}) {
    const zip = await loadTemplateZip(TEMPLATE_PATH)
    const documentXmlFile = zip.file('word/document.xml')
    if (!documentXmlFile) throw new Error('Template Laporan UTS tidak memiliki word/document.xml')
    const xmlString = await documentXmlFile.async('string')
    const parser = new DOMParser()
    const doc = parser.parseFromString(xmlString, 'application/xml')
    const parserError = doc.getElementsByTagName('parsererror')[0]
    if (parserError) throw new Error('Template Laporan UTS tidak dapat dibaca.')

    const tables = qsa(doc, 'tbl')
    if (tables.length < 3) {
      throw new Error('Template Laporan UTS belum sesuai. Tabel laporan belum lengkap.')
    }

    rebuildSubjectTable(tables[0], Array.isArray(payload?.subjects) ? payload.subjects : [])

    replaceMergeFields(doc, {
      F1: String(payload?.waliKelasName || ''),
      F2: String(payload?.studentName || ''),
      F3: String(payload?.studentNisn || ''),
      F4: String(payload?.className || ''),
      F5: String(payload?.midTahfizCapaian || ''),
      F6: String(payload?.midTahfizScore || ''),
      F18: String(payload?.totalScoreText || ''),
      F19: String(payload?.averageScoreText || ''),
      F20: String(payload?.halaqahSakitText || ''),
      F21: String(payload?.halaqahIzinText || ''),
      F22: String(payload?.kelasSakitText || ''),
      F23: String(payload?.kelasIzinText || ''),
      WALI_KELAS: String(payload?.waliKelasName || ''),
      WALIKELAS: String(payload?.waliKelasName || '')
    })
    replaceWaliKelasSignature(doc, payload?.waliKelasName || '')

    zip.file('word/document.xml', serializeXml(doc))
    return zip.generateAsync({
      type: 'blob',
      mimeType: DOCX_MIME,
      compression: 'DEFLATE',
      compressionOptions: { level: 6 }
    })
  }

  async function exportLaporanUtsDocx(payload = {}, fileName = 'Laporan UTS.docx') {
    const blob = await buildLaporanUtsDocxBlob(payload)
    triggerBlobDownload(blob, fileName)
    return { ok: true, fileName }
  }

  window.guruLaporanUtsDocxUtils = {
    buildLaporanUtsDocxBlob,
    exportLaporanUtsDocx
  }
})()
