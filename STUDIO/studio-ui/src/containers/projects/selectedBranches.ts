import { useCallback, useMemo, useRef, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { notification } from 'antd'
import { errorMessage } from '../../utils/errorMessage'
import { setSelectedBranches, type ProjectBranch } from '../../services/repositories'
import { useCommitInfoGuard } from '../../hooks'

interface SelectedBranchesEditorOptions {
    projectId: string
    /** Branches the project currently takes part in, as the server knows them. */
    selectedBranches: string[]
    /** The branch the project is open on; it must keep the project. */
    currentBranch: string
    onSaved: () => void
}

export interface SelectedBranchesEditor {
    selected: Set<string>
    toggle: (branch: string, checked: boolean) => void
    /** Seed the editor from the saved selection; call it whenever editing starts. */
    reset: () => void
    /** Whether the branch must keep the project — the main branch and the one in use. */
    isLocked: (branch: ProjectBranch) => boolean
    /** The selection differs from the saved one. */
    dirty: boolean
    /** Branches the project would be removed from, which deletes those branches for the project. */
    removing: string[]
    saving: boolean
    save: () => Promise<void>
    commitInfoModal: ReactNode
}

/**
 * Editing state of the branches a project takes part in, shared by every place that offers it.
 *
 * Clearing a branch deletes the project from that branch, so the caller is expected to confirm a save that
 * carries removals — {@link SelectedBranchesEditor#removing} says how many there are.
 */
export const useSelectedBranchesEditor = ({
    projectId,
    selectedBranches,
    currentBranch,
    onSaved,
}: SelectedBranchesEditorOptions): SelectedBranchesEditor => {
    const { t } = useTranslation('repository')
    const { runWithCommitInfo, commitInfoModal, busy: committing } = useCommitInfoGuard()
    const [selected, setSelected] = useState<Set<string>>(() => new Set(selectedBranches))
    const [saving, setSaving] = useState(false)

    // Callers pass a freshly built array on every render, so the saved selection is read through a ref:
    // that keeps `reset` stable and safe to depend on from an effect.
    const savedRef = useRef(selectedBranches)
    savedRef.current = selectedBranches
    const reset = useCallback(() => setSelected(new Set(savedRef.current)), [])

    const toggle = useCallback((branch: string, checked: boolean) => {
        setSelected(prev => {
            const next = new Set(prev)
            if (checked) {
                next.add(branch)
            } else {
                next.delete(branch)
            }
            return next
        })
    }, [])

    const isLocked = useCallback(
        (branch: ProjectBranch) => branch.base || branch.name === currentBranch,
        [currentBranch]
    )

    const removing = useMemo(
        () => selectedBranches.filter(branch => !selected.has(branch)),
        [selected, selectedBranches]
    )
    const dirty = removing.length > 0 || [...selected].some(branch => !selectedBranches.includes(branch))

    const save = useCallback(async () => {
        await runWithCommitInfo(async () => {
            setSaving(true)
            try {
                await setSelectedBranches(projectId, [...selected])
                notification.success({ title: t('browser.branch.selection_saved') })
                onSaved()
            } catch (e) {
                notification.error({ title: t('browser.branch.selection_failed'), description: errorMessage(e) })
            } finally {
                setSaving(false)
            }
        })
    }, [onSaved, projectId, runWithCommitInfo, selected, t])

    // The commit-identity check runs before the save itself; both keep the dialog busy.
    return { selected, toggle, reset, isLocked, dirty, removing, saving: saving || committing, save, commitInfoModal }
}
