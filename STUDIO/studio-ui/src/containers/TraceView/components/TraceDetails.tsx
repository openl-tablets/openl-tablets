import React from 'react'
import { Spin, Empty, Alert, Card } from 'antd'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
import TraceParameters, { SingleParameter } from './TraceParameters'
import TraceTableView from './TraceTableView'
import CopyJsonButton from './CopyJsonButton'
import type { MessageDescription } from 'types/trace'
import { useStyles } from './TraceDetails.styles'

/**
 * Component for displaying trace errors/warnings.
 */
const TraceErrors: React.FC<{ errors?: MessageDescription[] | undefined }> = ({ errors }) => {
    const { t } = useTranslation('trace')
    const { styles } = useStyles()

    if (!errors || errors.length === 0) {
        return null
    }

    return (
        <Card className={styles.errorsCard} size="small" title={t('details.errors')}>
            {errors.map((error, index) => (
                <Alert
                    key={index}
                    showIcon
                    message={error.summary}
                    style={{ marginBottom: 8 }}
                    description={
                        <>
                            {error.detail && <div>{error.detail}</div>}
                            {error.sourceLocation && (
                                <div className={styles.errorLocation}>
                                    {error.sourceLocation}
                                </div>
                            )}
                        </>
                    }
                    type={
                        error.severity === 'ERROR'
                            ? 'error'
                            : error.severity === 'WARNING'
                                ? 'warning'
                                : 'info'
                    }
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
    const { styles, cx } = useStyles()
    const {
        selectedNodeId,
        selectedNodeDetails,
        detailsLoading,
    } = useTraceStore()

    // Use explicit null check - 0 is a valid node ID
    if (selectedNodeId === null) {
        return (
            <div className={cx(styles.details, styles.detailsCentered)}>
                <Empty
                    description={t('details.noSelection')}
                    image={Empty.PRESENTED_IMAGE_SIMPLE}
                />
            </div>
        )
    }

    if (detailsLoading) {
        return (
            <div className={cx(styles.details, styles.detailsCentered)}>
                <Spin description={t('loadingDetails')} />
            </div>
        )
    }

    if (!selectedNodeDetails) {
        return (
            <div className={cx(styles.details, styles.detailsCentered)}>
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
        <div className={styles.details}>
            {/* Parameters (including context) */}
            <TraceParameters
                copyButton={<CopyJsonButton data={allParameters} tooltipKey="copy.parameters" />}
                emptyText={t('details.noParameters')}
                parameters={allParameters}
                title={t('details.parameters')}
            />
            {/* Returned Result */}
            <SingleParameter
                copyButton={<CopyJsonButton data={result} tooltipKey="copy.result" />}
                emptyText={t('details.noResult')}
                parameter={result}
                title={t('details.result')}
            />
            {/* Errors */}
            <TraceErrors errors={errors} />
            {/* Traced Table */}
            <TraceTableView nodeId={selectedNodeId} />
        </div>
    )
}

export default TraceDetails
