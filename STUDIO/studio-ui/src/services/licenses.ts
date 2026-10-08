import CONFIG from './config'

/** A third-party library OpenL Studio ships, and the license it is distributed under. */
export interface License {
    /** The npm package of a frontend library, `groupId:artifactId` of a backend one. */
    name: string
    version: string
    /** The license the library declares, as an SPDX expression such as `MIT` or `EPL-2.0 OR MPL-2.0`. */
    identifier?: string
    /** The text of the license as the library ships it, when it ships one. */
    text?: string
    /** Where the license is published, as the POM of a backend library names it. */
    url?: string
}

/**
 * Where the standard licenses are published, by SPDX identifier: the text of the license on the site of its steward.
 *
 * Every license the build accepts is listed: the frontend build accepts the licenses `npm run build` allows, the war
 * build the ones `license-maven-plugin` includes in `studio-backend/pom.xml`.
 */
const PUBLIC_LICENSES: Readonly<Record<string, string>> = {
    '0BSD': 'https://opensource.org/license/0bsd',
    'Apache-2.0': 'https://www.apache.org/licenses/LICENSE-2.0.txt',
    'BlueOak-1.0.0': 'https://blueoakcouncil.org/license/1.0.0',
    'BSD-2-Clause': 'https://opensource.org/license/bsd-2-clause',
    'BSD-3-Clause': 'https://opensource.org/license/bsd-3-clause',
    'CC0-1.0': 'https://creativecommons.org/publicdomain/zero/1.0/legalcode',
    'Classpath-exception-2.0': 'https://www.gnu.org/software/classpath/license.html',
    'EPL-1.0': 'https://www.eclipse.org/legal/epl-v10.html',
    'EPL-2.0': 'https://www.eclipse.org/legal/epl-2.0/',
    'GPL-2.0-only': 'https://www.gnu.org/licenses/old-licenses/gpl-2.0.txt',
    'ISC': 'https://opensource.org/license/isc-license-txt',
    'LGPL-2.1-or-later': 'https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt',
    'LGPL-3.0-only': 'https://www.gnu.org/licenses/lgpl-3.0.txt',
    'MIT': 'https://opensource.org/license/mit',
    'MIT-0': 'https://opensource.org/license/mit-0',
    'MPL-1.1': 'https://www.mozilla.org/en-US/MPL/1.1/',
    'MPL-2.0': 'https://www.mozilla.org/en-US/MPL/2.0/',
    'Python-2.0': 'https://docs.python.org/3/license.html',
    'Unlicense': 'https://unlicense.org/',
}

/** Where a standard license is published, by its SPDX identifier; `undefined` for any other license. */
export const publicLicense = (identifier: string): string | undefined => PUBLIC_LICENSES[identifier]

/** Splits an SPDX expression into its licenses, operators, spaces and parentheses, in their order. */
export const expressionParts = (expression: string): string[] => expression.split(/(\s+|[()])/).filter(Boolean)

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

/** Opens a text in a new window, as plain text. */
export const openText = (text: string): void => {
    const address = URL.createObjectURL(new Blob([text], { type: 'text/plain;charset=utf-8' }))
    window.open(address, '_blank', 'noopener')
    // The window has already taken the text: an address is resolved when the window is opened with it.
    URL.revokeObjectURL(address)
}
