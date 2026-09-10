import CONFIG from '../../services/config'
import type { Project } from '../../types/projects'

/**
 * Opens the comparison of a project in a window of its own.
 *
 * The window is told which project to compare and picks the rest itself - which file of the working
 * copy stands against which file of which revision - so nothing waits on a request before it opens.
 */
export const openCompareWindow = (project: Pick<Project, 'id'>): void => {
    window.open(
        `${CONFIG.CONTEXT}/compare?projectId=${encodeURIComponent(project.id)}`,
        'compare_win',
        'width=1240,height=800,resizable=yes,scrollbars=yes'
    )
}
