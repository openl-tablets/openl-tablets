import { useEffect, useState, type MouseEvent as ReactMouseEvent, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Button, Tooltip } from 'antd'
import {
    CopyOutlined,
    DashboardOutlined,
    DeleteOutlined,
    EditOutlined,
    ExperimentOutlined,
    FileAddOutlined,
    PlayCircleOutlined,
    RadarChartOutlined,
} from '@ant-design/icons'
import { createStyles } from 'antd-style'
import type { ModuleTable } from 'types/tables'
import { getTableTests, type TableTest } from '../../services/modules'
import { moduleRoute } from '../../services/projectId'

const useStyles = createStyles(({ css, token }) => ({
    /** A band over the table, carrying what can be done to the table under it. */
    bar: css`
        display: flex;
        flex: none;
        align-items: flex-start;
        gap: ${token.marginXS}px;
        padding: ${token.paddingXS}px ${token.padding}px;
        border-bottom: 1px solid ${token.colorBorderSecondary};
    `,
    /** Each action reads as the old toolbar read it: the picture, and its name under it. */
    action: css`
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 2px;
        height: auto;
        padding: ${token.paddingXXS}px ${token.paddingXS}px;
        line-height: 1.2;

        .anticon {
            font-size: ${token.fontSizeLG}px;
        }
    `,
    label: css`
        font-size: ${token.fontSizeSM}px;
    `,
    /** What exercises the table, named beside the actions as the old toolbar named it. */
    tests: css`
        display: flex;
        flex-direction: column;
        gap: 2px;
        margin-left: ${token.marginLG}px;
        line-height: 1.2;
    `,
    testsTitle: css`
        font-size: ${token.fontSizeSM}px;
        font-weight: 600;
    `,
    testLink: css`
        height: auto;
        padding: 0;
        font-size: ${token.fontSizeSM}px;
    `,
}))

/** What one action of the open table asks for, and the panel of EPBDS-16560 that answers it. */
interface TableAction {
    key: string
    labelKey: string
    icon: ReactNode
    /** The event the React panel listens on; absent for an action that arrives with the editing phase. */
    event?: string
    /** Offered only for a table that has tests of its own or is covered by some. */
    needsTests?: boolean
    /** Offered whatever the table is, not only for one that can be run. */
    always?: boolean
}

const ACTIONS: TableAction[] = [
    { key: 'edit', labelKey: 'browser.module.edit', icon: <EditOutlined />, always: true },
    { key: 'copy', labelKey: 'browser.module.copy', icon: <CopyOutlined />, always: true },
    { key: 'remove', labelKey: 'browser.module.remove', icon: <DeleteOutlined />, always: true },
    { key: 'run', labelKey: 'browser.module.run', icon: <PlayCircleOutlined />, event: 'openRunLaunch' },
    { key: 'trace', labelKey: 'browser.module.trace', icon: <RadarChartOutlined />, event: 'openTraceLaunch' },
    {
        key: 'benchmark',
        labelKey: 'browser.module.benchmark',
        icon: <DashboardOutlined />,
        event: 'openBenchmarkLaunch',
    },
    // Where the old Editor kept it: beside Create Test, and only for a table there is something to run.
    {
        key: 'tests',
        labelKey: 'browser.module.test',
        icon: <ExperimentOutlined />,
        event: 'openTestsLaunch',
        needsTests: true,
    },
    { key: 'createTest', labelKey: 'browser.module.create_test', icon: <FileAddOutlined />, always: true },
]

/** The families of table that can be executed at all; the rest are read, not run. */
const EXECUTABLE = new Set(['Rules', 'Spreadsheet', 'Method', 'Test', 'TBasic', 'Column Match', 'Run'])

interface TableToolbarProps {
    projectId: string
    moduleName: string
    table: ModuleTable
    /** Whether every module is compiled, since a test covering this table may be written in another one. */
    projectCompiled?: boolean
}

/**
 * What can be done to the table on screen, in a band of its own directly above it — where the old Editor kept it,
 * and reading the way it read there: the picture of the action with its name beneath, and beside them what
 * exercises the table.
 *
 * Running the table, tracing it or measuring it opens the panel EPBDS-16560 already built, asked for by the event
 * that panel listens on. Editing it, copying it, removing it and writing a test for it arrive with the editing
 * phase and stand disabled until then, so the band is already the shape it will keep.
 */
export const TableToolbar = ({ projectId, moduleName, table, projectCompiled = false }: TableToolbarProps) => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()
    const navigate = useNavigate()
    const [tests, setTests] = useState<TableTest[]>([])

    useEffect(() => {
        let dropped = false
        getTableTests(projectId, table.id, moduleName)
            .then(found => {
                if (!dropped) {
                    setTests(found)
                }
            })
            .catch(() => {
                if (!dropped) {
                    setTests([])
                }
            })
        return () => {
            dropped = true
        }
        // A test is a table of its own and may be written in another module, so what covers this one is known
        // once the project is compiled through. The list is read again then, rather than staying as it was.
    }, [projectId, table.id, moduleName, projectCompiled])

    const launch = (event: string, from: ReactMouseEvent<HTMLElement>) => {
        const { top, left, width, height } = from.currentTarget.getBoundingClientRect()
        window.dispatchEvent(new CustomEvent(event, {
            detail: { projectId, tableId: table.id, moduleName, anchor: { top, left, width, height } },
        }))
    }

    const executable = EXECUTABLE.has(table.kind)
    // A test table runs its own cases; any other table runs the tests written against it, when there are some.
    const hasTests = table.kind === 'Test' || tests.length > 0
    const offered = ACTIONS.filter(action =>
        action.always || (executable && (!action.needsTests || hasTests)))

    // A test is a table of its own, written where its author put it: another module of this project, or a
    // module of a project this one depends on. It is opened there, not beside the table it exercises.
    const openTest = (test: TableTest) =>
        navigate(moduleRoute(test.projectId ?? projectId, test.module ?? moduleName, test.id))

    /** What a test is called in the list, saying where it lives when that is not the module being read. */
    const testLabel = (test: TableTest) => {
        const name = test.info ? `${test.name} (${test.info})` : test.name
        return test.module && test.module !== moduleName ? `${name} — ${test.module}` : name
    }

    return (
        <div className={styles.bar} data-testid="table-toolbar">
            {offered.map(action => (
                <Tooltip key={action.key} title={action.event ? undefined : t('browser.module.planned')}>
                    <Button
                        className={styles.action}
                        data-testid={`table-${action.key}`}
                        disabled={!action.event}
                        icon={action.icon}
                        onClick={from => action.event && launch(action.event, from)}
                        size="small"
                        type="text"
                    >
                        <span className={styles.label}>{t(action.labelKey)}</span>
                    </Button>
                </Tooltip>
            ))}
            {tests.length > 0 && (
                <div className={styles.tests} data-testid="table-available-tests">
                    <span className={styles.testsTitle}>{t('browser.module.available_tests')}</span>
                    {tests.map(test => (
                        <Tooltip
                            key={test.id}
                            title={test.project && test.projectId === undefined
                                ? t('browser.module.test_elsewhere', { project: test.project })
                                : undefined}
                        >
                            <Button
                                className={styles.testLink}
                                data-testid={`table-test-${test.id}`}
                                // A project the session cannot address has no screen to open the test on.
                                disabled={test.module !== undefined && test.projectId === undefined}
                                onClick={() => openTest(test)}
                                type="link"
                            >
                                {testLabel(test)}
                            </Button>
                        </Tooltip>
                    ))}
                </div>
            )}
        </div>
    )
}
