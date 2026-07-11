import { useTranslation } from 'react-i18next'
import { Tooltip, Typography } from 'antd'
import { LockOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import type { Project } from '../../types/projects'
import type { RepositoryInfo } from '../../types/repositories'
import { RowCompileDot } from './CompileIndicator'
import { ProjectRowActions, type ProjectListHandlers, type RowActionId } from './ProjectRowActions'
import { deriveProjectRow, activateOnKey, ProjectTags } from './projectRow'
import { MOCKUP } from './projectsTheme'
import type { ProjectStatusUpdate } from '../../services/projectStatus'

/** Upper bound for the auto-sized Modified column so an unusually long author name can't crowd out the name. */
const MODIFIED_MAX_WIDTH = 260

const useStyles = createStyles(({ css, token }) => ({
    table: css`
        width: 100%;
        table-layout: auto;
        border-collapse: collapse;
        font-size: 14px;
    `,
    head: css`
        th {
            padding: 8px 12px;
            border-bottom: 1px solid ${token.colorBorderSecondary};
            color: ${token.colorTextTertiary};
            font-family: ${MOCKUP.fontMono};
            font-size: 11px;
            font-weight: 500;
            letter-spacing: 0.05em;
            text-align: left;
            text-transform: uppercase;
            white-space: nowrap;
        }

        th:first-of-type {
            padding-left: 16px;
        }
    `,
    /* Modified and actions shrink to their content; the name column (no width) absorbs the rest. */
    fit: css`
        width: 1px;
        white-space: nowrap;
    `,
    row: css`
        cursor: pointer;
        transition: background 0.15s ease;

        td {
            padding: 12px;
            border-bottom: 1px solid ${token.colorFillQuaternary};
            vertical-align: top;
        }

        td:first-of-type {
            padding-left: 16px;
        }

        &:hover {
            background: ${token.colorFillQuaternary};
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
    compileSlot: css`
        flex: none;
        margin-left: auto;
        display: flex;
        align-items: center;
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
    repoInfoOf: (project: Project) => RepositoryInfo
    handlers: ProjectListHandlers
    onOpen: (project: Project) => void
    compileStatusByProject: Map<string, ProjectStatusUpdate>
    pending: { projectId: string; actionId: RowActionId } | null
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
    repoInfoOf,
    handlers,
    onOpen,
    compileStatusByProject,
    pending,
}: ProjectsTableProps) => {
    const { t } = useTranslation('repository')
    const { styles, cx } = useStyles()

    return (
        <table className={styles.table} data-testid="projects-table">
            <thead className={styles.head}>
                <tr>
                    <th>{t('home.col_project')}</th>
                    <th className={styles.fit}>{t('home.col_modified')}</th>
                    <th aria-label={t('home.row_actions')} className={styles.fit} />
                </tr>
            </thead>
            <tbody>
                {projects.map(project => {
                    const { muted, supportsBranches, lockLabel, tags, date } = deriveProjectRow(project, repoInfoOf, t)
                    const pendingActionId = pending?.projectId === project.id ? pending.actionId : null
                    return (
                        <tr
                            key={project.id}
                            className={styles.row}
                            data-testid={`project-row-${project.id}`}
                            onClick={() => onOpen(project)}
                            onKeyDown={activateOnKey(() => onOpen(project))}
                            role="button"
                            tabIndex={0}
                        >
                            <td>
                                <div className={styles.projectText}>
                                    <div className={styles.nameRow}>
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
                                        <span className={styles.compileSlot}>
                                            <RowCompileDot
                                                branch={supportsBranches ? project.branch || null : null}
                                                initialStatus={compileStatusByProject.get(project.id)}
                                                projectId={project.id}
                                                status={project.status}
                                            />
                                        </span>
                                    </div>
                                    <div className={styles.subRow}>
                                        <ProjectTags tags={tags} />
                                    </div>
                                </div>
                            </td>
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
