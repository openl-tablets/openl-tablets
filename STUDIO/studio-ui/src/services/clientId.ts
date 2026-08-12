/** The header a request carries this tab's id in; matches {@code ChangeOriginResolver} on the backend. */
export const CLIENT_ID_HEADER = 'X-OpenL-Client-Id'

/**
 * The id this browser tab puts on every change it makes.
 *
 * The backend sends it back on the change ping the action causes, so the tab recognises the echo of
 * its own action and skips re-reading what it has already read. It names a running tab and nothing
 * else: a reload starts a new one, and it identifies no user.
 */
export const CLIENT_ID = newClientId()

/** An opaque token the backend accepts: letters, digits and `-_.`, at most 64 of them. */
function newClientId(): string {
    // `randomUUID` needs a secure context, which a plain-http install is not.
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
        return crypto.randomUUID()
    }
    return `${Math.random().toString(36).slice(2)}-${Math.random().toString(36).slice(2)}`
}
