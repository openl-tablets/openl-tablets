import { act, cleanup, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { ModuleTable, RawTableView } from 'types/tables'
import { getRawTable, searchTables } from '../../services/modules'
import { ApiHttpError } from '../../services/apiCall'
import { forgetKeptReads } from '../../services/keptReads'
import { AppThemeProvider } from '../../providers/AppThemeProvider'
import { renderInTheme } from '../../testing/theme'
import { ModuleSearchResults } from './ModuleSearchResults'

vi.mock('react-i18next', () => {
    const t = (key: string, options?: Record<string, unknown>) =>
        (options === undefined ? key : `${key} ${JSON.stringify(options)}`)
    return { useTranslation: () => ({ t, i18n: { language: 'en', resolvedLanguage: 'en' } }) }
})

vi.mock('../../services/modules', async importOriginal => ({
    ...await importOriginal<typeof import('../../services/modules')>(),
    searchTables: vi.fn(),
    getRawTable: vi.fn(),
}))

const found: ModuleTable[] = [
    {
        id: 'rate',
        name: 'Rate',
        kind: 'Rules',
        tableType: 'SimpleRules',
        sheet: 'Rates',
        pos: 'B3:D8',
        signature: 'Rate(String region)',
        returnType: 'Double',
    },
    { id: 'plan', name: 'Plan', kind: 'Spreadsheet', tableType: 'Spreadsheet', sheet: 'Rates', pos: 'F3:H9' },
    { id: 'driver', name: 'Driver', kind: 'Datatype', tableType: 'Datatype', sheet: 'Types', pos: 'A1:B4' },
    { id: 'notes', name: 'Notes', kind: 'Other', tableType: 'Other', sheet: 'Types', pos: 'D1:D2' },
]

const body: RawTableView = {
    id: 'rate',
    name: 'Rate',
    source: [
        [{ cell: 'B3', value: 'Rules Double Rate(String region)', colspan: 2 }, { covered: true }],
        [{ cell: 'B4', value: 'NORTH Region' }, { cell: 'C4', value: 1.2 }],
        [{ cell: 'B5', value: 'South' }, { cell: 'C5', value: 1.4 }],
    ],
    totalRows: 40,
}

const show = (over: Partial<Parameters<typeof ModuleSearchResults>[0]> = {}) => {
    const props = {
        projectId: 'p1',
        projectName: 'Rating',
        moduleName: 'Bank Rating',
        scope: 'module' as const,
        text: 'region',
        showOther: true,
        onShowOther: vi.fn(),
        onOpen: vi.fn(),
        onClose: vi.fn(),
        ...over,
    }
    const { rerender } = renderInTheme(<ModuleSearchResults {...props} />)
    // The screen keeps the theme it was drawn in when it is drawn again with other props.
    const redraw = (next: Partial<typeof props>) =>
        rerender(<AppThemeProvider><ModuleSearchResults {...props} {...next} /></AppThemeProvider>)
    return { ...props, redraw }
}

describe('ModuleSearchResults', () => {
    beforeEach(() => {
        // A table read by one test must not answer the next one.
        forgetKeptReads()
        vi.mocked(searchTables).mockResolvedValue(found)
        vi.mocked(getRawTable).mockResolvedValue(body)
    })

    it('searches every cell of the module for the text, in the order the workbook writes the tables', async () => {
        show()

        expect(screen.getByTestId('module-found-summary')).toHaveTextContent('browser.module.found_searching')
        await screen.findByTestId('module-found-rate')
        expect(searchTables).toHaveBeenCalledWith('p1', {
            module: 'Bank Rating',
            scope: 'module',
            text: 'region',
            includeOther: true,
            sort: 'position',
        })
        expect(screen.getByTestId('module-found-summary')).toHaveTextContent('browser.module.found {"count":4,"text":"region"}')
    })

    it('lists the tables found by the sheet they are written on, each named by its header and where it sits', async () => {
        show()

        const rates = await screen.findByRole('region', { name: 'Rates' })
        expect(within(rates).getAllByRole('listitem')).toHaveLength(2)
        expect(within(rates).getByText('2')).toBeInTheDocument()
        expect(screen.getByRole('region', { name: 'Types' })).toBeInTheDocument()
        const rate = screen.getByTestId('module-found-open-rate')
        expect(rate).toHaveTextContent('Rules Double Rate(String region)')
        expect(rate).toHaveTextContent('B3:D8')
        // The text looked for is marked wherever the header holds it, whatever the case of its letters.
        expect(within(rate).getByText('region', { selector: 'mark' })).toBeInTheDocument()
        // A table with no signature of its own is named after its type once.
        expect(screen.getByTestId('module-found-open-plan')).toHaveTextContent('Spreadsheet Plan')
    })

    it('names a version of a table by its dimensions after its header', async () => {
        vi.mocked(searchTables).mockResolvedValue([{ ...found[0] as ModuleTable, dimensions: { state: ['NY'] } }])
        show()

        expect(await screen.findByTestId('module-found-open-rate'))
            .toHaveTextContent('Rules Double Rate(String region) [state=NY]')
    })

    it('opens a table found in the editor, and puts the results away on Close', async () => {
        const { onOpen, onClose } = show()

        await userEvent.click(await screen.findByTestId('module-found-open-driver'))
        expect(onOpen).toHaveBeenCalledWith(found[2])

        await userEvent.click(screen.getByTestId('module-found-close'))
        expect(onClose).toHaveBeenCalled()
    })

    it('opens a table under its line, marking the cells that hold the text, and folds it away again', async () => {
        show()

        const toggle = await screen.findByTestId('module-found-toggle-rate')
        expect(toggle).toHaveAttribute('aria-expanded', 'false')
        await userEvent.click(toggle)

        const grid = await screen.findByTestId('module-found-grid-rate')
        expect(getRawTable).toHaveBeenCalledWith('p1', 'rate', expect.objectContaining({ module: 'Bank Rating', maxRows: 250 }))
        expect(toggle).toHaveAttribute('aria-expanded', 'true')
        const marked = (address: string) => (grid.querySelector(`td[data-cell="${address}"]`) as HTMLElement).className
        expect(marked('B4')).not.toBe(marked('B5'))
        expect(marked('B3')).toBe(marked('B4'))
        // A tall table is read a window deep, and says so.
        expect(screen.getByText('browser.module.found_part {"shown":3,"total":40}')).toBeInTheDocument()

        await userEvent.click(toggle)
        expect(screen.queryByTestId('module-found-grid-rate')).toBeNull()
    })

    it('says why a table could not be opened under its line', async () => {
        vi.mocked(getRawTable).mockRejectedValue(new Error('The table is gone'))
        show()

        await userEvent.click(await screen.findByTestId('module-found-toggle-rate'))

        expect(await screen.findByText('The table is gone')).toBeInTheDocument()
    })

    it('leaves the free-form tables out while the type filter hides them, counting them', async () => {
        const { onShowOther } = show({ showOther: false })

        await screen.findByTestId('module-found-rate')
        expect(screen.queryByTestId('module-found-notes')).toBeNull()
        expect(screen.getByTestId('module-found-summary')).toHaveTextContent('"count":3')

        await userEvent.click(screen.getByTestId('module-found-hidden-show'))
        expect(onShowOther).toHaveBeenCalledWith(true)
    })

    it('invites a shorter text when no cell holds it', async () => {
        vi.mocked(searchTables).mockResolvedValue([])
        show()

        expect(await screen.findByTestId('module-found-none')).toHaveTextContent('browser.module.found_none')
    })

    it('says why the search failed, and what to change for a text too long for the server', async () => {
        vi.mocked(searchTables).mockRejectedValueOnce(new Error('Module is not compiled'))
        show()
        expect(await screen.findByTestId('module-found-failed')).toHaveTextContent('Module is not compiled')
    })

    it('asks for a shorter text when the server refuses the address as too long', async () => {
        vi.mocked(searchTables).mockRejectedValueOnce(new ApiHttpError(414, ''))
        show()

        expect(await screen.findByTestId('module-found-failed')).toHaveTextContent('browser.module.found_too_long')
    })

    it('searches afresh for another text, dropping what the last search found', async () => {
        const { promise, resolve } = Promise.withResolvers<ModuleTable[]>()
        vi.mocked(searchTables).mockResolvedValueOnce(found).mockReturnValueOnce(promise)
        const { redraw } = show()
        await screen.findByTestId('module-found-rate')

        redraw({ text: 'premium' })

        expect(screen.queryByTestId('module-found-rate')).toBeNull()
        await waitFor(() => expect(searchTables).toHaveBeenLastCalledWith('p1', expect.objectContaining({ text: 'premium' })))
        await act(async () => resolve([]))
        expect(await screen.findByTestId('module-found-none')).toBeInTheDocument()
    })

    describe('searching wider than the module', () => {
        const elsewhere: ModuleTable[] = [
            { ...found[0] as ModuleTable, module: 'Bank Rating', project: 'Rating', projectId: 'p1' },
            { ...found[2] as ModuleTable, module: 'Domain', project: 'Rating', projectId: 'p1' },
            { ...found[1] as ModuleTable, module: 'Common', project: 'Shared', projectId: 'p2' },
        ]

        beforeEach(() => {
            vi.mocked(searchTables).mockResolvedValue(elsewhere)
        })

        it('offers to search the whole project with Enter, then the projects it depends on, then nothing wider', async () => {
            show()
            expect(await screen.findByTestId('module-found-wider')).toHaveTextContent('browser.module.found_wider_project')
            cleanup()

            show({ scope: 'project' })
            expect(await screen.findByTestId('module-found-wider')).toHaveTextContent('browser.module.found_wider_all')
            cleanup()

            show({ scope: 'all' })
            await screen.findByTestId('module-found-rate')
            expect(screen.queryByTestId('module-found-wider')).toBeNull()
            expect(searchTables).toHaveBeenLastCalledWith('p1', expect.objectContaining({ scope: 'all' }))
        })

        it('says the search waits for the project to be compiled while a wider one is on its way', () => {
            vi.mocked(searchTables).mockReturnValue(new Promise(() => {}))
            show({ scope: 'project' })

            expect(screen.getByText('browser.module.found_waiting')).toBeInTheDocument()
        })

        it('lists the tables of the project by module, each with its sheet before its range', async () => {
            show({ scope: 'project' })

            const domain = await screen.findByRole('region', { name: 'Domain' })
            expect(within(domain).getByTestId('module-found-open-driver')).toHaveTextContent('Types!A1:B4')
            expect(screen.getByRole('region', { name: 'Bank Rating' })).toBeInTheDocument()
        })

        it('lists the tables of the projects it depends on by project and module', async () => {
            show({ scope: 'all' })

            expect(await screen.findByRole('region', { name: 'Shared / Common' })).toBeInTheDocument()
            expect(screen.getByRole('region', { name: 'Rating / Domain' })).toBeInTheDocument()
        })

        it('reads a table opened under its line where it is written', async () => {
            show({ scope: 'all' })

            await userEvent.click(await screen.findByTestId('module-found-toggle-plan'))

            expect(getRawTable).toHaveBeenCalledWith('p2', 'plan', expect.objectContaining({ module: 'Common', maxRows: 250 }))
        })
    })
})
