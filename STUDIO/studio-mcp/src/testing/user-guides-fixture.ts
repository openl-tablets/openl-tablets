/**
 * The user guides of a fixture OpenL Studio: the table of contents it serves at `/docs/toc.json` and the pages it
 * holds, with a reference guide, the specifications, and the pages the guide tools leave out — folder indexes and
 * another guide. Folders are titled the way Studio titles them by their names.
 */

import type { MockHttp } from "./mock-http.js";
import type { UserGuideEntry } from "../types.js";

/** Ids of the fixture documents, exported so tests reference one source of truth. */
export const GUIDE_IDS = {
  rulesXml: "spec/rules.xml",
  basicConcepts: "guide/introduction/basic-concepts",
  smartRules: "guide/working-with-openl-tables/table-types/decision-table/smart-rules-tables",
} as const;

/** The pages of the fixture, by their path from `/docs`. */
export const GUIDE_PAGES: Readonly<Record<string, string>> = {
  "index.md": "# User Guides\n",
  "getting-started/index.md": "# Getting Started\n",
  "ref/index.md": "# Specifications\n",
  "ref/rules.xml.md":
    "# Rules Project Descriptor (rules.xml)\n\nEvery project has one. See [Table Types](table-types.md).\n",
  "reference-guide/index.md": "# Reference Guide\n",
  "reference-guide/01-introduction/03-basic-concepts.md":
    "# Basic Concepts\n\nTables hold the rules.\n\n![A table](../ref-guide-images/table.png)\n",
  "reference-guide/02-working-with-openl-tables/01-table-types/02-decision-table/03-smart-rules-tables.md":
    "# Smart Rules Table\n\nConditions are matched by column headers.\n",
};

function page(title: string, file: string, children?: UserGuideEntry[]): UserGuideEntry {
  return { title, file, size: Buffer.byteLength(GUIDE_PAGES[file], "utf-8"), ...(children ? { children } : {}) };
}

/** The table of contents OpenL Studio builds for the fixture pages. */
const GUIDE_CONTENTS: UserGuideEntry = page("User Guides", "index.md", [
  page("Getting Started", "getting-started/index.md"),
  page("Specifications", "ref/index.md", [page("Rules Project Descriptor (rules.xml)", "ref/rules.xml.md")]),
  page("Reference Guide", "reference-guide/index.md", [
    {
      title: "Introduction",
      children: [page("Basic Concepts", "reference-guide/01-introduction/03-basic-concepts.md")],
    },
    {
      title: "Working With OpenL Tables",
      children: [
        {
          title: "Table Types",
          children: [
            {
              title: "Decision Table",
              children: [
                page(
                  "Smart Rules Table",
                  "reference-guide/02-working-with-openl-tables/01-table-types/02-decision-table/03-smart-rules-tables.md",
                ),
              ],
            },
          ],
        },
      ],
    },
  ]),
]);

/** Answers the requests for the user guides of the fixture OpenL Studio. */
export function mockUserGuides(mock: MockHttp): void {
  mock.onGet("../docs/toc.json").reply(200, GUIDE_CONTENTS);
  for (const [file, text] of Object.entries(GUIDE_PAGES)) {
    mock.onGet(`../docs/${file}`).reply(200, text);
  }
}
