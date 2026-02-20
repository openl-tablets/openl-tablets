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

interface TraceProgressProps {
    status: TraceExecutionStatus | null
    message?: string
    onCancel?: () => void
    visible: boolean
}

/**
 * Progress overlay component showing trace execution status.
 * Displays during PENDING/STARTED states and on ERROR.
 */
const TraceProgress: React.FC<TraceProgressProps> = ({
    status,
    message,
    onCancel,
    visible,
}) => {
    const { t } = useTranslation('trace')

    if (!visible || !status) {
        return null
    }

    // Don't show overlay for COMPLETED status
    if (status === 'COMPLETED') {
        return null
    }

    const getStatusIcon = () => {
        switch (status) {
            case 'PENDING':
                return <ClockCircleOutlined style={{ fontSize: 48, color: '#1890ff' }} />
            case 'STARTED':
                return <Spin indicator={<LoadingOutlined style={{ fontSize: 48 }} spin />} />
            case 'INTERRUPTED':
                return <StopOutlined style={{ fontSize: 48, color: '#faad14' }} />
            case 'ERROR':
                return <ExclamationCircleOutlined style={{ fontSize: 48, color: '#ff4d4f' }} />
            default:
                return <CheckCircleOutlined style={{ fontSize: 48, color: '#52c41a' }} />
        }
    }

    const getStatusText = () => {
        switch (status) {
            case 'PENDING':
                return t('progress.pending')
            case 'STARTED':
                return t('progress.started')
            case 'INTERRUPTED':
                return t('progress.interrupted')
            case 'ERROR':
                return t('progress.error')
            default:
                return t('progress.completed')
        }
    }

    const showCancelButton = (status === 'PENDING' || status === 'STARTED') && onCancel

    return (
        <div className="trace-progress-overlay">
            <div className="trace-progress-content">
                <Result
                    icon={getStatusIcon()}
                    title={getStatusText()}
                    subTitle={message}
                    extra={
                        showCancelButton && (
                            <Button onClick={onCancel} danger>
                                {t('progress.cancel')}
                            </Button>
                        )
                    }
                />
            </div>
        </div>
    )
}

export default TraceProgress
