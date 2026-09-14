import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Alert, Button, Empty, Progress, Skeleton, Tooltip } from 'antd'
import { ReloadOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import type { ModuleTable, RawTableView, SummaryTable } from 'types/tables'
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
import { isCompiled } from '../services/projectStatus'
import { moduleRoute, toUrlSafeId } from '../services/projectId'
import { supportsBranches } from '../utils/repositoryFeatures'
import { errorMessage } from '../utils/errorMessage'
import { useLoadGeneration } from '../hooks'
import { useUserStore } from '../store'
import { ProjectStatus } from '../constants/project'
import { WorkspaceHeader } from '../components/WorkspaceHeader'
import { CompileDot, getCompileTooltip } from './projects/CompileIndicator'
import { CompileProblemsPanel } from './projects/CompileProblemsPanel'
import { ValueText } from './projects/ValueText'
import { BranchSwitcher } from './projects/BranchSwitcher'
import { closeProjectDialog, openProjectDialog } from './projects/openProjectDialog'
import { ModuleTablesTree } from './modules/ModuleTablesTree'
import { ModuleActionBar } from './modules/ModuleActionBar'
import { TableDetailsPanel } from './modules/TableDetailsPanel'
import { TableProblems } from './modules/TableProblems'
import { TableSearchModal } from './modules/TableSearchModal'
import { TableEditor } from './modules/TableEditor'
import { TableToolbar } from './modules/TableToolbar'
import { useModuleCompilation } from './modules/useModuleCompilation'
import { useSharedStyles } from './projects/sharedStyles'

const useStyles = createStyles(({ css, token }) => ({
    body: css`
        position: relative;
        flex: 1;
        min-width: 0;
        min-height: 0;
        display: flex;
        flex-direction: column;
    `,
    /** The table and, beside it, what the table says about itself. */
    withDetails: css`
        display: flex;
        flex: 1;
        min-width: 0;
        min-height: 0;
    `,
    main: css`
        display: flex;
        flex: 1;
        flex-direction: column;
        min-width: 0;
        min-height: 0;
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
    const { styles: shared } = useSharedStyles()
    const navigate = useNavigate()
    const { projectId, moduleName = '' } = useParams()
    const [search, setSearch] = useSearchParams()
    // What the extended search was opened with, and whether it stands open at all.
    const [searchFor, setSearchFor] = useState<string | null>(null)

    const [project, setProject] = useState<Project | null>(null)
    const [statusReadAt, setStatusReadAt] = useState(0)
    const [loadError, setLoadError] = useState<string | null>(null)
    const [opening, setOpening] = useState(false)
    const [modules, setModules] = useState<ModuleInfo[]>([])
    const [loaded, setLoaded] = useState<{ project: string, module: string, tables: ModuleTable[] } | null>(null)
    const [table, setTable] = useState<RawTableView | null>(null)
    const [tableError, setTableError] = useState<string | null>(null)
    const [moreLoading, setMoreLoading] = useState(false)
    const [cancelling, setCancelling] = useState(false)
    /** Whether the project has compiled through since this module was opened; see the effects below. */
    const [projectCompiled, setProjectCompiled] = useState(false)
    const [editing, setEditing] = useState(false)
    // The cell a message was raised against, asked for from beside that message.
    const [editCell, setEditCell] = useState<string | null>(null)
    // The table settings the user keeps for themselves, which the Editor has always obeyed.
    const showHeader = useUserStore(state => state.userProfile?.showHeader ?? true)
    const showFormulas = useUserStore(state => state.userProfile?.showFormulas ?? false)
    // Bumped by Refresh, so the module is compiled again and its tables read afresh.
    // What the reader asked to be compiled again, and how many times. A refresh belongs to the module it was
    // pressed on: carried over to the next module, it would rebuild that one from the workbook as well.
    // A reader who asks for the module again may mean either of two things: build it from the workbook
    // afresh, dropping everything compiled before, or simply read what it holds now. A write to a table
    // is the second: the session already lets go of what it compiled, and asking for a rebuild on top of
    // that spends a minute on work nobody asked for, with every read of the module waiting behind it.
    const [reload, setReload] = useState({ module: '', token: 0, rebuild: false })
    const reloadToken = reload.module === moduleName ? reload.token : 0
    const rebuild = reload.module === moduleName && reload.rebuild
    const tableLoads = useLoadGeneration()

    // Only the tables read for the module now open count as this screen's — and a module of another project
    // carrying the same name is another module, whatever it is called.
    const tables = loaded?.module === moduleName && loaded.project === projectId ? loaded.tables : null

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
                // A read that answers puts the last failure behind it: one error while a module compiles
                // would otherwise leave the screen on a dead end until the browser is reloaded.
                setLoadError(null)
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

    // Followed by the id the server issued, not the one the address carries: a link written elsewhere may
    // spell the same project a little differently, and the channel is named after the server's spelling.
    const compilation = useModuleCompilation(
        project?.id ?? '',
        project?.branch ?? null,
        moduleName,
        project?.compileStatus ?? null,
        statusReadAt,
        reloadToken,
        project !== null && !closed,
        rebuild
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
    //
    // The read on its way is remembered, because until it answers there is nothing to say it was made: anything
    // that moves while it is in flight — the project read again beside it, the compilation saying once more that
    // it is done — would otherwise ask for the same module a second time. Two such reads open the same module of
    // the same session at once, and the second takes the compilation the first is waiting on out from under it.
    const reading = useRef<string | null>(null)
    useEffect(() => {
        const asked = `${projectId} ${moduleName} ${reloadToken}`
        if (!projectId || !compilation.ready || reading.current === asked
                || (loaded?.module === moduleName && loaded.project === projectId)) {
            return
        }
        reading.current = asked
        getModuleTables(projectId, moduleName)
            .then(found => setLoaded({ project: projectId, module: moduleName, tables: found }))
            .catch((error: unknown) => setLoadError(errorMessage(error)))
            .finally(() => {
                if (reading.current === asked) {
                    reading.current = null
                }
            })
    }, [projectId, moduleName, compilation.ready, loaded, reloadToken])

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
        navigate(moduleRoute(projectId ?? '', moduleName, (tables[0] as ModuleTable).id), { replace: true })
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
    const refresh = useCallback((rebuilding = true) => {
        setLoaded(null)
        setTableError(null)
        setReload(asked => ({ module: moduleName, token: asked.token + 1, rebuild: rebuilding }))
    }, [moduleName])

    // Opening a revision replaces the workspace copy of the project: the project itself is read again, and the
    // module is compiled from what the revision holds.
    const reopenRevision = useCallback(() => {
        load()
        refresh()
    }, [load, refresh])

    // A table written from this screen — created, copied, or a test generated for one — is opened where it
    // landed. The module is compiled again first: a table is in the list only once the workbook is read again.
    const openWritten = useCallback((written: SummaryTable, module: string) => {
        load()
        refresh(false)
        navigate(moduleRoute(projectId ?? '', module, written.id))
    }, [load, navigate, projectId, refresh])

    // The properties of a table are rows of the table itself, so writing them rewrites it: the module is
    // compiled again and the table drawn afresh, under the id it has once it was written.
    //
    // Compiled again, not built afresh: a write already leaves the session without a compiled module, and
    // asking for a rebuild on top of that reads the whole workspace again for nothing.
    const tableRewritten = useCallback((written: string) => {
        load()
        refresh(false)
        if (written !== selectedId) {
            navigate(moduleRoute(projectId ?? '', moduleName, written), { replace: true })
        }
    }, [load, moduleName, navigate, projectId, refresh, selectedId])

    // A test covering this module's tables may be written in another one, so what covers them is only known
    // once the project's compilation has finished — however it finished.
    //
    // Held once it is true: the status says "compiled" as each module finishes and "compiling" again while the
    // next is built, and a screen that asks the server on this answer would ask again on every flip. Compiling
    // this module afresh is what makes the question open again.
    useEffect(() => {
        setProjectCompiled(false)
    }, [moduleName, reloadToken])

    useEffect(() => {
        if (isCompiled(compilation.state)) {
            setProjectCompiled(true)
        }
    }, [compilation.state])

    // A table that is gone leaves the screen on the module it was written in, which opens on its first table.
    const tableRemoved = useCallback(() => {
        reopenRevision()
        navigate(moduleRoute(projectId ?? '', moduleName), { replace: true })
    }, [moduleName, navigate, projectId, reopenRevision])

    // Another module of the same project opens in the same screen, on its own first table.
    const openModule = useCallback((picked: string) => {
        if (picked !== moduleName) {
            navigate(moduleRoute(projectId ?? '', picked))
        }
    }, [moduleName, navigate, projectId])

    // "Show Header" puts away the rows the table's header takes, which the read names — the header line, a
    // properties section, the service rows of a decision table. Cut once: the screen redraws on every status
    // the compilation pushes, and a table of thousands of rows is not re-cut for each of them.
    const rows = useMemo(
        () => showHeader ? table?.source ?? [] : (table?.source ?? []).slice(table?.headerHeight ?? 0),
        [table, showHeader]
    )

    const openTableById = useCallback((picked: string) => {
        setSearch(params => {
            const next = new URLSearchParams(params)
            next.set('table', picked)
            return next
        })
    }, [setSearch])

    const openTable = useCallback((picked: ModuleTable) => openTableById(picked.id), [openTableById])

    // Editing belongs to the table it started on: opening another one — or another module — leaves it.
    useEffect(() => {
        setEditing(false)
        setEditCell(null)
    }, [moduleName, selectedId])

    // A word in a cell that names another table is a way into it: the same screen when the table is one of
    // this module's, its own module's screen when it lives elsewhere.
    const openUsage = useCallback((usage: { tableId?: string, module?: string, projectId?: string }) => {
        if (!usage.tableId || !usage.module) {
            return
        }
        if (usage.module === moduleName) {
            openTableById(usage.tableId)
            return
        }
        // A table this one uses may be written in a project this one depends on, and is read through that
        // project's own screen — the module it names belongs to it, not to the project being read.
        navigate(moduleRoute(usage.projectId ?? projectId ?? '', usage.module, usage.tableId))
    }, [moduleName, navigate, openTableById, projectId])

    // A table a search found is opened where it is written: this screen when it belongs to the module on it,
    // its own module's screen — of its own project — when it does not.
    const openFound = useCallback((found: ModuleTable) => {
        if (!found.module || found.module === moduleName) {
            openTableById(found.id)
            return
        }
        navigate(moduleRoute(found.projectId ?? projectId ?? '', found.module, found.id))
    }, [moduleName, navigate, openTableById, projectId])

    // Whatever the address names is what is drawn, however it got there — a click, a link, or the Back button.
    //
    // Read once the module's own list names that table, which is the moment there is something to read: a link
    // followed into a project nobody opened is answered with "the project is not opened", and a link carried
    // over from another module names a table this one does not hold. The list arrives when the module is
    // compiled, and the table is read then — so opening the project from this screen draws it, unasked.
    // Read by the identifier the address names rather than by the entry the list hands back: reading the list
    // again builds those entries afresh, and a table would be read a second time for no other reason than
    // that — two reads of the same module of the same session, each of them opening it.
    const listed = selected !== null
    useEffect(() => {
        if (!projectId || selectedId === null || !listed) {
            setTable(null)
            setTableError(null)
            return
        }
        const { generation } = tableLoads.start(false)
        setTable(null)
        setTableError(null)
        // Only the first window of a tall table is drawn; the rest is fetched as the reader asks for it.
        getRawTable(projectId, selectedId, {
            module: moduleName,
            maxRows: TABLE_PAGE_ROWS,
            metaInfo: true,
            // What the band offers to run is what the read says can be run.
            runState: true,
        })
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
    }, [projectId, selectedId, listed, moduleName, tableLoads])

    // The next window of the same table, appended to what is already drawn.
    const showMoreRows = useCallback(() => {
        if (!projectId || selectedId === null || table === null || moreLoading) {
            return
        }
        setMoreLoading(true)
        // The window belongs to the table it continues: picking another one while it is on its way leaves
        // these rows with nothing to be added to, and they are dropped rather than drawn under the new table.
        const { generation } = tableLoads.start(true)
        getRawTable(projectId, selectedId, {
            module: moduleName,
            startRow: table.source.length,
            maxRows: TABLE_PAGE_ROWS,
            metaInfo: true,
        })
            .then(next => setTable(shown => (shown === null || !tableLoads.isLatest(generation) ? shown : {
                ...shown,
                source: [...shown.source, ...next.source],
            })))
            .catch((error: unknown) => {
                if (tableLoads.isLatest(generation)) {
                    setTableError(errorMessage(error))
                }
            })
            .finally(() => setMoreLoading(false))
    }, [projectId, selectedId, moduleName, table, moreLoading, tableLoads])

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
            <div className={shared.workspacePage}>
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
        <>
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
        </>
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
                        <Button icon={<ReloadOutlined />} onClick={() => refresh()} type="primary">
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
        if (selectedId === null || (tables !== null && tables.length === 0)) {
            // A module compiled to nothing has no table to draw, whatever the address still names.
            return (
                <div className={styles.centered}>
                    <Empty data-testid="module-no-table" description={t('browser.module.pick_a_table')} />
                </div>
            )
        }
        // The band of actions belongs to the table that was picked, not to the body being read for it, and it
        // keeps its place while that read is on its way — a band taken away and put back asks the server again
        // for everything it shows.
        const toolbar = selected === null ? null : (
            <TableToolbar
                canWrite={!!project.capabilities?.canWrite}
                moduleName={moduleName}
                onEdit={() => setEditing(true)}
                onRemoved={tableRemoved}
                onWritten={openWritten}
                projectCompiled={projectCompiled}
                projectId={project.id}
                runState={table?.runState}
                table={selected}
            />
        )
        if (!table || selected === null) {
            return (
                <>
                    {toolbar}
                    <div className={styles.canvas}><Skeleton active data-testid="module-table-loading" /></div>
                </>
            )
        }
        const shown = table.source.length
        const total = table.totalRows ?? shown
        return (
            <>
                {toolbar}
                <TableProblems
                    messages={table.messages ?? []}
                    onEditCell={project.capabilities?.canWrite ? setEditCell : undefined}
                />
                <TableEditor
                    canvasClassName={styles.canvas}
                    canWrite={!!project.capabilities?.canWrite}
                    editing={editing}
                    formulas={showFormulas}
                    maxRows={table.source.length}
                    moduleName={moduleName}
                    onEditingChange={setEditing}
                    onOpenedAt={() => setEditCell(null)}
                    onOpenUsage={openUsage}
                    onSaved={tableRewritten}
                    openAt={editCell}
                    projectId={project.id}
                    rows={rows}
                    tableId={selected.id}
                    testId="module-table"
                >
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
                </TableEditor>
            </>
        )
    }

    return (
        <div className={shared.workspacePage} data-testid="module-workspace">
            <div className={shared.workspaceBody}>
                <ModuleTablesTree
                    compiling={!closed && !compilation.ready && compilation.state === 'compiling'}
                    currentModule={moduleName}
                    modules={modules}
                    onExtendedSearch={setSearchFor}
                    onSelectModule={openModule}
                    onSelectTable={openTable}
                    selectedTableId={selected?.id}
                    tables={tables}
                />
                <TableSearchModal
                    initialName={searchFor ?? ''}
                    moduleName={moduleName}
                    onClose={() => setSearchFor(null)}
                    open={searchFor !== null}
                    projectId={projectId ?? ''}
                    onOpen={found => {
                        setSearchFor(null)
                        openFound(found)
                    }}
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
                                onProjectChanged={reopenRevision}
                                onRevisionOpened={reopenRevision}
                                onTableCreated={openWritten}
                                project={project}
                                projectCompiled={projectCompiled}
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
                                        onClick={() => refresh()}
                                        type="text"
                                    />
                                </Tooltip>
                            </>
                        )}
                    />
                    <div className={styles.withDetails}>
                        <div className={styles.main}>{canvas()}</div>
                        {compilation.ready && !closed && (
                            <TableDetailsPanel
                                canWrite={!!project.capabilities?.canWrite}
                                moduleName={moduleName}
                                onOpenTable={openTableById}
                                onSaved={tableRewritten}
                                projectId={project.id}
                                tableId={selectedId}
                            />
                        )}
                    </div>
                    <CompileProblemsPanel
                        project={project}
                        statusReadAt={statusReadAt}
                        supportsBranches={supportsBranches({ features: project.repositoryInfo?.features })}
                    />
                </div>
            </div>
        </div>
    )
}
