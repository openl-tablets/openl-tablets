import { notification } from 'antd'
import type { MockedFunction } from 'vitest'
import type { CopyTableRequest, CreateTableRequest } from 'types/tables'
import apiCall, { notifyLoadFailure } from './apiCall'
import {
    applyTableActions,
    copyTable,
    createTable,
    deleteTable,
    formatProject,
    getProjectTables,
    getProjectTypes,
    getTableCopyInfo,
    updateTableProperties,
} from './tables'

vi.mock('./apiCall', () => ({
    default: vi.fn(),
    asArray: (value: unknown) => Array.isArray(value) ? value : [],
    LOCAL_LOAD_API_OPTIONS: { throwError: true, suppressErrorPages: true },
    notifyLoadFailure: vi.fn(),
}))
vi.mock('../i18n', () => ({
    default: {
        t: (key: string, options?: { table?: string, count?: number }) => ({
            'project:table_format.formatted': `${options?.count} tables formatted`,
            'project:table_format.skipped': `${options?.count} tables were left as they are.`,
            'project:create_table_modal.created': 'Table created',
            'project:create_table_modal.created_description':
                `The "${options?.table}" table was created successfully.`,
            'project:create_table_modal.create_failed': 'Failed to create the table',
            'project:create_table_modal.created_table_not_found': 'The created table could not be loaded.',
            'project:copy_table_modal.copied': 'Table copied',
            'project:copy_table_modal.copied_description':
                `The "${options?.table}" table was copied successfully.`,
            'project:copy_table_modal.copy_failed': 'Failed to copy the table',
            'project:copy_table_modal.copied_table_not_found': 'The copied table could not be loaded.',
        })[key] ?? key,
    },
}))

const mockApiCall = apiCall as MockedFunction<typeof apiCall>

const request: CreateTableRequest = {
    moduleName: 'Main',
    sheetName: 'Rules',
    table: {
        tableType: 'RawSource',
        kind: 'Rules',
        name: 'Eligibility',
        source: [[{ value: 'Rules Boolean Eligibility()' }]],
    },
}

const copyRequest: CopyTableRequest = {
    moduleName: 'Main',
    sheetName: 'Rules',
    name: 'EligibilityCopy',
    properties: [{ name: 'version', value: '1.2.3' }],
}

describe('createTable', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.spyOn(notification, 'success').mockImplementation(() => {})
        vi.spyOn(notification, 'error').mockImplementation(() => {})
    })

    it('posts the raw table and reports success', async () => {
        mockApiCall.mockResolvedValueOnce({
            id: 'table-id',
            tableType: 'RawSource',
            kind: 'Rules',
            name: 'Eligibility',
        })

        await expect(createTable('project-id', request)).resolves.toMatchObject({ id: 'table-id' })

        expect(mockApiCall).toHaveBeenCalledWith(
            '/projects/project-id/tables',
            {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(request),
            },
            { throwError: true, suppressErrorPages: true }
        )
        expect(notification.success).toHaveBeenCalledWith({
            title: 'Table created',
            description: 'The "Eligibility" table was created successfully.',
        })
    })

    it('reports an API error and keeps the caller in the modal', async () => {
        mockApiCall.mockRejectedValueOnce(new Error('Project is locked'))

        await expect(createTable('project-id', request)).resolves.toBeNull()

        expect(notification.error).toHaveBeenCalledWith({
            title: 'Failed to create the table',
            description: 'Project is locked',
        })
    })

    it('treats a missing compiled table response as a failed creation', async () => {
        mockApiCall.mockResolvedValueOnce(null)

        await expect(createTable('project-id', request)).resolves.toBeNull()

        expect(notification.error).toHaveBeenCalledWith(expect.objectContaining({
            title: 'Failed to create the table',
        }))
    })
})

describe('getProjectTables', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('asks for every table of the given kinds at once, not for a page of them', async () => {
        mockApiCall.mockResolvedValueOnce({ content: [{ id: 'table-id', tableType: 'Datatype', name: 'Customer' }]})

        await expect(getProjectTables('project-id', ['Datatype'])).resolves
            .toEqual([{ id: 'table-id', tableType: 'Datatype', name: 'Customer' }])
        // Paging would hide the table being looked for behind a page boundary.
        expect(mockApiCall.mock.calls[0]![0]).toBe('/projects/project-id/tables?kind=Datatype&unpaged=true')
    })

    it('asks for several kinds in one query', async () => {
        mockApiCall.mockResolvedValueOnce({ content: []})

        await getProjectTables('project-id', ['Rules', 'Column Match'])

        expect(mockApiCall.mock.calls[0]![0])
            .toBe('/projects/project-id/tables?kind=Rules&kind=Column%20Match&unpaged=true')
    })

    it('reads a page without content as no tables', async () => {
        // The mapper leaves out an empty collection, so a project with nothing of that kind answers without content.
        mockApiCall.mockResolvedValueOnce({ pageNumber: 0 })

        await expect(getProjectTables('project-id', ['Datatype'])).resolves.toEqual([])
    })

})

describe('getTableCopyInfo', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('reads the lightweight properties view for an encoded table identifier', async () => {
        mockApiCall.mockResolvedValueOnce({
            name: 'Eligibility',
            kind: 'Rules',
            properties: [{ name: 'version', value: '1.2.3' }],
        })

        await expect(getTableCopyInfo('project-id', 'source id')).resolves.toMatchObject({
            name: 'Eligibility',
        })

        expect(mockApiCall).toHaveBeenCalledWith(
            '/projects/project-id/tables/source%20id/properties',
            undefined,
            { throwError: true, suppressErrorPages: true }
        )
    })
})

describe('copyTable', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.spyOn(notification, 'success').mockImplementation(() => {})
        vi.spyOn(notification, 'error').mockImplementation(() => {})
    })

    it('posts the copy request to the copy endpoint of the source table', async () => {
        mockApiCall.mockResolvedValueOnce({
            id: 'copy-id',
            tableType: 'SimpleRules',
            kind: 'Rules',
            name: 'EligibilityCopy',
        })

        await expect(copyTable('project-id', 'source id', copyRequest)).resolves.toMatchObject({ id: 'copy-id' })

        expect(mockApiCall).toHaveBeenCalledWith(
            '/projects/project-id/tables/source%20id/copy',
            {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(copyRequest),
            },
            { throwError: true, suppressErrorPages: true }
        )
        expect(notification.success).toHaveBeenCalledWith({
            title: 'Table copied',
            description: 'The "EligibilityCopy" table was copied successfully.',
        })
    })

    it('reports a copy failure without closing its caller', async () => {
        mockApiCall.mockRejectedValueOnce(new Error('Version already exists'))

        await expect(copyTable('project-id', 'source-id', copyRequest)).resolves.toBeNull()

        expect(notification.error).toHaveBeenCalledWith({
            title: 'Failed to copy the table',
            description: 'Version already exists',
        })
    })
})

describe('getProjectTypes', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('reads every type of the project in one request', async () => {
        const policy = {
            name: 'Policy',
            kind: 'datatype',
            project: 'Provider',
            fields: [{ name: 'number', type: 'String', collection: false }],
            values: [],
        }
        mockApiCall.mockResolvedValueOnce([policy])

        await expect(getProjectTypes('project-id')).resolves.toEqual([policy])
        expect(mockApiCall.mock.calls[0]![0]).toBe('/projects/project-id/types')
    })

    it('reads an answer without a list as no types', async () => {
        mockApiCall.mockResolvedValueOnce(null)

        await expect(getProjectTypes('project-id')).resolves.toEqual([])
    })
})

describe('reformat Excel files', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.spyOn(notification, 'success').mockImplementation(() => {})
    })

    it('says how many tables of the project were formatted and how many were left as they are', async () => {
        mockApiCall.mockResolvedValueOnce({ formatted: ['a', 'b'], skipped: ['c']})

        await expect(formatProject('project-id')).resolves.toEqual({ formatted: ['a', 'b'], skipped: ['c']})

        expect(mockApiCall).toHaveBeenCalledWith('/projects/project-id/format', { method: 'POST' },
            { throwError: true, suppressErrorPages: true })
        expect(notification.success).toHaveBeenCalledWith({
            title: '2 tables formatted',
            description: '1 tables were left as they are.',
        })
    })

    it('reads a list the server left out as an empty one', async () => {
        mockApiCall.mockResolvedValueOnce({})

        await formatProject('project-id')

        expect(notification.success).toHaveBeenCalledWith({ title: '0 tables formatted' })
    })

    it('answers nothing when the Excel files could not be formatted', async () => {
        mockApiCall.mockRejectedValueOnce(new Error('locked'))

        await expect(formatProject('project-id')).resolves.toBeNull()

        expect(notifyLoadFailure).toHaveBeenCalledWith('project:table_format.failed', expect.any(Error))
    })
})

describe('the version of the table a write is based on', () => {
    const headersOf = (call: number): HeadersInit | undefined => mockApiCall.mock.calls[call]![1]?.headers

    it('is named by every write of the table, so a write over a table changed since the read is refused', async () => {
        mockApiCall.mockResolvedValue(null)

        await applyTableActions('project-id', 'table-id', [], 'Main', 'v1')
        await updateTableProperties('project-id', 'table-id', [], 'Main', 'v1')
        await deleteTable('project-id', 'table-id', 'Rates', 'Main', 'v1')

        expect(headersOf(0)).toEqual({ 'Content-Type': 'application/json', 'If-Match': '"v1"' })
        expect(headersOf(1)).toEqual({ 'Content-Type': 'application/json', 'If-Match': '"v1"' })
        expect(headersOf(2)).toEqual({ 'If-Match': '"v1"' })
    })

    it('is left out when it is not known, and the write is not compared', async () => {
        mockApiCall.mockResolvedValue(null)

        await applyTableActions('project-id', 'table-id', [], 'Main')
        await deleteTable('project-id', 'table-id', 'Rates', 'Main')

        expect(headersOf(0)).toEqual({ 'Content-Type': 'application/json' })
        expect(headersOf(1)).toEqual({})
    })
})
