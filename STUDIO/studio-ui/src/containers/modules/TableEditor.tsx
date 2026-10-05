import React, { useCallback, useEffect, useImperativeHandle, useMemo, useRef, useState } from 'react'
import { Modal, Spin } from 'antd'
import { useTheme, useThemeMode } from 'antd-style'
import { useTranslation } from 'react-i18next'
import { useBlocker } from 'react-router'
import { PaperTheme } from '../../components/PaperTheme'
import { inLook, inTheme, tableColoursOf } from '../../styles/tableColours'
import type { ThemeName } from '../../styles/themes'
import { type CellDecoration, RawTableGrid } from '../../components/RawTableGrid'
import type { OpenUsage } from '../../components/RawTableCellText'
import { notifyLoadFailure } from '../../services/apiCall'
import { getRawTable, getTableEditors, type TableCellEditor, type TableEditors } from '../../services/modules'
import { applyTableActions } from '../../services/tables'
import { useTableThemesOf } from '../../hooks/useTableThemes'
import type { RawCellStyleInput, RawTableCell, RawTableCellStyle, RawTableTextRun, TableLayout } from 'types/tables'
import type { EditorKind } from './CellValueEditor'
import { withoutFirstRows } from './hiddenRows'
import { OpenCell } from './OpenCell'
import { TableEditToolbar } from './TableEditToolbar'
import { useStyles } from './TableEditor.styles'
import {
    blankLine,
    type CellAt,
    columnAsRead,
    columnDrawnFrom,
    type EditedTable,
    compile,
    type EditStep,
    keyOf,
    NO_EDITS,
    redo,
    replay,
    rowAsRead,
    rowDrawnFrom,
    sameCell,
    undo,
    withStep,
} from './tableEdits'

/**
 * What every cell of the part of the table this one stands in is written with.
 *
 * <p>A Data or a Test table, a decision table, a lookup — each declares what a part of it holds and holds it
 * for however many lines follow, so the answer stands for a cell nobody has written in yet as much as for one
 * that holds a value.
 *
 * <p>A part left open along an axis runs on past the table's edge. A line the reader has laid down stood
 * nowhere in the table that was read, so it belongs to such a part by where it is drawn; a part of a fixed
 * size takes no laid-down line at all, being only what the table declared.
 */
const areaAt = (asked: TableEditors | null, edited: EditedTable, at: CellAt): TableCellEditor | undefined => {
    if (asked?.kind !== 'declared') {
        return undefined
    }
    const row = rowAsRead(edited, at.row)
    const column = columnAsRead(edited, at.column)
    const found = asked.areas.find(area =>
        covers(area.row, area.rows, row, at.row, () => rowDrawnFrom(edited, area.row))
        && covers(area.column, area.columns, column, at.column, () => columnDrawnFrom(edited, area.column)))
    return found === undefined ? undefined : asked.editors[found.editor]
}

/**
 * Whether a part beginning at `from` and running `size` lines covers the line, along one axis.
 *
 * @param read  where the line stood in the table that was read, or null when the reader laid it down
 * @param drawn where the line is drawn now
 * @param begins where the part begins on screen, asked for only when the line was laid down
 */
const covers = (from: number, size: number | null, read: number | null, drawn: number,
    begins: () => number): boolean =>
    read === null ? size === null && drawn >= begins() : read >= from && (size === null || read < from + size)

/**
 * How long a value has to be before its cell is taken to run over more than one line.
 *
 * <p>Used where the cell cannot be measured — it is not drawn yet, or the screen has no layout to measure.
 */
const LONG_ENOUGH_TO_WRAP = 60

/**
 * Whether the cell drawn at the given address takes more than one line to show what it holds.
 *
 * <p>Measured off the cell itself rather than guessed from the value: a short value in a narrow column wraps
 * just as a long one does, and either way the reader wants the room to write in without asking for it.
 */
const takesSeveralLines = (address: string | undefined, value: string): boolean => {
    const drawn = address === undefined
        ? null
        : document.querySelector<HTMLElement>(`td[data-cell="${CSS.escape(address)}"]`)
    if (drawn === null) {
        return value.length > LONG_ENOUGH_TO_WRAP
    }
    // The browser lays the text out in one rectangle per line, so counting them says how many lines the cell
    // takes whatever it is styled with. Measuring its height instead needs the line height as a number, and a
    // stylesheet that leaves the line height to the browser cannot give one.
    const shown = document.createRange()
    shown.selectNodeContents(drawn)
    // Not every browser-like environment lays anything out — a test harness draws no lines to count.
    const lines = typeof shown.getClientRects === 'function' ? shown.getClientRects().length : 0
    // A screen with no layout to measure — a test, a cell not drawn yet — is answered from the value alone.
    return lines === 0 ? value.length > LONG_ENOUGH_TO_WRAP : lines > 1
}

/**
 * What a cell holds, as the reader writes it.
 *
 * <p>A cell written with a formula holds the formula, not the value it computed: opening it as that value
 * would write the value back over the formula the moment the reader saves.
 */
const heldBy = (cell: RawTableCell | undefined): string =>
    cell?.formula ?? (cell?.value == null ? '' : String(cell.value))

/** The key that takes the reader back the way they came, so pressing it returns to the cell they left. */
const BACK: Record<string, string> = {
    ArrowUp: 'ArrowDown',
    ArrowDown: 'ArrowUp',
    ArrowLeft: 'ArrowRight',
    ArrowRight: 'ArrowLeft',
}

/** How far back the way a reader came is remembered, as the old editor remembered it. */
const STEPS_REMEMBERED = 10

/**
 * How far a key moves the reader along one direction of the table: past the whole span of the cell they
 * leave going forward, one place going back, and nowhere for a key that moves along the other direction.
 */
const stride = (key: string, forward: string, back: string, span: number | undefined): number => {
    if (key === forward) {
        return span ?? 1
    }
    return key === back ? -1 : 0
}

/** The editors this screen draws; a cell asking for anything else is written as plain text. */
const DRAWN: ReadonlySet<string> = new Set([
    'combo', 'multiselect', 'numeric', 'date', 'boolean', 'array', 'range',
])

/** What a write of the pending cells came to. */
export interface Written {
    /** The table's id afterwards — a new one when the table had to be moved to grow. */
    tableId: string
    /** Whether anything reached the workbook, so the module has to be compiled again. */
    changed: boolean
}

/**
 * What the screen around the editor can ask of it.
 *
 * <p>The properties of a table are rows of the table itself, so the panel that writes them has to write what
 * the reader has done to its cells first — and the table may be moved as it is written.
 */
export interface TableEditorHandle {
    /** Writes the pending cells, or answers null when the table cannot be written as it stands. */
    write: () => Promise<Written | null>
}

/** The cell the reader is writing in, and what the writing starts from. */
interface OpenAt extends CellAt {
    /** What the cell held when it was opened — a formula as the formula, not as the value it computed. */
    from: string
    /** Whether the cell takes more than one line on screen, measured as it was opened. */
    several: boolean
}

interface TableEditorProps {
    projectId: string
    tableId: string
    /** Module the table is read through, so its editors are answered from the same module. */
    moduleName?: string | undefined
    /** How many rows the table was read as, which the editors are read for as well. */
    maxRows?: number | undefined
    /** Whether the whole table is on screen, which adding a column needs: it carries a cell per row. */
    whole?: boolean | undefined
    /**
     * The table body as it was read, which the pending edits are replayed over. A body read with a table theme carries
     * the look of the theme in its cells, which no edit starts from: the table is read again without it for editing.
     */
    rows: RawTableCell[][]
    /**
     * How many rows at the top of the table are kept out of sight — what "Show Header" puts away.
     *
     * <p>The table is still given whole, and every row keeps the number it has in the table: a row is written
     * where the reader made the edit, not where the screen happened to draw it.
     */
    hiddenRows?: number | undefined
    /** How the table is laid out, where the screen numbers the lines of its data, in the table's own rows. */
    layout?: TableLayout | undefined
    /** Draw the formula a cell was written with rather than the value it computed. */
    formulas?: boolean | undefined
    /**
     * The theme of the application whose look the table is drawn in, where the reader asked for the look of the
     * Studio theme and the table is read with the table theme of that look ({@link inLook}). Absent where the table
     * keeps the colours of its table theme, and ignored while the table is edited.
     */
    look?: ThemeName | undefined
    /** Follows a piece of a cell's text to the table it names. */
    onOpenUsage?: OpenUsage | undefined
    /** Whether the reader may change the table; one who may not never picks a cell. */
    canWrite: boolean
    /** Whether the reader is editing the table. */
    editing: boolean
    /** Told when the reader starts editing by opening a cell, and when they stop. */
    onEditingChange: (editing: boolean) => void
    /** Told whether the table holds cells the reader has written and not yet saved. */
    onDirtyChange?: ((dirty: boolean) => void) | undefined
    /** Told the table's id after a save; it changes when the table had to be moved to grow. */
    onSaved: (tableId: string) => void
    /** Handed to the screen, so a write of the table can be asked for from beside it. */
    ref?: React.Ref<TableEditorHandle> | undefined
    /** A cell to open for writing, named as the workbook names it — 'D9'. */
    openAt?: string | null | undefined
    /** A cell a compilation message was raised against, marked so a reader arriving from it finds the cell. */
    markCell?: string | null | undefined
    /** Told once that cell has been opened, so asking for the same one again opens it again. */
    onOpenedAt?: (() => void) | undefined
    /** The sheet the table is drawn on, which the band of actions sits above rather than on. */
    canvasClassName?: string | undefined
    /** What the screen draws under the table — the way on to the rows beyond this window. */
    children?: React.ReactNode
    testId?: string | undefined
}

/** How a table theme draws a cell: the style and the pieces of text a read naming the theme reports for it. */
interface ThemeLook {
    style: RawTableCellStyle
    runs?: RawTableTextRun[]
}

/**
 * How the themes chosen while editing one table draw its cells, for the rows it was read as: by theme, then by the
 * address of each cell.
 */
interface ThemePreviews {
    tableId: string
    maxRows: number | undefined
    looks: ReadonlyMap<string, Map<string, ThemeLook>>
}

/** The table read as the workbook holds it, for editing a table drawn with a theme, and the window it was read as. */
interface HeldTable {
    tableId: string
    maxRows: number | undefined
    rows: RawTableCell[][]
}

/** The look a theme gives each cell of a table read with it, by the address of the cell. */
const themeCellsOf = (rows: RawTableCell[][]): Map<string, ThemeLook> => {
    const cells = new Map<string, ThemeLook>()
    rows.forEach(row => row.forEach(cell => {
        if (cell.cell !== undefined && inTheme(cell)) {
            cells.set(cell.cell, { style: cell.style, ...(cell.runs !== undefined && { runs: cell.runs }) })
        }
    }))
    return cells
}

/**
 * The table as it is being edited, drawn with a theme the reader chose: each cell read with the table takes the
 * look the theme gives it, with the styling the reader asked for laid over it — which is what the save writes.
 *
 * <p>Only the drawing takes the look. What the toolbar and the save read is the table as it is edited, whose cells
 * keep the style the workbook has.
 */
const withTheme = (edited: EditedTable, looks: Map<string, ThemeLook>): RawTableCell[][] =>
    edited.rows.map((cells, row) => cells.map((cell, column) => {
        const theme = cell.cell === undefined ? undefined : looks.get(cell.cell)
        if (theme === undefined) {
            return cell
        }
        const asked = edited.styled.get(keyOf(edited, { row, column }))
        // The pieces the workbook formats the text in give way to the theme, as in a table that is read.
        const { runs: _workbookRuns, ...rest } = cell
        return { ...rest, style: { ...theme.style, ...asked }, ...(theme.runs && { runs: theme.runs }) }
    }))

/**
 * The table, and the editing of it.
 *
 * <p>A click picks a cell, a double click opens it for writing. What the reader writes is kept here, not sent:
 * the table is asked to write it only when the reader saves, and then everything they did goes in one request.
 * Taking an edit back drops it from that list and putting it again appends it, so neither asks the server
 * anything.
 */
export const TableEditor: React.FC<TableEditorProps> = ({
    projectId,
    tableId,
    moduleName,
    maxRows,
    whole = true,
    rows,
    hiddenRows,
    layout,
    formulas,
    look,
    onOpenUsage,
    canWrite,
    editing,
    onEditingChange,
    onDirtyChange,
    onSaved,
    ref,
    openAt,
    onOpenedAt,
    markCell,
    canvasClassName,
    children,
    testId,
}) => {
    const { t } = useTranslation('repository')
    const { styles, cx } = useStyles()
    const { isDarkMode } = useThemeMode()
    const token = useTheme()
    // Whether the table was read in the look of a table theme: a kind of table no theme styles is read with the
    // formatting of the workbook, whatever theme the read named.
    const drawnInTheme = useMemo(() => rows.some(row => row.some(inTheme)), [rows])
    // A table the theme does not style keeps the colours of the workbook, and so does one drawn without a look.
    const styledByTheme = look !== undefined && drawnInTheme
    // The colours of the look the theme of the application gives its tables. A table being edited keeps the paper of
    // the workbook, since what is drawn there is what a save writes. Rebuilt with the look and the appearance, which
    // the token follows: antd-style hands out a new token on every render.
    const colours = useMemo(
        () => (look !== undefined && styledByTheme && !editing ? tableColoursOf(look, isDarkMode, token) : undefined),
        [editing, isDarkMode, look, styledByTheme]
    )
    // Never more than the table has: a table read as fewer rows than its header takes is drawn whole.
    const hidden = Math.min(Math.max(hiddenRows ?? 0, 0), rows.length)
    const [buffer, setBuffer] = useState(NO_EDITS)
    const [picked, setPicked] = useState<CellAt | null>(null)
    // The table the picked cell belongs to; see where the picked cell is fitted to the table.
    const [pickedIn, setPickedIn] = useState(tableId)
    // The colour the reader is holding the pointer over in a palette, shown on the picked cell until they
    // take the pointer away or choose it.
    const [preview, setPreview] = useState<RawCellStyleInput | null>(null)
    // How each theme chosen while editing draws each cell, read once per theme for this table and the rows it was read
    // as. It is a drawing only: what the save writes is the theme the reader chose, laid over the table as the save
    // leaves it.
    const [themePreviews, setThemePreviews] = useState<ThemePreviews | null>(null)
    const [open, setOpen] = useState<OpenAt | null>(null)
    const [saving, setSaving] = useState(false)
    const [asked, setAsked] = useState<TableEditors | null>(null)
    // Guards the asking against being started again while it is in flight. Nothing on screen turns on it,
    // so it is held aside rather than in state: the table is large, and a re-render of it is not free.
    const loadingEditors = useRef(false)
    // Whether the reader asked to close the editor while cells of theirs were still unsaved.
    const [closing, setClosing] = useState(false)
    // Picking another way of writing the cell takes the pointer out of the field, which is not the reader
    // leaving the cell — without this the cell would close on the way to the menu and never switch at all.
    const switching = useRef(false)
    // The cell already closed, so a second closer of the same cell writes nothing more; see closeCell.
    const closedCell = useRef<CellAt | null>(null)
    // Whether cells are written and unsaved, where a callback can read it without being renewed on every one
    // of them: the asking below must not start again because the reader filled in a cell. Set beside `dirty`,
    // and cleared the moment the cells are saved or dropped.
    const unsaved = useRef(false)

    // How the cells take a value is read once, when the reader starts editing, and for the window the table was
    // read as — so nothing is asked while they edit, however many cells they open.
    //
    // Asking is also how the server is told the table has been taken up to write, and it reserves the project
    // for this reader then. So a refusal is a refusal to edit — the project is held by somebody else, or this
    // reader may not write to it — and the editor closes again instead of letting them fill in cells that could
    // never be saved. A table nothing is known about is not a refusal: it answers, with nothing in it, and
    // every cell of it is then written as plain text.
    useEffect(() => {
        if (!editing || asked !== null || loadingEditors.current) {
            return undefined
        }
        loadingEditors.current = true
        // The answer belongs to the table it was asked about, and this screen is kept across tables — the reader
        // may have moved on by the time it lands. Kept anyway it would describe the cells of a table nobody is
        // looking at, and refused it would close an editor that was never turned away.
        let asking = true
        getTableEditors(projectId, tableId, { module: moduleName, maxRows })
            .then(answer => {
                if (asking) {
                    setAsked(answer)
                }
            })
            .catch(error => {
                if (!asking) {
                    return
                }
                notifyLoadFailure(t('browser.module.edit_refused'), error)
                // Cells the reader has already written are theirs to save, and Save is on the editing toolbar:
                // a window that could not be read — more rows asked for while editing — leaves editing where
                // it is rather than taking the toolbar away with the work still on screen.
                if (unsaved.current) {
                    return
                }
                setOpen(null)
                onEditingChange(false)
            })
            .finally(() => {
                // An answer nobody is waiting for any more must not say that the table now on screen has been
                // asked about; the next table cleared that on its way in.
                if (asking) {
                    loadingEditors.current = false
                }
            })
        return () => {
            asking = false
            loadingEditors.current = false
        }
    }, [asked, editing, maxRows, moduleName, onEditingChange, projectId, t, tableId])

    // Opening another table asks again for the cells of that one, and so does reading more of this one: the
    // rows that were not there before are described by nothing until they are asked about. Putting the table
    // down asks again too: the project was let go with it, and taking it up again is what holds it — kept, the
    // answer would let the reader fill in cells of a project somebody else may have taken in the meantime.
    useEffect(() => { setAsked(null) }, [tableId, maxRows, editing])

    // The table themes that style this table, asked for when the reader starts editing it, in the order the server
    // offers them: the primary theme first.
    const themes = useTableThemesOf(projectId, tableId, moduleName, editing)

    // A table drawn with a table theme carries the look of the theme in its cells, which no edit may start from: what
    // the reader styles and saves is the style the workbook holds. So the table is read again without the theme when
    // the reader starts editing it, for the rows it was read as, and edited from that read. Until it is read, nothing
    // the reader could style is offered.
    const [asHeld, setAsHeld] = useState<HeldTable | null>(null)
    const held = asHeld?.tableId === tableId && asHeld.maxRows === maxRows ? asHeld.rows : undefined
    const holding = editing && drawnInTheme
    const reading = holding && held === undefined
    useEffect(() => {
        if (!reading) {
            return undefined
        }
        let current = true
        getRawTable(projectId, tableId, { module: moduleName, maxRows, metaInfo: true })
            .then(read => {
                if (current) {
                    setAsHeld({ tableId, maxRows, rows: read.source })
                }
            })
            .catch((error: unknown) => {
                if (current) {
                    notifyLoadFailure(t('browser.module.edit_read_failed'), error)
                    onEditingChange(false)
                }
            })
        return () => {
            current = false
        }
    }, [maxRows, moduleName, onEditingChange, projectId, reading, t, tableId])
    // The next time the reader edits the table, it is read as the workbook then holds it, a save of theirs included.
    useEffect(() => {
        if (!editing) {
            setAsHeld(null)
        }
    }, [editing])
    const base = holding ? held ?? rows : rows

    const edited = useMemo(() => replay(base, buffer.steps), [base, buffer.steps])

    /**
     * What the cell now sitting here asks to be written with, as the table said when editing started.
     *
     * <p>Asked for by where the cell stood in the table that was read, not by where it sits now: a row or a
     * column the reader has laid down since has moved it, and the place it now occupies was another cell's.
     *
     * <p>A cell the table said nothing of its own about takes what its line takes, where the table declares
     * its lines — so a row the reader has just laid down in a Data or a Test table is written with the type
     * its column was declared with rather than as plain text.
     */
    const askedAt = useCallback((at: CellAt): TableCellEditor | undefined => {
        const row = rowAsRead(edited, at.row)
        const column = columnAsRead(edited, at.column)
        const found = row === null || column === null
            ? undefined
            : asked?.cells?.find(cell => cell.row === row && cell.column === column)
        return found === undefined ? areaAt(asked, edited, at) : asked?.editors?.[found.editor]
    }, [asked, edited])
    const dirty = buffer.steps.length > 0
    unsaved.current = dirty

    // Cells written and not yet saved live on this screen alone: leaving it loses them, so the reader is asked
    // first — whether they leave by opening another table, by the Back button, or by closing the page.
    //
    // The router asks at the moment the reader leaves, not as of the last drawing. A save opens the table it has
    // written at once, before the editor is drawn again without the cells it saved.
    const holdsBack = useCallback(() => unsaved.current, [])
    const leaving = useBlocker(holdsBack)
    useEffect(() => {
        if (!dirty) {
            return undefined
        }
        const ask = (event: BeforeUnloadEvent) => event.preventDefault()
        window.addEventListener('beforeunload', ask)
        return () => window.removeEventListener('beforeunload', ask)
    }, [dirty])
    const written = edited.rows

    /**
     * Where the cell covering a place sits.
     *
     * <p>A merged cell is drawn once and covers the places around it; a move that lands on one of those places
     * lands on the cell that owns it. Worked out once per table rather than searched for on every key.
     */
    const ownerOf = useMemo(() => {
        const owners = new Map<string, CellAt>()
        written.forEach((cells, row) => cells.forEach((cell, column) => {
            if (cell.covered) {
                return
            }
            for (let down = 0; down < (cell.rowspan ?? 1); down++) {
                for (let along = 0; along < (cell.colspan ?? 1); along++) {
                    owners.set(`${row + down}:${column + along}`, { row, column })
                }
            }
        }))
        return owners
    }, [written])

    /** The cell covering a place the reader may be on, or null where nothing drawn covers it. */
    const ownerAt = (row: number, column: number): CellAt | null => {
        const owner = ownerOf.get(`${row}:${column}`)
        // The rows kept out of sight are not the reader's to be on: they are not drawn.
        return owner === undefined || owner.row < hidden ? null : owner
    }

    // The picked cell is kept on the table as it is now, and settled before anything is drawn from it.
    //
    // Taking a row or a column away leaves the reader where they were, so they land on the line that moved up
    // or along into its place and can take that one away next. Past the last line of the table they land on
    // the last one; on a merged cell, on the cell it belongs to. Nothing is picked once only the rows kept out
    // of sight are left, nor in another table until the reader picks a cell of it.
    const fit = (at: CellAt): CellAt | null => {
        const row = Math.min(at.row, written.length - 1)
        return ownerAt(row, Math.min(at.column, (written[row]?.length ?? 0) - 1))
    }
    const fitted = picked === null || pickedIn !== tableId ? null : fit(picked)
    if (pickedIn !== tableId || (picked !== null && !sameCell(fitted, picked))) {
        setPickedIn(tableId)
        setPicked(fitted)
    }

    const chosenTheme = editing ? edited.theme : null
    // The looks read for the rows on screen: rows read since are drawn by none of them, so they are read again.
    const previews = themePreviews?.tableId === tableId && themePreviews.maxRows === maxRows
        ? themePreviews.looks
        : undefined
    const previewed = chosenTheme === null ? undefined : previews?.get(chosenTheme)
    const themedRows = useMemo(() => (previewed === undefined ? written : withTheme(edited, previewed)),
        [edited, previewed, written])

    // The look of the theme the reader chose is read whichever way they came to it: by choosing it, or by taking a
    // later choice back. Each theme is read once; an answer for a theme no longer chosen is dropped.
    const previewMissing = chosenTheme !== null && previewed === undefined
    useEffect(() => {
        if (!previewMissing || chosenTheme === null) {
            return undefined
        }
        let current = true
        // Only the look of the theme is kept: the cells the theme draws, by their address.
        getRawTable(projectId, tableId, { module: moduleName, maxRows, tableTheme: chosenTheme })
            .then(read => {
                if (current) {
                    setThemePreviews(known => ({
                        tableId,
                        maxRows,
                        looks: new Map([
                            ...(known?.tableId === tableId && known.maxRows === maxRows ? known.looks : []),
                            [chosenTheme, themeCellsOf(read.source)],
                        ]),
                    }))
                }
            })
            .catch((error: unknown) => notifyLoadFailure(t('browser.module.edit_theme_failed'), error))
        return () => {
            current = false
        }
    }, [chosenTheme, maxRows, moduleName, previewMissing, projectId, t, tableId])
    /**
     * The table as the screen draws it, which is the table the reader has plus whatever colour they are
     * holding the pointer over in the palette.
     *
     * <p>A colour shown this way is not an edit: it is not kept, cannot be taken back, and reaches no save.
     */
    const shown = useMemo(() => {
        const cell = preview === null || picked === null ? undefined : themedRows[picked.row]?.[picked.column]
        if (cell === undefined || picked === null) {
            return themedRows
        }
        const rowsShown = [...themedRows]
        const row = [...(rowsShown[picked.row] ?? [])]
        row[picked.column] = { ...cell, style: { ...cell.style, ...preview } }
        rowsShown[picked.row] = row
        return rowsShown
    }, [themedRows, picked, preview])

    /**
     * Writes a theme into the table when the reader saves, and draws it over the table until then.
     *
     * <p>The look is read from the server, which knows how the table is laid out, for the table as it was read. Each
     * cell takes the look of the address it was read at. So until the save, a row the reader has added is drawn
     * plain, and a cell that a row or a column inserted or deleted has moved shows the look of its old place. The
     * save themes the table as it then stands.
     */
    const chooseTheme = (theme: string) => step({ kind: 'theme', theme })
    /**
     * How the grid numbers the lines of data it draws.
     *
     * <p>The table says where its data begins among its own rows, and the grid counts from the first row it
     * is given — so the rows kept out of sight come off that line. A transposed table numbers its columns,
     * which putting rows away does not move.
     */
    const numbering = useMemo(() => (layout === undefined || hidden === 0 || layout.transposed
        ? layout
        : { ...layout, firstDataLine: layout.firstDataLine - hidden }), [hidden, layout])

    const blocked = useMemo(() => {
        const line = blankLine(edited)
        return line === null ? null : t(`browser.module.edit_blank_${line}`)
    }, [edited, t])

    /** Notes one more thing the reader did. */
    const step = (one: EditStep) => setBuffer(current => withStep(current, one))

    // The table takes the focus once a cell is picked, so the keys reach the table and nothing else.
    const grid = useRef<HTMLTableElement>(null)
    // The way the reader came, so turning back lands on the cell they left rather than on its neighbour.
    const came = useRef<{ key: string, at: CellAt }[]>([])

    const pick = useCallback((row: number, column: number) => {
        came.current = []
        setPicked({ row, column })
        // The table takes the keys once a cell is picked — but never out of a field the reader is writing in,
        // nor out of the panel hanging under it, whose clicks reach here too.
        if (open === null) {
            grid.current?.focus()
        }
    }, [open])

    /**
     * The rows the grid is given: the ones the reader sees, with the header left off where it is hidden, in the
     * colours of the look where the table is drawn in it. The line under a hidden header is drawn over the first row
     * left.
     */
    const drawn = useMemo(() => {
        const seen = withoutFirstRows(shown, hidden)
        return colours === undefined ? seen : inLook(seen, colours)
    }, [colours, hidden, shown])

    /** The cell a move in the given direction reaches, or null where the table ends. */
    const reached = (from: CellAt, key: string): CellAt | null => {
        const cell = written[from.row]?.[from.column]
        const down = stride(key, 'ArrowDown', 'ArrowUp', cell?.rowspan)
        const along = stride(key, 'ArrowRight', 'ArrowLeft', cell?.colspan)
        return ownerAt(from.row + down, from.column + along)
    }

    /** What the keyboard does with the table, as the old editor did it. */
    const onKeyDown = (event: React.KeyboardEvent<HTMLTableElement>) => {
        // While a cell is open the keys belong to what is written into it, which handles its own.
        if (open !== null || picked === null) {
            return
        }
        if (BACK[event.key] !== undefined) {
            event.preventDefault()
            const back = came.current.at(-1)
            if (back?.key === event.key) {
                came.current.pop()
                setPicked(back.at)
                return
            }
            const next = reached(picked, event.key)
            if (next !== null) {
                came.current.push({ key: BACK[event.key] ?? '', at: picked })
                came.current = came.current.slice(-STEPS_REMEMBERED)
                setPicked(next)
            }
            return
        }
        if (event.key === 'Enter') {
            event.preventDefault()
            openCell(picked.row, picked.column)
            return
        }
        // Typing on a picked cell opens it and takes what was typed, the way a spreadsheet does.
        if (event.key.length === 1 && !event.ctrlKey && !event.metaKey && !event.altKey) {
            event.preventDefault()
            openCell(picked.row, picked.column, event.key)
        }
    }

    /**
     * Opens a cell for writing, starting from what it holds now — or from the character the reader typed,
     * which is what typing on a picked cell does: the cell opens and takes that character as its new value.
     */
    const openCell = useCallback((row: number, column: number, typed?: string) => {
        const cell = written[row]?.[column]
        if (cell === undefined || cell.covered) {
            return
        }
        setPicked({ row, column })
        // Measured while the cell still shows what it holds: once it is open, the field is what stands there.
        const from = typed ?? heldBy(cell)
        setOpen({ row, column, from, several: takesSeveralLines(cell.cell, from) })
        onEditingChange(true)
    }, [onEditingChange, written])

    // A message names the cell it was raised against, and the reader asks for that cell from beside it.
    useEffect(() => {
        if (openAt == null) {
            return
        }
        // A cell among the rows kept out of sight is not drawn, so there is nothing to open.
        const row = written.findIndex((cells, index) => index >= hidden && cells.some(cell => cell.cell === openAt))
        const column = row < 0 ? -1 : (written[row] ?? []).findIndex(cell => cell.cell === openAt)
        if (row >= 0 && column >= 0) {
            openCell(row, column)
        }
        onOpenedAt?.()
    }, [hidden, onOpenedAt, openAt, openCell, written])

    /**
     * Closes the open cell, keeping what was written into it or leaving it as it was.
     *
     * <p>What is kept is the draft the field has been reporting, unless the caller says otherwise: an editor
     * that writes and closes in one gesture — the calendar, where picking a date is both — knows the value
     * before the screen does.
     */
    const closeCell = (keep: boolean, value = '') => {
        const at = open
        // Two things can close one cell in the same breath — the field losing the focus, and the click that
        // took the focus off it — and what the cell holds is written once, not twice.
        if (switching.current || at === null || closedCell.current === at) {
            return
        }
        closedCell.current = at
        setOpen(null)
        grid.current?.focus()
        if (!keep) {
            return
        }
        // Compared against what the cell was opened on, which for a cell written with a formula is the
        // formula rather than the value it computed. Comparing against the value would read every formula
        // cell the reader merely looked into as rewritten, and write the formula over itself on the next save.
        if (value !== heldBy(written[at.row]?.[at.column])) {
            step({ kind: 'value', at: { row: at.row, column: at.column }, value })
        }
    }

    /** Leaves the table as it was read, dropping whatever was written into it. */
    const discard = () => {
        // Nothing is left to lose from here on, even before the editor is drawn again.
        unsaved.current = false
        setOpen(null)
        setBuffer(NO_EDITS)
        setThemePreviews(null)
        setClosing(false)
        onEditingChange(false)
    }

    // Closing the editor with cells still unsaved loses the same work as leaving the page with them, so it is
    // the same question, asked the same way.
    const stopEditing = () => {
        if (dirty) {
            setClosing(true)
        } else {
            discard()
        }
    }

    /**
     * Writes what the reader has done, and answers the table as it stands afterwards.
     *
     * <p>Answers null where the table cannot be written as it stands — a line left blank, which the band of
     * actions says in place of saving — so a caller writing something else of its own knows not to go on.
     */
    const write = useCallback(async (): Promise<Written | null> => {
        const actions = compile(rows, edited)
        // What the reader did may come to nothing — a row added and taken away again, a value written back to
        // what it was. There is nothing to write then, and the table is left as the reader found it.
        if (actions.length === 0) {
            discard()
            return { tableId, changed: false }
        }
        if (blocked !== null) {
            return null
        }
        setSaving(true)
        try {
            const savedId = await applyTableActions(projectId, tableId, actions, moduleName)
            if (savedId === null) {
                return null
            }
            discard()
            return { tableId: savedId, changed: true }
        } finally {
            setSaving(false)
        }
    }, [blocked, edited, moduleName, projectId, rows, tableId])

    useImperativeHandle(ref, () => ({ write }), [write])

    // The screen around the editor is told what it holds, so nothing that reads the table again throws the
    // reader's work away without asking. It holds nothing once it is gone.
    useEffect(() => {
        onDirtyChange?.(dirty)
        return () => onDirtyChange?.(false)
    }, [dirty, onDirtyChange])

    const save = async () => {
        const written = await write()
        if (written?.changed === true) {
            onSaved(written.tableId)
        }
    }

    /**
     * The way the cell asks to be written, or null when it asks for nothing of its own.
     *
     * <p>A cell whose type is a range is written as a range even where its text is not one yet — the table says
     * so only once the text parses, and a reader filling in an empty bound needs the dialog before that.
     */
    const ownKind = (at: CellAt, asked: TableCellEditor | undefined): EditorKind | null => {
        // The cell as it now stands, which carries what it was read with wherever the reader has moved it.
        const cell = written[at.row]?.[at.column]
        // A cell written with a formula asks to be written as one, whatever its type would say.
        if (cell?.formula !== undefined) {
            return 'formula'
        }
        const editor = asked?.editor
        if (editor !== undefined && DRAWN.has(editor)) {
            return editor as EditorKind
        }
        return (cell?.metaInfo?.type ?? '').endsWith('Range') ? 'range' : null
    }

    /** How a cell is drawn: picked, waiting to be written, or open for writing. */
    const decorate = (cell: RawTableCell, row: number, column: number): CellDecoration | undefined => {
        const at = { row, column }
        if (open !== null && sameCell(open, at)) {
            // What the cell asks to be written with is looked up once and read twice.
            const asked = askedAt(at)
            return {
                content: (
                    <OpenCell
                        address={written[at.row]?.[at.column]?.cell}
                        asked={asked}
                        from={open.from}
                        onCancel={() => closeCell(false)}
                        onCommit={value => closeCell(true, value)}
                        onSwitching={opened => { switching.current = opened }}
                        own={ownKind(at, asked)}
                        several={open.several}
                    />
                ),
            }
        }
        const isTouched = edited.touched.has(keyOf(edited, at))
        return {
            className: cx(isTouched && styles.touched, sameCell(picked, at) && styles.picked,
                markCell != null && cell.cell === markCell && styles.raised, canWrite && styles.editable),
            painted: isTouched,
        }
    }

    /** Adding a row or a column puts it where the picked cell is, pushing that one down or along. */
    const at = picked ?? { row: -1, column: -1 }
    // The cell the reader is on, and how far it reaches: a merged cell answers for every row and column it
    // covers, so a line written beside it goes past the whole of it and a line taken away takes all of it.
    const chosen = written[at.row]?.[at.column]
    const rowsOfChosen = chosen?.rowspan ?? 1
    const columnsOfChosen = chosen?.colspan ?? 1

    return (
        <>
            {/* A save rewrites the workbook and builds the module from it again. Nothing else can be asked for
                while that happens, so nothing else is offered: the screen is held until it answers. */}
            {saving && (
                <Spin
                    fullscreen
                    data-testid="table-edit-saving"
                    description={t('browser.module.edit_saving')}
                />
            )}
            <Modal
                cancelText={t('browser.module.edit_keep_editing')}
                okButtonProps={{ 'data-testid': 'table-edit-discard' }}
                okText={t('browser.module.edit_discard')}
                open={closing || leaving.state === 'blocked'}
                title={t('browser.module.edit_leaving')}
                onCancel={() => {
                    setClosing(false)
                    leaving.reset?.()
                }}
                onOk={() => {
                    discard()
                    leaving.proceed?.()
                }}
            >
                {closing ? t('browser.module.edit_closing_message') : t('browser.module.edit_leaving_message')}
            </Modal>
            {editing && (
                <TableEditToolbar
                    blocked={blocked}
                    canRedo={buffer.undone.length > 0}
                    canUndo={dirty}
                    cell={chosen}
                    dirty={dirty}
                    onCancel={stopEditing}
                    onInsertColumn={() => step({ kind: 'insertColumn', at: at.column })}
                    onInsertRow={() => step({ kind: 'insertRow', at: at.row + rowsOfChosen })}
                    onPreview={setPreview}
                    onRedo={() => setBuffer(redo)}
                    onSave={save}
                    onStyle={(style: RawCellStyleInput) => step({ kind: 'style', at, style })}
                    onTheme={chooseTheme}
                    onUndo={() => setBuffer(undo)}
                    // A table still being read as the workbook holds it has nothing to style yet.
                    picked={reading ? null : picked}
                    saving={saving}
                    theme={edited.theme}
                    themes={themes}
                    whole={whole}
                    // The reader stays where they were, on the line that takes the place of the one gone, so
                    // several lines are taken away one after another without picking a cell each time. Picking
                    // it again hands the keys back to the table, which the click on the button took away.
                    onRemoveColumn={() => {
                        step({ kind: 'removeColumn', at: at.column, lines: columnsOfChosen })
                        pick(at.row, at.column)
                    }}
                    onRemoveRow={() => {
                        step({ kind: 'removeRow', at: at.row, lines: rowsOfChosen })
                        pick(at.row, at.column)
                    }}
                />
            )}
            <div className={canvasClassName}>
                {/* The grid draws the rows it is given and numbers them from the first of them, so the rows
                    kept out of sight are taken off here and put back on every place it answers with. The field a
                    cell is written in lies on the paper of the workbook, and so does the grid around it. */}
                <PaperTheme>
                    <RawTableGrid
                        decorate={(cell, row, column) => decorate(cell, row + hidden, column)}
                        formulas={formulas}
                        layout={numbering}
                        // While the table is being edited its cells lead nowhere: a click is meant for the cell
                        // under it, and a reader aiming at one must not be taken to another table by mistake.
                        onKeyDown={canWrite && !reading ? onKeyDown : undefined}
                        onOpenCell={canWrite && !reading ? (row, column) => openCell(row + hidden, column) : undefined}
                        onOpenUsage={editing ? undefined : onOpenUsage}
                        onPickCell={canWrite && !reading ? (row, column) => pick(row + hidden, column) : undefined}
                        paper={colours?.paper}
                        rows={drawn}
                        tableRef={grid}
                        testId={testId}
                    />
                </PaperTheme>
                {children}
            </div>
        </>
    )
}

export default TableEditor
