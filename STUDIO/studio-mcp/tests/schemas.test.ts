/** Unit tests for MCP input contracts. */

import { describe, expect, it } from "@jest/globals";
import {
  appendTableSchema,
  appendTableColumnsSchema,
  appendTableRowsSchema,
  copyTableSchema,
  createProjectSchema,
  createProjectTableSchema,
  deleteProjectBranchSchema,
  deleteProjectSchema,
  getProjectRevisionsSchema,
  getTableDependenciesSchema,
  listProjectBranchesSchema,
  listProjectsSchema,
  mergeProjectBranchesSchema,
  projectIdSchema,
  runTableSchema,
  insertTableColumnsSchema,
  insertTableRowsSchema,
  updateTableCellSchema,
  updateTableColumnSchema,
  updateTableRangeSchema,
  updateTableRowSchema,
  updateTableSchema,
  z,
} from "../src/schemas.js";

const rawTable = {
  tableType: "RawSource" as const,
  name: "Rate",
  source: [[{
    value: "Rules void Rate()",
    cell: "A1",
    colspan: 2,
    rowspan: 2,
    covered: false,
  }]],
};

const rawCellWriteSchemas = [
  {
    name: "create table",
    schema: createProjectTableSchema,
    input: (value: unknown) => ({
      projectId: "p1", moduleName: "Main",
      table: { ...rawTable, source: [[{ value }]] },
    }),
  },
  {
    name: "full update",
    schema: updateTableSchema,
    input: (value: unknown) => ({
      projectId: "p1", tableId: "t1",
      view: { ...rawTable, source: [[{ value }]] },
    }),
  },
  {
    name: "append table",
    schema: appendTableSchema,
    input: (value: unknown) => ({
      projectId: "p1", tableId: "t1",
      appendData: { tableType: "RawSource", rows: [[{ value }]] },
    }),
  },
  {
    name: "append rows",
    schema: appendTableRowsSchema,
    input: (value: unknown) => ({ projectId: "p1", tableId: "t1", cells: [[{ value }]] }),
  },
  {
    name: "append columns",
    schema: appendTableColumnsSchema,
    input: (value: unknown) => ({ projectId: "p1", tableId: "t1", cells: [[{ value }]] }),
  },
  {
    name: "insert rows",
    schema: insertTableRowsSchema,
    input: (value: unknown) => ({ projectId: "p1", tableId: "t1", position: 1, cells: [[{ value }]] }),
  },
  {
    name: "insert columns",
    schema: insertTableColumnsSchema,
    input: (value: unknown) => ({ projectId: "p1", tableId: "t1", position: 1, cells: [[{ value }]] }),
  },
  {
    name: "update row",
    schema: updateTableRowSchema,
    input: (value: unknown) => ({ projectId: "p1", tableId: "t1", position: 0, cells: [{ value }] }),
  },
  {
    name: "update column",
    schema: updateTableColumnSchema,
    input: (value: unknown) => ({ projectId: "p1", tableId: "t1", position: 0, cells: [{ value }] }),
  },
  {
    name: "update cell",
    schema: updateTableCellSchema,
    input: (value: unknown) => ({ projectId: "p1", tableId: "t1", row: 0, column: 0, value }),
  },
  {
    name: "update range",
    schema: updateTableRangeSchema,
    input: (value: unknown) => ({
      projectId: "p1", tableId: "t1", row: 0, column: 0,
      cells: [[{ value }, { value: null }]],
    }),
  },
];

describe("opaque project identifiers", () => {
  it("rejects an empty ID without normalizing non-empty values", () => {
    expect(projectIdSchema.safeParse("").success).toBe(false);
    expect(projectIdSchema.parse(" design:Rules ")).toBe(" design:Rules ");
    expect(projectIdSchema.parse(" ")).toBe(" ");
  });
});

describe("RawSource-only table contracts", () => {
  it("publishes only RawSource for create, update, and append", () => {
    const createJson = z.toJSONSchema(createProjectTableSchema) as Record<string, any>;
    const updateJson = z.toJSONSchema(updateTableSchema) as Record<string, any>;
    const appendJson = z.toJSONSchema(appendTableSchema) as Record<string, any>;

    expect(createJson.properties.table.properties.tableType.const).toBe("RawSource");
    expect(updateJson.properties.view.properties.tableType.const).toBe("RawSource");
    expect(appendJson.properties.appendData.properties.tableType.const).toBe("RawSource");
    expect(JSON.stringify({ createJson, updateJson, appendJson })).not.toMatch(
      /SimpleRules|SmartRules|SpreadsheetAppend|DatatypeAppend|EditableTableView|"style"/,
    );
  });

  it("round-trips the complete raw cell representation", () => {
    expect(createProjectTableSchema.parse({
      projectId: "p1",
      moduleName: "Main",
      table: rawTable,
    }).table).toEqual(rawTable);

    expect(updateTableSchema.parse({
      projectId: "p1",
      tableId: "t1",
      view: rawTable,
    }).view.source[0][0]).toEqual(rawTable.source[0][0]);
  });

  it.each(rawCellWriteSchemas)("accepts Studio multi-value arrays for $name", ({ schema, input }) => {
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

  it.each(rawCellWriteSchemas)("rejects non-representable cell structures for $name", ({ schema, input }) => {
    for (const value of [
      [],
      [null],
      [["nested"]],
      { unsupported: true },
      [""],
      [" leading"],
      ["trailing "],
      ["\u0000leading control"],
      ["trailing control\u009F"],
      ["\u00A0leading non-breaking space"],
    ]) {
      expect(schema.safeParse(input(value)).success).toBe(false);
    }
  });

  it("publishes the same scalar-or-array value domain without an object alternative", () => {
    const json = z.toJSONSchema(updateTableCellSchema) as Record<string, any>;
    const serializedValueSchema = JSON.stringify(json.properties.value);

    expect(serializedValueSchema).toContain('"type":"array"');
    expect(serializedValueSchema).toContain('"maxItems":1');
    expect(serializedValueSchema).toContain('"minItems":2');
    expect(serializedValueSchema).toContain("\\\\u0000-\\\\u0020");
    expect(serializedValueSchema).toContain("\\\\u007F-\\\\u00A0");
    expect(serializedValueSchema).not.toContain('"type":"object"');
  });

  it("explains the supported cell-value domain in validation errors", () => {
    const objectResult = updateTableCellSchema.safeParse({
      projectId: "p1", tableId: "t1", row: 0, column: 0, value: { unsupported: true },
    });

    expect(objectResult.success).toBe(false);
    if (!objectResult.success) {
      expect(z.prettifyError(objectResult.error)).toMatch(
        /Cell value must be a string, number, boolean, null, or a representable one-dimensional array/,
      );
    }

    const singletonNullResult = updateTableCellSchema.safeParse({
      projectId: "p1", tableId: "t1", row: 0, column: 0, value: [null],
    });
    expect(singletonNullResult.success).toBe(false);
    if (!singletonNullResult.success) {
      expect(z.prettifyError(singletonNullResult.error)).toMatch(/\[null\] is not representable/);
    }
  });

  it("rejects read-only cell styles from every table write contract", () => {
    const styledCell = { value: "Rules void Rate()", style: { background: "#4472C4", bold: true } };

    expect(createProjectTableSchema.safeParse({
      projectId: "p1",
      moduleName: "Main",
      table: { tableType: "RawSource", name: "Rate", source: [[styledCell]] },
    }).success).toBe(false);
    expect(updateTableSchema.safeParse({
      projectId: "p1",
      tableId: "t1",
      view: { tableType: "RawSource", name: "Rate", source: [[styledCell]] },
    }).success).toBe(false);
    expect(appendTableSchema.safeParse({
      projectId: "p1",
      tableId: "t1",
      appendData: { tableType: "RawSource", rows: [[styledCell]] },
    }).success).toBe(false);
  });

  it("requires a nonblank name and complete source when creating", () => {
    expect(createProjectTableSchema.safeParse({
      projectId: "p1", moduleName: "Main", table: { tableType: "RawSource", source: [] },
    }).success).toBe(false);
    expect(createProjectTableSchema.safeParse({
      projectId: "p1", moduleName: "Main", table: { ...rawTable, name: "  " },
    }).success).toBe(false);
    expect(createProjectTableSchema.safeParse({
      projectId: "p1", moduleName: "Main", table: { tableType: "RawSource", name: "Rate" },
    }).success).toBe(false);
  });

  it("requires at least one raw append row", () => {
    expect(appendTableSchema.safeParse({
      projectId: "p1", tableId: "t1", appendData: { tableType: "RawSource", rows: [] },
    }).success).toBe(false);
    expect(appendTableSchema.safeParse({
      projectId: "p1", tableId: "t1", appendData: { tableType: "RawSource", rows: [[{ value: null }]] },
    }).success).toBe(true);
    expect(appendTableSchema.safeParse({
      projectId: "p1", tableId: "t1", appendData: { tableType: "RawSource", rows: [[]] },
    }).success).toBe(false);
    expect(appendTableSchema.safeParse({
      projectId: "p1", tableId: "t1", appendData: { tableType: "RawSource", rows: [[{ value: "x", colspan: 0 }]] },
    }).success).toBe(false);
  });

  it.each(["Datatype", "SimpleRules", "Spreadsheet", "Test"])(
    "rejects the lossy typed %s table contract",
    (tableType) => {
      expect(createProjectTableSchema.safeParse({
        projectId: "p1", moduleName: "Main", table: { ...rawTable, tableType },
      }).success).toBe(false);
      expect(updateTableSchema.safeParse({
        projectId: "p1", tableId: "t1", view: { ...rawTable, tableType },
      }).success).toBe(false);
      expect(appendTableSchema.safeParse({
        projectId: "p1", tableId: "t1", appendData: {
          tableType,
          rows: [[{ value: "x" }]],
        },
      }).success).toBe(false);
    },
  );

  it("accepts only an xlsx modulePath", () => {
    const base = { projectId: "p1", moduleName: "Rules", table: rawTable };
    expect(createProjectTableSchema.safeParse({ ...base, modulePath: "rules/Rules.xlsx" }).success).toBe(true);
    expect(createProjectTableSchema.safeParse({ ...base, modulePath: "rules/Rules.xls" }).success).toBe(false);
  });
});

describe("table workflow schemas", () => {
  it("accepts named and single-array inputs but rejects positional params wrappers", () => {
    expect(runTableSchema.safeParse({ projectId: "p1", tableId: "t1", inputJson: [42] }).success).toBe(true);
    expect(runTableSchema.safeParse({ projectId: "p1", tableId: "t1", inputJson: { params: { age: 25 } } }).success).toBe(true);
    const positional = runTableSchema.safeParse({ projectId: "p1", tableId: "t1", inputJson: { params: [42] } });
    expect(positional.success).toBe(false);
    if (!positional.success) {
      expect(positional.error.issues).toContainEqual(expect.objectContaining({
        path: ["inputJson", "params"],
        message: expect.stringMatching(/object keyed by method parameter name/),
      }));
    }
    expect(runTableSchema.safeParse({ projectId: "p1", tableId: "t1", inputJson: "invalid" }).success).toBe(false);
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
    const valid = { projectId: "p1", tableId: "t1", moduleName: "Main", name: "CopiedRate" };
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

    const jsonSchema = z.toJSONSchema(listProjectBranchesSchema);
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
      projectName: "Rules",
      branch: "main",
    }).success).toBe(false);
  });
});

describe("project creation", () => {
  it("rejects an empty copy source instead of falling back to blank project creation", () => {
    expect(createProjectSchema.safeParse({ repository: "design", projectName: "Copy", template: "" }).success).toBe(false);
  });
});
