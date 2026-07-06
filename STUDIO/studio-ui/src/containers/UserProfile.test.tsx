import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { notification } from 'antd'
import { UserProfile } from './UserProfile'
import * as services from '../services'
import type { MockedFunction } from 'vitest'

vi.mock('../services', () => ({ apiCall: vi.fn() }))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

vi.mock('./users/UserDatailsTab', () => ({
    UserDetailsTab: () => <div data-testid="user-details-tab" />,
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

    it('shows an error notification when saving the profile fails', async () => {
        mockApiCall.mockRejectedValueOnce(new Error('save failed'))
        await act(async () => {
            render(<UserProfile />)
        })

        await userEvent.click(screen.getByRole('button', { name: 'common:btn.save' }))

        await waitFor(() => expect(notification.error).toHaveBeenCalledWith({ title: 'save failed' }))
    })
})
