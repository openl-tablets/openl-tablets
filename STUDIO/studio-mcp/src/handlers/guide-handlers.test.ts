/**
 * Integration tests of the guidance tools of src/handlers/guide-handlers.ts: the onboarding, the user guides OpenL Studio
 * serves at `/docs`, and the AGENTS.md files that apply to a project.
 */

import { describe, it, expect, beforeEach } from "vitest";
import { executeTool } from "./index.js";
import { GUIDE_IDS, GUIDE_PAGES, mockUserGuides } from "../testing/user-guides-fixture.js";
import { harness, textOf, useToolHarness } from "../testing/harness.js";

useToolHarness();

describe("guidance tools", () => {
  // The guide tools read the user guides OpenL Studio serves at /docs.
  beforeEach(() => mockUserGuides(harness.mockHttp));

  it("openl_get_started returns the workflow protocol plus a documentation orientation, not an index dump", async () => {
    const result = await executeTool("get_started", {}, harness.client);

    const text = textOf(result);
    // The protocol must direct the agent to the other guidance tools.
    expect(text).toContain("openl_get_project_agent_context");
    expect(text).toContain("openl_list_guides");
    expect(text).toContain("openl_get_guides");
    // ...and to the skills it serves, for clients without the skills extension.
    expect(text).toContain("## Skills");
    expect(text).toContain("`skill://planning/SKILL.md`");
    // Orientation reflects the documents of OpenL Studio (count, spec ids, section groups)...
    expect(text).toContain("OpenL Studio serves 3 documents");
    expect(text).toContain(GUIDE_IDS.rulesXml);
    expect(text).toContain("Introduction (1)");
    // ...but stays an orientation: guide ids/titles are NOT dumped.
    expect(text).not.toContain(GUIDE_IDS.smartRules);
  });

  it("openl_list_guides returns filterable metadata only (no bodies)", async () => {
    const result = await executeTool("list_guides", {
      type: "guide",
      search: "smart",
    }, harness.client);

    const parsed = JSON.parse(textOf(result));
    expect(parsed.data).toHaveLength(1);
    const path = "reference-guide/02-working-with-openl-tables/01-table-types/02-decision-table/03-smart-rules-tables.md";
    // The id tells the type, and openl_get_guides the path
    expect(parsed.data[0]).toEqual({
      id: GUIDE_IDS.smartRules,
      title: "Smart Rules Table",
      size: Buffer.byteLength(GUIDE_PAGES[path]),
    });
    expect(parsed.pagination).toMatchObject({ total_count: 1 });
  });

  it("openl_get_guides returns the full bodies for the requested ids", async () => {
    const result = await executeTool("get_guides", {
      ids: [GUIDE_IDS.rulesXml, GUIDE_IDS.basicConcepts],
    }, harness.client);

    const text = textOf(result);
    expect(text).toContain("**Guide `spec/rules.xml`** (path: docs/ref/rules.xml.md)");
    expect(text).toContain("Every project has one. See [Table Types](table-types.md).");
    expect(text).toContain("![A table](../ref-guide-images/table.png)");
  });

  it("openl_get_guides fails actionably on unknown ids instead of falling back to the index", async () => {
    await expect(
      executeTool("get_guides", { ids: ["spec/rules.xml", "guide/no-such-doc"] }, harness.client),
    ).rejects.toThrow(/Unknown guide id\(s\): guide\/no-such-doc.*openl_list_guides/);
  });

  it("openl_get_project_agent_context aggregates the AGENTS.md files root-first", async () => {
    harness.mockHttp.onPost(/\/projects\/.*\/file-search/).reply(200, [
      { path: "foo/P1/AGENTS.md", name: "AGENTS.md", type: "file", basePath: "foo/P1", content: "project guidance" },
      { path: "foo/AGENTS.md", name: "AGENTS.md", type: "file", basePath: "foo", content: "root guidance" },
    ]);

    const result = await executeTool("get_project_agent_context", {
      projectId: "design-P1",
    }, harness.client);

    const text = textOf(result);
    expect(text).toContain("*The AGENTS.md files that apply");
    // Root file precedes the project file (root-first, project last = highest priority).
    expect(text.indexOf("## /foo/AGENTS.md")).toBeLessThan(text.indexOf("## /foo/P1/AGENTS.md"));
    expect(text.indexOf("root guidance")).toBeLessThan(text.indexOf("project guidance"));
    // No guide ids mentioned → no cross-reference section.
    expect(text).not.toContain("### Referenced guides");
  });

  it("openl_get_project_agent_context lists the guide ids the AGENTS.md files reference", async () => {
    harness.mockHttp.onPost(/\/projects\/.*\/file-search/).reply(200, [
      {
        path: "foo/P1/AGENTS.md", name: "AGENTS.md", type: "file", basePath: "foo/P1",
        content: `Datatypes are documented in ${GUIDE_IDS.basicConcepts}; also see spec/rules.xml.`,
      },
    ]);

    const result = await executeTool("get_project_agent_context", {
      projectId: "design-P1",
    }, harness.client);

    const text = textOf(result);
    expect(text).toContain("### Referenced guides");
    expect(text).toContain("openl_get_guides");
    // Both referenced ids listed, in index order (spec first in the fixture).
    expect(text.indexOf("- `spec/rules.xml`")).toBeLessThan(
      text.indexOf(`- \`${GUIDE_IDS.basicConcepts}\``),
    );
  });

  it("openl_get_project_agent_context returns a 'no files' note when the project has no AGENTS.md", async () => {
    harness.mockHttp.onPost(/\/projects\/.*\/file-search/).reply(200, []);

    const result = await executeTool("get_project_agent_context", {
      projectId: "design-P2",
    }, harness.client);

    expect(textOf(result)).toBe("No AGENTS.md files apply to this project.");
  });
});
