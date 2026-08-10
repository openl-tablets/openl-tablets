import React, { useCallback, useEffect, useRef, useState } from 'react'
import { notification } from 'antd'
import { useTranslation } from 'react-i18next'
import { useGlobalEvents } from 'hooks'
import { EmptyError, LOCAL_LOAD_API_OPTIONS } from 'services/apiCall'
import { getDesignRepositories, getProject } from 'services/repositories'
import { useAppStore } from 'store'
import { errorMessage } from 'utils/errorMessage'
import { creatableRepositories } from 'utils/repositoryFeatures'
import { CopyProjectModal } from 'containers/projects/CopyProjectModal'
import type { Project } from 'types/projects'
import type { Repository } from 'types/repositories'

/** Detail passed from the legacy JSF editor shell via the {@code openCopyProjectModal} event. */
export interface CopyProjectModalDetail {
    projectId: string
    /** Runs after a successful copy, e.g. to reload the editor page. */
    onSuccess?: () => void
}

/**
 * Opens the shared Copy dialog for the project the legacy editor has open.
 *
 * The dialog is the one the Projects tab uses. It works off the project itself and the repositories a copy
 * may be created in, so only the project id travels on the event and both are read here — the editor page
 * stays free of REST calls of its own.
 *
 * A project that cannot be read leaves the dialog closed and reports why, rather than opening it empty.
 *
 * @example globalThis.dispatchEvent(new CustomEvent('openCopyProjectModal', {detail: {projectId}}))
 */
export const CopyProjectModalHost: React.FC = () => {
    const { t } = useTranslation()
    const { detail } = useGlobalEvents<CopyProjectModalDetail>('openCopyProjectModal')
    const showLoader = useAppStore(state => state.showLoader)
    const hideLoader = useAppStore(state => state.hideLoader)
    const [project, setProject] = useState<Project | null>(null)
    const [repositories, setRepositories] = useState<Repository[]>([])

    const close = useCallback(() => {
        globalThis.dispatchEvent(new CustomEvent('openCopyProjectModal', { detail: null }))
    }, [])

    // Read inside the load and never depended on: a language change rebinds `t`, and re-running the load
    // would hand the dialog fresh objects, which blanks the form the user is filling in.
    const translate = useRef(t)
    translate.current = t

    /**
     * Reports a failed read. An expired session is not reported: the request layer answers it by asking for
     * a new login, and its error carries no message, so a toast would only cover that prompt with a blank one.
     */
    const report = useCallback((messageKey: string, error: unknown) => {
        if (error instanceof EmptyError) {
            return
        }
        notification.error({ title: translate.current(messageKey), description: errorMessage(error) })
    }, [])

    useEffect(() => {
        const projectId = detail?.projectId
        if (!projectId) {
            setProject(null)
            return
        }
        let active = true
        // The reads take as long as the design repository does, and the legacy page has no spinner of its
        // own, so the shared overlay both shows the work and blocks a second click from starting it again.
        showLoader()
        Promise.all([
            getProject(projectId, {}, LOCAL_LOAD_API_OPTIONS),
            // The dialog can still cut a branch without the list, so a failed read costs the copy half
            // rather than the whole dialog — but it is said out loud, or the copy half looks simply absent.
            getDesignRepositories(LOCAL_LOAD_API_OPTIONS).catch(error => {
                report('repository:browser.copy_dialog.repositories_failed', error)
                return [] as Repository[]
            }),
        ])
            .then(([loadedProject, loadedRepositories]) => {
                if (!active) {
                    return
                }
                setRepositories(creatableRepositories(loadedRepositories))
                setProject(loadedProject)
            })
            .catch(error => {
                if (active) {
                    report('repository:browser.copy_dialog.load_failed', error)
                    close()
                }
            })
            .finally(hideLoader)
        return () => {
            active = false
        }
    }, [detail, close, report, showLoader, hideLoader])

    return (
        <CopyProjectModal
            onClose={close}
            onCopied={() => detail?.onSuccess?.()}
            open={project !== null}
            project={project}
            repositories={repositories}
        />
    )
}
