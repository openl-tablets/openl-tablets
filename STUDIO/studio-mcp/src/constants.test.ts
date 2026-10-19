/**
 * Unit tests for src/constants.ts: the protocol-boundary tool-name mapping and the identity of the server.
 *
 * `mcpToolName` is the single place the `openl_` namespace prefix is added (on the MCP wire), so the registry and
 * the REST paths keep bare names.
 */

import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";

import { describe, it, expect } from "vitest";
import { SERVER_INFO, mcpToolName, resolveServerVersion } from "./constants.js";

describe("MCP tool-name prefix mapping", () => {
  it("mcpToolName prepends the openl_ namespace to a bare registry name", () => {
    expect(mcpToolName("list_repositories")).toBe("openl_list_repositories");
  });
});

describe("SERVER_INFO", () => {
  const pkg = JSON.parse(readFileSync(fileURLToPath(new URL("../package.json", import.meta.url)), "utf8")) as {
    name: string;
    version: string;
    description: string;
  };

  // Cross-validate the two independent sources: SERVER_INFO must be derived from
  // package.json, not re-hardcoded. This fails the moment someone copies literal
  // name/version/description back into constants.ts and lets it drift from the
  // package the server is actually published as.
  it("is sourced from package.json (name, version, description)", () => {
    expect(SERVER_INFO.NAME).toBe(pkg.name);
    expect(SERVER_INFO.VERSION).toBe(pkg.version);
    expect(SERVER_INFO.DESCRIPTION).toBe(pkg.description);
  });

  it("reports the version OpenL Studio gives in OPENL_VERSION, else the placeholder of package.json", () => {
    expect(resolveServerVersion("7.0.0")).toBe("7.0.0");
    expect(resolveServerVersion("")).toBe(pkg.version);
    expect(resolveServerVersion(undefined)).toBe(pkg.version);
  });
});
