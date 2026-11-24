import { createContext, useContext } from 'react'
import { nameKey } from './tagNames'

/**
 * The configured tag types of the projects screen, in the order an administrator arranged them in on the Tags
 * page: the latest read of any part of the screen. Empty while they are not read or cannot be, and null outside the
 * screen, where a part reads them itself.
 */
export const TagTypeOrder = createContext<readonly string[] | null>(null)

/** The configured tag types of the projects screen, in their order; none outside the screen. */
export const useTagTypeOrder = (): readonly string[] => useContext(TagTypeOrder) ?? []

/**
 * Hands the projects screen the configured tag types a part of it read itself, in their order, so the whole screen
 * follows the latest read: the list, the rail and the tree take the colours and the order the Overview of a project
 * does. Does nothing outside the screen.
 */
export const PublishTagTypes = createContext<(names: readonly string[]) => void>(() => undefined)

/**
 * The rank of a tag type in the order the configured tag types are arranged in. Types match regardless of case;
 * a type that is not configured ranks after every configured one.
 */
export const tagTypeRank = (types: readonly string[]): ((type: string) => number) => {
    const ranks = new Map(types.map((type, index) => [nameKey(type), index]))
    return type => ranks.get(nameKey(type)) ?? types.length
}

/**
 * The items in the order of the configured tag types. The items of types that are not configured follow, keeping
 * the order they came in.
 */
export const inTagTypeOrder = <T>(types: readonly string[], items: readonly T[], typeOf: (item: T) => string): T[] => {
    const rank = tagTypeRank(types)
    return items.toSorted((one, other) => rank(typeOf(one)) - rank(typeOf(other)))
}

/**
 * The items in the order of the configured tag types. The items of types that are not configured follow, in
 * alphabetical order regardless of case.
 */
export const inTagTypeThenNameOrder = <T>(types: readonly string[], items: readonly T[], typeOf: (item: T) => string): T[] => {
    const rank = tagTypeRank(types)
    return items.toSorted((one, other) => rank(typeOf(one)) - rank(typeOf(other))
        || typeOf(one).localeCompare(typeOf(other), undefined, { sensitivity: 'base' }))
}
