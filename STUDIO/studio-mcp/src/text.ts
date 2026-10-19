/**
 * Text helpers shared by the modules of the server.
 */

/** `1 table`, `2 tables`: a count with the noun it counts, in the plural unless it is one. */
export function formatCount(count: number, singular: string, plural: string = `${singular}s`): string {
  return `${count} ${count === 1 ? singular : plural}`;
}

/** The text without the repeats of `char` it ends with, and those it starts with unless `atStart` is false. */
export function trimChar(text: string, char: string, atStart = true): string {
  let from = 0;
  let to = text.length;
  while (atStart && from < to && text[from] === char) from++;
  while (to > from && text[to - 1] === char) to--;
  return text.slice(from, to);
}

/**
 * The rendering of the largest count from `low` to `high` whose rendering fits `charLimit`, found by halving: a
 * rendering only grows with the count. Undefined when even `low` does not fit.
 */
export function largestFitting(
  low: number,
  high: number,
  render: (count: number) => string,
  charLimit: number,
): string | undefined {
  let best: string | undefined;
  while (low <= high) {
    const middle = Math.floor((low + high) / 2);
    const candidate = render(middle);
    if (candidate.length <= charLimit) {
      best = candidate;
      low = middle + 1;
    } else {
      high = middle - 1;
    }
  }
  return best;
}
