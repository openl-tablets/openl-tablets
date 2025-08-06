import React, { FC, PropsWithChildren, useEffect, useMemo, useState } from 'react'
import { SystemContext, PermissionContext } from '../contexts'
import { apiCall } from '../services'
import { OpenlInfo, SystemSettings } from '../types/system'
import { SystemUserMode } from '../constants/system'
import { useUserStore } from 'store'

export const SecurityProvider: FC<PropsWithChildren> = ({ children }) => {
    const { userProfile } = useUserStore()
    const [systemSettings, setSystemSettings] = useState<SystemSettings>()
    const [openlInfo, setOpenlInfo] = useState<OpenlInfo>()
    const [appVersion, setAppVersion] = useState<string>('')

    const fetchSystemSettings = async () => {
        const settings: SystemSettings = await apiCall('/settings')
        setSystemSettings(settings)
    }

    const fetchOpenlInformation = async () => {
        const openlInfo = await apiCall('/public/info/openl.json')
        setOpenlInfo(openlInfo)
    }

    const loadUserProfileAndDetails = async () => {
        fetchOpenlInformation()
        fetchSystemSettings()
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

    const getLogoutUrl = () => {
        return systemSettings?.entrypoint.logoutUrl || ''
    }

    const hasAdminPermission = () => {
        return !!(userProfile && userProfile.administrator)
    }

    const isExternalAuthSystem = useMemo(() => {
        return systemSettings?.userMode === SystemUserMode.EXTERNAL
    }, [systemSettings])

    const isUserManagementEnabled = useMemo(() => {
        return systemSettings?.supportedFeatures?.userManagement || false
    }, [systemSettings])

    const isGroupsManagementEnabled = useMemo(() => {
        return systemSettings?.supportedFeatures?.groupsManagement || false
    }, [systemSettings])

    return (
        <SystemContext.Provider value={{ systemSettings, isExternalAuthSystem, isUserManagementEnabled, isGroupsManagementEnabled, openlInfo, appVersion, getLogoutUrl }}>
            <PermissionContext.Provider value={{ hasAdminPermission }}>
                {children}
            </PermissionContext.Provider>
        </SystemContext.Provider>
    )
}
