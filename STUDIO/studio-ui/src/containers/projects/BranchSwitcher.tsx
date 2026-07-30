import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Dropdown, notification } from 'antd'
import { DownOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { errorMessage } from '../../utils/errorMessage'
import {
    getProjectBranches,
    isProjectModifiedConflict,
    switchProjectBranch,
    type ProjectBranch,
} from '../../services/repositories'
import { DiscardChangesModal } from '../DiscardChangesModal'
import { BranchLabel } from './BranchLabel'

const useStyles = createStyles(({ css, token }) => ({
    trigger: css`
        display: inline-flex;
        align-items: center;
        gap: 6px;
        /* Never wider than the place it is put in: the branch name truncates instead. */
        max-width: 100%;
        min-width: 0;
        padding: 0 4px;
        border: 0;
        border-radius: ${token.borderRadiusSM}px;
        background: transparent;
        color: inherit;
        font: inherit;
        cursor: pointer;

        &:hover {
            background: ${token.colorFillTertiary};
        }

        &:disabled {
            cursor: progress;
        }
    `,
    caret: css`
        color: ${token.colorTextQuaternary};
        font-size: 10px;
    `,
}))

interface BranchSwitcherProps {
    projectId: string
    currentBranch: string
    /** Marks of the current branch, known from the project itself without listing the branches. */
    currentBranchProtected?: boolean | undefined
    currentBranchDefault?: boolean | undefined
    onSwitched: () => void
    'data-testid'?: string
}

/**
 * Switches the project to another of its branches, showing the Default and protected marks both for the
 * current branch and for every branch offered.
 *
 * The branch reads as plain text with a caret rather than as a form input, so it stays unobtrusive
 * wherever it is placed — the breadcrumb and the Overview tab render the very same control.
 *
 * The switch targets and their marks are fetched when the menu first opens. Simply showing the current
 * branch costs no request and does not carry every project membership in the projects response.
 */
export const BranchSwitcher = ({
    projectId,
    currentBranch,
    currentBranchProtected,
    currentBranchDefault,
    onSwitched,
    'data-testid': testId = 'branch-switcher',
}: BranchSwitcherProps) => {
    const { styles } = useStyles()
    const { t } = useTranslation('repository')
    const [branchInfo, setBranchInfo] = useState<ProjectBranch[] | null>(null)
    const [loading, setLoading] = useState(false)
    const [switching, setSwitching] = useState(false)
    const [discardSwitchBranch, setDiscardSwitchBranch] = useState<string | null>(null)

    const loadBranches = async () => {
        if (branchInfo !== null || loading) {
            return
        }
        setLoading(true)
        try {
            setBranchInfo(await getProjectBranches(projectId))
        } catch (e) {
            setBranchInfo([])
            notification.error({ title: t('browser.branch.load_failed'), description: errorMessage(e) })
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
            notification.error({ title: t('browser.branch.switch_failed'), description: errorMessage(e) })
        } finally {
            setSwitching(false)
        }
    }

    // The current branch carries its marks from the project; the others only once the list is loaded.
    const marksOf = (branch: string) => {
        const loaded = branchInfo?.find(item => item.name === branch)
        if (loaded) {
            return { isDefault: loaded.base, isProtected: loaded.protected }
        }
        return branch === currentBranch
            ? { isDefault: currentBranchDefault, isProtected: currentBranchProtected }
            : {}
    }

    const branchLabel = (branch: string) => <BranchLabel name={branch} {...marksOf(branch)} />

    const current = <BranchLabel withIcon name={currentBranch} testId={testId} {...marksOf(currentBranch)} />

    const discardModal = (
        <DiscardChangesModal
            cancelButtonTestId={`${testId}-discard-switch-cancel`}
            confirmButtonTestId={`${testId}-discard-switch-confirm`}
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
    )

    return (
        <>
            <Dropdown
                trigger={['click']}
                menu={{
                    items: (branchInfo ?? []).map(branch => ({
                        key: branch.name,
                        label: branchLabel(branch.name),
                    })),
                    selectedKeys: [currentBranch],
                    onClick: ({ key }) => void switchTo(key),
                }}
                onOpenChange={open => {
                    if (open) {
                        void loadBranches()
                    }
                }}
            >
                <button
                    className={styles.trigger}
                    data-testid={`${testId}-trigger`}
                    disabled={switching}
                    type="button"
                >
                    {current}
                    <DownOutlined className={styles.caret} />
                </button>
            </Dropdown>
            {discardModal}
        </>
    )
}
