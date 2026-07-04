import React, { useMemo } from 'react'
import { Spin, Empty, Alert, Card } from 'antd'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
import TraceParameters, { SingleParameter } from './TraceParameters'
import TraceTableView from './TraceTableView'
import SpreadsheetGrid from './SpreadsheetGrid'
import DecisionPanel from './DecisionPanel'
import CopyJsonButton from './CopyJsonButton'
import type { MessageDescription } from 'types/trace'
import { useStyles } from './TraceDetails.styles'

/**
 * Component for displaying trace errors/warnings.
 */
/** Map a message severity to the matching Ant Design Alert type. */
const alertType = (severity: MessageDescription['severity']): 'error' | 'warning' | 'info' => {
    if (severity === 'ERROR') {
        return 'error'
    }
    if (severity === 'WARNING') {
        return 'warning'
    }
    return 'info'
}

const TraceErrors: React.FC<{ errors?: MessageDescription[] | undefined }> = ({ errors }) => {
    const { t } = useTranslation('trace')
    const { styles } = useStyles()

    if (!errors || errors.length === 0) {
        return null
    }

    return (
        <Card className={styles.errorsCard} size="small" title={t('details.errors')}>
            {errors.map(error => (
                <Alert
                    key={`${error.severity}-${error.summary}-${error.sourceLocation ?? ''}`}
                    showIcon
                    style={{ marginBottom: 8 }}
                    title={error.summary}
                    type={alertType(error.severity)}
                    description={
                        <>
                            {error.detail && <div>{error.detail}</div>}
                            {error.sourceLocation && (
                                <div className={styles.errorLocation}>
                                    {error.sourceLocation}
                                </div>
                            )}
                        </>
                    }
                />
            ))}
        </Card>
    )
}

/**
 * Compact key to the execution-state colours shared by the traced table, the spreadsheet grid and the
 * decision panel, so a reader can tell at a glance what each highlight means.
 */
const TraceLegend: React.FC = () => {
    const { t } = useTranslation('trace')
    const { styles, cx } = useStyles()
    return (
        <div className={styles.legend} data-testid="trace-legend">
            <span className={styles.legendItem}>
                <span className={cx(styles.swatch, styles.swatchCurrent)} />
                {t('legend.current')}
            </span>
            <span className={styles.legendItem}>
                <span className={cx(styles.swatch, styles.swatchResult)} />
                {t('legend.result')}
            </span>
            <span className={styles.legendItem}>
                <span className={cx(styles.swatch, styles.swatchMet)} />
                {t('legend.conditionMet')}
            </span>
            <span className={styles.legendItem}>
                <span className={cx(styles.swatch, styles.swatchNotMet)} />
                {t('legend.conditionNotMet')}
            </span>
        </div>
    )
}

/**
 * Right panel: the selected stack frame's table and frozen variables.
 */
const TraceDetails: React.FC = () => {
    const { t } = useTranslation('trace')
    const { styles, cx } = useStyles()
    const frames = useTraceStore(s => s.frames)
    const selectedFrameIndex = useTraceStore(s => s.selectedFrameIndex)
    const variables = useTraceStore(s => s.variables)
    const variablesLoading = useTraceStore(s => s.variablesLoading)

    // Context is shown alongside the input parameters. Memoized so the parameter tree and its copy button
    // keep a stable prop reference and don't re-render on every unrelated store change.
    const allParameters = useMemo(() => {
        const parameters = variables?.parameters
        const context = variables?.context ?? undefined
        return context ? [...(parameters || []), context] : parameters
    }, [variables])

    if (selectedFrameIndex === null) {
        return (
            <div className={cx(styles.details, styles.detailsCentered)}>
                <Empty
                    description={t('details.noSelection')}
                    image={Empty.PRESENTED_IMAGE_SIMPLE}
                />
            </div>
        )
    }

    const frame = frames[selectedFrameIndex]
    const result = variables?.result ?? undefined
    const errors = variables?.errors

    return (
        <div className={styles.details} data-testid="debug-details">
            {frame && <span className={styles.frameTitle}>{frame.name}</span>}
            {/* Parameters and Result come first, so they stay reachable above a large traced table. */}
            {variablesLoading ? (
                <div className={styles.detailsCentered}>
                    <Spin description={t('loadingDetails')} />
                </div>
            ) : (
                <>
                    <TraceParameters
                        copyButton={<CopyJsonButton data={allParameters} tooltipKey="copy.parameters" />}
                        emptyText={t('details.noParameters')}
                        parameters={allParameters}
                        title={t('details.parameters')}
                    />
                    <SingleParameter
                        copyButton={<CopyJsonButton data={result} tooltipKey="copy.result" />}
                        emptyText={t('details.noResult')}
                        parameter={result}
                        title={t('details.result')}
                    />
                </>
            )}
            {/* Source table of the current frame, with the current line highlighted. */}
            <TraceTableView frameIndex={selectedFrameIndex} />
            <TraceLegend />
            {!variablesLoading && (
                <>
                    {/* For spreadsheets, also show the steps as a grid with per-cell breakpoints. */}
                    {frame?.kind === 'spreadsheet' && (
                        <SpreadsheetGrid
                            columns={variables?.gridColumns}
                            frameUri={frame.uri}
                            rows={variables?.gridRows}
                            steps={variables?.steps}
                        />
                    )}
                    {/* For decision tables, always offer the rule-fired breakpoint; the firing is
                        explained once a rule fires. */}
                    {frame?.kind === 'decisionTable' && (
                        <DecisionPanel
                            decision={variables?.decision ?? null}
                            frameName={frame.name}
                            frameUri={frame.uri}
                            ruleNames={variables?.ruleNames ?? null}
                        />
                    )}
                    <TraceErrors errors={errors} />
                </>
            )}
        </div>
    )
}

export default TraceDetails
