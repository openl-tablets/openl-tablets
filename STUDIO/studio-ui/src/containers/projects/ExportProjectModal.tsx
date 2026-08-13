import { useEffect, useState, type ReactElement, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, Button, Modal, notification, Select } from 'antd'
import { downloadProject } from '../../services/repositories'
import { downloadFile, fileExistsAt } from '../../services/files'
import { ProjectStatus } from '../../constants/project'
import { FieldRow } from '../../components/FieldRow'
import type { Project } from '../../types/projects'
import { basename } from './projectPaths'
import { useProjectRevisions } from './revisions'

const LABEL_WIDTH = 150

/** Appends the dialog's own entry under a dropdown's list, without defining a component to do it. */
const withFooter = (menu: ReactElement, footer: ReactNode): ReactElement => (
    <>
        {menu}
        {footer}
    </>
)

/** Marks the workspace copy rather than a committed revision. */
const WORKSPACE = 'workspace'

interface ExportProjectModalProps {
    open: boolean
    project: Project | null
    onClose: () => void
    /**
     * Project-relative path of a single file to export instead of the whole project, e.g. a module's
     * rules file. The revision choice is the same either way.
     */
    filePath?: string | undefined
}

/**
 * Downloads the project, or a single file of it, from the workspace copy or from any earlier revision.
 *
 * The workspace copy leads the list while the project is open: for a project being edited it carries the
 * local changes, and for one merely opened it is the revision it was opened on.
 *
 * Exporting a file offers that file's own revisions rather than the project's, so every entry is one the
 * file can actually be read from.
 */
export const ExportProjectModal = ({ open, project, onClose, filePath }: ExportProjectModalProps) => {
    const { t } = useTranslation('repository')
    const [chosen, setChosen] = useState<string | undefined>(undefined)
    const [checking, setChecking] = useState(false)

    const editing = project?.status === ProjectStatus.Editing
    const opened = editing || project?.status === ProjectStatus.Opened
        || project?.status === ProjectStatus.ViewingVersion
    const { revisions, options: revisionOptions, error, hasMore, loadMore, loadingMore } = useProjectRevisions(project, open, filePath)
    // The workspace copy is what an open project exports; a closed one exports its latest revision — the
    // latest one still offered, since a revision that removed the file is not among them.
    const selected = chosen ?? (opened ? WORKSPACE : revisionOptions[0]?.value ?? WORKSPACE)

    // The dialog stays mounted between openings; a revision picked for one project must not carry over.
    useEffect(() => {
        setChosen(undefined)
    }, [open, project, filePath])

    const options = [
        // What the workspace holds depends on the project state, so the entry says which one it is.
        ...(opened
            ? [{
                value: WORKSPACE,
                label: editing ? t('browser.export_dialog.in_editing') : t('browser.export_dialog.viewing'),
            }]
            : []),
        ...revisionOptions,
    ]

    const submit = async () => {
        if (!project) {
            return
        }
        // With no revision to choose from — an unreadable or empty history — the latest state is exported.
        const revision = options.length > 0 && selected !== WORKSPACE ? selected : undefined
        if (!filePath) {
            downloadProject(project.id, revision)
            onClose()
            return
        }
        // A file added later is absent from the revisions before it, and a refused download is silent —
        // the browser saves nothing and says nothing. Asking first turns that into an answer.
        setChecking(true)
        try {
            if (!await fileExistsAt(project.id, filePath, revision)) {
                notification.error({ title: t('browser.export_dialog.file_missing', { name }) })
                return
            }
        } catch {
            // The check itself failed; let the download try and report in its own way rather than
            // refusing an export that may well succeed.
        } finally {
            setChecking(false)
        }
        downloadFile(project.id, filePath, revision)
        onClose()
    }

    const name = filePath ? basename(filePath) : project?.name
    const loadOlder = hasMore
        ? (
            <Button
                block
                data-testid="export-project-load-more"
                loading={loadingMore}
                onClick={loadMore}
                type="link"
            >
                {t('browser.export_dialog.load_more')}
            </Button>
        )
        : null

    return (
        <Modal
            destroyOnHidden
            confirmLoading={checking}
            okButtonProps={{ 'data-testid': 'export-project-submit' }}
            okText={t('browser.export_dialog.submit')}
            onCancel={onClose}
            onOk={submit}
            open={open}
            title={t('browser.export_dialog.title', { name })}
        >
            {error && (
                <Alert showIcon data-testid="export-project-error" style={{ marginBottom: 12 }} title={error} type="error" />
            )}
            <FieldRow
                label={t(filePath ? 'browser.export_dialog.file_revision' : 'browser.export_dialog.revision')}
                labelWidth={LABEL_WIDTH}
                required={options.length > 0}
            >
                <Select
                    data-testid="export-project-revision"
                    loading={revisions === null}
                    onChange={value => setChosen(value as string)}
                    options={options}
                    // The newest revisions arrive first; older ones are fetched on demand, so a long
                    // history stays reachable without the dialog waiting for all of it.
                    popupRender={menu => withFooter(menu, loadOlder)}
                    style={{ width: '100%' }}
                    value={selected}
                />
            </FieldRow>
        </Modal>
    )
}
