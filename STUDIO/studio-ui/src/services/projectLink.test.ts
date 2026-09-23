import { describe, expect, it } from 'vitest'
import { ApiHttpError, ForbiddenError, NotFoundError } from './apiCall'
import { projectLinkProblemOf } from './projectLink'

const AMBIGUOUS_NAME = 'openl.error.409.project.identifier.ambiguous.message'

const CANDIDATES = [
    { id: 'ZGVzaWduOkhlbGxv', name: 'Hello', repository: 'design', repositoryName: 'Design' },
    { id: 'ZGVzaWduMTpoZWxsbw==', name: 'hello', repository: 'design1', repositoryName: 'Design1' },
]

describe('projectLinkProblemOf', () => {
    it('reads a project that is not there, or not visible to the reader, as a link to nothing', () => {
        expect(projectLinkProblemOf(new NotFoundError())).toEqual({ kind: 'missing' })
        expect(projectLinkProblemOf(new ForbiddenError())).toEqual({ kind: 'missing' })
    })

    it('reads a name several projects carry as a choice between them', () => {
        const error = new ApiHttpError(409, 'The project name is ambiguous.', {
            code: AMBIGUOUS_NAME,
            message: 'The project name is ambiguous.',
            candidates: CANDIDATES,
        })

        expect(projectLinkProblemOf(error)).toEqual({ kind: 'ambiguous', candidates: CANDIDATES })
    })

    it('leaves any other failure to be reported as a failure to read the project', () => {
        const otherConflict = new ApiHttpError(409, 'Modified', { code: 'openl.error.409.project.close.modified.message' })
        const noCandidates = new ApiHttpError(409, 'Ambiguous', { code: AMBIGUOUS_NAME })

        expect(projectLinkProblemOf(otherConflict)).toBeNull()
        expect(projectLinkProblemOf(noCandidates)).toBeNull()
        expect(projectLinkProblemOf(new ApiHttpError(500, 'boom'))).toBeNull()
        expect(projectLinkProblemOf(new Error('offline'))).toBeNull()
    })
})
