import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { applyProjectTableTheme } from '../../services/tables'
import { ApplyProjectThemeModal } from './ApplyProjectThemeModal'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

vi.mock('../../services/tables', () => ({ applyProjectTableTheme: vi.fn() }))

describe('ApplyProjectThemeModal', () => {
    beforeEach(() => {
        vi.mocked(applyProjectTableTheme).mockReset().mockResolvedValue({ themed: ['t1'], skipped: []})
    })

    const draw = (over: Partial<Parameters<typeof ApplyProjectThemeModal>[0]> = {}) => {
        const onApplied = vi.fn()
        const onClose = vi.fn()
        render(<ApplyProjectThemeModal open onApplied={onApplied} onClose={onClose} projectId="p1" {...over} />)
        return { onApplied, onClose }
    }

    it('writes the table theme into the project once the reader confirms it', async () => {
        const { onApplied, onClose } = draw()

        expect(screen.getByText('browser.module.apply_theme_project_body')).toBeInTheDocument()
        await userEvent.click(screen.getByTestId('apply-project-theme-ok'))

        await waitFor(() => expect(onClose).toHaveBeenCalled())
        expect(applyProjectTableTheme).toHaveBeenCalledWith('p1')
        expect(onApplied).toHaveBeenCalled()
    })

    it('stays open when the theme could not be written', async () => {
        // The failure has already been told to the reader, who may try again from here.
        vi.mocked(applyProjectTableTheme).mockResolvedValue(null)
        const { onApplied, onClose } = draw()

        await userEvent.click(screen.getByTestId('apply-project-theme-ok'))

        await waitFor(() => expect(applyProjectTableTheme).toHaveBeenCalled())
        expect(onApplied).not.toHaveBeenCalled()
        expect(onClose).not.toHaveBeenCalled()
    })

    it('says the project waits to be verified, and writes nothing until it is', () => {
        draw({ verifyNeeded: true })

        expect(screen.getByTestId('apply-project-theme-verify')).toHaveTextContent('browser.module.theme_verify_first')
        expect(screen.getByTestId('apply-project-theme-ok')).toBeDisabled()
        expect(applyProjectTableTheme).not.toHaveBeenCalled()
    })
})
