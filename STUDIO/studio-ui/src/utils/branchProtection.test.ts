import { describe, expect, it } from 'vitest'
import { isProtectedBranch } from './branchProtection'

describe('isProtectedBranch', () => {
    it('is never protected without a branch or patterns', () => {
        expect(isProtectedBranch(undefined, ['**'])).toBe(false)
        expect(isProtectedBranch('main', undefined)).toBe(false)
        expect(isProtectedBranch('main', [])).toBe(false)
    })

    it('matches a plain name exactly', () => {
        expect(isProtectedBranch('master', ['master'])).toBe(true)
        expect(isProtectedBranch('master', ['main'])).toBe(false)
        expect(isProtectedBranch('release/master', ['master'])).toBe(false)
    })

    it('reads * as any simple name and ** as any branch, minding the path separator', () => {
        expect(isProtectedBranch('release-21.10', ['*'])).toBe(true)
        // A single star stops at a path separator; a double star crosses it.
        expect(isProtectedBranch('release/21.10', ['*'])).toBe(false)
        expect(isProtectedBranch('release/21.10', ['**'])).toBe(true)
        expect(isProtectedBranch('feature/x/y', ['release/*'])).toBe(false)
        expect(isProtectedBranch('release/21.10', ['release/*'])).toBe(true)
    })

    it('reads the dot, brace choice and single-character patterns', () => {
        expect(isProtectedBranch('build.10', ['*.*'])).toBe(true)
        expect(isProtectedBranch('build.10', ['*.{10,11}'])).toBe(true)
        expect(isProtectedBranch('build.12', ['*.{10,11}'])).toBe(false)
        expect(isProtectedBranch('foo.1', ['foo.?'])).toBe(true)
        expect(isProtectedBranch('foo.12', ['foo.?'])).toBe(false)
    })

    it('protects a branch when any one of several patterns matches', () => {
        expect(isProtectedBranch('develop', ['master', 'develop', 'release/*'])).toBe(true)
        expect(isProtectedBranch('topic/x', ['master', 'develop', 'release/*'])).toBe(false)
    })

    it('reads a character set, including a negated one', () => {
        expect(isProtectedBranch('b', ['branch-[abc]'])).toBe(false)
        expect(isProtectedBranch('branch-b', ['branch-[abc]'])).toBe(true)
        expect(isProtectedBranch('branch-d', ['branch-[abc]'])).toBe(false)
        expect(isProtectedBranch('branch-d', ['branch-[!abc]'])).toBe(true)
        expect(isProtectedBranch('branch-a', ['branch-[!abc]'])).toBe(false)
    })

    it('reads a leading caret in a set as a literal, not negation', () => {
        // A glob set negates with '!', so '^' is an ordinary member — the set is { ^, a, b }.
        expect(isProtectedBranch('branch-^', ['branch-[^ab]'])).toBe(true)
        expect(isProtectedBranch('branch-a', ['branch-[^ab]'])).toBe(true)
        expect(isProtectedBranch('branch-c', ['branch-[^ab]'])).toBe(false)
    })

    it('marks nothing for a pattern that cannot be compiled, without throwing', () => {
        // A reversed range is not a valid character class; it must fall back to no match.
        expect(isProtectedBranch('main', ['[z-a]'])).toBe(false)
    })
})
