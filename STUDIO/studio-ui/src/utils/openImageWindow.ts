/** How the page of a picture opened on its own is laid out: the picture whole, centred on a dark ground. */
const PAGE_STYLE = `
    html, body { height: 100%; margin: 0; }
    body { display: flex; align-items: center; justify-content: center; background: #0e0e0e; }
    img { max-width: 100%; max-height: 100%; object-fit: contain; }
`

/**
 * Opens a picture in a browser tab of its own, titled by the name of its file.
 *
 * <p>The tab is a blank page of the application holding nothing but the picture, drawn as an image: a picture the
 * server answers as a download is shown rather than saved, and a drawing in SVG runs no script of its own.
 *
 * @returns whether the tab opened; a browser blocking pop-ups opens none
 */
export const openImageWindow = (url: string, name: string): boolean => {
    const opened = window.open('', '_blank')
    if (!opened) {
        return false
    }
    const page = opened.document
    page.title = name
    const style = page.createElement('style')
    style.textContent = PAGE_STYLE
    const image = page.createElement('img')
    image.alt = name
    image.src = new URL(url, window.location.href).href
    page.head.append(style)
    page.body.append(image)
    return true
}
