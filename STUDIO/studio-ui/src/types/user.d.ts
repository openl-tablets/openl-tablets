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
    lastLoginTime?: string
    userGroups: UserDetailsGroup[]
}

export interface UpdatedUserRequest {
    email: string
    displayName: string
    firstName: string
    lastName: string
    password: string | null
    groups?: string[]
    // Attributes for new user
    username?: string
    internalPassword?: {
        password: string | null
    }
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
    showComplexResult: boolean
    showFormulas: boolean
    showHeader: boolean
    showRealNumbers: boolean
    /**
     * The table theme the tables it styles are drawn with, and that is offered first when a theme is applied, by its
     * identifier. Absent or empty, the tables are drawn with the formatting of the Excel file.
     */
    tableTheme?: string
    testsFailuresOnly: boolean
    testsFailuresPerTest: number
    testsPerPage: number
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
