import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { fetchGuidePage } from 'services/userGuides'
import { GuidePage, outlineOf } from './GuidePage'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

vi.mock('services/userGuides', async importOriginal => ({
    ...(await importOriginal<typeof import('services/userGuides')>()),
    fetchGuidePage: vi.fn(),
}))

const article = (html: string): HTMLElement => {
    const element = document.createElement('article')
    element.innerHTML = `<div>${html}</div>`
    return element
}

const titles = (items: ReturnType<typeof outlineOf>): unknown[] =>
    items.map(item => [item.title, ...(item.children ?? []).map(child => child.title)])

describe('outlineOf', () => {
    it('lists the two levels below the heading the page opens with', () => {
        const outline = outlineOf(article(
            '<h2 id="rules-editor">Rules Editor</h2><p>Text</p>'
            + '<h3 id="viewing">Viewing</h3><h4 id="filtering">Filtering</h4><h5 id="deep">Deep</h5>'
            + '<h3 id="editing">Editing</h3>'
        ), '/docs/studio/editor')

        expect(titles(outline)).toEqual([['Viewing', 'Filtering'], ['Editing']])
        expect(outline[0]).toMatchObject({ key: 'viewing', href: '/docs/studio/editor#viewing' })
    })

    it('leaves out the title of a page whose sections share its level', () => {
        const outline = outlineOf(article('<h2 id="configuration">Configuration</h2><h2 id="sources">Sources</h2><h3 id="env">Env</h3>'), '')

        expect(titles(outline)).toEqual([['Sources', 'Env']])
    })

    it('starts from the first section of a page the viewer gave a title to', () => {
        const outline = outlineOf(article('<h1>Videocasts</h1><p>Text</p><h2 id="one">One</h2><h2 id="two">Two</h2>'), '')

        expect(titles(outline)).toEqual([['One'], ['Two']])
    })

    it('lists nothing for a page without sections', () => {
        expect(outlineOf(article('<p>Text</p>'), '')).toEqual([])
        expect(outlineOf(null, '')).toEqual([])
    })
})

describe('GuidePage', () => {
    const scrollIntoView = vi.fn()

    beforeEach(() => {
        Element.prototype.scrollIntoView = scrollIntoView
    })

    const renderPage = (hash = '') => render(
        <MemoryRouter>
            <GuidePage container={{ current: null }} file="openl-studio/rules-editor.md" hash={hash} title="Using Rules Editor" />
        </MemoryRouter>
    )

    it('draws the page with the outline of its sections beside it', async () => {
        vi.mocked(fetchGuidePage).mockResolvedValue('## Using Rules Editor\n\n### Viewing a Module\n\n### Copying a Table\n')

        renderPage()

        expect(await screen.findByRole('heading', { level: 2, name: 'Using Rules Editor' })).toBeInTheDocument()
        expect(fetchGuidePage).toHaveBeenCalledWith('openl-studio/rules-editor.md')
        expect(screen.getByText('guides:on_this_page')).toBeInTheDocument()
        expect(screen.getByRole('link', { name: 'Copying a Table' }))
            .toHaveAttribute('href', '/docs/openl-studio/rules-editor#copying-a-table')
    })

    it('scrolls to the heading the address names', async () => {
        vi.mocked(fetchGuidePage).mockResolvedValue('## Title\n\n### Viewing a Module\n')

        renderPage('#viewing-a-module')

        await waitFor(() => expect(scrollIntoView).toHaveBeenCalled())
        expect(scrollIntoView.mock.contexts.at(-1)).toHaveAttribute('id', 'viewing-a-module')
    })

    it('tells the page could not be read', async () => {
        vi.mocked(fetchGuidePage).mockRejectedValue(new Error('404'))

        renderPage()

        expect(await screen.findByText('guides:page_failed')).toBeInTheDocument()
        expect(screen.queryByText('guides:on_this_page')).not.toBeInTheDocument()
    })
})
