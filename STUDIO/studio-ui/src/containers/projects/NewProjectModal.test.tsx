import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { NewProjectModal } from './NewProjectModal'
import {
    copyProject,
    createProject,
    createProjectsFromWorkspace,
    getDesignRepositoryBranches,
    getDesignRepositoryConfig,
    getProjects,
    getProjectTemplates,
} from '../../services/repositories'
import { inspectOpenLArchive, zipProjectFolder } from '../../utils/openlArchive'

vi.mock('../../services/repositories', () => ({
    copyProject: vi.fn(),
    createProject: vi.fn(),
    createProjectsFromWorkspace: vi.fn(),
    getDesignRepositoryBranches: vi.fn(),
    getDesignRepositoryConfig: vi.fn(),
    getProjects: vi.fn(),
    getProjectTemplates: vi.fn(),
    getRepositoryConfig: vi.fn(),
}))

vi.mock('../../utils/openlArchive', () => ({ inspectOpenLArchive: vi.fn(), zipProjectFolder: vi.fn() }))

vi.mock('./RepoFolderPicker', () => ({ RepoFolderPicker: () => null }))

vi.mock('../../components/SuggestInput', () => ({
    SuggestInput: ({ onChange, options, value, ...rest }: {
        onChange: (value: string) => void
        options: { label: string, value: string }[]
        value: string
    }) => (
        <div>
            <input
                {...rest}
                onChange={event => onChange(event.target.value)}
                value={value}
            />
            {options.map(option => <span key={option.value}>{option.label}</span>)}
        </div>
    ),
}))

vi.mock('react-i18next', () => {
    const translations: Record<string, string> = {
        'browser.create.openapi_defaults.data_module_name': 'LocalizedModels',
        'browser.create.openapi_defaults.data_module_path': 'rules/LocalizedModels.xlsx',
        'browser.create.openapi_defaults.rules_module_name': 'LocalizedAlgorithms',
        'browser.create.openapi_defaults.rules_module_path': 'rules/LocalizedAlgorithms.xlsx',
        'browser.create.branch_invalid': 'Enter a valid Git branch name that matches the repository pattern',
    }
    const t = (key: string) => translations[key] ?? key
    return { useTranslation: () => ({ t }) }
})

vi.mock('antd-style', () => ({
    createStyles: () => () => ({
        styles: new Proxy({}, { get: () => '' }),
        cx: (...args: unknown[]) => args.filter(Boolean).join(' '),
    }),
    useTheme: () => new Proxy({}, { get: () => '#000' }),
}))

vi.mock('antd', () => {
    const Modal = ({ open, children, footer }: Record<string, unknown>) =>
        open ? <div>{children as never}{footer as never}</div> : null
    const Button = ({ children, onClick, icon, ...rest }: Record<string, unknown>) => {
        const { type, loading, danger, ...dom } = rest
        void type; void loading; void danger
        return <button onClick={onClick as never} {...dom}>{icon as never}{children as never}</button>
    }
    const Input = ({ onChange, addonAfter, ...rest }: Record<string, unknown>) => {
        void addonAfter
        return <input onChange={onChange as never} {...rest} />
    }
    Input.TextArea = ({ onChange, autoSize, ...rest }: Record<string, unknown>) => {
        void autoSize
        return <textarea onChange={onChange as never} {...rest} />
    }
    const folderBatch = (root: string): File[] => {
        const file = new File(['<project><name>Folder Proj</name></project>'], 'rules.xml')
        Object.defineProperty(file, 'webkitRelativePath', { value: `${root}/rules.xml` })
        return [file]
    }
    const Dragger = ({ children, beforeUpload, directory }: Record<string, unknown>) => {
        const before = beforeUpload as (f: File, batch: File[]) => void
        return (
            <div>
                {directory ? (
                    <>
                        <button data-testid="pick-folder" onClick={() => { const b = folderBatch('FromFolder'); before(b[0]!, b) }}>
                            pick folder
                        </button>
                        <button data-testid="pick-folder-2" onClick={() => { const b = folderBatch('Second'); before(b[0]!, b) }}>
                            pick folder 2
                        </button>
                    </>
                ) : (
                    <button
                        data-testid="pick-file"
                        onClick={() => { const f = new File(['x'], 'proj.zip', { type: 'application/zip' }); before(f, [f]) }}
                    >
                        pick
                    </button>
                )}
                {children as never}
            </div>
        )
    }
    const Upload = { Dragger, LIST_IGNORE: 'LIST_IGNORE' }
    const Segmented = ({ options, onChange, ...rest }: Record<string, unknown>) => (
        <div data-testid={rest['data-testid'] as string}>
            {((options as { value: string, label: string }[]) ?? []).map(option => (
                <button
                    key={option.value}
                    data-testid={`seg-${option.value}`}
                    onClick={() => (onChange as (v: unknown) => void)(option.value)}
                >
                    {option.label}
                </button>
            ))}
        </div>
    )
    const Alert = ({ title, showIcon, type, ...rest }: Record<string, unknown>) => {
        void showIcon; void type
        return <div {...rest}>{title as never}</div>
    }
    interface Opt { value: string, label: string, options?: Opt[] }
    const Select = ({ options, onChange, mode, ...rest }: Record<string, unknown>) => {
        const flat = ((options as Opt[]) ?? []).flatMap(option => option.options ?? [option])
        return (
            <div data-testid={rest['data-testid'] as string}>
                {flat.map(option => (
                    <button
                        key={option.value}
                        data-testid={`opt-${option.value}`}
                        onClick={() => (onChange as (v: unknown) => void)(mode === 'multiple' ? [option.value] : option.value)}
                    >
                        {option.label}
                    </button>
                ))}
            </div>
        )
    }
    interface TNode { value: string, title: string, disabled?: boolean, children?: TNode[] }
    const flattenTree = (nodes: TNode[]): TNode[] => nodes.flatMap(node => [node, ...(node.children ? flattenTree(node.children) : [])])
    const TreeSelect = ({ treeData, onChange, ...rest }: Record<string, unknown>) => (
        <div data-testid={rest['data-testid'] as string}>
            {flattenTree((treeData as TNode[]) ?? []).map(node => (
                <button key={node.value} data-testid={`opt-${node.value}`} disabled={node.disabled} onClick={() => (onChange as (v: unknown) => void)(node.value)}>
                    {node.title}
                </button>
            ))}
        </div>
    )
    const Checkbox = ({ children, checked, onChange, ...rest }: Record<string, unknown>) => {
        const { style, indeterminate, disabled, ...dom } = rest
        void style; void indeterminate; void disabled
        return (
            <label {...dom}>
                <input checked={checked as boolean} onChange={onChange as never} readOnly={onChange === undefined} type="checkbox" />
                {children as never}
            </label>
        )
    }
    const Space = ({ children }: Record<string, unknown>) => <div>{children as never}</div>
    Space.Compact = ({ children }: Record<string, unknown>) => <div>{children as never}</div>
    const Tooltip = ({ children }: Record<string, unknown>) => children as never
    const Typography = {
        Text: ({ children, type, ...rest }: Record<string, unknown>) => {
            void type
            return <span {...rest}>{children as never}</span>
        },
    }
    return { Modal, Button, Checkbox, Input, Upload, Alert, Segmented, Select, Space, Tooltip, TreeSelect, Typography }
})

const repositories = [{ id: 'design', name: 'Design', aclId: 'a', capabilities: { canCreateProject: true } }]
const mappedRepositories = [
    {
        id: 'design',
        name: 'Design',
        aclId: 'a',
        capabilities: { canCreateProject: true },
        features: { branches: false, searchable: false, mappedFolders: true },
    },
]
const branchingRepositories = [
    {
        id: 'design',
        name: 'Design',
        aclId: 'a',
        capabilities: { canCreateProject: true },
        features: { branches: true, searchable: true, mappedFolders: false },
    },
]

const renderWizard = (props: Record<string, unknown> = {}) =>
    render(<NewProjectModal open onClose={vi.fn()} onCreated={vi.fn()} repositories={repositories as never} {...props} />)

/** Click a method tile, which advances to the config step in a single click. */
const toConfig = async (methodId = 'template') => {
    await userEvent.click(screen.getByTestId(`new-project-method-${methodId}`))
}

describe('NewProjectModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(copyProject).mockResolvedValue()
        vi.mocked(createProject).mockResolvedValue()
        vi.mocked(getProjects).mockResolvedValue({ content: [], pageNumber: 0, pageSize: 0, numberOfElements: 0 } as never)
        vi.mocked(getProjectTemplates).mockResolvedValue([{ type: 'predefined', category: 'General', templates: ['Example']}])
        vi.mocked(inspectOpenLArchive).mockResolvedValue({ readable: false, isOpenLProject: false, name: 'proj' })
        vi.mocked(getDesignRepositoryConfig).mockResolvedValue({ comment: { templates: {} } })
        vi.mocked(getDesignRepositoryBranches).mockResolvedValue(['main', 'feature/rates'])
    })

    it.each(['template', 'archive', 'excel', 'openapi', 'workspace', 'copy'])(
        'shows one shared branch selector for the %s creation method',
        async method => {
            vi.mocked(getDesignRepositoryConfig).mockResolvedValue({
                branch: 'main',
                comment: { templates: {} },
            })
            renderWizard({ repositories: branchingRepositories })

            await toConfig(method)

            await waitFor(() => expect(screen.getByTestId('new-project-branch')).toHaveValue('main'))
            expect(screen.getAllByTestId('new-project-branch')).toHaveLength(1)
            expect(screen.getByTestId('new-project-branch')).toHaveStyle({ width: '100%' })
        }
    )

    it('clears the form when the create method is switched', async () => {
        renderWizard()

        await toConfig('archive')
        await userEvent.type(screen.getByTestId('new-project-name'), 'Typed name')
        expect((screen.getByTestId('new-project-name') as HTMLInputElement).value).toBe('Typed name')

        // Go back and pick a different method: the previously typed name must not linger.
        await userEvent.click(screen.getByTestId('new-project-back'))
        await toConfig('excel')

        expect((screen.getByTestId('new-project-name') as HTMLInputElement).value).toBe('')
    })

    it('advances to the config step on a single method click, without a Next button', async () => {
        renderWizard()

        // Method selection is now a single click — the separate "select + Next" step is gone.
        expect(screen.queryByTestId('new-project-next')).toBeNull()
        expect(screen.queryByTestId('new-project-submit')).toBeNull()

        await userEvent.click(screen.getByTestId('new-project-method-archive'))

        // A single click lands directly on the archive config: its submit action and file picker show.
        expect(await screen.findByTestId('new-project-submit')).toBeTruthy()
        expect(screen.getByTestId('pick-file')).toBeTruthy()
    })

    it('suggests the create comment the target repository configures, following the typed name', async () => {
        vi.mocked(getDesignRepositoryConfig).mockResolvedValue({
            comment: { templates: { create: 'Project {project-name} was created' } },
        })
        renderWizard()

        await toConfig()
        await userEvent.type(screen.getByTestId('new-project-name'), 'Alpha')

        await waitFor(() => expect((screen.getByTestId('new-project-comment') as HTMLTextAreaElement).value)
            .toBe('Project Alpha was created'))
    })

    it('refuses a comment the target repository pattern forbids', async () => {
        vi.mocked(getDesignRepositoryConfig).mockResolvedValue({
            comment: { userMessagePattern: 'EPBDS-\\d+.*', invalidUserMessageHint: 'Start with a ticket', templates: {} },
        })
        renderWizard()

        await toConfig()
        await userEvent.type(screen.getByTestId('new-project-name'), 'Alpha')
        await userEvent.type(screen.getByTestId('new-project-comment'), 'no ticket')

        // The repository words the rejection itself, and the wizard shows it on the field.
        await waitFor(() => expect(screen.getByTestId('new-project-comment-error'))
            .toHaveTextContent('Start with a ticket'))
        await userEvent.click(screen.getByTestId('new-project-submit'))
        expect(createProject).not.toHaveBeenCalled()
    })

    it('validates that a name is required', async () => {
        const onCreated = vi.fn()
        renderWizard({ onCreated })

        await toConfig()
        await userEvent.click(screen.getByTestId('new-project-submit'))

        expect(screen.getByTestId('new-project-error')).toBeTruthy()
        expect(createProject).not.toHaveBeenCalled()
        expect(onCreated).not.toHaveBeenCalled()
    })

    it('requires an archive before creating in archive mode', async () => {
        renderWizard()

        await toConfig('archive')
        await userEvent.type(screen.getByTestId('new-project-name'), 'Alpha')
        await userEvent.click(screen.getByTestId('new-project-submit'))

        expect(screen.getByTestId('new-project-error')).toBeTruthy()
        expect(createProject).not.toHaveBeenCalled()
    })

    it('creates the project from the uploaded archive', async () => {
        const onCreated = vi.fn()
        renderWizard({ onCreated })

        await toConfig('archive')
        await userEvent.type(screen.getByTestId('new-project-name'), '  Alpha  ')
        await userEvent.click(screen.getByTestId('pick-file'))
        await userEvent.click(screen.getByTestId('new-project-submit'))

        await waitFor(() => expect(createProject).toHaveBeenCalledTimes(1))
        const [repoId, name, options] = vi.mocked(createProject).mock.calls[0]!
        expect(repoId).toBe('design')
        expect(name).toBe('Alpha')
        expect(options.files?.[0]).toBeInstanceOf(File)
        // Every source opens the new project, so an archive no longer lands closed.
        expect(options.status).toBe('OPENED')
        await waitFor(() => expect(onCreated).toHaveBeenCalled())
    })

    it('zips a picked folder and creates the project from it as an archive', async () => {
        const onCreated = vi.fn()
        renderWizard({ onCreated })

        vi.mocked(zipProjectFolder).mockResolvedValue(
            new File(['zip-bytes'], 'FromFolder.zip', { type: 'application/zip' }))
        vi.mocked(inspectOpenLArchive).mockResolvedValue({ readable: true, isOpenLProject: true, name: 'FromFolder' })

        await toConfig('archive')
        await userEvent.type(screen.getByTestId('new-project-name'), 'FromFolder')
        await userEvent.click(screen.getByTestId('seg-folder'))
        await userEvent.click(screen.getByTestId('pick-folder'))

        // The folder is zipped in the browser; the submit button stays disabled until that finishes.
        await waitFor(() => expect(screen.getByTestId('new-project-submit')).not.toBeDisabled())
        await userEvent.click(screen.getByTestId('new-project-submit'))

        await waitFor(() => expect(createProject).toHaveBeenCalledTimes(1))
        const [, name, options] = vi.mocked(createProject).mock.calls[0]!
        expect(name).toBe('FromFolder')
        expect(options.files?.[0]?.name).toMatch(/\.zip$/)
        expect(options.status).toBe('OPENED')
        await waitFor(() => expect(onCreated).toHaveBeenCalled())
    })

    it('re-zips the latest folder when the folder is changed, not the folders combined', async () => {
        vi.mocked(zipProjectFolder).mockResolvedValue(
            new File(['zip-bytes'], 'Second.zip', { type: 'application/zip' }))
        vi.mocked(inspectOpenLArchive).mockResolvedValue({ readable: true, isOpenLProject: true, name: 'Second' })
        renderWizard()

        await toConfig('archive')
        await userEvent.type(screen.getByTestId('new-project-name'), 'Second')
        await userEvent.click(screen.getByTestId('seg-folder'))
        await userEvent.click(screen.getByTestId('pick-folder'))
        await userEvent.click(screen.getByTestId('pick-folder-2'))
        await waitFor(() => expect(screen.getByTestId('new-project-submit')).not.toBeDisabled())

        // The last zip is built from the second folder alone — the first pick is replaced, not appended.
        const lastBatch = vi.mocked(zipProjectFolder).mock.calls.at(-1)![0] as File[]
        expect(lastBatch).toHaveLength(1)
        expect((lastBatch[0] as File).webkitRelativePath).toBe('Second/rules.xml')
    })

    it('offers only repositories with create capability as project targets', async () => {
        renderWizard({
            repositories: [
                ...repositories,
                { id: 'readonly', name: 'Read Only', aclId: 'b', capabilities: {} },
            ],
        })

        await toConfig('template')

        expect(screen.getByTestId('opt-design')).toBeTruthy()
        expect(screen.queryByTestId('opt-readonly')).toBeNull()
    })

    it('hides the repository path field when the target repository is flat', async () => {
        renderWizard()

        await toConfig('template')

        expect(screen.queryByTestId('new-project-path')).toBeNull()
    })

    it('requires an Excel file before creating in Excel mode', async () => {
        renderWizard()

        await toConfig('excel')
        await userEvent.type(screen.getByTestId('new-project-name'), 'Alpha')
        await userEvent.click(screen.getByTestId('new-project-submit'))

        expect(screen.getByTestId('new-project-error')).toBeTruthy()
        expect(createProject).not.toHaveBeenCalled()
    })

    it('creates a project from uploaded Excel files sent as raw files', async () => {
        const onCreated = vi.fn()
        renderWizard({ onCreated })

        await toConfig('excel')
        await userEvent.type(screen.getByTestId('new-project-name'), 'FromXls')
        await userEvent.click(screen.getByTestId('pick-file'))
        await userEvent.click(screen.getByTestId('new-project-submit'))

        await waitFor(() => expect(createProject).toHaveBeenCalledTimes(1))
        const [repoId, name, options] = vi.mocked(createProject).mock.calls[0]!
        expect(repoId).toBe('design')
        expect(name).toBe('FromXls')
        expect(options.files?.[0]).toBeInstanceOf(File)
        expect(options.template).toBeUndefined()
        await waitFor(() => expect(onCreated).toHaveBeenCalled())
    })

    it('creates a project from an OpenAPI file with localized default module settings', async () => {
        const onCreated = vi.fn()
        renderWizard({ onCreated })

        await toConfig('openapi')
        await userEvent.type(screen.getByTestId('new-project-name'), 'FromApi')
        await userEvent.click(screen.getByTestId('pick-file'))
        await userEvent.click(screen.getByTestId('new-project-submit'))

        await waitFor(() => expect(createProject).toHaveBeenCalledTimes(1))
        const [, name, options] = vi.mocked(createProject).mock.calls[0]!
        expect(name).toBe('FromApi')
        expect(options.files?.[0]).toBeInstanceOf(File)
        expect(options.openApi?.modelsModuleName).toBe('LocalizedModels')
        expect(options.openApi?.modelsPath).toBe('rules/LocalizedModels.xlsx')
        expect(options.openApi?.algorithmsModuleName).toBe('LocalizedAlgorithms')
        expect(options.openApi?.algorithmsPath).toBe('rules/LocalizedAlgorithms.xlsx')
        await waitFor(() => expect(onCreated).toHaveBeenCalled())
    })

    it('requires every OpenAPI module name and path', async () => {
        renderWizard()

        await toConfig('openapi')
        await userEvent.type(screen.getByTestId('new-project-name'), 'FromApi')
        await userEvent.click(screen.getByTestId('pick-file'))
        // A module field cleared by the user must block submit even though the others keep their defaults.
        await userEvent.clear(screen.getByTestId('new-project-openapi-data-module'))
        await userEvent.click(screen.getByTestId('new-project-submit'))

        expect(screen.getByTestId('new-project-error')).toBeTruthy()
        expect(createProject).not.toHaveBeenCalled()
    })

    it('publishes selected local projects from the workspace', async () => {
        vi.mocked(createProjectsFromWorkspace).mockResolvedValue()
        vi.mocked(getProjects).mockResolvedValue({
            content: [
                { id: 'local1', name: 'Draft', status: 'LOCAL' },
                { id: 'local2', name: 'Sandbox', status: 'LOCAL' },
            ],
            pageNumber: 0,
            pageSize: 2,
            numberOfElements: 2,
            total: 2,
        } as never)
        const onCreated = vi.fn()
        renderWizard({ localProjects: ['Draft', 'Sandbox'], onCreated })

        await toConfig('workspace')
        await userEvent.click(screen.getByTestId('workspace-Draft'))
        await userEvent.click(screen.getByTestId('new-project-submit'))

        await waitFor(() => expect(createProjectsFromWorkspace).toHaveBeenCalledTimes(1))
        const [repoId, body] = vi.mocked(createProjectsFromWorkspace).mock.calls[0]!
        expect(repoId).toBe('design')
        expect(body.names).toEqual(['Draft'])
        await waitFor(() => expect(onCreated).toHaveBeenCalled())
    })

    it('creates a project from a selected template', async () => {
        const onCreated = vi.fn()
        renderWizard({ onCreated })

        await toConfig('template')
        // Templates are grouped: pick the category, then the template within it.
        await userEvent.click(await screen.findByTestId('template-group-General'))
        await userEvent.type(screen.getByTestId('new-project-name'), 'FromTpl')
        await userEvent.click(await screen.findByTestId(`template-${JSON.stringify(['predefined', 'General', 'Example'])}`))
        await userEvent.click(screen.getByTestId('new-project-submit'))

        await waitFor(() => expect(createProject).toHaveBeenCalledTimes(1))
        const [, name, options] = vi.mocked(createProject).mock.calls[0]!
        expect(name).toBe('FromTpl')
        expect(options.files).toBeUndefined()
        expect(options.template).toEqual({ type: 'predefined', category: 'General', name: 'Example' })
        await waitFor(() => expect(onCreated).toHaveBeenCalled())
    })

    it('suggests existing branches, accepts a new name and creates the project there', async () => {
        vi.mocked(getDesignRepositoryConfig).mockResolvedValue({
            branch: 'main',
            newBranch: { pattern: '[A-Za-z0-9/_-]+', invalidNameHint: 'Invalid branch' },
            comment: { templates: {} },
        })
        renderWizard({ repositories: branchingRepositories })

        await toConfig('template')
        await waitFor(() => expect(screen.getByTestId('new-project-branch')).toHaveValue('main'))
        expect(screen.getByText('feature/rates')).toBeInTheDocument()
        expect(getDesignRepositoryBranches).toHaveBeenCalledWith('design')

        await userEvent.clear(screen.getByTestId('new-project-branch'))
        await userEvent.type(screen.getByTestId('new-project-branch'), 'feature/new-project')
        await userEvent.click(await screen.findByTestId('template-group-General'))
        await userEvent.type(screen.getByTestId('new-project-name'), 'InBranch')
        await userEvent.click(await screen.findByTestId(`template-${JSON.stringify(['predefined', 'General', 'Example'])}`))
        await userEvent.click(screen.getByTestId('new-project-submit'))

        await waitFor(() => expect(createProject).toHaveBeenCalledTimes(1))
        expect(vi.mocked(createProject).mock.calls[0]![2].branch).toBe('feature/new-project')
    })

    it('accepts the configured branch when branch enumeration fails', async () => {
        vi.mocked(getDesignRepositoryConfig).mockResolvedValue({
            branch: 'main',
            newBranch: { namePattern: 'release/.+' },
            comment: { templates: {} },
        })
        vi.mocked(getDesignRepositoryBranches).mockRejectedValue(new Error('unavailable'))
        renderWizard({ repositories: branchingRepositories })

        await toConfig('template')
        await waitFor(() => expect(screen.getByTestId('new-project-branch')).toHaveValue('main'))
        await userEvent.click(await screen.findByTestId('template-group-General'))
        await userEvent.type(screen.getByTestId('new-project-name'), 'ConfiguredBranch')
        await userEvent.click(await screen.findByTestId(`template-${JSON.stringify(['predefined', 'General', 'Example'])}`))
        await userEvent.click(screen.getByTestId('new-project-submit'))

        await waitFor(() => expect(createProject).toHaveBeenCalledTimes(1))
        expect(vi.mocked(createProject).mock.calls[0]![2].branch).toBe('main')
    })

    it('shows invalid Git branch names as field errors before submission', async () => {
        vi.mocked(getDesignRepositoryConfig).mockResolvedValue({
            branch: 'main',
            comment: { templates: {} },
        })
        renderWizard({ repositories: branchingRepositories })

        await toConfig('template')
        await waitFor(() => expect(screen.getByTestId('new-project-branch')).toHaveValue('main'))
        await userEvent.clear(screen.getByTestId('new-project-branch'))
        await userEvent.type(screen.getByTestId('new-project-branch'), 'feature bad')

        expect(screen.getByTestId('new-project-branch-error')).toHaveTextContent('valid Git branch name')
        expect(createProject).not.toHaveBeenCalled()
    })

    it('sends the selected branch when publishing workspace projects', async () => {
        vi.mocked(getDesignRepositoryConfig).mockResolvedValue({
            branch: 'main',
            comment: { templates: {} },
        })
        vi.mocked(createProjectsFromWorkspace).mockResolvedValue()
        vi.mocked(getProjects).mockResolvedValue({
            content: [{ id: 'local1', name: 'Draft', status: 'LOCAL' }],
            pageNumber: 0,
            pageSize: 1,
            numberOfElements: 1,
            total: 1,
        } as never)
        renderWizard({ repositories: branchingRepositories })

        await toConfig('workspace')
        await waitFor(() => expect(screen.getByTestId('new-project-branch')).toHaveValue('main'))
        await userEvent.click(screen.getByTestId('workspace-Draft'))
        await userEvent.click(screen.getByTestId('new-project-submit'))

        await waitFor(() => expect(createProjectsFromWorkspace).toHaveBeenCalledWith(
            'design',
            expect.objectContaining({ names: ['Draft'], branch: 'main' })
        ))
    })

    it('passes the repository path when provided', async () => {
        renderWizard({ repositories: mappedRepositories })

        await toConfig('template')
        await userEvent.click(await screen.findByTestId('template-group-General'))
        await userEvent.type(screen.getByTestId('new-project-name'), 'Nested')
        await userEvent.type(screen.getByTestId('new-project-path'), 'team/rules')
        await userEvent.click(await screen.findByTestId(`template-${JSON.stringify(['predefined', 'General', 'Example'])}`))
        await userEvent.click(screen.getByTestId('new-project-submit'))

        await waitFor(() => expect(createProject).toHaveBeenCalledTimes(1))
        const [, name, options] = vi.mocked(createProject).mock.calls[0]!
        expect(name).toBe('Nested')
        expect(options.path).toBe('team/rules')
    })

    it('appends the project name to the path of an uploaded archive', async () => {
        renderWizard({ repositories: mappedRepositories })

        await toConfig('archive')
        await userEvent.type(screen.getByTestId('new-project-name'), 'Nested')
        await userEvent.type(screen.getByTestId('new-project-path'), 'team/rules')
        await userEvent.click(screen.getByTestId('pick-file'))
        await userEvent.click(screen.getByTestId('new-project-submit'))

        await waitFor(() => expect(createProject).toHaveBeenCalledTimes(1))
        // An archive is mapped by its full internal path, unlike every other create mode.
        expect(vi.mocked(createProject).mock.calls[0]![2].path).toBe('team/rules/Nested')
    })

    it('passes the repository path when copying a project', async () => {
        const projects = [
            { id: 'source', name: 'Source', repository: 'sourceRepo', capabilities: { canCopy: true } },
        ]
        vi.mocked(getProjects).mockResolvedValue({
            content: projects,
            pageNumber: 0,
            pageSize: 1,
            numberOfElements: 1,
            total: 1,
        } as never)
        const onCreated = vi.fn()
        renderWizard({ projects, onCreated, repositories: mappedRepositories })

        await toConfig('copy')
        await userEvent.click(await screen.findByTestId('opt-source'))
        // The name pre-fills from the source with a "(Copy)" suffix; the user can still edit it.
        await waitFor(() => expect((screen.getByTestId('new-project-name') as HTMLInputElement).value).toBe('Source (Copy)'))
        await userEvent.clear(screen.getByTestId('new-project-name'))
        await userEvent.type(screen.getByTestId('new-project-name'), 'Copied')
        await userEvent.type(screen.getByTestId('new-project-path'), 'team/rules')
        await userEvent.type(screen.getByTestId('new-project-comment'), 'copy comment')
        await userEvent.click(screen.getByTestId('new-project-submit'))

        await waitFor(() => expect(copyProject).toHaveBeenCalledTimes(1))
        expect(copyProject).toHaveBeenCalledWith(
            'sourceRepo',
            'Source',
            'design',
            'Copied',
            'copy comment',
            'team/rules',
            undefined,
            undefined
        )
        await waitFor(() => expect(onCreated).toHaveBeenCalled())
    })

    it('offers only projects with copy capability as copy sources', async () => {
        const projects = [
            { id: 'copyable', name: 'Copyable', repository: 'design', capabilities: { canCopy: true } },
            { id: 'readOnly', name: 'Read Only', repository: 'design', capabilities: {} },
        ]
        vi.mocked(getProjects).mockResolvedValue({
            content: projects,
            pageNumber: 0,
            pageSize: 2,
            numberOfElements: 2,
            total: 2,
        } as never)
        renderWizard({
            projects,
        })

        await toConfig('copy')

        expect(screen.getByTestId('opt-copyable')).toBeTruthy()
        expect(screen.queryByTestId('opt-readOnly')).toBeNull()
    })

    it('pre-fills the name from the selected template', async () => {
        renderWizard()

        await toConfig('template')
        await userEvent.click(await screen.findByTestId('template-group-General'))
        await userEvent.click(await screen.findByTestId(`template-${JSON.stringify(['predefined', 'General', 'Example'])}`))

        await waitFor(() => expect((screen.getByTestId('new-project-name') as HTMLInputElement).value).toBe('Example'))
    })

    it('keeps a name the user typed over the auto-filled suggestion', async () => {
        renderWizard()

        await toConfig('template')
        await userEvent.click(await screen.findByTestId('template-group-General'))
        await userEvent.type(screen.getByTestId('new-project-name'), 'Custom')
        await userEvent.click(await screen.findByTestId(`template-${JSON.stringify(['predefined', 'General', 'Example'])}`))

        expect((screen.getByTestId('new-project-name') as HTMLInputElement).value).toBe('Custom')
    })

    it('pre-fills the name from an inspected archive', async () => {
        vi.mocked(inspectOpenLArchive).mockResolvedValue({ readable: true, isOpenLProject: true, name: 'Pricing Rules' })
        renderWizard()

        await toConfig('archive')
        await userEvent.click(screen.getByTestId('pick-file'))

        await waitFor(() => expect((screen.getByTestId('new-project-name') as HTMLInputElement).value).toBe('Pricing Rules'))
    })

    it('warns and blocks submit when the archive is not an OpenL project', async () => {
        vi.mocked(inspectOpenLArchive).mockResolvedValue({ readable: true, isOpenLProject: false, name: 'proj' })
        renderWizard()

        await toConfig('archive')
        await userEvent.click(screen.getByTestId('pick-file'))

        await waitFor(() => expect(screen.getByTestId('new-project-archive-error')).toBeTruthy())
        await userEvent.click(screen.getByTestId('new-project-submit'))
        expect(createProject).not.toHaveBeenCalled()
    })
})
