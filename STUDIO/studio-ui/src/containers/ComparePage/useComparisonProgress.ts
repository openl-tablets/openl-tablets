import { useEffect, useState } from 'react'
import { useWebSocket } from 'hooks/useWebSocket'
import { readStatus } from 'services/executionStatus'
import { subscribeTopic } from 'services/stompTopic'
import { comparisonStatusTopic } from 'services/compare'
import type { ComparisonStatus } from 'types/compare'

export { isFinished } from 'services/executionStatus'

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
            const reported = readStatus(body)
            // A frame that says nothing this screen knows leaves what it knows alone, and a frame that
            // carries no reason leaves the reason the last one gave: what went wrong is said once.
            if (reported.status || reported.message) {
                setProgress(current => ({
                    ...current,
                    ...(reported.status && { status: reported.status }),
                    ...(reported.message && { error: reported.message }),
                }))
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
