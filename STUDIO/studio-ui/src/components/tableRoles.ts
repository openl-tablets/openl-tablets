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

/** The lines the Studio look draws, in the colours of the theme. */
export interface RoleLines {
    /** A line that sets the sections of a table apart: above and below the signature, below the body. */
    major: RawTableCellBorderSide
    /** A line that sets a part apart within a section: below the properties and a heading, above a group. */
    minor: RawTableCellBorderSide
    /** The line above what a table returns. */
    resultAbove: RawTableCellBorderSide
    /** The double line below what a table returns. */
    resultBelow: RawTableCellBorderSide
}

/**
 * The lines the Studio look draws around a cell, by its role and where it stands: the signature between two major
 * lines, a minor line under the properties and under a heading, the result between its own lines, a minor line above
 * the first line of a group, and a major line closing the table. No other line is drawn: the parts of a table are
 * told apart by their fill.
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
    if (region.role === 'signature') {
        border.top = lines.major
        border.bottom = lines.major
    } else if ((region.role === 'properties' || region.role === 'section') && lastRow === region.bottom) {
        border.bottom = lines.minor
    } else if (region.role === 'result') {
        if (place.row === region.top) {
            border.top = lines.resultAbove
        }
        if (lastRow === region.bottom) {
            border.bottom = lines.resultBelow
        }
    }
    if (border.top === undefined && roles.groupRows.has(place.row)) {
        border.top = lines.minor
    }
    if (roles.groupColumns.has(place.column)) {
        border.left = lines.minor
    }
    if (border.bottom === undefined && lastRow >= roles.bottom) {
        border.bottom = lines.major
    }
    return Object.keys(border).length === 0 ? undefined : border
}
