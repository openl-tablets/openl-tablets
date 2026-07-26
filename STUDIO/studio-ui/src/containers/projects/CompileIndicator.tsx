import { useTranslation } from 'react-i18next'
import type { TFunction } from 'i18next'
import { Tooltip } from 'antd'
import { createStyles, keyframes } from 'antd-style'
import { ProjectStatus } from '../../constants/project'
import { COMPILE_RELEVANT_STATUSES } from '../../constants/projectStatusMeta'
import { type ProjectCompileState, type ProjectStatusUpdate } from '../../services/projectStatus'
import { COMPILE_COLORS } from './projectsTheme'
import { useSharedStyles } from './sharedStyles'

// Only the compiling state animates: a soft pulse on its dot — the one state-driven motion moment.
const pulse = keyframes`
    0%, 100% { box-shadow: 0 0 0 0 ${COMPILE_COLORS.compiling}88; }
    50% { box-shadow: 0 0 0 5px ${COMPILE_COLORS.compiling}00; }
`

const useStyles = createStyles(({ css, token }) => ({
    bare: css`
        display: inline-flex;
        align-items: center;
    `,
    // State chip: a coloured dot (state hue) plus a label in a fully rounded pill.
    chip: css`
        display: inline-flex;
        align-items: center;
        gap: 7px;
        padding: 1px 10px 1px 9px;
        border: 1px solid ${token.colorBorderSecondary};
        border-radius: 999px;
        background: ${token.colorFillQuaternary};
        color: ${token.colorText};
        font-size: 12px;
        line-height: 20px;
        white-space: nowrap;
    `,
    dotPulse: css`
        animation: ${pulse} 1.4s ease-in-out infinite;

        @media (prefers-reduced-motion: reduce) {
            animation: none;
            box-shadow: 0 0 0 3px ${COMPILE_COLORS.compiling}4d;
        }
    `,
}))

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
 * Whether a compile state is worth showing to the user: compilation in progress, or a result carrying
 * warnings or errors. A clean ({@code ok}) or not-yet-compiled ({@code idle}) project shows nothing.
 */
export const isNoteworthyCompileState = (state: ProjectCompileState): boolean =>
    state === 'compiling' || state === 'warnings' || state === 'errors'

/**
 * Presentational compilation indicator: a coloured state dot in a rounded pill (optionally labelled),
 * encoding the compile state by hue. The compiling state pulses. Carries no data-fetching of its own.
 */
export const CompileDot = ({ state, showLabel, testId, tooltip }: CompileDotProps) => {
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const { t } = useTranslation('repository')
    const label = t(`browser.compile.${state}`)
    const title = tooltip ?? label
    return (
        <Tooltip title={title}>
            <span aria-label={title} className={showLabel ? styles.chip : styles.bare} data-testid={testId} role="img">
                <span
                    aria-hidden
                    className={cx(shared.stateDot, state === 'compiling' && styles.dotPulse)}
                    style={{ background: COMPILE_COLORS[state] }}
                />
                {showLabel && <span>{label}</span>}
            </span>
        </Tooltip>
    )
}

interface RowCompileDotProps {
    status: ProjectStatus
    compileStatus?: ProjectStatusUpdate | undefined
}

/**
 * Compile dot for a list row. Purely presentational: the projects page response bootstraps the
 * state, and the screen keeps it live from its one workspace-wide status subscription — a row never
 * subscribes on its own.
 */
export const RowCompileDot = ({ status, compileStatus }: RowCompileDotProps) => {
    const { t } = useTranslation('repository')
    const live = COMPILE_RELEVANT_STATUSES.has(status)

    const state = live ? compileStatus?.compileState ?? 'idle' : 'idle'
    if (!isNoteworthyCompileState(state)) {
        return null
    }
    return <CompileDot state={state} tooltip={getCompileTooltip(compileStatus, state, t)} />
}
