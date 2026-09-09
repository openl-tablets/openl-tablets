import React, { lazy, Suspense, useCallback, useEffect, useState } from 'react'
import { Form, Radio, Skeleton, Space } from 'antd'
import { useTranslation } from 'react-i18next'
import { initialFormValue, SchemaForm, type SchemaFormParameter } from 'components/schemaForm/SchemaForm'
import type { TraceParameterValue } from 'types/trace'

// The editor the Projects tab opens a file with. It is loaded on first use, as CodeMirror is heavy.
const CodeEditor = lazy(() => import('containers/projects/CodeEditor').then(module => ({ default: module.CodeEditor })))

const PARAMS = 'params'
const RUNTIME_CONTEXT = 'runtimeContext'
/**
 * The form holds the context under a name no parameter can take, so a rule that declares a parameter called
 * `runtimeContext` keeps it. The name of the wire key is put back when the input is written.
 */
const CONTEXT_FIELD = 'runtime-context'

/** What the input collected so far amounts to. The JSON the execution APIs take, or why it cannot be built. */
export interface ParametersInputValue {
    inputJson: string
    error?: string | undefined
}

export interface ParametersInputProps {
    /** Declared parameters of the table, each with the schema of its values. */
    parameters: TraceParameterValue[]
    /** Schema of the runtime context, when the project provides one. */
    runtimeContext?: TraceParameterValue | null | undefined
    onChange: (value: ParametersInputValue) => void
}

type InputMode = 'form' | 'json'

const asRecord = (value: unknown): Record<string, unknown> =>
    value && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, unknown> : {}

/** The structured input the run and trace APIs read. The parameters go by name, the context only when it is set. */
const toInputJson = (value: Record<string, unknown>): string => {
    const { [CONTEXT_FIELD]: context, ...params } = value
    const runtimeContext = asRecord(context)
    return JSON.stringify({
        [PARAMS]: params,
        ...(Object.keys(runtimeContext).length > 0 && { [RUNTIME_CONTEXT]: runtimeContext }),
    }, null, 2)
}

/**
 * Collects the input of a rule table.
 *
 * The input is a form built from the schema of each parameter, or the same input as JSON text. The text suits a
 * request copied from a service log.
 *
 * The runtime context, when the project provides one, is the last line of the form, under the parameters the
 * rule declares.
 *
 * Switching to JSON shows what the form holds. Switching back reads the text into the form when it parses.
 */
export const ParametersInput: React.FC<ParametersInputProps> = ({ parameters, runtimeContext, onChange }) => {
    const { t } = useTranslation('execution')
    const formParameters: SchemaFormParameter[] = [
        ...parameters.map(parameter => ({
            name: parameter.name,
            type: parameter.description,
            schema: parameter.schema,
            value: parameter.value,
        })),
        ...(runtimeContext
            ? [{
                name: CONTEXT_FIELD,
                label: t('input.runtimeContext'),
                type: runtimeContext.description,
                schema: runtimeContext.schema,
            }]
            : []),
    ]
    const [mode, setMode] = useState<InputMode>('form')
    const [value, setValue] = useState<Record<string, unknown>>(() => initialFormValue(formParameters))
    const [text, setText] = useState('')
    const [error, setError] = useState<string | undefined>(undefined)

    const parseText = useCallback((json: string): { parsed?: unknown, error?: string } => {
        if (json.trim() === '') {
            return { parsed: {} }
        }
        try {
            return { parsed: JSON.parse(json) }
        } catch (parseError) {
            return { error: t('input.jsonInvalid', { message: (parseError as Error).message }) }
        }
    }, [t])

    useEffect(() => {
        if (mode === 'form') {
            onChange({ inputJson: toInputJson(value) })
        } else {
            onChange({ inputJson: text, error })
        }
    }, [mode, value, text, error, onChange])

    const switchMode = (next: InputMode) => {
        if (next === 'json') {
            setText(toInputJson(value))
            setError(undefined)
        } else {
            // The text is read back into the form when it parses. A text that does not parse is kept as it is, so
            // nothing typed is lost.
            const { parsed, error: parseError } = parseText(text)
            if (parseError) {
                return
            }
            const root = asRecord(parsed)
            const { [RUNTIME_CONTEXT]: context, ...rest } = root
            const params = PARAMS in root ? asRecord(root[PARAMS]) : rest
            setValue(runtimeContext ? { ...params, [CONTEXT_FIELD]: asRecord(context) } : params)
        }
        setMode(next)
    }

    const updateText = (json: string) => {
        setText(json)
        setError(parseText(json).error)
    }

    return (
        <Space orientation="vertical" size="small" style={{ width: '100%' }}>
            <Radio.Group
                onChange={event => switchMode(event.target.value as InputMode)}
                optionType="button"
                size="small"
                value={mode}
                options={[
                    { label: t('input.form'), value: 'form' },
                    { label: t('input.json'), value: 'json' },
                ]}
            />
            {mode === 'form' ? (
                <SchemaForm onChange={setValue} parameters={formParameters} value={value} />
            ) : (
                <Form.Item style={{ marginBottom: 0 }} {...(error && { help: error, validateStatus: 'error' })}>
                    <div data-testid="input-json" style={{ height: 280 }}>
                        <Suspense fallback={<Skeleton active paragraph={{ rows: 6 }} title={false} />}>
                            <CodeEditor onChange={updateText} path="input.json" value={text} />
                        </Suspense>
                    </div>
                </Form.Item>
            )}
        </Space>
    )
}

export default ParametersInput
