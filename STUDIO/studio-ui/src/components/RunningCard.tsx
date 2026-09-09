import React from 'react'
import { Spin } from 'antd'
import { useStyles } from './RunningCard.styles'

export interface RunningCardProps {
    /** What the screen is waiting for, in the words of the screen. */
    description: React.ReactNode
    /** Lies over the screen the work belongs to, instead of standing in the middle of an empty one. */
    overlay?: boolean
    'data-testid'?: string
}

/**
 * What a screen shows while the work it started is still going on: a spinner over a short notice, in the
 * middle of the screen.
 *
 * A screen that already has something to show puts it over that, so nothing underneath is used meanwhile.
 */
export const RunningCard: React.FC<RunningCardProps> = ({ description, overlay, ...rest }) => {
    const { styles, cx } = useStyles()
    return (
        <div className={cx(styles.holder, overlay && styles.overlay)} data-testid={rest['data-testid']}>
            <div className={styles.card}>
                <Spin size="large" />
                <span className={styles.text}>{description}</span>
            </div>
        </div>
    )
}

export default RunningCard
