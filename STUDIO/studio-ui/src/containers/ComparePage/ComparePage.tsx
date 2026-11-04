import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Alert, Button, Checkbox, Empty, Segmented, Skeleton, Spin, Splitter, Tooltip, Upload } from 'antd'
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
    getConflictComparisonTable,
    getConflictFileText,
    getConflictFileVersions,
    startConflictComparison,
    startConflictVersionsComparison,
    startFileComparison,
    startLocalHistoryComparison,
    startProjectComparison,
} from 'services/compare'
import { fileExistsAt, getFileText } from 'services/files'
import type { ConflictFileStatus, ConflictFileVersions, ConflictSide, ProjectFileSide } from 'services/compare'
import type { Comparison, ComparisonTable, ConflictComparisonTable } from 'types/compare'
import {
    ComparisonPanes,
    ConflictPanes,
    THEIRS_FIRST_PANES,
    VERSION_PANES,
    type ConflictPane,
    type DiffView,
} from './ComparisonPanes'
import { ConflictHead } from './ConflictHead'
import { TextVersionsView } from './TextVersionsView'
import {
    comparedRevisionsOf,
    type ComparedRevisions,
    pageOf,
    RevisionsComparison,
    RevisionsHead,
    useProjectName,
} from './RevisionsComparison'
import { type LoadedChanges, useRevisionChanges } from '../projects/RevisionChanges'
import type { ProjectFileChange, ProjectFileChangeType } from 'services/projectStatus'
import { RevisionPicker, type PickedRevision } from './RevisionPicker'
import { ComparisonTree } from './ComparisonTree'
import { isFinished, useComparisonProgress } from './useComparisonProgress'
import { useStyles } from './ComparePage.styles'

const ACCEPTED = '.xls,.xlsx,.xlsm'
/** How long a screen that cannot hear the topic waits before asking again, in milliseconds. */
const ASK_AGAIN = 2000

const FILES_TO_COMPARE = 2

/**
 * What the comparison of a conflicted file shows: the three versions side by side, what your branch changed since the
 * base, what their branch changed, or your version against theirs.
 */
type ConflictView = 'all' | 'yours' | 'theirs' | 'versions'

/** How a view of a conflicted file is offered, and how it compares its versions. */
interface ConflictViewDefinition {
    label: string
    /** The versions it compares, the one it starts from first; the three of them in the view of all versions. */
    compared: [ConflictSide, ConflictSide] | [ConflictSide, ConflictSide, ConflictSide]
    /** Whether the version it compares second is shown on the left. */
    secondOnLeft: boolean
}

/**
 * How each view of a conflicted file compares its versions, in the order offered. The versions stand as merge tools
 * lay them out, your version on the left, theirs on the right and the base between them: what your branch changed
 * shows your version left of the base, though the comparison starts from the base.
 */
const CONFLICT_VIEWS: Record<ConflictView, ConflictViewDefinition> = {
    all: { label: 'view_all_versions', compared: ['base', 'ours', 'theirs'], secondOnLeft: false },
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

/** A file of the common revision against the same file of a later revision of another history. */
const COMMON_REVISION_TITLES: TitleKeys = { first: 'common_revision', second: 'later_revision' }

/** A file of the common revision against the same file of the working copy. */
const COMMON_WORKSPACE_TITLES: TitleKeys = { first: 'common_revision', second: 'user_workspace' }

/** A file of the revision picked against the same file of the working copy. */
const WORKSPACE_TITLES: TitleKeys = { first: 'selected_revision', second: 'user_workspace' }

/** What is known of a conflicted file whose versions cannot be read: the merge compared their two versions. */
const UNKNOWN_VERSIONS: ConflictFileVersions = { base: false, ours: true, theirs: true }

/**
 * The views of a conflicted file whose versions all hold it. The three versions side by side are offered for a
 * workbook only: a text is read line by line, two versions at a time.
 */
const viewsOf = (versions: ConflictFileVersions, workbook: boolean): ConflictView[] =>
    VIEWS.filter(view => (view !== 'all' || workbook) && CONFLICT_VIEWS[view].compared.every(side => versions[side]))

/**
 * The view a conflicted file opens on: the three versions side by side, as the merge sees them, when they are
 * offered; else what their branch changed, which the merge brings in.
 */
const firstViewOf = (views: ConflictView[]): ConflictView | null => {
    if (views.includes('all')) {
        return 'all'
    }
    return views.includes('theirs') ? 'theirs' : views[0] ?? null
}

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
    /**
     * Your version and theirs, in the order they stand, when both are compared with the version they were made from,
     * as the two versions of a conflict are: the version a merge goes into first, with what each changed and where
     * both changed the same cells.
     */
    panes?: ConflictPane[] | undefined
}

/** A file that is not a workbook at two versions: it reads line by line. */
interface TextVersionsRequest {
    path: string
    read: () => Promise<[string, string]>
    /** What the two versions are called, or nothing for a first and a second version. */
    titles?: TitleKeys | undefined
}

/** The three versions of a conflicted file of a merge: the base and the two versions made from it. */
interface ThreeVersions {
    path: string
    /** Which versions hold the file: a view whose versions do not all hold it is not offered. */
    held: () => Promise<ConflictFileVersions>
    /** Starts comparing two versions of a workbook, the one it starts from first. */
    compare: (first: ConflictSide, second: ConflictSide) => Promise<string>
    /** Starts comparing the base of a workbook with both versions at once. */
    compareAll: () => Promise<string>
    /** Reads a version of a file that is not a workbook. */
    text: (side: ConflictSide) => Promise<string>
}

/**
 * The three versions of a conflicted file of a merge, as the merge holds them. Which versions hold the file is what the
 * merge reports; without it, the two versions the merge compared are taken to hold it.
 */
const conflictOf = (projectId: string, path: string): ThreeVersions => ({
    path,
    held: () => getConflictFileVersions(projectId, path).catch(() => UNKNOWN_VERSIONS),
    compare: (first, second) => startConflictComparison(projectId, path, first, second),
    compareAll: () => startConflictVersionsComparison(projectId, path),
    text: side => getConflictFileText(projectId, path, side),
})

/** The versions the window was opened for: of a workbook, of a file that reads line by line, or three of a file. */
type NamedVersions = Pick<ComparisonRequest, 'versions' | 'textVersions' | 'threeVersions'>

/**
 * The keys of the names of the two versions a comparison shows. The versions of a conflicted file and the versions the
 * screen that opened the window named are named as what they are; only two files picked from the computer are a
 * first and a second file, with no names.
 *
 * @param shownView the view of a conflicted file shown, or null for any other comparison
 * @param named the versions the window was opened for, or null when it was opened for none
 */
const titleKeysOf = (
    shownView: ConflictViewDefinition | null,
    named: VersionsRequest | TextVersionsRequest | null
): TitleKeys | undefined => {
    if (shownView) {
        return { first: VERSION_TITLES[shownView.compared[0]], second: VERSION_TITLES[shownView.compared[1]] }
    }
    return named?.titles
}

/**
 * What the two versions of a file of a project are called: the earlier and the later revision, or a revision against
 * the working copy, as the revision picked against it or as the revision saved before the changes a save commits.
 */
const fileTitlesOf = (params: URLSearchParams, after: string | undefined): TitleKeys => {
    if (params.get('common')) {
        return after ? COMMON_REVISION_TITLES : COMMON_WORKSPACE_TITLES
    }
    if (after) {
        return REVISION_TITLES
    }
    return params.get('base') ? WORKSPACE_TITLES : SAVE_TITLES
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
 *
 * A file the later revision added (`change=added`) is absent before it, and one it deleted (`change=deleted`) is
 * absent after it. The side that does not hold the file is read as an empty file, so the other side shows whole: all
 * added or all removed. A file added by the first revision of a project has no earlier revision at all.
 *
 * An address naming the revision both histories parted at (`common`) compares the file from that revision, as a pull
 * request shows what a branch changed: the earlier revision is of another history.
 */
const fileRevisionsOf = (params: URLSearchParams, conflicting: boolean): NamedVersions | null => {
    const projectId = params.get('projectId')
    const file = params.get('file')
    const change = params.get('change')
    const absentBefore = change === 'added'
    // A file of two revisions compared is named by the revisions themselves. A revision of another history is compared
    // from the revision both histories parted at, as a pull request shows what a branch changed.
    const common = params.get('common') ?? undefined
    const before = common ?? params.get('before') ?? params.get('base') ?? undefined
    if (!projectId || !file || (!before && !absentBefore)) {
        return null
    }
    // A file named without a later revision is compared with the working copy.
    const after = params.get('after') ?? params.get('revision') ?? undefined
    const branch = params.get('branch') ?? undefined
    const absentAfter = change === 'deleted'
    const titles = fileTitlesOf(params, after)
    const earlier = params.get('base')
    if (common && earlier && conflicting && isWorkbookPath(file)) {
        // Both sides changed the same parts of the workbook: each is compared with the revision they parted at.
        const yoursEarlier = params.get('yours') === 'earlier'
        const start = () => startFromCommon({ projectId, path: file, branch, common, earlier, after, change, yoursEarlier })
        const panes = yoursEarlier ? VERSION_PANES : THEIRS_FIRST_PANES
        return { versions: { start, panes }, textVersions: null, threeVersions: null }
    }
    if (!isWorkbookPath(file)) {
        const read = () => Promise.all([
            absentBefore ? '' : getFileText(projectId, file, before),
            absentAfter ? '' : getFileText(projectId, file, after),
        ])
        return { versions: null, textVersions: { path: file, read, titles }, threeVersions: null }
    }
    const start = () => startProjectComparison(projectId,
        absentBefore ? { path: file, absent: true } : { path: file, branch, revision: before },
        absentAfter ? { path: file, absent: true } : sideAfter(file, branch, after))
    return { versions: { start, titles }, textVersions: null, threeVersions: null }
}

/** A workbook both sides of a comparison changed, as a pull request of the later side names them. */
interface ChangedByBoth {
    projectId: string
    path: string
    branch: string | undefined
    /** The revision both sides were made from. */
    common: string
    /** The earlier revision, of the other history: the side a merge of the later one goes into. */
    earlier: string
    /** The later revision, or nothing for the working copy. */
    after: string | undefined
    /** What the later side did to the workbook since the common revision. */
    change: string | null
    /** Whether the earlier side is your version; otherwise the later side is. */
    yoursEarlier: boolean
}

/**
 * Starts comparing your version of a workbook and their version with the revision both were made from. A version that
 * does not hold the workbook is compared as an empty one: the later side tells what it did to it, and the earlier
 * revision is asked whether it holds it.
 */
const startFromCommon = async ({ projectId, path, branch, common, earlier, after, change, yoursEarlier }: ChangedByBoth) => {
    const absent: ProjectFileSide = { path, absent: true }
    const earlierHeld = await fileExistsAt(projectId, path, earlier)
    const earlierSide: ProjectFileSide = earlierHeld ? { path, branch, revision: earlier } : absent
    const laterSide = change === 'deleted' ? absent : sideAfter(path, branch, after)
    return startProjectComparison(projectId,
        yoursEarlier ? earlierSide : laterSide,
        yoursEarlier ? laterSide : earlierSide,
        change === 'added' ? absent : { path, branch, revision: common })
}

/** The later side of a file: at a revision of the branch, or in the working copy when none is named. */
const sideAfter = (path: string, branch: string | undefined, after: string | undefined): ProjectFileSide =>
    after ? { path, branch, revision: after } : { path }

/**
 * A file of three versions that is not a workbook, read as the view of it compares it: the version it starts from
 * first.
 *
 * @param versionOf reads a version of the file
 */
const conflictTextOf = (
    conflict: string,
    view: ConflictViewDefinition,
    versionOf: (side: ConflictSide) => Promise<string>
): TextVersionsRequest => {
    const [first, second] = view.compared
    return { path: conflict, read: () => Promise.all([versionOf(first), versionOf(second)]) }
}

/** What the window was opened to compare, read from the address it was opened with. */
interface ComparisonRequest {
    /** Two versions, when the window was opened for them. */
    versions: VersionsRequest | null
    /** A file that is not a workbook at two revisions, when the window was opened for one. */
    textVersions: TextVersionsRequest | null
    /** The three versions of a file: a conflicted file, or a file compared through a common revision. */
    threeVersions: ThreeVersions | null
    /** The project whose revision is picked to compare its working copy with, or null when there is nothing to pick. */
    pickedProject: string | null
}

/**
 * What the address says the window is for.
 *
 * A window is opened for two versions, for a project whose files are picked, or for a
 * conflicted file of a project. Reading them apart in one place keeps the screen itself to what it
 * draws.
 *
 * @param conflicting whether the file of two revisions the address names may conflict in a merge of both
 */
const requestOf = (params: URLSearchParams, conflicting = false): ComparisonRequest => {
    const named = fileRevisionsOf(params, conflicting)
        ?? { versions: moduleVersionsOf(params), textVersions: null, threeVersions: null }
    const projectId = named.versions || named.textVersions || named.threeVersions ? null : params.get('projectId')
    // A conflicted file of a project is named on its own; a project named without one is a project to
    // pick two files of.
    const conflict = projectId ? params.get('conflict') : null
    return {
        ...named,
        threeVersions: named.threeVersions ?? (projectId && conflict ? conflictOf(projectId, conflict) : null),
        pickedProject: conflict ? null : projectId,
    }
}

/** The comparison the window was opened for, or null when it opens on something to pick. */
const startOf = (
    versions: VersionsRequest | null,
    three: ThreeVersions | null,
    conflictView: ConflictView | null
): (() => Promise<string>) | null => {
    if (versions) {
        return versions.start
    }
    // A workbook of three versions is compared once it is known which versions hold it, as the view picked compares it.
    if (three && isWorkbookPath(three.path) && conflictView === 'all') {
        return three.compareAll
    }
    if (three && isWorkbookPath(three.path) && conflictView) {
        const [first, second] = CONFLICT_VIEWS[conflictView].compared
        return () => three.compare(first, second)
    }
    return null
}

/**
 * What the window knows of a file of three versions: which versions hold it, what became of it, the views it is offered
 * in and the one shown, and its versions read as text when it is not a workbook. Nothing is known without a file.
 *
 * Which versions hold the file is read as the window opens. The versions stay as they are while the window is open,
 * so each is read once for every view; a version that could not be read is read again when a view asks for it next.
 */
const useThreeVersions = (three: ThreeVersions | null) => {
    // Which versions hold the file; null while they are being read.
    const [conflictVersions, setConflictVersions] = useState<ConflictFileVersions | null>(null)
    // The view the user picked; until then, the file is shown in the view it opens on.
    const [pickedView, setPickedView] = useState<ConflictView | null>(null)
    const conflictStatus = three && conflictVersions ? conflictFileStatusOf(conflictVersions) : null
    const conflictWorkbook = !!three && isWorkbookPath(three.path)
    const conflictViews = useMemo(() => conflictVersions ? viewsOf(conflictVersions, conflictWorkbook) : [],
        [conflictVersions, conflictWorkbook])
    const conflictView = pickedView ?? firstViewOf(conflictViews)
    // The versions read of the file, which another file starts anew.
    const conflictTexts = useRef<{ of: ThreeVersions | null, texts: Map<ConflictSide, Promise<string>> }>(
        { of: null, texts: new Map() })
    const conflictText = useMemo(() => {
        if (!three || conflictWorkbook || !conflictView) {
            return null
        }
        const versionOf = (side: ConflictSide) => {
            if (conflictTexts.current.of !== three) {
                conflictTexts.current = { of: three, texts: new Map() }
            }
            const { texts } = conflictTexts.current
            const known = texts.get(side)
            if (known) {
                return known
            }
            const text = three.text(side)
            texts.set(side, text)
            text.catch(() => texts.delete(side))
            return text
        }
        return conflictTextOf(three.path, CONFLICT_VIEWS[conflictView], versionOf)
    }, [three, conflictWorkbook, conflictView])

    useEffect(() => {
        if (!three) {
            return undefined
        }
        let cancelled = false
        three.held().then(found => {
            if (!cancelled) {
                setConflictVersions(found)
            }
        }, () => undefined)
        return () => {
            cancelled = true
        }
    }, [three])

    return { conflictVersions, conflictStatus, conflictViews, conflictView, setPickedView, conflictWorkbook, conflictText }
}

/**
 * Releases the comparison a screen is done with. A comparison holds both workbooks parsed, so a screen that is done with
 * it puts them down at once instead of leaving them until the session ends. The window may go without a word - closed,
 * or left for another page - so the comparison is released on both, and only once.
 */
const useReleasedOnLeave = (comparisonId: string | null) => {
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
}

interface ComparisonScreenProps {
    /** What heads the screen instead of the name of the file compared, when the window names it otherwise. */
    head?: React.ReactNode
    /** What the screen compares, when the window tells it otherwise than its address does. */
    address?: URLSearchParams | undefined
    /** Whether the file of two revisions compared may conflict in a merge of both. */
    conflicting?: boolean | undefined
}

/** The views a file of three versions is offered in, each available only when its versions hold the file. */
const ViewPicker: React.FC<{
    view: ConflictView
    offered: ConflictView[]
    onChange: (view: ConflictView) => void
}> = ({ view, offered, onChange }) => {
    const { t } = useTranslation('compare')
    return (
        <Segmented<ConflictView>
            aria-label={t('conflict_view_label')}
            data-testid="compare-conflict-view"
            onChange={onChange}
            size="small"
            value={view}
            options={VIEWS.map(option => ({
                label: t(CONFLICT_VIEWS[option].label),
                value: option,
                disabled: !offered.includes(option),
            }))}
        />
    )
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
 * A window opened for a conflicted file of a project shows the three versions side by side, what each
 * branch changed since the base version, or the two versions against each other: a workbook as a
 * comparison, anything else line by line. A view whose versions do not all hold the file is not
 * offered.
 */
const ComparisonScreen: React.FC<ComparisonScreenProps> = ({ head, address, conflicting = false }) => {
    const { t } = useTranslation('compare')
    const { styles } = useStyles()
    const [routed] = useSearchParams()
    const params = address ?? routed
    const { versions, textVersions, threeVersions: three } = useMemo(
        () => requestOf(params, conflicting), [params, conflicting]
    )
    const { conflictVersions, conflictStatus, conflictViews, conflictView, setPickedView, conflictWorkbook, conflictText } =
        useThreeVersions(three)

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
    const [conflictTable, setConflictTable] = useState<ConflictComparisonTable | null>(null)
    const [tableLoading, setTableLoading] = useState(false)
    const [tableError, setTableError] = useState<string | null>(null)
    const [error, setError] = useState<string | null>(null)
    const [starting, setStarting] = useState(false)
    const [showEqualElements, setShowEqualElements] = useState(false)
    // The cells outside OpenL tables are listed only when asked for.
    const [showOtherTables, setShowOtherTables] = useState(false)
    const otherTablesSwitch = (
        <Tooltip title={t('show_other_tables_hint')}>
            <Checkbox
                checked={showOtherTables}
                data-testid="compare-show-other-tables"
                onChange={event => setShowOtherTables(event.target.checked)}
            >
                {t('show_other_tables')}
            </Checkbox>
        </Tooltip>
    )
    const [showEqualRows, setShowEqualRows] = useState(false)
    // Two versions drawn as one table, or read side by side when the reader asks for that.
    const [view, setView] = useState<DiffView>('combined')
    // The width the list of elements was last given, and whether it is shown at all. The list is
    // hidden and brought back by a button of its own rather than by the divider, so the control is
    // always in sight. A window that heads the list with its three controls starts wider, so that none
    // of them is shortened before the user has touched the divider.
    const [treeWidth, setTreeWidth] = useState<number | string>(versions || three ? '42%' : '30%')
    const [treeHidden, setTreeHidden] = useState(false)

    // A window opened for a file says which file it compares, a workbook or not.
    const comparedFile = three?.path ?? textVersions?.path
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
        const start = startOf(versions, three, conflictView)
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
    }, [versions, three, conflictView, t])

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

    // The sides of an element are read when it is picked, because each of them is a whole table: the two compared, or
    // the three versions of a conflict side by side.
    const threeWay = conflictView === 'all' || !!versions?.panes
    useEffect(() => {
        if (!comparisonId || !selectedId) {
            setTable(null)
            setConflictTable(null)
            return
        }
        let cancelled = false
        setTableLoading(true)
        setTableError(null)
        const read = threeWay
            ? getConflictComparisonTable(comparisonId, selectedId).then(result => {
                if (!cancelled) setConflictTable(result)
            })
            : getComparisonTable(comparisonId, selectedId).then(result => {
                if (!cancelled) setTable(result)
            })
        read
            .catch((tableReadError: unknown) => {
                if (!cancelled) setTableError(errorMessage(tableReadError) || t('table_failed'))
            })
            .finally(() => {
                if (!cancelled) setTableLoading(false)
            })
        return () => {
            cancelled = true
        }
    }, [comparisonId, selectedId, threeWay, t])

    useReleasedOnLeave(comparisonId)

    /** Keeps the width the divider was dragged to, so hiding the list and bringing it back restores it. */
    const rememberTreeWidth = useCallback((width: number | undefined) => {
        if (width !== undefined && width > 0) {
            setTreeWidth(width)
        }
    }, [])

    const ready = files.length === FILES_TO_COMPARE

    const compare = useCallback(async () => {
        if (!ready) {
            return
        }
        setStarting(true)
        setError(null)
        try {
            setComparisonId(await startFileComparison(files[0]!, files[1]!))
        } catch (startError) {
            setError(errorMessage(startError) || t('failed'))
        } finally {
            setStarting(false)
        }
    }, [ready, files, t])

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
    const back = versions || three ? null : (
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
    const titleKeys = titleKeysOf(shownView, versions ?? textVersions)
    const titles = titleKeys && { first: t(titleKeys.first), second: t(titleKeys.second) }
    const secondOnLeft = !!shownView?.secondOnLeft

    // Side by side or as one table: offered wherever the versions can be drawn either way.
    const viewSwitch = (
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
    )

    const views = three && conflictView && (
        <ViewPicker offered={conflictViews} onChange={showView} view={conflictView} />
    )
    // A window that names the file otherwise heads the screen itself.
    const screenHead = head ?? (three && <ConflictHead path={three.path} status={conflictStatus}>{views}</ConflictHead>)

    if (three && !conflictVersions) {
        return (
            <div className={styles.page}>
                {screenHead}
                <div className={styles.center}>
                    <Spin description={t('comparing')} />
                </div>
            </div>
        )
    }

    if (three && conflictViews.length === 0) {
        return (
            <div className={styles.page}>
                {screenHead}
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
                {screenHead || <ConflictHead path={text.path} status={null} />}
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
            {screenHead}
            {!comparing && (
                <div className={styles.step}>
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
                    <Checkbox
                        checked={showEqualElements}
                        data-testid="compare-show-equal-elements"
                        onChange={event => setShowEqualElements(event.target.checked)}
                    >
                        {t('show_equal_elements')}
                    </Checkbox>
                    {otherTablesSwitch}
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
                                    {comparison && !back && otherTablesSwitch}
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
                                            showOtherTables={showOtherTables}
                                        />
                                    )}
                                </div>
                            </div>
                        )}
                    </Splitter.Panel>
                    <Splitter.Panel>
                        {/* Three versions are read side by side only: one table would hold two changes of each
                            cell. Your version and theirs, each compared with the base, are read either way. */}
                        {threeWay && (
                            <ConflictPanes
                                error={tableError}
                                loading={tableLoading}
                                panes={versions?.panes}
                                showEqualRows={showEqualRows}
                                table={conflictTable}
                                trailing={versions?.panes && comparison && viewSwitch}
                                view={versions?.panes ? view : 'sides'}
                                leading={treeHidden && (
                                    <Button
                                        data-testid="compare-tree-show"
                                        icon={<MenuUnfoldOutlined />}
                                        onClick={() => setTreeHidden(false)}
                                        size="small"
                                        title={t('show_tree')}
                                        type="text"
                                    />
                                )}
                            />
                        )}
                        {!threeWay && <ComparisonPanes
                            error={tableError}
                            loading={tableLoading}
                            secondOnLeft={secondOnLeft}
                            showEqualRows={showEqualRows}
                            table={table}
                            titles={titles}
                            trailing={comparison && viewSwitch}
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
                        />}
                    </Splitter.Panel>
                </Splitter>
            )}
        </div>
    )
}

/**
 * The comparison window. Opened for two revisions of a project, or for a revision and the working copy, it lists the
 * files changed between them first, and compares the file picked of them in the same window, with the way back to the
 * list; the address names the file, so the browser goes back to the list as well. Opened for a project alone, it picks
 * the revision the working copy is compared with first. Opened for anything else, it compares what the address names.
 */
export const ComparePage: React.FC = () => {
    const [params] = useSearchParams()
    const revisions = useMemo(() => comparedRevisionsOf(params), [params])
    const pickedProject = useMemo(() => requestOf(params).pickedProject, [params])
    if (revisions) {
        return <RevisionsWindow revisions={revisions} />
    }
    return pickedProject ? <RevisionPickerScreen projectId={pickedProject} /> : <ComparisonScreen />
}

/**
 * The revision the working copy of a project is compared with, picked before anything is compared. Compare lists the
 * files changed between the two in the same window, and the browser goes back to the pick.
 */
const RevisionPickerScreen: React.FC<{ projectId: string }> = ({ projectId }) => {
    const { t } = useTranslation('compare')
    const { styles } = useStyles()
    const [, setParams] = useSearchParams()
    const [picked, setPicked] = useState<PickedRevision | null>(null)

    useEffect(() => {
        document.title = t('title')
    }, [t])

    const compare = () => {
        if (!picked) {
            return
        }
        const next = new URLSearchParams({ projectId, base: picked.revision })
        if (picked.branch) {
            next.set('branch', picked.branch)
        }
        setParams(next)
    }

    return (
        <div className={styles.page}>
            <div className={styles.step}>
                <RevisionPicker onChange={setPicked} projectId={projectId} />
                <div>
                    <Button data-testid="compare-start" disabled={!picked} onClick={compare} type="primary">
                        {t('compare')}
                    </Button>
                </div>
            </div>
        </div>
    )
}

/** What became of a file, as the address tells it: added or deleted, and modified when it tells anything else. */
const changeTypeOf = (change: string | null): ProjectFileChangeType =>
    change === 'added' || change === 'deleted' ? change : 'modified'

/**
 * The address a file of two revisions is compared by: the revisions as the changes name them. The address the window
 * was opened with may name a branch for its last revision, as Sync names the branches it merges, and the file is read
 * at a revision only. A revision of another history is compared from the revision both histories parted at.
 */
const fileAddressOf = (params: URLSearchParams, changes: LoadedChanges): URLSearchParams => {
    if (!changes) {
        return params
    }
    const address = new URLSearchParams(params)
    if (changes.base) {
        address.set('base', changes.base.revisionNo)
    }
    if (changes.revision) {
        address.set('revision', changes.revision.revisionNo)
    }
    if (changes.common) {
        address.set('common', changes.common.revisionNo)
    }
    // A file opened by its address is read as the changes tell what became of it: added or deleted on the later side.
    const file = params.get('file')
    const listed = changes.files.find(changed => changed.path === file)
    if (listed && listed.type !== 'modified' && !params.has('change')) {
        address.set('change', listed.type)
    }
    return address
}

/**
 * Two revisions of a project: the files changed between them, or one of them compared. A file is compared once the
 * changes are read, at the revisions they name; when they cannot be read, at the revisions the address names.
 */
const RevisionsWindow: React.FC<{ revisions: ComparedRevisions }> = ({ revisions }) => {
    const { t } = useTranslation('compare')
    const [params, setParams] = useSearchParams()
    const projectName = useProjectName(revisions.projectId)
    // The changes tell the two revisions as well: who made each, when and why.
    const changes = useRevisionChanges(revisions.projectId, revisions.revision, revisions.base)
    const [page, setPage] = useState(1)
    const file = params.get('file')

    useEffect(() => {
        // A file compared names the window after itself.
        if (!file) {
            document.title = projectName ? t('revisions_title', { name: projectName }) : t('title')
        }
    }, [file, projectName, t])
    // The file compared last, which the list marks once the reader is back to it.
    const [lastFile, setLastFile] = useState(file)
    // Whether the file shown was picked of the list in this window, which is then the page before it.
    const pickedHere = useRef(false)

    const fileAddress = useMemo(() => fileAddressOf(params, changes), [params, changes])
    const listed = changes?.files ?? []
    const index = file ? listed.findIndex(changed => changed.path === file) : -1
    // A file the address names that the list does not hold is still compared, as the address says it changed.
    const shownFile: ProjectFileChange | null = file
        ? listed[index] ?? { path: file, type: changeTypeOf(params.get('change')) }
        : null

    const paramsOf = (changed: ProjectFileChange) => {
        const next = new URLSearchParams(params)
        next.set('file', changed.path)
        if (changed.type === 'modified') {
            next.delete('change')
        } else {
            next.set('change', changed.type)
        }
        // A revision of another history is compared through the revision both histories parted at.
        if (changes?.common) {
            next.set('common', changes.common.revisionNo)
        } else {
            next.delete('common')
        }
        return next
    }

    const compareFile = (changed: ProjectFileChange) => {
        pickedHere.current = true
        setLastFile(changed.path)
        setParams(paramsOf(changed))
    }

    /** The file next to the one compared takes its place, and the list turns to its page behind it. */
    const step = (by: -1 | 1) => {
        const next = listed[index + by]
        if (next) {
            setPage(pageOf(index + by))
            setLastFile(next.path)
            setParams(paramsOf(next), { replace: true })
        }
    }

    /**
     * Back to the list: to the page the file was picked on, when it was, so the history does not grow with every file;
     * otherwise the list takes the place of the file.
     */
    const backToFiles = () => {
        if (pickedHere.current) {
            pickedHere.current = false
            window.history.back()
            return
        }
        const next = new URLSearchParams(params)
        next.delete('file')
        next.delete('change')
        next.delete('common')
        setParams(next, { replace: true })
    }

    return (
        <>
            {/* The list stays while a file is compared, so going back neither reads it again nor leaves its page. */}
            <div hidden={!!file}>
                <RevisionsComparison
                    changes={changes}
                    onCompare={compareFile}
                    onPage={setPage}
                    page={page}
                    projectName={projectName}
                    revisions={revisions}
                    selected={lastFile}
                />
            </div>
            {/* Each file is a comparison of its own, so nothing of the one before is left on screen. */}
            {shownFile && changes === undefined && <Skeleton active paragraph={{ rows: 4 }} title={false} />}
            {shownFile && changes !== undefined && (
                <ComparisonScreen
                    key={shownFile.path}
                    address={fileAddress}
                    // A workbook both sides changed in the same parts is compared with the revision they parted at.
                    conflicting={!!changes?.conflicts.includes(shownFile.path)}
                    head={(
                        <RevisionsHead
                            changes={changes}
                            common={fileAddress.get('common') ?? undefined}
                            file={shownFile}
                            onBack={backToFiles}
                            projectName={projectName}
                            revisions={revisions}
                            steps={{ index, total: listed.length, onStep: step }}
                        />
                    )}
                />
            )}
        </>
    )
}

export default ComparePage
