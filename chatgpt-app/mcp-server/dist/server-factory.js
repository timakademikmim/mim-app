import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import { loadConfig } from "./config.js";
import { createToolHandlers } from "./tool-handlers.js";
function asTextResult(value) {
    const structuredContent = value && typeof value === "object" && !Array.isArray(value)
        ? value
        : { result: value };
    return {
        content: [
            {
                type: "text",
                text: JSON.stringify(value, null, 2)
            }
        ],
        structuredContent
    };
}
export function createExamDraftMcpServer(config) {
    const handlers = createToolHandlers(config);
    const server = new McpServer({
        name: "mim-exam-ai-drafts",
        version: "0.1.0"
    });
    server.registerTool("resolve_chatgpt_app_guru", {
        title: "Cek Guru Tertaut",
        description: "Cari guru yang terhubung ke identitas user ChatGPT agar tool ujian tidak perlu guru_id manual.",
        annotations: {
            readOnlyHint: true,
            openWorldHint: false
        },
        inputSchema: {
            provider: z.string().optional(),
            external_subject: z.string().min(1)
        }
    }, async (input) => asTextResult(await handlers.resolveChatGptAppGuru(input)));
    server.registerTool("find_exam_ai_target", {
        title: "Cari Target Ujian",
        description: "Cari target ujian dari guru, kelas, mapel, dan tanggal sebelum membuat draft soal.",
        annotations: {
            readOnlyHint: true,
            openWorldHint: false
        },
        inputSchema: {
            guru_id: z.string().min(1),
            kelas: z.string().min(1),
            mapel: z.string().min(1),
            tanggal: z.string().min(1),
            nama_ujian: z.string().optional(),
            jenis: z.string().optional()
        }
    }, async (input) => asTextResult(await handlers.findExamAiTarget(input)));
    server.registerTool("get_existing_exam_draft", {
        title: "Baca Draft Ujian",
        description: "Ambil draft ujian yang sudah ada untuk target ujian tertentu sebelum membuat revisi.",
        annotations: {
            readOnlyHint: true,
            openWorldHint: false
        },
        inputSchema: {
            jadwal_id: z.string().min(1),
            guru_id: z.string().min(1),
            kelas_target: z.string().optional()
        }
    }, async (input) => asTextResult(await handlers.getExistingExamDraft(input)));
    server.registerTool("exchange_chatgpt_app_link_code", {
        title: "Tukar Kode Tautan Guru",
        description: "Tukar kode tautan sekali pakai menjadi link permanen antara user ChatGPT dan guru.",
        annotations: {
            readOnlyHint: false,
            openWorldHint: false
        },
        inputSchema: {
            provider: z.string().optional(),
            code: z.string().min(1),
            external_subject: z.string().min(1),
            display_name: z.string().optional(),
            email: z.string().optional()
        }
    }, async (input) => asTextResult(await handlers.exchangeChatGptAppLinkCode(input)));
    server.registerTool("save_exam_ai_draft", {
        title: "Simpan Draft Soal AI",
        description: "Simpan draft soal hasil AI ke editor ujian guru di sistem sekolah.",
        annotations: {
            readOnlyHint: false,
            openWorldHint: false
        },
        inputSchema: {
            jadwal_id: z.string().min(1),
            kelas_target: z.string().optional(),
            guru_id: z.string().min(1),
            guru_nama: z.string().optional(),
            instruksi: z.string().optional(),
            instruksi_lang: z.string().optional(),
            source: z.string().optional(),
            sections: z.array(z.record(z.unknown())).optional(),
            questions: z.array(z.record(z.unknown())).min(1)
        }
    }, async (input) => asTextResult(await handlers.saveExamAiDraft(input)));
    server.registerTool("revise_exam_ai_draft", {
        title: "Revisi Draft Soal AI",
        description: "Perbarui draft soal yang sudah ada dengan mode ganti penuh atau tambah soal.",
        annotations: {
            readOnlyHint: false,
            openWorldHint: false
        },
        inputSchema: {
            jadwal_id: z.string().min(1),
            kelas_target: z.string().optional(),
            guru_id: z.string().min(1),
            guru_nama: z.string().optional(),
            instruksi: z.string().optional(),
            instruksi_lang: z.string().optional(),
            source: z.string().optional(),
            revision_mode: z.enum(["replace_all", "append_questions"]).optional(),
            fail_if_missing: z.boolean().optional(),
            sections: z.array(z.record(z.unknown())).optional(),
            questions: z.array(z.record(z.unknown())).min(1)
        }
    }, async (input) => asTextResult(await handlers.reviseExamAiDraft(input)));
    return server;
}
export function createExamDraftMcpServerFromEnv() {
    return createExamDraftMcpServer(loadConfig());
}
