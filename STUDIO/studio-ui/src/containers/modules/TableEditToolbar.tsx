import React, { useMemo, useSyncExternalStore } from 'react'
import {
    AlignCenterOutlined,
    AlignLeftOutlined,
    AlignRightOutlined,
    BgColorsOutlined,
    BoldOutlined,
    ClearOutlined,
    CloseOutlined,
    DeleteColumnOutlined,
    DeleteRowOutlined,
    FontColorsOutlined,
    InsertRowBelowOutlined,
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
import { useTheme } from 'antd-style'
import { useTranslation } from 'react-i18next'
import { MARK_COLORS, markInksOf } from 'components/textMarks'
import type { RawCellColor, RawCellStyleInput, RawTableCell } from 'types/tables'
import type { CellAt } from './tableEdits'
import { CellColourPicker } from './CellColourPicker'
import type { MarkerSlot } from './OpenCell'
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
    /**
     * Whether the look of a cell is the reader's to set: the alignment, the font and the colours. It is where the
     * formatting is both kept and seen: tables are not formatted on save, and the Excel formatting is shown.
     */
    formatting: boolean
    /**
     * Whether the indent is the reader's to set: wherever the look is, and in every TBasic and ColumnMatch table,
     * which read their structure from it.
     */
    indenting: boolean
    /** Whether pieces of a cell text can be marked: in a table of an `.xlsx` workbook, not of an `.xls` one. */
    marking: boolean
    /** Where the open cell leaves how to mark the selected piece of its text; empty while it holds none to mark. */
    markers: MarkerSlot
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
 * font, colours and indent — in one strip, in that order, with a rule between each group. The marks on a piece of a
 * cell text close it.
 *
 * <p>Every action here changes the table on screen alone. Nothing reaches the server until the reader saves,
 * and then all of it goes at once.
 *
 * <p>The actions on the look of a cell are offered only where the reader's formatting is kept and seen; elsewhere
 * the save would write the OpenL table style over it, or the screen would not show it. The indent stays for the tables
 * whose structure it is.
 *
 * <p>The marks act on the text being written in the open cell, on the piece of it the reader selected. Pressing one
 * keeps the focus in the cell, so that piece stays selected.
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
    formatting,
    indenting,
    marking,
    markers,
}) => {
    const { t } = useTranslation('repository')
    const { styles, cx } = useStyles()
    const token = useTheme()
    const inks = useMemo(() => markInksOf(token, [token.colorBgContainer]), [token])
    // Read here rather than handed down, so the table is not drawn again each time a cell is opened or closed.
    const onMark = useSyncExternalStore(markers.subscribe, markers.get)

    const row = picked?.row ?? -1
    const style = cell?.style

    const action = (
        key: string,
        icon: React.ReactNode,
        onClick: () => void,
        options: { disabled?: boolean, on?: boolean, why?: string | null, keepFocus?: boolean } = {}
    ) => (
        // A button that is off says why it is off, so the reader is not left guessing at a grey icon.
        <Tooltip key={key} title={options.disabled && options.why ? options.why : t(`browser.module.edit_${key}`)}>
            <Button
                aria-label={t(`browser.module.edit_${key}`)}
                className={cx(styles.button, options.on && styles.on)}
                data-testid={`table-edit-${key}`}
                disabled={options.disabled ?? picked === null}
                icon={icon}
                onClick={onClick}
                // An action on the text being written leaves the focus, and the selection, in the open cell.
                onMouseDown={options.keepFocus ? event => event.preventDefault() : undefined}
                size="small"
                type="text"
            />
        </Tooltip>
    )

    /** A colour of the picked cell. */
    const colour = (key: string, icon: React.ReactNode, styled: (chosen: RawCellColor) => RawCellStyleInput) => (
        <CellColourPicker
            className={styles.button}
            disabled={picked === null}
            icon={icon}
            onPick={chosen => onStyle(styled(chosen))}
            onPreview={chosen => onPreview(chosen === null ? null : styled(chosen))}
            testId={`table-edit-${key}`}
            title={t(`browser.module.edit_${key}`)}
        />
    )

    const rule = <span className={styles.rule} />

    /** How the marks are offered: only while a cell holding a text is open. */
    const marked = { disabled: onMark === null, why: t('browser.module.edit_mark_open_cell'), keepFocus: true }

    /**
     * Why an action is off, for the one the table's header stands in the way of.
     *
     * <p>The header is one cell banked across the table, and OpenL finds the table by the corner it starts in.
     * A column laid down before the first one, or taken away from under the header, leaves that corner where it
     * is — the bank widens or narrows over it. Taking the header's own row away does not.
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
            {action('insert_row', <InsertRowBelowOutlined />, onInsertRow)}
            {action('remove_row', <DeleteRowOutlined />, onRemoveRow,
                { disabled: picked === null || row < 1, why: off('browser.module.edit_header_row_kept') })}
            {rule}
            {action('insert_column', <InsertRowLeftOutlined />, onInsertColumn, {
                disabled: picked === null || !whole,
                why: off('browser.module.edit_whole_table'),
            })}
            {action('remove_column', <DeleteColumnOutlined />, onRemoveColumn)}
            {formatting && (
                <>
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
                    {colour('fill_colour', <BgColorsOutlined />, chosen => ({ background: chosen }))}
                    {colour('font_colour', <FontColorsOutlined />, chosen => ({ color: chosen }))}
                </>
            )}
            {indenting && (
                <>
                    {rule}
                    {action('outdent', <MenuUnfoldOutlined />,
                        () => onStyle({ indent: Math.max(0, (style?.indent ?? 0) - INDENT_STEP) }),
                        { disabled: picked === null || (style?.indent ?? 0) === 0 })}
                    {action('indent', <MenuFoldOutlined />,
                        () => onStyle({ indent: Math.min(MAX_INDENT, (style?.indent ?? 0) + INDENT_STEP) }),
                        { disabled: picked === null || (style?.indent ?? 0) >= MAX_INDENT })}
                </>
            )}
            {marking && (
                <>
                    {rule}
                    {MARK_COLORS.map(color => action(`mark_${color}`,
                        <span style={{ color: onMark === null ? undefined : inks[color], fontWeight: 700 }}>A</span>,
                        () => onMark?.(color), marked))}
                    {action('mark_clear', <ClearOutlined />, () => onMark?.(null), marked)}
                </>
            )}
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
