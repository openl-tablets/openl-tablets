import type { Project, ProjectDependency } from '../../types/projects'

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
 * dependencies as before. Otherwise the dialog offers the choice and warns about the dependencies the
 * branch of the project does not hold.
 *
 * @param run opens the project, with whether to open its dependencies too
 */
export const openProjectDialog = (
    project: OpenableProject,
    run: (openDependencies: boolean) => void
): void => {
    if (project.dependencies.length === 0) {
        run(true)
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
