import { guideFileUrl, type GuideEntry } from 'services/userGuides'

/** Where the viewer is opened, below the base of the application. */
export const GUIDES_ROUTE = '/docs'

/**
 * The address a page of the guides is shown at, relative to {@link GUIDES_ROUTE}: a folder ends with `/` and stands
 * for its `index.md`, and a page drops its `.md` — the way the documentation site addresses them.
 */
export const routeOfFile = (file: string): string => {
    if (file === 'index.md') {
        return ''
    }
    return file.endsWith('/index.md') ? file.slice(0, -'index.md'.length) : file.replace(/\.md$/, '')
}

/** The address of a page without the `/` a folder ends with, so both spellings find the page. */
export const routeKey = (route: string): string => {
    let end = route.length
    while (route[end - 1] === '/') {
        end--
    }
    return route.slice(0, end)
}

/** Every page of the table of contents, by the key of its address. */
export const pagesOf = (root: GuideEntry): Map<string, GuideEntry> => {
    const pages = new Map<string, GuideEntry>()
    const collect = (entry: GuideEntry) => {
        if (entry.file) {
            pages.set(routeKey(routeOfFile(entry.file)), entry)
        }
        entry.children?.forEach(collect)
    }
    collect(root)
    return pages
}

/** The entries from the root down to the given one, both included, or none when the tree does not hold it. */
export const trailTo = (entry: GuideEntry, wanted: GuideEntry): GuideEntry[] => {
    if (entry === wanted) {
        return [entry]
    }
    for (const child of entry.children ?? []) {
        const trail = trailTo(child, wanted)
        if (trail.length > 0) {
            return [entry, ...trail]
        }
    }
    return []
}

/** Whether a link leads out of the guides: it names a scheme or another host. */
export const isExternal = (href: string): boolean => /^(?:[a-z][a-z\d+.-]*:|\/\/)/i.test(href)

/**
 * The file a relative link of a page names, relative to the guides folder.
 *
 * @returns the path, or `undefined` when the link leaves the guides folder
 */
export const resolvePath = (from: string, target: string): string | undefined => {
    const segments: string[] = []
    for (const segment of `${from.slice(0, from.lastIndexOf('/') + 1)}${target}`.split('/')) {
        if (segment === '..') {
            if (segments.length === 0) {
                return undefined
            }
            segments.pop()
        } else if (segment !== '' && segment !== '.') {
            segments.push(segment)
        }
    }
    return segments.join('/')
}

const decoded = (path: string): string => {
    try {
        return decodeURI(path)
    } catch {
        return path
    }
}

/** Where a link of a page leads: out of OpenL Studio, to a page of the viewer, or to a file of the guides. */
export type GuideLinkTarget = { external: string } | { page: string } | { file: string } | Record<string, never>

/**
 * Where a link of the given page leads.
 *
 * A link to a page or a folder opens it in the viewer, keeping its `#heading`. A link to any other file of the guides,
 * such as a sample workbook, is the address of the file. A relative link leaving the guides leads nowhere.
 */
export const linkTargetOf = (from: string, href: string): GuideLinkTarget => {
    if (isExternal(href)) {
        return { external: href }
    }
    const hashAt = href.indexOf('#')
    const path = decoded(hashAt < 0 ? href : href.slice(0, hashAt))
    const hash = hashAt < 0 ? '' : href.slice(hashAt)
    const file = path === '' ? from : resolvePath(from, path)
    if (file === undefined) {
        return {}
    }
    if (file.endsWith('.md')) {
        return { page: `${GUIDES_ROUTE}/${routeOfFile(file)}${hash}` }
    }
    // A name without an extension is a folder, which stands for its index.md.
    if (!file.slice(file.lastIndexOf('/') + 1).includes('.')) {
        const folder = file === '' ? '' : `${file}/`
        return { page: `${GUIDES_ROUTE}/${folder}${hash}` }
    }
    return { file: guideFileUrl(file) }
}

/** The address of an image a page shows: an external one as it is, one of the guides as a file of the guides. */
export const imageSourceOf = (from: string, src: string): string | undefined => {
    if (isExternal(src)) {
        return src
    }
    const file = resolvePath(from, decoded(src))
    return file === undefined ? undefined : guideFileUrl(file)
}
