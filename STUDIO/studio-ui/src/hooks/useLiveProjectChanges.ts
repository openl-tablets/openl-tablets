import type { ChangePing } from '../services/changePing'
import { subscribeProjectChanges } from '../services/projectChanges'
import { useCoalescedChanges } from './useCoalescedChanges'

/** Whether the batch holds a change of the project the backend could not narrow to files. */
const projectWide = (pings: ChangePing[]): boolean =>
    pings.some(ping => ping.scope === 'project' && ping.files.length === 0)

/**
 * Runs the callback whenever the backend pings that the open project may be stale — the user changed
 * it from another session or client, or a design repository got new content.
 *
 * The callback receives the files the changes touched, when the backend named them — a folder stands
 * for anything under it, an empty list means a project-wide change or unknown files.
 *
 * Waits until the page knows its project: while {@code projectId} is {@code undefined} nothing is
 * watched. Bursts of pings collapse into one call, their files merged. With
 * {@code holdWhile} the page tells the hook it is running an action of its own, and the refresh
 * waits for it instead of racing the read the user is waiting for.
 */
export function useLiveProjectChanges(
    projectId: string | undefined,
    onChange: (files: string[]) => void,
    { holdWhile = false }: { holdWhile?: boolean } = {}
): void {
    useCoalescedChanges(
        projectId === undefined ? null : onPing => subscribeProjectChanges(projectId, onPing),
        // Only a project's own ping knows about files, and one naming none stands for anything, so
        // it swallows the files of the others. The workspace ping never names files at all.
        pings => onChange(projectWide(pings) ? [] : pings.flatMap(ping => ping.files)),
        [projectId],
        holdWhile
    )
}
