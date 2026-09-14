import CONFIG from '../../services/config'
import type { Project } from '../../types/projects'

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
 * Every comparison is shown in one and the same window, because a session holds one comparison at a
 * time: a second window would take the comparison the first one is reading.
 */
const openCompare = (query: string): void => {
    window.open(
        `${CONFIG.CONTEXT}/compare?${query}`,
        'compare_win',
        'width=1240,height=800,resizable=yes,scrollbars=yes'
    )
}
