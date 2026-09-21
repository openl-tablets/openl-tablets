import React, { useMemo } from 'react'
import { Checkbox, Flex, Pagination, Radio } from 'antd'
import { useTranslation } from 'react-i18next'
import { ListTable, type ListTableColumn } from 'components/ListTable'
import { ParameterValueList } from 'components/values/ParameterValues'
import { useTestCase, type ReadTestCase } from 'hooks/useTestCase'
import type { TableInputTestCase } from 'types/tables'
import type { TraceParameterValue } from 'types/trace'

export interface TestCaseSelectorProps {
    /** The test table the cases belong to. */
    tableId: string
    /** Whether one case is picked at a time, or several. */
    selection: 'single' | 'multiple'
    /** The cases of the page shown. */
    testCases: TableInputTestCase[]
    /** How many cases the table holds in all. */
    total: number
    /** The page shown, counted from 1 the way the pager counts it. */
    page: number
    pageSize: number
    /** Whether the page is on its way. */
    loading?: boolean | undefined
    onPageChange: (page: number) => void
    /**
     * The chosen cases: every case of the table but the ones left out, or the ids of some. Both stand while
     * the user looks through the other pages.
     */
    value: CaseSelection
    onChange: (selection: CaseSelection) => void
    /** Reads one case with every value, for the values a page only refers to. */
    loadCase: ReadTestCase<TableInputTestCase>
}

/** Every case of the table but the ids left out, or the ids of the chosen ones. */
export type CaseSelection = { except: string[] } | string[]

/** Every case of the table. */
export const EVERY_CASE: CaseSelection = { except: []}

/** Whether the selection stands for every case of the table, less the ones left out. */
export const isEveryCase = (selection: CaseSelection): selection is { except: string[] } => !Array.isArray(selection)

interface CaseRow {
    id: string
    description: string | null | undefined
    parameters: TraceParameterValue[]
}

/**
 * The value of one column of a case, labelled by the column.
 *
 * A case names its columns by the header the table author wrote, so the header is the label. The declared type
 * is left out: a case is picked, not edited.
 */
const columnValue = (parameter: TraceParameterValue): TraceParameterValue => ({
    ...parameter,
    name: parameter.description || parameter.name,
    description: '',
})

const toggled = (ids: string[], caseId: string): string[] => (ids.includes(caseId)
    ? ids.filter(id => id !== caseId)
    : [...ids, caseId])

/**
 * The cases picked after one of them is clicked: the only one, or one more (or one less) of several.
 *
 * Every case is picked at first, on every page; a case clicked then is left out of all of them, and the
 * rest stay picked, the way the legacy list of ticked cases did.
 */
const pickOne = (picked: CaseSelection, caseId: string, selection: 'single' | 'multiple'): CaseSelection => {
    if (selection === 'single') {
        return [caseId]
    }
    return isEveryCase(picked) ? { except: toggled(picked.except, caseId) } : toggled(picked, caseId)
}

/**
 * Picks the cases of a test table to execute.
 *
 * The cases are listed by id, one row each, and the row's values read the way the trace window shows the values
 * of a step. A value with inner structure is shown only when the user asks for it, and each value is shown on
 * its own: asking for one reads its case, and the values of that case that are asked for later are already at
 * hand.
 *
 * An action that runs one case takes one; an action that runs several takes as many as are ticked. What is
 * picked stays picked while the user pages through the rest.
 */
export const TestCaseSelector: React.FC<TestCaseSelectorProps> = ({
    tableId,
    selection,
    testCases,
    total,
    page,
    pageSize,
    loading,
    onPageChange,
    value,
    onChange,
    loadCase,
}) => {
    const { t } = useTranslation('execution')
    const readCase = useTestCase(loadCase)

    const rows = useMemo((): CaseRow[] => testCases.map(testCase => ({
        id: testCase.id,
        description: testCase.description,
        parameters: testCase.parameters.map(columnValue),
    })), [testCases])

    const picked = (row: CaseRow) => (isEveryCase(value) ? !value.except.includes(row.id) : value.includes(row.id))
    const everyCase = isEveryCase(value) && value.except.length === 0
    // Every case left out one by one is no case at all.
    const someCases = isEveryCase(value) ? value.except.length < total : value.length > 0

    const columns: ListTableColumn<CaseRow>[] = [
        {
            key: 'pick',
            fit: true,
            align: 'center',
            // The box in the header stands for every case of the table, on this page and the others.
            title: selection === 'multiple' && (
                <Checkbox
                    checked={everyCase}
                    data-testid="pick-all-cases"
                    indeterminate={!everyCase && someCases}
                    onChange={event => onChange(event.target.checked ? EVERY_CASE : [])}
                />
            ),
            // The row itself picks the case, so a click on the box must not pick it a second time.
            render: (row: CaseRow) => (
                <>
                    {selection === 'single'
                        ? (
                            <Radio
                                checked={picked(row)}
                                data-testid={`pick-case-${row.id}`}
                                name="test-case"
                                onChange={() => onChange([row.id])}
                                onClick={event => event.stopPropagation()}
                            />
                        )
                        : (
                            <Checkbox
                                checked={picked(row)}
                                data-testid={`pick-case-${row.id}`}
                                onChange={() => onChange(pickOne(value, row.id, selection))}
                                onClick={event => event.stopPropagation()}
                            />
                        )}
                </>
            ),
        },
        { key: 'id', fit: true, title: t('testCases.id'), render: (row: CaseRow) => row.id },
        ...(rows.some(row => row.description)
            ? [{
                key: 'description',
                fit: true,
                title: t('testCases.description'),
                render: (row: CaseRow) => row.description,
            } as ListTableColumn<CaseRow>]
            : []),
        {
            key: 'parameters',
            title: t('testCases.title'),
            render: (row: CaseRow) => (
                // Reading a value, or opening one, is not a way of picking the case: the click stops here.
                <div onClick={event => event.stopPropagation()} role="presentation">
                    <ParameterValueList
                        keyPrefix={`case-${row.id}`}
                        onLoad={index => readCase(tableId, row.id).then(read => read.parameters[index])}
                        parameters={row.parameters}
                    />
                </div>
            ),
        },
    ]

    return (
        <Flex vertical gap="small">
            <ListTable<CaseRow>
                columns={columns}
                data-testid="test-cases"
                loading={loading ?? false}
                maxHeight="40vh"
                onRowClick={row => onChange(pickOne(value, row.id, selection))}
                rowKey={row => row.id}
                rows={rows}
                selected={picked}
            />
            {total > pageSize && (
                <Pagination
                    align="end"
                    current={page}
                    onChange={onPageChange}
                    pageSize={pageSize}
                    showSizeChanger={false}
                    showTotal={count => t('testCases.total', { count })}
                    size="small"
                    total={total}
                />
            )}
        </Flex>
    )
}

export default TestCaseSelector
