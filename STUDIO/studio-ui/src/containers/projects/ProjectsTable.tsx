import { useTranslation } from 'react-i18next'
import { Tooltip, Typography } from 'antd'
import { CaretDownFilled, CaretUpFilled, LockOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import type { Project } from '../../types/projects'
import type { RepositoryInfo } from '../../types/repositories'
import { RowCompileDot } from './CompileIndicator'
import { StatusMark } from './StatusIndicator'
import { ProjectRowActions, type ProjectListHandlers, type RowActionId } from './ProjectRowActions'
import { deriveProjectRow, activateOnKey, hasBranch, ProjectTags, showsBranch } from './projectRow'
import { BranchSwitcher } from './BranchSwitcher'
import { useSharedStyles } from './sharedStyles'
import type { ProjectStatusUpdate } from '../../services/projectStatus'
import type { ProjectSort, SortDirection } from './projectListing'

/** Upper bound for the auto-sized Modified column so an unusually long author name can't crowd out the name. */
const MODIFIED_MAX_WIDTH = 260

const useStyles = createStyles(({ css, token }) => ({
    table: css`
        table-layout: auto;
    `,
    /* Modified and actions shrink to their content; the name column (no width) absorbs the rest. */
    branchCell: css`
        max-width: 280px;
        overflow: hidden;
        vertical-align: middle !important;
    `,
    fit: css`
        width: 1px;
        white-space: nowrap;
    `,
    /** A header is a plain button styled as the header text, with the sort arrow beside it when active. */
    sortHeader: css`
        display: inline-flex;
        align-items: center;
        gap: 4px;
        padding: 0;
        border: 0;
        background: transparent;
        color: inherit;
        cursor: pointer;
        font: inherit;
        text-transform: inherit;
        letter-spacing: inherit;

        &:hover {
            color: ${token.colorText};
        }

        .anticon {
            font-size: 10px;
        }
    `,
    row: css`
        cursor: pointer;
        transition: background 0.15s ease;

        td {
            vertical-align: top;
        }

        &:focus-visible {
            outline: 2px solid ${token.colorPrimaryBorder};
            outline-offset: -2px;
        }

        @media (prefers-reduced-motion: reduce) {
            transition: none;
        }
    `,
    projectText: css`
        min-width: 0;
    `,
    nameRow: css`
        display: flex;
        align-items: center;
        gap: 8px;
        min-width: 0;
    `,
    name: css`
        min-width: 0;
        font-weight: 600;
    `,
    nameMuted: css`
        text-decoration: line-through;
        color: ${token.colorTextTertiary};
        font-weight: 500;
    `,
    lock: css`
        flex: none;
        color: ${token.colorWarning};
    `,
    subRow: css`
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        gap: 4px 8px;
        margin-top: 4px;
    `,
    modAuthor: css`
        max-width: ${MODIFIED_MAX_WIDTH}px;
        font-size: 13px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    `,
    modDate: css`
        color: ${token.colorTextTertiary};
        font-size: 12px;
        white-space: nowrap;
    `,
    actionsCell: css`
        text-align: right;
    `,
    actionsWrap: css`
        display: flex;
        align-items: center;
        justify-content: flex-end;
    `,
}))

interface ProjectsTableProps {
    projects: Project[]
    /** Reloads the list after a row switched its project to another branch. */
    onChanged: () => void
    repoInfoOf: (project: Project) => RepositoryInfo
    handlers: ProjectListHandlers
    onOpen: (project: Project) => void
    compileStatusByProject: Map<string, ProjectStatusUpdate>
    /** The action running on each project, keyed by project id. */
    pending: Record<string, RowActionId>
    /** The column the list is sorted by, or null before the user sorted — no arrow shows then. */
    sort: ProjectSort | null
    direction: SortDirection
    /** A header was clicked: sort by that column, or flip the direction of the current one. */
    onSort: (column: ProjectSort) => void
}

/**
 * The projects list as a borderless table (Project / Modified / actions), each row navigating to the
 * project workspace. The compilation status sits next to the project name. The layout is content-driven:
 * the actions and modified columns shrink to fit their content (the modified column capped), and the
 * project name takes all remaining width. A hand-rolled table keeps rows fast in jsdom and matches the
 * mockup's underlined-row styling.
 */
export const ProjectsTable = ({
    projects,
    onChanged,
    repoInfoOf,
    handlers,
    onOpen,
    compileStatusByProject,
    pending,
    sort,
    direction,
    onSort,
}: ProjectsTableProps) => {
    const { t } = useTranslation('repository')
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    // Nothing to switch and nothing to name: a page of projects that all live without branches drops the
    // column instead of showing an empty one.
    const showBranches = projects.some(project => hasBranch(project, repoInfoOf(project)))

    // A header is the sort control of its column: the arrow only appears once the user sorted by it.
    const ascending = direction === 'asc'
    const ariaSort = ascending ? ('ascending' as const) : ('descending' as const)
    const arrow = ascending ? <CaretUpFilled /> : <CaretDownFilled />
    const sortHeader = (column: ProjectSort, label: string) => (
        <button
            aria-sort={sort === column ? ariaSort : undefined}
            className={styles.sortHeader}
            data-testid={`projects-sort-${column}`}
            onClick={() => onSort(column)}
            type="button"
        >
            {label}
            {sort === column && arrow}
        </button>
    )

    return (
        <table className={cx(shared.listTable, styles.table)} data-testid="projects-table">
            <thead className={cx(shared.listHead, shared.microLabel)}>
                <tr>
                    <th>{sortHeader('name', t('home.col_project'))}</th>
                    {showBranches && <th className={styles.fit}>{sortHeader('branch', t('home.col_branch'))}</th>}
                    <th className={styles.fit}>{sortHeader('updated', t('home.col_modified'))}</th>
                    <th aria-label={t('home.row_actions')} className={styles.fit} />
                </tr>
            </thead>
            <tbody>
                {projects.map(project => {
                    const { muted, supportsBranches, lockLabel, tags, date } = deriveProjectRow(project, repoInfoOf, t)
                    const pendingActionId = pending[project.id] ?? null
                    return (
                        <tr
                            key={project.id}
                            aria-label={project.name}
                            className={cx(shared.listRow, styles.row)}
                            data-testid={`project-row-${project.id}`}
                            onClick={() => onOpen(project)}
                            onKeyDown={activateOnKey(() => onOpen(project))}
                            tabIndex={0}
                        >
                            <td>
                                <div className={styles.projectText}>
                                    <div className={styles.nameRow}>
                                        <StatusMark status={project.status} testId={`status-${project.id}`} />
                                        <Typography.Text
                                            className={cx(styles.name, muted && styles.nameMuted)}
                                            ellipsis={{ tooltip: project.name }}
                                        >
                                            {project.name}
                                        </Typography.Text>
                                        {lockLabel && (
                                            <Tooltip title={lockLabel}>
                                                <LockOutlined aria-label={lockLabel} className={styles.lock} />
                                            </Tooltip>
                                        )}
                                        <RowCompileDot
                                            compileStatus={compileStatusByProject.get(project.id)}
                                            status={project.status}
                                        />
                                    </div>
                                    <div className={styles.subRow}>
                                        <ProjectTags tags={tags} />
                                    </div>
                                </div>
                            </td>
                            {/* Switching a branch is a row action of its own: it must not open the project. */}
                            {showBranches && (
                                <td className={cx(styles.fit, styles.branchCell)} onClick={event => event.stopPropagation()}>
                                    {showsBranch(project, supportsBranches) && (
                                        <BranchSwitcher
                                            currentBranch={project.branch}
                                            currentBranchDefault={project.branchDefault}
                                            currentBranchProtected={project.branchProtected}
                                            data-testid={`row-branch-${project.id}`}
                                            onSwitched={onChanged}
                                            projectId={project.id}
                                            selectedBranches={project.selectedBranches ?? []}
                                        />
                                    )}
                                </td>
                            )}
                            <td className={styles.fit}>
                                <div className={styles.modAuthor}>{project.modifiedBy || '—'}</div>
                                {date && <div className={styles.modDate}>{date}</div>}
                            </td>
                            <td className={cx(styles.fit, styles.actionsCell)}>
                                <div className={styles.actionsWrap}>
                                    <ProjectRowActions handlers={handlers} pendingActionId={pendingActionId} project={project} />
                                </div>
                            </td>
                        </tr>
                    )
                })}
            </tbody>
        </table>
    )
}
