import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { ModuleTable } from 'types/tables'
import { ModuleTablesTree } from './ModuleTablesTree'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

vi.mock('../../store', () => ({ useUserStore: () => undefined }))

const tables: ModuleTable[] = [
    { id: 'one', name: 'Greeting', kind: 'Rules', tableType: 'SimpleRules', sheet: 'Rules' } as ModuleTable,
]

const rail = () => {
    render(
        <ModuleTablesTree
            onExtendedSearch={vi.fn()}
            onSelectTable={vi.fn()}
            tables={tables}
        />
    )
}

describe('ModuleTablesTree', () => {
    beforeEach(() => localStorage.clear())

    it('takes the width it was dragged to, so a long name can be made room for', () => {
        localStorage.setItem('openl.module.rail.width', '420')

        rail()

        expect(screen.getByTestId('module-rail')).toHaveStyle({ width: '420px' })
        expect(screen.getByTestId('module-rail-resizer')).toBeInTheDocument()
    })

    it('searches the names it shows, and hands the rest to the extended search', async () => {
        const shown: ModuleTable[] = [
            { id: 'one', name: 'Greeting', kind: 'Rules', tableType: 'SimpleRules', sheet: 'Rules' } as ModuleTable,
            { id: 'two', name: 'Premium', kind: 'Rules', tableType: 'SimpleRules', sheet: 'Rules' } as ModuleTable,
        ]
        const onExtendedSearch = vi.fn()
        render(
            <ModuleTablesTree
                onExtendedSearch={onExtendedSearch}
                onSelectTable={vi.fn()}
                selectedTableId="two"
                tables={shown}
            />
        )
        expect(screen.getByText('Premium')).toBeInTheDocument()

        await userEvent.type(screen.getByTestId('module-tables-search'), 'greet')

        // The tables are already in the browser, so the search costs no request.
        expect(screen.queryByText('Premium')).toBeNull()

        await userEvent.click(screen.getByTestId('module-tables-search-extended'))

        // Anything wider than a name is the extended search's to ask the server, and it starts from what was typed.
        expect(onExtendedSearch).toHaveBeenCalledWith('greet')
    })

    it('says when nothing in the module answers the search', async () => {
        rail()

        await userEvent.type(screen.getByTestId('module-tables-search'), 'nothing here')

        expect(screen.getByTestId('module-tables-empty')).toHaveTextContent('browser.module.no_match')
    })

    it('says how many errors a table raised, and marks the one a test exercises', async () => {
        const broken = { ...tables[0], id: 'bad', name: 'Broken', errors: 3 } as ModuleTable
        const covered = { ...tables[0], id: 'ok', name: 'Covered', hasTests: true } as ModuleTable

        render(
            <ModuleTablesTree
                onExtendedSearch={vi.fn()}
                onSelectTable={vi.fn()}
                selectedTableId="bad"
                tables={[...tables, broken, covered]}
            />
        )

        // The broken table says three, and the sheet it is written on says three for it — the tables that
        // compiled say nothing. Only the table a test exercises is marked.
        expect(screen.getAllByTestId('module-table-errors').map(badge => badge.textContent)).toEqual(['3', '3'])
        expect(screen.getAllByTestId('module-table-tested')).toHaveLength(1)
    })

    it('gathers on a group the errors of everything under it', () => {
        const here = { ...tables[0], id: 'bad', name: 'Broken', sheet: 'Claims', errors: 3 } as ModuleTable
        const alsoHere = { ...tables[0], id: 'worse', name: 'Worse', sheet: 'Claims', errors: 4 } as ModuleTable

        render(
            <ModuleTablesTree
                onExtendedSearch={vi.fn()}
                onSelectTable={vi.fn()}
                selectedTableId="bad"
                tables={[...tables, here, alsoHere]}
            />
        )

        // The sheet holding both stands for seven; the sheet holding the table that compiled stands for none.
        expect(screen.getAllByTestId('module-table-errors').map(badge => badge.textContent)).toEqual(['7', '3', '4'])
    })

    it('shows what a table is, in full, on the name the tree cuts short', async () => {
        const named = { ...tables[0], id: 'sig', name: 'Region', signature: 'Region (String state)' } as ModuleTable

        render(
            <ModuleTablesTree
                onExtendedSearch={vi.fn()}
                onSelectTable={vi.fn()}
                selectedTableId="sig"
                tables={[named]}
            />
        )

        await userEvent.hover(screen.getByText('Region'))

        expect(await screen.findByText('Region (String state)')).toBeInTheDocument()
    })

    it('draws a table that takes no part in the rules apart from the others', () => {
        const switchedOff = { ...tables[0], id: 'off', name: 'Retired', active: false } as ModuleTable

        render(
            <ModuleTablesTree
                onExtendedSearch={vi.fn()}
                onSelectTable={vi.fn()}
                selectedTableId="off"
                tables={[...tables, switchedOff]}
            />
        )

        expect(screen.getByTestId('module-table-inactive')).toHaveTextContent('Retired')
        // Only the switched-off table is drawn that way.
        expect(screen.getAllByTestId('module-table-inactive')).toHaveLength(1)
    })
})
