import React, { Suspense, useEffect } from 'react'
import { router } from './routes'
import { App as AntApp } from 'antd'
import { fetchUserDetails, fetchUserProfile} from './containers/user/userSlice'
import { RootState, useAppDispatch, useAppSelector } from 'store'
import { fetchNotification } from './containers/notification/notificationSlice'
import { RouterProvider } from 'react-router-dom'
import { SecurityProvider } from './providers/SecurityProvider'

function App() {
    const dispatch = useAppDispatch()

    const userStatus = useAppSelector((state: RootState) => state.user.status)
    const isUserLoggedIn = useAppSelector((state: RootState) => state.user.isLoggedIn)
    const username = useAppSelector((state: RootState) => state.user.profile.username)

    useEffect(() => {
        dispatch(fetchNotification())
        const intervalCall = setInterval(() => {
            dispatch(fetchNotification())
        }, 30000)
        return () => {
            // clean up
            clearInterval(intervalCall)
        }
    }, [dispatch])

    useEffect(() => {
        if (userStatus === 'idle') {
            dispatch(fetchUserProfile())
        }
    }, [userStatus, dispatch])

    useEffect(() => {
        if (isUserLoggedIn && username) {
            dispatch(fetchUserDetails(username))
        }
    }, [username, dispatch])

    return isUserLoggedIn // && isAllPluginsLoaded
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
