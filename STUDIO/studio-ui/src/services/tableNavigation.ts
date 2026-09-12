import apiCall from './apiCall'

const TABLE_URL_API_OPTIONS = { throwError: true, suppressErrorPages: true }

/**
 * The address a table of the open project has in the editor.
 *
 * The address is the server's to give: it answers with a fragment of the editor page, such as
 * `#repo/project/module/table`, which the shell resolves on a hash change. It is used as it stands, because an
 * origin in front of it would leave the editor and lose the context path.
 *
 * A table of another project has no address here.
 *
 * @param tableId the table, as the APIs of the project report it
 * @return the address of the table, or `null` when it has none
 */
const tableUrl = async (tableId: string): Promise<string | null> => {
    try {
        const resolved = await apiCall(
            `/compile/table/${tableId}/url`,
            { method: 'GET' },
            TABLE_URL_API_OPTIONS
        ) as { url?: string | null } | null
        return resolved?.url ? `${resolved.url}?id=${tableId}` : null
    } catch {
        return null
    }
}

/**
 * Opens a table of the open project in the editor.
 *
 * A table that has no address here is left alone, and nothing happens.
 *
 * @param tableId the table, as the APIs of the project report it
 * @return whether the editor went to the table
 */
export const openTableInEditor = async (tableId: string): Promise<boolean> => {
    const url = await tableUrl(tableId)
    if (url === null) {
        return false
    }
    globalThis.location.href = url
    return true
}
