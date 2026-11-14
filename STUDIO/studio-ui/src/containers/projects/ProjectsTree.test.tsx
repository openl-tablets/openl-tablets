import { act, cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ProjectsTree } from './ProjectsTree'
import { getProjectIndex } from '../../services/projectIndex'
import { getProject, getProjectFiles } from '../../services/repositories'
import { listModules } from '../../services/modules'
import { ProjectStatus } from '../../constants/project'
import type { Project } from '../../types/projects'
import type { FsNode } from '../../types/files'
import type { Repository } from '../../types/repositories'
import { listingQueryOf } from './projectListing'
import { hasSearchScope, searchFor, searchScope } from '../../testing/search'

vi.mock('../../services/repositories', () => ({ getProject: vi.fn(), getProjectFiles: vi.fn() }))
vi.mock('../../services/modules', () => ({ listModules: vi.fn() }))

vi.mock('../../services/projectIndex', async importOriginal => ({
    getProjectIndex: vi.fn(),
    invalidateProjectIndex: vi.fn(),
    projectSignature: (await importOriginal<typeof import('../../services/projectIndex')>()).projectSignature,
}))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    const i18n = { language: 'en', resolvedLanguage: 'en' }
    return { useTranslation: () => ({ i18n, t }) }
})

// antd-style hands out new styles and a new token on every render, and so does this mock: only their values follow
// the appearance. Every tree the Tree is handed is kept, to tell a tree rebuilt from one kept.
const { appearance, drawnTrees, treeScrollTo } = vi.hoisted(() => ({
    appearance: { current: 'light' },
    drawnTrees: [] as unknown[],
    // What the tree was asked to bring into sight.
    treeScrollTo: vi.fn(),
}))

vi.mock('antd-style', () => {
    // antd-style's cx reads every argument as a style object, and a null one throws: so does this one.
    const cx = (...args: unknown[]) => {
        if (args.includes(null)) {
            throw new TypeError("right-hand side of 'in' should be an object, got null")
        }
        return args.filter(Boolean).join(' ')
    }
    return {
        createStyles: () => () => ({ styles: new Proxy({}, { get: (_target, name) => String(name) }), cx }),
        useTheme: () => new Proxy({}, { get: () => (appearance.current === 'dark' ? '#020202' : '#010101') }),
        useThemeMode: () => ({ isDarkMode: appearance.current === 'dark' }),
    }
})

// The icons carry the test ids the workspace marks them with, so they are forwarded.
vi.mock('@ant-design/icons', () => {
    const icon = (name: string) => (props: Record<string, unknown>) => <span {...props}>{name}</span>
    return {
        BranchesOutlined: icon('branches'),
        ClearOutlined: icon('clear'),
        SafetyOutlined: icon('safety'),
        CloudUploadOutlined: icon('cloud-upload'),
        DatabaseOutlined: icon('database'),
        DeleteOutlined: icon('delete'),
        EditOutlined: icon('edit'),
        FileExcelOutlined: icon('file-excel'),
        FileOutlined: icon('file'),
        FileTextOutlined: icon('file-text'),
        FolderOpenOutlined: icon('folder-open'),
        FolderOutlined: icon('folder'),
        HddOutlined: icon('hdd'),
        HistoryOutlined: icon('history'),
        PartitionOutlined: icon('partition'),
        ReloadOutlined: icon('reload'),
        TagOutlined: icon('tag'),
    }
})

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
    interface Node { key: string, title: unknown, icon?: unknown, isLeaf?: boolean, selectable?: boolean, children?: Node[] }
    const renderNodes = (
        nodes: Node[],
        expanded: string[],
        onSelect: (key: string) => void,
        onToggle: (key: string) => void,
        selected: string[] = []
    ) => (
        <ul>
            {nodes.map(node => (
                <li
                    key={node.key}
                    data-expanded={expanded.includes(node.key)}
                    data-selected={selected.includes(node.key)}
                    data-testid={`node-${node.key}`}
                >
                    {node.isLeaf !== true && (
                        <button data-testid={`caret-${node.key}`} onClick={() => onToggle(node.key)} type="button">
                            caret
                        </button>
                    )}
                    <button disabled={node.selectable === false} onClick={() => onSelect(node.key)} type="button">
                        {node.icon as never}
                        {node.title as never}
                    </button>
                    {expanded.includes(node.key) && node.children
                        && renderNodes(node.children, expanded, onSelect, onToggle, selected)}
                </li>
            ))}
        </ul>
    )
    const Tree = ({ treeData, expandedKeys, onSelect, onExpand, loadData, ref, ...rest }: Record<string, unknown>) => {
        drawnTrees.push(treeData)
        if (ref && typeof ref === 'object') {
            (ref as { current: unknown }).current = { scrollTo: treeScrollTo }
        }
        const { blockNode, className, loadedKeys, motion, selectedKeys, showIcon, expandAction, ...dom } = rest
        void blockNode; void className; void motion; void showIcon; void expandAction
        const expanded = (expandedKeys as string[]) ?? []
        const loaded = (loadedKeys as string[]) ?? []
        const select = (key: string) =>
            (onSelect as (keys: unknown, info: unknown) => void)([key], { node: { key } })
        const toggle = (key: string) => {
            // The real tree reads what a row holds the first time it is opened; so does this one.
            if (!expanded.includes(key) && !loaded.includes(key)) {
                void (loadData as ((node: { key: string }) => Promise<void>) | undefined)?.({ key })
            }
            const next = expanded.includes(key) ? expanded.filter(item => item !== key) : [...expanded, key]
            ;(onExpand as (keys: unknown) => void)(next)
        }
        return <div {...dom}>{renderNodes(treeData as Node[], expanded, select, toggle, (selectedKeys as string[]) ?? [])}</div>
    }
    const Button = ({ icon, onClick, ...rest }: Record<string, unknown>) => {
        const { size, type, ...dom } = rest
        void size; void type
        return <button onClick={onClick as never} {...dom}>{icon as never}</button>
    }
    // A hint is kept where a test can read it, without adding text to the row.
    const Tooltip = ({ children, title }: Record<string, unknown>) => (
        typeof title === 'string' ? <span data-tooltip={title}>{children as never}</span> : <>{children as never}</>
    )
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
    const Tag = ({ children, ...rest }: Record<string, unknown>) =>
        <span data-testid={rest['data-testid'] as string}>{children as never}</span>
    return { Alert, Button, Empty, Skeleton, Tag, Tooltip, Tree: Object.assign(Tree, { DirectoryTree: Tree }), Typography }
})

const repositories = [
    { id: 'design', name: 'Design', type: 'repo-jdbc' },
    { id: 'flat', name: 'Git Flat', type: 'repo-git' },
] as unknown as Repository[]

const projects = [
    { id: 'p1', name: 'Alpha', repository: 'design', status: ProjectStatus.Editing, tags: { Domain: 'Policy' } },
    { id: 'p2', name: 'Beta', repository: 'flat', status: ProjectStatus.Closed, tags: { lob: 'Auto' } },
] as unknown as Project[]

/** A read of the files of a project that the test answers itself, to see what the tree shows meanwhile. */
const deferredFiles = () => {
    let resolve!: (files: FsNode[]) => void
    const promise = new Promise<FsNode[]>(done => { resolve = done })
    return { promise, resolve }
}

/** The picks of the Filters view, written the way the list carries them in its address. */
const picks = (query = '') => listingQueryOf(new URLSearchParams(query))

const renderTree = async (props: Partial<Parameters<typeof ProjectsTree>[0]> = {}) => {
    const onOpenProject = vi.fn()
    const onOpenGroup = vi.fn()
    const onShowAll = vi.fn()
    const onClearFilters = vi.fn()
    const onOpenModule = vi.fn()
    const onOpenFile = vi.fn()
    const result = render(
        <ProjectsTree
            filters={picks()}
            onClearFilters={onClearFilters}
            onOpenFile={onOpenFile}
            onOpenGroup={onOpenGroup}
            onOpenModule={onOpenModule}
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
    return { onOpenProject, onOpenGroup, onShowAll, onClearFilters, onOpenModule, onOpenFile, rerender: result.rerender }
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
        appearance.current = 'light'
        drawnTrees.length = 0
        vi.mocked(getProjectIndex).mockResolvedValue({ projects, statuses: [], projectIndexHealth: {} })
        vi.mocked(getProjectFiles).mockResolvedValue([
            { path: 'rules', name: 'rules', basePath: '', type: 'folder' },
            { path: 'rules/Main.xlsx', name: 'Main.xlsx', basePath: 'rules', type: 'file', size: 12_800 },
            { path: 'rules.xml', name: 'rules.xml', basePath: '', type: 'file', size: 1536 },
        ])
        vi.mocked(listModules).mockResolvedValue([{ name: 'Main', path: 'rules/Main.xlsx' }])
        vi.mocked(getProject).mockResolvedValue({ id: 'p1', name: 'Alpha', compileStatus: { pendingChanges: { files: []} } } as never)
    })

    it('reads the projects once and groups them by repository by default', async () => {
        await renderTree()

        expect(getProjectIndex).toHaveBeenCalledTimes(1)
        // A fresh visitor sees repositories, not a flat wall of every project.
        expect(screen.getByText('Design')).toBeInTheDocument()
        expect(screen.getByText('Git Flat')).toBeInTheDocument()
        expect(screen.queryByTestId('tree-project-p1')).not.toBeInTheDocument()
    })

    it('uses the projects the screen supplies, without reading the index a second time', async () => {
        await renderTree({ projects })

        // The list already read the snapshot; the tree groups that one instead of pulling it again.
        expect(getProjectIndex).not.toHaveBeenCalled()
        expect(screen.getByText('Design')).toBeInTheDocument()
        expect(screen.getByText('Git Flat')).toBeInTheDocument()
    })

    it('shows the skeleton while the supplier is still loading its projects', async () => {
        await renderTree({ projects: null })

        expect(getProjectIndex).not.toHaveBeenCalled()
        expect(screen.queryByText('Design')).not.toBeInTheDocument()
    })

    it('asks the screen to refresh instead of reading the index when the projects are supplied', async () => {
        const onRefresh = vi.fn()
        render(
            <ProjectsTree
                filters={picks()}
                onClearFilters={vi.fn()}
                onOpenGroup={vi.fn()}
                onOpenProject={vi.fn()}
                onRefresh={onRefresh}
                onShowAll={vi.fn()}
                projects={projects}
                repositories={repositories}
            />
        )
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })

        await userEvent.click(screen.getByTestId('projects-tree-refresh'))

        expect(onRefresh).toHaveBeenCalledTimes(1)
        expect(getProjectIndex).not.toHaveBeenCalled()
    })

    it('unfolds a remembered group without the repository list and without looping', async () => {
        // The project-page rail passes no repositories; an unstable default here once cascaded through
        // the grouping memos into an endless expand-effect loop (React error #185).
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['[Repository]', '', '']))
        localStorage.setItem('openl.projects.tree.selected', 'grp/[Repository]=design')

        // The group was picked, so its filters stand.
        await renderTree({ repositories: undefined, filters: picks('repo=design') })

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

    it('leaves a picked project folded: only its caret opens it', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        const { onOpenProject, rerender } = await renderTree({ currentProjectId: 'p1' })

        await userEvent.click(screen.getByTestId('tree-project-p2'))
        // The screen moves on to the project picked, and the tree beside it stays.
        rerender(
            <ProjectsTree
                currentProjectId="p2"
                filters={picks()}
                onClearFilters={vi.fn()}
                onOpenGroup={vi.fn()}
                onOpenProject={onOpenProject}
                onShowAll={vi.fn()}
                repositories={repositories}
            />
        )
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })

        expect(onOpenProject).toHaveBeenCalledWith(expect.objectContaining({ id: 'p2' }))
        expect(screen.getByTestId('node-grp/prj:p2')).toHaveAttribute('data-expanded', 'false')
    })

    it('opens on the file a link names, unfolding what leads to it, and picks it', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['[Repository]', '', '']))
        await renderTree({ currentProjectId: 'p1', currentFile: 'rules/Main.xlsx' })
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })
        await act(async () => { await new Promise(resolve => requestAnimationFrame(() => resolve(undefined))) })

        expect(getProjectFiles).toHaveBeenCalledTimes(1)
        expect(screen.getByTestId('tree-file-p1-rules/Main.xlsx')).toBeInTheDocument()
        expect(screen.getByTestId('node-file:p1:rules/Main.xlsx')).toHaveAttribute('data-selected', 'true')
        expect(treeScrollTo).toHaveBeenCalledWith({ key: 'file:p1:rules/Main.xlsx', align: 'auto' })
    })

    it('reads the files again when a refresh drops their first read, and still opens on the file', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        const first = deferredFiles()
        vi.mocked(getProjectFiles).mockReturnValueOnce(first.promise)
        await renderTree({
            currentFile: 'rules/Main.xlsx',
            currentProjectId: 'p1',
            onRefresh: vi.fn(),
            projects,
        })

        // The reader refreshes the tree while the files the link needs are on their way.
        await userEvent.click(screen.getByTestId('projects-tree-refresh'))
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })
        await act(async () => first.resolve([]))
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })

        expect(getProjectFiles).toHaveBeenCalledTimes(2)
        expect(screen.getByTestId('node-file:p1:rules/Main.xlsx')).toHaveAttribute('data-selected', 'true')
    })

    it('opens on the file once, leaving what the reader folds afterwards folded', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        await renderTree({ currentProjectId: 'p1', currentFile: 'rules/Main.xlsx' })
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })

        await userEvent.click(screen.getByTestId('caret-folder:p1:rules'))

        expect(screen.queryByTestId('tree-file-p1-rules/Main.xlsx')).not.toBeInTheDocument()
    })

    it('picks no file once the screen shows none, and opens on the file again when it is shown again', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        const shown = {
            currentProjectId: 'p1',
            filters: picks(),
            onClearFilters: vi.fn(),
            onOpenGroup: vi.fn(),
            onOpenProject: vi.fn(),
            onShowAll: vi.fn(),
            repositories,
        }
        const { rerender } = await renderTree({ currentProjectId: 'p1', currentFile: 'rules/Main.xlsx' })
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })
        expect(screen.getByTestId('node-file:p1:rules/Main.xlsx')).toHaveAttribute('data-selected', 'true')

        // A tab of the project takes the place of the file, and the reader folds the folder of the file.
        rerender(<ProjectsTree {...shown} />)
        expect(screen.getByTestId('node-file:p1:rules/Main.xlsx')).toHaveAttribute('data-selected', 'false')
        await userEvent.click(screen.getByTestId('caret-folder:p1:rules'))
        expect(screen.queryByTestId('tree-file-p1-rules/Main.xlsx')).not.toBeInTheDocument()

        // A link to the same file opens the tree on it again.
        rerender(<ProjectsTree {...shown} currentFile="rules/Main.xlsx" />)
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })
        expect(screen.getByTestId('node-file:p1:rules/Main.xlsx')).toHaveAttribute('data-selected', 'true')
    })

    it('reads the files of a project when its row is opened, once', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        await renderTree()

        // Nothing is read until the reader asks for it.
        expect(getProjectFiles).not.toHaveBeenCalled()

        await userEvent.click(screen.getByTestId('caret-grp/prj:p1'))
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })

        expect(getProjectFiles).toHaveBeenCalledWith('p1')
        expect(screen.getByTestId('tree-file-p1-rules.xml')).toBeInTheDocument()
        // The folders the paths describe are there too, holding what is under them.
        await userEvent.click(screen.getByTestId('caret-folder:p1:rules'))
        expect(screen.getByTestId('tree-file-p1-rules/Main.xlsx')).toBeInTheDocument()

        // Folding and opening it again reads nothing a second time.
        await userEvent.click(screen.getByTestId('caret-grp/prj:p1'))
        await userEvent.click(screen.getByTestId('caret-grp/prj:p1'))
        expect(getProjectFiles).toHaveBeenCalledTimes(1)
    })

    it('opens a file that is a module in the editor, and any other file on the page of its project', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        const { onOpenModule, onOpenFile } = await renderTree()

        await userEvent.click(screen.getByTestId('caret-grp/prj:p1'))
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })
        await userEvent.click(screen.getByTestId('caret-folder:p1:rules'))

        await userEvent.click(screen.getByTestId('tree-file-p1-rules/Main.xlsx'))
        expect(onOpenModule).toHaveBeenCalledWith(expect.objectContaining({ id: 'p1' }), 'Main')

        await userEvent.click(screen.getByTestId('tree-file-p1-rules.xml'))
        expect(onOpenFile).toHaveBeenCalledWith(expect.objectContaining({ id: 'p1' }), 'rules.xml')
    })

    it('shows the size of each file', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        await renderTree()

        await userEvent.click(screen.getByTestId('caret-grp/prj:p1'))
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })
        await userEvent.click(screen.getByTestId('caret-folder:p1:rules'))

        expect(screen.getByTestId('tree-file-p1-rules.xml')).toHaveTextContent('rules.xml1.5 kB')
        expect(screen.getByTestId('tree-file-p1-rules/Main.xlsx')).toHaveTextContent('Main.xlsx13 kB')
        // A folder has no size of its own.
        expect(screen.getByTestId('tree-folder-p1-rules')).toHaveTextContent(/^rules$/)
    })

    it('opens a picked folder on the page of its project, leaving it folded', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        const { onOpenFile, onOpenModule } = await renderTree()

        await userEvent.click(screen.getByTestId('caret-grp/prj:p1'))
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })
        await userEvent.click(screen.getByTestId('tree-folder-p1-rules'))

        expect(onOpenFile).toHaveBeenCalledWith(expect.objectContaining({ id: 'p1' }), 'rules')
        expect(onOpenModule).not.toHaveBeenCalled()
        expect(screen.getByTestId('node-folder:p1:rules')).toHaveAttribute('data-expanded', 'false')
    })

    it('marks the files of the current project changed and not saved yet, and the folders holding them', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        const changes = new Map([['rules/Main.xlsx', 'modified' as const], ['rules.xml', 'added' as const]])
        await renderTree({ currentProjectId: 'p1', currentFileChanges: changes })

        await userEvent.click(screen.getByTestId('caret-grp/prj:p1'))
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })
        await userEvent.click(screen.getByTestId('caret-folder:p1:rules'))

        const main = screen.getByTestId('tree-file-p1-rules/Main.xlsx')
        expect(main).toHaveAttribute('data-change', 'modified')
        expect(main.parentElement).toHaveAttribute('data-tooltip', 'browser.files.change_hint.modified')
        expect(screen.getByTestId('tree-file-p1-rules.xml')).toHaveAttribute('data-change', 'added')
        expect(screen.getByTestId('tree-folder-p1-rules')).toHaveAttribute('data-change', 'folder')
    })

    it('keeps the files deleted from the current project as rows that open nothing', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        const changes = new Map([['rules/Old.xlsx', 'deleted' as const], ['old/Gone.txt', 'deleted' as const]])
        const { onOpenFile } = await renderTree({ currentProjectId: 'p1', currentFileChanges: changes })

        await userEvent.click(screen.getByTestId('caret-grp/prj:p1'))
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })
        await userEvent.click(screen.getByTestId('caret-folder:p1:rules'))
        await userEvent.click(screen.getByTestId('caret-folder:p1:old'))

        const old = screen.getByTestId('tree-file-p1-rules/Old.xlsx')
        expect(old).toHaveAttribute('data-change', 'deleted')
        expect(old.parentElement).toHaveAttribute('data-tooltip', 'browser.files.change_hint.deleted')
        // No size: the file is no longer there.
        expect(old).toHaveTextContent(/^Old\.xlsx$/)
        await userEvent.click(old)
        expect(onOpenFile).not.toHaveBeenCalled()
        // A folder left with nothing but deleted files is deleted too; one keeping a file only holds a change.
        expect(screen.getByTestId('tree-folder-p1-old')).toHaveAttribute('data-change', 'deleted')
        expect(screen.getByTestId('tree-folder-p1-rules')).toHaveAttribute('data-change', 'folder')
    })

    it('marks a folder holding only added files as added, and one that also holds older files as changed', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        vi.mocked(getProjectFiles).mockResolvedValue([
            { path: 'rules/Main.xlsx', name: 'Main.xlsx', basePath: 'rules', type: 'file' },
            { path: 'rules/New.xlsx', name: 'New.xlsx', basePath: 'rules', type: 'file' },
            { path: 'docs/a.txt', name: 'a.txt', basePath: 'docs', type: 'file' },
            { path: 'docs/b.txt', name: 'b.txt', basePath: 'docs', type: 'file' },
        ])
        const changes = new Map([
            ['rules/New.xlsx', 'added' as const],
            ['docs/a.txt', 'added' as const],
            ['docs/b.txt', 'added' as const],
        ])
        await renderTree({ currentProjectId: 'p1', currentFileChanges: changes })

        await userEvent.click(screen.getByTestId('caret-grp/prj:p1'))
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })

        const docs = screen.getByTestId('tree-folder-p1-docs')
        expect(docs).toHaveAttribute('data-change', 'added')
        expect(docs.parentElement).toHaveAttribute('data-tooltip', 'browser.files.change_hint.added')
        expect(screen.getByTestId('tree-folder-p1-rules')).toHaveAttribute('data-change', 'folder')
    })

    it('marks a folder a file was renamed in as changed, not as a new folder', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        vi.mocked(getProjectFiles).mockResolvedValue([
            { path: 'docs/b.md', name: 'b.md', basePath: 'docs', type: 'file' },
        ])
        // A file renamed is deleted under its old name and added under the new one.
        const changes = new Map([
            ['docs/a.md', 'deleted' as const],
            ['docs/b.md', 'added' as const],
        ])
        await renderTree({ currentProjectId: 'p1', currentFileChanges: changes })

        await userEvent.click(screen.getByTestId('caret-grp/prj:p1'))
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })

        expect(screen.getByTestId('tree-folder-p1-docs')).toHaveAttribute('data-change', 'folder')
    })

    it('takes the changes of the project the screen shows from the screen, not reading them again', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        const changes = new Map([['rules.xml', 'modified' as const]])
        const { rerender } = await renderTree({ currentProjectId: 'p1', currentFileChanges: changes, reloadToken: 1 })

        await userEvent.click(screen.getByTestId('caret-grp/prj:p1'))
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })
        // The screen read its project again: the tree reads the files again, the changes come with the report.
        rerender(
            <ProjectsTree
                currentFileChanges={new Map([['rules.xml', 'added' as const]])}
                currentProjectId="p1"
                filters={picks()}
                onClearFilters={vi.fn()}
                onOpenGroup={vi.fn()}
                onOpenProject={vi.fn()}
                onShowAll={vi.fn()}
                reloadToken={2}
                repositories={repositories}
            />
        )
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })

        expect(getProjectFiles).toHaveBeenCalledTimes(2)
        expect(getProject).not.toHaveBeenCalled()
        expect(screen.getByTestId('tree-file-p1-rules.xml')).toHaveAttribute('data-change', 'added')
    })

    it('marks the changes of a project being edited beside the list too, read with its files', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        vi.mocked(getProject).mockResolvedValue({
            id: 'p1',
            name: 'Alpha',
            path: 'Alpha',
            compileStatus: { pendingChanges: { files: [
                { path: 'Alpha/rules.xml', type: 'modified' },
                { path: 'Alpha/old.txt', type: 'deleted' },
            ]} },
        } as never)
        await renderTree({ projects, onRefresh: vi.fn() })

        await userEvent.click(screen.getByTestId('caret-grp/prj:p1'))
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })

        expect(getProject).toHaveBeenCalledWith('p1', { includes: ['status']}, expect.anything())
        expect(screen.getByTestId('tree-file-p1-rules.xml')).toHaveAttribute('data-change', 'modified')
        expect(screen.getByTestId('tree-file-p1-old.txt')).toHaveAttribute('data-change', 'deleted')
    })

    it('reads no changes of a project that is not being edited', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        await renderTree()

        await userEvent.click(screen.getByTestId('caret-grp/prj:p2'))
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })

        expect(getProject).not.toHaveBeenCalled()
    })

    it('reads the files of a project again once its state changed, as when it is saved', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        const { rerender } = await renderTree({ projects, onRefresh: vi.fn() })
        await userEvent.click(screen.getByTestId('caret-grp/prj:p1'))
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })
        expect(getProjectFiles).toHaveBeenCalledTimes(1)

        const saved = projects.map(project => (project.id === 'p1' ? { ...project, status: ProjectStatus.Opened } : project))
        rerender(
            <ProjectsTree
                filters={picks()}
                onClearFilters={vi.fn()}
                onOpenGroup={vi.fn()}
                onOpenProject={vi.fn()}
                onRefresh={vi.fn()}
                onShowAll={vi.fn()}
                projects={saved}
                repositories={repositories}
            />
        )
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })

        // Read again where it stands: the open row keeps its files and is told what is there now.
        expect(getProjectFiles).toHaveBeenCalledTimes(2)
        expect(screen.getByTestId('tree-file-p1-rules.xml')).toBeInTheDocument()
    })

    it('reads the files of a project again once it is on another branch, its state unchanged', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        const onBranch = (branch: string) => projects.map(project => ({ ...project, branch }))
        const shown = {
            filters: picks(),
            onClearFilters: vi.fn(),
            onOpenGroup: vi.fn(),
            onOpenProject: vi.fn(),
            onRefresh: vi.fn(),
            onShowAll: vi.fn(),
            repositories,
        }
        const { rerender } = await renderTree({ ...shown, projects: onBranch('main') })
        await userEvent.click(screen.getByTestId('caret-grp/prj:p1'))
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })
        expect(getProjectFiles).toHaveBeenCalledTimes(1)

        rerender(<ProjectsTree {...shown} projects={onBranch('feature')} />)
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })

        expect(getProjectFiles).toHaveBeenCalledTimes(2)
    })

    it('keeps the files read after a refresh over a read begun before it', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        const shown = {
            currentProjectId: 'p1',
            filters: picks(),
            onClearFilters: vi.fn(),
            onOpenGroup: vi.fn(),
            onOpenProject: vi.fn(),
            onRefresh: vi.fn(),
            onShowAll: vi.fn(),
            projects,
            repositories,
        }
        const { rerender } = await renderTree({ ...shown, reloadToken: 1 })
        await userEvent.click(screen.getByTestId('caret-grp/prj:p1'))
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })
        // A read again begins, and the reader refreshes the tree while it is on its way.
        const older = deferredFiles()
        vi.mocked(getProjectFiles).mockReturnValueOnce(older.promise)
        rerender(<ProjectsTree {...shown} reloadToken={2} />)
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })
        await userEvent.click(screen.getByTestId('projects-tree-refresh'))
        vi.mocked(getProjectFiles).mockResolvedValueOnce([{ path: 'fresh.txt', name: 'fresh.txt', basePath: '', type: 'file' }])
        await userEvent.click(screen.getByTestId('caret-grp/prj:p1'))
        await userEvent.click(screen.getByTestId('caret-grp/prj:p1'))
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })
        expect(screen.getByTestId('tree-file-p1-fresh.txt')).toBeInTheDocument()

        await act(async () => older.resolve([{ path: 'stale.txt', name: 'stale.txt', basePath: '', type: 'file' }]))

        expect(screen.getByTestId('tree-file-p1-fresh.txt')).toBeInTheDocument()
        expect(screen.queryByTestId('tree-file-p1-stale.txt')).toBeNull()
    })

    it('marks no file of a project the screen does not show', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        const changes = new Map([['rules.xml', 'added' as const]])
        await renderTree({ currentProjectId: 'p2', currentFileChanges: changes })

        await userEvent.click(screen.getByTestId('caret-grp/prj:p1'))
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })

        expect(screen.getByTestId('tree-file-p1-rules.xml')).not.toHaveAttribute('data-change')
    })

    it('lists the empty folders added to the current project until a file lands in them', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        await renderTree({ currentProjectId: 'p1', currentFolders: ['drafts/wip']})

        await userEvent.click(screen.getByTestId('caret-grp/prj:p1'))
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })

        expect(screen.getByTestId('tree-folder-p1-drafts')).toBeInTheDocument()
        // The empty folder has nothing to unfold, so it takes no arrow; the folder holding it does.
        expect(screen.getByTestId('caret-folder:p1:drafts')).toBeInTheDocument()
        expect(screen.queryByTestId('caret-folder:p1:drafts/wip')).not.toBeInTheDocument()
    })

    it('reads the files of the current project again once the screen reloads it', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        const { rerender } = await renderTree({ currentProjectId: 'p1', reloadToken: 1 })
        await userEvent.click(screen.getByTestId('caret-grp/prj:p1'))
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })
        expect(getProjectFiles).toHaveBeenCalledTimes(1)
        const reread = deferredFiles()
        vi.mocked(getProjectFiles).mockReturnValueOnce(reread.promise)

        rerender(
            <ProjectsTree
                currentProjectId="p1"
                filters={picks()}
                onClearFilters={vi.fn()}
                onOpenGroup={vi.fn()}
                onOpenProject={vi.fn()}
                onShowAll={vi.fn()}
                reloadToken={2}
                repositories={repositories}
            />
        )
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })

        // The open row keeps what it shows while the files are read again, so it neither empties nor jumps...
        expect(getProjectFiles).toHaveBeenCalledTimes(2)
        expect(screen.getByTestId('tree-file-p1-rules.xml')).toBeInTheDocument()

        // ...and lists what is there now once they arrive.
        await act(async () => reread.resolve([
            { path: 'rules.xml', name: 'rules.xml', basePath: '', type: 'file' },
            { path: 'notes.txt', name: 'notes.txt', basePath: '', type: 'file' },
        ]))
        expect(screen.getByTestId('tree-file-p1-notes.txt')).toBeInTheDocument()
    })

    it('reads the files of the current project again when the screen reloads it during a read', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        const first = deferredFiles()
        vi.mocked(getProjectFiles).mockReturnValueOnce(first.promise)
        const { rerender } = await renderTree({ currentProjectId: 'p1', reloadToken: 1 })
        await userEvent.click(screen.getByTestId('caret-grp/prj:p1'))

        // The screen changes the project while its files are on their way: what arrives was read before the change.
        rerender(
            <ProjectsTree
                currentProjectId="p1"
                filters={picks()}
                onClearFilters={vi.fn()}
                onOpenGroup={vi.fn()}
                onOpenProject={vi.fn()}
                onShowAll={vi.fn()}
                reloadToken={2}
                repositories={repositories}
            />
        )
        vi.mocked(getProjectFiles).mockResolvedValueOnce([
            { path: 'rules.xml', name: 'rules.xml', basePath: '', type: 'file' },
            { path: 'notes.txt', name: 'notes.txt', basePath: '', type: 'file' },
        ])
        await act(async () => first.resolve([{ path: 'rules.xml', name: 'rules.xml', basePath: '', type: 'file' }]))
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })

        expect(getProjectFiles).toHaveBeenCalledTimes(2)
        expect(screen.getByTestId('tree-file-p1-notes.txt')).toBeInTheDocument()
    })

    it('finds the files of an unfolded project by the search, opening the folders on the way', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        await renderTree()
        await userEvent.click(screen.getByTestId('caret-grp/prj:p1'))
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })
        await userEvent.click(screen.getByTestId('caret-grp/prj:p1'))

        searchFor('main.x')

        // The project stays for its file, folded rows open, and only what matched is listed.
        expect(screen.getByTestId('tree-project-p1')).toBeInTheDocument()
        expect(screen.queryByTestId('tree-project-p2')).not.toBeInTheDocument()
        expect(screen.getByTestId('tree-file-p1-rules/Main.xlsx')).toBeInTheDocument()
        expect(screen.queryByTestId('tree-file-p1-rules.xml')).not.toBeInTheDocument()
    })

    it('lists every file of a project found by its own name, those whose name holds the text too', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        vi.mocked(getProjectFiles).mockResolvedValue([
            { path: 'alpha-notes.txt', name: 'alpha-notes.txt', basePath: '', type: 'file' },
            { path: 'rules.xml', name: 'rules.xml', basePath: '', type: 'file' },
        ])
        await renderTree()
        await userEvent.click(screen.getByTestId('caret-grp/prj:p1'))
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })

        searchFor('alpha')

        expect(screen.getByTestId('tree-file-p1-alpha-notes.txt')).toBeInTheDocument()
        expect(screen.getByTestId('tree-file-p1-rules.xml')).toBeInTheDocument()
    })

    it('reads the files of a project that cannot say which of them are modules', async () => {
        // A closed project keeps no workspace copy, so it answers nothing about its modules; its
        // workbooks are still listed, and lead to the file rather than to the editor.
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        vi.mocked(listModules).mockRejectedValue(new Error('The project is not opened.'))
        const { onOpenModule, onOpenFile } = await renderTree()

        await userEvent.click(screen.getByTestId('caret-grp/prj:p2'))
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })
        await userEvent.click(screen.getByTestId('caret-folder:p2:rules'))

        await userEvent.click(screen.getByTestId('tree-file-p2-rules/Main.xlsx'))

        expect(onOpenModule).not.toHaveBeenCalled()
        expect(onOpenFile).toHaveBeenCalledWith(expect.objectContaining({ id: 'p2' }), 'rules/Main.xlsx')
    })

    it('says why the files of a project could not be read', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        vi.mocked(getProjectFiles).mockRejectedValue(new Error('no access'))
        await renderTree()

        await userEvent.click(screen.getByTestId('caret-grp/prj:p1'))
        await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })

        expect(screen.getByTestId('tree-files-error-p1')).toHaveTextContent('no access')
    })

    it('groups by the levels the user stored, and a group shows what it holds without unfolding', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['[Repository]', '', '']))

        const { onOpenGroup } = await renderTree()

        expect(screen.getByText('Design')).toBeInTheDocument()
        expect(screen.getByText('Git Flat')).toBeInTheDocument()
        expect(screen.queryByTestId('tree-project-p1')).not.toBeInTheDocument()

        await userEvent.click(screen.getByText('Design'))

        // Picking a group asks the screen for its projects and leaves the tree as it was.
        expect(onOpenGroup).toHaveBeenCalledWith({ repositories: ['design'], branches: [], tags: []})
        expect(screen.queryByTestId('tree-project-p1')).not.toBeInTheDocument()

        // Unfolding is what the caret is for.
        await userEvent.click(screen.getByTestId('caret-grp/[Repository]=design'))
        expect(screen.getByTestId('tree-project-p1')).toBeInTheDocument()
    })

    it('groups by the branch, marks each group with the icon and its default/protected marks, and opens it', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['[Branch]', '', '']))
        const branched = [
            { id: 'p1', name: 'Alpha', repository: 'design', status: ProjectStatus.Editing, branch: 'main', branchDefault: true },
            { id: 'p2', name: 'Beta', repository: 'flat', status: ProjectStatus.Closed, branch: 'feature/x', branchProtected: true },
        ] as unknown as Project[]

        const { onOpenGroup } = await renderTree({ projects: branched })

        expect(screen.getByText('main')).toBeInTheDocument()
        expect(screen.getByTestId('tree-branch-icon-main')).toBeInTheDocument()
        // Each branch group wears the same marks it wears on a project row.
        expect(screen.getByTestId('tree-branch-label-main-default')).toBeInTheDocument()
        expect(screen.getByTestId('tree-branch-label-feature/x-protected')).toBeInTheDocument()

        await userEvent.click(screen.getByText('main'))

        expect(onOpenGroup).toHaveBeenCalledWith({ repositories: [], branches: ['main'], tags: []})
    })

    it('marks a branch group from its own projects, so a same-named branch in another repository stays unmarked', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['[Repository]', '[Branch]', '']))
        const branched = [
            { id: 'p1', name: 'Alpha', repository: 'design', status: ProjectStatus.Editing, branch: 'main', branchDefault: true },
            { id: 'p2', name: 'Beta', repository: 'flat', status: ProjectStatus.Closed, branch: 'main', branchDefault: false },
        ] as unknown as Project[]

        await renderTree({ projects: branched })

        // Reveal the branch groups nested under each repository.
        await userEvent.click(screen.getByTestId('caret-grp/[Repository]=design'))
        await userEvent.click(screen.getByTestId('caret-grp/[Repository]=flat'))

        // Both repositories rendered a 'main' branch group, but only the design repository's main is default;
        // the flat repository's same-named main wears no mark.
        expect(screen.getAllByTestId('tree-branch-icon-main')).toHaveLength(2)
        expect(screen.getAllByTestId('tree-branch-label-main-default')).toHaveLength(1)
    })

    it('remembers the group the user picked and opens the tree on it next time', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['[Repository]', '', '']))
        await renderTree()

        await userEvent.click(screen.getByText('Design'))
        expect(localStorage.getItem('openl.projects.tree.selected')).toBe('grp/[Repository]=design')

        cleanup()
        // Picking the group set its filters, which the tree comes back with.
        await renderTree({ filters: picks('repo=design') })

        // The remembered group is unfolded, so the user lands where they left off.
        expect(screen.getByTestId('tree-project-p1')).toBeInTheDocument()
    })

    it('forgets the picked group once the filters are cleared, wherever they were cleared', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['[Repository]', '', '']))
        localStorage.setItem('openl.projects.tree.selected', 'grp/[Repository]=design')
        const props = {
            onClearFilters: vi.fn(),
            onOpenGroup: vi.fn(),
            onOpenProject: vi.fn(),
            onShowAll: vi.fn(),
            projects,
            onRefresh: vi.fn(),
            repositories,
        }
        const { rerender } = render(<ProjectsTree {...props} filters={picks('repo=design')} />)
        expect(localStorage.getItem('openl.projects.tree.selected')).toBe('grp/[Repository]=design')

        rerender(<ProjectsTree {...props} filters={picks()} />)

        expect(localStorage.getItem('openl.projects.tree.selected')).toBeNull()
    })

    it('shows the projects the filters select, and the project the screen shows whatever they say', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        const { rerender } = await renderTree({ projects, onRefresh: vi.fn(), filters: picks('repo=flat') })

        expect(screen.getByTestId('tree-project-p2')).toBeInTheDocument()
        expect(screen.queryByTestId('tree-project-p1')).not.toBeInTheDocument()

        rerender(
            <ProjectsTree
                currentProjectId="p1"
                filters={picks('repo=flat')}
                onClearFilters={vi.fn()}
                onOpenGroup={vi.fn()}
                onOpenProject={vi.fn()}
                onRefresh={vi.fn()}
                onShowAll={vi.fn()}
                projects={projects}
                repositories={repositories}
            />
        )

        // The reader is on that project: it stays in the tree, which leads on to the rest.
        expect(screen.getByTestId('tree-project-p1')).toBeInTheDocument()
        expect(screen.getByTestId('tree-project-p2')).toBeInTheDocument()
    })

    it('narrows to its own picks when a group picked in it filtered the list only', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        await renderTree({ projects, onRefresh: vi.fn(), filters: picks('repo=flat'), narrowBy: picks() })

        // The list shows the group; the tree stays whole, and still offers to clear what filters the list.
        expect(screen.getByTestId('tree-project-p1')).toBeInTheDocument()
        expect(screen.getByTestId('tree-project-p2')).toBeInTheDocument()
        expect(screen.getByTestId('projects-tree-clear-filters')).toBeInTheDocument()
    })

    it('keeps to the default branches until a branch is picked', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        const git = { id: 'design', name: 'Design', features: { branches: true, searchable: true, mappedFolders: false } }
        const branched = [
            { id: 'p1', name: 'Alpha', repository: 'design', status: ProjectStatus.Closed, branch: 'main', inDefaultBranch: true, repositoryInfo: git },
            { id: 'p3', name: 'Gamma', repository: 'design', status: ProjectStatus.Closed, branch: 'feature/x', repositoryInfo: git },
        ] as unknown as Project[]

        const { rerender } = await renderTree({ projects: branched, onRefresh: vi.fn() })

        expect(screen.getByTestId('tree-project-p1')).toBeInTheDocument()
        expect(screen.queryByTestId('tree-project-p3')).not.toBeInTheDocument()

        rerender(
            <ProjectsTree
                filters={picks('branch=feature/x')}
                onClearFilters={vi.fn()}
                onOpenGroup={vi.fn()}
                onOpenProject={vi.fn()}
                onRefresh={vi.fn()}
                onShowAll={vi.fn()}
                projects={branched}
                repositories={repositories}
            />
        )

        expect(screen.getByTestId('tree-project-p3')).toBeInTheDocument()
        expect(screen.queryByTestId('tree-project-p1')).not.toBeInTheDocument()
    })

    it('offers to clear the filters only while a value is picked', async () => {
        const { onClearFilters } = await renderTree({ filters: picks('status=EDITING') })

        await userEvent.click(screen.getByTestId('projects-tree-clear-filters'))

        expect(onClearFilters).toHaveBeenCalledTimes(1)

        cleanup()
        await renderTree()

        expect(screen.queryByTestId('projects-tree-clear-filters')).not.toBeInTheDocument()
    })

    it('says so when the filters select no project', async () => {
        await renderTree({ filters: picks('status=OPENED') })

        expect(screen.getByTestId('projects-tree-filtered-out')).toHaveTextContent('home.no_match')
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
        searchFor('design')
        expect(screen.getByTestId('tree-project-p1')).toBeInTheDocument()
        expect(screen.queryByText('Git Flat')).not.toBeInTheDocument()

        // A project answers for itself: only the branch leading to it stays.
        searchFor('')
        searchFor('beta')
        expect(screen.getByTestId('tree-project-p2')).toBeInTheDocument()
        expect(screen.queryByTestId('tree-project-p1')).not.toBeInTheDocument()
    })

    it('repaints the state of a project in the theme the reader switches to', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        // Nothing but the theme changes: the same filters, the same projects.
        const filters = picks()
        const { rerender } = await renderTree({ filters, projects, onRefresh: vi.fn() })
        expect(screen.getByTestId('tree-status-EDITING')).toHaveStyle({ color: '#010101' })

        appearance.current = 'dark'
        rerender(
            <ProjectsTree
                filters={filters}
                onClearFilters={vi.fn()}
                onOpenGroup={vi.fn()}
                onOpenProject={vi.fn()}
                onRefresh={vi.fn()}
                onShowAll={vi.fn()}
                projects={projects}
                repositories={repositories}
            />
        )

        expect(screen.getByTestId('tree-status-EDITING')).toHaveStyle({ color: '#020202' })
    })

    it('keeps its tree while the theme stays, though the theme hands out new objects on every render', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        const filters = picks()
        const { rerender } = await renderTree({ filters, projects, onRefresh: vi.fn() })
        const built = drawnTrees.at(-1)

        rerender(
            <ProjectsTree
                filters={filters}
                onClearFilters={vi.fn()}
                onOpenGroup={vi.fn()}
                onOpenProject={vi.fn()}
                onRefresh={vi.fn()}
                onShowAll={vi.fn()}
                projects={projects}
                repositories={repositories}
            />
        )

        expect(drawnTrees.at(-1)).toBe(built)
    })

    it('draws a project by the state it is in', async () => {
        localStorage.setItem('openl.projects.grouping', JSON.stringify(['', '', '']))
        await renderTree()

        // The same icon that marks the name of a project being edited elsewhere in the workspace.
        expect(screen.getByTestId('tree-status-EDITING')).toHaveTextContent('edit')
        expect(screen.getByTestId('tree-status-CLOSED')).toHaveTextContent('folder')
    })

    it('offers itself to the search of the header as a panel, which the main part of a screen goes before', async () => {
        await renderTree()

        expect(searchScope('projects-tree')).toMatchObject({
            label: 'common:search.scopes.projects',
            placeholder: 'home.tree.search_placeholder',
            secondary: true,
        })
    })

    it('follows the text of the screen around it, and then asks for no search of its own', async () => {
        await renderTree({ search: 'beta' })

        expect(hasSearchScope('projects-tree')).toBe(false)
        expect(screen.getByTestId('tree-project-p2')).toBeInTheDocument()
        expect(screen.queryByTestId('tree-project-p1')).not.toBeInTheDocument()
    })

    it('says so when nothing matches the search', async () => {
        await renderTree()

        searchFor('nothing here')

        expect(screen.getByTestId('projects-tree-no-match')).toBeInTheDocument()
    })

    it('says so when the projects cannot be read', async () => {
        vi.mocked(getProjectIndex).mockRejectedValue(new Error('nope'))

        await renderTree()

        expect(screen.getByTestId('projects-tree-error')).toHaveTextContent('nope')
    })

    it('says why the screen supplying the projects could not read them', async () => {
        await renderTree({ projects: [], onRefresh: vi.fn(), error: 'down' })

        expect(screen.getByTestId('projects-tree-error')).toHaveTextContent('down')
        expect(screen.queryByTestId('projects-tree-empty')).toBeNull()
    })


    it('offers the tags the projects carry as grouping levels, not the configured catalog', async () => {
        await renderTree()

        // Every tag key found on the projects, deduplicated and sorted — the same set the filters count.
        expect(groupModalMock).toHaveBeenCalledWith(expect.objectContaining({ tagTypes: ['Domain', 'lob']}))
    })
})
