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
import { Button, ColorPicker, Popconfirm, Tooltip } from 'antd'
import type { AggregationColor } from 'antd/es/color-picker/color'
import { useTranslation } from 'react-i18next'
import type { RawCellStyleInput, RawTableCell } from 'types/tables'
import type { CellAt } from './tableEdits'
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
    onUndo: () => void
    onRedo: () => void
    onSave: () => void
    onCancel: () => void
    onInsertRow: () => void
    onRemoveRow: () => void
    onInsertColumn: () => void
    onRemoveColumn: () => void
    onStyle: (style: RawCellStyleInput) => void
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
}) => {
    const { t } = useTranslation('repository')
    const { styles, cx } = useStyles()

    // The first row and the first column carry the table's header, which the API keeps: they are neither
    // removed nor pushed aside, so the actions that would touch them are not offered.
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

    // The table's first row and first column hold its header, and the engine finds the table by that corner:
    // nothing is added before them and neither is taken away.
    const header = picked === null ? t('browser.module.edit_pick_a_cell') : t('browser.module.edit_header_kept')

    /** The colour as the API writes it: #rrggbb, without whatever the picker says about opacity. */
    const colour = (chosen: AggregationColor) => chosen.toHexString().slice(0, 7)

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
            {action('insert_row', <InsertRowAboveOutlined />, onInsertRow,
                { disabled: row < 1, why: header })}
            {action('remove_row', <DeleteRowOutlined />, onRemoveRow, { disabled: row < 1, why: header })}
            {rule}
            {action('insert_column', <InsertRowLeftOutlined />, onInsertColumn,
                { disabled: column < 1, why: header })}
            {action('remove_column', <DeleteColumnOutlined />, onRemoveColumn,
                { disabled: column < 1, why: header })}
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
            <Tooltip title={t('browser.module.edit_fill_colour')}>
                <span>
                    <ColorPicker
                        disabled={picked === null}
                        format="hex"
                        onChangeComplete={chosen => onStyle({ background: colour(chosen) })}
                        value={style?.background ?? '#ffffff'}
                    >
                        <Button
                            className={styles.button}
                            data-testid="table-edit-fill_colour"
                            disabled={picked === null}
                            icon={<BgColorsOutlined />}
                            size="small"
                            type="text"
                        />
                    </ColorPicker>
                </span>
            </Tooltip>
            <Tooltip title={t('browser.module.edit_font_colour')}>
                <span>
                    <ColorPicker
                        disabled={picked === null}
                        format="hex"
                        onChangeComplete={chosen => onStyle({ color: colour(chosen) })}
                        value={style?.color ?? '#000000'}
                    >
                        <Button
                            className={styles.button}
                            data-testid="table-edit-font_colour"
                            disabled={picked === null}
                            icon={<FontColorsOutlined />}
                            size="small"
                            type="text"
                        />
                    </ColorPicker>
                </span>
            </Tooltip>
            {rule}
            {action('outdent', <MenuUnfoldOutlined />,
                () => onStyle({ indent: Math.max(0, (style?.indent ?? 0) - INDENT_STEP) }),
                { disabled: picked === null || (style?.indent ?? 0) === 0 })}
            {action('indent', <MenuFoldOutlined />,
                () => onStyle({ indent: Math.min(MAX_INDENT, (style?.indent ?? 0) + INDENT_STEP) }),
                { disabled: picked === null || (style?.indent ?? 0) >= MAX_INDENT })}
            <span className={styles.pending} />
            <Popconfirm
                cancelText={t('browser.module.edit_keep_editing')}
                disabled={!dirty}
                okText={t('browser.module.edit_discard')}
                onConfirm={onCancel}
                title={t('browser.module.edit_discard_question')}
            >
                <Tooltip title={t('browser.module.edit_close')}>
                    <Button
                        className={styles.button}
                        data-testid="table-edit-cancel"
                        icon={<CloseOutlined />}
                        onClick={dirty ? undefined : onCancel}
                        size="small"
                        type="text"
                    />
                </Tooltip>
            </Popconfirm>
        </div>
    )
}

export default TableEditToolbar
