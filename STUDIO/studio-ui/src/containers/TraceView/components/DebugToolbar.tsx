import React from 'react'
import { Button, Divider, notification, Space, Switch, Tooltip } from 'antd'
import {
    CaretRightOutlined,
    PauseOutlined,
    ReloadOutlined,
    VerticalAlignBottomOutlined,
    VerticalAlignTopOutlined,
    EnterOutlined,
} from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
import { isTraceExecutionInProgress } from 'utils/traceExecutionStatus'

/**
 * Debugger control toolbar: resume/pause, step into/over/out, and rerun.
 * Resume and pause share one slot — the one that applies to the current status is shown.
 * Buttons enable based on the current session status.
 */
const DebugToolbar: React.FC = () => {
    const { t } = useTranslation('trace')
    const status = useTraceStore(s => s.status)
    const loading = useTraceStore(s => s.loading)
    const stepInto = useTraceStore(s => s.stepInto)
    const stepOver = useTraceStore(s => s.stepOver)
    const stepOut = useTraceStore(s => s.stepOut)
    const resume = useTraceStore(s => s.resume)
    const pause = useTraceStore(s => s.pause)
    const rerun = useTraceStore(s => s.rerun)
    const profiling = useTraceStore(s => s.profiling)
    const setProfiling = useTraceStore(s => s.setProfiling)

    const [pausePending, setPausePending] = React.useState(false)

    const suspended = status === 'suspended'
    const inProgress = isTraceExecutionInProgress(status)

    // Disable pause the instant it is clicked so a rapid second click cannot issue a concurrent command;
    // clear the flag once the request settles, whether it resolved or failed.
    const requestPause = (): void => {
        setPausePending(true)
        void pause().finally(() => setPausePending(false))
    }

    return (
        <Space data-testid="debug-toolbar" size="small">
            {inProgress ? (
                <Tooltip title={t('debug.pause')}>
                    <Button
                        data-testid="debug-pause"
                        // Only an actually-running worker can be paused — not one that is still starting
                        // (pending) — and not while a pause request is already in flight or a step is loading.
                        disabled={status !== 'running' || loading || pausePending}
                        icon={<PauseOutlined />}
                        onClick={requestPause}
                        type="text"
                    />
                </Tooltip>
            ) : (
                <Tooltip title={t('debug.resume')}>
                    <Button
                        data-testid="debug-resume"
                        disabled={!suspended || loading}
                        icon={<CaretRightOutlined />}
                        onClick={resume}
                        type="text"
                    />
                </Tooltip>
            )}
            <Divider orientation="vertical" style={{ height: '1.2em', margin: 0 }} />
            <Tooltip title={t('debug.stepInto')}>
                <Button
                    data-testid="debug-step-into"
                    disabled={!suspended || loading}
                    icon={<VerticalAlignBottomOutlined />}
                    onClick={stepInto}
                    type="text"
                />
            </Tooltip>
            <Tooltip title={t('debug.stepOver')}>
                <Button
                    data-testid="debug-step-over"
                    disabled={!suspended || loading}
                    icon={<EnterOutlined />}
                    onClick={stepOver}
                    type="text"
                />
            </Tooltip>
            <Tooltip title={t('debug.stepOut')}>
                <Button
                    data-testid="debug-step-out"
                    disabled={!suspended || loading}
                    icon={<VerticalAlignTopOutlined />}
                    onClick={stepOut}
                    type="text"
                />
            </Tooltip>
            <Divider orientation="vertical" style={{ height: '1.2em', margin: 0 }} />
            <Tooltip title={t('debug.rerun')}>
                <Button
                    data-testid="debug-rerun"
                    disabled={loading}
                    icon={<ReloadOutlined />}
                    onClick={() => void rerun()}
                    type="text"
                />
            </Tooltip>
            <Divider orientation="vertical" style={{ height: '1.2em', margin: 0 }} />
            <Tooltip title={t('debug.profilingHint')}>
                <Space size={4}>
                    <Switch
                        checked={profiling}
                        data-testid="debug-profiling"
                        disabled={loading}
                        size="small"
                        onChange={(checked) => {
                            void setProfiling(checked)
                            if (checked) {
                                notification.info({ title: t('debug.profilingNotice') })
                            }
                        }}
                    />
                    <span>{t('debug.profiling')}</span>
                </Space>
            </Tooltip>
        </Space>
    )
}

export default DebugToolbar
