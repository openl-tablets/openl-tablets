import { render, screen, waitFor } from '@testing-library/react'
import { fireEvent } from '@testing-library/dom'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { NewFileModal } from './NewFileModal'
import { createTextFile } from '../../services/files'

vi.mock('../../services/files', () => ({ createTextFile: vi.fn() }))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

vi.mock('antd', () => {
    const Modal = ({ open, children, onOk, okButtonProps }: Record<string, unknown>) => open ? (
        <div role="dialog">
            {children as never}
            <button {...(okButtonProps as Record<string, unknown>)} onClick={onOk as never}>ok</button>
        </div>
    ) : null
    const Input = ({ value, onChange, onPressEnter, ...rest }: Record<string, unknown>) => {
        void onPressEnter
        return (
            <input
                data-testid={rest['data-testid'] as string}
                onChange={onChange as never}
                value={value as string}
            />
        )
    }
    const notification = { error: vi.fn() }
    return { Input, Modal, notification }
})

// The path field has its own tests; here it is just an input carrying the path.
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
    folders: ['rules'],
    targetFolder: '',
    onClose: vi.fn(),
    onCreated: vi.fn(),
}

describe('NewFileModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(createTextFile).mockResolvedValue()
    })

    it('creates the file under the chosen folder', async () => {
        render(<NewFileModal {...props} />)

        fireEvent.change(screen.getByTestId('files-text-file-name'), { target: { value: 'notes.txt' } })
        fireEvent.change(screen.getByTestId('files-text-file-path'), { target: { value: 'rules' } })
        await userEvent.click(screen.getByText('ok'))

        await waitFor(() => expect(createTextFile).toHaveBeenCalledWith('p1', 'rules/notes.txt'))
        expect(props.onCreated).toHaveBeenCalled()
        expect(props.onClose).toHaveBeenCalled()
    })

    it('creates in the project root when no folder is given', async () => {
        render(<NewFileModal {...props} />)

        fireEvent.change(screen.getByTestId('files-text-file-name'), { target: { value: 'notes.txt' } })
        await userEvent.click(screen.getByText('ok'))

        await waitFor(() => expect(createTextFile).toHaveBeenCalledWith('p1', 'notes.txt'))
    })

    it('starts from the selected folder', () => {
        render(<NewFileModal {...props} targetFolder="rules" />)

        expect((screen.getByTestId('files-text-file-path') as HTMLInputElement).value).toBe('rules')
    })

    it('needs a name before it creates anything', async () => {
        render(<NewFileModal {...props} />)

        await userEvent.click(screen.getByText('ok'))

        expect(createTextFile).not.toHaveBeenCalled()
    })
})
