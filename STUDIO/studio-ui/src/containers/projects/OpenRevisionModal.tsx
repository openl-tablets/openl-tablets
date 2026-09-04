import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, Modal, notification, Select } from 'antd'
import { createStyles } from 'antd-style'
import { errorMessage } from '../../utils/errorMessage'
import { isProjectModifiedConflict, openProjectRevision } from '../../services/repositories'
import { ProjectStatus } from '../../constants/project'
import { FieldRow } from '../../components/FieldRow'
import { DiscardChangesModal } from '../DiscardChangesModal'
import type { Project } from '../../types/projects'
import { useProjectRevisions } from './revisions'

const LABEL_WIDTH = 150

const useStyles = createStyles(({ css, token }) => ({
    note: css`
        margin: 16px 0 0;
        color: ${token.colorTextSecondary};
        font-size: 13px;
    `,
}))

interface OpenRevisionModalProps {
    open: boolean
    project: Project | null
    onClose: () => void
    onOpened: () => void
}

/**
 * Opens the project on one of its earlier revisions.
 *
 * Revisions are offered the way a business user reads them — who changed the project and when. Opening
 * replaces the workspace copy with that revision, so a project with unsaved changes warns first: pressing
 * Open then discards them.
 */
export const OpenRevisionModal = ({ open, project, onClose, onOpened }: OpenRevisionModalProps) => {
    const { styles } = useStyles()
    const { t } = useTranslation('repository')
    const [chosen, setChosen] = useState<string | undefined>(undefined)
    const [submitting, setSubmitting] = useState(false)
    const [discardOpen, setDiscardOpen] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const modified = project?.status === ProjectStatus.Editing
    const { revisions, options, error: revisionsError } = useProjectRevisions(project, open)
    // The latest revision is the one a user reaches for most often.
    const revision = chosen ?? revisions?.[0]?.revisionNo

    // The dialog stays mounted between openings; a revision picked for one project must not carry over.
    useEffect(() => {
        setChosen(undefined)
        setError(null)
    }, [open, project])

    const submit = async (discardChanges = false) => {
        if (!project || !revision) {
            setError(t('browser.open_revision_dialog.revision_required'))
            return
        }
        setSubmitting(true)
        setError(null)
        try {
            await openProjectRevision(project.id, revision, discardChanges ? { discardChanges: true } : {})
            notification.success({ title: t('browser.open_revision_dialog.success') })
            onOpened()
            onClose()
        } catch (e) {
            // Unsaved changes are never dropped on the first click: the user confirms discarding them,
            // exactly as switching a branch or opening a revision from the History tab does.
            if (!discardChanges && isProjectModifiedConflict(e)) {
                setDiscardOpen(true)
                return
            }
            setError(errorMessage(e))
        } finally {
            setSubmitting(false)
        }
    }

    const dialog = (
        <Modal
            destroyOnHidden
            confirmLoading={submitting}
            okButtonProps={{ 'data-testid': 'open-revision-submit', disabled: !revision }}
            okText={t('browser.open_revision_dialog.submit')}
            onCancel={onClose}
            onOk={() => void submit()}
            open={open}
            title={t('browser.open_revision_dialog.title', { name: project?.name })}
        >
            {(error ?? revisionsError) && (
                <Alert
                    showIcon
                    data-testid="open-revision-error"
                    style={{ marginBottom: 12 }}
                    title={error ?? revisionsError}
                    type="error"
                />
            )}
            {modified && (
                <Alert
                    showIcon
                    data-testid="open-revision-modified"
                    style={{ marginBottom: 12 }}
                    title={t('browser.open_revision_dialog.modified_warning')}
                    type="warning"
                />
            )}
            <FieldRow required label={t('browser.open_revision_dialog.revision')} labelWidth={LABEL_WIDTH}>
                <Select
                    data-testid="open-revision-select"
                    loading={revisions === null}
                    onChange={value => setChosen(value as string)}
                    options={options}
                    // A revision reads whole in the list: the field is narrower than the label it holds.
                    popupMatchSelectWidth={false}
                    style={{ width: '100%' }}
                    value={revision}
                />
            </FieldRow>
            <p className={styles.note}>{t('browser.open_revision_dialog.note')}</p>
        </Modal>
    )

    return (
        <>
            {dialog}
            <DiscardChangesModal
                cancelButtonTestId="open-revision-discard-cancel"
                confirmButtonTestId="open-revision-discard-confirm"
                confirmText={t('browser.open_revision_discard_confirm_unsafe')}
                onCancel={() => setDiscardOpen(false)}
                open={discardOpen}
                warning={t('browser.open_revision_discard_warning')}
                onConfirm={() => {
                    setDiscardOpen(false)
                    void submit(true)
                }}
            />
        </>
    )
}
