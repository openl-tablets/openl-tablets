import { ForbiddenError, NotFoundError } from './apiCall'

/**
 * Whether reading the project a link names failed because the link leads to no project the reader may see: no
 * project has the id, or the reader may not see the one that has it. A link that names a project by its name, or
 * by an id of an earlier version of OpenL Studio, leads to none.
 *
 * Any other failure, a server error for instance, is none of the link's doing, and the screen reports it as a
 * failure to read the project.
 */
export const isUnresolvedProjectLink = (error: unknown): boolean =>
    error instanceof NotFoundError || error instanceof ForbiddenError
