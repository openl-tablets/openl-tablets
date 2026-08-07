import React, { useCallback, useMemo, useRef, useState } from 'react'
import {
    Checkbox,
    Input,
    Modal,
    notification,
    Select,
    Space,
    Spin,
    Tooltip,
} from 'antd'
import {
    DeleteColumnOutlined,
    DeleteRowOutlined,
    InsertRowAboveOutlined,
    InsertRowBelowOutlined,
    InsertRowLeftOutlined,
    InsertRowRightOutlined,
    TableOutlined,
} from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { FieldRow } from 'components/FieldRow'
import { IconAction } from 'components/IconAction'
import { SuggestInput } from 'components/SuggestInput'
import { useGlobalEvents } from 'hooks'
import { errorMessage } from 'utils/errorMessage'
import { getModuleSheets, getProjectModules, getProjectProperties } from 'services/projects'
import { createTable, getDatatype, getProjectTables } from 'services/tables'
import type {
    DatatypeField,
    ProjectDatatype,
    ProjectTable,
    ProjectProperty,
    RawTableCellInput,
    SummaryTable,
} from 'types/tables'
import {
    bodyIsValid,
    buildTableColumns,
    buildTableHeader,
    buildTableSource,
    cellHoldsCondition,
    cellValueType,
    columnsGrow,
    defaultResultType,
    deriveTableName,
    dropEmptyRows,
    dropEmptyTrailingColumns,
    ENVIRONMENT_KEYS,
    hasTableHeader,
    hasTableName,
    headerBand,
    initialRows,
    isLookup,
    isTargeted,
    isTransposable,
    minimumColumns,
    minimumRows,
    normalizeFreeFormColumns,
    normalizeRows,
    ownValue,
    SIGNATURE_PRESETS,
    SIMPLE_TYPES,
    TABLE_PRESETS,
    tableKind,
    title,
    type TableArgument,
    type TableBuildContext,
    type TableCellEditor,
    type TableCellValue,
    type TableColumn,
    type TablePreset,
} from './tableSkeletons'
import {
    buildTargetStructure,
    canTargetTable,
    type FieldsOfType,
    targetTableName,
    type TargetStructure,
} from './testSkeleton'
import { tableValueIsValid, TypedTableValueInput } from './TypedTableValueInput'
import {
    asOptions,
    defaultModulePath,
    deleteAt,
    IDENTIFIER,
    insertAt,
    isValidSheetName,
    type ModuleOption,
    preferredModule,
    sheetNameFrom,
    toModuleOptions,
    toSortedOptions,
    withTrailingBlank,
} from '../tableModals/shared'
import { useSharedStyles } from '../tableModals/sharedStyles'
import { useSheetLoader } from '../tableModals/useSheetLoader'
import { initialPropertyValue, PropertyValueInput } from '../tableModals/PropertyValueInput'
import { useStyles } from './CreateTableModal.styles'

const COMPACT_DATATYPE_COLUMNS = new Set(['default', 'mandatory', 'example'])
const DEFAULT_VOCABULARY_TYPE = 'String'
/** The type a Spreadsheet returns. Offered in a signature only, never as a cell type. */
const SPREADSHEET_RESULT = 'SpreadsheetResult'
const EMPTY_ARGUMENT: TableArgument = { type: '', name: '' }
const blankArgument = (): TableArgument => ({ ...EMPTY_ARGUMENT })

// Built from frozen module constants, so the arrays are created once instead of on every render.
const SIMPLE_TYPE_OPTIONS = asOptions(SIMPLE_TYPES)
const ENVIRONMENT_KEY_OPTIONS = asOptions(ENVIRONMENT_KEYS)

export interface CreateTableModalDetail {
    projectId: string
    currentModuleName?: string
    sourceTableId?: string
    onSuccess?: (table: SummaryTable, moduleName: string) => void
}

/**
 * Kinds of table a Test or a Run table can call.
 *
 * <p>The kind is what the tables list filters on: `Rules` covers the decision tables and both lookups, and the
 * rest are the other table types OpenL compiles into a callable method.
 */
const EXECUTABLE_KINDS = ['Rules', 'Spreadsheet', 'Method', 'TBasic', 'Column Match']

/** Both are written with the `Datatype` keyword; the table type is what tells a vocabulary from a datatype. */
const namesOf = (types: ProjectTable[], tableType: string): string[] =>
    types.filter(type => type.tableType === tableType).map(type => type.name)

const rawCellValue = (value: TableCellValue | undefined): TableCellValue | null =>
    value === undefined || value === '' ? null : value

/**
 * The corner a lookup merges its argument titles into: `rows` tall and `columns` wide, starting right below the
 * table header. Its height is what tells OpenL how many arguments run across the top.
 */
interface MergedCorner {
    rows: number
    columns: number
}

const toRawSource = (
    grid: TableCellValue[][],
    options: { header: boolean, corner?: MergedCorner }
): RawTableCellInput[][] => {
    const width = Math.max(1, ...grid.map(row => row.length))
    const headerRows = options.header ? 1 : 0
    const cornerRows = options.corner?.rows ?? 0
    const cornerColumns = options.corner?.columns ?? 0
    return grid.map((row, rowIndex) => Array.from({ length: width }, (_, columnIndex) => {
        const value = rawCellValue(row[columnIndex])
        // The header is one cell merged across the table, the way OpenL writes it.
        if (options.header && rowIndex === 0 && width > 1) {
            return columnIndex === 0 ? { value, colspan: width } : { value: null, covered: true }
        }
        // The band opens on the row after the header, and every title in it is merged down to the band's last row.
        if (rowIndex >= headerRows && rowIndex < headerRows + cornerRows && columnIndex < cornerColumns) {
            return rowIndex === headerRows ? { value, rowspan: cornerRows } : { value: null, covered: true }
        }
        return { value }
    }))
}

const isEmptyArgument = (argument: TableArgument): boolean => !argument.type.trim() && !argument.name.trim()
const isCompleteArgument = (argument: TableArgument): boolean => Boolean(argument.type.trim() && argument.name.trim())

const normalizeArguments = (argumentsValue: TableArgument[]): TableArgument[] =>
    withTrailingBlank(argumentsValue, isCompleteArgument, blankArgument)

const CreateTableForm: React.FC<{ detail: CreateTableModalDetail }> = ({ detail }) => {
    const { t } = useTranslation()
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const [modules, setModules] = useState<ModuleOption[]>([])
    // What the project holds. Each is asked for once: only the sheets belong to a module rather than to the project.
    const [datatypes, setDatatypes] = useState<ProjectTable[]>([])
    // Fields of the datatypes an author has picked. The tables list carries none, so each is read once, on demand.
    const [datatypeFields, setDatatypeFields] = useState<Record<string, DatatypeField[]>>({})
    const [properties, setProperties] = useState<ProjectProperty[]>([])
    // The tables a test can call. Read when a Test or a Run table is being written, and not before.
    const [executables, setExecutables] = useState<ProjectTable[]>([])
    const [loadingModules, setLoadingModules] = useState(true)
    const [loadingTargets, setLoadingTargets] = useState(false)
    const [loadingSkeleton, setLoadingSkeleton] = useState(false)
    const [creating, setCreating] = useState(false)
    const [selectedModule, setSelectedModule] = useState('')
    const [tableName, setTableName] = useState('')
    const [preset, setPreset] = useState<TablePreset>('datatype')
    const [resultType, setResultType] = useState(defaultResultType('datatype'))
    const [argumentsValue, setArgumentsValue] = useState<TableArgument[]>([blankArgument()])
    const [vocabularyType, setVocabularyType] = useState(DEFAULT_VOCABULARY_TYPE)
    const [extendsType, setExtendsType] = useState('')
    const [datatypeName, setDatatypeName] = useState('')
    const [target, setTarget] = useState<TargetStructure | null>(null)
    const [transposed, setTransposed] = useState(false)
    const [rows, setRows] = useState<TableCellValue[][]>(() => normalizeRows([], 5))
    const sheetLoader = useSheetLoader(t('project:create_table_modal.options_load_failed'))
    const { sheets, sheetName, setSheetName } = sheetLoader
    // Identifies the latest skeleton load; a response from an older one must not overwrite the current selection.
    const skeletonToken = useRef(0)
    // Counted apart from the skeleton: a table type that exercises nothing starts no skeleton load of its own, and
    // the list of callable tables asked for by the type before it must not still choose a table for it.
    const targetToken = useRef(0)
    // And once more for a type's fields: two names read in quick succession may answer out of order, and only the
    // one the author has settled on may size the grid.
    const fieldsToken = useRef(0)
    // Datatypes are cached as the requests themselves: expanding a tested argument walks the datatypes it nests
    // within one turn, long before a state update could be seen by the walk that follows it.
    const datatypeCache = useRef(new Map<string, Promise<ProjectDatatype>>())
    // Every value each vocabulary offers, by name. A cache rather than state: reads are followed by a skeleton
    // update, and the context hands out this very object, so values learned after it was memoized are visible then.
    // Type names are project data, so the cache has no prototype for names such as `constructor` to collide with.
    const vocabularyValues = useRef<Record<string, string[]>>(Object.create(null) as Record<string, string[]>)
    // The request for the callable tables rather than its answer, so two quick switches between Test and Run, or a
    // switch and an opened list, share the one that is already on its way.
    const executablesRequest = useRef<Promise<ProjectTable[]> | null>(null)
    // The module read still on its way. Choosing a Test table names the sheet after the table it exercises while the
    // module's own worksheets are being read, and both write the sheet field: this is what orders the two.
    const moduleRead = useRef<Promise<void>>(Promise.resolve())

    const close = useCallback(() => {
        globalThis.dispatchEvent(new CustomEvent('openCreateTableModal', { detail: null }))
    }, [])

    const context = useMemo<TableBuildContext>(() => ({
        resultType,
        resultFields: ownValue(datatypeFields, resultType) ?? [],
        arguments: argumentsValue,
        vocabularyType,
        extendsType,
        datatypeName,
        dataFields: ownValue(datatypeFields, datatypeName) ?? [],
        targetName: target?.table.name ?? '',
        targetColumns: target?.columns ?? [],
        vocabularyValues: vocabularyValues.current,
    }), [argumentsValue, datatypeFields, datatypeName, extendsType, resultType, target, vocabularyType])

    const gridWidth = rows[0]?.length ?? 1
    const columns = useMemo(
        () => buildTableColumns(preset, context, gridWidth),
        [context, gridWidth, preset]
    )
    /** Rows the table type owns at the top of the body; below them the grid is the author's. */
    const band = useMemo(() => headerBand(preset, context), [context, preset])

    const buildTypeOptions = useCallback((complexTypes: string[]) => [
        {
            label: t('project:create_table_modal.type_groups.simple'),
            options: SIMPLE_TYPE_OPTIONS,
        },
        {
            label: t('project:create_table_modal.type_groups.vocabularies'),
            options: asOptions(namesOf(datatypes, 'Vocabulary')),
        },
        {
            label: t('project:create_table_modal.type_groups.datatypes'),
            options: asOptions([...namesOf(datatypes, 'Datatype'), ...complexTypes]),
        },
    ].filter(group => group.options.length), [datatypes, t])

    /** Types a cell may hold. SpreadsheetResult is not among them: only a method signature can name it. */
    const typeOptions = useMemo(() => buildTypeOptions([]), [buildTypeOptions])

    /** Types a signature may declare, as an argument or as the result — including what a Spreadsheet returns. */
    const signatureTypeOptions = useMemo(() => buildTypeOptions([SPREADSHEET_RESULT]), [buildTypeOptions])

    const generatedHeader = useMemo(
        () => buildTableHeader(preset, tableName.trim(), context),
        [context, preset, tableName]
    )

    /**
     * Measures a grid against the columns the table type describes, and keeps a spare row — and, where the type
     * grows its own columns, a spare column — for the author to write in.
     *
     * <p>Every change that moves a column — the table type, the module's datatypes, the signature — has to go
     * through here, or the grid keeps a width the header row no longer matches.
     */
    const measureRows = (
        forPreset: TablePreset,
        nextContext: TableBuildContext,
        current: TableCellValue[][]
    ): TableCellValue[][] => {
        const grows = columnsGrow(forPreset)
        const grown = grows ? normalizeFreeFormColumns(current, minimumColumns(forPreset, nextContext)) : current
        // Re-measuring the grid already on screen — what every keystroke does — is the memoized column list itself.
        // Only a growing type widens with the grid, and this branch is not one, so the memo's `gridWidth` cannot
        // make it disagree.
        const measured = forPreset === preset && nextContext === context
            ? columns
            : buildTableColumns(forPreset, nextContext)
        const width = grows ? Math.max(1, grown[0]?.length ?? 1) : Math.max(1, measured.length)
        return normalizeRows(grown, width, minimumRows(forPreset, nextContext))
    }

    /** `seed` replaces the current rows; left out, the rows already entered are kept and only re-measured. */
    const resizeRows = (
        forPreset: TablePreset,
        nextContext: TableBuildContext,
        seed?: TableCellValue[][]
    ) => {
        setRows(current => measureRows(forPreset, nextContext, seed ?? current))
    }

    const resetRowsForPreset = (
        nextPreset: TablePreset,
        knownTypes: ProjectTable[],
        nextDatatypeName = namesOf(knownTypes, 'Datatype')[0] ?? '',
        knownFields: Record<string, DatatypeField[]> = datatypeFields
    ) => {
        const nextContext: TableBuildContext = {
            resultType: defaultResultType(nextPreset),
            resultFields: [],
            arguments: [{ ...EMPTY_ARGUMENT }],
            vocabularyType: DEFAULT_VOCABULARY_TYPE,
            extendsType: '',
            datatypeName: nextDatatypeName,
            dataFields: ownValue(knownFields, nextDatatypeName) ?? [],
            targetName: '',
            targetColumns: [],
            vocabularyValues: vocabularyValues.current,
        }
        resizeRows(nextPreset, nextContext, initialRows(nextPreset, nextContext))
    }

    const applyTarget = (
        structure: TargetStructure,
        nextPreset: 'test' | 'run',
        updateTableName: boolean
    ) => {
        setTarget(structure)
        const nextContext: TableBuildContext = {
            ...context,
            targetName: structure.table.name,
            targetColumns: structure.columns,
            // Read from the ref rather than from the context: the context was memoized before the types were.
            vocabularyValues: vocabularyValues.current,
        }
        setRows(measureRows(nextPreset, nextContext, initialRows(nextPreset, nextContext)))
        if (updateTableName) {
            // A generated table is named after the table it exercises; the author is free to rename it.
            const generated = targetTableName(structure.table.name, nextPreset)
            setTableName(generated)
            // The destination module is read at the same time and points the sheet field at one of its worksheets.
            // The tested table has the last word, so it waits for that answer instead of racing it — otherwise the
            // sheet the table is written to is whichever request the server happened to answer first.
            void moduleRead.current.then(() => setSheetName(sheetNameFrom(structure.table.name || generated)))
        }
    }

    /**
     * Reads one type the project declares: the fields of a datatype, or the values of a vocabulary.
     *
     * <p>Inherited fields come first, and a vocabulary's values are remembered so its editor can offer them. A name
     * the project does not declare has neither: a value of it is written as one cell rather than opened up. A type
     * already being read is not read again — one extending itself, directly or through another, describes an endless
     * chain of fields.
     */
    const typeReader = (known: ProjectTable[]) => {
        // Declared types are indexed once per reader: expanding a tested argument looks up every field's type, and a
        // project declaring hundreds of datatypes would otherwise be scanned once per column.
        const declaredByName = new Map(known.map(table => [table.name, table]))
        const read = async (typeName: string, reading: Set<string>): Promise<ProjectDatatype | null> => {
            const declared = declaredByName.get(typeName)
            if (!declared || reading.has(typeName)) {
                return null
            }
            reading.add(typeName)
            let request = datatypeCache.current.get(typeName)
            if (!request) {
                // One unreadable type leaves its column unopened rather than failing the whole skeleton.
                request = getDatatype(detail.projectId, declared.id).catch(() => ({ fields: [], values: []}))
                datatypeCache.current.set(typeName, request)
            }
            const structure = await request
            if (declared.tableType === 'Vocabulary') {
                vocabularyValues.current[typeName] = structure.values
            }
            const parent = structure.extends ? await read(structure.extends, reading) : null
            return { ...structure, fields: [...parent?.fields ?? [], ...structure.fields]}
        }
        return (typeName: string) => read(typeName, new Set())
    }

    const fieldsOf = (known: ProjectTable[]): FieldsOfType => {
        const read = typeReader(known)
        return async typeName => (await read(typeName))?.fields ?? null
    }

    /**
     * Learns every value of each vocabulary among these types.
     *
     * <p>The first value seeds an example cell and the complete set restricts its editor. Only vocabularies are read:
     * a datatype names no value a single cell could hold, and reading one to find that out would cost a request per
     * column.
     */
    const readVocabularies = async (types: string[], known = datatypes): Promise<void> => {
        const read = typeReader(known)
        const vocabularies = new Set(known.filter(table => table.tableType === 'Vocabulary').map(table => table.name))
        const declaredTypes = new Set(types.map(type => type.trim()))
        const requested = [...declaredTypes].filter(type => vocabularies.has(type))
        // Mark each declared vocabulary before its values arrive. Its cell becomes a restricted empty selection
        // immediately, so Create cannot submit the previous type's value while the request is still in flight.
        requested.forEach(type => {
            vocabularyValues.current[type] ??= []
        })
        await Promise.all(requested.map(read))
    }

    /** Re-renders value cells after a vocabulary's finite choices arrive without changing the grid's shape. */
    const refreshVocabularyEditors = (types: string[]) => {
        void readVocabularies(types).then(() => setRows(current => [...current]))
    }

    /**
     * Builds the Test or Run table for a tested table and puts it in the grid.
     *
     * <p>The columns come from the signature the tested table declares, so the only thing read from the server is
     * the datatypes an argument of one opens up into — and each of those only once.
     */
    const loadTarget = async (
        table: ProjectTable,
        nextPreset: 'test' | 'run',
        updateTableName = true,
        knownTypes = datatypes
    ) => {
        const token = ++skeletonToken.current
        setLoadingSkeleton(true)
        try {
            const structure = await buildTargetStructure(table, nextPreset, fieldsOf(knownTypes))
            await readVocabularies(structure.columns.map(column => column.type), knownTypes)
            if (token !== skeletonToken.current) {
                return
            }
            applyTarget(structure, nextPreset, updateTableName)
        } catch (error) {
            if (token !== skeletonToken.current) {
                return
            }
            setTarget(null)
            notification.error({
                title: t('project:create_table_modal.tested_table_load_failed'),
                description: errorMessage(error),
            })
        } finally {
            if (token === skeletonToken.current) {
                setLoadingSkeleton(false)
            }
        }
    }

    /**
     * The tables a Test or a Run table can call, read once and kept while the modal stays open.
     *
     * <p>Most tables are not tests, and the list is behind a compilation of the whole project, so it is asked for
     * when one of those two types is being written rather than when the dialog opens.
     *
     * <p>A failed request is not remembered: choosing the type again, or reopening the list, asks once more.
     */
    const loadExecutables = (projectId: string): Promise<ProjectTable[]> => {
        if (!executablesRequest.current) {
            setLoadingTargets(true)
            // Reopening the modal drops the request, and the modal may reopen on another project entirely, so an
            // answer is applied only while it is still the one being waited for.
            const request: Promise<ProjectTable[]> = getProjectTables(projectId, EXECUTABLE_KINDS)
                .then(loaded => {
                    if (executablesRequest.current === request) {
                        setExecutables(loaded)
                        setLoadingTargets(false)
                    }
                    return loaded
                })
                .catch(error => {
                    if (executablesRequest.current === request) {
                        // Forgotten rather than cached, so choosing the type again asks once more.
                        executablesRequest.current = null
                        setLoadingTargets(false)
                        notification.error({
                            title: t('project:create_table_modal.options_load_failed'),
                            description: errorMessage(error),
                        })
                    }
                    return []
                })
            executablesRequest.current = request
        }
        return executablesRequest.current
    }

    /**
     * Points a Test or a Run table at the table it exercises.
     *
     * <p>The table already chosen survives a switch between Test and Run, and its datatypes are cached, so that
     * switch asks the server for nothing. Without one there is nothing to build a skeleton from: the project's
     * callable tables are read, and the first of them is taken.
     */
    const selectTargetTable = async (nextPreset: 'test' | 'run', kept: TargetStructure | null, token: number) => {
        if (kept) {
            // The name follows the tested table only when that table changes; a name the author already has stays.
            await loadTarget(kept.table, nextPreset, false)
            return
        }
        setLoadingSkeleton(true)
        try {
            const first = (await loadExecutables(detail.projectId))
                .find(table => canTargetTable(table, nextPreset))
            if (token === targetToken.current && first) {
                await loadTarget(first, nextPreset, true)
            }
        } finally {
            // A project offering no callable table leaves the spinner to this branch: nothing else clears it.
            if (token === targetToken.current) {
                setLoadingSkeleton(false)
            }
        }
    }

    // Keyed on `detail` alone, deliberately: nothing else belongs in the dependency array. i18next hands out a new
    // `t` on every `languageChanged`/`loaded`, and the handlers called below are redefined on every render, so
    // depending on any of them would restart this effect and discard whatever the author had already typed.
    React.useEffect(() => {
        let active = true
        // What every table type needs is asked for once, and the three answers are independent of one another.
        Promise.all([
            getProjectModules(detail.projectId),
            getProjectTables(detail.projectId, ['Datatype']),
            getProjectProperties(detail.projectId),
            // Opening on Create Test opens on a Test table, so the tables one can call are read here after all:
            // the tested table's signature is what the skeleton is built from, and the list is where it is named.
            detail.sourceTableId ? loadExecutables(detail.projectId) : Promise.resolve([]),
            // The module the author is looking at is the one the table lands in, so its sheets are asked for at the
            // same time. A name this project does not declare answers with nothing, and the module chosen below
            // asks again for its own.
            detail.currentModuleName
                ? getModuleSheets(detail.projectId, detail.currentModuleName).catch(() => [])
                : Promise.resolve([]),
        ])
            .then(([loadedModules, loadedTypes, loadedProperties, callable, loadedSheets]) => {
                if (!active) {
                    return
                }
                const available = toModuleOptions(loadedModules)
                const current = available.find(module => module.name === detail.currentModuleName)
                const resolvedModule = current?.name ?? available[0]?.name ?? ''
                setModules(available)
                setSelectedModule(resolvedModule)
                setDatatypes(loadedTypes)
                setProperties(loadedProperties)
                const firstDatatype = namesOf(loadedTypes, 'Datatype')[0] ?? ''
                setDatatypeName(firstDatatype)
                const sourceTable = callable.find(table => table.id === detail.sourceTableId)
                if (sourceTable) {
                    setPreset('test')
                    void loadTarget(sourceTable, 'test', true, loadedTypes)
                } else {
                    resetRowsForPreset('datatype', loadedTypes, firstDatatype)
                    if (detail.sourceTableId) {
                        // A table the Create Test button offered that the project no longer lists as callable. The
                        // modal opens on its own default rather than on nothing at all.
                        notification.error({
                            title: t('project:create_table_modal.tested_table_load_failed'),
                        })
                    }
                }
                if (loadedSheets.length && resolvedModule === detail.currentModuleName) {
                    sheetLoader.prime(resolvedModule, loadedSheets)
                } else {
                    void sheetLoader.load(detail.projectId, resolvedModule, available)
                }
            })
            .catch(error => {
                if (!active) {
                    return
                }
                setModules([])
                setDatatypes([])
                setProperties([])
                setSelectedModule('')
                resetRowsForPreset('datatype', [])
                notification.error({
                    title: t('project:create_table_modal.modules_load_failed'),
                    description: errorMessage(error),
                })
            })
            .finally(() => {
                if (active) {
                    setLoadingModules(false)
                }
            })
        return () => {
            active = false
        }
    }, [detail])

    const moduleName = selectedModule.trim()
    // Whatever the project does not already declare is a new module, written to the folder its table
    // type belongs in. The author names it; the path follows.
    const isNewModule = Boolean(moduleName) && !modules.some(module => module.name === moduleName)
    const partialArgument = argumentsValue.some(argument => !isEmptyArgument(argument) && !isCompleteArgument(argument))
    const declaredArguments = argumentsValue.filter(isCompleteArgument)
    // Every argument becomes a parameter of the compiled method: its name has to be an identifier, and no two of
    // them may share one. Either way OpenL cannot bind the signature the header declares.
    const argumentNamesValid = declaredArguments.every(argument => IDENTIFIER.test(argument.name.trim()))
        && new Set(declaredArguments.map(argument => argument.name.trim())).size === declaredArguments.length
    const typeSpecificValid = !partialArgument
        && argumentNamesValid
        && (!SIGNATURE_PRESETS.has(preset) || Boolean(resultType.trim()))
        // A lookup spreads its arguments over two axes, so it takes two before there is anything to look up by.
        && (!isLookup(preset) || declaredArguments.length >= 2)
        && (preset !== 'vocabulary' || Boolean(vocabularyType))
        && (preset !== 'data' || Boolean(datatypeName && context.dataFields.length))
        && (!isTargeted(preset) || Boolean(target?.columns.length))
    // A compiled table's name becomes an OpenL identifier; a type that carries no name imposes nothing.
    const tableNameValid = !hasTableName(preset) || IDENTIFIER.test(tableName.trim())
    // Blank rows are stripped before the table is written, so a preset that needs a body needs a filled row.
    const bodyValid = useMemo(() => bodyIsValid(preset, context, rows), [context, preset, rows])
    const typedValuesValid = useMemo(() => rows.every((row, rowIndex) =>
        columns.every((column, columnIndex) => {
            if (column.editor !== 'value') {
                return true
            }
            const declaredType = cellValueType(preset, context, rows, rowIndex, columnIndex)
            return tableValueIsValid(declaredType, row[columnIndex] ?? '', context.vocabularyValues,
                cellHoldsCondition(preset, context, rowIndex, columnIndex))
        })), [columns, context, preset, rows])
    // The table exactly as it will be written: the editor always keeps a blank row (and a blank Free Form column)
    // for input, and OpenL reads a blank line as a table boundary, so none of them may be submitted.
    const submittedBody = useMemo(() => {
        if (columnsGrow(preset)) {
            return dropEmptyTrailingColumns(dropEmptyRows(rows))
        }
        // A free-text type can temporarily narrow the rendered columns without resizing the rows. Only cells the
        // current table shape exposes belong to the submitted table; stale hidden cells must not reach the workbook.
        return dropEmptyRows(rows.map(row => row.slice(0, columns.length)))
    }, [columns.length, preset, rows])
    // The name OpenL will compile, read from the generated header the same way the compiler reads it. A Free Form
    // table has no header of its own, so OpenL names it after the first cell instead.
    const submittedName = deriveTableName(preset,
        hasTableHeader(preset) ? generatedHeader : String(submittedBody[0]?.[0] ?? ''))
    const valid = Boolean(
        moduleName
        && tableNameValid
        && isValidSheetName(sheetName)
        && submittedName
        && typeSpecificValid
        && bodyValid
        && typedValuesValid
    )

    const moduleOptions = useMemo(() => toSortedOptions(modules), [modules])

    // The option arrays below are memoized so a keystroke anywhere in the modal does not hand every Select a new
    // array identity, which makes rc-select re-flatten and re-filter a project-wide list on each render.
    const presetOptions = useMemo(() => TABLE_PRESETS.map(value => ({
        label: t(`project:create_table_modal.types.${value}`),
        value,
    })), [t])

    const sheetOptions = useMemo(() => asOptions(sheets), [sheets])

    const datatypeOptions = useMemo(() => asOptions(namesOf(datatypes, 'Datatype')), [datatypes])

    const targetOptions = useMemo(() => isTargeted(preset)
        ? executables
            .filter(table => canTargetTable(table, preset))
            .map(table => ({ label: table.name, value: table.id }))
        : [], [executables, preset])

    const handleTableNameChange = (value: string) => {
        setTableName(value)
        // The sheet mirrors the name until the author points it elsewhere. It follows the name clipped to Excel's
        // limit, not the raw name: a name past that limit would otherwise leave a sheet Excel rejects and disable
        // Create, though the name itself is a table OpenL compiles.
        setSheetName(current => current === sheetNameFrom(tableName) ? sheetNameFrom(value) : current)
    }

    const handleModuleChange = (value: string) => {
        setSelectedModule(value)
        moduleRead.current = sheetLoader.load(detail.projectId, value, modules)
    }

    const handlePresetChange = (value: TablePreset) => {
        // Taken for every type, not only the two that exercise a table: leaving one of them is what has to cancel
        // the table it was still choosing.
        const token = ++targetToken.current
        const fieldsVersion = ++fieldsToken.current
        // A skeleton being built for the type being left has to be cancelled too. Only `loadTarget` moves this
        // count, and a switch to a type that exercises no table never calls it, so its answer would otherwise
        // still pass its own guard and stamp a Test skeleton over the type just chosen.
        skeletonToken.current++
        setLoadingSkeleton(false)
        setPreset(value)
        setResultType(defaultResultType(value))
        setArgumentsValue([blankArgument()])
        setVocabularyType(DEFAULT_VOCABULARY_TYPE)
        setExtendsType('')
        setTransposed(false)
        // A Test and a Run table are written against the same tested table, so switching between the two keeps the
        // one already chosen; every other type is not written against a table at all.
        const keptTarget = isTargeted(value) && isTargeted(preset) ? target : null
        setTarget(keptTarget)
        if (value === 'data') {
            // A Data table's columns are the datatype's fields, so they have to be in hand before the grid is built.
            void loadFields(datatypeName).then(dataFields => {
                if (fieldsVersion === fieldsToken.current) {
                    rememberFields(datatypeName, dataFields)
                    resetRowsForPreset(value, datatypes, datatypeName, { [datatypeName]: dataFields })
                }
            })
        } else {
            resetRowsForPreset(value, datatypes, datatypeName)
        }
        // Only move the destination when the author picked an existing module; a name they typed themselves stays.
        // Trimmed, as every other read of the field is: a stray space must not make a declared module look new and
        // leave a Test table behind in the rules module.
        const destination = modules.some(module => module.name === moduleName)
            ? preferredModule(modules, tableKind(value), moduleName)
            : selectedModule
        if (destination !== selectedModule) {
            handleModuleChange(destination)
        }
        // A Test or Run table opens on a table to exercise; the list is the project's, not the destination's.
        if (isTargeted(value)) {
            void selectTargetTable(value, keptTarget, token)
        }
    }

    // Only these derive their columns from the signature; every other type keeps the grid it already has.
    const columnsFollowSignature = preset === 'smartRules' || preset === 'simpleRules' || isLookup(preset)

    const resizeRowsForArguments = (nextArguments: TableArgument[]) => {
        if (!columnsFollowSignature) {
            return
        }
        const nextContext = { ...context, arguments: nextArguments, vocabularyValues: vocabularyValues.current }
        const currentArguments = argumentsValue.filter(isCompleteArgument)
        const completeNextArguments = nextArguments.filter(isCompleteArgument)
        // Declaring or dropping an argument moves a column: it is inserted among the inputs, ahead of the outputs.
        // Re-measuring only pads or truncates at the right edge, which would leave every value one column from
        // where it was typed — an output read as an input. So the example is rebuilt instead. Renaming an argument
        // changes nothing about the shape, and rebuilding then would throw away everything typed so far, one
        // keystroke at a time.
        const shapeChanged = completeNextArguments.length !== currentArguments.length
        if (shapeChanged) {
            // Each column of the rebuilt example holds a value of its argument's type, a vocabulary's among them.
            // Guarded like every other rebuild: leaving the table type, or moving the signature again, must not let
            // this answer stamp the example of the shape it was started for over the grid now on screen.
            const token = ++fieldsToken.current
            void readVocabularies(nextArguments.map(argument => argument.type)).then(() => {
                if (token === fieldsToken.current) {
                    resizeRows(preset, nextContext, initialRows(preset, nextContext))
                }
            })
            return
        }
        const typesChanged = completeNextArguments.some((argument, index) =>
            argument.type.trim() !== currentArguments[index]?.type.trim())
        if (typesChanged) {
            // The columns stay in place, but a newly declared vocabulary needs its finite choices before the cell can
            // switch to the restricted editor. Keep the values already entered; invalid ones remain visible as empty
            // selections and keep Create disabled until the author replaces them.
            refreshVocabularyEditors(completeNextArguments.map(argument => argument.type))
            return
        }
        resizeRows(preset, nextContext)
    }

    /** Reads the fields of the datatype a Data table or a result type names. */
    const loadFields = async (name: string): Promise<DatatypeField[]> => {
        const loaded = await fieldsOf(datatypes)(name) ?? []
        await readVocabularies(loaded.map(field => field.type))
        return loaded
    }

    const rememberFields = (name: string, fields: DatatypeField[]) => {
        setDatatypeFields(current => ({ ...current, [name]: fields }))
    }

    /**
     * Whether this name is a type the grid can be sized to: one the project declares, or one the language fixes.
     *
     * <p>A half-typed name is neither, and sizing the grid to it would collapse the output columns.
     */
    const namesAType = (name: string): boolean => {
        const declared = name.trim()
        return SIMPLE_TYPES.includes(declared)
            || declared === SPREADSHEET_RESULT
            || datatypes.some(table => table.name === declared)
    }

    const handleResultTypeChange = (value: string) => {
        setResultType(value)
        if (preset === 'rules') {
            refreshVocabularyEditors([value])
            return
        }
        if (!columnsFollowSignature) {
            return
        }
        // The field is free text, so every keystroke on the way to a name arrives here. A name the project does not
        // declare yet has no fields, and resizing to it would narrow the grid to the single fallback column and drop
        // whatever was typed in the others for good. The columns follow a name that resolves; the rest just type.
        if (!namesAType(value)) {
            return
        }
        // Trimmed, exactly as `namesAType` accepted it: types are keyed on the name the project declares, and looking
        // one up with the author's trailing space would find no fields and collapse the grid to its one fallback
        // column, discarding whatever was typed in the others.
        const declared = value.trim()
        const token = ++fieldsToken.current
        void loadFields(declared).then(resultFields => {
            // An earlier type's fields may arrive after a later one's; only the newest may move the columns.
            if (token === fieldsToken.current) {
                rememberFields(declared, resultFields)
                resizeRows(preset, { ...context, resultType: declared, resultFields })
            }
        })
    }

    /**
     * Replaces the signature and resizes the grid to the columns it now describes.
     *
     * <p>The new list is computed before the state is set, not inside the updater: React may run an updater more
     * than once, and the resize is a side effect that must happen exactly once per change.
     */
    const changeArguments = (transform: (current: TableArgument[]) => TableArgument[]) => {
        const next = normalizeArguments(transform(argumentsValue))
        setArgumentsValue(next)
        resizeRowsForArguments(next)
    }

    const updateArgument = (index: number, field: keyof TableArgument, value: string) => changeArguments(
        current => current.map((argument, argumentIndex) =>
            argumentIndex === index ? { ...argument, [field]: value } : argument))

    const insertArgument = (index: number) => changeArguments(
        current => insertAt(current, index, blankArgument()))

    const removeArgument = (index: number) => changeArguments(current => deleteAt(current, index))

    const normalizeEditorRows = (nextRows: TableCellValue[][]): TableCellValue[][] =>
        measureRows(preset, context, nextRows)

    const updateCell = (row: number, column: number, value: TableCellValue) => {
        setRows(current => normalizeEditorRows(current.map((currentRow, rowIndex) => {
            if (rowIndex !== row) {
                return currentRow
            }
            // The grid can render more columns than the row holds; sizing by the target column keeps that write.
            const width = Math.max(currentRow.length, column + 1)
            return Array.from({ length: width }, (_, columnIndex) =>
                columnIndex === column ? value : currentRow[columnIndex] ?? '')
        })))
    }

    const updateCellForEditor = (row: number, column: number, value: TableCellValue) => {
        if ((preset !== 'datatype' && preset !== 'constants') || column !== 0) {
            updateCell(row, column, value)
            return
        }
        const dependentColumns = preset === 'datatype' ? new Set([2, 5]) : new Set([2])
        setRows(current => normalizeEditorRows(current.map((currentRow, rowIndex) => {
            if (rowIndex !== row) {
                return currentRow
            }
            return Array.from({ length: Math.max(currentRow.length, columns.length) }, (_, columnIndex) => {
                if (columnIndex === column) {
                    return value
                }
                return dependentColumns.has(columnIndex) ? '' : currentRow[columnIndex] ?? ''
            })
        })))
        if (preset === 'datatype') {
            void readVocabularies([String(value)]).then(() => setRows(current => [...current]))
        }
    }

    const insertRow = (index: number) => {
        setRows(current => normalizeEditorRows(insertAt(
            current,
            index,
            new Array<TableCellValue>(Math.max(1, columns.length)).fill('')
        )))
    }

    const removeRow = (index: number) => {
        setRows(current => normalizeEditorRows(deleteAt(current, index)))
    }

    const insertColumn = (index: number) => {
        setRows(current => normalizeEditorRows(current.map(row => insertAt(row, index, ''))))
    }

    const removeColumn = (index: number) => {
        setRows(current => normalizeEditorRows(current.map(row => deleteAt(row, index))))
    }

    const handleDatatypeChange = (value: string) => {
        setDatatypeName(value)
        const token = ++fieldsToken.current
        void loadFields(value).then(dataFields => {
            if (token === fieldsToken.current) {
                rememberFields(value, dataFields)
                resizeRows('data', { ...context, datatypeName: value, dataFields }, [])
            }
        })
    }

    const handleVocabularyTypeChange = (value: string) => {
        setVocabularyType(value)
        const nextContext = { ...context, vocabularyType: value }
        resizeRows('vocabulary', nextContext, initialRows('vocabulary', nextContext))
    }

    const handleTargetChange = (tableId: string) => {
        // The author has named the table they want, which cancels the default still being chosen for them.
        targetToken.current++
        const table = executables.find(candidate => candidate.id === tableId)
        if (table && isTargeted(preset)) {
            void loadTarget(table, preset)
        }
    }

    /**
     * Reads the callable tables when the author asks to see them.
     *
     * <p>A Test table created from the rules editor opens on the table it was created from, so the list is not
     * needed until the author looks for another one.
     */
    const handleTargetOpen = (open: boolean) => {
        if (open) {
            void loadExecutables(detail.projectId)
        }
    }

    const propertyOptions = useMemo(() => asOptions(properties.map(property => property.name)), [properties])

    const suggestionsFor = (editor: TableCellEditor) => {
        switch (editor) {
            case 'simpleType':
                return SIMPLE_TYPE_OPTIONS
            case 'environment':
                return ENVIRONMENT_KEY_OPTIONS
            case 'property':
                return propertyOptions
            default:
                return typeOptions
        }
    }

    // Read once per render rather than per cell: the grid asks for the same six labels on every row and column it
    // draws, and a wide Test table draws hundreds of them.
    const insertColumnLeft = t('project:create_table_modal.insert_column_left')
    const insertColumnRight = t('project:create_table_modal.insert_column_right')
    const deleteColumnLabel = t('project:create_table_modal.delete_column')
    const insertRowAbove = t('project:create_table_modal.insert_row_above')
    const insertRowBelow = t('project:create_table_modal.insert_row_below')
    const deleteRowLabel = t('project:create_table_modal.delete_row')

    const renderCellEditor = (column: TableColumn, value: TableCellValue, rowIndex: number, columnIndex: number) => {
        const common = {
            'data-testid': `create-table-cell-${rowIndex}-${columnIndex}`,
            'aria-label': t('project:create_table_modal.cell', {
                row: rowIndex + 1,
                column: columnIndex + 1,
            }),
        }
        if (column.editor === 'value') {
            return (
                <TypedTableValueInput
                    {...common}
                    condition={cellHoldsCondition(preset, context, rowIndex, columnIndex)}
                    onChange={next => updateCellForEditor(rowIndex, columnIndex, next)}
                    type={cellValueType(preset, context, rows, rowIndex, columnIndex)}
                    value={value}
                    vocabularyValues={context.vocabularyValues}
                />
            )
        }
        if (column.editor === 'text') {
            if (preset === 'properties' && columnIndex === 1) {
                const definition = properties.find(property => property.name === String(rows[rowIndex]?.[0] ?? ''))
                return (
                    <PropertyValueInput
                        {...common}
                        definition={definition}
                        onChange={next => updateCell(rowIndex, columnIndex, next)}
                        placeholder=""
                        value={value}
                    />
                )
            }
            return (
                <Input
                    {...common}
                    onChange={event => updateCellForEditor(rowIndex, columnIndex, event.target.value)}
                    value={String(value)}
                />
            )
        }
        if (column.editor === 'checkbox') {
            return (
                <div className={shared.checkboxEditor} data-testid={`${common['data-testid']}-wrapper`}>
                    <Checkbox
                        {...common}
                        checked={value === true}
                        onChange={event => updateCell(
                            rowIndex,
                            columnIndex,
                            event.target.checked ? true : ''
                        )}
                    />
                </div>
            )
        }
        // Every remaining editor suggests values without restricting them: a cell may hold a type, key or property
        // the module does not declare yet, so the list is a shortcut, never a whitelist.
        return (
            <SuggestInput
                {...common}
                options={suggestionsFor(column.editor)}
                value={String(value)}
                onChange={next => {
                    updateCellForEditor(rowIndex, columnIndex, next)
                    if (preset === 'properties' && columnIndex === 0) {
                        const definition = properties.find(property => property.name === next)
                        updateCell(rowIndex, 1, initialPropertyValue(definition))
                    }
                }}
            />
        )
    }

    const cellClassName = (column: TableColumn, columnIndex: number) => cx(
        preset === 'datatype' && COMPACT_DATATYPE_COLUMNS.has(column.key)
            ? styles.compactCell
            : styles.cell,
        preset === 'properties' && columnIndex === 1 ? styles.propertyValueCell : undefined
    )

    const renderTableHeader = (colSpan: number) => hasTableHeader(preset) ? (
        <tr className={styles.headerBand}>
            <th colSpan={colSpan}>
                <span data-testid="create-table-header">{generatedHeader}</span>
            </th>
        </tr>
    ) : null

    /**
     * The rows a table type opens its body with: a lookup's arguments, titled down the left and merged over the
     * whole band, or the row in which a Spreadsheet names its columns.
     *
     * <p>The band belongs to the table type rather than to the author — a lookup's height is what tells OpenL how
     * many arguments run across the top — so its rows carry no row controls of their own.
     */
    const renderHeaderBand = () => Array.from({ length: band.rows }, (_, bandIndex) => (
        <tr key={`band-${bandIndex}`} className={styles.bandRow}>
            <Tooltip title={band.hints[bandIndex]}>
                <td className={styles.gutter}>{bandIndex + 1}</td>
            </Tooltip>
            {bandIndex === 0
                ? band.titles.map((title, keyIndex) => (
                    <th key={`key-${keyIndex}`} rowSpan={band.rows}>{title}</th>
                ))
                : null}
            {columns.slice(band.keys).map((column, valueIndex) => (
                <td
                    key={column.key}
                    className={cellClassName(column, band.keys + valueIndex)}
                >
                    {renderCellEditor(
                        column,
                        rows[bandIndex]?.[band.keys + valueIndex] ?? '',
                        bandIndex,
                        band.keys + valueIndex
                    )}
                </td>
            ))}
            <td className={styles.rowActions} />
        </tr>
    ))

    /** One row of the body: the author's cells, numbered on the left and with the row controls on the right. */
    const renderBodyRow = (row: TableCellValue[], rowIndex: number) => (
        <tr key={`row-${rowIndex}`}>
            <td className={styles.gutter}>{rowIndex + 1}</td>
            {columns.map((column, columnIndex) => (
                <td
                    key={`${column.key}-${columnIndex}`}
                    className={cellClassName(column, columnIndex)}
                >
                    {renderCellEditor(column, row[columnIndex] ?? '', rowIndex, columnIndex)}
                </td>
            ))}
            <td className={styles.rowActions}>
                <Space.Compact>
                    <IconAction
                        icon={<InsertRowAboveOutlined />}
                        onClick={() => insertRow(rowIndex)}
                        size="small"
                        title={insertRowAbove}
                    />
                    <IconAction
                        icon={<InsertRowBelowOutlined />}
                        onClick={() => insertRow(rowIndex + 1)}
                        size="small"
                        title={insertRowBelow}
                    />
                    <IconAction
                        icon={<DeleteRowOutlined />}
                        onClick={() => removeRow(rowIndex)}
                        size="small"
                        title={deleteRowLabel}
                    />
                </Space.Compact>
            </td>
        </tr>
    )

    const renderTransposedTable = () => {
        const fieldNames = preset === 'data'
            ? context.dataFields.map(field => field.name)
            : context.targetColumns.map(column => column.name)
        const fieldTitles = preset === 'data'
            ? context.dataFields.map(field => title(field.name))
            : context.targetColumns.map(column => column.title)
        return (
            <>
                <thead>
                    {renderTableHeader(rows.length + 3)}
                    <tr className={styles.columnRow}>
                        <th className={styles.gutter} />
                        <th>{t('project:create_table_modal.field')}</th>
                        <th>{t('project:create_table_modal.field_title')}</th>
                        {rows.map((_row, recordIndex) => (
                            <th key={`record-${recordIndex}`}>
                                <div className={styles.columnHeader}>
                                    <span>{recordIndex + 1}</span>
                                    <Space.Compact>
                                        <IconAction
                                            icon={<InsertRowLeftOutlined />}
                                            onClick={() => insertRow(recordIndex)}
                                            size="small"
                                            title={t('project:create_table_modal.insert_record_before')}
                                        />
                                        <IconAction
                                            icon={<InsertRowRightOutlined />}
                                            onClick={() => insertRow(recordIndex + 1)}
                                            size="small"
                                            title={t('project:create_table_modal.insert_record_after')}
                                        />
                                        <IconAction
                                            icon={<DeleteColumnOutlined />}
                                            onClick={() => removeRow(recordIndex)}
                                            size="small"
                                            title={t('project:create_table_modal.delete_record')}
                                        />
                                    </Space.Compact>
                                </div>
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {columns.map((column, columnIndex) => (
                        <tr key={column.key}>
                            <td className={styles.gutter}>{columnIndex + 1}</td>
                            <th className={styles.structureCell}>{fieldNames[columnIndex]}</th>
                            <th className={styles.structureCell}>{fieldTitles[columnIndex]}</th>
                            {rows.map((row, rowIndex) => (
                                <td
                                    key={`record-${rowIndex}`}
                                    className={cellClassName(column, columnIndex)}
                                >
                                    {renderCellEditor(
                                        column,
                                        row[columnIndex] ?? '',
                                        rowIndex,
                                        columnIndex
                                    )}
                                </td>
                            ))}
                        </tr>
                    ))}
                </tbody>
            </>
        )
    }

    const handleCreate = async () => {
        if (!valid || creating) {
            return
        }
        setCreating(true)
        try {
            const source = buildTableSource(preset, generatedHeader, context, submittedBody, transposed)
            const table = await createTable(detail.projectId, {
                moduleName,
                sheetName: sheetName.trim(),
                ...(isNewModule ? { modulePath: defaultModulePath(moduleName, tableKind(preset)) } : {}),
                table: {
                    tableType: 'RawSource',
                    kind: tableKind(preset),
                    // Read from the generated header, the way OpenL names the table it compiles. Constants and
                    // similar table types carry no author-entered name of their own.
                    name: submittedName,
                    source: toRawSource(source, {
                        header: hasTableHeader(preset),
                        ...(band.keys ? { corner: { rows: band.rows, columns: band.keys } } : {}),
                    }),
                },
            })
            if (table) {
                close()
                detail.onSuccess?.(table, moduleName)
            }
        } finally {
            setCreating(false)
        }
    }

    const busy = loadingModules || loadingSkeleton || sheetLoader.loading

    return (
        <Modal
            destroyOnHidden
            open
            cancelButtonProps={{ disabled: creating }}
            cancelText={t('common:btn.cancel')}
            closable={!creating}
            keyboard={!creating}
            mask={{ closable: false }}
            okText={t('project:create_table_modal.create')}
            onCancel={close}
            onOk={handleCreate}
            width={1200}
            okButtonProps={{
                disabled: busy || !valid,
                loading: creating,
            }}
            title={(
                <Space>
                    <TableOutlined />
                    {t('project:create_table_modal.title')}
                </Space>
            )}
        >
            <Spin spinning={busy}>
                <div className={shared.form}>
                    <div className={shared.settings}>
                        <div className={shared.fields}>
                            <FieldRow
                                required
                                htmlFor="create-table-type"
                                label={t('project:create_table_modal.table_type')}
                            >
                                <Select
                                    className={shared.fullWidth}
                                    data-testid="create-table-type"
                                    id="create-table-type"
                                    onChange={handlePresetChange}
                                    options={presetOptions}
                                    value={preset}
                                />
                            </FieldRow>
                            {hasTableName(preset) ? (
                                <FieldRow
                                    required
                                    htmlFor="create-table-name"
                                    label={t('project:create_table_modal.table_name')}
                                >
                                    <Input
                                        data-testid="create-table-name"
                                        id="create-table-name"
                                        onChange={event => handleTableNameChange(event.target.value)}
                                        value={tableName}
                                    />
                                </FieldRow>
                            ) : null}
                        </div>
                        <div className={shared.fields}>
                            <FieldRow
                                required
                                htmlFor="create-table-module"
                                label={t('project:create_table_modal.module')}
                            >
                                <SuggestInput
                                    className={shared.fullWidth}
                                    data-testid="create-table-module"
                                    id="create-table-module"
                                    onChange={value => handleModuleChange(value)}
                                    options={moduleOptions}
                                    value={selectedModule}
                                />
                            </FieldRow>
                            <FieldRow
                                required
                                htmlFor="create-table-sheet"
                                label={t('project:create_table_modal.sheet')}
                            >
                                <SuggestInput
                                    className={shared.fullWidth}
                                    data-testid="create-table-sheet"
                                    id="create-table-sheet"
                                    onChange={setSheetName}
                                    options={sheetOptions}
                                    value={sheetName}
                                />
                            </FieldRow>
                        </div>
                        <div className={shared.fields}>
                            {preset === 'datatype' ? (
                                <FieldRow
                                    htmlFor="create-table-extends"
                                    label={t('project:create_table_modal.extends')}
                                >
                                    <SuggestInput
                                        className={shared.fullWidth}
                                        data-testid="create-table-extends"
                                        id="create-table-extends"
                                        onChange={setExtendsType}
                                        options={datatypeOptions}
                                        value={extendsType}
                                    />
                                </FieldRow>
                            ) : null}
                            {preset === 'vocabulary' ? (
                                <FieldRow
                                    required
                                    htmlFor="create-table-vocabulary-type"
                                    label={t('project:create_table_modal.base_type')}
                                >
                                    <Select
                                        className={shared.fullWidth}
                                        data-testid="create-table-vocabulary-type"
                                        id="create-table-vocabulary-type"
                                        onChange={handleVocabularyTypeChange}
                                        options={SIMPLE_TYPE_OPTIONS}
                                        value={vocabularyType}
                                    />
                                </FieldRow>
                            ) : null}
                            {preset === 'data' ? (
                                <FieldRow
                                    required
                                    htmlFor="create-table-datatype"
                                    label={t('project:create_table_modal.datatype')}
                                >
                                    <Select
                                        showSearch
                                        className={shared.fullWidth}
                                        data-testid="create-table-datatype"
                                        id="create-table-datatype"
                                        onChange={handleDatatypeChange}
                                        options={datatypeOptions}
                                        value={datatypeName || null}
                                    />
                                </FieldRow>
                            ) : null}
                            {isTargeted(preset) ? (
                                <FieldRow
                                    required
                                    htmlFor="create-table-target"
                                    label={t('project:create_table_modal.tested_table')}
                                >
                                    <Select
                                        showSearch
                                        className={shared.fullWidth}
                                        data-testid="create-table-target"
                                        id="create-table-target"
                                        loading={loadingTargets}
                                        onChange={handleTargetChange}
                                        onOpenChange={handleTargetOpen}
                                        options={targetOptions}
                                        value={target?.table.id ?? null}
                                    />
                                </FieldRow>
                            ) : null}
                            {isTransposable(preset) ? (
                                <FieldRow label={t('project:create_table_modal.transposed')}>
                                    <Checkbox
                                        checked={transposed}
                                        data-testid="create-table-transposed"
                                        onChange={event => setTransposed(event.target.checked)}
                                    />
                                </FieldRow>
                            ) : null}
                        </div>
                        {SIGNATURE_PRESETS.has(preset) ? (
                            <div className={shared.section}>
                                <FieldRow
                                    required
                                    htmlFor="create-table-result-type"
                                    label={t('project:create_table_modal.result_type')}
                                >
                                    <div
                                        className={shared.rowColumns}
                                        data-testid="create-table-result-type-row"
                                    >
                                        <SuggestInput
                                            className={shared.fullWidth}
                                            data-testid="create-table-result-type"
                                            id="create-table-result-type"
                                            onChange={handleResultTypeChange}
                                            options={signatureTypeOptions}
                                            value={resultType}
                                        />
                                    </div>
                                </FieldRow>
                                <FieldRow alignTop label={t('project:create_table_modal.input_arguments')}>
                                    <div className={shared.rowList}>
                                        {argumentsValue.map((argument, index) => (
                                            <div
                                                key={`argument-${index}`}
                                                className={cx(shared.rowColumns, shared.editableRow)}
                                                data-testid={`create-table-argument-row-${index}`}
                                            >
                                                <SuggestInput
                                                    data-testid={`create-table-argument-type-${index}`}
                                                    onChange={value => updateArgument(index, 'type', value)}
                                                    options={signatureTypeOptions}
                                                    placeholder={t('project:create_table_modal.argument_type')}
                                                    value={argument.type}
                                                />
                                                <Input
                                                    data-testid={`create-table-argument-name-${index}`}
                                                    onChange={e => updateArgument(index, 'name', e.target.value)}
                                                    placeholder={t('project:create_table_modal.argument_name')}
                                                    value={argument.name}
                                                />
                                                <Space.Compact>
                                                    <IconAction
                                                        icon={<InsertRowAboveOutlined />}
                                                        onClick={() => insertArgument(index)}
                                                        title={t('project:create_table_modal.insert_argument')}
                                                    />
                                                    <IconAction
                                                        icon={<DeleteRowOutlined />}
                                                        onClick={() => removeArgument(index)}
                                                        title={t('project:create_table_modal.delete_argument')}
                                                    />
                                                </Space.Compact>
                                            </div>
                                        ))}
                                    </div>
                                </FieldRow>
                            </div>
                        ) : null}
                    </div>
                    <div className={styles.sheet}>
                        <table className={styles.grid} data-testid="create-table-skeleton">
                            {transposed && isTransposable(preset) ? renderTransposedTable() : (
                                <>
                                    <thead>
                                        {renderTableHeader(columns.length + 2)}
                                        {band.rows ? renderHeaderBand() : (
                                            <tr className={styles.columnRow}>
                                                <th className={styles.gutter} />
                                                {columns.map((column, columnIndex) => (
                                                    <th key={column.key}>
                                                        <div className={styles.columnHeader}>
                                                            <span>{column.label}</span>
                                                            {preset === 'freeForm' ? (
                                                                <Space.Compact>
                                                                    <IconAction
                                                                        icon={<InsertRowLeftOutlined />}
                                                                        onClick={() => insertColumn(columnIndex)}
                                                                        size="small"
                                                                        title={insertColumnLeft}
                                                                    />
                                                                    <IconAction
                                                                        icon={<InsertRowRightOutlined />}
                                                                        onClick={() => insertColumn(columnIndex + 1)}
                                                                        size="small"
                                                                        title={insertColumnRight}
                                                                    />
                                                                    <IconAction
                                                                        icon={<DeleteColumnOutlined />}
                                                                        onClick={() => removeColumn(columnIndex)}
                                                                        size="small"
                                                                        title={deleteColumnLabel}
                                                                    />
                                                                </Space.Compact>
                                                            ) : null}
                                                        </div>
                                                    </th>
                                                ))}
                                                <th className={styles.rowActions} />
                                            </tr>
                                        )}
                                    </thead>
                                    <tbody>
                                        {/* The band rows are drawn above, by the header; the body starts after them. */}
                                        {rows.slice(band.rows).map((row, bodyIndex) =>
                                            renderBodyRow(row, bodyIndex + band.rows))}
                                    </tbody>
                                </>
                            )}
                        </table>
                    </div>
                </div>
            </Spin>
        </Modal>
    )
}

/**
 * Writes a new table into a module of the project, building its skeleton from the table type the author chooses.
 *
 * <p>The form is mounted only while the dialog is open, so closing it discards every field, every cached datatype
 * and the project's callable-table list rather than holding them until the page is reloaded.
 */
export const CreateTableModal: React.FC = () => {
    const { detail } = useGlobalEvents<CreateTableModalDetail>('openCreateTableModal')
    return detail
        ? <CreateTableForm key={`${detail.projectId} ${detail.sourceTableId ?? ''}`} detail={detail} />
        : null
}
