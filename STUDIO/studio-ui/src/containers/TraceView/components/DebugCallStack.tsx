import React from 'react'
import { Tag, Empty } from 'antd'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
import { useStyles } from './DebugCallStack.styles'

/**
 * The live execution call stack (current frame first). Selecting a frame loads its variables and table.
 * Breakpoints are managed in the shared breakpoints panel above this view.
 */
const DebugCallStack: React.FC = () => {
    const { t } = useTranslation('trace')
    const { styles, cx } = useStyles()
    const frames = useTraceStore(s => s.frames)
    const selectedFrameIndex = useTraceStore(s => s.selectedFrameIndex)
    const selectFrame = useTraceStore(s => s.selectFrame)

    if (frames.length === 0) {
        return <Empty description={t('debug.notSuspended')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
    }

    // Current frame first.
    const ordered = [...frames].reverse()

    return (
        <div className={styles.panel} data-testid="debug-callstack">
            <div className={styles.header}>{t('debug.callStack')}</div>
            {ordered.map((frame) => (
                <div
                    key={frame.index}
                    aria-current={frame.active ? 'true' : undefined}
                    data-testid={`debug-frame-${frame.index}`}
                    onClick={() => selectFrame(frame.index)}
                    className={cx(
                        styles.frame,
                        frame.active && styles.frameCurrent,
                        frame.index === selectedFrameIndex && styles.frameSelected
                    )}
                >
                    <span className={styles.name}>{frame.name}</span>
                    <Tag color="default">{frame.kind}</Tag>
                    {frame.location?.label && (
                        <span className={styles.location}>{frame.location.label}</span>
                    )}
                    {frame.error && <Tag color="error">{t('severity.ERROR')}</Tag>}
                </div>
            ))}
        </div>
    )
}

export default DebugCallStack
