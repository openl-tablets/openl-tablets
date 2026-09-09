import React, { useMemo, useState } from 'react'
import { Spin, Tree } from 'antd'
import { LoadingOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import type { TraceParameterValue } from 'types/trace'
import { errorMessage } from 'utils/errorMessage'
import { onActivate } from 'utils/keyboardActivate'
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

export interface ParameterValueTreeProps {
    param: TraceParameterValue
    /** Prefix of the tree node keys, unique on the screen. */
    paramKey: string
    /** Reads the value the API left out. Absent when the screen cannot read it. */
    onLoad?: (() => Promise<TraceParameterValue | undefined>) | undefined
}

/**
 * One parameter as a line of the list: `name (type) = value`.
 *
 * A value with inner structure becomes a tree that expands field by field. A value the API left out is a link
 * that reads it, and a spinner while it is read.
 */
export const ParameterValueTree: React.FC<ParameterValueTreeProps> = ({ param, paramKey, onLoad }) => {
    const { t } = useTranslation('common')
    const { styles } = useStyles()
    const [loading, setLoading] = useState(false)
    const [loaded, setLoaded] = useState(false)
    const [loadedValue, setLoadedValue] = useState<unknown>(undefined)
    const [error, setError] = useState<string | null>(null)

    const displayValue = loaded ? loadedValue : param.value
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

    const loadValue = () => {
        if (!onLoad) {
            return
        }
        setLoading(true)
        setError(null)
        onLoad()
            .then(result => {
                setLoadedValue(result?.value)
                setLoaded(true)
            })
            .catch(loadError => setError(errorMessage(loadError) || t('value.loadFailed')))
            .finally(() => setLoading(false))
    }

    if (loading) {
        return line(<Spin indicator={<LoadingOutlined spin />} size="small" />)
    }
    const readLink = (label: string) => (
        <span
            className={styles.valueLazy}
            data-testid={`load-${paramKey}`}
            onClick={loadValue}
            onKeyDown={onActivate(loadValue)}
            role="button"
            tabIndex={0}
        >
            {label}
        </span>
    )

    if (error) {
        // The reason comes with the link again: a read that failed once, on a project still compiling, works later.
        return line(<><span className={styles.valueError}>{error}</span>{onLoad && readLink(t('value.retry'))}</>)
    }
    if (param.lazy && onLoad && !loaded && displayValue == null) {
        return line(readLink(t('value.load')))
    }
    if (loaded && displayValue === undefined) {
        return line(<span className={styles.valueEmpty}>{'{}'}</span>)
    }
    if (!isComplex) {
        return line(<SimpleValue styles={styles} value={displayValue} />)
    }
    return (
        <div className={styles.paramTree}>
            <Tree
                blockNode
                defaultExpandedKeys={[]}
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
}

/** The values of one execution as a list of `name (type) = value` lines, in the order they are declared. */
export const ParameterValueList: React.FC<ParameterValueListProps> = ({ parameters, keyPrefix, onLoad }) => {
    const { styles } = useStyles()
    return (
        <div className={styles.list}>
            {parameters.map((param, index) => (
                <ParameterValueTree
                    key={`${param.name}-${index}`}
                    onLoad={onLoad ? () => onLoad(index) : undefined}
                    param={param}
                    paramKey={`${keyPrefix}-${index}`}
                />
            ))}
        </div>
    )
}
