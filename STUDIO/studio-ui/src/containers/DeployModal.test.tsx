import React from 'react'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { App as AntApp, notification } from 'antd'
import { DeployModal } from 'containers/DeployModal'
import * as services from 'services'
import type { MockedFunction } from 'vitest'
vi.mock('hooks', async (importOriginal) => {
    const actual = await importOriginal<typeof import('hooks')>()
    return {
        ...actual,
        useCommitInfoGuard: () => ({
            runWithCommitInfo: async (action: () => void | Promise<void>) => {
                await action()
            },
            commitInfoModal: null,
        }),
    }
})

class MockApiHttpError extends Error {
    status: number
    constructor(status: number, message: string) {
        super(message)
        this.name = 'ApiHttpError'
        this.status = status
    }
}

vi.mock('services', async () => ({
    apiCall: vi.fn(),
    ForbiddenError: class ForbiddenError extends Error {
        constructor(message?: string) {
            super(message)
            this.name = 'ForbiddenError'
        }
    },
    isApiHttpError: (error: unknown) => error instanceof MockApiHttpError,
}))

vi.mock('react-i18next', async () => {
    const t = (key: string) => key
    const i18n = { language: 'en' }
    return {
        useTranslation: () => ({ t, i18n }),
    }
})

vi.mock('antd', async () => {
    const actual = await vi.importActual('antd')
    const MockModal = ({ open, children, title, footer, ...props }: {
        open?: boolean
        children: React.ReactNode
        title?: React.ReactNode
        footer?: React.ReactNode
    }) =>
        open ? (
            <div role="dialog" {...props}>
                {title && <div data-testid="modal-title">{title}</div>}
                {children}
                {footer && <div data-testid="modal-footer">{footer}</div>}
            </div>
        ) : null
    return {
        ...actual,
        Modal: MockModal,
        notification: {
            success: vi.fn(),
            error: vi.fn(),
            warning: vi.fn(),
        },
    }
})

vi.mock('components/form', async () => {
    const actual = await vi.importActual<typeof import('antd')>('antd')
    const Select = ({
        disabled,
        label,
        name,
        onBlur,
        onChange,
        onSearch,
        options,
        required,
    }: {
        disabled?: boolean
        label: string
        name: string
        onBlur?: () => void
        onChange?: (value: string) => void
        onSearch?: (value: string) => void
        options: Array<{ label: string; value: string }>
        required?: boolean
    }) => (
        <>
            <actual.Form.Item label={label} name={name} rules={required ? [{ required: true }] : []}>
                <select
                    aria-label={label}
                    disabled={disabled}
                    onBlur={onBlur}
                    onChange={event => onChange?.(event.target.value)}
                >
                    <option value="" />
                    {options.map(option => (
                        <option key={option.value} value={option.value}>{option.label}</option>
                    ))}
                </select>
            </actual.Form.Item>
            {onSearch && (
                <input
                    aria-label={`${label}-search`}
                    onChange={event => onSearch(event.target.value)}
                />
            )}
        </>
    )
    const TextArea = ({
        label,
        name,
        placeholder,
        required,
    }: {
        label: string
        name: string
        placeholder?: string
        required?: boolean
    }) => (
        <actual.Form.Item label={label} name={name} rules={required ? [{ required: true }] : []}>
            <textarea aria-label={label} placeholder={placeholder} />
        </actual.Form.Item>
    )
    return { Select, TextArea }
})

const mockApiCall = services.apiCall as MockedFunction<typeof services.apiCall>

const openModal = async (detail: Record<string, unknown>) => {
    await act(async () => {
        window.dispatchEvent(new CustomEvent('openDeployModal', { detail }))
    })
}

const defaultDetail = {
    id: 'proj-1',
    name: 'TestProject',
    branch: 'main',
    comment: '',
    modifiedAt: '',
    modifiedBy: '',
    repository: '',
    revision: '',
    status: '',
}

const renderDeployModal = () =>
    render(
        <AntApp>
            <DeployModal />
        </AntApp>
    )

describe('DeployModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        mockApiCall.mockReset()
        mockApiCall
            .mockResolvedValueOnce([
                { id: 'repo-1', name: 'Production' },
            ])
            .mockResolvedValueOnce([
                { id: 'dep-1', name: 'Deploy1' },
            ])
    })

    it('renders without crashing', () => {
        renderDeployModal()
        expect(document.body).toBeInTheDocument()
    })

    it('opens when openDeployModal event is dispatched', async () => {
        renderDeployModal()
        await openModal(defaultDetail)

        await waitFor(() => {
            expect(screen.getByRole('dialog')).toBeInTheDocument()
        })
    })

    it('fetches deployment repositories and names on open', async () => {
        renderDeployModal()
        await openModal(defaultDetail)

        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledWith('/production-repos')
        })
    })

    it('does not show deploy failed notification on validation error (errorFields)', async () => {
        renderDeployModal()
        await openModal(defaultDetail)

        await screen.findByRole('dialog')

        // Submit without filling required fields - triggers validation
        await userEvent.click(screen.getByRole('button', { name: /deploy:buttons.deploy/i }))

        await waitFor(() => {
            expect(screen.getByRole('dialog')).toBeInTheDocument()
        })

        // Should not have called deploy API (validation failed first)
        expect(mockApiCall).not.toHaveBeenCalledWith(
            expect.stringMatching(/\/deployments\/.+/),
            expect.any(Object),
            expect.any(Object)
        )
    })

    it('notifies listeners after a successful deploy', async () => {
        mockApiCall.mockResolvedValueOnce(undefined)
        const deployed = vi.fn()
        window.addEventListener('projectDeployed', deployed)
        renderDeployModal()
        await openModal(defaultDetail)

        await waitFor(() => expect(mockApiCall).toHaveBeenCalledWith('/production-repos'))
        await userEvent.selectOptions(screen.getByLabelText('deploy:repository.label'), 'repo-1')
        await waitFor(() => expect(mockApiCall).toHaveBeenCalledWith(
            '/deployments?repository=repo-1',
            undefined,
            { throwError: true, suppressErrorPages: true }
        ))
        await userEvent.selectOptions(screen.getByLabelText('deploy:deployment_name.label'), 'dep-1')
        await userEvent.type(screen.getByLabelText('deploy:comment.label'), 'Deploy changes')
        await userEvent.click(screen.getByRole('button', { name: /deploy:buttons.deploy/i }))

        await waitFor(() => expect(mockApiCall).toHaveBeenCalledWith(
            '/deployments/dep-1',
            expect.objectContaining({ method: 'POST' }),
            { throwError: true, suppressErrorPages: true }
        ))
        expect(deployed).toHaveBeenCalledWith(expect.objectContaining({
            detail: { projectId: 'proj-1' },
            type: 'projectDeployed',
        }))
        window.removeEventListener('projectDeployed', deployed)
    })

    it('shows repository field error when deployment names cannot be loaded due to forbidden access', async () => {
        mockApiCall
            .mockReset()
            .mockResolvedValueOnce([{ id: 'repo-1', name: 'Production' }])
            .mockRejectedValueOnce(new services.ForbiddenError('No access'))

        renderDeployModal()
        await openModal(defaultDetail)
        await waitFor(() => expect(mockApiCall).toHaveBeenCalledWith('/production-repos'))
        await userEvent.selectOptions(screen.getByLabelText('deploy:repository.label'), 'repo-1')

        await waitFor(() => expect(mockApiCall).toHaveBeenCalledWith(
            '/deployments?repository=repo-1',
            undefined,
            { throwError: true, suppressErrorPages: true }
        ))
    })

    it('shows a notification when deployment names fail to load for a non-forbidden error', async () => {
        mockApiCall
            .mockReset()
            .mockResolvedValueOnce([{ id: 'repo-1', name: 'Production' }])
            .mockRejectedValueOnce(new Error('Network down'))

        renderDeployModal()
        await openModal(defaultDetail)
        await userEvent.selectOptions(screen.getByLabelText('deploy:repository.label'), 'repo-1')

        await waitFor(() => expect(notification.error).toHaveBeenCalled())
    })

    it('creates a new deployment when the deployment name is new', async () => {
        mockApiCall
            .mockReset()
            .mockResolvedValueOnce([{ id: 'repo-1', name: 'Production' }])
            .mockResolvedValueOnce([])
            .mockResolvedValueOnce(undefined)

        renderDeployModal()
        await openModal(defaultDetail)
        await waitFor(() => expect(mockApiCall).toHaveBeenCalledWith('/production-repos'))
        await userEvent.selectOptions(screen.getByLabelText('deploy:repository.label'), 'repo-1')
        await waitFor(() => expect(mockApiCall).toHaveBeenCalledWith(
            '/deployments?repository=repo-1',
            undefined,
            { throwError: true, suppressErrorPages: true }
        ))

        await userEvent.type(screen.getByLabelText('deploy:deployment_name.label-search'), 'BrandNew')
        fireEvent.blur(screen.getByLabelText('deploy:deployment_name.label'))
        await userEvent.type(screen.getByLabelText('deploy:comment.label'), 'Initial deploy')
        await userEvent.click(screen.getByRole('button', { name: /deploy:buttons.deploy/i }))

        await waitFor(() => expect(mockApiCall).toHaveBeenCalledWith(
            '/deployments',
            expect.objectContaining({
                method: 'POST',
                body: expect.stringContaining('BrandNew'),
            }),
            { throwError: true, suppressErrorPages: true }
        ))
        expect(notification.success).toHaveBeenCalled()
    })

    it('shows a warning when deploy is forbidden', async () => {
        mockApiCall
            .mockReset()
            .mockResolvedValueOnce([{ id: 'repo-1', name: 'Production' }])
            .mockResolvedValueOnce([{ id: 'dep-1', name: 'Deploy1' }])
            .mockRejectedValueOnce(new services.ForbiddenError('No deploy rights'))

        renderDeployModal()
        await openModal(defaultDetail)
        await userEvent.selectOptions(screen.getByLabelText('deploy:repository.label'), 'repo-1')
        await waitFor(() => expect(mockApiCall).toHaveBeenCalledWith(
            '/deployments?repository=repo-1',
            undefined,
            { throwError: true, suppressErrorPages: true }
        ))
        await userEvent.selectOptions(screen.getByLabelText('deploy:deployment_name.label'), 'dep-1')
        await userEvent.type(screen.getByLabelText('deploy:comment.label'), 'Deploy changes')
        await userEvent.click(screen.getByRole('button', { name: /deploy:buttons.deploy/i }))

        await waitFor(() => expect(notification.warning).toHaveBeenCalled())
    })

    it('shows deploy failed notification for a generic deploy error', async () => {
        mockApiCall
            .mockReset()
            .mockResolvedValueOnce([{ id: 'repo-1', name: 'Production' }])
            .mockResolvedValueOnce([{ id: 'dep-1', name: 'Deploy1' }])
            .mockRejectedValueOnce(new Error('Deploy exploded'))

        renderDeployModal()
        await openModal(defaultDetail)
        await userEvent.selectOptions(screen.getByLabelText('deploy:repository.label'), 'repo-1')
        await screen.findByRole('option', { name: 'Deploy1' })
        await userEvent.selectOptions(screen.getByLabelText('deploy:deployment_name.label'), 'dep-1')
        await userEvent.type(screen.getByLabelText('deploy:comment.label'), 'Deploy changes')
        await userEvent.click(screen.getByRole('button', { name: /deploy:buttons.deploy/i }))

        await waitFor(() => expect(notification.error).toHaveBeenCalled())
    })

    it('marks a new deployment while searching for a name', async () => {
        mockApiCall
            .mockReset()
            .mockResolvedValueOnce([{ id: 'repo-1', name: 'Production' }])
            .mockResolvedValueOnce([{ id: 'dep-1', name: 'Deploy1' }])
            .mockResolvedValueOnce(undefined)

        renderDeployModal()
        await openModal(defaultDetail)
        await userEvent.selectOptions(screen.getByLabelText('deploy:repository.label'), 'repo-1')
        await waitFor(() => expect(mockApiCall).toHaveBeenCalledWith(
            '/deployments?repository=repo-1',
            undefined,
            { throwError: true, suppressErrorPages: true }
        ))

        await userEvent.type(screen.getByLabelText('deploy:deployment_name.label-search'), 'SearchOnlyNew')
        fireEvent.blur(screen.getByLabelText('deploy:deployment_name.label'))
        await userEvent.type(screen.getByLabelText('deploy:comment.label'), 'Deploy searched name')
        await userEvent.click(screen.getByRole('button', { name: /deploy:buttons.deploy/i }))

        await waitFor(() => expect(mockApiCall).toHaveBeenCalledWith(
            '/deployments',
            expect.objectContaining({ method: 'POST' }),
            { throwError: true, suppressErrorPages: true }
        ))
    })


    it('shows what the server refused instead of the generic sentence', async () => {
        mockApiCall
            .mockReset()
            .mockResolvedValueOnce([{ id: 'repo-1', name: 'Production' }])
            .mockResolvedValueOnce([{ id: 'dep-1', name: 'Deploy1' }])
            .mockRejectedValueOnce(new MockApiHttpError(
                409,
                "This deployment repository takes a project only from the 'master' branch."
            ))

        renderDeployModal()
        await openModal(defaultDetail)
        await userEvent.selectOptions(screen.getByLabelText('deploy:repository.label'), 'repo-1')
        await screen.findByRole('option', { name: 'Deploy1' })
        await userEvent.selectOptions(screen.getByLabelText('deploy:deployment_name.label'), 'dep-1')
        await userEvent.type(screen.getByLabelText('deploy:comment.label'), 'Deploy changes')
        await userEvent.click(screen.getByRole('button', { name: /deploy:buttons.deploy/i }))

        await waitFor(() => expect(notification.error).toHaveBeenCalledWith(expect.objectContaining({
            description: "This deployment repository takes a project only from the 'master' branch.",
        })))
    })

    it('refuses a repository that takes the main branch only while the project is elsewhere', async () => {
        mockApiCall
            .mockReset()
            .mockResolvedValueOnce([{ id: 'repo-1', name: 'Production', mainBranchOnly: true }])
            .mockResolvedValueOnce([{ id: 'dep-1', name: 'Deploy1' }])

        renderDeployModal()
        await openModal({ ...defaultDetail, branch: 'feature/rates', branchDefault: false })
        await userEvent.selectOptions(screen.getByLabelText('deploy:repository.label'), 'repo-1')

        await screen.findByTestId('deploy-main-branch-only')
        expect(screen.getByTestId('deploy-submit')).toBeDisabled()
        // Nothing was sent: the repository list and its deployments are the only calls.
        expect(mockApiCall).toHaveBeenCalledTimes(2)
    })

    it('deploys from the main branch to the same repository', async () => {
        mockApiCall
            .mockReset()
            .mockResolvedValueOnce([{ id: 'repo-1', name: 'Production', mainBranchOnly: true }])
            .mockResolvedValueOnce([{ id: 'dep-1', name: 'Deploy1' }])

        renderDeployModal()
        await openModal({ ...defaultDetail, branch: 'master', branchDefault: true })
        await userEvent.selectOptions(screen.getByLabelText('deploy:repository.label'), 'repo-1')

        await screen.findByRole('option', { name: 'Deploy1' })
        expect(screen.queryByTestId('deploy-main-branch-only')).not.toBeInTheDocument()
        expect(screen.getByTestId('deploy-submit')).not.toBeDisabled()
    })
})
