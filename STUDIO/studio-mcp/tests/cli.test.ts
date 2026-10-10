/**
 * Tests for the CLI mode (`src/cli.ts`).
 *
 * Drives `runCli` with injected stdin/stdout/stderr and a mock-wrapped
 * OpenLClient. No child processes, no real network — purely in-process
 * so it integrates with the existing axios-mock-adapter setup.
 */

import { afterEach, describe, expect, it } from "@jest/globals";
import MockAdapter from "axios-mock-adapter";
import { Readable, Writable } from "node:stream";
import { mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";

import { CliError, EXIT_CODES, classifyError, isCliInvocation, parseArgs, runCli } from "../src/cli.js";
import { OpenLClient } from "../src/client.js";
import { hashFingerprint } from "../src/utils.js";
import type { OpenLConfig } from "../src/types.js";
import { mockRepositories } from "./mocks/openl-api-mocks.js";

/**
 * Build a captured-output harness for one CLI invocation.
 */
function createHarness(stdinText = "") {
  let stdoutBuf = "";
  let stderrBuf = "";
  const stdout = new Writable({
    write(chunk, _enc, cb) {
      stdoutBuf += chunk.toString();
      cb();
    },
  });
  const stderr = new Writable({
    write(chunk, _enc, cb) {
      stderrBuf += chunk.toString();
      cb();
    },
  });
  const stdin = Readable.from(stdinText, { objectMode: false });
  return {
    stdout,
    stderr,
    stdin,
    getStdout: () => stdoutBuf,
    getStderr: () => stderrBuf,
  };
}

/**
 * Build a client + axios mock for tests that need to hit the OpenL API.
 */
function createMockClient(): { client: OpenLClient; mock: MockAdapter } {
  const config: OpenLConfig = {
    baseUrl: "http://localhost:8080",
    personalAccessToken: "openl_pat_test",
  };
  const client = new OpenLClient(config);
  // @ts-ignore Access private axios instance for mocking, mirrors tests/integration/handlers.test.ts
  const mock = new MockAdapter(client.axiosInstance);
  return { client, mock };
}

const ENV_OK = {
  OPENL_BASE_URL: "http://localhost:8080",
  OPENL_PERSONAL_ACCESS_TOKEN: "openl_pat_test",
};

describe("CLI", () => {
  let mock: MockAdapter | undefined;

  afterEach(() => {
    if (mock) {
      mock.restore();
      mock = undefined;
    }
  });

  describe("--help", () => {
    it("prints usage and tool list, returns 0", async () => {
      const h = createHarness();
      const code = await runCli({
        argv: ["--help"],
        env: {},
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
      });
      expect(code).toBe(0);
      const out = h.getStdout();
      expect(out).toContain("Usage:");
      expect(out).toContain("--base-url");
      // Catalog lists tools by their prefix-less CLI name (line starts with the
      // short name, not `openl_…`), and the prefix-optional rule is documented.
      expect(out).toMatch(/^\s+list_repositories\b/m);
      expect(out).toMatch(/prefix is not used/i);
      // Requirement: --help shows the positional <url> as required and
      // documents the OPENL_BASE_URL fallback + precedence.
      expect(out).toContain("openl-mcp <url>");
      expect(out).toMatch(/Server URL \(required.*OPENL_BASE_URL/s);
      expect(out).toContain("positional takes precedence");
    });
  });

  describe("--list-tools", () => {
    it("returns JSON schema dump for all tools", async () => {
      const { client } = createMockClient();
      const h = createHarness();
      const code = await runCli({
        argv: ["--list-tools"],
        env: ENV_OK,
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
        clientFactory: () => client,
      });
      expect(code).toBe(0);
      const parsed = JSON.parse(h.getStdout());
      expect(Array.isArray(parsed)).toBe(true);
      expect(parsed.length).toBeGreaterThan(0);
      expect(parsed[0]).toHaveProperty("name");
      expect(parsed[0]).toHaveProperty("inputSchema");
    });

    it("works without any config (pure discovery, no auth needed)", async () => {
      const h = createHarness();
      const code = await runCli({
        argv: ["--list-tools"],
        env: {}, // no OPENL_BASE_URL, no auth
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
      });
      expect(code).toBe(0);
      expect(h.getStderr()).toBe("");
      const parsed = JSON.parse(h.getStdout());
      expect(parsed.length).toBeGreaterThan(0);
    });
  });

  describe("happy path", () => {
    it("invokes list_repositories with inline JSON and writes result to stdout", async () => {
      const { client, mock: m } = createMockClient();
      mock = m;
      m.onGet("/repos").reply(200, mockRepositories);

      const h = createHarness();
      const code = await runCli({
        argv: ["list_repositories", '{"response_format":"json"}'],
        env: ENV_OK,
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
        clientFactory: () => client,
      });

      expect(code).toBe(0);
      expect(h.getStdout()).toContain("Design Repository");
      expect(h.getStderr()).toBe("");
    });

    it("reads tool args from --stdin", async () => {
      const { client, mock: m } = createMockClient();
      mock = m;
      m.onGet("/repos").reply(200, mockRepositories);

      const h = createHarness('{"response_format":"json"}');
      const code = await runCli({
        argv: ["list_repositories", "--stdin"],
        env: ENV_OK,
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
        clientFactory: () => client,
      });

      expect(code).toBe(0);
      expect(h.getStdout()).toContain("Design Repository");
    });

    it("reads tool args from @file", async () => {
      const dir = await mkdtemp(join(tmpdir(), "openl-cli-test-"));
      try {
        const file = join(dir, "args.json");
        await writeFile(file, '{"response_format":"json"}');

        const { client, mock: m } = createMockClient();
        mock = m;
        m.onGet("/repos").reply(200, mockRepositories);

        const h = createHarness();
        const code = await runCli({
          argv: ["list_repositories", `@${file}`],
          env: ENV_OK,
          stdin: h.stdin,
          stdout: h.stdout,
          stderr: h.stderr,
          clientFactory: () => client,
        });

        expect(code).toBe(0);
        expect(h.getStdout()).toContain("Design Repository");
      } finally {
        await rm(dir, { recursive: true, force: true });
      }
    });

    it("works with no tool args (calls executeTool with undefined)", async () => {
      const { client, mock: m } = createMockClient();
      mock = m;
      m.onGet("/repos").reply(200, mockRepositories);

      const h = createHarness();
      const code = await runCli({
        argv: ["list_repositories"],
        env: ENV_OK,
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
        clientFactory: () => client,
      });

      expect(code).toBe(0);
      expect(h.getStdout()).toContain("Design Repository");
    });

    it("honors CLI flag overrides for auth", async () => {
      const { client, mock: m } = createMockClient();
      mock = m;
      m.onGet("/repos").reply(200, mockRepositories);

      const h = createHarness();
      const code = await runCli({
        argv: [
          "list_repositories",
          "--base-url",
          "http://localhost:8080",
          "--token",
          "openl_pat_test",
        ],
        env: {}, // no env config — overrides should fill in
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
        clientFactory: () => client,
      });

      expect(code).toBe(0);
      expect(h.getStdout()).toContain("Design Repository");
    });

    it("defaults to JSON when caller omits response_format", async () => {
      const { client, mock: m } = createMockClient();
      mock = m;
      m.onGet("/repos").reply(200, mockRepositories);

      const h = createHarness();
      const code = await runCli({
        argv: ["list_repositories"], // no args at all
        env: ENV_OK,
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
        clientFactory: () => client,
      });

      expect(code).toBe(0);
      const parsed = JSON.parse(h.getStdout());
      expect(parsed.data[0].name).toBe("Design Repository");
    });

    it("honors explicit response_format=markdown for human-readable output", async () => {
      const { client, mock: m } = createMockClient();
      mock = m;
      m.onGet("/repos").reply(200, mockRepositories);

      const h = createHarness();
      const code = await runCli({
        argv: ["list_repositories", '{"response_format":"markdown"}'],
        env: ENV_OK,
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
        clientFactory: () => client,
      });

      expect(code).toBe(0);
      expect(h.getStdout()).toContain("Design Repository");
      expect(() => JSON.parse(h.getStdout())).toThrow();
    });

    it("sets OPENL_CLI_QUIET=1 during the run and restores afterwards", async () => {
      const { client, mock: m } = createMockClient();
      mock = m;
      m.onGet("/repos").reply(200, mockRepositories);

      const prev = process.env.OPENL_CLI_QUIET;
      delete process.env.OPENL_CLI_QUIET;

      let seenAtRun: string | undefined;
      try {
        const h = createHarness();
        const code = await runCli({
          argv: ["list_repositories"],
          env: ENV_OK,
          stdin: h.stdin,
          stdout: h.stdout,
          stderr: h.stderr,
          clientFactory: (_config) => {
            seenAtRun = process.env.OPENL_CLI_QUIET;
            return client;
          },
        });
        expect(code).toBe(0);
        expect(seenAtRun).toBe("1");
        expect(process.env.OPENL_CLI_QUIET).toBeUndefined();
      } finally {
        if (prev !== undefined) process.env.OPENL_CLI_QUIET = prev;
      }
    });
  });

  describe("--cookie-jar", () => {
    it("writes captured JSESSIONID to the jar after a tool call", async () => {
      const dir = await mkdtemp(join(tmpdir(), "openl-cli-jar-"));
      try {
        const jarPath = join(dir, "session.jar");
        const { client, mock: m } = createMockClient();
        mock = m;
        // Server returns a Set-Cookie header, the client's interceptor
        // should capture it and store on the instance.
        m.onGet("/repos").reply(200, mockRepositories, {
          "set-cookie": "JSESSIONID=abc123session; Path=/; HttpOnly",
        });

        const h = createHarness();
        const code = await runCli({
          argv: ["list_repositories", "--cookie-jar", jarPath],
          env: ENV_OK,
          stdin: h.stdin,
          stdout: h.stdout,
          stderr: h.stderr,
          clientFactory: () => client,
        });
        expect(code).toBe(0);

        const persisted = JSON.parse((await readFile(jarPath, "utf-8")).trim());
        expect(persisted.jsessionId).toBe("abc123session");
        expect(persisted.baseUrl).toBe(client.getBaseUrl());
        expect(typeof persisted.authFingerprint).toBe("string");
      } finally {
        await rm(dir, { recursive: true, force: true });
      }
    });

    it("restores JSESSIONID from a jar bound to the same server and user", async () => {
      const dir = await mkdtemp(join(tmpdir(), "openl-cli-jar-"));
      try {
        const jarPath = join(dir, "session.jar");
        const { client, mock: m } = createMockClient();
        mock = m;
        // Write a jar whose binding matches the client (same base URL + principal).
        await writeFile(
          jarPath,
          JSON.stringify({
            baseUrl: client.getBaseUrl(),
            authFingerprint: hashFingerprint(client.getAuthorizationHeader() ?? "anonymous"),
            jsessionId: "prevsession789",
          }) + "\n",
        );

        // Capture the Cookie header the client sends.
        let sentCookie: string | undefined;
        m.onGet("/repos").reply((config) => {
          sentCookie = (config.headers?.Cookie || config.headers?.cookie) as string | undefined;
          return [200, mockRepositories];
        });

        const h = createHarness();
        const code = await runCli({
          argv: ["list_repositories", "--cookie-jar", jarPath],
          env: ENV_OK,
          stdin: h.stdin,
          stdout: h.stdout,
          stderr: h.stderr,
          clientFactory: () => client,
        });
        expect(code).toBe(0);
        expect(sentCookie).toContain("JSESSIONID=prevsession789");
      } finally {
        await rm(dir, { recursive: true, force: true });
      }
    });

    it("ignores a legacy bare-cookie jar (no binding) and warns", async () => {
      const dir = await mkdtemp(join(tmpdir(), "openl-cli-jar-"));
      try {
        const jarPath = join(dir, "session.jar");
        await writeFile(jarPath, "legacysession\n"); // pre-binding format

        const { client, mock: m } = createMockClient();
        mock = m;
        let sentCookie: string | undefined;
        m.onGet("/repos").reply((config) => {
          sentCookie = (config.headers?.Cookie || config.headers?.cookie) as string | undefined;
          return [200, mockRepositories];
        });

        const h = createHarness();
        const code = await runCli({
          argv: ["list_repositories", "--cookie-jar", jarPath],
          env: ENV_OK,
          stdin: h.stdin,
          stdout: h.stdout,
          stderr: h.stderr,
          clientFactory: () => client,
        });
        expect(code).toBe(0);
        expect(sentCookie ?? "").not.toContain("JSESSIONID=legacysession");
        expect(h.getStderr()).toContain("legacy cookie jar");
      } finally {
        await rm(dir, { recursive: true, force: true });
      }
    });

    it("ignores a jar saved for a different server or user", async () => {
      const dir = await mkdtemp(join(tmpdir(), "openl-cli-jar-"));
      try {
        const jarPath = join(dir, "session.jar");
        await writeFile(
          jarPath,
          JSON.stringify({
            baseUrl: "http://other-host:9999/rest",
            authFingerprint: "deadbeefdeadbeef",
            jsessionId: "othersession",
          }) + "\n",
        );

        const { client, mock: m } = createMockClient();
        mock = m;
        let sentCookie: string | undefined;
        m.onGet("/repos").reply((config) => {
          sentCookie = (config.headers?.Cookie || config.headers?.cookie) as string | undefined;
          return [200, mockRepositories];
        });

        const h = createHarness();
        const code = await runCli({
          argv: ["list_repositories", "--cookie-jar", jarPath],
          env: ENV_OK,
          stdin: h.stdin,
          stdout: h.stdout,
          stderr: h.stderr,
          clientFactory: () => client,
        });
        expect(code).toBe(0);
        expect(sentCookie ?? "").not.toContain("JSESSIONID=othersession");
        expect(h.getStderr()).toContain("different server or user");
      } finally {
        await rm(dir, { recursive: true, force: true });
      }
    });

    it("treats a missing jar file as a fresh session (no error)", async () => {
      const dir = await mkdtemp(join(tmpdir(), "openl-cli-jar-"));
      try {
        const jarPath = join(dir, "does-not-exist-yet.jar");
        const { client, mock: m } = createMockClient();
        mock = m;
        m.onGet("/repos").reply(200, mockRepositories);

        const h = createHarness();
        const code = await runCli({
          argv: ["list_repositories", "--cookie-jar", jarPath],
          env: ENV_OK,
          stdin: h.stdin,
          stdout: h.stdout,
          stderr: h.stderr,
          clientFactory: () => client,
        });
        expect(code).toBe(0);
        expect(h.getStderr()).toBe("");
      } finally {
        await rm(dir, { recursive: true, force: true });
      }
    });

    it("skips writing when no session cookie was established", async () => {
      const dir = await mkdtemp(join(tmpdir(), "openl-cli-jar-"));
      try {
        const jarPath = join(dir, "session.jar");
        const { client, mock: m } = createMockClient();
        mock = m;
        // No Set-Cookie in response — client never captures a session.
        m.onGet("/repos").reply(200, mockRepositories);

        const h = createHarness();
        const code = await runCli({
          argv: ["list_repositories", "--cookie-jar", jarPath],
          env: ENV_OK,
          stdin: h.stdin,
          stdout: h.stdout,
          stderr: h.stderr,
          clientFactory: () => client,
        });
        expect(code).toBe(0);

        // Jar should not exist (or be empty) — nothing to persist.
        let written = "";
        try {
          written = await readFile(jarPath, "utf-8");
        } catch (error) {
          // ENOENT is the expected outcome here (no jar written because
          // the stateless endpoint didn't set a session cookie). Anything
          // else — permissions, I/O — should surface as a test failure.
          if ((error as NodeJS.ErrnoException).code !== "ENOENT") throw error;
        }
        expect(written).toBe("");
      } finally {
        await rm(dir, { recursive: true, force: true });
      }
    });
  });

  describe("error handling", () => {
    it("returns EX_CONFIG (78) when OPENL_BASE_URL is missing", async () => {
      const h = createHarness();
      const code = await runCli({
        argv: ["list_repositories", "{}"],
        env: {},
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
      });
      expect(code).toBe(EXIT_CODES.CONFIG);
      expect(h.getStderr()).toContain("OPENL_BASE_URL");
    });

    it("runs unauthenticated when no token is configured (no creds → request proceeds)", async () => {
      // Authentication is optional: with a base URL but no token, the CLI does
      // not fail fast — it sends the request without an Authorization header.
      const { client, mock: m } = createMockClient();
      mock = m;
      m.onGet("/repos").reply(200, mockRepositories);

      const h = createHarness();
      const code = await runCli({
        argv: ["list_repositories"],
        env: { OPENL_BASE_URL: "http://localhost:8080" }, // base URL only, no auth
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
        clientFactory: () => client,
      });
      expect(code).toBe(EXIT_CODES.OK);
      expect(h.getStdout()).toContain("Design Repository");
    });

    it("returns EX_USAGE (64) on unknown flag", async () => {
      const h = createHarness();
      const code = await runCli({
        argv: ["--bogus"],
        env: ENV_OK,
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
      });
      expect(code).toBe(EXIT_CODES.USAGE);
      expect(h.getStderr()).toContain("Unknown flag");
    });

    it("returns EX_USAGE (64) when tool name is missing", async () => {
      const h = createHarness();
      const code = await runCli({
        argv: ['{"response_format":"json"}'],
        env: ENV_OK,
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
      });
      expect(code).toBe(EXIT_CODES.USAGE);
      expect(h.getStderr()).toContain("tool name is required");
    });

    it("returns EX_DATAERR (65) on malformed JSON args", async () => {
      const { client } = createMockClient();
      const h = createHarness();
      const code = await runCli({
        argv: ["list_repositories", "{not-json"],
        env: ENV_OK,
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
        clientFactory: () => client,
      });
      expect(code).toBe(EXIT_CODES.DATAERR);
      expect(h.getStderr()).toContain("Failed to parse tool arguments as JSON");
    });

    it("returns EX_USAGE (64) when more than one args source is provided", async () => {
      const h = createHarness();
      const code = await runCli({
        argv: ["list_repositories", "{}", "--stdin"],
        env: ENV_OK,
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
      });
      expect(code).toBe(EXIT_CODES.USAGE);
      expect(h.getStderr()).toContain("only one of");
    });

    it("returns EX_USAGE (64) for unknown tool name", async () => {
      const { client } = createMockClient();
      const h = createHarness();
      const code = await runCli({
        argv: ["does_not_exist", "{}"],
        env: ENV_OK,
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
        clientFactory: () => client,
      });
      // Unknown tool is a "user typed the wrong name" case — same category
      // as a typo'd flag — so the runCli pre-checks the registry before
      // dispatching to executeTool and returns EX_USAGE (consistent with
      // the `<tool> --help` path for the same error).
      expect(code).toBe(EXIT_CODES.USAGE);
      expect(h.getStderr()).toContain("Unknown tool");
    });

    it("prefers EX_USAGE over EX_CONFIG for an unknown tool with no config", async () => {
      // A typo'd tool with missing OPENL_BASE_URL/auth must still report the
      // typo (USAGE) — the unknown-tool check runs before config building, so
      // the config error doesn't mask the more actionable usage error.
      const h = createHarness();
      const code = await runCli({
        argv: ["typo_tool"],
        env: {}, // no base url, no auth
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
      });
      expect(code).toBe(EXIT_CODES.USAGE);
      expect(h.getStderr()).toContain("Unknown tool");
    });

    it("prefers EX_USAGE (missing tool name) over EX_CONFIG with no config", async () => {
      const h = createHarness();
      const code = await runCli({
        argv: ['{"foo":"bar"}'], // JSON arg but no tool name
        env: {}, // no config either
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
      });
      expect(code).toBe(EXIT_CODES.USAGE);
      expect(h.getStderr()).toContain("tool name is required");
    });

    it("returns EX_CONFIG (78) when --base-url is invalid", async () => {
      const h = createHarness();
      const code = await runCli({
        argv: ["list_repositories", "{}", "--base-url", "not-a-url"],
        env: { OPENL_PERSONAL_ACCESS_TOKEN: "openl_pat_test" },
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
      });
      expect(code).toBe(EXIT_CODES.CONFIG);
      expect(h.getStderr()).toContain("Invalid base URL");
    });
  });

  describe("--version", () => {
    it("prints version and exits 0 with no config", async () => {
      const h = createHarness();
      const code = await runCli({
        argv: ["--version"],
        env: {},
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
      });
      expect(code).toBe(EXIT_CODES.OK);
      expect(h.getStdout()).toMatch(/openl-mcp \d+\.\d+\.\d+/);
      expect(h.getStderr()).toBe("");
    });

    it("supports the -V short form", async () => {
      const h = createHarness();
      const code = await runCli({
        argv: ["-V"],
        env: {},
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
      });
      expect(code).toBe(EXIT_CODES.OK);
      expect(h.getStdout()).toMatch(/openl-mcp \d+\.\d+\.\d+/);
    });
  });

  describe("tool-specific --help", () => {
    it("renders schema details when a known tool is passed with --help", async () => {
      const h = createHarness();
      const code = await runCli({
        argv: ["list_repositories", "--help"],
        env: {},
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
      });
      expect(code).toBe(EXIT_CODES.OK);
      const out = h.getStdout();
      expect(out).toMatch(/^list_repositories$/m); // bare-name header
      expect(out).toContain("Arguments:");
      expect(out).toContain("response_format");
    });

    it("returns EX_USAGE (64) for unknown tool with --help", async () => {
      const h = createHarness();
      const code = await runCli({
        argv: ["fake_tool_xyz", "--help"],
        env: {},
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
      });
      expect(code).toBe(EXIT_CODES.USAGE);
      expect(h.getStderr()).toContain("Unknown tool");
    });
  });

  describe("documentation consistency", () => {
    /**
     * Defends against the class of bug where help text or README examples
     * reference a tool that's been disabled or renamed. Extracts every tool
     * name the catalog lists and asserts each is actually registered. (Names
     * are bare — the `openl_` prefix is an MCP-wire concern, absent here.)
     */
    it("every tool listed in the --help catalog is actually registered", async () => {
      const h = createHarness();
      const code = await runCli({
        argv: ["--help"],
        env: {},
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
      });
      expect(code).toBe(EXIT_CODES.OK);

      // --list-tools dump → array of registered tool definitions
      const listH = createHarness();
      const listCode = await runCli({
        argv: ["--list-tools"],
        env: {},
        stdin: listH.stdin,
        stdout: listH.stdout,
        stderr: listH.stderr,
      });
      // Guard against the second invocation silently failing — without this,
      // a broken --list-tools could still leave partial JSON that parses
      // and the regression check would pass for the wrong reason.
      expect(listCode).toBe(EXIT_CODES.OK);
      const registered = new Set(
        (JSON.parse(listH.getStdout()) as Array<{ name: string }>).map((t) => t.name),
      );

      // Catalog entries are indented "  <name>   <Title>" lines under the
      // "Available tools" header; category headers ("Repository:") are not
      // indented and are skipped. Scope to the catalog so prose/usage lines
      // (which mention `openl_list_repositories` as the wire-name example) don't
      // get mistaken for tool names.
      const catalog = h.getStdout().slice(h.getStdout().indexOf("Available tools"));
      const listed = new Set(
        [...catalog.matchAll(/^ {2}([a-z][a-z_]+)\s{2,}\S/gm)].map((m) => m[1]),
      );
      expect(listed.size).toBeGreaterThan(0);
      const missing = [...listed].filter((name) => !registered.has(name));
      expect(missing).toEqual([]);
    });

    /**
     * Defends against the Zod `.optional().default()` ordering trap: that
     * chain makes `z.toJSONSchema()` mark a field as required in the output
     * schema even though it's optional for input. Defaulted pagination /
     * options fields must never appear in a tool's `required` array, or
     * agents reading the schema will think they're mandatory.
     */
    it("does not mark defaulted optional fields (limit/offset/failures/unpaged) as required", async () => {
      const listH = createHarness();
      const code = await runCli({
        argv: ["--list-tools"],
        env: {},
        stdin: listH.stdin,
        stdout: listH.stdout,
        stderr: listH.stderr,
      });
      expect(code).toBe(EXIT_CODES.OK);

      const tools = JSON.parse(listH.getStdout()) as Array<{
        name: string;
        inputSchema: { required?: string[] };
      }>;
      const defaultedOptionals = ["limit", "offset", "failures", "unpaged"];
      const offenders = tools
        .map((t) => ({
          name: t.name,
          bad: (t.inputSchema.required ?? []).filter((r) => defaultedOptionals.includes(r)),
        }))
        .filter((t) => t.bad.length > 0);
      expect(offenders).toEqual([]);
    });
  });

  describe("--help grouping", () => {
    it("groups tools by category in --help output", async () => {
      const h = createHarness();
      const code = await runCli({
        argv: ["--help"],
        env: {},
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
      });
      expect(code).toBe(EXIT_CODES.OK);
      const out = h.getStdout();
      // Spot-check each category header is rendered
      expect(out).toContain("Repository:");
      expect(out).toContain("Project:");
      expect(out).toContain("Rules & Tables:");
      expect(out).toContain("Trace:");
      expect(out).toContain("Deployment:");
    });
  });

  describe("prefix-less tool names (CLI drops openl_)", () => {
    it("runs a tool given by its short name (openl_ omitted)", async () => {
      const { client, mock: m } = createMockClient();
      mock = m;
      m.onGet("/repos").reply(200, mockRepositories);

      const h = createHarness();
      const code = await runCli({
        argv: ["list_repositories", '{"response_format":"json"}'],
        env: ENV_OK,
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
        clientFactory: () => client,
      });

      expect(code).toBe(EXIT_CODES.OK);
      expect(h.getStdout()).toContain("Design Repository");
      expect(h.getStderr()).toBe("");
    });

    it("rejects the fully-qualified openl_ name (prefix is not accepted on the CLI)", async () => {
      const h = createHarness();
      const code = await runCli({
        argv: ["openl_list_repositories"],
        env: ENV_OK,
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
      });

      expect(code).toBe(EXIT_CODES.USAGE);
      expect(h.getStderr()).toContain("Unknown tool");
    });


    it("an unknown short name is still EX_USAGE", async () => {
      const h = createHarness();
      const code = await runCli({
        argv: ["does_not_exist"],
        env: ENV_OK,
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
      });
      expect(code).toBe(EXIT_CODES.USAGE);
      expect(h.getStderr()).toContain("Unknown tool");
    });

    it("--help catalog lists every registered tool by its prefix-less name", async () => {
      const helpH = createHarness();
      expect(
        await runCli({ argv: ["--help"], env: {}, stdin: helpH.stdin, stdout: helpH.stdout, stderr: helpH.stderr }),
      ).toBe(EXIT_CODES.OK);
      const listH = createHarness();
      expect(
        await runCli({ argv: ["--list-tools"], env: {}, stdin: listH.stdin, stdout: listH.stdout, stderr: listH.stderr }),
      ).toBe(EXIT_CODES.OK);

      const help = helpH.getStdout();
      const names = (JSON.parse(listH.getStdout()) as Array<{ name: string }>).map((t) => t.name);
      // The CLI is prefix-free everywhere: --list-tools reports bare names (no
      // openl_), and each appears verbatim on a catalog line in --help.
      expect(names.length).toBeGreaterThan(0);
      for (const name of names) {
        expect(name.startsWith("openl_")).toBe(false);
        expect(help).toMatch(new RegExp(`^\\s+${name}\\b`, "m"));
      }
    });
  });

  describe("positional <url> argument", () => {
    it("parseArgs captures a leading http(s) URL as baseUrlPositional, not toolName", () => {
      const p = parseArgs(["https://studio.example.com"]);
      expect(p.baseUrlPositional).toBe("https://studio.example.com");
      expect(p.toolName).toBeUndefined();
      expect(p.errors).toEqual([]);
    });

    it("parseArgs accepts <url> and <tool> in either order", () => {
      const a = parseArgs(["http://localhost:8080", "list_repositories"]);
      expect(a.baseUrlPositional).toBe("http://localhost:8080");
      expect(a.toolName).toBe("list_repositories");

      const b = parseArgs(["list_repositories", "http://localhost:8080"]);
      expect(b.baseUrlPositional).toBe("http://localhost:8080");
      expect(b.toolName).toBe("list_repositories");
    });

    it("parseArgs treats a non-http(s) URL-ish token as a tool name", () => {
      const p = parseArgs(["mailto:x@y.z"]);
      expect(p.baseUrlPositional).toBeUndefined();
      expect(p.toolName).toBe("mailto:x@y.z");
    });

    it("parseArgs reports a second URL as an error, not as the tool name", () => {
      const p = parseArgs(["http://a:1", "http://b:2", "list_repositories"]);
      expect(p.baseUrlPositional).toBe("http://a:1");
      // The genuine tool name is kept; the duplicate URL is the error, so the
      // diagnostic points at the right token.
      expect(p.toolName).toBe("list_repositories");
      expect(p.errors).toContain("Multiple base URLs are not allowed: http://b:2");
    });

    it("parseArgs reports a third positional (after <url> <tool>) as unexpected", () => {
      const p = parseArgs(["http://localhost:8080", "list_repositories", "extra"]);
      expect(p.baseUrlPositional).toBe("http://localhost:8080");
      expect(p.toolName).toBe("list_repositories");
      expect(p.errors).toContain("Unexpected positional argument: extra");
    });

    it("isCliInvocation: false for a bare URL or no args (server launch), true otherwise", () => {
      expect(isCliInvocation(parseArgs([]))).toBe(false);
      expect(isCliInvocation(parseArgs(["http://localhost:8080"]))).toBe(false);
      // URL + server flags, still no tool → server launch
      expect(isCliInvocation(parseArgs(["http://localhost:8080", "--token", "t"]))).toBe(false);
      // anything tool-ish → CLI
      expect(isCliInvocation(parseArgs(["list_repositories"]))).toBe(true);
      expect(isCliInvocation(parseArgs(["http://localhost:8080", "list_repositories"]))).toBe(true);
      expect(isCliInvocation(parseArgs(["--help"]))).toBe(true);
      expect(isCliInvocation(parseArgs(["--list-tools"]))).toBe(true);
      expect(isCliInvocation(parseArgs(["{}"]))).toBe(true); // json arg but no tool → CLI renders the usage error
      expect(isCliInvocation(parseArgs(["--bogus"]))).toBe(true); // parse error → CLI renders it
    });

    it("uses the positional URL as the base URL for the tool call, over OPENL_BASE_URL", async () => {
      const { client, mock: m } = createMockClient();
      mock = m;
      m.onGet("/repos").reply(200, mockRepositories);

      let seenBaseUrl: string | undefined;
      const h = createHarness();
      const code = await runCli({
        argv: ["http://localhost:8080", "list_repositories"],
        env: { OPENL_BASE_URL: "http://env-host:1234" }, // positional must win
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
        clientFactory: (cfg) => {
          seenBaseUrl = cfg.baseUrl;
          return client;
        },
      });

      expect(code).toBe(EXIT_CODES.OK);
      expect(seenBaseUrl).toBe("http://localhost:8080");
      expect(h.getStdout()).toContain("Design Repository");
    });

    it("positional URL beats --base-url when both are given", async () => {
      const { client, mock: m } = createMockClient();
      mock = m;
      m.onGet("/repos").reply(200, mockRepositories);

      let seenBaseUrl: string | undefined;
      const h = createHarness();
      const code = await runCli({
        argv: [
          "http://positional:8080",
          "list_repositories",
          "--base-url",
          "http://flag-host:5555",
        ],
        env: {},
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
        clientFactory: (cfg) => {
          seenBaseUrl = cfg.baseUrl;
          return client;
        },
      });

      expect(code).toBe(EXIT_CODES.OK);
      expect(seenBaseUrl).toBe("http://positional:8080");
    });
  });

  describe("classifyError", () => {
    it("returns the exitCode carried by a CliError", () => {
      expect(classifyError(new CliError("bad config", EXIT_CODES.CONFIG))).toBe(EXIT_CODES.CONFIG);
      expect(classifyError(new CliError("bad data", EXIT_CODES.DATAERR))).toBe(EXIT_CODES.DATAERR);
    });

    it.each([
      "connect ECONNREFUSED 127.0.0.1:8080",
      "ETIMEDOUT",
      "getaddrinfo ENOTFOUND studio.example.com",
      "EAI_AGAIN dns lookup failed",
      "read ECONNRESET",
      "connect EHOSTUNREACH",
      "connect ENETUNREACH",
    ])("maps network error %p to EX_UNAVAILABLE (69)", (msg) => {
      expect(classifyError(new Error(msg))).toBe(EXIT_CODES.UNAVAILABLE);
    });

    it.each([
      // Wrapped form: "OpenL Studio API error (NNN): ..."
      "OpenL Studio API error (401): Unauthorized [GET /repos]",
      "OpenL Studio API error (403): Forbidden [POST /projects]",
      // Raw axios form
      "Request failed with status code 401",
      "Request failed with status code 403",
    ])("maps auth failure %p to EX_NOPERM (77)", (msg) => {
      expect(classifyError(new Error(msg))).toBe(EXIT_CODES.NOPERM);
    });

    it.each([
      "OpenL Studio API error (500): Internal Server Error [GET /repos]",
      "OpenL Studio API error (503): Service Unavailable",
      "Request failed with status code 502",
    ])("maps 5xx %p to EX_UNAVAILABLE (69)", (msg) => {
      expect(classifyError(new Error(msg))).toBe(EXIT_CODES.UNAVAILABLE);
    });

    it("treats 4xx that isn't 401/403 (e.g. 404) as GENERIC (1)", () => {
      expect(classifyError(new Error("OpenL Studio API error (404): Not Found"))).toBe(EXIT_CODES.GENERIC);
    });

    it("falls back to GENERIC (1) for unclassifiable errors", () => {
      expect(classifyError(new Error("something unexpected happened"))).toBe(EXIT_CODES.GENERIC);
      expect(classifyError("a bare string error")).toBe(EXIT_CODES.GENERIC);
      expect(classifyError(undefined)).toBe(EXIT_CODES.GENERIC);
    });

    it("does not treat a bare '401' without HTTP context as auth failure", () => {
      // "processed 401 records" has no (NNN) status nor "status code NNN",
      // so it must not be misread as an auth failure.
      expect(classifyError(new Error("processed 401 records"))).toBe(EXIT_CODES.GENERIC);
    });
  });

  describe("classifyError end-to-end through runCli", () => {
    it("maps a 401 API response to EX_NOPERM (77)", async () => {
      const { client, mock: m } = createMockClient();
      mock = m;
      m.onGet("/repos").reply(401, { message: "Unauthorized" });

      const h = createHarness();
      const code = await runCli({
        argv: ["list_repositories"],
        env: ENV_OK,
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
        clientFactory: () => client,
      });
      expect(code).toBe(EXIT_CODES.NOPERM);
    });

    it("maps a 500 API response to EX_UNAVAILABLE (69)", async () => {
      const { client, mock: m } = createMockClient();
      mock = m;
      m.onGet("/repos").reply(500, { message: "Internal Server Error" });

      const h = createHarness();
      const code = await runCli({
        argv: ["list_repositories"],
        env: ENV_OK,
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
        clientFactory: () => client,
      });
      expect(code).toBe(EXIT_CODES.UNAVAILABLE);
    });
  });

  describe("argument parsing edge cases", () => {
    it("errors when a value-taking flag is missing its value", async () => {
      const h = createHarness();
      const code = await runCli({
        argv: ["list_repositories", "--base-url"], // no value follows
        env: {},
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
      });
      expect(code).toBe(EXIT_CODES.USAGE);
      expect(h.getStderr()).toContain("requires a value");
    });

    it("rejects an invalid --timeout value", async () => {
      const h = createHarness();
      const code = await runCli({
        argv: ["list_repositories", "--timeout", "not-a-number"],
        env: ENV_OK,
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
      });
      expect(code).toBe(EXIT_CODES.USAGE);
      expect(h.getStderr()).toContain("Invalid --timeout");
    });

    it("rejects an invalid OPENL_TIMEOUT env value (EX_CONFIG)", async () => {
      const h = createHarness();
      const code = await runCli({
        argv: ["list_repositories"],
        env: { ...ENV_OK, OPENL_TIMEOUT: "-5" },
        stdin: h.stdin,
        stdout: h.stdout,
        stderr: h.stderr,
      });
      expect(code).toBe(EXIT_CODES.CONFIG);
      expect(h.getStderr()).toContain("Invalid OPENL_TIMEOUT");
    });
  });
});
