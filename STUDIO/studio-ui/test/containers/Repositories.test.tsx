import React from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { Repositories } from 'containers/Repositories'

const mockNavigate = jest.fn()
jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
}))

jest.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key: string) => key,
        i18n: { language: 'en' },
    }),
}))

let mockHasUnsavedChanges = false
jest.mock('containers/repositories/DesignRepositoriesConfiguration', () => ({
    DesignRepositoriesConfiguration: React.forwardRef(
        (
            _props: { repositoryDataType: string; onEditingStateChange?: (v: boolean) => void },
            ref: React.Ref<{ hasUnsavedChanges: () => boolean; getForm: () => null; addRepository: () => Promise<void>; isEditingNewRepository: () => boolean }>
        ) => {
            React.useImperativeHandle(ref, () => ({
                hasUnsavedChanges: () => mockHasUnsavedChanges,
                getForm: () => null,
                addRepository: async () => {},
                isEditingNewRepository: () => false,
            }))
            return <div data-testid="design-repos-config">DesignRepositoriesConfiguration</div>
        }
    ),
}))

const mockConfirm = jest.fn()
jest.mock('antd', () => {
    const actual = jest.requireActual('antd')
    return {
        ...actual,
        Modal: {
            ...actual.Modal,
            confirm: (options: { onOk?: () => void }) => {
                mockConfirm(options)
                return () => {}
            },
        },
    }
})

const renderRepositories = (initialRoute = '/administration/repositories/design') =>
    render(
        <MemoryRouter initialEntries={[initialRoute]}>
            <Routes>
                <Route path="/administration/repositories/:repositoryTab" element={<Repositories />} />
            </Routes>
        </MemoryRouter>
    )

describe('Repositories', () => {
    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {})

    beforeEach(() => {
        jest.clearAllMocks()
        mockHasUnsavedChanges = false
    })

    afterAll(() => {
        consoleErrorSpy.mockRestore()
    })

    it('renders Design and Deployment tabs', () => {
        renderRepositories()
        expect(screen.getByRole('tab', { name: /repository:tabs.design_repositories/i })).toBeInTheDocument()
        expect(screen.getByRole('tab', { name: /repository:tabs.deployment_repositories/i })).toBeInTheDocument()
    })

    it('switches to Deployment tab without confirm when there are no unsaved changes', async () => {
        renderRepositories()
        mockHasUnsavedChanges = false

        await userEvent.click(screen.getByRole('tab', { name: /repository:tabs.deployment_repositories/i }))

        expect(mockConfirm).not.toHaveBeenCalled()
        expect(mockNavigate).toHaveBeenCalledWith('/administration/repositories/production')
    })

    it('shows confirm modal when switching tab with unsaved changes', async () => {
        renderRepositories()
        mockHasUnsavedChanges = true

        await userEvent.click(screen.getByRole('tab', { name: /repository:tabs.deployment_repositories/i }))

        expect(mockConfirm).toHaveBeenCalledWith(
            expect.objectContaining({
                title: 'repository:confirm_leave_without_saving',
                content: 'repository:confirm_leave_without_saving_message',
            })
        )
        expect(mockNavigate).not.toHaveBeenCalled()
    })

    it('navigates when user confirms leave with unsaved changes', async () => {
        renderRepositories()
        mockHasUnsavedChanges = true

        await userEvent.click(screen.getByRole('tab', { name: /repository:tabs.deployment_repositories/i }))

        const confirmOptions = mockConfirm.mock.calls[0][0]
        expect(confirmOptions.onOk).toBeDefined()
        confirmOptions.onOk!()

        expect(mockNavigate).toHaveBeenCalledWith('/administration/repositories/production')
    })
})
