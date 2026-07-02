import React, { useEffect, useState } from 'react'
import { Card, Spin, Empty } from 'antd'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
import traceService from 'services/traceService'
import { NotFoundError, isApiHttpError } from 'services'
import type { HighlightState, RawTableCell } from 'types/trace'
import { useStyles } from './TraceTableView.styles'

interface TraceTableViewProps {
    frameIndex: number
}

const formatValue = (value: RawTableCell['value']): string => (value == null ? '' : String(value))

/**
 * The cell's Excel styling. The trace highlight (a class) must win over the Excel background, so the
 * background is painted only when the cell is not highlighted; font and alignment always apply.
 */
const cellStyle = (s: RawTableCell['style'], highlighted: boolean): React.CSSProperties => ({
    background: highlighted ? undefined : s?.background,
    color: s?.color,
    textAlign: s?.align as React.CSSProperties['textAlign'],
    verticalAlign: s?.valign as React.CSSProperties['verticalAlign'],
    fontWeight: s?.bold ? 'bold' : undefined,
    fontStyle: s?.italic ? 'italic' : undefined,
    textDecoration: s?.underline ? 'underline' : undefined,
})

/**
 * Renders a stack frame's table from the raw Tables API grid and overlays the trace highlights
 * (current line, result, matched/unmatched conditions) by A1 cell address — no HTML injection.
 *
 * The raw grid is immutable during a session, so it is cached per table: revisiting a frame is instant
 * and stepping never reloads the structure — only the small highlight overlay is refetched.
 */
const TraceTableView: React.FC<TraceTableViewProps> = ({ frameIndex }) => {
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
    }, [projectId, frameIndex, stackVersion, tableId])

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

    return (
        <Card className={styles.card} size="small" title={t('details.table')}>
            <div className={styles.content}>
                <table className={styles.table} data-testid="trace-table">
                    <tbody>
                        {rows.map((row, r) => (
                            <tr key={r}>
                                {row.map((cell, c) => {
                                    if (cell.covered) return null
                                    const state = cell.cell ? highlights[cell.cell] : undefined
                                    return (
                                        <td
                                            key={c}
                                            className={cx(styles.cell, state && styles[state])}
                                            colSpan={cell.colspan}
                                            data-cell={cell.cell}
                                            rowSpan={cell.rowspan}
                                            style={cellStyle(cell.style, !!state)}
                                        >
                                            {formatValue(cell.value)}
                                        </td>
                                    )
                                })}
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
            {table.totalRows != null && (
                <div className={styles.truncated}>
                    {t('table.truncated', { count: rows.length, total: table.totalRows })}
                </div>
            )}
        </Card>
    )
}

export default TraceTableView
