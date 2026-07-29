import React, { useState, useEffect, useCallback } from 'react'
import { Modal, Result, notification, Spin } from 'antd'
import { LoadingOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { useGlobalEvents } from 'hooks'
import { traceService } from 'services/traceService'
import { retireTraceLaunch, stampTraceLaunch } from 'services/traceLaunchToken'
import CONFIG from 'services/config'
import { useStyles } from './TraceExecutionModal.styles'

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
    /** Advanced tracer checkbox on the JSF page: true opens the full debugger, false the business view. */
    advanced?: boolean
}

/** Save the exported trace text to the user's machine as trace.txt. */
const downloadTraceFile = (text: string): void => {
    const url = URL.createObjectURL(new Blob([text], { type: 'text/plain;charset=utf-8' }))
    const link = document.createElement('a')
    link.href = url
    link.download = 'trace.txt'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
}

/**
 * Launcher for the interactive trace debugger.
 *
 * On a trigger event it creates the debug session (so input parameters are sent server-side) and
 * opens the debugger in a new window, which then attaches to that session.
 *
 * @example dispatch a custom event 'openTraceExecutionModal' with the detail payload.
 */
export const TraceExecutionModal: React.FC = () => {
    const { t } = useTranslation('trace')
    const { styles } = useStyles()
    const { detail } = useGlobalEvents<TraceExecutionEventDetail>('openTraceExecutionModal')
    const [starting, setStarting] = useState(false)

    const openTraceWindow = useCallback((d: TraceExecutionEventDetail) => {
        const params = new URLSearchParams()
        params.set('tableId', d.tableId)
        if (d.fromModule) params.set('fromModule', d.fromModule)
        if (d.testRanges) params.set('testRanges', d.testRanges)
        // Carry the launch-time mode so the trace window opens straight into the business or advanced view.
        if (d.advanced) params.set('advanced', 'true')
        const url = `${CONFIG.CONTEXT}/trace/${encodeURIComponent(d.projectId)}?${params.toString()}`
        window.open(url, 'trace_win', 'width=1240,height=800,resizable=yes,scrollbars=yes')
    }, [])

    const launch = useCallback(async (d: TraceExecutionEventDetail) => {
        setStarting(true)
        // Stamp this launch before the session is created, so a debugger window closing while the request
        // is in flight sees a changed token and does not delete the session this launch is creating.
        const reserved = stampTraceLaunch()
        try {
            await traceService.startTrace(d.projectId, {
                tableId: d.tableId,
                ...(d.testRanges !== undefined && { testRanges: d.testRanges }),
                ...(d.fromModule !== undefined && { fromModule: d.fromModule }),
                ...(d.inputJson !== undefined && { inputJson: d.inputJson }),
                stopAtEntry: true,
            })
            if (d.downloadMode) {
                // Trace into File: replay the run, stream it to text, and save it — no debugger window.
                downloadTraceFile(await traceService.exportTrace(d.projectId, d.showRealNumbers))
            } else {
                openTraceWindow(d)
            }
        } catch (error: unknown) {
            // The launch failed, so nothing replaced the previous session — hand the token back to its owner.
            retireTraceLaunch(reserved)
            notification.error({
                title: t('modal.errors.startFailed'),
                description: error instanceof Error ? error.message : String(error),
            })
        } finally {
            setStarting(false)
            window.dispatchEvent(new CustomEvent('openTraceExecutionModal', { detail: null }))
        }
    }, [openTraceWindow, t])

    useEffect(() => {
        if (detail && Object.keys(detail).length > 0) {
            void launch(detail)
        }
    }, [detail, launch])

    return (
        <Modal
            className={styles.modal}
            closable={false}
            footer={null}
            mask={{ closable: false }}
            open={starting}
            title={t('modal.title')}
            width={420}
            zIndex={10000}
        >
            <Result
                icon={<Spin indicator={<LoadingOutlined spin style={{ fontSize: 48 }} />} />}
                title={t('modal.statuses.started')}
            />
        </Modal>
    )
}

export default TraceExecutionModal
