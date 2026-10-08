import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AboutModal } from './AboutModal'
import { SystemContext } from '../../contexts'
import type { License } from '../../services/licenses'
import type { OpenlInfo } from '../../types/system'

const { fetchLicenses, openText } = vi.hoisted(() => ({
    fetchLicenses: vi.fn(),
    openText: vi.fn(),
}))

vi.mock('../../services/licenses', async importOriginal => ({
    ...await importOriginal<typeof import('../../services/licenses')>(),
    fetchLicenses,
    openText,
}))

vi.mock('react-i18next', () => {
    const t = (key: string, options?: { count?: number }) =>
        [key, options?.count].filter(part => part !== undefined).join(' ')
    return { useTranslation: () => ({ t }) }
})

const FRONTEND: License[] = [
    { name: 'react', version: '19.3.0', identifier: 'MIT', text: 'MIT License' },
    { name: 'dompurify', version: '3.4.2', identifier: '(MPL-2.0 OR Apache-2.0)', notice: 'DOMPurify Notice' },
    { name: 'custom', version: '1.0.0', identifier: 'LicenseRef-Custom', text: 'Custom License' },
    { name: 'no-license-file', version: '1.0.0', identifier: 'LicenseRef-Proprietary' },
]
const BACKEND: License[] = [
    { name: 'org.slf4j:slf4j-api', version: '2.0.17', identifier: 'MIT', url: 'https://opensource.org/license/mit' },
    { name: 'com.example:undeclared', version: '1.0.0', url: 'https://example.com/license' },
]

/** The links of an element, each by its text and the address it opens. */
const linksOf = (element: HTMLElement) =>
    within(element).queryAllByRole('link').map(link => [link.textContent, link.getAttribute('href')])

const openlInfo = (buildDate: string): OpenlInfo => ({
    'openl.site': 'https://openl-tablets.org',
    'openl.version': '6.5.0',
    'openl.build.date': buildDate,
    'openl.start.milli': '0',
    'openl.start.time': '2026-10-04T00:00:00Z',
    'openl.start.hash': 'ABCDEFGH',
})

const renderAbout = (buildDate = '2026-10-04') => {
    const tree = (isOpen: boolean) => (
        <SystemContext.Provider
            value={{
                appVersion: '6.5.0',
                openlInfo: openlInfo(buildDate),
                isExternalAuthSystem: false,
                isUserManagementEnabled: false,
                isGroupsManagementEnabled: false,
                isPersonalAccessTokenEnabled: false,
            }}
        >
            <AboutModal onClose={vi.fn()} open={isOpen} />
        </SystemContext.Provider>
    )
    const view = render(tree(true))
    return { ...view, reopen: (isOpen: boolean) => view.rerender(tree(isOpen)) }
}

describe('AboutModal', () => {
    beforeEach(() => {
        fetchLicenses.mockImplementation(async (side: string) => (side === 'frontend' ? FRONTEND : BACKEND))
    })

    it('shows the version, the build date in the UI language and the license of OpenL Studio', async () => {
        renderAbout()

        await screen.findByText('common:about.version')
        const terms = screen.getAllByRole('term').map(term => [term.textContent, term.nextElementSibling?.textContent])
        expect(terms).toEqual([
            ['common:about.version', '6.5.0'],
            ['common:about.build_date', 'Oct 4, 2026'],
            ['common:about.license', 'common:about.lgpl'],
        ])
        const license = screen.getByRole('link', { name: 'common:about.lgpl' })
        expect(license).toHaveAttribute('href', 'https://www.gnu.org/licenses/lgpl-3.0.html')
        expect(license).toHaveAttribute('target', '_blank')
        expect(license).toHaveAttribute('rel', 'noopener noreferrer')
    })

    it('shows a build date the build did not stamp as the server wrote it', async () => {
        renderAbout('????-??-??')

        const buildDate = (await screen.findByText('common:about.build_date')).nextElementSibling
        expect(buildDate).toHaveTextContent('????-??-??')
    })

    it('shows both sides collapsed, drawing no library until a side is expanded', async () => {
        renderAbout()

        expect(await screen.findByText('common:about.frontend 4')).toBeInTheDocument()
        expect(screen.getByText('common:about.backend 2')).toBeInTheDocument()
        expect(screen.queryByRole('list')).not.toBeInTheDocument()
        expect(fetchLicenses).toHaveBeenCalledWith('frontend')
        expect(fetchLicenses).toHaveBeenCalledWith('backend')
    })

    it('opens the license text a library ships, as plain text', async () => {
        renderAbout()

        await userEvent.click(await screen.findByText('common:about.frontend 4'))

        const [react, , custom] = within(screen.getByRole('list')).getAllByRole('listitem')
        expect(linksOf(react!)).toEqual([])
        await userEvent.click(within(react!).getByRole('button', { name: 'MIT' }))
        expect(openText).toHaveBeenCalledWith('MIT License')
        await userEvent.click(within(custom!).getByRole('button', { name: 'LicenseRef-Custom' }))
        expect(openText).toHaveBeenCalledWith('Custom License')
    })

    it('keeps each license of a text it opens whole on a line, and names one the library does not', async () => {
        const dual = { name: 'dual', version: '1.0.0', identifier: 'LicenseRef-Custom OR MIT', text: 'Dual License' }
        const unnamed = { name: 'unnamed', version: '1.0.0', text: 'Unnamed License' }
        fetchLicenses.mockImplementation(async (side: string) => (side === 'frontend' ? [dual, unnamed] : BACKEND))
        renderAbout()

        await userEvent.click(await screen.findByText('common:about.frontend 2'))

        const items = within(screen.getByRole('list')).getAllByRole('listitem')
        const license = within(items[0]!).getByRole('button', { name: 'LicenseRef-Custom OR MIT' })
        expect([...license.querySelectorAll('span')].map(part => part.textContent))
            .toEqual(['LicenseRef-Custom', 'OR', 'MIT'])
        await userEvent.click(within(items[1]!).getByRole('button', { name: 'common:about.license' }))
        expect(openText).toHaveBeenCalledWith('Unnamed License')
    })

    it('links each standard license of a library shipping no text to its public text', async () => {
        renderAbout()

        await userEvent.click(await screen.findByText('common:about.frontend 4'))

        const [, dompurify, , noLicenseFile] = within(screen.getByRole('list')).getAllByRole('listitem')
        expect(dompurify).toHaveTextContent('dompurify 3.4.2(MPL-2.0 OR Apache-2.0) common:about.notice')
        expect(linksOf(dompurify!)).toEqual([
            ['MPL-2.0', 'https://www.mozilla.org/en-US/MPL/2.0/'],
            ['Apache-2.0', 'https://www.apache.org/licenses/LICENSE-2.0.txt'],
        ])
        const mpl = within(dompurify!).getByRole('link', { name: 'MPL-2.0' })
        expect(mpl).toHaveAttribute('target', '_blank')
        expect(mpl).toHaveAttribute('rel', 'noopener noreferrer')
        expect(noLicenseFile).toHaveTextContent('no-license-file 1.0.0LicenseRef-Proprietary')
        expect(linksOf(noLicenseFile!)).toEqual([])
        expect(within(noLicenseFile!).queryByRole('button')).not.toBeInTheDocument()
    })

    it('links a license naming no standard one, and shipping no text, to the address its POM gives', async () => {
        renderAbout()

        await userEvent.click(await screen.findByText('common:about.backend 2'))

        const [slf4j, undeclared] = within(screen.getByRole('list')).getAllByRole('listitem')
        expect(linksOf(slf4j!)).toEqual([['MIT', 'https://opensource.org/license/mit']])
        expect(linksOf(undeclared!)).toEqual([['common:about.license', 'https://example.com/license']])
    })

    it('opens the NOTICE of a library that ships one, and offers none for any other', async () => {
        renderAbout()

        await userEvent.click(await screen.findByText('common:about.frontend 4'))

        const [react, dompurify] = within(screen.getByRole('list')).getAllByRole('listitem')
        expect(within(react!).queryByRole('button', { name: 'common:about.notice' })).not.toBeInTheDocument()
        await userEvent.click(within(dompurify!).getByRole('button', { name: 'common:about.notice' }))
        expect(openText).toHaveBeenCalledWith('DOMPurify Notice')
    })

    it('hides the libraries again when the side is collapsed', async () => {
        renderAbout()
        const summary = await screen.findByText('common:about.frontend 4')

        await userEvent.click(summary)
        expect(await screen.findByRole('list')).toBeInTheDocument()
        await userEvent.click(summary)

        await vi.waitFor(() => expect(screen.queryByRole('list')).not.toBeInTheDocument())
    })

    it('reports a list the server cannot answer with, and still shows the other', async () => {
        fetchLicenses.mockImplementation(async (side: string) => {
            if (side === 'backend') {
                throw new Error('Failed to read the backend licenses: 404')
            }
            return FRONTEND
        })
        renderAbout()

        await userEvent.click(await screen.findByText('common:about.backend 0'))

        expect(screen.getByText('common:about.unavailable')).toBeInTheDocument()
        await userEvent.click(screen.getByText('common:about.frontend 4'))
        expect(screen.getAllByRole('listitem')).toHaveLength(4)
    })

    it('reads the lists once, however often the dialog is shown', async () => {
        const { reopen } = renderAbout()
        await screen.findByText('common:about.frontend 4')

        reopen(false)
        reopen(true)

        expect(await screen.findByText('common:about.frontend 4')).toBeInTheDocument()
        expect(fetchLicenses).toHaveBeenCalledTimes(2)
    })
})
