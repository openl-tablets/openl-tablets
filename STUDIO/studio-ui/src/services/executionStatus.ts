import type { ExecutionStatus } from 'types/execution'

/** Every status the server reports about work it was asked to do. */
const STATUSES = new Set<ExecutionStatus>(['PENDING', 'STARTED', 'COMPLETED', 'INTERRUPTED', 'ERROR'])

const TERMINAL = new Set<ExecutionStatus>(['COMPLETED', 'INTERRUPTED', 'ERROR'])

/** What a frame carries, read as JSON. A frame that is not JSON is the bare name of a status. */
const bodyOf = (body: string): unknown => {
    try {
        return JSON.parse(body)
    } catch {
        return body
    }
}

/** The status a frame reports, when it reports one this interface knows. */
const statusOf = (value: unknown): { status?: ExecutionStatus } =>
    typeof value === 'string' && STATUSES.has(value as ExecutionStatus)
        ? { status: value as ExecutionStatus }
        : {}

/**
 * What a reported status says.
 *
 * A status of its own arrives as its name, quoted as JSON text or bare; a status with a reason arrives
 * as an object carrying both. A frame that carries neither says nothing, and the screen keeps what it
 * knows.
 *
 * Every screen that follows work over the WebSocket - a run, a test run, a comparison - reads a frame
 * through this, so the rules a frame is read by are written once.
 */
export const readStatus = (body: string): { status?: ExecutionStatus, message?: string } => {
    const reported = bodyOf(body)
    if (reported === null || typeof reported !== 'object') {
        return statusOf(reported)
    }
    const frame = reported as { status?: unknown, message?: unknown }
    return {
        ...statusOf(frame.status),
        ...(typeof frame.message === 'string' && { message: frame.message }),
    }
}

/** Whether work that reports this status has ended, whatever the outcome. */
export const isFinished = (status: ExecutionStatus | null): boolean => status !== null && TERMINAL.has(status)
