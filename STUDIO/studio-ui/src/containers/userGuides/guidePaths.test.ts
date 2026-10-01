import type { GuideEntry } from 'services/userGuides'
import {
    imageSourceOf,
    isExternal,
    linkTargetOf,
    pagesOf,
    resolvePath,
    routeKey,
    routeOfFile,
    startsWithHeading,
    trailTo,
    withoutFrontMatter,
} from './guidePaths'

const editor: GuideEntry = { title: 'Using Rules Editor', file: 'openl-studio/rules-editor.md' }
const appendices: GuideEntry = { title: 'Appendices', children: [{ title: 'Error Pages', file: 'openl-studio/appendices/error-pages.md' }]}
const studio: GuideEntry = { title: 'OpenL Studio User Guide', file: 'openl-studio/index.md', children: [editor, appendices]}
const contents: GuideEntry = { title: 'User Guides', file: 'index.md', children: [studio]}

describe('guidePaths', () => {
    it('addresses a page the way the documentation site does', () => {
        expect(routeOfFile('index.md')).toBe('')
        expect(routeOfFile('openl-studio/index.md')).toBe('openl-studio/')
        expect(routeOfFile('openl-studio/rules-editor.md')).toBe('openl-studio/rules-editor')
        expect(routeKey('openl-studio/')).toBe('openl-studio')
    })

    it('finds every page by its address, with or without the slash of a folder', () => {
        const pages = pagesOf(contents)

        expect([...pages.keys()]).toEqual(['', 'openl-studio', 'openl-studio/rules-editor', 'openl-studio/appendices/error-pages'])
        expect(pages.get(routeKey('openl-studio/'))).toBe(studio)
    })

    it('leads from the root to an entry', () => {
        expect(trailTo(contents, editor)).toEqual([contents, studio, editor])
        expect(trailTo(contents, { title: 'Elsewhere' })).toEqual([])
    })

    it('resolves a relative path against the folder of the page', () => {
        expect(resolvePath('openl-studio/rules-editor.md', 'images/a.png')).toBe('openl-studio/images/a.png')
        expect(resolvePath('openl-studio/appendices/x.md', '../rules-editor.md')).toBe('openl-studio/rules-editor.md')
        expect(resolvePath('openl-studio/rules-editor.md', './appendices/')).toBe('openl-studio/appendices')
        expect(resolvePath('openl-studio/rules-editor.md', '../../DEPLOYMENT.md')).toBeUndefined()
    })

    it('tells a link out of OpenL Studio by its scheme or host', () => {
        expect(isExternal('https://openl-tablets.org')).toBe(true)
        expect(isExternal('mailto:openl@example.org')).toBe(true)
        expect(isExternal('//cdn.example.org/a.js')).toBe(true)
        expect(isExternal('rules-editor.md')).toBe(false)
        expect(isExternal('#top')).toBe(false)
    })

    it('opens a page or a folder of the guides in the viewer, keeping the heading', () => {
        const from = 'openl-studio/rules-editor.md'

        expect(linkTargetOf(from, 'getting-started.md#signing-in')).toEqual({ page: '/docs/openl-studio/getting-started#signing-in' })
        expect(linkTargetOf(from, '../index.md')).toEqual({ page: '/docs/' })
        expect(linkTargetOf(from, 'administration/')).toEqual({ page: '/docs/openl-studio/administration/' })
        expect(linkTargetOf(from, '..')).toEqual({ page: '/docs/' })
        expect(linkTargetOf(from, '#viewing-a-module')).toEqual({ page: '/docs/openl-studio/rules-editor#viewing-a-module' })
        expect(linkTargetOf(from, 'my%20page.md')).toEqual({ page: '/docs/openl-studio/my page' })
    })

    it('opens any other file of the guides as itself, and an external link as it is', () => {
        const from = 'getting-started/tutorials.md'

        expect(linkTargetOf(from, 'samples/Tutorial 1.xlsx')).toEqual({ file: '/docs/getting-started/samples/Tutorial%201.xlsx' })
        expect(linkTargetOf(from, 'https://openl-tablets.org/downloads')).toEqual({ external: 'https://openl-tablets.org/downloads' })
        expect(linkTargetOf(from, '../../DEPLOYMENT.md')).toEqual({})
    })

    it('shows an image of the guides as a file of the guides', () => {
        expect(imageSourceOf('openl-studio/appendices/error-pages.md', '../images/404.png')).toBe('/docs/openl-studio/images/404.png')
        expect(imageSourceOf('index.md', 'https://img.example.org/a.png')).toBe('https://img.example.org/a.png')
        expect(imageSourceOf('index.md', '../a.png')).toBeUndefined()
    })

    it('leaves a malformed address as it is written', () => {
        expect(linkTargetOf('index.md', 'bad%E0.md')).toEqual({ page: '/docs/bad%E0' })
    })

    it('drops the front matter, which the reader is not shown', () => {
        expect(withoutFrontMatter('---\ntitle: Tutorials\n---\n\nText')).toBe('\nText')
        expect(withoutFrontMatter('---\n\nA rule, then text')).toBe('---\n\nA rule, then text')
        expect(withoutFrontMatter('# Title')).toBe('# Title')
    })

    it('tells whether a page opens with a heading of its own', () => {
        expect(startsWithHeading('\n## Using Rules Editor\n')).toBe(true)
        expect(startsWithHeading('---\ntitle: Tutorials\n---\nSeries of tutorials')).toBe(false)
        expect(startsWithHeading('#hashtag is no heading')).toBe(false)
    })
})
