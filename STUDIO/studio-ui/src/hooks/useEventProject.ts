import { useEffect, useState } from 'react'
import { useGlobalEvents } from './useGlobalEvents'
import { LOCAL_LOAD_API_OPTIONS, notifyLoadFailure } from '../services/apiCall'
import { getProject } from '../services/repositories'
import { useAppStore } from '../store'
import i18next from 'i18next'
import type { Project } from '../types/projects'

/** What every dialog opened from the legacy editor shell is given: the project to act on. */
export interface EventProjectDetail {
    projectId: string
}

export interface EventProject<D> {
    /** The event payload, or undefined while no dialog was asked for. */
    detail: D | undefined
    /** The project the dialog works on; null until it is read, and again once the dialog is closed. */
    project: Project | null
    /** Closes the dialog by clearing the event payload. */
    close: () => void
}

/**
 * Opens a shared project dialog on request from the legacy editor shell.
 *
 * The shell sends the project id alone and the project is read here, so the editor page keeps no REST
 * calls of its own and every dialog is handed the same project the Projects tab works with.
 *
 * A project that cannot be read leaves the dialog closed and reports why, rather than opening it empty.
 *
 * @param eventName the {@code CustomEvent} the shell dispatches to open the dialog
 * @param failureKey translation key of the message shown when the project cannot be read
 */
export const useEventProject = <D extends EventProjectDetail>(
    eventName: string,
    failureKey: string
): EventProject<D> => {
    const { detail, close } = useGlobalEvents<D>(eventName)
    const showLoader = useAppStore(state => state.showLoader)
    const hideLoader = useAppStore(state => state.hideLoader)
    const [project, setProject] = useState<Project | null>(null)

    // Keyed on the id rather than the payload: re-opening the dialog for the project already loaded — the
    // same project first as a whole and then by one of its modules — must not read it a second time.
    const projectId = detail?.projectId

    useEffect(() => {
        // Dropped before the read, not after it: a dialog asked for another project must not stay open on
        // the one it still holds, acting on the project the user has just navigated away from.
        setProject(null)
        if (!projectId) {
            return
        }
        let active = true
        // The read takes as long as the design repository does, and the legacy page has no spinner of its
        // own, so the shared overlay both shows the work and blocks a second click from starting it again.
        showLoader()
        getProject(projectId, {}, LOCAL_LOAD_API_OPTIONS)
            .then(loaded => {
                if (active) {
                    setProject(loaded)
                }
            })
            .catch(error => {
                if (active) {
                    notifyLoadFailure(i18next.t(failureKey), error)
                    close()
                }
            })
            .finally(hideLoader)
        return () => {
            active = false
        }
    }, [projectId, failureKey, close, showLoader, hideLoader])

    return { detail, project, close }
}
