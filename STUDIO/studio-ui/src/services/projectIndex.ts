import { WORKSPACE_CHANGED_EVENT } from './apiCall'
import { getProjects } from './repositories'
import type { Project } from '../types/projects'
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
}

let pending: Promise<ProjectIndex> | undefined

/**
 * Reads the projects, once. Every caller after the first shares the same answer, so opening the tree
 * after the list, or coming back to the list, costs nothing.
 *
 * The read never blocks a screen: callers render what they have and fill in when it arrives.
 */
export const getProjectIndex = (): Promise<ProjectIndex> => {
    pending ??= getProjects(
        // Deleted projects come along so the status facet can show them without another read. The
        // compile states come along too: the server reads them from its compilation registry without
        // compiling anything, and only a project that is open has one.
        { unpaged: true, sort: 'name', includes: ['deleted', 'status']},
        { throwError: true, suppressErrorPages: true }
    ).then(page => ({
        projects: page.content,
        statuses: page.statuses ?? [],
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
 * True when a snapshot (or a read already underway) exists, so the next read is answered from
 * memory rather than the server. A screen that finds one paints it instantly and re-reads behind
 * it — the snapshot may predate changes made elsewhere.
 */
export const hasProjectIndex = (): boolean => pending !== undefined

// Whatever the user changed, wherever they changed it — a project deleted on its own page, a file saved,
// a branch switched — the snapshot no longer describes the workspace. The screen that comes next reads
// it again instead of showing what was true before the change.
window.addEventListener(WORKSPACE_CHANGED_EVENT, invalidateProjectIndex)
