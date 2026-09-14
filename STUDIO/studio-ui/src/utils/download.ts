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
        URL.revokeObjectURL(url)
    }
}
