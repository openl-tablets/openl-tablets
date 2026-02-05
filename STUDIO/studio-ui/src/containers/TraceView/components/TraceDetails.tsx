import React from 'react'
import { Spin, Empty, Alert, Card } from 'antd'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
import TraceParameters, { SingleParameter } from './TraceParameters'
import TraceTableView from './TraceTableView'
import type { MessageDescription } from 'types/trace'
import './TraceDetails.scss'

/**
 * Component for displaying trace errors/warnings.
 */
const TraceErrors: React.FC<{ errors?: MessageDescription[] }> = ({ errors }) => {
    const { t } = useTranslation('trace')

    if (!errors || errors.length === 0) {
        return null
    }

    return (
        <Card title={t('details.errors')} size="small" className="trace-errors-card">
            {errors.map((error, index) => (
                <Alert
                    key={index}
                    type={
                        error.severity === 'ERROR'
                            ? 'error'
                            : error.severity === 'WARNING'
                              ? 'warning'
                              : 'info'
                    }
                    message={error.summary}
                    description={
                        <>
                            {error.detail && <div>{error.detail}</div>}
                            {error.sourceLocation && (
                                <div className="trace-error-location">
                                    {error.sourceLocation}
                                </div>
                            )}
                        </>
                    }
                    showIcon
                    style={{ marginBottom: 8 }}
                />
            ))}
        </Card>
    )
}

/**
 * Right panel component displaying selected node details.
 * Shows parameters, context, result, errors, and table view.
 */
const TraceDetails: React.FC = () => {
    const { t } = useTranslation('trace')
    const {
        selectedNodeId,
        selectedNodeDetails,
        detailsLoading,
    } = useTraceStore()

    // Use explicit null check - 0 is a valid node ID
    if (selectedNodeId === null) {
        return (
            <div className="trace-details trace-details-empty">
                <Empty
                    description={t('details.noSelection')}
                    image={Empty.PRESENTED_IMAGE_SIMPLE}
                />
            </div>
        )
    }

    if (detailsLoading) {
        return (
            <div className="trace-details trace-details-loading">
                <Spin tip={t('loadingDetails')} />
            </div>
        )
    }

    if (!selectedNodeDetails) {
        return (
            <div className="trace-details trace-details-empty">
                <Empty
                    description={t('errors.detailsFailed')}
                    image={Empty.PRESENTED_IMAGE_SIMPLE}
                />
            </div>
        )
    }

    const { parameters, context, result, errors } = selectedNodeDetails

    // Combine parameters with context (context becomes part of parameters)
    const allParameters = context
        ? [...(parameters || []), context]
        : parameters

    return (
        <div className="trace-details">
            {/* Parameters (including context) */}
            <TraceParameters
                parameters={allParameters}
                title={t('details.parameters')}
                emptyText={t('details.noParameters')}
            />

            {/* Returned Result */}
            <SingleParameter
                parameter={result}
                title={t('details.result')}
                emptyText={t('details.noResult')}
            />

            {/* Errors */}
            <TraceErrors errors={errors} />

            {/* Traced Table */}
            <TraceTableView nodeId={selectedNodeId} />
        </div>
    )
}

export default TraceDetails
