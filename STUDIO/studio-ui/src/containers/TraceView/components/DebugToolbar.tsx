import React from 'react'
import { Button, Divider, Space, Tooltip } from 'antd'
import {
    CaretRightOutlined,
    PauseOutlined,
    PoweroffOutlined,
    VerticalAlignBottomOutlined,
    VerticalAlignTopOutlined,
    EnterOutlined,
} from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
import { isTraceExecutionInProgress } from 'utils/traceExecutionStatus'

/**
 * Debugger control toolbar: resume, pause, step into/over/out, and stop.
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
    const terminate = useTraceStore(s => s.terminate)

    const suspended = status === 'SUSPENDED'
    const running = isTraceExecutionInProgress(status)
    const active = suspended || running

    return (
        <Space data-testid="debug-toolbar" size="small">
            <Tooltip title={t('debug.resume')}>
                <Button
                    data-testid="debug-resume"
                    disabled={!suspended || loading}
                    icon={<CaretRightOutlined />}
                    onClick={resume}
                    type="text"
                />
            </Tooltip>
            <Tooltip title={t('debug.pause')}>
                <Button
                    data-testid="debug-pause"
                    disabled={!running}
                    icon={<PauseOutlined />}
                    onClick={pause}
                    type="text"
                />
            </Tooltip>
            <Divider style={{ height: '1.2em', margin: 0 }} type="vertical" />
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
            <Divider style={{ height: '1.2em', margin: 0 }} type="vertical" />
            <Tooltip title={t('debug.stop')}>
                <Button
                    danger
                    data-testid="debug-stop"
                    disabled={!active}
                    icon={<PoweroffOutlined />}
                    onClick={terminate}
                    type="text"
                />
            </Tooltip>
        </Space>
    )
}

export default DebugToolbar
