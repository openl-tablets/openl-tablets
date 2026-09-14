import React, { useState } from 'react'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { initialFormValue, SchemaForm, type SchemaFormParameter } from 'components/schemaForm/SchemaForm'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

const policy: SchemaFormParameter = {
    name: 'policy',
    type: 'Policy',
    schema: {
        type: 'object',
        properties: {
            number: { type: 'string' },
            age: { type: 'integer' },
            state: { type: 'string', enum: ['AL', 'NY']},
            active: { type: 'boolean' },
            since: { type: 'string', format: 'date' },
            drivers: { type: 'array', items: { type: 'object', properties: { name: { type: 'string' } } } },
            owner: { type: 'object', properties: { name: { type: 'string' }, vip: { type: 'boolean', default: false } } },
            limits: { type: 'object', additionalProperties: { type: 'integer' } },
        },
    },
}

const Harness: React.FC<{ parameters: SchemaFormParameter[], initial?: Record<string, unknown>, onChange: (value: unknown) => void }> = ({
    parameters, initial, onChange,
}) => {
    const [value, setValue] = useState(() => initial ?? initialFormValue(parameters))
    return (
        <SchemaForm
            parameters={parameters}
            value={value}
            onChange={next => {
                setValue(next)
                onChange(next)
            }}
        />
    )
}

/** Opens a node of the tree: a parameter starts folded, however many fields it holds. */
const open = async (path: string) => {
    const node = screen.getByTestId(`value-${path}`).closest('.ant-tree-treenode')
    await userEvent.click(node?.querySelector('.ant-tree-switcher') as HTMLElement)
}

describe('SchemaForm', () => {
    it('starts every structured parameter created, so the user has its fields to fill in', () => {
        expect(initialFormValue([policy, { name: 'age', schema: { type: 'integer' } }])).toEqual({ policy: {} })
    })

    it('starts a parameter with the defaults its type declares, null fields left out', async () => {
        const withDefaults = { ...policy, value: { state: 'NY', number: null } }
        expect(initialFormValue([withDefaults])).toEqual({ policy: { state: 'NY' } })
        render(<Harness onChange={vi.fn()} parameters={[withDefaults]} />)

        // A parameter starts folded; opening it shows its fields.
        expect(screen.queryByTestId('value-policy.state')).toBeNull()
        await open('policy')
        expect(screen.getByTestId('value-policy.state')).toHaveTextContent('"NY"')
        expect(screen.getByTestId('value-policy.number')).toHaveTextContent('null')
    })

    it('shows every field of a created object as null and edits a plain value behind its pencil', async () => {
        const onChange = vi.fn()
        render(<Harness onChange={onChange} parameters={[policy]} />)

        expect(screen.getByTestId('value-policy')).toHaveTextContent('{0 fields}')
        await open('policy')
        expect(screen.getByTestId('value-policy.number')).toHaveTextContent('null')
        await userEvent.click(screen.getByTestId('edit-policy.number'))
        await userEvent.type(screen.getByTestId('input-policy.number'), 'P-1{enter}')
        expect(onChange).toHaveBeenLastCalledWith({ policy: { number: 'P-1' } })
        expect(screen.getByTestId('value-policy.number')).toHaveTextContent('"P-1"')

        await userEvent.click(screen.getByTestId('edit-policy.age'))
        await userEvent.type(screen.getByTestId('input-policy.age'), '42{enter}')
        expect(onChange).toHaveBeenLastCalledWith({ policy: { number: 'P-1', age: 42 } })

        await userEvent.click(screen.getByTestId('clear-policy.number'))
        expect(onChange).toHaveBeenLastCalledWith({ policy: { age: 42 } })
    })

    it('offers the values of an enumeration and true or false for a boolean', async () => {
        const onChange = vi.fn()
        render(<Harness onChange={onChange} parameters={[policy]} />)

        await open('policy')
        await userEvent.click(screen.getByTestId('edit-policy.state'))
        await userEvent.click(await screen.findByTitle('NY'))
        expect(onChange).toHaveBeenLastCalledWith({ policy: { state: 'NY' } })

        await userEvent.click(screen.getByTestId('edit-policy.active'))
        await userEvent.click(await screen.findByTitle('input.no'))
        expect(onChange).toHaveBeenLastCalledWith({ policy: { state: 'NY', active: false } })
    })

    it('writes a date as the ISO text the rules read', async () => {
        const onChange = vi.fn()
        render(<Harness onChange={onChange} parameters={[policy]} />)

        await open('policy')
        await userEvent.click(screen.getByTestId('edit-policy.since'))
        await userEvent.type(screen.getByTestId('input-policy.since'), '2024-03-15{enter}')
        expect(onChange).toHaveBeenLastCalledWith({ policy: { since: '2024-03-15' } })
    })

    it('creates a nested object with its fields, and clears it back to null', async () => {
        const onChange = vi.fn()
        render(<Harness onChange={onChange} parameters={[policy]} />)

        await open('policy')
        expect(screen.queryByTestId('edit-policy.owner.name')).toBeNull()
        await userEvent.click(screen.getByTestId('create-policy.owner'))
        // A created object starts with the defaults its datatype declares.
        expect(onChange).toHaveBeenLastCalledWith({ policy: { owner: { vip: false } } })
        expect(screen.getByTestId('value-policy.owner.vip')).toHaveTextContent('false')
        expect(screen.getByTestId('value-policy.owner.name')).toHaveTextContent('null')

        await userEvent.click(screen.getByTestId('clear-policy.owner'))
        expect(onChange).toHaveBeenLastCalledWith({ policy: {} })
        expect(screen.getByTestId('create-policy.owner')).toBeInTheDocument()
    })

    it('grows a list with null slots, initialises an element on request and removes one', async () => {
        const onChange = vi.fn()
        render(<Harness onChange={onChange} parameters={[policy]} />)

        await open('policy')
        await userEvent.click(screen.getByTestId('create-policy.drivers'))
        await userEvent.click(screen.getByTestId('add-policy.drivers'))
        await userEvent.click(screen.getByTestId('add-policy.drivers'))
        expect(onChange).toHaveBeenLastCalledWith({ policy: { drivers: [null, null]} })

        await userEvent.click(screen.getByTestId('create-policy.drivers[1]'))
        await userEvent.click(screen.getByTestId('edit-policy.drivers[1].name'))
        await userEvent.type(screen.getByTestId('input-policy.drivers[1].name'), 'Bob{enter}')
        expect(onChange).toHaveBeenLastCalledWith({ policy: { drivers: [null, { name: 'Bob' }]} })

        await userEvent.click(screen.getByTestId('remove-policy.drivers[0]'))
        expect(onChange).toHaveBeenLastCalledWith({ policy: { drivers: [{ name: 'Bob' }]} })
        // The element that moved up keeps its fields in view, under the position it holds now.
        expect(screen.getByTestId('value-policy.drivers[0].name')).toHaveTextContent('"Bob"')
    })

    it('grows a map entry by entry with editable keys', async () => {
        const onChange = vi.fn()
        render(<Harness onChange={onChange} parameters={[policy]} />)

        await open('policy')
        await userEvent.click(screen.getByTestId('create-policy.limits'))
        await userEvent.click(screen.getByTestId('add-policy.limits'))
        expect(onChange).toHaveBeenLastCalledWith({ policy: { limits: { '': null } } })
        await userEvent.type(screen.getByTestId('key-policy.limits[0]'), 'max')
        await userEvent.click(screen.getByTestId('edit-policy.limits[0]'))
        await userEvent.type(screen.getByTestId('input-policy.limits[0]'), '5{enter}')
        expect(onChange).toHaveBeenLastCalledWith({ policy: { limits: { max: 5 } } })

        // A second entry comes under a name of its own, and a name another entry carries is refused.
        await userEvent.click(screen.getByTestId('add-policy.limits'))
        expect(onChange).toHaveBeenLastCalledWith({ policy: { limits: { max: 5, '': null } } })
        await userEvent.type(screen.getByTestId('key-policy.limits[1]'), 'max')
        await userEvent.click(screen.getByTestId('edit-policy.limits[1]'))
        expect(onChange).toHaveBeenLastCalledWith({ policy: { limits: { max: 5, '': null } } })
    })

    it('edits a value without a schema as JSON text and reports text that does not parse', async () => {
        const onChange = vi.fn()
        render(<Harness onChange={onChange} parameters={[{ name: 'any' }]} />)

        await userEvent.click(screen.getByTestId('edit-any'))
        await userEvent.type(screen.getByTestId('input-any'), '{{"a"')
        expect(screen.getByText(/input.jsonInvalid/)).toBeInTheDocument()
        expect(onChange).not.toHaveBeenCalled()
        await userEvent.type(screen.getByTestId('input-any'), ':1}')
        expect(onChange).toHaveBeenLastCalledWith({ any: { a: 1 } })
    })

    it('keeps the JSON field open while its text does not parse, and closes once it does', async () => {
        render(<Harness onChange={vi.fn()} parameters={[{ name: 'any' }]} />)

        await userEvent.click(screen.getByTestId('edit-any'))
        await userEvent.type(screen.getByTestId('input-any'), '{{"a"')
        await userEvent.tab()

        // Closing on text that is not a value would take the reason away and keep the value from before.
        expect(screen.getByTestId('input-any')).toBeInTheDocument()
        expect(screen.getByText(/input.jsonInvalid/)).toBeInTheDocument()

        await userEvent.type(screen.getByTestId('input-any'), ':1}')
        await userEvent.tab()

        await waitFor(() => expect(screen.queryByTestId('input-any')).toBeNull())
    })
})
