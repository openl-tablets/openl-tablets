import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { CloseCircleFilled, DownOutlined, UpOutlined, WarningFilled } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import type { ProjectStatusDetailedMessage } from '../../services/projectStatus'
import { readJson, writeJson } from '../../utils/localStore'
import { CompileMessages } from '../../components/CompileMessages'
import { ResizeHandle, useDragSize } from '../../components/ResizeHandle'
import { COMPILE_COLORS } from '../projects/projectsTheme'

/** Whether the section stands open, kept so a reader who folds it away keeps it folded. */
const STORAGE_KEY = 'openl.module.tableProblems'
/** The height it was last dragged to, kept for the next table the reader opens. */
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
        text-align: left;
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
        gap: 4px;
        font-size: ${token.fontSizeSM}px;
        color: ${token.colorTextSecondary};
    `,
    errorMark: css`
        color: ${COMPILE_COLORS.errors};
    `,
    warningMark: css`
        color: ${COMPILE_COLORS.warnings};
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
    /** The messages scroll inside the section, so a table with many of them keeps the table in view. */
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
    /** Reads the stack trace behind a message, when the reader opens it. */
    onStacktrace?: ((message: ProjectStatusDetailedMessage) => Promise<string>) | undefined
    /** The text a cell of this table holds, so a message can show the rule it was raised about. */
    cellText?: ((cell: string) => string | undefined) | undefined
    /**
     * Set on a table written as several partial tables. It is read here but not written, and saying so is the
     * whole point of the notice — the reader is otherwise left wondering why nothing can be edited.
     */
    partial?: boolean
}

/**
 * What the compiler said about the table on screen, in a section of its own above it — where the old Editor
 * kept its Problems block, and folded away the same way.
 *
 * Only the messages the read returned for this table are shown; everything the project raised elsewhere stays
 * in the panel at the foot of the screen.
 */
export const TableProblems = ({ messages, onEditCell, onStacktrace, cellText, partial = false }: TableProblemsProps) => {
    const { t } = useTranslation('repository')
    const { styles, cx } = useStyles()
    const [open, setOpen] = useState(() => readJson(STORAGE_KEY, true, (value): value is boolean =>
        typeof value === 'boolean'))
    const { size: height, startResize } = useDragSize(HEIGHT_STORAGE_KEY, 'bottom', HEIGHT)

    // Filtered once per read: a fresh array on every render would send the "show more" of the list below
    // back to its first page every time the screen redraws — which a compilation makes it do often.
    const errors = useMemo(() => messages.filter(message => message.severity === 'ERROR'), [messages])
    const warnings = useMemo(() => messages.filter(message => message.severity === 'WARN'), [messages])

    useEffect(() => {
        writeJson(STORAGE_KEY, open)
    }, [open])

    if (messages.length === 0 && !partial) {
        return null
    }

    return (
        <section className={styles.section} data-testid="table-problems">
            <button
                aria-expanded={open}
                className={styles.header}
                data-testid="table-problems-toggle"
                onClick={() => setOpen(current => !current)}
                type="button"
            >
                <span className={styles.title}>{t('browser.module.problems')}</span>
                {errors.length > 0 && (
                    <span className={cx(styles.count, styles.errorMark)} data-testid="table-problems-errors">
                        <CloseCircleFilled />
                        {errors.length}
                    </span>
                )}
                {warnings.length > 0 && (
                    <span className={cx(styles.count, styles.warningMark)} data-testid="table-problems-warnings">
                        <WarningFilled />
                        {warnings.length}
                    </span>
                )}
                {/* The whole heading folds the panel, so the mark only says which way it goes. */}
                <span className={styles.fold}>{open ? <UpOutlined /> : <DownOutlined />}</span>
            </button>
            {open && (
                // The grip is dragged against the box it sizes: measured against the whole section, every drag
                // would size the messages to the pointer plus the header above them.
                <div className={styles.resizable}>
                    <div className={styles.body} data-testid="table-problems-body" style={{ height }}>
                        {partial && (
                            <div className={styles.partial} data-testid="table-problems-partial">
                                <WarningFilled className={styles.warningMark} />
                                {t('browser.module.partial_table')}
                            </div>
                        )}
                        <CompileMessages
                            cellText={cellText}
                            messages={errors}
                            onEditCell={onEditCell}
                            onStacktrace={onStacktrace}
                            severity="error"
                            testIdPrefix="table-message"
                        />
                        <CompileMessages
                            cellText={cellText}
                            messages={warnings}
                            onEditCell={onEditCell}
                            onStacktrace={onStacktrace}
                            severity="warning"
                            testIdPrefix="table-message"
                        />
                    </div>
                    <ResizeHandle edge="bottom" onPointerDown={startResize} testId="table-problems-resizer" />
                </div>
            )}
        </section>
    )
}
