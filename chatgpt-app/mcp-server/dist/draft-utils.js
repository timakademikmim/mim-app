function normalizeText(value) {
    return String(value ?? "").trim();
}
function normalizeQuestionType(value) {
    const raw = normalizeText(value).toLowerCase();
    if (raw === "benar-salah")
        return "benar-salah";
    if (raw === "esai")
        return "esai";
    if (raw === "isi-titik")
        return "isi-titik";
    return "pilihan-ganda";
}
function renumberQuestions(questions, startNo = 1) {
    return (Array.isArray(questions) ? questions : []).map((question, index) => ({
        ...question,
        no: startNo + index,
        type: normalizeQuestionType(question?.type)
    }));
}
function deriveSectionFromQuestions(questions) {
    if (!questions.length)
        return [];
    const start = Number(questions[0]?.no || 1);
    const end = Number(questions[questions.length - 1]?.no || start);
    return [{
            type: normalizeQuestionType(questions[0]?.type),
            start,
            end,
            count: Math.max(1, questions.length),
            instruction: "",
            score: null
        }];
}
function offsetSections(sections, offset, questionCount, fallbackType = "pilihan-ganda") {
    if (!Array.isArray(sections) || !sections.length) {
        const start = offset + 1;
        return [{
                type: fallbackType,
                start,
                end: start + Math.max(0, questionCount - 1),
                count: Math.max(1, questionCount),
                instruction: "",
                score: null
            }];
    }
    return sections.map(section => {
        const count = Math.max(1, Number(section?.count || 1));
        const originalStart = Math.max(1, Number(section?.start || 1));
        const originalEnd = Math.max(originalStart, Number(section?.end || originalStart + count - 1));
        return {
            ...section,
            type: normalizeQuestionType(section?.type || fallbackType),
            start: originalStart + offset,
            end: originalEnd + offset,
            count
        };
    });
}
function parseInstruksiWithLang(rawValue) {
    const raw = normalizeText(rawValue);
    if (!raw)
        return { instruksi: "", instruksi_lang: "ID" };
    const match = raw.match(/^\[lang=(ID|AR)\]\s*(.*)$/i);
    if (match) {
        return {
            instruksi_lang: String(match[1] || "ID").toUpperCase(),
            instruksi: String(match[2] || "").trim()
        };
    }
    return {
        instruksi_lang: "ID",
        instruksi: raw
    };
}
export function extractExistingDraftPayload(existingDraftResponse) {
    const draft = (existingDraftResponse?.draft || {});
    const parsed = (draft?.parsed_questions_json || {});
    return {
        questions: Array.isArray(parsed?.questions) ? parsed.questions : [],
        sections: Array.isArray(parsed?.sections) ? parsed.sections : [],
        meta: parsed?.meta && typeof parsed.meta === "object" ? parsed.meta : {}
    };
}
export function buildRevisedDraftPayload(existingDraftResponse, incomingDraft, revisionMode) {
    const existingPayload = extractExistingDraftPayload(existingDraftResponse);
    const existingDraft = (existingDraftResponse?.draft || {});
    const existingInstruksiInfo = parseInstruksiWithLang(existingDraft?.instruksi);
    const normalizedIncomingQuestions = renumberQuestions(Array.isArray(incomingDraft.questions) ? incomingDraft.questions : [], 1);
    if (!normalizedIncomingQuestions.length) {
        throw new Error("questions revisi minimal 1 item");
    }
    if (revisionMode === "append_questions") {
        const existingQuestions = renumberQuestions(existingPayload.questions, 1);
        const appendedQuestions = renumberQuestions(normalizedIncomingQuestions, existingQuestions.length + 1);
        const mergedQuestions = [...existingQuestions, ...appendedQuestions];
        const fallbackType = normalizeQuestionType(normalizedIncomingQuestions[0]?.type);
        const appendedSections = offsetSections(Array.isArray(incomingDraft.sections) ? incomingDraft.sections : [], existingQuestions.length, appendedQuestions.length, fallbackType);
        return {
            instruksi: normalizeText(incomingDraft.instruksi) || existingInstruksiInfo.instruksi,
            instruksi_lang: normalizeText(incomingDraft.instruksi_lang).toUpperCase() || existingInstruksiInfo.instruksi_lang,
            source: normalizeText(incomingDraft.source || "chatgpt-app-revision") || "chatgpt-app-revision",
            questions: mergedQuestions,
            sections: [...(Array.isArray(existingPayload.sections) ? existingPayload.sections : []), ...appendedSections]
        };
    }
    const replaceQuestions = normalizedIncomingQuestions;
    const replaceSections = Array.isArray(incomingDraft.sections) && incomingDraft.sections.length
        ? offsetSections(incomingDraft.sections, 0, replaceQuestions.length, normalizeQuestionType(replaceQuestions[0]?.type))
        : deriveSectionFromQuestions(replaceQuestions);
    return {
        instruksi: normalizeText(incomingDraft.instruksi) || existingInstruksiInfo.instruksi,
        instruksi_lang: normalizeText(incomingDraft.instruksi_lang).toUpperCase() || existingInstruksiInfo.instruksi_lang,
        source: normalizeText(incomingDraft.source || "chatgpt-app-revision") || "chatgpt-app-revision",
        questions: replaceQuestions,
        sections: replaceSections
    };
}
