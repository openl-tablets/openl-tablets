import React from 'react'
import { useEventProject, type EventProjectDetail } from 'hooks'
import { ExportProjectModal } from 'containers/projects/ExportProjectModal'

/** Detail passed from the legacy JSF editor shell via the {@code openExportProjectModal} event. */
export interface ExportProjectModalDetail extends EventProjectDetail {
    /**
     * Project-relative path of a single file to export, e.g. the rules file of the open module. Omit it
     * to export the whole project.
     */
    filePath?: string
}

/**
 * Opens the shared Export dialog for the project the legacy editor has open, or for one file of it.
 *
 * The dialog is the one the Projects tab uses: it downloads the workspace copy or any earlier revision.
 *
 * @example globalThis.dispatchEvent(new CustomEvent('openExportProjectModal', {detail: {projectId}}))
 */
export const ExportProjectModalHost: React.FC = () => {
    const { detail, project, close } = useEventProject<ExportProjectModalDetail>(
        'openExportProjectModal',
        'repository:browser.export_dialog.load_failed'
    )

    return (
        <ExportProjectModal
            filePath={detail?.filePath}
            onClose={close}
            open={project !== null}
            project={project}
        />
    )
}
