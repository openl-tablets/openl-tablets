import { getProjectBranches, type ProjectBranch } from '../../services/repositories'
import type { Project } from '../../types/projects'

/** The project fields every branch dialog needs. */
type BranchProject = Pick<Project, 'id' | 'name' | 'repository' | 'branch' | 'branchProtected'>

/** The branch list seeds the dialogs; without it they still open, only without branch details. */
const branchesOf = async (projectId: string): Promise<ProjectBranch[]> => {
    try {
        return await getProjectBranches(projectId)
    } catch {
        return []
    }
}

/**
 * Opens the shared merge dialog for a project, seeded with its branches.
 *
 * @param initialStep opens the dialog straight on conflict resolution instead of the branch choice
 */
export const openMergeDialog = async (
    project: BranchProject,
    onSuccess: () => void,
    initialStep?: 'conflicts'
): Promise<void> => {
    window.dispatchEvent(new CustomEvent('openMergeModal', {
        detail: {
            projectId: project.id,
            projectName: project.name,
            repositoryId: project.repository,
            // Only Git repositories support branches, so a branch project is always Git.
            repositoryType: 'repo-git',
            currentBranch: project.branch || '',
            branches: await branchesOf(project.id),
            ...(initialStep ? { initialStep } : {}),
            onSuccess,
        },
    }))
}

/** Opens the shared delete-branch dialog for the branch the project is on. */
export const openDeleteBranchDialog = async (project: BranchProject, onSuccess: () => void): Promise<void> => {
    const branches = await branchesOf(project.id)
    window.dispatchEvent(new CustomEvent('openDeleteBranchModal', {
        detail: {
            projectId: project.id,
            repositoryId: project.repository,
            projectName: project.name,
            branch: project.branch,
            // The warning about unmerged changes names the branch the project would have merged into.
            mainBranch: branches.find(branch => branch.base)?.name,
            // A protected branch is deleted only with the repository bypass, which the server grants. The
            // project already knows whether its branch is protected; the branch list only confirms it, so
            // a failed listing never turns a protected branch into an unprotected one.
            branchProtected: project.branchProtected
                ?? branches.find(branch => branch.name === project.branch)?.protected
                ?? false,
            // Only the branches holding the project are listed, so a single entry means this branch keeps its
            // last copy and deleting it deletes the project. A failed listing is empty and answers the same
            // way: like every other unknown in this dialog, it asks for the deliberate confirmation rather
            // than presenting an irreversible deletion as routine branch cleanup.
            lastBranch: branches.length <= 1,
            onSuccess,
        },
    }))
}
