import { notification } from 'antd'
import type { MockedFunction } from 'vitest'
import type { CopyTableRequest, CreateTableRequest } from 'types/tables'
import apiCall, { notifyLoadFailure } from './apiCall'
import {
    applyProjectTableTheme,
    copyTable,
    createTable,
    getDatatype,
    getProjectTables,
    getTableCopyInfo,
    getTableThemes,
    getTableThemesOf,
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
            'project:table_theme.project_applied': `Table theme applied to ${options?.count} tables`,
            'project:table_theme.project_skipped': `${options?.count} tables were left as they are.`,
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

describe('table theme', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.spyOn(notification, 'success').mockImplementation(() => {})
    })

    it('asks for the table themes once, and again only after an answer that failed', async () => {
        mockApiCall.mockRejectedValueOnce(new Error('down'))
        await expect(getTableThemes()).rejects.toThrow('down')

        mockApiCall.mockResolvedValueOnce([{ id: 'default', name: 'Default' }])
        await expect(getTableThemes()).resolves.toEqual([{ id: 'default', name: 'Default' }])
        await expect(getTableThemes()).resolves.toEqual([{ id: 'default', name: 'Default' }])

        expect(mockApiCall).toHaveBeenCalledTimes(2)
        expect(mockApiCall).toHaveBeenLastCalledWith('/table-themes', undefined,
            { throwError: true, suppressErrorPages: true })
    })

    it('asks which table themes have a look for a table, through the module it is read in', async () => {
        mockApiCall.mockResolvedValueOnce([{ id: 'green', name: 'Green' }])

        await expect(getTableThemesOf('project-id', 'table-id', 'Main'))
            .resolves.toEqual([{ id: 'green', name: 'Green' }])

        expect(mockApiCall).toHaveBeenCalledWith('/projects/project-id/tables/table-id/themes?module=Main', undefined,
            { throwError: true, suppressErrorPages: true })
    })

    it('says how many tables of the project were themed and how many were left as they are', async () => {
        mockApiCall.mockResolvedValueOnce({ themed: ['a', 'b'], skipped: ['c']})

        await expect(applyProjectTableTheme('project-id', 'green'))
            .resolves.toEqual({ themed: ['a', 'b'], skipped: ['c']})

        expect(mockApiCall).toHaveBeenCalledWith('/projects/project-id/theme?theme=green', { method: 'POST' },
            { throwError: true, suppressErrorPages: true })
        expect(notification.success).toHaveBeenCalledWith({
            title: 'Table theme applied to 2 tables',
            description: '1 tables were left as they are.',
        })
    })

    it('reads a list the server left out as an empty one', async () => {
        mockApiCall.mockResolvedValueOnce({})

        await applyProjectTableTheme('project-id', 'default')

        expect(notification.success).toHaveBeenCalledWith({ title: 'Table theme applied to 0 tables' })
    })

    it('answers nothing when the theme could not be written into the project', async () => {
        mockApiCall.mockRejectedValueOnce(new Error('locked'))

        await expect(applyProjectTableTheme('project-id', 'default')).resolves.toBeNull()

        expect(notifyLoadFailure).toHaveBeenCalledWith('project:table_theme.apply_failed', expect.any(Error))
    })
})
