import React from 'react'
import { DatePicker, Input, InputNumber, Select } from 'antd'
import {
    datePickerFormatForLocale,
    ISO_DATE_FORMAT,
    parseDateValue,
} from '../tableModals/dateValue'
import type { TableCellValue } from './tableSkeletons'

const INTEGER_TYPES = new Set(['Integer', 'Long', 'Short', 'Byte', 'BigInteger'])
const DECIMAL_TYPES = new Set(['Double', 'Float', 'BigDecimal'])
const INTEGER_RANGES: Readonly<Record<string, readonly [bigint, bigint]>> = {
    Byte: [-128n, 127n],
    Short: [-32768n, 32767n],
    Integer: [-2147483648n, 2147483647n],
    Long: [-9223372036854775808n, 9223372036854775807n],
}
const INTEGER_VALUE = /^[+-]?\d+$/
const DECIMAL_VALUE = /^[+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:e[+-]?\d+)?$/i
const BOOLEAN_OPTIONS = [
    { label: 'TRUE', value: 'TRUE' },
    { label: 'FALSE', value: 'FALSE' },
]

interface TypedTableValueInputProps {
    'aria-label': string
    'data-testid': string
    /** Whether the cell holds a condition, which OpenL matches by range as readily as by equality. */
    condition: boolean
    onChange: (value: string) => void
    type: string | undefined
    value: TableCellValue
    vocabularyValues: Readonly<Record<string, string[]>>
}

/** Types that have a range of their own: OpenL compiles such a condition column as the matching range type. */
const RANGE_TYPES = new Set([...INTEGER_TYPES, ...DECIMAL_TYPES, 'Character', 'String', 'Date'])

/**
 * The type whose editor and validation the cell takes, or none when it takes free text.
 *
 * <p>A condition is matched by range as readily as by equality, so one of a range-capable type is left as text:
 * `18-30` is not a number and `[18 .. 30)` is not a day on a calendar. The column is compiled as `IntRange`,
 * `DoubleRange`, `CharRange`, `StringRange` or `DateRange`, and the shape of the expression is the engine's to
 * judge — it reports what it cannot read when the table compiles.
 */
const editorType = (type: string | undefined, condition: boolean): string => {
    const declared = type?.trim() ?? ''
    return condition && RANGE_TYPES.has(declared) ? '' : declared
}

/** Whether a value can be represented by the editor for its declared OpenL type. Empty is valid for every type. */
export const tableValueIsValid = (
    type: string | undefined,
    value: TableCellValue,
    vocabularyValues: Readonly<Record<string, string[]>>,
    condition = false
): boolean => {
    const declared = editorType(type, condition)
    const text = String(value ?? '').trim()
    if (!declared || !text) {
        return true
    }
    if (Object.hasOwn(vocabularyValues, declared)) {
        return vocabularyValues[declared]?.includes(text) ?? false
    }
    if (declared === 'Boolean') {
        return text === 'TRUE' || text === 'FALSE'
    }
    if (INTEGER_TYPES.has(declared)) {
        if (!INTEGER_VALUE.test(text)) {
            return false
        }
        const range = INTEGER_RANGES[declared]
        if (!range) {
            return true
        }
        const integer = BigInt(text)
        return integer >= range[0] && integer <= range[1]
    }
    if (DECIMAL_TYPES.has(declared)) {
        return DECIMAL_VALUE.test(text)
    }
    if (declared === 'Character') {
        return text.length === 1
    }
    if (declared === 'Date') {
        return parseDateValue(text) !== null
    }
    return true
}

/**
 * A cell editor constrained by the OpenL type declared for that cell.
 *
 * <p>Boolean and vocabulary values are finite selections. Numbers and dates use their native Ant Design editors.
 * Types without a finite single-cell representation keep a text input, which also covers references to Data rows.
 */
export const TypedTableValueInput: React.FC<TypedTableValueInputProps> = ({
    condition,
    onChange,
    type,
    value,
    vocabularyValues,
    ...common
}) => {
    const declared = editorType(type, condition)
    const vocabulary = Object.hasOwn(vocabularyValues, declared) ? vocabularyValues[declared] ?? [] : null
    if (vocabulary) {
        return (
            <Select
                {...common}
                allowClear
                onChange={next => onChange(next ?? '')}
                options={vocabulary.map(option => ({ label: option, value: option }))}
                showSearch={false}
                value={vocabulary.includes(String(value)) ? String(value) : undefined}
            />
        )
    }
    if (declared === 'Boolean') {
        const selected = BOOLEAN_OPTIONS.some(option => option.value === value) ? String(value) : undefined
        return (
            <Select
                {...common}
                allowClear
                onChange={next => onChange(next ?? '')}
                options={BOOLEAN_OPTIONS}
                value={selected}
            />
        )
    }
    if (INTEGER_TYPES.has(declared) || DECIMAL_TYPES.has(declared)) {
        const range = INTEGER_RANGES[declared]
        return (
            <InputNumber
                {...common}
                controls={false}
                {...(range ? { max: range[1].toString(), min: range[0].toString() } : {})}
                onChange={next => onChange(next === null ? '' : String(next))}
                {...(INTEGER_TYPES.has(declared) ? { precision: 0 } : {})}
                stringMode
                value={value === '' ? null : String(value)}
            />
        )
    }
    if (declared === 'Date') {
        return (
            <DatePicker
                {...common}
                format={datePickerFormatForLocale()}
                onChange={date => onChange(date ? date.format(ISO_DATE_FORMAT) : '')}
                style={{ width: '100%' }}
                value={parseDateValue(value)}
            />
        )
    }
    return (
        <Input
            {...common}
            maxLength={declared === 'Character' ? 1 : undefined}
            onChange={event => onChange(event.target.value)}
            value={String(value)}
        />
    )
}
