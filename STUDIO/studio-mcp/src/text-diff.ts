/**
 * Two versions of a text told as a unified diff, the way `git diff` tells them: the lines removed and added, each
 * hunk with lines of context around it and headed by where it stands in both versions.
 */

/** How many unchanged lines stand around a change, as `git diff` shows them. */
const CONTEXT_LINES = 3;

/** The most lines the two versions may hold together: the pairing of their lines costs their product. */
export const DIFF_MAX_CELLS = 4_000_000;

/** A line of a diff: kept in both versions, removed from the first or added in the second. */
interface DiffLine {
  kind: " " | "-" | "+";
  text: string;
  /** The number of the line in the first version, for a line it holds. */
  before?: number;
  /** The number of the line in the second version, for a line it holds. */
  after?: number;
}

/** The lines of a text, a last line break ending the text rather than starting an empty line. */
function linesOf(text: string): string[] {
  if (text === "") {
    return [];
  }
  const lines = text.split(/\r?\n/);
  return lines.at(-1) === "" ? lines.slice(0, -1) : lines;
}

/** The lines of both versions paired as the longest run of lines they share, then the rest removed or added. */
function pairLines(first: string[], second: string[]): DiffLine[] {
  const width = second.length + 1;
  // longest[i * width + j]: the most lines first[i..] and second[j..] share
  const longest = new Uint32Array((first.length + 1) * width);
  const shared = (i: number, j: number): number => longest[i * width + j] ?? 0;
  for (let i = first.length - 1; i >= 0; i--) {
    for (let j = second.length - 1; j >= 0; j--) {
      longest[i * width + j] = first[i] === second[j] ? shared(i + 1, j + 1) + 1 : Math.max(shared(i + 1, j), shared(i, j + 1));
    }
  }
  const lines: DiffLine[] = [];
  let i = 0;
  let j = 0;
  while (i < first.length || j < second.length) {
    if (i < first.length && j < second.length && first[i] === second[j]) {
      lines.push({ kind: " ", text: first[i] ?? "", before: i + 1, after: j + 1 });
      i++;
      j++;
    } else if (i < first.length && (j === second.length || shared(i + 1, j) >= shared(i, j + 1))) {
      // A line replaced is told removed before the line put in its place, as `git diff` tells it
      lines.push({ kind: "-", text: first[i] ?? "", before: i + 1 });
      i++;
    } else {
      lines.push({ kind: "+", text: second[j] ?? "", after: j + 1 });
      j++;
    }
  }
  return lines;
}

/** Where a hunk starts in one version and how many of its lines that version holds, as `@@` names it. */
function rangeOf(lines: DiffLine[], side: "before" | "after", previous: number): string {
  const held = lines.filter((line) => line[side] !== undefined);
  // A hunk holding no line of a version starts after the last line before it, as `git diff` names it
  const start = held[0]?.[side] ?? previous;
  return held.length === 1 ? `${start}` : `${start},${held.length}`;
}

/** The line of a version last told before a line of the diff, for a hunk that holds none of that version. */
function lastBefore(lines: DiffLine[], index: number, side: "before" | "after"): number {
  for (let at = index - 1; at >= 0; at--) {
    const number = lines[at]?.[side];
    if (number !== undefined) {
      return number;
    }
  }
  return 0;
}

/**
 * The lines of a unified diff of two versions of a text, without the file header: each hunk headed by `@@ -a,b +c,d @@`,
 * its unchanged lines led by a space, the removed ones by `-` and the added ones by `+`.
 *
 * Two versions with the same lines answer no line. Versions whose lines are too many to pair, more than
 * {@link DIFF_MAX_CELLS} by their product, answer undefined.
 */
export function unifiedDiff(before: string, after: string): string[] | undefined {
  const first = linesOf(before);
  const second = linesOf(after);
  if ((first.length + 1) * (second.length + 1) > DIFF_MAX_CELLS) {
    return undefined;
  }
  const lines = pairLines(first, second);
  const changed = lines.flatMap((line, index) => (line.kind === " " ? [] : [index]));
  const hunks: Array<[number, number]> = [];
  for (const index of changed) {
    const from = Math.max(0, index - CONTEXT_LINES);
    const to = Math.min(lines.length, index + CONTEXT_LINES + 1);
    const last = hunks.at(-1);
    if (last && from <= last[1]) {
      last[1] = Math.max(last[1], to);
    } else {
      hunks.push([from, to]);
    }
  }
  return hunks.flatMap(([from, to]) => {
    const hunk = lines.slice(from, to);
    const header = `@@ -${rangeOf(hunk, "before", lastBefore(lines, from, "before"))} `
      + `+${rangeOf(hunk, "after", lastBefore(lines, from, "after"))} @@`;
    return [header, ...hunk.map((line) => `${line.kind}${line.text}`)];
  });
}
