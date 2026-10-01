import React, { useEffect, useLayoutEffect, useRef, useState } from 'react'
import { Alert, Anchor, type AnchorProps, Skeleton, Typography } from 'antd'
import { useTranslation } from 'react-i18next'
import { useHref, useNavigate } from 'react-router-dom'
import { fetchGuidePage } from 'services/userGuides'
import { errorHandler } from 'utils/errorHandling'
import { GuideMarkdown } from './GuideMarkdown'
import { GUIDES_ROUTE, routeOfFile } from './guidePaths'
import { useStyles } from './UserGuides.styles'

/** A heading of the page a reader can jump to, with the headings under it. */
export interface OutlineItem {
    key: string
    href: string
    title: string
    children?: OutlineItem[]
}

/**
 * The headings of a page a reader can jump to: the two highest levels below its title.
 *
 * The heading a page opens with is its title, the way the documentation site reads it, so it is left out whatever its
 * level.
 *
 * A heading links to the given address of the page with the `#id` of the heading.
 */
export const outlineOf = (article: HTMLElement | null, page: string): OutlineItem[] => {
    const all = Array.from(article?.querySelectorAll<HTMLElement>('h1, h2, h3, h4, h5, h6') ?? [])
    const title = all[0]?.parentElement?.firstElementChild === all[0] ? all[0] : undefined
    const headings = all.filter(heading => heading !== title && heading.id)
    const levels = headings.map(heading => Number(heading.tagName.slice(1)))
    const first = Math.min(...levels)
    const outline: OutlineItem[] = []
    headings.forEach((heading, i) => {
        const item = { key: heading.id, href: `${page}#${heading.id}`, title: heading.textContent ?? '' }
        if (levels[i] === first) {
            outline.push({ ...item, children: []})
        } else if (levels[i] === first + 1 && outline.length > 0) {
            outline.at(-1)?.children?.push(item)
        }
    })
    return outline
}

const idOf = (hash: string): string => {
    try {
        return decodeURIComponent(hash.slice(1))
    } catch {
        return hash.slice(1)
    }
}

interface GuidePageProps {
    /** The page, relative to the guides folder. */
    file: string
    /** The title of the page in the table of contents. */
    title: string
    /** The `#heading` of the address, to be scrolled to. */
    hash: string
    /** The element the page scrolls in. */
    container: React.RefObject<HTMLElement | null>
}

/** A page of the guides with the outline of its headings beside it. */
export const GuidePage: React.FC<GuidePageProps> = ({ file, title, hash, container }) => {
    const { t } = useTranslation()
    const { styles } = useStyles()
    const navigate = useNavigate()
    const page = `${GUIDES_ROUTE}/${routeOfFile(file)}`
    const pageHref = useHref(page)
    const articleRef = useRef<HTMLDivElement>(null)
    // The heading the outline is scrolling to itself, so the page does not scroll there again when the address follows.
    const outlineTarget = useRef<string | undefined>(undefined)
    const [text, setText] = useState<string>()
    const [failed, setFailed] = useState(false)
    const [outline, setOutline] = useState<OutlineItem[]>([])

    useEffect(() => {
        let current = true
        setText(undefined)
        setFailed(false)
        fetchGuidePage(file)
            .then(page => {
                if (current) {
                    setText(page)
                }
            })
            .catch((error: unknown) => {
                errorHandler.logError(error instanceof Error ? error : new Error(String(error)))
                if (current) {
                    setFailed(true)
                }
            })
        return () => {
            current = false
        }
    }, [file])

    // The headings are read off the page once it is drawn, with the ids the renderer gave them.
    useLayoutEffect(() => {
        setOutline(text === undefined ? [] : outlineOf(articleRef.current, pageHref))
    }, [text, pageHref])

    useEffect(() => {
        const followed = outlineTarget.current === hash
        outlineTarget.current = undefined
        if (text !== undefined && !followed) {
            const id = idOf(hash)
            const target = id ? document.getElementById(id) : articleRef.current
            target?.scrollIntoView?.({ block: 'start' })
        }
    }, [text, hash])

    // Left alone, the outline writes the heading into the history itself: behind the router's back, and resolved
    // against the base of the application, which drops the address of the page. Here it only scrolls to the heading,
    // and the router takes the address.
    const followOutline: NonNullable<AnchorProps['onClick']> = (event, { href }) => {
        event.preventDefault()
        const heading = href.slice(href.indexOf('#'))
        if (heading !== hash) {
            outlineTarget.current = heading
            void navigate(`${page}${heading}`, { replace: true })
        }
    }

    const body = () => {
        if (failed) {
            return <Alert showIcon title={t('guides:page_failed')} type="error" />
        }
        if (text === undefined) {
            return <Skeleton active />
        }
        return <GuideMarkdown file={file} text={text} title={title} />
    }

    return (
        <div className={styles.page}>
            <article ref={articleRef} className={styles.article}>
                {body()}
            </article>
            <aside className={styles.outline}>
                {outline.length > 0 && (
                    <>
                        <Typography.Text strong type="secondary">{t('guides:on_this_page')}</Typography.Text>
                        <Anchor
                            affix={false}
                            getContainer={() => container.current ?? window}
                            items={outline}
                            onClick={followOutline}
                            targetOffset={16}
                        />
                    </>
                )}
            </aside>
        </div>
    )
}
