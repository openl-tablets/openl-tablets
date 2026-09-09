import React, { useCallback, useMemo, useRef } from 'react'
import { Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useTranslation } from 'react-i18next'
import { ParameterValueList } from 'components/values/ParameterValues'
import type { TableInputTestCase } from 'types/tables'
import type { TraceParameterValue } from 'types/trace'

export interface TestCaseSelectorProps {
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
    /** Id of the chosen case. It stays chosen while the user looks through the other pages. */
    value: string
    onChange: (caseId: string) => void
    /** Reads one case with every value, for the values a page only refers to. */
    loadCase: (caseId: string) => Promise<TableInputTestCase>
}

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

/**
 * Picks the one case of a test table to execute.
 *
 * The cases are listed by id, one row each, and the row's values read the way the trace window shows the values
 * of a step. A value with inner structure is shown only when the user asks for it, and each value is shown on
 * its own: asking for one reads its case, and the values of that case that are asked for later are already at
 * hand.
 *
 * The chosen case stays chosen while the user pages through the rest.
 */
export const TestCaseSelector: React.FC<TestCaseSelectorProps> = ({
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
    const readCases = useRef<Record<string, Promise<TableInputTestCase>>>({})

    const load = useCallback(async (caseId: string, index: number): Promise<TraceParameterValue | undefined> => {
        const reading = readCases.current[caseId] ?? loadCase(caseId).catch(error => {
            // A case that could not be read is read again on the next request.
            delete readCases.current[caseId]
            throw error
        })
        readCases.current[caseId] = reading
        const parameter = (await reading).parameters[index]
        return parameter && columnValue(parameter)
    }, [loadCase])

    const rows = useMemo((): CaseRow[] => testCases.map(testCase => ({
        id: testCase.id,
        description: testCase.description,
        parameters: testCase.parameters.map(columnValue),
    })), [testCases])

    const columns = useMemo((): ColumnsType<CaseRow> => [
        { title: t('testCases.id'), dataIndex: 'id', key: 'id', width: 60 },
        ...(rows.some(row => row.description)
            ? [{ title: t('testCases.description'), dataIndex: 'description', key: 'description', width: 160 }]
            : []),
        {
            title: t('testCases.title'),
            key: 'parameters',
            render: (_: unknown, row: CaseRow) => (
                // Reading a value, or opening one, is not a way of picking the case: the click stops here.
                <div onClick={event => event.stopPropagation()} role="presentation">
                    <ParameterValueList
                        keyPrefix={`case-${row.id}`}
                        onLoad={index => load(row.id, index)}
                        parameters={row.parameters}
                    />
                </div>
            ),
        },
    ], [rows, t, load])

    return (
        <Table<CaseRow>
            bordered
            columns={columns}
            data-testid="test-cases"
            dataSource={rows}
            loading={loading ?? false}
            onRow={row => ({ onClick: () => onChange(row.id) })}
            rowKey="id"
            scroll={{ y: '40vh' }}
            size="small"
            pagination={total > pageSize && {
                current: page,
                pageSize,
                total,
                size: 'small',
                showSizeChanger: false,
                showTotal: count => t('testCases.total', { count }),
                onChange: onPageChange,
            }}
            rowSelection={{
                type: 'radio',
                selectedRowKeys: value ? [value] : [],
                renderCell: (_checked, row, _index, node) => (
                    <span data-testid={`pick-case-${row.id}`}>{node}</span>
                ),
                onChange: keys => onChange(String(keys[0] ?? '')),
            }}
        />
    )
}

export default TestCaseSelector
