/** Unit tests for MCP input contracts. */

import { describe, expect, it } from "vitest";
import { z } from "zod";
import {
  closeProjectSchema,
  copyTableSchema,
  createProjectSchema,
  createProjectTableSchema,
  deleteProjectBranchSchema,
  deleteProjectSchema,
  editTableSchema,
  getProjectRevisionsSchema,
  getTableDependenciesSchema,
  listProjectBranchesSchema,
  listProjectsSchema,
  mergeProjectBranchesSchema,
  projectIdSchema,
  runTableSchema,
  saveProjectSchema,
  updateTableSchema,
} from "./schemas.js";

const tableGrid = {
  rows: [["Rules void Rate()"]],
  merges: [[0, 0, 2, 2]],
};

const cellWriteSchemas = [
  {
    name: "create table",
    schema: createProjectTableSchema,
    input: (value: unknown) => ({ projectId: "p1", module: "Main", name: "Rate", rows: [[value]] }),
  },
  {
    name: "full update",
    schema: updateTableSchema,
    input: (value: unknown) => ({ projectId: "p1", tableId: "t1", version: "v1", rows: [[value]] }),
  },
  ...([
    ["append_rows", (value: unknown) => ({ cells: [[value]] })],
    ["append_columns", (value: unknown) => ({ cells: [[value]] })],
    ["insert_rows", (value: unknown) => ({ position: 1, cells: [[value]] })],
    ["insert_columns", (value: unknown) => ({ position: 1, cells: [[value]] })],
    ["update_row", (value: unknown) => ({ position: 0, cells: [[value]] })],
    ["update_column", (value: unknown) => ({ position: 0, cells: [[value]] })],
    ["update_cell", (value: unknown) => ({ row: 0, column: 0, value })],
    ["update_range", (value: unknown) => ({ row: 0, column: 0, cells: [[value, null]] })],
  ] as const).map(([action, args]) => ({
    name: `edit ${action}`,
    schema: editTableSchema,
    input: (value: unknown) => ({ projectId: "p1", tableId: "t1", version: "v1", action, ...args(value) }),
  })),
];

describe("opaque project identifiers", () => {
  it("rejects an empty ID without normalizing non-empty values", () => {
    expect(projectIdSchema.safeParse("").success).toBe(false);
    expect(projectIdSchema.parse(" design:Rules ")).toBe(" design:Rules ");
    expect(projectIdSchema.parse(" ")).toBe(" ");
  });
});

describe("table contracts", () => {
  it("takes a table in the shape openl_get_table answers it: rows, merges and formulas", () => {
    const formulas = [[0, 1, "=B2*2"]];

    expect(updateTableSchema.parse({ projectId: "p1", tableId: "t1", version: "v1", ...tableGrid, formulas }))
      .toMatchObject({ ...tableGrid, formulas });
    expect(createProjectTableSchema.safeParse({ projectId: "p1", module: "Main", name: "Rate", ...tableGrid }).success)
      .toBe(true);
  });

  it.each(cellWriteSchemas)("accepts Studio multi-value arrays for $name", ({ schema, input }) => {
    for (const value of [
      ["MA2", "FA+", "SPA"],
      [42],
      [true],
      [null, "ACME, Inc", "C:\\"],
      ["\uFEFFStudio preserves BOM boundaries\uFEFF"],
    ]) {
      expect(schema.safeParse(input(value)).success).toBe(true);
    }
  });

  it.each(cellWriteSchemas)("rejects cell structures that are not a scalar or a flat array for $name", ({ schema, input }) => {
    for (const value of [[["nested"]], { value: "x" }, [{ unsupported: true }]]) {
      expect(schema.safeParse(input(value)).success).toBe(false);
    }
  });

  it("publishes the same scalar-or-array value domain without an object alternative", () => {
    const json = z.toJSONSchema(editTableSchema) as Record<string, any>;
    const serializedValueSchema = JSON.stringify(json.properties.value);

    expect(serializedValueSchema).toContain('"type":"array"');
    expect(serializedValueSchema).not.toContain('"type":"object"');
  });

  it.each(cellWriteSchemas.filter(({ name }) => name !== "create table"))(
    "requires the version of the table the write of $name is based on", ({ schema, input }) => {
      const { version: _, ...args } = input("x") as { version?: string };

      expect(schema.safeParse(args).success).toBe(false);
    });

  it("explains the supported cell-value domain in validation errors", () => {
    const objectResult = editTableSchema.safeParse({
      projectId: "p1", tableId: "t1", version: "v1", action: "update_cell", row: 0, column: 0, value: { unsupported: true },
    });

    expect(objectResult.success).toBe(false);
    if (!objectResult.success) {
      expect(z.prettifyError(objectResult.error)).toMatch(
        /Cell value must be a string, number, boolean, null, or a one-dimensional array/,
      );
    }
  });

  it("refuses a merge or a formula that is not one", () => {
    const update = (extra: object): boolean =>
      updateTableSchema.safeParse({ projectId: "p1", tableId: "t1", version: "v1", rows: [["x"]], ...extra }).success;

    expect(update({ merges: [[0, 0, 0, 2]] })).toBe(false);
    expect(update({ merges: [[0, 0, 2]] })).toBe(false);
    expect(update({ formulas: [[0, 0, "B2*2"]] })).toBe(false);
  });

  it("requires a nonblank name and the rows when creating", () => {
    expect(createProjectTableSchema.safeParse({ projectId: "p1", module: "Main", rows: [["x"]] }).success).toBe(false);
    expect(createProjectTableSchema.safeParse({ projectId: "p1", module: "Main", name: "  ", rows: [["x"]] }).success)
      .toBe(false);
    expect(createProjectTableSchema.safeParse({ projectId: "p1", module: "Main", name: "Rate" }).success).toBe(false);
  });

  it("requires at least one appended row of at least one cell", () => {
    const append = (cells: unknown): boolean => editTableSchema.safeParse({
      projectId: "p1", tableId: "t1", version: "v1", action: "append_rows", cells,
    }).success;

    expect(append([])).toBe(false);
    expect(append([[null]])).toBe(true);
    expect(append([[]])).toBe(false);
  });

  it("accepts only an xlsx modulePath", () => {
    const base = { projectId: "p1", module: "Rules", name: "Rate", rows: [["x"]] };
    expect(createProjectTableSchema.safeParse({ ...base, modulePath: "rules/Rules.xlsx" }).success).toBe(true);
    expect(createProjectTableSchema.safeParse({ ...base, modulePath: "rules/Rules.xls" }).success).toBe(false);
  });
});

describe("table workflow schemas", () => {
  it("accepts named and positional inputs, also as a JSON string, and nothing but an object or an array", () => {
    expect(runTableSchema.safeParse({ projectId: "p1", tableId: "t1", inputJson: [42] }).success).toBe(true);
    expect(runTableSchema.safeParse({ projectId: "p1", tableId: "t1", inputJson: { params: { age: 25 } } }).success).toBe(true);
    expect(runTableSchema.safeParse({ projectId: "p1", tableId: "t1", inputJson: { params: [42] } }).success).toBe(true);
    expect(runTableSchema.safeParse({ projectId: "p1", tableId: "t1", inputJson: '{"params":[42]}' }).success).toBe(true);
    expect(runTableSchema.safeParse({ projectId: "p1", tableId: "t1", inputJson: "invalid" }).success).toBe(false);
    expect(runTableSchema.safeParse({ projectId: "p1", tableId: "t1", inputJson: "42" }).success).toBe(false);
  });

  it("enforces distinct project-graph and table-neighborhood options", () => {
    expect(getTableDependenciesSchema.safeParse({ projectId: "p1", module: "Main", layer: "datatype" }).success).toBe(true);
    expect(getTableDependenciesSchema.safeParse({ projectId: "p1", tableId: "t1", direction: "DEPENDENCIES", depth: 2 }).success).toBe(true);
    expect(getTableDependenciesSchema.safeParse({ projectId: "p1", direction: "DEPENDENCIES" }).success).toBe(false);
    expect(getTableDependenciesSchema.safeParse({ projectId: "p1", tableId: "t1", module: "Main" }).success).toBe(false);
    expect(getTableDependenciesSchema.safeParse({ projectId: "p1", tableId: "t1", layer: "datatype" }).success).toBe(false);
    expect(getTableDependenciesSchema.safeParse({ projectId: "p1", layer: "unknown" }).success).toBe(false);
  });

  it("requires copy destinations and validates new module paths", () => {
    const valid = { projectId: "p1", tableId: "t1", module: "Main", name: "CopiedRate" };
    expect(copyTableSchema.safeParse(valid).success).toBe(true);
    expect(copyTableSchema.safeParse({ ...valid, modulePath: "rules/Copy.xlsx" }).success).toBe(true);
    expect(copyTableSchema.safeParse({ ...valid, modulePath: "rules/Copy.xls" }).success).toBe(false);
    expect(copyTableSchema.safeParse({ ...valid, name: " " }).success).toBe(false);
  });
});

describe("project branch and merge schemas", () => {
  it("defaults branch listings to project scope and accepts repository merge-target discovery", () => {
    expect(listProjectBranchesSchema.parse({ projectId: "p1" }).scope).toBe("project");
    expect(listProjectBranchesSchema.parse({ projectId: "p1", scope: "repository" }).scope).toBe("repository");
    expect(listProjectBranchesSchema.safeParse({ projectId: "p1", scope: "all" }).success).toBe(false);

    const jsonSchema = z.toJSONSchema(listProjectBranchesSchema, { io: "input" });
    expect(jsonSchema.required).not.toContain("scope");
    expect(jsonSchema.properties?.scope).toMatchObject({
      default: "project",
      enum: ["project", "repository"],
    });
  });

  it("requires explicit confirmation for protected-branch force", () => {
    const merge = { projectId: "p1", otherBranch: "release", mode: "send", force: true } as const;
    expect(mergeProjectBranchesSchema.safeParse(merge).success).toBe(false);
    expect(mergeProjectBranchesSchema.safeParse({ ...merge, confirmForce: true }).success).toBe(true);
  });

  it("requires exact deletion confirmations and force acknowledgement", () => {
    expect(deleteProjectSchema.safeParse({ projectId: "p1" }).success).toBe(false);
    expect(deleteProjectSchema.safeParse({ projectId: "p1", confirmProjectName: "Rating" }).success).toBe(true);
    const branch = { projectId: "p1", branch: "feature/rate", confirmBranchName: "feature/rate" };
    expect(deleteProjectBranchSchema.safeParse(branch).success).toBe(true);
    expect(deleteProjectBranchSchema.safeParse({ ...branch, confirmBranchName: "feature/other" }).success).toBe(false);
    expect(deleteProjectBranchSchema.safeParse({ ...branch, force: true }).success).toBe(false);
    expect(deleteProjectBranchSchema.safeParse({ ...branch, force: true, confirmForce: true }).success).toBe(true);
  });
});

describe("current project filters", () => {
  it("accepts DELETED and rejects removed ARCHIVED status", () => {
    expect(listProjectsSchema.safeParse({ status: "DELETED" }).success).toBe(true);
    expect(listProjectsSchema.safeParse({ status: "ARCHIVED" }).success).toBe(false);
  });

  it("addresses revision history by project ID and keeps pagination forms exclusive", () => {
    expect(getProjectRevisionsSchema.safeParse({ projectId: "design:Rules", offset: 25 }).success).toBe(true);
    expect(getProjectRevisionsSchema.safeParse({ projectId: "design:Rules", offset: 25, page: 0 }).success).toBe(false);
    expect(getProjectRevisionsSchema.safeParse({
      projectId: "design:Rules",
      repository: "design",
      name: "Rules",
      branch: "main",
    }).success).toBe(false);
  });
});

describe("commit comments", () => {
  const project = { projectId: "p1" };
  const lorem = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et "
    + "dolore magna aliqua. ";
  // Neither the 255th nor the 256th character is a space, so the trimmed comment of a save keeps its length.
  const comment = (length: number) => lorem.repeat(3).slice(0, length);
  const cases = [
    ["save", saveProjectSchema, project],
    ["close", closeProjectSchema, project],
    ["delete", deleteProjectSchema, { ...project, confirmProjectName: "Rating" }],
    ["create", createProjectSchema, { repository: "design", name: "Rating" }],
  ] as const;

  it.each(cases)("%s states and keeps the 255 characters Studio stores", (_, schema, base) => {
    expect(schema.safeParse({ ...base, comment: comment(255) }).success).toBe(true);
    const tooLong = schema.safeParse({ ...base, comment: comment(256) });
    expect(tooLong.success).toBe(false);
    expect(tooLong.error?.issues[0]).toMatchObject({
      path: ["comment"],
      message: "A commit comment has at most 255 characters.",
    });
    expect(z.toJSONSchema(schema, { io: "input" }).properties?.comment).toMatchObject({ maxLength: 255 });
  });
});

describe("project creation", () => {
  it("rejects an empty copy source instead of falling back to blank project creation", () => {
    expect(createProjectSchema.safeParse({ repository: "design", name: "Copy", template: "" }).success).toBe(false);
  });
});
