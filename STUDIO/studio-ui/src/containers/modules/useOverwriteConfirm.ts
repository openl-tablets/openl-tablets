import { useTranslation } from 'react-i18next'
import type { Project } from '../../types/projects'
import { type ConfirmBefore, useConfirmBefore } from './useConfirmBefore'

/** Runs a write, asking first where it would save over a revision the reader has not seen. */
export type ConfirmWrite = ConfirmBefore

/**
 * Asks before a write to a project opened on an older revision.
 *
 * A revision is opened to be read, and the copy in the workspace is then that revision rather than the latest.
 * Saving it puts it back over everything committed since, so the reader is told what they are about to do
 * before the first write and not after it. Once something has been written the project is modified, the
 * question is settled, and nothing asks it again.
 *
 * A project opened on the latest revision — and one the reader only reads — is never asked about.
 */
export const useOverwriteConfirm = (project: Project | null | undefined): ConfirmWrite => {
    const { t } = useTranslation('repository')
    return useConfirmBefore(project?.overwritesNewerRevision === true, {
        title: t('browser.module.overwrite_revision'),
        content: t('browser.module.overwrite_revision_body'),
        okText: t('browser.module.overwrite_revision_ok'),
    })
}
