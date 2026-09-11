import { useTranslation } from 'react-i18next'
import { Button, Dropdown, Space } from 'antd'
import {
    DashboardOutlined,
    DownloadOutlined,
    ExperimentOutlined,
    MoreOutlined,
    PlayCircleOutlined,
    ReloadOutlined,
    RadarChartOutlined,
} from '@ant-design/icons'
import type { ModuleTable } from 'types/tables'
import type { Project } from '../../types/projects'
import { downloadFile } from '../../services/files'

/** What an action of the More menu asks for, once the reader has a table open. */
interface TableAction {
    key: string
    labelKey: string
    icon: React.ReactNode
    /** The event the React panel of EPBDS-16560 listens for. */
    event: string
}

const TABLE_ACTIONS: TableAction[] = [
    { key: 'run', labelKey: 'browser.module.run', icon: <PlayCircleOutlined />, event: 'openRunLaunch' },
    { key: 'tests', labelKey: 'browser.module.run_tests', icon: <ExperimentOutlined />, event: 'openTestsLaunch' },
    { key: 'trace', labelKey: 'browser.module.trace', icon: <RadarChartOutlined />, event: 'openTraceLaunch' },
    {
        key: 'benchmark',
        labelKey: 'browser.module.benchmark',
        icon: <DashboardOutlined />,
        event: 'openBenchmarkLaunch',
    },
]

interface ModuleActionBarProps {
    project: Project
    moduleName: string
    /** The table on screen; the actions that run one are offered only while there is one. */
    table?: ModuleTable | null
    /** The path of the module's workbook, so it can be exported; absent while the project is still being read. */
    modulePath?: string | undefined
    onRefresh: () => void
}

/**
 * What can be done to the open module, in the place the project's own actions sit.
 *
 * Reading a module changes nothing, so only the two harmless actions are offered outright. Running the open table,
 * its tests, a trace or a benchmark are the panels EPBDS-16560 already built: they are asked for by the event each
 * one listens on, and gathered under More, as the old toolbar gathered them.
 */
export const ModuleActionBar = ({ project, moduleName, table, modulePath, onRefresh }: ModuleActionBarProps) => {
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

    return (
        <Space data-testid="module-actions">
            <Button
                data-testid="module-export"
                disabled={!modulePath}
                icon={<DownloadOutlined />}
                onClick={() => modulePath && downloadFile(project.id, modulePath)}
            >
                {t('browser.module.export')}
            </Button>
            <Button data-testid="module-refresh" icon={<ReloadOutlined />} onClick={onRefresh}>
                {t('browser.module.refresh')}
            </Button>
            <Dropdown
                trigger={['click']}
                menu={{
                    items: TABLE_ACTIONS.map(action => ({
                        key: action.key,
                        icon: action.icon,
                        label: t(action.labelKey),
                        disabled: !table,
                    })),
                    onClick: ({ key }) => {
                        const action = TABLE_ACTIONS.find(candidate => candidate.key === key)
                        if (action) {
                            launch(action.event)
                        }
                    },
                }}
            >
                <Button
                    aria-label={t('browser.module.more')}
                    data-testid="module-actions-more"
                    icon={<MoreOutlined />}
                />
            </Dropdown>
        </Space>
    )
}
