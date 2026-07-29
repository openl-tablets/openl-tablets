import React, { useState } from 'react'
import { Button, Input, Tag, Tooltip } from 'antd'
import { AimOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
import CollapsibleSection from './CollapsibleSection'
import { ParameterTree } from './TraceParameters'
import { useStyles } from './WatchPanel.styles'

/**
 * Watch a factor across the whole run. Add a cell by its {@code $...} name (or ref); Collect runs the
 * trace to completion and shows the cell's value on every execution of its table — so an outlier among
 * otherwise-uniform coverages is obvious at a glance. Clicking a value replays into that table to inspect
 * it live.
 */
const WatchPanel: React.FC = () => {
    const { t } = useTranslation('trace')
    const { styles } = useStyles()
    const watches = useTraceStore(s => s.watches)
    const watch = useTraceStore(s => s.watch)
    const loading = useTraceStore(s => s.loading)
    const setWatchCells = useTraceStore(s => s.setWatchCells)
    const collectWatch = useTraceStore(s => s.collectWatch)
    const replayNode = useTraceStore(s => s.replayNode)

    const [draft, setDraft] = useState('')

    const addWatch = (): void => {
        const cell = draft.trim()
        if (cell && !watches.includes(cell)) {
            void setWatchCells([...watches, cell])
        }
        setDraft('')
    }

    const removeWatch = (cell: string): void => {
        void setWatchCells(watches.filter(w => w !== cell))
    }

    // Collect stays in the header whether the panel is open or collapsed, so a run can be kicked off either way.
    const collect = (
        <Tooltip title={t('watch.collectHint')}>
            {/* Span wrapper so the tooltip still shows on hover while the button is disabled
                (a disabled button has pointer-events: none and receives no hover events). */}
            <span>
                <Button
                    data-testid="watch-collect"
                    disabled={watches.length === 0 || loading}
                    loading={loading}
                    onClick={() => void collectWatch()}
                    size="small"
                    type="primary"
                >
                    {t('watch.collect')}
                </Button>
            </span>
        </Tooltip>
    )

    return (
        <CollapsibleSection
            className={styles.panel}
            extra={collect}
            hint={t('watch.titleHint')}
            panelTestId="watch-panel"
            title={t('watch.title')}
            toggleTestId="watch-toggle"
        >
            <div className={styles.addRow}>
                <Input
                    data-testid="watch-add"
                    onChange={e => setDraft(e.target.value)}
                    onPressEnter={addWatch}
                    placeholder={t('watch.addPlaceholder')}
                    size="small"
                    value={draft}
                />
                <Tooltip title={t('watch.addHint')}>
                    <Button data-testid="watch-add-button" onClick={addWatch} size="small">
                        {t('watch.add')}
                    </Button>
                </Tooltip>
            </div>
            {watches.length > 0 && (
                <div className={styles.chips}>
                    {watches.map(cell => (
                        <Tag key={cell} closable data-testid="watch-chip" onClose={() => removeWatch(cell)}>
                            {cell}
                        </Tag>
                    ))}
                </div>
            )}
            {watch?.truncated && <div className={styles.truncated}>{t('watch.truncated')}</div>}
            {watch && watch.series.length === 0 && watches.length > 0 && (
                <div className={styles.hint}>{t('watch.empty')}</div>
            )}
            <div className={styles.seriesList}>
                {watch?.series.map(series => (
                    <div key={`${series.name} ${series.tableUri}`} className={styles.series} data-testid="watch-series">
                        <div className={styles.seriesTitle}>
                            {series.name} <span className={styles.seriesTable}>· {series.table}</span>
                        </div>
                        {series.points.map(point => (
                            <div key={`${point.instance} ${point.ref}`} className={styles.point} data-testid="watch-point">
                                <div className={styles.pointValue}>
                                    {point.value ? (
                                        <ParameterTree
                                            param={{ ...point.value, name: point.label }}
                                            paramKey={`watch-${series.name}-${point.instance}`}
                                        />
                                    ) : (
                                        <span className={styles.pointLabel}>
                                            {point.label} = {t('watch.noValue')}
                                        </span>
                                    )}
                                </div>
                                <Tooltip title={`${point.path.join(' ▸ ')} — ${t('watch.replayHint')}`}>
                                    <Button
                                        className={styles.replay}
                                        data-testid="watch-replay"
                                        icon={<AimOutlined />}
                                        onClick={() => void replayNode(`${point.ref}@${point.instance}`, series.name)}
                                        size="small"
                                        type="text"
                                    />
                                </Tooltip>
                            </div>
                        ))}
                        {series.total > series.points.length && (
                            <div className={styles.hint} data-testid="watch-more">
                                {t('watch.showing', { shown: series.points.length, total: series.total })}
                            </div>
                        )}
                    </div>
                ))}
            </div>
        </CollapsibleSection>
    )
}

export default WatchPanel
