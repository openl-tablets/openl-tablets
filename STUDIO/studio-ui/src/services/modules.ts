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
export const startModuleCompilation = async (projectId: string, moduleName: string): Promise<void> => {
    await apiCall(`${moduleUrl(projectId, moduleName)}/compile`, { method: 'POST' }, MODULE_API_OPTIONS)
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
 * One table as the workbook holds it — the cells with their spans and their Excel styling.
 *
 * This is the picture the editor shows. It is read per table, not with the list: a module can hold hundreds of
 * tables and only the one being looked at has to be drawn.
 */
export const getRawTable = async (projectId: string, tableId: string): Promise<RawTableView> =>
    await apiCall(
        `/projects/${toUrlSafeId(projectId)}/tables/${encodeURIComponent(tableId)}?raw=true&styles=true`,
        undefined,
        MODULE_API_OPTIONS
    ) as RawTableView
