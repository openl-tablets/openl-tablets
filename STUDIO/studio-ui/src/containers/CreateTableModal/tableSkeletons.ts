import type { DatatypeField } from 'types/tables'

/** Every table type the modal offers, in the order it presents them. */
export const TABLE_PRESETS = [
    'datatype',
    'vocabulary',
    'constants',
    'spreadsheet',
    'smartRules',
    'simpleRules',
    'smartLookup',
    'simpleLookup',
    'rules',
    'test',
    'run',
    'data',
    'environment',
    'properties',
    'freeForm',
] as const

export type TablePreset = typeof TABLE_PRESETS[number]

export type TableCellValue = string | boolean

export interface TableArgument {
    type: string
    name: string
}

export type TableCellEditor =
    'text' | 'type' | 'simpleType' | 'value' | 'checkbox' | 'environment' | 'property'

export interface TableColumn {
    key: string
    label: string
    editor: TableCellEditor
}

/** One column of a Test or Run table: a value the tested table's signature asks a call to supply. */
export interface TargetColumn {
    /** The path OpenL reads the value back with: `policy`, or `policy.mainDriver.age` for a field of a datatype. */
    name: string
    title: string
    /** The declared type of the value, which is what the example cell is written from. */
    type: string
}

export interface TableBuildContext {
    resultType: string
    resultFields: DatatypeField[]
    arguments: TableArgument[]
    vocabularyType: string
    extendsType: string
    datatypeName: string
    dataFields: DatatypeField[]
    targetName: string
    targetColumns: TargetColumn[]
    /** Every value each vocabulary offers, by vocabulary name. The first one seeds an example cell. */
    vocabularyValues: Record<string, string[]>
}

/**
 * Scalar types offered for a cell or a signature.
 *
 * <p>OpenL also understands the Java primitives, but a rules table has no use for a type that cannot be left empty,
 * so only the boxed forms are listed. Fixed by the language, so the list lives here rather than being fetched.
 */
export const SIMPLE_TYPES: string[] = [
    'String',
    'Boolean',
    'Integer',
    'Long',
    'Short',
    'Byte',
    'Double',
    'Float',
    'BigInteger',
    'BigDecimal',
    'Character',
    'Date',
    'IntRange',
    'DoubleRange',
]

/** The keywords an Environment table accepts. Fixed by the parser, which rejects anything else. */
export const ENVIRONMENT_KEYS: string[] = ['dependency', 'import', 'include']

/**
 * Result type a signature starts with. A Spreadsheet returns the result object OpenL builds from its steps, so it
 * defaults to SpreadsheetResult rather than to the scalar every other table starts with.
 */
export const defaultResultType = (preset: TablePreset): string =>
    preset === 'spreadsheet' ? 'SpreadsheetResult' : 'Boolean'

/** The keyword each table type that declares a method signature opens its header with. */
const SIGNATURE_KEYWORDS = {
    spreadsheet: 'Spreadsheet',
    smartRules: 'SmartRules',
    simpleRules: 'SimpleRules',
    smartLookup: 'SmartLookup',
    simpleLookup: 'SimpleLookup',
    rules: 'Rules',
} as const satisfies Partial<Record<TablePreset, string>>

export const SIGNATURE_PRESETS: ReadonlySet<TablePreset> = new Set(
    Object.keys(SIGNATURE_KEYWORDS) as TablePreset[]
)

const completeArguments = (argumentsValue: TableArgument[]): TableArgument[] =>
    argumentsValue.filter(argument => argument.type.trim() && argument.name.trim())

const functionHeader = (
    keyword: string,
    name: string,
    resultType: string,
    argumentsValue: TableArgument[]
): string => {
    const signature = completeArguments(argumentsValue)
        .map(argument => `${argument.type.trim()} ${argument.name.trim()}`)
        .join(', ')
    return `${keyword} ${resultType.trim() || 'Boolean'} ${name}(${signature})`
}

/**
 * Tells whether the table type is named in its header.
 *
 * <p>A Constants, Properties or Environment table declares no name — OpenL identifies it by its keyword alone. A
 * Free Form table has no name either: its header is whatever text the author types.
 */
export const hasTableName = (preset: TablePreset): boolean =>
    preset !== 'constants' && preset !== 'properties' && preset !== 'environment' && preset !== 'freeForm'

/**
 * Tells whether the table type is written against another table. Test and Run both name a tested table.
 *
 */
export const isTargeted = (preset: TablePreset): preset is 'test' | 'run' => preset === 'test' || preset === 'run'

/** Test, Run, and Data tables may put their declared fields down rows instead of across columns. */
export const isTransposable = (preset: TablePreset): preset is 'test' | 'run' | 'data' =>
    preset === 'test' || preset === 'run' || preset === 'data'

/**
 * Tells whether the table opens with an OpenL header cell of its own.
 *
 * <p>Every recognized table is introduced by one: a keyword, and whatever the keyword takes. A free-form table is
 * not recognized by OpenL at all, so it has no header to write — it is a plain grid, and OpenL names it after
 * whatever its first cell happens to say.
 */
export const hasTableHeader = (preset: TablePreset): boolean => preset !== 'freeForm'

/** Tells whether the table type looks a value up in two dimensions, with conditions on the left and on the top. */
export const isLookup = (preset: TablePreset): preset is 'smartLookup' | 'simpleLookup' =>
    preset === 'smartLookup' || preset === 'simpleLookup'

export interface HeaderBand {
    /** Rows at the top of the body that belong to the table type. */
    rows: number
    /** Leading columns the table type titles itself, each merged down the whole band. */
    keys: number
    /** Titles of those columns. */
    titles: string[]
    /** What each row of the band stands for, offered as a hint. */
    hints: string[]
}

const NO_BAND: HeaderBand = { rows: 0, keys: 0, titles: [], hints: []}

/**
 * The rows at the top of the body that belong to the table type rather than to the author.
 *
 * <p>A lookup opens with one row for each argument running across the top, titled down the left by the arguments
 * running down it. The leading arguments take the columns, the trailing ones the rows. OpenL reads the number of
 * horizontal arguments off the height of the top-left cell and takes the columns left of it as the vertical ones,
 * so the corner spans `rows` rows and `keys` columns and the two always add up to the signature. That corner is
 * kept as square as it can be, gaining a row before a column: two arguments give 1x1, three 2x1, five 3x2. Below
 * two arguments there is no lookup to build, and the smallest shape stands in while the signature is written.
 *
 * <p>A Spreadsheet opens with the row that names its columns, which the author writes and OpenL reads.
 *
 * <p>Every other type writes its structural rows itself and leaves the whole grid to the author.
 */
export const headerBand = (preset: TablePreset, context: TableBuildContext): HeaderBand => {
    if (isLookup(preset)) {
        const declared = completeArguments(context.arguments)
        const total = Math.max(2, declared.length)
        const rows = Math.ceil(total / 2)
        const keys = total - rows
        return {
            rows,
            keys,
            titles: Array.from({ length: keys }, (_, index) => declared[index]?.name ?? ''),
            hints: declared.slice(keys).map(argument => argument.name),
        }
    }
    return preset === 'spreadsheet' ? { ...NO_BAND, rows: 1 } : NO_BAND
}

/** Tells whether the grid grows a column of its own once the last one is filled. */
export const columnsGrow = (preset: TablePreset): boolean =>
    preset === 'freeForm' || preset === 'spreadsheet' || isLookup(preset)

/** Columns a growing grid never shrinks below: enough to show that the table has two dimensions. */
export const minimumColumns = (preset: TablePreset, context: TableBuildContext): number =>
    isLookup(preset) || preset === 'spreadsheet' ? headerBand(preset, context).keys + 2 : 1

const named = (keyword: string, name: string): string => (name.trim() ? `${keyword} ${name.trim()}` : keyword)

export const buildTableHeader = (
    preset: TablePreset,
    name: string,
    context: TableBuildContext
): string => {
    switch (preset) {
        case 'datatype': {
            const extendsType = context.extendsType.trim()
            const extendsClause = extendsType ? ` extends ${extendsType}` : ''
            return `Datatype ${name}${extendsClause}`
        }
        case 'vocabulary':
            return `Datatype ${name} <${context.vocabularyType || 'String'}>`
        case 'constants':
            return 'Constants'
        case 'spreadsheet':
        case 'smartRules':
        case 'simpleRules':
        case 'smartLookup':
        case 'simpleLookup':
        case 'rules':
            return functionHeader(SIGNATURE_KEYWORDS[preset], name, context.resultType, context.arguments)
        case 'test':
            return named(`Test ${context.targetName}`, name)
        case 'run':
            return named(`Run ${context.targetName}`, name)
        case 'data':
            return `Data ${context.datatypeName} ${name}`
        case 'environment':
            return 'Environment'
        case 'properties':
            return 'Properties'
        case 'freeForm':
            return name
    }
}

/**
 * The name OpenL will give the table, read from its header exactly as the compiler reads it.
 *
 * <p>The header is the authority: a Constants table is named after its keyword, a Test table after the table it
 * tests when it declares no name of its own, and a free-form table after its whole (shortened) text.
 */
export const deriveTableName = (preset: TablePreset, header: string): string => {
    const text = header.trim()
    if (!text) {
        return ''
    }
    if (preset === 'freeForm') {
        return text.length > 57 ? `${text.slice(0, 57)}...` : text
    }
    if (preset === 'datatype' || preset === 'vocabulary') {
        return text.split(/\s+/)[1] ?? ''
    }
    // Every other type is named by the last word before the argument list.
    return text.split('(').at(0)?.trim().split(/\s+/).at(-1) ?? ''
}

const textColumn = (key: string, label: string): TableColumn => ({ key, label, editor: 'text' })
const valueColumn = (key: string, label: string): TableColumn => ({ key, label, editor: 'value' })

/** Spreadsheet column name for a zero-based index: A, B, ... Z, AA. The only title a plain grid can carry. */
const columnLetter = (index: number): string => {
    let name = ''
    for (let rest = index; rest >= 0; rest = Math.floor(rest / 26) - 1) {
        name = String.fromCodePoint(65 + rest % 26) + name
    }
    return name
}

export const buildTableColumns = (
    preset: TablePreset,
    context: TableBuildContext,
    gridWidth = 1
): TableColumn[] => {
    switch (preset) {
        case 'datatype':
            return [
                { key: 'type', label: 'Type', editor: 'type' },
                textColumn('name', 'Name'),
                valueColumn('default', 'Default Value'),
                { key: 'mandatory', label: 'Mandatory', editor: 'checkbox' },
                textColumn('description', 'Description'),
                valueColumn('example', 'Examples'),
            ]
        case 'vocabulary':
            return [valueColumn('value', 'Value')]
        case 'constants':
            return [
                { key: 'type', label: 'Type', editor: 'simpleType' },
                textColumn('name', 'Name'),
                valueColumn('default', 'Default Value'),
            ]
        case 'spreadsheet':
            // The author names a Spreadsheet's columns in the first row of the body, so the grid titles none of them.
            return Array.from({ length: Math.max(minimumColumns(preset, context), gridWidth) }, (_, index) =>
                textColumn(`column-${index}`, ''))
        case 'smartRules':
        case 'simpleRules': {
            // An input column is matched to a parameter by name; with no parameters declared there is nothing to
            // match, and a placeholder column would silently bind as a second return instead.
            const inputColumns = completeArguments(context.arguments)
                .map((argument, index) => valueColumn(`input-${index}`, argument.name))
            const outputColumns = context.resultFields.length
                ? context.resultFields.map((field, index) => valueColumn(`output-${index}`, field.name))
                : [valueColumn('output', 'Output')]
            return [...inputColumns, ...outputColumns]
        }
        case 'smartLookup':
        case 'simpleLookup': {
            // The left columns are titled after the arguments they carry; the rest of the grid is the value matrix,
            // whose columns are the horizontal arguments' values and so are titled by the author, not here.
            const { keys, titles } = headerBand(preset, context)
            const width = Math.max(minimumColumns(preset, context), gridWidth)
            return [
                ...titles.map((name, index) => valueColumn(`key-${index}`, name)),
                ...Array.from({ length: width - keys }, (_, index) => valueColumn(`value-${index}`, '')),
            ]
        }
        case 'rules':
            return [valueColumn('condition', 'Condition'), valueColumn('output', 'Output')]
        case 'test':
        case 'run':
            return context.targetColumns.map((column, index) => valueColumn(
                `target-${index}`,
                column.title || column.name
            ))
        case 'data':
            return context.dataFields.map((field, index) => valueColumn(`field-${index}`, title(field.name)))
        case 'environment':
            return [
                { key: 'key', label: 'Key', editor: 'environment' },
                textColumn('value', 'Value'),
            ]
        case 'properties':
            return [
                { key: 'key', label: 'Property', editor: 'property' },
                textColumn('value', 'Value'),
            ]
        case 'freeForm':
            // Nothing in a free-form table is generated, so its columns are named the way a sheet names them.
            return Array.from({ length: gridWidth }, (_, index) =>
                textColumn(`column-${index}`, columnLetter(index)))
    }
}

/**
 * The OpenL type a value cell must accept.
 *
 * <p>Most generated tables declare one type per column. A lookup also declares types down its top band: each band
 * row belongs to one horizontal argument, the leading cells below it belong to the vertical arguments, and the
 * remaining matrix holds the result type.
 */
export const cellValueType = (
    preset: TablePreset,
    context: TableBuildContext,
    rows: TableCellValue[][],
    row: number,
    column: number
): string | undefined => {
    switch (preset) {
        case 'datatype':
            return column === 2 || column === 5 ? String(rows[row]?.[0] ?? '') : undefined
        case 'vocabulary':
            return context.vocabularyType
        case 'constants':
            return column === 2 ? String(rows[row]?.[0] ?? '') : undefined
        case 'smartRules':
        case 'simpleRules': {
            const argumentsValue = completeArguments(context.arguments)
            if (column < argumentsValue.length) {
                return argumentsValue[column]?.type
            }
            const resultColumn = column - argumentsValue.length
            return context.resultFields.length
                ? context.resultFields[resultColumn]?.type
                : context.resultType
        }
        case 'smartLookup':
        case 'simpleLookup': {
            const argumentsValue = completeArguments(context.arguments)
            const band = headerBand(preset, context)
            if (row < band.rows) {
                return column < band.keys ? undefined : argumentsValue[band.keys + row]?.type
            }
            return column < band.keys ? argumentsValue[column]?.type : context.resultType
        }
        case 'rules':
            return column === 0 ? 'Boolean' : context.resultType
        case 'test':
        case 'run':
            return context.targetColumns[column]?.type
        case 'data':
            return context.dataFields[column]?.type
        default:
            return undefined
    }
}

/** Rows the grid always shows: the header band the table type fixes, plus one for the author to fill in. */
export const minimumRows = (preset: TablePreset, context: TableBuildContext): number =>
    headerBand(preset, context).rows + 1

/**
 * The value a map holds under a key of its own, or `undefined`.
 *
 * <p>Type names are free text the author types, so one that happens to name an `Object.prototype` member —
 * `constructor`, `toString` — would otherwise resolve to the inherited function. A function is not nullish, so it
 * survives every `??` fallback and reaches code expecting the mapped type.
 */
export const ownValue = <T>(map: Readonly<Record<string, T>>, key: string): T | undefined =>
    Object.hasOwn(map, key) ? map[key] : undefined

/**
 * What an example value of each scalar type looks like, written the way OpenL reads a cell of text.
 *
 * <p>A date is written in ISO 8601 `yyyy-MM-dd` format and a boolean as the keyword rather than as a tick — a value
 * cell holds text whatever its type.
 */
const EXAMPLE_VALUES: Readonly<Record<string, string>> = {
    String: 'Text1',
    Boolean: 'TRUE',
    Integer: '1',
    Long: '1',
    Short: '1',
    Byte: '1',
    BigInteger: '1',
    Double: '1.0',
    Float: '1.0',
    BigDecimal: '1.0',
    Character: 'A',
    Date: '2026-06-15',
    IntRange: '1-10',
    DoubleRange: '1.0-10.0',
}

/** The column a Test table compares the call's result against. OpenL knows it by this name. */
export const EXPECTED_RESULT = '_res_'

/** What an example value is named after: the last segment of a column path, the expected result reading as one. */
const placeholderName = (path: string): string =>
    path === EXPECTED_RESULT ? 'result' : path.split('.').at(-1) ?? path

/**
 * The example value a cell of the given type opens with.
 *
 * <p>A scalar gets a value of its own type and a vocabulary the first value it offers, so a table created untouched
 * is a table that compiles. Everything else is a value no cell can spell out — another datatype, a collection, a
 * type this project does not declare — and is written as a reference to the row of a Data table holding it, named
 * after the field that points at it.
 */
const exampleValue = (
    type: string,
    path: string,
    vocabularies: Record<string, string[]> = {}
): string => {
    const declared = type.trim()
    return ownValue(EXAMPLE_VALUES, declared) ?? ownValue(vocabularies, declared)?.[0]
        ?? `${placeholderName(path)}_id_1`
}

/** The example row for the table types whose columns are fixed, written out cell by cell. */
const EXAMPLE_ROWS = {
    datatype: ['String', 'field1'],
    constants: ['Integer', 'CONSTANT1', '1'],
    environment: ['import', 'org.openl.rules.helpers'],
    properties: ['scope', 'Module'],
} as const satisfies Partial<Record<TablePreset, readonly TableCellValue[]>>

/** The example values one row of the table type carries, in column order. */
const exampleCells = (preset: TablePreset, context: TableBuildContext): TableCellValue[] => {
    const known = context.vocabularyValues
    switch (preset) {
        case 'vocabulary':
            return [exampleValue(context.vocabularyType, 'value', known)]
        case 'rules':
            // The condition the skeleton declares is a Boolean; the result is whatever the signature returns.
            return ['TRUE', exampleValue(context.resultType, 'result', known)]
        case 'smartRules':
        case 'simpleRules': {
            const inputs = completeArguments(context.arguments)
                .map(argument => exampleValue(argument.type, argument.name, known))
            const outputs = context.resultFields.length
                ? context.resultFields.map(field => exampleValue(field.type, field.name, known))
                : [exampleValue(context.resultType, 'result', known)]
            return [...inputs, ...outputs]
        }
        case 'test':
        case 'run':
            return context.targetColumns.map(column => exampleValue(column.type, column.name, known))
        case 'data':
            return context.dataFields.map(field => exampleValue(field.type, field.name, known))
        default:
            // The rest declare rather than carry values, so their example is written out cell by cell.
            return [...EXAMPLE_ROWS[preset as keyof typeof EXAMPLE_ROWS] ?? []]
    }
}

/**
 * Rows the skeleton starts with: the rows the table type owns, then one row of example data.
 *
 * <p>The example is a placeholder to write over, not a suggestion. An empty grid says nothing about what a row of
 * this table looks like, and a filled one does. Every value matches the type its column declares, so a table
 * created untouched is a table that works.
 *
 * <p>A Properties table is the exception: `scope` is not an example but the row without which OpenL rejects the
 * table.
 */
export const initialRows = (preset: TablePreset, context: TableBuildContext): TableCellValue[][] => {
    if (isLookup(preset)) {
        const { rows, keys } = headerBand(preset, context)
        const declared = completeArguments(context.arguments)
        // One row per argument running across the top, each holding a value of that argument's own type, and then
        // a row of the values to look up by — the arguments running down the left, and the result they give.
        const known = context.vocabularyValues
        const band = Array.from({ length: rows }, (_, index) => {
            const argument = declared[keys + index]
            return [
                ...new Array<TableCellValue>(keys).fill(''),
                argument ? exampleValue(argument.type, argument.name, known) : 'Value1',
            ]
        })
        const lookedUpBy = Array.from({ length: keys }, (_, index) => {
            const argument = declared[index]
            return argument ? exampleValue(argument.type, argument.name, known) : 'Key1'
        })
        return [...band, [...lookedUpBy, exampleValue(context.resultType, 'result', known)]]
    }
    if (preset === 'spreadsheet') {
        return [['Steps', 'Formula'], ['Step1', '= 1']]
    }
    if (preset === 'freeForm') {
        return [['Cell 1', 'Cell 2']]
    }
    return [exampleCells(preset, context)]
}

const isEmptyCell = (value: TableCellValue): boolean => value === '' || value === false

const isEmptyRow = (row: TableCellValue[]): boolean => row.every(isEmptyCell)

/** Whether no value is written twice in a column, comparing the values the rows actually carry. */
const hasDistinctValues = (rows: TableCellValue[][], column = 0): boolean => {
    const written = rows.filter(row => !isEmptyRow(row)).map(row => String(row[column] ?? '').trim())
    return new Set(written).size === written.length
}

/** The column a Datatype or Constants row names the field it declares in. */
const FIELD_NAME_COLUMN = 1

/** Whether every row carrying anything names the field it declares. */
const hasFieldNames = (rows: TableCellValue[][]): boolean => rows
    .filter(row => !isEmptyRow(row))
    .every(row => Boolean(String(row[FIELD_NAME_COLUMN] ?? '').trim()))

/**
 * Tells whether the body carries what the table type needs.
 *
 * <p>Blank rows are never written, so a table whose fixed rows do not already form a legal body would reach OpenL as
 * a lone header. Every type needs a body of some kind: a Datatype its fields, a Vocabulary its values, a Properties
 * table the row without which OpenL rejects it, a Spreadsheet at least one step.
 *
 * <p>A Datatype or Constants row declares one field, so it has to name it, and no two rows may name the same one.
 * A Vocabulary's values are equally a set. Either way the duplicate compiles to nothing, leaving a row in the sheet
 * that means nothing, and a nameless field fails the module outright.
 *
 * <p>A lookup asks for more. Each of its horizontal arguments owns a row of values, and a row left blank is dropped
 * before the table is written — the corner cell would then span rows that are gone, and OpenL would read a different
 * number of arguments as horizontal. So every row of the band needs a value, and the matrix below it at least one
 * row of its own.
 */
export const bodyIsValid = (
    preset: TablePreset,
    context: TableBuildContext,
    rows: TableCellValue[][]
): boolean => {
    if (!hasTableHeader(preset)) {
        // With no header of its own, the table starts at its first cell. OpenL reads a table from that cell and
        // names it after it, and a table whose first cell is blank is no table at all. Blank rows are dropped
        // before the table is written, so that first cell is the one of the first row carrying anything.
        return !isEmptyCell(dropEmptyRows(rows)[0]?.[0] ?? '')
    }
    if (preset === 'vocabulary') {
        // A vocabulary compiles to a set, so a value repeated in two rows is dropped on compilation with nothing
        // reported anywhere — the row would stay in the sheet meaning nothing. Rejected here instead.
        return rows.some(row => !isEmptyRow(row)) && hasDistinctValues(rows)
    }
    if (preset === 'datatype' || preset === 'constants') {
        // The table is written verbatim and the server checks only its name, so a nameless or repeated field would
        // reach the project as a module that no longer compiles.
        return rows.some(row => !isEmptyRow(row))
            && hasFieldNames(rows)
            && hasDistinctValues(rows, FIELD_NAME_COLUMN)
    }
    const { rows: bandHeight, keys } = headerBand(preset, context)
    if (!bandHeight) {
        // A header alone is not a table: a Datatype declares no field, a Vocabulary no value, and a Properties
        // table is rejected outright by OpenL without the row its JavaDoc below describes.
        return rows.some(row => !isEmptyRow(row))
    }
    const band = rows.slice(0, bandHeight)
    return band.length === bandHeight
        && band.every(row => row.slice(keys).some(cell => !isEmptyCell(cell)))
        && rows.slice(bandHeight).some(row => !isEmptyRow(row))
}

/**
 * Drops every blank row, including the trailing one the editor always keeps for input. OpenL reads a blank row as a
 * table boundary and the write API rejects one outright, so no blank row may reach the server.
 */
export const dropEmptyRows = (rows: TableCellValue[][]): TableCellValue[][] => rows.filter(row => !isEmptyRow(row))

/**
 * Drops the trailing blank columns a growing grid keeps for input. A blank column ends the table the same way a blank
 * row does.
 */
export const dropEmptyTrailingColumns = (rows: TableCellValue[][]): TableCellValue[][] => {
    const width = Math.max(0, ...rows.map(row => row.length))
    let used = width
    while (used > 1 && rows.every(row => isEmptyCell(row[used - 1] ?? ''))) {
        used--
    }
    return used === width ? rows : rows.map(row => row.slice(0, used))
}

/** The row at exactly `width` cells: cells past the width are dropped, missing ones come out blank. */
const padRow = (row: TableCellValue[], width: number): TableCellValue[] => [
    ...row.slice(0, width),
    ...new Array<TableCellValue>(Math.max(0, width - row.length)).fill(''),
]

export const normalizeRows = (
    rows: TableCellValue[][],
    width: number,
    minimum = 1
): TableCellValue[][] => {
    const normalized = rows.map(row => padRow(row, width))
    while (normalized.length < minimum) {
        normalized.push(new Array<TableCellValue>(width).fill(''))
    }
    if (!normalized.length || !isEmptyRow(normalized.at(-1) ?? [])) {
        normalized.push(new Array<TableCellValue>(width).fill(''))
    }
    return normalized
}

const isEmptyColumn = (rows: TableCellValue[][], column: number): boolean =>
    rows.every(row => isEmptyCell(row[column] ?? ''))

export const normalizeFreeFormColumns = (
    rows: TableCellValue[][],
    minimumWidth = 1
): TableCellValue[][] => {
    let width = Math.max(minimumWidth, ...rows.map(row => row.length))
    if (!isEmptyColumn(rows, width - 1)) {
        width++
    }
    return rows.map(row => padRow(row, width))
}

/**
 * The heading a generated column carries, read as one phrase rather than as a list of identifiers.
 *
 * <p>Underscores and camel humps open a new word. Every word is capitalized: `policy mainDriver age` reads as
 * `Policy Main Driver Age`. A word already written in capitals keeps them.
 */
export const title = (name: string): string => name
    .replaceAll(/[_-]+/g, ' ')
    .replaceAll(/([A-Z])(?=[A-Z][a-z])/g, '$1 ')
    .replaceAll(/([a-z\d])([A-Z])/g, '$1 $2')
    .trim()
    .split(/\s+/)
    .map(word => /^[A-Z\d]+$/.test(word)
        ? word
        : word.toLowerCase().replace(/^./, first => first.toUpperCase()))
    .join(' ')

/**
 * The complete matrix written to the sheet: the header cell, the structural rows the table type requires, then the
 * rows the author filled in. The generated header is passed in so the preview and submitted source share one value.
 */
export const buildTableSource = (
    preset: TablePreset,
    headerText: string,
    context: TableBuildContext,
    rows: TableCellValue[][],
    transposed = false
): TableCellValue[][] => {
    if (!hasTableHeader(preset)) {
        return rows
    }
    const header = [headerText]
    switch (preset) {
        case 'datatype':
            return [header, ['Type', 'Name', 'Default', 'Mandatory', 'Description', 'Example'], ...rows]
        case 'smartRules':
        case 'simpleRules':
            return [header, buildTableColumns(preset, context).map(column => column.label), ...rows]
        case 'rules':
            // Row 2 is the expression and row 3 declares the parameters it uses, so both parameters must be named:
            // a bare type is given a generated name and the expression would reference nothing. Keeping the
            // condition self-contained rather than derived from the first argument makes the skeleton bind for any
            // signature — a condition typed after one argument contradicts the others.
            return [
                header,
                ['C1', 'RET1'],
                ['condition', 'result'],
                ['Boolean condition', `${context.resultType.trim() || 'Boolean'} result`],
                ['Condition', 'Result'],
                ...rows,
            ]
        case 'smartLookup':
        case 'simpleLookup': {
            // The vertical arguments are titled in the sheet, in the same row their first horizontal values sit in:
            // that row opens the band whose height tells OpenL how many arguments are horizontal.
            const { keys, titles } = headerBand(preset, context)
            const [band = [], ...body] = rows
            return [header, [...titles, ...band.slice(keys)], ...body]
        }
        case 'test':
        case 'run': {
            const body = [
                context.targetColumns.map(column => column.name),
                context.targetColumns.map(column => column.title),
                ...rows,
            ]
            return [header, ...(transposed ? transpose(body) : body)]
        }
        case 'data': {
            const body = [
                context.dataFields.map(field => field.name),
                context.dataFields.map(field => title(field.name)),
                ...rows,
            ]
            return [header, ...(transposed ? transpose(body) : body)]
        }
        default:
            return [header, ...rows]
    }
}

/** Turns rows into columns, padding short rows so the result stays rectangular. */
const transpose = (rows: TableCellValue[][]): TableCellValue[][] => {
    const width = Math.max(0, ...rows.map(row => row.length))
    return Array.from({ length: width }, (_, column) => rows.map(row => row[column] ?? ''))
}

const TABLE_KINDS = {
    datatype: 'Datatype',
    vocabulary: 'Datatype',
    spreadsheet: 'Spreadsheet',
    smartRules: 'Rules',
    simpleRules: 'Rules',
    smartLookup: 'Rules',
    simpleLookup: 'Rules',
    rules: 'Rules',
    test: 'Test',
    run: 'Run',
    data: 'Data',
    environment: 'Environment',
    properties: 'Properties',
    constants: 'Constants',
    freeForm: 'Other',
} as const satisfies Record<TablePreset, string>

export const tableKind = (preset: TablePreset): string => TABLE_KINDS[preset]
