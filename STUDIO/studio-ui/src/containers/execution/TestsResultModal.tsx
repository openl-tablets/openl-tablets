import React, { useCallback, useEffect, useState } from 'react'
import { Alert, Button, Checkbox, Empty, Flex, Pagination, Result, Select, Space, Tag, Typography } from 'antd'
import { CheckOutlined, CloseOutlined, DownloadOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { ListTable, type ListTableColumn } from 'components/ListTable'
import { RunningCard } from 'components/RunningCard'
import { TableLink } from 'components/TableLink'
import { ValueCell } from 'components/values/ParameterValues'
import { useTestCase } from 'hooks/useTestCase'
import {
    ALL_TESTS_ON_A_PAGE,
    FAILURES_PER_TEST,
    FAILURES_PER_TEST_OPTIONS,
    getTestCaseResult,
    getTestsSummaryWorkbook,
    isStillRunning,
    readTestsSummary,
    TESTS_PAGE_SIZE,
    TESTS_PAGE_SIZES,
    XLSX_MEDIA_TYPE,
    type TestsQuery,
} from 'services/execution'
import { useUserStore } from 'store'
import type { TestsSummary, TestStatus, TestTableResult, TestUnitResult } from 'types/execution'
import type { UserProfile } from 'types/user'
import type { TraceParameterValue } from 'types/trace'
import { saveFile } from 'utils/download'
import { errorMessage } from 'utils/errorMessage'
import { isFinished, useExecutionProgress } from './useExecutionProgress'
import { ExecutionErrors, ExecutionModal, nameOf } from './ExecutionModal'

const { Text, Title } = Typography

/** What the results say a case, or one comparison of it, ended as. */
const STATUS: Record<TestStatus, string> = {
    TR_OK: 'tests.passed',
    TR_NEQ: 'tests.failed',
    TR_EXCEPTION: 'tests.error',
}

/** The mark a comparison carries: a tick when the values matched, a cross when they did not. */
const StatusMark: React.FC<{ status: TestStatus, title: string }> = ({ status, title }) => (status === 'TR_OK'
    ? <Text title={title} type="success"><CheckOutlined /></Text>
    : <Text title={title} type="danger"><CloseOutlined /></Text>)

/** How many comparisons of a case did not match. */
const failureCount = (unit: TestUnitResult): number =>
    (unit.testAssertions ?? []).filter(assertion => assertion.status !== 'TR_OK').length

/** The values of a test unit under the names of the columns they came from, context first. */
const inputsOf = (unit: TestUnitResult): TraceParameterValue[] =>
    [...(unit.contextParameters ?? []), ...(unit.parameters ?? [])]

/**
 * The columns of the results of one test table.
 *
 * A test table gives every case the same columns, so they are read from the first case that ran: what it was
 * given, and what the rule is expected to return. A case that failed also shows the value that was expected,
 * under the value that came out.
 *
 * A case that ended with an error compares nothing, so the compared values are read from the first case that
 * did compare something. Without that, a table whose first case threw would show no results at all.
 */
const columnsOf = (
    table: TestTableResult,
    compoundResult: boolean,
    t: (key: string) => string,
    readCase: ReadCase
): ListTableColumn<TestUnitResult>[] => {
    const units = table.testUnits ?? []
    const first = units[0]
    const inputs = first ? inputsOf(first) : []
    const assertions = units.find(unit => (unit.testAssertions ?? []).length > 0)?.testAssertions ?? []
    const key = `${table.tableId}`
    return [
        {
            key: 'id',
            fit: true,
            title: t('tests.id'),
            render: unit => (
                <Space size={4}>
                    <Text strong>{unit.id}</Text>
                    {!table.runTable && <StatusMark status={unit.status} title={t(STATUS[unit.status])} />}
                    {!table.runTable && failureCount(unit) > 0 && (
                        <Text strong type="danger">({failureCount(unit)})</Text>
                    )}
                </Space>
            ),
        },
        ...(units.some(unit => unit.description) ? [{
            key: 'description',
            title: t('tests.description'),
            render: (unit: TestUnitResult) => <Text type="secondary">{unit.description}</Text>,
        }] : []),
        ...inputs.map((input, index): ListTableColumn<TestUnitResult> => ({
            key: `input-${index}`,
            title: nameOf(input),
            render: unit => {
                const value = inputsOf(unit)[index]
                return (
                    <ValueCell
                        lazy={value?.lazy ?? false}
                        onLoad={() => readCase(table.tableId, unit.id).then(read => inputsOf(read)[index])}
                        path={`${key}-in-${unit.id}-${index}`}
                        value={value?.value}
                    />
                )
            },
        })),
        ...assertions.map((assertion, index): ListTableColumn<TestUnitResult> => ({
            key: `assertion-${index}`,
            title: assertion.description || t('tests.result'),
            render: unit => {
                const actual = unit.testAssertions?.[index]
                return (
                    <Space orientation="vertical" size={2}>
                        <Space size={4}>
                            {!table.runTable && actual && (
                                <StatusMark status={actual.status} title={t(STATUS[actual.status])} />
                            )}
                            <ValueCell path={`${key}-out-${unit.id}-${index}`} value={actual?.actualValue} />
                        </Space>
                        {actual && actual.status !== 'TR_OK' && (
                            <Space size={4}>
                                <Text type="secondary">{t('tests.expected')}</Text>
                                <ValueCell path={`${key}-exp-${unit.id}-${index}`} value={actual.expectedValue} />
                            </Space>
                        )}
                    </Space>
                )
            },
        })),
        ...(compoundResult ? [{
            key: 'compound',
            title: t('tests.compoundResult'),
            render: (unit: TestUnitResult) => (
                <Space size={4}>
                    {!table.runTable && <StatusMark status={unit.status} title={t(STATUS[unit.status])} />}
                    <ValueCell
                        lazy={unit.result?.lazy ?? false}
                        onLoad={() => readCase(table.tableId, unit.id).then(read => read.result)}
                        path={`${key}-whole-${unit.id}`}
                        value={unit.result?.value}
                    />
                </Space>
            ),
        }] : []),
        ...(units.some(unit => (unit.errors ?? []).length > 0) ? [{
            key: 'errors',
            title: t('tests.error'),
            render: (unit: TestUnitResult) => <ExecutionErrors errors={unit.errors} />,
        }] : []),
    ]
}

/** Reads one case of the run that has ended, with every value it holds. */
type ReadCase = (tableId: string, caseId: string) => Promise<TestUnitResult>

/** The results of the test units of one test table, a case to a row. */
const TestTable: React.FC<{
    table: TestTableResult
    projectId: string
    compoundResult: boolean
    readCase: ReadCase
    onOpenTable: () => void
}> = ({ table, projectId, compoundResult, readCase, onOpenTable }) => {
    const { t } = useTranslation('execution')

    // A test table holds test cases; a Run table holds runs, because it states no expected values.
    const kind = table.runTable ? 'runs' : 'cases'
    const cases = table.numberOfTests === 0
        ? t(`tests.no_${kind}`)
        : t(`tests.${kind}`, { count: table.numberOfTests })
    const units = table.testUnits ?? []
    return (
        <Space orientation="vertical" size="small" style={{ width: '100%' }}>
            <Flex wrap align="baseline" gap="small">
                {/* The name carries the outcome of the whole table, green when every case passed. A Run table
                    states no expected values, so nothing there can pass or fail. */}
                <Title level={5} style={{ margin: 0 }}>
                    <TableLink
                        data-testid={`test-table-${table.tableId}`}
                        module={table.module}
                        onOpen={onOpenTable}
                        projectId={projectId}
                        tableId={table.tableId}
                        {...(!table.runTable && { type: table.numberOfFailures > 0 ? 'danger' : 'success' })}
                    >
                        {table.name}
                    </TableLink>
                </Title>
                <Tag>{cases}</Tag>
                {table.numberOfFailures > 0 && <Tag color="error">{table.numberOfFailures}</Tag>}
                {table.description && <Text type="secondary">{table.description}</Text>}
            </Flex>
            {/* A table the screen is set to leave out keeps its name and its counts, and nothing more. */}
            {units.length > 0 && (
                <ListTable<TestUnitResult>
                    columns={columnsOf(table, compoundResult, t, readCase)}
                    data-testid={`test-results-${table.tableId}`}
                    rowKey={unit => unit.id}
                    rows={units}
                />
            )}
        </Space>
    )
}

/**
 * What the screen shows to begin with: the settings of the user, as they were saved in the profile.
 *
 * A count the API does not take - the whole page of failures the old pages offered - falls back to what the
 * screen shows by default.
 */
const savedQuery = (profile: UserProfile | null): Required<TestsQuery> => ({
    failuresOnly: profile?.testsFailuresOnly ?? false,
    failures: (profile?.testsFailuresPerTest ?? 0) > 0 ? profile!.testsFailuresPerTest : FAILURES_PER_TEST,
    compoundResult: profile?.showComplexResult ?? false,
    // The screen only reads the values, so a value with inner structure is read when it is asked for.
    lazyValues: true,
    page: 0,
    size: profile?.testsPerPage || TESTS_PAGE_SIZE,
})

export interface TestsResultModalProps {
    projectId: string
    /** The table whose tests ran, when a single table was asked for. */
    tableId?: string | undefined
    /** What the screen shows to begin with, when the panel that started the run has already asked. */
    options?: Partial<TestsQuery> | undefined
    onClose: () => void
}

/**
 * The results of a test run: every test table that ran, with the cases it holds.
 *
 * The window opens while the tests are still running and shows them as soon as they are there. Each case is a
 * row of what it was given, what the rule returned and, where they differ, what was expected. The screen
 * decides what it shows - only the failures, how many of them, the whole returned value, how many tables to a
 * page - and the whole run can be saved as a workbook.
 */
export const TestsResultModal: React.FC<TestsResultModalProps> = ({ projectId, tableId, options, onClose }) => {
    const { t } = useTranslation('execution')
    const profile = useUserStore(state => state.userProfile)
    const [query, setQuery] = useState<Required<TestsQuery>>(() => ({ ...savedQuery(profile ?? null), ...options }))
    const readResult = useCallback(
        (tableId: string, caseId: string) => getTestCaseResult(projectId, tableId, caseId),
        [projectId]
    )
    const readCase = useTestCase(readResult)
    const [summary, setSummary] = useState<TestsSummary | null>(null)
    const [failure, setFailure] = useState<string | null>(null)
    const [error, setError] = useState<string | null>(null)
    const [saving, setSaving] = useState(false)

    const project = encodeURIComponent(projectId)
    const scope = tableId ? `${project}/tables/${encodeURIComponent(tableId)}` : project
    const progress = useExecutionProgress(`/user/topic/projects/${scope}/tests/status`,
        `/user/topic/projects/${scope}/tests/results`)

    const finished = isFinished(progress.status)

    // The run says when it has ended. The screen also reads once as soon as it is listening, for a run that
    // ended before the window was there to hear about it, and again whenever it is set to show something else.
    useEffect(() => {
        if (!progress.subscribed) {
            return undefined
        }
        let active = true
        readTestsSummary(projectId, query)
            .then(loaded => {
                if (active) {
                    setSummary(loaded)
                    setFailure(null)
                }
            })
            .catch(readError => {
                if (active && !isStillRunning(readError)) {
                    setFailure(errorMessage(readError))
                }
            })
        return () => {
            active = false
        }
    }, [projectId, query, progress.subscribed, finished])

    const save = () => {
        setSaving(true)
        getTestsSummaryWorkbook(projectId, query)
            .then(workbook => saveFile(workbook, 'test-results.xlsx', XLSX_MEDIA_TYPE))
            .catch(saveError => setError(errorMessage(saveError)))
            .finally(() => setSaving(false))
    }

    const update = (change: Partial<Required<TestsQuery>>) => setQuery(current => ({ ...current, page: 0, ...change }))

    if (summary === null) {
        const testsFailed = failure !== null || (finished && progress.status !== 'COMPLETED')
        return (
            <ExecutionModal onClose={onClose} title={t('tests.title')}>
                {testsFailed
                    ? <Result status="error" subTitle={progress.error ?? failure ?? undefined} title={t('tests.startFailed')} />
                    : (
                        <RunningCard
                            data-testid="tests-running"
                            description={progress.arrived > 0
                                ? t('tests.runningDone', { count: progress.arrived })
                                : t('tests.running')}
                        />
                    )}
            </ExecutionModal>
        )
    }

    const tables = summary.testCases ?? []
    return (
        <ExecutionModal
            onClose={onClose}
            title={t('tests.title')}
            extra={(
                <Button data-testid="tests-save" icon={<DownloadOutlined />} loading={saving} onClick={save}>
                    {t('tests.saveToExcel')}
                </Button>
            )}
            subtitle={t(summary.numberOfFailures > 0 ? 'tests.summaryFailed' : 'tests.summary', {
                tests: summary.numberOfTests,
                failures: summary.numberOfFailures,
                ms: Math.round(summary.executionTimeMs),
            })}
        >
            <Space orientation="vertical" size="middle" style={{ width: '100%' }}>
                {error && <Alert showIcon closable={{ onClose: () => setError(null) }} title={error} type="error" />}
                {/* The results on screen are the ones that were read last: say so when a new read failed. */}
                {failure && <Alert showIcon data-testid="tests-read-failed" title={failure} type="error" />}
                <Flex wrap align="center" gap="middle">
                    <Checkbox
                        checked={query.failuresOnly}
                        data-testid="tests-failures-only"
                        onChange={event => update({ failuresOnly: event.target.checked })}
                    >
                        {t('tests.failuresOnly')}
                    </Checkbox>
                    <Space size={4}>
                        <Text type="secondary">{t('tests.failuresPerTest')}</Text>
                        <Select<number>
                            data-testid="tests-failures"
                            disabled={!query.failuresOnly}
                            onChange={(failures: number) => update({ failures })}
                            options={FAILURES_PER_TEST_OPTIONS.map(count => ({ value: count, label: String(count) }))}
                            size="small"
                            style={{ width: 80 }}
                            value={query.failures}
                        />
                    </Space>
                    <Checkbox
                        checked={query.compoundResult}
                        data-testid="tests-compound-result"
                        onChange={event => update({ compoundResult: event.target.checked })}
                    >
                        {t('tests.compoundResult')}
                    </Checkbox>
                    <Space size={4}>
                        <Text type="secondary">{t('tests.perPage')}</Text>
                        <Select<number>
                            data-testid="tests-per-page"
                            onChange={(size: number) => update({ size })}
                            size="small"
                            style={{ width: 80 }}
                            value={query.size}
                            options={TESTS_PAGE_SIZES.map(size => ({
                                value: size,
                                label: size === ALL_TESTS_ON_A_PAGE ? t('tests.allOnAPage') : String(size),
                            }))}
                        />
                    </Space>
                </Flex>
                {tables.length === 0
                    ? <Empty description={t('tests.none')} />
                    : tables.map(table => (
                        <TestTable
                            key={table.tableId}
                            compoundResult={query.compoundResult}
                            onOpenTable={onClose}
                            projectId={projectId}
                            readCase={readCase}
                            table={table}
                        />
                    ))}
                {query.size !== ALL_TESTS_ON_A_PAGE && summary.total > query.size && (
                    <Pagination
                        current={query.page + 1}
                        data-testid="tests-pagination"
                        onChange={page => setQuery(current => ({ ...current, page: page - 1 }))}
                        pageSize={query.size}
                        showSizeChanger={false}
                        showTotal={count => t('tests.total', { count })}
                        size="small"
                        total={summary.total}
                    />
                )}
            </Space>
        </ExecutionModal>
    )
}

export default TestsResultModal
