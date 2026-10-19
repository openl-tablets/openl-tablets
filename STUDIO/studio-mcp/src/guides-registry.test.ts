/**
 * Unit tests for the guide registry: the documents of the table of contents OpenL Studio serves, their filtering, the
 * bodies read verbatim with unknown-id reporting, free-text id scanning, and the get_started overview.
 */

import { describe, it, expect, beforeEach } from "vitest";

import { OpenLClient } from "./client.js";
import {
  findKnownGuideIds,
  guidesOverview,
  listGuides,
  readGuideBodies,
} from "./guides-registry.js";
import { MockHttp } from "./testing/mock-http.js";
import { GUIDE_IDS, GUIDE_PAGES, mockUserGuides } from "./testing/user-guides-fixture.js";

let client: OpenLClient;
let mockHttp: MockHttp;

beforeEach(() => {
  client = new OpenLClient({ baseUrl: "http://localhost:8080/studio" });
  mockHttp = new MockHttp(client);
  mockUserGuides(mockHttp);
});

describe("listGuides", () => {
  it("lists the specifications, then the reference guide, leaving out folder indexes and other guides", async () => {
    expect(await listGuides(client)).toEqual([
      {
        id: GUIDE_IDS.rulesXml,
        type: "specification",
        title: "Rules Project Descriptor (rules.xml)",
        path: "docs/ref/rules.xml.md",
        size: Buffer.byteLength(GUIDE_PAGES["ref/rules.xml.md"]),
      },
      {
        id: GUIDE_IDS.basicConcepts,
        type: "guide",
        title: "Basic Concepts",
        path: "docs/reference-guide/01-introduction/03-basic-concepts.md",
        size: Buffer.byteLength(GUIDE_PAGES["reference-guide/01-introduction/03-basic-concepts.md"]),
      },
      expect.objectContaining({ id: GUIDE_IDS.smartRules, title: "Smart Rules Table" }),
    ]);
  });

  it("reads only the table of contents of OpenL Studio, beside its REST API, to list", async () => {
    await listGuides(client);
    expect(mockHttp.history.get.map((request) => request.url)).toEqual(["../docs/toc.json"]);
  });

  it("filters by type and by a case-insensitive search over id and title", async () => {
    expect((await listGuides(client, { type: "specification" })).map((g) => g.id)).toEqual([GUIDE_IDS.rulesXml]);
    expect((await listGuides(client, { search: "SMART" })).map((g) => g.id)).toEqual([GUIDE_IDS.smartRules]);
    expect((await listGuides(client, { search: "descriptor" })).map((g) => g.id)).toEqual([GUIDE_IDS.rulesXml]);
    expect(await listGuides(client, { type: "specification", search: "concepts" })).toEqual([]);
  });
});

describe("readGuideBodies", () => {
  it("returns the bodies verbatim, links included, and reports unknown ids without throwing", async () => {
    const { found, unknown } = await readGuideBodies(client, [GUIDE_IDS.basicConcepts, "spec/does-not-exist"]);

    expect(found.map((f) => f.entry.id)).toEqual([GUIDE_IDS.basicConcepts]);
    expect(found[0].body).toBe(GUIDE_PAGES["reference-guide/01-introduction/03-basic-concepts.md"]);
    expect(unknown).toEqual(["spec/does-not-exist"]);
  });

  it("de-duplicates requested ids preserving first occurrence", async () => {
    const { found } = await readGuideBodies(client, [GUIDE_IDS.rulesXml, GUIDE_IDS.basicConcepts, GUIDE_IDS.rulesXml]);
    expect(found.map((f) => f.entry.id)).toEqual([GUIDE_IDS.rulesXml, GUIDE_IDS.basicConcepts]);
  });
});

describe("findKnownGuideIds", () => {
  it("returns the ids referenced in the text, in index order", async () => {
    const text = `Check ${GUIDE_IDS.smartRules} first, then ${GUIDE_IDS.rulesXml}. Ignore spec/unknown-thing.`;
    expect(await findKnownGuideIds(client, text)).toEqual([GUIDE_IDS.rulesXml, GUIDE_IDS.smartRules]);
  });

  it("returns [] instead of failing when OpenL Studio does not serve its guides (enrichment only)", async () => {
    mockHttp.onGet("../docs/toc.json").reply(500);
    expect(await findKnownGuideIds(client, "mentions spec/rules.xml")).toEqual([]);
  });
});

describe("guidesOverview", () => {
  it("lists every specification and groups the guides by the sections OpenL Studio titles", async () => {
    const overview = await guidesOverview(client);
    expect(overview.specifications.map((s) => s.id)).toEqual([GUIDE_IDS.rulesXml]);
    expect(overview.guideCount).toBe(2);
    expect(overview.guideSections).toEqual([
      { section: "Introduction", count: 1 },
      { section: "Working With OpenL Tables", count: 1 },
    ]);
  });
});
