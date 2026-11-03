/**
 * Integration tests of openl_repository_project_revision_changes with a path, of src/handlers/file-compare.ts: the changes of one file between two
 * versions, a workbook table by table and any other text line by line.
 */

import { describe, it, expect } from "vitest";
import { executeTool } from "./index.js";
import { bodyOf } from "../testing/mock-http.js";
import { harness, textOf, useToolHarness } from "../testing/harness.js";

useToolHarness();

/** Answers whether a version of a file of project p1 holds it: the working copy without a version. */
const answerMeta = (path: string, version: string | undefined, found = true): void => {
  harness.mockHttp.onGet(`/projects/p1/files/${path}`, { params: expect.objectContaining({ view: "meta", version }) })
    .reply(found ? 200 : 404, found ? { path, type: "file", size: 10 } : { message: "Not found" });
};

/** Answers the text of a version of a file of project p1. */
const answerText = (path: string, version: string | undefined, text: string): void => {
  harness.mockHttp.onGet(`/projects/p1/files/${path}`, { params: expect.objectContaining({ version }) })
    .reply(200, Buffer.from(text), { "content-type": "text/plain; charset=utf-8" });
};

/**
 * Answers the revisions of project p1 compared as the history names them, and the revision both parted at when the
 * base is of another history.
 */
const answerChanges = ({ base, revision, common }: { base: string; revision?: string; common?: string }): void => {
  harness.mockHttp.onGet("/projects/p1/history/changes").reply(200, {
    base: { revisionNo: base },
    ...(revision ? { revision: { revisionNo: revision } } : {}),
    ...(common ? { common: { revisionNo: common } } : {}),
    files: [],
  });
};

/** A table of rates, as one workbook of a comparison holds it at A1. */
const rates = (rows: Array<[string, string | number]>) => ({
  source: [[{ cell: "A1", value: "Rules rate" }], ...rows.map(([country, rate], i) => [
    { cell: `A${i + 2}`, value: country }, { cell: `B${i + 2}`, value: rate },
  ])],
});

describe("openl_repository_project_revision_changes of a file", () => {
  it("tells what a save would commit in a workbook: the tables that differ, row by row and cell by cell", async () => {
    harness.mockHttp.onGet("/projects/p1").reply(200, { id: "p1", name: "P", revision: "r7" });
    answerMeta("rules/Rates.xlsx", "r7");
    answerMeta("rules/Rates.xlsx", undefined);
    harness.mockHttp.onPost("/projects/p1/compare").reply(202, { id: "c1" });
    harness.mockHttp.onGet("/compare/c1").reply(200, { id: "c1", identical: false, sheets: [{
      name: "Rates", status: "changed", children: [
        { id: "0-0", name: "Rules rate", status: "changed" },
        { id: "0-1", name: "Rules pushed", status: "equal" },
        { id: "0-2", name: "Rules moved", status: "moved" },
      ],
    }]});
    harness.mockHttp.onGet("/compare/c1/tables/0-0").reply(200, {
      id: "0-0", name: "Rules rate", status: "changed", firstRange: "A1:B4", secondRange: "A1:B5",
      first: { ...rates([["US", 1], ["FR", 1.2], ["ES", 1.3]]), changedCells: ["B3"] },
      second: { ...rates([["US", 1], ["IT", 1.15], ["FR", 1.25]]), changedCells: ["B4"] },
      rows: [
        { first: 0, second: 0, status: "equal" },
        { first: 1, second: 1, status: "equal" },
        { second: 2, status: "added" },
        { first: 2, second: 3, status: "changed" },
        { first: 3, status: "removed" },
      ],
      columns: [{ first: 0, second: 0, status: "equal" }, { first: 1, second: 1, status: "changed" }],
    });
    harness.mockHttp.onGet("/compare/c1/tables/0-2").reply(200, {
      id: "0-2", name: "Rules moved", status: "moved", moved: true, formattingChanged: true,
      firstRange: "A16:B17", secondRange: "E1:F2", rows: [], columns: [],
    });
    harness.mockHttp.onDelete("/compare/c1").reply(204);

    const text = textOf(await executeTool("repository_project_revision_changes", { projectId: "p1", path: "rules/Rates.xlsx" }, harness.client));

    // The saved revision stands against the working copy, as the Save dialog compares them.
    expect(bodyOf(harness.mockHttp.history.post[0])).toEqual({
      first: { path: "rules/Rates.xlsx", revision: "r7" },
      second: { path: "rules/Rates.xlsx" },
    });
    expect(text).toBe([
      "rules/Rates.xlsx: revision r7 → the working copy",
      "2 tables differ; 1–2 below.",
      "### Rates / Rules rate: changed\n- row 3 added: IT | 1.15\n- B4 (was B3, row FR): 1.2 → 1.25\n- old row 4 removed: ES | 1.3",
      "### Rates / Rules moved: moved\nMoved from A16:B17 to E1:F2.\nFormatting changed.",
    ].join("\n\n"));
    // The comparison is let go once read.
    expect(harness.mockHttp.history.delete).toHaveLength(1);
  });

  it("tells a cell whose merging or note changed alone by what changed, and a cell the merge covered with it", async () => {
    answerChanges({ base: "r1" });
    answerMeta("rules/Rates.xlsx", "r1");
    answerMeta("rules/Rates.xlsx", undefined);
    harness.mockHttp.onPost("/projects/p1/compare").reply(202, { id: "c1" });
    harness.mockHttp.onGet("/compare/c1").reply(200, { id: "c1", identical: false, sheets: [{
      name: "Rates", status: "changed", children: [{ id: "0-0", name: "Rules rate", status: "changed" }],
    }]});
    harness.mockHttp.onGet("/compare/c1/tables/0-0").reply(200, {
      id: "0-0", name: "Rules rate", status: "changed", firstRange: "A1:B2", secondRange: "A1:B2",
      first: {
        source: [[{ cell: "A1", value: "Rules rate", colspan: 2 }, { covered: true }],
          [{ cell: "A2", value: "US" }, { cell: "B2", value: 1 }]],
        changedCells: ["A1", "B2"],
      },
      second: {
        source: [[{ cell: "A1", value: "Rules rate" }, { cell: "B1" }],
          [{ cell: "A2", value: "US" }, { cell: "B2", value: 1, comment: "Checked" }]],
        changedCells: ["A1", "B1", "B2"],
      },
      rows: [{ first: 0, second: 0, status: "changed" }, { first: 1, second: 1, status: "changed" }],
      columns: [{ first: 0, second: 0, status: "equal" }, { first: 1, second: 1, status: "changed" }],
    });
    harness.mockHttp.onDelete("/compare/c1").reply(204);

    const text = textOf(await executeTool("repository_project_revision_changes", {
      projectId: "p1", path: "rules/Rates.xlsx", base: "r1",
    }, harness.client));

    // B1 is the part of the header the merge covered: the header tells it
    expect(text).toMatch(/### Rates \/ Rules rate: changed\n- A1: merged A1:B1 → not merged\n- B2 \(row US\): note '' → 'Checked'$/);
  });

  it("tells a kind changed, a cell emptied and a value holding a separator, and a rule by its properties", async () => {
    answerChanges({ base: "r1" });
    answerMeta("rules/Rates.xlsx", "r1");
    answerMeta("rules/Rates.xlsx", undefined);
    harness.mockHttp.onPost("/projects/p1/compare").reply(202, { id: "c1" });
    harness.mockHttp.onGet("/compare/c1").reply(200, { id: "c1", identical: false, sheets: [{
      name: "Rates", status: "changed", children: [
        { id: "0-0", name: "Rules rate", status: "changed" },
        { id: "0-1", name: "Rules rate", status: "equal" },
      ],
    }]});
    // A version of the rule for one state: the properties row stands under the header
    const version = (state: string, rate: unknown, note: unknown, fee: unknown) => ({
      source: [[{ cell: "A1", value: "Rules rate" }],
        [{ cell: "A2", value: "properties" }, { cell: "B2", value: "state" }, { cell: "C2", value: state }],
        [{ cell: "A3", value: "US" }, { cell: "B3", value: rate }, { cell: "C3", value: note }],
        [{ cell: "A4", value: "DE" }, { cell: "B4", value: fee }, { cell: "C4", value: "x" }]],
    });
    harness.mockHttp.onGet("/compare/c1/tables/0-0").reply(200, {
      id: "0-0", name: "Rules rate", status: "changed", firstRange: "A1:C4", secondRange: "A1:C4",
      first: { ...version("CA", "1", "a", 5), changedCells: ["C2", "B3", "C3", "B4"] },
      second: { ...version("NV", 1, "a | b\nc", ""), changedCells: ["C2", "B3", "C3", "B4"] },
      rows: [0, 1, 2, 3].map((row) => ({ first: row, second: row, status: row < 1 ? "equal" : "changed" })),
      columns: [0, 1, 2].map((column) => ({ first: column, second: column, status: "changed" })),
    });
    harness.mockHttp.onDelete("/compare/c1").reply(204);

    const text = textOf(await executeTool("repository_project_revision_changes", {
      projectId: "p1", path: "rules/Rates.xlsx", base: "r1",
    }, harness.client));

    expect(text).toMatch(/### Rates \/ Rules rate \[state=NV\]: changed\n- C2 \(row property state\): CA → NV\n/);
    expect(text).toMatch(/\n- B3 \(row US\): 1 \(text\) → 1 \(number\)\n/);
    expect(text).toMatch(/\n- C3 \(row US \/ 1\): a → "a \| b\\nc"\n/);
    expect(text).toMatch(/\n- B4 \(row DE\): 5 → \(empty\)$/);
  });

  it("lists every change of a table alone on a page, and counts them past 30 among other tables", async () => {
    answerChanges({ base: "r1" });
    answerMeta("rules/Rates.xlsx", "r1");
    answerMeta("rules/Rates.xlsx", undefined);
    harness.mockHttp.onPost("/projects/p1/compare").reply(202, { id: "c1" });
    harness.mockHttp.onGet("/compare/c1").reply(200, { id: "c1", identical: false, sheets: [{
      name: "Rates", status: "changed", children: [
        { id: "0-0", name: "Rules rate", status: "changed" },
        { id: "0-1", name: "Rules age", status: "moved", moved: true },
      ],
    }]});
    const countries = Array.from({ length: 40 }, (_, i): [string, number] => [`C${i}`, i]);
    harness.mockHttp.onGet("/compare/c1/tables/0-0").reply(200, {
      id: "0-0", name: "Rules rate", status: "changed", firstRange: "A1:B41", secondRange: "A1:B41",
      first: { ...rates(countries), changedCells: countries.map((_, i) => `B${i + 2}`) },
      second: { ...rates(countries.map(([country, rate]) => [country, rate + 1])), changedCells: countries.map((_, i) => `B${i + 2}`) },
      rows: [0, ...countries.keys().map((i) => i + 1)].map((row) => ({ first: row, second: row, status: row === 0 ? "equal" : "changed" })),
      columns: [{ first: 0, second: 0, status: "equal" }, { first: 1, second: 1, status: "changed" }],
    });
    harness.mockHttp.onGet("/compare/c1/tables/0-1").reply(200, {
      id: "0-1", name: "Rules age", status: "moved", moved: true, firstRange: "A50:B51", secondRange: "D1:E2", rows: [], columns: [],
    });
    harness.mockHttp.onDelete("/compare/c1").reply(204);

    const both = textOf(await executeTool("repository_project_revision_changes", {
      projectId: "p1", path: "rules/Rates.xlsx", base: "r1",
    }, harness.client));
    const alone = textOf(await executeTool("repository_project_revision_changes", {
      projectId: "p1", path: "rules/Rates.xlsx", base: "r1", limit: 1,
    }, harness.client));

    expect(both).toMatch(/\n- … 10 more changes: offset=0 with limit=1 lists them all\.\n/);
    expect(alone).toMatch(/\n- B41 \(row C39\): 39 → 40$/);
  });

  it("pages the tables of a workbook and tells a table one version holds row by row", async () => {
    answerChanges({ base: "r1", revision: "r2" });
    answerMeta("rules/Rates.xlsx", "r1", false);
    answerMeta("rules/Rates.xlsx", "r2");
    harness.mockHttp.onPost("/projects/p1/compare").reply(202, { id: "c1" });
    harness.mockHttp.onGet("/compare/c1").reply(200, { id: "c1", identical: false, sheets: [{
      name: "Rates", status: "added", children: [
        { id: "0-0", name: "Rules rate", status: "added" },
        { id: "0-1", name: "Rules age", status: "added" },
      ],
    }]});
    harness.mockHttp.onGet("/compare/c1/tables/0-0").reply(200, {
      id: "0-0", name: "Rules rate", status: "added", secondRange: "A3:B4", second: rates([["US", 1]]),
    });
    harness.mockHttp.onDelete("/compare/c1").reply(204);

    const text = textOf(await executeTool("repository_project_revision_changes", {
      projectId: "p1", path: "rules/Rates.xlsx", base: "r1", revision: "r2", branch: "main", limit: 1,
    }, harness.client));

    // The base does not hold the file, so it is compared as an empty workbook.
    expect(bodyOf(harness.mockHttp.history.post[0])).toEqual({
      first: { path: "rules/Rates.xlsx", absent: true },
      second: { path: "rules/Rates.xlsx", revision: "r2", branch: "main" },
    });
    expect(text).toBe("rules/Rates.xlsx: revision r1 (no file) → revision r2\n\n"
      + "2 tables differ; 1–1 below, the next page at offset=1.\n\n"
      + "### Rates / Rules rate: added at A3:B4\n- row 3: Rules rate\n- row 4: US | 1");
  });

  it("tells what a revision changed in a text file against the revision it was made on, as a unified diff", async () => {
    harness.mockHttp.onGet("/projects/p1/history/changes", { params: { revision: "r2" } })
      .reply(200, { base: { revisionNo: "r1" }, revision: { revisionNo: "r2" }, files: [] });
    answerMeta("notes.txt", "r1");
    answerMeta("notes.txt", "r2");
    answerText("notes.txt", "r1", "one\ntwo\nthree\n");
    answerText("notes.txt", "r2", "one\n2\nthree\n");

    const text = textOf(await executeTool("repository_project_revision_changes", { projectId: "p1", path: "notes.txt", revision: "r2" }, harness.client));

    expect(text).toBe("notes.txt: revision r1 → revision r2\n\n```diff\n@@ -1,3 +1,3 @@\n one\n-two\n+2\n three\n```");
  });

  it("says when both versions of a text read the same, and when a file is not text", async () => {
    answerChanges({ base: "r1" });
    answerMeta("notes.txt", "r1");
    answerMeta("notes.txt", undefined);
    answerText("notes.txt", "r1", "same\n");
    answerText("notes.txt", undefined, "same\n");
    answerMeta("lib.jar", "r1");
    answerMeta("lib.jar", undefined);
    harness.mockHttp.onGet("/projects/p1/files/lib.jar").reply(200, Buffer.from([0x50, 0x4b]), { "content-type": "application/java-archive" });

    const same = textOf(await executeTool("repository_project_revision_changes", { projectId: "p1", path: "notes.txt", base: "r1" }, harness.client));
    const binary = textOf(await executeTool("repository_project_revision_changes", { projectId: "p1", path: "lib.jar", base: "r1" }, harness.client));

    expect(same).toBe("notes.txt: revision r1 → the working copy\n\nBoth versions read the same.");
    expect(binary).toMatch(/not text/);
  });

  it("reads a revision named by a branch as the history names it, so its file is found", async () => {
    harness.mockHttp.onGet("/projects/p1/history/changes", { params: { revision: "feature-vet-fee", base: "r1" } })
      .reply(200, { base: { revisionNo: "r1" }, revision: { revisionNo: "f9e8d7" }, files: [] });
    answerMeta("rules.xml", "r1");
    answerMeta("rules.xml", "f9e8d7");
    answerText("rules.xml", "r1", "<project/>\n");
    answerText("rules.xml", "f9e8d7", "<project name=\"P\"/>\n");

    const text = textOf(await executeTool("repository_project_revision_changes", {
      projectId: "p1", path: "rules.xml", revision: "feature-vet-fee", base: "r1",
    }, harness.client));

    expect(text).toBe("rules.xml: revision r1 → revision f9e8d7\n\n```diff\n@@ -1 +1 @@\n-<project/>\n+<project name=\"P\"/>\n```");
  });

  it("refuses a revision the history does not know instead of comparing a file it does not hold", async () => {
    harness.mockHttp.onGet("/projects/p1/history/changes").reply(404, {
      code: "openl.error.404.project.revision.message", message: "The project revision 'HEAD~1' is not found.",
    });

    // The refusal tells how a revision is named
    await expect(executeTool("repository_project_revision_changes", {
      projectId: "p1", path: "rules/Main.xlsx", base: "HEAD~1",
    }, harness.client)).rejects.toThrow(/404.*is not found\.\nName a revision by the revisionNo/s);
    // Nothing is compared
    expect(harness.mockHttp.history.post).toHaveLength(0);
  });

  it("names a field of a datatype by its name and a record of a data table by its first value", async () => {
    answerChanges({ base: "r1" });
    answerMeta("rules/Pets.xlsx", "r1");
    answerMeta("rules/Pets.xlsx", undefined);
    harness.mockHttp.onPost("/projects/p1/compare").reply(202, { id: "c1" });
    harness.mockHttp.onGet("/compare/c1").reply(200, { id: "c1", identical: false, sheets: [{
      name: "Pets", status: "changed", children: [
        { id: "0-0", name: "Datatype Pet", status: "changed" },
        { id: "0-1", name: "Data Pet pets", status: "changed" },
      ],
    }]});
    const pet = (type: string, name: string) => ({
      source: [[{ cell: "A1", value: "Datatype Pet" }],
        [{ cell: "A2", value: "String" }, { cell: "B2", value: "name" }],
        [{ cell: "A3", value: type }, { cell: "B3", value: name }]],
    });
    harness.mockHttp.onGet("/compare/c1/tables/0-0").reply(200, {
      id: "0-0", name: "Datatype Pet", status: "changed", firstRange: "A1:B3", secondRange: "A1:B3",
      first: { ...pet("Double", "age"), changedCells: ["A3", "B3"] },
      second: { ...pet("Integer", "ageYears"), changedCells: ["A3", "B3"] },
      rows: [0, 1, 2].map((row) => ({ first: row, second: row, status: row < 2 ? "equal" : "changed" })),
      columns: [0, 1].map((column) => ({ first: column, second: column, status: "changed" })),
    });
    const pets = (second: string) => ({
      source: [[{ cell: "D1", value: "Data Pet pets" }],
        [{ cell: "D2", value: "id" }, { cell: "E2", value: "name" }, { cell: "F2", value: "kind" }],
        [{ cell: "D3", value: "P2" }, { cell: "E3", value: second }, { cell: "F3", value: "dog" }]],
    });
    harness.mockHttp.onGet("/compare/c1/tables/0-1").reply(200, {
      id: "0-1", name: "Data Pet pets", status: "changed", firstRange: "D1:F3", secondRange: "D1:F3",
      first: { ...pets("Rex"), changedCells: ["E3"] },
      second: { ...pets("Max"), changedCells: ["E3"] },
      rows: [0, 1, 2].map((row) => ({ first: row, second: row, status: row < 2 ? "equal" : "changed" })),
      columns: [0, 1, 2].map((column) => ({ first: column, second: column, status: column === 1 ? "changed" : "equal" })),
    });
    harness.mockHttp.onDelete("/compare/c1").reply(204);

    const text = textOf(await executeTool("repository_project_revision_changes", {
      projectId: "p1", path: "rules/Pets.xlsx", base: "r1",
    }, harness.client));

    // The name of the field names it, a changed name names itself; a record is its id, not a field beside it
    expect(text).toMatch(/\n- A3 \(row ageYears\): Double → Integer\n- B3: age → ageYears\n/);
    expect(text).toMatch(/\n- E3 \(row P2\): Rex → Max$/);
  });

  it("names a cell of a spreadsheet, a lookup and constants as each kind of table names it, and tells formula results", async () => {
    answerChanges({ base: "r1" });
    answerMeta("rules/Calc.xlsx", "r1");
    answerMeta("rules/Calc.xlsx", undefined);
    harness.mockHttp.onPost("/projects/p1/compare").reply(202, { id: "c1" });
    harness.mockHttp.onGet("/compare/c1").reply(200, { id: "c1", identical: false, sheets: [{
      name: "Calc", status: "changed", children: [
        { id: "0-0", name: "Spreadsheet SpreadsheetResult Calc()", status: "changed" },
        { id: "0-1", name: "SimpleLookup Double Rate(Integer age, String species)", status: "changed" },
        { id: "0-2", name: "Constants", status: "changed" },
      ],
    }]});
    // A table of three rows whose last row changed, each cell of it given as the version holds it
    const cellsOf = (cells: object[]) => cells.map((cell, i) => ({ cell: `${"ABC"[i]}3`, ...cell }));
    const table = (name: string, titles: string[], changed: string[], before: object[], after: object[]) => {
      const head = [[{ cell: "A1", value: name }], titles.map((value, i) => ({ cell: `${"ABC"[i]}2`, value }))];
      return {
        id: "x", name, status: "changed", firstRange: "A1:C3", secondRange: "A1:C3",
        first: { source: [...head, cellsOf(before)], changedCells: changed },
        second: { source: [...head, cellsOf(after)], changedCells: changed },
        rows: [0, 1, 2].map((r) => ({ first: r, second: r, status: r === 2 ? "changed" : "equal" })),
        columns: [0, 1, 2].map((c) => ({ first: c, second: c, status: c === 0 ? "equal" : "changed" })),
      };
    };
    harness.mockHttp.onGet("/compare/c1/tables/0-0").reply(200, table("Spreadsheet SpreadsheetResult Calc()",
      ["Step", "Low", "High"], ["B3", "C3"],
      [{ value: "Factor" }, { value: 4, formula: "=C3*2" }, { value: 2 }],
      [{ value: "Factor" }, { value: 6, formula: "=C3*2" }, { value: 3 }]));
    harness.mockHttp.onGet("/compare/c1/tables/0-1").reply(200, table("SimpleLookup Double Rate(Integer age, String species)",
      ["Age", "Dog", "Cat"], ["C3"],
      [{ value: "6-10" }, { value: 1.2 }, { value: 1.3 }],
      [{ value: "6-10" }, { value: 1.2 }, { value: 1.4 }]));
    harness.mockHttp.onGet("/compare/c1/tables/0-2").reply(200, table("Constants",
      ["Double", "base", "1"], ["C3"],
      [{ value: "Double" }, { value: "rate" }, { value: 1.2 }],
      [{ value: "Double" }, { value: "rate" }, { value: 1.25 }]));
    harness.mockHttp.onDelete("/compare/c1").reply(204);

    const text = textOf(await executeTool("repository_project_revision_changes", {
      projectId: "p1", path: "rules/Calc.xlsx", base: "r1",
    }, harness.client));

    // The formula is the same: its result is not, as the cell it reads changed
    expect(text).toMatch(/\n- B3 \(\$Low\$Factor\): =C3\*2 \(4\) → =C3\*2 \(6\)\n- C3 \(\$High\$Factor\): 2 → 3\n/);
    expect(text).toMatch(/\n- C3 \(row 6-10, column Cat\): 1\.3 → 1\.4\n/);
    expect(text).toMatch(/\n- C3 \(row rate\): 1\.2 → 1\.25$/);
  });

  it("names a value of a lookup by both levels of its titles, a title by nothing, and a kind changed with the text", async () => {
    answerChanges({ base: "r1" });
    answerMeta("rules/Rates.xlsx", "r1");
    answerMeta("rules/Rates.xlsx", undefined);
    harness.mockHttp.onPost("/projects/p1/compare").reply(202, { id: "c1" });
    const name = "SmartLookup Double Rate(Integer age, String plan, String species)";
    harness.mockHttp.onGet("/compare/c1").reply(200, { id: "c1", identical: false, sheets: [{
      name: "Rates", status: "changed", children: [{ id: "0-0", name, status: "changed" }],
    }]});
    // The title of the condition spans both rows of the titles, and each plan spans its two species
    const version = (title: string, gold: unknown, active: unknown) => ({
      source: [
        [{ cell: "A1", value: name, colspan: 5 }, { covered: true }, { covered: true }, { covered: true }, { covered: true }],
        [{ cell: "A2", value: "Age", rowspan: 2 }, { cell: "B2", value: "Basic", colspan: 2 }, { covered: true },
          { cell: "D2", value: "Gold", colspan: 2 }, { covered: true }],
        [{ covered: true }, { cell: "B3", value: "Dog" }, { cell: "C3", value: title }, { cell: "D3", value: "Dog" },
          { cell: "E3", value: "Cat" }],
        [{ cell: "A4", value: "0-5" }, { cell: "B4", value: 1 }, { cell: "C4", value: 2 }, { cell: "D4", value: 3 },
          { cell: "E4", value: gold }],
        [{ cell: "A5", value: "6-10" }, { cell: "B5", value: active }, { cell: "C5", value: 2 }, { cell: "D5", value: 3 },
          { cell: "E5", value: 4 }],
      ],
    });
    harness.mockHttp.onGet("/compare/c1/tables/0-0").reply(200, {
      id: "0-0", name, status: "changed", firstRange: "A1:E5", secondRange: "A1:E5",
      first: { ...version("Cat", 4, "TRUE"), changedCells: ["C3", "E4", "B5"] },
      second: { ...version("Hamster", 5, true), changedCells: ["C3", "E4", "B5"] },
      rows: [0, 1, 2, 3, 4].map((row) => ({ first: row, second: row, status: row < 2 ? "equal" : "changed" })),
      columns: [0, 1, 2, 3, 4].map((column) => ({ first: column, second: column, status: "changed" })),
    });
    harness.mockHttp.onDelete("/compare/c1").reply(204);

    const text = textOf(await executeTool("repository_project_revision_changes", {
      projectId: "p1", path: "rules/Rates.xlsx", base: "r1",
    }, harness.client));

    expect(text).toMatch(/\n- C3: Cat → Hamster\n/);
    expect(text).toMatch(/\n- E4 \(row 0-5, column Gold \/ Cat\): 4 → 5\n/);
    expect(text).toMatch(/\n- B5 \(row 6-10, column Basic \/ Dog\): TRUE \(text\) → true \(boolean\)$/);
  });

  it("says when only the line ends of a text changed, which a commit still takes", async () => {
    answerChanges({ base: "r1" });
    answerMeta("notes.txt", "r1");
    answerMeta("notes.txt", undefined);
    answerText("notes.txt", "r1", "one\ntwo");
    answerText("notes.txt", undefined, "one\ntwo\n");

    const text = textOf(await executeTool("repository_project_revision_changes", { projectId: "p1", path: "notes.txt", base: "r1" }, harness.client));

    expect(text).toBe("notes.txt: revision r1 → the working copy\n\n"
      + "Only the line ends changed, such as the line break at the end of the file.");
  });

  it("names an empty cell of a row added as empty", async () => {
    answerChanges({ base: "r1" });
    answerMeta("rules/Rates.xlsx", "r1");
    answerMeta("rules/Rates.xlsx", undefined);
    harness.mockHttp.onPost("/projects/p1/compare").reply(202, { id: "c1" });
    harness.mockHttp.onGet("/compare/c1").reply(200, { id: "c1", identical: false, sheets: [{
      name: "Rates", status: "changed", children: [{ id: "0-0", name: "Rules rate", status: "changed" }],
    }]});
    harness.mockHttp.onGet("/compare/c1/tables/0-0").reply(200, {
      id: "0-0", name: "Rules rate", status: "changed", firstRange: "A1:B2", secondRange: "A1:B3",
      first: rates([["US", 1]]),
      second: { source: [...rates([["US", 1]]).source, [{ cell: "A3", value: "L10" }, { cell: "B3" }]] },
      rows: [{ first: 0, second: 0, status: "equal" }, { first: 1, second: 1, status: "equal" }, { second: 2, status: "added" }],
      columns: [{ first: 0, second: 0, status: "equal" }, { first: 1, second: 1, status: "equal" }],
    });
    harness.mockHttp.onDelete("/compare/c1").reply(204);

    const text = textOf(await executeTool("repository_project_revision_changes", {
      projectId: "p1", path: "rules/Rates.xlsx", base: "r1",
    }, harness.client));

    expect(text).toMatch(/\n- row 3 added: L10 \| \(empty\)$/);
  });

  it("refuses a file neither version holds", async () => {
    answerChanges({ base: "r1" });
    answerMeta("gone.txt", "r1", false);
    answerMeta("gone.txt", undefined, false);

    await expect(executeTool("repository_project_revision_changes", { projectId: "p1", path: "gone.txt", base: "r1" }, harness.client))
      .rejects.toThrow("Neither revision r1 nor the working copy holds 'gone.txt'.");
  });

  it("compares a base of another branch from the revision both were made from, as a pull request does", async () => {
    answerChanges({ base: "t1", common: "c1" });
    answerMeta("notes.txt", "c1");
    answerMeta("notes.txt", undefined);
    answerText("notes.txt", "c1", "one\ntwo\n");
    answerText("notes.txt", undefined, "one\nthree\n");

    const text = textOf(await executeTool("repository_project_revision_changes", {
      projectId: "p1", path: "notes.txt", base: "t1",
    }, harness.client));

    // What the working copy changed since it and t1 parted, not the difference from t1 itself
    expect(text).toContain("notes.txt: revision c1 (where it and revision t1 parted) → the working copy");
    expect(text).toContain("-two\n+three");
  });
});
