import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { MoreOutlined } from '@ant-design/icons'
import { Button, Dropdown } from 'antd'
import { useTranslation } from 'react-i18next'
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
        const found = asked?.cells.find(cell => cell.row === row && cell.column === column)
        return found === undefined ? undefined : asked?.editors[found.editor]
    }, [asked])

    const edited = useMemo(() => replay(rows, buffer.steps), [rows, buffer.steps])
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
        setPicked({ row, column })
        setOpen({ row, column })
        setSwitched(null)
        setDraft(cell.value == null ? '' : String(cell.value))
        onEditingChange(true)
    }, [onEditingChange, written])

    /** Keeps what was written into the open cell, unless it is what the cell already held. */
    const closeCell = (keep: boolean) => {
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

    const stopEditing = () => {
        setOpen(null)
        setBuffer(NO_EDITS)
        onEditingChange(false)
    }

    const save = async () => {
        setSaving(true)
        try {
            const savedId = await applyTableActions(projectId, tableId, compile(rows, edited))
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
        if (draft.includes('\n')) {
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
                        <Dropdown menu={{ items: switches(at, kind) }} trigger={['click']}>
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
            {editing && (
                <TableEditToolbar
                    blocked={blocked}
                    canRedo={buffer.undone.length > 0}
                    canUndo={buffer.steps.length > 0}
                    cell={picked === null ? undefined : written[at.row]?.[at.column]}
                    dirty={buffer.steps.length > 0}
                    height={written.length}
                    onCancel={stopEditing}
                    onInsertColumn={() => step({ kind: 'insertColumn', at: at.column })}
                    onInsertRow={() => step({ kind: 'insertRow', at: at.row })}
                    onRedo={() => setBuffer(redo)}
                    onSave={save}
                    onStyle={(style: RawCellStyleInput) => step({ kind: 'style', at, style })}
                    onUndo={() => setBuffer(undo)}
                    picked={picked}
                    saving={saving}
                    width={written[0]?.length ?? 0}
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
            <RawTableGrid
                decorate={decorate}
                formulas={formulas}
                onOpenCell={canWrite ? openCell : undefined}
                onOpenUsage={onOpenUsage}
                onPickCell={canWrite ? pick : undefined}
                rows={written}
                testId={testId}
            />
        </>
    )
}

export default TableEditor
