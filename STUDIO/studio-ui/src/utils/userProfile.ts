export interface UserIdentity {
    email?: string | null
    firstName?: string | null
    lastName?: string | null
    displayName?: string | null
}

const isFilled = (value?: string | null): boolean => Boolean(value?.trim())

export const isUserProfileComplete = (identity?: UserIdentity | null): boolean =>
    Boolean(
        identity
        && isFilled(identity.email)
        && isFilled(identity.displayName)
    )
