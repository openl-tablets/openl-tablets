import dayjs from 'dayjs'
import { describe, expect, it } from 'vitest'
import {
    applyTemplate,
    suggestBranchName,
    suggestComment,
    validateBranchName,
    validateComment,
} from './repositoryConfig'
import type { RepositoryConfig } from '../types/repositories'

const config = (overrides: Partial<RepositoryConfig> = {}): RepositoryConfig => ({
    newBranch: { pattern: '{project-name}/{username}/{current-date}' },
    comment: { templates: { copy: 'Copied from: {project-name}.' } },
    ...overrides,
})

describe('applyTemplate', () => {
    it('fills the project, user and date placeholders', () => {
        const today = dayjs().format('YYYYMMDD')

        expect(applyTemplate('{project-name}/{username}/{current-date}', { projectName: 'Alpha', username: 'jdoe' }))
            .toBe(`Alpha/jdoe/${today}`)
    })

    it('drops a placeholder that has no value', () => {
        expect(applyTemplate('{project-name}/{username}', { projectName: 'Alpha' })).toBe('Alpha/')
    })

    it('returns an empty string when the repository configures no template', () => {
        expect(applyTemplate(undefined, { projectName: 'Alpha' })).toBe('')
    })
})

describe('suggestBranchName', () => {
    it('keeps only the symbols a branch name accepts from the project name', () => {
        const today = dayjs().format('YYYYMMDD')

        expect(suggestBranchName(config(), { projectName: 'Example 1 - Bank Rating', username: 'jdoe' }))
            .toBe(`Example1-BankRating/jdoe/${today}`)
    })

    it('suggests nothing for a repository without branches', () => {
        expect(suggestBranchName(config({ newBranch: undefined }), { projectName: 'Alpha' })).toBe('')
    })
})

describe('suggestComment', () => {
    it('builds the comment from the repository template', () => {
        expect(suggestComment(config(), 'copy', 'Alpha')).toBe('Copied from: Alpha.')
    })

    it('suggests nothing when the repository configures no copy template', () => {
        expect(suggestComment(config({ comment: { templates: {} } }), 'copy', 'Alpha')).toBe('')
    })
})

describe('validateBranchName', () => {
    const restricted = config({ newBranch: { namePattern: 'release/.+', invalidNameHint: 'Use release/<name>' } })

    it('rejects a blank name', () => {
        expect(validateBranchName('  ', config(), 'Name is required', 'Name is invalid')).toBe('Name is required')
    })

    it('rejects a name the configured expression forbids', () => {
        expect(validateBranchName('feature/rates', restricted, 'Name is required', 'Name is invalid')).toBe('Use release/<name>')
    })

    it('accepts a name the configured expression allows', () => {
        expect(validateBranchName('release/2026', restricted, 'Name is required', 'Name is invalid')).toBeNull()
    })

    it.each([
        'anything goes',
        'feature[1]',
        '.feature',
        'feature/.rates',
        'feature..rates',
        'feature@{rates',
        'feature.lock/rates',
        'feature/',
    ])('rejects the invalid Git branch name %s', name => {
        expect(validateBranchName(name, config(), 'Name is required', 'Name is invalid')).toBe('Name is invalid')
    })

    it('accepts a valid Git branch name when the repository configures no expression', () => {
        expect(validateBranchName('feature/rates', config(), 'Name is required', 'Name is invalid')).toBeNull()
    })
})

describe('validateComment', () => {
    const restricted = config({
        comment: { userMessagePattern: 'EPBDS-\\d+.*', invalidUserMessageHint: 'Start with a ticket', templates: {} },
    })

    it('rejects a comment the configured expression forbids', () => {
        expect(validateComment('done', restricted, 'Too long', 'Invalid')).toBe('Start with a ticket')
    })

    it('accepts a comment the configured expression allows', () => {
        expect(validateComment('EPBDS-1 done', restricted, 'Too long', 'Invalid')).toBeNull()
    })

    it('rejects a comment longer than the server accepts', () => {
        expect(validateComment('x'.repeat(256), config(), 'Too long', 'Invalid')).toBe('Too long')
    })

    it('accepts everything when the repository does not customize comments', () => {
        expect(validateComment('anything', config(), 'Too long', 'Invalid')).toBeNull()
    })

    it('accepts everything when the configured expression cannot be compiled', () => {
        const broken = config({ comment: { userMessagePattern: '([', templates: {} } })

        expect(validateComment('anything', broken, 'Too long', 'Invalid')).toBeNull()
    })
})
