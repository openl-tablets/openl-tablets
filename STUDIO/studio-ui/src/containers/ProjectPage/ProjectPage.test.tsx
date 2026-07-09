import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { MockedFunction } from 'vitest'
import { ProjectPage } from './ProjectPage'
import { fetchProject } from 'services/projects'
import {
    fetchProjectDescriptor,
    fetchProjectStatus,
    generateProjectOpenApiSchema,
    subscribeProjectStatus,
    updateProjectDescriptor,
} from 'services'
import type { ProjectDescriptorView } from '../../types/projectDescriptor'

const { modalConfirm, notifySuccess, notifyError } = vi.hoisted(() => ({
    modalConfirm: vi.fn(),
    notifySuccess: vi.fn(),
    notifyError: vi.fn(),
}))

vi.mock('services/projects', () => ({ fetchProject: vi.fn() }))
vi.mock('services', () => ({
    fetchProjectDescriptor: vi.fn(),
    fetchProjectStatus: vi.fn(),
    subscribeProjectStatus: vi.fn(),
    updateProjectDescriptor: vi.fn(),
    generateProjectOpenApiSchema: vi.fn(),
    isApiHttpError: (error: unknown) => !!error && typeof error === 'object' && 'status' in error,
}))
vi.mock('react-i18next', () => ({
    useTranslation: () => ({ t: (key: string) => key, i18n: { language: 'en' } }),
}))
vi.mock('utils/dateFormat', () => ({ formatDateTime: (value: string) => value }))
vi.mock('./ProjectPage.styles', () => ({
    useStyles: () => ({
        styles: new Proxy({}, { get: (_target, prop) => String(prop) }),
        cx: (...classes: unknown[]) => classes.filter(Boolean).join(' '),
    }),
}))
vi.mock('antd', () => {
    const Input = ({ value, onChange, placeholder, 'data-testid': testId }: {
        value?: string
        onChange?: (event: { target: { value: string } }) => void
        placeholder?: string
        'data-testid'?: string
    }) => <input data-testid={testId} onChange={onChange} placeholder={placeholder} value={value} />
    Input.TextArea = ({ value, onChange, placeholder }: {
        value?: string
        onChange?: (event: { target: { value: string } }) => void
        placeholder?: string
    }) => <textarea onChange={onChange} placeholder={placeholder} value={value} />
    return {
        App: { useApp: () => ({ modal: { confirm: modalConfirm }, notification: { success: notifySuccess, error: notifyError } }) },
        Typography: {
            Title: ({ children }: { children?: React.ReactNode }) => <h1>{children}</h1>,
            Paragraph: ({ children }: { children?: React.ReactNode }) => <p>{children}</p>,
            Text: ({ children }: { children?: React.ReactNode }) => <span>{children}</span>,
        },
        Badge: ({ text }: { text?: React.ReactNode }) => <span>{text}</span>,
        Skeleton: () => <div data-testid="skeleton" />,
        Alert: ({ message }: { message?: React.ReactNode }) => <div role="alert">{message}</div>,
        Checkbox: ({ checked, onChange, children }: {
            checked?: boolean
            onChange?: (event: { target: { checked: boolean } }) => void
            children?: React.ReactNode
        }) => <label><input checked={checked} onChange={onChange} type="checkbox" />{children}</label>,
        Select: ({ value, onChange, options }: {
            value?: string
            onChange?: (value: string) => void
            options?: { value: string; label: React.ReactNode }[]
        }) => (
            <select onChange={(event) => onChange?.(event.target.value)} value={value ?? ''}>
                <option value="" />
                {(options ?? []).map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
            </select>
        ),
        Button: ({ children, onClick, disabled, loading, 'data-testid': testId }: {
            children?: React.ReactNode
            onClick?: () => void
            disabled?: boolean
            loading?: boolean
            'data-testid'?: string
        }) => <button data-testid={testId} disabled={disabled || loading} onClick={onClick} type="button">{children}</button>,
        Input,
    }
})
vi.mock('@ant-design/icons', () => ({ CloseOutlined: () => <span />, PlusOutlined: () => <span /> }))
vi.mock('components/StringListEditor', () => ({
    StringListEditor: ({ values }: { values?: string[] }) => <div data-testid="list-editor">{(values ?? []).join(',')}</div>,
}))
vi.mock('components/SortablePatternList', () => ({
    SortablePatternList: ({ values }: { values?: string[] }) => <div data-testid="pattern-list">{(values ?? []).join(',')}</div>,
}))

const mockFetchProject = fetchProject as MockedFunction<typeof fetchProject>
const mockFetchDescriptor = fetchProjectDescriptor as MockedFunction<typeof fetchProjectDescriptor>
const mockFetchStatus = fetchProjectStatus as MockedFunction<typeof fetchProjectStatus>
const mockSubscribe = subscribeProjectStatus as MockedFunction<typeof subscribeProjectStatus>
const mockUpdate = updateProjectDescriptor as MockedFunction<typeof updateProjectDescriptor>
const mockGenerate = generateProjectOpenApiSchema as MockedFunction<typeof generateProjectOpenApiSchema>

const descriptor: ProjectDescriptorView = {
    name: 'My Project',
    comment: 'A short description',
    modules: [{ name: 'Main', rulesRootPath: 'rules/Main.xlsx', wildcard: false }],
    dependencies: [{ name: 'Common', autoIncluded: true }],
    classpath: ['lib/*.jar'],
    propertiesFileNamePatterns: [],
    editable: true,
    contentHash: 'abc',
}

const project = {
    id: 'x',
    name: 'My Project',
    status: 'EDITING',
    branch: 'main',
    repository: 'Design',
    revision: 'r1',
    modifiedBy: 'Jane',
    modifiedAt: '2026-07-08T15:00:00Z',
    path: 'My Project',
    comment: 'A short description',
}

describe('ProjectPage', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        mockFetchStatus.mockResolvedValue({ projectId: 'x', compileState: 'ok' } as never)
        mockSubscribe.mockReturnValue({ unsubscribe: vi.fn() })
    })

    it('shows a skeleton while loading', () => {
        mockFetchProject.mockReturnValue(new Promise(() => {}) as never)
        mockFetchDescriptor.mockReturnValue(new Promise(() => {}) as never)
        mockFetchStatus.mockReturnValue(new Promise(() => {}) as never)
        render(<ProjectPage projectId="x" />)
        expect(screen.getByTestId('skeleton')).toBeInTheDocument()
    })

    it('renders the descriptor as a document once loaded', async () => {
        mockFetchProject.mockResolvedValue(project as never)
        mockFetchDescriptor.mockResolvedValue(descriptor)
        render(<ProjectPage projectId="x" />)

        expect(await screen.findByTestId('project-page')).toBeInTheDocument()
        expect(screen.getByText('EDITING')).toBeInTheDocument()
        expect(screen.getByText('Jane')).toBeInTheDocument()
        expect(screen.getByText('Design')).toBeInTheDocument()
        expect(screen.getByText('project:page.modules')).toBeInTheDocument()
        expect(screen.getByText('Main')).toBeInTheDocument()
        expect(screen.getByText('rules/Main.xlsx')).toBeInTheDocument()
        expect(screen.getByText('Common')).toBeInTheDocument()
        expect(screen.getByText('lib/*.jar')).toBeInTheDocument()
    })

    it('shows a quiet empty state for sections with no entries', async () => {
        mockFetchProject.mockResolvedValue(project as never)
        mockFetchDescriptor.mockResolvedValue({ editable: true } as ProjectDescriptorView)
        render(<ProjectPage projectId="x" />)

        expect(await screen.findByText('project:page.empty_modules')).toBeInTheDocument()
        expect(screen.getByText('project:page.empty_dependencies')).toBeInTheDocument()
        expect(screen.getByText('project:page.empty_sources')).toBeInTheDocument()
    })

    it('shows an error when loading fails', async () => {
        mockFetchProject.mockRejectedValue(new Error('boom'))
        mockFetchDescriptor.mockResolvedValue(descriptor)
        render(<ProjectPage projectId="x" />)

        expect(await screen.findByRole('alert')).toHaveTextContent('project:page.load_failed')
    })

    it('edits the comment and saves the whole descriptor', async () => {
        mockFetchProject.mockResolvedValue(project as never)
        mockFetchDescriptor.mockResolvedValue(descriptor)
        mockUpdate.mockResolvedValue({ ...descriptor, comment: 'Edited', contentHash: 'def' })
        render(<ProjectPage projectId="x" />)

        await userEvent.click(await screen.findByTestId('edit-button'))
        const commentBox = screen.getByPlaceholderText('project:page.comment_placeholder')
        await userEvent.clear(commentBox)
        await userEvent.type(commentBox, 'Edited')
        const nameBox = screen.getByTestId('project-name-input')
        await userEvent.clear(nameBox)
        await userEvent.type(nameBox, 'Renamed')
        await userEvent.click(screen.getByText('project:page.save'))

        await waitFor(() => expect(mockUpdate).toHaveBeenCalled())
        const [id, body, force] = mockUpdate.mock.calls[0]!
        expect(id).toBe('x')
        expect(body.name).toBe('Renamed')
        expect(body.comment).toBe('Edited')
        expect(body.contentHash).toBe('abc')
        expect(force).toBe(false)
        await waitFor(() => expect(notifySuccess).toHaveBeenCalled())
    })

    it('adds a module and an exposed method, then saves them', async () => {
        mockFetchProject.mockResolvedValue(project as never)
        mockFetchDescriptor.mockResolvedValue({ editable: true, contentHash: 'h' } as ProjectDescriptorView)
        mockUpdate.mockResolvedValue({ editable: true } as ProjectDescriptorView)
        render(<ProjectPage projectId="x" />)

        await userEvent.click(await screen.findByTestId('edit-button'))
        await userEvent.click(screen.getByText('project:page.add_module'))
        await userEvent.click(screen.getByText('project:page.add_pattern'))
        await userEvent.type(screen.getByPlaceholderText('project:page.module_name_label'), 'M1')
        await userEvent.type(screen.getByPlaceholderText('project:page.pattern_placeholder'), '*calc*')
        const typeSelect = screen.getAllByRole('combobox')
            .find((select) => [...(select as HTMLSelectElement).options].some((option) => option.value === 'exclude'))!
        await userEvent.selectOptions(typeSelect, 'exclude')
        await userEvent.click(screen.getByText('project:page.save'))

        await waitFor(() => expect(mockUpdate).toHaveBeenCalled())
        const [, body] = mockUpdate.mock.calls[0]!
        expect(body.modules).toEqual([{ name: 'M1' }])
        expect(body.exposedMethods).toEqual([{ pattern: '*calc*', type: 'exclude' }])
    })

    it('edits the OpenAPI config and saves it', async () => {
        mockFetchProject.mockResolvedValue(project as never)
        mockFetchDescriptor.mockResolvedValue({ editable: true, contentHash: 'h' } as ProjectDescriptorView)
        mockUpdate.mockResolvedValue({ editable: true } as ProjectDescriptorView)
        render(<ProjectPage projectId="x" />)

        await userEvent.click(await screen.findByTestId('edit-button'))
        await userEvent.type(screen.getByPlaceholderText('project:page.openapi_path_placeholder'), 'openapi.json')
        await userEvent.selectOptions(screen.getByRole('combobox'), 'GENERATION')
        await userEvent.click(screen.getByText('project:page.save'))

        await waitFor(() => expect(mockUpdate).toHaveBeenCalled())
        const [, body] = mockUpdate.mock.calls[0]!
        expect(body.openapi).toEqual({ path: 'openapi.json', mode: 'GENERATION' })
    })

    it('generates the OpenAPI schema on demand', async () => {
        mockFetchProject.mockResolvedValue(project as never)
        mockFetchDescriptor.mockResolvedValue(descriptor)
        mockGenerate.mockResolvedValue({ ...descriptor, openapi: { path: 'openapi.json', mode: 'RECONCILIATION' } })
        render(<ProjectPage projectId="x" />)

        await userEvent.click(await screen.findByTestId('generate-openapi-button'))

        await waitFor(() => expect(mockGenerate).toHaveBeenCalledWith('x'))
        await waitFor(() => expect(notifySuccess).toHaveBeenCalled())
    })

    it('confirms and force-overwrites on a stale conflict (409)', async () => {
        mockFetchProject.mockResolvedValue(project as never)
        mockFetchDescriptor.mockResolvedValue(descriptor)
        mockUpdate.mockRejectedValueOnce({ status: 409 }).mockResolvedValueOnce({ ...descriptor })
        render(<ProjectPage projectId="x" />)

        await userEvent.click(await screen.findByTestId('edit-button'))
        await userEvent.click(screen.getByText('project:page.save'))

        await waitFor(() => expect(modalConfirm).toHaveBeenCalled())
        await act(async () => {
            modalConfirm.mock.calls[0]![0].onOk()
            await new Promise((resolve) => setTimeout(resolve, 0))
        })

        expect(mockUpdate).toHaveBeenCalledTimes(2)
        expect(mockUpdate.mock.calls[1]![2]).toBe(true)
        await waitFor(() => expect(screen.getByTestId('edit-button')).toBeInTheDocument())
    })
})
