import CONFIG from './config'
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

const openTraceWindow = (request: TraceLaunchRequest): void => {
    const params = new URLSearchParams()
    params.set('tableId', request.tableId)
    if (request.fromModule) params.set('fromModule', request.fromModule)
    if (request.testRanges) params.set('testRanges', request.testRanges)
    // Carry the launch-time mode so the trace window opens straight into the business or advanced view.
    if (request.advanced) params.set('advanced', 'true')
    const url = `${CONFIG.CONTEXT}/trace/${encodeURIComponent(request.projectId)}?${params.toString()}`
    window.open(url, 'trace_win', 'width=1240,height=800,resizable=yes,scrollbars=yes')
}

/**
 * Starts a trace and shows it.
 *
 * The trace window opens and attaches to the session created here. With the download option the whole trace is
 * saved as a text file instead. The input is sent once, so the window needs nothing but the session.
 *
 * Rejects when the session cannot be started. Nothing is opened then, and the launch token goes back to its
 * previous owner.
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
    } else {
        openTraceWindow(request)
    }
}
