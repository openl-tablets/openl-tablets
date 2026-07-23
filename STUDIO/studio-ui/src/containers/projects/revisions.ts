import { useEffect, useState } from 'react'
import { errorMessage } from '../../utils/errorMessage'
import { getProjectRevisions, REVISIONS_PAGE_SIZE, type ProjectRevision } from '../../services/repositories'
import type { Project } from '../../types/projects'
import { formatDateTime } from '../../utils/dateFormat'

/** How many characters of a revision identify it on screen. */
const SHORT_REVISION_LENGTH = 6

/**
 * The technical revision as it is shown: shortened, the way every screen shows it.
 *
 * The full value stays in the model — it is what the server is asked about and what a user copies.
 */
export const shortRevision = (revision: string): string => revision.slice(0, SHORT_REVISION_LENGTH)

/**
 * How a revision reads to a business user: who changed the project and when.
 *
 * The technical revision number is what travels to the server; it is never what the user picks by.
 */
export const revisionLabel = (revision: ProjectRevision): string => {
    const author = revision.author?.displayName ?? revision.author?.email
    const changedAt = formatDateTime(revision.createdAt) ?? revision.createdAt
    return author ? `${author}: ${changedAt}` : changedAt
}

export interface ProjectRevisions {
    /** The revisions, newest first; null while they are loading. */
    revisions: ProjectRevision[] | null
    /** Ready-made dropdown options, labelled the way a business user reads a revision. */
    options: Array<{ value: string, label: string }>
    /** Why the history could not be read, if it could not. */
    error: string | null
}

/**
 * The latest page of a project's history, loaded while the given dialog is open.
 *
 * Every dialog that lets the user reach back into the history — export, open, copy — shows the same page,
 * so they all ask for it the same way.
 */
export const useProjectRevisions = (project: Project | null, enabled: boolean): ProjectRevisions => {
    const [revisions, setRevisions] = useState<ProjectRevision[] | null>(null)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        if (!enabled || !project) {
            return
        }
        let current = true
        setRevisions(null)
        setError(null)
        getProjectRevisions(project.repository, project.name, project.branch || null, { size: REVISIONS_PAGE_SIZE })
            .then(page => current && setRevisions(page.content))
            .catch(e => {
                if (current) {
                    // An empty history renders the dialog's empty state instead of spinning forever.
                    setRevisions([])
                    setError(errorMessage(e))
                }
            })
        return () => {
            current = false
        }
    }, [enabled, project])

    return {
        revisions,
        options: (revisions ?? []).map(revision => ({
            value: revision.revisionNo,
            label: revisionLabel(revision),
        })),
        error,
    }
}
