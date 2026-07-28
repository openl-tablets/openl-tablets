import React from 'react'
import { CaretDownOutlined, CaretRightOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { useStyles } from './TraceTree.styles'

/**
 * Row primitives shared by the advanced ({@link TraceTree}) and business ({@link SimpleTraceTree}) trees:
 * indentation, the expand/collapse chevron, the expand-set updater, and the "not retained" marker. Each
 * tree keeps its own expansion state and toggle behaviour (the advanced tree also lazily fetches sub-calls)
 * and passes them in, so only the shared markup lives here.
 */

/** Indentation for a tree row at the given depth. */
export const treeIndent = (depth: number): React.CSSProperties => ({ paddingLeft: 8 + depth * 14 })

/** State updater that flips a key's presence in the expanded set. */
export const toggleKey = (key: string) => (prev: Set<string>): Set<string> => {
    const next = new Set(prev)
    if (!next.delete(key)) {
        next.add(key)
    }
    return next
}

interface TwistyProps {
    expandKey: string | undefined
    expanded: Set<string>
    onToggle: (key: string) => void
    /** Prefix for the toggle's data-testid, e.g. `tree` or `simple`. */
    testIdPrefix: string
}

/** A chevron that expands/collapses a row, or an empty slot when the row has nothing to expand. */
export const Twisty: React.FC<TwistyProps> = ({ expandKey, expanded, onToggle, testIdPrefix }) => {
    const { styles } = useStyles()
    if (!expandKey) {
        return <span className={styles.chevronSlot} />
    }
    const key = expandKey
    const fire = (): void => onToggle(key)
    return (
        <span
            aria-expanded={expanded.has(key)}
            className={styles.chevron}
            data-testid={`${testIdPrefix}-toggle-${key}`}
            onClick={(e) => { e.stopPropagation(); fire() }}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault()
                    e.stopPropagation()
                    fire()
                }
            }}
        >
            {expanded.has(key) ? <CaretDownOutlined /> : <CaretRightOutlined />}
        </span>
    )
}

/** The "+N sub-calls not retained" info row shown where the executed tree hit its size cap. */
export const NotRetainedRow: React.FC<{ depth: number; count: number; testId?: string }> = (
    { depth, count, testId }
) => {
    const { styles, cx } = useStyles()
    const { t } = useTranslation('trace')
    return (
        <div className={cx(styles.row, styles.inactive, styles.notRetained)} data-testid={testId} style={treeIndent(depth)}>
            <span className={styles.chevronSlot} />
            <span className={styles.leafLabel}>{t('tree.notRetained', { count })}</span>
        </div>
    )
}
