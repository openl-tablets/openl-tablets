/**
 * Bridge that exposes the full-screen loading overlay to the legacy JSF/RichFaces
 * pages running in the same document. The bridge is a side-effect import — pulling
 * it into the app sets {@code globalThis.openl.loader} once, so legacy inline
 * scripts block the page during AJAX requests in the same style as the React screens.
 *
 * <p>Calls may overlap: the overlay stays open until every {@code show()} is paired
 * with {@code hide()}. An unpaired {@code hide()} is ignored.
 */
import { useAppStore } from '../store'

/** Shape published to {@code globalThis.openl.loader} for legacy JSF callers. */
export interface LoaderBridge {
    /** Opens the overlay; each call must be paired with {@code hide()}. */
    show(): void
    /** Closes the overlay once every {@code show()} is paired. */
    hide(): void
}

const bridge: LoaderBridge = {
    show: () => useAppStore.getState().showLoader(),
    hide: () => useAppStore.getState().hideLoader(),
}

globalThis.openl = globalThis.openl ?? {}
globalThis.openl.loader = bridge
