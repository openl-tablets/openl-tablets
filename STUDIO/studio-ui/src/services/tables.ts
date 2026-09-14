import { notification } from 'antd'
import i18n from '../i18n'
import type {
    CopyTableRequest,
    CreateTableRequest,
    ProjectDatatype,
    ProjectTable,
    SummaryTable,
    TableCopyInfo,
    TableInput,
    TableInputCasesPage,
    TableInputTestCase,
    TableEdit,
    TableProperty,
} from 'types/tables'
import { errorMessage } from 'utils/errorMessage'
import apiCall, { asArray, LOCAL_LOAD_API_OPTIONS, notifyLoadFailure } from './apiCall'
import { toUrlSafeId } from './projectId'

/** How many cases of a test table a page carries, as the API pages them. */
export const TEST_CASES_PAGE_SIZE = 25

interface TableWriteMessages {
    successTitle: string
    successDescription: (tableName: string) => string
    failureTitle: string
    missingTable: string
}

const writeTable = async <T>(
    url: string,
    request: T,
    messages: TableWriteMessages
): Promise<SummaryTable | null> => {
    try {
        const table = await apiCall(
            url,
            {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(request),
            },
            LOCAL_LOAD_API_OPTIONS
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
        LOCAL_LOAD_API_OPTIONS
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
        LOCAL_LOAD_API_OPTIONS
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
): Promise<SummaryTable | null> => writeTable(`/projects/${toUrlSafeId(projectId)}/tables`, request, {
    successTitle: i18n.t('project:create_table_modal.created'),
    successDescription: table => i18n.t('project:create_table_modal.created_description', { table }),
    failureTitle: i18n.t('project:create_table_modal.create_failed'),
    missingTable: i18n.t('project:create_table_modal.created_table_not_found'),
})

/**
 * The source table's name, kind and its own properties, read for the copy dialog.
 *
 * <p>It carries none of the table's rows, so a table of any size is read cheaply — the whole table stays on the
 * server and is copied there by id.
 */
export const getTableCopyInfo = async (
    projectId: string,
    tableId: string
): Promise<TableCopyInfo> => apiCall(
    `/projects/${toUrlSafeId(projectId)}/tables/${encodeURIComponent(tableId)}/properties`,
    undefined,
    LOCAL_LOAD_API_OPTIONS
) as Promise<TableCopyInfo>

/** Copy a table on the server by its id, without sending the table content. */
export const copyTable = async (
    projectId: string,
    tableId: string,
    request: CopyTableRequest
): Promise<SummaryTable | null> => writeTable(
    `/projects/${toUrlSafeId(projectId)}/tables/${encodeURIComponent(tableId)}/copy`,
    request,
    {
        successTitle: i18n.t('project:copy_table_modal.copied'),
        successDescription: table => i18n.t('project:copy_table_modal.copied_description', { table }),
        failureTitle: i18n.t('project:copy_table_modal.copy_failed'),
        missingTable: i18n.t('project:copy_table_modal.copied_table_not_found'),
    }
)

/**
 * Writes properties onto a table, leaving the ones it is not told about as they are.
 *
 * <p>Only the values cross the wire: the table's body takes no part in this, however large it is. A property
 * given no value is taken away, and a value the module or the category also declares applies again in its place.
 *
 * @returns the table's id after the write — it changes when the table had to be moved to grow — or null when
 *          the write failed, which is reported to the reader here
 */
export const updateTableProperties = async (
    projectId: string,
    tableId: string,
    properties: TableProperty[]
): Promise<string | null> => {
    try {
        const written = await apiCall(
            `/projects/${toUrlSafeId(projectId)}/tables/${encodeURIComponent(tableId)}/properties`,
            {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ properties }),
            },
            LOCAL_LOAD_API_OPTIONS
        ) as { id?: string } | null
        notification.success({ title: i18n.t('project:table_properties.saved') })
        // The table keeps its id unless it had to be moved to grow, and then the answer carries the new one.
        return written?.id ?? tableId
    } catch (error) {
        notifyLoadFailure(i18n.t('project:table_properties.save_failed'), error)
        return null
    }
}

/**
 * Writes the edits a reader made to a table, all of them in one request.
 *
 * <p>The edits are applied in the order they were made and the table is written once, so an editing session of
 * any size costs a single request — and a refused edit leaves the table exactly as it was.
 *
 * @returns the table's id after the write — it changes when the table had to be moved to grow — or null when
 *          the write failed, which is reported to the reader here
 */
export const applyTableActions = async (
    projectId: string,
    tableId: string,
    actions: TableEdit[]
): Promise<string | null> => {
    try {
        const written = await apiCall(
            `/projects/${toUrlSafeId(projectId)}/tables/${encodeURIComponent(tableId)}/actions/batch`,
            {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ actions }),
            },
            LOCAL_LOAD_API_OPTIONS
        ) as { id?: string } | null
        notification.success({ title: i18n.t('project:table_edit.saved') })
        // The table keeps its id unless it had to be moved to grow, and then the answer carries the new one.
        return written?.id ?? tableId
    } catch (error) {
        notifyLoadFailure(i18n.t('project:table_edit.save_failed'), error)
        return null
    }
}

/**
 * Removes a table from the module it is written in.
 *
 * <p>The whole area the table takes is cleared from its sheet, whatever kind of table it is; what stands around
 * it stays where it is. The table is gone from the rules once the module is compiled again, and out of the
 * Design repository once the project is saved.
 *
 * @returns whether the table was removed; a failure is reported to the reader here
 */
export const deleteTable = async (projectId: string, tableId: string, tableName: string): Promise<boolean> => {
    try {
        await apiCall(
            `/projects/${toUrlSafeId(projectId)}/tables/${encodeURIComponent(tableId)}`,
            { method: 'DELETE' },
            LOCAL_LOAD_API_OPTIONS
        )
        notification.success({
            title: i18n.t('project:delete_table.deleted'),
            description: i18n.t('project:delete_table.deleted_description', { table: tableName }),
        })
        return true
    } catch (error) {
        notifyLoadFailure(i18n.t('project:delete_table.delete_failed'), error)
        return false
    }
}

/** The address of a table's input, and of the cases and single case under it. */
const inputUrl = (projectId: string, tableId: string, suffix = ''): string =>
    `/projects/${toUrlSafeId(projectId)}/tables/${encodeURIComponent(tableId)}/input${suffix}`

/**
 * Reads the input a table takes to be executed.
 *
 * A rule table answers with its declared parameters, each carrying the JSON schema of the values it accepts. The
 * schema of the runtime context comes with them when the project provides one. A test table declares no
 * parameters of its own: its input is the cases it carries.
 */
export const getTableInput = async (
    projectId: string,
    tableId: string,
    options: { fromModule?: string } = {}
): Promise<TableInput> => {
    const query = options.fromModule ? `?fromModule=${encodeURIComponent(options.fromModule)}` : ''
    return await apiCall(inputUrl(projectId, tableId, query), undefined, LOCAL_LOAD_API_OPTIONS)
}

/**
 * Reads a page of the cases of a test table.
 *
 * A value with inner structure is left out of the page and read with its case.
 */
export const getTableInputCases = async (
    projectId: string,
    tableId: string,
    options: { fromModule?: string, page?: number, size?: number } = {}
): Promise<TableInputCasesPage> => {
    const params = new URLSearchParams()
    if (options.fromModule) {
        params.set('fromModule', options.fromModule)
    }
    params.set('page', String(options.page ?? 0))
    params.set('size', String(options.size ?? TEST_CASES_PAGE_SIZE))
    const page = await apiCall(
        inputUrl(projectId, tableId, `/cases?${params}`),
        undefined,
        LOCAL_LOAD_API_OPTIONS
    ) as TableInputCasesPage | null
    return { ...page, content: asArray(page?.content), total: page?.total ?? 0 }
}

/**
 * Reads one case of a test table with every value written in full.
 *
 * A page of cases leaves a value with inner structure out. This call brings it.
 */
export const getTableInputCase = async (
    projectId: string,
    tableId: string,
    caseId: string,
    options: { fromModule?: string } = {}
): Promise<TableInputTestCase> => {
    const query = options.fromModule ? `?fromModule=${encodeURIComponent(options.fromModule)}` : ''
    return await apiCall(
        inputUrl(projectId, tableId, `/cases/${encodeURIComponent(caseId)}${query}`),
        undefined,
        LOCAL_LOAD_API_OPTIONS
    )
}
