import type { TagFacetSummary } from '../../types/projects'
import type { TagType } from './TagTable'
import { groupQuery } from '../projects/projectGrouping'
import { nameKey } from './tagNames'

/** A tag value the projects carry, and how many projects carry it. */
export interface FoundTag {
    value: string
    projects: number
}

/** A tag type the projects carry values of that are not configured, with those values. */
export interface FoundTagType {
    /** The name of the tag type: as it is configured, or as the projects spell it when it is not. */
    type: string
    /** Whether the tag type is configured, so that only its values are missing. */
    defined: boolean
    values: FoundTag[]
}

/**
 * Compares the tags the projects carry with the configured tag types and values, and answers what the
 * configuration does not have: a tag type that is not configured with every value the projects carry for it, and
 * a configured one with the values it lacks.
 *
 * Names match regardless of case, the way the server resolves them, so a value configured under another case is
 * not reported. The tag types and the values keep the order the projects summary lists them in.
 */
export const tagsInProjects = (tagTypes: TagType[], carried: TagFacetSummary[]): FoundTagType[] => {
    const configured = new Map(tagTypes.map(type => [nameKey(type.name), type]))
    return carried.flatMap(({ type: typeName, values: counts }): FoundTagType[] => {
        const type = configured.get(nameKey(typeName))
        const known = new Set((type?.tags ?? []).map(tag => nameKey(tag.name)))
        const values = counts
            .filter(count => !known.has(nameKey(count.name)))
            .map(count => ({ value: count.name, projects: count.count }))
        return values.length > 0 ? [{ type: type?.name ?? typeName, defined: type !== undefined, values }] : []
    })
}

/** The address of the projects list showing the projects that carry the tag. */
export const projectsWithTag = (type: string, value: string): string =>
    '/projects?' + groupQuery({ repositories: [], branches: [], tags: [type + ':' + value]})
