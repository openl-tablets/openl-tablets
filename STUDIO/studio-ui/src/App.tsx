import React, { Suspense, useEffect } from 'react'
import { router } from './routes'
import { App as AntApp } from 'antd'
import { useAppStore, useNotificationStore, useUserStore } from 'store'
import { RouterProvider } from 'react-router-dom'
import { SecurityProvider } from './providers/SecurityProvider'
import { CONFIG } from './services'
import ErrorBoundary from './components/ErrorBoundary'
import { errorHandler, setupGlobalErrorHandling } from './utils/errorHandling'

function App() {
    const { showLogin } = useAppStore()
    const { fetchUserInfo, isLoggedIn } = useUserStore()
    const { fetchNotification } = useNotificationStore()

    const loginPage = `${CONFIG.CONTEXT}/login`
    const isLoginPage = location.pathname === loginPage

    useEffect(() => {
        // Set up global error handling
        setupGlobalErrorHandling()

        fetchUserInfo()
    }, [])

    useEffect(() => {
        fetchNotification()
        const intervalCall = setInterval(() => {
            fetchNotification()
        }, 30000)
        return () => {
            // clean up
            clearInterval(intervalCall)
        }
    }, [fetchNotification])

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

    return(
        <ErrorBoundary
                onError={(error: Error, errorInfo: any) => {
                    errorHandler.logError(error, {
                        componentStack: errorInfo?.componentStack || undefined,
                        message: `App Level Error: ${error.message}`,
                    })
                }}
            >
                <Suspense fallback={<div>Loading...</div>}>
            <AntApp>
                <SecurityProvider>
                    <RouterProvider router={router} />
                </SecurityProvider>
            </AntApp>
        </Suspense></ErrorBoundary>
    )
}

export default App
