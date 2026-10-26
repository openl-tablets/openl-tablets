import React from 'react'
import { Spin } from 'antd'
import { CaretDownOutlined, CaretRightOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { onActivate } from 'utils/keyboardActivate'
import type { TraceTreeStyles } from './TraceTree.styles'

/**
 * Row primitives shared by the advanced ({@link TraceTree}) and business ({@link SimpleTraceTree}) trees:
 * indentation, the expand/collapse chevron, the expand-set updater, and the "more", "loading" and "not retained"
 * markers. Each tree keeps its own expansion state and toggle behaviour (both lazily fetch sub-calls) and passes
 * them in, so only the shared markup lives here.
 */

/** Indentation for a tree row at the given depth. */
export const treeIndent = (depth: number): React.CSSProperties => ({ paddingLeft: 8 + depth * 14 })

/**
 * Makes a row an item of the tree that runs the given action when it is clicked or activated from the keyboard.
 *
 * Only a frame is ever the selected item of the tree, so such a row is never selected.
 */
export const treeItem = (action: () => void) => ({
    'aria-selected': false,
    onClick: action,
    onKeyDown: onActivate(action),
    role: 'treeitem',
    tabIndex: 0,
})

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
    /** The tree's styles, read once by the tree: a row reads none of its own. */
    treeStyles: TraceTreeStyles
}

/** A chevron that expands/collapses a row, or an empty slot when the row has nothing to expand. */
export const Twisty: React.FC<TwistyProps> = ({ expandKey, expanded, onToggle, testIdPrefix, treeStyles }) => {
    const { styles } = treeStyles
    const { t } = useTranslation('trace')
    if (!expandKey) {
        return <span className={styles.chevronSlot} />
    }
    const key = expandKey
    const fire = (): void => onToggle(key)
    return (
        <button
            aria-expanded={expanded.has(key)}
            aria-label={expanded.has(key) ? t('tree.collapse') : t('tree.expand')}
            className={styles.chevron}
            data-testid={`${testIdPrefix}-toggle-${key}`}
            onClick={(e) => { e.stopPropagation(); fire() }}
            type="button"
        >
            {expanded.has(key) ? <CaretDownOutlined /> : <CaretRightOutlined />}
        </button>
    )
}

/**
 * The "+N more executions" row of a step whose sub-calls are not all shown. Given `onLoad`, a click loads the next
 * of them; without it the row is a plain count.
 */
export const MoreRow: React.FC<{
    depth: number
    count: number
    onLoad?: (() => void) | undefined
    testId?: string
    /** The tree's styles, read once by the tree: a row reads none of its own. */
    treeStyles: TraceTreeStyles
}> = ({ depth, count, onLoad, testId, treeStyles }) => {
    const { styles, cx } = treeStyles
    const { t } = useTranslation('trace')
    return (
        <div
            className={cx(styles.row, styles.inactive)}
            style={treeIndent(depth)}
            {...(onLoad && { ...treeItem(onLoad), 'data-testid': testId })}
        >
            <span className={styles.chevronSlot} />
            <span className={cx(styles.leafLabel, onLoad && styles.moreLink)}>{t('tree.more', { count })}</span>
        </div>
    )
}

/** The row a step shows while its sub-calls load. */
export const LoadingRow: React.FC<{
    depth: number
    testId?: string
    /** The tree's styles, read once by the tree: a row reads none of its own. */
    treeStyles: TraceTreeStyles
}> = ({ depth, testId, treeStyles }) => {
    const { styles, cx } = treeStyles
    const { t } = useTranslation('trace')
    return (
        <div className={cx(styles.row, styles.inactive)} data-testid={testId} style={treeIndent(depth)}>
            <span className={styles.chevronSlot} />
            <Spin size="small" />
            <span className={styles.leafLabel}>{t('tree.loading')}</span>
        </div>
    )
}

/** The "+N sub-calls not retained" info row shown where the executed tree hit its size cap. */
export const NotRetainedRow: React.FC<{
    depth: number
    count: number
    testId?: string
    /** The tree's styles, read once by the tree: a row reads none of its own. */
    treeStyles: TraceTreeStyles
}> = ({ depth, count, testId, treeStyles }) => {
    const { styles, cx } = treeStyles
    const { t } = useTranslation('trace')
    return (
        <div className={cx(styles.row, styles.inactive, styles.notRetained)} data-testid={testId} style={treeIndent(depth)}>
            <span className={styles.chevronSlot} />
            <span className={styles.leafLabel}>{t('tree.notRetained', { count })}</span>
        </div>
    )
}
