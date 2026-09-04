/**
 * Cross-window guard for the reused debugger window.
 *
 * The launcher opens the debugger in a single named window and reuses it for every subsequent trace. That
 * reuse navigates the window, so the outgoing document fires `pagehide` — and its close-time session
 * teardown would delete the session the launcher just created for the incoming document (the session
 * registry holds one slot per browser session, so even another project's trace would be torn down).
 *
 * To scope the teardown to the document that still owns the session, the launcher stamps a fresh token on
 * every trace it starts, and the debugger window records the token it opened with. On close the window
 * releases the session only while its token is still the current one; a newer launch changes the token, so
 * the outgoing document keeps its hands off the newer session. A genuine window close leaves the token
 * unchanged and still releases the session.
 */
import { readStored, writeStored } from 'utils/localStore'

const KEY = 'openl.trace.launchToken'

/** Stamp a new launch token before opening or reusing the debugger window. Returns the new token. */
export const stampTraceLaunch = (): string => {
    const next = String(Number(readStored(KEY) ?? '0') + 1)
    writeStored(KEY, next)
    return next
}

/** The token of the most recent launch, or `null` when no trace has been launched this session. */
export const currentTraceLaunch = (): string | null => readStored(KEY)

/**
 * Give up a reserved launch after a failed start, restoring the previous token so the debugger window
 * that owns it keeps releasing its session on close. Does nothing when a newer launch has already stamped
 * its own token — the reservation is stale and the token belongs to that launch.
 */
export const retireTraceLaunch = (reserved: string): void => {
    if (readStored(KEY) === reserved) {
        writeStored(KEY, String(Number(reserved) - 1))
    }
}
