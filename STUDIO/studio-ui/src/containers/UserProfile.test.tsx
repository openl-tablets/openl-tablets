import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { notification } from 'antd'
import { UserProfile } from './UserProfile'
import { SystemContext } from '../contexts'
import { SystemUserMode } from '../constants/system'
import * as services from '../services'
import type { MockedFunction } from 'vitest'
import type { SystemSettings } from '../types/system'

vi.mock('../services', () => ({ apiCall: vi.fn() }))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

vi.mock('./users/UserDatailsTab', () => ({
    UserDetailsTab: ({ requireEmailAndDisplayName }: { requireEmailAndDisplayName?: boolean }) => (
        <div
            data-required-fields={requireEmailAndDisplayName}
            data-testid="user-details-tab"
        />
    ),
}))

vi.mock('../hooks/useIsFormChanged', () => ({
    useIsFormChanged: () => true,
}))

vi.mock('store', () => ({
    useUserStore: () => ({
        userProfile: {
            email: 'admin@example.com',
            firstName: 'Ada',
            lastName: 'Admin',
            displayName: 'Ada Admin',
            externalFlags: {},
        },
        fetchUserProfile: vi.fn(),
    }),
}))

vi.mock('antd', async () => {
    const actual = await vi.importActual('antd')
    return {
        ...actual,
        notification: { error: vi.fn(), success: vi.fn(), warning: vi.fn() },
    }
})

const mockApiCall = services.apiCall as MockedFunction<typeof services.apiCall>

describe('UserProfile', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('shows a success notification when the profile is saved', async () => {
        mockApiCall.mockResolvedValueOnce(undefined)
        await act(async () => {
            render(<UserProfile />)
        })

        await userEvent.click(screen.getByRole('button', { name: 'common:btn.save' }))

        await waitFor(() => expect(notification.success).toHaveBeenCalledWith({
            title: 'users:user_profile_updated_successfully',
        }))
    })

    it('requires email and display name in the profile editor', async () => {
        await act(async () => {
            render(<UserProfile />)
        })

        expect(screen.getByTestId('user-details-tab')).toHaveAttribute('data-required-fields', 'true')
    })

    it('shows an error notification when saving the profile fails', async () => {
        mockApiCall.mockRejectedValueOnce(new Error('save failed'))
        await act(async () => {
            render(<UserProfile />)
        })

        await userEvent.click(screen.getByRole('button', { name: 'common:btn.save' }))

        await waitFor(() => expect(notification.error).toHaveBeenCalledWith({ title: 'save failed' }))
    })

    const renderWithUserMode = async (userMode?: SystemUserMode) => {
        const contextValue = {
            systemSettings: { userMode, supportedFeatures: {} } as SystemSettings,
            isExternalAuthSystem: userMode === SystemUserMode.EXTERNAL,
            isUserManagementEnabled: false,
            isGroupsManagementEnabled: false,
            isPersonalAccessTokenEnabled: false,
        }
        await act(async () => {
            render(
                <SystemContext.Provider value={contextValue}>
                    <UserProfile />
                </SystemContext.Provider>
            )
        })
    }

    it('shows the change password section for internal user management (multi mode)', async () => {
        await renderWithUserMode(SystemUserMode.INTERNAL)

        expect(screen.getByText('users:edit_modal.change_password')).toBeDefined()
    })

    it('hides the change password section in single user mode', async () => {
        await renderWithUserMode(undefined)

        expect(screen.queryByText('users:edit_modal.change_password')).toBeNull()
    })

    it('hides the change password section for external user management', async () => {
        await renderWithUserMode(SystemUserMode.EXTERNAL)

        expect(screen.queryByText('users:edit_modal.change_password')).toBeNull()
    })
})
