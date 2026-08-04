import React from 'react'
import { act, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { MockedFunction } from 'vitest'
import { getModuleSheets, getProjectModules, getProjectProperties } from 'services/projects'
import { copyTable, getTableCopyInfo } from 'services/tables'
import type { TableCopyInfo } from 'types/tables'
import { CopyTableModal, type CopyTableModalDetail } from './CopyTableModal'

vi.mock('services/projects', () => ({
    getProjectModules: vi.fn(),
    getModuleSheets: vi.fn(),
    getProjectProperties: vi.fn(),
}))

vi.mock('services/tables', () => ({
    copyTable: vi.fn(),
    getTableCopyInfo: vi.fn(),
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

const sourceInfo: TableCopyInfo = {
    name: 'Eligibility',
    kind: 'Rules',
    properties: [
        { name: 'version', value: '1.2.3' },
        { name: 'lob', value: 'Auto' },
    ],
}

const mockGetInfo = getTableCopyInfo as MockedFunction<typeof getTableCopyInfo>
const mockGetModules = getProjectModules as MockedFunction<typeof getProjectModules>
const mockGetProperties = getProjectProperties as MockedFunction<typeof getProjectProperties>
const mockGetSheets = getModuleSheets as MockedFunction<typeof getModuleSheets>
const mockCopy = copyTable as MockedFunction<typeof copyTable>

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
        mockGetInfo.mockResolvedValue(sourceInfo)
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
        mockCopy.mockResolvedValue({
            id: 'copy-id',
            tableType: 'SimpleRules',
            kind: 'Rules',
            name: 'EligibilityCopy',
        })
    })

    it('reads the properties and copies the table on the server by its id', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CopyTableModal />)
        const onSuccess = await openModal()

        await waitFor(() => expect(screen.getByTestId('copy-table-module')).toHaveValue('Main'))
        expect(mockGetInfo).toHaveBeenCalledWith('project-id', 'source-id')
        expect(mockGetProperties).toHaveBeenCalledWith('project-id', 'Rules')
        expect(screen.getByRole('dialog')).toHaveTextContent('project:copy_table_modal.title:Eligibility')
        expect(screen.getByTestId('copy-table-sheet')).toHaveValue('Rules')
        expect(screen.getByTestId('copy-table-property-name-0')).toHaveValue('version')
        expect(screen.getByTestId('copy-table-property-value-1')).toHaveValue('Auto')

        await user.clear(screen.getByTestId('copy-table-name'))
        await user.type(screen.getByTestId('copy-table-name'), 'EligibilityCopy')
        await user.click(screen.getByRole('button', { name: 'project:copy_table_modal.copy' }))

        await waitFor(() => expect(mockCopy).toHaveBeenCalledTimes(1))
        expect(mockCopy).toHaveBeenCalledWith('project-id', 'source-id', {
            moduleName: 'Main',
            sheetName: 'Rules',
            name: 'EligibilityCopy',
            properties: [
                { name: 'version', value: '1.2.3' },
                { name: 'lob', value: 'Auto' },
            ],
        })
        expect(onSuccess).toHaveBeenCalledWith(expect.objectContaining({ id: 'copy-id' }), 'Main')
    })

    it('allows a copy to keep the source table name', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CopyTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('copy-table-name')).toHaveValue('Eligibility'))

        const copyButton = screen.getByRole('button', { name: 'project:copy_table_modal.copy' })
        expect(copyButton).toBeEnabled()
        await user.click(copyButton)

        await waitFor(() => expect(mockCopy).toHaveBeenCalledTimes(1))
        expect(mockCopy.mock.calls[0]![2].name).toBe('Eligibility')
    })

    it('keeps Copy enabled for a long name, clipping the mirrored sheet to Excel\'s limit', async () => {
        mockGetInfo.mockResolvedValueOnce({ name: 'E'.repeat(40), kind: 'Rules' })
        // No worksheet pins the sheet field, so it mirrors the name the way a fresh module's does.
        mockGetSheets.mockResolvedValue([])
        const user = userEvent.setup({ delay: null })
        render(<CopyTableModal />)
        await openModal()

        // A source name past the 31-character worksheet limit mirrors into a sheet clipped to fit.
        await waitFor(() => expect(screen.getByTestId('copy-table-sheet')).toHaveValue('E'.repeat(31)))
        expect(screen.getByRole('button', { name: 'project:copy_table_modal.copy' })).toBeEnabled()

        // Renaming the copy to another long name keeps the mirror clipped, so Copy stays enabled.
        await user.clear(screen.getByTestId('copy-table-name'))
        await user.type(screen.getByTestId('copy-table-name'), 'N'.repeat(40))
        expect(screen.getByTestId('copy-table-sheet')).toHaveValue('N'.repeat(31))
        expect(screen.getByRole('button', { name: 'project:copy_table_modal.copy' })).toBeEnabled()

        await user.click(screen.getByRole('button', { name: 'project:copy_table_modal.copy' }))

        await waitFor(() => expect(mockCopy).toHaveBeenCalledTimes(1))
        expect(mockCopy.mock.calls[0]![2]).toMatchObject({ name: 'N'.repeat(40), sheetName: 'N'.repeat(31) })
    })

    it('copies a source that declares no properties', async () => {
        mockGetInfo.mockResolvedValue({ name: 'Eligibility', kind: 'Rules' })
        const user = userEvent.setup({ delay: null })
        render(<CopyTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('copy-table-module')).toHaveValue('Main'))

        await user.clear(screen.getByTestId('copy-table-name'))
        await user.type(screen.getByTestId('copy-table-name'), 'EligibilityCopy')
        await user.click(screen.getByRole('button', { name: 'project:copy_table_modal.copy' }))

        await waitFor(() => expect(mockCopy).toHaveBeenCalledTimes(1))
        expect(mockCopy.mock.calls[0]![2].properties).toEqual([])
    })

    it('shows enum values but sends their codes', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CopyTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('copy-table-property-row-2')).toBeInTheDocument())

        await user.type(screen.getByTestId('copy-table-property-name-2'), 'state')
        const value = screen.getByTestId('copy-table-property-value-2')
        expect(value).toHaveAttribute('data-searchable', 'true')
        expect(within(value).getByRole('option', { name: 'Alabama' })).toHaveValue('AL')
        await user.selectOptions(value, 'AL')
        await user.click(screen.getByRole('button', { name: 'project:copy_table_modal.copy' }))

        await waitFor(() => expect(mockCopy).toHaveBeenCalledTimes(1))
        expect(mockCopy.mock.calls[0]![2].properties).toContainEqual({ name: 'state', value: 'AL' })
    })

    it('adds properties with the same trailing-row behavior as Spreadsheet arguments', async () => {
        const user = userEvent.setup({ delay: null })
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

        await waitFor(() => expect(mockCopy).toHaveBeenCalledTimes(1))
        expect(mockCopy.mock.calls[0]![2].properties).toEqual([
            { name: 'version', value: '1.2.3' },
            { name: 'lob', value: 'Auto' },
            { name: 'region', value: 'EU' },
        ])
    })

    it('uses the Create Table module and sheet behavior for a new module', async () => {
        const user = userEvent.setup({ delay: null })
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

        await waitFor(() => expect(mockCopy).toHaveBeenCalledTimes(1))
        expect(mockCopy.mock.calls[0]![2]).toMatchObject({
            moduleName: 'New Pricing',
            modulePath: 'rules/New Pricing.xlsx',
            sheetName: 'Eligibility',
        })
        expect(mockGetSheets).toHaveBeenCalledTimes(1)
    })

    it('does not submit an incomplete or duplicated property row', async () => {
        const user = userEvent.setup({ delay: null })
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
