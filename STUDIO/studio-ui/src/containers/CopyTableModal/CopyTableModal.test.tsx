import React from 'react'
import { act, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { MockedFunction } from 'vitest'
import { getModuleSheets, getProjectModules, getProjectProperties } from 'services/projects'
import { createTableCopy, getTableRaw } from 'services/tables'
import type { RawTable } from 'types/tables'
import { CopyTableModal, type CopyTableModalDetail } from './CopyTableModal'

vi.mock('services/projects', () => ({
    getProjectModules: vi.fn(),
    getModuleSheets: vi.fn(),
    getProjectProperties: vi.fn(),
}))

vi.mock('services/tables', () => ({
    createTableCopy: vi.fn(),
    getTableRaw: vi.fn(),
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
        okButtonProps?: { disabled?: boolean }
    }) => open ? (
        <div role="dialog">
            <div>{title}</div>
            {children}
            <button onClick={onCancel}>{cancelText}</button>
            <button disabled={okButtonProps?.disabled} onClick={onOk}>{okText}</button>
        </div>
    ) : null
    const MockAutoComplete = ({
        id,
        value,
        options,
        onChange,
        ...props
    }: {
        id?: string
        value?: string
        options?: { value: string }[]
        onChange?: (value: string) => void
        'data-testid'?: string
    }) => (
        <>
            <input
                data-testid={props['data-testid']}
                id={id}
                onChange={event => onChange?.(event.target.value)}
                value={value ?? ''}
            />
            <datalist>
                {options?.map(option => <option key={option.value} value={option.value} />)}
            </datalist>
        </>
    )
    const MockSelect = ({
        value,
        options,
        mode,
        onChange,
        showSearch,
        ...props
    }: {
        value?: string | string[]
        options?: { label: React.ReactNode, value: string }[]
        mode?: 'multiple'
        onChange?: (value: string | string[]) => void
        showSearch?: boolean
        'data-testid'?: string
    }) => (
        <select
            data-searchable={String(Boolean(showSearch))}
            data-testid={props['data-testid']}
            multiple={mode === 'multiple'}
            value={value ?? (mode === 'multiple' ? [] : '')}
            onChange={event => onChange?.(mode === 'multiple'
                ? [...event.target.selectedOptions].map(option => option.value)
                : event.target.value)}
        >
            {mode !== 'multiple' && !value ? <option value="" /> : null}
            {options?.map(option => (
                <option key={option.value} value={option.value}>{option.label}</option>
            ))}
        </select>
    )
    return {
        ...actual,
        AutoComplete: MockAutoComplete,
        Modal: MockModal,
        Select: MockSelect,
        Spin: ({ children }: { children?: React.ReactNode }) => <>{children}</>,
        notification: { error: vi.fn() },
    }
})

vi.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key: string, values?: Record<string, unknown>) =>
            values?.['table'] ? `${key}:${values['table']}` : key,
    }),
}))

const source: RawTable = {
    tableType: 'RawSource',
    kind: 'Rules',
    name: 'Eligibility',
    source: [
        [
            { value: 'Rules Boolean Eligibility(Policy policy)', colspan: 3 },
            { value: null, covered: true },
            { value: null, covered: true },
        ],
        [
            { value: 'properties', rowspan: 2 },
            { value: 'version' },
            { value: '1.2.3' },
        ],
        [
            { value: null, covered: true },
            { value: 'lob' },
            { value: 'Auto' },
        ],
        [{ value: 'C1' }, { value: 'RET1' }, { value: null }],
    ],
}

const mockGetRaw = getTableRaw as MockedFunction<typeof getTableRaw>
const mockGetModules = getProjectModules as MockedFunction<typeof getProjectModules>
const mockGetProperties = getProjectProperties as MockedFunction<typeof getProjectProperties>
const mockGetSheets = getModuleSheets as MockedFunction<typeof getModuleSheets>
const mockCreateCopy = createTableCopy as MockedFunction<typeof createTableCopy>

const openModal = async (overrides: Partial<CopyTableModalDetail> = {}) => {
    const onSuccess = overrides.onSuccess ?? vi.fn()
    await act(async () => {
        window.dispatchEvent(new CustomEvent<CopyTableModalDetail>('openCopyTableModal', {
            detail: {
                projectId: 'project-id',
                currentModuleName: 'Main',
                sourceTableId: 'source-id',
                ...overrides,
                onSuccess,
            },
        }))
    })
    return onSuccess
}

describe('CopyTableModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        mockGetRaw.mockResolvedValue(source)
        mockGetModules.mockResolvedValue([
            { name: 'Main', path: 'rules/Main.xlsx' },
            { name: 'Pricing', path: 'rules/Pricing.xlsx' },
        ])
        mockGetProperties.mockResolvedValue([
            { name: 'version', type: 'text', multiple: false, values: []},
            { name: 'lob', type: 'text', multiple: false, values: []},
            { name: 'region', type: 'text', multiple: false, values: []},
            {
                name: 'state',
                type: 'enum',
                multiple: true,
                values: [
                    { code: 'AL', value: 'Alabama' },
                    { code: 'AK', value: 'Alaska' },
                ],
            },
        ])
        mockGetSheets.mockResolvedValue(['Rules', 'Archive'])
        mockCreateCopy.mockResolvedValue({
            id: 'copy-id',
            tableType: 'SimpleRules',
            kind: 'Rules',
            name: 'EligibilityCopy',
        })
    })

    it('reads raw cells and creates the copy through the ordinary table write request', async () => {
        const user = userEvent.setup()
        render(<CopyTableModal />)
        const onSuccess = await openModal()

        await waitFor(() => expect(screen.getByTestId('copy-table-module')).toHaveValue('Main'))
        expect(mockGetRaw).toHaveBeenCalledWith('project-id', 'source-id')
        expect(mockGetProperties).toHaveBeenCalledWith('project-id', 'Rules')
        expect(screen.getByRole('dialog')).toHaveTextContent('project:copy_table_modal.title:Eligibility')
        expect(screen.queryByText('Copy As')).not.toBeInTheDocument()
        expect(screen.getByTestId('copy-table-sheet')).toHaveValue('Rules')
        expect(screen.getByTestId('copy-table-property-name-0')).toHaveValue('version')
        expect(screen.getByTestId('copy-table-property-value-1')).toHaveValue('Auto')

        await user.clear(screen.getByTestId('copy-table-name'))
        await user.type(screen.getByTestId('copy-table-name'), 'EligibilityCopy')
        await user.click(screen.getByRole('button', { name: 'project:copy_table_modal.copy' }))

        await waitFor(() => expect(mockCreateCopy).toHaveBeenCalledTimes(1))
        expect(mockCreateCopy).toHaveBeenCalledWith('project-id', {
            moduleName: 'Main',
            sheetName: 'Rules',
            table: {
                tableType: 'RawSource',
                kind: 'Rules',
                name: 'EligibilityCopy',
                source: [
                    [
                        { value: 'Rules Boolean EligibilityCopy(Policy policy)', colspan: 3 },
                        { value: null, covered: true },
                        { value: null, covered: true },
                    ],
                    [
                        { value: 'properties', rowspan: 2 },
                        { value: 'version' },
                        { value: '1.2.3' },
                    ],
                    [
                        { value: null, covered: true },
                        { value: 'lob' },
                        { value: 'Auto' },
                    ],
                    [{ value: 'C1' }, { value: 'RET1' }, { value: null }],
                ],
            },
        })
        expect(onSuccess).toHaveBeenCalledWith(expect.objectContaining({ id: 'copy-id' }), 'Main')
    })

    it('allows a copy to keep the source table name', async () => {
        const user = userEvent.setup()
        render(<CopyTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('copy-table-name')).toHaveValue('Eligibility'))

        const copyButton = screen.getByRole('button', { name: 'project:copy_table_modal.copy' })
        expect(copyButton).toBeEnabled()
        await user.click(copyButton)

        await waitFor(() => expect(mockCreateCopy).toHaveBeenCalledTimes(1))
        expect(mockCreateCopy.mock.calls[0]![1].table.name).toBe('Eligibility')
    })

    it('shows enum values but writes their codes', async () => {
        const user = userEvent.setup()
        render(<CopyTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('copy-table-property-row-2')).toBeInTheDocument())

        await user.type(screen.getByTestId('copy-table-property-name-2'), 'state')
        const value = screen.getByTestId('copy-table-property-value-2')
        expect(value).toHaveAttribute('data-searchable', 'true')
        expect(within(value).getByRole('option', { name: 'Alabama' })).toHaveValue('AL')
        await user.selectOptions(value, 'AL')
        await user.click(screen.getByRole('button', { name: 'project:copy_table_modal.copy' }))

        await waitFor(() => expect(mockCreateCopy).toHaveBeenCalledTimes(1))
        const written = mockCreateCopy.mock.calls[0]![1].table.source
        expect(written[3]?.[1]?.value).toBe('state')
        expect(written[3]?.[2]?.value).toBe('AL')
    })

    it('adds properties with the same trailing-row behavior as Spreadsheet arguments', async () => {
        const user = userEvent.setup()
        render(<CopyTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('copy-table-property-row-2')).toBeInTheDocument())

        await user.type(screen.getByTestId('copy-table-property-name-2'), 'region')
        expect(screen.queryByTestId('copy-table-property-row-3')).not.toBeInTheDocument()
        await user.type(screen.getByTestId('copy-table-property-value-2'), 'EU')
        expect(screen.getByTestId('copy-table-property-row-3')).toBeInTheDocument()

        await user.clear(screen.getByTestId('copy-table-name'))
        await user.type(screen.getByTestId('copy-table-name'), 'EligibilityEurope')
        await user.click(screen.getByRole('button', { name: 'project:copy_table_modal.copy' }))

        await waitFor(() => expect(mockCreateCopy).toHaveBeenCalledTimes(1))
        const written = mockCreateCopy.mock.calls[0]![1].table.source
        expect(written.slice(1, 4).map(row => [row[1]?.value, row[2]?.value])).toEqual([
            ['version', '1.2.3'],
            ['lob', 'Auto'],
            ['region', 'EU'],
        ])
        expect(written[1]?.[0]).toEqual({ value: 'properties', rowspan: 3 })
    })

    it('uses the Create Table module and sheet behavior for a new module', async () => {
        const user = userEvent.setup()
        render(<CopyTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('copy-table-module')).toHaveValue('Main'))

        await user.clear(screen.getByTestId('copy-table-name'))
        await user.type(screen.getByTestId('copy-table-name'), 'EligibilityCopy')
        await user.clear(screen.getByTestId('copy-table-module'))
        await user.type(screen.getByTestId('copy-table-module'), 'New Pricing')
        await user.clear(screen.getByTestId('copy-table-sheet'))
        await user.type(screen.getByTestId('copy-table-sheet'), 'Eligibility')
        await user.click(screen.getByRole('button', { name: 'project:copy_table_modal.copy' }))

        await waitFor(() => expect(mockCreateCopy).toHaveBeenCalledTimes(1))
        expect(mockCreateCopy.mock.calls[0]![1]).toMatchObject({
            moduleName: 'New Pricing',
            modulePath: 'rules/New Pricing.xlsx',
            sheetName: 'Eligibility',
        })
        expect(mockGetSheets).toHaveBeenCalledTimes(1)
    })

    it('does not submit an incomplete or duplicated property row', async () => {
        const user = userEvent.setup()
        render(<CopyTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('copy-table-property-row-2')).toBeInTheDocument())
        const copyButton = screen.getByRole('button', { name: 'project:copy_table_modal.copy' })

        await user.clear(screen.getByTestId('copy-table-name'))
        await user.type(screen.getByTestId('copy-table-name'), 'EligibilityCopy')
        await user.type(screen.getByTestId('copy-table-property-name-2'), 'version')
        expect(copyButton).toBeDisabled()
        await user.type(screen.getByTestId('copy-table-property-value-2'), '2.0.0')
        expect(copyButton).toBeDisabled()
    })
})
