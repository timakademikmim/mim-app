import "dotenv/config";
import express from "express";
import { StreamableHTTPServerTransport } from "@modelcontextprotocol/sdk/server/streamableHttp.js";
import { createExamDraftMcpServerFromEnv } from "./server-factory.js";
import { loadConfig } from "./config.js";
const config = loadConfig();
const app = express();
app.use(express.json({ limit: "2mb" }));
app.get("/", (_req, res) => {
    res.json({
        ok: true,
        name: "mim-exam-ai-drafts",
        transport: "streamable-http",
        mcp_path: config.httpPath,
        note: "Gunakan endpoint MCP pada path yang disediakan. Untuk testing lokal, tunnel server ini dengan HTTPS."
    });
});
app.get("/healthz", (_req, res) => {
    res.json({
        ok: true,
        status: "healthy"
    });
});
app.all(config.httpPath, async (req, res) => {
    const method = String(req.method || "").toUpperCase();
    if (method !== "POST" && method !== "GET" && method !== "DELETE") {
        res.status(405).json({
            jsonrpc: "2.0",
            error: {
                code: -32000,
                message: "Method not allowed."
            },
            id: null
        });
        return;
    }
    const server = createExamDraftMcpServerFromEnv();
    try {
        const transport = new StreamableHTTPServerTransport({
            sessionIdGenerator: undefined
        });
        await server.connect(transport);
        await transport.handleRequest(req, res, req.body);
        res.on("close", () => {
            transport.close().catch(() => { });
            server.close().catch(() => { });
        });
    }
    catch (error) {
        console.error("Error handling MCP HTTP request:", error);
        if (!res.headersSent) {
            res.status(500).json({
                jsonrpc: "2.0",
                error: {
                    code: -32603,
                    message: "Internal server error"
                },
                id: null
            });
        }
    }
});
app.listen(config.httpPort, () => {
    console.log(`MCP HTTP server ready on http://localhost:${config.httpPort}${config.httpPath}`);
    console.log("Health endpoint: http://localhost:" + config.httpPort + "/healthz");
});
