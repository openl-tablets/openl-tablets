import { parseChangePing, type ChangePing } from './changePing'
import { subscribeTopic, type TopicSubscription } from './stompTopic'
import { subscribeWorkspaceChanges } from './workspaceChanges'

/**
 * Watch for anything that can make an open project page stale: changes of that project in the user's
 * own workspace — from any session or client — and committed changes of any design repository. Matches
 * {@code ProjectSocketNotificationService.notifyProjectChanged} on the backend.
 *
 * The project's own ping names the files the change touched when the backend knows them — a folder
 * stands for anything under it, an empty list means a project-wide change. The page re-reads through
 * the REST API either way; the files let it refresh an open one precisely. Every ping names the
 * clients whose requests caused it, so a page can tell its own echo from a real change.
 *
 * The workspace-wide pings are watched too (the same pair the projects list watches): a project's id
 * mutates when it opens or turns local (it hashes the path), so a ping addressed to the new id would
 * slip past a subscription keyed by the old one — the id-free pings cannot miss.
 */
export function subscribeProjectChanges(projectId: string, onChange: (ping: ChangePing) => void): TopicSubscription {
    const own = subscribeTopic(`/user/topic/projects/${encodeURIComponent(projectId)}/changed`,
        body => onChange(parseChangePing(body, 'project')))
    const workspaceWide = subscribeWorkspaceChanges(onChange)
    return {
        unsubscribe: () => {
            own.unsubscribe()
            workspaceWide.unsubscribe()
        },
    }
}
