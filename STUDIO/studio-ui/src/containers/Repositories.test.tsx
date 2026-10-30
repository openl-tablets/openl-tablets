import React from 'react'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router'
import { Repositories } from 'containers/Repositories'
import { apiCall } from 'services'
import { notifyLoadFailure } from 'services/apiCall'

vi.mock('services', () => ({ apiCall: vi.fn() }))
// A notification renders outside the screen, where nothing unmounts it: it would close after the test environment.
vi.mock('services/apiCall', async importOriginal => ({
    ...await importOriginal<typeof import('services/apiCall')>(),
    notifyLoadFailure: vi.fn(),
}))

/** Answers the lists of repositories, the ones named read-only. */
const listsSetOutside = (...readOnly: string[]) => {
    vi.mocked(apiCall).mockImplementation(async (url: string) => ({
        readOnly: readOnly.some(repositoryDataType => url === `/admin/settings/repos/${repositoryDataType}/configs`),
    }))
}

const mockNavigate = vi.fn()
vi.mock('react-router', async () => ({
    ...await vi.importActual('react-router'),
    useNavigate: () => mockNavigate,
}))

vi.mock('react-i18next', async () => {
    const t = (key: string) => key
    const i18n = { language: 'en' }
    return {
        useTranslation: () => ({ t, i18n }),
    }
})

let mockHasUnsavedChanges = false
const mockConfigurationProps = vi.fn()
vi.mock('containers/repositories/DesignRepositoriesConfiguration', async () => ({
    DesignRepositoriesConfiguration: React.forwardRef(
        (
            props: { repositoryDataType: string; onEditingStateChange?: (v: boolean) => void; isListReadOnly?: boolean },
            ref: React.Ref<{ hasUnsavedChanges: () => boolean; getForm: () => null; addRepository: () => Promise<void>; isEditingNewRepository: () => boolean }>
        ) => {
            React.useImperativeHandle(ref, () => ({
                hasUnsavedChanges: () => mockHasUnsavedChanges,
                getForm: () => null,
                addRepository: async () => {},
                isEditingNewRepository: () => false,
            }))
            mockConfigurationProps(props)
            return <div data-testid="design-repos-config">DesignRepositoriesConfiguration</div>
        }
    ),
}))

const mockConfirm = vi.fn()
vi.mock('antd', async () => {
    const actual = await vi.importActual<typeof import('antd')>('antd')
    const { withStaticApp } = await import('testing/staticAntdApp')
    return withStaticApp({
        ...actual,
        Modal: {
            ...actual.Modal,
            confirm: (options: { onOk?: () => void }) => {
                mockConfirm(options)
                return () => {}
            },
        },
    })
})

/** Renders the screen and waits until it has read the lists of repositories. */
const renderRepositories = async (initialRoute = '/administration/repositories/design') => {
    render(
        <MemoryRouter initialEntries={[initialRoute]}>
            <Routes>
                <Route element={<Repositories />} path="/administration/repositories/:repositoryTab" />
            </Routes>
        </MemoryRouter>
    )
    await waitFor(() => expect(mockConfigurationProps).toHaveBeenLastCalledWith(
        expect.objectContaining({ isListReadOnly: expect.any(Boolean) })))
}

describe('Repositories', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        mockHasUnsavedChanges = false
        listsSetOutside()
    })

    it('renders Design and Deployment tabs', async () => {
        await renderRepositories()
        expect(screen.getByRole('tab', { name: /repository:tabs.design_repositories/i })).toBeInTheDocument()
        expect(screen.getByRole('tab', { name: /repository:tabs.deployment_repositories/i })).toBeInTheDocument()
    })

    it('switches to Deployment tab without confirm when there are no unsaved changes', async () => {
        await renderRepositories()
        mockHasUnsavedChanges = false

        await userEvent.click(screen.getByRole('tab', { name: /repository:tabs.deployment_repositories/i }))

        expect(mockConfirm).not.toHaveBeenCalled()
        expect(mockNavigate).toHaveBeenCalledWith('/administration/repositories/production')
    })

    it('shows confirm modal when switching tab with unsaved changes', async () => {
        await renderRepositories()
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
        await renderRepositories()
        mockHasUnsavedChanges = true

        await userEvent.click(screen.getByRole('tab', { name: /repository:tabs.deployment_repositories/i }))

        const confirmOptions = mockConfirm.mock.calls[0]![0]
        expect(confirmOptions.onOk).toBeDefined()
        confirmOptions.onOk!()

        expect(mockNavigate).toHaveBeenCalledWith('/administration/repositories/production')
    })

    it('offers to add and remove repositories while their list is not set outside', async () => {
        await renderRepositories()

        expect(screen.getByRole('button', { name: /repository:add_design_repository/ })).toBeEnabled()
        expect(mockConfigurationProps).toHaveBeenLastCalledWith(expect.objectContaining({ isListReadOnly: false }))
    })

    it('neither adds nor removes repositories while their list is set outside', async () => {
        listsSetOutside('design')
        await renderRepositories()

        expect(screen.getByRole('button', { name: /repository:add_design_repository/ })).toBeDisabled()
        expect(mockConfigurationProps).toHaveBeenLastCalledWith(
            expect.objectContaining({ repositoryDataType: 'design', isListReadOnly: true }))
        await userEvent.hover(screen.getByRole('button', { name: /repository:add_design_repository/ }))
        expect(await screen.findByText('repository:list_set_outside')).toBeInTheDocument()
    })

    it('offers to add a repository when it cannot read whether the list is set outside', async () => {
        const failure = new Error('The server is not available')
        vi.mocked(apiCall).mockRejectedValue(failure)
        await renderRepositories()

        expect(screen.getByRole('button', { name: /repository:add_design_repository/ })).toBeEnabled()
        expect(notifyLoadFailure).toHaveBeenCalledWith('repository:list_load_failed', failure)
    })

    it('adds deployment repositories while only the list of the design repositories is set outside', async () => {
        listsSetOutside('design')
        await renderRepositories('/administration/repositories/production')

        expect(screen.getByRole('button', { name: /repository:add_deployment_repository/ })).toBeEnabled()
    })
})
