import React, { useEffect, useState } from 'react'
import { Card, Spin, Empty } from 'antd'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
import traceService from 'services/traceService'
import { NotFoundError, isApiHttpError } from 'services'
import type { HighlightState, RawTableCell } from 'types/trace'
import { RawTableGrid } from 'components/RawTableGrid'
import { useStyles } from './TraceTableView.styles'

interface TraceTableViewProps {
    frameIndex: number
    /** When set, highlight exactly this A1 cell as current instead of fetching the frame's highlights. */
    highlightCell?: string | null | undefined
    /** Mute every non-highlighted cell to grey (the business view), so the highlights carry the colour. */
    dimOthers?: boolean | undefined
}

/**
 * Renders a stack frame's table from the raw Tables API grid and overlays the trace highlights
 * (current line, result, matched/unmatched conditions) by A1 cell address — no HTML injection.
 *
 * The raw grid is immutable during a session, so it is cached per table: revisiting a frame is instant
 * and stepping never reloads the structure — only the small highlight overlay is refetched.
 */
const TraceTableView: React.FC<TraceTableViewProps> = ({ frameIndex, highlightCell, dimOthers }) => {
    const { t } = useTranslation('trace')
    const { styles, cx } = useStyles()
    const projectId = useTraceStore(s => s.projectId)
    const stackVersion = useTraceStore(s => s.stackVersion)
    const tableId = useTraceStore(s => s.frames[frameIndex]?.tableId)
    const table = useTraceStore(s => (tableId ? s.rawTableCache[tableId] : undefined)) ?? null
    const loadRawTable = useTraceStore(s => s.loadRawTable)

    const [highlights, setHighlights] = useState<Record<string, HighlightState>>({})
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)

    // Structure: fetched once per table, served from the session cache afterwards.
    useEffect(() => {
        if (!tableId || table) {
            setError(null)
            setLoading(false)
            return
        }
        let cancelled = false
        setLoading(true)
        setError(null)
        loadRawTable(tableId)
            .catch((err: unknown) => {
                if (cancelled) return
                // 404 means the frame has no table view; render nothing.
                if (!(err instanceof NotFoundError) && !(isApiHttpError(err) && err.status === 404)) {
                    setError(err instanceof Error ? err.message : t('errors.tableFailed'))
                }
            })
            .finally(() => {
                if (!cancelled) setLoading(false)
            })
        return () => {
            cancelled = true
        }
    }, [tableId, table, loadRawTable, t])

    // Highlights move as execution advances; the cached structure stays put, so stepping is flicker-free.
    useEffect(() => {
        // A caller that knows the exact cell to point at (a clicked step) paints it directly.
        if (highlightCell) {
            setHighlights({ [highlightCell]: 'current' })
            return
        }
        if (!projectId || !tableId) {
            setHighlights({})
            return
        }
        let cancelled = false
        traceService
            .getFrameHighlights(projectId, frameIndex)
            .then(list => {
                if (!cancelled) setHighlights(Object.fromEntries(list.map(h => [h.cell, h.state])))
            })
            .catch(() => {
                if (!cancelled) setHighlights({})
            })
        return () => {
            cancelled = true
        }
    }, [projectId, frameIndex, stackVersion, tableId, highlightCell])

    if (loading) {
        return (
            <Card className={styles.card} size="small" title={t('details.table')}>
                <div className={styles.loading}>
                    <Spin description={t('loadingTable')} />
                </div>
            </Card>
        )
    }

    if (error) {
        return (
            <Card className={styles.card} size="small" title={t('details.table')}>
                <Empty description={error} />
            </Card>
        )
    }

    const rows = table?.source ?? []
    if (!table || rows.length === 0) {
        return null
    }

    // Mute the rest of the table only when there is a calculation to point at — a table with no
    // highlights stays fully readable.
    const dim = !!dimOthers && Object.keys(highlights).length > 0

    // The effective highlight of a cell: the dimmed business view suppresses the 'current' colour (the clicked
    // cell already stands out undimmed), so the legend below keys only the colours actually painted.
    const paintedState = (state: HighlightState | undefined): HighlightState | undefined =>
        dim && state === 'current' ? undefined : state
    const paintedStates = new Set(
        Object.values(highlights).map(paintedState).filter((state): state is HighlightState => state !== undefined)
    )
    const legend: { state: HighlightState; swatch: string; label: string }[] = [
        { state: 'current', swatch: styles.swatchCurrent, label: 'legend.current' },
        { state: 'result', swatch: styles.swatchResult, label: 'legend.result' },
        { state: 'conditionTrue', swatch: styles.swatchMet, label: 'legend.conditionMet' },
        { state: 'conditionFalse', swatch: styles.swatchNotMet, label: 'legend.conditionNotMet' },
    ]
    const shownLegend = legend.filter(item => paintedStates.has(item.state))

    // In the dimmed business view the current cell needs no colour of its own — it is the only cell
    // left uncoloured, so it already stands out; keep it exactly as the table draws it. A decision
    // table's matched conditions and result still carry meaning and keep their highlight.
    const decorate = (cell: RawTableCell) => {
        const state = cell.cell ? highlights[cell.cell] : undefined
        const painted = paintedState(state)
        return {
            className: cx(painted && styles[painted], dim && !state && styles.dimmed),
            painted: !!painted,
        }
    }

    return (
        <Card className={styles.card} size="small" title={t('details.table')}>
            <div className={styles.content}>
                <RawTableGrid decorate={decorate} rows={rows} testId="trace-table" />
            </div>
            {shownLegend.length > 0 && (
                <div className={styles.legend} data-testid="trace-legend">
                    {shownLegend.map(item => (
                        <span key={item.state} className={styles.legendItem}>
                            <span className={cx(styles.swatch, item.swatch)} />
                            {t(item.label)}
                        </span>
                    ))}
                </div>
            )}
            {table.totalRows != null && (
                <div className={styles.truncated}>
                    {t('table.truncated', { count: rows.length, total: table.totalRows })}
                </div>
            )}
        </Card>
    )
}

export default TraceTableView
