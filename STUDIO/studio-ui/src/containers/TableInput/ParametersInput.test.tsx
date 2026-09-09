import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ParametersInput, type ParametersInputValue } from 'containers/TableInput/ParametersInput'

// CodeMirror does not take typed input under jsdom. A plain text area stands in for the editor.
vi.mock('containers/projects/CodeEditor', () => ({
    CodeEditor: ({ value, onChange }: { value: string, onChange: (value: string) => void }) => (
        <textarea data-testid="json-editor" onChange={event => onChange(event.target.value)} value={value} />
    ),
}))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

const parameters = [
    { name: 'age', description: 'int', lazy: false, schema: { type: 'integer' } },
    { name: 'policy', description: 'Policy', lazy: false, schema: { type: 'object', properties: { number: { type: 'string' } } } },
]

const runtimeContext = { name: 'runtimeContext', description: 'Context', lazy: false, schema: { type: 'object', properties: { lob: { type: 'string' } } } }

const lastValue = (onChange: ReturnType<typeof vi.fn>): ParametersInputValue =>
    onChange.mock.calls.at(-1)?.[0] as ParametersInputValue

const parsed = (onChange: ReturnType<typeof vi.fn>) => JSON.parse(lastValue(onChange).inputJson)

/** Opens a node of the tree: a parameter starts folded, however many fields it holds. */
const openNode = async (path: string) => {
    const node = screen.getByTestId(`value-${path}`).closest('.ant-tree-treenode')
    await userEvent.click(node?.querySelector('.ant-tree-switcher') as HTMLElement)
}

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
})
