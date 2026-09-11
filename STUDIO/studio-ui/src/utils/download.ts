/**
 * Trigger a browser download of the given URL via a transient anchor. Pass a filename to set the
 * download name; an empty name lets the server's Content-Disposition decide.
 */
export function triggerDownload(url: string, filename = ''): void {
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = filename
    document.body.appendChild(anchor)
    anchor.click()
    anchor.remove()
}

/**
 * How long the address of a saved file stays alive after the click.
 *
 * The browser reads it when it starts the download, which is not the tick of the click: a browser asking
 * the user where to save the file reads it only once that is answered. Released before then, the address
 * names nothing and the download is abandoned; the file is held in memory until it is released.
 */
const RELEASE_AFTER_MS = 40_000

/**
 * Saves content the browser already holds to the user's machine, under the given name.
 *
 * The address the download goes through is made and released here, so a caller only says what to save.
 */
export function saveFile(content: Blob | string, filename: string, type = 'text/plain;charset=utf-8'): void {
    const blob = typeof content === 'string' ? new Blob([content], { type }) : content
    const url = URL.createObjectURL(blob)
    try {
        triggerDownload(url, filename)
    } finally {
        setTimeout(() => URL.revokeObjectURL(url), RELEASE_AFTER_MS)
    }
}
