import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StreamableHTTPClientTransport } from "@modelcontextprotocol/sdk/client/streamableHttp.js";
async function main() {
    const baseUrl = String(process.env.MCP_HTTP_BASE_URL || "http://127.0.0.1:3030/mcp").trim();
    const client = new Client({
        name: "mim-exam-ai-http-smoke",
        version: "0.1.0"
    });
    const transport = new StreamableHTTPClientTransport(new URL(baseUrl));
    await client.connect(transport);
    try {
        const tools = await client.listTools();
        console.log(JSON.stringify({
            ok: true,
            base_url: baseUrl,
            tool_count: Array.isArray(tools.tools) ? tools.tools.length : 0,
            tool_names: Array.isArray(tools.tools)
                ? tools.tools.map(tool => tool.name)
                : []
        }, null, 2));
    }
    finally {
        await client.close();
    }
}
main().catch(error => {
    console.error(JSON.stringify({
        ok: false,
        error: String(error?.message || error)
    }, null, 2));
    process.exitCode = 1;
});
