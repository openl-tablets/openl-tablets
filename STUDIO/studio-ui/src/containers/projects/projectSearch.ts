/** The filter qualifiers parsed out of the projects search box. */
export interface ProjectSearch {
    name?: string
    author?: string
    branch?: string
}

// A token is an optional `key:` prefix followed by a quoted or bare value.
const TOKEN = /(\w+:)?("[^"]*"|'[^']*'|[^\s"']+)/g

const unquote = (value: string): string =>
    (value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))
        ? value.slice(1, -1)
        : value

/**
 * Parses a search string into filter qualifiers. Bare words (and unknown qualifiers)
 * become the `name` filter; `author:`, `branch:` and `name:` qualifiers set their own field. Values
 * may be quoted to include spaces, e.g. `author:"John Doe" branch:main alpha`.
 */
export const parseProjectSearch = (query: string): ProjectSearch => {
    const result: ProjectSearch = {}
    const nameParts: string[] = []
    for (const match of query.matchAll(TOKEN)) {
        const key = match[1]?.slice(0, -1).toLowerCase()
        const value = unquote(match[2] ?? '')
        if (key === 'author' || key === 'branch') {
            result[key] = value
        } else if (key === 'name') {
            nameParts.push(value)
        } else {
            // A bare word, or an unrecognized `key:value` kept verbatim, contributes to the name.
            nameParts.push((match[1] ?? '') + value)
        }
    }
    const name = nameParts.join(' ').trim()
    if (name) {
        result.name = name
    }
    return result
}
