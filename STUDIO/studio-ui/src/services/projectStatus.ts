import apiCall from './apiCall'
import { subscribeTopic, type TopicSubscription } from './stompTopic'

/**
 * Compilation severity values produced by the backend (`@JsonProperty` on the enum, but
 * the legacy compile API serialised them as the enum name, so we accept both forms).
 */
export type ProjectStatusSeverity = 'INFO' | 'WARN' | 'ERROR'

/**
 * Compile-state values from the {@code CompileState} enum on the backend
 * (`@JsonProperty` -> lower-case).
 */
export type ProjectCompileState = 'idle' | 'compiling' | 'ok' | 'warnings' | 'errors'

export interface ProjectStatusMessage {
    id: number
    summary: string
    severity: ProjectStatusSeverity
}

/**
 * Origin of a compilation message. Mirrors the polymorphic backend
 * {@code MessageSource} (discriminator: {@code type}).
 *  - {@code module}: workbook-level location (no specific table)
 *  - {@code table}: specific table cell, with id/name/module/cell
 */
export interface ProjectStatusModuleMessageSource {
    type: 'module'
    name?: string
}

export interface ProjectStatusTableMessageSource {
    type: 'table'
    id?: string
    name?: string
    module?: string
    cell?: string
}

export type ProjectStatusMessageSource =
    | ProjectStatusModuleMessageSource
    | ProjectStatusTableMessageSource

/**
 * Compilation message enriched with origin and stack-trace availability.
 * The {@code source} fields ({@code id}, {@code summary}, {@code severity}) are
 * inlined by Jackson's {@code @JsonUnwrapped} on the backend, so they appear at the
 * top level alongside {@code location} and {@code stacktrace}.
 */
export interface ProjectStatusDetailedMessage extends ProjectStatusMessage {
    stacktrace: boolean
    location?: ProjectStatusMessageSource
}

export interface ProjectStatusCompilationMessages {
    /** The detailed message list. Populated for the project detail/status views; omitted for list rows, which show only the counts. */
    items?: ProjectStatusDetailedMessage[]
    total: number
    errors: number
    warnings: number
}

export interface ProjectStatusCompilationModules {
    /** Names of compiled modules. Populated for the project detail/status views; omitted for list rows, which show only the counts. */
    compiledModules?: string[]
    total: number
    compiled: number
}

export interface ProjectStatusCompilationTests {
    total: number
}

export interface ProjectStatusCompilation {
    messages?: ProjectStatusCompilationMessages
    modules?: ProjectStatusCompilationModules
    tests?: ProjectStatusCompilationTests
}

export type ProjectFileChangeType = 'added' | 'modified' | 'deleted'

export interface ProjectFileChange {
    path: string
    type: ProjectFileChangeType
}

export interface ProjectPendingChanges {
    total: number
    files: ProjectFileChange[]
}

export interface ProjectStatusUpdate {
    projectId: string
    branch?: string | null
    revision?: string
    compileState: ProjectCompileState
    compilation?: ProjectStatusCompilation
    pendingChanges?: ProjectPendingChanges
}

/** The one subscription handle shape — an alias of the shared topic multiplexer's. */
export type ProjectStatusSubscription = TopicSubscription

/**
 * Shape published to {@code window.openl.projectStatus} for legacy JSF callers.
 */
export interface ProjectStatusBridge {
    fetch(projectId: string): Promise<ProjectStatusUpdate>
    subscribe(
        projectId: string,
        branch: string | null,
        onUpdate: (status: ProjectStatusUpdate) => void
    ): ProjectStatusSubscription
}


/**
 * Build the user-scoped STOMP destination matching
 * {@code ProjectSocketNotificationService.notifyProjectStatus} on the backend:
 *   - with branch:  /user/topic/projects/{urlEncoded(projectId)}/branches/{urlEncoded(branch)}/status
 *   - without:      /user/topic/projects/{urlEncoded(projectId)}/status
 */
function buildDestination(projectId: string, branch: string | null | undefined): string {
    const base = `/user/topic/projects/${encodeURIComponent(projectId)}`
    if (branch === null || branch === undefined || branch === '') {
        return `${base}/status`
    }
    return `${base}/branches/${encodeURIComponent(branch)}/status`
}

/**
 * Concurrent fetches for the same project share a network round trip. The entry is
 * cleared as soon as the request settles so subsequent fetches still hit the network.
 */
const inflightFetches = new Map<string, Promise<ProjectStatusUpdate>>()

/**
 * One-shot fetch of the current project status. Used to bootstrap the UI before
 * subscribing — STOMP only delivers transitions, so without a bootstrap a subscriber
 * landing after compilation has already finished would see nothing.
 *
 * Throws on non-2xx; lets the caller decide how to render failures.
 */
export function fetchProjectStatus(projectId: string): Promise<ProjectStatusUpdate> {
    const existing = inflightFetches.get(projectId)
    if (existing) {
        return existing
    }
    const promise = fetchSingleProjectStatus(projectId).finally(() => {
        inflightFetches.delete(projectId)
    })
    inflightFetches.set(projectId, promise)
    return promise
}

function fetchSingleProjectStatus(projectId: string): Promise<ProjectStatusUpdate> {
    // Background poll: throw on error so callers can decide how to render, and suppress
    // the global "show login / forbidden / not-found / server error" page redirects —
    // a stale status fetch shouldn't take over the whole UI.
    return apiCall(
        `/projects/${encodeURIComponent(projectId)}/status?branch=`,
        {
            method: 'GET',
            credentials: 'same-origin',
            headers: { Accept: 'application/json' },
        },
        { throwError: true, suppressErrorPages: true }
    ) as Promise<ProjectStatusUpdate>
}

/**
 * Subscribe to project status updates pushed by the backend over WebSocket. The
 * callback receives the full {@code ProjectStatusViewModel} for every transition
 * (compile-cycle start, per-module progress, terminal state).
 *
 * <p>Multiple legacy panels watch the same destination (testPanel, problems, table — all watching
 * the open project); the shared topic subscription fans the messages out locally.
 *
 * <p>Caller responsibilities:
 *   - call {@link fetchProjectStatus} first to render the current state — STOMP only
 *     pushes transitions and does not replay state on subscribe.
 *   - call {@code subscription.unsubscribe()} when navigating away to avoid leaking
 *     handlers across project / module changes.
 */
export function subscribeProjectStatus(
    projectId: string,
    branch: string | null,
    onUpdate: (status: ProjectStatusUpdate) => void
): ProjectStatusSubscription {
    return subscribeTopic(buildDestination(projectId, branch), statusBodyHandler(onUpdate))
}

/**
 * Whether a status the channel pushed still outranks the one a read carries.
 *
 * A push and a read answer the same question from two sides, and the fresher one wins: a push beats
 * a read that started before it arrived, and gives way to a read started after it. Both screens ask
 * this — the list about each row, the project page about the one project — so they ask it here.
 *
 * @param pushedAt       when the pushed status arrived
 * @param readStartedAt  when the read carrying the other status started
 */
export function isPushFresherThanRead(pushedAt: number, readStartedAt: number): boolean {
    return pushedAt > readStartedAt
}

/**
 * The one status stream of the whole workspace, matching
 * {@code ProjectSocketNotificationService.notifyWorkspaceProjectStatus} on the backend.
 * Every update names its own project and branch, so a screen showing many projects — the projects
 * list — holds this single subscription and routes the updates itself, instead of one subscription
 * per row. The single-project screens keep {@link subscribeProjectStatus}.
 */
export function subscribeWorkspaceProjectStatuses(
    onUpdate: (status: ProjectStatusUpdate) => void
): ProjectStatusSubscription {
    return subscribeTopic('/user/topic/workspace/projects/status', statusBodyHandler(onUpdate))
}

function statusBodyHandler(onUpdate: (status: ProjectStatusUpdate) => void): (body: string) => void {
    return body => {
        let payload: ProjectStatusUpdate
        try {
            payload = JSON.parse(body) as ProjectStatusUpdate
        } catch {
            return
        }
        onUpdate(payload)
    }
}
