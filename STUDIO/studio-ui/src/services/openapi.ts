import apiCall from './apiCall'
import { toUrlSafeId } from './projectId'

/** Where a project's specification was written, and whether the file was added or written over. */
interface OpenApiSchema {
    path: string
    created: boolean
}

/** One of the two modules a generation writes, as it will stand once the generation has run. */
export interface OpenApiModule {
    name: string
    /** The workbook the module reads, relative to the project. */
    path: string
    /**
     * Whether the project already reads a module under this name. Such a module is written where it reads,
     * so the workbook it is given is not the reader's to choose.
     */
    declared: boolean
    /** Whether a file stands at that workbook today, which the generation writes over. */
    overwrites: boolean
}

/** What generating tables from a specification would write, before it writes it. */
export interface OpenApiGenerationPlan {
    algorithm: OpenApiModule
    model: OpenApiModule
}

/** What to generate the tables from, and where to put them. */
export interface OpenApiGeneration {
    path: string
    algorithmModuleName: string
    algorithmModulePath: string
    modelModuleName: string
    modelModulePath: string
}

/** Where the generation writes its two modules, once the reader has settled it. */
export type OpenApiTargets = Pick<OpenApiGeneration, 'algorithmModulePath' | 'modelModulePath'>

const openApiUrl = (projectId: string, what: string) => `/projects/${toUrlSafeId(projectId)}/openapi/${what}`

/**
 * Writes the project's own specification, generated from the rules it compiled.
 *
 * <p>The whole project is compiled first, so this answers in as long as the compilation takes.
 */
export async function writeOpenApiSchema(projectId: string): Promise<OpenApiSchema> {
    return await apiCall(openApiUrl(projectId, 'schema'), { method: 'POST' }, { throwError: true }) as OpenApiSchema
}

/**
 * Reads which two modules a generation would write, so the reader sees what it replaces before it runs.
 *
 * <p>A module name left out is answered with the configured default.
 */
export async function getOpenApiGenerationPlan(
    projectId: string,
    algorithmModuleName?: string,
    modelModuleName?: string
): Promise<OpenApiGenerationPlan> {
    const asked = new URLSearchParams()
    if (algorithmModuleName) {
        asked.set('algorithmModuleName', algorithmModuleName)
    }
    if (modelModuleName) {
        asked.set('modelModuleName', modelModuleName)
    }
    const query = asked.size > 0 ? `?${asked.toString()}` : ''
    return await apiCall(
        `${openApiUrl(projectId, 'generation')}${query}`,
        undefined,
        { throwError: true }
    ) as OpenApiGenerationPlan
}

/** Writes the project's tables from the specification it names. */
export async function generateOpenApiTables(projectId: string, asked: OpenApiGeneration): Promise<void> {
    await apiCall(
        openApiUrl(projectId, 'generation'),
        { method: 'POST', body: JSON.stringify(asked), headers: { 'Content-Type': 'application/json' } },
        { throwError: true }
    )
}
