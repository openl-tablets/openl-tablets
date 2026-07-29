import {
    bodyIsValid,
    buildTableColumns,
    buildTableHeader,
    buildTableSource,
    cellValueType,
    defaultResultType,
    deriveTableName,
    dropEmptyRows,
    dropEmptyTrailingColumns,
    ENVIRONMENT_KEYS,
    hasTableName,
    headerBand,
    initialRows,
    isTargeted,
    isTransposable,
    minimumRows,
    normalizeFreeFormColumns,
    normalizeRows,
    SIMPLE_TYPES,
    TABLE_PRESETS,
    tableKind,
    title,
    type TableArgument,
    type TableBuildContext,
} from './tableSkeletons'

const args = (...names: string[]): TableArgument[] => names.map(name => ({ type: 'String', name }))

const context = (overrides: Partial<TableBuildContext> = {}): TableBuildContext => ({
    resultType: 'Boolean',
    resultFields: [],
    arguments: [],
    vocabularyType: 'String',
    extendsType: '',
    datatypeName: 'Customer',
    dataFields: [{ name: 'name', type: 'String' }, { name: 'age', type: 'Integer' }],
    targetName: 'Eligibility',
    targetColumns: [
        { name: 'age', title: 'Age', type: 'Integer' },
        { name: '_res_', title: 'Result', type: 'Boolean' },
    ],
    vocabularyValues: { Country: ['USA', 'Canada']},
    ...overrides,
})

describe('tableSkeletons', () => {
    it('exposes only the supported table types in the requested order', () => {
        expect(TABLE_PRESETS).toEqual([
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
        ])
    })

    it('builds OpenL headers from the selected types and signatures', () => {
        const signature = context({
            resultType: 'Integer',
            arguments: [
                { type: 'String', name: 'country' },
                { type: 'Customer', name: 'customer' },
            ],
        })

        expect(buildTableHeader('vocabulary', 'Status', context({ vocabularyType: 'Integer' })))
            .toBe('Datatype Status <Integer>')
        expect(buildTableHeader('datatype', 'Driver', context({ extendsType: 'Person' })))
            .toBe('Datatype Driver extends Person')
        expect(buildTableHeader('spreadsheet', 'Premium', signature))
            .toBe('Spreadsheet Integer Premium(String country, Customer customer)')
        expect(buildTableHeader('test', 'Regression', context())).toBe('Test Eligibility Regression')
        expect(buildTableHeader('data', 'Customers', context())).toBe('Data Customer Customers')
        expect(buildTableHeader('freeForm', 'Custom Header', context())).toBe('Custom Header')
    })

    it('uses the requested fixed columns and datatype metadata columns', () => {
        expect(buildTableColumns('datatype', context()).map(column => column.label))
            .toEqual(['Type', 'Name', 'Default Value', 'Mandatory', 'Description', 'Examples'])
        expect(buildTableColumns('constants', context()).map(column => column.label))
            .toEqual(['Type', 'Name', 'Default Value'])
        expect(buildTableColumns('rules', context()).map(column => column.label))
            .toEqual(['Condition', 'Output'])
        expect(buildTableColumns('environment', context()).map(column => column.label))
            .toEqual(['Key', 'Value'])
        expect(buildTableColumns('properties', context()).map(column => column.label))
            .toEqual(['Property', 'Value'])
    })

    it('derives Smart Rules columns from complete arguments and the result', () => {
        const columns = buildTableColumns('smartRules', context({
            arguments: [
                { type: 'Integer', name: 'age' },
                { type: 'String', name: 'country' },
                { type: 'Customer', name: '' },
            ],
        }))

        expect(columns.map(column => column.label)).toEqual(['age', 'country', 'Output'])
    })

    it('expands a compound Smart Rules result into datatype fields', () => {
        const columns = buildTableColumns('smartRules', context({
            resultType: 'Customer',
            resultFields: [{ name: 'name', type: 'String' }, { name: 'age', type: 'Integer' }],
            arguments: [{ type: 'String', name: 'id' }],
        }))

        expect(columns.map(column => column.label)).toEqual(['id', 'name', 'age'])
    })

    it('derives Test, Run, and Data columns from the selected source table or datatype', () => {
        expect(buildTableColumns('test', context()).map(column => column.label)).toEqual(['Age', 'Result'])
        expect(buildTableColumns('run', context()).map(column => column.label)).toEqual(['Age', 'Result'])
        // The column names themselves are what the table is written with, titles only what the editor shows.
        expect(buildTableSource('run', 'Run Eligibility', context(), [])[1]).toEqual(['age', '_res_'])
        expect(buildTableColumns('data', context()).map(column => column.label)).toEqual(['Name', 'Age'])
        expect(buildTableColumns('data', context({
            dataFields: [{ name: 'mainDriverAge', type: 'Integer' }],
        })).map(column => column.label)).toEqual(['Main Driver Age'])
    })

    it('keys columns by position, so two same-named arguments cannot collide', () => {
        const columns = buildTableColumns('smartRules', context({
            arguments: [{ type: 'String', name: 'id' }, { type: 'Integer', name: 'id' }],
        }))

        expect(columns.map(column => column.label)).toEqual(['id', 'id', 'Output'])
        expect(new Set(columns.map(column => column.key)).size).toBe(columns.length)
    })

    it('automatically keeps one empty row below the last completed row', () => {
        expect(normalizeRows([['John', '42']], 2)).toEqual([
            ['John', '42'],
            ['', ''],
        ])
        expect(normalizeRows([[''], [''], ['']], 1)).toEqual([[''], [''], ['']])
    })

    it('opens a Lookup with a row for every horizontal argument and one to look up by', () => {
        expect(minimumRows('smartLookup', context({ arguments: args('make', 'year') }))).toBe(2)
        expect(minimumRows('smartLookup', context({ arguments: args('a', 'b', 'c', 'd', 'e') }))).toBe(4)
        // Every other table type owns no row of the body at all.
        expect(minimumRows('datatype', context())).toBe(1)
        expect(headerBand('datatype', context()).rows).toBe(0)
    })

    it('automatically keeps one empty Free Form column to the right', () => {
        expect(normalizeFreeFormColumns([['A']])).toEqual([['A', '']])
        expect(normalizeFreeFormColumns([['A', '', '']])).toEqual([['A', '', '']])
    })

    it('names both Decision Table parameters so the expressions resolve', () => {
        // Row 2 is the expression and row 3 declares its parameters; a bare type is given a generated name, so an
        // expression naming `result` would resolve to nothing.
        const rulesHeader = 'Rules String Greeting()'
        expect(buildTableSource('rules', rulesHeader, context({ resultType: 'String' }), [['TRUE', 'Hi']])).toEqual([
            ['Rules String Greeting()'],
            ['C1', 'RET1'],
            ['condition', 'result'],
            ['Boolean condition', 'String result'],
            ['Condition', 'Result'],
            ['TRUE', 'Hi'],
        ])
    })

    it('starts a Spreadsheet from the result object OpenL builds for it', () => {
        expect(defaultResultType('spreadsheet')).toBe('SpreadsheetResult')
        expect(defaultResultType('smartRules')).toBe('Boolean')
        expect(buildTableHeader('spreadsheet', 'Premium', context({ resultType: defaultResultType('spreadsheet') })))
            .toBe('Spreadsheet SpreadsheetResult Premium()')
    })

    it('holds the language constants itself instead of fetching them', () => {
        // Scalar types, Environment keywords and table types are fixed by OpenL, so they are not module data.
        expect(SIMPLE_TYPES).toContain('String')
        expect(SIMPLE_TYPES).not.toContain('int')
        expect(SIMPLE_TYPES).not.toContain('SpreadsheetResult')
        // Exactly the keywords SequentialXlsLoader accepts; anything else is an "unrecognized keyword" error.
        expect(ENVIRONMENT_KEYS).toEqual(['dependency', 'import', 'include'])
        expect(TABLE_PRESETS).toHaveLength(15)
    })

    it('leaves the keyword alone for the table types that carry no name', () => {
        expect(TABLE_PRESETS.filter(preset => !hasTableName(preset)))
            .toEqual(['constants', 'environment', 'properties', 'freeForm'])
        expect(buildTableHeader('constants', 'Ignored', context())).toBe('Constants')
        expect(buildTableHeader('environment', 'Ignored', context())).toBe('Environment')
        expect(buildTableHeader('properties', 'Ignored', context())).toBe('Properties')
    })

    it('names a Test or Run table only when one is given', () => {
        expect(TABLE_PRESETS.filter(isTargeted)).toEqual(['test', 'run'])
        expect(buildTableHeader('test', '', context())).toBe('Test Eligibility')
        expect(buildTableHeader('test', 'Regression', context())).toBe('Test Eligibility Regression')
        expect(buildTableHeader('run', '', context())).toBe('Run Eligibility')
    })

    it('reads the table name out of the header the way OpenL does', () => {
        expect(deriveTableName('datatype', 'Datatype Customer')).toBe('Customer')
        expect(deriveTableName('vocabulary', 'Datatype Status <String>')).toBe('Status')
        expect(deriveTableName('constants', 'Constants')).toBe('Constants')
        expect(deriveTableName('test', 'Test Eligibility')).toBe('Eligibility')
        expect(deriveTableName('test', 'Test Eligibility Regression')).toBe('Regression')
        expect(deriveTableName('rules', 'Rules String Greeting(Integer hour)')).toBe('Greeting')
        expect(deriveTableName('spreadsheet', 'Spreadsheet SpreadsheetResult Premium()')).toBe('Premium')
        // A free-form header names the table, shortened exactly as the compiler shortens it
        expect(deriveTableName('freeForm', 'Some free text')).toBe('Some free text')
        expect(deriveTableName('freeForm', 'x'.repeat(60))).toBe(`${'x'.repeat(57)}...`)
    })

    it('requires a filled row of every table type', () => {
        // A header alone is not a table for any type: a Datatype declares no field, a free-form table starts at a
        // first cell that is not there, and OpenL rejects a Properties table carrying no property row.
        expect(TABLE_PRESETS.filter(preset => bodyIsValid(preset, context(), [['', '']]))).toEqual([])
        expect(bodyIsValid('datatype', context(), [['String', 'name']])).toBe(true)
        expect(bodyIsValid('properties', context(), [['scope', 'Module']])).toBe(true)
        expect(bodyIsValid('freeForm', context(), [['Cell 1', '']])).toBe(true)
        // Blank rows are dropped before the table is written, so the first cell OpenL reads is the first filled one.
        expect(bodyIsValid('freeForm', context(), [['', ''], ['Cell 1', '']])).toBe(true)
        // A Spreadsheet names its columns in the first row, so a step below them is what makes it a table.
        expect(bodyIsValid('spreadsheet', context(), [['Steps', 'Formula']])).toBe(false)
        expect(bodyIsValid('spreadsheet', context(), [['Steps', 'Formula'], ['Premium', '= 10']])).toBe(true)
    })

    it('requires every Datatype and Constants row to name a field, once', () => {
        // The table is written verbatim and the server checks only its own name, so a nameless or repeated field
        // would land in the project as a module that no longer compiles.
        expect(bodyIsValid('datatype', context(), [['String', 'name'], ['Integer', 'age']])).toBe(true)
        expect(bodyIsValid('datatype', context(), [['String', ''], ['Integer', 'age']])).toBe(false)
        expect(bodyIsValid('datatype', context(), [['String', 'name'], ['Integer', 'name']])).toBe(false)
        // A default value with no name to attach it to is the same nameless field.
        expect(bodyIsValid('datatype', context(), [['String', '', 'Smith']])).toBe(false)
        expect(bodyIsValid('constants', context(), [['Integer', 'MAX', '10'], ['Integer', 'MIN', '1']])).toBe(true)
        expect(bodyIsValid('constants', context(), [['Integer', 'MAX', '10'], ['Integer', 'MAX', '1']])).toBe(false)
    })

    it('spreads a Lookup over both axes, taking a row before a column', () => {
        const shapeOf = (count: number) => {
            const { rows, keys } = headerBand('simpleLookup', context({
                arguments: args(...Array.from({ length: count }, (_, index) => `a${index}`)),
            }))
            return `${rows}x${keys}`
        }

        // The corner stays as square as it can be, and its rows and columns always add up to the signature.
        expect([2, 3, 4, 5, 6].map(shapeOf)).toEqual(['1x1', '2x1', '2x2', '3x2', '3x3'])
        // A signature too short to look anything up still renders, on the smallest shape there is.
        expect([0, 1].map(shapeOf)).toEqual(['1x1', '1x1'])
    })

    it('titles a Lookup down the left with its leading arguments', () => {
        const fiveArguments = context({ arguments: args('make', 'model', 'year', 'area', 'age') })

        // Two down the left, three across the top: the trailing arguments are positional and go untitled.
        expect(headerBand('simpleLookup', fiveArguments).titles).toEqual(['make', 'model'])
        expect(buildTableColumns('simpleLookup', fiveArguments).map(column => column.label))
            .toEqual(['make', 'model', '', ''])
        // The value matrix grows with the grid; the key columns do not.
        expect(buildTableColumns('simpleLookup', fiveArguments, 6).map(column => column.label))
            .toEqual(['make', 'model', '', '', '', ''])
    })

    it('writes a Lookup title in the row its first horizontal values sit in', () => {
        const twoArguments = context({ arguments: args('make', 'year') })

        expect(buildTableSource('simpleLookup', 'SimpleLookup Integer rate(String make, String year)', twoArguments, [
            ['', '2024', '2025'],
            ['Audi', '10', '20'],
        ])).toEqual([
            ['SimpleLookup Integer rate(String make, String year)'],
            ['make', '2024', '2025'],
            ['Audi', '10', '20'],
        ])
    })

    it('rejects a Lookup until every horizontal argument has a value to match', () => {
        const threeArguments = context({ arguments: args('make', 'year', 'area') })
        // Two rows of horizontal values, then the rows looked up by the single key column.
        const band = [['', '2024'], ['', 'North']]

        expect(bodyIsValid('smartLookup', threeArguments, [...band, ['Audi', '10']])).toBe(true)
        // A blank band row is dropped before the table is written, which would shorten the merged corner.
        expect(bodyIsValid('smartLookup', threeArguments, [['', '2024'], ['', ''], ['Audi', '10']])).toBe(false)
        // Values across the top but nothing to look up by is not a table yet.
        expect(bodyIsValid('smartLookup', threeArguments, [...band, ['', '']])).toBe(false)
    })

    it('creates the raw structural rows required by OpenL', () => {
        expect(buildTableSource('datatype', 'Datatype Customer', context(), [
            ['String', 'name', 'Unknown', 'TRUE', 'Customer name', 'John Doe'],
        ])).toEqual([
            ['Datatype Customer'],
            ['Type', 'Name', 'Default', 'Mandatory', 'Description', 'Example'],
            ['String', 'name', 'Unknown', 'TRUE', 'Customer name', 'John Doe'],
        ])
        expect(buildTableSource('test', 'Test Eligibility Regression', context(), [['18', 'true']])).toEqual([
            ['Test Eligibility Regression'],
            ['age', '_res_'],
            ['Age', 'Result'],
            ['18', 'true'],
        ])
    })

    it('transposes the structural rows and records of Test, Run, and Data tables', () => {
        expect(TABLE_PRESETS.filter(isTransposable)).toEqual(['test', 'run', 'data'])
        expect(buildTableSource(
            'test',
            'Test Eligibility Regression',
            context(),
            [['18', 'TRUE'], ['21', 'FALSE']],
            true
        )).toEqual([
            ['Test Eligibility Regression'],
            ['age', 'Age', '18', '21'],
            ['_res_', 'Result', 'TRUE', 'FALSE'],
        ])
        expect(buildTableSource('data', 'Data Customer Customers', context(), [['Ann', '30']], true)).toEqual([
            ['Data Customer Customers'],
            ['name', 'Name', 'Ann'],
            ['age', 'Age', '30'],
        ])
    })

    it('resolves the declared type for every value-bearing table shape', () => {
        expect(cellValueType('datatype', context(), [['Country', 'origin']], 0, 2)).toBe('Country')
        expect(cellValueType('datatype', context(), [['Country', 'origin']], 0, 5)).toBe('Country')
        expect(cellValueType('constants', context(), [['Integer', 'LIMIT']], 0, 2)).toBe('Integer')
        expect(cellValueType('vocabulary', context({ vocabularyType: 'Boolean' }), [[]], 0, 0)).toBe('Boolean')
        expect(cellValueType('data', context(), [[]], 0, 1)).toBe('Integer')
        expect(cellValueType('test', context(), [[]], 0, 1)).toBe('Boolean')
        expect(cellValueType('rules', context({ resultType: 'String' }), [[]], 0, 0)).toBe('Boolean')
        expect(cellValueType('rules', context({ resultType: 'String' }), [[]], 0, 1)).toBe('String')

        const lookup = context({
            resultType: 'Double',
            arguments: [
                { type: 'String', name: 'make' },
                { type: 'Integer', name: 'year' },
                { type: 'Country', name: 'country' },
            ],
        })
        expect(cellValueType('simpleLookup', lookup, [[], [], []], 0, 1)).toBe('Integer')
        expect(cellValueType('simpleLookup', lookup, [[], [], []], 1, 1)).toBe('Country')
        expect(cellValueType('simpleLookup', lookup, [[], [], []], 2, 0)).toBe('String')
        expect(cellValueType('simpleLookup', lookup, [[], [], []], 2, 1)).toBe('Double')
    })

    it('maps every preset to a table kind and reserves Other for Free Form', () => {
        expect(TABLE_PRESETS.map(tableKind)).not.toContain('')
        expect(tableKind('freeForm')).toBe('Other')
        expect(tableKind('smartRules')).toBe('Rules')
    })

    it('omits the input column when the signature declares no parameter', () => {
        // A placeholder input column has no parameter to match and would bind as a second return instead.
        expect(buildTableColumns('smartRules', context({ arguments: []})).map(column => column.label))
            .toEqual(['Output'])
        expect(buildTableColumns('simpleRules', context({ arguments: [{ type: 'String', name: 'id' }]}))
            .map(column => column.label)).toEqual(['id', 'Output'])
    })

    it('opens every table on an example row, written over rather than typed from nothing', () => {
        expect(initialRows('datatype', context())).toEqual([['String', 'field1']])
        expect(initialRows('constants', context())).toEqual([['Integer', 'CONSTANT1', '1']])
        // A Spreadsheet names its own columns, so its example starts one row lower.
        expect(initialRows('spreadsheet', context())).toEqual([['Steps', 'Formula'], ['Step1', '= 1']])
        // Where the columns come from a datatype or a signature, every cell is an example of its own type.
        expect(initialRows('data', context())).toEqual([['Text1', '1']])
        expect(initialRows('test', context())).toEqual([['1', 'TRUE']])
        // A Properties table declares `scope` to compile at all: the example row is the requirement.
        expect(initialRows('properties', context())).toEqual([['scope', 'Module']])
    })

    it('fills a Lookup example across both of its axes, each cell typed by its own argument', () => {
        // Three arguments: two rows of horizontal values, one key column, and a row to look up by. The leading
        // argument runs down the left, so the key cell is a String and the two rows above it are what follows it.
        const arguments3 = [
            { type: 'String', name: 'make' },
            { type: 'Integer', name: 'year' },
            { type: 'Date', name: 'registered' },
        ]

        expect(initialRows('smartLookup', context({ arguments: arguments3 }))).toEqual([
            ['', '1'],
            ['', '2026-06-15'],
            ['Text1', 'TRUE'],
        ])
    })

    it('opens a vocabulary cell on the first value the vocabulary offers', () => {
        // A vocabulary accepts a fixed set of values, so one of them is a better example than a reference to a row.
        expect(initialRows('data', context({
            dataFields: [{ name: 'origin', type: 'Country' }],
        }))).toEqual([['USA']])
        expect(initialRows('vocabulary', context({ vocabularyType: 'Country' }))).toEqual([['USA']])
        // A vocabulary whose values were not read is a type like any other the modal cannot spell out.
        expect(initialRows('data', context({
            dataFields: [{ name: 'origin', type: 'Region' }],
        }))).toEqual([['origin_id_1']])
    })

    it('writes an example a cell cannot spell out as a reference to a Data table row', () => {
        // A datatype the modal could not open up, a collection, a type this project does not declare: the value
        // lives in a Data table of its own, and the cell points at the row holding it.
        expect(initialRows('data', context({
            dataFields: [{ name: 'driver', type: 'Driver' }, { name: 'history', type: 'Claim[]' }],
        }))).toEqual([['driver_id_1', 'history_id_1']])
        // The expected result is named after what it is rather than after the column OpenL knows it by.
        expect(initialRows('test', context({
            targetColumns: [{ name: '_res_', title: 'Result', type: 'Policy' }],
        }))).toEqual([['result_id_1']])
    })

    it('capitalizes every word in a generated Test, Run, or Data title', () => {
        expect(buildTableSource('test', 'Test Premium', context({
            targetColumns: [{ name: 'policy.mainDriver.age', title: title('policy mainDriver age'), type: 'Integer' }],
        }), [])[2]).toEqual(['Policy Main Driver Age'])
        // A word already written in capitals keeps them.
        expect(title('vehicle VIN')).toBe('Vehicle VIN')
        expect(title('vehicleUSAState')).toBe('Vehicle USA State')
    })

    it('drops every blank row, which OpenL reads as the end of the table', () => {
        expect(dropEmptyRows(normalizeRows([['String', 'name']], 2))).toEqual([['String', 'name']])
        expect(dropEmptyRows([['String', 'name'], ['', ''], ['Integer', 'age'], ['', '']]))
            .toEqual([['String', 'name'], ['Integer', 'age']])
        // An unticked checkbox alone is not data, so its row is blank too.
        expect(dropEmptyRows([['', '', '', false, '']])).toEqual([])
    })

    it('drops the trailing blank Free Form columns but keeps interior ones', () => {
        expect(dropEmptyTrailingColumns([['A', '', 'C', '', '']])).toEqual([['A', '', 'C']])
        expect(dropEmptyTrailingColumns([['A', 'B'], ['C', '']])).toEqual([['A', 'B'], ['C', '']])
        expect(dropEmptyTrailingColumns([['', '']])).toEqual([['']])
    })
})
