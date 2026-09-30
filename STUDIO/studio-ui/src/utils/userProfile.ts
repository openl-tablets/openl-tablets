export interface UserIdentity {
    email?: string | null
    firstName?: string | null
    lastName?: string | null
    displayName?: string | null
}

const isFilled = (value?: string | null): boolean => Boolean(value?.trim())

/**
 * The fields of a form that differ from the values it was opened with.
 *
 * A profile update keeps whatever it is not sent, so sending only these leaves alone what has been saved since the
 * form was opened, in another tab for example.
 */
export const changedValues = <T extends object>(values: T, initial?: object | null): Partial<T> => {
    const opened = (initial ?? {}) as Record<string, unknown>
    return Object.fromEntries(Object.entries(values).filter(([key, value]) => value !== opened[key])) as Partial<T>
}

export const isUserProfileComplete = (identity?: UserIdentity | null): boolean =>
    Boolean(
        identity
        && isFilled(identity.email)
        && isFilled(identity.displayName)
    )
