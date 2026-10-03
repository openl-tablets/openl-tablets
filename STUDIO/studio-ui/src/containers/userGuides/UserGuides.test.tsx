import React from 'react'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes, useLocation, useNavigationType } from 'react-router'
import { fetchGuidePage, fetchGuidesContents, type GuideEntry } from 'services/userGuides'
import UserGuides from './UserGuides'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

vi.mock('services/userGuides', async importOriginal => ({
    ...(await importOriginal<typeof import('services/userGuides')>()),
    fetchGuidesContents: vi.fn(),
    fetchGuidePage: vi.fn(),
}))

const CONTENTS: GuideEntry = {
    title: 'User Guides',
    file: 'index.md',
    children: [
        {
            title: 'OpenL Studio User Guide',
            file: 'openl-studio/index.md',
            children: [
                { title: 'Using Rules Editor', file: 'openl-studio/rules-editor.md' },
                { title: 'Appendices', children: [{ title: 'Error Pages', file: 'openl-studio/appendices/error-pages.md' }]},
            ],
        },
    ],
}

const PAGES: Record<string, string> = {
    'index.md': 'Welcome to the guides.',
    'openl-studio/index.md': '## The Studio Guide\n',
    'openl-studio/rules-editor.md': '## Using Rules Editor\n\nSee [the guide](index.md).\n\n### Viewing a Module\n\n### Copying a Table\n',
    'openl-studio/appendices/error-pages.md': '# Appendix D: Error Pages\n',
}

/** The address the viewer is at, and how it got there. */
const Address: React.FC = () => {
    const { pathname, hash } = useLocation()
    return <span data-action={useNavigationType()} data-testid="address">{`${pathname}${hash}`}</span>
}

const renderAt = (address: string) => render(
    <MemoryRouter initialEntries={[address]}>
        <Routes>
            <Route element={<><UserGuides /><Address /></>} path="/docs/*" />
        </Routes>
    </MemoryRouter>
)

const tree = () => within(document.querySelector('.ant-tree') as HTMLElement)

describe('UserGuides', () => {
    beforeEach(() => {
        Element.prototype.scrollIntoView = vi.fn()
        vi.mocked(fetchGuidesContents).mockResolvedValue(CONTENTS)
        vi.mocked(fetchGuidePage).mockImplementation(async file => PAGES[file] ?? '')
    })

    afterEach(() => {
        localStorage.clear()
    })

    it('opens the page the address names, with its guide open in the contents', async () => {
        renderAt('/docs/openl-studio/rules-editor')

        expect(await screen.findByRole('heading', { level: 2, name: 'Using Rules Editor' })).toBeInTheDocument()
        expect(fetchGuidePage).toHaveBeenCalledWith('openl-studio/rules-editor.md')
        expect(tree().getByTitle('Using Rules Editor').closest('.ant-tree-treenode')).toHaveClass('ant-tree-treenode-selected')
        expect(tree().getByTitle('Appendices')).toBeInTheDocument()
        expect(tree().queryByTitle('Error Pages')).not.toBeInTheDocument()
    })

    it('opens the root page at /docs, with or without the slash', async () => {
        renderAt('/docs')

        expect(await screen.findByRole('heading', { level: 1, name: 'User Guides' })).toBeInTheDocument()
        expect(fetchGuidePage).toHaveBeenCalledWith('index.md')
    })

    it('opens a page picked in the contents, and a folder without a page of its own only unfolds', async () => {
        renderAt('/docs/openl-studio/')
        await screen.findByRole('heading', { name: 'The Studio Guide' })

        await userEvent.click(tree().getByTitle('Appendices'))
        await userEvent.click(await tree().findByTitle('Error Pages'))

        expect(await screen.findByRole('heading', { level: 1, name: 'Appendix D: Error Pages' })).toBeInTheDocument()
        expect(screen.getByTestId('address')).toHaveTextContent('/docs/openl-studio/appendices/error-pages')
    })

    it('follows a link of a page to another page', async () => {
        renderAt('/docs/openl-studio/rules-editor')

        await userEvent.click(await screen.findByRole('link', { name: 'the guide' }))

        expect(await screen.findByRole('heading', { name: 'The Studio Guide' })).toBeInTheDocument()
        expect(screen.getByTestId('address')).toHaveTextContent('/docs/openl-studio/')
    })

    it('names the section picked in the outline in the address of the page, which the outline scrolls to alone', async () => {
        renderAt('/docs/openl-studio/rules-editor')
        const section = await screen.findByRole('link', { name: 'Copying a Table' })
        vi.mocked(Element.prototype.scrollIntoView).mockClear()

        await userEvent.click(section)

        expect(screen.getByTestId('address')).toHaveTextContent('/docs/openl-studio/rules-editor#copying-a-table')
        expect(screen.getByTestId('address')).toHaveAttribute('data-action', 'REPLACE')
        expect(Element.prototype.scrollIntoView).not.toHaveBeenCalled()
    })

    it('widens the contents to where its edge is dragged, and opens them that wide next time', async () => {
        const { unmount } = renderAt('/docs/')
        await screen.findByRole('heading', { level: 1, name: 'User Guides' })

        fireEvent.pointerDown(screen.getByTestId('guides-rail-resizer'))
        fireEvent(window, new MouseEvent('pointermove', { clientX: 420 }))
        fireEvent(window, new MouseEvent('pointerup', { clientX: 420 }))

        expect(screen.getByRole('navigation')).toHaveStyle({ width: '420px' })
        unmount()
        renderAt('/docs/')
        expect(await screen.findByRole('navigation')).toHaveStyle({ width: '420px' })
    })

    it('tells the guides hold no page at an unknown address', async () => {
        renderAt('/docs/openl-studio/missing')

        expect(await screen.findByText('guides:page_not_found')).toBeInTheDocument()
        expect(fetchGuidePage).not.toHaveBeenCalled()
    })

    it('tells the guides could not be read', async () => {
        vi.mocked(fetchGuidesContents).mockRejectedValue(new Error('500'))

        renderAt('/docs/')

        expect(await screen.findByText('guides:load_failed')).toBeInTheDocument()
        await waitFor(() => expect(document.querySelector('.ant-tree')).toBeNull())
    })
})
