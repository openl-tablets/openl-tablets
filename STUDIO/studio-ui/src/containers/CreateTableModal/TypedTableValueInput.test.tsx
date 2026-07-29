import React from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { tableValueIsValid, TypedTableValueInput } from './TypedTableValueInput'

vi.mock('antd', () => ({
    DatePicker: ({
        onChange,
        ...props
    }: {
        onChange?: (date: { format: (pattern: string) => string } | null) => void
        'aria-label'?: string
        'data-testid'?: string
    }) => (
        <input
            aria-label={props['aria-label']}
            data-testid={props['data-testid']}
            type="date"
            onChange={event => onChange?.(event.target.value
                ? { format: () => event.target.value }
                : null)}
        />
    ),
    Input: (props: React.InputHTMLAttributes<HTMLInputElement>) => <input {...props} />,
    InputNumber: ({
        onChange,
        value,
        max,
        min,
        ...props
    }: {
        onChange?: (value: string | null) => void
        value?: string | null
        max?: string
        min?: string
        'aria-label'?: string
        'data-testid'?: string
    }) => (
        <input
            aria-label={props['aria-label']}
            data-testid={props['data-testid']}
            max={max}
            min={min}
            onChange={event => onChange?.(event.target.value || null)}
            type="number"
            value={value ?? ''}
        />
    ),
    Select: ({
        onChange,
        options,
        showSearch,
        value,
        ...props
    }: {
        onChange?: (value: string | undefined) => void
        options?: { label: string, value: string }[]
        showSearch?: boolean
        value?: string
        'aria-label'?: string
        'data-testid'?: string
    }) => (
        <select
            aria-label={props['aria-label']}
            data-searchable={String(Boolean(showSearch))}
            data-testid={props['data-testid']}
            onChange={event => onChange?.(event.target.value || undefined)}
            value={value ?? ''}
        >
            <option value="" />
            {options?.map(option => (
                <option key={option.value} value={option.value}>{option.label}</option>
            ))}
        </select>
    ),
}))

const vocabularies = { Country: ['USA', 'Canada']}

const renderValue = (
    type: string,
    value = '',
    onChange = vi.fn()
) => {
    render(
        <TypedTableValueInput
            aria-label="Value"
            data-testid="value"
            onChange={onChange}
            type={type}
            value={value}
            vocabularyValues={vocabularies}
        />
    )
    return onChange
}

describe('TypedTableValueInput', () => {
    it('offers empty, TRUE, and FALSE for Boolean values', async () => {
        const onChange = renderValue('Boolean')
        const input = screen.getByTestId('value')

        expect(screen.getAllByRole('option').map(option => option.getAttribute('value')))
            .toEqual(['', 'TRUE', 'FALSE'])
        await userEvent.selectOptions(input, 'FALSE')
        expect(onChange).toHaveBeenLastCalledWith('FALSE')
        await userEvent.selectOptions(input, '')
        expect(onChange).toHaveBeenLastCalledWith('')
    })

    it('restricts a vocabulary to the values it declares', async () => {
        const onChange = renderValue('Country', 'USA')

        expect(screen.getByTestId('value')).toHaveAttribute('data-searchable', 'false')
        expect(screen.getAllByRole('option').map(option => option.getAttribute('value')))
            .toEqual(['', 'USA', 'Canada'])
        await userEvent.selectOptions(screen.getByTestId('value'), 'Canada')
        expect(onChange).toHaveBeenCalledWith('Canada')
    })

    it('uses numeric, date, and one-character inputs for their scalar types', async () => {
        const { rerender } = render(
            <TypedTableValueInput
                aria-label="Value"
                data-testid="value"
                onChange={vi.fn()}
                type="Integer"
                value=""
                vocabularyValues={vocabularies}
            />
        )
        expect(screen.getByTestId('value')).toHaveAttribute('type', 'number')
        expect(screen.getByTestId('value')).toHaveAttribute('min', '-2147483648')
        expect(screen.getByTestId('value')).toHaveAttribute('max', '2147483647')

        rerender(
            <TypedTableValueInput
                aria-label="Value"
                data-testid="value"
                onChange={vi.fn()}
                type="Date"
                value=""
                vocabularyValues={vocabularies}
            />
        )
        expect(screen.getByTestId('value')).toHaveAttribute('type', 'date')

        rerender(
            <TypedTableValueInput
                aria-label="Value"
                data-testid="value"
                onChange={vi.fn()}
                type="Character"
                value=""
                vocabularyValues={vocabularies}
            />
        )
        expect(screen.getByTestId('value')).toHaveAttribute('maxlength', '1')
    })

    it('stores a selected Date in ISO 8601 format', async () => {
        const onChange = renderValue('Date')

        await userEvent.type(screen.getByTestId('value'), '2026-06-15')

        expect(onChange).toHaveBeenLastCalledWith('2026-06-15')
    })

    it('validates finite and scalar values while allowing an empty cell', () => {
        expect(tableValueIsValid('Boolean', '', vocabularies)).toBe(true)
        expect(tableValueIsValid('Boolean', 'TRUE', vocabularies)).toBe(true)
        expect(tableValueIsValid('Boolean', 'yes', vocabularies)).toBe(false)
        expect(tableValueIsValid('Integer', '-12', vocabularies)).toBe(true)
        expect(tableValueIsValid('Integer', '1.5', vocabularies)).toBe(false)
        expect(tableValueIsValid('Integer', '2147483648', vocabularies)).toBe(false)
        expect(tableValueIsValid('Long', '9223372036854775807', vocabularies)).toBe(true)
        expect(tableValueIsValid('Long', '9223372036854775808', vocabularies)).toBe(false)
        expect(tableValueIsValid('BigInteger', '9223372036854775808', vocabularies)).toBe(true)
        expect(tableValueIsValid('Double', '1.5e2', vocabularies)).toBe(true)
        expect(tableValueIsValid('Character', 'AB', vocabularies)).toBe(false)
        expect(tableValueIsValid('Character', '🙂', vocabularies)).toBe(false)
        expect(tableValueIsValid('Date', '2026-06-15', vocabularies)).toBe(true)
        expect(tableValueIsValid('Date', '06/15/2026', vocabularies)).toBe(false)
        expect(tableValueIsValid('Country', 'Canada', vocabularies)).toBe(true)
        expect(tableValueIsValid('Country', 'Mexico', vocabularies)).toBe(false)
    })
})
