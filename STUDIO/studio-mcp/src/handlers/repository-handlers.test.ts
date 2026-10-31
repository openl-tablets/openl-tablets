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

  it("openl_repository_project_revisions uses the current revision total to derive more pages", async () => {
    const revision = (revisionNo: string, fullComment: string) => ({
      revisionNo, createdAt: "2026-01-01T00:00:00Z", fullComment, deleted: false, technicalRevision: false,
    });
    harness.mockHttp.onGet(historyPath, { params: { techRevs: false, offset: 0, size: 2 } }).reply(200, {
      content: [revision("abc123", "First\n\nDetail"), { ...revision("def456", "Second"), technicalRevision: true }],
      pageNumber: 0,
      pageSize: 2,
      numberOfElements: 2,
      total: 4,
    });

    const result = await executeTool("repository_project_revisions", {
      projectId,
      limit: 2,
    }, harness.client);

    // A revision is named by its first line of comment; the flags that are false are left out
    expect(textOf(result)).toBe("4 revisions; 1–2 below, the next page at offset=2.\n\n"
      + "| revision | date | author | comment | flags |\n| --- | --- | --- | --- | --- |\n"
      + "| abc123 | 2026-01-01T00:00:00Z |  | First |  |\n| def456 | 2026-01-01T00:00:00Z |  | Second | technical |");
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
