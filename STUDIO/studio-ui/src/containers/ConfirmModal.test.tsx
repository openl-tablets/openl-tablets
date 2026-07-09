import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ConfirmModal, type ConfirmModalDetail } from './ConfirmModal'

vi.mock('antd', async () => {
    const actual = await vi.importActual<typeof import('antd')>('antd')
    const MockModal = ({
        open,
        title,
        children,
        okText,
        cancelText,
        onOk,
        onCancel,
    }: {
        open?: boolean
        title?: React.ReactNode
        children?: React.ReactNode
        okText?: React.ReactNode
        cancelText?: React.ReactNode
        onOk?: () => void
        onCancel?: () => void
    }) =>
        open ? (
            <div role="dialog">
                {title && <div data-testid="modal-title">{title}</div>}
                {children}
                <button onClick={onCancel}>{cancelText}</button>
                <button onClick={onOk}>{okText}</button>
            </div>
        ) : null
    return { ...actual, Modal: MockModal }
})

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

const openConfirm = async (detail: ConfirmModalDetail | null) => {
    await act(async () => {
        window.dispatchEvent(new CustomEvent('openConfirmModal', { detail }))
    })
}

describe('ConfirmModal', () => {
    it('does not render until an event with a message arrives', () => {
        render(<ConfirmModal />)
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })

    it('shows the title and message, runs onOK and closes on confirm', async () => {
        render(<ConfirmModal />)
        const onOK = vi.fn()
        await openConfirm({ title: 'Tests', message: 'Run within current module only?', okButton: 'Run', onOK })

        expect(screen.getByRole('dialog')).toBeInTheDocument()
        expect(screen.getByTestId('modal-title')).toHaveTextContent('Tests')
        expect(screen.getByText('Run within current module only?')).toBeInTheDocument()

        await userEvent.click(screen.getByRole('button', { name: 'Run' }))

        expect(onOK).toHaveBeenCalledTimes(1)
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    })

    it('closes on cancel without running onOK', async () => {
        render(<ConfirmModal />)
        const onOK = vi.fn()
        await openConfirm({ message: 'Are you sure?', onOK })

        await userEvent.click(screen.getByRole('button', { name: 'common:btn.cancel' }))

        expect(onOK).not.toHaveBeenCalled()
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    })

    it('falls back to the default title and button labels', async () => {
        render(<ConfirmModal />)
        await openConfirm({ message: 'Proceed?' })

        expect(screen.getByTestId('modal-title')).toHaveTextContent('common:confirm.title')
        expect(screen.getByRole('button', { name: 'common:btn.ok' })).toBeInTheDocument()
        expect(screen.getByRole('button', { name: 'common:btn.cancel' })).toBeInTheDocument()
    })

    it('tolerates a missing onOK callback', async () => {
        render(<ConfirmModal />)
        await openConfirm({ message: 'Proceed?', okButton: 'Yes' })

        await userEvent.click(screen.getByRole('button', { name: 'Yes' }))

        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    })
})
