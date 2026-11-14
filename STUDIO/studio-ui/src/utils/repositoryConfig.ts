import dayjs from 'dayjs'
import type { RepositoryConfig } from '../types/repositories'
import { commentMoment } from './dateFormat'

/** The server rejects longer comments (`CommentValidator.MAX_COMMENT_LENGTH`). */
const MAX_COMMENT_LENGTH = 255

/**
 * Symbols a branch name keeps from the project name. Mirrors the server's `Comments.newBranch`, which
 * strips everything else before filling the pattern.
 */
const BRANCH_NAME_RESTRICTED = /[^\p{L}\p{Nd}\-$]/gu

/** Characters forbidden by Git refs and by the server's `NewBranchValidator`. */
// The control-char range stops before the ASCII whitespace the whitespace class already matches, so the
// class carries no duplicate characters while matching exactly the same set of characters.
const INVALID_BRANCH_CHARACTERS = /[\u0000-\u0008\u000E-\u001F\u007F\s\\~^:?*[\]{}"<>|]/u

interface TemplateValues {
    projectName?: string | undefined
    username?: string | undefined
}

/** Fills a repository template — `{project-name}`, `{username}` and `{current-date}`. */
export const applyTemplate = (template: string | undefined, values: TemplateValues): string => {
    if (!template) {
        return ''
    }
    return template
        .replaceAll('{project-name}', values.projectName ?? '')
        .replaceAll('{username}', values.username ?? '')
        .replaceAll('{current-date}', dayjs().format('YYYYMMDD'))
}

/** The branch name the repository suggests for a new branch of this project. */
export const suggestBranchName = (
    config: RepositoryConfig | undefined,
    values: TemplateValues
): string => applyTemplate(config?.newBranch?.pattern, {
    ...values,
    projectName: values.projectName?.replace(BRANCH_NAME_RESTRICTED, ''),
})

/**
 * The comment suggested for an action that commits a project: `Project Bank Rating is saved.`, `Project Bank
 * Rating is created.` or `Copied from: Bank Rating.`, naming the copied project.
 *
 * The server writes the same comments (`Comments`) when a request carries none, so a commit reads alike whether its
 * comment was typed or not. A comment is text stored in the repository and read by anyone, so it is not translated.
 */
export const suggestComment = (action: 'save' | 'create' | 'copy', projectName: string | undefined): string => {
    const name = projectName ?? ''
    switch (action) {
        case 'save':
            return `Project ${name} is saved.`
        case 'create':
            return `Project ${name} is created.`
        case 'copy':
            return `Copied from: ${name}.`
    }
}

/**
 * The comment suggested for saving a project opened on an older revision, which restores that revision: it names the
 * author and the moment of the revision, `Restored from revision of John on 06/22/2020 09:02:42 PM.`
 */
export const suggestRestoreComment = (author: string | undefined, modifiedAt: string | undefined): string =>
    `Restored from revision of ${author?.trim() ?? ''} on ${commentMoment(modifiedAt)}.`

/**
 * Checks a branch name against the Git naming rules and the expression the repository configures, so an
 * obviously wrong name never reaches the server. The server repeats both checks before creating a ref.
 *
 * @returns the message to show, or null when the name is acceptable
 */
export const validateBranchName = (
    name: string,
    config: RepositoryConfig | undefined,
    emptyMessage: string,
    invalidMessage: string
): string | null => {
    const trimmed = name.trim()
    if (!trimmed) {
        return emptyMessage
    }
    if (!isValidGitBranchName(trimmed)) {
        return invalidMessage
    }
    // The repository may word the rejection itself; that message is the one the server would show.
    return matches(trimmed, config?.newBranch?.namePattern)
        ? null
        : config?.newBranch?.invalidNameHint ?? invalidMessage
}

/** Mirrors the ref-format and structural checks applied by the server's `NewBranchValidator`. */
const isValidGitBranchName = (name: string): boolean => {
    if (name.length < 2 || name === '@' || INVALID_BRANCH_CHARACTERS.test(name) ||
        name.includes('..') || name.includes('@{')) {
        return false
    }
    return name.split('/').every(part =>
        part.length > 0 &&
        !part.startsWith('.') &&
        !part.endsWith('.') &&
        !part.endsWith('.lock')
    )
}

/**
 * Checks a commit comment against the expression the repository configures and the maximum length the
 * server accepts.
 *
 * @returns the message to show, or null when the comment is acceptable
 */
export const validateComment = (
    comment: string,
    config: RepositoryConfig | undefined,
    tooLongMessage: string,
    invalidMessage: string
): string | null => {
    if (comment.length > MAX_COMMENT_LENGTH) {
        return tooLongMessage
    }
    return matches(comment, config?.comment?.userMessagePattern)
        ? null
        : config?.comment?.invalidUserMessageHint ?? invalidMessage
}

/** An unset or unusable expression accepts everything, exactly as it does server-side. */
const matches = (value: string, pattern: string | undefined): boolean => {
    if (!pattern) {
        return true
    }
    try {
        return new RegExp(`^(?:${pattern})$`, 'u').test(value)
    } catch {
        return true
    }
}
