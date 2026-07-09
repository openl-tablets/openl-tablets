import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { DeploymentsHome } from './DeploymentsHome'
import { getDeployment, getDeployments, getProductionRepositories } from '../services/deployments'

vi.mock('react-router-dom', async () => {
    const { useState } = await import('react')
    return {
        useSearchParams: () => {
            const [params, setParams] = useState(new URLSearchParams())
            const set = (next: URLSearchParams | ((prev: URLSearchParams) => URLSearchParams)) => {
                setParams(prev => (typeof next === 'function' ? next(prev) : next))
            }
            return [params, set] as const
        },
    }
})

vi.mock('../services/deployments', () => ({
    getProductionRepositories: vi.fn(),
    getDeployments: vi.fn(),
    getDeployment: vi.fn(),
}))

vi.mock('react-i18next', () => ({
    useTranslation: () => ({ t: (key: string, values?: Record<string, unknown>) => values ? `${key}:${JSON.stringify(values)}` : key }),
}))

vi.mock('antd-style', () => ({
    createStyles: () => () => ({
        styles: new Proxy({}, { get: () => '' }),
        cx: (...args: unknown[]) => args.filter(Boolean).join(' '),
    }),
}))

vi.mock('./projects/RepoBadge', () => ({
    RepoBadge: ({ name }: { name: string }) => <span>{name}</span>,
}))

vi.mock('antd', () => {
    const Button = ({ children, icon, onClick, disabled, ...rest }: Record<string, unknown>) => (
        <button disabled={disabled as boolean} onClick={onClick as never} {...rest}>
            {icon as never}
            {children as never}
        </button>
    )
    const Input = ({ onChange, value, placeholder, allowClear, className, ...rest }: Record<string, unknown>) => {
        void allowClear
        void className
        return (
            <input
                onChange={onChange as never}
                placeholder={placeholder as never}
                value={(value as string) ?? ''}
                {...rest}
            />
        )
    }
    const Empty = ({ description, image, children, ...rest }: Record<string, unknown>) => {
        void image
        return <div {...rest}>{description as never}{children as never}</div>
    }
    Empty.PRESENTED_IMAGE_SIMPLE = 'simple'
    const Skeleton = () => <div>skeleton</div>
    const Alert = ({ action, title, description, showIcon, type, ...rest }: Record<string, unknown>) => {
        void showIcon
        void type
        return <div {...rest}>{title as never}{description as never}{action as never}</div>
    }
    const Tooltip = ({ children }: { children?: unknown }) => <>{children as never}</>
    const Typography = {
        Text: ({ children, ellipsis, className, ...rest }: Record<string, unknown>) => {
            void ellipsis
            void className
            return <span {...rest}>{children as never}</span>
        },
    }
    return { Alert, Button, Empty, Input, Skeleton, Tooltip, Typography }
})

vi.mock('@ant-design/icons', () => ({
    ReloadOutlined: () => <span>reload</span>,
    RightOutlined: () => <span>right</span>,
}))

const repositories = [
    { id: 'prod', name: 'Production', aclId: 'p', type: 'repo-git' },
    { id: 'uat', name: 'UAT', aclId: 'u', type: 'repo-file' },
]

const deployments = [
    { id: 'd1', name: 'Benefits' },
    { id: 'd2', name: 'Rating' },
]

async function renderPage() {
    await act(async () => {
        render(<DeploymentsHome />)
        await new Promise(resolve => setTimeout(resolve, 50))
    })
}

describe('DeploymentsHome', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(getProductionRepositories).mockResolvedValue(repositories)
        vi.mocked(getDeployments).mockImplementation(async repoId => repoId === 'prod' ? deployments : [
            { id: 'd3', name: 'Claims' },
        ])
        vi.mocked(getDeployment).mockResolvedValue({
            id: 'd1',
            name: 'Benefits',
            repository: 'prod',
            items: [
                {
                    name: 'Benefits Rules',
                    modifiedAt: '2026-07-09T10:30:00Z',
                    modifiedBy: 'jane',
                    revision: 'abc123',
                },
            ],
        })
    })

    it('loads production repositories and deployments for the first repository', async () => {
        await renderPage()

        expect(getProductionRepositories).toHaveBeenCalledTimes(1)
        await waitFor(() => expect(getDeployments).toHaveBeenCalledWith('prod'))
        expect(screen.getByTestId('deployment-repository-prod')).toBeInTheDocument()
        expect(screen.getByTestId('deployment-row-d1')).toBeInTheDocument()
        expect(screen.getByTestId('deployment-row-d2')).toBeInTheDocument()
    })

    it('loads deployed projects lazily when a deployment is expanded', async () => {
        await renderPage()

        await userEvent.click(screen.getAllByLabelText('deployments.expand')[0]!)

        await waitFor(() => expect(getDeployment).toHaveBeenCalledWith('d1'))
        expect(screen.getByTestId('deployment-project-row-Benefits Rules')).toBeInTheDocument()
        expect(screen.getByText('abc123')).toBeInTheDocument()
        expect(screen.getByText('jane')).toBeInTheDocument()
    })

    it('switches repositories and loads deployments for the selected repository', async () => {
        await renderPage()

        await userEvent.click(screen.getByTestId('deployment-repository-uat'))

        await waitFor(() => expect(getDeployments).toHaveBeenCalledWith('uat'))
        expect(screen.getByTestId('deployment-row-d3')).toBeInTheDocument()
    })
})
