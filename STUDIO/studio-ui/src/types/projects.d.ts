import { ProjectStatus } from '../constants/project'
import { Role } from '../constants'
import type { ProjectStatusUpdate } from '../services/projectStatus'
import type { RepositoryInfo } from './repositories'

/** The repository a project reports about itself. */
export interface ProjectRepository extends RepositoryInfo {
    id: string
}

export interface Project {
    branch: string
    comment: string
    id: string
    lockInfo?: {
        lockedAt: string
        lockedBy: string
    }
    modifiedAt: string
    modifiedBy: string
    name: string
    path?: string
    /** Id of the repository the project is stored in. */
    repository: string
    /**
     * The repository the project is stored in. Comes with the project, so it stays readable for a user
     * granted a single project and no access to the repository as a whole.
     */
    repositoryInfo?: ProjectRepository
    revision: string
    status: ProjectStatus
    tags?: Record<string, string>
    /** Whether the project's current branch is protected. */
    branchProtected?: boolean
    /** Whether the project's current branch is the repository main branch. */
    branchDefault?: boolean
    capabilities?: ProjectCapabilities
    /** Other projects this project depends on (from its rules.xml). */
    dependencies?: ProjectDependency[]
    /** Other projects that depend on this one. Only populated on the single-project detail response. */
    usedBy?: ProjectDependency[]
    /**
     * The parts of rules.xml the UI cannot work out from the file itself — the resolved modules and the
     * sources — with whether each is an engine default. Only populated when the descriptor is requested.
     */
    descriptor?: ProjectDescriptorInfo
    /** Current compilation status. Only populated when requested. */
    compileStatus?: ProjectStatusUpdate
}

/**
 * The parts of a project's rules.xml the backend resolves for the UI: the modules a wildcard expands to,
 * and the source path entries. Each carries whether it is the engine's default because the file declares
 * none — a default is shown as such and is never written back into rules.xml.
 */
export interface ProjectDescriptorInfo {
    modules?: ProjectModule[]
    modulesDefault?: boolean
    sources?: string[]
    sourcesDefault?: boolean
}

/** A reference to another project, used for the depends-on / used-by relations. */
export interface ProjectDependency {
    name: string
    /** Absent when the declared project is missing from the workspace. */
    id?: string
    status?: ProjectStatus
    repository?: string
    branch?: string
    /** Whether the branch of the dependency is the repository main branch. */
    branchDefault?: boolean
    /** Whether direct commits to the branch of the dependency are restricted. */
    branchProtected?: boolean
    /** Whether rules.xml declares this project but the workspace has no such project. */
    missing?: boolean
    /** Whether another dependency declares this one instead of the project itself. */
    transitive?: boolean
}

/**
 * A module as rules.xml declares it. A declaration whose path is a pattern stands for the files it
 * matched and carries them as its own modules; every other declaration carries none.
 */
export interface ProjectModule {
    /** Absent when the declaration is a pattern that names no module of its own. */
    name?: string
    path?: string
    modules?: ProjectModule[]
}

/**
 * What the current user may do on a project, computed server-side. Each flag is present only when
 * granted (a denied capability is omitted). Base project capabilities (canWrite/canDelete) are
 * flattened into the project response.
 */
export interface ProjectCapabilities {
    canWrite?: boolean
    canDelete?: boolean
    canOpen?: boolean
    canClose?: boolean
    canSave?: boolean
    canUnlock?: boolean
    canDeploy?: boolean
    canCompare?: boolean
    canViewHistory?: boolean
    canManage?: boolean
    canCopy?: boolean
    /** Whether the project branches can be created, merged and deleted. */
    canManageBranches?: boolean
    /** Whether the branch the project sits on can be deleted. Deleting the only branch holding the project
     *  deletes the project, so that case also takes the permission to delete it. */
    canDeleteBranch?: boolean
    canExport?: boolean
}

export interface ProjectRole {
    id: string
    name: string
    role: Role
}

export interface ProjectStatusSummary {
    local: number
    opened: number
    viewingVersion: number
    editing: number
    closed: number
    deleted: number
}

export interface FacetCount {
    id: string
    name: string
    count: number
}

export interface TagFacetSummary {
    type: string
    values: FacetCount[]
}

export interface ProjectsPage {
    content: Project[]
    pageNumber: number
    pageSize: number
    numberOfElements: number
    total?: number
    statusCounts?: ProjectStatusSummary
    repositoryCounts?: FacetCount[]
    tagCounts?: TagFacetSummary[]
    statuses?: ProjectStatusUpdate[]
    /** Cross-branch index health keyed by readable design-repository id. */
    projectIndexHealth?: Record<string, ProjectIndexHealth>
}

export interface ProjectIndexHealth {
    state: 'indexing' | 'ready' | 'degraded'
    failedBranches: string[]
    lastError?: string
}
