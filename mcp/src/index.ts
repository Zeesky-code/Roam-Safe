#!/usr/bin/env node
/**
 * RoamSafe MCP server.
 *
 * Exposes RoamSafe's evidence-backed travel intelligence to AI agents over the
 * Model Context Protocol. Every tool is a thin wrapper over the RoamSafe REST
 * API, so the agent only ever sees data computed from real traveler reports and
 * official advisories. When we have no data for a place, the tools say so
 * rather than guessing - RoamSafe summarizes, it never invents.
 *
 * Tools return both readable text and `structuredContent` JSON so an agent can
 * reason over the numbers instead of parsing prose.
 */
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import http from "node:http";
import https from "node:https";
import { URL } from "node:url";

const API_URL = (process.env.ROAMSAFE_API_URL ?? "http://localhost:8080").replace(/\/$/, "");
const API_KEY = process.env.ROAMSAFE_API_KEY ?? "roamsafe-secret-key-123";

type Neighborhood = { name: string; score: number; reports: number; nightIncidents: number };
type CategoryStat = { category: string; reports: number; avgSeverity: number; riskScore: number };
type Coverage = {
  totalReports: number;
  recentReports: number;
  confidencePct: number;
  oldestReport: string | null;
  newestReport: string | null;
};
type CityRisk = {
  city: string;
  country: string;
  overallScore: number;
  riskLevel: string;
  riskBreakdown: { financial: number; physical: number; digital: number };
  coverage: Coverage;
  categoryBreakdown: CategoryStat[];
  neighborhoods: Neighborhood[];
  nightRisk: { nightIncidentShare: number; topNightAreas: string[] };
  summary: string;
  latestAlerts: Array<{
    name: string;
    description: string;
    neighborhood?: string;
    severityScore: number;
    category?: string;
    preventionTips?: string;
  }>;
};
type CityListItem = { city: string; country?: string; score?: number; riskLevel?: string; reports: number };
type Report = {
  name: string;
  description: string;
  city: string;
  neighborhood?: string;
  severityScore: number;
  category?: string;
};

/**
 * GET JSON from the RoamSafe API.
 *
 * Deliberately uses node:http(s) rather than global fetch: MCP clients spawn
 * this server with their own (sometimes older) Node runtime, and `fetch` is
 * only global on Node >= 18. This works everywhere and adds no dependencies.
 */
function api(path: string): Promise<{ ok: true; data: unknown } | { ok: false; status: number }> {
  return new Promise((resolve) => {
    let url: URL;
    try {
      url = new URL(`${API_URL}${path}`);
    } catch {
      resolve({ ok: false, status: 0 });
      return;
    }
    const client = url.protocol === "https:" ? https : http;
    const req = client.request(
      url,
      {
        method: "GET",
        headers: { "X-API-KEY": API_KEY, "User-Agent": "roamsafe-mcp/1.0", Accept: "application/json" },
        timeout: 20_000,
      },
      (res) => {
        const status = res.statusCode ?? 0;
        let body = "";
        res.setEncoding("utf8");
        res.on("data", (chunk) => (body += chunk));
        res.on("end", () => {
          if (status < 200 || status >= 300) {
            resolve({ ok: false, status });
            return;
          }
          try {
            resolve({ ok: true, data: JSON.parse(body) });
          } catch {
            resolve({ ok: false, status: 0 });
          }
        });
      }
    );
    req.on("timeout", () => req.destroy(new Error("timeout")));
    req.on("error", () => resolve({ ok: false, status: 0 }));
    req.end();
  });
}

/** Text-only result. */
const text = (s: string) => ({ content: [{ type: "text" as const, text: s }] });

/** Text + machine-readable JSON, so agents can reason over real numbers. */
const rich = (s: string, structured: Record<string, unknown>) => ({
  content: [{ type: "text" as const, text: s }],
  structuredContent: structured,
});

const apiError = (status: number, what: string) =>
  status === 0
    ? `Cannot reach the RoamSafe API at ${API_URL} (${what}). Is the RoamSafe app running, ` +
      `and is ROAMSAFE_API_URL correct? Do not answer from memory - report that the tool is unavailable.`
    : `RoamSafe API error (HTTP ${status}) while ${what}.`;

/** Covered cities, used for discovery and for suggesting real alternatives. */
async function coveredCities(): Promise<CityListItem[]> {
  const r = await api("/api/v1/cities");
  return r.ok ? (r.data as CityListItem[]) : [];
}

/** Suggest alternatives when a city isn't covered: same country first. */
async function suggestAlternatives(city: string): Promise<string> {
  const all = await coveredCities();
  if (!all.length) return "";
  const target = all.find((c) => c.city.toLowerCase() === city.toLowerCase());
  const sameCountry = target?.country
    ? all.filter((c) => c.country === target.country && c.city !== target.city)
    : [];
  const pick = (sameCountry.length ? sameCountry : all).filter((c) => c.score != null).slice(0, 5);
  if (!pick.length) return "";
  return (
    `\n\nCovered alternatives you could ask about instead: ` +
    pick.map((c) => `${c.city} (${c.score}/100)`).join(", ") +
    `.`
  );
}

const server = new McpServer({ name: "roamsafe", version: "1.1.0" });

/* ------------------------------ city safety ------------------------------ */

server.registerTool(
  "get_city_safety",
  {
    title: "Get city safety profile",
    description:
      "Full evidence-backed safety profile for a city: overall score (0-100), risk level, " +
      "risk split by concern (theft/scams/harassment/transport...), neighborhood-level scores, " +
      "night-time risk and the areas it clusters in, how much evidence backs the numbers " +
      "(report count, date range, confidence), and the most severe recent reports. " +
      "Use for 'is X safe', 'what scams', 'which areas to avoid', or to justify a claim with evidence. " +
      "Returns an explicit no-coverage answer (plus real alternatives) if the city isn't covered.",
    inputSchema: { city: z.string().describe("City name, e.g. 'Tokyo' or 'Mexico City'") },
  },
  async ({ city }) => {
    const r = await api(`/api/v1/risk/city/${encodeURIComponent(city)}`);
    if (!r.ok) {
      if (r.status === 404) {
        const alts = await suggestAlternatives(city);
        return text(
          `RoamSafe has no coverage for "${city}", so there is no safety score to report. ` +
            `Do not infer or estimate one - say coverage is missing.${alts}`
        );
      }
      return text(apiError(r.status, `looking up "${city}"`));
    }
    const d = r.data as CityRisk;
    const cov = d.coverage;
    const hoods = (d.neighborhoods ?? []).filter((n) => n.reports > 0);
    const safest = hoods.slice(0, 3);
    const riskiest = [...hoods].reverse().slice(0, 3);
    const cats = d.categoryBreakdown ?? [];

    const lines: string[] = [];
    lines.push(`RoamSafe safety profile for ${d.city}${d.country && d.country !== "Unknown" ? `, ${d.country}` : ""}`);
    lines.push("");
    lines.push(`Overall: ${d.overallScore}/100 (${d.riskLevel} risk)`);
    if (cov) {
      lines.push(
        `Evidence: ${cov.totalReports} reports (${cov.recentReports} in the last 6 months), ` +
          `${cov.confidencePct}% confidence` +
          (cov.oldestReport && cov.newestReport ? `, spanning ${cov.oldestReport} to ${cov.newestReport}` : "")
      );
    }
    if (cats.length) {
      lines.push("");
      lines.push("By concern (lower score = riskier):");
      cats.forEach((c) =>
        lines.push(`- ${c.category}: ${c.riskScore}/100 from ${c.reports} report(s), avg severity ${c.avgSeverity}/10`)
      );
    }
    if (riskiest.length) {
      lines.push("");
      lines.push(`Riskiest areas reported: ${riskiest.map((n) => `${n.name} (${n.score}/100)`).join(", ")}`);
      if (safest.length) {
        lines.push(`Calmer areas reported: ${safest.map((n) => `${n.name} (${n.score}/100)`).join(", ")}`);
      }
    }
    if (d.nightRisk) {
      lines.push("");
      lines.push(
        `Night risk: ${d.nightRisk.nightIncidentShare}% of reports are night-time incidents` +
          (d.nightRisk.topNightAreas?.length
            ? `; concentrated in ${d.nightRisk.topNightAreas.join(", ")}`
            : "")
      );
    }
    const alerts = (d.latestAlerts ?? []).slice(0, 5);
    if (alerts.length) {
      lines.push("");
      lines.push("Most severe recent reports:");
      alerts.forEach((a) =>
        lines.push(
          `- ${a.name} (severity ${a.severityScore}/10${a.neighborhood ? `, ${a.neighborhood}` : ""})\n  ${a.description}` +
            (a.preventionTips ? `\n  Avoid it by: ${a.preventionTips}` : "")
        )
      );
    }
    lines.push("");
    lines.push(
      `Source: computed from real traveler reports. Scores reflect recency-weighted severity, not report volume.`
    );

    return rich(lines.join("\n"), {
      city: d.city,
      country: d.country,
      overallScore: d.overallScore,
      riskLevel: d.riskLevel,
      coverage: cov,
      categoryBreakdown: cats,
      neighborhoods: hoods,
      nightRisk: d.nightRisk,
    });
  }
);

/* --------------------------- covered cities ------------------------------ */

server.registerTool(
  "list_covered_cities",
  {
    title: "List cities RoamSafe covers",
    description:
      "Every city RoamSafe has data for, safest first, with score and report count. " +
      "Call this FIRST when you need to pick or suggest destinations, so you compare " +
      "real covered cities instead of guessing names. Supports an optional country filter.",
    inputSchema: {
      country: z.string().optional().describe("Optional country filter, e.g. 'Italy'"),
      limit: z.number().int().min(1).max(200).default(30).describe("Max cities to return"),
    },
  },
  async ({ country, limit }) => {
    const all = await coveredCities();
    if (!all.length) return text(apiError(0, "listing covered cities"));
    const filtered = country
      ? all.filter((c) => (c.country ?? "").toLowerCase() === country.toLowerCase())
      : all;
    if (!filtered.length) {
      const known = [...new Set(all.map((c) => c.country).filter(Boolean))].join(", ");
      return text(`No covered cities in "${country}". Countries with coverage: ${known || "unknown"}.`);
    }
    const rows = filtered.slice(0, limit ?? 30);
    const list = rows
      .map(
        (c, i) =>
          `${i + 1}. ${c.city}${c.country ? ` (${c.country})` : ""} - ` +
          (c.score != null ? `${c.score}/100 ${c.riskLevel}` : "not scored yet") +
          `, ${c.reports} reports`
      )
      .join("\n");
    return rich(`${rows.length} RoamSafe-covered cities, safest first:\n\n${list}`, {
      count: rows.length,
      cities: rows,
    });
  }
);

/* ------------------------- neighborhood detail --------------------------- */

server.registerTool(
  "get_neighborhood_safety",
  {
    title: "Neighborhood-level safety for a city",
    description:
      "Neighborhood/district safety scores within one city, safest first, with how many " +
      "reports and night-time incidents each has. Use for 'which area should I stay in', " +
      "'which neighborhoods to avoid', or 'where is risky after dark'. Only areas that " +
      "travelers actually named are listed - nothing is inferred.",
    inputSchema: { city: z.string().describe("City name, e.g. 'Barcelona'") },
  },
  async ({ city }) => {
    const r = await api(`/api/v1/risk/city/${encodeURIComponent(city)}`);
    if (!r.ok) {
      if (r.status === 404) {
        const alts = await suggestAlternatives(city);
        return text(`RoamSafe has no coverage for "${city}", so no neighborhood data exists.${alts}`);
      }
      return text(apiError(r.status, `loading neighborhoods for "${city}"`));
    }
    const d = r.data as CityRisk;
    const hoods = d.neighborhoods ?? [];
    if (!hoods.length) {
      return text(
        `RoamSafe covers ${d.city} (${d.overallScore}/100) but no reports there name a specific ` +
          `neighborhood, so there is no district-level breakdown. Do not invent one.`
      );
    }
    const list = hoods
      .map(
        (n) =>
          `- ${n.name}: ${n.score}/100 (${n.reports} report${n.reports === 1 ? "" : "s"}` +
          `${n.nightIncidents ? `, ${n.nightIncidents} at night` : ""})`
      )
      .join("\n");
    const night = d.nightRisk?.topNightAreas ?? [];
    return rich(
      `Neighborhood safety in ${d.city}, safest first:\n\n${list}\n\n` +
        (night.length ? `Areas where night-time incidents cluster: ${night.join(", ")}\n\n` : "") +
        `Based only on areas travelers named in their reports.`,
      { city: d.city, neighborhoods: hoods, nightRisk: d.nightRisk }
    );
  }
);

/* ------------------------------ recent feed ------------------------------ */

server.registerTool(
  "list_recent_alerts",
  {
    title: "List recent global safety alerts",
    description:
      "The newest traveler-reported safety signals worldwide (scams, theft, transport and " +
      "area warnings), most recent first. Use for 'what's happening right now' questions. " +
      "Optionally filter to one city.",
    inputSchema: {
      limit: z.number().int().min(1).max(50).default(10).describe("How many alerts to return (1-50)"),
      city: z.string().optional().describe("Optional: only alerts for this city"),
    },
  },
  async ({ limit, city }) => {
    // Pull extra when filtering client-side so the filter still fills the limit.
    const fetchSize = city ? Math.min(50, (limit ?? 10) * 5) : limit ?? 10;
    const r = await api(`/api/v1/alerts/feed?page=0&size=${fetchSize}`);
    if (!r.ok) return text(apiError(r.status, "loading the alert feed"));
    let rows = r.data as Report[];
    if (city) {
      rows = rows.filter((a) => a.city?.toLowerCase() === city.toLowerCase()).slice(0, limit ?? 10);
      if (!rows.length) {
        return text(`No recent RoamSafe alerts for "${city}" in the latest feed.`);
      }
    }
    if (!rows.length) return text("No alerts in the RoamSafe feed right now.");
    const list = rows
      .map(
        (a) =>
          `- [${a.city}${a.neighborhood ? ` / ${a.neighborhood}` : ""}] ${a.name} ` +
          `(severity ${a.severityScore}/10${a.category ? `, ${a.category}` : ""})\n  ${a.description}`
      )
      .join("\n");
    return rich(`${rows.length} most recent RoamSafe traveler reports:\n\n${list}`, {
      count: rows.length,
      alerts: rows,
    });
  }
);

/* ------------------------------- comparison ------------------------------ */

const CONCERNS = ["overall", "theft", "financial", "harassment", "transport", "digital", "tourism"] as const;

server.registerTool(
  "compare_cities",
  {
    title: "Compare safety across cities",
    description:
      "Rank up to 10 cities by safety, either overall or by a specific concern " +
      "(theft, financial/scams, harassment, transport, digital, tourism). Explains WHY the " +
      "top city beats the bottom one using their category scores. Cities without coverage are " +
      "listed separately with real covered alternatives - never estimated.",
    inputSchema: {
      cities: z.array(z.string()).min(2).max(10).describe("2-10 city names to compare"),
      concern: z
        .enum(CONCERNS)
        .default("overall")
        .describe("Rank by this concern instead of the overall score"),
    },
  },
  async ({ cities, concern }) => {
    const key = concern ?? "overall";
    const results = await Promise.all(
      cities.map(async (city) => ({ city, r: await api(`/api/v1/risk/city/${encodeURIComponent(city)}`) }))
    );

    const covered = results.filter((x) => x.r.ok).map((x) => (x.r as { ok: true; data: CityRisk }).data);
    const missing = results.filter((x) => !x.r.ok && (x.r as { status: number }).status === 404).map((x) => x.city);
    const unreachable = results.every((x) => !x.r.ok && (x.r as { status: number }).status === 0);
    if (unreachable) return text(apiError(0, "comparing cities"));

    // Score for the requested concern; fall back to overall when a city has no
    // reports in that category (never invent a category score).
    const scoreFor = (d: CityRisk) => {
      if (key === "overall") return { value: d.overallScore, exact: true };
      const hit = (d.categoryBreakdown ?? []).find((c) => c.category.toLowerCase() === key.toLowerCase());
      return hit ? { value: hit.riskScore, exact: true } : { value: d.overallScore, exact: false };
    };

    const ranked = covered
      .map((d) => ({ d, ...scoreFor(d) }))
      .sort((a, b) => b.value - a.value);

    if (!ranked.length) {
      const alts = missing.length ? await suggestAlternatives(missing[0]) : "";
      return text(`RoamSafe has no coverage for any of: ${cities.join(", ")}.${alts}`);
    }

    const label = key === "overall" ? "overall safety" : `${key} risk`;
    const table = ranked
      .map(
        (x, i) =>
          `${i + 1}. ${x.d.city} - ${x.value}/100 (${x.d.riskLevel})` +
          (x.exact ? "" : ` [no ${key} reports; overall score used]`)
      )
      .join("\n");

    // Explain the gap between best and worst using their category profiles.
    let why = "";
    if (ranked.length >= 2) {
      const best = ranked[0].d;
      const worst = ranked[ranked.length - 1].d;
      const worstCats = (worst.categoryBreakdown ?? []).slice(0, 2).map((c) => `${c.category} (${c.riskScore}/100)`);
      const bestCats = (best.categoryBreakdown ?? []).slice(0, 2).map((c) => `${c.category} (${c.riskScore}/100)`);
      why =
        `\n\nWhy ${best.city} ranks above ${worst.city}: ` +
        `${worst.city}'s riskiest reported concerns are ${worstCats.join(" and ") || "not broken down"}, ` +
        `versus ${bestCats.join(" and ") || "no notable concerns"} for ${best.city}. ` +
        `${best.city} has ${best.coverage?.totalReports ?? 0} reports backing it, ${worst.city} has ${worst.coverage?.totalReports ?? 0}.`;
    }

    let missingNote = "";
    if (missing.length) {
      const alts = await suggestAlternatives(missing[0]);
      missingNote = `\n\nNo RoamSafe coverage (excluded, not estimated): ${missing.join(", ")}.${alts}`;
    }

    return rich(`RoamSafe comparison by ${label}, safest first:\n\n${table}${why}${missingNote}`, {
      concern: key,
      ranked: ranked.map((x) => ({
        city: x.d.city,
        score: x.value,
        riskLevel: x.d.riskLevel,
        exactConcernMatch: x.exact,
        reports: x.d.coverage?.totalReports ?? 0,
      })),
      noCoverage: missing,
    });
  }
);

const transport = new StdioServerTransport();
await server.connect(transport);
