import { readStored, removeStored, writeStored } from '../../utils/localStore'
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { Alert, Button, Empty, Skeleton, Tooltip, Tree } from 'antd'
import { BranchesOutlined, PartitionOutlined, ReloadOutlined, TagOutlined } from '@ant-design/icons'
import { createStyles, useTheme } from 'antd-style'
import { useTranslation } from 'react-i18next'
import { errorMessage } from '../../utils/errorMessage'
import { getProjectIndex, invalidateProjectIndex } from '../../services/projectIndex'
import type { Project } from '../../types/projects'
import { getProjectFiles } from '../../services/repositories'
import { listModules } from '../../services/modules'
import { buildFileNodes, type FileNode } from './fileNodes'
import { iconFor } from './fileIcons'
import type { Repository } from '../../types/repositories'
import { STATUS_META } from '../../constants/projectStatusMeta'
import type { ProjectStatus } from '../../constants/project'
import { useSharedStyles } from './sharedStyles'
import { RepoIcon } from './RepoBadge'
import { BranchMarks } from './BranchMarks'
import { SearchInput } from '../../components/SearchInput'
import {
    activeLevels,
    buildGroupTree,
    findNode,
    GROUP_BY_BRANCH,
    GROUP_BY_REPOSITORY,
    groupKeys,
    pathToNode,
    searchTree,
    loadGrouping,
    pathToProject,
    saveGrouping,
    type GroupNode,
    type GroupingLevels,
    type NodeFilters,
} from './projectGrouping'
import { GroupProjectsModal } from './GroupProjectsModal'

const SELECTED_STORAGE_KEY = 'openl.projects.tree.selected'

/** The key of a file row: the project it belongs to, and the path inside it. Neither part holds a colon. */
const fileKey = (projectId: string, path: string): string => `file:${projectId}:${path}`

/** The project and path a file row stands for, or null when the key is not a file's. */
const fileKeyParts = (key: string): { projectId: string, path: string } | null => {
    if (!key.startsWith('file:')) {
        return null
    }
    const rest = key.slice('file:'.length)
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
    files: FileNode[]
    /** The name of the module a file is, by the file's path. */
    modules: Record<string, string>
    error?: string
}

const loadSelectedNode = (): string | null => readStored(SELECTED_STORAGE_KEY)

const saveSelectedNode = (key: string | null): void =>
    key === null ? removeStored(SELECTED_STORAGE_KEY) : writeStored(SELECTED_STORAGE_KEY, key)

const useStyles = createStyles(({ css, token }) => ({
    /**
     * The tree scrolls sideways instead of squeezing the names: a project is recognised by its full
     * name, and a wrapped or clipped one is neither readable nor comparable to the row above it.
     */
    body: css`
        padding: 4px 8px 12px;
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
    state: css`
        padding: 12px 16px;
    `,
    search: css`
        padding: 0 12px 8px;
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
 * `null` supplied means the screen is still loading — the tree shows its skeleton.
 */
export type ProjectsSource =
    | { projects: Project[] | null, onRefresh: () => void }
    | { projects?: undefined, onRefresh?: undefined }

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
    /** A file that is no module was picked: show it on the project's Files tab. */
    onOpenFile?: ((project: Project, path: string) => void) | undefined
    /** A group was picked: show the projects it holds. */
    onOpenGroup: (filters: NodeFilters) => void
    /** The title of the tree was picked: show every project again. */
    onShowAll: () => void
    /** Bumped by the screen when it changed the workspace, so the tree reads it again. */
    reloadToken?: number | undefined
    /** What the rail hangs on the header row, beside the actions of the tree itself. */
    headerActions?: ReactNode
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
    reloadToken,
    headerActions,
    projects: providedProjects,
    onRefresh,
}: ProjectsTreeProps) => {
    const { t } = useTranslation('repository')
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const token = useTheme()
    const [levels, setLevels] = useState<GroupingLevels>(loadGrouping)
    const [selfProjects, setSelfProjects] = useState<Project[] | null>(null)
    const [error, setError] = useState<string | null>(null)
    const [grouping, setGrouping] = useState(false)
    const [expanded, setExpanded] = useState<string[]>([])
    // The group the user picked last is remembered, so stepping into a project and back shows where
    // they were in the tree.
    const [selected, setSelected] = useState<string[]>(() => {
        const remembered = loadSelectedNode()
        return remembered ? [remembered] : []
    })
    const [search, setSearch] = useState('')
    const [openedOn, setOpenedOn] = useState<string | null>(null)
    // The files of the projects whose rows have been opened, read once each and kept while the tree stands.
    const [contents, setContents] = useState<Record<string, ProjectContents>>({})

    // The list supplies the projects it already read; only a single-project page lets the tree read them
    // itself, so the list screen no longer pulls the same /projects snapshot a second time.
    const controlled = providedProjects !== undefined
    const projects = controlled ? providedProjects : selfProjects

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
        () => buildGroupTree(projects ?? [], activeLevels(levels), repositoryName),
        [levels, projects, repositoryName]
    )
    // What the search found, with everything under a group that matched by its own name.
    const nodes = useMemo(() => searchTree(grouped, search), [grouped, search])
    // A search shows what it found straight away; without one the user decides what is open.
    const openKeys = useMemo(
        () => (search.trim() ? groupKeys(nodes) : expanded),
        [expanded, nodes, search]
    )

    // The group remembered from an earlier visit is unfolded once the tree knows its shape.
    useEffect(() => {
        const remembered = selected[0]
        if (!remembered || projects === null) {
            return
        }
        const path = pathToNode(grouped, remembered)
        if (path) {
            // Nothing new to unfold keeps the previous array: a fresh identity here would re-render,
            // re-run this effect and loop the tree into React's update-depth limit.
            setExpanded(previous => {
                const next = new Set([...previous, ...path, remembered])
                return next.size === previous.length ? previous : [...next]
            })
        }
        // Only on the first tree the projects build; afterwards the user decides what is open.
    }, [grouped, projects])

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
        // Depends on what shapes a node, not on which nodes are open — expanding must not rebuild the tree.
        () => nodes.map(node => toTreeNode(node)),
        [contents, currentProjectId, nodes, repoMeta]
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

    /**
     * Reads the files of a project, and the modules among them, the first time its row is opened.
     *
     * <p>Which files are modules is answered by a project that is open; a closed one cannot say, so its
     * files are all read as plain files rather than the read failing.
     */
    const loadContents = async (project: Project) => {
        if (contents[project.id]) {
            return
        }
        const [files, modules] = await Promise.all([
            getProjectFiles(project.id).then(
                read => read.filter(node => node.type === 'file').map(node => node.path),
                (e: unknown) => errorMessage(e)
            ),
            listModules(project.id).catch(() => []),
        ])
        setContents(previous => ({
            ...previous,
            [project.id]: typeof files === 'string'
                ? { files: [], modules: {}, error: files }
                : {
                    files: buildFileNodes(files),
                    modules: Object.fromEntries(modules.filter(module => module.path).map(module => [module.path, module.name])),
                },
        }))
    }

    // A file of a project reads as it does on the Files tab — the same icon, the same name.
    function fileNodesOf(project: Project, read: ProjectContents, files: FileNode[]): TreeNodeData[] {
        return files.map(file => {
            const { Icon, color } = iconFor(file.name, file.isFile, {
                success: token.colorSuccess,
                info: token.colorInfo,
                warning: token.colorWarning,
                muted: token.colorTextTertiary,
            })
            return {
                key: file.isFile ? fileKey(project.id, file.path) : `folder:${project.id}:${file.path}`,
                isLeaf: file.isFile,
                icon: <Icon style={{ color }} />,
                selectable: file.isFile,
                title: (
                    <span
                        className={styles.node}
                        data-testid={`tree-${file.isFile ? 'file' : 'folder'}-${project.id}-${file.path}`}
                    >
                        {file.name}
                    </span>
                ),
                ...(file.children ? { children: fileNodesOf(project, read, file.children) } : {}),
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
        return fileNodesOf(project, read, read.files)
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
            const status = project.status as ProjectStatus
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
     * Follows a file to where it is read: a module to the editor, anything else to the project's Files tab.
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
        saveGrouping(next)
        setExpanded([])
        setOpenedOn(null)
        setGrouping(false)
    }

    const body = () => {
        if (projects === null) {
            return <div className={styles.state}><Skeleton active paragraph={{ rows: 6 }} title={false} /></div>
        }
        if (error) {
            return (
                <div className={styles.state}>
                    <Alert showIcon data-testid="projects-tree-error" title={error} type="error" />
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
                    treeData={treeData as never}
                    loadData={node => {
                        const project = findNode(nodes, String(node.key))?.project
                        return project ? loadContents(project) : Promise.resolve()
                    }}
                    onSelect={(keys, info) => {
                        const key = String(info.node.key)
                        setSelected(keys as string[])
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
                            saveSelectedNode(key)
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
                        saveSelectedNode(null)
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
            <div className={styles.search}>
                <SearchInput
                    data-testid="projects-tree-search"
                    onChange={event => setSearch(event.target.value)}
                    placeholder={t('home.tree.search_placeholder')}
                    value={search}
                />
            </div>
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
