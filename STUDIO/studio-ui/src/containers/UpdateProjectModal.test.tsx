import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { notification } from 'antd'
import { toUploadEntries, UpdateProjectModal, type UpdateProjectModalDetail } from './UpdateProjectModal'
import { updateProjectFromFiles, updateProjectFromZip } from 'services/projects'
import type { MockedFunction } from 'vitest'

vi.mock('services/projects', () => ({
    updateProjectFromFiles: vi.fn(),
    updateProjectFromZip: vi.fn(),
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

const mockZipUpload = updateProjectFromZip as MockedFunction<typeof updateProjectFromZip>
const mockFilesUpload = updateProjectFromFiles as MockedFunction<typeof updateProjectFromFiles>

const folderFile = (content: string, name: string, relativePath: string): File => {
    const file = new File([content], name)
    Object.defineProperty(file, 'webkitRelativePath', { value: relativePath })
    return file
}

const openModal = async (onSuccess = vi.fn()) => {
    await act(async () => {
        window.dispatchEvent(new CustomEvent<UpdateProjectModalDetail>('openUpdateProjectModal', {
            detail: { projectId: 'proj-id', projectName: 'MyProject', onSuccess },
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

const okButton = () => screen.getByRole('button', { name: 'project:update_project_modal.confirm_button' })

describe('toUploadEntries', () => {
    it('strips the single common root folder reported by the folder picker', () => {
        const files = [
            folderFile('a', 'Main.xlsx', 'MyProject/rules/Main.xlsx'),
            folderFile('b', 'deployment.xml', 'MyProject/deployment.xml'),
        ]

        expect(toUploadEntries(files).map(entry => entry.path)).toEqual(['rules/Main.xlsx', 'deployment.xml'])
    })

    it('keeps the paths when the files come from several root folders', () => {
        const files = [
            folderFile('a', 'x.txt', 'alpha/x.txt'),
            folderFile('b', 'y.txt', 'beta/y.txt'),
        ]

        expect(toUploadEntries(files).map(entry => entry.path)).toEqual(['alpha/x.txt', 'beta/y.txt'])
    })

    it('keeps folder paths when loose files are picked alongside a folder', () => {
        const files = [
            folderFile('a', 'x.txt', 'alpha/x.txt'),
            new File(['b'], 'plain.txt'),
        ]

        expect(toUploadEntries(files).map(entry => entry.path)).toEqual(['alpha/x.txt', 'plain.txt'])
    })

    it('normalizes backslash separators before mapping', () => {
        const files = [folderFile('a', 'Main.xlsx', 'MyProject\\rules\\Main.xlsx')]

        expect(toUploadEntries(files).map(entry => entry.path)).toEqual(['rules/Main.xlsx'])
    })
})

describe('UpdateProjectModal', () => {
    let infoSpy: ReturnType<typeof vi.spyOn>

    beforeEach(() => {
        vi.clearAllMocks()
        infoSpy = vi.spyOn(notification, 'info').mockImplementation(() => {})
    })

    afterEach(() => {
        infoSpy.mockRestore()
    })

    it('does not render until an event with a detail arrives', () => {
        render(<UpdateProjectModal />)
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })

    it('uploads the picked zip archive and closes on success', async () => {
        mockZipUpload.mockResolvedValueOnce(true)
        const user = userEvent.setup()

        render(<UpdateProjectModal />)
        const onSuccess = await openModal()

        expect(okButton()).toBeDisabled()

        const archive = new File(['zip-bytes'], 'project.zip', { type: 'application/zip' })
        await user.upload(fileInput(), archive)

        await waitFor(() => expect(okButton()).toBeEnabled())
        await user.click(okButton())

        await waitFor(() => expect(mockZipUpload).toHaveBeenCalledWith('proj-id', 'MyProject', archive))
        expect(onSuccess).toHaveBeenCalledTimes(1)
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    })

    it('rejects a non-zip file and keeps the update disabled', async () => {
        const user = userEvent.setup({ applyAccept: false })

        render(<UpdateProjectModal />)
        await openModal()

        await user.upload(fileInput(), new File(['x'], 'notes.txt'))

        await waitFor(() => expect(infoSpy).toHaveBeenCalledWith({ title: 'project:update_project_modal.only_zip' }))
        expect(okButton()).toBeDisabled()
        expect(mockZipUpload).not.toHaveBeenCalled()
    })

    it('uploads the picked folder as project-relative entries', async () => {
        mockFilesUpload.mockResolvedValueOnce(true)
        const user = userEvent.setup()

        render(<UpdateProjectModal />)
        const onSuccess = await openModal()

        await user.click(screen.getByText('project:update_project_modal.source_folder'))

        const main = folderFile('a', 'Main.xlsx', 'MyProject/rules/Main.xlsx')
        const deployment = folderFile('b', 'deployment.xml', 'MyProject/deployment.xml')
        await user.upload(fileInput(), [main, deployment])

        await waitFor(() => expect(screen.getByTestId('update-project-folder-summary')).toBeInTheDocument())
        await user.click(okButton())

        await waitFor(() => expect(mockFilesUpload).toHaveBeenCalledWith('proj-id', 'MyProject', [
            { path: 'rules/Main.xlsx', file: main },
            { path: 'deployment.xml', file: deployment },
        ]))
        expect(onSuccess).toHaveBeenCalledTimes(1)
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    })

    it('stays open and does not report success when the upload fails', async () => {
        mockZipUpload.mockResolvedValueOnce(false)
        const user = userEvent.setup()

        render(<UpdateProjectModal />)
        const onSuccess = await openModal()

        await user.upload(fileInput(), new File(['zip-bytes'], 'project.zip', { type: 'application/zip' }))
        await waitFor(() => expect(okButton()).toBeEnabled())
        await user.click(okButton())

        await waitFor(() => expect(mockZipUpload).toHaveBeenCalledTimes(1))
        expect(onSuccess).not.toHaveBeenCalled()
        expect(screen.getByRole('dialog')).toBeInTheDocument()
    })
})
