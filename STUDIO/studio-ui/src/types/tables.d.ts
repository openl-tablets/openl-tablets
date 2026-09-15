import type { ProjectStatusDetailedMessage } from '../services/projectStatus'

import type { TraceParameterValue } from './trace'

/** Excel cell style read from the workbook; every field is optional and absent when it is the default. */
export interface RawTableCellStyle {
    /** Background colour as #rrggbb (absent when white) */
    background?: string
    /** Font colour as #rrggbb (absent when black) */
    color?: string
    /** Horizontal alignment */
    align?: string
    /** Vertical alignment */
    valign?: string
    bold?: boolean
    italic?: boolean
    underline?: boolean
    indent?: number
}

/** One cell of a raw table grid (Tables API `?raw=true`). */
export interface RawTableCell {
    /** Cell address in A1 notation; absent on covered cells */
    cell?: string
    /** Typed cell value (number, string, boolean), or absent when empty */
    value?: string | number | boolean | null
    /** The formula the cell was written with, as Excel writes it (`=B2*C2`); absent for a plain value */
    formula?: string
    /** The note a reader left on the cell in Excel; absent when the cell carries none */
    comment?: string
    /** Number of columns this cell spans (>= 2), when merged */
    colspan?: number
    /** Number of rows this cell spans (>= 2), when merged */
    rowspan?: number
    /** True for a cell masked by another cell's span */
    covered?: boolean
    /** Excel cell style, present only when the raw table was requested with `styles=true` */
    style?: RawTableCellStyle
    /** What the compiler knows about the cell, present only when the read asked with `metaInfo=true` */
    metaInfo?: RawTableCellMetaInfo
}

/** What a piece of a cell's text refers to, as the compiler read it. */
export type RawTableUsageKind = 'rule' | 'datatype' | 'data' | 'field' | 'underlined' | 'other'

/** One piece of a cell's text the compiler resolved to something. */
export interface RawTableCellUsage {
    /** Index of the first character of the cell's text the usage covers */
    start: number
    /** Index after the last character it covers */
    end: number
    /** What the compiler says about it, shown as a tooltip */
    description: string
    /** The table it refers to, as the Tables API addresses it; absent when it refers to no table */
    tableId?: string
    /** The module that table is read through; absent when no module of the workspace holds it */
    module?: string
    /** The project that module belongs to, which for a table of a dependency is not the one being read */
    projectId?: string
    kind: RawTableUsageKind
}

/** What the compiler knows about one cell, beside what the cell says. */
export interface RawTableCellMetaInfo {
    /** The pieces of the cell's text that refer to something, in the order they appear */
    usages?: RawTableCellUsage[]
    /** The type the cell holds, as the compiler names it */
    type?: string
    /** True when this is the cell a decision table returns */
    returnCell?: boolean
    /** The editor the cell asks for */
    editor?: string
}

/** The styling to set on cells; an attribute left out is not touched. */
export interface RawCellStyleInput {
    /** Background colour as #rrggbb */
    background?: string
    /** Font colour as #rrggbb */
    color?: string
    /** Horizontal alignment; `left` puts the cells back to the default */
    align?: 'left' | 'center' | 'right' | 'justify'
    bold?: boolean
    italic?: boolean
    underline?: boolean
    /** Left indent in Excel indent units; 0 takes the indent away */
    indent?: number
}

/**
 * One edit of a table's raw source, as the Tables API takes it.
 *
 * <p>`operation` selects the edit and the target's `type` the resource it acts on. A sequence of these is sent
 * together, each addressing the table as the previous one left it, and the table is written once.
 */
export type TableEdit =
    | { operation: 'update', target: { type: 'cell', row: number, column: number, value: string | number | boolean | null } }
    | { operation: 'insert', target: { type: 'rows', position: number, cells: RawTableCellInput[][] } }
    | { operation: 'insert', target: { type: 'columns', position: number, cells: RawTableCellInput[][] } }
    | { operation: 'delete', target: { type: 'rows', position: number, count: number } }
    | { operation: 'delete', target: { type: 'columns', position: number, count: number } }
    | {
        operation: 'style'
        target: {
            type: 'cells'
            row: number
            column: number
            rowspan: number
            colspan: number
            style: RawCellStyleInput
        }
    }

export interface RawTableCellInput {
    value: string | number | boolean | null
    colspan?: number
    rowspan?: number
    covered?: boolean
}

export interface RawTable {
    tableType: 'RawSource'
    kind: string
    name: string
    source: RawTableCellInput[][]
}

export interface CreateTableRequest {
    moduleName: string
    sheetName?: string
    modulePath?: string
    table: RawTable
}

/** A table property in display string form; a blank value removes the property from a copy. */
export interface TableProperty {
    name: string
    value: string | null
}

/** The versions of one table: the one it stands for, the one offered next, and the ones already taken. */
export interface TableVersions {
    /** Version the table stands for; `0.0.1` while it declares none. */
    current: string
    /** First free version after the current one. */
    next: string
    /** Versions already carried by the table's versions, the current one included. */
    taken: string[]
}

/** A table's name, kind and its own properties — read cheaply for the copy dialog, without the body. */
export interface TableCopyInfo {
    name: string
    kind: string
    /** Absent when the table declares no properties (the backend omits an empty list). */
    properties?: TableProperty[]
    /** Absent for a kind of table that carries no versions, such as a Datatype or a Data table. */
    versions?: TableVersions
}

/** Copy an existing table (named by its id in the path) into a module of the same project. */
export interface CopyTableRequest {
    moduleName: string
    sheetName?: string
    modulePath?: string
    name: string
    properties?: TableProperty[]
}

export interface SummaryTable {
    id: string
    tableType: string
    kind: string
    name: string
}

/** A table of the project, as its list reports it. */
export interface ProjectTable {
    id: string
    /** The keyword the table is written with: `Datatype`, `Vocabulary`, `SimpleRules`, ... */
    tableType: string
    name: string
    /** What the table returns, absent unless its header declares a method. `void` for a table returning nothing. */
    returnType?: string
    /** The header text after the return type, as the compiler reads it: `Premium(Policy policy, Integer age)`. */
    signature?: string
}

/**
 * A table of one module, as the editor's tree reads it.
 *
 * The tree is grouped in the browser, so the list carries everything a grouping can be built from rather than a
 * shape the server chose.
 */
export interface ModuleTable extends ProjectTable {
    /** The family the table belongs to: `Rules`, `Spreadsheet`, `Datatype`, `Test`, ... */
    kind: string
    /** Workbook the table is written in, relative to the workspace. */
    file?: string
    /** Excel sheet the table is written on — what the tree groups by when it opens. */
    sheet?: string
    /** Where the table sits in the workbook, in A1 notation: `B3:D8`. */
    pos?: string
    /** The properties the table declares, `category` among them. */
    properties?: Record<string, unknown>
    /**
     * The name that tells this version of the table from the others, carrying what they are told apart by:
     * `CarPrice [effectiveDate=01/01/2020]`. Absent unless the table is written in more than one version.
     */
    displayName?: string
    /** What the versions of one table share, and no other table carries. Absent unless there is more than one. */
    overloadGroup?: string
    /** `false` on a table switched off by the `active` property, which takes no part in the rules. */
    active?: boolean
    /** How many errors the compilation raised about this table; absent when it raised none. */
    errors?: number
    /** `true` when a test table exercises this one; absent when nothing tests it. */
    hasTests?: boolean
    /** Module the table is written in; answered by a search that spans more than the module it was asked through. */
    module?: string
    /** Name of the project that module belongs to, answered with the module. */
    project?: string
    /** Identifier of that project, as the Projects API addresses it. */
    projectId?: string
}

/**
 * Whether the table can be run as it stands, and how far a run of it may reach.
 *
 * A table the compiler could not build runs nothing, and neither does a test whose rules failed; where only
 * the open module is built, or what is built beyond it has errors, a run stays inside that module.
 */
export type TableRunState = 'can-run' | 'can-run-module' | 'cannot-run'

/** A table in raw tabular form: a 2D matrix of cells with merge geometry. */
export interface RawTableView {
    id: string
    name: string
    /** The table body as a 2D matrix indexed source[row][col] */
    source: RawTableCell[][]
    /** Full row count when the response was truncated by maxRows; absent when the whole table is returned */
    totalRows?: number
    /** How many rows at the top of the table its header takes, which a screen hiding the header leaves out */
    headerHeight?: number
    /** What the compiler said about this table — the errors and warnings it raised, if any */
    messages?: ProjectStatusDetailedMessage[]
    /** Whether the table can be run, and how far; absent while the read that carries it is on its way. */
    runState?: TableRunState
    /**
     * `true` when the table is written as several partial tables gathered from more than one place in the
     * workbook. Such a table is read here but not written — only Excel can edit the cells it is drawn from.
     */
    partial?: boolean
}

/** One field a Datatype table declares. */
export interface DatatypeField {
    name: string
    type: string
}

/** A Datatype table as the modal reads it — a vocabulary included, which is written with the same keyword. */
export interface ProjectDatatype {
    /** The datatype this one inherits its first fields from, if any. */
    extends?: string
    fields: DatatypeField[]
    /** The values a vocabulary accepts, written as text. Empty for a datatype, which declares fields instead. */
    values: string[]
}

/** A property a table may declare, and what a value for it looks like. */
export interface ProjectPropertyValue {
    /** Value written to the table. */
    code: string
    /** Value shown to the author. */
    value: string
}

export interface ProjectProperty {
    name: string
    /** Name a business user reads the property by, the way Table Details names it. */
    displayName: string
    /** Group the property is listed under: Info, Business Dimension, Version or Dev. */
    group: string
    type: 'text' | 'date' | 'boolean' | 'enum'
    /** Several values, written separated by commas. */
    multiple: boolean
    /** Whether the engine dispatches on the property. */
    dimensional: boolean
    /** Value the property stands for while a table declares none. */
    defaultValue: string | null
    /** Regular expression a value must match, when the property states one. */
    pattern: string | null
    /** Values the property accepts, empty unless the type is an enum. */
    values: ProjectPropertyValue[]
}

/** One case of a test table. Its id, its description and the values of its columns. */
export interface TableInputTestCase {
    id: string
    description?: string | null
    /**
     * The context columns first, then the input columns of the tested rule. Each is named by its column.
     *
     * In a page of cases a value with inner structure is lazy and left out. A case read on its own carries every
     * value.
     */
    parameters: TraceParameterValue[]
}

/**
 * The input a table takes to be executed.
 *
 * A rule table declares parameters, each with the JSON schema of the values it accepts. A test table carries
 * cases instead, which are read a page at a time.
 */
export interface TableInput {
    tableId: string
    name: string
    /** Whether the table is a test table. A test table carries cases, a rule table declares parameters. */
    testTable: boolean
    /** Declared parameters of a rule table. Absent when there are none, as every empty list of the API is. */
    parameters?: TraceParameterValue[]
    /** Schema of the runtime context, present when the project provides one to its rules. */
    runtimeContext?: TraceParameterValue | null
}

/** One page of the cases of a test table, with the number of cases the table holds. */
export interface TableInputCasesPage {
    content: TableInputTestCase[]
    pageNumber?: number
    pageSize?: number
    numberOfElements?: number
    total: number
}
