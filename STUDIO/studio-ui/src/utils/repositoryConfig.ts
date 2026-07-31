import dayjs from 'dayjs'
import type { RepositoryConfig } from '../types/repositories'

/** The server rejects longer comments (`CommentValidator.MAX_COMMENT_LENGTH`). */
const MAX_COMMENT_LENGTH = 255

/**
 * Symbols a branch name keeps from the project name. Mirrors the server's `Comments.newBranch`, which
 * strips everything else before filling the pattern.
 */
const BRANCH_NAME_RESTRICTED = /[^\p{L}\p{Nd}\-$]/gu

/** Characters forbidden by Git refs and by the server's `NewBranchValidator`. */
const INVALID_BRANCH_CHARACTERS = /[\u0000-\u0020\u007F\s\\~^:?*[\]{}"<>|]/u

interface TemplateValues {
    projectName?: string | undefined
    username?: string | undefined
    revision?: string | undefined
    author?: string | undefined
    datetime?: string | undefined
}

/**
 * Fills a repository template — `{project-name}`, `{username}`, `{current-date}`, and, for a restored
 * revision, `{revision}`, `{author}` and `{datetime}` — the way the server does when it generates the
 * value itself.
 */
export const applyTemplate = (template: string | undefined, values: TemplateValues): string => {
    if (!template) {
        return ''
    }
    return template
        .replaceAll('{project-name}', values.projectName ?? '')
        .replaceAll('{username}', values.username ?? '')
        .replaceAll('{current-date}', dayjs().format('YYYYMMDD'))
        .replaceAll('{revision}', values.revision ?? '')
        .replaceAll('{author}', values.author ?? '')
        .replaceAll('{datetime}', values.datetime ?? '')
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
 * The comment the repository suggests for an action that commits. An action the repository configures no
 * template for starts from an empty comment.
 */
export const suggestComment = (
    config: RepositoryConfig | undefined,
    action: keyof NonNullable<RepositoryConfig['comment']>['templates'],
    values: TemplateValues | string | undefined
): string => applyTemplate(
    config?.comment?.templates?.[action],
    typeof values === 'string' || values === undefined ? { projectName: values } : values
)

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
