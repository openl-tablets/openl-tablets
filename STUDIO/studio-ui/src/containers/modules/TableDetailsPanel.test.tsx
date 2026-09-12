import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getTableDetails, type TableDetails } from '../../services/modules'
import { TableDetailsPanel } from './TableDetailsPanel'

vi.mock('../../services/modules', () => ({ getTableDetails: vi.fn() }))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

const DETAILS: TableDetails = {
    name: 'Greeting',
    groups: [
        {
            name: 'Info',
            properties: [{ name: 'description', displayName: 'Description', value: 'Says hello' }],
        },
        {
            name: 'Business Dimension',
            properties: [{
                name: 'lob',
                displayName: 'LOB',
                value: 'Banking',
                inheritedFrom: 'module',
                inheritedTableId: 'props-1',
            }],
        },
    ],
}

const draw = async (props: { tableId?: string | null, onOpenTable?: (id: string) => void } = {}) => {
    const view = render(
        <TableDetailsPanel
            moduleName="Claims"
            onOpenTable={props.onOpenTable ?? vi.fn()}
            projectId="p1"
            tableId={props.tableId === undefined ? 'table-1' : props.tableId}
        />
    )
    await act(async () => {
        await new Promise(resolve => setTimeout(resolve, 50))
    })
    return view
}

describe('TableDetailsPanel', () => {
    beforeEach(() => {
        localStorage.clear()
        vi.mocked(getTableDetails).mockResolvedValue(DETAILS)
    })

    it('lists what the table declares together with what it inherits', async () => {
        await draw()

        expect(getTableDetails).toHaveBeenCalledWith('p1', 'table-1', 'Claims')
        expect(screen.getByTestId('table-details-description')).toHaveTextContent('Says hello')
        expect(screen.getByTestId('table-details-lob')).toHaveTextContent('Banking')
        // Both groups are named, so a reader sees which dimension a value belongs to.
        expect(screen.getByText('Info')).toBeInTheDocument()
        expect(screen.getByText('Business Dimension')).toBeInTheDocument()
    })

    it('opens the properties table an inherited value comes from', async () => {
        const onOpenTable = vi.fn()
        await draw({ onOpenTable })

        await userEvent.click(screen.getByTestId('table-details-source-lob'))

        expect(onOpenTable).toHaveBeenCalledWith('props-1')
        // Only an inherited value names a table to open; the table's own property has none.
        expect(screen.queryByTestId('table-details-source-description')).not.toBeInTheDocument()
    })

    it('says so when the table declares no properties at all', async () => {
        vi.mocked(getTableDetails).mockResolvedValue({ name: 'Greeting', groups: []})
        await draw()

        expect(screen.getByTestId('table-details-empty')).toBeInTheDocument()
    })

    it('takes the width it was dragged to, and keeps it for the next table', async () => {
        localStorage.setItem('openl.module.tableDetails.width', '480')

        await draw()

        expect(screen.getByTestId('table-details')).toHaveStyle({ width: '480px' })
        expect(screen.getByTestId('table-details-resizer')).toBeInTheDocument()
    })

    it('reads at the size of the table it stands beside', async () => {
        await draw()

        // A property, and the group it belongs to, are the table's own small text rather than a louder voice.
        const [group] = screen.getAllByTestId(/^table-details-group-/)
        expect(group?.querySelector('.ant-descriptions-title')).toHaveStyle({ fontSize: '12px' })
        expect(group?.querySelector('.ant-descriptions-item-label')).toHaveStyle({ fontSize: '12px' })
        expect(group?.querySelector('.ant-descriptions-item-content')).toHaveStyle({ fontSize: '12px' })
    })

    it('keeps its own name for a table that carries none', async () => {
        // A properties table has no name of its own, and a bare panel head would name nothing at all.
        vi.mocked(getTableDetails).mockResolvedValue({ name: '', groups: []})
        await draw()

        expect(screen.getByText('browser.module.details')).toBeInTheDocument()
    })

    it('draws nothing for a table read that has not been asked for', async () => {
        await draw({ tableId: null })

        expect(getTableDetails).not.toHaveBeenCalled()
        expect(screen.getByTestId('table-details-empty')).toBeInTheDocument()
    })

    it('asks for nothing while it stands folded away', async () => {
        await draw()

        await userEvent.click(screen.getByTestId('table-details-toggle'))

        expect(screen.queryByTestId('table-details-body')).not.toBeInTheDocument()
        vi.mocked(getTableDetails).mockClear()
        await draw()
        expect(getTableDetails).not.toHaveBeenCalled()
    })
})
