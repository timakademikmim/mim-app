import "dotenv/config";
import { loadConfig } from "./config.js";
import { createToolHandlers } from "./tool-handlers.js";
function requiredEnv(name, fallback = "") {
    const value = String(process.env[name] || fallback).trim();
    if (!value)
        throw new Error(`${name} wajib diisi untuk smoke test`);
    return value;
}
async function run() {
    const config = loadConfig();
    const handlers = createToolHandlers(config);
    const externalSubject = String(process.env.SMOKE_EXTERNAL_SUBJECT || "").trim();
    const linkCode = String(process.env.SMOKE_LINK_CODE || "").trim();
    const smokeDisplayName = String(process.env.SMOKE_DISPLAY_NAME || "").trim();
    const smokeEmail = String(process.env.SMOKE_EMAIL || "").trim();
    const provider = String(process.env.SMOKE_PROVIDER || "chatgpt").trim() || "chatgpt";
    let guruId = String(process.env.SMOKE_GURU_ID || "").trim();
    const kelas = requiredEnv("SMOKE_KELAS");
    const mapel = requiredEnv("SMOKE_MAPEL");
    const tanggal = requiredEnv("SMOKE_TANGGAL");
    const namaUjian = String(process.env.SMOKE_NAMA_UJIAN || "").trim();
    const jenis = String(process.env.SMOKE_JENIS || "").trim();
    const enableSave = String(process.env.SMOKE_ENABLE_SAVE || "").trim() === "1";
    const enableRevise = String(process.env.SMOKE_ENABLE_REVISE || "").trim() === "1";
    const revisionMode = String(process.env.SMOKE_REVISION_MODE || "replace_all").trim() || "replace_all";
    if (externalSubject && linkCode) {
        console.log("0. Menukar kode tautan ChatGPT...");
        const exchangeResult = await handlers.exchangeChatGptAppLinkCode({
            provider,
            code: linkCode,
            external_subject: externalSubject,
            display_name: smokeDisplayName || undefined,
            email: smokeEmail || undefined
        });
        console.log(JSON.stringify(exchangeResult, null, 2));
    }
    if (externalSubject) {
        console.log("0. Mencari link user ChatGPT ke guru...");
        const resolveResult = await handlers.resolveChatGptAppGuru({
            provider,
            external_subject: externalSubject
        });
        console.log(JSON.stringify(resolveResult, null, 2));
        if (String(resolveResult?.status || "") === "linked") {
            const guru = (resolveResult?.guru || {});
            guruId = String(guru.guru_id || "").trim();
        }
    }
    guruId = requiredEnv("SMOKE_GURU_ID", guruId);
    console.log("1. Mencari target ujian...");
    const targetResult = await handlers.findExamAiTarget({
        guru_id: guruId,
        kelas,
        mapel,
        tanggal,
        nama_ujian: namaUjian || undefined,
        jenis: jenis || undefined
    });
    console.log(JSON.stringify(targetResult, null, 2));
    if (String(targetResult?.status || "") !== "found") {
        console.log("Smoke test berhenti: target ujian belum tunggal.");
        return;
    }
    const target = (targetResult?.target || {});
    const jadwalId = String(target.jadwal_id || "").trim();
    const kelasTarget = String(target.kelas_target || "").trim();
    console.log("\n2. Membaca draft yang sudah ada...");
    const existingDraft = await handlers.getExistingExamDraft({
        jadwal_id: jadwalId,
        guru_id: guruId,
        kelas_target: kelasTarget || undefined
    });
    console.log(JSON.stringify(existingDraft, null, 2));
    if (!enableSave && !enableRevise) {
        console.log("\n3. Save/revise dilewati. Set SMOKE_ENABLE_SAVE=1 atau SMOKE_ENABLE_REVISE=1 untuk menguji tulis draft.");
        return;
    }
    if (enableRevise) {
        console.log(`\n3. Merevisi draft dengan mode ${revisionMode}...`);
        const reviseResult = await handlers.reviseExamAiDraft({
            jadwal_id: jadwalId,
            kelas_target: kelasTarget || undefined,
            guru_id: guruId,
            guru_nama: String(process.env.SMOKE_GURU_NAMA || "Guru Uji").trim() || "Guru Uji",
            instruksi: "Kerjakan soal berikut dengan teliti.",
            instruksi_lang: "ID",
            source: "chatgpt-app-local-revision-smoke",
            revision_mode: revisionMode === "append_questions" ? "append_questions" : "replace_all",
            fail_if_missing: false,
            sections: [
                {
                    type: "pilihan-ganda",
                    start: 1,
                    end: 1,
                    count: 1,
                    instruction: "Pilih jawaban yang paling tepat.",
                    score: null
                }
            ],
            questions: [
                {
                    no: 1,
                    type: "pilihan-ganda",
                    text: `Soal revisi lokal untuk ${mapel} kelas ${kelas}.`,
                    options: {
                        a: "Pilihan A",
                        b: "Pilihan B",
                        c: "Pilihan C",
                        d: "Pilihan D"
                    },
                    answer: "A"
                }
            ]
        });
        console.log(JSON.stringify(reviseResult, null, 2));
        return;
    }
    console.log("\n3. Menyimpan draft uji ringan...");
    const saveResult = await handlers.saveExamAiDraft({
        jadwal_id: jadwalId,
        kelas_target: kelasTarget || undefined,
        guru_id: guruId,
        guru_nama: String(process.env.SMOKE_GURU_NAMA || "Guru Uji").trim() || "Guru Uji",
        instruksi: "Kerjakan soal berikut dengan teliti.",
        instruksi_lang: "ID",
        source: "chatgpt-app-local-smoke",
        sections: [
            {
                type: "pilihan-ganda",
                start: 1,
                end: 1,
                count: 1,
                instruction: "Pilih jawaban yang paling tepat.",
                score: null
            }
        ],
        questions: [
            {
                no: 1,
                type: "pilihan-ganda",
                text: `Soal uji lokal untuk ${mapel} kelas ${kelas}.`,
                options: {
                    a: "Pilihan A",
                    b: "Pilihan B",
                    c: "Pilihan C",
                    d: "Pilihan D"
                },
                answer: "A"
            }
        ]
    });
    console.log(JSON.stringify(saveResult, null, 2));
}
run().catch(error => {
    console.error(error);
    process.exitCode = 1;
});
