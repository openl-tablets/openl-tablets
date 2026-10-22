import type {
    RawTableCellBorder,
    RawTableCellBorderSide,
    RawTableRegion,
    RawTableSignaturePiece,
    RawTableValueType,
    RawTableView,
} from 'types/tables'

/** How many rows of a table one bucket of the index of its regions spans. */
const BUCKET = 32

/** A cell named in A1 notation: the letters of its column, then the number of its row. */
const ADDRESS = /^([A-Z]+)(\d+)$/

/** The row and the column of a cell of a sheet, counted from 0. */
interface Place {
    row: number
    column: number
}

/** The place of a cell of a sheet named in A1 notation, or undefined for a text naming no cell. */
export const placeOf = (address: string | undefined): Place | undefined => {
    const match = address === undefined ? null : ADDRESS.exec(address)
    if (match === null) {
        return undefined
    }
    const [, letters = '', digits = ''] = match
    let column = 0
    for (const letter of letters) {
        column = column * 26 + (letter.codePointAt(0) ?? 64) - 64
    }
    return { row: Number(digits) - 1, column: column - 1 }
}

/** The letters Excel names a column of a sheet by, the column counted from 0: `A` to `Z`, then `AA`. */
export const columnLettersOf = (column: number): string => {
    let letters = ''
    for (let rest = column + 1; rest > 0; rest = Math.floor((rest - 1) / 26)) {
        letters = String.fromCodePoint(65 + (rest - 1) % 26) + letters
    }
    return letters
}

/**
 * The roles of the cells of a table, read once with the table, and where each cell of it stands in the table.
 *
 * A cell is found by its address, so the rows of the table may come in windows, with the header kept out of sight, or
 * with rows a reader inserted that the server has not read yet: a cell with no address takes no role.
 */
export interface TableRoles {
    /** The regions of the roles, in table coordinates. */
    regions: RawTableRegion[]
    /** The rows that start a group, in table coordinates. */
    groupRows: ReadonlySet<number>
    /** The columns that start a group, in table coordinates. */
    groupColumns: ReadonlySet<number>
    /** The pieces of the header text. */
    signature: RawTableSignaturePiece[]
    /** The language the code of the table is written in: the steps of a TBasic table, or an OpenL expression. */
    language: 'openl' | 'tbasic'
    /** Where the table starts on its sheet. */
    origin: Place
    /** The last row of the table, in table coordinates. */
    bottom: number
    /** The regions reaching each bucket of rows of the table. */
    buckets: RawTableRegion[][]
}

/**
 * The roles of the cells of a table a read reports, or undefined for a table read with no roles: a table of the
 * kind Other, or a read of the values alone.
 */
export const rolesOf = (view: Pick<RawTableView, 'kind' | 'pos' | 'regions' | 'groups' | 'signature'>):
    TableRoles | undefined => {
    const origin = placeOf(view.pos?.split(':')[0])
    if (view.regions === undefined || view.regions.length === 0 || origin === undefined) {
        return undefined
    }
    const bottom = Math.max(...view.regions.map(region => region.bottom))
    const buckets: RawTableRegion[][] = Array.from({ length: Math.floor(bottom / BUCKET) + 1 }, () => [])
    for (const region of view.regions) {
        for (let bucket = Math.floor(region.top / BUCKET); bucket <= Math.floor(region.bottom / BUCKET); bucket++) {
            buckets[bucket]?.push(region)
        }
    }
    return {
        regions: view.regions,
        groupRows: new Set(view.groups?.rows ?? []),
        groupColumns: new Set(view.groups?.columns ?? []),
        signature: view.signature ?? [],
        language: view.kind === 'TBasic' ? 'tbasic' : 'openl',
        origin,
        bottom,
        buckets,
    }
}

/**
 * Whether a table is drawn in the formatting of its workbook, on the paper of a workbook: a table read with no roles,
 * and every table while the reader has Excel Formatting on. Any other table is drawn in the Studio look.
 */
export const onPaper = (roles: TableRoles | undefined, excelFormatting: boolean): boolean =>
    excelFormatting || roles === undefined

/** Where a cell stands in its table, or undefined for a cell with no address. */
export const tablePlaceOf = (roles: TableRoles, address: string | undefined): Place | undefined => {
    const place = placeOf(address)
    return place === undefined
        ? undefined
        : { row: place.row - roles.origin.row, column: place.column - roles.origin.column }
}

/** The region holding a place of the table, or undefined for a place no region holds. */
export const regionAt = (roles: TableRoles, place: Place): RawTableRegion | undefined =>
    roles.buckets[Math.floor(place.row / BUCKET)]?.find(region => place.row >= region.top
        && place.row <= region.bottom && place.column >= region.left && place.column <= region.right)

/** What a cell is in the Studio look: its region, where it stands, and the type of its value. */
export interface CellRole {
    region: RawTableRegion
    place: Place
    /** The type of the value: the cell's own, or the type of its region. */
    type: RawTableValueType | undefined
}

/** What a cell is, or undefined for a cell that takes no role. */
export const cellRoleOf = (roles: TableRoles, address: string | undefined, own: RawTableValueType | undefined):
    CellRole | undefined => {
    const place = tablePlaceOf(roles, address)
    const region = place === undefined ? undefined : regionAt(roles, place)
    return place === undefined || region === undefined ? undefined : { region, place, type: own ?? region.type }
}

/**
 * Whether a cell holds code, which the Studio look highlights as the code editor does: a declaration the compiler
 * reads, or a value the compiler reads as an expression.
 */
export const holdsCode = (cell: CellRole): boolean => cell.region.role === 'code' || cell.type === 'code'

/** The lines the Studio look draws, in the colours of the theme, as the Excel style draws them. */
export interface RoleLines {
    /** The line of a role: above the signature, the properties, the code, the titles, a heading and the result. */
    role: RawTableCellBorderSide
    /** The line above the first line of a group, and on the left of the first column of one. */
    group: RawTableCellBorderSide
    /** The double line below what a table returns. */
    resultBelow: RawTableCellBorderSide
}

/** The lines a role draws above the first row and below the last row of its region, as its Excel style does. */
const ROLE_EDGES: Readonly<Record<string, { top: keyof RoleLines, bottom?: keyof RoleLines }>> = {
    signature: { top: 'role', bottom: 'role' },
    properties: { top: 'role', bottom: 'role' },
    code: { top: 'role', bottom: 'role' },
    inputTitle: { top: 'role' },
    outputTitle: { top: 'role' },
    section: { top: 'role' },
    result: { top: 'role', bottom: 'resultBelow' },
}

/** The parts of the header, which the line on the left of a group column does not cross. */
const HEADER_PARTS: ReadonlySet<string> = new Set(['signature', 'properties', 'code'])

/**
 * The lines the Studio look draws around a cell, by its role and where it stands: the line of a role above the first
 * row of the region of the signature, the properties, the code, the titles, a heading and the result — a lookup has a
 * region for each row of its horizontal values — and below the last row of the signature, the properties and the
 * code, a double line below the result, a group line above the first line of a group and on the left of a group
 * column from the titles down, and the line of a role closing the table. No other line is drawn: the parts of a table
 * are told apart by their fill.
 *
 * @param roles  the roles of the cells of the table
 * @param cell   what the cell is
 * @param span   how many rows and columns the cell takes
 * @param lines  the lines of the theme
 * @returns the lines on the sides of the cell, or undefined for none
 */
export const roleLinesOf = (roles: TableRoles, cell: CellRole, span: { rows: number, columns: number },
    lines: RoleLines): RawTableCellBorder | undefined => {
    const { region, place } = cell
    const lastRow = place.row + span.rows - 1
    const border: RawTableCellBorder = {}
    const edges = ROLE_EDGES[region.role]
    if (edges && place.row === region.top) {
        border.top = lines[edges.top]
    }
    if (edges?.bottom && lastRow === region.bottom) {
        border.bottom = lines[edges.bottom]
    }
    if (border.top === undefined && roles.groupRows.has(place.row)) {
        border.top = lines.group
    }
    if (roles.groupColumns.has(place.column) && !HEADER_PARTS.has(region.role)) {
        border.left = lines.group
    }
    if (border.bottom === undefined && lastRow >= roles.bottom) {
        border.bottom = lines.role
    }
    return Object.keys(border).length === 0 ? undefined : border
}
