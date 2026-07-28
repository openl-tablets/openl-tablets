import { notification } from 'antd'
import i18n from '../i18n'
import type {
    CreateTableRequest,
    ProjectDatatype,
    ProjectTable,
    RawTable,
    SummaryTable,
} from 'types/tables'
import { errorMessage } from 'utils/errorMessage'
import apiCall, { asArray } from './apiCall'
import { toUrlSafeId } from './projectId'

const TABLE_API_OPTIONS = { throwError: true, suppressErrorPages: true }

interface TableWriteMessages {
    successTitle: string
    successDescription: (tableName: string) => string
    failureTitle: string
    missingTable: string
}

const writeTable = async (
    projectId: string,
    request: CreateTableRequest,
    messages: TableWriteMessages
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
            throw new Error(messages.missingTable)
        }
        notification.success({
            title: messages.successTitle,
            description: messages.successDescription(table.name),
        })
        return table
    } catch (error) {
        notification.error({
            title: messages.failureTitle,
            description: errorMessage(error),
        })
        return null
    }
}

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
): Promise<SummaryTable | null> => writeTable(projectId, request, {
    successTitle: i18n.t('project:create_table_modal.created'),
    successDescription: table => i18n.t('project:create_table_modal.created_description', { table }),
    failureTitle: i18n.t('project:create_table_modal.create_failed'),
    missingTable: i18n.t('project:create_table_modal.created_table_not_found'),
})

/** Read the exact cell matrix that the browser uses to create a copy. */
export const getTableRaw = async (
    projectId: string,
    tableId: string
): Promise<RawTable> => apiCall(
    `/projects/${toUrlSafeId(projectId)}/tables/${encodeURIComponent(tableId)}?raw=true`,
    undefined,
    TABLE_API_OPTIONS
) as Promise<RawTable>

/** Write a browser-built copy through the ordinary Create Table endpoint. */
export const createTableCopy = async (
    projectId: string,
    request: CreateTableRequest
): Promise<SummaryTable | null> => writeTable(projectId, request, {
    successTitle: i18n.t('project:copy_table_modal.copied'),
    successDescription: table => i18n.t('project:copy_table_modal.copied_description', { table }),
    failureTitle: i18n.t('project:copy_table_modal.copy_failed'),
    missingTable: i18n.t('project:copy_table_modal.copied_table_not_found'),
})
