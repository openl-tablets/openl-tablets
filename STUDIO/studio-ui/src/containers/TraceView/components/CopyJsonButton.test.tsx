import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { TraceParameterValue } from 'types/trace'
import CopyJsonButton from 'containers/TraceView/components/CopyJsonButton'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

const param = (over: Partial<TraceParameterValue> = {}): TraceParameterValue => ({
    name: 'premium',
    description: 'Double',
    lazy: false,
    value: 42,
    ...over,
})

const writeText = vi.fn().mockResolvedValue(undefined)

beforeAll(() => {
    Object.defineProperty(navigator, 'clipboard', {
        configurable: true,
        value: { writeText },
    })
})

describe('CopyJsonButton', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('renders nothing when there is no data', () => {
        const { container } = render(<CopyJsonButton data={undefined} tooltipKey="copy.result" />)
        expect(container).toBeEmptyDOMElement()
    })

    it('renders nothing for an empty parameter array', () => {
        const { container } = render(<CopyJsonButton data={[]} tooltipKey="copy.parameters" />)
        expect(container).toBeEmptyDOMElement()
    })

    it('copies a single result value as JSON on click', async () => {
        render(<CopyJsonButton data={param({ name: 'out', value: { rate: 1.5 } })} tooltipKey="copy.result" />)

        await userEvent.click(screen.getByRole('button'))
        await waitFor(() => expect(writeText).toHaveBeenCalledTimes(1))
        // The result copies just its value, pretty-printed.
        expect(writeText).toHaveBeenCalledWith(JSON.stringify({ rate: 1.5 }, null, 2))
    })

    it('copies an array of parameters as a { name: value } object', async () => {
        render(
            <CopyJsonButton
                data={[param({ name: 'age', value: 30 }), param({ name: 'state', value: 'NY' })]}
                tooltipKey="copy.parameters"
            />
        )

        await userEvent.click(screen.getByRole('button'))
        await waitFor(() => expect(writeText).toHaveBeenCalledTimes(1))
        expect(writeText).toHaveBeenCalledWith(JSON.stringify({ age: 30, state: 'NY' }, null, 2))
    })

    it('disables copy while a parameter still has an unfetched lazy value', () => {
        render(
            <CopyJsonButton
                data={[param({ name: 'big', lazy: true, value: undefined })]}
                tooltipKey="copy.parameters"
            />
        )
        expect(screen.getByRole('button')).toBeDisabled()
    })

    it('does not copy when disabled (unfetched lazy value)', async () => {
        render(<CopyJsonButton data={param({ lazy: true, value: undefined })} tooltipKey="copy.result" />)

        // The button is disabled, so clicking it does nothing.
        await userEvent.click(screen.getByRole('button'))
        expect(writeText).not.toHaveBeenCalled()
    })
})
