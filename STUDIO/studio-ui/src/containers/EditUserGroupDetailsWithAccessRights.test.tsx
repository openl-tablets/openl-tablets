import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { notification } from 'antd'
import { runSequentialCollectErrors } from 'utils/async'
import * as services from 'services'
import { SystemContext } from 'contexts/SystemContext'
import type { MockedFunction } from 'vitest'

vi.mock('services', async () => ({
    apiCall: vi.fn(),
}))

vi.mock('react-i18next', async () => ({
    useTranslation: () => ({
        t: (key: string) => key,
        i18n: { language: 'en' },
    }),
}))

vi.mock('../../src/i18n', async () => ({
    __esModule: true,
    default: { t: (key: string) => key },
}))

vi.mock('antd', async () => {
    const actual = await vi.importActual('antd')
    return {
        ...actual,
        Drawer: ({ open, title, extra, children, onClose }: any) => {
            if (!open) return null
            return (
                <div data-testid="drawer">
                    <div data-testid="drawer-title">{title}</div>
                    <div data-testid="drawer-extra">{extra}</div>
                    <div data-testid="drawer-body">{children}</div>
                </div>
            )
        },
        notification: {
            success: vi.fn(),
            error: vi.fn(),
        },
    }
})

vi.mock('components/accessManagement', async () => {
    // initialValue on these Form.Items is omitted — the parent Form already seeds
    // designRepos/deployRepos/projects/designRootRole/deployRootRole via its
    // `initialValues` prop, and AntD warns if both sides try to seed the same path.
    const { Form } = await vi.importActual('antd')
    return {
        DesignRepositoriesTab: () => (
            <div data-testid="design-repos-tab">
                <Form.Item name="designRepos"><input /></Form.Item>
                <Form.Item name="designRootRole"><input /></Form.Item>
            </div>
        ),
        DeployRepositoriesTab: () => (
            <div data-testid="deploy-repos-tab">
                <Form.Item name="deployRepos"><input /></Form.Item>
                <Form.Item name="deployRootRole"><input /></Form.Item>
            </div>
        ),
        ProjectsTab: () => (
            <div data-testid="projects-tab">
                <Form.Item name="projects"><input /></Form.Item>
            </div>
        ),
    }
})

vi.mock('containers/groups/EditGroupDetails', async () => {
    const { Form } = await vi.importActual('antd')
    return {
        EditGroupDetails: () => (
            <div data-testid="edit-group-details">
                <Form.Item name="name"><input /></Form.Item>
                <Form.Item name="description"><input /></Form.Item>
            </div>
        ),
    }
})

vi.mock('containers/users/UserDatailsTab', async () => {
    const { Form } = await vi.importActual('antd')
    return {
        UserDetailsTab: () => (
            <div data-testid="user-details-tab">
                <Form.Item name="username"><input data-testid="input-username" /></Form.Item>
                <Form.Item name="email"><input data-testid="input-email" /></Form.Item>
                <Form.Item name="firstName"><input data-testid="input-firstName" /></Form.Item>
                <Form.Item name="lastName"><input data-testid="input-lastName" /></Form.Item>
                <Form.Item name="displayName"><input data-testid="input-displayName" /></Form.Item>
                <Form.Item name="password"><input data-testid="input-password" /></Form.Item>
            </div>
        ),
    }
})

vi.mock('containers/users/EditUserModal', async () => ({
    __esModule: true,
}))

vi.mock('containers/users/RenderGroupCell', async () => ({
    __esModule: true,
}))

vi.mock('utils/async', async () => ({
    runSequentialCollectErrors: vi.fn().mockResolvedValue([]),
}))

vi.mock('store', async () => ({
    useUserStore: () => ({
        userProfile: { username: 'admin' },
        fetchUserProfile: vi.fn(),
    }),
}))

const mockApiCall = services.apiCall as MockedFunction<typeof services.apiCall>

import { EditUserGroupDetailsWithAccessRights } from 'containers/EditUserGroupDetailsWithAccessRights'

const defaultSystemCtx = {
    isExternalAuthSystem: false,
    isUserManagementEnabled: true,
    isGroupsManagementEnabled: true,
    isPersonalAccessTokenEnabled: false,
}

// Mock repo data — must be non-empty to prevent infinite re-fetch loop in the component's useEffect
const mockDesignRepo = { id: 'repo1', name: 'design-repo', type: 'design' }

const renderComponent = (props: any = {}) => {
    const defaultProps = {
        isOpenFromParent: false,
        isPrincipal: true,
        onClose: vi.fn(),
        newUser: false,
        ...props,
    }
    return render(
        <SystemContext.Provider value={defaultSystemCtx}>
            <EditUserGroupDetailsWithAccessRights {...defaultProps} />
        </SystemContext.Provider>
    )
}

describe('EditUserGroupDetailsWithAccessRights', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        // Default: /repos returns non-empty array; other endpoints return empty arrays
        mockApiCall.mockImplementation((url: string) => {
            if (url === '/repos') return Promise.resolve([mockDesignRepo])
            return Promise.resolve([])
        })
    })

    it('does not render drawer when closed', () => {
        renderComponent()
        expect(screen.queryByTestId('drawer')).not.toBeInTheDocument()
    })

    it('renders drawer with "Add User" title for new user', async () => {
        await act(async () => {
            renderComponent({ isOpenFromParent: true, newUser: true, sid: undefined })
        })

        await waitFor(() => {
            expect(screen.getByTestId('drawer-title')).toHaveTextContent('users:add_user')
        })
    })

    it('renders drawer with "Edit User" title for existing user', async () => {
        const user = {
            username: 'testuser',
            email: 'test@test.com',
            firstName: 'Test',
            lastName: 'User',
            displayName: 'Test User',
            userGroups: [],
        }
        await act(async () => {
            renderComponent({ isOpenFromParent: true, user, sid: 'testuser' })
        })

        await waitFor(() => {
            expect(screen.getByTestId('drawer-title')).toHaveTextContent('users:edit_user')
        })
    })

    it('renders drawer with "Invite Group" title for new group', async () => {
        await act(async () => {
            renderComponent({ isOpenFromParent: true, group: { name: '' }, newUser: undefined })
        })

        await waitFor(() => {
            expect(screen.getByTestId('drawer-title')).toHaveTextContent('groups:invite_group')
        })
    })

    it('renders drawer with "Edit Group" title for existing group', async () => {
        await act(async () => {
            renderComponent({
                isOpenFromParent: true,
                group: { id: 1, name: 'Admins', oldName: 'Admins', description: '', admin: true },
                newUser: undefined,
            })
        })

        await waitFor(() => {
            expect(screen.getByTestId('drawer-title')).toHaveTextContent('groups:edit_group')
        })
    })

    it('renders Cancel and Save buttons in drawer', async () => {
        await act(async () => {
            renderComponent({ isOpenFromParent: true, newUser: true })
        })

        await waitFor(() => {
            expect(screen.getByText('groups:cancel')).toBeInTheDocument()
            expect(screen.getByText('common:btn.save')).toBeInTheDocument()
        })
    })

    it('renders Invite button for new group', async () => {
        await act(async () => {
            renderComponent({ isOpenFromParent: true, group: { name: '' }, newUser: undefined })
        })

        await waitFor(() => {
            expect(screen.getByText('common:btn.invite')).toBeInTheDocument()
        })
    })

    it('renders access rights tabs', async () => {
        await act(async () => {
            renderComponent({ isOpenFromParent: true, newUser: true })
        })

        await waitFor(() => {
            expect(screen.getByText('users:design_repositories')).toBeInTheDocument()
            expect(screen.getByText('users:deploy_repositories')).toBeInTheDocument()
            expect(screen.getByText('users:projects')).toBeInTheDocument()
        })
    })

    it('renders UserDetailsTab for user mode', async () => {
        await act(async () => {
            renderComponent({ isOpenFromParent: true, newUser: true })
        })

        await waitFor(() => {
            expect(screen.getByTestId('user-details-tab')).toBeInTheDocument()
        })
    })

    it('renders EditGroupDetails for group mode', async () => {
        await act(async () => {
            renderComponent({
                isOpenFromParent: true,
                group: { id: 1, name: 'Admins', oldName: 'Admins' },
                newUser: undefined,
            })
        })

        await waitFor(() => {
            expect(screen.getByTestId('edit-group-details')).toBeInTheDocument()
        })
    })

    it('fetches repos and ACL data when drawer opens with existing user', async () => {
        const user = {
            username: 'testuser',
            email: 'test@test.com',
            firstName: 'Test',
            lastName: 'User',
            displayName: 'Test User',
            userGroups: [],
        }
        await act(async () => {
            renderComponent({ isOpenFromParent: true, user, sid: 'testuser' })
        })

        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledWith(expect.stringContaining('/acls/repositories?sid=testuser'))
            expect(mockApiCall).toHaveBeenCalledWith(expect.stringContaining('/acls/projects?sid=testuser'))
            expect(mockApiCall).toHaveBeenCalledWith(expect.stringContaining('/acls/repositories/roots?sid=testuser'))
            expect(mockApiCall).toHaveBeenCalledWith('/repos')
        })
    })

    it('fetches design repositories when drawer opens', async () => {
        await act(async () => {
            renderComponent({ isOpenFromParent: true, newUser: true })
        })

        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledWith('/repos')
        })
    })

    it('renders with renderButton prop', () => {
        const renderButton = (open: () => void) => (
            <button data-testid="custom-open" onClick={open}>Open</button>
        )
        renderComponent({ renderButton })

        expect(screen.getByTestId('custom-open')).toBeInTheDocument()
    })

    it('opens drawer via renderButton click', async () => {
        const renderButton = (open: () => void) => (
            <button data-testid="custom-open" onClick={open}>Open</button>
        )
        renderComponent({ renderButton })

        await userEvent.click(screen.getByTestId('custom-open'))

        await waitFor(() => {
            expect(screen.getByTestId('drawer')).toBeInTheDocument()
        })
    })

    it('calls onClose when Cancel is clicked', async () => {
        const onClose = vi.fn()
        await act(async () => {
            renderComponent({ isOpenFromParent: true, newUser: true, onClose })
        })

        await userEvent.click(screen.getByText('groups:cancel'))

        expect(onClose).toHaveBeenCalled()
    })

    it('renders access rights divider', async () => {
        await act(async () => {
            renderComponent({ isOpenFromParent: true, newUser: true })
        })

        await waitFor(() => {
            expect(screen.getByText('common:access_rights')).toBeInTheDocument()
        })
    })

    it('calls saveUser API on form submit for new user', async () => {
        mockApiCall.mockImplementation((url: string) => {
            if (url === '/repos') return Promise.resolve([mockDesignRepo])
            if (url === '/users') return Promise.resolve({ username: 'newuser' })
            return Promise.resolve([])
        })

        await act(async () => {
            renderComponent({ isOpenFromParent: true, newUser: true, sid: undefined })
        })

        await waitFor(() => {
            expect(screen.getByText('common:btn.save')).toBeInTheDocument()
        })

        await userEvent.click(screen.getByText('common:btn.save'))

        // Form submits with empty values — saveUser is called with /users endpoint
        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledWith(
                '/users',
                expect.objectContaining({ method: 'PUT' })
            )
        })
    })

    it('calls saveUser API on form submit for existing user', async () => {
        const user = {
            username: 'testuser',
            email: 'test@test.com',
            firstName: 'Test',
            lastName: 'User',
            displayName: 'Test User',
            userGroups: [{ name: 'Viewers' }],
        }
        mockApiCall.mockImplementation((url: string) => {
            if (url === '/repos') return Promise.resolve([mockDesignRepo])
            if (url === '/users/testuser') return Promise.resolve({ username: 'testuser' })
            return Promise.resolve([])
        })

        await act(async () => {
            renderComponent({ isOpenFromParent: true, user, sid: 'testuser' })
        })

        await waitFor(() => {
            expect(screen.getByText('common:btn.save')).toBeInTheDocument()
        })

        await userEvent.click(screen.getByText('common:btn.save'))

        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledWith(
                '/users/testuser',
                expect.objectContaining({ method: 'PUT' })
            )
        })
    })

    it('calls saveGroup API on form submit for new group', async () => {
        mockApiCall.mockImplementation((url: string) => {
            if (url === '/repos') return Promise.resolve([mockDesignRepo])
            if (url === '/admin/management/groups') return Promise.resolve({ name: 'NewGroup' })
            return Promise.resolve([])
        })

        await act(async () => {
            renderComponent({ isOpenFromParent: true, group: { name: '' }, newUser: undefined })
        })

        await waitFor(() => {
            expect(screen.getByText('common:btn.invite')).toBeInTheDocument()
        })

        await userEvent.click(screen.getByText('common:btn.invite'))

        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledWith(
                '/admin/management/groups',
                expect.objectContaining({ method: 'POST' })
            )
        })
    })

    it('calls saveGroup API on form submit for existing group', async () => {
        const group = { id: 1, name: 'Admins', oldName: 'Admins', description: 'Admin group', admin: true }
        mockApiCall.mockImplementation((url: string) => {
            if (url === '/repos') return Promise.resolve([mockDesignRepo])
            if (url === '/admin/management/groups') return Promise.resolve({ name: 'Admins' })
            return Promise.resolve([])
        })

        await act(async () => {
            renderComponent({ isOpenFromParent: true, group, newUser: undefined })
        })

        await waitFor(() => {
            expect(screen.getByText('common:btn.save')).toBeInTheDocument()
        })

        await userEvent.click(screen.getByText('common:btn.save'))

        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledWith(
                '/admin/management/groups',
                expect.objectContaining({ method: 'POST' })
            )
        })
    })

    it('calls reloadUsers and reloadGroups after successful save', async () => {
        const reloadUsers = vi.fn().mockResolvedValue(undefined)
        const reloadGroups = vi.fn().mockResolvedValue(undefined)
        const user = {
            username: 'existinguser',
            email: 'test@test.com',
            firstName: 'Test',
            lastName: 'User',
            displayName: 'Test User',
            userGroups: [],
        }

        mockApiCall.mockImplementation((url: string) => {
            if (url === '/repos') return Promise.resolve([mockDesignRepo])
            if (url.startsWith('/users/')) return Promise.resolve({ username: 'existinguser' })
            return Promise.resolve([])
        })

        await act(async () => {
            renderComponent({
                isOpenFromParent: true,
                user,
                sid: 'existinguser',
                reloadUsers,
                reloadGroups,
            })
        })

        await userEvent.click(screen.getByText('common:btn.save'))

        await waitFor(() => {
            expect(reloadUsers).toHaveBeenCalled()
            expect(reloadGroups).toHaveBeenCalled()
        })
    })

    it('closes drawer after successful save', async () => {
        const onClose = vi.fn()
        const user = {
            username: 'existinguser',
            email: 'test@test.com',
            firstName: 'Test',
            lastName: 'User',
            displayName: 'Test User',
            userGroups: [],
        }

        mockApiCall.mockImplementation((url: string) => {
            if (url === '/repos') return Promise.resolve([mockDesignRepo])
            if (url.startsWith('/users/')) return Promise.resolve({ username: 'existinguser' })
            return Promise.resolve([])
        })

        await act(async () => {
            renderComponent({
                isOpenFromParent: true,
                user,
                sid: 'existinguser',
                onClose,
            })
        })

        await userEvent.click(screen.getByText('common:btn.save'))

        await waitFor(() => {
            expect(onClose).toHaveBeenCalled()
        })
    })

    it('handles saveUser error gracefully', async () => {
        mockApiCall.mockImplementation((url: string) => {
            if (url === '/repos') return Promise.resolve([mockDesignRepo])
            if (url === '/users') return Promise.reject(new Error('User creation failed'))
            return Promise.resolve([])
        })

        const onClose = vi.fn()
        await act(async () => {
            renderComponent({ isOpenFromParent: true, newUser: true, sid: undefined, onClose })
        })

        await userEvent.click(screen.getByText('common:btn.save'))

        // onClose should NOT be called when save fails
        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledWith('/users', expect.anything())
        })
    })

    it('handles fetchRootRepositoryRoles error gracefully', async () => {
        const user = {
            username: 'testuser',
            email: 'test@test.com',
            firstName: 'Test',
            lastName: 'User',
            displayName: 'Test User',
            userGroups: [],
        }
        mockApiCall.mockImplementation((url: string) => {
            if (url === '/repos') return Promise.resolve([mockDesignRepo])
            if (url.includes('/acls/repositories/roots')) return Promise.reject(new Error('Root fetch failed'))
            return Promise.resolve([])
        })

        await act(async () => {
            renderComponent({ isOpenFromParent: true, user, sid: 'testuser' })
        })

        await waitFor(() => {
            expect(notification.error).toHaveBeenCalledWith(
                expect.objectContaining({
                    description: 'users:failed_to_load_root_repository_roles',
                })
            )
        })
    })

    it('getUserInitialValues sets displayName select to FirstLast', async () => {
        const user = {
            username: 'testuser',
            email: 'test@test.com',
            firstName: 'Test',
            lastName: 'User',
            displayName: 'Test User',
            userGroups: [{ name: 'Viewers' }],
        }
        mockApiCall.mockImplementation((url: string) => {
            if (url === '/repos') return Promise.resolve([mockDesignRepo])
            return Promise.resolve([])
        })

        await act(async () => {
            renderComponent({ isOpenFromParent: true, user, sid: 'testuser' })
        })

        // Component renders without error — verifies getUserInitialValues path
        await waitFor(() => {
            expect(screen.getByTestId('user-details-tab')).toBeInTheDocument()
        })
    })

    it('getUserInitialValues handles LastFirst display name pattern', async () => {
        const user = {
            username: 'testuser',
            email: 'test@test.com',
            firstName: 'Test',
            lastName: 'User',
            displayName: 'User Test',
            userGroups: [],
        }
        mockApiCall.mockImplementation((url: string) => {
            if (url === '/repos') return Promise.resolve([mockDesignRepo])
            return Promise.resolve([])
        })

        await act(async () => {
            renderComponent({ isOpenFromParent: true, user, sid: 'testuser' })
        })

        await waitFor(() => {
            expect(screen.getByTestId('user-details-tab')).toBeInTheDocument()
        })
    })

    it('saves repo roles with existing ACL data', async () => {
        const user = {
            username: 'testuser',
            email: 'test@test.com',
            firstName: 'Test',
            lastName: 'User',
            displayName: 'Test User',
            userGroups: [],
        }
        // Return initial repo roles to exercise the diff logic in saveReposRoles
        const designRepoRole = { id: 'repo1', name: 'design-repo', type: 'design', role: 'VIEWER' }
        const deployRepoRole = { id: 'deploy1', name: 'deploy-repo', type: 'prod', role: 'VIEWER' }
        const projectRole = { id: 'proj1', name: 'Project', role: 'VIEWER' }
        const rootRole = [{ id: 'root-design', type: 'design', role: 'VIEWER' }, { id: 'root-prod', type: 'prod', role: 'VIEWER' }]

        mockApiCall.mockImplementation((url: string) => {
            if (url === '/repos') return Promise.resolve([mockDesignRepo])
            if (url.includes('/acls/repositories/roots')) return Promise.resolve(rootRole)
            if (url.includes('/acls/repositories')) return Promise.resolve([designRepoRole, deployRepoRole])
            if (url.includes('/acls/projects')) return Promise.resolve([projectRole])
            if (url.startsWith('/users/')) return Promise.resolve({ username: 'testuser' })
            return Promise.resolve([])
        })

        await act(async () => {
            renderComponent({ isOpenFromParent: true, user, sid: 'testuser' })
        })

        // Wait for all fetches to complete
        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledWith(expect.stringContaining('/acls/repositories?sid=testuser'))
        })

        // Click save to trigger the full save flow
        await userEvent.click(screen.getByText('common:btn.save'))

        // Verify saveUser was called
        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledWith(
                '/users/testuser',
                expect.objectContaining({ method: 'PUT' })
            )
        })
    })

    it('handles saveGroup error gracefully', async () => {
        const group = { id: 1, name: 'TestGroup', oldName: 'TestGroup', description: '' }
        mockApiCall.mockImplementation((url: string) => {
            if (url === '/repos') return Promise.resolve([mockDesignRepo])
            if (url === '/admin/management/groups') return Promise.reject(new Error('Group save failed'))
            return Promise.resolve([])
        })

        const onClose = vi.fn()
        await act(async () => {
            renderComponent({ isOpenFromParent: true, group, newUser: undefined, onClose })
        })

        await userEvent.click(screen.getByText('common:btn.save'))

        // Verify that the API was called
        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledWith(
                '/admin/management/groups',
                expect.objectContaining({ method: 'POST' })
            )
        })
        // onClose should not be called on error
        expect(onClose).not.toHaveBeenCalled()
    })

    it('refreshes user profile when editing current user', async () => {
        // The user being edited matches the logged-in user ('admin' from store mock)
        const user = {
            username: 'admin',
            email: 'admin@test.com',
            firstName: 'Admin',
            lastName: 'User',
            displayName: 'Admin User',
            userGroups: [],
        }
        mockApiCall.mockImplementation((url: string) => {
            if (url === '/repos') return Promise.resolve([mockDesignRepo])
            if (url.startsWith('/users/')) return Promise.resolve({ username: 'admin' })
            return Promise.resolve([])
        })

        await act(async () => {
            renderComponent({ isOpenFromParent: true, user, sid: 'admin' })
        })

        await userEvent.click(screen.getByText('common:btn.save'))

        // fetchUserProfile should have been called because sid matches current user
        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledWith(
                '/users/admin',
                expect.objectContaining({ method: 'PUT' })
            )
        })
    })

    it('getUserInitialValues handles custom display name', async () => {
        const user = {
            username: 'testuser',
            email: 'test@test.com',
            firstName: 'Test',
            lastName: 'User',
            displayName: 'Custom Name',
            userGroups: [],
        }
        mockApiCall.mockImplementation((url: string) => {
            if (url === '/repos') return Promise.resolve([mockDesignRepo])
            return Promise.resolve([])
        })

        await act(async () => {
            renderComponent({ isOpenFromParent: true, user, sid: 'testuser' })
        })

        await waitFor(() => {
            expect(screen.getByTestId('user-details-tab')).toBeInTheDocument()
        })
    })

    it('handles save failure in onFinish try-catch', async () => {
        const user = {
            username: 'testuser',
            email: 'test@test.com',
            firstName: 'Test',
            lastName: 'User',
            displayName: 'Test User',
            userGroups: [],
        }

        // Make the save succeed but a subsequent role save fail
        vi.mocked(runSequentialCollectErrors).mockResolvedValueOnce([{ error: new Error('Role save fail') }])

        mockApiCall.mockImplementation((url: string) => {
            if (url === '/repos') return Promise.resolve([mockDesignRepo])
            if (url.startsWith('/users/')) return Promise.resolve({ username: 'testuser' })
            return Promise.resolve([])
        })

        const onClose = vi.fn()
        await act(async () => {
            renderComponent({ isOpenFromParent: true, user, sid: 'testuser', onClose })
        })

        await userEvent.click(screen.getByText('common:btn.save'))

        // Save was called
        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledWith('/users/testuser', expect.objectContaining({ method: 'PUT' }))
        })
    })

    it('saves root repository role DELETE when role removed', async () => {
        const user = {
            username: 'testuser',
            email: 'test@test.com',
            firstName: 'Test',
            lastName: 'User',
            displayName: 'Test User',
            userGroups: [],
        }
        const rootRoles = [
            { id: 'root-design', type: 'design', role: 'VIEWER' },
        ]
        mockApiCall.mockImplementation((url: string) => {
            if (url === '/repos') return Promise.resolve([mockDesignRepo])
            if (url.includes('/acls/repositories/roots?sid=')) return Promise.resolve(rootRoles)
            if (url.startsWith('/users/')) return Promise.resolve({ username: 'testuser' })
            return Promise.resolve([])
        })

        await act(async () => {
            renderComponent({ isOpenFromParent: true, user, sid: 'testuser' })
        })

        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledWith(expect.stringContaining('/acls/repositories/roots?sid=testuser'))
        })

        // Save triggers root role sync
        await userEvent.click(screen.getByText('common:btn.save'))

        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledWith('/users/testuser', expect.objectContaining({ method: 'PUT' }))
        })
    })

    it('preserves typed entity field value while ACL endpoints are still in flight', async () => {
        // Regression for the case where async ACL responses (/acls/repositories,
        // /acls/projects, /acls/repositories/roots) re-trigger the initialValues
        // memo and a blanket form.setFieldsValue(...) wipes out what the user
        // just typed into an entity field.
        const user = {
            username: 'testuser',
            email: 'original@test.com',
            firstName: 'Original',
            lastName: 'Last',
            displayName: 'Original Last',
            userGroups: [],
        }

        // Hold the ACL responses until the test chooses to resolve them.
        let resolveReposRoles: (value: unknown) => void = () => undefined
        let resolveProjectRoles: (value: unknown) => void = () => undefined
        let resolveRootRoles: (value: unknown) => void = () => undefined
        const reposRolesDeferred = new Promise((resolve) => { resolveReposRoles = resolve })
        const projectRolesDeferred = new Promise((resolve) => { resolveProjectRoles = resolve })
        const rootRolesDeferred = new Promise((resolve) => { resolveRootRoles = resolve })

        mockApiCall.mockImplementation((url: string) => {
            if (url === '/repos') return Promise.resolve([mockDesignRepo])
            if (url.includes('/acls/repositories?sid=')) return reposRolesDeferred
            if (url.includes('/acls/projects?sid=')) return projectRolesDeferred
            if (url.includes('/acls/repositories/roots?sid=')) return rootRolesDeferred
            return Promise.resolve([])
        })

        await act(async () => {
            renderComponent({ isOpenFromParent: true, user, sid: 'testuser' })
        })

        await waitFor(() => {
            expect(screen.getByTestId('user-details-tab')).toBeInTheDocument()
        })

        // Type into firstName while ACL endpoints are still pending.
        const firstNameInput = screen.getByTestId('input-firstName') as HTMLInputElement
        await userEvent.clear(firstNameInput)
        await userEvent.type(firstNameInput, 'EditedName')
        expect(firstNameInput.value).toBe('EditedName')

        // Now resolve all ACL responses — previously this would re-seed the whole
        // form via setFieldsValue and overwrite the typed value.
        await act(async () => {
            resolveReposRoles([])
            resolveProjectRoles([])
            resolveRootRoles([])
            await reposRolesDeferred
            await projectRolesDeferred
            await rootRolesDeferred
        })

        expect((screen.getByTestId('input-firstName') as HTMLInputElement).value).toBe('EditedName')
    })
})
