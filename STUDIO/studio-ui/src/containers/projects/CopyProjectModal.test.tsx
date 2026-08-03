import type { ComponentProps } from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import { fireEvent } from '@testing-library/dom'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CopyProjectModal } from './CopyProjectModal'
import type { Project } from '../../types/projects'
import type { Repository } from '../../types/repositories'
import {
    copyProject,
    createProjectBranch,
    getDesignRepositoryBranches,
    getDesignRepositoryConfig,
    getProjectRevisions,
    getRepositoryConfig,
    isProjectModifiedConflict,
    switchProjectBranch,
} from '../../services/repositories'

vi.mock('../../services/repositories', () => ({
    copyProject: vi.fn(),
    createProjectBranch: vi.fn(),
    getDesignRepositoryBranches: vi.fn(),
    getDesignRepositoryConfig: vi.fn(),
    getProjectRevisions: vi.fn(),
    getRepositoryConfig: vi.fn(),
    isProjectModifiedConflict: vi.fn(() => false),
    switchProjectBranch: vi.fn(),
    REVISIONS_PAGE_SIZE: 50,
}))

vi.mock('../../store', async importOriginal => ({
    ...await importOriginal<object>(),
    useUserStore: () => 'jdoe',
}))

// The real BranchSelect shows marked-up branch labels; the test needs the field value and the offered names.
vi.mock('./BranchSelect', () => ({
    BranchSelect: ({ onChange, branchNames, value, placeholder, marksOf, allowNew, ...rest }: {
        onChange: (value: string) => void
        branchNames: string[]
        value?: string
        placeholder?: string
        marksOf?: unknown
        allowNew?: boolean
        'data-testid'?: string
    }) => {
        void marksOf; void allowNew
        return (
            <>
                <input {...rest} onChange={event => onChange(event.target.value)} placeholder={placeholder} value={value ?? ''} />
                {branchNames.map(name => (
                    <span key={name} data-testid={`target-branch-option-${name}`}>{name}</span>
                ))}
            </>
        )
    },
}))

vi.mock('react-i18next', () => {
    // Stable t: a new t per render would re-run the modal's reset effect and clobber field edits.
    const t = (key: string, opts?: Record<string, unknown>) => (opts?.['name'] ? `${key}:${opts['name']}` : key)
    return { useTranslation: () => ({ t }) }
})

vi.mock('antd', () => {
    const Modal = ({ open, children, onOk, okText, okButtonProps }: Record<string, unknown>) =>
        open
            ? (
                <div>
                    {children as never}
                    <button
                        data-testid={(okButtonProps as Record<string, string> | undefined)?.['data-testid']}
                        onClick={onOk as never}
                    >
                        {okText as never}
                    </button>
                </div>
            )
            : null
    const Input = ({ onChange, ...rest }: Record<string, unknown>) => <input onChange={onChange as never} {...rest} />
    Input.TextArea = ({ onChange, autoSize, ...rest }: Record<string, unknown>) => {
        void autoSize
        return <textarea onChange={onChange as never} {...rest} />
    }
    const Alert = ({ title, showIcon, ...rest }: Record<string, unknown>) => {
        void showIcon
        return <div {...rest}>{title as never}</div>
    }
    const Checkbox = ({ checked, onChange, ...rest }: Record<string, unknown>) => (
        <input
            checked={checked as boolean}
            data-testid={rest['data-testid'] as string}
            onChange={onChange as never}
            type="checkbox"
        />
    )
    interface Opt { value: string, label: string }
    const Select = ({ options, onChange, value, ...rest }: Record<string, unknown>) => (
        <select
            data-testid={rest['data-testid'] as string}
            onChange={event => (onChange as (v: string) => void)(event.target.value)}
            value={value as string}
        >
            {(options as Opt[]).map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
        </select>
    )
    const Button = ({ children, onClick, icon, ...rest }: Record<string, unknown>) => {
        const { type, loading, ...dom } = rest
        void type; void loading
        return <button onClick={onClick as never} {...dom}>{icon as never}{children as never}</button>
    }
    const Space = ({ children }: Record<string, unknown>) => <div>{children as never}</div>
    Space.Compact = ({ children }: Record<string, unknown>) => <div>{children as never}</div>
    const Tooltip = ({ children }: Record<string, unknown>) => children as never
    const notification = { success: vi.fn(), error: vi.fn() }
    const Typography = {
        Text: ({ children, type, ...rest }: Record<string, unknown>) => {
            void type
            return <span {...rest}>{children as never}</span>
        },
    }
    return { Alert, Button, Checkbox, Input, Modal, notification, Select, Space, Tooltip, Typography }
})

vi.mock('./RepoFolderPicker', () => ({ RepoFolderPicker: () => null }))

const project = {
    id: 'p1',
    name: 'Alpha',
    repository: 'design',
    branch: 'master',
    capabilities: { canCopy: true, canManageBranches: true },
} as unknown as Project
const repositories = [
    { id: 'design', name: 'Design', features: { branches: true } },
    { id: 'prod', name: 'Prod', features: { branches: true } },
] as unknown as Repository[]
const mappedRepositories = [
    {
        id: 'design',
        name: 'Design',
        features: { branches: false, searchable: false, mappedFolders: true },
    },
] as unknown as Repository[]

const renderModal = async (overrides: Partial<ComponentProps<typeof CopyProjectModal>> = {}) => {
    const props = { open: true, onClose: vi.fn(), onCopied: vi.fn(), project, repositories, ...overrides }
    render(<CopyProjectModal {...props} />)
    await waitFor(() => expect(getRepositoryConfig).toHaveBeenCalled())
    return props
}

const asNewProject = async () => {
    await userEvent.click(screen.getByTestId('copy-project-as-new'))
    await screen.findByTestId('copy-project-name')
}

describe('CopyProjectModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(copyProject).mockResolvedValue()
        vi.mocked(createProjectBranch).mockResolvedValue()
        vi.mocked(switchProjectBranch).mockResolvedValue()
        vi.mocked(getRepositoryConfig).mockResolvedValue({
            newBranch: { pattern: '{project-name}/{username}', namePattern: '[A-Za-z0-9/-]+' },
            comment: { templates: { copy: 'Copied from: {project-name}.' } },
        })
        vi.mocked(getDesignRepositoryBranches).mockResolvedValue(['master', 'feature/rates'])
        vi.mocked(getDesignRepositoryConfig).mockResolvedValue({
            branch: 'master',
            newBranch: { namePattern: '[A-Za-z0-9/-]+' },
            comment: { templates: { copy: 'Copied from: {project-name}.' } },
        })
        vi.mocked(getProjectRevisions).mockResolvedValue({
            content: [
                { revisionNo: 'rev-2', shortRevisionNo: 'rev2', createdAt: '2026-07-22T16:35:00Z', fullComment: '', deleted: false, technicalRevision: false, author: { displayName: 'Joe Doe' } },
                { revisionNo: 'rev-1', shortRevisionNo: 'rev1', createdAt: '2026-07-21T10:00:00Z', fullComment: '', deleted: false, technicalRevision: false, author: { displayName: 'Jane Roe' } },
            ],
            pageNumber: 0,
            pageSize: 50,
            numberOfElements: 2,
            total: 2,
        })
    })

    it('rejects a branch name the repository pattern forbids, without calling the API', async () => {
        vi.mocked(getRepositoryConfig).mockResolvedValue({
            newBranch: { pattern: '{project-name}', namePattern: 'release/.+', invalidNameHint: 'Use release/<name>' },
            comment: { templates: {} },
        })
        await renderModal()
        await waitFor(() => expect((screen.getByTestId('copy-project-branch') as HTMLInputElement).value).toBe('Alpha'))

        // The repository words the rejection itself, and the field says so before the dialog is submitted.
        await waitFor(() => expect(screen.getByTestId('copy-project-branch-error')).toHaveTextContent('Use release/<name>'))

        await userEvent.click(screen.getByTestId('copy-project-submit'))

        await waitFor(() => expect(screen.getByTestId('copy-project-error')).toHaveTextContent('Use release/<name>'))
        expect(createProjectBranch).not.toHaveBeenCalled()
    })

    it('rejects a comment the repository pattern forbids, without calling the API', async () => {
        vi.mocked(getDesignRepositoryConfig).mockResolvedValue({
            branch: 'master',
            comment: {
                userMessagePattern: 'EPBDS-\\d+.*',
                invalidUserMessageHint: 'Start with a ticket',
                templates: { copy: 'Copied from: {project-name}.' },
            },
        })
        await renderModal()
        await asNewProject()

        fireEvent.change(screen.getByTestId('copy-project-name'), { target: { value: 'Beta' } })
        await waitFor(() => expect(screen.getByTestId('copy-project-comment-error')).toHaveTextContent('Start with a ticket'))

        await userEvent.click(screen.getByTestId('copy-project-submit'))

        await waitFor(() => expect(screen.getByTestId('copy-project-error')).toHaveTextContent('Start with a ticket'))
        expect(copyProject).not.toHaveBeenCalled()
    })

    it('offers a new branch built from the repository pattern', async () => {
        await renderModal()

        await waitFor(() => expect((screen.getByTestId('copy-project-branch') as HTMLInputElement).value)
            .toBe('Alpha/jdoe'))
        expect(screen.getByTestId('copy-project-current-branch')).toHaveTextContent('master')
        expect(screen.queryByTestId('copy-project-name')).toBeNull()
    })

    it('creates the branch and moves the project onto it', async () => {
        const props = await renderModal()
        await waitFor(() => expect((screen.getByTestId('copy-project-branch') as HTMLInputElement).value).not.toBe(''))

        await userEvent.click(screen.getByTestId('copy-project-submit'))

        await waitFor(() => expect(createProjectBranch).toHaveBeenCalledWith('p1', 'Alpha/jdoe'))
        expect(switchProjectBranch).toHaveBeenCalledWith('p1', 'Alpha/jdoe')
        expect(copyProject).not.toHaveBeenCalled()
        expect(props.onCopied).toHaveBeenCalled()
    })

    it('copies into a new project with the suggested comment when asked', async () => {
        await renderModal()
        await asNewProject()
        await waitFor(() => expect((screen.getByTestId('copy-project-target-branch') as HTMLInputElement).value)
            .toBe('master'))

        fireEvent.change(screen.getByTestId('copy-project-name'), { target: { value: 'Beta' } })
        await userEvent.click(screen.getByTestId('copy-project-submit'))

        await waitFor(() => expect(copyProject).toHaveBeenCalledWith(
            'design',
            'Alpha',
            'design',
            'Beta',
            'Copied from: Alpha.',
            undefined,
            undefined,
            'master'
        ))
        expect(createProjectBranch).not.toHaveBeenCalled()
    })

    it('copies into a valid free-form target branch', async () => {
        await renderModal()
        await asNewProject()
        await screen.findByTestId('copy-project-target-branch')

        fireEvent.change(screen.getByTestId('copy-project-name'), { target: { value: 'Beta' } })
        fireEvent.change(screen.getByTestId('copy-project-target-branch'), { target: { value: 'release/rates' } })
        await userEvent.click(screen.getByTestId('copy-project-submit'))

        await waitFor(() => expect(copyProject).toHaveBeenCalledWith(
            'design', 'Alpha', 'design', 'Beta', 'Copied from: Alpha.', undefined, undefined, 'release/rates'
        ))
    })

    it('ignores a stale branch response after the target repository changes', async () => {
        let resolveDesign!: (branches: string[]) => void
        const designBranches = new Promise<string[]>(resolve => {
            resolveDesign = resolve
        })
        vi.mocked(getDesignRepositoryBranches).mockImplementation(repositoryId =>
            repositoryId === 'design' ? designBranches : Promise.resolve(['prod-only']))
        await renderModal()
        await asNewProject()
        await waitFor(() => expect(getDesignRepositoryBranches).toHaveBeenCalledWith('design'))

        fireEvent.change(screen.getByTestId('copy-project-repository'), { target: { value: 'prod' } })
        await screen.findByTestId('target-branch-option-prod-only')
        await act(async () => {
            resolveDesign(['design-only'])
            await designBranches
        })

        expect(screen.getByTestId('target-branch-option-prod-only')).toBeInTheDocument()
        expect(screen.queryByTestId('target-branch-option-design-only')).not.toBeInTheDocument()
    })

    it('copies to the configured target branch when branch enumeration fails', async () => {
        vi.mocked(getDesignRepositoryBranches).mockRejectedValue(new Error('unavailable'))
        vi.mocked(getDesignRepositoryConfig).mockResolvedValue({
            branch: 'master',
            newBranch: { namePattern: 'release/.+' },
            comment: { templates: { copy: 'Copied from: {project-name}.' } },
        })
        await renderModal()
        await asNewProject()
        await waitFor(() => expect(screen.getByTestId('copy-project-target-branch')).toHaveValue('master'))

        fireEvent.change(screen.getByTestId('copy-project-name'), { target: { value: 'Beta' } })
        await userEvent.click(screen.getByTestId('copy-project-submit'))

        await waitFor(() => expect(copyProject).toHaveBeenCalledWith(
            'design', 'Alpha', 'design', 'Beta', 'Copied from: Alpha.', undefined, undefined, 'master'
        ))
    })

    it('shows an invalid target branch as a field error', async () => {
        await renderModal()
        await asNewProject()
        await screen.findByTestId('copy-project-target-branch')

        fireEvent.change(screen.getByTestId('copy-project-target-branch'), { target: { value: 'feature bad' } })

        expect(screen.getByTestId('copy-project-target-branch-error'))
            .toHaveTextContent('browser.create.branch_invalid')
        expect(copyProject).not.toHaveBeenCalled()
    })

    it('copies from a chosen older revision', async () => {
        await renderModal()
        await asNewProject()
        fireEvent.change(screen.getByTestId('copy-project-name'), { target: { value: 'Beta' } })

        await userEvent.click(screen.getByTestId('copy-project-old-revision'))
        await screen.findByTestId('copy-project-revision')
        // Business users read a revision as who changed the project and when, not as its number.
        expect(screen.getByText(/^Joe Doe: /)).toBeInTheDocument()
        fireEvent.change(screen.getByTestId('copy-project-revision'), { target: { value: 'rev-1' } })
        await userEvent.click(screen.getByTestId('copy-project-submit'))

        await waitFor(() => expect(copyProject).toHaveBeenCalledWith(
            'design', 'Alpha', 'design', 'Beta', 'Copied from: Alpha.', undefined, 'rev-1', 'master'
        ))
    })

    it('copies into a new project directly when branching is not granted', async () => {
        await renderModal({ project: { ...project, capabilities: { canCopy: true } } })

        expect(screen.queryByTestId('copy-project-as-new')).toBeNull()
        expect(screen.getByTestId('copy-project-name')).toBeInTheDocument()
    })

    it('only branches when creating a project is not granted', async () => {
        await renderModal({ project: { ...project, capabilities: { canManageBranches: true } } })

        expect(screen.queryByTestId('copy-project-as-new')).toBeNull()
        expect(screen.getByTestId('copy-project-branch')).toBeInTheDocument()
        expect(screen.queryByTestId('copy-project-name')).toBeNull()
    })

    it('passes a target path only for repositories that support mapped folders', async () => {
        await renderModal({ repositories: mappedRepositories })
        await asNewProject()

        fireEvent.change(screen.getByTestId('copy-project-name'), { target: { value: 'Beta' } })
        fireEvent.change(screen.getByTestId('copy-project-path'), { target: { value: 'folder' } })
        await userEvent.click(screen.getByTestId('copy-project-submit'))

        await waitFor(() => expect(copyProject).toHaveBeenCalledWith(
            'design', 'Alpha', 'design', 'Beta', 'Copied from: Alpha.', 'folder', undefined, undefined
        ))
    })

    it('hides the target path for flat repositories', async () => {
        await renderModal()
        await asNewProject()

        expect(screen.queryByTestId('copy-project-path')).toBeNull()
    })

    it('rejects an empty name without calling the API', async () => {
        await renderModal()
        await asNewProject()

        await userEvent.click(screen.getByTestId('copy-project-submit'))

        await screen.findByTestId('copy-project-error')
        expect(copyProject).not.toHaveBeenCalled()
    })

    it('rejects an empty branch name without calling the API', async () => {
        vi.mocked(getRepositoryConfig).mockResolvedValue({ comment: { templates: {} } })
        await renderModal()

        await userEvent.click(screen.getByTestId('copy-project-submit'))

        await screen.findByTestId('copy-project-error')
        expect(createProjectBranch).not.toHaveBeenCalled()
    })

    it('warns that a branch leaves the unsaved changes behind', async () => {
        await renderModal({ project: { ...project, status: 'EDITING' } as unknown as Project })

        expect(screen.getByTestId('copy-project-modified-warning')).toHaveTextContent(
            'browser.copy_dialog.modified_warning'
        )

        // The warning is about moving onto the branch; copying into a new project does not move anything.
        await asNewProject()
        expect(screen.queryByTestId('copy-project-modified-warning')).not.toBeInTheDocument()
    })

    it('asks before dropping the unsaved changes the branch cannot take', async () => {
        vi.mocked(isProjectModifiedConflict).mockReturnValue(true)
        vi.mocked(switchProjectBranch).mockRejectedValueOnce(new Error('modified'))
        const props = await renderModal({ project: { ...project, status: 'EDITING' } as unknown as Project })

        await userEvent.click(screen.getByTestId('copy-project-submit'))

        // The branch is created; the project stays where it is until the user accepts losing the changes.
        await screen.findByTestId('copy-project-discard-confirm')
        expect(createProjectBranch).toHaveBeenCalled()
        expect(props.onCopied).not.toHaveBeenCalled()

        vi.mocked(switchProjectBranch).mockResolvedValueOnce()
        await userEvent.click(screen.getByTestId('copy-project-discard-confirm'))

        await waitFor(() => expect(switchProjectBranch).toHaveBeenLastCalledWith('p1', expect.any(String), {
            discardChanges: true,
        }))
        await waitFor(() => expect(props.onCopied).toHaveBeenCalled())
    })
})
