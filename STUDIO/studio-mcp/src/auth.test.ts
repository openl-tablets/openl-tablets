/**
 * The authentication `OpenLClient` sends to OpenL Studio, and the diagnostics of a 401 answer.
 *
 * The client sends the `Authorization` header of the request to the MCP server as it is: OpenL Studio has checked it
 * at `/mcp`, whatever its scheme.
 */

import { afterEach, describe, it, expect, vi } from "vitest";
import { MockHttp } from "./testing/mock-http.js";
import { OpenLClient } from "./client.js";
import type { OpenLConfig } from "./types.js";

/** The `Authorization` header a client of `config` sends. */
async function sentAuthorization(config: OpenLConfig): Promise<string | undefined> {
  const client = new OpenLClient(config);
  const mock = new MockHttp(client);
  mock.onGet("/repos").reply(200, []);
  await client.listRepositories();
  return mock.history.get[0].headers.Authorization;
}

describe("authentication of the calls to OpenL Studio", () => {
  const errors = vi.spyOn(console, "error").mockImplementation(() => {});

  afterEach(() => {
    errors.mockClear();
  });

  it.each([
    "Token openl_pat_public.secret",
    "Bearer eyJhbGciOiJSUzI1NiJ9.payload.signature",
    "Basic YWRtaW46YWRtaW4=",
  ])("sends %s as it is", async (authorization) => {
    expect(await sentAuthorization({ baseUrl: "http://studio:8080", authorization })).toBe(authorization);
  });

  it("sends nothing without credentials, and for empty ones", async () => {
    expect(await sentAuthorization({ baseUrl: "http://studio:8080" })).toBeUndefined();
    expect(await sentAuthorization({ baseUrl: "http://studio:8080", authorization: "" })).toBeUndefined();
  });

  it("explains a 401 answer with the message of Studio and the scheme in use, never the secret", async () => {
    const client = new OpenLClient({ baseUrl: "http://studio:8080", authorization: "Token openl_pat_a.b" });
    const mock = new MockHttp(client);
    mock.onGet("/repos").reply(401, { message: "Token expired" });
    mock.onGet("/repos/design/branches").reply(404, { message: "Not found" });

    await expect(client.listRepositories()).rejects.toMatchObject({ kind: "status" });
    await expect(client.listBranches("design")).rejects.toMatchObject({ kind: "status" });

    const lines = errors.mock.calls.map(([line]) => String(line)).filter((line) => line.startsWith("[Auth] 4"));
    expect(lines).toEqual(["[Auth] 401 Unauthorized: Token expired (Token)"]);
  });

  it("names no scheme for a 401 answer to a call without credentials", async () => {
    const client = new OpenLClient({ baseUrl: "http://studio:8080" });
    new MockHttp(client).onGet("/repos").reply(401, {});

    await expect(client.listRepositories()).rejects.toMatchObject({ kind: "status" });

    expect(errors.mock.calls.map(([line]) => String(line))).toContain("[Auth] 401 Unauthorized: Unauthorized (No Auth)");
  });
});
