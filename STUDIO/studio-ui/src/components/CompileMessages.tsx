import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Tooltip } from 'antd'
import { EditOutlined } from '@ant-design/icons'
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
    /** The way straight to the cell a message was raised against, beside the message itself. */
    editCell: css`
        width: 22px;
        height: 22px;
        min-width: 22px;
        margin-left: ${token.marginXXS}px;
        padding: 0;
        vertical-align: middle;
        color: ${token.colorTextTertiary};
    `,
    /** A message that leads somewhere reads as something to press. */
    openable: css`
        cursor: pointer;

        &:hover,
        &:focus-within {
            background: ${token.controlItemBgHover};
        }
    `,
    /** The message fills its row and reads as the text it is, whatever frame a button would bring. */
    open: css`
        display: block;
        width: 100%;
        padding: 0;
        border: none;
        background: none;
        color: inherit;
        font: inherit;
        text-align: left;
        white-space: pre-wrap;
        word-break: break-word;
        cursor: pointer;
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
    /**
     * Opens the cell a message was raised against, for the reader to correct it.
     *
     * <p>Offered beside a message that names one, the way the old editor put a pencil there. Absent on a
     * screen that does no editing, and on a message that names no cell there is nothing to open.
     */
    onEditCell?: ((cell: string) => void) | undefined
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
    onEditCell,
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
                    const text = <MessageText value={message.summary} />
                    const cell = message.location?.type === 'table' ? message.location.cell : undefined
                    return (
                        <li
                            key={message.id}
                            className={cx(styles.message, styles[severity], openable && styles.openable)}
                        >
                            {openable ? (
                                // A message that leads to its table is a button, so the keyboard reaches
                                // it the way the pointer does.
                                <button
                                    className={styles.open}
                                    data-testid={`${testIdPrefix}-${message.id}`}
                                    onClick={() => onOpen(message)}
                                    type="button"
                                >
                                    {text}
                                </button>
                            ) : (
                                <span data-testid={`${testIdPrefix}-${message.id}`}>{text}</span>
                            )}
                            {onEditCell !== undefined && cell !== undefined && (
                                <Tooltip title={t('browser.module.edit_this_cell', { cell })}>
                                    <Button
                                        className={styles.editCell}
                                        data-testid={`${testIdPrefix}-${message.id}-edit`}
                                        icon={<EditOutlined />}
                                        onClick={() => onEditCell(cell)}
                                        size="small"
                                        type="text"
                                    />
                                </Tooltip>
                            )}
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
