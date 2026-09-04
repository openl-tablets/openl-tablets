import React from 'react'
import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import type { Dayjs } from 'dayjs'
import userEvent from '@testing-library/user-event'
import type { MockedFunction } from 'vitest'
import { getModuleSheets, getProjectModules, getProjectProperties } from 'services/projects'
import { copyTable, getTableCopyInfo } from 'services/tables'
import type { TableCopyInfo } from 'types/tables'
import { property } from '../tableModals/propertyFixture'
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
        options?: ({ label: React.ReactNode, value: string } | { label: string, options: { label: React.ReactNode, value: string }[] })[]
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
            {options?.map(option => 'options' in option ? (
                <optgroup key={option.label} label={option.label}>
                    {option.options.map(item => (
                        <option key={item.value} value={item.value}>{item.label}</option>
                    ))}
                </optgroup>
            ) : (
                <option key={option.value} value={option.value}>{option.label}</option>
            ))}
        </select>
    )
    // A real dayjs value: the editor reads the day out of it and carries over the time the value it replaces held.
    const dayjs = (await import('dayjs')).default
    const MockDatePicker = ({
        value,
        onChange,
        ...props
    }: {
        value?: Dayjs | null
        onChange?: (date: Dayjs | null) => void
        'data-testid'?: string
    }) => (
        <input
            data-testid={props['data-testid']}
            onChange={event => onChange?.(event.target.value ? dayjs(event.target.value) : null)}
            type="date"
            value={value ? value.format('YYYY-MM-DD') : ''}
        />
    )
    const MockInputNumber = ({
        value,
        onChange,
        ...props
    }: {
        value?: number
        onChange?: (value: number | null) => void
        'data-testid'?: string
        'aria-label'?: string
    }) => (
        <input
            aria-label={props['aria-label']}
            data-testid={props['data-testid']}
            onChange={event => onChange?.(event.target.value === '' ? null : Number(event.target.value))}
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
    versions: { current: '1.2.3', next: '1.2.4', taken: ['1.2.2', '1.2.3']},
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
            property({
                name: 'version',
                displayName: 'Version',
                group: 'Version',
                pattern: '(\\d+\\.\\d+\\.\\d+)',
            }),
            property({ name: 'lob', displayName: 'LOB', group: 'Business Dimension', dimensional: true }),
            property({ name: 'region', displayName: 'Region', group: 'Business Dimension', dimensional: true }),
            property({
                name: 'state',
                displayName: 'US States',
                group: 'Business Dimension',
                dimensional: true,
                type: 'enum',
                multiple: true,
                values: [
                    { code: 'AL', value: 'Alabama' },
                    { code: 'AK', value: 'Alaska' },
                ],
            }),
        ])
        mockGetSheets.mockResolvedValue(['Rules', 'Archive'])
        mockCopy.mockResolvedValue({
            id: 'copy-id',
            tableType: 'SimpleRules',
            kind: 'Rules',
            name: 'EligibilityCopy',
        })
    })

    it('prefills a date property with the date the source declares', async () => {
        const user = userEvent.setup({ delay: null })
        mockGetInfo.mockResolvedValue({
            ...sourceInfo,
            properties: [
                { name: 'effectiveDate', value: '2009-01-01' },
                { name: 'expirationDate', value: '2009-12-31T23:59' },
            ],
        })
        mockGetProperties.mockResolvedValue([
            property({
                name: 'effectiveDate',
                displayName: 'Effective Date',
                group: 'Business Dimension',
                dimensional: true,
                type: 'date',
            }),
            property({
                name: 'expirationDate',
                displayName: 'Expiration Date',
                group: 'Business Dimension',
                dimensional: true,
                type: 'date',
            }),
        ])
        render(<CopyTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('copy-table-module')).toHaveValue('Main'))

        // An author who cannot see the date cannot tell it is there, and picking one silently replaces it.
        expect(screen.getByTestId('copy-table-property-value-0')).toHaveValue('2009-01-01')
        // A date naming a moment of the day is shown as the day it falls on.
        expect(screen.getByTestId('copy-table-property-value-1')).toHaveValue('2009-12-31')

        await user.clear(screen.getByTestId('copy-table-name'))
        await user.type(screen.getByTestId('copy-table-name'), 'EligibilityCopy')
        await user.click(screen.getByRole('button', { name: 'project:copy_table_modal.copy' }))

        await waitFor(() => expect(mockCopy).toHaveBeenCalledTimes(1))
        expect(mockCopy.mock.calls[0]![2].properties).toEqual([
            { name: 'effectiveDate', value: '2009-01-01' },
            { name: 'expirationDate', value: '2009-12-31T23:59' },
        ])
    })

    it('keeps the time of day a date carries when another day is picked', async () => {
        const user = userEvent.setup({ delay: null })
        mockGetInfo.mockResolvedValue({
            ...sourceInfo,
            properties: [{ name: 'expirationDate', value: '2009-12-31T23:59' }],
        })
        mockGetProperties.mockResolvedValue([
            property({
                name: 'expirationDate',
                displayName: 'Expiration Date',
                group: 'Business Dimension',
                dimensional: true,
                type: 'date',
            }),
        ])
        render(<CopyTableModal />)
        await openModal()
        await waitFor(() => expect(screen.getByTestId('copy-table-module')).toHaveValue('Main'))

        await user.clear(screen.getByTestId('copy-table-name'))
        await user.type(screen.getByTestId('copy-table-name'), 'EligibilityCopy')
        // The picker chooses a day; the moment the value names is not the author's to lose by touching the field.
        fireEvent.change(screen.getByTestId('copy-table-property-value-0'), { target: { value: '2010-06-30' } })
        await user.click(screen.getByRole('button', { name: 'project:copy_table_modal.copy' }))

        await waitFor(() => expect(mockCopy).toHaveBeenCalledTimes(1))
        expect(mockCopy.mock.calls[0]![2].properties)
            .toEqual([{ name: 'expirationDate', value: '2010-06-30T23:59' }])
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
                { name: 'version', value: '1.2.4' },
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
        await screen.findByTestId('copy-table-property-row-2')

        await user.selectOptions(screen.getByTestId('copy-table-property-name-2'), 'state')
        const value = screen.getByTestId('copy-table-property-value-2')
        expect(value).toHaveAttribute('data-searchable', 'true')
        expect(within(value).getByRole('option', { name: 'Alabama' })).toHaveValue('AL')
        await user.selectOptions(value, 'AL')
        await user.click(screen.getByRole('button', { name: 'project:copy_table_modal.copy' }))

        await waitFor(() => expect(mockCopy).toHaveBeenCalledTimes(1))
        expect(mockCopy.mock.calls[0]![2].properties).toContainEqual({ name: 'state', value: 'AL' })
    })

    it('offers the properties by display name under their groups', async () => {
        render(<CopyTableModal />)
        await openModal()
        await screen.findByTestId('copy-table-property-row-2')

        const names = screen.getByTestId('copy-table-property-name-2')
        // Grouped the way Table Details groups them, so the dimensional properties are presented, not guessed.
        expect([...names.querySelectorAll('optgroup')].map(group => group.label))
            .toEqual(['Business Dimension', 'Version'])
        expect(within(names).getByRole('option', { name: 'US States' })).toHaveValue('state')
        expect(within(names).getByRole('option', { name: 'LOB' })).toHaveValue('lob')
    })

    it('adds properties with the same trailing-row behavior as Spreadsheet arguments', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CopyTableModal />)
        await openModal()
        await screen.findByTestId('copy-table-property-row-2')

        await user.selectOptions(screen.getByTestId('copy-table-property-name-2'), 'region')
        expect(screen.queryByTestId('copy-table-property-row-3')).not.toBeInTheDocument()
        await user.type(screen.getByTestId('copy-table-property-value-2'), 'EU')
        expect(screen.getByTestId('copy-table-property-row-3')).toBeInTheDocument()

        await user.clear(screen.getByTestId('copy-table-name'))
        await user.type(screen.getByTestId('copy-table-name'), 'EligibilityEurope')
        await user.click(screen.getByRole('button', { name: 'project:copy_table_modal.copy' }))

        await waitFor(() => expect(mockCopy).toHaveBeenCalledTimes(1))
        expect(mockCopy.mock.calls[0]![2].properties).toEqual([
            { name: 'version', value: '1.2.4' },
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
        await screen.findByTestId('copy-table-property-row-2')
        const copyButton = screen.getByRole('button', { name: 'project:copy_table_modal.copy' })

        await user.clear(screen.getByTestId('copy-table-name'))
        await user.type(screen.getByTestId('copy-table-name'), 'EligibilityCopy')
        // A named row with no value yet is half-written, so the copy waits for it.
        await user.selectOptions(screen.getByTestId('copy-table-property-name-2'), 'region')
        expect(copyButton).toBeDisabled()
        await user.type(screen.getByTestId('copy-table-property-value-2'), 'EU')
        expect(copyButton).toBeEnabled()

        // The same property twice says two things about one property.
        await user.selectOptions(screen.getByTestId('copy-table-property-name-2'), 'lob')
        await user.clear(screen.getByTestId('copy-table-property-value-2'))
        await user.type(screen.getByTestId('copy-table-property-value-2'), 'Auto')
        expect(copyButton).toBeDisabled()
    })

    it('offers the next free version and shows the one the table stands for', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CopyTableModal />)
        await openModal()
        await screen.findByTestId('copy-table-property-row-2')

        // The window opens on the first version the table's versions leave free — 1.2.3 is the source's own and
        // 1.2.2 belongs to the version that stepped aside — with the current one named beside the editor.
        expect(screen.getByTestId('copy-table-property-value-0-current'))
            .toHaveTextContent('project:copy_table_modal.version_current')
        expect(screen.getByTestId('copy-table-property-value-0-0')).toHaveValue(1)
        expect(screen.getByTestId('copy-table-property-value-0-1')).toHaveValue(2)
        expect(screen.getByTestId('copy-table-property-value-0-2')).toHaveValue(4)

        // A property chosen anew opens on that same free version.
        await user.selectOptions(screen.getByTestId('copy-table-property-name-2'), 'region')
        await user.selectOptions(screen.getByTestId('copy-table-property-name-2'), 'version')
        expect(screen.getByTestId('copy-table-property-value-2-2')).toHaveValue(4)
    })

    it('does not submit a version another version of the table already carries', async () => {
        const user = userEvent.setup({ delay: null })
        render(<CopyTableModal />)
        await openModal()
        await screen.findByTestId('copy-table-property-row-2')
        const copyButton = screen.getByRole('button', { name: 'project:copy_table_modal.copy' })

        // The offered 1.2.4 is free, so the copy starts submittable.
        expect(copyButton).toBeEnabled()

        // 1.2.2 belongs to a version that stepped aside: two versions under one number cannot be ordered.
        await user.clear(screen.getByTestId('copy-table-property-value-0-2'))
        await user.type(screen.getByTestId('copy-table-property-value-0-2'), '2')
        expect(copyButton).toBeDisabled()

        // And so does 1.2.3, the one the source itself stands for — carrying it over is no excuse.
        await user.clear(screen.getByTestId('copy-table-property-value-0-2'))
        await user.type(screen.getByTestId('copy-table-property-value-0-2'), '3')
        expect(copyButton).toBeDisabled()
    })

    it('offers a readable version even when the one the source carries is not', async () => {
        // A table written when a shorter version was documented as valid: the copy must still be writable.
        mockGetInfo.mockResolvedValueOnce({
            name: 'Eligibility',
            kind: 'Rules',
            properties: [{ name: 'version', value: '1.0' }],
            versions: { current: '1.0', next: '0.0.1', taken: ['1.0']},
        })
        render(<CopyTableModal />)
        await openModal()
        await screen.findByTestId('copy-table-property-row-1')

        expect(screen.getByTestId('copy-table-property-value-0-0')).toHaveValue(0)
        expect(screen.getByTestId('copy-table-property-value-0-2')).toHaveValue(1)
        expect(screen.getByRole('button', { name: 'project:copy_table_modal.copy' })).toBeEnabled()
    })
})
