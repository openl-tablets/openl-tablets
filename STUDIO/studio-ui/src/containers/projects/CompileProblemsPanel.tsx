import { useCallback, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
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
import { CompileMessages } from '../../components/CompileMessages'
import { COMPILE_COLORS, MOCKUP } from './projectsTheme'


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
    // A running compilation reports how many problems it has raised, not which — resolving each to its table
    // is work it does not do while it runs. The panel stands on those counts, so it does not disappear under
    // a reader the moment a compilation starts and return only when it ends.
    const errorCount = status.compilation?.messages?.errors ?? errors.length
    const warningCount = status.compilation?.messages?.warnings ?? warnings.length

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

    if (errorCount === 0 && warningCount === 0 && errors.length === 0 && warnings.length === 0) {
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
                {errorCount > 0 && (
                    <span className={styles.count} data-testid="compile-problems-errors">
                        <CloseCircleFilled aria-label={t('browser.compile.error_count', { count: errorCount })} className={styles.errorIcon} />
                        {errorCount}
                    </span>
                )}
                {warningCount > 0 && (
                    <span className={styles.count} data-testid="compile-problems-warnings">
                        <WarningFilled aria-label={t('browser.compile.warning_count', { count: warningCount })} className={styles.warningIcon} />
                        {warningCount}
                    </span>
                )}
                <ToggleIcon aria-hidden className={styles.toggle} />
            </button>
            {!collapsed && (
                <div className={styles.body} data-testid="compile-problems-body">
                    <CompileMessages messages={errors} severity="error" />
                    <CompileMessages messages={warnings} severity="warning" />
                </div>
            )}
        </section>
    )
}
