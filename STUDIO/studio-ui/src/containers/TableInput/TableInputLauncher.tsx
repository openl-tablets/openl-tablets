import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Alert, Button, Checkbox, Input, Space, Tooltip, Typography } from 'antd'
import { FieldRow } from 'components/FieldRow'
import { carriesCases } from 'constants/tableKinds'
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
import { type CaseSelection, EVERY_CASE, isEveryCase, TestCaseSelector } from './TestCaseSelector'
import { testRangesOf } from './testRanges'

/** What the table page sends to start an action on the table it shows. */
export interface TableLaunchDetail extends EventProjectDetail {
    tableId: string
    /** The kind of the table, as the page lists it: a Test or Run table carries cases, any other takes an input. */
    kind: string
    moduleName: string
    /** Viewport rectangle of the button the panel hangs under. */
    anchor: PopoverAnchor
    /** Only the current module can be used. The project is still loading, or another module has errors. */
    moduleOnlyLocked?: boolean
}

/** A button under the panel that starts the action with what the panel holds. */
export interface TableLaunchAction {
    /** Names the button for the tests of the screen. */
    key: string
    label: React.ReactNode
    primary?: boolean
    loading?: boolean
    disabled?: boolean
    /** Started with what the panel collected. When the panel is not ready it says why itself and starts nothing. */
    run: (value: TableLaunchValue) => void
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
    options?: React.ReactNode
    /**
     * The buttons that start the action, right-aligned under the panel.
     *
     * A button starts its action with what the panel holds - later, when the cases to run are every page of the
     * table but some: the rest are read first. While they are read, the buttons wait under a spinner.
     */
    actions: TableLaunchAction[]
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
    // Whether what the panel holds is still being collected - the rest of the cases read - so that neither the
    // cases nor the buttons take a click that would start the action twice or change what it is given.
    const [collecting, setCollecting] = useState(false)
    // A read that lands once the panel is closed starts nothing: the reader has left.
    const open = useRef(true)
    useEffect(() => () => {
        open.current = false
    }, [])
    const [page, setPage] = useState(1)
    // An action that takes several cases starts with every case ticked; one that takes a single case is offered
    // the first of the table once it is read.
    const [cases, setCases] = useState<CaseSelection>(caseSelection === 'multiple' ? EVERY_CASE : [])
    // The cases can be named by a range of ids instead of ticked one by one, as the legacy editor offered.
    const [useRange, setUseRange] = useState(false)
    const [range, setRange] = useState('')
    const [parameters, setParameters] = useState<ParametersInputValue>({ inputJson: '{}' })

    // The table is described the way it will be run: within the current module when that is what is asked
    // for, and always so while the project is still loading.
    const fromModule = moduleOnly ? detail.moduleName : undefined
    const readWithin = useMemo(() => (fromModule ? { fromModule } : {}), [fromModule])
    const testTable = carriesCases(detail.kind)
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
                        setCases(current => (Array.isArray(current) && current.length > 0
                            ? current
                            : [loaded.content[0]?.id ?? ''].filter(Boolean)))
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

    // The cases picked, named by ids and ranges. Every case but some are the rest of every page of the table,
    // read in one go unless the page at hand holds them all.
    const pickedCases = async (): Promise<string> => {
        if (!isEveryCase(cases)) {
            return cases.join(',')
        }
        const total = casesPage?.total ?? 0
        const all = casesPage && casesPage.content.length >= total
            ? casesPage
            : await getTableInputCases(project.id, detail.tableId, { ...readWithin, page: 0, size: total })
        const left = new Set(cases.except)
        return testRangesOf(all.content.map(testCase => testCase.id), id => !left.has(id))
    }

    const collectCases = async (): Promise<TableLaunchValue | null> => {
        if (useRange) {
            const testRanges = range.trim()
            if (!testRanges) {
                onError(t('testCases.rangeRequired'))
                return null
            }
            return { testRanges, fromModule }
        }
        if (isEveryCase(cases) && cases.except.length === 0) {
            return { fromModule }
        }
        try {
            const testRanges = await pickedCases()
            if (!testRanges) {
                onError(caseSelection === 'single' ? t('testCases.noCase') : t('testCases.none'))
                return null
            }
            return { testRanges, fromModule }
        } catch (readError) {
            onError(errorMessage(readError))
            return null
        }
    }

    // What the panel holds, or null when it is not ready - the reason is shown under the panel.
    const collect = async (): Promise<TableLaunchValue | null> => {
        onError(null)
        if (withCases) {
            return collectCases()
        }
        if (parameters.error) {
            onError(parameters.error)
            return null
        }
        return { inputJson: parameters.inputJson, fromModule }
    }

    const launch = (action: (value: TableLaunchValue) => void) => {
        if (collecting) {
            return
        }
        setCollecting(true)
        collect().then(value => {
            if (!open.current) {
                return
            }
            setCollecting(false)
            if (value) {
                action(value)
            }
        })
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
            collecting={collecting}
            onClose={onClose}
            width={withCases ? 560 : 520}
            footer={actions.map(action => (
                <Button
                    key={action.key}
                    data-testid={action.key}
                    disabled={action.disabled ?? false}
                    loading={action.loading ?? false}
                    onClick={() => launch(action.run)}
                    type={action.primary ? 'primary' : 'default'}
                >
                    {action.label}
                </Button>
            ))}
        >
            <Space orientation="vertical" size="small" style={{ width: '100%' }}>
                <Space wrap size="middle">
                    {detail.moduleOnlyLocked
                        ? <Tooltip title={t('input.moduleOnlyLocked')}>{moduleOnlyOption}</Tooltip>
                        : moduleOnlyOption}
                    {options}
                    {withCases && caseSelection === 'multiple' && (
                        <Tooltip title={t('testCases.useRangeHint')}>
                            <Checkbox
                                checked={useRange}
                                data-testid="launch-use-range"
                                onChange={event => setUseRange(event.target.checked)}
                            >
                                {t('testCases.useRange')}
                            </Checkbox>
                        </Tooltip>
                    )}
                </Space>
                {withCases && useRange && (
                    <Space orientation="vertical" size={4} style={{ width: '100%' }}>
                        <FieldRow required label={t('testCases.range')}>
                            <Input
                                data-testid="launch-range"
                                onChange={event => setRange(event.target.value)}
                                placeholder={t('testCases.rangeHint')}
                                value={range}
                            />
                        </FieldRow>
                        <Typography.Text type="secondary">
                            {t('testCases.total', { count: casesPage?.total ?? 0 })}
                        </Typography.Text>
                    </Space>
                )}
                {withCases && !useRange && (
                    <TestCaseSelector
                        loadCase={loadCase}
                        loading={casesLoading || collecting}
                        onChange={setCases}
                        onPageChange={setPage}
                        page={page}
                        pageSize={TEST_CASES_PAGE_SIZE}
                        selection={caseSelection}
                        tableId={detail.tableId}
                        testCases={casesPage?.content ?? []}
                        total={casesPage?.total ?? 0}
                        value={cases}
                    />
                )}
                {!withCases && (
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
