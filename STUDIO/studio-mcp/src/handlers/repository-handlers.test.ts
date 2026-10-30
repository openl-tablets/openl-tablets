/**
 * Integration tests of the repository tools of src/handlers/repository-handlers.ts: the design repositories, their
 * branches and features, and the revision history of a project.
 */

import { describe, it, expect } from "vitest";
import { executeTool } from "./index.js";
import { mockRepositories } from "../testing/openl-api-mocks.js";
import { harness, textOf, useToolHarness } from "../testing/harness.js";

useToolHarness();

const projectId = "design:insurance-rules:hash123";
const historyPath = `/projects/${encodeURIComponent(projectId)}/history`;

describe("repository tools", () => {
  it("openl_list_repositories pages the repositories", async () => {
    harness.mockHttp.onGet("/repos").reply(200, mockRepositories);

    const first = JSON.parse(textOf(await executeTool("list_repositories", { limit: 1 }, harness.client)));
    const second = JSON.parse(textOf(await executeTool("list_repositories", {
      limit: 1, offset: 1, }, harness.client)));

    expect(first.data.map((repository: { id: string }) => repository.id)).toEqual(["design"]);
    expect(first.pagination).toMatchObject({ has_more: true, next_offset: 1, total_count: 2 });
    expect(second.data.map((repository: { id: string }) => repository.id)).toEqual(["production"]);
  });

  it("openl_list_branches resolves the repository by its display name", async () => {
    harness.mockHttp.onGet("/repos").reply(200, mockRepositories);
    harness.mockHttp.onGet("/repos/design/branches").reply(200, ["main", "development"]);

    const text = textOf(await executeTool("list_branches", { repository: "Design Repository" }, harness.client));

    expect(text).toContain("main");
    expect(text).toContain("development");
  });

  it("openl_repository_project_revisions uses the current revision total to derive more pages", async () => {
    const revision = (revisionNo: string, fullComment: string) => ({
      revisionNo, createdAt: "2026-01-01T00:00:00Z", fullComment, deleted: false, technicalRevision: false,
    });
    harness.mockHttp.onGet(historyPath, { params: { techRevs: false, page: 0, size: 2 } }).reply(200, {
      content: [revision("abc123", "First"), revision("def456", "Second")],
      pageNumber: 0,
      pageSize: 2,
      numberOfElements: 2,
      total: 4,
    });

    const result = await executeTool("repository_project_revisions", {
      projectId,
      page: 0,
      size: 2,
    }, harness.client);

    expect(JSON.parse(textOf(result)).pagination).toEqual({
      limit: 2,
      offset: 0,
      has_more: true,
      next_offset: 2,
      total_count: 4,
    });
  });

  it("openl_repository_project_revision_changes pages the files a revision changed, telling the two revisions", async () => {
    const revision = (revisionNo: string, fullComment: string) => ({
      revisionNo, createdAt: "2026-01-01T00:00:00Z", fullComment, author: { displayName: "Ada Admin" },
    });
    harness.mockHttp.onGet(`${historyPath}/changes`, { params: { revision: "def456", base: "abc123" } }).reply(200, {
      base: { ...revision("abc123", "Before"), shortRevisionNo: "abc123", deleted: false, technicalRevision: false },
      revision: { ...revision("def456", "After"), shortRevisionNo: "def456", deleted: false, technicalRevision: true },
      files: [
        { path: "rules/Main.xlsx", type: "modified" },
        { path: "notes.txt", type: "deleted" },
        { path: "reviews/new.txt", type: "added" },
      ],
    });

    const result = await executeTool("repository_project_revision_changes", {
      projectId,
      revision: "def456",
      base: "abc123",
      limit: 2,
      offset: 1,
    }, harness.client);

    // The revisions are answered as the history answers them: no short revision, no flag that is false.
    expect(JSON.parse(textOf(result))).toEqual({
      base: revision("abc123", "Before"),
      revision: { ...revision("def456", "After"), technicalRevision: true },
      data: [{ path: "notes.txt", type: "deleted" }, { path: "reviews/new.txt", type: "added" }],
      pagination: { limit: 2, offset: 1, has_more: false, total_count: 3 },
    });
  });

  it("openl_repository_project_revisions preserves a non-page-aligned offset in the response metadata", async () => {
    harness.mockHttp.onGet(historyPath, { params: { techRevs: false, offset: 25, size: 50 } }).reply(200, {
      content: [],
      pageNumber: 0,
      pageSize: 50,
      numberOfElements: 0,
      total: 25,
    });

    const result = await executeTool("repository_project_revisions", {
      projectId,
      offset: 25,
      size: 50,
    }, harness.client);

    expect(JSON.parse(textOf(result)).pagination).toMatchObject({
      limit: 50,
      offset: 25,
      total_count: 25,
      has_more: false,
    });
  });
});
