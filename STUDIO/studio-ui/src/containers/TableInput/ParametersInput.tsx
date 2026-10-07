import React, { lazy, Suspense, useCallback, useEffect, useMemo, useState } from 'react'
import { Form, Radio, Skeleton, Space } from 'antd'
import { useTranslation } from 'react-i18next'
import { initialFormValue, SchemaForm, type SchemaFormParameter } from 'components/schemaForm/SchemaForm'
import { primaryType, resolveSchema, type JsonSchema } from 'components/schemaForm/schema'
import type { TraceParameterValue } from 'types/trace'
import { RUNTIME_CONTEXT_LABELS } from './runtimeContextLabels'

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

interface ParametersInputProps {
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

/** The values a list gives the parameters, in the order they are declared. */
const byPosition = (values: unknown[], names: string[]): Record<string, unknown> =>
    Object.fromEntries(names.slice(0, values.length).map((name, i) => [name, values[i]]))

/** The values an object gives the parameters it names. A field that names no parameter is left out. */
const byName = (values: Record<string, unknown>, names: string[]): Record<string, unknown> =>
    Object.fromEntries(names.filter(name => Object.hasOwn(values, name)).map(name => [name, values[name]]))

/**
 * Reads input JSON into the values of the parameters, the way the run and trace APIs read it.
 *
 * The structured input lists the values under `params`, by name or in order. Without `params`, a list gives the
 * values in order, and an object gives them by name. A table with one parameter also takes the value itself:
 * an object that does not name that parameter is the parameter's value as a whole, as a request copied from a
 * service log usually is.
 *
 * The context is read from `runtimeContext`, and never from the value of the only parameter. Without `params`, a
 * parameter named `runtimeContext` takes that field, as it does on the server.
 *
 * Returns `null` for a list with more values than the table has parameters, which the run refuses as well.
 */
const readInput = (parsed: unknown, parameters: TraceParameterValue[]): {
    params: Record<string, unknown>
    context: unknown
} | null => {
    const names = parameters.map(parameter => parameter.name)
    const root = asRecord(parsed)
    const inOrder = (values: unknown[]) => (values.length > names.length ? null : byPosition(values, names))
    if (Object.hasOwn(root, PARAMS)) {
        const params = root[PARAMS]
        const values = Array.isArray(params) ? inOrder(params) : byName(asRecord(params), names)
        return values && { params: values, context: root[RUNTIME_CONTEXT] }
    }
    const only = parameters.length === 1 ? parameters[0] : undefined
    const schema = (only?.schema ?? {}) as JsonSchema
    if (Array.isArray(parsed) && primaryType(resolveSchema(schema, schema)) !== 'array') {
        const values = inOrder(parsed)
        return values && { params: values, context: undefined }
    }
    // An empty object gives no values, as it does to the run.
    const empty = root === parsed && Object.keys(root).length === 0
    if (only && !empty && !Object.hasOwn(root, only.name) && !Object.hasOwn(root, RUNTIME_CONTEXT)) {
        return { params: { [only.name]: parsed }, context: undefined }
    }
    return { params: byName(root, names), context: names.includes(RUNTIME_CONTEXT) ? undefined : root[RUNTIME_CONTEXT] }
}

/**
 * Collects the input of a rule table.
 *
 * The input is a form built from the schema of each parameter, or the same input as JSON text. The text suits a
 * request copied from a service log.
 *
 * The runtime context, when the project provides one, is the last line of the form, under the parameters the
 * rule declares. Its fields show the names of their codes, such as `Québec` for `QC`. The code is what is sent.
 *
 * Switching to JSON shows what the form holds. Switching back reads the text into the form when it parses, the way
 * the run reads it, so the value of the only parameter pasted as it is fills that parameter.
 */
export const ParametersInput: React.FC<ParametersInputProps> = ({ parameters, runtimeContext, onChange }) => {
    const { t } = useTranslation('execution')
    const formParameters: SchemaFormParameter[] = useMemo(() => [
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
                labels: RUNTIME_CONTEXT_LABELS,
            }]
            : []),
    ], [parameters, runtimeContext, t])
    const [mode, setMode] = useState<InputMode>('form')
    const [value, setValue] = useState<Record<string, unknown>>(() => initialFormValue(formParameters))
    const [text, setText] = useState('')
    const [error, setError] = useState<string | undefined>(undefined)

    // The same table can be described differently — read within the current module only, it may take other
    // parameters — so the input starts again when the description changes. Kept as it was, it would send a
    // value typed for a parameter that is gone, and leave a parameter that has appeared without its declared
    // default. The text is the same input in another shape, and starts again with it: what it holds is what
    // is sent while JSON is the input shown.
    //
    // What is watched is what the description says, not the answer it arrived in. Reading the table again
    // hands over a new list every time, and a table described twice over in the same words is the same
    // table: starting again on that would take away what the reader had typed, which is most of what they
    // came to do. The label of the context comes from the translation, which may be handed out anew on its
    // own, and what is typed survives that too.
    const described = useMemo(
        () => JSON.stringify([parameters, runtimeContext ?? null]),
        [parameters, runtimeContext]
    )
    useEffect(() => {
        const restarted = initialFormValue(formParameters)
        setValue(restarted)
        setText(toInputJson(restarted))
        setError(undefined)
    }, [described])

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
            // The text is read back into the form when it parses. A text that does not parse, or that gives more
            // values than the table takes, is kept as it is, so nothing typed is lost.
            const { parsed, error: parseError } = parseText(text)
            if (parseError) {
                return
            }
            const input = readInput(parsed, parameters)
            if (!input) {
                setError(t('input.tooManyValues', { count: parameters.length }))
                return
            }
            setValue(runtimeContext ? { ...input.params, [CONTEXT_FIELD]: asRecord(input.context) } : input.params)
        }
        setMode(next)
    }

    const updateText = (json: string) => {
        setText(json)
        setError(parseText(json).error)
    }

    const form = useMemo(
        () => <SchemaForm onChange={setValue} parameters={formParameters} value={value} />,
        [formParameters, value]
    )

    // A table that declares nothing has nothing to fill in, in either form: the choice between them would be a
    // choice of which emptiness to look at.
    if (formParameters.length === 0) {
        return null
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
            {/* The form is put away rather than taken down. What the reader has arranged in it — the nodes left
                open, which row is being written, the order a map's rows are drawn in — lives in the form itself,
                and a look at the JSON would otherwise put every one of them back the way the value happens to
                list it. Nothing of the form changes while the JSON is typed, so it is kept as it was drawn. */}
            <div hidden={mode !== 'form'}>{form}</div>
            {mode === 'json' && (
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
