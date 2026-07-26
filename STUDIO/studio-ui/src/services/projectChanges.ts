import { subscribeTopic, type TopicSubscription } from './stompTopic'
import { subscribeWorkspaceChanges } from './workspaceChanges'

/**
 * Watch for anything that can make an open project page stale: changes of that project in the user's
 * own workspace — from any session or client — and committed changes of any design repository. Matches
 * {@code ProjectSocketNotificationService.notifyProjectChanged} on the backend.
 *
 * The project's own ping names the files the change touched when the backend knows them — a folder
 * stands for anything under it, an empty list means a project-wide change. The page re-reads through
 * the REST API either way; the files let it refresh an open one precisely.
 *
 * The workspace-wide pings are watched too (the same pair the projects list watches): a project's id
 * mutates when it opens or turns local (it hashes the path), so a ping addressed to the new id would
 * slip past a subscription keyed by the old one — the id-free pings cannot miss.
 */
export function subscribeProjectChanges(projectId: string, onChange: (files: string[]) => void): TopicSubscription {
    const own = subscribeTopic(`/user/topic/projects/${encodeURIComponent(projectId)}/changed`,
        body => onChange(parseChangedFiles(body)))
    const workspaceWide = subscribeWorkspaceChanges(() => onChange([]))
    return {
        unsubscribe: () => {
            own.unsubscribe()
            workspaceWide.unsubscribe()
        },
    }
}

/** The ping body is `{"files": [...]}`; anything else reads as a change with unknown files. */
function parseChangedFiles(body: string): string[] {
    try {
        const payload = JSON.parse(body) as { files?: unknown }
        return Array.isArray(payload.files)
            ? payload.files.filter((file: unknown): file is string => typeof file === 'string')
            : []
    } catch {
        return []
    }
}
