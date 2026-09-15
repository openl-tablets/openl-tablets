import React from 'react'
import {
    AlignCenterOutlined,
    AlignLeftOutlined,
    AlignRightOutlined,
    BgColorsOutlined,
    BoldOutlined,
    CloseOutlined,
    DeleteColumnOutlined,
    DeleteRowOutlined,
    FontColorsOutlined,
    InsertRowAboveOutlined,
    InsertRowLeftOutlined,
    ItalicOutlined,
    MenuFoldOutlined,
    MenuUnfoldOutlined,
    RedoOutlined,
    SaveOutlined,
    UnderlineOutlined,
    UndoOutlined,
} from '@ant-design/icons'
import { Button, Tooltip } from 'antd'
import { useTranslation } from 'react-i18next'
import type { RawCellStyleInput, RawTableCell } from 'types/tables'
import type { CellAt } from './tableEdits'
import { CellColourPicker } from './CellColourPicker'
import { useStyles } from './TableEditToolbar.styles'

interface TableEditToolbarProps {
    /** The cell the reader picked, which the actions act on; none until they pick one. */
    picked: CellAt | null
    /** The cell as it stands, so a button shows whether what it sets is already on. */
    cell: RawTableCell | undefined
    canUndo: boolean
    canRedo: boolean
    /** Whether anything is waiting to be written. */
    dirty: boolean
    saving: boolean
    /** Why the table cannot be written yet, shown in place of saving it. */
    blocked: string | null
    /** Whether the whole table is on screen, which adding a column needs: it carries a cell per row. */
    whole: boolean
    onUndo: () => void
    onRedo: () => void
    onSave: () => void
    onCancel: () => void
    onInsertRow: () => void
    onRemoveRow: () => void
    onInsertColumn: () => void
    onRemoveColumn: () => void
    onStyle: (style: RawCellStyleInput) => void
    /**
     * Shows a colour on the picked cell before it is chosen, and takes it back off with null.
     *
     * <p>What is shown this way is not an edit: nothing of it is kept, taken back or saved.
     */
    onPreview: (style: RawCellStyleInput | null) => void
}

/** How far one press of the indent buttons moves a cell, as the legacy editor moved it. */
const INDENT_STEP = 1

/** The most a cell can be indented, which is as far as a workbook carries it. */
const MAX_INDENT = 15

/**
 * The band of editing actions, the one the legacy editor had: save and take back, rows and columns, alignment,
 * font, colours and indent — in one strip, in that order, with a rule between each group.
 *
 * <p>Every action here changes the table on screen alone. Nothing reaches the server until the reader saves,
 * and then all of it goes at once.
 */
export const TableEditToolbar: React.FC<TableEditToolbarProps> = ({
    picked,
    cell,
    canUndo,
    canRedo,
    dirty,
    saving,
    blocked,
    onUndo,
    onRedo,
    onSave,
    onCancel,
    onInsertRow,
    onRemoveRow,
    onInsertColumn,
    onRemoveColumn,
    onStyle,
    onPreview,
    whole,
}) => {
    const { t } = useTranslation('repository')
    const { styles, cx } = useStyles()

    const row = picked?.row ?? -1
    const column = picked?.column ?? -1
    const style = cell?.style

    const action = (
        key: string,
        icon: React.ReactNode,
        onClick: () => void,
        options: { disabled?: boolean, on?: boolean, why?: string } = {}
    ) => (
        // A button that is off says why it is off, so the reader is not left guessing at a grey icon.
        <Tooltip key={key} title={options.disabled && options.why ? options.why : t(`browser.module.edit_${key}`)}>
            <Button
                className={cx(styles.button, options.on && styles.on)}
                data-testid={`table-edit-${key}`}
                disabled={options.disabled ?? picked === null}
                icon={icon}
                onClick={onClick}
                size="small"
                type="text"
            />
        </Tooltip>
    )

    const rule = <span className={styles.rule} />

    /**
     * Why an action is off, for the two the table's header stands in the way of.
     *
     * <p>The header is one cell banked across the table, and OpenL finds the table by the corner it starts in.
     * A row added under the header or a column taken away from under it leave that corner where it is; taking
     * the header's own row away, or laying a column down before the one it starts in, do not.
     */
    const off = (why: string) => (picked === null ? t('browser.module.edit_pick_a_cell') : t(why))

    return (
        <div className={styles.toolbar} data-testid="table-edit-toolbar">
            <Tooltip title={blocked ?? t('browser.module.edit_save')}>
                <Button
                    className={styles.button}
                    data-testid="table-edit-save"
                    disabled={!dirty || blocked !== null}
                    icon={<SaveOutlined />}
                    loading={saving}
                    onClick={onSave}
                    size="small"
                    type="text"
                />
            </Tooltip>
            {rule}
            {action('undo', <UndoOutlined />, onUndo, { disabled: !canUndo })}
            {action('redo', <RedoOutlined />, onRedo, { disabled: !canRedo })}
            {rule}
            {action('insert_row', <InsertRowAboveOutlined />, onInsertRow)}
            {action('remove_row', <DeleteRowOutlined />, onRemoveRow,
                { disabled: picked === null || row < 1, why: off('browser.module.edit_header_row_kept') })}
            {rule}
            {action('insert_column', <InsertRowLeftOutlined />, onInsertColumn, {
                disabled: picked === null || column < 1 || !whole,
                why: off(column < 1 ? 'browser.module.edit_header_column_kept' : 'browser.module.edit_whole_table'),
            })}
            {action('remove_column', <DeleteColumnOutlined />, onRemoveColumn)}
            {rule}
            {action('align_left', <AlignLeftOutlined />, () => onStyle({ align: 'left' }),
                { on: style?.align === undefined || style.align === 'left' })}
            {action('align_center', <AlignCenterOutlined />, () => onStyle({ align: 'center' }),
                { on: style?.align === 'center' })}
            {action('align_right', <AlignRightOutlined />, () => onStyle({ align: 'right' }),
                { on: style?.align === 'right' })}
            {rule}
            {action('bold', <BoldOutlined />, () => onStyle({ bold: !style?.bold }), { on: !!style?.bold })}
            {action('italic', <ItalicOutlined />, () => onStyle({ italic: !style?.italic }),
                { on: !!style?.italic })}
            {action('underline', <UnderlineOutlined />, () => onStyle({ underline: !style?.underline }),
                { on: !!style?.underline })}
            {rule}
            <CellColourPicker
                className={styles.button}
                disabled={picked === null}
                icon={<BgColorsOutlined />}
                onPick={chosen => onStyle({ background: chosen })}
                onPreview={chosen => onPreview(chosen === null ? null : { background: chosen })}
                testId="table-edit-fill_colour"
                title={t('browser.module.edit_fill_colour')}
                value={style?.background ?? '#ffffff'}
            />
            <CellColourPicker
                className={styles.button}
                disabled={picked === null}
                icon={<FontColorsOutlined />}
                onPick={chosen => onStyle({ color: chosen })}
                onPreview={chosen => onPreview(chosen === null ? null : { color: chosen })}
                testId="table-edit-font_colour"
                title={t('browser.module.edit_font_colour')}
                value={style?.color ?? '#000000'}
            />
            {rule}
            {action('outdent', <MenuUnfoldOutlined />,
                () => onStyle({ indent: Math.max(0, (style?.indent ?? 0) - INDENT_STEP) }),
                { disabled: picked === null || (style?.indent ?? 0) === 0 })}
            {action('indent', <MenuFoldOutlined />,
                () => onStyle({ indent: Math.min(MAX_INDENT, (style?.indent ?? 0) + INDENT_STEP) }),
                { disabled: picked === null || (style?.indent ?? 0) >= MAX_INDENT })}
            <span className={styles.pending} />
            <Tooltip title={t('browser.module.edit_close')}>
                <Button
                    className={styles.button}
                    data-testid="table-edit-cancel"
                    icon={<CloseOutlined />}
                    onClick={onCancel}
                    size="small"
                    type="text"
                />
            </Tooltip>
        </div>
    )
}

export default TableEditToolbar
