import React, { FC, PropsWithChildren, useEffect, useState } from 'react'
import { UserContext} from '../contexts/User'
import { PermissionContext } from '../contexts/Permission'
import { apiCall } from '../services'
import { UserGroupType } from '../constants'

export const SecurityProvider: FC<PropsWithChildren> = ({ children }) => {
    const [userProfile, setUserProfile] = useState({})
    const [userDetails, setUserDetails] = useState({})
    const [permission, setPermission] = useState({})
    const [isExternalAuthSystem, setIsExternalAuthSystem] = useState(true)

    const fetchUserProfileAndDetails = async () => {
        const profile = await apiCall('/users/profile')
        console.log('profile', profile)
        setUserProfile(profile)
        const details = await apiCall(`/users/${profile.username}`)
        console.log('details', details)
        setUserDetails(details)
    }

    useEffect(() => {
        fetchUserProfileAndDetails()
    }, [])
    
    const hasAdminPermission = () => {
        return userDetails.userGroups?.some(group => group.type === UserGroupType.ADMIN)
    }

    return (
        <UserContext.Provider value={{userProfile, userDetails, isExternalAuthSystem}}>
            <PermissionContext.Provider value={{hasAdminPermission}}>
                {children}
            </PermissionContext.Provider>
        </UserContext.Provider>
    )
}