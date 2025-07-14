import React, { Suspense, useEffect } from 'react'
import { router } from './routes'
import { App as AntApp } from 'antd'
import { useUserStore, useNotificationStore, useAppStore } from 'store'
import { RouterProvider } from 'react-router-dom'
import { SecurityProvider } from './providers/SecurityProvider'
import { CONFIG } from './services'

function App() {
    const { showLogin } = useAppStore()
    const { fetchUserInfo, isLoggedIn } = useUserStore()
    const { fetchNotification } = useNotificationStore()

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

    if (showLogin) {
        window.location.href = CONFIG.LOGIN_URL + `?redirect=${encodeURIComponent(window.location.href)}`
    }

    return isLoggedIn
        ? (
            <Suspense fallback={<div>Loading...</div>}>
                <AntApp>
                    <SecurityProvider>
                        <RouterProvider router={router} />
                    </SecurityProvider>
                </AntApp>
            </Suspense>
        )
        : null
}

export default App
