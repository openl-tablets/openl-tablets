import { useCallback, useEffect, useRef, useState } from 'react'
import { errorMessage } from '../../utils/errorMessage'
import { getProjectRevisions, REVISIONS_PAGE_SIZE, type ProjectRevision, type RevisionPage } from '../../services/repositories'
import { getFileRevisions } from '../../services/files'
import type { Project } from '../../types/projects'
import { formatDateTime } from '../../utils/dateFormat'

/** How many characters of a revision hash identify it on screen. */
const SHORT_REVISION_LENGTH = 6

/** A revision a repository counts rather than hashes, such as the row id a database repository assigns. */
const NUMBERED_REVISION = /^-?\d+$/

/**
 * The technical revision as it is shown: a hash cut to its leading characters, a counted revision whole.
 *
 * Cutting a counted revision would name a different one — revision 1000123 shown as 100012 — and give
 * the same name to its nine neighbours, so only a hash is shortened.
 *
 * The full value stays in the model — it is what the server is asked about and what a user copies.
 */
export const shortRevision = (revision: string): string => NUMBERED_REVISION.test(revision)
    ? revision
    : revision.slice(0, SHORT_REVISION_LENGTH)

/**
 * How a revision reads to a business user: which revision it is, then who changed the project and when.
 *
 * The revision opens the label because it is the only part that is always unique, and because the
 * dropdowns showing it are narrow: a label too long for the field is cut at the end, so anything
 * placed after the author and the date would be the first thing lost.
 */
export const revisionLabel = (revision: ProjectRevision): string => {
    const author = revision.author?.displayName ?? revision.author?.email
    const changedAt = formatDateTime(revision.createdAt) ?? revision.createdAt
    const changedBy = author ? `${author}: ${changedAt}` : changedAt
    return `${shortRevision(revision.revisionNo)} · ${changedBy}`
}

export interface ProjectRevisions {
    /** The revisions loaded so far, newest first; null while the first page is loading. */
    revisions: ProjectRevision[] | null
    /** Ready-made dropdown options, labelled the way a business user reads a revision. */
    options: Array<{ value: string, label: string }>
    /** Why the history could not be read, if it could not. */
    error: string | null
    /** Whether the repository holds revisions older than the ones loaded. */
    hasMore: boolean
    /** Appends the next page of older revisions. */
    loadMore: () => void
    /** Whether that next page is on its way. */
    loadingMore: boolean
}

/**
 * A history, loaded a page at a time while the given dialog is open.
 *
 * Every dialog that lets the user reach back into the history — export, open, copy — reads it the same way.
 * The newest page arrives first and the rest is fetched on demand, so a long history stays reachable without
 * the dialog waiting for all of it.
 *
 * Naming a file reads that file's own history instead of the project's: only the revisions that changed it,
 * with the revision that removed it left out, so every revision offered is one the file can be read from.
 */
export const useProjectRevisions = (
    project: Project | null,
    enabled: boolean,
    filePath?: string
): ProjectRevisions => {
    const [revisions, setRevisions] = useState<ProjectRevision[] | null>(null)
    const [error, setError] = useState<string | null>(null)
    const [lastPage, setLastPage] = useState<RevisionPage | null>(null)
    const [loadingMore, setLoadingMore] = useState(false)
    // Guards the appends: a dialog reopened on another project must not collect the pages of the old one.
    const generation = useRef(0)

    const readPage = useCallback((page: number): Promise<RevisionPage> => {
        const query = { size: REVISIONS_PAGE_SIZE, page }
        return filePath
            ? getFileRevisions(project!.id, filePath, query)
            : getProjectRevisions(project!.id, query)
    }, [project?.id, filePath])

    useEffect(() => {
        // Bumped before the guard: a page still in flight when the dialog closes must not land in the
        // state the next project's dialog opens on.
        const current = ++generation.current
        if (!enabled || !project) {
            setRevisions(null)
            setError(null)
            setLastPage(null)
            setLoadingMore(false)
            return
        }
        setRevisions(null)
        setError(null)
        setLastPage(null)
        setLoadingMore(false)
        readPage(0)
            .then(page => {
                if (current === generation.current) {
                    setRevisions(page.content)
                    setLastPage(page)
                }
            })
            .catch(e => {
                if (current === generation.current) {
                    // An empty history renders the dialog's empty state instead of spinning forever.
                    setRevisions([])
                    setError(errorMessage(e))
                }
            })
    }, [enabled, project?.id, filePath])

    // A total says outright whether anything is left; without one, a full page means there may be.
    const loaded = revisions?.length ?? 0
    const hasMore = lastPage !== null && (lastPage.total !== null
        ? loaded < lastPage.total
        : lastPage.numberOfElements === lastPage.pageSize && lastPage.numberOfElements > 0)

    const loadMore = () => {
        if (!project || !lastPage || loadingMore) {
            return
        }
        const current = generation.current
        setLoadingMore(true)
        readPage(lastPage.pageNumber + 1)
            .then(page => {
                if (current === generation.current) {
                    setRevisions(previous => [...(previous ?? []), ...page.content])
                    setLastPage(page)
                }
            })
            .catch(e => {
                if (current === generation.current) {
                    setError(errorMessage(e))
                }
            })
            .finally(() => {
                if (current === generation.current) {
                    setLoadingMore(false)
                }
            })
    }

    return {
        revisions,
        options: (revisions ?? []).filter(revision => !revision.deleted).map(revision => ({
            value: revision.revisionNo,
            label: revisionLabel(revision),
        })),
        error,
        hasMore,
        loadMore,
        loadingMore,
    }
}
