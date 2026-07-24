import { render, screen } from '@testing-library/react'
import { fireEvent } from '@testing-library/dom'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { NewFolderModal } from './NewFolderModal'

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
    return { Modal }
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
    folders: ['rules', 'rules/nested'],
    targetFolder: '',
    onClose: vi.fn(),
    onCreate: vi.fn(),
}

describe('NewFolderModal', () => {
    beforeEach(() => vi.clearAllMocks())

    it('creates the folder at the path shown in the field', async () => {
        render(<NewFolderModal {...props} />)

        fireEvent.change(screen.getByTestId('files-folder-path'), { target: { value: 'rules/2026' } })
        await userEvent.click(screen.getByText('ok'))

        expect(props.onCreate).toHaveBeenCalledWith('rules/2026')
        expect(props.onClose).toHaveBeenCalled()
    })

    it('starts from the selected folder so the field always shows the full path', () => {
        render(<NewFolderModal {...props} targetFolder="rules" />)

        expect((screen.getByTestId('files-folder-path') as HTMLInputElement).value).toBe('rules/')
    })

    it('keeps a blank path from creating anything', async () => {
        render(<NewFolderModal {...props} />)

        await userEvent.click(screen.getByText('ok'))

        expect(props.onCreate).not.toHaveBeenCalled()
    })
})
