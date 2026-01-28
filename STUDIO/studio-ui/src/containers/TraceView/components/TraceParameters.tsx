import React, { useState } from 'react'
import { Spin, Typography } from 'antd'
import { PlusSquareOutlined, MinusSquareOutlined, LoadingOutlined } from '@ant-design/icons'
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

interface ValueNodeProps {
    name: string
    type?: string
    value: any
    depth?: number
    defaultExpanded?: boolean
}

/**
 * Renders a single value node with expand/collapse for complex values.
 */
const ValueNode: React.FC<ValueNodeProps> = ({
    name,
    type,
    value,
    depth = 0,
    defaultExpanded = false,
}) => {
    const [expanded, setExpanded] = useState(defaultExpanded)
    const isComplex = isComplexValue(value)

    const toggleExpand = () => {
        if (isComplex) {
            setExpanded(!expanded)
        }
    }

    const renderValue = () => {
        if (!isComplex) {
            const { display, className } = formatSimpleValue(value)
            return <span className={className}>{display}</span>
        }
        return <span className="trace-value-summary">{getComplexSummary(value)}</span>
    }

    const renderChildren = () => {
        if (!isComplex) return null

        const childNodes = Array.isArray(value)
            ? value.map((item, index) => (
                <ValueNode
                    key={index}
                    name={`[${index}]`}
                    value={item}
                    depth={depth + 1}
                />
            ))
            : Object.entries(value).map(([key, val]) => (
                <ValueNode
                    key={key}
                    name={key}
                    value={val}
                    depth={depth + 1}
                />
            ))

        return (
            <div className={`trace-value-children-wrapper ${expanded ? 'expanded' : ''}`}>
                <div className="trace-value-children">
                    {childNodes}
                </div>
            </div>
        )
    }

    return (
        <div className="trace-value-node">
            <div className="trace-value-row" onClick={toggleExpand}>
                {isComplex ? (
                    <span className="trace-expand-icon">
                        {expanded ? <MinusSquareOutlined /> : <PlusSquareOutlined />}
                    </span>
                ) : (
                    <span className="trace-expand-icon trace-expand-placeholder" />
                )}
                <span className="trace-value-name">{name}</span>
                {type && <span className="trace-value-type">{type}</span>}
                <span className="trace-value-equals">=</span>
                {renderValue()}
            </div>
            {renderChildren()}
        </div>
    )
}

interface ParameterItemProps {
    param: TraceParameterValue
    inline?: boolean
}

/**
 * Single parameter item with lazy loading support (IDEA debug style).
 */
const ParameterItem: React.FC<ParameterItemProps> = ({ param, inline = false }) => {
    const { t } = useTranslation('trace')
    const { fetchLazyParameter } = useTraceStore()
    const [loading, setLoading] = useState(false)
    const [loaded, setLoaded] = useState(false)
    const [loadedValue, setLoadedValue] = useState<any>(undefined)
    const [error, setError] = useState<string | null>(null)

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

    const displayValue = loaded ? loadedValue : param.value

    // Lazy parameter not yet loaded
    if (param.lazy && !loaded && displayValue === undefined && !loading) {
        return (
            <div className={`trace-param-item ${inline ? 'inline' : ''}`}>
                <span className="trace-expand-icon trace-expand-placeholder" />
                <span className="trace-value-name">{param.name}</span>
                <span className="trace-value-type">{param.description}</span>
                <span className="trace-value-equals">=</span>
                <span className="trace-value-lazy" onClick={handleLoadValue}>
                    {t('param.loadValue')}
                </span>
            </div>
        )
    }

    // Loading state
    if (loading) {
        return (
            <div className={`trace-param-item ${inline ? 'inline' : ''}`}>
                <span className="trace-expand-icon trace-expand-placeholder" />
                <span className="trace-value-name">{param.name}</span>
                <span className="trace-value-type">{param.description}</span>
                <span className="trace-value-equals">=</span>
                <Spin indicator={<LoadingOutlined spin />} size="small" />
            </div>
        )
    }

    // Error state
    if (error) {
        return (
            <div className={`trace-param-item ${inline ? 'inline' : ''}`}>
                <span className="trace-expand-icon trace-expand-placeholder" />
                <span className="trace-value-name">{param.name}</span>
                <span className="trace-value-type">{param.description}</span>
                <span className="trace-value-equals">=</span>
                <span className="trace-value-error">{error}</span>
            </div>
        )
    }

    // Loaded but value is undefined - show empty braces
    if (loaded && displayValue === undefined) {
        return (
            <div className={`trace-param-item ${inline ? 'inline' : ''}`}>
                <span className="trace-expand-icon trace-expand-placeholder" />
                <span className="trace-value-name">{param.name}</span>
                <span className="trace-value-type">{param.description}</span>
                <span className="trace-value-equals">=</span>
                <span className="trace-value-empty">{'{}'}</span>
            </div>
        )
    }

    return (
        <ValueNode
            name={param.name}
            type={param.description}
            value={displayValue}
            defaultExpanded={false}
        />
    )
}

interface TraceParametersProps {
    parameters?: TraceParameterValue[]
    title: string
    emptyText?: string
}

/**
 * Component for displaying trace parameters (IDEA debug style).
 * Format: Parameters: + param1 Type = value    param2 Type = value
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
                    <ParameterItem key={`${param.name}-${index}`} param={param} />
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
                <ParameterItem param={parameter} />
            </div>
        </div>
    )
}

export default TraceParameters
