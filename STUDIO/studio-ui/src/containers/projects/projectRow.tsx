import type { KeyboardEvent } from 'react'
import type { TFunction } from 'i18next'
import { createStyles } from 'antd-style'
import { ProjectStatus } from '../../constants/project'
import { STATUS_META } from '../../constants/projectStatusMeta'
import { formatDate, formatDateTime, formatDateTimeToMinute } from '../../utils/dateFormat'
import type { Project } from '../../types/projects'
import type { RepositoryInfo } from '../../types/repositories'
import { supportsBranches } from '../../utils/repositoryFeatures'
import { BranchLabel } from './BranchLabel'
import { BranchSwitcher } from './BranchSwitcher'

interface ProjectRowData {
    muted: boolean
    supportsBranches: boolean
    lockLabel: string | undefined
    /** When the project was last modified, down to the minute. */
    date: string | null
    /** The day the project was last modified, without the time, for a list too narrow for it. */
    day: string | null
}

/** Derive the display values of the row of a project in the list. */
export function deriveProjectRow(project: Project, repoInfoOf: (project: Project) => RepositoryInfo, t: TFunction): ProjectRowData {
    const repository = repoInfoOf(project)
    return {
        muted: !!STATUS_META[project.status]?.muted,
        supportsBranches: supportsBranches(repository),
        lockLabel: project.lockInfo
            ? t('browser.locked_by', {
                by: project.lockInfo.lockedBy,
                at: formatDateTime(project.lockInfo.lockedAt) ?? project.lockInfo.lockedAt,
            })
            : undefined,
        date: formatDateTimeToMinute(project.modifiedAt),
        day: project.modifiedAt ? formatDate(project.modifiedAt) : null,
    }
}

/** Activate a non-button table row from the keyboard: Enter or Space run the handler. */
export const activateOnKey = (onActivate: () => void) => (event: KeyboardEvent) => {
    if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault()
        onActivate()
    }
}

const useStyles = createStyles(({ css, token }) => ({
    branch: css`
        max-width: 200px;
        font-size: ${token.fontSizeSM}px;
    `,
}))

/** Whether the project is shown on a branch: a local copy and a repository without branches have none. */
const showsBranch = (project: Project, repositorySupportsBranches: boolean): boolean =>
    repositorySupportsBranches && !!project.branch && project.status !== ProjectStatus.Local

/** {@link showsBranch}, resolving the repository's support from the repository itself. */
export const hasBranch = (project: Project, repository: RepositoryInfo): boolean =>
    showsBranch(project, supportsBranches(repository))

/**
 * The branch control of a project in the list: the branch it is on, switchable, and blocked while the project is
 * busy with something else. A project shown without a branch renders none.
 */
export const ProjectBranchSwitch = ({ project, supportsBranches, busy, onSwitched, onSwitching }: {
    project: Project
    supportsBranches: boolean
    /** Whether the project is occupied by another operation, which this switch waits for. */
    busy: boolean
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
            data-testid={`row-branch-${project.id}`}
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
