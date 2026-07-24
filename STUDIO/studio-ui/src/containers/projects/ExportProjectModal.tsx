import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, Modal, Select } from 'antd'
import { downloadProject } from '../../services/repositories'
import { ProjectStatus } from '../../constants/project'
import { FieldRow } from '../../components/FieldRow'
import type { Project } from '../../types/projects'
import { useProjectRevisions } from './revisions'

const LABEL_WIDTH = 150

/** Marks the workspace copy rather than a committed revision. */
const WORKSPACE = 'workspace'

interface ExportProjectModalProps {
    open: boolean
    project: Project | null
    onClose: () => void
}

/**
 * Downloads the project as an archive, from the workspace copy or from any earlier revision.
 *
 * The workspace copy leads the list while the project is open: for a project being edited it carries the
 * local changes, and for one merely opened it is the revision it was opened on.
 */
export const ExportProjectModal = ({ open, project, onClose }: ExportProjectModalProps) => {
    const { t } = useTranslation('repository')
    const [chosen, setChosen] = useState<string | undefined>(undefined)

    const editing = project?.status === ProjectStatus.Editing
    const opened = editing || project?.status === ProjectStatus.Opened
        || project?.status === ProjectStatus.ViewingVersion
    const { revisions, options: revisionOptions, error } = useProjectRevisions(project, open)
    // The workspace copy is what an open project exports; a closed one exports its latest revision.
    const selected = chosen ?? (opened ? WORKSPACE : revisions?.[0]?.revisionNo ?? WORKSPACE)

    // The dialog stays mounted between openings; a revision picked for one project must not carry over.
    useEffect(() => {
        setChosen(undefined)
    }, [open, project])

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

    const submit = () => {
        if (!project) {
            return
        }
        // With no revision to choose from — an unreadable or empty history — the latest state is exported.
        const revision = options.length > 0 && selected !== WORKSPACE ? selected : undefined
        downloadProject(project.id, revision)
        onClose()
    }

    return (
        <Modal
            destroyOnHidden
            okButtonProps={{ 'data-testid': 'export-project-submit' }}
            okText={t('browser.export_dialog.submit')}
            onCancel={onClose}
            onOk={submit}
            open={open}
            title={t('browser.export_dialog.title', { name: project?.name })}
        >
            {error && (
                <Alert showIcon data-testid="export-project-error" style={{ marginBottom: 12 }} title={error} type="error" />
            )}
            <FieldRow label={t('browser.export_dialog.revision')} labelWidth={LABEL_WIDTH} required={options.length > 0}>
                <Select
                    data-testid="export-project-revision"
                    loading={revisions === null}
                    onChange={value => setChosen(value as string)}
                    options={options}
                    style={{ width: '100%' }}
                    value={selected}
                />
            </FieldRow>
        </Modal>
    )
}
