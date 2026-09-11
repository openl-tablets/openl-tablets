import CONFIG from './config'
import { errorHandler } from 'utils/errorHandling'
import { saveFile } from 'utils/download'
import { retireTraceLaunch, stampTraceLaunch } from './traceLaunchToken'
import { traceService } from './traceService'

/** What a trace is started with, as the launcher collects it from the table page. */
export interface TraceLaunchRequest {
    projectId: string
    tableId: string
    /** Ids or ranges of the test cases to trace. Only for a test table. */
    testRanges?: string | undefined
    /** Module to trace within. The whole project when absent. */
    fromModule?: string | undefined
    /** Input of a rule table as JSON. Absent for a test table. */
    inputJson?: string | undefined
    /** Save the trace as a text file instead of opening the trace window. */
    download?: boolean | undefined
    /** Open the full step debugger instead of the business view. */
    advanced?: boolean | undefined
    /** Write numbers as the rules computed them, without rounding, in the downloaded file. */
    showRealNumbers: boolean
}

/** What a launch says when the browser would not open the trace window. */
export const TRACE_WINDOW_BLOCKED = 'trace.windowBlocked'

/** Opens the trace window, answering whether the browser opened it. */
const openTraceWindow = (request: TraceLaunchRequest): boolean => {
    const params = new URLSearchParams()
    params.set('tableId', request.tableId)
    if (request.fromModule) params.set('fromModule', request.fromModule)
    if (request.testRanges) params.set('testRanges', request.testRanges)
    // Carry the launch-time mode so the trace window opens straight into the business or advanced view.
    if (request.advanced) params.set('advanced', 'true')
    const url = `${CONFIG.CONTEXT}/trace/${encodeURIComponent(request.projectId)}?${params.toString()}`
    return window.open(url, 'trace_win', 'width=1240,height=800,resizable=yes,scrollbars=yes') !== null
}

/**
 * Starts a trace and shows it.
 *
 * The trace window opens and attaches to the session created here. With the download option the whole trace is
 * saved as a text file instead. The input is sent once, so the window needs nothing but the session.
 *
 * Rejects when the session cannot be started, and when the browser will not open the window. The launch
 * token goes back to its previous owner then, and the session that has nothing to attach to is ended.
 */
export const launchTrace = async (request: TraceLaunchRequest): Promise<void> => {
    // The launch is stamped before the session is created. A debugger window closing meanwhile sees a changed
    // token and leaves the new session alone.
    const reserved = stampTraceLaunch()
    try {
        await traceService.startTrace(request.projectId, {
            tableId: request.tableId,
            ...(request.testRanges !== undefined && { testRanges: request.testRanges }),
            ...(request.fromModule !== undefined && { fromModule: request.fromModule }),
            ...(request.inputJson !== undefined && { inputJson: request.inputJson }),
            stopAtEntry: true,
        })
    } catch (error) {
        // The launch failed and nothing replaced the previous session. The token goes back to its owner.
        retireTraceLaunch(reserved)
        throw error
    }
    if (request.download) {
        // Trace into File saves the run as text. No debugger window opens.
        saveFile(await traceService.exportTrace(request.projectId, request.showRealNumbers), 'trace.txt')
        return
    }
    // Creating the session waits for a project that may still be compiling, so by the time the window is
    // asked for, the click that started all this may no longer count as one. A window the browser refused is
    // a launch that did not happen: nothing will ever attach to the session, so it is asked to end rather
    // than left holding a parked execution, and the caller is told either way. A session that could not be
    // ended here is reclaimed by the server, which reaps a session nothing has used.
    if (!openTraceWindow(request)) {
        await traceService.cancelTrace(request.projectId).catch(cancelError => errorHandler.logError(cancelError, {
            message: `Could not release the trace session of ${request.projectId} after the window was refused`,
        }))
        retireTraceLaunch(reserved)
        throw new Error(TRACE_WINDOW_BLOCKED)
    }
}
