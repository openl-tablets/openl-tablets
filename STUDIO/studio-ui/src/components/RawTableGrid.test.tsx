import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { RawTableGrid } from 'components/RawTableGrid'
import { roleColoursOf } from 'components/RawTableGrid.styles'
import { rolesOf } from 'components/tableRoles'
import { contrastRatio, opaque } from 'styles/colorMath'
import { paperToken } from 'styles/paper'
import { LIST_PAGE_COLORS } from 'styles/listPageTheme'
import { THEME_ORDER, variantOf } from 'styles/themes'
import { hex, renderInTheme, tokenFor } from 'testing/theme'
import type { RawTableCell, RawTableCellBorder } from 'types/tables'
import { highlight } from 'utils/codeHighlight'

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

    it('indents a cell by an em a step, from the side it is aligned to', () => {
        const table: RawTableCell[][] = [[
            { cell: 'A1', value: 'C2', style: { indent: 4 } },
            { cell: 'B1', value: 'total', style: { indent: 2, align: 'right' } },
            { cell: 'C1', value: 'plain' },
        ]]

        render(<RawTableGrid rows={table} testId="grid" />)

        const [left, right, plain] = Array.from(screen.getByTestId('grid').querySelectorAll('td'))
        expect(left!.style.getPropertyValue('--cell-indent-left')).toBe('4em')
        expect(right!.style.getPropertyValue('--cell-indent-right')).toBe('2em')
        expect(right!.style.getPropertyValue('--cell-indent-left')).toBe('')
        expect(plain!.getAttribute('style')).toBeNull()
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

    it('draws a line the lower cell names under the upper cell, the wider of two lines winning', () => {
        const table: RawTableCell[][] = [
            [{ cell: 'A1', value: 'step' }, { cell: 'B1', value: 'thin', style: { border: { bottom: { width: 1 } } } }],
            [
                { cell: 'A2', value: 'heading', style: { border: { top: { style: 'solid', width: 2 } } } },
                { cell: 'B2', value: 'wide', style: { border: { top: { style: 'solid', width: 2 } } } },
            ],
        ]

        render(<RawTableGrid rows={table} testId="grid" />)

        const cellAt = (address: string) =>
            screen.getByTestId('grid').querySelector(`[data-cell="${address}"]`) as HTMLElement
        expect(cellAt('A1').style.borderBottom).toBe(`2px solid ${written(paperToken().colorText)}`)
        expect(cellAt('A2').style.borderTop).toBe('')
        expect(cellAt('B1').style.borderBottom).toBe(`2px solid ${written(paperToken().colorText)}`)
        expect(cellAt('B2').style.borderTop).toBe('')
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

    it('draws the pieces of a text in the weights and the colours the workbook gives them', () => {
        const table: RawTableCell[][] = [[{
            cell: 'A1',
            value: 'Datatype Person',
            runs: [
                { text: 'Datatype', style: { color: '#808080' } },
                { text: ' ' },
                { text: 'Person', style: { bold: true, strikeout: true } },
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
        // The text is written in the font of the application, whatever font the workbook names.
        expect((pieces[2] as HTMLElement).style.fontFamily).toBe('')
        expect((pieces[2] as HTMLElement).style.fontSize).toBe('')
    })

    describe('in the Studio look', () => {
        // A rule of one condition: its header, the titles of what it takes and gives, and a line of each.
        const rule: RawTableCell[][] = [
            [{ cell: 'B2', value: 'Rules Double Rate(Integer age)', colspan: 2, style: { background: '#ff0000' } },
                { covered: true }],
            [{ cell: 'B3', value: 'Age' }, { cell: 'C3', value: 'Rate' }],
            [{ cell: 'B4', value: '18', style: { indent: 2 } }, { cell: 'C4', value: 0.5, type: 'number' }],
            [{ cell: 'B5', value: '65' }, { cell: 'C5', value: 0.25 }],
        ]
        const roles = rolesOf({
            pos: 'B2:C5',
            regions: [
                { role: 'signature', top: 0, left: 0, bottom: 0, right: 1 },
                { role: 'inputTitle', top: 1, left: 0, bottom: 1, right: 0 },
                { role: 'outputTitle', top: 1, left: 1, bottom: 1, right: 1 },
                { role: 'value', type: 'string', top: 2, left: 0, bottom: 3, right: 0 },
                { role: 'output', type: 'string', top: 2, left: 1, bottom: 3, right: 1 },
            ],
            groups: { rows: [3], columns: []},
            signature: [
                { part: 'keyword', to: 5 },
                { part: 'type', to: 12 },
                { part: 'name', to: 17 },
                { part: 'punctuation', to: 18 },
                { part: 'parameterType', to: 25 },
                { part: 'parameterName', to: 29 },
                { part: 'punctuation', to: 30 },
            ],
        })
        const dark = tokenFor('standard', true)
        const colours = roleColoursOf(dark)
        const cellAt = (address: string) =>
            screen.getByTestId('grid').querySelector(`td[data-cell="${address}"]`) as HTMLElement
        const drawIn = (excelFormatting = false) => renderInTheme(
            <RawTableGrid roles={roles} rows={rule} testId="grid" />,
            { theme: 'standard', mode: 'dark', excelFormatting }
        )

        it('draws each cell by its role, in the colours of the theme, whatever the workbook formats it with', () => {
            drawIn()

            expect(screen.getByTestId('grid')).toHaveStyle({ backgroundColor: dark.colorBgContainer })
            expect(cellAt('B2')).toHaveAttribute('data-role', 'signature')
            expect(cellAt('B2').style.background).toBe('')
            expect(cellAt('B3')).toHaveStyle({ backgroundColor: colours.inputTitleFill })
            expect(cellAt('C3')).toHaveStyle({ backgroundColor: colours.outputTitleFill })
            expect(cellAt('C4')).toHaveStyle({ backgroundColor: colours.outputFill })
            expect(cellAt('B4')).toHaveAttribute('data-role', 'value')
        })

        it('keeps the indent of the workbook, which carries the structure of a table', () => {
            drawIn()

            expect(cellAt('B4').style.getPropertyValue('--cell-indent-left')).toBe('2em')
        })

        it('lines a title up in the middle and a number on the right', () => {
            drawIn()

            expect(cellAt('B3').style.textAlign).toBe('center')
            // The cell names its own type, which wins over the type of its region.
            expect(cellAt('C4').style.textAlign).toBe('right')
            expect(cellAt('C5').style.textAlign).toBe('')
        })

        it('writes the name of the table bold and its keyword, types and punctuation faint', () => {
            drawIn()

            const pieces = Array.from(cellAt('B2').querySelectorAll('span')) as HTMLElement[]
            expect(pieces.map(piece => piece.textContent))
                .toEqual(['Rules', ' Double', ' Rate', '(', 'Integer', ' age', ')'])
            expect(pieces[2]?.style.fontWeight).toBe('bold')
            expect(pieces[0]).toHaveStyle({ color: colours.faint })
            expect(pieces[4]).toHaveStyle({ color: colours.faint })
            expect(pieces[5]?.style.color).toBe('')
        })

        it('draws the lines of the parts of the table, and above the first line of a group', () => {
            drawIn()

            expect(cellAt('B2').style.borderTop).toBe(`1px solid ${written(dark.colorBorder)}`)
            expect(cellAt('B2').style.borderBottom).toBe(`1px solid ${written(dark.colorBorder)}`)
            // The group of the last line is set apart by a line drawn on the line above it.
            expect(cellAt('B4').style.borderBottom).toBe(`1px solid ${written(dark.colorBorderSecondary)}`)
            // The last line closes the table.
            expect(cellAt('B5').style.borderBottom).toBe(`1px solid ${written(dark.colorBorder)}`)
        })

        it('draws the formatting of the workbook with Excel Formatting on', () => {
            drawIn(true)

            expect(screen.getByTestId('grid')).toHaveStyle({ backgroundColor: paperToken().colorBgContainer })
            expect(cellAt('B2').style.background).toContain('rgb(255, 0, 0)')
            expect(cellAt('B2')).not.toHaveAttribute('data-role')
            expect(cellAt('B2').querySelectorAll('span')).toHaveLength(0)
        })

        it('draws a table read with no roles in the formatting of its workbook', () => {
            renderInTheme(<RawTableGrid rows={rule} testId="grid" />, { theme: 'standard', mode: 'dark' })

            expect(cellAt('B2').style.background).toContain('rgb(255, 0, 0)')
        })

        const VARIANTS = THEME_ORDER.flatMap(name => [false, true].map(isDarkMode => ({
            name,
            appearance: isDarkMode ? 'dark' : 'light',
            token: tokenFor(name, isDarkMode),
        })))

        it.each(VARIANTS)('keeps the text of every role readable in $name $appearance', ({ token }) => {
            const theme = roleColoursOf(token)
            const surface = token.colorBgContainer
            const contrastOn = (color: string, fill: string) => {
                const ground = opaque(fill, surface)
                return contrastRatio(opaque(color, ground), ground)
            }

            // The text of a role reads on its fill as the text of the application does on its surfaces.
            for (const fill of [surface, theme.inputTitleFill, theme.outputTitleFill, theme.outputFill]) {
                expect(contrastOn(theme.text, fill)).toBeGreaterThanOrEqual(4.5)
            }
            // A faint text reads as the secondary text of the application.
            expect(contrastOn(theme.faint, surface)).toBeGreaterThanOrEqual(3)
        })
    })

    describe('code and values in the Studio look', () => {
        // A Spreadsheet step of a formula, a TBasic-free table: the values of each type and a formula.
        const steps: RawTableCell[][] = [
            [{ cell: 'A1', value: 'Spreadsheet Double Plan()', colspan: 5 }, { covered: true }, { covered: true },
                { covered: true }, { covered: true }],
            [{ cell: 'A2', value: 'Total' }, { cell: 'B2', value: 1200, type: 'number' },
                { cell: 'C2', value: '2026-10-15', type: 'date' }, { cell: 'D2', value: true, type: 'boolean' },
                { cell: 'E2', value: '= $Rate * 18', type: 'code' }],
            [{ cell: 'A3', value: 'C1' }, { cell: 'B3', value: 'North' }, { cell: 'C3', value: 'x', formula: '=1+2' },
                { cell: 'D3', value: 'y' }, { cell: 'E3', value: 'z' }],
        ]
        const rolesOfKind = (kind?: string) => rolesOf({
            ...(kind !== undefined && { kind }),
            pos: 'A1:E3',
            regions: [
                { role: 'signature', top: 0, left: 0, bottom: 0, right: 4 },
                { role: 'key', top: 1, left: 0, bottom: 1, right: 0 },
                { role: 'value', type: 'string', top: 1, left: 1, bottom: 1, right: 4 },
                { role: 'code', top: 2, left: 0, bottom: 2, right: 0 },
                { role: 'value', type: 'string', top: 2, left: 1, bottom: 2, right: 4 },
            ],
        })
        const { syntax } = variantOf('standard', false)
        const cellAt = (address: string) =>
            screen.getByTestId('grid').querySelector(`td[data-cell="${address}"]`) as HTMLElement
        /** The colour of the piece of the text of a cell that reads as given. */
        const colourOf = (address: string, text: string) => hex([...cellAt(address).querySelectorAll('span')]
            .find(piece => piece.textContent === text)?.style.color)
        /** The colour the code editor of the theme gives a keyword. */
        const highlightedKeyword = () =>
            hex(highlight('if', 'openl', 'standard', false)?.find(piece => piece.text === 'if')?.style?.color)
        const drawIn = (rows = steps, kind?: string, formulas = false) => renderInTheme(
            <RawTableGrid formulas={formulas} roles={rolesOfKind(kind)} rows={rows} testId="grid" />,
            { theme: 'standard', mode: 'light' }
        )

        it('colours a value by its type, a date whole with its separators', () => {
            drawIn()

            expect(cellAt('B2')).toHaveStyle({ color: LIST_PAGE_COLORS.syntaxNumber })
            expect(cellAt('C2')).toHaveStyle({ color: LIST_PAGE_COLORS.syntaxNumber })
            expect(cellAt('C2').querySelectorAll('span')).toHaveLength(0)
            expect(cellAt('D2')).toHaveStyle({ color: LIST_PAGE_COLORS.syntaxBoolean })
            expect(cellAt('B3')).toHaveStyle({ color: LIST_PAGE_COLORS.syntaxString })
            // A name of a step holds no value of a type.
            expect(cellAt('A2')).not.toHaveStyle({ color: LIST_PAGE_COLORS.syntaxString })
        })

        it('highlights the code a cell holds as the code editor of the theme does', async () => {
            drawIn()

            await waitFor(() => expect(colourOf('E2', '18')).toBe(syntax.number))
            expect(cellAt('E2')).toHaveTextContent('= $Rate * 18')
            // A declaration the compiler reads is code too.
            expect(cellAt('A3').querySelectorAll('span').length).toBeGreaterThan(0)
        })

        it('highlights the operations of a TBasic table', async () => {
            const tbasic: RawTableCell[][] = [
                [{ cell: 'A1', value: 'TBasic Void Run()' }],
                [{ cell: 'A2', value: 'END IF', type: 'code' }],
            ]
            const keyword = highlightedKeyword()
            renderInTheme(<RawTableGrid
                rows={tbasic}
                testId="grid"
                roles={rolesOf({
                    kind: 'TBasic',
                    pos: 'A1:A2',
                    regions: [{ role: 'signature', top: 0, left: 0, bottom: 0, right: 0 },
                        { role: 'value', type: 'code', top: 1, left: 0, bottom: 1, right: 0 }],
                })}
            />, { theme: 'standard', mode: 'light' })

            await waitFor(() => expect(colourOf('A2', 'END')).toBe(keyword))
            expect(colourOf('A2', 'IF')).toBe(keyword)
        })

        it('draws the formula of a cell plain, as another text than its value', async () => {
            drawIn(steps, undefined, true)
            await waitFor(() => expect(colourOf('E2', '18')).toBe(syntax.number))

            expect(cellAt('C3')).toHaveTextContent('=1+2')
            expect(cellAt('C3').querySelectorAll('span')).toHaveLength(0)
        })

        it('draws code as written in the formatting of the workbook', () => {
            renderInTheme(<RawTableGrid roles={rolesOfKind()} rows={steps} testId="grid" />,
                { theme: 'standard', mode: 'light', excelFormatting: true })

            expect(cellAt('E2').querySelectorAll('span')).toHaveLength(0)
            expect(cellAt('B2')).not.toHaveStyle({ color: LIST_PAGE_COLORS.syntaxNumber })
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
