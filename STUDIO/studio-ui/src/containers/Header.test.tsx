import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { Header } from './Header'
import { hasDeploymentRepositories } from '../services/deployments'

const { appNavigateMock, pathnameRef } = vi.hoisted(() => ({
    appNavigateMock: vi.fn(),
    pathnameRef: { current: '/projects' },
}))

vi.mock('../services/deployments', () => ({ hasDeploymentRepositories: vi.fn() }))

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

vi.mock('react-router-dom', () => ({
    Link: ({ children }: { children?: unknown }) => <a href="/">{children as never}</a>,
    useLocation: () => ({ pathname: pathnameRef.current }),
}))

vi.mock('./Header.styles', () => ({
    useStyles: () => ({ styles: new Proxy({}, { get: () => '' }) }),
}))

vi.mock('./header/UserMenu', () => ({ UserMenu: () => null }))
vi.mock('../components/Logo', () => ({ default: () => null }))
vi.mock('../hooks', () => ({ useAppNavigate: () => appNavigateMock, useScript: () => {} }))
vi.mock('store', () => ({ useNotificationStore: () => ({ notification: '' }) }))
vi.mock('../services', () => ({ CONFIG: { CONTEXT: '/web' } }))

vi.mock('antd', () => {
    interface Item { key: string, label: unknown }
    const Menu = ({ items, onClick, selectedKeys }: {
        items?: Item[]
        onClick?: (info: { key: string }) => void
        selectedKeys?: string[]
    }) => (
        <ul>
            {items?.map(item => (
                <li
                    key={item.key}
                    data-selected={selectedKeys?.includes(item.key) || undefined}
                    data-testid={`menu-${item.key}`}
                >
                    <button onClick={() => onClick?.({ key: item.key })} type="button">{item.label as never}</button>
                </li>
            ))}
        </ul>
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
        pathnameRef.current = '/projects'
    })

    it('offers the deployments tab to a user who may read a deployment repository', async () => {
        vi.mocked(hasDeploymentRepositories).mockResolvedValue(true)

        render(<Header />)

        expect(await screen.findByTestId('menu-/deployments')).toBeTruthy()
    })

    it('hides the deployments tab from a user who may read none', async () => {
        vi.mocked(hasDeploymentRepositories).mockResolvedValue(false)

        render(<Header />)

        await waitFor(() => expect(hasDeploymentRepositories).toHaveBeenCalled())
        expect(screen.queryByTestId('menu-/deployments')).toBeNull()
        // The tab it cannot offer never costs the user the other two.
        expect(screen.getByTestId('menu-/')).toBeTruthy()
        expect(screen.getByTestId('menu-/projects')).toBeTruthy()
    })

    it('moves the highlight with the location and drops it on screens without a tab', async () => {
        vi.mocked(hasDeploymentRepositories).mockResolvedValue(true)

        const { rerender } = render(<Header />)
        expect((await screen.findByTestId('menu-/projects')).getAttribute('data-selected')).toBe('true')
        // A project page still belongs to the Projects tab.
        pathnameRef.current = '/projects/abc'
        rerender(<Header />)
        expect(screen.getByTestId('menu-/projects').getAttribute('data-selected')).toBe('true')

        // Administration has no tab of its own, so no tab may stay lit.
        pathnameRef.current = '/administration/system'
        rerender(<Header />)
        expect(screen.getByTestId('menu-/projects').getAttribute('data-selected')).toBeNull()
        expect(screen.getByTestId('menu-/').getAttribute('data-selected')).toBeNull()

        // The legacy pages under faces/ are the Editor's own screens.
        pathnameRef.current = '/faces/main.xhtml'
        rerender(<Header />)
        expect(screen.getByTestId('menu-/').getAttribute('data-selected')).toBe('true')
    })

    it('switches tabs through the app instead of loading the page anew', async () => {
        vi.mocked(hasDeploymentRepositories).mockResolvedValue(true)

        render(<Header />)

        await userEvent.click(await screen.findByText('common:menu.deployments'))
        expect(appNavigateMock).toHaveBeenCalledWith('/deployments')
    })
})
