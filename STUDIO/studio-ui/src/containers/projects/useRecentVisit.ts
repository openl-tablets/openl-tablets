import { useEffect } from 'react'
import { recentBranches, recentProjects } from '../../utils/recentChoices'

/**
 * Remembers the project a screen shows, and the branch it is read on, as the ones used most recently: the switchers
 * of the trail list them first.
 *
 * @param projectId    the project on screen; none while it is read
 * @param repositoryId the repository the project is stored in, whose branch it is
 * @param branch       the branch it is read on; null for a repository without branches
 */
export const useRecentVisit = (
    projectId: string | undefined,
    repositoryId: string | undefined,
    branch: string | null | undefined
): void => {
    useEffect(() => {
        if (!projectId) {
            return
        }
        recentProjects.remember(projectId)
        if (branch && repositoryId) {
            recentBranches.remember(branch, repositoryId)
        }
    }, [projectId, repositoryId, branch])
}
