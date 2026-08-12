import { parseChangePing, type ChangePing } from './changePing'
import { subscribeTopic, type TopicSubscription } from './stompTopic'

/**
 * The user's own workspace changed: they opened, closed, saved or deleted a project, switched its
 * branch, or edited it — possibly from another session or client.
 * Matches {@code ProjectSocketNotificationService.notifyWorkspaceChanged} on the backend.
 */
const WORKSPACE_CHANGED_TOPIC = '/user/topic/workspace/changed'

/**
 * The content of a design repository changed — a commit or merge from any user, or an external push.
 * Matches {@code ProjectSocketNotificationService.notifyProjectsChanged}, which sends it to each
 * user separately: it concerns everyone, but the clients it names are only ever the reader's own.
 */
const PROJECTS_CHANGED_TOPIC = '/user/topic/projects/changed'

/**
 * Watch for anything that can make a projects list stale: changes of the user's own workspace and
 * committed changes of any design repository. The pings carry no project data — the caller re-reads
 * what it shows through the REST API — only the clients whose requests caused them.
 */
export function subscribeWorkspaceChanges(onChange: (ping: ChangePing) => void): TopicSubscription {
    const onBody = (body: string) => onChange(parseChangePing(body, 'workspace'))
    const own = subscribeTopic(WORKSPACE_CHANGED_TOPIC, onBody)
    const everyone = subscribeTopic(PROJECTS_CHANGED_TOPIC, onBody)
    return {
        unsubscribe: () => {
            own.unsubscribe()
            everyone.unsubscribe()
        },
    }
}
