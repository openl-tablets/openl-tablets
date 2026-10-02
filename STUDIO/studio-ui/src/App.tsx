import { Suspense, useEffect } from 'react'
import { router } from './routes'
import { App as AntApp, Skeleton } from 'antd'
import { useAppStore, useNotificationStore, useUserStore } from 'store'
import { RouterProvider } from 'react-router-dom'
import { SecurityProvider } from './providers/SecurityProvider'
import { CONFIG } from './services'
import ErrorBoundary from './components/ErrorBoundary'
import { errorHandler, setupGlobalErrorHandling } from './utils/errorHandling'
import { AppStyles } from './App.styles.ts'
import { PopupsBridge } from './services/popups'
import { AppThemeProvider } from './providers/AppThemeProvider'
import { UserProfileCompletionModal } from './containers/users/UserProfileCompletionModal'
import { isUserProfileComplete } from './utils/userProfile'
import { readStored, removeStored, writeStored } from './utils/localStore'

/** Where the tab remembers the page it reloaded for the sign-in. */
const RELOADED_PAGE_KEY = 'openl.signIn.reloadedPage'

/**
 * Takes a signed-out user to the sign-in.
 *
 * The page is reloaded, so that the server answers its address with the sign-in it is configured for: the login
 * form or an identity provider (SAML, OAuth2). The server brings the user back to the page afterwards.
 *
 * The context root opens the login page at once. So does a page that comes back from that reload still signed out,
 * instead of reloading again: its server guards no page, as the Vite dev server does not. The login page replaces
 * the page in the history, as a redirect of the server does.
 *
 * The tab remembers the reloaded page until the login page is opened for it or the user signs in.
 */
const goToSignIn = (loginPage: string) => {
    const reloaded = readStored(RELOADED_PAGE_KEY, 'sessionStorage') === location.href
    if (reloaded || location.pathname === `${CONFIG.CONTEXT}/`) {
        removeStored(RELOADED_PAGE_KEY, 'sessionStorage')
        location.replace(loginPage)
    } else {
        writeStored(RELOADED_PAGE_KEY, location.href, 'sessionStorage')
        location.reload()
    }
}

function App() {
    const { showLogin } = useAppStore()
    const { fetchUserInfo, isLoggedIn, userProfile } = useUserStore()
    const { initializeWebSocket, cleanupWebSocket } = useNotificationStore()

    const loginPage = `${CONFIG.CONTEXT}/login`
    const leavesForSignIn = showLogin && location.pathname !== loginPage

    useEffect(() => {
        // Set up global error handling
        setupGlobalErrorHandling()

        void fetchUserInfo()
    }, [])

    useEffect(() => {
        if (isLoggedIn) {
            // Initialize WebSocket connection for real-time notifications
            initializeWebSocket()
        }

        return () => {
            // Clean up WebSocket connection when component unmounts or user logs out
            cleanupWebSocket()
        }
    }, [isLoggedIn, initializeWebSocket, cleanupWebSocket])

    // Not in the render: a page drawn again before it unloads would take its own reload for one that came back.
    useEffect(() => {
        if (leavesForSignIn) {
            goToSignIn(loginPage)
        } else if (isLoggedIn) {
            // Signed in through the server: a session lost later is reloaded through it again.
            removeStored(RELOADED_PAGE_KEY, 'sessionStorage')
        }
    }, [leavesForSignIn, isLoggedIn, loginPage])

    if (leavesForSignIn) {
        return null
    }

    // The surface is painted at once; the screens wait for the user profile behind it.
    return (
        <AppThemeProvider>
            <AppStyles />
            {(showLogin || isLoggedIn) && (
                <ErrorBoundary
                    onError={(error: Error, errorInfo: any) => {
                        errorHandler.logError(error, {
                            componentStack: errorInfo?.componentStack || undefined,
                            message: `App Level Error: ${error.message}`,
                        })
                    }}
                >
                    <Suspense fallback={<Skeleton active style={{ padding: 24 }} />}>
                        <AntApp>
                            <PopupsBridge />
                            <SecurityProvider>
                                <RouterProvider router={router} />
                            </SecurityProvider>
                            {userProfile && !isUserProfileComplete(userProfile) && (
                                <UserProfileCompletionModal
                                    open
                                    required
                                    onSave={fetchUserInfo}
                                    profile={userProfile}
                                />
                            )}
                        </AntApp>
                    </Suspense>
                </ErrorBoundary>
            )}
        </AppThemeProvider>
    )
}

export default App
