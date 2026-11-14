import dayjs from 'dayjs'
import { describe, expect, it } from 'vitest'
import {
    applyTemplate,
    suggestBranchName,
    suggestComment,
    suggestRestoreComment,
    validateBranchName,
    validateComment,
} from './repositoryConfig'
import type { RepositoryConfig } from '../types/repositories'

const config = (overrides: Partial<RepositoryConfig> = {}): RepositoryConfig => ({
    newBranch: { pattern: '{project-name}/{username}/{current-date}' },
    comment: {},
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
    it('names the project of every action that commits, as the server does', () => {
        expect(suggestComment('save', 'Bank Rating')).toBe('Project Bank Rating is saved.')
        expect(suggestComment('create', 'Bank Rating')).toBe('Project Bank Rating is created.')
        expect(suggestComment('copy', 'Bank Rating')).toBe('Copied from: Bank Rating.')
    })

    it('writes a missing project as an empty text', () => {
        expect(suggestComment('save', undefined)).toBe('Project  is saved.')
    })
})

describe('suggestRestoreComment', () => {
    it('names the author and the moment of the restored revision in UTC', () => {
        expect(suggestRestoreComment(' John ', '2020-06-23T00:02:42.517+03:00'))
            .toBe('Restored from revision of John on 06/22/2020 09:02:42 PM.')
        expect(suggestRestoreComment(undefined, undefined)).toBe('Restored from revision of  on .')
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
        'feature"rates',
        'feature<rates',
        'feature>rates',
        'feature|rates',
        'feature\u00A0rates',
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
        comment: { userMessagePattern: 'EPBDS-\\d+.*', invalidUserMessageHint: 'Start with a ticket' },
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

    it('accepts everything when the repository sets no pattern', () => {
        expect(validateComment('anything', config(), 'Too long', 'Invalid')).toBeNull()
    })

    it('accepts everything when the configured expression cannot be compiled', () => {
        const broken = config({ comment: { userMessagePattern: '([' } })

        expect(validateComment('anything', broken, 'Too long', 'Invalid')).toBeNull()
    })
})
