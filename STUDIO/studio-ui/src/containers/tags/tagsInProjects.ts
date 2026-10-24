import type { TagFacetSummary } from '../../types/projects'
import type { TagType } from './TagTable'
import { groupQuery } from '../projects/projectGrouping'
import { nameKey } from './tagNames'

/** A tag value the projects carry, and how many projects carry it. */
export interface FoundTag {
    value: string
    projects: number
}

/** A tag type the projects carry that is not configured, with the values they carry for it. */
export interface FoundTagType {
    type: string
    values: FoundTag[]
}

/** What the projects carry beyond the configuration. */
interface TagsInProjects {
    /** The values a configured tag type does not have, by the id of the tag type. */
    values: Map<number, FoundTag[]>
    /** The tag types that are not configured at all. */
    types: FoundTagType[]
}

/**
 * Compares the tags the projects carry with the configured tag types and values.
 *
 * Names match regardless of case, the way the server resolves them, so a value configured under another case is
 * not reported. The values keep the order the projects summary lists them in.
 */
export const tagsInProjects = (tagTypes: TagType[], carried: TagFacetSummary[]): TagsInProjects => {
    const configured = new Map(tagTypes.map(type => [nameKey(type.name), type]))
    const values = new Map<number, FoundTag[]>()
    const types: FoundTagType[] = []
    for (const { type: typeName, values: counts } of carried) {
        const found = counts.map(count => ({ value: count.name, projects: count.count }))
        const type = configured.get(nameKey(typeName))
        if (type) {
            const known = new Set((type.tags ?? []).map(tag => nameKey(tag.name)))
            const extra = found.filter(tag => !known.has(nameKey(tag.value)))
            if (extra.length > 0) {
                values.set(type.id, extra)
            }
        } else if (found.length > 0) {
            types.push({ type: typeName, values: found })
        }
    }
    return { values, types }
}

/** The address of the projects list showing the projects that carry the tag. */
export const projectsWithTag = (type: string, value: string): string =>
    '/projects?' + groupQuery({ repositories: [], branches: [], tags: [type + ':' + value]})
