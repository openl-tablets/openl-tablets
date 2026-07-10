import { useCallback, useRef, useState } from 'react'
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

type ColumnKey = 'project' | 'modified' | 'compilation'

const DEFAULT_WIDTHS: Record<ColumnKey, number> = { project: 440, modified: 220, compilation: 140 }
const MIN_WIDTH = 96
const ACTIONS_WIDTH = 240
const STORAGE_KEY = 'openl.projects.table.widths'

const readStoredWidths = (): Record<ColumnKey, number> => {
    try {
        const raw = window.localStorage.getItem(STORAGE_KEY)
        return raw ? { ...DEFAULT_WIDTHS, ...JSON.parse(raw) } : { ...DEFAULT_WIDTHS }
    } catch {
        return { ...DEFAULT_WIDTHS }
    }
}

const useStyles = createStyles(({ css, token }) => ({
    table: css`
        table-layout: fixed;
        border-collapse: collapse;
        font-size: 14px;
    `,
    head: css`
        th {
            position: relative;
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
            overflow: hidden;
            text-overflow: ellipsis;
        }

        th:first-of-type {
            padding-left: 16px;
        }
    `,
    resizeHandle: css`
        position: absolute;
        top: 0;
        right: -3px;
        z-index: 1;
        width: 7px;
        height: 100%;
        cursor: col-resize;
        touch-action: none;

        &::after {
            content: '';
            position: absolute;
            top: 6px;
            bottom: 6px;
            left: 3px;
            width: 1px;
            background: ${token.colorBorderSecondary};
        }

        &:hover::after {
            background: ${token.colorPrimaryBorder};
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
    compileCell: css`
        text-align: center;
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
 * The projects list as a borderless table (Project / Modified / Compilation / actions), each row
 * navigating to the project workspace. Column widths are user-resizable and persisted. A hand-rolled
 * table keeps rows fast in jsdom and matches the mockup's underlined-row styling.
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
    const [widths, setWidths] = useState<Record<ColumnKey, number>>(readStoredWidths)
    const drag = useRef<{ key: ColumnKey; startX: number; startWidth: number } | null>(null)

    const startResize = useCallback((key: ColumnKey) => (event: React.PointerEvent) => {
        event.preventDefault()
        drag.current = { key, startX: event.clientX, startWidth: widths[key] }
        const onMove = (move: PointerEvent) => {
            const state = drag.current
            if (!state) {
                return
            }
            const next = Math.max(MIN_WIDTH, state.startWidth + move.clientX - state.startX)
            setWidths(prev => ({ ...prev, [state.key]: next }))
        }
        const onUp = () => {
            drag.current = null
            document.removeEventListener('pointermove', onMove)
            document.removeEventListener('pointerup', onUp)
            setWidths(current => {
                try {
                    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(current))
                } catch {
                    // storage is best-effort; ignore quota/availability errors
                }
                return current
            })
        }
        document.addEventListener('pointermove', onMove)
        document.addEventListener('pointerup', onUp)
    }, [widths])

    const resizeHandle = (key: ColumnKey) => (
        <span
            aria-hidden
            className={styles.resizeHandle}
            data-testid={`projects-table-resize-${key}`}
            onClick={event => event.stopPropagation()}
            onPointerDown={startResize(key)}
        />
    )

    return (
        <table className={styles.table} data-testid="projects-table" style={{ minWidth: '100%' }}>
            <colgroup>
                <col style={{ width: widths.project }} />
                <col style={{ width: widths.modified }} />
                <col style={{ width: widths.compilation }} />
                <col style={{ width: ACTIONS_WIDTH }} />
            </colgroup>
            <thead className={styles.head}>
                <tr>
                    <th>{t('home.col_project')}{resizeHandle('project')}</th>
                    <th>{t('home.col_modified')}{resizeHandle('modified')}</th>
                    <th className={styles.compileCell}>{t('home.col_compile')}{resizeHandle('compilation')}</th>
                    <th aria-label={t('home.row_actions')} />
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
                                    </div>
                                    <div className={styles.subRow}>
                                        <ProjectTags tags={tags} />
                                    </div>
                                </div>
                            </td>
                            <td>
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
