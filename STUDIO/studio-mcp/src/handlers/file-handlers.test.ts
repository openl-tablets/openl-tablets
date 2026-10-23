/**
 * Integration tests of the project file tools of src/handlers/file-handlers.ts: reading, writing, deleting, searching,
 * copying and moving the files of a project.
 */

import { describe, it, expect } from "vitest";
import { executeTool } from "./index.js";
import { bodyOf } from "../testing/mock-http.js";
import { dataOf, harness, textOf, useToolHarness } from "../testing/harness.js";

useToolHarness();

/** Answers the read of a file of project p1 with its bytes. */
const answerFile = (path: string, body: string | Buffer, contentType = "application/octet-stream"): void => {
  harness.mockHttp.onGet(`/projects/p1/files/${path}`).reply(200, body, {
    "content-type": contentType,
    "content-disposition": "attachment",
  });
};

/** The bytes the first write sent. */
const writtenBytes = (): Buffer => Buffer.from(harness.mockHttp.history.post[0].data as Uint8Array);

describe("openl_read_project_file", () => {
  const project = { id: "p1", name: "Bank Rating", repository: "design", path: "Bank Rating", branch: "main" };

  /** Answers the metadata of a file of project p1. */
  const answerMeta = (path: string, size: number): void => {
    harness.mockHttp.onGet(`/projects/p1/files/${path}`, { params: expect.objectContaining({ view: "meta" }) })
      .reply(200, { path, name: path.split("/").at(-1), type: "file", size });
  };
  const singleUser = (): void => {
    harness.mockHttp.onGet("/settings").reply(200, {});
    harness.mockHttp.onGet("/projects/p1").reply(200, project);
  };
  const authenticating = (): void => {
    harness.mockHttp.onGet("/settings").reply(200, { userMode: "INTERNAL" });
    harness.mockHttp.onPost("/transfer-links").reply(201, { path: "transfers/k3Jx", expiresAt: "2026-10-23T10:15:00Z" });
    harness.mockHttp.onGet("/projects/p1").reply(200, project);
  };

  it("returns a text file of up to 5,000 bytes verbatim", async () => {
    answerMeta("readme.md", 5000);
    answerFile("readme.md", "# Title\nbody", "text/markdown");

    const result = await executeTool("read_project_file", { projectId: "p1", path: "readme.md" }, harness.client);

    expect(textOf(result)).toBe("# Title\nbody");
    expect(harness.mockHttp.history.get[0].params).toMatchObject({ view: "meta" });
  });

  it("decodes a text in the charset Studio names for it", async () => {
    answerMeta("i18n/fr.properties", 4);
    answerFile("i18n/fr.properties", Buffer.from([0x63, 0x61, 0x66, 0xe9]), "text/plain; charset=ISO-8859-1");

    const result = await executeTool("read_project_file", { projectId: "p1", path: "i18n/fr.properties" },
      harness.client);

    expect(textOf(result)).toBe("café");
  });

  it("answers a larger text with the address of the REST API in single-user mode, reading no byte of it", async () => {
    answerMeta("rules.xml", 5001);
    singleUser();

    const result = await executeTool("read_project_file", {
      projectId: "p1", path: "rules.xml", branch: "feature",
    }, harness.client);

    expect(dataOf(result)).toEqual({
      url: "http://localhost:8080/rest/projects/p1/files/rules.xml?branch=feature",
      method: "GET",
      size: 5001,
      repositoryId: "design",
      repositoryPath: "Bank Rating/rules.xml",
      branch: "feature",
      curl: "curl -fsS -o 'rules.xml' 'http://localhost:8080/rest/projects/p1/files/rules.xml?branch=feature'",
      note: "Run curl to save the file.",
    });
    expect(harness.mockHttp.history.get.filter((request) => !request.params?.view
      && request.url.includes("/files/"))).toHaveLength(0);
  });

  it("answers a small binary file with a transfer link of a revision where Studio authenticates", async () => {
    answerMeta("rules/Model.xlsx", 4);
    answerFile("rules/Model.xlsx", Buffer.from([0x50, 0x4b, 0x03, 0x04]),
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    authenticating();

    const result = await executeTool("read_project_file", {
      projectId: "p1", path: "rules/Model.xlsx", version: "r1",
    }, harness.client);

    expect(bodyOf(harness.mockHttp.history.post[0])).toEqual({
      kind: "download", projectId: "p1", path: "rules/Model.xlsx", version: "r1",
    });
    expect(dataOf(result)).toMatchObject({
      url: "http://localhost:8080/transfers/k3Jx",
      method: "GET",
      expiresAt: "2026-10-23T10:15:00Z",
      size: 4,
      mimeType: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      repositoryPath: "Bank Rating/rules/Model.xlsx",
      branch: "main",
      curl: "curl -fsS -o 'Model.xlsx' 'http://localhost:8080/transfers/k3Jx'",
    });
  });

  it.each([
    ["a folder", "rules/", "rules.zip", "Bank Rating/rules/"],
    ["the project root", "", "project.zip", "Bank Rating/"],
  ])("answers %s with a link to its ZIP archive", async (_case, path, local, repositoryPath) => {
    singleUser();

    const result = await executeTool("read_project_file", { projectId: "p1", path }, harness.client);

    expect(dataOf(result)).toMatchObject({
      url: `http://localhost:8080/rest/projects/p1/files/${path}?download=true`,
      curl: `curl -fsS -o '${local}' 'http://localhost:8080/rest/projects/p1/files/${path}?download=true'`,
      repositoryPath,
    });
    expect(harness.mockHttp.history.get.some((request) => request.params?.view === "meta")).toBe(false);
  });

  it("answers a path the metadata names a folder with a link to its archive", async () => {
    harness.mockHttp.onGet("/projects/p1/files/rules", { params: { view: "meta" } })
      .reply(200, { path: "rules", name: "rules", type: "folder" });
    authenticating();

    await executeTool("read_project_file", { projectId: "p1", path: "rules" }, harness.client);

    expect(bodyOf(harness.mockHttp.history.post[0])).toMatchObject({ kind: "archive", path: "rules" });
  });

  it.each([
    ["encoding", { encoding: "base64" }],
    ["a byte range", { offset: 0, length: 10 }],
    ["a folder listing", { recursive: true }],
  ])("refuses %s, which it no longer takes", async (_case, extra) => {
    await expect(executeTool("read_project_file", { projectId: "p1", path: "rules.xml", ...extra },
      harness.client)).rejects.toThrow(/invalid arguments|unrecognized/i);
    expect(harness.mockHttp.history.get).toHaveLength(0);
  });
});

describe("openl_create_upload_link", () => {
  const project = { id: "p1", name: "Bank Rating", repository: "design-flat" };

  it("issues a transfer link that writes the file once where Studio authenticates", async () => {
    harness.mockHttp.onGet("/settings").reply(200, { userMode: "EXTERNAL" });
    harness.mockHttp.onPost("/transfer-links").reply(201, { path: "transfers/up", expiresAt: "2026-10-23T10:15:00Z" });
    harness.mockHttp.onGet("/projects/p1").reply(200, project);
    const sha256 = "ab".repeat(32);

    const result = await executeTool("create_upload_link", {
      projectId: "p1", path: "rules/Model.xlsx", conflictPolicy: "OVERWRITE", size: 4, sha256,
    }, harness.client);

    expect(bodyOf(harness.mockHttp.history.post[0])).toEqual({
      kind: "upload", projectId: "p1", path: "rules/Model.xlsx", conflictPolicy: "OVERWRITE", createFolders: true,
      size: 4, sha256,
    });
    expect(dataOf(result)).toMatchObject({
      url: "http://localhost:8080/transfers/up",
      method: "PUT",
      expiresAt: "2026-10-23T10:15:00Z",
      repositoryId: "design-flat",
      repositoryPath: "rules/Model.xlsx",
      curl: "curl -fsS -T 'Model.xlsx' 'http://localhost:8080/transfers/up'",
    });
    expect(dataOf<{ note: string }>(result).note).toMatch(/once.*openl_save_project/);
  });

  it("answers the raw POST of the files API in single-user mode, its quotes kept for the shell", async () => {
    harness.mockHttp.onGet("/settings").reply(200, {});
    harness.mockHttp.onGet("/projects/p1").reply(200, project);

    const result = await executeTool("create_upload_link", {
      projectId: "p1", path: "docs/it's new.md", createFolders: false, branch: "main",
    }, harness.client);

    const url = "http://localhost:8080/rest/projects/p1/files/docs/it's%20new.md"
      + "?createFolders=false&conflictPolicy=FAIL&branch=main";
    const quotedUrl = `'${url.replaceAll("'", String.raw`'\''`)}'`;
    expect(dataOf(result)).toMatchObject({
      url,
      method: "POST",
      branch: "main",
      curl: String.raw`curl -fsS --data-binary @'it'\''s new.md' -H 'Content-Type: application/octet-stream' `
        + quotedUrl,
    });
    expect(dataOf(result)).not.toHaveProperty("expiresAt");
    expect(harness.mockHttp.history.post).toHaveLength(0);
  });

  it("refuses a folder, which a link cannot write as one file", async () => {
    await expect(executeTool("create_upload_link", { projectId: "p1", path: "rules/" }, harness.client))
      .rejects.toThrow(/names a folder/);
    expect(harness.mockHttp.history.get).toHaveLength(0);
  });

  it("refuses a sha256 that is not hex of 64 characters", async () => {
    await expect(executeTool("create_upload_link", { projectId: "p1", path: "a.bin", sha256: "abc" },
      harness.client)).rejects.toThrow(/sha256/);
  });
});

describe("openl_write_project_file", () => {
  it("writes UTF-8 text and reports an uncommitted working-copy write", async () => {
    harness.mockHttp.onPost("/projects/p1/files/docs/new.md").reply(201);

    const result = await executeTool("write_project_file", {
      projectId: "p1",
      path: "docs/new.md",
      content: "héllo",
      response_format: "json",
    }, harness.client);

    expect(writtenBytes().toString("utf-8")).toBe("héllo");
    // createFolders default (true) is materialized by the handler.
    expect(harness.mockHttp.history.post[0].params?.createFolders).toBe(true);
    expect(dataOf(result)).toMatchObject({ success: true, bytesWritten: 6, committed: false });
  });

  it.each([
    ["a base64 blob", { blob: Buffer.from("bytes").toString("base64") }],
    ["base64 content", { content: "aGVsbG8=", encoding: "base64" }],
    ["no content", {}],
  ])("refuses %s before calling OpenL Studio", async (_case, args) => {
    await expect(executeTool("write_project_file", { projectId: "p1", path: "x.bin", ...args }, harness.client))
      .rejects.toThrow(/invalid arguments|unrecognized|expected string/i);
    expect(harness.mockHttp.history.post).toHaveLength(0);
  });

  it("commits the write with a 'message' and reports committed:true", async () => {
    harness.mockHttp.onPost("/projects/p1/files/docs/x.md").reply(201);
    // saveProject(): GET project (EDITING, design) -> PATCH commit
    harness.mockHttp.onGet("/projects/p1").reply(200, { id: "p1", name: "P", status: "EDITING", repository: "design" });
    harness.mockHttp.onPatch("/projects/p1").reply(204);

    const result = await executeTool("write_project_file", {
      projectId: "p1",
      path: "docs/x.md",
      content: "hello",
      message: "add docs/x.md",
      response_format: "json",
    }, harness.client);

    expect(harness.mockHttp.history.patch.map(bodyOf)).toEqual([{ save: true, comment: "add docs/x.md" }]);
    expect(dataOf(result).committed).toBe(true);
    expect(dataOf(result).message).toContain("committed");
  });

  it("reports a written file it could not commit instead of failing", async () => {
    harness.mockHttp.onPost("/projects/p1/files/docs/x.md").reply(201);
    harness.mockHttp.onGet("/projects/p1").reply(200, { id: "p1", name: "P", status: "EDITING", repository: "local" });

    const result = await executeTool("write_project_file", {
      projectId: "p1",
      path: "docs/x.md",
      content: "hello",
      message: "add docs/x.md",
      response_format: "json",
    }, harness.client);

    expect(dataOf(result)).toMatchObject({ success: true, committed: false });
    expect(harness.mockHttp.history.patch).toHaveLength(0);
  });

  it("POSTs with conflictPolicy FAIL by default and forwards branch", async () => {
    harness.mockHttp.onPost("/projects/p1/files/docs/x.md").reply(201);

    const result = await executeTool("write_project_file", {
      projectId: "p1",
      path: "docs/x.md",
      content: "hi",
      branch: "dev",
      response_format: "json",
    }, harness.client);

    expect(harness.mockHttp.history.post[0].params).toMatchObject({ conflictPolicy: "FAIL", branch: "dev" });
    expect(dataOf(result)).toMatchObject({ action: "created", branch: "dev" });
  });

  it("lets Studio replace an existing file with conflictPolicy=OVERWRITE", async () => {
    harness.mockHttp.onPost("/projects/p1/files/docs/x.md").reply(204);

    const result = await executeTool("write_project_file", {
      projectId: "p1",
      path: "docs/x.md",
      content: "v2-overwrite",
      conflictPolicy: "OVERWRITE",
      response_format: "json",
    }, harness.client);

    expect(harness.mockHttp.history.post[0].params?.conflictPolicy).toBe("OVERWRITE");
    expect(writtenBytes().toString("utf-8")).toBe("v2-overwrite");
    expect(dataOf(result)).toMatchObject({ success: true, action: "overwritten" });
    expect(harness.mockHttp.history.put).toHaveLength(0);
  });

  it.each([
    ["keeps the CRLF line ends of the file it replaces", "a=1\r\nb=2\r\n", "a=1\nb=2\nc=3\n", "a=1\r\nb=2\r\nc=3\r\n"],
    ["keeps the LF line ends of the file it replaces", "a=1\nb=2\n", "a=1\nb=2\nc=3\n", "a=1\nb=2\nc=3\n"],
    ["writes CR characters it is given as they are", "a=1\r\n", "a=1\r\nb=2", "a=1\r\nb=2"],
  ])("conflictPolicy=OVERWRITE %s (EPBDS-16856)", async (_case, existing, content, written) => {
    answerFile("i18n/message.properties", existing, "text/plain");
    harness.mockHttp.onPost("/projects/p1/files/i18n/message.properties").reply(204);

    await executeTool("write_project_file", {
      projectId: "p1", path: "i18n/message.properties", content, conflictPolicy: "OVERWRITE",
    }, harness.client);

    expect(writtenBytes().toString("utf-8")).toBe(written);
  });

  it("conflictPolicy=OVERWRITE writes a file that does not exist yet as it is given (EPBDS-16856)", async () => {
    harness.mockHttp.onGet("/projects/p1/files/new.txt").reply(404, { message: "Not found" });
    harness.mockHttp.onPost("/projects/p1/files/new.txt").reply(204);

    await executeTool("write_project_file", {
      projectId: "p1", path: "new.txt", content: "a\nb\n", conflictPolicy: "OVERWRITE",
    }, harness.client);

    expect(writtenBytes().toString("utf-8")).toBe("a\nb\n");
  });

  it("reports an existing file left unchanged with conflictPolicy=SKIP", async () => {
    // Studio leaves the file there as it is for SKIP.
    harness.mockHttp.onPost("/projects/p1/files/docs/x.md").reply((config) => [config.params?.conflictPolicy === "SKIP" ? 204 : 400]);

    const result = await executeTool("write_project_file", {
      projectId: "p1",
      path: "docs/x.md",
      content: "v2",
      conflictPolicy: "SKIP",
      response_format: "json",
    }, harness.client);

    expect(dataOf(result)).toMatchObject({ success: true, skipped: true, written: false });
  });

  it("passes on the reason Studio refuses a write for", async () => {
    harness.mockHttp.onPost("/projects/p1/files/docs/x.md").reply(409, { message: "Project is locked" });

    await expect(executeTool("write_project_file", {
      projectId: "p1",
      path: "docs/x.md",
      content: "v2",
      conflictPolicy: "OVERWRITE",
    }, harness.client)).rejects.toThrow(/Project is locked/);
  });
});

describe("delete, search, copy and move", () => {
  it("openl_delete_project_file deletes and reports success", async () => {
    harness.mockHttp.onDelete("/projects/p1/files/old.txt").reply(204);

    const result = await executeTool("delete_project_file", {
      projectId: "p1",
      path: "old.txt",
      response_format: "json",
    }, harness.client);

    expect(dataOf(result)).toMatchObject({ success: true, path: "old.txt" });
  });

  it("openl_search_project_files builds the query body and returns matches", async () => {
    const nodes = [{ path: "rules/config.xml", name: "config.xml", type: "file" }];
    harness.mockHttp.onPost("/projects/p1/file-search").reply(200, nodes);

    const result = await executeTool("search_project_files", {
      projectId: "p1",
      pattern: "**/*.xml",
      content: "premium",
      response_format: "json",
    }, harness.client);

    expect(bodyOf(harness.mockHttp.history.post[0])).toEqual({ pattern: "**/*.xml", content: "premium" });
    expect(dataOf(result)).toEqual(nodes);
  });

  it("openl_search_project_files forwards extensions/type/scope=ANCESTORS/recursive/from/version", async () => {
    harness.mockHttp.onPost("/projects/p1/file-search").reply(200, []);
    const query = {
      extensions: ["xlsx", "xml"],
      type: "FOLDER",
      scope: "ANCESTORS",
      recursive: true,
      from: "rules",
      version: "abc123",
    };

    await executeTool("search_project_files", { projectId: "p1", ...query }, harness.client);

    expect(bodyOf(harness.mockHttp.history.post[0])).toEqual(query);
  });

  it("openl_search_project_files paginates the match set client-side (limit/offset)", async () => {
    const nodes = Array.from({ length: 5 }, (_, i) => ({ path: `f${i}.xlsx`, name: `f${i}.xlsx`, type: "file" }));
    harness.mockHttp.onPost("/projects/p1/file-search").reply(200, nodes);

    const page1 = JSON.parse(textOf(await executeTool("search_project_files", {
      projectId: "p1", pattern: "**/*.xlsx", limit: 2, offset: 0, response_format: "json",
    }, harness.client)));
    const page3 = JSON.parse(textOf(await executeTool("search_project_files", {
      projectId: "p1", pattern: "**/*.xlsx", limit: 2, offset: 4, response_format: "json",
    }, harness.client)));

    expect(page1.data.map((node: { name: string }) => node.name)).toEqual(["f0.xlsx", "f1.xlsx"]);
    expect(page1.pagination).toMatchObject({ limit: 2, offset: 0, total_count: 5, has_more: true });
    expect(page3.data.map((node: { name: string }) => node.name)).toEqual(["f4.xlsx"]);
    expect(page3.pagination).toMatchObject({ total_count: 5, has_more: false });
  });

  it.each([
    ["copy_project_file", "file-copy", 201],
    ["move_project_file", "file-move", 204],
  ])("openl_%s sends the paths and reports them", async (tool, endpoint, status) => {
    harness.mockHttp.onPost(`/projects/p1/${endpoint}`).reply(status);
    const paths = { sourcePath: "rules/M.xlsx", destinationPath: "legacy/M.xlsx" };

    const result = await executeTool(tool, { projectId: "p1", ...paths, response_format: "json" }, harness.client);

    expect(bodyOf(harness.mockHttp.history.post[0])).toEqual(paths);
    expect(dataOf(result)).toMatchObject({ success: true, ...paths });
  });

  it.each([
    ["copy_project_file", "file-copy"],
    ["move_project_file", "file-move"],
  ])("openl_%s explains a destination already there", async (tool, endpoint) => {
    harness.mockHttp.onPost(`/projects/p1/${endpoint}`).reply(409, { message: "Conflict" });

    await expect(executeTool(tool, {
      projectId: "p1",
      sourcePath: "a.xlsx",
      destinationPath: "b.xlsx",
    }, harness.client)).rejects.toThrow(/to 'b\.xlsx': a file already exists there/);
  });
});
