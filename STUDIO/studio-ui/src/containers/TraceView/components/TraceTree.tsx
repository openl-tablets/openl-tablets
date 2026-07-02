import React, { useMemo, useRef, useState } from 'react'
import { Empty, Segmented, Tooltip } from 'antd'
import { BranchesOutlined, CaretDownOutlined, CaretRightOutlined, LinkOutlined, RedoOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
import type { CallNodeView, DebugFrameView, DispatchInfo, StepValueView } from 'types/trace'
import { useStyles } from './TraceTree.styles'

/** Format a millisecond duration compactly: `1.2 s`, `45 ms`, `3.4 ms`, `<1 ms`. */
const formatMs = (ms: number): string => {
    if (ms >= 1000) {
        return `${(ms / 1000).toFixed(1)} s`
    }
    if (ms >= 10) {
        return `${Math.round(ms)} ms`
    }
    if (ms > 0) {
        return `${ms.toFixed(1)} ms`
    }
    return '0 ms'
}

/** One row of the flattened tree: a live frame, a live step, or an executed-branch node/step. */
interface TreeRow {
    type: 'frame' | 'liveStep' | 'callNode' | 'callStep'
    key: string
    depth: number
    frame?: DebugFrameView
    frameIndex?: number
    step?: StepValueView
    node?: CallNodeView
    /** URI of the table a callStep belongs to, so it can be replayed to with a `uri#ref` breakpoint. */
    nodeUri?: string
    /** Set when the row has an executed sub-tree to expand. */
    expandKey?: string
    /** For a step-reference row, the key of the original step row it points at. */
    refTargetKey?: string
}

const hasChildren = (step: StepValueView): boolean => !!step.children && step.children.length > 0

/**
 * Flatten the live stack — plus any expanded executed branches — into indented rows. The live path is
 * always present and rebuilds as execution moves. Executed sub-calls (profiling mode) hang off the step
 * that made them and are collapsed by default; expanding one walks its retained structure, never values.
 */
const flatten = (frames: DebugFrameView[], tree: CallNodeView | null, expanded: Set<string>): TreeRow[] => {
    const rows: TreeRow[] = []

    // `refBase` is the key namespace of the branch the node hangs off, so a step-reference node can point
    // back at the original step row (`${refBase}/${refStep}`) of the frame or table that owns both.
    const walkNode = (node: CallNodeView, depth: number, path: string, refBase?: string): void => {
        rows.push({ type: 'callNode', key: path, depth, node,
            ...(node.refStep && refBase ? { refTargetKey: `${refBase}/${node.refStep}` } : {}) })
        for (const step of node.steps) {
            const stepKey = `${path}/${step.ref}`
            const open = hasChildren(step)
            rows.push({ type: 'callStep', key: stepKey, depth: depth + 1, step, nodeUri: node.uri,
                ...(open ? { expandKey: stepKey } : {}) })
            if (open && expanded.has(stepKey)) {
                step.children?.forEach((child, i) => walkNode(child, depth + 2, `${stepKey}#${i}`, path))
            }
        }
    }

    const walk = (i: number, depth: number): void => {
        const frame = frames[i]
        if (!frame) {
            return
        }
        rows.push({ type: 'frame', key: `f${i}`, depth, frameIndex: i, frame })
        let drilled = false
        for (const step of frame.steps ?? []) {
            const stepKey = `f${i}/${step.ref}`
            const open = hasChildren(step)
            rows.push({ type: 'liveStep', key: stepKey, depth: depth + 1, frameIndex: i, frame, step,
                ...(open ? { expandKey: stepKey } : {}) })
            if (open && expanded.has(stepKey)) {
                step.children?.forEach((child, idx) => walkNode(child, depth + 2, `${stepKey}#${idx}`, `f${i}`))
            }
            if (!drilled && step.status === 'current' && i + 1 < frames.length) {
                walk(i + 1, depth + 2)
                drilled = true
            }
        }
        // A frame without a matched current step still has its child on the stack — keep the path.
        if (!drilled && i + 1 < frames.length) {
            walk(i + 1, depth + 2)
        }
    }

    // The finished trace exposes its whole executed tree (the live stack is empty by then); otherwise the
    // live stack drives the view.
    if (tree) {
        walkNode(tree, 0, 'tree')
    } else if (frames.length > 0) {
        walk(0, 0)
    }
    return rows
}

const dotFor = (status: StepValueView['status']): 'dotExecuted' | 'dotCurrent' | 'dot' =>
    status === 'executed' ? 'dotExecuted' : status === 'current' ? 'dotCurrent' : 'dot'

/**
 * Simple-mode view of a trace: the live call stack as a mutating tree, with executed branches retained
 * (profiling mode) as collapsible sub-trees. The current line of each frame expands into the called
 * table; already-executed lines are inactive (click to read their result); not-yet-reached lines run
 * execution here on click. The live path is always shown; returned branches collapse and expand on demand.
 */
const TraceTree: React.FC = () => {
    const { t } = useTranslation('trace')
    const { styles, cx } = useStyles()
    const frames = useTraceStore(s => s.frames)
    const tree = useTraceStore(s => s.tree)
    const selectedFrameIndex = useTraceStore(s => s.selectedFrameIndex)
    const selectFrame = useTraceStore(s => s.selectFrame)
    const runTo = useTraceStore(s => s.runTo)
    const replayNode = useTraceStore(s => s.replayNode)
    const status = useTraceStore(s => s.status)
    const [expanded, setExpanded] = useState<Set<string>>(new Set())
    const [timeMode, setTimeMode] = useState<'total' | 'self'>('total')
    const [flashKey, setFlashKey] = useState<string | null>(null)
    const treeRef = useRef<HTMLDivElement>(null)
    const rows = useMemo(() => flatten(frames, tree, expanded), [frames, tree, expanded])

    // Scroll the referenced original step into view and flash it, so the eye lands on it.
    const jumpToRow = (key: string): void => {
        treeRef.current?.querySelector(`[data-rowkey="${CSS.escape(key)}"]`)
            ?.scrollIntoView({ block: 'center', behavior: 'smooth' })
        setFlashKey(key)
        window.setTimeout(() => setFlashKey(prev => (prev === key ? null : prev)), 1600)
    }

    if (frames.length === 0 && !tree) {
        return <Empty description={t('debug.notSuspended')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
    }

    const canRunTo = status === 'SUSPENDED'
    const indent = (depth: number): React.CSSProperties => ({ paddingLeft: 8 + depth * 14 })
    // Timing of a row that has one: an executed call-tree node, or a frame that has already returned
    // (for example the root after a step out). In-progress frames have no timing yet.
    const timingOf = (row: TreeRow): number | null => {
        if (row.node) {
            return timeMode === 'self' ? row.node.selfMillis : row.node.durationMillis
        }
        if (row.step?.durationMillis != null) {
            return timeMode === 'self' ? (row.step.selfMillis ?? row.step.durationMillis) : row.step.durationMillis
        }
        // Only a frame row shows its frame's total; a step with no measured time of its own stays blank
        // rather than inheriting the frame's duration.
        const frame = row.frame
        if (!row.step && frame?.completed && frame.durationMillis != null) {
            return timeMode === 'self' ? (frame.selfMillis ?? frame.durationMillis) : frame.durationMillis
        }
        return null
    }
    const hasTimings = rows.some(row => timingOf(row) != null)
    // The slowest timing (by the chosen metric) sets the scale for the heatmap.
    const maxDuration = rows.reduce((max, row) => {
        const ms = timingOf(row)
        return ms != null ? Math.max(max, ms) : max
    }, 0)
    // Render a timing as a length-based heat bar plus the value, so relative cost reads pre-attentively by
    // bar length and the status colours stay free to mean only execution state.
    const durationCell = (ms: number): React.ReactNode => (
        <span className={styles.duration}>
            <span className={styles.durationBar}>
                <span
                    className={styles.durationFill}
                    style={{ width: `${maxDuration > 0 && ms > 0 ? Math.max(4, (ms / maxDuration) * 100) : 0}%` }}
                />
            </span>
            <span className={styles.durationValue}>{formatMs(ms)}</span>
        </span>
    )

    const toggle = (key: string): void => setExpanded(prev => {
        const next = new Set(prev)
        if (!next.delete(key)) {
            next.add(key)
        }
        return next
    })

    const twisty = (expandKey?: string): React.ReactNode => {
        if (!expandKey) {
            return <span className={styles.chevronSlot} />
        }
        return (
            <span
                className={styles.chevron}
                data-testid={`tree-toggle-${expandKey}`}
                onClick={(e) => { e.stopPropagation(); toggle(expandKey) }}
            >
                {expanded.has(expandKey) ? <CaretDownOutlined /> : <CaretRightOutlined />}
            </span>
        )
    }

    const replayButton = (key: string, label: string, testId: string, hint: string): React.ReactNode => (
        <Tooltip title={hint}>
            <RedoOutlined
                className={styles.replay}
                data-testid={testId}
                onClick={(e) => { e.stopPropagation(); void replayNode(key, label) }}
            />
        </Tooltip>
    )

    // A dispatched table (versioned by dimension properties) is shown in place, badged with the versions it
    // was chosen from — the chosen one flagged — rather than as an extra dispatcher node in the tree.
    const dispatchBadge = (dispatch?: DispatchInfo | null): React.ReactNode => {
        if (!dispatch || dispatch.candidates.length === 0) {
            return null
        }
        const tip = (
            <div>
                <div className={styles.dispatchTipTitle}>
                    {t('tree.dispatchTitle', { count: dispatch.candidates.length })}
                </div>
                {dispatch.candidates.map((candidate, i) => (
                    <div
                        key={`${i}-${candidate.label}`}
                        className={cx(styles.dispatchCandidate, candidate.chosen && styles.dispatchChosen)}
                    >
                        {candidate.label}
                    </div>
                ))}
            </div>
        )
        return (
            <Tooltip title={tip}>
                <span className={styles.dispatchTag} data-testid="tree-dispatch">
                    <BranchesOutlined />
                    {dispatch.candidates.length}
                </span>
            </Tooltip>
        )
    }

    const renderFrame = (row: TreeRow): React.ReactNode => {
        const frame = row.frame as DebugFrameView
        const ms = timingOf(row)
        return (
            <div
                key={row.key}
                data-testid={`tree-frame-${row.frameIndex}`}
                onClick={() => selectFrame(row.frameIndex as number)}
                style={indent(row.depth)}
                className={cx(styles.row, styles.frame, frame.active && styles.current,
                    row.frameIndex === selectedFrameIndex && styles.selected)}
            >
                <span className={styles.chevronSlot} />
                <span
                    className={cx(styles.dot, frame.error
                        ? styles.dotError
                        : frame.completed ? styles.dotExecuted : styles.dotFrame)}
                />
                <span className={styles.name}>{frame.name}</span>
                <span className={styles.kind}>{frame.kind}</span>
                {dispatchBadge(frame.dispatch)}
                {ms != null && durationCell(ms)}
                {frame.completed && replayButton(frame.uri, frame.name, `tree-replay-${frame.uri}`, t('tree.replayHint'))}
            </div>
        )
    }

    const renderLiveStep = (row: TreeRow): React.ReactNode => {
        const step = row.step as StepValueView
        const frame = row.frame as DebugFrameView
        const ms = timingOf(row)
        const runnable = canRunTo && step.status === 'pending'
        const onClick = runnable
            ? () => runTo(`${frame.uri}#${step.ref}`, `${frame.name}: ${step.label || step.ref}`)
            : () => selectFrame(row.frameIndex as number)
        const tooltip = runnable
            ? t('tree.runToHint')
            : step.status === 'executed' ? t('tree.resultHint') : undefined
        return (
            <Tooltip key={row.key} title={tooltip}>
                <div
                    data-testid={`tree-step-${row.frameIndex}-${step.ref}`}
                    data-rowkey={row.key}
                    onClick={onClick}
                    style={indent(row.depth)}
                    className={cx(styles.row,
                        runnable && styles.runnable,
                        step.status === 'executed' && styles.inactive,
                        step.status === 'current' && styles.currentStep,
                        step.status === 'pending' && styles.pending,
                        flashKey === row.key && styles.flashed)}
                >
                    {twisty(row.expandKey)}
                    <span className={cx(styles.dot, styles[dotFor(step.status)])} />
                    <span className={styles.leafLabel}>{step.label || step.ref}</span>
                    {ms != null && durationCell(ms)}
                    {step.status === 'executed' && replayButton(`${frame.uri}#${step.ref}`,
                        step.label || step.ref, `tree-replay-${frame.uri}#${step.ref}`, t('tree.replayStepHint'))}
                </div>
            </Tooltip>
        )
    }

    const renderCallNode = (row: TreeRow): React.ReactNode => {
        const node = row.node as CallNodeView
        // A step reference is not an execution of its own: the formula used a step computed elsewhere in
        // the same table. It is marked as a link, never duplicated, and a click jumps to the original row.
        if (node.kind === 'stepRef') {
            return (
                <Tooltip key={row.key} title={t('tree.referenceHint')}>
                    <div
                        data-testid={`tree-ref-${row.key}`}
                        className={cx(styles.row, styles.inactive, row.refTargetKey && styles.runnable)}
                        style={indent(row.depth)}
                        onClick={row.refTargetKey ? () => jumpToRow(row.refTargetKey as string) : undefined}
                    >
                        <span className={styles.chevronSlot} />
                        <LinkOutlined className={styles.refIcon} />
                        <span className={styles.leafLabel}>{node.name}</span>
                        <span className={styles.kind}>{t('tree.referenceTag')}</span>
                    </div>
                </Tooltip>
            )
        }
        const ms = timingOf(row) ?? 0
        return (
            <div
                key={row.key}
                className={cx(styles.row, styles.frame, styles.callNode)}
                style={indent(row.depth)}
            >
                <span className={styles.chevronSlot} />
                <span className={cx(styles.dot, styles.dotExecuted)} />
                <span className={styles.name}>{node.name}</span>
                <span className={styles.kind}>{node.kind}</span>
                {dispatchBadge(node.dispatch)}
                {durationCell(ms)}
                {replayButton(node.uri, node.name, `tree-replay-${node.uri}`, t('tree.replayHint'))}
            </div>
        )
    }

    const renderCallStep = (row: TreeRow): React.ReactNode => {
        const step = row.step as StepValueView
        const ms = timingOf(row)
        const replayKey = `${row.nodeUri}#${step.ref}`
        return (
            <div
                key={row.key}
                data-rowkey={row.key}
                className={cx(styles.row, styles.inactive, row.expandKey && styles.runnable,
                    flashKey === row.key && styles.flashed)}
                onClick={row.expandKey ? () => toggle(row.expandKey as string) : undefined}
                style={indent(row.depth)}
            >
                {twisty(row.expandKey)}
                <span className={cx(styles.dot, styles.dotExecuted)} />
                <span className={styles.leafLabel}>{step.label || step.ref}</span>
                {ms != null && durationCell(ms)}
                {row.nodeUri && replayButton(replayKey, step.label || step.ref,
                    `tree-replay-${replayKey}`, t('tree.replayStepHint'))}
            </div>
        )
    }

    const render = (row: TreeRow): React.ReactNode => {
        switch (row.type) {
            case 'frame': return renderFrame(row)
            case 'liveStep': return renderLiveStep(row)
            case 'callNode': return renderCallNode(row)
            default: return renderCallStep(row)
        }
    }

    return (
        <div ref={treeRef} className={styles.tree} data-testid="trace-tree">
            <div className={styles.header}>
                <span>{t('tree.title')}</span>
                {hasTimings && (
                    <Segmented
                        className={styles.timeToggle}
                        data-testid="trace-time-mode"
                        onChange={(value) => setTimeMode(value as 'total' | 'self')}
                        size="small"
                        value={timeMode}
                        options={[
                            { label: t('tree.timeTotal'), value: 'total' },
                            { label: t('tree.timeSelf'), value: 'self' },
                        ]}
                    />
                )}
            </div>
            {rows.map(render)}
        </div>
    )
}

export default TraceTree
