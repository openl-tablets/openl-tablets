import { readStored, removeStored, writeStored } from '../../utils/localStore'
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { Alert, Button, Empty, Skeleton, Tooltip, Tree } from 'antd'
import { PartitionOutlined, ReloadOutlined, TagOutlined } from '@ant-design/icons'
import { createStyles, useTheme } from 'antd-style'
import { useTranslation } from 'react-i18next'
import { errorMessage } from '../../utils/errorMessage'
import { getProjectIndex, invalidateProjectIndex } from '../../services/projectIndex'
import type { Project } from '../../types/projects'
import type { Repository } from '../../types/repositories'
import { STATUS_META } from '../../constants/projectStatusMeta'
import type { ProjectStatus } from '../../constants/project'
import { useSharedStyles } from './sharedStyles'
import { RepoIcon } from './RepoBadge'
import { SearchInput } from '../../components/SearchInput'
import {
    activeLevels,
    buildGroupTree,
    findNode,
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
    tree: css`
        background: transparent;
        width: max-content;
        min-width: 100%;

        .ant-tree-treenode {
            padding-bottom: 0;
            white-space: nowrap;
            align-items: center;
        }

        /* The rail is narrow: every step of the hierarchy costs width, so it stays small. */
        .ant-tree-indent-unit {
            width: 12px;
        }

        .ant-tree-switcher {
            width: 18px;
            line-height: 24px;
        }

        .ant-tree-node-content-wrapper {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            min-height: 24px;
            line-height: 24px;
            padding: 0 4px;
            overflow: visible;
        }

        /* A name is read in full, on one line: the section scrolls sideways instead of clipping it. */
        .ant-tree-title,
        .ant-tree-node-content-wrapper .ant-tree-title {
            overflow: visible;
            text-overflow: clip;
            white-space: nowrap;
        }

        .ant-tree-iconEle {
            width: auto;
            line-height: 24px;
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

interface ProjectsTreeProps {
    repositories: Repository[]
    /** The project the screen is showing, highlighted and opened in the tree. */
    currentProjectId?: string | undefined
    onOpenProject: (project: Project) => void
    /** A group was picked: show the projects it holds. */
    onOpenGroup: (filters: NodeFilters) => void
    /** The title of the tree was picked: show every project again. */
    onShowAll: () => void
    /** Bumped by the screen when it changed the workspace, so the tree reads it again. */
    reloadToken?: number | undefined
    /** What the rail hangs on the header row, beside the actions of the tree itself. */
    headerActions?: ReactNode
}

/**
 * The projects as a tree, grouped by up to three levels the user picks — a repository or a tag type.
 *
 * The tree reads one lightweight list of projects the first time it is opened and groups it in the
 * browser, so expanding a node costs nothing and the screen around it never waits for the tree.
 */
export const ProjectsTree = ({
    repositories,
    currentProjectId,
    onOpenProject,
    onOpenGroup,
    onShowAll,
    reloadToken,
    headerActions,
}: ProjectsTreeProps) => {
    const { t } = useTranslation('repository')
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const token = useTheme()
    const [levels, setLevels] = useState<GroupingLevels>(loadGrouping)
    const [projects, setProjects] = useState<Project[] | null>(null)
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

    const load = useCallback(() => {
        setError(null)
        getProjectIndex()
            .then(index => setProjects(index.projects))
            .catch((e: unknown) => {
                setProjects([])
                setError(errorMessage(e))
            })
    }, [])

    useEffect(load, [load, reloadToken])

    const repositoryName = useCallback(
        (id: string) => repositories.find(repo => repo.id === id)?.name ?? id,
        [repositories]
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
            setExpanded(previous => [...new Set([...previous, ...path, remembered])])
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
        [currentProjectId, nodes, repositories]
    )

    function toTreeNode(node: GroupNode): TreeNodeData {
        if (node.project) {
            const project = node.project
            const current = project.id === currentProjectId
            const status = project.status as ProjectStatus
            const meta = STATUS_META[status]
            const Icon = meta.icon
            return {
                key: node.key,
                isLeaf: true,
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
            // A repository carries its own icon — a branch, a database, a disk — as it does everywhere
            // else, and a tag value carries the tag it is.
            icon: node.groupedBy === GROUP_BY_REPOSITORY
                ? <RepoIcon type={repositories.find(repo => repo.id === node.value)?.type} />
                : <TagOutlined data-testid={`tree-tag-icon-${node.value}`} />,
            title: (
                <span className={styles.node} data-testid={`tree-group-${node.key}`}>{node.title}</span>
            ),
            children: node.children.map(child => toTreeNode(child)),
        }
    }

    const refresh = () => {
        invalidateProjectIndex()
        setProjects(null)
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
                    className={styles.tree}
                    data-testid="projects-tree"
                    expandedKeys={openKeys}
                    onExpand={keys => setExpanded(keys as string[])}
                    selectedKeys={selected}
                    treeData={treeData as never}
                    onSelect={(keys, info) => {
                        const key = String(info.node.key)
                        setSelected(keys as string[])
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
    children?: TreeNodeData[]
}
