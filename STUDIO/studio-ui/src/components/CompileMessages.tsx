import { useEffect, useMemo, useState } from 'react'
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
/** How much of a stack trace is shown before that: enough to see where it was raised, and no more. */
const TRACE_CHARS = 1600
const TRACE_LINES = 16

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
    /** The rule the message was raised about, set in the typewriter face the old editor showed it in. */
    code: css`
        display: block;
        margin-top: 2px;
        font-family: ${token.fontFamilyCode};
        font-size: 12px;
        white-space: pre-wrap;
        word-break: break-word;
        color: ${token.colorText};
    `,
    /** The piece of the rule the message is about, marked so the reader finds it among the rest. */
    marked: css`
        color: ${COMPILE_COLORS.errors};
        font-weight: bold;
    `,
    action: css`
        margin-top: 2px;
        padding: 0;
        height: auto;
        font-size: 12px;
    `,
    /**
     * The stack trace behind a message, set in the typewriter face.
     *
     * <p>It wraps and scrolls with the panel rather than inside a box of its own: a scroller within a scroller
     * is scrolled by the page itself, which then redraws the whole trace on every frame of a drag.
     */
    stacktrace: css`
        display: block;
        margin-top: 4px;
        font-family: ${token.fontFamilyCode};
        font-size: 12px;
        white-space: pre-wrap;
        overflow-wrap: anywhere;
        color: ${token.colorTextSecondary};
    `,
    pager: css`
        display: flex;
        gap: 8px;
        margin-top: 8px;
    `,
}))

/** What of a piece of text is shown, and whether the rest of it is being held back. */
const shorten = (value: string, chars: number, lines: number): { text: string, long: boolean } => {
    const written = value.split(/\r?\n/)
    const byLines = written.length > lines ? written.slice(0, lines).join('\n') : value
    const text = byLines.length > chars ? byLines.slice(0, chars).trimEnd() : byLines
    return { text, long: text.length < value.length }
}

/**
 * One piece of text, shown in full only when it is short or the reader asked for the rest of it.
 *
 * <p>What is not shown is not drawn either: a stack trace put on screen whole is thousands of lines the page
 * lays out and paints again on every frame of a scroll.
 */
const MessageText = ({
    value,
    chars = PREVIEW_CHARS,
    lines = PREVIEW_LINES,
}: {
    value: string
    chars?: number
    lines?: number
}) => {
    const { styles } = useStyles()
    const { t } = useTranslation('repository')
    const [expanded, setExpanded] = useState(false)
    const shown = useMemo(() => shorten(value, chars, lines), [value, chars, lines])
    const long = shown.long
    const text = expanded || !long ? value : `${shown.text}...`

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

/**
 * The rule a message was raised about, with the piece it is about marked the way the old editor marked it.
 *
 * <p>The rule is what the cell says, and the cell is on screen already: the message names only where in it the
 * piece begins and ends.
 */
const MessageCode = ({
    text,
    start,
    end,
    testId,
}: {
    text: string
    start: number
    end: number
    testId: string
}) => {
    const { styles } = useStyles()
    return (
        <span className={styles.code} data-testid={testId}>
            <span>{text.slice(0, start)}</span>
            <span className={styles.marked} data-testid={`${testId}-marked`}>
                {text.slice(start, end)}
            </span>
            <span>{text.slice(end)}</span>
        </span>
    )
}

/** The rule a message points at, when the screen holds the cell it is written in. */
const ruleOf = (
    message: ProjectStatusDetailedMessage,
    cellText: ((cell: string) => string | undefined) | undefined
): { text: string, start: number, end: number } | null => {
    const where = message.location
    if (cellText === undefined || where?.type !== 'table' || where.cell === undefined
        || where.start === undefined || where.end === undefined) {
        return null
    }
    const text = cellText(where.cell)
    return text === undefined || where.end > text.length
        ? null
        : { text, start: where.start, end: where.end }
}

/**
 * The stack trace behind a message, read when the reader opens it.
 *
 * <p>A trace runs to thousands of characters and most messages are read without one, so nothing is asked for
 * until it is opened, and what was read stays read for as long as the message is on screen.
 */
const MessageStacktrace = ({
    load,
    testId,
}: {
    load: () => Promise<string>
    testId: string
}) => {
    const { styles } = useStyles()
    const { t } = useTranslation('repository')
    const [open, setOpen] = useState(false)
    const [trace, setTrace] = useState<string | null>(null)
    // Set where the trace could not be read. Kept apart from the trace itself, so asking again asks the
    // server again: a read that failed once — a connection dropped, a request refused — is worth retrying,
    // while a trace already read is not.
    const [failure, setFailure] = useState<string | null>(null)
    const [loading, setLoading] = useState(false)

    const toggle = () => {
        if (open) {
            setOpen(false)
            return
        }
        setOpen(true)
        if (trace !== null || loading) {
            return
        }
        setLoading(true)
        setFailure(null)
        load()
            .then(setTrace)
            .catch(() => setFailure(t('browser.compile.stacktrace_failed')))
            .finally(() => setLoading(false))
    }

    return (
        <div>
            <Button
                className={styles.action}
                data-testid={`${testId}-toggle`}
                loading={loading}
                size="small"
                type="link"
                onClick={event => {
                    event.stopPropagation()
                    toggle()
                }}
            >
                {open ? t('browser.compile.hide_stacktrace') : t('browser.compile.show_stacktrace')}
            </Button>
            {open && !loading && (
                <div className={styles.stacktrace} data-testid={testId}>
                    <MessageText chars={TRACE_CHARS} lines={TRACE_LINES} value={trace ?? failure ?? ''} />
                </div>
            )}
        </div>
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
    /**
     * Reads the stack trace behind a message, when the reader opens it.
     *
     * <p>Offered only on a message that carries one, and asked for only once it is opened: a trace runs to
     * thousands of characters, and a screen listing hundreds of messages reads none of them until asked.
     */
    onStacktrace?: ((message: ProjectStatusDetailedMessage) => Promise<string>) | undefined
    /**
     * The text a cell of the table on screen holds, so a message can show the rule it was raised about.
     *
     * <p>Absent on a screen that draws no table — there the message names a cell nobody can see, and the rule
     * is not shown at all.
     */
    cellText?: ((cell: string) => string | undefined) | undefined
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
    onStacktrace,
    cellText,
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
                    const rule = ruleOf(message, cellText)
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
                            {rule !== null && (
                                <MessageCode
                                    end={rule.end}
                                    start={rule.start}
                                    testId={`${testIdPrefix}-${message.id}-code`}
                                    text={rule.text}
                                />
                            )}
                            {message.stacktrace && onStacktrace !== undefined && (
                                <MessageStacktrace
                                    load={() => onStacktrace(message)}
                                    testId={`${testIdPrefix}-${message.id}-stacktrace`}
                                />
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
