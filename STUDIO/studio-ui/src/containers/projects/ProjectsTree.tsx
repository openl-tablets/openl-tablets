import { plainText, storedSetting } from '../../utils/localStore'
import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { Alert, Button, Empty, Skeleton, Tooltip, Tree, type GetRef } from 'antd'
import { BranchesOutlined, PartitionOutlined, ReloadOutlined, TagOutlined } from '@ant-design/icons'
import { createStyles, useTheme, useThemeMode } from 'antd-style'
import { useTranslation } from 'react-i18next'
import { errorMessage } from '../../utils/errorMessage'
import { getProjectIndex, invalidateProjectIndex, projectSignature } from '../../services/projectIndex'
import type { Project } from '../../types/projects'
import { getProject, getProjectFiles } from '../../services/repositories'
import { LOCAL_LOAD_API_OPTIONS } from '../../services/apiCall'
import { ProjectStatus } from '../../constants/project'
import { buildFileChangeMap } from './fileChanges'
import { listModules } from '../../services/modules'
import { buildFileNodes, type FileNode } from './fileNodes'
import { iconFor } from './fileIcons'
import type { Repository } from '../../types/repositories'
import { STATUS_META } from '../../constants/projectStatusMeta'
import { useSharedStyles } from './sharedStyles'
import { MOCKUP } from './projectsTheme'
import { RepoIcon } from './RepoBadge'
import { BranchMarks } from './BranchMarks'
import { useSearchScope } from '../../hooks/useSearchScope'
import { useAppTheme } from '../../providers/AppThemeProvider'
import {
    activeLevels,
    buildGroupTree,
    findNode,
    GROUP_BY_BRANCH,
    GROUP_BY_REPOSITORY,
    groupKeys,
    pathToNode,
    searchTree,
    groupingSetting,
    pathToProject,
    projectRowOf,
    type GroupNode,
    type GroupingLevels,
    type NodeFilters,
} from './projectGrouping'
import { GroupProjectsModal } from './GroupProjectsModal'
import { isFiltered, selectProjects, type ListingQuery } from './projectListing'
import { ClearFiltersRow } from './ClearFiltersRow'
import { useStoredSetting } from '../../hooks/useStoredSetting'
import { formatFileSize } from '../../utils/fileSize'
import { joinClasses } from '../../utils/joinClasses'
import type { ProjectFileChangeType } from '../../services/projectStatus'

const SELECTED_STORAGE_KEY = 'openl.projects.tree.selected'

/**
 * The key of a file or folder row: what it is, the project it belongs to, and the path inside it. The project id
 * holds no colon.
 */
const fileKey = (projectId: string, path: string, isFile: boolean): string =>
    `${isFile ? 'file' : 'folder'}:${projectId}:${path}`

/** The project and path a file or folder row stands for, or null when the key is not one of theirs. */
const fileKeyParts = (key: string): { projectId: string, path: string } | null => {
    const kind = ['file:', 'folder:'].find(prefix => key.startsWith(prefix))
    if (kind === undefined) {
        return null
    }
    const rest = key.slice(kind.length)
    const separator = rest.indexOf(':')
    return separator < 0 ? null : { projectId: rest.slice(0, separator), path: rest.slice(separator + 1) }
}

/**
 * The files of one project, as the tree reads them when its row is opened.
 *
 * <p>Which of them are modules is only known while the project is open — a closed project keeps no
 * workspace copy to resolve its descriptor against — so the map is empty rather than wrong for one.
 */
interface ProjectContents {
    /**
     * The project as it was when its files were read — its state, branch, revision and last change: another one makes
     * them stale.
     */
    signature: string
    /** The paths of the files of the project. */
    paths: string[]
    /** The files changed and not saved yet, by path: read for a project being edited only. */
    changes?: ReadonlyMap<string, ProjectFileChangeType>
    /** The size of a file in bytes, by its path. */
    sizes: Record<string, number>
    /** The name of the module a file is, by the file's path. */
    modules: Record<string, string>
    error?: string
}

// No node picked is the default, kept as no key at all.
const selectedNode = storedSetting(SELECTED_STORAGE_KEY, '', plainText)

/** The space between the rows and the sides of the panel. */
const BODY_SIDE_PADDING = 8

/**
 * Reads the files of a project, the modules among them and, for a project being edited, its changes.
 *
 * <p>Which files are modules is answered by a project that is open; a closed one cannot say, so its files are all read
 * as plain files rather than the read failing. The changes the screen already knows — those of the project it shows —
 * are taken as they are rather than read again.
 */
const readContents = async (
    project: Project,
    knownChanges: ReadonlyMap<string, ProjectFileChangeType> | undefined
): Promise<ProjectContents> => {
    const [files, modules, changes] = await Promise.all([
        getProjectFiles(project.id).then(
            read => read.filter(node => node.type === 'file'),
            (e: unknown) => errorMessage(e)
        ),
        listModules(project.id).catch(() => []),
        // Only a project being edited has changes to mark; they come with its status.
        project.status === ProjectStatus.Editing && !knownChanges
            ? getProject(project.id, { includes: ['status']}, LOCAL_LOAD_API_OPTIONS)
                .then(read => buildFileChangeMap(read.compileStatus?.pendingChanges?.files ?? [], read.path, read.name))
                .catch(() => undefined)
            : knownChanges,
    ])
    if (typeof files === 'string') {
        return { signature: projectSignature(project), paths: [], sizes: {}, modules: {}, error: files }
    }
    return {
        signature: projectSignature(project),
        ...(changes ? { changes } : {}),
        paths: files.map(file => file.path),
        sizes: Object.fromEntries(files.flatMap(file => (file.size === undefined ? [] : [[file.path, file.size]]))),
        modules: Object.fromEntries(modules.filter(module => module.path).map(module => [module.path, module.name])),
    }
}

/** Ends a read of the files of a project, unless a refresh since has handed its place to another read. */
const settle = (inFlight: Map<string, number>, projectId: string, begun: number): void => {
    if (inFlight.get(projectId) === begun) {
        inFlight.delete(projectId)
    }
}

/** One empty list for a project without empty folders, so the memos built on it hold. */
const NO_FOLDERS: string[] = []

/**
 * How a file or a folder is marked: a file added, modified or deleted and not saved yet, a folder deleted with all its
 * files, or a folder holding such a file.
 */
type FileMark = 'added' | 'modified' | 'deleted' | 'folder'

/**
 * The files and empty folders whose path holds the searched text, by the project they belong to. Only the projects
 * whose files have been read are searched; none is without a text.
 */
const findFiles = (
    contents: Record<string, ProjectContents>,
    search: string,
    foldersOf: (projectId: string) => string[]
): Map<string, string[]> => {
    const needle = search.trim().toLowerCase()
    const found = new Map<string, string[]>()
    if (!needle) {
        return found
    }
    for (const [projectId, read] of Object.entries(contents)) {
        const paths = [...read.paths, ...foldersOf(projectId)].filter(path => path.toLowerCase().includes(needle))
        if (paths.length > 0) {
            found.set(projectId, paths)
        }
    }
    return found
}

/** The ids of the projects among the nodes, at any depth. */
const projectIdsOf = (nodes: GroupNode[]): string[] =>
    nodes.flatMap(node => (node.project ? [node.project.id] : projectIdsOf(node.children)))

/** The folders above a path, outermost first: `a/b/c.txt` lies in `a` and `a/b`. */
const foldersAbove = (path: string): string[] => {
    const segments = path.split('/')
    return segments.slice(0, -1).map((_, index) => segments.slice(0, index + 1).join('/'))
}

/** The rows a search opens to show the files it found: the projects holding them and the folders on the way. */
const foundKeys = (nodes: GroupNode[], found: Map<string, string[]>): string[] => nodes.flatMap(node => {
    if (!node.project) {
        return foundKeys(node.children, found)
    }
    const projectId = node.project.id
    const paths = found.get(projectId)
    return paths
        ? [node.key, ...paths.flatMap(foldersAbove).map(folder => fileKey(projectId, folder, false))]
        : []
})

/** What tells the changed files and folders of a project apart. */
interface ProjectMarks {
    /** The files changed and not saved yet, by path; none for a project that is not being edited. */
    changes: ReadonlyMap<string, ProjectFileChangeType> | undefined
    /** The folders holding a file that is there, or added in the browser; any other was deleted with its files. */
    live: Set<string>
    /** The folders whose files are all added and not saved yet: new folders. */
    added: Set<string>
    /** The folders holding a changed file, at any depth. */
    changed: Set<string>
}

/**
 * The marks of the files and folders of a project, from the paths of its files, the empty folders added in the
 * browser and the changes not saved yet. An empty folder added in the browser is not saved until a file lands in it,
 * so it is no added folder yet.
 */
const marksOf = (
    paths: string[],
    emptyFolders: string[],
    changes: ReadonlyMap<string, ProjectFileChangeType> | undefined
): ProjectMarks => {
    const live = new Set([...paths, ...emptyFolders.map(folder => `${folder}/`)].flatMap(foldersAbove))
    // A folder that holds, or held, a file older than the changes was there before them: a file renamed in it is
    // deleted there and added again.
    const older = new Set([...paths, ...changes?.keys() ?? []]
        .filter(path => changes?.get(path) !== 'added')
        .flatMap(foldersAbove))
    const added = new Set(changes ? paths.flatMap(foldersAbove).filter(folder => !older.has(folder)) : [])
    const changed = new Set([...changes?.keys() ?? []].flatMap(foldersAbove))
    return { changes, live, added, changed }
}

/**
 * What tells a changed file or a folder holding one: a dot on its icon, the colour of the change on its name and a hint
 * of what changed.
 */
const changeOf = (file: FileNode, marks: ProjectMarks): FileMark | null => {
    if (!file.isFile) {
        if (!marks.live.has(file.path)) {
            return 'deleted'
        }
        if (marks.added.has(file.path)) {
            return 'added'
        }
        return marks.changed.has(file.path) ? 'folder' : null
    }
    return marks.changes?.get(file.path) ?? null
}

/**
 * The projects the tree shows: those the filters select, as the list shows them, and the project the screen is
 * showing whatever the filters say — it is where the reader is. The order of the projects is kept.
 */
const treeProjects = (projects: Project[], filters: ListingQuery, currentProjectId: string | undefined): Project[] => {
    const selected = new Set(selectProjects(projects, filters))
    return projects.filter(project => selected.has(project) || project.id === currentProjectId)
}

const useStyles = createStyles(({ css, token }) => ({
    /**
     * The tree scrolls sideways instead of squeezing the names: a project is recognised by its full
     * name, and a wrapped or clipped one is neither readable nor comparable to the row above it.
     */
    body: css`
        padding: 4px ${BODY_SIDE_PADDING}px 12px;
        overflow-x: auto;

        .ant-tree-list-holder-inner,
        .ant-tree-list {
            min-width: max-content;
        }
    `,
    /** The tree is laid out at the width its names need; the body around it is what scrolls. */
    tree: css`
        width: max-content;
        min-width: 100%;

        /* A row takes the whole width of the tree, so the size of a file can keep to the right. */
        .ant-tree-node-content-wrapper {
            flex: auto;
        }

        .ant-tree-title {
            flex: 1;
        }
    `,
    node: css`
        display: inline-flex;
        align-items: center;
        gap: 6px;
        white-space: nowrap;
    `,
    current: css`
        font-weight: 600;
        color: ${token.colorPrimary};
    `,
    /** A file or folder row: its name, and the size of a file on the right. */
    file: css`
        display: flex;
        align-items: baseline;
        gap: ${token.marginXS}px;
        white-space: nowrap;
    `,
    fileName: css`
        flex: 1;
    `,
    /** A file added or modified and not saved yet: its name in the colour of the change. */
    added: css`
        color: ${token.colorSuccessText};
    `,
    modified: css`
        color: ${token.colorWarningText};
    `,
    /** The icon of a changed file, or of a folder holding one, with the dot of the change at its corner. */
    markedIcon: css`
        position: relative;
        display: inline-flex;
    `,
    changeDot: css`
        position: absolute;
        right: -2px;
        bottom: 0;
        width: 6px;
        height: 6px;
        border-radius: 50%;
        box-shadow: 0 0 0 1px ${MOCKUP.pageBg};
    `,
    addedDot: css`
        background: ${token.colorSuccess};
    `,
    modifiedDot: css`
        background: ${token.colorWarning};
    `,
    folderDot: css`
        background: ${token.colorWarning};
    `,
    /** A file or a folder deleted and not saved yet: struck through and faint, as it is no longer there. */
    deleted: css`
        color: ${token.colorTextQuaternary};
        text-decoration: line-through;
    `,
    deletedDot: css`
        background: ${token.colorError};
    `,
    /**
     * The tree is as wide as its widest row and may run past the rail, so the size keeps to the right edge of what
     * the rail shows rather than to the end of the row. A long name then runs under it, so the size wears the
     * colour of its row: the rail, and the tint of a row pointed at or picked over it.
     */
    size: css`
        flex: none;
        position: sticky;
        /* Over the padding of the body, up to the edge of the rail: a name running under the size never shows past it. */
        right: -${BODY_SIDE_PADDING}px;
        padding: 0 ${BODY_SIDE_PADDING}px 0 ${token.paddingXS}px;
        background: ${MOCKUP.pageBg};
        color: ${token.colorTextTertiary};

        .ant-tree-node-content-wrapper:hover & {
            background: linear-gradient(${token.controlItemBgHover}, ${token.controlItemBgHover}), ${MOCKUP.pageBg};
        }

        .ant-tree-node-selected & {
            background: linear-gradient(${token.controlItemBgActive}, ${token.controlItemBgActive}), ${MOCKUP.pageBg};
        }
        font-size: ${token.fontSizeSM}px;
    `,
    state: css`
        padding: 12px 16px;
    `,
    /** The title leads back to every project, so it reads as the root of the tree. */
    title: css`
        padding: 0;
        height: auto;
        color: inherit;
        font-size: 14px;
        font-weight: 600;
    `,
}))

/**
 * Where the tree gets its projects. The screen either supplies them — and then owns the re-read — or the
 * tree reads them itself. Supplying projects requires supplying the refresh, so a controlled tree can never
 * silently read on its own; a single-project page supplies neither.
 *
 * `null` supplied means the screen is still loading — the tree shows its skeleton. A screen that failed to read them
 * says why in `error`, which the tree shows in their place.
 */
export type ProjectsSource =
    | { projects: Project[] | null, onRefresh: () => void, error?: string | null | undefined }
    | { projects?: undefined, onRefresh?: undefined, error?: undefined }

interface ProjectsTreeBaseProps {
    /**
     * The design repositories, when the screen has read them. The tree names and marks a repository
     * from the projects themselves otherwise, so a user granted a single project — for whom the
     * repository list reads as empty — still sees proper group titles.
     */
    repositories?: Repository[] | undefined
    /** The project the screen is showing, highlighted and opened in the tree. */
    currentProjectId?: string | undefined
    onOpenProject: (project: Project) => void
    /** A module of a project was picked: read it in the editor. */
    onOpenModule?: ((project: Project, moduleName: string) => void) | undefined
    /** A folder, or a file that is no module, was picked: show it on the page of its project. */
    onOpenFile?: ((project: Project, path: string) => void) | undefined
    /** A group was picked: show the projects it holds. */
    onOpenGroup: (filters: NodeFilters) => void
    /** The title of the tree was picked: show the list without filters. */
    onShowAll: () => void
    /** The picks the list is filtered by: the tree offers to clear them, and narrows to them unless told otherwise. */
    filters: ListingQuery
    /**
     * The picks the tree narrows to, when they differ from the list's: a group picked in the tree filters the list,
     * not the tree, which keeps the picks it had before.
     */
    narrowBy?: ListingQuery | undefined
    /** The picks were cleared from the tree. */
    onClearFilters: () => void
    /**
     * Bumped by the screen when it changed the workspace, so the tree reads it again, the files of the current
     * project included.
     */
    reloadToken?: number | undefined
    /** The files of the current project changed and not saved yet, by path: the tree marks them. */
    currentFileChanges?: ReadonlyMap<string, ProjectFileChangeType> | undefined
    /** Empty folders added to the current project in the browser, listed until a file lands in them. */
    currentFolders?: string[] | undefined
    /**
     * The file or folder of the current project the screen shows, as a link may name it: the tree opens on it once,
     * unfolding what leads to it, and picks it.
     */
    currentFile?: string | undefined
    /** What the rail hangs on the header row, beside the actions of the tree itself. */
    headerActions?: ReactNode
    /**
     * The text the screen around the tree is searched by. A screen that has none leaves it out, and the tree
     * offers itself to the search of the header.
     */
    search?: string | undefined
}

type ProjectsTreeProps = ProjectsTreeBaseProps & ProjectsSource

/**
 * One stable value for every render without the prop: a fresh `[]` default would change identity
 * each render, cascading through the grouping memos into an endless expand-effect loop.
 */
const NO_REPOSITORIES: Repository[] = []

/**
 * The projects as a tree, grouped by up to three levels the user picks — a repository or a tag type.
 *
 * The tree shows the projects the Filters view selects, the same ones the list shows, without the list's paging.
 * The search of the header narrows it: by the text the list beside it is searched by, or, on a screen whose main
 * part is no list of projects, as a scope of its own. While a value is picked, a row under its title offers to
 * clear the filters.
 *
 * The tree reads one lightweight list of projects the first time it is opened and groups it in the
 * browser, so expanding a node costs nothing and the screen around it never waits for the tree.
 */
export const ProjectsTree = ({
    repositories = NO_REPOSITORIES,
    currentProjectId,
    onOpenProject,
    onOpenModule,
    onOpenFile,
    onOpenGroup,
    onShowAll,
    filters,
    narrowBy = filters,
    onClearFilters,
    reloadToken,
    currentFileChanges,
    currentFolders = NO_FOLDERS,
    currentFile,
    headerActions,
    projects: providedProjects,
    onRefresh,
    error: providedError,
    search: screenSearch,
}: ProjectsTreeProps) => {
    const { i18n, t } = useTranslation('repository')
    const locale = i18n.resolvedLanguage ?? i18n.language
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const token = useTheme()
    const { themeName } = useAppTheme()
    const { isDarkMode } = useThemeMode()
    const [levels, setLevels] = useStoredSetting(groupingSetting)
    const [selfProjects, setSelfProjects] = useState<Project[] | null>(null)
    const [error, setError] = useState<string | null>(null)
    const [grouping, setGrouping] = useState(false)
    const [expanded, setExpanded] = useState<string[]>([])
    const filtered = isFiltered(filters)
    // The group the user picked last is remembered, so stepping into a project and back shows where
    // they were in the tree. A group is picked by setting its filters, so without them none is.
    const [selected, setSelected] = useState<string[]>(() => {
        const remembered = filtered ? selectedNode.read() : ''
        return remembered ? [remembered] : []
    })
    const [ownSearch, setOwnSearch] = useState('')
    const search = screenSearch ?? ownSearch
    // Beside a screen with a search of its own the tree is a panel: it takes the field once the reader picks it.
    useSearchScope(screenSearch === undefined
        ? {
            id: 'projects-tree',
            label: t('common:search.scopes.projects'),
            onChange: setOwnSearch,
            placeholder: t('home.tree.search_placeholder'),
            secondary: true,
            value: ownSearch,
        }
        : null)
    const [openedOn, setOpenedOn] = useState<string | null>(null)
    // The files of the projects whose rows have been opened, read once each and kept while the tree stands.
    const [contents, setContents] = useState<Record<string, ProjectContents>>({})

    // The list supplies the projects it already read; only a single-project page lets the tree read them
    // itself, so the list screen no longer pulls the same /projects snapshot a second time.
    const controlled = providedProjects !== undefined
    const projects = controlled ? providedProjects : selfProjects
    const shownError = controlled ? providedError ?? null : error
    const visible = useMemo(
        () => treeProjects(projects ?? [], narrowBy, currentProjectId),
        [currentProjectId, narrowBy, projects]
    )

    // Once the filters are cleared — from the tree, the Filters view or the list — the group they came from is no
    // longer picked, and the tree does not open on it next time.
    useEffect(() => {
        if (!filtered) {
            setSelected(previous => (previous.length === 0 ? previous : []))
            selectedNode.write('')
        }
    }, [filtered])

    const load = useCallback(() => {
        setError(null)
        getProjectIndex()
            .then(index => setSelfProjects(index.projects))
            .catch((e: unknown) => {
                setSelfProjects([])
                setError(errorMessage(e))
            })
    }, [])

    useEffect(() => {
        if (controlled) {
            return
        }
        load()
    }, [controlled, load, reloadToken])

    // Files already read are read again where they stand: a row keeps what it shows until the fresh files replace it,
    // so an open row neither empties nor jumps. A project is read again once at a time.
    const rereading = useRef(new Map<string, number>())
    // The projects whose files are read for the first time.
    const reading = useRef(new Map<string, number>())
    // The projects asked to be read again while a read of them was on its way, which began before the change: they are
    // read once more when it is back. Bumped as such a read comes back.
    const readAgain = useRef(new Set<string>())
    const [readsBack, setReadsBack] = useState(0)
    const readBack = useCallback((projectId: string) => {
        if (readAgain.current.has(projectId)) {
            setReadsBack(count => count + 1)
        }
    }, [])
    // Bumped by a refresh, which drops the files read so far: a read begun before it never replaces a newer one, nor
    // holds back the read that replaces it. Each read in flight is kept with the refresh it began after.
    const generation = useRef(0)
    const reread = useCallback((ids: string[]) => {
        const stale = (projects ?? []).filter(project => ids.includes(project.id) && !rereading.current.has(project.id))
        const begun = generation.current
        for (const project of stale) {
            rereading.current.set(project.id, begun)
            void readContents(project, project.id === currentProjectId ? currentFileChanges : undefined)
                .then(read => setContents(previous => (begun === generation.current && project.id in previous
                    ? { ...previous, [project.id]: read }
                    : previous)))
                .finally(() => {
                    settle(rereading.current, project.id, begun)
                    readBack(project.id)
                })
        }
    }, [currentFileChanges, currentProjectId, projects, readBack])

    // A change the screen made is read again in the files of its project, so an open row lists what is there now.
    const seenToken = useRef(reloadToken)
    useEffect(() => {
        if (reloadToken === seenToken.current) {
            return
        }
        seenToken.current = reloadToken
        if (!currentProjectId) {
            return
        }
        if (rereading.current.has(currentProjectId) || reading.current.has(currentProjectId)) {
            readAgain.current.add(currentProjectId)
        } else if (contents[currentProjectId]) {
            reread([currentProjectId])
        }
    }, [contents, currentProjectId, reloadToken, reread])

    // A project asked to be read again while a read of it was on its way is read once that read is back.
    useEffect(() => {
        const due = [...readAgain.current]
            .filter(id => !rereading.current.has(id) && !reading.current.has(id))
        due.forEach(id => readAgain.current.delete(id))
        if (due.length > 0) {
            reread(due)
        }
    }, [readsBack, reread])

    // Every project names and types the repository it lives in, so the groups stay properly titled
    // even when the repository list itself is not readable; the read list wins where both exist.
    const repoMeta = useMemo(() => {
        const meta = new Map<string, { name?: string | undefined, type?: string | undefined }>()
        for (const project of projects ?? []) {
            if (project.repositoryInfo) {
                meta.set(project.repositoryInfo.id, project.repositoryInfo)
            }
        }
        for (const repo of repositories) {
            meta.set(repo.id, repo)
        }
        return meta
    }, [projects, repositories])

    const repositoryName = useCallback(
        (id: string) => repoMeta.get(id)?.name ?? id,
        [repoMeta]
    )

    // The grouping levels offer every tag the projects actually carry — the same tags the filter rail
    // counts — not only the types an administrator configured in the database.
    const tagTypes = useMemo(() => {
        const bySpelling = new Map<string, string>()
        for (const project of projects ?? []) {
            for (const [type, value] of Object.entries(project.tags ?? {})) {
                if (type && value && !bySpelling.has(type.toLowerCase())) {
                    bySpelling.set(type.toLowerCase(), type)
                }
            }
        }
        return [...bySpelling.values()].sort((left, right) => left.localeCompare(right, undefined, { sensitivity: 'base' }))
    }, [projects])

    const grouped = useMemo(
        () => buildGroupTree(visible, activeLevels(levels), repositoryName),
        [levels, repositoryName, visible]
    )
    // The files of the current project include the empty folders added to it in the browser.
    const foldersOf = useCallback(
        (projectId: string) => (projectId === currentProjectId ? currentFolders : NO_FOLDERS),
        [currentFolders, currentProjectId]
    )
    // The files a search finds, by the project they belong to: only the files of the projects already unfolded are
    // known to the tree.
    const foundFiles = useMemo(
        () => findFiles(contents, search, foldersOf),
        [contents, foldersOf, search]
    )
    // What the search found, with everything under a group that matched by its own name, and a project whose files
    // hold the text.
    const nodes = useMemo(
        () => searchTree(grouped, search, project => foundFiles.has(project.id)),
        [foundFiles, grouped, search]
    )
    // The projects the search keeps whole, found by their own fields or under a group found by its name: they list all
    // their files. Any other one is kept for the files it found alone.
    const keptWhole = useMemo(() => new Set(projectIdsOf(searchTree(grouped, search))), [grouped, search])
    // A search shows what it found straight away, the folders of the files it found included; without one the
    // user decides what is open. What the reader opens themselves is added to it either way — the files of a
    // project are read only when it is unfolded, and a search that pinned the list would leave a found project
    // with a switcher that does nothing.
    const openKeys = useMemo(
        () => (search.trim()
            ? [...new Set([...groupKeys(nodes), ...foundKeys(nodes, foundFiles), ...expanded])]
            : expanded),
        [expanded, foundFiles, nodes, search]
    )
    // The changes of a project: the screen's own for the project it shows, which follow every change of it, and the
    // ones read with the files for any other.
    const changesOf = useCallback(
        (projectId: string) => (projectId === currentProjectId && currentFileChanges
            ? currentFileChanges
            : contents[projectId]?.changes),
        [contents, currentFileChanges, currentProjectId]
    )
    // Files read of another state of their project are stale — saved, closed, opened, switched to another branch or
    // changed since — and are read again.
    useEffect(() => {
        const signatures = new Map((projects ?? []).map(project => [project.id, projectSignature(project)]))
        reread(Object.entries(contents).flatMap(([id, read]) => {
            const signature = signatures.get(id)
            return signature !== undefined && signature !== read.signature ? [id] : []
        }))
    }, [contents, projects, reread])

    // The group remembered from an earlier visit is unfolded once, as soon as the tree knows its shape.
    // Afterwards the user decides what is open: a row picked later, a project above all, opens only by
    // its caret.
    const [restored, setRestored] = useState(false)
    useEffect(() => {
        if (restored || projects === null) {
            return
        }
        setRestored(true)
        const remembered = selected[0]
        const path = remembered ? pathToNode(grouped, remembered) : null
        if (remembered && path) {
            setExpanded(previous => [...new Set([...previous, ...path, remembered])])
        }
    }, [grouped, projects, restored, selected])

    /** Reads the files of a project the first time its row is opened; a read already on its way is not repeated. */
    const loadContents = useCallback(async (project: Project) => {
        if (contents[project.id] || reading.current.has(project.id)) {
            return
        }
        const begun = generation.current
        reading.current.set(project.id, begun)
        try {
            const read = await readContents(project, project.id === currentProjectId ? currentFileChanges : undefined)
            if (begun === generation.current) {
                setContents(previous => ({ ...previous, [project.id]: read }))
            }
        } finally {
            settle(reading.current, project.id, begun)
            readBack(project.id)
        }
    }, [contents, currentFileChanges, currentProjectId, readBack])

    // The tree opens on the file the screen shows, once each time it is shown: the project is unfolded, which has its
    // files read, then the folders leading to the file, and the file is picked and brought into sight. After that the
    // user decides what is open. A screen that no longer shows a file has no file picked in the tree either.
    const treeRef = useRef<GetRef<typeof Tree>>(null)
    const openedOnFile = useRef<string | null>(null)
    useEffect(() => {
        if (!currentProjectId || !currentFile) {
            openedOnFile.current = null
            setSelected(previous => (previous.some(key => fileKeyParts(key)) ? [] : previous))
            return
        }
        const target = `${currentProjectId}:${currentFile}`
        const row = projectRowOf(nodes, currentProjectId)
        if (target === openedOnFile.current || !row) {
            return
        }
        const read = contents[currentProjectId]
        const { path: groups, key: projectKey } = row
        if (!read) {
            // The file is found once the files of the project are read.
            setExpanded(previous => [...new Set([...previous, ...groups, projectKey])])
            const project = projects?.find(candidate => candidate.id === currentProjectId)
            if (project) {
                void loadContents(project)
            }
            return
        }
        openedOnFile.current = target
        const folders = foldersAbove(currentFile).map(folder => fileKey(currentProjectId, folder, false))
        setExpanded(previous => [...new Set([...previous, ...groups, projectKey, ...folders])])
        const fileRow = fileKey(currentProjectId, currentFile, read.paths.includes(currentFile))
        setSelected([fileRow])
        // The rows of the unfolded folders are drawn by the next render; the row is brought into sight after it.
        requestAnimationFrame(() => treeRef.current?.scrollTo({ key: fileRow, align: 'auto' }))
    }, [contents, currentFile, currentProjectId, loadContents, nodes, projects])

    // The tree opens on the project the screen is showing, once — after that the user decides what is open.
    useEffect(() => {
        if (!currentProjectId || projects === null || openedOn === currentProjectId) {
            return
        }
        const path = pathToProject(nodes, currentProjectId)
        setOpenedOn(currentProjectId)
        if (path && path.length > 0) {
            setExpanded(previous => [...new Set([...previous, ...path])])
        }
    }, [currentProjectId, nodes, openedOn, projects])

    const treeData = useMemo(
        // Depends on what shapes a node and on the theme it is drawn in, not on which nodes are open — expanding
        // must not rebuild the tree, while switching the theme or the appearance must repaint the titles. The theme
        // is told by its name and appearance: antd-style hands out new styles and a new token on every render.
        () => nodes.map(node => toTreeNode(node)),
        [changesOf, contents, currentProjectId, foldersOf, foundFiles, isDarkMode, keptWhole, locale, nodes, repoMeta, t,
            themeName]
    )

    // Which rows have been read is told to the tree rather than left to it, so a refresh that drops what
    // was read has the rows read again instead of standing open and empty.
    const loadedKeys = useMemo(() => {
        const keys: string[] = []
        const walk = (list: GroupNode[]) => list.forEach(node => {
            if (node.project) {
                if (contents[node.project.id]) {
                    keys.push(node.key)
                }
            } else {
                walk(node.children)
            }
        })
        walk(nodes)
        return keys
    }, [contents, nodes])

    // A file of a project reads as it does on the page of the project — the same icon, the same name, the same size.
    // A file and a folder alike open there when picked; only the arrow before a folder unfolds it. A file deleted and
    // not saved yet stays, struck through, and opens nothing.
    function fileNodesOf(project: Project, read: ProjectContents, files: FileNode[], marks: ProjectMarks): TreeNodeData[] {
        return files.map(file => {
            const { Icon, color } = iconFor(file.name, file.isFile, {
                success: token.colorSuccess,
                info: token.colorInfo,
                warning: token.colorWarning,
                muted: token.colorTextTertiary,
            })
            const mark = changeOf(file, marks)
            const deleted = mark === 'deleted'
            const title = (
                <span
                    className={styles.file}
                    data-change={mark ?? undefined}
                    data-testid={`tree-${file.isFile ? 'file' : 'folder'}-${project.id}-${file.path}`}
                >
                    <span className={joinClasses(styles.fileName, mark && mark !== 'folder' && styles[mark])}>
                        {file.name}
                    </span>
                    {file.isFile && !deleted && (
                        <span className={styles.size}>{formatFileSize(read.sizes[file.path], locale)}</span>
                    )}
                </span>
            )
            return {
                key: fileKey(project.id, file.path, file.isFile),
                // An empty folder, added in the browser, has nothing to unfold. Given an arrow, the tree would ask to
                // read it as long as it stands open, and the arrow would flicker between that and a spinner.
                isLeaf: file.isFile || file.children?.length === 0,
                ...(deleted ? { selectable: false } : {}),
                icon: mark
                    ? (
                        <span className={styles.markedIcon}>
                            <Icon style={{ color }} />
                            <span className={joinClasses(styles.changeDot, styles[`${mark}Dot`])} />
                        </span>
                    )
                    : <Icon style={{ color }} />,
                title: mark ? <Tooltip title={t(`browser.files.change_hint.${mark}`)}>{title}</Tooltip> : title,
                ...(file.children ? { children: fileNodesOf(project, read, file.children, marks) } : {}),
            }
        })
    }

    // What a project's row holds once it has been opened: its files, or why they could not be read.
    function contentsOf(project: Project): TreeNodeData[] | undefined {
        const read = contents[project.id]
        if (!read) {
            return undefined
        }
        if (read.error) {
            return [{
                key: `error:${project.id}`,
                isLeaf: true,
                selectable: false,
                icon: null,
                title: <span className={styles.node} data-testid={`tree-files-error-${project.id}`}>{read.error}</span>,
            }]
        }
        // A search shows the files it found in a project, and every file of a project found by its own name. The files
        // deleted and not saved yet are listed beside the ones that are there.
        const found = keptWhole.has(project.id) ? undefined : foundFiles.get(project.id)
        const folders = foldersOf(project.id)
        const changes = changesOf(project.id)
        const there = new Set(read.paths)
        const deleted = [...changes ?? []]
            .flatMap(([path, change]) => (change === 'deleted' && !there.has(path) ? [path] : []))
        const files = found
            ? buildFileNodes(found.filter(path => !folders.includes(path)), found.filter(path => folders.includes(path)))
            : buildFileNodes([...read.paths, ...deleted], folders)
        return fileNodesOf(project, read, files, marksOf(read.paths, folders, changes))
    }

    // A repository carries its own icon — a branch, a database, a disk — as it does everywhere else, a
    // branch group carries the branch mark, and a tag value carries the tag it is.
    function groupIcon(node: GroupNode): ReactNode {
        if (node.groupedBy === GROUP_BY_REPOSITORY) {
            return <RepoIcon type={repoMeta.get(node.value ?? '')?.type} />
        }
        if (node.groupedBy === GROUP_BY_BRANCH) {
            return <BranchesOutlined data-testid={`tree-branch-icon-${node.value}`} />
        }
        return <TagOutlined data-testid={`tree-tag-icon-${node.value}`} />
    }

    // The projects a group holds, gathered from its own subtree.
    function projectsOf(node: GroupNode): Project[] {
        return node.project ? [node.project] : node.children.flatMap(projectsOf)
    }

    // The Default and protected marks a branch group wears, read from the projects it actually holds — so a
    // protected or default main in one repository never marks a same-named branch grouped under another.
    function branchMarksOf(node: GroupNode): { isDefault: boolean, isProtected: boolean } {
        return projectsOf(node).reduce<{ isDefault: boolean, isProtected: boolean }>(
            (carried, project) => ({
                isDefault: carried.isDefault || (project.branchDefault ?? false),
                isProtected: carried.isProtected || (project.branchProtected ?? false),
            }),
            { isDefault: false, isProtected: false }
        )
    }

    // A branch group reads as its plain name in the tree's own style, with the same Default and protected
    // marks it wears on a project row; every other group is just its name.
    function groupTitle(node: GroupNode): ReactNode {
        if (node.groupedBy === GROUP_BY_BRANCH) {
            const marks = branchMarksOf(node)
            return (
                <>
                    {node.title}
                    <BranchMarks
                        isDefault={marks.isDefault}
                        isProtected={marks.isProtected}
                        testId={`tree-branch-label-${node.value}`}
                    />
                </>
            )
        }
        return node.title
    }

    function toTreeNode(node: GroupNode): TreeNodeData {
        if (node.project) {
            const project = node.project
            const current = project.id === currentProjectId
            const status = project.status
            const meta = STATUS_META[status]
            const Icon = meta.icon
            const held = contentsOf(project)
            return {
                key: node.key,
                // The row opens into the files of the project, read when it is first opened. A project
                // that turned out to hold none has nothing to open into.
                isLeaf: held?.length === 0,
                ...(held ? { children: held } : {}),
                // A project carries the icon of its state, the same one that marks its name elsewhere:
                // an open folder while it is open, a pencil while it is being edited, and so on. A state
                // that asks for attention is coloured, the ordinary ones are not.
                icon: (
                    <Tooltip title={t(meta.labelKey)}>
                        <Icon
                            aria-label={t(meta.labelKey)}
                            data-testid={`tree-status-${status}`}
                            style={meta.hintKey ? { color: token[meta.tokenColor] } : {}}
                        />
                    </Tooltip>
                ),
                title: (
                    <span className={cx(styles.node, current && styles.current)} data-testid={`tree-project-${project.id}`}>
                        {project.name}
                    </span>
                ),
            }
        }
        return {
            key: node.key,
            icon: groupIcon(node),
            title: (
                <span className={styles.node} data-testid={`tree-group-${node.key}`}>{groupTitle(node)}</span>
            ),
            children: node.children.map(child => toTreeNode(child)),
        }
    }

    /**
     * Follows a file to where it is read: a module to the editor, any other file or a folder to the page of its
     * project.
     */
    const openFile = (projectId: string, path: string) => {
        const project = (projects ?? []).find(candidate => candidate.id === projectId)
        if (!project) {
            return
        }
        const moduleName = contents[projectId]?.modules[path]
        if (moduleName && onOpenModule) {
            onOpenModule(project, moduleName)
        } else {
            onOpenFile?.(project, path)
        }
    }

    const refresh = () => {
        generation.current += 1
        reading.current.clear()
        rereading.current.clear()
        setContents({})
        // When the screen supplies the projects, it owns the read: ask it to refresh and hand them down.
        // The controlled shape guarantees a refresh, so a supplied tree never falls back to reading itself.
        if (controlled) {
            onRefresh?.()
            return
        }
        invalidateProjectIndex()
        setSelfProjects(null)
        load()
    }

    const applyGrouping = (next: GroupingLevels) => {
        setLevels(next)
        setExpanded([])
        setOpenedOn(null)
        setGrouping(false)
    }

    const body = () => {
        if (projects === null) {
            return <div className={styles.state}><Skeleton active paragraph={{ rows: 6 }} title={false} /></div>
        }
        if (shownError) {
            return (
                <div className={styles.state}>
                    <Alert showIcon data-testid="projects-tree-error" title={shownError} type="error" />
                </div>
            )
        }
        if (projects.length === 0) {
            return (
                <div className={styles.state}>
                    <Empty data-testid="projects-tree-empty" description={t('home.tree.empty')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
                </div>
            )
        }
        if (visible.length === 0) {
            return (
                <div className={styles.state}>
                    <Empty
                        data-testid="projects-tree-filtered-out"
                        description={t('home.no_match')}
                        image={Empty.PRESENTED_IMAGE_SIMPLE}
                    />
                </div>
            )
        }
        if (nodes.length === 0) {
            return (
                <div className={styles.state}>
                    <Empty
                        data-testid="projects-tree-no-match"
                        description={t('home.tree.no_match')}
                        image={Empty.PRESENTED_IMAGE_SIMPLE}
                    />
                </div>
            )
        }
        return (
            <div className={styles.body}>
                <Tree
                    ref={treeRef}
                    blockNode
                    showIcon
                    className={cx(shared.railTree, styles.tree)}
                    data-testid="projects-tree"
                    expandedKeys={openKeys}
                    loadedKeys={loadedKeys}
                    // A branch opens at once: the height the rows slide down with is painted by the page.
                    motion={false}
                    onExpand={keys => setExpanded(keys as string[])}
                    selectedKeys={selected}
                    treeData={treeData}
                    loadData={node => {
                        const project = findNode(nodes, String(node.key))?.project
                        return project ? loadContents(project) : Promise.resolve()
                    }}
                    onSelect={(keys, info) => {
                        const key = String(info.node.key)
                        setSelected(keys as string[])
                        // A file opens where it is read, a folder on the page of its project.
                        const picked = fileKeyParts(key)
                        if (picked) {
                            openFile(picked.projectId, picked.path)
                            return
                        }
                        const node = findNode(nodes, key)
                        if (!node) {
                            return
                        }
                        if (node.project) {
                            onOpenProject(node.project)
                        } else {
                            // A group answers what it holds: the list shows exactly its projects, and
                            // the tree remembers where the user was, whichever screen they end up on.
                            selectedNode.write(key)
                            onOpenGroup(node.filters)
                        }
                    }}
                />
            </div>
        )
    }

    return (
        <>
            <div className={shared.railHead}>
                <Button
                    className={styles.title}
                    data-testid="projects-tree-all"
                    size="small"
                    type="link"
                    onClick={() => {
                        setSelected([])
                        selectedNode.write('')
                        onShowAll()
                    }}
                >
                    {t('home.tree.title')}
                </Button>
                <span>
                    <Tooltip title={t('home.tree.group')}>
                        <Button
                            aria-label={t('home.tree.group')}
                            data-testid="projects-tree-group"
                            icon={<PartitionOutlined />}
                            onClick={() => setGrouping(true)}
                            size="small"
                            type="text"
                        />
                    </Tooltip>
                    <Tooltip title={t('home.tree.refresh')}>
                        <Button
                            aria-label={t('home.tree.refresh')}
                            data-testid="projects-tree-refresh"
                            icon={<ReloadOutlined />}
                            onClick={refresh}
                            size="small"
                            type="text"
                        />
                    </Tooltip>
                    {headerActions}
                </span>
            </div>
            {filtered && (
                <ClearFiltersRow
                    data-testid="projects-tree-clear-filters"
                    hint={t('home.tree.filtered')}
                    onClick={onClearFilters}
                />
            )}
            <div className={shared.railScroll}>{body()}</div>
            <GroupProjectsModal
                levels={levels}
                onApply={applyGrouping}
                onClose={() => setGrouping(false)}
                open={grouping}
                tagTypes={tagTypes}
            />
        </>
    )
}

interface TreeNodeData {
    key: string
    title: ReactNode
    icon: ReactNode
    isLeaf?: boolean
    selectable?: boolean
    children?: TreeNodeData[]
}
