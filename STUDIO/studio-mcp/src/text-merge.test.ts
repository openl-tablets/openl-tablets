import { describe, expect, it } from "vitest";
import { mergedTextOf } from "./text-merge.js";
import type * as Types from "./types.js";

describe("mergedTextOf", () => {
  // A conflict at the end of a file whose last line ends without a line break.
  const atEnd: Types.ConflictTextBlock[] = [
    { kind: "merged", text: "a\n" },
    { kind: "conflict", ours: "b", base: "c", theirs: "d" },
  ];
  const decisions: [Types.ConflictTextDecision, string][] = [
    [{ side: "ours" }, "a\nb"],
    [{ side: "theirs" }, "a\nd"],
    [{ side: "base" }, "a\nc"],
    [{ side: "ours-theirs" }, "a\nb\nd"],
    [{ side: "theirs-ours" }, "a\nd\nb"],
    // The versions end without a line break, so the lines are written as they are.
    [{ lines: "e\nf\n" }, "a\ne\nf\n"],
    [{ lines: "" }, "a\n"],
  ];

  it.each(decisions)("puts the lines of %o in place of a conflict", (decision, text) => {
    expect(mergedTextOf(atEnd, [decision])).toBe(text);
  });

  it("writes typed lines with the line break most lines of the file end with, the last one optional", () => {
    const windows: Types.ConflictTextBlock[] = [
      { kind: "conflict", ours: "a\r\n", base: "", theirs: "b\r\n" },
      { kind: "merged", text: "c\r\nd\n" },
    ];
    expect(mergedTextOf(windows, [{ lines: "x\ny" }])).toBe("x\r\ny\r\nc\r\nd\n");
    expect(mergedTextOf(windows, [{ lines: "x\r\ny\r\n" }])).toBe("x\r\ny\r\nc\r\nd\n");

    const unix: Types.ConflictTextBlock[] = [
      { kind: "merged", text: "a\r\nb\nc\n" },
      { kind: "conflict", ours: "d\n", base: "", theirs: "e\n" },
    ];
    expect(mergedTextOf(unix, [{ lines: "x\r\ny" }])).toBe("a\r\nb\nc\nx\ny\n");
  });
});
