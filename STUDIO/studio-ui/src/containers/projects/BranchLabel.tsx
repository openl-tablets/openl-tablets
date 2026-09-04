import { BranchesOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { ValueText } from './ValueText'
import { BranchMarks } from './BranchMarks'

const useStyles = createStyles(({ css, token }) => ({
    label: css`
        display: inline-flex;
        align-items: center;
        gap: 6px;
        min-width: 0;
        max-width: 100%;
        color: ${token.colorTextTertiary};

        .anticon {
            flex: none;
            font-size: 13px;
        }
    `,
    /**
     * The branch as the subject of the screen rather than a note about one: the name reads in the normal
     * text colour and is never clipped, so a dialog shows all of it.
     */
    prominent: css`
        color: ${token.colorText};
        flex-wrap: wrap;
    `,
    prominentName: css`
        color: ${token.colorText};
    `,
    /** Breadcrumb tone: the branch reads like the other breadcrumb links, not as faint metadata. */
    secondary: css`
        color: ${token.colorTextSecondary};
    `,
}))

/** How prominently a branch reads. `secondary` matches a breadcrumb link instead of faint metadata. */
export type BranchTone = 'secondary'

interface BranchLabelProps {
    name: string
    /** The branch is the repository main branch. */
    isDefault?: boolean | undefined
    /** Direct commits to the branch are restricted. */
    isProtected?: boolean | undefined
    /** Prefix the name with the branch icon — used where the label stands on its own. */
    withIcon?: boolean
    /**
     * Render the name as the value it is, in the normal text colour and in full. Used where the branch is
     * what the screen is about — a dialog field — instead of metadata beside something else.
     */
    prominent?: boolean | undefined
    /** Colour tone. `secondary` makes the branch read like a breadcrumb link rather than faint metadata. */
    tone?: BranchTone | undefined
    /**
     * Prefix for the marks' test ids — `crumb-branch` yields `crumb-branch-default` and
     * `crumb-branch-protected`.
     */
    testId?: string | undefined
    className?: string
}

/**
 * A branch with the marks it carries — the Default badge for the repository main branch and the shield for
 * a protected one.
 *
 * This is the single rendering of a branch across the workspace: the project list, the breadcrumb, the
 * Overview tab and every entry of the branch switcher, so a branch always reads the same way.
 */
export const BranchLabel = ({ name, isDefault, isProtected, withIcon, prominent, tone, testId, className }: BranchLabelProps) => {
    const { styles, cx } = useStyles()
    const secondary = !prominent && tone === 'secondary'
    const toneClassName = secondary ? styles.secondary : undefined
    const nameClassName = prominent ? styles.prominentName : toneClassName
    return (
        <span className={cx(styles.label, prominent && styles.prominent, secondary && styles.secondary, className)} data-testid={testId}>
            {withIcon && <BranchesOutlined />}
            <ValueText ellipsis={!prominent} {...(nameClassName ? { className: nameClassName } : {})}>
                {name}
            </ValueText>
            <BranchMarks isDefault={isDefault} isProtected={isProtected} testId={testId} />
        </span>
    )
}
