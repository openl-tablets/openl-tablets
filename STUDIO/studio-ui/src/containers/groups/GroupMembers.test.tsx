import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import * as services from 'services'
import type { MockedFunction } from 'vitest'

vi.mock('services', async () => ({
    apiCall: vi.fn(),
}))

vi.mock('react-i18next', async () => {
    const t = (key: string) => key
    const i18n = { language: 'en' }
    return {
        useTranslation: () => ({ t, i18n }),
    }
})

const mockApiCall = services.apiCall as MockedFunction<typeof services.apiCall>

import { GroupMembers } from 'containers/groups/GroupMembers'

describe('GroupMembers', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('fetches and renders the members of the group', async () => {
        mockApiCall.mockResolvedValue([
            { username: 'jdoe', displayName: 'Joe Doe' },
            { username: 'jsmith', displayName: 'John Smith' },
        ])

        await act(async () => {
            render(<GroupMembers groupId={42} />)
        })

        expect(mockApiCall).toHaveBeenCalledWith('/admin/management/groups/42/users', undefined,
            { suppressErrorPages: true, throwError: true })
        await waitFor(() => {
            expect(screen.getByText('jdoe')).toBeInTheDocument()
        })
        expect(screen.getByText('jsmith')).toBeInTheDocument()
    })

    it('shows a message when the group has no members', async () => {
        mockApiCall.mockResolvedValue([])

        await act(async () => {
            render(<GroupMembers groupId={42} />)
        })

        await waitFor(() => {
            expect(screen.getByText('groups:no_members')).toBeInTheDocument()
        })
    })

    it('shows an error when the members cannot be loaded', async () => {
        mockApiCall.mockRejectedValue(new Error('boom'))

        await act(async () => {
            render(<GroupMembers groupId={42} />)
        })

        await waitFor(() => {
            expect(screen.getByText('groups:failed_to_load_members')).toBeInTheDocument()
        })
    })
})
