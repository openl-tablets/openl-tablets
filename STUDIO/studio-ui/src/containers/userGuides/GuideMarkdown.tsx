import React, { memo, useMemo } from 'react'
import { Alert } from 'antd'
import type { Element } from 'hast'
import { toString } from 'hast-util-to-string'
import Markdown, { type Components, type Options } from 'react-markdown'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import rehypeRaw from 'rehype-raw'
import rehypeSanitize from 'rehype-sanitize'
import remarkGfm from 'remark-gfm'
import { startsWithHeading, withoutFrontMatter } from './frontMatter'
import { imageSourceOf, linkTargetOf } from './guidePaths'
import {
    languageOf,
    NOTE_TAG,
    rehypeHeadingIds,
    rehypeNoteAlerts,
    SANITIZE_SCHEMA,
} from './markdownPlugins'
import { CsvTable, OpenLTable } from './GuideTables'
import { HighlightedCode } from './HighlightedCode'
import { MermaidDiagram } from './MermaidDiagram'
import { useStyles } from './UserGuides.styles'

type Styles = ReturnType<typeof useStyles>['styles']

const REMARK_PLUGINS: Options['remarkPlugins'] = [remarkGfm]

// The raw HTML is parsed and sanitized first, so what the viewer adds itself afterwards is kept.
const REHYPE_PLUGINS: Options['rehypePlugins'] = [
    rehypeRaw,
    [rehypeSanitize, SANITIZE_SCHEMA],
    rehypeHeadingIds,
    rehypeNoteAlerts,
]

interface GuideLinkProps {
    file: string
    href?: string | undefined
    children?: React.ReactNode
}

/** A link of a page: another page opens in the viewer, a file of the guides as itself, anything else in a new tab. */
const GuideLink: React.FC<GuideLinkProps> = ({ file, href, children }) => {
    const target = href ? linkTargetOf(file, href) : {}
    if ('page' in target) {
        return <Link to={target.page}>{children}</Link>
    }
    if ('file' in target) {
        return <a href={target.file}>{children}</a>
    }
    if ('external' in target) {
        return <a href={target.external} rel="noopener noreferrer" target="_blank">{children}</a>
    }
    return <span>{children}</span>
}

const codeOf = (pre: Element | undefined): Element | undefined =>
    pre?.children.find((child): child is Element => child.type === 'element' && child.tagName === 'code')

/** The code blocks drawn as something else than code: a diagram or a table. */
const DRAWN_BLOCKS: Record<string, React.FC<{ text: string }>> = {
    mermaid: ({ text }) => <MermaidDiagram source={text} />,
    csv: CsvTable,
    openl: OpenLTable,
}

/** How the parts of a page are drawn, for the page in the given file. */
const componentsOf = (file: string, styles: Styles, noteTitle: string): Components => ({
    a: ({ href, children }) => <GuideLink file={file} href={href}>{children}</GuideLink>,
    img: ({ src, alt, title, width, height }) => (
        <img
            alt={alt}
            height={height}
            src={typeof src === 'string' ? imageSourceOf(file, src) : undefined}
            title={title}
            width={width}
        />
    ),
    pre: ({ node, children }) => {
        const code = codeOf(node)
        const language = languageOf(code)
        const Drawn = DRAWN_BLOCKS[language ?? '']
        if (code && Drawn) {
            return <Drawn text={toString(code)} />
        }
        return code && language ? <HighlightedCode code={toString(code)} language={language} /> : <pre>{children}</pre>
    },
    table: ({ children }) => (
        <div className={styles.tableScroll}>
            <table className={styles.table}>{children}</table>
        </div>
    ),
    [NOTE_TAG]: ({ children }) => (
        <Alert showIcon className={styles.note} description={children} title={noteTitle} type="info" />
    ),
})

interface GuideMarkdownProps {
    /** The page, relative to the guides folder, which its relative links and images are read against. */
    file: string
    /** The Markdown text of the page. */
    text: string
    /** The title of the page, shown when the page does not open with a heading of its own. */
    title: string
}

/**
 * Draws a page of the user guides.
 *
 * A long page takes a while to draw, so it is drawn again only for another text: not when the address moves to
 * another heading of the page.
 */
export const GuideMarkdown = memo<GuideMarkdownProps>(({ file, text, title }) => {
    const { t } = useTranslation()
    const { styles } = useStyles()
    const components = useMemo(() => componentsOf(file, styles, t('guides:note')), [file, styles, t])
    const markdown = useMemo(() => withoutFrontMatter(text), [text])

    return (
        <div className={styles.markdown}>
            {!startsWithHeading(markdown) && <h1>{title}</h1>}
            <Markdown components={components} rehypePlugins={REHYPE_PLUGINS} remarkPlugins={REMARK_PLUGINS}>
                {markdown}
            </Markdown>
        </div>
    )
})
