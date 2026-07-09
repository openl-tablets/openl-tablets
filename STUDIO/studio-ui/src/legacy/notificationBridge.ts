/**
 * Bridge that exposes Ant Design toast notifications to the legacy JSF/RichFaces
 * pages running in the same document. The bridge is a side-effect import — pulling
 * it into the app sets {@code globalThis.openl.notification} once, so legacy inline
 * scripts show messages in the same style as the React screens.
 *
 * <p>Content is rendered as plain text, so callers must pass raw, unescaped strings.
 */
import { notification } from 'antd'

/** Shape published to {@code globalThis.openl.notification} for legacy JSF callers. */
export interface NotificationBridge {
    /** Short-lived confirmation toast. */
    success(content: string): void
    /** Failure toast; stays open until the user closes it. */
    error(content: string): void
    /** Longer-lived informational toast. */
    info(content: string): void
}

// Durations (in seconds) mirror the retired jQuery toasts: 4s success, sticky error, 8s info.
const bridge: NotificationBridge = {
    success: content => notification.success({ title: content, duration: 4 }),
    error: content => notification.error({ title: content, duration: 0 }),
    info: content => notification.info({ title: content, duration: 8 }),
}

globalThis.openl = globalThis.openl ?? {}
globalThis.openl.notification = bridge
