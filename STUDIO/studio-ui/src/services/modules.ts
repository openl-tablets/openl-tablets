import type { ModuleTable, RawTableView } from 'types/tables'
import apiCall, { asArray } from './apiCall'
import { toUrlSafeId } from './projectId'

const MODULE_API_OPTIONS = { throwError: true, suppressErrorPages: true }

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
        MODULE_API_OPTIONS
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
    await apiCall(`${moduleUrl(projectId, moduleName)}/compile${query}`, { method: 'POST' }, MODULE_API_OPTIONS)
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
        MODULE_API_OPTIONS
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
        MODULE_API_OPTIONS
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
    options: { module?: string, startRow?: number, maxRows?: number } = {}
): Promise<RawTableView> => {
    const params = new URLSearchParams({ raw: 'true', styles: 'true' })
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
        MODULE_API_OPTIONS
    ) as RawTableView
}

/** A test or run table that exercises another table. */
export interface TableTest {
    id: string
    name: string
    /** What the test holds, as the Editor phrases it — "1 test case"; absent for a run table. */
    info?: string
}

/**
 * The tests and runs that exercise the given table.
 *
 * Each carries the id the Tables API addresses it by, so the editor opens one as it opens any other table.
 */
export const getTableTests = async (projectId: string, tableId: string, module?: string): Promise<TableTest[]> =>
    asArray(await apiCall(
        `/projects/${toUrlSafeId(projectId)}/tables/${encodeURIComponent(tableId)}/tests`
        + (module === undefined ? '' : `?module=${encodeURIComponent(module)}`),
        undefined,
        MODULE_API_OPTIONS
    ) as TableTest[] | null)
