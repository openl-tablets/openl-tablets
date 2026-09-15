import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Modal } from 'antd'
import { describe, expect, it, vi } from 'vitest'
import type { Project } from '../../types/projects'
import { useOverwriteConfirm } from './useOverwriteConfirm'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

vi.mock('antd', async () => {
    const actual = await vi.importActual<typeof import('antd')>('antd')
    return { ...actual, Modal: { ...actual.Modal, confirm: vi.fn() } }
})

const Writer = ({ project, onWrite }: { project: Project | null, onWrite: () => void }) => {
    const confirmWrite = useOverwriteConfirm(project)
    return <button onClick={() => confirmWrite(onWrite)} type="button">write</button>
}

const write = async (project: Project | null) => {
    const onWrite = vi.fn()
    render(<Writer onWrite={onWrite} project={project} />)

    await userEvent.click(screen.getByText('write'))

    return onWrite
}

describe('useOverwriteConfirm', () => {

    it('writes without a word where the project is open on its latest revision', async () => {
        const onWrite = await write({ id: 'p1' } as Project)

        expect(onWrite).toHaveBeenCalled()
        expect(Modal.confirm).not.toHaveBeenCalled()
    })

    it('asks before a write that would save over a revision the reader has not seen', async () => {
        const onWrite = await write({ id: 'p1', overwritesNewerRevision: true } as Project)

        expect(onWrite).not.toHaveBeenCalled()
        // The write is what the question is about, so it happens only once the reader has answered it.
        const asked = vi.mocked(Modal.confirm).mock.calls.at(-1)?.[0]
        expect(asked?.title).toBe('browser.module.overwrite_revision')
        await asked?.onOk?.()
        expect(onWrite).toHaveBeenCalled()
    })
})
