import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import * as services from 'services'
import { PermissionContext } from 'contexts/PermissionContext'
import { SystemContext } from 'contexts/SystemContext'
import { GroupsContext, GroupsProvider } from 'contexts/GroupsContext'

jest.mock('services', () => ({
    apiCall: jest.fn(),
}))

jest.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key: string) => key,
        i18n: { language: 'en' },
    }),
}))

const mockApiCall = services.apiCall as jest.MockedFunction<typeof services.apiCall>

const mockGroupsResponse = {
    Admins: {
        id: 1,
        description: 'Administrator group',
        privileges: ['ADMIN'],
        numberOfMembers: { external: 0, internal: 2, total: 2 },
    },
    Viewers: {
        id: 2,
        description: 'Read-only group',
        privileges: [],
        numberOfMembers: { external: 1, internal: 3, total: 4 },
    },
}

const Consumer: React.FC = () => {
    const { groups, loading, error, reloadGroups } = React.useContext(GroupsContext)
    return (
        <div>
            <span data-testid="loading">{String(loading)}</span>
            <span data-testid="error">{error?.message ?? 'none'}</span>
            <span data-testid="count">{groups.length}</span>
            {groups.map((g) => (
                <span data-testid="group" key={g.name}>{g.name}</span>
            ))}
            <button onClick={reloadGroups}>reload</button>
        </div>
    )
}

// Stable function refs to prevent useCallback re-creation in GroupsProvider
const hasAdminTrue = () => true
const hasAdminFalse = () => false

const renderWithProviders = (opts: { hasAdmin?: boolean; groupsEnabled?: boolean } = {}) => {
    const { hasAdmin = true, groupsEnabled = true } = opts
    return render(
        <SystemContext.Provider
            value={{
                isExternalAuthSystem: false,
                isUserManagementEnabled: true,
                isGroupsManagementEnabled: groupsEnabled,
                isPersonalAccessTokenEnabled: false,
            }}
        >
            <PermissionContext.Provider value={{ hasAdminPermission: hasAdmin ? hasAdminTrue : hasAdminFalse }}>
                <GroupsProvider>
                    <Consumer />
                </GroupsProvider>
            </PermissionContext.Provider>
        </SystemContext.Provider>
    )
}

describe('GroupsProvider', () => {
    beforeEach(() => {
        jest.clearAllMocks()
    })

    it('fetches and maps groups for admin user', async () => {
        mockApiCall.mockResolvedValueOnce(mockGroupsResponse)

        await act(async () => {
            renderWithProviders()
        })

        await waitFor(() => {
            expect(screen.getByTestId('count')).toHaveTextContent('2')
        })
        expect(mockApiCall).toHaveBeenCalledWith('/admin/management/groups')

        const groupEls = screen.getAllByTestId('group')
        expect(groupEls.map((el) => el.textContent)).toEqual(['Admins', 'Viewers'])
    })

    it('skips fetch when user is not admin', async () => {
        await act(async () => {
            renderWithProviders({ hasAdmin: false })
        })

        await waitFor(() => {
            expect(screen.getByTestId('loading')).toHaveTextContent('false')
        })
        expect(screen.getByTestId('count')).toHaveTextContent('0')
        expect(mockApiCall).not.toHaveBeenCalled()
    })

    it('skips fetch when groups management is disabled', async () => {
        await act(async () => {
            renderWithProviders({ groupsEnabled: false })
        })

        await waitFor(() => {
            expect(screen.getByTestId('loading')).toHaveTextContent('false')
        })
        expect(screen.getByTestId('count')).toHaveTextContent('0')
        expect(mockApiCall).not.toHaveBeenCalled()
    })

    it('sets error state when fetch fails', async () => {
        mockApiCall.mockRejectedValueOnce(new Error('Network error'))

        await act(async () => {
            renderWithProviders()
        })

        await waitFor(() => {
            expect(screen.getByTestId('error')).toHaveTextContent('Network error')
        })
        expect(screen.getByTestId('count')).toHaveTextContent('0')
    })

    it('reloads groups on reloadGroups call', async () => {
        const updatedResponse = {
            NewGroup: {
                id: 3,
                description: '',
                privileges: [],
                numberOfMembers: { external: 0, internal: 0, total: 0 },
            },
        }
        mockApiCall.mockResolvedValue(mockGroupsResponse)

        await act(async () => {
            renderWithProviders()
        })

        await waitFor(() => {
            expect(screen.getByTestId('count')).toHaveTextContent('2')
        })

        // Change mock for next call, then trigger reload
        mockApiCall.mockResolvedValue(updatedResponse)

        await userEvent.click(screen.getByText('reload'))
        await waitFor(() => {
            expect(screen.getByTestId('count')).toHaveTextContent('1')
        }, { timeout: 3000 })
    })
})
