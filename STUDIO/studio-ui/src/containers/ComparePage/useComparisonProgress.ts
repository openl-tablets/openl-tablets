import { useEffect, useState } from 'react'
import { useWebSocket } from 'hooks/useWebSocket'
import { subscribeTopic } from 'services/stompTopic'
import { comparisonStatusTopic } from 'services/compare'
import type { ComparisonStatus } from 'types/compare'

const STATUSES: ReadonlySet<string> = new Set(['PENDING', 'STARTED', 'COMPLETED', 'INTERRUPTED', 'ERROR'])

/** Whether the comparison has stopped, whatever came of it. */
export const isFinished = (status: ComparisonStatus | null): boolean =>
    status === 'COMPLETED' || status === 'ERROR' || status === 'INTERRUPTED'

/** The status a message carries, or null when it carries none this screen knows. */
const statusOf = (value: unknown): ComparisonStatus | null =>
    typeof value === 'string' && STATUSES.has(value) ? value as ComparisonStatus : null

/** What a message says: a bare status, or a status with the reason a comparison could not be made. */
export const readStatus = (body: string): { status: ComparisonStatus | null; error: string | null } => {
    const bare = statusOf(body)
    if (bare) {
        return { status: bare, error: null }
    }
    try {
        const parsed: unknown = JSON.parse(body)
        if (parsed && typeof parsed === 'object') {
            const message = parsed as { status?: unknown; message?: unknown }
            return {
                status: statusOf(message.status),
                error: typeof message.message === 'string' ? message.message : null,
            }
        }
    } catch {
        // A body that is neither a status nor an object says nothing about the comparison.
    }
    return { status: null, error: null }
}

export interface ComparisonProgress {
    status: ComparisonStatus | null
    error: string | null
    /** Whether the screen is listening. Until it is, the comparison may have ended unheard. */
    subscribed: boolean
}

/**
 * Follows one comparison as the server reports it.
 *
 * The comparison is watched from the moment it is named until it is dropped, so a screen that starts
 * another comparison sees the progress of that one and nothing of the previous.
 *
 * A topic carries only what happens while it is listened to, and the connection is opened when the
 * comparison is started, so the progress says whether the screen is listening yet. A comparison that
 * ends before that has to be asked about.
 */
export const useComparisonProgress = (comparisonId: string | null): ComparisonProgress => {
    const { isConnected } = useWebSocket({ autoConnect: true })
    const [progress, setProgress] = useState<ComparisonProgress>(
        { status: null, error: null, subscribed: false }
    )

    // What the previous comparison reported is forgotten, and only when another one is named: a
    // connection that drops and comes back says nothing about the comparison itself.
    useEffect(() => {
        setProgress({ status: null, error: null, subscribed: false })
    }, [comparisonId])

    useEffect(() => {
        if (!comparisonId || !isConnected) {
            return
        }
        const subscription = subscribeTopic(comparisonStatusTopic(comparisonId), body => {
            const next = readStatus(body)
            if (next.status || next.error) {
                setProgress(current => ({ ...current, ...next }))
            }
        })
        setProgress(current => ({ ...current, subscribed: true }))
        return () => {
            subscription.unsubscribe()
            setProgress(current => ({ ...current, subscribed: false }))
        }
    }, [comparisonId, isConnected])

    return progress
}
