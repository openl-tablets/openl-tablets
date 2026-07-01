# Authentication Precedence: Authorization Header over Cookie

- **Status** — Partly implemented; the rest proposed
- **Date** — 2026-10-23
- **Tickets** — [EPBDS-14371](https://jira.eisgroup.com/browse/EPBDS-14371) (no session for a stateless request),
  [EPBDS-16690](https://jira.eisgroup.com/browse/EPBDS-16690) (the API only under `/rest`, the handshake at `/ws`),
  [EPBDS-16757](https://jira.eisgroup.com/browse/EPBDS-16757) (transfer links)
- **Scope** — which credential decides who a request acts as when it carries more than one, in every `user.mode`
  of OpenL Studio. Where the state of the request is kept is [Client Sessions](client-sessions.md); what remains
  to build for token clients is [REST API Access with Tokens](rest-api-token-access.md).

---

## 1. Context

Three kinds of credential reach OpenL Studio:

- **The session cookie** — form, SAML and OpenID Connect sign-ins set `JSESSIONID`. The browser attaches it to every
  request to the host by itself, also to a request a page of another site makes it send.
- **The `Authorization` header** — `Basic …`, `Token …` (a personal access token) or `Bearer …` (a token of the
  identity provider). The client sets it on purpose; a page of another site cannot make the browser add it.
- **A transfer link** ([MCP File Transfer Links](mcp-file-transfer.md)) — a random id in the path of
  `/transfers/{id}` that stands for one recorded operation of one client. It carries no header and no cookie of its
  own.

A request may carry a cookie and a header at once: the API documentation page calls the API from a signed-in
browser, an agent shares a browser profile with its user, a test suite keeps the cookie of an earlier user.

## 2. Current behavior

### 2.1 The chains

`RequestMatchers.api()` — `/rest/**`, `/ws` and `/mcp/**` — is served by one chain per mode:

| Mode | Header credentials on the API chain | Security context of the API chain | Other chains |
|---|---|---|---|
| `oauth2` | PAT, Bearer | `restSecurityContextFilter`: reads the session, never saves | session only |
| `saml` | PAT | `restSecurityContextFilter`: reads the session, never saves | session only |
| `ad`, `multi` | PAT, Basic | `HttpSecurity` default, request cache disabled: saves nothing for a header | session only |
| `single` | none — every request is the one administrator | none | none |

- **No session for a header** — no chain opens an `HttpSession` for a request authenticated by its header, and none
  writes a header identity into an existing session.
- **Transfer links** — `/transfers/**` is one of the paths `SecurityConfig` passes without authentication, session
  or security context. `TransferHandler` signs the request in as the owner of the link
  (`TransferLinkAuthentication`, `link` in the [Audit Log](audit-log.md)).
- **CSRF** — disabled on every chain; the hand-built `oauth2` and `saml` chains contain no `CsrfFilter`.
- **The cookie** — `web.xml` sets `http-only` and the `COOKIE` tracking mode; there is no `Secure` and no `SameSite`.

### 2.2 Who a request acts as

| `Authorization` header | Session cookie | The request acts as | Its state ([Client Sessions](client-sessions.md)) |
|---|---|---|---|
| user P | none | P | kept for the credential |
| user P | signed in as P | P | the browser session |
| user P | signed in as Q | P | kept for the credential; Q's session is never touched |
| invalid or expired | any | nobody — `401` | none |
| none | signed in as Q | Q | the browser session |
| none | none | nobody — `401` on the API chain | none |
| a transfer link, any header | any | the user who asked for the link | the client that issued it |

- **The header wins over another user's cookie** — `PatAuthenticationFilter` and Spring's
  `BasicAuthenticationFilter` authenticate the header unless the session already holds the same user;
  `BearerTokenAuthenticationFilter` always does.
- **An invalid header never falls back to the cookie** — a refused PAT answers `401` from the filter, a refused Basic
  header the entry point, a refused bearer token the bearer failure handler.
- **The same user on both** — PAT and Basic keep the `Authentication` of the session, Bearer puts its own in place.
  The user is the same either way, and `ClientSessions` sends the request to the browser session.

The rule is spread over the filters, not stated once: each filter decides for its own header.

## 3. Decision

**The `Authorization` header decides who a request acts as. The cookie decides only when there is no header. A
transfer link decides alone, on a path no other credential reaches.**

- **D1 — Explicit over implicit.** The header is set on purpose; the cookie is sent by the browser whatever page
  caused the request. Trusting the header follows RFC 6750 and keeps a cross-site request from choosing the
  identity. Rejected: the session over the header — a stale cookie of another user would win over the credential the
  client chose.
- **D2 — A header request opens no session and writes none.** Its state lives under its credential, never in an
  `HttpSession` and never in the session of the cookie it came with. Matching a header to a session by the user name
  is rejected: ambiguous between two browsers of one user, racy, and a bleed of privileges where the session holds
  more. Implemented ([EPBDS-14371](https://jira.eisgroup.com/browse/EPBDS-14371)).
- **D3 — An invalid header is a refusal, not an absence.** A request whose header fails answers `401`, even with a
  valid cookie: a client that sent a credential meant that one. Implemented.
- **D4 — The rule stays in the filters.** Rejected: one central filter that picks the credential first. Each filter
  already applies D1 for its own header, and a central filter would have to know every credential type of every
  mode. A test of the matrix of §2.2 in every mode guards the rule instead (§4).
- **D5 — A transfer link is a capability on a path of its own.** `/transfers/**` is not one of
  `RequestMatchers.apiPatterns()`, so no chain reads a header or a cookie there: the link alone names the user and
  the client it acts for. Consequences:
  - a link request acts as the user who asked for the link, with that user's rights at the time of the request
    ([MCP File Transfer Links](mcp-file-transfer.md) D6), whatever header or cookie it carries;
  - a link enters the state of the client that issued it through `ClientSessions.enter`; the browser session has no
    key, so a browser is issued no link;
  - the API chains keep reading no credential from a URL: `DefaultBearerTokenResolver` stays header-only, and no
    query parameter authenticates a `/rest` request. A credential in a URL is written to access logs, browser
    history and `Referer` headers; a link is bounded by minutes, by one use for an upload and by the life of its
    client, and answers `Cache-Control: no-store` and `Referrer-Policy: no-referrer`.

## 4. Remaining work

- **Test the matrix in every mode** — an ITEST that sends each row of §2.2 — header and another user's cookie
  included — to `oauth2`, `saml`, `ad` and `multi`, and checks the user the request acted as and that the cookie's
  session kept its state. Unit tests cover the filters and `ClientSessions` one by one; no suite sends a header and a
  foreign cookie together. About 1 WP.
- **Harden the cookie** — `Secure` on `JSESSIONID` when Studio is reached over HTTPS, and `SameSite=Lax`. `Lax` keeps
  the cookie on the redirect `GET` that brings an OpenID Connect sign-in back. It drops it on the cross-site `POST`
  of the SAML response, while Spring keeps the pending SAML request in the session: the `saml` mode needs that
  request kept elsewhere, or the cookie stays `SameSite=None` there. About 1 WP, with the ITESTs of both sign-ins.
- **Decide the CSRF stance** — the cookie authenticates state-changing requests on `/rest`, the STOMP handshake on
  `/ws` and the MCP endpoint on `/mcp`, with CSRF disabled. `SameSite=Lax` stops a cross-site `POST`, `PUT` or
  `DELETE` from carrying the cookie in current browsers; CSRF tokens would also cover an older browser and a
  same-site attacker, at the cost of a token round trip in the UI and the API documentation page. Open decision.
- **Move the non-API chains off `SecurityContextPersistenceFilter`** — the sign-in, logout, callback and catch-all
  chains of `oauth2` and `saml` still build it in `CommonAuthenticationConfig`. It is deprecated; the replacement is
  a `SecurityContextHolderFilter` with explicit saving where a sign-in completes. About 0.5–1 WP.
- **`@NotPatAuth` and the same user's cookie** — a PAT sent with the cookie of its own user keeps the session's
  `Authentication`, so `@NotPatAuth` lets the request manage tokens. The request is the browser of that user, which
  may manage them anyway, so this is accepted; the matrix test records it.

## 5. Risks

| Risk | Severity | Likelihood | Mitigation |
|---|---|---|---|
| CSRF on cookie requests — CSRF disabled everywhere; `/ws` and `/mcp` take the cookie too | High | Medium | `SameSite=Lax` + `Secure`; decide on CSRF tokens (§4) |
| A filter changed or added without D1 — the rule is spread over the filters (D4) | Medium | Low | The matrix ITEST in every mode |
| A credential read from a URL on the API chains | Medium | Low | Header-only `DefaultBearerTokenResolver`; links only on `/transfers` (D5) |
| A leaked transfer link | Medium | Low | Minutes of life, one use for an upload, bound to its client, `no-store` and `no-referrer` ([MCP File Transfer Links](mcp-file-transfer.md)) |
