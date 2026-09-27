import { render, screen, waitFor } from '@testing-library/react'
import { Form, Input } from 'antd'
import { useIsFormChanged } from './useIsFormChanged'

interface Values {
    name?: string
    tags?: string[]
    settings?: { url?: string }
}

/** Holds a value no input can show, such as a list, so the form reports it with the others. */
const Holder = () => null

const Probe = ({ initialValues, values }: { initialValues: Values, values: Values }) => {
    const [form] = Form.useForm<Values>()
    const changed = useIsFormChanged({ form, initialValues })
    return (
        <Form form={form} initialValues={values}>
            <span data-testid="changed">{String(changed)}</span>
            <Form.Item name="name"><Input /></Form.Item>
            <Form.Item name="tags"><Holder /></Form.Item>
            <Form.Item name={['settings', 'url']}><Input /></Form.Item>
        </Form>
    )
}

const saved: Values = { name: 'design', tags: ['a', 'b'], settings: { url: 'http://repo' } }

const changed = () => screen.getByTestId('changed')

describe('useIsFormChanged', () => {
    it.each([
        ['an item of a list', { ...saved, tags: ['a', 'c']}],
        ['the length of a list', { ...saved, tags: ['a']}],
        ['a nested field', { ...saved, settings: { url: 'http://other' } }],
    ])('tells a change of %s', async (_, values) => {
        render(<Probe initialValues={saved} values={values} />)

        await waitFor(() => expect(changed()).toHaveTextContent('true'))
    })

    it('tells the form unchanged once the saved values equal it again', async () => {
        const values = { ...saved, tags: ['a', 'c']}
        const { rerender } = render(<Probe initialValues={saved} values={values} />)
        await waitFor(() => expect(changed()).toHaveTextContent('true'))

        rerender(<Probe initialValues={{ ...saved, tags: ['a', 'c']}} values={values} />)

        await waitFor(() => expect(changed()).toHaveTextContent('false'))
    })
})
