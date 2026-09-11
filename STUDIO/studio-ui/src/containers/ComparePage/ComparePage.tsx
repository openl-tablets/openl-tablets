import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Alert, Button, Checkbox, Empty, Segmented, Spin, Splitter, Upload } from 'antd'
import {
    ArrowLeftOutlined,
    InboxOutlined,
    MenuFoldOutlined,
    MenuUnfoldOutlined,
    PaperClipOutlined,
} from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router-dom'
import { isApiHttpError } from 'services'
import { errorMessage } from 'utils/errorMessage'
import {
    dropComparison,
    getComparison,
    getComparisonTable,
    getConflictFileStatus,
    startConflictComparison,
    startFileComparison,
    startLocalHistoryComparison,
    startProjectComparison,
} from 'services/compare'
import type { ConflictFileStatus } from 'services/compare'
import type { Comparison, ComparisonTable } from 'types/compare'
import { ComparisonPanes, type DiffView } from './ComparisonPanes'
import { ConflictHead } from './ConflictHead'
import { ConflictTextView } from './ConflictTextView'
import { RevisionPicker, type ProjectComparisonSides } from './RevisionPicker'
import { ComparisonTree } from './ComparisonTree'
import { isFinished, useComparisonProgress } from './useComparisonProgress'
import { useStyles } from './ComparePage.styles'

const ACCEPTED = '.xls,.xlsx,.xlsm'
/** How long a screen that cannot hear the topic waits before asking again, in milliseconds. */
const ASK_AGAIN = 2000
const FILES_TO_COMPARE = 2
const EXCEL_FILE = /\.(xlsx?|xlsm)$/i

/** Two versions of a module, named by the screen that opened the window. */
interface VersionsRequest {
    projectId: string
    moduleName: string | undefined
    first: string
    second: string
}

/** What the window was opened to compare, or null when it opens on the files to pick. */
const versionsRequestOf = (params: URLSearchParams): VersionsRequest | null => {
    const projectId = params.get('projectId')
    const first = params.get('first')
    const second = params.get('second')
    if (!projectId || !first || !second) {
        return null
    }
    return { projectId, moduleName: params.get('module') ?? undefined, first, second }
}

/** The comparison the window was opened for, or null when it opens on something to pick. */
const startOf = (
    versions: VersionsRequest | null,
    projectId: string | null,
    conflict: string | null,
    conflictText: string | null,
    conflictStatus: ConflictFileStatus | null
): (() => Promise<string>) | null => {
    if (versions) {
        return () => startLocalHistoryComparison(versions.projectId, versions.moduleName,
            versions.first, versions.second)
    }
    // A conflicted workbook is compared once what became of it is known: a file one version no longer
    // holds has nothing to be compared with.
    if (projectId && conflict && !conflictText && conflictStatus === 'modified') {
        return () => startConflictComparison(projectId, conflict)
    }
    return null
}

/**
 * Compares two Excel files, in a window of its own.
 *
 * The page has two steps: the files to compare are picked first, and the comparison is shown after
 * that - what the two files hold, element by element, and the two versions of the element the user
 * picks. The step of the files is a click away, so another pair can be compared in the same window.
 *
 * A screen that already knows what to compare opens the window on the comparison itself, naming what
 * it wants in the address: two versions of a module, as Local Changes does. There are no files to
 * pick then, and the comparison starts as the window opens.
 *
 * A window opened for a project alone picks instead of files: which file of the working copy stands
 * against which file of which revision. One opened for a conflicted file of a project shows its two
 * versions: a workbook as a comparison, anything else line by line.
 */
export const ComparePage: React.FC = () => {
    const { t } = useTranslation('compare')
    const { styles } = useStyles()
    const [params] = useSearchParams()
    const versions = useMemo(() => versionsRequestOf(params), [params])
    const projectId = versions ? null : params.get('projectId')
    // A conflicted file of a project is named on its own; a project named without one is a project to
    // pick two files of.
    const conflict = projectId ? params.get('conflict') : null
    // A file that is not a workbook is not compared as one: it reads line by line, in this window.
    const conflictText = conflict && !EXCEL_FILE.test(conflict) ? conflict : null
    const pickedProject = conflict ? null : projectId
    const [sides, setSides] = useState<ProjectComparisonSides | null>(null)
    const [conflictStatus, setConflictStatus] = useState<ConflictFileStatus | null>(null)

    const [files, setFiles] = useState<File[]>([])

    /**
     * Takes the files that were dropped or chosen, up to the two that are compared. The list of them
     * is drawn by this page rather than by the upload control, whose own list is left empty.
     */
    const pick = useCallback((file: File, batch: File[]) => {
        // The control asks once per file of a batch, and hands the whole batch every time.
        if (file === batch[0]) {
            setFiles(current => [...current, ...batch].slice(0, FILES_TO_COMPARE))
        }
        return false
    }, [])
    const [comparisonId, setComparisonId] = useState<string | null>(null)
    const [comparison, setComparison] = useState<Comparison | null>(null)
    /** Counts the times the result was asked for, so that asking again runs the effect that asks. */
    const [attempt, setAttempt] = useState(0)
    const [selectedId, setSelectedId] = useState<string | null>(null)
    const [table, setTable] = useState<ComparisonTable | null>(null)
    const [tableLoading, setTableLoading] = useState(false)
    const [tableError, setTableError] = useState<string | null>(null)
    const [error, setError] = useState<string | null>(null)
    const [starting, setStarting] = useState(false)
    const [showEqualElements, setShowEqualElements] = useState(false)
    const [showEqualRows, setShowEqualRows] = useState(false)
    // Two versions read side by side, as the old page read them, or drawn as one table.
    const [view, setView] = useState<DiffView>('sides')
    // The width the list of elements was last given, and whether it is shown at all. The list is
    // hidden and brought back by a button of its own rather than by the divider, so the control is
    // always in sight. A window that heads the list with both controls starts wider, so that neither
    // of them is shortened before the user has touched the divider.
    const [treeWidth, setTreeWidth] = useState<number | string>(versions || conflict ? '36%' : '30%')
    const [treeHidden, setTreeHidden] = useState(false)

    const progress = useComparisonProgress(comparisonId)
    // The comparison is on screen from the moment it is started; until it answers, its progress is.
    // A window opened for a comparison of its own has nothing else to show, so it is on screen at once.
    const comparing = !!comparisonId || !!versions || (!!conflict && !conflictText)
    const running = comparing && !comparison && !error

    useEffect(() => {
        if (!conflict || !projectId) {
            return undefined
        }
        let cancelled = false
        getConflictFileStatus(projectId, conflict)
            .then(status => {
                if (!cancelled) {
                    setConflictStatus(status)
                }
            })
            .catch(() => {
                // What became of the file is told beside the comparison; without it the comparison stands.
                if (!cancelled) {
                    setConflictStatus('modified')
                }
            })
        return () => {
            cancelled = true
        }
    }, [conflict, projectId])

    // The comparison the window was opened for is started once, however often the effect is run.
    const requested = useRef(false)
    useEffect(() => {
        if (requested.current) {
            return
        }
        if (conflictStatus === 'deleted') {
            // One version no longer holds the file: there is nothing to put the other one against.
            return
        }
        const start = startOf(versions, projectId, conflict, conflictText, conflictStatus)
        if (!start) {
            return
        }
        requested.current = true
        start()
            .then(setComparisonId)
            .catch((failure: unknown) => setError(errorMessage(failure) || t('failed')))
    }, [versions, projectId, conflict, conflictText, conflictStatus, t])

    // The result is read when the comparison says it has finished, and again as soon as the page is
    // listening: a comparison of two small files can be over before then, and what was pushed to the
    // topic by then is not repeated. A comparison still running answers 409, and the topic then says
    // when to ask again.
    useEffect(() => {
        if (!comparisonId) {
            return
        }
        if (isFinished(progress.status) && progress.status !== 'COMPLETED') {
            setError(progress.error ?? t(progress.status === 'INTERRUPTED' ? 'interrupted' : 'failed'))
            return
        }
        if (progress.status && progress.status !== 'COMPLETED') {
            return
        }
        let cancelled = false
        let asking: number | undefined
        getComparison(comparisonId)
            .then(result => {
                if (!cancelled) setComparison(result)
            })
            .catch((failure: unknown) => {
                if (cancelled) {
                    return
                }
                if (isApiHttpError(failure) && failure.status === 409) {
                    // Still running. The topic says when it ends; a screen that is not listening to it -
                    // a connection that never opened - would wait for a word that never comes, so it asks.
                    if (!progress.subscribed) {
                        asking = window.setTimeout(() => setAttempt(count => count + 1), ASK_AGAIN)
                    }
                    return
                }
                setError(errorMessage(failure) || t('failed'))
            })
        return () => {
            cancelled = true
            window.clearTimeout(asking)
        }
    }, [comparisonId, progress.status, progress.error, progress.subscribed, attempt, t])

    // The two sides of an element are read when it is picked, because each of them is a whole table.
    useEffect(() => {
        if (!comparisonId || !selectedId) {
            setTable(null)
            return
        }
        let cancelled = false
        setTableLoading(true)
        setTableError(null)
        getComparisonTable(comparisonId, selectedId)
            .then(result => {
                if (!cancelled) setTable(result)
            })
            .catch((failure: unknown) => {
                if (!cancelled) setTableError(errorMessage(failure) || t('table_failed'))
            })
            .finally(() => {
                if (!cancelled) setTableLoading(false)
            })
        return () => {
            cancelled = true
        }
    }, [comparisonId, selectedId, t])

    // A comparison holds both workbooks parsed, so closing the window puts them down at once instead
    // of leaving them until the session ends.
    useEffect(() => {
        if (!comparisonId) {
            return
        }
        const release = () => {
            void dropComparison(comparisonId).catch(() => {
                // The comparison is released with the session in any case.
            })
        }
        window.addEventListener('pagehide', release)
        return () => window.removeEventListener('pagehide', release)
    }, [comparisonId])

    /** Keeps the width the divider was dragged to, so hiding the list and bringing it back restores it. */
    const rememberTreeWidth = useCallback((width: number | undefined) => {
        if (width !== undefined && width > 0) {
            setTreeWidth(width)
        }
    }, [])

    // Two files of a project are picked; without a project, two files are uploaded.
    const ready = pickedProject ? sides !== null : files.length === FILES_TO_COMPARE

    const compare = useCallback(async () => {
        if (!ready) {
            return
        }
        setStarting(true)
        setError(null)
        try {
            setComparisonId(pickedProject && sides
                ? await startProjectComparison(pickedProject, sides.first, sides.second)
                : await startFileComparison(files[0]!, files[1]!))
        } catch (failure) {
            setError(errorMessage(failure) || t('failed'))
        } finally {
            setStarting(false)
        }
    }, [ready, pickedProject, sides, files, t])

    /** Back to the files, leaving the comparison behind: another pair is compared from here. */
    const pickOtherFiles = useCallback(() => {
        if (comparisonId) {
            void dropComparison(comparisonId).catch(() => {
                // The comparison is released with the session in any case.
            })
        }
        // The files that were compared are let go with it, so the next pair is the pair that is picked.
        setFiles([])
        setComparisonId(null)
        setComparison(null)
        setSelectedId(null)
        setTable(null)
        setTableError(null)
        setError(null)
        setShowEqualRows(false)
    }, [comparisonId])

    // The way back to the files never hides with the list of elements: it heads the list while the
    // list is shown, and joins the control that brings it back when it is not. A window opened for
    // two versions of a module has no files to go back to.
    const back = versions || conflict ? null : (
        <Button
            data-testid="compare-back"
            icon={<ArrowLeftOutlined />}
            onClick={pickOtherFiles}
            type="link"
        >
            {t('back')}
        </Button>
    )

    if (conflict && conflictStatus === 'deleted') {
        return (
            <div className={styles.page}>
                <ConflictHead path={conflict} status={conflictStatus} />
                <div className={styles.center} data-testid="compare-conflict-deleted">
                    <Empty description={t('conflict_deleted')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
                </div>
            </div>
        )
    }

    if (conflictText && projectId) {
        return (
            <div className={styles.page}>
                <ConflictHead path={conflictText} status={conflictStatus} />
                <ConflictTextView path={conflictText} projectId={projectId} />
            </div>
        )
    }

    return (
        <div className={styles.page}>
            {conflict && <ConflictHead path={conflict} status={conflictStatus} />}
            {!comparing && (
                <div className={styles.step}>
                    {pickedProject && <RevisionPicker onChange={setSides} projectId={pickedProject} />}
                    {!pickedProject && (
                        <div className={styles.picker}>
                            <Upload.Dragger
                                multiple
                                accept={ACCEPTED}
                                beforeUpload={pick}
                                data-testid="compare-files"
                                fileList={[]}
                                showUploadList={false}
                            >
                                <p className="ant-upload-drag-icon"><InboxOutlined /></p>
                                <p className="ant-upload-text">{t('select')}</p>
                                <p className="ant-upload-hint">{t('select_hint')}</p>
                            </Upload.Dragger>
                            <ul className={styles.files} data-testid="compare-file-list">
                                {files.map((file, index) => (
                                    <li key={`${index}-${file.name}`} className={styles.file}>
                                        <PaperClipOutlined />
                                        <span className={styles.fileName}>{file.name}</span>
                                        <Button
                                            data-testid="compare-file-clear"
                                            onClick={() => setFiles(rest => rest.filter((_, at) => at !== index))}
                                            size="small"
                                            type="link"
                                        >
                                            {t('clear')}
                                        </Button>
                                    </li>
                                ))}
                            </ul>
                        </div>
                    )}
                    <Checkbox
                        checked={showEqualElements}
                        data-testid="compare-show-equal-elements"
                        onChange={event => setShowEqualElements(event.target.checked)}
                    >
                        {t('show_equal_elements')}
                    </Checkbox>
                    <div>
                        <Button
                            data-testid="compare-start"
                            disabled={!ready}
                            loading={starting}
                            onClick={() => void compare()}
                            type="primary"
                        >
                            {t('compare')}
                        </Button>
                    </div>
                    {error && <Alert showIcon data-testid="compare-error" message={error} type="error" />}
                </div>
            )}
            {comparing && (
                <Splitter className={styles.result} onResize={sizes => rememberTreeWidth(sizes[0])}>
                    <Splitter.Panel
                        max="60%"
                        min={treeHidden ? 0 : 200}
                        resizable={!treeHidden}
                        size={treeHidden ? 0 : treeWidth}
                    >
                        {/* Nothing is drawn in a hidden panel: a control that cannot be seen must not
                            be reachable by the keyboard either. */}
                        {!treeHidden && (
                            <div className={styles.column}>
                                {/* The controls of the comparison head their own column, on the line the
                                two files are named on. */}
                                <div className={styles.head}>
                                    {back}
                                    {/* A window that opens on a comparison of its own has no files to
                                    put this next to, so it is offered here, as the old page offered it. */}
                                    {comparison && !back && (
                                        <Checkbox
                                            checked={showEqualElements}
                                            data-testid="compare-show-equal-elements"
                                            onChange={event => setShowEqualElements(event.target.checked)}
                                        >
                                            {t('show_equal_elements')}
                                        </Checkbox>
                                    )}
                                    {comparison && (
                                        <Checkbox
                                            checked={showEqualRows}
                                            data-testid="compare-show-equal-rows"
                                            onChange={event => setShowEqualRows(event.target.checked)}
                                        >
                                            {t('show_equal_rows')}
                                        </Checkbox>
                                    )}
                                    <Button
                                        className={styles.headAction}
                                        data-testid="compare-tree-hide"
                                        icon={<MenuFoldOutlined />}
                                        onClick={() => setTreeHidden(true)}
                                        size="small"
                                        title={t('hide_tree')}
                                        type="text"
                                    />
                                </div>
                                <div className={styles.body} data-testid="compare-tree">
                                    {error && <Alert showIcon data-testid="compare-error" message={error} type="error" />}
                                    {comparison?.identical && (
                                        <Alert
                                            showIcon
                                            data-testid="compare-identical"
                                            message={t('identical')}
                                            type="info"
                                        />
                                    )}
                                    {running && (
                                        <div className={styles.center}>
                                            <Spin description={t('comparing')} />
                                        </div>
                                    )}
                                    {comparison && (
                                        <ComparisonTree
                                            comparison={comparison}
                                            onSelect={setSelectedId}
                                            showEqualElements={showEqualElements}
                                        />
                                    )}
                                </div>
                            </div>
                        )}
                    </Splitter.Panel>
                    <Splitter.Panel>
                        <ComparisonPanes
                            error={tableError}
                            loading={tableLoading}
                            showEqualRows={showEqualRows}
                            table={table}
                            view={view}
                            leading={treeHidden && (
                                <>
                                    <Button
                                        data-testid="compare-tree-show"
                                        icon={<MenuUnfoldOutlined />}
                                        onClick={() => setTreeHidden(false)}
                                        size="small"
                                        title={t('show_tree')}
                                        type="text"
                                    />
                                    {back}
                                </>
                            )}
                            titles={conflict
                                ? { first: t('their_version'), second: t('your_version') }
                                : undefined}
                            trailing={comparison && (
                                <Segmented<DiffView>
                                    aria-label={t('view_label')}
                                    data-testid="compare-view"
                                    onChange={setView}
                                    size="small"
                                    value={view}
                                    options={[
                                        { label: t('view_sides'), value: 'sides' },
                                        { label: t('view_combined'), value: 'combined' },
                                    ]}
                                />
                            )}
                        />
                    </Splitter.Panel>
                </Splitter>
            )}
        </div>
    )
}

export default ComparePage
