/** Reads UTF-8 and refuses bytes that are not UTF-8, instead of putting the replacement character in their place. */
const UTF8 = new TextDecoder('utf-8', { fatal: true })

/** Reads every byte as a character of its own, as Western European text was written before UTF-8. */
const WINDOWS_1252 = new TextDecoder('windows-1252')

/**
 * The text a file holds, read from its bytes. A file written in UTF-8 reads as written.
 *
 * A file in another encoding, such as a properties file in ISO-8859-1, reads as Windows-1252, the superset of
 * ISO-8859-1 that browsers read it as. Every byte stays a character of its own, so two lines that differ in a single
 * letter still differ, rather than both showing the replacement character.
 */
export const decodeText = (bytes: ArrayBuffer): string => {
    try {
        return UTF8.decode(bytes)
    } catch {
        return WINDOWS_1252.decode(bytes)
    }
}
