import type { ReactNode } from 'react'
import { Typography } from 'antd'
import { createStyles } from 'antd-style'

const useStyles = createStyles(({ css, token }) => ({
    header: css`
        min-width: 0;
        padding: 12px 16px;
        border-bottom: 1px solid ${token.colorBorderSecondary};
    `,
    crumb: css`
        display: flex;
        align-items: center;
        gap: 6px;
        color: ${token.colorTextTertiary};
        font-size: 14px;

        a {
            color: ${token.colorTextSecondary};

            &:hover {
                color: ${token.colorPrimary};
            }
        }
    `,
    titleRow: css`
        display: flex;
        flex-wrap: nowrap;
        align-items: center;
        justify-content: space-between;
        gap: 12px;
        min-width: 0;
        margin-top: 8px;
    `,
    titleLeft: css`
        display: flex;
        flex: 1 1 auto;
        align-items: center;
        gap: 12px;
        min-width: 0;
    `,
    title: css`
        margin: 0 !important;
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
    /** The trail above the title: where the thing on screen sits, each step linking back to it. */
    crumbs: ReactNode
    /** What the screen is about — the name of the project, or of the module opened from it. */
    title: string
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
            <div className={styles.crumb}>{crumbs}</div>
            <div className={styles.titleRow}>
                <div className={styles.titleLeft}>
                    {titleBefore}
                    <Typography.Title
                        className={cx(styles.title, muted && styles.titleMuted)}
                        ellipsis={{ tooltip: title }}
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
