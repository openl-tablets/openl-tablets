import { beforeEach, describe, expect, it, vi } from 'vitest'
import apiCall from './apiCall'
import { getProjectAcl, searchProjectAclSubjects } from './acl'

vi.mock('./apiCall', () => ({
    default: vi.fn(),
    asArray: (value: unknown) => Array.isArray(value) ? value : [],
}))

describe('acl service', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('requests project-scoped ACL subject suggestions', async () => {
        vi.mocked(apiCall).mockResolvedValue(['jane'])

        const result = await searchProjectAclSubjects('abc=', true, ' ja ', 7)

        expect(result).toEqual(['jane'])
        expect(apiCall).toHaveBeenCalledWith(
            '/acls/projects/abc%3D/subjects?principal=true&search=ja&pageSize=7',
            undefined,
            { throwError: true }
        )
    })

    it('requests inherited project ACL entries when requested', async () => {
        vi.mocked(apiCall).mockResolvedValue([
            { role: 'VIEWER', sid: { sid: 'jane', principal: true }, source: 'REPOSITORY' },
        ])

        const result = await getProjectAcl('abc=', { inherited: true })

        expect(result).toEqual([
            { role: 'VIEWER', source: 'REPOSITORY', sub: { sid: 'jane', principal: true } },
        ])
        expect(apiCall).toHaveBeenCalledWith(
            '/acls/projects/abc%3D?inherited=true',
            undefined,
            { throwError: true }
        )
    })
})
