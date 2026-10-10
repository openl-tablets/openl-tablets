/**
 * The outgoing authentication scheme (`Token` vs `Bearer`).
 *
 * A Studio in oauth2 mode accepts an IdP access token as `Bearer`, and the same
 * token presented as `Token` does not authenticate — so the scheme is not
 * cosmetic. Two things are worth pinning:
 *
 *   1. BOTH emit sites agree. The REST interceptor and
 *      `getAuthorizationHeader()` (what the STOMP WebSocket handshake sends)
 *      used to hardcode `Token` independently; a fix that touches only one of
 *      them yields a server whose REST calls authenticate and whose
 *      project-status-with-wait silently does not.
 *   2. The default is unchanged. Every existing caller — env PAT, CLI `--token`,
 *      stdio — omits the scheme and must keep getting `Token`.
 */

import { describe, it, expect, beforeEach, afterEach } from "@jest/globals";
import axios, { AxiosInstance } from "axios";
import MockAdapter from "axios-mock-adapter";
import { AuthenticationManager } from "../src/auth.js";
import type { OpenLConfig } from "../src/types.js";

describe("outgoing auth scheme", () => {
  let mockAxios: MockAdapter;
  let axiosInstance: AxiosInstance;

  beforeEach(() => {
    axiosInstance = axios.create();
    mockAxios = new MockAdapter(axiosInstance);
  });

  afterEach(() => {
    mockAxios.reset();
    mockAxios.restore();
  });

  async function sentAuthorizationHeader(config: OpenLConfig): Promise<string | undefined> {
    const auth = new AuthenticationManager(config);
    auth.setupInterceptors(axiosInstance);
    let seen: string | undefined;
    mockAxios.onGet("/probe").reply((request) => {
      seen = request.headers?.Authorization as string | undefined;
      return [200, {}];
    });
    await axiosInstance.get("/probe");
    return seen;
  }

  it("defaults to Token when no scheme is configured", async () => {
    const config: OpenLConfig = { baseUrl: "http://studio:8080", personalAccessToken: "openl_pat_a.b" };
    expect(await sentAuthorizationHeader(config)).toBe("Token openl_pat_a.b");
    expect(new AuthenticationManager(config).getAuthorizationHeader()).toBe("Token openl_pat_a.b");
  });

  it("sends Token when Token is configured explicitly", async () => {
    const config: OpenLConfig = {
      baseUrl: "http://studio:8080",
      personalAccessToken: "openl_pat_a.b",
      authScheme: "Token",
    };
    expect(await sentAuthorizationHeader(config)).toBe("Token openl_pat_a.b");
    expect(new AuthenticationManager(config).getAuthorizationHeader()).toBe("Token openl_pat_a.b");
  });

  it("sends Bearer when Bearer is configured — on REST AND on the STOMP handshake", async () => {
    const jwt = "eyJhbGciOiJSUzI1NiJ9.payload.signature";
    const config: OpenLConfig = {
      baseUrl: "http://studio:8080",
      personalAccessToken: jwt,
      authScheme: "Bearer",
    };
    // The whole point: these two must not disagree.
    expect(await sentAuthorizationHeader(config)).toBe(`Bearer ${jwt}`);
    expect(new AuthenticationManager(config).getAuthorizationHeader()).toBe(`Bearer ${jwt}`);
  });

  it("still sends nothing when no credential is configured, whatever the scheme says", async () => {
    const config: OpenLConfig = { baseUrl: "http://studio:8080", authScheme: "Bearer" };
    expect(await sentAuthorizationHeader(config)).toBeUndefined();
    expect(new AuthenticationManager(config).getAuthorizationHeader()).toBeUndefined();
  });

  it("reports the method it is actually using", () => {
    const base = { baseUrl: "http://studio:8080", personalAccessToken: "t" };
    expect(new AuthenticationManager(base).getAuthMethod()).toBe("Personal Access Token");
    expect(new AuthenticationManager({ ...base, authScheme: "Bearer" }).getAuthMethod()).toBe("Bearer token");
    expect(new AuthenticationManager({ baseUrl: "http://studio:8080" }).getAuthMethod()).toBe("No Auth");
  });
});
