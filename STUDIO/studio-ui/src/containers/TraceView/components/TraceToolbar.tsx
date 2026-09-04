import React, { useState } from 'react'
import { Button, notification, Space, Switch, Tag, Tooltip } from 'antd'
import { SettingOutlined, SyncOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
import type { DebugStatus } from 'types/trace'
import DebugToolbar from './DebugToolbar'
import { useStyles } from './TraceToolbar.styles'

const STATUS_STYLE = {
    pending: 'statusNeutral',
    running: 'statusRunning',
    suspended: 'statusPaused',
    completed: 'statusFinished',
    error: 'statusFailed',
    terminated: 'statusNeutral',
} as const satisfies Record<DebugStatus, string>

/**
 * The trace toolbar, in the left column of both modes and always visible so its controls stay reachable.
 *
 * The business view shows only its **Show detailed view** toggle. The advanced view shows the debugger
 * buttons and the run status, with a gear pinned to the right that swaps them for the run settings —
 * **Profiling** and **Show detailed view**.
 */
const TraceToolbar: React.FC = () => {
    const { t } = useTranslation('trace')
    const { styles, cx } = useStyles()
    const advanced = useTraceStore(s => s.advanced)
    const showDetailed = useTraceStore(s => s.showDetailed)
    const setShowDetailed = useTraceStore(s => s.setShowDetailed)
    const profiling = useTraceStore(s => s.profiling)
    const setProfiling = useTraceStore(s => s.setProfiling)
    const loading = useTraceStore(s => s.loading)
    const status = useTraceStore(s => s.status)
    const [settingsOpen, setSettingsOpen] = useState(false)

    const detailedToggle = (
        <Space size={4}>
            <Switch checked={showDetailed} data-testid="trace-detailed" onChange={setShowDetailed} size="small" />
            <span>{t('tree.showDetailed')}</span>
        </Space>
    )

    // The business view's only control.
    if (!advanced) {
        return (
            <div className={styles.toolbar} data-testid="debug-header">
                {detailedToggle}
            </div>
        )
    }

    const settings = (
        <Space size="middle">
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
            {/* The detailed breakdown lives in the executed tree, which the debugger keeps only while profiling. */}
            {profiling && detailedToggle}
        </Space>
    )

    const controls = (
        <Space size="small">
            <DebugToolbar />
            {status && (
                <Tag
                    className={cx(styles.statusTag, styles[STATUS_STYLE[status]])}
                    data-testid="debug-status"
                    icon={status === 'running' ? <SyncOutlined spin /> : undefined}
                >
                    {t(`debug.status.${status}`)}
                </Tag>
            )}
        </Space>
    )

    return (
        <div className={styles.toolbar} data-testid="debug-header">
            <div className={styles.main}>{settingsOpen ? settings : controls}</div>
            <Tooltip title={t('debug.settings')}>
                <Button
                    className={styles.gear}
                    data-testid="trace-settings"
                    icon={<SettingOutlined />}
                    onClick={() => setSettingsOpen(open => !open)}
                    type="text"
                />
            </Tooltip>
        </div>
    )
}

export default TraceToolbar
