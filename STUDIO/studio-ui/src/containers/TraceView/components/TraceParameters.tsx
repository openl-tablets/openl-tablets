import React, { useState } from 'react'
import { Card, Descriptions, Button, Spin, Typography, Tag } from 'antd'
import { DownOutlined, RightOutlined, LoadingOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
import type { TraceParameterValue } from 'types/trace'

const { Text, Paragraph } = Typography

interface TraceParametersProps {
    parameters?: TraceParameterValue[]
    title: string
    emptyText?: string
}

interface ParameterItemProps {
    param: TraceParameterValue
}

/**
 * Single parameter item with lazy loading support.
 */
const ParameterItem: React.FC<ParameterItemProps> = ({ param }) => {
    const { t } = useTranslation('trace')
    const { fetchLazyParameter } = useTraceStore()
    const [loading, setLoading] = useState(false)
    const [loadedValue, setLoadedValue] = useState<any>(null)
    const [expanded, setExpanded] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const handleLoadValue = async () => {
        if (!param.lazy || param.parameterId == null) return

        setLoading(true)
        setError(null)
        try {
            const result = await fetchLazyParameter(param.parameterId)
            setLoadedValue(result.value)
        } catch (err: any) {
            setError(err?.message || t('errors.parameterFailed'))
        } finally {
            setLoading(false)
        }
    }

    const displayValue = loadedValue ?? param.value
    const isComplex =
        typeof displayValue === 'object' && displayValue !== null

    const renderValue = () => {
        if (param.lazy && displayValue === undefined && !loading) {
            return (
                <Button
                    type="link"
                    size="small"
                    onClick={handleLoadValue}
                    disabled={loading}
                >
                    {t('param.loadValue')}
                </Button>
            )
        }

        if (loading) {
            return <Spin indicator={<LoadingOutlined spin />} size="small" />
        }

        if (error) {
            return <Text type="danger">{error}</Text>
        }

        if (displayValue === null) {
            return <Text type="secondary">null</Text>
        }

        if (displayValue === undefined) {
            return <Text type="secondary">undefined</Text>
        }

        if (isComplex) {
            return (
                <div className="trace-param-complex">
                    <Button
                        type="text"
                        size="small"
                        icon={expanded ? <DownOutlined /> : <RightOutlined />}
                        onClick={() => setExpanded(!expanded)}
                    >
                        {expanded ? t('param.collapse') : t('param.expand')}
                    </Button>
                    {expanded && (
                        <Paragraph>
                            <pre className="trace-param-json">
                                {JSON.stringify(displayValue, null, 2)}
                            </pre>
                        </Paragraph>
                    )}
                </div>
            )
        }

        return <Text>{String(displayValue)}</Text>
    }

    return (
        <div className="trace-param-item">
            <div className="trace-param-header">
                <Text strong>{param.name}</Text>
                <Tag color="blue">{param.description}</Tag>
            </div>
            <div className="trace-param-value">{renderValue()}</div>
        </div>
    )
}

/**
 * Component for displaying trace parameters (input, context, result).
 */
const TraceParameters: React.FC<TraceParametersProps> = ({
    parameters,
    title,
    emptyText,
}) => {
    const { t } = useTranslation('trace')

    if (!parameters || parameters.length === 0) {
        return (
            <Card title={title} size="small" className="trace-params-card">
                <Text type="secondary">{emptyText || t('details.noParameters')}</Text>
            </Card>
        )
    }

    return (
        <Card title={title} size="small" className="trace-params-card">
            {parameters.map((param, index) => (
                <ParameterItem key={`${param.name}-${index}`} param={param} />
            ))}
        </Card>
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
            <Card title={title} size="small" className="trace-params-card">
                <Text type="secondary">{emptyText || t('details.noResult')}</Text>
            </Card>
        )
    }

    return (
        <Card title={title} size="small" className="trace-params-card">
            <ParameterItem param={parameter} />
        </Card>
    )
}

export default TraceParameters
