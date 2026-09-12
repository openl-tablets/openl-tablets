import { useCallback, useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from 'antd'
import { CloseCircleFilled, DownOutlined, UpOutlined, WarningFilled } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import type { ProjectStatusDetailedMessage } from '../../services/projectStatus'
import { readJson, readStored, writeJson, writeStored } from '../../utils/localStore'
import { CompileMessages } from '../../components/CompileMessages'
import { COMPILE_COLORS } from '../projects/projectsTheme'

/** Whether the section stands open, kept so a reader who folds it away keeps it folded. */
const STORAGE_KEY = 'openl.module.tableProblems'
/** The height it was last dragged to, kept for the next table the reader opens. */
const HEIGHT_STORAGE_KEY = 'openl.module.tableProblems.height'

const MIN_HEIGHT = 80
const MAX_HEIGHT = 600
const DEFAULT_HEIGHT = 220

const loadHeight = (): number => {
    const stored = Number(readStored(HEIGHT_STORAGE_KEY))
    return Number.isFinite(stored) && stored >= MIN_HEIGHT && stored <= MAX_HEIGHT ? stored : DEFAULT_HEIGHT
}

const useStyles = createStyles(({ css, token }) => ({
    section: css`
        position: relative;
        flex: none;
        border-bottom: 1px solid ${token.colorBorderSecondary};
        background: ${token.colorBgContainer};
    `,
    /** The bottom edge the section is dragged by; it widens on hover so it can be grabbed without aiming. */
    resizer: css`
        position: absolute;
        bottom: -3px;
        left: 0;
        right: 0;
        height: 6px;
        margin: 0;
        border: none;
        background: transparent;
        cursor: row-resize;
        touch-action: none;
        z-index: 2;

        &:hover,
        &:active {
            background: ${token.colorPrimaryBorder};
        }
    `,
    header: css`
        display: flex;
        align-items: center;
        gap: ${token.marginXS}px;
        padding: ${token.paddingXXS}px ${token.padding}px;
        cursor: pointer;
        user-select: none;
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
    /** The messages scroll inside the section, so a table with many of them keeps the table in view. */
    body: css`
        overflow: auto;
        padding: 0 ${token.padding}px ${token.paddingSM}px;
    `,
}))

interface TableProblemsProps {
    /** What the read said about this table; nothing is drawn when it said nothing. */
    messages: ProjectStatusDetailedMessage[]
}

/**
 * What the compiler said about the table on screen, in a section of its own above it — where the old Editor
 * kept its Problems block, and folded away the same way.
 *
 * Only the messages the read returned for this table are shown; everything the project raised elsewhere stays
 * in the panel at the foot of the screen.
 */
export const TableProblems = ({ messages }: TableProblemsProps) => {
    const { t } = useTranslation('repository')
    const { styles, cx } = useStyles()
    const [open, setOpen] = useState(() => readJson(STORAGE_KEY, true, (value): value is boolean =>
        typeof value === 'boolean'))
    const [height, setHeight] = useState(loadHeight)

    // Filtered once per read: a fresh array on every render would send the "show more" of the list below
    // back to its first page every time the screen redraws — which a compilation makes it do often.
    const errors = useMemo(() => messages.filter(message => message.severity === 'ERROR'), [messages])
    const warnings = useMemo(() => messages.filter(message => message.severity === 'WARN'), [messages])

    useEffect(() => {
        writeJson(STORAGE_KEY, open)
    }, [open])

    // Dragging the bottom edge sizes the section; the height it is left at is where it opens next time.
    const startResize = useCallback((event: React.PointerEvent<HTMLHRElement>) => {
        event.preventDefault()
        const top = (event.currentTarget.parentElement ?? event.currentTarget).getBoundingClientRect().top
        const heightAt = (moved: PointerEvent) =>
            Math.min(MAX_HEIGHT, Math.max(MIN_HEIGHT, Math.round(moved.clientY - top)))
        const resize = (moved: PointerEvent) => setHeight(heightAt(moved))
        const stop = (moved: PointerEvent) => {
            resize(moved)
            window.removeEventListener('pointermove', resize)
            window.removeEventListener('pointerup', stop)
            writeStored(HEIGHT_STORAGE_KEY, String(heightAt(moved)))
        }
        window.addEventListener('pointermove', resize)
        window.addEventListener('pointerup', stop)
    }, [])

    if (messages.length === 0) {
        return null
    }

    return (
        <section className={styles.section} data-testid="table-problems">
            <div
                className={styles.header}
                data-testid="table-problems-toggle"
                onClick={() => setOpen(current => !current)}
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
                <Button
                    aria-label={t(open ? 'browser.compile.problems_collapse' : 'browser.compile.problems_expand')}
                    icon={open ? <UpOutlined /> : <DownOutlined />}
                    size="small"
                    type="text"
                />
            </div>
            {open && (
                <>
                    <div className={styles.body} data-testid="table-problems-body" style={{ height }}>
                        <CompileMessages messages={errors} severity="error" testIdPrefix="table-message" />
                        <CompileMessages messages={warnings} severity="warning" testIdPrefix="table-message" />
                    </div>
                    <hr
                        aria-label={t('browser.compile.resize')}
                        className={styles.resizer}
                        data-testid="table-problems-resizer"
                        onPointerDown={startResize}
                    />
                </>
            )}
        </section>
    )
}
