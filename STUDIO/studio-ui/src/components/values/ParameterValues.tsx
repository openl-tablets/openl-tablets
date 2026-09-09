import React, { useMemo, useRef, useState } from 'react'
import { Spin, Tree } from 'antd'
import { LoadingOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import type { TraceParameterValue } from 'types/trace'
import { errorMessage } from 'utils/errorMessage'
import {
    buildValueTreeData,
    complexValueSummary,
    describeSimpleValue,
    isComplexValue,
    type SimpleValueKind,
    type ValueNodeTitle,
} from './valueTree'
import { useStyles } from './parameterValues.styles'

type Styles = ReturnType<typeof useStyles>['styles']

const KIND_CLASS: Record<SimpleValueKind, keyof Styles> = {
    null: 'valueNull',
    string: 'valueString',
    number: 'valueNumber',
    boolean: 'valueBoolean',
    other: 'valueDefault',
}

/** A value without inner structure, coloured by its kind the way a code editor colours a literal. */
const SimpleValue: React.FC<{ value: unknown, styles: Styles }> = ({ value, styles }) => {
    const { display, kind } = describeSimpleValue(value)
    return <span className={styles[KIND_CLASS[kind]]}>{display}</span>
}

/** One node title of a value tree, `name (type) = value`. A value with inner structure shows a summary. */
const renderTitle = ({ name, value, type }: ValueNodeTitle, styles: Styles): React.ReactNode => (
    <span className={styles.treeTitle}>
        <span className={styles.valueName}>{name}</span>
        {type && <span className={styles.valueType}>{type}</span>}
        <span className={styles.valueEquals}>=</span>
        {isComplexValue(value)
            ? <span className={styles.valueSummary}>{complexValueSummary(value)}</span>
            : <SimpleValue styles={styles} value={value} />}
    </span>
)

/** One line of the list, `name (type) = ` followed by whatever stands for the value. */
const ParameterLine: React.FC<{
    name: string
    type?: string | undefined
    styles: Styles
    children: React.ReactNode
}> = ({ name, type, styles, children }) => (
    <div className={styles.item}>
        <span className={styles.treeTitle}>
            <span className={styles.valueName}>{name}</span>
            {type && <span className={styles.valueType}>{type}</span>}
            <span className={styles.valueEquals}>=</span>
            {children}
        </span>
    </div>
)

/** A value that may only be referred to: what stands for it now, and how it is read when it is asked for. */
interface ReadOnDemand {
    /** The value as it stands: the one that came with the response, or the one that was read. */
    value: unknown
    loaded: boolean
    loading: boolean
    error: string | null
    read: () => void
}

/**
 * Follows a value the API only referred to.
 *
 * The value is read once, when the screen asks for it. A read that failed can be tried again, because a
 * project that was still compiling answers later.
 *
 * A line that comes to stand for another value starts over: what was read for the value before it is neither
 * shown nor waited for. A screen reuses its lines - another step of a trace, another page of cases - so a read
 * that is still on its way when that happens is left behind. A value the API only refers to is told apart by
 * that reference, because the line itself holds nothing to compare.
 */
const useReadOnDemand = (
    value: unknown,
    onLoad?: (() => Promise<TraceParameterValue | undefined>) | undefined,
    reference?: unknown
): ReadOnDemand => {
    const { t } = useTranslation('common')
    const [loading, setLoading] = useState(false)
    const [loaded, setLoaded] = useState(false)
    const [loadedValue, setLoadedValue] = useState<unknown>(undefined)
    const [error, setError] = useState<string | null>(null)
    const [shown, setShown] = useState({ value, reference })
    const request = useRef(0)

    if (shown.value !== value || shown.reference !== reference) {
        setShown({ value, reference })
        request.current += 1
        setLoading(false)
        setLoaded(false)
        setLoadedValue(undefined)
        setError(null)
    }

    const read = () => {
        if (!onLoad) {
            return
        }
        const reading = request.current
        setLoading(true)
        setError(null)
        onLoad()
            .then(result => {
                if (reading === request.current) {
                    setLoadedValue(result?.value)
                    setLoaded(true)
                }
            })
            .catch(loadError => {
                if (reading === request.current) {
                    setError(errorMessage(loadError) || t('value.loadFailed'))
                }
            })
            .finally(() => {
                if (reading === request.current) {
                    setLoading(false)
                }
            })
    }

    return { value: loaded ? loadedValue : value, loaded, loading, error, read }
}

/**
 * What stands in place of a value that is not shown: a spinner while it is read, the reason it could not be
 * read, or the link that reads it.
 *
 * Answers nothing when the value itself is there to show.
 */
const placeholderOf = (
    state: ReadOnDemand,
    lazy: boolean,
    testId: string,
    styles: Styles,
    t: (key: string) => string
): React.ReactNode => {
    if (state.loading) {
        return <Spin indicator={<LoadingOutlined spin />} size="small" />
    }
    // A real button, so that the keyboard reaches it the way it reaches every other control.
    const readLink = (label: string) => (
        <button className={styles.valueLazy} data-testid={`load-${testId}`} onClick={state.read} type="button">
            {label}
        </button>
    )
    if (state.error) {
        // The reason comes with the link again: a read that failed once, on a project still compiling, works later.
        return <><span className={styles.valueError}>{state.error}</span>{lazy && readLink(t('value.retry'))}</>
    }
    if (lazy && !state.loaded && state.value == null) {
        return readLink(t('value.load'))
    }
    if (state.loaded && state.value === undefined) {
        return <span className={styles.valueEmpty}>{'{}'}</span>
    }
    return null
}

export interface ValueCellProps {
    value: unknown
    /** Prefix of the tree node keys, unique on the screen. */
    path: string
    /** The API referred to the value instead of writing it. */
    lazy?: boolean | undefined
    /** Reads the value the API left out. Absent when the screen cannot read it. */
    onLoad?: (() => Promise<TraceParameterValue | undefined>) | undefined
}

/**
 * A value on its own, without the name of the column it belongs to.
 *
 * A value with inner structure becomes a tree that expands field by field; a plain value is coloured the way a
 * debugger colours it. Use it where the name is already the header of a column.
 *
 * A value the API only referred to is a link that reads it, and a spinner while it is read.
 */
export const ValueCell: React.FC<ValueCellProps> = ({ value, path, lazy, onLoad }) => {
    const { t } = useTranslation('common')
    const { styles } = useStyles()
    const state = useReadOnDemand(value, onLoad)
    const treeData = useMemo(
        () => (isComplexValue(state.value)
            ? [buildValueTreeData({ name: '', value: state.value }, title => renderTitle(title, styles), path)]
            : []),
        [state.value, path, styles]
    )

    const placeholder = placeholderOf(state, Boolean(lazy) && onLoad !== undefined, path, styles, t)
    if (placeholder !== null) {
        return <>{placeholder}</>
    }
    if (!isComplexValue(state.value)) {
        return <SimpleValue styles={styles} value={state.value} />
    }
    return (
        <div className={styles.paramTree}>
            <Tree blockNode defaultExpandedKeys={[]} selectable={false} showLine={{ showLeafIcon: false }} treeData={treeData} />
        </div>
    )
}

export interface ParameterValueTreeProps {
    param: TraceParameterValue
    /** Prefix of the tree node keys, unique on the screen. */
    paramKey: string
    /** Reads the value the API left out. Absent when the screen cannot read it. */
    onLoad?: (() => Promise<TraceParameterValue | undefined>) | undefined
    /** The value opens with its fields in view, for a screen whose point is the value itself. */
    open?: boolean | undefined
}

/**
 * One parameter as a line of the list: `name (type) = value`.
 *
 * A value with inner structure becomes a tree that expands field by field. A value the API left out is a link
 * that reads it, and a spinner while it is read.
 */
export const ParameterValueTree: React.FC<ParameterValueTreeProps> = ({ param, paramKey, onLoad, open }) => {
    const { t } = useTranslation('common')
    const { styles } = useStyles()
    const state = useReadOnDemand(param.value, onLoad, param.parameterId)

    const displayValue = state.value
    const isComplex = isComplexValue(displayValue)

    const treeData = useMemo(() => (isComplex
        ? [buildValueTreeData(
            { name: param.name, value: displayValue, type: param.description },
            title => renderTitle(title, styles),
            paramKey
        )]
        : []), [param.name, param.description, displayValue, paramKey, isComplex, styles])

    const line = (children: React.ReactNode) => (
        <ParameterLine name={param.name} styles={styles} type={param.description}>{children}</ParameterLine>
    )

    const placeholder = placeholderOf(state, Boolean(param.lazy) && onLoad !== undefined, paramKey, styles, t)
    if (placeholder !== null) {
        return line(placeholder)
    }
    if (!isComplex) {
        return line(<SimpleValue styles={styles} value={displayValue} />)
    }
    return (
        <div className={styles.paramTree}>
            <Tree
                blockNode
                defaultExpandedKeys={open ? [paramKey] : []}
                selectable={false}
                showLine={{ showLeafIcon: false }}
                treeData={treeData}
            />
        </div>
    )
}

export interface ParameterValueListProps {
    parameters: TraceParameterValue[]
    /** Prefix of the tree node keys, unique on the screen. */
    keyPrefix: string
    /** Reads the value of the parameter at the given position, for a value the API left out. */
    onLoad?: ((index: number) => Promise<TraceParameterValue | undefined>) | undefined
    /** Every value opens with its fields in view, for a screen whose point is the values themselves. */
    open?: boolean | undefined
}

/** The values of one execution as a list of `name (type) = value` lines, in the order they are declared. */
export const ParameterValueList: React.FC<ParameterValueListProps> = ({ parameters, keyPrefix, onLoad, open }) => {
    const { styles } = useStyles()
    return (
        <div className={styles.list}>
            {parameters.map((param, index) => (
                <ParameterValueTree
                    key={`${param.name}-${index}`}
                    onLoad={onLoad ? () => onLoad(index) : undefined}
                    open={open}
                    param={param}
                    paramKey={`${keyPrefix}-${index}`}
                />
            ))}
        </div>
    )
}
