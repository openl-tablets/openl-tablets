import { render, screen, waitFor } from '@testing-library/react'
import { fireEvent } from '@testing-library/dom'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CopyProjectModal } from './CopyProjectModal'
import { copyProject } from '../../services/repositories'

vi.mock('../../services/repositories', () => ({ copyProject: vi.fn() }))
vi.mock('react-i18next', () => {
    // Stable t: a new t per render would re-run the modal's reset effect and clobber field edits.
    const t = (key: string, opts?: Record<string, unknown>) => (opts?.['name'] ? `${key}:${opts['name']}` : key)
    return { useTranslation: () => ({ t }) }
})

vi.mock('antd', () => {
    const Modal = ({ open, children, onOk, okText }: Record<string, unknown>) =>
        open
            ? <div>{children as never}<button data-testid="copy-ok" onClick={onOk as never}>{okText as never}</button></div>
            : null
    const Input = ({ onChange, ...rest }: Record<string, unknown>) => <input onChange={onChange as never} {...rest} />
    Input.TextArea = ({ onChange, autoSize, ...rest }: Record<string, unknown>) => {
        void autoSize
        return <textarea onChange={onChange as never} {...rest} />
    }
    const Alert = ({ message, showIcon, ...rest }: Record<string, unknown>) => {
        void showIcon
        return <div {...rest}>{message as never}</div>
    }
    const Form = ({ children }: Record<string, unknown>) => <div>{children as never}</div>
    Form.Item = ({ children }: Record<string, unknown>) => <div>{children as never}</div>
    interface Opt { value: string, label: string }
    const Select = ({ options, onChange, value, ...rest }: Record<string, unknown>) => (
        <select
            data-testid={rest['data-testid'] as string}
            onChange={event => (onChange as (v: string) => void)(event.target.value)}
            value={value as string}
        >
            {(options as Opt[]).map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
        </select>
    )
    const notification = { success: vi.fn(), error: vi.fn() }
    return { Alert, Form, Input, Modal, notification, Select }
})

const project = { id: 'p1', name: 'Alpha', repository: 'design' } as never
const repositories = [{ id: 'design', name: 'Design' }, { id: 'prod', name: 'Prod' }] as never
const mappedRepositories = [
    {
        id: 'design',
        name: 'Design',
        features: { branches: false, searchable: false, mappedFolders: true },
    },
] as never

describe('CopyProjectModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(copyProject).mockResolvedValue()
    })

    it('copies with the default name to the source repository', async () => {
        render(<CopyProjectModal open onClose={vi.fn()} onCopied={vi.fn()} project={project} repositories={repositories} />)
        await waitFor(() => expect((screen.getByTestId('copy-project-name') as HTMLInputElement).value).toBe('Alpha (Copy)'))

        await userEvent.click(screen.getByTestId('copy-ok'))

        await waitFor(() => expect(copyProject).toHaveBeenCalledWith(
            'design',
            'Alpha',
            'design',
            'Alpha (Copy)',
            undefined,
            undefined
        ))
    })

    it('copies to a chosen repository under an edited name', async () => {
        const onCopied = vi.fn()
        render(<CopyProjectModal open onClose={vi.fn()} onCopied={onCopied} project={project} repositories={repositories} />)
        await waitFor(() => expect(screen.getByTestId('copy-project-name')).toBeTruthy())

        fireEvent.change(screen.getByTestId('copy-project-name'), { target: { value: 'Beta' } })
        fireEvent.change(screen.getByTestId('copy-project-repository'), { target: { value: 'prod' } })
        await userEvent.click(screen.getByTestId('copy-ok'))

        await waitFor(() => expect(copyProject).toHaveBeenCalledWith(
            'design',
            'Alpha',
            'prod',
            'Beta',
            undefined,
            undefined
        ))
        await waitFor(() => expect(onCopied).toHaveBeenCalled())
    })

    it('passes an entered commit comment without generating a default one', async () => {
        render(<CopyProjectModal open onClose={vi.fn()} onCopied={vi.fn()} project={project} repositories={repositories} />)
        await waitFor(() => expect(screen.getByTestId('copy-project-comment')).toBeTruthy())

        fireEvent.change(screen.getByTestId('copy-project-comment'), { target: { value: 'manual copy message' } })
        await userEvent.click(screen.getByTestId('copy-ok'))

        await waitFor(() => expect(copyProject).toHaveBeenCalledWith(
            'design',
            'Alpha',
            'design',
            'Alpha (Copy)',
            'manual copy message',
            undefined
        ))
    })

    it('passes a target path only for repositories that support mapped folders', async () => {
        render(<CopyProjectModal open onClose={vi.fn()} onCopied={vi.fn()} project={project} repositories={mappedRepositories} />)
        await waitFor(() => expect(screen.getByTestId('copy-project-path')).toBeTruthy())

        fireEvent.change(screen.getByTestId('copy-project-path'), { target: { value: 'team/rules' } })
        await userEvent.click(screen.getByTestId('copy-ok'))

        await waitFor(() => expect(copyProject).toHaveBeenCalledWith(
            'design',
            'Alpha',
            'design',
            'Alpha (Copy)',
            undefined,
            'team/rules'
        ))
    })

    it('hides the target path for flat repositories', async () => {
        render(<CopyProjectModal open onClose={vi.fn()} onCopied={vi.fn()} project={project} repositories={repositories} />)
        await waitFor(() => expect(screen.getByTestId('copy-project-name')).toBeTruthy())

        expect(screen.queryByTestId('copy-project-path')).toBeNull()
    })

    it('defaults to the first creatable repository when the source is not creatable', async () => {
        render(
            <CopyProjectModal
                open
                onClose={vi.fn()}
                onCopied={vi.fn()}
                project={project}
                repositories={[{ id: 'prod', name: 'Prod' }] as never}
            />
        )
        await waitFor(() => expect((screen.getByTestId('copy-project-repository') as HTMLSelectElement).value).toBe('prod'))

        await userEvent.click(screen.getByTestId('copy-ok'))

        await waitFor(() => expect(copyProject).toHaveBeenCalledWith(
            'design',
            'Alpha',
            'prod',
            'Alpha (Copy)',
            undefined,
            undefined
        ))
    })

    it('rejects an empty name without calling the API', async () => {
        render(<CopyProjectModal open onClose={vi.fn()} onCopied={vi.fn()} project={project} repositories={repositories} />)
        await waitFor(() => expect(screen.getByTestId('copy-project-name')).toBeTruthy())

        fireEvent.change(screen.getByTestId('copy-project-name'), { target: { value: '  ' } })
        await userEvent.click(screen.getByTestId('copy-ok'))

        expect(copyProject).not.toHaveBeenCalled()
        await waitFor(() => expect(screen.getByTestId('copy-project-error')).toBeTruthy())
    })
})
