import type { ModuleTable, RawTableView } from 'types/tables'
import apiCall, { asArray, LOCAL_LOAD_API_OPTIONS } from './apiCall'
import { toUrlSafeId } from './projectId'

const moduleUrl = (projectId: string, moduleName: string): string =>
    `/projects/${toUrlSafeId(projectId)}/modules/${encodeURIComponent(moduleName)}`

/** A module of the project, as the project resolves it: the name it is known by and the workbook it is written in. */
export interface ModuleInfo {
    name: string
    path?: string
}

/**
 * The modules the project resolves.
 *
 * Read instead of the descriptor because a project whose modules are discovered by pattern declares none of them
 * by name, and the editor still has to name the workbook a module is written in.
 */
export const listModules = async (projectId: string): Promise<ModuleInfo[]> =>
    asArray(await apiCall(
        `/projects/${toUrlSafeId(projectId)}/modules`,
        undefined,
        LOCAL_LOAD_API_OPTIONS
    ) as ModuleInfo[] | null)

/**
 * Asks for a module to be compiled and returns as soon as the request is taken.
 *
 * Compiling a module takes as long as it takes — minutes, on a large project — so nothing is waited for here. How
 * far the compilation has come arrives on the project's status channel, which names each module as it finishes.
 */
export const startModuleCompilation = async (
    projectId: string,
    moduleName: string,
    reset = false
): Promise<void> => {
    const query = reset ? '?reset=true' : ''
    await apiCall(`${moduleUrl(projectId, moduleName)}/compile${query}`, { method: 'POST' }, LOCAL_LOAD_API_OPTIONS)
}

/**
 * The tables of one module, the whole list at once.
 *
 * The answer is ready once this module is compiled and does not wait for the modules that follow it. Called before
 * that, it waits — which is why the editor asks only after the status channel has named the module as compiled.
 */
export const getModuleTables = async (projectId: string, moduleName: string): Promise<ModuleTable[]> => {
    const page = await apiCall(
        `/projects/${toUrlSafeId(projectId)}/tables?module=${encodeURIComponent(moduleName)}&unpaged=true`,
        undefined,
        LOCAL_LOAD_API_OPTIONS
    ) as { content?: ModuleTable[] } | null
    return asArray(page?.content)
}

/**
 * Tells the compilation of a module to stop.
 *
 * <p>Answers at once: the module being compiled at that moment is finished and nothing after it is started.
 * What was compiled stays readable, and a refresh compiles the module again from the workbook.
 */
export const cancelModuleCompilation = async (projectId: string, moduleName: string): Promise<void> => {
    await apiCall(
        `${moduleUrl(projectId, moduleName)}/compile`,
        { method: 'DELETE' },
        LOCAL_LOAD_API_OPTIONS
    )
}

/** How many rows of a table are drawn at once; the rest are fetched as the reader asks for them. */
export const TABLE_PAGE_ROWS = 120

/**
 * One table as the workbook holds it — the cells with their spans and their Excel styling.
 *
 * This is the picture the editor shows. It is read per table, not with the list: a module can hold hundreds of
 * tables and only the one being looked at has to be drawn. A tall table arrives a window at a time, and says in
 * `totalRows` how many it has in all.
 *
 * Asked with `metaInfo`, every cell also carries what the compiler knows about it: the pieces of its text that
 * refer to something — each with the table and module they lead to — the type it holds, whether a decision table
 * returns it, and the editor it asks for.
 *
 * Every cell carries both what it computed and the formula it was written with, and the table says how many
 * rows its header takes — so showing formulas or hiding the header is the screen's own choice, made without
 * asking again.
 *
 * Naming the module lets the read answer as soon as that module is compiled, without waiting for the rest of the
 * project.
 */
export const getRawTable = async (
    projectId: string,
    tableId: string,
    options: {
        module?: string
        startRow?: number
        maxRows?: number
        metaInfo?: boolean
        /** Ask what the table is as something to run: the editor offers Run and Trace on the answer. */
        runState?: boolean
    } = {}
): Promise<RawTableView> => {
    const params = new URLSearchParams({ raw: 'true', styles: 'true' })
    if (options.metaInfo) {
        params.set('metaInfo', 'true')
    }
    if (options.runState) {
        params.set('runState', 'true')
    }
    if (options.module !== undefined) {
        params.set('module', options.module)
    }
    if (options.startRow !== undefined) {
        params.set('startRow', String(options.startRow))
    }
    if (options.maxRows !== undefined) {
        params.set('maxRows', String(options.maxRows))
    }
    return await apiCall(
        `/projects/${toUrlSafeId(projectId)}/tables/${encodeURIComponent(tableId)}?${params}`,
        undefined,
        LOCAL_LOAD_API_OPTIONS
    ) as RawTableView
}

/** One way of entering a value, with everything that way of entering it needs. */
export interface TableCellEditor {
    /** 'combo', 'multiselect', 'numeric', 'array', 'range', 'date' or 'boolean' */
    editor: string
    /** Values to choose from, for 'combo' and 'multiselect' */
    choices?: string[]
    /** What to show for each choice, in the order of `choices` */
    displayValues?: string[]
    /** What separates the chosen values in the cell, for 'multiselect' and 'array' */
    separator?: string
    /** What precedes a separator that belongs to a value, for 'multiselect' */
    separatorEscaper?: string
    /** Smallest value the cell's type holds, for 'numeric' */
    min?: number
    /** Largest value the cell's type holds, for 'numeric' */
    max?: number
    /** True when only whole numbers are accepted, for 'numeric' and 'array' */
    intOnly?: boolean
    /** The editor one entry is written with, for 'array' and 'range' */
    entryEditor?: string
}

/** How the cells of a table take a value: the ways of entering one, and which cell asks for which. */
export interface TableEditors {
    editors: TableCellEditor[]
    cells: Array<{ row: number, column: number, editor: number }>
}

/**
 * How the cells of a window of a table take a value.
 *
 * <p>Read once, when the reader starts editing the table, and for the same window the table itself was read as —
 * so a cell is pointed at by the same row and column in both, and nothing is asked while the reader edits.
 *
 * <p>A cell the answer does not name is written as plain text.
 */
export const getTableEditors = async (
    projectId: string,
    tableId: string,
    options: { module?: string | undefined, startRow?: number | undefined, maxRows?: number | undefined } = {}
): Promise<TableEditors> => {
    const params = new URLSearchParams()
    if (options.module !== undefined) {
        params.set('module', options.module)
    }
    if (options.startRow !== undefined) {
        params.set('startRow', String(options.startRow))
    }
    if (options.maxRows !== undefined) {
        params.set('maxRows', String(options.maxRows))
    }
    const query = params.size > 0 ? `?${params}` : ''
    return await apiCall(
        `/projects/${toUrlSafeId(projectId)}/tables/${encodeURIComponent(tableId)}/editors${query}`,
        undefined,
        LOCAL_LOAD_API_OPTIONS
    ) as TableEditors
}

/**
 * The stack trace behind one compilation message, read when a reader opens it.
 *
 * A trace runs to thousands of characters and most messages are read without one, so the messages carry only
 * whether there is a trace, and the trace itself is asked for one message at a time.
 */
export const getMessageStacktrace = async (
    projectId: string,
    messageId: number,
    moduleName?: string
): Promise<string> => {
    const params = new URLSearchParams()
    if (moduleName !== undefined) {
        params.set('module', moduleName)
    }
    const query = params.size > 0 ? `?${params}` : ''
    return await apiCall(
        `/projects/${toUrlSafeId(projectId)}/messages/${messageId}/stacktrace${query}`,
        undefined,
        LOCAL_LOAD_API_OPTIONS
    ) as string
}

/** A test or run table that exercises another table. */
export interface TableTest {
    id: string
    name: string
    /** What the test holds, as the Editor phrases it — "1 test case"; absent for a run table. */
    info?: string
    /** Module the test is written in, which need not be the one it exercises. */
    module?: string
    /** Name of the project that module belongs to. */
    project?: string
    /** Identifier of that project; absent when the session cannot address it, and then the test cannot be opened. */
    projectId?: string
}

/**
 * The tests and runs that exercise the given table.
 *
 * Each carries the id the Tables API addresses it by and where it is written — a test is a table of its own,
 * and its author may have put it in another module, or in a project this one depends on.
 */
export const getTableTests = async (projectId: string, tableId: string, module?: string): Promise<TableTest[]> =>
    asArray(await apiCall(
        `/projects/${toUrlSafeId(projectId)}/tables/${encodeURIComponent(tableId)}/tests`
        + (module === undefined ? '' : `?module=${encodeURIComponent(module)}`),
        undefined,
        LOCAL_LOAD_API_OPTIONS
    ) as TableTest[] | null)

/** A table that a test or a run table exercises. */
export interface TableTarget {
    id: string
    /** Name it is known by, with the dimension properties that tell this version of it from the others. */
    name: string
    /** Module the tested table is written in, which need not be the one the test is in. */
    module?: string
    /** Name of the project that module belongs to. */
    project?: string
    /** Identifier of that project; absent when the session cannot address it, and then it cannot be opened. */
    projectId?: string
}

/**
 * The tables the given test or run table exercises.
 *
 * Each says where it is written, because a test may be written against a table of another module or of a project
 * this one depends on. A table of any other kind exercises nothing and answers with an empty list.
 */
export const getTableTargets = async (
    projectId: string,
    tableId: string,
    module?: string
): Promise<TableTarget[]> =>
    asArray(await apiCall(
        `/projects/${toUrlSafeId(projectId)}/tables/${encodeURIComponent(tableId)}/targets`
        + (module === undefined ? '' : `?module=${encodeURIComponent(module)}`),
        undefined,
        LOCAL_LOAD_API_OPTIONS
    ) as TableTarget[] | null)

/** How wide a search reaches, as the Tables API names it. */
export type TableSearchScope = 'module' | 'project' | 'all'

/** What an extended search asks for. Everything is optional: a search asking nothing lists the tables. */
export interface TableSearchCriteria {
    /** The module the search is made through, and the one it covers unless the scope says wider. */
    module?: string | undefined
    scope?: TableSearchScope | undefined
    /** Part of the table's name. */
    name?: string | undefined
    /** Part of the table's header line — its keyword, what it returns, the arguments it takes. */
    header?: string | undefined
    /** Text written in any cell of the table. */
    text?: string | undefined
    /** The families of table the search covers; empty means every family. */
    kinds?: string[]
    /** Values the table's properties must carry, by property name. */
    properties?: Record<string, string>
}

/**
 * The tables of the project that answer the search.
 *
 * A search wider than one module waits for the project to be compiled through, and each table it answers with
 * says which module and project it lives in — so a result can be opened where it is written.
 */
export const searchTables = async (projectId: string, criteria: TableSearchCriteria): Promise<ModuleTable[]> => {
    const params = new URLSearchParams({ unpaged: 'true' })
    const add = (key: string, value?: string) => {
        if (value) {
            params.set(key, value)
        }
    }
    add('module', criteria.module)
    add('scope', criteria.scope)
    add('name', criteria.name)
    add('header', criteria.header)
    add('text', criteria.text)
    for (const kind of criteria.kinds ?? []) {
        params.append('kind', kind)
    }
    for (const [name, value] of Object.entries(criteria.properties ?? {})) {
        add(`properties.${name}`, value)
    }
    const page = await apiCall(
        `/projects/${toUrlSafeId(projectId)}/tables?${params}`,
        undefined,
        LOCAL_LOAD_API_OPTIONS
    ) as { content?: ModuleTable[] } | null
    return asArray(page?.content)
}

/** Where a property that applies to a table is defined, when it is not written on the table itself. */
export type PropertyInheritance = 'category' | 'module' | 'external'

/** One property that applies to a table, as the details panel lists it. */
export interface TablePropertyDetail {
    name: string
    displayName: string
    value: string
    /** Absent when the table declares the property itself. */
    inheritedFrom?: PropertyInheritance
    /** The properties table the value comes from, so the reader can open it. */
    inheritedTableId?: string
}

/** One group of table properties, named as the property dictionary names it. */
export interface TablePropertyGroup {
    name: string
    properties: TablePropertyDetail[]
}

/** What a table says about itself besides its cells, and what may still be written on it. */
export interface TableDetails {
    name: string
    groups: TablePropertyGroup[]
    /** Whether this kind of table carries properties at all; one that does not is never edited here. */
    canEditProperties: boolean
    /** Names of the properties the table may still be given — what it already shows is changed where it stands. */
    available: string[]
}

/**
 * The name of a table and every property that applies to it.
 *
 * The list holds what the table declares together with what it inherits from the properties table of its module
 * or its category, each saying where it came from — which is what a reader needs when the table's header is
 * hidden and the values are nowhere on screen.
 */
export const getTableDetails = async (
    projectId: string,
    tableId: string,
    module?: string
): Promise<TableDetails> => {
    const read = await apiCall(
        `/projects/${toUrlSafeId(projectId)}/tables/${encodeURIComponent(tableId)}/details`
        + (module === undefined ? '' : `?module=${encodeURIComponent(module)}`),
        undefined,
        LOCAL_LOAD_API_OPTIONS
    ) as TableDetails | null
    // A table with nothing to say about itself answers without the list at all.
    return {
        name: read?.name ?? '',
        groups: asArray(read?.groups),
        canEditProperties: read?.canEditProperties ?? false,
        available: asArray(read?.available),
    }
}
