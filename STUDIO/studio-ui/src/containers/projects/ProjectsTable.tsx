import { useTranslation } from 'react-i18next'
import { Tooltip, Typography } from 'antd'
import { LockOutlined, RightOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import type { Project } from '../../types/projects'
import type { RepositoryInfo } from '../../types/repositories'
import { StatusPill } from './StatusIndicator'
import { RepoBadge } from './RepoBadge'
import { RowCompileDot } from './CompileIndicator'
import { ProjectRowActions, type ProjectListHandlers } from './ProjectRowActions'
import { deriveProjectRow, activateOnKey, ProjectTags, BranchLabel } from './projectRow'
import { MOCKUP } from './projectsTheme'
import type { ProjectStatusUpdate } from '../../services/projectStatus'

const useStyles = createStyles(({ css, token }) => ({
    table: css`
        width: 100%;
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

        &:hover .row-chevron,
        &:focus-visible .row-chevron {
            opacity: 1;
            transform: translateX(2px);
        }

        @media (prefers-reduced-motion: reduce) {
            transition: none;
        }
    `,
    projectMain: css`
        display: flex;
        align-items: flex-start;
        gap: 10px;
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
        max-width: 360px;
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
        font-size: 13px;
    `,
    modDate: css`
        color: ${token.colorTextTertiary};
        font-size: 12px;
        white-space: nowrap;
    `,
    compileCell: css`
        text-align: center;
        width: 64px;
    `,
    actionsCell: css`
        width: 76px;
    `,
    actionsWrap: css`
        display: flex;
        align-items: center;
        justify-content: flex-end;
        gap: 2px;
    `,
    chevron: css`
        color: ${token.colorTextQuaternary};
        font-size: 12px;
        opacity: 0.5;
        transition: opacity 0.15s ease, transform 0.15s ease;

        @media (prefers-reduced-motion: reduce) {
            transition: none;
        }
    `,
    hideMd: css`
        @media (max-width: 768px) {
            display: none;
        }
    `,
    hideLg: css`
        @media (max-width: 992px) {
            display: none;
        }
    `,
    hideXl: css`
        @media (max-width: 1200px) {
            display: none;
        }
    `,
}))

interface ProjectsTableProps {
    projects: Project[]
    repoInfoOf: (project: Project) => RepositoryInfo
    handlers: ProjectListHandlers
    onOpen: (project: Project) => void
    compileStatusByProject: Map<string, ProjectStatusUpdate>
}

/**
 * The projects list as a borderless table (Project / Status / Repository / Branch / Modified /
 * Compilation), each row navigating to the project workspace. A hand-rolled table keeps rows fast in
 * jsdom and matches the mockup's underlined-row styling.
 */
export const ProjectsTable = ({
    projects,
    repoInfoOf,
    handlers,
    onOpen,
    compileStatusByProject,
}: ProjectsTableProps) => {
    const { t } = useTranslation('repository')
    const { styles, cx } = useStyles()

    return (
        <table className={styles.table} data-testid="projects-table">
            <thead className={styles.head}>
                <tr>
                    <th>{t('home.col_project')}</th>
                    <th className={styles.hideLg}>{t('home.col_status')}</th>
                    <th className={styles.hideXl}>{t('home.col_repository')}</th>
                    <th className={styles.hideXl}>{t('home.col_branch')}</th>
                    <th className={styles.hideMd}>{t('home.col_modified')}</th>
                    <th className={styles.compileCell}>{t('home.col_compile')}</th>
                    <th aria-label={t('home.row_actions')} />
                </tr>
            </thead>
            <tbody>
                {projects.map(project => {
                    const { muted, repoLabel, repoType, supportsBranches, lockLabel, tags, date } = deriveProjectRow(project, repoInfoOf, t)
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
                                <div className={styles.projectMain}>
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
                                        </div>
                                        <div className={styles.subRow}>
                                            <ProjectTags tags={tags} />
                                        </div>
                                    </div>
                                </div>
                            </td>
                            <td className={styles.hideLg}><StatusPill status={project.status} /></td>
                            <td className={styles.hideXl}><RepoBadge name={repoLabel} type={repoType} /></td>
                            <td className={styles.hideXl}><BranchLabel project={project} supportsBranches={supportsBranches} /></td>
                            <td className={styles.hideMd}>
                                <div className={styles.modAuthor}>{project.modifiedBy || '—'}</div>
                                {date && <div className={styles.modDate}>{date}</div>}
                            </td>
                            <td className={styles.compileCell}>
                                <RowCompileDot
                                    branch={supportsBranches ? project.branch || null : null}
                                    initialStatus={compileStatusByProject.get(project.id)}
                                    projectId={project.id}
                                    status={project.status}
                                />
                            </td>
                            <td className={styles.actionsCell}>
                                <div className={styles.actionsWrap}>
                                    <ProjectRowActions handlers={handlers} project={project} />
                                    <RightOutlined aria-hidden className={cx(styles.chevron, 'row-chevron')} />
                                </div>
                            </td>
                        </tr>
                    )
                })}
            </tbody>
        </table>
    )
}
