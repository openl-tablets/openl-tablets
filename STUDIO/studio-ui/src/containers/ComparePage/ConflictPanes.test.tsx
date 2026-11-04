import { screen } from '@testing-library/react'
import { ConflictPanes, THEIRS_FIRST_PANES, VERSION_PANES } from './ComparisonPanes'
import { renderInTheme } from 'testing/theme'
import type { ConflictComparisonTable } from 'types/compare'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

/**
 * A greeting both branches changed: your branch and theirs set R20 differently, and their branch alone changed R30.
 */
const GREETING: ConflictComparisonTable = {
    id: '0-0',
    name: 'Rules String Hello',
    status: 'changed',
    yoursStatus: 'changed',
    theirsStatus: 'changed',
    base: {
        source: [
            [{ cell: 'A1', value: 'R10' }, { cell: 'B1', value: 'Good Morning' }],
            [{ cell: 'A2', value: 'R20' }, { cell: 'B2', value: 'Hi' }],
            [{ cell: 'A3', value: 'R30' }, { cell: 'B3', value: 'Good Evening' }],
        ],
    },
    yours: {
        source: [
            [{ cell: 'A1', value: 'R10' }, { cell: 'B1', value: 'Good Morning' }],
            [{ cell: 'A2', value: 'R20' }, { cell: 'B2', value: 'Hi!' }],
            [{ cell: 'A3', value: 'R30' }, { cell: 'B3', value: 'Good Evening' }],
        ],
        changedCells: ['B2'],
        conflictCells: ['B2'],
    },
    theirs: {
        source: [
            [{ cell: 'A1', value: 'R10' }, { cell: 'B1', value: 'Good Morning' }],
            [{ cell: 'A2', value: 'R20' }, { cell: 'B2', value: 'Hello' }],
            [{ cell: 'A3', value: 'R30' }, { cell: 'B3', value: 'Evening!' }],
        ],
        changedCells: ['B2', 'B3'],
        conflictCells: ['B2'],
    },
    rows: [
        { base: 0, yours: 0, theirs: 0, yoursStatus: 'equal', theirsStatus: 'equal' },
        { base: 1, yours: 1, theirs: 1, yoursStatus: 'changed', theirsStatus: 'changed' },
        { base: 2, yours: 2, theirs: 2, yoursStatus: 'equal', theirsStatus: 'changed' },
    ],
    columns: [
        { base: 0, yours: 0, theirs: 0, yoursStatus: 'equal', theirsStatus: 'equal' },
        { base: 1, yours: 1, theirs: 1, yoursStatus: 'changed', theirsStatus: 'changed' },
    ],
}

const panes = (view: 'sides' | 'combined', order = VERSION_PANES) => (
    <ConflictPanes
        showEqualRows
        error={null}
        loading={false}
        panes={order}
        table={GREETING}
        view={view}
    />
)

describe('ConflictPanes', () => {
    it('stands your version and theirs side by side, each compared with the base, without the base', () => {
        renderInTheme(panes('sides'))

        expect(screen.getByTestId('compare-pane-yours')).toHaveTextContent('Hi!')
        expect(screen.getByTestId('compare-pane-theirs')).toHaveTextContent('Evening!')
        expect(screen.queryByTestId('compare-pane-base')).toBeNull()
        expect(screen.getByTestId('compare-legend')).toHaveTextContent('status_conflict')
    })

    it('draws both versions as one table: a cell both changed differently carries the base and both values', () => {
        renderInTheme(panes('combined'))

        const combined = screen.getByTestId('compare-combined')
        // The base value first, then each value named, so neither reads as what the cell ends up holding.
        expect(combined).toHaveTextContent('Hi →yours_shortHi!theirs_shortHello')
        expect(screen.getByTestId('compare-combined-yours')).toHaveTextContent('yours_shortHi!')
        expect(screen.getByTestId('compare-combined-theirs')).toHaveTextContent('theirs_shortHello')
        expect(screen.getByTitle('status_conflict')).toBeInTheDocument()
        // A cell only their branch changed carries the base and their value.
        expect(combined).toHaveTextContent('Good Evening → Evening!')
        expect(combined).not.toHaveTextContent('Good Morning →')
    })

    it('frames a row one version removed while the other one changed it as a conflict', () => {
        const removedByYours: ConflictComparisonTable = {
            ...GREETING,
            yours: { source: GREETING.yours!.source.slice(0, 2), changedCells: ['B2'], conflictCells: ['B2']},
            rows: [
                GREETING.rows![0]!,
                GREETING.rows![1]!,
                { base: 2, theirs: 2, yoursStatus: 'removed', theirsStatus: 'changed' },
            ],
        }
        const { container } = renderInTheme(
            <ConflictPanes
                showEqualRows
                error={null}
                loading={false}
                panes={VERSION_PANES}
                table={removedByYours}
                view="combined"
            />)

        const evening = screen.getByText('Good Evening')
        expect(evening.closest('td')?.className).not.toBe('')
        // The removed row carries no value change, only its base value.
        expect(container).not.toHaveTextContent('Good Evening →')
    })

    it('draws both tables when both versions added a table the base does not hold', () => {
        const added: ConflictComparisonTable = {
            id: GREETING.id,
            name: GREETING.name,
            status: 'added',
            yoursStatus: 'added',
            theirsStatus: 'added',
            yours: GREETING.yours!,
            theirs: GREETING.theirs!,
            rows: [],
            columns: [],
        }
        renderInTheme(
            <ConflictPanes
                showEqualRows
                error={null}
                loading={false}
                panes={VERSION_PANES}
                table={added}
                view="combined"
            />)

        const combined = screen.getByTestId('compare-combined')
        expect(combined).toHaveTextContent('Hi!')
        expect(combined).toHaveTextContent('Hello')
    })

    it('stands their version first when your changes go into their branch', () => {
        const { unmount } = renderInTheme(panes('sides', THEIRS_FIRST_PANES))
        const [first] = screen.getAllByTestId(/^compare-pane-(yours|theirs)$/)
        expect(first).toHaveAttribute('data-testid', 'compare-pane-theirs')
        unmount()

        renderInTheme(panes('combined', THEIRS_FIRST_PANES))
        expect(screen.getByTestId('compare-combined')).toHaveTextContent('Hi →theirs_shortHelloyours_shortHi!')
    })
})
