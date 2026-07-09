import type { Repository } from '../types/repositories'

export const supportsBranches = (repository: Pick<Repository, 'features'> | null | undefined): boolean =>
    repository?.features?.branches ?? false

export const supportsRevisionSearch = (repository: Pick<Repository, 'features'> | null | undefined): boolean =>
    repository?.features?.searchable ?? false

export const supportsMappedFolders = (repository: Pick<Repository, 'features'> | null | undefined): boolean =>
    repository?.features?.mappedFolders ?? false
