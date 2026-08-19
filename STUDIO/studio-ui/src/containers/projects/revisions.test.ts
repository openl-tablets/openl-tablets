import { revisionLabel, shortRevision } from './revisions'
import type { ProjectRevision } from '../../services/repositories'

const revision = (overrides: Partial<ProjectRevision> = {}): ProjectRevision => ({
    revisionNo: '00f5781f0a32b2ab7f779d305795f20a257a6c1a',
    shortRevisionNo: '00f578',
    createdAt: '2026-08-17T11:28:38Z',
    fullComment: 'Save the project',
    author: { displayName: 'Dillon Rippin' },
    deleted: false,
    technicalRevision: false,
    ...overrides,
})

describe('shortRevision', () => {
    it('keeps the leading characters that identify a revision', () => {
        expect(shortRevision('00f5781f0a32b2ab7f779d305795f20a257a6c1a')).toBe('00f578')
    })

    it('leaves a revision shorter than that untouched', () => {
        expect(shortRevision('12')).toBe('12')
    })

    // A database repository counts its revisions, so 1000123 cut to 100012 would name a different
    // revision — and the same one as its nine neighbours.
    it('keeps a counted revision whole however long it grows', () => {
        expect(shortRevision('1000123')).toBe('1000123')
        expect(shortRevision('1000124')).not.toBe(shortRevision('1000123'))
    })
})

describe('revisionLabel', () => {
    it('opens with the revision, then the author and the time', () => {
        expect(revisionLabel(revision())).toMatch(/^00f578 · Dillon Rippin: /)
    })

    // Two saves within the same minute used to render as the very same text, so neither the user nor
    // the UI test could tell which revision was being picked. See EPBDS-16458.
    it('tells apart two revisions made in the same second', () => {
        const one = revisionLabel(revision({ revisionNo: 'aaaaaa11111' }))
        const other = revisionLabel(revision({ revisionNo: 'bbbbbb22222' }))

        expect(one).not.toBe(other)
    })

    it('shows a counted revision whole, however long it grows', () => {
        expect(revisionLabel(revision({ revisionNo: '1000123' }))).toMatch(/^1000123 · /)
    })

    it('falls back to the email when the author has no display name', () => {
        const label = revisionLabel(revision({ author: { email: 'dillon@example.com' } }))

        expect(label).toMatch(/^00f578 · dillon@example\.com: /)
    })

    it('drops the prefix when the revision has no author at all', () => {
        const { author, ...anonymous } = revision()
        const label = revisionLabel(anonymous)

        expect(label).not.toContain(': ')
        expect(label).toMatch(/^00f578 · /)
    })
})
