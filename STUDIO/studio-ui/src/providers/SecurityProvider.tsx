import { FC, PropsWithChildren, useCallback, useEffect, useMemo, useState } from 'react'
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

    const loadUserProfileAndDetails = () => {
        void fetchOpenlInformation()
        void fetchSystemSettings()
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

    const hasAdminPermission = useCallback(() => {
        return !!userProfile?.administrator
    }, [userProfile])

    const isExternalAuthSystem = useMemo(() => {
        return systemSettings?.userMode === SystemUserMode.EXTERNAL
    }, [systemSettings])

    const isUserManagementEnabled = useMemo(() => {
        return systemSettings?.supportedFeatures?.userManagement || false
    }, [systemSettings])

    const isGroupsManagementEnabled = useMemo(() => {
        return systemSettings?.supportedFeatures?.groupsManagement || false
    }, [systemSettings])

    const isPersonalAccessTokenEnabled = useMemo(() => {
        return systemSettings?.supportedFeatures?.personalAccessToken || false
    }, [systemSettings])

    const system = useMemo(() => ({
        systemSettings,
        isExternalAuthSystem,
        isUserManagementEnabled,
        isGroupsManagementEnabled,
        isPersonalAccessTokenEnabled,
        openlInfo,
        appVersion,
    }), [systemSettings, isExternalAuthSystem, isUserManagementEnabled, isGroupsManagementEnabled,
        isPersonalAccessTokenEnabled, openlInfo, appVersion])

    const permission = useMemo(() => ({ hasAdminPermission }), [hasAdminPermission])

    return (
        <SystemContext.Provider value={system}>
            <PermissionContext.Provider value={permission}>
                {children}
            </PermissionContext.Provider>
        </SystemContext.Provider>
    )
}
