import React, { useEffect, useMemo, useState } from 'react'
import { Checkbox, Empty, Spin, Tooltip } from 'antd'
import { CaretDownOutlined, CaretRightOutlined, LinkOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { treeChildKey, useTraceStore } from 'store'
import type { SimpleInspectTarget, SimpleStepFocus } from 'store/traceStore'
import type { CallNodeView, StepValueView } from 'types/trace'
import ConditionRow from './ConditionRow'
import { displaySteps, isCondition } from './decisionRows'
import DispatchBadge from './DispatchBadge'
import { onActivate } from './keyboardActivate'
import { kindIcon, stepIcon } from './TraceIcons'
import { useFlashJump } from './useFlashJump'
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
    /** For a step-reference row, the key of the original step row it points at. */
    refTargetKey?: string
    /** For a capped-branch note, how many sub-calls were dropped. */
    count?: number
}

const nodeTarget = (node: CallNodeView): SimpleInspectTarget => ({
    key: `${node.uri}@${node.instance}`,
    selectionKey: `${node.uri}@${node.instance}`,
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
    return {
        key: selectionKey,
        selectionKey,
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
    const walkNode = (node: CallNodeView, depth: number, path: string, refBase?: string): void => {
        if (node.kind === 'stepRef') {
            rows.push({ type: 'ref', key: path, depth, node,
                ...(node.refStep && refBase ? { refTargetKey: `${refBase}/${node.refStep}` } : {}) })
            return
        }
        const open = expanded.has(path)
        rows.push({ type: 'node', key: path, depth, node, target: nodeTarget(node),
            ...(node.steps.length > 0 ? { expandKey: path } : {}) })
        if (!open) {
            return
        }
        // The business view stays plain by default: a decision table shows only its returned rule, unless
        // "Show detailed trace" is on, which reveals the per-condition breakdown like the classic trace.
        const steps = displaySteps(node.steps, showDetailed)
        for (const step of steps) {
            const stepPath = `${path}/${step.ref}`
            // A decision-table condition is an info row: its synthetic ref runs nothing, so it is not clickable.
            if (isCondition(step)) {
                rows.push({ type: 'condition', key: stepPath, depth: depth + 1, step })
                continue
            }
            const kids = step.children ?? children[treeChildKey(node.uri, node.instance, step.ref)] ?? []
            rows.push({ type: 'step', key: stepPath, depth: depth + 1, step, owner: node,
                target: stepTarget(node, step), ...(kids.length > 0 ? { expandKey: stepPath } : {}) })
            if (kids.length > 0 && expanded.has(stepPath)) {
                kids.forEach((kid, i) => walkNode(kid, depth + 2, `${stepPath}#${i}`, path))
            }
        }
        if ((node.notRetained ?? 0) > 0) {
            rows.push({ type: 'notRetained', key: `${path}/notRetained`, depth: depth + 1,
                count: node.notRetained ?? 0 })
        }
    }
    walkNode(root, 0, 'tree')
    return rows
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
    const loaded = useTraceStore(s => s.simpleLoadedCount)
    const total = useTraceStore(s => s.simpleTotalCount)
    const selectedKey = useTraceStore(s => s.simpleSelectedKey)
    const inspect = useTraceStore(s => s.simpleInspect)
    const status = useTraceStore(s => s.status)
    const showDetailed = useTraceStore(s => s.showDetailed)
    const setShowDetailed = useTraceStore(s => s.setShowDetailed)
    const { treeRef, flashKey, jumpToRow } = useFlashJump()

    // The root starts open so the run's top-level steps read at a glance; everything deeper is collapsed.
    // Keyed on the snapshot: only a new Run replaces it, while inspect re-runs never touch the expansions.
    const [expanded, setExpanded] = useState<Set<string>>(new Set(['tree']))
    useEffect(() => {
        setExpanded(new Set(['tree']))
    }, [tree])

    const rows = useMemo(
        () => (tree && ready ? flattenSimple(tree, children, expanded, showDetailed) : []),
        [tree, ready, children, expanded, showDetailed]
    )

    if (preparing) {
        return (
            <div className={styles.progress} data-testid="simple-tree-progress">
                <Spin size="small" />
                <span>
                    {status === 'running'
                        ? t('simple.calculating')
                        : t('simple.preparing', { loaded, total: total ?? loaded })}
                </span>
            </div>
        )
    }
    if (!tree || !ready) {
        return <Empty description={t('simple.pressRun')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
    }

    const indent = (depth: number): React.CSSProperties => ({ paddingLeft: 8 + depth * 14 })

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
                aria-expanded={expanded.has(expandKey)}
                className={styles.chevron}
                data-testid={`simple-toggle-${expandKey}`}
                onClick={(e) => { e.stopPropagation(); toggle(expandKey) }}
                role="button"
                tabIndex={0}
                onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault()
                        e.stopPropagation()
                        toggle(expandKey)
                    }
                }}
            >
                {expanded.has(expandKey) ? <CaretDownOutlined /> : <CaretRightOutlined />}
            </span>
        )
    }

    const renderNode = (row: SimpleRow): React.ReactNode => {
        const node = row.node as CallNodeView
        const target = row.target as SimpleInspectTarget
        const open = () => void inspect(target)
        return (
            <Tooltip key={row.key} title={t('simple.inspectHint')}>
                <div
                    data-rowkey={row.key}
                    data-testid={`simple-node-${row.key}`}
                    onClick={open}
                    onKeyDown={onActivate(open)}
                    role="button"
                    style={indent(row.depth)}
                    tabIndex={0}
                    className={cx(styles.row, styles.frame, styles.runnable,
                        selectedKey === (target.selectionKey ?? target.key) && styles.selected)}
                >
                    {twisty(row.expandKey)}
                    {kindIcon(node.kind)}
                    <span className={styles.name}>{node.name}</span>
                    <span className={styles.kind}>{node.kind}</span>
                    <DispatchBadge dispatch={node.dispatch} />
                </div>
            </Tooltip>
        )
    }

    const renderStep = (row: SimpleRow): React.ReactNode => {
        const step = row.step as StepValueView
        const target = row.target as SimpleInspectTarget
        const open = () => void inspect(target)
        return (
            <Tooltip key={row.key} title={t('simple.inspectHint')}>
                <div
                    data-rowkey={row.key}
                    data-testid={`simple-step-${row.key}`}
                    onClick={open}
                    onKeyDown={onActivate(open)}
                    role="button"
                    style={indent(row.depth)}
                    tabIndex={0}
                    className={cx(styles.row, styles.runnable,
                        selectedKey === (target.selectionKey ?? target.key) && styles.selected,
                        flashKey === row.key && styles.flashed)}
                >
                    {twisty(row.expandKey)}
                    {stepIcon(row.owner?.kind)}
                    <span className={styles.leafLabel}>{step.label || step.ref}</span>
                </div>
            </Tooltip>
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

    const renderRef = (row: SimpleRow): React.ReactNode => {
        const node = row.node as CallNodeView
        const jump = row.refTargetKey ? () => jumpToRow(row.refTargetKey as string) : undefined
        return (
            <Tooltip key={row.key} title={t('tree.referenceHint')}>
                <div
                    className={cx(styles.row, styles.inactive, row.refTargetKey && styles.runnable)}
                    data-testid={`simple-ref-${row.key}`}
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

    const renderNotRetained = (row: SimpleRow): React.ReactNode => (
        <div
            key={row.key}
            className={cx(styles.row, styles.inactive, styles.notRetained)}
            style={indent(row.depth)}
        >
            <span className={styles.chevronSlot} />
            <span className={styles.leafLabel}>{t('tree.notRetained', { count: row.count })}</span>
        </div>
    )

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
        <div ref={treeRef} className={styles.tree} data-testid="simple-tree">
            <div className={styles.header}>
                <span>{t('tree.title')}</span>
                <span className={styles.headerControls}>
                    <Checkbox
                        checked={showDetailed}
                        className={styles.detailedToggle}
                        data-testid="simple-detailed"
                        onChange={(e) => setShowDetailed(e.target.checked)}
                    >
                        {t('tree.showDetailed')}
                    </Checkbox>
                </span>
            </div>
            {rows.map(render)}
        </div>
    )
}

export default SimpleTraceTree
