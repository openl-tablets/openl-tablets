import React from 'react'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { GuideMarkdown } from './GuideMarkdown'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

const mermaid = vi.hoisted(() => ({
    initialize: vi.fn(),
    render: vi.fn(async (id: string) => ({ svg: `<svg id="${id}"><text>drawn</text></svg>` })),
}))
vi.mock('mermaid', () => ({ default: mermaid }))

const renderPage = (text: string, file = 'openl-studio/rules-editor.md', title = 'Page Title') => render(
    <MemoryRouter>
        <GuideMarkdown file={file} text={text} title={title} />
    </MemoryRouter>
)

describe('GuideMarkdown', () => {
    it('gives every heading the id GitHub gives it', () => {
        const { container } = renderPage('# Editing & Testing\n\n## Example\n\n## Example\n\n### The `rules.xml` File\n')

        expect(Array.from(container.querySelectorAll('h1, h2, h3')).map(heading => heading.id))
            .toEqual(['editing--testing', 'example', 'example-1', 'the-rulesxml-file'])
    })

    it('shows the title of the page only when the page does not open with a heading', () => {
        renderPage('---\ntitle: Tutorials\n---\n\nSeries of tutorials.', 'getting-started/tutorials.md', 'Tutorials')

        expect(screen.getByRole('heading', { level: 1, name: 'Tutorials' })).toBeInTheDocument()
        expect(screen.queryByText(/title: Tutorials/)).not.toBeInTheDocument()
    })

    it('takes the heading a page opens with for its title', () => {
        renderPage('## Using Rules Editor\n\nText.')

        expect(screen.queryByRole('heading', { name: 'Page Title' })).not.toBeInTheDocument()
        expect(screen.getByRole('heading', { level: 2, name: 'Using Rules Editor' })).toBeInTheDocument()
    })

    it('draws a [!Note] quote as a note, without the marker', () => {
        const { container } = renderPage('> [!Note]\n> Mind the **gap**.\n\n> A plain quote.')

        const note = container.querySelector('.ant-alert-info')
        expect(note).toHaveTextContent('guides:note')
        expect(note).toHaveTextContent('Mind the gap.')
        expect(note).not.toHaveTextContent('[!Note]')
        expect(container.querySelector('blockquote')).toHaveTextContent('A plain quote.')
    })

    it('opens a page of the guides in the viewer, a file as itself, and an external link in a new tab', () => {
        renderPage([
            '[Getting started](getting-started.md#signing-in)',
            '[Workbook](samples/Example.xlsx)',
            '[Site](https://openl-tablets.org)',
            '[Elsewhere](../../DEPLOYMENT.md)',
        ].join('\n\n'))

        expect(screen.getByRole('link', { name: 'Getting started' })).toHaveAttribute('href', '/docs/openl-studio/getting-started#signing-in')
        expect(screen.getByRole('link', { name: 'Workbook' })).toHaveAttribute('href', '/docs/openl-studio/samples/Example.xlsx')
        const site = screen.getByRole('link', { name: 'Site' })
        expect(site).toHaveAttribute('target', '_blank')
        expect(site).toHaveAttribute('rel', 'noopener noreferrer')
        expect(screen.queryByRole('link', { name: 'Elsewhere' })).not.toBeInTheDocument()
        expect(screen.getByText('Elsewhere')).toBeInTheDocument()
    })

    it('shows an image of the guides from the files of the guides, at the size the page asks for', () => {
        renderPage('![Shot](images/editor.png)\n\n<img src="images/login.png" width="400" alt="Login"/>')

        expect(screen.getByRole('img', { name: 'Shot' })).toHaveAttribute('src', '/docs/openl-studio/images/editor.png')
        const sized = screen.getByRole('img', { name: 'Login' })
        expect(sized).toHaveAttribute('src', '/docs/openl-studio/images/login.png')
        expect(sized).toHaveAttribute('width', '400')
    })

    it('lets a wide table scroll, with a line break in a cell', () => {
        const { container } = renderPage('| Name | Text |\n|---|---|\n| a | one<br/>two |')

        expect(container.querySelector('div > table')).toBeInTheDocument()
        expect(container.querySelector('td br')).toBeInTheDocument()
    })

    it('keeps a YouTube player and removes what could run a script', () => {
        const { container } = renderPage([
            '<p><iframe src="https://www.youtube.com/embed/abc" width="420" allowfullscreen></iframe></p>',
            '<script>window.hacked = true</script>',
            '<img src="images/x.png" alt="X" onerror="window.hacked = true"/>',
            '[Click](javascript:alert(1))',
        ].join('\n\n'))

        expect(container.querySelector('iframe')).toHaveAttribute('src', 'https://www.youtube.com/embed/abc')
        expect(container.querySelector('script')).not.toBeInTheDocument()
        expect(screen.getByRole('img', { name: 'X' })).not.toHaveAttribute('onerror')
        expect(screen.queryByRole('link', { name: 'Click' })).not.toBeInTheDocument()
    })

    it('highlights the code of a language the guides use and leaves any other as plain text', () => {
        const { container } = renderPage('```java\npublic class Rules {}\n```\n\n```text\npublic class Plain {}\n```')

        const [java, text] = Array.from(container.querySelectorAll('pre code'))
        expect(java?.querySelector('.hljs-keyword')).toHaveTextContent('public')
        expect(text?.querySelector('[class^="hljs"]')).toBeNull()
        expect(text).toHaveTextContent('public class Plain {}')
    })

    it('draws a csv block as a table whose first record is the column header', () => {
        renderPage('```csv\nName,Note\nOne,"a, b"\nTwo\n```')

        const table = screen.getByTestId('guide-csv-table')
        expect(Array.from(table.querySelectorAll('th')).map(cell => cell.textContent)).toEqual(['Name', 'Note'])
        expect(Array.from(table.querySelectorAll('tbody tr')).map(row => Array.from(row.querySelectorAll('td')).map(cell => cell.textContent)))
            .toEqual([['One', 'a, b'], ['Two', '']])
    })

    it('draws an openl block the way the table editor draws an OpenL table', () => {
        renderPage('```openl\nRules String hello(Integer hour)\nC1,RET1\n---\n0-12,Good Morning\n^,Good Day\n```')

        const table = screen.getByTestId('guide-openl-table')
        const header = screen.getByText('Rules String hello(Integer hour)')
        expect(header).toHaveAttribute('colspan', '2')
        expect(screen.getByText('0-12')).toHaveAttribute('rowspan', '2')
        expect(table.querySelectorAll('tr')).toHaveLength(4)
        expect(screen.getByText('C1').className).toBe(header.className)
        expect(screen.getByText('Good Day').className).not.toBe(header.className)
    })

    it('draws a mermaid block as a diagram', async () => {
        renderPage('```mermaid\nflowchart LR\n  A --> B\n```')

        expect(await screen.findByTestId('guide-diagram')).toHaveTextContent('drawn')
        expect(mermaid.render).toHaveBeenCalledWith(expect.stringMatching(/^guide-diagram-/), 'flowchart LR\n  A --> B\n')
        await waitFor(() => expect(mermaid.initialize).toHaveBeenCalledWith(expect.objectContaining({ securityLevel: 'strict' })))
    })
})
