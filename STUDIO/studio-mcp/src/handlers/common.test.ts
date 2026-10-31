/**
 * Integration tests of the tool registry of src/handlers/common.ts: the published schemas, the validation of the
 * arguments before any request, and the errors of OpenL Studio as the agent reads them.
 */

import { describe, it, expect } from "vitest";
import { pageOf } from "../testing/openl-api-mocks.js";
import { executeTool, getAllTools } from "./index.js";
import { harness, useToolHarness } from "../testing/harness.js";

useToolHarness();

describe("tool registry", () => {
  it("exposes every tool under a bare name (the openl_ prefix is a wire-only concern)", () => {
    const names = getAllTools().map((t) => t.name);
    // The registry never carries the namespace prefix — it is added/stripped only
    // at the MCP protocol boundary (see constants.test.ts).
    expect(names.some((n) => n.startsWith("openl_"))).toBe(false);
    // The bare names are exactly what executeTool dispatches on.
    expect(names).toContain("list_repositories");
  });

  it("publishes only strict objects, so an unknown argument is refused, not dropped", () => {
    const lenient: string[] = [];
    const visit = (node: unknown, path: string): void => {
      if (Array.isArray(node)) {
        node.forEach((item, index) => visit(item, `${path}[${index}]`));
      } else if (node !== null && typeof node === "object") {
        const schema = node as Record<string, unknown>;
        if (schema.type === "object" && "properties" in schema && schema.additionalProperties !== false) {
          lenient.push(path);
        }
        Object.entries(schema).forEach(([key, value]) => visit(value, `${path}.${key}`));
      }
    };
    getAllTools().forEach((tool) => visit(tool.inputSchema, tool.name));
    expect(lenient).toEqual([]);
  });

  it("publishes no dialect and no safe-integer bounds, but the bounds of a tool", () => {
    const published = JSON.stringify(getAllTools().map((tool) => tool.inputSchema));
    const listTables = getAllTools().find((tool) => tool.name === "list_tables")?.inputSchema;

    expect(published).not.toContain("$schema");
    expect(published).not.toContain(String(Number.MAX_SAFE_INTEGER));
    expect(listTables?.properties).toMatchObject({
      limit: { type: "integer", exclusiveMinimum: 0, maximum: 200 },
      offset: { type: "integer", minimum: 0 },
    });
  });

  it("advertises as destructive only the tools that replace or remove data", () => {
    // MCP takes a tool that is not read-only for destructive unless it says otherwise
    const destructive = getAllTools()
      .filter(({ annotations }) => !annotations?.readOnlyHint && annotations?.destructiveHint !== false)
      .map(({ name }) => name);
    expect(destructive.toSorted()).toEqual([
      "cancel_merge_conflicts", "close_project", "delete_project", "delete_project_branch", "delete_project_file",
      "delete_table", "deploy_project", "edit_table", "merge_project_branches", "move_project_file",
      "resolve_merge_conflicts", "restore_project_local_change", "update_table", "write_project_file",
    ]);
  });

  it("leaves a field with a default out of the required ones", () => {
    const required = (name: string): unknown =>
      getAllTools().find((tool) => tool.name === name)?.inputSchema.required;
    expect(required("read_project_file")).toEqual(["projectId"]);
    expect(required("list_projects")).toBeUndefined();
  });
});

describe("argument validation", () => {
  it("rejects unknown arguments before a handler can call the backend", async () => {
    await expect(
      executeTool("list_repositories", { limti: 10 }, harness.client)
    ).rejects.toThrow(/Invalid arguments for list_repositories.*limti.*list_repositories takes only: .*\blimit\b/s);
    expect(harness.mockHttp.history.get).toHaveLength(0);
  });

  it.each([
    ["an unknown argument inside another one", "list_tables",
      { projectId: "p1", properties: { state: 1 } },
      /properties/],
    ["a missing argument", "get_table", { projectId: "p1" }, /tableId/],
  ])("names no arguments for %s", async (_case, toolName, args, rejected) => {
    const error = await executeTool(toolName, args, harness.client).catch((thrown: unknown) => thrown);

    expect((error as Error).message).toMatch(rejected);
    expect((error as Error).message).not.toContain("takes only");
  });

  it.each([
    ["project_status", { projectId: "p1", timeoutMs: 600_001 }, /timeoutMs/],
    ["create_upload_link", { projectId: "p1", path: "a.bin", size: -1 }, /size/],
    ["get_test_results", { projectId: "p1", unpaged: true, offset: 0 }, /mutually exclusive/],
    ["list_projects", { limit: 300 }, /Invalid arguments for list_projects.*limit/s],
    ["list_projects", { limit: 0 }, /Invalid arguments for list_projects.*limit/s],
    ["get_table", { projectId: "p1", tableId: "t1", response_format: "json" }, /response_format/],
    ["list_tables", { projectId: "p1", offset: 2 ** 60 }, /Invalid arguments for list_tables.*offset/s],
  ])("enforces the published %s constraints at runtime", async (toolName, args, message) => {
    await expect(executeTool(toolName, args, harness.client)).rejects.toThrow(message);
    expect(harness.mockHttp.history.get).toHaveLength(0);
  });

  it("names the tool that lists the missing identifier", async () => {
    await expect(executeTool("get_project", {}, harness.client))
      .rejects.toThrow(/Invalid arguments for get_project.*projectId.*openl_list_projects/s);
  });
});

describe("a table named by its name", () => {
  const tablesPath = "/projects/p1/tables";

  it("is read by the id of the one table of that name, with a note naming the id", async () => {
    harness.mockHttp.onGet(`${tablesPath}/AgeFactor`).reply(404, { message: "The table is not found." });
    harness.mockHttp.onGet(tablesPath, { params: { name: "AgeFactor", size: 50 } })
      .reply(200, pageOf([{ id: "T1", name: "AgeFactor" }, { id: "T2", name: "AgeFactorOld" }], { total: 2 }));
    harness.mockHttp.onGet(`${tablesPath}/T1`).reply(200, { id: "T1", name: "AgeFactor", tableType: "RawSource", source: [[{ value: "x" }]] });

    const result = await executeTool("get_table", { projectId: "p1", tableId: "AgeFactor" }, harness.client);

    expect(JSON.parse(result.content[0].text).data.rows).toEqual([["x"]]);
    expect(result.content[1].text).toBe("'AgeFactor' is the name of a table, not its id: its id is 'T1', which the table tools take.");
  });

  it("is refused with the ids of the tables of that name when there are several", async () => {
    harness.mockHttp.onGet(`${tablesPath}/Rate`).reply(404, { message: "The table is not found." });
    harness.mockHttp.onGet(tablesPath).reply(200, pageOf([
      { id: "T1", name: "Rate", module: "Rates-2026" }, { id: "T2", name: "Rate", module: "Rates-2027" },
    ], { total: 2 }));

    await expect(executeTool("get_table", { projectId: "p1", tableId: "Rate" }, harness.client))
      .rejects.toThrow("'Rate' is the name of 2 tables, not an id: give the id of one of them, T1 (Rates-2026), T2 (Rates-2027).");
  });

  it("is not looked for when no table has that name", async () => {
    harness.mockHttp.onGet(`${tablesPath}/T9`).reply(404, { message: "The table is not found." });
    harness.mockHttp.onGet(tablesPath).reply(200, pageOf([], { total: 0 }));

    await expect(executeTool("get_table", { projectId: "p1", tableId: "T9" }, harness.client)).rejects.toThrow(/not found/);
  });
});

describe("errors of OpenL Studio", () => {
  const callAndCatch = async (): Promise<Error> => {
    try {
      await executeTool("edit_table", {
        projectId: "p1", tableId: "t1", version: "v1", action: "update_cell", row: 0, column: 0, value: "x",
      }, harness.client);
    } catch (e) {
      return e as Error;
    }
    throw new Error("expected the tool to throw");
  };

  it("surfaces a ValidationError's field and global errors in the message", async () => {
    harness.mockHttp.onPost(/\/tables\/t1\/actions$/).reply(400, {
      code: "VALIDATION_ERROR",
      message: "Validation failed",
      fields: [
        { field: "cells", code: "TOO_TALL", message: "column height 6 exceeds table height 5", rejectedValue: [[{ value: "a" }]] },
        { field: "position", message: "must be >= 1", rejectedValue: 0 },
      ],
      errors: [{ code: "G1", message: "the table has no room to grow" }],
    });

    const msg = (await callAndCatch()).message;
    // Top-level message kept as the headline.
    expect(msg).toContain("Validation failed");
    // Each field error surfaces with its field, reason, and rejected value.
    expect(msg).toContain("Field errors:");
    expect(msg).toContain("cells: column height 6 exceeds table height 5");
    expect(msg).toContain("position: must be >= 1 (rejected: 0)");
    // Additional global errors surface too.
    expect(msg).toContain("Additional errors:");
    expect(msg).toContain("the table has no room to grow");
  });

  it("uses the field errors as the headline when the ValidationError has no top-level message", async () => {
    harness.mockHttp.onPost(/\/tables\/t1\/actions$/).reply(400, {
      code: "VALIDATION_ERROR",
      fields: [{ field: "row", message: "must be within the table" }],
    });

    const msg = (await callAndCatch()).message;
    expect(msg).toContain("Field errors:");
    expect(msg).toContain("row: must be within the table");
  });

  it("leaves a simple BaseError message intact (no field section)", async () => {
    harness.mockHttp.onPost(/\/tables\/t1\/actions$/).reply(409, { code: "LOCKED", message: "Project is locked" });

    const msg = (await callAndCatch()).message;
    expect(msg).toContain("Project is locked");
    expect(msg).not.toContain("Field errors");
  });

  it("passes on what OpenL Studio says to do about a refused token, so the agent can tell the user", async () => {
    harness.mockHttp.onPost(/\/tables\/t1\/actions$/).reply(401, {
      code: "openl.error.401.token.expired.message",
      message: "The personal access token has expired. Create a new token in Personal Access Tokens of OpenL Studio "
        + "and use it instead.",
    });

    const msg = (await callAndCatch()).message;
    expect(msg).toBe("OpenL Studio API error (401): The personal access token has expired. Create a new token in "
      + "Personal Access Tokens of OpenL Studio and use it instead.");
  });

  it("names the call that opens a project Studio refuses to read while it is closed", async () => {
    harness.mockHttp.onPost(/\/tables\/t1\/actions$/).reply(409, {
      code: "openl.error.409.project.not.opened.message",
      message: "The project is not opened.",
    });

    const msg = (await callAndCatch()).message;
    expect(msg).toBe("OpenL Studio API error (409): The project is not opened.\nOpen it with openl_open_project first.");
  });

  it("tells the agent to read the table again when a workbook changed outside Studio, as no refresh call exists", async () => {
    harness.mockHttp.onPost(/\/tables\/t1\/actions$/).reply(409, {
      code: "openl.error.409.workbook.changed.outside.message",
      message: "Bank Rating.xlsx changed outside OpenL Studio since it was read. Refresh the project and repeat the edit.",
    });

    const msg = (await callAndCatch()).message;
    expect(msg).toMatch(/changed outside OpenL Studio.*read the table again with openl_get_table/);
    expect(msg).not.toContain("Refresh the project");
  });
});
