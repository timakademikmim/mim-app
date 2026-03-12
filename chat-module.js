(function initChatModule() {
  const CHAT_THREADS_TABLE = 'chat_threads'
  const CHAT_MEMBERS_TABLE = 'chat_thread_members'
  const CHAT_MESSAGES_TABLE = 'chat_messages'
  const CHAT_RETENTION_HOURS = 24
  const CHAT_EMOJIS = ['\u{1F600}', '\u{1F601}', '\u{1F602}', '\u{1F923}', '\u{1F60A}', '\u{1F60D}', '\u{1F970}', '\u{1F618}', '\u{1F60E}', '\u{1F914}', '\u{1F62D}', '\u{1F621}', '\u{1F44D}', '\u{1F44F}', '\u{1F64F}', '\u{1F4AA}', '\u{1F525}', '\u{2B50}', '\u{1F389}', '\u{2705}', '\u{274C}', '\u{1F4DA}', '\u{1F4DD}', '\u{1F4CC}', '\u{1F4AF}']
  const STICKER_PREFIX = '[[sticker]]'
  const CHAT_STICKER_BUCKET = 'chat-stickers'
  const CHAT_STICKER_MAX_DIM = 512
  const CHAT_STICKER_MAX_PER_USER = 40

  function escapeHtml(value) {
    return String(value ?? '')
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#39;')
  }

  function buildStickerMessage(url) {
    const clean = String(url || '').trim()
    return clean ? `${STICKER_PREFIX}${clean}` : ''
  }

  function parseStickerMessage(text) {
    const raw = String(text || '').trim()
    if (!raw.startsWith(STICKER_PREFIX)) return ''
    return raw.slice(STICKER_PREFIX.length).trim()
  }

  function isStickerMessage(text) {
    return !!parseStickerMessage(text)
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
    if (isStickerMessage(text)) return 0
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
    if (active?.content instanceof HTMLElement) {
      active.content.classList.remove('chat-full-bleed')
    }
    if (active?.popstateHandler) {
      try {
        window.removeEventListener('popstate', active.popstateHandler)
      } catch (_) {}
    }
    if (active?.outsideClickHandler) {
      try {
        document.removeEventListener('pointerdown', active.outsideClickHandler, true)
      } catch (_) {}
    }
    if (document.body?.classList?.contains('platform-android')) {
      document.body.classList.remove('android-chat-open')
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
    const requestedThreadId = safeId(options?.openThreadId || localStorage.getItem('chat_open_thread_id') || '')
    if (requestedThreadId) {
      try {
        localStorage.removeItem('chat_open_thread_id')
      } catch (_error) {}
    }
    const isAndroid = document.body?.classList?.contains('platform-android')
    if (isAndroid) content.classList.add('chat-full-bleed')

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
      mediaTab: 'emoji',
      stickerLibrary: [],
      stickerLoading: false,
      stickerStatus: '',
      stickerError: '',
      dmModalOpen: false,
      dmDraftUserId: '',
      threadMenuOpenId: '',
      groupModalOpen: false,
      groupDraftName: '',
      groupDraftMembers: new Set(),
      groupMembersScrollTop: 0,
      messageListAtBottom: true,
      forceStickBottom: false,
      keepMessageFocus: false,
      realtimeChannel: null,
      refreshTimer: null,
      uiReady: false,
      refreshInFlight: false,
      pendingRefresh: null
      ,
      threadListRenderKey: '',
      lastReadWriteByThread: new Map(),
      mobileChatView: 'list',
      popstateHandler: null,
      mobileThreadHistoryPushed: false,
      outsideClickHandler: null
    }

    function syncAndroidChatOpenClass() {
      if (!document.body?.classList?.contains('platform-android')) return
      const shouldOpen = isPhoneChatMode() && state.mobileChatView === 'thread'
      document.body.classList.toggle('android-chat-open', shouldOpen)
    }

    function enterMobileThreadView() {
      if (!isPhoneChatMode()) {
        state.mobileChatView = 'thread'
        syncAndroidChatOpenClass()
        return
      }
      if (state.mobileChatView !== 'thread') {
        try {
          window.history.pushState({ chatModuleView: 'thread', at: Date.now() }, '')
          state.mobileThreadHistoryPushed = true
        } catch (_) {}
      }
      state.mobileChatView = 'thread'
      syncAndroidChatOpenClass()
    }

    function openMobileThreadListView() {
      state.mobileChatView = 'list'
      state.mobileThreadHistoryPushed = false
      syncAndroidChatOpenClass()
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
          font-family: 'Poppins', 'Segoe UI', sans-serif;
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
          font-family: 'Poppins', 'Segoe UI', sans-serif;
        }
        .chat-thread-preview {
          font-size: 12px;
          color: #64748b;
          margin-top: 2px;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
          font-family: 'Poppins', 'Segoe UI', sans-serif;
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
        .chat-thread-badge {
          position: absolute;
          top: 8px;
          right: 36px;
          min-width: 18px;
          height: 18px;
          padding: 0 5px;
          border-radius: 999px;
          background: #2563eb;
          color: #fff;
          font-size: 10px;
          font-weight: 700;
          display: inline-flex;
          align-items: center;
          justify-content: center;
          box-shadow: 0 6px 16px rgba(37,99,235,0.35);
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
        #chat-btn-open-group-modal,
        #chat-btn-create-group {
          font-family: 'Poppins', 'Segoe UI', sans-serif;
        }
        .chat-media-tabs {
          display: flex;
          gap: 6px;
          padding: 6px;
          border-bottom: 1px solid #e2e8f0;
          background: #f8fafc;
        }
        .chat-media-tab {
          flex: 1;
          border: 1px solid #e2e8f0;
          border-radius: 8px;
          background: #fff;
          color: #475569;
          font-size: 12px;
          font-weight: 600;
          padding: 6px 8px;
          cursor: pointer;
        }
        .chat-media-tab.active {
          background: #2563eb;
          color: #fff;
          border-color: #2563eb;
        }
        .chat-sticker-controls {
          display: flex;
          gap: 6px;
          padding: 8px;
          flex-wrap: wrap;
          align-items: center;
        }
        .chat-sticker-btn {
          border: 1px solid #e2e8f0;
          border-radius: 8px;
          background: #fff;
          color: #0f172a;
          font-size: 12px;
          font-weight: 600;
          padding: 6px 10px;
          cursor: pointer;
        }
        .chat-sticker-btn.primary {
          background: #2563eb;
          color: #fff;
          border-color: #2563eb;
        }
        .chat-sticker-status {
          padding: 6px 8px;
          font-size: 12px;
          color: #64748b;
        }
        .chat-sticker-grid {
          display: grid;
          grid-template-columns: repeat(3, 1fr);
          gap: 6px;
          padding: 0 8px 8px;
          overflow: auto;
          flex: 1;
          min-height: 0;
        }
        .chat-sticker-item {
          border: 1px solid #e2e8f0;
          border-radius: 10px;
          padding: 4px;
          background: #fff;
          cursor: pointer;
          display: flex;
          align-items: center;
          justify-content: center;
        }
        .chat-sticker-item img {
          width: 100%;
          height: auto;
          display: block;
          border-radius: 8px;
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
      const raw = String(last?.message_text || '').trim()
      const stickerUrl = parseStickerMessage(raw)
      if (stickerUrl) return 'Sticker'
      return raw || '-'
    }

    function getThreadUnreadCount(thread) {
      const tid = safeId(thread?.id)
      if (!tid) return 0
      const selected = safeId(state.selectedThreadId)
      if (selected === tid && isThreadPanelVisible()) return 0
      const list = state.messagesByThread.get(tid) || []
      if (!list.length) return 0
      const member = (state.members || []).find(item => safeId(item?.thread_id) === tid && safeId(item?.karyawan_id) === state.currentUser.id)
      const readMs = member?.last_read_at ? getTimestampMs(member.last_read_at) : 0
      let count = 0
      list.forEach(msg => {
        if (safeId(msg?.sender_id) === state.currentUser.id) return
        const msgMs = getTimestampMs(msg?.created_at)
        if (!readMs || msgMs > readMs) count += 1
      })
      return count
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
      state.emojiPickerOpen = false
      updateMediaPanels()
    }

    function setMediaTab(tab) {
      if (tab !== 'emoji' && tab !== 'sticker') return
      state.mediaTab = tab
      updateMediaPanels()
      if (tab === 'sticker') ensureStickerLoaded()
    }

    function openMediaPicker(tab) {
      if (tab) state.mediaTab = tab
      state.emojiPickerOpen = true
      updateMediaPanels()
      if (state.mediaTab === 'sticker') ensureStickerLoaded()
    }

    function toggleEmojiPicker(tab = 'emoji') {
      if (state.emojiPickerOpen && state.mediaTab === tab) {
        closeEmojiPicker()
        return
      }
      openMediaPicker(tab)
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

    function getStickerFolder() {
      const rawId = safeId(state.currentUser.id)
      if (!rawId) return ''
      const safe = String(rawId).replaceAll('/', '-').replaceAll('\\', '-')
      return `users/${safe}`
    }

    function formatStickerError(prefix, error) {
      const message = String(error?.message || error || '').trim()
      if (!message) return prefix
      return `${prefix}: ${message}`
    }

    function buildStickerStorageErrorMessage(error) {
      const message = String(error?.message || '').trim()
      const lower = message.toLowerCase()
      if (lower.includes('bucket') && lower.includes('not')) {
        return 'Bucket stiker belum tersedia. Buat bucket "chat-stickers" dan set Public.'
      }
      if (lower.includes('permission') || lower.includes('policy') || lower.includes('jwt')) {
        return 'Akses stiker ditolak. Periksa policy Supabase Storage.'
      }
      return formatStickerError('Gagal memuat stiker', message || 'Unknown error')
    }

    function getStickerCreatedAt(item) {
      const createdAt = String(item?.created_at || item?.updated_at || '').trim()
      if (createdAt) {
        const ms = Date.parse(createdAt)
        if (!Number.isNaN(ms)) return ms
      }
      const name = String(item?.name || '')
      const match = name.match(/(\d{13})/)
      if (match) {
        const ms = Number(match[1])
        if (Number.isFinite(ms)) return ms
      }
      return 0
    }

    function loadImageFromFile(file) {
      return new Promise((resolve, reject) => {
        if (!file) {
          reject(new Error('File kosong.'))
          return
        }
        const url = URL.createObjectURL(file)
        const img = new Image()
        img.onload = () => {
          URL.revokeObjectURL(url)
          resolve(img)
        }
        img.onerror = () => {
          URL.revokeObjectURL(url)
          reject(new Error('File gambar tidak bisa dibaca.'))
        }
        img.src = url
      })
    }

    function canvasToBlob(canvas, type, quality) {
      return new Promise(resolve => {
        canvas.toBlob(blob => resolve(blob), type, quality)
      })
    }

    async function compressImageToWebp(file) {
      const img = await loadImageFromFile(file)
      const width = Number(img.naturalWidth || img.width || 0)
      const height = Number(img.naturalHeight || img.height || 0)
      if (!width || !height) throw new Error('Ukuran gambar tidak valid.')
      const maxDim = CHAT_STICKER_MAX_DIM
      const scale = Math.min(1, maxDim / Math.max(width, height))
      const targetWidth = Math.max(1, Math.round(width * scale))
      const targetHeight = Math.max(1, Math.round(height * scale))
      const canvas = document.createElement('canvas')
      canvas.width = targetWidth
      canvas.height = targetHeight
      const ctx = canvas.getContext('2d')
      if (!ctx) throw new Error('Gagal memproses gambar.')
      ctx.drawImage(img, 0, 0, targetWidth, targetHeight)
      const webpBlob = await canvasToBlob(canvas, 'image/webp', 0.82)
      if (webpBlob) return { blob: webpBlob, ext: 'webp' }
      const pngBlob = await canvasToBlob(canvas, 'image/png', 0.92)
      if (!pngBlob) throw new Error('Gagal mengubah gambar.')
      return { blob: pngBlob, ext: 'png' }
    }

    async function refreshStickerLibrary() {
      if (state.stickerLoading) return
      const folder = getStickerFolder()
      if (!folder) return
      state.stickerLoading = true
      state.stickerStatus = 'Memuat stiker...'
      state.stickerError = ''
      renderStickerResults()
      try {
        const { data, error } = await sb.storage
          .from(CHAT_STICKER_BUCKET)
          .list(folder, { limit: 200 })
        if (error) throw error
        const files = Array.isArray(data) ? data.filter(item => item?.name && !String(item.name).endsWith('/')) : []
        const mapped = files.map(item => {
          const path = `${folder}/${item.name}`
          const { data: publicData } = sb.storage.from(CHAT_STICKER_BUCKET).getPublicUrl(path)
          return {
            name: String(item.name),
            path,
            url: String(publicData?.publicUrl || ''),
            createdAt: getStickerCreatedAt(item)
          }
        }).filter(item => item.url)
        mapped.sort((a, b) => (b.createdAt - a.createdAt) || b.name.localeCompare(a.name))
        const limited = mapped.slice(0, CHAT_STICKER_MAX_PER_USER)
        state.stickerLibrary = limited
        if (mapped.length > CHAT_STICKER_MAX_PER_USER) {
          const excess = mapped.slice(CHAT_STICKER_MAX_PER_USER)
          void cleanupOldStickers(excess)
        }
      } catch (error) {
        state.stickerLibrary = []
        state.stickerError = buildStickerStorageErrorMessage(error)
      } finally {
        state.stickerLoading = false
        state.stickerStatus = ''
        renderStickerResults()
      }
    }

    async function cleanupOldStickers(list) {
      if (!Array.isArray(list) || list.length === 0) return
      const paths = list.map(item => item?.path).filter(Boolean)
      if (!paths.length) return
      try {
        await sb.storage.from(CHAT_STICKER_BUCKET).remove(paths)
      } catch (_error) {}
    }

    async function uploadStickerFromFile(file) {
      if (!file) return
      const folder = getStickerFolder()
      if (!folder) return
      state.stickerLoading = true
      state.stickerStatus = 'Mengompres stiker...'
      state.stickerError = ''
      renderStickerResults()
      try {
        const { blob, ext } = await compressImageToWebp(file)
        const stamp = Date.now()
        const random = Math.random().toString(36).slice(2, 8)
        const fileName = `sticker-${stamp}-${random}.${ext}`
        const path = `${folder}/${fileName}`
        state.stickerStatus = 'Mengunggah stiker...'
        renderStickerResults()
        const { error } = await sb.storage
          .from(CHAT_STICKER_BUCKET)
          .upload(path, blob, {
            contentType: blob.type || 'image/webp',
            upsert: false
          })
        if (error) throw error
        const { data: publicData } = sb.storage.from(CHAT_STICKER_BUCKET).getPublicUrl(path)
        const publicUrl = String(publicData?.publicUrl || '')
        if (!publicUrl) throw new Error('URL stiker tidak ditemukan. Pastikan bucket public.')
        await sendStickerMessage(publicUrl)
        state.stickerLoading = false
        state.stickerStatus = ''
        await refreshStickerLibrary()
      } catch (error) {
        state.stickerError = formatStickerError('Gagal mengunggah stiker', error)
      } finally {
        state.stickerLoading = false
        state.stickerStatus = ''
        renderStickerResults()
      }
    }

    async function deleteStickerByPath(path) {
      const cleanPath = String(path || '').trim()
      if (!cleanPath) return
      const confirmDelete = window.confirm('Hapus stiker ini?')
      if (!confirmDelete) return
      state.stickerLoading = true
      state.stickerStatus = 'Menghapus stiker...'
      state.stickerError = ''
      renderStickerResults()
      try {
        const { error } = await sb.storage.from(CHAT_STICKER_BUCKET).remove([cleanPath])
        if (error) throw error
      } catch (error) {
        state.stickerError = formatStickerError('Gagal menghapus stiker', error)
      } finally {
        state.stickerLoading = false
        state.stickerStatus = ''
      }
      await refreshStickerLibrary()
    }

    function renderStickerResults() {
      const gridEls = [
        document.getElementById('chat-sticker-grid-popover'),
        document.getElementById('chat-sticker-grid-bar')
      ].filter(Boolean)
      const statusEls = [
        document.getElementById('chat-sticker-status-popover'),
        document.getElementById('chat-sticker-status-bar')
      ].filter(Boolean)
      const hasResults = state.stickerLibrary.length > 0
      const statusText = state.stickerLoading
        ? (state.stickerStatus || 'Memuat stiker...')
        : state.stickerError
          ? state.stickerError
          : !hasResults
            ? 'Belum ada stiker. Klik Upload untuk menambahkan.'
            : ''
      statusEls.forEach(el => {
        el.textContent = statusText
        el.style.display = statusText ? 'block' : 'none'
      })
      gridEls.forEach(grid => {
        if (!hasResults) {
          grid.innerHTML = ''
          return
        }
        grid.innerHTML = state.stickerLibrary.map(item => `
          <button type="button" class="chat-sticker-item" data-sticker-url="${escapeHtml(item.url)}" data-sticker-path="${escapeHtml(item.path)}" title="Sticker">
            <img src="${escapeHtml(item.url)}" alt="Sticker">
          </button>
        `).join('')
        Array.from(grid.querySelectorAll('[data-sticker-url]')).forEach(btn => {
          let pressTimer = null
          let longPressTriggered = false
          let startX = 0
          let startY = 0
          const clearTimer = () => {
            if (pressTimer) {
              window.clearTimeout(pressTimer)
              pressTimer = null
            }
          }
          btn.addEventListener('pointerdown', event => {
            if (event.button && event.button !== 0) return
            longPressTriggered = false
            startX = Number(event.clientX || 0)
            startY = Number(event.clientY || 0)
            clearTimer()
            pressTimer = window.setTimeout(async () => {
              longPressTriggered = true
              btn.dataset.longpressBlock = '1'
              const path = String(btn.getAttribute('data-sticker-path') || '')
              await deleteStickerByPath(path)
              window.setTimeout(() => {
                delete btn.dataset.longpressBlock
              }, 0)
            }, 600)
          })
          btn.addEventListener('pointerup', clearTimer)
          btn.addEventListener('pointerleave', clearTimer)
          btn.addEventListener('pointercancel', clearTimer)
          btn.addEventListener('pointermove', event => {
            if (!pressTimer) return
            const dx = Math.abs(Number(event.clientX || 0) - startX)
            const dy = Math.abs(Number(event.clientY || 0) - startY)
            if (dx > 8 || dy > 8) clearTimer()
          })
          btn.addEventListener('click', async event => {
            if (btn.dataset.longpressBlock === '1' || longPressTriggered) {
              event.preventDefault()
              event.stopPropagation()
              return
            }
            event.stopPropagation()
            const url = String(btn.getAttribute('data-sticker-url') || '')
            await sendStickerMessage(url)
          })
        })
      })
    }

    function ensureStickerLoaded() {
      if (state.stickerLoading) return
      if (state.stickerLibrary.length) return
      refreshStickerLibrary()
    }

    async function sendMessage(customText = null) {
      const threadId = safeId(state.selectedThreadId)
      const textEl = document.getElementById('chat-message-input')
      const messageText = String(customText !== null ? customText : (textEl?.value || '')).trim()
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

      if (customText === null) {
        if (textEl) textEl.value = ''
        state.draftByThread.set(threadId, '')
        state.keepMessageFocus = true
      }
      state.forceStickBottom = true
      await refresh(false, threadId)
    }

    async function sendStickerMessage(url) {
      const stickerUrl = String(url || '').trim()
      if (!stickerUrl) return
      const payload = buildStickerMessage(stickerUrl)
      if (!payload) return
      await sendMessage(payload)
      closeEmojiPicker()
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
      if (isThreadPanelVisible()) markThreadAsRead(thread.id)
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
        const rawMessage = String(item.message_text || '')
        const stickerUrl = parseStickerMessage(rawMessage)
        const isSticker = !!stickerUrl
        const emojiOnlyCount = getEmojiOnlyCount(rawMessage)
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

        const bubbleStyle = isSticker
          ? `max-width:75%; background:transparent; border:${selected ? '1px solid #16a34a' : '1px solid transparent'}; border-radius:12px; padding:2px; ${mine ? 'cursor:pointer;' : ''}`
          : `max-width:75%; background:${mine ? (selected ? '#bbf7d0' : '#dcfce7') : '#f1f5f9'}; border:1px solid ${selected ? '#16a34a' : '#e2e8f0'}; border-radius:10px; padding:8px 10px; ${mine ? 'cursor:pointer;' : ''}`
        const messageBody = isSticker
          ? `<img src="${escapeHtml(stickerUrl)}" alt="Sticker" style="max-width:220px; width:100%; height:auto; display:block; border-radius:10px;">`
          : `<div style="${isStickerEmoji ? 'font-size:52px; line-height:1.05; text-align:center; padding:6px 0;' : 'font-size:13px; color:#0f172a; white-space:pre-wrap;'}">${escapeHtml(rawMessage)}</div>`

        rowsHtml.push(`
          <div style="display:flex; justify-content:${mine ? 'flex-end' : 'flex-start'}; margin-bottom:8px;" data-chat-row-id="${escapeHtml(msgId)}">
            <div style="display:flex; align-items:flex-end; gap:6px; ${mine ? 'flex-direction:row-reverse;' : ''}">
              <div data-chat-own-click="${mine ? '1' : '0'}" data-chat-message-id="${escapeHtml(msgId)}" style="${bubbleStyle}">
                ${showSender ? `<div style="font-size:11px; color:#64748b; margin-bottom:2px;">${escapeHtml(String(sender?.nama || 'User'))}</div>` : ''}
                ${messageBody}
                ${mine ? `<div style="font-size:10px; color:${mineStatus === 'Sudah dibaca' ? '#16a34a' : '#64748b'}; margin-top:4px; text-align:right;">${escapeHtml(mineStatus || 'Terkirim')}</div>` : ''}
              </div>
              <div style="font-size:10px; color:#94a3b8; margin-bottom:2px; white-space:nowrap;">${escapeHtml(formatTimeOnly(item.created_at))}</div>
            </div>
          </div>
        `)

        prevSenderId = senderId
        prevMinuteStamp = minuteStamp
      })
      box.innerHTML = rowsHtml.join('')
      const toggleMessageSelection = targetId => {
        if (!targetId) return
        if (state.selectedMessageIds.has(targetId)) state.selectedMessageIds.delete(targetId)
        else state.selectedMessageIds.add(targetId)
        renderMessagesPanel()
      }
      Array.from(box.querySelectorAll('[data-chat-own-click="1"]')).forEach(el => {
        let longPressTimer = null
        let longPressTriggered = false
        const targetId = String(el.getAttribute('data-chat-message-id') || '')
        const clearLongPress = () => {
          if (longPressTimer) clearTimeout(longPressTimer)
          longPressTimer = null
        }
        const handlePointerDown = event => {
          if (!targetId) return
          if (state.selectedMessageIds.size > 0) return
          if (event.pointerType === 'mouse') return
          longPressTriggered = false
          clearLongPress()
          longPressTimer = setTimeout(() => {
            longPressTriggered = true
            toggleMessageSelection(targetId)
          }, 450)
        }
        const handlePointerUp = () => {
          clearLongPress()
        }
        el.addEventListener('pointerdown', handlePointerDown)
        el.addEventListener('pointerup', handlePointerUp)
        el.addEventListener('pointerleave', handlePointerUp)
        el.addEventListener('pointercancel', handlePointerUp)
        el.addEventListener('click', event => {
          if (!targetId) return
          if (longPressTriggered) {
            longPressTriggered = false
            event.preventDefault()
            event.stopPropagation()
            return
          }
          if (state.selectedMessageIds.size > 0) {
            toggleMessageSelection(targetId)
            event.stopPropagation()
            return
          }
          if (event.ctrlKey || event.metaKey) {
            toggleMessageSelection(targetId)
            event.stopPropagation()
          }
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
      const isAndroid = document.body?.classList?.contains('platform-android')
      const selectedId = safeId(state.selectedThreadId)
      const renderKey = JSON.stringify({
        selectedId,
        menu: safeId(state.threadMenuOpenId),
        threads: state.threads.map(thread => ({
          id: safeId(thread.id),
          title: getThreadTitle(thread),
          preview: getThreadPreview(thread),
          unread: getThreadUnreadCount(thread)
        }))
      })
      if (renderKey === state.threadListRenderKey && box.childElementCount > 0) return
      state.threadListRenderKey = renderKey
      if (!state.threads.length) {
        box.innerHTML = '<div style="color:#64748b; font-size:13px;">Belum ada thread chat.</div>'
        return
      }
      const unreadByThread = new Map()
      state.threads.forEach(thread => {
        const tid = safeId(thread?.id)
        if (!tid) return
        unreadByThread.set(tid, getThreadUnreadCount(thread))
      })
      box.innerHTML = state.threads.map(thread => {
        const tid = safeId(thread.id)
        const active = tid === selectedId
        const activeBorderColor = isAndroid ? '#dbe4ef' : '#86efac'
        const menuOpen = tid === safeId(state.threadMenuOpenId)
        const preview = getThreadPreview(thread)
        const title = getThreadTitle(thread)
        const avatar = getThreadAvatarInfo(thread)
        const unreadCount = unreadByThread.get(tid) || 0
        const badgeHtml = unreadCount > 0
          ? `<span class="chat-thread-badge">${unreadCount > 99 ? '99+' : unreadCount}</span>`
          : ''
        return `
          <div class="chat-thread-wrap">
            <button type="button" class="chat-thread-btn" data-chat-thread-id="${escapeHtml(tid)}" style="--chat-thread-border:${active ? activeBorderColor : '#dbe4ef'};">
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
            ${badgeHtml}
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
          if (isPhoneChatMode()) enterMobileThreadView()
          state.selectedMessageIds.clear()
          state.forceStickBottom = true
          if (isPhoneChatMode()) renderUI()
          else renderDynamicUI({ threadChanged: true })
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

    function isPhoneChatMode() {
      const width = Number(window.innerWidth || 0)
      return width > 0 && width <= 820
    }

    function isThreadPanelVisible() {
      const panel = document.getElementById('chat-thread-panel')
      if (!panel) return true
      return panel.style.display !== 'none'
    }

    function updateMediaPanels() {
      const isAndroid = document.body?.classList?.contains('platform-android')
      const popover = document.getElementById('chat-emoji-picker-popover')
      const bar = document.getElementById('chat-emoji-picker-bar')
      const barWrap = document.getElementById('chat-emoji-bar')
      const emojiPanelPopover = document.getElementById('chat-emoji-panel-popover')
      const stickerPanelPopover = document.getElementById('chat-sticker-panel-popover')
      const emojiPanelBar = document.getElementById('chat-emoji-panel-bar')
      const stickerPanelBar = document.getElementById('chat-sticker-panel-bar')
      if (popover) popover.style.display = state.emojiPickerOpen && !isAndroid ? 'block' : 'none'
      if (bar) bar.style.display = state.emojiPickerOpen && isAndroid ? 'block' : 'none'
      if (barWrap) barWrap.style.display = state.emojiPickerOpen && isAndroid ? 'block' : 'none'
      const showEmoji = state.mediaTab === 'emoji'
      if (emojiPanelPopover) emojiPanelPopover.style.display = showEmoji ? 'block' : 'none'
      if (stickerPanelPopover) stickerPanelPopover.style.display = showEmoji ? 'none' : 'flex'
      if (emojiPanelBar) emojiPanelBar.style.display = showEmoji ? 'block' : 'none'
      if (stickerPanelBar) stickerPanelBar.style.display = showEmoji ? 'none' : 'flex'
      const tabs = Array.from(document.querySelectorAll('[data-chat-media-tab]'))
      tabs.forEach(btn => {
        const tab = btn.getAttribute('data-chat-media-tab')
        btn.classList.toggle('active', tab === state.mediaTab)
      })
      if (state.emojiPickerOpen && isAndroid && barWrap) {
        const list = document.getElementById('chat-message-list')
        if (list) list.scrollTop = list.scrollHeight
      }
    }

    function restoreMessageFocus() {
      if (!state.keepMessageFocus) return
      state.keepMessageFocus = false
      const input = document.getElementById('chat-message-input')
      if (!input) return
      window.setTimeout(() => {
        try {
          input.focus()
          const len = input.value.length
          input.setSelectionRange(len, len)
        } catch (_error) {}
      }, 0)
    }

    function renderDynamicUI(options = {}) {
      const threadChanged = Boolean(options?.threadChanged)
      if (!isPhoneChatMode()) state.mobileChatView = 'thread'
      syncAndroidChatOpenClass()
      const titleEl = document.getElementById('chat-thread-title')
      const selected = getSelectedThread()
      if (titleEl) titleEl.textContent = getThreadTitle(selected)
      renderThreadList()
      renderMessagesPanel()
      renderStickerResults()
      syncComposerForSelectedThread(threadChanged)
      updateMediaPanels()
      restoreMessageFocus()
    }

    function renderUI() {
      state.threadListRenderKey = ''
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
      const isPhone = isPhoneChatMode()
      const showThreadPanel = !isPhone || state.mobileChatView === 'thread'
      const showListPanel = !isPhone || state.mobileChatView !== 'thread'
      syncAndroidChatOpenClass()

      content.innerHTML = `
        <div style="display:grid; width:100%; max-width:100%; grid-template-columns:${isPhone ? '1fr' : 'minmax(220px, 300px) minmax(0, 1fr)'}; gap:12px; height:100%; min-height:0; overflow:hidden;">
          <div style="display:${showListPanel ? 'flex' : 'none'}; border:1px solid #e2e8f0; border-radius:12px; background:#fff; padding:10px; min-height:0; min-width:0; flex-direction:column; overflow:hidden;">
            <div style="display:grid; gap:8px; margin-bottom:10px;">
              <div style="display:flex; gap:8px;">
                <button type="button" class="modal-btn" id="chat-btn-open-dm-modal" title="Tambah DM" style="min-width:42px; padding:8px 12px; font-weight:700; font-size:18px; line-height:1;">+</button>
                <button type="button" class="modal-btn" id="chat-btn-open-group-modal">Buat Grup</button>
              </div>
            </div>
            <div style="font-size:12px; color:#64748b; margin-bottom:6px;">Thread</div>
            <div id="chat-thread-list" style="flex:1; min-height:0; overflow:auto; padding-right:2px;"></div>
          </div>
          <div id="chat-thread-panel" style="display:${showThreadPanel ? 'flex' : 'none'}; border:${isPhone ? 'none' : '1px solid #e2e8f0'}; border-radius:${isPhone ? '0' : '12px'}; background:${isPhone ? 'transparent' : '#fff'}; min-height:0; min-width:0; flex-direction:column; overflow:hidden;">
            <div style="padding:10px 12px; border-bottom:1px solid #e2e8f0; font-weight:700; color:#0f172a; display:flex; align-items:center; gap:8px;">
              ${isPhone ? '<button type="button" class="modal-btn" id="chat-btn-mobile-back" style="padding:6px 10px; min-width:0; font-size:16px; font-weight:700; line-height:1;">&lt;</button>' : ''}
              <div id="chat-thread-title" style="font-size:${isPhone ? '14px' : '16px'}; line-height:1.2;">${escapeHtml(getThreadTitle(selected))}</div>
            </div>
            <div id="chat-selection-bar" style="display:${selectedCount > 0 ? 'flex' : 'none'}; align-items:center; justify-content:space-between; gap:8px; padding:8px 12px; border-bottom:1px solid #e2e8f0; background:#f8fafc;">
              <div id="chat-selection-text" style="font-size:12px; color:#334155;">${selectedCount} pesan dipilih</div>
              <div style="display:flex; gap:8px;">
                <button type="button" class="modal-btn" id="chat-btn-clear-selection">Batal Pilih</button>
                <button type="button" class="modal-btn" style="background:#dc2626; color:#fff;" id="chat-btn-delete-selected">Hapus Terpilih</button>
              </div>
            </div>
            <div id="chat-message-list" style="flex:1; min-height:0; overflow:auto; padding:10px;"></div>
            <div style="padding:10px; border-top:1px solid #e2e8f0; display:flex; gap:8px; align-items:center; position:relative;">
              <button type="button" class="modal-btn" id="chat-btn-emoji" style="min-width:42px; padding:8px 10px;" aria-label="Emoji">&#128522;</button>
              <input id="chat-message-input" class="guru-field" type="text" placeholder="Ketik pesan..." style="flex:1;">
              <button type="button" class="modal-btn modal-btn-primary" id="chat-btn-send">Kirim</button>
              <div id="chat-emoji-picker-popover" style="display:none; position:absolute; left:10px; bottom:56px; width:min(360px, 90vw); max-height:360px; overflow:hidden; background:#fff; border:1px solid #e2e8f0; border-radius:10px; padding:0; box-shadow:0 8px 24px rgba(15,23,42,0.12); z-index:20;">
                <div class="chat-media-tabs">
                  <button type="button" class="chat-media-tab" data-chat-media-tab="emoji">Emoji</button>
                  <button type="button" class="chat-media-tab" data-chat-media-tab="sticker">Sticker</button>
                </div>
                <div id="chat-emoji-panel-popover">
                  <emoji-picker id="chat-emoji-picker-el-popover" style="width:100%; height:300px;"></emoji-picker>
                  <div id="chat-emoji-fallback-popover" style="display:none; padding:8px; max-height:200px; overflow:auto;">
                    <div style="display:grid; grid-template-columns:repeat(8, 1fr); gap:6px;">
                      ${CHAT_EMOJIS.map(item => `<button type="button" data-chat-emoji="${escapeHtml(item)}" style="border:1px solid #e2e8f0; background:#fff; border-radius:8px; height:32px; cursor:pointer; font-size:18px;">${item}</button>`).join('')}
                    </div>
                  </div>
                </div>
                <div id="chat-sticker-panel-popover" style="display:none; height:300px; flex-direction:column;">
                  <div class="chat-sticker-controls" style="flex:0 0 auto;">
                    <button type="button" class="chat-sticker-btn primary" id="chat-sticker-upload-popover">Upload</button>
                    <button type="button" class="chat-sticker-btn" id="chat-sticker-refresh-popover">Refresh</button>
                    <input id="chat-sticker-file-popover" type="file" accept="image/*" style="display:none;">
                  </div>
                  <div id="chat-sticker-status-popover" class="chat-sticker-status" style="flex:0 0 auto;"></div>
                  <div id="chat-sticker-grid-popover" class="chat-sticker-grid"></div>
                </div>
              </div>
            </div>
            <div id="chat-emoji-bar" style="display:none; border-top:1px solid #e2e8f0; background:#fff; padding:0; flex:0 0 auto;">
              <div id="chat-emoji-picker-bar" style="display:none; position:static; width:100%; max-height:340px; overflow:hidden; background:#fff; border:none; border-radius:0; padding:0; box-shadow:none;">
                <div class="chat-media-tabs">
                  <button type="button" class="chat-media-tab" data-chat-media-tab="emoji">Emoji</button>
                  <button type="button" class="chat-media-tab" data-chat-media-tab="sticker">Sticker</button>
                </div>
                <div id="chat-emoji-panel-bar">
                  <emoji-picker id="chat-emoji-picker-el-bar" style="width:100%; height:300px;"></emoji-picker>
                  <div id="chat-emoji-fallback-bar" style="display:none; padding:8px; max-height:300px; overflow:auto;">
                    <div style="display:grid; grid-template-columns:repeat(8, 1fr); gap:6px;">
                      ${CHAT_EMOJIS.map(item => `<button type="button" data-chat-emoji="${escapeHtml(item)}" style="border:1px solid #e2e8f0; background:#fff; border-radius:8px; height:32px; cursor:pointer; font-size:18px;">${item}</button>`).join('')}
                    </div>
                  </div>
                </div>
                <div id="chat-sticker-panel-bar" style="display:none; height:300px; flex-direction:column;">
                  <div class="chat-sticker-controls" style="flex:0 0 auto;">
                    <button type="button" class="chat-sticker-btn primary" id="chat-sticker-upload-bar">Upload</button>
                    <button type="button" class="chat-sticker-btn" id="chat-sticker-refresh-bar">Refresh</button>
                    <input id="chat-sticker-file-bar" type="file" accept="image/*" style="display:none;">
                  </div>
                  <div id="chat-sticker-status-bar" class="chat-sticker-status" style="flex:0 0 auto;"></div>
                  <div id="chat-sticker-grid-bar" class="chat-sticker-grid"></div>
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
      renderStickerResults()

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
      const mobileBackBtn = document.getElementById('chat-btn-mobile-back')
      const input = document.getElementById('chat-message-input')
      const emojiBtn = document.getElementById('chat-btn-emoji')
      const emojiPickerPopover = document.getElementById('chat-emoji-picker-popover')
      const emojiPickerBar = document.getElementById('chat-emoji-picker-bar')
      const emojiPickerElPopover = document.getElementById('chat-emoji-picker-el-popover')
      const emojiPickerElBar = document.getElementById('chat-emoji-picker-el-bar')
      const emojiFallbackPopover = document.getElementById('chat-emoji-fallback-popover')
      const emojiFallbackBar = document.getElementById('chat-emoji-fallback-bar')
      const emojiBar = document.getElementById('chat-emoji-bar')
      const emojiPickerEls = [emojiPickerElPopover, emojiPickerElBar].filter(Boolean)
      const emojiFallbacks = [emojiFallbackPopover, emojiFallbackBar].filter(Boolean)
      const stickerUploadBtnPopover = document.getElementById('chat-sticker-upload-popover')
      const stickerUploadBtnBar = document.getElementById('chat-sticker-upload-bar')
      const stickerRefreshBtnPopover = document.getElementById('chat-sticker-refresh-popover')
      const stickerRefreshBtnBar = document.getElementById('chat-sticker-refresh-bar')
      const stickerFilePopover = document.getElementById('chat-sticker-file-popover')
      const stickerFileBar = document.getElementById('chat-sticker-file-bar')
      state.emojiBarEl = emojiBar
      const clearSelectionBtn = document.getElementById('chat-btn-clear-selection')
      const deleteSelectedBtn = document.getElementById('chat-btn-delete-selected')
      const cancelDeleteBtn = document.getElementById('chat-btn-cancel-delete')
      const confirmDeleteBtn = document.getElementById('chat-btn-confirm-delete')
      const deleteOverlay = document.getElementById('chat-delete-confirm-overlay')
      updateMediaPanels()
      if (sendBtn) sendBtn.addEventListener('click', () => {
        sendMessage()
      })
      if (mobileBackBtn) {
        mobileBackBtn.addEventListener('click', () => {
          if (isPhoneChatMode() && state.mobileThreadHistoryPushed) {
            try {
              window.history.back()
              return
            } catch (_) {}
          }
          openMobileThreadListView()
          renderUI()
        })
      }
      if (emojiBtn) {
        emojiBtn.addEventListener('click', event => {
          event.stopPropagation()
          toggleEmojiPicker('emoji')
        })
      }
      Array.from(document.querySelectorAll('[data-chat-media-tab]')).forEach(btn => {
        btn.addEventListener('click', event => {
          event.stopPropagation()
          const tab = String(btn.getAttribute('data-chat-media-tab') || '').trim()
          if (tab) openMediaPicker(tab)
        })
      })
      emojiPickerEls.forEach(pickerEl => {
        const usePicker = typeof customElements !== 'undefined' && customElements.get('emoji-picker')
        pickerEl.style.display = usePicker ? 'block' : 'none'
        const fallback =
          pickerEl === emojiPickerElPopover
            ? emojiFallbackPopover
            : pickerEl === emojiPickerElBar
              ? emojiFallbackBar
              : null
        if (fallback) fallback.style.display = usePicker ? 'none' : 'block'
        pickerEl.addEventListener('emoji-click', event => {
          const emoji = String(event?.detail?.unicode || '').trim()
          if (!emoji) return
          insertEmojiToInput(emoji)
        })
      })
      emojiFallbacks.forEach(fallback => {
        Array.from(fallback.querySelectorAll('[data-chat-emoji]')).forEach(btn => {
          btn.addEventListener('click', event => {
            event.stopPropagation()
            const emoji = String(btn.getAttribute('data-chat-emoji') || '')
            if (!emoji) return
            insertEmojiToInput(emoji)
          })
        })
      })
      if (stickerUploadBtnPopover && stickerFilePopover) {
        stickerUploadBtnPopover.addEventListener('click', () => {
          stickerFilePopover.click()
        })
      }
      if (stickerUploadBtnBar && stickerFileBar) {
        stickerUploadBtnBar.addEventListener('click', () => {
          stickerFileBar.click()
        })
      }
      if (stickerRefreshBtnPopover) stickerRefreshBtnPopover.addEventListener('click', refreshStickerLibrary)
      if (stickerRefreshBtnBar) stickerRefreshBtnBar.addEventListener('click', refreshStickerLibrary)
      if (stickerFilePopover) {
        stickerFilePopover.addEventListener('change', async () => {
          const file = stickerFilePopover.files?.[0] || null
          stickerFilePopover.value = ''
          if (file) await uploadStickerFromFile(file)
        })
      }
      if (stickerFileBar) {
        stickerFileBar.addEventListener('change', async () => {
          const file = stickerFileBar.files?.[0] || null
          stickerFileBar.value = ''
          if (file) await uploadStickerFromFile(file)
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
        input.addEventListener('focus', () => {
          const isAndroid = document.body?.classList?.contains('platform-android')
          if (!isAndroid) return
          if (!state.emojiPickerOpen) return
          window.setTimeout(() => {
            if (state.emojiPickerOpen) closeEmojiPicker()
          }, 120)
        })
        input.addEventListener('keydown', async event => {
          if (event.key !== 'Enter' || event.shiftKey) return
          event.preventDefault()
          await sendMessage()
        })
      }
      if (!state.outsideClickHandler) {
        state.outsideClickHandler = event => {
          if (!state.emojiPickerOpen) return
          const pickerPopover = document.getElementById('chat-emoji-picker-popover')
          const pickerBar = document.getElementById('chat-emoji-picker-bar')
          const emojiBar = document.getElementById('chat-emoji-bar')
          const emojiTrigger = document.getElementById('chat-btn-emoji')
          const messageInput = document.getElementById('chat-message-input')
          const path = typeof event.composedPath === 'function' ? event.composedPath() : []
          const clickedInsidePicker =
            (pickerPopover && (path.includes(pickerPopover) || pickerPopover.contains(event.target))) ||
            (pickerBar && (path.includes(pickerBar) || pickerBar.contains(event.target))) ||
            (emojiBar && (path.includes(emojiBar) || emojiBar.contains(event.target)))
          const clickedTrigger =
            (emojiTrigger && (path.includes(emojiTrigger) || emojiTrigger.contains(event.target)))
          const clickedInput =
            (messageInput && (path.includes(messageInput) || messageInput.contains(event.target)))
          if (clickedInsidePicker || clickedTrigger || clickedInput) return
          closeEmojiPicker()
        }
        document.addEventListener('pointerdown', state.outsideClickHandler, true)
      }

      restoreMessageFocus()
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
      const hasActiveSelection = state.selectedMessageIds.size > 0
      const desiredSelection = requestedThreadId || (keepSelection ? safeId(state.selectedThreadId) : '')
      if (desiredSelection && state.threads.some(item => safeId(item.id) === desiredSelection)) {
        if (!hasActiveSelection || safeId(state.selectedThreadId) !== desiredSelection) {
          state.selectedMessageIds.clear()
        }
        state.selectedThreadId = desiredSelection
      } else {
        state.selectedThreadId = safeId(state.threads[0]?.id)
        if (!hasActiveSelection) {
          state.selectedMessageIds.clear()
        }
      }
      if (!hasActiveSelection) {
        const visibleIds = new Set((state.messagesByThread.get(safeId(state.selectedThreadId)) || []).map(item => String(item.id || '')))
        ;[...state.selectedMessageIds].forEach(id => {
          if (!visibleIds.has(String(id))) state.selectedMessageIds.delete(id)
        })
      }
      if (requestedThreadId && isPhoneChatMode()) enterMobileThreadView()
      if (!state.uiReady) renderUI()
      else if (isPhoneChatMode() && requestedThreadId) renderUI()
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
      state.popstateHandler = () => {
        if (!isPhoneChatMode()) return
        if (state.mobileChatView !== 'thread') return
        openMobileThreadListView()
        renderUI()
      }
      window.addEventListener('popstate', state.popstateHandler)
      await loadUsers()
      await refresh(false, requestedThreadId)
      startRealtime()
      startPolling()
      window.__chatModuleActiveState = state
      window.ChatModule.openThread = async function openThread(threadId) {
        const tid = safeId(threadId)
        if (!tid) return false
        await refresh(true, tid)
        return true
      }
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

