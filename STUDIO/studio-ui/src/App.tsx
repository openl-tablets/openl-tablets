import React, { Suspense, useEffect } from 'react'
import { router } from './routes'
import { App as AntApp } from 'antd'
import { useAppStore, useNotificationStore, useUserStore } from 'store'
import { RouterProvider } from 'react-router-dom'
import { SecurityProvider } from './providers/SecurityProvider'
import { CONFIG } from './services'

function App() {
    const { showLogin } = useAppStore()
    const { fetchUserInfo, isLoggedIn } = useUserStore()
    const { fetchNotification } = useNotificationStore()

    const loginPage = `${CONFIG.CONTEXT}/login`
    const isLoginPage = window.location.pathname === loginPage

    useEffect(() => {
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
        window.location.href = loginPage + `?from=${encodeURIComponent(window.location.href)}`
    }

    return(
        <Suspense fallback={<div>Loading...</div>}>
            <AntApp>
                <SecurityProvider>
                    <RouterProvider router={router} />
                </SecurityProvider>
            </AntApp>
        </Suspense>
    )
}

export default App
