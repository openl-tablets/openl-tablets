import { useEffect } from 'react'
import traceService from 'services/traceService'
import { currentTraceLaunch } from 'services/traceLaunchToken'

/**
 * Ends the debug session when the debugger window closes. There is no Stop button — closing the
 * window is how a trace is stopped — so the session must not be left running on the server.
 *
 * The release is scoped to the document that still owns the session. It is skipped when the page is only
 * frozen for the back/forward cache (it may be restored), and when the launcher has reused this window for
 * a newer trace — otherwise the outgoing document would delete the session that newer launch just created.
 * A genuine window close (or a reload, which then starts a fresh session) still releases the session.
 */
export const useTerminateOnClose = (projectId: string | undefined): void => {
    useEffect(() => {
        if (!projectId) return undefined
        const openedWith = currentTraceLaunch()
        const release = (event: PageTransitionEvent): void => {
            // A back/forward-cache freeze may be restored; keep the session alive for it.
            if (event.persisted) return
            // A newer launch reused this window: its token differs, so this session is no longer ours.
            if (currentTraceLaunch() !== openedWith) return
            traceService.releaseOnClose(projectId)
        }
        window.addEventListener('pagehide', release)
        return () => window.removeEventListener('pagehide', release)
    }, [projectId])
}

export default useTerminateOnClose
