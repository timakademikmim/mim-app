export async function callSupabaseFunction(config, functionName, payload, options = {}) {
    const response = await fetch(`${config.supabaseFunctionsBaseUrl}/${functionName}`, {
        method: "POST",
        headers: {
            "content-type": "application/json",
            "apikey": config.supabaseAnonKey,
            "authorization": `Bearer ${config.supabaseAnonKey}`
        },
        body: JSON.stringify({
            shared_secret: String(options.sharedSecret || config.sharedSecret || "").trim(),
            ...payload
        })
    });
    const text = await response.text();
    let body = null;
    try {
        body = text ? JSON.parse(text) : null;
    }
    catch (_error) {
        throw new Error(`${functionName} mengembalikan respons non-JSON: ${text}`);
    }
    if (!response.ok) {
        const message = typeof body === "object" && body && "error" in body
            ? String(body.error || "Unknown error")
            : `HTTP ${response.status}`;
        throw new Error(`${functionName} gagal: ${message}`);
    }
    return body;
}
