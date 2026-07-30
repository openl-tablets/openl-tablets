import { useTranslation } from 'react-i18next'
import { Tooltip, Typography } from 'antd'
import { LockOutlined, RightOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import type { Project } from '../../types/projects'
import type { RepositoryInfo } from '../../types/repositories'
import { StatusMark } from './StatusIndicator'
import { RepoBadge } from './RepoBadge'
import { RowCompileDot } from './CompileIndicator'
import { ProjectRowActions, type ProjectListHandlers, type RowActionId } from './ProjectRowActions'
import { deriveProjectRow, activateOnKey, ProjectTags, ProjectBranch } from './projectRow'
import type { ProjectStatusUpdate } from '../../services/projectStatus'

const useStyles = createStyles(({ css, token }) => ({
    grid: css`
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
        gap: 12px;
    `,
    card: css`
        display: flex;
        flex-direction: column;
        padding: 16px;
        border: 1px solid ${token.colorBorderSecondary};
        border-radius: ${token.borderRadiusLG}px;
        background: ${token.colorBgContainer};
        cursor: pointer;
        transition: border-color 0.15s ease, background 0.15s ease;

        &:hover {
            border-color: ${token.colorPrimaryBorder};
            background: ${token.colorFillQuaternary};
        }

        &:focus-visible {
            outline: 2px solid ${token.colorPrimaryBorder};
            outline-offset: -2px;
        }

        &:hover .card-chevron {
            opacity: 1;
            transform: translateX(2px);
        }
    `,
    head: css`
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 8px;
    `,
    title: css`
        display: flex;
        align-items: center;
        gap: 8px;
        min-width: 0;
        /* The name keeps the height of the actions button, so the header band never shifts when a card
           carries no actions. */
        min-height: ${token.controlHeight}px;
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
    headRight: css`
        display: flex;
        align-items: center;
        gap: 2px;
        flex: none;
    `,
    status: css`
        margin-top: 12px;
    `,
    tags: css`
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        gap: 4px;
        margin-top: 12px;
    `,
    footer: css`
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 8px;
        margin-top: 12px;
        padding-top: 12px;
        border-top: 1px solid ${token.colorFillQuaternary};
    `,
    meta: css`
        display: flex;
        align-items: flex-end;
        justify-content: space-between;
        gap: 8px;
        margin-top: 12px;
    `,
    modAuthor: css`
        font-size: 13px;
    `,
    modDate: css`
        color: ${token.colorTextTertiary};
        font-size: 12px;
    `,
    /** The card repository: compact and muted, matching the branch beside it. */
    repoMeta: css`
        font-size: 12px;
        color: ${token.colorTextTertiary};
    `,
    chevron: css`
        flex: none;
        color: ${token.colorTextQuaternary};
        font-size: 12px;
        opacity: 0.5;
        transition: opacity 0.15s ease, transform 0.15s ease;
    `,
}))

interface ProjectsGridProps {
    projects: Project[]
    repoInfoOf: (project: Project) => RepositoryInfo
    handlers: ProjectListHandlers
    onOpen: (project: Project) => void
    compileStatusByProject: Map<string, ProjectStatusUpdate>
    /** The action running on each project, keyed by project id. */
    pending: Record<string, RowActionId>
}

/** Card-grid view of the projects list, mirroring the table's data with the same row actions. */
export const ProjectsGrid = ({ projects, repoInfoOf, handlers, onOpen, compileStatusByProject, pending }: ProjectsGridProps) => {
    const { t } = useTranslation('repository')
    const { styles, cx } = useStyles()

    return (
        <div className={styles.grid} data-testid="projects-grid">
            {projects.map(project => {
                const { muted, repoLabel, repoType, supportsBranches, lockLabel, tags, date } = deriveProjectRow(project, repoInfoOf, t)
                const pendingActionId = pending[project.id] ?? null
                return (
                    <div
                        key={project.id}
                        aria-label={project.name}
                        className={styles.card}
                        data-testid={`project-card-${project.id}`}
                        onClick={() => onOpen(project)}
                        onKeyDown={activateOnKey(() => onOpen(project))}
                        role="button"
                        tabIndex={0}
                    >
                        <div className={styles.head}>
                            <div className={styles.title}>
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
                            <div className={styles.headRight}>
                                <ProjectRowActions handlers={handlers} layout="menu" pendingActionId={pendingActionId} project={project} />
                            </div>
                        </div>
                        <div className={styles.tags}>
                            <ProjectTags tags={tags} />
                        </div>
                        <div className={styles.footer}>
                            <RepoBadge className={styles.repoMeta} name={repoLabel} type={repoType} />
                            <ProjectBranch project={project} supportsBranches={supportsBranches} />
                        </div>
                        <div className={styles.meta}>
                            <div>
                                <div className={styles.modAuthor}>{project.modifiedBy || '—'}</div>
                                {date && <div className={styles.modDate}>{date}</div>}
                            </div>
                            <RightOutlined aria-hidden className={cx(styles.chevron, 'card-chevron')} />
                        </div>
                    </div>
                )
            })}
        </div>
    )
}
