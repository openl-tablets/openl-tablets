import { useTranslation } from 'react-i18next'
import { Tag, Tooltip } from 'antd'
import { SafetyOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { useSharedStyles } from './sharedStyles'

const useStyles = createStyles(({ css, token }) => ({
    marks: css`
        display: inline-flex;
        align-items: center;
        gap: 6px;

        .anticon {
            flex: none;
            font-size: 13px;
        }

        .anticon-safety {
            color: ${token.colorInfo};
        }
    `,
    defaultTag: css`
        padding: 0 6px;
    `,
}))

interface BranchMarksProps {
    /** The branch is the repository main branch. */
    isDefault?: boolean | undefined
    /** Direct commits to the branch are restricted. */
    isProtected?: boolean | undefined
    /**
     * Prefix for the marks' test ids — `crumb-branch` yields `crumb-branch-default` and
     * `crumb-branch-protected`.
     */
    testId?: string | undefined
}

/**
 * The marks a branch carries — the Default badge for the repository main branch and the shield for a
 * protected one — and nothing else.
 *
 * The name is left to the caller, so a branch reads in the colour and size of wherever it stands — a filter
 * row, a tree node, a form field — while its marks always look the same. Renders nothing when the branch
 * carries neither mark.
 */
export const BranchMarks = ({ isDefault, isProtected, testId }: BranchMarksProps) => {
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const { t } = useTranslation('repository')
    if (!isDefault && !isProtected) {
        return null
    }
    return (
        <span className={styles.marks}>
            {isDefault && (
                <Tag
                    className={cx(shared.chipTag, styles.defaultTag)}
                    {...(testId ? { 'data-testid': `${testId}-default` } : {})}
                >
                    {t('browser.branch.default_tag')}
                </Tag>
            )}
            {isProtected && (
                <Tooltip title={t('browser.branch.protected_tag')}>
                    <SafetyOutlined
                        aria-label={t('browser.branch.protected_tag')}
                        {...(testId ? { 'data-testid': `${testId}-protected` } : {})}
                    />
                </Tooltip>
            )}
        </span>
    )
}
