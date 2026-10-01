import React from 'react'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import type { GuideEntry } from 'services/userGuides'
import { GuidesSearch, scopesOf } from './GuidesSearch'
import type { GuideSearchResult } from './guideSearch'
import { startGuideSearch } from './guideSearchClient'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

vi.mock('./guideSearchClient', () => ({ startGuideSearch: vi.fn() }))

// antd's Select cannot be driven in jsdom, so a native one stands in for it.
vi.mock('antd', async importOriginal => ({
    ...(await importOriginal<typeof import('antd')>()),
    Select: ({ options, value, onChange, 'aria-label': label }: {
        options: { value: string, label: string }[]
        value: string
        onChange: (value: string) => void
        'aria-label': string
    }) => (
        <select aria-label={label} onChange={event => onChange(event.target.value)} value={value}>
            {options.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
        </select>
    ),
}))

const editor: GuideEntry = { title: 'Using Rules Editor', file: 'openl-studio/rules-editor.md' }
const studio: GuideEntry = { title: 'OpenL Studio User Guide', file: 'openl-studio/index.md', children: [editor]}
const CONTENTS: GuideEntry = { title: 'User Guides', file: 'index.md', children: [studio]}

const RESULT: GuideSearchResult = {
    file: 'openl-studio/rules-editor.md',
    title: 'Using Rules Editor',
    heading: 'Viewing Tables',
    anchor: 'viewing-tables',
    snippet: [{ text: 'A decision ', match: false }, { text: 'table', match: true }],
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

        await userEvent.type(screen.getByRole('textbox', { name: 'guides:search' }), 'decision tab')

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

        const box = screen.getByRole('textbox', { name: 'guides:search' })
        await userEvent.type(box, 'rule')
        await screen.findByRole('link', { name: /Viewing Tables/ })
        await userEvent.clear(box)

        expect(screen.getByText('the table of contents')).toBeVisible()
        expect(screen.queryByRole('list', { name: 'guides:results' })).not.toBeInTheDocument()
    })

    it('narrows the search to a folder above the page shown', async () => {
        renderSearch()

        await userEvent.selectOptions(screen.getByRole('combobox', { name: 'guides:scope' }), 'OpenL Studio User Guide')
        await userEvent.type(screen.getByRole('textbox', { name: 'guides:search' }), 'table')

        await waitFor(() => expect(search).toHaveBeenLastCalledWith('table', 'openl-studio/'))
    })

    it('tells nothing matched, also when the search failed', async () => {
        search.mockRejectedValue(new Error('broken'))
        renderSearch()

        await userEvent.type(screen.getByRole('textbox', { name: 'guides:search' }), 'zzz')

        expect(await screen.findByText('guides:no_matches')).toBeInTheDocument()
    })

    it('stops the search when the guides are closed', async () => {
        const { unmount } = renderSearch()
        await userEvent.type(screen.getByRole('textbox', { name: 'guides:search' }), 'rule')
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
