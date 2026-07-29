import React, { useEffect, useMemo, useState } from 'react'
import { Spin, Empty, Alert, Card } from 'antd'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
import traceService from 'services/traceService'
import type { StepInputsView } from 'types/trace'
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

    // A focused step is self-contained: its inputs in the formula's own terms ($LimitIndex, MaxLimit,
    // currentFinancialData…), its own returned value, and its cell address all come from one endpoint —
    // the heavy frame-variables payload is never fetched. Re-fetched on every settle since a later stop
    // records more values.
    const [stepInputs, setStepInputs] = useState<StepInputsView | null>(null)
    const [stepInputsLoading, setStepInputsLoading] = useState(false)
    useEffect(() => {
        if (!focus || !projectId || ownerIndex < 0) {
            setStepInputs(null)
            setStepInputsLoading(false)
            return undefined
        }
        let cancelled = false
        setStepInputsLoading(true)
        traceService.getStepInputs(projectId, ownerIndex, focus.ref)
            .then(step => {
                if (!cancelled) setStepInputs(step)
            })
            .catch(() => {
                if (!cancelled) setStepInputs(null)
            })
            .finally(() => {
                if (!cancelled) setStepInputsLoading(false)
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
    // step itself: the Parameters are the values its formula consumed and the Result is the step's own
    // returned value (already named `return`) — both from the single step-inputs payload.
    // Only the advanced view shows a title, and it never has a focused step (focus is forced to null there),
    // so the title is always the frame's name — never a step label.
    const title = frame?.name
    const tableIndex = stepView ? ownerIndex : selectedFrameIndex
    // The cell address comes from the owner's live step outline; a static cell is absent there, so fall
    // back to the step-inputs payload, which addresses every cell including static ones.
    const highlightCell = stepView
        ? frames[ownerIndex]?.steps?.find(s => s.ref === stepView.ref)?.cell ?? stepInputs?.cell ?? undefined
        : undefined
    const shownParameters = stepView ? stepInputs?.inputs ?? undefined : allParameters
    const shownResult = stepView ? stepInputs?.result ?? undefined : result
    // A focused step shows its own error — the step the run failed on carries it — so clicking the failing
    // step explains why it failed, not only the whole table's frame, like the advanced view shows both.
    const shownErrors = stepView ? stepInputs?.errors ?? undefined : errors
    // A focused step is self-contained from step-inputs; only the frame view waits on the variables payload.
    const loadingDetails = stepView ? stepInputsLoading : variablesLoading

    return (
        <div className={styles.details} data-testid="debug-details">
            {/* The business view already names the rule in the tree; a title here would just duplicate it. */}
            {advanced && title && <span className={styles.frameTitle}>{title}</span>}
            {/* Parameters and Result come first, so they stay reachable above a large traced table. */}
            {loadingDetails ? (
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
            {!loadingDetails && (
                <>
                    {/* The frame-level panels belong to the frame view; a focused step keeps its own
                        identity and shows only its inputs, result, and owning table. They are an advanced
                        detail: the business view already shows the steps and the decision breakdown in the
                        tree, so the extra grid/decision table below the traced table would just duplicate it. */}
                    {advanced && !stepView && frame?.kind === 'spreadsheet' && (
                        <SpreadsheetGrid
                            columns={variables?.gridColumns}
                            frameUri={frame.uri}
                            rows={variables?.gridRows}
                            steps={variables?.steps}
                        />
                    )}
                    {/* For decision tables, always offer the rule-fired breakpoint; the firing is
                        explained once a rule fires. */}
                    {advanced && !stepView && frame?.kind === 'decisionTable' && (
                        <DecisionPanel
                            decision={variables?.decision ?? null}
                            frameName={frame.name}
                            frameUri={frame.uri}
                            ruleNames={variables?.ruleNames ?? null}
                        />
                    )}
                    <TraceErrors errors={shownErrors} />
                </>
            )}
        </div>
    )
}

export default TraceDetails
