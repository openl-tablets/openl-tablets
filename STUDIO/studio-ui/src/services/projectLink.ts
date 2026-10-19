import { isApiHttpError } from './apiCall'

/** The statuses that tell a link leads to no project the reader may see. */
const UNRESOLVED = new Set([403, 404, 410])

/**
 * Whether reading the project a link names failed because the link leads to no project the reader may see: no
 * project has the id, the reader may not see the one that has it, or the project was deleted. A link that names a
 * project by its name, or by an id of an earlier version of OpenL Studio, leads to none.
 *
 * Any other failure, a server error for instance, is none of the link's doing, and the screen reports it as a
 * failure to read the project.
 */
export const isUnresolvedProjectLink = (error: unknown): boolean =>
    isApiHttpError(error) && UNRESOLVED.has(error.status)
