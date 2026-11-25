import { useTranslation } from 'react-i18next'
import { Tooltip, Typography } from 'antd'
import { CaretDownFilled, CaretUpFilled, LockOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import type { Project } from '../../types/projects'
import type { RepositoryInfo } from '../../types/repositories'
import { RowCompileDot } from './CompileIndicator'
import { StatusMark } from './StatusIndicator'
import { ProjectRowActions, type ProjectListHandlers } from './ProjectRowActions'
import type { RowBusyId } from './projectActions'
import { deriveProjectRow, activateOnKey, hasBranch, ProjectBranchSwitch } from './projectRow'
import { useSharedStyles } from './sharedStyles'
import type { ProjectStatusUpdate } from '../../services/projectStatus'
import type { ProjectSort, SortDirection } from './projectListing'
import { useDateFormats } from '../../utils/dateFormat'

const useStyles = createStyles(({ css, token }) => ({
    table: css`
        table-layout: auto;
    `,
    /* Author, Modified at and actions shrink to their content; the name column (no width) absorbs the rest. */
    branchCell: css`
        max-width: 280px;
        overflow: hidden;
        font-size: 12px;

        /* A box of its own rather than a word on the line, which would sit it on the text baseline above the centre. */
        & > * {
            display: flex;
        }
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

        /* Every value sits on one line, centred on the row the action buttons set the height of. */
        && td {
            padding-block: ${token.paddingXXS}px;
            vertical-align: middle;
        }

        &:focus-visible {
            outline: 2px solid ${token.colorPrimaryBorder};
            outline-offset: -2px;
        }

        @media (prefers-reduced-motion: reduce) {
            transition: none;
        }
    `,
    /*
     * The name asks the table for no width of its own, or the column would never be narrower than the whole name and
     * its ellipsis would never show: it takes what the other columns leave, down to its minimum.
     */
    nameRow: css`
        contain: inline-size;
        display: flex;
        align-items: center;
        gap: 8px;
        min-width: 200px;
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
    /* Capped, so an unusually long author name can't crowd out the project name. */
    author: css`
        max-width: 260px;
        font-size: 13px;
    `,
    date: css`
        color: ${token.colorTextTertiary};
        font-size: 12px;
        white-space: nowrap;
    `,
}))

interface ProjectsTableProps {
    projects: Project[]
    /**
     * Reloads the list after a row switched its project to another branch. The promise it returns says
     * when the reloaded list is on screen, so the row stays busy until then.
     */
    onChanged: () => void | Promise<unknown>
    /** Whether a branch switch is running on a project, so its row can be gated like any other action. */
    onBranchSwitching?: ((project: Project, busy: boolean) => void) | undefined
    repoInfoOf: (project: Project) => RepositoryInfo
    handlers: ProjectListHandlers
    onOpen: (project: Project) => void
    compileStatusByProject: Map<string, ProjectStatusUpdate>
    /** What each project is busy with, keyed by project id. */
    pending: Record<string, RowBusyId>
    /** The column the list is sorted by, or null before the user sorted — no arrow shows then. */
    sort: ProjectSort | null
    direction: SortDirection
    /** A header was clicked: sort by that column, or flip the direction of the current one. */
    onSort: (column: ProjectSort) => void
}

/**
 * The projects list as a borderless table (Project / Branch / Author / Modified at / actions), each row
 * navigating to the project workspace. The compilation status sits next to the project name. The layout is
 * content-driven: the other columns shrink to fit their content (the author capped), and the project name takes
 * all remaining width. A hand-rolled table keeps rows fast in jsdom and matches the
 * mockup's underlined-row styling.
 */
export const ProjectsTable = ({
    projects,
    onChanged,
    onBranchSwitching,
    repoInfoOf,
    handlers,
    onOpen,
    compileStatusByProject,
    pending,
    sort,
    direction,
    onSort,
}: ProjectsTableProps) => {
    useDateFormats()
    const { t } = useTranslation('repository')
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    // Nothing to switch and nothing to name: a page of projects that all live without branches drops the
    // column instead of showing an empty one.
    const showBranches = projects.some(project => hasBranch(project, repoInfoOf(project)))
    // The class names of a row are put together once for the page, not once for every row.
    const classes = {
        row: cx(shared.listRow, styles.row),
        name: styles.name,
        mutedName: cx(styles.name, styles.nameMuted),
        branchCell: cx(styles.fit, styles.branchCell),
        author: cx(shared.ellipsis, styles.author),
        date: cx(styles.fit, styles.date),
    }

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
        <table className={cx(shared.listTable, shared.flushTable, styles.table)} data-testid="projects-table">
            <thead className={cx(shared.listHead, shared.microLabel)}>
                <tr>
                    <th>{sortHeader('name', t('home.col_project'))}</th>
                    {showBranches && <th className={styles.fit}>{sortHeader('branch', t('home.col_branch'))}</th>}
                    <th className={styles.fit}>{sortHeader('author', t('home.col_author'))}</th>
                    <th className={styles.fit}>{sortHeader('updated', t('home.col_modified_at'))}</th>
                    <th aria-label={t('home.row_actions')} className={styles.fit} />
                </tr>
            </thead>
            <tbody>
                {projects.map(project => {
                    const { muted, supportsBranches, lockLabel, date } = deriveProjectRow(project, repoInfoOf, t)
                    const pendingActionId = pending[project.id] ?? null
                    return (
                        <tr
                            key={project.id}
                            aria-label={project.name}
                            className={classes.row}
                            data-testid={`project-row-${project.id}`}
                            onClick={() => onOpen(project)}
                            onKeyDown={activateOnKey(() => onOpen(project))}
                            tabIndex={0}
                        >
                            <td>
                                <div className={styles.nameRow}>
                                    <StatusMark status={project.status} testId={`status-${project.id}`} />
                                    <Typography.Text
                                        className={muted ? classes.mutedName : classes.name}
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
                            </td>
                            {/* Switching a branch is a row action of its own: it must not open the project. */}
                            {showBranches && (
                                <td className={classes.branchCell} onClick={event => event.stopPropagation()}>
                                    <ProjectBranchSwitch
                                        busy={pendingActionId !== null}
                                        onSwitched={onChanged}
                                        onSwitching={onBranchSwitching}
                                        project={project}
                                        supportsBranches={supportsBranches}
                                    />
                                </td>
                            )}
                            <td className={styles.fit}>
                                <div className={classes.author}>{project.modifiedBy || '—'}</div>
                            </td>
                            <td className={classes.date}>{date ?? '—'}</td>
                            <td className={styles.fit}>
                                <ProjectRowActions handlers={handlers} pendingActionId={pendingActionId} project={project} />
                            </td>
                        </tr>
                    )
                })}
            </tbody>
        </table>
    )
}
