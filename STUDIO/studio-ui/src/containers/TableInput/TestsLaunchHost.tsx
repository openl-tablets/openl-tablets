import React, { useCallback, useEffect, useRef, useState } from 'react'
import { Alert, Button, Checkbox, Space, Tooltip } from 'antd'
import { useTranslation } from 'react-i18next'
import { CompoundResultOption, CountSelect, FailuresOption, savedFailuresOption } from 'containers/execution/ResultOptions'
import { TestsResultModal } from 'containers/execution/TestsResultModal'
import { isFinished, useExecutionProgress, useQuietSpells } from 'containers/execution/useExecutionProgress'
import { testsTopics } from 'containers/execution/topics'
import { useEventProject } from 'hooks'
import { ALL_TESTS_ON_A_PAGE, TESTS_PAGE_SIZE, TESTS_PAGE_SIZES } from 'constants/tests'
import {
    getTestsSummaryWorkbook,
    readTestsSummaryWorkbook,
    runTests,
    XLSX_MEDIA_TYPE,
    type TestsQuery,
} from 'services/execution'
import { isStillRunning } from 'services/taskResult'
import { useUserStore } from 'store'
import type { Project } from 'types/projects'
import { saveFile } from 'utils/download'
import { errorMessage } from 'utils/errorMessage'
import { TableInputPopover } from './TableInputPopover'
import type { TableLaunchDetail } from './TableInputLauncher'

/** What the panel asks the results for: how many tables to a page, and what of every case to show. */
type TestsOptions = Required<Pick<TestsQuery, 'size' | 'failuresOnly' | 'failures' | 'compoundResult'>>

/** What a page sends to run tests: one table's tests, or every test of the project when no table is named. */
export interface TestsLaunchDetail extends Omit<TableLaunchDetail, 'tableId'> {
    tableId?: string
}

interface TestsLaunchProps {
    detail: TestsLaunchDetail
    project: Project
    onClose: () => void
}

const TestsLaunch: React.FC<TestsLaunchProps> = ({ detail, project, onClose }) => {
    const { t } = useTranslation('execution')
    const profile = useUserStore(state => state.userProfile)
    const [moduleOnly, setModuleOnly] = useState(detail.moduleOnlyLocked ?? false)
    const [options, setOptions] = useState<TestsOptions>(() => ({
        size: profile?.testsPerPage || TESTS_PAGE_SIZE,
        ...savedFailuresOption(profile),
        compoundResult: profile?.showComplexResult ?? false,
    }))
    const [error, setError] = useState<string | null>(null)
    const [starting, setStarting] = useState(false)
    const [ran, setRan] = useState(false)
    const [savingFile, setSavingFile] = useState(false)
    // Whether the results are still being saved, as the reads waiting their turn below see it: one of them
    // may come up after another has already saved the workbook.
    const saving = useRef(false)
    const setSaving = useCallback((what: boolean) => {
        saving.current = what
        setSavingFile(what)
    }, [])
    // The reads of the results, one after another. A signal arriving while a read is on its way waits for
    // it: two reads could both find the results, and the workbook would be saved twice.
    const reads = useRef<Promise<unknown>>(Promise.resolve())

    // The panel listens from the moment it opens, so a run it starts cannot end unheard.
    const progress = useExecutionProgress(testsTopics(project.id, detail.tableId).status)
    const quietSpells = useQuietSpells(progress, savingFile)

    const start = (intoFile: boolean) => {
        setStarting(true)
        setError(null)
        // What an earlier run reported is forgotten, so that its end is not taken for the end of this one.
        progress.reset()
        const table = detail.tableId ? { tableId: detail.tableId } : {}
        runTests(project.id, { ...table, ...(moduleOnly && { fromModule: detail.moduleName }) })
            .then(() => {
                if (intoFile) {
                    setSaving(true)
                } else {
                    setRan(true)
                }
            })
            .catch(startError => {
                setError(errorMessage(startError))
                setStarting(false)
            })
    }

    /**
     * Saves the results as the workbook, once the tests have ended.
     *
     * Tests that said they ended are waited out while they publish the results. Tests still going on answer one
     * read, and the panel waits for them to say more.
     *
     * @param ended whether the tests said they ended
     */
    const saveResults = useCallback((ended: boolean): Promise<void> => {
        const read = ended ? readTestsSummaryWorkbook : getTestsSummaryWorkbook
        return read(project.id, options)
            .then(workbook => saveFile(workbook, 'test-results.xlsx', XLSX_MEDIA_TYPE))
            .then(() => {
                setSaving(false)
                setStarting(false)
                onClose()
            })
            .catch(saveError => {
                // Tests that are still running have produced no results to save yet. The panel goes on
                // waiting for them rather than reporting a failure that has not happened.
                if (isStillRunning(saveError)) {
                    return
                }
                setError(errorMessage(saveError))
                setSaving(false)
                setStarting(false)
            })
    }, [project.id, options, onClose, setSaving])

    useEffect(() => {
        if (!savingFile) {
            return
        }
        const ended = isFinished(progress.status)
        if (ended && progress.status !== 'COMPLETED') {
            // A run that was stopped produced no results to save; it says so instead.
            setError(progress.error ?? t('tests.startFailed'))
            setSaving(false)
            setStarting(false)
            return
        }
        // The results are asked for when the tests say they have ended, whenever the panel starts or stops
        // hearing them, and when they have said nothing for a while — a run may have ended before the panel
        // started saving, and a connection that drops takes the message with it. Tests still running leave the
        // panel waiting.
        reads.current = reads.current.then(() => (saving.current ? saveResults(ended) : undefined))
    }, [savingFile, progress.status, progress.subscribed, quietSpells])

    if (ran) {
        return <TestsResultModal onClose={onClose} options={options} projectId={project.id} tableId={detail.tableId} />
    }

    const moduleOnlyOption = (
        <Checkbox
            checked={moduleOnly}
            data-testid="tests-module-only"
            disabled={detail.moduleOnlyLocked ?? false}
            onChange={event => setModuleOnly(event.target.checked)}
        >
            {t('input.moduleOnly')}
        </Checkbox>
    )

    return (
        <TableInputPopover
            open
            anchor={detail.anchor}
            busy={starting}
            onClose={onClose}
            width={380}
            footer={(
                <>
                    <Button data-testid="tests-start" loading={starting && !savingFile} onClick={() => start(false)} type="primary">
                        {t('tests.start')}
                    </Button>
                    <Button data-testid="tests-into-file" loading={savingFile} onClick={() => start(true)}>
                        {t('tests.intoFile')}
                    </Button>
                </>
            )}
        >
            <Space orientation="vertical" size="small" style={{ width: '100%' }}>
                {detail.moduleOnlyLocked
                    ? <Tooltip title={t('input.moduleOnlyLocked')}>{moduleOnlyOption}</Tooltip>
                    : moduleOnlyOption}
                {/* Paging matters for a run of the whole project; the tests of one table are a page of their own. */}
                {!detail.tableId && (
                    <CountSelect
                        allValue={ALL_TESTS_ON_A_PAGE}
                        counts={TESTS_PAGE_SIZES}
                        data-testid="tests-per-page"
                        label={t('tests.perPage')}
                        onChange={size => setOptions(current => ({ ...current, size }))}
                        value={options.size}
                    />
                )}
                <FailuresOption
                    failures={options.failures}
                    failuresOnly={options.failuresOnly}
                    onChange={change => setOptions(current => ({ ...current, ...change }))}
                />
                <CompoundResultOption
                    checked={options.compoundResult}
                    onChange={compoundResult => setOptions(current => ({ ...current, compoundResult }))}
                />
                {error && <Alert showIcon data-testid="tests-launch-error" title={error} type="error" />}
            </Space>
        </TableInputPopover>
    )
}

/**
 * Opens the test launcher under the Test button of the table page, and under the one of the module toolbar.
 *
 * The Test button of a rule table runs every test that tests it. The button of the module toolbar names no
 * table and runs every test of the project, or of the current module when the user asks for that. Both report
 * in the same window, which opens with the options the panel was left with.
 *
 * Test into File saves the results as a workbook without showing them, which is what a run too large to read
 * on screen is taken with.
 */
export const TestsLaunchHost: React.FC = () => {
    const { detail, project, close } = useEventProject<TestsLaunchDetail>('openTestsLaunch', 'execution:tests.startFailed')
    return detail && project
        ? <TestsLaunch key={`${project.id} ${detail.tableId ?? 'all'}`} detail={detail} onClose={close} project={project} />
        : null
}

export default TestsLaunchHost
