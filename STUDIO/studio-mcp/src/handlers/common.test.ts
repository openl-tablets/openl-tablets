/**
 * Integration tests of the tool registry of src/handlers/common.ts: the published schemas, the validation of the
 * arguments before any request, and the errors of OpenL Studio as the agent reads them.
 */

import { describe, it, expect } from "vitest";
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

  it("offers response_format only on the tools whose answer has a Markdown view of its own", () => {
    const formatted = getAllTools()
      .filter((tool) => "response_format" in (tool.inputSchema.properties as Record<string, unknown>))
      .map((tool) => tool.name);
    expect(formatted.toSorted()).toEqual([
      "get_table_dependencies", "get_test_results", "get_test_results_by_table", "get_test_results_summary",
      "list_deployments", "list_projects", "list_repositories", "list_tables",
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
    ).rejects.toThrow(/Invalid arguments for list_repositories.*limti/s);
    expect(harness.mockHttp.history.get).toHaveLength(0);
  });

  it.each([
    ["project_status", { projectId: "p1", timeoutMs: 600_001 }, /timeoutMs/],
    ["create_upload_link", { projectId: "p1", path: "a.bin", size: -1 }, /size/],
    ["get_test_results", { projectId: "p1", page: 0, offset: 0 }, /mutually exclusive/],
    ["list_projects", { limit: 300 }, /Invalid arguments for list_projects.*limit/s],
    ["list_projects", { limit: 0 }, /Invalid arguments for list_projects.*limit/s],
    ["list_projects", { response_format: "xml" }, /markdown_concise.*markdown_detailed/],
    ["get_table", { projectId: "p1", tableId: "t1", response_format: "json" }, /response_format/],
  ])("enforces the published %s constraints at runtime", async (toolName, args, message) => {
    await expect(executeTool(toolName, args, harness.client)).rejects.toThrow(message);
    expect(harness.mockHttp.history.get).toHaveLength(0);
  });

  it("names the tool that lists the missing identifier", async () => {
    await expect(executeTool("get_project", {}, harness.client))
      .rejects.toThrow(/Invalid arguments for get_project.*projectId.*openl_list_projects/s);
  });
});

describe("errors of OpenL Studio", () => {
  const callAndCatch = async (): Promise<Error> => {
    try {
      await executeTool("update_table_cell", {
        projectId: "p1", tableId: "t1", version: "v1", row: 0, column: 0, value: "x",
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
