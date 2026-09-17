import { useEffect, useRef, useState, type MouseEvent as ReactMouseEvent, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { App, Button, Dropdown, Tooltip } from 'antd'
import {
    CaretDownOutlined,
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
import type { ModuleTable, SummaryTable, TableRunState } from 'types/tables'
import { getTableTargets, getTableTests, type TableTarget, type TableTest } from '../../services/modules'
import { deleteTable } from '../../services/tables'
import { moduleRoute } from '../../services/projectId'
import type { ConfirmWrite } from './useOverwriteConfirm'
import { canTargetTable, EXECUTABLE_KINDS } from '../CreateTableModal/testSkeleton'

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
        /* Each name starts where the one above it starts: a button fills the column and centres its own
           label, which leaves a list of names of different lengths wandering from line to line. */
        align-items: flex-start;
        gap: 2px;
        margin-left: ${token.marginLG}px;
        line-height: 1.2;
    `,
    testsTitle: css`
        font-size: ${token.fontSizeSM}px;
        font-weight: 600;
    `,
    /** The one that is named, and the way to the rest, side by side. */
    testsLine: css`
        display: flex;
        align-items: center;
        gap: ${token.marginXXS}px;
    `,
    testLink: css`
        height: auto;
        padding: 0;
        font-size: ${token.fontSizeSM}px;
        text-align: left;
    `,
    testsMore: css`
        width: 18px;
        height: 18px;
        min-width: 18px;
        padding: 0;
        color: ${token.colorTextSecondary};
    `,
}))

/**
 * Tables a copy cannot be made of: a datatype, and the kinds that carry no properties at all.
 *
 * <p>A copy is told from the table it was made of by the properties it is given, so a table that can hold none —
 * the environment, the properties table itself, whatever OpenL could not name — has nothing to be copied into.
 */
const NOT_COPYABLE = new Set(['Datatype', 'Environment', 'Properties', 'Other'])

/** The families written against another table: they are the only ones with a table to name. */
const EXERCISING = new Set(['Test', 'Run'])

/** The families of table that can be executed at all — what the rules can call, and what calls it. */
const EXECUTABLE = new Set([...EXECUTABLE_KINDS, ...EXERCISING])

/**
 * Whether a test can be written against the table.
 *
 * <p>The rules must be able to call it, and it must answer with something: a test asserts a result, and a table
 * returning nothing gives it nothing to compare against. A test table itself is not one — a test of a test
 * exercises nothing.
 */
const canCreateTest = (table: ModuleTable) =>
    EXECUTABLE_KINDS.includes(table.kind) && canTargetTable(table, 'test')

/** What one action of the open table does, and what a table must be for it to be offered at all. */
interface TableAction {
    key: string
    labelKey: string
    icon: ReactNode
    /** The event the panel of EPBDS-16560 listens on; only a table that can be run carries one. */
    event?: string
    /** Offered only for a table that has tests of its own or is covered by some. */
    needsTests?: boolean
    /** Writes to the module, so it is offered only to a reader who may edit the project. */
    writes?: boolean
    /** What the table must be for the action to mean anything; without it every table suits. */
    suits?: (table: ModuleTable) => boolean
}

/** The dialog a writing action opens, by the event the dialog listens on. */
const DIALOGS: Record<string, string> = {
    copy: 'openCopyTableModal',
    createTest: 'openCreateTableModal',
}

const ACTIONS: TableAction[] = [
    { key: 'edit', labelKey: 'browser.module.edit', icon: <EditOutlined />, writes: true },
    {
        key: 'copy',
        labelKey: 'browser.module.copy',
        icon: <CopyOutlined />,
        writes: true,
        suits: table => !NOT_COPYABLE.has(table.kind),
    },
    { key: 'remove', labelKey: 'browser.module.remove', icon: <DeleteOutlined />, writes: true },
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
    {
        key: 'createTest',
        labelKey: 'browser.module.create_test',
        icon: <FileAddOutlined />,
        writes: true,
        suits: canCreateTest,
    },
]

interface TableToolbarProps {
    projectId: string
    moduleName: string
    table: ModuleTable
    /** Whether every module is compiled, since a test covering this table may be written in another one. */
    projectCompiled?: boolean
    /** Whether the reader may edit the project; what writes to the module is offered only then. */
    canWrite?: boolean
    /**
     * What the table is as something to run, as the read of it answered.
     *
     * <p>Absent while that read is on its way, and then nothing is offered to run: a table the compiler could
     * not build — or a test whose rules failed — has nothing to run, and the old Editor took those actions
     * off the band rather than letting them fail.
     */
    runState?: TableRunState | undefined
    /** A table written from here — a copy of this one, or a test for it — and the module it landed in. */
    onWritten?: ((written: SummaryTable, moduleName: string) => void) | undefined
    /** Called once this table is gone from the module. */
    onRemoved?: (() => void) | undefined
    /** Starts editing the table's cells; absent where this screen does no editing. */
    onEdit?: (() => void) | undefined
    /**
     * Runs a write after asking whatever has to be asked first — that the project is open on an older
     * revision, say. Absent where nothing stands in the way of a write.
     */
    confirmWrite?: ConfirmWrite | undefined
}

/**
 * What can be done to the table on screen, in a band of its own directly above it — where the old Editor kept it,
 * and reading the way it read there: the picture of the action with its name beneath, and beside them what
 * exercises the table.
 *
 * Running the table, tracing it or measuring it opens the panel EPBDS-16560 already built, asked for by the event
 * that panel listens on. Copying the table, writing a test for it and removing it are offered only to a reader
 * who may edit the project, and only where they mean something — a datatype is copied by nobody, a table that
 * answers with nothing has no test to write. Editing the cells arrives with the editing phase and stands
 * disabled until then.
 */
export const TableToolbar = ({
    projectId,
    moduleName,
    table,
    projectCompiled = false,
    canWrite = false,
    runState,
    onWritten,
    onRemoved,
    onEdit,
    confirmWrite,
}: TableToolbarProps) => {
    const { modal } = App.useApp()
    const { t } = useTranslation('repository')
    const { styles } = useStyles()
    const navigate = useNavigate()
    const [tests, setTests] = useState<TableTest[]>([])
    // What this table exercises, which only a test or a run table does.
    const [targets, setTargets] = useState<TableTarget[]>([])
    // What a table was answered to exercise, kept so the question is not asked about it a second time — and so
    // the answer comes back with the table when the reader returns to it.
    const named = useRef<{ id: string, targets: TableTarget[] } | null>(null)

    useEffect(() => {
        // Only a table the rules can call is covered by tests; a test table of its own, a datatype or a data
        // table is covered by none, and the server is not asked about them.
        if (!EXECUTABLE_KINDS.includes(table.kind)) {
            setTests([])
            return
        }
        let dropped = false
        // What the table before it was covered by is none of this table's business while the answer is on
        // its way.
        setTests([])
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
    }, [projectId, table.id, table.kind, moduleName, projectCompiled])

    // A test says which table it is written against, so a reader arrives at the rules from the test as easily
    // as they reach the test from the rules. Nothing else exercises anything, and nothing is asked for it.
    useEffect(() => {
        if (!EXERCISING.has(table.kind)) {
            setTargets([])
            return
        }
        // The table it exercises may be written in a module compiled after this one, so the question is asked
        // again when the project is compiled through — but only while it went unanswered. Once the table is
        // named, compiling the rest of the project cannot name it differently.
        const answered = named.current
        if (answered !== null && answered.id === table.id) {
            setTargets(answered.targets)
            return
        }
        let dropped = false
        setTargets([])
        getTableTargets(projectId, table.id, moduleName)
            .then(found => {
                if (!dropped) {
                    setTargets(found)
                    if (found.length > 0) {
                        named.current = { id: table.id, targets: found }
                    }
                }
            })
            .catch(() => {
                if (!dropped) {
                    setTargets([])
                }
            })
        return () => {
            dropped = true
        }
    }, [projectId, table.id, table.kind, moduleName, projectCompiled])

    const launch = (event: string, from: ReactMouseEvent<HTMLElement>) => {
        const { top, left, width, height } = from.currentTarget.getBoundingClientRect()
        window.dispatchEvent(new CustomEvent(event, {
            detail: {
                projectId,
                tableId: table.id,
                moduleName,
                anchor: { top, left, width, height },
                // Where what is built beyond this module has errors, the run stays inside the module.
                moduleOnlyLocked: runState === 'can-run-module',
            },
        }))
    }

    /** Opens a dialog over this table: where it is written, and what to do with what it writes. */
    const openDialog = (event: string) => window.dispatchEvent(new CustomEvent(event, {
        detail: {
            projectId,
            currentModuleName: moduleName,
            sourceTableId: table.id,
            onSuccess: onWritten,
        },
    }))

    // Asked before it is done, as the old Editor asked: the table goes from the sheet it is written on, and
    // only saving the project carries that to the Design repository.
    const remove = () => {
        modal.confirm({
            title: t('browser.module.remove_confirm', { table: table.displayName ?? table.name }),
            content: t('browser.module.remove_confirm_body'),
            okButtonProps: { danger: true },
            okText: t('browser.module.remove'),
            onOk: async () => {
                if (await deleteTable(projectId, table.id, table.name, moduleName)) {
                    onRemoved?.()
                }
            },
        })
    }

    /**
     * What the button does, with whatever has to be asked before a write asked once, here — every one of them
     * reaches the same workbook, and the question belongs to the write rather than to a button's wording.
     */
    const answer = (action: TableAction) => {
        const act = handler(action)
        if (act === undefined || !action.writes || confirmWrite === undefined) {
            return act
        }
        // The event is carried through: an action that opens beside its own button is anchored to it.
        return (from: ReactMouseEvent<HTMLElement>) => confirmWrite(() => act(from))
    }

    /** What the button does before anything is asked about it. */
    const handler = (action: TableAction) => {
        if (action.event) {
            return (from: ReactMouseEvent<HTMLElement>) => launch(action.event ?? '', from)
        }
        if (action.key === 'remove') {
            return remove
        }
        if (action.key === 'edit') {
            return onEdit
        }
        const dialog = DIALOGS[action.key]
        return dialog ? () => openDialog(dialog) : undefined
    }

    // Runnable is what the server says it is; a table whose read has not answered yet is not offered a run.
    const runnable = EXECUTABLE.has(table.kind) && (runState === 'can-run' || runState === 'can-run-module')
    // A test table runs its own cases; any other table runs the tests written against it, when there are some.
    const hasTests = table.kind === 'Test' || tests.length > 0
    const offered = ACTIONS.filter(action => action.writes
        ? canWrite && (action.suits?.(table) ?? true)
        : runnable && (!action.needsTests || hasTests))

    // A test is a table of its own, written where its author put it: another module of this project, or a
    // module of a project this one depends on. It is opened there, not beside the table it exercises.
    const openTable = (found: TableTest | TableTarget) =>
        navigate(moduleRoute(found.projectId ?? projectId, found.module ?? moduleName, found.id))

    /** Whether the table can be opened at all: one in a project the session cannot address cannot. */
    const unreachable = (item: TableTest | TableTarget) =>
        item.module !== undefined && item.projectId === undefined

    /**
     * The tables beside the band: what this one exercises, and what exercises it.
     *
     * <p>The first is named and the rest are behind a caret beside it, as the old toolbar had them — a table
     * exercised by two dozen others would otherwise push the band down the screen.
     *
     * <p>Each is a link to where that table is written. A project the session cannot address has no screen to
     * open it on, and the reader is told where it lives instead.
     */
    const related = (
        testId: string,
        title: string,
        items: Array<TableTest | TableTarget>,
        idPrefix: string,
        label: (item: TableTest & TableTarget) => string
    ) => {
        const [first, ...rest] = items
        if (first === undefined) {
            return false
        }
        return (
            <div className={styles.tests} data-testid={testId}>
                <span className={styles.testsTitle}>{title}</span>
                <div className={styles.testsLine}>
                    <Tooltip
                        title={first.project && first.projectId === undefined
                            ? t('browser.module.test_elsewhere', { project: first.project })
                            : undefined}
                    >
                        <Button
                            className={styles.testLink}
                            data-testid={`${idPrefix}${first.id}`}
                            disabled={unreachable(first)}
                            onClick={() => openTable(first)}
                            type="link"
                        >
                            {label(first as TableTest & TableTarget)}
                        </Button>
                    </Tooltip>
                    {rest.length > 0 && (
                        <Dropdown
                            trigger={['click']}
                            menu={{
                                items: rest.map(item => ({
                                    key: item.id,
                                    label: label(item as TableTest & TableTarget),
                                    disabled: unreachable(item),
                                    onClick: () => openTable(item),
                                })),
                            }}
                        >
                            <Button
                                className={styles.testsMore}
                                data-testid={`${testId}-more`}
                                icon={<CaretDownOutlined />}
                                size="small"
                                title={t('browser.module.related_more', { count: rest.length })}
                                type="text"
                            />
                        </Dropdown>
                    )}
                </div>
            </div>
        )
    }

    /** What a test is called in the list, saying where it lives when that is not the module being read. */
    const testLabel = (test: TableTest) => {
        const name = test.info ? `${test.name} (${test.info})` : test.name
        return test.module && test.module !== moduleName ? `${name} — ${test.module}` : name
    }

    return (
        <div className={styles.bar} data-testid="table-toolbar">
            {offered.map(action => {
                const answers = answer(action)
                return (
                    <Tooltip key={action.key} title={answers ? undefined : t('browser.module.planned')}>
                        <Button
                            className={styles.action}
                            data-testid={`table-${action.key}`}
                            disabled={!answers}
                            icon={action.icon}
                            onClick={from => answers?.(from)}
                            size="small"
                            type="text"
                        >
                            <span className={styles.label}>{t(action.labelKey)}</span>
                        </Button>
                    </Tooltip>
                )
            })}
            {related('table-target-tables',
                t(targets.length > 1 ? 'browser.module.target_tables' : 'browser.module.target_table'),
                targets, 'table-target-', target => target.name)}
            {related('table-available-tests', t('browser.module.available_tests'), tests, 'table-test-', testLabel)}
        </div>
    )
}
