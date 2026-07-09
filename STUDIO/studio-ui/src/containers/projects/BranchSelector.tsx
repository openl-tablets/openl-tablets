import { useState } from 'react'
import { errorMessage } from '../../utils/errorMessage'
import { useTranslation } from 'react-i18next'
import { notification, Select, Tooltip } from 'antd'
import { BranchesOutlined } from '@ant-design/icons'
import {
    getProjectBranches,
    isProjectModifiedConflict,
    switchProjectBranch,
    type ProjectBranch,
} from '../../services/repositories'
import { DiscardChangesModal } from '../DiscardChangesModal'

interface BranchSelectorProps {
    projectId: string
    currentBranch: string
    onSwitched: () => void
}

/**
 * Compact branch switcher. The branch list is fetched the first time the dropdown opens (lazy, so
 * selecting a project costs no request). Choosing a branch switches the project to it, then refreshes.
 */
export const BranchSelector = ({ projectId, currentBranch, onSwitched }: BranchSelectorProps) => {
    const { t } = useTranslation('repository')
    const [branches, setBranches] = useState<ProjectBranch[] | null>(null)
    const [loading, setLoading] = useState(false)
    const [switching, setSwitching] = useState(false)
    const [discardSwitchBranch, setDiscardSwitchBranch] = useState<string | null>(null)

    const loadBranches = async () => {
        if (branches !== null || loading) {
            return
        }
        setLoading(true)
        try {
            setBranches(await getProjectBranches(projectId))
        } catch {
            setBranches([])
        } finally {
            setLoading(false)
        }
    }

    const switchTo = async (branch: string, discardChanges = false) => {
        if (branch === currentBranch) {
            return
        }
        setSwitching(true)
        try {
            await switchProjectBranch(projectId, branch, discardChanges ? { discardChanges: true } : {})
            onSwitched()
        } catch (e) {
            if (!discardChanges && isProjectModifiedConflict(e)) {
                setDiscardSwitchBranch(branch)
                return
            }
            notification.error({
                title: t('browser.branch.switch_failed'),
                description: errorMessage(e),
            })
        } finally {
            setSwitching(false)
        }
    }

    const options = (branches ?? [{ name: currentBranch, protected: false, bypassEligible: false }])
        .map(branch => ({ value: branch.name, label: branch.name }))

    return (
        <>
            <Tooltip title={t('browser.branch.switch')}>
                <Select
                    data-testid="branch-selector"
                    loading={loading || switching}
                    onChange={branch => void switchTo(branch)}
                    options={options}
                    popupMatchSelectWidth={false}
                    prefix={<BranchesOutlined />}
                    size="small"
                    style={{ minWidth: 140 }}
                    value={currentBranch}
                    onOpenChange={open => {
                        if (open) {
                            void loadBranches()
                        }
                    }}
                />
            </Tooltip>
            <DiscardChangesModal
                cancelButtonTestId="branch-selector-discard-switch-cancel"
                confirmButtonTestId="branch-selector-discard-switch-confirm"
                confirmText={t('browser.switch_branch_discard_confirm_unsafe')}
                onCancel={() => setDiscardSwitchBranch(null)}
                open={discardSwitchBranch !== null}
                warning={t('browser.switch_branch_discard_warning')}
                onConfirm={() => {
                    const branch = discardSwitchBranch
                    setDiscardSwitchBranch(null)
                    if (branch) {
                        void switchTo(branch, true)
                    }
                }}
            />
        </>
    )
}
