import { useCallback, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
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
import { moduleRoute } from '../../services/projectId'
import { CompileMessages } from '../../components/CompileMessages'
import { ResizeHandle, useDragSize } from '../../components/ResizeHandle'
import { COMPILE_COLORS, MOCKUP } from './projectsTheme'


/** How low and how tall the panel may be dragged, and where it opens the first time. */
const HEIGHT = { min: 120, max: 600, fallback: 240 }

const HEIGHT_STORAGE_KEY = 'openl.project.problems.height'
const COLLAPSED_STORAGE_KEY = 'openl.project.problems.collapsed'

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
    const navigate = useNavigate()
    const live = COMPILE_RELEVANT_STATUSES.has(project.status)
    const [collapsed, setCollapsed] = useState(() => readStored(COLLAPSED_STORAGE_KEY) === 'yes')
    const { size: height, startResize } = useDragSize(HEIGHT_STORAGE_KEY, 'top', HEIGHT)
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

    // Every message already says where it came from — the project, the module and the table — so opening one
    // costs no request: a project raising a thousand messages still asks the server nothing to be read.
    const open = useCallback((message: ProjectStatusDetailedMessage) => {
        const where = message.location
        if (!where) {
            return
        }
        const module = where.type === 'table' ? where.module : where.name
        if (!module) {
            return
        }
        navigate(moduleRoute(where.projectId ?? project.id, module, where.type === 'table' ? where.id : undefined))
    }, [navigate, project.id])

    // A module is compiled for one session at a time, so while that is running the reader is kept where they
    // are: opening another module would only queue behind it. The compiling screen offers to stop it.
    const canOpen = useCallback((message: ProjectStatusDetailedMessage) => {
        if (status.compileState === 'compiling') {
            return false
        }
        const where = message.location
        return !!where && !!(where.type === 'table' ? where.module : where.name)
    }, [status.compileState])

    const fold = (next: boolean) => {
        setCollapsed(next)
        writeStored(COLLAPSED_STORAGE_KEY, next ? 'yes' : 'no')
    }

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
                <ResizeHandle edge="top" onPointerDown={startResize} testId="compile-problems-resizer" />
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
                    <CompileMessages
                        canOpen={canOpen}
                        messages={errors}
                        onOpen={open}
                        severity="error"
                    />
                    <CompileMessages
                        canOpen={canOpen}
                        messages={warnings}
                        onOpen={open}
                        severity="warning"
                    />
                </div>
            )}
        </section>
    )
}
