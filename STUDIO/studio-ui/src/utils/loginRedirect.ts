/**
 * Validates redirect URL after login to prevent redirects to extension scripts
 * (e.g. fetchtrace-sw.js) or other unsafe targets.
 *
 * @param url - The URL to validate (from response.url after fetch redirect)
 * @param baseOrigin - Origin to allow (default: window.location.origin)
 * @returns true if the URL is safe to redirect to
 */
export function isSafeRedirectUrl(url: string, baseOrigin = window.location.origin): boolean {
    try {
        const parsed = new URL(url, baseOrigin)
        const path = parsed.pathname
        // Reject redirects to scripts, extensions (e.g. fetchtrace-sw.js), static assets
        if (path.endsWith('.js') || path.endsWith('.css') || path.includes('fetchtrace') || path.includes('-sw.')) {
            return false
        }
        // Only allow same-origin redirects to app routes
        return parsed.origin === baseOrigin
    } catch {
        return false
    }
}
