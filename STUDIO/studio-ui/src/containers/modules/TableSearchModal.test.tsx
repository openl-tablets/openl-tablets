import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { ModuleTable } from 'types/tables'
import { getRawTable, searchTables } from '../../services/modules'
import { getProjectProperties } from '../../services/projects'
import { TableSearchModal } from './TableSearchModal'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

vi.mock('../../services/modules', () => ({
    searchTables: vi.fn(),
    getRawTable: vi.fn(),
    TABLE_PAGE_ROWS: 120,
}))
vi.mock('../../services/projects', () => ({ getProjectProperties: vi.fn() }))

const found = (over: Partial<ModuleTable> = {}): ModuleTable => ({
    id: 'table-9',
    name: 'Greeting',
    kind: 'Rules',
    tableType: 'SimpleRules',
    ...over,
} as ModuleTable)

const open = (onOpen = vi.fn()) => {
    render(
        <TableSearchModal
            open
            initialName="Greet"
            moduleName="Claims"
            onClose={vi.fn()}
            onOpen={onOpen}
            projectId="p1"
        />
    )
    return onOpen
}

describe('TableSearchModal', () => {
    beforeEach(() => {
        vi.mocked(searchTables).mockResolvedValue([])
        vi.mocked(getProjectProperties).mockResolvedValue([
            {
                name: 'category',
                displayName: 'Category',
                group: 'Info',
                type: 'text',
                multiple: false,
                dimensional: false,
                defaultValue: null,
                pattern: null,
                values: [],
            },
            {
                name: 'state',
                displayName: 'State',
                group: 'Business Dimension',
                type: 'enum',
                multiple: true,
                dimensional: true,
                defaultValue: null,
                pattern: null,
                values: [{ code: 'AL', value: 'Alabama' }, { code: 'AZ', value: 'Arizona' }],
            },
        ])
    })

    it('opens on the module being read, carrying what was typed in the rail', async () => {
        open()

        await userEvent.click(screen.getByTestId('table-search-run'))

        expect(searchTables).toHaveBeenCalledWith('p1', expect.objectContaining({
            module: 'Claims',
            scope: 'module',
            name: 'Greet',
        }))
    })

    it('asks for everything the extended search offers', async () => {
        open()

        await userEvent.type(screen.getByTestId('table-search-header'), 'Spreadsheet')
        await userEvent.type(screen.getByTestId('table-search-text'), 'Good morning')
        await userEvent.click(screen.getByTestId('table-search-property-add'))
        await waitFor(() => expect(screen.getByTestId('table-search-property-value-0')).toBeInTheDocument())
        await userEvent.type(screen.getByTestId('table-search-property-value-0'), 'Auto')
        await userEvent.click(screen.getByTestId('table-search-run'))

        // A property with no name picked is not a filter, so it is left out of the request.
        expect(searchTables).toHaveBeenCalledWith('p1', expect.objectContaining({
            header: 'Spreadsheet',
            text: 'Good morning',
            properties: {},
        }))
    })

    it('narrows by several families of table at once, the way the Editor\'s own search did', async () => {
        open()

        await userEvent.click(screen.getByTestId('table-search-kind'))
        await userEvent.click(await screen.findByTitle('Rules'))
        await userEvent.click(await screen.findByTitle('Spreadsheet'))
        await userEvent.click(screen.getByTestId('table-search-run'))

        expect(searchTables).toHaveBeenCalledWith('p1', expect.objectContaining({
            kinds: ['Rules', 'Spreadsheet'],
        }))
    })

    it('narrows a property by the editor its own type asks for', async () => {
        open()

        await userEvent.click(screen.getByTestId('table-search-property-add'))
        await userEvent.click(await screen.findByTestId('table-search-property-0'))
        await userEvent.click(await screen.findByTitle('State'))
        // An enumeration is picked from its own values rather than typed, and several of them narrow together.
        await userEvent.click(screen.getByTestId('table-search-property-value-0'))
        await userEvent.click(await screen.findByTitle('Alabama'))
        await userEvent.click(await screen.findByTitle('Arizona'))
        await userEvent.click(screen.getByTestId('table-search-run'))

        expect(searchTables).toHaveBeenCalledWith('p1', expect.objectContaining({
            properties: { state: 'AL, AZ' },
        }))
    })

    it('reads each result by the header it is written with, and opens the one that is picked', async () => {
        vi.mocked(searchTables).mockResolvedValue([found({
            module: 'ClaimsTests',
            project: 'Pricing',
            returnType: 'String',
            signature: 'Greeting(Integer hour)',
        })])
        const onOpen = open()

        await userEvent.click(screen.getByTestId('table-search-run'))
        await waitFor(() => expect(screen.getByTestId('table-search-results')).toBeInTheDocument())

        // The whole header reads on one line, the type first, and where the table is written is said under it.
        expect(screen.getByTestId('table-search-result-table-9'))
            .toHaveTextContent('Rules String Greeting(Integer hour)')
        expect(screen.getByTestId('table-search-results')).toHaveTextContent('Pricing · ClaimsTests')

        await userEvent.click(screen.getByTestId('table-search-open-table-9'))

        expect(onOpen).toHaveBeenCalledWith(expect.objectContaining({ id: 'table-9', module: 'ClaimsTests' }))
    })

    it('reads a table that is no method by its own header, and one named after its type just once', async () => {
        vi.mocked(searchTables).mockResolvedValue([
            found({ id: 'datatype-1', kind: 'Datatype', name: 'Driver', signature: 'Driver' }),
            found({ id: 'test-1', kind: 'Test', name: 'DriverPremiumTest',
                signature: 'DetermineDriverPremium DriverPremiumTest' }),
            found({ id: 'env-1', kind: 'Environment', name: 'Environment' }),
        ])
        open()

        await userEvent.click(screen.getByTestId('table-search-run'))
        await waitFor(() => expect(screen.getByTestId('table-search-results')).toBeInTheDocument())

        expect(screen.getByTestId('table-search-result-datatype-1')).toHaveTextContent('Datatype Driver')
        expect(screen.getByTestId('table-search-result-test-1'))
            .toHaveTextContent('Test DetermineDriverPremium DriverPremiumTest')
        // A table with no signature of its own is named after its type, which is not written twice.
        expect(screen.getByTestId('table-search-result-env-1').textContent).toContain('Environment')
        expect(screen.getByTestId('table-search-result-env-1').textContent).not.toContain('Environment Environment')
    })

    it('reads the body of a result only when it is asked for, and folds it away again', async () => {
        vi.mocked(searchTables).mockResolvedValue([found({ module: 'ClaimsTests', project: 'Pricing' })])
        vi.mocked(getRawTable).mockResolvedValue({
            id: 'table-9',
            name: 'Greeting',
            source: [[{ cell: 'A1', value: 'Hello' }]],
        } as never)
        open()

        await userEvent.click(screen.getByTestId('table-search-run'))
        await waitFor(() => expect(screen.getByTestId('table-search-results')).toBeInTheDocument())

        // A search can answer with hundreds of tables; none of them is read until someone asks.
        expect(getRawTable).not.toHaveBeenCalled()

        await userEvent.click(screen.getByTestId('table-search-body-table-9'))

        await waitFor(() => expect(screen.getByTestId('table-search-grid-table-9')).toBeInTheDocument())
        // The body is read where the table is written, not where the search was started.
        expect(getRawTable).toHaveBeenCalledWith('p1', 'table-9', expect.objectContaining({ module: 'ClaimsTests' }))

        await userEvent.click(screen.getByTestId('table-search-body-table-9'))

        expect(screen.queryByTestId('table-search-grid-table-9')).toBeNull()
    })

    it('starts afresh when it is opened again, after a table was gone to', async () => {
        vi.mocked(searchTables).mockResolvedValue([found({ module: 'ClaimsTests', project: 'Pricing' })])
        const panel = (shown: boolean, initialName: string) => (
            <TableSearchModal
                initialName={initialName}
                moduleName="Claims"
                onClose={vi.fn()}
                onOpen={vi.fn()}
                open={shown}
                projectId="p1"
            />
        )
        const { rerender } = render(panel(true, 'Greet'))

        await userEvent.type(screen.getByTestId('table-search-header'), 'Spreadsheet')
        await userEvent.click(screen.getByTestId('table-search-run'))
        await waitFor(() => expect(screen.getByTestId('table-search-results')).toBeInTheDocument())

        // Opening a result closes the search; what it was asked last time is not what it is opened for next.
        rerender(panel(false, ''))
        rerender(panel(true, ''))

        await waitFor(() => expect(screen.queryByTestId('table-search-results')).toBeNull())
        expect(screen.getByTestId('table-search-name')).toHaveValue('')
        expect(screen.getByTestId('table-search-header')).toHaveValue('')
    })

    it('says when nothing answers the search', async () => {
        open()

        await userEvent.click(screen.getByTestId('table-search-run'))

        await waitFor(() => expect(screen.getByTestId('table-search-results'))
            .toHaveTextContent('browser.module.search_no_match'))
    })
})
