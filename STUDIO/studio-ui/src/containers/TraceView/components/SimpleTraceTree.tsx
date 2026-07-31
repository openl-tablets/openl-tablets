import React, { useEffect, useMemo, useState } from 'react'
import { Button, Empty, Spin, Typography } from 'antd'
import { LinkOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { treeChildKey, useTraceStore } from 'store'
import { isTraceExecutionError } from 'utils/traceExecutionStatus'
import type { SimpleInspectTarget, SimpleStepFocus } from 'store/traceStore'
import type { CallNodeView, StepValueView } from 'types/trace'
import ConditionRow from './ConditionRow'
import { displaySteps, isCondition } from './decisionRows'
import DispatchBadge from './DispatchBadge'
import { onActivate } from './keyboardActivate'
import { kindIcon, stepIcon } from './TraceIcons'
import { NotRetainedRow, treeIndent, toggleKey, Twisty } from './TreeRow'
import { useStyles } from './TraceTree.styles'

/** One row of the flattened simple tree: a rule call, one of its steps, a step reference, or a capped note. */
interface SimpleRow {
    type: 'node' | 'step' | 'condition' | 'ref' | 'notRetained'
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
    /** For a capped-branch note, how many sub-calls were dropped. */
    count?: number
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

/**
 * Flatten the downloaded tree into indented rows, walking only expanded branches. Everything renders
 * from the snapshot alone — expanding never calls the backend.
 */
const flattenSimple = (
    root: CallNodeView,
    children: Record<string, CallNodeView[]>,
    expanded: Set<string>,
    showDetailed: boolean
): SimpleRow[] => {
    const rows: SimpleRow[] = []
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
            ...(kids.length > 0 ? { expandKey: path } : {}) })
        if (kids.length > 0 && expanded.has(path)) {
            kids.forEach((kid, i) => walkNode(kid, depth + 1, `${path}#${i}`, refOwner))
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
        // The one-shot full tree is capped: a step looped past the limit carries its first sub-calls inline
        // and reports the full count, so the rest read as "omitted" rather than silently missing.
        const omitted = (step.childrenTotal ?? kids.length) - kids.length
        // A decision-table breakdown row (the returned rule) is not a spreadsheet cell, so it has no
        // step-inputs to show — inspect the owning DT frame instead, whose Details carry the rule's result and
        // the table with the fired rule highlighted. The row keeps its own selection highlight.
        const target = node.kind === 'decisionTable'
            ? { ...nodeTarget(node), selectionKey: stepPath }
            : stepTarget(node, step)
        rows.push({ type: 'step', key: stepPath, depth: depth + 1, step, owner: node,
            target, ...(kids.length > 0 || omitted > 0 ? { expandKey: stepPath } : {}) })
        if (expanded.has(stepPath)) {
            kids.forEach((kid, i) => walkNode(kid, depth + 2, `${stepPath}#${i}`, node))
            if (omitted > 0) {
                rows.push({ type: 'notRetained', key: `${stepPath}/omitted`, depth: depth + 2, count: omitted })
            }
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
        displaySteps(node.steps, showDetailed).forEach(step => walkStep(node, step, depth, path))
        if ((node.notRetained ?? 0) > 0) {
            rows.push({ type: 'notRetained', key: `${path}/notRetained`, depth: depth + 1,
                count: node.notRetained ?? 0 })
        }
    }
    walkNode(root, 0, 'tree')
    return rows
}

/** The detailed-view marker a failed table node and every step on the path to it carry. */
const ERROR_SUFFIX = ' = ERROR'

/** Whether a detailed title marks a failed table or step on the error path. */
const isErrorLabel = (text: string): boolean => text.endsWith(ERROR_SUFFIX)

/**
 * Collect the tree paths to open so a failed run's whole error branch shows at once: every node and step on
 * the path marked "= ERROR". Path keys mirror flattenSimple's. Returns whether this subtree failed, so a
 * parent opens the step that leads into it.
 */
const collectErrorPath = (node: CallNodeView, path: string, open: Set<string>): boolean => {
    let failed = (node.name ?? '').endsWith(ERROR_SUFFIX)
    for (const step of node.steps) {
        const stepPath = `${path}/${step.ref}`
        let stepFailed = (step.label ?? '').endsWith(ERROR_SUFFIX)
        const kids = step.children ?? []
        kids.forEach((child, i) => {
            if (collectErrorPath(child, `${stepPath}#${i}`, open)) {
                stepFailed = true
            }
        })
        if (stepFailed) {
            open.add(path)
            open.add(stepPath)
            failed = true
        }
    }
    return failed
}

/**
 * Business view of a finished trace: the whole executed tree, downloaded once and browsed offline.
 * Expanding a branch renders it from the snapshot; clicking a row silently re-runs execution through
 * it, so its inputs and result appear in the Details panel — like the classic trace, with no stepping.
 */
const SimpleTraceTree: React.FC = () => {
    const { t } = useTranslation('trace')
    const { styles, cx } = useStyles()
    const tree = useTraceStore(s => s.simpleTree)
    const children = useTraceStore(s => s.simpleChildren)
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
    useEffect(() => {
        const open = new Set(['tree'])
        // On a failed run the whole path to the error reads "= ERROR"; open it so the failing node shows at
        // once — like the advanced view opens the frame it stopped on — instead of being lost deep in the tree.
        if (tree && (tree.name ?? '').endsWith(ERROR_SUFFIX)) {
            collectErrorPath(tree, 'tree', open)
        }
        setExpanded(open)
    }, [tree])

    const rows = useMemo(
        () => (tree && ready ? flattenSimple(tree, children, expanded, showDetailed) : []),
        [tree, ready, children, expanded, showDetailed]
    )

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
        <Twisty expanded={expanded} expandKey={expandKey} onToggle={toggle} testIdPrefix="simple" />

    const renderNode = (row: SimpleRow): React.ReactNode => {
        const node = row.node as CallNodeView
        const target = row.target as SimpleInspectTarget
        const open = () => void inspect(target)
        const failed = isErrorLabel(node.name)
        return (
            <div
                key={row.key}
                data-failed={failed || undefined}
                data-rowkey={row.key}
                data-testid={`simple-node-${row.key}`}
                onClick={open}
                onKeyDown={onActivate(open)}
                role="treeitem"
                style={indent(row.depth)}
                tabIndex={0}
                className={cx(styles.row, styles.frame, styles.runnable,
                    selectedKey === (target.selectionKey ?? target.key) && styles.selected)}
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
                <DispatchBadge dispatch={node.dispatch} />
            </div>
        )
    }

    const renderStep = (row: SimpleRow): React.ReactNode => {
        const step = row.step as StepValueView
        const target = row.target as SimpleInspectTarget
        const open = () => void inspect(target)
        const label = step.label || step.ref
        const failed = isErrorLabel(label)
        return (
            <div
                key={row.key}
                data-failed={failed || undefined}
                data-rowkey={row.key}
                data-testid={`simple-step-${row.key}`}
                onClick={open}
                onKeyDown={onActivate(open)}
                role="treeitem"
                style={indent(row.depth)}
                tabIndex={0}
                className={cx(styles.row, styles.runnable,
                    selectedKey === (target.selectionKey ?? target.key) && styles.selected)}
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
        <NotRetainedRow key={row.key} count={row.count ?? 0} depth={row.depth} />

    const render = (row: SimpleRow): React.ReactNode => {
        switch (row.type) {
            case 'node': return renderNode(row)
            case 'step': return renderStep(row)
            case 'condition': return renderCondition(row)
            case 'ref': return renderRef(row)
            default: return renderNotRetained(row)
        }
    }

    return (
        <div className={styles.tree} data-testid="simple-tree" role="tree">
            <div className={styles.header}>
                <span>{t('tree.title')}</span>
            </div>
            {rows.map(render)}
        </div>
    )
}

export default SimpleTraceTree
