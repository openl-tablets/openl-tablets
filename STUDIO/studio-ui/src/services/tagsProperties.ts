// The tags.properties file of a project, written on the client the way the descriptors are: the file is
// the source of truth for the tags, and the UI edits it through the generic files API.

/** The file at the project root that holds the tags. */
export const TAGS_FILE_NAME = 'tags.properties'

/** One tag: the key (tag type) and the value the project carries for it. */
export interface TagEntry {
    key: string
    value: string
}

// Mirrors the escaping of the server-side properties writer (PropertiesUtils.store), so a file written
// here reads back identically on the server.
const escapeText = (value: string): string => value
    .replaceAll('\u005C', String.raw`\\`)
    .replaceAll('\f', String.raw`\f`)
    .replaceAll('\t', String.raw`\t`)
    .replaceAll('\r', String.raw`\r`)
    .replaceAll('\n', String.raw`\n`)

const escapeKey = (key: string): string => escapeText(key)
    .replaceAll(':', String.raw`\:`)
    .replaceAll('=', String.raw`\=`)
    .replace(/^#/, String.raw`\#`)

/**
 * Serializes tags to the properties text the server reads. Entries with a blank key or value are left
 * out — a tag without both sides says nothing.
 */
export const serializeTagsProperties = (entries: TagEntry[]): string => entries
    .map(entry => ({ key: entry.key.trim(), value: entry.value.trim() }))
    .filter(entry => entry.key && entry.value)
    .map(entry => `${escapeKey(entry.key)}=${escapeText(entry.value)}\n`)
    .join('')
