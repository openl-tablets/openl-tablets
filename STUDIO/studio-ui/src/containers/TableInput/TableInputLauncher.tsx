import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { Alert, Checkbox, Space, Tooltip } from 'antd'
import { useTranslation } from 'react-i18next'
import type { EventProjectDetail } from 'hooks'
import { notifyLoadFailure } from 'services/apiCall'
import { getTableInput, getTableInputCase, getTableInputCases, TEST_CASES_PAGE_SIZE } from 'services/tables'
import { useAppStore } from 'store'
import type { Project } from 'types/projects'
import type { TableInput, TableInputCasesPage, TableInputTestCase } from 'types/tables'
import { errorMessage } from 'utils/errorMessage'
import { ParametersInput, type ParametersInputValue } from './ParametersInput'
import { TableInputPopover, type PopoverAnchor } from './TableInputPopover'
import { TestCaseSelector } from './TestCaseSelector'

/** What the table page sends to start an action on the table it shows. */
export interface TableLaunchDetail extends EventProjectDetail {
    tableId: string
    moduleName: string
    /** Viewport rectangle of the button the panel hangs under. */
    anchor: PopoverAnchor
    /** Only the current module can be used. The project is still loading, or another module has errors. */
    moduleOnlyLocked?: boolean
}

/** What the panel is asking for, so that an action offers only what applies to the table. */
export interface TableLaunchState {
    /** Whether the table is a test table, whose cases are picked instead of an input. */
    testTable: boolean
}

/** What the panel collected: the input of a rule table, or the cases of a test table, and where to look. */
export interface TableLaunchValue {
    inputJson?: string | undefined
    testRanges?: string | undefined
    fromModule?: string | undefined
}

export interface TableInputLauncherProps {
    detail: TableLaunchDetail
    project: Project
    /** Whether the action takes one case of a test table, several of them, or none at all. */
    caseSelection: 'single' | 'multiple' | 'none'
    /** Whether an action the panel started is still running. */
    busy: boolean
    /** Why the action could not be started, shown under the panel. */
    error?: string | null | undefined
    /** Options of the action, shown next to "Within Current Module Only". */
    options?: ((state: TableLaunchState) => React.ReactNode) | undefined
    /**
     * The buttons that start the action.
     *
     * `collect` answers what the panel holds, or `null` when it is not ready - it then says why itself.
     */
    actions: (collect: () => TableLaunchValue | null, state: TableLaunchState) => React.ReactNode
    /** Starts the action for a table that asks for nothing, so no panel is shown for it. */
    onNothingToAsk?: ((value: TableLaunchValue) => void) | undefined
    /** Reports a reason of its own, such as a case that is not picked. */
    onError: (reason: string | null) => void
    onClose: () => void
}

/**
 * The panel a table action starts from, under the button that opened it.
 *
 * A rule table is given its input: a form built from the schema of every parameter, or the same input as JSON.
 * A test table is given the cases to run instead, one or several. Both can be limited to the current module.
 *
 * A table that asks for nothing shows no panel: the action starts as soon as the table says so.
 */
export const TableInputLauncher: React.FC<TableInputLauncherProps> = ({
    detail,
    project,
    caseSelection,
    busy,
    error,
    options,
    actions,
    onNothingToAsk,
    onError,
    onClose,
}) => {
    const { t } = useTranslation('execution')
    const showLoader = useAppStore(state => state.showLoader)
    const hideLoader = useAppStore(state => state.hideLoader)
    const [input, setInput] = useState<TableInput | null>(null)
    const [moduleOnly, setModuleOnly] = useState(detail.moduleOnlyLocked ?? false)
    const [casesPage, setCasesPage] = useState<TableInputCasesPage | null>(null)
    const [casesLoading, setCasesLoading] = useState(false)
    const [page, setPage] = useState(1)
    const [caseIds, setCaseIds] = useState<string[]>([])
    const [allCases, setAllCases] = useState(caseSelection === 'multiple')
    const [parameters, setParameters] = useState<ParametersInputValue>({ inputJson: '{}' })

    // The table is described the way it will be run: within the current module when that is what is asked
    // for, and always so while the project is still loading.
    const fromModule = moduleOnly ? detail.moduleName : undefined
    const readWithin = useMemo(() => (fromModule ? { fromModule } : {}), [fromModule])
    const testTable = input?.testTable ?? false
    // The API leaves an empty list out, so a rule table without parameters carries none at all. The list is
    // the one the table was read with: built anew on every render, it would look like another description of
    // the table each time, and the form would start again under the user.
    const declaredParameters = useMemo(() => input?.parameters ?? [], [input])
    const withCases = testTable && caseSelection !== 'none'
    // A table with a runtime context always has something to ask for: the rules read the context even when the
    // table itself takes no parameters, and versions of a rule are chosen by it.
    const nothingToAsk = input !== null && !withCases && !input.runtimeContext
        && (testTable || declaredParameters.length === 0)

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
                    notifyLoadFailure(t('input.loadFailed'), loadError)
                    onClose()
                }
            })
            .finally(hideLoader)
        return () => {
            active = false
        }
    }, [project.id, detail.tableId, readWithin, showLoader, hideLoader, t, onClose])

    useEffect(() => {
        if (!withCases) {
            return undefined
        }
        let active = true
        setCasesLoading(true)
        getTableInputCases(project.id, detail.tableId, { ...readWithin, page: page - 1, size: TEST_CASES_PAGE_SIZE })
            .then(loaded => {
                if (active) {
                    setCasesPage(loaded)
                    // One case is asked for: the first of the table is offered. Several are all of them at once.
                    if (caseSelection === 'single') {
                        setCaseIds(current => (current.length > 0 ? current : [loaded.content[0]?.id ?? ''].filter(Boolean)))
                    }
                }
            })
            .catch(loadError => {
                if (active) {
                    onError(errorMessage(loadError))
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
    }, [withCases, caseSelection, project.id, detail.tableId, readWithin, page, onError])

    const loadCase = useCallback(
        (tableId: string, id: string): Promise<TableInputTestCase> => getTableInputCase(project.id, tableId, id, readWithin),
        [project.id, readWithin]
    )

    const collect = (): TableLaunchValue | null => {
        onError(null)
        if (withCases) {
            if (allCases) {
                return { fromModule }
            }
            if (caseIds.length === 0) {
                onError(caseSelection === 'single' ? t('testCases.noCase') : t('testCases.none'))
                return null
            }
            return { testRanges: caseIds.join(','), fromModule }
        }
        if (parameters.error) {
            onError(parameters.error)
            return null
        }
        return { inputJson: parameters.inputJson, fromModule }
    }

    useEffect(() => {
        if (nothingToAsk && onNothingToAsk) {
            onNothingToAsk({ ...(testTable ? {} : { inputJson: '{}' }), fromModule })
        }
        // The action starts once, as soon as the table says it asks for nothing.
    }, [nothingToAsk])

    if (input === null || (nothingToAsk && onNothingToAsk)) {
        return null
    }

    const launchState: TableLaunchState = { testTable }

    const pickCases = (picked: string[]) => {
        setCaseIds(picked)
        setAllCases(false)
    }

    const moduleOnlyOption = (
        <Checkbox
            checked={moduleOnly}
            data-testid="launch-module-only"
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
            busy={busy}
            footer={actions(collect, launchState)}
            onClose={onClose}
            width={withCases ? 560 : 520}
        >
            <Space orientation="vertical" size="small" style={{ width: '100%' }}>
                <Space wrap size="middle">
                    {detail.moduleOnlyLocked
                        ? <Tooltip title={t('input.moduleOnlyLocked')}>{moduleOnlyOption}</Tooltip>
                        : moduleOnlyOption}
                    {options?.(launchState)}
                    {withCases && caseSelection === 'multiple' && (
                        <Checkbox
                            checked={allCases}
                            data-testid="launch-all-cases"
                            onChange={event => {
                                setAllCases(event.target.checked)
                                if (event.target.checked) {
                                    setCaseIds([])
                                }
                            }}
                        >
                            {t('tests.allCases')}
                        </Checkbox>
                    )}
                </Space>
                {withCases ? (
                    <TestCaseSelector
                        loadCase={loadCase}
                        loading={casesLoading}
                        onChange={pickCases}
                        onPageChange={setPage}
                        page={page}
                        pageSize={TEST_CASES_PAGE_SIZE}
                        selection={caseSelection}
                        tableId={detail.tableId}
                        testCases={casesPage?.content ?? []}
                        total={casesPage?.total ?? 0}
                        value={caseIds}
                    />
                ) : (
                    <ParametersInput
                        onChange={setParameters}
                        parameters={declaredParameters}
                        runtimeContext={input.runtimeContext}
                    />
                )}
                {error && <Alert showIcon data-testid="launch-error" title={error} type="error" />}
            </Space>
        </TableInputPopover>
    )
}

export default TableInputLauncher
