/**
 * Smoke test: speaks real MCP over stdio to the server, lists tools and calls
 * each one. Run with the RoamSafe app up:  node test-client.mjs
 */
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";

const transport = new StdioClientTransport({
  command: "node",
  args: ["dist/index.js"],
  env: {
    ...process.env,
    ROAMSAFE_API_URL: process.env.ROAMSAFE_API_URL ?? "http://localhost:8080",
    ROAMSAFE_API_KEY: process.env.ROAMSAFE_API_KEY ?? "roamsafe-secret-key-123",
  },
});

const client = new Client({ name: "smoke-test", version: "1.0.0" });
await client.connect(transport);

const { tools } = await client.listTools();
console.log("TOOLS:", tools.map((t) => t.name).join(", "));

async function call(name, args) {
  const r = await client.callTool({ name, arguments: args });
  const body = r.content.map((c) => c.text).join("\n");
  const hasJson = r.structuredContent ? " [+structuredContent JSON]" : " [text only]";
  console.log(`\n===== ${name}(${JSON.stringify(args)})${hasJson} =====\n${body.slice(0, 900)}`);
}

await call("list_covered_cities", { limit: 5 });
await call("get_city_safety", { city: "Paris" });
await call("get_city_safety", { city: "Atlantis" }); // expect: no coverage + alternatives
await call("get_neighborhood_safety", { city: "Barcelona" });
await call("list_recent_alerts", { limit: 2, city: "Rome" });
await call("compare_cities", { cities: ["Paris", "Rome", "Tokyo", "Madrid"], concern: "theft" });

await client.close();
console.log("\nSMOKE TEST DONE");
