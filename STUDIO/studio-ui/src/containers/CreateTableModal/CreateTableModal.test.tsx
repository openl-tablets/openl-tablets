import React from 'react'
import { act, render, screen, waitFor, within } from '@testing-library/react'
import userEvent, { type UserEvent } from '@testing-library/user-event'
import { notification } from 'antd'
import type { MockedFunction } from 'vitest'
import { getModuleSheets, getProjectModules, getProjectProperties } from 'services/projects'
import { createTable, getDatatype, getProjectTables } from 'services/tables'
import type { CreateTableRequest, ProjectDatatype, ProjectProperty, ProjectTable } from 'types/tables'
import { defaultModulePath } from '../tableModals/shared'
import { CreateTableModal, type CreateTableModalDetail } from './CreateTableModal'
import { SIMPLE_TYPES } from './tableSkeletons'

vi.mock('services/projects', () => ({
    getProjectModules: vi.fn(),
    getProjectProperties: vi.fn(),
    getModuleSheets: vi.fn(),
}))

vi.mock('services/tables', () => ({
    createTable: vi.fn(),
    getProjectTables: vi.fn(),
    getDatatype: vi.fn(),
}))

vi.mock('antd', async () => {
    const actual = await vi.importActual<typeof import('antd')>('antd')
    const MockModal = ({
        open,
        title,
        children,
        okText,
        cancelText,
        onOk,
        onCancel,
        okButtonProps,
    }: {
        open?: boolean
        title?: React.ReactNode
        children?: React.ReactNode
        okText?: React.ReactNode
        cancelText?: React.ReactNode
        onOk?: () => void
        onCancel?: () => void
        okButtonProps?: { disabled?: boolean, loading?: boolean }
    }) => open ? (
        <div role="dialog">
            <div>{title}</div>
            {children}
            <button onClick={onCancel}>{cancelText}</button>
            <button disabled={okButtonProps?.disabled} onClick={onOk}>{okText}</button>
        </div>
    ) : null
    const MockSelect = ({
        id,
        value,
        options,
        onChange,
        onOpenChange,
        allowClear,
        mode,
        showSearch,
        ...props
    }: {
        id?: string
        value?: string | string[] | null
        options?: { label: React.ReactNode, value: string }[]
        onChange?: (value: string | string[]) => void
        onOpenChange?: (open: boolean) => void
        allowClear?: boolean
        mode?: 'multiple'
        showSearch?: boolean
        'aria-label'?: string
        'data-testid'?: string
    }) => (
        <select
            aria-label={props['aria-label']}
            className={mode === 'multiple' ? 'ant-select ant-select-multiple' : 'ant-select'}
            data-mode={mode ?? 'single'}
            data-searchable={String(Boolean(showSearch))}
            data-testid={props['data-testid']}
            id={id}
            multiple={mode === 'multiple'}
            // A native select shows its options as soon as it is reached, which is what the real one reports here.
            onFocus={() => onOpenChange?.(true)}
            value={value ?? ''}
            onChange={event => onChange?.(mode === 'multiple'
                ? Array.from(event.target.selectedOptions, option => option.value)
                : event.target.value)}
        >
            {allowClear || !value ? <option value="" /> : null}
            {options?.map(option => (
                <option key={option.value} value={option.value}>{option.label}</option>
            ))}
        </select>
    )
    const MockAutoComplete = ({
        id,
        value,
        options,
        onChange,
        placeholder,
        ...props
    }: {
        id?: string
        value?: string
        options?: { label?: React.ReactNode, value?: string, options?: { value: string }[] }[]
        onChange?: (value: string) => void
        placeholder?: string
        'data-testid'?: string
        'aria-label'?: string
    }) => (
        <>
            <input
                aria-label={props['aria-label']}
                data-testid={props['data-testid']}
                id={id}
                list={`${id}-options`}
                onChange={event => onChange?.(event.target.value)}
                placeholder={placeholder}
                value={value ?? ''}
            />
            <datalist data-testid={`${props['data-testid']}-options`} id={`${id}-options`}>
                {options?.flatMap(option => option.options ?? [option]).map(option => (
                    <option key={option.value} value={option.value} />
                ))}
            </datalist>
        </>
    )
    const MockSpin = ({ children }: { children?: React.ReactNode }) => <>{children}</>
    const MockTooltip = ({ children }: { children?: React.ReactNode }) => <>{children}</>
    const MockDatePicker = ({
        onChange,
        ...props
    }: {
        onChange?: (date: { format: (pattern: string) => string } | null) => void
        'data-testid'?: string
    }) => (
        <input
            data-testid={props['data-testid']}
            type="date"
            onChange={event => onChange?.({
                format: pattern => {
                    if (pattern === 'YYYY-MM-DD') {
                        return event.target.value
                    }
                    if (pattern === 'MM/DD/YYYY') {
                        const [year, month, day] = event.target.value.split('-')
                        return `${month}/${day}/${year}`
                    }
                    return `unexpected:${pattern}`
                },
            })}
        />
    )
    const MockInputNumber = ({
        onChange,
        value,
        ...props
    }: {
        onChange?: (value: string | null) => void
        value?: string | null
        'data-testid'?: string
        'aria-label'?: string
    }) => (
        <input
            aria-label={props['aria-label']}
            data-testid={props['data-testid']}
            onChange={event => onChange?.(event.target.value || null)}
            type="number"
            value={value ?? ''}
        />
    )
    return {
        ...actual,
        AutoComplete: MockAutoComplete,
        DatePicker: MockDatePicker,
        InputNumber: MockInputNumber,
        Modal: MockModal,
        Select: MockSelect,
        Spin: MockSpin,
        Tooltip: MockTooltip,
        // The static API renders through a portal of its own and warns that it sees no theme context.
        notification: { error: vi.fn() },
    }
})

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

const projectTypes: ProjectTable[] = [
    { id: 'country-id', tableType: 'Vocabulary', name: 'Country' },
    { id: 'customer-id', tableType: 'Datatype', name: 'Customer' },
]

// What a test can call. The signature is what a Test or Run table is generated from, so the list carries it.
const executableTables: ProjectTable[] = [{
    id: 'source-table-id',
    tableType: 'SimpleRules',
    name: 'Eligibility',
    returnType: 'Boolean',
    signature: 'Eligibility(Integer age, String country)',
}]

const projectProperties: ProjectProperty[] = [
    { name: 'active', type: 'boolean', multiple: false, values: []},
    { name: 'category', type: 'text', multiple: false, values: []},
    { name: 'scope', type: 'text', multiple: false, values: []},
    {
        name: 'state',
        type: 'enum',
        multiple: false,
        values: [{ code: 'AL', value: 'Alabama' }, { code: 'AK', value: 'Alaska' }],
    },
    {
        name: 'effectiveDate',
        type: 'date',
        multiple: false,
        values: [],
    },
    {
        name: 'country',
        type: 'enum',
        multiple: true,
        values: [
            { code: 'AE', value: 'United Arab Emirates' },
            { code: 'AL', value: 'Albania' },
            { code: 'AR', value: 'Argentina' },
        ],
    },
]

const mockGetModules = getProjectModules as MockedFunction<typeof getProjectModules>
const mockGetTables = getProjectTables as MockedFunction<typeof getProjectTables>
const mockGetDatatype = getDatatype as MockedFunction<typeof getDatatype>
const mockGetProperties = getProjectProperties as MockedFunction<typeof getProjectProperties>
const mockGetSheets = getModuleSheets as MockedFunction<typeof getModuleSheets>
const mockCreateTable = createTable as MockedFunction<typeof createTable>
const mockNotifyError = notification.error as MockedFunction<typeof notification.error>

const openModal = async (detail: Partial<CreateTableModalDetail> = {}) => {
    const onSuccess = detail.onSuccess ?? vi.fn()
    await act(async () => {
        window.dispatchEvent(new CustomEvent<CreateTableModalDetail>('openCreateTableModal', {
            detail: {
                projectId: 'project-id',
                currentModuleName: 'Main',
                ...detail,
                onSuccess,
            },
        }))
    })
    return onSuccess
}

const createButton = () => screen.getByRole('button', { name: 'project:create_table_modal.create' })
const enterTableName = (user: UserEvent, name = 'NewTable') =>
    user.type(screen.getByTestId('create-table-name'), name)

describe('CreateTableModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        mockGetModules.mockResolvedValue([
            { name: 'Main', path: 'rules/Main.xlsx' },
            { name: 'Pricing', path: 'rules/Pricing.xlsx' },
        ])
        // The tables list answers by kind: the datatypes of the project, or what a test can call.
        mockGetTables.mockImplementation(async (_projectId, kinds) => kinds.includes('Datatype')
            ? projectTypes
            : executableTables)
        // The datatypes list holds a datatype and a vocabulary; `GET /tables/{id}` answers for whichever is read.
        mockGetDatatype.mockImplementation(async (_projectId, tableId) => tableId === 'country-id'
            ? { fields: [], values: ['USA', 'Canada']}
            : { fields: [{ name: 'name', type: 'String' }, { name: 'age', type: 'Integer' }], values: []})
        mockGetProperties.mockResolvedValue(projectProperties)
        mockGetSheets.mockResolvedValue(['Main', 'Rates'])
        mockCreateTable.mockResolvedValue({
            id: 'table-id',
            tableType: 'Datatype',
            kind: 'Datatype',
            name: 'NewTable',
        })
    })

    it('offers only the supported table types and renders Datatype immediately', async () => {
        render(<CreateTableModal />)
        await openModal()

        await waitFor(() => expect(screen.getByTestId('create-table-module')).toHaveValue('Main'))
        const typeSelector = screen.getByTestId('create-table-type')
        expect(within(typeSelector).getAllByRole('option').map(option => option.getAttribute('value'))).toEqual([
            'datatype',
            'vocabulary',
            'constants',
            'spreadsheet',
            'smartRules',
            'simpleRules',
            'smartLookup',
            'simpleLookup',
            'rules',
            'test',
            'run',
            'data',
            'environment',
            'properties',
            'freeForm',
        ])
        expect(screen.getByTestId('create-table-name')).toHaveValue('')
        expect(screen.getByTestId('create-table-header')).toHaveTextContent('Datatype')
        expect(screen.getByTestId('create-table-header')).not.toHaveTextContent('NewTable')
        expect(createButton()).toBeDisabled()
        expect(screen.getByTestId('create-table-extends')).toHaveValue('')
        expect(screen.getByTestId('create-table-skeleton'))
            .toHaveTextContent('TypeNameDefault ValueMandatoryDescriptionExamples')
        expect(getComputedStyle(screen.getByTestId('create-table-cell-0-1').closest('td')!).minWidth).toBe('150px')
        expect(getComputedStyle(screen.getByTestId('create-table-cell-0-2').closest('td')!).minWidth).toBe('110px')
        expect(screen.getByTestId('create-table-cell-0-3')).toHaveAttribute('type', 'checkbox')
        expect(screen.getByTestId('create-table-cell-0-3')).not.toBeChecked()
        expect(getComputedStyle(screen.getByTestId('create-table-cell-0-5').closest('td')!).minWidth).toBe('110px')
    })

    it('adds an empty row automatically and creates the edited Datatype', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        const onSuccess = await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-module')).toHaveValue('Main'))

        // The grid opens on an example row, and the row below it is already waiting.
        expect(screen.getByTestId('create-table-cell-0-0')).toHaveValue('String')
        expect(screen.getByTestId('create-table-cell-1-0')).toBeInTheDocument()
        await user.clear(screen.getByTestId('create-table-cell-0-1'))
        await user.type(screen.getByTestId('create-table-cell-0-1'), 'customerName')
        await user.click(screen.getByTestId('create-table-cell-0-3'))
        await user.type(screen.getByTestId('create-table-cell-1-0'), 'String')
        await user.type(screen.getByTestId('create-table-cell-1-1'), 'optionalName')
        await user.clear(screen.getByTestId('create-table-name'))
        await user.type(screen.getByTestId('create-table-name'), 'Customer')
        await user.click(createButton())

        await waitFor(() => expect(mockCreateTable).toHaveBeenCalledTimes(1))
        const request = mockCreateTable.mock.calls[0]![1]
        expect(request).toMatchObject({
            moduleName: 'Main',
            // The table goes into a sheet the module already has, not into one named after itself.
            sheetName: 'Main',
            table: {
                tableType: 'RawSource',
                kind: 'Datatype',
                name: 'Customer',
            },
        })
        expect(request).not.toHaveProperty('modulePath')
        expect(request.table.source[0]).toEqual([
            { value: 'Datatype Customer', colspan: 6 },
            { value: null, covered: true },
            { value: null, covered: true },
            { value: null, covered: true },
            { value: null, covered: true },
            { value: null, covered: true },
        ])
        expect(request.table.source[1]?.map(cell => cell.value))
            .toEqual(['Type', 'Name', 'Default', 'Mandatory', 'Description', 'Example'])
        expect(request.table.source[2]?.map(cell => cell.value))
            .toEqual(['String', 'customerName', null, true, null, null])
        expect(request.table.source[3]?.map(cell => cell.value))
            .toEqual(['String', 'optionalName', null, null, null, null])
        expect(onSuccess).toHaveBeenCalledWith(expect.objectContaining({ id: 'table-id' }), 'Main')
    })

    it('extends a Datatype from a suggested complex type only', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-module')).toHaveValue('Main'))

        const options = within(screen.getByTestId('create-table-extends-options'))
            .getAllByRole('option', { hidden: true })
            .map(option => option.getAttribute('value'))
        expect(options).toEqual(['Customer'])
        expect(options).not.toContain('Country')
        expect(options).not.toContain('SpreadsheetResult')

        await enterTableName(user)
        await user.type(screen.getByTestId('create-table-extends'), 'Customer')
        expect(screen.getByTestId('create-table-header')).toHaveTextContent('Datatype NewTable extends Customer')
        await user.click(createButton())

        await waitFor(() => expect(mockCreateTable).toHaveBeenCalledTimes(1))
        expect(mockCreateTable.mock.calls[0]![1].table.source[0]?.[0]?.value)
            .toBe('Datatype NewTable extends Customer')
    })

    it('offers an empty, TRUE, or FALSE default for Boolean Datatype and Constants fields', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-module')).toHaveValue('Main'))

        await user.clear(screen.getByTestId('create-table-cell-0-0'))
        await user.type(screen.getByTestId('create-table-cell-0-0'), 'Boolean')
        expect(within(screen.getByTestId('create-table-cell-0-2')).getAllByRole('option')
            .map(option => option.getAttribute('value'))).toEqual(['', 'TRUE', 'FALSE'])
        await user.selectOptions(screen.getByTestId('create-table-cell-0-2'), 'FALSE')

        await user.selectOptions(screen.getByTestId('create-table-type'), 'constants')
        await user.clear(screen.getByTestId('create-table-cell-0-0'))
        await user.type(screen.getByTestId('create-table-cell-0-0'), 'Boolean')
        expect(within(screen.getByTestId('create-table-cell-0-2')).getAllByRole('option')
            .map(option => option.getAttribute('value'))).toEqual(['', 'TRUE', 'FALSE'])
        await user.selectOptions(screen.getByTestId('create-table-cell-0-2'), 'FALSE')
        await user.click(createButton())

        await waitFor(() => expect(mockCreateTable).toHaveBeenCalledTimes(1))
        expect(mockCreateTable.mock.calls[0]![1].table.source.map(row => row.map(cell => cell.value))).toEqual([
            ['Constants', null, null],
            ['Boolean', 'CONSTANT1', 'FALSE'],
        ])
    })

    it('restricts Vocabulary values to the selected Base Type', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-module')).toHaveValue('Main'))

        await user.selectOptions(screen.getByTestId('create-table-type'), 'vocabulary')
        const baseType = screen.getByTestId('create-table-vocabulary-type')
        expect(within(baseType).getAllByRole('option').map(option => option.getAttribute('value')))
            .toEqual(SIMPLE_TYPES)
        await user.selectOptions(baseType, 'Boolean')

        const value = screen.getByTestId('create-table-cell-0-0')
        expect(value).toHaveValue('TRUE')
        expect(within(value).getAllByRole('option').map(option => option.getAttribute('value')))
            .toEqual(['', 'TRUE', 'FALSE'])
    })

    it('builds Smart Rules columns from input arguments and result type', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-type')).toHaveValue('datatype'))

        await enterTableName(user)
        await user.selectOptions(screen.getByTestId('create-table-type'), 'smartRules')
        expect(getComputedStyle(screen.getByTestId('create-table-result-type-row')).gridTemplateColumns)
            .toBe(getComputedStyle(screen.getByTestId('create-table-argument-row-0')).gridTemplateColumns)
        await user.clear(screen.getByTestId('create-table-result-type'))
        await user.type(screen.getByTestId('create-table-result-type'), 'Integer')
        await user.type(screen.getByTestId('create-table-argument-type-0'), 'Integer')
        await user.type(screen.getByTestId('create-table-argument-name-0'), 'age')

        expect(screen.getByTestId('create-table-argument-type-1')).toBeInTheDocument()
        expect(screen.getByTestId('create-table-header'))
            .toHaveTextContent('SmartRules Integer NewTable(Integer age)')
        expect(screen.getByTestId('create-table-skeleton')).toHaveTextContent('ageOutput')

        await user.clear(screen.getByTestId('create-table-result-type'))
        await user.type(screen.getByTestId('create-table-result-type'), 'Customer')
        expect(screen.getByTestId('create-table-cell-0-2')).toBeInTheDocument()

        await user.type(screen.getByTestId('create-table-cell-0-0'), '18')
        expect(screen.getByTestId('create-table-cell-1-0')).toBeInTheDocument()
    })

    it('loads a vocabulary when a complete Smart Rules argument changes type', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-type')).toHaveValue('datatype'))

        await enterTableName(user)
        await user.selectOptions(screen.getByTestId('create-table-type'), 'smartRules')
        await user.type(screen.getByTestId('create-table-argument-type-0'), 'String')
        await user.type(screen.getByTestId('create-table-argument-name-0'), 'country')
        await user.clear(screen.getByTestId('create-table-argument-type-0'))
        await user.type(screen.getByTestId('create-table-argument-type-0'), 'Country')

        await waitFor(() => expect(mockGetDatatype).toHaveBeenCalledWith('project-id', 'country-id'))
        const country = screen.getByTestId('create-table-cell-0-0')
        expect(within(country).getAllByRole('option').map(option => option.getAttribute('value')))
            .toEqual(['', 'USA', 'Canada'])
        expect(createButton()).toBeDisabled()
    })

    it('loads a vocabulary selected as the Rules result type', async () => {
        let resolveCountry: ((value: ProjectDatatype) => void) | undefined
        mockGetDatatype.mockImplementationOnce(() => new Promise<ProjectDatatype>(resolve => {
            resolveCountry = resolve
        }))
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-type')).toHaveValue('datatype'))

        await user.selectOptions(screen.getByTestId('create-table-type'), 'rules')
        await user.clear(screen.getByTestId('create-table-result-type'))
        await user.type(screen.getByTestId('create-table-result-type'), 'Country')

        await waitFor(() => expect(mockGetDatatype).toHaveBeenCalledWith('project-id', 'country-id'))
        expect(within(screen.getByTestId('create-table-cell-0-1')).getAllByRole('option')
            .map(option => option.getAttribute('value'))).toEqual([''])
        expect(createButton()).toBeDisabled()

        await act(async () => {
            resolveCountry?.({ fields: [], values: ['USA', 'Canada']})
        })

        expect(within(screen.getByTestId('create-table-cell-0-1')).getAllByRole('option')
            .map(option => option.getAttribute('value')))
            .toEqual(['', 'USA', 'Canada'])
        expect(createButton()).toBeDisabled()
    })

    it('does not submit stale cells hidden by a free-text result type', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-type')).toHaveValue('datatype'))

        await enterTableName(user)
        await user.selectOptions(screen.getByTestId('create-table-type'), 'smartRules')
        await user.clear(screen.getByTestId('create-table-result-type'))
        await user.type(screen.getByTestId('create-table-result-type'), 'Customer')
        expect(await screen.findByTestId('create-table-cell-0-1')).toBeInTheDocument()

        await user.clear(screen.getByTestId('create-table-result-type'))
        await user.type(screen.getByTestId('create-table-result-type'), 'ExternalResult')
        expect(screen.queryByTestId('create-table-cell-0-1')).not.toBeInTheDocument()
        await user.click(createButton())

        await waitFor(() => expect(mockCreateTable).toHaveBeenCalledTimes(1))
        expect(mockCreateTable.mock.calls[0]![1].table.source.map(row => row.map(cell => cell.value))).toEqual([
            ['SmartRules ExternalResult NewTable()'],
            ['Output'],
            ['TRUE'],
        ])
    })

    it('spreads Lookup arguments over both axes and merges the corner they meet in', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-type')).toHaveValue('datatype'))

        await enterTableName(user)
        await user.selectOptions(screen.getByTestId('create-table-type'), 'simpleLookup')
        await user.clear(screen.getByTestId('create-table-result-type'))
        await user.type(screen.getByTestId('create-table-result-type'), 'Integer')
        for (const [index, name] of ['make', 'year', 'area'].entries()) {
            await user.type(screen.getByTestId(`create-table-argument-type-${index}`), 'String')
            await user.type(screen.getByTestId(`create-table-argument-name-${index}`), name)
        }

        // Three arguments: the leading one titles the single key column, the other two take a row across the top.
        expect(within(screen.getByTestId('create-table-skeleton')).getByText('make'))
            .toHaveAttribute('rowspan', '2')
        expect(screen.queryByTestId('create-table-cell-0-0')).not.toBeInTheDocument()
        for (const [testid, value] of [
            ['create-table-cell-0-1', '2024'],
            ['create-table-cell-1-1', 'North'],
            ['create-table-cell-2-0', 'Audi'],
            ['create-table-cell-2-1', '10'],
        ]) {
            await user.clear(screen.getByTestId(testid!))
            await user.type(screen.getByTestId(testid!), value!)
        }

        await user.click(createButton())

        await waitFor(() => expect(mockCreateTable).toHaveBeenCalledTimes(1))
        expect(mockCreateTable.mock.calls[0]![1].table.source).toEqual([
            [{ value: 'SimpleLookup Integer NewTable(String make, String year, String area)', colspan: 2 },
                { value: null, covered: true }],
            // The title is merged down the whole band, which is how OpenL counts the arguments across the top.
            [{ value: 'make', rowspan: 2 }, { value: '2024' }],
            [{ value: null, covered: true }, { value: 'North' }],
            [{ value: 'Audi' }, { value: '10' }],
        ])
    })

    it('refuses a Lookup that has nothing to look up by', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-type')).toHaveValue('datatype'))

        await enterTableName(user)
        await user.selectOptions(screen.getByTestId('create-table-type'), 'simpleLookup')
        // One argument cannot span two axes.
        await user.type(screen.getByTestId('create-table-argument-type-0'), 'String')
        await user.type(screen.getByTestId('create-table-argument-name-0'), 'make')
        expect(createButton()).toBeDisabled()

        await user.type(screen.getByTestId('create-table-argument-type-1'), 'String')
        await user.type(screen.getByTestId('create-table-argument-name-1'), 'year')
        expect(createButton()).toBeEnabled()

        // Nothing across the top: the row would be dropped as blank and shorten the merged corner.
        await user.clear(screen.getByTestId('create-table-cell-0-1'))
        expect(createButton()).toBeDisabled()
        await user.type(screen.getByTestId('create-table-cell-0-1'), '2024')
        expect(createButton()).toBeEnabled()

        // Values across the top, but nothing to look them up by.
        await user.clear(screen.getByTestId('create-table-cell-1-0'))
        await user.selectOptions(screen.getByTestId('create-table-cell-1-1'), '')
        expect(createButton()).toBeDisabled()
    })

    it('reads a datatype\'s fields when a Data table needs them, and reads them once', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-type')).toHaveValue('datatype'))

        await user.selectOptions(screen.getByTestId('create-table-type'), 'data')

        // The tables list names the project's datatypes; the fields come from the datatype itself.
        await waitFor(() => expect(screen.getByTestId('create-table-skeleton')).toHaveTextContent('NameAge'))
        expect(mockGetDatatype).toHaveBeenCalledWith('project-id', 'customer-id')

        await user.selectOptions(screen.getByTestId('create-table-datatype'), 'Customer')

        // Already known: picking the same datatype again asks nobody.
        expect(mockGetDatatype).toHaveBeenCalledTimes(1)
    })

    it('does not apply Data fields after the author switched to another table type', async () => {
        let resolveCustomer: ((value: ProjectDatatype) => void) | undefined
        mockGetDatatype.mockImplementationOnce(() => new Promise<ProjectDatatype>(resolve => {
            resolveCustomer = resolve
        }))
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-type')).toHaveValue('datatype'))

        await user.selectOptions(screen.getByTestId('create-table-type'), 'data')
        await waitFor(() => expect(mockGetDatatype).toHaveBeenCalledWith('project-id', 'customer-id'))
        await user.selectOptions(screen.getByTestId('create-table-type'), 'constants')
        expect(screen.getByTestId('create-table-cell-0-0')).toHaveValue('Integer')

        await act(async () => {
            resolveCustomer?.({
                fields: [{ name: 'name', type: 'String' }, { name: 'age', type: 'Integer' }],
                values: [],
            })
        })

        expect(screen.getByTestId('create-table-type')).toHaveValue('constants')
        expect(screen.getByTestId('create-table-cell-0-0')).toHaveValue('Integer')
    })

    it('keeps the fields of the latest Data datatype when requests answer out of order', async () => {
        const types = [
            ...projectTypes,
            { id: 'policy-id', tableType: 'Datatype', name: 'Policy' },
        ]
        mockGetTables.mockImplementation(async (_projectId, kinds) => kinds.includes('Datatype')
            ? types
            : executableTables)
        let resolveCustomer: ((value: ProjectDatatype) => void) | undefined
        let resolvePolicy: ((value: ProjectDatatype) => void) | undefined
        mockGetDatatype.mockImplementation((_projectId, tableId) => new Promise<ProjectDatatype>(resolve => {
            if (tableId === 'customer-id') {
                resolveCustomer = resolve
            } else {
                resolvePolicy = resolve
            }
        }))
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-type')).toHaveValue('datatype'))

        await user.selectOptions(screen.getByTestId('create-table-type'), 'data')
        await waitFor(() => expect(mockGetDatatype).toHaveBeenCalledWith('project-id', 'customer-id'))
        await user.selectOptions(screen.getByTestId('create-table-datatype'), 'Policy')
        await waitFor(() => expect(mockGetDatatype).toHaveBeenCalledWith('project-id', 'policy-id'))
        await act(async () => {
            resolvePolicy?.({
                fields: [{ name: 'approved', type: 'Boolean' }],
                values: [],
            })
        })
        await waitFor(() => expect(screen.getByTestId('create-table-skeleton')).toHaveTextContent('Approved'))
        await user.selectOptions(screen.getByTestId('create-table-cell-0-0'), 'FALSE')

        await act(async () => {
            resolveCustomer?.({
                fields: [{ name: 'name', type: 'String' }, { name: 'age', type: 'Integer' }],
                values: [],
            })
        })

        expect(screen.getByTestId('create-table-datatype')).toHaveValue('Policy')
        expect(screen.getByTestId('create-table-cell-0-0')).toHaveValue('FALSE')
    })

    it('lets a Spreadsheet name its own columns and add more of them', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-type')).toHaveValue('datatype'))

        await enterTableName(user)
        await user.selectOptions(screen.getByTestId('create-table-type'), 'spreadsheet')

        // The column names are the first row of the table, so they are cells the author writes, not fixed titles.
        expect(screen.getByTestId('create-table-cell-0-0')).toHaveValue('Steps')
        expect(screen.getByTestId('create-table-cell-0-1')).toHaveValue('Formula')
        await user.type(screen.getByTestId('create-table-cell-0-2'), 'Comment')
        expect(screen.getByTestId('create-table-cell-0-3')).toBeInTheDocument()

        await user.click(createButton())

        await waitFor(() => expect(mockCreateTable).toHaveBeenCalledTimes(1))
        expect(mockCreateTable.mock.calls[0]![1].table.source.map(row => row.map(cell => cell.value))).toEqual([
            ['Spreadsheet SpreadsheetResult NewTable()', null, null],
            ['Steps', 'Formula', 'Comment'],
            ['Step1', '= 1', null],
        ])
    })

    it('adds a Free Form column automatically and allows explicit column insertion', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-type')).toHaveValue('datatype'))

        await user.selectOptions(screen.getByTestId('create-table-type'), 'freeForm')
        await user.clear(screen.getByTestId('create-table-cell-0-0'))
        await user.type(screen.getByTestId('create-table-cell-0-0'), 'A')

        expect(screen.getByTestId('create-table-cell-0-2')).toBeInTheDocument()
        await user.click(screen.getAllByRole('button', {
            name: 'project:create_table_modal.insert_column_right',
        })[0]!)
        expect(screen.getByTestId('create-table-cell-0-3')).toBeInTheDocument()
    })

    it('writes a Free Form table exactly as it stands, with no header of its own', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-type')).toHaveValue('datatype'))

        await user.selectOptions(screen.getByTestId('create-table-type'), 'freeForm')

        // A plain grid: no header cell, no name, and the columns named the way a sheet names them.
        expect(screen.queryByTestId('create-table-header')).not.toBeInTheDocument()
        expect(screen.queryByTestId('create-table-name')).not.toBeInTheDocument()
        const skeleton = screen.getByTestId('create-table-skeleton')
        expect(within(skeleton).getByText('A')).toBeInTheDocument()
        expect(within(skeleton).getByText('B')).toBeInTheDocument()
        expect(screen.getByTestId('create-table-cell-0-0')).toHaveValue('Cell 1')
        expect(createButton()).toBeEnabled()

        await user.click(createButton())

        await waitFor(() => expect(mockCreateTable).toHaveBeenCalledTimes(1))
        expect(mockCreateTable.mock.calls[0]![1].table).toMatchObject({
            tableType: 'RawSource',
            kind: 'Other',
            // OpenL names an unrecognized table after its first cell, so that is the name it is created under.
            name: 'Cell 1',
        })
        expect(mockCreateTable.mock.calls[0]![1].table.source.map(row => row.map(cell => cell.value)))
            .toEqual([['Cell 1', 'Cell 2']])
    })

    it('refuses a Free Form table whose first cell is blank', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-type')).toHaveValue('datatype'))

        await user.selectOptions(screen.getByTestId('create-table-type'), 'freeForm')
        // OpenL reads a table from its first cell; blank, there is nothing to read.
        await user.clear(screen.getByTestId('create-table-cell-0-0'))

        expect(createButton()).toBeDisabled()
    })

    it('opens Create Test with columns generated from the selected executable table', async () => {
        render(<CreateTableModal />)

        await openModal({ sourceTableId: 'source-table-id' })

        // A Test table opens named the way OpenL would name it, and the author is free to rename it.
        await waitFor(() => expect(screen.getByTestId('create-table-header'))
            .toHaveTextContent('Test Eligibility EligibilityTest'))
        expect(screen.getByTestId('create-table-name')).toHaveValue('EligibilityTest')
        expect(screen.getByTestId('create-table-type')).toHaveValue('test')
        // One column per argument the tested table declares, then the one its result is compared against.
        expect(screen.getByTestId('create-table-skeleton')).toHaveTextContent('AgeCountryResult')
        expect(screen.getByTestId('create-table-cell-0-2')).toBeInTheDocument()
        // Each of them opens on a value of its own type, the result included.
        expect(screen.getByTestId('create-table-cell-0-0')).toHaveValue(1)
        expect(screen.getByTestId('create-table-cell-0-1')).toHaveValue('Text1')
        expect(screen.getByTestId('create-table-cell-0-2')).toHaveValue('TRUE')
    })

    it('renders and writes Test tables in vertical format when Transposed is selected', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal({ sourceTableId: 'source-table-id' })
        await waitFor(() => expect(screen.getByTestId('create-table-type')).toHaveValue('test'))

        await user.click(screen.getByTestId('create-table-transposed'))

        const skeleton = screen.getByTestId('create-table-skeleton')
        expect(skeleton).toHaveTextContent('project:create_table_modal.field')
        expect(skeleton).toHaveTextContent('ageAge')
        expect(screen.getByTestId('create-table-cell-0-0')).toHaveValue(1)
        await user.click(createButton())

        await waitFor(() => expect(mockCreateTable).toHaveBeenCalledTimes(1))
        expect(mockCreateTable.mock.calls[0]![1].table.source.map(row => row.map(cell => cell.value))).toEqual([
            ['Test Eligibility EligibilityTest', null, null],
            ['age', 'Age', '1'],
            ['country', 'Country', 'Text1'],
            ['_res_', 'Result', 'TRUE'],
        ])
    })

    it('opens a vocabulary column on a value that vocabulary accepts', async () => {
        mockGetTables.mockImplementation(async (_projectId, kinds) => kinds.includes('Datatype')
            ? projectTypes
            : [{
                id: 'rate-id',
                tableType: 'SimpleRules',
                name: 'Rate',
                returnType: 'Double',
                signature: 'Rate(Country origin)',
            }])
        render(<CreateTableModal />)

        await openModal({ sourceTableId: 'rate-id' })

        // A vocabulary declares values rather than fields, so its column is one cell — holding one of those values.
        await waitFor(() => expect(screen.getByTestId('create-table-cell-0-0')).toHaveValue('USA'))
        expect(mockGetDatatype).toHaveBeenCalledWith('project-id', 'country-id')
        expect(screen.getByTestId('create-table-skeleton')).toHaveTextContent('OriginResult')
    })

    it('opens a Test table on the columns a datatype argument expands into', async () => {
        mockGetTables.mockImplementation(async (_projectId, kinds) => kinds.includes('Datatype')
            ? projectTypes
            : [{
                id: 'premium-id',
                tableType: 'SimpleRules',
                name: 'Premium',
                returnType: 'Double',
                signature: 'Premium(Customer customer)',
            }])
        render(<CreateTableModal />)

        await openModal({ sourceTableId: 'premium-id' })

        // A datatype argument is opened into one column per field, addressed by the path OpenL reads it back with.
        await waitFor(() => expect(screen.getByTestId('create-table-skeleton'))
            .toHaveTextContent('Customer NameCustomer AgeResult'))
        expect(mockGetDatatype).toHaveBeenCalledWith('project-id', 'customer-id')
        expect(screen.getByTestId('create-table-cell-0-0')).toHaveValue('Text1')
        expect(screen.getByTestId('create-table-cell-0-1')).toHaveValue(1)
        expect(screen.getByTestId('create-table-cell-0-2')).toHaveValue(1)
    })

    it('creates the first module when the project has no modules', async () => {
        mockGetModules.mockResolvedValueOnce([])
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()

        await waitFor(() => expect(screen.getByTestId('create-table-module')).toHaveValue(''))
        await enterTableName(user)
        await user.type(screen.getByTestId('create-table-module'), 'Pricing')

        await user.click(createButton())

        await waitFor(() => expect(mockCreateTable).toHaveBeenCalledTimes(1))
        expect(mockCreateTable.mock.calls[0]![1]).toMatchObject({
            moduleName: 'Pricing',
            modulePath: defaultModulePath('Pricing'),
        } satisfies Partial<CreateTableRequest>)
    })

    it('keeps the modal open when creation fails', async () => {
        mockCreateTable.mockResolvedValueOnce(null)
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        const onSuccess = await openModal()
        await enterTableName(user)
        await waitFor(() => expect(createButton()).toBeEnabled())

        await user.click(createButton())

        await waitFor(() => expect(mockCreateTable).toHaveBeenCalledTimes(1))
        expect(screen.getByRole('dialog')).toBeInTheDocument()
        expect(onSuccess).not.toHaveBeenCalled()
    })

    it('never submits a blank row, which the write API rejects', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-module')).toHaveValue('Main'))

        await enterTableName(user)
        await user.type(screen.getByTestId('create-table-cell-0-0'), 'String')
        await user.type(screen.getByTestId('create-table-cell-0-1'), 'name')
        // The editor keeps an empty row below the last filled one; it must not reach the server.
        expect(screen.getByTestId('create-table-cell-1-0')).toBeInTheDocument()
        await user.click(createButton())

        await waitFor(() => expect(mockCreateTable).toHaveBeenCalledTimes(1))
        const source = mockCreateTable.mock.calls[0]![1].table.source
        expect(source).toHaveLength(3)
        expect(source.every(row => row.some(cell => cell.value !== null || cell.covered))).toBe(true)
    })

    it('seeds a Properties table with the scope OpenL requires', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-module')).toHaveValue('Main'))

        await user.selectOptions(screen.getByTestId('create-table-type'), 'properties')
        await user.click(createButton())

        await waitFor(() => expect(mockCreateTable).toHaveBeenCalledTimes(1))
        expect(mockCreateTable.mock.calls[0]![1].table.source.map(row => row.map(cell => cell.value))).toEqual([
            ['Properties', null],
            ['scope', 'Module'],
        ])
    })

    it('keeps a cell value the suggestion list does not offer', async () => {
        // The dropdowns are a shortcut, not a whitelist: a restricting Select would discard this value silently.
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-module')).toHaveValue('Main'))

        await user.selectOptions(screen.getByTestId('create-table-type'), 'properties')
        await user.type(screen.getByTestId('create-table-cell-1-0'), 'myCustomProperty')
        await user.type(screen.getByTestId('create-table-cell-1-1'), 'on')
        await user.click(createButton())

        await waitFor(() => expect(mockCreateTable).toHaveBeenCalledTimes(1))
        expect(mockCreateTable.mock.calls[0]![1].table.source.map(row => row.map(cell => cell.value))).toEqual([
            ['Properties', null],
            ['scope', 'Module'],
            ['myCustomProperty', 'on'],
        ])
    })

    it('uses typed property editors and writes enum codes in a Properties table', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-module')).toHaveValue('Main'))

        await user.selectOptions(screen.getByTestId('create-table-type'), 'properties')
        await user.type(screen.getByTestId('create-table-cell-1-0'), 'state')
        const enumValue = screen.getByTestId('create-table-cell-1-1')
        expect(enumValue).toHaveAttribute('data-searchable', 'false')
        expect(within(enumValue).getByRole('option', { name: 'Alabama' })).toHaveValue('AL')
        await user.selectOptions(enumValue, 'AL')
        await user.type(screen.getByTestId('create-table-cell-2-0'), 'effectiveDate')
        const dateValue = screen.getByTestId('create-table-cell-2-1')
        expect(dateValue).toHaveAttribute('type', 'date')
        await user.type(dateValue, '2026-01-02')
        await user.type(screen.getByTestId('create-table-cell-3-0'), 'active')
        const checkbox = screen.getByTestId('create-table-cell-3-1')
        expect(checkbox).toHaveAttribute('type', 'checkbox')
        expect(getComputedStyle(screen.getByTestId('create-table-cell-3-1-wrapper')).paddingLeft).toBe('8px')
        await user.click(createButton())

        await waitFor(() => expect(mockCreateTable).toHaveBeenCalledTimes(1))
        expect(mockCreateTable.mock.calls[0]![1].table.source[2]?.map(cell => cell.value)).toEqual(['state', 'AL'])
        expect(mockCreateTable.mock.calls[0]![1].table.source[3]?.map(cell => cell.value))
            .toEqual(['effectiveDate', '2026-01-02'])
    })

    it('wraps multiple property values in a bounded column', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-module')).toHaveValue('Main'))

        await user.selectOptions(screen.getByTestId('create-table-type'), 'properties')
        await user.type(screen.getByTestId('create-table-cell-1-0'), 'country')

        const value = screen.getByTestId('create-table-cell-1-1')
        const valueCell = value.closest('td')
        expect(value).toHaveAccessibleName('project:create_table_modal.cell')
        expect(value).toHaveAttribute('data-mode', 'multiple')
        expect(getComputedStyle(valueCell!)).toMatchObject({
            minWidth: '320px',
            maxWidth: '480px',
        })
        expect(getComputedStyle(value).height).toBe('auto')
    })

    it('omits a named wildcard declaration, which resolves to no module', async () => {
        mockGetModules.mockResolvedValueOnce([
            { name: 'Main', path: 'rules/Main.xlsx' },
            { name: 'Pricing', path: 'rules/Pricing.xlsx' },
        ])
        render(<CreateTableModal />)
        await openModal()

        await waitFor(() => expect(screen.getByTestId('create-table-module')).toHaveValue('Main'))
        // A datalist is invisible by design, so its entries are read as hidden.
        const options = within(screen.getByTestId('create-table-module-options'))
            .getAllByRole('option', { hidden: true })
        expect(options.map(option => option.getAttribute('value'))).not.toContain('All')
        expect(options.map(option => option.getAttribute('value'))).toContain('Pricing')
    })

    it('defaults to the first declared module, the one the options were loaded from', async () => {
        // The backend resolves an omitted module to the first the descriptor declares, so an alphabetical default
        // would describe a different module than the one whose types are offered.
        mockGetModules.mockResolvedValueOnce([
            { name: 'Policy', path: 'rules/Policy.xlsx' },
            { name: 'Auto', path: 'rules/Auto.xlsx' },
        ])
        render(<CreateTableModal />)
        await openModal({ currentModuleName: '' })

        await waitFor(() => expect(screen.getByTestId('create-table-module')).toHaveValue('Policy'))
        expect(mockGetSheets).toHaveBeenCalledWith('project-id', 'Policy')
    })

    it('puts a Test table in the tests folder and picks a tests module', async () => {
        mockGetModules.mockResolvedValueOnce([
            { name: 'Main', path: 'rules/Main.xlsx' },
            { name: 'AutoPolicyTests', path: 'tests/AutoPolicyTests.xlsx' },
        ])
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-module')).toHaveValue('Main'))

        await user.selectOptions(screen.getByTestId('create-table-type'), 'test')

        // A Test table belongs with the project's tests, so the destination follows the table type.
        await waitFor(() => expect(screen.getByTestId('create-table-module')).toHaveValue('AutoPolicyTests'))
    })

    it('defaults a new module for a Test table to the tests folder', async () => {
        expect(defaultModulePath('Regression', 'Test')).toBe('tests/Regression.xlsx')
        expect(defaultModulePath('Regression', 'Run')).toBe('tests/Regression.xlsx')
        expect(defaultModulePath('Pricing', 'Datatype')).toBe('rules/Pricing.xlsx')
    })

    it('lists the sheets of the module that is selected', async () => {
        mockGetModules.mockResolvedValueOnce([
            { name: 'Main', path: 'rules/Main.xlsx' },
            { name: 'Other', path: 'rules/Other.xlsx' },
        ])
        mockGetSheets.mockImplementation(async (_projectId, moduleName) =>
            moduleName === 'Other' ? ['Rates', 'Factors'] : ['Main'])
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-sheet')).toHaveValue('Main'))

        await user.clear(screen.getByTestId('create-table-module'))
        await user.type(screen.getByTestId('create-table-module'), 'Other')

        // The sheets belong to the module: both the suggestions and the sheet chosen follow it.
        await waitFor(() => expect(screen.getByTestId('create-table-sheet')).toHaveValue('Rates'))
        const sheets = within(screen.getByTestId('create-table-sheet-options'))
            .getAllByRole('option', { hidden: true })
        expect(sheets.map(option => option.getAttribute('value'))).toEqual(['Rates', 'Factors'])
    })

    it('suggests the sheets the module already has and takes a new one', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-module')).toHaveValue('Main'))

        await enterTableName(user)
        await user.clear(screen.getByTestId('create-table-sheet'))
        await user.type(screen.getByTestId('create-table-sheet'), 'Rates')
        expect(screen.getByTestId('create-table-sheet')).toHaveValue('Rates')

        // The list is a shortcut, not a whitelist: a sheet that does not exist yet is accepted.
        await user.clear(screen.getByTestId('create-table-sheet'))
        await user.type(screen.getByTestId('create-table-sheet'), 'BrandNewSheet')
        await user.click(createButton())

        await waitFor(() => expect(mockCreateTable).toHaveBeenCalledTimes(1))
        expect(mockCreateTable.mock.calls[0]![1].sheetName).toBe('BrandNewSheet')
    })

    it('keeps the tested table when the destination module changes', async () => {
        mockGetModules.mockResolvedValueOnce([
            { name: 'Main', path: 'rules/Main.xlsx' },
            { name: 'Other', path: 'rules/Other.xlsx' },
        ])
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-module')).toHaveValue('Main'))

        await user.selectOptions(screen.getByTestId('create-table-type'), 'test')
        await waitFor(() => expect(screen.getByTestId('create-table-target')).toHaveValue('source-table-id'))

        // The module is only where the test is written; it must not disturb the table under test.
        await user.clear(screen.getByTestId('create-table-module'))
        await user.type(screen.getByTestId('create-table-module'), 'Other')

        await waitFor(() => expect(screen.getByTestId('create-table-module')).toHaveValue('Other'))
        expect(screen.getByTestId('create-table-target')).toHaveValue('source-table-id')
        expect(screen.getByTestId('create-table-header')).toHaveTextContent('Test Eligibility EligibilityTest')
    })

    it('refuses a table name that is not a legal OpenL identifier', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-module')).toHaveValue('Main'))

        await user.clear(screen.getByTestId('create-table-name'))
        await user.type(screen.getByTestId('create-table-name'), 'New Table')
        expect(createButton()).toBeDisabled()

        await user.clear(screen.getByTestId('create-table-name'))
        await user.type(screen.getByTestId('create-table-name'), 'NewTable')
        expect(createButton()).toBeEnabled()
    })

    it('requires a name for a Test table too', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal({ sourceTableId: 'source-table-id' })
        await waitFor(() => expect(screen.getByTestId('create-table-name')).toHaveValue('EligibilityTest'))

        await user.clear(screen.getByTestId('create-table-name'))

        expect(createButton()).toBeDisabled()
    })

    it('refuses an argument name OpenL cannot bind as a parameter', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-module')).toHaveValue('Main'))

        await enterTableName(user)
        await user.selectOptions(screen.getByTestId('create-table-type'), 'smartRules')
        await user.type(screen.getByTestId('create-table-argument-type-0'), 'Integer')
        await user.type(screen.getByTestId('create-table-argument-name-0'), '9 age')

        expect(createButton()).toBeDisabled()

        await user.clear(screen.getByTestId('create-table-argument-name-0'))
        await user.type(screen.getByTestId('create-table-argument-name-0'), 'age')

        expect(createButton()).toBeEnabled()
    })

    it('refuses two arguments sharing a name, which no signature can declare', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-module')).toHaveValue('Main'))

        await user.selectOptions(screen.getByTestId('create-table-type'), 'smartRules')
        await user.type(screen.getByTestId('create-table-argument-type-0'), 'Integer')
        await user.type(screen.getByTestId('create-table-argument-name-0'), 'age')
        await user.type(screen.getByTestId('create-table-argument-type-1'), 'String')
        await user.type(screen.getByTestId('create-table-argument-name-1'), 'age')

        expect(createButton()).toBeDisabled()
    })

    it('keeps the entered values when a Lookup argument is renamed', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-module')).toHaveValue('Main'))

        await user.selectOptions(screen.getByTestId('create-table-type'), 'simpleLookup')
        await user.clear(screen.getByTestId('create-table-result-type'))
        await user.type(screen.getByTestId('create-table-result-type'), 'String')
        for (const [index, name] of ['make', 'year'].entries()) {
            await user.type(screen.getByTestId(`create-table-argument-type-${index}`), 'String')
            await user.type(screen.getByTestId(`create-table-argument-name-${index}`), name)
        }
        const filled = screen.getByTestId('create-table-cell-1-1')
        await user.clear(filled)
        await user.type(filled, 'Audi')

        // Renaming an argument leaves the shape of the lookup untouched, so the matrix stays as it was typed.
        await user.type(screen.getByTestId('create-table-argument-name-1'), 's')

        expect(screen.getByTestId('create-table-cell-1-1')).toHaveValue('Audi')
    })

    it('reads the callable tables only when a table that exercises one is asked for', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-module')).toHaveValue('Main'))

        // Opening asks for the datatypes every table type offers, and for nothing only a test would need.
        expect(mockGetTables).toHaveBeenCalledTimes(1)
        expect(mockGetTables).toHaveBeenCalledWith('project-id', ['Datatype'])

        await user.selectOptions(screen.getByTestId('create-table-type'), 'test')

        await waitFor(() => expect(screen.getByTestId('create-table-target')).toHaveValue('source-table-id'))
        expect(mockGetTables).toHaveBeenLastCalledWith(
            'project-id',
            ['Rules', 'Spreadsheet', 'Method', 'TBasic', 'Column Match']
        )
        expect(mockGetTables).toHaveBeenCalledTimes(2)
    })

    it('asks for the callable tables again after the list failed to load', async () => {
        mockGetTables.mockImplementation(async (_projectId, kinds) => {
            if (kinds.includes('Datatype')) {
                return projectTypes
            }
            throw new Error('compilation failed')
        })
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-module')).toHaveValue('Main'))

        await user.selectOptions(screen.getByTestId('create-table-type'), 'test')

        await waitFor(() => expect(mockNotifyError).toHaveBeenCalledWith(expect.objectContaining({
            title: 'project:create_table_modal.options_load_failed',
        })))
        // Nothing to write a test against, so the table cannot be created — and the dialog stays open to say so.
        expect(createButton()).toBeDisabled()

        // A failure is not an answer: choosing the type again asks once more.
        await user.selectOptions(screen.getByTestId('create-table-type'), 'datatype')
        await user.selectOptions(screen.getByTestId('create-table-type'), 'test')

        await waitFor(() => expect(mockGetTables).toHaveBeenCalledTimes(3))
    })

    it('reads the callable tables once when Create Test opens on one of them', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal({ sourceTableId: 'source-table-id' })
        await waitFor(() => expect(screen.getByTestId('create-table-target')).toHaveValue('source-table-id'))

        // A Test table opens on a Test table, so the list is read after all: the tested table's signature is what
        // the skeleton is built from, and the list is where the project names it.
        expect(mockGetTables).toHaveBeenCalledTimes(2)
        const targetSelect = screen.getByTestId('create-table-target')
        expect(within(targetSelect).getByRole('option', { name: 'Eligibility' })).toBeInTheDocument()

        // Looking for another table asks nobody: the list is already in hand.
        await user.click(targetSelect)

        expect(mockGetTables).toHaveBeenCalledTimes(2)
    })

    it('offers a void method to Run but not to Test', async () => {
        const targets: ProjectTable[] = [
            {
                id: 'void-table-id',
                tableType: 'SimpleRules',
                name: 'Audit',
                returnType: 'void',
                signature: 'Audit(String message)',
            },
            ...executableTables,
        ]
        mockGetTables.mockImplementation(async (_projectId, kinds) => kinds.includes('Datatype')
            ? projectTypes
            : targets)
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-module')).toHaveValue('Main'))

        await user.selectOptions(screen.getByTestId('create-table-type'), 'test')
        const targetSelect = screen.getByTestId('create-table-target')
        await waitFor(() => expect(targetSelect).toHaveValue('source-table-id'))
        expect(within(targetSelect).queryByRole('option', { name: 'Audit' })).not.toBeInTheDocument()

        await user.selectOptions(screen.getByTestId('create-table-type'), 'run')
        expect(within(targetSelect).getByRole('option', { name: 'Audit' })).toBeInTheDocument()
        await user.selectOptions(targetSelect, 'void-table-id')

        await waitFor(() => expect(screen.getByTestId('create-table-header')).toHaveTextContent('Run Audit AuditRun'))
        expect(screen.getByTestId('create-table-skeleton')).toHaveTextContent('Message')
    })

    it('keeps the tested table when Test is switched to Run', async () => {
        mockGetTables.mockResolvedValue([
            { id: 'other-table-id', tableType: 'SimpleRules', name: 'Discount' },
            { id: 'source-table-id', tableType: 'SimpleRules', name: 'Eligibility' },
        ])
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-module')).toHaveValue('Main'))

        await user.selectOptions(screen.getByTestId('create-table-type'), 'test')
        // Choosing the type takes the first table the project offers; the author then picks the one they meant.
        await waitFor(() => expect(screen.getByTestId('create-table-target')).toHaveValue('other-table-id'))
        await user.selectOptions(screen.getByTestId('create-table-target'), 'source-table-id')
        await waitFor(() => expect(screen.getByTestId('create-table-header'))
            .toHaveTextContent('Test Eligibility EligibilityTest'))

        // Both types are written against the same tested table, so switching must not fall back to the first one.
        await user.selectOptions(screen.getByTestId('create-table-type'), 'run')

        await waitFor(() => expect(screen.getByTestId('create-table-target')).toHaveValue('source-table-id'))
        expect(screen.getByTestId('create-table-header')).toHaveTextContent('Run Eligibility EligibilityTest')
        // One list for both types: the datatypes at opening, the callable tables when Test was chosen.
        expect(mockGetTables).toHaveBeenCalledTimes(2)
    })

    it('refuses a Datatype declaring no field', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-module')).toHaveValue('Main'))

        // A header alone is not a table; OpenL binds the label row as a field declaration and the datatype breaks.
        await user.clear(screen.getByTestId('create-table-cell-0-0'))
        await user.clear(screen.getByTestId('create-table-cell-0-1'))

        expect(createButton()).toBeDisabled()
    })

    it('opens on its own default when the table offered is no longer one a test can call', async () => {
        render(<CreateTableModal />)

        await openModal({ sourceTableId: 'deleted-table-id' })

        // One unusable table must not cost the author every other type the modal offers.
        await waitFor(() => expect(screen.getByTestId('create-table-type')).toHaveValue('datatype'))
        expect(screen.getByTestId('create-table-module')).toHaveValue('Main')
        expect(screen.getByTestId('create-table-name')).toHaveValue('')
        expect(createButton()).toBeDisabled()
        expect(mockNotifyError).toHaveBeenCalledWith(expect.objectContaining({
            title: 'project:create_table_modal.tested_table_load_failed',
        }))
    })

    it('lists the sheets of the module a Test table moves to', async () => {
        mockGetModules.mockResolvedValue([
            { name: 'Main', path: 'rules/Main.xlsx' },
            { name: 'Tests', path: 'tests/Tests.xlsx' },
        ])
        mockGetSheets.mockImplementation(async (_project: string, module: string) =>
            module === 'Tests' ? ['Regression'] : ['Main', 'Rates'])
        const user = userEvent.setup({ delay: null })
        render(<CreateTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('create-table-module')).toHaveValue('Main'))

        // Moving the destination and building the skeleton happen at once; neither may cancel the other.
        await user.selectOptions(screen.getByTestId('create-table-type'), 'test')

        await waitFor(() => expect(screen.getByTestId('create-table-module')).toHaveValue('Tests'))
        await waitFor(() => {
            const sheets = within(screen.getByTestId('create-table-sheet-options'))
                .getAllByRole('option', { hidden: true })
            expect(sheets.map(option => option.getAttribute('value'))).toEqual(['Regression'])
        })
    })
})
