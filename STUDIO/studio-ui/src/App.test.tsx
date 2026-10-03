import React from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'

interface Profile {
    username: string
    firstName: string
    lastName: string
    displayName: string
    email: string
}

const appState = vi.hoisted(() => ({
    showLogin: false,
    isLoggedIn: true,
    userProfile: undefined as Profile | undefined,
    fetchUserInfo: vi.fn(),
    initializeWebSocket: vi.fn(),
    cleanupWebSocket: vi.fn(),
}))

vi.mock('store', () => ({
    useAppStore: () => ({ showLogin: appState.showLogin }),
    useUserStore: () => ({
        fetchUserInfo: appState.fetchUserInfo,
        isLoggedIn: appState.isLoggedIn,
        userProfile: appState.userProfile,
    }),
    useNotificationStore: () => ({
        initializeWebSocket: appState.initializeWebSocket,
        cleanupWebSocket: appState.cleanupWebSocket,
    }),
}))

vi.mock('antd', () => ({
    App: ({ children }: { children: React.ReactNode }) => <>{children}</>,
    Skeleton: () => null,
}))

vi.mock('react-router/dom', () => ({
    RouterProvider: () => <div data-testid="router" />,
}))

vi.mock('./routes', () => ({ router: {} }))
vi.mock('./services', () => ({ CONFIG: { CONTEXT: '/webstudio' } }))
vi.mock('./legacy', () => ({}))
vi.mock('./App.styles.ts', () => ({ AppStyles: () => null }))
vi.mock('./services/popups', () => ({ PopupsBridge: () => null }))
vi.mock('./providers/SecurityProvider', () => ({
    SecurityProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}))
vi.mock('./components/ErrorBoundary', () => ({
    default: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}))
vi.mock('./utils/errorHandling', () => ({
    errorHandler: { logError: vi.fn() },
    setupGlobalErrorHandling: vi.fn(),
}))
vi.mock('./containers/users/UserProfileCompletionModal', () => ({
    UserProfileCompletionModal: ({
        required,
        onSave,
    }: {
        required?: boolean
        onSave: () => void
    }) => (
        <div data-required={required} data-testid="profile-completion-modal">
            <button onClick={onSave} type="button">Save profile</button>
        </div>
    ),
}))

describe('App profile completion', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        appState.showLogin = false
        appState.isLoggedIn = true
        appState.userProfile = {
            username: 'jane',
            firstName: '',
            lastName: '',
            displayName: '',
            email: '',
        }
    })

    it('requires missing profile details immediately after authentication', async () => {
        render(<App />)

        const modal = screen.getByTestId('profile-completion-modal')
        expect(modal).toHaveAttribute('data-required', 'true')
        expect(screen.getByTestId('router')).toBeInTheDocument()
        expect(appState.fetchUserInfo).toHaveBeenCalledTimes(1)

        await userEvent.click(screen.getByRole('button', { name: 'Save profile' }))

        expect(appState.fetchUserInfo).toHaveBeenCalledTimes(2)
    })

    it('does not prompt after a complete profile is loaded', () => {
        appState.userProfile = {
            username: 'jane',
            firstName: '',
            lastName: '',
            displayName: 'Jane Doe',
            email: 'jane@example.com',
        }

        render(<App />)

        expect(screen.queryByTestId('profile-completion-modal')).not.toBeInTheDocument()
    })
})

describe('App sign-in', () => {
    const reload = vi.fn()
    const replace = vi.fn()

    /** Opens the application at a path, as the address bar shows it. */
    const openAt = (path: string) => {
        const url = new URL(path, 'http://localhost:3100')
        vi.stubGlobal('location', { href: url.href, pathname: url.pathname, reload, replace })
    }

    beforeEach(() => {
        sessionStorage.clear()
        appState.showLogin = true
        appState.isLoggedIn = false
        appState.userProfile = undefined
    })

    it('reloads a signed-out page, so that the server answers it with its sign-in', () => {
        openAt('/webstudio/projects')

        render(<App />)

        expect(reload).toHaveBeenCalledTimes(1)
        expect(replace).not.toHaveBeenCalled()
        expect(screen.queryByTestId('router')).not.toBeInTheDocument()
    })

    it('opens the login page when the reload brings the page back signed out', () => {
        openAt('/webstudio/projects')
        render(<App />).unmount()

        render(<App />)

        expect(reload).toHaveBeenCalledTimes(1)
        expect(replace).toHaveBeenCalledExactlyOnceWith('/webstudio/login')
        expect(sessionStorage).toHaveLength(0)
    })

    it('reloads a page once however often it is drawn before it unloads', () => {
        openAt('/webstudio/projects')
        const { rerender } = render(<App />)

        rerender(<App />)

        expect(reload).toHaveBeenCalledTimes(1)
        expect(replace).not.toHaveBeenCalled()
    })

    it('reloads a page that another page was reloaded before', () => {
        openAt('/webstudio/projects')
        render(<App />).unmount()
        openAt('/webstudio/deployments')

        render(<App />)

        expect(reload).toHaveBeenCalledTimes(2)
        expect(replace).not.toHaveBeenCalled()
    })

    it('reloads through the server again a page that lost the session it signed in with', () => {
        openAt('/webstudio/projects')
        render(<App />).unmount()
        appState.showLogin = false
        appState.isLoggedIn = true
        const { rerender } = render(<App />)

        appState.showLogin = true
        rerender(<App />)

        expect(reload).toHaveBeenCalledTimes(2)
        expect(replace).not.toHaveBeenCalled()
    })

    it('reloads the context root through the server like any other page', () => {
        openAt('/webstudio/')
        render(<App />).unmount()

        render(<App />)

        expect(reload).toHaveBeenCalledTimes(1)
        expect(replace).toHaveBeenCalledExactlyOnceWith('/webstudio/login')
    })

    it('draws the login page for a signed-out user who is on it', () => {
        openAt('/webstudio/login')

        render(<App />)

        expect(screen.getByTestId('router')).toBeInTheDocument()
        expect(reload).not.toHaveBeenCalled()
        expect(replace).not.toHaveBeenCalled()
    })
})
