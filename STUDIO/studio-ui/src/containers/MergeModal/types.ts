// Merge mode - direction of the merge operation
export type MergeMode = 'receive' | 'send'

// Conflict resolution strategies
export type ConflictResolutionStrategy = 'BASE' | 'OURS' | 'THEIRS' | 'CUSTOM'

// File version side for downloads
export type FileSide = 'BASE' | 'OURS' | 'THEIRS'

// Check merge status
export type CheckMergeStatus = 'mergeable' | 'up-to-date'

// Merge result status
export type MergeResultStatus = 'success' | 'conflicts'

/**
 * What stands between the user and the merge they asked about. Reported next to the status: whether the
 * branches differ is one question, whether this user may merge them is another.
 */
export type MergeBlockedBy = 'bypass-required' | 'protected-branch' | 'locked'

// API Response: Check merge result
export interface CheckMergeResult {
    sourceBranch: string
    targetBranch: string
    status: CheckMergeStatus
    /** Whether the merge may be performed as is. */
    canMerge: boolean
    /** Absent when the merge may be performed. */
    blockedBy?: MergeBlockedBy
}

// API Response: Merge result
export interface MergeResultResponse {
    status: MergeResultStatus
    conflictGroups: ConflictGroup[]
}

// Conflict group containing files from same project
export interface ConflictGroup {
    projectName: string
    projectPath: string
    files: string[]
}

// Revision info for a version (OURS, THEIRS, BASE)
export interface RevisionInfo {
    commit: string
    branch: string | null
    author: string | null
    modifiedAt: string | null
    exists: boolean
}

export interface ConflictFileAvailability {
    ours: boolean
    theirs: boolean
    base: boolean
}

// API Response: Conflict details with revision info
export interface ConflictDetails {
    conflictGroups: ConflictGroup[]
    fileAvailability: Record<string, ConflictFileAvailability>
    oursRevision: RevisionInfo
    theirsRevision: RevisionInfo
    baseRevision: RevisionInfo
    defaultMessage: string
}

// Branch info for dropdown
export interface BranchInfo {
    name: string
    protected: boolean
    /** The repository main branch, marked as the default one wherever a branch is shown. */
    base?: boolean
}

// Modal detail passed from RichFaces via CustomEvent
export interface MergeModalDetail {
    projectId: string
    projectName: string
    repositoryId: string
    repositoryType: string
    currentBranch: string
    targetBranch?: string
    branches: BranchInfo[]
    initialStep?: MergeStep
    onSuccess?: () => void
    onCompare?: (filePath: string) => void
}

// Internal state: Conflict file with resolution
export interface ConflictFileState {
    filePath: string
    resolution: ConflictResolutionStrategy | null
    customFile?: File | undefined
}

// Modal step enum
export type MergeStep = 'branches' | 'conflicts'
