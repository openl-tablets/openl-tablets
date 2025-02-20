import React, { FC, PropsWithChildren, useEffect, useMemo, useState } from 'react'
import { UserContext, SystemContext, PermissionContext } from '../contexts'
import { apiCall } from '../services'
import { UserDetails, UserProfile } from '../types/user'
import { SystemSettings } from '../types/system'

export const SecurityProvider: FC<PropsWithChildren> = ({ children }) => {
    const [userProfile, setUserProfile] = useState<UserProfile>()
    const [userDetails, setUserDetails] = useState<UserDetails>()
    const [systemSettings, setSystemSettings] = useState<SystemSettings>()
    const [isProfileLoaded, setIsProfileLoaded] = useState(false)
    const [isDetailsLoaded, setIsDetailsLoaded] = useState(false)
    const [isSystemSettingsLoaded, setIsSystemSettingsLoaded] = useState(false)

    const fetchUserProfileAndDetails = async () => {
        await loadSystemSettings()
        setIsSystemSettingsLoaded(true)
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
        return !!(userProfile && userProfile.administrator)
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
        const settings: SystemSettings = await apiCall('/settings')
        // TODO: delete this line
        // settings.supportedFeatures.groupsManagement = true
        setSystemSettings(settings)
    }

    const isExternalAuthSystem = useMemo(() => {
        return !!(systemSettings?.supportedFeatures.groupsManagement)
    }, [systemSettings])

    if (!isProfileLoaded || !isDetailsLoaded || !isSystemSettingsLoaded) {
        return null
    }

    if (!userProfile || !userDetails || !systemSettings) {
        console.error('User profile or details are not loaded')
        // TODO: Redirect to login/logout page
        return null
    }

    return (
        <SystemContext.Provider value={{ systemSettings, isExternalAuthSystem }}>
            <UserContext.Provider value={{ userProfile, userDetails, loadUserProfile }}>
                <PermissionContext.Provider value={{ hasAdminPermission }}>
                    {children}
                </PermissionContext.Provider>
            </UserContext.Provider>
        </SystemContext.Provider>
    )
}