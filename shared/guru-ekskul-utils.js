;(function initGuruEkskulUtils() {
  if (window.guruEkskulUtils) return

  function getEmptyStarsHtml() {
    return Array.from({ length: 5 })
      .map(() => '<span style="color:#cbd5e1; font-size:17px; line-height:1;">&#9733;</span>')
      .join('')
  }

  function buildStarsHtml(value, { emptyAsDash = true } = {}) {
    const num = Number(value)
    if (!Number.isFinite(num)) return emptyAsDash ? '-' : getEmptyStarsHtml()
    const score = Math.max(1, Math.min(100, num))
    const rating = Math.round((score / 20) * 2) / 2
    const full = Math.floor(rating)
    const half = rating - full >= 0.5
    let html = ''
    for (let i = 0; i < 5; i += 1) {
      if (i < full) {
        html += '<span style="color:#f59e0b; font-size:17px; line-height:1;">&#9733;</span>'
      } else if (i === full && half) {
        html += '<span style="background:linear-gradient(90deg,#f59e0b 50%,#cbd5e1 50%); -webkit-background-clip:text; background-clip:text; color:transparent; -webkit-text-fill-color:transparent; font-size:17px; line-height:1;">&#9733;</span>'
      } else {
        html += '<span style="color:#cbd5e1; font-size:17px; line-height:1;">&#9733;</span>'
      }
    }
    return html
  }

  function buildMemberCardHtml({ sid, name, index, isActive, escapeHtml }) {
    return `
      <div style="display:grid; grid-template-columns:1fr auto; gap:6px; align-items:center; padding:8px; border:1px solid #e2e8f0; border-radius:8px; margin-bottom:6px; background:#fff;">
        <button type="button" class="modal-btn" onclick="selectGuruEkskulProgresSantri('${escapeHtml(sid)}')" style="display:block; width:100%; text-align:left; font-size:13px; ${isActive ? 'border-color:#d4d456; background:#fefce8; font-weight:700;' : 'font-weight:500;'}">${index}. ${escapeHtml(String(name || '-'))}</button>
        <button type="button" class="modal-btn" onclick="openGuruEkskulSantriDetail('${escapeHtml(sid)}')">Detail</button>
      </div>
    `
  }

  function buildIndikatorCardHtml({ nama, deskripsi, escapeHtml }) {
    return `
      <div style="padding:6px 8px; border:1px solid #e2e8f0; border-radius:8px; margin-bottom:6px;">
        <div style="font-weight:600; font-size:13px;">${escapeHtml(String(nama || '-'))}</div>
        <div style="font-size:12px; color:#64748b;">${escapeHtml(String(deskripsi || '-'))}</div>
      </div>
    `
  }

  function buildProgressDetailRowHtml({ tanggal, indikatorNama, nilaiHtml, catatan, escapeHtml }) {
    return `
      <tr>
        <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(String(tanggal || '-'))}</td>
        <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(String(indikatorNama || 'Umum'))}</td>
        <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${nilaiHtml}</td>
        <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(String(catatan || '-'))}</td>
      </tr>
    `
  }

  function buildMonthlyTableRowHtml({ index, sid, nama, kehadiranValue, catatanValue, escapeHtml }) {
    return `
      <tr data-guru-ekskul-monthly-row="1" data-santri-id="${escapeHtml(sid)}">
        <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${index}</td>
        <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(String(nama || '-'))}</td>
        <td style="padding:8px; border:1px solid #e2e8f0;">
          <input class="guru-field" type="number" min="0" max="100" step="0.01" placeholder="0-100" data-guru-ekskul-monthly-kehadiran="1" value="${escapeHtml(String(kehadiranValue || ''))}">
        </td>
        <td style="padding:8px; border:1px solid #e2e8f0;">
          <input class="guru-field" type="text" placeholder="Catatan PJ ekskul" data-guru-ekskul-monthly-catatan="1" value="${escapeHtml(String(catatanValue || ''))}">
        </td>
      </tr>
    `
  }

  function buildProgressInputTableRowHtml({ index, iid, nama, deskripsi, emptyStarsHtml, escapeHtml }) {
    return `
      <tr data-guru-ekskul-indikator-row="1" data-indikator-id="${escapeHtml(iid)}">
        <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${index}</td>
        <td style="padding:8px; border:1px solid #e2e8f0;">
          <div style="font-weight:600;">${escapeHtml(String(nama || '-'))}</div>
          <div style="font-size:11px; color:#64748b;">${escapeHtml(String(deskripsi || '-'))}</div>
        </td>
        <td style="padding:8px; border:1px solid #e2e8f0;">
          <div style="display:grid; grid-template-columns:86px 1fr; gap:8px; align-items:center;">
            <input class="guru-field" type="number" min="1" max="100" step="1" placeholder="1-100" data-guru-ekskul-indikator-nilai="1" oninput="guruEkskulUpdateIndicatorStars(this)">
            <div data-guru-ekskul-star-view="1">${emptyStarsHtml}</div>
          </div>
        </td>
        <td style="padding:8px; border:1px solid #e2e8f0;">
          <input class="guru-field" type="text" placeholder="Catatan indikator" data-guru-ekskul-indikator-catatan="1">
        </td>
      </tr>
    `
  }

  function buildEkskulPageHtml({ ekskulRows, selectedEkskulId, monthlyPeriode, todayDate, monthToday, escapeHtml }) {
    const rows = Array.isArray(ekskulRows) ? ekskulRows : []
    return `
      <div style="display:grid; gap:12px;">
        <div style="border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">
          <div style="font-weight:700; margin-bottom:8px;">Ekskul Binaan</div>
          <div style="display:flex; flex-wrap:wrap; gap:8px;">
            ${rows.map(item => `
              <button type="button" class="modal-btn" onclick="selectGuruEkskul('${escapeHtml(String(item.id || ''))}')" style="${String(selectedEkskulId || '') === String(item.id || '') ? 'border-color:#d4d456; background:#fefce8;' : ''}">
                ${escapeHtml(String(item.nama || '-'))}
              </button>
            `).join('')}
          </div>
        </div>

        <div style="display:flex; gap:8px; flex-wrap:wrap;">
          <button id="guru-ekskul-tab-btn-progres" type="button" class="modal-btn modal-btn-primary" onclick="setGuruEkskulTab('progres')">Input Progres</button>
          <button id="guru-ekskul-tab-btn-laporan" type="button" class="modal-btn" onclick="setGuruEkskulTab('laporan')">Laporan Bulanan Ekskul</button>
        </div>

        <div id="guru-ekskul-tab-progres" style="display:grid; gap:12px;">
          <div style="display:grid; grid-template-columns:1fr 1fr; gap:12px;">
            <div style="border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">
              <div style="font-weight:700; margin-bottom:8px;">Anggota Ekskul</div>
              <div style="display:grid; grid-template-columns:1fr auto; gap:8px; margin-bottom:8px;">
                <select id="guru-ekskul-santri" class="guru-field"></select>
                <button type="button" class="modal-btn modal-btn-primary" onclick="addGuruEkskulMember()">Tambah</button>
              </div>
              <div id="guru-ekskul-member-list">Loading...</div>
            </div>

            <div style="border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">
              <div style="font-weight:700; margin-bottom:8px;">Indikator Penilaian</div>
              <div style="display:grid; gap:8px; margin-bottom:8px;">
                <input id="guru-ekskul-indikator-nama" class="guru-field" type="text" placeholder="Nama indikator">
                <input id="guru-ekskul-indikator-deskripsi" class="guru-field" type="text" placeholder="Deskripsi indikator (opsional)">
                <button type="button" class="modal-btn modal-btn-primary" onclick="addGuruEkskulIndikator()">Tambah Indikator</button>
              </div>
              <div id="guru-ekskul-indikator-list">Loading...</div>
            </div>
          </div>

          <div style="border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">
            <div style="font-weight:700; margin-bottom:8px;">Input Progres Ekskul</div>
            <div style="display:grid; grid-template-columns:160px 1fr auto; gap:8px; margin-bottom:8px;">
              <input id="guru-ekskul-progres-tanggal" class="guru-field" type="date" value="${escapeHtml(String(todayDate || ''))}">
              <select id="guru-ekskul-progres-santri" class="guru-field" onchange="selectGuruEkskulProgresSantri(this.value)"></select>
              <button type="button" class="modal-btn modal-btn-primary" onclick="saveGuruEkskulProgressBatch()">Submit Progres</button>
            </div>
            <div id="guru-ekskul-progres-input-list">Loading...</div>
          </div>
        </div>

        <div id="guru-ekskul-tab-laporan" style="display:none; border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px;">
          <div style="font-weight:700; margin-bottom:8px;">Input Laporan Bulanan Ekskul</div>
          <div style="font-size:12px; color:#64748b; margin-bottom:8px;">Data ini akan dipakai di Detail Laporan Bulanan pada page guru (bagian D. Ekstrakulikuler).</div>
          <div style="display:grid; grid-template-columns:180px 1fr auto; gap:8px; margin-bottom:8px; align-items:end;">
            <div>
              <label class="guru-label">Periode Bulan</label>
              <input id="guru-ekskul-monthly-periode" class="guru-field" type="month" value="${escapeHtml(String(monthlyPeriode || monthToday || ''))}" onchange="onGuruEkskulMonthlyPeriodeChange()">
            </div>
            <div></div>
            <div>
              <button type="button" class="modal-btn modal-btn-primary" onclick="saveGuruEkskulMonthlyReport()">Simpan Laporan Bulanan</button>
            </div>
          </div>
          <div id="guru-ekskul-monthly-list">Loading...</div>
        </div>
      </div>
      <div id="guru-ekskul-santri-detail-overlay" style="display:none; position:fixed; inset:0; background:rgba(15,23,42,0.45); z-index:2000; padding:20px;">
        <div style="max-width:940px; margin:0 auto; background:#fff; border-radius:12px; border:1px solid #e2e8f0; overflow:hidden;">
          <div style="display:flex; align-items:center; justify-content:space-between; padding:12px 14px; border-bottom:1px solid #e2e8f0;">
            <div id="guru-ekskul-santri-detail-title" style="font-weight:700; color:#0f172a;">Detail Progres</div>
            <button type="button" class="modal-btn" onclick="closeGuruEkskulSantriDetail()">Tutup</button>
          </div>
          <div id="guru-ekskul-santri-detail-body" style="padding:12px; max-height:70vh; overflow:auto;">Loading...</div>
        </div>
      </div>
    `
  }

  window.guruEkskulUtils = {
    buildStarsHtml,
    getEmptyStarsHtml,
    buildMemberCardHtml,
    buildIndikatorCardHtml,
    buildProgressDetailRowHtml,
    buildMonthlyTableRowHtml,
    buildProgressInputTableRowHtml,
    buildEkskulPageHtml
  }
})()
