import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { notification } from 'antd'
import { UserProfile } from './UserProfile'
import { SystemContext } from '../contexts'
import { SystemUserMode } from '../constants/system'
import * as services from '../services'
import { useUserStore } from 'store'
import type { MockedFunction } from 'vitest'
import type { SystemSettings } from '../types/system'
import type { UserProfile as StoredProfile } from '../types/user'

vi.mock('../services', () => ({ apiCall: vi.fn() }))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

// The details tab stands in with two details of its own, so a test can watch the form follow the profile.
vi.mock('./users/UserDatailsTab', async () => {
    const { Form, Input } = await vi.importActual<typeof import('antd')>('antd')
    return {
        UserDetailsTab: ({ requireEmailAndDisplayName }: { requireEmailAndDisplayName?: boolean }) => (
            <div
                data-required-fields={requireEmailAndDisplayName}
                data-testid="user-details-tab"
            >
                <Form.Item label="firstName" name="firstName">
                    <Input />
                </Form.Item>
                <Form.Item label="lastName" name="lastName">
                    <Input />
                </Form.Item>
            </div>
        ),
    }
})

vi.mock('../hooks/useIsFormChanged', () => ({
    useIsFormChanged: () => true,
}))

// A real store, so the page renders the profile it holds and renders again when a save reads it anew.
vi.mock('store', async () => {
    const { create } = await import('zustand')
    return { useUserStore: create(() => ({})) }
})

const profile = {
    email: 'admin@example.com',
    firstName: 'Ada',
    lastName: 'Admin',
    displayName: 'Ada Admin',
    externalFlags: {},
    showHeader: true,
    testsPerPage: 5,
} as StoredProfile

vi.mock('antd', async () => {
    const actual = await vi.importActual('antd')
    const { withStaticApp } = await import('testing/staticAntdApp')
    return withStaticApp({
        ...actual,
        notification: { error: vi.fn(), success: vi.fn(), warning: vi.fn() },
    })
})

const mockApiCall = services.apiCall as MockedFunction<typeof services.apiCall>

describe('UserProfile', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        useUserStore.setState({ userProfile: profile, fetchUserProfile: vi.fn(async () => undefined) })
    })

    it('shows a success notification when the profile is saved', async () => {
        mockApiCall.mockResolvedValueOnce(undefined)
        render(<UserProfile />)

        await userEvent.click(screen.getByRole('button', { name: 'common:btn.save' }))

        await waitFor(() => expect(notification.success).toHaveBeenCalledWith({
            title: 'users:user_profile_updated_successfully',
        }))
    })

    it('sends only the details the user changed, and never the settings', async () => {
        mockApiCall.mockResolvedValueOnce(undefined)
        render(<UserProfile />)

        await userEvent.click(screen.getByRole('button', { name: 'common:btn.save' }))

        await waitFor(() => expect(mockApiCall).toHaveBeenCalledWith('/users/profile', expect.anything(), expect.anything()))
        const request = mockApiCall.mock.calls[0]?.[1] as RequestInit
        expect(JSON.parse(request.body as string)).toEqual({
            changePassword: { newPassword: '', currentPassword: '', confirmPassword: '' },
        })
    })

    it('keeps what the user types while the page reads the profile on opening', async () => {
        let finishReading = () => {}
        useUserStore.setState({
            fetchUserProfile: vi.fn(() => new Promise<void>(resolve => {
                finishReading = () => {
                    useUserStore.setState({ userProfile: { ...profile, firstName: 'Augusta', lastName: 'King' } })
                    resolve()
                }
            })),
        })
        render(<UserProfile />)

        await userEvent.clear(screen.getByLabelText('lastName'))
        await userEvent.type(screen.getByLabelText('lastName'), 'Byron')
        act(() => finishReading())

        // The detail read anew reaches the untouched field, and the one being edited keeps what was typed.
        await waitFor(() => expect(screen.getByLabelText('firstName')).toHaveValue('Augusta'))
        expect(screen.getByLabelText('lastName')).toHaveValue('Byron')
    })

    it('requires email and display name in the profile editor', async () => {
        render(<UserProfile />)

        expect(screen.getByTestId('user-details-tab')).toHaveAttribute('data-required-fields', 'true')
    })

    it('shows an error notification when saving the profile fails', async () => {
        mockApiCall.mockRejectedValueOnce(new Error('save failed'))
        render(<UserProfile />)

        await userEvent.click(screen.getByRole('button', { name: 'common:btn.save' }))

        await waitFor(() => expect(notification.error).toHaveBeenCalledWith({ title: 'save failed' }))
    })

    const renderWithUserMode = (userMode?: SystemUserMode) => {
        const contextValue = {
            systemSettings: { userMode, supportedFeatures: {} } as SystemSettings,
            isExternalAuthSystem: userMode === SystemUserMode.EXTERNAL,
            isUserManagementEnabled: false,
            isGroupsManagementEnabled: false,
            isPersonalAccessTokenEnabled: false,
        }
        render(
            <SystemContext.Provider value={contextValue}>
                <UserProfile />
            </SystemContext.Provider>
        )
    }

    it('shows the change password section for internal user management (multi mode)', async () => {
        renderWithUserMode(SystemUserMode.INTERNAL)

        expect(screen.getByText('users:edit_modal.change_password')).toBeDefined()
    })

    it('keeps what another tab saved when a second save follows the first', async () => {
        // The profile the first save reads back holds a change another tab saved in the meantime.
        useUserStore.setState({
            fetchUserProfile: vi.fn()
                .mockResolvedValueOnce(undefined)
                .mockImplementationOnce(async () => {
                    useUserStore.setState({ userProfile: { ...profile, lastName: 'Lovelace' } })
                }),
        })
        mockApiCall.mockResolvedValue(undefined)
        render(<UserProfile />)

        await userEvent.click(screen.getByRole('button', { name: 'common:btn.save' }))
        await waitFor(() => expect(screen.getByLabelText('lastName')).toHaveValue('Lovelace'))

        // jsdom never ends the leave motion of the loading icon, so the name keeps it: match the label, wait for the state.
        const save = screen.getByRole('button', { name: /common:btn\.save/ })
        await waitFor(() => expect(save).not.toHaveClass('ant-btn-loading'))
        await userEvent.click(save)

        await waitFor(() => expect(mockApiCall).toHaveBeenCalledTimes(2))
        const secondBody = JSON.parse((mockApiCall.mock.calls[1]?.[1] as RequestInit).body as string)
        expect(secondBody).toEqual({ changePassword: { newPassword: '', currentPassword: '', confirmPassword: '' } })
    })

    it('hides the change password section in single user mode', async () => {
        renderWithUserMode(undefined)

        expect(screen.queryByText('users:edit_modal.change_password')).toBeNull()
    })

    it('hides the change password section for external user management', async () => {
        renderWithUserMode(SystemUserMode.EXTERNAL)

        expect(screen.queryByText('users:edit_modal.change_password')).toBeNull()
    })
})
