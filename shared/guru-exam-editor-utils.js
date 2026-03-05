;(function initGuruExamEditorUtils() {
  if (window.guruExamEditorUtils) return

  function getTypeShortLabel(type) {
    if (type === 'esai') return 'Esai'
    if (type === 'pasangkan-kata') return 'Pasangkan Kata'
    if (type === 'isi-titik') return 'Isi Titik'
    return 'PG'
  }

  function buildTypeInfoText(typeCfg) {
    const sections = typeCfg?.sections || []
    const sectionLabel = sections.map(item => `${getTypeShortLabel(item.type)} ${item.start}-${item.end}`).join(' | ')
    return `Model aktif: ${sectionLabel || '-'}`
  }

  function buildSectionRowHtml({ i, qType, sectionForNo, sectionIndexByStart, sectionsLength, escapeHtml }) {
    const secData = sectionIndexByStart.get(i) || { idx: 0, ...sectionForNo }
    const secIdx = Number(secData.idx || 0)
    const secCount = Math.max(1, Number(secData.end || i) - Number(secData.start || i) + 1)
    return `
      <div class="placeholder-card guru-ujian-section-row" data-index="${secIdx}" style="margin-bottom:8px; border-color:#cbd5e1; background:#f8fafc;">
        <div style="display:grid; grid-template-columns:1fr 180px auto; gap:8px; align-items:end;">
          <div>
            <label class="guru-label">Model Soal</label>
            <select id="guru-ujian-section-type-${secIdx}" class="guru-field" onchange="onGuruUjianSectionChange()">
              <option value="pilihan-ganda" ${qType === 'pilihan-ganda' ? 'selected' : ''}>Pilihan Ganda</option>
              <option value="esai" ${qType === 'esai' ? 'selected' : ''}>Esai</option>
              <option value="pasangkan-kata" ${qType === 'pasangkan-kata' ? 'selected' : ''}>Pasangkan Kata (A-B)</option>
              <option value="isi-titik" ${qType === 'isi-titik' ? 'selected' : ''}>Isi Titik Kosong</option>
            </select>
          </div>
          <div>
            <label class="guru-label">Jumlah Soal</label>
            <input id="guru-ujian-section-count-${secIdx}" class="guru-field" type="number" min="1" max="200" value="${escapeHtml(String(secCount))}" onchange="onGuruUjianSectionChange()">
          </div>
          <div>
            <button type="button" class="modal-btn" ${sectionsLength > 1 ? '' : 'disabled'} onclick="removeGuruUjianSection(${secIdx})">Hapus</button>
          </div>
        </div>
        <input id="guru-ujian-section-end-${secIdx}" type="hidden" value="${escapeHtml(String(sectionForNo.end || i))}">
        ${qType === 'isi-titik' ? `
          <div style="margin-top:8px;">
            <label class="guru-label">Pilihan Kata (pisahkan koma)</label>
            <input id="guru-ujian-section-words-${secIdx}" class="guru-field" type="text" value="${escapeHtml(String(sectionForNo.wordPool || ''))}" placeholder="ana, ila, aina" onchange="onGuruUjianSectionChange()">
          </div>
        ` : ''}
      </div>
    `
  }

  function buildQuestionPgHtml({ i, localNo, prev, escapeHtml }) {
    const opts = prev.options || {}
    return `
      <div class="placeholder-card guru-ujian-question-row" data-no="${i}" data-type="pilihan-ganda" style="margin-bottom:8px;">
        <div style="font-weight:700; margin-bottom:6px;">Nomor ${localNo} <span style="font-weight:600; color:#2563eb;">(Pilihan Ganda)</span></div>
        <textarea id="guru-ujian-q-${i}" class="guru-field" rows="2" placeholder="Tulis pertanyaan">${escapeHtml(String(prev.text || ''))}</textarea>
        <div style="display:grid; grid-template-columns:1fr 1fr; gap:8px; margin-top:8px;">
          <input id="guru-ujian-q-${i}-a" class="guru-field" type="text" placeholder="Opsi A" value="${escapeHtml(String(opts.a || ''))}">
          <input id="guru-ujian-q-${i}-b" class="guru-field" type="text" placeholder="Opsi B" value="${escapeHtml(String(opts.b || ''))}">
          <input id="guru-ujian-q-${i}-c" class="guru-field" type="text" placeholder="Opsi C" value="${escapeHtml(String(opts.c || ''))}">
          <input id="guru-ujian-q-${i}-d" class="guru-field" type="text" placeholder="Opsi D" value="${escapeHtml(String(opts.d || ''))}">
        </div>
        <div style="margin-top:8px;">
          <label class="guru-label">Kunci Jawaban</label>
          <select id="guru-ujian-q-${i}-answer" class="guru-field" style="max-width:140px;">
            <option value="">Pilih</option>
            <option value="A" ${String(prev.answer || '') === 'A' ? 'selected' : ''}>A</option>
            <option value="B" ${String(prev.answer || '') === 'B' ? 'selected' : ''}>B</option>
            <option value="C" ${String(prev.answer || '') === 'C' ? 'selected' : ''}>C</option>
            <option value="D" ${String(prev.answer || '') === 'D' ? 'selected' : ''}>D</option>
          </select>
        </div>
      </div>
    `
  }

  function buildQuestionPasangkanHtml({ i, localNo, prev, escapeHtml }) {
    const pairs = Array.isArray(prev.pairs) ? prev.pairs : []
    const getPair = idx => pairs[idx] || {}
    return `
      <div class="placeholder-card guru-ujian-question-row" data-no="${i}" data-type="pasangkan-kata" style="margin-bottom:8px;">
        <div style="font-weight:700; margin-bottom:6px;">Nomor ${localNo} <span style="font-weight:600; color:#7c3aed;">(Pasangkan Kata)</span></div>
        <textarea id="guru-ujian-q-${i}" class="guru-field" rows="2" placeholder="Tulis instruksi singkat soal (opsional)">${escapeHtml(String(prev.text || ''))}</textarea>
        <div style="display:grid; grid-template-columns:1fr 1fr; gap:8px; margin-top:8px;">
          <input id="guru-ujian-q-${i}-a1" class="guru-field" type="text" placeholder="Baris A 1" value="${escapeHtml(String(getPair(0).a || ''))}">
          <input id="guru-ujian-q-${i}-b1" class="guru-field" type="text" placeholder="Baris B 1" value="${escapeHtml(String(getPair(0).b || ''))}">
          <input id="guru-ujian-q-${i}-a2" class="guru-field" type="text" placeholder="Baris A 2" value="${escapeHtml(String(getPair(1).a || ''))}">
          <input id="guru-ujian-q-${i}-b2" class="guru-field" type="text" placeholder="Baris B 2" value="${escapeHtml(String(getPair(1).b || ''))}">
          <input id="guru-ujian-q-${i}-a3" class="guru-field" type="text" placeholder="Baris A 3" value="${escapeHtml(String(getPair(2).a || ''))}">
          <input id="guru-ujian-q-${i}-b3" class="guru-field" type="text" placeholder="Baris B 3" value="${escapeHtml(String(getPair(2).b || ''))}">
          <input id="guru-ujian-q-${i}-a4" class="guru-field" type="text" placeholder="Baris A 4" value="${escapeHtml(String(getPair(3).a || ''))}">
          <input id="guru-ujian-q-${i}-b4" class="guru-field" type="text" placeholder="Baris B 4" value="${escapeHtml(String(getPair(3).b || ''))}">
        </div>
      </div>
    `
  }

  function buildQuestionIsiTitikHtml({ i, localNo, prev, sectionForNo, escapeHtml }) {
    const fragments = Array.isArray(sectionForNo?.wordList) ? sectionForNo.wordList : []
    return `
      <div class="placeholder-card guru-ujian-question-row" data-no="${i}" data-type="isi-titik" style="margin-bottom:8px;">
        <div style="font-weight:700; margin-bottom:6px;">Nomor ${localNo} <span style="font-weight:600; color:#ea580c;">(Isi Titik Kosong)</span></div>
        <div style="font-size:12px; color:#64748b; margin-bottom:6px;">Pilihan kata: (${escapeHtml(fragments.join(', ') || '-')})</div>
        <textarea id="guru-ujian-q-${i}" class="guru-field" rows="2" placeholder="Tulis kalimat dengan titik kosong, contoh: Ana ... ila madrasah.">${escapeHtml(String(prev.text || ''))}</textarea>
        <input id="guru-ujian-q-${i}-answer" class="guru-field" type="text" placeholder="Jawaban benar" value="${escapeHtml(String(prev.answer || ''))}" style="margin-top:8px;">
      </div>
    `
  }

  function buildQuestionEsaiHtml({ i, localNo, prev, escapeHtml }) {
    return `
      <div class="placeholder-card guru-ujian-question-row" data-no="${i}" data-type="esai" style="margin-bottom:8px;">
        <div style="font-weight:700; margin-bottom:6px;">Nomor ${localNo} <span style="font-weight:600; color:#0f766e;">(Esai)</span></div>
        <textarea id="guru-ujian-q-${i}" class="guru-field" rows="3" placeholder="Tulis pertanyaan">${escapeHtml(String(prev.text || ''))}</textarea>
      </div>
    `
  }

  function buildQuestionCardHtml({ i, qType, localNo, prev, sectionForNo, escapeHtml }) {
    if (qType === 'pilihan-ganda') return buildQuestionPgHtml({ i, localNo, prev, escapeHtml })
    if (qType === 'pasangkan-kata') return buildQuestionPasangkanHtml({ i, localNo, prev, escapeHtml })
    if (qType === 'isi-titik') return buildQuestionIsiTitikHtml({ i, localNo, prev, sectionForNo, escapeHtml })
    return buildQuestionEsaiHtml({ i, localNo, prev, escapeHtml })
  }

  window.guruExamEditorUtils = {
    getTypeShortLabel,
    buildTypeInfoText,
    buildSectionRowHtml,
    buildQuestionPgHtml,
    buildQuestionPasangkanHtml,
    buildQuestionIsiTitikHtml,
    buildQuestionEsaiHtml,
    buildQuestionCardHtml
  }
})()
