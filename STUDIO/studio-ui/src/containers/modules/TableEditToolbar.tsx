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
    FormatPainterOutlined,
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
    /** Whether the table theme styles the table: every table but a table of the kind Other. */
    themeable?: boolean | undefined
    /** Whether the reader chose to write the table theme into the table. */
    theme?: boolean | undefined
    /** Chooses to write the table theme into the table when the reader saves, with no other change. */
    onTheme?: (() => void) | undefined
    /** Whether the module waits to be verified, so its tables are not compiled as they stand and take no theme. */
    verifyNeeded?: boolean | undefined
    /**
     * Whether the formatting of a cell can be changed: its alignment, its font and its colours. It can where the table
     * is shown in the formatting of its Excel file (**Show Original Excel Formatting** in My Settings); otherwise the
     * table is shown formatted with the table theme, which would hide the change. The indent can always be changed: it
     * sets out the structure of a table, such as the steps of a TBasic algorithm.
     */
    formattable?: boolean | undefined
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
 *
 * <p>The table theme is applied on its own. While it is chosen, every action that changes the table is off; while the
 * table holds edits, the theme is. Each says why.
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
    themeable = false,
    theme = false,
    onTheme,
    verifyNeeded = false,
    formattable = true,
}) => {
    const { t } = useTranslation('repository')
    const { styles, cx } = useStyles()

    const row = picked?.row ?? -1
    const style = cell?.style

    const action = (
        key: string,
        icon: React.ReactNode,
        onClick: () => void,
        options: { disabled?: boolean, on?: boolean, why?: string | null } = {}
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

    // Why the actions that change the table are off, or null while the theme is not chosen.
    const themed = theme ? t('browser.module.edit_theme_alone') : null

    /**
     * Why the theme cannot be chosen, or null when it can. The theme is laid out by the table as it was saved and
     * compiled, so a table holding edits of the reader takes none, and nor does one of a module that waits to be
     * verified.
     */
    const themeOff = (): string | null => {
        // A theme chosen stays chosen: Undo takes it back.
        if (theme) {
            return null
        }
        if (dirty) {
            return t('browser.module.edit_theme_after_edits')
        }
        return verifyNeeded ? t('browser.module.theme_verify_first') : null
    }
    const noTheme = themeOff()

    /** An action that changes the table, which is off while the theme is chosen. */
    const edit: typeof action = (key, icon, onClick, options = {}) =>
        action(key, icon, onClick, themed === null ? options : { ...options, disabled: true, why: themed })

    // Why the formatting of a cell cannot be changed, or null when it can.
    const unformattable = formattable ? null : t('browser.module.edit_format_with_excel')

    /** An action that formats the picked cell, which is off where the table is shown formatted with the theme. */
    const format: typeof action = (key, icon, onClick, options = {}) =>
        edit(key, icon, onClick, unformattable === null ? options : { ...options, disabled: true, why: unformattable })

    /** A colour of the picked cell, which is off where every formatting action is. */
    const colour = (key: string, icon: React.ReactNode, styled: (chosen: string) => RawCellStyleInput,
        value: string) => (
        <CellColourPicker
            className={styles.button}
            disabled={themed !== null || unformattable !== null || picked === null}
            icon={icon}
            onPick={chosen => onStyle(styled(chosen))}
            onPreview={chosen => onPreview(chosen === null ? null : styled(chosen))}
            testId={`table-edit-${key}`}
            title={themed ?? unformattable ?? t(`browser.module.edit_${key}`)}
            value={value}
        />
    )

    const rule = <span className={styles.rule} />

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
            {edit('insert_row', <InsertRowBelowOutlined />, onInsertRow)}
            {edit('remove_row', <DeleteRowOutlined />, onRemoveRow,
                { disabled: picked === null || row < 1, why: off('browser.module.edit_header_row_kept') })}
            {rule}
            {edit('insert_column', <InsertRowLeftOutlined />, onInsertColumn, {
                disabled: picked === null || !whole,
                why: off('browser.module.edit_whole_table'),
            })}
            {edit('remove_column', <DeleteColumnOutlined />, onRemoveColumn)}
            {rule}
            {format('align_left', <AlignLeftOutlined />, () => onStyle({ align: 'left' }),
                { on: style?.align === undefined || style.align === 'left' })}
            {format('align_center', <AlignCenterOutlined />, () => onStyle({ align: 'center' }),
                { on: style?.align === 'center' })}
            {format('align_right', <AlignRightOutlined />, () => onStyle({ align: 'right' }),
                { on: style?.align === 'right' })}
            {rule}
            {format('bold', <BoldOutlined />, () => onStyle({ bold: !style?.bold }), { on: !!style?.bold })}
            {format('italic', <ItalicOutlined />, () => onStyle({ italic: !style?.italic }),
                { on: !!style?.italic })}
            {format('underline', <UnderlineOutlined />, () => onStyle({ underline: !style?.underline }),
                { on: !!style?.underline })}
            {rule}
            {colour('fill_colour', <BgColorsOutlined />, chosen => ({ background: chosen }),
                style?.background ?? '#ffffff')}
            {colour('font_colour', <FontColorsOutlined />, chosen => ({ color: chosen }), style?.color ?? '#000000')}
            {rule}
            {edit('outdent', <MenuUnfoldOutlined />,
                () => onStyle({ indent: Math.max(0, (style?.indent ?? 0) - INDENT_STEP) }),
                { disabled: picked === null || (style?.indent ?? 0) === 0 })}
            {edit('indent', <MenuFoldOutlined />,
                () => onStyle({ indent: Math.min(MAX_INDENT, (style?.indent ?? 0) + INDENT_STEP) }),
                { disabled: picked === null || (style?.indent ?? 0) >= MAX_INDENT })}
            {themeable && (
                <>
                    {rule}
                    {/* The theme is written into the whole table, so it needs no cell picked. */}
                    {action('theme', <FormatPainterOutlined />, () => onTheme?.(),
                        { disabled: noTheme !== null, on: theme, why: noTheme })}
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
