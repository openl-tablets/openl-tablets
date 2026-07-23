import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Input } from 'antd'
import { FieldError } from '../../components/FieldError'
import { FieldRow } from '../../components/FieldRow'
import { validateComment } from '../../utils/repositoryConfig'
import type { RepositoryConfig } from '../../types/repositories'

/**
 * The message to show for a commit comment, or null when it is acceptable. Applies the expression the
 * repository configures, so an obviously wrong comment never reaches the server.
 */
export const useCommentError = (comment: string, config: RepositoryConfig | undefined): string | null => {
    const { t } = useTranslation('repository')
    return useMemo(
        () => validateComment(comment, config, t('browser.comment.too_long'), t('browser.comment.invalid')),
        [comment, config, t]
    )
}

interface CommentFieldProps {
    value: string
    onChange: (value: string) => void
    /** Repository settings the comment is validated against; absent while they are still loading. */
    config?: RepositoryConfig | undefined
    testId: string
    labelWidth?: number | undefined
}

/**
 * The commit comment of an action that writes to the repository, suggested by the repository and validated
 * against its pattern. Every dialog that commits shows the same field.
 */
export const CommentField = ({ value, onChange, config, testId, labelWidth }: CommentFieldProps) => {
    const { t } = useTranslation('repository')
    const error = useCommentError(value, config)

    return (
        <FieldRow alignTop label={t('browser.comment.label')} {...labelWidth === undefined ? {} : { labelWidth }}>
            <Input.TextArea
                autoSize={{ maxRows: 6, minRows: 2 }}
                data-testid={testId}
                onChange={event => onChange(event.target.value)}
                status={error ? 'error' : ''}
                value={value}
            />
            <FieldError message={error} testId={`${testId}-error`} />
        </FieldRow>
    )
}
