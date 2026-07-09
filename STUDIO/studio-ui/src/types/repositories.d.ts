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

export interface Repository {
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
