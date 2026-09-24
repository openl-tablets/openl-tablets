import React, { type Key, useMemo, useRef, useState } from 'react'
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
    LINES_PER_STEP,
    type SimpleValueKind,
    type ValueNodeTitle,
} from './valueTree'
import { useStyles } from './parameterValues.styles'

/** The look of a value: its colours, and the lines of a value with inner structure. */
export type ValueStyles = ReturnType<typeof useStyles>['styles']

/**
 * The look of the values of a list, read once for all of them and handed to every {@link ValueCell}.
 *
 * Reading the look copies the whole theme, which costs more than drawing a plain value. A list of thousands of
 * values that reads it for every value runs the browser out of memory.
 */
export const useValueStyles = (): ValueStyles => useStyles().styles

const KIND_CLASS: Record<SimpleValueKind, keyof ValueStyles> = {
    null: 'valueNull',
    string: 'valueString',
    number: 'valueNumber',
    boolean: 'valueBoolean',
    other: 'valueDefault',
}

/** A value without inner structure, coloured by its kind the way a code editor colours a literal. */
const SimpleValue: React.FC<{ value: unknown, styles: ValueStyles }> = ({ value, styles }) => {
    const { display, kind } = describeSimpleValue(value)
    return <span className={styles[KIND_CLASS[kind]]}>{display}</span>
}

/**
 * What a value with inner structure is known as: its type and, in braces, the key it is referred to by -
 * `Driver (Sara)`. It stands for the value before it is read and stays as its title once it is. A value that
 * is referred to by no key is known as nothing: read, it is counted by its fields.
 */
export const valueLabel = ({ type, key }: Pick<TraceParameterValue, 'type' | 'key'>): string | undefined => (
    type && key ? `${type} (${key})` : undefined
)

/**
 * One node title of a value tree, `name (type) = value`. A value with inner structure shows what it is known
 * as, or else a count of its fields.
 */
const renderTitle = ({ name, value, type, summary }: ValueNodeTitle, styles: ValueStyles): React.ReactNode => (
    <span className={styles.treeTitle}>
        <span className={styles.valueName}>{name}</span>
        {type && <span className={styles.valueType}>{type}</span>}
        <span className={styles.valueEquals}>=</span>
        {isComplexValue(value)
            ? <span className={styles.valueSummary}>{summary ?? complexValueSummary(value)}</span>
            : <SimpleValue styles={styles} value={value} />}
    </span>
)

/** One line of the list, `name (type) = ` followed by whatever stands for the value. */
const ParameterLine: React.FC<{
    name: string
    type?: string | undefined
    styles: ValueStyles
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
 * A link among the lines of a value: it reads the value, reads it again, or lists more of it.
 *
 * A real button, so that the keyboard reaches it the way it reaches every other control.
 */
const ValueLink: React.FC<{
    testId: string
    onClick: () => void
    styles: ValueStyles
    children: React.ReactNode
}> = ({ testId, onClick, styles, children }) => (
    <button className={styles.valueLazy} data-testid={testId} onClick={onClick} type="button">
        {children}
    </button>
)

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
    styles: ValueStyles,
    t: (key: string) => string,
    label?: string
): React.ReactNode => {
    if (state.loading) {
        return <Spin indicator={<LoadingOutlined spin />} size="small" />
    }
    const readLink = (label: string) => (
        <ValueLink onClick={state.read} styles={styles} testId={`load-${testId}`}>{label}</ValueLink>
    )
    if (state.error) {
        // The reason comes with the link again: a read that failed once, on a project still compiling, works later.
        return <><span className={styles.valueError}>{state.error}</span>{lazy && readLink(t('value.retry'))}</>
    }
    if (lazy && !state.loaded && state.value == null) {
        // What the value is known as before it is read - its type and the key it is referred to by - tells the
        // values of a list apart, the way the legacy tree showed a folded value.
        return (
            <>
                {label && <span className={styles.valueSummary} data-testid={`summary-${testId}`}>{label}</span>}
                {readLink(t('value.load'))}
            </>
        )
    }
    if (state.loaded && state.value === undefined) {
        return <span className={styles.valueEmpty}>{'{}'}</span>
    }
    return null
}

interface ValueCellProps {
    value: unknown
    /** Prefix of the tree node keys, unique on the screen. */
    path: string
    /** The API referred to the value instead of writing it. */
    lazy?: boolean | undefined
    /** Reads the value the API left out. Absent when the screen cannot read it. */
    onLoad?: (() => Promise<TraceParameterValue | undefined>) | undefined
    /**
     * What the value is known as, such as `Driver (Sara)`: stands for it while it is only referred to, and
     * titles it once it is read. Absent, a read value is counted by its fields.
     */
    label?: string | undefined
    /** The look of the value, read once by the list that holds it with {@link useValueStyles}. */
    styles: ValueStyles
}

interface ValueTreeProps extends ValueNodeTitle {
    /** The key of the root node, unique on the screen. */
    path: string
    /** The value opens with its fields in view. */
    open?: boolean | undefined
    styles: ValueStyles
}

/**
 * A value with inner structure, drawn as a tree as far as the reader has opened it.
 *
 * A node holds the lines of its fields and elements only while it is open, and lists them a step at a time. A
 * whole value can hold millions of them, and a tree given all of them at once runs the browser out of memory
 * before the first one is read.
 */
const ValueTree: React.FC<ValueTreeProps> = ({ name, value, type, summary, path, open, styles }) => {
    const { t } = useTranslation('common')
    const [openKeys, setOpenKeys] = useState<Key[]>(open ? [path] : [])
    const [listed, setListed] = useState<ReadonlyMap<Key, number>>(new Map())
    const treeData = useMemo(
        () => [buildValueTreeData(
            { name, value, type, summary },
            title => renderTitle(title, styles),
            path,
            {
                open: new Set(openKeys),
                listed,
                renderMore: (key, left) => (
                    <ValueLink
                        styles={styles}
                        testId={`more-${key}`}
                        onClick={() => setListed(current => new Map(current)
                            .set(key, (current.get(key) ?? LINES_PER_STEP) + LINES_PER_STEP))}
                    >
                        {t('value.more', { count: left })}
                    </ValueLink>
                ),
            }
        )],
        [name, value, type, summary, path, styles, openKeys, listed, t]
    )
    return (
        <div className={styles.paramTree}>
            <Tree
                blockNode
                expandedKeys={openKeys}
                motion={false}
                onExpand={keys => setOpenKeys(keys)}
                selectable={false}
                showLine={{ showLeafIcon: false }}
                treeData={treeData}
            />
        </div>
    )
}

/** A value with inner structure, or one the API only referred to: it is read when asked for, and drawn as a tree. */
const ReadValueCell: React.FC<ValueCellProps> = ({ value, path, lazy, onLoad, label, styles }) => {
    const { t } = useTranslation('common')
    const state = useReadOnDemand(value, onLoad)

    const placeholder = placeholderOf(state, Boolean(lazy) && onLoad !== undefined, path, styles, t, label)
    if (placeholder !== null) {
        // The same spacing as a labelled line, so the label and the link that follows it do not run together.
        return <span className={styles.treeTitle}>{placeholder}</span>
    }
    if (!isComplexValue(state.value)) {
        return <SimpleValue styles={styles} value={state.value} />
    }
    return <ValueTree name="" path={path} styles={styles} summary={label} value={state.value} />
}

/**
 * A value on its own, without the name of the column it belongs to.
 *
 * A value with inner structure becomes a tree that expands field by field; a plain value is coloured the way a
 * debugger colours it. Use it where the name is already the header of a column.
 *
 * A value the API only referred to is a link that reads it, and a spinner while it is read.
 *
 * A list can hold thousands of values, so a plain value is drawn with its colour alone. Only a value that has
 * inner structure or is still to be read keeps what it has read.
 */
export const ValueCell: React.FC<ValueCellProps> = props => (!props.lazy && !isComplexValue(props.value)
    ? <SimpleValue styles={props.styles} value={props.value} />
    : <ReadValueCell {...props} />)

interface ParameterValueTreeProps {
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
    const label = valueLabel(param)

    const line = (children: React.ReactNode) => (
        <ParameterLine name={param.name} styles={styles} type={param.description}>{children}</ParameterLine>
    )

    const placeholder = placeholderOf(state, Boolean(param.lazy) && onLoad !== undefined, paramKey, styles, t, label)
    if (placeholder !== null) {
        return line(placeholder)
    }
    if (!isComplex) {
        return line(<SimpleValue styles={styles} value={displayValue} />)
    }
    return (
        <ValueTree
            name={param.name}
            open={open}
            path={paramKey}
            styles={styles}
            summary={label}
            type={param.description}
            value={displayValue}
        />
    )
}

interface ParameterValueListProps {
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
