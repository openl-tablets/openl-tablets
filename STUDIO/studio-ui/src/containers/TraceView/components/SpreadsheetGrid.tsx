import React from 'react'
import { Empty, Tag, Tooltip } from 'antd'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
import type { StepValueView } from 'types/trace'
import { ParameterTree } from './TraceParameters'
import { useStyles } from './SpreadsheetGrid.styles'

interface SpreadsheetGridProps {
    steps?: StepValueView[] | undefined
    columns?: string[] | null | undefined
    rows?: string[] | null | undefined
    frameUri: string
}

/**
 * Renders a spreadsheet frame's steps as a grid that mirrors the source table: one column per
 * spreadsheet column and one row per spreadsheet row. Each cell shows its computed value and status,
 * the current cell is highlighted, and clicking a not-yet-executed cell toggles a breakpoint on it.
 */
const SpreadsheetGrid: React.FC<SpreadsheetGridProps> = ({ steps, columns, rows, frameUri }) => {
    const { t } = useTranslation('trace')
    const { styles, cx } = useStyles()
    const breakpoints = useTraceStore(s => s.breakpoints)
    const toggleBreakpoint = useTraceStore(s => s.toggleBreakpoint)

    if (!steps || steps.length === 0 || !columns || !rows) {
        return <Empty description={t('debug.noSteps')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
    }

    const byRef = new Map(steps.map(step => [step.ref, step]))

    const renderCell = (step: StepValueView): React.ReactNode => {
        const key = `${frameUri}#${step.ref}`
        const hasBreakpoint = breakpoints.includes(key)
        return (
            <div className={styles.cellInner}>
                {step.status !== 'executed' && (
                    <Tooltip title={hasBreakpoint ? t('debug.removeBreakpoint') : t('debug.addBreakpoint')}>
                        <span
                            className={cx(styles.gutter, hasBreakpoint && styles.gutterActive)}
                            data-testid={`debug-cell-bp-${step.ref}`}
                            onClick={() => toggleBreakpoint(key, step.label || step.ref)}
                        />
                    </Tooltip>
                )}
                {step.value ? (
                    <ParameterTree param={{ ...step.value, name: '' }} paramKey={`cell-${step.ref}`} />
                ) : step.status === 'current' ? (
                    <Tag color="processing">{t('debug.executing')}</Tag>
                ) : (
                    <span className={styles.pending}>{t('debug.pending')}</span>
                )}
            </div>
        )
    }

    return (
        <div className={styles.section} data-testid="debug-spreadsheet-grid">
            <div className={styles.header}>{t('debug.steps')}</div>
            <div className={styles.scroll}>
                <table className={styles.grid}>
                    <thead>
                        <tr>
                            <th className={styles.corner} />
                            {columns.map((col, c) => (
                                <th key={c} className={styles.colHeader}>{col}</th>
                            ))}
                        </tr>
                    </thead>
                    <tbody>
                        {rows.map((row, r) => (
                            <tr key={r}>
                                <th className={styles.rowHeader}>{row}</th>
                                {columns.map((_col, c) => {
                                    const step = byRef.get(`R${r}C${c}`)
                                    return (
                                        <td
                                            key={c}
                                            className={cx(styles.cell, step?.status === 'current' && styles.current)}
                                            data-testid={`debug-cell-R${r}C${c}`}
                                        >
                                            {step && renderCell(step)}
                                        </td>
                                    )
                                })}
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    )
}

export default SpreadsheetGrid
