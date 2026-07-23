import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { DeploymentsHome } from './DeploymentsHome'
import { getDeployments, getProductionRepositories } from '../services/deployments'

const navigate = vi.fn()

vi.mock('react-router-dom', async () => {
    const { useState } = await import('react')
    return {
        useNavigate: () => navigate,
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
    // The shared search input carries its own icon.
    SearchOutlined: () => <span>search</span>,
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
    })

    it('loads production repositories and deployments for the first repository', async () => {
        await renderPage()

        expect(getProductionRepositories).toHaveBeenCalledTimes(1)
        await waitFor(() => expect(getDeployments).toHaveBeenCalledWith('prod'))
        expect(screen.getByTestId('deployment-repository-prod')).toBeInTheDocument()
        expect(screen.getByTestId('deployment-row-d1')).toBeInTheDocument()
        expect(screen.getByTestId('deployment-row-d2')).toBeInTheDocument()
    })

    it('opens a deployment on its own page', async () => {
        await renderPage()

        await userEvent.click(screen.getByTestId('deployment-open-d1'))

        expect(navigate).toHaveBeenCalledWith('/deployments/d1')
    })

    it('counts the deployments without mentioning the repositories', async () => {
        await renderPage()

        expect(screen.getByTestId('deployments-summary')).toHaveTextContent('deployments.summary:{"count":2}')
    })

    it('switches repositories and loads deployments for the selected repository', async () => {
        await renderPage()

        await userEvent.click(screen.getByTestId('deployment-repository-uat'))

        await waitFor(() => expect(getDeployments).toHaveBeenCalledWith('uat'))
        expect(screen.getByTestId('deployment-row-d3')).toBeInTheDocument()
    })

    it('reports failing repositories instead of an empty screen', async () => {
        vi.mocked(getProductionRepositories).mockRejectedValue(new Error('down'))
        await renderPage()

        expect(screen.getByTestId('deployments-repositories-error')).toBeInTheDocument()
    })

    it('reports failing deployments and loads them again on retry', async () => {
        vi.mocked(getDeployments).mockRejectedValueOnce(new Error('down'))
        await renderPage()

        expect(screen.getByTestId('deployments-error')).toBeInTheDocument()

        await userEvent.click(screen.getByText('deployments.retry'))

        await screen.findByText('Benefits')
    })

    it('tells apart an empty repository from a search without matches', async () => {
        vi.mocked(getDeployments).mockResolvedValue([])
        await renderPage()

        expect(screen.getByTestId('deployments-empty')).toBeInTheDocument()

        vi.mocked(getDeployments).mockImplementation(async () => deployments)
        await userEvent.click(screen.getByText('UAT'))
        await screen.findByText('Benefits')
        await userEvent.type(screen.getByTestId('deployments-search'), 'nothing-matches')

        expect(screen.getByTestId('deployments-no-match')).toBeInTheDocument()

        await userEvent.click(screen.getByText('deployments.clear_search'))
        expect(screen.queryByTestId('deployments-no-match')).toBeNull()
    })

    it('reloads the open repository when a project was deployed', async () => {
        await renderPage()
        const calls = vi.mocked(getDeployments).mock.calls.length

        await act(async () => {
            window.dispatchEvent(new Event('projectDeployed'))
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        expect(vi.mocked(getDeployments).mock.calls.length).toBeGreaterThan(calls)
    })
})
