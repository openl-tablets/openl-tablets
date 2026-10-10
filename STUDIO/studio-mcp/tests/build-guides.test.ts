/**
 * Unit tests for the guide-bundle build logic in src/build-guides.ts: id and
 * title derivation, relative-URL rewriting, and index assembly. The filesystem
 * plumbing around them is exercised by the real build.
 */

import { describe, it, expect } from "@jest/globals";

import {
  buildGuideBundle,
  deriveGuideId,
  extractFirstHeader,
  rewriteRelativeUrls,
  titleFromFilename,
  type DocsOrigin,
  type GuideSource,
} from "../src/build-guides.js";

const origin: DocsOrigin = { repo: "openl-tablets/openl-tablets", ref: "6.3.0" };

describe("deriveGuideId", () => {
  it("strips the numeric ordering prefix from every path segment and drops .md", () => {
    expect(deriveGuideId("guide", "01-introduction/03-basic-concepts.md")).toBe(
      "guide/introduction/basic-concepts",
    );
    expect(
      deriveGuideId("guide", "02-working-with-openl-tables/03-table-types/02-decision-table/05-smart-rules-tables.md"),
    ).toBe("guide/working-with-openl-tables/table-types/decision-table/smart-rules-tables");
  });

  it("namespaces specifications under spec/ and keeps dots in file names", () => {
    expect(deriveGuideId("specification", "rules.xml.md")).toBe("spec/rules.xml");
  });
});

describe("extractFirstHeader", () => {
  it("returns the first ATX header with inline code/emphasis markers stripped", () => {
    const md = "Some preamble\n\n# OpenL Tablets — Rules Project Descriptor (`rules.xml`)\n\n## Later\n";
    expect(extractFirstHeader(md)).toBe("OpenL Tablets — Rules Project Descriptor (rules.xml)");
  });

  it("ignores header-looking lines inside fenced code blocks", () => {
    const md = "```bash\n# a shell comment, not a header\n```\n\n## Real Header\n";
    expect(extractFirstHeader(md)).toBe("Real Header");
  });

  it("returns undefined when the document has no header", () => {
    expect(extractFirstHeader("just text\nno headers here\n")).toBeUndefined();
  });
});

describe("titleFromFilename", () => {
  it("humanizes the file name after stripping the ordering prefix", () => {
    expect(titleFromFilename("01-introduction/02-what-is-openl-tablets.md")).toBe(
      "What is openl tablets",
    );
  });
});

describe("rewriteRelativeUrls", () => {
  const sourceDir = "Docs/user-guides/reference-guide/01-introduction";

  it("points relative images at raw content and relative links at the blob page, resolving ..", () => {
    const md = "![shot](../ref-guide-images/a.png)\nSee [next](../02-tables/01-intro.md#basics).";
    const out = rewriteRelativeUrls(md, sourceDir, origin);
    expect(out).toContain(
      "![shot](https://raw.githubusercontent.com/openl-tablets/openl-tablets/6.3.0/Docs/user-guides/reference-guide/ref-guide-images/a.png)",
    );
    expect(out).toContain(
      "[next](https://github.com/openl-tablets/openl-tablets/blob/6.3.0/Docs/user-guides/reference-guide/02-tables/01-intro.md#basics)",
    );
  });

  it("leaves absolute URLs, pure anchors, mailto:, and fenced code untouched", () => {
    const md = [
      "[a](https://example.com/x) [b](#section) [c](mailto:x@y.z)",
      "```",
      "[fenced](relative.md)",
      "```",
    ].join("\n");
    expect(rewriteRelativeUrls(md, sourceDir, origin)).toBe(md);
  });

  it("leaves targets that escape the repository root unchanged", () => {
    const md = "[out](../../../../../outside.md)";
    expect(rewriteRelativeUrls(md, "Docs/ref", origin)).toBe(md);
  });
});

describe("buildGuideBundle", () => {
  const guideSource = (relPath: string, content: string): GuideSource => ({
    type: "guide",
    repoDir: "Docs/user-guides/reference-guide",
    relPath,
    content,
  });

  it("orders specifications first, then guides in source (reading) order, with derived metadata", () => {
    const { index, bodies } = buildGuideBundle(
      [
        guideSource("02-tables/01-recognition.md", "# Table Recognition\n\nBody B.\n"),
        guideSource("01-intro/01-preface.md", "# Preface\n\nBody A.\n"),
        {
          type: "specification",
          repoDir: "Docs/ref",
          relPath: "rules.xml.md",
          content: "no header here\n",
        },
      ],
      origin,
      "2026-01-01T00:00:00.000Z",
    );

    expect(index.source_repo).toBe(origin.repo);
    expect(index.source_ref).toBe(origin.ref);
    expect(index.generated_at).toBe("2026-01-01T00:00:00.000Z");
    expect(index.guides.map((g) => g.id)).toEqual([
      "spec/rules.xml",
      "guide/intro/preface",
      "guide/tables/recognition",
    ]);

    const spec = index.guides[0];
    // No header in the source → file-name fallback title.
    expect(spec.title).toBe("Rules.xml");
    expect(spec.path).toBe("spec/rules.xml.md");
    expect(spec.source_path).toBe("Docs/ref/rules.xml.md");
    expect(spec.size_bytes).toBe(Buffer.byteLength("no header here\n", "utf-8"));
    expect(bodies.get("guide/intro/preface")).toBe("# Preface\n\nBody A.\n");
  });

  it("fails the build when two documents derive the same id", () => {
    const sources = [
      guideSource("01-intro/01-preface.md", "# A\n"),
      guideSource("01-intro/02-preface.md", "# B\n"),
    ];
    expect(() => buildGuideBundle(sources, origin, "2026-01-01T00:00:00.000Z")).toThrow(
      /collision.*guide\/intro\/preface/,
    );
  });
});
