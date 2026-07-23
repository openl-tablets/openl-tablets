import React from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'

const appState = vi.hoisted(() => ({
    showLogin: false,
    isLoggedIn: true,
    userProfile: {
        username: 'jane',
        firstName: '',
        lastName: '',
        displayName: '',
        email: '',
    },
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
}))

vi.mock('react-router-dom', () => ({
    RouterProvider: () => <div data-testid="router" />,
}))

vi.mock('./routes', () => ({ router: {} }))
vi.mock('./services', () => ({ CONFIG: { CONTEXT: '/webstudio' } }))
vi.mock('./legacy', () => ({}))
vi.mock('./App.styles.ts', () => ({ AppStyles: () => null }))
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
