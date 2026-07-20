#!/usr/bin/env node
/**
 * RoamSafe MCP server.
 *
 * Exposes RoamSafe's evidence-backed travel intelligence to AI agents over the
 * Model Context Protocol. Every tool is a thin wrapper over the RoamSafe REST
 * API, so the agent only ever sees data computed from real traveler reports and
 * official advisories. When we have no data for a place, the tools say so
 * rather than guessing - RoamSafe summarizes, it never invents.
 */
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";

const API_URL = (process.env.ROAMSAFE_API_URL ?? "http://localhost:8080").replace(/\/$/, "");
const API_KEY = process.env.ROAMSAFE_API_KEY ?? "roamsafe-secret-key-123";

type CityRisk = {
  city: string;
  country: string;
  overallScore: number;
  riskLevel: string;
  riskBreakdown: { financial: number; physical: number; digital: number };
  summary: string;
  latestAlerts: Array<{
    name: string;
    description: string;
    neighborhood?: string;
    severityScore: number;
    safetyZone?: string;
    preventionTips?: string;
  }>;
};

type Report = {
  name: string;
  description: string;
  city: string;
  neighborhood?: string;
  severityScore: number;
  safetyZone?: string;
  category?: string;
  createdAt?: string;
};

async function api(path: string): Promise<{ ok: true; data: unknown } | { ok: false; status: number }> {
  const res = await fetch(`${API_URL}${path}`, {
    headers: { "X-API-KEY": API_KEY, "User-Agent": "roamsafe-mcp/1.0" },
  });
  if (!res.ok) return { ok: false, status: res.status };
  return { ok: true, data: await res.json() };
}

/** Plain-text tool result. */
const text = (s: string) => ({ content: [{ type: "text" as const, text: s }] });

const server = new McpServer({ name: "roamsafe", version: "1.0.0" });

server.registerTool(
  "get_city_safety",
  {
    title: "Get city safety profile",
    description:
      "Evidence-backed safety profile for a city: an overall safety score (0-100), " +
      "risk level, a financial/physical/digital risk breakdown, and the most severe " +
      "recent traveler reports. Use this when asked whether a destination is safe, " +
      "what scams to watch for, or to justify a travel-safety claim with evidence. " +
      "Returns 'no coverage' if RoamSafe has no reports for that city.",
    inputSchema: { city: z.string().describe("City name, e.g. 'Tokyo' or 'Mexico City'") },
  },
  async ({ city }) => {
    const r = await api(`/api/v1/risk/city/${encodeURIComponent(city)}`);
    if (!r.ok) {
      if (r.status === 404) {
        return text(
          `RoamSafe has no coverage for "${city}" yet, so there is no safety score to report. ` +
            `Do not infer a score - say coverage is missing.`
        );
      }
      return text(`RoamSafe API error (HTTP ${r.status}) while looking up "${city}".`);
    }
    const d = r.data as CityRisk;
    const alerts = (d.latestAlerts ?? [])
      .slice(0, 5)
      .map(
        (a) =>
          `- ${a.name} (severity ${a.severityScore}/10${a.neighborhood ? `, ${a.neighborhood}` : ""})\n` +
          `  ${a.description}` +
          (a.preventionTips ? `\n  Avoid it by: ${a.preventionTips}` : "")
      )
      .join("\n");

    // Skip the summary when it's the "not generated yet" placeholder - an empty
    // line is better than telling the agent something it can't use.
    const hasSummary = d.summary && !/^no summary available/i.test(d.summary);

    return text(
      `RoamSafe safety profile for ${d.city}${d.country && d.country !== "Unknown" ? `, ${d.country}` : ""}\n\n` +
        `Overall safety score: ${d.overallScore}/100 (${d.riskLevel} risk)\n` +
        `Risk breakdown - financial ${d.riskBreakdown?.financial}, physical ${d.riskBreakdown?.physical}, digital ${d.riskBreakdown?.digital} (higher = safer)\n\n` +
        (hasSummary ? `Summary: ${d.summary}\n\n` : "") +
        (alerts ? `Most severe recent traveler reports:\n${alerts}\n\n` : "") +
        `Source: computed from real traveler reports in RoamSafe. Scores reflect the ` +
        `recency-weighted severity of what travelers actually reported, not report volume.`
    );
  }
);

server.registerTool(
  "list_recent_alerts",
  {
    title: "List recent global safety alerts",
    description:
      "The newest traveler-reported safety signals worldwide (scams, theft, transport " +
      "and area warnings), most recent first. Use this for 'what's happening right now' " +
      "questions or to surface emerging risks across destinations.",
    inputSchema: {
      limit: z.number().int().min(1).max(50).default(10).describe("How many alerts to return (1-50)"),
    },
  },
  async ({ limit }) => {
    const r = await api(`/api/v1/alerts/feed?page=0&size=${limit ?? 10}`);
    if (!r.ok) return text(`RoamSafe API error (HTTP ${r.status}) while loading the alert feed.`);
    const rows = r.data as Report[];
    if (!rows.length) return text("No alerts in the RoamSafe feed right now.");
    const list = rows
      .map(
        (a) =>
          `- [${a.city}${a.neighborhood ? ` / ${a.neighborhood}` : ""}] ${a.name} ` +
          `(severity ${a.severityScore}/10${a.category ? `, ${a.category}` : ""})\n  ${a.description}`
      )
      .join("\n");
    return text(`${rows.length} most recent RoamSafe traveler reports:\n\n${list}`);
  }
);

server.registerTool(
  "compare_cities",
  {
    title: "Compare safety across cities",
    description:
      "Compare RoamSafe safety scores across 2-5 cities side by side, ranked safest first. " +
      "Use this when someone is choosing between destinations. Cities with no RoamSafe " +
      "coverage are listed separately rather than guessed at.",
    inputSchema: {
      cities: z.array(z.string()).min(2).max(5).describe("2-5 city names to compare"),
    },
  },
  async ({ cities }) => {
    const results = await Promise.all(
      cities.map(async (city) => {
        const r = await api(`/api/v1/risk/city/${encodeURIComponent(city)}`);
        return { city, r };
      })
    );
    const scored = results
      .filter((x) => x.r.ok)
      .map((x) => x.r as { ok: true; data: CityRisk })
      .map((x) => x.data)
      .sort((a, b) => b.overallScore - a.overallScore);
    const missing = results.filter((x) => !x.r.ok).map((x) => x.city);

    if (!scored.length) {
      return text(`RoamSafe has no coverage for any of: ${cities.join(", ")}. No comparison is possible.`);
    }
    const table = scored
      .map((d, i) => `${i + 1}. ${d.city} - ${d.overallScore}/100 (${d.riskLevel})`)
      .join("\n");
    return text(
      `RoamSafe safety comparison, safest first:\n\n${table}\n\n` +
        (missing.length ? `No RoamSafe coverage (excluded, not estimated): ${missing.join(", ")}\n\n` : "") +
        `Scores are computed from real traveler reports.`
    );
  }
);

const transport = new StdioServerTransport();
await server.connect(transport);
