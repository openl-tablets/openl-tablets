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
    userMode: SystemUserMode
}

export interface OpenlInfo {
    'openl.site': string
    'openl.start.milli': string,
    'openl.version': string,
    'openl.build.date': string,
    'openl.start.time': string,
    'openl.start.hash': string
}