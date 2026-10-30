import CONFIG from '../../services/config'
import type { Project } from '../../types/projects'
import type { ProjectFileChangeType } from '../../services/projectStatus'

/**
 * Opens the comparison of a project in a window of its own.
 *
 * The window is told which project to compare and picks the rest itself - which file of the working
 * copy stands against which file of which revision - so nothing waits on a request before it opens.
 */
export const openCompareWindow = (project: Pick<Project, 'id'>): void => {
    openCompare(`projectId=${encodeURIComponent(project.id)}`)
}

/**
 * Opens the comparison of two Excel files the reader picks from their own computer - the Compare Excel
 * Files tool of the editor, which reads no project at all.
 */
export const openExcelFilesCompareWindow = (): void => {
    openCompare('')
}

/**
 * Opens the comparison of the two versions of a conflicted file: the one being merged in against the
 * one the workspace holds.
 *
 * The window reads both versions itself, so it opens straight away however long they take to read.
 */
export const openConflictCompareWindow = (projectId: string, path: string): void => {
    openCompare(`projectId=${encodeURIComponent(projectId)}&conflict=${encodeURIComponent(path)}`)
}

/**
 * Opens the comparison of two versions of a module, named by the screen that asks for it.
 *
 * The window starts the comparison itself, so it opens on the click rather than after a request.
 */
export const openVersionsCompareWindow = (
    projectId: string,
    moduleName: string,
    first: string,
    second: string
): void => {
    openCompare(new URLSearchParams({ projectId, module: moduleName, first, second }).toString())
}

/**
 * Opens the comparison of a file at a revision against the same file later: at another revision, such
 * as before and after a merge, or in the working copy, such as the changes a save commits. A workbook
 * is compared element by element, any other file line by line.
 *
 * A file the later revision added is absent from the earlier one, and a file it deleted is absent from the later
 * one: the side that does not hold it is compared as an empty file, so the other side shows whole.
 *
 * @param projectId the project holding the file
 * @param file      the path of the file inside the project
 * @param branch    the branch holding the revisions, or nothing in a repository without branches
 * @param before    the earlier revision, or nothing for a file added by the first revision of the project
 * @param after     the later revision, or nothing for the working copy
 * @param change    what the later revision did to the file; a modified file is held by both sides
 */
export const openFileRevisionsCompareWindow = (
    projectId: string,
    file: string,
    branch: string | undefined,
    before: string | null,
    after?: string,
    change: ProjectFileChangeType = 'modified'
): void => {
    const query = new URLSearchParams({ projectId, file })
    if (branch) {
        query.set('branch', branch)
    }
    if (before) {
        query.set('before', before)
    }
    if (after) {
        query.set('after', after)
    }
    if (change !== 'modified') {
        query.set('change', change)
    }
    openCompare(query.toString())
}

/**
 * Every comparison is shown in one and the same window, because a session holds one comparison at a
 * time: a second window would take the comparison the first one is reading.
 */
const openCompare = (query: string): void => {
    const search = query ? `?${query}` : ''
    window.open(
        `${CONFIG.CONTEXT}/compare${search}`,
        'compare_win',
        'width=1240,height=800,resizable=yes,scrollbars=yes'
    )
}
