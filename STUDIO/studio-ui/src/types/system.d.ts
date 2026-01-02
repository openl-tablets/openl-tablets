import { SystemUserMode } from '../constants/system'

export interface SystemSettings {
    supportedFeatures?: {
        emailVerification: boolean
        groupsManagement: boolean
        userManagement: boolean
        personalAccessToken: boolean
    },
    userMode?: SystemUserMode,
    scripts?: string | string[]
}

export interface OpenlInfo {
    'openl.site': string
    'openl.start.milli': string,
    'openl.version': string,
    'openl.build.date': string,
    'openl.build.number'?: string,
    'openl.start.time': string,
    'openl.start.hash': string
}
