function normalizeUrl(value) {
    return String(value || "").trim().replace(/\/+$/, "");
}
function normalizeResolverMode(value) {
    const mode = String(value || "").trim().toLowerCase();
    if (mode === "local-only" || mode === "live-first" || mode === "live-only")
        return mode;
    return "local-first";
}
function normalizePort(value) {
    const parsed = Number.parseInt(String(value || "").trim(), 10);
    if (!Number.isFinite(parsed) || parsed <= 0)
        return 3030;
    return parsed;
}
function normalizeHttpPath(value) {
    const raw = String(value || "").trim();
    if (!raw)
        return "/mcp";
    return raw.startsWith("/") ? raw : `/${raw}`;
}
export function loadConfig() {
    const supabaseFunctionsBaseUrl = normalizeUrl(process.env.SUPABASE_FUNCTIONS_BASE_URL ||
        "https://optucpelkueqmlhwlbej.supabase.co/functions/v1");
    const supabaseAnonKey = String(process.env.SUPABASE_ANON_KEY ||
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9wdHVjcGVsa3VlcW1saHdsYmVqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzAxOTY4MTgsImV4cCI6MjA4NTc3MjgxOH0.Vqaey9pcnltu9uRbPk0J-AGWaGDZjQLw92pcRv67GNE").trim();
    const sharedSecret = String(process.env.EXAM_AI_APP_SHARED_SECRET || "").trim();
    const resolverSharedSecret = String(process.env.CHATGPT_APP_GURU_RESOLVER_SHARED_SECRET ||
        sharedSecret).trim();
    const resolverMode = normalizeResolverMode(process.env.CHATGPT_APP_GURU_RESOLVER_MODE || "local-first");
    const httpPort = normalizePort(process.env.MCP_HTTP_PORT || "3030");
    const httpPath = normalizeHttpPath(process.env.MCP_HTTP_PATH || "/mcp");
    const localGuruLinksRaw = String(process.env.CHATGPT_APP_LOCAL_LINKS_JSON || "").trim();
    let localGuruLinks = [];
    if (!supabaseFunctionsBaseUrl) {
        throw new Error("SUPABASE_FUNCTIONS_BASE_URL wajib diisi");
    }
    if (!supabaseAnonKey) {
        throw new Error("SUPABASE_ANON_KEY wajib diisi");
    }
    if (!sharedSecret) {
        throw new Error("EXAM_AI_APP_SHARED_SECRET wajib diisi");
    }
    if (localGuruLinksRaw) {
        try {
            const parsed = JSON.parse(localGuruLinksRaw);
            if (Array.isArray(parsed)) {
                localGuruLinks = parsed.filter(item => item && typeof item === "object");
            }
        }
        catch (error) {
            throw new Error(`CHATGPT_APP_LOCAL_LINKS_JSON tidak valid: ${String(error?.message || error)}`);
        }
    }
    return {
        supabaseFunctionsBaseUrl,
        supabaseAnonKey,
        sharedSecret,
        resolverSharedSecret,
        resolverMode,
        localGuruLinks,
        httpPort,
        httpPath
    };
}
