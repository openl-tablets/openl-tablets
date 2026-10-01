import { indexGuides, readPages, sectionsOf, snippetOf } from './guideSearch'

const page = (file: string, title: string, text: string) => ({ file, title, text })

describe('sectionsOf', () => {
    it('cuts a page at its headings, each part named by the id the viewer gives its heading', () => {
        const sections = sectionsOf(page('a.md', 'Page A', [
            '---',
            'title: Page A',
            '---',
            'Before any heading.',
            '',
            '## The `rules.xml` File',
            '',
            'Text with ![a screenshot](images/x.png) and <br/> HTML.',
            '',
            '## Example',
            '',
            '> ## Example',
            '',
            '## Example',
            '',
            '| Name | Value |',
            '|---|---|',
            '| one | 1 |',
        ].join('\n')))

        expect(sections.map(({ heading, anchor, text }) => ({ heading, anchor, text }))).toEqual([
            { heading: '', anchor: '', text: 'Before any heading.' },
            { heading: 'The rules.xml File', anchor: 'the-rulesxml-file', text: 'Text with a screenshot and HTML.' },
            { heading: 'Example', anchor: 'example', text: 'Example' },
            { heading: 'Example', anchor: 'example-2', text: 'Name Value one 1' },
        ])
        expect(sections[1]).toMatchObject({ id: 'a.md#the-rulesxml-file', file: 'a.md', title: 'Page A' })
    })

    it('keeps the content of code and tables, and leaves a diagram out', () => {
        const [section] = sectionsOf(page('a.md', 'A', [
            '```java',
            'class Rules {}',
            '```',
            '',
            '```openl',
            'Rules void hello()',
            '```',
            '',
            '```mermaid',
            'flowchart LR',
            '```',
        ].join('\n')))

        expect(section?.text).toBe('class Rules {} Rules void hello()')
    })
})

describe('snippetOf', () => {
    const text = `${'word '.repeat(40)}the matched Table is here and table again ${'tail '.repeat(40)}`

    it('shows the text around the first match, every match marked', () => {
        const snippet = snippetOf(text, ['table'])

        expect(snippet[0]).toEqual({ text: '…', match: false })
        expect(snippet.at(-1)).toEqual({ text: '…', match: false })
        expect(snippet.filter(part => part.match).map(part => part.text)).toEqual(['Table', 'table'])
        expect(snippet.map(part => part.text).join('')).toContain('the matched Table is here')
    })

    it('starts a short text at its beginning, and shows it whole', () => {
        expect(snippetOf('A Table.', ['table'])).toEqual([
            { text: 'A ', match: false },
            { text: 'Table', match: true },
            { text: '.', match: false },
        ])
        expect(snippetOf('Nothing matched here.', [])).toEqual([{ text: 'Nothing matched here.', match: false }])
    })

    it('reads a term literally, whatever characters it holds', () => {
        expect(snippetOf('Cost (USD) + tax', ['(usd)']).filter(part => part.match)).toEqual([{ text: '(USD)', match: true }])
    })
})

describe('indexGuides', () => {
    const index = indexGuides([
        page('studio/editor.md', 'Using Rules Editor', '## Using Rules Editor\n\nThe editor shows a table.\n\n## Viewing Tables\n\nA decision table lists rules.'),
        page('reference/decision.md', 'Decision Tables', '# Decision Tables\n\nRules of a decision table.\n\n## Merged Cells\n\nCells are merged.'),
        page('reference/spreadsheet.md', 'Spreadsheets', '# Spreadsheets\n\nA spreadsheet has steps. Decisions are made elsewhere.'),
    ])

    const found = (query: string, scope = '') => index.search(query, scope).map(result => `${result.file}#${result.anchor}`)

    it('finds the parts holding every word, the last one as a prefix', () => {
        expect(found('decision tab')).toEqual(expect.arrayContaining(['reference/decision.md#decision-tables', 'studio/editor.md#viewing-tables']))
        expect(found('decision tab')).not.toContain('reference/spreadsheet.md#spreadsheets')
        expect(found('merged')).toEqual(['reference/decision.md#merged-cells'])
    })

    it('ranks a word in the title of a page above one in its text', () => {
        expect(found('decision')[0]).toBe('reference/decision.md#decision-tables')
    })

    it('tolerates a small typo', () => {
        expect(found('spreadshet')).toContain('reference/spreadsheet.md#spreadsheets')
    })

    it('searches one folder when asked to', () => {
        expect(found('table', 'studio/')).toEqual(expect.arrayContaining(['studio/editor.md#viewing-tables']))
        expect(found('table', 'studio/').every(result => result.startsWith('studio/'))).toBe(true)
    })

    it('tells the page and the heading of a result, with a snippet of its text', () => {
        const [result] = index.search('merged', '')

        expect(result).toMatchObject({ title: 'Decision Tables', heading: 'Merged Cells', anchor: 'merged-cells' })
        expect(result?.snippet).toEqual([{ text: 'Cells are ', match: false }, { text: 'merged', match: true }, { text: '.', match: false }])
    })
})

describe('readPages', () => {
    afterEach(() => {
        vi.unstubAllGlobals()
    })

    it('reads every page it can and leaves out the others', async () => {
        vi.stubGlobal('fetch', vi.fn(async (url: string) => (url.endsWith('a.md')
            ? new Response('# A', { status: 200 })
            : new Response('', { status: 404 }))))

        const pages = await readPages([
            { file: 'a.md', title: 'A', url: 'http://host/docs/a.md' },
            { file: 'b.md', title: 'B', url: 'http://host/docs/b.md' },
        ])

        expect(pages).toEqual([{ file: 'a.md', title: 'A', text: '# A' }])
    })
})
