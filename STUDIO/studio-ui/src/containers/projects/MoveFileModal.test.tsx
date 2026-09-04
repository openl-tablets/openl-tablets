import { render, screen, waitFor } from '@testing-library/react'
import { fireEvent } from '@testing-library/dom'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MoveFileModal } from './MoveFileModal'
import { moveFile } from '../../services/files'

vi.mock('../../services/files', () => ({ moveFile: vi.fn() }))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

vi.mock('antd', () => {
    const Modal = ({ open, children, onOk, okButtonProps, title }: Record<string, unknown>) => open ? (
        <div role="dialog">
            <h2>{title as never}</h2>
            {children as never}
            <button {...(okButtonProps as Record<string, unknown>)} onClick={onOk as never}>ok</button>
        </div>
    ) : null
    const Input = ({ value, onChange, onPressEnter, ...rest }: Record<string, unknown>) => {
        void onPressEnter
        return <input data-testid={rest['data-testid'] as string} onChange={onChange as never} value={value as string} />
    }
    const notification = { error: vi.fn() }
    return { Input, Modal, notification }
})

vi.mock('./ProjectFolderInput', () => ({
    ProjectFolderInput: ({ value, onChange, ...rest }: Record<string, unknown>) => (
        <input
            data-testid={rest['data-testid'] as string}
            onChange={event => (onChange as (v: string) => void)(event.target.value)}
            value={value as string}
        />
    ),
}))

vi.mock('../../components/FieldRow', () => ({
    FieldRow: ({ children, label }: Record<string, unknown>) => <label>{label as never}{children as never}</label>,
}))

const props = {
    open: true,
    projectId: 'p1',
    path: 'rules/Main.xlsx',
    folders: ['rules', 'rules/nested'],
    onClose: vi.fn(),
    onMoved: vi.fn(),
}

describe('MoveFileModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(moveFile).mockResolvedValue()
    })

    it('renames the file in place, keeping its folder', async () => {
        render(<MoveFileModal {...props} mode="rename" />)

        expect(screen.getByRole('heading')).toHaveTextContent('browser.files.rename_title')
        expect((screen.getByTestId('file-move-name') as HTMLInputElement).value).toBe('Main.xlsx')
        expect(screen.queryByTestId('file-move-path')).toBeNull()

        fireEvent.change(screen.getByTestId('file-move-name'), { target: { value: 'Rates.xlsx' } })
        await userEvent.click(screen.getByText('ok'))

        await waitFor(() => expect(moveFile).toHaveBeenCalledWith('p1', 'rules/Main.xlsx', 'rules/Rates.xlsx'))
        expect(props.onMoved).toHaveBeenCalled()
    })

    it('moves the file to the folder the user picks, keeping its name', async () => {
        render(<MoveFileModal {...props} mode="move" />)

        expect(screen.getByRole('heading')).toHaveTextContent('browser.files.move_title')
        expect((screen.getByTestId('file-move-path') as HTMLInputElement).value).toBe('rules')
        expect(screen.queryByTestId('file-move-name')).toBeNull()

        fireEvent.change(screen.getByTestId('file-move-path'), { target: { value: 'rules/nested' } })
        await userEvent.click(screen.getByText('ok'))

        await waitFor(() => expect(moveFile).toHaveBeenCalledWith('p1', 'rules/Main.xlsx', 'rules/nested/Main.xlsx'))
    })

    it('moves nothing when the file would land where it already is', async () => {
        render(<MoveFileModal {...props} mode="move" />)

        await userEvent.click(screen.getByText('ok'))

        expect(moveFile).not.toHaveBeenCalled()
        expect(props.onClose).toHaveBeenCalled()
    })
})
