import { useEffect, useState } from 'react'
import { errorMessage } from '../../utils/errorMessage'
import { useTranslation } from 'react-i18next'
import { Button, Input, Modal, notification, Space, Tooltip } from 'antd'
import { BranchesOutlined, MergeOutlined, PlusOutlined } from '@ant-design/icons'
import { createProjectBranch, getProjectBranches, type ProjectBranch } from '../../services/repositories'
import { useCommitInfoGuard } from '../../hooks'

interface BranchActionsProps {
    projectId: string
    projectName: string
    repositoryId: string
    currentBranch: string
    onChanged: () => void
}

/**
 * Merge and delete-branch controls for a project on a branch-capable (Git) repository. Both reuse the
 * existing global modals via custom events. Merge first loads the branch list to seed the modal.
 */
export const BranchActions = ({ projectId, projectName, repositoryId, currentBranch, onChanged }: BranchActionsProps) => {
    const { t } = useTranslation('repository')
    const { runWithCommitInfo, commitInfoModal } = useCommitInfoGuard()
    const [loadingMerge, setLoadingMerge] = useState(false)
    const [creating, setCreating] = useState(false)
    const [newBranch, setNewBranch] = useState('')
    const [submittingBranch, setSubmittingBranch] = useState(false)
    const [currentInfo, setCurrentInfo] = useState<ProjectBranch | null>(null)
    const [mainBranch, setMainBranch] = useState<string | null>(null)

    // The base branch can never be deleted; a protected branch only by a bypass-eligible manager.
    useEffect(() => {
        let cancelled = false
        getProjectBranches(projectId)
            .then(branches => {
                if (!cancelled) {
                    setCurrentInfo(branches.find(branch => branch.name === currentBranch) ?? null)
                    setMainBranch(branches.find(branch => branch.base)?.name ?? null)
                }
            })
            .catch(() => {
                if (!cancelled) {
                    setCurrentInfo(null)
                    setMainBranch(null)
                }
            })
        return () => {
            cancelled = true
        }
    }, [projectId, currentBranch])

    const deleteBlocked = !currentInfo || currentInfo.base || (currentInfo.protected && !currentInfo.bypassEligible)

    const createBranch = async () => {
        const name = newBranch.trim()
        if (!name) {
            return
        }
        await runWithCommitInfo(async () => {
            setSubmittingBranch(true)
            try {
                await createProjectBranch(projectId, name)
                setCreating(false)
                setNewBranch('')
                onChanged()
            } catch (e) {
                notification.error({
                    title: t('browser.branch.create_failed'),
                    description: errorMessage(e),
                })
            } finally {
                setSubmittingBranch(false)
            }
        })
    }

    const openMerge = async () => {
        setLoadingMerge(true)
        try {
            const branches = await getProjectBranches(projectId)
            window.dispatchEvent(new CustomEvent('openMergeModal', {
                detail: {
                    projectId,
                    projectName,
                    repositoryId,
                    // Only Git repositories support branches, so a branch project is always Git.
                    repositoryType: 'repo-git',
                    currentBranch,
                    branches,
                    onSuccess: onChanged,
                },
            }))
        } catch (e) {
            notification.error({
                title: t('browser.branch.merge_failed'),
                description: errorMessage(e),
            })
        } finally {
            setLoadingMerge(false)
        }
    }

    const openDeleteBranch = () => {
        window.dispatchEvent(new CustomEvent('openDeleteBranchModal', {
            detail: { repositoryId, projectName, branch: currentBranch, mainBranch: mainBranch ?? undefined, onSuccess: onChanged },
        }))
    }

    return (
        <>
            <Space size="small">
                <Button
                    data-testid="branch-new"
                    icon={<PlusOutlined />}
                    onClick={() => setCreating(true)}
                    size="small"
                >
                    {t('browser.branch.create')}
                </Button>
                <Button
                    data-testid="branch-merge"
                    icon={<MergeOutlined />}
                    loading={loadingMerge}
                    onClick={openMerge}
                    size="small"
                >
                    {t('browser.branch.merge')}
                </Button>
                <Tooltip title={deleteBlocked ? t('browser.branch.delete_protected') : ''}>
                    <Button
                        data-testid="branch-delete"
                        disabled={deleteBlocked}
                        icon={<BranchesOutlined />}
                        onClick={openDeleteBranch}
                        size="small"
                    >
                        {t('browser.branch.delete')}
                    </Button>
                </Tooltip>
                <Modal
                    destroyOnHidden
                    confirmLoading={submittingBranch}
                    okButtonProps={{ 'data-testid': 'branch-create-submit', disabled: !newBranch.trim() }}
                    onCancel={() => setCreating(false)}
                    onOk={createBranch}
                    open={creating}
                    title={t('browser.branch.create_title')}
                >
                    <Input
                        data-testid="branch-name"
                        onChange={event => setNewBranch(event.target.value)}
                        onPressEnter={createBranch}
                        placeholder={t('browser.branch.name')}
                        value={newBranch}
                    />
                </Modal>
            </Space>
            {commitInfoModal}
        </>
    )
}
