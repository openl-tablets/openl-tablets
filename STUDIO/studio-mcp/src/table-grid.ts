/**
 * The one shape of a table the table tools read and write: its rows of plain values, and the merged cells, the Excel
 * formulas, the notes and the styles of its cells listed apart by their positions in the table. Studio reads and
 * writes a matrix of cell objects instead, which spends most of a table on wrapping each value.
 *
 * A position is `[row, column]` from 0 in the whole table, the header row and the label column included, also in a
 * slice of its rows: the positions the edit actions take.
 */

import { ProtocolError, ProtocolErrorCode } from "@modelcontextprotocol/server";

import type * as Types from "./types.js";

/** A value of a cell: a scalar, a one-dimensional array of scalars, or null for an empty or covered cell. */
type GridValue = string | number | boolean | null | Array<string | number | boolean | null>;

/** A merged cell: its first row and column, and how many rows and columns it spans. */
type GridMerge = [row: number, column: number, rowspan: number, columns: number];

/** Something a cell holds beside its value, such as its Excel formula, by the position of the cell. */
type GridEntry<T> = [row: number, column: number, entry: T];

/** The cells of a table, as the table tools read and write them. */
interface Grid {
  /** Every row the answer holds, one value per column; a row ends at its last value that is not null. */
  rows: GridValue[][];
  merges?: GridMerge[];
  /** The Excel formulas of the cells whose value they compute. */
  formulas?: Array<GridEntry<string>>;
  /** The Excel notes of the cells, which a write keeps. */
  notes?: Array<GridEntry<string>>;
  /** The Excel styles of the cells, read when asked for; a write keeps them. */
  styles?: Array<GridEntry<Types.RawTableCellStyle>>;
}

/** What a grid lists of its cells beside their values. */
type Entries = Omit<Grid, "rows">;

/** The lists of entries that hold any: an empty one tells nothing. */
function nonEmpty(entries: Entries): Entries {
  return Object.fromEntries(Object.entries(entries).filter(([, list]) => (list?.length ?? 0) > 0)) as Entries;
}

/** The formulas, notes and styles of a grid in the rows `keep` keeps. */
function entriesIn(grid: Grid, keep: (row: number) => boolean): Entries {
  return {
    formulas: grid.formulas?.filter(([row]) => keep(row)),
    notes: grid.notes?.filter(([row]) => keep(row)),
    styles: grid.styles?.filter(([row]) => keep(row)),
  };
}

/** The cells of a table in the shape of the table tools, from the rows of Studio's matrix starting at row `offset`. */
export function gridOf(source: Types.RawTableCell[][], offset: number, pos: string | undefined): Grid {
  const origin = pos === undefined ? undefined : firstCellOf(pos);
  const merges: GridMerge[] = [];
  const formulas: Array<GridEntry<string>> = [];
  const notes: Array<GridEntry<string>> = [];
  const styles: Array<GridEntry<Types.RawTableCellStyle>> = [];
  const rows = source.map((cells, index) => {
    const row = offset + index;
    const values = cells.map((cell, column): GridValue => {
      const area = cell.mergedArea && origin ? areaIn(cell.mergedArea, origin) : undefined;
      if (area) {
        merges.push(area);
      } else if ((cell.rowspan ?? 1) > 1 || (cell.colspan ?? 1) > 1) {
        merges.push([row, column, cell.rowspan ?? 1, cell.colspan ?? 1]);
      }
      if (cell.formula !== undefined) formulas.push([row, column, cell.formula]);
      if (cell.comment !== undefined) notes.push([row, column, cell.comment]);
      if (cell.style !== undefined && Object.keys(cell.style).length > 0) styles.push([row, column, cell.style]);
      return cell.covered ? null : cell.value ?? null;
    });
    // The trailing empty cells of a row tell nothing the position of the table does not
    let length = values.length;
    while (length > 0 && values[length - 1] === null) length--;
    return values.slice(0, length);
  });
  return { rows, ...nonEmpty({ merges, formulas, notes, styles }) };
}

/**
 * The first rows of a grid read from row `offset`: what the merges, formulas, notes and styles of the rows left out
 * say is left out with them, while a merge reaching past the last row kept keeps its whole area.
 */
export function leadingRows(grid: Grid, offset: number, count: number): Grid {
  const kept = (row: number): boolean => row < offset + count;
  return {
    rows: grid.rows.slice(0, count),
    ...nonEmpty({ merges: grid.merges?.filter(([row]) => kept(row)), ...entriesIn(grid, kept) }),
  };
}

/**
 * Studio's matrix of a whole table from its grid, `width` columns wide at least: a merged cell names its span on its
 * first cell and covers the others, and a cell with a formula is written as the formula. Notes and styles are not
 * written: Studio keeps them. A formula outside the table is refused: it names no cell to write.
 */
export function sourceOf(grid: Pick<Grid, "rows" | "merges" | "formulas">, width = 0): Types.RawCellInput[][] {
  const merges = grid.merges ?? [];
  const height = Math.max(grid.rows.length, ...merges.map(([row, , rowspan]) => row + rowspan));
  const columns = Math.max(width, ...grid.rows.map((row) => row.length), ...merges.map(([, column, , colspan]) => column + colspan));
  const source: Types.RawCellInput[][] = Array.from({ length: height }, (_, row) =>
    Array.from({ length: columns }, (_, column) => ({ value: grid.rows[row]?.[column] ?? null })));
  for (const [row, column, rowspan, colspan] of merges) {
    for (let r = row; r < row + rowspan; r++) {
      for (let c = column; c < column + colspan; c++) {
        source[r][c] = { covered: true };
      }
    }
    source[row][column] = {
      value: grid.rows[row]?.[column] ?? null,
      ...(rowspan > 1 ? { rowspan } : {}),
      ...(colspan > 1 ? { colspan } : {}),
    };
  }
  for (const [row, column, formula] of grid.formulas ?? []) {
    if (row >= height || column >= columns) {
      throw new ProtocolError(ProtocolErrorCode.InvalidParams, `The formula at [${row}, ${column}] lies outside the table ` +
        `of ${height} rows and ${columns} columns: place it at a cell of rows or of a merge.`);
    }
    const { value: _, ...cell } = source[row][column];
    source[row][column] = { ...cell, formula };
  }
  return source;
}

/** The number of columns of a table at an A1 position such as `B2:E920`, or 0 when it cannot be read. */
export function widthOf(pos: string | undefined): number {
  const range = pos === undefined ? undefined : rangeOf(pos);
  return range ? range.lastColumn - range.firstColumn + 1 : 0;
}

/** The column and the row of the first cell of an A1 position, such as `B2` of `B2:E920`, from 1. */
function firstCellOf(pos: string): { column: number; row: number } | undefined {
  const match = /^([A-Z]+)(\d+)/.exec(pos);
  return match ? { column: columnIndex(match[1]), row: Number(match[2]) } : undefined;
}

/** A merged area in A1 notation, such as `B4:B299`, as a merge at its position in the table that starts at `origin`. */
function areaIn(area: string, origin: { column: number; row: number }): GridMerge | undefined {
  const range = rangeOf(area);
  return range && [
    range.firstRow - origin.row,
    range.firstColumn - origin.column,
    range.lastRow - range.firstRow + 1,
    range.lastColumn - range.firstColumn + 1,
  ];
}

/** The first and the last row and column of an A1 range such as `B4:B299`, from 1. */
function rangeOf(range: string): { firstColumn: number; firstRow: number; lastColumn: number; lastRow: number } | undefined {
  const match = /^([A-Z]+)(\d+):([A-Z]+)(\d+)$/.exec(range);
  return match
    ? { firstColumn: columnIndex(match[1]), firstRow: Number(match[2]), lastColumn: columnIndex(match[3]), lastRow: Number(match[4]) }
    : undefined;
}

/** The index of an Excel column from its letters, from 1: A is 1, Z 26, AA 27. */
function columnIndex(letters: string): number {
  return [...letters].reduce((index, letter) => index * 26 + (letter.codePointAt(0) ?? 64) - 64, 0);
}
