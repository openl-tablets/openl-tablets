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

function App() {
    const { showLogin } = useAppStore()
    const { fetchUserInfo, isLoggedIn, userProfile } = useUserStore()
    const { initializeWebSocket, cleanupWebSocket } = useNotificationStore()

    const loginPage = `${CONFIG.CONTEXT}/login`
    const isLoginPage = location.pathname === loginPage

    useEffect(() => {
        // Set up global error handling
        setupGlobalErrorHandling()

        fetchUserInfo()
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

    if (showLogin && !isLoginPage) {
        if (location.pathname === `${CONFIG.CONTEXT}/`) {
            // navigate to the login page
            window.location.href = loginPage
            return
        }
        // do redirect through the server
        location.reload()
        return
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
