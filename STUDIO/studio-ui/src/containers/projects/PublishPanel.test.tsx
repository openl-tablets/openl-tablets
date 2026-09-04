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
    const Tooltip = ({ children, title }: Record<string, unknown>) =>
        <span title={title as string}>{children as never}</span>
    return { Alert, Button, Skeleton, Tag, Tooltip }
})

vi.mock('@ant-design/icons', () => ({
    RocketOutlined: () => null,
}))

vi.mock('./DeployConfigPanel', () => ({
    DeployConfigPanel: ({ onSaved }: { onSaved: () => void }) => (
        <button data-testid="deploy-config-panel" onClick={onSaved} type="button" />
    ),
}))

vi.mock('./ValueText', () => ({
    ValueText: ({ children }: Record<string, unknown>) => <span>{children as never}</span>,
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
                    designRevision: {
                        revision: 'abcdef123456',
                        modifiedBy: 'john',
                        modifiedAt: '2024-01-02T00:00:00Z',
                    },
                    modifiedAt: '2024-03-04T00:00:00Z',
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
        // Who authored the deployed revision, never the technical revision id the response still carries.
        const card = screen.getByTestId('publish-deployment-prod:d1')
        expect(card.textContent).toContain('john')
        expect(card.textContent).not.toContain('abcdef')
        // The revision it carries and the date it was deployed are two labelled facts, not one.
        expect(card.textContent).toContain('browser.publish.card_revision')
        expect(card.textContent).toContain('browser.publish.card_deployed')
        expect(card.textContent).toContain('Jan 2, 2024')
        expect(card.textContent).toContain('Mar 4, 2024')
    })

    it('explains the dash when no design revision matches, and still dates the deployment', async () => {
        vi.mocked(getProjectDeployments).mockImplementation(async (repoId) => repoId === 'prod'
            ? [{
                id: 'd1',
                name: 'Service One',
                repository: 'prod',
                items: [{ name: 'Alpha', modifiedAt: '2024-03-04T00:00:00Z' }],
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

        expect(screen.getByTitle('deployments.revision_unknown')).toHaveTextContent('\u2014')
        expect(screen.getByTestId('publish-deployment-prod:d1').textContent).toContain('Mar 4, 2024')
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
