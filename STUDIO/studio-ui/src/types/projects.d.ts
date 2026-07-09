import { ProjectStatus } from '../constants/project'
import { Role } from '../constants'
import type { ProjectStatusUpdate } from '../services/projectStatus'

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
    repository: string
    revision: string
    status: ProjectStatus
    tags?: Record<string, string>
    selectedBranches?: string[]
    /** Whether the project's current branch is protected. */
    branchProtected?: boolean
    capabilities?: ProjectCapabilities
    /** Other projects this project depends on (from its rules.xml). */
    dependencies?: ProjectDependency[]
    /** Other projects that depend on this one. Only populated on the single-project detail response. */
    usedBy?: ProjectDependency[]
    /** rules.xml project comment. Only populated when requested. */
    description?: string
    /** rules.xml modules. Only populated when requested. */
    modules?: ProjectModule[]
    /** rules.xml properties file-name patterns. Only populated when requested. */
    versionPatterns?: string[]
    /** rules.xml exposed-methods filter. Only populated when requested. */
    exposedMethods?: ProjectExposedMethods
    /** Current compilation status. Only populated when requested. */
    compileStatus?: ProjectStatusUpdate
}

/** A reference to another project, used for the depends-on / used-by relations. */
export interface ProjectDependency {
    name: string
    id: string
    status?: ProjectStatus
    repository?: string
    branch?: string
}

/** A rules module declared in rules.xml. */
export interface ProjectModule {
    name: string
    path?: string
}

/** The rules.xml exposed-methods filter (glob patterns on method names). */
export interface ProjectExposedMethods {
    includes?: string[]
    excludes?: string[]
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
    canEditTags?: boolean
    canManage?: boolean
    canCopy?: boolean
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
}
