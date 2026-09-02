import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { notification } from 'antd'
import { EditUserModal } from './EditUserModal'
import * as services from '../../services'
import type { UserDetails } from '../../types/user'
import type { MockedFunction } from 'vitest'

vi.mock('../../services', () => ({ apiCall: vi.fn() }))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

vi.mock('./UserDatailsTab', () => ({
    UserDetailsTab: ({ requireEmailAndDisplayName }: { requireEmailAndDisplayName?: boolean }) => (
        <div
            data-required-fields={requireEmailAndDisplayName}
            data-testid="user-details-tab"
        />
    ),
}))

vi.mock('antd', async () => {
    const actual = await vi.importActual('antd')
    return {
        ...actual,
        notification: { error: vi.fn(), success: vi.fn() },
    }
})

const mockApiCall = services.apiCall as MockedFunction<typeof services.apiCall>

const user = {
    username: 'jdoe',
    email: 'jdoe@example.com',
    displayName: 'John Doe',
    firstName: 'John',
    lastName: 'Doe',
    userGroups: [],
} as unknown as UserDetails

const renderModal = async (updateUser = vi.fn()) => {
    await act(async () => {
        render(
            <EditUserModal
                closeModal={vi.fn()}
                onAddUser={vi.fn()}
                updateUser={updateUser}
                user={user}
            />
        )
    })
    return updateUser
}

describe('EditUserModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('requires email and display name', async () => {
        await renderModal()

        expect(screen.getByTestId('user-details-tab')).toHaveAttribute('data-required-fields', 'true')
    })

    it('shows an error notification when updating the user fails', async () => {
        mockApiCall.mockRejectedValueOnce(new Error('update failed'))
        const updateUser = await renderModal()

        await userEvent.click(screen.getByRole('button', { name: 'common:btn.save' }))

        await waitFor(() => expect(notification.error).toHaveBeenCalledWith({ title: 'update failed' }))
        expect(updateUser).not.toHaveBeenCalled()
    })

    it('shows a generic error notification when the update returns no response', async () => {
        mockApiCall.mockResolvedValueOnce(undefined)
        const updateUser = await renderModal()

        await userEvent.click(screen.getByRole('button', { name: 'common:btn.save' }))

        await waitFor(() => expect(notification.error).toHaveBeenCalledWith({ title: 'common:error' }))
        expect(updateUser).not.toHaveBeenCalled()
    })
})
