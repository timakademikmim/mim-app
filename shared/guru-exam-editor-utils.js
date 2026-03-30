;(function initGuruExamEditorUtils() {
  if (window.guruExamEditorUtils) return

  function getTypeShortLabel(type) {
    if (type === 'esai') return 'Esai'
    if (type === 'pasangkan-kata') return 'Pasangkan Kata'
    if (type === 'isi-titik') return 'Isi Titik'
    return 'PG'
  }

  function getTypeTitleLabel(type) {
    if (type === 'esai') return 'Esai'
    if (type === 'pasangkan-kata') return 'Pasangkan Kata'
    if (type === 'isi-titik') return 'Isi Titik Kosong'
    return 'Pilihan Ganda'
  }

  function getTypeAccent(type) {
    if (type === 'esai') {
      return {
        border: '#99f6e4',
        background: 'linear-gradient(180deg,#ecfeff 0%,#ffffff 100%)',
        badgeBg: '#ccfbf1',
        badgeText: '#0f766e',
        summaryText: '#0f766e',
        noteBorder: '#99f6e4',
        noteBg: '#f0fdfa',
        noteText: '#115e59',
        groupBorder: '#99f6e4',
        groupBackground: 'linear-gradient(180deg,#ecfeff 0%,#ffffff 28%)',
        groupShadow: '0 10px 30px rgba(13,148,136,0.08)',
        groupStripe: '#2dd4bf'
      }
    }
    if (type === 'pasangkan-kata') {
      return {
        border: '#c4b5fd',
        background: 'linear-gradient(180deg,#f5f3ff 0%,#ffffff 100%)',
        badgeBg: '#ede9fe',
        badgeText: '#6d28d9',
        summaryText: '#7c3aed',
        noteBorder: '#c4b5fd',
        noteBg: '#f5f3ff',
        noteText: '#6d28d9',
        groupBorder: '#c4b5fd',
        groupBackground: 'linear-gradient(180deg,#f5f3ff 0%,#ffffff 28%)',
        groupShadow: '0 10px 30px rgba(124,58,237,0.08)',
        groupStripe: '#8b5cf6'
      }
    }
    if (type === 'isi-titik') {
      return {
        border: '#fdba74',
        background: 'linear-gradient(180deg,#fff7ed 0%,#ffffff 100%)',
        badgeBg: '#fed7aa',
        badgeText: '#c2410c',
        summaryText: '#ea580c',
        noteBorder: '#fdba74',
        noteBg: '#fff7ed',
        noteText: '#9a3412',
        groupBorder: '#fdba74',
        groupBackground: 'linear-gradient(180deg,#fff7ed 0%,#ffffff 28%)',
        groupShadow: '0 10px 30px rgba(234,88,12,0.08)',
        groupStripe: '#f97316'
      }
    }
    return {
      border: '#bfdbfe',
      background: 'linear-gradient(180deg,#eff6ff 0%,#ffffff 100%)',
      badgeBg: '#dbeafe',
      badgeText: '#1d4ed8',
      summaryText: '#2563eb',
      noteBorder: '#bfdbfe',
      noteBg: '#eff6ff',
      noteText: '#1e40af',
      groupBorder: '#bfdbfe',
      groupBackground: 'linear-gradient(180deg,#eff6ff 0%,#ffffff 28%)',
      groupShadow: '0 10px 30px rgba(37,99,235,0.08)',
      groupStripe: '#60a5fa'
    }
  }

  function buildTypeInfoText(typeCfg) {
    const sections = typeCfg?.sections || []
    const sectionLabel = sections.map(item => `${getTypeShortLabel(item.type)} ${item.start}-${item.end}`).join(' | ')
    return `Model aktif: ${sectionLabel || '-'}`
  }

  function buildSectionRowHtml({
    i,
    qType,
    sectionForNo,
    sectionIndexByStart,
    sectionsLength,
    escapeHtml,
    scorePlanItem = null,
    scoreMeta = null,
    scoreOverAllocated = false,
    scoreInputValue = undefined,
    formatScoreValue = value => String(value == null ? '' : value)
  }) {
    const secData = sectionIndexByStart.get(i) || { idx: 0, ...sectionForNo }
    const secIdx = Number(secData.idx || 0)
    const secCount = Math.max(1, Number(secData.end || i) - Number(secData.start || i) + 1)
    const secInstruction = String(secData.instruction || sectionForNo?.instruction || '')
    const rawSectionScore = scoreInputValue !== undefined
      ? scoreInputValue
      : (secData.score ?? sectionForNo?.score ?? '')
    const rawSectionScoreText = String(rawSectionScore ?? '').trim()
    const sectionScore = rawSectionScoreText !== '' && Number.isFinite(Number(rawSectionScoreText)) ? Number(rawSectionScoreText) : null
    const questionCount = Math.max(1, Number(scorePlanItem?.questionCount || secCount))
    const explicitScore = Number.isFinite(Number(scorePlanItem?.explicitScore)) ? Number(scorePlanItem.explicitScore) : null
    const recommendedScore = Number.isFinite(Number(scorePlanItem?.recommendedScore)) ? Number(scorePlanItem.recommendedScore) : null
    const perQuestionScore = Number.isFinite(Number(scorePlanItem?.perQuestionScore)) ? Number(scorePlanItem.perQuestionScore) : null
    const sectionScoreValue = sectionScore !== null ? formatScoreValue(sectionScore) : escapeHtml(rawSectionScoreText)
    const sectionCode = String.fromCharCode(65 + (secIdx % 26))
    const sectionLabel = getTypeShortLabel(qType)
    const sectionTitle = getTypeTitleLabel(qType)
    const accent = getTypeAccent(qType)
    let scoreHint = 'Kosongkan jika ingin melihat rekomendasi otomatis dari sisa nilai.'
    let scoreHintColor = accent.noteText
    if (scoreOverAllocated) {
      scoreHint = 'Total nilai bagian soal melewati batas maksimal ujian. Kurangi sebagian alokasi.'
      scoreHintColor = '#b91c1c'
    } else if (explicitScore !== null && perQuestionScore !== null) {
      scoreHint = questionCount > 1
        ? `Setiap soal pada bagian ini bernilai ${formatScoreValue(perQuestionScore)} poin.`
        : `Bagian ini bernilai ${formatScoreValue(explicitScore)} poin.`
    } else if (recommendedScore !== null && perQuestionScore !== null) {
      scoreHint = questionCount > 1
        ? `Rekomendasi ${formatScoreValue(recommendedScore)} poin (${formatScoreValue(perQuestionScore)} poin per soal).`
        : `Rekomendasi ${formatScoreValue(recommendedScore)} poin untuk bagian ini.`
    } else if (scoreMeta && Number.isFinite(Number(scoreMeta.maxScore))) {
      scoreHint = `Batas maksimal ${String(scoreMeta.label || 'ujian').toLowerCase()} adalah ${formatScoreValue(scoreMeta.maxScore)} poin.`
    }
    return `
      <div class="placeholder-card guru-ujian-section-row" data-index="${secIdx}" style="margin-bottom:12px; border:1px solid ${accent.border}; border-radius:14px; background:${accent.background}; box-shadow:0 1px 2px rgba(15,23,42,0.04);">
        <div style="display:flex; align-items:flex-start; justify-content:space-between; gap:12px; margin-bottom:10px; flex-wrap:wrap;">
          <div>
            <div style="display:inline-flex; align-items:center; gap:8px; margin-bottom:4px;">
              <span style="display:inline-flex; align-items:center; justify-content:center; min-width:34px; height:34px; padding:0 10px; border-radius:999px; background:${accent.badgeBg}; color:${accent.badgeText}; font-size:12px; font-weight:800;">${escapeHtml(sectionCode)}</span>
              <div style="font-size:15px; font-weight:700; color:#0f172a;">${escapeHtml(sectionTitle)}</div>
            </div>
            <div style="font-size:12px; color:${accent.summaryText};">${escapeHtml(sectionLabel)} | Nomor ${escapeHtml(String(i))}-${escapeHtml(String(sectionForNo.end || i))}</div>
          </div>
          <div style="display:flex; align-items:center; width:100%; max-width:120px;">
            <button type="button" class="modal-btn" ${sectionsLength > 1 ? '' : 'disabled'} onclick="removeGuruUjianSection(${secIdx})" style="width:100%;">Hapus</button>
          </div>
        </div>
        <div style="display:grid; grid-template-columns:repeat(auto-fit,minmax(180px,1fr)); gap:10px; align-items:end;">
          <div style="min-width:0;">
            <label class="guru-label">Model Soal</label>
            <select id="guru-ujian-section-type-${secIdx}" class="guru-field" onchange="onGuruUjianSectionChange()">
              <option value="pilihan-ganda" ${qType === 'pilihan-ganda' ? 'selected' : ''}>Pilihan Ganda</option>
              <option value="esai" ${qType === 'esai' ? 'selected' : ''}>Esai</option>
              <option value="pasangkan-kata" ${qType === 'pasangkan-kata' ? 'selected' : ''}>Pasangkan Kata (A-B)</option>
              <option value="isi-titik" ${qType === 'isi-titik' ? 'selected' : ''}>Isi Titik Kosong</option>
            </select>
          </div>
          <div style="min-width:0;">
            <label class="guru-label">Jumlah Soal</label>
            <input id="guru-ujian-section-count-${secIdx}" class="guru-field" type="number" min="1" max="200" value="${escapeHtml(String(secCount))}" onchange="onGuruUjianSectionChange()">
          </div>
          <div style="min-width:0;">
            <label class="guru-label">Nilai Bagian</label>
            <input id="guru-ujian-section-score-${secIdx}" class="guru-field" type="text" inputmode="decimal" value="${sectionScoreValue}" placeholder="${recommendedScore !== null ? formatScoreValue(recommendedScore) : 'Opsional'}" oninput="onGuruUjianSectionScoreInput(${secIdx}, this.value)" onkeydown="onGuruUjianSectionScoreKeydown(event, ${secIdx}, this)" onchange="onGuruUjianSectionChange()">
            <div style="margin-top:6px; font-size:11px; line-height:1.5; color:${scoreHintColor};">${escapeHtml(scoreHint)}</div>
          </div>
        </div>
        <input id="guru-ujian-section-end-${secIdx}" type="hidden" value="${escapeHtml(String(sectionForNo.end || i))}">
        <div style="margin-top:10px; padding:10px 12px; border:1px solid ${accent.noteBorder}; border-radius:12px; background:${accent.noteBg};">
          <label class="guru-label">Instruksi Bagian Soal</label>
          <textarea id="guru-ujian-section-instruction-${secIdx}" class="guru-field" rows="2" placeholder="Opsional. Jika diisi, akan tampil di bawah judul bagian soal saat dicetak." onchange="onGuruUjianSectionChange()" style="background:#fff;">${escapeHtml(secInstruction)}</textarea>
          <div style="margin-top:6px; font-size:11px; line-height:1.5; color:${accent.noteText};">Kosongkan jika bagian ini tidak perlu catatan khusus di hasil cetak.</div>
        </div>
        ${qType === 'isi-titik' ? `
          <div style="margin-top:10px; padding:10px 12px; border:1px dashed #fdba74; border-radius:12px; background:#fff7ed;">
            <label class="guru-label">Pilihan Kata (pisahkan koma)</label>
            <input id="guru-ujian-section-words-${secIdx}" class="guru-field" type="text" value="${escapeHtml(String(sectionForNo.wordPool || ''))}" placeholder="ana, ila, aina" onchange="onGuruUjianSectionChange()">
            <div style="margin-top:6px; font-size:11px; line-height:1.5; color:#9a3412;">Urutan boleh bebas. Pisahkan setiap kata dengan tanda koma.</div>
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
        <div style="display:grid; grid-template-columns:repeat(auto-fit,minmax(180px,1fr)); gap:8px; margin-top:8px;">
          <input id="guru-ujian-q-${i}-a" class="guru-field" type="text" placeholder="Opsi A" value="${escapeHtml(String(opts.a || ''))}">
          <input id="guru-ujian-q-${i}-b" class="guru-field" type="text" placeholder="Opsi B" value="${escapeHtml(String(opts.b || ''))}">
          <input id="guru-ujian-q-${i}-c" class="guru-field" type="text" placeholder="Opsi C" value="${escapeHtml(String(opts.c || ''))}">
          <input id="guru-ujian-q-${i}-d" class="guru-field" type="text" placeholder="Opsi D" value="${escapeHtml(String(opts.d || ''))}">
        </div>
        <div style="margin-top:8px;">
          <label class="guru-label">Kunci Jawaban</label>
          <select id="guru-ujian-q-${i}-answer" class="guru-field" style="width:100%; max-width:180px;">
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
    const legacyPairs = Array.isArray(prev.pairs) ? prev.pairs : []
    const columnA = Array.isArray(prev.columnA)
      ? prev.columnA
      : legacyPairs.map(item => String(item?.a || '').trim()).filter(Boolean)
    const columnB = Array.isArray(prev.columnB)
      ? prev.columnB
      : legacyPairs.map(item => String(item?.b || '').trim()).filter(Boolean)
    const renderInputs = (items, sideLabel, sideKey, accent) => {
      const values = [...items.map(item => String(item || '')), '']
      return `
        <div style="min-width:0;">
          <div style="display:flex; align-items:center; justify-content:space-between; gap:8px; margin-bottom:6px;">
            <label class="guru-label" style="margin:0;">${sideLabel}</label>
            <span style="font-size:11px; color:${accent};">Tambah terus sesuai kebutuhan</span>
          </div>
          <div id="guru-ujian-q-${i}-col-${sideKey}" style="display:flex; flex-direction:column; gap:8px;">
            ${values.map((value, idx) => `
              <input
                id="guru-ujian-q-${i}-${sideKey}-${idx + 1}"
                class="guru-field guru-ujian-match-input"
                data-side="${sideKey}"
                type="text"
                value="${escapeHtml(value)}"
                placeholder="Masukkan pilihan ${sideLabel}"
                oninput="ensureGuruUjianMatchingTrailingInput(${i}, '${sideKey}')"
              >
            `).join('')}
          </div>
        </div>
      `
    }
    return `
      <div class="placeholder-card guru-ujian-question-row" data-no="${i}" data-type="pasangkan-kata" style="margin-bottom:8px;">
        <div style="display:grid; grid-template-columns:minmax(0,1fr) minmax(0,1fr); gap:12px; align-items:start;">
          ${renderInputs(columnA, 'Qoimah A', 'a', '#7c3aed')}
          ${renderInputs(columnB, 'Qoimah B', 'b', '#7c3aed')}
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
    getTypeAccent,
    buildTypeInfoText,
    buildSectionRowHtml,
    buildQuestionPgHtml,
    buildQuestionPasangkanHtml,
    buildQuestionIsiTitikHtml,
    buildQuestionEsaiHtml,
    buildQuestionCardHtml
  }
})()
