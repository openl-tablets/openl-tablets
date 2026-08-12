import { subscribeProjectChanges } from '../services/projectChanges'
import { useCoalescedChanges } from './useCoalescedChanges'

/**
 * Runs the callback whenever the backend pings that the open project may be stale — the user changed
 * it from another session or client, or a design repository got new content.
 *
 * The callback receives the files the changes touched, when the backend named them — a folder stands
 * for anything under it, an empty list means a project-wide change or unknown files.
 *
 * Waits until the page knows its project: while {@code projectId} is {@code undefined} nothing is
 * watched. Bursts of pings collapse into one call, their files merged.
 */
export function useLiveProjectChanges(projectId: string | undefined, onChange: (files: string[]) => void): void {
    useCoalescedChanges(
        projectId === undefined ? null : onPing => subscribeProjectChanges(projectId, onPing),
        pings => onChange(pings.flatMap(ping => ping.files)),
        [projectId]
    )
}
