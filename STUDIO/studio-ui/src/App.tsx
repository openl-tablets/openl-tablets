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

    if (!showLogin && !isLoggedIn) {
        return null
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
