import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { RawTableGrid } from 'components/RawTableGrid'
import { paperToken } from 'styles/paper'
import { inLook, type TableColours } from 'styles/tableColours'
import { renderInTheme } from 'testing/theme'
import type { RawTableCell, RawTableCellBorder } from 'types/tables'

/** A colour as a style writes it back once the browser has read it. */
const written = (colour: string): string => {
    const probe = document.createElement('i')
    probe.style.color = colour
    return probe.style.color
}

const rows: RawTableCell[][] = [
    [
        { cell: 'A1', value: 'Datatype Person', colspan: 2 },
        { covered: true },
    ],
    [
        { cell: 'A2', value: 'String', style: { background: '#ffff00', bold: true } },
        { cell: 'B2', value: 'name' },
    ],
]

const computed: RawTableCell[][] = [[{ cell: 'A1', value: 3, formula: '=1+2' }, { cell: 'B1', value: 'plain' }]]

describe('RawTableGrid', () => {
    it('draws the cells with their merges, leaving the covered ones out', () => {
        render(<RawTableGrid rows={rows} testId="grid" />)

        const cells = screen.getByTestId('grid').querySelectorAll('td')
        expect(cells).toHaveLength(3)
        expect(cells[0]).toHaveAttribute('colspan', '2')
        expect(cells[0]).toHaveAttribute('data-cell', 'A1')
        expect(cells[0]).toHaveTextContent('Datatype Person')
        expect(cells[1]).toHaveTextContent('String')
    })

    // A row whose cells all run on into the next one would otherwise fold away, and a1 would read as covering
    // one rule while a2 covered two.
    it('stands every row at least one line tall, a row of cells merged into the next one included', () => {
        const staggered: RawTableCell[][] = [
            [{ cell: 'A1', value: 'a1', rowspan: 2 }, { cell: 'B1', value: 'false' }, { cell: 'C1', value: 'AAA' }],
            [{ covered: true }, { cell: 'B2', value: 'true', rowspan: 3 }, { cell: 'C2', value: 'BBB', rowspan: 3 }],
            [{ cell: 'A3', value: 'a2', rowspan: 2 }, { covered: true }, { covered: true }],
            [{ covered: true }, { covered: true }, { covered: true }],
            [{ cell: 'A5', value: 'a3' }, { cell: 'B5', value: 'false' }, { cell: 'C5', value: 'CCC' }],
        ]
        render(<RawTableGrid rows={staggered} testId="grid" />)

        const heights = [...screen.getByTestId('grid').querySelectorAll('tr')].map(line => getComputedStyle(line).height)
        expect(heights).toHaveLength(5)
        // The second row has no cell of its own height, the fourth no cell at all: both are given the same line
        // as the others.
        expect(new Set(heights).size).toBe(1)
        expect(Number.parseFloat(heights[0]!)).toBeGreaterThan(0)
    })

    it('numbers the lines of data down the side of a table written the usual way round', () => {
        const table: RawTableCell[][] = [
            [{ cell: 'A1', value: 'Test greeting greetingTest' }, { covered: true }],
            [{ cell: 'A2', value: 'name' }, { cell: 'B2', value: '_res_' }],
            [{ cell: 'A3', value: 'John' }, { cell: 'B3', value: 'Hi, John' }],
            [{ cell: 'A4', value: 'Mary' }, { cell: 'B4', value: 'Hello, Mary' }],
        ]

        render(<RawTableGrid layout={{ firstDataLine: 2 }} rows={table} testId="grid" />)

        // The two lines of headings carry no number; the cases below them are counted from one.
        expect(screen.getAllByTestId('table-line-number').map(cell => cell.textContent)).toEqual(['1', '2'])
    })

    it('numbers the lines of data across the top of a table written the other way round', () => {
        const table: RawTableCell[][] = [
            [{ cell: 'A1', value: 'Test greeting greetingTest' }, { covered: true }, { covered: true }],
            [{ cell: 'A2', value: 'name' }, { cell: 'B2', value: 'John' }, { cell: 'C2', value: 'Mary' }],
            [{ cell: 'A3', value: '_res_' }, { cell: 'B3', value: 'Hi' }, { cell: 'C3', value: 'Hello' }],
        ]

        render(<RawTableGrid layout={{ firstDataLine: 1, transposed: true }} rows={table} testId="grid" />)

        expect(screen.getAllByTestId('table-line-number').map(cell => cell.textContent)).toEqual(['1', '2'])
    })

    it('numbers nothing on a table that says nothing about its lines', () => {
        render(<RawTableGrid rows={rows} testId="grid" />)

        expect(screen.queryByTestId('table-line-number')).not.toBeInTheDocument()
    })

    it('draws a number at the precision Excel keeps, without the binary noise of the double behind it', () => {
        const table: RawTableCell[][] = [[
            { cell: 'A1', value: 1.1500000000000001 },
            { cell: 'B1', value: 1.4500000000000002 },
            { cell: 'C1', value: -1.1500000000000001 },
            { cell: 'D1', value: 0.1 + 0.2 },
            { cell: 'E1', value: 434.99999999999994 },
            { cell: 'F1', value: 1e-7 },
            { cell: 'G1', value: 1234567890123456 },
            { cell: 'H1', value: '1.1500000000000001' },
        ]]

        render(<RawTableGrid rows={table} testId="grid" />)

        const cells = Array.from(screen.getByTestId('grid').querySelectorAll('td'), cell => cell.textContent)
        expect(cells).toEqual(['1.15', '1.45', '-1.15', '0.3', '435', '1e-7', '1234567890123456', '1.1500000000000001'])
    })

    it('paints the cell the way the workbook has it', () => {
        render(<RawTableGrid rows={rows} testId="grid" />)

        const styled = screen.getByTestId('grid').querySelectorAll('td')[1] as HTMLElement
        expect(styled.style.background).toContain('rgb(255, 255, 0)')
        expect(styled.style.fontWeight).toBe('bold')
    })

    it('lines a cell up from top to bottom as the workbook does, its middle named as CSS names it', () => {
        const table: RawTableCell[][] = [[
            { cell: 'A1', value: 'middle', rowspan: 2, style: { valign: 'center' } },
            { cell: 'B1', value: 'top', style: { valign: 'top' } },
        ], [
            { covered: true },
            { cell: 'B2', value: 'bottom', style: { bold: true } },
        ]]

        render(<RawTableGrid rows={table} testId="grid" />)

        const cells = screen.getByTestId('grid').querySelectorAll('td')
        expect(cells[0]).toHaveStyle({ verticalAlign: 'middle' })
        expect(cells[1]).toHaveStyle({ verticalAlign: 'top' })
        // A cell aligned to the bottom, Excel's default, names no alignment and keeps the one of the grid.
        expect((cells[2] as HTMLElement).style.verticalAlign).toBe('')
    })

    describe('in a dark theme', () => {
        const drawDark = (table: RawTableCell[][]) => renderInTheme(
            <RawTableGrid layout={{ firstDataLine: 1 }} rows={table} testId="grid" />,
            { theme: 'dracula', mode: 'dark' }
        )

        it('writes the cells in black on white, as Excel does, so a cell its author filled stays readable', () => {
            const filled: RawTableCell = { cell: 'B1', value: 'filled', style: { background: '#00ffff' } }
            drawDark([[{ cell: 'A1', value: 'plain' }, filled]])

            const paper = paperToken()
            expect(screen.getByTestId('grid'))
                .toHaveStyle({ backgroundColor: paper.colorBgContainer, color: paper.colorText })
            // A cyan cell keeps its fill and is written in the ink of the paper, not in the light text of the theme.
            expect(screen.getByText('filled')).toHaveStyle({ backgroundColor: '#00ffff', color: paper.colorText })
        })

        it('keeps a font colour the workbook gave a cell', () => {
            drawDark([[{ cell: 'A1', value: 'red', style: { color: '#ff0000' } }]])

            expect(screen.getByText('red')).toHaveStyle({ color: '#ff0000' })
        })

        it('numbers the lines in the margin of the screen, which follows the theme', () => {
            drawDark([[{ cell: 'A1', value: 'head' }], [{ cell: 'A2', value: 'case' }]])

            const margin = screen.getByTestId('table-line-number').closest('td')
            expect(margin).not.toHaveStyle({ backgroundColor: paperToken().colorBgContainer })
        })
    })

    it('draws the borders the workbook has, and the grid line on every other side', () => {
        const table: RawTableCell[][] = [[{
            cell: 'A1',
            value: 'x',
            style: {
                color: '#ffffff',
                border: { bottom: { style: 'solid', width: 2, color: '#ff0000' }, top: { style: 'dashed', width: 1 } },
            },
        }]]

        render(<RawTableGrid rows={table} testId="grid" />)

        const cell = screen.getByTestId('grid').querySelector('td') as HTMLElement
        expect(cell.style.borderBottom).toBe('2px solid rgb(255, 0, 0)')
        // A side without a colour of its own is drawn in the ink of the paper, as Excel draws it, and not in the
        // white the text of the cell has.
        expect(cell.style.borderTop).toBe(`1px dashed ${written(paperToken().colorText)}`)
        expect(cell.style.borderLeft).toBe('')
    })

    it('draws the line of the grid two cells share once, by the upper or the left one', () => {
        const line: RawTableCellBorder = { right: { style: 'solid', width: 1 } }
        const table: RawTableCell[][] = [
            [{ cell: 'A1', value: 'R1', style: { border: line } }, { cell: 'B1', value: 'Young', rowspan: 2 }],
            [{ cell: 'A2', value: 'R2', style: { border: line } }, { covered: true }],
            [{ cell: 'A3', value: 'R3' }, { cell: 'B3', value: 'Senior' }],
        ]

        render(<RawTableGrid rows={table} testId="grid" />)

        const cellAt = (address: string) => screen.getByTestId('grid').querySelector(`[data-cell="${address}"]`)
        // Along the edge of the table a cell draws the line of the grid on that side as well.
        expect(cellAt('A1')).toHaveStyle({ borderTopStyle: 'solid', borderLeftStyle: 'solid' })
        expect(cellAt('A3')).toHaveStyle({ borderLeftStyle: 'solid', borderBottomStyle: 'solid' })
        // The merged cell is laid out before R2, so a line of the grid on its left would be drawn over the line
        // R2 has on its right.
        expect(cellAt('B1')).toHaveStyle({ borderTopStyle: 'solid', borderRightStyle: 'solid' })
        expect(cellAt('B1')).not.toHaveStyle({ borderLeftStyle: 'solid' })
        expect(cellAt('B3')).toHaveStyle({ borderRightStyle: 'solid', borderBottomStyle: 'solid' })
        expect(cellAt('B3')).not.toHaveStyle({ borderTopStyle: 'solid' })
        expect(cellAt('B3')).not.toHaveStyle({ borderLeftStyle: 'solid' })
    })

    it('draws a line naming no colour in the ink of the paper, and in the grey of a muted cell', () => {
        const line: RawTableCellBorder = { bottom: { style: 'solid', width: 1 } }
        const table: RawTableCell[][] = [[
            { cell: 'A1', value: 'filled', style: { background: '#ddebf7', border: line } },
            { cell: 'B1', value: 'muted', style: { color: '#ff0000', border: line } },
        ]]

        render(<RawTableGrid decorate={cell => ({ muted: cell.cell === 'B1' })} rows={table} testId="grid" />)

        const [filled, muted] = Array.from(screen.getByTestId('grid').querySelectorAll('td')) as HTMLElement[]
        // The line lies on the paper of the workbook, fill or not, as the text of the table does.
        expect(filled?.style.borderBottom).toBe(`1px solid ${written(paperToken().colorText)}`)
        // A muted cell leaves the colour out, so its line is drawn in the grey its text is drawn in.
        expect(muted?.style.borderBottom).toBe('1px solid')
    })

    it('draws the pieces of a text in the fonts the workbook gives them', () => {
        const table: RawTableCell[][] = [[{
            cell: 'A1',
            value: 'Datatype Person',
            runs: [
                { text: 'Datatype', style: { color: '#808080' } },
                { text: ' ' },
                { text: 'Person', style: { bold: true, strikeout: true, fontFamily: 'Calibri', fontSize: 14 } },
            ],
        }]]

        render(<RawTableGrid rows={table} testId="grid" />)

        const cell = screen.getByTestId('grid').querySelector('td') as HTMLElement
        expect(cell).toHaveTextContent('Datatype Person')
        const pieces = cell.querySelectorAll('span')
        expect(pieces).toHaveLength(3)
        expect((pieces[0] as HTMLElement).style.color).toBe('rgb(128, 128, 128)')
        // A run with a font of its own names every attribute of it: what it leaves out is at its default.
        expect((pieces[0] as HTMLElement).style.fontWeight).toBe('normal')
        expect(pieces[2]).toHaveStyle({ color: paperToken().colorText })
        expect((pieces[1] as HTMLElement).style.fontWeight).toBe('')
        expect((pieces[2] as HTMLElement).style.fontWeight).toBe('bold')
        expect((pieces[2] as HTMLElement).style.textDecoration).toBe('line-through')
        // The font and the size a table theme gives a piece are drawn on the piece itself.
        expect((pieces[2] as HTMLElement).style.fontFamily).toBe('"Calibri", sans-serif')
        expect((pieces[2] as HTMLElement).style.fontSize).toBe('14pt')
    })

    it('draws the look of a table theme a read reports as the style of a cell', () => {
        const table: RawTableCell[][] = [[{
            cell: 'A1',
            value: 'Datatype Person',
            style: { background: '#b4c6e7', fontFamily: 'Franklin Gothic Book', fontSize: 10, source: 'theme' },
            runs: [{ text: 'Datatype', style: { color: '#808080', source: 'theme' } }, { text: ' Person' }],
        }]]

        render(<RawTableGrid rows={table} testId="grid" />)

        const themed = screen.getByTestId('grid').querySelector('td') as HTMLElement
        expect(themed.style.background).toContain('rgb(180, 198, 231)')
        expect(themed.style.fontFamily).toBe('"Franklin Gothic Book", sans-serif')
        expect(themed.style.fontSize).toBe('10pt')
        expect(themed.querySelectorAll('span')).toHaveLength(2)
    })

    describe('in the colours of the application', () => {
        const colours: TableColours = {
            keyed: {
                'base.style.color': '#dcdcdc',
                'base.header.keyword.color': '#adadad',
                'datatype.name.background': '#15325b',
                'spreadsheet.resultRow.border.top.color': '#424242',
            },
            paper: {
                background: '#141414',
                text: '#dcdcdc',
                grid: '#303030',
                link: '#1668dc',
                linkHover: '#3c89e8',
                note: '#dc4446',
            },
        }
        // The table editor draws a table in the look as the look recolours it, on the paper of the look.
        const drawIn = (table: RawTableCell[][]) => render(
            <RawTableGrid paper={colours.paper} rows={inLook(table, colours)} testId="grid" />
        )

        it('lays the table on the ground of the application, written in its text', () => {
            drawIn(rows)

            expect(screen.getByTestId('grid')).toHaveStyle({ backgroundColor: '#141414', color: '#dcdcdc' })
            expect(screen.getByTestId('grid').querySelector('td')).toHaveStyle({ borderRightColor: '#303030' })
        })

        it('draws each colour of the table theme in the colour of the application the key it is set at takes', () => {
            drawIn([[{
                cell: 'A1',
                value: 'name',
                style: {
                    background: '#ddebf7',
                    backgroundKey: 'datatype.name.background',
                    color: '#000000',
                    colorKey: 'base.style.color',
                    bold: true,
                    fontFamily: 'Franklin Gothic Book',
                    fontSize: 10,
                    border: {
                        bottom: {
                            style: 'solid',
                            width: 1,
                            color: '#d9d9d9',
                            colorKey: 'spreadsheet.resultRow.border.top.color',
                        },
                        top: { style: 'solid', width: 1 },
                    },
                    source: 'theme',
                },
            }, {
                cell: 'B1',
                value: 'own',
                style: { background: '#ff0000', color: '#00ff00', colorKey: 'corporate.values.color', source: 'theme' },
            }]])

            const [keyed, own] = Array.from(screen.getByTestId('grid').querySelectorAll('td')) as HTMLElement[]
            expect(keyed?.style.background).toContain('rgb(21, 50, 91)')
            expect(keyed?.style.color).toBe('rgb(220, 220, 220)')
            // A line is coloured by the key of its side in the file, whichever side it is drawn on.
            expect(keyed?.style.borderBottom).toBe('1px solid rgb(66, 66, 66)')
            // A line naming no colour is drawn in the text of the application, as Excel draws it in its ink.
            expect(keyed?.style.borderTop).toBe(`1px solid ${written('#dcdcdc')}`)
            // The table theme still decides the bold, while the text is set in the font of the application.
            expect(keyed?.style.fontWeight).toBe('bold')
            expect(keyed?.style.fontFamily).toBe('')
            expect(keyed?.style.fontSize).toBe('')
            // A colour without a key, or of a key the application has no colour for, is the table theme's own.
            expect(own?.style.background).toContain('rgb(255, 0, 0)')
            expect(own?.style.color).toBe('rgb(0, 255, 0)')
        })

        it('leaves a cell the table theme does not draw in the colours of the workbook', () => {
            drawIn([[{ cell: 'A1', value: 'own', style: { background: '#ffff00', color: '#ff0000' } }]])

            const cell = screen.getByTestId('grid').querySelector('td') as HTMLElement
            expect(cell.style.background).toContain('rgb(255, 255, 0)')
            expect(cell.style.color).toBe('rgb(255, 0, 0)')
        })

        it('draws the pieces of a text in the colours of the application their keys take, in its font', () => {
            drawIn([[{
                cell: 'A1',
                value: 'Datatype Person',
                style: { fontFamily: 'Franklin Gothic Book', fontSize: 10, source: 'theme' },
                runs: [
                    {
                        text: 'Datatype',
                        style: {
                            color: '#808080',
                            colorKey: 'base.header.keyword.color',
                            fontFamily: 'Arial',
                            source: 'theme',
                        },
                    },
                    { text: ' ' },
                    { text: 'Person', style: { bold: true, fontSize: 10, source: 'theme' } },
                ],
            }]])

            const pieces = screen.getByTestId('grid').querySelectorAll('span')
            expect((pieces[0] as HTMLElement).style.color).toBe('rgb(173, 173, 173)')
            expect((pieces[0] as HTMLElement).style.fontFamily).toBe('')
            // A piece naming no colour is drawn in the text of the application.
            expect((pieces[2] as HTMLElement).style.color).toBe('rgb(220, 220, 220)')
            expect((pieces[2] as HTMLElement).style.fontSize).toBe('')
        })
    })

    it('leaves out the Excel background of a cell the screen paints itself', () => {
        render(
            <RawTableGrid
                decorate={cell => (cell.cell === 'A2' ? { className: 'marked', painted: true } : undefined)}
                rows={rows}
                testId="grid"
            />
        )

        const marked = screen.getByTestId('grid').querySelectorAll('td')[1] as HTMLElement
        expect(marked.className).toContain('marked')
        expect(marked.style.background).toBe('')
        // The rest of its Excel styling stays.
        expect(marked.style.fontWeight).toBe('bold')
    })

    it('draws a muted cell in grey, at the brightness of its own colour', () => {
        render(
            <RawTableGrid
                decorate={cell => (cell.cell === 'A2' ? { muted: true } : undefined)}
                rows={rows}
                testId="grid"
            />
        )

        // Yellow averages to 170, and four fifths of that is the grey it steps back to.
        const muted = screen.getByTestId('grid').querySelectorAll('td')[1] as HTMLElement
        expect(muted.style.background).toContain('rgb(136, 136, 136)')
        // The cell keeps everything the colour does not decide.
        expect(muted.style.fontWeight).toBe('bold')
    })

    it('leaves an unfilled cell unfilled when it is muted', () => {
        render(
            <RawTableGrid
                decorate={() => ({ muted: true })}
                rows={rows}
                testId="grid"
            />
        )

        const plain = screen.getByTestId('grid').querySelectorAll('td')[2] as HTMLElement
        expect(plain.style.background).toBe('')
    })

    it('shows the note a reader left on a cell, and marks the cell carrying it', async () => {
        const noted: RawTableCell[][] = [[
            { cell: 'A1', value: 'Premium', comment: 'Agreed with legal\non 3 May' },
            { cell: 'B1', value: 'plain' },
        ]]
        render(<RawTableGrid rows={noted} testId="grid" />)

        const cells = screen.getByTestId('grid').querySelectorAll('td')
        // The cell wearing the note is marked; the one beside it is not.
        expect(cells[0]?.className).not.toEqual(cells[1]?.className)

        await userEvent.hover(cells[0] as Element)

        expect(await screen.findByText(/Agreed with legal/)).toBeInTheDocument()
    })

    it('is a grid that takes the keys only where the keyboard moves around it', async () => {
        const onKeyDown = vi.fn()
        const { rerender } = render(<RawTableGrid rows={rows} testId="grid" />)
        expect(screen.getByRole('table')).toBe(screen.getByTestId('grid'))
        expect(screen.queryByRole('grid')).not.toBeInTheDocument()
        expect(screen.getByTestId('grid')).not.toHaveAttribute('tabindex')

        rerender(<RawTableGrid onKeyDown={onKeyDown} rows={rows} testId="grid" />)
        const grid = screen.getByRole('grid')
        expect(grid).toBe(screen.getByTestId('grid'))
        expect(grid).toHaveAttribute('tabindex', '-1')

        grid.focus()
        await userEvent.keyboard('{ArrowDown}')
        expect(onKeyDown).toHaveBeenCalledWith(expect.objectContaining({ key: 'ArrowDown' }))
    })

    it('draws an empty table without a row', () => {
        render(<RawTableGrid rows={[]} testId="grid" />)

        expect(screen.getByTestId('grid').querySelectorAll('td')).toHaveLength(0)
    })

    it('draws what a cell computed, and the formula behind it when the screen asks', () => {
        const { rerender } = render(<RawTableGrid rows={computed} testId="grid" />)
        expect(screen.getByTestId('grid').querySelectorAll('td')[0]).toHaveTextContent('3')

        rerender(<RawTableGrid formulas rows={computed} testId="grid" />)

        const cells = screen.getByTestId('grid').querySelectorAll('td')
        expect(cells[0]).toHaveTextContent('=1+2')
        // A cell written as a plain value has no formula to show, so it reads the same either way.
        expect(cells[1]).toHaveTextContent('plain')
    })

    it('leaves the formula unmarked: what the compiler knows describes the value, not the formula', () => {
        const marked: RawTableCell[][] = [[{
            cell: 'A1',
            value: 'Premium',
            formula: '=B1&C1',
            metaInfo: { usages: [{ start: 0, end: 7, description: 'Rules Double Premium()', kind: 'rule' }]},
        }]]

        const { rerender } = render(<RawTableGrid rows={marked} testId="grid" />)
        expect(screen.getByTestId('cell-usage-0')).toHaveTextContent('Premium')

        rerender(<RawTableGrid formulas rows={marked} testId="grid" />)

        // The ranges are measured over the value, so on the formula they would mark whatever happened to be
        // at those positions — and lead somewhere else entirely.
        expect(screen.queryByTestId('cell-usage-0')).toBeNull()
        expect(screen.getByTestId('grid').querySelectorAll('td')[0]).toHaveTextContent('=B1&C1')
    })
})
