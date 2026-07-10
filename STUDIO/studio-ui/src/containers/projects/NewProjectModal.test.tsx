import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { NewProjectModal } from './NewProjectModal'
import {
    copyProject,
    createProject,
    createProjectsFromWorkspace,
    getProjects,
    getProjectTemplates,
} from '../../services/repositories'

vi.mock('../../services/repositories', () => ({
    copyProject: vi.fn(),
    createProject: vi.fn(),
    createProjectsFromWorkspace: vi.fn(),
    getProjects: vi.fn(),
    getProjectTemplates: vi.fn(),
}))

vi.mock('react-i18next', () => {
    const translations: Record<string, string> = {
        'browser.create.openapi_defaults.data_module_name': 'LocalizedModels',
        'browser.create.openapi_defaults.data_module_path': 'rules/LocalizedModels.xlsx',
        'browser.create.openapi_defaults.rules_module_name': 'LocalizedAlgorithms',
        'browser.create.openapi_defaults.rules_module_path': 'rules/LocalizedAlgorithms.xlsx',
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
    const Input = ({ onChange, ...rest }: Record<string, unknown>) => <input onChange={onChange as never} {...rest} />
    Input.TextArea = ({ onChange, ...rest }: Record<string, unknown>) => <textarea onChange={onChange as never} {...rest} />
    const Dragger = ({ children, beforeUpload }: Record<string, unknown>) => (
        <div>
            <button
                data-testid="pick-file"
                onClick={() => (beforeUpload as (f: File) => void)(new File(['x'], 'proj.zip', { type: 'application/zip' }))}
            >
                pick
            </button>
            {children as never}
        </div>
    )
    const Upload = { Dragger }
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
    return { Modal, Button, Checkbox, Input, Upload, Alert, Select, TreeSelect }
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

const renderWizard = (props: Record<string, unknown> = {}) =>
    render(<NewProjectModal open onClose={vi.fn()} onCreated={vi.fn()} repositories={repositories as never} {...props} />)

/** Pick a method tile (if given) and advance to the config step. */
const toConfig = async (methodId?: string) => {
    if (methodId) {
        await userEvent.click(screen.getByTestId(`new-project-method-${methodId}`))
    }
    await userEvent.click(screen.getByTestId('new-project-next'))
}

describe('NewProjectModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(copyProject).mockResolvedValue()
        vi.mocked(createProject).mockResolvedValue()
        vi.mocked(getProjects).mockResolvedValue({ content: [], pageNumber: 0, pageSize: 0, numberOfElements: 0 } as never)
        vi.mocked(getProjectTemplates).mockResolvedValue([{ type: 'predefined', category: 'General', templates: ['Example']}])
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
        await waitFor(() => expect(onCreated).toHaveBeenCalled())
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
            'team/rules'
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
})
