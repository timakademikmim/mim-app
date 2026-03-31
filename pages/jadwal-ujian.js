(function () {
  var EXAM_SCHEDULE_TABLE = 'jadwal_ujian'
  var EXAM_QUESTION_TABLE = 'soal_ujian'
  var folderRowsCache = []
  var activeMapelRow = null
  var EXAM_PARTICIPANT_TABLE = 'peserta_ujian'
  var participantStatsByJadwal = new Map()
  var activeParticipantModalState = {
    row: null,
    participants: []
  }
  var createFolderDraftModalResolver = null

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

  function missingParticipantTableMessage() {
    return "Tabel '" + EXAM_PARTICIPANT_TABLE + "' belum ada.\n\nJalankan SQL:\ncreate table if not exists public." + EXAM_PARTICIPANT_TABLE + " (\n  id uuid primary key default gen_random_uuid(),\n  jadwal_id uuid not null,\n  santri_id text not null,\n  nama_santri text not null,\n  kelas_id text not null,\n  kelas_nama text not null,\n  nomor_urut integer not null,\n  nomor_urut_label text not null,\n  exam_code text not null,\n  sheet_code text not null,\n  released_at timestamptz null default now(),\n  created_at timestamptz not null default now(),\n  updated_at timestamptz not null default now(),\n  unique (jadwal_id, santri_id),\n  unique (jadwal_id, kelas_id, nomor_urut)\n);"
  }

  function missingExamQuestionTableMessage() {
    return "Tabel '" + EXAM_QUESTION_TABLE + "' belum ada.\n\nJalankan SQL:\ncreate table if not exists public." + EXAM_QUESTION_TABLE + " (\n  id uuid primary key default gen_random_uuid(),\n  jadwal_id uuid not null,\n  guru_id text not null,\n  guru_nama text,\n  kelas_target text,\n  bentuk_soal text,\n  jumlah_nomor integer,\n  instruksi text,\n  questions_json text,\n  status text not null default 'draft',\n  created_at timestamptz not null default now(),\n  updated_at timestamptz not null default now()\n);"
  }

  function isExamQuestionColumnMissingError(error) {
    var msg = String(error && error.message || '').toLowerCase()
    var code = String(error && error.code || '').toUpperCase()
    return code === '42703' ||
      msg.includes('column') ||
      msg.includes('kelas_target')
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

  function getExamJenisCode(jenis) {
    var raw = String(jenis || '').trim().toUpperCase()
    if (!raw) return 'UJ'
    if (raw.includes('UTS') || raw.includes('PTS')) return 'UT'
    if (raw.includes('UAS') || raw.includes('PAS')) return 'UA'
    if (raw === 'UN' || raw.includes('UJIAN NASIONAL')) return 'UN'
    if (raw.includes('TRY')) return 'TO'
    if (raw.includes('PRAK')) return 'PR'
    if (raw.includes('TUGAS')) return 'TG'
    return raw.replace(/[^A-Z0-9]/g, '').slice(0, 2) || 'UJ'
  }

  function getExamYearCode(tahunNama) {
    var raw = String(tahunNama || '').trim()
    var matches = raw.match(/\d{4}/g)
    if (matches && matches[0]) return String(matches[0]).slice(-2)
    return String(new Date().getFullYear()).slice(-2)
  }

  function getExamSemesterCode(semesterNama) {
    var raw = String(semesterNama || '').trim().toLowerCase()
    if (!raw) return '1'
    if (raw.includes('genap')) return '2'
    if (raw.includes('ganjil')) return '1'
    var match = raw.match(/\b([12])\b/)
    if (match && match[1]) return String(match[1])
    return '1'
  }

  function getExamClassCode(kelasNama) {
    return String(kelasNama || '')
      .trim()
      .toUpperCase()
      .replace(/\s+/g, '')
      .replace(/[^A-Z0-9]/g, '') || 'KLS'
  }

  function getExamMapelCode(mapelNama) {
    var raw = String(mapelNama || '').trim()
    var lower = raw.toLowerCase()
    if (!lower) return 'MP'
    if (lower.includes('arab')) return 'AR'
    if (lower.includes('indonesia')) return 'BI'
    if (lower.includes('inggris')) return 'EN'
    if (lower.includes('matematika')) return 'MT'
    if (lower.includes('fiq') || lower.includes('fik')) return 'FQ'
    if (lower.includes('aqidah') || lower.includes('akidah')) return 'AQ'
    if (lower.includes('akhlak')) return 'AK'
    if (lower.includes('hadis') || lower.includes('hadits')) return 'HD'
    if (lower.includes('qur') || lower.includes('quran')) return 'QR'
    if (lower.includes('tajwid')) return 'TJ'
    if (lower.includes('nahwu')) return 'NH'
    if (lower.includes('shorof') || lower.includes('sharaf') || lower.includes('sorof')) return 'SR'
    if (lower.includes('siroh') || lower.includes('sirah')) return 'SH'
    var words = raw
      .replace(/\b(smp|sma|umum)\b/gi, ' ')
      .split(/\s+/)
      .map(function (item) { return String(item || '').replace(/[^A-Za-z0-9]/g, '') })
      .filter(Boolean)
    if (!words.length) return 'MP'
    if (words.length === 1) return String(words[0]).toUpperCase().slice(0, 2)
    return (String(words[0] || '').charAt(0) + String(words[1] || '').charAt(0)).toUpperCase()
  }

  function buildExamCode(row, meta, classRow) {
    var jenisCode = getExamJenisCode(row && row.jenis)
    var tahunCode = getExamYearCode(meta && meta.tahun_ajaran_nama)
    var semesterCode = getExamSemesterCode(meta && meta.semester_nama)
    var kelasCode = getExamClassCode(classRow && classRow.kelas_nama)
    var mapelCode = getExamMapelCode((meta && meta.mapel_nama) || (row && row.mapel) || '')
    return [jenisCode, tahunCode, semesterCode, kelasCode, mapelCode].join('-')
  }

  function formatDateLabel(value) {
    var raw = String(value || '').trim()
    if (!raw) return '-'
    var date = /^\d{4}-\d{2}-\d{2}$/.test(raw) ? new Date(raw + 'T00:00:00') : new Date(raw)
    if (Number.isNaN(date.getTime())) return raw
    return date.toLocaleDateString('id-ID', { day: '2-digit', month: 'long', year: 'numeric' })
  }

  function formatTimeRangeLabel(row) {
    var start = String(row && row.jam_mulai || '').trim()
    var end = String(row && row.jam_selesai || '').trim()
    if (!start && !end) return '-'
    return [start || '-', end || '-'].join(' - ')
  }

  function buildSheetNumberLabel(value) {
    return String(Number(value || 0)).padStart(2, '0')
  }

  function parseExamQuestionPayload(value) {
    if (Array.isArray(value)) return { questions: value, sections: [] }
    var raw = String(value || '').trim()
    if (!raw) return { questions: [], sections: [] }
    try {
      var parsed = JSON.parse(raw)
      if (Array.isArray(parsed)) return { questions: parsed, sections: [] }
      if (parsed && typeof parsed === 'object') {
        return {
          questions: Array.isArray(parsed.questions) ? parsed.questions : [],
          sections: Array.isArray(parsed.sections) ? parsed.sections : []
        }
      }
    } catch (_err) {}
    return { questions: [], sections: [] }
  }

  function normalizeExamQuestionType(value, fallbackType) {
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

  function normalizeClassTarget(value) {
    return String(value || '').trim().replace(/\s+/g, ' ').toUpperCase()
  }

  function getInput(id) {
    return document.getElementById(id)
  }

  function buildFolderNameByParts(jenis, semesterNama, tahunNama) {
    return (String(jenis || '').trim() + ' Semester ' + String(semesterNama || '').trim() + ' Tahun Ajaran ' + String(tahunNama || '').trim())
      .replace(/\s+/g, ' ')
      .trim()
  }

  function getMapelGroupKey(group) {
    return String(group && group.mapel_label || '').toLowerCase() + '|' + String(group && group.perangkatan || '').toLowerCase()
  }

  function getSelectedSelectLabel(selectEl) {
    if (!selectEl) return ''
    var idx = Number(selectEl.selectedIndex || 0)
    var option = selectEl.options && selectEl.options[idx] ? selectEl.options[idx] : null
    return String(option && option.text || '').trim()
  }

  function normalizeReferenceRows(rows, fallbackItem) {
    var list = Array.isArray(rows) ? rows.slice() : []
    var fallbackId = String(fallbackItem && fallbackItem.id || '').trim()
    var fallbackNama = String(fallbackItem && fallbackItem.nama || '').trim()
    if (fallbackId && fallbackNama && !list.some(function (item) { return String(item && item.id || '') === fallbackId })) {
      list.unshift({ id: fallbackId, nama: fallbackNama, aktif: true })
    }
    var seen = new Set()
    return list.filter(function (item) {
      var id = String(item && item.id || '').trim()
      var nama = String(item && item.nama || '').trim()
      if (!id || !nama || seen.has(id)) return false
      seen.add(id)
      return true
    })
  }

  function populateReferenceSelect(selectEl, rows, selectedId, selectedNama) {
    if (!selectEl) return
    var list = Array.isArray(rows) ? rows : []
    if (!list.length) {
      selectEl.innerHTML = '<option value="">-</option>'
      return
    }
    selectEl.innerHTML = list.map(function (item) {
      return '<option value="' + escapeHtml(String(item.id || '')) + '">' + escapeHtml(String(item.nama || '-')) + '</option>'
    }).join('')
    var sid = String(selectedId || '').trim()
    var sname = String(selectedNama || '').trim().toLowerCase()
    if (sid && list.some(function (item) { return String(item.id || '') === sid })) {
      selectEl.value = sid
      return
    }
    var byName = list.find(function (item) { return String(item && item.nama || '').trim().toLowerCase() === sname })
    if (byName) {
      selectEl.value = String(byName.id || '')
      return
    }
    selectEl.selectedIndex = 0
  }

  async function loadCreateFolderReferenceOptions(active) {
    var semesterRows = []
    var tahunRows = []

    var semesterRes = await sb
      .from('semester')
      .select('id, nama, aktif')
      .order('aktif', { ascending: false })
      .order('nama', { ascending: false })
    if (semesterRes.error) {
      if (!(active && active.semester && active.semester.id)) {
        throw new Error('Gagal memuat data semester: ' + String(semesterRes.error.message || 'Unknown error'))
      }
    } else {
      semesterRows = semesterRes.data || []
    }

    var tahunRes = await sb
      .from('tahun_ajaran')
      .select('id, nama, aktif')
      .order('aktif', { ascending: false })
      .order('nama', { ascending: false })
    if (tahunRes.error) {
      if (!(active && active.tahun && active.tahun.id)) {
        throw new Error('Gagal memuat data tahun ajaran: ' + String(tahunRes.error.message || 'Unknown error'))
      }
    } else {
      tahunRows = tahunRes.data || []
    }

    return {
      semesters: normalizeReferenceRows(semesterRows, active && active.semester),
      years: normalizeReferenceRows(tahunRows, active && active.tahun)
    }
  }

  function renderCreateFolderMapelSelector(groups, selectedKeySet) {
    var bodyEl = getInput('ju-create-folder-mapel-select')
    if (!bodyEl) return
    var list = Array.isArray(groups) ? groups : []
    if (!list.length) {
      bodyEl.innerHTML = '<div style="font-size:12px; color:#64748b;">Belum ada mapel yang akan dibuat.</div>'
      return
    }
    var selected = selectedKeySet instanceof Set ? selectedKeySet : new Set()
    bodyEl.innerHTML = list.map(function (group, idx) {
      var key = getMapelGroupKey(group)
      var checked = selected.has(key)
      return '<label style="display:flex; align-items:flex-start; gap:8px; padding:7px 4px; border-bottom:1px dashed #e2e8f0; cursor:pointer;">' +
        '<input type="checkbox" data-mapel-key="' + escapeHtml(key) + '"' + (checked ? ' checked' : '') + ' onchange="updateCreateFolderMapelSummary()">' +
        '<span style="display:block; flex:1; font-size:13px; color:#0f172a;">' +
          '<span style="display:inline-block; min-width:26px; color:#64748b;">' + (idx + 1) + '.</span> ' + escapeHtml(String(group && group.mapel_label || '-')) +
          '<span style="display:block; font-size:11px; color:#64748b; margin-left:26px;">Target kelas: ' + escapeHtml(String((group && group.class_rows && group.class_rows.length) || 0)) + '</span>' +
        '</span>' +
      '</label>'
    }).join('')
  }

  function getSelectedCreateFolderMapelKeys() {
    var bodyEl = getInput('ju-create-folder-mapel-select')
    if (!bodyEl) return []
    return Array.from(bodyEl.querySelectorAll('input[type="checkbox"][data-mapel-key]:checked'))
      .map(function (inputEl) { return String(inputEl.getAttribute('data-mapel-key') || '').trim() })
      .filter(Boolean)
  }

  async function openCreateExamFolderDraftModal(draft, groups, references) {
    var modalEl = getInput('ju-create-folder-modal')
    var jenisEl = getInput('ju-create-folder-jenis')
    var semesterEl = getInput('ju-create-folder-semester')
    var tahunEl = getInput('ju-create-folder-tahun')
    var nameEl = getInput('ju-create-folder-name')
    if (!modalEl || !jenisEl || !semesterEl || !tahunEl || !nameEl) {
      var fallbackName = String(draft && draft.folderName || '').trim()
      var fallbackOk = true
      if (typeof window.showPopupConfirm === 'function') fallbackOk = await window.showPopupConfirm('Buat folder ujian: ' + fallbackName + ' ?')
      else fallbackOk = window.confirm('Buat folder ujian: ' + fallbackName + ' ?')
      return fallbackOk ? Object.assign({}, draft, {
        selectedMapelKeys: Array.isArray(groups) ? groups.map(getMapelGroupKey) : []
      }) : null
    }

    jenisEl.value = String(draft && draft.jenis || '').trim().toUpperCase()
    populateReferenceSelect(semesterEl, references && references.semesters, draft && draft.semesterId, draft && draft.semesterNama)
    populateReferenceSelect(tahunEl, references && references.years, draft && draft.tahunId, draft && draft.tahunNama)
    nameEl.value = String(draft && draft.folderName || '').trim()
    var defaultSelectedKeys = Array.isArray(draft && draft.selectedMapelKeys) && draft.selectedMapelKeys.length
      ? draft.selectedMapelKeys
      : (Array.isArray(groups) ? groups.map(getMapelGroupKey) : [])
    renderCreateFolderMapelSelector(groups, new Set(defaultSelectedKeys))
    window.updateCreateFolderMapelSummary()

    var syncName = function () {
      var auto = buildFolderNameByParts(jenisEl.value, getSelectedSelectLabel(semesterEl), getSelectedSelectLabel(tahunEl))
      var raw = String(nameEl.value || '').trim()
      if (!raw || String(nameEl.dataset.manualName || '') !== '1') nameEl.value = auto
    }
    nameEl.dataset.manualName = '0'
    nameEl.oninput = function () {
      if (String(nameEl.value || '').trim()) nameEl.dataset.manualName = '1'
      else nameEl.dataset.manualName = '0'
    }
    jenisEl.oninput = syncName
    semesterEl.onchange = syncName
    tahunEl.onchange = syncName
    syncName()

    modalEl.style.display = 'flex'
    setTimeout(function () {
      try { nameEl.focus() } catch (_err) {}
    }, 0)

    return await new Promise(function (resolve) {
      createFolderDraftModalResolver = resolve
    })
  }

  function resolveCreateExamFolderDraftModal(payload) {
    var modalEl = getInput('ju-create-folder-modal')
    if (modalEl) modalEl.style.display = 'none'
    var resolve = createFolderDraftModalResolver
    createFolderDraftModalResolver = null
    if (typeof resolve === 'function') resolve(payload || null)
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

    var allRows = distribusiRes.data || []
    var hasActiveSemester = Boolean(active.semester && active.semester.id)
    var list = allRows
    if (hasActiveSemester) {
      var activeSemesterId = String(active.semester.id)
      var listByActiveSemester = allRows.filter(function (item) {
        return String(item.semester_id || '') === activeSemesterId
      })
      var listLegacyNoSemester = allRows.filter(function (item) {
        var sid = String(item && item.semester_id || '').trim().toLowerCase()
        return !sid || sid === 'null' || sid === 'undefined'
      })
      // Prioritas semester aktif + tetap dukung data distribusi lama yang belum punya semester_id.
      if (listByActiveSemester.length || listLegacyNoSemester.length) {
        var seen = new Set()
        list = listByActiveSemester.concat(listLegacyNoSemester).filter(function (item) {
          var key = String(item && item.id || '') || [String(item && item.kelas_id || ''), String(item && item.mapel_id || ''), String(item && item.semester_id || '')].join('|')
          if (!key || seen.has(key)) return false
          seen.add(key)
          return true
        })
      }
    }

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
    await loadParticipantStats(folderRowsCache.map(function (item) { return String(item && item.id || '') }))
    return folderRowsCache
  }

  async function loadParticipantStats(jadwalIds) {
    participantStatsByJadwal = new Map()
    var ids = Array.isArray(jadwalIds) ? jadwalIds.filter(Boolean) : []
    if (!ids.length) return participantStatsByJadwal

    var res = await sb
      .from(EXAM_PARTICIPANT_TABLE)
      .select('jadwal_id, kelas_id')
      .in('jadwal_id', ids)

    if (res.error) {
      if (String(res.error.message || '').toLowerCase().includes(EXAM_PARTICIPANT_TABLE)) return participantStatsByJadwal
      return participantStatsByJadwal
    }

    ;(res.data || []).forEach(function (item) {
      var key = String(item && item.jadwal_id || '')
      if (!key) return
      if (!participantStatsByJadwal.has(key)) participantStatsByJadwal.set(key, { count: 0, kelasSet: new Set() })
      var target = participantStatsByJadwal.get(key)
      target.count += 1
      if (String(item && item.kelas_id || '').trim()) target.kelasSet.add(String(item.kelas_id))
    })
    return participantStatsByJadwal
  }

  function getParticipantStatText(jadwalId) {
    var stat = participantStatsByJadwal.get(String(jadwalId || ''))
    if (!stat || !Number(stat.count)) return 'Belum dirilis'
    return String(stat.count) + ' santri / ' + String(stat.kelasSet.size || 0) + ' kelas'
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
      html += '<th style="padding:8px; border:1px solid #e2e8f0; width:140px;">Peserta</th>'
      html += '<th style="padding:8px; border:1px solid #e2e8f0; width:250px;">Aksi</th>'
      html += '</tr></thead><tbody>'

      items.forEach(function (item, idx) {
        var waktu = [item.jam_mulai || '-', item.jam_selesai || '-'].join(' - ')
        html += '<tr>'
        html += '<td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">' + (idx + 1) + '</td>'
        html += '<td style="padding:8px; border:1px solid #e2e8f0;">' + escapeHtml(item.mapel || '-') + '</td>'
        html += '<td style="padding:8px; border:1px solid #e2e8f0;">' + escapeHtml(item.tanggal || '-') + '</td>'
        html += '<td style="padding:8px; border:1px solid #e2e8f0;">' + escapeHtml(waktu) + '</td>'
        html += '<td style="padding:8px; border:1px solid #e2e8f0;">' + escapeHtml(item.lokasi || '-') + '</td>'
        html += '<td style="padding:8px; border:1px solid #e2e8f0;">' + escapeHtml(getParticipantStatText(item.id)) + '</td>'
        html += '<td style="padding:8px; border:1px solid #e2e8f0; white-space:nowrap;">'
        html += '<button type="button" class="ju-btn" onclick="openJadwalMapelDetail(\'' + escapeHtml(String(item.id || '')) + '\')">Atur Detail</button>'
        html += '<button type="button" class="ju-btn secondary" style="margin-left:6px;" onclick="releaseExamParticipants(\'' + escapeHtml(String(item.id || '')) + '\')">Rilis Peserta</button>'
        html += '<button type="button" class="ju-btn secondary" style="margin-left:6px;" onclick="openExamParticipantsModal(\'' + escapeHtml(String(item.id || '')) + '\')">Peserta</button>'
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

  function ensureParticipantClassRows(meta, row) {
    return ensureClassRows(meta || {}, row || {}).filter(function (item) {
      return String(item && item.kelas_id || '').trim() && String(item && item.kelas_nama || '').trim()
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

  async function loadExamParticipantsByJadwal(jadwalId) {
    var res = await sb
      .from(EXAM_PARTICIPANT_TABLE)
      .select('id, jadwal_id, santri_id, nama_santri, kelas_id, kelas_nama, nomor_urut, nomor_urut_label, exam_code, sheet_code, released_at, created_at, updated_at')
      .eq('jadwal_id', String(jadwalId || ''))
      .order('kelas_nama', { ascending: true })
      .order('nomor_urut', { ascending: true })

    if (res.error) {
      if (String(res.error.message || '').toLowerCase().includes(EXAM_PARTICIPANT_TABLE)) {
        alert(missingParticipantTableMessage())
        return null
      }
      alert('Gagal memuat peserta ujian: ' + String(res.error.message || 'Unknown error'))
      return null
    }
    return res.data || []
  }

  async function loadExamQuestionRowsByJadwal(jadwalId) {
    var sid = String(jadwalId || '')
    var supportsKelasTarget = true
    var res = await sb
      .from(EXAM_QUESTION_TABLE)
      .select('id, jadwal_id, kelas_target, guru_id, guru_nama, bentuk_soal, jumlah_nomor, instruksi, questions_json, status, updated_at, created_at')
      .eq('jadwal_id', sid)
      .order('updated_at', { ascending: false })
      .order('created_at', { ascending: false })

    if (res.error && isExamQuestionColumnMissingError(res.error)) {
      supportsKelasTarget = false
      res = await sb
        .from(EXAM_QUESTION_TABLE)
        .select('id, jadwal_id, guru_id, guru_nama, bentuk_soal, jumlah_nomor, instruksi, questions_json, status, updated_at, created_at')
        .eq('jadwal_id', sid)
        .order('updated_at', { ascending: false })
        .order('created_at', { ascending: false })
    }

    if (res.error) {
      if (isMissingTableError(res.error) || String(res.error.message || '').toLowerCase().includes(EXAM_QUESTION_TABLE)) {
        alert(missingExamQuestionTableMessage())
        return null
      }
      alert('Gagal memuat data soal ujian: ' + String(res.error.message || 'Unknown error'))
      return null
    }

    return {
      rows: (res.data || []).map(function (item) {
        if (supportsKelasTarget) return item
        var cloned = Object.assign({}, item)
        cloned.kelas_target = ''
        return cloned
      }),
      supportsKelasTarget: supportsKelasTarget
    }
  }

  function getExamQuestionStatusRank(value) {
    var raw = String(value || '').trim().toLowerCase()
    if (raw === 'submitted') return 3
    if (raw === 'draft') return 2
    return 1
  }

  function countMultipleChoiceQuestions(soalRow) {
    var payload = parseExamQuestionPayload(soalRow && soalRow.questions_json)
    var sectionTotal = 0
    ;(Array.isArray(payload.sections) ? payload.sections : []).forEach(function (item) {
      var qType = normalizeExamQuestionType(item && item.type, soalRow && soalRow.bentuk_soal)
      if (qType !== 'pilihan-ganda') return
      var start = Number(item && item.start || 0)
      var end = Number(item && item.end || 0)
      var count = 0
      if (Number.isFinite(start) && Number.isFinite(end) && end >= start) count = Math.max(0, end - start + 1)
      if (!count) {
        var explicitCount = Number(item && item.count || 0)
        if (Number.isFinite(explicitCount) && explicitCount > 0) count = Math.round(explicitCount)
      }
      sectionTotal += count
    })
    if (sectionTotal > 0) return sectionTotal

    var questionTotal = (payload.questions || []).filter(function (item) {
      return normalizeExamQuestionType(item && item.type, soalRow && soalRow.bentuk_soal) === 'pilihan-ganda'
    }).length
    if (questionTotal > 0) return questionTotal

    var bentuk = normalizeExamQuestionType('', soalRow && soalRow.bentuk_soal)
    var jumlah = Number(soalRow && soalRow.jumlah_nomor || 0)
    if (bentuk === 'pilihan-ganda' && Number.isFinite(jumlah) && jumlah > 0) return Math.round(jumlah)
    return 0
  }

  function extractMultipleChoiceAnswerKey(soalRow) {
    var payload = parseExamQuestionPayload(soalRow && soalRow.questions_json)
    return (payload.questions || [])
      .filter(function (item) {
        return normalizeExamQuestionType(item && item.type, soalRow && soalRow.bentuk_soal) === 'pilihan-ganda'
      })
      .map(function (item, idx) {
        var no = Number(item && item.no || (idx + 1))
        var answer = String(item && item.answer || '').trim().toUpperCase()
        if (!Number.isFinite(no) || no <= 0 || !/^[ABCD]$/.test(answer)) return null
        return { no: no, answer: answer }
      })
      .filter(Boolean)
      .sort(function (a, b) { return a.no - b.no })
  }

  function pickQuestionRowForClass(questionRows, kelasNama) {
    var rows = Array.isArray(questionRows) ? questionRows.slice() : []
    if (!rows.length) return null
    var target = normalizeClassTarget(kelasNama)
    var exact = rows.filter(function (item) {
      return normalizeClassTarget(item && item.kelas_target) === target
    })
    var generic = rows.filter(function (item) {
      return !normalizeClassTarget(item && item.kelas_target)
    })
    var pool = exact.length ? exact : (generic.length ? generic : rows)
    pool.sort(function (a, b) {
      return getExamQuestionStatusRank(b && b.status) - getExamQuestionStatusRank(a && a.status)
    })
    return pool[0] || null
  }

  function buildDigitBubbleRows() {
    var rows = ''
    for (var digit = 0; digit <= 9; digit += 1) {
      rows += '<tr><td class="digit-no-cell">' + digit + '</td><td class="digit-bubble-cell"><span class="bubble small"></span></td><td class="digit-bubble-cell"><span class="bubble small"></span></td></tr>'
    }
    return rows
  }

  function buildDigitGuideRail() {
    var html = ''
    for (var digit = 0; digit <= 9; digit += 1) {
      html += '<span class="row-guide digit" data-digit-guide="' + digit + '"></span>'
    }
    return html
  }

  function buildDigitColumnGuideRail() {
    return '' +
      '<span class="column-guide digit" data-digit-col-guide="tens"></span>' +
      '<span class="column-guide digit" data-digit-col-guide="ones"></span>'
  }

  function buildAnswerSheetFiducialSvg() {
    return '' +
      '<svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg" aria-hidden="true" focusable="false">' +
      '<rect x="2" y="2" width="96" height="96" fill="#111"/>' +
      '<rect x="18" y="18" width="64" height="64" fill="#fff"/>' +
      '<rect x="32" y="32" width="36" height="36" fill="#111"/>' +
      '</svg>'
  }

  function buildAnswerBubbleGrid(totalQuestions) {
    var safeTotal = Math.max(1, Math.min(200, Number(totalQuestions || 0) || 1))
    var columnCount = safeTotal <= 30 ? 1 : (safeTotal <= 80 ? 2 : 3)
    var rowsPerColumn = Math.ceil(safeTotal / columnCount)
    var columns = []
    for (var col = 0; col < columnCount; col += 1) {
      var start = (col * rowsPerColumn) + 1
      var end = Math.min(safeTotal, start + rowsPerColumn - 1)
      if (start > safeTotal) break
      var rowsHtml = ''
      for (var no = start; no <= end; no += 1) {
        rowsHtml += '<tr><td style="padding:5px 4px; border:1px solid #111; text-align:center; width:34px;">' + no + '</td><td style="padding:5px 4px; border:1px solid #111; text-align:center; width:36px;"><span class="bubble" data-answer-group="' + col + '" data-answer-choice="A"></span></td><td style="padding:5px 4px; border:1px solid #111; text-align:center; width:36px;"><span class="bubble" data-answer-group="' + col + '" data-answer-choice="B"></span></td><td style="padding:5px 4px; border:1px solid #111; text-align:center; width:36px;"><span class="bubble" data-answer-group="' + col + '" data-answer-choice="C"></span></td><td style="padding:5px 4px; border:1px solid #111; text-align:center; width:36px;"><span class="bubble" data-answer-group="' + col + '" data-answer-choice="D"></span></td><td class="guide-side-cell"><span class="row-guide answer" data-answer-guide="' + no + '"></span></td></tr>'
      }
      columns.push('' +
        '<div class="answer-col">' +
          '<div class="answer-col-wrap">' +
            '<table class="bubble-table"><thead><tr><th style="width:34px;">No</th><th>A</th><th>B</th><th>C</th><th>D</th><th class="guide-side-head"></th></tr></thead><tbody>' + rowsHtml + '<tr class="column-guide-bottom-row"><td></td><td><span class="column-guide answer" data-answer-col-guide="' + col + ':A"></span></td><td><span class="column-guide answer" data-answer-col-guide="' + col + ':B"></span></td><td><span class="column-guide answer" data-answer-col-guide="' + col + ':C"></span></td><td><span class="column-guide answer" data-answer-col-guide="' + col + ':D"></span></td><td></td></tr></tbody></table>' +
          '</div>' +
        '</div>')
    }
    return columns.join('')
  }

  function buildExamAnswerSheetPrintHtml(row, participants, questionRows) {
    var meta = parseMeta(row)
    var grouped = groupParticipantsByClass(participants)
    var releaseMap = new Map()
    var releaseClasses = Array.isArray(meta && meta.participant_release && meta.participant_release.classes) ? meta.participant_release.classes : []
    releaseClasses.forEach(function (item) { releaseMap.set(String(item && item.kelas_id || ''), item) })

    var sectionsHtml = ''
    grouped.forEach(function (items, kelasId) {
      var first = items[0] || {}
      var releaseInfo = releaseMap.get(kelasId) || {}
      var soalRow = pickQuestionRowForClass(questionRows, first.kelas_nama || '')
      var pgCount = countMultipleChoiceQuestions(soalRow)
      var payload = parseExamQuestionPayload(soalRow && soalRow.questions_json)
      var hasNonPg = (payload.questions || []).some(function (item) {
        return normalizeExamQuestionType(item && item.type, soalRow && soalRow.bentuk_soal) !== 'pilihan-ganda'
      })
      if (!hasNonPg && Array.isArray(payload.sections) && payload.sections.length) {
        hasNonPg = payload.sections.some(function (item) {
          return normalizeExamQuestionType(item && item.type, soalRow && soalRow.bentuk_soal) !== 'pilihan-ganda'
        })
      }
      var otherSectionsNote = hasNonPg
        ? '<div class="subnote">Lembar ini hanya dipakai untuk bagian pilihan ganda. Bagian soal lain tetap dikerjakan di lembar terpisah.</div>'
        : ''

      sectionsHtml += '<section class="sheet">' +
        '<span class="corner tl">' + buildAnswerSheetFiducialSvg() + '</span><span class="corner tr">' + buildAnswerSheetFiducialSvg() + '</span><span class="corner bl">' + buildAnswerSheetFiducialSvg() + '</span><span class="corner br">' + buildAnswerSheetFiducialSvg() + '</span>' +
        '<div class="sheet-frame">' +
        '<div class="sheet-header"><h2>Lembar Jawaban Pilihan Ganda</h2><div class="meta-grid">' +
        '<div><b>Nama Ujian:</b> ' + escapeHtml(row && row.nama || '-') + '</div>' +
        '<div><b>Jenis:</b> ' + escapeHtml(row && row.jenis || '-') + '</div>' +
        '<div><b>Mapel:</b> ' + escapeHtml(row && row.mapel || '-') + '</div>' +
        '<div><b>Kelas:</b> ' + escapeHtml(first.kelas_nama || '-') + '</div>' +
        '<div><b>Tanggal:</b> ' + escapeHtml(formatDateLabel(row && row.tanggal)) + '</div>' +
        '<div><b>Waktu:</b> ' + escapeHtml(formatTimeRangeLabel(row)) + '</div>' +
        '<div><b>Lokasi:</b> ' + escapeHtml(releaseInfo.lokasi || '-') + '</div>' +
        '<div><b>Pengawas:</b> ' + escapeHtml(releaseInfo.pengawas || '-') + '</div>' +
        '<div style="grid-column:1 / -1;"><b>Kode Ujian:</b> <span class="mono">' + escapeHtml(first.exam_code || '-') + '</span> | <b>Jumlah Peserta:</b> ' + items.length + ' santri | <b>Cetak:</b> ' + items.length + ' lembar</div>' +
        '</div></div>' +
        '<div class="instruction-box"><div><b>Petunjuk:</b> Isi nomor urut ujian 2 digit, lalu arsir nomor urut sesuai daftar peserta ujian.</div><div><b>Fokus Scan:</b> Nomor urut</div></div>' +
        otherSectionsNote +
        '<div class="identity-grid">' +
        '<div class="identity-card"><div class="identity-title">Identitas Siswa</div><div class="identity-line"><span>Nama:</span><span class="line"></span></div><div class="identity-line"><span>Nomor Urut:</span><span class="line short"></span></div><div class="identity-code"><b>Kode Ujian:</b> <span class="mono">' + escapeHtml(first.exam_code || '-') + '</span></div><div class="identity-foot">Siswa hanya mengisi nomor urut 2 digit sesuai daftar peserta ujian.</div></div>' +
        '<div class="digit-stage"><div class="digit-card"><div class="identity-title">Arsir Nomor Urut</div><div class="digit-region-wrap"><div class="digit-table-wrap"><table class="digit-table"><thead><tr><th>Digit</th><th>Puluhan</th><th>Satuan</th></tr></thead><tbody>' + buildDigitBubbleRows() + '</tbody></table></div></div></div><div class="digit-guide-rail">' + buildDigitGuideRail() + '</div><div class="digit-column-rail">' + buildDigitColumnGuideRail() + '</div></div>' +
        '</div>' +
        '</div>' +
        '</section>'
    })

    return '<!doctype html><html lang="id"><head><meta charset="utf-8"><title>Lembar Jawaban PG</title><style>' +
      '@page{size:A4 portrait;margin:10mm;}' +
      'body{font-family:\"Times New Roman\",serif;margin:0;color:#111;font-size:12px;-webkit-print-color-adjust:exact;print-color-adjust:exact;}' +
      '.sheet{position:relative;page-break-after:always;min-height:calc(297mm - 20mm);padding:0;box-sizing:border-box;}' +
      '.sheet:last-child{page-break-after:auto;}' +
      '.sheet-frame{position:relative;min-height:calc(297mm - 20mm - 34px);margin:16px 16px 18px;border:1px solid #111;padding:6mm 4mm 5mm;box-sizing:border-box;}' +
      '.corner{position:absolute;width:22px;height:22px;display:block;box-sizing:border-box;line-height:0;}' +
      '.corner svg{display:block;width:100%;height:100%;}' +
      '.corner.tl{top:0;left:0;}.corner.tr{top:0;right:0;}.corner.bl{bottom:0;left:0;}.corner.br{bottom:0;right:0;}' +
      '.sheet-header h2{margin:0 0 8px;text-align:center;font-size:20px;letter-spacing:.2px;}' +
      '.meta-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:5px 12px;margin-bottom:8px;}' +
      '.instruction-box{display:flex;justify-content:space-between;gap:12px;align-items:center;padding:2px 0 8px;margin-bottom:8px;font-size:12px;}' +
      '.subnote{margin-bottom:8px;font-size:11px;color:#334155;}' +
      '.identity-grid{display:grid;grid-template-columns:1.15fr .95fr;gap:18px;margin-bottom:10px;align-items:start;min-height:520px;}' +
      '.identity-card{padding:4px 6px 6px 0;min-height:100%;box-sizing:border-box;}' +
      '.digit-stage{position:relative;justify-self:end;padding:4px 0 0 0;box-sizing:border-box;}' +
      '.digit-card{padding:0;min-height:100%;box-sizing:border-box;}' +
      '.identity-title{font-weight:700;margin-bottom:8px;text-transform:uppercase;font-size:12px;letter-spacing:.4px;}' +
      '.identity-line{display:flex;align-items:center;gap:6px;margin-bottom:8px;}' +
      '.identity-line .line{display:inline-block;flex:1;border-bottom:1px solid #111;height:16px;}' +
      '.identity-line .line.short{max-width:80px;}' +
      '.identity-code{margin-top:12px;}.identity-foot{margin-top:10px;font-size:11px;color:#475569;}' +
      '.mono{font-family:\"Courier New\",monospace;letter-spacing:.4px;}' +
      '.digit-region-wrap{padding-top:10px;}' +
      '.digit-table-wrap{position:relative;}' +
      '.digit-table{width:auto;border-collapse:collapse;table-layout:fixed;}' +
      '.digit-table th,.digit-table td{border:1px solid #111;text-align:center;padding:0;height:30px;}' +
      '.digit-table th{background:#f8fafc;height:32px;padding:0 4px;}' +
      '.digit-table th:first-child,.digit-no-cell{width:54px;}' +
      '.digit-table th:nth-child(2),.digit-table th:nth-child(3),.digit-bubble-cell{width:86px;}' +
      '.row-guide{display:inline-block;background:#111;border-radius:1px;}' +
      '.row-guide.digit{width:14px;height:6px;}' +
      '.column-guide{display:inline-block;justify-self:center;background:#111;border-radius:1px;}' +
      '.column-guide.digit{width:6px;height:14px;}' +
      '.digit-guide-rail{position:absolute;top:50px;right:-34px;display:grid;grid-template-rows:repeat(10,30px);align-items:center;justify-items:start;width:18px;}' +
      '.digit-column-rail{position:absolute;left:56px;bottom:-32px;width:172px;display:grid;grid-template-columns:86px 86px;align-items:start;justify-items:center;}' +
      '.bubble{display:inline-block;width:14px;height:14px;border:1.5px solid #111;border-radius:999px;vertical-align:middle;}' +
      '.bubble.small{width:12px;height:12px;}' +
      '</style></head><body>' + sectionsHtml + '</body></html>'
  }

  async function buildExamParticipantReleasePayload(row) {
    var meta = parseMeta(row)
    var classRows = ensureParticipantClassRows(meta, row)
    if (!classRows.length) throw new Error('Detail kelas untuk jadwal ini belum lengkap. Buka "Atur Detail" terlebih dahulu.')

    var kelasIds = classRows.map(function (item) { return String(item.kelas_id || '').trim() }).filter(Boolean)
    if (!kelasIds.length) throw new Error('Kelas peserta ujian belum ditemukan.')

    if (!String(meta.semester_nama || '').trim() || !String(meta.tahun_ajaran_nama || '').trim()) {
      var active = await getActiveSemesterAndYear()
      if (active.semester && !meta.semester_nama) meta.semester_nama = String(active.semester.nama || '')
      if (active.tahun && !meta.tahun_ajaran_nama) meta.tahun_ajaran_nama = String(active.tahun.nama || '')
    }

    var santriRes = await sb
      .from('santri')
      .select('id, nama, kelas_id, aktif')
      .eq('aktif', true)
      .in('kelas_id', kelasIds)
      .order('nama', { ascending: true })

    if (santriRes.error) throw santriRes.error

    var nowIso = new Date().toISOString()
    var santriByKelas = new Map()
    ;(santriRes.data || []).forEach(function (item) {
      var kelasId = String(item && item.kelas_id || '')
      if (!kelasId) return
      if (!santriByKelas.has(kelasId)) santriByKelas.set(kelasId, [])
      santriByKelas.get(kelasId).push(item)
    })

    var payload = []
    var releaseClasses = []
    classRows.forEach(function (classRow) {
      var kelasId = String(classRow.kelas_id || '')
      var santriList = (santriByKelas.get(kelasId) || []).slice().sort(function (a, b) {
        return String(a && a.nama || '').localeCompare(String(b && b.nama || ''), 'id')
      })
      var examCode = buildExamCode(row, meta, classRow)
      releaseClasses.push({
        kelas_id: kelasId,
        kelas_nama: String(classRow.kelas_nama || '-'),
        lokasi: String(classRow.lokasi || ''),
        pengawas: String(classRow.pengawas || ''),
        exam_code: examCode,
        total_peserta: santriList.length
      })
      santriList.forEach(function (santri, idx) {
        var nomorUrut = idx + 1
        var nomorLabel = buildSheetNumberLabel(nomorUrut)
        payload.push({
          jadwal_id: String(row.id || ''),
          santri_id: String(santri.id || ''),
          nama_santri: String(santri.nama || '-'),
          kelas_id: kelasId,
          kelas_nama: String(classRow.kelas_nama || '-'),
          nomor_urut: nomorUrut,
          nomor_urut_label: nomorLabel,
          exam_code: examCode,
          sheet_code: examCode + '-' + nomorLabel,
          released_at: nowIso,
          updated_at: nowIso
        })
      })
    })

    return {
      row: row,
      meta: meta,
      payload: payload,
      releaseClasses: releaseClasses,
      participantTotal: payload.length,
      releasedAt: nowIso
    }
  }

  window.releaseExamParticipants = async function releaseExamParticipants(jadwalId) {
    var sid = String(jadwalId || '')
    var row = (folderRowsCache || []).find(function (item) { return String(item.id || '') === sid })
    if (!row) {
      alert('Jadwal ujian tidak ditemukan.')
      return
    }

    var existingParticipants = await loadExamParticipantsByJadwal(sid)
    if (existingParticipants === null) return
    if (existingParticipants.length) {
      var replaceOk = true
      if (typeof window.showPopupConfirm === 'function') replaceOk = await window.showPopupConfirm('Peserta ujian untuk mapel ini sudah pernah dirilis. Susun ulang nomor urut dan sinkronkan ulang peserta?')
      else replaceOk = window.confirm('Peserta ujian untuk mapel ini sudah pernah dirilis. Susun ulang nomor urut dan sinkronkan ulang peserta?')
      if (!replaceOk) return
    }

    var built
    try {
      built = await buildExamParticipantReleasePayload(row)
    } catch (error) {
      alert('Gagal menyiapkan peserta ujian: ' + String(error && error.message || 'Unknown error'))
      return
    }

    if (!built.payload.length) {
      alert('Belum ada santri aktif pada kelas yang terhubung ke jadwal ini.')
      return
    }

    var deleteRes = await sb.from(EXAM_PARTICIPANT_TABLE).delete().eq('jadwal_id', sid)
    if (deleteRes.error) {
      if (String(deleteRes.error.message || '').toLowerCase().includes(EXAM_PARTICIPANT_TABLE)) {
        alert(missingParticipantTableMessage())
        return
      }
      alert('Gagal membersihkan peserta ujian lama: ' + String(deleteRes.error.message || 'Unknown error'))
      return
    }

    var insertRes = await sb.from(EXAM_PARTICIPANT_TABLE).insert(built.payload)
    if (insertRes.error) {
      if (String(insertRes.error.message || '').toLowerCase().includes(EXAM_PARTICIPANT_TABLE)) {
        alert(missingParticipantTableMessage())
        return
      }
      alert('Gagal merilis peserta ujian: ' + String(insertRes.error.message || 'Unknown error'))
      return
    }

    var newMeta = Object.assign({}, built.meta, {
      participant_release: {
        released_at: built.releasedAt,
        total_peserta: built.participantTotal,
        class_count: built.releaseClasses.length,
        classes: built.releaseClasses
      }
    })
    var updateRes = await sb
      .from(EXAM_SCHEDULE_TABLE)
      .update({
        keterangan: JSON.stringify(newMeta),
        updated_at: new Date().toISOString()
      })
      .eq('id', sid)
    if (updateRes.error) {
      alert('Peserta sudah dibuat, tetapi metadata jadwal gagal diperbarui: ' + String(updateRes.error.message || 'Unknown error'))
    }

    alert('Peserta ujian berhasil dirilis: ' + String(built.participantTotal) + ' santri.')
    await window.reloadJadwalUjianFolders()
  }

  function groupParticipantsByClass(rows) {
    var grouped = new Map()
    ;(rows || []).forEach(function (item) {
      var kelasId = String(item && item.kelas_id || '')
      if (!grouped.has(kelasId)) grouped.set(kelasId, [])
      grouped.get(kelasId).push(item)
    })
    return grouped
  }

  function buildParticipantModalHtml(row, participants) {
    var meta = parseMeta(row)
    var grouped = groupParticipantsByClass(participants)
    if (!participants.length) return '<div class="placeholder-card">Peserta ujian belum dirilis untuk mapel ini.</div>'

    var releaseMap = new Map()
    var releaseClasses = Array.isArray(meta && meta.participant_release && meta.participant_release.classes) ? meta.participant_release.classes : []
    releaseClasses.forEach(function (item) {
      releaseMap.set(String(item && item.kelas_id || ''), item)
    })

    var html = ''
    grouped.forEach(function (items, kelasId) {
      var first = items[0] || {}
      var releaseInfo = releaseMap.get(kelasId) || {}
      html += '<div style="border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:12px; margin-bottom:12px;">'
      html += '<div style="display:flex; align-items:flex-start; justify-content:space-between; gap:10px; flex-wrap:wrap; margin-bottom:8px;">'
      html += '<div><div style="font-weight:700; color:#0f172a;">Kelas ' + escapeHtml(first.kelas_nama || '-') + '</div><div style="font-size:12px; color:#475569;">Kode ujian: <b>' + escapeHtml(first.exam_code || '-') + '</b> | Peserta: ' + items.length + ' santri</div></div>'
      html += '<div style="font-size:12px; color:#64748b; text-align:right;"><div>Lokasi: ' + escapeHtml(releaseInfo.lokasi || '-') + '</div><div>Pengawas: ' + escapeHtml(releaseInfo.pengawas || '-') + '</div></div>'
      html += '</div>'
      html += '<div style="overflow:auto; border:1px solid #e2e8f0; border-radius:10px;">'
      html += '<table style="width:100%; min-width:680px; border-collapse:collapse; font-size:13px;"><thead><tr style="background:#f8fafc;"><th style="padding:8px; border:1px solid #e2e8f0; width:44px;">No</th><th style="padding:8px; border:1px solid #e2e8f0; width:90px;">Nomor Urut</th><th style="padding:8px; border:1px solid #e2e8f0;">Nama Santri</th><th style="padding:8px; border:1px solid #e2e8f0;">Sheet Code</th></tr></thead><tbody>'
      items.forEach(function (item, idx) {
        html += '<tr><td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">' + (idx + 1) + '</td><td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">' + escapeHtml(item.nomor_urut_label || '-') + '</td><td style="padding:8px; border:1px solid #e2e8f0;">' + escapeHtml(item.nama_santri || '-') + '</td><td style="padding:8px; border:1px solid #e2e8f0; font-family:monospace;">' + escapeHtml(item.sheet_code || '-') + '</td></tr>'
      })
      html += '</tbody></table></div></div>'
    })
    return html
  }

  window.openExamParticipantsModal = async function openExamParticipantsModal(jadwalId) {
    var sid = String(jadwalId || '')
    var row = (folderRowsCache || []).find(function (item) { return String(item.id || '') === sid })
    if (!row) {
      alert('Jadwal ujian tidak ditemukan.')
      return
    }
    var modalEl = getInput('ju-participants-modal')
    var titleEl = getInput('ju-participants-modal-title')
    var bodyEl = getInput('ju-participants-modal-body')
    if (!modalEl || !titleEl || !bodyEl) return

    bodyEl.innerHTML = 'Loading peserta ujian...'
    titleEl.textContent = 'Peserta Ujian: ' + String(row.mapel || '-')
    modalEl.style.display = 'flex'

    var participants = await loadExamParticipantsByJadwal(sid)
    if (participants === null) {
      modalEl.style.display = 'none'
      return
    }
    activeParticipantModalState = { row: row, participants: participants }
    bodyEl.innerHTML = buildParticipantModalHtml(row, participants)
  }

  window.closeExamParticipantsModal = function closeExamParticipantsModal(event) {
    if (event && event.target && event.target.id !== 'ju-participants-modal') return
    var modalEl = getInput('ju-participants-modal')
    if (modalEl) modalEl.style.display = 'none'
    activeParticipantModalState = { row: null, participants: [] }
  }

  window.closeCreateExamFolderDraftModal = function closeCreateExamFolderDraftModal(event) {
    if (event && event.target && event.target.id !== 'ju-create-folder-modal') return
    resolveCreateExamFolderDraftModal(null)
  }

  window.updateCreateFolderMapelSummary = function updateCreateFolderMapelSummary() {
    var bodyEl = getInput('ju-create-folder-mapel-select')
    var summaryEl = getInput('ju-create-folder-mapel-summary')
    if (!summaryEl || !bodyEl) return
    var total = bodyEl.querySelectorAll('input[type="checkbox"][data-mapel-key]').length
    var picked = bodyEl.querySelectorAll('input[type="checkbox"][data-mapel-key]:checked').length
    summaryEl.textContent = picked + ' dari ' + total + ' mapel dipilih'
  }

  window.selectAllCreateFolderMapel = function selectAllCreateFolderMapel(allChecked) {
    var bodyEl = getInput('ju-create-folder-mapel-select')
    if (!bodyEl) return
    bodyEl.querySelectorAll('input[type="checkbox"][data-mapel-key]').forEach(function (inputEl) {
      inputEl.checked = Boolean(allChecked)
    })
    window.updateCreateFolderMapelSummary()
  }

  window.confirmCreateExamFolderDraftModal = function confirmCreateExamFolderDraftModal() {
    var jenis = String(getInput('ju-create-folder-jenis') && getInput('ju-create-folder-jenis').value || '').trim().toUpperCase()
    var semesterEl = getInput('ju-create-folder-semester')
    var tahunEl = getInput('ju-create-folder-tahun')
    var semesterId = String(semesterEl && semesterEl.value || '').trim()
    var tahunId = String(tahunEl && tahunEl.value || '').trim()
    var semesterNama = getSelectedSelectLabel(semesterEl)
    var tahunNama = getSelectedSelectLabel(tahunEl)
    var folderName = String(getInput('ju-create-folder-name') && getInput('ju-create-folder-name').value || '').trim()
    var selectedMapelKeys = getSelectedCreateFolderMapelKeys()
    if (!jenis) {
      alert('Jenis ujian wajib diisi.')
      return
    }
    if (!semesterId || !semesterNama) {
      alert('Semester wajib diisi.')
      return
    }
    if (!tahunId || !tahunNama) {
      alert('Tahun ajaran wajib diisi.')
      return
    }
    if (!folderName) {
      alert('Nama folder ujian wajib diisi.')
      return
    }
    if (!selectedMapelKeys.length) {
      alert('Pilih minimal 1 mapel untuk dibuatkan folder ujian.')
      return
    }
    resolveCreateExamFolderDraftModal({
      jenis: jenis,
      semesterId: semesterId,
      semesterNama: semesterNama,
      tahunId: tahunId,
      tahunNama: tahunNama,
      folderName: folderName,
      selectedMapelKeys: selectedMapelKeys
    })
  }

  function buildExamAttendancePrintHtml(row, participants) {
    var meta = parseMeta(row)
    var grouped = groupParticipantsByClass(participants)
    var releaseMap = new Map()
    var releaseClasses = Array.isArray(meta && meta.participant_release && meta.participant_release.classes) ? meta.participant_release.classes : []
    releaseClasses.forEach(function (item) { releaseMap.set(String(item && item.kelas_id || ''), item) })

    var sectionsHtml = ''
    grouped.forEach(function (items, kelasId) {
      var first = items[0] || {}
      var releaseInfo = releaseMap.get(kelasId) || {}
      var rowsHtml = items.map(function (item, idx) {
        return '<tr><td style="padding:8px; border:1px solid #111; text-align:center;">' + (idx + 1) + '</td><td style="padding:8px; border:1px solid #111; text-align:center;">' + escapeHtml(item.nomor_urut_label || '-') + '</td><td style="padding:8px; border:1px solid #111;">' + escapeHtml(item.nama_santri || '-') + '</td><td style="padding:8px; border:1px solid #111; height:34px;"></td><td style="padding:8px; border:1px solid #111;"></td></tr>'
      }).join('')
      sectionsHtml += '<section class="sheet"><div class="sheet-header"><h2>Daftar Hadir Ujian</h2><div class="meta-grid"><div><b>Nama Ujian:</b> ' + escapeHtml(row && row.nama || '-') + '</div><div><b>Jenis:</b> ' + escapeHtml(row && row.jenis || '-') + '</div><div><b>Mapel:</b> ' + escapeHtml(row && row.mapel || '-') + '</div><div><b>Kelas:</b> ' + escapeHtml(first.kelas_nama || '-') + '</div><div><b>Tanggal:</b> ' + escapeHtml(formatDateLabel(row && row.tanggal)) + '</div><div><b>Waktu:</b> ' + escapeHtml(formatTimeRangeLabel(row)) + '</div><div><b>Lokasi:</b> ' + escapeHtml(releaseInfo.lokasi || '-') + '</div><div><b>Pengawas:</b> ' + escapeHtml(releaseInfo.pengawas || '-') + '</div><div style="grid-column:1 / -1;"><b>Kode Ujian:</b> ' + escapeHtml(first.exam_code || '-') + '</div></div></div><table class="attendance-table"><thead><tr><th style="width:44px;">No</th><th style="width:90px;">Nomor Urut</th><th>Nama Santri</th><th style="width:180px;">Tanda Tangan</th><th style="width:140px;">Keterangan</th></tr></thead><tbody>' + rowsHtml + '</tbody></table></section>'
    })

    return '<!doctype html><html lang="id"><head><meta charset="utf-8"><title>Daftar Hadir Ujian</title><style>@page{size:A4 portrait;margin:12mm;}body{font-family:"Times New Roman",serif;margin:0;color:#111;font-size:13px;}.sheet{page-break-after:always;}.sheet:last-child{page-break-after:auto;}h2{margin:0 0 10px 0;text-align:center;font-size:22px;}.meta-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:6px 12px;margin-bottom:12px;}.attendance-table{width:100%;border-collapse:collapse;font-size:13px;}.attendance-table th,.attendance-table td{border:1px solid #111;padding:8px;vertical-align:top;}.attendance-table th{text-align:center;background:#f8fafc;}</style></head><body>' + sectionsHtml + '</body></html>'
  }

  window.printExamAttendanceSheet = function printExamAttendanceSheet() {
    var row = activeParticipantModalState.row
    var participants = activeParticipantModalState.participants || []
    if (!row || !participants.length) {
      alert('Belum ada peserta ujian untuk dicetak.')
      return
    }
    var html = buildExamAttendancePrintHtml(row, participants)
    var win = window.open('', '_blank')
    if (!win) {
      alert('Popup diblokir browser. Izinkan popup untuk mencetak daftar hadir.')
      return
    }
    win.document.open()
    win.document.write(html)
    win.document.close()
    win.focus()
    setTimeout(function () { win.print() }, 250)
  }

  window.printExamAnswerSheetTemplate = async function printExamAnswerSheetTemplate() {
    var row = activeParticipantModalState.row
    var participants = activeParticipantModalState.participants || []
    if (!row || !participants.length) {
      alert('Belum ada peserta ujian untuk dicetak.')
      return
    }

    var questionRes = await loadExamQuestionRowsByJadwal(row.id)
    if (!questionRes) return

    var grouped = groupParticipantsByClass(participants)
    var missingClasses = []
    grouped.forEach(function (items) {
      var kelasNama = String(items && items[0] && items[0].kelas_nama || '-')
      var soalRow = pickQuestionRowForClass(questionRes.rows, kelasNama)
      var pgCount = countMultipleChoiceQuestions(soalRow)
      if (!soalRow || pgCount <= 0) missingClasses.push(kelasNama)
    })
    if (missingClasses.length) {
      alert('Lembar jawaban PG belum bisa dicetak karena soal pilihan ganda belum ditemukan untuk kelas: ' + missingClasses.join(', ') + '. Pastikan guru sudah menyimpan atau submit soal PG untuk kelas tersebut.')
      return
    }

    var html = buildExamAnswerSheetPrintHtml(row, participants, questionRes.rows)
    var win = window.open('', '_blank')
    if (!win) {
      alert('Popup diblokir browser. Izinkan popup untuk mencetak lembar jawaban.')
      return
    }
    win.document.open()
    win.document.write(html)
    win.document.close()
    win.focus()
    setTimeout(function () { win.print() }, 250)
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

    var semesterId = String(distribusi.active && distribusi.active.semester && distribusi.active.semester.id || '').trim()
    var tahunId = String(distribusi.active && distribusi.active.tahun && distribusi.active.tahun.id || '').trim()
    var semesterNama = String(distribusi.active && distribusi.active.semester && distribusi.active.semester.nama || '-').trim()
    var tahunNama = String(distribusi.active && distribusi.active.tahun && distribusi.active.tahun.nama || '-').trim()
    var folderName = buildFolderNameByParts(jenis, semesterNama, tahunNama)

    var groups = groupMapelPerangkatan(distribusi.rows, distribusi.kelasMap, distribusi.mapelMap)
    if (!groups.length) {
      alert('Belum ada distribusi mapel aktif untuk dibuatkan folder ujian.')
      return
    }

    var referenceOptions
    try {
      referenceOptions = await loadCreateFolderReferenceOptions(distribusi.active)
    } catch (error) {
      alert(String(error && error.message || 'Gagal memuat data referensi semester/tahun ajaran.'))
      return
    }
    var editedDraft = await openCreateExamFolderDraftModal({
      jenis: jenis,
      semesterId: semesterId,
      semesterNama: semesterNama,
      tahunId: tahunId,
      tahunNama: tahunNama,
      folderName: folderName,
      selectedMapelKeys: groups.map(getMapelGroupKey)
    }, groups, referenceOptions)
    if (!editedDraft) return

    jenis = String(editedDraft.jenis || '').trim().toUpperCase()
    semesterId = String(editedDraft.semesterId || '').trim()
    semesterNama = String(editedDraft.semesterNama || '').trim()
    tahunId = String(editedDraft.tahunId || '').trim()
    tahunNama = String(editedDraft.tahunNama || '').trim()
    folderName = String(editedDraft.folderName || '').trim()
    var selectedMapelKeySet = new Set(Array.isArray(editedDraft.selectedMapelKeys) ? editedDraft.selectedMapelKeys : [])
    var selectedGroups = groups.filter(function (group) { return selectedMapelKeySet.has(getMapelGroupKey(group)) })

    if (!jenis) {
      alert('Jenis ujian wajib diisi.')
      return
    }
    if (!semesterId || !semesterNama) {
      alert('Semester wajib dipilih.')
      return
    }
    if (!tahunId || !tahunNama) {
      alert('Tahun ajaran wajib dipilih.')
      return
    }
    if (!folderName) {
      alert('Nama folder ujian wajib diisi.')
      return
    }
    if (!selectedGroups.length) {
      alert('Pilih minimal 1 mapel untuk dibuatkan folder ujian.')
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
    selectedGroups.forEach(function (group) {
      var key = getMapelGroupKey(group)
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
          semester_id: semesterId || null,
          semester_nama: semesterNama,
          tahun_ajaran_id: tahunId || null,
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
