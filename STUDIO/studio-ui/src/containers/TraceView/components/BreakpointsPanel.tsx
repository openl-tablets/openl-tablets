import React, { useCallback, useState } from 'react'
import { Button, Select, Tooltip } from 'antd'
import { CloseOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
import traceService from 'services/traceService'
import type { BreakpointTableView } from 'types/trace'
import { useStyles } from './BreakpointsPanel.styles'

/**
 * Set and manage breakpoints by table name, without that table being on screen. Picking a table arms a
 * breakpoint on every table entered with that name; Resume then stops there — so a user who knows the
 * name of a deeply nested table can target it up front, before stepping into it. The list shows the
 * armed breakpoints and removes them. Kept beside the main controls so it works in the tree view too.
 */
const BreakpointsPanel: React.FC = () => {
    const { t } = useTranslation('trace')
    const { styles } = useStyles()
    const breakpoints = useTraceStore(s => s.breakpoints)
    const breakpointLabels = useTraceStore(s => s.breakpointLabels)
    const toggleBreakpoint = useTraceStore(s => s.toggleBreakpoint)
    const projectId = useTraceStore(s => s.projectId)

    // Candidate tables for "set a breakpoint by name", loaded lazily when the search opens.
    const [tables, setTables] = useState<BreakpointTableView[]>([])
    const [tablesLoaded, setTablesLoaded] = useState(false)
    const [selectKey, setSelectKey] = useState(0)

    const loadTables = useCallback(async (open: boolean): Promise<void> => {
        if (!open || tablesLoaded || !projectId) return
        setTablesLoaded(true)
        try {
            setTables(await traceService.getBreakpointTables(projectId))
        } catch {
            // best-effort: leave the candidate list empty
        }
    }, [projectId, tablesLoaded])

    const addTableBreakpoint = (name: string): void => {
        // The name is the breakpoint key: execution stops on any table entered with this name.
        if (name && !breakpoints.includes(name)) {
            void toggleBreakpoint(name, name)
        }
        setSelectKey(k => k + 1) // reset the search back to its placeholder
    }

    return (
        <div className={styles.panel} data-testid="breakpoints-panel">
            <div className={styles.header}>{t('debug.breakpoints')}</div>
            <Select
                key={selectKey}
                showSearch
                className={styles.addBreakpoint}
                data-testid="debug-add-breakpoint"
                onChange={addTableBreakpoint}
                onOpenChange={loadTables}
                options={tables.map(table => ({ label: table.name, value: table.name }))}
                placeholder={t('debug.addBreakpointPlaceholder')}
                size="small"
                virtual={false}
                filterOption={(input, option) =>
                    String(option?.label ?? '').toLowerCase().includes(input.toLowerCase())}
            />
            {breakpoints.length === 0 ? (
                <div className={styles.hint}>{t('debug.noBreakpoints')}</div>
            ) : (
                <div className={styles.list}>
                    {breakpoints.map(uri => (
                        <div key={uri} className={styles.breakpoint} data-testid="debug-breakpoint-item">
                            <span className={styles.breakpointDot} />
                            <span className={styles.name}>{breakpointLabels[uri] || uri}</span>
                            <Tooltip title={t('debug.removeBreakpoint')}>
                                <Button
                                    icon={<CloseOutlined />}
                                    onClick={() => toggleBreakpoint(uri)}
                                    size="small"
                                    type="text"
                                />
                            </Tooltip>
                        </div>
                    ))}
                </div>
            )}
        </div>
    )
}

export default BreakpointsPanel
