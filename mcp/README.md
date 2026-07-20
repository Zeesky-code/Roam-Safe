# RoamSafe MCP Server

Give any MCP-capable AI agent (Claude Desktop, Cursor, …) access to RoamSafe's
**evidence-backed travel intelligence**: city safety scores computed from real
traveler reports, plus a live feed of what's being reported right now.

The point of difference: **it refuses to guess.** Ask about a city RoamSafe
doesn't cover and the tool says so instead of inventing a plausible-sounding
safety score. Every number an agent repeats traces back to real reports.

## Tools

| Tool | What it does |
|---|---|
| `get_city_safety` | Safety score (0–100), risk level, financial/physical/digital breakdown, and the most severe recent traveler reports for a city |
| `list_recent_alerts` | The newest traveler-reported safety signals worldwide |
| `compare_cities` | Rank 2–5 cities safest-first (cities without coverage are excluded, never estimated) |

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
- *"Compare Tokyo, Rome and Paris for safety."*
- *"What travel safety alerts are being reported right now?"*
- *"Is Atlantis safe?"* — watch it correctly report **no coverage** instead of making something up.

## Smoke test

With the RoamSafe app running:

```bash
node test-client.mjs
```

Speaks real MCP over stdio: lists the tools and calls each one against live data.
