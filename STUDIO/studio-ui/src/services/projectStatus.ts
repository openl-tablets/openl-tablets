import apiCall from './apiCall'
import { webSocketService, WebSocketMessage } from './websocket'

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
    items: ProjectStatusDetailedMessage[]
    total: number
    errors: number
    warnings: number
}

export interface ProjectStatusCompilationModules {
    compiledModules: string[]
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

export interface ProjectStatusUpdate {
    projectId: string
    branch?: string | null
    revision?: string
    compileState: ProjectCompileState
    compilation?: ProjectStatusCompilation
    // pendingChanges / lastModifiedBy are part of the payload but unused by the legacy UI.
}

export interface ProjectStatusSubscription {
    /**
     * Cancel the WebSocket subscription. Idempotent.
     */
    unsubscribe(): void
}

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
 * Concurrent fetches for the same project share one network round trip — several
 * legacy panels (testPanel, problems panel, …) bootstrap in the same load tick and
 * would otherwise hammer the backend with identical requests. The entry is cleared
 * as soon as the request settles so subsequent fetches still hit the network.
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
    // Background poll: throw on error so callers can decide how to render, and suppress
    // the global "show login / forbidden / not-found / server error" page redirects —
    // a stale status fetch shouldn't take over the whole UI.
    const promise = (apiCall(
        `/projects/${encodeURIComponent(projectId)}/status?branch=`,
        {
            method: 'GET',
            credentials: 'same-origin',
            headers: { Accept: 'application/json' },
        },
        { throwError: true, suppressErrorPages: true }
    ) as Promise<ProjectStatusUpdate>).finally(() => {
        inflightFetches.delete(projectId)
    })
    inflightFetches.set(projectId, promise)
    return promise
}

interface MultiplexedSubscription {
    listeners: Set<(status: ProjectStatusUpdate) => void>
    stompSubscriptionId: string
    teardownTimer: ReturnType<typeof setTimeout> | undefined
}

/**
 * Multiple legacy panels subscribe to the same destination (testPanel, problems,
 * table — all watching the open project). One STOMP subscription is held per
 * destination and fan-out happens locally.
 *
 * <p>The teardown is deferred by a short cooldown after the last listener detaches.
 * Each panel reload runs `unsubscribe()` synchronously and re-`subscribe()`s only
 * after its `fetch()` resolves — without the cooldown the listener count would
 * briefly hit zero, killing the STOMP subscription and dropping any in-flight
 * compilation events.
 */
const subscriptions = new Map<string, MultiplexedSubscription>()
const TEARDOWN_COOLDOWN_MS = 5000

/**
 * Subscribe to project status updates pushed by the backend over WebSocket. The
 * callback receives the full {@code ProjectStatusViewModel} for every transition
 * (compile-cycle start, per-module progress, terminal state).
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
    const destination = buildDestination(projectId, branch)

    let entry = subscriptions.get(destination)
    if (entry) {
        // A pending teardown from the last panel-reload tick — rescue it.
        if (entry.teardownTimer) {
            clearTimeout(entry.teardownTimer)
            entry.teardownTimer = undefined
        }
    } else {
        // Best-effort connect — `webSocketService.subscribe` queues subscriptions until
        // the STOMP client is connected, so we don't have to await this promise.
        void webSocketService.connect().catch(() => {
            // Connection failures surface via the service's own reconnect loop.
        })
        const newEntry: MultiplexedSubscription = {
            listeners: new Set(),
            stompSubscriptionId: '',
            teardownTimer: undefined,
        }
        newEntry.stompSubscriptionId = webSocketService.subscribe(
            destination,
            (message: WebSocketMessage) => {
                let payload: ProjectStatusUpdate
                try {
                    payload = JSON.parse(message.body) as ProjectStatusUpdate
                } catch {
                    return
                }
                newEntry.listeners.forEach((listener) => listener(payload))
            }
        )
        subscriptions.set(destination, newEntry)
        entry = newEntry
    }
    entry.listeners.add(onUpdate)

    let unsubscribed = false
    return {
        unsubscribe: () => {
            if (unsubscribed) return
            unsubscribed = true
            entry.listeners.delete(onUpdate)
            if (entry.listeners.size === 0) {
                entry.teardownTimer = setTimeout(() => {
                    // Re-check size — a `subscribe()` could have arrived during the cooldown.
                    if (entry.listeners.size === 0) {
                        webSocketService.unsubscribe(entry.stompSubscriptionId)
                        subscriptions.delete(destination)
                    }
                    entry.teardownTimer = undefined
                }, TEARDOWN_COOLDOWN_MS)
            }
        },
    }
}
