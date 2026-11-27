import { Children, type ReactNode } from 'react'
import { Typography } from 'antd'
import { createStyles } from 'antd-style'

/**
 * The least room the actions take beside the trail, in px: a few buttons and the menu of the rest. With less left on
 * the line of the trail, they go to a line of their own.
 */
const ACTIONS_MIN_WIDTH = 240

const useStyles = createStyles(({ css, token }) => ({
    /*
     * The trail and the actions share one line: the trail as wide as its steps, the actions taking the rest of the
     * line at its right end. Where less than the room of a few buttons is left beside the trail, the actions go to a
     * line of their own under it.
     */
    header: css`
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        column-gap: ${token.marginLG}px;
        row-gap: ${token.marginXS}px;
        min-width: 0;
        padding: ${token.paddingXS}px ${token.padding}px;
        border-bottom: 1px solid ${token.colorBorderSecondary};
    `,
    /*
     * The trail wraps onto as many lines as it needs, and only after a slash: a step keeps its name whole with the
     * slash that follows it, so a narrow window moves a step to the next line rather than breaking its name. A name
     * wider than a whole line is the one thing that ends in an ellipsis.
     */
    crumb: css`
        flex: 0 1 auto;
        min-width: 0;
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
    /*
     * A step is a line of text high, whatever it holds: the button of a mark after the name stands out over the gaps
     * around the line rather than making the head of one screen higher than another's.
     */
    step: css`
        display: inline-flex;
        flex: 0 1 auto;
        align-items: center;
        height: ${Math.round(token.fontSize * token.lineHeight)}px;
        gap: ${token.marginXS}px;
        min-width: 0;
        max-width: 100%;
        white-space: nowrap;

        > :first-child {
            min-width: 0;
        }
    `,
    /** What the screen is about ends the trail: its name, with the marks of its state on either side. */
    title: css`
        && {
            min-width: 0;
            margin: 0;
            color: ${token.colorText};
            font-size: 14px;
            font-weight: 600;
            line-height: inherit;
        }

        /* A switcher in place of the name reads as the name, not as a step leading elsewhere. */
        && button {
            color: ${token.colorText};
            font-weight: 600;
        }
    `,
    titleMuted: css`
        && {
            color: ${token.colorTextTertiary};
            text-decoration: line-through;
        }
    `,
    /**
     * The actions take what the trail leaves of its line, at its right end; the buttons they have no room for go to
     * the menu at the end of their bar.
     */
    actions: css`
        display: flex;
        flex: 1 1 ${ACTIONS_MIN_WIDTH}px;
        justify-content: flex-end;
        min-width: 0;
    `,
}))

interface WorkspaceHeaderProps {
    /** The trail to what is open: where it sits, a step each, linking back to it. A slash follows every step. */
    crumbs: ReactNode[]
    /**
     * What the screen is about — the name of the project, or of the module opened from it — the step of the trail
     * that names the page, at its end unless a step under it follows.
     *
     * <p>A screen that offers to open another of the same kind puts the switcher itself here, so the name
     * stays where the reader reads it rather than moving aside for a control.
     */
    title: ReactNode
    /** Shown before the name, as the state of the thing named: a status mark. */
    titleBefore?: ReactNode
    /** Shown after the name, as what is happening to it: a compile indicator. */
    titleAfter?: ReactNode
    /** The step under what the name names, after it in the trail: the branch the project is read on. */
    trailAfter?: ReactNode
    /** Struck through and greyed, for a name that no longer stands. */
    muted?: boolean
    /** The actions offered on what the name names, at the end of the line of the trail. */
    actions?: ReactNode
    testId?: string
}

/**
 * The head of a workspace screen: the trail to the name of what is open, and the actions on it.
 *
 * <p>The name and the marks of its state stand in the trail as a step of their own, which wraps onto the next line
 * as a whole: the last one, or the one before the step under what it names. The actions stand on the same line, at
 * its right end, and fold the buttons they have no room for into their menu; where the trail leaves them too little
 * of the line, they take a line of their own under it.
 *
 * <p>A project and a module opened from it wear the same head, so opening a module reads as going a step deeper
 * into the project rather than as arriving somewhere else.
 */
export const WorkspaceHeader = ({
    crumbs,
    title,
    titleBefore,
    titleAfter,
    trailAfter,
    muted = false,
    actions,
    testId,
}: WorkspaceHeaderProps) => {
    const { styles, cx } = useStyles()
    return (
        <div className={styles.header} data-testid={testId}>
            <div className={styles.crumb}>
                {/* A step keeps the key it was given, or else its place in the trail; a missing one is left out. */}
                {Children.map(crumbs, step => step && (
                    <span className={styles.step}>
                        {step}
                        <span aria-hidden>/</span>
                    </span>
                ))}
                <span className={styles.step}>
                    {titleBefore}
                    <Typography.Title
                        aria-current="page"
                        className={cx(styles.title, muted && styles.titleMuted)}
                        ellipsis={typeof title === 'string' ? { tooltip: title } : true}
                        level={3}
                    >
                        {title}
                    </Typography.Title>
                    {titleAfter}
                    {trailAfter && <span aria-hidden>/</span>}
                </span>
                {trailAfter && <span className={styles.step}>{trailAfter}</span>}
            </div>
            {actions && <div className={styles.actions}>{actions}</div>}
        </div>
    )
}
