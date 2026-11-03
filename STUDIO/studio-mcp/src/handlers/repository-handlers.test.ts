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

    const first = textOf(await executeTool("list_repositories", { limit: 1 }, harness.client));
    const second = textOf(await executeTool("list_repositories", { limit: 1, offset: 1, }, harness.client));

    expect(first.split("\n")[0]).toBe("2 repositories; 1–1 below, the next page at offset=1.");
    expect(first).toMatch(/\n\| design \| /);
    expect(second).toMatch(/\n\| production \| /);
  });

  it("openl_list_branches resolves the repository by its display name", async () => {
    harness.mockHttp.onGet("/repos").reply(200, mockRepositories);
    harness.mockHttp.onGet("/repos/design/branches").reply(200, ["main", "development"]);

    const text = textOf(await executeTool("list_branches", { repository: "Design Repository" }, harness.client));

    expect(text).toContain("main");
    expect(text).toContain("development");
  });

  it("openl_repository_project_revisions reads a branch and uses the current revision total to derive more pages", async () => {
    const revision = (revisionNo: string, fullComment?: string) => ({
      revisionNo, createdAt: "2026-01-01T00:00:00Z", fullComment, deleted: false, technicalRevision: false,
    });
    harness.mockHttp.onGet(historyPath, { params: { branch: "feature", techRevs: false, offset: 0, size: 3 } }).reply(200, {
      // Studio sends the commit that deleted a project without a comment
      content: [
        revision("abc123", "First\n\nDetail"),
        { ...revision("def456", "Second"), technicalRevision: true },
        { ...revision("fed789"), deleted: true },
      ],
      pageNumber: 0,
      pageSize: 3,
      numberOfElements: 3,
      total: 4,
    });

    const result = await executeTool("repository_project_revisions", {
      projectId,
      branch: "feature",
      limit: 3,
    }, harness.client);

    // A revision is named by its first line of comment; the flags that are false are left out
    expect(textOf(result)).toBe("4 revisions; 1–3 below, the next page at offset=3.\n\n"
      + "| revision | date | author | comment | flags |\n| --- | --- | --- | --- | --- |\n"
      + "| abc123 | 2026-01-01T00:00:00Z |  | First |  |\n| def456 | 2026-01-01T00:00:00Z |  | Second | technical |\n"
      + "| fed789 | 2026-01-01T00:00:00Z |  |  | deleted |");
  });

  it("openl_repository_project_revision_changes pages the files a revision changed, telling the two revisions", async () => {
    const revision = (revisionNo: string, fullComment: string) => ({
      revisionNo, shortRevisionNo: revisionNo, createdAt: "2026-01-01T00:00:00Z", fullComment,
      author: { displayName: "Ada Admin" }, deleted: false, technicalRevision: false,
    });
    harness.mockHttp.onGet(`${historyPath}/changes`, { params: { revision: "def456", base: "abc123" } }).reply(200, {
      base: revision("abc123", "Before"),
      revision: { ...revision("def456", "After\n\nDetail"), technicalRevision: true },
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

    // The revisions compared follow the files, in the columns the history lists a revision by.
    expect(textOf(result)).toBe("3 files; 2–3 below.\n\n"
      + "| path | change |\n| --- | --- |\n| notes.txt | deleted |\n| reviews/new.txt | added |\n\n"
      + "| compared | revision | date | author | comment | flags |\n| --- | --- | --- | --- | --- | --- |\n"
      + "| base | abc123 | 2026-01-01T00:00:00Z | Ada Admin | Before |  |\n"
      + "| revision | def456 | 2026-01-01T00:00:00Z | Ada Admin | After | technical |");
  });

  it("openl_repository_project_revision_changes lists the files the working copy changed since it was saved", async () => {
    harness.mockHttp.onGet(`/projects/${encodeURIComponent(projectId)}`).reply(200, { id: projectId, revision: "abc123" });
    harness.mockHttp.onGet(`${historyPath}/changes`, { params: { revision: undefined, base: "abc123" } }).reply(200, {
      base: {
        revisionNo: "abc123", createdAt: "2026-01-01T00:00:00Z", fullComment: "Before",
        author: { displayName: "Ada Admin" }, deleted: false, technicalRevision: false,
      },
      // Studio leaves the list out when no file changed; one changed here, the empty answer is read below
      files: [{ path: "rules/Main.xlsx", type: "modified" }],
    });
    harness.mockHttp.onGet(`${historyPath}/changes`, { params: { revision: undefined, base: "saved" } }).reply(200, {
      base: { revisionNo: "saved", createdAt: "", deleted: false, technicalRevision: false },
    });

    const none = await executeTool("repository_project_revision_changes", { projectId, base: "saved" }, harness.client);
    expect(textOf(none)).toMatch(/^No files\.|^0 files/);
    const result = await executeTool("repository_project_revision_changes", { projectId }, harness.client);

    // What a save would commit: the working copy against the revision it was saved at, named where the later
    // revision stands, with nothing else told of it.
    expect(textOf(result)).toMatch(/\| revision \| working copy \|  \|  \|  \|  \|$/);
    expect(textOf(result)).toMatch(/^1 file\./);
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
      limit: 50,
    }, harness.client);

    expect(textOf(result)).toBe("25 revisions; none from offset=25.");
  });
});
