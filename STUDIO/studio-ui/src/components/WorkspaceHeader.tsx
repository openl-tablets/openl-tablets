import type { ReactNode } from 'react'
import { Typography } from 'antd'
import { createStyles } from 'antd-style'

const useStyles = createStyles(({ css, token }) => ({
    header: css`
        min-width: 0;
        padding: ${token.paddingSM}px ${token.padding}px;
        border-bottom: 1px solid ${token.colorBorderSecondary};
    `,
    /*
     * The trail wraps onto as many lines as it needs, and only after a slash: a step keeps its name whole with the
     * slash that follows it, so a narrow window moves a step to the next line rather than breaking its name. A name
     * wider than a whole line is the one thing that ends in an ellipsis.
     */
    crumb: css`
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        column-gap: ${token.marginXS}px;
        row-gap: ${token.marginXXS}px;
        color: ${token.colorTextTertiary};
        font-size: 14px;

        /* Every step that leads somewhere reads alike: a link, or a switcher's trigger. */
        a,
        button {
            color: ${token.colorTextSecondary};
        }

        a:hover {
            color: ${token.colorPrimary};
        }
    `,
    step: css`
        display: inline-flex;
        flex: 0 1 auto;
        align-items: center;
        gap: ${token.marginXS}px;
        min-width: 0;
        max-width: 100%;
        white-space: nowrap;

        > :first-child {
            min-width: 0;
        }
    `,
    titleRow: css`
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: ${token.marginSM}px;
        min-width: 0;
        margin-top: ${token.marginXS}px;
    `,
    titleLeft: css`
        display: flex;
        flex: 1 1 auto;
        align-items: center;
        gap: ${token.marginSM}px;
        min-width: 0;
    `,
    title: css`
        && {
            margin: 0;
        }
        min-width: 0;
        font-size: 22px;
        font-weight: 600;
        letter-spacing: -0.01em;
    `,
    titleMuted: css`
        color: ${token.colorTextTertiary};
        text-decoration: line-through;
    `,
}))

interface WorkspaceHeaderProps {
    /**
     * The trail above the title: where the thing on screen sits, a step each, linking back to it. A slash follows
     * every step, the last one too, so the title reads as the next step.
     */
    crumbs: ReactNode[]
    /**
     * What the screen is about — the name of the project, or of the module opened from it.
     *
     * <p>A screen that offers to open another of the same kind puts the switcher itself here, so the name
     * stays where the reader reads it rather than moving aside for a control.
     */
    title: ReactNode
    /** Shown before the title, as the state of the thing named: a status mark. */
    titleBefore?: ReactNode
    /** Shown after the title, as what is happening to it: a compile indicator. */
    titleAfter?: ReactNode
    /** Struck through and greyed, for a name that no longer stands. */
    muted?: boolean
    /** The actions offered on what the title names. */
    actions?: ReactNode
    testId?: string
}

/**
 * The head of a workspace screen: the trail, the name of what is open, and the actions on it.
 *
 * <p>A project and a module opened from it wear the same head, so opening a module reads as going a step deeper
 * into the project rather than as arriving somewhere else.
 */
export const WorkspaceHeader = ({
    crumbs,
    title,
    titleBefore,
    titleAfter,
    muted = false,
    actions,
    testId,
}: WorkspaceHeaderProps) => {
    const { styles, cx } = useStyles()
    return (
        <div className={styles.header} data-testid={testId}>
            <div className={styles.crumb}>
                {crumbs.filter(Boolean).map((step, index) => (
                    // The steps stand in a fixed order: where each one is in the trail names it.
                    <span key={index} className={styles.step}>
                        {step}
                        <span aria-hidden>/</span>
                    </span>
                ))}
            </div>
            <div className={styles.titleRow}>
                <div className={styles.titleLeft}>
                    {titleBefore}
                    <Typography.Title
                        className={cx(styles.title, muted && styles.titleMuted)}
                        ellipsis={typeof title === 'string' ? { tooltip: title } : true}
                        level={3}
                    >
                        {title}
                    </Typography.Title>
                    {titleAfter}
                </div>
                {actions}
            </div>
        </div>
    )
}
