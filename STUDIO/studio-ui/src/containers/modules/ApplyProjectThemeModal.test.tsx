import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { applyProjectTableTheme, getTableThemes } from '../../services/tables'
import { useUserStore } from '../../store'
import { ApplyProjectThemeModal } from './ApplyProjectThemeModal'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

vi.mock('../../services/tables', () => ({ applyProjectTableTheme: vi.fn(), getTableThemes: vi.fn() }))

const THEMES = [{ id: 'default', name: 'Default' }, { id: 'green', name: 'Green' }]

describe('ApplyProjectThemeModal', () => {
    beforeEach(() => {
        vi.mocked(applyProjectTableTheme).mockReset().mockResolvedValue({ themed: ['t1'], skipped: []})
        vi.mocked(getTableThemes).mockResolvedValue(THEMES)
        useUserStore.setState({ userProfile: { tableTheme: 'green' } as never })
    })

    const draw = () => {
        const onApplied = vi.fn()
        const onClose = vi.fn()
        render(<ApplyProjectThemeModal open onApplied={onApplied} onClose={onClose} projectId="p1" />)
        return { onApplied, onClose }
    }

    it('starts from the theme the reader\'s settings name', async () => {
        const { onApplied, onClose } = draw()
        await waitFor(() => expect(screen.getByTestId('apply-project-theme-ok')).toBeEnabled())

        await userEvent.click(screen.getByTestId('apply-project-theme-ok'))

        await waitFor(() => expect(onClose).toHaveBeenCalled())
        expect(applyProjectTableTheme).toHaveBeenCalledWith('p1', 'green')
        expect(onApplied).toHaveBeenCalled()
    })

    it('writes the theme the reader chooses into the project', async () => {
        draw()
        await waitFor(() => expect(screen.getByTestId('apply-project-theme-ok')).toBeEnabled())

        await userEvent.click(screen.getByRole('combobox'))
        await userEvent.click(await screen.findByTitle('Default'))
        await userEvent.click(screen.getByTestId('apply-project-theme-ok'))

        await waitFor(() => expect(applyProjectTableTheme).toHaveBeenCalledWith('p1', 'default'))
    })

    it('forgets the theme picked once the dialog is closed, and starts again from the reader\'s own', async () => {
        const onClose = vi.fn()
        const dialog = (open: boolean) => (
            <ApplyProjectThemeModal onApplied={vi.fn()} onClose={onClose} open={open} projectId="p1" />
        )
        const { rerender } = render(dialog(true))
        await waitFor(() => expect(screen.getByTestId('apply-project-theme-ok')).toBeEnabled())
        await userEvent.click(screen.getByRole('combobox'))
        await userEvent.click(await screen.findByTitle('Default'))

        await userEvent.keyboard('{Escape}')
        expect(onClose).toHaveBeenCalled()
        rerender(dialog(false))
        rerender(dialog(true))
        await userEvent.click(await screen.findByTestId('apply-project-theme-ok'))

        await waitFor(() => expect(applyProjectTableTheme).toHaveBeenCalledWith('p1', 'green'))
    })

    it('stays open when the theme could not be written', async () => {
        // The failure has already been told to the reader, who may try again from here.
        vi.mocked(applyProjectTableTheme).mockResolvedValue(null)
        const { onApplied, onClose } = draw()
        await waitFor(() => expect(screen.getByTestId('apply-project-theme-ok')).toBeEnabled())

        await userEvent.click(screen.getByTestId('apply-project-theme-ok'))

        await waitFor(() => expect(applyProjectTableTheme).toHaveBeenCalled())
        expect(onApplied).not.toHaveBeenCalled()
        expect(onClose).not.toHaveBeenCalled()
    })

    it('offers nothing to write while Studio offers no theme', async () => {
        vi.mocked(getTableThemes).mockResolvedValue([])
        draw()

        await waitFor(() => expect(getTableThemes).toHaveBeenCalled())

        expect(screen.getByTestId('apply-project-theme-ok')).toBeDisabled()
    })
})
