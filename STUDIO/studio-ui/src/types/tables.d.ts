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
export interface ProjectProperty {
    name: string
    type: 'text' | 'date' | 'boolean' | 'enum'
    /** Several values, written separated by commas. */
    multiple: boolean
    /** Values the property accepts, empty unless the type is an enum. */
    values: string[]
}
