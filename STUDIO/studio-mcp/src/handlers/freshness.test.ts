/**
 * A tool that works with what Studio compiled keeps the project current with its files by itself: the agent does not
 * have to ask for it.
 */
import { describe, it, expect } from "vitest";

import { executeTool } from "./index.js";
import { STATUS_SUMMARY_QUERY } from "../client.js";
import { pageOf } from "../testing/openl-api-mocks.js";
import { dataOf, harness, useToolHarness } from "../testing/harness.js";

useToolHarness();

const projectPath = "/projects/p1";
const status = { params: STATUS_SUMMARY_QUERY };
const compilePath = "/projects/p1/modules/Main/compile";

/** Studio tells that the files changed, then that it compiles the project again, then that the compilation ended. */
function filesChangedOnDisk(): void {
  harness.mockHttp
    .onGet(projectPath, status).replyOnce(200, { compileStatus: { compileState: "ok", changedOnDisk: true } })
    .onGet(projectPath, status).replyOnce(200, { compileStatus: { compileState: "compiling" } })
    .onGet(projectPath, status).reply(200, { compileStatus: { compileState: "ok" } })
    .onGet("/projects/p1/modules").reply(200, [{ name: "Main", path: "rules/Main.xlsx" }])
    .onPost(compilePath).reply((request) => [request.params?.reset ? 202 : 400]);
}

describe("a project whose files changed on disk", () => {
  it("is compiled again before a tool reads what Studio compiled of it", async () => {
    filesChangedOnDisk();
    // The tables of the module a workbook added answer only once the compilation from the files ended.
    const statusReads = () => harness.mockHttp.history.get.filter((request) => request.url === projectPath).length;
    harness.mockHttp.onGet("/projects/p1/tables").reply(() => statusReads() >= 3
      ? [200, pageOf([{ id: "t1", name: "Added" }])]
      : [500, { message: "read before the compilation from the files ended" }]);

    const result = await executeTool("list_tables", { projectId: "p1" }, harness.client);

    expect(dataOf(result)).toEqual([expect.objectContaining({ id: "t1" })]);
  });

  it("is compiled again before an edit, which names the version of the table it is based on", async () => {
    filesChangedOnDisk();
    harness.mockHttp.onPost("/projects/p1/tables/t1/actions").reply(204);
    harness.mockHttp.onGet("/projects/p1/tables/t1").reply(200, { id: "t1", name: "T", tableType: "RawSource" });

    await executeTool("update_table_cell", {
      projectId: "p1", tableId: "t1", version: "v1", row: 1, column: 1, value: "x",
    }, harness.client);

    // Studio compares the version with the tables compiled from the files as they are, and refuses a table changed
    expect(harness.mockHttp.history.post.map((request) => request.url))
      .toEqual([compilePath, "/projects/p1/tables/t1/actions"]);
    expect(harness.mockHttp.history.post[1].headers).toMatchObject({ "If-Match": '"v1"' });
  });

  it("is not compiled again while its files stand as compiled", async () => {
    harness.mockHttp.onGet(projectPath, status).reply(200, { compileStatus: { compileState: "ok" } });
    harness.mockHttp.onGet("/projects/p1/tables").reply(200, pageOf([]));

    await executeTool("list_tables", { projectId: "p1" }, harness.client);

    expect(harness.mockHttp.history.get.map((request) => request.url)).toEqual([projectPath, "/projects/p1/tables"]);
    expect(harness.mockHttp.history.post).toHaveLength(0);
  });

  it("is not asked about by a tool that does not work with what was compiled", async () => {
    harness.mockHttp.onGet(projectPath, status)
      .reply(200, { compileStatus: { compileState: "ok", changedOnDisk: true } });
    harness.mockHttp.onGet("/projects/p1/modules").reply(200, []);

    await executeTool("list_project_modules", { projectId: "p1" }, harness.client);

    expect(harness.mockHttp.history.get.map((request) => request.url)).toEqual(["/projects/p1/modules"]);
  });
});
