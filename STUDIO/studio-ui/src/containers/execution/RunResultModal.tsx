import React, { useEffect, useMemo, useState } from 'react'
import { Alert, Button, Result, Space } from 'antd'
import { DownloadOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { ListTable, type ListTableColumn } from 'components/ListTable'
import { RunningCard } from 'components/RunningCard'
import { ValueCell } from 'components/values/ParameterValues'
import { SpreadsheetValue } from 'components/values/SpreadsheetValue'
import { getRunResultWorkbook, isStillRunning, readRunResult, XLSX_MEDIA_TYPE, type RunFileOptions } from 'services/execution'
import type { RunResult } from 'types/execution'
import { saveFile } from 'utils/download'
import { errorMessage } from 'utils/errorMessage'
import { isFinished, useExecutionProgress } from './useExecutionProgress'
import { runStatusTopic } from './topics'
import { ExecutionErrors, ExecutionModal, nameOf } from './ExecutionModal'

/** The single row of the result table: the values the table ran with, and the value it returned. */
interface RunRow {
    key: string
}

/** What the run reported, shown as one row of the values it ran with and the value it returned. */
const RunResultTable: React.FC<{ result: RunResult }> = ({ result }) => {
    const { t } = useTranslation('execution')
    const inputs = useMemo(
        () => [...(result.contextParameters ?? []), ...(result.parameters ?? [])],
        [result.contextParameters, result.parameters]
    )

    const columns: ListTableColumn<RunRow>[] = [
        ...inputs.map((input, index): ListTableColumn<RunRow> => ({
            key: `input-${index}`,
            title: nameOf(input),
            render: () => <ValueCell path={`run-input-${index}`} value={input.value} />,
        })),
        {
            key: 'result',
            title: t('run.result'),
            render: () => (result.resultSpreadsheet
                ? <SpreadsheetValue keyPrefix="run-result" spreadsheet={result.resultSpreadsheet} />
                : <ValueCell path="run-result" value={result.result} />),
        },
    ]

    return (
        <ListTable<RunRow>
            columns={columns}
            data-testid="run-result-table"
            rowKey={row => row.key}
            rows={[{ key: 'run' }]}
        />
    )
}

export interface RunResultModalProps {
    projectId: string
    tableId: string
    /** How the workbook of this run is written, as the launch panel asked for it. */
    fileOptions: RunFileOptions
    onClose: () => void
}

/**
 * The result of running one table.
 *
 * The window opens while the run is still on its way and waits for it, asking again as long as the run answers
 * that it has not ended. The input the table ran with and the value it returned stand side by side, a column
 * each, and a spreadsheet result is shown as the table it was calculated by.
 */
export const RunResultModal: React.FC<RunResultModalProps> = ({ projectId, tableId, fileOptions, onClose }) => {
    const { t } = useTranslation('execution')
    const [result, setResult] = useState<RunResult | null>(null)
    const [failure, setFailure] = useState<string | null>(null)
    const [error, setError] = useState<string | null>(null)
    const [saving, setSaving] = useState(false)
    const progress = useExecutionProgress(runStatusTopic(projectId, tableId))

    const finished = isFinished(progress.status)

    // The run says when it has ended. The screen also reads once as soon as it is listening, for a run that
    // ended before the window was there to hear about it.
    useEffect(() => {
        if (!progress.subscribed) {
            return undefined
        }
        let active = true
        // The window shows a spreadsheet result as the table it was calculated by, so it asks for that layout.
        readRunResult(projectId, { spreadsheet: true })
            .then(loaded => {
                if (active) {
                    setResult(loaded)
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

    const save = () => {
        setSaving(true)
        getRunResultWorkbook(projectId, fileOptions)
            .then(workbook => saveFile(workbook, 'run-result.xlsx', XLSX_MEDIA_TYPE))
            .catch(saveError => setError(errorMessage(saveError)))
            .finally(() => setSaving(false))
    }

    if (result === null) {
        const reason = progress.error ?? failure
        const runFailed = failure !== null || (finished && progress.status !== 'COMPLETED')
        return (
            <ExecutionModal onClose={onClose} title={t('run.title')}>
                {runFailed
                    ? <Result status="error" subTitle={reason ?? undefined} title={t('run.failed')} />
                    : <RunningCard data-testid="run-running" description={t('run.running')} />}
            </ExecutionModal>
        )
    }

    return (
        <ExecutionModal
            onClose={onClose}
            subtitle={t('run.time', { ms: Math.round(result.executionTimeMs) })}
            title={t('run.of', { table: result.tableName })}
            extra={(
                <Button data-testid="run-save" icon={<DownloadOutlined />} loading={saving} onClick={save}>
                    {t('run.saveToExcel')}
                </Button>
            )}
        >
            <Space orientation="vertical" size="middle" style={{ width: '100%' }}>
                {error && <Alert showIcon closable={{ onClose: () => setError(null) }} title={error} type="error" />}
                <ExecutionErrors errors={result.errors} />
                <RunResultTable result={result} />
            </Space>
        </ExecutionModal>
    )
}

export default RunResultModal
