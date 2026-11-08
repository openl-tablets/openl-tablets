import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, Button, Empty, Progress, Tooltip } from 'antd'
import { ReloadOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { cancelModuleCompilation } from '../../services/modules'
import { useSharedStyles } from '../projects/sharedStyles'
import { CompileDot, getCompileTooltip, isNoteworthyCompileState } from '../projects/CompileIndicator'
import type { ModuleCompilation } from './useModuleCompilation'

const useStyles = createStyles(({ css }) => ({
    /** The progress of the compilation the screen waits for, as wide as a line of text. */
    progress: css`
        width: 320px;
        max-width: 100%;
    `,
}))

/** Whether a screen still waits for the compilation instead of showing what was compiled. */
export const waitsForCompilation = (compilation: ModuleCompilation): boolean =>
    compilation.failure !== null || !compilation.ready

interface CompilationWaitProps {
    compilation: ModuleCompilation
    projectId: string
    /** The module the screen waits for: the one it is of, or the one the project is compiled through. */
    moduleName: string
    /** Compiles the module again, after the reader stopped the compilation. */
    onRefresh: () => void
    /** The compilation could not be stopped. */
    onCancelFailed: (error: unknown) => void
}

/**
 * What stands in for a screen while the compilation it waits for runs.
 *
 * <p>The progress counts the modules compiled, as the status channel reports them, so it moves as each module
 * finishes. The reader may stop the wait. What was compiled is kept then, and Refresh compiles the module again.
 *
 * <p>A compilation that could not be asked for is told instead. Nothing is drawn once the module is compiled.
 */
export const CompilationWait = ({
    compilation,
    projectId,
    moduleName,
    onRefresh,
    onCancelFailed,
}: CompilationWaitProps) => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()
    const { styles: shared } = useSharedStyles()
    const [cancelling, setCancelling] = useState(false)

    const cancel = () => {
        setCancelling(true)
        cancelModuleCompilation(projectId, moduleName)
            .catch(onCancelFailed)
            .finally(() => setCancelling(false))
    }

    if (compilation.failure !== null) {
        return (
            <div className={shared.workspaceCentered}>
                <Alert
                    showIcon
                    data-testid="module-compile-failed"
                    description={compilation.failure}
                    title={t('browser.module.compile_failed')}
                    type="error"
                />
            </div>
        )
    }
    if (compilation.ready) {
        return null
    }
    if (compilation.state === 'cancelled') {
        return (
            <div className={shared.workspaceCentered} data-testid="module-compile-cancelled">
                <Empty description={t('browser.module.compile_cancelled', { module: moduleName })}>
                    <Button icon={<ReloadOutlined />} onClick={onRefresh} type="primary">
                        {t('browser.module.refresh')}
                    </Button>
                </Empty>
            </div>
        )
    }
    return (
        <div className={shared.workspaceCentered} data-testid="module-compiling">
            <Progress
                className={styles.progress}
                percent={compilation.total === 0 ? 0 : Math.round((compilation.compiled / compilation.total) * 100)}
                status="active"
            />
            <span>
                {t('browser.module.compiling', {
                    module: moduleName,
                    compiled: compilation.compiled,
                    total: compilation.total,
                })}
            </span>
            <Button data-testid="module-compile-cancel" loading={cancelling} onClick={cancel}>
                {t('browser.module.compile_cancel')}
            </Button>
        </div>
    )
}

interface CompilationMarkProps {
    compilation: ModuleCompilation
    /** Nothing can be compiled in a project nobody opened. */
    disabled: boolean
    /** Compiles the module again and reads the screen afresh. */
    onRefresh: () => void
}

/**
 * The mark beside the title of a screen that shows what was compiled, and the button that compiles it again.
 *
 * <p>The mark is the one the project screens carry: nothing for a compilation that raised nothing, and a coloured
 * dot where it raised something. A compilation under way also says how far it has come.
 */
export const CompilationMark = ({ compilation, disabled, onRefresh }: CompilationMarkProps) => {
    const { t } = useTranslation('repository')
    const compiling = compilation.state === 'compiling'

    return (
        <>
            {isNoteworthyCompileState(compilation.state) && (
                <CompileDot
                    showLabel={compiling}
                    state={compilation.state}
                    testId="module-compile-state"
                    tooltip={getCompileTooltip(compilation.status, compilation.state, t)}
                    label={compiling && compilation.total > 0
                        ? t('browser.module.compile_progress', {
                            compiled: compilation.compiled,
                            total: compilation.total,
                        })
                        : undefined}
                />
            )}
            <Tooltip title={t('browser.module.refresh')}>
                <Button
                    aria-label={t('browser.module.refresh')}
                    data-testid="module-refresh"
                    disabled={disabled}
                    icon={<ReloadOutlined />}
                    onClick={onRefresh}
                    type="text"
                />
            </Tooltip>
        </>
    )
}
