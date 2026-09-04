import React from 'react'
import { CheckOutlined, CloseOutlined } from '@ant-design/icons'
import type { StepValueView } from 'types/trace'
import { useStyles } from './TraceTree.styles'

interface ConditionRowProps {
    step: StepValueView
    depth: number
    /** Stable test id, namespaced by the tree that renders the row. */
    testId: string
    /** Row identity for scroll-to-original jumps; only the advanced tree targets it. */
    rowKey?: string
    /**
     * Prepend an empty status-mark column so the ✓/✗ lines up with the sibling rule rows. The advanced tree
     * carries that column on every row (to align with live-step status marks); the business tree does not.
     */
    markSlot?: boolean
}

/**
 * A decision table's evaluated condition: a green check when it matched, a red cross when it did not.
 * An informational row — never runnable or replayable — reproducing the legacy detailed trace. Shared by
 * the business and advanced trees so both read identically.
 */
const ConditionRow: React.FC<ConditionRowProps> = ({ step, depth, testId, rowKey, markSlot }) => {
    const { styles, cx } = useStyles()
    const matched = step.decision === 'matched'
    return (
        <div
            className={cx(styles.row, styles.conditionRow)}
            data-rowkey={rowKey}
            data-testid={testId}
            style={{ paddingLeft: 8 + depth * 14 }}
        >
            <span className={styles.chevronSlot} />
            {markSlot && <span className={styles.mark} />}
            {matched
                ? <CheckOutlined className={cx(styles.mark, styles.condMatched)} />
                : <CloseOutlined className={cx(styles.mark, styles.condUnmatched)} />}
            <span className={cx(styles.leafLabel, styles.condLabel)}>{step.label || step.ref}</span>
        </div>
    )
}

export default ConditionRow
