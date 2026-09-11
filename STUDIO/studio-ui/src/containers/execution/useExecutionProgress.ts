import { useCallback, useEffect, useRef, useState } from 'react'
import { useWebSocket } from 'hooks/useWebSocket'
import { subscribeTopic, type TopicSubscription } from 'services/stompTopic'
import type { ExecutionStatus } from 'types/execution'

/** What the server reports while a run or a test run goes on: a status, and why it failed when it did. */
export interface ExecutionProgress {
    status: ExecutionStatus | null
    error: string | null
    /** How many results have arrived so far, for a run that reports them one by one. */
    arrived: number
    /** Whether the screen is listening. Until it is, a status may have been reported without it. */
    subscribed: boolean
    /** Forgets what the last execution reported, so that the next one is followed from its start. */
    reset: () => void
}

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

/** The status a frame reports, when it reports one this screen knows. */
const statusOf = (value: unknown): { status?: ExecutionStatus } =>
    typeof value === 'string' && STATUSES.has(value as ExecutionStatus)
        ? { status: value as ExecutionStatus }
        : {}

/**
 * What a reported status says.
 *
 * A status of its own arrives as its name, quoted as JSON text or bare; a status with a reason arrives as an
 * object carrying both. A frame that carries neither says nothing, and the screen keeps what it knows.
 */
const readStatus = (body: string): { status?: ExecutionStatus, message?: string } => {
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

/** Whether a run that reports this status has ended, whatever the outcome. */
export const isFinished = (status: ExecutionStatus | null): boolean => status !== null && TERMINAL.has(status)

/**
 * Follows a run, or a test run, while it goes on.
 *
 * The status arrives on the topic the server reports it on: as a plain status, or as a status with the reason
 * it failed. A run that reports its results one by one also counts them, so a screen can show what it is
 * waiting for.
 *
 * A screen that starts one execution after another on the same topic forgets what the last one reported
 * before it starts the next, so that a status left over from before is not taken for the new one.
 *
 * @param statusTopic  topic the status is reported on
 * @param resultsTopic topic each result is reported on, for a run that reports them
 */
export const useExecutionProgress = (statusTopic: string | null, resultsTopic?: string | null): ExecutionProgress => {
    const { isConnected } = useWebSocket({ autoConnect: true })
    const [progress, setProgress] = useState<Omit<ExecutionProgress, 'reset'>>(
        { status: null, error: null, arrived: 0, subscribed: false }
    )
    const subscriptions = useRef<TopicSubscription[]>([])

    const onStatus = useCallback((body: string) => {
        const reported = readStatus(body)
        setProgress(current => ({
            ...current,
            status: reported.status ?? current.status,
            // The reason is kept the way the status is: a message carrying none says nothing about why an
            // execution failed, and the next execution clears it by starting.
            error: reported.message ?? current.error,
        }))
    }, [])

    const onResult = useCallback(() => setProgress(current => ({ ...current, arrived: current.arrived + 1 })), [])

    const reset = useCallback(
        () => setProgress(current => ({ ...current, status: null, error: null, arrived: 0 })),
        []
    )

    useEffect(() => {
        if (!isConnected || !statusTopic) {
            return undefined
        }
        // One listener of this screen's own on each topic. Naming a subscription after the topic would give
        // two screens watching the same execution the same name, and the second would silence the first.
        subscriptions.current = [
            subscribeTopic(statusTopic, onStatus),
            ...(resultsTopic ? [subscribeTopic(resultsTopic, onResult)] : []),
        ]
        setProgress(current => ({ ...current, subscribed: true }))
        return () => {
            subscriptions.current.forEach(subscription => subscription.unsubscribe())
            subscriptions.current = []
            setProgress(current => ({ ...current, subscribed: false }))
        }
    }, [isConnected, statusTopic, resultsTopic, onStatus, onResult])

    return { ...progress, reset }
}
