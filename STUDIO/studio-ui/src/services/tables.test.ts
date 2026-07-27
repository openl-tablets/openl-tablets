import { notification } from 'antd'
import type { MockedFunction } from 'vitest'
import type { CreateTableRequest } from 'types/tables'
import apiCall from './apiCall'
import { createTable, getDatatype, getProjectTables } from './tables'

vi.mock('./apiCall', () => ({
    default: vi.fn(),
    asArray: (value: unknown) => Array.isArray(value) ? value : [],
}))
vi.mock('../i18n', () => ({
    default: {
        t: (key: string, options?: { table?: string }) => ({
            'project:create_table_modal.created': 'Table created',
            'project:create_table_modal.created_description':
                `The "${options?.table}" table was created successfully.`,
            'project:create_table_modal.create_failed': 'Failed to create the table',
            'project:create_table_modal.created_table_not_found': 'The created table could not be loaded.',
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

describe('getDatatype', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('reads the fields of one datatype, which the list does not carry', async () => {
        mockApiCall.mockResolvedValueOnce({
            extends: 'Party',
            fields: [{ name: 'name', type: 'String' }, { name: 'age' }],
        })

        await expect(getDatatype('project-id', 'customer id')).resolves.toEqual({
            extends: 'Party',
            // A field OpenL could not type is still a column; it is the example value that has nothing to go on.
            fields: [{ name: 'name', type: 'String' }, { name: 'age', type: '' }],
            values: [],
        })
        expect(mockApiCall.mock.calls[0]![0]).toBe('/projects/project-id/tables/customer%20id')
    })

    it('reads a datatype extending nothing without an empty parent', async () => {
        mockApiCall.mockResolvedValueOnce({ fields: [{ name: 'name', type: 'String' }]})

        await expect(getDatatype('project-id', 'customer-id')).resolves
            .toEqual({ fields: [{ name: 'name', type: 'String' }], values: []})
    })

    it('reads the values of a vocabulary, which is written with the same keyword', async () => {
        // A vocabulary declares values rather than fields, and a value of any type is written as it reads.
        mockApiCall.mockResolvedValueOnce({ type: 'Integer', values: [{ value: 1 }, { value: 2 }, {}]})

        await expect(getDatatype('project-id', 'limits-id')).resolves.toEqual({ fields: [], values: ['1', '2']})
    })
})
