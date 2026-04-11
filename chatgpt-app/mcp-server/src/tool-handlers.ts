import type { ChatGptExamMcpConfig } from "./config.js"
import { callSupabaseFunction } from "./supabase-edge.js"
import { buildRevisedDraftPayload, extractExistingDraftPayload, type RevisionMode } from "./draft-utils.js"

export type FindExamAiTargetInput = {
  guru_id: string
  kelas: string
  mapel: string
  tanggal: string
  nama_ujian?: string
  jenis?: string
}

export type ResolveChatGptAppGuruInput = {
  provider?: string
  external_subject: string
}

export type GetExistingExamDraftInput = {
  jadwal_id: string
  guru_id: string
  kelas_target?: string | null
}

export type ExchangeChatGptAppLinkCodeInput = {
  provider?: string
  code: string
  external_subject: string
  display_name?: string
  email?: string
}

export type SaveExamAiDraftInput = {
  jadwal_id: string
  kelas_target?: string | null
  guru_id: string
  guru_nama?: string | null
  instruksi?: string | null
  instruksi_lang?: string | null
  source?: string | null
  sections?: Array<Record<string, unknown>>
  questions: Array<Record<string, unknown>>
}

export type ReviseExamAiDraftInput = SaveExamAiDraftInput & {
  revision_mode?: RevisionMode
  fail_if_missing?: boolean
}

function findLocalGuruLink(config: ChatGptExamMcpConfig, provider: string, externalSubject: string) {
  return (config.localGuruLinks || []).find(item => (
    String(item.provider || "chatgpt").trim() === provider &&
    String(item.external_subject || "").trim() === externalSubject &&
    String(item.is_active ?? true).trim() !== "false"
  ))
}

function buildLocalLinkedResponse(localMatch: Record<string, unknown>, provider: string, externalSubject: string) {
  return {
    ok: true,
    status: "linked",
    guru: {
      ...localMatch,
      provider,
      external_subject: externalSubject,
      metadata_json: {
        source: "local-config",
        ...(localMatch.metadata_json && typeof localMatch.metadata_json === "object" ? localMatch.metadata_json as Record<string, unknown> : {})
      }
    }
  }
}

export function createToolHandlers(config: ChatGptExamMcpConfig) {
  return {
    async resolveChatGptAppGuru(input: ResolveChatGptAppGuruInput) {
      const provider = String(input.provider || "chatgpt").trim() || "chatgpt"
      const externalSubject = String(input.external_subject || "").trim()
      const localMatch = findLocalGuruLink(config, provider, externalSubject)

      if (config.resolverMode === "local-only") {
        if (localMatch) return buildLocalLinkedResponse(localMatch, provider, externalSubject)
        return {
          ok: true,
          status: "not_linked",
          guru: null,
          meta: {
            source: "local-only",
            resolver_mode: config.resolverMode
          }
        }
      }

      const callLiveResolver = async () => await callSupabaseFunction<ResolveChatGptAppGuruInput, Record<string, unknown>>(
        config,
        "resolve-chatgpt-app-guru",
        input,
        { sharedSecret: config.resolverSharedSecret }
      )

      if (config.resolverMode === "local-first") {
        if (localMatch) return buildLocalLinkedResponse(localMatch, provider, externalSubject)
        return await callLiveResolver()
      }

      if (config.resolverMode === "live-first") {
        try {
          const liveResult = await callLiveResolver()
          if (String((liveResult as Record<string, unknown>)?.status || "") === "linked") return liveResult
          if (localMatch) return buildLocalLinkedResponse(localMatch, provider, externalSubject)
          return liveResult
        } catch (error) {
          if (localMatch) {
            return {
              ...buildLocalLinkedResponse(localMatch, provider, externalSubject),
              meta: {
                source: "local-fallback-after-live-error",
                resolver_mode: config.resolverMode,
                live_error: String((error as Error)?.message || error)
              }
            }
          }
          throw error
        }
      }

      return await callLiveResolver()
    },

    async findExamAiTarget(input: FindExamAiTargetInput) {
      return await callSupabaseFunction<FindExamAiTargetInput, Record<string, unknown>>(
        config,
        "find-exam-ai-target",
        input
      )
    },

    async getExistingExamDraft(input: GetExistingExamDraftInput) {
      return await callSupabaseFunction<GetExistingExamDraftInput, Record<string, unknown>>(
        config,
        "get-existing-exam-draft",
        input
      )
    },

    async exchangeChatGptAppLinkCode(input: ExchangeChatGptAppLinkCodeInput) {
      return await callSupabaseFunction<ExchangeChatGptAppLinkCodeInput, Record<string, unknown>>(
        config,
        "exchange-chatgpt-app-link-code",
        input,
        { sharedSecret: config.resolverSharedSecret }
      )
    },

    async saveExamAiDraft(input: SaveExamAiDraftInput) {
      return await callSupabaseFunction<SaveExamAiDraftInput, Record<string, unknown>>(
        config,
        "save-exam-ai-draft",
        input
      )
    },

    async reviseExamAiDraft(input: ReviseExamAiDraftInput) {
      const existing = await callSupabaseFunction<GetExistingExamDraftInput, Record<string, unknown>>(
        config,
        "get-existing-exam-draft",
        {
          jadwal_id: input.jadwal_id,
          guru_id: input.guru_id,
          kelas_target: input.kelas_target
        }
      )

      const status = String((existing as Record<string, unknown>)?.status || "")
      if (status === "not_found" && input.fail_if_missing) {
        return {
          ok: false,
          status: "not_found",
          error: "Draft lama belum ada, jadi revisi dibatalkan."
        }
      }

      if (status === "not_found") {
        const created = await callSupabaseFunction<SaveExamAiDraftInput, Record<string, unknown>>(
          config,
          "save-exam-ai-draft",
          {
            ...input,
            source: String(input.source || "chatgpt-app-new-from-revise").trim() || "chatgpt-app-new-from-revise"
          }
        )
        return {
          ok: true,
          status: "created",
          draft: (created as Record<string, unknown>)?.draft || null,
          summary: {
            mode: "create_if_missing",
            previous_question_count: 0,
            next_question_count: Array.isArray(input.questions) ? input.questions.length : 0,
            previous_source: null
          }
        }
      }

      const existingParsed = extractExistingDraftPayload(existing)
      const mergedPayload = buildRevisedDraftPayload(
        existing,
        input,
        (String(input.revision_mode || "replace_all").trim() as RevisionMode) || "replace_all"
      )

      const saved = await callSupabaseFunction<SaveExamAiDraftInput, Record<string, unknown>>(
        config,
        "save-exam-ai-draft",
        {
          jadwal_id: input.jadwal_id,
          kelas_target: input.kelas_target,
          guru_id: input.guru_id,
          guru_nama: input.guru_nama,
          instruksi: mergedPayload.instruksi,
          instruksi_lang: mergedPayload.instruksi_lang,
          source: mergedPayload.source,
          sections: mergedPayload.sections,
          questions: mergedPayload.questions
        }
      )

      return {
        ok: true,
        status: "revised",
        draft: (saved as Record<string, unknown>)?.draft || null,
        summary: {
          mode: String(input.revision_mode || "replace_all"),
          previous_question_count: existingParsed.questions.length,
          next_question_count: mergedPayload.questions.length,
          previous_source: String(existingParsed.meta?.source || "") || null
        }
      }
    }
  }
}
