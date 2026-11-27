import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { CloseCircleFilled, DownOutlined, InfoCircleFilled, UpOutlined, WarningFilled } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import type { ProjectStatusDetailedMessage } from '../../services/projectStatus'
import { booleanText, storedSetting } from '../../utils/localStore'
import { CompileMessages } from '../../components/CompileMessages'
import { ResizeHandle, useDragSize } from '../../components/ResizeHandle'
import { compileColorsOf } from '../projects/projectsTheme'
import { useStoredSetting } from '../../hooks/useStoredSetting'

/** Whether the section stands open, kept so a reader who folds it away keeps it folded. */
const STORAGE_KEY = 'openl.module.tableProblems'

/** Whether the panel is open; open, the default, stores nothing. */
const openSetting = storedSetting(STORAGE_KEY, true, booleanText)
/** The height the messages may take at most, as last dragged to, kept for the next table the reader opens. */
const HEIGHT_STORAGE_KEY = 'openl.module.tableProblems.height'

const HEIGHT = { min: 80, max: 600, fallback: 220 }

const useStyles = createStyles(({ css, token }) => ({
    section: css`
        position: relative;
        flex: none;
        border-bottom: 1px solid ${token.colorBorderSecondary};
        background: ${token.colorBgContainer};
    `,
    /** The heading folds the panel, so it is a button — the frame of one is not what a heading reads as. */
    header: css`
        display: flex;
        align-items: center;
        gap: ${token.marginXS}px;
        width: 100%;
        padding: ${token.paddingXXS}px ${token.padding}px;
        border: none;
        background: none;
        cursor: pointer;
        user-select: none;
    `,
    /** The mark that says which way the heading folds, sitting where the button used to. */
    fold: css`
        margin-left: auto;
        color: ${token.colorTextTertiary};
    `,
    title: css`
        font-size: ${token.fontSizeSM}px;
        font-weight: 600;
        color: ${token.colorTextSecondary};
    `,
    count: css`
        display: inline-flex;
        align-items: center;
        gap: ${token.marginXXS}px;
        font-size: ${token.fontSizeSM}px;
        color: ${token.colorTextSecondary};
    `,
    errorMark: css`
        color: ${compileColorsOf(token).errors};
    `,
    warningMark: css`
        color: ${compileColorsOf(token).warnings};
    `,
    infoMark: css`
        color: ${compileColorsOf(token).infos};
    `,
    /** The notice on a table nothing here can write — the one the Editor put at the top of its Problems. */
    partial: css`
        display: flex;
        align-items: center;
        gap: ${token.marginXS}px;
        padding: ${token.paddingXXS}px ${token.padding}px;
        font-size: ${token.fontSizeSM}px;
        color: ${token.colorTextSecondary};
    `,
    /** Holds the messages and the grip that sizes them, so the grip is measured against what it sizes. */
    resizable: css`
        position: relative;
    `,
    /**
     * The messages take the room they need, up to the height the section was dragged to, and scroll inside it, so a
     * table with few of them is not pushed down under the problems panel at the foot of the screen (EPBDS-17076).
     */
    body: css`
        overflow: auto;
        padding: 0 ${token.padding}px ${token.paddingSM}px;
    `,
}))

interface TableProblemsProps {
    /** What the read said about this table; nothing is drawn when it said nothing. */
    messages: ProjectStatusDetailedMessage[]
    /** Opens the cell a message was raised against; absent for a reader who may not write the table. */
    onEditCell?: ((cell: string) => void) | undefined
    /** Opens the file a message about a Markdown file that breaks the format points to, on its line. */
    onOpenFile?: ((path: string, line: number | undefined) => void) | undefined
    /** Reads the stack trace behind a message, when the reader opens it. */
    onStacktrace?: ((message: ProjectStatusDetailedMessage) => Promise<string>) | undefined
    /** The text a cell of this table holds, so a message can show the rule it was raised about. */
    cellText?: ((cell: string) => string | undefined) | undefined
    /**
     * Set on a table written as several partial tables. It is read here but not written, and saying so is the
     * whole point of the notice — the reader is otherwise left wondering why nothing can be edited.
     */
    partial?: boolean
    /** Set on a table of a Markdown file, whose parts are files changed as text rather than in Excel. */
    markdown?: boolean
}

/**
 * What the compiler said about the table on screen, in a section of its own above it — where the old Editor
 * kept its Problems block, and folded away the same way: the errors, the warnings, then the information messages.
 *
 * Only the messages the read returned for this table are shown; everything the project raised elsewhere stays
 * in the panel at the foot of the screen.
 */
export const TableProblems = ({
    messages,
    onEditCell,
    onOpenFile,
    onStacktrace,
    cellText,
    partial = false,
    markdown = false,
}: TableProblemsProps) => {
    const { t } = useTranslation('repository')
    const { styles, cx } = useStyles()
    const [open, setOpen] = useStoredSetting(openSetting)
    const { size: height, startResize } = useDragSize(HEIGHT_STORAGE_KEY, 'bottom', HEIGHT)

    // The element the messages scroll in, which the list draws the messages in sight of.
    const [body, setBody] = useState<HTMLDivElement | null>(null)
    // Counted once per read, as the screen redraws often while the project compiles. The server lists the messages
    // the errors first and the information messages last.
    const counts = useMemo(() => ({
        errors: messages.filter(message => message.severity === 'ERROR').length,
        warnings: messages.filter(message => message.severity === 'WARN').length,
        infos: messages.filter(message => message.severity === 'INFO').length,
    }), [messages])

    if (messages.length === 0 && !partial) {
        return null
    }

    return (
        <section className={styles.section} data-testid="table-problems">
            <button
                aria-expanded={open}
                className={styles.header}
                data-testid="table-problems-toggle"
                onClick={() => setOpen(!open)}
                type="button"
            >
                <span className={styles.title}>{t('browser.module.problems')}</span>
                {counts.errors > 0 && (
                    <span className={cx(styles.count, styles.errorMark)} data-testid="table-problems-errors">
                        <CloseCircleFilled />
                        {counts.errors}
                    </span>
                )}
                {counts.warnings > 0 && (
                    <span className={cx(styles.count, styles.warningMark)} data-testid="table-problems-warnings">
                        <WarningFilled />
                        {counts.warnings}
                    </span>
                )}
                {counts.infos > 0 && (
                    <span className={cx(styles.count, styles.infoMark)} data-testid="table-problems-infos">
                        <InfoCircleFilled />
                        {counts.infos}
                    </span>
                )}
                {/* The whole heading folds the panel, so the mark only says which way it goes. */}
                <span className={styles.fold}>{open ? <UpOutlined /> : <DownOutlined />}</span>
            </button>
            {open && (
                // The grip is dragged against the box it sizes: measured against the whole section, every drag
                // would size the messages to the pointer plus the header above them.
                <div className={styles.resizable}>
                    <div
                        ref={setBody}
                        className={styles.body}
                        data-testid="table-problems-body"
                        style={{ maxHeight: height }}
                    >
                        {partial && (
                            <div className={styles.partial} data-testid="table-problems-partial">
                                <WarningFilled className={styles.warningMark} />
                                {t(markdown ? 'browser.module.partial_markdown_table' : 'browser.module.partial_table')}
                            </div>
                        )}
                        <CompileMessages
                            cellText={cellText}
                            messages={messages}
                            onEditCell={onEditCell}
                            onOpenFile={onOpenFile}
                            onStacktrace={onStacktrace}
                            scroller={body}
                            testIdPrefix="table-message"
                        />
                    </div>
                    <ResizeHandle edge="bottom" onPointerDown={startResize} testId="table-problems-resizer" />
                </div>
            )}
        </section>
    )
}
