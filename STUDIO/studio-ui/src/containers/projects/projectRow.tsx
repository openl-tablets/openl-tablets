import type { KeyboardEvent } from 'react'
import { useTranslation } from 'react-i18next'
import type { TFunction } from 'i18next'
import { Tag, Tooltip } from 'antd'
import { BranchesOutlined, SafetyOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { ProjectStatus } from '../../constants/project'
import { STATUS_META } from '../../constants/projectStatusMeta'
import { formatDateTime } from '../../utils/dateFormat'
import type { Project } from '../../types/projects'
import type { RepositoryInfo } from '../../types/repositories'
import { supportsBranches } from '../../utils/repositoryFeatures'
import { MonoChip } from './MonoChip'

/** Tags shown inline on a project row before the rest collapse into a "+N" chip. */
const MAX_TAGS = 4

export interface ProjectRowData {
    muted: boolean
    repoLabel: string
    repoType: string | undefined
    supportsBranches: boolean
    lockLabel: string | undefined
    tags: string[]
    date: string | null
}

/** Derive the per-row display values shared by the table and grid views. */
export function deriveProjectRow(project: Project, repoInfoOf: (project: Project) => RepositoryInfo, t: TFunction): ProjectRowData {
    const repository = repoInfoOf(project)
    return {
        muted: !!STATUS_META[project.status]?.muted,
        repoLabel: repository.name,
        repoType: repository.type,
        supportsBranches: supportsBranches(repository),
        lockLabel: project.lockInfo
            ? t('browser.locked_by', { by: project.lockInfo.lockedBy, at: project.lockInfo.lockedAt })
            : undefined,
        tags: Object.values(project.tags ?? {}),
        date: formatDateTime(project.modifiedAt),
    }
}

/** Activate a non-button row/card from the keyboard: Enter or Space run the handler. */
export const activateOnKey = (onActivate: () => void) => (event: KeyboardEvent) => {
    if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault()
        onActivate()
    }
}

const useStyles = createStyles(({ css, token }) => ({
    tag: css`
        margin: 0;
        border-radius: ${token.borderRadiusSM}px;
        font-size: 11px;
        line-height: 18px;
    `,
    tagMore: css`
        color: ${token.colorTextTertiary};
        font-size: 11px;
    `,
    branch: css`
        display: inline-flex;
        align-items: center;
        gap: 6px;
        min-width: 0;
        max-width: 200px;
        color: ${token.colorTextTertiary};

        .anticon {
            flex: none;
            font-size: 13px;
        }

        .anticon-safety {
            color: ${token.colorInfo};
        }
    `,
    dash: css`
        color: ${token.colorTextTertiary};
    `,
}))

/** The project's tags as chips, collapsing everything past {@link MAX_TAGS} into a "+N" indicator. */
export const ProjectTags = ({ tags }: { tags: string[] }) => {
    const { styles } = useStyles()
    if (tags.length === 0) {
        return null
    }
    return (
        <>
            {tags.slice(0, MAX_TAGS).map((value, index) => (
                <Tag key={index} className={styles.tag}>{value}</Tag>
            ))}
            {tags.length > MAX_TAGS && <span className={styles.tagMore}>+{tags.length - MAX_TAGS}</span>}
        </>
    )
}

/** The project's branch with a protected-branch shield, or an em dash for a project shown without one. */
export const BranchLabel = ({ project, supportsBranches }: { project: Project, supportsBranches: boolean }) => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()
    const showBranch = supportsBranches && !!project.branch && project.status !== ProjectStatus.Local
    if (!showBranch) {
        return <span className={styles.dash}>—</span>
    }
    return (
        <span className={styles.branch}>
            <BranchesOutlined />
            <MonoChip ellipsis>{project.branch}</MonoChip>
            {project.branchProtected && (
                <Tooltip title={t('browser.branch.protected_tag')}>
                    <SafetyOutlined />
                </Tooltip>
            )}
        </span>
    )
}
