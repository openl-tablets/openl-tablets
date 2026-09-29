import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ParametersInput, type ParametersInputValue } from 'containers/TableInput/ParametersInput'
import { keysOf, nameEntry, openNode } from 'testing/schemaTree'

// CodeMirror does not take typed input under jsdom. A plain text area stands in for the editor.
vi.mock('containers/projects/CodeEditor', () => ({
    CodeEditor: ({ value, onChange }: { value: string, onChange: (value: string) => void }) => (
        <textarea data-testid="json-editor" onChange={event => onChange(event.target.value)} value={value} />
    ),
}))

// A screen is handed a new `t` when the language changes. It is kept here so that a test can hand out
// another one, and stays the same between renders otherwise.
const { translation } = vi.hoisted(() => ({ translation: { t: (key: string) => key } }))

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: translation.t }) }))

const parameters = [
    { name: 'age', description: 'int', lazy: false, schema: { type: 'integer' } },
    { name: 'policy', description: 'Policy', lazy: false, schema: { type: 'object', properties: { number: { type: 'string' } } } },
]

const runtimeContext = { name: 'runtimeContext', description: 'Context', lazy: false, schema: { type: 'object', properties: { lob: { type: 'string' } } } }

const lastValue = (onChange: ReturnType<typeof vi.fn>): ParametersInputValue =>
    onChange.mock.calls.at(-1)?.[0] as ParametersInputValue

const parsed = (onChange: ReturnType<typeof vi.fn>) => JSON.parse(lastValue(onChange).inputJson)


describe('ParametersInput', () => {
    it('writes the form as the structured input, with the context only when it is set', async () => {
        const onChange = vi.fn()
        render(<ParametersInput onChange={onChange} parameters={parameters} runtimeContext={runtimeContext} />)

        expect(parsed(onChange)).toEqual({ params: { policy: {} } })
        await userEvent.click(screen.getByTestId('edit-age'))
        await userEvent.type(screen.getByTestId('input-age'), '7{enter}')
        expect(parsed(onChange)).toEqual({ params: { age: 7, policy: {} } })

        // The context is a line of the same form, under the parameters, and it starts folded.
        expect(screen.queryByTestId('edit-runtime-context.lob')).toBeNull()
        await openNode('runtime-context')
        await userEvent.click(await screen.findByTestId('edit-runtime-context.lob'))
        await userEvent.type(screen.getByTestId('input-runtime-context.lob'), 'Auto{enter}')
        expect(parsed(onChange)).toEqual({ params: { age: 7, policy: {} }, runtimeContext: { lob: 'Auto' } })
    })

    it('shows the names of the codes the context takes and sends the code', async () => {
        const onChange = vi.fn()
        const context = { ...runtimeContext, schema: { type: 'object', properties: { caRegion: { type: 'string', enum: ['QC', 'HQ']} } } }
        render(<ParametersInput onChange={onChange} parameters={[]} runtimeContext={context} />)

        await openNode('runtime-context')
        await userEvent.click(await screen.findByTestId('edit-runtime-context.caRegion'))
        expect(await screen.findByTitle('Hors Québec')).toBeInTheDocument()
        await userEvent.click(screen.getByTitle('Québec'))
        expect(parsed(onChange)).toEqual({ params: {}, runtimeContext: { caRegion: 'QC' } })
        expect(screen.getByTestId('value-runtime-context.caRegion')).toHaveTextContent(/^Québec$/)
    })

    it('keeps a parameter of its own named runtimeContext apart from the context', async () => {
        const onChange = vi.fn()
        const named = { name: 'runtimeContext', description: 'String', lazy: false, schema: { type: 'string' } }
        render(<ParametersInput onChange={onChange} parameters={[named]} runtimeContext={runtimeContext} />)

        await userEvent.click(screen.getByTestId('edit-runtimeContext'))
        await userEvent.type(screen.getByTestId('input-runtimeContext'), 'mine{enter}')
        await openNode('runtime-context')
        await userEvent.click(screen.getByTestId('edit-runtime-context.lob'))
        await userEvent.type(screen.getByTestId('input-runtime-context.lob'), 'Auto{enter}')

        expect(parsed(onChange)).toEqual({ params: { runtimeContext: 'mine' }, runtimeContext: { lob: 'Auto' } })
    })

    it('keeps what the reader arranged in the form while they look at the JSON', async () => {
        const onChange = vi.fn()
        const limits = { name: 'limits', description: 'Map', lazy: false, schema: { type: 'object', additionalProperties: { type: 'integer' } } }
        render(<ParametersInput onChange={onChange} parameters={[...parameters, limits]} />)

        await userEvent.click(screen.getByTestId('add-limits'))
        await openNode('limits')
        await userEvent.type(screen.getByTestId('key-limits[]'), '10{enter}')
        await nameEntry('limits', '2')
        expect(keysOf('limits')).toEqual(['10', '2'])

        // The form is put away rather than taken down, so the rows come back in the order they were put in —
        // and the map comes back open.
        await userEvent.click(screen.getByText('input.json'))
        await userEvent.click(screen.getByText('input.form'))

        expect(keysOf('limits')).toEqual(['10', '2'])
    })

    it('shows the form as JSON, reports text that does not parse, and reads valid text back into the form', async () => {
        const onChange = vi.fn()
        render(<ParametersInput onChange={onChange} parameters={parameters} />)

        await userEvent.click(screen.getByTestId('edit-age'))
        await userEvent.type(screen.getByTestId('input-age'), '7{enter}')
        await userEvent.click(screen.getByText('input.json'))
        const text = await screen.findByTestId('json-editor') as HTMLTextAreaElement
        expect(JSON.parse(text.value)).toEqual({ params: { age: 7, policy: {} } })
        expect(lastValue(onChange).error).toBeUndefined()

        await userEvent.clear(text)
        await userEvent.type(text, '{{"params": {{"age": ')
        expect(lastValue(onChange).error).toBe('input.jsonInvalid')

        await userEvent.clear(text)
        await userEvent.type(text, '{{"age": 9, "policy": {{"number": "P-1"}}')
        expect(lastValue(onChange).error).toBeUndefined()
        await userEvent.click(screen.getByText('input.form'))
        expect(screen.getByTestId('value-age')).toHaveTextContent('9')
        await openNode('policy')
        expect(screen.getByTestId('value-policy.number')).toHaveTextContent('"P-1"')
        expect(parsed(onChange)).toEqual({ params: { age: 9, policy: { number: 'P-1' } } })
    })
    it('starts again when the table is described with other parameters', async () => {
        // Reading a table within the current module only can describe it differently. What was typed for a
        // parameter that is gone must not be sent, and one that has appeared starts from the value the table
        // declares for it.
        const onChange = vi.fn()
        const { rerender } = render(<ParametersInput onChange={onChange} parameters={parameters} />)

        await userEvent.click(screen.getByTestId('edit-age'))
        await userEvent.type(screen.getByTestId('input-age'), '42{enter}')
        expect(parsed(onChange)).toEqual({ params: { age: 42, policy: {} } })

        const other = [{ name: 'limit', description: 'int', lazy: false, schema: { type: 'integer' }, value: 7 }]
        rerender(<ParametersInput onChange={onChange} parameters={other} />)

        expect(parsed(onChange)).toEqual({ params: { limit: 7 } })
    })

    it('keeps what is typed when the table is read again and described in the same words', async () => {
        // Ticking "Within Current Module Only" reads the table again, and the answer is a list of its own
        // every time. Described the same way, it is the same table, and what the reader typed stays.
        const onChange = vi.fn()
        const { rerender } = render(<ParametersInput onChange={onChange} parameters={parameters} />)

        await userEvent.click(screen.getByTestId('edit-age'))
        await userEvent.type(screen.getByTestId('input-age'), '42{enter}')

        rerender(<ParametersInput onChange={onChange} parameters={structuredClone(parameters)} />)

        expect(parsed(onChange)).toEqual({ params: { age: 42, policy: {} } })
    })

    it('keeps what is typed when the same parameters are described by another translation', async () => {
        const onChange = vi.fn()
        const shown = () => (
            <ParametersInput onChange={onChange} parameters={parameters} runtimeContext={runtimeContext} />
        )
        const { rerender } = render(shown())

        await userEvent.click(screen.getByTestId('edit-age'))
        await userEvent.type(screen.getByTestId('input-age'), '42{enter}')

        // The language changed: the label of the context is taken from a new translation, while the table
        // takes the parameters it took before.
        translation.t = (key: string) => key
        rerender(shown())

        expect(parsed(onChange)).toEqual({ params: { age: 42, policy: {} } })
    })

    it('starts the text again too, so what was typed for a parameter that is gone is not sent', async () => {
        // The text is what is sent while JSON is the input shown. Left as it was, the panel would go on
        // sending the JSON typed against the parameters the table was described with before.
        const onChange = vi.fn()
        const { rerender } = render(<ParametersInput onChange={onChange} parameters={parameters} />)

        await userEvent.click(screen.getByText('input.json'))
        const text = await screen.findByTestId('json-editor') as HTMLTextAreaElement
        await userEvent.clear(text)
        await userEvent.type(text, '{{"params": {{"age": 42')
        expect(lastValue(onChange).error).toBe('input.jsonInvalid')

        const other = [{ name: 'limit', description: 'int', lazy: false, schema: { type: 'integer' }, value: 7 }]
        rerender(<ParametersInput onChange={onChange} parameters={other} />)

        // The text starts again from the new declarations, and the reason the old text did not parse goes
        // with it.
        expect(parsed(onChange)).toEqual({ params: { limit: 7 } })
        expect(lastValue(onChange).error).toBeUndefined()
        expect((screen.getByTestId('json-editor') as HTMLTextAreaElement).value).toBe(
            JSON.stringify({ params: { limit: 7 } }, null, 2)
        )
    })
})
