import { createContext }  from 'react'
import { SystemSettings } from '../types/system'

export const SystemContext = createContext({
    systemSettings: {} as SystemSettings,
    isExternalAuthSystem: false,
})