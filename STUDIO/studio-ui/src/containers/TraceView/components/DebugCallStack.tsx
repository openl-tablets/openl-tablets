import React from 'react'
import { List, Tag, Empty, Button, Tooltip } from 'antd'
import { CloseOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
import { useStyles } from './DebugCallStack.styles'

/**
 * The live execution call stack (current frame first) and the list of breakpoints.
 *
 * Selecting a frame loads its variables and table. Breakpoints are set from the table view; this list
 * shows them and lets the user remove them.
 */
const DebugCallStack: React.FC = () => {
    const { t } = useTranslation('trace')
    const { styles, cx } = useStyles()
    const frames = useTraceStore(s => s.frames)
    const selectedFrameIndex = useTraceStore(s => s.selectedFrameIndex)
    const selectFrame = useTraceStore(s => s.selectFrame)
    const breakpoints = useTraceStore(s => s.breakpoints)
    const breakpointLabels = useTraceStore(s => s.breakpointLabels)
    const toggleBreakpoint = useTraceStore(s => s.toggleBreakpoint)

    // Current frame first.
    const ordered = [...frames].reverse()

    return (
        <div className={styles.panel} data-testid="debug-callstack">
            <div className={styles.header}>{t('debug.callStack')}</div>
            {frames.length === 0 ? (
                <Empty description={t('debug.notSuspended')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
            ) : (
                <List
                    dataSource={ordered}
                    size="small"
                    renderItem={(frame) => (
                        <List.Item
                            className={cx(styles.frame, frame.index === selectedFrameIndex && styles.frameSelected)}
                            data-testid={`debug-frame-${frame.index}`}
                            onClick={() => selectFrame(frame.index)}
                        >
                            <span className={styles.name}>{frame.name}</span>
                            <Tag color="default">{frame.kind}</Tag>
                            {frame.location?.label && (
                                <span className={styles.location}>{frame.location.label}</span>
                            )}
                            {frame.error && <Tag color="error">{t('severity.ERROR')}</Tag>}
                        </List.Item>
                    )}
                />
            )}
            <div className={styles.header}>{t('debug.breakpoints')}</div>
            {breakpoints.length === 0 ? (
                <div className={styles.hint}>{t('debug.noBreakpoints')}</div>
            ) : (
                <List
                    dataSource={breakpoints}
                    size="small"
                    renderItem={(uri) => (
                        <List.Item
                            className={styles.breakpoint}
                            data-testid="debug-breakpoint-item"
                            actions={[
                                <Tooltip key="remove" title={t('debug.removeBreakpoint')}>
                                    <Button
                                        icon={<CloseOutlined />}
                                        onClick={() => toggleBreakpoint(uri)}
                                        size="small"
                                        type="text"
                                    />
                                </Tooltip>,
                            ]}
                        >
                            <span className={styles.breakpointDot} />
                            <span className={styles.name}>{breakpointLabels[uri] || uri}</span>
                        </List.Item>
                    )}
                />
            )}
        </div>
    )
}

export default DebugCallStack
