import React, { type SetStateAction, useCallback, useMemo, useRef, useState } from 'react'
import { App, Alert, Checkbox, Input, Modal, Select, Space, Spin, Tooltip } from 'antd'
import type { ModalProps } from 'antd'
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
import { createTable, getProjectTables, getProjectTypes } from 'services/tables'
import type {
    DatatypeField,
    ProjectProperty,
    ProjectTable,
    ProjectType,
    RawTableCellInput,
    SummaryTable,
} from 'types/tables'
import {
    bodyIsValid,
    buildTableColumns,
    buildTableHeader,
    buildTableRegions,
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
    EXECUTABLE_KINDS,
    type FieldsOfType,
    targetTableName,
    type TargetStructure,
} from './testSkeleton'
import { tableValueIsValid, TypedTableValueInput } from './TypedTableValueInput'
import {
    asOptions,
    defaultModulePath,
    deleteAt,
    destinationOf,
    IDENTIFIER,
    insertAt,
    isValidSheetName,
    type ModuleFormat,
    type ModuleOption,
    preferredModule,
    sheetNameFrom,
    toModuleOptions,
    toPropertyGroups,
    toSortedOptions,
    withTrailingBlank,
} from '../tableModals/shared'
import { useSharedStyles } from '../tableModals/sharedStyles'
import { useSheetLoader } from '../tableModals/useSheetLoader'
import { WriteButton } from '../tableModals/WriteButton'
import { initialPropertyValue, PropertyValueInput } from '../tableModals/PropertyValueInput'
import { useStyles } from './CreateTableModal.styles'

const COMPACT_DATATYPE_COLUMNS = new Set(['default', 'mandatory', 'example'])
const DEFAULT_VOCABULARY_TYPE = 'String'
/** The type a Spreadsheet returns. Offered in a signature only, never as a cell type. */
const SPREADSHEET_RESULT = 'SpreadsheetResult'
const EMPTY_ARGUMENT: TableArgument = { type: '', name: '' }

/** An argument of the signature being written, with the id that tells its row apart while it is edited. */
interface ArgumentRow extends TableArgument {
    id: number
}

/** The next argument row's id: a row keyed by its place in the list would carry its neighbour's state when one goes. */
let nextArgumentId = 0

const blankArgument = (): ArgumentRow => ({ ...EMPTY_ARGUMENT, id: nextArgumentId++ })

/**
 * The rows of the grid, with a key for each that stays with the row while rows are put in or taken out before it,
 * and the key the next new row takes, so that no two rows ever share one.
 */
interface Grid {
    rows: TableCellValue[][]
    keys: number[]
    nextKey: number
}

/** The grid of the given rows: each keeps the key at its place among the given keys, a row past them gets a new one. */
const regrid = (rows: TableCellValue[][], keys: number[], nextKey: number): Grid => {
    const added = Math.max(0, rows.length - keys.length)
    return {
        rows,
        keys: [...keys.slice(0, rows.length), ...Array.from({ length: added }, (_, index) => nextKey + index)],
        nextKey: nextKey + added,
    }
}

// Built from frozen module constants, so the arrays are created once instead of on every render.
const SIMPLE_TYPE_OPTIONS = asOptions(SIMPLE_TYPES)
const ENVIRONMENT_KEY_OPTIONS = asOptions(ENVIRONMENT_KEYS)

export interface CreateTableModalDetail {
    projectId: string
    currentModuleName?: string
    sourceTableId?: string
    onSuccess?: (table: SummaryTable, moduleName: string) => void
}

/** Both are written with the `Datatype` keyword; what they declare is what tells a vocabulary from a datatype. */
const namesOf = (types: ProjectType[], kind: ProjectType['kind']): string[] =>
    types.filter(type => type.kind === kind).map(type => type.name)

/** What a table is built from: the fields of each datatype, and every value each vocabulary offers. */
interface TypeIndex {
    fieldsOf: FieldsOfType
    vocabularyValues: Record<string, string[]>
}

/**
 * Indexes the types by the name the rules write them with.
 *
 * <p>The server answers each name once, for the type the rules mean by it, its dependencies' types included. A name
 * it does not know — a simple type, an array, a half-typed one — has no fields, and a value of it stays one cell. The
 * first value of a vocabulary seeds an example cell, and the whole set restricts its editor.
 */
const indexTypes = (types: ProjectType[]): TypeIndex => {
    const byName = new Map(types.map(type => [type.name, type]))
    return {
        fieldsOf: typeName => byName.get(typeName.trim())?.fields ?? null,
        vocabularyValues: Object.fromEntries(types
            .filter(type => type.kind === 'vocabulary')
            .map(type => [type.name, type.values])),
    }
}

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

const normalizeArguments = (argumentsValue: ArgumentRow[]): ArgumentRow[] =>
    withTrailingBlank(argumentsValue, isCompleteArgument, blankArgument)

/** Whether every argument is named as an identifier, and no two of them share a name. */
const argumentNamesAreValid = (declaredArguments: TableArgument[]): boolean =>
    declaredArguments.every(argument => IDENTIFIER.test(argument.name.trim()))
        && new Set(declaredArguments.map(argument => argument.name.trim())).size === declaredArguments.length

/** The table as the author has written it so far, as far as what may keep it from being written. */
interface TableDraft {
    preset: TablePreset
    moduleName: string
    tableNameValid: boolean
    sheetName: string
    resultType: string
    partialArgument: boolean
    argumentNamesValid: boolean
    declaredArguments: TableArgument[]
    vocabularyType: string
    datatypeName: string
    dataFields: DatatypeField[]
    target: TargetStructure | null
    bodyValid: boolean
    submittedName: string
    typedValuesValid: boolean
}

/**
 * Why the table cannot be written yet, or `undefined` when it can.
 *
 * <p>The one list the dialog gates on: `valid` is read off it, so a rule can never disable Create without
 * naming itself, or name itself while Create stays enabled. Ordered the way the fields are laid out, so the
 * first thing the author reads is the first thing to fix.
 */
const blockingOf = (draft: TableDraft) => ([
    [!draft.moduleName, 'module'],
    [!draft.tableNameValid, 'table_name'],
    [!isValidSheetName(draft.sheetName), 'sheet'],
    [SIGNATURE_PRESETS.has(draft.preset) && !draft.resultType.trim(), 'result_type'],
    [draft.partialArgument, 'partial_argument'],
    [!draft.argumentNamesValid, 'argument_names'],
    // A lookup looks a value up by its arguments, so it takes at least one. One is enough: OpenL then reads
    // the table as rules, which is what the geometry above builds — see EPBDS-16417.
    [isLookup(draft.preset) && !draft.declaredArguments.length, 'lookup_arguments'],
    [draft.preset === 'vocabulary' && !draft.vocabularyType, 'vocabulary_type'],
    [draft.preset === 'data' && !(draft.datatypeName && draft.dataFields.length), 'datatype'],
    [isTargeted(draft.preset) && !draft.target?.columns.length, 'target'],
    [!draft.bodyValid, 'body'],
    // A Free Form table is named after its first cell, so an empty grid leaves it nameless.
    [!draft.submittedName, 'body'],
    [!draft.typedValuesValid, 'values'],
] as const).find(([blocked]) => blocked)?.[1]

const CreateTableForm: React.FC<{ detail: CreateTableModalDetail }> = ({ detail }) => {
    const { notification } = App.useApp()
    const { t } = useTranslation()
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const [modules, setModules] = useState<ModuleOption[]>([])
    // What the project holds. Each is asked for once: only the sheets belong to a module rather than to the project.
    // The types are read whole, every field and every value of each, so nothing is read again while the dialog is open.
    const [types, setTypes] = useState<ProjectType[]>([])
    const [properties, setProperties] = useState<ProjectProperty[]>([])
    // The tables a test can call. Read when a Test or a Run table is being written, and not before.
    const [executables, setExecutables] = useState<ProjectTable[]>([])
    const [loadingModules, setLoadingModules] = useState(true)
    const [loadingTargets, setLoadingTargets] = useState(false)
    const [choosingTarget, setChoosingTarget] = useState(false)
    const [creating, setCreating] = useState(false)
    const [selectedModule, setSelectedModule] = useState('')
    const [tableName, setTableName] = useState('')
    const [preset, setPreset] = useState<TablePreset>('datatype')
    const [resultType, setResultType] = useState(defaultResultType('datatype'))
    const [argumentsValue, setArgumentsValue] = useState<ArgumentRow[]>(() => [blankArgument()])
    const [vocabularyType, setVocabularyType] = useState(DEFAULT_VOCABULARY_TYPE)
    const [extendsType, setExtendsType] = useState('')
    const [datatypeName, setDatatypeName] = useState('')
    const [target, setTarget] = useState<TargetStructure | null>(null)
    const [transposed, setTransposed] = useState(false)
    const [grid, setGrid] = useState<Grid>(() => regrid(normalizeRows([], 5), [], 0))
    const { rows, keys: rowKeys } = grid
    /**
     * Replaces the rows, each keeping the key at its place. A row put in or taken out before others is not one of
     * these changes: it goes through {@link insertRow} and {@link removeRow}, which move the keys along with it.
     */
    const setRows = useCallback((next: SetStateAction<TableCellValue[][]>) => setGrid(current =>
        regrid(typeof next === 'function' ? next(current.rows) : next, current.keys, current.nextKey)), [])
    const sheetLoader = useSheetLoader(t('project:create_table_modal.options_load_failed'))
    const { sheets, sheetName, setSheetName } = sheetLoader
    // Identifies the latest choice of a table to exercise: the list of callable tables asked for by a type the
    // author has left, or by a default the author has overridden, must not still choose a table.
    const targetToken = useRef(0)
    // The request for the callable tables rather than its answer, so two quick switches between Test and Run, or a
    // switch and an opened list, share the one that is already on its way.
    const executablesRequest = useRef<Promise<ProjectTable[]> | null>(null)
    // The module read still on its way. Choosing a Test table names the sheet after the table it exercises while the
    // module's own worksheets are being read, and both write the sheet field: this is what orders the two.
    const moduleRead = useRef<Promise<void>>(Promise.resolve())

    const close = useCallback(() => {
        globalThis.dispatchEvent(new CustomEvent('openCreateTableModal', { detail: null }))
    }, [])

    const typeIndex = useMemo(() => indexTypes(types), [types])

    const context = useMemo<TableBuildContext>(() => ({
        resultType,
        resultFields: typeIndex.fieldsOf(resultType) ?? [],
        arguments: argumentsValue,
        vocabularyType,
        extendsType,
        datatypeName,
        dataFields: typeIndex.fieldsOf(datatypeName) ?? [],
        targetName: target?.table.name ?? '',
        targetColumns: target?.columns ?? [],
        vocabularyValues: typeIndex.vocabularyValues,
    }), [argumentsValue, datatypeName, extendsType, resultType, target, typeIndex, vocabularyType])

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
            options: asOptions(namesOf(types, 'vocabulary')),
        },
        {
            label: t('project:create_table_modal.type_groups.datatypes'),
            options: asOptions([...namesOf(types, 'datatype'), ...complexTypes]),
        },
    ].filter(group => group.options.length), [types, t])

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
        const grows = columnsGrow(forPreset, nextContext)
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

    /** `index` is the dialog's own, unless the types have just arrived. */
    const resetRowsForPreset = (nextPreset: TablePreset, nextDatatypeName: string, index = typeIndex) => {
        const nextContext: TableBuildContext = {
            resultType: defaultResultType(nextPreset),
            resultFields: [],
            arguments: [{ ...EMPTY_ARGUMENT }],
            vocabularyType: DEFAULT_VOCABULARY_TYPE,
            extendsType: '',
            datatypeName: nextDatatypeName,
            dataFields: index.fieldsOf(nextDatatypeName) ?? [],
            targetName: '',
            targetColumns: [],
            vocabularyValues: index.vocabularyValues,
        }
        resizeRows(nextPreset, nextContext, initialRows(nextPreset, nextContext))
    }

    /**
     * Builds the Test or Run table for a tested table and puts it in the grid.
     *
     * <p>The columns come from the signature the tested table declares, an argument of a datatype opened into the
     * fields the dialog already holds, so nothing is read from the server. `index` is the dialog's own, unless the
     * types have just arrived.
     */
    const buildTarget = (
        table: ProjectTable,
        nextPreset: 'test' | 'run',
        updateTableName = true,
        index = typeIndex
    ) => {
        const structure = buildTargetStructure(table, nextPreset, index.fieldsOf)
        setTarget(structure)
        const nextContext: TableBuildContext = {
            ...context,
            targetName: structure.table.name,
            targetColumns: structure.columns,
            vocabularyValues: index.vocabularyValues,
        }
        setRows(measureRows(nextPreset, nextContext, initialRows(nextPreset, nextContext)))
        if (updateTableName) {
            // A generated table is named after the table it exercises; the author is free to rename it.
            const generated = targetTableName(table.name, nextPreset)
            setTableName(generated)
            // The destination module is read at the same time and points the sheet field at one of its worksheets.
            // The tested table has the last word, so it waits for that answer instead of racing it — otherwise the
            // sheet the table is written to is whichever request the server happened to answer first.
            void moduleRead.current.then(() => setSheetName(sheetNameFrom(table.name || generated)))
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
     * <p>The table already chosen survives a switch between Test and Run, so that switch asks the server for
     * nothing. Without one there is nothing to build a skeleton from: the project's callable tables are read, and the
     * first of them is taken.
     */
    const selectTargetTable = async (nextPreset: 'test' | 'run', kept: TargetStructure | null, token: number) => {
        if (kept) {
            // The name follows the tested table only when that table changes; a name the author already has stays.
            buildTarget(kept.table, nextPreset, false)
            return
        }
        setChoosingTarget(true)
        try {
            const first = (await loadExecutables(detail.projectId))
                .find(table => canTargetTable(table, nextPreset))
            if (token === targetToken.current && first) {
                buildTarget(first, nextPreset, true)
            }
        } finally {
            // A project offering no callable table leaves the spinner to this branch: nothing else clears it.
            if (token === targetToken.current) {
                setChoosingTarget(false)
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
            // As the compiler resolved them, so a type of a project this one depends on is offered and opened up too.
            getProjectTypes(detail.projectId),
            // The ones a Properties table's contents may name: the only table written here that carries any.
            getProjectProperties(detail.projectId, 'Properties'),
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
                setTypes(loadedTypes)
                const loadedIndex = indexTypes(loadedTypes)
                setProperties(loadedProperties)
                const firstDatatype = namesOf(loadedTypes, 'datatype')[0] ?? ''
                setDatatypeName(firstDatatype)
                const sourceTable = callable.find(table => table.id === detail.sourceTableId)
                if (sourceTable) {
                    setPreset('test')
                    buildTarget(sourceTable, 'test', true, loadedIndex)
                } else {
                    resetRowsForPreset('datatype', firstDatatype, loadedIndex)
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
                setTypes([])
                setProperties([])
                setSelectedModule('')
                resetRowsForPreset('datatype', '')
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
    const argumentNamesValid = argumentNamesAreValid(declaredArguments)
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
        if (columnsGrow(preset, context)) {
            return dropEmptyTrailingColumns(dropEmptyRows(rows))
        }
        // A free-text type can temporarily narrow the rendered columns without resizing the rows. Only cells the
        // current table shape exposes belong to the submitted table; stale hidden cells must not reach the workbook.
        return dropEmptyRows(rows.map(row => row.slice(0, columns.length)))
    }, [columns.length, context, preset, rows])
    // The name OpenL will compile, read from the generated header the same way the compiler reads it. A Free Form
    // table has no header of its own, so OpenL names it after the first cell instead.
    const submittedName = deriveTableName(preset,
        hasTableHeader(preset) ? generatedHeader : String(submittedBody[0]?.[0] ?? ''))


    const blocking = blockingOf({
        preset,
        moduleName,
        tableNameValid,
        sheetName,
        resultType,
        partialArgument,
        argumentNamesValid,
        declaredArguments,
        vocabularyType,
        datatypeName,
        dataFields: context.dataFields,
        target,
        bodyValid,
        submittedName,
        typedValuesValid,
    })
    const valid = !blocking
    const blockingReason = blocking && t(`project:create_table_modal.blocked.${blocking}`)

    const moduleOptions = useMemo(() => toSortedOptions(modules), [modules])

    // The option arrays below are memoized so a keystroke anywhere in the modal does not hand every Select a new
    // array identity, which makes rc-select re-flatten and re-filter a project-wide list on each render.
    const presetOptions = useMemo(() => TABLE_PRESETS.map(value => ({
        label: t(`project:create_table_modal.types.${value}`),
        value,
    })), [t])

    const sheetOptions = useMemo(() => asOptions(sheets), [sheets])

    const datatypeOptions = useMemo(() => asOptions(namesOf(types, 'datatype')), [types])

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
        // The table being chosen for the type being left no longer clears the spinner, so it is cleared here.
        setChoosingTarget(false)
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
        resetRowsForPreset(value, datatypeName)
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
        const nextContext = { ...context, arguments: nextArguments }
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
            resizeRows(preset, nextContext, initialRows(preset, nextContext))
            return
        }
        const typesChanged = completeNextArguments.some((argument, index) =>
            argument.type.trim() !== currentArguments[index]?.type.trim())
        // When only a type changed, the columns stay in place, and each cell takes the editor of its new type. The
        // values already entered are kept; invalid ones remain visible as empty selections and keep Create disabled
        // until the author replaces them.
        if (!typesChanged) {
            resizeRows(preset, nextContext)
        }
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
            || typeIndex.fieldsOf(declared) !== null
    }

    const handleResultTypeChange = (value: string) => {
        setResultType(value)
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
        resizeRows(preset, { ...context, resultType: declared, resultFields: typeIndex.fieldsOf(declared) ?? []})
    }

    /**
     * Replaces the signature and resizes the grid to the columns it now describes.
     *
     * <p>The new list is computed before the state is set, not inside the updater: React may run an updater more
     * than once, and the resize is a side effect that must happen exactly once per change.
     */
    const changeArguments = (transform: (current: ArgumentRow[]) => ArgumentRow[]) => {
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
    }

    const insertRow = (index: number) => {
        setGrid(current => regrid(
            normalizeEditorRows(insertAt(
                current.rows,
                index,
                new Array<TableCellValue>(Math.max(1, columns.length)).fill('')
            )),
            insertAt(current.keys, index, current.nextKey),
            current.nextKey + 1
        ))
    }

    const removeRow = (index: number) => {
        setGrid(current => regrid(
            normalizeEditorRows(deleteAt(current.rows, index)),
            deleteAt(current.keys, index),
            current.nextKey
        ))
    }

    const insertColumn = (index: number) => {
        setRows(current => normalizeEditorRows(current.map(row => insertAt(row, index, ''))))
    }

    const removeColumn = (index: number) => {
        setRows(current => normalizeEditorRows(current.map(row => deleteAt(row, index))))
    }

    const handleDatatypeChange = (value: string) => {
        setDatatypeName(value)
        resizeRows('data', { ...context, datatypeName: value, dataFields: typeIndex.fieldsOf(value) ?? []}, [])
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
            buildTarget(table, preset)
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

    // Display names under their groups, the way Table Details lists them; the cell keeps the technical name, which
    // is what a Properties table declares.
    const propertyOptions = useMemo(() => toPropertyGroups(properties), [properties])

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
            {/* The band titles the leading columns, one title over each. */}
            {bandIndex === 0
                ? columns.slice(0, band.keys).map(column => (
                    <th key={column.key} rowSpan={band.rows}>{column.label}</th>
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
        <tr key={rowKeys[rowIndex]}>
            <td className={styles.gutter}>{rowIndex + 1}</td>
            {columns.map((column, columnIndex) => (
                <td
                    key={column.key}
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
                            <th key={rowKeys[recordIndex]}>
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
                                    key={rowKeys[rowIndex]}
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

    /** Writes the table; a new module is written in the format given, a module the project has in its own. */
    const handleCreate = async (format: ModuleFormat = 'excel') => {
        if (!valid || creating) {
            return
        }
        setCreating(true)
        try {
            const source = buildTableSource(preset, generatedHeader, context, submittedBody, transposed)
            const regions = buildTableRegions(preset, context, source, transposed)
            const table = await createTable(detail.projectId, {
                moduleName,
                sheetName: sheetName.trim(),
                ...(isNewModule ? { modulePath: defaultModulePath(moduleName, tableKind(preset), format) } : {}),
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
                ...(regions.length ? { regions } : {}),
            })
            if (table) {
                close()
                detail.onSuccess?.(table, moduleName)
            }
        } finally {
            setCreating(false)
        }
    }

    const busy = loadingModules || choosingTarget || sheetLoader.loading

    const renderFooter: ModalProps['footer'] = (_, { CancelBtn }) => (
        <>
            <CancelBtn />
            <WriteButton
                destination={destinationOf(modules, moduleName)}
                disabled={busy || !valid}
                label={t('project:create_table_modal.create')}
                loading={creating}
                markdownLabel={t('project:create_table_modal.create_markdown')}
                onWrite={format => void handleCreate(format)}
                testId="create-table"
            />
        </>
    )

    return (
        <Modal
            destroyOnHidden
            open
            cancelButtonProps={{ disabled: creating }}
            cancelText={t('common:btn.cancel')}
            closable={!creating}
            footer={renderFooter}
            keyboard={!creating}
            mask={{ closable: false }}
            onCancel={close}
            width={1200}
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
                                                key={argument.id}
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
                    {!busy && blockingReason && (
                        <Alert
                            showIcon
                            data-testid="create-table-blocked"
                            title={blockingReason}
                            type="info"
                        />
                    )}
                </div>
            </Spin>
        </Modal>
    )
}

/**
 * Writes a new table into a module of the project, building its skeleton from the table type the author chooses.
 *
 * <p>The form is mounted only while the dialog is open, so closing it discards every field, the project's types and
 * its callable-table list rather than holding them until the page is reloaded.
 */
export const CreateTableModal: React.FC = () => {
    const { detail } = useGlobalEvents<CreateTableModalDetail>('openCreateTableModal')
    return detail
        ? <CreateTableForm key={`${detail.projectId} ${detail.sourceTableId ?? ''}`} detail={detail} />
        : null
}
