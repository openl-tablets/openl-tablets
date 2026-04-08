import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { App as AntApp } from 'antd'
import { DeployModal } from 'containers/DeployModal'
import * as services from 'services'
jest.mock('services', () => ({
    apiCall: jest.fn(),
    ForbiddenError: class ForbiddenError extends Error {
        constructor(message?: string) {
            super(message)
            this.name = 'ForbiddenError'
        }
    },
}))

jest.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key: string) => key,
        i18n: { language: 'en' },
    }),
}))

jest.mock('antd', () => {
    const actual = jest.requireActual('antd')
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
            success: jest.fn(),
            error: jest.fn(),
            warning: jest.fn(),
        },
    }
})

const mockApiCall = services.apiCall as jest.MockedFunction<typeof services.apiCall>

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
    selectedBranches: [] as string[],
    status: '',
}

const renderDeployModal = () =>
    render(
        <AntApp>
            <DeployModal />
        </AntApp>
    )

describe('DeployModal', () => {
    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {})

    beforeEach(() => {
        jest.clearAllMocks()
        consoleErrorSpy.mockClear()
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

        await waitFor(() => expect(screen.getByRole('dialog')).toBeInTheDocument())

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

    afterAll(() => {
        consoleErrorSpy.mockRestore()
    })
})
