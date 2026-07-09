import { useEffect, useState } from 'react'
import { errorMessage } from '../../utils/errorMessage'
import { useTranslation } from 'react-i18next'
import { Alert, Form, Input, Modal, notification, Select } from 'antd'
import { copyProject } from '../../services/repositories'
import type { Project } from '../../types/projects'
import type { Repository } from '../../types/repositories'
import { supportsMappedFolders } from '../../utils/repositoryFeatures'
import { useCommitInfoGuard } from '../../hooks'

interface CopyProjectModalProps {
    open: boolean
    project: Project | null
    repositories: Repository[]
    onClose: () => void
    onCopied: () => void
}

/**
 * Copy a project into a chosen design repository under a new name, mirroring the legacy Copy dialog.
 * The server performs the copy directly in the repository (no download) and validates the name and
 * comment; a name collision is rejected rather than silently overwritten.
 */
export const CopyProjectModal = ({ open, project, repositories, onClose, onCopied }: CopyProjectModalProps) => {
    const { t } = useTranslation('repository')
    const { runWithCommitInfo, commitInfoModal } = useCommitInfoGuard()
    const [targetRepositoryId, setTargetRepositoryId] = useState('')
    const [name, setName] = useState('')
    const [comment, setComment] = useState('')
    const [path, setPath] = useState('')
    const [submitting, setSubmitting] = useState(false)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        if (open && project) {
            setTargetRepositoryId(repositories.some(repo => repo.id === project.repository)
                ? project.repository
                : repositories[0]?.id ?? '')
            setName(`${project.name}-copy`)
            setComment('')
            setPath('')
            setError(null)
        }
    }, [open, project, repositories, t])

    // A path inside the repository only applies to non-flat (mapped-folder) repositories.
    const targetSupportsFolders = supportsMappedFolders(repositories.find(repo => repo.id === targetRepositoryId))

    const submit = async () => {
        if (!project || !targetRepositoryId) {
            return
        }
        const trimmed = name.trim()
        if (!trimmed) {
            setError(t('browser.copy_dialog.name_required'))
            return
        }
        await runWithCommitInfo(async () => {
            setSubmitting(true)
            setError(null)
            try {
                await copyProject(
                    project.repository,
                    project.name,
                    targetRepositoryId,
                    trimmed,
                    comment.trim() || undefined,
                    targetSupportsFolders ? path : undefined
                )
                notification.success({ title: t('browser.copy_dialog.success', { name: trimmed }) })
                onCopied()
                onClose()
            } catch (e) {
                setError(errorMessage(e))
            } finally {
                setSubmitting(false)
            }
        })
    }

    return (
        <>
            <Modal
                destroyOnHidden
                confirmLoading={submitting}
                okButtonProps={{ 'data-testid': 'copy-project-submit', disabled: repositories.length === 0 }}
                okText={t('browser.copy_dialog.submit')}
                onCancel={onClose}
                onOk={submit}
                open={open}
                title={t('browser.copy_dialog.title', { name: project?.name })}
            >
                {error && (
                    <Alert showIcon data-testid="copy-project-error" message={error} style={{ marginBottom: 12 }} type="error" />
                )}
                <Form layout="vertical">
                    <Form.Item label={t('browser.copy_dialog.target_repository')}>
                        <Select
                            data-testid="copy-project-repository"
                            onChange={value => setTargetRepositoryId(value as string)}
                            options={repositories.map(repo => ({ value: repo.id, label: repo.name }))}
                            value={targetRepositoryId}
                        />
                    </Form.Item>
                    <Form.Item label={t('browser.copy_dialog.new_name')}>
                        <Input data-testid="copy-project-name" onChange={event => setName(event.target.value)} value={name} />
                    </Form.Item>
                    {targetSupportsFolders && (
                        <Form.Item label={t('browser.copy_dialog.path')}>
                            <Input
                                data-testid="copy-project-path"
                                onChange={event => setPath(event.target.value)}
                                placeholder={t('browser.copy_dialog.path_placeholder')}
                                value={path}
                            />
                        </Form.Item>
                    )}
                    <Form.Item label={t('browser.copy_dialog.comment')}>
                        <Input.TextArea
                            autoSize={{ maxRows: 6, minRows: 2 }}
                            data-testid="copy-project-comment"
                            onChange={event => setComment(event.target.value)}
                            value={comment}
                        />
                    </Form.Item>
                </Form>
            </Modal>
            {commitInfoModal}
        </>
    )
}
