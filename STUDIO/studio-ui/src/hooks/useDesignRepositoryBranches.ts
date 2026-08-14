import { useEffect, useState } from 'react'
import { getDesignRepositoryBranches } from '../services/repositories'
import { errorHandler } from '../utils/errorHandling'

/** The branches of a design repository, and whether they are still being read. */
export interface DesignRepositoryBranches {
    branches: string[]
    loading: boolean
}

/**
 * The branches a form may write to, read from the repository it writes to.
 *
 * <p>A heavy repository takes seconds to answer, so the reading is reported alongside the branches: until
 * it finishes an empty list means "not known yet" rather than "this repository has no branch", and the
 * form can say so instead of offering nothing.
 *
 * <p>The answer always belongs to the repository being asked about: a repository that has not answered yet
 * reads as still loading from its very first render, and never borrows the branches read for the previous
 * one.
 *
 * @param repositoryId the repository to read, or `null` while the form has none to ask about
 * @returns the branches, empty while they are loading or unreadable — a failed read leaves the form to
 * accept a branch name typed by hand, exactly as it did before the listing existed
 */
export const useDesignRepositoryBranches = (repositoryId: string | null): DesignRepositoryBranches => {
    // What was read, and the repository it was read from; the two travel together so that neither can be
    // told about the other's repository.
    const [read, setRead] = useState<{ repositoryId: string, branches: string[] } | null>(null)

    useEffect(() => {
        if (!repositoryId) {
            return
        }
        let current = true
        getDesignRepositoryBranches(repositoryId)
            .then(branches => {
                if (current) {
                    setRead({ repositoryId, branches })
                }
            })
            .catch(error => {
                // The listing is a convenience — the form still accepts a branch name typed by hand — but a
                // repository that cannot be read is worth keeping for support rather than losing in silence.
                errorHandler.logError(error instanceof Error ? error : new Error(String(error)))
                if (current) {
                    setRead({ repositoryId, branches: []})
                }
            })
        return () => {
            current = false
        }
    }, [repositoryId])

    return read?.repositoryId === repositoryId
        ? { branches: read.branches, loading: false }
        : { branches: [], loading: !!repositoryId }
}
