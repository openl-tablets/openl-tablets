import React, { useEffect, useState } from 'react'
import { useEventProject, type EventProjectDetail } from 'hooks'
import { LOCAL_LOAD_API_OPTIONS, notifyLoadFailure } from 'services/apiCall'
import { getDesignRepositories } from 'services/repositories'
import { useAppStore } from 'store'
import { creatableRepositories } from 'utils/repositoryFeatures'
import { CopyProjectModal } from 'containers/projects/CopyProjectModal'
import i18next from 'i18next'
import type { Repository } from 'types/repositories'

/** Detail passed from the legacy JSF editor shell via the {@code openCopyProjectModal} event. */
export interface CopyProjectModalDetail extends EventProjectDetail {
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
 * Both reads start together, hold the loading overlay together, and the dialog waits for both: opening on
 * the project alone would show the copy half as unavailable, then rebuild the form under the user once the
 * repositories arrive.
 *
 * @example globalThis.dispatchEvent(new CustomEvent('openCopyProjectModal', {detail: {projectId}}))
 */
export const CopyProjectModalHost: React.FC = () => {
    const { detail, project, close } = useEventProject<CopyProjectModalDetail>(
        'openCopyProjectModal',
        'repository:browser.copy_dialog.load_failed'
    )
    const [repositories, setRepositories] = useState<Repository[] | null>(null)
    const showLoader = useAppStore(state => state.showLoader)
    const hideLoader = useAppStore(state => state.hideLoader)
    const projectId = detail?.projectId

    useEffect(() => {
        // Dropped before the read for the same reason the project is: what a copy may target depends on
        // the project, so last time's answer must not outlive it.
        setRepositories(null)
        if (!projectId) {
            return
        }
        let active = true
        // The overlay counts its holders, so this read keeps it up for as long as it runs even once the
        // project has arrived: it is what the dialog is still waiting for, and the click that would start
        // the whole thing again lands on the overlay rather than on a page that looks idle.
        showLoader()
        getDesignRepositories(LOCAL_LOAD_API_OPTIONS)
            .then(loaded => {
                if (active) {
                    setRepositories(creatableRepositories(loaded))
                }
            })
            // The dialog can still cut a branch without the list, so a failed read costs the copy half
            // rather than the whole dialog — but it is said out loud, or the copy half looks simply absent.
            .catch(error => {
                if (active) {
                    setRepositories([])
                    notifyLoadFailure(i18next.t('repository:browser.copy_dialog.repositories_failed'), error)
                }
            })
            .finally(hideLoader)
        return () => {
            active = false
        }
    }, [projectId, showLoader, hideLoader])

    return (
        <CopyProjectModal
            onClose={close}
            onCopied={() => detail?.onSuccess?.()}
            open={project !== null && repositories !== null}
            project={project}
            repositories={repositories ?? []}
        />
    )
}
