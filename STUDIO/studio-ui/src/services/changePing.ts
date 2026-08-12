import { CLIENT_ID } from './clientId'

/**
 * What a change ping stands for. Matches the bodies `ProjectSocketNotificationService` sends: the
 * workspace ping and the broadcast carry origins alone, the per-project one carries files too.
 */
export interface ChangePing {
    /**
     * The clients whose requests caused the ping. Empty when the change was made outside a request —
     * the workspace files watcher, a repository poll — so nothing may be read as an echo.
     */
    origins: string[]
    /**
     * The project-relative files the change touched, when the backend knows them — a folder stands
     * for anything under it. Empty means a project-wide change or unknown files.
     */
    files: string[]
}

/** A ping whose body is missing or malformed reads as a change of unknown files and unknown origin. */
export function parseChangePing(body: string): ChangePing {
    try {
        const payload = JSON.parse(body) as { files?: unknown, origins?: unknown }
        return { files: strings(payload.files), origins: strings(payload.origins) }
    } catch {
        return { files: [], origins: []}
    }
}

/**
 * True when this tab caused everything the ping stands for: it has already read the change, and
 * re-reading would only repeat that.
 *
 * A ping coalesces the changes of a moment, so it can name several clients. One foreign origin is
 * enough to deliver it — otherwise another session's change would be dropped along with the echo —
 * and a ping naming no origin at all is never anyone's own.
 */
export function isOwnEcho(ping: ChangePing): boolean {
    return ping.origins.length > 0 && ping.origins.every(origin => origin === CLIENT_ID)
}

const strings = (value: unknown): string[] =>
    Array.isArray(value) ? value.filter((item: unknown): item is string => typeof item === 'string') : []
