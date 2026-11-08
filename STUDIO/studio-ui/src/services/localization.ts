import apiCall, { LOCAL_LOAD_API_OPTIONS } from './apiCall'
import type { ProjectStatusDetailedMessage } from './projectStatus'
import type { LocalizationAction, MessagesPage } from '../types/localization'

/** The folder of a project the engine reads the messages from. */
export const BUNDLE_FOLDER = 'i18n'

/**
 * The name of a localization file: `message.properties`, or `message_` with a language tag, as
 * `message_de_LU.properties`: a language of two or three letters, then the country and the variant.
 */
const MESSAGE_FILE = /^message(_[A-Za-z]{2,3}(?:_\w*[A-Za-z0-9])?)?\.properties$/

/** Whether a path of a project names one of its localization files, the ones the Localization screen edits. */
export const isMessageFilePath = (path: string): boolean => {
    const slash = path.lastIndexOf('/')
    return path.slice(0, slash) === BUNDLE_FOLDER && MESSAGE_FILE.test(path.slice(slash + 1))
}

/** What the pages of the keys of the messages are narrowed down to, and the order of the keys, as the server takes it. */
export interface MessageQuery {
    /** A piece of a key or of a text of the project in any language, in any case. */
    search: string
    /** Only the keys the check of the files warns about. */
    onlyProblems: boolean
    /** Only the keys each of these languages shows the default text for; empty for any. */
    notTranslatedInto: readonly string[]
    /** The keys by their names, or null in the order they are written. */
    order: 'asc' | 'desc' | null
}

const queryOf = (query: MessageQuery): URLSearchParams => {
    const params = new URLSearchParams()
    if (query.search.trim() !== '') {
        params.set('search', query.search.trim())
    }
    if (query.onlyProblems) {
        params.set('problems', 'true')
    }
    query.notTranslatedInto.forEach(tag => params.append('untranslated', tag))
    if (query.order !== null) {
        params.set('sort', query.order)
    }
    return params
}

const localizationUrl = (projectId: string, path: string, params: URLSearchParams): string => {
    const query = params.toString()
    const resource = path === '' ? `/projects/${projectId}/localization` : `/projects/${projectId}/localization/${path}`
    return query === '' ? resource : `${resource}?${query}`
}

/**
 * Every key of the messages of an open project, as the nodes of their tree, in the order they are written: those of
 * the default file first, then the rest.
 */
export async function fetchMessageKeys(projectId: string): Promise<string[]> {
    const nodes = await apiCall(
        localizationUrl(projectId, 'keys', new URLSearchParams()),
        undefined,
        LOCAL_LOAD_API_OPTIONS
    ) as Array<{ key: string }>
    return nodes.map(node => node.key)
}

/**
 * A page of the keys of the messages of an open project the query leaves, each with its texts in every language, with
 * the languages of the project. A key asked for is answered alone, whatever the query.
 */
export async function fetchMessages(
    projectId: string,
    query: MessageQuery,
    { key, page, size }: { key?: string, page: number, size: number }
): Promise<MessagesPage> {
    const params = key === undefined ? queryOf(query) : new URLSearchParams({ key })
    params.set('page', String(page))
    params.set('size', String(size))
    return await apiCall(
        localizationUrl(projectId, '', params),
        undefined,
        LOCAL_LOAD_API_OPTIONS
    ) as MessagesPage
}

/**
 * Makes the changes of the localization files of an open project in order, as one write: a change refused refuses
 * them all. Only what changes is written.
 */
export async function applyMessageActions(projectId: string, actions: LocalizationAction[]): Promise<void> {
    await apiCall(
        `/projects/${projectId}/localization/actions`,
        { method: 'POST', body: JSON.stringify({ actions }), headers: { 'Content-Type': 'application/json' } },
        { throwError: true }
    )
}

/**
 * What the places of a problem of a localization file are counted from: the pieces of the text of the key, which a
 * screen of the texts marks, or the places of the file, which an editor of the file marks.
 */
export type MessagePositions = 'text' | 'file'

/**
 * Checks the localization files of a project, without compiling it: what is wrong with each text, by its key and
 * language, each problem placed as the positions say. A project nobody opened is not read, and nothing is found about
 * it.
 */
export async function checkMessageFiles(
    projectId: string,
    positions: MessagePositions
): Promise<ProjectStatusDetailedMessage[]> {
    return await apiCall(
        `/projects/${projectId}/localization/problems?positions=${positions}`,
        undefined,
        LOCAL_LOAD_API_OPTIONS
    ) as ProjectStatusDetailedMessage[]
}
