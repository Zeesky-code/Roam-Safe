/**
 * Cross-interface integration test.
 *
 * RoamSafe's premise is that the intelligence layer is the product and the
 * website, REST API and MCP server are three interfaces onto the same data. If
 * they ever disagree about a city's score, its emergency number or what's
 * happening there, the premise is broken. This test asserts they don't: it
 * pulls the same facts through all three and fails on any drift.
 *
 * Run against a running server (prod profile, real DB):
 *   ROAMSAFE_API_URL=http://127.0.0.1:8080 \
 *   ROAMSAFE_API_KEY=roamsafe-secret-key-123 \
 *   node cross-interface.test.mjs
 *
 * Exit code is non-zero if any interface drifts, so CI can gate on it.
 */
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";
import http from "node:http";

const API_URL = (process.env.ROAMSAFE_API_URL ?? "http://127.0.0.1:8080").replace(/\/$/, "");
const API_KEY = process.env.ROAMSAFE_API_KEY ?? "roamsafe-secret-key-123";
const CITIES = ["Istanbul", "Tokyo", "Barcelona", "London", "Bangkok"];

let failures = 0;
const ok = (m) => console.log(`  ✓ ${m}`);
const bad = (m) => { console.log(`  ✗ ${m}`); failures++; };

/** GET a path from the server, returning parsed JSON or raw text. */
function get(path, json = true) {
  return new Promise((resolve, reject) => {
    const req = http.request(`${API_URL}${path}`, {
      headers: { "X-API-KEY": API_KEY, Accept: json ? "application/json" : "text/html" },
      timeout: 20000,
    }, (res) => {
      let body = "";
      res.setEncoding("utf8");
      res.on("data", (c) => (body += c));
      res.on("end", () => resolve(json ? JSON.parse(body) : body));
    });
    req.on("timeout", () => req.destroy(new Error("timeout")));
    req.on("error", reject);
    req.end();
  });
}

/** Pull the ring score the website renders for a city. */
function websiteScore(html) {
  const m = html.match(/rs-ring-score"[^>]*>\s*(\d+)/);
  return m ? Number(m[1]) : null;
}

const mcpTransport = new StdioClientTransport({
  command: "node",
  args: ["dist/index.js"],
  env: { ...process.env, ROAMSAFE_API_URL: API_URL, ROAMSAFE_API_KEY: API_KEY },
});
const mcp = new Client({ name: "cross-interface-test", version: "1.0.0" });
await mcp.connect(mcpTransport);

console.log(`Cross-interface consistency against ${API_URL}\n`);

for (const city of CITIES) {
  console.log(city);
  const enc = encodeURIComponent(city);

  const api = await get(`/api/v1/risk/city/${enc}`);
  const html = await get(`/scams?city=${enc}`, false);
  const mcpRes = await mcp.callTool({ name: "get_city_safety", arguments: { city } });
  const mcpData = mcpRes.structuredContent ?? {};

  // 1. The safety score must be identical across all three interfaces.
  const web = websiteScore(html);
  if (api.overallScore === web && api.overallScore === mcpData.overallScore) {
    ok(`score agrees across website/API/MCP (${api.overallScore})`);
  } else {
    bad(`score drift: website=${web} api=${api.overallScore} mcp=${mcpData.overallScore}`);
  }

  // 2. Risk level must match between API and MCP.
  if (api.riskLevel === mcpData.riskLevel) ok(`risk level agrees (${api.riskLevel})`);
  else bad(`risk level drift: api=${api.riskLevel} mcp=${mcpData.riskLevel}`);

  // 3. Live incidents present in the API must appear on the website too.
  const apiInc = (api.liveIncidents ?? []).length;
  const webInc = /data-panel="incidents"/.test(html);
  if (apiInc === 0 || webInc) ok(`incidents consistent (api=${apiInc}, web tab=${webInc})`);
  else bad(`API has ${apiInc} incidents but website shows no incidents tab`);
}

// 4. Emergency numbers: the dedicated endpoint and the MCP tool must agree.
console.log("Emergency numbers");
for (const country of ["Japan", "Türkiye", "United Kingdom"]) {
  const api = await get(`/api/v1/emergency/${encodeURIComponent(country)}`);
  const mcpRes = await mcp.callTool({ name: "get_emergency_numbers", arguments: { country } });
  const text = mcpRes.content?.[0]?.text ?? "";
  if (api.primary && text.includes(api.primary)) ok(`${country}: API and MCP agree on ${api.primary}`);
  else bad(`${country}: API primary=${api.primary} not reflected in MCP text`);
}

await mcp.close();
console.log(`\n${failures === 0 ? "PASS - all interfaces consistent" : `FAIL - ${failures} inconsistency(ies)`}`);
process.exit(failures === 0 ? 0 : 1);
