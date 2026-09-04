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
