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
