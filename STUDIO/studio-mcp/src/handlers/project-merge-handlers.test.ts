/**
 * Integration tests of the branch, merge and deletion tools of src/handlers/project-merge-handlers.ts.
 */

import { describe, expect, it } from "vitest";
import { executeTool } from "./index.js";
import { bodyOf } from "../testing/mock-http.js";
import { dataOf, harness, textOf, useToolHarness } from "../testing/harness.js";

useToolHarness();

describe("project branch and merge handlers", () => {
  it("lists project branches by default and repository-wide merge targets on request", async () => {
    const projectBranches = [
      { name: "feature/rates" },
    ];
    const repositoryBranches = [
      { name: "main", base: true, protected: true },
      ...projectBranches,
    ];
    const scopes: unknown[] = [];
    harness.mockHttp.onGet("/projects/p1/branches").reply((config) => {
      const scope = config.params?.scope;
      scopes.push(scope);
      return [200, scope === "repository" ? repositoryBranches : projectBranches];
    });

    const projectResponse = await executeTool("list_project_branches", {
      projectId: "p1", }, harness.client);
    const repositoryResponse = await executeTool("list_project_branches", {
      projectId: "p1", scope: "repository", }, harness.client);

    expect(scopes).toEqual(["project", "repository"]);
    expect(dataOf(projectResponse)).toEqual(projectBranches);
    expect(dataOf(repositoryResponse)).toEqual(repositoryBranches);
  });

  it("checks merge direction with the current backend request shape", async () => {
    harness.mockHttp.onPost("/projects/p1/merge/check").reply(200, {
      sourceBranch: "feature", targetBranch: "main", status: "mergeable", canMerge: true,
    });

    const response = await executeTool("check_project_merge", {
      projectId: "p1", otherBranch: "feature", mode: "receive", }, harness.client);

    expect(bodyOf(harness.mockHttp.history.post[0])).toEqual({ mode: "receive", otherBranch: "feature" });
    expect(dataOf<Record<string, unknown>>(response)).toMatchObject({
      sourceBranch: "feature", targetBranch: "main", status: "mergeable",
    });
  });

  it("returns up-to-date without issuing the mutating merge request", async () => {
    harness.mockHttp.onPost("/projects/p1/merge/check").reply(200, {
      sourceBranch: "feature", targetBranch: "main", status: "up-to-date", canMerge: true,
    });

    const response = await executeTool("merge_project_branches", {
      projectId: "p1", otherBranch: "feature", mode: "receive", }, harness.client);

    expect(harness.mockHttp.history.post.map((request) => request.url)).toEqual(["/projects/p1/merge/check"]);
    expect(dataOf<Record<string, unknown>>(response)).toMatchObject({
      success: true, status: "up-to-date",
    });
  });

  it("returns the conflict recovery path without the superseded relationship check", async () => {
    harness.mockHttp.onPost("/projects/p1/merge/check").reply(200, {
      sourceBranch: "feature", targetBranch: "main", status: "mergeable", canMerge: true,
    });
    harness.mockHttp.onPost("/projects/p1/merge").reply(202);
    // Studio runs the merge in the background: the result is read until it is ready.
    harness.mockHttp.onGet("/projects/p1/merge").replyOnce(202, { status: "notReady" });
    harness.mockHttp.onGet("/projects/p1/merge").replyOnce(200, {
      status: "conflicts",
      conflictGroups: [{ projectName: "Rating", files: ["rules/Main.xlsx"] }],
    });

    const response = await executeTool("merge_project_branches", {
      projectId: "p1", otherBranch: "feature", mode: "receive", }, harness.client);

    expect(bodyOf(harness.mockHttp.history.post[1])).toEqual({ mode: "receive", otherBranch: "feature" });
    expect(harness.mockHttp.history.post[1].params).toEqual({ force: false });
    expect(harness.mockHttp.history.get.filter((request) => request.url === "/projects/p1/merge")).toHaveLength(2);
    const result = dataOf<Record<string, unknown>>(response);
    expect(result).toMatchObject({
      success: false,
      status: "conflicts",
      nextAction: expect.stringContaining("openl_get_merge_conflicts"),
    });
    expect(result).not.toHaveProperty("check");
    expect(result.nextAction).toEqual(expect.stringContaining("does not predict conflicts"));
    expect(result.nextAction).toEqual(expect.stringContaining(
      "resolve them with openl_resolve_merge_conflicts only as the user decides"));
  });

  it("tolerates a null merge blocker as equivalent to an omitted blocker", async () => {
    harness.mockHttp.onPost("/projects/p1/merge/check").reply(200, {
      sourceBranch: "feature",
      targetBranch: "main",
      status: "mergeable",
      canMerge: true,
      blockedBy: null,
    });
    harness.mockHttp.onPost("/projects/p1/merge").reply(202);
    harness.mockHttp.onGet("/projects/p1/merge").reply(200, { status: "success" });

    const response = await executeTool("merge_project_branches", {
      projectId: "p1",
      otherBranch: "feature",
      mode: "receive",
    }, harness.client);

    expect(dataOf<Record<string, unknown>>(response)).toMatchObject({
      success: true,
      status: "success",
    });
  });

  it("refuses a merge blocker before issuing the mutating request", async () => {
    harness.mockHttp.onPost("/projects/p1/merge/check").reply(200, {
      sourceBranch: "feature", targetBranch: "main", status: "mergeable",
      canMerge: false, blockedBy: "locked",
    });

    await expect(executeTool("merge_project_branches", {
      projectId: "p1", otherBranch: "feature", mode: "receive",
    }, harness.client)).rejects.toThrow(/locked/);
    expect(harness.mockHttp.history.post.map((request) => request.url)).toEqual(["/projects/p1/merge/check"]);
  });

  it("sends force only for the bypass-required blocker", async () => {
    harness.mockHttp.onPost("/projects/p1/merge/check").reply(200, {
      sourceBranch: "feature", targetBranch: "main", status: "mergeable", canMerge: true,
    });

    await expect(executeTool("merge_project_branches", {
      projectId: "p1", otherBranch: "feature", mode: "receive", force: true, confirmForce: true,
    }, harness.client)).rejects.toThrow(/bypass-required/);
    expect(harness.mockHttp.history.post).toHaveLength(1);

    harness.mockHttp.reset();
    harness.mockHttp.onPost("/projects/p1/merge/check").reply(200, {
      sourceBranch: "feature", targetBranch: "main", status: "mergeable",
      canMerge: false, blockedBy: "bypass-required",
    });
    harness.mockHttp.onPost("/projects/p1/merge").reply(202);
    harness.mockHttp.onGet("/projects/p1/merge").reply(200, { status: "success" });

    await executeTool("merge_project_branches", {
      projectId: "p1", otherBranch: "feature", mode: "receive", force: true, confirmForce: true,
    }, harness.client);
    expect(harness.mockHttp.history.post[1].params).toEqual({ force: true });
  });

  describe("a version of a conflicted file", () => {
    // A conflicted file is named by its path in the repository: the folder of the project comes first
    const availability = (text: boolean, base = true) => ({
      oursRevision: { branch: "main", commit: "abc", exists: true },
      theirsRevision: { branch: "feature", commit: "def", exists: true },
      conflictGroups: [{ projectName: "Rating", files: ["Rating/rules/Main.xlsx", "Rating/rules/config.txt"] }],
      fileAvailability: {
        "Rating/rules/Main.xlsx": { ours: true, base, theirs: true, text },
        "Rating/rules/config.txt": { ours: true, base, theirs: true, text },
      },
    });
    const project = { id: "p1", name: "Rating", repository: "design", path: "Rating", branch: "main" };

    it("is a transfer link for a binary version where Studio authenticates, read by no tool call", async () => {
      harness.mockHttp.onGet("/projects/p1/merge/conflicts").reply(200, availability(false));
      harness.mockHttp.onGet("/settings").reply(200, { userMode: "INTERNAL" });
      harness.mockHttp.onPost("/transfer-links").reply(201, { path: "transfers/abc", expiresAt: "2026-10-23T10:15:00Z" });
      harness.mockHttp.onGet("/projects/p1").reply(200, project);

      const response = await executeTool("read_merge_conflict_file", {
        projectId: "p1", file: "Rating/rules/Main.xlsx", side: "OURS",
      }, harness.client);

      expect(bodyOf(harness.mockHttp.history.post[0])).toEqual({
        kind: "conflictVersion", projectId: "p1", path: "Rating/rules/Main.xlsx", side: "ours", branch: "main",
      });
      expect(dataOf(response)).toMatchObject({
        url: "http://localhost:8080/transfers/abc",
        method: "GET",
        expiresAt: "2026-10-23T10:15:00Z",
        repositoryId: "design",
        repositoryPath: "Rating/rules/Main.xlsx",
        branch: "main",
        curl: "curl -fsS -o 'Main.xlsx' 'http://localhost:8080/transfers/abc'",
      });
      expect(harness.mockHttp.history.get.map((request) => request.url)).not.toContain(
        "/projects/p1/merge/conflicts/files");
    });

    it("is returned inline when it is a text of up to 5,000 bytes", async () => {
      harness.mockHttp.onGet("/projects/p1/merge/conflicts").reply(200, availability(true));
      harness.mockHttp.onGet("/projects/p1/merge/conflicts/files").reply(200, "rate = 1.5\n", {
        "content-type": "text/plain; charset=utf-8",
      });

      const response = await executeTool("read_merge_conflict_file", {
        projectId: "p1", file: "Rating/rules/config.txt", side: "THEIRS",
      }, harness.client);

      expect(textOf(response)).toBe("rate = 1.5\n");
      expect(harness.mockHttp.history.get[1].params).toEqual({ file: "Rating/rules/config.txt", side: "THEIRS" });
    });

    it("is the address of the REST API for a larger text in single-user mode", async () => {
      harness.mockHttp.onGet("/projects/p1/merge/conflicts").reply(200, availability(true));
      harness.mockHttp.onGet("/projects/p1/merge/conflicts/files").reply(200, "x".repeat(5001), {
        "content-type": "text/plain",
      });
      harness.mockHttp.onGet("/settings").reply(200, {});
      harness.mockHttp.onGet("/projects/p1").reply(200, project);

      const response = await executeTool("read_merge_conflict_file", {
        projectId: "p1", file: "Rating/rules/config.txt", side: "THEIRS",
      }, harness.client);

      expect(dataOf(response)).toMatchObject({
        url: "http://localhost:8080/rest/projects/p1/merge/conflicts/files?file=Rating%2Frules%2Fconfig.txt&side=theirs",
        method: "GET",
        size: 5001,
        repositoryPath: "Rating/rules/config.txt",
        branch: "feature",
      });
      expect(dataOf(response)).not.toHaveProperty("expiresAt");
      expect(dataOf(response)).not.toHaveProperty("mimeType");
      expect(harness.mockHttp.history.post).toHaveLength(0);
    });

    it("is refused when fileAvailability marks that it holds no file", async () => {
      harness.mockHttp.onGet("/projects/p1/merge/conflicts").reply(200, availability(true, false));

      await expect(executeTool("read_merge_conflict_file", {
        projectId: "p1", file: "Rating/rules/config.txt", side: "BASE",
      }, harness.client)).rejects.toThrow(/does not hold the file/);
    });
  });

  describe("a conflicted workbook", () => {
    const cell = (address: string, value: unknown, more: Record<string, unknown> = {}) => ({
      cell: address, value, style: 0, ...more,
    });
    const version = (rate: number) => ({
      range: "A1:B2",
      cells: [[cell("A1", "Rules rate", { colspan: 2 }), { covered: true }], [cell("A2", "US"), cell("B2", rate)]],
      styles: [{ bold: true }],
      rows: 2,
      changedCells: ["B2"],
      formattingChanged: false,
      conflictCells: ["B2"],
    });
    const workbook = {
      sheets: [
        { name: "Notes", status: "theirs", ours: true, base: true, theirs: true, tables: [] },
        {
          name: "Rules",
          status: "conflict",
          ours: true,
          base: true,
          theirs: true,
          tables: [
            {
              id: "Rules/A1:B2",
              name: "Rules rate",
              reason: "cells",
              ours: version(10),
              base: version(1),
              theirs: version(20),
              merged: {
                range: "A1:B2",
                cells: [[cell("A1", "Rules rate", { colspan: 2 }), { covered: true }], [cell("A2", "USA"), cell("B2", 1)]],
                styles: [{ bold: true }],
                rows: 2,
                changedCells: ["A2"],
                conflicts: [{
                  row: 1, column: 1, cell: "B2", ourCell: "B2", theirCell: "B1",
                  ours: cell("B2", 10), theirs: cell("B2", 20),
                }],
              },
            },
            { id: "Rules/A9:C12", name: "Data Driver drivers", reason: "rows", ours: version(3), base: version(2) },
          ],
        },
      ],
      palette: false,
    };

    it("lists the conflicting tables with the conflicting cells of a table merged cell by cell", async () => {
      harness.mockHttp.onGet("/projects/p1/merge/conflicts/workbook").reply(200, workbook);

      const overview = dataOf(await executeTool("get_merge_conflict_workbook", {
        projectId: "p1", file: "rules/Main.xlsx", }, harness.client));

      expect(harness.mockHttp.history.get[0].params).toEqual({ file: "rules/Main.xlsx" });
      expect(overview.sheets[0]).toEqual({
        name: "Notes", status: "theirs", inVersions: { ours: true, base: true, theirs: true }, tables: [],
      });
      expect(overview.sheets[1].tables).toEqual([
        {
          id: "Rules/A1:B2",
          name: "Rules rate",
          reason: "cells",
          byCells: true,
          ranges: { ours: "A1:B2", base: "A1:B2", theirs: "A1:B2", merged: "A1:B2" },
          conflictCells: [{
            row: 1,
            column: 1,
            cell: "B2",
            ourCell: "B2",
            theirCell: "B1",
            ours: { cell: "B2", value: 10 },
            theirs: { cell: "B2", value: 20 },
          }],
        },
        {
          id: "Rules/A9:C12",
          name: "Data Driver drivers",
          reason: "rows",
          byCells: false,
          ranges: { ours: "A1:B2", base: "A1:B2" },
        },
      ]);
    });

    it("reads one table in every version and merged, without the formatting of its cells", async () => {
      harness.mockHttp.onGet("/projects/p1/merge/conflicts/workbook").reply(200, workbook);

      const table = dataOf(await executeTool("get_merge_conflict_workbook", {
        projectId: "p1", file: "rules/Main.xlsx", table: "Rules/A1:B2", }, harness.client));

      expect(table).toMatchObject({ sheet: "Rules", id: "Rules/A1:B2", byCells: true });
      expect(table.theirs).toEqual({
        range: "A1:B2",
        rows: 2,
        changedCells: ["B2"],
        conflictCells: ["B2"],
        formattingChanged: false,
        cells: [
          [{ cell: "A1", value: "Rules rate", colspan: 2 }, { covered: true }],
          [{ cell: "A2", value: "US" }, { cell: "B2", value: 20 }],
        ],
      });
      expect(table.merged.cells[1]).toEqual([{ cell: "A2", value: "USA" }, { cell: "B2", value: 1 }]);
      expect(table.merged.changedCells).toEqual(["A2"]);
      expect(table.merged.conflicts[0]).toMatchObject({ cell: "B2", ours: { value: 10 }, theirs: { value: 20 } });
    });

    it("refuses a table that is no conflict of the workbook", async () => {
      harness.mockHttp.onGet("/projects/p1/merge/conflicts/workbook").reply(200, workbook);

      await expect(executeTool("get_merge_conflict_workbook", {
        projectId: "p1", file: "rules/Main.xlsx", table: "Rules/Z1:Z2",
      }, harness.client)).rejects.toThrow(/no conflicting table/);
    });
  });

  describe("a conflicted text file", () => {
    // Lines end with CR LF, as in a file written on Windows; the last one ends without a line break.
    const run = Array.from({ length: 10 }, (_, line) => `  <!-- ${line} -->\r\n`);
    const text = {
      blocks: [
        { kind: "merged", text: "<project>\r\n" },
        { kind: "conflict", ours: "  <module>Rates</module>\r\n", base: "", theirs: "  <module>Taxes</module>\r\n" },
        { kind: "merged", text: run.join(""), ours: run.join(""), base: "", theirs: "" },
        { kind: "conflict", ours: "  <v>2</v>\r\n", base: "  <v>1</v>\r\n", theirs: "  <v>3</v>\r\n" },
        { kind: "merged", text: "</project>" },
      ],
    };

    it("reads the conflicts numbered, and a long run of merged lines by its ends", async () => {
      harness.mockHttp.onGet("/projects/p1/merge/conflicts/text").reply(200, text);

      const response = await executeTool("get_merge_conflict_text", {
        projectId: "p1", file: "rules.xml", }, harness.client);

      expect(harness.mockHttp.history.get[0].params).toEqual({ file: "rules.xml" });
      expect(dataOf(response)).toEqual({
        file: "rules.xml",
        conflicts: 2,
        blocks: [
          { kind: "merged", text: "<project>\r\n" },
          {
            kind: "conflict",
            conflict: 0,
            ours: "  <module>Rates</module>\r\n",
            base: "",
            theirs: "  <module>Taxes</module>\r\n",
          },
          { kind: "merged", head: run.slice(0, 3).join(""), omittedLines: 4, tail: run.slice(-3).join("") },
          { kind: "conflict", conflict: 1, ours: "  <v>2</v>\r\n", base: "  <v>1</v>\r\n", theirs: "  <v>3</v>\r\n" },
          { kind: "merged", text: "</project>" },
        ],
      });
    });

    it("reads the rest of a conflicted text from a conflict, with the merged lines before it", async () => {
      harness.mockHttp.onGet("/projects/p1/merge/conflicts/text").reply(200, text);

      const response = dataOf<any>(await executeTool("get_merge_conflict_text", {
        projectId: "p1", file: "rules.xml", fromConflict: 1, }, harness.client));

      expect(response.conflicts).toBe(2);
      expect(response.blocks.map((block: any) => block.kind)).toEqual(["merged", "conflict", "merged"]);
      expect(response.blocks[1].conflict).toBe(1);
      await expect(executeTool("get_merge_conflict_text", { projectId: "p1", file: "rules.xml", fromConflict: 2 },
        harness.client)).rejects.toThrow(/no conflict 2/);
    });

    it("cuts a conflicted text over the size limit by whole blocks, continued at the next conflict", async () => {
      const big = (n: number) => ({ kind: "conflict", ours: `${n}${"o".repeat(6_000)}\n`, base: "", theirs: "t\n" });
      harness.mockHttp.onGet("/projects/p1/merge/conflicts/text").reply(200, {
        blocks: [big(0), { kind: "merged", text: "m\n" }, big(1), big(2), big(3), big(4)],
      });

      const response = JSON.parse(textOf(await executeTool("get_merge_conflict_text", {
        projectId: "p1", file: "notes.txt", }, harness.client)));

      expect(response.truncated).toBe(true);
      expect(response.truncation_message).toMatch(/continue with fromConflict=\d/);
      expect(response.data.conflicts).toBe(5);
    });

    it("refuses decisions that are not one per conflict before resolving anything", async () => {
      harness.mockHttp.onGet("/projects/p1/merge/conflicts").reply(200, {
        conflictGroups: [{ projectName: "Rating", files: ["rules.xml"] }],
      });
      harness.mockHttp.onGet("/projects/p1/merge/conflicts/text").reply(200, text);

      await expect(executeTool("resolve_merge_conflicts", {
        projectId: "p1", resolutions: [{ file: "rules.xml", conflicts: [{ side: "ours" }] }],
      }, harness.client)).rejects.toThrow(/has 2 conflicts, but 1 decisions/);
      expect(harness.mockHttp.history.post).toHaveLength(0);
    });

    it("refuses a decision that leaves the file empty, which Studio cannot store, before resolving anything", async () => {
      harness.mockHttp.onGet("/projects/p1/merge/conflicts").reply(200, {
        conflictGroups: [{ projectName: "Rating", files: ["notes.txt"] }],
      });
      harness.mockHttp.onGet("/projects/p1/merge/conflicts/text").reply(200, {
        blocks: [{ kind: "conflict", ours: "a\n", base: "", theirs: "b\n" }],
      });

      await expect(executeTool("resolve_merge_conflicts", {
        projectId: "p1", resolutions: [{ file: "notes.txt", conflicts: [{ lines: "" }] }],
      }, harness.client)).rejects.toThrow(/would be empty/);
      expect(harness.mockHttp.history.post).toHaveLength(0);
    });

    it("resolves with a workbook merged as decided and the other files by their strategies", async () => {
      harness.mockHttp.onGet("/projects/p1/merge/conflicts").reply(200, {
        conflictGroups: [{ projectName: "Rating", files: ["rules/Main.xlsx", "rules.xml", "rules-deploy.xml"] }],
      });
      harness.mockHttp.onGet("/projects/p1/merge/conflicts/text").reply(200, text);
      harness.mockHttp.onPost("/projects/p1/merge/conflicts/workbook").reply(200, Buffer.from("merged"));
      harness.mockHttp.onPost("/projects/p1/merge/conflicts/resolve").reply(200, {
        status: "success", resolvedFiles: ["rules/Main.xlsx", "rules.xml", "rules-deploy.xml"],
      });
      const decisions = [
        { id: "Rules/A1:B2", cells: { side: "ours", cells: [{ row: 1, column: 1, side: "theirs" }, { row: 1, column: 0, value: "USA" }] } },
        { id: "Notes", side: "base" },
      ];

      const response = await executeTool("resolve_merge_conflicts", {
        projectId: "p1",
        resolutions: [
          { file: "rules/Main.xlsx", decisions },
          { file: "rules.xml", conflicts: [{ side: "ours-theirs" }, { lines: "  <v>4</v>\n" }] },
          { file: "rules-deploy.xml", strategy: "THEIRS" },
        ],
        message: "Merge rates",
      }, harness.client);

      expect(harness.mockHttp.history.get.find((request) => request.url?.endsWith("/text"))?.params)
        .toEqual({ file: "rules.xml" });
      const [workbookRequest, resolveRequest] = harness.mockHttp.history.post;
      expect(workbookRequest.params).toEqual({ file: "rules/Main.xlsx" });
      expect(bodyOf(workbookRequest)).toEqual({ decisions });
      const form = resolveRequest.data as FormData;
      expect(form.get("resolutions[0].filePath")).toBe("rules/Main.xlsx");
      expect(form.get("resolutions[0].strategy")).toBe("CUSTOM");
      const merged = form.get("resolutions[0].file") as File;
      expect(merged.name).toBe("Main.xlsx");
      expect(await merged.text()).toBe("merged");
      expect(form.get("resolutions[1].filePath")).toBe("rules.xml");
      expect(form.get("resolutions[1].strategy")).toBe("CUSTOM");
      // Both modules are kept, and the typed line takes the line break of the file.
      const mergedText = form.get("resolutions[1].file") as File;
      expect(mergedText.name).toBe("rules.xml");
      expect(await mergedText.text()).toBe(
        "<project>\r\n  <module>Rates</module>\r\n  <module>Taxes</module>\r\n" + run.join("") +
        "  <v>4</v>\r\n</project>",
      );
      expect(form.get("resolutions[2].filePath")).toBe("rules-deploy.xml");
      expect(form.get("resolutions[2].strategy")).toBe("THEIRS");
      expect(form.get("resolutions[2].file")).toBeNull();
      expect(form.get("message")).toBe("Merge rates");
      expect(dataOf(response)).toEqual({
        status: "success", resolvedFiles: ["rules/Main.xlsx", "rules.xml", "rules-deploy.xml"],
      });
    });
  });

  it("refuses a resolution that leaves a conflicted file out before sending anything", async () => {
    harness.mockHttp.onGet("/projects/p1/merge/conflicts").reply(200, {
      conflictGroups: [{ projectName: "Rating", files: ["rules/Main.xlsx", "rules.xml"] }],
    });

    await expect(executeTool("resolve_merge_conflicts", {
      projectId: "p1", resolutions: [{ file: "rules.xml", strategy: "OURS" }],
    }, harness.client)).rejects.toThrow(/missing: rules\/Main\.xlsx/);
    expect(harness.mockHttp.history.post).toHaveLength(0);
  });

  it("refuses a cell decision with neither a side nor a value", async () => {
    await expect(executeTool("resolve_merge_conflicts", {
      projectId: "p1",
      resolutions: [{ file: "rules/Main.xlsx", decisions: [{ id: "Rules/A1:B2", cells: { cells: [{ row: 1, column: 1 }] } }] }],
    }, harness.client)).rejects.toThrow();
    expect(harness.mockHttp.history.get).toHaveLength(0);
  });

  it("clears the pending conflict session", async () => {
    harness.mockHttp.onDelete("/projects/p1/merge/conflicts").reply(204);
    const response = await executeTool("cancel_merge_conflicts", {
      projectId: "p1", }, harness.client);
    expect(dataOf<Record<string, unknown>>(response)).toMatchObject({ success: true });
  });

  it("deletes a project only after its current name matches confirmation", async () => {
    harness.mockHttp.onGet("/projects/p1").reply(200, { id: "p1", name: "Rating" });
    harness.mockHttp.onDelete("/projects/p1").reply(204);

    await expect(executeTool("delete_project", {
      projectId: "p1", confirmProjectName: "Other",
    }, harness.client)).rejects.toThrow(/does not match/);
    expect(harness.mockHttp.history.delete).toHaveLength(0);

    const response = await executeTool("delete_project", {
      projectId: "p1", confirmProjectName: "Rating", comment: "Remove retired project", }, harness.client);
    expect(harness.mockHttp.history.delete[0].params).toEqual({ comment: "Remove retired project" });
    // A repository without branches deletes the project as a whole
    expect(dataOf<Record<string, unknown>>(response)).toEqual({ success: true, deleted: true });
  });

  it("tells that a project is deleted on its current branch only", async () => {
    harness.mockHttp.onGet("/projects/p1").reply(200, { id: "p1", name: "Rating", branch: "feature" });
    harness.mockHttp.onDelete("/projects/p1").reply(204);

    const response = await executeTool("delete_project", { projectId: "p1", confirmProjectName: "Rating" }, harness.client);

    expect(dataOf<Record<string, unknown>>(response)).toEqual({ success: true, deletedOnBranch: "feature" });
  });

  it("protects base/protected branches and preserves slashes when force-deleting", async () => {
    const branches = [
      { name: "main", base: true },
      { name: "release", protected: true },
      { name: "feature/rates 2026" },
    ];
    harness.mockHttp.onGet("/projects/p1/branches").reply((config) => {
      expect(config.params?.scope).toBeUndefined();
      return [200, branches];
    });
    harness.mockHttp.onGet("/projects/p1").reply(200, {
      id: "p1", name: "Rating", branch: "feature/rates 2026", status: "OPENED",
    });
    harness.mockHttp.onPost("/projects/p1/merge/check").reply(200, {
      sourceBranch: "feature/rates 2026", targetBranch: "main", status: "up-to-date", canMerge: true,
    });
    harness.mockHttp.onDelete(/\/projects\/p1\/branches\//).reply(204);

    await expect(executeTool("delete_project_branch", {
      projectId: "p1", branch: "main", confirmBranchName: "main",
    }, harness.client)).rejects.toThrow(/base branch/);
    await expect(executeTool("delete_project_branch", {
      projectId: "p1", branch: "release", confirmBranchName: "release",
    }, harness.client)).rejects.toThrow(/protected/);

    await executeTool("delete_project_branch", {
      projectId: "p1",
      branch: "release",
      confirmBranchName: "release",
      force: true,
      confirmForce: true,
      confirmDataLoss: true,
    }, harness.client);

    await executeTool("delete_project_branch", {
      projectId: "p1",
      branch: "feature/rates 2026",
      confirmBranchName: "feature/rates 2026",
    }, harness.client);
    expect(harness.mockHttp.history.delete.map(({ url, params }) => ({ url, params }))).toEqual([
      { url: "/projects/p1/branches/release", params: { force: true } },
      { url: "/projects/p1/branches/feature/rates%202026", params: { force: false } },
    ]);
  });

  it("refuses an unmerged current branch until data loss is explicitly confirmed", async () => {
    const branches = [
      { name: "main", base: true },
      { name: "feature/rates" },
    ];
    harness.mockHttp.onGet("/projects/p1/branches").reply(200, branches);
    harness.mockHttp.onGet("/projects/p1").reply(200, {
      id: "p1", name: "Rating", branch: "feature/rates", status: "OPENED",
    });
    harness.mockHttp.onPost("/projects/p1/merge/check").reply(200, {
      sourceBranch: "feature/rates", targetBranch: "main", status: "mergeable", canMerge: true,
    });
    harness.mockHttp.onDelete("/projects/p1/branches/feature/rates").reply(204);

    const request = {
      projectId: "p1", branch: "feature/rates", confirmBranchName: "feature/rates",
    };
    await expect(executeTool("delete_project_branch", request, harness.client)).rejects.toThrow(
      /not merged into base branch 'main'.*confirmDataLoss=true/,
    );
    expect(harness.mockHttp.history.delete).toHaveLength(0);

    const response = await executeTool("delete_project_branch", {
      ...request,
      confirmDataLoss: true,
    }, harness.client);

    expect(harness.mockHttp.history.post.map(bodyOf)).toEqual([
      { mode: "send", otherBranch: "main" },
      { mode: "send", otherBranch: "main" },
    ]);
    expect(dataOf<Record<string, unknown>>(response)).toMatchObject({
      success: true,
      dataLossConfirmed: true,
      safety: {
        baseBranch: "main",
        currentBranch: "feature/rates",
        divergenceStatus: "unmerged",
        unsavedChanges: false,
      },
    });
  });

  it("does not run a misleading divergence check for a non-current deletion target", async () => {
    const branches = [
      { name: "main", base: true },
      { name: "feature/other" },
    ];
    harness.mockHttp.onGet("/projects/p1/branches").reply(200, branches);
    harness.mockHttp.onGet("/projects/p1").reply(200, {
      id: "p1", name: "Rating", branch: "main", status: "OPENED",
    });

    await expect(executeTool("delete_project_branch", {
      projectId: "p1", branch: "feature/other", confirmBranchName: "feature/other",
    }, harness.client)).rejects.toThrow(/current branch 'main'.*Open 'feature\/other'/);

    expect(harness.mockHttp.history.post).toHaveLength(0);
    expect(harness.mockHttp.history.delete).toHaveLength(0);
  });

  it.each([
    ["feature/concurrent", "main"],
    ["feature/rates", "develop"],
  ])("rejects an up-to-date check for unexpected branches (%s -> %s)", async (sourceBranch, targetBranch) => {
    const branches = [
      { name: "main", base: true },
      { name: "feature/rates" },
    ];
    harness.mockHttp.onGet("/projects/p1/branches").reply(200, branches);
    harness.mockHttp.onGet("/projects/p1").reply(200, {
      id: "p1", name: "Rating", branch: "feature/rates", status: "OPENED",
    });
    harness.mockHttp.onPost("/projects/p1/merge/check").reply(200, {
      sourceBranch, targetBranch, status: "up-to-date", canMerge: true,
    });

    await expect(executeTool("delete_project_branch", {
      projectId: "p1", branch: "feature/rates", confirmBranchName: "feature/rates",
    }, harness.client)).rejects.toThrow(
      `Studio checked branch '${sourceBranch}' into '${targetBranch}' instead of ` +
        "'feature/rates' into 'main', so deletion-target divergence was not verified.",
    );

    expect(harness.mockHttp.history.delete).toHaveLength(0);
  });

  it("requires data-loss confirmation for unsaved changes without running merge check", async () => {
    const branches = [
      { name: "main", base: true },
      { name: "feature/rates" },
    ];
    harness.mockHttp.onGet("/projects/p1/branches").reply(200, branches);
    harness.mockHttp.onGet("/projects/p1").reply(200, {
      id: "p1", name: "Rating", branch: "feature/rates", status: "EDITING",
    });

    await expect(executeTool("delete_project_branch", {
      projectId: "p1", branch: "feature/rates", confirmBranchName: "feature/rates",
    }, harness.client)).rejects.toThrow(/unsaved working-copy changes.*confirmDataLoss=true/);

    expect(harness.mockHttp.history.post).toHaveLength(0);
    expect(harness.mockHttp.history.delete).toHaveLength(0);
  });
});
