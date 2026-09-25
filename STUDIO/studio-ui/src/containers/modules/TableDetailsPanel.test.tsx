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
    kind: 'Rules',
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
    available: ['category', 'tags'],
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
    // Written on a table alone, never in a Properties table: only the dictionary of the table's kind knows it.
    property({ name: 'tags', displayName: 'Tags', multiple: true }),
    // In the dictionary, but not among the ones this table may still be given.
    property({ name: 'createdBy', displayName: 'Created By', group: 'Info' }),
]

interface DrawProps {
    tableId?: string | null
    onOpenTable?: (id: string) => void
    canWrite?: boolean
    onSaved?: (tableId: string) => void
    listed?: boolean
    beforeSave?: () => Promise<string | null>
}

const panel = (props: DrawProps) => (
    <TableDetailsPanel
        beforeSave={props.beforeSave}
        canWrite={props.canWrite ?? false}
        listed={props.listed ?? true}
        moduleName="Claims"
        onOpenTable={props.onOpenTable ?? vi.fn()}
        onSaved={props.onSaved}
        projectId="p1"
        tableId={props.tableId === undefined ? 'table-1' : props.tableId}
    />
)

/** Lets the reads on their way answer. */
const settle = () => act(async () => {
    await new Promise(resolve => setTimeout(resolve, 50))
})

const draw = async (props: DrawProps = {}) => {
    const view = render(panel(props))
    await settle()
    return view
}

/** Opens the panel for writing, with the property dictionary read. */
const edit = async (props: DrawProps = {}) => {
    const view = await draw({ canWrite: true, ...props })
    await userEvent.click(screen.getByTestId('table-details-edit'))
    await settle()
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
            name: 'Greeting', kind: 'Rules', groups: [], canEditProperties: true, available: [],
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
            name: '', kind: 'Other', groups: [], canEditProperties: false, available: [],
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
            name: 'Environment', kind: 'Environment', groups: [], canEditProperties: false, available: [],
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
            [{ name: 'description', value: 'Greets by the hour' }], 'Claims')
        expect(onSaved).toHaveBeenCalledWith('table-1')
    })

    it('writes what the table holds before its properties, and writes them to the table that leaves', async () => {
        // The reader has cells of their own on screen; writing them may move the table, so the properties
        // are written to the table as it stands afterwards.
        const beforeSave = vi.fn().mockResolvedValue('table-2')
        const onSaved = vi.fn()
        await edit({ beforeSave, onSaved })

        await userEvent.clear(screen.getByTestId('table-details-input-description'))
        await userEvent.type(screen.getByTestId('table-details-input-description'), 'Greets by the hour')
        await userEvent.click(screen.getByTestId('table-details-save'))

        expect(beforeSave).toHaveBeenCalled()
        expect(updateTableProperties).toHaveBeenCalledWith('p1', 'table-2',
            [{ name: 'description', value: 'Greets by the hour' }], 'Claims')
        expect(onSaved).toHaveBeenCalledWith('table-1')
    })

    it('writes no properties where what the table holds cannot be written', async () => {
        const beforeSave = vi.fn().mockResolvedValue(null)
        await edit({ beforeSave })

        await userEvent.clear(screen.getByTestId('table-details-input-description'))
        await userEvent.type(screen.getByTestId('table-details-input-description'), 'Greets by the hour')
        await userEvent.click(screen.getByTestId('table-details-save'))

        // Written on their own, the properties would be written over the cells waiting beside them.
        expect(updateTableProperties).not.toHaveBeenCalled()
        expect(screen.getByTestId('table-details-save')).toBeInTheDocument()
    })

    it('writes an inherited value onto the table when the reader changes it', async () => {
        await edit()

        await userEvent.clear(screen.getByTestId('table-details-input-lob'))
        await userEvent.type(screen.getByTestId('table-details-input-lob'), 'Insurance')
        await userEvent.click(screen.getByTestId('table-details-save'))

        expect(updateTableProperties).toHaveBeenCalledWith('p1', 'table-1',
            [{ name: 'lob', value: 'Insurance' }], 'Claims')
    })

    it('takes a property away by writing it with no value', async () => {
        await edit()

        await userEvent.click(screen.getByTestId('table-details-remove-description'))

        // The row goes at once, and what is sent says the property is to be taken away.
        expect(screen.queryByTestId('table-details-input-description')).not.toBeInTheDocument()
        await userEvent.click(screen.getByTestId('table-details-save'))
        expect(updateTableProperties).toHaveBeenCalledWith('p1', 'table-1',
            [{ name: 'description', value: null }], 'Claims')
    })

    it('adds only a property the table may still be given', async () => {
        await edit()

        await userEvent.click(screen.getByTestId('table-details-add'))
        // The ones it already shows are not offered again: they are changed where they stand.
        expect(await screen.findByTitle('Category')).toBeInTheDocument()
        expect(screen.queryByTitle('Description')).not.toBeInTheDocument()
        // Nor is one the table may not carry, whatever the dictionary holds.
        expect(screen.queryByTitle('Created By')).not.toBeInTheDocument()

        await userEvent.click(screen.getByTitle('Category'))

        expect(screen.getByTestId('table-details-input-category')).toBeInTheDocument()
    })

    it('offers a property a table alone may carry, which no Properties table declares', async () => {
        await edit()

        await userEvent.click(screen.getByTestId('table-details-add'))

        // The dictionary is read for the table's own kind: the one read for a Properties table would not hold it.
        expect(getProjectProperties).toHaveBeenCalledWith('p1', 'Rules')
        expect(await screen.findByTitle('Tags')).toBeInTheDocument()
    })

    it('drops what was written when the reader gives up', async () => {
        await edit()

        await userEvent.clear(screen.getByTestId('table-details-input-description'))
        await userEvent.type(screen.getByTestId('table-details-input-description'), 'Something else')
        await userEvent.click(screen.getByTestId('table-details-cancel'))

        expect(updateTableProperties).not.toHaveBeenCalled()
        expect(screen.getByTestId('table-details-description')).toHaveTextContent('Says hello')
    })

    it('reads the dictionary of the kind the table belongs to, once per kind', async () => {
        vi.mocked(getTableDetails).mockResolvedValue({ ...DETAILS, kind: 'Spreadsheet' })
        const view = await edit()
        expect(getProjectProperties).toHaveBeenCalledWith('p1', 'Spreadsheet')

        // Another table of the same kind: the dictionary read for it is the one already held.
        vi.mocked(getTableDetails).mockResolvedValue({ ...DETAILS, name: 'Farewell', kind: 'Spreadsheet' })
        view.rerender(panel({ canWrite: true, tableId: 'table-2' }))
        await settle()
        await userEvent.click(screen.getByTestId('table-details-edit'))
        await settle()
        expect(getProjectProperties).toHaveBeenCalledTimes(1)
    })

    it('holds a dictionary that answers late under its own kind', async () => {
        const forData = Promise.withResolvers<ProjectProperty[]>()
        vi.mocked(getProjectProperties).mockImplementation((_project, kind) => kind === 'Data'
            ? forData.promise
            : Promise.resolve(DICTIONARY))
        vi.mocked(getTableDetails).mockResolvedValue({ ...DETAILS, kind: 'Data' })
        const view = await edit()

        // A Rules table is looked at while the Data dictionary is still on its way, and that one answers last.
        vi.mocked(getTableDetails).mockResolvedValue(DETAILS)
        view.rerender(panel({ canWrite: true, tableId: 'table-2' }))
        await settle()
        await userEvent.click(screen.getByTestId('table-details-edit'))
        await settle()
        forData.resolve([property({ name: 'category', displayName: 'Category of a Data table' })])
        await settle()

        await userEvent.click(screen.getByTestId('table-details-add'))
        expect(await screen.findByTitle('Category')).toBeInTheDocument()
        expect(screen.queryByTitle('Category of a Data table')).not.toBeInTheDocument()
    })

    it('asks for a dictionary once while it is on its way', async () => {
        const forRules = Promise.withResolvers<ProjectProperty[]>()
        vi.mocked(getProjectProperties).mockReturnValue(forRules.promise)
        await edit()

        // Writing is given up and taken up again before the dictionary answers: it is not asked for a second time.
        await userEvent.click(screen.getByTestId('table-details-cancel'))
        await userEvent.click(screen.getByTestId('table-details-edit'))
        await settle()
        expect(getProjectProperties).toHaveBeenCalledTimes(1)

        forRules.resolve(DICTIONARY)
        await userEvent.click(screen.getByTestId('table-details-add'))
        expect(await screen.findByTitle('Category')).toBeInTheDocument()
    })

    it('asks for a dictionary again after a read of it failed', async () => {
        vi.mocked(getProjectProperties).mockRejectedValueOnce(new Error('down'))
        await edit()
        expect(getProjectProperties).toHaveBeenCalledTimes(1)

        // Nothing was held for the failure: the next time the reader writes, the dictionary is read again.
        await userEvent.click(screen.getByTestId('table-details-cancel'))
        await userEvent.click(screen.getByTestId('table-details-edit'))
        await settle()
        expect(getProjectProperties).toHaveBeenCalledTimes(2)
        await userEvent.click(screen.getByTestId('table-details-add'))
        expect(await screen.findByTitle('Category')).toBeInTheDocument()
    })

    it('shows nothing of another table while the module is being read again', async () => {
        const view = await draw()
        expect(screen.getByText('Says hello')).toBeInTheDocument()

        // The reader moved to another table before the module was read again: what was read for the first one
        // is not shown under the second one's name.
        view.rerender(panel({ tableId: 'table-2', listed: false }))
        await settle()
        expect(screen.queryByText('Says hello')).not.toBeInTheDocument()
        expect(getTableDetails).toHaveBeenCalledTimes(1)
    })

    it('reads the properties again once the module was read again after a write', async () => {
        const view = await draw()
        expect(getTableDetails).toHaveBeenCalledTimes(1)

        // The module is being compiled again: what is shown stays, and nothing is asked for yet.
        view.rerender(panel({ listed: false }))
        await settle()
        expect(getTableDetails).toHaveBeenCalledTimes(1)
        expect(screen.getByText('Says hello')).toBeInTheDocument()

        // Read again: the table now says what was written on it.
        vi.mocked(getTableDetails).mockResolvedValue({
            ...DETAILS,
            groups: [{ name: 'Info', properties: [{ name: 'description', displayName: 'Description', value: 'Waves' }]}],
        })
        view.rerender(panel({ listed: true }))
        expect(await screen.findByText('Waves')).toBeInTheDocument()
        expect(getTableDetails).toHaveBeenCalledTimes(2)
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
