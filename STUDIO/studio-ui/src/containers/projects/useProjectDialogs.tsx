import { useEffect, useState, type ReactNode } from 'react'
import type { Project } from '../../types/projects'
import type { Repository } from '../../types/repositories'
import { getDesignRepositories } from '../../services/repositories'
import { LOCAL_LOAD_API_OPTIONS } from '../../services/apiCall'
import { creatableRepositories } from '../../utils/repositoryFeatures'
import { SaveProjectModal } from './SaveProjectModal'
import { CopyProjectModal } from './CopyProjectModal'
import { openMergeDialog } from './branchDialogs'
import type { BusyId } from './projectActions'

/** The actions of a project that answer with a dialog, wherever the project is shown from. */
export interface ProjectDialogActions {
    save: () => void
    copy: () => void
    sync: () => void
    deploy: () => void
}

interface ProjectDialogOptions {
    /** Reads the project again, after an action changed it. */
    onChanged: () => void
    /**
     * Marks the screen busy for as long as the action runs, when the screen shows such a state. The read
     * that follows an action is the slower half of it, so it is covered too.
     */
    busy?: ((id: BusyId, run: () => Promise<unknown>) => void) | undefined
}

/**
 * The project's own actions as both the project screen and the module editor offer them: one place decides
 * what each of them opens and what happens once it is done, so a project is never saved one way on one
 * screen and another way on the next.
 *
 * <p>What the caller draws is up to it — which actions are offered at all comes from the project's
 * capabilities, read through {@link isActionAvailable}.
 */
export const useProjectDialogs = (
    project: Project | null,
    { onChanged, busy }: ProjectDialogOptions
): { actions: ProjectDialogActions, dialogs: ReactNode } => {
    const [saveOpen, setSaveOpen] = useState(false)
    const [copySource, setCopySource] = useState<Project | null>(null)
    // Read only when the copy dialog first opens; `null` until then.
    const [repositories, setRepositories] = useState<Repository[] | null>(null)

    useEffect(() => {
        if (copySource === null || repositories !== null) {
            return
        }
        getDesignRepositories(LOCAL_LOAD_API_OPTIONS)
            .then(setRepositories)
            .catch(() => setRepositories([]))
    }, [copySource, repositories])

    /** Runs the work under the screen's busy state when it has one, and plainly when it has none. */
    const run = (id: BusyId, work: () => Promise<unknown>) => {
        if (busy) {
            busy(id, work)
        } else {
            void work()
        }
    }

    const actions: ProjectDialogActions = {
        save: () => setSaveOpen(true),
        copy: () => setCopySource(project),
        // The dialog reads the project's branches before it can be shown — a whole round trip the button
        // holds its spinner for, instead of dead-ending until a dialog appears unannounced.
        sync: () => {
            if (project) {
                run('sync', () => openMergeDialog(project, () => run('sync', async () => onChanged())))
            }
        },
        deploy: () => window.dispatchEvent(new CustomEvent('openDeployModal', { detail: project })),
    }

    // Each dialog hands the project back busy: its own spinner covers the request, and the busy state
    // covers the read that follows it.
    const dialogs = (
        <>
            <SaveProjectModal
                onClose={() => setSaveOpen(false)}
                onSaved={() => run('save', async () => onChanged())}
                open={saveOpen}
                project={project}
            />
            <CopyProjectModal
                onClose={() => setCopySource(null)}
                onCopied={() => run('copy', async () => onChanged())}
                open={copySource !== null}
                project={copySource}
                repositories={creatableRepositories(repositories ?? [])}
            />
        </>
    )

    return { actions, dialogs }
}
