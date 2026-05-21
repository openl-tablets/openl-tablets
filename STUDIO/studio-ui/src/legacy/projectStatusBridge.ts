/**
 * Bridge that exposes the React-side project-status API to the legacy JSF/RichFaces
 * pages running in the same document. The bridge is a side-effect import — pulling
 * it into the app sets {@code window.openl.projectStatus} once, and legacy inline
 * scripts can then call it via the {@code openl:ready} event.
 *
 * <p>Legacy callers should:
 *   <pre>
 *     function whenReady(cb) {
 *         if (window.openl &amp;&amp; window.openl.projectStatus) { cb(); return; }
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

window.openl = window.openl ?? {}
window.openl.projectStatus = bridge

document.dispatchEvent(new CustomEvent('openl:ready'))

export {}
