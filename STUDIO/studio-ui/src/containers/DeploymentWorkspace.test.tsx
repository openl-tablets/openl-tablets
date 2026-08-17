import { act, render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { DeploymentWorkspace } from './DeploymentWorkspace'
import { getDeployment } from '../services/deployments'

const navigate = vi.fn()

vi.mock('react-router-dom', () => ({
    Link: ({ children, to }: Record<string, unknown>) => <a href={to as string}>{children as never}</a>,
    useNavigate: () => navigate,
    useParams: () => ({ deploymentId: 'd1' }),
}))

vi.mock('../services/deployments', () => ({ getDeployment: vi.fn() }))

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

vi.mock('antd-style', () => ({
    createStyles: () => () => ({
        styles: new Proxy({}, { get: () => '' }),
        cx: (...args: unknown[]) => args.filter(Boolean).join(' '),
    }),
}))

vi.mock('@ant-design/icons', () => ({ RocketOutlined: () => <span>rocket</span> }))

vi.mock('antd', () => {
    interface Item { key: string, label: unknown, children: unknown }
    const Tabs = ({ items, ...rest }: Record<string, unknown>) => (
        <div {...rest}>
            {(items as Item[]).map(item => (
                <section key={item.key} data-testid={`tab-${item.key}`}>
                    <h2>{item.label as never}</h2>
                    {item.children as never}
                </section>
            ))}
        </div>
    )
    const Button = ({ children, onClick, ...rest }: Record<string, unknown>) => {
        const { type, ...dom } = rest
        void type
        return <button onClick={onClick as never} {...dom}>{children as never}</button>
    }
    const Empty = ({ description, children, ...rest }: Record<string, unknown>) =>
        <div {...rest}>{description as never}{children as never}</div>
    const Skeleton = () => <div>skeleton</div>
    const Tooltip = ({ children, title }: Record<string, unknown>) =>
        <span title={title as string}>{children as never}</span>
    const Typography = {
        Text: ({ children, ellipsis, className, ...rest }: Record<string, unknown>) => {
            void ellipsis; void className
            return <span {...rest}>{children as never}</span>
        },
        Title: ({ children, ellipsis, className, level, ...rest }: Record<string, unknown>) => {
            void ellipsis; void className; void level
            return <h1 {...rest}>{children as never}</h1>
        },
    }
    return { Button, Empty, Skeleton, Tabs, Tooltip, Typography }
})

const deployment = {
    id: 'd1',
    name: 'Benefits',
    repository: 'prod',
    items: [
        {
            name: 'Benefits Rules',
            modifiedAt: '2026-07-09T10:30:00Z',
            modifiedBy: 'jane',
            designRevision: {
                revision: 'abc123def456789',
                modifiedBy: 'john',
                modifiedAt: '2026-07-08T08:15:00Z',
            },
        },
    ],
}

const renderPage = async () => {
    render(<DeploymentWorkspace />)
    // The mount-time loads land asynchronously; flush them before the assertions read the screen.
    await act(async () => {
        await new Promise(resolve => setTimeout(resolve, 0))
    })
}

describe('DeploymentWorkspace', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(getDeployment).mockResolvedValue(deployment)
    })

    it('lists the deployed projects of the deployment in the URL', async () => {
        await renderPage()

        expect(getDeployment).toHaveBeenCalledWith('d1')
        expect(screen.getByTestId('deployment-title')).toHaveTextContent('Benefits')
        // One tab, and it is the projects the deployment carries.
        expect(screen.getByTestId('tab-projects')).toBeInTheDocument()
        expect(screen.getByTestId('deployment-project-row-Benefits Rules')).toBeInTheDocument()
        expect(screen.getByText('jane')).toBeInTheDocument()
    })

    it('shows the design revision the deployed project was built from, not the one of the deployment', async () => {
        await renderPage()

        // Who committed that revision and when, never the technical revision id the response still carries.
        expect(screen.getByText('john')).toBeInTheDocument()
        expect(screen.queryByText(/abc123/)).not.toBeInTheDocument()
    })

    it('explains the missing design revision when no design revision matches the deployed content', async () => {
        const { designRevision, ...withoutRevision } = deployment.items[0]!
        void designRevision
        vi.mocked(getDeployment).mockResolvedValue({ ...deployment, items: [withoutRevision]})
        await renderPage()

        expect(screen.getByTitle('deployments.revision_unknown')).toHaveTextContent('—')
    })

    it('says so when the deployment carries no project', async () => {
        vi.mocked(getDeployment).mockResolvedValue({ ...deployment, items: []})
        await renderPage()

        expect(screen.getByTestId('deployment-no-projects')).toBeInTheDocument()
    })

    it('offers the way back when the deployment cannot be read', async () => {
        vi.mocked(getDeployment).mockRejectedValue(new Error('gone'))
        await renderPage()

        expect(screen.getByTestId('deployment-missing')).toHaveTextContent('gone')
    })
})
