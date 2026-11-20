import { subscribeTopic, type TopicSubscription } from './stompTopic'
import type { SubscriptionStarted } from './websocket'

/**
 * Compilation severity values produced by the backend (`@JsonProperty` on the enum, but
 * the legacy compile API serialised them as the enum name, so we accept both forms).
 */
export type ProjectStatusSeverity = 'INFO' | 'WARN' | 'ERROR'

/**
 * Compile-state values from the {@code CompileState} enum on the backend
 * (`@JsonProperty` -> lower-case).
 */
export type ProjectCompileState = 'idle' | 'compiling' | 'ok' | 'warnings' | 'errors' | 'cancelled'

/**
 * Whether the project's compilation ran to its end, however it ended.
 *
 * What spans the whole project — which tables a test covers, what every module raised — is known only then. A
 * compilation not started, still running, or stopped by the reader has not answered for the modules it never
 * reached.
 */
export const isCompiled = (state: ProjectCompileState): boolean =>
    state === 'ok' || state === 'warnings' || state === 'errors'

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
 *  - {@code file}: a line of another file of the project, such as a localization file
 */
export interface ProjectStatusModuleMessageSource {
    type: 'module'
    name?: string
    /** The project the module belongs to, which is not always the one compiled: a dependency is a project too */
    projectId?: string
    project?: string
    /**
     * The path in the project of the file the message points to, such as a Markdown file of a module folder that
     * holds no table. Absent when the message points to no line of a file.
     */
    path?: string
    /** The line of that file, counted from 1. */
    line?: number
    /**
     * Where the message is in that file, for an editor of the file to mark: the text of the line of a Markdown file
     * that breaks the format.
     */
    places?: FilePlace[]
}

export interface ProjectStatusTableMessageSource {
    type: 'table'
    id?: string
    name?: string
    module?: string
    cell?: string
    /**
     * First character of the cell's text the message is about, which a screen marks so the reader finds it
     * among the rest. Absent when the message is about no part of the cell in particular.
     */
    start?: number
    /** Character of the cell's text after the last one the message is about. */
    end?: number
    /**
     * The path in the project of the Markdown file the table is written in, for a message about a place that breaks
     * the format of the file: only the text of the file tells what is wrong, so a screen sends the reader there rather
     * than to the cell. Absent for any other message.
     */
    path?: string
    /** The line of that file, counted from 1. */
    line?: number
    /** Where the message is in that file, for an editor of the file to mark. */
    places?: FilePlace[]
    /** The project the table belongs to, which is not always the one compiled: a dependency is a project too */
    projectId?: string
    project?: string
}

/** A piece of a text, from the index of its first character to the index after its last one. */
export interface TextSpot {
    start: number
    end: number
}

/**
 * A place of a text file, from a line and a column up to another, the end left out. Lines count from 1, columns
 * count the characters of a line from 0, whatever line breaks the file is written with.
 */
export interface FilePlace {
    line: number
    column: number
    endLine: number
    endColumn: number
}

export interface ProjectStatusLocalizationMessageSource {
    type: 'localization'
    /** The path of the file, from the project folder. */
    path?: string
    line?: number
    /** The key of the localization file the message is about; absent for the file as a whole. */
    key?: string
    /** The language of the localization file, empty for the default one. */
    language?: string
    /**
     * The pieces of the text of the key the message is about, in the order of the text, which a screen marks so the
     * reader finds them among the rest. Told only when the positions in the text are asked for, and absent when the
     * message is about no part of the text in particular.
     */
    spots?: TextSpot[]
    /**
     * Where the message is in the file, for an editor of the file to mark: each piece of the text where it is
     * written, or the key for a message about the text as a whole. Told only when the positions in the file are asked
     * for, as the compilation status tells them, and absent for a message about the whole file.
     */
    places?: FilePlace[]
    /** A file message is always of the project being compiled. */
    projectId?: undefined
}

export type ProjectStatusMessageSource =
    | ProjectStatusModuleMessageSource
    | ProjectStatusTableMessageSource
    | ProjectStatusLocalizationMessageSource

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

interface ProjectPendingChanges {
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
    /**
     * Set when a workbook the open module is compiled with was written to while automatic compilation is off, so
     * what the compiler says about it is what it said before the write, and the reader is the one who asks for it to
     * be compiled.
     */
    manualCompileNeeded?: boolean
    /**
     * Set when another program, such as Excel or an IDE, changed a file the open module is compiled from since the
     * compilation read it: what the screen shows of the file is what it held before, until the reader refreshes.
     */
    changedOnDisk?: boolean
    /**
     * Orders the statuses of the server: one read later has a larger sequence, whether it was read over REST or
     * pushed over the socket.
     */
    sequence: number
}

/** The one subscription handle shape — an alias of the shared topic multiplexer's. */
export type ProjectStatusSubscription = TopicSubscription

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
 * Subscribe to project status updates pushed by the backend over WebSocket. The
 * callback receives the full {@code ProjectStatusViewModel} for every transition
 * (compile-cycle start, per-module progress, terminal state).
 *
 * <p>Multiple legacy panels watch the same destination (testPanel, problems, table — all watching
 * the open project); the shared topic subscription fans the messages out locally.
 *
 * <p>Caller responsibilities:
 *   - read the current state — {@code getProject} with the {@code status} expansion — STOMP only pushes
 *     transitions and does not replay state on subscribe. {@code onStarted} tells the number the subscription
 *     started at, on every start and reconnect; a status read before it ({@link isReadBefore}) is read again.
 *   - keep the fresher status of what is read and what is pushed ({@link fresherStatus}).
 *   - call {@code subscription.unsubscribe()} when navigating away to avoid leaking
 *     handlers across project / module changes.
 */
export function subscribeProjectStatus(
    projectId: string,
    branch: string | null,
    onUpdate: (status: ProjectStatusUpdate) => void,
    onStarted?: SubscriptionStarted
): ProjectStatusSubscription {
    return subscribeTopic(buildDestination(projectId, branch), statusBodyHandler(onUpdate), onStarted)
}

/**
 * The fresher of two statuses of one project: the one the server read later, whether it was read or pushed.
 * A push and a read answer the same question from two sides, and arrive in no particular order.
 * Null stands for no status at all, and gives way to any status.
 */
export function fresherStatus(one: ProjectStatusUpdate | null, other: ProjectStatusUpdate): ProjectStatusUpdate
export function fresherStatus(
    one: ProjectStatusUpdate | null,
    other: ProjectStatusUpdate | null
): ProjectStatusUpdate | null
export function fresherStatus(
    one: ProjectStatusUpdate | null,
    other: ProjectStatusUpdate | null
): ProjectStatusUpdate | null {
    if (one === null || other === null) {
        return one ?? other
    }
    return other.sequence > one.sequence ? other : one
}

/**
 * Whether two statuses say the same, whenever the server read them: a re-read that changed nothing keeps what a
 * screen derived from the status it shows.
 */
export function isSameStatus(one: ProjectStatusUpdate | null, other: ProjectStatusUpdate | null): boolean {
    if (one === other) {
        return true
    }
    return one !== null && other !== null
        && JSON.stringify({ ...one, sequence: 0 }) === JSON.stringify({ ...other, sequence: 0 })
}

/**
 * Whether the status was read before a subscription started at the given number. A change told in between reached
 * nobody, so the status may be out of date with nothing on its way to correct it, and is read again.
 */
export function isReadBefore(status: ProjectStatusUpdate, startedAt: number): boolean {
    return status.sequence < startedAt
}

/**
 * The one status stream of the whole workspace, matching
 * {@code ProjectSocketNotificationService.notifyWorkspaceProjectStatus} on the backend.
 * Every update names its own project and branch, so a screen showing many projects — the projects
 * list — holds this single subscription and routes the updates itself, instead of one subscription
 * per row. The single-project screens keep {@link subscribeProjectStatus}.
 */
export function subscribeWorkspaceProjectStatuses(
    onUpdate: (status: ProjectStatusUpdate) => void,
    onStarted?: SubscriptionStarted
): ProjectStatusSubscription {
    return subscribeTopic('/user/topic/workspace/projects/status', statusBodyHandler(onUpdate), onStarted)
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
