import React from 'react'
import { Spin, Button, Result } from 'antd'
import {
    LoadingOutlined,
    ClockCircleOutlined,
    CheckCircleOutlined,
    StopOutlined,
    ExclamationCircleOutlined,
} from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import type { TraceExecutionStatus } from 'types/trace'
import {
    isTraceExecutionInProgress,
    isTraceExecutionTerminal,
    TRACE_EXECUTION_STATUS,
} from 'utils/traceExecutionStatus'

interface TraceProgressProps {
    status: TraceExecutionStatus | null
    message?: string
    onCancel?: () => void
    onDismiss?: () => void
    visible: boolean
}

/**
 * Progress overlay component showing trace execution status.
 * Displays during in-progress states (PENDING/STARTED).
 * Terminal statuses (ERROR/INTERRUPTED) are rendered by parent containers as banners.
 */
const TraceProgress: React.FC<TraceProgressProps> = ({
    status,
    message,
    onCancel,
    onDismiss,
    visible,
}) => {
    const { t } = useTranslation('trace')

    if (!visible || !status) {
        return null
    }

    // Don't show overlay for COMPLETED status
    if (status === TRACE_EXECUTION_STATUS.COMPLETED) {
        return null
    }

    const getStatusIcon = () => {
        switch (status) {
            case TRACE_EXECUTION_STATUS.PENDING:
                return <ClockCircleOutlined style={{ fontSize: 48, color: '#1890ff' }} />
            case TRACE_EXECUTION_STATUS.STARTED:
                return <Spin indicator={<LoadingOutlined spin style={{ fontSize: 48 }} />} />
            case TRACE_EXECUTION_STATUS.INTERRUPTED:
                return <StopOutlined style={{ fontSize: 48, color: '#faad14' }} />
            case TRACE_EXECUTION_STATUS.ERROR:
                return <ExclamationCircleOutlined style={{ fontSize: 48, color: '#ff4d4f' }} />
            default:
                return <CheckCircleOutlined style={{ fontSize: 48, color: '#52c41a' }} />
        }
    }

    const getStatusText = () => {
        switch (status) {
            case TRACE_EXECUTION_STATUS.PENDING:
                return t('progress.pending')
            case TRACE_EXECUTION_STATUS.STARTED:
                return t('progress.started')
            case TRACE_EXECUTION_STATUS.INTERRUPTED:
                return t('progress.interrupted')
            case TRACE_EXECUTION_STATUS.ERROR:
                return t('progress.error')
            default:
                return t('progress.completed')
        }
    }

    const showCancelButton = isTraceExecutionInProgress(status) && onCancel
    const showDismissButton = isTraceExecutionTerminal(status) && onDismiss

    return (
        <div className="trace-progress-overlay">
            <div className="trace-progress-content">
                <Result
                    icon={getStatusIcon()}
                    subTitle={message}
                    title={getStatusText()}
                    extra={
                        showCancelButton ? (
                            <Button danger onClick={onCancel}>
                                {t('progress.cancel')}
                            </Button>
                        ) : showDismissButton ? (
                            <Button onClick={onDismiss}>
                                {t('modal.actions.close')}
                            </Button>
                        ) : null
                    }
                />
            </div>
        </div>
    )
}

export default TraceProgress
