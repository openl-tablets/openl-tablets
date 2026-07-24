import { act, render, screen, waitFor } from '@testing-library/react'
import { PublishPanel } from './PublishPanel'
import { getProductionRepositories, getProjectDeployments } from '../../services/deployments'

vi.mock('../../services/deployments', () => ({
    getProductionRepositories: vi.fn(),
    getProjectDeployments: vi.fn(),
}))

vi.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key: string, params?: { revision?: string }) => params?.revision ?? key,
    }),
}))

vi.mock('antd', () => {
    const Alert = ({ showIcon, title, type, ...rest }: Record<string, unknown>) => {
        void showIcon
        void type
        return <div {...rest}>{title as never}</div>
    }
    const Button = ({ children, icon, onClick, type, ...rest }: Record<string, unknown>) => {
        void icon
        void type
        return <button onClick={onClick as never} {...rest}>{children as never}</button>
    }
    const Skeleton = () => <div data-testid="publish-loading" />
    const Tag = ({ children }: Record<string, unknown>) => <span>{children as never}</span>
    return { Alert, Button, Skeleton, Tag }
})

vi.mock('@ant-design/icons', () => ({
    RocketOutlined: () => null,
}))

vi.mock('./DeployConfigPanel', () => ({
    DeployConfigPanel: ({ onSaved }: { onSaved: () => void }) => (
        <button data-testid="deploy-config-panel" onClick={onSaved} type="button" />
    ),
}))

vi.mock('./MonoChip', () => ({
    MonoChip: ({ children }: Record<string, unknown>) => <span>{children as never}</span>,
}))

describe('PublishPanel', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(getProductionRepositories).mockResolvedValue([
            { id: 'prod', name: 'Production' },
            { id: 'stage', name: 'Staging' },
        ] as never)
    })

    it('loads project deployments without drilling into every deployment', async () => {
        vi.mocked(getProjectDeployments).mockImplementation(async (repoId) => repoId === 'prod'
            ? [{
                id: 'd1',
                name: 'Service One',
                repository: 'prod',
                items: [{
                    name: 'Alpha',
                    revision: 'abcdef123456',
                    modifiedAt: '2024-01-02T00:00:00Z',
                }],
            }]
            : [])

        render(
            <PublishPanel
                canWrite={false}
                onChanged={vi.fn()}
                projectId="p1"
                projectName="Alpha"
            />
        )

        await waitFor(() => expect(getProjectDeployments).toHaveBeenCalledTimes(2))

        expect(getProjectDeployments).toHaveBeenCalledWith('prod', 'Alpha')
        expect(getProjectDeployments).toHaveBeenCalledWith('stage', 'Alpha')
        expect(screen.getByTestId('publish-deployment-prod:d1').textContent).toContain('Service One')
        expect(screen.getByTestId('publish-deployment-prod:d1').textContent).toContain('Production')
        expect(screen.getByTestId('publish-deployment-prod:d1').textContent).toContain('abcdef')
    })

    it('reloads project deployments when the project reloads', async () => {
        vi.mocked(getProjectDeployments).mockResolvedValue([])
        const { rerender } = render(
            <PublishPanel
                canWrite={false}
                onChanged={vi.fn()}
                projectId="p1"
                projectName="Alpha"
                reloadToken={0}
            />
        )

        await waitFor(() => expect(getProjectDeployments).toHaveBeenCalledTimes(2))

        rerender(
            <PublishPanel
                canWrite={false}
                onChanged={vi.fn()}
                projectId="p1"
                projectName="Alpha"
                reloadToken={1}
            />
        )

        await waitFor(() => expect(getProjectDeployments).toHaveBeenCalledTimes(4))
    })

    it('reloads project deployments after the project is deployed', async () => {
        vi.mocked(getProjectDeployments).mockResolvedValue([])
        render(
            <PublishPanel
                canWrite={false}
                onChanged={vi.fn()}
                projectId="p1"
                projectName="Alpha"
            />
        )

        await waitFor(() => expect(getProjectDeployments).toHaveBeenCalledTimes(2))

        act(() => {
            window.dispatchEvent(new CustomEvent('projectDeployed', { detail: { projectId: 'other' } }))
        })
        expect(getProjectDeployments).toHaveBeenCalledTimes(2)

        act(() => {
            window.dispatchEvent(new CustomEvent('projectDeployed', { detail: { projectId: 'p1' } }))
        })

        await waitFor(() => expect(getProjectDeployments).toHaveBeenCalledTimes(4))
    })

    it('notifies the project workspace when the deploy descriptor changes', async () => {
        const onChanged = vi.fn()
        vi.mocked(getProjectDeployments).mockResolvedValue([])

        render(
            <PublishPanel
                canWrite
                onChanged={onChanged}
                projectId="p1"
                projectName="Alpha"
            />
        )

        await waitFor(() => expect(getProjectDeployments).toHaveBeenCalledTimes(2))

        screen.getByTestId('deploy-config-panel').click()

        expect(onChanged).toHaveBeenCalledTimes(1)
    })
})
