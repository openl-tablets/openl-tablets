import React from 'react'
import { Tooltip } from 'antd'
import { useTheme, useThemeMode } from 'antd-style'
import { defaultRangeExtractor, type Range, useVirtualizer, type VirtualItem } from '@tanstack/react-virtual'
import { shallow } from 'zustand/shallow'
import type {
    RawTableCell,
    RawTableCellBorder,
    RawTableCellStyle,
    RawTableSignaturePart,
    RawTableSignaturePiece,
    RawTableTextRun,
    TableLayout,
} from 'types/tables'
import { RawTableCellText, type OpenUsage } from './RawTableCellText'
import { type MarkInks, markInksOf, markedRuns, marksOf } from './textMarks'
import { opaque } from '../styles/colorMath'
import { themePaper, workbookPaper } from '../styles/paper'
import { type RawTableGridStyles, roleColoursOf, rowHeightOf, useStyles } from './RawTableGrid.styles'
import { borders, collapsedBorders, indentOf, textDecoration } from './rawTableStyle'
import { useLaidOut } from '../hooks/useLaidOut'
import { useScrollMargin, windowRect } from '../hooks/useScrollMargin'
import { useAppTheme } from '../providers/AppThemeProvider'
import { type CodeHighlighter, useCodeHighlighter } from '../hooks/useCodeHighlighter'
import type { CodePiece } from '../utils/codeHighlight'
import { cached } from '../utils/cached'
import { joinClasses } from '../utils/joinClasses'
import { formatLocalDateTime, useDateFormats } from '../utils/dateFormat'
import {
    type CellRole, cellRoleOf, columnLettersOf, holdsCode, onPaper, placeOf, roleLinesOf, type RoleLines,
    type TableRoles,
} from './tableRoles'

/** A row or a column of a table picked by its heading, counted from 0 in the cells drawn. */
export type HeadingLine = { row: number } | { column: number }

/** The headings of the sheet a table lies on, drawn above and beside the table as Excel draws them. */
export interface SheetHeadings {
    /** The first cell drawn in A1 notation: `A9` heads the first column with A and the first row with 9. */
    from: string
    /** The rows and the columns the reader can pick by their headings, counted from 0 in the cells drawn. */
    picks?: { rows: ReadonlySet<number>; columns: ReadonlySet<number> } | undefined
    /** Told which row or column the reader picked by its heading, and the heading itself. */
    onPick?: ((line: HeadingLine, heading: HTMLElement) => void) | undefined
}

/** How the screen showing a table marks one of its cells. */
export interface CellDecoration {
    /**
     * Class the cell is drawn with, on top of the table's own. Several are given joined by spaces, as they are
     * ({@link joinClasses}): the grid puts them together with its own, once for every set of them it draws.
     */
    className?: string | undefined
    /** The screen paints the cell itself, so its Excel background is left out from under that paint. */
    painted?: boolean
    /** What the cell shows, when the screen has more to say about it than the workbook does. */
    content?: React.ReactNode
    /** What the screen sets before or after the text of the cell, such as a button acting on the cell. */
    before?: React.ReactNode
    after?: React.ReactNode
}

/** A cell of a table, by its row and its column, each counted from 0. */
export interface CellPlace {
    row: number
    column: number
}

interface RawTableGridProps {
    /** The table body as the Tables API reports it, indexed rows[row][column]. */
    rows: RawTableCell[][]
    /** How each cell is marked; a cell the screen says nothing about is drawn as the workbook has it. */
    decorate?: ((cell: RawTableCell, row: number, column: number) => CellDecoration | undefined) | undefined
    /** Draw the formula a cell was written with rather than the value it computed, where it has one. */
    formulas?: boolean | undefined
    /** Follows a piece of a cell's text to the table it names; absent when this screen cannot go there. */
    onOpenUsage?: OpenUsage | undefined
    /** Told which cell the reader picked; absent on a screen where a cell cannot be picked. */
    onPickCell?: ((row: number, column: number) => void) | undefined
    /** Told which cell the reader opened, by double-clicking it. */
    onOpenCell?: ((row: number, column: number) => void) | undefined
    /**
     * What the keyboard does with the table: moving between cells, opening one, writing into one.
     *
     * <p>Given only where the table can be written. The table takes the focus so that the keys reach it and
     * nothing else — a screen full of other fields keeps its own. Such a table is a grid, a table moved around
     * by its cells, and says so to assistive technology.
     */
    onKeyDown?: ((event: React.KeyboardEvent<HTMLTableElement>) => void) | undefined
    /** The table itself, so the screen can hand it the focus once a cell is picked. */
    tableRef?: React.Ref<HTMLTableElement> | undefined
    /**
     * How the table is laid out, where the screen numbers the lines of its data — the cases of a test table.
     * Absent where the lines are not numbered, which is every other table.
     */
    layout?: TableLayout | undefined
    /**
     * The roles of the cells of the table, which the Studio look draws it by. Absent for a table drawn in the
     * formatting of its workbook alone, such as a side of a comparison or a table of the kind Other.
     */
    roles?: TableRoles | undefined
    /**
     * The letter of each column above the table and the number of each row beside it, as the sheet names them.
     * Absent where the table is drawn without them.
     */
    headings?: SheetHeadings | undefined
    /**
     * The element the table scrolls in.
     *
     * <p>Given, the table draws the rows in sight and a few around them, and the others as the reader scrolls to
     * them: a table of thousands of rows opens as fast as a short one. A merged cell and a numbered case are drawn
     * whole or not at all. A column keeps the widest width it has been drawn at, so the columns do not move as the
     * rows go by. Absent, or where the element has no layout to measure, every row is drawn. Null while the element
     * is not drawn yet: nothing is drawn until it is, as nothing can be measured.
     */
    scroller?: HTMLElement | null | undefined
    /**
     * The cell the reader is on, counted from 0 in the rows given, which the table scrolls into sight whenever it
     * changes, so the keys never take the reader out of sight. Its rows are drawn wherever the table is scrolled, so a
     * cell being written in keeps what is written while the reader scrolls away and back. Kept only where the table is
     * drawn as it scrolls.
     */
    keepInSight?: CellPlace | undefined
    testId?: string | undefined
}

const EXCEL_SIGNIFICANT_DIGITS = 15

const formatNumber = (value: number): string =>
    Number.isInteger(value) ? String(value) : String(Number(value.toPrecision(EXCEL_SIGNIFICANT_DIGITS)))

/**
 * The text of a value. A value the compiler types as a date, which the read sends with no time zone, is written in the
 * formats of the reader; a text that only looks like a date is written as it is.
 */
const formatValue = (value: RawTableCell['value'], date = false): string => {
    if (value == null) {
        return ''
    }
    if (typeof value === 'number') {
        return formatNumber(value)
    }
    return date && typeof value === 'string' ? formatLocalDateTime(value) : String(value)
}

/** Stable row key from the first cell's A1 address (e.g. `A2`); falls back to the row position. */
const rowKey = (row: RawTableCell[], index: number): string => {
    const address = row.find(cell => cell.cell)?.cell
    return address ?? `r${index}`
}

/** Given to every cell the workbook styled in no way at all, rather than a fresh object each. */
const PLAIN: React.CSSProperties = {}

/**
 * How CSS lines a cell up from top to bottom, for the alignment the workbook gives it. CSS calls the middle of a
 * cell `middle`, where the workbook says `center`. A cell aligned to the bottom, Excel's default, has no alignment
 * of its own and keeps the top of the grid.
 */
const verticalAlignOf = (valign: string | undefined): React.CSSProperties['verticalAlign'] =>
    valign === 'center' ? 'middle' : valign

/**
 * The edges of the table a cell lies along, or undefined for a cell inside it. A cell draws the line of the grid
 * above it and on its left only along an edge: inside the table, the cell above it and the one on its left draw
 * those lines. Along every edge the line is the frame the screen draws around the table.
 */
const edgeOf = (row: number, column: number, cell: RawTableCell, height: number, width: number): string | undefined => {
    // Most cells lie inside the table: they are told so without building a text.
    const top = row === 0
    const left = column === 0
    const bottom = row + (cell.rowspan ?? 1) >= height
    const right = column + (cell.colspan ?? 1) >= width
    if (!top && !left && !bottom && !right) {
        return undefined
    }
    return `${top ? 'top ' : ''}${left ? 'left ' : ''}${bottom ? 'bottom ' : ''}${right ? 'right' : ''}`.trimEnd()
}

/** The number of a case beside the table, and the lines of the table the case takes. */
interface CaseNumber {
    number: number
    lines: number
}

/**
 * The numbers of the cases of a table, by the line each case begins on.
 *
 * A case takes the lines the layout gives it. Where the layout gives none, or its lines no longer add up to the
 * lines drawn, as after a line is added in the editor, each line is a case of its own.
 */
const caseNumbersOf = (layout: TableLayout | undefined, lines: number): Map<number, CaseNumber> => {
    const numbers = new Map<number, CaseNumber>()
    if (layout === undefined) {
        return numbers
    }
    const given = layout.caseLines
    const fits = given !== undefined && given.reduce((sum, caseLines) => sum + caseLines, 0) === lines
    const spans = fits ? given : Array.from({ length: lines }, () => 1)
    let line = layout.firstDataLine
    spans.forEach((caseLines, index) => {
        numbers.set(line, { number: index + 1, lines: caseLines })
        line += caseLines
    })
    return numbers
}

/** About how many cells the rows drawn together hold, in a table drawn as it scrolls. */
const CELLS_PER_BLOCK = 2000

/** How many blocks of rows beyond those in sight are drawn ahead, above and below. */
const BLOCKS_AHEAD = 2

/** Rows drawn together, from the first of them up to the row after the last. */
interface RowBlock {
    from: number
    to: number
}

/** The last row a cell of the row reaches, as far as its merges take it down. */
const lastRowOf = (row: RawTableCell[], index: number): number =>
    row.reduce((last, cell) => (cell.covered ? last : Math.max(last, index + (cell.rowspan ?? 1) - 1)), index)

/**
 * The rows of a table in blocks of about `size` rows, drawn together.
 *
 * A block never ends inside a merged cell or a numbered case, which take several rows: such a group is drawn whole
 * or not at all. A group longer than `size` makes a block of its own length.
 *
 * @param cases the numbers beside the rows, by the row a case begins on; nothing for a table numbered across its
 *              columns
 */
const blocksOf = (rows: RawTableCell[][], cases: Map<number, CaseNumber> | undefined, size: number): RowBlock[] => {
    const blocks: RowBlock[] = []
    let from = 0
    let reach = 0
    rows.forEach((row, index) => {
        reach = Math.max(reach, lastRowOf(row, index), index + (cases?.get(index)?.lines ?? 1) - 1)
        const closed = index >= reach
        const full = index + 1 - from >= size
        if (closed && full) {
            blocks.push({ from, to: index + 1 })
            from = index + 1
        }
    })
    if (from < rows.length) {
        blocks.push({ from, to: rows.length })
    }
    return blocks
}

/**
 * A cell standing alone in each column of the grid the blocks draw, by the column: every such cell of a column is as
 * wide as the column, so the rows are read only until each column has one. A column only merged cells cross keeps
 * none.
 *
 * @param columns how many columns the grid has
 */
const cellsByColumn = (bodies: HTMLTableSectionElement[], columns: number): Map<number, HTMLTableCellElement> => {
    const found = new Map<number, HTMLTableCellElement>()
    const rows = bodies.flatMap(body => [...body.rows])
    for (const row of rows) {
        if (found.size >= columns) {
            break
        }
        for (const cell of row.cells) {
            const at = Number(cell.dataset['col'] ?? Number.NaN)
            if (cell.colSpan === 1 && !Number.isNaN(at) && !found.has(at)) {
                found.set(at, cell)
            }
        }
    }
    return found
}

/**
 * How far to scroll along one axis to bring a box into sight: back to its near edge when it lies wholly before what
 * is in sight, and on towards its far edge when it lies wholly after, never past its near edge.
 *
 * A box partly in sight stays where it is. A cell the reader can click is in sight, and moving the table under the
 * pointer between the two clicks of a double click would open the cell beside it.
 */
const scrollToward = (start: number, end: number, viewStart: number, viewSize: number): number => {
    const viewEnd = viewStart + viewSize
    if (end <= viewStart) {
        return start - viewStart
    }
    return start >= viewEnd ? Math.min(end - viewEnd, start - viewStart) : 0
}

/** How the rows of a block are drawn, and everything they are drawn from. */
interface BlockDrawing {
    /** Draws the rows of a block. */
    draw: (from: number, to: number) => React.ReactNode
    /**
     * Everything the rows are drawn from, what they show included. A block is drawn again only when one of them
     * changes, so scrolling draws the blocks coming into sight and leaves the others as they are.
     */
    inputs: readonly unknown[]
}

interface RowBlockBodyProps {
    /** The place of the block among the blocks of the table, which the virtualizer measures it by. */
    index: number
    from: number
    to: number
    drawing: BlockDrawing
    /** Measures the block for the virtualizer. */
    measure: (body: HTMLTableSectionElement | null) => void
    /**
     * Told the block has been drawn showing other text, so the widths of its columns can be read: when it comes into
     * sight and when what the cells show changes, not when a cell is only marked otherwise.
     */
    onDrawn: (body: HTMLTableSectionElement) => void
    /** What the cells show — their values and how they are written — which the widths of the columns follow. */
    shown: object
}

/** The rows of one block, in a body of their own, so that no merged cell crosses into another block. */
const RowBlockBody = React.memo(function RowBlockBody(
    { index, from, to, drawing, measure, onDrawn, shown }: RowBlockBodyProps
) {
    const body = React.useRef<HTMLTableSectionElement>(null)
    const attach = React.useCallback((node: HTMLTableSectionElement | null) => {
        body.current = node
        measure(node)
    }, [measure])
    React.useLayoutEffect(() => {
        if (body.current !== null) {
            onDrawn(body.current)
        }
        // What the cells show is what the widths follow: a cell picked or marked otherwise leaves them as they were.
    }, [from, onDrawn, shown, to])
    return <tbody ref={attach} data-index={index}>{drawing.draw(from, to)}</tbody>
}, (before, after) => before.index === after.index && before.from === after.from && before.to === after.to
    && before.measure === after.measure && before.onDrawn === after.onDrawn
    && shallow(before.drawing.inputs, after.drawing.inputs))

/**
 * The bodies of a table drawn as it scrolls: each block drawn, after the room of the rows not drawn before it, and the
 * room of the rows after the last. The first room is always there: where it begins is where the rows begin.
 *
 * @param margin where the rows begin in what scrolls them
 * @param total  how tall all the rows are taken to be
 */
const bodiesOf = (
    items: VirtualItem[],
    margin: number,
    total: number,
    room: (key: string, height: number, first?: boolean) => React.ReactNode,
    blockOf: (item: VirtualItem) => React.ReactNode
): React.ReactNode[] => {
    let reached = items[0]?.start ?? margin
    const bodies: React.ReactNode[] = [room('first', reached - margin, true)]
    for (const item of items) {
        if (item.start > reached) {
            bodies.push(room(`before-${item.key}`, item.start - reached))
        }
        bodies.push(blockOf(item))
        reached = item.end
    }
    bodies.push(room('last', Math.max(0, total + margin - reached)))
    return bodies
}

/**
 * The cell's Excel styling. A cell the screen paints keeps its font and alignment but not its own
 * background, which would otherwise sit over the paint.
 *
 * A side of the cell a border is drawn on, as the table collapses the lines its cells name
 * ({@link collapsedBorders}), is drawn with that border; any other side keeps the line of the grid, which the cell
 * above or on the left draws where there is one. The text is written in the font of the application, whatever font
 * the workbook names. An indented cell leaves the room of its indent beside its text.
 */
const cellStyle = (style: RawTableCell['style'], border: RawTableCellBorder | undefined, painted: boolean,
    ink: string): React.CSSProperties => {
    // Most cells of a workbook are written in no style at all, and a table holds thousands of them.
    if (style === undefined && border === undefined && !painted) {
        return PLAIN
    }
    return {
        background: painted ? undefined : style?.background,
        color: style?.color,
        textAlign: style?.align as React.CSSProperties['textAlign'],
        verticalAlign: verticalAlignOf(style?.valign),
        fontWeight: style?.bold ? 'bold' : undefined,
        fontStyle: style?.italic ? 'italic' : undefined,
        textDecoration: textDecoration(style),
        ...indentOf(style),
        ...borders(border, ink),
    }
}

/**
 * How a cell of the Studio look lines up across its width: a title and a heading in the middle, a number or a date on
 * the right as Excel lines up a value by its type, and everything else on the left. An indented cell stays on the
 * side of its indent whatever its role, as the formatter writes it: its indent carries the structure of the table.
 */
const roleAlignOf = (role: CellRole | undefined, style: RawTableCell['style']): React.CSSProperties['textAlign'] => {
    if (style?.indent) {
        return undefined
    }
    const name = role?.region.role
    if (name === 'inputTitle' || name === 'outputTitle' || name === 'section') {
        return 'center'
    }
    const valued = name === 'value' || name === 'output' || name === 'result'
    return valued && (role?.type === 'number' || role?.type === 'date') ? 'right' : undefined
}

/**
 * A cell drawn in the Studio look: lined up by its role, keeping the indent of the workbook, which carries the
 * structure of a TBasic and a ColumnMatch table, with the lines the look draws around it. Nothing else of the
 * formatting of the workbook is drawn.
 */
const studioCellStyle = (role: CellRole | undefined, style: RawTableCell['style'], border: RawTableCellBorder | undefined,
    ink: string): React.CSSProperties => ({
    textAlign: roleAlignOf(role, style),
    ...indentOf(style),
    ...borders(border, ink),
})

/** The roles that hold the values of a table, which the Studio look colours by their type. */
const VALUED: ReadonlySet<string> = new Set(['value', 'output', 'result'])

/**
 * The class a value takes in the Studio look by its type: a number and a date in the hue of a number, a boolean and a
 * text in their own. Code is highlighted piece by piece instead, and a cell of no role or no type keeps the colour of
 * its role.
 */
const typeClassOf = (role: CellRole | undefined, styles: RawTableGridStyles): string | false => {
    if (role === undefined || !VALUED.has(role.region.role)) {
        return false
    }
    switch (role.type) {
        case 'number':
        case 'date':
            return styles.numberValue
        case 'boolean':
            return styles.booleanValue
        case 'string':
            return styles.stringValue
        default:
            return false
    }
}

/** The classes a cell takes from its role in the Studio look: its text, and its fill unless the screen paints it. */
const roleClassesOf = (role: CellRole | undefined, styles: RawTableGridStyles,
    painted: boolean): Array<string | false> => {
    const name = role?.region.role
    const text = {
        signature: styles.signature,
        properties: styles.muted,
        code: styles.muted,
        inputTitle: styles.inputTitle,
        section: styles.inputTitle,
        outputTitle: styles.outputTitle,
        key: styles.strong,
        result: styles.strong,
        value: false as const,
        output: false as const,
    }
    const fill = {
        inputTitle: styles.inputTitleFill,
        section: styles.inputTitleFill,
        outputTitle: styles.outputTitleFill,
        output: styles.outputFill,
    }
    return [
        styles.studioCell,
        name !== undefined && text[name],
        typeClassOf(role, styles),
        !painted && name !== undefined && name in fill && fill[name as keyof typeof fill],
    ]
}

/** The pieces of the header text the Studio look draws in a colour of their own. */
const MUTED_PIECES: ReadonlySet<RawTableSignaturePart> = new Set(['keyword', 'type', 'punctuation', 'parameterType'])

/**
 * The header text in the pieces of its signature: the name of the table bold, the names of the parameters in the
 * colour of the text, and the keyword, the types and the punctuation in a fainter one.
 */
const signatureRuns = (text: string, pieces: RawTableSignaturePiece[], faint: string): RawTableTextRun[] => {
    const name: RawTableCellStyle = { bold: true }
    const muted: RawTableCellStyle = { color: faint }
    let from = 0
    return pieces.map(piece => {
        const run: RawTableTextRun = { text: text.slice(from, piece.to) }
        if (piece.part === 'name') {
            run.style = name
        } else if (MUTED_PIECES.has(piece.part)) {
            run.style = muted
        }
        from = piece.to
        return run
    })
}

/** A piece of highlighted code as a piece of the text of a cell, in the colour and the font the highlighter gives it. */
const codeRunOf = ({ text, style }: CodePiece, bold: boolean): RawTableTextRun => {
    if (style === undefined) {
        return { text }
    }
    const decoration = String(style.textDecoration ?? style.textDecorationLine ?? '')
    const font: RawTableCellStyle = {
        ...(typeof style.color === 'string' && { color: style.color }),
        ...((bold || style.fontWeight === 'bold') && { bold: true }),
        ...(style.fontStyle === 'italic' && { italic: true }),
        ...(decoration.includes('underline') && { underline: true }),
        ...(decoration.includes('line-through') && { strikeout: true }),
    }
    return { text, style: font }
}

/**
 * The pieces of the code a cell holds, as the code editor of the theme highlights them, or undefined for a cell that
 * holds no code. A piece takes the weight of the cell, which a result is drawn in.
 */
const codeRunsOf = (cell: RawTableCell, role: CellRole | undefined, roles: TableRoles, highlighter: CodeHighlighter,
    theme: { name: Parameters<CodeHighlighter['highlight']>[2], dark: boolean }): RawTableTextRun[] | undefined => {
    if (role === undefined || !holdsCode(role) || typeof cell.value !== 'string') {
        return undefined
    }
    const bold = role.region.role === 'result'
    return highlighter.highlight(cell.value, roles.language, theme.name, theme.dark)?.map(piece => codeRunOf(piece, bold))
}

/**
 * A cell as the Studio look writes its text: the header in the pieces of its signature, code in the pieces the code
 * editor highlights, any other text in the font of the cell. Of the pieces the workbook formats the text in, only the
 * marks are kept, drawn over the code in the inks of the theme.
 */
const studioTextOf = (cell: RawTableCell, role: CellRole | undefined, roles: TableRoles | undefined,
    faint: string, code: RawTableTextRun[] | undefined, inks: MarkInks): RawTableCell => {
    const { runs, ...plain } = cell
    const header = role?.region.role === 'signature' && role.place.row === 0 && role.place.column === 0
    if (header && roles !== undefined && roles.signature.length > 0 && typeof cell.value === 'string') {
        return { ...plain, runs: signatureRuns(cell.value, roles.signature, faint) }
    }
    const marks = marksOf(runs)
    if (marks.length > 0 && typeof cell.value === 'string') {
        return { ...plain, runs: markedRuns(cell.value, code, marks, inks) }
    }
    if (code !== undefined) {
        return { ...plain, runs: code }
    }
    return runs === undefined ? cell : plain
}

/** What the compiler knows about a cell, but the links to the tables its text names. */
const withoutUsages = (metaInfo: RawTableCell['metaInfo']): RawTableCell['metaInfo'] => {
    if (metaInfo === undefined) {
        return undefined
    }
    const { usages: _usages, ...rest } = metaInfo
    return rest
}

/**
 * What the compiler knows about a cell, as far as it applies to the text the cell is shown in.
 *
 * A formula is another text than the value, and a date is shown in another text than the one it is stored as: the
 * links to the tables a text names, measured over the stored text, apply to neither. A date still keeps the mark of
 * the cell a table returns, which marks the cell whole.
 */
const shownMetaInfo = (cell: RawTableCell, asFormula: boolean, asDate: boolean): RawTableCell['metaInfo'] => {
    if (asFormula) {
        return undefined
    }
    return asDate ? withoutUsages(cell.metaInfo) : cell.metaInfo
}

/**
 * The text of one cell, marked with what the compiler knows about it.
 *
 * What it knows describes the value the cell holds, and the ranges it marks are measured over that text. A
 * cell shown as the formula it was written with is another text altogether, so it is drawn plain — which is
 * what the legacy editor did, where the formula replaced the marked content.
 */
const cellText = (cell: RawTableCell, formulas: boolean, styles: RawTableGridStyles, ink: string,
    onOpenUsage?: OpenUsage, date = false) => {
    const asFormula = formulas && Boolean(cell.formula)
    const asDate = date && !asFormula
    const text = formatValue(asFormula ? cell.formula : cell.value, asDate)
    const metaInfo = shownMetaInfo(cell, asFormula, asDate)
    // The pieces of the text are measured over the stored text, so they apply to neither a formula nor a date.
    const runs = asFormula || asDate ? undefined : cell.runs
    // Most cells have nothing marked — what the compiler knows about them is the type behind them and the
    // editor they ask for. Those are drawn as the text they are, rather than through a component of their own.
    if (!metaInfo?.usages?.length && !metaInfo?.returnCell && !runs?.length) {
        return text
    }
    return (
        <RawTableCellText
            ink={ink}
            metaInfo={metaInfo}
            onOpenUsage={onOpenUsage}
            runs={runs}
            styles={styles}
            text={text}
        />
    )
}

/**
 * Draws a table of a workbook in one of two looks, with the values already evaluated — or, where the screen asks for
 * it, with the formulas the cells were written with, which every cell carries beside its value.
 *
 * The Studio look draws the table by what its cells mean ({@link TableRoles}), in the colours of the theme of the
 * application, light or dark. It keeps nothing of the formatting of the workbook but the indent.
 *
 * The formatting of the workbook draws the table the way its author wrote it in Excel: the same cells, the same
 * merges, the same styling, in the colours Excel draws them in whatever the theme — black text on white, under what
 * the workbook fills or colours itself, so a filled cell stays readable in a dark theme. A table read with no roles is
 * drawn this way, and so is every table while the reader has **Excel Formatting** on in the theme menu.
 *
 * Every screen that shows a table of a workbook — the trace window, the comparison — draws it through
 * this component and only says how its own cells are marked, so a table looks the same everywhere.
 */
export const RawTableGrid: React.FC<RawTableGridProps> = ({
    rows,
    decorate,
    formulas,
    onOpenUsage,
    onPickCell,
    onOpenCell,
    onKeyDown,
    tableRef,
    layout,
    roles,
    headings,
    scroller,
    keepInSight,
    testId,
}) => {
    const dateFormats = useDateFormats()
    const { compact, excelFormatting, themeName } = useAppTheme()
    const { isDarkMode } = useThemeMode()
    const token = useTheme()
    // The Studio look draws a table by what its cells mean, in the colours of the theme; a table read with no roles,
    // or with the Excel Formatting of the theme menu on, keeps the formatting of its workbook.
    const studio = !onPaper(roles, excelFormatting)
    const {
        colorBgContainer, colorText, colorFillTertiary, colorLink, colorLinkHover, colorError, colorTextSecondary,
        colorBorder, colorPrimary, colorTextBase, colorErrorText, colorWarningText, colorSuccessText, colorInfoText,
    } = token
    // The marks read on every fill a role draws, in every theme.
    const inks = React.useMemo(() => {
        const roles = roleColoursOf({ colorText, colorTextSecondary, colorFillTertiary, colorBgContainer, colorPrimary })
        const grounds = [colorBgContainer, roles.inputTitleFill, roles.outputTitleFill, roles.outputFill]
            .map(fill => opaque(fill, colorBgContainer))
        return markInksOf({ colorErrorText, colorWarningText, colorSuccessText, colorInfoText }, grounds)
    }, [colorBgContainer, colorErrorText, colorFillTertiary, colorInfoText, colorPrimary, colorSuccessText, colorText,
        colorTextSecondary, colorWarningText])
    // The paper and the lines of the theme, worked out again only when a colour of them changes.
    const look = React.useMemo(() => ({
        paper: themePaper({ colorBgContainer, colorText, colorFillTertiary, colorBorder, colorLink, colorLinkHover,
            colorError }),
        faint: roleColoursOf({ colorText, colorTextSecondary, colorFillTertiary, colorBgContainer, colorPrimary }).faint,
        lines: {
            // Every line in the base text colour, so the parts of a table stand apart at a glance. A group line is
            // dotted, as the hair line of the Excel style, so a group reads apart from the parts of the table.
            role: { color: colorTextBase, style: 'solid', width: 1 },
            group: { color: colorTextBase, style: 'dotted', width: 1 },
            resultBelow: { color: colorTextBase, style: 'double', width: 3 },
        } satisfies RoleLines,
    }), [colorBgContainer, colorBorder, colorError, colorFillTertiary, colorLink, colorLinkHover,
        colorPrimary, colorText, colorTextBase, colorTextSecondary])
    const laidOn = studio ? look.paper : workbookPaper()
    const { styles, cx } = useStyles(laidOn)
    // What each cell is, found by its address in the table. Either look writes a date in the formats of the reader;
    // the Studio look also draws a cell by its role.
    const typedRoles = React.useMemo(() => (roles === undefined
        ? undefined
        : rows.map(row => row.map(cell => (cell.covered ? undefined : cellRoleOf(roles, cell.cell, cell.type))))),
    [roles, rows])
    const cellRoles = studio ? typedRoles : undefined
    // The grammars of the code editor are loaded with the first table holding code, and each code cell is highlighted
    // once for the rows and the theme it is drawn in.
    const hasCode = React.useMemo(() => cellRoles?.some(row => row.some(role => role !== undefined && holdsCode(role)))
        ?? false, [cellRoles])
    const highlighter = useCodeHighlighter(hasCode)
    const codeRuns = React.useMemo(() => (cellRoles === undefined || roles === undefined || highlighter === undefined
        ? undefined
        : rows.map((row, rowIndex) => row.map((cell, column) => codeRunsOf(cell, cellRoles[rowIndex]?.[column], roles,
            highlighter, { name: themeName, dark: isDarkMode })))), [cellRoles, highlighter, isDarkMode, roles, rows,
        themeName])
    // Each line two cells share is drawn once, by the cell above or on the left: in the Studio look the lines of the
    // roles, in the formatting of the workbook the borders of the cells.
    const borderLines = React.useMemo(() => collapsedBorders(rows, cellRoles === undefined || roles === undefined
        ? undefined
        : (cell, row, column) => {
            const role = cellRoles[row]?.[column]
            return role === undefined
                ? undefined
                : roleLinesOf(roles, role, { rows: cell.rowspan ?? 1, columns: cell.colspan ?? 1 }, look.lines)
        }), [cellRoles, look, roles, rows])
    // The grid is as wide as its widest row; a covered cell takes a place of its own in the matrix, so the
    // count is the column count the browser lays the table out in.
    const columns = React.useMemo(() => rows.reduce((widest, row) => Math.max(widest, row.length), 0), [rows])
    // How many lines of data there are: from where the data begins to the end of the table, counted down the rows
    // or across the columns according to how the table is written. The cases of the table take them.
    const extent = layout?.transposed ? columns : rows.length
    const lines = layout === undefined ? 0 : Math.max(0, extent - layout.firstDataLine)
    const caseNumbers = React.useMemo(() => caseNumbersOf(layout, lines), [layout, lines])
    /**
     * The margin beside a line of the table: blank beside the headings, the number of a case on the line it begins
     * on, spanning the lines it takes, and nothing on the other lines of a case, which that number spans.
     *
     * @param col the column of the grid the margin stands in, which a table drawn as it scrolls keeps the width of
     */
    const lineNumberOf = (line: number, span: 'rowSpan' | 'colSpan', col?: number) => {
        if (layout === undefined || line < layout.firstDataLine) {
            return <td key={line} className={styles.lineNumber} data-col={col} />
        }
        const found = caseNumbers.get(line)
        return found && (
            <td key={line} className={styles.lineNumber} data-col={col} {...{ [span]: found.lines }}>
                <span data-testid="table-line-number">{found.number}</span>
            </td>
        )
    }
    // The class names of a cell are put together once for each set of them a drawing meets, not once for each cell:
    // putting them together is what costs, and a table repeats a few sets over thousands of cells.
    const classNames = new Map<string, string>()
    const classOf = (...names: Array<string | false | undefined>): string => {
        const key = joinClasses(...names)
        return cached(classNames, key, () => cx(key))
    }
    // Where the first cell drawn lies on the sheet, when the headings of the sheet are drawn.
    const origin = headings === undefined ? undefined : placeOf(headings.from)
    /**
     * The heading of a row or a column, which picks it where the screen lets it be picked.
     *
     * @param col the column of the grid the heading stands in, which a table drawn as it scrolls keeps the width of
     */
    const headingOf = (label: string, line: HeadingLine, col?: number) => {
        const picks = headings?.picks
        const pickable = 'row' in line ? picks?.rows.has(line.row) : picks?.columns.has(line.column)
        const onPick = pickable ? headings?.onPick : undefined
        return (
            <td
                key={`heading-${label}`}
                className={classOf(styles.heading, onPick && styles.pickableHeading)}
                data-col={col}
                data-heading={label}
                onClick={onPick && (event => onPick(line, event.currentTarget))}
            >
                {label}
            </td>
        )
    }

    // A table drawn as it scrolls is measured as it is drawn. A screen with no layout to measure, as a test has, or
    // not laid out yet, draws every row, and draws them as they scroll once it is laid out.
    const measurable = useLaidOut(scroller, 'height')
    const virtual = scroller !== undefined && measurable
    // The table keeps the place the reader scrolled to by itself as the rows above it are measured, so the browser is
    // not to move it a second time.
    React.useEffect(() => {
        if (!virtual || scroller == null) {
            return undefined
        }
        const anchoring = scroller.style.overflowAnchor
        scroller.style.overflowAnchor = 'none'
        return () => {
            scroller.style.overflowAnchor = anchoring
        }
    }, [scroller, virtual])
    // The columns of the margin before the cells: the heading of a row, and the number of a case.
    const headed = origin !== undefined
    const numbered = layout !== undefined && !layout.transposed
    const prefix = Number(headed) + Number(numbered)
    /** The column of the grid a column of the table stands in, behind the margin. */
    const colOf = (column: number) => (virtual ? prefix + column : undefined)
    // The rows above the cells: the letters of the columns, and the numbers of the cases of a transposed table.
    const numberedAcross = layout?.transposed === true && lines > 0
    const headingCount = Number(headed) + Number(numberedAcross)

    const headingRows = (
        <>
            {origin && (
                <tr aria-rowindex={1}>
                    <td className={styles.heading} />
                    {numbered && <td className={styles.lineNumber} />}
                    {Array.from({ length: columns }, (unused, column) =>
                        headingOf(columnLettersOf(origin.column + column), { column }))}
                </tr>
            )}
            {/*
              * A transposed table's data runs across its columns, so its numbers run above them — one cell
              * per column of the grid, blank over the headings the data begins after.
              */}
            {numberedAcross && (
                <tr aria-rowindex={headingCount}>
                    {origin && <td className={styles.heading} />}
                    {Array.from({ length: columns }, (unused, column) => lineNumberOf(column, 'colSpan'))}
                </tr>
            )}
        </>
    )

    /** One row of the table, counted from 0 in the rows given. */
    const drawRow = (row: RawTableCell[], rowIndex: number) => (
        <tr
            key={rowKey(row, rowIndex)}
            aria-rowindex={headingCount + rowIndex + 1}
            className={styles.row}
            data-row={virtual ? rowIndex : undefined}
        >
            {origin && headingOf(String(origin.row + rowIndex + 1), { row: rowIndex }, colOf(-prefix))}
            {/* A table written the usual way round is numbered down its side, as the Editor did. */}
            {numbered && lineNumberOf(rowIndex, 'rowSpan', colOf(-1))}
            {row.map((cell, columnIndex) => {
                if (cell.covered) return null
                const decoration = decorate?.(cell, rowIndex, columnIndex)
                const painted = !!decoration?.painted
                const key = cell.cell ?? `c${columnIndex}`
                const border = borderLines[rowIndex]?.[columnIndex]
                const role = cellRoles?.[rowIndex]?.[columnIndex]
                const drawn = (
                    <td
                        colSpan={cell.colspan}
                        data-cell={cell.cell}
                        data-col={colOf(columnIndex)}
                        data-edge={edgeOf(rowIndex, columnIndex, cell, rows.length, columns)}
                        data-role={role?.region.role}
                        onClick={onPickCell && (() => onPickCell(rowIndex, columnIndex))}
                        onDoubleClick={onOpenCell && (() => onOpenCell(rowIndex, columnIndex))}
                        rowSpan={cell.rowspan}
                        className={classOf(styles.cell, cell.comment !== undefined && styles.commented,
                            ...(studio ? roleClassesOf(role, styles, painted) : []),
                            decoration?.className)}
                        style={studio
                            ? studioCellStyle(role, cell.style, border, laidOn.text)
                            : cellStyle(cell.style, border, painted, laidOn.text)}
                    >
                        {decoration?.before}
                        {decoration?.content ?? cellText(studio
                            ? studioTextOf(cell, role, roles, look.faint, codeRuns?.[rowIndex]?.[columnIndex], inks)
                            : cell, !!formulas, styles, laidOn.text, onOpenUsage,
                        typedRoles?.[rowIndex]?.[columnIndex]?.type === 'date')}
                        {decoration?.after}
                    </td>
                )
                // The note is shown while the cell is read. A cell the screen has taken over — one
                // being written into — shows what the screen put there, not a note over the top of it.
                return cell.comment === undefined || decoration?.content !== undefined
                    ? <React.Fragment key={key}>{drawn}</React.Fragment>
                    : (
                        <Tooltip
                            key={key}
                            placement="rightBottom"
                            title={<span className={styles.note}>{cell.comment}</span>}
                        >
                            {drawn}
                        </Tooltip>
                    )
            })}
        </tr>
    )

    // The rows in blocks drawn together, which a merged cell or a numbered case never crosses.
    const rowCases = numbered ? caseNumbers : undefined
    const blocks = React.useMemo(() => (virtual
        ? blocksOf(rows, rowCases, Math.max(1, Math.ceil(CELLS_PER_BLOCK / Math.max(columns, 1))))
        : []), [columns, rowCases, rows, virtual])
    // The block of the cell the reader is on, drawn wherever the table is scrolled: the cell may be open for writing,
    // and a cell drawn away would take what is written in it along.
    const keptRow = keepInSight?.row
    const keptColumn = keepInSight?.column ?? 0
    const pinned = keptRow === undefined ? -1 : blocks.findIndex(one => one.from <= keptRow && keptRow < one.to)
    // Where the rows begin in what the element scrolls: under its padding and the headings drawn above them.
    const anchor = React.useRef<HTMLTableSectionElement>(null)
    const margin = useScrollMargin(anchor, scroller, virtual)
    // The options the virtualizer works its measurements out from are kept while the blocks stay the same, so a render
    // that only scrolls does not have it work them all out again.
    const rowHeight = rowHeightOf(token)
    // A block not measured yet is taken to be made of rows of one line.
    const estimateSize = React.useCallback((index: number) =>
        ((blocks[index]?.to ?? 0) - (blocks[index]?.from ?? 0)) * rowHeight, [blocks, rowHeight])
    const getItemKey = React.useCallback((index: number) => blocks[index]?.from ?? index, [blocks])
    const rangeExtractor = React.useCallback((range: Range) => {
        const indexes = defaultRangeExtractor(range)
        return pinned < 0 || indexes.includes(pinned) ? indexes : [...indexes, pinned].sort((one, other) => one - other)
    }, [pinned])
    const virtualizer = useVirtualizer<HTMLElement, HTMLTableSectionElement>({
        count: blocks.length,
        enabled: virtual,
        getScrollElement: () => scroller ?? null,
        estimateSize,
        getItemKey,
        // Before the sheet is there to measure, it is taken to be as tall as the window: the first drawing already
        // holds the rows in sight rather than none.
        initialRect: windowRect(),
        overscan: BLOCKS_AHEAD,
        rangeExtractor,
        scrollMargin: margin,
        // The rows come into sight with the next frame rather than inside the scroll itself, which the browser then
        // keeps smooth.
        useFlushSync: false,
    })
    const items = virtualizer.getVirtualItems()

    // The widest each column of the grid has been drawn at, the margin included, which it keeps: the browser lays a
    // table out by the rows drawn, and the columns would otherwise move as the rows go by.
    // A <col> holds the widest width of its column. The widths start anew for another set of columns, and whenever the
    // cells are written otherwise — their formulas, the other look, another density, other date formats — which may
    // take less room than before.
    const columnSet = React.useRef<HTMLTableColElement>(null)
    const grid = `${prefix}:${columns}:${String(formulas)}:${String(studio)}:${String(compact)}:${dateFormats.dateTime}`
    // The blocks drawn with other values since the widths were last read. They are read together once a frame, every
    // width before any is written, so the table is laid out once for all of them rather than once for every cell.
    const drawnBlocks = React.useRef(new Set<HTMLTableSectionElement>())
    const nextFrame = React.useRef<number | null>(null)
    const readWidths = React.useCallback(() => {
        nextFrame.current = null
        const set = columnSet.current?.children
        const cells = cellsByColumn([...drawnBlocks.current].filter(body => body.isConnected), set?.length ?? 0)
        drawnBlocks.current.clear()
        const drawnWidths = [...cells].map(([at, cell]) => [at, cell.offsetWidth] as const)
        drawnWidths.forEach(([at, width]) => {
            const col = set?.[at] as HTMLElement | undefined
            if (col !== undefined && width > (Number.parseFloat(col.style.width) || 0)) {
                col.style.width = `${width}px`
            }
        })
    }, [])
    const widen = React.useCallback((body: HTMLTableSectionElement) => {
        drawnBlocks.current.add(body)
        nextFrame.current ??= requestAnimationFrame(readWidths)
    }, [readWidths])
    React.useEffect(() => () => {
        if (nextFrame.current !== null) {
            cancelAnimationFrame(nextFrame.current)
        }
    }, [])

    // The cell the reader is on is scrolled into sight, across and down, when they move, wherever the keys take them.
    // Its block is drawn wherever the table is scrolled, so it is there to be scrolled to once that block is drawn.
    const kept = React.useRef<string | undefined>(undefined)
    const pinnedDrawn = items.some(item => item.index === pinned)
    React.useEffect(() => {
        if (keptRow === undefined) {
            // Nothing is picked: the next cell picked is scrolled to, even the one picked before.
            kept.current = undefined
            return
        }
        const place = `${keptRow}:${keptColumn}`
        const row = anchor.current?.closest('table')?.querySelector<HTMLElement>(`tr[data-row="${keptRow}"]`)
        if (!virtual || scroller == null || place === kept.current || row == null) {
            return
        }
        kept.current = place
        const cell = row.querySelector<HTMLElement>(`:scope > td[data-col="${prefix + keptColumn}"]`) ?? row
        const view = scroller.getBoundingClientRect()
        const box = cell.getBoundingClientRect()
        scroller.scrollTop += scrollToward(box.top, box.bottom, view.top, scroller.clientHeight)
        scroller.scrollLeft += scrollToward(box.left, box.right, view.left, scroller.clientWidth)
    }, [keptColumn, keptRow, pinnedDrawn, prefix, scroller, virtual])

    const width = Math.max(1, prefix + columns)
    // What the cells show, which the widths of the columns follow: their values, and how they are written.
    const shown = React.useMemo(() => ({ rows, formulas, studio, compact, dateFormats }),
        [compact, dateFormats, formulas, rows, studio])
    const drawing: BlockDrawing = {
        draw: (from, to) => rows.slice(from, to).map((row, index) => drawRow(row, from + index)),
        // The look of the cells follows the theme, its appearance and its density, which the styles are read in.
        inputs: [rows, decorate, formulas, onOpenUsage, onPickCell, onOpenCell, roles, layout, headings, studio,
            laidOn, look, inks, cellRoles, typedRoles, codeRuns, borderLines, caseNumbers, columns, dateFormats,
            compact, themeName, isDarkMode],
    }
    /** The room of rows not drawn, before, between or after the blocks that are. */
    const room = (key: string, height: number, first = false) => (
        <tbody key={key} ref={first ? anchor : undefined} aria-hidden data-testid="table-rows-room">
            <tr>
                <td colSpan={width} style={{ height, padding: 0, border: 'none' }} />
            </tr>
        </tbody>
    )
    const blockOf = (item: VirtualItem) => {
        const block = blocks[item.index]
        return block && (
            <RowBlockBody
                key={item.key}
                drawing={drawing}
                from={block.from}
                index={item.index}
                measure={virtualizer.measureElement}
                onDrawn={widen}
                shown={shown}
                to={block.to}
            />
        )
    }

    const body = virtual
        ? (
            <>
                <colgroup key={grid} ref={columnSet} data-testid="table-column-widths">
                    {Array.from({ length: prefix + columns }, (unused, column) => <col key={column} />)}
                </colgroup>
                <tbody>{headingRows}</tbody>
                {bodiesOf(items, margin, virtualizer.getTotalSize(), room, blockOf)}
            </>
        )
        : (
            <tbody>
                {headingRows}
                {rows.map((row, rowIndex) => drawRow(row, rowIndex))}
            </tbody>
        )

    // A table moved around by its cells is a grid, and says so to assistive technology. Its rows are counted whole,
    // the ones not drawn included, so a reader knows where a row stands among them.
    return (
        <table
            ref={tableRef}
            aria-rowcount={headingCount + rows.length}
            className={styles.table}
            data-testid={testId}
            {...(onKeyDown && { onKeyDown, role: 'grid', tabIndex: -1 })}
        >
            {body}
        </table>
    )
}

export default RawTableGrid
