import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import type { GuideEntry } from 'services/userGuides'
import { GuidesSearch, scopesOf } from './GuidesSearch'
import type { GuideSearchResult } from './guideSearch'
import { startGuideSearch } from './guideSearchClient'
import { chooseInSearch, searchFor, searchScope } from '../../testing/search'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

vi.mock('./guideSearchClient', () => ({ startGuideSearch: vi.fn() }))

const editor: GuideEntry = { title: 'Using Rules Editor', file: 'openl-studio/rules-editor.md' }
const studio: GuideEntry = { title: 'OpenL Studio User Guide', file: 'openl-studio/index.md', children: [editor]}
const CONTENTS: GuideEntry = { title: 'User Guides', file: 'index.md', children: [studio]}

const RESULT: GuideSearchResult = {
    file: 'openl-studio/rules-editor.md',
    title: 'Using Rules Editor',
    heading: 'Viewing Tables',
    anchor: 'viewing-tables',
    snippet: [{ text: 'A decision ', match: false, at: 0 }, { text: 'table', match: true, at: 11 }],
}

describe('GuidesSearch', () => {
    const search = vi.fn()
    const dispose = vi.fn()

    beforeEach(() => {
        vi.mocked(startGuideSearch).mockReturnValue({ search, dispose })
        search.mockResolvedValue([RESULT])
    })

    const renderSearch = () => render(
        <MemoryRouter>
            <GuidesSearch contents={CONTENTS} current={editor}>
                <div>the table of contents</div>
            </GuidesSearch>
        </MemoryRouter>
    )

    it('lists the parts found in place of the table of contents, each opening its page at the part', async () => {
        renderSearch()

        searchFor('decision tab')

        const result = await screen.findByRole('link', { name: /Viewing Tables/ })
        expect(result).toHaveAttribute('href', '/docs/openl-studio/rules-editor#viewing-tables')
        expect(result).toHaveTextContent('Using Rules Editor')
        expect(screen.getByText('table', { selector: 'mark' })).toBeInTheDocument()
        expect(screen.getByText('the table of contents')).not.toBeVisible()
        expect(search).toHaveBeenLastCalledWith('decision tab', '')
        expect(startGuideSearch).toHaveBeenCalledTimes(1)
    })

    it('starts no search until something is typed, and shows the contents again once the query is cleared', async () => {
        renderSearch()
        expect(startGuideSearch).not.toHaveBeenCalled()

        searchFor('rule')
        await screen.findByRole('link', { name: /Viewing Tables/ })
        searchFor('')

        expect(screen.getByText('the table of contents')).toBeVisible()
        expect(screen.queryByRole('list', { name: 'guides:results' })).not.toBeInTheDocument()
    })

    it('narrows the search to a folder above the page shown', async () => {
        renderSearch()

        // The header draws the guides to choose from, the widest first, and names the one searched.
        expect(searchScope('guides')).toMatchObject({
            label: 'guides:all_guides',
            placeholder: 'guides:search_placeholder',
            action: {
                choice: '',
                choices: [
                    { value: '', label: 'guides:all_guides' },
                    { value: 'openl-studio/', label: 'OpenL Studio User Guide' },
                ],
                label: 'guides:scope',
            },
        })

        chooseInSearch('openl-studio/')
        searchFor('table')

        await waitFor(() => expect(search).toHaveBeenLastCalledWith('table', 'openl-studio/'))
        expect(searchScope('guides')).toMatchObject({
            label: 'OpenL Studio User Guide',
            action: { choice: 'openl-studio/' },
        })
    })

    it('keeps no search box of its own beside the table of contents', () => {
        renderSearch()

        expect(screen.queryByRole('textbox')).not.toBeInTheDocument()
        expect(screen.queryByRole('combobox')).not.toBeInTheDocument()
    })

    it('tells nothing matched, also when the search failed', async () => {
        search.mockRejectedValue(new Error('broken'))
        renderSearch()

        searchFor('zzz')

        expect(await screen.findByText('guides:no_matches')).toBeInTheDocument()
    })

    it('stops the search when the guides are closed', async () => {
        const { unmount } = renderSearch()
        searchFor('rule')
        await screen.findByRole('link', { name: /Viewing Tables/ })

        unmount()

        expect(dispose).toHaveBeenCalled()
    })
})

describe('scopesOf', () => {
    it('offers every folder above a page, the outermost first', () => {
        const appendix: GuideEntry = { title: 'Error Pages', file: 'openl-studio/appendices/error-pages.md' }
        const appendices: GuideEntry = { title: 'Appendices', children: [appendix]}

        expect(scopesOf([CONTENTS, studio, appendices, appendix], appendix.file)).toEqual([
            { value: 'openl-studio/', label: 'OpenL Studio User Guide' },
            { value: 'openl-studio/appendices/', label: 'Appendices' },
        ])
        expect(scopesOf([CONTENTS, studio], studio.file)).toEqual([{ value: 'openl-studio/', label: 'OpenL Studio User Guide' }])
        expect(scopesOf([], undefined)).toEqual([])
    })
})
