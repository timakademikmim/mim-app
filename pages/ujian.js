(function () {
  var EXAM_SCHEDULE_TABLE = 'jadwal_ujian'
  var EXAM_QUESTION_TABLE = 'soal_ujian'
  var EXAM_ARABIC_FONT_FILE = 'Traditional Arabic Regular.ttf'
  var EXAM_ARABIC_FONT_NAME = 'TraditionalArabic'
  var EXAM_ARABIC_FONT_BOLD_FILE = 'Traditional Arabic Bold.ttf'
  var EXAM_ARABIC_FONT_BOLD_NAME = 'TraditionalArabicBold'
  var EXAM_ARABIC_FONT_VFS_KEY = 'traditional-arabic-regular.ttf'
  var EXAM_ARABIC_FONT_BOLD_VFS_KEY = 'traditional-arabic-bold.ttf'
  var EXAM_PRINT_BACKGROUND_URL = 'Bg Ujian.png'
  var examArabicFontBase64 = ''
  var examArabicFontBoldBase64 = ''
  var examArabicFontLoadPromise = null
  var examPrintBackgroundDataUrl = ''
  var examPrintBackgroundLoadPromise = null

  function escapeHtml(value) {
    return String(value == null ? '' : value)
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#39;')
  }

  function parseQuestions(value) {
    if (Array.isArray(value)) return value
    var raw = String(value || '').trim()
    if (!raw) return []
    try {
      var parsed = JSON.parse(raw)
      return Array.isArray(parsed) ? parsed : []
    } catch (_err) {
      return []
    }
  }

  function parseInstruksiMeta(value) {
    var raw = String(value || '')
    var marker = raw.match(/^\[\[LANG:(AR|ID)\]\]\s*\n?/i)
    var lang = marker ? String(marker[1] || 'ID').toUpperCase() : 'ID'
    var text = marker ? raw.slice(marker[0].length) : raw
    return {
      lang: lang === 'AR' ? 'AR' : 'ID',
      text: String(text || '').trim()
    }
  }

  function blobToDataUrl(blob) {
    return new Promise(function (resolve, reject) {
      var reader = new FileReader()
      reader.onload = function () { resolve(String(reader.result || '')) }
      reader.onerror = function () { reject(new Error('Gagal membaca file background ujian.')) }
      reader.readAsDataURL(blob)
    })
  }

  async function loadBackgroundDataUrl(url) {
    var res = await fetch(encodeURI(String(url || '')), { cache: 'no-cache' })
    if (!res.ok) throw new Error('Background ujian tidak ditemukan (' + String(res.status) + ').')
    var blob = await res.blob()
    var dataUrl = await blobToDataUrl(blob)
    if (!String(dataUrl || '').startsWith('data:image/')) throw new Error('Format background ujian harus gambar.')
    return dataUrl
  }

  async function ensureExamPrintBackgroundLoaded() {
    if (examPrintBackgroundDataUrl) return examPrintBackgroundDataUrl
    if (examPrintBackgroundLoadPromise) return examPrintBackgroundLoadPromise
    examPrintBackgroundLoadPromise = (async function () {
      try {
        var candidates = [EXAM_PRINT_BACKGROUND_URL, 'Ujian.png', 'bg ujian.png']
        var dataUrl = ''
        for (var i = 0; i < candidates.length; i += 1) {
          try {
            dataUrl = await loadBackgroundDataUrl(candidates[i])
            if (dataUrl) break
          } catch (_err) {}
        }
        examPrintBackgroundDataUrl = dataUrl
        return examPrintBackgroundDataUrl
      } catch (err) {
        console.warn('Gagal memuat background ujian admin.', err)
        examPrintBackgroundDataUrl = ''
        return ''
      } finally {
        examPrintBackgroundLoadPromise = null
      }
    })()
    return examPrintBackgroundLoadPromise
  }

  async function ensureArabicFontLoaded() {
    if (examArabicFontBase64) return true
    if (examArabicFontLoadPromise) return examArabicFontLoadPromise
    examArabicFontLoadPromise = (async function () {
      try {
        var loadFile = async function (filename, required) {
          var candidates = [
            String(filename || '').trim(),
            './' + String(filename || '').trim(),
            '../' + String(filename || '').trim(),
            '/' + String(filename || '').trim()
          ].filter(Boolean)
          var lastErr = null
          for (var ci = 0; ci < candidates.length; ci += 1) {
            try {
              var res = await fetch(encodeURI(candidates[ci]), { cache: 'no-cache' })
              if (!res.ok) throw new Error('HTTP ' + String(res.status))
              var buf = await res.arrayBuffer()
              var bytes = new Uint8Array(buf)
              var binary = ''
              var chunkSize = 0x8000
              for (var i = 0; i < bytes.length; i += chunkSize) {
                binary += String.fromCharCode.apply(null, bytes.subarray(i, i + chunkSize))
              }
              return btoa(binary)
            } catch (err) {
              lastErr = err
            }
          }
          if (!required) return ''
          throw (lastErr || new Error('Gagal memuat font: ' + String(filename || '-')))
        }
        examArabicFontBase64 = await loadFile(EXAM_ARABIC_FONT_FILE, true)
        examArabicFontBoldBase64 = await loadFile(EXAM_ARABIC_FONT_BOLD_FILE, false)
        return Boolean(examArabicFontBase64)
      } catch (err) {
        console.warn('Gagal memuat font Arab untuk PDF admin.', err)
        examArabicFontBase64 = ''
        examArabicFontBoldBase64 = ''
        return false
      } finally {
        examArabicFontLoadPromise = null
      }
    })()
    return examArabicFontLoadPromise
  }

  function getPdfStaticText(langCode) {
    var lang = String(langCode || 'ID').toUpperCase()
    if (lang === 'AR') {
      return {
        title: 'أسئلة الامتحان',
        jenis: 'النوع',
        namaUjian: 'اسم الاختبار',
        kelas: 'الصف',
        mapel: 'المادة',
        tanggal: 'التاريخ',
        waktu: 'الوقت',
        guru: 'المعلم',
        instruksiUmum: 'تعليمات عامة'
      }
    }
    return {
      title: 'SOAL UJIAN',
      jenis: 'Jenis',
      namaUjian: 'Nama Ujian',
      kelas: 'Kelas',
      mapel: 'Mapel',
      tanggal: 'Tanggal',
      waktu: 'Waktu',
      guru: 'Guru',
      instruksiUmum: 'Instruksi Umum'
    }
  }

  function toArabicIndicDigits(value) {
    var map = ['٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩']
    return String(value == null ? '' : value).replace(/\d/g, function (d) { return map[Number(d)] || d })
  }

  function formatExamNumber(value, langCode) {
    var lang = String(langCode || 'ID').toUpperCase()
    return lang === 'AR' ? toArabicIndicDigits(value) : String(value)
  }

  function getExamMarkerSeparator(langCode) {
    return '.'
  }

  function formatExamMarker(token, langCode) {
    var lang = String(langCode || 'ID').toUpperCase()
    var body = String(token || '')
    return lang === 'AR' ? ('.' + body) : (body + '.')
  }

  function getArabicLetterByIndex(index) {
    var letters = ['أ', 'ب', 'ج', 'د', 'هـ', 'و', 'ز', 'ح', 'ط', 'ي', 'ك', 'ل', 'م', 'ن', 'س', 'ع', 'ف', 'ص', 'ق', 'ر', 'ش', 'ت', 'ث', 'خ', 'ذ', 'ض']
    return letters[Number(index || 0) % letters.length]
  }

  function buildPrintSections(questions, fallbackType) {
    var rows = Array.isArray(questions) ? questions : []
    var sections = []
    var currentType = ''
    var currentItems = []
    rows.forEach(function (item, idx) {
      var qType = normalizeQuestionType(item && item.type, fallbackType)
      var numbered = Object.assign({}, item, { no: Number(item && item.no || (idx + 1)) })
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

  function getPrintTypeTitle(type, index, langCode) {
    var parts = getPrintTypeParts(type, index, langCode)
    return parts.marker + ' ' + parts.label
  }

  function getPrintTypeParts(type, index, langCode) {
    var lang = String(langCode || 'ID').toUpperCase()
    var label = 'Pilihan Ganda'
    if (lang === 'AR') {
      if (type === 'esai') label = 'مقال'
      else if (type === 'pasangkan-kata') label = 'وصل الكلمات'
      else if (type === 'isi-titik') label = 'املأ الفراغ'
      else label = 'اختيار من متعدد'
    } else {
      if (type === 'esai') label = 'Esai'
      else if (type === 'pasangkan-kata') label = 'Pasangkan Kata'
      else if (type === 'isi-titik') label = 'Isi Titik Kosong'
    }
    var code = lang === 'AR' ? getArabicLetterByIndex(index) : String.fromCharCode(65 + (Number(index || 0) % 26))
    return {
      marker: formatExamMarker(code, lang),
      label: label
    }
  }

  function getPrintTypeInstruction(type, langCode) {
    var lang = String(langCode || 'ID').toUpperCase()
    if (lang === 'AR') {
      if (type === 'esai') return 'أجب عن الأسئلة التالية بوضوح وصحة.'
      if (type === 'pasangkan-kata') return 'صِل كلمات العمود (أ) بما يناسبها في العمود (ب).'
      if (type === 'isi-titik') return 'أكمل الفراغ بالكلمة المناسبة من الكلمات المعطاة.'
      return 'اختر إجابة واحدة صحيحة.'
    }
    if (type === 'esai') return 'Jawablah soal berikut dengan jelas dan benar.'
    if (type === 'pasangkan-kata') return 'Pasangkan kata pada baris A dengan pasangan yang tepat pada baris B.'
    if (type === 'isi-titik') return 'Lengkapi bagian yang kosong dengan penggalan kata yang disediakan.'
    return 'Pilihlah satu jawaban yang paling tepat.'
  }

  function normalizeQuestionType(value, fallbackType) {
    var raw = String(value || '').trim().toLowerCase()
    if (raw === 'esai' || raw === 'essay') return 'esai'
    if (raw === 'pilihan-ganda' || raw === 'pilihan ganda' || raw === 'pg') return 'pilihan-ganda'
    if (raw === 'pasangkan-kata' || raw === 'pasangkan kata' || raw === 'matching') return 'pasangkan-kata'
    if (raw === 'isi-titik' || raw === 'isi titik' || raw === 'fill-blank' || raw === 'fill blank') return 'isi-titik'
    var fallback = String(fallbackType || '').trim().toLowerCase()
    if (fallback === 'esai' || fallback === 'essay') return 'esai'
    if (fallback === 'pasangkan-kata' || fallback === 'pasangkan kata' || fallback === 'matching') return 'pasangkan-kata'
    if (fallback === 'isi-titik' || fallback === 'isi titik' || fallback === 'fill-blank' || fallback === 'fill blank') return 'isi-titik'
    return 'pilihan-ganda'
  }

  function toTimeLabel(value) {
    var text = String(value || '').trim()
    if (!text) return '-'
    return text.length >= 5 ? text.slice(0, 5) : text
  }

  function statusLabel(status) {
    var s = String(status || '').toLowerCase()
    if (s === 'submitted') return 'Terkirim'
    if (s === 'draft') return 'Draft'
    return '-'
  }

  function isMissingTableError(error, tableName) {
    var msg = String(error && error.message || '').toLowerCase()
    var code = String(error && error.code || '').toUpperCase()
    return code === '42P01' ||
      msg.includes('does not exist') ||
      msg.includes('relation') ||
      msg.includes('could not find the table') ||
      msg.includes('public.' + String(tableName || ''))
  }

  async function createExamPdfDoc(jadwal, soal) {
    var jsPdfApi = window.jspdf
    if (!jsPdfApi || typeof jsPdfApi.jsPDF !== 'function') {
      alert('Library PDF belum termuat. Refresh halaman lalu coba lagi.')
      return null
    }

    var jsPDF = jsPdfApi.jsPDF
    var doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' })
    var margin = 15
    var pageWidth = doc.internal.pageSize.getWidth()
    var pageHeight = doc.internal.pageSize.getHeight()
    var usableWidth = pageWidth - margin * 2
    var y = margin

    await ensureExamPrintBackgroundLoaded()
    var drawPageBackground = function () {
      if (!examPrintBackgroundDataUrl) return
      try {
        doc.addImage(examPrintBackgroundDataUrl, 'PNG', 0, 0, pageWidth, pageHeight, undefined, 'FAST')
      } catch (bgErr) {
        console.warn('Gagal render background ujian admin.', bgErr)
      }
    }
    var originalAddPage = doc.addPage.bind(doc)
    doc.addPage = function () {
      var out = originalAddPage.apply(doc, arguments)
      drawPageBackground()
      return out
    }
    drawPageBackground()

    var instruksiMeta = parseInstruksiMeta(soal && soal.instruksi)
    var lang = instruksiMeta.lang
    var isAr = lang === 'AR'
    var arabicRegularReady = false
    if (isAr) {
      await ensureArabicFontLoaded()
      if (!examArabicFontBase64) return null
      var tryRegister = function (vfsKey, familyName, base64) {
        try {
          doc.addFileToVFS(vfsKey, base64)
          doc.addFont(vfsKey, familyName, 'normal')
          doc.setFont(familyName, 'normal')
          return true
        } catch (_err) {
          return false
        }
      }
      arabicRegularReady = tryRegister(EXAM_ARABIC_FONT_VFS_KEY, EXAM_ARABIC_FONT_NAME, examArabicFontBase64) ||
        tryRegister(EXAM_ARABIC_FONT_FILE, EXAM_ARABIC_FONT_NAME, examArabicFontBase64)
      if (!arabicRegularReady) console.warn('Registrasi font Arab regular gagal (admin).')
      if (!arabicRegularReady) return null
      try {
        doc.setFont(EXAM_ARABIC_FONT_NAME, 'normal')
        doc.splitTextToSize('اختبار', 20)
      } catch (fontErr) {
        console.warn('Validasi font Arab regular gagal (admin).', fontErr)
        return null
      }
    }
    var setBold = function () { doc.setFont(isAr ? EXAM_ARABIC_FONT_NAME : 'times', isAr ? 'normal' : 'bold') }
    var setNormal = function () { doc.setFont(isAr ? EXAM_ARABIC_FONT_NAME : 'times', 'normal') }
    var safeSplit = function (text, width) {
      try {
        return doc.splitTextToSize(String(text || ''), width)
      } catch (err) {
        console.warn('splitTextToSize gagal (admin), fallback single-line.', err)
        return [String(text || '')]
      }
    }
    if (typeof doc.setR2L === 'function') doc.setR2L(false)
    var lineX = function (indent) { return (isAr ? pageWidth - margin - Number(indent || 0) : margin + Number(indent || 0)) }
    var toRtl = function (text) { return (isAr ? '\u202B' + String(text || '') + '\u202C' : String(text || '')) }
    var drawLine = function (text, indent) {
      var str = toRtl(text)
      if (isAr) doc.text(str, lineX(indent), y, { align: 'right' })
      else doc.text(str, lineX(indent), y)
    }
    var drawWrapped = function (text, wrapWidth, indent) {
      var linesArr = safeSplit(text, wrapWidth)
      linesArr.forEach(function (line) {
        if (y > 285) {
          doc.addPage()
          y = margin
        }
        var wrappedLine = toRtl(line)
        if (isAr) doc.text(wrappedLine, lineX(indent), y, { align: 'right' })
        else doc.text(wrappedLine, lineX(indent), y)
        y += 5
      })
    }
    var t = getPdfStaticText(lang)

    setBold()
    doc.setFontSize(14)
    doc.text(t.title, pageWidth / 2, y, { align: 'center' })
    y += 8
    setNormal()
    doc.setFontSize(11)
    drawLine(t.jenis + ': ' + String(jadwal && jadwal.jenis || '-'), 0)
    y += 6
    drawLine(t.namaUjian + ': ' + String(jadwal && jadwal.nama || '-'), 0)
    y += 6
    drawLine(t.kelas + ': ' + String(jadwal && jadwal.kelas || '-') + ' | ' + t.mapel + ': ' + String(jadwal && jadwal.mapel || '-'), 0)
    y += 6
    drawLine(t.tanggal + ': ' + String(jadwal && jadwal.tanggal || '-') + ' | ' + t.waktu + ': ' + toTimeLabel(jadwal && jadwal.jam_mulai) + ' - ' + toTimeLabel(jadwal && jadwal.jam_selesai), 0)
    y += 6
    drawLine(t.guru + ': ' + String(soal && soal.guru_nama || '-'), 0)
    y += 8

    var instruksi = instruksiMeta.text
    if (instruksi) {
      setBold()
      drawLine(t.instruksiUmum + ':', 0)
      y += 5
      setNormal()
      drawWrapped(instruksi, usableWidth, 0)
      y += 2
    }

    var questions = parseQuestions(soal && soal.questions_json)
    var sections = buildPrintSections(questions, soal && soal.bentuk_soal)
    var questionIndent = 4
    var optionIndent = 10
    sections.forEach(function (section, sectionIndex) {
      var headingParts = getPrintTypeParts(section.type, sectionIndex, lang)
      var heading = headingParts.marker + ' ' + headingParts.label
      var modelInstruksi = getPrintTypeInstruction(section.type, lang)
      if (y + 12 > 285) {
        doc.addPage()
        y = margin
      }
      if (isAr) {
        setBold()
        drawLine(headingParts.marker, 0)
        setNormal()
        drawLine(headingParts.label, 16)
      } else {
        setBold()
        drawLine(heading, 0)
      }
      y += 5
      setNormal()
      drawWrapped(modelInstruksi, usableWidth - 4, 4)
      y += 2

      var sectionItems = section.items || []
      if (section.type === 'isi-titik') {
        var fragSet = new Set()
        sectionItems.forEach(function (item) {
          var frags = Array.isArray(item && item.fragments) ? item.fragments : []
          frags.forEach(function (f) {
            var txt = String(f || '').trim()
            if (txt) fragSet.add(txt)
          })
        })
        var fragList = Array.from(fragSet)
        if (fragList.length) {
          var fragLine = 'Pilihan kata: (' + fragList.join(', ') + ')'
          drawWrapped(fragLine, usableWidth - optionIndent, optionIndent)
          y += 2
        }
      }

      sectionItems.forEach(function (q, idx) {
        var no = idx + 1
        var qTextRaw = String(q && q.text || '-')
        if (isAr) {
          var noPrefix = formatExamMarker(formatExamNumber(no, lang), lang)
          var qLines = safeSplit(qTextRaw, usableWidth - questionIndent - 14)
          if (y > 285) {
            doc.addPage()
            y = margin
          }
          doc.text(toRtl(noPrefix), lineX(questionIndent), y, { align: 'right' })
          doc.text(toRtl(String(qLines[0] || '-')), lineX(questionIndent + 14), y, { align: 'right' })
          y += 5
          for (var qli = 1; qli < qLines.length; qli += 1) {
            if (y > 285) {
              doc.addPage()
              y = margin
            }
            doc.text(toRtl(String(qLines[qli] || '')), lineX(questionIndent + 14), y, { align: 'right' })
            y += 5
          }
        } else {
          var title = formatExamMarker(formatExamNumber(no, lang), lang) + ' ' + qTextRaw
          drawWrapped(title, usableWidth - questionIndent, questionIndent)
        }

        if (section.type === 'pilihan-ganda') {
          var opts = q && q.options || {}
          var aTxt = 'a. ' + String(opts.a || '-')
          var bTxt = 'b. ' + String(opts.b || '-')
          var cTxt = 'c. ' + String(opts.c || '-')
          var dTxt = 'd. ' + String(opts.d || '-')
          var maxLen = Math.max(aTxt.length, bTxt.length, cTxt.length, dTxt.length)
          var useTwoCols = maxLen <= 36 && !isAr
          if (useTwoCols) {
            var leftX = margin + optionIndent
            var rightX = margin + optionIndent + 70
            ;[[aTxt, cTxt], [bTxt, dTxt]].forEach(function (pair) {
              if (y > 285) {
                doc.addPage()
                y = margin
              }
              doc.text(pair[0], leftX, y)
              doc.text(pair[1], rightX, y)
              y += 5
            })
          } else {
            var pgLines = isAr
              ? [
                { marker: 'أ', text: String(opts.a || '-') },
                { marker: 'ب', text: String(opts.b || '-') },
                { marker: 'ج', text: String(opts.c || '-') },
                { marker: 'د', text: String(opts.d || '-') }
              ]
              : [aTxt, bTxt, cTxt, dTxt]
            pgLines.forEach(function (line) {
              if (y > 285) {
                doc.addPage()
                y = margin
              }
              if (isAr) {
                var marker = formatExamMarker(String(line.marker || ''), lang)
                var val = String(line.text || '-')
                var vLines = safeSplit(val, usableWidth - optionIndent - 14)
                doc.text(toRtl(marker), lineX(optionIndent), y, { align: 'right' })
                doc.text(toRtl(String(vLines[0] || '-')), lineX(optionIndent + 14), y, { align: 'right' })
                y += 5
                for (var vi = 1; vi < vLines.length; vi += 1) {
                  if (y > 285) {
                    doc.addPage()
                    y = margin
                  }
                  doc.text(toRtl(String(vLines[vi] || '')), lineX(optionIndent + 14), y, { align: 'right' })
                  y += 5
                }
              } else {
                doc.text(line, margin + optionIndent, y)
                y += 5
              }
            })
          }
        } else if (section.type === 'pasangkan-kata') {
          var pairs = Array.isArray(q && q.pairs) ? q.pairs : []
          if (pairs.length) {
            if (y > 285) {
              doc.addPage()
              y = margin
            }
            setBold()
            var colA = isAr ? 'العمود أ' : 'Baris A'
            var colB = isAr ? 'العمود ب' : 'Baris B'
            if (isAr) {
              doc.text(colA, lineX(optionIndent), y, { align: 'right' })
              doc.text(colB, lineX(optionIndent + 60), y, { align: 'right' })
            } else {
              doc.text(colA, margin + optionIndent, y)
              doc.text(colB, margin + optionIndent + 60, y)
            }
            y += 5
            setNormal()
            pairs.forEach(function (pair, idxPair) {
              if (y > 285) {
                doc.addPage()
                y = margin
              }
              var left = formatExamMarker(formatExamNumber(idxPair + 1, lang), lang) + ' ' + String(pair && pair.a || '-')
              var right = formatExamMarker((isAr ? getArabicLetterByIndex(idxPair) : String.fromCharCode(65 + idxPair)), lang) + ' ' + String(pair && pair.b || '-')
              if (isAr) {
                doc.text(left, lineX(optionIndent), y, { align: 'right' })
                doc.text(right, lineX(optionIndent + 60), y, { align: 'right' })
              } else {
                doc.text(left, margin + optionIndent, y)
                doc.text(right, margin + optionIndent + 60, y)
              }
              y += 5
            })
          }
        }
        y += 2
      })
    })

    return doc
  }

  function openExamBrowserPrint(jadwal, soal) {
    var instruksiMeta = parseInstruksiMeta(soal && soal.instruksi)
    var lang = instruksiMeta.lang
    var isAr = lang === 'AR'
    var t = getPdfStaticText(lang)
    var questions = parseQuestions(soal && soal.questions_json)
    var sections = buildPrintSections(questions, soal && soal.bentuk_soal)

    var sectionHtml = sections.map(function (section, sectionIndex) {
      var headingParts = getPrintTypeParts(section.type, sectionIndex, lang)
      var modelInstruksi = getPrintTypeInstruction(section.type, lang)
      var items = section.items || []
      var extraLine = ''
      if (section.type === 'isi-titik') {
        var fragSet = new Set()
        items.forEach(function (item) {
          var frags = Array.isArray(item && item.fragments) ? item.fragments : []
          frags.forEach(function (f) {
            var txt = String(f || '').trim()
            if (txt) fragSet.add(txt)
          })
        })
        var fragList = Array.from(fragSet)
        if (fragList.length) {
          extraLine = '<p><strong>' + escapeHtml(isAr ? 'اختيارات الكلمات' : 'Pilihan kata') + ':</strong> (' + escapeHtml(fragList.join(', ')) + ')</p>'
        }
      }

      var questionsHtml = items.map(function (q, idx) {
        var no = idx + 1
        var qText = '<div class="q-title">' + escapeHtml(formatExamMarker(formatExamNumber(no, lang), lang)) + ' ' + escapeHtml(String(q && q.text || '-')) + '</div>'
        if (section.type === 'pilihan-ganda') {
          var opts = q && q.options || {}
          var mA = isAr ? 'أ' : 'a'
          var mB = isAr ? 'ب' : 'b'
          var mC = isAr ? 'ج' : 'c'
          var mD = isAr ? 'د' : 'd'
          return '<li>' + qText + '<div class="pg-grid">' +
            '<div>' + escapeHtml(formatExamMarker(mA, lang)) + ' ' + escapeHtml(String(opts.a || '-')) + '</div>' +
            '<div>' + escapeHtml(formatExamMarker(mC, lang)) + ' ' + escapeHtml(String(opts.c || '-')) + '</div>' +
            '<div>' + escapeHtml(formatExamMarker(mB, lang)) + ' ' + escapeHtml(String(opts.b || '-')) + '</div>' +
            '<div>' + escapeHtml(formatExamMarker(mD, lang)) + ' ' + escapeHtml(String(opts.d || '-')) + '</div>' +
            '</div></li>'
        }
        if (section.type === 'pasangkan-kata') {
          var pairs = Array.isArray(q && q.pairs) ? q.pairs : []
          var rows = pairs.map(function (pair, i) {
            var m = isAr ? getArabicLetterByIndex(i) : String.fromCharCode(65 + i)
            return '<tr><td>' + escapeHtml(formatExamMarker(formatExamNumber(i + 1, lang), lang)) + ' ' + escapeHtml(String(pair && pair.a || '-')) + '</td><td>' + escapeHtml(formatExamMarker(m, lang)) + ' ' + escapeHtml(String(pair && pair.b || '-')) + '</td></tr>'
          }).join('')
          return '<li>' + qText + '<table class="pair-table"><thead><tr><th>' + escapeHtml(isAr ? 'العمود أ' : 'Baris A') + '</th><th>' + escapeHtml(isAr ? 'العمود ب' : 'Baris B') + '</th></tr></thead><tbody>' + rows + '</tbody></table></li>'
        }
        return '<li>' + qText + '</li>'
      }).join('')

      return '<section class="sec"><h3><strong>' + escapeHtml(headingParts.marker) + '</strong> ' + escapeHtml(headingParts.label) + '</h3><p>' + escapeHtml(modelInstruksi) + '</p>' + extraLine + '<ol>' + questionsHtml + '</ol></section>'
    }).join('')

    var instruksiUmum = instruksiMeta.text
      ? '<p><strong>' + escapeHtml(t.instruksiUmum) + ':</strong> ' + escapeHtml(instruksiMeta.text) + '</p>'
      : ''

    var html = '<!doctype html><html lang="' + (isAr ? 'ar' : 'id') + '" dir="' + (isAr ? 'rtl' : 'ltr') + '">' +
      '<head><meta charset="utf-8"><title>' + escapeHtml(t.title) + '</title>' +
      '<style>body{font-family:"Times New Roman",serif;margin:20px;color:#111}h1{text-align:center;font-size:22px;margin:0 0 12px 0}.meta p{margin:4px 0;font-size:14px}.sec{margin-top:14px}.sec h3{margin:0 0 6px 0;font-size:16px}.sec p{margin:4px 0;font-size:14px}ol{margin:6px 0 0 0;padding-' + (isAr ? 'right' : 'left') + ':20px}li{margin:8px 0}.q-title{margin-bottom:4px}.pg-grid{display:grid;grid-template-columns:1fr 1fr;gap:2px 18px;margin-' + (isAr ? 'right' : 'left') + ':20px}.pair-table{border-collapse:collapse;width:100%;margin-top:4px}.pair-table th,.pair-table td{border:1px solid #999;padding:4px 6px;font-size:13px;text-align:' + (isAr ? 'right' : 'left') + '}@media print{body{margin:10mm}}</style>' +
      '</head><body>' +
      '<h1>' + escapeHtml(t.title) + '</h1>' +
      '<div class="meta">' +
      '<p><strong>' + escapeHtml(t.jenis) + ':</strong> ' + escapeHtml(String(jadwal && jadwal.jenis || '-')) + '</p>' +
      '<p><strong>' + escapeHtml(t.namaUjian) + ':</strong> ' + escapeHtml(String(jadwal && jadwal.nama || '-')) + '</p>' +
      '<p><strong>' + escapeHtml(t.kelas) + ':</strong> ' + escapeHtml(String(jadwal && jadwal.kelas || '-')) + ' | <strong>' + escapeHtml(t.mapel) + ':</strong> ' + escapeHtml(String(jadwal && jadwal.mapel || '-')) + '</p>' +
      '<p><strong>' + escapeHtml(t.tanggal) + ':</strong> ' + escapeHtml(String(jadwal && jadwal.tanggal || '-')) + ' | <strong>' + escapeHtml(t.waktu) + ':</strong> ' + escapeHtml(toTimeLabel(jadwal && jadwal.jam_mulai) + ' - ' + toTimeLabel(jadwal && jadwal.jam_selesai)) + '</p>' +
      '<p><strong>' + escapeHtml(t.guru) + ':</strong> ' + escapeHtml(String(soal && soal.guru_nama || '-')) + '</p>' +
      instruksiUmum +
      '</div>' + sectionHtml + '</body></html>'

    var win = window.open('', '_blank')
    if (!win) {
      alert('Popup diblokir browser. Izinkan popup untuk mencetak soal Arab.')
      return
    }
    win.document.open()
    win.document.write(html)
    win.document.close()
    win.focus()
    setTimeout(function () { win.print() }, 250)
  }

  async function buildExamWordHtml(jadwal, soal) {
    var instruksiMeta = parseInstruksiMeta(soal && soal.instruksi)
    var lang = instruksiMeta.lang
    var isAr = lang === 'AR'
    var t = getPdfStaticText(lang)
    var bgDataUrl = await ensureExamPrintBackgroundLoaded()
    var questions = parseQuestions(soal && soal.questions_json)
    var sections = buildPrintSections(questions, soal && soal.bentuk_soal)
    var wordFontFamily = isAr ? '"Traditional Arabic","Times New Roman",serif' : '"Times New Roman",serif'
    var wordDirectionCss = isAr ? 'direction:rtl; unicode-bidi:embed; text-align:right;' : 'direction:ltr; text-align:left;'
    var wordBidiCss = isAr ? 'mso-bidi-font-family:"Traditional Arabic"; mso-fareast-font-family:"Traditional Arabic";' : ''
    var markerHtml = function (token) {
      var marker = formatExamMarker(token, lang)
      if (!isAr) return escapeHtml(marker)
      return '<span class="ar-marker">' + escapeHtml(marker) + '</span>'
    }
    var sectionHtml = sections.map(function (section, sectionIndex) {
      var headingParts = getPrintTypeParts(section.type, sectionIndex, lang)
      var modelInstruksi = getPrintTypeInstruction(section.type, lang)
      var items = section.items || []
      var questionsHtml = items.map(function (q, idx) {
        var no = idx + 1
        var qText = '<div class="q-title">' + markerHtml(formatExamNumber(no, lang)) + ' ' + escapeHtml(String(q && q.text || '-')) + '</div>'
        if (section.type === 'pilihan-ganda') {
          var opts = q && q.options || {}
          var mA = isAr ? 'أ' : 'a'
          var mB = isAr ? 'ب' : 'b'
          var mC = isAr ? 'ج' : 'c'
          var mD = isAr ? 'د' : 'd'
          return '<li>' + qText + '<div class="pg-grid">' +
            '<div>' + markerHtml(mA) + ' ' + escapeHtml(String(opts.a || '-')) + '</div>' +
            '<div>' + markerHtml(mC) + ' ' + escapeHtml(String(opts.c || '-')) + '</div>' +
            '<div>' + markerHtml(mB) + ' ' + escapeHtml(String(opts.b || '-')) + '</div>' +
            '<div>' + markerHtml(mD) + ' ' + escapeHtml(String(opts.d || '-')) + '</div>' +
            '</div></li>'
        }
        return '<li>' + qText + '</li>'
      }).join('')
      return '<section class="sec"><h3><strong>' + (isAr ? ('<span class="ar-marker">' + escapeHtml(headingParts.marker) + '</span>') : escapeHtml(headingParts.marker)) + '</strong> ' + escapeHtml(headingParts.label) + '</h3><p>' + escapeHtml(modelInstruksi) + '</p><ol>' + questionsHtml + '</ol></section>'
    }).join('')

    var instruksiUmum = instruksiMeta.text
      ? '<p><strong>' + escapeHtml(t.instruksiUmum) + ':</strong> ' + escapeHtml(instruksiMeta.text) + '</p>'
      : ''

    return '<!doctype html><html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:w="urn:schemas-microsoft-com:office:word" xmlns:v="urn:schemas-microsoft-com:vml" lang="' + (isAr ? 'ar' : 'id') + '" dir="' + (isAr ? 'rtl' : 'ltr') + '">' +
      '<head><meta charset="utf-8"><title>' + escapeHtml(t.title) + '</title>' +
      '<style>@page{size:A4;margin:15mm}body{font-family:' + wordFontFamily + ';' + wordBidiCss + wordDirectionCss + 'margin:0;padding:15mm;color:#111}.page-bg{position:fixed;top:0;left:0;width:210mm;height:297mm;z-index:0}.page-bg img{width:100%;height:100%;display:block}.doc-wrap{position:relative;z-index:1}h1{text-align:center;font-size:22px;margin:0 0 12px 0}.meta p{margin:4px 0;font-size:14px}.sec{margin-top:14px}.sec h3{margin:0 0 6px 0;font-size:16px}.sec p{margin:4px 0;font-size:14px}ol{margin:6px 0 0 0;padding-' + (isAr ? 'right' : 'left') + ':20px}li{margin:8px 0}.q-title{margin-bottom:4px}.pg-grid{display:grid;grid-template-columns:1fr 1fr;gap:2px 18px;margin-' + (isAr ? 'right' : 'left') + ':20px}.ar-marker{display:inline-block;white-space:nowrap;direction:rtl;unicode-bidi:isolate-override;min-width:20px}*{font-family:' + wordFontFamily + ';' + wordBidiCss + '}</style>' +
      '</head><body>' +
      (bgDataUrl ? '<!--[if gte mso 9]><v:background id="bg" o:bwmode="white"><v:fill type="frame" src="' + bgDataUrl + '" /></v:background><![endif]-->' : '') +
      (bgDataUrl ? '<div class="page-bg"><img src="' + bgDataUrl + '" alt=""></div>' : '') +
      '<div class="doc-wrap">' +
      '<h1>' + escapeHtml(t.title) + '</h1>' +
      '<div class="meta">' +
      '<p><strong>' + escapeHtml(t.jenis) + ':</strong> ' + escapeHtml(String(jadwal && jadwal.jenis || '-')) + '</p>' +
      '<p><strong>' + escapeHtml(t.namaUjian) + ':</strong> ' + escapeHtml(String(jadwal && jadwal.nama || '-')) + '</p>' +
      '<p><strong>' + escapeHtml(t.kelas) + ':</strong> ' + escapeHtml(String(jadwal && jadwal.kelas || '-')) + ' | <strong>' + escapeHtml(t.mapel) + ':</strong> ' + escapeHtml(String(jadwal && jadwal.mapel || '-')) + '</p>' +
      '<p><strong>' + escapeHtml(t.tanggal) + ':</strong> ' + escapeHtml(String(jadwal && jadwal.tanggal || '-')) + ' | <strong>' + escapeHtml(t.waktu) + ':</strong> ' + escapeHtml(toTimeLabel(jadwal && jadwal.jam_mulai) + ' - ' + toTimeLabel(jadwal && jadwal.jam_selesai)) + '</p>' +
      '<p><strong>' + escapeHtml(t.guru) + ':</strong> ' + escapeHtml(String(soal && soal.guru_nama || '-')) + '</p>' +
      instruksiUmum +
      '</div>' + sectionHtml + '</div></body></html>'
  }

  async function exportExamWordFile(jadwal, soal, fileName) {
    var html = await buildExamWordHtml(jadwal, soal)
    var blob = new Blob(['\ufeff' + html], { type: 'application/msword;charset=utf-8' })
    var url = URL.createObjectURL(blob)
    var a = document.createElement('a')
    a.href = url
    a.download = fileName
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    setTimeout(function () { URL.revokeObjectURL(url) }, 1200)
  }

  function renderRows(jadwalRows, soalRows) {
    var infoEl = document.getElementById('ujian-placeholder-info')
    if (!infoEl) return

    var soalMap = new Map()
    ;(soalRows || []).forEach(function (item) {
      var key = String(item.jadwal_id || '')
      if (!key) return
      var prev = soalMap.get(key)
      if (!prev) {
        soalMap.set(key, item)
        return
      }
      var prevTime = String(prev.updated_at || prev.created_at || '')
      var curTime = String(item.updated_at || item.created_at || '')
      if (curTime > prevTime) soalMap.set(key, item)
    })

    if (!jadwalRows.length) {
      infoEl.innerHTML = '<div class="placeholder-card">Belum ada jadwal ujian.</div>'
      return
    }

    var html = ''
    html += '<div style="overflow:auto;">'
    html += '<table style="width:100%; border-collapse:collapse; font-size:13px; min-width:980px;">'
    html += '<thead><tr style="background:#f8fafc;">'
    html += '<th style="padding:8px; border:1px solid #e2e8f0; width:44px;">No</th>'
    html += '<th style="padding:8px; border:1px solid #e2e8f0;">Jenis</th>'
    html += '<th style="padding:8px; border:1px solid #e2e8f0;">Nama Ujian</th>'
    html += '<th style="padding:8px; border:1px solid #e2e8f0;">Kelas</th>'
    html += '<th style="padding:8px; border:1px solid #e2e8f0;">Mapel</th>'
    html += '<th style="padding:8px; border:1px solid #e2e8f0;">Tanggal</th>'
    html += '<th style="padding:8px; border:1px solid #e2e8f0;">Guru</th>'
    html += '<th style="padding:8px; border:1px solid #e2e8f0;">Status</th>'
    html += '<th style="padding:8px; border:1px solid #e2e8f0; width:170px;">Aksi</th>'
    html += '</tr></thead><tbody>'

    jadwalRows.forEach(function (row, index) {
      var soal = soalMap.get(String(row.id || '')) || null
      var ready = !!(soal && String(soal.status || '').toLowerCase() === 'submitted')
      html += '<tr>'
      html += '<td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">' + (index + 1) + '</td>'
      html += '<td style="padding:8px; border:1px solid #e2e8f0;">' + escapeHtml(row.jenis || '-') + '</td>'
      html += '<td style="padding:8px; border:1px solid #e2e8f0;">' + escapeHtml(row.nama || '-') + '</td>'
      html += '<td style="padding:8px; border:1px solid #e2e8f0;">' + escapeHtml(row.kelas || '-') + '</td>'
      html += '<td style="padding:8px; border:1px solid #e2e8f0;">' + escapeHtml(row.mapel || '-') + '</td>'
      html += '<td style="padding:8px; border:1px solid #e2e8f0;">' + escapeHtml(row.tanggal || '-') + '</td>'
      html += '<td style="padding:8px; border:1px solid #e2e8f0;">' + escapeHtml(soal && soal.guru_nama || '-') + '</td>'
      html += '<td style="padding:8px; border:1px solid #e2e8f0;">' + escapeHtml(statusLabel(soal && soal.status)) + '</td>'
      html += '<td style="padding:8px; border:1px solid #e2e8f0; white-space:nowrap;">'
      html += '<button type="button" class="ujian-btn" ' + (ready ? '' : 'disabled') + ' onclick="chooseAndPrintAdminExam(\'' + escapeHtml(String(row.id || '')) + '\')">Cetak</button>'
      html += '</td>'
      html += '</tr>'
    })

    html += '</tbody></table></div>'
    infoEl.innerHTML = html

    window.__adminExamJadwalRows = jadwalRows || []
    window.__adminExamSoalRows = soalRows || []
  }

  window.printAdminExam = async function printAdminExam(jadwalId, format) {
    try {
      var mode = String(format || '').trim().toLowerCase()

      var sid = String(jadwalId || '')
      var jadwal = (window.__adminExamJadwalRows || []).find(function (item) { return String(item.id || '') === sid })
      var soal = (window.__adminExamSoalRows || []).find(function (item) {
        return String(item.jadwal_id || '') === sid && String(item.status || '').toLowerCase() === 'submitted'
      })
      if (!jadwal || !soal) {
        alert('Soal belum tersedia untuk dicetak.')
        return
      }
      var lang = parseInstruksiMeta(soal && soal.instruksi).lang || 'ID'
      if (mode === 'word' || (mode !== 'pdf' && lang === 'AR')) {
        window.exportAdminExamWord(jadwalId)
        return
      }

      var doc = await createExamPdfDoc(jadwal, soal)
      if (!doc) {
        alert('Cetak gagal: font Arab/PDF belum siap. Pastikan file TTF tersedia dan refresh halaman.')
        return
      }
      var fileName = 'Soal ' + String(jadwal.nama || 'Ujian') + ' - ' + String(jadwal.kelas || '-') + '.pdf'
      doc.save(fileName)
    } catch (err) {
      console.error('printAdminExam error:', err)
      alert('Cetak gagal: ' + String(err && err.message || err || 'Unknown error'))
    }
  }

  window.chooseAndPrintAdminExam = async function chooseAndPrintAdminExam(jadwalId) {
    try {
      await window.printAdminExam(jadwalId)
    } catch (err) {
      console.error('chooseAndPrintAdminExam error:', err)
      alert('Cetak gagal: ' + String(err && err.message || err || 'Unknown error'))
    }
  }

  window.exportAdminExamWord = function exportAdminExamWord(jadwalId) {
    try {
      var sid = String(jadwalId || '')
      var jadwal = (window.__adminExamJadwalRows || []).find(function (item) { return String(item.id || '') === sid })
      var soal = (window.__adminExamSoalRows || []).find(function (item) {
        return String(item.jadwal_id || '') === sid && String(item.status || '').toLowerCase() === 'submitted'
      })
      if (!jadwal || !soal) {
        alert('Soal belum tersedia untuk export Word.')
        return
      }
      var fileName = 'Soal ' + String(jadwal.nama || 'Ujian') + ' - ' + String(jadwal.kelas || '-') + '.doc'
      await exportExamWordFile(jadwal, soal, fileName)
    } catch (err) {
      console.error('exportAdminExamWord error:', err)
      alert('Export Word gagal: ' + String(err && err.message || err || 'Unknown error'))
    }
  }

  window.initUjianPage = async function initUjianPage() {
    var infoEl = document.getElementById('ujian-placeholder-info')
    if (infoEl) infoEl.innerHTML = 'Loading data ujian...'

    var jadwalRes = await sb
      .from(EXAM_SCHEDULE_TABLE)
      .select('id, jenis, nama, kelas, mapel, tanggal, jam_mulai, jam_selesai')
      .order('tanggal', { ascending: true })

    if (jadwalRes.error) {
      if (isMissingTableError(jadwalRes.error, EXAM_SCHEDULE_TABLE)) {
        if (infoEl) infoEl.innerHTML = '<div class="placeholder-card">Tabel jadwal ujian belum tersedia. Buka menu Jadwal Ujian untuk lihat SQL setup.</div>'
        return
      }
      if (infoEl) infoEl.innerHTML = '<div class="placeholder-card">Gagal memuat jadwal ujian.</div>'
      return
    }

    var ids = (jadwalRes.data || []).map(function (item) { return String(item.id || '') }).filter(Boolean)
    var soalRes = { data: [] }
    if (ids.length) {
      soalRes = await sb
        .from(EXAM_QUESTION_TABLE)
        .select('id, jadwal_id, guru_id, guru_nama, bentuk_soal, jumlah_nomor, instruksi, questions_json, status, created_at, updated_at')
        .in('jadwal_id', ids)
      if (soalRes.error) {
        if (isMissingTableError(soalRes.error, EXAM_QUESTION_TABLE)) {
          soalRes = { data: [] }
        } else {
          soalRes = { data: [] }
        }
      }
    }

    renderRows(jadwalRes.data || [], soalRes.data || [])
  }
})()
