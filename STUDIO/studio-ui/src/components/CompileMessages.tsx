import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Tooltip, Typography } from 'antd'
import { EditOutlined, FileTextOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { useVirtualizer } from '@tanstack/react-virtual'
import type { ProjectStatusDetailedMessage } from '../services/projectStatus'
import { basename } from '../containers/projects/projectPaths'
import { compileColorsOf } from '../containers/projects/projectsTheme'
import { useLaidOut } from '../hooks/useLaidOut'
import { ScreenMemory, useRemembered } from '../hooks/useRemembered'
import { useScrollMargin } from '../hooks/useScrollMargin'

/** How tall a message is taken to be before it is drawn and measured: one line of text. */
const MESSAGE_ESTIMATE = 30
/** How many messages past the edges of the view are drawn, so a short scroll shows no blank. */
const MESSAGES_AHEAD = 10
/** How much of a long message is shown before the reader asks for the rest. */
const PREVIEW_CHARS = 260
const PREVIEW_LINES = 4
/** How much of a stack trace is shown before that: enough to see where it was raised, and no more. */
const TRACE_CHARS = 1600
const TRACE_LINES = 16

/**
 * The look of the compilation messages, which any list of problems shares so they read the same wherever they are
 * shown.
 */
export const useCompileMessageStyles = createStyles(({ css, token }) => ({
    list: css`
        list-style: none;
        margin: ${token.marginSM}px 0 0;
        padding: 0;
        display: flex;
        flex-direction: column;
        gap: ${token.marginXS}px;
    `,
    /** The list of the messages drawn as they scroll into sight: it takes the room of all of them. */
    scrolledList: css`
        position: relative;
        list-style: none;
        margin: ${token.marginSM}px 0 0;
        padding: 0;
    `,
    /** A message of the list, with the room between it and the next one. */
    entry: css`
        padding-bottom: ${token.marginXS}px;
    `,
    /** A message drawn where its place in the list puts it. */
    scrolledEntry: css`
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
    `,
    /** One message, marked by the colour stripe of its severity — the way the legacy editor listed them. */
    message: css`
        padding: ${token.paddingXXS}px ${token.paddingXS}px;
        border-left: 3px solid transparent;
        white-space: pre-wrap;
        word-break: break-word;
        color: ${token.colorTextSecondary};
        font-size: 13px;
    `,
    error: css`
        border-left-color: ${compileColorsOf(token).errors};
    `,
    warning: css`
        border-left-color: ${compileColorsOf(token).warnings};
    `,
    info: css`
        border-left-color: ${compileColorsOf(token).infos};
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
        color: ${compileColorsOf(token).errors};
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
        margin-top: ${token.marginXXS}px;
        font-family: ${token.fontFamilyCode};
        font-size: 12px;
        white-space: pre-wrap;
        overflow-wrap: anywhere;
        color: ${token.colorTextSecondary};
    `,
}))

/** The styles of the messages, read once by the list and handed to each part it draws many times. */
type MessageStyles = ReturnType<typeof useCompileMessageStyles>['styles']

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
    styles,
    chars = PREVIEW_CHARS,
    lines = PREVIEW_LINES,
    rememberAs,
}: {
    value: string
    styles: MessageStyles
    chars?: number
    lines?: number
    /** The key the list remembers the text opened in full by, while it is out of sight. */
    rememberAs?: string
}) => {
    const { t } = useTranslation('repository')
    const [expanded, setExpanded] = useRemembered(rememberAs, () => false)
    const shown = useMemo(() => shorten(value, chars, lines), [value, chars, lines])
    const long = shown.long
    const text = expanded || !long ? value : `${shown.text}...`

    // Another text in place of this one, such as a stack trace read again, starts shortened
    const shownValue = useRef(value)
    useEffect(() => {
        if (shownValue.current !== value) {
            shownValue.current = value
            setExpanded(false)
        }
    }, [value, setExpanded])

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
 * The text a message was raised about, with the piece it is about marked the way the old editor marked it.
 *
 * <p>The text is on screen already: the message names only where in it the piece begins and ends.
 */
export const MessageCode = ({
    text,
    start,
    end,
    testId,
    styles,
}: {
    text: string
    start: number
    end: number
    testId: string
    styles: MessageStyles
}) => {
    return (
        <span className={styles.code} data-testid={testId}>
            {text.slice(0, start)}
            <span className={styles.marked} data-testid={`${testId}-marked`}>
                {text.slice(start, end)}
            </span>
            {text.slice(end)}
        </span>
    )
}

/**
 * Where in the localization files a message is, as the reader names it: the key and the file, or the file alone for
 * a message about the whole file. The text of the message does not repeat it.
 */
const localizationPlaceOf = (message: ProjectStatusDetailedMessage): string | null => {
    const where = message.location
    if (where?.type !== 'localization') {
        return null
    }
    const path = where.path ?? ''
    const file = path.slice(path.lastIndexOf('/') + 1)
    return where.key === undefined ? file : `${where.key} · ${file}`
}

/** A file a message points to, and the line of it. */
interface MessageFile {
    path: string
    line: number | undefined
}

/**
 * The file a message about a Markdown file that breaks the format points to, and the line of it; null for any other
 * message. A localization file is named in the message itself.
 */
const fileOf = (message: ProjectStatusDetailedMessage): MessageFile | null => {
    const where = message.location
    if (where === undefined || where.type === 'localization' || where.path === undefined) {
        return null
    }
    return { path: where.path, line: where.line }
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
    rememberAs,
    testId,
    styles,
}: {
    load: () => Promise<string>
    /** The key the list remembers the trace by, opened and read, while it is out of sight. */
    rememberAs: string
    testId: string
    styles: MessageStyles
}) => {
    const { t } = useTranslation('repository')
    const [open, setOpen] = useRemembered(rememberAs, () => false)
    // The read of the trace, on its way or done, kept by the list: a message drawn again as the reader scrolls back to
    // it follows the read it started, which may still be on its way.
    const [read, setRead] = useRemembered<Promise<string> | null>(`${rememberAs} read`, () => null)
    // What the read answered: the trace, or why it could not be read. A read that failed once — a connection dropped,
    // a request refused — is worth retrying, so opening the trace again asks the server again; a trace read is not.
    const [answer, setAnswer] = useState<{ of: Promise<string>, trace: string | null, failure: string | null }>()
    const answered = answer?.of === read ? answer : undefined
    const loading = read !== null && answered === undefined

    useEffect(() => {
        if (read === null) {
            return undefined
        }
        let current = true
        read
            .then(trace => {
                if (current) {
                    setAnswer({ of: read, trace, failure: null })
                }
            })
            .catch(() => {
                if (current) {
                    setAnswer({ of: read, trace: null, failure: t('browser.compile.stacktrace_failed') })
                }
            })
        return () => {
            current = false
        }
    }, [read, t])

    const toggle = () => {
        if (open) {
            setOpen(false)
            return
        }
        setOpen(true)
        if (read === null || answered?.failure != null) {
            setRead(load())
        }
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
                    <MessageText
                        chars={TRACE_CHARS}
                        lines={TRACE_LINES}
                        rememberAs={`${rememberAs} text`}
                        styles={styles}
                        value={answered?.trace ?? answered?.failure ?? ''}
                    />
                </div>
            )}
        </div>
    )
}

/** The colour stripe of each severity a message carries. */
const STRIPES = { ERROR: 'error', WARN: 'warning', INFO: 'info' } as const

interface CompileMessagesProps {
    /** The messages, in the order they are listed: the errors first, the information messages last. */
    messages: ProjectStatusDetailedMessage[]
    /**
     * The element the list scrolls in, null while it is not drawn yet. Only the messages in sight of it are drawn.
     */
    scroller: HTMLElement | null
    /** Prefix of the test id each message row carries, so a screen can find its own. */
    testIdPrefix?: string
    /**
     * What opening a message does — usually showing the table it was raised against.
     *
     * <p>Every message already says where it came from, so opening one costs no request of its own: a project
     * raising a thousand of them still asks the server nothing.
     */
    onOpen?: ((message: ProjectStatusDetailedMessage) => void) | undefined
    /** Which messages can be opened at all; the rest are read where they are. */
    canOpen?: ((message: ProjectStatusDetailedMessage) => boolean) | undefined
    /**
     * Opens the cell a message was raised against, for the reader to correct it.
     *
     * <p>Offered beside a message that names one, the way the old editor put a pencil there. Absent on a
     * screen that does no editing, and on a message that names no cell there is nothing to open.
     */
    onEditCell?: ((cell: string) => void) | undefined
    /**
     * Opens the file a message points to on the line it is about.
     *
     * <p>Offered beside a message about a Markdown file that breaks the format, in place of the cell: only the text of
     * the file tells what is wrong, which the table drawn from it does not show. Absent on a screen that opens no file.
     */
    onOpenFile?: ((path: string, line: number | undefined) => void) | undefined
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
 * The compilation messages, as the legacy editor listed them: each marked by the colour stripe of its severity.
 *
 * Both the project's problems panel and the problems of a single table are drawn through this, so a message
 * reads the same wherever it is shown.
 *
 * A project can raise thousands of messages, so only the messages in sight of the element the list scrolls in, and a
 * few around them, are drawn; each keeps the room it takes, measured as it is drawn. A message the reader opened in
 * full, and a stack trace read, stay so while the message is out of sight. Where the element has no layout to
 * measure, every message is drawn.
 */
export const CompileMessages = ({
    messages,
    scroller,
    testIdPrefix = 'compile-message',
    ...actions
}: CompileMessagesProps) => {
    const { styles, cx } = useCompileMessageStyles()
    const list = useRef<HTMLUListElement>(null)
    const laidOut = useLaidOut(scroller, 'height')
    // Where the list begins in what scrolls it, under the notice and the messages listed above it.
    const margin = useScrollMargin(list, scroller, laidOut)
    // What the reader opened of the messages scrolled out of sight, for as long as the list is on screen
    const [memory] = useState(() => new Map<string, unknown>())
    const getItemKey = useCallback((index: number) => messages[index]?.id ?? index, [messages])
    const virtualizer = useVirtualizer<HTMLElement, HTMLLIElement>({
        count: messages.length,
        enabled: laidOut,
        estimateSize: () => MESSAGE_ESTIMATE,
        getItemKey,
        getScrollElement: () => scroller,
        overscan: MESSAGES_AHEAD,
        scrollMargin: margin,
        // The messages come into sight with the next frame rather than inside the scroll itself.
        useFlushSync: false,
    })

    if (messages.length === 0) {
        return null
    }
    const row = (message: ProjectStatusDetailedMessage) => (
        <MessageRow cx={cx} message={message} styles={styles} testIdPrefix={testIdPrefix} {...actions} />
    )
    return (
        <ScreenMemory.Provider value={memory}>
            <ul
                ref={list}
                className={styles.scrolledList}
                style={laidOut ? { height: virtualizer.getTotalSize() } : undefined}
            >
                {laidOut
                    ? virtualizer.getVirtualItems().map(item => {
                        const message = messages[item.index]
                        return message && (
                            <li
                                key={item.key}
                                ref={virtualizer.measureElement}
                                className={cx(styles.entry, styles.scrolledEntry)}
                                data-index={item.index}
                                style={{ transform: `translateY(${item.start - margin}px)` }}
                            >
                                {row(message)}
                            </li>
                        )
                    })
                    : messages.map(message => (
                        <li key={message.id} className={styles.entry}>
                            {row(message)}
                        </li>
                    ))}
            </ul>
        </ScreenMemory.Provider>
    )
}

type MessageRowProps = Omit<CompileMessagesProps, 'messages' | 'scroller' | 'testIdPrefix'> & {
    message: ProjectStatusDetailedMessage
    testIdPrefix: string
    /** The styles of the list, read once for all of its messages. */
    styles: MessageStyles
    cx: ReturnType<typeof useCompileMessageStyles>['cx']
}

/** One message, marked by the colour stripe of its severity, with the ways it offers into what it is about. */
const MessageRow = ({
    message,
    testIdPrefix,
    styles,
    cx,
    onOpen,
    canOpen,
    onEditCell,
    onOpenFile,
    onStacktrace,
    cellText,
}: MessageRowProps) => {
    const { t } = useTranslation('repository')
    const openable = onOpen !== undefined && (canOpen === undefined || canOpen(message))
    const place = localizationPlaceOf(message)
    const text = (
        <>
            {place !== null && (
                <Typography.Text data-testid={`${testIdPrefix}-${message.id}-place`} type="secondary">
                    {`${place}: `}
                </Typography.Text>
            )}
            <MessageText rememberAs={`${message.id} text`} styles={styles} value={message.summary} />
        </>
    )
    const file = fileOf(message)
    // A message about the text of a file is read there: the cell shows nothing of it.
    const cell = message.location?.type === 'table' && file === null ? message.location.cell : undefined
    const rule = file === null ? ruleOf(message, cellText) : null
    return (
        <div className={cx(styles.message, styles[STRIPES[message.severity]], openable && styles.openable)}>
            {openable ? (
                // A message that leads to its table is a button, so the keyboard reaches it the way the pointer does.
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
                    styles={styles}
                    testId={`${testIdPrefix}-${message.id}-code`}
                    text={rule.text}
                />
            )}
            {message.stacktrace && onStacktrace !== undefined && (
                <MessageStacktrace
                    load={() => onStacktrace(message)}
                    rememberAs={`${message.id} trace`}
                    styles={styles}
                    testId={`${testIdPrefix}-${message.id}-stacktrace`}
                />
            )}
            {onOpenFile !== undefined && file !== null && (
                <Tooltip
                    title={t('browser.module.open_file_line', {
                        file: basename(file.path),
                        line: file.line,
                    })}
                >
                    <Button
                        className={styles.editCell}
                        data-testid={`${testIdPrefix}-${message.id}-file`}
                        icon={<FileTextOutlined />}
                        onClick={() => onOpenFile(file.path, file.line)}
                        size="small"
                        type="text"
                    />
                </Tooltip>
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
        </div>
    )
}

export default CompileMessages
