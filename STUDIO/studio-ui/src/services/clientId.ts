/** The header a request carries this tab's id in; matches {@code ChangeOriginResolver} on the backend. */
export const CLIENT_ID_HEADER = 'X-OpenL-Client-Id'

/**
 * The id this browser tab puts on every change it makes.
 *
 * The backend sends it back on the change ping the action causes, so the tab recognises the echo of
 * its own action and skips re-reading what it has already read. It names a running tab and nothing
 * else: a reload starts a new one, and it identifies no user.
 *
 * Hard to guess on purpose. The id is not a secret, but a client that knew another tab's id could
 * put it on its own changes and have that tab skip them as its own echo, so it is drawn from the
 * platform's cryptographic source rather than from `Math.random`.
 */
export const CLIENT_ID = newClientId()

/** An opaque token the backend accepts: letters, digits and `-_.`, at most 64 of them. */
function newClientId(): string {
    // `randomUUID` needs a secure context, which a plain-http install is not; `getRandomValues`
    // does not, so it covers the same installs without falling back to a guessable id.
    if (typeof crypto !== 'undefined') {
        if (typeof crypto.randomUUID === 'function') {
            return crypto.randomUUID()
        }
        if (typeof crypto.getRandomValues === 'function') {
            return Array.from(crypto.getRandomValues(new Uint8Array(16)),
                byte => byte.toString(16).padStart(2, '0')).join('')
        }
    }
    // No cryptographic source at all — older than anything the UI supports. The tab still needs a
    // name of its own; two tabs opened in the same millisecond would share one and skip each other's
    // echoes, which costs a missed refresh and nothing more.
    return `t${Date.now().toString(36)}`
}
