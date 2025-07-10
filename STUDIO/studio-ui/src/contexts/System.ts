import { createContext }  from 'react'
import { OpenlInfo, SystemSettings } from '../types/system'

export const SystemContext = createContext({
    systemSettings: {} as SystemSettings,
    openlInfo: {} as OpenlInfo,
    appVersion: '',
    isExternalAuthSystem: false,
    isUserManagementEnabled: false,
    isGroupsManagementEnabled: false,
})
