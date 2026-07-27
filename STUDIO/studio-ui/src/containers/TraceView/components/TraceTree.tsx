import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { Button, Empty, Segmented, Spin, Tooltip } from 'antd'
import {
    CaretDownOutlined,
    CaretRightOutlined,
    CloseCircleFilled,
    DoubleRightOutlined,
    LinkOutlined,
    RedoOutlined,
} from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { treeChildKey, useTraceStore } from 'store'
import type { CallNodeView, DebugFrameView, FrameKind, StepValueView } from 'types/trace'
import { formatMs } from 'utils/formatDuration'
import DispatchBadge from './DispatchBadge'
import { onActivate } from './keyboardActivate'
import { kindIcon, stepIcon } from './TraceIcons'
import { useFlashJump } from './useFlashJump'
import { useStyles } from './TraceTree.styles'

/** One row of the flattened tree: a live frame, a live step, an executed-branch node/step, or a "more" marker. */
interface TreeRow {
    type: 'frame' | 'liveStep' | 'callNode' | 'callStep' | 'more' | 'loading' | 'notRetained'
    key: string
    depth: number
    frame?: DebugFrameView
    frameIndex?: number
    step?: StepValueView
    node?: CallNodeView
    /** URI of the table a callStep belongs to, so it can be replayed to with a `uri#ref@instance` breakpoint. */
    nodeUri?: string
    /** Execution index of the table a callStep belongs to, so replay targets that exact iteration. */
    nodeInstance?: number
    /** Kind of the table a callStep belongs to, so the step shows that kind's icon. */
    nodeKind?: FrameKind
    /** Set when the row has an executed sub-tree to expand. */
    expandKey?: string
    /** For a step-reference row, the key of the original step row it points at. */
    refTargetKey?: string
    /** For a "more" row, how many further executions were not rendered. */
    moreCount?: number
}

const hasChildren = (step: StepValueView): boolean =>
    (step.children?.length ?? 0) > 0 || (step.childrenTotal ?? 0) > 0

/**
 * Flatten the live stack — plus any expanded executed branches — into indented rows. The live path is
 * always present and rebuilds as execution moves. Executed sub-calls (profiling mode) hang off the step
 * that made them and are collapsed by default; expanding one walks its retained structure, never values.
 */
const flatten = (
    frames: DebugFrameView[],
    tree: CallNodeView | null,
    expanded: Set<string>,
    treeChildren: Record<string, CallNodeView[]>,
    treeLoading: Record<string, boolean>
): TreeRow[] => {
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
                nodeInstance: node.instance, nodeKind: node.kind, ...(open ? { expandKey: stepKey } : {}) })
            if (open && expanded.has(stepKey)) {
                walkChildren(node.uri, node.instance, step, depth + 2, stepKey, path)
            }
        }
        // A branch that outgrew the tree's size limit honestly reports how many of its sub-calls were dropped,
        // so an analyst reading Hot Spots knows this node's children are incomplete rather than absent.
        if ((node.notRetained ?? 0) > 0) {
            rows.push({ type: 'notRetained', key: `${path}/notRetained`, depth: depth + 1, moreCount: node.notRetained ?? 0 })
        }
    }

    // A live frame carries its sub-calls inline (capped for display); the completed tree fetches them lazily
    // by (uri, instance, step) and pages them, so expanding a hot branch never floods the tree or the response.
    const walkChildren = (nodeUri: string, nodeInstance: number, step: StepValueView,
        depth: number, keyBase: string, refBase: string): void => {
        if (step.children) {
            // The server already caps inline children and reports the full count in childrenTotal, so the list
            // is rendered as-is and the "+N more" reflects what the server omitted.
            step.children.forEach((child, i) => walkNode(child, depth, `${keyBase}#${i}`, refBase))
            const total = step.childrenTotal ?? step.children.length
            if (total > step.children.length) {
                rows.push({ type: 'more', key: `${keyBase}/more`, depth, moreCount: total - step.children.length })
            }
            return
        }
        const key = treeChildKey(nodeUri, nodeInstance, step.ref)
        const loaded = treeChildren[key]
        if (loaded) {
            loaded.forEach((child, i) => walkNode(child, depth, `${keyBase}#${i}`, refBase))
            const total = step.childrenTotal ?? loaded.length
            if (total > loaded.length) {
                rows.push({ type: 'more', key: `${keyBase}/more`, depth, moreCount: total - loaded.length,
                    nodeUri, nodeInstance, step })
            }
        }
        if (treeLoading[key]) {
            rows.push({ type: 'loading', key: `${keyBase}/loading`, depth })
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
                walkChildren(frame.uri, frame.instance, step, depth + 2, stepKey, `f${i}`)
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

type TimeMode = 'total' | 'self'

// Timing of a row that has one: an executed call-tree node, or a frame that has already returned
// (for example the root after a step out). In-progress frames have no timing yet.
const rowTiming = (row: TreeRow, timeMode: TimeMode): number | null => {
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

/**
 * Simple-mode view of a trace: the live call stack as a mutating tree, with executed branches retained
 * (profiling mode) as collapsible sub-trees. The current line of each frame expands into the called
 * table; already-executed lines read as plain text (click to read their result); not-yet-reached lines run
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
    const truncated = useTraceStore(s => s.profile?.truncated ?? false)
    const treeChildren = useTraceStore(s => s.treeChildren)
    const treeLoading = useTraceStore(s => s.treeLoading)
    const fetchTreeChildren = useTraceStore(s => s.fetchTreeChildren)
    const runId = useTraceStore(s => s.runId)
    const profiling = useTraceStore(s => s.profiling)
    const [expanded, setExpanded] = useState<Set<string>>(new Set())
    const [timeMode, setTimeMode] = useState<TimeMode>('total')

    // Row keys are positional, so a new run (replay/rerun) would inherit the previous run's expansions:
    // matching rows silently re-open, and rows without loaded children show an open chevron over a
    // collapsed branch that then needs two clicks to expand. A fresh run starts fully collapsed.
    useEffect(() => {
        setExpanded(new Set())
    }, [runId])
    const { treeRef, flashKey, jumpToRow } = useFlashJump()
    const rows = useMemo(() => flatten(frames, tree, expanded, treeChildren, treeLoading),
        [frames, tree, expanded, treeChildren, treeLoading])
    // The single gate on timings: they are a profiling concern, so without profiling no durations
    // (or the Total/Self toggle) are shown, even where the backend reports them.
    const timingOf = useCallback(
        (row: TreeRow): number | null => (profiling ? rowTiming(row, timeMode) : null),
        [profiling, timeMode]
    )
    // One pass over the rows for the heatmap: whether any row is timed, and the slowest timing (by the
    // chosen metric) that sets the bar scale. Recomputed only when the rows or the gate change.
    const { hasTimings, maxDuration } = useMemo(() => {
        let max = 0
        let anyTimed = false
        for (const row of rows) {
            const ms = timingOf(row)
            if (ms != null) {
                anyTimed = true
                max = Math.max(max, ms)
            }
        }
        return { hasTimings: anyTimed, maxDuration: max }
    }, [rows, timingOf])

    if (frames.length === 0 && !tree) {
        return <Empty description={t('debug.notSuspended')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
    }

    const canRunTo = status === 'suspended'
    const indent = (depth: number): React.CSSProperties => ({ paddingLeft: 8 + depth * 14 })
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

    // Toggle a step and, when opening a lazily-loaded tree step, fetch its first page of sub-calls.
    const onToggle = (key: string): void => {
        const willOpen = !expanded.has(key)
        toggle(key)
        if (!willOpen) {
            return
        }
        const row = rows.find(r => r.expandKey === key)
        // Fetch a lazy tree step's first page only once — re-expanding a loaded branch reuses the cache; the
        // "+N more" row is what pages in the rest.
        if (row?.type === 'callStep' && row.step && !row.step.children && row.nodeUri !== undefined
            && !treeChildren[treeChildKey(row.nodeUri, row.nodeInstance ?? 0, row.step.ref)]) {
            void fetchTreeChildren(row.nodeUri, row.nodeInstance ?? 0, row.step.ref)
        }
    }

    const twisty = (expandKey?: string): React.ReactNode => {
        if (!expandKey) {
            return <span className={styles.chevronSlot} />
        }
        return (
            <span
                aria-expanded={expanded.has(expandKey)}
                className={styles.chevron}
                data-testid={`tree-toggle-${expandKey}`}
                onClick={(e) => { e.stopPropagation(); onToggle(expandKey) }}
                role="button"
                tabIndex={0}
                onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault()
                        e.stopPropagation()
                        onToggle(expandKey)
                    }
                }}
            >
                {expanded.has(expandKey) ? <CaretDownOutlined /> : <CaretRightOutlined />}
            </span>
        )
    }

    // Plain, self-explanatory marks instead of coloured dots: a » points along the call path the
    // calculation is on right now (bright on the current line, muted on the callers waiting above it),
    // a red cross flags a failure. Executed lines read as plain text; not-yet-reached lines are greyed.
    // A double chevron, so the mark cannot be mistaken for the single expand/collapse triangle.
    const stepMark = (status: StepValueView['status']): React.ReactNode =>
        (status === 'current'
            ? <DoubleRightOutlined className={cx(styles.mark, styles.markCurrent)} />
            : <span className={styles.mark} />)

    const frameMark = (frame: DebugFrameView): React.ReactNode => {
        if (frame.error) {
            return <CloseCircleFilled className={cx(styles.mark, styles.markError)} />
        }
        if (frame.completed) {
            return <span className={styles.mark} />
        }
        return <DoubleRightOutlined className={cx(styles.mark, frame.active ? styles.markFrame : styles.markWaiting)} />
    }

    const replayButton = (key: string, label: string, testId: string, hint: string): React.ReactNode => (
        <Tooltip title={hint}>
            <Button
                className={styles.replay}
                data-testid={testId}
                icon={<RedoOutlined />}
                onClick={(e) => { e.stopPropagation(); void replayNode(key, label) }}
                size="small"
                type="text"
            />
        </Tooltip>
    )

    const renderFrame = (row: TreeRow): React.ReactNode => {
        const frame = row.frame as DebugFrameView
        const ms = timingOf(row)
        const selectThisFrame = () => selectFrame(row.frameIndex as number)
        return (
            <div
                key={row.key}
                data-testid={`tree-frame-${row.frameIndex}`}
                onClick={selectThisFrame}
                onKeyDown={onActivate(selectThisFrame)}
                role="button"
                style={indent(row.depth)}
                tabIndex={0}
                className={cx(styles.row, styles.frame, frame.active && styles.current,
                    row.frameIndex === selectedFrameIndex && styles.selected)}
            >
                <span className={styles.chevronSlot} />
                {frameMark(frame)}
                {kindIcon(frame.kind)}
                <span className={styles.name}>{frame.name}</span>
                {frame.instance > 0 && (
                    <Tooltip title={t('tree.passHint', { n: frame.instance + 1 })}>
                        <span className={styles.pass} data-testid={`tree-pass-${row.frameIndex}`}>
                            #{frame.instance + 1}
                        </span>
                    </Tooltip>
                )}
                <span className={styles.kind}>{frame.kind}</span>
                <DispatchBadge dispatch={frame.dispatch} />
                {ms != null && durationCell(ms)}
                {frame.completed && replayButton(`${frame.uri}@${frame.instance}`, frame.name,
                    `tree-replay-${frame.uri}`, t('tree.replayHint'))}
            </div>
        )
    }

    // A runnable (not-yet-reached) step is hinted to run-to; an executed one to read its result; the
    // current line has no hint of its own.
    const liveStepTooltip = (runnable: boolean, status: StepValueView['status']): string | undefined => {
        if (runnable) {
            return t('tree.runToHint')
        }
        return status === 'executed' ? t('tree.resultHint') : undefined
    }

    const renderLiveStep = (row: TreeRow): React.ReactNode => {
        const step = row.step as StepValueView
        const frame = row.frame as DebugFrameView
        const ms = timingOf(row)
        const runnable = canRunTo && step.status === 'pending'
        const onClick = runnable
            ? () => runTo(`${frame.uri}#${step.ref}`, `${frame.name}: ${step.label || step.ref}`)
            : () => selectFrame(row.frameIndex as number)
        const tooltip = liveStepTooltip(runnable, step.status)
        return (
            <Tooltip key={row.key} title={tooltip}>
                <div
                    data-rowkey={row.key}
                    data-testid={`tree-step-${row.frameIndex}-${step.ref}`}
                    onClick={onClick}
                    onKeyDown={onActivate(onClick)}
                    role="button"
                    style={indent(row.depth)}
                    tabIndex={0}
                    className={cx(styles.row,
                        runnable && styles.runnable,
                        step.status === 'current' && styles.currentStep,
                        step.status === 'pending' && styles.pending,
                        flashKey === row.key && styles.flashed)}
                >
                    {twisty(row.expandKey)}
                    {stepMark(step.status)}
                    {/* A not-yet-executed step stays bare — an icon on it would suggest it already ran. */}
                    {step.status === 'pending'
                        ? <span className={styles.mark} />
                        : stepIcon(frame.kind)}
                    <span className={styles.leafLabel}>{step.label || step.ref}</span>
                    {ms != null && durationCell(ms)}
                    {step.status === 'executed' && replayButton(`${frame.uri}#${step.ref}@${frame.instance}`,
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
            const jump = row.refTargetKey ? () => jumpToRow(row.refTargetKey as string) : undefined
            return (
                <Tooltip key={row.key} title={t('tree.referenceHint')}>
                    <div
                        className={cx(styles.row, styles.inactive, row.refTargetKey && styles.runnable)}
                        data-testid={`tree-ref-${row.key}`}
                        style={indent(row.depth)}
                        {...(jump && { onClick: jump, onKeyDown: onActivate(jump), role: 'button', tabIndex: 0 })}
                    >
                        <span className={styles.chevronSlot} />
                        <LinkOutlined className={styles.refIcon} />
                        <span className={styles.leafLabel}>{node.name}</span>
                        <span className={styles.kind}>{t('tree.referenceTag')}</span>
                    </div>
                </Tooltip>
            )
        }
        const ms = timingOf(row)
        return (
            <div
                key={row.key}
                className={cx(styles.row, styles.frame, styles.callNode)}
                style={indent(row.depth)}
            >
                <span className={styles.chevronSlot} />
                {/* An empty mark slot, matching the live frame's, so executed nodes line up with live rows. */}
                <span className={styles.mark} />
                {kindIcon(node.kind)}
                <span className={styles.name}>{node.name}</span>
                <span className={styles.kind}>{node.kind}</span>
                <DispatchBadge dispatch={node.dispatch} />
                {ms != null && durationCell(ms)}
                {replayButton(`${node.uri}@${node.instance}`, node.name, `tree-replay-${node.uri}`, t('tree.replayHint'))}
            </div>
        )
    }

    const renderCallStep = (row: TreeRow): React.ReactNode => {
        const step = row.step as StepValueView
        const ms = timingOf(row)
        const replayKey = `${row.nodeUri}#${step.ref}@${row.nodeInstance}`
        const expand = row.expandKey ? () => onToggle(row.expandKey as string) : undefined
        return (
            <div
                key={row.key}
                data-rowkey={row.key}
                style={indent(row.depth)}
                className={cx(styles.row, row.expandKey && styles.runnable,
                    flashKey === row.key && styles.flashed)}
                {...(expand && { onClick: expand, onKeyDown: onActivate(expand), role: 'button', tabIndex: 0 })}
            >
                {twisty(row.expandKey)}
                {/* An empty mark slot, matching the live step's, so executed steps line up with live rows. */}
                <span className={styles.mark} />
                {stepIcon(row.nodeKind)}
                <span className={styles.leafLabel}>{step.label || step.ref}</span>
                {ms != null && durationCell(ms)}
                {row.nodeUri && replayButton(replayKey, step.label || step.ref,
                    `tree-replay-${row.nodeUri}#${step.ref}`, t('tree.replayStepHint'))}
            </div>
        )
    }

    const renderMore = (row: TreeRow): React.ReactNode => {
        // A lazy tree step carries its address on the "more" row, so clicking loads the next page of
        // executions. A live frame's inline "more" has no address and stays a plain count.
        const { nodeUri, nodeInstance, step } = row
        const loadMore = nodeUri !== undefined && step
            ? () => void fetchTreeChildren(nodeUri, nodeInstance ?? 0, step.ref)
            : undefined
        return (
            <div
                key={row.key}
                className={cx(styles.row, styles.inactive)}
                style={indent(row.depth)}
                {...(loadMore && { onClick: loadMore, onKeyDown: onActivate(loadMore), role: 'button',
                    tabIndex: 0, 'data-testid': `tree-more-${row.key}` })}
            >
                <span className={styles.chevronSlot} />
                <span className={cx(styles.leafLabel, loadMore && styles.moreLink)}>
                    {t('tree.more', { count: row.moreCount })}
                </span>
            </div>
        )
    }

    const renderLoading = (row: TreeRow): React.ReactNode => (
        <div key={row.key} className={cx(styles.row, styles.inactive)} style={indent(row.depth)}>
            <span className={styles.chevronSlot} />
            <Spin size="small" />
            <span className={styles.leafLabel}>{t('tree.loading')}</span>
        </div>
    )

    // A node whose sub-calls overflowed the tree's size limit: label how many were dropped, so the gap is
    // visible instead of silently missing. Not clickable — the dropped branches were never retained to load.
    const renderNotRetained = (row: TreeRow): React.ReactNode => (
        <div
            key={row.key}
            className={cx(styles.row, styles.inactive, styles.notRetained)}
            data-testid={`tree-not-retained-${row.key}`}
            style={indent(row.depth)}
        >
            <span className={styles.chevronSlot} />
            <span className={styles.leafLabel}>{t('tree.notRetained', { count: row.moreCount })}</span>
        </div>
    )

    const render = (row: TreeRow): React.ReactNode => {
        switch (row.type) {
            case 'frame': return renderFrame(row)
            case 'liveStep': return renderLiveStep(row)
            case 'callNode': return renderCallNode(row)
            case 'more': return renderMore(row)
            case 'loading': return renderLoading(row)
            case 'notRetained': return renderNotRetained(row)
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
            {truncated && (
                <div className={styles.truncated} data-testid="trace-tree-truncated">{t('tree.truncated')}</div>
            )}
            {rows.map(render)}
        </div>
    )
}

export default TraceTree
