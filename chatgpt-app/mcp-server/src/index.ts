import "dotenv/config"
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js"
import { createExamDraftMcpServerFromEnv } from "./server-factory.js"

const server = createExamDraftMcpServerFromEnv()
const transport = new StdioServerTransport()

await server.connect(transport)
