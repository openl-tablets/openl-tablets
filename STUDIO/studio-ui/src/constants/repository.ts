export enum RepositoryType {
    DESIGN = 'DESIGN',
    PROD = 'PROD',
}

/**
 * Map of repository types to their Base64-encoded root repository IDs.
 * These IDs are used to identify root-level repository ACL entries.
 * 
 * Decoded values:
 * - 'REVTSUdO' = 'DESIGN' (Base64)
 * - 'UFJPRA==' = 'PROD' (Base64)
 */
export const ROOT_REPOSITORY_ID_MAP: Record<RepositoryType, string> = {
    [RepositoryType.DESIGN]: 'REVTSUdO', // Base64('DESIGN')
    [RepositoryType.PROD]: 'UFJPRA=='    // Base64('PROD')
}
