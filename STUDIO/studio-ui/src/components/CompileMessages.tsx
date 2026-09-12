import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from 'antd'
import { createStyles } from 'antd-style'
import type { ProjectStatusDetailedMessage } from '../services/projectStatus'
import { COMPILE_COLORS } from '../containers/projects/projectsTheme'

/** How many messages of one severity are listed before the reader asks for more. */
const PAGE_SIZE = 10
/** How much of a long message is shown before the reader asks for the rest. */
const PREVIEW_CHARS = 260
const PREVIEW_LINES = 4

const useStyles = createStyles(({ css, token }) => ({
    list: css`
        list-style: none;
        margin: 12px 0 0;
        padding: 0;
        display: flex;
        flex-direction: column;
        gap: 6px;
    `,
    /** One message, marked by the colour stripe of its severity — the way the legacy editor listed them. */
    message: css`
        padding: 4px 8px;
        border-left: 3px solid transparent;
        white-space: pre-wrap;
        word-break: break-word;
        color: ${token.colorTextSecondary};
        font-size: 13px;
    `,
    error: css`
        border-left-color: ${COMPILE_COLORS.errors};
    `,
    warning: css`
        border-left-color: ${COMPILE_COLORS.warnings};
    `,
    /** A message that leads somewhere reads as something to press. */
    openable: css`
        cursor: pointer;

        &:hover,
        &:focus-visible {
            background: ${token.controlItemBgHover};
        }
    `,
    action: css`
        margin-top: 2px;
        padding: 0;
        height: auto;
        font-size: 12px;
    `,
    pager: css`
        display: flex;
        gap: 8px;
        margin-top: 8px;
    `,
}))

const truncate = (value: string): string => {
    const lines = value.split(/\r?\n/)
    const byLines = lines.length > PREVIEW_LINES ? lines.slice(0, PREVIEW_LINES).join('\n') : value
    return byLines.length > PREVIEW_CHARS ? byLines.slice(0, PREVIEW_CHARS).trimEnd() : byLines
}

const isLong = (value: string): boolean =>
    value.length > PREVIEW_CHARS || value.split(/\r?\n/).length > PREVIEW_LINES

/** One message, shown in full only when it is short or the reader asked for the rest of it. */
const MessageText = ({ value }: { value: string }) => {
    const { styles } = useStyles()
    const { t } = useTranslation('repository')
    const [expanded, setExpanded] = useState(false)
    const long = isLong(value)
    const text = !long || expanded ? value : `${truncate(value)}...`

    useEffect(() => {
        setExpanded(false)
    }, [value])

    return (
        <>
            {text}
            {long && (
                <div>
                    <Button
                        className={styles.action}
                        size="small"
                        type="link"
                        onClick={event => {
                            event.stopPropagation()
                            setExpanded(current => !current)
                        }}
                    >
                        {expanded ? t('browser.compile.show_less') : t('browser.compile.show_more_text')}
                    </Button>
                </div>
            )}
        </>
    )
}

interface CompileMessagesProps {
    messages: ProjectStatusDetailedMessage[]
    /** The severity these messages carry, which decides the colour of the stripe beside them. */
    severity: 'error' | 'warning'
    /** Prefix of the test id each message row carries, so a screen can find its own. */
    testIdPrefix?: string
    /**
     * What opening a message does — usually showing the table it was raised against.
     *
     * <p>Every message already says where it came from, so opening one costs no request of its own: a project
     * raising a thousand of them still asks the server nothing.
     */
    onOpen?: (message: ProjectStatusDetailedMessage) => void
    /** Which messages can be opened at all; the rest are read where they are. */
    canOpen?: (message: ProjectStatusDetailedMessage) => boolean
}

/**
 * The compilation messages of one severity, as the legacy editor listed them: each marked by the colour stripe
 * of its severity, and paged so that a project raising hundreds of them stays responsive.
 *
 * Both the project's problems panel and the problems of a single table are drawn through this, so a message
 * reads the same wherever it is shown.
 */
export const CompileMessages = ({
    messages,
    severity,
    testIdPrefix = 'compile-message',
    onOpen,
    canOpen,
}: CompileMessagesProps) => {
    const { styles, cx } = useStyles()
    const { t } = useTranslation('repository')
    const [visibleCount, setVisibleCount] = useState(PAGE_SIZE)

    useEffect(() => {
        setVisibleCount(PAGE_SIZE)
    }, [messages])

    if (messages.length === 0) {
        return null
    }
    const visible = messages.slice(0, visibleCount)
    const remaining = messages.length - visibleCount
    return (
        <>
            <ul className={styles.list}>
                {visible.map(message => {
                    const openable = onOpen !== undefined && (canOpen === undefined || canOpen(message))
                    return (
                        <li
                            key={message.id}
                            className={cx(styles.message, styles[severity], openable && styles.openable)}
                            data-testid={`${testIdPrefix}-${message.id}`}
                            onClick={openable ? () => onOpen(message) : undefined}
                            role={openable ? 'button' : undefined}
                            tabIndex={openable ? 0 : undefined}
                            onKeyDown={openable
                                ? event => {
                                    if (event.key === 'Enter' || event.key === ' ') {
                                        event.preventDefault()
                                        onOpen(message)
                                    }
                                }
                                : undefined}
                        >
                            <MessageText value={message.summary} />
                        </li>
                    )
                })}
            </ul>
            {(remaining > 0 || visibleCount > PAGE_SIZE) && (
                <div className={styles.pager}>
                    {remaining > 0 && (
                        <Button onClick={() => setVisibleCount(count => count + PAGE_SIZE)} size="small" type="link">
                            {t('browser.compile.show_more', { count: Math.min(PAGE_SIZE, remaining) })}
                        </Button>
                    )}
                    {visibleCount > PAGE_SIZE && (
                        <Button onClick={() => setVisibleCount(PAGE_SIZE)} size="small" type="link">
                            {t('browser.compile.show_less')}
                        </Button>
                    )}
                </div>
            )}
        </>
    )
}

export default CompileMessages
