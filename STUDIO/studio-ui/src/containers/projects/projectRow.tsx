import type { KeyboardEvent } from 'react'
import type { TFunction } from 'i18next'
import { Tag } from 'antd'
import { createStyles } from 'antd-style'
import { useSharedStyles } from './sharedStyles'
import { ProjectStatus } from '../../constants/project'
import { STATUS_META } from '../../constants/projectStatusMeta'
import { formatDateTime } from '../../utils/dateFormat'
import type { Project } from '../../types/projects'
import type { RepositoryInfo } from '../../types/repositories'
import { supportsBranches } from '../../utils/repositoryFeatures'
import { BranchLabel } from './BranchLabel'
import { BranchSwitcher } from './BranchSwitcher'

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
            ? t('browser.locked_by', {
                by: project.lockInfo.lockedBy,
                at: formatDateTime(project.lockInfo.lockedAt) ?? project.lockInfo.lockedAt,
            })
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
        border-radius: ${token.borderRadiusSM}px;
    `,
    tagMore: css`
        color: ${token.colorTextTertiary};
        font-size: 11px;
    `,
    branch: css`
        max-width: 200px;
        font-size: 12px;
    `,
}))

/** The project's tags as chips, collapsing everything past {@link MAX_TAGS} into a "+N" indicator. */
export const ProjectTags = ({ tags }: { tags: string[] }) => {
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    if (tags.length === 0) {
        return null
    }
    return (
        <>
            {/* One tag per type, so the value itself identifies the chip. */}
            {tags.slice(0, MAX_TAGS).map(value => (
                <Tag key={value} className={cx(shared.chipTag, styles.tag)}>{value}</Tag>
            ))}
            {tags.length > MAX_TAGS && <span className={styles.tagMore}>+{tags.length - MAX_TAGS}</span>}
        </>
    )
}

/** Whether the project is shown on a branch: a local copy and a repository without branches have none. */
export const showsBranch = (project: Project, repositorySupportsBranches: boolean): boolean =>
    repositorySupportsBranches && !!project.branch && project.status !== ProjectStatus.Local

/** {@link showsBranch}, resolving the repository's support from the repository itself. */
export const hasBranch = (project: Project, repository: RepositoryInfo): boolean =>
    showsBranch(project, supportsBranches(repository))

/**
 * The branch control of a project in the list, in either view: the branch it is on, switchable, and
 * blocked while the project is busy with something else. A project shown without a branch renders none.
 *
 * Both views gate it the same way, so the rule lives here rather than once per view.
 */
export const ProjectBranchSwitch = ({ project, supportsBranches, busy, testIdPrefix, onSwitched, onSwitching }: {
    project: Project
    supportsBranches: boolean
    /** Whether the project is occupied by another operation, which this switch waits for. */
    busy: boolean
    /** `row` on a table row, `card` on a grid card. */
    testIdPrefix: 'row' | 'card'
    onSwitched: () => void | Promise<unknown>
    onSwitching?: ((project: Project, busy: boolean) => void) | undefined
}) => {
    if (!showsBranch(project, supportsBranches)) {
        return null
    }
    return (
        <BranchSwitcher
            currentBranch={project.branch}
            currentBranchDefault={project.branchDefault}
            currentBranchProtected={project.branchProtected}
            data-testid={`${testIdPrefix}-branch-${project.id}`}
            disabled={busy}
            onBusyChange={switching => onSwitching?.(project, switching)}
            onSwitched={onSwitched}
            projectId={project.id}
        />
    )
}

/** The project's branch with its marks, or nothing for a project shown without one. */
export const ProjectBranch = ({ project, supportsBranches }: { project: Project, supportsBranches: boolean }) => {
    const { styles } = useStyles()
    if (!showsBranch(project, supportsBranches)) {
        return null
    }
    return (
        <BranchLabel
            withIcon
            className={styles.branch}
            isDefault={project.branchDefault}
            isProtected={project.branchProtected}
            name={project.branch}
            testId="row-branch"
        />
    )
}
