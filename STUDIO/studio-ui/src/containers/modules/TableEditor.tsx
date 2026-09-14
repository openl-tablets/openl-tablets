import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { MoreOutlined } from '@ant-design/icons'
import { Button, Dropdown, Modal, Spin } from 'antd'
import { useTranslation } from 'react-i18next'
import { useBlocker } from 'react-router-dom'
import { type CellDecoration, RawTableGrid } from '../../components/RawTableGrid'
import type { OpenUsage } from '../../components/RawTableCellText'
import { getTableEditors, type TableCellEditor, type TableEditors } from '../../services/modules'
import { applyTableActions } from '../../services/tables'
import type { RawCellStyleInput, RawTableCell } from 'types/tables'
import { CellValueEditor, type EditorKind } from './CellValueEditor'
import { RangeDialog } from './RangeDialog'
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

/** The editors this screen draws; a cell asking for anything else is written as plain text. */
const DRAWN: ReadonlySet<string> = new Set([
    'combo', 'multiselect', 'numeric', 'date', 'boolean', 'array', 'range',
])

interface TableEditorProps {
    projectId: string
    tableId: string
    /** Module the table is read through, so its editors are answered from the same module. */
    moduleName?: string | undefined
    /** First row of the window the table was read as, which the editors are read for as well. */
    startRow?: number | undefined
    /** How many rows that window holds. */
    maxRows?: number | undefined
    /** The table body as it was read, which the pending edits are replayed over. */
    rows: RawTableCell[][]
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
    startRow,
    maxRows,
    rows,
    formulas,
    onOpenUsage,
    canWrite,
    editing,
    onEditingChange,
    onSaved,
    openAt,
    onOpenedAt,
    canvasClassName,
    children,
    testId,
}) => {
    const { t } = useTranslation('repository')
    const { styles, cx } = useStyles()
    const [buffer, setBuffer] = useState(NO_EDITS)
    const [picked, setPicked] = useState<CellAt | null>(null)
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

    // How the cells take a value is read once, when the reader starts editing, and for the window the table was
    // read as — so nothing is asked while they edit, however many cells they open.
    useEffect(() => {
        if (!editing || asked !== null || loadingEditors) {
            return
        }
        setLoadingEditors(true)
        getTableEditors(projectId, tableId, { module: moduleName, startRow, maxRows })
            .then(setAsked)
            // A table nothing is known about is written as plain text, which is what an empty answer says.
            .catch(() => setAsked({ editors: [], cells: []}))
            .finally(() => setLoadingEditors(false))
    }, [asked, editing, loadingEditors, maxRows, moduleName, projectId, startRow, tableId])

    // Opening another table asks again for the cells of that one.
    useEffect(() => { setAsked(null) }, [tableId])

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
    const blocked = useMemo(() => {
        const line = blankLine(edited)
        return line === null ? null : t(`browser.module.edit_blank_${line}`)
    }, [edited, t])

    /** Notes one more thing the reader did. */
    const step = (one: EditStep) => setBuffer(current => withStep(current, one))

    const pick = useCallback((row: number, column: number) => setPicked({ row, column }), [])

    /** Opens a cell for writing, starting from what it holds now. */
    const openCell = useCallback((row: number, column: number) => {
        const cell = written[row]?.[column]
        if (cell === undefined || cell.covered) {
            return
        }
        const value = cell.value == null ? '' : String(cell.value)
        setPicked({ row, column })
        setOpen({ row, column })
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
        const row = written.findIndex(cells => cells.some(cell => cell.cell === openAt))
        const column = row < 0 ? -1 : (written[row] ?? []).findIndex(cell => cell.cell === openAt)
        if (row >= 0 && column >= 0) {
            openCell(row, column)
        }
        onOpenedAt?.()
    }, [onOpenedAt, openAt, openCell, written])

    /** Keeps what was written into the open cell, unless it is what the cell already held. */
    const closeCell = (keep: boolean) => {
        if (switching.current) {
            return
        }
        const at = open
        setOpen(null)
        if (!keep || at === null) {
            return
        }
        const was = written[at.row]?.[at.column]?.value
        if (draft !== (was == null ? '' : String(was))) {
            step({ kind: 'value', at, value: draft })
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
        setSaving(true)
        try {
            const savedId = await applyTableActions(projectId, tableId, compile(rows, edited), moduleName)
            if (savedId !== null) {
                setBuffer(NO_EDITS)
                setOpen(null)
                onEditingChange(false)
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
            return 'text'
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
        const editor = askedAt(at.row, at.column)?.editor
        if (editor !== undefined && DRAWN.has(editor)) {
            return editor as EditorKind
        }
        return (rows[at.row]?.[at.column]?.metaInfo?.type ?? '').endsWith('Range') ? 'range' : null
    }

    /** The other ways this cell can be written, which the reader picks from beside it. */
    const switches = (at: CellAt, current: EditorKind) => {
        const own = ownKind(at)
        const others: EditorKind[] = ['multiline', 'text']
        if (own !== null) {
            others.unshift(own)
        }
        return others
            .filter(other => other !== current)
            .map(other => ({
                key: other,
                label: t(`browser.module.editor_switch_${other}`),
                onClick: () => setSwitched(other),
            }))
    }

    /** How a cell is drawn: picked, waiting to be written, or open for writing. */
    const decorate = (cell: RawTableCell, row: number, column: number): CellDecoration | undefined => {
        const at = { row, column }
        if (sameCell(open, at)) {
            const kind = kindOf(at)
            if (kind === 'range') {
                // The bounds are entered in a dialog, so the cell itself keeps showing what it holds.
                return undefined
            }
            return {
                painted: true,
                content: (
                    <div className={styles.open}>
                        <CellValueEditor
                            asked={askedAt(at.row, at.column)}
                            className={styles.input}
                            kind={kind}
                            onCancel={() => closeCell(false)}
                            onChange={setDraft}
                            onCommit={() => closeCell(true)}
                            value={draft}
                        />
                        <Dropdown
                            menu={{ items: switches(at, kind) }}
                            onOpenChange={opened => { switching.current = opened }}
                            trigger={['click']}
                        >
                            <Button
                                data-testid="table-cell-switch"
                                icon={<MoreOutlined />}
                                // The menu is opened by the pointer, and opening it must not close the cell.
                                onMouseDown={event => event.preventDefault()}
                                size="small"
                                title={t('browser.module.editor_switch')}
                                type="text"
                            />
                        </Dropdown>
                    </div>
                ),
            }
        }
        const isTouched = edited.touched.has(keyOf(edited, at))
        return {
            className: cx(isTouched && styles.touched, sameCell(picked, at) && styles.picked,
                canWrite && styles.editable),
            painted: isTouched,
        }
    }

    /** Adding a row or a column puts it where the picked cell is, pushing that one down or along. */
    const at = picked ?? { row: -1, column: -1 }

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
                    cell={picked === null ? undefined : written[at.row]?.[at.column]}
                    dirty={dirty}
                    onCancel={stopEditing}
                    onInsertColumn={() => step({ kind: 'insertColumn', at: at.column })}
                    onInsertRow={() => step({ kind: 'insertRow', at: at.row })}
                    onRedo={() => setBuffer(redo)}
                    onSave={save}
                    onStyle={(style: RawCellStyleInput) => step({ kind: 'style', at, style })}
                    onUndo={() => setBuffer(undo)}
                    picked={picked}
                    saving={saving}
                    onRemoveColumn={() => {
                        step({ kind: 'removeColumn', at: at.column })
                        setPicked(null)
                    }}
                    onRemoveRow={() => {
                        step({ kind: 'removeRow', at: at.row })
                        setPicked(null)
                    }}
                />
            )}
            <RangeDialog
                intOnly={open === null ? undefined : askedAt(open.row, open.column)?.entryEditor === 'integer'}
                onCancel={() => closeCell(false)}
                open={open !== null && kindOf(open) === 'range'}
                value={draft}
                onWrite={value => {
                    const at = open
                    setOpen(null)
                    if (at !== null && value !== String(written[at.row]?.[at.column]?.value ?? '')) {
                        step({ kind: 'value', at, value })
                    }
                }}
            />
            <div className={canvasClassName}>
                <RawTableGrid
                    decorate={decorate}
                    formulas={formulas}
                    onOpenCell={canWrite ? openCell : undefined}
                    // While the table is being edited its cells lead nowhere: a click is meant for the cell
                    // under it, and a reader aiming at one must not be taken to another table by mistake.
                    onOpenUsage={editing ? undefined : onOpenUsage}
                    onPickCell={canWrite ? pick : undefined}
                    rows={written}
                    testId={testId}
                />
                {children}
            </div>
        </>
    )
}

export default TableEditor
