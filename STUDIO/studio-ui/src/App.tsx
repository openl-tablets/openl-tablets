import React, { Suspense, useEffect } from 'react'
import { router } from './routes'
import { App as AntApp } from 'antd'
import { useUserStore, useNotificationStore } from 'store'
import { RouterProvider } from 'react-router-dom'
import { SecurityProvider } from './providers/SecurityProvider'

function App() {
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
