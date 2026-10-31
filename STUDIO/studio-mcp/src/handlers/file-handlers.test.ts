/**
 * Integration tests of the project file tools of src/handlers/file-handlers.ts: reading, writing, deleting, searching,
 * copying and moving the files of a project.
 */

import { createHash } from "node:crypto";
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

  it("keeps the byte order mark of a text, which a write back then keeps too", async () => {
    answerMeta("data.csv", 4);
    answerFile("data.csv", Buffer.from([0xef, 0xbb, 0xbf, 0x61]), "text/plain");

    const result = await executeTool("read_project_file", { projectId: "p1", path: "data.csv" }, harness.client);

    expect(textOf(result)).toBe("\uFEFFa");
  });

  it("answers a text whose bytes its charset does not decode by a link, not as replacement characters", async () => {
    answerMeta("legacy.txt", 4);
    answerFile("legacy.txt", Buffer.from([0x63, 0x61, 0x66, 0xe9]), "text/plain");
    singleUser();

    const result = await executeTool("read_project_file", { projectId: "p1", path: "legacy.txt" }, harness.client);

    expect(textOf(result)).not.toContain("\uFFFD");
    expect(dataOf<{ curl: string }>(result).curl).toContain("legacy.txt");
  });

  it("answers a larger text with the address of the REST API in single-user mode, reading no byte of it", async () => {
    answerMeta("rules.xml", 5001);
    singleUser();

    const result = await executeTool("read_project_file", {
      projectId: "p1", path: "rules.xml", branch: "feature",
    }, harness.client);

    expect(dataOf(result)).toEqual({
      url: "http://localhost:8080/rest/projects/p1/files/rules.xml?branch=feature",
      size: 5001,
      version: "v1",
      repositoryId: "design",
      repositoryPath: "Bank Rating/rules.xml",
      branch: "feature",
      curl: "curl -fsS -o 'rules.xml' 'http://localhost:8080/rest/projects/p1/files/rules.xml?branch=feature'",
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
      projectId: "p1", path: "rules/Model.xlsx", revision: "r1",
    }, harness.client);

    expect(bodyOf(harness.mockHttp.history.post[0])).toEqual({
      kind: "download", projectId: "p1", path: "rules/Model.xlsx", version: "r1",
    });
    expect(dataOf(result)).toMatchObject({
      url: "http://localhost:8080/transfers/k3Jx",
      expiresAt: "2026-10-23T10:15:00Z",
      size: 4,
      repositoryPath: "Bank Rating/rules/Model.xlsx",
      branch: "main",
      curl: "curl -fsS -o 'Model.xlsx' 'http://localhost:8080/transfers/k3Jx'",
    });
    // The extension of the file tells its type
    expect(dataOf(result)).not.toHaveProperty("mimeType");
    // The content of an earlier commit replaces nothing
    expect(dataOf(result)).not.toHaveProperty("version");
  });

  it("answers the text of an earlier commit without a version to replace the file by", async () => {
    answerMeta("readme.md", 4);
    answerFile("readme.md", Buffer.from("old\n"), "text/markdown");

    const result = await executeTool("read_project_file", { projectId: "p1", path: "readme.md", revision: "r1" },
      harness.client);

    expect(result.content).toEqual([{ type: "text", text: "old\n" }]);
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
      expiresAt: "2026-10-23T10:15:00Z",
      repositoryId: "design-flat",
      repositoryPath: "rules/Model.xlsx",
      curl: "curl -sS --fail-with-body -T 'Model.xlsx' 'http://localhost:8080/transfers/up'",
    });
    expect(dataOf(result)).not.toHaveProperty("note");
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
      branch: "main",
      curl: String.raw`curl -sS --fail-with-body --data-binary @'it'\''s new.md' -H 'Content-Type: application/octet-stream' `
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
      curl: `curl -sS --fail-with-body -X PUT -H 'If-Match: "v1"' --data-binary @'Model.xlsx' -H 'Content-Type: application/octet-stream' '${url}'`,
    });
  });

  it("answers an upload link into a project edited in place as any other, with no note", async () => {
    harness.mockHttp.onGet("/settings").reply(200, {});
    editedInPlace();

    const result = await executeTool("create_upload_link", { projectId: "p1", path: "rules/Model.xlsx" },
      harness.client);

    expect(dataOf(result)).toHaveProperty("curl");
    expect(dataOf(result)).not.toHaveProperty("note");
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
    // The version of the new file is the digest of its content, which the next replacement names
    expect(dataOf(result)).toEqual({
      action: "created", version: createHash("sha256").update("héllo").digest("hex"), committed: false,
    });
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

    expect(dataOf(result)).toMatchObject({ committed: false });
    expect(harness.mockHttp.history.patch).toHaveLength(0);
  });

  it("reports a write to a closed project as committed, which Studio commits itself", async () => {
    harness.mockHttp.onPost("/projects/p1/files/docs/x.md").reply(201);
    harness.mockHttp.onGet("/projects/p1").reply(200, { id: "p1", name: "P", status: "CLOSED", repository: "design" });

    const result = await executeTool("write_project_file", {
      projectId: "p1",
      path: "docs/x.md",
      content: "hello",
    }, harness.client);

    expect(harness.mockHttp.history.patch).toHaveLength(0);
    expect(dataOf(result)).toMatchObject({ action: "created", committed: true });
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
    expect(dataOf(result)).toMatchObject({ action: "created", committed: false });
    expect(dataOf(result)).not.toHaveProperty("note");
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
    answerMeta("docs/x.md", 2);

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
    expect(dataOf(result)).toMatchObject({ action: "overwritten" });
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
  it("openl_delete_project_file deletes the version of a file it names and names it", async () => {
    harness.mockHttp.onDelete("/projects/p1/files/old.txt").reply(204);

    const result = await executeTool("delete_project_file", {
      projectId: "p1",
      path: "old.txt",
      version: "v1",
    }, harness.client);

    expect(harness.mockHttp.history.delete[0].headers).toMatchObject({ "If-Match": '"v1"' });
    expect(dataOf(result)).toEqual({ deleted: "old.txt" });
    expect(fileReads()).toHaveLength(0);
  });

  it.each([
    ["delete_project_file", "delete", { path: "old.txt", version: "v1" }],
    ["copy_project_file", "post", { sourcePath: "a.txt", destinationPath: "b.txt" }],
    ["move_project_file", "post", { sourcePath: "a.txt", destinationPath: "b.txt" }],
  ] as const)("openl_%s answers what it changed with no request for the project", async (tool, method, args) => {
    harness.mockHttp.onDelete("/projects/p1/files/old.txt").reply(204);
    harness.mockHttp.onPost(/\/projects\/p1\/file-(copy|move)/).reply(201);
    editedInPlace();

    const result = await executeTool(tool, { projectId: "p1", ...args }, harness.client);

    expect(harness.mockHttp.history[method]).toHaveLength(1);
    // Whether the project is edited in place, which the description tells, needs no read of it
    expect(harness.mockHttp.history.get.filter((request) => request.url === "/projects/p1")).toHaveLength(0);
    expect(Object.keys(dataOf(result))).toEqual([tool === "delete_project_file" ? "deleted" : `${method === "post" && tool.startsWith("copy") ? "copied" : "moved"}`]);
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
    harness.mockHttp.onPost("/projects/p1/file-search", expect.objectContaining({ content: "premium" }))
      .reply(200, nodes);
    harness.mockHttp.onPost("/projects/p1/file-search", expect.objectContaining({ type: "FILE" })).reply(200, []);

    const result = await executeTool("search_project_files", {
      projectId: "p1",
      pattern: "**/*.xml",
      content: "premium",
    }, harness.client);

    expect(bodyOf(harness.mockHttp.history.post[0])).toEqual({ pattern: "**/*.xml", content: "premium" });
    expect(textOf(result)).toBe("1 match.\n\n| path | type | size | lastModified |\n| --- | --- | --- | --- |\n"
      + "| rules/config.xml | file |  |  |");
  });

  it("openl_search_project_files counts the workbooks a content search covers but cannot read", async () => {
    harness.mockHttp.onPost("/projects/p1/file-search", expect.objectContaining({ content: "Vallidaiton" }))
      .reply(200, [{ path: "i18n/message.properties", type: "file" }]);
    harness.mockHttp.onPost("/projects/p1/file-search", expect.objectContaining({ type: "FILE" }))
      .reply(200, [{ path: "rules/Main.xlsx" }, { path: "rules/Lookups.xlsx" }]);

    const result = await executeTool("search_project_files", {
      projectId: "p1",
      content: "Vallidaiton",
      extensions: ["XLSX", "properties"],
      recursive: true,
    }, harness.client);

    const [search, workbooks] = harness.mockHttp.history.post;
    expect(bodyOf(workbooks)).toEqual({ extensions: ["xlsx"], type: "FILE", recursive: true });
    expect(workbooks.params).toEqual({ fields: "path" });
    expect(bodyOf(search)).toEqual({ content: "Vallidaiton", extensions: ["XLSX", "properties"], recursive: true });
    expect(textOf(result)).toMatch(/\n\nNot searched: 2 workbooks: content is never matched .*openl_list_tables text\.$/);
  });

  it.each([
    ["no workbook extension", { extensions: ["properties"] }],
    ["an ANCESTORS search", { scope: "ANCESTORS", from: "rules" }],
    ["a folder search", { type: "FOLDER" }],
  ])("openl_search_project_files looks for no workbook when a content search names %s", async (_case, args) => {
    harness.mockHttp.onPost("/projects/p1/file-search").reply(200, []);

    const result = await executeTool("search_project_files", { projectId: "p1", content: "rate", ...args },
      harness.client);

    expect(harness.mockHttp.history.post).toHaveLength(1);
    expect(textOf(result)).not.toContain("Not searched");
  });

  it("openl_search_project_files forwards extensions/type/scope=ANCESTORS/recursive/from/revision", async () => {
    harness.mockHttp.onPost("/projects/p1/file-search").reply(200, []);
    const query = {
      extensions: ["xlsx", "xml"],
      type: "FOLDER",
      scope: "ANCESTORS",
      recursive: true,
      from: "rules",
    };

    await executeTool("search_project_files", { projectId: "p1", ...query, revision: "abc123" }, harness.client);

    expect(bodyOf(harness.mockHttp.history.post[0])).toEqual({ ...query, version: "abc123" });
  });

  it.each([
    [{ from: "rules" }, { pattern: "rules/*", recursive: true }],
    [{ from: "/rules/", recursive: true }, { pattern: "rules/**", recursive: true }],
    [{ from: "rules", pattern: "*.xlsx" }, { pattern: "rules/*.xlsx", recursive: true }],
    [{ from: "rules", pattern: "*.xlsx", recursive: true }, { pattern: "rules/**/*.xlsx", recursive: true }],
  ])("openl_search_project_files searches %j under the folder, which Studio does not start a SUBTREE search from",
    async (args, body) => {
      harness.mockHttp.onPost("/projects/p1/file-search").reply(200, []);

      await executeTool("search_project_files", { projectId: "p1", ...args }, harness.client);

      expect(bodyOf(harness.mockHttp.history.post[0])).toEqual(body);
    });

  it("openl_search_project_files from a folder without recursive answers the entries of that folder only", async () => {
    harness.mockHttp.onPost("/projects/p1/file-search").reply(200, [
      { path: "rules/Main.xlsx", name: "Main.xlsx", type: "file" },
      { path: "rules/old/Old.xlsx", name: "Old.xlsx", type: "file" },
      { path: "rules/old", name: "old", type: "folder" },
    ]);

    const result = await executeTool("search_project_files", {
      projectId: "p1", from: "rules", pattern: "**/*", fields: "path",
    }, harness.client);

    expect(bodyOf(harness.mockHttp.history.post[0])).toEqual({ pattern: "rules/**/*", recursive: true });
    expect(textOf(result)).toBe("2 matches.\n\n| path |\n| --- |\n| rules/Main.xlsx |\n| rules/old |");
  });

  it("openl_search_project_files paginates the match set client-side (limit/offset)", async () => {
    const nodes = Array.from({ length: 5 }, (_, i) => ({ path: `f${i}.xlsx`, name: `f${i}.xlsx`, type: "file" }));
    harness.mockHttp.onPost("/projects/p1/file-search").reply(200, nodes);

    const page1 = textOf(await executeTool("search_project_files", {
      projectId: "p1", pattern: "**/*.xlsx", limit: 2, offset: 0, fields: "name", }, harness.client));
    const page3 = textOf(await executeTool("search_project_files", {
      projectId: "p1", pattern: "**/*.xlsx", limit: 2, offset: 4, fields: "name", }, harness.client));

    expect(page1).toBe("5 matches; 1–2 below, the next page at offset=2.\n\n| name |\n| --- |\n| f0.xlsx |\n| f1.xlsx |");
    expect(page3).toBe("5 matches; 5–5 below.\n\n| name |\n| --- |\n| f4.xlsx |");
  });

  it.each([
    ["copy_project_file", "file-copy", 201],
    ["move_project_file", "file-move", 204],
  ])("openl_%s sends the paths and reports them", async (tool, endpoint, status) => {
    harness.mockHttp.onPost(`/projects/p1/${endpoint}`).reply(status);
    const paths = { sourcePath: "rules/M.xlsx", destinationPath: "legacy/M.xlsx" };

    const result = await executeTool(tool, { projectId: "p1", ...paths }, harness.client);

    expect(bodyOf(harness.mockHttp.history.post[0])).toEqual(paths);
    expect(dataOf(result)).toEqual({ [tool.startsWith("copy") ? "copied" : "moved"]: "legacy/M.xlsx" });
  });

  it.each([
    ["copy_project_file", "file-copy"],
    ["move_project_file", "file-move"],
  ])("openl_%s explains a destination already there", async (tool, endpoint) => {
    harness.mockHttp.onPost(`/projects/p1/${endpoint}`).reply(409, { message: "Conflict" });
    answerMeta("b.xlsx", 2);

    await expect(executeTool(tool, {
      projectId: "p1",
      sourcePath: "a.xlsx",
      destinationPath: "b.xlsx",
    }, harness.client)).rejects.toThrow(/to 'b\.xlsx': a file already exists there/);
  });

  it("passes on the reason Studio gives a conflict when no file is at the destination", async () => {
    harness.mockHttp.onPost("/projects/p1/file-copy").reply(409, { message: "The project is locked by another user." });
    harness.mockHttp.onGet("/projects/p1/files/b.xlsx").reply(404, { message: "Not found" });

    await expect(executeTool("copy_project_file", {
      projectId: "p1", sourcePath: "a.xlsx", destinationPath: "b.xlsx",
    }, harness.client)).rejects.toThrow(/locked by another user/);
  });

  it("names the path of a file a read names with the folder of the project before it", async () => {
    harness.mockHttp.onGet("/projects/p1/files/Rating/i18n/message.properties").reply(404, { message: "Not found" });
    harness.mockHttp.onGet("/projects/p1/files/i18n/message.properties").reply(200, { path: "i18n/message.properties", type: "file" });

    await expect(executeTool("read_project_file", { projectId: "p1", path: "Rating/i18n/message.properties" }, harness.client))
      .rejects.toThrow("No file is at 'Rating/i18n/message.properties': a path is relative to the folder of the project, "
        + "and the file is 'i18n/message.properties'.");
  });

  it("answers a path no file takes with the slash a folder needs", async () => {
    harness.mockHttp.onGet("/projects/p1/files/rules").reply(404, { message: "Not found" });

    await expect(executeTool("read_project_file", { projectId: "p1", path: "rules" }, harness.client))
      .rejects.toThrow(/No file is at 'rules'.*'rules\/'/);
  });

  it("answers an ANCESTORS match larger than an inline text without its content", async () => {
    harness.mockHttp.onPost("/projects/p1/file-search").reply(200, [
      { path: "AGENTS.md", name: "AGENTS.md", type: "file", content: "short" },
      { path: "../AGENTS.md", name: "AGENTS.md", type: "file", content: "x".repeat(6_000) },
      // 2,000 characters of 3 bytes each
      { path: "../../AGENTS.md", name: "AGENTS.md", type: "file", content: "規".repeat(2_000) },
    ]);

    const result = await executeTool("search_project_files", {
      projectId: "p1", scope: "ANCESTORS", from: "rules", pattern: "AGENTS.md",
    }, harness.client);

    expect(textOf(result)).toBe([
      "## AGENTS.md\n\nshort",
      "## ../AGENTS.md\n\nThe content is over 5000 bytes: read it with openl_read_project_file.",
      "## ../../AGENTS.md\n\nThe content is over 5000 bytes: read it with openl_read_project_file.",
    ].join("\n\n"));
  });
});
