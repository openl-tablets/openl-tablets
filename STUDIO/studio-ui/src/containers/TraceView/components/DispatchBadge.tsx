import React from 'react'
import { Tooltip } from 'antd'
import { BranchesOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import type { DispatchInfo } from 'types/trace'
import { useStyles } from './TraceTree.styles'

/**
 * Badge on a dispatched table (versioned by dimension properties): the table is shown in place, badged
 * with the number of versions it was chosen from, and the tooltip lists them with the chosen one flagged.
 * Renders nothing for an undispatched table.
 */
const DispatchBadge: React.FC<{ dispatch?: DispatchInfo | null | undefined }> = ({ dispatch }) => {
    const { t } = useTranslation('trace')
    const { styles, cx } = useStyles()
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

export default DispatchBadge
