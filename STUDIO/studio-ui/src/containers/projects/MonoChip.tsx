import type { ReactNode } from 'react'
import { createStyles } from 'antd-style'
import { useSharedStyles } from './sharedStyles'

const useStyles = createStyles(({ css, token }) => ({
    mono: css`
        color: ${token.colorTextTertiary};
    `,
    filled: css`
        display: inline-block;
        max-width: 100%;
        padding: 0 6px;
        border-radius: ${token.borderRadiusSM}px;
        background: ${token.colorFillTertiary};
        color: ${token.colorTextSecondary};
        line-height: 20px;
        vertical-align: middle;
    `,
    ellipsis: css`
        display: inline-block;
        max-width: 100%;
        /* Lets the chip shrink inside a flex row instead of forcing it wider than its cell. */
        min-width: 0;
        vertical-align: middle;
    `,
}))

interface MonoChipProps {
    children: ReactNode
    /** Wraps the text in a subtle filled chip (used for the branch pill on list rows). */
    filled?: boolean
    /** Truncate with an ellipsis instead of wrapping. */
    ellipsis?: boolean
    className?: string
    title?: string
    'data-testid'?: string
}

/**
 * Monospace inline text — the heavy code-font motif of the mockup (repository names, branches, paths,
 * revisions, service names). Plain by default; `filled` renders a subtle chip.
 */
export const MonoChip = ({ children, filled, ellipsis, className, title, 'data-testid': testId }: MonoChipProps) => {
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const clipped = filled || ellipsis
    return (
        <span
            data-testid={testId}
            title={title}
            className={cx(
                shared.mono,
                styles.mono,
                clipped && shared.ellipsis,
                filled && styles.filled,
                !filled && ellipsis && styles.ellipsis,
                className
            )}
        >
            {children}
        </span>
    )
}
