import React, { useEffect, useMemo, useRef, useState } from 'react'
import { SearchOutlined } from '@ant-design/icons'
import { Empty, Input, Select, Spin } from 'antd'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import type { GuideEntry } from 'services/userGuides'
import { errorHandler } from 'utils/errorHandling'
import { GUIDES_ROUTE, routeOfFile, trailTo } from './guidePaths'
import type { GuideSearchResult } from './guideSearch'
import { type GuideSearch, startGuideSearch } from './guideSearchClient'
import { useStyles } from './UserGuides.styles'

/** How long typing pauses before the query is searched, in milliseconds. */
const SEARCH_DELAY = 200

interface ScopeOption {
    value: string
    label: string
}

/** The folders above a page, each a part of the guides a search can be narrowed to, the outermost first. */
export const scopesOf = (trail: GuideEntry[], file: string | undefined): ScopeOption[] => {
    const folders = file?.split('/') ?? []
    return trail.slice(1).flatMap((entry, depth) => (entry.children?.length
        ? [{ value: `${folders.slice(0, depth + 1).join('/')}/`, label: entry.title }]
        : []))
}

interface SearchResultsProps {
    results: GuideSearchResult[] | undefined
}

/** The address a result opens: its page, at the heading of its part when the part has one. */
const addressOf = ({ file, anchor }: GuideSearchResult): string => {
    const heading = anchor ? `#${anchor}` : ''
    return `${GUIDES_ROUTE}/${routeOfFile(file)}${heading}`
}

/** The parts of the pages a search found, each opening its page at the part it found. */
const SearchResults: React.FC<SearchResultsProps> = ({ results }) => {
    const { t } = useTranslation()
    const { styles } = useStyles()
    if (results === undefined) {
        return <Spin className={styles.state} description={t('guides:searching')} />
    }
    if (results.length === 0) {
        return <Empty className={styles.state} description={t('guides:no_matches')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
    }
    return (
        <ul aria-label={t('guides:results')} className={styles.results}>
            {results.map(result => (
                <li key={`${result.file}#${result.anchor}`}>
                    <Link className={styles.result} to={addressOf(result)}>
                        <span className={styles.resultTitle}>{result.heading || result.title}</span>
                        {result.heading !== '' && result.heading !== result.title && (
                            <span className={styles.resultPage}>{result.title}</span>
                        )}
                        <span className={styles.snippet}>
                            {result.snippet.map(part => (part.match
                                ? <mark key={part.at}>{part.text}</mark>
                                : <React.Fragment key={part.at}>{part.text}</React.Fragment>))}
                        </span>
                    </Link>
                </li>
            ))}
        </ul>
    )
}

interface GuidesSearchProps {
    contents: GuideEntry
    /** The page shown, whose folders a search can be narrowed to. */
    current?: GuideEntry | undefined
    /** What the rail shows while nothing is searched: the table of contents. */
    children: React.ReactNode
}

/**
 * Searches the text of the guides, all of them or one folder above the page shown.
 *
 * The search runs in a worker, started with the first search: it reads every page once and indexes it, so a
 * reader who never searches never downloads the guides. While a query is typed, the results take the place of the
 * table of contents, which keeps the folders the reader opened.
 */
export const GuidesSearch: React.FC<GuidesSearchProps> = ({ contents, current, children }) => {
    const { t } = useTranslation()
    const { styles } = useStyles()
    const [text, setText] = useState('')
    const [scope, setScope] = useState('')
    const [results, setResults] = useState<GuideSearchResult[]>()
    const search = useRef<GuideSearch | undefined>(undefined)
    const scopes = useMemo(() => [
        { value: '', label: t('guides:all_guides') },
        ...scopesOf(current ? trailTo(contents, current) : [], current?.file),
    ], [contents, current, t])
    // A folder left behind by opening a page elsewhere is no scope any more, so the search widens to all guides.
    const shownScope = scopes.some(option => option.value === scope) ? scope : ''
    const query = text.trim()

    useEffect(() => () => search.current?.dispose(), [])

    useEffect(() => {
        let wanted = true
        setResults(undefined)
        const timer = query === '' ? undefined : setTimeout(() => {
            search.current ??= startGuideSearch(contents)
            search.current.search(query, shownScope)
                .then(found => {
                    if (wanted) {
                        setResults(found)
                    }
                })
                .catch((error: unknown) => {
                    errorHandler.logError(error instanceof Error ? error : new Error(String(error)))
                    if (wanted) {
                        setResults([])
                    }
                })
        }, SEARCH_DELAY)
        return () => {
            wanted = false
            clearTimeout(timer)
        }
    }, [contents, query, shownScope])

    return (
        <>
            <div className={styles.search}>
                <Input
                    allowClear
                    aria-label={t('guides:search')}
                    onChange={event => setText(event.target.value)}
                    placeholder={t('guides:search_placeholder')}
                    prefix={<SearchOutlined />}
                    value={text}
                />
                <Select
                    aria-label={t('guides:scope')}
                    className={styles.scope}
                    onChange={setScope}
                    options={scopes}
                    popupMatchSelectWidth={false}
                    size="small"
                    value={shownScope}
                />
            </div>
            {query !== '' && <SearchResults results={results} />}
            <div className={styles.railSlot} hidden={query !== ''}>
                {children}
            </div>
        </>
    )
}
