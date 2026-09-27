import { describe, expect, it } from 'vitest'
import type { RepositoryConfig } from '../../types/repositories'
import { withConfiguredBranch } from './configBranchMarks'

const configured = (branch?: string) => ({ branch }) as RepositoryConfig

describe('withConfiguredBranch', () => {
    it('heads the branches with the configured one the repository does not have yet', () => {
        expect(withConfiguredBranch(configured('main'), ['feature/a'])).toEqual(['main', 'feature/a'])
    })

    it('keeps the branches as they are when the repository has the configured one or none is configured', () => {
        const branches = ['feature/a', 'main']

        expect(withConfiguredBranch(configured('main'), branches)).toBe(branches)
        expect(withConfiguredBranch(configured(), branches)).toBe(branches)
        expect(withConfiguredBranch(undefined, branches)).toBe(branches)
    })
})
