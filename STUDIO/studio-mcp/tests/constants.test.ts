/**
 * Unit tests for the protocol-boundary tool-name mapping in src/constants.ts.
 *
 * `mcpToolName` / `stripToolPrefix` are the single place the `openl_` namespace
 * prefix is added (on the MCP wire) and removed (before registry dispatch), so
 * the round-trip and the "only strip the leading prefix" semantics are what keep
 * the registry, CLI, and REST paths agreeing on bare names.
 */

import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";

import { describe, it, expect } from "@jest/globals";
import { SERVER_INFO, TOOL_PREFIX, mcpToolName, stripToolPrefix } from "../src/constants.js";

describe("MCP tool-name prefix mapping", () => {
  it("mcpToolName prepends the openl_ namespace to a bare registry name", () => {
    expect(mcpToolName("list_repositories")).toBe("openl_list_repositories");
    expect(mcpToolName("get_table")).toBe(`${TOOL_PREFIX}get_table`);
  });

  it("stripToolPrefix removes the prefix, inverting mcpToolName", () => {
    expect(stripToolPrefix("openl_list_repositories")).toBe("list_repositories");
    for (const bare of ["list_repositories", "get_table", "start_project_tests"]) {
      expect(stripToolPrefix(mcpToolName(bare))).toBe(bare);
    }
  });

  it("stripToolPrefix is a no-op on an already-bare name (clients may omit the prefix)", () => {
    expect(stripToolPrefix("list_repositories")).toBe("list_repositories");
    expect(stripToolPrefix("")).toBe("");
  });

  it("stripToolPrefix strips only the leading prefix, not an interior occurrence", () => {
    // A name that merely embeds the prefix mid-string must survive the round-trip
    // intact — exactly one prefix is added and exactly one is removed.
    expect(stripToolPrefix("openl_openl_x")).toBe("openl_x");
    expect(stripToolPrefix(mcpToolName("export_openl_trace"))).toBe("export_openl_trace");
  });
});

describe("SERVER_INFO", () => {
  // Cross-validate the two independent sources: SERVER_INFO must be derived from
  // package.json, not re-hardcoded. This fails the moment someone copies literal
  // name/version/description back into constants.ts and lets it drift from the
  // package the server is actually published as.
  it("is sourced from package.json (name, version, description)", () => {
    const pkgPath = fileURLToPath(new URL("../package.json", import.meta.url));
    const pkg = JSON.parse(readFileSync(pkgPath, "utf8")) as {
      name: string;
      version: string;
      description: string;
    };

    expect(SERVER_INFO.NAME).toBe(pkg.name);
    expect(SERVER_INFO.VERSION).toBe(pkg.version);
    expect(SERVER_INFO.DESCRIPTION).toBe(pkg.description);
  });
});
