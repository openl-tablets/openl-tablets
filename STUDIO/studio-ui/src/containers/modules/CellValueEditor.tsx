import React, { useMemo } from 'react'
import { DatePicker, Input, InputNumber, Select } from 'antd'
import dayjs from 'dayjs'
import type { TableCellEditor } from '../../services/modules'
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

interface CellValueEditorProps {
    /** The way the value is being written. */
    kind: EditorKind
    /** What the cell asks for, with the values to choose from and the bounds to stay within. */
    asked: TableCellEditor | undefined
    value: string
    onChange: (value: string) => void
    /** Keeps what was written and closes the cell. */
    onCommit: () => void
    /** Leaves the cell as it was. */
    onCancel: () => void
    className?: string
}

/** How a date is written into a cell, which is how the workbook reads it back. */
const DATE_FORMAT = 'MM/DD/YYYY'

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
    className,
}) => {
    const choices = useMemo(() => choicesOf(asked), [asked])
    const separator = asked?.separator ?? ','
    const numeric = useMemo(() => numberOnly(asked?.intOnly), [asked?.intOnly])

    // Enter keeps what was written and Escape leaves the cell as it was, wherever the reader is writing.
    const keys = (event: React.KeyboardEvent) => {
        if (event.key === 'Enter' && kind !== 'multiline') {
            onCommit()
        } else if (event.key === 'Escape') {
            onCancel()
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
                    onBlur={onCommit}
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
                    onBlur={onCommit}
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
                    onBlur={onCommit}
                    onChange={(chosen: string[]) => onChange(chosen.join(separator))}
                    onInputKeyDown={keys}
                    options={choices}
                    popupMatchSelectWidth={false}
                    style={{ minWidth: 180 }}
                    value={value === '' ? [] : value.split(separator).map(one => one.trim())}
                />
            )
        case 'boolean':
            return (
                <Select
                    {...shared}
                    allowClear
                    defaultOpen
                    onBlur={onCommit}
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
                    onBlur={onCommit}
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
            const written = dayjs(value, DATE_FORMAT, true)
            return (
                <DatePicker
                    {...shared}
                    open
                    format={DATE_FORMAT}
                    onKeyDown={keys}
                    value={written.isValid() ? written : null}
                    onChange={picked => {
                        onChange(picked ? picked.format(DATE_FORMAT) : '')
                        onCommit()
                    }}
                />
            )
        }
        case 'range':
            // The bounds are entered in the panel under the cell, so the cell itself only shows what they
            // come to — as the old editor did, where the field could not be typed into either.
            return <Input {...shared} readOnly onKeyDown={keys} value={value} />
        case 'array':
        case 'text':
        default:
            return (
                <Input
                    {...shared}
                    onBlur={onCommit}
                    onChange={event => onChange(event.target.value)}
                    onKeyDown={keys}
                    value={value}
                />
            )
    }
}

export default CellValueEditor
