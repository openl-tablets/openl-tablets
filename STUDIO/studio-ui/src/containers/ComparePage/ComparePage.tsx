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
import { useSearchParams } from 'react-router'
import { isStillRunning } from 'services/taskResult'
import { errorMessage } from 'utils/errorMessage'
import { isWorkbookPath } from 'utils/workbooks'
import { basename } from 'containers/projects/projectPaths'
import {
    dropComparison,
    getComparison,
    getComparisonTable,
    getConflictFileText,
    getConflictFileVersions,
    startConflictComparison,
    startFileComparison,
    startLocalHistoryComparison,
    startProjectComparison,
} from 'services/compare'
import { getFileText } from 'services/files'
import type { ConflictFileStatus, ConflictFileVersions, ConflictSide } from 'services/compare'
import type { Comparison, ComparisonTable } from 'types/compare'
import { ComparisonPanes, type DiffView } from './ComparisonPanes'
import { ConflictHead } from './ConflictHead'
import { TextVersionsView } from './TextVersionsView'
import { RevisionPicker, type ProjectComparisonSides } from './RevisionPicker'
import { ComparisonTree } from './ComparisonTree'
import { isFinished, useComparisonProgress } from './useComparisonProgress'
import { useStyles } from './ComparePage.styles'

const ACCEPTED = '.xls,.xlsx,.xlsm'
/** How long a screen that cannot hear the topic waits before asking again, in milliseconds. */
const ASK_AGAIN = 2000

const FILES_TO_COMPARE = 2

/**
 * What the comparison of a conflicted file shows: what your branch changed since the base, what their branch changed,
 * or your version against theirs.
 */
type ConflictView = 'yours' | 'theirs' | 'versions'

/** How a view of a conflicted file is offered, and how it compares its two versions. */
interface ConflictViewDefinition {
    label: string
    /** The two versions it compares, the one it starts from first. */
    compared: [ConflictSide, ConflictSide]
    /** Whether the version it compares second is shown on the left. */
    secondOnLeft: boolean
}

/**
 * How each view of a conflicted file compares its versions, in the order offered. The versions stand as merge tools
 * lay them out, your version on the left, theirs on the right and the base between them: what your branch changed
 * shows your version left of the base, though the comparison starts from the base.
 */
const CONFLICT_VIEWS: Record<ConflictView, ConflictViewDefinition> = {
    yours: { label: 'view_your_changes', compared: ['base', 'ours'], secondOnLeft: true },
    theirs: { label: 'view_their_changes', compared: ['base', 'theirs'], secondOnLeft: false },
    versions: { label: 'view_versions', compared: ['ours', 'theirs'], secondOnLeft: false },
}

/** The views of a conflicted file, in the order offered. */
const VIEWS = Object.keys(CONFLICT_VIEWS) as ConflictView[]

/** How each version of a conflicted file is named. */
const VERSION_TITLES: Record<ConflictSide, string> = {
    base: 'base_version',
    ours: 'your_version',
    theirs: 'their_version',
}

/** The keys of the names of the two versions a comparison shows, the first and the second. */
interface TitleKeys {
    first: string
    second: string
}

/** A file at the revision the working copy was taken from against the working copy, as the Save dialog names it. */
const SAVE_TITLES: TitleKeys = { first: 'saved_revision', second: 'your_changes' }

/** A file at a revision against the same file at a later one, such as before and after a merge. */
const REVISION_TITLES: TitleKeys = { first: 'earlier_revision', second: 'later_revision' }

/** A file of the working copy against a file of the revision picked beside it. */
const PICKED_TITLES: TitleKeys = { first: 'user_workspace', second: 'selected_revision' }

/** What is known of a conflicted file whose versions cannot be read: the merge compared their two versions. */
const UNKNOWN_VERSIONS: ConflictFileVersions = { base: false, ours: true, theirs: true }

/** The views of a conflicted file whose two versions both hold it. */
const viewsOf = (versions: ConflictFileVersions): ConflictView[] =>
    VIEWS.filter(view => CONFLICT_VIEWS[view].compared.every(side => versions[side]))

/** The view a conflicted file opens on: what their branch changed, which the merge brings in, when it is offered. */
const firstViewOf = (views: ConflictView[]): ConflictView | null =>
    views.includes('theirs') ? 'theirs' : views[0] ?? null

/**
 * What became of a conflicted file: a file both branches still hold was modified on both sides, one that a branch no
 * longer holds was deleted there.
 */
const conflictFileStatusOf = (versions: ConflictFileVersions): ConflictFileStatus =>
    versions.ours && versions.theirs ? 'modified' : 'deleted'

/** Lets a comparison go, with the workbooks it holds parsed. */
const releaseComparison = (comparisonId: string) => {
    dropComparison(comparisonId).catch(() => {
        // The comparison is released with the session in any case.
    })
}

/** Two versions named by the screen that opened the window, and how their comparison is started. */
interface VersionsRequest {
    start: () => Promise<string>
    /** What the two versions are called, or nothing for a first and a second version. */
    titles?: TitleKeys | undefined
}

/** A file that is not a workbook at two versions: it reads line by line. */
interface TextVersionsRequest {
    path: string
    read: () => Promise<[string, string]>
    /** What the two versions are called, or nothing for a first and a second version. */
    titles?: TitleKeys | undefined
}

/** The versions the window was opened for: of a workbook, or of a file that reads line by line. */
type NamedVersions = Pick<ComparisonRequest, 'versions' | 'textVersions'>

/**
 * The keys of the names of the two versions a comparison shows. The versions of a conflicted file, the versions the
 * screen that opened the window named, and the files picked of a project are named as what they are; only two files
 * picked from the computer are a first and a second file, with no names.
 *
 * @param shownView the view of a conflicted file shown, or null for any other comparison
 * @param named the versions the window was opened for, or null when it was opened for none
 * @param picked whether the files compared were picked of a project
 */
const titleKeysOf = (
    shownView: ConflictViewDefinition | null,
    named: VersionsRequest | TextVersionsRequest | null,
    picked: boolean
): TitleKeys | undefined => {
    if (shownView) {
        return { first: VERSION_TITLES[shownView.compared[0]], second: VERSION_TITLES[shownView.compared[1]] }
    }
    return named?.titles ?? (picked ? PICKED_TITLES : undefined)
}

/** Two versions of a module, as Local Changes names them, or null when none are named. */
const moduleVersionsOf = (params: URLSearchParams): VersionsRequest | null => {
    const projectId = params.get('projectId')
    const first = params.get('first')
    const second = params.get('second')
    if (!projectId || !first || !second) {
        return null
    }
    const moduleName = params.get('module') ?? undefined
    return { start: () => startLocalHistoryComparison(projectId, moduleName, first, second) }
}

/**
 * A file of a project at a revision against the same file later, or null when none is named: at
 * another revision of the branch, as the summary of a merge names it before and after the merge, or
 * in the working copy, as the Save dialog names the changes it commits. A workbook is compared element
 * by element, any other file reads line by line. A revision is a commit of the repository, so it is
 * read whichever branch the project is on. The two versions are named as what they are: the saved revision
 * and your changes, or the earlier revision and the later one.
 */
const fileRevisionsOf = (params: URLSearchParams): NamedVersions | null => {
    const projectId = params.get('projectId')
    const file = params.get('file')
    const before = params.get('before')
    if (!projectId || !file || !before) {
        return null
    }
    // A file named without a later revision is compared with the working copy.
    const after = params.get('after') ?? undefined
    const titles = after ? REVISION_TITLES : SAVE_TITLES
    if (!isWorkbookPath(file)) {
        const read = () => Promise.all([
            getFileText(projectId, file, before),
            getFileText(projectId, file, after),
        ])
        return { versions: null, textVersions: { path: file, read, titles } }
    }
    const branch = params.get('branch') ?? undefined
    const start = () => startProjectComparison(projectId,
        { path: file, branch, revision: before },
        after ? { path: file, branch, revision: after } : { path: file })
    return { versions: { start, titles }, textVersions: null }
}

/**
 * A conflicted file that is not a workbook, read as the view of it compares it: the version it starts from first.
 *
 * @param versionOf reads a version of the file
 */
const conflictTextOf = (
    conflict: string,
    view: ConflictView,
    versionOf: (side: ConflictSide) => Promise<string>
): TextVersionsRequest => {
    const [first, second] = CONFLICT_VIEWS[view].compared
    return { path: conflict, read: () => Promise.all([versionOf(first), versionOf(second)]) }
}

/** What the window was opened to compare, read from the address it was opened with. */
interface ComparisonRequest {
    /** Two versions, when the window was opened for them. */
    versions: VersionsRequest | null
    /** A file that is not a workbook at two revisions, when the window was opened for one. */
    textVersions: TextVersionsRequest | null
    /** The project the window was opened for, whether to pick its files or to read its conflict. */
    projectId: string | null
    /** The conflicted file of that project, when the window was opened for one. */
    conflict: string | null
    /** The project whose two files are picked, or null when there is nothing to pick. */
    pickedProject: string | null
}

/**
 * What the address says the window is for.
 *
 * A window is opened for two versions, for a project whose files are picked, or for a
 * conflicted file of a project. Reading them apart in one place keeps the screen itself to what it
 * draws.
 */
const requestOf = (params: URLSearchParams): ComparisonRequest => {
    const { versions, textVersions } = fileRevisionsOf(params)
        ?? { versions: moduleVersionsOf(params), textVersions: null }
    const projectId = versions || textVersions ? null : params.get('projectId')
    // A conflicted file of a project is named on its own; a project named without one is a project to
    // pick two files of.
    const conflict = projectId ? params.get('conflict') : null
    return {
        versions,
        textVersions,
        projectId,
        conflict,
        pickedProject: conflict ? null : projectId,
    }
}

/** The comparison the window was opened for, or null when it opens on something to pick. */
const startOf = (
    versions: VersionsRequest | null,
    projectId: string | null,
    conflict: string | null,
    conflictView: ConflictView | null
): (() => Promise<string>) | null => {
    if (versions) {
        return versions.start
    }
    // A conflicted workbook is compared once it is known which versions hold it, as the view picked compares it.
    if (projectId && conflict && isWorkbookPath(conflict) && conflictView) {
        const [first, second] = CONFLICT_VIEWS[conflictView].compared
        return () => startConflictComparison(projectId, conflict, first, second)
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
 * it wants in the address: two versions of a module, as Local Changes does, a file at two revisions,
 * as the summary of a merge does, or a file at its saved revision against the working copy, as the
 * Save dialog does. There are no files to pick then, and the comparison starts as the window opens;
 * a file that is not a workbook reads line by line.
 *
 * A window opened for a project alone picks instead of files: which file of the working copy stands
 * against which file of which revision. One opened for a conflicted file of a project shows what each
 * branch changed since the base version, or the two versions against each other: a workbook as a
 * comparison, anything else line by line. A view whose versions do not both hold the file is not offered.
 */
/**
 * What the window knows of a conflicted file: which versions hold it, what became of it, the views it is offered in
 * and the one shown, and its versions read as text when it is not a workbook. Nothing is known without a file.
 *
 * Which versions hold the file is read as the window opens; without what the merge reports, the two versions the
 * merge compared are taken to hold it. The versions stay as they are while the window is open, so each is read once
 * for every view; a version that could not be read is read again when a view asks for it next.
 */
const useConflictFile = (projectId: string | null, conflict: string | null) => {
    // Which versions hold the conflicted file; null while they are being read.
    const [conflictVersions, setConflictVersions] = useState<ConflictFileVersions | null>(null)
    // The view the user picked; until then, the file is shown in the view it opens on.
    const [pickedView, setPickedView] = useState<ConflictView | null>(null)
    const conflictStatus = conflictVersions && conflictFileStatusOf(conflictVersions)
    const conflictViews = useMemo(() => conflictVersions ? viewsOf(conflictVersions) : [], [conflictVersions])
    const conflictView = pickedView ?? firstViewOf(conflictViews)
    const conflictWorkbook = !!conflict && isWorkbookPath(conflict)
    const conflictTexts = useRef(new Map<ConflictSide, Promise<string>>())
    const conflictText = useMemo(() => {
        if (!projectId || !conflict || conflictWorkbook || !conflictView) {
            return null
        }
        const versionOf = (side: ConflictSide) => {
            const known = conflictTexts.current.get(side)
            if (known) {
                return known
            }
            const text = getConflictFileText(projectId, conflict, side)
            conflictTexts.current.set(side, text)
            text.catch(() => conflictTexts.current.delete(side))
            return text
        }
        return conflictTextOf(conflict, conflictView, versionOf)
    }, [projectId, conflict, conflictWorkbook, conflictView])

    useEffect(() => {
        if (!conflict || !projectId) {
            return undefined
        }
        let cancelled = false
        const shown = (found: ConflictFileVersions) => {
            if (!cancelled) {
                setConflictVersions(found)
            }
        }
        getConflictFileVersions(projectId, conflict).then(shown, () => shown(UNKNOWN_VERSIONS))
        return () => {
            cancelled = true
        }
    }, [conflict, projectId])

    return { conflictVersions, conflictStatus, conflictViews, conflictView, setPickedView, conflictWorkbook, conflictText }
}

export const ComparePage: React.FC = () => {
    const { t } = useTranslation('compare')
    const { styles } = useStyles()
    const [params] = useSearchParams()
    const { versions, textVersions, projectId, conflict, pickedProject } = useMemo(
        () => requestOf(params), [params]
    )
    const [sides, setSides] = useState<ProjectComparisonSides | null>(null)
    const { conflictVersions, conflictStatus, conflictViews, conflictView, setPickedView, conflictWorkbook, conflictText } =
        useConflictFile(projectId, conflict)

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

    // A window opened for a file says which file it compares, a workbook or not.
    const comparedFile = conflict ?? textVersions?.path
    useEffect(() => {
        document.title = comparedFile ? t('file_title', { name: basename(comparedFile) }) : t('title')
    }, [comparedFile, t])

    const progress = useComparisonProgress(comparisonId)
    // The comparison is on screen from the moment it is started; until it answers, its progress is.
    // A window opened for a comparison of its own has nothing else to show, so it is on screen at once.
    const comparing = !!comparisonId || !!versions || conflictWorkbook
    const running = comparing && !comparison && !error

    // The comparison the window shows is started once for each view of it, however often the effect is run. Only
    // the start made last is answered on screen, though an earlier one was made for the same view.
    const requested = useRef<{ key: string } | null>(null)
    useEffect(() => {
        const start = startOf(versions, projectId, conflict, conflictView)
        const key = conflictView ?? 'request'
        if (!start || requested.current?.key === key) {
            return
        }
        const request = { key }
        requested.current = request
        start()
            .then(id => {
                if (requested.current === request) {
                    setComparisonId(id)
                } else {
                    // Another view was picked meanwhile: a comparison nobody shows is let go at once.
                    releaseComparison(id)
                }
            })
            .catch((startError: unknown) => {
                if (requested.current === request) {
                    setError(errorMessage(startError) || t('failed'))
                }
            })
    }, [versions, projectId, conflict, conflictView, t])

    /**
     * Leaves the comparison on screen and what was read of it. The comparison itself is released by the effect that
     * holds it, which lets go of it as soon as the screen stops naming it.
     */
    const clearComparison = useCallback(() => {
        setComparisonId(null)
        setComparison(null)
        setSelectedId(null)
        setTable(null)
        setTableError(null)
        setError(null)
    }, [])

    /** Shows another view of the conflicted file: the comparison of the view left is let go. */
    const showView = useCallback((view: ConflictView) => {
        clearComparison()
        setPickedView(view)
    }, [clearComparison, setPickedView])

    // The result is read when the comparison says it has finished, and again as soon as the page is
    // listening: a comparison of two small files can be over before then, and what was pushed to the
    // topic by then is not repeated. A comparison still running answers that its result is not ready,
    // and the topic then says when to ask again.
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
            .catch((readError: unknown) => {
                if (cancelled) {
                    return
                }
                if (isStillRunning(readError)) {
                    // The topic says when it ends; a screen that is not listening to it - a connection
                    // that never opened - would wait for a word that never comes, so it asks again.
                    if (!progress.subscribed) {
                        asking = window.setTimeout(() => setAttempt(count => count + 1), ASK_AGAIN)
                    }
                    return
                }
                setError(errorMessage(readError) || t('failed'))
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
            .catch((tableReadError: unknown) => {
                if (!cancelled) setTableError(errorMessage(tableReadError) || t('table_failed'))
            })
            .finally(() => {
                if (!cancelled) setTableLoading(false)
            })
        return () => {
            cancelled = true
        }
    }, [comparisonId, selectedId, t])

    // A comparison holds both workbooks parsed, so a screen that is done with it puts them down at
    // once instead of leaving them until the session ends. The window may go without a word - closed,
    // or left for another page - so the comparison is released on both, and only once.
    useEffect(() => {
        if (!comparisonId) {
            return undefined
        }
        let released = false
        const release = () => {
            if (released) {
                return
            }
            released = true
            releaseComparison(comparisonId)
        }
        window.addEventListener('pagehide', release)
        return () => {
            window.removeEventListener('pagehide', release)
            release()
        }
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
        } catch (startError) {
            setError(errorMessage(startError) || t('failed'))
        } finally {
            setStarting(false)
        }
    }, [ready, pickedProject, sides, files, t])

    /** Back to the files, leaving the comparison behind: another pair is compared from here. */
    const pickOtherFiles = useCallback(() => {
        // The files that were compared are let go with it, so the next pair is the pair that is picked.
        setFiles([])
        clearComparison()
        setShowEqualRows(false)
    }, [clearComparison])

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

    // The two versions a view compares, as the window names them, and where it shows them.
    const shownView = conflictView ? CONFLICT_VIEWS[conflictView] : null
    const titleKeys = titleKeysOf(shownView, versions ?? textVersions, !!pickedProject)
    const titles = titleKeys && { first: t(titleKeys.first), second: t(titleKeys.second) }
    const secondOnLeft = !!shownView?.secondOnLeft

    const conflictHead = conflict && (
        <ConflictHead path={conflict} status={conflictStatus}>
            {conflictView && (
                <Segmented<ConflictView>
                    aria-label={t('conflict_view_label')}
                    data-testid="compare-conflict-view"
                    onChange={showView}
                    size="small"
                    value={conflictView}
                    options={VIEWS.map(view => ({
                        label: t(CONFLICT_VIEWS[view].label),
                        value: view,
                        disabled: !conflictViews.includes(view),
                    }))}
                />
            )}
        </ConflictHead>
    )

    if (conflict && !conflictVersions) {
        return (
            <div className={styles.page}>
                {conflictHead}
                <div className={styles.center}>
                    <Spin description={t('comparing')} />
                </div>
            </div>
        )
    }

    if (conflict && conflictViews.length === 0) {
        return (
            <div className={styles.page}>
                {conflictHead}
                <div className={styles.center} data-testid="compare-conflict-deleted">
                    <Empty description={t('conflict_deleted')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
                </div>
            </div>
        )
    }

    const text = textVersions ?? conflictText
    if (text) {
        return (
            <div className={styles.page}>
                {conflictHead || <ConflictHead path={text.path} status={null} />}
                <TextVersionsView
                    key={conflictView}
                    read={text.read}
                    secondOnLeft={secondOnLeft}
                    titles={titles}
                />
            </div>
        )
    }

    return (
        <div className={styles.page}>
            {conflictHead}
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
                    {error && <Alert showIcon data-testid="compare-error" title={error} type="error" />}
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
                                    {error && <Alert showIcon data-testid="compare-error" title={error} type="error" />}
                                    {comparison?.identical && (
                                        <Alert
                                            showIcon
                                            data-testid="compare-identical"
                                            title={t('identical')}
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
                            secondOnLeft={secondOnLeft}
                            showEqualRows={showEqualRows}
                            table={table}
                            titles={titles}
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
