import React, { useCallback, useEffect, useState } from 'react'
import { Alert, Button, Checkbox, Select, Space, Tooltip, Typography } from 'antd'
import { useTranslation } from 'react-i18next'
import { TestsResultModal } from 'containers/execution/TestsResultModal'
import { isFinished, useExecutionProgress } from 'containers/execution/useExecutionProgress'
import { useEventProject } from 'hooks'
import {
    ALL_TESTS_ON_A_PAGE,
    readTestsSummaryWorkbook,
    runTests,
    TESTS_PAGE_SIZE,
    TESTS_PAGE_SIZES,
    XLSX_MEDIA_TYPE,
    type TestsQuery,
} from 'services/execution'
import { useUserStore } from 'store'
import type { Project } from 'types/projects'
import { saveFile } from 'utils/download'
import { errorMessage } from 'utils/errorMessage'
import { TableInputPopover } from './TableInputPopover'
import type { TableLaunchDetail } from './TableInputLauncher'

const { Text } = Typography

/** What the panel asks the results for: how many tables to a page, and what of every case to show. */
type TestsOptions = Required<Pick<TestsQuery, 'size' | 'failuresOnly' | 'compoundResult'>>

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
    const [options, setOptions] = useState<TestsOptions>({
        size: profile?.testsPerPage || TESTS_PAGE_SIZE,
        failuresOnly: profile?.testsFailuresOnly ?? false,
        compoundResult: profile?.showComplexResult ?? false,
    })
    const [error, setError] = useState<string | null>(null)
    const [starting, setStarting] = useState(false)
    const [ran, setRan] = useState(false)
    const [savingFile, setSavingFile] = useState(false)

    const scope = detail.tableId
        ? `${encodeURIComponent(project.id)}/tables/${encodeURIComponent(detail.tableId)}`
        : encodeURIComponent(project.id)
    // The panel listens from the moment it opens, so a run it starts cannot end unheard.
    const progress = useExecutionProgress(`/user/topic/projects/${scope}/tests/status`)

    const start = (intoFile: boolean) => {
        setStarting(true)
        setError(null)
        // What an earlier run reported is forgotten, so that its end is not taken for the end of this one.
        progress.reset()
        const table = detail.tableId ? { tableId: detail.tableId } : {}
        runTests(project.id, { ...table, ...(moduleOnly && { fromModule: detail.moduleName }) })
            .then(() => {
                if (intoFile) {
                    setSavingFile(true)
                } else {
                    setRan(true)
                }
            })
            .catch(startError => {
                setError(errorMessage(startError))
                setStarting(false)
            })
    }

    /** Saves the results as the workbook, once the tests have ended. */
    const saveResults = useCallback(() => {
        readTestsSummaryWorkbook(project.id, options)
            .then(workbook => saveFile(workbook, 'test-results.xlsx', XLSX_MEDIA_TYPE))
            .then(onClose)
            .catch(saveError => setError(errorMessage(saveError)))
            .finally(() => {
                setSavingFile(false)
                setStarting(false)
            })
    }, [project.id, options, onClose])

    useEffect(() => {
        if (!savingFile || !isFinished(progress.status)) {
            return
        }
        if (progress.status === 'COMPLETED') {
            saveResults()
            return
        }
        // A run that was stopped produced no results to save; it says so instead.
        setError(progress.error ?? t('tests.startFailed'))
        setSavingFile(false)
        setStarting(false)
        // The tests have ended: their results are there to be saved.
    }, [savingFile, progress.status])

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
                    <Space size={4}>
                        <Text type="secondary">{t('tests.perPage')}</Text>
                        <Select<number>
                            data-testid="tests-per-page"
                            onChange={(size: number) => setOptions(current => ({ ...current, size }))}
                            size="small"
                            style={{ width: 80 }}
                            value={options.size}
                            options={TESTS_PAGE_SIZES.map(size => ({
                                value: size,
                                label: size === ALL_TESTS_ON_A_PAGE ? t('tests.allOnAPage') : String(size),
                            }))}
                        />
                    </Space>
                )}
                <Checkbox
                    checked={options.failuresOnly}
                    data-testid="tests-failures-only"
                    onChange={event => setOptions(current => ({ ...current, failuresOnly: event.target.checked }))}
                >
                    {t('tests.failuresOnly')}
                </Checkbox>
                <Checkbox
                    checked={options.compoundResult}
                    data-testid="tests-compound-result"
                    onChange={event => setOptions(current => ({ ...current, compoundResult: event.target.checked }))}
                >
                    {t('tests.compoundResult')}
                </Checkbox>
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
