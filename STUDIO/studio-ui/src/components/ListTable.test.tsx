import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { onTestFinished } from 'vitest'
import { ListTable, type ListTableColumn } from 'components/ListTable'
import { layOut } from 'testing/layOut'

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

    it('keeps a click in a cell of its own from picking the row', async () => {
        const onRowClick = vi.fn()
        const own: ListTableColumn<Row> = { key: 'note', title: 'Note', ownClicks: true, render: row => `note ${row.id}` }
        renderTable({ columns: [...columns, own], onRowClick })

        await userEvent.click(screen.getByText('note 2'))
        expect(onRowClick).not.toHaveBeenCalled()

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

    describe('transposed', () => {
        it('shows a column to a line under its name, and a row to a column under the first column', () => {
            renderTable({ transposed: true })

            const table = screen.getByTestId('rows')
            expect([...table.querySelectorAll('thead th')].map(cell => cell.textContent)).toEqual(['ID', '1', '2'])
            const lines = table.querySelectorAll('tbody tr')
            expect(lines).toHaveLength(1)
            expect([...lines[0]!.children].map(cell => cell.textContent)).toEqual(['Name', 'First', 'Second'])
        })

        it('keeps a click in a cell of its own to the cell', async () => {
            const onClick = vi.fn()
            const own: ListTableColumn<Row> = {
                key: 'note',
                title: 'Note',
                ownClicks: true,
                render: row => <button onClick={onClick} type="button">{`note ${row.id}`}</button>,
            }
            const parent = vi.fn()
            renderTable({ transposed: true, columns: [...columns, own]})
            // React handles the click on the element it renders into, so the page above it hears what is not kept.
            document.body.addEventListener('click', parent)
            onTestFinished(() => document.body.removeEventListener('click', parent))

            await userEvent.click(screen.getByText('note 2'))

            expect(onClick).toHaveBeenCalledTimes(1)
            expect(parent).not.toHaveBeenCalled()
        })

        it('lets the keyboard reach the box its lines scroll in', () => {
            const { rerender } = renderTable({ transposed: true, maxHeight: '400px' })
            expect(screen.getByTestId('rows').parentElement).toHaveAttribute('tabindex', '0')

            rerender(<ListTable<Row> transposed columns={columns} data-testid="rows" rowKey={row => row.id} rows={rows} />)

            expect(screen.getByTestId('rows').parentElement).not.toHaveAttribute('tabindex')
        })

        it('shows only the head while there are no other columns', () => {
            renderTable({ transposed: true, columns: [columns[0]!]})

            const table = screen.getByTestId('rows')
            expect(table.querySelectorAll('thead th')).toHaveLength(3)
            expect(table.querySelectorAll('tbody tr')).toHaveLength(0)
        })

        describe('drawing only the lines in sight', () => {
            /** How tall jsdom is told a line and the list are: a line as tall as one not measured yet is taken to be. */
            const LINE = 40
            const VIEW = 400

            const tall: ListTableColumn<Row>[] = [
                columns[0]!,
                ...Array.from({ length: 300 }, (unused, index) => ({
                    key: `c${index}`,
                    title: `Field ${index}`,
                    render: (row: Row) => `${row.id}:${index}`,
                })),
            ]

            const laidOut = (height = VIEW) => {
                layOut(HTMLDivElement.prototype, 'clientHeight', () => height)
                layOut(HTMLTableCellElement.prototype, 'offsetHeight', () => LINE)
            }

            const lines = () => [...screen.getByTestId('rows').querySelectorAll<HTMLElement>('tbody th[data-index]')]
                .map(line => Number(line.dataset['index']))

            it('draws the lines in sight and a few below them, keeping the room of the rest', async () => {
                laidOut()
                renderTable({ columns: tall, columnsInSight: true, maxHeight: `${VIEW}px`, transposed: true })

                await waitFor(() => expect(lines()).toContain(0))
                expect(lines().length).toBeLessThan(30)
                expect(lines()).not.toContain(299)
                expect(Number.parseFloat(screen.getByTestId('list-lines-after').style.height))
                    .toBeGreaterThan(200 * LINE)
                expect(screen.queryByTestId('list-lines-before')).not.toBeInTheDocument()
            })

            it('draws the lines scrolled down to, and lets go of the ones scrolled past', async () => {
                laidOut()
                renderTable({ columns: tall, columnsInSight: true, maxHeight: `${VIEW}px`, transposed: true })
                await waitFor(() => expect(lines()).toContain(0))
                const scroller = screen.getByTestId('rows').parentElement!
                let top = 0
                Object.defineProperty(scroller, 'scrollTop', { configurable: true, get: () => top })

                act(() => {
                    top = 250 * LINE
                    scroller.dispatchEvent(new Event('scroll'))
                })

                await waitFor(() => expect(lines()).toContain(252))
                expect(lines()).not.toContain(0)
                expect(screen.getByText('Field 252')).toBeInTheDocument()
                expect(screen.getByTestId('list-lines-before')).toBeInTheDocument()
            })

            it('draws every line where the list has no layout to measure', () => {
                laidOut(0)
                renderTable({ columns: tall, columnsInSight: true, transposed: true })

                expect(lines()).toHaveLength(300)
                expect(screen.queryByTestId('list-lines-after')).not.toBeInTheDocument()
            })
        })
    })

    // A test table comparing 2559 values drew 51 000 cells for 20 cases and took the browser down. See EPBDS-17091.
    describe('drawing only the columns in sight', () => {
        /** How wide jsdom is told a column and the list are: a column as wide as one not measured yet is taken to be. */
        const COLUMN = 120
        const VIEW = 600

        const wide: ListTableColumn<Row>[] = Array.from({ length: 200 }, (unused, index) => ({
            key: `c${index}`,
            title: `Column ${index}`,
            render: row => `${row.id}:${index}`,
        }))


        /** Lays the list out as wide as the screen shows it, its columns as wide as their headers. */
        const laidOut = (width = VIEW) => {
            layOut(HTMLDivElement.prototype, 'clientWidth', () => width)
            layOut(HTMLTableCellElement.prototype, 'offsetWidth', () => COLUMN)
        }

        const headers = () => [...screen.getByTestId('rows').querySelectorAll<HTMLElement>('thead th[data-index]')]
            .map(header => Number(header.dataset['index']))

        it('draws the columns in sight and a few beside them, keeping the room of the rest', async () => {
            laidOut()
            renderTable({ columns: wide, columnsInSight: true })

            await waitFor(() => expect(headers()).toContain(0))
            expect(headers().length).toBeLessThan(20)
            expect(headers()).not.toContain(199)
            // Every row draws the same columns as the header, and keeps the room of the columns left out after them.
            expect(screen.getAllByText(/^1:\d+$/)).toHaveLength(headers().length)
            const rooms = screen.getAllByTestId('list-cells-after')
            expect(rooms).toHaveLength(rows.length)
            expect(Number.parseFloat(rooms[0]!.style.width)).toBeGreaterThan(100 * COLUMN)
            expect(screen.queryByTestId('list-columns-before')).not.toBeInTheDocument()
        })

        it('draws the columns scrolled across to, and lets go of the ones scrolled past', async () => {
            laidOut()
            renderTable({ columns: wide, columnsInSight: true })
            await waitFor(() => expect(headers()).toContain(0))
            const scroller = screen.getByTestId('rows').parentElement!
            let left = 0
            Object.defineProperty(scroller, 'scrollLeft', { configurable: true, get: () => left })

            act(() => {
                left = 150 * COLUMN
                scroller.dispatchEvent(new Event('scroll'))
            })

            await waitFor(() => expect(headers()).toContain(152))
            expect(headers()).not.toContain(0)
            expect(screen.getByText('Column 152')).toBeInTheDocument()
            expect(screen.getByTestId('list-columns-before')).toBeInTheDocument()
        })

        it('draws every column where the list has no layout to measure', () => {
            laidOut(0)
            renderTable({ columns: wide, columnsInSight: true })

            expect(headers()).toHaveLength(200)
            expect(screen.queryByTestId('list-columns-after')).not.toBeInTheDocument()
        })
    })
})
