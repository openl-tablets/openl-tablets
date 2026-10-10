/**
 * The optional server-side tool allow-list (`OPENL_MCP_TOOLS`).
 *
 * Two properties carry the weight:
 *   1. Unset means unchanged — every registered tool is served, so no existing
 *      deployment is altered by this feature existing.
 *   2. Set gates EXECUTION, not just discovery. Filtering tools/list alone would
 *      be decoration: a client that already knows a name could still call it.
 */

import { describe, it, expect, beforeEach, afterEach } from "@jest/globals";
import { parseToolAllowList, toolAllowList, mcpToolName } from "../src/constants.js";

describe("parseToolAllowList", () => {
  it("returns null when unset, meaning every tool is served", () => {
    expect(parseToolAllowList(undefined)).toBeNull();
  });

  it.each(["", "   ", ",", " , , "])(
    "fails CLOSED on %p — set but naming nothing serves nothing",
    (raw) => {
      // The two failure modes are not symmetric. Reading an empty value as "unset"
      // would hand an operator who wrote OPENL_MCP_TOOLS= intending maximum
      // restriction every tool instead, silently. Serving nothing is useless but
      // visible, and the server says why on stderr.
      const set = parseToolAllowList(raw);
      expect(set).not.toBeNull();
      expect(set!.size).toBe(0);
    },
  );

  it("still serves everything only when the variable is genuinely unset", () => {
    expect(parseToolAllowList(undefined)).toBeNull();
  });

  it("accepts bare names", () => {
    const set = parseToolAllowList("get_table,list_tables");
    expect([...set!].sort()).toEqual(["get_table", "list_tables"]);
  });

  it("accepts wire-prefixed names, because the prefix is a wire concern", () => {
    const set = parseToolAllowList("openl_get_table, openl_list_tables");
    expect([...set!].sort()).toEqual(["get_table", "list_tables"]);
  });

  it("tolerates whitespace and mixed forms", () => {
    const set = parseToolAllowList("  openl_get_table ,list_tables,  , get_started ");
    expect([...set!].sort()).toEqual(["get_started", "get_table", "list_tables"]);
  });
});

describe("toolAllowList (process env)", () => {
  let saved: string | undefined;
  beforeEach(() => {
    saved = process.env.OPENL_MCP_TOOLS;
    delete process.env.OPENL_MCP_TOOLS;
  });
  afterEach(() => {
    if (saved === undefined) delete process.env.OPENL_MCP_TOOLS;
    else process.env.OPENL_MCP_TOOLS = saved;
  });

  it("is null by default", () => {
    expect(toolAllowList()).toBeNull();
  });

  it("reflects OPENL_MCP_TOOLS", () => {
    process.env.OPENL_MCP_TOOLS = "openl_get_table";
    const set = toolAllowList();
    expect(set?.has("get_table")).toBe(true);
    expect(set?.has("update_table")).toBe(false);
    // and the wire name round-trips back to the gated registry name
    expect(mcpToolName("get_table")).toBe("openl_get_table");
  });
});
