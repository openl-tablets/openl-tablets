import { ForbiddenError, isApiHttpError, NotFoundError } from './apiCall'
import type { ProjectCandidate } from '../types/projects'

/** What the server answers when a project name that several projects carry is used to address one. */
const AMBIGUOUS_NAME = 'openl.error.409.project.identifier.ambiguous.message'

/**
 * Why a link does not lead to one project.
 *
 * `missing`: no project the reader may see answers to it. None carries that id or name, or the reader may not
 * see the one that does.
 *
 * `ambiguous`: several projects carry the name the link gives, listed as candidates to choose from.
 */
export type ProjectLinkProblem =
    | { kind: 'missing' }
    | { kind: 'ambiguous', candidates: ProjectCandidate[] }

/**
 * Tells why reading the project a link names failed, when the link itself is the reason.
 *
 * Any other failure, a server error for instance, is none of the link's doing: it answers null, and the screen
 * reports it as a failure to read the project.
 */
export const projectLinkProblemOf = (error: unknown): ProjectLinkProblem | null => {
    if (error instanceof NotFoundError || error instanceof ForbiddenError) {
        return { kind: 'missing' }
    }
    const payload = isApiHttpError(error) ? error.payload as { code?: unknown, candidates?: unknown } | undefined : null
    if (payload?.code === AMBIGUOUS_NAME && Array.isArray(payload.candidates)) {
        return { kind: 'ambiguous', candidates: payload.candidates as ProjectCandidate[] }
    }
    return null
}
