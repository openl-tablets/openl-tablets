import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { formatProject } from '../../services/tables'
import { ReformatExcelFilesModal } from './ReformatExcelFilesModal'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

vi.mock('../../services/tables', () => ({ formatProject: vi.fn() }))

describe('ReformatExcelFilesModal', () => {
    beforeEach(() => {
        vi.mocked(formatProject).mockReset().mockResolvedValue({ formatted: ['t1'], skipped: []})
    })

    const draw = () => {
        const onFormatted = vi.fn()
        const onClose = vi.fn()
        render(<ReformatExcelFilesModal open onClose={onClose} onFormatted={onFormatted} projectId="p1" />)
        return { onFormatted, onClose }
    }

    it('says what reformatting does before it formats the project', async () => {
        const { onFormatted, onClose } = draw()
        expect(screen.getByText('browser.module.reformat_body')).toBeInTheDocument()

        await userEvent.click(screen.getByTestId('reformat-ok'))

        await waitFor(() => expect(onClose).toHaveBeenCalled())
        expect(formatProject).toHaveBeenCalledWith('p1')
        expect(onFormatted).toHaveBeenCalled()
    })

    it('stays open when the Excel files could not be formatted', async () => {
        // The failure has already been told to the reader, who may try again from here.
        vi.mocked(formatProject).mockResolvedValue(null)
        const { onFormatted, onClose } = draw()

        await userEvent.click(screen.getByTestId('reformat-ok'))

        await waitFor(() => expect(formatProject).toHaveBeenCalled())
        expect(onFormatted).not.toHaveBeenCalled()
        expect(onClose).not.toHaveBeenCalled()
    })

    it('formats nothing when the reader cancels', async () => {
        const { onClose } = draw()

        await userEvent.keyboard('{Escape}')

        expect(onClose).toHaveBeenCalled()
        expect(formatProject).not.toHaveBeenCalled()
    })
})
