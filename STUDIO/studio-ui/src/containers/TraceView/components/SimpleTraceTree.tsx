import React, { useEffect, useMemo, useRef, useState } from 'react'
import { Button, Empty, Spin, Typography } from 'antd'
import { LinkOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { treeChildKey, useTraceStore } from 'store'
import { walkDepthFirst, type Later } from 'utils/depthFirst'
import { isTraceExecutionError } from 'utils/traceExecutionStatus'
import type { SimpleBranchState, SimpleInspectTarget, SimpleStepFocus } from 'store/traceStore'
import type { CallNodeView, StepValueView } from 'types/trace'
import ConditionRow from './ConditionRow'
import { displaySteps, isCondition } from './decisionRows'
import DispatchBadge from './DispatchBadge'
import { onActivate } from 'utils/keyboardActivate'
import { kindIcon, stepIcon } from './TraceIcons'
import { LoadingRow, MoreRow, NotRetainedRow, treeIndent, toggleKey, Twisty } from './TreeRow'
import { useStyles } from './TraceTree.styles'

/** How many calls of an expanded step the tree shows at first, and how many more each "more" row shows. */
const CALLS_PER_PAGE = 100

/** A step whose calls the kept tree still has to give: its address, and the tree path its calls show at. */
interface Branch {
    uri: string
    instance: number
    step: string
    path: string
}

/** What a "more" row shows next: the calls of the step already here, then the next ones from the kept tree. */
interface MoreCalls {
    /** The tree path of the step's row. */
    path: string
    /** How many of the step's calls are shown now. */
    shown: number
    /** How many of the step's calls are here. */
    loaded: number
    /** Set when the kept tree has more calls of the step to load. */
    branch?: Branch
}

/** One row of the flattened simple tree: a rule call, one of its steps, a step reference, or a marker. */
interface SimpleRow {
    type: 'node' | 'step' | 'condition' | 'ref' | 'notRetained' | 'more' | 'loading'
    key: string
    depth: number
    node?: CallNodeView
    step?: StepValueView
    /** The table a step belongs to, for its icon. */
    owner?: CallNodeView
    /** Set when the row has sub-rows to expand. */
    expandKey?: string
    /** What a click inspects: re-runs execution through this row so its inputs and result are readable. */
    target?: SimpleInspectTarget
    /** True when a step row is a reference rendered inline (the same step used again elsewhere). */
    isRef?: boolean
    /** For a capped-branch note, how many sub-calls were dropped; for a "more" row, how many are not shown. */
    count?: number
    /** For a "more" row, what a click shows next; absent when nothing more can be shown. */
    more?: MoreCalls
}

/** The rows of the expanded tree, and the expanded steps none of whose sub-calls are loaded yet. */
interface Flattened {
    rows: SimpleRow[]
    unloaded: Branch[]
}


const nodeTarget = (node: CallNodeView): SimpleInspectTarget => ({
    // The selection highlight falls back to the run key when no distinct selectionKey is given, and a
    // table row's run key already uniquely identifies it — so no separate selectionKey is needed here.
    key: `${node.uri}@${node.instance}`,
    frameUri: node.uri,
    frameInstance: node.instance,
    stepType: 'out',
    label: node.name,
})

/**
 * What a click on a step inspects: execution runs right through the step within its owning table. The
 * Details panel then presents it like the classic trace — the owning table's inputs as Parameters and
 * the step's own computed value as the result.
 */
const stepTarget = (owner: CallNodeView, step: StepValueView): SimpleInspectTarget => {
    const focus: SimpleStepFocus = {
        ref: step.ref,
        label: step.label || step.ref,
        ownerUri: owner.uri,
        ownerInstance: owner.instance,
    }
    // The step's own key identifies its row for the selection highlight — unique even for a static cell,
    // whose run key below is the shared owning table.
    const selectionKey = `${owner.uri}#${step.ref}@${owner.instance}`
    if (step.constant) {
        // Static content never executes, so there is no line to run to — run the owning table through
        // instead: its frozen steps then carry this cell's value for the Details panel.
        return {
            key: `${owner.uri}@${owner.instance}`,
            selectionKey,
            frameUri: owner.uri,
            frameInstance: owner.instance,
            stepType: 'out',
            label: focus.label,
            focus,
        }
    }
    // The run key already IS the step's unique key, so the selection highlight's `?? key` fallback covers
    // it — only the static-cell branch above, whose run key is the shared owning table, needs a distinct one.
    return {
        key: selectionKey,
        frameUri: owner.uri,
        frameInstance: owner.instance,
        stepType: 'over',
        label: focus.label,
        focus,
    }
}

/** Where the loading of each step's calls stands, by `treeChildKey`, and whether the kept tree was released. */
interface Loading {
    state: Record<string, SimpleBranchState>
    released: boolean
}

/**
 * Flatten the downloaded tree into indented rows, walking only expanded branches. An expanded step shows its calls a
 * page at a time (`shown`, by the path of the step's row), and ends with a row that shows the next of them: those
 * already here, then the next ones from the kept tree. While they load, the row shows that. An expanded step with
 * none of them here is reported, so they are loaded. A branch thousands of calls deep, as a runaway recursion leaves,
 * is walked without running out of call stack.
 */
const flattenSimple = (
    root: CallNodeView,
    children: Record<string, CallNodeView[]>,
    { state, released }: Loading,
    expanded: Set<string>,
    shown: Record<string, number>,
    showDetailed: boolean
): Flattened => {
    const rows: SimpleRow[] = []
    const unloaded: Branch[] = []
    let later: Later = () => undefined
    // The calls of an expanded step, a page at a time, then what is not shown: loading, or a row showing more.
    const walkCalls = (owner: CallNodeView, step: StepValueView, kids: CallNodeView[], path: string, depth: number,
        walkKid: (kid: CallNodeView, path: string) => void): void => {
        const key = treeChildKey(owner.uri, owner.instance, step.ref)
        const visible = Math.min(kids.length, shown[path] ?? CALLS_PER_PAGE)
        later(...kids.slice(0, visible).map((kid, i) => () => walkKid(kid, `${path}#${i}`)), () => {
            if (state[key] === 'loading') {
                rows.push({ type: 'loading', key: `${path}/loading`, depth })
                return
            }
            const total = Math.max(step.childrenTotal ?? 0, kids.length)
            if (visible >= total) {
                return
            }
            // The first view holds the upper levels of the tree: a deeper step reports how many calls it made, and
            // they load from the tree the server keeps.
            const branch = kids.length < total && !released && state[key] !== 'ended'
                ? { uri: owner.uri, instance: owner.instance, step: step.ref, path }
                : undefined
            if (branch && kids.length === 0) {
                unloaded.push(branch)
            }
            const canShowMore = visible < kids.length || branch !== undefined
            rows.push({ type: 'more', key: `${path}/more`, depth, count: total - visible,
                ...(canShowMore ? { more: { path, shown: visible, loaded: kids.length, ...(branch ? { branch } : {}) } } : {}) })
        })
    }
    /** Whether a step has calls to expand: here, or reported by the first view and kept by the server. */
    const hasCalls = (step: StepValueView, kids: CallNodeView[]): boolean =>
        kids.length > 0 || (step.childrenTotal ?? 0) > 0
    // A referenced step ($…@…) is rendered INLINE in the business view — its label, icon, click-to-inspect
    // and its own expandable subtree — right where it is used, so how it was computed is one expand away
    // instead of a hunt for the original elsewhere in the tree. (The advanced tree jumps to it instead.)
    const walkRef = (node: CallNodeView, depth: number, path: string, refOwner?: CallNodeView): void => {
        const original = node.refStep && refOwner
            ? refOwner.steps.find(s => s.ref === node.refStep)
            : undefined
        if (!refOwner || !original) {
            // The original is not in the snapshot (should not happen once the tree is downloaded): a marker.
            rows.push({ type: 'ref', key: path, depth, node })
            return
        }
        const kids = original.children
            ?? children[treeChildKey(refOwner.uri, refOwner.instance, original.ref)] ?? []
        rows.push({ type: 'step', key: path, depth, step: original, owner: refOwner, isRef: true,
            // Run to the original step to read its inputs/result; the row's own path keeps the highlight
            // here rather than lighting up the original occurrence elsewhere too.
            target: { ...stepTarget(refOwner, original), selectionKey: path },
            ...(hasCalls(original, kids) ? { expandKey: path } : {}) })
        if (expanded.has(path)) {
            walkCalls(refOwner, original, kids, path, depth + 1,
                (kid, kidPath) => walkNode(kid, depth + 1, kidPath, refOwner))
        }
    }

    const walkStep = (node: CallNodeView, step: StepValueView, depth: number, path: string): void => {
        const stepPath = `${path}/${step.ref}`
        // A decision-table condition is an info row: its synthetic ref runs nothing, so it is not clickable.
        if (isCondition(step)) {
            rows.push({ type: 'condition', key: stepPath, depth: depth + 1, step })
            return
        }
        const kids = step.children ?? children[treeChildKey(node.uri, node.instance, step.ref)] ?? []
        // A decision-table breakdown row (the returned rule) is not a spreadsheet cell, so it has no
        // step-inputs to show — inspect the owning DT frame instead, whose Details carry the rule's result and
        // the table with the fired rule highlighted. The row keeps its own selection highlight.
        const target = node.kind === 'decisionTable'
            ? { ...nodeTarget(node), selectionKey: stepPath }
            : stepTarget(node, step)
        rows.push({ type: 'step', key: stepPath, depth: depth + 1, step, owner: node,
            target, ...(hasCalls(step, kids) ? { expandKey: stepPath } : {}) })
        if (expanded.has(stepPath)) {
            walkCalls(node, step, kids, stepPath, depth + 2, (kid, kidPath) => walkNode(kid, depth + 2, kidPath, node))
        }
    }

    const walkNode = (node: CallNodeView, depth: number, path: string, refOwner?: CallNodeView): void => {
        if (node.kind === 'stepRef') {
            walkRef(node, depth, path, refOwner)
            return
        }
        rows.push({ type: 'node', key: path, depth, node, target: nodeTarget(node),
            ...(node.steps.length > 0 ? { expandKey: path } : {}) })
        if (!expanded.has(path)) {
            return
        }
        // The business view stays plain by default: a decision table shows only its returned rule, unless
        // "Show detailed trace" is on, which reveals the per-condition breakdown like the classic trace.
        later(
            ...displaySteps(node.steps, showDetailed).map(step => () => walkStep(node, step, depth, path)),
            () => {
                if ((node.notRetained ?? 0) > 0) {
                    rows.push({ type: 'notRetained', key: `${path}/notRetained`, depth: depth + 1,
                        count: node.notRetained ?? 0 })
                }
            }
        )
    }
    walkDepthFirst(queue => {
        later = queue
        walkNode(root, 0, 'tree')
    })
    return { rows, unloaded }
}

/** The detailed-view marker a failed table node and every step on the path to it carry. */
const ERROR_SUFFIX = ' = ERROR'

/** Whether a detailed title marks a failed table or step on the error path. */
const isErrorLabel = (text: string): boolean => text.endsWith(ERROR_SUFFIX)

/** The paths an error-path walk opens, the calls it shows at each step, and its queue of later visits. */
interface ErrorWalk {
    open: Set<string>
    reveal: Record<string, number>
    later: (...tasks: Array<() => void>) => void
}

/** Show the calls of the step at `stepPath` up to the call at `index`. */
const revealCall = (reveal: Record<string, number>, stepPath: string, index: number): void => {
    reveal[stepPath] = Math.max(reveal[stepPath] ?? 0, index + 1)
}

/** Visit a node of the error path; `reportFailed` tells its parent step that the node failed. */
const visitErrorPath = (walk: ErrorWalk, node: CallNodeView, path: string, reportFailed: () => void): void => {
    const failedSteps = new Set<string>()
    const calls: Array<() => void> = []
    for (const step of node.steps) {
        const stepPath = `${path}/${step.ref}`
        if ((step.label ?? '').endsWith(ERROR_SUFFIX)) {
            failedSteps.add(stepPath)
        }
        (step.children ?? []).forEach((child, i) => calls.push(() => visitErrorPath(walk, child, `${stepPath}#${i}`,
            () => {
                failedSteps.add(stepPath)
                revealCall(walk.reveal, stepPath, i)
            })))
    }
    // Once every call below has said whether it failed, the node knows the steps to open.
    walk.later(...calls, () => {
        failedSteps.forEach(stepPath => walk.open.add(stepPath))
        if (failedSteps.size > 0) {
            walk.open.add(path)
        }
        if (failedSteps.size > 0 || (node.name ?? '').endsWith(ERROR_SUFFIX)) {
            reportFailed()
        }
    })
}

/**
 * Collect the tree paths to open so a failed run's whole error branch shows at once: every node and step on
 * the path marked "= ERROR". Path keys mirror flattenSimple's. A step that leads into a failed call is opened
 * too, however deep the call is, and shows calls up to the failed one (`reveal`, by the path of the step's row).
 * `root` is the node at `rootPath`: the root of the tree, or a call of a loaded branch, whose error path opens as
 * it arrives. `onFailed` is told when `root` itself failed, so the step of a loaded call can show it.
 */
const collectErrorPath = (root: CallNodeView, open: Set<string>, reveal: Record<string, number>,
    rootPath = 'tree', onFailed: () => void = () => undefined): void => {
    walkDepthFirst(later => visitErrorPath({ open, reveal, later }, root, rootPath, onFailed))
}

/**
 * Collect the error paths of the loaded calls of a step that arrived from `from` on, shown under the step's row
 * at `path`. A failed call past the first page of the step shows at once, as it does in the first view.
 */
const collectArrivedErrorPaths = (calls: CallNodeView[], from: number, path: string, open: Set<string>,
    reveal: Record<string, number>): void => {
    calls.slice(from).forEach((call, i) =>
        collectErrorPath(call, open, reveal, `${path}#${from + i}`, () => revealCall(reveal, path, from + i)))
}

/** Whether the run failed: only a failed run has an error path, and its root reads "= ERROR". */
const isFailedRun = (root: CallNodeView): boolean => (root.name ?? '').endsWith(ERROR_SUFFIX)

/** The page counts that show, at each step, the failed calls its error path leads to. */
const pagesFor = (reveal: Record<string, number>, prev: Record<string, number> = {}): Record<string, number> =>
    Object.fromEntries(Object.entries(reveal).map(([path, count]) => [path, Math.max(prev[path] ?? CALLS_PER_PAGE, count)]))

/**
 * Business view of a finished trace: the executed tree of the run, its upper levels at once and its deep branches
 * loaded on expand from the tree the server keeps — the calculation is not run again for them. Clicking a row
 * silently re-runs execution through it, so its inputs and result appear in the Details panel — like the classic
 * trace, with no stepping.
 */
const SimpleTraceTree: React.FC = () => {
    const { t } = useTranslation('trace')
    const treeStyles = useStyles()
    const { styles, cx } = treeStyles
    const tree = useTraceStore(s => s.simpleTree)
    const children = useTraceStore(s => s.simpleChildren)
    const branchState = useTraceStore(s => s.simpleBranchState)
    const treeReleased = useTraceStore(s => s.simpleTreeReleased)
    const loadBranch = useTraceStore(s => s.loadSimpleBranch)
    const ready = useTraceStore(s => s.simpleReady)
    const preparing = useTraceStore(s => s.simpleLoading)
    const status = useTraceStore(s => s.status)
    const error = useTraceStore(s => s.error)
    const simpleRun = useTraceStore(s => s.simpleRun)
    const selectedKey = useTraceStore(s => s.simpleSelectedKey)
    const inspect = useTraceStore(s => s.simpleInspect)
    const showDetailed = useTraceStore(s => s.showDetailed)

    // The root starts open so the run's top-level steps read at a glance; everything deeper is collapsed.
    // Keyed on the snapshot: only a new Run replaces it, while inspect re-runs never touch the expansions.
    const [expanded, setExpanded] = useState<Set<string>>(new Set(['tree']))
    // How many calls each expanded step shows, by the path of its row, past the first page.
    const [shown, setShown] = useState<Record<string, number>>({})
    // The tree paths each requested branch shows at — a step shown both in place and as a reference has two — and
    // how many calls of each step had arrived, so the error path of a branch opens as it arrives, as it did for
    // the first view.
    const requested = useRef(new Map<string, Set<string>>())
    const arrived = useRef<Record<string, number>>({})
    useEffect(() => {
        const open = new Set(['tree'])
        const reveal: Record<string, number> = {}
        // On a failed run the whole path to the error reads "= ERROR"; open it so the failing node shows at
        // once — like the advanced view opens the frame it stopped on — instead of being lost deep in the tree.
        if (tree && isFailedRun(tree)) {
            collectErrorPath(tree, open, reveal)
        }
        setExpanded(open)
        setShown(pagesFor(reveal))
        requested.current.clear()
        arrived.current = {}
    }, [tree])

    useEffect(() => {
        if (!tree || !isFailedRun(tree)) {
            return
        }
        const open = new Set<string>()
        const reveal: Record<string, number> = {}
        for (const [key, paths] of requested.current) {
            const calls = children[key] ?? []
            const from = arrived.current[key] ?? 0
            paths.forEach(path => collectArrivedErrorPaths(calls, from, path, open, reveal))
            arrived.current[key] = calls.length
        }
        if (open.size > 0) {
            setExpanded(prev => new Set([...prev, ...open]))
        }
        if (Object.keys(reveal).length > 0) {
            setShown(prev => ({ ...prev, ...pagesFor(reveal, prev) }))
        }
    }, [children, tree])

    const { rows, unloaded } = useMemo<Flattened>(
        () => (tree && ready
            ? flattenSimple(tree, children, { state: branchState, released: treeReleased }, expanded, shown, showDetailed)
            : { rows: [], unloaded: []}),
        [tree, ready, children, branchState, treeReleased, expanded, shown, showDetailed]
    )

    /** Remember where a branch shows; true when it was not requested before. */
    const request = (branch: Branch): boolean => {
        const key = treeChildKey(branch.uri, branch.instance, branch.step)
        const paths = requested.current.get(key)
        requested.current.set(key, new Set([...(paths ?? []), branch.path]))
        return paths === undefined
    }

    const showMore = ({ path, shown: visible, loaded, branch }: MoreCalls): void => {
        setShown(prev => ({ ...prev, [path]: visible + CALLS_PER_PAGE }))
        if (branch && visible + CALLS_PER_PAGE > loaded) {
            request(branch)
            void loadBranch(branch.uri, branch.instance, branch.step)
        }
    }

    // An expanded step none of whose calls are here loads them, once: after a failure the "more" row retries.
    useEffect(() => {
        unloaded.forEach(branch => {
            if (request(branch)) {
                void loadBranch(branch.uri, branch.instance, branch.step)
            }
        })
    }, [unloaded, loadBranch])

    if (preparing) {
        // One request runs the whole calculation and returns the tree deep, so there is nothing to page
        // and no count to show — just that the calculation is running.
        return (
            <div className={styles.progress} data-testid="simple-tree-progress">
                <Spin size="small" />
                <span>{t('simple.calculating')}</span>
            </div>
        )
    }
    if (!tree || !ready) {
        // A failed run left no browsable tree. The business view has no toolbar Run button, so offer the
        // retry here — otherwise the only way back is to reopen the window. A rule error is different: it
        // returns a tree (marked `= ERROR`) and never reaches this branch.
        if (isTraceExecutionError(status)) {
            return (
                <div className={styles.progress} data-testid="simple-tree-error">
                    <Empty description={error || t('simple.failed')} image={Empty.PRESENTED_IMAGE_SIMPLE}>
                        <Button data-testid="simple-retry" onClick={() => void simpleRun()} type="primary">
                            {t('simple.retry')}
                        </Button>
                    </Empty>
                </div>
            )
        }
        return <Empty description={t('simple.preparing')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
    }

    const indent = treeIndent

    const toggle = (key: string): void => setExpanded(toggleKey(key))

    const twisty = (expandKey?: string): React.ReactNode =>
        <Twisty expanded={expanded} expandKey={expandKey} onToggle={toggle} testIdPrefix="simple" treeStyles={treeStyles} />

    const renderNode = (row: SimpleRow): React.ReactNode => {
        const node = row.node as CallNodeView
        const target = row.target as SimpleInspectTarget
        const open = () => void inspect(target)
        const failed = isErrorLabel(node.name)
        const selected = selectedKey === (target.selectionKey ?? target.key)
        return (
            <div
                key={row.key}
                aria-selected={selected}
                className={cx(styles.row, styles.frame, styles.runnable, selected && styles.selected)}
                data-failed={failed || undefined}
                data-rowkey={row.key}
                data-testid={`simple-node-${row.key}`}
                onClick={open}
                onKeyDown={onActivate(open)}
                role="treeitem"
                style={indent(row.depth)}
                tabIndex={0}
            >
                {twisty(row.expandKey)}
                {kindIcon(node.kind)}
                {/* The detailed title carries the kind prefix (DT, SpreadSheet…), so no separate kind tag. It
                    truncates in place, with the full title on hover only when it does not fit. A failed path
                    paints the whole label red — not only the "= ERROR" suffix. */}
                <Typography.Text
                    className={cx(styles.labelText, failed && styles.errorLabel)}
                    ellipsis={{ tooltip: node.name }}
                >
                    {node.name}
                </Typography.Text>
                <DispatchBadge dispatch={node.dispatch} treeStyles={treeStyles} />
            </div>
        )
    }

    const renderStep = (row: SimpleRow): React.ReactNode => {
        const step = row.step as StepValueView
        const target = row.target as SimpleInspectTarget
        const open = () => void inspect(target)
        const label = step.label || step.ref
        const failed = isErrorLabel(label)
        const selected = selectedKey === (target.selectionKey ?? target.key)
        return (
            <div
                key={row.key}
                aria-selected={selected}
                className={cx(styles.row, styles.runnable, selected && styles.selected)}
                data-failed={failed || undefined}
                data-rowkey={row.key}
                data-testid={`simple-step-${row.key}`}
                onClick={open}
                onKeyDown={onActivate(open)}
                role="treeitem"
                style={indent(row.depth)}
                tabIndex={0}
            >
                {twisty(row.expandKey)}
                {stepIcon(row.owner?.kind)}
                <Typography.Text
                    className={cx(styles.labelText, failed && styles.errorLabel)}
                    ellipsis={{ tooltip: label }}
                >
                    {label}
                </Typography.Text>
                {/* A referenced step reads exactly like its original occurrence, tagged so it is clear it
                    is the same step used again — its subtree and value are browsable right here. */}
                {row.isRef && <span className={styles.kind}>{t('tree.referenceTag')}</span>}
            </div>
        )
    }

    const renderCondition = (row: SimpleRow): React.ReactNode => (
        <ConditionRow
            key={row.key}
            depth={row.depth}
            step={row.step as StepValueView}
            testId={`simple-condition-${row.key}`}
            treeStyles={treeStyles}
        />
    )

    // Fallback for a reference whose original step is missing from the snapshot (should not happen once
    // the tree is downloaded): a plain, non-interactive marker. The common case renders inline as a step.
    const renderRef = (row: SimpleRow): React.ReactNode => {
        const node = row.node as CallNodeView
        return (
            <div
                key={row.key}
                className={cx(styles.row, styles.inactive)}
                data-testid={`simple-ref-${row.key}`}
                style={indent(row.depth)}
            >
                <span className={styles.chevronSlot} />
                <LinkOutlined className={styles.refIcon} />
                <span className={styles.leafLabel}>{node.name}</span>
                <span className={styles.kind}>{t('tree.referenceTag')}</span>
            </div>
        )
    }

    const renderNotRetained = (row: SimpleRow): React.ReactNode =>
        <NotRetainedRow key={row.key} count={row.count ?? 0} depth={row.depth} treeStyles={treeStyles} />

    const renderMore = (row: SimpleRow): React.ReactNode => {
        const { more } = row
        return (
            <MoreRow
                key={row.key}
                count={row.count ?? 0}
                depth={row.depth}
                onLoad={more && (() => showMore(more))}
                testId={`simple-more-${row.key}`}
                treeStyles={treeStyles}
            />
        )
    }

    const render = (row: SimpleRow): React.ReactNode => {
        switch (row.type) {
            case 'node': return renderNode(row)
            case 'step': return renderStep(row)
            case 'condition': return renderCondition(row)
            case 'ref': return renderRef(row)
            case 'more': return renderMore(row)
            case 'loading':
                return <LoadingRow key={row.key} depth={row.depth} testId={`simple-loading-${row.key}`} treeStyles={treeStyles} />
            default: return renderNotRetained(row)
        }
    }

    return (
        <div className={styles.tree} data-testid="simple-tree" role="tree">
            <div className={styles.header}>
                <span>{t('tree.title')}</span>
            </div>
            <div className={styles.rows}>
                {rows.map(render)}
            </div>
        </div>
    )
}

export default SimpleTraceTree
