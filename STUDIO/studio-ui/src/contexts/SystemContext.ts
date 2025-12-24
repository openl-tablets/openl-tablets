import { createContext }  from 'react'
import { OpenlInfo, SystemSettings } from '../types/system'

type SystemContextType = {
    systemSettings?: SystemSettings
    openlInfo?: OpenlInfo
    appVersion?: string
    isExternalAuthSystem: boolean
    isUserManagementEnabled: boolean
    isGroupsManagementEnabled: boolean
    isPersonalAccessTokenEnabled: boolean
}

export const SystemContext = createContext<SystemContextType>({
    isExternalAuthSystem: false,
    isUserManagementEnabled: false,
    isGroupsManagementEnabled: false,
    isPersonalAccessTokenEnabled: false,
})
