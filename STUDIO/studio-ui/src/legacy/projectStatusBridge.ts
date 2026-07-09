/**
 * Bridge that exposes the React-side project-status API to the legacy JSF/RichFaces
 * pages running in the same document. The bridge is a side-effect import — pulling
 * it into the app sets {@code globalThis.openl.projectStatus} once. Readiness is
 * announced by the {@code openl:ready} event dispatched from {@code ./index}.
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
