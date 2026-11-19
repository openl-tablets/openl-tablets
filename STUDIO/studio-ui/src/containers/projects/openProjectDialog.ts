import type { SetProjectStatusOptions } from '../../services/repositories'
import type { Project, ProjectDependency } from '../../types/projects'

/** What the user answered about the dependencies of the project being opened. */
export type OpenDependenciesOptions = Pick<SetProjectStatusOptions, 'openDependencies' | 'switchDependencies'>

/**
 * The project fields opening needs. Both the list and the detail response carry the dependencies, so the
 * screen always knows them; a project that declares none simply carries an empty list.
 */
type OpenableProject = Pick<Project, 'id' | 'name' | 'repository' | 'branch'> & {
    dependencies: ProjectDependency[]
}

/**
 * Closes the dialog without opening anything.
 *
 * The dialog is mounted above the routes and answers back into the screen that opened it, so a screen
 * leaving that project takes its question with it rather than letting it confirm into a tree that is gone.
 */
export const closeProjectDialog = (): void => {
    window.dispatchEvent(new CustomEvent('openProjectModal', { detail: null }))
}

/**
 * Opens a project, asking first when it declares dependencies.
 *
 * A project that declares none has nothing to decide, so it opens straight away, together with its
 * dependencies as before. Otherwise the dialog offers the choice, offers to move the dependencies onto the
 * branch of the project, and warns about the ones that stay off it.
 *
 * @param run opens the project, with whether to open its dependencies too and whether to move them onto its
 *            branch
 */
export const openProjectDialog = (
    project: OpenableProject,
    run: (options: OpenDependenciesOptions) => void
): void => {
    if (project.dependencies.length === 0) {
        // The list can be older than the project: whatever the server finds to open follows it to its branch.
        run({ openDependencies: true, switchDependencies: true })
        return
    }
    window.dispatchEvent(new CustomEvent('openProjectModal', {
        detail: {
            projectName: project.name,
            repository: project.repository,
            branch: project.branch,
            dependencies: project.dependencies,
            onConfirm: run,
        },
    }))
}
