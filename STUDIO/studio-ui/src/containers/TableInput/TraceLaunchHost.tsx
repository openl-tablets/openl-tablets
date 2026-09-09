import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { Alert, Button, Checkbox, notification, Space, Tooltip } from 'antd'
import { useTranslation } from 'react-i18next'
import { useEventProject, type EventProjectDetail } from 'hooks'
import { notifyLoadFailure } from 'services/apiCall'
import { getTableInput, getTableInputCase, getTableInputCases, TEST_CASES_PAGE_SIZE } from 'services/tables'
import { launchTrace } from 'services/traceLaunch'
import { useAppStore, useUserStore } from 'store'
import type { Project } from 'types/projects'
import type { TableInput, TableInputCasesPage, TableInputTestCase } from 'types/tables'
import { errorMessage } from 'utils/errorMessage'
import { ParametersInput, type ParametersInputValue } from './ParametersInput'
import { TableInputPopover, type PopoverAnchor } from './TableInputPopover'
import { TestCaseSelector } from './TestCaseSelector'

/** What the table page sends to start a trace of the table it shows. */
export interface TraceLaunchDetail extends EventProjectDetail {
    tableId: string
    moduleName: string
    /** Viewport rectangle of the Trace button, which the popover hangs under. */
    anchor: PopoverAnchor
    /** Only the current module can be traced. The project is still loading, or another module has errors. */
    moduleOnlyLocked?: boolean
}

interface TraceLaunchProps {
    detail: TraceLaunchDetail
    project: Project
    onClose: () => void
}

const TraceLaunch: React.FC<TraceLaunchProps> = ({ detail, project, onClose }) => {
    const { t } = useTranslation('trace')
    const { t: te } = useTranslation('execution')
    const showLoader = useAppStore(state => state.showLoader)
    const hideLoader = useAppStore(state => state.hideLoader)
    const showRealNumbers = useUserStore(state => state.userProfile?.showRealNumbers ?? false)
    const [input, setInput] = useState<TableInput | null>(null)
    const [moduleOnly, setModuleOnly] = useState(detail.moduleOnlyLocked ?? false)
    const [advanced, setAdvanced] = useState(false)
    const [casesPage, setCasesPage] = useState<TableInputCasesPage | null>(null)
    const [casesLoading, setCasesLoading] = useState(false)
    const [page, setPage] = useState(1)
    const [caseId, setCaseId] = useState('')
    const [parameters, setParameters] = useState<ParametersInputValue>({ inputJson: '{}' })
    const [error, setError] = useState<string | null>(null)
    const [starting, setStarting] = useState(false)

    const fromModule = moduleOnly ? detail.moduleName : undefined
    // A project still loading can only describe the table within its module.
    const readWithin = useMemo(
        () => (detail.moduleOnlyLocked ? { fromModule: detail.moduleName } : {}),
        [detail.moduleOnlyLocked, detail.moduleName]
    )

    const start = useCallback(async (request: { testRanges?: string | undefined, inputJson?: string | undefined }, download: boolean) => {
        setStarting(true)
        setError(null)
        try {
            await launchTrace({
                projectId: project.id,
                tableId: detail.tableId,
                ...request,
                fromModule,
                download,
                advanced,
                showRealNumbers,
            })
            onClose()
        } catch (launchError) {
            setError(errorMessage(launchError))
        } finally {
            setStarting(false)
        }
    }, [project.id, detail.tableId, fromModule, advanced, showRealNumbers, onClose])

    // The API leaves an empty list out, so a rule table without parameters carries none at all.
    const declaredParameters = input?.parameters ?? []
    const testTable = input?.testTable ?? false
    // A table without parameters and without cases has nothing to ask. It is traced at once.
    const traceAtOnce = input !== null && !testTable && declaredParameters.length === 0

    useEffect(() => {
        let active = true
        showLoader()
        getTableInput(project.id, detail.tableId, readWithin)
            .then(loaded => {
                if (active) {
                    setInput(loaded)
                }
            })
            .catch(loadError => {
                if (active) {
                    notifyLoadFailure(t('launch.loadFailed'), loadError)
                    onClose()
                }
            })
            .finally(hideLoader)
        return () => {
            active = false
        }
    }, [project.id, detail.tableId, readWithin, showLoader, hideLoader, t, onClose])

    useEffect(() => {
        if (!testTable) {
            return undefined
        }
        let active = true
        setCasesLoading(true)
        getTableInputCases(project.id, detail.tableId, { ...readWithin, page: page - 1, size: TEST_CASES_PAGE_SIZE })
            .then(loaded => {
                if (active) {
                    setCasesPage(loaded)
                    // The first case of the table is the one offered; a later page leaves the choice as it is.
                    setCaseId(current => current || (loaded.content[0]?.id ?? ''))
                }
            })
            .catch(loadError => {
                if (active) {
                    setError(errorMessage(loadError))
                }
            })
            .finally(() => {
                if (active) {
                    setCasesLoading(false)
                }
            })
        return () => {
            active = false
        }
    }, [testTable, project.id, detail.tableId, readWithin, page])

    const loadCase = useCallback(
        (id: string): Promise<TableInputTestCase> => getTableInputCase(project.id, detail.tableId, id, readWithin),
        [project.id, detail.tableId, readWithin]
    )

    useEffect(() => {
        if (traceAtOnce) {
            start({ inputJson: '{}' }, false).catch(() => undefined)
        }
    }, [traceAtOnce, start])

    useEffect(() => {
        if (traceAtOnce && error) {
            notification.error({ title: t('launch.startFailed'), description: error })
            onClose()
        }
    }, [traceAtOnce, error, t, onClose])

    if (input === null || traceAtOnce) {
        return null
    }

    const trace = (download: boolean) => {
        if (testTable) {
            if (!caseId) {
                setError(te('testCases.noCase'))
                return
            }
            // The trace API takes the case as a range of one id.
            void start({ testRanges: caseId }, download)
        } else {
            if (parameters.error) {
                setError(parameters.error)
                return
            }
            void start({ inputJson: parameters.inputJson }, download)
        }
    }

    const moduleOnlyOption = (
        <Checkbox
            checked={moduleOnly}
            data-testid="trace-module-only"
            disabled={detail.moduleOnlyLocked ?? false}
            onChange={event => setModuleOnly(event.target.checked)}
        >
            {te('input.moduleOnly')}
        </Checkbox>
    )

    return (
        <TableInputPopover
            open
            anchor={detail.anchor}
            busy={starting}
            onClose={onClose}
            width={testTable ? 560 : 520}
            footer={(
                <>
                    <Button data-testid="trace-start" loading={starting} onClick={() => trace(false)} type="primary">
                        {t('launch.trace')}
                    </Button>
                    <Button data-testid="trace-download" disabled={starting} onClick={() => trace(true)}>
                        {t('launch.traceIntoFile')}
                    </Button>
                </>
            )}
        >
            <Space orientation="vertical" size="small" style={{ width: '100%' }}>
                <Space wrap size="middle">
                    {detail.moduleOnlyLocked
                        ? <Tooltip title={te('input.moduleOnlyLocked')}>{moduleOnlyOption}</Tooltip>
                        : moduleOnlyOption}
                    <Tooltip title={t('launch.advancedHint')}>
                        <Checkbox
                            checked={advanced}
                            data-testid="trace-advanced"
                            onChange={event => setAdvanced(event.target.checked)}
                        >
                            {t('launch.advanced')}
                        </Checkbox>
                    </Tooltip>
                </Space>
                {testTable ? (
                    <TestCaseSelector
                        loadCase={loadCase}
                        loading={casesLoading}
                        onChange={setCaseId}
                        onPageChange={setPage}
                        page={page}
                        pageSize={TEST_CASES_PAGE_SIZE}
                        testCases={casesPage?.content ?? []}
                        total={casesPage?.total ?? 0}
                        value={caseId}
                    />
                ) : (
                    <ParametersInput
                        onChange={setParameters}
                        parameters={declaredParameters}
                        runtimeContext={input.runtimeContext}
                    />
                )}
                {error && <Alert showIcon data-testid="trace-launch-error" title={error} type="error" />}
            </Space>
        </TableInputPopover>
    )
}

/**
 * Opens the trace launcher under the Trace button of the table page.
 *
 * The page sends the project id, the table and where the button is. The project and the input the table takes
 * are read here.
 *
 * A rule table gets a form for its parameters. A test table gets the choice of its cases. A table that takes
 * nothing is traced at once.
 */
export const TraceLaunchHost: React.FC = () => {
    const { detail, project, close } = useEventProject<TraceLaunchDetail>('openTraceLaunch', 'trace:launch.loadFailed')
    return detail && project
        ? <TraceLaunch key={`${project.id} ${detail.tableId}`} detail={detail} onClose={close} project={project} />
        : null
}

export default TraceLaunchHost
