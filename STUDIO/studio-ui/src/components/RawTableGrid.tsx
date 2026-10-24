import React from 'react'
import { Tooltip } from 'antd'
import { useTheme, useThemeMode } from 'antd-style'
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
import { type RawTableGridStyles, roleColoursOf, useStyles } from './RawTableGrid.styles'
import { borders, collapsedBorders, indentOf, textDecoration, tinted } from './rawTableStyle'
import { useAppTheme } from '../providers/AppThemeProvider'
import { type CodeHighlighter, useCodeHighlighter } from '../hooks/useCodeHighlighter'
import type { CodePiece } from '../utils/codeHighlight'
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
    /** Class the cell is drawn with, on top of the table's own. */
    className?: string | undefined
    /** The screen paints the cell itself, so its Excel background is left out from under that paint. */
    painted?: boolean
    /** The cell is beside the point on this screen, so its colours are drawn in grey. */
    muted?: boolean
    /** What the cell shows, when the screen has more to say about it than the workbook does. */
    content?: React.ReactNode
    /** What the screen sets before or after the text of the cell, such as a button acting on the cell. */
    before?: React.ReactNode
    after?: React.ReactNode
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
    testId?: string | undefined
}

const EXCEL_SIGNIFICANT_DIGITS = 15

const formatNumber = (value: number): string =>
    Number.isInteger(value) ? String(value) : String(Number(value.toPrecision(EXCEL_SIGNIFICANT_DIGITS)))

const formatValue = (value: RawTableCell['value']): string => {
    if (value == null) {
        return ''
    }
    return typeof value === 'number' ? formatNumber(value) : String(value)
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

/**
 * The cell's Excel styling. A cell the screen paints keeps its font and alignment but not its own
 * background, which would otherwise sit over the paint. A muted cell keeps everything but the colours,
 * which are drawn in grey.
 *
 * A side of the cell a border is drawn on, as the table collapses the lines its cells name
 * ({@link collapsedBorders}), is drawn with that border; any other side keeps the line of the grid, which the cell
 * above or on the left draws where there is one. The text is written in the font of the application, whatever font
 * the workbook names. An indented cell leaves the room of its indent beside its text.
 */
const cellStyle = (style: RawTableCell['style'], border: RawTableCellBorder | undefined, painted: boolean,
    muted: boolean, ink: string): React.CSSProperties => {
    // Most cells of a workbook are written in no style at all, and a table holds thousands of them.
    if (style === undefined && border === undefined && !painted && !muted) {
        return PLAIN
    }
    return {
        background: painted ? undefined : tinted(style?.background, muted),
        color: tinted(style?.color, muted),
        textAlign: style?.align as React.CSSProperties['textAlign'],
        verticalAlign: verticalAlignOf(style?.valign),
        fontWeight: style?.bold ? 'bold' : undefined,
        fontStyle: style?.italic ? 'italic' : undefined,
        textDecoration: textDecoration(style),
        ...indentOf(style),
        ...borders(border, muted, ink),
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
    muted: boolean, ink: string): React.CSSProperties => ({
    textAlign: roleAlignOf(role, style),
    ...indentOf(style),
    ...borders(border, muted, ink),
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
const roleClassesOf = (role: CellRole | undefined, styles: RawTableGridStyles, painted: boolean,
    muted: boolean): Array<string | false> => {
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
        !painted && !muted && name !== undefined && name in fill && fill[name as keyof typeof fill],
        muted && styles.mutedRole,
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

/**
 * The text of one cell, marked with what the compiler knows about it.
 *
 * What it knows describes the value the cell holds, and the ranges it marks are measured over that text. A
 * cell shown as the formula it was written with is another text altogether, so it is drawn plain — which is
 * what the legacy editor did, where the formula replaced the marked content.
 */
const cellText = (cell: RawTableCell, formulas: boolean, muted: boolean, styles: RawTableGridStyles, ink: string,
    onOpenUsage?: OpenUsage) => {
    const asFormula = formulas && Boolean(cell.formula)
    const text = formatValue(asFormula ? cell.formula : cell.value)
    const metaInfo = asFormula ? undefined : cell.metaInfo
    // A formula is another text than the value, so the pieces the value is formatted in do not apply to it.
    const runs = asFormula ? undefined : cell.runs
    // Most cells have nothing marked — what the compiler knows about them is the type behind them and the
    // editor they ask for. Those are drawn as the text they are, rather than through a component of their own.
    if (!metaInfo?.usages?.length && !metaInfo?.returnCell && !runs?.length) {
        return text
    }
    return (
        <RawTableCellText
            ink={ink}
            metaInfo={metaInfo}
            muted={muted}
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
    testId,
}) => {
    const { excelFormatting, themeName } = useAppTheme()
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
            // Every line in the base text colour, so the parts of a table stand apart at a glance.
            role: { color: colorTextBase, style: 'solid', width: 1 },
            group: { color: colorTextBase, style: 'solid', width: 1 },
            resultBelow: { color: colorTextBase, style: 'double', width: 3 },
        } satisfies RoleLines,
    }), [colorBgContainer, colorBorder, colorError, colorFillTertiary, colorLink, colorLinkHover,
        colorPrimary, colorText, colorTextBase, colorTextSecondary])
    const laidOn = studio ? look.paper : workbookPaper()
    const { styles, cx } = useStyles(laidOn)
    // What each cell is in the Studio look, found by its address in the table.
    const cellRoles = React.useMemo(() => (studio && roles !== undefined
        ? rows.map(row => row.map(cell => (cell.covered ? undefined : cellRoleOf(roles, cell.cell, cell.type))))
        : undefined), [roles, rows, studio])
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
    // How many lines of data there are, and so how many numbers: from where the data begins to the end of the
    // table, counted down the rows or across the columns according to how the table is written.
    // The grid is as wide as its widest row; a covered cell takes a place of its own in the matrix, so the
    // count is the column count the browser lays the table out in.
    const columns = rows.reduce((widest, row) => Math.max(widest, row.length), 0)
    // How many lines of data there are, and so how many numbers: from where the data begins to the end of the
    // table, counted down the rows or across the columns according to how the table is written.
    const extent = layout?.transposed ? columns : rows.length
    const lines = layout === undefined ? 0 : Math.max(0, extent - layout.firstDataLine)
    // Where the first cell drawn lies on the sheet, when the headings of the sheet are drawn.
    const origin = headings === undefined ? undefined : placeOf(headings.from)
    /** The heading of a row or a column, which picks it where the screen lets it be picked. */
    const headingOf = (label: string, line: HeadingLine) => {
        const picks = headings?.picks
        const pickable = 'row' in line ? picks?.rows.has(line.row) : picks?.columns.has(line.column)
        const onPick = pickable ? headings?.onPick : undefined
        return (
            <td
                key={`heading-${label}`}
                className={cx(styles.heading, onPick && styles.pickableHeading)}
                data-heading={label}
                onClick={onPick && (event => onPick(line, event.currentTarget))}
            >
                {label}
            </td>
        )
    }

    const body = (
        <tbody>
            {origin && (
                <tr>
                    <td className={styles.heading} />
                    {layout !== undefined && !layout.transposed && <td className={styles.lineNumber} />}
                    {Array.from({ length: columns }, (unused, column) =>
                        headingOf(columnLettersOf(origin.column + column), { column }))}
                </tr>
            )}
            {/*
              * A transposed table's data runs across its columns, so its numbers run above them — one cell
              * per column of the grid, blank over the headings the data begins after.
              */}
            {layout?.transposed && lines > 0 && (
                <tr>
                    {origin && <td className={styles.heading} />}
                    {Array.from({ length: columns }, (unused, column) => (
                        <td key={column} className={styles.lineNumber}>
                            {column >= layout.firstDataLine
                                ? <span data-testid="table-line-number">{column - layout.firstDataLine + 1}</span>
                                : null}
                        </td>
                    ))}
                </tr>
            )}
            {rows.map((row, rowIndex) => (
                <tr key={rowKey(row, rowIndex)} className={styles.row}>
                    {origin && headingOf(String(origin.row + rowIndex + 1), { row: rowIndex })}
                    {/* A table written the usual way round is numbered down its side, as the Editor did. */}
                    {layout !== undefined && !layout.transposed && (
                        <td className={styles.lineNumber}>
                            {rowIndex >= layout.firstDataLine
                                ? <span data-testid="table-line-number">{rowIndex - layout.firstDataLine + 1}</span>
                                : null}
                        </td>
                    )}
                    {row.map((cell, columnIndex) => {
                        if (cell.covered) return null
                        const decoration = decorate?.(cell, rowIndex, columnIndex)
                        const muted = !!decoration?.muted
                        const painted = !!decoration?.painted
                        const key = cell.cell ?? `c${columnIndex}`
                        const border = borderLines[rowIndex]?.[columnIndex]
                        const role = cellRoles?.[rowIndex]?.[columnIndex]
                        const drawn = (
                            <td
                                colSpan={cell.colspan}
                                data-cell={cell.cell}
                                data-edge={edgeOf(rowIndex, columnIndex, cell, rows.length, columns)}
                                data-role={role?.region.role}
                                onClick={onPickCell && (() => onPickCell(rowIndex, columnIndex))}
                                onDoubleClick={onOpenCell && (() => onOpenCell(rowIndex, columnIndex))}
                                rowSpan={cell.rowspan}
                                className={cx(styles.cell, cell.comment !== undefined && styles.commented,
                                    ...(studio ? roleClassesOf(role, styles, painted, muted) : []),
                                    decoration?.className)}
                                style={studio
                                    ? studioCellStyle(role, cell.style, border, muted, laidOn.text)
                                    : cellStyle(cell.style, border, painted, muted, laidOn.text)}
                            >
                                {decoration?.before}
                                {decoration?.content ?? cellText(studio
                                    ? studioTextOf(cell, role, roles, look.faint, codeRuns?.[rowIndex]?.[columnIndex], inks)
                                    : cell, !!formulas, muted, styles, laidOn.text, onOpenUsage)}
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
            ))}
        </tbody>
    )

    return onKeyDown === undefined
        ? <table ref={tableRef} className={styles.table} data-testid={testId}>{body}</table>
        : (
            <table
                ref={tableRef}
                className={styles.table}
                data-testid={testId}
                onKeyDown={onKeyDown}
                role="grid"
                tabIndex={-1}
            >
                {body}
            </table>
        )
}

export default RawTableGrid
