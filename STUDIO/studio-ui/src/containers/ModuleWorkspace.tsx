import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Alert, Button, Empty, Progress, Skeleton, Tooltip } from 'antd'
import { ReloadOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import type { ModuleTable, RawTableView } from 'types/tables'
import type { Project } from '../types/projects'
import { getProject, setProjectStatus } from '../services/repositories'
import {
    cancelModuleCompilation,
    getModuleTables,
    getRawTable,
    listModules,
    TABLE_PAGE_ROWS,
    type ModuleInfo,
} from '../services/modules'
import { LOCAL_LOAD_API_OPTIONS } from '../services/apiCall'
import { toUrlSafeId } from '../services/projectId'
import { supportsBranches } from '../utils/repositoryFeatures'
import { errorMessage } from '../utils/errorMessage'
import { useLoadGeneration } from '../hooks'
import { useUserStore } from '../store'
import { ProjectStatus } from '../constants/project'
import { RawTableGrid } from '../components/RawTableGrid'
import { WorkspaceHeader } from '../components/WorkspaceHeader'
import { CompileDot, getCompileTooltip } from './projects/CompileIndicator'
import { CompileProblemsPanel } from './projects/CompileProblemsPanel'
import { ValueText } from './projects/ValueText'
import { BranchSwitcher } from './projects/BranchSwitcher'
import { closeProjectDialog, openProjectDialog } from './projects/openProjectDialog'
import { ModuleTablesTree } from './modules/ModuleTablesTree'
import { ModuleActionBar } from './modules/ModuleActionBar'
import { TableProblems } from './modules/TableProblems'
import { TableToolbar } from './modules/TableToolbar'
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
        gap: 6px;
        color: ${token.colorTextTertiary};

        a {
            color: ${token.colorTextSecondary};

            &:hover {
                color: ${token.colorPrimary};
            }
        }
    `,
    /** A breadcrumb value (the repository): reads like the links beside it. */
    crumbValue: css`
        color: ${token.colorTextSecondary};
    `,
    /**
     * The table is drawn at the width its own text needs, and the canvas scrolls around it.
     *
     * The canvas is the grey of the Projects page, so the table reads as a sheet laid on it rather than as part
     * of the page — its own white cells stay its own.
     */
    canvas: css`
        flex: 1;
        min-width: 0;
        min-height: 0;
        overflow: auto;
        padding: 16px;
        background: ${token.colorBgLayout};
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
    /** The invitation to read further, under the rows already drawn. */
    more: css`
        display: flex;
        justify-content: center;
        padding: 12px 0;
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
    const [search, setSearch] = useSearchParams()

    const [project, setProject] = useState<Project | null>(null)
    const [statusReadAt, setStatusReadAt] = useState(0)
    const [loadError, setLoadError] = useState<string | null>(null)
    const [opening, setOpening] = useState(false)
    const [modules, setModules] = useState<ModuleInfo[]>([])
    const [loaded, setLoaded] = useState<{ module: string, tables: ModuleTable[] } | null>(null)
    const [table, setTable] = useState<RawTableView | null>(null)
    const [tableError, setTableError] = useState<string | null>(null)
    const [moreLoading, setMoreLoading] = useState(false)
    const [cancelling, setCancelling] = useState(false)
    // The table settings the user keeps for themselves, which the Editor has always obeyed.
    const showHeader = useUserStore(state => state.userProfile?.showHeader ?? true)
    const showFormulas = useUserStore(state => state.userProfile?.showFormulas ?? false)
    // Bumped by Refresh, so the module is compiled again and its tables read afresh.
    const [reloadToken, setReloadToken] = useState(0)
    const tableLoads = useLoadGeneration()

    // Only the tables read for the module now open count as this screen's.
    const tables = loaded?.module === moduleName ? loaded.tables : null

    // The table on screen rides in the address, so a link to it opens it again, Back steps between tables, and
    // a refresh keeps the reader where they were.
    const selectedId = search.get('table')
    const selected = useMemo(
        () => (tables ?? []).find(candidate => candidate.id === selectedId) ?? null,
        [tables, selectedId]
    )

    const load = useCallback(() => {
        if (!projectId) {
            return
        }
        const startedAt = Date.now()
        getProject(projectId, { includes: ['status']}, LOCAL_LOAD_API_OPTIONS)
            .then(loaded => {
                setProject(loaded)
                setStatusReadAt(startedAt)
            })
            .catch((error: unknown) => setLoadError(errorMessage(error)))
    }, [projectId])

    useEffect(load, [load])

    // The dialog is mounted above the routes and answers back with this project, so leaving takes its question
    // along.
    useEffect(() => closeProjectDialog, [projectId])

    // A module of a project nobody opened cannot be read: the workspace holds no copy of it to compile. The
    // same question the Projects screen asks is asked here, and the module opens once it is answered.
    const closed = project !== null && project.status === ProjectStatus.Closed
    const openThisProject = useCallback(() => {
        if (!project || !projectId) {
            return
        }
        openProjectDialog(
            { ...project, dependencies: project.dependencies ?? []},
            openDependencies => {
                setOpening(true)
                setProjectStatus(projectId, 'OPENED', { openDependencies })
                    .then(() => load())
                    .catch((error: unknown) => setLoadError(errorMessage(error)))
                    .finally(() => setOpening(false))
            }
        )
    }, [load, project, projectId])

    const compilation = useModuleCompilation(
        projectId ?? '',
        project?.branch ?? null,
        moduleName,
        project?.compileStatus ?? null,
        statusReadAt,
        reloadToken,
        project !== null && !closed
    )

    // Where the module's own workbook is named, so it can be exported. The descriptor does not always spell it
    // out — a project whose modules are discovered by pattern declares none — so the resolved list is read.
    useEffect(() => {
        if (!projectId || closed) {
            return
        }
        listModules(projectId).then(setModules).catch(() => setModules([]))
    }, [projectId, closed, reloadToken])

    // The tables are read once the module is compiled, so the read answers at once instead of waiting for the
    // compilation to reach it. They are kept under the module they belong to: moving to another module of the
    // same project keeps this screen mounted, and the tables left behind are none of the new module's.
    useEffect(() => {
        if (!projectId || !compilation.ready || loaded?.module === moduleName) {
            return
        }
        getModuleTables(projectId, moduleName)
            .then(found => setLoaded({ module: moduleName, tables: found }))
            .catch((error: unknown) => setLoadError(errorMessage(error)))
    }, [projectId, moduleName, compilation.ready, loaded])

    // A module opens on a table rather than on an empty canvas: the first one the list carries. The same
    // correction moves off a table named in the address that this module does not hold — the one the module
    // left behind was showing.
    useEffect(() => {
        if (tables === null || tables.length === 0) {
            return
        }
        if (selectedId !== null && tables.some(candidate => candidate.id === selectedId)) {
            return
        }
        navigate(
            `/projects/${toUrlSafeId(projectId ?? '')}/modules/${encodeURIComponent(moduleName)}`
            + `?table=${encodeURIComponent((tables[0] as ModuleTable).id)}`,
            { replace: true }
        )
    }, [tables, selectedId, moduleName, navigate, projectId])

    // A compilation of a large project takes minutes, and a reader who no longer wants to wait says so. What
    // was compiled stays readable; Refresh starts it again.
    const cancelCompilation = useCallback(() => {
        if (!projectId) {
            return
        }
        setCancelling(true)
        cancelModuleCompilation(projectId, moduleName)
            .catch((error: unknown) => setLoadError(errorMessage(error)))
            .finally(() => setCancelling(false))
    }, [moduleName, projectId])

    // Refresh compiles the module again and re-reads its tables. What the reader was looking at is kept: the
    // address still names it, and it is drawn again as soon as the tables are back.
    const refresh = useCallback(() => {
        setLoaded(null)
        setTableError(null)
        setReloadToken(token => token + 1)
    }, [])

    // Another module of the same project opens in the same screen, on its own first table.
    const openModule = useCallback((picked: string) => {
        if (picked !== moduleName) {
            navigate(`/projects/${toUrlSafeId(projectId ?? '')}/modules/${encodeURIComponent(picked)}`)
        }
    }, [moduleName, navigate, projectId])

    const openTable = useCallback((picked: ModuleTable) => {
        setSearch(params => {
            const next = new URLSearchParams(params)
            next.set('table', picked.id)
            return next
        })
    }, [setSearch])

    // Whatever the address names is what is drawn, however it got there — a click, a link, or the Back button.
    useEffect(() => {
        if (!projectId || selectedId === null) {
            setTable(null)
            return
        }
        const { generation } = tableLoads.start(false)
        setTable(null)
        setTableError(null)
        // Only the first window of a tall table is drawn; the rest is fetched as the reader asks for it.
        getRawTable(projectId, selectedId, { module: moduleName, maxRows: TABLE_PAGE_ROWS })
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
    }, [projectId, selectedId, moduleName, tableLoads, reloadToken])

    // The next window of the same table, appended to what is already drawn.
    const showMoreRows = useCallback(() => {
        if (!projectId || selectedId === null || table === null || moreLoading) {
            return
        }
        setMoreLoading(true)
        getRawTable(projectId, selectedId, {
            module: moduleName,
            startRow: table.source.length,
            maxRows: TABLE_PAGE_ROWS,
        })
            .then(next => setTable(shown => (shown === null ? next : {
                ...shown,
                source: [...shown.source, ...next.source],
            })))
            .catch((error: unknown) => setTableError(errorMessage(error)))
            .finally(() => setMoreLoading(false))
    }, [projectId, selectedId, moduleName, table, moreLoading])

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

    const modulePath = modules.find(declared => declared.name === moduleName)?.path
    const testCount = compilation.tests
    const hasBranches = supportsBranches({ features: project.repositoryInfo?.features }) && !!project.branch

    const crumbs = (
        <span className={styles.crumb}>
            <Link to="/projects">{t('home.title')}</Link>
            <span aria-hidden>/</span>
            <ValueText className={styles.crumbValue}>
                {project.repositoryInfo?.name ?? project.repository}
            </ValueText>
            {hasBranches && (
                <>
                    <span aria-hidden>/</span>
                    <BranchSwitcher
                        currentBranch={project.branch ?? ''}
                        currentBranchDefault={project.branchDefault}
                        currentBranchProtected={project.branchProtected}
                        data-testid="crumb-branch"
                        disabled={opening}
                        onSwitched={() => load()}
                        projectId={project.id}
                        tone="secondary"
                    />
                </>
            )}
            <span aria-hidden>/</span>
            <Link to={`/projects/${toUrlSafeId(project.id)}`}>{project.name}</Link>
        </span>
    )

    const canvas = () => {
        if (closed) {
            return (
                <div className={styles.centered} data-testid="module-project-closed">
                    <Empty description={t('browser.module.project_closed')}>
                        <Button loading={opening} onClick={openThisProject} type="primary">
                            {t('browser.open')}
                        </Button>
                    </Empty>
                </div>
            )
        }
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
        if (compilation.state === 'cancelled' && !compilation.ready) {
            // The reader asked for the wait to end. What was compiled is kept, and Refresh starts it again.
            return (
                <div className={styles.centered} data-testid="module-compile-cancelled">
                    <Empty description={t('browser.module.compile_cancelled', { module: moduleName })}>
                        <Button icon={<ReloadOutlined />} onClick={refresh} type="primary">
                            {t('browser.module.refresh')}
                        </Button>
                    </Empty>
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
                    <Button data-testid="module-compile-cancel" loading={cancelling} onClick={cancelCompilation}>
                        {t('browser.module.compile_cancel')}
                    </Button>
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
        if (selectedId === null) {
            return (
                <div className={styles.centered}>
                    <Empty data-testid="module-no-table" description={t('browser.module.pick_a_table')} />
                </div>
            )
        }
        if (!table) {
            return <div className={styles.canvas}><Skeleton active data-testid="module-table-loading" /></div>
        }
        // "Show Header" puts away the rows the table's header takes, which the read names — the header
        // line, a properties section, the service rows of a decision table.
        const rows = showHeader ? table.source : table.source.slice(table.headerHeight ?? 0)
        const shown = table.source.length
        const total = table.totalRows ?? shown
        return (
            <>
                {selected !== null && (
                    <TableToolbar
                        moduleName={moduleName}
                        projectCompiled={compilation.total > 0 && compilation.compiled >= compilation.total}
                        projectId={project.id}
                        table={selected}
                    />
                )}
                <TableProblems messages={table.messages ?? []} />
                <div className={styles.canvas}>
                    <RawTableGrid formulas={showFormulas} rows={rows} testId="module-table" />
                    {shown < total && (
                        <div className={styles.more}>
                            <Button
                                data-testid="module-table-more"
                                loading={moreLoading}
                                onClick={showMoreRows}
                            >
                                {t('browser.module.show_more_rows', { shown, total })}
                            </Button>
                        </div>
                    )}
                </div>
            </>
        )
    }

    return (
        <div className={styles.page} data-testid="module-workspace">
            <div className={styles.withTree}>
                <ModuleTablesTree
                    currentModule={moduleName}
                    modules={modules}
                    onSelectModule={openModule}
                    onSelectTable={openTable}
                    selectedTableId={selected?.id}
                    tables={tables}
                />
                <div className={styles.body}>
                    <WorkspaceHeader
                        crumbs={crumbs}
                        testId="module-header"
                        title={moduleName}
                        actions={(
                            <ModuleActionBar
                                disabled={closed}
                                moduleName={moduleName}
                                modulePath={modulePath}
                                project={project}
                                testCount={testCount}
                            />
                        )}
                        titleAfter={(
                            <>
                                <CompileDot
                                    showLabel
                                    state={compilation.state}
                                    testId="module-compile-state"
                                    tooltip={getCompileTooltip(compilation.status, compilation.state, t)}
                                    label={compilation.state === 'compiling' && compilation.total > 0
                                        ? t('browser.module.compile_progress', {
                                            compiled: compilation.compiled,
                                            total: compilation.total,
                                        })
                                        : undefined}
                                />
                                <Tooltip title={t('browser.module.refresh')}>
                                    <Button
                                        aria-label={t('browser.module.refresh')}
                                        data-testid="module-refresh"
                                        disabled={closed}
                                        icon={<ReloadOutlined />}
                                        onClick={refresh}
                                        type="text"
                                    />
                                </Tooltip>
                            </>
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
