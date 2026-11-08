import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { MessagesToolbar } from './MessagesToolbar'

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

const toolbar = (props: Partial<Parameters<typeof MessagesToolbar>[0]> = {}) => render(
    <MessagesToolbar
        canAddKey
        canAddLanguage
        canRedo={false}
        canUndo={false}
        dirty={false}
        onAddKey={vi.fn()}
        onAddLanguage={vi.fn()}
        onRedo={vi.fn()}
        onSave={vi.fn()}
        onUndo={vi.fn()}
        saving={false}
        {...props}
    />
)

describe('MessagesToolbar', () => {
    it('names each action under its picture, as the band over a table does', () => {
        toolbar()

        expect(screen.getByTestId('localization-save')).toHaveTextContent('save')
        expect(screen.getByTestId('localization-add-key')).toHaveTextContent('add_key')
        expect(screen.getByTestId('localization-add-language')).toHaveTextContent('add_language')
    })

    it('offers Remove Key on the page of a key alone', async () => {
        const onRemoveKey = vi.fn()
        const { unmount } = toolbar({ onRemoveKey })

        await userEvent.click(screen.getByTestId('localization-remove-key'))
        expect(onRemoveKey).toHaveBeenCalled()

        unmount()
        toolbar()
        expect(screen.queryByTestId('localization-remove-key')).toBeNull()
    })

    it('takes the last step back and does it again, as the band over a table does', async () => {
        const onUndo = vi.fn()
        const onRedo = vi.fn()
        toolbar({ dirty: true, canUndo: true, onUndo, onRedo })

        expect(screen.getByTestId('localization-save')).toBeEnabled()
        expect(screen.getByTestId('localization-redo')).toBeDisabled()
        await userEvent.click(screen.getByTestId('localization-undo'))
        expect(onUndo).toHaveBeenCalled()
    })
})
