import React from 'react'
import { Empty, Tag, Tooltip } from 'antd'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
import type { DebugFrameView, StepValueView } from 'types/trace'
import { useStyles } from './TraceTree.styles'

/** One row of the flattened call tree: a frame header (no step) or one of that frame's steps. */
interface TreeRow {
    key: string
    depth: number
    frameIndex: number
    frame: DebugFrameView
    step?: StepValueView
}

/**
 * Flatten the live stack into an indented tree. Each frame contributes its header and its steps
 * (spreadsheet cells or decision-table rules); the *current* step of a frame drills into the child
 * frame, so only the active path expands. Executed and pending steps are leaves — a returned (popped)
 * sub-table is never re-drawn, which keeps the tree bounded by the live stack and never re-runs anything.
 */
const flatten = (frames: DebugFrameView[]): TreeRow[] => {
    const rows: TreeRow[] = []
    const walk = (i: number, depth: number): void => {
        const frame = frames[i]
        if (!frame) {
            return
        }
        rows.push({ key: `f${i}`, depth, frameIndex: i, frame })
        let drilled = false
        for (const step of frame.steps ?? []) {
            rows.push({ key: `f${i}-${step.ref}`, depth: depth + 1, frameIndex: i, frame, step })
            if (!drilled && step.status === 'current' && i + 1 < frames.length) {
                walk(i + 1, depth + 2)
                drilled = true
            }
        }
        // A frame without a matched current step (e.g. a decision table firing into a sub-table) still has
        // its child on the stack — drop it in so the active path is never lost.
        if (!drilled && i + 1 < frames.length) {
            walk(i + 1, depth + 2)
        }
    }
    if (frames.length > 0) {
        walk(0, 0)
    }
    return rows
}

const dotFor = (status: StepValueView['status']): 'dotExecuted' | 'dotCurrent' | 'dot' =>
    status === 'executed' ? 'dotExecuted' : status === 'current' ? 'dotCurrent' : 'dot'

/**
 * Simple-mode view of a trace: the live call stack as a mutating tree. It grows as execution descends
 * and rebuilds on every step. The current line of each frame expands into the called table; already
 * executed lines are inactive (click to read their result); not-yet-reached lines run execution here on
 * click. Returned sub-tables collapse to their result, so a business user navigates by clicking rather
 * than by stepping in and out.
 */
const TraceTree: React.FC = () => {
    const { t } = useTranslation('trace')
    const { styles, cx } = useStyles()
    const frames = useTraceStore(s => s.frames)
    const selectedFrameIndex = useTraceStore(s => s.selectedFrameIndex)
    const selectFrame = useTraceStore(s => s.selectFrame)
    const runTo = useTraceStore(s => s.runTo)
    const status = useTraceStore(s => s.status)

    if (frames.length === 0) {
        return <Empty description={t('debug.notSuspended')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
    }

    const canRunTo = status === 'SUSPENDED'
    const indent = (depth: number): React.CSSProperties => ({ paddingLeft: 8 + depth * 14 })

    const renderFrame = (row: TreeRow): React.ReactNode => {
        const frame = row.frame
        return (
            <div
                key={row.key}
                data-testid={`tree-frame-${row.frameIndex}`}
                onClick={() => selectFrame(row.frameIndex)}
                style={indent(row.depth)}
                className={cx(styles.row, styles.frame, frame.active && styles.current,
                    row.frameIndex === selectedFrameIndex && styles.selected)}
            >
                <span
                    className={cx(styles.dot, frame.error
                        ? styles.dotError
                        : frame.completed ? styles.dotExecuted : styles.dotFrame)}
                />
                <span className={styles.name}>{frame.name}</span>
                <Tag color="default">{frame.kind}</Tag>
            </div>
        )
    }

    const renderStep = (row: TreeRow): React.ReactNode => {
        const step = row.step as StepValueView
        const frame = row.frame
        const runnable = canRunTo && step.status === 'pending'
        const onClick = runnable
            ? () => runTo(`${frame.uri}#${step.ref}`, `${frame.name}: ${step.label || step.ref}`)
            : () => selectFrame(row.frameIndex)
        const tooltip = runnable
            ? t('tree.runToHint')
            : step.status === 'executed' ? t('tree.resultHint') : undefined
        return (
            <Tooltip key={row.key} title={tooltip}>
                <div
                    data-testid={`tree-step-${row.frameIndex}-${step.ref}`}
                    onClick={onClick}
                    style={indent(row.depth)}
                    className={cx(styles.row,
                        runnable && styles.runnable,
                        step.status === 'executed' && styles.inactive,
                        step.status === 'current' && styles.currentStep,
                        step.status === 'pending' && styles.pending)}
                >
                    <span className={cx(styles.dot, styles[dotFor(step.status)])} />
                    <span className={styles.leafLabel}>{step.label || step.ref}</span>
                </div>
            </Tooltip>
        )
    }

    return (
        <div className={styles.tree} data-testid="trace-tree">
            <div className={styles.header}>{t('tree.title')}</div>
            {flatten(frames).map(row => row.step ? renderStep(row) : renderFrame(row))}
        </div>
    )
}

export default TraceTree
