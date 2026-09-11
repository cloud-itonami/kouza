# Operator Quickstart — kouza (口座)

Shortest path from clone to a verified local check of the **kouza** read-only
financial account facade (`cloud-itonami/kouza`, actor
`did:web:kouza.etzhayyim.com`).

Read this first: **nothing is deployed.** `kouza.etzhayyim.com` does not
resolve, and neither do the two upstreams this facade forwards to. You cannot
exercise kouza end-to-end today. You *can* verify that it compiles and that its
containment boundary closes, and those are the two things worth verifying. Step
3 checks the deployment claim rather than assuming it.

No invented metrics; this is an extracted edge facade, not a hosted service.

## Prerequisites

- Node 22+ (`node --version`) — step 4 imports the TypeScript module directly
  using native type stripping, so there is no install or build step
- `nbb` (`npm i -g nbb`) for step 4
- Network access for step 2 only (it fetches the TypeScript compiler)

## 1. Clone

```bash
git clone https://github.com/cloud-itonami/kouza.git
cd kouza
```

## 2. Typecheck the Worker

```bash
cd appview/kouza-core-k0uz401
npm run typecheck
cd -
```

Expect **no output and exit 0**. This compiles `src/app.ts` under `--strict`.

## 3. Check whether the upstreams exist

The facade is a proxy. It is only useful if what it proxies to is reachable.

```bash
for h in kouza.etzhayyim.com dispatcher.etzhayyim.com mcp.etzhayyim.com; do
  printf '%-32s ' "$h"; host "$h" >/dev/null 2>&1 && echo RESOLVES || echo NXDOMAIN
done
```

As of 2026-08-14 all three answer `NXDOMAIN`. If that is still true, stop here
for anything operational — there is no deployment to talk to, and step 4's
`:forward` case will show a failed fetch rather than a real upstream status.
That is expected and is not a fault in this repo.

## 4. Verify the containment boundary

kouza reads accounts. It must never move money. The edge enforces this by
forwarding **only** `com.etzhayyim.apps.kouza.*` and refusing everything else:

```bash
nbb scripts/probe-guardrail.cljk
```

Expect **exit 0** and five `ok` lines. The two that matter are the refusals of
`com.etzhayyim.apps.kaikei.transfer` and `com.example.evil.withdraw` — sibling
and foreign methods with money-moving shapes, both answered `404` at the edge.

The probe exits 1 if any refusal case is not refused. To confirm it can actually
fail, widen the prefix in `appview/kouza-core-k0uz401/src/app.ts`:

```bash
# NSID_PREFIX = "com.etzhayyim.apps.kouza."   ->   "com."
nbb scripts/probe-guardrail.cljk   # now: 2 FAIL, exit 1
git checkout appview/kouza-core-k0uz401/src/app.ts
```

## Where the logic sits

Not in this repository. `kotodama.jsonld` names both locations, in
`etzhayyim/root`:

- aggregation — `40-engine/kotoba/crates/kotoba-kotodama/py/src/kotodama/ingest/kouza.py`
- process contracts — `00-contracts/bpmn/com/etzhayyim/kouza`

This repo is the edge: `src/app.ts` (Worker → dispatcher). The former
SvelteKit BFF → MCP router path (`svelte/src/routes/xrpc/[...path]/+server.ts`)
was preserved, unwired, at `src/xrpc-mcp-router-proxy.ts` when the frontend
was migrated off SvelteKit (2026-09-07); it does not run as-is (imports from
`@sveltejs/kit`) and is not part of any active forward path.

## Deploying

Do not deploy without restoring the upstreams first — a live facade in front of
absent upstreams returns failures to real callers under a financial identity.
The Worker is configured in `appview/kouza-core-k0uz401/wrangler.jsonc`, routed
at `kouza.etzhayyim.com/*`; static assets are served from
`cljs/public` (built by `cd cljs && npm run release`) — **this asset/main
wiring is UNVERIFIED by an actual `wrangler deploy`; verifying the deploy is
separate follow-up work.**

## Constraints

- Read-only. No payments, transfers, withdrawals, FX, or securities orders —
  this holds for any change to this repo, not just for the running service
- Handles `pii-tier3` data; keep account identifiers out of logs and commits
- Secrets (`DISPATCHER_INTERNAL_SECRET`) stay out of this repo
- No force-push; keep the `NOTICE` Charter Rider intact
