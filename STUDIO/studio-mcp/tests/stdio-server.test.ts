/**
 * Unit tests for `loadConfigFromEnv` (the stdio MCP transport's config loader)
 * exported from src/stdio-server.ts.
 *
 * It is a pure-ish validator with several branches (missing base URL, invalid
 * URL, invalid timeout, missing auth); these cover the validation logic
 * directly, in-process, without spawning the binary.
 */

import { afterEach, beforeEach, describe, expect, it, jest } from "@jest/globals";
import { loadConfigFromEnv } from "../src/stdio-server.js";

describe("loadConfigFromEnv (stdio MCP transport)", () => {
  const OPENL_KEYS = [
    "OPENL_BASE_URL",
    "OPENL_PERSONAL_ACCESS_TOKEN",
    "OPENL_TIMEOUT",
  ] as const;

  let saved: Record<string, string | undefined>;
  let errSpy: ReturnType<typeof jest.spyOn>;

  beforeEach(() => {
    // Snapshot and clear the OPENL_* env so each test starts from a clean slate.
    saved = {};
    for (const k of OPENL_KEYS) {
      saved[k] = process.env[k];
      delete process.env[k];
    }
    // loadConfigFromEnv emits diagnostic [Config] lines to stderr — silence them.
    errSpy = jest.spyOn(console, "error").mockImplementation(() => {});
  });

  afterEach(() => {
    for (const k of OPENL_KEYS) {
      if (saved[k] === undefined) delete process.env[k];
      else process.env[k] = saved[k];
    }
    errSpy.mockRestore();
  });

  it("throws when no base URL is provided, naming both the positional and the env var", async () => {
    // The message must mention both ways to supply the URL so the binary's
    // usage hint (printed by main()) and `--help` stay consistent.
    await expect(loadConfigFromEnv()).rejects.toThrow(/OPENL_BASE_URL/);
    await expect(loadConfigFromEnv()).rejects.toThrow(/positional argument/i);
  });

  it("throws on an invalid base URL", async () => {
    process.env.OPENL_BASE_URL = "bad";
    await expect(loadConfigFromEnv()).rejects.toThrow(/Invalid OpenL base URL/);
  });

  it("prefers an explicit base URL override (positional <url>) over OPENL_BASE_URL", async () => {
    process.env.OPENL_BASE_URL = "http://env-host:9999";
    const cfg = await loadConfigFromEnv({ baseUrl: "http://positional:8080" });
    expect(cfg.baseUrl).toBe("http://positional:8080");
  });

  it("validates an invalid base URL override the same way", async () => {
    await expect(loadConfigFromEnv({ baseUrl: "not-a-url" })).rejects.toThrow(/Invalid OpenL base URL/);
  });

  it("applies token/timeout overrides (server-launch flags) over the environment", async () => {
    process.env.OPENL_BASE_URL = "http://env-host:9999";
    process.env.OPENL_PERSONAL_ACCESS_TOKEN = "openl_pat_env";
    const cfg = await loadConfigFromEnv({
      baseUrl: "http://localhost:8080",
      personalAccessToken: "openl_pat_flag",
      timeout: 12345,
    });
    expect(cfg).toMatchObject({
      baseUrl: "http://localhost:8080",
      personalAccessToken: "openl_pat_flag",
      timeout: 12345,
    });
  });

  it("throws on an invalid timeout", async () => {
    process.env.OPENL_BASE_URL = "http://localhost:8080";
    process.env.OPENL_PERSONAL_ACCESS_TOKEN = "t";
    process.env.OPENL_TIMEOUT = "0";
    await expect(loadConfigFromEnv()).rejects.toThrow(/Invalid OPENL_TIMEOUT/);
  });

  it("resolves with no auth and logs nothing about authentication (single-user mode)", async () => {
    process.env.OPENL_BASE_URL = "http://localhost:8080";
    const cfg = await loadConfigFromEnv();
    expect(cfg).toMatchObject({ baseUrl: "http://localhost:8080" });
    expect(cfg.personalAccessToken).toBeUndefined();
    // Running without a token is the normal single-user case: no auth status
    // line and no "no authentication" notice are logged for it.
    expect(errSpy).not.toHaveBeenCalledWith(expect.stringMatching(/Personal Access Token|No authentication/i));
  });

  it("treats a blank/whitespace token as absent (anonymous, never an empty credential)", async () => {
    process.env.OPENL_BASE_URL = "http://localhost:8080";
    process.env.OPENL_PERSONAL_ACCESS_TOKEN = "   ";
    const cfg = await loadConfigFromEnv();
    expect(cfg.personalAccessToken).toBeUndefined();
  });

  it("resolves with a PAT and logs that a token is configured", async () => {
    process.env.OPENL_BASE_URL = "http://localhost:8080";
    process.env.OPENL_PERSONAL_ACCESS_TOKEN = "openl_pat_x";
    const cfg = await loadConfigFromEnv();
    expect(cfg).toMatchObject({
      baseUrl: "http://localhost:8080",
      personalAccessToken: "openl_pat_x",
    });
    // A configured token is confirmed (value hidden) — the complement of the
    // no-auth case, which logs nothing.
    expect(errSpy).toHaveBeenCalledWith(expect.stringMatching(/Personal Access Token: configured \(hidden\)/));
  });

  it("parses a valid OPENL_TIMEOUT from the environment", async () => {
    process.env.OPENL_BASE_URL = "http://localhost:8080";
    process.env.OPENL_PERSONAL_ACCESS_TOKEN = "openl_pat_x";
    process.env.OPENL_TIMEOUT = "45000";
    await expect(loadConfigFromEnv()).resolves.toMatchObject({
      baseUrl: "http://localhost:8080",
      personalAccessToken: "openl_pat_x",
      timeout: 45000,
    });
  });
});
