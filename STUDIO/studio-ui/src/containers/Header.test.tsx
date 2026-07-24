import { render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { Header } from './Header'
import { hasDeploymentRepositories } from '../services/deployments'

vi.mock('../services/deployments', () => ({ hasDeploymentRepositories: vi.fn() }))

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

vi.mock('react-router-dom', () => ({
    Link: ({ children }: { children?: unknown }) => <a href="/">{children as never}</a>,
}))

vi.mock('./Header.styles', () => ({
    useStyles: () => ({ styles: new Proxy({}, { get: () => '' }) }),
}))

vi.mock('./header/UserMenu', () => ({ UserMenu: () => null }))
vi.mock('../components/Logo', () => ({ default: () => null }))
vi.mock('../hooks', () => ({ useScript: () => {} }))
vi.mock('store', () => ({ useNotificationStore: () => ({ notification: '' }) }))
vi.mock('../services', () => ({ CONFIG: { CONTEXT: '/web' } }))

vi.mock('antd', () => {
    interface Item { key: string, label: unknown }
    const Menu = ({ items }: { items?: Item[] }) => (
        <ul>{items?.map(item => <li key={item.key} data-testid={`menu-${item.key}`}>{item.label as never}</li>)}</ul>
    )
    const Layout = { Header: ({ children }: { children?: unknown }) => <header>{children as never}</header> }
    const passthrough = ({ children }: { children?: unknown }) => <div>{children as never}</div>
    return { Alert: passthrough, Avatar: passthrough, Col: passthrough, Layout, Menu, Row: passthrough }
})

vi.mock('@ant-design/icons', () => ({ UserOutlined: () => null }))

describe('Header', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.resetModules()
    })

    it('offers the deployments tab to a user who may read a deployment repository', async () => {
        vi.mocked(hasDeploymentRepositories).mockResolvedValue(true)

        render(<Header />)

        expect(await screen.findByTestId('menu-/web/deployments')).toBeTruthy()
    })

    it('hides the deployments tab from a user who may read none', async () => {
        vi.mocked(hasDeploymentRepositories).mockResolvedValue(false)

        render(<Header />)

        await waitFor(() => expect(hasDeploymentRepositories).toHaveBeenCalled())
        expect(screen.queryByTestId('menu-/web/deployments')).toBeNull()
        // The tab it cannot offer never costs the user the other two.
        expect(screen.getByTestId('menu-/web/')).toBeTruthy()
        expect(screen.getByTestId('menu-/web/projects')).toBeTruthy()
    })
})
