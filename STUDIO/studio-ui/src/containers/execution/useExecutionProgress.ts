import { useCallback, useEffect, useRef, useState } from 'react'
import { useWebSocket } from 'hooks/useWebSocket'
import { isFinished, readStatus } from 'services/executionStatus'
import { subscribeTopic, type TopicSubscription } from 'services/stompTopic'
import type { ExecutionStatus } from 'types/execution'

export { isFinished }

/** What the server reports while a run or a test run goes on: a status, and why it failed when it did. */
interface ExecutionProgress {
    status: ExecutionStatus | null
    error: string | null
    /** How many results have arrived so far, for a run that reports them one by one. */
    arrived: number
    /** Whether the screen is listening. Until it is, a status may have been reported without it. */
    subscribed: boolean
    /** Forgets what the last execution reported, so that the next one is followed from its start. */
    reset: () => void
}

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

/** How long a screen that waits for an execution to end hears nothing before it asks after the result again. */
export const QUIET_SPELL_MS = 5000

/**
 * Counts the quiet spells of an execution a screen waits for.
 *
 * A spell is {@link QUIET_SPELL_MS} in which the server reports neither a status nor a result. The screen asks
 * after the result again at each one: an execution that ends just as the screen starts listening reports its
 * end to nobody, and the screen would wait for good.
 *
 * Anything the server reports starts the spell over, so a screen that hears the execution going on asks nothing.
 * An execution that has said it ended has nothing more to say, and counts no spell.
 *
 * @param progress what the server reports of the execution
 * @param waiting  whether the screen still waits for the result
 * @return how many spells have passed
 */
export const useQuietSpells = (
    progress: Pick<ExecutionProgress, 'status' | 'arrived' | 'subscribed'>,
    waiting: boolean
): number => {
    const [spells, setSpells] = useState(0)
    const { status, arrived, subscribed } = progress
    const listening = waiting && subscribed && !isFinished(status)

    useEffect(() => {
        if (!listening) {
            return undefined
        }
        const spell = setTimeout(() => setSpells(count => count + 1), QUIET_SPELL_MS)
        return () => clearTimeout(spell)
    }, [listening, status, arrived, spells])

    return spells
}
