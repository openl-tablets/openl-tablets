/**
 * How the HTTP transport turns an inbound `Authorization` header into the
 * credential it presents to Studio.
 *
 * The transport accepts both schemes on the way in and has always rewritten
 * whatever arrived to `Token` on the way out. That is correct for a PAT and
 * wrong for an IdP access token: a Studio in oauth2 mode wants it as `Bearer`,
 * and as `Token` it does not authenticate — so an OAuth-capable MCP client can
 * reach this server but not Studio through it.
 *
 * `OPENL_MCP_PRESERVE_AUTH_SCHEME` opts a deployment into forwarding the scheme.
 * It is off by default and the default must stay byte-identical to the previous
 * behaviour, because forwarding a client-supplied credential upstream is token
 * passthrough — forbidden by the MCP specification until this server validates
 * the token itself (`iss`/`aud`/`exp` against the IdP JWKS, plus RFC 9728
 * resource metadata; the plan's P2.1). Opting in should be a visible decision,
 * not a silent default.
 */

import { describe, it, expect, beforeEach, afterEach } from "@jest/globals";
import { createHttpSessionClient } from "../src/http-server.js";

const JWT = "eyJhbGciOiJSUzI1NiJ9.payload.signature";
const PAT = "openl_pat_public.secret";
const BASE = "http://studio:8080";

describe("inbound Authorization -> outgoing scheme", () => {
  let saved: string | undefined;

  beforeEach(() => {
    saved = process.env.OPENL_MCP_PRESERVE_AUTH_SCHEME;
    delete process.env.OPENL_MCP_PRESERVE_AUTH_SCHEME;
  });

  afterEach(() => {
    if (saved === undefined) delete process.env.OPENL_MCP_PRESERVE_AUTH_SCHEME;
    else process.env.OPENL_MCP_PRESERVE_AUTH_SCHEME = saved;
  });

  describe("default (flag unset) — unchanged behaviour", () => {
    it("rewrites an inbound Bearer to Token", () => {
      const client = createHttpSessionClient(BASE, `Bearer ${JWT}`);
      expect(client.getAuthorizationHeader()).toBe(`Token ${JWT}`);
    });

    it("keeps an inbound Token as Token", () => {
      const client = createHttpSessionClient(BASE, `Token ${PAT}`);
      expect(client.getAuthorizationHeader()).toBe(`Token ${PAT}`);
    });
  });

  describe("opted in", () => {
    beforeEach(() => {
      process.env.OPENL_MCP_PRESERVE_AUTH_SCHEME = "1";
    });

    it("forwards an inbound Bearer as Bearer", () => {
      const client = createHttpSessionClient(BASE, `Bearer ${JWT}`);
      expect(client.getAuthorizationHeader()).toBe(`Bearer ${JWT}`);
    });

    it("does NOT upgrade an inbound Token to Bearer", () => {
      // The flag preserves what arrived; it never invents a scheme. A PAT
      // presented as Bearer would stop authenticating.
      const client = createHttpSessionClient(BASE, `Token ${PAT}`);
      expect(client.getAuthorizationHeader()).toBe(`Token ${PAT}`);
    });

    it("matches the scheme case-insensitively, as HTTP requires", () => {
      expect(createHttpSessionClient(BASE, `bearer ${JWT}`).getAuthorizationHeader()).toBe(`Bearer ${JWT}`);
      expect(createHttpSessionClient(BASE, `BEARER ${JWT}`).getAuthorizationHeader()).toBe(`Bearer ${JWT}`);
    });

    it.each(["1", "true", "yes", "on", "TRUE"])("accepts %s as opting in", (value) => {
      process.env.OPENL_MCP_PRESERVE_AUTH_SCHEME = value;
      expect(createHttpSessionClient(BASE, `Bearer ${JWT}`).getAuthorizationHeader()).toBe(`Bearer ${JWT}`);
    });

    it.each(["0", "false", "no", "off", ""])("treats %p as NOT opting in", (value) => {
      process.env.OPENL_MCP_PRESERVE_AUTH_SCHEME = value;
      expect(createHttpSessionClient(BASE, `Bearer ${JWT}`).getAuthorizationHeader()).toBe(`Token ${JWT}`);
    });
  });

  describe("no usable credential", () => {
    it.each([undefined, "", "Basic dXNlcjpwYXNz", "Bearer", "Bearer   "])(
      "sends no Authorization for %p",
      (header) => {
        process.env.OPENL_MCP_PRESERVE_AUTH_SCHEME = "1";
        const client = createHttpSessionClient(BASE, header as string | undefined);
        expect(client.getAuthorizationHeader()).toBeUndefined();
      },
    );
  });
});
