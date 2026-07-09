import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { DeployConfigPanel } from './DeployConfigPanel'
import { getFileContent, rootFileExists, updateFileContent, uploadFiles } from '../../services/files'

vi.mock('../../services/files', () => ({
    getFileContent: vi.fn(),
    rootFileExists: vi.fn(),
    updateFileContent: vi.fn(),
    uploadFiles: vi.fn(),
}))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

vi.mock('antd', () => {
    const Form = ({ children, ...rest }: Record<string, unknown>) => {
        const { layout, ...dom } = rest
        void layout
        return <form {...dom}>{children as never}</form>
    }
    Form.Item = ({ children, label }: Record<string, unknown>) => <div><label>{label as never}</label>{children as never}</div>
    const Input = ({ onChange, ...rest }: Record<string, unknown>) => <input onChange={onChange as never} {...rest} />
    const Switch = ({ checked, onChange, ...rest }: Record<string, unknown>) => {
        const { children, ...dom } = rest
        void children
        return <input checked={checked as boolean} onChange={e => (onChange as (v: boolean) => void)(e.target.checked)} role="switch" type="checkbox" {...dom} />
    }
    const Select = ({ value, onChange, ...rest }: Record<string, unknown>) => {
        const { options, mode, placeholder, ...dom } = rest
        void options; void mode; void placeholder
        return (
            <input
                {...dom}
                data-value={(value as string[]).join(',')}
                onChange={e => (onChange as (v: string[]) => void)(e.target.value ? e.target.value.split(',') : [])}
            />
        )
    }
    const Button = ({ children, onClick, ...rest }: Record<string, unknown>) => {
        const { loading, type, ...dom } = rest
        void loading; void type
        return <button onClick={onClick as never} type="button" {...dom}>{children as never}</button>
    }
    const Alert = ({ title, ...rest }: Record<string, unknown>) => {
        const { showIcon, type, ...dom } = rest
        void showIcon; void type
        return <div {...dom}>{title as never}</div>
    }
    const Skeleton = () => <div>loading</div>
    const Space = ({ children }: Record<string, unknown>) => <div>{children as never}</div>
    const notification = { error: vi.fn(), success: vi.fn() }
    return { Alert, Button, Form, Input, Select, Skeleton, Space, Switch, notification }
})

async function renderPanel(canWrite = true, onSaved = vi.fn()) {
    await act(async () => {
        render(<DeployConfigPanel canWrite={canWrite} onSaved={onSaved} projectId="p1" />)
        await new Promise(resolve => setTimeout(resolve, 0))
    })
}

describe('DeployConfigPanel', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(updateFileContent).mockResolvedValue()
        vi.mocked(uploadFiles).mockResolvedValue()
        vi.mocked(rootFileExists).mockResolvedValue(true)
    })

    it('loads and shows the parsed descriptor', async () => {
        vi.mocked(getFileContent).mockResolvedValue('<rules-deploy><serviceName>svc</serviceName></rules-deploy>')
        await renderPanel()

        await waitFor(() => expect((screen.getByTestId('deploy-service-name') as HTMLInputElement).value).toBe('svc'))
    })

    it('shows a hint and creates the file on save when missing', async () => {
        const onSaved = vi.fn()
        vi.mocked(rootFileExists).mockResolvedValue(false)
        await renderPanel(true, onSaved)

        expect(screen.getByTestId('deploy-config-missing')).toBeInTheDocument()
        await userEvent.type(screen.getByTestId('deploy-service-name'), 'brand-new')
        await userEvent.click(screen.getByTestId('deploy-config-save'))

        await waitFor(() => expect(uploadFiles).toHaveBeenCalledTimes(1))
        const [projectId, path, files] = vi.mocked(uploadFiles).mock.calls[0]!
        expect(projectId).toBe('p1')
        expect(path).toBe('')
        expect(files).toHaveLength(1)
        expect(files[0]!.name).toBe('rules-deploy.xml')
        const xml = await files[0]!.text()
        expect(xml).toContain('<serviceName>brand-new</serviceName>')
        expect(updateFileContent).not.toHaveBeenCalled()
        expect(onSaved).toHaveBeenCalledTimes(1)
    })

    it('notifies the parent after updating an existing descriptor', async () => {
        const onSaved = vi.fn()
        vi.mocked(getFileContent).mockResolvedValue('<rules-deploy><serviceName>svc</serviceName></rules-deploy>')
        await renderPanel(true, onSaved)

        await waitFor(() => expect((screen.getByTestId('deploy-service-name') as HTMLInputElement).value).toBe('svc'))
        await userEvent.clear(screen.getByTestId('deploy-service-name'))
        await userEvent.type(screen.getByTestId('deploy-service-name'), 'updated')
        await userEvent.click(screen.getByTestId('deploy-config-save'))

        await waitFor(() => expect(updateFileContent).toHaveBeenCalledWith(
            'p1',
            'rules-deploy.xml',
            expect.stringContaining('<serviceName>updated</serviceName>')
        ))
        expect(onSaved).toHaveBeenCalledTimes(1)
    })

    it('hides the save button without write access', async () => {
        vi.mocked(getFileContent).mockResolvedValue('<rules-deploy/>')
        await renderPanel(false)

        expect(screen.queryByTestId('deploy-config-save')).toBeNull()
    })

    it('surfaces a load error', async () => {
        vi.mocked(rootFileExists).mockRejectedValue(new Error('Failed to list files'))
        await renderPanel()

        expect(screen.getByTestId('deploy-config-error')).toBeInTheDocument()
    })

    it('does not render or save a malformed descriptor', async () => {
        vi.mocked(getFileContent).mockResolvedValue('<rules-deploy><serviceName>svc</rules-deploy>')
        await renderPanel()

        expect(screen.getByTestId('deploy-config-invalid')).toBeInTheDocument()
        expect(screen.queryByTestId('deploy-config')).toBeNull()
        expect(updateFileContent).not.toHaveBeenCalled()
    })
})
