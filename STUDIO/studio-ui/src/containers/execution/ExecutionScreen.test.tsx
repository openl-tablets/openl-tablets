import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ExecutionScreen } from './ExecutionScreen'

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

describe('ExecutionScreen', () => {
    it('shows the results with their actions and Close, and no dialog', async () => {
        const close = vi.fn()
        render(
            <ExecutionScreen extra={<button type="button">Save to Excel</button>} onClose={close} subtitle="2 tests" title="Run">
                Results
            </ExecutionScreen>
        )

        expect(screen.getByRole('heading', { name: 'Run' })).toBeInTheDocument()
        expect(screen.getByText('2 tests')).toBeInTheDocument()
        expect(screen.getByText('Results')).toBeInTheDocument()
        expect(screen.getByText('Save to Excel')).toBeInTheDocument()
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
        await userEvent.click(screen.getByTestId('execution-close'))
        expect(close).toHaveBeenCalledOnce()
    })
})
