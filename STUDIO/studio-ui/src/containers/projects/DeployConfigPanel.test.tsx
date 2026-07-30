import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { DeployConfigPanel } from './DeployConfigPanel'
import { getFileContent, rootFileExists, writeRootFile } from '../../services/files'
import { getProjectMigration, migrateProject } from '../../services/migration'

vi.mock('./CodeEditor', () => ({
    CodeEditor: ({ value, readOnly }: { value: string, readOnly?: boolean }) => (
        <textarea data-testid="deploy-xml" readOnly={readOnly} value={value} />
    ),
}))

vi.mock('../../services/files', () => ({
    getFileContent: vi.fn(),
    rootFileExists: vi.fn(),
    writeRootFile: vi.fn(),
}))

vi.mock('../../services/migration', () => ({
    EMPTY_MIGRATION: {
        rulesXml: { movableRootModules: [], migratable: false },
        rulesDeploy: { migratable: false },
    },
    getProjectMigration: vi.fn().mockResolvedValue({
        rulesXml: { movableRootModules: [], migratable: false },
        rulesDeploy: { migratable: false },
    }),
    migrateProject: vi.fn().mockResolvedValue(undefined),
}))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

vi.mock('../../components/FieldRow', () => ({
    FieldRow: ({ label, children }: Record<string, unknown>) => <div><label>{label as never}</label>{children as never}</div>,
}))

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
    const Tag = ({ children }: Record<string, unknown>) => <span>{children as never}</span>
    const Tooltip = ({ children }: Record<string, unknown>) => <>{children as never}</>
    const notification = { error: vi.fn(), success: vi.fn() }
    const App = { useApp: () => ({ notification }) }
    return { Alert, App, Button, Form, Input, Select, Skeleton, Space, Switch, Tag, Tooltip, notification }
})

vi.mock('antd-style', () => ({
    createStyles: () => () => ({
        styles: new Proxy({}, { get: (_t, name) => String(name) }),
        cx: (...args: unknown[]) => args.filter(Boolean).join(' '),
    }),
}))

vi.mock('@ant-design/icons', () => ({
    EditOutlined: () => <span>edit</span>,
    CheckOutlined: () => <span>check</span>,
    CloseOutlined: () => <span>close</span>,
    ThunderboltOutlined: () => <span>migrate</span>,
}))

async function renderPanel(canWrite = true, onSaved = vi.fn()) {
    await act(async () => {
        render(<DeployConfigPanel canWrite={canWrite} onSaved={onSaved} projectId="p1" />)
        await new Promise(resolve => setTimeout(resolve, 0))
    })
}

describe('DeployConfigPanel', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(writeRootFile).mockResolvedValue()
        vi.mocked(rootFileExists).mockResolvedValue(true)
        // clearAllMocks keeps mockResolvedValue implementations, so reset the migration flag each test.
        vi.mocked(getProjectMigration).mockResolvedValue({
            rulesXml: { movableRootModules: [], migratable: false },
            rulesDeploy: { migratable: false },
        })
    })

    it('reads the parsed descriptor as plain values, not as fields, by default', async () => {
        vi.mocked(getFileContent).mockResolvedValue('<rules-deploy><serviceName>svc</serviceName></rules-deploy>')
        await renderPanel()

        await waitFor(() => expect(screen.getByTestId('deploy-service-name')).toHaveTextContent('svc'))
        // No inputs before the user asks to edit; the runtime-context toggle reads disabled.
        expect(screen.getByTestId('deploy-service-name').tagName).toBe('SPAN')
        expect(screen.getByTestId('deploy-runtime-context')).toBeDisabled()
        // The Edit button is the only way in; Save appears only in the editing view.
        expect(screen.getByTestId('deploy-config-edit')).toBeInTheDocument()
        expect(screen.queryByTestId('deploy-config-save')).toBeNull()
    })

    it('turns the values into fields when the user clicks Edit, and back on cancel', async () => {
        vi.mocked(getFileContent).mockResolvedValue('<rules-deploy><serviceName>svc</serviceName></rules-deploy>')
        await renderPanel()
        await waitFor(() => expect(screen.getByTestId('deploy-service-name')).toHaveTextContent('svc'))

        await userEvent.click(screen.getByTestId('deploy-config-edit'))
        expect((screen.getByTestId('deploy-service-name') as HTMLInputElement).value).toBe('svc')

        await userEvent.clear(screen.getByTestId('deploy-service-name'))
        await userEvent.type(screen.getByTestId('deploy-service-name'), 'changed')
        await userEvent.click(screen.getByTestId('deploy-config-cancel'))

        // Cancel discards the edit and returns to the read view of the saved value.
        expect(screen.getByTestId('deploy-service-name')).toHaveTextContent('svc')
        expect(writeRootFile).not.toHaveBeenCalled()
    })

    it('shows the raw XML as a read-only file, even in the editing view', async () => {
        vi.mocked(getFileContent).mockResolvedValue(
            '<rules-deploy><configuration><foo/></configuration></rules-deploy>')
        await renderPanel()

        await userEvent.click(screen.getByTestId('deploy-config-edit'))
        expect(screen.getByTestId('deploy-xml')).toHaveAttribute('readOnly')
    })

    it('shows a hint and creates the file on save when missing', async () => {
        const onSaved = vi.fn()
        vi.mocked(rootFileExists).mockResolvedValue(false)
        await renderPanel(true, onSaved)

        expect(screen.getByTestId('deploy-config-missing')).toBeInTheDocument()
        await userEvent.click(screen.getByTestId('deploy-config-edit'))
        await userEvent.type(screen.getByTestId('deploy-service-name'), 'brand-new')
        await userEvent.click(screen.getByTestId('deploy-config-save'))

        // A missing descriptor is created — writeRootFile is told the file does not exist yet.
        await waitFor(() => expect(writeRootFile).toHaveBeenCalledTimes(1))
        const [projectId, name, xml, exists] = vi.mocked(writeRootFile).mock.calls[0]!
        expect(projectId).toBe('p1')
        expect(name).toBe('rules-deploy.xml')
        expect(xml).toContain('<serviceName>brand-new</serviceName>')
        expect(exists).toBe('create')
        expect(onSaved).toHaveBeenCalledTimes(1)
    })

    it('notifies the parent after updating an existing descriptor', async () => {
        const onSaved = vi.fn()
        vi.mocked(getFileContent).mockResolvedValue('<rules-deploy><serviceName>svc</serviceName></rules-deploy>')
        await renderPanel(true, onSaved)

        await waitFor(() => expect(screen.getByTestId('deploy-service-name')).toHaveTextContent('svc'))
        await userEvent.click(screen.getByTestId('deploy-config-edit'))
        await userEvent.clear(screen.getByTestId('deploy-service-name'))
        await userEvent.type(screen.getByTestId('deploy-service-name'), 'updated')
        await userEvent.click(screen.getByTestId('deploy-config-save'))

        await waitFor(() => expect(writeRootFile).toHaveBeenCalledWith(
            'p1',
            'rules-deploy.xml',
            expect.stringContaining('<serviceName>updated</serviceName>'),
            'overwrite'
        ))
        expect(onSaved).toHaveBeenCalledTimes(1)
    })

    it('hides the save button without write access', async () => {
        vi.mocked(getFileContent).mockResolvedValue('<rules-deploy/>')
        await renderPanel(false)

        expect(screen.queryByTestId('deploy-config-save')).toBeNull()
        expect(screen.queryByTestId('deploy-config-edit')).toBeNull()
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
        expect(writeRootFile).not.toHaveBeenCalled()
    })

    it('offers a migrate when rules-deploy.xml can be rewritten, and posts it with the scope', async () => {
        vi.mocked(getFileContent).mockResolvedValue('<rules-deploy><serviceName>svc</serviceName></rules-deploy>')
        vi.mocked(getProjectMigration).mockResolvedValue({
            rulesXml: { movableRootModules: [], migratable: false },
            rulesDeploy: { migratable: true },
        })
        await renderPanel()

        await userEvent.click(await screen.findByTestId('deploy-migrate'))

        await waitFor(() => expect(migrateProject).toHaveBeenCalledWith('p1', 'rulesDeploy'))
    })

    it('disables the edit button while a migrate is in flight', async () => {
        vi.mocked(getFileContent).mockResolvedValue('<rules-deploy><serviceName>svc</serviceName></rules-deploy>')
        vi.mocked(getProjectMigration).mockResolvedValue({
            rulesXml: { movableRootModules: [], migratable: false },
            rulesDeploy: { migratable: true },
        })
        let resolveMigrate!: () => void
        vi.mocked(migrateProject).mockReturnValueOnce(new Promise(resolve => { resolveMigrate = () => resolve(undefined) }))
        await renderPanel()

        await userEvent.click(await screen.findByTestId('deploy-migrate'))

        // While the migrate write is still in flight, editing the same rules-deploy.xml must be blocked.
        await waitFor(() => expect(screen.getByTestId('deploy-config-edit')).toBeDisabled())

        await act(async () => {
            resolveMigrate()
            await Promise.resolve()
        })
    })

    it('offers no migrate when rules-deploy.xml is already minimal', async () => {
        vi.mocked(getFileContent).mockResolvedValue('<rules-deploy><serviceName>svc</serviceName></rules-deploy>')
        await renderPanel()

        await screen.findByTestId('deploy-config')
        expect(screen.queryByTestId('deploy-migrate')).toBeNull()
    })
})
