import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { notification } from 'antd'
import { UserSettings } from './UserSettings'
import * as services from '../services'
import type { MockedFunction } from 'vitest'

vi.mock('../services', () => ({ apiCall: vi.fn() }))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

vi.mock('store', () => ({
    useUserStore: () => ({
        userProfile: {
            profiles: [],
            showHeader: true,
            showFormulas: false,
            testsPerPage: 5,
            testsFailuresOnly: false,
            showComplexResult: false,
            showRealNumbers: false,
        },
        fetchUserProfile: vi.fn(),
    }),
}))

vi.mock('antd', async () => {
    const actual = await vi.importActual('antd')
    return {
        ...actual,
        notification: { error: vi.fn(), success: vi.fn() },
    }
})

const mockApiCall = services.apiCall as MockedFunction<typeof services.apiCall>

describe('UserSettings', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('shows a success notification when settings are saved', async () => {
        mockApiCall.mockResolvedValueOnce(undefined)
        await act(async () => {
            render(<UserSettings />)
        })

        await userEvent.click(screen.getByRole('button', { name: 'common:btn.save' }))

        await waitFor(() => expect(notification.success).toHaveBeenCalledWith({
            title: 'users:user_settings_updated_successfully',
        }))
    })

    it('shows an error notification when saving settings fails', async () => {
        mockApiCall.mockRejectedValueOnce(new Error('save failed'))
        await act(async () => {
            render(<UserSettings />)
        })

        await userEvent.click(screen.getByRole('button', { name: 'common:btn.save' }))

        await waitFor(() => expect(notification.error).toHaveBeenCalledWith({ title: 'save failed' }))
    })
})
