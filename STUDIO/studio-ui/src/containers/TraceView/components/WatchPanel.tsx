import React, { useState } from 'react'
import { Button, Input, Tag, Tooltip } from 'antd'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
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

    return (
        <div className={styles.panel} data-testid="watch-panel">
            <div className={styles.header}>
                <span>{t('watch.title')}</span>
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
            </div>

            <div className={styles.addRow}>
                <Input
                    data-testid="watch-add"
                    onChange={e => setDraft(e.target.value)}
                    onPressEnter={addWatch}
                    placeholder={t('watch.addPlaceholder')}
                    size="small"
                    value={draft}
                />
                <Button data-testid="watch-add-button" onClick={addWatch} size="small">
                    {t('watch.add')}
                </Button>
            </div>

            {watches.length > 0 && (
                <div className={styles.chips}>
                    {watches.map(cell => (
                        <Tag closable data-testid="watch-chip" key={cell} onClose={() => removeWatch(cell)}>
                            {cell}
                        </Tag>
                    ))}
                </div>
            )}

            {watch?.truncated && <div className={styles.truncated}>{t('watch.truncated')}</div>}

            {watch && watch.series.length === 0 && watches.length > 0 && (
                <div className={styles.hint}>{t('watch.empty')}</div>
            )}

            {watch?.series.map(series => (
                <div className={styles.series} data-testid="watch-series" key={`${series.name} ${series.tableUri}`}>
                    <div className={styles.seriesTitle}>
                        {series.name} <span className={styles.seriesTable}>· {series.table}</span>
                    </div>
                    {series.points.map(point => (
                        <Tooltip key={`${point.instance} ${point.ref}`} title={point.path.join(' ▸ ')}>
                            <div
                                className={styles.point}
                                data-testid="watch-point"
                                onClick={() => void replayNode(series.tableUri, series.name)}
                            >
                                <span className={styles.pointLabel}>{point.label}</span>
                                <span className={styles.pointValue}>
                                    {point.value === null ? t('watch.noValue') : String(point.value)}
                                </span>
                            </div>
                        </Tooltip>
                    ))}
                </div>
            ))}
        </div>
    )
}

export default WatchPanel
