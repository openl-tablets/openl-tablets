import { RepositoryType, Role } from '../constants'

/**
 * What the current user may do on a repository, computed server-side (honouring both the ACL and the
 * repository configuration). Each flag is present only when granted. No raw role or permission set is
 * exposed, because repository configuration can take precedence over the ACL.
 */
export interface RepositoryCapabilities {
    canCreateProject?: boolean
    canManage?: boolean
}

export interface RepositoryFeatures {
    /** Repository supports branch-specific project operations. */
    branches: boolean
    /** Repository supports revision search, technical revisions and paged history. */
    searchable: boolean
    /** Repository uses a non-flat structure and accepts a project path within the repository. */
    mappedFolders: boolean
}

/**
 * Settings of the repository a project is stored in, as the project forms need them: how a branch name and
 * a commit comment are suggested, and the expressions they must match.
 */
export interface RepositoryConfig {
    /** Branch rules; absent when the repository has no branches. */
    newBranch?: {
        /** Pattern the suggested name is built from, e.g. `{project-name}/{username}/{current-date}`. */
        pattern?: string | undefined
        /** Expression a branch name must match; absent when any name is accepted. */
        namePattern?: string | undefined
        invalidNameHint?: string | undefined
    } | undefined
    comment: {
        /** Expression a comment must match; absent when the repository does not customize comments. */
        userMessagePattern?: string | undefined
        invalidUserMessageHint?: string | undefined
        /** Templates the suggested comments are built from. */
        templates: {
            save?: string | undefined
            create?: string | undefined
            copy?: string | undefined
            restoreFrom?: string | undefined
        }
    }
}

export interface Repository {
    /** A deployment repository that takes a project only from the main branch of its design repository. */
    mainBranchOnly?: boolean
    aclId: string
    id: string
    name: string
    type?: string | undefined
    capabilities?: RepositoryCapabilities
    features?: RepositoryFeatures | undefined
}

export interface RepositoryInfo {
    name: string
    type?: string | undefined
    features?: RepositoryFeatures | undefined
}

export interface RepositoryRole {
    id: string
    name: string
    role: Role
    type: RepositoryType
}

export interface RepositoryRootRole {
    id: string
    role: Role
    type: RepositoryType
}
