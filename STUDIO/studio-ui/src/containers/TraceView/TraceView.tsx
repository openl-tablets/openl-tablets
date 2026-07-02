import React, { useEffect, useRef, useState, useCallback } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { Alert, Badge, Collapse, Segmented } from 'antd'
import type { BadgeProps } from 'antd'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
import type { DebugError } from 'types/trace'
import DebugToolbar from './components/DebugToolbar'
import DebugCallStack from './components/DebugCallStack'
import TraceTree from './components/TraceTree'
import BreakpointsPanel from './components/BreakpointsPanel'
import TraceDetails from './components/TraceDetails'
import useTraceProgress from './hooks/useTraceProgress'
import { isTraceExecutionError, isTraceExecutionTerminal } from 'utils/traceExecutionStatus'
import { useStyles } from './TraceView.styles'

interface TraceViewParams {
    projectId: string
}

// Distinct semantics per state: suspended (paused — your turn) reads as a calm amber, while running
// (busy — please wait) is the only animated, blue "processing" dot. No two states share a colour.
const STATUS_BADGE: Record<string, BadgeProps['status']> = {
    pending: 'default',
    running: 'processing',
    suspended: 'warning',
    completed: 'success',
    error: 'error',
    terminated: 'default',
}

/**
 * Failure description for the terminal banner: where it failed, plus a collapsible technical drill-down
 * so the everyday view stays free of stack traces and Java type names.
 */
const TerminalErrorDescription: React.FC<{ error: DebugError }> = ({ error }) => {
    const { t } = useTranslation('trace')
    const { styles } = useStyles()
    const where = [
        error.table ? t('error.inTable', { table: error.table }) : null,
        error.location ? t('error.atLocation', { location: error.location }) : null,
    ].filter(Boolean).join(' ')
    const technical = error.type || error.detail
    return (
        <>
            {where && <div className={styles.errorWhere}>{where}</div>}
            {technical && (
                <Collapse
                    ghost
                    size="small"
                    items={[{
                        key: 'tech',
                        label: t('error.technicalDetails'),
                        children: (
                            <>
                                {error.type && <div className={styles.errorType}>{error.type}</div>}
                                {error.detail && <pre className={styles.errorStack}>{error.detail}</pre>}
                            </>
                        ),
                    }]}
                />
            )}
        </>
    )
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
    const debugError = useTraceStore(s => s.debugError)
    const error = useTraceStore(s => s.error)

    const [leftPanelWidth, setLeftPanelWidth] = useState(35)
    const [isResizing, setIsResizing] = useState(false)
    const [bannerDismissed, setBannerDismissed] = useState(false)
    // Default to the simple call-tree view; the stepwise call stack is the "Advanced" mode.
    const [viewMode, setViewMode] = useState<'tree' | 'advanced'>('tree')
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
                    <span className={styles.statusPill} data-testid="debug-status">
                        <Badge status={STATUS_BADGE[status] || 'default'} text={t(`debug.status.${status}`)} />
                    </span>
                )}
            </div>
            {showTerminalBanner && (
                <Alert
                    closable
                    className={styles.errorBanner}
                    message={(isTraceExecutionError(status) && debugError?.summary) || t(`debug.status.${status}`)}
                    onClose={() => setBannerDismissed(true)}
                    type={isTraceExecutionError(status) ? 'error' : status === 'completed' ? 'success' : 'warning'}
                    description={
                        isTraceExecutionError(status) && debugError
                            ? <TerminalErrorDescription error={debugError} />
                            : undefined
                    }
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
                    <Segmented
                        block
                        className={styles.viewModeToggle}
                        data-testid="trace-view-mode"
                        onChange={(value) => setViewMode(value as 'tree' | 'advanced')}
                        size="small"
                        value={viewMode}
                        options={[
                            { label: t('tree.modeSimple'), value: 'tree' },
                            { label: t('tree.modeCallStack'), value: 'advanced' },
                        ]}
                    />
                    <BreakpointsPanel />
                    <div className={styles.viewContent}>
                        {viewMode === 'tree' ? <TraceTree /> : <DebugCallStack />}
                    </div>
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
