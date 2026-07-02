import React, { useState, useEffect, useCallback } from 'react'
import { Modal, Result, notification, Spin } from 'antd'
import { LoadingOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { useGlobalEvents } from 'hooks'
import { traceService } from 'services/traceService'
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
        const url = `${CONFIG.CONTEXT}/trace/${encodeURIComponent(d.projectId)}?${params.toString()}`
        window.open(url, 'trace_win', 'width=1240,height=800,resizable=yes,scrollbars=yes')
    }, [])

    const launch = useCallback(async (d: TraceExecutionEventDetail) => {
        setStarting(true)
        try {
            await traceService.startTrace(d.projectId, {
                tableId: d.tableId,
                ...(d.testRanges !== undefined && { testRanges: d.testRanges }),
                ...(d.fromModule !== undefined && { fromModule: d.fromModule }),
                ...(d.inputJson !== undefined && { inputJson: d.inputJson }),
                stopAtEntry: true,
            })
            openTraceWindow(d)
        } catch (error: unknown) {
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
