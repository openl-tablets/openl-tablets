import React, { useEffect } from 'react'
import RootRoutes from './routes'
import { App as AntApp } from 'antd'
import { fetchUserProfile } from './containers/user/userSlice'
import { RootState, useAppDispatch, useAppSelector } from 'store'
import { fetchNotification } from './containers/notification/notificationSlice'

function App() {
    const dispatch = useAppDispatch()

    const userStatus = useAppSelector((state: RootState) => state.user.status)
    const isUserLoggedIn = useAppSelector((state: RootState) => state.user.isLoggedIn)

    useEffect(() => {
        dispatch(fetchNotification())
        const intervalCall = setInterval(() => {
            dispatch(fetchNotification())
        }, 30000)
        return () => {
            // clean up
            clearInterval(intervalCall)
        }
    }, [ dispatch ])

    useEffect(() => {
        if (userStatus === 'idle') {
            dispatch(fetchUserProfile())
        }
    }, [ userStatus, dispatch ])

    return isUserLoggedIn
        ? (
            <AntApp>
                <RootRoutes />
            </AntApp>
        )
        : null
}

export default App
