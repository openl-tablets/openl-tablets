import { useCallback, useEffect, useRef, useState } from 'react'
import { fetchMessageKeys, fetchMessages, type MessageQuery } from '../../services/localization'
import type { MessageLanguageView, MessageRow } from '../../types/localization'
import { errorMessage } from '../../utils/errorMessage'
import { NO_FILTER } from './messageFilter'

/** How many keys a page of the table holds. */
export const PAGE_SIZE = 250

/** The keys of the table read so far, page by page. */
export interface MessagePages {
    /** The languages of the project, or null until the first page is read. */
    languages: MessageLanguageView[] | null
    rows: MessageRow[]
    /** How many keys the query leaves in all. */
    total: number
    loading: boolean
    failure: string | null
    /** Reads the next page, unless every key is read or a page is being read. */
    loadMore: () => void
}

/**
 * Reads the keys of the messages the query leaves a page at a time: the first page at once, each next one when the
 * reader asks for more. A new query, or a new token, reads them again from the first page.
 *
 * @param projectId the project
 * @param query     what the keys are narrowed down to
 * @param token     bumped to read the keys afresh, as after a save
 * @param enabled   whether the keys are read at all
 */
export const useMessagePages = (projectId: string, query: MessageQuery, token: number, enabled: boolean): MessagePages => {
    const [state, setState] = useState<Omit<MessagePages, 'loadMore'>>({
        languages: null, rows: [], total: 0, loading: false, failure: null,
    })
    // Bumped by every new read from the first page, so a page of an older one is dropped when it comes.
    const generation = useRef(0)
    const loading = useRef(false)

    // A page is put where it stands among the keys, so a page read again replaces itself rather than being added twice.
    // A page shorter than the size is the last one, whatever the total said: the keys changed since it was counted.
    const read = useCallback((page: number) => {
        const from = page * PAGE_SIZE
        const asked = generation.current
        loading.current = true
        setState(current => ({ ...current, loading: true }))
        fetchMessages(projectId, query, { page, size: PAGE_SIZE })
            .then(read => {
                if (asked === generation.current) {
                    const content = read.content ?? []
                    setState(current => ({
                        languages: read.languages ?? [],
                        rows: [...current.rows.slice(0, from), ...content],
                        total: content.length < PAGE_SIZE ? from + content.length : read.total ?? 0,
                        loading: false,
                        failure: null,
                    }))
                }
            })
            .catch((error: unknown) => {
                if (asked === generation.current) {
                    setState(current => ({ ...current, loading: false, failure: errorMessage(error) }))
                }
            })
            .finally(() => {
                if (asked === generation.current) {
                    loading.current = false
                }
            })
    }, [projectId, query])

    useEffect(() => {
        if (!enabled) {
            return
        }
        generation.current++
        read(0)
    }, [enabled, read, token])

    const { rows, total } = state
    const loadMore = useCallback(() => {
        if (!loading.current && rows.length < total) {
            read(Math.floor(rows.length / PAGE_SIZE))
        }
    }, [read, rows.length, total])

    return { ...state, loadMore }
}

/** One key of the messages with its texts in every language, as its page shows it. */
export interface MessageKeyPage {
    languages: MessageLanguageView[] | null
    /** The key, or null for one neither the project nor a project it depends on gives; undefined while it is read. */
    row: MessageRow | null | undefined
    failure: string | null
}

/** Reads one key of the messages, again whenever the token is bumped. */
export const useMessageKey = (projectId: string, key: string | null, token: number, enabled: boolean): MessageKeyPage => {
    const [state, setState] = useState<MessageKeyPage & { of: string | null }>({
        of: null, languages: null, row: undefined, failure: null,
    })
    useEffect(() => {
        if (!enabled || key === null) {
            return undefined
        }
        let current = true
        fetchMessages(projectId, NO_FILTER, { key, page: 0, size: 1 })
            .then(read => {
                if (current) {
                    setState({ of: key, languages: read.languages ?? [], row: read.content?.[0] ?? null, failure: null })
                }
            })
            .catch((error: unknown) => {
                if (current) {
                    setState({ of: key, languages: null, row: null, failure: errorMessage(error) })
                }
            })
        return () => {
            current = false
        }
    }, [projectId, key, token, enabled])
    // What was read about another key is not this one's.
    return state.of === key ? state : { languages: state.languages, row: undefined, failure: null }
}

/** Reads every key, for the tree of the keys, again whenever the token is bumped. */
export const useMessageKeys = (projectId: string, token: number, enabled: boolean): string[] | null => {
    const [keys, setKeys] = useState<string[] | null>(null)
    useEffect(() => {
        if (!enabled) {
            return undefined
        }
        let current = true
        fetchMessageKeys(projectId)
            .then(read => {
                if (current) {
                    setKeys(read)
                }
            })
            // A tree that cannot be read lists nothing: the table says why.
            .catch(() => {
                if (current) {
                    setKeys([])
                }
            })
        return () => {
            current = false
        }
    }, [projectId, token, enabled])
    return keys
}
