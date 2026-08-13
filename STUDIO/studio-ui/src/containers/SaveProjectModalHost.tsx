import React from 'react'
import { useEventProject, type EventProjectDetail } from 'hooks'
import { SaveProjectModal } from 'containers/projects/SaveProjectModal'

/** Detail passed from the legacy JSF editor shell via the {@code openSaveProjectModal} event. */
export interface SaveProjectModalDetail extends EventProjectDetail {
    /** Runs after a successful save, e.g. to move the editor page to the saved project. */
    onSuccess?: () => void
}

/**
 * Opens the shared Save dialog for the project the legacy editor has open.
 *
 * The dialog is the one the Projects tab uses: it suggests the repository's commit comment, completes the
 * commit identity when the profile misses it, and hands a save-time merge conflict to the merge resolver.
 *
 * @example globalThis.dispatchEvent(new CustomEvent('openSaveProjectModal', {detail: {projectId}}))
 */
export const SaveProjectModalHost: React.FC = () => {
    const { detail, project, close } = useEventProject<SaveProjectModalDetail>(
        'openSaveProjectModal',
        'repository:browser.save_dialog.load_failed'
    )

    return (
        <SaveProjectModal
            onClose={close}
            onSaved={() => detail?.onSuccess?.()}
            open={project !== null}
            project={project}
        />
    )
}
