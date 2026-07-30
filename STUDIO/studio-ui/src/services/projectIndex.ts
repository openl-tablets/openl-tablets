import { WORKSPACE_CHANGED_EVENT } from './apiCall'
import { getProjects } from './repositories'
import type { Project, ProjectIndexHealth } from '../types/projects'
import type { ProjectStatusUpdate } from './projectStatus'

/**
 * Every project the user may see, read in one request.
 *
 * The projects screen and the grouped tree both work from this one snapshot: filtering, sorting and
 * paging happen in the browser, so a facet click or a page step costs nothing and the server is asked
 * once instead of on every interaction. The facet counts are not asked for at all — they are counted
 * from the snapshot, which is the expensive part of the list response.
 */
export interface ProjectIndex {
    projects: Project[]
    /** The compile state of the projects the workspace has a live one for. */
    statuses: ProjectStatusUpdate[]
    /** Cross-branch index health keyed by readable design-repository id. */
    projectIndexHealth: Record<string, ProjectIndexHealth>
}

let pending: Promise<ProjectIndex> | undefined
/** When the snapshot was asked for; the staleness policy counts from here. */
let takenAt = 0

/**
 * How long a read is trusted without any invalidation. Changes normally arrive as server pings
 * or local mutations; the age limit catches what those miss — a lost ping, a laptop waking up.
 * The project page applies the same window to its own detail read.
 */
export const PROJECT_INDEX_TTL_MS = 5 * 60_000

/**
 * Reads the projects, once. Every caller after the first shares the same answer, so opening the tree
 * after the list, or coming back to the list, costs nothing. A snapshot older than its trust window
 * is re-read instead of served.
 *
 * The read never blocks a screen: callers render what they have and fill in when it arrives.
 */
export const getProjectIndex = (): Promise<ProjectIndex> => {
    if (pending && isProjectIndexStale()) {
        pending = undefined
    }
    if (!pending) {
        takenAt = Date.now()
    }
    pending ??= getProjects(
        // Deleted projects come along so the status facet can show them without another read. The
        // compile states come along too: the server reads them from its compilation registry without
        // compiling anything, and only a project that is open has one.
        { unpaged: true, sort: 'name', includes: ['deleted', 'status']},
        { throwError: true, suppressErrorPages: true }
    ).then(page => ({
        projects: page.content,
        statuses: page.statuses ?? [],
        projectIndexHealth: page.projectIndexHealth ?? {},
    })).catch(error => {
        // A failed read must not be remembered as the answer: the next open tries again.
        pending = undefined
        throw error
    })
    return pending
}

/** Drops the snapshot, so the next read sees the workspace as it is now. */
export const invalidateProjectIndex = (): void => {
    pending = undefined
}

/**
 * What makes a project visibly different on the screens — enough to tell a background refresh that
 * changed something from one that merely echoed the user's own action.
 */
export const projectSignature = (project: Project | null): string =>
    project === null
        ? ''
        : JSON.stringify([
            project.id,
            project.revision,
            project.status,
            project.branch,
            project.modifiedAt,
        ])

/**
 * True when a snapshot (or a read already underway) exists, so the next read is answered from
 * memory rather than the server. A screen that finds one paints it instantly and re-reads behind
 * it — the snapshot may predate changes made elsewhere.
 */
export const hasProjectIndex = (): boolean => pending !== undefined

/**
 * True when there is no snapshot, or the one there is has outlived its trust window. A screen the
 * user comes back to checks this and re-reads quietly instead of trusting what a sleeping tab kept.
 */
export const isProjectIndexStale = (): boolean => pending === undefined || Date.now() - takenAt > PROJECT_INDEX_TTL_MS

// Whatever the user changed, wherever they changed it — a project deleted on its own page, a file saved,
// a branch switched — the snapshot no longer describes the workspace. The screen that comes next reads
// it again instead of showing what was true before the change.
window.addEventListener(WORKSPACE_CHANGED_EVENT, invalidateProjectIndex)
