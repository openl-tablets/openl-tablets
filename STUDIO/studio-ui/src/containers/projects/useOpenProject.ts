import { useCallback, useEffect, useState } from 'react'
import { setProjectStatus } from '../../services/repositories'
import type { Project } from '../../types/projects'
import { closeProjectDialog, openProjectDialog } from './openProjectDialog'

/**
 * Opens a closed project from a screen of it, asking about its dependencies first, as the Projects screen does.
 *
 * <p>The question is closed when the screen leaves the project, so it never opens a project the reader left.
 *
 * @param project  the project, or null while it is read
 * @param onOpened reads the project again, once it is open
 * @param onFailed tells why the project could not be opened
 * @returns whether the project is being opened, and the action that opens it
 */
export const useOpenProject = (
    project: Project | null,
    onOpened: () => void,
    onFailed: (error: unknown) => void
) => {
    const [opening, setOpening] = useState(false)
    const projectId = project?.id
    useEffect(() => closeProjectDialog, [projectId])
    const open = useCallback(() => {
        if (!project) {
            return
        }
        openProjectDialog({ ...project, dependencies: project.dependencies ?? []}, openDependencies => {
            setOpening(true)
            setProjectStatus(project.id, 'OPENED', { openDependencies })
                .then(onOpened)
                .catch(onFailed)
                .finally(() => setOpening(false))
        })
    }, [onFailed, onOpened, project])
    return { opening, open }
}
