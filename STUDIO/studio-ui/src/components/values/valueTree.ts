import type { ReactNode } from 'react'
import type { TreeDataNode } from 'antd'

export type SimpleValueKind = 'null' | 'string' | 'number' | 'boolean' | 'other'

/** A value without inner structure. The text a viewer shows and the kind it colours it by. */
export interface SimpleValueText {
    display: string
    kind: SimpleValueKind
}

/** Whether a value has inner structure that a viewer expands. An object or an array does. */
export const isComplexValue = (value: unknown): value is object => value !== null && typeof value === 'object'

/** The one-line summary of a value with inner structure. It counts the fields or elements the value holds. */
export const complexValueSummary = (value: object): string =>
    Array.isArray(value) ? `{${value.length} elements}` : `{${Object.keys(value).length} fields}`

/** Describes a value without inner structure the way a debugger shows it. Strings are quoted, null is spelled out. */
export const describeSimpleValue = (value: unknown): SimpleValueText => {
    if (value === null) {
        return { display: 'null', kind: 'null' }
    }
    if (value === undefined) {
        return { display: 'undefined', kind: 'null' }
    }
    if (typeof value === 'string') {
        return { display: `"${value}"`, kind: 'string' }
    }
    if (typeof value === 'number') {
        return { display: String(value), kind: 'number' }
    }
    if (typeof value === 'boolean') {
        return { display: String(value), kind: 'boolean' }
    }
    if (isComplexValue(value)) {
        // A value with inner structure is opened as a tree; on one line it is named by what it holds.
        return { display: complexValueSummary(value), kind: 'other' }
    }
    return { display: String(value), kind: 'other' }
}

/** The lines a value with inner structure is opened into: its elements, or its fields under their names. */
const childrenOf = (value: unknown): ValueNodeTitle[] => {
    if (!isComplexValue(value)) {
        return []
    }
    return Array.isArray(value)
        ? value.map((item, index) => ({ name: `[${index}]`, value: item }))
        : Object.entries(value).map(([name, item]) => ({ name, value: item }))
}

/** One node of a value tree. The name it is shown under, its value and, for a root, its declared type. */
export interface ValueNodeTitle {
    name: string
    value: unknown
    type?: string | undefined
}

/**
 * The tree of a value in the shape the Ant Design Tree renders.
 *
 * Every field or element becomes a node, recursively. The caller renders every title, so each screen keeps its
 * own look while all of them share the traversal.
 *
 * A value without inner structure, or an empty one, is a leaf.
 */
export const buildValueTreeData = (
    title: ValueNodeTitle,
    renderTitle: (title: ValueNodeTitle) => ReactNode,
    keyPrefix = '0'
): TreeDataNode => {
    const { value } = title
    const children = childrenOf(value)
    if (children.length === 0) {
        return { key: keyPrefix, title: renderTitle(title), isLeaf: true }
    }
    return {
        key: keyPrefix,
        title: renderTitle(title),
        isLeaf: false,
        children: children.map((child, index) => buildValueTreeData(child, renderTitle, `${keyPrefix}-${index}`)),
    }
}
