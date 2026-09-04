import React, { useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import { useGlobalEvents } from 'hooks'
import { UpdateFileModal } from './projects/UpdateFileModal'

/**
 * Detail passed from the legacy JSF editor shell via the {@code openUpdateModuleModal} event.
 */
export interface UpdateModuleModalDetail {
    projectId: string
    /** Project-relative path of the module's rules file, e.g. "rules/Main.xlsx". */
    modulePath: string
    /** Runs after a successful upload, e.g. to reload the editor page. */
    onSuccess?: () => void
}

const MODULE_EXTENSIONS = ['.xls', '.xlsx', '.xlsm']

/**
 * Replaces the open module's rules file, standing in for the RichFaces "Update module" popup. It is the
 * shared update dialog restricted to Excel files, mounted once in {@link DefaultLayout} and opened by the
 * legacy shell.
 *
 * @example globalThis.dispatchEvent(new CustomEvent('openUpdateModuleModal', {detail: {projectId, modulePath}}))
 */
export const UpdateModuleModal: React.FC = () => {
    const { t } = useTranslation()
    const { detail } = useGlobalEvents<UpdateModuleModalDetail>('openUpdateModuleModal')

    const close = useCallback(() => {
        globalThis.dispatchEvent(new CustomEvent('openUpdateModuleModal', { detail: null }))
    }, [])

    const open = !!(detail && Object.keys(detail).length > 0)
    if (!detail) {
        return null
    }

    return (
        <UpdateFileModal
            extensions={MODULE_EXTENSIONS}
            onClose={close}
            onUpdated={() => detail.onSuccess?.()}
            open={open}
            path={detail.modulePath}
            projectId={detail.projectId}
            title={t('project:update_module_modal.title')}
        />
    )
}
