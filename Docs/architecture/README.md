# OpenL Tablets — Architecture Notes

## Session, state & token-access design

A document set on letting clients use OpenL Studio's REST API with **tokens** (no cookies), how per-user
server-side state is keyed across channels, the authentication precedence that enables it, and the external
contract and operations the token surface requires.

> [!Note]
> **Decision of record (near-term plan): [rest-api-token-access.md](rest-api-token-access.md).**
> The "grand design" in [principal-scoped-shared-state.md](principal-scoped-shared-state.md) is **deferred** — its own
> Critical Assessment concludes it is disproportionate to the goal and recommends the MVP.

### Reading order

1. [auth-precedence-header-over-cookie.md](auth-precedence-header-over-cookie.md) — which credential wins when both a
   token and a cookie are present (**header over cookie**). The identity layer everything else assumes.
2. [decoupling-projectmodel-from-webstudio.md](decoupling-projectmodel-from-webstudio.md) — the prerequisite refactor:
   break the `ProjectModel → WebStudio` back-reference so a compiled model can be served without a window (Steps 0–3).
3. [rest-api-token-access.md](rest-api-token-access.md) — **build this**: principal-keyed, stateless REST access with
   tokens (the MVP — *how* token requests resolve per-principal state).
4. [rest-token-contract-and-ops.md](rest-token-contract-and-ops.md) — the external **API contract, authorization &
   operations** on top of the MVP: error model, branch semantics, token scopes, audit, realtime, clustering/HA,
   data-consistency, observability, rate-limiting, canonical principal id, rollout.
5. [principal-scoped-shared-state.md](principal-scoped-shared-state.md) — the deferred grand design (one shared
   per-principal state across all channels) and its **Critical Assessment**. Read last, as rationale.

### Effort, reconciled (WP ≈ 8h)

| Plan | Net-new | Shared decoupling prerequisite | Combined |
| --- | --- | --- | --- |
| **MVP** — rest-api-token-access | ~13.5 WP | ~7.5 WP (decoupling Steps 0–3) | **~21 WP** |
| Grand design — principal-scoped | ~14.5 WP | ~11 WP (full decoupling, incl. Step 4) | **~25.5 WP** |

Both plans share the decoupling prerequisite; the **Combined** column counts it once per plan. This is the single
reconciled source for these figures — other docs reference it rather than restating their own totals.

## Other architecture references

- [dependencies.md](dependencies.md) — module dependency graph.
- [technology-stack.md](technology-stack.md) — technology stack.
- [legacy-system-map.md](legacy-system-map.md) — legacy vs modern component map.
