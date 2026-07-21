import React from 'react'
import { Button, Empty, Tooltip } from 'antd'
import { RedoOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
import { formatMs } from 'utils/formatDuration'
import { useStyles } from './HotspotsPanel.styles'

/**
 * Hot spots of a finished profiling run: the tables that cost the most own time, aggregated across every
 * invocation and ranked like a Java profiler's hot-spot view. Each row shows how many times the table ran,
 * its own time (with a heat bar) and its inclusive time; the replay button restarts and runs to it.
 *
 * Only meaningful in profiling mode, once the run has finished and the executed tree is available.
 */
const HotspotsPanel: React.FC = () => {
    const { t } = useTranslation('trace')
    const { styles } = useStyles()
    const profile = useTraceStore(s => s.profile)
    const replayNode = useTraceStore(s => s.replayNode)

    if (!profile || profile.hotspots.length === 0) {
        return <Empty description={t('hotspots.empty')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
    }

    // The slowest table by own time sets the heat-bar scale; the list already arrives sorted by self time.
    const maxSelf = profile.hotspots.reduce((max, hotspot) => Math.max(max, hotspot.selfMillis), 0)

    return (
        <div className={styles.panel} data-testid="hotspots-panel">
            <div className={styles.header}>
                <span>{t('hotspots.title')}</span>
                <span className={styles.summary}>
                    {t('hotspots.summary', {
                        tables: profile.distinctTables,
                        invocations: profile.nodeCount,
                        total: formatMs(profile.totalMillis),
                    })}
                </span>
            </div>
            <div className={styles.columns}>
                <span className={styles.bar} style={{ background: 'transparent' }} />
                <span className={styles.name}>{t('hotspots.colTable')}</span>
                <span className={styles.count}>{t('hotspots.colRuns')}</span>
                <span className={styles.self}>{t('hotspots.colSelf')}</span>
                <span className={styles.total}>{t('hotspots.colTotal')}</span>
                <span className={styles.replay} />
            </div>
            {profile.hotspots.map(hotspot => (
                <div key={hotspot.uri} className={styles.row} data-testid="hotspot-row">
                    <span className={styles.bar}>
                        <span
                            className={styles.fill}
                            style={{ width: `${maxSelf > 0 ? Math.max(4, (hotspot.selfMillis / maxSelf) * 100) : 0}%` }}
                        />
                    </span>
                    <span className={styles.name} title={hotspot.uri}>{hotspot.name}</span>
                    <span className={styles.count}>×{hotspot.count}</span>
                    <span className={styles.self}>{formatMs(hotspot.selfMillis)}</span>
                    <span className={styles.total}>{formatMs(hotspot.totalMillis)}</span>
                    <Tooltip title={t('hotspots.replayHint')}>
                        <Button
                            className={styles.replay}
                            data-testid="hotspot-replay"
                            icon={<RedoOutlined />}
                            onClick={() => void replayNode(hotspot.uri, hotspot.name)}
                            size="small"
                            type="text"
                        />
                    </Tooltip>
                </div>
            ))}
            {profile.distinctTables > profile.hotspots.length && (
                <div className={styles.more}>
                    {t('hotspots.more', { shown: profile.hotspots.length, total: profile.distinctTables })}
                </div>
            )}
        </div>
    )
}

export default HotspotsPanel
