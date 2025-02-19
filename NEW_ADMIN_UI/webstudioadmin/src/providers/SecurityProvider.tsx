import React, {FC, PropsWithChildren, useEffect, useMemo, useState} from 'react'
import {UserContext} from '../contexts/User'
import {PermissionContext} from '../contexts/Permission'
import {apiCall} from '../services'
import {UserGroupType} from '../constants'
import {UserDetails, UserProfile} from '../types/user'
import {SystemSettings} from "../types/system";
import {SystemUserMode} from "../constants/system";

export const SecurityProvider: FC<PropsWithChildren> = ({ children }) => {
    const [userProfile, setUserProfile] = useState<UserProfile>()
    const [userDetails, setUserDetails] = useState<UserDetails>()
    const [systemSettings, setSystemSettings] = useState<SystemSettings>()
    const [isProfileLoaded, setIsProfileLoaded] = useState(false)
    const [isDetailsLoaded, setIsDetailsLoaded] = useState(false)

    const fetchUserProfileAndDetails = async () => {
        loadSystemSettings()
        if (!userProfile) {
            await loadUserProfile()
            setIsProfileLoaded(true)
        }
        if (userProfile && userProfile.username) {
            await loadUserDetails()
            setIsDetailsLoaded(true)
        }
    }

    useEffect(() => {
        fetchUserProfileAndDetails()
    }, [userProfile])

    const hasAdminPermission = () => {
        return userDetails && 'userGroups' in userDetails
            ? userDetails.userGroups.some(group => group.type === UserGroupType.Admin)
            : false
    }

    const loadUserProfile = async () => {
        const profile = await apiCall('/users/profile')
        setUserProfile(profile)
    }

    const loadUserDetails = async () => {
        if (userProfile && userProfile.username) {
            const details = await apiCall(`/users/${userProfile.username}`)
            setUserDetails(details)
        }
    }

    const loadSystemSettings = async () => {
        const settings = await apiCall('/settings')
        setSystemSettings(settings)
    }

    const isExternalAuthSystem = useMemo(() => {
        return systemSettings?.userMode === SystemUserMode.EXTERNAL
    }, [systemSettings])

    if (!isProfileLoaded || !isDetailsLoaded) {
        return null
    }

    if (!userProfile || !userDetails) {
        console.log('User profile or details are not loaded')
        // TODO: Redirect to login/logout page
        return null
    }

    return (
        <UserContext.Provider value={{ userProfile, userDetails, isExternalAuthSystem, loadUserProfile }}>
            <PermissionContext.Provider value={{ hasAdminPermission }}>
                {children}
            </PermissionContext.Provider>
        </UserContext.Provider>
    )
}