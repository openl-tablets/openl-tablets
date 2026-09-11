import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Space, Tooltip } from 'antd'
import {
    DashboardOutlined,
    DownloadOutlined,
    ExperimentOutlined,
    PlayCircleOutlined,
    RadarChartOutlined,
} from '@ant-design/icons'
import type { ModuleTable } from 'types/tables'
import type { Project } from '../../types/projects'
import { downloadFile } from '../../services/files'

/** What one action of the open table asks for, and the panel of EPBDS-16560 that answers it. */
interface TableAction {
    key: string
    labelKey: string
    icon: ReactNode
    event: string
    /** Only a test table runs its tests; everything else is offered for any executable table. */
    testsOnly?: boolean
}

const TABLE_ACTIONS: TableAction[] = [
    { key: 'run', labelKey: 'browser.module.run', icon: <PlayCircleOutlined />, event: 'openRunLaunch' },
    {
        key: 'tests',
        labelKey: 'browser.module.run_tests',
        icon: <ExperimentOutlined />,
        event: 'openTestsLaunch',
        testsOnly: true,
    },
    { key: 'trace', labelKey: 'browser.module.trace', icon: <RadarChartOutlined />, event: 'openTraceLaunch' },
    { key: 'benchmark', labelKey: 'browser.module.benchmark', icon: <DashboardOutlined />, event: 'openBenchmarkLaunch' },
]

/** The families of table that can be executed at all; the rest are read, not run. */
const EXECUTABLE = new Set(['Rules', 'Spreadsheet', 'Method', 'Test', 'TBasic', 'Column Match', 'Run'])

interface ModuleActionBarProps {
    project: Project
    moduleName: string
    /** The table on screen. The actions that run one belong to it, and are offered only while it is open. */
    table?: ModuleTable | null
    /** The workbook the module is written in, so it can be exported. */
    modulePath?: string | undefined
}

/**
 * What can be done to the open module and to the table shown in it, in the place the project's own actions sit.
 *
 * Running a table, its tests, a trace or a benchmark belong to the table rather than to the module, so they stand
 * beside it and are offered only for a table that can be executed — as the old toolbar offered them. Reading a
 * module changes nothing, so the only module-wide actions are the two harmless ones.
 */
export const ModuleActionBar = ({ project, moduleName, table, modulePath }: ModuleActionBarProps) => {
    const { t } = useTranslation('repository')

    const launch = (event: string) => {
        if (!table) {
            return
        }
        window.dispatchEvent(new CustomEvent(event, {
            detail: {
                projectId: project.id,
                tableId: table.id,
                moduleName,
                anchor: { top: 0, left: 0, width: 0, height: 0 },
            },
        }))
    }

    const executable = table !== null && table !== undefined && EXECUTABLE.has(table.kind)
    const offered = TABLE_ACTIONS.filter(action => !action.testsOnly || table?.kind === 'Test')

    return (
        <Space data-testid="module-actions">
            {executable && offered.map(action => (
                <Button
                    key={action.key}
                    data-testid={`module-${action.key}`}
                    icon={action.icon}
                    onClick={() => launch(action.event)}
                >
                    {t(action.labelKey)}
                </Button>
            ))}
            <Tooltip title={modulePath ? undefined : t('browser.module.export_unavailable')}>
                <Button
                    data-testid="module-export"
                    disabled={!modulePath}
                    icon={<DownloadOutlined />}
                    onClick={() => modulePath && downloadFile(project.id, modulePath)}
                >
                    {t('browser.module.export')}
                </Button>
            </Tooltip>
        </Space>
    )
}
