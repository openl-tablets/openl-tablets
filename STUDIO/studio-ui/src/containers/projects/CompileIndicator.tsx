import { useEffect, useState, type ComponentType } from 'react'
import { useTranslation } from 'react-i18next'
import type { TFunction } from 'i18next'
import {
    CheckCircleFilled,
    CloseCircleFilled,
    ExclamationCircleFilled,
    LoadingOutlined,
    MinusCircleOutlined,
} from '@ant-design/icons'
import { createStyles, useTheme } from 'antd-style'
import { ProjectStatus } from '../../constants/project'
import { COMPILE_RELEVANT_STATUSES } from '../../constants/projectStatusMeta'
import {
    subscribeProjectStatus,
    type ProjectCompileState,
    type ProjectStatusUpdate,
} from '../../services/projectStatus'

const useStyles = createStyles(({ css }) => ({
    dot: css`
        display: inline-flex;
        align-items: center;
        gap: 6px;
        line-height: 1;

        .anticon {
            font-size: 15px;
        }
    `,
    label: css`
        font-size: 12px;
    `,
}))

type ColorToken = 'colorSuccess' | 'colorWarning' | 'colorError' | 'colorInfo' | 'colorTextQuaternary'

interface CompileMeta {
    icon: ComponentType<{ style?: React.CSSProperties; spin?: boolean }>
    color: ColorToken
    spin?: boolean
}

const COMPILE_META: Record<ProjectCompileState, CompileMeta> = {
    ok: { icon: CheckCircleFilled, color: 'colorSuccess' },
    warnings: { icon: ExclamationCircleFilled, color: 'colorWarning' },
    errors: { icon: CloseCircleFilled, color: 'colorError' },
    compiling: { icon: LoadingOutlined, color: 'colorInfo', spin: true },
    idle: { icon: MinusCircleOutlined, color: 'colorTextQuaternary' },
}

interface CompileDotProps {
    state: ProjectCompileState
    showLabel?: boolean
    testId?: string
    tooltip?: string
}

export const getCompileTooltip = (
    status: ProjectStatusUpdate | null | undefined,
    fallbackState: ProjectCompileState,
    t: TFunction<'repository'>
): string => {
    const messages = status?.compilation?.messages
    const parts: string[] = []
    if (messages && messages.errors > 0) {
        parts.push(t('browser.compile.error_count', { count: messages.errors }))
    }
    if (messages && messages.warnings > 0) {
        parts.push(t('browser.compile.warning_count', { count: messages.warnings }))
    }
    return parts.length > 0 ? parts.join(', ') : t(`browser.compile.${fallbackState}`)
}

/**
 * Presentational compilation indicator: a coloured glyph (optionally labelled) encoding the compile state
 * through both hue and shape, matching the mockup's dot. Carries no data-fetching of its own.
 */
export const CompileDot = ({ state, showLabel, testId, tooltip }: CompileDotProps) => {
    const { styles } = useStyles()
    const { t } = useTranslation('repository')
    const token = useTheme()
    const meta = COMPILE_META[state]
    const Icon = meta.icon
    const label = t(`browser.compile.${state}`)
    const title = tooltip ?? label
    return (
        <span aria-label={title} className={styles.dot} data-testid={testId} role="img" title={title}>
            <Icon spin={meta.spin === true} style={{ color: token[meta.color] }} />
            {showLabel && <span className={styles.label}>{label}</span>}
        </span>
    )
}

interface RowCompileDotProps {
    status: ProjectStatus
    projectId: string
    branch: string | null
    initialStatus?: ProjectStatusUpdate | undefined
}

/**
 * Compile dot for a list row. The projects page response bootstraps the state; visible rows subscribe
 * to the same project-status WebSocket channel as the project workspace.
 */
export const RowCompileDot = ({ status, projectId, branch, initialStatus }: RowCompileDotProps) => {
    const { t } = useTranslation('repository')
    const live = COMPILE_RELEVANT_STATUSES.has(status)
    const [currentStatus, setCurrentStatus] = useState<ProjectStatusUpdate | null>(initialStatus ?? null)

    useEffect(() => {
        if (!live) {
            setCurrentStatus(null)
            return
        }
        let cancelled = false
        setCurrentStatus(initialStatus ?? null)
        const subscription = subscribeProjectStatus(projectId, branch, update => {
            if (!cancelled) {
                setCurrentStatus(update)
            }
        })
        return () => {
            cancelled = true
            subscription.unsubscribe()
        }
    }, [projectId, branch, initialStatus, live])

    const state = live ? currentStatus?.compileState ?? initialStatus?.compileState ?? 'idle' : 'idle'
    const tooltip = getCompileTooltip(currentStatus ?? initialStatus, state, t)
    return <CompileDot state={state} tooltip={tooltip} />
}
