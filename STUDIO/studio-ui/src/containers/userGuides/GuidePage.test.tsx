import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { fetchGuidePage } from 'services/userGuides'
import { SystemContext } from '../../contexts'
import { documentTitle, GuidePage, outlineOf, printedHeads, studioName } from './GuidePage'

vi.mock('react-i18next', () => {
    const t = (key: string) => (key === 'common:openl_studio' ? 'OpenL Studio' : key)
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

describe('printedHeads', () => {
    it('heads every printed sheet with the title of the page and the version of OpenL Studio', () => {
        const heads = printedHeads('Using "Rules" Editor', studioName('OpenL Studio', '7.1.0'))

        expect(heads).toContain('@top-left { content: "Using \\"Rules\\" Editor";')
        expect(heads).toContain('@top-right { content: "OpenL Studio 7.1.0";')
    })

    it('names OpenL Studio alone before its version is read', () => {
        const studio = studioName('OpenL Studio', undefined)

        expect(printedHeads('Preface', studio)).toContain('@top-right { content: "OpenL Studio";')
        expect(documentTitle('Preface', studio)).toBe('Preface – OpenL Studio')
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
        // The outline is hidden below 1100px, and jsdom draws every page 1024px wide.
        expect(screen.getByRole('link', { hidden: true, name: 'Copying a Table' }))
            .toHaveAttribute('href', '/docs/openl-studio/rules-editor#copying-a-table')
    })

    it('gives the printed sheets of the page its title and the version of OpenL Studio', async () => {
        vi.mocked(fetchGuidePage).mockResolvedValue('## Using Rules Editor\n')

        const { container } = render(
            <SystemContext.Provider value={{ appVersion: '7.1.0' } as never}>
                <MemoryRouter>
                    <GuidePage container={{ current: null }} file="openl-studio/rules-editor.md" hash="" title="Using Rules Editor" />
                </MemoryRouter>
            </SystemContext.Provider>
        )

        await screen.findByRole('heading', { level: 2, name: 'Using Rules Editor' })
        expect(container.querySelector('style')?.textContent)
            .toBe(printedHeads('Using Rules Editor', 'OpenL Studio 7.1.0'))
    })

    it('names the browser tab after the page and the version while the page is open, which Firefox and Safari print', () => {
        vi.mocked(fetchGuidePage).mockResolvedValue('## Using Rules Editor\n')
        document.title = 'OpenL Studio'

        const { unmount } = render(
            <SystemContext.Provider value={{ appVersion: '7.1.0' } as never}>
                <MemoryRouter>
                    <GuidePage container={{ current: null }} file="openl-studio/rules-editor.md" hash="" title="Using Rules Editor" />
                </MemoryRouter>
            </SystemContext.Provider>
        )

        expect(document.title).toBe('Using Rules Editor – OpenL Studio 7.1.0')
        unmount()
        expect(document.title).toBe('OpenL Studio')
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
