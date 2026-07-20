# RoamSafe MCP Server

Give any MCP-capable AI agent (Claude Desktop, Cursor, …) access to RoamSafe's
**evidence-backed travel intelligence**: city safety scores computed from real
traveler reports, plus a live feed of what's being reported right now.

The point of difference: **it refuses to guess.** Ask about a city RoamSafe
doesn't cover and the tool says so instead of inventing a plausible-sounding
safety score. Every number an agent repeats traces back to real reports.

## Tools

All data tools return **both readable text and `structuredContent` JSON**, so an
agent can reason over the numbers instead of parsing prose.

| Tool | What it does |
|---|---|
| `list_covered_cities` | Every covered city, safest first, with score + report count. Call this first so you pick real cities instead of guessing. Optional `country` filter. |
| `get_city_safety` | Full profile: score, risk level, **risk split by concern** (theft/financial/harassment/transport/digital), **neighborhood scores**, **night-time risk + where it clusters**, and **evidence metadata** (report count, date range, confidence) |
| `get_neighborhood_safety` | District-level scores within a city, safest first, with report and night-incident counts |
| `list_recent_alerts` | Newest traveler-reported signals worldwide; optional `city` filter |
| `street_intelligence` | **One specific street, square or district**: its own risk score, concerns reported there, prevention tips travelers gave, better-scoring areas nearby, and the country's emergency number. Disambiguates when a name occurs in several cities |
| `get_emergency_numbers` | Emergency numbers for a country, from structured official data. Refuses to guess — a wrong number costs someone time in a crisis |
| `compare_cities` | Rank up to **10** cities, either overall or by a specific `concern`. Explains *why* the top beats the bottom, and suggests real alternatives for uncovered cities |

### What it deliberately won't do

RoamSafe only reports what its data supports. These are **not** available because
the underlying data doesn't exist — the tools will say so rather than guess:

- **Historical trends / "is it improving?"** — daily score snapshots now run, but a trend needs days of history before it can be shown
- **Seasonality** ("August protests") — report timestamps aren't reliable enough
- **Population / region filters** — no population data; country is unknown for many cities
- **Non-safety criteria** ("safety + nightlife") — RoamSafe has no nightlife data

## Setup

```bash
cd mcp
npm install
npm run build
```

Then add it to your client. **Claude Desktop** —
`~/Library/Application Support/Claude/claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "roamsafe": {
      "command": "/opt/homebrew/bin/node",
      "args": ["/absolute/path/to/Globe-Trotter/mcp/dist/index.js"],
      "env": {
        "ROAMSAFE_API_URL": "http://localhost:8080",
        "ROAMSAFE_API_KEY": "your-api-key"
      }
    }
  }
}
```

**Cursor** — same shape in `.cursor/mcp.json`. Restart the client after editing.

> Use an **absolute path** for `command`. MCP clients spawn servers with a
> minimal `PATH` and often won't find a shell-managed (nvm) node. Any Node >= 14
> works — the server uses `node:http`, not global `fetch`.

### Environment

| Variable | Default | Notes |
|---|---|---|
| `ROAMSAFE_API_URL` | `http://localhost:8080` | Point at your deployed instance in production |
| `ROAMSAFE_API_KEY` | `roamsafe-secret-key-123` | Sent as the `X-API-KEY` header |

The RoamSafe app must be running and reachable at `ROAMSAFE_API_URL`.

## Try it

Once connected, ask your agent:

- *"Using RoamSafe, how safe is Paris and what scams should I watch for?"*
- *"Compare Tokyo, Rome and Paris for theft risk specifically."*
- *"Which neighborhoods in Barcelona should I avoid after dark?"*
- *"What cities does RoamSafe actually cover?"*
- *"What travel safety alerts are being reported right now?"*
- *"Is Atlantis safe?"* — watch it correctly report **no coverage** instead of making something up.

## Smoke test

With the RoamSafe app running:

```bash
node test-client.mjs
```

Speaks real MCP over stdio: lists the tools and calls each one against live data.
