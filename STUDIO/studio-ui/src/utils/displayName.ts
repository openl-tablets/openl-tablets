import { DisplayUserName } from '../constants'

/** The parts of a user identity that determine how its display name is built. */
interface DisplayNameParts {
    firstName?: string | null
    lastName?: string | null
    displayName?: string | null
}

/**
 * Chooses the display-name mode from a stored identity: "First Last", "Last First", or a custom value
 * that matches neither ordering.
 */
export const deriveDisplayNameMode = ({ firstName, lastName, displayName }: DisplayNameParts): DisplayUserName => {
    const first = firstName || ''
    const last = lastName || ''
    if (displayName && displayName === `${first} ${last}`.trim()) {
        return DisplayUserName.FirstLast
    }
    if (displayName && displayName === `${last} ${first}`.trim()) {
        return DisplayUserName.LastFirst
    }
    return DisplayUserName.Other
}

/**
 * The display name produced by a mode, or {@code null} when the mode is a custom value the caller keeps
 * as-is.
 */
export const formatDisplayName = (
    mode: DisplayUserName,
    firstName?: string | null,
    lastName?: string | null
): string | null => {
    // A default parameter would not cover null, which the profile sends for a missing name.
    const first = firstName || '' // NOSONAR typescript:S7760
    const last = lastName || '' // NOSONAR typescript:S7760
    if (mode === DisplayUserName.FirstLast) {
        return `${first} ${last}`.trim()
    }
    if (mode === DisplayUserName.LastFirst) {
        return `${last} ${first}`.trim()
    }
    return null
}
