import CONFIG from './config'

/** A third-party library OpenL Studio ships, and the license it is distributed under. */
export interface License {
    /** The npm package of a frontend library, `groupId:artifactId` of a backend one. */
    name: string
    version: string
    /** The license the library declares: an SPDX expression, or the names its POM gives. */
    identifier?: string
    /** The text of the license as the library ships it. Only a frontend library has one. */
    text?: string
    /** Where the license is published. Only a backend library has one. */
    url?: string
}

/** The libraries bundled into the pages, and the libraries the server runs on. */
export type LicenseSide = 'frontend' | 'backend'

/**
 * The libraries of one side with their licenses.
 *
 * The lists are files the build writes beside the application rather than REST resources, so they are read with
 * `fetch` instead of `apiCall`.
 */
export const fetchLicenses = async (side: LicenseSide): Promise<License[]> => {
    const response = await fetch(`${CONFIG.CONTEXT}/licenses/${side}-licenses.json`)
    if (!response.ok) {
        throw new Error(`Failed to read the ${side} licenses: ${response.status}`)
    }
    return response.json()
}

/** Whether the license of a library can be opened: its text or its address is known. */
export const canOpenLicense = ({ text, url }: License): boolean => Boolean(text || url)

/**
 * Opens the license of a library in a new window.
 *
 * The text the library ships is shown as plain text. A library without one opens the page its license is published on.
 */
export const openLicense = ({ text, url }: License): void => {
    if (text) {
        const address = URL.createObjectURL(new Blob([text], { type: 'text/plain;charset=utf-8' }))
        window.open(address, '_blank', 'noopener')
        // The window has already taken the text: an address is resolved when the window is opened with it.
        URL.revokeObjectURL(address)
    } else if (url) {
        window.open(url, '_blank', 'noopener,noreferrer')
    }
}
