import React, { useCallback, useEffect, useState } from 'react'
import { Alert, Button, Empty, Flex, Pagination, Result, Space, Tag, Typography } from 'antd'
import { CheckOutlined, CloseOutlined, DownloadOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { ListTable, type ListTableColumn } from 'components/ListTable'
import { RunningCard } from 'components/RunningCard'
import { TableLink } from 'components/TableLink'
import {
    type ReadLines,
    useValueStyles,
    ValueCell,
    valueLabel,
    type ValueStyles,
} from 'components/values/ParameterValues'
import { ALL_TESTS_ON_A_PAGE, TESTS_PAGE_SIZE, TESTS_PAGE_SIZES } from 'constants/tests'
import {
    getTestCaseLines,
    getTestsSummary,
    getTestsSummaryWorkbook,
    readTestsSummary,
    XLSX_MEDIA_TYPE,
    type TestCaseValue,
    type TestsQuery,
} from 'services/execution'
import { isStillRunning } from 'services/taskResult'
import { useUserStore } from 'store'
import type { TestsSummary, TestStatus, TestTableResult, TestUnitResult } from 'types/execution'
import type { UserProfile } from 'types/user'
import type { TraceParameterValue } from 'types/trace'
import { saveFile } from 'utils/download'
import { errorMessage } from 'utils/errorMessage'
import { CompoundResultOption, CountSelect, FailuresOption, savedFailuresOption } from './ResultOptions'
import { isFinished, useExecutionProgress, useQuietSpells } from './useExecutionProgress'
import { testsTopics } from './topics'
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
    readLines: ReadCaseLines,
    valueStyles: ValueStyles
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
                // The runtime context comes whole: only a value the case was given is read a level at a time.
                const parameter = index - (unit.contextParameters ?? []).length
                return (
                    <ValueCell
                        label={value ? valueLabel(value) : undefined}
                        lazy={value?.lazy ?? false}
                        path={`${key}-in-${unit.id}-${index}`}
                        readLines={readLines(table.tableId, unit.id, { of: 'parameter', index: parameter })}
                        styles={valueStyles}
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
                            <ValueCell
                                lazy={actual?.actualLazy ?? false}
                                path={`${key}-out-${unit.id}-${index}`}
                                readLines={readLines(table.tableId, unit.id, { of: 'assertion', index })}
                                styles={valueStyles}
                                value={actual?.actualValue}
                            />
                        </Space>
                        {actual && actual.status !== 'TR_OK' && (
                            <Space size={4}>
                                <Text type="secondary">{t('tests.expected')}</Text>
                                <ValueCell
                                    lazy={actual.expectedLazy ?? false}
                                    path={`${key}-exp-${unit.id}-${index}`}
                                    readLines={readLines(table.tableId, unit.id, { of: 'expected', index })}
                                    styles={valueStyles}
                                    value={actual.expectedValue}
                                />
                            </Space>
                        )}
                    </Space>
                )
            },
        })),
        // A Run table states no expected values: what a case returned is its result, shown whether or not the
        // compound result is asked for.
        ...(compoundResult || table.runTable ? [{
            key: 'compound',
            title: table.runTable ? t('tests.result') : t('tests.compoundResult'),
            render: (unit: TestUnitResult) => (
                <Space size={4}>
                    {!table.runTable && <StatusMark status={unit.status} title={t(STATUS[unit.status])} />}
                    {/* A case that returned nothing to show, one that ended with an error, leaves the cell empty. */}
                    {unit.result && (
                        <ValueCell
                            label={valueLabel(unit.result)}
                            lazy={unit.result.lazy}
                            path={`${key}-whole-${unit.id}`}
                            readLines={readLines(table.tableId, unit.id, { of: 'result' })}
                            styles={valueStyles}
                            value={unit.result.value}
                        />
                    )}
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

/** Reads a value of a case of the run that has ended, a level at a time. */
type ReadCaseLines = (tableId: string, caseId: string, value: TestCaseValue) => ReadLines

/** How many cases of one test table are listed at a time. */
const CASES_PER_PAGE = 20

/**
 * The results of the test units of one test table, a case to a row.
 *
 * A table lists its cases a page at a time, and the pager under it reaches the rest. A test table can hold
 * thousands of cases, and drawn at once, their rows are more than the browser can hold.
 */
const TestTable: React.FC<{
    table: TestTableResult
    projectId: string
    compoundResult: boolean
    readLines: ReadCaseLines
    /** The look of the values, read once by the window for every table it lists. */
    valueStyles: ValueStyles
    onOpenTable: () => void
}> = ({ table, projectId, compoundResult, readLines, valueStyles, onOpenTable }) => {
    const { t } = useTranslation('execution')
    const [page, setPage] = useState(1)

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
            {/* A table the screen is set to leave out keeps its name and its counts, and nothing more. The columns
                come from every case listed, so they stay the same from page to page. */}
            {units.length > 0 && (
                <ListTable<TestUnitResult>
                    columns={columnsOf(table, compoundResult, t, readLines, valueStyles)}
                    data-testid={`test-results-${table.tableId}`}
                    rowKey={unit => unit.id}
                    rows={units.slice((page - 1) * CASES_PER_PAGE, page * CASES_PER_PAGE)}
                />
            )}
            {units.length > CASES_PER_PAGE && (
                <Pagination
                    align="end"
                    current={page}
                    data-testid={`test-cases-pagination-${table.tableId}`}
                    onChange={setPage}
                    pageSize={CASES_PER_PAGE}
                    showSizeChanger={false}
                    size="small"
                    total={units.length}
                />
            )}
        </Space>
    )
}

/** What the screen shows to begin with: the settings of the user, as they were saved in the profile. */
const savedQuery = (profile: UserProfile | null): Required<TestsQuery> => ({
    ...savedFailuresOption(profile),
    compoundResult: profile?.showComplexResult ?? false,
    // The screen only reads the values, so a value with inner structure is read when it is asked for.
    lazyValues: true,
    page: 0,
    size: profile?.testsPerPage || TESTS_PAGE_SIZE,
})

interface TestsResultModalProps {
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
    const valueStyles = useValueStyles()
    const profile = useUserStore(state => state.userProfile)
    const [query, setQuery] = useState<Required<TestsQuery>>(() => ({ ...savedQuery(profile ?? null), ...options }))
    const [summary, setSummary] = useState<TestsSummary | null>(null)
    // What the results on screen were read with. The columns follow it rather than what was just asked for, so a
    // column added while the results are read again does not stand empty over the results before.
    const [shownQuery, setShownQuery] = useState(query)
    const readLines = useCallback<ReadCaseLines>(
        (tableId, caseId, value) => (path, offset) => getTestCaseLines(projectId, tableId, caseId, value, path, offset),
        [projectId]
    )
    const [failure, setFailure] = useState<string | null>(null)
    const [error, setError] = useState<string | null>(null)
    const [saving, setSaving] = useState(false)

    const topics = testsTopics(projectId, tableId)
    const progress = useExecutionProgress(topics.status, topics.results)

    const finished = isFinished(progress.status)
    const quietSpells = useQuietSpells(progress, summary === null)

    // The run says when it has ended, and the results are read then. The screen also reads once as soon as it is
    // listening, for a run that ended before the window was there to hear about it, again whenever it is set to
    // show something else, and again when the run has said nothing for a while.
    useEffect(() => {
        if (!progress.subscribed) {
            return undefined
        }
        let active = true
        // Only a run that has said it ended is waited out while it publishes the results. A run still going on
        // answers one read, and the screen waits for the run to say more.
        const read = finished ? readTestsSummary : getTestsSummary
        read(projectId, query)
            .then(loaded => {
                if (active) {
                    setSummary(loaded)
                    setShownQuery(query)
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
    }, [projectId, query, progress.subscribed, finished, quietSpells])

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
                    <FailuresOption
                        failures={query.failures}
                        failuresOnly={query.failuresOnly}
                        onChange={update}
                    />
                    <CompoundResultOption
                        checked={query.compoundResult}
                        onChange={compoundResult => update({ compoundResult })}
                    />
                    <CountSelect
                        allValue={ALL_TESTS_ON_A_PAGE}
                        counts={TESTS_PAGE_SIZES}
                        data-testid="tests-per-page"
                        label={t('tests.perPage')}
                        onChange={size => update({ size })}
                        value={query.size}
                    />
                </Flex>
                {tables.length === 0
                    ? <Empty description={t('tests.none')} />
                    : tables.map(table => (
                        // A table set to list other cases lists them from its first page again.
                        <TestTable
                            key={`${table.tableId} ${shownQuery.failuresOnly} ${shownQuery.failures}`}
                            compoundResult={shownQuery.compoundResult}
                            onOpenTable={onClose}
                            projectId={projectId}
                            readLines={readLines}
                            table={table}
                            valueStyles={valueStyles}
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
