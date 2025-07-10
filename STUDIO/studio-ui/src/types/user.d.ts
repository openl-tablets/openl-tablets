import { DisplayUserName, UserGroupType } from '../constants'

export interface UserDetailsGroup {
    name: string
    type: UserGroupType
}

export interface UserExternalFlags {
    firstNameExternal: boolean
    lastNameExternal: boolean
    displayNameExternal: boolean
    emailExternal: boolean
    emailVerified: boolean
}

export interface UserDetails {
    email: string
    displayName: string
    firstName: string
    lastName: string
    password: string
    groups: string[]
    username: string
    internalPassword: {
        password: string
    }
    currentUser: boolean
    superUser: boolean
    unsafePassword: boolean
    externalFlags: UserExternalFlags
    notMatchedExternalGroupsCount: number
    online: boolean
    userGroups: UserDetailsGroup[]
}

export interface UserProfile {
    administrator: boolean
    displayName: string
    email: string
    externalFlags: {
        displayNameExternal: boolean
        emailExternal: boolean
        emailVerified: boolean
        firstNameExternal: boolean
        lastNameExternal: boolean
    }
    firstName: string
    lastName: string
    profiles: [{
        description: string
        displayName: string
        name: string
    }]
    showComplexResult: boolean
    showFormulas: boolean
    showHeader: boolean
    showRealNumbers: boolean
    testsFailuresOnly: boolean
    testsFailuresPerTest: number
    testsPerPage: number
    treeView: string
    username: string
}

export interface UserProfileFormFields extends UserProfile {
    changePassword?: {
        currentPassword?: string
        newPassword?: string
        confirmPassword?: string
    }
    displayNameSelect?: DisplayUserName
}
