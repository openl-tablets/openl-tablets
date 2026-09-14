import React, { useCallback, useEffect, useRef, useState } from 'react'
import { Button, Checkbox, notification, Space } from 'antd'
import { useTranslation } from 'react-i18next'
import { RunResultModal } from 'containers/execution/RunResultModal'
import { TestsResultModal } from 'containers/execution/TestsResultModal'
import { isFinished, useExecutionProgress } from 'containers/execution/useExecutionProgress'
import { runStatusTopic, testsTopics } from 'containers/execution/topics'
import { useEventProject } from 'hooks'
import {
    isStillRunning,
    readRunResult,
    readRunResultWorkbook,
    readTestsSummaryWorkbook,
    runTests,
    startRun,
    XLSX_MEDIA_TYPE,
    type TestsQuery,
} from 'services/execution'
import { useUserStore } from 'store'
import type { Project } from 'types/projects'
import { saveFile } from 'utils/download'
import { errorMessage } from 'utils/errorMessage'
import { TableInputLauncher, type TableLaunchDetail, type TableLaunchValue } from './TableInputLauncher'

/** What the run writes into a file: how the workbook is laid out, or that the result is saved as JSON. */
interface RunFileChoice {
    skipEmptyParameters: boolean
    flattenParameters: boolean
    resultInJson: boolean
}

/** What the results of the cases of a test table show. */
type TestsOptions = Required<Pick<TestsQuery, 'failuresOnly' | 'compoundResult'>>

/** The results the run opens: one result for a rule table, the results of the cases for a test table. */
type RunResults = 'run' | 'tests'

interface RunLaunchProps {
    detail: TableLaunchDetail
    project: Project
    onClose: () => void
}

const RunLaunch: React.FC<RunLaunchProps> = ({ detail, project, onClose }) => {
    const { t } = useTranslation('execution')
    const profile = useUserStore(state => state.userProfile)
    const [error, setError] = useState<string | null>(null)
    const [starting, setStarting] = useState(false)
    const [results, setResults] = useState<RunResults | null>(null)
    const [savingFile, setSavingFile] = useState<RunResults | null>(null)
    // What is being saved, as the reads waiting their turn below see it: one of them may come up after
    // another has already saved the file.
    const saving = useRef<RunResults | null>(null)
    const setSaving = useCallback((what: RunResults | null) => {
        saving.current = what
        setSavingFile(what)
    }, [])
    // The reads of a result, one after another. A signal arriving while a read is on its way waits for it:
    // two reads could both find the result, and the file would be saved twice.
    const reads = useRef<Promise<unknown>>(Promise.resolve())
    // The workbook starts out the way it is written when nothing is asked for, which is the sheet the
    // download this panel replaced produced.
    const [file, setFile] = useState<RunFileChoice>({
        skipEmptyParameters: true,
        flattenParameters: false,
        resultInJson: false,
    })
    const [testsOptions, setTestsOptions] = useState<TestsOptions>({
        failuresOnly: profile?.testsFailuresOnly ?? false,
        compoundResult: profile?.showComplexResult ?? false,
    })

    // The panel listens from the moment it opens, so a run it starts cannot end unheard. A rule table and a
    // test table report on topics of their own, and only one of the two is ever started from here.
    const runProgress = useExecutionProgress(runStatusTopic(project.id, detail.tableId))
    const testsProgress = useExecutionProgress(testsTopics(project.id, detail.tableId).status)

    /**
     * Runs what the panel collected.
     *
     * A rule table runs on its own and reports one result. A test table runs the cases that are picked, which
     * is a test run and reports the results of every case.
     */
    const run = useCallback((value: TableLaunchValue): Promise<RunResults> => {
        const { inputJson, testRanges, fromModule } = value
        return inputJson === undefined
            ? runTests(project.id, { tableId: detail.tableId, ...(testRanges && { testRanges }), ...(fromModule && { fromModule }) })
                .then((): RunResults => 'tests')
            : startRun(project.id, detail.tableId, inputJson, { ...(fromModule && { fromModule }) })
                .then((): RunResults => 'run')
    }, [project.id, detail.tableId])

    /** Runs what the panel collected and opens the window it reports in. */
    const start = useCallback((value: TableLaunchValue) => {
        setStarting(true)
        setError(null)
        return run(value)
            .then(setResults)
            .finally(() => setStarting(false))
    }, [run])

    /** Runs what the panel collected and saves what came out, without showing it. */
    const startIntoFile = (value: TableLaunchValue) => {
        setStarting(true)
        setError(null)
        // What an earlier run reported is forgotten, so that its end is not taken for the end of this one.
        runProgress.reset()
        testsProgress.reset()
        run(value)
            .then(setSaving)
            .catch(runError => {
                setError(errorMessage(runError))
                setStarting(false)
            })
    }

    /**
     * Saves what the execution returned.
     *
     * A rule table gives the workbook of the run, written the way the options ask for, or the returned value
     * on its own in JSON. A test table gives the workbook of the results.
     */
    const saveResult = useCallback((kind: RunResults): Promise<void> => {
        const write = (): Promise<void> => {
            if (kind === 'tests') {
                return readTestsSummaryWorkbook(project.id, testsOptions)
                    .then(workbook => saveFile(workbook, 'test-results.xlsx', XLSX_MEDIA_TYPE))
            }
            if (file.resultInJson) {
                return readRunResult(project.id).then(result =>
                    saveFile(JSON.stringify(result.result ?? null, null, 2), 'response.json', 'application/json'))
            }
            return readRunResultWorkbook(project.id, file)
                .then(workbook => saveFile(workbook, 'run-result.xlsx', XLSX_MEDIA_TYPE))
        }
        return write()
            .then(() => {
                setSaving(null)
                setStarting(false)
                onClose()
            })
            .catch(saveError => {
                // An execution that is still going on has produced nothing to save yet. The panel goes on
                // waiting for it rather than reporting a failure that has not happened.
                if (isStillRunning(saveError)) {
                    return
                }
                setError(errorMessage(saveError))
                setSaving(null)
                setStarting(false)
            })
    }, [project.id, file, testsOptions, onClose, setSaving])

    // The execution that is being saved reports on its own topic: a rule table on the run's, a test table on
    // the tests'.
    const progress = savingFile === 'tests' ? testsProgress : runProgress
    const finished = isFinished(progress.status)
    useEffect(() => {
        if (!savingFile) {
            return
        }
        if (finished && progress.status !== 'COMPLETED') {
            // A run that failed or was stopped produced nothing to save; it says why instead.
            setError(progress.error ?? t(savingFile === 'tests' ? 'tests.startFailed' : 'run.startFailed'))
            setSaving(null)
            setStarting(false)
            return
        }
        // What the execution produced is asked for when it says it has ended, and whenever the panel starts
        // or stops hearing it — a run may have ended before the panel started saving, and a connection that
        // drops takes the message with it. A run still going on leaves the panel waiting.
        reads.current = reads.current.then(() => (saving.current ? saveResult(saving.current) : undefined))
    }, [savingFile, finished, progress.subscribed])

    const fileOption = (name: keyof RunFileChoice, label: string) => (
        <Checkbox
            checked={file[name]}
            data-testid={`run-${name}`}
            onChange={event => setFile(current => ({ ...current, [name]: event.target.checked }))}
        >
            {label}
        </Checkbox>
    )

    const testsOption = (name: keyof TestsOptions, testId: string, label: string) => (
        <Checkbox
            checked={testsOptions[name]}
            data-testid={testId}
            onChange={event => setTestsOptions(current => ({ ...current, [name]: event.target.checked }))}
        >
            {label}
        </Checkbox>
    )

    if (results !== null) {
        return results === 'run'
            ? <RunResultModal fileOptions={file} onClose={onClose} projectId={project.id} tableId={detail.tableId} />
            : (
                <TestsResultModal
                    onClose={onClose}
                    options={testsOptions}
                    projectId={project.id}
                    tableId={detail.tableId}
                />
            )
    }

    return (
        <TableInputLauncher
            busy={starting}
            caseSelection="multiple"
            detail={detail}
            error={error}
            onClose={onClose}
            onError={setError}
            project={project}
            actions={(collect, state) => (
                <>
                    <Button
                        data-testid="run-start"
                        loading={starting && savingFile === null}
                        type="primary"
                        onClick={() => {
                            const value = collect()
                            if (value) {
                                start(value).catch(startError => setError(errorMessage(startError)))
                            }
                        }}
                    >
                        {t('run.start')}
                    </Button>
                    <Button
                        data-testid="run-into-file"
                        loading={savingFile !== null}
                        onClick={() => {
                            const value = collect()
                            if (value) {
                                startIntoFile(value)
                            }
                        }}
                    >
                        {state.testTable ? t('tests.intoFile') : t('run.intoFile')}
                    </Button>
                </>
            )}
            onNothingToAsk={value => {
                start(value).catch(startError => {
                    notification.error({ title: t('run.startFailed'), description: errorMessage(startError) })
                    onClose()
                })
            }}
            options={state => (state.testTable
                ? (
                    <Space wrap size="middle">
                        {testsOption('failuresOnly', 'tests-failures-only', t('tests.failuresOnly'))}
                        {testsOption('compoundResult', 'tests-compound-result', t('tests.compoundResult'))}
                    </Space>
                )
                : (
                    <Space wrap size="middle">
                        {fileOption('skipEmptyParameters', t('run.skipEmptyParameters'))}
                        {fileOption('flattenParameters', t('run.flattenParameters'))}
                        {fileOption('resultInJson', t('run.resultInJson'))}
                    </Space>
                ))}
        />
    )
}

/**
 * Opens the run launcher under the Run button of the table page.
 *
 * A rule table is run with the input the panel collects and reports one result. A test table runs the cases
 * that are picked, and reports them as a test run. A table that takes nothing runs at once.
 *
 * The second button saves what came out without showing it: the workbook of the run or the returned value in
 * JSON for a rule table, the workbook of the results for a test table.
 */
export const RunLaunchHost: React.FC = () => {
    const { detail, project, close } = useEventProject<TableLaunchDetail>('openRunLaunch', 'execution:input.loadFailed')
    return detail && project
        ? <RunLaunch key={`${project.id} ${detail.tableId}`} detail={detail} onClose={close} project={project} />
        : null
}

export default RunLaunchHost
