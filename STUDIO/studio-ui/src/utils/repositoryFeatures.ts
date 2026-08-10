import type { Repository } from '../types/repositories'

export const supportsBranches = (repository: Pick<Repository, 'features'> | null | undefined): boolean =>
    repository?.features?.branches ?? false

/**
 * The repositories a project may be written into — the target list every dialog that creates or copies a
 * project picks from. A repository the user cannot create in is not a destination, so it is never offered.
 */
export const creatableRepositories = (repositories: Repository[] | null | undefined): Repository[] =>
    (repositories ?? []).filter(repository => repository.capabilities?.canCreateProject)

export const supportsRevisionSearch = (repository: Pick<Repository, 'features'> | null | undefined): boolean =>
    repository?.features?.searchable ?? false

export const supportsMappedFolders = (repository: Pick<Repository, 'features'> | null | undefined): boolean =>
    repository?.features?.mappedFolders ?? false
