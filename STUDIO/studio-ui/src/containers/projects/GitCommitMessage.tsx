import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from 'antd'
import { createStyles } from 'antd-style'

const DEFAULT_MAX_CHARS = 220
const DEFAULT_MAX_LINES = 3

const useStyles = createStyles(({ css, token }) => ({
    root: css`
        min-width: 0;
    `,
    text: css`
        white-space: pre-wrap;
        overflow-wrap: anywhere;
        line-height: 1.45;
    `,
    strong: css`
        font-weight: 500;
    `,
    toggle: css`
        display: inline-flex;
        height: auto;
        margin-top: 2px;
        padding: 0;
        font-size: 12px;
        line-height: 1.4;
    `,
}))

interface GitCommitMessageProps {
    className?: string
    message?: string | null
    maxChars?: number
    maxLines?: number
    strong?: boolean
    testId?: string
}

const trimByLines = (message: string, maxLines: number): string => message.split(/\r?\n/).slice(0, maxLines).join('\n')

const excerptOf = (message: string, maxChars: number, maxLines: number): string => {
    const byLines = trimByLines(message, maxLines)
    if (byLines.length <= maxChars) {
        return byLines.trimEnd()
    }
    return byLines.slice(0, maxChars).trimEnd()
}

/**
 * Shows a git commit message with long multi-line bodies collapsed by default.
 */
export const GitCommitMessage = ({
    className,
    message,
    maxChars = DEFAULT_MAX_CHARS,
    maxLines = DEFAULT_MAX_LINES,
    strong,
    testId,
}: GitCommitMessageProps) => {
    const { t } = useTranslation('repository')
    const { styles, cx } = useStyles()
    const [expanded, setExpanded] = useState(false)
    const text = message?.trim() || '—'
    const lineCount = useMemo(() => text.split(/\r?\n/).length, [text])
    const collapsible = text.length > maxChars || lineCount > maxLines
    const visibleText = expanded || !collapsible ? text : `${excerptOf(text, maxChars, maxLines)}...`

    return (
        <span className={cx(styles.root, className)} data-testid={testId}>
            <span className={cx(styles.text, strong && styles.strong)}>{visibleText}</span>
            {collapsible && (
                <>
                    {' '}
                    <Button
                        className={styles.toggle}
                        data-testid={testId ? `${testId}-toggle` : undefined}
                        onClick={() => setExpanded(value => !value)}
                        size="small"
                        type="link"
                    >
                        {expanded ? t('browser.commit.show_less') : t('browser.commit.show_more')}
                    </Button>
                </>
            )}
        </span>
    )
}
