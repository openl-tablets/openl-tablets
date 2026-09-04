import { useCallback, useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from 'antd'
import { CloseCircleFilled, DownOutlined, UpOutlined, WarningFilled } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { COMPILE_RELEVANT_STATUSES } from '../../constants/projectStatusMeta'
import type { Project } from '../../types/projects'
import { useLiveProjectStatus } from '../../hooks/useLiveProjectStatus'
import {
    type ProjectCompileState,
    type ProjectStatusDetailedMessage,
    type ProjectStatusUpdate,
} from '../../services/projectStatus'
import { readStored, writeStored } from '../../utils/localStore'
import { COMPILE_COLORS, MOCKUP } from './projectsTheme'

const PAGE_SIZE = 10
const PREVIEW_CHARS = 260
const PREVIEW_LINES = 4

/** How low and how tall the panel may be dragged, and where it opens the first time. */
const MIN_HEIGHT = 120
const MAX_HEIGHT = 600
const DEFAULT_HEIGHT = 240

const HEIGHT_STORAGE_KEY = 'openl.project.problems.height'
const COLLAPSED_STORAGE_KEY = 'openl.project.problems.collapsed'

const loadHeight = (): number => {
    const stored = Number(readStored(HEIGHT_STORAGE_KEY))
    return Number.isFinite(stored) && stored >= MIN_HEIGHT && stored <= MAX_HEIGHT ? stored : DEFAULT_HEIGHT
}

const useStyles = createStyles(({ css, token }) => ({
    /** The panel docks to the bottom of the project screen, under whatever tab is open. */
    panel: css`
        position: relative;
        flex: none;
        display: flex;
        flex-direction: column;
        border-top: 1px solid ${token.colorBorderSecondary};
        background: ${MOCKUP.sidebarBg};
    `,
    /** The top edge the panel is dragged by; it widens on hover so it can be grabbed without aiming. */
    resizer: css`
        position: absolute;
        top: -3px;
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
    /** The whole header folds the panel; the counts stay in view either way. */
    header: css`
        width: 100%;
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 6px 12px;
        border: 0;
        background: transparent;
        color: ${token.colorText};
        cursor: pointer;
        font: inherit;
        text-align: left;
    `,
    count: css`
        display: inline-flex;
        align-items: center;
        gap: 6px;
        font-variant-numeric: tabular-nums;
        font-weight: 600;
    `,
    errorIcon: css`
        color: ${COMPILE_COLORS.errors};
    `,
    warningIcon: css`
        color: ${COMPILE_COLORS.warnings};
    `,
    toggle: css`
        margin-left: auto;
        flex: none;
        color: ${token.colorTextTertiary};
        font-size: 12px;
    `,
    /** The messages scroll inside the panel, so the panel keeps the height the user gave it. */
    body: css`
        flex: 1;
        min-height: 0;
        overflow-y: auto;
        padding: 0 12px 12px;
        border-top: 1px solid ${token.colorBorderSecondary};
    `,
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
    messageError: css`
        border-left-color: ${COMPILE_COLORS.errors};
    `,
    messageWarning: css`
        border-left-color: ${COMPILE_COLORS.warnings};
    `,
    messageAction: css`
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

const buildStatus = (project: Project, state: ProjectCompileState, supportsBranches: boolean): ProjectStatusUpdate => ({
    projectId: project.id,
    branch: supportsBranches ? project.branch || null : null,
    compileState: state,
})

const errorMessagesOf = (status: ProjectStatusUpdate): ProjectStatusDetailedMessage[] =>
    (status.compilation?.messages?.items ?? []).filter(message => message.severity === 'ERROR')

const warningMessagesOf = (status: ProjectStatusUpdate): ProjectStatusDetailedMessage[] =>
    (status.compilation?.messages?.items ?? []).filter(message => message.severity === 'WARN')

const truncateMessage = (value: string): string => {
    const lines = value.split(/\r?\n/)
    const byLines = lines.length > PREVIEW_LINES ? lines.slice(0, PREVIEW_LINES).join('\n') : value
    return byLines.length > PREVIEW_CHARS ? byLines.slice(0, PREVIEW_CHARS).trimEnd() : byLines
}

const isLongMessage = (value: string): boolean =>
    value.length > PREVIEW_CHARS || value.split(/\r?\n/).length > PREVIEW_LINES

const MessageText = ({ value }: { value: string }) => {
    const { styles } = useStyles()
    const { t } = useTranslation('repository')
    const [expanded, setExpanded] = useState(false)
    const long = isLongMessage(value)
    const text = !long || expanded ? value : `${truncateMessage(value)}...`

    useEffect(() => {
        setExpanded(false)
    }, [value])

    return (
        <>
            {text}
            {long && (
                <div>
                    <Button
                        className={styles.messageAction}
                        onClick={() => setExpanded(current => !current)}
                        size="small"
                        type="link"
                    >
                        {expanded ? t('browser.compile.show_less') : t('browser.compile.show_more_text')}
                    </Button>
                </div>
            )}
        </>
    )
}

/** The messages of one severity, paged so a project with hundreds of them stays responsive. */
const MessageGroup = ({ messages, stripeClassName }: {
    messages: ProjectStatusDetailedMessage[]
    stripeClassName: string
}) => {
    const { styles, cx } = useStyles()
    const { t } = useTranslation('repository')
    const [visibleCount, setVisibleCount] = useState(PAGE_SIZE)

    useEffect(() => {
        setVisibleCount(PAGE_SIZE)
    }, [messages])

    if (messages.length === 0) {
        return null
    }
    const visibleMessages = messages.slice(0, visibleCount)
    const remaining = messages.length - visibleCount
    return (
        <>
            <ul className={styles.list}>
                {visibleMessages.map(message => (
                    <li key={message.id} className={cx(styles.message, stripeClassName)} data-testid={`compile-message-${message.id}`}>
                        <MessageText value={message.summary} />
                    </li>
                ))}
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

/**
 * The compilation problems of the project, docked to the bottom of its screen the way the legacy editor
 * listed them: a header that always shows how many errors and warnings there are, above the messages
 * themselves, each marked by the colour stripe of its severity.
 *
 * The panel folds by its header, is dragged taller or lower by its top edge, and keeps both between
 * visits. It only exists while there is something to show — a clean project has no panel at all.
 */
export const CompileProblemsPanel = ({ project, supportsBranches = true, statusReadAt }: {
    project: Project
    supportsBranches?: boolean
    /** When the read carrying the project's compile status started; an older push gives way to it. */
    statusReadAt?: number | undefined
}) => {
    const { styles } = useStyles()
    const { t } = useTranslation('repository')
    const live = COMPILE_RELEVANT_STATUSES.has(project.status)
    const [collapsed, setCollapsed] = useState(() => readStored(COLLAPSED_STORAGE_KEY) === 'yes')
    const [height, setHeight] = useState(loadHeight)
    const liveStatus = useLiveProjectStatus(
        project.id,
        supportsBranches ? project.branch || null : null,
        live,
        live ? project.compileStatus ?? buildStatus(project, 'compiling', supportsBranches) : null,
        statusReadAt
    )
    const status = liveStatus ?? buildStatus(project, 'idle', supportsBranches)

    // Filtered once per status: a fresh array on every render would reset the "show more" pager below on
    // every compile-status push.
    const errors = useMemo(() => errorMessagesOf(status), [status])
    const warnings = useMemo(() => warningMessagesOf(status), [status])

    const fold = (next: boolean) => {
        setCollapsed(next)
        writeStored(COLLAPSED_STORAGE_KEY, next ? 'yes' : 'no')
    }

    // Dragging the top edge sizes the panel; the height it is left at is where it opens next time.
    const startResize = useCallback((event: React.PointerEvent<HTMLHRElement>) => {
        event.preventDefault()
        const bottom = (event.currentTarget.parentElement ?? event.currentTarget).getBoundingClientRect().bottom
        const heightAt = (moved: PointerEvent) =>
            Math.min(MAX_HEIGHT, Math.max(MIN_HEIGHT, Math.round(bottom - moved.clientY)))
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

    if (errors.length === 0 && warnings.length === 0) {
        return null
    }
    const ToggleIcon = collapsed ? UpOutlined : DownOutlined
    return (
        <section
            className={styles.panel}
            data-testid="compile-problems"
            style={collapsed ? undefined : { height }}
        >
            {!collapsed && (
                <hr
                    aria-label={t('browser.compile.resize')}
                    className={styles.resizer}
                    data-testid="compile-problems-resizer"
                    onPointerDown={startResize}
                />
            )}
            <button
                aria-expanded={!collapsed}
                aria-label={t(collapsed ? 'browser.compile.problems_expand' : 'browser.compile.problems_collapse')}
                className={styles.header}
                data-testid="compile-problems-header"
                onClick={() => fold(!collapsed)}
                type="button"
            >
                {errors.length > 0 && (
                    <span className={styles.count} data-testid="compile-problems-errors">
                        <CloseCircleFilled aria-label={t('browser.compile.error_count', { count: errors.length })} className={styles.errorIcon} />
                        {errors.length}
                    </span>
                )}
                {warnings.length > 0 && (
                    <span className={styles.count} data-testid="compile-problems-warnings">
                        <WarningFilled aria-label={t('browser.compile.warning_count', { count: warnings.length })} className={styles.warningIcon} />
                        {warnings.length}
                    </span>
                )}
                <ToggleIcon aria-hidden className={styles.toggle} />
            </button>
            {!collapsed && (
                <div className={styles.body} data-testid="compile-problems-body">
                    <MessageGroup messages={errors} stripeClassName={styles.messageError} />
                    <MessageGroup messages={warnings} stripeClassName={styles.messageWarning} />
                </div>
            )}
        </section>
    )
}
