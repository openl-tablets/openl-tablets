import React, { useState, useEffect, useRef, useCallback } from 'react'
import { Modal, Button, Result, notification, Spin } from 'antd'
import {
    ClockCircleOutlined,
    LoadingOutlined,
    CheckCircleOutlined,
    StopOutlined,
    ExclamationCircleOutlined,
} from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { useGlobalEvents } from 'hooks'
import { useWebSocket } from 'hooks/useWebSocket'
import { traceService } from 'services/traceService'
import CONFIG from 'services/config'
import type { TraceExecutionStatus, TraceProgressMessage } from 'types/trace'
import type { WebSocketMessage } from 'services/websocket'
import './TraceExecutionModal.scss'

/**
 * Event detail passed from JSF to React via CustomEvent.
 * Supports both test tables and executable tables.
 */
export interface TraceExecutionEventDetail {
    projectId: string
    tableId: string
    moduleName: string
    showRealNumbers: boolean
    testRanges?: string
    fromModule?: string
    inputJson?: string
    downloadMode?: boolean
}

/**
 * Get status icon based on current execution status.
 */
const getStatusIcon = (status: TraceExecutionStatus | null) => {
    switch (status) {
        case 'PENDING':
            return <ClockCircleOutlined style={{ fontSize: 48, color: '#1890ff' }} />
        case 'STARTED':
            return <Spin indicator={<LoadingOutlined style={{ fontSize: 48 }} spin />} />
        case 'COMPLETED':
            return <CheckCircleOutlined style={{ fontSize: 48, color: '#52c41a' }} />
        case 'INTERRUPTED':
            return <StopOutlined style={{ fontSize: 48, color: '#faad14' }} />
        case 'ERROR':
            return <ExclamationCircleOutlined style={{ fontSize: 48, color: '#ff4d4f' }} />
        default:
            return <ClockCircleOutlined style={{ fontSize: 48, color: '#1890ff' }} />
    }
}

/**
 * TraceExecutionModal component.
 * Handles trace execution initialization from JSF, shows progress, and opens trace view on completion.
 *
 * @example to trigger this modal, dispatch a custom event 'openTraceExecutionModal' with details:
 * window.dispatchEvent(new CustomEvent('openTraceExecutionModal', {
 *     detail: { repositoryId, projectName, tableId, moduleName, showRealNumbers, ... }
 * }))
 */
export const TraceExecutionModal: React.FC = () => {
    const { t } = useTranslation('trace')
    const { detail } = useGlobalEvents<TraceExecutionEventDetail>('openTraceExecutionModal')
    const { subscribe, unsubscribe, connect, isConnected } = useWebSocket({ autoConnect: false })

    const [visible, setVisible] = useState(false)
    const [status, setStatus] = useState<TraceExecutionStatus | null>(null)
    const [message, setMessage] = useState<string | null>(null)
    const [projectId, setProjectId] = useState<string | null>(null)
    const [tableId, setTableId] = useState<string | null>(null)
    const [showRealNumbers, setShowRealNumbers] = useState(false)
    const [downloadMode, setDownloadMode] = useState(false)
    const [isCancelling, setIsCancelling] = useState(false)

    const subscriptionRef = useRef<string | null>(null)

    /**
     * Handle WebSocket progress message.
     */
    const handleProgressMessage = useCallback((msg: WebSocketMessage) => {
        try {
            const data: TraceProgressMessage = JSON.parse(msg.body)
            setStatus(data.status)
            setMessage(data.message || null)
        } catch {
            // Fallback: treat body as plain status string
            const statusValue = msg.body as TraceExecutionStatus
            setStatus(statusValue)
        }
    }, [])

    /**
     * Open trace view window.
     */
    const openTraceWindow = useCallback((projId: string, tblId: string, showReal: boolean) => {
        const viewUrl = `${CONFIG.CONTEXT}/trace/${encodeURIComponent(projId)}?tableId=${encodeURIComponent(tblId)}&showRealNumbers=${showReal}`
        window.open(viewUrl, 'trace_win', 'width=1240,height=700,resizable=yes,scrollbars=yes')
    }, [])

    /**
     * Download trace file.
     * The release=true parameter tells the server to clear trace from memory after export completes.
     */
    const downloadTraceFile = useCallback((projId: string, showReal: boolean) => {
        const downloadUrl = `${CONFIG.CONTEXT}/web/projects/${encodeURIComponent(projId)}/trace/export?showRealNumbers=${showReal}&release=true`
        window.location.href = downloadUrl
    }, [])

    /**
     * Close modal and cleanup.
     */
    const handleClose = useCallback(() => {
        // Unsubscribe from WebSocket
        if (subscriptionRef.current) {
            unsubscribe(subscriptionRef.current)
            subscriptionRef.current = null
        }
        setVisible(false)
        setStatus(null)
        setMessage(null)
        // Clear event detail
        window.dispatchEvent(new CustomEvent('openTraceExecutionModal', { detail: null }))
    }, [unsubscribe])

    /**
     * Start trace execution.
     */
    const startExecution = useCallback(async (eventDetail: TraceExecutionEventDetail) => {
        const { projectId: projId, tableId: tblId, showRealNumbers: showReal, downloadMode: download } = eventDetail

        // Reset state
        setVisible(true)
        setStatus('PENDING')
        setMessage(null)
        setProjectId(projId)
        setTableId(tblId)
        setShowRealNumbers(showReal)
        setDownloadMode(download || false)

        try {
            // 1. Connect WebSocket if needed
            if (!isConnected) {
                await connect()
            }

            // 2. Subscribe to progress topic BEFORE API call (use local vars, not state)
            const topic = `/user/topic/projects/${encodeURIComponent(projId)}/tables/${encodeURIComponent(tblId)}/trace/status`
            subscriptionRef.current = subscribe(topic, handleProgressMessage, `trace-modal-${Date.now()}`)

            // 3. Call trace API
            await traceService.startTrace(projId, {
                tableId: tblId,
                testRanges: eventDetail.testRanges,
                fromModule: eventDetail.fromModule,
                inputJson: eventDetail.inputJson
            })
        } catch (error: unknown) {
            const errorMessage = error instanceof Error ? error.message : String(error)
            notification.error({
                message: t('modal.errors.startFailed'),
                description: errorMessage
            })
            handleClose()
        }
    }, [isConnected, connect, subscribe, handleProgressMessage, handleClose, t])

    /**
     * Handle incoming event from JSF.
     */
    useEffect(() => {
        if (detail && Object.keys(detail).length > 0) {
            startExecution(detail)
        }
    }, [detail, startExecution])

    /**
     * Handle status changes - open window or download file on COMPLETED.
     */
    useEffect(() => {
        if (status === 'COMPLETED' && projectId && tableId) {
            if (downloadMode) {
                downloadTraceFile(projectId, showRealNumbers)
            } else {
                openTraceWindow(projectId, tableId, showRealNumbers)
            }
            handleClose()
        }
    }, [status, projectId, tableId, showRealNumbers, downloadMode, openTraceWindow, downloadTraceFile, handleClose])

    /**
     * Cancel trace execution.
     */
    const handleCancel = async () => {
        if (!projectId) return
        setIsCancelling(true)
        try {
            await traceService.cancelTrace(projectId)
            // Close modal after successful cancellation
            handleClose()
        } catch (error: unknown) {
            const errorMessage = error instanceof Error ? error.message : String(error)
            notification.error({
                message: t('modal.errors.cancelFailed'),
                description: errorMessage
            })
        } finally {
            setIsCancelling(false)
        }
    }

    /**
     * Get status text for display.
     */
    const getStatusText = () => {
        switch (status) {
            case 'PENDING':
                return t('modal.statuses.pending')
            case 'STARTED':
                return t('modal.statuses.started')
            case 'COMPLETED':
                return t('modal.statuses.completed')
            case 'INTERRUPTED':
                return t('modal.statuses.interrupted')
            case 'ERROR':
                return t('modal.statuses.error')
            default:
                return t('modal.statuses.pending')
        }
    }

    const canCancel = status === 'PENDING' || status === 'STARTED'
    const canClose = status === 'ERROR' || status === 'INTERRUPTED'

    const footerButtons = []
    if (canCancel) {
        footerButtons.push(
            <Button key="cancel" danger loading={isCancelling} onClick={handleCancel}>
                {t('modal.actions.cancel')}
            </Button>
        )
    }
    if (canClose) {
        footerButtons.push(
            <Button key="close" onClick={handleClose}>
                {t('modal.actions.close')}
            </Button>
        )
    }

    return (
        <Modal
            className="trace-execution-modal"
            wrapClassName="trace-execution-modal-wrapper"
            zIndex={10000}
            closable={false}
            footer={footerButtons.length > 0 ? footerButtons : null}
            maskClosable={false}
            open={visible}
            title={t('modal.title')}
            width={500}
        >
            <Result
                icon={getStatusIcon(status)}
                subTitle={message}
                title={getStatusText()}
            />
        </Modal>
    )
}

export default TraceExecutionModal
