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
 * Valid trace execution status values.
 */
const VALID_TRACE_STATUSES: readonly TraceExecutionStatus[] = [
    'PENDING',
    'STARTED',
    'COMPLETED',
    'INTERRUPTED',
    'ERROR',
] as const

/**
 * Type guard to validate if a value is a valid TraceExecutionStatus.
 */
const isValidTraceExecutionStatus = (value: unknown): value is TraceExecutionStatus => {
    return typeof value === 'string' && VALID_TRACE_STATUSES.includes(value as TraceExecutionStatus)
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
    // Execution token to detect stale async operations after modal close
    const executionTokenRef = useRef<string | null>(null)

    /**
     * Handle WebSocket progress message.
     */
    const handleProgressMessage = useCallback((msg: WebSocketMessage) => {
        try {
            const data: TraceProgressMessage = JSON.parse(msg.body)
            if (isValidTraceExecutionStatus(data.status)) {
                setStatus(data.status)
                setMessage(data.message || null)
            } else {
                console.error('Invalid trace status in parsed message:', data.status)
                setMessage(null)
            }
        } catch (error) {
            // Log parse error for debugging
            console.error('Failed to parse trace progress message:', error, 'Raw body:', msg.body)
            // Fallback: treat body as plain status string, but validate it first
            if (isValidTraceExecutionStatus(msg.body)) {
                setStatus(msg.body)
                setMessage(null)
            }
            // If body is not a valid status, keep previous state (don't update)
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
     * Close modal and cleanup.
     * Clears execution token to signal in-flight async operations to abort.
     */
    const handleClose = useCallback(() => {
        // Clear execution token to abort any in-flight async operations
        executionTokenRef.current = null

        // Unsubscribe from WebSocket
        if (subscriptionRef.current) {
            unsubscribe(subscriptionRef.current)
            subscriptionRef.current = null
        }
        setVisible(false)
        setStatus(null)
        setMessage(null)
        setIsCancelling(false)
        // Clear event detail
        window.dispatchEvent(new CustomEvent('openTraceExecutionModal', { detail: null }))
    }, [unsubscribe])

    /**
     * Start trace execution.
     * Uses execution token to detect and abort stale operations after modal close.
     */
    const startExecution = useCallback(async (eventDetail: TraceExecutionEventDetail) => {
        const { projectId: projId, tableId: tblId, showRealNumbers: showReal, downloadMode: download } = eventDetail

        // Generate unique execution token for this execution
        const executionToken = `exec-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
        executionTokenRef.current = executionToken

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

            // Check if execution was cancelled during connect
            if (executionTokenRef.current !== executionToken) {
                console.warn('Trace execution aborted: modal closed during WebSocket connect')
                return
            }

            // 2. Subscribe to progress topic BEFORE API call (use local vars, not state)
            const topic = `/user/topic/projects/${encodeURIComponent(projId)}/tables/${encodeURIComponent(tblId)}/trace/status`
            const subscriptionId = subscribe(topic, handleProgressMessage, `trace-modal-${Date.now()}`)

            // Only store subscription if execution is still valid
            if (executionTokenRef.current !== executionToken) {
                // Execution was cancelled during subscribe - clean up immediately
                console.warn('Trace execution aborted: modal closed during WebSocket subscribe')
                unsubscribe(subscriptionId)
                return
            }
            subscriptionRef.current = subscriptionId

            // 3. Call trace API
            await traceService.startTrace(projId, {
                tableId: tblId,
                testRanges: eventDetail.testRanges,
                fromModule: eventDetail.fromModule,
                inputJson: eventDetail.inputJson
            })

            // Check if execution was cancelled during API call
            if (executionTokenRef.current !== executionToken) {
                console.warn('Trace execution aborted: modal closed during startTrace API call')
                return
            }
        } catch (error: unknown) {
            // Only show error if this execution is still active
            if (executionTokenRef.current !== executionToken) {
                console.warn('Trace execution error ignored: modal was closed')
                return
            }
            const errorMessage = error instanceof Error ? error.message : String(error)
            notification.error({
                message: t('modal.errors.startFailed'),
                description: errorMessage
            })
            handleClose()
        }
    }, [isConnected, connect, subscribe, unsubscribe, handleProgressMessage, handleClose, t])

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
                // Use release=true to clear trace from memory after download
                traceService.exportTrace(projectId, showRealNumbers, true)
            } else {
                openTraceWindow(projectId, tableId, showRealNumbers)
            }
            handleClose()
        }
    }, [status, projectId, tableId, showRealNumbers, downloadMode, openTraceWindow, handleClose])

    /**
     * Cancel trace execution.
     * Uses execution token to detect and abort stale operations after modal close.
     */
    const handleCancel = async () => {
        if (!projectId) return

        // Capture current execution token to detect if modal closes during cancel
        const currentToken = executionTokenRef.current

        setIsCancelling(true)
        try {
            await traceService.cancelTrace(projectId)

            // Only close if this cancel operation is still relevant
            if (executionTokenRef.current === currentToken && currentToken !== null) {
                handleClose()
            }
        } catch (error: unknown) {
            // Only show error if modal is still open with the same execution
            if (executionTokenRef.current === currentToken && currentToken !== null) {
                const errorMessage = error instanceof Error ? error.message : String(error)
                notification.error({
                    message: t('modal.errors.cancelFailed'),
                    description: errorMessage
                })
                setIsCancelling(false)
            }
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
