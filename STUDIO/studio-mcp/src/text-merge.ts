/**
 * A conflicted text file put together from the parts Studio merges it into line by line, each conflict resolved as
 * decided, as the Merge line by line window of OpenL Studio puts it together.
 */
import type * as Types from "./types.js";

type ConflictBlock = Extract<Types.ConflictTextBlock, { kind: "conflict" }>;

/** The texts of a part of a file: its lines, or every version of a conflict. */
function textsOf(block: Types.ConflictTextBlock): string[] {
  return block.kind === "merged" ? [block.text] : [block.ours, block.base, block.theirs];
}

/** How many times texts hold a line break. */
function lineBreakCount(texts: string[], lineBreak: string): number {
  return texts.reduce((count, text) => count + text.split(lineBreak).length - 1, 0);
}

/**
 * The line break a file is written with: the one of Windows when it ends most lines of the versions, a line feed
 * alone otherwise.
 */
function lineBreakOf(blocks: Types.ConflictTextBlock[]): string {
  const texts = blocks.flatMap(textsOf);
  const windows = lineBreakCount(texts, "\r\n");
  return windows > lineBreakCount(texts, "\n") - windows ? "\r\n" : "\n";
}

/** Puts lines after other lines, ending the first ones with a line break when they end without one. */
function joinLines(first: string, second: string, lineBreak: string): string {
  return first && second && !first.endsWith("\n") ? first + lineBreak + second : first + second;
}

/**
 * Lines written in place of a conflict, put in the line breaks of the file.
 *
 * They end with a line break when the versions of the conflict do, so the file keeps the line break it ends with: a
 * line break written after the last line adds no empty line. Nothing written gives no lines.
 */
function writtenLines(written: string, block: ConflictBlock, lineBreak: string): string {
  const endsWithLineBreak = block.ours.endsWith("\n") || block.theirs.endsWith("\n");
  const lines = written.replaceAll("\r\n", "\n");
  const shown = endsWithLineBreak ? lines.replace(/\n$/, "") : lines;
  const text = shown.replaceAll("\n", lineBreak);
  return text && endsWithLineBreak ? text + lineBreak : text;
}

/** The lines a decision puts in place of a conflict. */
function linesOf(block: ConflictBlock, decision: Types.ConflictTextDecision, lineBreak: string): string {
  if ("lines" in decision) {
    return writtenLines(decision.lines, block, lineBreak);
  }
  switch (decision.side) {
    case "ours":
      return block.ours;
    case "theirs":
      return block.theirs;
    case "base":
      return block.base;
    case "ours-theirs":
      return joinLines(block.ours, block.theirs, lineBreak);
    case "theirs-ours":
      return joinLines(block.theirs, block.ours, lineBreak);
  }
}

/** How many conflicts the parts of a file hold. */
export function conflictCount(blocks: Types.ConflictTextBlock[]): number {
  return blocks.filter((block) => block.kind === "conflict").length;
}

/**
 * The text of a file whose conflicts are resolved as decided: the lines merged and the lines of each decision, in
 * their order. Lines followed by more lines end with a line break.
 *
 * @param decisions how each conflict of the file is resolved, in their order: one for every conflict
 */
export function mergedTextOf(blocks: Types.ConflictTextBlock[], decisions: Types.ConflictTextDecision[]): string {
  const lineBreak = lineBreakOf(blocks);
  let conflict = 0;
  let text = "";
  for (const block of blocks) {
    const lines = block.kind === "merged" ? block.text : linesOf(block, decisions[conflict++], lineBreak);
    text = joinLines(text, lines, lineBreak);
  }
  return text;
}
