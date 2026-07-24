import { useTranslation } from 'react-i18next'
import { Tag, Tooltip } from 'antd'
import { BranchesOutlined, SafetyOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { useSharedStyles } from './sharedStyles'
import { MonoChip } from './MonoChip'

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

        .anticon-safety {
            color: ${token.colorInfo};
        }
    `,
    defaultTag: css`
        padding: 0 6px;
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
}))

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
export const BranchLabel = ({ name, isDefault, isProtected, withIcon, prominent, testId, className }: BranchLabelProps) => {
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const { t } = useTranslation('repository')
    return (
        <span className={cx(styles.label, prominent && styles.prominent, className)} data-testid={testId}>
            {withIcon && <BranchesOutlined />}
            <MonoChip ellipsis={!prominent} {...(prominent ? { className: styles.prominentName } : {})}>
                {name}
            </MonoChip>
            {isDefault && (
                <Tag className={cx(shared.chipTag, styles.defaultTag)} data-testid={testId && `${testId}-default`}>
                    {t('browser.branch.default_tag')}
                </Tag>
            )}
            {isProtected && (
                <Tooltip title={t('browser.branch.protected_tag')}>
                    <SafetyOutlined data-testid={testId && `${testId}-protected`} />
                </Tooltip>
            )}
        </span>
    )
}
