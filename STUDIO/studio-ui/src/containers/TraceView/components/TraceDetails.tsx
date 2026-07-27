import React, { useEffect, useMemo, useState } from 'react'
import { Spin, Empty, Alert, Card } from 'antd'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
import traceService from 'services/traceService'
import type { TraceParameterValue } from 'types/trace'
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
 *
 * In the simple mode a clicked step keeps its own identity here: the panel is titled by the step, shows
 * the step's inputs and result, and renders the step's OWNING table with its cell highlighted — never
 * the frame the suspension happens to be paused in.
 */
const TraceDetails: React.FC = () => {
    const { t } = useTranslation('trace')
    const { styles, cx } = useStyles()
    const frames = useTraceStore(s => s.frames)
    const selectedFrameIndex = useTraceStore(s => s.selectedFrameIndex)
    const variables = useTraceStore(s => s.variables)
    const variablesLoading = useTraceStore(s => s.variablesLoading)
    const advanced = useTraceStore(s => s.advanced)
    const focus = useTraceStore(s => (s.advanced ? null : s.simpleFocus))
    const projectId = useTraceStore(s => s.projectId)
    const stackVersion = useTraceStore(s => s.stackVersion)

    // Context is shown alongside the input parameters. Memoized so the parameter tree and its copy button
    // keep a stable prop reference and don't re-render on every unrelated store change.
    const allParameters = useMemo(() => {
        const parameters = variables?.parameters
        const context = variables?.context ?? undefined
        return context ? [...(parameters || []), context] : parameters
    }, [variables])

    // A focused step is presented from its owning table's frame: the deepest published frame of that
    // table anchors the traced-table view, the step's cell highlight, and the step-inputs fetch.
    const ownerIndex = useMemo(() => {
        if (!focus) {
            return -1
        }
        for (let i = frames.length - 1; i >= 0; i -= 1) {
            const candidate = frames[i]
            if (candidate && candidate.uri === focus.ownerUri && candidate.instance === focus.ownerInstance) {
                return i
            }
        }
        return -1
    }, [focus, frames])

    // The step's inputs in the formula's own terms ($LimitIndex, MaxLimit, currentFinancialData…) come
    // from a dedicated endpoint; re-fetched on every settle since a later stop records more values.
    const [stepInputs, setStepInputs] = useState<TraceParameterValue[] | null>(null)
    useEffect(() => {
        if (!focus || !projectId || ownerIndex < 0) {
            setStepInputs(null)
            return undefined
        }
        let cancelled = false
        traceService.getStepInputs(projectId, ownerIndex, focus.ref)
            .then(inputs => {
                if (!cancelled) setStepInputs(inputs)
            })
            .catch(() => {
                if (!cancelled) setStepInputs(null)
            })
        return () => {
            cancelled = true
        }
    }, [focus, projectId, ownerIndex, stackVersion])

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

    const stepView = focus && ownerIndex >= 0 ? focus : null
    // A focused step is presented like the classic trace presented a spreadsheet cell, sharpened to the
    // step itself: the Parameters are the values its formula consumed (the fetched step inputs), and the
    // result is the step's own frozen value, named `return` — the raw cell ref would read as jargon.
    const stepValue = stepView ? variables?.steps.find(s => s.ref === stepView.ref)?.value : undefined
    const title = stepView ? stepView.label : frame?.name
    const tableIndex = stepView ? ownerIndex : selectedFrameIndex
    const highlightCell = stepView
        ? frames[ownerIndex]?.steps?.find(s => s.ref === stepView.ref)?.cell
        : undefined
    const shownParameters = stepView ? stepInputs ?? undefined : allParameters
    const shownResult = stepView
        ? (stepValue ? { ...stepValue, name: 'return' } : undefined)
        : result

    return (
        <div className={styles.details} data-testid="debug-details">
            {title && <span className={styles.frameTitle}>{title}</span>}
            {/* Parameters and Result come first, so they stay reachable above a large traced table. */}
            {variablesLoading ? (
                <div className={styles.detailsCentered}>
                    <Spin description={t('loadingDetails')} />
                </div>
            ) : (
                <>
                    <TraceParameters
                        copyButton={<CopyJsonButton data={shownParameters} tooltipKey="copy.parameters" />}
                        emptyText={t('details.noParameters')}
                        parameters={shownParameters}
                        title={t('details.parameters')}
                    />
                    <SingleParameter
                        copyButton={<CopyJsonButton data={shownResult} tooltipKey="copy.result" />}
                        emptyText={t('details.noResult')}
                        parameter={shownResult}
                        title={t('details.result')}
                    />
                </>
            )}
            {/* Source table: the owning table of a focused step (its cell highlighted), else the frame's. */}
            {/* The business view mutes everything but the highlighted calculation, like the legacy trace. */}
            <TraceTableView dimOthers={!advanced} frameIndex={tableIndex} highlightCell={highlightCell} />
            <TraceLegend />
            {!variablesLoading && (
                <>
                    {/* The frame-level panels belong to the frame view; a focused step keeps its own
                        identity and shows only its inputs, result, and owning table. */}
                    {!stepView && frame?.kind === 'spreadsheet' && (
                        <SpreadsheetGrid
                            columns={variables?.gridColumns}
                            frameUri={frame.uri}
                            rows={variables?.gridRows}
                            steps={variables?.steps}
                        />
                    )}
                    {/* For decision tables, always offer the rule-fired breakpoint; the firing is
                        explained once a rule fires. */}
                    {!stepView && frame?.kind === 'decisionTable' && (
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
