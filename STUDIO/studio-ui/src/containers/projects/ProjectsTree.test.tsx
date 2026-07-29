import { act, cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ProjectsTree } from './ProjectsTree'
import { getProjectIndex } from '../../services/projectIndex'
import { ProjectStatus } from '../../constants/project'
import type { Project } from '../../types/projects'
import type { Repository } from '../../types/repositories'

vi.mock('../../services/projectIndex', () => ({
    getProjectIndex: vi.fn(),
    invalidateProjectIndex: vi.fn(),
}))

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

vi.mock('antd-style', () => ({
    createStyles: () => () => ({
        styles: new Proxy({}, { get: (_target, name) => String(name) }),
        cx: (...args: unknown[]) => args.filter(Boolean).join(' '),
    }),
    useTheme: () => new Proxy({}, { get: (_target, name) => String(name) }),
}))

// The icons carry the test ids the workspace marks them with, so they are forwarded.
vi.mock('@ant-design/icons', () => {
    const icon = (name: string) => (props: Record<string, unknown>) => <span {...props}>{name}</span>
    return {
        BranchesOutlined: icon('branches'),
        CloudUploadOutlined: icon('cloud-upload'),
        DatabaseOutlined: icon('database'),
        DeleteOutlined: icon('delete'),
        EditOutlined: icon('edit'),
        FolderOpenOutlined: icon('folder-open'),
        FolderOutlined: icon('folder'),
        HddOutlined: icon('hdd'),
        HistoryOutlined: icon('history'),
        PartitionOutlined: icon('partition'),
        ReloadOutlined: icon('reload'),
        TagOutlined: icon('tag'),
    }
})

vi.mock('../../components/SearchInput', () => ({
    SearchInput: ({ onChange, value, ...rest }: Record<string, unknown>) => {
        const { placeholder, ...dom } = rest
        void placeholder
        return <input onChange={onChange as never} value={value as string} {...dom} />
    },
}))
const { groupModalMock } = vi.hoisted(() => ({ groupModalMock: vi.fn() }))
vi.mock('./GroupProjectsModal', () => ({
    GroupProjectsModal: (props: { tagTypes: string[] }) => {
        groupModalMock(props)
        return null
    },
}))

// AntD's Tree does not settle in jsdom; a nested list keeps the assertions on the component itself.
// The caret and the node are separate controls here, as they are in the real tree: picking a node does
// not unfold it.
vi.mock('antd', () => {
    interface Node { key: string, title: unknown, icon?: unknown, children?: Node[] }
    const renderNodes = (
        nodes: Node[],
        expanded: string[],
        onSelect: (key: string) => void,
        onToggle: (key: string) => void
    ) => (
        <ul>
            {nodes.map(node => (
                <li key={node.key}>
                    {node.children && node.children.length > 0 && (
                        <button data-testid={`caret-${node.key}`} onClick={() => onToggle(node.key)} type="button">
                            caret
                        </button>
                    )}
                    <button onClick={() => onSelect(node.key)} type="button">
                        {node.icon as never}
                        {node.title as never}
                    </button>
                    {expanded.includes(node.key) && node.children
                        && renderNodes(node.children, expanded, onSelect, onToggle)}
                </li>
            ))}
        </ul>
    )
    const Tree = ({ treeData, expandedKeys, onSelect, onExpand, ...rest }: Record<string, unknown>) => {
        const { blockNode, className, selectedKeys, showIcon, expandAction, ...dom } = rest
        void blockNode; void className; void selectedKeys; void showIcon; void expandAction
        const expanded = (expandedKeys as string[]) ?? []
        const select = (key: string) =>
            (onSelect as (keys: unknown, info: unknown) => void)([key], { node: { key } })
        const toggle = (key: string) => {
            const next = expanded.includes(key) ? expanded.filter(item => item !== key) : [...expanded, key]
            ;(onExpand as (keys: unknown) => void)(next)
        }
        return <div {...dom}>{renderNodes(treeData as Node[], expanded, select, toggle)}</div>
    }
    const Button = ({ icon, onClick, ...rest }: Record<string, unknown>) => {
        const { size, type, ...dom } = rest
        void size; void type
        return <button onClick={onClick as never} {...dom}>{icon as never}</button>
    }
    const Tooltip = ({ children }: Record<string, unknown>) => <>{children as never}</>
    const Empty = ({ description, ...rest }: Record<string, unknown>) => {
        const { image, ...dom } = rest
        void image
        return <div {...dom}>{description as never}</div>
    }
    Empty.PRESENTED_IMAGE_SIMPLE = 'simple'
    const Skeleton = () => <div data-testid="tree-skeleton" />
    const Alert = ({ title, ...rest }: Record<string, unknown>) => {
        const { showIcon, type, ...dom } = rest
        void showIcon; void type
        return <div {...dom}>{title as never}</div>
    }
    const Typography = {
        Text: ({ children, ...rest }: Record<string, unknown>) => {
            const { ellipsis, className, ...dom } = rest
            void ellipsis; void className
            return <span {...dom}>{children as never}</span>
        },
    }
    return { Alert, Button, Empty, Skeleton, Tooltip, Tree: Object.assign(Tree, { DirectoryTree: Tree }), Typography }
})

const repositories = [
    { id: 'design', name: 'Design', type: 'repo-jdbc' },
    { id: 'flat', name: 'Git Flat', type: 'repo-git' },
] as unknown as Repository[]

const projects = [
    { id: 'p1', name: 'Alpha', repository: 'design', status: ProjectStatus.Editing, tags: { Domain: 'Policy' } },
    { id: 'p2', name: 'Beta', repository: 'flat', status: ProjectStatus.Closed, tags: { lob: 'Auto' } },
] as unknown as Project[]

const renderTree = async (props: Partial<Parameters<typeof ProjectsTree>[0]> = {}) => {
    const onOpenProject = vi.fn()
    const onOpenGroup = vi.fn()
    const onShowAll = vi.fn()
    render(
        <ProjectsTree
            onOpenGroup={onOpenGroup}
            onOpenProject={onOpenProject}
            onShowAll={onShowAll}
            repositories={repositories}
            {...props}
        />
    )
    // The mount-time loads land asynchronously; flush them before the assertions read the screen.
    await act(async () => {
        await new Promise(resolve => setTimeout(resolve, 0))
    })
    return { onOpenProject, onOpenGroup, onShowAll }
}

/** The grouping is remembered in the browser; each test starts from its own, empty memory. */
const stubStorage = () => {
    const store: Record<string, string> = {}
    vi.stubGlobal('localStorage', {
        getItem: (key: string) => store[key] ?? null,
        setItem: (key: string, value: string) => { store[key] = value },
        removeItem: (key: string) => { delete store[key] },
        clear: () => Object.keys(store).forEach(key => delete store[key]),
    })
}

describe('ProjectsTree', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        stubStorage()
        vi.mocked(getProjectIndex).mockResolvedValue({ projects, statuses: [], projectIndexHealth: {} })
    })

    it('reads the projects once and groups them by repository by default', async () => {
        await renderTree()

        expect(getProjectIndex).toHaveBeenCalledTimes(1)
        // A fresh visitor sees repositories, not a flat wall of every project.
        expect(screen.getByText('Design')).toBeInTheDocument()
        expect(screen.getByText('Git Flat')).toBeInTheDocument()
        expect(screen.queryByTestId('tree-project-p1')).not.toBeInTheDocument()
    })

    it('unfolds a remembered group without the repository list and without looping', async () => {
        // The project-page rail passes no repositories; an unstable default here once cascaded through
        // the grouping memos into an endless expand-effect loop (React error #185).
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['[Repository]', '', '']))
        localStorage.setItem('openl.projects.tree.selected', 'grp/[Repository]=design')

        await renderTree({ repositories: undefined })

        expect(screen.getByTestId('tree-project-p1')).toBeInTheDocument()
    })

    it('names repository groups from the projects when the repository list is unreadable', async () => {
        // Granted single projects only, the user reads no repositories — the groups still carry names.
        vi.mocked(getProjectIndex).mockResolvedValue({
            projects: [
                {
                    id: 'p1', name: 'Alpha', repository: 'design', status: ProjectStatus.Closed,
                    repositoryInfo: { id: 'design', name: 'Design', type: 'repo-jdbc' },
                },
            ] as unknown as Project[],
            statuses: [],
            projectIndexHealth: {},
        })

        await renderTree({ repositories: undefined })

        expect(screen.getByText('Design')).toBeInTheDocument()
    })

    it('lists every project flat once the user picks None', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        await renderTree()

        expect(screen.getByTestId('tree-project-p1')).toBeInTheDocument()
        expect(screen.getByTestId('tree-project-p2')).toBeInTheDocument()
    })

    it('opens the project the user picks', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        const { onOpenProject } = await renderTree()

        await userEvent.click(screen.getByTestId('tree-project-p1'))

        expect(onOpenProject).toHaveBeenCalledWith(expect.objectContaining({ id: 'p1' }))
    })

    it('groups by the levels the user stored, and a group shows what it holds without unfolding', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['[Repository]', '', '']))

        const { onOpenGroup } = await renderTree()

        expect(screen.getByText('Design')).toBeInTheDocument()
        expect(screen.getByText('Git Flat')).toBeInTheDocument()
        expect(screen.queryByTestId('tree-project-p1')).not.toBeInTheDocument()

        await userEvent.click(screen.getByText('Design'))

        // Picking a group asks the screen for its projects and leaves the tree as it was.
        expect(onOpenGroup).toHaveBeenCalledWith({ repositories: ['design'], tags: []})
        expect(screen.queryByTestId('tree-project-p1')).not.toBeInTheDocument()

        // Unfolding is what the caret is for.
        await userEvent.click(screen.getByTestId('caret-grp/[Repository]=design'))
        expect(screen.getByTestId('tree-project-p1')).toBeInTheDocument()
    })

    it('remembers the group the user picked and opens the tree on it next time', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['[Repository]', '', '']))
        await renderTree()

        await userEvent.click(screen.getByText('Design'))
        expect(localStorage.getItem('openl.projects.tree.selected')).toBe('grp/[Repository]=design')

        cleanup()
        await renderTree()

        // The remembered group is unfolded, so the user lands where they left off.
        expect(screen.getByTestId('tree-project-p1')).toBeInTheDocument()
    })

    it('leads back to every project by its title, forgetting the pick', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['[Repository]', '', '']))
        const { onShowAll } = await renderTree()

        await userEvent.click(screen.getByText('Design'))
        await userEvent.click(screen.getByTestId('projects-tree-all'))

        expect(onShowAll).toHaveBeenCalled()
        expect(localStorage.getItem('openl.projects.tree.selected')).toBeNull()
    })

    it('opens the groups holding the project the screen shows', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['[Repository]', '', '']))

        await renderTree({ currentProjectId: 'p2' })

        expect(screen.getByTestId('tree-project-p2')).toBeInTheDocument()
        expect(screen.queryByTestId('tree-project-p1')).not.toBeInTheDocument()
    })

    it('marks a repository group with the icon of its kind, and a tag group with a tag', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['[Repository]', 'Domain', '']))

        await renderTree()

        // The same icons the rest of the workspace uses: a database for the DB repository, a branch for Git.
        expect(screen.getByTestId('repo-badge-database')).toBeInTheDocument()
        expect(screen.getByTestId('repo-badge-git')).toBeInTheDocument()

        await userEvent.click(screen.getByTestId('caret-grp/[Repository]=design'))
        expect(screen.getByTestId('tree-tag-icon-Policy')).toBeInTheDocument()
    })

    it('searches the tree, keeping a group that matches whole', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['[Repository]', '', '']))

        await renderTree()

        // A repository answers by its own name: its projects come along, already unfolded.
        await userEvent.type(screen.getByTestId('projects-tree-search'), 'design')
        expect(screen.getByTestId('tree-project-p1')).toBeInTheDocument()
        expect(screen.queryByText('Git Flat')).not.toBeInTheDocument()

        // A project answers for itself: only the branch leading to it stays.
        await userEvent.clear(screen.getByTestId('projects-tree-search'))
        await userEvent.type(screen.getByTestId('projects-tree-search'), 'beta')
        expect(screen.getByTestId('tree-project-p2')).toBeInTheDocument()
        expect(screen.queryByTestId('tree-project-p1')).not.toBeInTheDocument()
    })

    it('draws a project by the state it is in', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        await renderTree()

        // The same icon that marks the name of a project being edited elsewhere in the workspace.
        expect(screen.getByTestId('tree-status-EDITING')).toHaveTextContent('edit')
        expect(screen.getByTestId('tree-status-CLOSED')).toHaveTextContent('folder')
    })

    it('says so when nothing matches the search', async () => {
        await renderTree()

        await userEvent.type(screen.getByTestId('projects-tree-search'), 'nothing here')

        expect(screen.getByTestId('projects-tree-no-match')).toBeInTheDocument()
    })

    it('says so when the projects cannot be read', async () => {
        vi.mocked(getProjectIndex).mockRejectedValue(new Error('nope'))

        await renderTree()

        expect(screen.getByTestId('projects-tree-error')).toHaveTextContent('nope')
    })


    it('offers the tags the projects carry as grouping levels, not the configured catalog', async () => {
        await renderTree()

        // Every tag key found on the projects, deduplicated and sorted — the same set the filters count.
        expect(groupModalMock).toHaveBeenCalledWith(expect.objectContaining({ tagTypes: ['Domain', 'lob']}))
    })
})
