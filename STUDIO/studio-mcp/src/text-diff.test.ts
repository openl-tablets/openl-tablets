import { describe, expect, it } from "vitest";
import { DIFF_MAX_CELLS, unifiedDiff } from "./text-diff.js";

const numbered = (count: number, from = 1) => Array.from({ length: count }, (_, i) => `line ${from + i}`);

describe("unifiedDiff", () => {
  it("tells a line changed as removed then added, with three lines of context around it", () => {
    const before = [...numbered(9), ""].join("\n");
    const after = before.replace("line 5", "line five");

    expect(unifiedDiff(before, after)).toEqual([
      "@@ -2,7 +2,7 @@",
      " line 2", " line 3", " line 4",
      "-line 5",
      "+line five",
      " line 6", " line 7", " line 8",
    ]);
  });

  it("parts changes far apart into hunks of their own, and joins near ones into one", () => {
    const lines = numbered(30);
    const after = [...lines];
    after[1] = "second";
    after[4] = "fifth";
    after[25] = "twenty-sixth";

    const diff = unifiedDiff(lines.join("\n"), after.join("\n"))!;

    expect(diff.filter((line) => line.startsWith("@@"))).toEqual(["@@ -1,8 +1,8 @@", "@@ -23,7 +23,7 @@"]);
  });

  it("names where a hunk of added lines stands in a version that holds none of them", () => {
    expect(unifiedDiff("", "one\ntwo\n")).toEqual(["@@ -0,0 +1,2 @@", "+one", "+two"]);
    expect(unifiedDiff("one\n", "")).toEqual(["@@ -1 +0,0 @@", "-one"]);
  });

  it("answers no line for versions holding the same lines, a last line break or not", () => {
    expect(unifiedDiff("a\r\nb\r\n", "a\nb")).toEqual([]);
  });

  it("answers nothing for versions too long to pair", () => {
    const long = numbered(Math.ceil(Math.sqrt(DIFF_MAX_CELLS))).join("\n");

    expect(unifiedDiff(long, `${long}\nmore`)).toBeUndefined();
  });
});
