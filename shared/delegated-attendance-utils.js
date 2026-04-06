;(function initDelegatedAttendanceUtils() {
  if (window.DelegatedAttendanceUtils) return

  const ATTENDANCE_TABLE = 'absensi_santri'
  const ATTENDANCE_DELEGATION_TABLE = 'absensi_pengganti_tugas'
  const ATTENDANCE_STATUSES = ['Hadir', 'Terlambat', 'Sakit', 'Izin', 'Alpa']
  const stateByScope = new Map()

  function escapeHtml(value) {
    return String(value ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;')
  }

  function pickLabelByKeys(item, keys) {
    for (const key of keys) {
      const value = item?.[key]
      if (value !== null && value !== undefined && String(value).trim() !== '') {
        return String(value).trim()
      }
    }
    return ''
  }

  function getMapelLabel(mapel) {
    if (!mapel) return '-'
    const namaMapel = pickLabelByKeys(mapel, ['nama', 'nama_mapel', 'mapel']) || '-'
    return `${namaMapel}${mapel.kategori ? ` (${mapel.kategori})` : ''}`
  }

  function toTimeLabel(value) {
    const raw = String(value || '').trim()
    if (!raw) return '-'
    return raw.slice(0, 5)
  }

  function round2(value) {
    const num = Number(value)
    if (!Number.isFinite(num)) return null
    return Math.round(num * 100) / 100
  }

  function calculateAttendanceScore(rows = [], maxScore = 10) {
    const total = rows.length
    if (!total) return null
    const hadirCount = rows.filter(row => {
      const status = String(row?.status || '').trim().toLowerCase()
      return status === 'hadir' || status === 'terlambat'
    }).length
    return round2((hadirCount / total) * maxScore)
  }

  function isMissingTableError(error, tableName) {
    const code = String(error?.code || '').toUpperCase()
    const msg = String(error?.message || '').toLowerCase()
    const table = String(tableName || '').trim().toLowerCase()
    if (code === '42P01') return true
    if (!table) return false
    return msg.includes(`public.${table}`) || (msg.includes('relation') && msg.includes(table))
  }

  function buildAttendanceTableMissingMessage() {
    return `Tabel '${ATTENDANCE_TABLE}' belum ada di Supabase.`
  }

  function buildDelegationTableMissingMessage() {
    return `Tabel '${ATTENDANCE_DELEGATION_TABLE}' belum ada di Supabase.`
  }

  function getTodayDate() {
    return new Date().toISOString().slice(0, 10)
  }

  function getState(scope) {
    const key = String(scope || 'default').trim() || 'default'
    if (!stateByScope.has(key)) {
      stateByScope.set(key, {
        scope: key,
        options: null,
        profile: null,
        pendingRows: [],
        kelasMap: new Map(),
        mapelMap: new Map(),
        guruMap: new Map(),
        jamMap: new Map(),
        santriRows: [],
        selectedDate: getTodayDate(),
        selectedKelasId: '',
        selectedMapelId: ''
      })
    }
    return stateByScope.get(key)
  }

  async function hasPendingAssignments({ sb, getProfile }) {
    const profile = await getProfile()
    const uid = String(profile?.id || '').trim()
    if (!uid) return false
    const { data, error } = await sb
      .from(ATTENDANCE_DELEGATION_TABLE)
      .select('id')
      .eq('guru_pengganti_id', uid)
      .eq('status', 'pending')
      .limit(1)
    if (error) {
      console.error(error)
      return false
    }
    return Array.isArray(data) && data.length > 0
  }

  async function loadPendingRows(state) {
    const sb = state.options?.sb
    const uid = String(state.profile?.id || '').trim()
    const tanggal = String(state.selectedDate || '').trim()
    if (!sb || !uid || !tanggal) {
      state.pendingRows = []
      return []
    }
    const { data, error } = await sb
      .from(ATTENDANCE_DELEGATION_TABLE)
      .select('id, tanggal, kelas_id, mapel_id, distribusi_id, semester_id, jam_pelajaran_id, guru_asal_id, guru_pengganti_id, keterangan, status, created_at, filled_at')
      .eq('guru_pengganti_id', uid)
      .eq('tanggal', tanggal)
      .eq('status', 'pending')
      .order('created_at', { ascending: true })
    if (error) throw error
    state.pendingRows = data || []
    return state.pendingRows
  }

  async function loadLookupMaps(state) {
    const sb = state.options?.sb
    const rows = state.pendingRows || []
    const kelasIds = [...new Set(rows.map(item => String(item.kelas_id || '').trim()).filter(Boolean))]
    const mapelIds = [...new Set(rows.map(item => String(item.mapel_id || '').trim()).filter(Boolean))]
    const guruIds = [...new Set(rows.map(item => String(item.guru_asal_id || '').trim()).filter(Boolean))]
    const jamIds = [...new Set(rows.map(item => String(item.jam_pelajaran_id || '').trim()).filter(Boolean))]

    const [kelasRes, mapelRes, guruRes, jamRes] = await Promise.all([
      kelasIds.length ? sb.from('kelas').select('id, nama_kelas').in('id', kelasIds) : Promise.resolve({ data: [], error: null }),
      mapelIds.length ? sb.from('mapel').select('id, nama, nama_mapel, kategori').in('id', mapelIds) : Promise.resolve({ data: [], error: null }),
      guruIds.length ? sb.from('karyawan').select('id, nama, id_karyawan').in('id', guruIds) : Promise.resolve({ data: [], error: null }),
      jamIds.length ? sb.from('jam_pelajaran').select('id, nama, jam_mulai, jam_selesai, urutan').in('id', jamIds).order('urutan', { ascending: true }) : Promise.resolve({ data: [], error: null })
    ])

    if (kelasRes.error) console.error(kelasRes.error)
    if (mapelRes.error) console.error(mapelRes.error)
    if (guruRes.error) console.error(guruRes.error)
    if (jamRes.error) console.error(jamRes.error)

    state.kelasMap = new Map((kelasRes.data || []).map(item => [String(item.id || ''), item]))
    state.mapelMap = new Map((mapelRes.data || []).map(item => [String(item.id || ''), item]))
    state.guruMap = new Map((guruRes.data || []).map(item => [String(item.id || ''), item]))
    state.jamMap = new Map((jamRes.data || []).map(item => [String(item.id || ''), item]))
  }

  async function loadSantriByKelas(state) {
    const sb = state.options?.sb
    const kelasId = String(state.selectedKelasId || '').trim()
    if (!sb || !kelasId) {
      state.santriRows = []
      return []
    }
    const { data, error } = await sb
      .from('santri')
      .select('id, nama, kelas_id')
      .eq('kelas_id', kelasId)
      .eq('aktif', true)
      .order('nama')
    if (error) throw error
    state.santriRows = data || []
    return state.santriRows
  }

  function getFilteredRows(state) {
    return (state.pendingRows || []).filter(row => {
      if (state.selectedKelasId && String(row.kelas_id || '') !== state.selectedKelasId) return false
      if (state.selectedMapelId && String(row.mapel_id || '') !== state.selectedMapelId) return false
      return true
    })
  }

  function getContainer(state) {
    return document.getElementById(String(state.options?.containerId || ''))
  }

  function renderSantriTable(state) {
    const box = document.getElementById(`delegated-absensi-students-${state.scope}`)
    if (!box) return
    if (!state.selectedKelasId || !state.selectedMapelId) {
      box.innerHTML = '<div class="placeholder-card">Pilih kelas dan mapel amanah terlebih dahulu.</div>'
      return
    }
    if (!(state.santriRows || []).length) {
      box.innerHTML = '<div class="placeholder-card">Belum ada data santri untuk kelas ini.</div>'
      return
    }
    box.innerHTML = `
      <div style="overflow:auto; border:1px solid #e2e8f0; border-radius:10px;">
        <table style="width:100%; min-width:520px; border-collapse:collapse; font-size:13px;">
          <thead>
            <tr style="background:#f8fafc;">
              <th style="padding:8px; border:1px solid #e2e8f0; width:50px;">No</th>
              <th style="padding:8px; border:1px solid #e2e8f0; text-align:left;">Nama Santri</th>
              <th style="padding:8px; border:1px solid #e2e8f0; width:190px; text-align:left;">Kehadiran</th>
            </tr>
          </thead>
          <tbody>
            ${(state.santriRows || []).map((santri, idx) => `
              <tr>
                <td style="padding:8px; border:1px solid #e2e8f0; text-align:center;">${idx + 1}</td>
                <td style="padding:8px; border:1px solid #e2e8f0;">${escapeHtml(String(santri.nama || '-'))}</td>
                <td style="padding:8px; border:1px solid #e2e8f0;">
                  <select class="guru-field" data-delegated-absensi-santri-id="${escapeHtml(String(santri.id || ''))}">
                    ${ATTENDANCE_STATUSES.map(status => `<option value="${escapeHtml(status)}" ${status === 'Hadir' ? 'selected' : ''}>${escapeHtml(status)}</option>`).join('')}
                  </select>
                </td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>
    `
  }

  function renderInfo(state) {
    const box = document.getElementById(`delegated-absensi-info-${state.scope}`)
    if (!box) return
    const rows = getFilteredRows(state)
    if (!rows.length) {
      box.innerHTML = '<div class="placeholder-card">Belum ada amanah absensi pada tanggal ini.</div>'
      return
    }
    const guruNames = [...new Set(rows.map(row => String(state.guruMap.get(String(row.guru_asal_id || ''))?.nama || '-')).filter(Boolean))]
    const jamLabels = rows.map(row => {
      const jam = state.jamMap.get(String(row.jam_pelajaran_id || ''))
      return jam
        ? `${String(jam.nama || 'Jam')} (${toTimeLabel(jam.jam_mulai)}-${toTimeLabel(jam.jam_selesai)})`
        : 'Jam tanpa label'
    })
    box.innerHTML = `
      <div class="placeholder-card" style="background:#fff; border-color:#cbd5e1;">
        <div style="font-weight:700; color:#0f172a; margin-bottom:6px;">Amanah Absensi Aktif</div>
        <div style="font-size:13px; color:#475569; line-height:1.6;">
          Menggantikan: <strong>${escapeHtml(guruNames.join(', ') || '-')}</strong><br>
          Sesi: ${escapeHtml(jamLabels.join(', '))}
          ${rows[0]?.keterangan ? `<br>Catatan: ${escapeHtml(String(rows[0].keterangan || ''))}` : ''}
        </div>
      </div>
    `
  }

  function renderControls(state) {
    const kelasSelect = document.getElementById(`delegated-absensi-kelas-${state.scope}`)
    const mapelSelect = document.getElementById(`delegated-absensi-mapel-${state.scope}`)
    if (!kelasSelect || !mapelSelect) return

    const kelasIds = [...new Set((state.pendingRows || []).map(row => String(row.kelas_id || '')).filter(Boolean))]
    kelasSelect.innerHTML = '<option value="">-- Pilih Kelas --</option>' + kelasIds
      .map(id => {
        const kelas = state.kelasMap.get(id)
        return `<option value="${escapeHtml(id)}">${escapeHtml(String(kelas?.nama_kelas || id))}</option>`
      }).join('')
    if (state.selectedKelasId && kelasIds.includes(state.selectedKelasId)) {
      kelasSelect.value = state.selectedKelasId
    } else {
      state.selectedKelasId = kelasIds.length === 1 ? kelasIds[0] : ''
      kelasSelect.value = state.selectedKelasId
    }

    const mapelIds = [...new Set((state.pendingRows || [])
      .filter(row => !state.selectedKelasId || String(row.kelas_id || '') === state.selectedKelasId)
      .map(row => String(row.mapel_id || ''))
      .filter(Boolean))]
    mapelSelect.innerHTML = '<option value="">-- Pilih Mapel --</option>' + mapelIds
      .map(id => {
        const mapel = state.mapelMap.get(id)
        return `<option value="${escapeHtml(id)}">${escapeHtml(getMapelLabel(mapel))}</option>`
      }).join('')
    if (state.selectedMapelId && mapelIds.includes(state.selectedMapelId)) {
      mapelSelect.value = state.selectedMapelId
    } else {
      state.selectedMapelId = mapelIds.length === 1 ? mapelIds[0] : ''
      mapelSelect.value = state.selectedMapelId
    }
  }

  function renderShell(state) {
    const container = getContainer(state)
    if (!container) return
    container.innerHTML = `
      <div class="placeholder-card" style="border-color:#93c5fd;">
        <div style="font-weight:700; color:#0f172a; margin-bottom:8px;">Input Absen Amanah Pengganti</div>
        <div style="font-size:12px; color:#64748b; margin-bottom:12px;">Menu ini hanya muncul saat Anda mendapat amanah absensi dari guru.</div>
        <div style="display:grid; grid-template-columns:repeat(auto-fit,minmax(180px,1fr)); gap:10px; align-items:end;">
          <div>
            <label class="guru-label">Tanggal</label>
            <input id="delegated-absensi-tanggal-${state.scope}" class="guru-field" type="date" value="${escapeHtml(state.selectedDate)}">
          </div>
          <div>
            <label class="guru-label">Kelas</label>
            <select id="delegated-absensi-kelas-${state.scope}" class="guru-field"></select>
          </div>
          <div>
            <label class="guru-label">Mapel</label>
            <select id="delegated-absensi-mapel-${state.scope}" class="guru-field"></select>
          </div>
          <div>
            <label class="guru-label">Patron Materi (Opsional)</label>
            <input id="delegated-absensi-materi-${state.scope}" class="guru-field" type="text" placeholder="Tulis materi yang diajarkan">
          </div>
        </div>
        <div id="delegated-absensi-info-${state.scope}" style="margin-top:12px;"></div>
        <div id="delegated-absensi-students-${state.scope}" style="margin-top:12px;"></div>
        <div style="margin-top:14px;">
          <button id="delegated-absensi-save-${state.scope}" type="button" class="modal-btn modal-btn-primary">Simpan Absensi</button>
        </div>
      </div>
    `
  }

  async function recalculateAttendanceScoreForDistribusi(sb, distribusi, santriIds) {
    const safeDistribusi = distribusi && typeof distribusi === 'object' ? distribusi : null
    const safeSantriIds = [...new Set((Array.isArray(santriIds) ? santriIds : []).map(id => String(id || '').trim()).filter(Boolean))]
    if (!safeDistribusi?.kelas_id || !safeDistribusi?.mapel_id || !safeDistribusi?.semester_id || !safeSantriIds.length) return
    const [absensiRes, existingRes] = await Promise.all([
      sb
        .from(ATTENDANCE_TABLE)
        .select('santri_id, tanggal, status')
        .eq('kelas_id', String(safeDistribusi.kelas_id))
        .eq('mapel_id', String(safeDistribusi.mapel_id))
        .eq('semester_id', String(safeDistribusi.semester_id))
        .in('santri_id', safeSantriIds),
      sb
        .from('nilai_akademik')
        .select('*')
        .eq('mapel_id', String(safeDistribusi.mapel_id))
        .eq('semester_id', String(safeDistribusi.semester_id))
        .in('santri_id', safeSantriIds)
    ])
    if (absensiRes.error) throw absensiRes.error
    if (existingRes.error) throw existingRes.error

    const absensiBySantri = new Map()
    ;(absensiRes.data || []).forEach(row => {
      const sid = String(row?.santri_id || '')
      if (!sid) return
      if (!absensiBySantri.has(sid)) absensiBySantri.set(sid, [])
      absensiBySantri.get(sid).push(row)
    })
    const existingMap = new Map((existingRes.data || []).map(item => [String(item.santri_id || ''), item]))

    for (const sid of safeSantriIds) {
      const existing = existingMap.get(sid) || {}
      const nilai_kehadiran = calculateAttendanceScore(absensiBySantri.get(sid) || [])
      const payload = {
        santri_id: sid,
        mapel_id: String(safeDistribusi.mapel_id),
        semester_id: String(safeDistribusi.semester_id),
        nilai_tugas: existing.nilai_tugas ?? null,
        nilai_ulangan_harian: existing.nilai_ulangan_harian ?? 0,
        nilai_pts: existing.nilai_pts ?? 0,
        nilai_pas: existing.nilai_pas ?? 0,
        nilai_kehadiran,
        nilai_akhir: round2(
          Number(existing.nilai_tugas || 0) +
          Number(existing.nilai_ulangan_harian || 0) +
          Number(existing.nilai_pts || 0) +
          Number(existing.nilai_pas || 0) +
          Number(nilai_kehadiran || 0)
        ),
        nilai_keterampilan: existing.nilai_keterampilan ?? 0
      }
      if (existing.id) {
        const { error } = await sb.from('nilai_akademik').update(payload).eq('id', existing.id)
        if (error) throw error
      } else {
        const { error } = await sb.from('nilai_akademik').insert(payload)
        if (error) throw error
      }
    }
  }

  async function saveAttendance(state) {
    const sb = state.options?.sb
    const rows = getFilteredRows(state)
    const saveBtn = document.getElementById(`delegated-absensi-save-${state.scope}`)
    if (!sb || !saveBtn) return
    if (!rows.length) {
      alert('Belum ada amanah absensi yang dipilih.')
      return
    }
    if (!(state.santriRows || []).length) {
      alert('Data santri untuk kelas ini belum tersedia.')
      return
    }
    saveBtn.disabled = true
    const previousText = saveBtn.textContent
    saveBtn.textContent = 'Menyimpan...'
    try {
      const materi = String(document.getElementById(`delegated-absensi-materi-${state.scope}`)?.value || '').trim()
      const statusMap = new Map()
      document.querySelectorAll(`[data-delegated-absensi-santri-id]`).forEach(selectEl => {
        const sid = String(selectEl.getAttribute('data-delegated-absensi-santri-id') || '').trim()
        if (!sid) return
        statusMap.set(sid, String(selectEl.value || 'Hadir').trim() || 'Hadir')
      })
      const santriIds = (state.santriRows || []).map(item => String(item.id || '')).filter(Boolean)

      for (const row of rows) {
        const payloads = (state.santriRows || []).map(santri => ({
          tanggal: String(row.tanggal || state.selectedDate),
          kelas_id: String(row.kelas_id || ''),
          mapel_id: String(row.mapel_id || ''),
          guru_id: String(row.guru_asal_id || ''),
          jam_pelajaran_id: String(row.jam_pelajaran_id || '').trim() || null,
          semester_id: String(row.semester_id || '').trim() || null,
          distribusi_id: String(row.distribusi_id || '').trim() || null,
          santri_id: String(santri.id || ''),
          status: statusMap.get(String(santri.id || '')) || 'Hadir',
          guru_pengganti_id: String(state.profile?.id || '').trim() || null,
          keterangan_pengganti: String(row.keterangan || '').trim() || null,
          patron_materi: materi || null
        }))
        let saveRes = await sb
          .from(ATTENDANCE_TABLE)
          .upsert(payloads, { onConflict: 'tanggal,kelas_id,mapel_id,jam_pelajaran_id,santri_id' })
        if (saveRes.error) {
          const msg = String(saveRes.error.message || '').toLowerCase()
          if (msg.includes('no unique or exclusion constraint matching the on conflict specification')) {
            alert("Database absensi masih memakai unique lama. Mohon update unique constraint agar mencakup jam_pelajaran_id.")
            return
          }
          if (isMissingTableError(saveRes.error, ATTENDANCE_TABLE)) {
            alert(buildAttendanceTableMissingMessage())
            return
          }
          throw saveRes.error
        }
        const doneRes = await sb
          .from(ATTENDANCE_DELEGATION_TABLE)
          .update({ status: 'done', filled_at: new Date().toISOString() })
          .eq('id', String(row.id || ''))
        if (doneRes.error && !isMissingTableError(doneRes.error, ATTENDANCE_DELEGATION_TABLE)) {
          throw doneRes.error
        }
      }

      const distribusiMap = new Map()
      rows.forEach(row => {
        const key = `${String(row.distribusi_id || '')}|${String(row.kelas_id || '')}|${String(row.mapel_id || '')}|${String(row.semester_id || '')}`
        if (!String(row.distribusi_id || '').trim() || !String(row.semester_id || '').trim()) return
        if (!distribusiMap.has(key)) {
          distribusiMap.set(key, {
            id: String(row.distribusi_id || ''),
            kelas_id: String(row.kelas_id || ''),
            mapel_id: String(row.mapel_id || ''),
            semester_id: String(row.semester_id || '')
          })
        }
      })
      for (const distribusi of distribusiMap.values()) {
        try {
          await recalculateAttendanceScoreForDistribusi(sb, distribusi, santriIds)
        } catch (error) {
          console.error(error)
        }
      }

      await refreshPage(state.scope, { keepSelection: false })
      if (typeof state.options?.onTasksChanged === 'function') {
        await state.options.onTasksChanged()
      }
      alert('Absensi amanah pengganti berhasil disimpan.')
    } catch (error) {
      console.error(error)
      alert(`Gagal menyimpan absensi: ${String(error?.message || 'Unknown error')}`)
    } finally {
      saveBtn.disabled = false
      saveBtn.textContent = previousText
    }
  }

  function bindEvents(state) {
    const dateEl = document.getElementById(`delegated-absensi-tanggal-${state.scope}`)
    const kelasEl = document.getElementById(`delegated-absensi-kelas-${state.scope}`)
    const mapelEl = document.getElementById(`delegated-absensi-mapel-${state.scope}`)
    const saveBtn = document.getElementById(`delegated-absensi-save-${state.scope}`)
    if (dateEl) {
      dateEl.addEventListener('change', async () => {
        state.selectedDate = String(dateEl.value || '').trim() || getTodayDate()
        state.selectedKelasId = ''
        state.selectedMapelId = ''
        await refreshPage(state.scope, { keepSelection: false })
      })
    }
    if (kelasEl) {
      kelasEl.addEventListener('change', async () => {
        state.selectedKelasId = String(kelasEl.value || '').trim()
        state.selectedMapelId = ''
        await loadSantriByKelas(state)
        renderControls(state)
        renderInfo(state)
        renderSantriTable(state)
      })
    }
    if (mapelEl) {
      mapelEl.addEventListener('change', () => {
        state.selectedMapelId = String(mapelEl.value || '').trim()
        renderInfo(state)
        renderSantriTable(state)
      })
    }
    if (saveBtn) {
      saveBtn.addEventListener('click', () => {
        void saveAttendance(state)
      })
    }
  }

  async function refreshPage(scope, { keepSelection = true } = {}) {
    const state = getState(scope)
    if (!state.options) return
    const container = getContainer(state)
    if (!container) return
    container.innerHTML = '<div class="placeholder-card">Loading absensi...</div>'
    try {
      if (!state.profile) state.profile = await state.options.getProfile()
      if (!state.profile?.id) {
        container.innerHTML = '<div class="placeholder-card">Profil karyawan tidak ditemukan.</div>'
        return
      }
      renderShell(state)
      await loadPendingRows(state)
      await loadLookupMaps(state)
      if (!keepSelection) {
        state.selectedKelasId = ''
        state.selectedMapelId = ''
      }
      renderControls(state)
      await loadSantriByKelas(state)
      renderInfo(state)
      renderSantriTable(state)
      bindEvents(state)
    } catch (error) {
      console.error(error)
      if (isMissingTableError(error, ATTENDANCE_DELEGATION_TABLE)) {
        container.innerHTML = `<div class="placeholder-card">${escapeHtml(buildDelegationTableMissingMessage())}</div>`
      } else {
        container.innerHTML = `<div class="placeholder-card">Gagal load absensi amanah: ${escapeHtml(String(error?.message || 'Unknown error'))}</div>`
      }
    }
  }

  window.DelegatedAttendanceUtils = {
    hasPendingAssignments,
    renderPage: async function renderPage(options = {}) {
      const scope = String(options.scope || 'default').trim() || 'default'
      const state = getState(scope)
      state.options = options
      state.profile = await options.getProfile()
      state.selectedDate = state.selectedDate || getTodayDate()
      await refreshPage(scope, { keepSelection: true })
    }
  }
})()
