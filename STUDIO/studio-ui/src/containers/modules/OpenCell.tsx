import React, { useEffect, useMemo, useRef, useState } from 'react'
import { Button, Dropdown, Popover, Tooltip } from 'antd'
import { ClearOutlined } from '@ant-design/icons'
import { useTheme } from 'antd-style'
import { useTranslation } from 'react-i18next'
import { MARK_COLORS, markInksOf, markRange } from 'components/textMarks'
import type { RawTextMark, RawTextMarkColor } from 'types/tables'
import type { TableCellEditor } from '../../services/modules'
import { CellValueEditor, type EditorKind } from './CellValueEditor'
import { MarkedTextField, selectionIn, type TextSelection } from './MarkedTextField'
import { RANGE_PANEL, RangeEditor } from './RangeEditor'
import { paperToken } from '../../styles/paper'
import { useStyles } from './TableEditor.styles'

interface OpenCellProps {
    /** What the cell held when it was opened, which is where the writing starts from. */
    from: string
    /** Whether the cell takes more than one line on screen, measured as it was opened. */
    several: boolean
    /** The way the cell asks to be written, or null when it asks for nothing of its own. */
    own: EditorKind | null
    /** What the cell asks for: the values to choose from, the bounds to stay within. */
    asked: TableCellEditor | undefined
    /** The cell's address in the workbook, which tells a click inside it from one outside. */
    address: string | undefined
    /** The marks on the text the cell held when it was opened. */
    marks: RawTextMark[]
    /** Whether pieces of the text can be marked: a text of a cell the table was read with, not its header. */
    markable: boolean
    /** Keeps what was written, with the marks on it where the text can be marked, and closes the cell. */
    onCommit: (value: string, marks?: RawTextMark[]) => void
    /** Closes the cell, leaving it as it was. */
    onCancel: () => void
    /** Told while the reader has the menu of other ways open, which is not them leaving the cell. */
    onSwitching: (open: boolean) => void
    /** Whether the table is drawn on the paper of a workbook, which the field then lies on too. */
    onPaper: boolean
}

/**
 * The cell the reader is writing in.
 *
 * <p>What is being written lives here rather than in the table. A table draws every cell it holds on every
 * render, so a keystroke kept in the table would redraw all of them — thousands, for a table read whole — and
 * the tab would answer a key at a time. Here a keystroke redraws the one cell it is typed into.
 */
export const OpenCell: React.FC<OpenCellProps> = ({
    from, marks: fromMarks, markable, several, own, asked, address, onCommit, onCancel, onSwitching, onPaper,
}) => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles(onPaper)
    const token = useTheme()
    // The field lies on the paper of the workbook where the table is drawn in its formatting.
    const ground = onPaper ? paperToken() : token
    const inks = useMemo(() => markInksOf(ground, [ground.colorBgContainer]), [ground])
    const [draft, setDraft] = useState(from)
    const [marks, setMarks] = useState(fromMarks)
    // Where the selection is put once a mark is laid, which draws the text again.
    const [selection, setSelection] = useState<TextSelection | null>(null)
    // The field the marked text is written in, which the selection a mark is laid over is read from.
    const field = useRef<HTMLDivElement | null>(null)
    // Set while a mark swaps the field a text is typed in for the one that shows marks, or back: the field it leaves
    // is not the reader leaving the cell.
    const marking = useRef(false)
    // The way the reader chose to write the cell, when it is not the way the cell asks for.
    const [switched, setSwitched] = useState<EditorKind | null>(null)
    // Whether the reader has asked for the panel the bounds of a range are entered in.
    const [rangeOpen, setRangeOpen] = useState(false)

    /**
     * The way the cell is written.
     *
     * <p>What the reader switched to wins. Otherwise a value that is a formula or runs over several lines is
     * written as text, whatever the cell's type would ask for — those two follow the value being written, not
     * the table as it was compiled, so they are decided here rather than by the server.
     */
    const kindOf = (): EditorKind => {
        if (switched !== null) {
            return switched
        }
        if (draft.startsWith('=')) {
            return 'formula'
        }
        if (draft.includes('\n') || several) {
            return 'multiline'
        }
        return own ?? 'text'
    }

    const kind = kindOf()
    const marked = markable && (kind === 'text' || kind === 'multiline' || kind === 'formula')

    /** Keeps what was written, with its marks where the text can be marked. */
    const commit = (value: string) => {
        if (!marking.current) {
            onCommit(value, marked ? marks : undefined)
        }
    }

    /** Lays a mark over the selected piece of the text, or takes the marks off it. */
    const mark = (color: RawTextMarkColor | null) => {
        const active = document.activeElement
        const typed = active instanceof HTMLInputElement || active instanceof HTMLTextAreaElement
            ? { from: active.selectionStart ?? 0, to: active.selectionEnd ?? 0 }
            : null
        const at = field.current === null ? typed : selectionIn(field.current)
        if (at === null || at.from === at.to) {
            return
        }
        const next = markRange(marks, at.from, at.to, color)
        // A text that comes to hold marks, or loses the last of them, is written in the other field from now on.
        marking.current = (marks.length > 0) !== (next.length > 0)
        setSelection(at)
        setMarks(next)
    }

    // The field that shows marks has taken the place of the one the text was typed in, and has the focus now.
    useEffect(() => {
        marking.current = false
    }, [marks])

    // A field the reader writes in closes the cell by losing the focus. The bounds of a range are entered in a
    // panel of its own, and going to that panel takes the focus out of the field — so what closes a range cell
    // is the click that lands outside both, the way the old editor closed its panel. The panel itself waits to
    // be asked for: opening a cell opens its field, and the field drops the panel when the reader goes to it.
    useEffect(() => {
        if (kind !== 'range' || address === undefined) {
            return undefined
        }
        const away = (event: MouseEvent) => {
            const target = event.target instanceof Element ? event.target : null
            const inPanel = target?.closest(`.${RANGE_PANEL}`) != null
            const inCell = target?.closest(`[data-cell="${address}"]`) != null
            if (target !== null && !inPanel && !inCell) {
                // Closed the way every cell is closed, which leaves the menu that picks how to write it
                // alone: that menu opens beside the cell rather than in it, and choosing from it must not
                // take the cell away. What was entered in the panel is written by Done alone.
                onCancel()
            }
        }
        document.addEventListener('mousedown', away)
        return () => document.removeEventListener('mousedown', away)
    }, [address, kind, onCancel])

    /** The other ways this cell can be written, which the reader picks from beside it. */
    const switches = useMemo(() => {
        // A cell can always be written as a formula, whatever it holds now — as the old editor offered it.
        // The way the cell asks for comes first, and is not offered twice when it is one of those three.
        const others = [...new Set<EditorKind>([...(own === null ? [] : [own]), 'formula', 'multiline', 'text'])]
        // What the menu is for is said once, over the ways of writing it offers — as the Editor said it.
        return [{
            key: 'switch-to',
            type: 'group' as const,
            label: t('browser.module.editor_switch_to'),
            children: others
                .filter(other => other !== kind)
                .map(other => ({
                    key: other,
                    label: t(`browser.module.editor_kind_${other}`),
                    onClick: () => setSwitched(other),
                })),
        }]
    }, [kind, own, t])

    const inCell = (
        // Another way of writing the value is asked for with the right button, where the Editor asked
        // for it: a button of its own beside the field would widen the cell, and a table whose columns
        // move as a cell is opened is a table the reader loses their place in.
        <Dropdown menu={{ items: switches }} onOpenChange={onSwitching} trigger={['contextMenu']}>
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
                {marked && marks.length > 0 ? (
                    <MarkedTextField
                        className={styles.markedInput}
                        fieldRef={field}
                        inks={inks}
                        marks={marks}
                        multiline={kind === 'multiline'}
                        onCancel={onCancel}
                        onCommit={() => commit(draft)}
                        onSwitch={() => setSwitched('multiline')}
                        selection={selection}
                        value={draft}
                        onChange={(value, written) => {
                            setDraft(value)
                            setMarks(written)
                        }}
                    />
                ) : (
                    <CellValueEditor
                        asked={asked}
                        className={styles.input}
                        kind={kind}
                        onCancel={onCancel}
                        onChange={setDraft}
                        onCommit={value => commit(value ?? draft)}
                        onSwitch={setSwitched}
                        value={draft}
                    />
                )}
            </div>
        </Dropdown>
    )

    // The marks are offered above the cell, where they cover no other cell of its row. Pressing one keeps the
    // focus in the field, so the selection it marks stays where the reader put it.
    const bar = (
        <div aria-label={t('browser.module.marks_bar')} className={styles.marksBar} role="toolbar">
            {MARK_COLORS.map(color => (
                <Tooltip key={color} title={t(`browser.module.mark_${color}`)}>
                    <Button
                        aria-label={t(`browser.module.mark_${color}`)}
                        data-testid={`table-cell-mark-${color}`}
                        icon={<span style={{ color: inks[color], fontWeight: 700 }}>A</span>}
                        onClick={() => mark(color)}
                        onMouseDown={event => event.preventDefault()}
                        size="small"
                        type="text"
                    />
                </Tooltip>
            ))}
            <Tooltip title={t('browser.module.mark_clear')}>
                <Button
                    aria-label={t('browser.module.mark_clear')}
                    data-testid="table-cell-mark-clear"
                    icon={<ClearOutlined />}
                    onClick={() => mark(null)}
                    onMouseDown={event => event.preventDefault()}
                    size="small"
                    type="text"
                />
            </Tooltip>
        </div>
    )

    if (marked) {
        return (
            <Popover open arrow={false} content={bar} placement="topLeft" trigger={[]}>
                {inCell}
            </Popover>
        )
    }

    // The bounds of a range are entered under the cell rather than in the cell, the way the old editor
    // dropped its panel there — and the cell keeps the way out to writing it as text.
    return kind !== 'range' ? inCell : (
        <Popover
            open={rangeOpen}
            placement="bottomLeft"
            trigger={[]}
            content={(
                <RangeEditor
                    intOnly={asked?.entryEditor === 'integer'}
                    onWrite={entered => onCommit(entered)}
                    value={draft}
                />
            )}
        >
            {inCell}
        </Popover>
    )
}
