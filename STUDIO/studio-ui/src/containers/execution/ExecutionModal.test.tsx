import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ExecutionModal, ExecutionWindowContext } from './ExecutionModal'

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

describe('ExecutionModal', () => {
    it('offers Open in Window in the editor dialog', async () => {
        const open = vi.fn(() => true)
        render(
            <ExecutionWindowContext.Provider value={{ open }}>
                <ExecutionModal onClose={vi.fn()} title="Test Results">Results</ExecutionModal>
            </ExecutionWindowContext.Provider>
        )

        await userEvent.click(screen.getByTestId('execution-open-window'))
        expect(open).toHaveBeenCalledOnce()
        expect(within(screen.getByTestId('execution-open-window')).getByRole('img', { name: 'export' })).toBeInTheDocument()
    })

    it('keeps the dialog and explains when the browser blocks the separate window', async () => {
        render(
            <ExecutionWindowContext.Provider value={{ open: () => false }}>
                <ExecutionModal onClose={vi.fn()} title="Test Results">Results</ExecutionModal>
            </ExecutionWindowContext.Provider>
        )

        await userEvent.click(screen.getByTestId('execution-open-window'))
        expect(screen.getByText('windowBlocked')).toBeInTheDocument()
        expect(screen.getByRole('dialog')).toBeInTheDocument()
    })

    it('shows the same actions without a dialog in the separate window', () => {
        render(
            <ExecutionWindowContext.Provider value={{ standalone: true }}>
                <ExecutionModal extra={<button type="button">Save to Excel</button>} onClose={vi.fn()} title="Run">
                    Results
                </ExecutionModal>
            </ExecutionWindowContext.Provider>
        )

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
        expect(screen.getByText('Save to Excel')).toBeInTheDocument()
        expect(screen.getByTestId('execution-close')).toBeInTheDocument()
        expect(screen.queryByTestId('execution-open-window')).not.toBeInTheDocument()
    })
})
