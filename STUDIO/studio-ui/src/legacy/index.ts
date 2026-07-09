/**
 * Installs every bridge published to the legacy JSF pages, then announces readiness
 * with a single {@code openl:ready} event. The React entry point is a deferred ESM
 * module, so legacy inline scripts that run on first page paint wait for this event
 * when {@code globalThis.openl} is not populated yet:
 *
 * <pre>
 *     if (globalThis.openl &amp;&amp; globalThis.openl.notification) { cb(); return; }
 *     document.addEventListener('openl:ready', cb, { once: true });
 * </pre>
 */
import './notificationBridge'
import './projectStatusBridge'

document.dispatchEvent(new CustomEvent('openl:ready'))
