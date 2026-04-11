import { callSupabaseFunction } from "./supabase-edge.js";
import { buildRevisedDraftPayload, extractExistingDraftPayload } from "./draft-utils.js";
function findLocalGuruLink(config, provider, externalSubject) {
    return (config.localGuruLinks || []).find(item => (String(item.provider || "chatgpt").trim() === provider &&
        String(item.external_subject || "").trim() === externalSubject &&
        String(item.is_active ?? true).trim() !== "false"));
}
function buildLocalLinkedResponse(localMatch, provider, externalSubject) {
    return {
        ok: true,
        status: "linked",
        guru: {
            ...localMatch,
            provider,
            external_subject: externalSubject,
            metadata_json: {
                source: "local-config",
                ...(localMatch.metadata_json && typeof localMatch.metadata_json === "object" ? localMatch.metadata_json : {})
            }
        }
    };
}
export function createToolHandlers(config) {
    return {
        async resolveChatGptAppGuru(input) {
            const provider = String(input.provider || "chatgpt").trim() || "chatgpt";
            const externalSubject = String(input.external_subject || "").trim();
            const localMatch = findLocalGuruLink(config, provider, externalSubject);
            if (config.resolverMode === "local-only") {
                if (localMatch)
                    return buildLocalLinkedResponse(localMatch, provider, externalSubject);
                return {
                    ok: true,
                    status: "not_linked",
                    guru: null,
                    meta: {
                        source: "local-only",
                        resolver_mode: config.resolverMode
                    }
                };
            }
            const callLiveResolver = async () => await callSupabaseFunction(config, "resolve-chatgpt-app-guru", input, { sharedSecret: config.resolverSharedSecret });
            if (config.resolverMode === "local-first") {
                if (localMatch)
                    return buildLocalLinkedResponse(localMatch, provider, externalSubject);
                return await callLiveResolver();
            }
            if (config.resolverMode === "live-first") {
                try {
                    const liveResult = await callLiveResolver();
                    if (String(liveResult?.status || "") === "linked")
                        return liveResult;
                    if (localMatch)
                        return buildLocalLinkedResponse(localMatch, provider, externalSubject);
                    return liveResult;
                }
                catch (error) {
                    if (localMatch) {
                        return {
                            ...buildLocalLinkedResponse(localMatch, provider, externalSubject),
                            meta: {
                                source: "local-fallback-after-live-error",
                                resolver_mode: config.resolverMode,
                                live_error: String(error?.message || error)
                            }
                        };
                    }
                    throw error;
                }
            }
            return await callLiveResolver();
        },
        async findExamAiTarget(input) {
            return await callSupabaseFunction(config, "find-exam-ai-target", input);
        },
        async getExistingExamDraft(input) {
            return await callSupabaseFunction(config, "get-existing-exam-draft", input);
        },
        async exchangeChatGptAppLinkCode(input) {
            return await callSupabaseFunction(config, "exchange-chatgpt-app-link-code", input, { sharedSecret: config.resolverSharedSecret });
        },
        async saveExamAiDraft(input) {
            return await callSupabaseFunction(config, "save-exam-ai-draft", input);
        },
        async reviseExamAiDraft(input) {
            const existing = await callSupabaseFunction(config, "get-existing-exam-draft", {
                jadwal_id: input.jadwal_id,
                guru_id: input.guru_id,
                kelas_target: input.kelas_target
            });
            const status = String(existing?.status || "");
            if (status === "not_found" && input.fail_if_missing) {
                return {
                    ok: false,
                    status: "not_found",
                    error: "Draft lama belum ada, jadi revisi dibatalkan."
                };
            }
            if (status === "not_found") {
                const created = await callSupabaseFunction(config, "save-exam-ai-draft", {
                    ...input,
                    source: String(input.source || "chatgpt-app-new-from-revise").trim() || "chatgpt-app-new-from-revise"
                });
                return {
                    ok: true,
                    status: "created",
                    draft: created?.draft || null,
                    summary: {
                        mode: "create_if_missing",
                        previous_question_count: 0,
                        next_question_count: Array.isArray(input.questions) ? input.questions.length : 0,
                        previous_source: null
                    }
                };
            }
            const existingParsed = extractExistingDraftPayload(existing);
            const mergedPayload = buildRevisedDraftPayload(existing, input, String(input.revision_mode || "replace_all").trim() || "replace_all");
            const saved = await callSupabaseFunction(config, "save-exam-ai-draft", {
                jadwal_id: input.jadwal_id,
                kelas_target: input.kelas_target,
                guru_id: input.guru_id,
                guru_nama: input.guru_nama,
                instruksi: mergedPayload.instruksi,
                instruksi_lang: mergedPayload.instruksi_lang,
                source: mergedPayload.source,
                sections: mergedPayload.sections,
                questions: mergedPayload.questions
            });
            return {
                ok: true,
                status: "revised",
                draft: saved?.draft || null,
                summary: {
                    mode: String(input.revision_mode || "replace_all"),
                    previous_question_count: existingParsed.questions.length,
                    next_question_count: mergedPayload.questions.length,
                    previous_source: String(existingParsed.meta?.source || "") || null
                }
            };
        }
    };
}
