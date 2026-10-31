/**
 * Unit tests of src/markdown-list.ts: a list as a Markdown table with the line counting it, paged and cut by rows.
 */

import { describe, it, expect } from "vitest";
import { cellText, formatList, formatListOf, type Column } from "./markdown-list.js";

interface Item { id: number; name: string }

const columns: Array<Column<Item>> = [
  { header: "id", value: (item) => item.id },
  { header: "name", value: (item) => item.name },
];
const items = (count: number, from = 0): Item[] =>
  Array.from({ length: count }, (_, i) => ({ id: from + i, name: `item ${from + i}` }));

describe("markdown list", () => {
  it("answers a whole list as the count and the table", () => {
    expect(formatList({ noun: "items", items: items(2), columns, offset: 0, total: 2 })).toBe(
      "2 items.\n\n| id | name |\n| --- | --- |\n| 0 | item 0 |\n| 1 | item 1 |",
    );
  });

  it.each([
    ["a page before more", { offset: 0, total: 120 }, "120 items; 1–2 below, the next page at offset=2."],
    ["the last page", { offset: 118, total: 120 }, "120 items; 119–120 below."],
    ["a full page of an unknown total", { offset: 50, hasMore: true }, "items; 51–52 below, the next page at offset=52."],
    ["the last page of an unknown total", { offset: 50 }, "52 items; 51–52 below."],
  ])("counts %s", (_case, page, line) => {
    expect(formatList({ noun: "items", items: items(2), columns, ...page }).split("\n")[0]).toBe(line);
  });

  it("counts one item in the singular, an irregular one as named", () => {
    expect(formatList({ noun: "items", items: items(1), columns, offset: 0, total: 1 }).split("\n")[0]).toBe("1 item.");
    expect(formatList({ noun: "matches", one: "match", items: items(1), columns, offset: 0, total: 1 }).split("\n")[0])
      .toBe("1 match.");
  });

  it.each([
    [0, undefined, "No items."],
    [100, 40, "40 items; none from offset=100."],
  ])("tells an empty page from offset %d", (offset, total, text) => {
    expect(formatList({ noun: "items", items: [], columns, offset, total })).toBe(text);
  });

  it("pages a list the server holds whole, with the notes after the table", () => {
    const text = formatListOf(items(100), 20, 40, { noun: "items", columns, notes: ["Searched 3 folders."] });

    expect(text.split("\n")[0]).toBe("100 items; 41–60 below, the next page at offset=60.");
    expect(text).toContain("| 59 | item 59 |");
    expect(text).not.toContain("| 60 |");
    expect(text.endsWith("Searched 3 folders.")).toBe(true);
  });

  it("cuts a page over the limit on a whole row and names the offset of the row it did not hold", () => {
    const text = formatList({ noun: "items", items: items(50), columns, offset: 10, total: 500 }, 300);

    const rows = text.split("\n").filter((line) => /^\| \d/.test(line));
    expect(text.length).toBeLessThanOrEqual(300);
    expect(rows.length).toBeLessThan(50);
    expect(text.split("\n")[0]).toBe(`500 items; 11–${10 + rows.length} below, the next page at offset=${10 + rows.length}.`);
  });

  it.each([
    [null, ""],
    [false, ""],
    [true, "yes"],
    [["a", "b"], "a, b"],
    [{ k: 1 }, '{"k":1}'],
    ["a | b\nc", String.raw`a \| b c`],
  ])("writes %j in a cell as %j", (value, text) => {
    expect(cellText(value)).toBe(text);
  });
});
