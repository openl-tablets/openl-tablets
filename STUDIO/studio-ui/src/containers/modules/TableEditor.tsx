import React, { useCallback, useMemo, useState } from 'react'
import { RedoOutlined, SaveOutlined, UndoOutlined } from '@ant-design/icons'
import { Button, Input, Popconfirm, Space } from 'antd'
import { useTranslation } from 'react-i18next'
import { type CellDecoration, RawTableGrid } from '../../components/RawTableGrid'
import type { OpenUsage } from '../../components/RawTableCellText'
import { applyTableActions } from '../../services/tables'
import type { RawTableCell } from 'types/tables'
import { useStyles } from './TableEditor.styles'
import { type CellAt, keyOf, NO_EDITS, redo, replay, sameCell, undo, withEdit } from './tableEdits'

interface TableEditorProps {
    projectId: string
    tableId: string
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

    const { rows: written, touched } = useMemo(() => replay(rows, buffer.edits), [rows, buffer.edits])

    const pick = useCallback((row: number, column: number) => setPicked({ row, column }), [])

    /** Opens a cell for writing, starting from what it holds now. */
    const openCell = useCallback((row: number, column: number) => {
        const cell = written[row]?.[column]
        if (cell === undefined || cell.covered) {
            return
        }
        setPicked({ row, column })
        setOpen({ row, column })
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
            setBuffer(current => withEdit(current, {
                operation: 'update',
                target: { type: 'cell', row: at.row, column: at.column, value: draft },
            }))
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
            const savedId = await applyTableActions(projectId, tableId, buffer.edits)
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

    /** How a cell is drawn: picked, waiting to be written, or open for writing. */
    const decorate = (cell: RawTableCell, row: number, column: number): CellDecoration | undefined => {
        const at = { row, column }
        if (sameCell(open, at)) {
            return {
                painted: true,
                content: (
                    <Input
                        autoFocus
                        className={styles.input}
                        data-testid="table-cell-input"
                        onBlur={() => closeCell(true)}
                        onChange={event => setDraft(event.target.value)}
                        size="small"
                        value={draft}
                        onKeyDown={event => {
                            if (event.key === 'Enter') {
                                closeCell(true)
                            } else if (event.key === 'Escape') {
                                closeCell(false)
                            }
                        }}
                    />
                ),
            }
        }
        const isTouched = touched.has(keyOf(at))
        return {
            className: cx(isTouched && styles.touched, sameCell(picked, at) && styles.picked,
                canWrite && styles.editable),
            painted: isTouched,
        }
    }

    return (
        <>
            {editing && (
                <div className={styles.toolbar} data-testid="table-edit-toolbar">
                    <Space.Compact>
                        <Button
                            data-testid="table-edit-undo"
                            disabled={buffer.edits.length === 0}
                            icon={<UndoOutlined />}
                            onClick={() => setBuffer(undo)}
                            title={t('browser.module.edit_undo')}
                        />
                        <Button
                            data-testid="table-edit-redo"
                            disabled={buffer.undone.length === 0}
                            icon={<RedoOutlined />}
                            onClick={() => setBuffer(redo)}
                            title={t('browser.module.edit_redo')}
                        />
                    </Space.Compact>
                    <Button
                        data-testid="table-edit-save"
                        disabled={buffer.edits.length === 0}
                        icon={<SaveOutlined />}
                        loading={saving}
                        onClick={save}
                        type="primary"
                    >
                        {t('browser.module.edit_save')}
                    </Button>
                    <Popconfirm
                        cancelText={t('browser.module.edit_keep_editing')}
                        disabled={buffer.edits.length === 0}
                        okText={t('browser.module.edit_discard')}
                        onConfirm={stopEditing}
                        title={t('browser.module.edit_discard_question')}
                    >
                        <Button
                            data-testid="table-edit-cancel"
                            onClick={buffer.edits.length === 0 ? stopEditing : undefined}
                        >
                            {t('browser.module.edit_cancel')}
                        </Button>
                    </Popconfirm>
                    <span className={styles.pending} data-testid="table-edit-pending">
                        {t('browser.module.edit_pending', { count: buffer.edits.length })}
                    </span>
                </div>
            )}
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
