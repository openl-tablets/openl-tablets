import { useEffect, useRef, useState } from 'react'
import { App } from 'antd'
import { useTranslation } from 'react-i18next'
import { errorMessage } from '../../utils/errorMessage'
import {
    getProjectBranches,
    isProjectModifiedConflict,
    switchProjectBranch,
    type ProjectBranch,
} from '../../services/repositories'
import { DiscardChangesModal } from '../DiscardChangesModal'
import { BranchLabel, type BranchTone } from './BranchLabel'
import { CrumbSwitcher } from './CrumbSwitcher'

interface BranchSwitcherProps {
    projectId: string
    currentBranch: string
    /** Marks of the current branch, known from the project itself without listing the branches. */
    currentBranchProtected?: boolean | undefined
    currentBranchDefault?: boolean | undefined
    /**
     * The project moved to another branch and has to be read again. A promise is awaited, so the switch
     * counts as running until the screen shows the new branch's data.
     */
    onSwitched: () => void | Promise<unknown>
    /**
     * Whether the switch — the request and the reload behind it — is running, so the screen around it can
     * mark the project busy and block its other actions meanwhile.
     */
    onBusyChange?: ((busy: boolean) => void) | undefined
    /** Blocks the switch while the project is busy with another operation of its own. */
    disabled?: boolean | undefined
    /** Colour tone of the current branch — `secondary` to read like a breadcrumb link. */
    tone?: BranchTone | undefined
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
 *
 * A switch takes seconds on a slow environment, so the trigger says so for as long as it runs — through
 * the request and through the reload that follows it, since only then does the screen show the new branch.
 */
export const BranchSwitcher = ({
    projectId,
    currentBranch,
    currentBranchProtected,
    currentBranchDefault,
    onSwitched,
    onBusyChange,
    disabled = false,
    tone,
    'data-testid': testId = 'branch-switcher',
}: BranchSwitcherProps) => {
    const { notification } = App.useApp()
    const { t } = useTranslation('repository')
    const [branchInfo, setBranchInfo] = useState<ProjectBranch[] | null>(null)
    const [loading, setLoading] = useState(false)
    const [switching, setSwitching] = useState(false)
    const [discardSwitchBranch, setDiscardSwitchBranch] = useState<string | null>(null)
    const projectIdRef = useRef(projectId)

    useEffect(() => {
        projectIdRef.current = projectId
        setBranchInfo(null)
        setLoading(false)
        setDiscardSwitchBranch(null)
    }, [projectId])

    const loadBranches = async () => {
        if (branchInfo !== null || loading) {
            return
        }
        const requestedProjectId = projectId
        setLoading(true)
        try {
            const loaded = await getProjectBranches(requestedProjectId)
            if (projectIdRef.current === requestedProjectId) {
                setBranchInfo(loaded)
            }
        } catch (e) {
            if (projectIdRef.current === requestedProjectId) {
                notification.error({ title: t('browser.branch.load_failed'), description: errorMessage(e) })
            }
        } finally {
            if (projectIdRef.current === requestedProjectId) {
                setLoading(false)
            }
        }
    }

    const switchTo = async (branch: string, discardChanges = false) => {
        if (branch === currentBranch) {
            return
        }
        setSwitching(true)
        onBusyChange?.(true)
        try {
            await switchProjectBranch(projectId, branch, discardChanges ? { discardChanges: true } : {})
            // The reload is part of the switch: until it lands the screen still shows the branch the user
            // switched away from, so the busy state has to outlive the request that started it.
            await onSwitched()
        } catch (e) {
            if (!discardChanges && isProjectModifiedConflict(e)) {
                setDiscardSwitchBranch(branch)
                return
            }
            notification.error({ title: t('browser.branch.switch_failed'), description: errorMessage(e) })
        } finally {
            setSwitching(false)
            onBusyChange?.(false)
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

    const current = <BranchLabel withIcon name={currentBranch} testId={testId} tone={tone} {...marksOf(currentBranch)} />

    const items = branchInfo === null ? null : branchInfo.map(branch => ({
        key: branch.name,
        label: branchLabel(branch.name),
        search: branch.name,
    }))

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
            <CrumbSwitcher
                busy={switching}
                current={current}
                disabled={disabled}
                emptyText={t('browser.branch.no_match')}
                items={items}
                loading={loading}
                onOpen={() => void loadBranches()}
                onSelect={branch => void switchTo(branch)}
                searchPlaceholder={t('browser.branch.filter')}
                selectedKey={currentBranch}
                testId={testId}
            />
            {discardModal}
        </>
    )
}
