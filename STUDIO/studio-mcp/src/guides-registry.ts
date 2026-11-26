/**
 * The OpenL documentation the guide tools hand to agents: the specifications of the project files, the chapters of the
 * Reference Guide and the migration notes of the releases, which OpenL Studio serves at `/docs` with its user guides,
 * and the OpenL skills the server carries.
 *
 * The table of contents of OpenL Studio lists the documents with their titles and sizes: `openl_list_guides` returns
 * that metadata only, and `openl_get_guides` reads the bodies it is asked for, verbatim. Every call reads OpenL Studio,
 * so the documents are always those of the running Studio.
 *
 * A document has a stable id made of its path without the number prefixes of its folders and file, so renumbering the
 * chapters does not change the ids.
 *
 * The migration notes of a release are given as their part for Rules Authors, the section `## Rules Authors`, under
 * the id `migration/<version>`, such as `migration/7.0.0`. A release whose notes have no such section is not listed.
 * The notes are listed oldest release first, the order a project is migrated in, whatever order the table of contents
 * shows them in.
 */

import skillFiles from 'virtual:skills'
import type { OpenLClient } from './client.js'
import { sectionsOf } from './formatters.js'
import { SKILLS } from './skills.js'
import type { GUIDE_TYPES, UserGuideEntry } from './types.js'

/**
 * Document kind: 'specification' (the project files, from `docs/ref`), 'guide' (Reference Guide chapters),
 * 'migration' (the migration notes of a release, from `docs/migration-notes`) or 'skill' (a file of an OpenL skill).
 */
type GuideType = (typeof GUIDE_TYPES)[number]

/** One document: everything `openl_list_guides` returns about it. */
interface GuideEntry {
    /**
   * Stable id — the contract used by `openl_get_guides` and by AGENTS.md files to reference a document (e.g.
   * 'spec/rules.xml', 'guide/introduction/basic-concepts', 'migration/7.0.0', 'skill/planning').
   */
    id: string
    type: GuideType
    title: string
    /**
   * The path of the document from the address of OpenL Studio, such as `docs/ref/rules.xml.md`; of a file of a skill,
   * its resource URI, such as `skill://planning/SKILL.md`.
   */
    path: string
    /**
   * The size of the document in bytes — lets an agent budget openl_get_guides calls. For migration notes, the size of
   * their part for Rules Authors once {@link listGuides} has read it, else of the whole notes.
   */
    size: number
}

/** The folder of the user guides that holds the Reference Guide. */
const GUIDE_FOLDER = 'reference-guide/'

/** The title of the section of migration notes that tells what a release asks of Rules Authors. */
const RULES_AUTHORS = 'Rules Authors'

/** The page of the migration notes of a release, whose folder is named by the release. */
const MIGRATION_NOTES = /^migration-notes\/([^/]+)\/migration\.md$/

/** The folders of the user guides the documents come from, with the type and the id prefix of their documents. */
const SOURCES: ReadonlyArray<{ type: GuideType, folder: string, idPrefix: string }> = [
    { type: 'specification', folder: 'ref/', idPrefix: 'spec' },
    { type: 'guide', folder: GUIDE_FOLDER, idPrefix: 'guide' },
]

/** The start of the id of every file of a skill. */
const SKILL_PREFIX = 'skill/'

/** The `SKILL.md` of a skill, by the folder of the skill. */
const SKILL_MD = /^([^/]+)\/SKILL\.md$/

/** The id of a file of a skill, by its path in `skills/`: the skill for its `SKILL.md`, else the skill and the path. */
function skillId(path: string): string {
    return `${SKILL_PREFIX}${SKILL_MD.exec(path)?.[1] ?? path}`
}

/** The files of the skills, by id, as the server carries them. */
const SKILL_TEXTS = new Map(Object.entries(skillFiles).map(([path, text]) => [skillId(path), text]))

/** The descriptions of the skills in their frontmatter, by the id of their `SKILL.md`, lower case on one line. */
const SKILL_DESCRIPTIONS = new Map(SKILLS.entries.map(({ frontmatter: { name, description } }) =>
    [`skill/${name}`, description.replaceAll(/\s+/g, ' ').trim().toLowerCase()]))

/** The files of the skills, a skill before its other files, titled by their first heading or else their name. */
const SKILL_GUIDES: readonly GuideEntry[] = Object.entries(skillFiles)
    .map(([path, text]) => ({
        id: skillId(path),
        type: 'skill' as const,
        title: /^# (.+)$/m.exec(text)?.[1].trim() ?? path.slice(path.lastIndexOf('/') + 1),
        path: `skill://${path}`,
        size: Buffer.byteLength(text),
    }))
    .sort((a, b) => a.id.localeCompare(b.id, 'en'))

/** Strip a numeric ordering prefix from a path segment: '03-basic-concepts' → 'basic-concepts'. */
function stripOrderPrefix(segment: string): string {
    return segment.replace(/^\d+-/, '')
}

/** The id of a document: its path in the folder, without `.md` and number prefixes, after the prefix of its type. */
function guideId(idPrefix: string, relPath: string): string {
    return `${idPrefix}/${relPath.replace(/\.md$/, '').split('/').map(stripOrderPrefix).join('/')}`
}

/** Whether a page of the table of contents is a document of the folder: any page but the index of a folder. */
function isDocument(file: string, folder: string): boolean {
    return file.startsWith(folder) && !file.endsWith('/index.md')
}

type Page = Required<Pick<UserGuideEntry, 'title' | 'file'>> & Pick<UserGuideEntry, 'size'>

/** The pages of the table of contents, in its order. */
function pagesOf(entry: UserGuideEntry): Page[] {
    const own = entry.file === undefined ? [] : [{ title: entry.title, file: entry.file, size: entry.size }]
    return [...own, ...(entry.children ?? []).flatMap(pagesOf)]
}

/**
 * The documents of a table of contents: the specifications, the guides in its order, then the migration notes.
 */
function documentsOf(contents: UserGuideEntry): GuideEntry[] {
    const pages = pagesOf(contents)
    const documents = SOURCES.flatMap(({ type, folder, idPrefix }) =>
        pages
            .filter((page) => isDocument(page.file, folder))
            .map((page) => ({
                id: guideId(idPrefix, page.file.slice(folder.length)),
                type,
                title: page.title,
                path: `docs/${page.file}`,
                size: page.size ?? 0,
            }))
    )
    return [...documents, ...migrationNotesOf(pages)]
}

/** The migration notes of the releases, titled after their release, oldest first: `5.9.0` comes before `5.10.0`. */
function migrationNotesOf(pages: Page[]): GuideEntry[] {
    return pages
        .flatMap((page) => {
            const release = MIGRATION_NOTES.exec(page.file)?.[1]
            return release === undefined ? [] : [{
                id: `migration/${release}`,
                type: 'migration' as const,
                title: `OpenL Tablets ${release} Migration Notes`,
                path: `docs/${page.file}`,
                size: page.size ?? 0,
            }]
        })
        .sort((a, b) => a.id.localeCompare(b.id, 'en', { numeric: true }))
}

/** Every document: those OpenL Studio serves, which a type of skills alone leaves unread, then the skills. */
async function loadGuides(client: OpenLClient, type?: GuideType): Promise<GuideEntry[]> {
    const served = type === 'skill' ? [] : documentsOf(await client.getUserGuideContents())
    return [...served, ...SKILL_GUIDES]
}

/** Filters accepted by {@link listGuides} (all optional; omitted = match all). */
interface ListGuidesFilter {
    type?: GuideType
    /** Case-insensitive substring matched against each entry's id and title, and the description of a skill. */
    search?: string
}

/**
 * The part of migration notes for Rules Authors: the section `## Rules Authors` up to the next section of its level,
 * a heading inside a code block left alone. None when the notes have no such section.
 */
export function rulesAuthorsPart(notes: string): string | undefined {
    const text = notes.replaceAll('\r\n', '\n')
    const section = sectionsOf(text).find(({ level, title }) => level === 2 && title === RULES_AUTHORS)
    return section && text.slice(section.start, section.end).trimEnd()
}

/**
 * The text the guide tools give for a document: its page as OpenL Studio serves it, or, for migration notes, their
 * part for Rules Authors under the title of the notes. None for notes that ask nothing of Rules Authors.
 */
async function readBody(client: OpenLClient, entry: GuideEntry): Promise<string | undefined> {
    if (entry.type === 'skill') {
        return SKILL_TEXTS.get(entry.id)
    }
    const page = await client.readUserGuide(entry.path)
    if (entry.type !== 'migration') {
        return page
    }
    const part = rulesAuthorsPart(page)
    return part === undefined ? undefined : `# ${entry.title}\n\n${part}\n`
}

/**
 * The metadata of the documents (never bodies), optionally filtered. A search finds a skill by its description too,
 * which the index does not show: the server instructions do. Migration notes are read to size their part for
 * Rules Authors, and the notes that ask nothing of Rules Authors are left out.
 */
export async function listGuides(client: OpenLClient, filter: ListGuidesFilter = {}): Promise<GuideEntry[]> {
    const needle = filter.search?.toLowerCase()
    const matching = (await loadGuides(client, filter.type)).filter(
        (g) =>
            (!filter.type || g.type === filter.type) &&
            (!needle || g.id.toLowerCase().includes(needle) || g.title.toLowerCase().includes(needle)
                || SKILL_DESCRIPTIONS.get(g.id)?.includes(needle) === true)
    )
    const sized = await Promise.all(
        matching.map(async (g) => {
            if (g.type !== 'migration') {
                return [g]
            }
            const body = await readBody(client, g)
            return body === undefined ? [] : [{ ...g, size: Buffer.byteLength(body) }]
        })
    )
    return sized.flat()
}

/** One resolved document: its entry plus the full markdown body. */
interface GuideBody {
    entry: GuideEntry
    body: string
}

/**
 * The full markdown bodies of the requested ids, as OpenL Studio serves them; of migration notes, their part for Rules
 * Authors.
 *
 * Ids are de-duplicated preserving first occurrence. Unknown ids do not throw here — they are reported so the caller
 * can compose an actionable error (the tool must never silently return something else instead). Migration notes that
 * ask nothing of Rules Authors are unknown, as the index does not list them. Ids of skills alone, known or not, leave
 * OpenL Studio unread.
 */
export async function readGuideBodies(
    client: OpenLClient,
    ids: string[]
): Promise<{ found: GuideBody[], unknown: string[] }> {
    const requested = [...new Set(ids)]
    // OpenL Studio serves no id of the skills, so an unknown one among them is reported without reading it
    const skillsOnly = requested.every((id) => id.startsWith(SKILL_PREFIX))
    const byId = new Map((await loadGuides(client, skillsOnly ? 'skill' : undefined)).map((g) => [g.id, g]))
    const read = await Promise.all(
        requested
            .flatMap((id) => byId.get(id) ?? [])
            .map(async (entry) => ({ entry, body: await readBody(client, entry) }))
    )
    const found = read.filter((document): document is GuideBody => document.body !== undefined)
    const known = new Set(found.map((document) => document.entry.id))
    return { found, unknown: requested.filter((id) => !known.has(id)) }
}

/**
 * Whether a text names an id as a whole: `migration/5.26.13` does not name `migration/5.26.1`, while an id that ends a
 * sentence, as in `see spec/rules.xml.`, is named.
 */
function names(text: string, id: string): boolean {
    return new RegExp(String.raw`(?<![\w/.-])${RegExp.escape(id)}(?![\w/-]|\.\w)`).test(text)
}

/**
 * The ids of the documents a free text (e.g. an aggregated AGENTS.md document) references, in index order. This is
 * an enrichment for `openl_get_project_agent_context` — when the documents cannot be read it returns [] rather than
 * failing the caller.
 */
export async function findKnownGuideIds(client: OpenLClient, text: string): Promise<string[]> {
    try {
        const referenced = (await loadGuides(client)).filter((g) => names(text, g.id))
        // Migration notes that ask nothing of Rules Authors are no document of the index, which openl_get_guides refuses;
        // notes that cannot be read are left out alone, so the other references stay
        const readable = await Promise.all(
            referenced.map(async (g) =>
                g.type !== 'migration' || (await readBody(client, g).catch(() => undefined)) !== undefined)
        )
        return referenced.filter((_, index) => readable[index]).map((g) => g.id)
    } catch {
        return []
    }
}
