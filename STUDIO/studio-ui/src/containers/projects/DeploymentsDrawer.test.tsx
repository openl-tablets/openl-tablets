import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { DeploymentsDrawer } from './DeploymentsDrawer'
import { getDeployment, getDeployments, getProductionRepositories } from '../../services/deployments'

vi.mock('../../services/deployments', () => ({
    getProductionRepositories: vi.fn(),
    getDeployments: vi.fn(),
    getDeployment: vi.fn(),
}))

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

vi.mock('antd', () => {
    const Drawer = ({ open, children, title }: Record<string, unknown>) =>
        open ? <div>{title as never}{children as never}</div> : null
    const Skeleton = () => <div>loading</div>
    const Empty = ({ description, ...rest }: Record<string, unknown>) => {
        const { image, ...dom } = rest
        void image
        return <div {...dom}>{description as never}</div>
    }
    Empty.PRESENTED_IMAGE_SIMPLE = 'simple'
    interface Item { key: string, label?: unknown, children?: unknown }
    const Collapse = ({ items, onChange }: { items?: Item[], onChange?: (k: string[]) => void }) => (
        <div>
            {items?.map(item => (
                <div key={item.key}>
                    <button onClick={() => onChange?.([item.key])}>{item.label as never}</button>
                    <div>{item.children as never}</div>
                </div>
            ))}
        </div>
    )
    interface ListProps { dataSource?: unknown[], renderItem?: (i: unknown) => unknown }
    const ListItem = Object.assign(
        ({ children, ...rest }: Record<string, unknown>) => <span {...rest}>{children as never}</span>,
        { Meta: ({ title, description }: Record<string, unknown>) => <span>{title as never}{description as never}</span> }
    )
    const List = Object.assign(
        ({ dataSource, renderItem }: ListProps) => <ul>{dataSource?.map((d, i) => <li key={i}>{renderItem?.(d) as never}</li>)}</ul>,
        { Item: ListItem }
    )
    return { Drawer, Skeleton, Empty, Collapse, List }
})

async function renderDrawer() {
    await act(async () => {
        render(<DeploymentsDrawer open onClose={vi.fn()} />)
        await new Promise(resolve => setTimeout(resolve, 0))
    })
}

describe('DeploymentsDrawer', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(getProductionRepositories).mockResolvedValue([{ id: 'prod', name: 'Production', aclId: 'x' }] as never)
        vi.mocked(getDeployments).mockResolvedValue([{ id: 'd1', name: 'Deployment One' }])
    })

    it('lists production repositories and lazily loads deployments on expand', async () => {
        await renderDrawer()

        await waitFor(() => expect(screen.getByTestId('deployment-repo-prod')).toBeTruthy())
        expect(getDeployments).not.toHaveBeenCalled()

        await userEvent.click(screen.getByTestId('deployment-repo-prod'))

        await waitFor(() => expect(getDeployments).toHaveBeenCalledWith('prod'))
        await waitFor(() => expect(screen.getByTestId('deployment-d1')).toBeTruthy())
        expect(screen.getByText('Deployment One')).toBeTruthy()
    })

    it('drills into a deployment to show its deployed projects', async () => {
        vi.mocked(getDeployment).mockResolvedValue({
            id: 'd1',
            name: 'Deployment One',
            repository: 'prod',
            items: [{ name: 'ProjA', revision: 'abc123', modifiedBy: 'alice' }],
        })
        await renderDrawer()

        await userEvent.click(screen.getByTestId('deployment-repo-prod'))
        await waitFor(() => expect(screen.getByTestId('deployment-d1')).toBeTruthy())
        expect(getDeployment).not.toHaveBeenCalled()

        await userEvent.click(screen.getByTestId('deployment-d1'))

        await waitFor(() => expect(getDeployment).toHaveBeenCalledWith('d1'))
        await waitFor(() => expect(screen.getByTestId('deployment-project-ProjA')).toBeTruthy())
    })
})
