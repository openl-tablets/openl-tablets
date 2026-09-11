import React, { useCallback, useMemo, useState } from 'react'
import { Tree } from 'antd'
import type { TreeDataNode } from 'antd'
import { buildNode, type TreeContext } from './SchemaTree'
import { createValue, withoutNulls, type JsonSchema } from './schema'

/** One value the form collects. A parameter of a table, or the runtime context. */
export interface SchemaFormParameter {
    /** Key the value is written under. */
    name: string
    /** Label the value is shown under. The name is used when absent. */
    label?: string | undefined
    /** Display name of the declared type, shown next to the label. */
    type?: string | undefined
    /** JSON schema of the values accepted. A value without a schema is edited as JSON text. */
    schema?: object | null | undefined
    /** Value the parameter starts with. It holds the defaults its type declares. */
    value?: unknown
}

export interface SchemaFormProps {
    parameters: SchemaFormParameter[]
    /** The values collected so far, keyed by parameter name. A parameter without a value is absent. */
    value: Record<string, unknown>
    onChange: (value: Record<string, unknown>) => void
}

const rootSchema = (parameter: SchemaFormParameter): JsonSchema => (parameter.schema ?? {}) as JsonSchema

/**
 * The values a form starts with.
 *
 * A parameter with a starting value keeps it, without its null fields. Any other structured parameter is created
 * empty, so the user has its fields to fill in.
 */
export const initialFormValue = (parameters: SchemaFormParameter[]): Record<string, unknown> =>
    Object.fromEntries(parameters
        .map(parameter => [parameter.name, parameter.value === undefined || parameter.value === null
            ? createValue(rootSchema(parameter), rootSchema(parameter))
            : withoutNulls(parameter.value)])
        .filter(([, initial]) => initial !== undefined))

/**
 * An input form built from the JSON schema of each parameter.
 *
 * The form is a tree of `name = value` lines, the way the trace window shows a parameter. A plain value is edited
 * in place behind its pencil. A structure is created and cleared with plus and cross. A list grows element by
 * element.
 *
 * Every parameter starts folded - a rule can declare many - and opening one shows its fields. A structure the
 * user creates opens itself, so the fields to fill in are in view.
 *
 * The form collects plain JSON in the shape the run and trace APIs read. What it holds can also be shown and
 * edited as JSON text.
 */
export const SchemaForm: React.FC<SchemaFormProps> = ({ parameters, value, onChange }) => {
    const [editing, setEditing] = useState<string | null>(null)
    const [expanded, setExpanded] = useState<React.Key[]>([])
    const expand = useCallback((path: string) => setExpanded(keys => (keys.includes(path) ? keys : [...keys, path])), [])
    // An element is addressed by its position, so the elements after the removed one are open under a new key.
    const afterRemove = useCallback((path: string, index: number) => setExpanded(keys => keys.flatMap(key => {
        const prefix = `${path}[`
        if (typeof key !== 'string' || !key.startsWith(prefix)) {
            return [key]
        }
        const at = Number(key.slice(prefix.length, key.indexOf(']', prefix.length)))
        if (Number.isNaN(at) || at < index) {
            return [key]
        }
        return at === index ? [] : [key.replace(`${prefix}${at}]`, `${prefix}${at - 1}]`)]
    })), [])

    // A map entry is addressed by its key, so renaming one carries what is open under it to the new key, and
    // removing one forgets it. Nothing else moves: the other entries keep the keys they had.
    const entryMoved = useCallback((from: string, to: string | null) => setExpanded(keys => keys.flatMap(key => {
        const under = typeof key === 'string'
            && (key === from || key.startsWith(`${from}.`) || key.startsWith(`${from}[`))
        if (!under) {
            return [key]
        }
        return to === null ? [] : [`${to}${(key as string).slice(from.length)}`]
    })), [])

    const treeData = useMemo((): TreeDataNode[] => parameters.map(parameter => {
        const root = rootSchema(parameter)
        const context: TreeContext = { root, editing, setEditing, expand, afterRemove, entryMoved }
        return buildNode({
            name: parameter.name,
            label: parameter.label,
            type: parameter.type,
            schema: root,
            value: value[parameter.name],
            path: parameter.name,
            onChange: (next: unknown) => {
                const { [parameter.name]: _previous, ...rest } = value
                onChange(next === undefined ? rest : { ...value, [parameter.name]: next })
            },
            context,
        })
    }), [parameters, value, onChange, editing, expand, afterRemove, entryMoved])

    return (
        // The expand animation is off. While it runs, a structure created by a click would not show its fields.
        // `false` turns it off, `null` would keep the default.
        <Tree
            blockNode
            expandedKeys={expanded}
            motion={false}
            onExpand={keys => setExpanded(keys)}
            selectable={false}
            showLine={{ showLeafIcon: false }}
            treeData={treeData}
            virtual={false}
        />
    )
}

export default SchemaForm
