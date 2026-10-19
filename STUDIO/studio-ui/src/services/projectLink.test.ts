import { describe, expect, it } from 'vitest'
import { ApiHttpError, ForbiddenError, NotFoundError } from './apiCall'
import { isUnresolvedProjectLink } from './projectLink'

describe('isUnresolvedProjectLink', () => {
    it('reads a project that is not there, or not visible to the reader, as a link to nothing', () => {
        expect(isUnresolvedProjectLink(new NotFoundError())).toBe(true)
        expect(isUnresolvedProjectLink(new ForbiddenError())).toBe(true)
    })

    it('reads a project that was deleted as a link to nothing', () => {
        expect(isUnresolvedProjectLink(new ApiHttpError(410, 'The project was deleted.', {
            code: 'openl.error.410.project.deleted.message',
        }))).toBe(true)
    })

    it('leaves any other failure to be reported as a failure to read the project', () => {
        expect(isUnresolvedProjectLink(new ApiHttpError(409, 'Modified', { code: 'openl.error.409.project.close.modified.message' })))
            .toBe(false)
        expect(isUnresolvedProjectLink(new ApiHttpError(500, 'boom'))).toBe(false)
        expect(isUnresolvedProjectLink(new Error('offline'))).toBe(false)
    })
})
