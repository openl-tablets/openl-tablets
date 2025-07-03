import { createContext }  from 'react'
import { OpenlInfo, SystemSettings } from '../types/system'

export const SystemContext = createContext({
    systemSettings: {} as SystemSettings,
    openlInfo: {} as OpenlInfo,
    isExternalAuthSystem: false,
    isUserManagementEnabled: false,
    isGroupsManagementEnabled: false,
})