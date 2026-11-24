import { useTranslation } from 'react-i18next'
import { Tooltip } from 'antd'
import { HomeOutlined, SafetyOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'

const useStyles = createStyles(({ css, token }) => ({
    marks: css`
        display: inline-flex;
        align-items: center;
        gap: ${token.marginXS}px;

        .anticon {
            flex: none;
            font-size: 13px;
        }
    `,
    /** The repository main branch: its name and its house in blue. */
    defaultBranch: css`
        && {
            color: ${token.blue};
        }
    `,
    /** A branch closed to direct commits: its name and its shield in orange. */
    protectedBranch: css`
        && {
            color: ${token.orange};
        }
    `,
}))

/** What a branch is marked as. */
interface BranchKind {
    /** The branch is the repository main branch. */
    isDefault?: boolean | undefined
    /** Direct commits to the branch are restricted. */
    isProtected?: boolean | undefined
}

/**
 * The colour of a branch name, as a class: blue for the repository main branch, orange for a protected one, and
 * none for any other branch, which keeps the colour of where it stands. The main branch stays blue when it is
 * protected too; its shield says so.
 */
export const useBranchColor = (): ((kind: BranchKind) => string | undefined) => {
    const { styles } = useStyles()
    return ({ isDefault, isProtected }) => {
        if (isDefault) {
            return styles.defaultBranch
        }
        return isProtected ? styles.protectedBranch : undefined
    }
}

interface BranchMarksProps extends BranchKind {
    /**
     * Prefix for the marks' test ids — `crumb-branch` yields `crumb-branch-default` and
     * `crumb-branch-protected`.
     */
    testId?: string | undefined
}

/**
 * The marks a branch carries — the blue house of the repository main branch and the orange shield of a protected
 * one — and nothing else. Each names itself in its tooltip.
 *
 * The name is left to the caller, which colours it with {@link useBranchColor}, so a branch reads in the size of
 * wherever it stands — a filter row, a tree node, a form field — while it and its marks share one colour. Renders
 * nothing when the branch carries neither mark.
 */
export const BranchMarks = ({ isDefault, isProtected, testId }: BranchMarksProps) => {
    const { styles } = useStyles()
    const { t } = useTranslation('repository')
    if (!isDefault && !isProtected) {
        return null
    }
    return (
        <span className={styles.marks}>
            {isDefault && (
                <Tooltip title={t('browser.branch.default_tag')}>
                    <HomeOutlined
                        aria-label={t('browser.branch.default_tag')}
                        className={styles.defaultBranch}
                        {...(testId ? { 'data-testid': `${testId}-default` } : {})}
                    />
                </Tooltip>
            )}
            {isProtected && (
                <Tooltip title={t('browser.branch.protected_tag')}>
                    <SafetyOutlined
                        aria-label={t('browser.branch.protected_tag')}
                        className={styles.protectedBranch}
                        {...(testId ? { 'data-testid': `${testId}-protected` } : {})}
                    />
                </Tooltip>
            )}
        </span>
    )
}
