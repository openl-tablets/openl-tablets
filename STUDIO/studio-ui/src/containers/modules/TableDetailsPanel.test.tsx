import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { ProjectProperty } from 'types/tables'
import { getTableDetails, type TableDetails } from '../../services/modules'
import { getProjectProperties } from '../../services/projects'
import { updateTableProperties } from '../../services/tables'
import { TableDetailsPanel } from './TableDetailsPanel'

vi.mock('../../services/modules', () => ({ getTableDetails: vi.fn() }))
vi.mock('../../services/projects', () => ({ getProjectProperties: vi.fn() }))
vi.mock('../../services/tables', () => ({ updateTableProperties: vi.fn() }))

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
    canEditProperties: true,
    available: ['category'],
}

const property = (over: Partial<ProjectProperty>): ProjectProperty => ({
    name: 'description',
    displayName: 'Description',
    group: 'Info',
    type: 'text',
    multiple: false,
    dimensional: false,
    defaultValue: null,
    pattern: null,
    values: [],
    ...over,
})

const DICTIONARY: ProjectProperty[] = [
    property({}),
    property({ name: 'category', displayName: 'Category' }),
    property({ name: 'lob', displayName: 'LOB', group: 'Business Dimension' }),
]

interface DrawProps {
    tableId?: string | null
    onOpenTable?: (id: string) => void
    canWrite?: boolean
    onSaved?: (tableId: string) => void
}

const draw = async (props: DrawProps = {}) => {
    const view = render(
        <TableDetailsPanel
            canWrite={props.canWrite ?? false}
            moduleName="Claims"
            onOpenTable={props.onOpenTable ?? vi.fn()}
            onSaved={props.onSaved}
            projectId="p1"
            tableId={props.tableId === undefined ? 'table-1' : props.tableId}
        />
    )
    await act(async () => {
        await new Promise(resolve => setTimeout(resolve, 50))
    })
    return view
}

/** Opens the panel for writing, with the property dictionary read. */
const edit = async (props: DrawProps = {}) => {
    const view = await draw({ canWrite: true, ...props })
    await userEvent.click(screen.getByTestId('table-details-edit'))
    await act(async () => {
        await new Promise(resolve => setTimeout(resolve, 50))
    })
    return view
}

describe('TableDetailsPanel', () => {
    beforeEach(() => {
        localStorage.clear()
        vi.mocked(getTableDetails).mockResolvedValue(DETAILS)
        vi.mocked(getProjectProperties).mockResolvedValue(DICTIONARY)
        vi.mocked(updateTableProperties).mockResolvedValue('table-1')
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
        vi.mocked(getTableDetails).mockResolvedValue({
            name: 'Greeting', groups: [], canEditProperties: true, available: [],
        })
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
        vi.mocked(getTableDetails).mockResolvedValue({
            name: '', groups: [], canEditProperties: false, available: [],
        })
        await draw()

        expect(screen.getByText('browser.module.details')).toBeInTheDocument()
    })

    it('draws nothing for a table read that has not been asked for', async () => {
        await draw({ tableId: null })

        expect(getTableDetails).not.toHaveBeenCalled()
        expect(screen.getByTestId('table-details-empty')).toBeInTheDocument()
    })

    it('offers no writing to a reader who may not edit the project', async () => {
        await draw()

        expect(screen.queryByTestId('table-details-edit')).not.toBeInTheDocument()
    })

    it('offers no writing on a table that carries no properties at all', async () => {
        vi.mocked(getTableDetails).mockResolvedValue({
            name: 'Environment', groups: [], canEditProperties: false, available: [],
        })
        await draw({ canWrite: true })

        expect(screen.queryByTestId('table-details-edit')).not.toBeInTheDocument()
    })

    it('writes only the properties the reader changed', async () => {
        const onSaved = vi.fn()
        await edit({ onSaved })

        await userEvent.clear(screen.getByTestId('table-details-input-description'))
        await userEvent.type(screen.getByTestId('table-details-input-description'), 'Greets by the hour')
        await userEvent.click(screen.getByTestId('table-details-save'))

        // The value it was not asked about is not sent, so the table keeps it as it stands.
        expect(updateTableProperties).toHaveBeenCalledWith('p1', 'table-1',
            [{ name: 'description', value: 'Greets by the hour' }])
        expect(onSaved).toHaveBeenCalledWith('table-1')
    })

    it('writes an inherited value onto the table when the reader changes it', async () => {
        await edit()

        await userEvent.clear(screen.getByTestId('table-details-input-lob'))
        await userEvent.type(screen.getByTestId('table-details-input-lob'), 'Insurance')
        await userEvent.click(screen.getByTestId('table-details-save'))

        expect(updateTableProperties).toHaveBeenCalledWith('p1', 'table-1',
            [{ name: 'lob', value: 'Insurance' }])
    })

    it('takes a property away by writing it with no value', async () => {
        await edit()

        await userEvent.click(screen.getByTestId('table-details-remove-description'))

        // The row goes at once, and what is sent says the property is to be taken away.
        expect(screen.queryByTestId('table-details-input-description')).not.toBeInTheDocument()
        await userEvent.click(screen.getByTestId('table-details-save'))
        expect(updateTableProperties).toHaveBeenCalledWith('p1', 'table-1',
            [{ name: 'description', value: null }])
    })

    it('adds only a property the table may still be given', async () => {
        await edit()

        await userEvent.click(screen.getByTestId('table-details-add'))
        // The ones it already shows are not offered again: they are changed where they stand.
        expect(await screen.findByTitle('Category')).toBeInTheDocument()
        expect(screen.queryByTitle('Description')).not.toBeInTheDocument()

        await userEvent.click(screen.getByTitle('Category'))

        expect(screen.getByTestId('table-details-input-category')).toBeInTheDocument()
    })

    it('drops what was written when the reader gives up', async () => {
        await edit()

        await userEvent.clear(screen.getByTestId('table-details-input-description'))
        await userEvent.type(screen.getByTestId('table-details-input-description'), 'Something else')
        await userEvent.click(screen.getByTestId('table-details-cancel'))

        expect(updateTableProperties).not.toHaveBeenCalled()
        expect(screen.getByTestId('table-details-description')).toHaveTextContent('Says hello')
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
