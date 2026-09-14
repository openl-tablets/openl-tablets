import React, { useEffect, useMemo, useState } from 'react'
import { Alert, Button, Checkbox, Empty, Result, Space, Tag, Tooltip, Typography } from 'antd'
import { useTranslation } from 'react-i18next'
import { ListTable, type ListTableColumn } from 'components/ListTable'
import { RunningCard } from 'components/RunningCard'
import { TableLink } from 'components/TableLink'
import { ParameterValueList } from 'components/values/ParameterValues'
import { deleteBenchmarks, getBenchmarks, isStillRunning, readBenchmarks } from 'services/execution'
import type { BenchmarkResult } from 'types/execution'
import { errorMessage } from 'utils/errorMessage'
import { formatRate, formatRatio, formatTime, metricsOf } from './benchmarkMetrics'
import { isFinished, useExecutionProgress } from './useExecutionProgress'
import { benchmarkStatusTopic } from './topics'
import { ExecutionModal } from './ExecutionModal'

const { Text, Title } = Typography

/** The colour the place in a comparison is marked with, the fastest one first. */
const RANK_COLOR = ['success', 'processing', 'warning']

/** Where one of the compared measurements stands: its place, and how much slower it is than the fastest. */
interface Ranking {
    rank: number
    ratio: number
}

/** A column of measured numbers, under a caption that says what the numbers mean. */
const metricColumn = (
    key: string,
    title: string,
    hint: string,
    render: (measurement: BenchmarkResult) => React.ReactNode
): ListTableColumn<BenchmarkResult> => ({
    key,
    align: 'right',
    fit: true,
    title: <Tooltip title={hint}>{title}</Tooltip>,
    render,
})

export interface BenchmarkResultModalProps {
    projectId: string
    /** The table the benchmark that is still running was started on. */
    tableId: string
    onClose: () => void
}

/**
 * The benchmarks of the session: how fast every measured table ran.
 *
 * The window opens while the measurement is still going on and waits for it. A measurement that ends joins the
 * rows above the ones taken before it, so that measurements of different tables and test cases stand side by
 * side. The rows that are ticked can be compared with each other, which places them by speed, or deleted.
 */
export const BenchmarkResultModal: React.FC<BenchmarkResultModalProps> = ({ projectId, tableId, onClose }) => {
    const { t } = useTranslation('execution')
    const [measurements, setMeasurements] = useState<BenchmarkResult[] | null>(null)
    const [picked, setPicked] = useState<string[]>([])
    const [compared, setCompared] = useState<string[] | null>(null)
    const [failure, setFailure] = useState<string | null>(null)
    const [error, setError] = useState<string | null>(null)
    const [deleting, setDeleting] = useState(false)
    const progress = useExecutionProgress(benchmarkStatusTopic(projectId, tableId))

    const finished = isFinished(progress.status)

    // A benchmark that failed or was abandoned measured nothing; the rows of the ones before it are not its
    // result, so the window says what went wrong instead of showing them.
    const benchmarkFailed = failure !== null || (finished && progress.status !== 'COMPLETED')

    // The benchmark says when it has ended. The screen also reads once as soon as it is listening, for a
    // measurement that ended before the window was there to hear about it. That read does not wait for a
    // measurement that is still going on, which takes seconds: the status says when to read again.
    useEffect(() => {
        if (!progress.subscribed || (finished && progress.status !== 'COMPLETED')) {
            return undefined
        }
        let active = true
        const read = finished ? readBenchmarks(projectId) : getBenchmarks(projectId)
        read
            .then(loaded => {
                if (active) {
                    setMeasurements(loaded)
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
    }, [projectId, progress.subscribed, finished])

    const rows = measurements ?? []

    /** The measurements that are compared, the fastest first, so that the comparison reads in its own order. */
    const comparedRows = useMemo(
        () => (compared === null
            ? []
            : rows.filter(row => compared.includes(row.id))
                .sort((one, other) => metricsOf(other).testCasesPerSecond - metricsOf(one).testCasesPerSecond)),
        [compared, rows]
    )

    /** Where every compared measurement stands: its place, and how much slower it is than the fastest. */
    const ranking = useMemo(() => {
        const fastest = comparedRows.length > 0 ? metricsOf(comparedRows[0]!).testCasesPerSecond : 0
        return new Map<string, Ranking>(comparedRows.map((row, index) => [
            row.id,
            { rank: index + 1, ratio: fastest / metricsOf(row).testCasesPerSecond },
        ]))
    }, [comparedRows])

    const remove = () => {
        setDeleting(true)
        setError(null)
        deleteBenchmarks(projectId, picked)
            .then(() => getBenchmarks(projectId))
            .then(loaded => {
                setMeasurements(loaded)
                setPicked([])
            })
            .catch(deleteError => setError(errorMessage(deleteError)))
            .finally(() => setDeleting(false))
    }

    const pick = (row: BenchmarkResult) => setPicked(current => (current.includes(row.id)
        ? current.filter(id => id !== row.id)
        : [...current, row.id]))

    const pickAll = (checked: boolean) => setPicked(checked ? rows.map(row => row.id) : [])

    const position = (row: BenchmarkResult) => rows.indexOf(row) + 1

    /** The name of the measured table, with the number of test cases one run covered. */
    const measuredTable = (row: BenchmarkResult) => {
        const kind = row.runTable ? 'runs' : 'cases'
        return (
            <Space size={4}>
                <TableLink data-testid={`benchmark-table-${row.id}`} onOpen={onClose} tableId={row.tableId}>
                    {row.name}
                </TableLink>
                {row.testTable && <Tag>{t(`tests.${kind}`, { count: row.testCases })}</Tag>}
            </Space>
        )
    }

    const columns: ListTableColumn<BenchmarkResult>[] = [
        {
            key: 'pick',
            fit: true,
            align: 'center',
            title: (
                <Checkbox
                    checked={rows.length > 0 && picked.length === rows.length}
                    data-testid="benchmark-pick-all"
                    onChange={event => pickAll(event.target.checked)}
                />
            ),
            // The row itself picks the measurement, so a click on the box must not pick it a second time.
            render: row => (
                <Checkbox
                    checked={picked.includes(row.id)}
                    data-testid={`benchmark-pick-${row.id}`}
                    onChange={() => pick(row)}
                    onClick={event => event.stopPropagation()}
                />
            ),
        },
        { key: 'position', fit: true, title: '#', render: row => <Text type="secondary">{position(row)}</Text> },
        { key: 'name', title: t('benchmark.name'), render: measuredTable },
        ...(rows.some(row => (row.parameters ?? []).length > 0) ? [{
            key: 'parameters',
            title: t('benchmark.parameters'),
            render: (row: BenchmarkResult) => (
                <ParameterValueList keyPrefix={`benchmark-${row.id}`} parameters={row.parameters ?? []} />
            ),
        }] : []),
        metricColumn('testCaseMs', t('benchmark.testCaseMs'), t('benchmark.testCaseMsHint'),
            row => formatTime(metricsOf(row).testCaseMs)),
        metricColumn('testCasesPerSecond', t('benchmark.testCasesPerSecond'), t('benchmark.testCasesPerSecondHint'),
            row => formatRate(metricsOf(row).testCasesPerSecond)),
        metricColumn('testCases', t('benchmark.testCases'), t('benchmark.testCasesHint'), row => row.testCases),
        metricColumn('runMs', t('benchmark.runMs'), t('benchmark.runMsHint'),
            row => formatTime(metricsOf(row).runMs)),
        metricColumn('runsPerSecond', t('benchmark.runsPerSecond'), t('benchmark.runsPerSecondHint'),
            row => formatRate(metricsOf(row).runsPerSecond)),
    ]

    const comparedColumns: ListTableColumn<BenchmarkResult>[] = [
        { key: 'position', fit: true, title: '#', render: row => <Text type="secondary">{position(row)}</Text> },
        { key: 'name', title: t('benchmark.name'), render: measuredTable },
        metricColumn('testCasesPerSecond', t('benchmark.testCasesPerSecond'), t('benchmark.testCasesPerSecondHint'),
            row => formatRate(metricsOf(row).testCasesPerSecond)),
        {
            key: 'rank',
            fit: true,
            align: 'center',
            title: t('benchmark.rank'),
            render: row => {
                const place = ranking.get(row.id)?.rank ?? 0
                return <Tag color={RANK_COLOR[place - 1] ?? 'default'}>{place}</Tag>
            },
        },
        metricColumn('ratio', t('benchmark.ratio'), t('benchmark.ratioHint'),
            row => formatRatio(ranking.get(row.id)?.ratio ?? Number.NaN)),
    ]

    if (measurements === null || benchmarkFailed) {
        return (
            <ExecutionModal onClose={onClose} title={t('benchmark.title')}>
                {benchmarkFailed
                    ? <Result status="error" subTitle={progress.error ?? failure ?? undefined} title={t('benchmark.failed')} />
                    : <RunningCard data-testid="benchmark-running" description={t('benchmark.running')} />}
            </ExecutionModal>
        )
    }

    return (
        <ExecutionModal
            onClose={onClose}
            subtitle={t('benchmark.summary', { count: rows.length })}
            title={t('benchmark.title')}
            extra={(
                <>
                    <Button
                        data-testid="benchmark-compare"
                        disabled={picked.length === 0}
                        onClick={() => setCompared(picked)}
                    >
                        {t('benchmark.compare')}
                    </Button>
                    <Button
                        danger
                        data-testid="benchmark-delete"
                        disabled={picked.length === 0}
                        loading={deleting}
                        onClick={remove}
                    >
                        {t('benchmark.deleteSelected')}
                    </Button>
                </>
            )}
        >
            <Space orientation="vertical" size="middle" style={{ width: '100%' }}>
                {error && <Alert showIcon closable={{ onClose: () => setError(null) }} title={error} type="error" />}
                {rows.length === 0
                    ? <Empty description={t('benchmark.none')} />
                    : (
                        <ListTable<BenchmarkResult>
                            columns={columns}
                            data-testid="benchmark-table"
                            onRowClick={pick}
                            rowKey={row => row.id}
                            rows={rows}
                            selected={row => picked.includes(row.id)}
                        />
                    )}
                {comparedRows.length > 0 && (
                    <Space orientation="vertical" size="small" style={{ width: '100%' }}>
                        <Title level={5} style={{ margin: 0 }}>{t('benchmark.comparison')}</Title>
                        <ListTable<BenchmarkResult>
                            columns={comparedColumns}
                            data-testid="benchmark-comparison"
                            rowKey={row => row.id}
                            rows={comparedRows}
                        />
                    </Space>
                )}
            </Space>
        </ExecutionModal>
    )
}

export default BenchmarkResultModal
