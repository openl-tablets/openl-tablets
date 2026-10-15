import type { ProjectFileChange, ProjectFileChangeType } from '../../services/projectStatus'
import type {
    ConflictFileAvailability,
    ConflictSide,
    ConflictTextBlock,
    ConflictWorkbook,
} from '../../services/compare'

// Merge mode - direction of the merge operation
export type MergeMode = 'receive' | 'send'

// Conflict resolution strategies
export type ConflictResolutionStrategy = 'BASE' | 'OURS' | 'THEIRS' | 'CUSTOM'

// File version side for downloads
export type { ConflictSide as FileSide } from '../../services/compare'

// Check merge status
export type CheckMergeStatus = 'mergeable' | 'up-to-date'

// Merge result status: a cancelled merge was stopped on request before it committed anything
export type MergeResultStatus = 'success' | 'conflicts' | 'cancelled'

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
    /** What the merge would bring in; absent when the target branch is up to date. */
    scope?: MergeScope
}

/** Which branch a sheet of a workbook merged automatically was taken from. */
export type SheetOrigin = 'target' | 'source' | 'both'

/**
 * A project a merge changes. The files outside any project form a group of their own, which names no project and
 * has no status.
 */
export interface ChangedProject {
    id?: string
    name?: string
    path?: string
    /** Whether it is the project the dialog was opened for. */
    current: boolean
    /** What the merge does to the project as a whole. */
    status?: ProjectFileChangeType
}

/** A project a merge would change, with how many of its files change. */
export interface ProjectScope extends ChangedProject {
    changedFiles: number
    /** The files both branches changed: Sync merges them by itself or reports them as conflicts. */
    changedInBoth?: string[]
}

/** What a merge would bring into the target branch. */
export interface MergeScope {
    projects: ProjectScope[]
    /** How many changed projects the user may not read. */
    hiddenProjects: number
}

export interface SheetChange {
    name: string
    takenFrom: SheetOrigin
}

/** A file the merge changed, by its path from the root of the repository. */
export interface FileChange extends ProjectFileChange {
    /** The sheets of a workbook Sync merged by itself; absent for any other file. */
    sheets?: SheetChange[]
}

/** A project the merge changed, or the files outside any project. */
export interface ProjectChanges extends ChangedProject {
    files: FileChange[]
}

/** What a merge changed in the target branch. */
export interface MergeSummary {
    sourceBranch: string
    targetBranch: string
    /** The revision of the target branch before the merge. */
    before: string
    /** The revision of the merge. */
    after: string
    projects: ProjectChanges[]
    hiddenProjects: number
}

// API Response: Merge result
export interface MergeResultResponse {
    status: MergeResultStatus
    conflictGroups: ConflictGroup[]
    /** What a successful merge changed. */
    summary?: MergeSummary
}

// API Response: Conflict resolution result
export interface ResolveConflictsResponse {
    status: 'success'
    resolvedFiles: string[]
    /** What the merge changed; absent when the conflicts of a save were resolved. */
    summary?: MergeSummary
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

export type { ConflictFileAvailability }

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
}

/**
 * How the dialog resolves a conflicted file: by a strategy of the server, or `MERGE` for a file merged in the dialog,
 * a text file line by line or a workbook table by table, which is sent as a merged file.
 */
export type FileResolution = ConflictResolutionStrategy | 'MERGE'

// Internal state: Conflict file with resolution
export interface ConflictFileState {
    filePath: string
    resolution: FileResolution | null
    customFile?: File | undefined
    /** The text file as the user merged it in the editor of the dialog, for the `MERGE` resolution. */
    merged?: MergedText | undefined
    /** The workbook as the user merged it in the editor of the dialog, for the `MERGE` resolution. */
    mergedWorkbook?: MergedWorkbook | undefined
}

export type { ConflictSheet, ConflictTable, ConflictTableVersion, ConflictWorkbook } from '../../services/compare'

/**
 * A workbook merged in the editor of the dialog: the merged file, the decisions made in the editor, and the conflicts
 * they were made for. The decisions include the ones a sheet taken whole stands for, which the merge left out.
 */
export interface MergedWorkbook {
    file: Blob
    decisions: Record<string, ConflictSide>
    workbook: ConflictWorkbook
}

export type {
    ConflictedTextBlock,
    ConflictText,
    ConflictTextBlock,
    MergedTextBlock,
} from '../../services/compare'

/**
 * How a conflict of a text file is resolved: by one version of its lines, by both in an order, or by lines the user
 * wrote.
 */
export type ConflictChoice = 'ours' | 'theirs' | 'ours-theirs' | 'theirs-ours' | 'base' | 'edited'

/** How one conflict of a text file was resolved: the choice and the lines it gives. */
export interface ConflictDecision {
    choice: ConflictChoice
    lines: string
}

/**
 * A text file merged in the editor of the dialog: its text, how each conflict, in their order, was resolved, and the
 * parts of the file the decisions were made for.
 */
export interface MergedText {
    text: string
    decisions: ConflictDecision[]
    blocks: ConflictTextBlock[]
}

// Modal step enum
export type MergeStep = 'branches' | 'conflicts'
