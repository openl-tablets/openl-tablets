/**
 * What changed in one file of a project between two of its versions: two revisions, or a revision and the working
 * copy. A workbook is compared table by table, as OpenL Studio compares it; any other text is told as a unified diff.
 *
 * A base of another history, as a revision of another branch is, is compared from the revision both histories parted
 * at, as a pull request shows what a branch changed: what the later side changed since then.
 */

import type { OpenLClient } from "../client.js";
import { isNotFoundError } from "../http.js";
import { fitLines } from "../markdown-list.js";
import { firstCellOf } from "../table-grid.js";
import { unifiedDiff } from "../text-diff.js";
import { formatCount } from "../text.js";
import { textOf } from "../transfer-links.js";
import type * as Types from "../types.js";
import { isWorkbook } from "../utils.js";
import type { ToolHandlerExtra } from "./common.js";
import { waitForResult, type WaitOptions } from "./wait.js";

/** How a comparison is waited for while Studio compares the two versions. */
const COMPARISON_WAIT: WaitOptions = {
  timeoutMs: 60_000,
  progressMessage: "Comparing the two versions",
  timeoutMessage: "The comparison of the two versions did not end in time.",
};

/** How many changes of one table are told; the rest are counted. */
const CHANGES_PER_TABLE = 30;

/** What the comparison of a file is asked for. */
export interface FileComparisonRequest {
  projectId: string;
  path: string;
  /** The earlier revision; omitted for the one the later revision was made on, or the saved revision. */
  base?: string;
  /** The later revision; omitted for the working copy. */
  revision?: string;
  /** The branch the revisions of a workbook are read on; omitted for the one the project is on. */
  branch?: string;
  limit: number;
  offset: number;
}

/** One of the two versions compared: a revision, or the working copy, and whether it holds the file. */
interface Version {
  revision?: string;
  holds: boolean;
}

/** The revisions of the versions compared, as the history names them; the working copy has none. */
interface ComparedRevisions {
  /** The earlier revision, or nothing for the first revision of a project, which every file was added by. */
  base: string | undefined;
  /** The later revision, or nothing for the working copy. */
  revision: string | undefined;
  /** The revision both parted at, when the base is not in the history of the later side. */
  common: string | undefined;
}

/**
 * The revisions to compare: the ones named, else the revision the later one was made on, or the revision the working
 * copy was saved at. A revision named is read back as the history of the project names it: a short id or a branch
 * would name no version of a file, which then reads as a file the version does not hold. A name the history does not
 * know is refused.
 */
async function versionsOf(client: OpenLClient, request: FileComparisonRequest): Promise<ComparedRevisions> {
  if (request.base === undefined && request.revision === undefined) {
    // The working copy is compared with the revision it was saved at, as the Save dialog compares it
    const { revision } = await client.getProject(request.projectId);
    if (!revision) {
      throw new Error("The working copy has no saved revision to compare with: name the base.");
    }
    return { base: revision, revision: undefined, common: undefined };
  }
  // Only the revisions are read here: the estimate of the conflicts is left out.
  const changes = await client.getProjectRevisionChanges(request.projectId, request.revision, request.base, false);
  return {
    base: changes.base?.revisionNo,
    revision: changes.revision?.revisionNo,
    common: changes.common?.revisionNo,
  };
}

/** Whether a version holds the file. A revision is a commit, so it is read whichever branch the project is on. */
async function holds(client: OpenLClient, request: FileComparisonRequest, revision: string | undefined): Promise<boolean> {
  return client.getProjectFileMeta(request.projectId, request.path, { version: revision })
    .then(() => true, (error: unknown) => (isNotFoundError(error) ? false : Promise.reject(error)));
}

/** A version as the answer names it. */
function labelOf(version: Version): string {
  const name = version.revision === undefined ? "the working copy" : `revision ${version.revision}`;
  return version.holds ? name : `${name} (no file)`;
}

/**
 * What changed in a file between two of its versions, as text: the tables of a workbook that differ, each with its
 * rows and columns added and removed and its cells changed, a page of them at a time; the unified diff of any other
 * text. A version that does not hold the file reads as empty, so the file is added or deleted whole.
 */
export async function compareProjectFile(
  client: OpenLClient,
  request: FileComparisonRequest,
  extra: ToolHandlerExtra | undefined,
): Promise<string> {
  const { base, revision, common } = await versionsOf(client, request);
  // A base of another branch is compared from the revision both parted at, as a pull request shows a branch
  const since = common ?? base;
  // Both versions are asked about at once: neither answer waits for the other
  const [firstHolds, secondHolds] = await Promise.all([
    since !== undefined && holds(client, request, since),
    holds(client, request, revision),
  ]);
  const first: Version = { revision: since, holds: firstHolds };
  const second: Version = { revision, holds: secondHolds };
  if (!first.holds && !second.holds) {
    throw new Error(`Neither ${labelOf({ ...first, holds: true })} nor ${labelOf({ ...second, holds: true })} holds '${request.path}'.`);
  }
  const parted = common === undefined ? "" : ` (where it and revision ${base} parted)`;
  const title = `${request.path}: ${labelOf(first)}${parted} → ${labelOf(second)}`;
  return isWorkbook(request.path)
    ? compareWorkbook(client, request, [first, second], title, extra)
    : compareText(client, request, [first, second], title);
}

/** A side of a comparison of a workbook: the version read, or an empty workbook for one without the file. */
function sideOf(request: FileComparisonRequest, version: Version): Types.ComparisonSide {
  if (!version.holds) {
    return { path: request.path, absent: true };
  }
  return version.revision === undefined
    ? { path: request.path }
    : { path: request.path, revision: version.revision, branch: request.branch };
}

/** The tables two workbooks differ in, by sheet, the tables only pushed by rows inserted above them left out. */
function changedTablesOf(comparison: Types.ComparisonView): Array<{ sheet: string; table: Types.ComparisonNodeView }> {
  return comparison.sheets.flatMap((sheet) => (sheet.children ?? [])
    .filter((table) => table.status !== "equal")
    .map((table) => ({ sheet: sheet.name ?? "", table })));
}

async function compareWorkbook(
  client: OpenLClient,
  request: FileComparisonRequest,
  [first, second]: [Version, Version],
  title: string,
  extra: ToolHandlerExtra | undefined,
): Promise<string> {
  const id = await client.compareProjectFiles(request.projectId, sideOf(request, first), sideOf(request, second));
  return tablesThatDiffer(client, id, request, title, extra, async (sheet, table, shown) => tableSection(sheet,
    await waitForResult((read) => client.getComparisonTable(id, table.id ?? "", read), COMPARISON_WAIT, extra), shown));
}

/**
 * A page of the tables a comparison Studio made finds different, each told by the section it reads, and the
 * comparison dropped once read. The tables are cut to the size of an answer, and the next page then starts at the
 * first table left out.
 */
async function tablesThatDiffer(
  client: OpenLClient,
  id: string,
  request: FileComparisonRequest,
  title: string,
  extra: ToolHandlerExtra | undefined,
  section: (sheet: string, table: Types.ComparisonNodeView, shown: Shown) => Promise<string>,
): Promise<string> {
  try {
    const comparison = await waitForResult((read) => client.getComparison(id, read), COMPARISON_WAIT, extra);
    const tables = changedTablesOf(comparison);
    if (tables.length === 0) {
      // Studio writes a whole workbook anew on every save, so a file changed and changed back is still modified
      return `${title}\n\nNo table differs: the workbook changed outside its tables only, or was saved anew unchanged.`;
    }
    const twins = twinsOf(comparison);
    const page = tables.slice(request.offset, request.offset + request.limit);
    const whole = page.length === 1;
    const sections: string[] = [];
    for (const [at, { sheet, table }] of page.entries()) {
      // One table after another: they are read from the one comparison Studio keeps for the client
      const shown = { index: request.offset + at, whole, twin: twins.has(`${sheet}\n${table.name ?? ""}`) };
      sections.push(await section(sheet, table, shown)); // NOSONAR typescript:S9382
    }
    const end = request.offset + page.length;
    const more = end < tables.length ? `, the next page at offset=${end}` : "";
    const verb = tables.length === 1 ? "differs" : "differ";
    const summary = `${formatCount(tables.length, "table")} ${verb}; ${request.offset + 1}–${end} below${more}.`;
    if (whole) {
      // The one table of the page lists every change, cut by whole changes to the size of an answer
      return fitLines({
        head: [title, "", summary, ""],
        lines: sections[0]?.split("\n") ?? [],
        more: (left) => `… ${formatCount(left, "more change")}, which the answer has no room for.`,
      });
    }
    return fitLines({
      head: [title, "", summary],
      lines: sections.map((text) => `\n${text}`),
      more: (left) => `\n… ${formatCount(left, "more table")}: the next page at offset=${end - left}.`,
    });
  } finally {
    await client.deleteComparison(id).catch(() => undefined);
  }
}

/** How a table of a page is told: its place in the list, whether it is the one table of the page, whether its name repeats. */
interface Shown {
  index: number;
  whole: boolean;
  twin: boolean;
}

/** The tables of a sheet whose name another table of the sheet has too, such as the versions of a rule, by sheet and name. */
function twinsOf(comparison: Types.ComparisonView): Set<string> {
  const counts = new Map<string, number>();
  for (const sheet of comparison.sheets) {
    for (const table of sheet.children ?? []) {
      const key = `${sheet.name ?? ""}\n${table.name ?? ""}`;
      counts.set(key, (counts.get(key) ?? 0) + 1);
    }
  }
  return new Set([...counts].filter(([, count]) => count > 1).map(([key]) => key));
}

/**
 * The properties a table declares under its header, as `state=CA`, which tell the versions of a rule apart; nothing for
 * a table that declares none.
 */
function propertiesOf(source: Types.RawTableCell[][] | undefined): string | undefined {
  const rows = source ?? [];
  if (textOfCell(rows[1]?.[0]).toLowerCase() !== "properties") {
    return undefined;
  }
  const pairs: string[] = [];
  // The properties are the rows the merged cell of the word properties covers
  for (let row = 1; row < rows.length && (row === 1 || rows[row]?.[0]?.covered); row++) {
    const key = textOfCell(rows[row]?.[1]);
    if (key) {
      pairs.push(`${key}=${textOfCell(rows[row]?.[2])}`);
    }
  }
  return pairs.length > 0 ? pairs.join(", ") : undefined;
}

/** The name of a table under which a section tells it: with its properties or its range when another table has the name. */
function nameOf(name: string, shown: Shown, source: Types.RawTableCell[][] | undefined, range: string | undefined): string {
  if (!shown.twin) {
    return name;
  }
  const version = propertiesOf(source) ?? range;
  return version ? `${name} [${version}]` : name;
}

/** The text of a cell: its formula, else its value. */
function textOfCell(cell: Types.RawTableCell | undefined): string {
  const value = cell?.value === undefined || cell.value === null ? "" : String(cell.value);
  // A formula is told with its result, which changes when a cell it reads changes though the formula does not
  const result = value === "" ? "" : ` (${value})`;
  const text = cell?.formula ? `${cell.formula}${result}` : value;
  // A value that would read as several, or as another one, is quoted: a separator, a line break, a quote, an end space
  return /[|→"\r\n]|^\s|\s$/.test(text) ? JSON.stringify(text) : text;
}

/** What a cell holds: a formula, a number, a boolean, a text, a list or nothing. */
function kindOfCell(cell: Types.RawTableCell | undefined): string {
  if (cell?.formula) {
    return "formula";
  }
  const value = cell?.value;
  if (value === undefined || value === null || value === "") {
    return "empty";
  }
  if (Array.isArray(value)) {
    return "list";
  }
  return typeof value === "string" ? "text" : typeof value;
}

/** A value as a change tells it, an empty one named so. */
function changed(text: string): string {
  return text === "" ? "(empty)" : text;
}

/** The values of a row, the cells a merge covers left out. */
function rowValues(row: Types.RawTableCell[] | undefined): string {
  return (row ?? []).filter((cell) => !cell.covered).map((cell) => changed(textOfCell(cell))).join(" | ");
}

/** The values of a column, top to bottom, the cells a merge covers left out. */
function columnValues(source: Types.RawTableCell[][], column: number): string {
  return source.map((row) => row[column]).filter((cell) => cell && !cell.covered)
    .map((cell) => changed(textOfCell(cell))).join(" | ");
}

/** The first row and column of a range in A1 notation, from 1 and from 0. */
function cornerOf(range: string | undefined): { row: number; column: number } {
  const cell = firstCellOf(range ?? "");
  return cell ? { row: cell.row, column: cell.column - 1 } : { row: 1, column: 0 };
}

/** The letters of a column of a sheet, counted from 0. */
function letterOf(column: number): string {
  let letters = "";
  for (let rest = column + 1; rest > 0; rest = Math.floor((rest - 1) / 26)) {
    letters = String.fromCodePoint(65 + ((rest - 1) % 26)) + letters;
  }
  return letters;
}

/** The place of every cell of a side by its address. */
function placesOf(source: Types.RawTableCell[][]): Map<string, [number, number]> {
  const places = new Map<string, [number, number]>();
  source.forEach((row, r) => row.forEach((cell, c) => {
    if (cell.cell) {
      places.set(cell.cell, [r, c]);
    }
  }));
  return places;
}

/** The changes of a table both workbooks hold: the columns and the rows added or removed, then the cells changed. */
function changesOf(view: Types.ComparisonTableView): string[] {
  const table = tableOf(view);
  return [
    ...(view.columns ?? []).flatMap((column) => columnChangeOf(table, column)),
    ...(view.rows ?? []).flatMap((row) => rowChangesOf(table, row)),
  ];
}

/** Both versions of a compared table, with where each starts on its sheet and what the later one changed. */
interface ComparedTable {
  view: Types.ComparisonTableView;
  first: Types.RawTableCell[][];
  second: Types.RawTableCell[][];
  firstCorner: { row: number; column: number };
  secondCorner: { row: number; column: number };
  /** The column of the earlier version each column of the later one stands for. */
  firstColumnOf: Map<number, number>;
  /** The changed cells by the row of the later version they stand in, each row keeping their order. */
  changedInRow: Map<number, Array<[string, number]>>;
}

function tableOf(view: Types.ComparisonTableView): ComparedTable {
  const second = view.second?.source ?? [];
  const places = placesOf(second);
  const changedInRow = new Map<number, Array<[string, number]>>();
  for (const address of view.second?.changedCells ?? []) {
    const [r, c] = places.get(address) ?? [-1, -1];
    changedInRow.set(r, [...(changedInRow.get(r) ?? []), [address, c]]);
  }
  return {
    view,
    first: view.first?.source ?? [],
    second,
    firstCorner: cornerOf(view.firstRange),
    secondCorner: cornerOf(view.secondRange),
    firstColumnOf: new Map((view.columns ?? []).flatMap((column) => (
      column.second === undefined || column.first === undefined ? [] : [[column.second, column.first] as const]))),
    changedInRow,
  };
}

/** A column added or removed, with its values. */
function columnChangeOf(table: ComparedTable, column: Types.ComparisonLine): string[] {
  if (column.status === "added" && column.second !== undefined) {
    const letter = letterOf(table.secondCorner.column + column.second);
    return [`column ${letter} added: ${columnValues(table.second, column.second)}`];
  }
  if (column.status === "removed" && column.first !== undefined) {
    const letter = letterOf(table.firstCorner.column + column.first);
    return [`old column ${letter} removed: ${columnValues(table.first, column.first)}`];
  }
  return [];
}

/** A row added or removed, with its values, or the cells of a changed row. */
function rowChangesOf(table: ComparedTable, row: Types.ComparisonLine): string[] {
  const { first, second, firstCorner, secondCorner } = table;
  if (row.status === "added" && row.second !== undefined) {
    return [`row ${secondCorner.row + row.second} added: ${rowValues(second[row.second])}`];
  }
  if (row.status === "removed" && row.first !== undefined) {
    return [`old row ${firstCorner.row + row.first} removed: ${rowValues(first[row.first])}`];
  }
  if (row.status !== "changed" || row.first === undefined || row.second === undefined) {
    return [];
  }
  const [earlier, later] = [row.first, row.second];
  return (table.changedInRow.get(later) ?? []).flatMap(([address, c]) => {
    const firstColumn = table.firstColumnOf.get(c);
    if (firstColumn === undefined) {
      return [];
    }
    const was = `${letterOf(firstCorner.column + firstColumn)}${firstCorner.row + earlier}`;
    const places = { address, was, row: rowKeyOf(second, later, c, table.view.name) };
    const change = cellChange(places, first[earlier]?.[firstColumn], second[later]?.[c]);
    return change === undefined ? [] : [change];
  });
}

/** Where a changed cell stands in the later version, and where it stood in the earlier one. */
interface CellPlaces {
  address: string;
  was: string;
  /** What names the cell within its table, such as `row Dog / Basic` or `$High$Factor`; empty for nothing. */
  row: string;
}

/** How many conditions left of a changed cell name the row of a rule at most. */
const KEY_VALUES = 3;

/** The kinds of tables whose rows are rules, told apart by the conditions that lead them. */
const RULE_KINDS = new Set(["rules", "simplerules", "smartrules"]);

/** The kinds of tables that look a value up by its row and its column. */
const LOOKUP_KINDS = new Set(["lookup", "simplelookup", "smartlookup"]);

/** The kinds of tables whose rows are a type and a name: a field of a datatype, a constant. */
const NAMED_KINDS = new Set(["datatype", "constants"]);

/**
 * What names a cell within its table. A rule is named by its conditions left of the cell, up to three, as
 * `row Dog / Basic`; a value of a lookup by its row and its column, as `row 6-10, column Cat`; a cell of a spreadsheet
 * as OpenL refers to it, `$High$Factor`; a field of a datatype or a constant by its name; a record or a test by its
 * first value. A row of the properties of a table is named by its property. A cell of the header row has none, nor
 * has a cell that names its row or its column itself.
 *
 * @param header the header of the table, whose first word is the kind of the table
 */
function rowKeyOf(source: Types.RawTableCell[][], row: number, column: number, header: string): string {
  if (row === 0) {
    return "";
  }
  if (shownAt(source, row, 0).toLowerCase() === "properties") {
    return `row property ${textOfCell(source[row]?.[1])}`;
  }
  const kind = header.split(" ", 1)[0]?.toLowerCase() ?? "";
  // A field of a datatype is its type and its name: the name names it, unless the name changed
  if (NAMED_KINDS.has(kind)) {
    return column === 1 ? "" : named([shownAt(source, row, 1)]);
  }
  if (column === 0) {
    return "";
  }
  if (kind === "spreadsheet") {
    return row === 1 ? "" : `$${shownAt(source, 1, column)}$${shownAt(source, row, 0)}`;
  }
  if (LOOKUP_KINDS.has(kind)) {
    return lookupKeyOf(source, row, column);
  }
  const keys = RULE_KINDS.has(kind) ? Math.min(column, KEY_VALUES) : 1;
  return named(Array.from({ length: keys }, (_, at) => shownAt(source, row, at)));
}

/** What names a cell of a lookup: the value of its row and the titles of its column; a title names nothing. */
function lookupKeyOf(source: Types.RawTableCell[][], row: number, column: number): string {
  // A title of a column names that column itself
  if (titleRowsOf(source, row).includes(row)) {
    return "";
  }
  const value = shownAt(source, row, 0);
  const title = columnTitleOf(source, row, column);
  return [value ? `row ${value}` : "", title ? `column ${title}` : ""].filter(Boolean).join(", ");
}

/** The values that name a row, as `row Dog / Basic`, or nothing when none of them holds a value. */
function named(values: string[]): string {
  const filled = values.filter(Boolean);
  return filled.length === 0 ? "" : `row ${filled.join(" / ")}`;
}

/**
 * The values that head a column of a lookup: of the row under the header, and of the next one too when it is a row of
 * the titles as well, the first cell of which the title of the conditions covers.
 */
function columnTitleOf(source: Types.RawTableCell[][], row: number, column: number): string {
  return titleRowsOf(source, row).map((line) => shownAt(source, line, column)).filter(Boolean).join(" / ");
}

/** The rows of the titles of the columns of a lookup: the row under the header, and the next one when it is a title too. */
function titleRowsOf(source: Types.RawTableCell[][], row: number): number[] {
  return row >= 2 && source[2]?.[0]?.covered ? [1, 2] : [1];
}

/** The value a cell shows: its own, or the one of the merged cell that covers it from above. */
function shownAt(source: Types.RawTableCell[][], row: number, column: number): string {
  const cell = source[row]?.[column];
  return textOfCell(cell?.covered ? mergedCellOver(source, row, column) : cell);
}

/** The merged cell that covers a cell, above it, left of it or both. */
function mergedCellOver(source: Types.RawTableCell[][], row: number, column: number): Types.RawTableCell | undefined {
  for (let line = row; line >= 0; line--) {
    const head = source[line]?.slice(0, column + 1)
      .findLast((cell, at) => !cell.covered && at + (cell.colspan ?? 1) > column && line + (cell.rowspan ?? 1) > row);
    if (head) {
      return head;
    }
  }
  return undefined;
}

/** What a cell spans: covered by a merged cell, merged over an area, or a cell of its own. */
function mergeOf(cell: Types.RawTableCell | undefined, address: string): string {
  if (cell?.covered) {
    return "covered by a merged cell";
  }
  const width = cell?.colspan ?? 1;
  const height = cell?.rowspan ?? 1;
  if (width === 1 && height === 1) {
    return "not merged";
  }
  const { row, column } = cornerOf(address);
  return `merged ${address}:${letterOf(column + width - 1)}${row + height - 1}`;
}

/**
 * A changed cell, named where it stands now and, when rows or columns moved it, where it stood: its text before and
 * after, else what it spans, else its note, which changed alone. A cell a merged cell covered in one version and left
 * empty in the other is told with the merged cell, not on its own.
 */
function cellChange(places: CellPlaces, before: Types.RawTableCell | undefined, after: Types.RawTableCell | undefined): string | undefined {
  const where = [places.was === places.address ? "" : `was ${places.was}`, places.row]
    .filter(Boolean);
  const name = where.length === 0 ? places.address : `${places.address} (${where.join(", ")})`;
  const was = textOfCell(before);
  const now = textOfCell(after);
  const kinds = [kindOfCell(before), kindOfCell(after)];
  // A value turned into another kind, as a text TRUE into a boolean, is told with its kinds, which may read alike
  const kindChanged = kinds[0] !== kinds[1] && !kinds.includes("empty");
  if (kindChanged) {
    return `${name}: ${was} (${kinds[0]}) → ${now} (${kinds[1]})`;
  }
  if (was !== now) {
    return `${name}: ${changed(was)} → ${changed(now)}`;
  }
  const merges = [mergeOf(before, places.was), mergeOf(after, places.address)];
  const partOfAMerge = was === "" && merges.includes("covered by a merged cell") && merges.includes("not merged");
  if (merges[0] !== merges[1]) {
    return partOfAMerge ? undefined : `${name}: ${merges[0]} → ${merges[1]}`;
  }
  const notes = [before?.comment ?? "", after?.comment ?? ""];
  return notes[0] === notes[1] ? undefined : `${name}: note '${notes[0]}' → '${notes[1]}'`;
}

/** A table only one workbook holds, row by row. */
function wholeTable(view: Types.ComparisonTableView): string[] {
  const added = view.status === "added";
  const source = (added ? view.second : view.first)?.source ?? [];
  const corner = cornerOf(added ? view.secondRange : view.firstRange);
  return source.map((row, index) => `row ${corner.row + index}: ${rowValues(row)}`);
}

/** A table that differs, under its sheet and name: where it moved, whether its formatting changed, and its changes. */
function tableSection(sheet: string, view: Types.ComparisonTableView, shown: Shown): string {
  const notes: string[] = [];
  if (view.moved && view.firstRange && view.secondRange) {
    notes.push(`Moved from ${view.firstRange} to ${view.secondRange}.`);
  }
  if (view.formattingChanged) {
    notes.push("Formatting changed.");
  }
  const name = nameOf(view.name, shown, (view.second ?? view.first)?.source, view.secondRange ?? view.firstRange);
  return [`### ${sheet} / ${name}: ${statusOf(view)}`, ...notes, ...shownChanges(changeLinesOf(view), shown)].join("\n");
}

/** What became of a table, with the range of a table only one version holds. */
function statusOf(view: Types.ComparisonTableView): string {
  const range = view.status === "added" ? view.secondRange : view.firstRange;
  const whole = view.status === "added" || view.status === "removed";
  return whole && range ? `${view.status} at ${range}` : view.status;
}

/** The changes of a table: row by row for a table one workbook holds, else its lines and cells that differ. */
function changeLinesOf(view: Types.ComparisonTableView): string[] {
  return view.status === "added" || view.status === "removed" ? wholeTable(view) : changesOf(view);
}

/**
 * The changes of a table as list items. A table told among others lists its first ones and counts the rest, which the
 * table alone on a page lists.
 */
function shownChanges(changes: string[], shown: Shown): string[] {
  const listed = shown.whole ? changes : changes.slice(0, CHANGES_PER_TABLE);
  const items = listed.map((change) => `- ${change}`);
  if (listed.length < changes.length) {
    items.push(`- … ${formatCount(changes.length - listed.length, "more change")}: offset=${shown.index} with limit=1 lists them all.`);
  }
  return items;
}

/** A version of a text file, empty for a version without it; undefined for a file that is not text. */
async function textAt(client: OpenLClient, request: FileComparisonRequest, version: Version): Promise<string | undefined> {
  if (!version.holds) {
    return "";
  }
  return textOf(await client.readProjectFile(request.projectId, request.path, { version: version.revision }));
}

async function compareText(
  client: OpenLClient,
  request: FileComparisonRequest,
  [first, second]: [Version, Version],
  title: string,
): Promise<string> {
  const [before, after] = await Promise.all([textAt(client, request, first), textAt(client, request, second)]);
  if (before === undefined || after === undefined) {
    return `${title}\n\nThe file is not text, so its versions are not compared line by line.`;
  }
  const diff = unifiedDiff(before, after);
  if (diff === undefined) {
    return `${title}\n\nThe file is too long to compare line by line: read both versions with openl_read_project_file.`;
  }
  if (diff.length === 0) {
    const same = before === after ? "Both versions read the same" : "Only the line ends changed, such as the line break at the end of the file";
    return `${title}\n\n${same}.`;
  }
  return fitLines({
    head: [title, "", "```diff"],
    lines: diff,
    tail: ["```"],
    more: (left) => `… ${formatCount(left, "more line")} of the diff.`,
  });
}
