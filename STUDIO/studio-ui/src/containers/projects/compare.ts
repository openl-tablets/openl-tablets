import CONFIG from '../../services/config'
import type { Project } from '../../types/projects'

/**
 * Opens the revision comparison for a project.
 *
 * Comparison stays in the legacy JSF screen, opened in a separate popup window rather than a browser tab.
 * The repository id disambiguates projects that share a name across repositories.
 */
export const openCompareWindow = (project: Pick<Project, 'name' | 'repository'>): void => {
    window.open(
        `${CONFIG.CONTEXT}/faces/pages/modules/repository/compare.xhtml`
            + `?projectName=${encodeURIComponent(project.name)}`
            + `&repoId=${encodeURIComponent(project.repository)}`,
        'compare_win',
        'width=1240,height=800,resizable=yes,scrollbars=yes'
    )
}
