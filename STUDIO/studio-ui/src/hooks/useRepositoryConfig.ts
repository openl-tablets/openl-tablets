import { useEffect, useState } from 'react'
import { getDesignRepositoryConfig, getRepositoryConfig } from '../services/repositories'
import type { RepositoryConfig } from '../types/repositories'

/** Where the settings are read from: an existing project, or a repository a project is created in. */
export interface RepositoryConfigSource {
    projectId?: string | undefined
    repositoryId?: string | undefined
}

/**
 * Settings of the repository a form writes to — the branch and comment rules it suggests values from and
 * validates against.
 *
 * <p>A form of an existing project asks through the project, because access may be granted on the project
 * alone; a create form asks the repository it is about to write to.
 *
 * @returns the settings, or `undefined` while they are loading or unreadable — the form then suggests
 * nothing and accepts anything, exactly as it did before the settings existed
 */
export const useRepositoryConfig = (source: RepositoryConfigSource | null): RepositoryConfig | undefined => {
    const [config, setConfig] = useState<RepositoryConfig | undefined>(undefined)
    const projectId = source?.projectId
    const repositoryId = source?.repositoryId

    useEffect(() => {
        if (!projectId && !repositoryId) {
            setConfig(undefined)
            return
        }
        let current = true
        setConfig(undefined)
        const load = projectId ? getRepositoryConfig(projectId) : getDesignRepositoryConfig(repositoryId!)
        load
            .then(loaded => {
                if (current) {
                    setConfig(loaded)
                }
            })
            .catch(() => {
                // The suggestions are a convenience: the user can still type the values.
            })
        return () => {
            current = false
        }
    }, [projectId, repositoryId])

    return config
}
