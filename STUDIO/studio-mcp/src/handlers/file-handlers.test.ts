/**
 * Integration tests of the project file tools of src/handlers/file-handlers.ts: reading, writing, deleting, searching,
 * copying and moving the files of a project.
 */

import { describe, it, expect } from "vitest";
import { executeTool, getAllTools } from "./index.js";
import { bodyOf, queryOf } from "../testing/mock-http.js";
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
  it("returns UTF-8 text content verbatim", async () => {
    answerFile("readme.md", "# Title\nbody", "text/markdown");

    const result = await executeTool("read_project_file", { projectId: "p1", path: "readme.md" }, harness.client);

    expect(textOf(result)).toBe("# Title\nbody");
  });

  it("keeps binary content in a lossless base64 envelope of one text block", async () => {
    const binary = Buffer.from([0x00, 0x01, 0x02, 0xff, 0x00]);
    answerFile("data.bin", binary);

    const result = await executeTool("read_project_file", {
      projectId: "p1",
      path: "data.bin",
    }, harness.client, { signal: new AbortController().signal });

    expect(dataOf(result)).toMatchObject({
      path: "data.bin",
      mimeType: "application/octet-stream",
      encoding: "base64",
      byteLength: 5,
      returnedBytes: 5,
      content: binary.toString("base64"),
    });
    expect(result.content).toHaveLength(1);
    expect(result.structuredContent).toBeUndefined();
  });

  it("returns binary-ish bytes as raw text when encoding='utf-8' is asked for", async () => {
    const bytes = Buffer.from([0x41, 0x00, 0x42]); // 'A', NUL, 'B' -> looksBinary true
    answerFile("data.bin", bytes);

    const result = await executeTool("read_project_file", { projectId: "p1", path: "data.bin", encoding: "utf-8" }, harness.client);

    expect(textOf(result)).toBe(bytes.toString("utf-8"));
  });

  it("tells text from binary by the share of control characters: 1/10 stays text, 2/10 becomes base64", async () => {
    const oneCtrl = Buffer.concat([Buffer.from("A".repeat(9)), Buffer.from([0x01])]);
    const twoCtrl = Buffer.concat([Buffer.from("A".repeat(8)), Buffer.from([0x01, 0x01])]);
    answerFile("a.txt", oneCtrl);
    answerFile("b.txt", twoCtrl);

    const textResult = await executeTool("read_project_file", { projectId: "p1", path: "a.txt" }, harness.client);
    const binResult = await executeTool("read_project_file", { projectId: "p1", path: "b.txt", response_format: "json" }, harness.client);

    expect(textOf(textResult)).toBe(oneCtrl.toString("utf-8"));
    expect(dataOf(binResult).encoding).toBe("base64");
  });

  it("applies a client-side byte range", async () => {
    answerFile("nums.txt", "0123456789", "text/plain");

    const result = await executeTool("read_project_file", {
      projectId: "p1", path: "nums.txt", offset: 2, length: 3,
    }, harness.client);

    expect(dataOf(result)).toEqual({
      path: "nums.txt", contentType: "text/plain", mimeType: "text/plain", encoding: "utf-8",
      byteLength: 10, returnedBytes: 3, range: { offset: 2, length: 3 }, nextOffset: 5, content: "234",
    });
  });

  it("names the line ends of a text chunk and no next offset at the end of the file (EPBDS-16856)", async () => {
    answerFile("i18n/message.properties", "#MasterOffer\r\nkey=value\r\n", "text/plain");

    const result = await executeTool("read_project_file", {
      projectId: "p1", path: "i18n/message.properties", offset: 14,
    }, harness.client);

    expect(dataOf(result)).toMatchObject({
      byteLength: 25, range: { offset: 14, length: 11 }, lineEnding: "CRLF", content: "key=value\r\n",
    });
    expect(dataOf(result)).not.toHaveProperty("nextOffset");
  });

  it("keeps the characters of a byte range whole", async () => {
    // "a" (1 byte), "é" (2 bytes), "€" (3 bytes), "b": bytes 2-3 cut both multi-byte characters
    answerFile("chars.txt", "aé€b", "text/plain");

    const result = await executeTool("read_project_file", {
      projectId: "p1", path: "chars.txt", offset: 2, length: 2,
    }, harness.client);

    expect(dataOf(result)).toMatchObject({ range: { offset: 3, length: 3 }, nextOffset: 6, content: "€" });
  });

  it("caps oversized text at 25K and appends a continuation cursor", async () => {
    answerFile("big.txt", "A".repeat(30000), "text/plain");

    const text = textOf(await executeTool("read_project_file", { projectId: "p1", path: "big.txt" }, harness.client));

    expect(text.startsWith("A".repeat(100))).toBe(true);
    // First 25000 chars of content + a continuation note pointing at the next byte offset.
    expect(text).toContain("continue with offset=25000");
    expect(text.length).toBeLessThan(25000 + 300);
  });

  it("does not truncate a large base64 binary payload", async () => {
    // 30 KB of NUL bytes -> base64 ~40 KB, well past the 25k markdown cap.
    answerFile("big.bin", Buffer.alloc(30000, 0));

    const data = dataOf(await executeTool("read_project_file", { projectId: "p1", path: "big.bin" }, harness.client));

    expect(data.encoding).toBe("base64");
    expect(data.byteLength).toBe(30000);
    expect(Buffer.from(data.content, "base64")).toHaveLength(30000);
  });

  it("returns a folder listing and the metadata of a file as JSON", async () => {
    harness.mockHttp.onGet("/projects/p1/files/").reply(200, [
      { path: "a.xlsx", name: "a.xlsx", type: "file" },
      { path: "sub", name: "sub", type: "folder" },
    ], { "content-type": "application/json" });
    harness.mockHttp.onGet("/projects/p1/files/a.xlsx").reply(200, { path: "a.xlsx", name: "a.xlsx", type: "file", size: 100 }, {
      "content-type": "application/json",
    });

    const listing = await executeTool("read_project_file", {
      projectId: "p1", path: "", recursive: true, response_format: "json",
    }, harness.client);
    const meta = await executeTool("read_project_file", {
      projectId: "p1", path: "a.xlsx", view: "meta", response_format: "json",
    }, harness.client);

    expect(dataOf(listing).map((node: { name: string }) => node.name)).toEqual(["a.xlsx", "sub"]);
    expect(dataOf(meta).size).toBe(100);
  });

  it("forwards version and branch, and download=true for a base64 ZIP of a folder", async () => {
    answerFile("a.xlsx", "x", "text/plain");
    const zip = Buffer.from("PK\x03\x04fakezip");
    harness.mockHttp.onGet("/projects/p1/files/rules/").reply(200, zip, {
      "content-type": "application/zip",
      "content-disposition": "attachment; filename=rules.zip",
    });

    await executeTool("read_project_file", { projectId: "p1", path: "a.xlsx", version: "rev1", branch: "dev" }, harness.client);
    const result = await executeTool("read_project_file", {
      projectId: "p1", path: "rules/", download: true, response_format: "json",
    }, harness.client);

    expect(harness.mockHttp.history.get[0].params).toMatchObject({ version: "rev1", branch: "dev" });
    expect(queryOf(harness.mockHttp.history.get[1]).download).toBe("true");
    expect(dataOf(result)).toMatchObject({ encoding: "base64", content: zip.toString("base64") });
  });
});

describe("openl_write_project_file", () => {
  it("decodes base64 and reports an uncommitted working-copy write", async () => {
    harness.mockHttp.onPost("/projects/p1/files/docs/new.md").reply(201);

    const result = await executeTool("write_project_file", {
      projectId: "p1",
      path: "docs/new.md",
      content: Buffer.from("hello").toString("base64"),
      encoding: "base64",
      response_format: "json",
    }, harness.client);

    expect(writtenBytes().toString("utf-8")).toBe("hello");
    // createFolders default (true) is materialized by the handler.
    expect(harness.mockHttp.history.post[0].params?.createFolders).toBe(true);
    expect(dataOf(result)).toMatchObject({ success: true, bytesWritten: 5, committed: false });
  });

  it("advertises and accepts a JSON Schema base64 blob", async () => {
    const tool = getAllTools().find((candidate) => candidate.name === "write_project_file");
    const alternatives = tool?.inputSchema.anyOf as Array<Record<string, unknown>>;
    expect(tool?.inputSchema.type).toBe("object");
    expect(alternatives).toHaveLength(3);

    const blobAlternative = alternatives.find((alternative) =>
      Object.hasOwn(alternative.properties as object, "blob"));
    const blobProperties = blobAlternative?.properties as Record<string, Record<string, unknown>>;
    expect(blobProperties.blob).toMatchObject({
      type: "string",
      contentEncoding: "base64",
      contentMediaType: "application/octet-stream",
    });
    expect(blobAlternative?.required).toEqual(expect.arrayContaining(["blob"]));
    expect(blobProperties).not.toHaveProperty("content");
    expect(alternatives.every((alternative) => alternative.additionalProperties === false)).toBe(true);

    harness.mockHttp.onPost("/projects/p1/files/rules/model.xlsx").reply(201);
    const bytes = Buffer.from([0x50, 0x4b, 0x03, 0x04]);
    await executeTool("write_project_file", {
      projectId: "p1",
      path: "rules/model.xlsx",
      blob: bytes.toString("base64"),
    }, harness.client);

    expect(writtenBytes()).toEqual(bytes);
  });

  it("accepts whitespace-wrapped legacy base64 content", async () => {
    harness.mockHttp.onPost("/projects/p1/files/rules/model.xlsx").reply(201);
    const bytes = Buffer.from([0x50, 0x4b, 0x03, 0x04, 0xff, 0x00]);
    const base64 = bytes.toString("base64");

    await executeTool("write_project_file", {
      projectId: "p1",
      path: "rules/model.xlsx",
      content: `${base64.slice(0, 4)}\n${base64.slice(4)}\n`,
      encoding: "base64",
    }, harness.client);

    expect(writtenBytes()).toEqual(bytes);
  });

  it.each([
    ["an invalid blob", { blob: "not base64!" }, /base64/i],
    ["invalid base64 content", { content: "not valid base64 @@@!!!", encoding: "base64" }, /base64/i],
    ["both content and blob", { content: "text", blob: Buffer.from("bytes").toString("base64") }, /invalid arguments/i],
    ["neither content nor blob", {}, /invalid arguments/i],
  ])("refuses %s before calling OpenL Studio", async (_case, args, message) => {
    await expect(executeTool("write_project_file", { projectId: "p1", path: "x.bin", ...args }, harness.client))
      .rejects.toThrow(message);
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

    expect(harness.mockHttp.history.patch.map(bodyOf)).toEqual([{ comment: "add docs/x.md" }]);
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

describe("openl_edit_project_file (EPBDS-16855)", () => {
  const bundle = "i18n/message.properties";
  const editFile = (edits: Array<{ oldText: string; newText: string }>, extra: object = {}) =>
    executeTool("edit_project_file", { projectId: "p1", path: bundle, edits, response_format: "json", ...extra }, harness.client);

  it("adds keys to a bundle whose lines end with CRLF, leaving every other byte as it is", async () => {
    answerFile(bundle, "#Bundle\r\na=1\r\nb=2\r\n", "text/plain");
    harness.mockHttp.onPost(`/projects/p1/files/${bundle}`).reply(204);

    const result = await editFile([
      { oldText: "a=1\n", newText: "a=1\nc=3\n" },
      { oldText: "", newText: "z=26\n" },
    ]);

    expect(writtenBytes().toString("utf-8")).toBe("#Bundle\r\na=1\r\nc=3\r\nb=2\r\nz=26\r\n");
    expect(harness.mockHttp.history.post[0].params).toMatchObject({ conflictPolicy: "OVERWRITE", createFolders: false });
    expect(dataOf(result)).toEqual({
      success: true, path: bundle, edits: 2, bytesBefore: 19, bytesAfter: 30, committed: false,
      message: `Applied 2 edit(s) to '${bundle}' in the project working copy. Changes are NOT committed — pass 'message' to commit, or use openl_save_project.`,
    });
  });

  it("commits the edit with a 'message'", async () => {
    answerFile(bundle, "a=1\n", "text/plain");
    harness.mockHttp.onPost(`/projects/p1/files/${bundle}`).reply(204);
    harness.mockHttp.onGet("/projects/p1").reply(200, { id: "p1", name: "P", status: "EDITING", repository: "design" });
    harness.mockHttp.onPatch("/projects/p1").reply(204);

    const result = await editFile([{ oldText: "a=1", newText: "a=2" }], { message: "Change a" });

    expect(writtenBytes().toString("utf-8")).toBe("a=2\n");
    expect(dataOf(result)).toMatchObject({
      committed: true,
      message: `Applied 1 edit(s) to '${bundle}' and committed the project to Git: "Change a".`,
    });
  });

  it.each([
    ["is not in the file", "x=9", /Edit 2: oldText is not in the file/],
    ["occurs more than once", "=", /Edit 2: oldText occurs more than once/],
  ])("writes nothing when an oldText %s", async (_case, oldText, message) => {
    answerFile(bundle, "a=1\nb=2\n", "text/plain");

    await expect(editFile([{ oldText: "a=1", newText: "a=2" }, { oldText, newText: "" }])).rejects.toThrow(message);
    expect(harness.mockHttp.history.post).toHaveLength(0);
  });

  it.each([
    ["a binary file", Buffer.from([0x50, 0x4b, 0x03, 0x04, 0x00, 0x00, 0xff]), "application/octet-stream", /not UTF-8 text/],
    ["a file that is not UTF-8", Buffer.from("caf\xe9 au lait", "latin1"), "text/plain", /not UTF-8 text/],
  ])("refuses %s", async (_case, body, contentType, message) => {
    answerFile(bundle, body, contentType);

    await expect(editFile([{ oldText: "", newText: "x" }])).rejects.toThrow(message);
    expect(harness.mockHttp.history.post).toHaveLength(0);
  });

  it("refuses a folder", async () => {
    harness.mockHttp.onGet("/projects/p1/files/i18n/").reply(200, [{ name: "message.properties" }], {
      "content-type": "application/json",
    });

    await expect(executeTool("edit_project_file", {
      projectId: "p1", path: "i18n/", edits: [{ oldText: "", newText: "x" }],
    }, harness.client)).rejects.toThrow(/is a folder/);
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
