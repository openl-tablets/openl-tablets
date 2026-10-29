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

/** The bytes the first write sent: the POST of a new file, or the PUT of a replacement. */
const writtenBytes = (method: "post" | "put" = "post"): Buffer =>
  Buffer.from(harness.mockHttp.history[method][0].data as Uint8Array);

/** The reads of a file of a project the tool sent; the read of the project itself is none. */
const fileReads = () => harness.mockHttp.history.get.filter((request) => request.url?.includes("/files/"));

/** Answers the project p1 as edited in place: neither opened nor closed. */
const editedInPlace = (): void => {
  harness.mockHttp.onGet("/projects/p1").reply(200, { id: "p1", name: "P", repository: "design", capabilities: {} });
};

/** Answers the metadata of a file of project p1, with its version in the ETag header. */
const answerMeta = (path: string, size: number): void => {
  harness.mockHttp.onGet(`/projects/p1/files/${path}`, { params: expect.objectContaining({ view: "meta" }) })
    .reply(200, { path, name: path.split("/").at(-1), type: "file", size }, { etag: '"v1"' });
};

describe("openl_read_project_file", () => {
  const project = { id: "p1", name: "Bank Rating", repository: "design", path: "Bank Rating", branch: "main" };

  const singleUser = (): void => {
    harness.mockHttp.onGet("/settings").reply(200, {});
    harness.mockHttp.onGet("/projects/p1").reply(200, project);
  };
  const authenticating = (): void => {
    harness.mockHttp.onGet("/settings").reply(200, { userMode: "INTERNAL" });
    harness.mockHttp.onPost("/transfer-links").reply(201, { path: "transfers/k3Jx", expiresAt: "2026-10-23T10:15:00Z" });
    harness.mockHttp.onGet("/projects/p1").reply(200, project);
  };

  it("returns a text file of up to 5,000 bytes verbatim, its version told apart", async () => {
    answerMeta("readme.md", 5000);
    answerFile("readme.md", "# Title\nbody", "text/markdown");

    const result = await executeTool("read_project_file", { projectId: "p1", path: "readme.md" }, harness.client);

    expect(textOf(result)).toBe("# Title\nbody");
    expect(result.content[1].text).toMatch(/^version: v1 — pass it as 'version'/);
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
      version: "v1",
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
      repositoryPath: "Bank Rating/rules/Model.xlsx",
      branch: "main",
      curl: "curl -fsS -o 'Model.xlsx' 'http://localhost:8080/transfers/k3Jx'",
    });
    // The extension of the file tells its type
    expect(dataOf(result)).not.toHaveProperty("mimeType");
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
      projectId: "p1", path: "rules/Model.xlsx", version: "v1", size: 4, sha256,
    }, harness.client);

    expect(bodyOf(harness.mockHttp.history.post[0])).toEqual({
      kind: "upload", projectId: "p1", path: "rules/Model.xlsx", expectedVersion: "v1", createFolders: true,
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

  it("answers the raw PUT of the files API naming the version a replacement in single-user mode is based on", async () => {
    harness.mockHttp.onGet("/settings").reply(200, {});
    harness.mockHttp.onGet("/projects/p1").reply(200, project);

    const result = await executeTool("create_upload_link", {
      projectId: "p1", path: "rules/Model.xlsx", version: "v1",
    }, harness.client);

    const url = "http://localhost:8080/rest/projects/p1/files/rules/Model.xlsx";
    expect(dataOf(result)).toMatchObject({
      url,
      method: "PUT",
      curl: `curl -fsS -X PUT -H 'If-Match: "v1"' --data-binary @'Model.xlsx' -H 'Content-Type: application/octet-stream' '${url}'`,
    });
  });

  it("tells that an upload into a project edited in place needs no commit", async () => {
    harness.mockHttp.onGet("/settings").reply(200, {});
    editedInPlace();

    const result = await executeTool("create_upload_link", { projectId: "p1", path: "rules/Model.xlsx" },
      harness.client);

    expect(dataOf<{ note: string }>(result).note).toMatch(/edited in place.*nothing to commit/);
    expect(dataOf<{ note: string }>(result).note).not.toContain("openl_save_project");
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
    }, harness.client);

    expect(dataOf(result)).toMatchObject({ success: true, committed: false });
    expect(harness.mockHttp.history.patch).toHaveLength(0);
  });

  it("commits nothing in a project edited in place, telling the file is written", async () => {
    harness.mockHttp.onPost("/projects/p1/files/docs/x.md").reply(201);
    editedInPlace();

    const result = await executeTool("write_project_file", {
      projectId: "p1",
      path: "docs/x.md",
      content: "hello",
      message: "add docs/x.md",
    }, harness.client);

    expect(harness.mockHttp.history.patch).toHaveLength(0);
    expect(dataOf(result)).toMatchObject({ success: true, committed: false });
    expect(dataOf(result).message).toMatch(/^Wrote 5 byte\(s\) to 'docs\/x\.md'\. The project is edited in place/);
  });

  it("creates a new file with conflictPolicy FAIL and forwards branch", async () => {
    harness.mockHttp.onPost("/projects/p1/files/docs/x.md").reply(201);

    const result = await executeTool("write_project_file", {
      projectId: "p1",
      path: "docs/x.md",
      content: "hi",
      branch: "dev",
    }, harness.client);

    expect(harness.mockHttp.history.post[0].params).toMatchObject({ conflictPolicy: "FAIL", branch: "dev" });
    expect(dataOf(result)).toMatchObject({ action: "created", branch: "dev" });
  });

  it("tells to read the version of a file already at the path, which a write without one leaves as it is", async () => {
    harness.mockHttp.onPost("/projects/p1/files/docs/x.md").reply(409, { message: "A file already exists at 'docs/x.md'." });

    await expect(executeTool("write_project_file", { projectId: "p1", path: "docs/x.md", content: "v2" },
      harness.client)).rejects.toThrow(/read it with openl_read_project_file and pass the 'version'/);
    expect(harness.mockHttp.history.put).toHaveLength(0);
  });

  it("replaces the version of a file it names", async () => {
    harness.mockHttp.onPut("/projects/p1/files/docs/x.md").reply(204);

    const result = await executeTool("write_project_file", {
      projectId: "p1",
      path: "docs/x.md",
      content: "v2-overwrite",
      version: "v1",
      branch: "dev",
    }, harness.client);

    expect(harness.mockHttp.history.put[0].headers).toMatchObject({ "If-Match": '"v1"' });
    expect(harness.mockHttp.history.put[0].params).toEqual({ branch: "dev" });
    expect(writtenBytes("put").toString("utf-8")).toBe("v2-overwrite");
    expect(dataOf(result)).toMatchObject({ success: true, action: "overwritten" });
    expect(harness.mockHttp.history.post).toHaveLength(0);
  });

  it.each([
    ["keeps the CRLF line ends of the file it replaces", "a=1\r\nb=2\r\n", "a=1\nb=2\nc=3\n", "a=1\r\nb=2\r\nc=3\r\n"],
    ["keeps the LF line ends of the file it replaces", "a=1\nb=2\n", "a=1\nb=2\nc=3\n", "a=1\nb=2\nc=3\n"],
    ["writes CR characters it is given as they are", "a=1\r\n", "a=1\r\nb=2", "a=1\r\nb=2"],
  ])("a replacement %s (EPBDS-16856)", async (_case, existing, content, written) => {
    answerFile("i18n/message.properties", existing, "text/plain");
    harness.mockHttp.onPut("/projects/p1/files/i18n/message.properties").reply(204);

    await executeTool("write_project_file", {
      projectId: "p1", path: "i18n/message.properties", content, version: "v1",
    }, harness.client);

    expect(writtenBytes("put").toString("utf-8")).toBe(written);
  });

  it("writes a new file as it is given, reading no file before it (EPBDS-16856)", async () => {
    harness.mockHttp.onPost("/projects/p1/files/new.txt").reply(201);

    await executeTool("write_project_file", { projectId: "p1", path: "new.txt", content: "a\nb\n" }, harness.client);

    expect(writtenBytes().toString("utf-8")).toBe("a\nb\n");
    expect(fileReads()).toHaveLength(0);
  });

  it("passes on the reason Studio refuses a replacement for", async () => {
    harness.mockHttp.onPut("/projects/p1/files/docs/x.md").reply(409, {
      message: "docs/x.md changed since it was read, on disk or through another client",
    });

    await expect(executeTool("write_project_file", {
      projectId: "p1",
      path: "docs/x.md",
      content: "v2",
      version: "v1",
    }, harness.client)).rejects.toThrow(/changed since it was read/);
  });

  it("refuses a conflict policy, which it no longer takes", async () => {
    await expect(executeTool("write_project_file", {
      projectId: "p1", path: "docs/x.md", content: "v2", conflictPolicy: "OVERWRITE",
    }, harness.client)).rejects.toThrow(/invalid arguments|unrecognized/i);
    expect(harness.mockHttp.history.post).toHaveLength(0);
  });
});

describe("delete, search, copy and move", () => {
  it("openl_delete_project_file deletes the version of a file it names and reports success", async () => {
    harness.mockHttp.onDelete("/projects/p1/files/old.txt").reply(204);

    const result = await executeTool("delete_project_file", {
      projectId: "p1",
      path: "old.txt",
      version: "v1",
    }, harness.client);

    expect(harness.mockHttp.history.delete[0].headers).toMatchObject({ "If-Match": '"v1"' });
    expect(dataOf(result)).toMatchObject({ success: true, path: "old.txt" });
    expect(dataOf(result).message).toContain("openl_save_project");
    expect(fileReads()).toHaveLength(0);
  });

  it.each([
    ["delete_project_file", "delete", { path: "old.txt", version: "v1" }],
    ["copy_project_file", "post", { sourcePath: "a.txt", destinationPath: "b.txt" }],
    ["move_project_file", "post", { sourcePath: "a.txt", destinationPath: "b.txt" }],
  ] as const)("openl_%s tells that a change of a project edited in place needs no commit", async (tool, method, args) => {
    harness.mockHttp.onDelete("/projects/p1/files/old.txt").reply(204);
    harness.mockHttp.onPost(/\/projects\/p1\/file-(copy|move)/).reply(201);
    editedInPlace();

    const result = await executeTool(tool, { projectId: "p1", ...args }, harness.client);

    expect(harness.mockHttp.history[method]).toHaveLength(1);
    expect(dataOf(result).message).toMatch(/The project is edited in place/);
    expect(dataOf(result).message).not.toContain("openl_save_project");
  });

  it("openl_delete_project_file refuses to delete a file without its version", async () => {
    answerMeta("old.txt", 3);

    await expect(executeTool("delete_project_file", { projectId: "p1", path: "old.txt" }, harness.client))
      .rejects.toThrow(/is a file: read it with openl_read_project_file/);
    expect(harness.mockHttp.history.delete).toHaveLength(0);
  });

  it.each([
    ["a folder path", "docs/", false],
    ["a path Studio has no file metadata for", "docs", true],
  ])("openl_delete_project_file deletes %s without a version", async (_case, path, asked) => {
    harness.mockHttp.onGet(`/projects/p1/files/${path}`).reply(404, { message: "Not found" });
    harness.mockHttp.onDelete(`/projects/p1/files/${path}`).reply(204);

    await executeTool("delete_project_file", { projectId: "p1", path }, harness.client);

    expect(harness.mockHttp.history.delete[0].headers).not.toHaveProperty("If-Match");
    expect(fileReads()).toHaveLength(asked ? 1 : 0);
  });

  it("openl_search_project_files builds the query body and returns matches", async () => {
    const nodes = [{ path: "rules/config.xml", name: "config.xml", type: "file" }];
    harness.mockHttp.onPost("/projects/p1/file-search").reply(200, nodes);

    const result = await executeTool("search_project_files", {
      projectId: "p1",
      pattern: "**/*.xml",
      content: "premium",
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
      projectId: "p1", pattern: "**/*.xlsx", limit: 2, offset: 0, }, harness.client)));
    const page3 = JSON.parse(textOf(await executeTool("search_project_files", {
      projectId: "p1", pattern: "**/*.xlsx", limit: 2, offset: 4, }, harness.client)));

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

    const result = await executeTool(tool, { projectId: "p1", ...paths }, harness.client);

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
