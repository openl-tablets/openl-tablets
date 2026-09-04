import type { RepositoryConfig } from '../../types/repositories'
import { isProtectedBranch } from '../../utils/branchProtection'
import type { BranchMarksInfo } from './BranchSelect'

/**
 * The marks a branch carries, told from a repository configuration: it reads as Default when it is the
 * configured branch, and as protected when a protected pattern matches it. Used by the create and copy
 * forms, where the branches are known only by name.
 */
export const branchMarksFromConfig = (config: RepositoryConfig | null | undefined) => (name: string): BranchMarksInfo => ({
    isDefault: name === config?.branch,
    isProtected: isProtectedBranch(name, config?.protectedBranches),
})
