# kouza — read-only financial account aggregator (口座)

`kouza` is **口座** ("bank account"). The name is a metaphor and does not state
the function, so this README states it: kouza is the **read-only aggregation
face for external financial accounts** — bank, securities, card, custody — and
for the statement documents those institutions issue.

It is a **thin edge facade**. This repository holds the Cloudflare Worker and
the ClojureScript (reagent + re-frame + jp-go-dds) appview that sit in front
of the domain; it does not hold the aggregation logic. That lives outside
this repo (see [Boundaries](#boundaries)).

## The guardrail is the point

kouza reads. It never moves money.

> read-only; no payments, transfers, withdrawals, FX, or securities orders

This is declared in three places that must agree — `src/app.ts` (`/health`
response), `kotodama.jsonld` (`convoSystemPrompt`, `complianceFrameworks`), and
the lexicon prefix the facade will forward. The edge enforces the last one
mechanically: it forwards **only** `com.etzhayyim.apps.kouza.*` and answers
`404` to everything else, including transfer- and withdrawal-shaped methods
belonging to sibling apps. `scripts/probe-guardrail.cljk` demonstrates this
against the real module; see the [operator quickstart](docs/operator-quickstart.md).

Data handled here is classified `pii-tier3`.

## Shape

```
Request ──▶ appview/kouza-core-k0uz401/
              ├── src/app.ts ............................. Worker facade → dispatcher.etzhayyim.com
              ├── src/xrpc-mcp-router-proxy.ts ............ preserved, NOT wired (see below)
              └── cljs/ .................................. reagent + re-frame + jp-go-dds appview
```

One forward path is live today:

| Path | Entry | Forwards to |
|---|---|---|
| Worker facade | `src/app.ts` | `DISPATCHER_URL` (default `dispatcher.etzhayyim.com`) |

`src/xrpc-mcp-router-proxy.ts` is the former SvelteKit BFF
(`svelte/src/routes/xrpc/[...path]/+server.ts`) that forwarded to
`AGENTGATEWAY_MCP_ROUTER_URL` (default `mcp.etzhayyim.com`). The SvelteKit
frontend it shipped with was retired in the cljs migration (2026-09-07); the
file was preserved byte-for-byte rather than deleted because it still imports
from `@sveltejs/kit` and does not run as-is. Whether to re-wire it onto a
plain Worker entry is left as an undecided product question — see the header
comment in that file.

Actor identity is `did:web:kouza.etzhayyim.com`, nanoid `k0uz401`.

### Collections

The five lexicon collections kouza subscribes to, all under
`com.etzhayyim.apps.kouza.`:

`institutionConnection` · `financialAccount` · `externalTransaction` ·
`accountDocument` · `syncRun`

Declared capabilities: `bpmn-dispatch`, `connection-registry`,
`statement-import`, `account-documents`, `kaikei-bank-transaction-candidates`.

## Boundaries

- **Aggregation logic is not here.** `kotodama.jsonld` points at
  `40-engine/kotoba/crates/kotoba-kotodama/py/src/kotodama/ingest/kouza.py` and
  the BPMN contracts under `00-contracts/bpmn/com/etzhayyim/kouza`, both in
  `etzhayyim/root`. This repo is the edge only.
- **`kaikei` (会計, accounting) is a different app.** kouza produces
  *candidates* for it (`kaikei-bank-transaction-candidates`); it does not post
  entries. The facade rejects `com.etzhayyim.apps.kaikei.*` outright.

## Current state — read before operating

This repository was extracted from `etzhayyim/root`
(`60-apps/etzhayyim-project-kouza`, 12 files) on 2026-05-21 and has **one
commit**. As measured on 2026-08-14:

- `kouza.etzhayyim.com` — **does not resolve**. Nothing is deployed.
- `dispatcher.etzhayyim.com` and `mcp.etzhayyim.com` — **do not resolve**.
  Both upstreams this facade forwards to are absent.
- `etzhayyim.com` itself does resolve.

So the facade is **not operable end-to-end today**. What you can still verify
locally is real and worth verifying: that it compiles, and that its containment
boundary closes. Both are covered by the
[operator quickstart](docs/operator-quickstart.md).

Provenance and the extraction contract are in `migration.edn`; architecture and
the reasoning behind the boundary are in
[`docs/adr/0001-architecture.md`](docs/adr/0001-architecture.md).

## Licence

Apache 2.0 with the etzhayyim Charter Compliance Rider v3.1. See `NOTICE`.
