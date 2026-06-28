import React, { useEffect, useRef, useState, useCallback } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { Alert, Tag } from 'antd'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
import DebugToolbar from './components/DebugToolbar'
import DebugCallStack from './components/DebugCallStack'
import TraceDetails from './components/TraceDetails'
import useTraceProgress from './hooks/useTraceProgress'
import { isTraceExecutionError, isTraceExecutionTerminal } from 'utils/traceExecutionStatus'
import { useStyles } from './TraceView.styles'

interface TraceViewParams {
    projectId: string
}

const STATUS_COLOR: Record<string, string> = {
    SUSPENDED: 'processing',
    RUNNING: 'blue',
    PENDING: 'default',
    COMPLETED: 'success',
    ERROR: 'error',
    TERMINATED: 'warning',
}

/**
 * Interactive trace debugger page.
 *
 * URL: /trace/{projectId}?tableId={tableId}&fromModule=&testRanges=
 */
const TraceView: React.FC = () => {
    const { t } = useTranslation('trace')
    const { styles, cx } = useStyles()
    const { projectId } = useParams<keyof TraceViewParams>()
    const [searchParams] = useSearchParams()
    const tableId = searchParams.get('tableId')
    const fromModule = searchParams.get('fromModule')
    const testRanges = searchParams.get('testRanges')

    const setRouteParams = useTraceStore(s => s.setRouteParams)
    const start = useTraceStore(s => s.start)
    const loadBreakpoints = useTraceStore(s => s.loadBreakpoints)
    const reset = useTraceStore(s => s.reset)
    const status = useTraceStore(s => s.status)
    const errorMessage = useTraceStore(s => s.errorMessage)
    const error = useTraceStore(s => s.error)

    const [leftPanelWidth, setLeftPanelWidth] = useState(35)
    const [isResizing, setIsResizing] = useState(false)
    const [bannerDismissed, setBannerDismissed] = useState(false)
    const containerRef = useRef<HTMLDivElement>(null)

    useTraceProgress({
        projectId: projectId || '',
        tableId: tableId || '',
        enabled: !!projectId && !!tableId,
    })

    useEffect(() => {
        if (projectId && tableId) {
            setRouteParams({ projectId, tableId, fromModule, testRanges })
            void loadBreakpoints()
            void start()
        }
        return () => reset()
    }, [projectId, tableId, fromModule, testRanges, setRouteParams, loadBreakpoints, start, reset])

    const handleMouseDown = useCallback((e: React.MouseEvent) => {
        e.preventDefault()
        setIsResizing(true)
    }, [])

    const handleMouseMove = useCallback((e: MouseEvent) => {
        if (!isResizing || !containerRef.current) return
        const rect = containerRef.current.getBoundingClientRect()
        const newWidth = ((e.clientX - rect.left) / rect.width) * 100
        if (newWidth >= 15 && newWidth <= 70) {
            setLeftPanelWidth(newWidth)
        }
    }, [isResizing])

    const handleMouseUp = useCallback(() => setIsResizing(false), [])

    useEffect(() => {
        if (isResizing) {
            document.addEventListener('mousemove', handleMouseMove)
            document.addEventListener('mouseup', handleMouseUp)
        }
        return () => {
            document.removeEventListener('mousemove', handleMouseMove)
            document.removeEventListener('mouseup', handleMouseUp)
        }
    }, [isResizing, handleMouseMove, handleMouseUp])

    useEffect(() => {
        if (status && !isTraceExecutionTerminal(status)) {
            setBannerDismissed(false)
        }
    }, [status])

    if (!projectId || !tableId) {
        return (
            <div className={cx(styles.view, styles.viewError)} id="trace-view">
                <Alert description={t('errors.missingParams')} message={t('errors.notFound')} type="error" />
            </div>
        )
    }

    const showTerminalBanner = !bannerDismissed && isTraceExecutionTerminal(status)

    return (
        <div className={styles.debugView} id="trace-view">
            <div className={styles.toolbar} data-testid="debug-header">
                <DebugToolbar />
                {status && (
                    <Tag color={STATUS_COLOR[status] || 'default'} data-testid="debug-status">
                        {t(`debug.status.${status}`)}
                    </Tag>
                )}
            </div>
            {showTerminalBanner && (
                <Alert
                    closable
                    className={styles.errorBanner}
                    description={errorMessage || undefined}
                    message={t(`debug.status.${status}`)}
                    onClose={() => setBannerDismissed(true)}
                    type={isTraceExecutionError(status) ? 'error' : status === 'COMPLETED' ? 'success' : 'warning'}
                />
            )}
            {error && (
                <Alert closable className={styles.errorBanner} message={error} type="error" />
            )}
            <div
                ref={containerRef}
                className={cx(styles.panels, isResizing && styles.resizing)}
            >
                <div
                    className={cx(styles.leftPanel, isResizing && styles.panelDisabled)}
                    style={{ width: `${leftPanelWidth}%` }}
                >
                    <DebugCallStack />
                </div>
                <div className={styles.resizer} onMouseDown={handleMouseDown} />
                <div
                    className={cx(styles.rightPanel, isResizing && styles.panelDisabled)}
                    style={{ width: `${100 - leftPanelWidth}%` }}
                >
                    <TraceDetails />
                </div>
            </div>
        </div>
    )
}

export default TraceView
