import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Dropdown, Modal, Popover, Spin } from 'antd'
import { useTranslation } from 'react-i18next'
import { useBlocker } from 'react-router-dom'
import { type CellDecoration, RawTableGrid } from '../../components/RawTableGrid'
import type { OpenUsage } from '../../components/RawTableCellText'
import { getTableEditors, type TableCellEditor, type TableEditors } from '../../services/modules'
import { applyTableActions } from '../../services/tables'
import type { RawCellStyleInput, RawTableCell, TableLayout } from 'types/tables'
import { CellValueEditor, type EditorKind } from './CellValueEditor'
import { RANGE_PANEL, RangeEditor } from './RangeEditor'
import { TableEditToolbar } from './TableEditToolbar'
import { useStyles } from './TableEditor.styles'
import {
    blankLine,
    type CellAt,
    compile,
    type EditStep,
    keyOf,
    NO_EDITS,
    redo,
    replay,
    sameCell,
    undo,
    withStep,
} from './tableEdits'

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

/** The editors this screen draws; a cell asking for anything else is written as plain text. */
const DRAWN: ReadonlySet<string> = new Set([
    'combo', 'multiselect', 'numeric', 'date', 'boolean', 'array', 'range',
])

interface TableEditorProps {
    projectId: string
    tableId: string
    /** Module the table is read through, so its editors are answered from the same module. */
    moduleName?: string | undefined
    /** How many rows the table was read as, which the editors are read for as well. */
    maxRows?: number | undefined
    /** Whether the whole table is on screen, which adding a column needs: it carries a cell per row. */
    whole?: boolean | undefined
    /** The table body as it was read, which the pending edits are replayed over. */
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
    /** Follows a piece of a cell's text to the table it names. */
    onOpenUsage?: OpenUsage | undefined
    /** Whether the reader may change the table; one who may not never picks a cell. */
    canWrite: boolean
    /** Whether the reader is editing the table. */
    editing: boolean
    /** Told when the reader starts editing by opening a cell, and when they stop. */
    onEditingChange: (editing: boolean) => void
    /** Told the table's id after a save; it changes when the table had to be moved to grow. */
    onSaved: (tableId: string) => void
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
    onOpenUsage,
    canWrite,
    editing,
    onEditingChange,
    onSaved,
    openAt,
    onOpenedAt,
    markCell,
    canvasClassName,
    children,
    testId,
}) => {
    const { t } = useTranslation('repository')
    const { styles, cx } = useStyles()
    // Never more than the table has: a table read as fewer rows than its header takes is drawn whole.
    const hidden = Math.min(Math.max(hiddenRows ?? 0, 0), rows.length)
    const [buffer, setBuffer] = useState(NO_EDITS)
    const [picked, setPicked] = useState<CellAt | null>(null)
    // The colour the reader is holding the pointer over in a palette, shown on the picked cell until they
    // take the pointer away or choose it.
    const [preview, setPreview] = useState<RawCellStyleInput | null>(null)
    // Whether the reader has asked the open cell for the panel its bounds are entered in.
    const [rangeOpen, setRangeOpen] = useState(false)
    const [open, setOpen] = useState<CellAt | null>(null)
    const [draft, setDraft] = useState('')
    const [saving, setSaving] = useState(false)
    const [asked, setAsked] = useState<TableEditors | null>(null)
    const [loadingEditors, setLoadingEditors] = useState(false)
    // The way the reader chose to write the open cell, when it is not the way the cell asks for.
    const [switched, setSwitched] = useState<EditorKind | null>(null)
    // Whether the cell that was opened takes more than one line on screen, measured as it was opened.
    const [several, setSeveral] = useState(false)
    // Whether the reader asked to close the editor while cells of theirs were still unsaved.
    const [closing, setClosing] = useState(false)
    // Picking another way of writing the cell takes the pointer out of the field, which is not the reader
    // leaving the cell — without this the cell would close on the way to the menu and never switch at all.
    const switching = useRef(false)
    // The cell already closed, so a second closer of the same cell writes nothing more; see closeCell.
    const closedCell = useRef<CellAt | null>(null)

    // How the cells take a value is read once, when the reader starts editing, and for the window the table was
    // read as — so nothing is asked while they edit, however many cells they open.
    useEffect(() => {
        if (!editing || asked !== null || loadingEditors) {
            return
        }
        setLoadingEditors(true)
        getTableEditors(projectId, tableId, { module: moduleName, maxRows })
            .then(setAsked)
            // A table nothing is known about is written as plain text, which is what an empty answer says.
            .catch(() => setAsked({ editors: [], cells: []}))
            .finally(() => setLoadingEditors(false))
    }, [asked, editing, loadingEditors, maxRows, moduleName, projectId, tableId])

    // Opening another table asks again for the cells of that one, and so does reading more of this one: the
    // rows that were not there before are described by nothing until they are asked about.
    useEffect(() => { setAsked(null) }, [tableId, maxRows])

    /** What the cell at the given place asks to be written with, as the table said when editing started. */
    const askedAt = useCallback((row: number, column: number): TableCellEditor | undefined => {
        const found = asked?.cells?.find(cell => cell.row === row && cell.column === column)
        return found === undefined ? undefined : asked?.editors?.[found.editor]
    }, [asked])

    const edited = useMemo(() => replay(rows, buffer.steps), [rows, buffer.steps])
    const dirty = buffer.steps.length > 0

    // Cells written and not yet saved live on this screen alone: leaving it loses them, so the reader is asked
    // first — whether they leave by opening another table, by the Back button, or by closing the page.
    const leaving = useBlocker(dirty)
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
     * The table as the screen draws it, which is the table the reader has plus whatever colour they are
     * holding the pointer over in the palette.
     *
     * <p>A colour shown this way is not an edit: it is not kept, cannot be taken back, and reaches no save.
     */
    const shown = useMemo(() => {
        const cell = preview === null || picked === null ? undefined : written[picked.row]?.[picked.column]
        if (cell === undefined || picked === null) {
            return written
        }
        const rowsShown = [...written]
        const row = [...(rowsShown[picked.row] ?? [])]
        row[picked.column] = { ...cell, style: { ...cell.style, ...preview } }
        rowsShown[picked.row] = row
        return rowsShown
    }, [written, picked, preview])
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

    /** The cell a move in the given direction reaches, or null where the table ends. */
    const reached = (from: CellAt, key: string): CellAt | null => {
        const cell = written[from.row]?.[from.column]
        const down = key === 'ArrowDown' ? (cell?.rowspan ?? 1) : (key === 'ArrowUp' ? -1 : 0)
        const along = key === 'ArrowRight' ? (cell?.colspan ?? 1) : (key === 'ArrowLeft' ? -1 : 0)
        const owner = ownerOf.get(`${from.row + down}:${from.column + along}`)
        // The rows kept out of sight are not the reader's to move into: they are not drawn.
        return owner === undefined || owner.row < hidden ? null : owner
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
        const value = typed ?? heldBy(cell)
        setPicked({ row, column })
        setOpen({ row, column })
        setRangeOpen(false)
        setSwitched(null)
        setSeveral(takesSeveralLines(cell.cell, value))
        setDraft(value)
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
    const closeCell = (keep: boolean, value = draft) => {
        const at = open
        // Two things can close one cell in the same breath — the field losing the focus, and the click that
        // took the focus off it — and what the cell holds is written once, not twice.
        if (switching.current || at === null || closedCell.current === at) {
            return
        }
        closedCell.current = at
        setOpen(null)
        setRangeOpen(false)
        grid.current?.focus()
        if (!keep) {
            return
        }
        // Compared against what the cell was opened on, which for a cell written with a formula is the
        // formula rather than the value it computed. Comparing against the value would read every formula
        // cell the reader merely looked into as rewritten, and write the formula over itself on the next save.
        if (value !== heldBy(written[at.row]?.[at.column])) {
            step({ kind: 'value', at, value })
        }
    }

    /** Leaves the table as it was read, dropping whatever was written into it. */
    const discard = () => {
        setOpen(null)
        setBuffer(NO_EDITS)
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

    const save = async () => {
        const actions = compile(rows, edited)
        // What the reader did may come to nothing — a row added and taken away again, a value written back to
        // what it was. There is nothing to write then, and the table is left as the reader found it.
        if (actions.length === 0) {
            discard()
            return
        }
        setSaving(true)
        try {
            const savedId = await applyTableActions(projectId, tableId, actions, moduleName)
            if (savedId !== null) {
                discard()
                onSaved(savedId)
            }
        } finally {
            setSaving(false)
        }
    }

    /**
     * The way the open cell is written.
     *
     * <p>What the reader switched to wins. Otherwise a value that is a formula or runs over several lines is
     * written as text, whatever the cell's type would ask for — those two follow the value being written, not
     * the table as it was compiled, so they are decided here rather than by the server.
     */
    const kindOf = (at: CellAt): EditorKind => {
        if (switched !== null) {
            return switched
        }
        if (draft.startsWith('=')) {
            return 'formula'
        }
        if (draft.includes('\n') || several) {
            return 'multiline'
        }
        const editor = ownKind(at)
        return editor ?? 'text'
    }

    /**
     * The way the cell asks to be written, or null when it asks for nothing of its own.
     *
     * <p>A cell whose type is a range is written as a range even where its text is not one yet — the table says
     * so only once the text parses, and a reader filling in an empty bound needs the dialog before that.
     */
    const ownKind = (at: CellAt): EditorKind | null => {
        // A cell written with a formula asks to be written as one, whatever its type would say.
        if (rows[at.row]?.[at.column]?.formula !== undefined) {
            return 'formula'
        }
        const editor = askedAt(at.row, at.column)?.editor
        if (editor !== undefined && DRAWN.has(editor)) {
            return editor as EditorKind
        }
        return (rows[at.row]?.[at.column]?.metaInfo?.type ?? '').endsWith('Range') ? 'range' : null
    }

    /** The other ways this cell can be written, which the reader picks from beside it. */
    const switches = (at: CellAt, current: EditorKind) => {
        // A cell can always be written as a formula, whatever it holds now — as the old editor offered it.
        // The way the cell asks for comes first, and is not offered twice when it is one of those three.
        const own = ownKind(at)
        const others = [...new Set<EditorKind>([...(own === null ? [] : [own]), 'formula', 'multiline', 'text'])]
        // What the menu is for is said once, over the ways of writing it offers — as the Editor said it.
        return [{
            key: 'switch-to',
            type: 'group' as const,
            label: t('browser.module.editor_switch_to'),
            children: others
                .filter(other => other !== current)
                .map(other => ({
                    key: other,
                    label: t(`browser.module.editor_kind_${other}`),
                    onClick: () => setSwitched(other),
                })),
        }]
    }

    // The open range cell, named as the workbook names it, or null where another kind is open.
    //
    // A field the reader writes in closes the cell by losing the focus. The bounds of a range are entered in a
    // panel of its own, and going to that panel takes the focus out of the field — so what closes a range cell
    // is the click that lands outside both, the way the old editor closed its panel. The panel itself waits to
    // be asked for: opening a cell opens its field, and the field drops the panel when the reader goes to it.
    const rangeCell = open !== null && kindOf(open) === 'range'
        ? written[open.row]?.[open.column]?.cell ?? null
        : null

    useEffect(() => {
        if (rangeCell === null) {
            return undefined
        }
        const away = (event: MouseEvent) => {
            const target = event.target instanceof Element ? event.target : null
            const inPanel = target?.closest(`.${RANGE_PANEL}`) != null
            const inCell = target?.closest(`[data-cell="${rangeCell}"]`) != null
            if (target !== null && !inPanel && !inCell) {
                // Closed the way every cell is closed, which leaves the menu that picks how to write it
                // alone: that menu opens beside the cell rather than in it, and choosing from it must not
                // take the cell away. What was entered in the panel is written by Done alone.
                closeCell(false)
            }
        }
        document.addEventListener('mousedown', away)
        return () => document.removeEventListener('mousedown', away)
    }, [open, rangeCell])

    /** How a cell is drawn: picked, waiting to be written, or open for writing. */
    const decorate = (cell: RawTableCell, row: number, column: number): CellDecoration | undefined => {
        const at = { row, column }
        if (sameCell(open, at)) {
            const kind = kindOf(at)
            const inCell = (
                // Another way of writing the value is asked for with the right button, where the Editor asked
                // for it: a button of its own beside the field would widen the cell, and a table whose columns
                // move as a cell is opened is a table the reader loses their place in.
                <Dropdown
                    menu={{ items: switches(at, kind) }}
                    onOpenChange={opened => { switching.current = opened }}
                    trigger={['contextMenu']}
                >
                    <div
                        className={styles.open}
                        data-testid="table-cell-switch"
                        // The bounds are entered in the panel, which the field the reader goes to drops.
                        onClick={kind === 'range' ? () => setRangeOpen(true) : undefined}
                        // The field of a range is read-only, so the panel also opens from the keyboard, with
                        // the arrow that opens a list of choices everywhere else.
                        onKeyDown={kind === 'range'
                            ? event => {
                                if (event.key === 'ArrowDown') {
                                    event.preventDefault()
                                    setRangeOpen(true)
                                }
                            }
                            : undefined}
                    >
                        <CellValueEditor
                            asked={askedAt(at.row, at.column)}
                            className={styles.input}
                            kind={kind}
                            onCancel={() => closeCell(false)}
                            onChange={setDraft}
                            onCommit={value => closeCell(true, value)}
                            onSwitch={setSwitched}
                            value={draft}
                        />
                    </div>
                </Dropdown>
            )
            return {
                // The bounds of a range are entered under the cell rather than in the cell, the way the old
                // editor dropped its panel there — and the cell keeps the way out to writing it as text.
                content: kind !== 'range' ? inCell : (
                    <Popover
                        open={rangeOpen}
                        placement="bottomLeft"
                        trigger={[]}
                        content={(
                            <RangeEditor
                                intOnly={askedAt(at.row, at.column)?.entryEditor === 'integer'}
                                onWrite={entered => closeCell(true, entered)}
                                value={draft}
                            />
                        )}
                    >
                        {inCell}
                    </Popover>
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
    const chosen = picked === null ? undefined : written[at.row]?.[at.column]
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
                    onUndo={() => setBuffer(undo)}
                    picked={picked}
                    saving={saving}
                    whole={whole}
                    onRemoveColumn={() => {
                        step({ kind: 'removeColumn', at: at.column, lines: columnsOfChosen })
                        setPicked(null)
                    }}
                    onRemoveRow={() => {
                        step({ kind: 'removeRow', at: at.row, lines: rowsOfChosen })
                        setPicked(null)
                    }}
                />
            )}
            <div className={canvasClassName}>
                {/* The grid draws the rows it is given and numbers them from the first of them, so the rows
                    kept out of sight are taken off here and put back on every place it answers with. */}
                <RawTableGrid
                    decorate={(cell, row, column) => decorate(cell, row + hidden, column)}
                    formulas={formulas}
                    layout={numbering}
                    // While the table is being edited its cells lead nowhere: a click is meant for the cell
                    // under it, and a reader aiming at one must not be taken to another table by mistake.
                    onKeyDown={canWrite ? onKeyDown : undefined}
                    onOpenCell={canWrite ? (row, column) => openCell(row + hidden, column) : undefined}
                    onOpenUsage={editing ? undefined : onOpenUsage}
                    onPickCell={canWrite ? (row, column) => pick(row + hidden, column) : undefined}
                    rows={hidden === 0 ? shown : shown.slice(hidden)}
                    tableRef={grid}
                    testId={testId}
                />
                {children}
            </div>
        </>
    )
}

export default TableEditor
