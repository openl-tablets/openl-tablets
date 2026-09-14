import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ListTable, type ListTableColumn } from 'components/ListTable'

interface Row {
    id: string
    name: string
}

const rows: Row[] = [{ id: '1', name: 'First' }, { id: '2', name: 'Second' }]

const columns: ListTableColumn<Row>[] = [
    { key: 'id', fit: true, title: 'ID', render: row => row.id },
    { key: 'name', title: 'Name', render: row => row.name },
]

const renderTable = (over: Partial<React.ComponentProps<typeof ListTable<Row>>> = {}) => render(
    <ListTable<Row> columns={columns} data-testid="rows" rowKey={row => row.id} rows={rows} {...over} />
)

describe('ListTable', () => {
    it('shows a header of column names and a row of every entry', () => {
        renderTable()

        const table = screen.getByTestId('rows')
        expect(table.querySelectorAll('thead th')).toHaveLength(2)
        expect(table).toHaveTextContent('ID')
        expect(table).toHaveTextContent('Name')
        expect(table.querySelectorAll('tbody tr')).toHaveLength(2)
        expect(table).toHaveTextContent('Second')
    })

    it('gives the row that was clicked to the screen that owns it', async () => {
        const onRowClick = vi.fn()
        renderTable({ onRowClick })

        await userEvent.click(screen.getByText('Second'))

        expect(onRowClick).toHaveBeenCalledWith(rows[1])
    })

    it('gives a row the keyboard reached to the screen that owns it', async () => {
        const onRowClick = vi.fn()
        renderTable({ onRowClick })

        // A row that is chosen takes focus, so Enter and Space reach it.
        const row = screen.getByText('Second').closest('tr')!
        expect(row).toHaveAttribute('tabindex', '0')
        row.focus()
        await userEvent.keyboard('{Enter}')
        await userEvent.keyboard(' ')

        expect(onRowClick).toHaveBeenCalledTimes(2)
        expect(onRowClick).toHaveBeenLastCalledWith(rows[1])
    })

    it('leaves a row that is only read out of the keyboard order', () => {
        renderTable()

        expect(screen.getByText('Second').closest('tr')).not.toHaveAttribute('tabindex')
    })

    it('shows no rows and only the header while there is nothing to list', () => {
        renderTable({ rows: []})

        const table = screen.getByTestId('rows')
        expect(table.querySelectorAll('thead th')).toHaveLength(2)
        expect(table.querySelectorAll('tbody tr')).toHaveLength(0)
    })
})
