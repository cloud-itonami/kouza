# ADR-0001: kouza is an edge facade whose only local invariant is the read-only boundary

## Status

Accepted (2026-08-14). Records the architecture of `cloud-itonami/kouza` as
extracted, and fixes the boundary that this repository — and only this
repository — is responsible for holding.

## Context

`kouza` (口座, "bank account") is the read-only aggregation face for external
financial accounts: bank, securities, card, custody, and the statement documents
those institutions issue. It was extracted from `etzhayyim/root`
(`60-apps/etzhayyim-project-kouza`, revision `c9e7df4b`, 12 files, 14,428 bytes)
on 2026-05-21 and has a single commit.

The extraction moved the **edge** and left the **logic** behind.
`kotodama.jsonld` names what stayed in `etzhayyim/root`: the aggregation
implementation (`…/kotodama/ingest/kouza.py`) and the process contracts
(`00-contracts/bpmn/com/etzhayyim/kouza`). What arrived here originally was two
proxies:

| Entry | Forwards to |
|---|---|
| `appview/kouza-core-k0uz401/src/app.ts` (Worker) | `DISPATCHER_URL`, default `dispatcher.etzhayyim.com` |
| `svelte/src/routes/xrpc/[...path]/+server.ts` (BFF) | `AGENTGATEWAY_MCP_ROUTER_URL`, default `mcp.etzhayyim.com` |

**Update (2026-09-07):** the SvelteKit frontend that shipped alongside the
BFF proxy was retired and replaced with a ClojureScript (reagent + re-frame +
jp-go-dds) appview at `appview/kouza-core-k0uz401/cljs/`. The BFF proxy code
itself was preserved byte-for-byte (not deleted) at
`appview/kouza-core-k0uz401/src/xrpc-mcp-router-proxy.ts`, because it still
imports from `@sveltejs/kit` and does not run as-is without the SvelteKit
build. It is **not wired** into `wrangler.jsonc` today, so only one proxy is
live:

| Entry | Forwards to |
|---|---|
| `appview/kouza-core-k0uz401/src/app.ts` (Worker) | `DISPATCHER_URL`, default `dispatcher.etzhayyim.com` |

This creates a documentation hazard that motivated this ADR. A reader landing
here sees a financial application and reasonably assumes the repository governs
financial behaviour. It does not. Almost every guarantee kouza makes about
*what it does with account data* is enforced somewhere else. If this repo
describes itself as "the kouza service", every future reader will look for
controls that were never here and conclude they are missing or, worse, assume
they are present.

Compounding it, the name states nothing: `kouza` is a metaphor, and the
workspace naming rules require such a repository to declare itself in its
README rather than rely on the name.

## Decision

**1. This repository is named and documented as an edge facade, not as the
kouza service.** `README.md` states the function in its first sentence, names
both locations of the real logic, and marks the boundary explicitly. The
quickstart repeats it.

**2. The one invariant that lives here is the containment boundary**, and it is
mechanical rather than prose. The facade forwards only NSIDs under
`com.etzhayyim.apps.kouza.` and answers `404` to everything else. This is what
prevents a read-only aggregator from being used as a path to money-moving
methods belonging to sibling apps — notably `com.etzhayyim.apps.kaikei.*`
(会計, accounting), for which kouza produces *candidates* and never postings.

**3. That invariant is demonstrated, not asserted.**
`scripts/probe-guardrail.cljk` imports the real module and exercises five
requests, including `kaikei.transfer` and a foreign `evil.withdraw`. It exits 1
if any refusal case is not refused. It was verified to discriminate: widening
`NSID_PREFIX` to `com.` makes exactly those two cases fail and the script exit
1, while the unrelated cases stay green.

**4. The undeployed state is documented rather than left to be discovered.**
Measured 2026-08-14, `kouza.etzhayyim.com`, `dispatcher.etzhayyim.com`, and
`mcp.etzhayyim.com` all return `NXDOMAIN`. The quickstart makes checking this a
step rather than an assumption, so the document degrades honestly as the world
changes instead of silently becoming false.

## Consequences

- An operator can verify two real things offline — that the Worker compiles
  strict, and that the boundary closes — without a deployment, an install, or a
  build. Neither check depends on the absent upstreams.
- The `--lib` list in `appview/kouza-core-k0uz401/package.json` was missing the
  iterable helpers, so the repository's only executable script failed with
  `TS2488` on `for (… of url.searchParams)`. Documenting the step required
  fixing it; `WebWorker.Iterable` was added and `npm run typecheck` now exits 0.
  A documented step that does not run is worse than an undocumented one.
- The probe is an operator diagnostic, not a test suite. It lives in `scripts/`,
  runs on demand, and is not wired into CI. Turning the boundary into a standing
  regression check is deliberately left as separate work.
- kouza remains inoperable end-to-end until the upstreams return. This ADR does
  not restore them and does not propose a schedule; it makes their absence
  legible so nobody deploys a financial identity in front of nothing.

## Alternatives considered

- **Describe the repo as the kouza service and document the aggregation
  behaviour.** Rejected: this repository cannot enforce any of it, and the
  description would rot independently of the code it describes.
- **Assert the read-only guardrail in prose only**, as `kotodama.jsonld` already
  does in `convoSystemPrompt`. Rejected: three declarations already exist and
  none of them are checked. A fourth would not have caught a widened prefix.
- **Wire the probe into CI.** Deferred, not rejected. The workspace runs CI on
  the murakumo fleet rather than GitHub Actions, and adding a gate there is a
  larger change than this ADR's scope; the probe was written to exit non-zero so
  that it is ready to become one.
