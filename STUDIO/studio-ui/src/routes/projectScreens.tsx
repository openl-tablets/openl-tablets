import React, { type ComponentType } from 'react'

/**
 * A screen whose code is split from the rest of the application.
 *
 * Until its code is there it suspends, as {@code React.lazy} does, and the placeholder of the route shows. Once the
 * code is loaded, on the first use of the screen or ahead of it, the screen renders at once: a lazy component would
 * suspend for a moment on its first use even then, and the placeholder would blink between two screens.
 */
const splitScreen = <P extends object>(load: () => Promise<ComponentType<P>>) => {
    let loaded: ComponentType<P> | undefined
    const preload = () => load().then(screen => {
        loaded = screen
        return screen
    })
    const Lazy = React.lazy(() => preload().then(screen => ({ default: screen })))
    const Screen = (props: P) => {
        const Loaded = loaded
        return Loaded === undefined ? <Lazy {...props} /> : <Loaded {...props} />
    }
    return { Screen, preload }
}

const projectWorkspace = splitScreen(() => import('../containers/ProjectWorkspace').then(module => module.ProjectWorkspace))
const moduleWorkspace = splitScreen(() => import('../containers/ModuleWorkspace').then(module => module.ModuleWorkspace))

/** The page of a project: its Overview and the rest of its tabs. */
export const ProjectWorkspaceScreen = projectWorkspace.Screen

/** The editor of a module of a project. */
export const ModuleWorkspaceScreen = moduleWorkspace.Screen

/**
 * Loads the code of the page of a project and of its editor ahead of their first use, so moving to either one paints
 * it at once. The list of the projects stays light: it asks for them once it is on screen.
 *
 * A load that fails here is no failure of anything the reader did: the first use of the screen loads it again, and
 * its route shows what went wrong then.
 */
export const preloadProjectScreens = (): void => {
    projectWorkspace.preload().catch(() => undefined)
    moduleWorkspace.preload().catch(() => undefined)
}

/** How long a browser with no idle callback waits before it loads the screens ahead: past the first reads of the list. */
const PRELOAD_DELAY_MS = 1000

/**
 * Loads the code of the page of a project and of its editor once the browser has nothing else to do, so the list of
 * the projects draws and reads first.
 *
 * @returns what cancels the load while it has not started, for the effect that asks for it to clean up with
 */
export const preloadProjectScreensWhenIdle = (): (() => void) => {
    if (typeof window.requestIdleCallback === 'function') {
        const handle = window.requestIdleCallback(preloadProjectScreens)
        return () => window.cancelIdleCallback(handle)
    }
    const timer = window.setTimeout(preloadProjectScreens, PRELOAD_DELAY_MS)
    return () => window.clearTimeout(timer)
}
