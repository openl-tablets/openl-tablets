import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Alert, Button, Empty, Progress, Skeleton } from 'antd'
import { createStyles } from 'antd-style'
import type { ModuleTable, RawTableView } from 'types/tables'
import type { Project } from '../types/projects'
import { getProject } from '../services/repositories'
import { getModuleTables, getRawTable } from '../services/modules'
import { LOCAL_LOAD_API_OPTIONS } from '../services/apiCall'
import { toUrlSafeId } from '../services/projectId'
import { supportsBranches } from '../utils/repositoryFeatures'
import { errorMessage } from '../utils/errorMessage'
import { useLoadGeneration } from '../hooks'
import { RawTableGrid } from '../components/RawTableGrid'
import { WorkspaceHeader } from '../components/WorkspaceHeader'
import { CompileProblemsPanel } from './projects/CompileProblemsPanel'
import { ValueText } from './projects/ValueText'
import { ModuleTablesTree } from './modules/ModuleTablesTree'
import { ModuleActionBar } from './modules/ModuleActionBar'
import { useModuleCompilation } from './modules/useModuleCompilation'

const useStyles = createStyles(({ css, token }) => ({
    page: css`
        height: calc(100vh - 64px);
        display: flex;
        flex-direction: column;
        overflow: hidden;
        background: ${token.colorBgContainer};
    `,
    withTree: css`
        display: flex;
        flex: 1;
        min-width: 0;
        min-height: 0;
    `,
    body: css`
        position: relative;
        flex: 1;
        min-width: 0;
        min-height: 0;
        display: flex;
        flex-direction: column;
    `,
    crumb: css`
        display: inline-flex;
        align-items: center;
        gap: ${token.marginXXS}px;
        color: ${token.colorTextTertiary};

        a {
            color: ${token.colorTextSecondary};

            &:hover {
                color: ${token.colorPrimary};
            }
        }
    `,
    canvas: css`
        flex: 1;
        min-width: 0;
        min-height: 0;
        overflow: auto;
        padding: 16px;
    `,
    centered: css`
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 12px;
        flex: 1;
        padding: ${token.paddingXL}px;
        text-align: center;
    `,
    progress: css`
        width: 320px;
        max-width: 100%;
    `,
}))

/**
 * One module of a project, opened for reading.
 *
 * The screen wears the project's own head — the project sits in the trail, the module takes the title — so opening
 * a module reads as a step deeper into the project rather than as a second place to be.
 *
 * A module can only be read once it is compiled, and compiling through to it takes minutes on a large project. So
 * nothing is waited for over HTTP: the compilation is asked for, its progress is followed on the project's status
 * channel, and the tables are read the moment this module is named as compiled — while the modules after it go on
 * compiling behind the open screen.
 */
export const ModuleWorkspace = () => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()
    const navigate = useNavigate()
    const { projectId, moduleName = '' } = useParams()

    const [project, setProject] = useState<Project | null>(null)
    const [statusReadAt, setStatusReadAt] = useState(0)
    const [loadError, setLoadError] = useState<string | null>(null)
    const [tables, setTables] = useState<ModuleTable[] | null>(null)
    const [selected, setSelected] = useState<ModuleTable | null>(null)
    const [table, setTable] = useState<RawTableView | null>(null)
    const [tableError, setTableError] = useState<string | null>(null)
    // Bumped by Refresh, so the module is compiled again and its tables read afresh.
    const [reloadToken, setReloadToken] = useState(0)
    const tableLoads = useLoadGeneration()

    useEffect(() => {
        if (!projectId) {
            return
        }
        const startedAt = Date.now()
        getProject(projectId, { includes: ['status', 'descriptor']}, LOCAL_LOAD_API_OPTIONS)
            .then(loaded => {
                setProject(loaded)
                setStatusReadAt(startedAt)
            })
            .catch((error: unknown) => setLoadError(errorMessage(error)))
    }, [projectId])

    const compilation = useModuleCompilation(
        projectId ?? '',
        project?.branch ?? null,
        moduleName,
        project?.compileStatus ?? null,
        statusReadAt,
        reloadToken
    )

    // The tables are read once the module is compiled, so the read answers at once instead of waiting for the
    // compilation to reach it.
    useEffect(() => {
        if (!projectId || !compilation.ready || tables !== null) {
            return
        }
        getModuleTables(projectId, moduleName)
            .then(setTables)
            .catch((error: unknown) => setLoadError(errorMessage(error)))
    }, [projectId, moduleName, compilation.ready, tables])

    const refresh = useCallback(() => {
        setTables(null)
        setSelected(null)
        setTable(null)
        setTableError(null)
        setReloadToken(token => token + 1)
    }, [])

    const openTable = useCallback((picked: ModuleTable) => {
        if (!projectId) {
            return
        }
        const { generation } = tableLoads.start(false)
        setSelected(picked)
        setTable(null)
        setTableError(null)
        getRawTable(projectId, picked.id)
            .then(loaded => {
                if (tableLoads.isLatest(generation)) {
                    setTable(loaded)
                }
            })
            .catch((error: unknown) => {
                if (tableLoads.isLatest(generation)) {
                    setTableError(errorMessage(error))
                }
            })
    }, [projectId, tableLoads])

    if (loadError) {
        return (
            <Alert
                showIcon
                data-testid="module-workspace-error"
                description={loadError}
                title={t('browser.load_error')}
                type="error"
            />
        )
    }

    if (!project) {
        return (
            <div className={styles.page}>
                <div className={styles.centered} data-testid="module-workspace-loading">
                    <Skeleton active />
                </div>
            </div>
        )
    }

    // The workbook the module is written in, so it can be exported.
    const modulePath = project.descriptor?.modules?.find(declared => declared.name === moduleName)?.path

    const crumbs = (
        <span className={styles.crumb}>
            <Link to="/projects">{t('home.title')}</Link>
            <span aria-hidden>/</span>
            <ValueText>{project.repositoryInfo?.name ?? project.repository}</ValueText>
            {project.branch && (
                <>
                    <span aria-hidden>/</span>
                    <ValueText>{project.branch}</ValueText>
                </>
            )}
            <span aria-hidden>/</span>
            <Link to={`/projects/${toUrlSafeId(project.id)}`}>{project.name}</Link>
        </span>
    )

    const canvas = () => {
        if (compilation.failure !== null) {
            return (
                <div className={styles.centered}>
                    <Alert
                        showIcon
                        data-testid="module-compile-failed"
                        description={compilation.failure}
                        title={t('browser.module.compile_failed')}
                        type="error"
                    />
                </div>
            )
        }
        if (!compilation.ready) {
            // Shown while the compilation works towards this module. The count is what the status channel
            // reports, so it moves as each module finishes rather than sitting at nothing.
            return (
                <div className={styles.centered} data-testid="module-compiling">
                    <Progress
                        className={styles.progress}
                        status="active"
                        percent={compilation.total === 0
                            ? 0
                            : Math.round((compilation.compiled / compilation.total) * 100)}
                    />
                    <span>
                        {t('browser.module.compiling', {
                            module: moduleName,
                            compiled: compilation.compiled,
                            total: compilation.total,
                        })}
                    </span>
                </div>
            )
        }
        if (tableError !== null) {
            return (
                <div className={styles.centered}>
                    <Alert showIcon description={tableError} title={t('browser.load_error')} type="error" />
                </div>
            )
        }
        if (!selected) {
            return (
                <div className={styles.centered}>
                    <Empty data-testid="module-no-table" description={t('browser.module.pick_a_table')} />
                </div>
            )
        }
        if (!table) {
            return <div className={styles.canvas}><Skeleton active data-testid="module-table-loading" /></div>
        }
        return (
            <div className={styles.canvas}>
                <RawTableGrid rows={table.source} testId="module-table" />
            </div>
        )
    }

    return (
        <div className={styles.page} data-testid="module-workspace">
            <div className={styles.withTree}>
                <ModuleTablesTree
                    onSelectTable={openTable}
                    selectedTableId={selected?.id}
                    tables={tables ?? []}
                />
                <div className={styles.body}>
                    <WorkspaceHeader
                        crumbs={crumbs}
                        testId="module-header"
                        title={moduleName}
                        actions={(
                            <ModuleActionBar
                                moduleName={moduleName}
                                modulePath={modulePath}
                                onRefresh={refresh}
                                project={project}
                                table={selected}
                            />
                        )}
                    />
                    {canvas()}
                    <CompileProblemsPanel
                        project={project}
                        statusReadAt={statusReadAt}
                        supportsBranches={supportsBranches({ features: project.repositoryInfo?.features })}
                    />
                </div>
            </div>
            {!projectId && (
                <div className={styles.centered}>
                    <Empty description={t('home.not_found')}>
                        <Button onClick={() => navigate('/projects')} type="primary">
                            {t('home.back_to_projects')}
                        </Button>
                    </Empty>
                </div>
            )}
        </div>
    )
}
