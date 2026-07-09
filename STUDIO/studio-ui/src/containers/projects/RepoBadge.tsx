import { BranchesOutlined, DatabaseOutlined, HddOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { ELLIPSIS, MONO_TEXT } from './projectsTheme'

const GIT_REPOSITORY_TYPE = 'repo-git'
const LOCAL_REPOSITORY_TYPE = 'repo-file'

const useStyles = createStyles(({ css, token }) => ({
    badge: css`
        display: inline-flex;
        align-items: center;
        gap: 6px;
        min-width: 0;
        color: ${token.colorTextTertiary};
        ${MONO_TEXT}

        .anticon {
            flex: none;
            font-size: 13px;
        }
    `,
    name: css`
        ${ELLIPSIS}
    `,
}))

interface RepoBadgeProps {
    name: string
    type?: string | undefined
    className?: string
}

/**
 * A repository shown as a facet chip — a small icon plus its monospace name.
 */
export const RepoBadge = ({ name, type, className }: RepoBadgeProps) => {
    const { styles, cx } = useStyles()
    const kind = iconKind(type)
    const Icon = kind === 'local' ? HddOutlined : kind === 'git' ? BranchesOutlined : DatabaseOutlined
    return (
        <span className={cx(styles.badge, className)} title={name}>
            <Icon data-testid={`repo-badge-${kind}`} />
            <span className={styles.name}>{name}</span>
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
