import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { ModuleTable } from 'types/tables'
import { searchTables } from '../../services/modules'
import { getProjectProperties } from '../../services/projects'
import { TableSearchModal } from './TableSearchModal'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

vi.mock('../../services/modules', () => ({ searchTables: vi.fn() }))
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

    it('lists what it found and opens the one that is picked', async () => {
        vi.mocked(searchTables).mockResolvedValue([found({ module: 'ClaimsTests', project: 'Pricing' })])
        const onOpen = open()

        await userEvent.click(screen.getByTestId('table-search-run'))
        await waitFor(() => expect(screen.getByTestId('table-search-results')).toBeInTheDocument())

        // Where a table lives is shown beside it, since a search reaches past the module on screen.
        expect(screen.getByTestId('table-search-results')).toHaveTextContent('Pricing · ClaimsTests')

        await userEvent.click(screen.getByText('Greeting'))

        expect(onOpen).toHaveBeenCalledWith(expect.objectContaining({ id: 'table-9', module: 'ClaimsTests' }))
    })

    it('says when nothing answers the search', async () => {
        open()

        await userEvent.click(screen.getByTestId('table-search-run'))

        await waitFor(() => expect(screen.getByTestId('table-search-results'))
            .toHaveTextContent('browser.module.search_no_match'))
    })
})
