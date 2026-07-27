import { notification } from 'antd'
import i18n from '../i18n'
import type { CreateTableRequest, ProjectDatatype, ProjectTable, SummaryTable } from 'types/tables'
import { errorMessage } from 'utils/errorMessage'
import apiCall, { asArray } from './apiCall'
import { toUrlSafeId } from './projectId'

const TABLE_API_OPTIONS = { throwError: true, suppressErrorPages: true }

/**
 * Tables of the given kinds, from anywhere in the project.
 *
 * <p>The whole list at once: it names a table to point at, so paging it would hide the table being looked for.
 */
export const getProjectTables = async (projectId: string, kinds: string[]): Promise<ProjectTable[]> => {
    const filter = kinds.map(kind => `kind=${encodeURIComponent(kind)}`).join('&')
    const page = await apiCall(
        `/projects/${toUrlSafeId(projectId)}/tables?${filter}&unpaged=true`,
        undefined,
        TABLE_API_OPTIONS
    ) as { content?: ProjectTable[] } | null
    return asArray(page?.content)
}

/**
 * One Datatype table: the fields it declares, the datatype it extends, and — for a vocabulary, written with the
 * same keyword — the values it accepts.
 *
 * <p>The tables list carries none of them, so a type a table is about to be built from is read in full when it is
 * picked.
 */
export const getDatatype = async (projectId: string, tableId: string): Promise<ProjectDatatype> => {
    const table = await apiCall(
        `/projects/${toUrlSafeId(projectId)}/tables/${encodeURIComponent(tableId)}`,
        undefined,
        TABLE_API_OPTIONS
    ) as {
        extends?: string
        fields?: { name?: string, type?: string }[]
        values?: { value?: unknown }[]
    } | null
    return {
        ...(table?.extends ? { extends: table.extends } : {}),
        fields: asArray<{ name?: string, type?: string }>(table?.fields)
            .filter(field => field.name)
            .map(field => ({ name: field.name ?? '', type: field.type ?? '' })),
        values: asArray<{ value?: unknown }>(table?.values)
            .map(entry => entry.value)
            .filter(value => value !== null && value !== undefined)
            .map(String),
    }
}

export const createTable = async (
    projectId: string,
    request: CreateTableRequest
): Promise<SummaryTable | null> => {
    try {
        const table = await apiCall(
            `/projects/${toUrlSafeId(projectId)}/tables`,
            {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(request),
            },
            TABLE_API_OPTIONS
        ) as SummaryTable | null
        // An empty 201 body means the compiled table could not be found again; apiCall turns that into the `true`
        // sentinel, so a plain falsy check would let it through as a success.
        if (!table || typeof table !== 'object') {
            throw new Error(i18n.t('project:create_table_modal.created_table_not_found'))
        }
        notification.success({
            title: i18n.t('project:create_table_modal.created'),
            description: i18n.t('project:create_table_modal.created_description', { table: table.name }),
        })
        return table
    } catch (error) {
        notification.error({
            title: i18n.t('project:create_table_modal.create_failed'),
            description: errorMessage(error),
        })
        return null
    }
}
