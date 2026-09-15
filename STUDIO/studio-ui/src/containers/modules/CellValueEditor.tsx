import React, { useMemo } from 'react'
import { DatePicker, Input, InputNumber, Select } from 'antd'
import dayjs from 'dayjs'
// A date typed into a cell is read by the format the workbook writes, and nothing else: without this dayjs
// falls back to guessing, and 13/01/2024 is taken for a date rather than refused.
import customParseFormat from 'dayjs/plugin/customParseFormat'
import type { TableCellEditor } from '../../services/modules'
import { joinValues, splitValues } from './multiValue'
import { numberOnly } from './numberOnly'

/** How a value is being written, which is not always the way the cell asks for it. */
export type EditorKind =
    | 'text'
    | 'multiline'
    | 'combo'
    | 'multiselect'
    | 'numeric'
    | 'date'
    | 'boolean'
    | 'array'
    /** Entered in the panel under the cell rather than typed into it. */
    | 'range'
    /** The formula the cell is written with, entered as Excel writes it: `=B2*C2`. */
    | 'formula'

interface CellValueEditorProps {
    /** The way the value is being written. */
    kind: EditorKind
    /** What the cell asks for, with the values to choose from and the bounds to stay within. */
    asked: TableCellEditor | undefined
    value: string
    onChange: (value: string) => void
    /**
     * Keeps what was written and closes the cell.
     *
     * <p>A value given here is what is kept, for an editor that writes and closes in one gesture — picking a
     * date from the calendar — where what the field reported a moment ago has not reached the screen yet.
     */
    onCommit: (value?: string) => void
    /** Leaves the cell as it was. */
    onCancel: () => void
    /** Asked for another way of writing the value, which Alt+Enter asks for from a one-line field. */
    onSwitch?: ((kind: EditorKind) => void) | undefined
    className?: string
}

dayjs.extend(customParseFormat)

/**
 * The formats a date cell is read in, in the order OpenL reads them (`String2DateConvertor`).
 *
 * <p>Tried longest first, so a value carrying a time is not cut down to its day by a shorter pattern matching
 * the head of it. A date the reader picks is written back in the format the cell already held, so opening a
 * cell and closing it does not quietly rewrite what its author put there.
 */
const DATE_SHAPES = [
    'M/D/YYYY h:mm A',
    'M/D/YYYY H:mm:ss',
    'M/D/YYYY H:mm',
    'M/D/YYYY',
    'YYYY-M-D[T]H:mm:ss.SSS',
    'YYYY-M-D[T]H:mm:ss',
    'YYYY-M-D[T]H:mm',
    'YYYY-M-D',
    'M/D/YY',
]

/**
 * The same shapes with the month, day and hour written out in two digits.
 *
 * <p>Java reads `3` and `03` under one pattern; dayjs matches a token's width exactly, so `2024-03-07` is
 * refused by the pattern that reads `2024-3-7`. Each shape is therefore tried both ways.
 */
const widened = (shape: string) => shape.replace(/(?<![MDHh])([MDHh])(?![MDHh])/g, '$1$1')

const DATE_FORMATS = DATE_SHAPES.flatMap(shape => [shape, widened(shape)])

/** What a date the reader picks is written in, where the cell held nothing to follow. */
const DATE_FORMAT = 'M/D/YYYY'

/** The date the cell holds and the format it is written in, or nothing where it holds no date. */
const dateOf = (text: string) => {
    for (const format of DATE_FORMATS) {
        const read = dayjs(text, format, true)
        if (read.isValid()) {
            return { read, format }
        }
    }
    return { read: null, format: DATE_FORMAT }
}

const BOOLEAN_CHOICES = [{ value: 'true', label: 'true' }, { value: 'false', label: 'false' }]

/** The values to choose from, shown by whatever wording the domain gives them. */
const choicesOf = (asked: TableCellEditor | undefined) =>
    (asked?.choices ?? []).map((choice, index) => ({
        value: choice,
        label: asked?.displayValues?.[index] ?? choice,
    }))

/**
 * What the reader writes into a cell with.
 *
 * <p>A cell that holds one of a known set of values is chosen from that set, a number is entered within the
 * bounds of its type, a date from a calendar — as the legacy editor offered them. Whatever the cell asks for,
 * the reader can always switch to writing it as plain text, and this draws that too.
 */
export const CellValueEditor: React.FC<CellValueEditorProps> = ({
    kind,
    asked,
    value,
    onChange,
    onCommit,
    onCancel,
    onSwitch,
    className,
}) => {
    const choices = useMemo(() => choicesOf(asked), [asked])
    const separator = asked?.separator ?? ','
    const numeric = useMemo(() => numberOnly(asked?.intOnly), [asked?.intOnly])

    /**
     * Enter keeps what was written and Escape leaves the cell as it was, wherever the reader is writing.
     *
     * <p>A field that holds several lines takes Enter for a line of its own, so there Ctrl+Enter is what keeps
     * what was written; and Alt+Enter is what asks a one-line field for the room to write several, as the old
     * editor did. F2 and F3 put the caret at the two ends of what is already there.
     */
    const keys = (event: React.KeyboardEvent) => {
        const several = kind === 'multiline'
        if (event.key === 'Enter' && event.altKey && !several) {
            event.preventDefault()
            onSwitch?.('multiline')
        } else if (event.key === 'Enter' && (several ? event.ctrlKey || event.metaKey : true)) {
            onCommit()
        } else if (event.key === 'Escape') {
            onCancel()
        } else if (event.key === 'F2' || event.key === 'F3') {
            // The two ends of what the cell already holds, which is where the old editor put the caret.
            const field = event.currentTarget
            if (field instanceof HTMLInputElement || field instanceof HTMLTextAreaElement) {
                event.preventDefault()
                const at = event.key === 'F2' ? 0 : field.value.length
                field.setSelectionRange(at, at)
            }
        }
    }

    const shared = {
        autoFocus: true,
        className: className ?? '',
        'data-testid': 'table-cell-input',
        size: 'small',
    } as const

    switch (kind) {
        case 'multiline':
            return (
                <Input.TextArea
                    {...shared}
                    autoSize={{ minRows: 2, maxRows: 10 }}
                    onBlur={() => onCommit()}
                    onChange={event => onChange(event.target.value)}
                    onKeyDown={keys}
                    value={value}
                />
            )
        case 'combo':
            return (
                <Select
                    {...shared}
                    allowClear
                    defaultOpen
                    showSearch
                    onBlur={() => onCommit()}
                    onChange={chosen => onChange(chosen ?? '')}
                    onInputKeyDown={keys}
                    options={choices}
                    popupMatchSelectWidth={false}
                    style={{ minWidth: 140 }}
                    value={value === '' ? undefined : value}
                />
            )
        case 'multiselect':
            return (
                <Select
                    {...shared}
                    defaultOpen
                    mode="multiple"
                    onBlur={() => onCommit()}
                    onChange={(chosen: string[]) => onChange(joinValues(chosen, separator, asked?.separatorEscaper))}
                    onInputKeyDown={keys}
                    options={choices}
                    popupMatchSelectWidth={false}
                    style={{ minWidth: 180 }}
                    value={splitValues(value, separator, asked?.separatorEscaper)}
                />
            )
        case 'boolean':
            return (
                <Select
                    {...shared}
                    allowClear
                    defaultOpen
                    onBlur={() => onCommit()}
                    onChange={chosen => onChange(chosen ?? '')}
                    onInputKeyDown={keys}
                    options={BOOLEAN_CHOICES}
                    style={{ minWidth: 100 }}
                    value={value === '' ? undefined : value}
                />
            )
        case 'numeric':
            return (
                <InputNumber
                    {...shared}
                    {...(asked?.max === undefined ? {} : { max: String(asked.max) })}
                    {...(asked?.min === undefined ? {} : { min: String(asked.min) })}
                    {...(asked?.intOnly ? { precision: 0 } : {})}
                    stringMode
                    onBlur={() => onCommit()}
                    onChange={entered => onChange(entered == null ? '' : String(entered))}
                    onPaste={numeric.onPaste}
                    value={value === '' ? null : value}
                    // A cell that holds a number takes nothing but a number: a key that cannot stand in one
                    // never reaches the field.
                    onKeyDown={event => {
                        numeric.onKeyDown(event)
                        keys(event)
                    }}
                />
            )
        case 'date': {
            const { read, format } = dateOf(value)
            return (
                <DatePicker
                    {...shared}
                    allowClear
                    defaultOpen
                    // Shown as the cell holds it, and read from anything OpenL would read, so a date typed in
                    // another of its formats is understood rather than thrown away.
                    format={[format, ...DATE_FORMATS]}
                    onKeyDown={keys}
                    // The calendar closes when a date is picked and when the reader clicks away from it; both
                    // leave the cell, as leaving the field did in the old editor.
                    onOpenChange={opened => {
                        if (!opened) {
                            onCommit()
                        }
                    }}
                    // A time is offered only where the cell already carries one: picking a day must neither
                    // invent a time nor drop the one its author wrote.
                    showTime={format.includes('H') || format.includes('h')}
                    value={read}
                    onChange={picked => {
                        const written = picked ? picked.format(format) : ''
                        onChange(written)
                        onCommit(written)
                    }}
                />
            )
        }
        case 'range':
            // The bounds are entered in the panel under the cell, so the cell itself only shows what they
            // come to — as the old editor did, where the field could not be typed into either.
            return <Input {...shared} readOnly onKeyDown={keys} value={value} />
        case 'array': {
            // A cell holding several numbers is written as they are read: the numbers with the separator
            // between them. Only what can stand in one reaches the field, the separator included.
            const entries = numberOnly(asked?.intOnly, separator)
            return (
                <Input
                    {...shared}
                    onBlur={() => onCommit()}
                    onChange={event => onChange(event.target.value)}
                    onPaste={entries.onPaste}
                    value={value}
                    onKeyDown={event => {
                        entries.onKeyDown(event)
                        keys(event)
                    }}
                />
            )
        }
        case 'formula':
        case 'text':
        default:
            return (
                <Input
                    {...shared}
                    onBlur={() => onCommit()}
                    onChange={event => onChange(event.target.value)}
                    onKeyDown={keys}
                    value={value}
                />
            )
    }
}

export default CellValueEditor
