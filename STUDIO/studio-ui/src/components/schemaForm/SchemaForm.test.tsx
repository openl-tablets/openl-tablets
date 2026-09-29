import React, { useState } from 'react'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { initialFormValue, SchemaForm, type SchemaFormParameter } from 'components/schemaForm/SchemaForm'
import { keysOf, nameEntry, openNode as open } from 'testing/schemaTree'

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

/** A list whose elements say nothing about themselves, so each is written as JSON text. */
const notes: SchemaFormParameter = {
    name: 'notes',
    schema: { type: 'array', items: {} },
}

/** A map of maps, and a list of structures each holding a map: what stands under a node that moves. */
const nested: SchemaFormParameter = {
    name: 'nested',
    schema: {
        type: 'object',
        properties: {
            outer: { type: 'object', additionalProperties: { type: 'object', additionalProperties: { type: 'integer' } } },
            rows: {
                type: 'array',
                items: { type: 'object', properties: { inner: { type: 'object', additionalProperties: { type: 'integer' } } } },
            },
        },
    },
}

/** A field of codes with names listed for some of them. */
const region: SchemaFormParameter = {
    name: 'region',
    schema: { type: 'object', properties: { code: { type: 'string', enum: ['QC', 'HQ', 'constructor']} } },
    labels: { code: { QC: 'Québec', HQ: 'Hors Québec' } },
}

/** A map of structures: an entry holds fields of its own, under the entry's key. */
const quotes: SchemaFormParameter = {
    name: 'quotes',
    type: 'Quotes',
    schema: { type: 'object', additionalProperties: { type: 'object', properties: { b: { type: 'string' } } } },
}

const Harness: React.FC<{
    parameters: SchemaFormParameter[]
    initial?: Record<string, unknown>
    /** A value arriving from outside the form, put in by the button beside it — a run read back, or JSON text. */
    arriving?: Record<string, unknown>
    /** Watched only by a test that asserts on what the form emits. */
    onChange?: ((value: unknown) => void) | undefined
}> = ({ parameters, initial, arriving, onChange }) => {
    const [value, setValue] = useState(() => initial ?? initialFormValue(parameters))
    return (
        <>
            {arriving !== undefined
                && <button data-testid="arrives" onClick={() => setValue(arriving)} type="button">arrives</button>}
            <SchemaForm
                parameters={parameters}
                value={value}
                onChange={next => {
                    setValue(next)
                    onChange?.(next)
                }}
            />
        </>
    )
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

    it('shows the names given for the codes of an enumeration, finds them by name or code, and writes the code', async () => {
        const onChange = vi.fn()
        const named = { ...policy, labels: { state: { AL: 'Alabama', NY: 'New York' } } }
        render(<Harness onChange={onChange} parameters={[named]} />)

        await open('policy')
        await userEvent.click(screen.getByTestId('edit-policy.state'))
        await userEvent.type(screen.getByRole('combobox'), 'york')
        expect(screen.queryByTitle('Alabama')).toBeNull()
        await userEvent.click(await screen.findByTitle('New York'))
        expect(onChange).toHaveBeenLastCalledWith({ policy: { state: 'NY' } })
        expect(screen.getByTestId('value-policy.state')).toHaveTextContent(/^New York$/)

        await userEvent.click(screen.getByTestId('edit-policy.state'))
        await userEvent.type(screen.getByRole('combobox'), 'AL')
        await userEvent.click(await screen.findByTitle('Alabama'))
        expect(onChange).toHaveBeenLastCalledWith({ policy: { state: 'AL' } })
    })

    it('finds a name whatever its case and accents', async () => {
        const onChange = vi.fn()
        render(<Harness onChange={onChange} parameters={[region]} />)

        await open('region')
        await userEvent.click(screen.getByTestId('edit-region.code'))
        await userEvent.type(screen.getByRole('combobox'), 'HORS QUEBEC')
        expect(screen.queryByTitle('Québec')).toBeNull()
        await userEvent.click(await screen.findByTitle('Hors Québec'))
        expect(onChange).toHaveBeenLastCalledWith({ region: { code: 'HQ' } })
    })

    it('shows a code with no name listed for it as it is, even one every object has as a property', async () => {
        render(<Harness initial={{ region: { code: '__proto__' } }} onChange={vi.fn()} parameters={[region]} />)

        await open('region')
        expect(screen.getByTestId('value-region.code')).toHaveTextContent(/^"__proto__"$/)
        await userEvent.click(screen.getByTestId('edit-region.code'))
        expect(await screen.findByTitle('constructor')).toBeInTheDocument()
    })

    it('shows the name of a code written as a number', async () => {
        const tier: SchemaFormParameter = {
            name: 'tier',
            schema: { type: 'object', properties: { grade: { type: 'integer', enum: [1, 2]} } },
            labels: { grade: { 1: 'Gold', 2: 'Silver' } },
        }
        render(<Harness initial={{ tier: { grade: 2 } }} onChange={vi.fn()} parameters={[tier]} />)

        await open('tier')
        expect(screen.getByTestId('value-tier.grade')).toHaveTextContent(/^Silver$/)
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

    it('carries an open editor with the row it belongs to when one before it is removed', async () => {
        render(<Harness parameters={[notes]} />)

        await userEvent.click(screen.getByTestId('add-notes'))
        await open('notes')
        await userEvent.click(screen.getByTestId('add-notes'))
        await userEvent.click(screen.getByTestId('edit-notes[1]'))
        // Text that does not parse keeps the editor open when the pointer leaves it, so nothing typed is lost.
        await userEvent.type(screen.getByTestId('input-notes[1]'), '"half writ')

        await userEvent.click(screen.getByTestId('remove-notes[0]'))

        // The editor stands on the element it was opened on, which holds the place before it now. Left where it
        // was, it would be writing into whatever element came to stand there — or into nothing at all.
        expect(screen.getByTestId('input-notes[0]')).toBeInTheDocument()
        expect(screen.queryByTestId('input-notes[1]')).toBeNull()
    })

    it('grows a map entry by entry with editable keys', async () => {
        const onChange = vi.fn()
        render(<Harness onChange={onChange} parameters={[policy]} />)

        await open('policy')
        await userEvent.click(screen.getByTestId('create-policy.limits'))
        await userEvent.click(screen.getByTestId('add-policy.limits'))
        expect(onChange).toHaveBeenLastCalledWith({ policy: { limits: { '': null } } })
        // An entry is addressed by the key it carries, so naming it moves what is open under it with it.
        await userEvent.type(screen.getByTestId('key-policy.limits[]'), 'max{enter}')
        await userEvent.click(screen.getByTestId('edit-policy.limits[max]'))
        await userEvent.type(screen.getByTestId('input-policy.limits[max]'), '5{enter}')
        expect(onChange).toHaveBeenLastCalledWith({ policy: { limits: { max: 5 } } })

        // A second entry comes under a name of its own, and a name another entry carries is refused.
        await userEvent.click(screen.getByTestId('add-policy.limits'))
        expect(onChange).toHaveBeenLastCalledWith({ policy: { limits: { max: 5, '': null } } })
        await userEvent.type(screen.getByTestId('key-policy.limits[]'), 'max')
        await userEvent.click(screen.getByTestId('edit-policy.limits[]'))
        expect(onChange).toHaveBeenLastCalledWith({ policy: { limits: { max: 5, '': null } } })
    })

    it('keeps an entry with its key when naming one reorders the map', async () => {
        const onChange = vi.fn()
        render(<Harness onChange={onChange} parameters={[policy]} />)

        await open('policy')
        await userEvent.click(screen.getByTestId('create-policy.limits'))
        await userEvent.click(screen.getByTestId('add-policy.limits'))
        await userEvent.type(screen.getByTestId('key-policy.limits[]'), '10{enter}')
        await userEvent.click(screen.getByTestId('edit-policy.limits[10]'))
        await userEvent.type(screen.getByTestId('input-policy.limits[10]'), '1{enter}')

        // A map lists a key that reads as a whole number first, so naming this one rebuilds the map with the
        // rows in another order. Each entry still answers under its own key, with its own value.
        await userEvent.click(screen.getByTestId('add-policy.limits'))
        await userEvent.type(screen.getByTestId('key-policy.limits[]'), '2{enter}')
        await userEvent.click(screen.getByTestId('edit-policy.limits[2]'))
        await userEvent.type(screen.getByTestId('input-policy.limits[2]'), '2{enter}')

        expect(onChange).toHaveBeenLastCalledWith({ policy: { limits: { 2: 2, 10: 1 } } })
        expect(screen.getByTestId('value-policy.limits[10]')).toHaveTextContent('1')
        expect(screen.getByTestId('value-policy.limits[2]')).toHaveTextContent('2')
        // And they are drawn where they were put, however the map itself lists them.
        expect(keysOf('policy.limits')).toEqual(['10', '2'])
    })

    it('draws a map that arrives with its entries in the order it holds them, and keeps that order', async () => {
        const onChange = vi.fn()
        // Read back from a run or written as JSON text: the form was told nothing about these, so they are
        // drawn as the map lists them.
        render(<Harness initial={{ policy: { limits: { 10: 1, 2: 2 } } }} onChange={onChange} parameters={[policy]} />)

        await open('policy')
        await open('policy.limits')
        expect(keysOf('policy.limits')).toEqual(['2', '10'])

        // An entry added to it goes under the rest, and the ones already there do not move.
        await userEvent.click(screen.getByTestId('add-policy.limits'))
        expect(keysOf('policy.limits')).toEqual(['2', '10', ''])
        await userEvent.type(screen.getByTestId('key-policy.limits[]'), '1{enter}')
        expect(keysOf('policy.limits')).toEqual(['2', '10', '1'])
    })

    it('leaves the rows around a removed entry where they are', async () => {
        const onChange = vi.fn()
        render(<Harness onChange={onChange} parameters={[policy]} />)

        await open('policy')
        await userEvent.click(screen.getByTestId('create-policy.limits'))
        for (const key of ['30', '2', '10']) {
            await nameEntry('policy.limits', key)
        }
        expect(keysOf('policy.limits')).toEqual(['30', '2', '10'])

        await userEvent.click(screen.getByTestId('remove-policy.limits[2]'))

        // What the map itself lists is `10, 30`; the rows are where the reader put them.
        expect(keysOf('policy.limits')).toEqual(['30', '10'])
    })

    it('carries the arrangement of a map under an entry that is renamed', async () => {
        const onChange = vi.fn()
        render(<Harness onChange={onChange} parameters={[nested]} />)

        await open('nested')
        await userEvent.click(screen.getByTestId('create-nested.outer'))
        await nameEntry('nested.outer', 'a')
        await userEvent.click(screen.getByTestId('create-nested.outer[a]'))
        await nameEntry('nested.outer[a]', '10')
        await nameEntry('nested.outer[a]', '2')
        expect(keysOf('nested.outer[a]')).toEqual(['10', '2'])

        // The inner map is addressed under the entry holding it, so renaming that entry moves it.
        await userEvent.clear(screen.getByTestId('key-nested.outer[a]'))
        await userEvent.type(screen.getByTestId('key-nested.outer[a]'), 'b{enter}')

        expect(keysOf('nested.outer[b]')).toEqual(['10', '2'])
    })

    it('carries the arrangement of a map under a list element that moves up', async () => {
        const onChange = vi.fn()
        render(<Harness onChange={onChange} parameters={[nested]} />)

        await open('nested')
        await userEvent.click(screen.getByTestId('create-nested.rows'))
        await userEvent.click(screen.getByTestId('add-nested.rows'))
        await userEvent.click(screen.getByTestId('add-nested.rows'))
        // Creating the list opened it, so its slots are already in view.
        await userEvent.click(screen.getByTestId('create-nested.rows[1]'))
        await userEvent.click(screen.getByTestId('create-nested.rows[1].inner'))
        await nameEntry('nested.rows[1].inner', '10')
        await nameEntry('nested.rows[1].inner', '2')
        expect(keysOf('nested.rows[1].inner')).toEqual(['10', '2'])

        // The element before it goes, so this one stands one place earlier — and takes its map with it.
        await userEvent.click(screen.getByTestId('remove-nested.rows[0]'))

        expect(keysOf('nested.rows[0].inner')).toEqual(['10', '2'])
    })

    it('draws a map that arrives where a cleared one stood as it comes', async () => {
        const onChange = vi.fn()
        const arriving = { policy: { limits: { 2: 7, 10: 8 } } }
        render(<Harness arriving={arriving} onChange={onChange} parameters={[policy]} />)

        await open('policy')
        await userEvent.click(screen.getByTestId('create-policy.limits'))
        await nameEntry('policy.limits', '10')
        await nameEntry('policy.limits', '2')
        expect(keysOf('policy.limits')).toEqual(['10', '2'])

        // Cleared away, the map takes its arrangement with it. What arrives in its place is somebody else's,
        // and is drawn the way it comes rather than the way the reader once arranged the map that stood there.
        await userEvent.click(screen.getByTestId('clear-policy.limits'))
        await userEvent.click(screen.getByTestId('arrives'))
        // Nothing of the map that stood here is remembered, down to its rows being folded again.
        await open('policy.limits')

        expect(keysOf('policy.limits')).toEqual(['2', '10'])
    })

    it('keeps the rows apart when a key is written with the characters a path is made of', async () => {
        const onChange = vi.fn()
        const limits = { 'a].b': { b: 'of the entry named a].b' }, a: { b: 'of the entry named a' } }
        render(<Harness initial={{ quotes: limits }} onChange={onChange} parameters={[quotes]} />)

        await open('quotes')
        await open('quotes[a]')

        // Read as a path, the key `a].b` names the field `b` of the entry `a`. The two rows are still two.
        expect(screen.getByTestId('value-quotes[a].b')).toHaveTextContent('of the entry named a')
        expect(screen.getByTestId('value-quotes[a%5D.b]')).toBeInTheDocument()
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
