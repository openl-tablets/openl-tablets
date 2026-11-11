/**
 * Unit tests for the guide registry: the documents of the table of contents OpenL Studio serves, their filtering, the
 * bodies read verbatim with unknown-id reporting, the part of migration notes for Rules Authors, free-text id scanning.
 */

import { describe, it, expect, beforeEach } from "vitest";

import { OpenLClient } from "./client.js";
import {
  findKnownGuideIds,
  listGuides,
  readGuideBodies,
  rulesAuthorsPart,
} from "./guides-registry.js";
import { MockHttp } from "./testing/mock-http.js";
import { GUIDE_IDS, GUIDE_PAGES, mockUserGuides } from "./testing/user-guides-fixture.js";

/** What the guide tools give for the migration notes of 6.4.0 and 7.0.0: their part for Rules Authors, titled. */
const NOTES_64 =
  "# OpenL Tablets 6.4.0 Migration Notes\n\n## Rules Authors\n\n* **Deleting a project is permanent.**\n\n" +
  "```bash\n## not a heading\n```\n\n### Trace\n\nOpens in the business view.\n";
const NOTES_700 =
  "# OpenL Tablets 7.0.0 Migration Notes\n\n## Rules Authors\n\n* A percent value equals the fraction it stands for.\n";

let client: OpenLClient;
let mockHttp: MockHttp;

beforeEach(() => {
  client = new OpenLClient({ baseUrl: "http://localhost:8080/studio" });
  mockHttp = new MockHttp(client);
  mockUserGuides(mockHttp);
});

describe("listGuides", () => {
  it("lists the specifications, the reference guide, then the migration notes, oldest first", async () => {
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
      {
        id: GUIDE_IDS.migration64,
        type: "migration",
        title: "OpenL Tablets 6.4.0 Migration Notes",
        path: "docs/migration-notes/6.4.0/migration.md",
        size: Buffer.byteLength(NOTES_64),
      },
      expect.objectContaining({ id: GUIDE_IDS.migration700, size: Buffer.byteLength(NOTES_700) }),
    ]);
  });

  it("reads the table of contents beside the REST API, and only the migration notes it lists", async () => {
    await listGuides(client, { type: "guide" });
    expect(mockHttp.history.get.map((request) => request.url)).toEqual(["../docs/toc.json"]);

    await listGuides(client, { search: "6.4" });
    expect(mockHttp.history.get.map((request) => request.url).slice(1)).toEqual([
      "../docs/toc.json",
      "../docs/migration-notes/6.4.0/migration.md",
    ]);
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

  it("returns the Rules Authors part of migration notes, and takes notes without one for unknown", async () => {
    const { found, unknown } = await readGuideBodies(client, [GUIDE_IDS.migration700, GUIDE_IDS.migration65]);

    expect(found.map((f) => f.body)).toEqual([NOTES_700]);
    expect(unknown).toEqual([GUIDE_IDS.migration65]);
  });

  it("de-duplicates requested ids preserving first occurrence", async () => {
    const { found } = await readGuideBodies(client, [GUIDE_IDS.rulesXml, GUIDE_IDS.basicConcepts, GUIDE_IDS.rulesXml]);
    expect(found.map((f) => f.entry.id)).toEqual([GUIDE_IDS.rulesXml, GUIDE_IDS.basicConcepts]);
  });
});

describe("rulesAuthorsPart", () => {
  it("takes the section up to the next one of its level, skipping a heading inside code", () => {
    expect(rulesAuthorsPart("Intro\n\n## Rules Authors\n\n```\n## code\n```\n### Sub\n\nText\n\n## Developers\n\nX\n"))
      .toBe("## Rules Authors\n\n```\n## code\n```\n### Sub\n\nText");
  });

  it("takes the last section to the end, whatever the line breaks, and nothing from notes without it", () => {
    expect(rulesAuthorsPart("## Developers\r\n\r\nX\r\n## Rules Authors  \r\n\r\n* Y\r\n"))
      .toBe("## Rules Authors  \n\n* Y");
    expect(rulesAuthorsPart("## Developers\n\n### Rules Authors\n")).toBeUndefined();
  });
});

describe("findKnownGuideIds", () => {
  it("returns the ids referenced in the text, in index order", async () => {
    const text = `Check ${GUIDE_IDS.smartRules} first, then ${GUIDE_IDS.rulesXml}. Ignore spec/unknown-thing.`;
    expect(await findKnownGuideIds(client, text)).toEqual([GUIDE_IDS.rulesXml, GUIDE_IDS.smartRules]);
  });

  it("takes an id only as a whole, not as the start of a longer one", async () => {
    const text = `Read migration/7.0.0.1, migration/6.4.0-rc and xspec/rules.xml; see ${GUIDE_IDS.basicConcepts}.`;
    expect(await findKnownGuideIds(client, text)).toEqual([GUIDE_IDS.basicConcepts]);
  });

  it("leaves out the migration notes that ask nothing of Rules Authors, as openl_get_guides refuses them", async () => {
    const text = `Read ${GUIDE_IDS.migration65} and ${GUIDE_IDS.migration700}.`;
    expect(await findKnownGuideIds(client, text)).toEqual([GUIDE_IDS.migration700]);
  });

  it("leaves out only the migration notes it cannot read, keeping the other references", async () => {
    mockHttp.onGet("../docs/migration-notes/7.0.0/migration.md").reply(500);
    const text = `Check ${GUIDE_IDS.rulesXml} and ${GUIDE_IDS.migration700}, then ${GUIDE_IDS.migration64}.`;
    expect(await findKnownGuideIds(client, text)).toEqual([GUIDE_IDS.rulesXml, GUIDE_IDS.migration64]);
  });

  it("returns [] instead of failing when OpenL Studio does not serve its guides (enrichment only)", async () => {
    mockHttp.onGet("../docs/toc.json").reply(500);
    expect(await findKnownGuideIds(client, "mentions spec/rules.xml")).toEqual([]);
  });
});
