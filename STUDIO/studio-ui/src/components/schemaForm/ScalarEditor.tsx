import React, { useState } from 'react'
import { DatePicker, Form, Input, InputNumber, Select } from 'antd'
import dayjs from 'dayjs'
import { useTranslation } from 'react-i18next'
import type { FieldKind, JsonSchema } from './schema'

/** The text a value is edited as. A value with no text of its own is edited from empty. */
const asText = (value: unknown): string => {
    if (typeof value === 'string') {
        return value
    }
    if (typeof value === 'number' || typeof value === 'boolean' || typeof value === 'bigint') {
        return String(value)
    }
    return ''
}

const DATE_FORMAT = 'YYYY-MM-DD'
const DATE_TIME_FORMAT = 'YYYY-MM-DDTHH:mm:ss'

export interface ScalarEditorProps {
    kind: FieldKind
    schema: JsonSchema
    value: unknown
    /** Path of the field inside the parameter. Tests find the control by it. */
    path: string
    /** Reports the new value. A cleared field reports `undefined`. */
    onChange: (value: unknown) => void
    /** Reports that the editing is over. The control lost focus or a choice was made. */
    onDone: () => void
}

/**
 * Edits a value the schema says nothing about as JSON text. Any value the rule accepts can be typed in.
 *
 * Text that is not JSON is not a value, so the field stays open until it reads as one or is cleared. Closing
 * on it would take the reason away and leave the value the field held before, as though nothing was typed.
 */
const JsonEditor: React.FC<Omit<ScalarEditorProps, 'kind' | 'schema'>> = ({ value, path, onChange, onDone }) => {
    const { t } = useTranslation('execution')
    const [text, setText] = useState(value === undefined || value === null ? '' : JSON.stringify(value))
    const [error, setError] = useState<string | null>(null)
    const update = (next: string) => {
        setText(next)
        if (next.trim() === '') {
            setError(null)
            onChange(undefined)
            return
        }
        try {
            onChange(JSON.parse(next))
            setError(null)
        } catch (parseError) {
            setError(t('input.jsonInvalid', { message: (parseError as Error).message }))
        }
    }
    return (
        <Form.Item style={{ marginBottom: 0 }} {...(error && { help: error, validateStatus: 'error' })}>
            <Input.TextArea
                autoFocus
                data-testid={`input-${path}`}
                onChange={event => update(event.target.value)}
                placeholder={t('input.jsonPlaceholder')}
                rows={2}
                size="small"
                style={{ minWidth: 260 }}
                value={text}
                onBlur={() => {
                    if (error === null) {
                        onDone()
                    }
                }}
            />
        </Form.Item>
    )
}

/**
 * Edits a date, with the time of day when the type asks for one.
 *
 * The editing ends with the chosen date or with the calendar closing, never with the blur: a click in the
 * calendar blurs the field before the date arrives.
 */
const DateEditor: React.FC<{
    value: unknown
    testId: string
    withTime: boolean
    onChange: (value: unknown) => void
    onDone: () => void
}> = ({ value, testId, withTime, onChange, onDone }) => {
    const format = withTime ? DATE_TIME_FORMAT : DATE_FORMAT
    const parsed = typeof value === 'string' ? dayjs(value) : null
    return (
        <DatePicker
            autoFocus
            data-testid={testId}
            onChange={next => onChange(next ? next.format(format) : undefined)}
            showTime={withTime}
            size="small"
            value={parsed?.isValid() ? parsed : null}
            onOpenChange={open => {
                if (!open) {
                    onDone()
                }
            }}
        />
    )
}

/**
 * The inline control a field is edited with.
 *
 * The control is chosen by the kind of value the schema describes. A text or number box, a choice for an
 * enumeration or a boolean, a calendar for a date, and JSON text for anything else.
 *
 * The control opens focused. A choice ends the editing at once. A typed value ends it when the control loses
 * focus or Enter is pressed.
 */
export const ScalarEditor: React.FC<ScalarEditorProps> = ({ kind, schema, value, path, onChange, onDone }) => {
    const { t } = useTranslation('execution')
    const testId = `input-${path}`
    const choose = (next: unknown) => {
        onChange(next ?? undefined)
        onDone()
    }
    switch (kind) {
        case 'enum':
            return (
                <Select
                    allowClear
                    autoFocus
                    defaultOpen
                    showSearch
                    data-testid={testId}
                    onBlur={onDone}
                    onChange={choose}
                    options={(schema.enum ?? []).map(option => ({ value: option as string | number, label: String(option) }))}
                    size="small"
                    style={{ minWidth: 160 }}
                    value={value === undefined || value === null ? undefined : value as string | number}
                />
            )
        case 'boolean':
            return (
                <Select
                    allowClear
                    autoFocus
                    defaultOpen
                    data-testid={testId}
                    onBlur={onDone}
                    onChange={choose}
                    size="small"
                    style={{ minWidth: 120 }}
                    value={typeof value === 'boolean' ? value : undefined}
                    options={[
                        { value: true, label: t('input.yes') },
                        { value: false, label: t('input.no') },
                    ]}
                />
            )
        case 'integer':
        case 'number':
            return (
                <InputNumber
                    autoFocus
                    data-testid={testId}
                    onBlur={onDone}
                    onChange={next => onChange(next ?? undefined)}
                    onPressEnter={onDone}
                    size="small"
                    style={{ minWidth: 160 }}
                    value={typeof value === 'number' ? value : null}
                    {...(kind === 'integer' && { precision: 0 })}
                />
            )
        case 'date':
        case 'datetime':
            return <DateEditor onChange={choose} onDone={onDone} testId={testId} value={value} withTime={kind === 'datetime'} />
        case 'unknown':
            return <JsonEditor onChange={onChange} onDone={onDone} path={path} value={value} />
        default:
            return (
                <Input
                    allowClear
                    autoFocus
                    data-testid={testId}
                    onBlur={onDone}
                    onChange={event => onChange(event.target.value === '' ? undefined : event.target.value)}
                    onPressEnter={onDone}
                    size="small"
                    style={{ minWidth: 200 }}
                    value={asText(value)}
                />
            )
    }
}

export default ScalarEditor
