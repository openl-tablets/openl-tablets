import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { onTestFinished } from 'vitest'
import { ListTable, type ListTableColumn } from 'components/ListTable'
import { layOut } from 'testing/layOut'
import { afterBreaks } from 'testing/pathBreaks'

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

    // Clipped as `hidden` does, the table became the box its sticky head and the names of its lines stuck to, so they
    // scrolled away with the rows. See EPBDS-17288.
    it('clips its corners without becoming a box that scrolls, so a sticky head stays in view', () => {
        renderTable({ maxHeight: '400px' })

        expect(getComputedStyle(screen.getByTestId('rows')).overflow).toBe('clip')
    })

    it('shows no rows and only the header while there is nothing to list', () => {
        renderTable({ rows: [] })

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

        it('wraps a field path in the name of a line before a dot, and leaves a name drawn by the screen alone', () => {
            const named: ListTableColumn<Row>[] = [
                { key: 'id', title: 'Plan (quote.planName)', render: row => row.id },
                { key: 'path', title: 'Region (coverage.regionCode)', render: row => row.name },
                { key: 'drawn', title: <b>coverage.regionCode</b>, render: row => row.name },
            ]
            renderTable({ transposed: true, columns: named })

            expect(afterBreaks(screen.getByText('Plan (quote.planName)'))).toEqual(['.planName)'])
            const path = screen.getByText('Region (coverage.regionCode)')
            expect(afterBreaks(path)).toEqual(['.regionCode)'])
            expect(path).toHaveTextContent(/^Region \(coverage\.regionCode\)$/)
            expect(screen.getByText('coverage.regionCode').querySelector('wbr')).toBeNull()
        })

        // A name kept to a width of its own wrapped in a column the list gave far more room, as a table of one case
        // does. See EPBDS-17283.
        it('lets the name of a line take the room its column has, its own width asked of the column alone', () => {
            renderTable({ transposed: true })

            const name = getComputedStyle(screen.getByText('Name'))
            expect(name.minWidth).toBe('stretch')
            expect(name.width).toBe('max-content')
            expect(name.maxWidth).not.toBe('none')
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
            renderTable({ transposed: true, columns: [...columns, own] })
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
            renderTable({ transposed: true, columns: [columns[0]!] })

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
                expect(screen.queryByTestId('list-column-widths')).not.toBeInTheDocument()
            })

            // A line with a long name widened the first column, which wrapped the values and made every line taller,
            // so the line left the lines drawn, the column narrowed, and the line came back on every frame.
            // See EPBDS-17288.
            describe('keeping the widths of the columns', () => {
                /**
                 * Lays the head out and hands back what tells the list that its sizes changed. A cell is as wide as
                 * what it holds, or as its column where the column is held wider, and is read half a pixel past a
                 * column held: a browser lays the cells out to a fraction of a pixel, which the list must not take for
                 * a wider value.
                 */
                const measured = () => {
                    /** How wide the box is, and how wide what each cell of the head holds is, the names first. */
                    const drawn = { box: 800, head: [0, 0, 0] }
                    laidOut()
                    layOut(HTMLDivElement.prototype, 'clientWidth', () => drawn.box)
                    layOut(HTMLTableCellElement.prototype, 'getBoundingClientRect', function () {
                        const cell = this as HTMLTableCellElement
                        const holds = cell.closest('thead') ? drawn.head[cell.cellIndex] ?? 0 : 0
                        const col = cell.closest('table')?.querySelector('colgroup')?.children[cell.cellIndex]
                        const held = Number.parseFloat((col as HTMLElement | undefined)?.style.width ?? '')
                        const width = Number.isNaN(held) ? holds : Math.max(held, holds) + 0.5
                        return () => ({ width }) as DOMRect
                    })
                    const told: ResizeObserverCallback[] = []
                    vi.stubGlobal('ResizeObserver', class {
                        constructor(callback: ResizeObserverCallback) {
                            told.push(callback)
                        }

                        observe = vi.fn()
                        unobserve = vi.fn()
                        disconnect = vi.fn()
                    })
                    // The list holds the widths it is told of before the next frame is laid out.
                    return async (box: number, ...head: number[]) => {
                        act(() => {
                            drawn.box = box
                            drawn.head = head
                            told.forEach(callback => callback([], {} as ResizeObserver))
                        })
                        await act(() => new Promise<void>(resolve => requestAnimationFrame(() => resolve())))
                    }
                }

                /** The widths the columns keep, the names first. */
                const widths = () => [...screen.getByTestId('list-column-widths').children]
                    .map(col => (col as HTMLElement).style.width)

                /** The list of the tall lines, drawn as they scroll, of these lines and these rows. */
                const list = (lineColumns = tall, listed = rows) => (
                    <ListTable<Row>
                        columnsInSight
                        transposed
                        columns={lineColumns}
                        data-testid="rows"
                        maxHeight={`${VIEW}px`}
                        rowKey={row => row.id}
                        rows={listed}
                    />
                )

                const renderLines = async () => {
                    const view = render(list())
                    await waitFor(() => expect(lines()).toContain(0))
                    return view
                }

                it('keeps the widest width each column was drawn at, so a line going out of sight leaves it as it is',
                    async () => {
                        const resized = measured()
                        await renderLines()

                        await resized(800, 572.25, 300, 300)
                        expect(widths()).toEqual(['572.25px', '300px', '300px'])

                        await resized(800, 89, 300, 300)
                        expect(widths()).toEqual(['572.25px', '300px', '300px'])
                    })

                // Taken up to the next whole pixel, every column held widened a pixel on every frame on a screen of
                // two device pixels to one, until the names took the whole box.
                it('leaves a column as it is while its cells are read a fraction of a pixel past it', async () => {
                    const resized = measured()
                    await renderLines()

                    for (let frame = 0; frame < 5; frame++) {
                        await resized(800, 572.25, 300, 300)
                    }

                    expect(widths()).toEqual(['572.25px', '300px', '300px'])
                })

                it('lets the rows share the box anew once the names widen, so the list keeps to the box', async () => {
                    const resized = measured()
                    await renderLines()
                    await resized(800, 122, 413, 413)

                    await resized(800, 572, 214, 214)

                    expect(widths()).toEqual(['572.5px', '214px', '214px'])
                })

                it('lets the rows share the box anew once a row outgrows its column, so the list keeps to the box',
                    async () => {
                        const resized = measured()
                        await renderLines()
                        await resized(800, 122, 413, 413)

                        await resized(800, 122, 600, 200)

                        const [name, first, second] = widths().map(width => Number.parseFloat(width))
                        expect(name).toBe(122)
                        expect(first).toBeGreaterThanOrEqual(600)
                        expect(second).toBe(200)
                    })

                // Shared anew from the values in sight, a column a value out of sight had widened narrowed again, and
                // two such values took the width from each other as the reader scrolled between them.
                it('keeps the width a row needed while another row outgrows its column', async () => {
                    const resized = measured()
                    await renderLines()
                    await resized(800, 122, 413, 413)
                    await resized(800, 122, 600, 200)

                    await resized(800, 122, 200, 600)

                    const [, first, second] = widths().map(width => Number.parseFloat(width))
                    expect(first).toBeGreaterThanOrEqual(600)
                    expect(second).toBeGreaterThanOrEqual(600)
                })

                it('starts the widths anew when the box changes its width', async () => {
                    const resized = measured()
                    await renderLines()
                    await resized(800, 572, 300, 300)

                    await resized(600, 200, 200, 200)

                    expect(widths()).toEqual(['200px', '200px', '200px'])
                })

                it('starts the widths anew for other rows', async () => {
                    const resized = measured()
                    const { rerender } = await renderLines()
                    await resized(800, 572, 300, 300)

                    rerender(list(tall, [rows[1]!, { id: '3', name: 'Third' }]))

                    expect(widths()).toEqual(['', '', ''])
                })

                it('starts the widths anew for other lines, as their names in another language', async () => {
                    const resized = measured()
                    const { rerender } = await renderLines()
                    await resized(800, 572, 300, 300)

                    rerender(list([...tall]))

                    expect(widths()).toEqual(['', '', ''])
                })

                it('holds nothing once the list is gone before the widths it was told of are held', async () => {
                    const resized = measured()
                    const { unmount } = await renderLines()
                    const cancelled = vi.spyOn(window, 'cancelAnimationFrame')
                    onTestFinished(() => cancelled.mockRestore())

                    const held = resized(800, 572, 300, 300)
                    unmount()
                    await held

                    expect(cancelled).toHaveBeenCalled()
                })
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
