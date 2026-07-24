import { BranchesOutlined, DatabaseOutlined, HddOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { useSharedStyles } from './sharedStyles'

const GIT_REPOSITORY_TYPE = 'repo-git'
const LOCAL_REPOSITORY_TYPE = 'repo-file'

const useStyles = createStyles(({ css, token }) => ({
    badge: css`
        display: inline-flex;
        align-items: center;
        gap: 6px;
        min-width: 0;
        color: ${token.colorTextTertiary};

        .anticon {
            flex: none;
            font-size: 13px;
        }
    `,
}))

interface RepoIconProps {
    type?: string | undefined
    className?: string
}

/** The icon a repository carries: a branch for Git, a disk for the local files, a database otherwise. */
export const RepoIcon = ({ type, className }: RepoIconProps) => {
    const kind = iconKind(type)
    const Icon = kind === 'local' ? HddOutlined : kind === 'git' ? BranchesOutlined : DatabaseOutlined
    return <Icon className={className} data-testid={`repo-badge-${kind}`} />
}

interface RepoBadgeProps {
    name: string
    type?: string | undefined
    className?: string
}

/**
 * A repository shown as a facet chip — a small icon plus its monospace name.
 */
export const RepoBadge = ({ name, type, className }: RepoBadgeProps) => {
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    return (
        <span className={cx(shared.mono, styles.badge, className)} title={name}>
            <RepoIcon type={type} />
            <span className={shared.ellipsis}>{name}</span>
        </span>
    )
}

const iconKind = (type: string | undefined) => {
    if (type === LOCAL_REPOSITORY_TYPE) {
        return 'local'
    }
    if (type === GIT_REPOSITORY_TYPE) {
        return 'git'
    }
    return 'database'
}
