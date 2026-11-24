import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { AddLanguageModal } from './AddLanguageModal'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en', resolvedLanguage: 'en' } }) }
})

/** Picks an option of a select by a piece of its name, as the reader types it. */
const pick = async (testId: string, typed: string, option: string) => {
    const select = await screen.findByTestId(testId)
    await userEvent.click(within(select).getByRole('combobox'))
    await userEvent.type(within(select).getByRole('combobox'), typed)
    await userEvent.click(await screen.findByTitle(option))
}

describe('AddLanguageModal', () => {
    it('adds a language, named by its code', async () => {
        const added = vi.fn()
        render(<AddLanguageModal open onAdd={added} onClose={vi.fn()} tags={['']} />)

        await pick('localization-add-language-language', 'Germ', 'German (de)')
        await userEvent.click(screen.getByTestId('localization-add-language-ok'))

        await waitFor(() => expect(added).toHaveBeenCalledWith('de'))
    })

    it('adds the language of a country, found by its code', async () => {
        const added = vi.fn()
        render(<AddLanguageModal open onAdd={added} onClose={vi.fn()} tags={['', 'de']} />)

        await pick('localization-add-language-language', 'Germ', 'German (de)')
        await pick('localization-add-language-country', '(LU)', 'Luxembourg (LU)')
        await userEvent.click(screen.getByTestId('localization-add-language-ok'))

        await waitFor(() => expect(added).toHaveBeenCalledWith('de_LU'))
    })

    it('opens empty again after a language is added or the dialog is cancelled', async () => {
        const added = vi.fn()
        const closed = vi.fn()
        const dialog = (open: boolean) => <AddLanguageModal onAdd={added} onClose={closed} open={open} tags={['']} />
        const { rerender } = render(dialog(true))
        // Opened again at once, before its closing ends: the choice is cleared all the same.
        const reopen = () => {
            rerender(dialog(false))
            rerender(dialog(true))
        }

        await pick('localization-add-language-language', 'Portu', 'Portuguese (pt)')
        await pick('localization-add-language-country', '(BR)', 'Brazil (BR)')
        await userEvent.click(screen.getByTestId('localization-add-language-ok'))
        await waitFor(() => expect(added).toHaveBeenCalledWith('pt_BR'))

        reopen()
        await pick('localization-add-language-language', 'Germ', 'German (de)')
        await userEvent.click(screen.getByTestId('localization-add-language-ok'))
        await waitFor(() => expect(added).toHaveBeenLastCalledWith('de'))

        reopen()
        await pick('localization-add-language-language', 'Fren', 'French (fr)')
        await pick('localization-add-language-country', '(BR)', 'Brazil (BR)')
        await userEvent.click(screen.getByRole('button', { name: 'common:btn.cancel' }))
        expect(closed).toHaveBeenCalled()

        reopen()
        await userEvent.click(await screen.findByTestId('localization-add-language-ok'))
        expect(await screen.findByText('language_required')).toBeInTheDocument()
        expect(added).toHaveBeenCalledTimes(2)

        await pick('localization-add-language-language', 'Germ', 'German (de)')
        await userEvent.click(screen.getByTestId('localization-add-language-ok'))
        await waitFor(() => expect(added).toHaveBeenCalledTimes(3))
        expect(added).toHaveBeenLastCalledWith('de')
    })

    it('asks for the language, and adds none the messages have already', async () => {
        const added = vi.fn()
        render(<AddLanguageModal open onAdd={added} onClose={vi.fn()} tags={['', 'fr']} />)

        await userEvent.click(await screen.findByTestId('localization-add-language-ok'))
        expect(await screen.findByText('language_required')).toBeInTheDocument()

        await pick('localization-add-language-language', 'Fren', 'French (fr)')
        await userEvent.click(screen.getByTestId('localization-add-language-ok'))
        expect(await screen.findByText('language_exists')).toBeInTheDocument()
        expect(added).not.toHaveBeenCalled()
    })
})
