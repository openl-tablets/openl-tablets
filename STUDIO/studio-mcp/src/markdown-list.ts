/**
 * A list a tool answers as a Markdown table: a line that counts the items and tells the page, the table of the page
 * with one row per item, and the notes that go with it. A table names each field once, in its header, where a JSON
 * list repeats every name in every item: the same page reads in about a third of the tokens.
 */

import { RESPONSE_LIMITS } from "./constants.js";
import { formatCount, largestFitting } from "./text.js";
import type * as Types from "./types.js";

/** A column of a table: its header, and the value an item has in it. */
export interface Column<T> {
  header: string;
  value: (item: T) => unknown;
}

/** A page of a list, and how it continues. */
export interface ListPage<T> {
  /** What the list counts, in the plural, such as `tables`. */
  noun: string;
  /** What the list counts in the singular, when it is not the plural without its last `s`, such as `match`. */
  one?: string;
  /** The items of the page. */
  items: T[];
  columns: Array<Column<T>>;
  /** The index of the first item of the page in the whole list. */
  offset: number;
  /** How many items the whole list has, when it is known. */
  total?: number;
  /** Whether items follow the page, when the total is not known. */
  hasMore?: boolean;
  /** Lines after the table. */
  notes?: string[];
}

/** The text of a value in a cell of a table: a list joined by commas, an object as JSON, on one line. */
export function cellText(value: unknown): string {
  if (value === undefined || value === null || value === false) {
    return "";
  }
  if (value === true) {
    return "yes";
  }
  const one = (item: unknown): string =>
    typeof item === "object" && item !== null ? JSON.stringify(item) : String(item as string | number | bigint);
  const text = Array.isArray(value) ? value.map(one).join(", ") : one(value);
  return text.replaceAll("|", String.raw`\|`).replaceAll(/\r?\n/g, " ");
}

/** The rows of items as a Markdown table, every column of every row filled, an empty value as an empty cell. */
export function markdownTable<T>(items: T[], columns: Array<Column<T>>): string {
  const line = (cells: string[]): string => `| ${cells.join(" | ")} |`;
  return [
    line(columns.map(({ header }) => header)),
    line(columns.map(() => "---")),
    ...items.map((item) => line(columns.map(({ value }) => cellText(value(item))))),
  ].join("\n");
}

/**
 * A page of a list as Markdown, cut to the response limit by whole rows: a line counting the items and naming the
 * offset of the next page, the table, and the notes.
 */
export function formatList<T>(page: ListPage<T>, charLimit: number = RESPONSE_LIMITS.MAX_CHARACTERS): string {
  return fitItems((count) => [
    countLine(page, count),
    ...(count > 0 ? [markdownTable(page.items.slice(0, count), page.columns)] : []),
    ...(page.notes ?? []),
  ].join("\n\n"), page.items.length, charLimit);
}

/** What a list is, beside its items and its page. */
type ListOf<T> = Omit<ListPage<T>, "items" | "offset" | "total" | "hasMore">;

/** A page of a list the server holds whole: at most `limit` items from `offset`. */
export function formatListOf<T>(items: T[], limit: number, offset: number, page: ListOf<T>): string {
  return formatList({ ...page, items: items.slice(offset, offset + limit), offset, total: items.length });
}

/** A list answered whole, cut to the response limit by whole rows. */
export function formatWholeList<T>(items: T[], page: ListOf<T>): string {
  return formatListOf(items, items.length, 0, page);
}

/** A page of a list OpenL Studio pages itself, from `offset`: Studio takes a true item offset. */
export function formatStudioPage<T>(page: Types.CollectionPage<T>, offset: number, list: ListOf<T>): string {
  return formatList({
    ...list,
    items: page.items,
    offset,
    total: page.total,
    hasMore: hasMoreItems(offset, page.items.length, page.pageSize, page.total),
  });
}

/** Whether more items follow a page of a Studio list: past the total when Studio tells it, else when the page is full. */
export function hasMoreItems(offset: number, delivered: number, pageSize: number, total: number | undefined): boolean {
  return total !== undefined ? offset + delivered < total : delivered >= pageSize;
}

/**
 * The rendering of as many of `items` leading items as fit `charLimit`, at least one: `render` renders the first
 * `count` of them.
 */
export function fitItems(render: (count: number) => string, items: number, charLimit: number = RESPONSE_LIMITS.MAX_CHARACTERS): string {
  const whole = render(items);
  if (whole.length <= charLimit || items <= 1) {
    return whole;
  }
  return largestFitting(1, items - 1, render, charLimit) ?? render(1);
}

/** `381 tables; 1–50 below, the next page at offset=50.`, for the first `count` items of the page. */
export function countLine<T>({ noun, one, items, offset, total, hasMore }: Pick<ListPage<T>, "noun" | "one" | "items" | "offset" | "total" | "hasMore">, count: number): string {
  const counted = (number: number): string => formatCount(number, one ?? noun.replace(/s$/, ""), noun);
  if (count === 0) {
    return offset > 0 && total !== undefined ? `${counted(total)}; none from offset=${offset}.` : `No ${noun}.`;
  }
  const next = offset + count;
  const more = total === undefined ? count < items.length || hasMore === true : next < total;
  // A list whose last page this is has as many items as its pages hold
  const known = total ?? (more ? undefined : offset + items.length);
  const whole = offset === 0 && !more;
  const counts = known === undefined ? noun : counted(known);
  const shown = whole ? "" : `; ${offset + 1}–${next} below`;
  const continued = more ? `, the next page at offset=${next}` : "";
  return `${counts}${shown}${continued}.`;
}
