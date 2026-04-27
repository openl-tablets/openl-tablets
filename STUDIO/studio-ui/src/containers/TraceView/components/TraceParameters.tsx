import React, { useState, useMemo } from 'react'
import { Tree, Spin, Typography } from 'antd'
import type { TreeDataNode } from 'antd'
import { LoadingOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
import type { TraceParameterValue } from 'types/trace'
import { useStyles } from './TraceParameters.styles'

const { Text } = Typography

const SECTION_CLASS = 'trace-params-section'

type Styles = ReturnType<typeof useStyles>['styles']

/**
 * Format a simple value for display (IDEA-style).
 */
const formatSimpleValue = (value: any, styles: Styles): { display: string; className: string } => {
    if (value === null) {
        return { display: 'null', className: styles.valueNull }
    }
    if (value === undefined) {
        return { display: 'undefined', className: styles.valueNull }
    }
    if (typeof value === 'string') {
        return { display: `"${value}"`, className: styles.valueString }
    }
    if (typeof value === 'number') {
        return { display: String(value), className: styles.valueNumber }
    }
    if (typeof value === 'boolean') {
        return { display: String(value), className: styles.valueBoolean }
    }
    return { display: String(value), className: styles.valueDefault }
}

/**
 * Check if value is complex (object or array).
 */
const isComplexValue = (value: any): boolean => {
    return value !== null && typeof value === 'object'
}

/**
 * Get summary for complex value (IDEA-style).
 */
const getComplexSummary = (value: any): string => {
    if (Array.isArray(value)) {
        return `{${value.length} elements}`
    }
    if (typeof value === 'object' && value !== null) {
        const keys = Object.keys(value)
        return `{${keys.length} fields}`
    }
    return ''
}

/**
 * Build tree data from a value recursively.
 */
const buildTreeDataFromValue = (
    name: string,
    value: any,
    styles: Styles,
    type?: string,
    keyPrefix: string = '0'
): TreeDataNode => {
    const isComplex = isComplexValue(value)

    const hasChildren = isComplex && (
        Array.isArray(value) ? value.length > 0 : Object.keys(value).length > 0
    )

    const renderTitle = () => {
        const valueDisplay = isComplex
            ? <span className={styles.valueSummary}>{getComplexSummary(value)}</span>
            : (() => {
                const { display, className } = formatSimpleValue(value, styles)
                return <span className={className}>{display}</span>
            })()

        return (
            <span className={styles.treeTitle}>
                <span className={styles.valueName}>{name}</span>
                {type && <span className={styles.valueType}>{type}</span>}
                <span className={styles.valueEquals}>=</span>
                {valueDisplay}
            </span>
        )
    }

    if (!hasChildren) {
        return {
            key: keyPrefix,
            title: renderTitle(),
            isLeaf: true,
        }
    }

    const children: TreeDataNode[] = Array.isArray(value)
        ? value.map((item, index) =>
            buildTreeDataFromValue(`[${index}]`, item, styles, undefined, `${keyPrefix}-${index}`)
        )
        : Object.entries(value).map(([key, val], index) =>
            buildTreeDataFromValue(key, val, styles, undefined, `${keyPrefix}-${index}`)
        )

    return {
        key: keyPrefix,
        title: renderTitle(),
        isLeaf: false,
        children,
    }
}

interface ParameterTreeProps {
    param: TraceParameterValue
    paramKey: string
}

/**
 * Single parameter as an Ant Design Tree with lazy loading support.
 */
const ParameterTree: React.FC<ParameterTreeProps> = ({ param, paramKey }) => {
    const { t } = useTranslation('trace')
    const { styles } = useStyles()
    const { fetchLazyParameter } = useTraceStore()
    const [loading, setLoading] = useState(false)
    const [loaded, setLoaded] = useState(false)
    const [loadedValue, setLoadedValue] = useState<any>(undefined)
    const [error, setError] = useState<string | null>(null)

    const displayValue = loaded ? loadedValue : param.value
    const isComplex = isComplexValue(displayValue)

    const treeData = useMemo(() => {
        if (displayValue === undefined || displayValue === null || !isComplex) {
            return []
        }
        return [buildTreeDataFromValue(param.name, displayValue, styles, param.description, paramKey)]
    }, [param.name, param.description, displayValue, paramKey, isComplex, styles])

    const handleLoadValue = async () => {
        if (!param.lazy || param.parameterId == null) return

        setLoading(true)
        setError(null)
        try {
            const result = await fetchLazyParameter(param.parameterId)
            setLoadedValue(result.value)
            setLoaded(true)
        } catch (err: any) {
            setError(err?.message || t('errors.parameterFailed'))
        } finally {
            setLoading(false)
        }
    }

    if (param.lazy && !loaded && (displayValue === undefined || displayValue === null) && !loading) {
        return (
            <div className={styles.item}>
                <span className={styles.treeTitle}>
                    <span className={styles.valueName}>{param.name}</span>
                    <span className={styles.valueType}>{param.description}</span>
                    <span className={styles.valueEquals}>=</span>
                    <span className={styles.valueLazy} onClick={handleLoadValue}>
                        {t('param.loadValue')}
                    </span>
                </span>
            </div>
        )
    }

    if (loading) {
        return (
            <div className={styles.item}>
                <span className={styles.treeTitle}>
                    <span className={styles.valueName}>{param.name}</span>
                    <span className={styles.valueType}>{param.description}</span>
                    <span className={styles.valueEquals}>=</span>
                    <Spin indicator={<LoadingOutlined spin />} size="small" />
                </span>
            </div>
        )
    }

    if (error) {
        return (
            <div className={styles.item}>
                <span className={styles.treeTitle}>
                    <span className={styles.valueName}>{param.name}</span>
                    <span className={styles.valueType}>{param.description}</span>
                    <span className={styles.valueEquals}>=</span>
                    <span className={styles.valueError}>{error}</span>
                </span>
            </div>
        )
    }

    if (loaded && displayValue === undefined) {
        return (
            <div className={styles.item}>
                <span className={styles.treeTitle}>
                    <span className={styles.valueName}>{param.name}</span>
                    <span className={styles.valueType}>{param.description}</span>
                    <span className={styles.valueEquals}>=</span>
                    <span className={styles.valueEmpty}>{'{}'}</span>
                </span>
            </div>
        )
    }

    if (!isComplex) {
        const { display, className } = formatSimpleValue(displayValue, styles)
        return (
            <div className={styles.item}>
                <span className={styles.treeTitle}>
                    <span className={styles.valueName}>{param.name}</span>
                    <span className={styles.valueType}>{param.description}</span>
                    <span className={styles.valueEquals}>=</span>
                    <span className={className}>{display}</span>
                </span>
            </div>
        )
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

interface TraceParametersProps {
    parameters?: TraceParameterValue[] | undefined
    title: string
    emptyText?: string | undefined
    /** Optional copy button to display next to the title */
    copyButton?: React.ReactNode | undefined
}

/**
 * Component for displaying trace parameters using Ant Design Tree.
 * Maintains IDEA debug style with colored values.
 */
const TraceParameters: React.FC<TraceParametersProps> = ({
    parameters,
    title,
    emptyText,
    copyButton,
}) => {
    const { t } = useTranslation('trace')
    const { styles, cx } = useStyles()

    if (!parameters || parameters.length === 0) {
        return (
            <div className={cx(styles.section, SECTION_CLASS)}>
                <div className={styles.header}>
                    <span className={styles.title}>{title}:</span>
                </div>
                <Text className={styles.empty} type="secondary">
                    {emptyText || t('details.noParameters')}
                </Text>
            </div>
        )
    }

    return (
        <div className={cx(styles.section, SECTION_CLASS)}>
            <div className={styles.header}>
                <span className={styles.title}>{title}:</span>
                {copyButton}
            </div>
            <div className={styles.list}>
                {parameters.map((param, index) => (
                    <ParameterTree
                        key={`${param.name}-${index}`}
                        param={param}
                        paramKey={`param-${index}`}
                    />
                ))}
            </div>
        </div>
    )
}

/**
 * Component for displaying a single parameter value (e.g., context, result).
 */
export const SingleParameter: React.FC<{
    parameter?: TraceParameterValue | undefined
    title: string
    emptyText?: string | undefined
    /** Optional copy button to display next to the title */
    copyButton?: React.ReactNode | undefined
}> = ({ parameter, title, emptyText, copyButton }) => {
    const { t } = useTranslation('trace')
    const { styles, cx } = useStyles()

    if (!parameter) {
        return (
            <div className={cx(styles.section, SECTION_CLASS)}>
                <div className={styles.header}>
                    <span className={styles.title}>{title}:</span>
                </div>
                <Text className={styles.empty} type="secondary">
                    {emptyText || t('details.noResult')}
                </Text>
            </div>
        )
    }

    return (
        <div className={cx(styles.section, SECTION_CLASS)}>
            <div className={styles.header}>
                <span className={styles.title}>{title}:</span>
                {copyButton}
            </div>
            <div className={styles.list}>
                <ParameterTree param={parameter} paramKey="single-param" />
            </div>
        </div>
    )
}

export default TraceParameters
