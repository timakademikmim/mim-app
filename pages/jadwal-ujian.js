(function () {
  var EXAM_SCHEDULE_TABLE = 'jadwal_ujian'
  var folderRowsCache = []
  var activeMapelRow = null

  function escapeHtml(value) {
    return String(value == null ? '' : value)
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#39;')
  }

  function isMissingTableError(error) {
    var msg = String(error && error.message || '').toLowerCase()
    var code = String(error && error.code || '').toUpperCase()
    return code === '42P01' ||
      msg.includes('does not exist') ||
      msg.includes('relation') ||
      msg.includes('could not find the table') ||
      msg.includes('public.' + EXAM_SCHEDULE_TABLE)
  }

  function missingTableMessage() {
    return "Tabel '" + EXAM_SCHEDULE_TABLE + "' belum ada.\\n\\nJalankan SQL:\\ncreate table if not exists public." + EXAM_SCHEDULE_TABLE + " (\\n  id uuid primary key default gen_random_uuid(),\\n  jenis text not null,\\n  nama text not null,\\n  kelas text not null,\\n  mapel text not null,\\n  tanggal date not null,\\n  jam_mulai time,\\n  jam_selesai time,\\n  lokasi text,\\n  keterangan text,\\n  created_at timestamptz not null default now(),\\n  updated_at timestamptz not null default now()\\n);"
  }

  function detectPerangkatan(kelasName) {
    var text = String(kelasName || '').trim().toLowerCase()
    if (!text) return 'SMP'
    if (text.includes('smp') || /^([789])([a-z]|\b|[-\s]|$)/i.test(text) || /\b7\b|\b8\b|\b9\b/.test(text)) return 'SMP'
    if (text.includes('sma') || text.includes('ma ') || text.endsWith(' ma') || /^(x|xi|xii)(\b|[-\s]|$)/i.test(text) || /\b10\b|\b11\b|\b12\b/.test(text)) return 'SMA'
    return 'SMP'
  }

  function todayDate() {
    return new Date().toISOString().slice(0, 10)
  }

  function parseMeta(row) {
    var raw = String(row && row.keterangan || '').trim()
    if (!raw) return {}
    try {
      var parsed = JSON.parse(raw)
      return parsed && typeof parsed === 'object' ? parsed : {}
    } catch (_err) {
      return {}
    }
  }

  function getInput(id) {
    return document.getElementById(id)
  }

  async function getActiveSemesterAndYear() {
    var semesterRes = await sb
      .from('semester')
      .select('id, nama')
      .eq('aktif', true)
      .order('id', { ascending: false })
      .limit(1)

    var tahunRes = await sb
      .from('tahun_ajaran')
      .select('id, nama')
      .eq('aktif', true)
      .order('id', { ascending: false })
      .limit(1)

    return {
      semester: semesterRes.data && semesterRes.data[0] ? semesterRes.data[0] : null,
      tahun: tahunRes.data && tahunRes.data[0] ? tahunRes.data[0] : null
    }
  }

  async function loadDistribusiMapelAktif() {
    var active = await getActiveSemesterAndYear()

    var distribusiRes = await sb
      .from('distribusi_mapel')
      .select('id, kelas_id, mapel_id, semester_id')

    if (distribusiRes.error) throw distribusiRes.error

    var kelasRes = await sb.from('kelas').select('id, nama_kelas')
    if (kelasRes.error) throw kelasRes.error

    var mapelRes = await sb.from('mapel').select('id, nama_mapel')
    if (mapelRes.error) {
      mapelRes = await sb.from('mapel').select('id, nama')
    }
    if (mapelRes.error) throw mapelRes.error

    var kelasMap = new Map((kelasRes.data || []).map(function (item) { return [String(item.id || ''), item] }))
    var mapelMap = new Map((mapelRes.data || []).map(function (item) { return [String(item.id || ''), item] }))

    var list = (distribusiRes.data || []).filter(function (item) {
      if (!active.semester || !active.semester.id) return true
      return String(item.semester_id || '') === String(active.semester.id)
    })

    return {
      active: active,
      rows: list,
      kelasMap: kelasMap,
      mapelMap: mapelMap
    }
  }

  function groupMapelPerangkatan(rows, kelasMap, mapelMap) {
    var grouped = new Map()
    ;(rows || []).forEach(function (item) {
      var kelas = kelasMap.get(String(item.kelas_id || ''))
      var mapel = mapelMap.get(String(item.mapel_id || ''))
      var kelasNama = String(kelas && kelas.nama_kelas || '').trim()
      var mapelNama = String(mapel && (mapel.nama_mapel || mapel.nama) || '').trim()
      if (!kelasNama || !mapelNama) return

      var perangkatan = detectPerangkatan(kelasNama)
      var mapelNamaLabel = mapelNama
      if (perangkatan === 'SMP') {
        mapelNamaLabel = mapelNamaLabel.replace(/\bumum\b/gi, 'SMP').replace(/\s{2,}/g, ' ').trim()
      }
      var key = mapelNamaLabel.toLowerCase() + '|' + perangkatan
      if (!grouped.has(key)) {
        var label = mapelNamaLabel
        var rePerangkatanSuffix = new RegExp('\\s+' + perangkatan + '$', 'i')
        if (!rePerangkatanSuffix.test(label)) label = (label + ' ' + perangkatan).trim()
        grouped.set(key, {
          mapel_nama: mapelNamaLabel,
          perangkatan: perangkatan,
          mapel_label: label,
          class_rows: []
        })
      }
      var target = grouped.get(key)
      if (!target.class_rows.some(function (x) { return String(x.kelas_id || '') === String(item.kelas_id || '') })) {
        target.class_rows.push({
          kelas_id: String(item.kelas_id || ''),
          kelas_nama: kelasNama,
          lokasi: '',
          pengawas: ''
        })
      }
    })

    return Array.from(grouped.values()).sort(function (a, b) {
      var pa = String(a.perangkatan || '')
      var pb = String(b.perangkatan || '')
      if (pa !== pb) return pa.localeCompare(pb)
      return String(a.mapel_nama || '').localeCompare(String(b.mapel_nama || ''))
    })
  }

  async function readRows() {
    var listEl = getInput('ju-folder-list')
    if (listEl) listEl.innerHTML = 'Loading...'

    var res = await sb
      .from(EXAM_SCHEDULE_TABLE)
      .select('id, jenis, nama, kelas, mapel, tanggal, jam_mulai, jam_selesai, lokasi, keterangan, created_at, updated_at')
      .order('nama', { ascending: true })
      .order('mapel', { ascending: true })

    if (res.error) {
      if (isMissingTableError(res.error)) {
        alert(missingTableMessage())
      } else {
        alert('Gagal load jadwal ujian: ' + String(res.error.message || 'Unknown error'))
      }
      if (listEl) listEl.innerHTML = '<div class="placeholder-card">Gagal memuat data jadwal ujian.</div>'
      return []
    }

    folderRowsCache = res.data || []
    return folderRowsCache
  }

  function renderFolders(rows) {
    var listEl = getInput('ju-folder-list')
    if (!listEl) return

    if (!rows.length) {
      listEl.innerHTML = '<div class="placeholder-card">Belum ada folder ujian.</div>'
      return
    }

    var grouped = new Map()
    rows.forEach(function (row) {
      var key = String(row.nama || '-')
      if (!grouped.has(key)) grouped.set(key, [])
      grouped.get(key).push(row)
    })

    var html = ''
    Array.from(grouped.entries()).forEach(function (entry) {
      var folderName = entry[0]
      var items = entry[1]
      var jenis = String(items[0] && items[0].jenis || '-')
      html += '<div class="ju-folder">'
      html += '<div style="display:flex; align-items:center; justify-content:space-between; gap:8px; flex-wrap:wrap; margin-bottom:8px;">'
      html += '<div>'
      html += '<div style="font-weight:700; color:#0f172a;">' + escapeHtml(folderName) + '</div>'
      html += '<div style="font-size:12px; color:#64748b;">Jenis: ' + escapeHtml(jenis) + ' | Total mapel perangkatan: ' + items.length + '</div>'
      html += '</div>'
      html += '<button type="button" class="ju-btn secondary" onclick="deleteExamFolder(\'' + escapeHtml(folderName) + '\')">Hapus Folder</button>'
      html += '</div>'

      html += '<div style="overflow:auto; border:1px solid #e2e8f0; border-radius:10px;">'
      html += '<table style="width:100%; border-collapse:collapse; font-size:13px; min-width:760px;">'
      html += '<thead><tr style="background:#f8fafc;">'
      html += '<th style="padding:8px; border:1px solid #e2e8f0; width:44px;">No</th>'
      html += '<th style="padding:8px; border:1px solid #e2e8f0;">Mapel Perangkatan</th>'
      html += '<th style="padding:8px; border:1px solid #e2e8f0; width:130px;">Tanggal</th>'
      html += '<th style="padding:8px; border:1px solid #e2e8f0; width:130px;">Waktu</th>'
      html += '<th style="padding:8px; border:1px solid #e2e8f0;">Lokasi</th>'
      html += '<th style="padding:8px; border:1px solid #e2e8f0; width:150px;">Aksi</th>'
      html += '</tr></thead><tbody>'

      items.forEach(function (item, idx) {
        var waktu = [item.jam_mulai || '-', item.jam_selesai || '-'].join(' - ')
        html += '<tr>'
        html += '<td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">' + (idx + 1) + '</td>'
        html += '<td style="padding:8px; border:1px solid #e2e8f0;">' + escapeHtml(item.mapel || '-') + '</td>'
        html += '<td style="padding:8px; border:1px solid #e2e8f0;">' + escapeHtml(item.tanggal || '-') + '</td>'
        html += '<td style="padding:8px; border:1px solid #e2e8f0;">' + escapeHtml(waktu) + '</td>'
        html += '<td style="padding:8px; border:1px solid #e2e8f0;">' + escapeHtml(item.lokasi || '-') + '</td>'
        html += '<td style="padding:8px; border:1px solid #e2e8f0; white-space:nowrap;">'
        html += '<button type="button" class="ju-btn" onclick="openJadwalMapelDetail(\'' + escapeHtml(String(item.id || '')) + '\')">Atur Detail</button>'
        html += '</td>'
        html += '</tr>'
      })

      html += '</tbody></table></div>'
      html += '</div>'
    })

    listEl.innerHTML = html
  }

  function ensureClassRows(meta, row) {
    var classRows = Array.isArray(meta.class_rows) ? meta.class_rows : []
    if (!classRows.length) return []
    return classRows.map(function (item) {
      return {
        kelas_id: String(item.kelas_id || ''),
        kelas_nama: String(item.kelas_nama || '-'),
        lokasi: String(item.lokasi || row.lokasi || ''),
        pengawas: String(item.pengawas || '')
      }
    })
  }

  function renderMapelDetailModal(row) {
    var titleEl = getInput('ju-mapel-modal-title')
    var bodyEl = getInput('ju-mapel-modal-body')
    var modalEl = getInput('ju-mapel-modal')
    if (!titleEl || !bodyEl || !modalEl) return

    activeMapelRow = row
    var meta = parseMeta(row)
    var classRows = ensureClassRows(meta, row)

    titleEl.textContent = 'Atur Detail: ' + String(row.mapel || '-')
    var classRowsHtml = classRows.map(function (item, idx) {
      return '<tr>' +
        '<td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">' + (idx + 1) + '</td>' +
        '<td style="padding:8px; border:1px solid #e2e8f0;">' + escapeHtml(item.kelas_nama || '-') + '</td>' +
        '<td style="padding:8px; border:1px solid #e2e8f0;"><input id="ju-kelas-lokasi-' + (idx + 1) + '" class="ju-field" type="text" value="' + escapeHtml(item.lokasi || '') + '" placeholder="Lokasi per kelas"></td>' +
        '<td style="padding:8px; border:1px solid #e2e8f0;"><input id="ju-kelas-pengawas-' + (idx + 1) + '" class="ju-field" type="text" value="' + escapeHtml(item.pengawas || '') + '" placeholder="Nama pengawas"></td>' +
      '</tr>'
    }).join('')

    bodyEl.innerHTML = '' +
      '<div style="display:grid; grid-template-columns:repeat(auto-fit,minmax(180px,1fr)); gap:8px; margin-bottom:10px;">' +
      '  <div><label class="ju-label">Tanggal Umum</label><input id="ju-detail-tanggal" class="ju-field" type="date" value="' + escapeHtml(String(row.tanggal || todayDate())) + '"></div>' +
      '  <div><label class="ju-label">Jam Mulai Umum</label><input id="ju-detail-jam-mulai" class="ju-field" type="time" value="' + escapeHtml(String(row.jam_mulai || '')) + '"></div>' +
      '  <div><label class="ju-label">Jam Selesai Umum</label><input id="ju-detail-jam-selesai" class="ju-field" type="time" value="' + escapeHtml(String(row.jam_selesai || '')) + '"></div>' +
      '</div>' +
      '<div style="font-size:12px; color:#64748b; margin-bottom:8px;">Atur lokasi dan pengawas per kelas.</div>' +
      '<div style="overflow:auto; border:1px solid #e2e8f0; border-radius:10px; margin-bottom:10px;">' +
      '  <table style="width:100%; border-collapse:collapse; font-size:13px; min-width:720px;">' +
      '    <thead><tr style="background:#f8fafc;">' +
      '      <th style="padding:8px; border:1px solid #e2e8f0; width:44px;">No</th>' +
      '      <th style="padding:8px; border:1px solid #e2e8f0;">Kelas</th>' +
      '      <th style="padding:8px; border:1px solid #e2e8f0;">Lokasi</th>' +
      '      <th style="padding:8px; border:1px solid #e2e8f0;">Pengawas</th>' +
      '    </tr></thead>' +
      '    <tbody>' + (classRowsHtml || '<tr><td colspan="4" style="padding:10px; border:1px solid #e2e8f0; text-align:center;">Tidak ada kelas terkait.</td></tr>') + '</tbody>' +
      '  </table>' +
      '</div>' +
      '<div style="display:flex; gap:8px;">' +
      '  <button type="button" class="ju-btn" onclick="saveJadwalMapelDetail()">Simpan Detail</button>' +
      '</div>'

    bodyEl.dataset.classRows = JSON.stringify(classRows)
    modalEl.style.display = 'flex'
  }

  window.openJadwalMapelDetail = function openJadwalMapelDetail(id) {
    var sid = String(id || '')
    var row = (folderRowsCache || []).find(function (item) { return String(item.id || '') === sid })
    if (!row) return
    renderMapelDetailModal(row)
  }

  window.closeJadwalMapelDetailModal = function closeJadwalMapelDetailModal(event) {
    if (event && event.target && event.target.id !== 'ju-mapel-modal') return
    var modalEl = getInput('ju-mapel-modal')
    if (modalEl) modalEl.style.display = 'none'
    activeMapelRow = null
  }

  window.saveJadwalMapelDetail = async function saveJadwalMapelDetail() {
    if (!activeMapelRow || !activeMapelRow.id) return

    var tanggal = String(getInput('ju-detail-tanggal') && getInput('ju-detail-tanggal').value || '').trim()
    var jamMulai = String(getInput('ju-detail-jam-mulai') && getInput('ju-detail-jam-mulai').value || '').trim()
    var jamSelesai = String(getInput('ju-detail-jam-selesai') && getInput('ju-detail-jam-selesai').value || '').trim()
    if (!tanggal) {
      alert('Tanggal umum wajib diisi.')
      return
    }
    if (jamMulai && jamSelesai && jamSelesai < jamMulai) {
      alert('Jam selesai tidak boleh lebih awal dari jam mulai.')
      return
    }

    var bodyEl = getInput('ju-mapel-modal-body')
    var classRows = []
    try {
      classRows = JSON.parse(String(bodyEl && bodyEl.dataset.classRows || '[]'))
      if (!Array.isArray(classRows)) classRows = []
    } catch (_err) {
      classRows = []
    }

    classRows = classRows.map(function (item, idx) {
      var no = idx + 1
      var lokasi = String(getInput('ju-kelas-lokasi-' + no) && getInput('ju-kelas-lokasi-' + no).value || '').trim()
      var pengawas = String(getInput('ju-kelas-pengawas-' + no) && getInput('ju-kelas-pengawas-' + no).value || '').trim()
      return {
        kelas_id: item.kelas_id,
        kelas_nama: item.kelas_nama,
        lokasi: lokasi,
        pengawas: pengawas
      }
    })

    var oldMeta = parseMeta(activeMapelRow)
    var newMeta = {
      folder_name: String(oldMeta.folder_name || activeMapelRow.nama || ''),
      semester_id: oldMeta.semester_id || null,
      semester_nama: oldMeta.semester_nama || '',
      tahun_ajaran_id: oldMeta.tahun_ajaran_id || null,
      tahun_ajaran_nama: oldMeta.tahun_ajaran_nama || '',
      mapel_nama: oldMeta.mapel_nama || activeMapelRow.mapel || '',
      perangkatan: oldMeta.perangkatan || activeMapelRow.kelas || '',
      class_rows: classRows
    }

    var res = await sb
      .from(EXAM_SCHEDULE_TABLE)
      .update({
        tanggal: tanggal,
        jam_mulai: jamMulai || null,
        jam_selesai: jamSelesai || null,
        lokasi: null,
        keterangan: JSON.stringify(newMeta),
        updated_at: new Date().toISOString()
      })
      .eq('id', activeMapelRow.id)

    if (res.error) {
      alert('Gagal menyimpan detail mapel: ' + String(res.error.message || 'Unknown error'))
      return
    }

    alert('Detail jadwal mapel berhasil disimpan.')
    window.closeJadwalMapelDetailModal()
    await window.reloadJadwalUjianFolders()
  }

  window.deleteExamFolder = async function deleteExamFolder(folderName) {
    var name = String(folderName || '').trim()
    if (!name) return
    var ok = true
    if (typeof window.showPopupConfirm === 'function') ok = await window.showPopupConfirm('Hapus seluruh mapel di folder ini?')
    else ok = window.confirm('Hapus seluruh mapel di folder ini?')
    if (!ok) return

    var res = await sb.from(EXAM_SCHEDULE_TABLE).delete().eq('nama', name)
    if (res.error) {
      alert('Gagal menghapus folder ujian: ' + String(res.error.message || 'Unknown error'))
      return
    }

    await window.reloadJadwalUjianFolders()
  }

  window.createExamFolderFromJenis = async function createExamFolderFromJenis() {
    var jenis = String(getInput('ju-jenis-folder') && getInput('ju-jenis-folder').value || '').trim().toUpperCase()
    if (!jenis) {
      alert('Jenis ujian wajib dipilih.')
      return
    }

    var distribusi
    try {
      distribusi = await loadDistribusiMapelAktif()
    } catch (error) {
      alert('Gagal membaca data mapel/kelas: ' + String(error && error.message || 'Unknown error'))
      return
    }

    var semesterNama = String(distribusi.active && distribusi.active.semester && distribusi.active.semester.nama || '-').trim()
    var tahunNama = String(distribusi.active && distribusi.active.tahun && distribusi.active.tahun.nama || '-').trim()
    var folderName = (jenis + ' Semester ' + semesterNama + ' Tahun Ajaran ' + tahunNama).replace(/\s+/g, ' ').trim()

    var groups = groupMapelPerangkatan(distribusi.rows, distribusi.kelasMap, distribusi.mapelMap)
    if (!groups.length) {
      alert('Belum ada distribusi mapel aktif untuk dibuatkan folder ujian.')
      return
    }

    var existingRes = await sb
      .from(EXAM_SCHEDULE_TABLE)
      .select('id, nama, mapel, kelas')
      .eq('nama', folderName)

    if (existingRes.error) {
      if (isMissingTableError(existingRes.error)) {
        alert(missingTableMessage())
      } else {
        alert('Gagal cek folder ujian: ' + String(existingRes.error.message || 'Unknown error'))
      }
      return
    }

    var existingKey = new Set((existingRes.data || []).map(function (item) {
      return String(item.mapel || '').toLowerCase() + '|' + String(item.kelas || '').toLowerCase()
    }))

    var now = new Date().toISOString()
    var payload = []
    groups.forEach(function (group) {
      var key = String(group.mapel_label || '').toLowerCase() + '|' + String(group.perangkatan || '').toLowerCase()
      if (existingKey.has(key)) return
      payload.push({
        jenis: jenis,
        nama: folderName,
        kelas: group.perangkatan,
        mapel: group.mapel_label,
        tanggal: todayDate(),
        jam_mulai: null,
        jam_selesai: null,
        lokasi: null,
        keterangan: JSON.stringify({
          folder_name: folderName,
          semester_id: distribusi.active && distribusi.active.semester ? distribusi.active.semester.id : null,
          semester_nama: semesterNama,
          tahun_ajaran_id: distribusi.active && distribusi.active.tahun ? distribusi.active.tahun.id : null,
          tahun_ajaran_nama: tahunNama,
          mapel_nama: group.mapel_nama,
          perangkatan: group.perangkatan,
          class_rows: group.class_rows
        }),
        updated_at: now
      })
    })

    if (!payload.length) {
      alert('Folder sudah ada. Tidak ada mapel baru yang ditambahkan.')
      await window.reloadJadwalUjianFolders()
      return
    }

    var insertRes = await sb.from(EXAM_SCHEDULE_TABLE).insert(payload)
    if (insertRes.error) {
      if (isMissingTableError(insertRes.error)) {
        alert(missingTableMessage())
      } else {
        alert('Gagal membuat folder ujian: ' + String(insertRes.error.message || 'Unknown error'))
      }
      return
    }

    alert('Folder ujian berhasil dibuat: ' + folderName)
    await window.reloadJadwalUjianFolders()
  }

  window.reloadJadwalUjianFolders = async function reloadJadwalUjianFolders() {
    var rows = await readRows()
    renderFolders(rows)
  }

  window.initJadwalUjianPage = async function initJadwalUjianPage() {
    await window.reloadJadwalUjianFolders()
  }
})()
