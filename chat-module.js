(function initChatModule() {
  const CHAT_THREADS_TABLE = 'chat_threads'
  const CHAT_MEMBERS_TABLE = 'chat_thread_members'
  const CHAT_MESSAGES_TABLE = 'chat_messages'
  const CHAT_RETENTION_HOURS = 24
  const CHAT_EMOJIS = ['😀', '😁', '😂', '🤣', '😊', '😍', '🥰', '😘', '😎', '🤔', '😭', '😡', '👍', '👏', '🙏', '💪', '🔥', '⭐', '🎉', '✅', '❌', '📚', '📝', '📌', '💯']

  function escapeHtml(value) {
    return String(value ?? '')
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#39;')
  }

  function getCutoffIso() {
    return new Date(Date.now() - CHAT_RETENTION_HOURS * 60 * 60 * 1000).toISOString()
  }

  function safeId(value) {
    return String(value || '').trim()
  }

  function formatDateTime(value) {
    const raw = String(value || '').trim()
    if (!raw) return '-'
    const date = new Date(raw)
    if (Number.isNaN(date.getTime())) return raw
    return date.toLocaleString('id-ID', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  }

  function getLocalDateKey(value) {
    const date = new Date(String(value || '').trim())
    if (Number.isNaN(date.getTime())) return ''
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
  }

  function getLocalMinuteKey(value) {
    const date = new Date(String(value || '').trim())
    if (Number.isNaN(date.getTime())) return ''
    return `${getLocalDateKey(value)} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  }

  function formatDateLabel(value) {
    const date = new Date(String(value || '').trim())
    if (Number.isNaN(date.getTime())) return '-'
    return date.toLocaleDateString('id-ID', {
      weekday: 'long',
      day: '2-digit',
      month: 'long',
      year: 'numeric'
    })
  }

  function formatTimeOnly(value) {
    const date = new Date(String(value || '').trim())
    if (Number.isNaN(date.getTime())) return '-'
    return date.toLocaleTimeString('id-ID', { hour: '2-digit', minute: '2-digit' })
  }

  function getTimestampMs(value) {
    const date = new Date(String(value || '').trim())
    const ms = date.getTime()
    return Number.isNaN(ms) ? null : ms
  }

  function getEmojiOnlyCount(text) {
    const raw = String(text || '').trim()
    if (!raw) return 0
    if (/\s/.test(raw)) return 0
    try {
      const segmenter = (typeof Intl !== 'undefined' && Intl.Segmenter)
        ? new Intl.Segmenter('id', { granularity: 'grapheme' })
        : null
      const chunks = segmenter
        ? Array.from(segmenter.segment(raw), item => item.segment)
        : Array.from(raw)
      if (!chunks.length) return 0
      const emojiRegex = /\p{Extended_Pictographic}/u
      const onlyEmoji = chunks.every(chunk => emojiRegex.test(String(chunk || '').replaceAll('\uFE0F', '')))
      return onlyEmoji ? chunks.length : 0
    } catch (_) {
      return 0
    }
  }

  function stop() {
    const active = window.__chatModuleActiveState
    if (active?.pollHandle) {
      window.clearInterval(active.pollHandle)
    }
    if (active?.refreshTimer) {
      window.clearTimeout(active.refreshTimer)
    }
    if (active?.realtimeChannel && active?.sb) {
      try {
        active.sb.removeChannel(active.realtimeChannel)
      } catch (_) {}
    }
    window.__chatModuleActiveState = null
  }

  async function render(options) {
    stop()
    const sb = options?.sb
    const containerId = String(options?.containerId || '').trim()
    const currentUser = options?.currentUser || {}
    const content = document.getElementById(containerId)
    if (!sb || !content || !safeId(currentUser.id)) return

    const state = {
      sb,
      content,
      containerId,
      currentUser: {
        id: safeId(currentUser.id),
        nama: String(currentUser.nama || currentUser.id_karyawan || '-')
      },
      users: [],
      threads: [],
      members: [],
      messagesByThread: new Map(),
      selectedThreadId: '',
      pollHandle: null,
      draftByThread: new Map(),
      selectedMessageIds: new Set(),
      emojiPickerOpen: false,
      dmModalOpen: false,
      dmDraftUserId: '',
      threadMenuOpenId: '',
      groupModalOpen: false,
      groupDraftName: '',
      groupDraftMembers: new Set(),
      groupMembersScrollTop: 0,
      messageListAtBottom: true,
      forceStickBottom: false,
      realtimeChannel: null,
      refreshTimer: null,
      uiReady: false,
      refreshInFlight: false,
      pendingRefresh: null
      ,
      threadListRenderKey: '',
      lastReadWriteByThread: new Map()
    }

    function ensureChatVisualStyle() {
      if (document.getElementById('chat-thread-glass-style')) return
      const style = document.createElement('style')
      style.id = 'chat-thread-glass-style'
      style.textContent = `
        .chat-thread-wrap {
          position: relative;
          margin-bottom: 6px;
        }
        .chat-thread-btn {
          width: 100%;
          text-align: left;
          border: 1px solid var(--chat-thread-border, #dbe6f5);
          background:
            linear-gradient(135deg, rgba(255,255,255,0.68) 0%, rgba(241,245,249,0.52) 100%);
          backdrop-filter: blur(10px);
          -webkit-backdrop-filter: blur(10px);
          border-radius: 12px;
          padding: 9px 38px 9px 10px;
          cursor: pointer;
          box-shadow: 0 10px 24px rgba(15,23,42,0.08);
          transition: transform .18s ease, box-shadow .2s ease, border-color .2s ease;
        }
        .chat-thread-btn:hover {
          transform: translateY(-1px) scale(1.01);
          box-shadow: 0 14px 30px rgba(15,23,42,0.12);
        }
        .chat-thread-head {
          display: flex;
          align-items: center;
          gap: 8px;
        }
        .chat-thread-avatar {
          width: 28px;
          height: 28px;
          border-radius: 999px;
          border: 1px solid #d8e2ee;
          background: #e2e8f0;
          color: #0f172a;
          overflow: hidden;
          display: inline-flex;
          align-items: center;
          justify-content: center;
          font-size: 11px;
          font-weight: 700;
          flex: 0 0 auto;
        }
        .chat-thread-avatar img {
          width: 100%;
          height: 100%;
          object-fit: cover;
          display: block;
        }
        .chat-thread-text {
          min-width: 0;
          flex: 1;
        }
        .chat-thread-title {
          font-size: 13px;
          font-weight: 600;
          color: #0f172a;
        }
        .chat-thread-preview {
          font-size: 12px;
          color: #64748b;
          margin-top: 2px;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
        .chat-thread-menu-trigger {
          position: absolute;
          top: 8px;
          right: 7px;
          width: 24px;
          height: 24px;
          border: 1px solid #dbe4ef;
          border-radius: 8px;
          background: rgba(255,255,255,0.78);
          backdrop-filter: blur(8px);
          -webkit-backdrop-filter: blur(8px);
          color: #475569;
          cursor: pointer;
          font-size: 14px;
          line-height: 1;
          transition: transform .15s ease, box-shadow .2s ease;
        }
        .chat-thread-menu-trigger:hover {
          transform: scale(1.04);
          box-shadow: 0 8px 20px rgba(15,23,42,0.12);
        }
        .chat-thread-menu {
          position: absolute;
          top: 36px;
          right: 6px;
          min-width: 126px;
          background: rgba(255,255,255,0.92);
          backdrop-filter: blur(12px);
          -webkit-backdrop-filter: blur(12px);
          border: 1px solid #e2e8f0;
          border-radius: 10px;
          box-shadow: 0 12px 28px rgba(15,23,42,0.14);
          z-index: 25;
          padding: 6px;
        }
      `
      document.head.appendChild(style)
    }

    function getThreadMembers(threadId) {
      const tid = safeId(threadId)
      const memberRows = state.members.filter(item => safeId(item.thread_id) === tid)
      return memberRows.map(item => state.users.find(user => safeId(user.id) === safeId(item.karyawan_id))).filter(Boolean)
    }

    function getThreadTitle(thread) {
      if (!thread) return '-'
      if (thread.is_group) return String(thread.title || 'Grup')
      const members = getThreadMembers(thread.id)
      const other = members.find(user => safeId(user.id) !== state.currentUser.id)
      return String(other?.nama || thread.title || 'DM')
    }

    function getThreadPreview(thread) {
      const list = state.messagesByThread.get(safeId(thread?.id)) || []
      const last = list[list.length - 1]
      return String(last?.message_text || '-').trim() || '-'
    }

    function getInitials(name) {
      const parts = String(name || '').trim().split(/\s+/).filter(Boolean)
      if (!parts.length) return '?'
      if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase()
      return `${parts[0][0] || ''}${parts[1][0] || ''}`.toUpperCase()
    }

    function getThreadAvatarInfo(thread) {
      const title = getThreadTitle(thread)
      if (thread?.is_group) {
        return {
          fotoUrl: '',
          initials: getInitials(title),
          label: title
        }
      }
      const members = getThreadMembers(thread?.id)
      const other = members.find(user => safeId(user?.id) !== state.currentUser.id) || members[0] || null
      const fotoUrl = String(other?.foto_url || '').trim()
      const label = String(other?.nama || title || 'DM')
      return {
        fotoUrl,
        initials: getInitials(label),
        label
      }
    }

    function stopPolling() {
      if (!state.pollHandle) return
      window.clearInterval(state.pollHandle)
      state.pollHandle = null
    }

    function scheduleRefresh(keepSelection = true) {
      if (state.refreshTimer) {
        window.clearTimeout(state.refreshTimer)
      }
      state.refreshTimer = window.setTimeout(() => {
        state.refreshTimer = null
        refresh(keepSelection).catch(error => console.error('Chat realtime refresh error:', error))
      }, 250)
    }

    function stopRealtime() {
      if (!state.realtimeChannel) return
      try {
        sb.removeChannel(state.realtimeChannel)
      } catch (_) {}
      state.realtimeChannel = null
    }

    function startRealtime() {
      stopRealtime()
      const channel = sb.channel(`chat-live-${state.currentUser.id}-${Date.now()}`)
      channel
        .on('postgres_changes', { event: '*', schema: 'public', table: CHAT_MESSAGES_TABLE }, () => {
          scheduleRefresh(true)
        })
        .on('postgres_changes', { event: '*', schema: 'public', table: CHAT_THREADS_TABLE }, () => {
          scheduleRefresh(true)
        })
        .on('postgres_changes', { event: '*', schema: 'public', table: CHAT_MEMBERS_TABLE }, () => {
          scheduleRefresh(true)
        })
        .subscribe()
      state.realtimeChannel = channel
    }

    async function cleanupOldMessagesBestEffort() {
      try {
        await sb
          .from(CHAT_MESSAGES_TABLE)
          .delete()
          .lt('created_at', getCutoffIso())
      } catch (_) {
        // Ignore: retention should ideally be handled by DB scheduled job.
      }
    }

    async function loadUsers() {
      const { data, error } = await sb
        .from('karyawan')
        .select('id, id_karyawan, nama, aktif, role, foto_url')
        .eq('aktif', true)
        .order('nama')
      if (error) throw error
      state.users = data || []
    }

    async function loadThreadsAndMessages() {
      const cutoffIso = getCutoffIso()
      const myId = state.currentUser.id

      let membersRes = await sb
        .from(CHAT_MEMBERS_TABLE)
        .select('thread_id, karyawan_id, joined_at, last_read_at')
        .eq('karyawan_id', myId)
      if (membersRes.error && String(membersRes.error?.message || '').toLowerCase().includes('last_read_at')) {
        membersRes = await sb
          .from(CHAT_MEMBERS_TABLE)
          .select('thread_id, karyawan_id, joined_at')
          .eq('karyawan_id', myId)
        if (!membersRes.error) {
          membersRes.data = (membersRes.data || []).map(item => ({ ...item, last_read_at: null }))
        }
      }
      if (membersRes.error) throw membersRes.error
      const myThreadIds = [...new Set((membersRes.data || []).map(item => safeId(item.thread_id)).filter(Boolean))]
      if (!myThreadIds.length) {
        state.threads = []
        state.members = []
        state.messagesByThread = new Map()
        return
      }

      const [threadsRes, allMembersResRaw, messagesRes] = await Promise.all([
        sb
          .from(CHAT_THREADS_TABLE)
          .select('id, title, is_group, created_by, created_at, last_message_at')
          .in('id', myThreadIds)
          .order('last_message_at', { ascending: false, nullsFirst: false }),
        (async () => {
          let res = await sb
            .from(CHAT_MEMBERS_TABLE)
            .select('thread_id, karyawan_id, joined_at, last_read_at')
            .in('thread_id', myThreadIds)
          if (res.error && String(res.error?.message || '').toLowerCase().includes('last_read_at')) {
            res = await sb
              .from(CHAT_MEMBERS_TABLE)
              .select('thread_id, karyawan_id, joined_at')
              .in('thread_id', myThreadIds)
            if (!res.error) {
              res.data = (res.data || []).map(item => ({ ...item, last_read_at: null }))
            }
          }
          return res
        })(),
        sb
          .from(CHAT_MESSAGES_TABLE)
          .select('id, thread_id, sender_id, message_text, created_at')
          .in('thread_id', myThreadIds)
          .gte('created_at', cutoffIso)
          .order('created_at', { ascending: true })
      ])

      if (threadsRes.error) throw threadsRes.error
      if (allMembersResRaw.error) throw allMembersResRaw.error
      if (messagesRes.error) throw messagesRes.error

      state.threads = threadsRes.data || []
      state.members = allMembersResRaw.data || []
      state.messagesByThread = new Map()
      ;(messagesRes.data || []).forEach(item => {
        const key = safeId(item.thread_id)
        if (!state.messagesByThread.has(key)) state.messagesByThread.set(key, [])
        state.messagesByThread.get(key).push(item)
      })
    }

    async function createDm(otherUserId) {
      const myId = state.currentUser.id
      const otherId = safeId(otherUserId)
      if (!otherId || otherId === myId) {
        alert('Pilih guru/karyawan lain untuk DM.')
        return
      }

      const existing = state.threads.find(thread => {
        if (thread.is_group) return false
        const memberIds = getThreadMembers(thread.id).map(item => safeId(item.id))
        return memberIds.includes(myId) && memberIds.includes(otherId) && memberIds.length === 2
      })
      if (existing) {
        state.selectedThreadId = safeId(existing.id)
        renderUI()
        return
      }

      const { data: thread, error: threadError } = await sb
        .from(CHAT_THREADS_TABLE)
        .insert([{ title: null, is_group: false, created_by: myId, last_message_at: new Date().toISOString() }])
        .select('id')
        .single()
      if (threadError) {
        alert(`Gagal membuat DM: ${threadError.message || 'Unknown error'}`)
        return
      }

      const threadId = safeId(thread?.id)
      const { error: memberError } = await sb
        .from(CHAT_MEMBERS_TABLE)
        .insert([
          { thread_id: threadId, karyawan_id: myId },
          { thread_id: threadId, karyawan_id: otherId }
        ])
      if (memberError) {
        alert(`Gagal menambahkan anggota DM: ${memberError.message || 'Unknown error'}`)
        return
      }

      await refresh(true, threadId)
    }

    async function createDmFromModal() {
      const userSelect = document.getElementById('chat-dm-user-modal')
      const otherUserId = safeId(userSelect?.value)
      if (!otherUserId) {
        alert('Pilih karyawan terlebih dahulu.')
        return
      }
      await createDm(otherUserId)
      state.dmDraftUserId = ''
      closeDmModal()
    }

    function openDmModal() {
      const overlay = document.getElementById('chat-dm-overlay')
      state.dmModalOpen = true
      if (overlay) overlay.style.display = 'flex'
    }

    function closeDmModal() {
      const overlay = document.getElementById('chat-dm-overlay')
      state.dmModalOpen = false
      if (overlay) overlay.style.display = 'none'
    }

    async function createGroupFromModal() {
      const nameInput = document.getElementById('chat-group-name-modal')
      const memberSelect = document.getElementById('chat-group-members-modal')
      const name = String(nameInput?.value || '').trim()
      if (!name) {
        alert('Nama grup wajib diisi.')
        return
      }
      const chosen = Array.from(memberSelect?.selectedOptions || []).map(opt => safeId(opt.value)).filter(Boolean)
      const unique = [...new Set([state.currentUser.id, ...chosen])]
      if (unique.length < 2) {
        alert('Pilih minimal 1 anggota lain.')
        return
      }

      const { data: thread, error: threadError } = await sb
        .from(CHAT_THREADS_TABLE)
        .insert([{ title: name, is_group: true, created_by: state.currentUser.id, last_message_at: new Date().toISOString() }])
        .select('id')
        .single()
      if (threadError) {
        alert(`Gagal membuat grup: ${threadError.message || 'Unknown error'}`)
        return
      }
      const threadId = safeId(thread?.id)
      const payload = unique.map(karyawanId => ({ thread_id: threadId, karyawan_id: karyawanId }))
      const { error: memberError } = await sb
        .from(CHAT_MEMBERS_TABLE)
        .insert(payload)
      if (memberError) {
        alert(`Gagal menambahkan anggota grup: ${memberError.message || 'Unknown error'}`)
        return
      }

      if (nameInput) nameInput.value = ''
      if (memberSelect) Array.from(memberSelect.options || []).forEach(option => { option.selected = false })
      state.groupDraftName = ''
      state.groupDraftMembers.clear()
      closeGroupModal()
      await refresh(true, threadId)
    }

    function openGroupModal() {
      const overlay = document.getElementById('chat-group-overlay')
      state.groupModalOpen = true
      if (overlay) overlay.style.display = 'flex'
    }

    function closeGroupModal() {
      const overlay = document.getElementById('chat-group-overlay')
      state.groupModalOpen = false
      if (overlay) overlay.style.display = 'none'
    }

    function closeEmojiPicker() {
      const picker = document.getElementById('chat-emoji-picker')
      if (picker) picker.style.display = 'none'
      state.emojiPickerOpen = false
    }

    function toggleEmojiPicker() {
      const picker = document.getElementById('chat-emoji-picker')
      if (!picker) return
      const willOpen = picker.style.display === 'none' || !picker.style.display
      picker.style.display = willOpen ? 'block' : 'none'
      state.emojiPickerOpen = willOpen
    }

    function insertEmojiToInput(emoji) {
      const input = document.getElementById('chat-message-input')
      if (!input) return
      const raw = String(input.value || '')
      const start = Number.isFinite(input.selectionStart) ? input.selectionStart : raw.length
      const end = Number.isFinite(input.selectionEnd) ? input.selectionEnd : raw.length
      const next = `${raw.slice(0, start)}${emoji}${raw.slice(end)}`
      input.value = next
      const nextPos = start + String(emoji).length
      input.focus()
      input.setSelectionRange(nextPos, nextPos)
      state.draftByThread.set(safeId(state.selectedThreadId), next)
    }

    async function sendMessage() {
      const threadId = safeId(state.selectedThreadId)
      const textEl = document.getElementById('chat-message-input')
      const messageText = String(textEl?.value || '').trim()
      if (!threadId) {
        alert('Pilih chat terlebih dahulu.')
        return
      }
      if (!messageText) return

      const { error } = await sb
        .from(CHAT_MESSAGES_TABLE)
        .insert([{
          thread_id: threadId,
          sender_id: state.currentUser.id,
          message_text: messageText
        }])
      if (error) {
        alert(`Gagal kirim pesan: ${error.message || 'Unknown error'}`)
        return
      }

      await sb
        .from(CHAT_THREADS_TABLE)
        .update({ last_message_at: new Date().toISOString() })
        .eq('id', threadId)

      if (textEl) textEl.value = ''
      state.draftByThread.set(threadId, '')
      state.forceStickBottom = true
      await refresh(false, threadId)
    }

    async function markThreadAsRead(threadId) {
      const tid = safeId(threadId)
      if (!tid) return
      const now = Date.now()
      const lastWrite = Number(state.lastReadWriteByThread.get(tid) || 0)
      if (now - lastWrite < 5000) return
      state.lastReadWriteByThread.set(tid, now)

      const nowIso = new Date().toISOString()
      try {
        const { error } = await sb
          .from(CHAT_MEMBERS_TABLE)
          .update({ last_read_at: nowIso })
          .eq('thread_id', tid)
          .eq('karyawan_id', state.currentUser.id)
        if (error) {
          const msg = String(error?.message || '').toLowerCase()
          if (!msg.includes('last_read_at')) {
            console.warn('Gagal update status baca chat:', error)
          }
          return
        }

        state.members = (state.members || []).map(item => {
          if (safeId(item?.thread_id) !== tid) return item
          if (safeId(item?.karyawan_id) !== state.currentUser.id) return item
          return { ...item, last_read_at: nowIso }
        })
      } catch (error) {
        console.warn('Gagal sinkron status baca chat:', error)
      }
    }

    function openDeleteConfirmModal() {
      const overlay = document.getElementById('chat-delete-confirm-overlay')
      const countEl = document.getElementById('chat-delete-selected-count')
      if (countEl) countEl.textContent = String(state.selectedMessageIds.size)
      if (overlay) overlay.style.display = 'flex'
    }

    function closeDeleteConfirmModal() {
      const overlay = document.getElementById('chat-delete-confirm-overlay')
      if (overlay) overlay.style.display = 'none'
    }

    async function deleteThread(threadId) {
      const tid = safeId(threadId)
      if (!tid) return
      const thread = state.threads.find(item => safeId(item.id) === tid)
      const title = getThreadTitle(thread)
      const yes = window.showPopupConfirm
        ? await window.showPopupConfirm(`Hapus thread "${title}"?\nPercakapan di thread ini akan terhapus.`)
        : confirm(`Hapus thread "${title}"?\nPercakapan di thread ini akan terhapus.`)
      if (!yes) return

      const { error } = await sb
        .from(CHAT_THREADS_TABLE)
        .delete()
        .eq('id', tid)
      if (error) {
        alert(`Gagal hapus thread: ${error.message || 'Unknown error'}`)
        return
      }

      state.draftByThread.delete(tid)
      state.messagesByThread.delete(tid)
      state.selectedMessageIds.clear()
      if (safeId(state.selectedThreadId) === tid) {
        state.selectedThreadId = ''
      }
      await refresh(true)
    }

    async function deleteSelectedMessagesConfirmed() {
      const ids = [...state.selectedMessageIds].map(item => String(item || '').trim()).filter(Boolean)
      if (!ids.length) {
        closeDeleteConfirmModal()
        return
      }
      const { error } = await sb
        .from(CHAT_MESSAGES_TABLE)
        .delete()
        .in('id', ids)
        .eq('sender_id', state.currentUser.id)
        .eq('thread_id', safeId(state.selectedThreadId))
      if (error) {
        alert(`Gagal hapus pesan: ${error.message || 'Unknown error'}`)
        return
      }
      state.selectedMessageIds.clear()
      closeDeleteConfirmModal()
      await refresh(true, state.selectedThreadId)
    }

    function getSelectedThread() {
      return state.threads.find(item => safeId(item.id) === safeId(state.selectedThreadId)) || null
    }

    function getMyMessageReadStatus(message) {
      if (safeId(message?.sender_id) !== state.currentUser.id) return ''
      const threadId = safeId(message?.thread_id)
      const recipients = (state.members || []).filter(item => (
        safeId(item?.thread_id) === threadId &&
        safeId(item?.karyawan_id) !== state.currentUser.id
      ))
      if (!recipients.length) return 'Terkirim'

      const msgTime = getTimestampMs(message?.created_at)
      if (msgTime === null) return 'Terkirim'

      let readCount = 0
      let hasReadSignal = false
      recipients.forEach(member => {
        const readMs = getTimestampMs(member?.last_read_at)
        if (readMs !== null) {
          hasReadSignal = true
          if (readMs >= msgTime) readCount += 1
        }
      })

      if (readCount === recipients.length && recipients.length > 0) return 'Sudah dibaca'
      if (!hasReadSignal && (Date.now() - msgTime) < 15000) return 'Terkirim'
      return 'Belum dibaca'
    }

    function renderMessagesPanel() {
      const box = document.getElementById('chat-message-list')
      if (!box) return
      const prevScrollTop = Number(box.scrollTop || 0)
      const prevScrollHeight = Number(box.scrollHeight || 0)
      const distanceFromBottom = prevScrollHeight - (prevScrollTop + Number(box.clientHeight || 0))
      const wasNearBottom = state.forceStickBottom || distanceFromBottom <= 28
      const thread = getSelectedThread()
      if (!thread) {
        box.innerHTML = '<div style="color:#64748b;">Pilih thread untuk mulai chat.</div>'
        return
      }
      markThreadAsRead(thread.id)
      const list = state.messagesByThread.get(safeId(thread.id)) || []
      const selectionBar = document.getElementById('chat-selection-bar')
      const selectionText = document.getElementById('chat-selection-text')
      if (selectionBar && selectionText) {
        const count = state.selectedMessageIds.size
        selectionBar.style.display = count > 0 ? 'flex' : 'none'
        selectionText.textContent = `${count} pesan dipilih`
      }
      if (!list.length) {
        box.innerHTML = '<div style="color:#64748b;">Belum ada pesan.</div>'
        return
      }
      let prevDateKey = ''
      let prevSenderId = ''
      let prevMinuteStamp = null
      const rowsHtml = []

      list.forEach(item => {
        const senderId = safeId(item.sender_id)
        const mine = senderId === state.currentUser.id
        const sender = state.users.find(user => safeId(user.id) === senderId)
        const msgId = String(item.id || '')
        const selected = state.selectedMessageIds.has(msgId)
        const emojiOnlyCount = getEmojiOnlyCount(item.message_text)
        const isStickerEmoji = emojiOnlyCount === 1
        const mineStatus = getMyMessageReadStatus(item)
        const dateKey = getLocalDateKey(item.created_at)
        const ts = getTimestampMs(item.created_at)
        const minuteStamp = ts === null ? null : Math.floor(ts / 60000)
        const showSender = !(senderId === prevSenderId && minuteStamp !== null && minuteStamp === prevMinuteStamp)

        if (dateKey && dateKey !== prevDateKey) {
          rowsHtml.push(`
            <div style="display:flex; justify-content:center; margin:6px 0 10px;">
              <span style="font-size:11px; color:#64748b; background:#f8fafc; border:1px solid #e2e8f0; border-radius:999px; padding:4px 10px;">${escapeHtml(formatDateLabel(item.created_at))}</span>
            </div>
          `)
          prevDateKey = dateKey
          prevSenderId = ''
          prevMinuteStamp = null
        }

        rowsHtml.push(`
          <div style="display:flex; justify-content:${mine ? 'flex-end' : 'flex-start'}; margin-bottom:8px;" data-chat-row-id="${escapeHtml(msgId)}">
            <div data-chat-own-click="${mine ? '1' : '0'}" data-chat-message-id="${escapeHtml(msgId)}" style="max-width:75%; background:${mine ? (selected ? '#bbf7d0' : '#dcfce7') : '#f1f5f9'}; border:1px solid ${selected ? '#16a34a' : '#e2e8f0'}; border-radius:10px; padding:8px 10px; ${mine ? 'cursor:pointer;' : ''}">
              ${showSender ? `<div style="font-size:11px; color:#64748b; margin-bottom:2px;">${escapeHtml(String(sender?.nama || 'User'))}</div>` : ''}
              <div style="${isStickerEmoji ? 'font-size:52px; line-height:1.05; text-align:center; padding:6px 0;' : 'font-size:13px; color:#0f172a; white-space:pre-wrap;'}">${escapeHtml(item.message_text || '')}</div>
              <div style="font-size:10px; color:#64748b; margin-top:4px; text-align:right;">
                <span>${escapeHtml(formatTimeOnly(item.created_at))}</span>
                ${mine ? `<span style="margin-left:6px; color:${mineStatus === 'Sudah dibaca' ? '#16a34a' : '#64748b'};">${escapeHtml(mineStatus || 'Terkirim')}</span>` : ''}
              </div>
            </div>
          </div>
        `)

        prevSenderId = senderId
        prevMinuteStamp = minuteStamp
      })
      box.innerHTML = rowsHtml.join('')
      Array.from(box.querySelectorAll('[data-chat-own-click="1"]')).forEach(el => {
        el.addEventListener('click', event => {
          const targetId = String(el.getAttribute('data-chat-message-id') || '')
          if (!targetId) return
          if (state.selectedMessageIds.has(targetId)) state.selectedMessageIds.delete(targetId)
          else state.selectedMessageIds.add(targetId)
          renderMessagesPanel()
          event.stopPropagation()
        })
      })
      box.onscroll = () => {
        const remain = Number(box.scrollHeight || 0) - (Number(box.scrollTop || 0) + Number(box.clientHeight || 0))
        state.messageListAtBottom = remain <= 28
      }
      if (wasNearBottom) {
        box.scrollTop = box.scrollHeight
        state.messageListAtBottom = true
      } else {
        box.scrollTop = prevScrollTop
        state.messageListAtBottom = false
      }
      state.forceStickBottom = false
    }

    function renderThreadList() {
      const box = document.getElementById('chat-thread-list')
      if (!box) return
      ensureChatVisualStyle()
      const selectedId = safeId(state.selectedThreadId)
      const renderKey = JSON.stringify({
        selectedId,
        menu: safeId(state.threadMenuOpenId),
        threads: state.threads.map(thread => ({
          id: safeId(thread.id),
          title: getThreadTitle(thread),
          preview: getThreadPreview(thread)
        }))
      })
      if (renderKey === state.threadListRenderKey) return
      state.threadListRenderKey = renderKey
      if (!state.threads.length) {
        box.innerHTML = '<div style="color:#64748b; font-size:13px;">Belum ada thread chat.</div>'
        return
      }
      box.innerHTML = state.threads.map(thread => {
        const tid = safeId(thread.id)
        const active = tid === selectedId
        const menuOpen = tid === safeId(state.threadMenuOpenId)
        const preview = getThreadPreview(thread)
        const title = getThreadTitle(thread)
        const avatar = getThreadAvatarInfo(thread)
        return `
          <div class="chat-thread-wrap">
            <button type="button" class="chat-thread-btn" data-chat-thread-id="${escapeHtml(tid)}" style="--chat-thread-border:${active ? '#86efac' : '#dbe4ef'};">
              <div class="chat-thread-head">
                <span class="chat-thread-avatar" title="${escapeHtml(avatar.label)}">
                  ${avatar.fotoUrl ? `<img src="${escapeHtml(avatar.fotoUrl)}" alt="${escapeHtml(avatar.label)}">` : escapeHtml(avatar.initials)}
                </span>
                <div class="chat-thread-text">
                  <div class="chat-thread-title">${escapeHtml(title)}</div>
                  <div class="chat-thread-preview">${escapeHtml(preview)}</div>
                </div>
              </div>
            </button>
            <button type="button" class="chat-thread-menu-trigger" data-chat-thread-menu-trigger="${escapeHtml(tid)}" title="Aksi thread">&#8942;</button>
            <div class="chat-thread-menu" data-chat-thread-menu="${escapeHtml(tid)}" style="display:${menuOpen ? 'block' : 'none'};">
              <button type="button" data-chat-thread-delete="${escapeHtml(tid)}" style="width:100%; border:none; background:transparent; text-align:left; padding:7px 8px; border-radius:8px; color:#dc2626; cursor:pointer; font-size:12px; font-weight:600;">Hapus Chat</button>
            </div>
          </div>
        `
      }).join('')
      Array.from(box.querySelectorAll('[data-chat-thread-id]')).forEach(btn => {
        btn.addEventListener('click', event => {
          const tid = safeId(btn.getAttribute('data-chat-thread-id'))
          state.threadMenuOpenId = ''
          state.selectedThreadId = tid
          state.selectedMessageIds.clear()
          state.forceStickBottom = true
          renderDynamicUI({ threadChanged: true })
        })
      })
      Array.from(box.querySelectorAll('[data-chat-thread-menu-trigger]')).forEach(btn => {
        btn.addEventListener('click', event => {
          event.preventDefault()
          event.stopPropagation()
          const tid = safeId(btn.getAttribute('data-chat-thread-menu-trigger'))
          state.threadMenuOpenId = state.threadMenuOpenId === tid ? '' : tid
          renderThreadList()
          setTimeout(() => {
            document.addEventListener('click', () => {
              if (!state.threadMenuOpenId) return
              state.threadMenuOpenId = ''
              renderThreadList()
            }, { once: true })
          }, 0)
        })
      })
      Array.from(box.querySelectorAll('[data-chat-thread-delete]')).forEach(btn => {
        btn.addEventListener('click', async event => {
          event.preventDefault()
          event.stopPropagation()
          const tid = safeId(btn.getAttribute('data-chat-thread-delete'))
          state.threadMenuOpenId = ''
          await deleteThread(tid)
        })
      })
    }

    function syncComposerForSelectedThread(threadChanged = false) {
      const input = document.getElementById('chat-message-input')
      if (!input) return
      const isTypingHere = document.activeElement === input
      if (isTypingHere && !threadChanged) return
      input.value = state.draftByThread.get(safeId(state.selectedThreadId)) || ''
    }

    function renderDynamicUI(options = {}) {
      const threadChanged = Boolean(options?.threadChanged)
      const titleEl = document.getElementById('chat-thread-title')
      const selected = getSelectedThread()
      if (titleEl) titleEl.textContent = getThreadTitle(selected)
      renderThreadList()
      renderMessagesPanel()
      syncComposerForSelectedThread(threadChanged)
    }

    function renderUI() {
      const oldInput = document.getElementById('chat-message-input')
      if (oldInput && state.selectedThreadId) {
        state.draftByThread.set(safeId(state.selectedThreadId), String(oldInput.value || ''))
      }
      const oldGroupName = document.getElementById('chat-group-name-modal')
      const oldGroupMembers = document.getElementById('chat-group-members-modal')
      if (oldGroupName) state.groupDraftName = String(oldGroupName.value || '')
      if (oldGroupMembers) {
        state.groupMembersScrollTop = Number(oldGroupMembers.scrollTop || 0)
        state.groupDraftMembers = new Set(
          Array.from(oldGroupMembers.selectedOptions || [])
            .map(opt => safeId(opt.value))
            .filter(Boolean)
        )
      }
      const otherUsers = state.users.filter(user => safeId(user.id) !== state.currentUser.id)
      const dmOptions = ['<option value="">- Pilih karyawan -</option>']
      otherUsers.forEach(user => dmOptions.push(`<option value="${escapeHtml(safeId(user.id))}">${escapeHtml(String(user.nama || user.id_karyawan || '-'))}</option>`))
      const selected = getSelectedThread()
      const selectedCount = state.selectedMessageIds.size

      content.innerHTML = `
        <div style="display:grid; grid-template-columns: minmax(150px, 320px) minmax(560px, 1fr); gap:12px; height:calc(100vh - 220px); min-height:460px; overflow:auto;">
          <div style="border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:10px; min-height:0; display:flex; flex-direction:column; overflow:hidden;">
            <div style="display:grid; gap:8px; margin-bottom:10px;">
              <div style="display:flex; gap:8px;">
                <button type="button" class="modal-btn" id="chat-btn-open-dm-modal" title="Tambah DM" style="min-width:42px; padding:8px 12px; font-weight:700; font-size:18px; line-height:1;">+</button>
                <button type="button" class="modal-btn" id="chat-btn-open-group-modal">Buat Grup</button>
              </div>
            </div>
            <div style="font-size:12px; color:#64748b; margin-bottom:6px;">Thread</div>
            <div id="chat-thread-list" style="flex:1; min-height:0; overflow:auto; padding-right:2px;"></div>
          </div>
          <div style="border:1px solid #e2e8f0; border-radius:12px; background:#fff; min-height:0; display:flex; flex-direction:column; overflow:hidden;">
            <div id="chat-thread-title" style="padding:10px 12px; border-bottom:1px solid #e2e8f0; font-weight:700; color:#0f172a;">${escapeHtml(getThreadTitle(selected))}</div>
            <div id="chat-selection-bar" style="display:${selectedCount > 0 ? 'flex' : 'none'}; align-items:center; justify-content:space-between; gap:8px; padding:8px 12px; border-bottom:1px solid #e2e8f0; background:#f8fafc;">
              <div id="chat-selection-text" style="font-size:12px; color:#334155;">${selectedCount} pesan dipilih</div>
              <div style="display:flex; gap:8px;">
                <button type="button" class="modal-btn" id="chat-btn-clear-selection">Batal Pilih</button>
                <button type="button" class="modal-btn" style="background:#dc2626; color:#fff;" id="chat-btn-delete-selected">Hapus Terpilih</button>
              </div>
            </div>
            <div id="chat-message-list" style="flex:1; min-height:0; overflow:auto; padding:10px;"></div>
            <div style="padding:10px; border-top:1px solid #e2e8f0; display:flex; gap:8px; align-items:center; position:relative;">
              <button type="button" class="modal-btn" id="chat-btn-emoji" style="min-width:42px; padding:8px 10px;">😊</button>
              <input id="chat-message-input" class="guru-field" type="text" placeholder="Ketik pesan..." style="flex:1;">
              <button type="button" class="modal-btn modal-btn-primary" id="chat-btn-send">Kirim</button>
              <div id="chat-emoji-picker" style="display:none; position:absolute; left:10px; bottom:56px; width:min(360px, 90vw); max-height:340px; overflow:hidden; background:#fff; border:1px solid #e2e8f0; border-radius:10px; padding:0; box-shadow:0 8px 24px rgba(15,23,42,0.12); z-index:20;">
                <emoji-picker id="chat-emoji-picker-el" style="width:100%; height:320px;"></emoji-picker>
                <div id="chat-emoji-fallback" style="display:none; padding:8px; max-height:180px; overflow:auto;">
                  <div style="display:grid; grid-template-columns:repeat(8, 1fr); gap:6px;">
                    ${CHAT_EMOJIS.map(item => `<button type="button" data-chat-emoji="${escapeHtml(item)}" style="border:1px solid #e2e8f0; background:#fff; border-radius:8px; height:32px; cursor:pointer; font-size:18px;">${item}</button>`).join('')}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div id="chat-dm-overlay" style="display:none; position:fixed; inset:0; background:rgba(15,23,42,0.45); z-index:3450; align-items:center; justify-content:center; padding:16px;">
          <div style="width:min(420px, 96vw); background:#fff; border:1px solid #e2e8f0; border-radius:12px; overflow:hidden;">
            <div style="display:flex; align-items:center; justify-content:space-between; padding:12px; border-bottom:1px solid #e2e8f0;">
              <div style="font-weight:700;">Tambah DM</div>
              <button type="button" class="modal-btn" id="chat-btn-close-dm-modal">Tutup</button>
            </div>
            <div style="padding:12px; display:grid; gap:8px;">
              <select id="chat-dm-user-modal" class="guru-field">${dmOptions.join('')}</select>
              <div style="display:flex; justify-content:flex-end;">
                <button type="button" class="modal-btn modal-btn-primary" id="chat-btn-create-dm-modal">Mulai Chat</button>
              </div>
            </div>
          </div>
        </div>
        <div id="chat-group-overlay" style="display:none; position:fixed; inset:0; background:rgba(15,23,42,0.45); z-index:3500; align-items:center; justify-content:center; padding:16px;">
          <div style="width:min(520px, 96vw); background:#fff; border:1px solid #e2e8f0; border-radius:12px; overflow:hidden;">
            <div style="display:flex; align-items:center; justify-content:space-between; padding:12px; border-bottom:1px solid #e2e8f0;">
              <div style="font-weight:700;">Buat Grup Chat</div>
              <button type="button" class="modal-btn" id="chat-btn-close-group-modal">Tutup</button>
            </div>
            <div style="padding:12px; display:grid; gap:8px;">
              <input id="chat-group-name-modal" class="guru-field" type="text" placeholder="Nama grup">
              <select id="chat-group-members-modal" class="guru-field" multiple style="min-height:140px;">
                ${otherUsers.map(user => `<option value="${escapeHtml(safeId(user.id))}">${escapeHtml(String(user.nama || user.id_karyawan || '-'))}</option>`).join('')}
              </select>
              <div style="font-size:12px; color:#64748b;">Pilih anggota dengan Ctrl/Command + Klik.</div>
              <div style="display:flex; justify-content:flex-end;">
                <button type="button" class="modal-btn modal-btn-primary" id="chat-btn-create-group">Buat Grup</button>
              </div>
            </div>
          </div>
        </div>
        <div id="chat-delete-confirm-overlay" style="display:none; position:fixed; inset:0; background:rgba(15,23,42,0.45); z-index:3600; align-items:center; justify-content:center; padding:16px;">
          <div style="width:min(420px, 96vw); background:#fff; border:1px solid #e2e8f0; border-radius:12px; overflow:hidden;">
            <div style="padding:12px; border-bottom:1px solid #e2e8f0; font-weight:700;">Konfirmasi Hapus Pesan</div>
            <div style="padding:12px; font-size:13px; color:#334155;">
              Hapus <span id="chat-delete-selected-count">0</span> pesan terpilih?
            </div>
            <div style="padding:0 12px 12px; display:flex; justify-content:flex-end; gap:8px;">
              <button type="button" class="modal-btn" id="chat-btn-cancel-delete">Batal</button>
              <button type="button" class="modal-btn" style="background:#dc2626; color:#fff;" id="chat-btn-confirm-delete">Hapus</button>
            </div>
          </div>
        </div>
      `

      renderThreadList()
      renderMessagesPanel()

      const openDmBtn = document.getElementById('chat-btn-open-dm-modal')
      const closeDmBtn = document.getElementById('chat-btn-close-dm-modal')
      const createDmBtn = document.getElementById('chat-btn-create-dm-modal')
      const dmOverlay = document.getElementById('chat-dm-overlay')
      const dmUserSelect = document.getElementById('chat-dm-user-modal')
      if (dmOverlay) dmOverlay.style.display = state.dmModalOpen ? 'flex' : 'none'
      if (dmUserSelect) dmUserSelect.value = state.dmDraftUserId || ''
      if (openDmBtn) openDmBtn.addEventListener('click', openDmModal)
      if (closeDmBtn) closeDmBtn.addEventListener('click', closeDmModal)
      if (createDmBtn) createDmBtn.addEventListener('click', createDmFromModal)
      if (dmUserSelect) {
        dmUserSelect.addEventListener('change', () => {
          state.dmDraftUserId = safeId(dmUserSelect.value)
        })
      }
      if (dmOverlay) {
        dmOverlay.addEventListener('click', event => {
          if (event.target !== dmOverlay) return
          closeDmModal()
        })
      }

      const groupBtn = document.getElementById('chat-btn-create-group')
      const openGroupBtn = document.getElementById('chat-btn-open-group-modal')
      const closeGroupBtn = document.getElementById('chat-btn-close-group-modal')
      const groupOverlay = document.getElementById('chat-group-overlay')
      const groupNameInput = document.getElementById('chat-group-name-modal')
      const groupMembersInput = document.getElementById('chat-group-members-modal')
      if (groupOverlay) groupOverlay.style.display = state.groupModalOpen ? 'flex' : 'none'
      if (groupNameInput) {
        groupNameInput.value = state.groupDraftName || ''
        groupNameInput.addEventListener('input', () => {
          state.groupDraftName = String(groupNameInput.value || '')
        })
      }
      if (groupMembersInput) {
        Array.from(groupMembersInput.options || []).forEach(option => {
          option.selected = state.groupDraftMembers.has(safeId(option.value))
        })
        groupMembersInput.scrollTop = Number(state.groupMembersScrollTop || 0)
        groupMembersInput.addEventListener('change', () => {
          state.groupDraftMembers = new Set(
            Array.from(groupMembersInput.selectedOptions || [])
              .map(opt => safeId(opt.value))
              .filter(Boolean)
          )
        })
        groupMembersInput.addEventListener('scroll', () => {
          state.groupMembersScrollTop = Number(groupMembersInput.scrollTop || 0)
        })
      }
      if (openGroupBtn) openGroupBtn.addEventListener('click', openGroupModal)
      if (closeGroupBtn) closeGroupBtn.addEventListener('click', closeGroupModal)
      if (groupOverlay) {
        groupOverlay.addEventListener('click', event => {
          if (event.target !== groupOverlay) return
          closeGroupModal()
        })
      }
      if (groupBtn) {
        groupBtn.addEventListener('click', async () => {
          await createGroupFromModal()
        })
      }

      const sendBtn = document.getElementById('chat-btn-send')
      const input = document.getElementById('chat-message-input')
      const emojiBtn = document.getElementById('chat-btn-emoji')
      const emojiPicker = document.getElementById('chat-emoji-picker')
      const emojiPickerEl = document.getElementById('chat-emoji-picker-el')
      const emojiFallback = document.getElementById('chat-emoji-fallback')
      const clearSelectionBtn = document.getElementById('chat-btn-clear-selection')
      const deleteSelectedBtn = document.getElementById('chat-btn-delete-selected')
      const cancelDeleteBtn = document.getElementById('chat-btn-cancel-delete')
      const confirmDeleteBtn = document.getElementById('chat-btn-confirm-delete')
      const deleteOverlay = document.getElementById('chat-delete-confirm-overlay')
      if (emojiPicker) emojiPicker.style.display = state.emojiPickerOpen ? 'block' : 'none'
      if (sendBtn) sendBtn.addEventListener('click', sendMessage)
      if (emojiBtn) {
        emojiBtn.addEventListener('click', event => {
          event.stopPropagation()
          toggleEmojiPicker()
        })
      }
      if (emojiPickerEl) {
        if (typeof customElements !== 'undefined' && customElements.get('emoji-picker')) {
          emojiPickerEl.style.display = 'block'
          if (emojiFallback) emojiFallback.style.display = 'none'
        } else {
          emojiPickerEl.style.display = 'none'
          if (emojiFallback) emojiFallback.style.display = 'block'
        }
        emojiPickerEl.addEventListener('emoji-click', event => {
          const emoji = String(event?.detail?.unicode || '').trim()
          if (!emoji) return
          insertEmojiToInput(emoji)
        })
      }
      if (emojiFallback) {
        Array.from(emojiFallback.querySelectorAll('[data-chat-emoji]')).forEach(btn => {
          btn.addEventListener('click', event => {
            event.stopPropagation()
            const emoji = String(btn.getAttribute('data-chat-emoji') || '')
            if (!emoji) return
            insertEmojiToInput(emoji)
          })
        })
      }
      if (clearSelectionBtn) {
        clearSelectionBtn.addEventListener('click', () => {
          state.selectedMessageIds.clear()
          renderMessagesPanel()
        })
      }
      if (deleteSelectedBtn) {
        deleteSelectedBtn.addEventListener('click', () => {
          openDeleteConfirmModal()
        })
      }
      if (cancelDeleteBtn) cancelDeleteBtn.addEventListener('click', closeDeleteConfirmModal)
      if (confirmDeleteBtn) confirmDeleteBtn.addEventListener('click', deleteSelectedMessagesConfirmed)
      if (deleteOverlay) {
        deleteOverlay.addEventListener('click', event => {
          if (event.target !== deleteOverlay) return
          closeDeleteConfirmModal()
        })
      }
      if (input) {
        input.value = state.draftByThread.get(safeId(state.selectedThreadId)) || ''
        input.addEventListener('input', () => {
          state.draftByThread.set(safeId(state.selectedThreadId), String(input.value || ''))
        })
        input.addEventListener('keydown', async event => {
          if (event.key !== 'Enter' || event.shiftKey) return
          event.preventDefault()
          await sendMessage()
        })
      }
      document.addEventListener('click', event => {
        const picker = document.getElementById('chat-emoji-picker')
        const trigger = document.getElementById('chat-btn-emoji')
        if (!picker || !trigger) return
        if (picker.contains(event.target) || trigger.contains(event.target)) return
        closeEmojiPicker()
      }, { once: true })

      state.uiReady = true
    }

    async function refresh(keepSelection = true, nextThreadId = '') {
      const requestedThreadId = safeId(nextThreadId)
      if (state.refreshInFlight) {
        const prevPending = state.pendingRefresh || { keepSelection: true, nextThreadId: '' }
        state.pendingRefresh = {
          keepSelection: prevPending.keepSelection || keepSelection,
          nextThreadId: requestedThreadId || prevPending.nextThreadId || ''
        }
        return
      }

      state.refreshInFlight = true
      try {
      await cleanupOldMessagesBestEffort()
      await loadThreadsAndMessages()

      // Prefer explicit target thread. If absent, keep latest user selection at decision time.
      const desiredSelection = requestedThreadId || (keepSelection ? safeId(state.selectedThreadId) : '')
      if (desiredSelection && state.threads.some(item => safeId(item.id) === desiredSelection)) {
        state.selectedThreadId = desiredSelection
        state.selectedMessageIds.clear()
      } else {
        state.selectedThreadId = safeId(state.threads[0]?.id)
        state.selectedMessageIds.clear()
      }
      const visibleIds = new Set((state.messagesByThread.get(safeId(state.selectedThreadId)) || []).map(item => String(item.id || '')))
      ;[...state.selectedMessageIds].forEach(id => {
        if (!visibleIds.has(String(id))) state.selectedMessageIds.delete(id)
      })
      if (!state.uiReady) renderUI()
      else renderDynamicUI({ threadChanged: !!requestedThreadId })
      } finally {
        state.refreshInFlight = false
      }

      if (state.pendingRefresh) {
        const pending = state.pendingRefresh
        state.pendingRefresh = null
        await refresh(pending.keepSelection, pending.nextThreadId)
      }
    }

    function isUserInteracting() {
      const activeEl = document.activeElement
      const activeId = String(activeEl?.id || '')
      if (state.selectedMessageIds.size > 0) return true
      if (state.dmModalOpen) return true
      if (state.groupModalOpen) return true
      if (state.emojiPickerOpen) return true
      if (!state.messageListAtBottom) return true
      if (activeId === 'chat-message-input' || activeId === 'chat-group-name-modal' || activeId === 'chat-group-members-modal' || activeId === 'chat-dm-user-modal') return true
      return false
    }

    function startPolling() {
      stopPolling()
      state.pollHandle = window.setInterval(() => {
        if (isUserInteracting()) return
        refresh(true).catch(error => console.error('Chat sync refresh error:', error))
      }, 3000)
      window.__chatModuleActiveState = state
    }

    try {
      await loadUsers()
      await refresh(false)
      startRealtime()
      startPolling()
      window.__chatModuleActiveState = state
    } catch (error) {
      console.error('Chat init error:', error)
      const msg = String(error?.message || '').toLowerCase()
      const isMissingTable = msg.includes('does not exist') || (msg.includes('schema cache') && msg.includes('chat_'))
      content.innerHTML = `
        <div class="placeholder-card">
          <div style="font-weight:700; margin-bottom:6px;">Chat belum siap</div>
          <div style="font-size:13px; color:#475569; margin-bottom:8px;">${escapeHtml(error?.message || 'Unknown error')}</div>
          ${isMissingTable ? `
            <div style="font-size:12px; color:#334155; background:#f8fafc; border:1px solid #e2e8f0; border-radius:8px; padding:8px; white-space:pre-wrap;">Jalankan SQL setup chat:

create table if not exists public.chat_threads (
  id uuid primary key default gen_random_uuid(),
  title text,
  is_group boolean not null default false,
  created_by text,
  created_at timestamptz not null default now(),
  last_message_at timestamptz
);

create table if not exists public.chat_thread_members (
  id bigint generated always as identity primary key,
  thread_id uuid not null references public.chat_threads(id) on delete cascade,
  karyawan_id text not null,
  last_read_at timestamptz null,
  joined_at timestamptz not null default now(),
  unique(thread_id, karyawan_id)
);

create table if not exists public.chat_messages (
  id bigint generated always as identity primary key,
  thread_id uuid not null references public.chat_threads(id) on delete cascade,
  sender_id text not null,
  message_text text not null,
  created_at timestamptz not null default now()
);
            </div>
          ` : ''}
        </div>
      `
      window.__chatModuleActiveState = null
    }
  }

  window.ChatModule = {
    render,
    stop
  }
})()
