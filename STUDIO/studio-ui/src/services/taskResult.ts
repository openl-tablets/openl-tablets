/**
 * Raised when a result is asked for while the task producing it - a run, a test run, a benchmark, a
 * comparison - has not ended yet.
 *
 * The endpoint accepts such a request rather than refusing it, so the browser logs no error while a screen
 * waits for the task to end. Nothing has failed: the caller waits for the task to report its end, and asks
 * again.
 */
export class ResultNotReadyError extends Error {
    constructor() {
        super('The result is not ready: the task producing it has not ended yet')
        this.name = 'ResultNotReadyError'
    }
}

/**
 * Whether a result is missing because the task is still going on, rather than because it failed.
 *
 * Any other reason means the result will not come.
 */
export const isStillRunning = (error: unknown): error is ResultNotReadyError => error instanceof ResultNotReadyError
