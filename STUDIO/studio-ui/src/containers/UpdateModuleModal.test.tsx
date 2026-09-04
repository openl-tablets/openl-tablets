import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { notification } from 'antd'
import { UpdateModuleModal, type UpdateModuleModalDetail } from './UpdateModuleModal'
import { replaceFile } from '../services/files'
import type { MockedFunction } from 'vitest'

vi.mock('../services/files', () => ({
    replaceFile: vi.fn(),
}))

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
        okButtonProps,
    }: {
        open?: boolean
        title?: React.ReactNode
        children?: React.ReactNode
        okText?: React.ReactNode
        cancelText?: React.ReactNode
        onOk?: () => void
        onCancel?: () => void
        okButtonProps?: { disabled?: boolean, loading?: boolean }
    }) =>
        open ? (
            <div role="dialog">
                {title && <div data-testid="modal-title">{title}</div>}
                {children}
                <button onClick={onCancel}>{cancelText}</button>
                <button disabled={okButtonProps?.disabled} onClick={onOk}>{okText}</button>
            </div>
        ) : null
    return { ...actual, Modal: MockModal }
})

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

const mockReplaceFile = replaceFile as MockedFunction<typeof replaceFile>

const openModal = async (onSuccess = vi.fn()) => {
    await act(async () => {
        window.dispatchEvent(new CustomEvent<UpdateModuleModalDetail>('openUpdateModuleModal', {
            detail: { projectId: 'proj-id', modulePath: 'rules/Main.xlsx', onSuccess },
        }))
    })
    return onSuccess
}

const fileInput = (): HTMLInputElement => {
    const input = document.querySelector('input[type="file"]')
    if (!(input instanceof HTMLInputElement)) {
        throw new Error('upload input is not rendered')
    }
    return input
}

const okButton = () => screen.getByRole('button', { name: 'project:update_module_modal.confirm_button' })

describe('UpdateModuleModal', () => {
    let infoSpy: ReturnType<typeof vi.spyOn>

    beforeEach(() => {
        vi.clearAllMocks()
        infoSpy = vi.spyOn(notification, 'info').mockImplementation(() => {})
        vi.spyOn(notification, 'success').mockImplementation(() => {})
        vi.spyOn(notification, 'error').mockImplementation(() => {})
    })

    afterEach(() => {
        infoSpy.mockRestore()
    })

    it('does not render until an event with a detail arrives', () => {
        render(<UpdateModuleModal />)
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })

    it('replaces the module file with the picked one and closes on success', async () => {
        mockReplaceFile.mockResolvedValueOnce()
        const user = userEvent.setup()

        render(<UpdateModuleModal />)
        const onSuccess = await openModal()

        expect(okButton()).toBeDisabled()

        const file = new File(['xlsx-bytes'], 'Main.xlsx')
        await user.upload(fileInput(), file)

        await waitFor(() => expect(okButton()).toBeEnabled())
        expect(screen.queryByTestId('update-file-name-warning')).not.toBeInTheDocument()

        await user.click(okButton())

        await waitFor(() => expect(mockReplaceFile).toHaveBeenCalledWith('proj-id', 'rules/Main.xlsx', file))
        expect(onSuccess).toHaveBeenCalledTimes(1)
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    })

    it('warns when the selected file name differs from the module file', async () => {
        const user = userEvent.setup()

        render(<UpdateModuleModal />)
        await openModal()

        await user.upload(fileInput(), new File(['xlsx-bytes'], 'Other.xlsx'))

        await screen.findByTestId('update-file-name-warning')
        expect(okButton()).toBeEnabled()
    })

    it('rejects a non-Excel file and keeps the update disabled', async () => {
        const user = userEvent.setup({ applyAccept: false })

        render(<UpdateModuleModal />)
        await openModal()

        await user.upload(fileInput(), new File(['x'], 'notes.txt'))

        await waitFor(() => expect(infoSpy).toHaveBeenCalledWith({ title: 'project:update_module_modal.only_excel' }))
        expect(okButton()).toBeDisabled()
        expect(mockReplaceFile).not.toHaveBeenCalled()
    })

    it('stays open and does not report success when the update fails', async () => {
        mockReplaceFile.mockRejectedValueOnce(new Error('locked'))
        const user = userEvent.setup()

        render(<UpdateModuleModal />)
        const onSuccess = await openModal()

        await user.upload(fileInput(), new File(['xlsx-bytes'], 'Main.xlsx'))
        await waitFor(() => expect(okButton()).toBeEnabled())
        await user.click(okButton())

        await waitFor(() => expect(mockReplaceFile).toHaveBeenCalledTimes(1))
        expect(onSuccess).not.toHaveBeenCalled()
        expect(screen.getByRole('dialog')).toBeInTheDocument()
    })
})
