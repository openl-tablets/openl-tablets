import CONFIG from './config'

/** An entry of the table of contents of the user guides: a page, or a folder with the entries inside it. */
export interface GuideEntry {
    title: string
    /** The page, relative to the guides folder; a folder without an `index.md` has none. */
    file?: string
    children?: GuideEntry[]
}

/**
 * Where OpenL Studio serves the user guides.
 *
 * The guides are files rather than REST resources, so they are read with `fetch` here instead of `apiCall`.
 */
const GUIDES_ROOT = `${CONFIG.CONTEXT}/docs`

/** The address of a file of the guides, given relative to the guides folder. */
export const guideFileUrl = (file: string): string =>
    `${GUIDES_ROOT}/${file.split('/').map(encodeURIComponent).join('/')}`

const read = async (file: string): Promise<Response> => {
    const response = await fetch(guideFileUrl(file))
    if (!response.ok) {
        throw new Error(`Failed to read ${file} of the user guides: ${response.status}`)
    }
    return response
}

/** The table of contents of the guides, laid out as the documentation site lays out its sidebar. */
export const fetchGuidesContents = async (): Promise<GuideEntry> => (await read('toc.json')).json()

/** The Markdown text of a page of the guides. */
export const fetchGuidePage = async (file: string): Promise<string> => (await read(file)).text()
