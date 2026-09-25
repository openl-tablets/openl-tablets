import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Modal } from 'antd'
import { describe, expect, it, vi } from 'vitest'
import { useDiscardConfirm } from './useDiscardConfirm'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

vi.mock('antd', async () => {
    const actual = await vi.importActual<typeof import('antd')>('antd')
    const { withStaticApp } = await import('testing/staticAntdApp')
    return withStaticApp({ ...actual, Modal: { ...actual.Modal, confirm: vi.fn() } })
})

const Reader = ({ dirty, onRead }: { dirty: boolean, onRead: () => void }) => {
    const confirmDiscard = useDiscardConfirm(dirty)
    return <button onClick={() => confirmDiscard(onRead)} type="button">read</button>
}

const read = async (dirty: boolean) => {
    const onRead = vi.fn()
    render(<Reader dirty={dirty} onRead={onRead} />)

    await userEvent.click(screen.getByText('read'))

    return onRead
}

describe('useDiscardConfirm', () => {

    it('reads the table again without a word when nothing of it is waiting to be saved', async () => {
        const onRead = await read(false)

        expect(onRead).toHaveBeenCalled()
        expect(Modal.confirm).not.toHaveBeenCalled()
    })

    it('asks before reading over cells the reader has written and not saved', async () => {
        const onRead = await read(true)

        // What is about to be lost is theirs, so nothing is read again until they have said so.
        expect(onRead).not.toHaveBeenCalled()
        const asked = vi.mocked(Modal.confirm).mock.calls.at(-1)?.[0]
        expect(asked?.title).toBe('browser.module.edit_leaving')
        expect(asked?.content).toBe('browser.module.edit_reloading_message')
        await asked?.onOk?.()
        expect(onRead).toHaveBeenCalled()
    })
})
