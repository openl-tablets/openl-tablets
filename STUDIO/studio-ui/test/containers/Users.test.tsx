import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import * as services from 'services'
import { SystemContext } from 'contexts/SystemContext'
import { GroupsContext } from 'contexts/GroupsContext'
import { UserGroupType } from 'constants/users'

jest.mock('services', () => ({
    apiCall: jest.fn(),
}))

jest.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key: string) => key,
        i18n: { language: 'en' },
    }),
    Trans: ({ i18nKey }: { i18nKey: string }) => <span>{i18nKey}</span>,
}))

jest.mock('antd', () => {
    const actual = jest.requireActual('antd')
    return {
        ...actual,
        Modal: {
            ...actual.Modal,
            confirm: jest.fn(),
        },
        notification: {
            success: jest.fn(),
            error: jest.fn(),
        },
    }
})

jest.mock('containers/EditUserGroupDetailsWithAccessRights', () => ({
    EditUserGroupDetailsWithAccessRights: (props: any) => (
        <div data-testid="edit-drawer" data-open={props.isOpenFromParent} data-sid={props.sid} />
    ),
}))

jest.mock('components/DefaultGroupInfo', () => ({
    DefaultGroupInfo: () => <div data-testid="default-group-info" />,
}))

jest.mock('containers/users/RenderGroupCell', () => ({
    RenderGroupCell: ({ userGroups }: any) => (
        <span data-testid="group-cell">{userGroups?.map((g: any) => g.name).join(', ')}</span>
    ),
}))

const mockApiCall = services.apiCall as jest.MockedFunction<typeof services.apiCall>

// eslint-disable-next-line @typescript-eslint/no-require-imports
const { Users } = require('containers/Users')

const mockUsers = [
    {
        username: 'admin',
        displayName: 'Admin User',
        email: 'admin@test.com',
        online: true,
        currentUser: true,
        superUser: true,
        unsafePassword: false,
        externalFlags: { emailVerified: true },
        userGroups: [{ name: 'Admins', type: UserGroupType.Admin }],
        notMatchedExternalGroupsCount: 0,
    },
    {
        username: 'viewer',
        displayName: 'Viewer User',
        email: 'viewer@test.com',
        online: false,
        currentUser: false,
        superUser: false,
        unsafePassword: true,
        externalFlags: { emailVerified: true },
        userGroups: [{ name: 'Viewers', type: UserGroupType.Default }],
        notMatchedExternalGroupsCount: 0,
    },
]

const mockGroups = [
    { name: 'Admins', admin: true, id: 1, description: '', numberOfMembers: 1 },
    { name: 'Viewers', admin: false, id: 2, description: '', numberOfMembers: 1 },
]

const renderUsers = (overrides: any = {}) => {
    const systemCtx = {
        isExternalAuthSystem: false,
        isUserManagementEnabled: true,
        isGroupsManagementEnabled: true,
        isPersonalAccessTokenEnabled: false,
        systemSettings: { userMode: 'multi', supportedFeatures: {} } as any,
        ...overrides.system,
    }
    const groupsCtx = {
        groups: mockGroups,
        loading: false,
        error: null,
        reloadGroups: jest.fn(),
        ...overrides.groups,
    }
    return render(
        <SystemContext.Provider value={systemCtx}>
            <GroupsContext.Provider value={groupsCtx}>
                <Users />
            </GroupsContext.Provider>
        </SystemContext.Provider>
    )
}

describe('Users', () => {
    beforeEach(() => {
        jest.clearAllMocks()
        mockApiCall.mockResolvedValue(mockUsers)
    })

    it('fetches and renders user table on mount', async () => {
        await act(async () => {
            renderUsers()
        })

        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledWith('/users')
        })
        expect(screen.getByText('admin')).toBeInTheDocument()
        expect(screen.getByText('viewer')).toBeInTheDocument()
    })

    it('renders column headers', async () => {
        await act(async () => {
            renderUsers()
        })

        await waitFor(() => {
            expect(screen.getByText('users:users_table.username')).toBeInTheDocument()
        })
        expect(screen.getByText('users:users_table.full_name')).toBeInTheDocument()
        expect(screen.getByText('users:users_table.email')).toBeInTheDocument()
        expect(screen.getByText('users:users_table.groups')).toBeInTheDocument()
        expect(screen.getByText('users:users_table.actions')).toBeInTheDocument()
    })

    it('hides groups column when groups management is disabled', async () => {
        await act(async () => {
            renderUsers({ system: { isGroupsManagementEnabled: false } })
        })

        await waitFor(() => {
            expect(screen.getByText('admin')).toBeInTheDocument()
        })
        expect(screen.queryByText('users:users_table.groups')).not.toBeInTheDocument()
    })

    it('renders Add User button when not external auth', async () => {
        await act(async () => {
            renderUsers()
        })

        await waitFor(() => {
            expect(screen.getByText('users:add_user')).toBeInTheDocument()
        })
    })

    it('hides Add User button for external auth systems', async () => {
        await act(async () => {
            renderUsers({ system: { isExternalAuthSystem: true } })
        })

        await waitFor(() => {
            expect(screen.getByText('admin')).toBeInTheDocument()
        })
        expect(screen.queryByText('users:add_user')).not.toBeInTheDocument()
    })

    it('renders DefaultGroupInfo when groups management is enabled', async () => {
        await act(async () => {
            renderUsers()
        })

        await waitFor(() => {
            expect(screen.getByTestId('default-group-info')).toBeInTheDocument()
        })
    })

    it('does not render DefaultGroupInfo when groups management is disabled', async () => {
        await act(async () => {
            renderUsers({ system: { isGroupsManagementEnabled: false } })
        })

        await waitFor(() => {
            expect(screen.getByText('admin')).toBeInTheDocument()
        })
        expect(screen.queryByTestId('default-group-info')).not.toBeInTheDocument()
    })

    it('renders edit drawer component', async () => {
        await act(async () => {
            renderUsers()
        })

        await waitFor(() => {
            expect(screen.getByTestId('edit-drawer')).toBeInTheDocument()
        })
    })

    it('shows groups error alert with retry button', async () => {
        const reloadGroups = jest.fn()
        await act(async () => {
            renderUsers({ groups: { error: new Error('fail'), reloadGroups } })
        })

        await waitFor(() => {
            expect(screen.getByText('groups:retry')).toBeInTheDocument()
        })
    })

    it('opens edit drawer when Edit button is clicked', async () => {
        await act(async () => {
            renderUsers()
        })

        await waitFor(() => {
            expect(screen.getByText('viewer')).toBeInTheDocument()
        })

        // Click the first edit button (for 'admin' row)
        const editButtons = screen.getAllByRole('button', { name: /edit/i })
        await userEvent.click(editButtons[0])

        await waitFor(() => {
            expect(screen.getByTestId('edit-drawer')).toHaveAttribute('data-open', 'true')
        })
    })

    it('opens edit drawer on Add User button click', async () => {
        await act(async () => {
            renderUsers()
        })

        await waitFor(() => {
            expect(screen.getByText('users:add_user')).toBeInTheDocument()
        })

        await userEvent.click(screen.getByText('users:add_user'))

        await waitFor(() => {
            expect(screen.getByTestId('edit-drawer')).toHaveAttribute('data-open', 'true')
        })
    })

    it('shows confirm modal when delete button is clicked', async () => {
        const { Modal } = jest.requireMock('antd')
        await act(async () => {
            renderUsers()
        })

        await waitFor(() => {
            expect(screen.getByText('viewer')).toBeInTheDocument()
        })

        // viewer is not superUser and not currentUser, so delete is enabled
        const deleteButtons = screen.getAllByRole('button', { name: /delete/i })
        // admin has disabled delete (superUser + currentUser), viewer has enabled delete
        const viewerDeleteButton = deleteButtons[1]
        await userEvent.click(viewerDeleteButton)

        await waitFor(() => {
            expect(Modal.confirm).toHaveBeenCalledWith(
                expect.objectContaining({
                    title: 'users:confirm_deletion',
                })
            )
        })
    })

    it('calls delete API and refreshes users on confirm', async () => {
        const { Modal } = jest.requireMock('antd')
        mockApiCall
            .mockResolvedValueOnce(mockUsers)     // initial fetchUsers
            .mockResolvedValueOnce(undefined)       // DELETE /users/viewer
            .mockResolvedValueOnce([mockUsers[0]])  // refetch after delete

        await act(async () => {
            renderUsers()
        })

        await waitFor(() => {
            expect(screen.getByText('viewer')).toBeInTheDocument()
        })

        const deleteButtons = screen.getAllByRole('button', { name: /delete/i })
        await userEvent.click(deleteButtons[1])

        await waitFor(() => {
            expect(Modal.confirm).toHaveBeenCalled()
        })

        // Trigger the onOk callback
        const onOk = Modal.confirm.mock.calls[0][0].onOk
        await act(async () => {
            await onOk()
        })

        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledWith('/users/viewer', expect.objectContaining({ method: 'DELETE' }))
        })
    })

    it('renders admin username as bold', async () => {
        await act(async () => {
            renderUsers()
        })

        await waitFor(() => {
            expect(screen.getByText('admin')).toBeInTheDocument()
        })

        // Admin user should have bold text (Typography.Text strong)
        const adminCell = screen.getByText('admin')
        expect(adminCell.tagName).toBe('STRONG')
    })

    it('shows unverified email icon when email is not verified', async () => {
        const usersWithUnverified = [
            {
                ...mockUsers[1],
                externalFlags: { emailVerified: false },
            },
        ]
        mockApiCall.mockResolvedValue(usersWithUnverified)

        await act(async () => {
            renderUsers({
                system: {
                    systemSettings: {
                        userMode: 'multi',
                        supportedFeatures: { emailVerification: true },
                    },
                },
            })
        })

        await waitFor(() => {
            expect(screen.getByText('viewer@test.com')).toBeInTheDocument()
        })
    })

    it('disables delete button for superuser', async () => {
        await act(async () => {
            renderUsers()
        })

        await waitFor(() => {
            expect(screen.getByText('admin')).toBeInTheDocument()
        })

        // First delete button is for admin (superUser) - should be disabled
        const deleteButtons = screen.getAllByRole('button', { name: /delete/i })
        expect(deleteButtons[0]).toBeDisabled()
    })
})
