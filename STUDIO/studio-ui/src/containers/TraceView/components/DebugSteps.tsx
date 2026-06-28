import React from 'react'
import { Tag, Tooltip, Empty } from 'antd'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
import type { StepValueView } from 'types/trace'
import { ParameterTree } from './TraceParameters'
import { useStyles } from './DebugSteps.styles'

interface DebugStepsProps {
    steps?: StepValueView[] | undefined
    frameUri: string
}

/**
 * Interactive list of a frame's steps (spreadsheet cells / fired rules).
 *
 * Each step has a breakpoint gutter — clicking it suspends execution when that step runs (key
 * {@code uri#ref}). Executed steps show their value, the current step is highlighted, and pending
 * steps are dimmed.
 */
const DebugSteps: React.FC<DebugStepsProps> = ({ steps, frameUri }) => {
    const { t } = useTranslation('trace')
    const { styles, cx } = useStyles()
    const breakpoints = useTraceStore(s => s.breakpoints)
    const toggleBreakpoint = useTraceStore(s => s.toggleBreakpoint)

    return (
        <div className={styles.section} data-testid="debug-steps">
            <div className={styles.header}>{t('debug.steps')}</div>
            {!steps || steps.length === 0 ? (
                <Empty description={t('debug.noSteps')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
            ) : (
                steps.map((step) => {
                    const key = `${frameUri}#${step.ref}`
                    const hasBreakpoint = breakpoints.includes(key)
                    const name = step.label || step.ref
                    return (
                        <div
                            key={step.ref}
                            className={cx(styles.step, step.status === 'current' && styles.current)}
                            data-testid={`debug-step-${step.ref}`}
                        >
                            {step.status === 'executed' ? (
                                // An executed step cannot be broken on anymore — no gutter, keep alignment.
                                <span className={styles.gutterPlaceholder} />
                            ) : (
                                <Tooltip title={hasBreakpoint ? t('debug.removeBreakpoint') : t('debug.addBreakpoint')}>
                                    <span
                                        className={cx(styles.gutter, hasBreakpoint && styles.gutterActive)}
                                        data-testid={`debug-step-bp-${step.ref}`}
                                        onClick={() => toggleBreakpoint(key, name)}
                                    />
                                </Tooltip>
                            )}
                            <div className={styles.body}>
                                {step.value ? (
                                    <ParameterTree param={{ ...step.value, name }} paramKey={`step-${step.ref}`} />
                                ) : (
                                    <span className={styles.name}>{name}</span>
                                )}
                                {step.status === 'current' && <Tag color="processing">{t('debug.executing')}</Tag>}
                                {step.status === 'pending' && <span className={styles.pending}>{t('debug.pending')}</span>}
                            </div>
                        </div>
                    )
                })
            )}
        </div>
    )
}

export default DebugSteps
