import React, { useState, useMemo } from 'react'
import { Tree, Spin, Typography } from 'antd'
import type { TreeDataNode } from 'antd'
import { LoadingOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
import type { TraceParameterValue } from 'types/trace'
import './TraceParameters.scss'

const { Text } = Typography

/**
 * Format a simple value for display (IDEA-style).
 */
const formatSimpleValue = (value: any): { display: string; className: string } => {
    if (value === null) {
        return { display: 'null', className: 'trace-value-null' }
    }
    if (value === undefined) {
        return { display: 'undefined', className: 'trace-value-null' }
    }
    if (typeof value === 'string') {
        return { display: `"${value}"`, className: 'trace-value-string' }
    }
    if (typeof value === 'number') {
        return { display: String(value), className: 'trace-value-number' }
    }
    if (typeof value === 'boolean') {
        return { display: String(value), className: 'trace-value-boolean' }
    }
    return { display: String(value), className: 'trace-value-default' }
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
    type?: string,
    keyPrefix: string = '0'
): TreeDataNode => {
    const isComplex = isComplexValue(value)

    // Check if complex value has children
    const hasChildren = isComplex && (
        Array.isArray(value) ? value.length > 0 : Object.keys(value).length > 0
    )

    const renderTitle = () => {
        const valueDisplay = isComplex
            ? <span className="trace-value-summary">{getComplexSummary(value)}</span>
            : (() => {
                const { display, className } = formatSimpleValue(value)
                return <span className={className}>{display}</span>
            })()

        return (
            <span className="trace-tree-title">
                <span className="trace-value-name">{name}</span>
                {type && <span className="trace-value-type">{type}</span>}
                <span className="trace-value-equals">=</span>
                {valueDisplay}
            </span>
        )
    }

    // Leaf node: simple value or empty complex value
    if (!hasChildren) {
        return {
            key: keyPrefix,
            title: renderTitle(),
            isLeaf: true,
        }
    }

    const children: TreeDataNode[] = Array.isArray(value)
        ? value.map((item, index) =>
            buildTreeDataFromValue(`[${index}]`, item, undefined, `${keyPrefix}-${index}`)
        )
        : Object.entries(value).map(([key, val], index) =>
            buildTreeDataFromValue(key, val, undefined, `${keyPrefix}-${index}`)
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
    const { fetchLazyParameter } = useTraceStore()
    const [loading, setLoading] = useState(false)
    const [loaded, setLoaded] = useState(false)
    const [loadedValue, setLoadedValue] = useState<any>(undefined)
    const [error, setError] = useState<string | null>(null)

    const displayValue = loaded ? loadedValue : param.value
    const isComplex = isComplexValue(displayValue)

    // Build tree data - must be called before any conditional returns (React hooks rule)
    const treeData = useMemo(() => {
        if (displayValue === undefined || displayValue === null || !isComplex) {
            return []
        }
        return [buildTreeDataFromValue(param.name, displayValue, param.description, paramKey)]
    }, [param.name, param.description, displayValue, paramKey, isComplex])

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

    // Lazy parameter not yet loaded
    if (param.lazy && !loaded && (displayValue === undefined || displayValue === null) && !loading) {
        return (
            <div className="trace-param-item">
                <span className="trace-tree-title">
                    <span className="trace-value-name">{param.name}</span>
                    <span className="trace-value-type">{param.description}</span>
                    <span className="trace-value-equals">=</span>
                    <span className="trace-value-lazy" onClick={handleLoadValue}>
                        {t('param.loadValue')}
                    </span>
                </span>
            </div>
        )
    }

    // Loading state
    if (loading) {
        return (
            <div className="trace-param-item">
                <span className="trace-tree-title">
                    <span className="trace-value-name">{param.name}</span>
                    <span className="trace-value-type">{param.description}</span>
                    <span className="trace-value-equals">=</span>
                    <Spin indicator={<LoadingOutlined spin />} size="small" />
                </span>
            </div>
        )
    }

    // Error state
    if (error) {
        return (
            <div className="trace-param-item">
                <span className="trace-tree-title">
                    <span className="trace-value-name">{param.name}</span>
                    <span className="trace-value-type">{param.description}</span>
                    <span className="trace-value-equals">=</span>
                    <span className="trace-value-error">{error}</span>
                </span>
            </div>
        )
    }

    // Loaded but value is undefined - show empty braces
    if (loaded && displayValue === undefined) {
        return (
            <div className="trace-param-item">
                <span className="trace-tree-title">
                    <span className="trace-value-name">{param.name}</span>
                    <span className="trace-value-type">{param.description}</span>
                    <span className="trace-value-equals">=</span>
                    <span className="trace-value-empty">{'{}'}</span>
                </span>
            </div>
        )
    }

    // Simple value - render without tree
    if (!isComplex) {
        const { display, className } = formatSimpleValue(displayValue)
        return (
            <div className="trace-param-item">
                <span className="trace-tree-title">
                    <span className="trace-value-name">{param.name}</span>
                    <span className="trace-value-type">{param.description}</span>
                    <span className="trace-value-equals">=</span>
                    <span className={className}>{display}</span>
                </span>
            </div>
        )
    }

    return (
        <div className="trace-param-tree">
            <Tree
                treeData={treeData}
                showLine={{ showLeafIcon: false }}
                defaultExpandedKeys={[]}
                selectable={false}
                blockNode
            />
        </div>
    )
}

interface TraceParametersProps {
    parameters?: TraceParameterValue[]
    title: string
    emptyText?: string
}

/**
 * Component for displaying trace parameters using Ant Design Tree.
 * Maintains IDEA debug style with colored values.
 */
const TraceParameters: React.FC<TraceParametersProps> = ({
    parameters,
    title,
    emptyText,
}) => {
    const { t } = useTranslation('trace')

    if (!parameters || parameters.length === 0) {
        return (
            <div className="trace-params-section">
                <span className="trace-params-title">{title}:</span>
                <Text type="secondary" className="trace-params-empty">
                    {emptyText || t('details.noParameters')}
                </Text>
            </div>
        )
    }

    return (
        <div className="trace-params-section">
            <span className="trace-params-title">{title}:</span>
            <div className="trace-params-list">
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
    parameter?: TraceParameterValue
    title: string
    emptyText?: string
}> = ({ parameter, title, emptyText }) => {
    const { t } = useTranslation('trace')

    if (!parameter) {
        return (
            <div className="trace-params-section">
                <span className="trace-params-title">{title}:</span>
                <Text type="secondary" className="trace-params-empty">
                    {emptyText || t('details.noResult')}
                </Text>
            </div>
        )
    }

    return (
        <div className="trace-params-section">
            <span className="trace-params-title">{title}:</span>
            <div className="trace-params-list">
                <ParameterTree param={parameter} paramKey="single-param" />
            </div>
        </div>
    )
}

export default TraceParameters
