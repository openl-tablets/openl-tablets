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
import { Button, Dropdown, Tooltip } from 'antd'
import { useTranslation } from 'react-i18next'
import type { RawCellStyleInput, RawTableCell, TableThemeOption } from 'types/tables'
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
    /** The table themes the reader may write into the table, the primary one first; absent for none. */
    themes?: TableThemeOption[] | undefined
    /** The theme the reader chose to write into the table, or null when they chose none. */
    theme?: string | null | undefined
    /** Chooses a theme to write into the table when the reader saves, with no other change. */
    onTheme?: ((theme: string) => void) | undefined
    /** Whether the module waits to be verified, so its tables are not compiled as they stand and take no theme. */
    verifyNeeded?: boolean | undefined
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
 * <p>A table theme is applied on its own. While a theme is chosen, every action that changes the table is off; while
 * the table holds edits, the theme is. Each says why.
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
    themes,
    theme,
    onTheme,
    verifyNeeded = false,
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

    // Why the actions that change the table are off, or null while no theme is chosen.
    const themed = theme ? t('browser.module.edit_theme_alone') : null

    /**
     * Why no theme can be chosen, or null when one can. A theme is laid out by the table as it was saved and compiled,
     * so a table holding edits of the reader takes none, and nor does one of a module that waits to be verified.
     */
    const themeOff = (): string | null => {
        // A theme chosen can always be changed for another.
        if (theme) {
            return null
        }
        if (dirty) {
            return t('browser.module.edit_theme_after_edits')
        }
        return verifyNeeded ? t('browser.module.theme_verify_first') : null
    }
    const noTheme = themeOff()

    /** An action that changes the table, which is off while a theme is chosen. */
    const edit: typeof action = (key, icon, onClick, options = {}) =>
        action(key, icon, onClick, themed === null ? options : { ...options, disabled: true, why: themed })

    /** A colour of the picked cell, which is off while a theme is chosen, as every action changing the table is. */
    const colour = (key: string, icon: React.ReactNode, styled: (chosen: string) => RawCellStyleInput,
        value: string) => (
        <CellColourPicker
            className={styles.button}
            disabled={themed !== null || picked === null}
            icon={icon}
            onPick={chosen => onStyle(styled(chosen))}
            onPreview={chosen => onPreview(chosen === null ? null : styled(chosen))}
            testId={`table-edit-${key}`}
            title={themed ?? t(`browser.module.edit_${key}`)}
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
            {edit('align_left', <AlignLeftOutlined />, () => onStyle({ align: 'left' }),
                { on: style?.align === undefined || style.align === 'left' })}
            {edit('align_center', <AlignCenterOutlined />, () => onStyle({ align: 'center' }),
                { on: style?.align === 'center' })}
            {edit('align_right', <AlignRightOutlined />, () => onStyle({ align: 'right' }),
                { on: style?.align === 'right' })}
            {rule}
            {edit('bold', <BoldOutlined />, () => onStyle({ bold: !style?.bold }), { on: !!style?.bold })}
            {edit('italic', <ItalicOutlined />, () => onStyle({ italic: !style?.italic }),
                { on: !!style?.italic })}
            {edit('underline', <UnderlineOutlined />, () => onStyle({ underline: !style?.underline }),
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
            {themes !== undefined && themes.length > 0 && (
                <>
                    {rule}
                    {/* A theme is written into the whole table, so it needs no cell picked. */}
                    <Dropdown
                        trigger={['click']}
                        menu={{
                            items: themes.map(option => ({ key: option.id, label: option.name })),
                            onClick: ({ key }) => onTheme?.(key),
                            selectable: true,
                            selectedKeys: theme ? [theme] : [],
                        }}
                    >
                        {/* The menu opens on a click, so the button itself does nothing more. A button that is off
                            takes no click, so it opens nothing, and its tooltip still says why. */}
                        {action('theme', <FormatPainterOutlined />, () => undefined,
                            { disabled: noTheme !== null, on: !!theme, why: noTheme })}
                    </Dropdown>
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
