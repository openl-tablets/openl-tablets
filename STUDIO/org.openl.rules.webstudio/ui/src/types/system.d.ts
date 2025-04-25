import { SystemUserMode } from '../constants/system'

export interface SystemSettings {
    entrypoint: {
        logoutUrl: string
    }
    supportedFeatures: {
        emailVerification: boolean
        groupsManagement: boolean
        userManagement: boolean
    },
    'userMode': SystemUserMode
}