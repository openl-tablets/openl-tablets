/**
 * Bridge that exposes the React-side project-status API to the legacy JSF/RichFaces
 * pages running in the same document. The bridge is a side-effect import — pulling
 * it into the app sets {@code globalThis.openl.projectStatus} once, and legacy inline
 * scripts can then call it via the {@code openl:ready} event.
 *
 * <p>Legacy callers should:
 *   <pre>
 *     function whenReady(cb) {
 *         if (globalThis.openl &amp;&amp; globalThis.openl.projectStatus) { cb(); return; }
 *         document.addEventListener('openl:ready', cb, { once: true });
 *     }
 *   </pre>
 */
import {
    fetchProjectStatus,
    subscribeProjectStatus,
    type ProjectStatusBridge,
} from '../services/projectStatus'

const bridge: ProjectStatusBridge = {
    fetch: fetchProjectStatus,
    subscribe: subscribeProjectStatus,
}

globalThis.openl = globalThis.openl ?? {}
globalThis.openl.projectStatus = bridge

document.dispatchEvent(new CustomEvent('openl:ready'))

export {}
