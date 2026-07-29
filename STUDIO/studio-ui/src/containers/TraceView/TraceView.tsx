import React, { useEffect, useRef, useState, useCallback } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { Alert, Collapse, Segmented, Space, Spin, Tag } from 'antd'
import { SyncOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
import type { DebugError, DebugStatus } from 'types/trace'
import DebugToolbar from './components/DebugToolbar'
import DebugCallStack from './components/DebugCallStack'
import TraceTree from './components/TraceTree'
import SimpleTraceTree from './components/SimpleTraceTree'
import HotspotsPanel from './components/HotspotsPanel'
import BreakpointsPanel from './components/BreakpointsPanel'
import WatchPanel from './components/WatchPanel'
import TraceDetails from './components/TraceDetails'
import useTraceProgress from './hooks/useTraceProgress'
import useTerminateOnClose from './hooks/useTerminateOnClose'
import {
    isTraceExecutionAbnormalTerminal,
    isTraceExecutionError,
    isTraceExecutionTerminal,
} from 'utils/traceExecutionStatus'
import { useStyles } from './TraceView.styles'

interface TraceViewParams {
    projectId: string
}

// Distinct semantics per state: suspended (paused — your turn) reads as a calm amber, while running
// (busy — please wait) is the only animated, blue "calculating" badge. No two states share a colour.
const STATUS_STYLE = {
    pending: 'statusNeutral',
    running: 'statusRunning',
    suspended: 'statusPaused',
    completed: 'statusFinished',
    error: 'statusFailed',
    terminated: 'statusNeutral',
} as const satisfies Record<DebugStatus, string>

// Left-panel tabs of the advanced debugger: the call tree, the stepwise execution path, and the profiler
// hot-spots overview that appears as a third tab only while profiling.
type ViewMode = 'tree' | 'advanced' | 'hotspots'

const VIEW_COMPONENTS: Record<ViewMode, React.FC> = {
    tree: TraceTree,
    advanced: DebugCallStack,
    hotspots: HotspotsPanel,
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
    // The trace mode is chosen at launch (the Advanced tracer checkbox on the JSF page), not in this view.
    const advancedLaunch = searchParams.get('advanced') === 'true'

    const setRouteParams = useTraceStore(s => s.setRouteParams)
    const start = useTraceStore(s => s.start)
    const loadBreakpoints = useTraceStore(s => s.loadBreakpoints)
    const reset = useTraceStore(s => s.reset)
    const status = useTraceStore(s => s.status)
    const debugError = useTraceStore(s => s.debugError)
    const error = useTraceStore(s => s.error)
    const profiling = useTraceStore(s => s.profiling)
    const advanced = useTraceStore(s => s.advanced)
    const simpleRun = useTraceStore(s => s.simpleRun)
    const simpleLoading = useTraceStore(s => s.simpleLoading)

    const [leftPanelWidth, setLeftPanelWidth] = useState(35)
    const [isResizing, setIsResizing] = useState(false)
    const [bannerDismissed, setBannerDismissed] = useState(false)
    const [viewMode, setViewMode] = useState<ViewMode>('tree')
    const [busy, setBusy] = useState(false)
    const containerRef = useRef<HTMLDivElement>(null)

    // The hot-spots tab is profiling-only; fall back to the tree if profiling is switched off while it is open.
    useEffect(() => {
        if (!profiling && viewMode === 'hotspots') {
            setViewMode('tree')
        }
    }, [profiling, viewMode])

    useTraceProgress({
        projectId: projectId || '',
        tableId: tableId || '',
        enabled: !!projectId && !!tableId,
    })

    // There is no Stop button — closing the debugger window terminates the session instead.
    useTerminateOnClose(projectId)

    useEffect(() => {
        if (projectId && tableId) {
            setRouteParams({ projectId, tableId, fromModule, testRanges, advanced: advancedLaunch })
            if (advancedLaunch) {
                void loadBreakpoints()
                void start()
            } else {
                // Business mode is fixed at launch, so it runs straight away — there is no Run button.
                void simpleRun()
            }
        }
        return () => reset()
    }, [projectId, tableId, fromModule, testRanges, advancedLaunch,
        setRouteParams, loadBreakpoints, start, simpleRun, reset])

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

    // Dim the content with a spinner once a run lasts a moment, so a heavy request is clearly busy — for the
    // advanced debugger while it runs, and for the business view for its whole auto-run (which has no Run
    // button to signal it started). Delayed so quick step-to-step or fast runs do not flash it.
    const running = status === 'running' || simpleLoading
    useEffect(() => {
        if (!running) {
            setBusy(false)
            return undefined
        }
        const id = setTimeout(() => setBusy(true), 500)
        return () => clearTimeout(id)
    }, [running])

    if (!projectId || !tableId) {
        return (
            <div className={cx(styles.view, styles.viewError)} id="trace-view">
                <Alert description={t('errors.missingParams')} title={t('errors.notFound')} type="error" />
            </div>
        )
    }

    // A clean finish needs no banner — the status tag already says Finished. The advanced mode also
    // flags an interrupted run; in the simple view a stop is just click mechanics (inspections restart
    // the session), so only a real failure warrants a banner there.
    const showTerminalBanner = !bannerDismissed && (advanced
        ? isTraceExecutionAbnormalTerminal(status)
        : isTraceExecutionError(status))
    const isError = isTraceExecutionError(status)
    const ActiveView = VIEW_COMPONENTS[viewMode]
    const bannerType = isError ? 'error' : 'warning'
    // The business view shows no status pills — its states (Finished, Paused, …) are stepping mechanics that
    // only add noise there; a real failure still surfaces through the error banner below. The debugger keeps them.
    const statusVisible = status && advanced

    return (
        <div className={styles.debugView} id="trace-view">
            {/* The mode is fixed at launch (the Advanced tracer checkbox on the JSF page): the advanced
                debugger keeps its toolbar and status, while the business view runs straight away and has no
                toolbar of its own — its only control, Show detailed view, sits in the tree panel. */}
            {advanced && (
                <div className={styles.toolbar} data-testid="debug-header">
                    <div>
                        <DebugToolbar />
                    </div>
                    <Space size="middle">
                        {statusVisible && (
                            <Tag
                                className={cx(styles.statusTag, styles[STATUS_STYLE[status]])}
                                data-testid="debug-status"
                                icon={status === 'running' ? <SyncOutlined spin /> : undefined}
                            >
                                {t(`debug.status.${status}`)}
                            </Tag>
                        )}
                    </Space>
                </div>
            )}
            {showTerminalBanner && (
                <Alert
                    className={styles.errorBanner}
                    closable={{ onClose: () => setBannerDismissed(true) }}
                    title={(isError && debugError?.summary) || t(`debug.status.${status}`)}
                    type={bannerType}
                    description={
                        isError && debugError
                            ? <TerminalErrorDescription error={debugError} />
                            : undefined
                    }
                />
            )}
            {error && (
                <Alert closable className={styles.errorBanner} title={error} type="error" />
            )}
            <div
                ref={containerRef}
                className={cx(styles.panels, isResizing && styles.resizing)}
            >
                {busy && (
                    <div className={styles.runningOverlay} data-testid="trace-running-overlay">
                        <div className={styles.runningCard}>
                            <Spin size="large" />
                            <span className={styles.runningText}>
                                {advanced ? t('debug.runningNotice') : t('simple.calculating')}
                            </span>
                        </div>
                    </div>
                )}
                <div
                    className={cx(styles.leftPanel, isResizing && styles.panelDisabled)}
                    style={{ width: `${leftPanelWidth}%` }}
                >
                    {/* The business view is one tree: no view tabs, breakpoints, watches, or execution path. */}
                    {advanced ? (
                        <>
                            <Segmented
                                block
                                className={styles.viewModeToggle}
                                data-testid="trace-view-mode"
                                onChange={(value) => setViewMode(value as ViewMode)}
                                size="small"
                                value={viewMode}
                                options={[
                                    { label: t('tree.modeSimple'), value: 'tree' },
                                    { label: t('tree.modeCallStack'), value: 'advanced' },
                                    // The hot-spots overview only exists in profiling mode (it needs the executed tree).
                                    ...(profiling ? [{ label: t('hotspots.tab'), value: 'hotspots' }] : []),
                                ]}
                            />
                            <BreakpointsPanel />
                            <WatchPanel />
                            <div className={styles.viewContent}>
                                <ActiveView />
                            </div>
                        </>
                    ) : (
                        <div className={styles.viewContent}>
                            <SimpleTraceTree />
                        </div>
                    )}
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
