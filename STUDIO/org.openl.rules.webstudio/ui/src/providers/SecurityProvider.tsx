import React, { FC, PropsWithChildren, useEffect, useMemo, useState } from 'react'
import { UserContext, SystemContext, PermissionContext } from '../contexts'
import { apiCall } from '../services'
import { UserDetails, UserProfile } from '../types/user'
import { OpenlInfo, SystemSettings } from '../types/system'
import { SystemUserMode } from '../constants/system'

export const SecurityProvider: FC<PropsWithChildren> = ({ children }) => {
    const [userProfile, setUserProfile] = useState<UserProfile>()
    const [userDetails, setUserDetails] = useState<UserDetails>()
    const [systemSettings, setSystemSettings] = useState<SystemSettings>()
    const [openlInfo, setOpenlInfo] = useState<OpenlInfo>()
    const [appVersion, setAppVersion] = useState<string>('')
    const [isProfileLoaded, setIsProfileLoaded] = useState(false)
    const [isDetailsLoaded, setIsDetailsLoaded] = useState(false)
    const [isSystemSettingsLoaded, setIsSystemSettingsLoaded] = useState(false)

    const fetchUserProfile = async () => {
        const profile = await apiCall('/users/profile')
        setUserProfile(profile)
    }

    const fetchUserDetails = async () => {
        if (userProfile && userProfile.username) {
            const details = await apiCall(`/users/${userProfile.username}`)
            setUserDetails(details)
        }
    }

    const fetchSystemSettings = async () => {
        const settings: SystemSettings = await apiCall('/settings')
        // TODO: delete this line
        // settings.userMode = SystemUserMode.EXTERNAL
        setSystemSettings(settings)
    }

    const fetchOpenlInformation = async () => {
        const openlInfo = await apiCall('/public/info/openl.json')
        setOpenlInfo(openlInfo)
    }

    const loadUserProfileAndDetails = async () => {
        fetchOpenlInformation()
        await fetchSystemSettings()
        setIsSystemSettingsLoaded(true)
        if (!userProfile) {
            await fetchUserProfile()
            setIsProfileLoaded(true)
        }
        if (userProfile && userProfile.username) {
            await fetchUserDetails()
            setIsDetailsLoaded(true)
        }
    }

    useEffect(() => {
        loadUserProfileAndDetails()
    }, [userProfile])

    useEffect(() => {
        const version = openlInfo?.['openl.version'] || ''
        const buildNumber = openlInfo?.['openl.build.number'] || ''
        const snapshotRE = /-SNAPSHOT/
        // If the version ends with -SNAPSHOT, we don't want to show it in the UI. Replace it with the build number.
        if (snapshotRE.test(version)) {
            setAppVersion(`${version.replace(snapshotRE, '')}-${buildNumber}`)
        } else {
            setAppVersion(version)
        }

    }, [openlInfo])

    const hasAdminPermission = () => {
        return !!(userProfile && userProfile.administrator)
    }

    const isExternalAuthSystem = useMemo(() => {
        return systemSettings?.userMode === SystemUserMode.EXTERNAL
    }, [systemSettings])

    const isUserManagementEnabled = useMemo(() => {
        return systemSettings?.supportedFeatures.userManagement || false
    }, [systemSettings])

    const isGroupsManagementEnabled = useMemo(() => {
        return systemSettings?.supportedFeatures.groupsManagement || false
    }, [systemSettings])

    if (!isProfileLoaded || !isDetailsLoaded || !isSystemSettingsLoaded) {
        return null
    }

    if (!userProfile || !userDetails || !systemSettings || !openlInfo) {
        console.error('User profile or details are not loaded')
        // TODO: Redirect to login/logout page
        return null
    }

    return (
        <SystemContext.Provider value={{ systemSettings, isExternalAuthSystem, isUserManagementEnabled, isGroupsManagementEnabled, openlInfo, appVersion }}>
            <UserContext.Provider value={{ userProfile, userDetails, fetchUserProfile }}>
                <PermissionContext.Provider value={{ hasAdminPermission }}>
                    {children}
                </PermissionContext.Provider>
            </UserContext.Provider>
        </SystemContext.Provider>
    )
}